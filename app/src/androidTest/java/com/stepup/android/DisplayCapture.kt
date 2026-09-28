package com.stepup.android

import android.app.Activity
import android.graphics.Bitmap
import android.os.Build
import androidx.test.platform.app.InstrumentationRegistry
import java.io.File
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit

/**
 * Wait until the activity window has handed a freshly drawn frame to the display. On the software-GPU emulator a heavy
 * first screen can reach the display well after composition settles, so an immediate display capture may still show
 * the previous frame (2026-09-28: a "loading" frame after the shoes tab had already rendered its stage).
 */
internal fun awaitFrameOnScreen(activity: Activity, timeoutMs: Long = 3_000) {
    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) return
    val committed = CountDownLatch(1)
    InstrumentationRegistry.getInstrumentation().runOnMainSync {
        val view = activity.window.decorView
        view.viewTreeObserver.registerFrameCommitCallback { committed.countDown() }
        view.invalidate()
    }
    committed.await(timeoutMs, TimeUnit.MILLISECONDS)
    // One more vsync for the display to latch the submitted buffer.
    Thread.sleep(50)
}

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
