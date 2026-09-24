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
import com.stepup.android.ui.theme.CarbonHigh
import com.stepup.android.ui.theme.Volt

/** Shared surface for utility screens and commerce; artwork and controls remain independent. */
@Composable
fun CommerceBackdrop(modifier: Modifier = Modifier) {
    Box(modifier.background(Brush.verticalGradient(listOf(CarbonHigh, Night, Night)))) {
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
