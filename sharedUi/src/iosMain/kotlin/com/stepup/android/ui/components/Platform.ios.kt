@file:OptIn(kotlinx.cinterop.ExperimentalForeignApi::class)

package com.stepup.android.ui.components

import platform.Foundation.currentLocale
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.toComposeImageBitmap
import androidx.compose.ui.text.style.LineBreak
import kotlinx.cinterop.addressOf
import kotlinx.cinterop.usePinned
import platform.Foundation.NSData
import platform.Foundation.NSDocumentDirectory
import platform.Foundation.NSFileManager
import platform.Foundation.NSUserDomainMask
import platform.Foundation.dataWithContentsOfFile
import platform.posix.memcpy

actual fun koreanLineBreak(): LineBreak = LineBreak.Paragraph

actual fun readCustomAvatar(): ImageBitmap? = runCatching {
    val dir = NSFileManager.defaultManager.URLForDirectory(NSDocumentDirectory, NSUserDomainMask, null, false, null)?.path
        ?: return null
    val data = NSData.dataWithContentsOfFile("$dir/${com.stepup.android.data.prefs.UserPrefs.AVATAR_FILE}") ?: return null
    val bytes = ByteArray(data.length.toInt())
    if (bytes.isNotEmpty()) bytes.usePinned { memcpy(it.addressOf(0), data.bytes, data.length) }
    org.jetbrains.skia.Image.makeFromEncoded(bytes).toComposeImageBitmap()
}.getOrNull()

@OptIn(androidx.compose.ui.ExperimentalComposeUiApi::class)
actual fun clearDialogProperties(dismissOnClickOutside: Boolean): androidx.compose.ui.window.DialogProperties =
    androidx.compose.ui.window.DialogProperties(
        dismissOnClickOutside = dismissOnClickOutside, usePlatformDefaultWidth = false,
        scrimColor = androidx.compose.ui.graphics.Color.Transparent,
    )


@androidx.compose.runtime.Composable
actual fun LightSystemBarIcons() = Unit

/** 기기 시각 꼴에 오전/오후(a)가 없으면 24시간 */
actual fun is24HourClock(): Boolean =
    platform.Foundation.NSDateFormatter.dateFormatFromTemplate("j", 0u, platform.Foundation.NSLocale.currentLocale)?.contains("a") != true
