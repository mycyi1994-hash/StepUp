package com.stepup.android.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import com.stepup.android.ui.theme.Night
import com.stepup.android.ui.theme.Volt

/** Shared dark surface for owned items and live market data; product art stays independent. */
@Composable
fun CommerceBackdrop(modifier: Modifier = Modifier) {
    Box(modifier.background(Brush.verticalGradient(listOf(Color(0xFF0B1B37), Night, Night)))) {
        Canvas(Modifier.fillMaxSize()) {
            drawCircle(
                brush = Brush.radialGradient(
                    listOf(Volt.copy(alpha = 0.15f), Color.Transparent),
                    center = Offset(size.width * 0.82f, size.height * 0.12f),
                    radius = size.width * 1.1f,
                ),
                radius = size.width * 1.1f,
                center = Offset(size.width * 0.82f, size.height * 0.12f),
            )
        }
    }
}
