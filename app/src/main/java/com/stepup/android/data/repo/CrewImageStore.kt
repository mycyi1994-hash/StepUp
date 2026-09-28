package com.stepup.android.data.repo

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.ImageDecoder
import android.graphics.Matrix
import android.net.Uri
import android.os.Build
import android.util.Base64
import android.util.LruCache
import com.stepup.android.data.remote.ServerResult
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileNotFoundException
import java.util.concurrent.ConcurrentHashMap
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext

/**
 * 크루 대표 사진 — 서버(crew_images)에서 받아 이 폰에 둔다. 사진 버전(image_ver)이 오르면 새로 받는다.
 * 받지 못하면 null — 화면은 크루 이름이 들어간 기본 이미지를 보인다(모든 크루를 같은 그림으로 바꾸지 않는다).
 */
class CrewImageStore(
    private val fetch: suspend (crewId: String) -> ServerResult<String>,
    private val dir: File,
) {
    private val memory = object : LruCache<String, Bitmap>(MEMORY_BYTES) {
        override fun sizeOf(key: String, value: Bitmap): Int = value.byteCount
    }
    private val locks = ConcurrentHashMap<String, Mutex>()

    /** 이 크루의 [version] 사진. 없거나 못 받으면 null */
    suspend fun bitmap(crewId: String, version: Int): Bitmap? {
        val key = "$crewId-$version"
        memory.get(key)?.let { return it }
        val lock = locks.getOrPut(key) { Mutex() }
        return lock.withLock {
            memory.get(key)?.let { return@withLock it }
            val file = File(dir, "$key.jpg")
            if (!file.exists()) {
                val data = (fetch(crewId) as? ServerResult.Ok)?.value?.takeIf { it.isNotEmpty() } ?: return@withLock null
                val bytes = runCatching { Base64.decode(data, Base64.DEFAULT) }.getOrNull() ?: return@withLock null
                withContext(Dispatchers.IO) {
                    dir.mkdirs()
                    // 같은 크루의 예전 버전은 지운다
                    dir.listFiles { f -> f.name.startsWith("$crewId-") && f.name != file.name }?.forEach { it.delete() }
                    val temp = File(dir, "$key.tmp")
                    temp.writeBytes(bytes)
                    temp.renameTo(file)
                }
            }
            val decoded = withContext(Dispatchers.IO) { CrewPhotos.decodeFile(file, CrewPhotos.SIZE) }
            if (decoded == null) {
                file.delete()
                return@withLock null
            }
            memory.put(key, decoded)
            decoded
        }
    }

    /** 방금 올린 사진을 받지 않고 바로 쓴다 */
    fun remember(crewId: String, version: Int, bitmap: Bitmap) {
        memory.put("$crewId-$version", bitmap)
    }

    companion object {
        private const val MEMORY_BYTES = 12 * 1024 * 1024
    }
}

/** 사진 고르기 · 자르기 · 올리기 */
object CrewPhotos {
    /** 올리는 정사각형 한 변(px) */
    const val SIZE = 512

    /** 서버 칸(200,000자)보다 넉넉히 작게 */
    private const val MAX_BASE64 = 180_000

    /** 고른 사진이 읽히지 않거나 사진 접근이 막혔다 */
    sealed interface Loaded {
        data class Ok(val bitmap: Bitmap) : Loaded

        /** 사진을 읽을 수 없다 — 다른 사진 · 기본 이미지로(29) */
        data object Broken : Loaded

        /** 사진 접근이 막혔다 — 설정 · 기본 이미지로(69) */
        data object Denied : Loaded
    }

    /** OS 사진 선택기가 준 사진을 자르기 화면이 쓸 크기로 읽는다(긴 변 [maxSide], 사진의 회전을 반영) */
    suspend fun load(context: Context, uri: Uri, maxSide: Int = 2048): Loaded = withContext(Dispatchers.IO) {
        try {
            val bitmap = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                ImageDecoder.decodeBitmap(ImageDecoder.createSource(context.contentResolver, uri)) { decoder, info, _ ->
                    decoder.allocator = ImageDecoder.ALLOCATOR_SOFTWARE
                    val longest = maxOf(info.size.width, info.size.height)
                    if (longest > maxSide) {
                        val scale = maxSide.toFloat() / longest
                        decoder.setTargetSize((info.size.width * scale).toInt().coerceAtLeast(1), (info.size.height * scale).toInt().coerceAtLeast(1))
                    }
                }
            } else {
                legacyLoad(context, uri, maxSide)
            }
            if (bitmap == null || bitmap.width < 8 || bitmap.height < 8) Loaded.Broken else Loaded.Ok(bitmap)
        } catch (_: SecurityException) {
            Loaded.Denied
        } catch (_: FileNotFoundException) {
            Loaded.Broken
        } catch (_: Exception) {
            Loaded.Broken
        } catch (_: OutOfMemoryError) {
            Loaded.Broken
        }
    }

    private fun legacyLoad(context: Context, uri: Uri, maxSide: Int): Bitmap? {
        val resolver = context.contentResolver
        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        resolver.openInputStream(uri)?.use { BitmapFactory.decodeStream(it, null, bounds) }
        if (bounds.outWidth <= 0 || bounds.outHeight <= 0) return null
        var sample = 1
        while (maxOf(bounds.outWidth, bounds.outHeight) / (sample * 2) >= maxSide) sample *= 2
        val decoded = resolver.openInputStream(uri)?.use {
            BitmapFactory.decodeStream(it, null, BitmapFactory.Options().apply { inSampleSize = sample })
        } ?: return null
        val degrees = resolver.openInputStream(uri)?.use { stream ->
            when (android.media.ExifInterface(stream).getAttributeInt(android.media.ExifInterface.TAG_ORIENTATION, 1)) {
                android.media.ExifInterface.ORIENTATION_ROTATE_90 -> 90f
                android.media.ExifInterface.ORIENTATION_ROTATE_180 -> 180f
                android.media.ExifInterface.ORIENTATION_ROTATE_270 -> 270f
                else -> 0f
            }
        } ?: 0f
        if (degrees == 0f) return decoded
        return Bitmap.createBitmap(decoded, 0, 0, decoded.width, decoded.height, Matrix().apply { postRotate(degrees) }, true)
    }

    /**
     * 자르기 — 원본 사진 좌표의 정사각형([left], [top], 한 변 [side])을 [SIZE] 로.
     * 화면이 보여 준 틀 그대로 자른다(틀 밖은 버린다).
     */
    fun crop(source: Bitmap, left: Float, top: Float, side: Float): Bitmap {
        val out = Bitmap.createBitmap(SIZE, SIZE, Bitmap.Config.ARGB_8888)
        val canvas = android.graphics.Canvas(out)
        canvas.drawColor(android.graphics.Color.BLACK)
        val scale = SIZE / side
        val matrix = Matrix().apply {
            postTranslate(-left, -top)
            postScale(scale, scale)
        }
        canvas.drawBitmap(source, matrix, android.graphics.Paint(android.graphics.Paint.FILTER_BITMAP_FLAG))
        return out
    }

    /** 올릴 모양(JPEG base64). 너무 크면 화질을 낮춘다 */
    fun encode(bitmap: Bitmap): String? {
        var quality = 86
        while (quality >= 40) {
            val bytes = ByteArrayOutputStream().use { out ->
                bitmap.compress(Bitmap.CompressFormat.JPEG, quality, out)
                out.toByteArray()
            }
            val text = Base64.encodeToString(bytes, Base64.NO_WRAP)
            if (text.length <= MAX_BASE64) return text
            quality -= 12
        }
        return null
    }

    fun save(bitmap: Bitmap, file: File): Boolean = runCatching {
        file.parentFile?.mkdirs()
        val temp = File(file.parentFile, file.name + ".tmp")
        temp.outputStream().use { bitmap.compress(Bitmap.CompressFormat.JPEG, 92, it) }
        temp.renameTo(file)
    }.getOrDefault(false)

    fun decodeFile(file: File, maxSide: Int = SIZE): Bitmap? = runCatching {
        if (!file.exists()) return null
        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        BitmapFactory.decodeFile(file.path, bounds)
        if (bounds.outWidth <= 0) return null
        var sample = 1
        while (maxOf(bounds.outWidth, bounds.outHeight) / (sample * 2) >= maxSide) sample *= 2
        BitmapFactory.decodeFile(file.path, BitmapFactory.Options().apply { inSampleSize = sample })
    }.getOrNull()
}
