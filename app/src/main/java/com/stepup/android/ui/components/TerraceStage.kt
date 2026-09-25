package com.stepup.android.ui.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import com.stepup.android.R

/** Scenery shares the avatar's bounds, so changing the hero height cannot move its feet into the sky. */
@Composable
fun TerraceStage(modifier: Modifier = Modifier, content: @Composable BoxScope.() -> Unit) {
    // Retired scenery: preserve caller content without a character stage.
    Box(modifier, content = content)
}
