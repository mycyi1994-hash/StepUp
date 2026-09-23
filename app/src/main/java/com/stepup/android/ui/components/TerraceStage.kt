package com.stepup.android.ui.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import com.stepup.android.R
import com.stepup.android.ui.theme.Night

/** Scenery shares the avatar's bounds, so changing the hero height cannot move its feet into the sky. */
@Composable
fun TerraceStage(modifier: Modifier = Modifier, content: @Composable BoxScope.() -> Unit) {
    Box(modifier.clipToBounds()) {
        Image(painterResource(R.drawable.scene_terrace_stage), contentDescription = null,
            contentScale = ContentScale.Crop, modifier = Modifier.fillMaxSize())
        Box(Modifier.fillMaxSize().background(Brush.verticalGradient(
            0f to Night, 0.16f to Color.Transparent, 0.92f to Color.Transparent, 1f to Night,
        )))
        content()
    }
}
