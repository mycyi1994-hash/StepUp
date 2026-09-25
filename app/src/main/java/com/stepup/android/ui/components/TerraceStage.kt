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
    Box(modifier.clipToBounds()) {
        Image(painterResource(R.drawable.scene_terrace_stage), contentDescription = null,
            contentScale = ContentScale.Crop, modifier = Modifier.fillMaxSize()
                .graphicsLayer { compositingStrategy = CompositingStrategy.Offscreen }
                .drawWithContent {
                    drawContent()
                    // Reveal the actual canvas behind the artwork, including its gradient/theme.
                    // Mask only scenery: the avatar and interactive content retain full opacity.
                    drawRect(Brush.verticalGradient(
                        0f to Color.Transparent, 0.16f to Color.White,
                        0.92f to Color.White, 1f to Color.Transparent,
                    ), blendMode = BlendMode.DstIn)
                    drawRect(Brush.horizontalGradient(
                        0f to Color.Transparent, 0.09f to Color.White,
                        0.91f to Color.White, 1f to Color.Transparent,
                    ), blendMode = BlendMode.DstIn)
                })
        content()
    }
}
