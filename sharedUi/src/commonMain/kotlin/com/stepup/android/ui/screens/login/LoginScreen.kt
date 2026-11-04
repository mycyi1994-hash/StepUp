package com.stepup.android.ui.screens.login

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import org.jetbrains.compose.resources.stringResource
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import com.stepup.ui.res.*
import org.jetbrains.compose.resources.painterResource
import com.stepup.android.ui.components.RunBackdrop
import com.stepup.android.ui.components.Wordmark
import com.stepup.android.ui.components.runTextStyle
import com.stepup.android.ui.components.runTone
import com.stepup.android.ui.theme.BrandLogoRole
import com.stepup.android.ui.theme.StepUpFonts
import kotlinx.coroutines.launch
import com.stepup.android.ui.components.StopBrush

/** 약관 · 개인정보처리방침 — 기존 주소 그대로. 문서 내용은 앱이 새로 쓰거나 요약하지 않는다 */


/**
 * 로그인 화면(파란 톤 v4 ONB01 · ONB05~09) — 로고 → 큰 제목 → 한 줄 → 아치 강변 → 버튼 위 안내 칸 → Google 버튼.
 * 아래 동의 문구 · 약관 링크는 뺐다(2026-10-29 사용자 — 이용약관 · 개인정보처리방침은 내 정보 › 설정 › 약관 및 정책).
 * 안내 칸은 가장 긴 문구(제목 + 두 줄) 높이를 늘 잡아 두어, 상태가 바뀌어도 Google 버튼 자리가 움직이지 않는다.
 * 한 스크롤 면이라 큰 글씨 · 작은 화면에서도 버튼까지 닿는다(사진부터 줄인다).
 * 상태만 넣어 그릴 수 있다(시안 검사). 실제 상태는 [LoginScreen] 의 로그인 결과에서만 온다.
 */
@Composable
@OptIn(ExperimentalLayoutApi::class)
fun LoginContent(
    phase: LoginPhase,
    notice: LoginNotice?,
    onSignIn: () -> Unit,
    /** iOS — Apple 로그인(App Store 심사 4.8: 구글 로그인을 두면 Apple 로그인도 같은 자리에). 안드로이드는 null(버튼 없음) */
    onApple: (() -> Unit)? = null,
) {
    val t = runTone()
    val largeText = LocalDensity.current.fontScale > 1.2f
    val archHeight = loginArchHeight(with(androidx.compose.ui.platform.LocalDensity.current) { androidx.compose.ui.platform.LocalWindowInfo.current.containerSize.height.toDp().value.toInt() }, largeText)
    Box(Modifier.fillMaxSize()) {
        RunBackdrop(Modifier.fillMaxSize())
        Column(
            Modifier.fillMaxSize()
                .windowInsetsPadding(WindowInsets.safeDrawing)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 24.dp, vertical = 16.dp),
        ) {
            Spacer(Modifier.height(20.dp))
            Wordmark(role = BrandLogoRole.Header)
            Spacer(Modifier.height(36.dp))
            Text(
                stringResource(Res.string.onb_login_headline),
                style = TextStyle(
                    fontFamily = StepUpFonts.sans, fontWeight = FontWeight.ExtraBold, fontSize = 38.sp, lineHeight = 1.25.em,
                    letterSpacing = (-0.03).em, color = t.text,
                ),
                modifier = Modifier.semantics { heading() },
            )
            Spacer(Modifier.height(16.dp))
            Text(stringResource(Res.string.onb_login_subtitle), style = runTextStyle(17.sp, t.label, FontWeight.Medium, 1.45f))
            Spacer(Modifier.height(30.dp))
            // 아치 아래 한 칸 — 로그인 중이면 확인 중, 결과 안내가 있으면 그 안내, 아니면 시작 안내.
            // 안내 칸은 바닥색으로 녹아드는 아치 아랫부분에 걸쳐 놓아, 어떤 상태든 Google 버튼 자리가 같다
            Box(Modifier.fillMaxWidth()) {
                LoginArch(
                    Res.drawable.home_banner_blue_night,
                    Modifier.align(Alignment.TopCenter).height(archHeight).width(archHeight * ARCH_RATIO),
                )
                Box(
                    Modifier.fillMaxWidth().padding(top = archHeight - NOTICE_OVERLAP).heightIn(min = NOTICE_SLOT),
                    contentAlignment = Alignment.BottomCenter,
                ) {
                    when {
                        phase == LoginPhase.Verifying -> LoginCaption(
                            stringResource(Res.string.onb_login_verifying),
                            Modifier.testTag("login-verifying").semantics { liveRegion = LiveRegionMode.Polite },
                        )
                        notice != null -> {
                            val (title, body) = noticeText(notice)
                            LoginSlotNotice(title, body, Modifier.testTag("login-notice-${notice.name.lowercase()}"))
                        }
                        else -> LoginCaption(stringResource(Res.string.onb_login_caption), Modifier.testTag("login-caption"))
                    }
                }
            }
            Spacer(Modifier.height(14.dp))
            if (onApple != null) {
                AppleSignInButton(phase, onClick = onApple)
                Spacer(Modifier.height(12.dp))
            }
            GoogleSignInButton(phase, onClick = onSignIn)
            Spacer(Modifier.height(18.dp))
        }
    }
}

/** 아치 가로 : 세로 — 시안 238 × 319 */
private const val ARCH_RATIO = 238f / 319f

/** 버튼 위 안내 칸 — 가장 긴 안내(굵은 제목 한 줄 + 설명 두 줄) 높이. 아치 아랫부분(바닥색으로 녹는 곳)에 걸친다 */
private val NOTICE_SLOT = 112.dp
private val NOTICE_OVERLAP = 36.dp

/** 아치 높이 — 작은 화면 · 큰 글씨에서 풍경부터 줄여 버튼과 약관이 먼저 보이게 한다 */
private fun loginArchHeight(screenHeightDp: Int, largeText: Boolean): Dp = when {
    largeText -> 150.dp
    screenHeightDp < 700 -> 170.dp
    screenHeightDp < 780 -> 220.dp
    screenHeightDp < 860 -> 270.dp
    else -> 310.dp
}

@Composable
private fun noticeText(notice: LoginNotice): Pair<String, String> = when (notice) {
    LoginNotice.Offline -> stringResource(Res.string.onb_login_offline_title) to stringResource(Res.string.onb_login_offline_body)
    LoginNotice.Failed -> stringResource(Res.string.onb_login_failed_title) to stringResource(Res.string.onb_login_failed_body)
    LoginNotice.NoAccount -> stringResource(Res.string.onb_login_no_account_title) to stringResource(Res.string.onb_login_no_account_body)
    LoginNotice.SignInAgain -> stringResource(Res.string.onb_login_again_title) to stringResource(Res.string.onb_login_again_body)
}

@Composable
private fun LoginCaption(text: String, modifier: Modifier = Modifier) {
    Text(
        text, style = runTextStyle(16.sp, runTone().label, FontWeight.Medium, 1.45f),
        textAlign = TextAlign.Center, modifier = modifier.fillMaxWidth().padding(bottom = 4.dp),
    )
}

/** 버튼 위 안내(06~10) — 가운데 굵은 한 줄과 설명. 상자 없이 아치 아래 바닥에 얹는다. 스크린리더가 바로 읽는다 */
@Composable
private fun LoginSlotNotice(title: String, body: String, modifier: Modifier = Modifier) {
    val t = runTone()
    Column(
        modifier.fillMaxWidth().semantics(mergeDescendants = true) { liveRegion = LiveRegionMode.Polite }
            .padding(horizontal = 12.dp).padding(bottom = 2.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        Text(title, style = runTextStyle(20.sp, t.text, FontWeight.Bold, 1.3f), textAlign = TextAlign.Center)
        Text(body, style = runTextStyle(15.sp, t.label, FontWeight.Medium, 1.5f), textAlign = TextAlign.Center)
    }
}

/**
 * 아치 풍경 — 위가 반원인 틀에 기존 강변 사진(home_banner_blue_night)을 담고 얇은 파란 테를 두른다.
 * 아래로 바닥색에 녹아든다. 지도나 현재 위치가 아니다 — 장식이라 읽지 않는다.
 */
@Composable
private fun LoginArch(image: org.jetbrains.compose.resources.DrawableResource, modifier: Modifier = Modifier) {
    val t = runTone()
    val shape = RoundedCornerShape(topStartPercent = 50, topEndPercent = 50)
    Box(
        modifier.clip(shape).background(t.inset)
            .border(
                1.5.dp,
                StopBrush.verticalGradient(0f to t.cobalt, 0.7f to t.cobalt.copy(alpha = 0.5f), 1f to Color.Transparent),
                shape,
            ),
    ) {
        androidx.compose.foundation.Image(
            painter = painterResource(image), contentDescription = null,
            contentScale = androidx.compose.ui.layout.ContentScale.Crop, modifier = Modifier.fillMaxSize(),
        )
        Box(
            Modifier.fillMaxSize().background(
                StopBrush.verticalGradient(0f to Color.Transparent, 0.72f to t.screen.copy(alpha = 0.10f), 1f to t.screenBottom),
            ),
        )
    }
}

/**
 * Google 버튼 — 흰 면 · 공식 G · Google 글꼴 규칙(Roboto Medium) 그대로. 앱의 기울어진 게임식 주 버튼으로 바꾸지 않는다.
 * 계정 선택 창이 떠 있는 동안은 누를 수 없고, 계정을 고른 뒤 서버가 확인하는 동안(05)만 옅은 면 · 도는 표시 하나 · "로그인 중…".
 */
@Composable
private fun GoogleSignInButton(phase: LoginPhase, onClick: () -> Unit) {
    val verifying = phase == LoginPhase.Verifying
    androidx.compose.material3.OutlinedButton(
        onClick = onClick, enabled = phase == LoginPhase.Idle,
        modifier = Modifier.fillMaxWidth().heightIn(min = 58.dp).testTag("login-google"),
        shape = RoundedCornerShape(18.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, if (verifying) Color(0xFF9FB3D4) else Color(0xFFD7DEEA)),
        colors = androidx.compose.material3.ButtonDefaults.outlinedButtonColors(
            containerColor = Color.White, contentColor = Color(0xFF1F1F1F),
            // 서버 확인 중(05) — 시안의 옅은 파란 회색 면. 계정 선택 창이 떠 있는 동안(Picking)은 흰 면 그대로 잠근다
            disabledContainerColor = if (verifying) Color(0xFFA9BCDB) else Color.White,
            disabledContentColor = Color(0xFF1F1F1F),
        ),
        contentPadding = PaddingValues(horizontal = 18.dp, vertical = 12.dp),
    ) {
        androidx.compose.foundation.Image(
            painter = org.jetbrains.compose.resources.painterResource(Res.drawable.google_g),
            contentDescription = null, modifier = Modifier.size(24.dp),
        )
        Row(Modifier.weight(1f), horizontalArrangement = Arrangement.Center, verticalAlignment = Alignment.CenterVertically) {
            if (verifying) {
                CircularProgressIndicator(Modifier.size(20.dp), color = Color(0xFF0754FF), strokeWidth = 2.5.dp,
                    trackColor = Color.White.copy(alpha = 0.6f))
                Spacer(Modifier.width(12.dp))
            }
            Text(
                stringResource(if (verifying) Res.string.login_google_progress else Res.string.login_google),
                fontSize = 16.sp, fontWeight = FontWeight.Medium,
                fontFamily = androidx.compose.ui.text.font.FontFamily.SansSerif,
                textAlign = TextAlign.Center,
            )
        }
        // G 와 같은 폭을 오른쪽에도 비워 둔다 — 글자가 버튼 가운데에 선다
        Spacer(Modifier.size(24.dp))
    }
}

/**
 * Apple 로그인 버튼(iOS) — Apple 디자인 지침의 검은 면 · 흰 글자 · Apple 로고(시스템 글꼴의  글자), 구글 버튼과 같은 크기 · 모양.
 */
@Composable
private fun AppleSignInButton(phase: LoginPhase, onClick: () -> Unit) {
    androidx.compose.material3.Button(
        onClick = onClick, enabled = phase == LoginPhase.Idle,
        modifier = Modifier.fillMaxWidth().heightIn(min = 58.dp).testTag("login-apple"),
        shape = RoundedCornerShape(18.dp),
        colors = androidx.compose.material3.ButtonDefaults.buttonColors(
            containerColor = Color.Black, contentColor = Color.White,
            disabledContainerColor = Color(0xFF3A3A3C), disabledContentColor = Color.White,
        ),
        contentPadding = PaddingValues(horizontal = 18.dp, vertical = 12.dp),
    ) {
        Text(
            "\uF8FF  " + stringResource(Res.string.login_apple),
            fontSize = 17.sp, fontWeight = FontWeight.SemiBold,
            fontFamily = androidx.compose.ui.text.font.FontFamily.Default,
            textAlign = TextAlign.Center,
        )
    }
}
