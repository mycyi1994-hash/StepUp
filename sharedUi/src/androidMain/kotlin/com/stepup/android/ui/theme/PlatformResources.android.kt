package com.stepup.android.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalDensity

/**
 * 안드로이드 — Compose 리소스가 프로세스 기본 언어 대신 이 화면의 설정(LocalConfiguration · 앱 언어)으로 고르게 한다
 * (예전 안드로이드 stringResource 와 같다). 다리는 [ComposeResourcesBridge].
 */
@Composable
actual fun ProvidePlatformResources(content: @Composable () -> Unit) {
    val locale = LocalConfiguration.current.locales.get(0)
    val dark = isSystemInDarkTheme()
    val density = LocalDensity.current.density
    val environment = remember(locale, dark, density) {
        ComposeResourcesBridge.environment(locale.language, locale.country, dark, density)
    }
    CompositionLocalProvider(ComposeResourcesBridge.local() provides environment, content = content)
}
