package com.stepup.android.ui.screens.login

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import com.stepup.android.R
import com.stepup.android.core.Analytics
import com.stepup.android.core.ExternalIntents
import com.stepup.android.core.ServiceLocator
import com.stepup.android.sync.SessionUploadWorker
import com.stepup.android.ui.components.OnboardingArch
import com.stepup.android.ui.components.OnboardingNotice
import com.stepup.android.ui.components.OnboardingToast
import com.stepup.android.ui.components.S2Stage
import com.stepup.android.ui.components.Wordmark
import com.stepup.android.ui.components.settingsPalette
import com.stepup.android.ui.experience.feedbackClickable
import com.stepup.android.ui.theme.BrandLogoRole
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/** 약관 · 개인정보처리방침 — 기존 주소 그대로. 문서 내용은 앱이 새로 쓰거나 요약하지 않는다 */
private const val TERMS_URL = "https://stepupcrew.com/terms.html"
private const val PRIVACY_URL = "https://stepupcrew.com/privacy.html"

/**
 * 첫 진입 로그인.
 *
 * 로그인 수단은 구글 하나다. 기록이 서버에 쌓이려면 누구의 기록인지가 정해져야
 * 하고, 그걸 정할 방법이 이것뿐이다.
 *
 * 로그인에 성공하기 전에는 이 화면을 벗어날 수 없다. 예전에는 "게스트로
 * 둘러보기"가 있었지만, 게스트가 쌓은 기록은 기기를 바꾸는 순간 사라진다 —
 * 그 상태로 몇 달 모은 뒤에 잃는 것보다 지금 한 번 탭하는 편이 낫다.
 *
 * 시작·로그인·첫 사용 v1(시안 01 · 05~10): 계정 선택은 시스템 창(Credential Manager)이 그린다 — 앱은 그 위에 창을 띄우지 않는다.
 * 계정을 고른 뒤 서버가 확인하는 동안만 "로그인 중"(05)이고, 서버가 세션을 준 뒤에만 로그인 표시를 남긴다.
 * 취소는 조용히 01, 통신 오류는 연결 안내(06) — 계정 만료로 단정하지 않는다.
 */
@Composable
fun LoginScreen(onDone: () -> Unit) {
    var phase by remember { mutableStateOf(LoginPhase.Idle) }
    // 이번 시도의 결과 안내 — 없으면 기본(다시 로그인하러 왔으면 09, 아니면 01)
    var result by remember { mutableStateOf<LoginNotice?>(null) }
    val signInAgain by ServiceLocator.userPrefs.signInAgain.collectAsState(initial = false)
    val base = LoginRules.base(signInAgain)
    val scope = rememberCoroutineScope()
    val context = LocalContext.current

    fun signIn() {
        // 한 번에 하나 — 계정 선택 창 · 서버 확인 중에는 다시 누를 수 없다
        if (phase != LoginPhase.Idle) return
        val activity = context.findActivity()
        if (activity == null) {
            // 계정 선택 창을 띄울 화면이 없다. 정상 실행에서는 일어나지 않는다.
            result = LoginNotice.Failed
            return
        }

        phase = LoginPhase.Picking
        result = null
        scope.launch {
            try {
                when (val picked = LoginRules.afterPick(ServiceLocator.googleSignIn.requestIdToken(activity), base)) {
                    is LoginStep.Verify -> {
                        phase = LoginPhase.Verifying
                        val session = ServiceLocator.sessionHolder.signInWithGoogle(picked.idToken, picked.nonce)
                        when (val step = LoginRules.afterServer(session)) {
                            LoginStep.SignedIn -> {
                                ServiceLocator.userPrefs.setSignInAgain(false)
                                ServiceLocator.userPrefs.setLoginMethod("google")
                                Analytics.login()
                                // 이제 서버가 받아 준다 — 이 폰으로 알림을 보내도록 적어 둔다
                                ServiceLocator.pushRegistrar.syncInBackground()
                                // 이 계정의 서버 잔고 · 신발을 받아 온다 (첫 신발 · 무료 뽑기 10회 포함)
                                ServiceLocator.refreshEconomyInBackground()
                                // 로그인 전에 끝나 대기열에 남은 러닝을 올린다
                                runCatching { SessionUploadWorker.schedule(activity) }
                                onDone()
                            }
                            // 서버가 거절했으면 07(대개 설정 문제 — 클라이언트 ID 가 서버에 등록되지 않은 경우가 가장 흔하다),
                            // 서버에 닿지 못했으면 06
                            is LoginStep.Stay -> result = step.notice
                            is LoginStep.Verify -> Unit
                        }
                    }
                    // 취소 · 계정 없음 · 실패 — 취소는 오류를 띄우지 않는다(본인이 한 일이다)
                    is LoginStep.Stay -> result = picked.notice
                    LoginStep.SignedIn -> Unit
                }
            } catch (cancelled: kotlinx.coroutines.CancellationException) {
                throw cancelled
            } catch (_: Exception) {
                result = LoginNotice.Failed
            } finally {
                // 성공하면 화면이 바뀌지만, 그 사이 무엇이 터져도 버튼이
                // 영원히 도는 상태로 남으면 안 된다.
                phase = LoginPhase.Idle
            }
        }
    }

    LoginContent(phase = phase, notice = result ?: base, onSignIn = ::signIn)
}

/**
 * 로그인 화면 — 로고 → 큰 제목 → 한 줄 → 아치 강변 → 버튼 위 한 줄(시작 안내 · 확인 중 · 결과 안내) → Google 버튼 → 약관.
 * 한 스크롤 면이라 큰 글씨 · 작은 화면에서도 버튼과 약관 링크까지 닿는다.
 * 상태만 넣어 그릴 수 있다(시안 검사). 실제 상태는 [LoginScreen] 의 로그인 결과에서만 온다.
 */
@Composable
@OptIn(ExperimentalLayoutApi::class)
internal fun LoginContent(
    phase: LoginPhase,
    notice: LoginNotice?,
    onSignIn: () -> Unit,
    /** 시안 10 을 그대로 그려 볼 때만 — 실제로는 약관 링크를 열지 못했을 때 잠깐 뜬다 */
    legalFailedAtStart: Boolean = false,
) {
    val context = LocalContext.current
    val p = settingsPalette()
    var legalFailed by remember { mutableStateOf(legalFailedAtStart) }
    var legalFailures by remember { mutableIntStateOf(0) }
    LaunchedEffect(legalFailures) {
        if (legalFailures > 0) {
            delay(4_000)
            legalFailed = false
        }
    }
    // 여는 것 자체가 실패했을 때만 알린다 — 브라우저 안의 결과는 짐작하지 않는다. 돌아오면 로그인 화면은 그대로다
    fun openLegal(url: String) {
        if (ExternalIntents.tryOpenUrl(context, url)) {
            legalFailed = false
        } else {
            legalFailed = true
            legalFailures++
        }
    }
    val largeText = LocalDensity.current.fontScale > 1.2f
    val archHeight = loginArchHeight(LocalConfiguration.current.screenHeightDp, largeText)
    Box(Modifier.fillMaxSize()) {
        S2Stage(Modifier.fillMaxSize())
        Column(
            Modifier.fillMaxSize()
                .windowInsetsPadding(WindowInsets.safeDrawing)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 24.dp, vertical = 16.dp),
        ) {
            Spacer(Modifier.height(20.dp))
            Wordmark(role = BrandLogoRole.Header)
            Spacer(Modifier.height(40.dp))
            Text(
                stringResource(R.string.onb_login_headline), color = p.text, fontSize = 35.sp,
                fontWeight = FontWeight.SemiBold, lineHeight = 1.23.em, letterSpacing = (-0.028).em,
                modifier = Modifier.semantics { heading() },
            )
            Spacer(Modifier.height(16.dp))
            Text(stringResource(R.string.onb_login_subtitle), color = p.secondary, fontSize = 15.sp, lineHeight = 1.45.em)
            Spacer(Modifier.height(36.dp))
            // 아치 아래 한 칸 — 로그인 중이면 확인 중, 결과 안내가 있으면 그 안내, 아니면 시작 안내. 약관 링크를 열지 못했으면
            // 그 위에 잠깐 뜬다(시안 10). 안내 칸은 바닥색에 녹아드는 아치 아랫부분에 걸쳐 놓아, 어떤 상태든 Google 버튼 자리가 같다
            Box(Modifier.fillMaxWidth()) {
                OnboardingArch(
                    R.drawable.home_banner_blue_night,
                    Modifier.align(Alignment.TopCenter).height(archHeight).width(archHeight * ARCH_RATIO),
                )
                Box(
                    Modifier.fillMaxWidth().padding(top = archHeight - NOTICE_OVERLAP).heightIn(min = NOTICE_SLOT),
                    contentAlignment = Alignment.Center,
                ) {
                    when {
                        phase == LoginPhase.Verifying -> LoginCaption(
                            stringResource(R.string.onb_login_verifying),
                            Modifier.padding(top = NOTICE_OVERLAP).testTag("login-verifying")
                                .semantics { liveRegion = LiveRegionMode.Polite },
                        )
                        notice != null -> {
                            val (title, body) = noticeText(notice)
                            OnboardingNotice(title, body, Modifier.testTag("login-notice-${notice.name.lowercase()}"))
                        }
                        else -> LoginCaption(
                            stringResource(R.string.onb_login_caption),
                            Modifier.padding(top = NOTICE_OVERLAP).testTag("login-caption"),
                        )
                    }
                    if (legalFailed) {
                        OnboardingToast(
                            stringResource(R.string.onb_legal_failed_title), stringResource(R.string.onb_legal_failed_body),
                            Modifier.padding(horizontal = 10.dp).testTag("login-legal-failed"),
                        )
                    }
                }
            }
            Spacer(Modifier.height(17.dp))
            GoogleSignInButton(phase, onClick = {
                legalFailed = false
                onSignIn()
            })
            Spacer(Modifier.height(18.dp))
            Text(
                stringResource(R.string.login_terms), color = p.secondary, fontSize = 12.sp, lineHeight = 1.55.em,
                textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp),
            )
            // 큰 글씨 · 긴 번역이면 두 줄로 — 두 링크가 늘 화면 안에 있다
            FlowRow(
                Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(20.dp, Alignment.CenterHorizontally),
            ) {
                LegalLink(stringResource(R.string.login_terms_link), Modifier.testTag("login-terms")) { openLegal(TERMS_URL) }
                LegalLink(stringResource(R.string.login_privacy_link), Modifier.testTag("login-privacy")) { openLegal(PRIVACY_URL) }
            }
        }
    }
}

/** 아치 가로 : 세로 — 시안 238 × 319 */
private const val ARCH_RATIO = 238f / 319f

/** 버튼 위 안내 칸 — 시안의 안내 상자 높이(78). 아치 아랫부분(바닥색으로 녹는 곳)에 24 만큼 걸친다 */
private val NOTICE_SLOT = 78.dp
private val NOTICE_OVERLAP = 24.dp

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
    LoginNotice.Offline -> stringResource(R.string.onb_login_offline_title) to stringResource(R.string.onb_login_offline_body)
    LoginNotice.Failed -> stringResource(R.string.onb_login_failed_title) to stringResource(R.string.onb_login_failed_body)
    LoginNotice.NoAccount -> stringResource(R.string.onb_login_no_account_title) to stringResource(R.string.onb_login_no_account_body)
    LoginNotice.SignInAgain -> stringResource(R.string.onb_login_again_title) to stringResource(R.string.onb_login_again_body)
}

@Composable
private fun LoginCaption(text: String, modifier: Modifier = Modifier) {
    Text(
        text, color = settingsPalette().secondary, fontSize = 14.sp, lineHeight = 1.45.em,
        textAlign = TextAlign.Center, modifier = modifier.fillMaxWidth(),
    )
}

/** 약관 링크 — 글자는 작아도 누르는 곳은 48dp 이상 */
@Composable
private fun LegalLink(text: String, modifier: Modifier = Modifier, onClick: () -> Unit) {
    Box(
        modifier.heightIn(min = 48.dp).clip(RoundedCornerShape(12.dp)).feedbackClickable(onClick = onClick)
            .padding(horizontal = 10.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(text, color = settingsPalette().accent, fontSize = 13.sp)
    }
}

/**
 * Google 버튼 — 흰 면 · 공식 G · Google 글꼴 규칙(Roboto Medium 14) 그대로. 시안의 앱 글꼴로 바꾸지 않는다.
 * 계정 선택 창이 떠 있는 동안은 누를 수 없고, 계정을 고른 뒤 서버가 확인하는 동안(05)만 "로그인 중…"과 도는 표시.
 */
@Composable
private fun GoogleSignInButton(phase: LoginPhase, onClick: () -> Unit) {
    val verifying = phase == LoginPhase.Verifying
    androidx.compose.material3.OutlinedButton(
        onClick = onClick, enabled = phase == LoginPhase.Idle,
        modifier = Modifier.fillMaxWidth().heightIn(min = 54.dp).testTag("login-google"),
        shape = RoundedCornerShape(50),
        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF747775)),
        colors = androidx.compose.material3.ButtonDefaults.outlinedButtonColors(
            containerColor = Color.White, contentColor = Color(0xFF1F1F1F),
            disabledContainerColor = Color.White, disabledContentColor = Color(0xFF1F1F1F),
        ),
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
    ) {
        androidx.compose.foundation.Image(
            painter = androidx.compose.ui.res.painterResource(R.drawable.google_g),
            contentDescription = null, modifier = Modifier.size(20.dp),
        )
        Spacer(Modifier.size(10.dp))
        Text(stringResource(if (verifying) R.string.login_google_progress else R.string.login_google),
            fontSize = 14.sp, fontWeight = FontWeight.Medium,
            fontFamily = androidx.compose.ui.text.font.FontFamily.SansSerif,
            modifier = Modifier.weight(1f), textAlign = TextAlign.Center)
        // 도는 표시가 없을 때도 같은 자리를 비워 둔다 — 글자가 가운데에서 흔들리지 않게
        Box(Modifier.size(20.dp), contentAlignment = Alignment.Center) {
            if (verifying) CircularProgressIndicator(Modifier.size(18.dp), color = Color(0xFF33465E), strokeWidth = 2.dp)
        }
    }
}

/**
 * 계정 선택 창을 띄우려면 화면(Activity)이 필요하다.
 *
 * Compose 의 LocalContext 가 늘 Activity 인 것은 아니다 — 테마를 감싸는
 * 래퍼가 끼면 ContextWrapper 가 온다. 벗겨 내며 찾는다.
 */
private fun Context.findActivity(): Activity? {
    var current: Context? = this
    while (current is ContextWrapper) {
        if (current is Activity) return current
        current = current.baseContext
    }
    return null
}
