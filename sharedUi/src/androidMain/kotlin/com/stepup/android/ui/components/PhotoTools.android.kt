package com.stepup.android.ui.components

import android.Manifest
import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.core.app.ActivityCompat
import androidx.core.content.FileProvider
import java.io.ByteArrayOutputStream
import java.io.File

actual fun ImageBitmap.encodeJpeg(quality: Int): ByteArray? = runCatching {
    ByteArrayOutputStream().use { out ->
        if (!asAndroidBitmap().compress(Bitmap.CompressFormat.JPEG, quality, out)) null else out.toByteArray()
    }
}.getOrNull()

actual fun decodeImageFile(path: String): ImageBitmap? = runCatching { BitmapFactory.decodeFile(path)?.asImageBitmap() }.getOrNull()

actual fun writeFileBytes(path: String, bytes: ByteArray): Boolean = runCatching {
    val file = File(path)
    file.parentFile?.mkdirs()
    val temp = File(file.parentFile, file.name + ".tmp")
    temp.writeBytes(bytes)
    temp.renameTo(file) || run { file.writeBytes(bytes); temp.delete(); true }
}.getOrDefault(false)

actual fun readFileBytes(path: String): ByteArray? = runCatching { File(path).takeIf { it.exists() }?.readBytes() }.getOrNull()

/** 고른 사진(content://)을 앱 캐시의 파일로 옮긴다 — 공용 화면은 파일 경로만 다룬다 */
private fun Context.copyToCache(uri: Uri, prefix: String): String? = runCatching {
    val dir = File(cacheDir, "picked").apply { mkdirs() }
    val out = File(dir, "$prefix-${System.currentTimeMillis()}.jpg")
    contentResolver.openInputStream(uri)?.use { input -> out.outputStream().use { input.copyTo(it) } } ?: return null
    out.path
}.getOrNull()

@Composable
actual fun rememberPhotoPicker(onPicked: (String?) -> Unit): () -> Unit {
    val context = LocalContext.current
    val callback by rememberUpdatedState(onPicked)
    val picker = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
        callback(uri?.let { context.copyToCache(it, "pick") })
    }
    return remember(picker) {
        {
            runCatching { picker.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)) }
                .onFailure { callback(null) }
        }
    }
}

/** 정확한 위치만 따로 물으면 Android 12+ 가 무시할 수 있다 — 둘을 함께 물어 OS 가 정확 · 대략을 고르게 한다 */
@Composable
actual fun rememberLocationPermissionRequest(onResult: (Boolean) -> Unit): () -> Unit {
    val callback by rememberUpdatedState(onResult)
    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { result ->
        callback(result.values.any { it })
    }
    return remember(launcher) {
        {
            runCatching {
                launcher.launch(arrayOf(Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION))
            }.onFailure { callback(false) }
        }
    }
}

/** 카메라 — 찍은 사진은 앱 캐시의 camera/ (FileProvider "<앱>.share" 의 camera 경로) */
@Composable
actual fun rememberCameraCapture(onTaken: (String?) -> Unit): (() -> Unit)? {
    val context = LocalContext.current
    val callback by rememberUpdatedState(onTaken)
    var pending by rememberSaveable { mutableStateOf<String?>(null) }
    val camera = rememberLauncherForActivityResult(ActivityResultContracts.TakePicture()) { ok ->
        val path = pending
        pending = null
        if (ok && path != null && File(path).length() > 0) callback(path) else {
            path?.let { File(it).delete() }
            callback(null)
        }
    }
    if (!context.packageManager.hasSystemFeature(android.content.pm.PackageManager.FEATURE_CAMERA_ANY)) return null
    return remember(camera) {
        launch@{
            val file = runCatching {
                File(context.cacheDir, "camera").apply { mkdirs() }.let { File(it, "chat-${System.currentTimeMillis()}.jpg") }
            }.getOrNull()
            val uri = file?.let { runCatching { FileProvider.getUriForFile(context, "${context.packageName}.share", it) }.getOrNull() }
            if (file == null || uri == null) {
                callback(null)
                return@launch
            }
            pending = file.path
            runCatching { camera.launch(uri) }.onFailure {
                pending = null
                callback(null)
            }
        }
    }
}

@Composable
actual fun rememberActivityPermissionLauncher(onResult: (Boolean) -> Unit): () -> Boolean {
    val callback by rememberUpdatedState(onResult)
    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { callback(it) }
    return remember(launcher) {
        {
            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) {
                callback(true)
                true
            } else {
                runCatching { launcher.launch(Manifest.permission.ACTIVITY_RECOGNITION) }.isSuccess
            }
        }
    }
}

@Composable
actual fun rememberNotificationPermissionLauncher(onResult: (Boolean) -> Unit): () -> Boolean {
    val callback by rememberUpdatedState(onResult)
    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { callback(it) }
    return remember(launcher) {
        {
            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) {
                callback(true)
                true
            } else {
                runCatching { launcher.launch(Manifest.permission.POST_NOTIFICATIONS) }.isSuccess
            }
        }
    }
}

private fun Context.findActivity(): Activity? {
    var current: Context? = this
    while (current is ContextWrapper) {
        if (current is Activity) return current
        current = current.baseContext
    }
    return null
}

/** OS 가 지금 걸음 권한 설명을 권하는가 — 화면(Activity)이 없으면 false */
@Composable
actual fun rememberActivityRationale(): () -> Boolean {
    val context = LocalContext.current
    return remember(context) {
        {
            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) false
            else context.findActivity()?.let { ActivityCompat.shouldShowRequestPermissionRationale(it, Manifest.permission.ACTIVITY_RECOGNITION) } ?: false
        }
    }
}

actual val permissionApiLevel: Int get() = Build.VERSION.SDK_INT
