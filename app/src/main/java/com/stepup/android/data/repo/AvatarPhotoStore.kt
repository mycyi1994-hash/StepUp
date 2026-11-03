package com.stepup.android.data.repo

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.media.ExifInterface
import java.io.File
import java.io.InputStream
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * 갤러리 사진 파일 — 고른 사진을 긴 변 512px 이하 JPEG(품질 90)로 줄여 앱 저장소에 둔다(기존 정책 그대로), 사진의 방향은 바로 세운다.
 *
 * 새 사진은 임시 파일에 먼저 만들고 다시 읽어 볼 수 있을 때만 기존 파일과 바꾼다. 설정(아바타 번호 · 리비전, [commit])을
 * 적지 못하면 기존 파일을 되돌린다 — 실패하면 기존 사진 · 번호 · 리비전이 그대로다. 파일을 바꾸고 설정을 적는 동안은
 * 취소되지 않는다(반쯤 바뀐 채로 남지 않게).
 */
class AvatarPhotoStore(
    private val dir: File,
    private val fileName: String,
    private val commit: suspend () -> Unit,
    private val maxSide: Int = 512,
) {
    suspend fun save(open: () -> InputStream?): Boolean = withContext(Dispatchers.IO) {
        val target = File(dir, fileName)
        val temp = File(dir, "$fileName.new")
        val backup = File(dir, "$fileName.old")
        try {
            val bitmap = decode(open) ?: return@withContext false
            val written = temp.outputStream().use { bitmap.compress(Bitmap.CompressFormat.JPEG, 90, it) }
            if (!written || !readable(temp)) return@withContext false
            withContext(NonCancellable) { swap(target, temp, backup) }
        } finally {
            temp.delete()
        }
    }

    private suspend fun swap(target: File, temp: File, backup: File): Boolean {
        backup.delete()
        val hadOld = target.exists()
        if (hadOld && !target.renameTo(backup)) return false
        if (!temp.renameTo(target)) {
            if (hadOld) backup.renameTo(target)
            return false
        }
        return try {
            commit()
            backup.delete()
            true
        } catch (_: Exception) {
            target.delete()
            if (hadOld) backup.renameTo(target)
            false
        }
    }

    /** 크기만 먼저 읽어 줄여 읽고(큰 사진도 메모리를 넘기지 않게), 방향을 세우고, 긴 변을 [maxSide] 이하로 */
    private fun decode(open: () -> InputStream?): Bitmap? {
        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        // 크기만 읽을 때 decodeStream 은 늘 null 을 돌려준다 — 열었는지는 스트림으로, 읽었는지는 크기로 본다
        (open() ?: return null).use { BitmapFactory.decodeStream(it, null, bounds) }
        if (bounds.outWidth <= 0 || bounds.outHeight <= 0) return null
        var sample = 1
        while (maxOf(bounds.outWidth, bounds.outHeight) / (sample * 2) >= maxSide) sample *= 2
        val raw = open()?.use { BitmapFactory.decodeStream(it, null, BitmapFactory.Options().apply { inSampleSize = sample }) } ?: return null
        val rotation = runCatching {
            open()?.use {
                when (ExifInterface(it).getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL)) {
                    ExifInterface.ORIENTATION_ROTATE_90 -> 90f
                    ExifInterface.ORIENTATION_ROTATE_180 -> 180f
                    ExifInterface.ORIENTATION_ROTATE_270 -> 270f
                    else -> 0f
                }
            } ?: 0f
        }.getOrDefault(0f)
        val scale = minOf(maxSide.toFloat() / raw.width, maxSide.toFloat() / raw.height, 1f)
        if (scale >= 1f && rotation == 0f) return raw
        val matrix = Matrix().apply {
            postScale(scale, scale)
            postRotate(rotation)
        }
        return Bitmap.createBitmap(raw, 0, 0, raw.width, raw.height, matrix, true)
    }

    private fun readable(file: File): Boolean {
        val check = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        BitmapFactory.decodeFile(file.path, check)
        return check.outWidth > 0 && check.outHeight > 0
    }
}
