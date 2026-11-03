package com.stepup.android.ui.screens.leaderboard

import android.graphics.Matrix
import android.graphics.SweepGradient
import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.MutableLongState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.RoundRect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Paint
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.ShaderBrush
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.currentStateAsState

/*
 * 리더보드 배경(2026-10-17 전달본 motion-spec.json) — 네이비 위 고정된 러닝 트랙 세 줄을 따라 푸른 빛이 흐른다.
 *
 * 아래 → 위: 네이비 바탕 → 부드러운 블루 빛 → 고정 트랙과 그 위를 도는 빛 → 내용. 트랙 · 숫자 · 목록은 움직이지 않는다 —
 * 빛(그라디언트)만 돈다. 그라디언트는 한 번 만들어 두고 프레임마다 회전 행렬만 바꾼다(픽셀을 다시 만들지 않는다).
 * 시계는 화면이 보이고 · 앱이 앞에 있고 · 시트가 닫혀 있고 · 모션이 켜져 있을 때만 흐른다. 멈췄다 돌아오면 그 자리부터 잇고,
 * 목록 · 범위 · 데이터가 바뀌어도 처음부터 다시 돌지 않는다(시계를 화면 묶음에 하나 둔다).
 */

internal object LeaderboardMotion {
    val Navy = Color(0xFF061B30)
    val Accent = Color(0xFF1450FF)
    private val TrackLine = Color(0x443F79E5)

    const val LIGHT_MS = 18_000L
    val LIGHT_PHASES = longArrayOf(-1_000L, -7_000L, -13_000L)
    const val GLOW_MS = 16_000L
    const val GLOW_PHASE = -5_000L
    const val SHEEN_MS = 10_000L
    const val SHEEN_PHASE = -3_000L

    private val GlowEasing = CubicBezierEasing(0.77f, 0f, 0.175f, 1f)

    /** 화면마다 트랙 · 빛의 자리(dp, 시안 360 너비 기준 — 오른쪽에서 잰다)와 배경 세기 */
    enum class Scene(val intensity: Float, val trackTop: Float, val trackRight: Float, val trackRotation: Float, val glowTop: Float, val glowRight: Float) {
        CREW(0.8f, 105f, -270f, -29f, 35f, -195f),
        PERSONAL(0.9f, 145f, -200f, -36f, 90f, -175f),
        POINTS(0.8f, 155f, -230f, -63f, 85f, -220f),
    }

    /** 트랙 한 줄의 빛 회전(도) — 18초에 한 바퀴, 줄마다 시작 위상이 다르다 */
    fun lightDegrees(clockMs: Long, lane: Int): Float {
        val t = Math.floorMod(clockMs - LIGHT_PHASES[lane], LIGHT_MS)
        return 360f * t / LIGHT_MS
    }

    /** 부드러운 빛 — 16초 왕복(alternate), 투명도 .58→1 · 이동 (0,0)→(−12,10) */
    fun glow(clockMs: Long, reduced: Boolean): Triple<Float, Float, Float> {
        if (reduced) return Triple(0.7f, 0f, 0f)
        val cycle = Math.floorMod(clockMs - GLOW_PHASE, 2 * GLOW_MS)
        val forward = if (cycle < GLOW_MS) cycle.toFloat() / GLOW_MS else 1f - (cycle - GLOW_MS).toFloat() / GLOW_MS
        val p = GlowEasing.transform(forward.coerceIn(0f, 1f))
        return Triple(0.58f + 0.42f * p, -12f * p, 10f * p)
    }

    /** 크루 1위 띠 — 10초에 한 번 대각선으로 지난다. (이동 비율 0..1, 투명도) */
    fun sheen(clockMs: Long): Pair<Float, Float> {
        val p = Math.floorMod(clockMs - SHEEN_PHASE, SHEEN_MS).toFloat() / SHEEN_MS
        val alpha = when {
            p < 0.1f -> 0.2f * (p / 0.1f)
            p < 0.5f -> 0.2f + 0.45f * ((p - 0.1f) / 0.4f)
            p < 0.9f -> 0.65f - 0.45f * ((p - 0.5f) / 0.4f)
            else -> 0.2f * (1f - (p - 0.9f) / 0.1f)
        }
        return p to alpha
    }

    internal fun trackLine() = TrackLine

    /** 세로 가림 — 화면 높이 1% 에서 투명, 13~51% 보임, 73% 에서 다시 투명 */
    val VerticalMask = arrayOf(0f to Color.Transparent, 0.01f to Color.Transparent, 0.13f to Color.Black, 0.51f to Color.Black, 0.73f to Color.Transparent, 1f to Color.Transparent)
}

/**
 * 리더보드 화면 묶음의 시계 하나 — [running] 일 때만 프레임마다 흐른다(ms). 멈추면 그대로 두고, 다시 흐르면 그 자리부터 잇는다.
 * 앱이 앞에 없으면(RESUMED 아님) 흐르지 않는다.
 */
@Composable
internal fun rememberLeaderboardClock(running: Boolean): MutableLongState {
    val clock = remember { mutableLongStateOf(0L) }
    val lifecycle = LocalLifecycleOwner.current.lifecycle
    val state by lifecycle.currentStateAsState()
    val active = running && state.isAtLeast(Lifecycle.State.RESUMED)
    LaunchedEffect(active) {
        if (!active) return@LaunchedEffect
        var last = withFrameNanos { it }
        while (true) {
            val now = withFrameNanos { it }
            // 멈췄다 돌아온 첫 프레임이 크게 건너뛰지 않게 한 프레임 최대 100ms
            clock.longValue += ((now - last) / 1_000_000L).coerceIn(0L, 100L)
            last = now
        }
    }
    return clock
}

/**
 * 배경 그림 — 네이비 바탕 · 부드러운 빛 · 트랙 세 줄과 그 위를 도는 빛. [clock] 은 [rememberLeaderboardClock] 의 값을 읽는 함수다
 * (그리기 단계에서만 읽어 화면 전체를 다시 구성하지 않는다). 모션 감소면 빛이 돌지 않는 정적인 트랙 · 빛만 둔다.
 *
 * 길(Path) · 붓은 크기 · 판이 바뀔 때만 한 번 만들고(drawWithCache), 프레임마다는 투명도 · 위치 · 회전 행렬만 바꾼다.
 */
@Composable
internal fun LeaderboardBackdrop(
    scene: LeaderboardMotion.Scene,
    clock: () -> Long,
    reduced: Boolean,
    modifier: Modifier = Modifier,
) {
    val lanes = remember { List(3) { LaneLight() } }
    val currentScene by rememberUpdatedState(scene)
    val still by rememberUpdatedState(reduced)
    val currentClock by rememberUpdatedState(clock)
    Spacer(
        modifier.fillMaxSize().clipToBounds().drawWithCache {
            val s = currentScene
            val unit = 1.dp.toPx()
            // 부드러운 빛 — CSS radial-gradient(ellipse at center) 의 기본 크기는 farthest-corner(정사각형이면 반너비 × √2)
            val glowSize = 430f * unit
            val glowRadius = glowSize / 2f * 1.4142135f
            val glowCenter = Offset(glowSize / 2f, glowSize / 2f)
            val glowBrush = Brush.radialGradient(
                0f to Color(0x661450FF), 0.43f to Color(0x221450FF), 0.71f to Color.Transparent,
                center = glowCenter, radius = glowRadius,
            )
            // 트랙 — 560 × 336 경기장, 안쪽으로 20 · 40 씩 들어간 세 줄(모서리 168 · 148 · 128), 두께 2, 화면마다 기울기 · 자리
            val fieldW = 560f * unit
            val fieldH = 336f * unit
            val fieldLeft = size.width - (fieldW + s.trackRight * unit)
            val fieldTop = s.trackTop * unit
            val center = Offset(fieldLeft + fieldW / 2f, fieldTop + fieldH / 2f)
            val paths = List(3) { lane ->
                val inset = 20f * lane * unit
                val radius = (168f - 20f * lane) * unit
                Path().apply {
                    addRoundRect(
                        RoundRect(
                            Rect(fieldLeft + inset, fieldTop + inset, fieldLeft + fieldW - inset, fieldTop + fieldH - inset),
                            CornerRadius(radius, radius),
                        ),
                    )
                }
            }
            val stroke = Stroke(2f * unit)
            val mask = Brush.verticalGradient(*LeaderboardMotion.VerticalMask)
            val layer = Paint().apply { alpha = s.intensity }
            onDrawBehind {
                drawRect(LeaderboardMotion.Navy)
                val t = currentClock()
                val reducedNow = still
                // 배경 전체 세기 · 세로 가림은 한 겹 안에서(가림은 이 겹에만 든다 — 네이비 바탕은 그대로)
                drawContext.canvas.saveLayer(Rect(Offset.Zero, size), layer)
                val (glowAlpha, gx, gy) = LeaderboardMotion.glow(t, reducedNow)
                translate(size.width - (glowSize + s.glowRight * unit) + gx * unit, s.glowTop * unit + gy * unit) {
                    drawCircle(glowBrush, radius = glowRadius * 0.71f, center = glowCenter, alpha = glowAlpha)
                }
                withTransform({ rotate(s.trackRotation, pivot = center) }) {
                    paths.forEachIndexed { lane, path ->
                        drawPath(path, LeaderboardMotion.trackLine(), alpha = 0.9f, style = stroke)
                        if (!reducedNow) {
                            // 트랙 위를 도는 빛 — 같은 가운데의 원뿔 그라디언트(conic from -90deg)를 돌려 선 위에만 칠한다
                            drawPath(path, lanes[lane].brush(center, 180f + LeaderboardMotion.lightDegrees(t, lane)), alpha = 0.9f, style = stroke)
                        }
                    }
                }
                // 세로 가림 — 위 1% 투명 → 13~51% 보임 → 73% 투명
                drawRect(mask, blendMode = BlendMode.DstIn)
                drawContext.canvas.restore()
            }
        },
    )
}

/**
 * 트랙 한 줄의 빛 붓 — 원뿔 그라디언트 하나를 만들어 두고 회전만 바꾼다(프레임마다 새로 만들지 않는다).
 * 색 차례는 conic-gradient(from -90deg, 투명 0~235° · #1450FF1A 260° · #1450FF88 320° · #75A9FF 354° · #DAE9FF 359° · 투명 360°).
 */
private class LaneLight {
    private var shader: SweepGradient? = null
    private var brush: ShaderBrush? = null
    private var at: Offset = Offset.Unspecified
    private val matrix = Matrix()

    fun brush(center: Offset, degrees: Float): ShaderBrush {
        val current = shader?.takeIf { at == center } ?: SweepGradient(
            center.x, center.y,
            intArrayOf(
                Color.Transparent.toArgb(), Color.Transparent.toArgb(), Color(0x1A1450FF).toArgb(), Color(0x881450FF).toArgb(),
                Color(0xFF75A9FF).toArgb(), Color(0xFFDAE9FF).toArgb(), Color.Transparent.toArgb(),
            ),
            floatArrayOf(0f, 235f / 360f, 260f / 360f, 320f / 360f, 354f / 360f, 359f / 360f, 1f),
        ).also {
            shader = it
            brush = ShaderBrush(it)
            at = center
        }
        matrix.setRotate(degrees, center.x, center.y)
        current.setLocalMatrix(matrix)
        return brush ?: ShaderBrush(current).also { brush = it }
    }
}

/** 크루 1위 빛 띠의 붓 — 폭 90 띠의 가로 그라디언트(한 번 만들어 둔다) */
internal fun championSheenBrush(bandWidthPx: Float): Brush =
    Brush.horizontalGradient(listOf(Color.Transparent, Color(0x44C1D7FF), Color.Transparent), startX = 0f, endX = bandWidthPx)

/** 크루 1위 영역의 빛 띠 — 폭 90 · 높이 420 띠가 −22° 기울어 10초에 한 번 지난다(카드 안에서만, 글자 아래). 모션 감소면 그리지 않는다 */
internal fun DrawScope.drawChampionSheen(clockMs: Long, band: Brush) {
    val unit = 1.dp.toPx()
    val (p, alpha) = LeaderboardMotion.sheen(clockMs)
    val bandW = 90f * unit
    val bandH = 420f * unit
    val left = -160f * unit + 640f * unit * p
    val top = -100f * unit
    translate(left, top) {
        rotate(-22f, pivot = Offset(bandW / 2f, bandH / 2f)) {
            drawRect(band, size = Size(bandW, bandH), alpha = alpha)
        }
    }
}
