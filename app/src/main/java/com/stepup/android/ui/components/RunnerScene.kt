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
import androidx.compose.ui.graphics.graphicsLayer
import com.stepup.android.ui.experience.LocalMotion
import com.stepup.android.R
import com.stepup.android.ui.theme.Night

/** Scenery only. The navigation shell owns chrome; the screen owns the equipped avatar. */
enum class RunnerSetting { Night, Sunset, Wardrobe, HomeBlueNight, HomeDawn, RunNight, RunSunset }

/** Only scenes with visible ground at the standing foot anchor belong here. */
object WardrobeBackgrounds {
    // The night skyline has water at the wardrobe foot anchor, so it stays out of this pool.
    val settings = listOf(RunnerSetting.Wardrobe, RunnerSetting.Sunset)

    fun next(current: RunnerSetting): RunnerSetting = settings.filterNot { it == current }.random()
}

/** Home scenes share a ground plane with the standing character. Keep the choice
 * in the navigation shell so recomposition, balance updates and outfit changes
 * never shuffle the scenery underneath the user. */
object HomeBackgrounds {
    val initial = listOf(RunnerSetting.HomeBlueNight, RunnerSetting.HomeDawn)
    val settings = initial + RunnerSetting.Night

    fun next(current: RunnerSetting): RunnerSetting = settings.filterNot { it == current }.random()

    fun nextInOrder(current: RunnerSetting): RunnerSetting =
        settings[(settings.indexOf(current).coerceAtLeast(0) + 1) % settings.size]

    fun previous(current: RunnerSetting): RunnerSetting =
        settings[(settings.indexOf(current).coerceAtLeast(0) + settings.size - 1) % settings.size]
}

/** A running session keeps one scene from preparation through its result. */
object RunBackgrounds {
    val settings = listOf(RunnerSetting.RunNight, RunnerSetting.RunSunset)
}

/** Grounded scenery compatible with the profile bench; artwork and controls stay separate. */
object ProfileBackgrounds {
    val settings = listOf(RunnerSetting.HomeBlueNight, RunnerSetting.HomeDawn)

    fun next(current: RunnerSetting): RunnerSetting = settings.filterNot { it == current }.random()
}

@Composable
fun RunnerScene(
    modifier: Modifier = Modifier,
    setting: RunnerSetting = RunnerSetting.Night,
    wardrobe: Boolean = false,
    home: Boolean = false,
) {
    val drift = if (LocalMotion.current.decorative) ambientPhase(16000, reverse = true) else null
    Box(modifier) {
        Image(
            painterResource(when (setting) {
                RunnerSetting.Night -> R.drawable.scene_riverside_night
                RunnerSetting.Sunset -> R.drawable.scene_riverside_sunset
                RunnerSetting.Wardrobe -> R.drawable.scene_wardrobe_terrace
                RunnerSetting.HomeBlueNight -> R.drawable.scene_home_blue_night
                RunnerSetting.HomeDawn -> R.drawable.scene_home_dawn
                RunnerSetting.RunNight -> R.drawable.scene_run_night
                RunnerSetting.RunSunset -> R.drawable.scene_run_sunset
            }),
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = Modifier.fillMaxSize().graphicsLayer {
                // Scenery alone moves. Native controls and the character keep their anchors.
                val progress = drift?.value ?: 0.5f
                scaleX = 1.035f
                scaleY = 1.035f
                translationX = (progress - 0.5f) * 10.dp.toPx()
            },
        )
        // Protect native header/footer contrast in both themes without tinting the avatar.
        val stops = if (wardrobe) arrayOf(
            0f to Night.copy(alpha = 0.86f), 0.12f to Night.copy(alpha = 0.3f),
            0.25f to Color.Transparent, 0.6f to Color.Transparent,
            0.78f to Night, 1f to Night,
        ) else if (home) arrayOf(
            0f to Night.copy(alpha = 0.52f), 0.14f to Night.copy(alpha = 0.22f),
            0.28f to Color.Transparent, 0.70f to Color.Transparent,
            0.90f to Night.copy(alpha = 0.54f), 1f to Night,
        ) else arrayOf(
            0f to Night, 0.16f to Night.copy(alpha = 0.8f),
            0.33f to Color.Transparent, 0.72f to Color.Transparent,
            0.91f to Night.copy(alpha = 0.92f), 1f to Night,
        )
        Box(Modifier.fillMaxSize().background(Brush.verticalGradient(*stops)))
    }
}
