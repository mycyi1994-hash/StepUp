@file:OptIn(kotlinx.cinterop.ExperimentalForeignApi::class, kotlin.io.encoding.ExperimentalEncodingApi::class, kotlinx.cinterop.BetaInteropApi::class)

package com.stepup.ui

import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asSkiaBitmap
import androidx.compose.ui.graphics.toComposeImageBitmap
import com.stepup.android.data.remote.ServerResult
import com.stepup.android.data.repo.ChatImageCodec
import com.stepup.android.data.repo.ChatPhoto
import com.stepup.android.data.repo.CrewPhotoMemory
import com.stepup.android.domain.ByteLruCache
import com.stepup.android.domain.randomUuid
import kotlin.io.encoding.Base64
import kotlinx.cinterop.addressOf
import kotlinx.cinterop.usePinned
import org.jetbrains.skia.EncodedImageFormat
import org.jetbrains.skia.Image
import org.jetbrains.skia.Rect
import org.jetbrains.skia.Surface
import platform.Foundation.NSData
import platform.Foundation.create
import platform.Foundation.NSFileManager
import platform.Foundation.NSSearchPathForDirectoriesInDomains
import platform.Foundation.NSCachesDirectory
import platform.Foundation.NSDocumentDirectory
import platform.Foundation.NSUserDomainMask
import platform.Foundation.dataWithContentsOfFile
import platform.Foundation.writeToFile
import platform.posix.memcpy

/** iOS 앱 폴더 — 문서(지우지 않는 것) · 캐시(다시 받을 수 있는 것) */
object IosDirs {
    val documents: String by lazy { (NSSearchPathForDirectoriesInDomains(NSDocumentDirectory, NSUserDomainMask, true).first() as String) }
    val caches: String by lazy { (NSSearchPathForDirectoriesInDomains(NSCachesDirectory, NSUserDomainMask, true).first() as String) }
}

internal fun readBytes(path: String): ByteArray? {
    val data = NSData.dataWithContentsOfFile(path) ?: return null
    val size = data.length.toInt()
    if (size == 0) return ByteArray(0)
    return ByteArray(size).also { out -> out.usePinned { memcpy(it.addressOf(0), data.bytes, data.length) } }
}

internal fun writeBytes(path: String, bytes: ByteArray): Boolean {
    NSFileManager.defaultManager.createDirectoryAtPath(path.substringBeforeLast('/'), withIntermediateDirectories = true, attributes = null, error = null)
    val data = bytes.usePinned { platform.Foundation.NSData.create(bytes = it.addressOf(0), length = bytes.size.toULong()) }
    return data.writeToFile(path, atomically = true)
}

/** 화면 그림 → JPEG(긴 변 [maxSide] 까지 줄여서) */
internal fun ImageBitmap.jpeg(maxSide: Int, quality: Int): Pair<ByteArray, Pair<Int, Int>>? {
    val image = Image.makeFromBitmap(asSkiaBitmap())
    val scale = minOf(1f, maxSide.toFloat() / maxOf(width, height))
    val w = (width * scale).toInt().coerceAtLeast(1)
    val h = (height * scale).toInt().coerceAtLeast(1)
    val scaled = if (scale >= 1f) image else Surface.makeRasterN32Premul(w, h).also {
        it.canvas.drawImageRect(image, Rect.makeWH(w.toFloat(), h.toFloat()))
    }.makeImageSnapshot()
    val data = scaled.encodeToData(EncodedImageFormat.JPEG, quality) ?: return null
    return data.bytes to (w to h)
}

internal fun decodeImage(bytes: ByteArray): ImageBitmap? = runCatching { Image.makeFromEncoded(bytes).toComposeImageBitmap() }.getOrNull()

/** iOS 채팅 사진 — 안드로이드 AndroidChatImageCodec 과 같은 줄이기 · 화질 단계(긴 변 −240 · 화질 84 → 46) */
object IosChatImageCodec : ChatImageCodec<ImageBitmap> {
    override fun byteCount(img: ImageBitmap): Int = img.width * img.height * 4
    override fun decodeFile(path: String): ImageBitmap? = readBytes(path)?.let(::decodeImage)
    override fun decodeBase64(data: String): ImageBitmap? = runCatching { Base64.Mime.decode(data) }.getOrNull()?.let(::decodeImage)

    override fun encode(source: ImageBitmap, dir: String, maxSide: Int, maxBase64: Int): ChatPhoto? {
        var side = maxSide
        while (side >= 480) {
            var quality = 84
            while (quality >= 46) {
                val (bytes, size) = source.jpeg(side, quality) ?: return null
                val text = Base64.encode(bytes)
                if (text.length <= maxBase64) {
                    val path = "$dir/photo-${randomUuid()}.jpg"
                    if (!writeBytes(path, bytes)) return null
                    return ChatPhoto(text, path, size.first, size.second)
                }
                quality -= 12
            }
            side -= 240
        }
        return null
    }

    override fun clearDir(dir: String) {
        val fm = NSFileManager.defaultManager
        (fm.contentsOfDirectoryAtPath(dir, error = null) as? List<*>)?.forEach { fm.removeItemAtPath("$dir/$it", error = null) }
    }
}

/** iOS 크루 대표 사진 — 서버(crew_images)에서 받아 메모리에 둔다(안드로이드 CrewImageStore 와 같은 판 열쇠) */
class IosCrewImages(private val fetch: suspend (String) -> ServerResult<String>) : CrewPhotoMemory {
    private val memory = ByteLruCache<String, ImageBitmap>(12 * 1024 * 1024) { it.width * it.height * 4 }

    suspend fun image(crewId: String, version: Int): ImageBitmap? {
        val key = "$crewId-$version"
        memory.get(key)?.let { return it }
        val data = (fetch(crewId) as? ServerResult.Ok)?.value?.takeIf { it.isNotEmpty() } ?: return null
        val decoded = runCatching { Base64.Mime.decode(data) }.getOrNull()?.let(::decodeImage) ?: return null
        memory.put(key, decoded)
        return decoded
    }

    /** 크루 대표 사진 올릴 모양 — 안드로이드 CrewPhotos.encode 와 같은 화질 단계(86 → 40) · 크기(512) · 한도(base64 180,000자) */
    override fun encodeFile(path: String): String? {
        val image = readBytes(path)?.let(::decodeImage) ?: return null
        var quality = 86
        while (quality >= 40) {
            val (bytes, _) = image.jpeg(CREW_SIZE, quality) ?: return null
            val text = Base64.encode(bytes)
            if (text.length <= CREW_MAX_BASE64) return text
            quality -= 12
        }
        return null
    }

    override fun rememberFile(crewId: String, version: Int, path: String) {
        readBytes(path)?.let(::decodeImage)?.let { memory.put("$crewId-$version", it) }
    }

    companion object {
        private const val CREW_SIZE = 512
        private const val CREW_MAX_BASE64 = 180_000
    }
}
