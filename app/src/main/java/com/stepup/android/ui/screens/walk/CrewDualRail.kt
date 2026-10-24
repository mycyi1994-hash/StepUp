package com.stepup.android.ui.screens.walk

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.WarningAmber
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.MutableLongState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.layout
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.disabled
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.currentStateAsState
import com.stepup.android.R
import com.stepup.android.ui.experience.feedbackClickable
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.sin

/*
 * 크루 러닝 3번 듀얼 레일(2026-10-24 핸드오프 04_DESIGN_BACKGROUND · design-tokens.json).
 *
 * - 크루원 카드와 내 신발 카드는 같은 틀 하나([DualRailFrame]): 아이스 블루 5dp 바깥 테 · 9dp 안쪽 1.5dp 두 번째 선 · 짙은 남색 속.
 *   테와 카드는 움직이지 않는다. 일반 줄 · 작은 버튼에는 두르지 않는다.
 * - 사진 터널과 움직이는 빛은 대기실에만([LobbyTunnelBackdrop]). 보일 때만 흐르고 시트 · 카운트다운 · 앱이 뒤에 있으면 멈춘다.
 *   모션 줄이기면 사진은 그대로, 빛의 세기만 천천히 바뀐다(이동 · 확대 · 회전 없음).
 * - 그 밖의 크루 화면(홈 · 목록 · 개설 · 코스 · 결과)은 앱의 기존 배경(crewColors)에 같은 틀만 쓴다.
 */
internal object LobbyInk {
    val Bg = Color(0xFF031427)
    val Text = Color(0xFFF5F8FF)
    val Sub = Color(0xFFB5CBE6)
    val Accent = Color(0xFF1166FF)
    val Info = Color(0xFF48D9FA)
    val Ice = Color(0xFF95E6FF)
    val ChipFace = Color(0xFF83E5FF)
    val ChipInk = Color(0xFF073346)
    val ChipSoftFace = Color(0xFF174962)
    val ChipSoftInk = Color(0xFFC4F2FF)
    val ChipWarnFace = Color(0xFFE8D5A2)
    val ChipWarnInk = Color(0xFF332B15)
    val ShoeButtonInk = Color(0xFF07384D)
    val PrimaryInk = Color(0xFF071B3D)
    val DisabledFace = Color(0xFF24455D)
    val DisabledInk = Color(0xFFA2BDD0)
    val Sheet = Color(0xFF08243A)
    val SheetEdge = Color(0x6666B8DB)
    val Track = Color(0xFF1D425E)
    val LevelBar = Color(0xFF83AFFF)
    val Avatar = Color(0xFF12435E)
    val AvatarRing = Color(0xFF78D9F9)
    val AvatarEmpty = Color(0xFF09283F)
    val AvatarEmptyRing = Color(0xFF53738C)
    val AvatarEmptyInk = Color(0xFF91B6CF)
    val HostTop = Color(0xFF2B83FF)
    val HostBottom = Color(0xFF0D56E5)
    val CheckFace = Color(0xFFB6F0FF)
    val CheckInk = Color(0xFF06364F)
    val CheckRing = Color(0xFF133950)
    val Map = Color(0xFF15304A)
    val NoteFace = Color(0xFF163D56)
    val NoteInk = Color(0xFFD5ECFA)
    val WarnFace = Color(0xFF423C2C)
    val WarnInk = Color(0xFFF1E4BD)
    val WarnIcon = Color(0xFFF1D989)
    val Divider = Color(0x2693CDEE)
    val RowLine = Color(0x20A4CDEA)
    val Field = Color(0xFF09253C)
    val FieldEdge = Color(0xFF4E8DAE)
    val FieldHint = Color(0xFF88ABC3)
    val Choice = Color(0xFF0C2C45)
    val ChoiceEdge = Color(0xFF396B87)
    val ChoiceOn = Color(0xFF16425C)
    val Label = Color(0xFFB7CDE8)
    val Scrim = Color(0xAA000000)

    // 듀얼 레일 틀
    val RailA = Color(0xFF96EAFF)
    val RailB = Color(0xFF56CBFA)
    val RailC = Color(0xFF2E9FD5)
    val PanelTop = Color(0xFF113A58)
    val PanelBottom = Color(0xFF09253C)
    val InnerLine = Color(0xA682D8F9)
    val InnerLineDim = Color(0xFF416E88)
    val BottomShade = Color(0xFF052338)

    val Gutter = 24.dp
}

/**
 * CSS linear-gradient(각도) 를 이 크기의 붓으로 — 0도는 위, 90도는 오른쪽. 선은 가운데를 지나고 길이는 모서리까지(CSS 와 같다).
 */
internal fun cssLinearGradient(degrees: Float, size: Size, vararg stops: Pair<Float, Color>): Brush {
    val rad = Math.toRadians(degrees.toDouble())
    val dx = sin(rad).toFloat()
    val dy = -cos(rad).toFloat()
    val half = (abs(size.width * dx) + abs(size.height * dy)) / 2f
    val center = Offset(size.width / 2f, size.height / 2f)
    return Brush.linearGradient(
        *stops,
        start = Offset(center.x - dx * half, center.y - dy * half),
        end = Offset(center.x + dx * half, center.y + dy * half),
    )
}

/**
 * 듀얼 레일 틀 — 크루원 · 내 신발 카드(같은 부품). 바깥 테 5dp(110° #96EAFF → #56CBFA 45% → #2E9FD5) · 모서리 20dp,
 * 속 125° #113A58 → #09253C 82%, 9dp 안쪽 1.5dp 두 번째 선(#82D8F9 65%, 모서리 11dp), 아래 5dp 음영과 부드러운 그림자.
 * [dimmed] 면(고르지 않은 선택지) 바깥 테를 옅게, 안쪽 선을 어둡게 — 모양 · 크기는 같다.
 */
@Composable
internal fun DualRailFrame(
    modifier: Modifier = Modifier,
    padding: PaddingValues = PaddingValues(20.dp),
    dimmed: Boolean = false,
    content: @Composable BoxScope.() -> Unit,
) {
    val shape = RoundedCornerShape(20.dp)
    Box(
        modifier
            .drawBehind {
                // 아래 5dp 음영 — 틀과 같은 모양을 아래로 5dp
                drawRoundRect(LobbyInk.BottomShade, topLeft = Offset(0f, 5.dp.toPx()), size = size, cornerRadius = CornerRadius(20.dp.toPx()))
            }
            .shadow(10.dp, shape, clip = false, ambientColor = Color.Black.copy(alpha = 0.27f), spotColor = Color.Black.copy(alpha = 0.27f))
            .drawWithCache {
                val panel = cssLinearGradient(125f, size, 0f to LobbyInk.PanelTop, 0.82f to LobbyInk.PanelBottom)
                val rail = cssLinearGradient(110f, size, 0f to LobbyInk.RailA, 0.45f to LobbyInk.RailB, 1f to LobbyInk.RailC)
                val outer = 5.dp.toPx()
                val inset = 9.dp.toPx()
                val line = 1.5.dp.toPx()
                val r = 20.dp.toPx()
                val innerR = 11.dp.toPx()
                onDrawBehind {
                    drawRoundRect(panel, cornerRadius = CornerRadius(r))
                    // 바깥 테 — 선 가운데가 2.5dp 안쪽이면 바깥 모서리 20 · 안쪽 15(CSS 의 5px 테)
                    drawRoundRect(
                        rail, topLeft = Offset(outer / 2f, outer / 2f), size = Size(size.width - outer, size.height - outer),
                        cornerRadius = CornerRadius(r - outer / 2f), style = Stroke(outer), alpha = if (dimmed) 0.42f else 1f,
                    )
                    // 두 번째 선 — 바깥 가장자리가 9dp 안쪽, 모서리 11dp
                    val c = inset + line / 2f
                    drawRoundRect(
                        if (dimmed) LobbyInk.InnerLineDim else LobbyInk.InnerLine, topLeft = Offset(c, c),
                        size = Size(size.width - c * 2f, size.height - c * 2f), cornerRadius = CornerRadius(innerR - line / 2f),
                        style = Stroke(line),
                    )
                }
            }
            .padding(padding),
        content = content,
    )
}

/** 대기실의 큰 주 버튼 — 흰 면 #F5F8FF · 글자 #071B3D · 모서리 14dp · 높이 60dp. 못 누르면 #24455D / #A2BDD0(실제로 막힌다) */
@Composable
internal fun LobbyPrimaryButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    busy: Boolean = false,
) {
    val shape = RoundedCornerShape(14.dp)
    val live = enabled && !busy
    Box(
        modifier.fillMaxWidth().heightIn(min = 60.dp)
            .shadow(if (live) 6.dp else 0.dp, shape, ambientColor = Color.Black.copy(alpha = 0.13f), spotColor = Color.Black.copy(alpha = 0.13f))
            .clip(shape)
            .background(if (enabled) LobbyInk.Text else LobbyInk.DisabledFace, shape)
            .then(
                if (live) Modifier.feedbackClickable(role = Role.Button, onClick = onClick)
                else Modifier.semantics { if (!enabled) disabled() },
            )
            .padding(horizontal = 18.dp, vertical = 12.dp),
        contentAlignment = Alignment.Center,
    ) {
        if (busy) {
            CircularProgressIndicator(Modifier.size(22.dp), color = LobbyInk.PrimaryInk, strokeWidth = 2.5.dp)
        } else {
            Text(
                text, style = navyText(18.sp, if (enabled) LobbyInk.PrimaryInk else LobbyInk.DisabledInk, FontWeight.ExtraBold, 1.3f),
                textAlign = TextAlign.Center,
            )
        }
    }
}

/** 작은 칩 — 준비(하늘 면) · 부드러운(남색 면) · 주의(모래 면). 누르는 것이 아니다 */
internal enum class LobbyChipKind { Ready, Soft, Warn }

@Composable
internal fun LobbyChip(text: String, modifier: Modifier = Modifier, kind: LobbyChipKind = LobbyChipKind.Ready) {
    val (face, ink) = when (kind) {
        LobbyChipKind.Ready -> LobbyInk.ChipFace to LobbyInk.ChipInk
        LobbyChipKind.Soft -> LobbyInk.ChipSoftFace to LobbyInk.ChipSoftInk
        LobbyChipKind.Warn -> LobbyInk.ChipWarnFace to LobbyInk.ChipWarnInk
    }
    Text(
        text, style = navyText(12.sp, ink, FontWeight.ExtraBold, 1.55f), maxLines = 1,
        modifier = modifier.clip(RoundedCornerShape(6.dp)).background(face).padding(horizontal = 9.dp, vertical = 3.dp),
    )
}

/** 내 신발 변경 — 면 #95E6FF · 글자 #07384D · 모서리 8dp · 높이 44dp 이상(누르는 곳은 48dp) */
@Composable
internal fun ShoeChangeButton(text: String, onClick: () -> Unit, modifier: Modifier = Modifier, enabled: Boolean = true) {
    Box(
        modifier.heightIn(min = 48.dp).padding(vertical = 2.dp)
            .clip(RoundedCornerShape(8.dp))
            .background(if (enabled) LobbyInk.Ice else LobbyInk.DisabledFace)
            .feedbackClickable(enabled = enabled, role = Role.Button, onClick = onClick)
            .heightIn(min = 44.dp).padding(horizontal = 10.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(text, style = navyText(12.sp, if (enabled) LobbyInk.ShoeButtonInk else LobbyInk.DisabledInk, FontWeight.ExtraBold), maxLines = 1)
    }
}

/** 진행 막대 — 6dp, 어두운 길 위 하늘색(레벨은 연한 파랑) */
@Composable
internal fun LobbyTrack(fraction: Float, modifier: Modifier = Modifier, level: Boolean = false, height: Dp = 6.dp) {
    val shape = RoundedCornerShape(6.dp)
    Box(modifier.fillMaxWidth().heightIn(min = height, max = height).clip(shape).background(LobbyInk.Track)) {
        Box(
            Modifier.fillMaxWidth(fraction.coerceIn(0f, 1f)).heightIn(min = height, max = height).clip(shape)
                .background(if (level) LobbyInk.LevelBar else LobbyInk.Info),
        )
    }
}

/** 안내 띠 — 남색(정보) · 모래(주의) 면, 아이콘과 글(색만으로 가르지 않는다) */
@Composable
internal fun LobbyNotice(
    text: String,
    modifier: Modifier = Modifier,
    warn: Boolean = false,
    icon: androidx.compose.ui.graphics.vector.ImageVector = if (warn) Icons.Outlined.WarningAmber else Icons.Outlined.Info,
) {
    Row(
        modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp)).background(if (warn) LobbyInk.WarnFace else LobbyInk.NoteFace)
            .padding(horizontal = 14.dp, vertical = 12.dp),
        verticalAlignment = Alignment.Top,
    ) {
        Icon(
            icon, contentDescription = null, tint = if (warn) LobbyInk.WarnIcon else LobbyInk.Ice,
            modifier = Modifier.padding(top = 2.dp).size(17.dp),
        )
        Spacer(Modifier.width(9.dp))
        Text(text, style = navyText(13.sp, if (warn) LobbyInk.WarnInk else LobbyInk.NoteInk, FontWeight.Medium, 1.6f))
    }
}

// ── 대기실 터널 ─────────────────────────────────────────────────────

/**
 * 대기실 터널의 시간표(04_DESIGN_BACKGROUND 6절 · prototype all-scenes.js) — 시각(ms)을 넣으면 모습이 나온다(LobbyTunnelMotionTest).
 * 사진 두 겹이 11초마다 앞으로 다가오며 겹친다(시작 위상 10% · 60%). 옆 빛은 4.2초 왕복, 가운데 빛은 같은 박자로 세기만.
 */
internal object LobbyTunnelMotion {
    const val PUSH_MS = 11_000L
    const val LIGHT_MS = 4_200L
    const val PHASE_A = 0.10f
    const val PHASE_B = 0.60f

    /** 모션 줄이기 — 같은 박자를 1.6배 느리게, 빛의 세기만 */
    const val GENTLE = 1.6f

    /** 사진 한 겹 — 배율 · 투명도. 0%: 1 · 0 → 14%: 1.03 · 1 → 70%: 1.15 · 1 → 100%: 1.23 · 0 */
    fun push(clockMs: Long, phase: Float): Pair<Float, Float> {
        val p = ((clockMs.toDouble() / PUSH_MS + phase) % 1.0).toFloat()
        return when {
            p < 0.14f -> lerp(1f, 1.03f, p / 0.14f) to lerp(0f, 1f, p / 0.14f)
            p < 0.70f -> lerp(1.03f, 1.15f, (p - 0.14f) / 0.56f) to 1f
            else -> lerp(1.15f, 1.23f, (p - 0.70f) / 0.30f) to lerp(1f, 0f, (p - 0.70f) / 0.30f)
        }
    }

    /** 왕복(alternate) ease-in-out 진행 0 → 1 → 0. 처음은 26% 지점(시안의 시작 위치) */
    fun swing(clockMs: Long, gentle: Boolean): Float {
        val period = if (gentle) (LIGHT_MS * GENTLE).toLong() else LIGHT_MS
        val t = clockMs + (period * 0.26f).toLong()
        val cycle = Math.floorMod(t, 2 * period)
        val forward = if (cycle < period) cycle.toFloat() / period else 1f - (cycle - period).toFloat() / period
        return easeInOut(forward)
    }

    /** 옆 빛 — 왼쪽 x +60 → −30dp · 회전 −20 → −30도 · 세기 .3 → 1(오른쪽은 거울). 모션 줄이기면 세기 .25 → .65 만 */
    data class Side(val dx: Float, val degrees: Float, val alpha: Float)

    fun side(clockMs: Long, gentle: Boolean, left: Boolean): Side {
        val k = swing(clockMs, gentle)
        if (gentle) return Side(0f, 0f, lerp(0.25f, 0.65f, k))
        val sign = if (left) 1f else -1f
        return Side(sign * lerp(60f, -30f, k), -sign * lerp(20f, 30f, k), lerp(0.3f, 1f, k))
    }

    /** 가운데 빛 세기 .4 → 1 */
    fun glow(clockMs: Long, gentle: Boolean): Float = lerp(0.4f, 1f, swing(clockMs, gentle))

    // CSS ease-in-out = cubic-bezier(.42,0,.58,1)
    private val EaseInOut = androidx.compose.animation.core.CubicBezierEasing(0.42f, 0f, 0.58f, 1f)
    private fun easeInOut(x: Float): Float = EaseInOut.transform(x.coerceIn(0f, 1f))
    private fun lerp(a: Float, b: Float, t: Float): Float = a + (b - a) * t.coerceIn(0f, 1f)
}

/** 대기실 시계 — [running] 이고 앱이 앞에 있을 때만 흐른다. 멈췄다 돌아오면 그 자리부터(처음부터 다시 돌지 않는다) */
@Composable
internal fun rememberLobbyClock(running: Boolean): MutableLongState {
    val clock = remember { mutableLongStateOf(0L) }
    val lifecycle = LocalLifecycleOwner.current.lifecycle
    val state by lifecycle.currentStateAsState()
    val active = running && state.isAtLeast(Lifecycle.State.RESUMED)
    LaunchedEffect(active) {
        if (!active) return@LaunchedEffect
        var last = withFrameNanos { it }
        while (true) {
            val now = withFrameNanos { it }
            clock.longValue += ((now - last) / 1_000_000L).coerceIn(0L, 100L)
            last = now
        }
    }
    return clock
}

/**
 * 대기실 배경 — 정지 사진 한 장 위에 앞으로 다가오는 사진 두 겹, 옆 빛 둘, 가운데 빛, 글 쪽을 어둡게 하는 덮개.
 * [running] 이 아니면(시트 · 카운트다운 · 앱이 뒤) 그 자리에 멈춘다. [dim] 이면(시트 아래) 덮개를 짙게.
 * 모션 줄이기면 시계가 서고 다가오는 겹을 두지 않는다 — 정지 사진과 멈춘 빛.
 * [still] 이면(불러오는 중 — LOBBY_STATIC) 사진과 덮개만. 사진은 이 화면에 들어올 때 한 번 읽는다(출발할 때마다 다시 읽지 않는다).
 */
@Composable
internal fun LobbyTunnelBackdrop(
    running: Boolean,
    reduced: Boolean,
    modifier: Modifier = Modifier,
    dim: Boolean = false,
    still: Boolean = false,
) {
    // 모션 줄이기면 시계를 세우고 정지 사진만(이동 · 확대 · 회전 · 빛의 흐름 없음)
    val clock = rememberLobbyClock(running && !still && !reduced)
    val gentle by rememberUpdatedState(reduced)
    val photo = painterResource(R.drawable.crew_lobby_tunnel)
    Box(modifier.fillMaxSize().background(LobbyInk.Bg).clipToBounds()) {
        // 바탕 사진 — 가장자리 12dp 를 넘겨 깔아 확대해도 빈 곳이 보이지 않게
        Image(photo, contentDescription = null, contentScale = ContentScale.Crop, modifier = Modifier.fillMaxSize().bleed(12.dp))
        if (!still && !reduced) {
            for (phase in floatArrayOf(LobbyTunnelMotion.PHASE_A, LobbyTunnelMotion.PHASE_B)) {
                Image(
                    photo, contentDescription = null, contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize().bleed(12.dp).graphicsLayer {
                        val (scale, alpha) = LobbyTunnelMotion.push(clock.longValue, phase)
                        scaleX = scale
                        scaleY = scale
                        this.alpha = alpha
                        transformOrigin = TransformOrigin(0.5f, 0.43f)
                    },
                )
            }
        }
        // 빛 — 옆 두 줄기 · 가운데(그리기 단계에서만 시계를 읽는다)
        if (!still) {
            Spacer(
                Modifier.fillMaxSize().drawWithCache {
                    val stripW = 180.dp.toPx()
                    val stripH = 1250.dp.toPx()
                    val strip = Brush.horizontalGradient(
                        0f to Color.Transparent, 0.42f to Color(0x5562CFFF), 0.52f to Color(0x66E5F8FF), 0.68f to Color.Transparent,
                        startX = 0f, endX = stripW,
                    )
                    // 가운데 빛 — 480 × 410 상자의 radial-gradient(ellipse) 기본 크기(farthest-corner = 반너비 × √2), 65% 에서 투명
                    val glowW = 480.dp.toPx()
                    val glowH = 410.dp.toPx()
                    val glowCenter = Offset(size.width / 2f, 175.dp.toPx() + glowH / 2f)
                    val glowRadius = glowW / 2f * 1.4142135f
                    val glow = Brush.radialGradient(
                        0f to Color(0x551C8EFF), 0.65f to Color.Transparent, center = glowCenter, radius = glowRadius,
                    )
                    onDrawBehind {
                        val t = clock.longValue
                        for (left in booleanArrayOf(true, false)) {
                            val s = LobbyTunnelMotion.side(t, gentle, left)
                            val x = if (left) -110.dp.toPx() else size.width + 110.dp.toPx() - stripW
                            val top = -150.dp.toPx()
                            // 회전 중심 — 왼쪽은 오른쪽 끝 45%, 오른쪽은 왼쪽 끝 45%(시안 transform-origin)
                            val pivot = Offset(if (left) x + stripW else x, top + stripH * 0.45f)
                            translate(s.dx.dp.toPx(), 0f) {
                                rotate(s.degrees, pivot) {
                                    translate(x, top) {
                                        drawRect(strip, size = Size(stripW, stripH), alpha = s.alpha, blendMode = BlendMode.Screen)
                                    }
                                }
                            }
                        }
                        scale(1f, glowH / glowW, pivot = glowCenter) {
                            drawCircle(
                                glow, radius = glowRadius * 0.65f, center = glowCenter,
                                alpha = LobbyTunnelMotion.glow(t, gentle), blendMode = BlendMode.Screen,
                            )
                        }
                    }
                },
            )
        }
        // 덮개 — 위 · 아래를 조금, 제목이 오는 왼쪽 위를 더 어둡게(글이 읽히게). 시트 아래면 전체를 짙게
        Spacer(
            Modifier.fillMaxSize().drawWithCache {
                val vertical = Brush.verticalGradient(
                    0f to Color(0x44031427), (175.dp.toPx() / size.height).coerceIn(0f, 1f) to Color.Transparent,
                    (440.dp.toPx() / size.height).coerceIn(0f, 1f) to Color.Transparent, 1f to Color(0x44031427),
                )
                val title = Brush.radialGradient(
                    0f to Color(0x99031427), 1f to Color.Transparent,
                    center = Offset(size.width * 0.05f, 126.dp.toPx()), radius = 290.dp.toPx(),
                )
                val body = Brush.radialGradient(
                    0f to Color(0xCC031427), 1f to Color.Transparent,
                    center = Offset(size.width * 0.09f, size.height * 0.65f), radius = 235.dp.toPx(),
                )
                onDrawBehind {
                    drawRect(vertical)
                    // 타원 덮개 — 가로로 긴 타원(290 × 88, 235 × 145)
                    scale(1f, 88f / 290f, pivot = Offset(size.width * 0.05f, 126.dp.toPx())) { drawRect(title) }
                    scale(1f, 145f / 235f, pivot = Offset(size.width * 0.09f, size.height * 0.65f)) { drawRect(body) }
                    if (dim) drawRect(Color(0x99031427))
                }
            },
        )
    }
}

/** 둘레로 [by] 만큼 넘겨 깐다(CSS inset: -12px) — 확대하는 사진 가장자리에 빈 곳이 보이지 않게 */
private fun Modifier.bleed(by: Dp): Modifier = layout { measurable, constraints ->
    val extra = (by * 2).roundToPx()
    val w = constraints.maxWidth
    val h = constraints.maxHeight
    val placeable = measurable.measure(androidx.compose.ui.unit.Constraints.fixed(w + extra, h + extra))
    layout(w, h) { placeable.place(-extra / 2, -extra / 2) }
}
