package com.stepup.android.ui.theme

import androidx.compose.runtime.Composable

/** iOS — Compose 리소스 기본값(기기 · 앱 언어)을 그대로 쓴다 */
@Composable
actual fun ProvidePlatformResources(content: @Composable () -> Unit) = content()
