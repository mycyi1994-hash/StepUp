package com.stepup.android.ui.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import com.stepup.android.R
import com.stepup.android.ui.theme.Night

/** Scenery only. The navigation shell owns chrome; the screen owns the equipped avatar. */
enum class RunnerSetting { Night, Sunset, Wardrobe }

/** Only standing, riverside scenes share this pool. Seated artwork needs its own pool. */
object WardrobeBackgrounds {
    val settings = listOf(RunnerSetting.Wardrobe, RunnerSetting.Night, RunnerSetting.Sunset)

    fun next(current: RunnerSetting): RunnerSetting = settings.filterNot { it == current }.random()
}

@Composable
fun RunnerScene(
    modifier: Modifier = Modifier,
    setting: RunnerSetting = RunnerSetting.Night,
    wardrobe: Boolean = false,
) {
    Box(modifier) {
        Image(
            painterResource(when (setting) {
                RunnerSetting.Night -> R.drawable.scene_riverside_night
                RunnerSetting.Sunset -> R.drawable.scene_riverside_sunset
                RunnerSetting.Wardrobe -> R.drawable.scene_wardrobe_terrace
            }),
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = Modifier.fillMaxSize(),
        )
        // Protect native header/footer contrast in both themes without tinting the avatar.
        val stops = if (wardrobe) arrayOf(
            0f to Night.copy(alpha = 0.86f), 0.12f to Night.copy(alpha = 0.3f),
            0.25f to Color.Transparent, 0.6f to Color.Transparent,
            0.78f to Night, 1f to Night,
        ) else arrayOf(
            0f to Night, 0.16f to Night.copy(alpha = 0.8f),
            0.33f to Color.Transparent, 0.72f to Color.Transparent,
            0.91f to Night.copy(alpha = 0.92f), 1f to Night,
        )
        Box(Modifier.fillMaxSize().background(Brush.verticalGradient(*stops)))
    }
}
