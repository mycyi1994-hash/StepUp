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
import androidx.compose.ui.unit.dp
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
    val settings = initial

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
    // The standing avatar's foot anchor lands on the near path in these scenes.
    // Riverside home scenes place that same anchor in the water.
    val settings = listOf(RunnerSetting.RunNight, RunnerSetting.RunSunset)

    fun next(current: RunnerSetting): RunnerSetting = settings.filterNot { it == current }.random()
}

@Composable
fun RunnerScene(
    modifier: Modifier = Modifier,
    setting: RunnerSetting = RunnerSetting.Night,
    wardrobe: Boolean = false,
    home: Boolean = false,
) {
    // Scenery now belongs to an independent banner, never to a full-screen character stage.
    Box(modifier.background(Night))
}

/** Independent, text-free scenery shared by home and login. */
@Composable
fun RunnerBanner(
    modifier: Modifier = Modifier,
    setting: RunnerSetting = RunnerSetting.HomeBlueNight,
) {
    val dawn = setting == RunnerSetting.HomeDawn || setting == RunnerSetting.Sunset || setting == RunnerSetting.RunSunset
    Image(
        painter = painterResource(if (dawn) R.drawable.home_banner_dawn else R.drawable.home_banner_blue_night),
        contentDescription = null,
        contentScale = ContentScale.Crop,
        modifier = modifier,
    )
}
