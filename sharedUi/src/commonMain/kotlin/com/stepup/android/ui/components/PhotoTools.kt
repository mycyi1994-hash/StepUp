package com.stepup.android.ui.components

import androidx.compose.runtime.Composable
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Canvas
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.FilterQuality
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.Paint
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import kotlin.io.encoding.Base64
import kotlin.io.encoding.ExperimentalEncodingApi
import kotlin.math.roundToInt

/**
 * 사진 고르기 · 자르기 · 올리기(안드로이드 CrewPhotos 의 화면 쪽 자리, 2026-11-03 iOS C5) — 그림은 화면 그림(ImageBitmap).
 * 크기 · 화질 단계는 안드로이드와 같다(대표 사진 512 · JPEG 86 → 40 · base64 180,000자).
 */
object PhotoTools {
    const val SIZE = 512
    private const val MAX_BASE64 = 180_000

    sealed interface Loaded {
        data class Ok(val image: ImageBitmap) : Loaded
        /** 사진이 아니거나 깨졌다 */
        data object Broken : Loaded
        /** 사진에 접근할 수 없다(권한) */
        data object Denied : Loaded
    }

    /** 고른 사진 파일(rememberPhotoPicker 가 준 경로)을 읽는다 */
    fun load(path: String?): Loaded {
        if (path == null) return Loaded.Denied
        return decodeImageFile(path)?.let { Loaded.Ok(it) } ?: Loaded.Broken
    }

    /** 원본 좌표의 정사각형([left], [top], 한 변 [side])을 [SIZE] 로 — 틀 밖은 버리고 검은 바탕 */
    fun crop(source: ImageBitmap, left: Float, top: Float, side: Float): ImageBitmap {
        val out = ImageBitmap(SIZE, SIZE)
        val canvas = Canvas(out)
        canvas.drawRect(0f, 0f, SIZE.toFloat(), SIZE.toFloat(), Paint().apply { color = Color.Black })
        canvas.drawImageRect(
            source,
            srcOffset = IntOffset(left.roundToInt(), top.roundToInt()),
            srcSize = IntSize(side.roundToInt().coerceAtLeast(1), side.roundToInt().coerceAtLeast(1)),
            dstOffset = IntOffset.Zero,
            dstSize = IntSize(SIZE, SIZE),
            paint = Paint().apply { filterQuality = FilterQuality.High },
        )
        return out
    }

    /** 올릴 모양(JPEG base64). 너무 크면 화질을 낮춘다 */
    @OptIn(ExperimentalEncodingApi::class)
    fun encode(image: ImageBitmap): String? {
        var quality = 86
        while (quality >= 40) {
            val bytes = image.encodeJpeg(quality) ?: return null
            val text = Base64.encode(bytes)
            if (text.length <= MAX_BASE64) return text
            quality -= 12
        }
        return null
    }

    fun save(image: ImageBitmap, path: String): Boolean = image.encodeJpeg(92)?.let { writeFileBytes(path, it) } ?: false

    @Suppress("unused")
    private val unusedOffset = Offset.Zero to Size.Zero
}

/** 화면 그림 → JPEG */
expect fun ImageBitmap.encodeJpeg(quality: Int): ByteArray?

fun jpegOf(image: ImageBitmap, quality: Int): ByteArray? = image.encodeJpeg(quality)

/** 앱 안 사진 파일 → 화면 그림. 못 읽으면 null */
expect fun decodeImageFile(path: String): ImageBitmap?

expect fun writeFileBytes(path: String, bytes: ByteArray): Boolean

expect fun readFileBytes(path: String): ByteArray?

/**
 * 사진 고르기(안드로이드 PickVisualMedia 자리) — 고른 사진을 앱 캐시의 JPEG 파일로 두고 그 경로를 준다. 고르지 않으면 null.
 * 돌려받은 함수를 부르면 고르기 창이 열린다.
 */
@Composable
expect fun rememberPhotoPicker(onPicked: (String?) -> Unit): () -> Unit

/** 위치 권한 묻기(안드로이드 RequestMultiplePermissions 자리) — 허락했으면 true */
@Composable
expect fun rememberLocationPermissionRequest(onResult: (Boolean) -> Unit): () -> Unit

/** 카메라로 찍기(안드로이드 TakePicture 자리) — 찍은 사진은 앱 캐시의 JPEG 파일 경로, 그만두면 null. 카메라가 없으면 null 을 돌려준다 */
@Composable
expect fun rememberCameraCapture(onTaken: (String?) -> Unit): (() -> Unit)?
