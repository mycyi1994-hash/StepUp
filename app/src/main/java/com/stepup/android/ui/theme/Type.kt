package com.stepup.android.ui.theme

import androidx.compose.ui.unit.em
import androidx.compose.material3.Typography
import androidx.compose.ui.text.PlatformTextStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import com.stepup.android.R

/** Bundled OFL fonts work offline. Android supplies missing CJK/emoji glyphs. */
val StepUpSans = FontFamily(
    Font(R.font.pretendard_regular, FontWeight.Normal),
    Font(R.font.pretendard_medium, FontWeight.Medium),
    Font(R.font.pretendard_semibold, FontWeight.SemiBold),
    Font(R.font.pretendard_bold, FontWeight.Bold),
    Font(R.font.pretendard_extrabold, FontWeight.ExtraBold),
)
/**
 * 숫자 자리. S2 는 숫자도 본문과 같은 Pretendard 를 쓴다(자리 맞춤은 tnum).
 * Barlow 파일과 라이선스는 남겨 둔다 — 이전 캡처 · 문서가 가리킨다.
 */
val StepUpNumbers = FontFamily(
    Font(R.font.pretendard_regular, FontWeight.Normal),
    Font(R.font.pretendard_semibold, FontWeight.SemiBold),
    Font(R.font.pretendard_bold, FontWeight.Bold),
    Font(R.font.pretendard_extrabold, FontWeight.ExtraBold),
)

private fun type(size: Int, height: Int, weight: FontWeight, tracking: Double = 0.0) = TextStyle(
    fontFamily = StepUpSans, fontWeight = weight, fontSize = size.sp, lineHeight = (height.toFloat() / size).em,
    fontFeatureSettings = "tnum", letterSpacing = tracking.sp, platformStyle = PlatformTextStyle(includeFontPadding = false),
)

val StepUpTypography = Typography(
    displayLarge = type(56, 62, FontWeight.ExtraBold, -1.1),
    displayMedium = type(44, 50, FontWeight.ExtraBold, -.8),
    displaySmall = type(36, 42, FontWeight.ExtraBold, -.6),
    headlineLarge = type(32, 40, FontWeight.ExtraBold, -.5),
    headlineMedium = type(28, 36, FontWeight.Bold, -.4),
    headlineSmall = type(24, 32, FontWeight.Bold, -.3),
    titleLarge = type(22, 30, FontWeight.Bold, -.2),
    titleMedium = type(16, 24, FontWeight.SemiBold),
    titleSmall = type(14, 21, FontWeight.SemiBold),
    bodyLarge = type(16, 25, FontWeight.Normal),
    bodyMedium = type(15, 23, FontWeight.Normal),
    bodySmall = type(13, 20, FontWeight.Medium),
    labelLarge = type(14, 20, FontWeight.SemiBold, .15),
    labelMedium = type(12, 18, FontWeight.SemiBold, .2),
    labelSmall = type(12, 18, FontWeight.SemiBold, .3),
)

val MetricTypography = TextStyle(
    fontFamily = StepUpNumbers, fontWeight = FontWeight.Bold, lineHeight = 1.2.em,
    fontFeatureSettings = "tnum", letterSpacing = (-.5).sp,
    platformStyle = PlatformTextStyle(includeFontPadding = false),
)
