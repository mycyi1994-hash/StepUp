package com.stepup.android.shots

import androidx.activity.ComponentActivity
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import com.stepup.android.R
import com.stepup.android.ui.components.PlainEmptyState
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [35], qualifiers = "w393dp-h852dp-xxhdpi")
class SmokeShotsTest {
    @get:Rule val compose = createAndroidComposeRule<ComponentActivity>()

    @Before fun only() = Shots.require()

    @Test fun empty() {
        compose.shot("00_smoke", dark = false, size = ShotSizes[0]) {
            PlainEmptyState(R.drawable.scene_blanket, "기록이 없어요", body = "첫 러닝을 시작해 보세요", action = "지난 기록 보기", onAction = {})
        }
    }
}
