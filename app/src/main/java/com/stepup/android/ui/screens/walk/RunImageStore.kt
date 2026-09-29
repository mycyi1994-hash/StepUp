package com.stepup.android.ui.screens.walk

import android.Manifest
import android.content.ContentValues
import android.content.Context
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.media.MediaScannerConnection
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import androidx.annotation.RequiresApi
import androidx.core.content.ContextCompat
import java.io.File
import java.io.FileOutputStream

/**
 * 러닝 결과 그림을 사진(Pictures/StepUp)에 넣는다 — 러닝 완료의 "이미지 저장"(2026-09-29 전달본 03).
 * Android 10 부터는 권한 없이 사진 모음에 넣는다. 9 이하만 저장소 쓰기 권한이 필요하다(매니페스트 maxSdkVersion 28).
 */
internal object RunImageStore {
    private const val FOLDER = "StepUp"

    fun needsPermission(context: Context): Boolean =
        Build.VERSION.SDK_INT < Build.VERSION_CODES.Q &&
            ContextCompat.checkSelfPermission(context, Manifest.permission.WRITE_EXTERNAL_STORAGE) !=
            PackageManager.PERMISSION_GRANTED

    /** 넣었으면 true. 실패하면 반쯤 쓴 항목을 남기지 않는다 */
    fun save(context: Context, bitmap: Bitmap, name: String): Boolean {
        val file = "$name.png"
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) saveScoped(context, bitmap, file) else saveLegacy(context, bitmap, file)
    }

    @RequiresApi(Build.VERSION_CODES.Q)
    private fun saveScoped(context: Context, bitmap: Bitmap, file: String): Boolean {
        val resolver = context.contentResolver
        val values = ContentValues().apply {
            put(MediaStore.Images.Media.DISPLAY_NAME, file)
            put(MediaStore.Images.Media.MIME_TYPE, "image/png")
            put(MediaStore.Images.Media.RELATIVE_PATH, Environment.DIRECTORY_PICTURES + "/" + FOLDER)
            put(MediaStore.Images.Media.IS_PENDING, 1)
        }
        val uri = resolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values) ?: return false
        val written = runCatching {
            resolver.openOutputStream(uri)?.use { bitmap.compress(Bitmap.CompressFormat.PNG, 100, it) } ?: false
        }.getOrDefault(false)
        if (!written) {
            runCatching { resolver.delete(uri, null, null) }
            return false
        }
        val published = ContentValues().apply { put(MediaStore.Images.Media.IS_PENDING, 0) }
        return runCatching { resolver.update(uri, published, null, null) > 0 }.getOrDefault(false)
    }

    @Suppress("DEPRECATION")
    private fun saveLegacy(context: Context, bitmap: Bitmap, file: String): Boolean {
        val dir = File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES), FOLDER)
        if (!dir.exists() && !dir.mkdirs()) return false
        val target = File(dir, file)
        val written = runCatching {
            FileOutputStream(target).use { bitmap.compress(Bitmap.CompressFormat.PNG, 100, it) }
        }.getOrDefault(false)
        if (!written) {
            target.delete()
            return false
        }
        MediaScannerConnection.scanFile(context, arrayOf(target.absolutePath), arrayOf("image/png"), null)
        return true
    }
}
