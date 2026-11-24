package com.stepup.android.shots

import android.graphics.Bitmap
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.requiredSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.junit4.ComposeContentTestRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import androidx.compose.ui.graphics.asAndroidBitmap
import com.stepup.android.ui.components.runTone
import com.stepup.android.ui.theme.StepUpTheme
import com.stepup.android.ui.theme.ThemeMode
import org.junit.Assume.assumeTrue
import java.io.File

/** 재디자인 확인용 캡처 — `./gradlew :app:testDebugUnitTest --tests '*Shots*' -Pshots=1`, 결과는 build/shots/<이름>_<테마>_<폭>.png */
object Shots {
    val on: Boolean get() = !System.getProperty("shots").isNullOrBlank()
    fun require() = assumeTrue("캡처는 -Pshots 로만", on)
    val dir: File get() = File(System.getProperty("shotsDir") ?: "build/shots").apply { mkdirs() }
}

data class ShotSize(val widthDp: Int, val heightDp: Int, val font: Float = 1f, val tag: String)

val ShotSizes = listOf(
    ShotSize(393, 852, 1f, "393"),
    ShotSize(360, 780, 1.3f, "360f130"),
)

fun ComposeContentTestRule.shot(name: String, dark: Boolean, size: ShotSize, content: @Composable () -> Unit) {
    setContent {
        CompositionLocalProvider(LocalDensity provides Density(3f, size.font)) {
            StepUpTheme(if (dark) ThemeMode.DARK else ThemeMode.LIGHT) {
                val t = runTone()
                Box(Modifier.requiredSize(size.widthDp.dp, size.heightDp.dp).background(t.screen).testTag("shot-root")) {
                    content()
                }
            }
        }
    }
    waitForIdle()
    mainClock.advanceTimeBy(1500)
    waitForIdle()
    // captureToImage 는 Robolectric 에서 다시 그리기를 기다리다 멈춘다 — 뿌리 뷰를 직접 그린다
    val view = (this as androidx.compose.ui.test.junit4.AndroidComposeTestRule<*, *>).activity.window.decorView
    val img = Bitmap.createBitmap(view.width.coerceAtLeast(1), view.height.coerceAtLeast(1), Bitmap.Config.ARGB_8888)
    view.draw(android.graphics.Canvas(img))
    val f = File(Shots.dir, "${name}_${if (dark) "dark" else "light"}_${size.tag}.png")
    f.outputStream().use { img.compress(Bitmap.CompressFormat.PNG, 100, it) }
}
