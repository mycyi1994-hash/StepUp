package com.stepup.android

import android.graphics.Bitmap
import androidx.test.platform.app.InstrumentationRegistry
import java.io.File

/** Preserve what the device actually displayed, including separate dialog/IME windows. */
internal fun captureDisplay(destination: File) {
    val bitmap = InstrumentationRegistry.getInstrumentation().uiAutomation.takeScreenshot()
        ?: error("Could not capture display: ${destination.name}")
    try {
        destination.parentFile?.mkdirs()
        destination.outputStream().use { stream ->
            check(bitmap.compress(Bitmap.CompressFormat.PNG, 100, stream)) {
                "Could not write display capture: ${destination.name}"
            }
        }
    } finally {
        bitmap.recycle()
    }
}
