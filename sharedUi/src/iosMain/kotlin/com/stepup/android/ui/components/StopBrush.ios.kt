package com.stepup.android.ui.components

import androidx.compose.ui.graphics.Brush

/** iOS 는 Metal 이 그린다 — 여러 색 그라디언트를 그대로 쓴다(StopBrush.kt) */
actual val gradientsNeedRaster: Boolean = false

actual fun rasterGradient(spec: GradientSpec, plain: Brush): Brush = plain
