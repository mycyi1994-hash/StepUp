package com.stepup.android.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.LineHeightStyle
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import com.stepup.ui.res.Res
import com.stepup.ui.res.paperlogy_4regular
import com.stepup.ui.res.paperlogy_5medium
import com.stepup.ui.res.paperlogy_6semibold
import com.stepup.ui.res.paperlogy_7bold
import com.stepup.ui.res.paperlogy_8extrabold
import com.stepup.ui.res.saira_condensed_semibold
import org.jetbrains.compose.resources.Font

/*
 * 글꼴 · 글자 크기(2026-11-03 iOS C0) — 안드로이드 app/.../ui/theme/Type.kt 와 같은 값. 글꼴 파일도 같다(빌드할 때 res/font 에서 가져온다).
 * Compose Multiplatform 의 글꼴은 화면 안에서 읽어(@Composable) 만든다 — 안드로이드처럼 맨 위 값(val StepUpSans)으로 둘 수 없다.
 * 안드로이드에만 있는 줄 바꿈(낱말 단위 · includeFontPadding)은 iOS 에 같은 뜻의 설정이 없어 뺐다.
 */

/**
 * 지금 쓰는 글꼴 — 테마([StepUpTheme])가 글꼴 파일을 읽어 넣는다(안드로이드의 맨 위 값 StepUpSans · StepUpNumbers 자리).
 * 색 팔레트(applyPalette)와 같은 방식이라 화면 코드가 @Composable 밖(글자 모양 표)에서도 읽는다. 바뀌면 다시 그린다.
 */
object StepUpFonts {
    var sans: FontFamily by mutableStateOf(FontFamily.Default)
    var numbers: FontFamily by mutableStateOf(FontFamily.Default)
}

/** 테마가 부른다 — 글꼴을 읽어 [StepUpFonts] 에 넣는다 */
@Composable
fun applyFonts() {
    val sans = stepUpSans()
    val numbers = stepUpNumbers()
    if (StepUpFonts.sans != sans) StepUpFonts.sans = sans
    if (StepUpFonts.numbers != numbers) StepUpFonts.numbers = numbers
}

/** 한글 — 페이퍼로지. 같은 굵기 숫자에서 한 단계 무거워 보여 한 칸씩 낮춰 잇는다(안드로이드와 같은 표) */
@Composable
fun stepUpSans(): FontFamily = FontFamily(
    Font(Res.font.paperlogy_4regular, FontWeight.Normal),
    Font(Res.font.paperlogy_5medium, FontWeight.Medium),
    Font(Res.font.paperlogy_5medium, FontWeight.SemiBold),
    Font(Res.font.paperlogy_6semibold, FontWeight.Bold),
    Font(Res.font.paperlogy_7bold, FontWeight.ExtraBold),
    Font(Res.font.paperlogy_8extrabold, FontWeight.Black),
)

/** 숫자 — Saira Condensed SemiBold 한 파일(가짜 굵게를 만들지 않게 굵기마다 같은 파일) */
@Composable
fun stepUpNumbers(): FontFamily {
    val semibold = Font(Res.font.saira_condensed_semibold, FontWeight.SemiBold)
    return FontFamily(
        Font(Res.font.saira_condensed_semibold, FontWeight.Normal),
        Font(Res.font.saira_condensed_semibold, FontWeight.Medium),
        semibold,
        Font(Res.font.saira_condensed_semibold, FontWeight.Bold),
        Font(Res.font.saira_condensed_semibold, FontWeight.ExtraBold),
        Font(Res.font.saira_condensed_semibold, FontWeight.Black),
    )
}

/** 자간은 em — 본문 −2.5% · 제목 −3.5~−4.5% · 13sp 이하 −2%(tokens.json fonts.letterSpacing) */
private fun type(family: FontFamily, size: Int, height: Int, weight: FontWeight, tracking: Double) = TextStyle(
    fontFamily = family, fontWeight = weight, fontSize = size.sp, lineHeight = (height.toFloat() / size).em,
    fontFeatureSettings = "tnum", letterSpacing = tracking.em,
)

@Composable
fun stepUpTypography(): Typography {
    val sans = stepUpSans()
    return remember(sans) {
        Typography(
            displayLarge = type(sans, 56, 62, FontWeight.ExtraBold, -0.045),
            displayMedium = type(sans, 44, 50, FontWeight.ExtraBold, -0.045),
            displaySmall = type(sans, 34, 42, FontWeight.ExtraBold, -0.045),
            headlineLarge = type(sans, 32, 40, FontWeight.ExtraBold, -0.04),
            headlineMedium = type(sans, 28, 36, FontWeight.ExtraBold, -0.04),
            headlineSmall = type(sans, 24, 32, FontWeight.ExtraBold, -0.04),
            titleLarge = type(sans, 20, 28, FontWeight.ExtraBold, -0.04),
            titleMedium = type(sans, 17, 24, FontWeight.Bold, -0.03),
            titleSmall = type(sans, 15, 21, FontWeight.Bold, -0.03),
            bodyLarge = type(sans, 16, 22, FontWeight.Normal, -0.025),
            bodyMedium = type(sans, 15, 21, FontWeight.Normal, -0.025),
            bodySmall = type(sans, 13, 18, FontWeight.Normal, -0.02),
            labelLarge = type(sans, 14, 20, FontWeight.Bold, -0.02),
            labelMedium = type(sans, 12, 17, FontWeight.SemiBold, -0.02),
            labelSmall = type(sans, 12, 17, FontWeight.SemiBold, -0.02),
        )
    }
}

/** 큰 숫자 — 줄 상자 가운데 맞춤(안드로이드 MetricTypography 와 같다) */
@Composable
fun metricTypography(): TextStyle {
    val numbers = stepUpNumbers()
    return remember(numbers) {
        TextStyle(
            fontFamily = numbers, fontWeight = FontWeight.Bold, lineHeight = 1.0.em,
            fontFeatureSettings = "tnum", letterSpacing = 0.sp,
            lineHeightStyle = LineHeightStyle(LineHeightStyle.Alignment.Center, LineHeightStyle.Trim.None),
        )
    }
}
