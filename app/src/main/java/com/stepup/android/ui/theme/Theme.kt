package com.stepup.android.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable

private val StepUpLightColors = lightColorScheme(
    primary = Volt,
    onPrimary = Night,
    primaryContainer = CarbonHigh,
    onPrimaryContainer = VoltDeep,
    secondary = VoltSoft,
    onSecondary = Night,
    secondaryContainer = CarbonHigh,
    onSecondaryContainer = Snow,
    tertiary = Volt,
    onTertiary = Night,
    background = Night,
    onBackground = Snow,
    surface = Carbon,
    onSurface = Snow,
    surfaceVariant = CarbonHigh,
    onSurfaceVariant = Silver,
    surfaceContainer = Carbon,
    surfaceContainerHigh = CarbonHigh,
    surfaceContainerHighest = CarbonHigh,
    error = Alert,
    onError = Night,
    outline = Edge,
    outlineVariant = Edge,
    scrim = Snow,
)

/** StepUp은 화이트 캔버스에 블루 하나를 쓰는 라이트 테마만 사용한다. */
@Composable
fun StepUpTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = StepUpLightColors,
        typography = StepUpTypography,
        shapes = StepUpShapes,
        content = content,
    )
}
