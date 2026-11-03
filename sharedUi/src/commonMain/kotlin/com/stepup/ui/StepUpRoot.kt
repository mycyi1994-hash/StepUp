package com.stepup.ui

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import com.stepup.android.ui.theme.StepUpTheme
import com.stepup.ui.res.Res
import com.stepup.ui.res.stepup_logo_tagline_on_navy
import org.jetbrains.compose.resources.painterResource

/**
 * 로고 바탕(브랜드 네이비) — 로고 그림의 바탕이 불투명이라 같은 색에 얹는다(design/brand/README.md). 그림에서 가장 많은 바탕 색(#031938)이다 —
 * #031737 에 얹으니 시뮬레이터 캡처에서 로고 둘레에 옅은 네모가 보였다. iOS 실행 화면(LaunchBackground)도 같은 색
 */
private val BrandNavy = Color(0xFF031938)

/**
 * iOS 앱의 맨 위 화면 — 지금은 뼈대(2026-11-02)라 로고만 보인다. 로그인 · 접속 코드 · 하단 탭이 여기 아래로 들어온다(docs/ios/README.md 순서).
 */
@Composable
fun StepUpRoot() = StepUpTheme {
    Box(
        modifier = Modifier.fillMaxSize().background(BrandNavy),
        contentAlignment = Alignment.Center,
    ) {
        Image(
            painter = painterResource(Res.drawable.stepup_logo_tagline_on_navy),
            contentDescription = "StepUp",
            modifier = Modifier.fillMaxWidth(0.72f),
        )
    }
}
