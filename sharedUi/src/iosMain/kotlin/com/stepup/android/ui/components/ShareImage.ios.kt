@file:OptIn(kotlinx.cinterop.ExperimentalForeignApi::class, kotlinx.cinterop.BetaInteropApi::class)

package com.stepup.android.ui.components

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asSkiaBitmap
import kotlinx.cinterop.addressOf
import kotlinx.cinterop.usePinned
import org.jetbrains.skia.EncodedImageFormat
import org.jetbrains.skia.Image
import platform.Foundation.NSData
import platform.Foundation.dataWithBytes
import platform.Photos.PHAccessLevelAddOnly
import platform.Photos.PHAssetChangeRequest
import platform.Photos.PHAuthorizationStatusAuthorized
import platform.Photos.PHAuthorizationStatusLimited
import platform.Photos.PHPhotoLibrary
import platform.UIKit.UIImage
import platform.darwin.dispatch_async
import platform.darwin.dispatch_get_main_queue

/** 그림 → PNG → UIImage(공유 · 사진첩이 받는 꼴) */
private fun ImageBitmap.toUiImage(): UIImage? {
    val png = Image.makeFromBitmap(asSkiaBitmap()).encodeToData(EncodedImageFormat.PNG)?.bytes ?: return null
    val data = png.usePinned { NSData.dataWithBytes(it.addressOf(0), png.size.toULong()) }
    return UIImage.imageWithData(data)
}

actual suspend fun shareImage(image: ImageBitmap, text: String, chooserTitle: String?) {
    val ui = image.toUiImage()
    val items: List<Any> = if (ui != null) listOf(ui, text) else listOf(text)
    if (!com.stepup.android.core.ExternalIntents.share(items)) error("no window")
}

/** 사진 보관함에 더하기만(읽지 않는다) — Info.plist NSPhotoLibraryAddUsageDescription */
@Composable
actual fun rememberRunImageSaver(onResult: (Boolean) -> Unit): (image: ImageBitmap, name: String) -> Unit {
    val callback = rememberUpdatedState(onResult)
    return remember {
        { image, _ ->
            val ui = image.toUiImage()
            if (ui == null) {
                callback.value(false)
            } else {
                PHPhotoLibrary.requestAuthorizationForAccessLevel(PHAccessLevelAddOnly) { status ->
                    if (status != PHAuthorizationStatusAuthorized && status != PHAuthorizationStatusLimited) {
                        dispatch_async(dispatch_get_main_queue()) { callback.value(false) }
                    } else {
                        PHPhotoLibrary.sharedPhotoLibrary().performChanges({
                            PHAssetChangeRequest.creationRequestForAssetFromImage(ui)
                        }) { ok, _ -> dispatch_async(dispatch_get_main_queue()) { callback.value(ok) } }
                    }
                }
            }
        }
    }
}
