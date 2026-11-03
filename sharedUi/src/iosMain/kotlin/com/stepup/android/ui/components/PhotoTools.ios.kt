@file:OptIn(kotlinx.cinterop.ExperimentalForeignApi::class, kotlinx.cinterop.BetaInteropApi::class)

package com.stepup.android.ui.components

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asSkiaBitmap
import androidx.compose.ui.graphics.toComposeImageBitmap
import kotlinx.cinterop.addressOf
import kotlinx.cinterop.usePinned
import org.jetbrains.skia.EncodedImageFormat
import org.jetbrains.skia.Image
import platform.CoreLocation.CLLocationManager
import platform.CoreLocation.CLLocationManagerDelegateProtocol
import platform.CoreLocation.kCLAuthorizationStatusAuthorizedAlways
import platform.CoreLocation.kCLAuthorizationStatusAuthorizedWhenInUse
import platform.CoreLocation.kCLAuthorizationStatusNotDetermined
import platform.Foundation.NSData
import platform.Foundation.NSFileManager
import platform.Foundation.NSSearchPathForDirectoriesInDomains
import platform.Foundation.NSCachesDirectory
import platform.Foundation.NSUserDomainMask
import platform.Foundation.create
import platform.Foundation.dataWithContentsOfFile
import platform.Foundation.writeToFile
import platform.PhotosUI.PHPickerConfiguration
import platform.PhotosUI.PHPickerFilter
import platform.PhotosUI.PHPickerResult
import platform.PhotosUI.PHPickerViewController
import platform.PhotosUI.PHPickerViewControllerDelegateProtocol
import platform.UIKit.UIApplication
import platform.UIKit.UIImage
import platform.UIKit.UIImageJPEGRepresentation
import platform.UIKit.UIViewController
import platform.darwin.NSObject
import platform.darwin.dispatch_async
import platform.darwin.dispatch_get_main_queue
import platform.posix.memcpy

private fun NSData.toByteArray(): ByteArray {
    val size = length.toInt()
    if (size == 0) return ByteArray(0)
    return ByteArray(size).also { out -> out.usePinned { memcpy(it.addressOf(0), bytes, length) } }
}

actual fun ImageBitmap.encodeJpeg(quality: Int): ByteArray? =
    runCatching { Image.makeFromBitmap(asSkiaBitmap()).encodeToData(EncodedImageFormat.JPEG, quality)?.bytes }.getOrNull()

actual fun decodeImageFile(path: String): ImageBitmap? = runCatching {
    val data = NSData.dataWithContentsOfFile(path) ?: return null
    Image.makeFromEncoded(data.toByteArray()).toComposeImageBitmap()
}.getOrNull()

actual fun readFileBytes(path: String): ByteArray? = NSData.dataWithContentsOfFile(path)?.toByteArray()

actual fun writeFileBytes(path: String, bytes: ByteArray): Boolean {
    NSFileManager.defaultManager.createDirectoryAtPath(path.substringBeforeLast('/'), withIntermediateDirectories = true, attributes = null, error = null)
    val data = bytes.usePinned { NSData.create(bytes = it.addressOf(0), length = bytes.size.toULong()) }
    return data.writeToFile(path, atomically = true)
}

private fun topController(): UIViewController? {
    var top = UIApplication.sharedApplication.keyWindow?.rootViewController
    while (top?.presentedViewController != null) top = top.presentedViewController
    return top
}

private val pickedDir: String by lazy {
    (NSSearchPathForDirectoriesInDomains(NSCachesDirectory, NSUserDomainMask, true).first() as String) + "/picked"
}

@Composable
actual fun rememberPhotoPicker(onPicked: (String?) -> Unit): () -> Unit {
    val callback = rememberUpdatedState(onPicked)
    val delegate = remember {
        object : NSObject(), PHPickerViewControllerDelegateProtocol {
            override fun picker(picker: PHPickerViewController, didFinishPicking: List<*>) {
                picker.dismissViewControllerAnimated(true, completion = null)
                val result = didFinishPicking.firstOrNull() as? PHPickerResult
                if (result == null) {
                    callback.value(null)
                    return
                }
                result.itemProvider.loadDataRepresentationForTypeIdentifier("public.image") { data, _ ->
                    // HEIC · PNG 도 JPEG 로 바꿔 둔다(안드로이드처럼 화면은 JPEG 파일만 읽는다)
                    val jpeg = data?.let { UIImage(data = it) }?.let { UIImageJPEGRepresentation(it, 0.95) }
                    val path = jpeg?.let {
                        val p = "$pickedDir/photo-${com.stepup.android.domain.randomUuid()}.jpg"
                        if (writeFileBytes(p, it.toByteArray())) p else null
                    }
                    dispatch_async(dispatch_get_main_queue()) { callback.value(path) }
                }
            }
        }
    }
    return remember {
        {
            val config = PHPickerConfiguration().apply {
                filter = PHPickerFilter.imagesFilter
                selectionLimit = 1
            }
            val picker = PHPickerViewController(configuration = config)
            picker.delegate = delegate
            topController()?.presentViewController(picker, animated = true, completion = null)
        }
    }
}

@Composable
actual fun rememberLocationPermissionRequest(onResult: (Boolean) -> Unit): () -> Unit {
    val callback = rememberUpdatedState(onResult)
    val manager = remember { CLLocationManager() }
    val delegate = remember {
        object : NSObject(), CLLocationManagerDelegateProtocol {
            override fun locationManagerDidChangeAuthorization(manager: CLLocationManager) {
                val status = manager.authorizationStatus
                if (status == kCLAuthorizationStatusNotDetermined) return
                callback.value(status == kCLAuthorizationStatusAuthorizedWhenInUse || status == kCLAuthorizationStatusAuthorizedAlways)
            }
        }
    }
    DisposableEffect(Unit) {
        manager.delegate = delegate
        onDispose { manager.delegate = null }
    }
    return remember {
        {
            val status = manager.authorizationStatus
            if (status == kCLAuthorizationStatusNotDetermined) manager.requestWhenInUseAuthorization()
            else callback.value(status == kCLAuthorizationStatusAuthorizedWhenInUse || status == kCLAuthorizationStatusAuthorizedAlways)
        }
    }
}

@Composable
actual fun rememberCameraCapture(onTaken: (String?) -> Unit): (() -> Unit)? {
    if (!platform.UIKit.UIImagePickerController.isSourceTypeAvailable(platform.UIKit.UIImagePickerControllerSourceType.UIImagePickerControllerSourceTypeCamera)) return null
    val callback = rememberUpdatedState(onTaken)
    val delegate = remember {
        object : NSObject(), platform.UIKit.UIImagePickerControllerDelegateProtocol, platform.UIKit.UINavigationControllerDelegateProtocol {
            override fun imagePickerController(picker: platform.UIKit.UIImagePickerController, didFinishPickingMediaWithInfo: Map<Any?, *>) {
                picker.dismissViewControllerAnimated(true, completion = null)
                val image = didFinishPickingMediaWithInfo[platform.UIKit.UIImagePickerControllerOriginalImage] as? UIImage
                val path = image?.let { UIImageJPEGRepresentation(it, 0.95) }?.let {
                    val p = "$pickedDir/camera-${com.stepup.android.domain.randomUuid()}.jpg"
                    if (writeFileBytes(p, it.toByteArray())) p else null
                }
                callback.value(path)
            }

            override fun imagePickerControllerDidCancel(picker: platform.UIKit.UIImagePickerController) {
                picker.dismissViewControllerAnimated(true, completion = null)
                callback.value(null)
            }
        }
    }
    return remember {
        {
            val picker = platform.UIKit.UIImagePickerController().apply {
                sourceType = platform.UIKit.UIImagePickerControllerSourceType.UIImagePickerControllerSourceTypeCamera
                this.delegate = delegate
            }
            topController()?.presentViewController(picker, animated = true, completion = null)
        }
    }
}
