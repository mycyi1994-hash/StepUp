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
@Composable
fun RunnerScene(modifier: Modifier = Modifier) {
    Box(modifier) {
        Image(
            painterResource(R.drawable.scene_riverside_night),
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = Modifier.fillMaxSize(),
        )
        // Protect native header/footer contrast in both themes without tinting the avatar.
        Box(Modifier.fillMaxSize().background(Brush.verticalGradient(
            0f to Night, 0.16f to Night.copy(alpha = 0.8f),
            0.33f to Color.Transparent, 0.72f to Color.Transparent,
            0.91f to Night.copy(alpha = 0.92f), 1f to Night,
        )))
    }
}
