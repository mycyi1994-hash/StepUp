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
val StepUpNumbers = FontFamily(
    Font(R.font.barlow_semibold, FontWeight.SemiBold),
    Font(R.font.barlow_bold, FontWeight.Bold),
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
