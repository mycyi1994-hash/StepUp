package com.stepup.android.ui.components

import android.Manifest
import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.media.MediaScannerConnection
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.RequiresApi
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import com.stepup.android.core.AndroidHost
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream

/** 그림을 캐시(shared/)에 PNG 로 두고 공유 창을 연다 — 큰 그림의 저장은 화면 스레드 밖에서(누르는 순간 멈칫하지 않게) */
actual suspend fun shareImage(image: ImageBitmap, text: String, chooserTitle: String?) {
    val context = AndroidHost.app
    val bitmap = image.asAndroidBitmap()
    val file = withContext(Dispatchers.IO) {
        val dir = File(context.cacheDir, "shared").apply { mkdirs() }
        File(dir, "stepup-run.png").also { out -> out.outputStream().use { bitmap.compress(Bitmap.CompressFormat.PNG, 100, it) } }
    }
    val uri = FileProvider.getUriForFile(context, "${context.packageName}.share", file)
    val send = Intent(Intent.ACTION_SEND).apply {
        type = "image/png"
        putExtra(Intent.EXTRA_STREAM, uri)
        putExtra(Intent.EXTRA_TEXT, text)
        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
    }
    context.startActivity(Intent.createChooser(send, chooserTitle).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
}

/**
 * 러닝 결과 그림을 사진(Pictures/StepUp)에 넣는다(예전 RunImageStore 그대로).
 * Android 10 부터는 권한 없이 사진 모음에 넣는다. 9 이하만 저장소 쓰기 권한을 먼저 묻는다(매니페스트 maxSdkVersion 28).
 */
@Composable
actual fun rememberRunImageSaver(onResult: (Boolean) -> Unit): (image: ImageBitmap, name: String) -> Unit {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val callback by rememberUpdatedState(onResult)
    var waiting by remember { mutableStateOf<Pair<ImageBitmap, String>?>(null) }
    fun save(image: ImageBitmap, name: String) {
        scope.launch {
            val saved = withContext(Dispatchers.IO) { runCatching { RunImageStore.save(context, image.asAndroidBitmap(), name) }.getOrDefault(false) }
            callback(saved)
        }
    }
    val permission = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        val job = waiting
        waiting = null
        if (granted && job != null) save(job.first, job.second) else callback(false)
    }
    return remember(permission) {
        { image, name ->
            if (RunImageStore.needsPermission(context)) {
                waiting = image to name
                permission.launch(Manifest.permission.WRITE_EXTERNAL_STORAGE)
            } else {
                save(image, name)
            }
        }
    }
}

internal object RunImageStore {
    private const val FOLDER = "StepUp"

    fun needsPermission(context: Context): Boolean =
        Build.VERSION.SDK_INT < Build.VERSION_CODES.Q &&
            ContextCompat.checkSelfPermission(context, Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED

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
        val written = runCatching { FileOutputStream(target).use { bitmap.compress(Bitmap.CompressFormat.PNG, 100, it) } }.getOrDefault(false)
        if (!written) {
            target.delete()
            return false
        }
        MediaScannerConnection.scanFile(context, arrayOf(target.absolutePath), arrayOf("image/png"), null)
        return true
    }
}
