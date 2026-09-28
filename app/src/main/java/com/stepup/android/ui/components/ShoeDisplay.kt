package com.stepup.android.ui.components

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.util.LruCache
import androidx.annotation.DrawableRes
import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Image
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.State
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.BiasAlignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.FilterQuality
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntRect
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import com.stepup.android.domain.Sneaker
import com.stepup.android.ui.experience.LocalMotion
import com.stepup.android.ui.theme.StepUpColors
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/*
 * 보유 신발 전시(보유 신발 상세 v1, 2026-09-28 전달본 — docs/redesign/shoe-detail-v1) — 낮은 받침 · 접촉 그림자 ·
 * 배경에 스며드는 조명 위에 실제 신발 한 켤레. 신발 탭 첫 화면과 신발 상세가 쓴다.
 *
 * 받침 · 조명은 시안(03-components/shoe-stage.svg, 390 × 420 좌표)의 도형을 그대로 Compose 로 그린다 — 그림 파일을 붙이지 않는다.
 * 신발은 앱에 든 실제 신발 그림([sneakerArtRes] — 새 도감 70종은 모델 번호로, 예전 52종은 속성 × 변형으로)이고,
 * 그림에서 보이는 부분(알파) 경계를 재어 바닥을 접촉 그림자에 맞춘다. 신발 뒤에 색 판을 깔지 않는다.
 */

/** 신발 그림 한 장 — 디코드한 그림과 그 안에서 보이는 부분(알파) 경계(픽셀) */
@Immutable
class ShoeArt(val image: ImageBitmap, val visible: IntRect)

sealed interface ShoeArtLoad {
    data object Loading : ShoeArtLoad

    /** 그림을 읽지 못했다 — 이 앱에 그림이 없는 모델이거나 디코드 실패(시안 16). 다른 신발 그림으로 채우지 않는다 */
    data object Failed : ShoeArtLoad
    data class Ready(val art: ShoeArt) : ShoeArtLoad
}

/**
 * 신발 그림 메모리 사본 — 개수가 아니라 바이트로 잰다(640 × 640 한 장이 1.6MB).
 * 읽기는 화면 밖 스레드에서 하고, 읽은 김에 보이는 부분 경계를 재 둔다.
 */
object ShoeArtCache {
    private val memory = object : LruCache<String, ShoeArt>(budgetBytes()) {
        override fun sizeOf(key: String, value: ShoeArt): Int = value.image.width * value.image.height * 4
    }

    private fun budgetBytes(): Int =
        (Runtime.getRuntime().maxMemory() / 16).coerceIn(8L * 1024 * 1024, 24L * 1024 * 1024).toInt()

    private fun key(@DrawableRes res: Int, sample: Int) = "$res@$sample"

    fun peek(@DrawableRes res: Int, sample: Int): ShoeArt? = memory.get(key(res, sample))

    /** 다시 불러오기 — 그 그림의 사본을 버린다 */
    fun forget(@DrawableRes res: Int) {
        listOf(1, 2).forEach { memory.remove(key(res, it)) }
    }

    /** 한 장 읽기. 읽지 못하면 null — 부르는 쪽은 실패로 보인다 */
    suspend fun load(context: Context, @DrawableRes res: Int, sample: Int): ShoeArt? {
        peek(res, sample)?.let { return it }
        return withContext(Dispatchers.IO) {
            val art = try {
                decode(context, res, sample)
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (_: Exception) {
                null
            } catch (_: OutOfMemoryError) {
                null
            }
            art?.also { memory.put(key(res, sample), it) }
        }
    }

    private fun decode(context: Context, @DrawableRes res: Int, sample: Int): ShoeArt? {
        val options = BitmapFactory.Options().apply {
            inSampleSize = sample.coerceAtLeast(1)
            inPreferredConfig = Bitmap.Config.ARGB_8888
            inScaled = false
        }
        val bitmap = BitmapFactory.decodeResource(context.resources, res, options) ?: return null
        val visible = visibleBounds(bitmap) ?: return null
        return ShoeArt(bitmap.asImageBitmap(), visible)
    }

    /** 알파가 조금이라도 있는 픽셀의 경계 — 가장자리 반투명(그림자 번짐)은 빼고 신발 몸만 */
    private fun visibleBounds(bitmap: Bitmap): IntRect? {
        val width = bitmap.width
        val row = IntArray(width)
        var top = -1
        var bottom = -1
        var left = width
        var right = -1
        for (y in 0 until bitmap.height) {
            bitmap.getPixels(row, 0, width, 0, y, width, 1)
            var any = false
            for (x in 0 until width) {
                if ((row[x] ushr 24) > ALPHA_FLOOR) {
                    any = true
                    if (x < left) left = x
                    if (x > right) right = x
                }
            }
            if (any) {
                if (top < 0) top = y
                bottom = y
            }
        }
        return if (top < 0) null else IntRect(left, top, right + 1, bottom + 1)
    }

    private const val ALPHA_FLOOR = 24
}

/** 큰 무대는 원본 그대로, 목록 칸은 반으로 줄여 읽는다 */
const val ShoeArtFull = 1
const val ShoeArtThumb = 2

/**
 * 이 켤레의 그림을 읽는다. [attempt] 를 올리면 다시 읽는다(이미지 다시 불러오기).
 * 이미 읽어 둔 그림이면 첫 그림부터 바로 보인다(깜박이지 않게).
 */
@Composable
fun rememberShoeArt(sneaker: Sneaker, sample: Int, attempt: Int = 0): State<ShoeArtLoad> =
    rememberShoeArt(sneakerArtRes(sneaker), sample, attempt)

/**
 * [res] 가 null 이면(이 앱에 그림이 없는 모델) 바로 실패. 그림 · 크기 · 다시 읽기가 바뀌면 그 자리에서 새 상태로 시작한다
 * (앞 그림을 한 프레임도 이어 보이지 않게) — 읽어 둔 그림이면 곧바로 Ready, 아니면 Loading 뒤에 읽는다.
 */
@Composable
fun rememberShoeArt(@DrawableRes res: Int?, sample: Int, attempt: Int = 0): State<ShoeArtLoad> {
    val context = LocalContext.current.applicationContext
    val load = remember(res, sample, attempt) { mutableStateOf(cachedLoad(res, sample)) }
    LaunchedEffect(load) {
        if (res != null && load.value !is ShoeArtLoad.Ready) {
            load.value = ShoeArtCache.load(context, res, sample)?.let { ShoeArtLoad.Ready(it) } ?: ShoeArtLoad.Failed
        }
    }
    return load
}

private fun cachedLoad(res: Int?, sample: Int): ShoeArtLoad = when {
    res == null -> ShoeArtLoad.Failed
    else -> ShoeArtCache.peek(res, sample)?.let { ShoeArtLoad.Ready(it) } ?: ShoeArtLoad.Loading
}

/** 그 켤레 그림을 미리 읽는다 — 신발 탭이 이름과 그림을 함께 바꾸려고(이름만 앞서 바뀌지 않게) */
suspend fun preloadShoeArt(context: Context, sneaker: Sneaker, sample: Int = ShoeArtFull) {
    sneakerArtRes(sneaker)?.let { ShoeArtCache.load(context.applicationContext, it, sample) }
}

/** 다시 불러오기 — 이 켤레 그림의 사본을 버린다(다음 [rememberShoeArt] 가 새로 읽는다) */
fun forgetShoeArt(sneaker: Sneaker) {
    sneakerArtRes(sneaker)?.let(ShoeArtCache::forget)
}

// ── 받침 ───────────────────────────────────────────────────────

/** 시안 좌표(390 × 420) 중 무대 상자가 덮는 부분 — 위쪽 130 까지는 조명만 번지므로 상자 밖에 그린다 */
private const val StageWidth = 390f
private const val StageTop = 130f
private const val StageHeight = 420f - StageTop

/** 무대 상자의 가로 : 세로 */
const val ShoeStageRatio = StageWidth / StageHeight

/** 받침 위 신발이 들어갈 자리 — 가운데 195, 보이는 바닥이 접촉 그림자 350 에, 폭 337 · 높이 215 안 */
private const val ShoeCenterX = 195f
private const val ShoeBottom = 350f
private const val ShoeMaxWidth = 337f
private const val ShoeMaxHeight = 215f

/** 무대 안 글자 자리 — 조회 중 문구(시안 13)는 받침 위 가운데, 이미지 오류(16)는 받침 윗면 */
val ShoeStageMessageAlignment = BiasAlignment(0f, -0.38f)
val ShoeStageErrorAlignment = BiasAlignment(0f, 0.55f)

@Immutable
private class PlinthColors(
    val air0: Color, val air65: Color,
    val shadow: Color, val shadowAlpha: Float,
    val front0: Color, val front1: Color,
    val top0: Color, val top47: Color, val top1: Color,
    val edge: Color,
    val contact: Color, val contactAlpha: Float,
)

/** 시안 값(어두운 톤) */
private val DarkPlinth = PlinthColors(
    air0 = Color(0x2977889C), air65 = Color(0x0F415773),
    shadow = Color.Black, shadowAlpha = .7f,
    front0 = Color(0xFF111C2C), front1 = Color(0xFF070D17),
    top0 = Color(0xFF243347), top47 = Color(0xFF1A273A), top1 = Color(0xFF101A2A),
    edge = Color(0x6674839A),
    contact = Color.Black, contactAlpha = .8f,
)

/** 밝은 테마 — 어두운 받침으로 덮지 않고 설정 v1 의 밝은 값(#E7EDF6 · #D8E0ED · #10203B)에서 고른 밝은 받침 */
private val LightPlinth = PlinthColors(
    air0 = Color(0x1A335EAB), air65 = Color(0x0A335EAB),
    shadow = Color(0xFF10203B), shadowAlpha = .22f,
    front0 = Color(0xFFD8E0ED), front1 = Color(0xFFC3CDDD),
    top0 = Color(0xFFF7F9FC), top47 = Color(0xFFEDF1F7), top1 = Color(0xFFE2E8F1),
    edge = Color(0xB3FFFFFF),
    contact = Color(0xFF10203B), contactAlpha = .32f,
)

/**
 * 보유 신발 전시 — 조명 → 받침 그림자 → 받침 앞면 → 윗면 → 윗면 가장자리 빛 → (접촉 그림자 → 신발).
 *
 * [sneaker] 가 null 이면 빈 받침(조회 중, 시안 13 — [overlay] 에 "불러오고 있어요"). 그림을 읽지 못하면 신발 자리를 비우고
 * [artFailed] 를 받침 위에 둔다(16). 켤레가 바뀌면 신발 층만 짧게(120ms) 바꾼다 — 받침은 그대로. 동작 줄이기면 바로.
 * 조명 · 받침은 장식이라 읽지 않는다. 무대가 목록 안에서 앞 글자 뒤로 번지게 하려면 부르는 쪽이 zIndex 를 낮춘다.
 */
@Composable
fun ShoeStage(
    sneaker: Sneaker?,
    modifier: Modifier = Modifier,
    artAttempt: Int = 0,
    overlay: @Composable BoxScope.() -> Unit = {},
    artFailed: @Composable BoxScope.() -> Unit = {},
) {
    val colors = if (StepUpColors.dark) DarkPlinth else LightPlinth
    val motion = LocalMotion.current
    Box(modifier.aspectRatio(ShoeStageRatio).drawBehind { drawPlinth(colors) }.testTag("shoe-stage")) {
        if (sneaker != null) {
            // 그림(모델)이 바뀔 때만 신발 층을 바꾼다 — 같은 켤레의 값(착용 · 레벨)이나 같은 모델의 다른 켤레는 그대로
            Crossfade(targetState = ArtSlot(sneakerArtRes(sneaker)), animationSpec = tween(motion.duration(120)), label = "shoeSwap") { slot ->
                val load by rememberShoeArt(slot.res, ShoeArtFull, artAttempt)
                Box(Modifier.fillMaxSize()) {
                    when (val current = load) {
                        is ShoeArtLoad.Ready -> ShoeOnPlinth(current.art, colors, Modifier.fillMaxSize().testTag("shoe-art"))
                        ShoeArtLoad.Failed -> Box(Modifier.fillMaxSize().testTag("shoe-art-failed"), content = artFailed)
                        ShoeArtLoad.Loading -> Unit
                    }
                }
            }
        }
        overlay()
    }
}

/** 신발 층의 열쇠 — 그림 자원 번호(null 이면 그림 없음) */
@Immutable
private data class ArtSlot(@DrawableRes val res: Int?)

/** 접촉 그림자와 신발 — 처음 읽은 그림은 짧게 나타난다(동작 줄이기면 바로) */
@Composable
private fun ShoeOnPlinth(art: ShoeArt, colors: PlinthColors, modifier: Modifier) {
    val motion = LocalMotion.current
    val alpha = remember(art) { Animatable(if (motion.reduced) 1f else 0f) }
    LaunchedEffect(art) { alpha.animateTo(1f, tween(motion.duration(120))) }
    Canvas(modifier) {
        val s = size.width / StageWidth
        val dst = fitOnPlinth(art.visible, s)
        // 접촉 그림자 — 신발 폭에 맞춘 납작한 타원(시안 144 : 337)
        drawEllipseShadow(
            center = Offset(ShoeCenterX * s, (ShoeBottom - StageTop) * s),
            rx = dst.width * 0.43f, ry = 14f * s,
            color = colors.contact, alpha = colors.contactAlpha * alpha.value,
        )
        drawVisible(art, dst, alpha.value)
    }
}

/** 보이는 부분을 받침 위 자리에 — 비율 그대로, 폭 337 · 높이 215 안, 가운데, 바닥이 350 */
private fun fitOnPlinth(visible: IntRect, s: Float): Rect {
    val scale = min(ShoeMaxWidth * s / visible.width, ShoeMaxHeight * s / visible.height)
    val width = visible.width * scale
    val height = visible.height * scale
    val bottom = (ShoeBottom - StageTop) * s
    val left = ShoeCenterX * s - width / 2f
    return Rect(left, bottom - height, left + width, bottom)
}

private fun DrawScope.drawVisible(art: ShoeArt, dst: Rect, alpha: Float) {
    drawImage(
        image = art.image,
        srcOffset = IntOffset(art.visible.left, art.visible.top),
        srcSize = IntSize(art.visible.width, art.visible.height),
        dstOffset = IntOffset(dst.left.roundToInt(), dst.top.roundToInt()),
        dstSize = IntSize(dst.width.roundToInt().coerceAtLeast(1), dst.height.roundToInt().coerceAtLeast(1)),
        alpha = alpha,
        filterQuality = FilterQuality.High,
    )
}

/** 시안 shoe-stage.svg 의 display-plinth 그대로 — 조명 · 받침 그림자 · 앞면 · 윗면 · 가장자리 빛 */
private fun DrawScope.drawPlinth(c: PlinthColors) {
    val s = size.width / StageWidth
    fun x(v: Float) = v * s
    fun y(v: Float) = (v - StageTop) * s

    // 조명 — 무대 상자 위(앞의 글자 뒤)까지 번진다. 원형 그라데이션을 거의 원인 타원에 그린다
    val air = Offset(x(187f), y(233f))
    drawOval(
        brush = Brush.radialGradient(0f to c.air0, 0.65f to c.air65, 1f to Color.Transparent, center = air, radius = 205f * s),
        topLeft = Offset(air.x - 205f * s, air.y - 196f * s),
        size = Size(410f * s, 392f * s),
    )
    // 받침 그림자
    drawEllipseShadow(Offset(x(195f), y(390f)), rx = 187f * s, ry = 24f * s, color = c.shadow, alpha = c.shadowAlpha)
    // 앞면(두께)
    val front = Path().apply {
        moveTo(x(12f), y(329f))
        cubicTo(x(12f), y(259f), x(378f), y(259f), x(378f), y(329f))
        lineTo(x(378f), y(340f))
        cubicTo(x(378f), y(414f), x(12f), y(414f), x(12f), y(340f))
        close()
    }
    drawPath(front, Brush.verticalGradient(listOf(c.front0, c.front1), startY = y(276f), endY = y(396f)))
    // 윗면
    drawOval(
        brush = Brush.linearGradient(
            0f to c.top0, 0.47f to c.top47, 1f to c.top1,
            start = Offset(x(12f), y(274f)), end = Offset(x(378f), y(384f)),
        ),
        topLeft = Offset(x(12f), y(274f)),
        size = Size(366f * s, 110f * s),
    )
    // 윗면 앞 가장자리 빛
    val edge = Path().apply {
        moveTo(x(13f), y(329f))
        cubicTo(x(13f), y(400f), x(377f), y(400f), x(377f), y(329f))
    }
    drawPath(
        edge,
        Brush.horizontalGradient(
            0f to c.edge.copy(alpha = 0f), 0.36f to c.edge, 1f to c.edge.copy(alpha = 0f),
            startX = x(13f), endX = x(377f),
        ),
        style = Stroke(width = max(1f, 0.7f * s)),
    )
}

/** 가로로 긴 부드러운 그림자 — 원형 그라데이션을 세로로 눌러 타원으로 */
private fun DrawScope.drawEllipseShadow(center: Offset, rx: Float, ry: Float, color: Color, alpha: Float) {
    if (rx <= 0f || ry <= 0f) return
    withTransform({ scale(1f, ry / rx, pivot = center) }) {
        drawCircle(
            brush = Brush.radialGradient(0f to color.copy(alpha = 0.9f), 1f to color.copy(alpha = 0f), center = center, radius = rx),
            radius = rx,
            center = center,
            alpha = alpha.coerceIn(0f, 1f),
        )
    }
}

// ── 작은 그림 ─────────────────────────────────────────────────

/**
 * 목록 칸 · 켤레 줄의 작은 신발 — 보이는 부분만 칸에 맞춘다(비율 그대로, 가운데).
 * 그림을 읽지 못하면 다른 신발 대신 작은 그림 아이콘을 둔다.
 */
@Composable
fun ShoeArtThumbnail(sneaker: Sneaker, modifier: Modifier = Modifier, failedTint: Color = Color.Gray) {
    val load by rememberShoeArt(sneaker, ShoeArtThumb)
    Box(modifier, contentAlignment = Alignment.Center) {
        when (val current = load) {
            is ShoeArtLoad.Ready -> Canvas(Modifier.fillMaxSize()) {
                val visible = current.art.visible
                val scale = min(size.width / visible.width, size.height / visible.height)
                val width = visible.width * scale
                val height = visible.height * scale
                val left = (size.width - width) / 2f
                val top = (size.height - height) / 2f
                drawVisible(current.art, Rect(left, top, left + width, top + height), 1f)
            }
            ShoeArtLoad.Failed -> Icon(Icons.Outlined.Image, contentDescription = null, tint = failedTint, modifier = Modifier.size(24.dp))
            ShoeArtLoad.Loading -> Unit
        }
    }
}
