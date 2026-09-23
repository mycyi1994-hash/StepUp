package com.stepup.android.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable

/**
 * 화면 테마 — 사용자가 고른다.
 *
 * 기기 설정을 따르는 것이 기본이다. 다만 따로 고른 사람도 있다 — 기기는
 * 다크인데 러닝 앱만 밝게 쓰고 싶거나, 그 반대이거나.
 */
enum class ThemeMode {
    /** 기기 설정을 따른다 */
    SYSTEM,

    /** 흰 바탕 */
    LIGHT,

    /** 검은 바탕 */
    DARK;

    /** @param systemDark 기기가 지금 다크인지 */
    fun isDark(systemDark: Boolean): Boolean = when (this) {
        SYSTEM -> systemDark
        LIGHT -> false
        DARK -> true
    }

    companion object {
        /** 저장된 이름을 되읽는다. 모르는 값이면 기기 설정을 따른다. */
        /**
         * 저장된 이름을 테마로. 아직 고른 적이 없으면 **다크**다.
         *
         * StepUp 의 기본 모습은 블랙·블루다. 밝은 테마와 기기 설정 따르기는
         * 설정 > 테마에서 고를 수 있고, 한 번 고르면 그 값이 남는다.
         */
        fun of(name: String?): ThemeMode =
            entries.firstOrNull { it.name == name } ?: DARK
    }
}

private fun schemeOf(p: StepUpPalette) = if (p.dark) {
    darkColorScheme(
        primary = p.volt,
        onPrimary = p.onVolt,
        primaryContainer = p.carbonHigh,
        onPrimaryContainer = p.voltSoft,
        secondary = p.voltSoft,
        onSecondary = p.onVolt,
        secondaryContainer = p.carbonHigh,
        onSecondaryContainer = p.snow,
        tertiary = p.volt,
        onTertiary = p.onVolt,
        background = p.night,
        onBackground = p.snow,
        surface = p.carbon,
        onSurface = p.snow,
        surfaceVariant = p.carbonHigh,
        onSurfaceVariant = p.silver,
        surfaceContainer = p.carbon,
        surfaceContainerHigh = p.carbonHigh,
        surfaceContainerHighest = p.carbonHigh,
        error = p.alert,
        onError = p.onVolt,
        outline = p.edge,
        outlineVariant = p.edge,
        scrim = p.night,
    )
} else {
    lightColorScheme(
        primary = p.volt,
        onPrimary = p.onVolt,
        primaryContainer = p.carbonHigh,
        onPrimaryContainer = p.voltDeep,
        secondary = p.voltSoft,
        onSecondary = p.onVolt,
        secondaryContainer = p.carbonHigh,
        onSecondaryContainer = p.snow,
        tertiary = p.volt,
        onTertiary = p.onVolt,
        background = p.night,
        onBackground = p.snow,
        surface = p.carbon,
        onSurface = p.snow,
        surfaceVariant = p.carbonHigh,
        onSurfaceVariant = p.silver,
        surfaceContainer = p.carbon,
        surfaceContainerHigh = p.carbonHigh,
        surfaceContainerHighest = p.carbonHigh,
        error = p.alert,
        onError = p.onVolt,
        outline = p.edge,
        outlineVariant = p.edge,
        scrim = p.snow,
    )
}

/**
 * 고른 테마의 팔레트를 꽂고 화면을 그린다.
 *
 * 팔레트를 [content] 보다 **먼저** 꽂는 것이 중요하다. 나중에 꽂으면 테마를
 * 바꾼 첫 프레임이 옛 색으로 한 번 그려졌다가 바뀌어, 눈에 깜빡임으로 남는다.
 */
@Composable
fun StepUpTheme(mode: ThemeMode = ThemeMode.SYSTEM, content: @Composable () -> Unit) {
    val palette = if (mode.isDark(isSystemInDarkTheme())) DarkPalette else LightPalette
    applyPalette(palette)
    MaterialTheme(
        colorScheme = schemeOf(palette),
        typography = StepUpTypography,
        shapes = StepUpShapes,
        content = content,
    )
}
