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

/**
 * 한글 — 페이퍼로지(UI 최종 통합안 2026-10-25, SIL OFL). 앱이 오프라인에서도 같은 글자로 그린다.
 *
 * 페이퍼로지는 같은 굵기 숫자에서 한 단계 무거워 보여 **한 칸씩 낮춰** 잇는다(tokens.json fonts.korean.weights):
 * 400 Regular · 500/600 Medium · 700 SemiBold · 800 Bold · 900 ExtraBold. 화면 코드는 굵기 이름(SemiBold · Bold · ExtraBold)을
 * 그대로 쓰면 시안 CSS 와 같은 무게가 나온다.
 */
val StepUpSans = FontFamily(
    Font(R.font.paperlogy_4regular, FontWeight.Normal),
    Font(R.font.paperlogy_5medium, FontWeight.Medium),
    Font(R.font.paperlogy_5medium, FontWeight.SemiBold),
    Font(R.font.paperlogy_6semibold, FontWeight.Bold),
    Font(R.font.paperlogy_7bold, FontWeight.ExtraBold),
    Font(R.font.paperlogy_8extrabold, FontWeight.Black),
)

/**
 * 숫자 글꼴 파일 — 이 한 줄만 바꾸면 앱의 큰 숫자가 모두 바뀐다.
 *
 * 시안은 Agency FB Bold(Monotype 유료)다. 라이선스가 없어 무료(OFL) **Saira Condensed SemiBold** 로 잇는다.
 * 라이선스를 사면 `res/font/agency_fb_bold.ttf` 를 넣고 이 값을 `R.font.agency_fb_bold` 로 바꾼다.
 */
private val NumberFontFile = R.font.saira_condensed_semibold

/**
 * 숫자 — 시간 · 거리 · 인원 · 칸 수 같은 큰 숫자(콘덴스드, 자간 0). 한 굵기 파일이라 어느 굵기를 불러도 같은 파일을 쓴다
 * (가짜 굵게를 만들지 않게 굵기마다 같은 파일을 단다). 한글 단위는 숫자 옆에 [StepUpSans] 로 따로 쓴다.
 */
val StepUpNumbers = FontFamily(
    Font(NumberFontFile, FontWeight.Normal),
    Font(NumberFontFile, FontWeight.Medium),
    Font(NumberFontFile, FontWeight.SemiBold),
    Font(NumberFontFile, FontWeight.Bold),
    Font(NumberFontFile, FontWeight.ExtraBold),
    Font(NumberFontFile, FontWeight.Black),
)

/** 시안 이름 그대로(HANDOFF 1-1) — 같은 글꼴 */
val NumberFont: FontFamily get() = StepUpNumbers

/** 자간은 em — 본문 −2.5% · 제목 −3.5~−4.5% · 13sp 이하 −2%(tokens.json fonts.letterSpacing) */
private fun type(size: Int, height: Int, weight: FontWeight, tracking: Double) = TextStyle(
    fontFamily = StepUpSans, fontWeight = weight, fontSize = size.sp, lineHeight = (height.toFloat() / size).em,
    fontFeatureSettings = "tnum", letterSpacing = tracking.em, platformStyle = PlatformTextStyle(includeFontPadding = false),
)

val StepUpTypography = Typography(
    displayLarge = type(56, 62, FontWeight.ExtraBold, -0.045),
    displayMedium = type(44, 50, FontWeight.ExtraBold, -0.045),
    displaySmall = type(34, 42, FontWeight.ExtraBold, -0.045),
    headlineLarge = type(32, 40, FontWeight.ExtraBold, -0.04),
    headlineMedium = type(28, 36, FontWeight.ExtraBold, -0.04),
    headlineSmall = type(24, 32, FontWeight.ExtraBold, -0.04),
    titleLarge = type(20, 28, FontWeight.ExtraBold, -0.04),
    titleMedium = type(17, 24, FontWeight.Bold, -0.03),
    titleSmall = type(15, 21, FontWeight.Bold, -0.03),
    bodyLarge = type(16, 22, FontWeight.Normal, -0.025),
    bodyMedium = type(15, 21, FontWeight.Normal, -0.025),
    bodySmall = type(13, 18, FontWeight.Normal, -0.02),
    labelLarge = type(14, 20, FontWeight.Bold, -0.02),
    labelMedium = type(12, 17, FontWeight.SemiBold, -0.02),
    labelSmall = type(12, 17, FontWeight.SemiBold, -0.02),
)

val MetricTypography = TextStyle(
    fontFamily = StepUpNumbers, fontWeight = FontWeight.Bold, lineHeight = 1.0.em,
    fontFeatureSettings = "tnum", letterSpacing = 0.sp,
    platformStyle = PlatformTextStyle(includeFontPadding = false),
)
