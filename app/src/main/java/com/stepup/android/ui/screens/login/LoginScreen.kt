package com.stepup.android.ui.screens.login

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.stepup.android.R
import com.stepup.android.core.Analytics
import com.stepup.android.core.ServiceLocator
import com.stepup.android.data.remote.GoogleIdResult
import com.stepup.android.data.remote.TokenResult
import com.stepup.android.ui.components.Wordmark
import com.stepup.android.ui.theme.Alert
import com.stepup.android.ui.theme.Silver
import com.stepup.android.ui.theme.Snow
import kotlinx.coroutines.launch

/**
 * 첫 진입 로그인.
 *
 * 로그인 수단은 구글 하나다. 기록이 서버에 쌓이려면 누구의 기록인지가 정해져야
 * 하고, 그걸 정할 방법이 이것뿐이다.
 *
 * 로그인에 성공하기 전에는 이 화면을 벗어날 수 없다. 예전에는 "게스트로
 * 둘러보기"가 있었지만, 게스트가 쌓은 기록은 기기를 바꾸는 순간 사라진다 —
 * 그 상태로 몇 달 모은 뒤에 잃는 것보다 지금 한 번 탭하는 편이 낫다.
 */
@Composable
fun LoginScreen(onDone: () -> Unit) {
    var signingIn by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<Int?>(null) }
    val scope = rememberCoroutineScope()
    val context = LocalContext.current

    fun signIn() {
        if (signingIn) return
        val activity = context.findActivity()
        if (activity == null) {
            // 계정 선택 창을 띄울 화면이 없다. 정상 실행에서는 일어나지 않는다.
            error = R.string.login_failed
            return
        }

        signingIn = true
        error = null
        scope.launch {
            try {
                when (val id = ServiceLocator.googleSignIn.requestIdToken(activity)) {
                    is GoogleIdResult.Ok -> {
                        when (val session =
                            ServiceLocator.sessionHolder.signInWithGoogle(id.idToken, id.nonce)) {
                            is TokenResult.Ok -> {
                                ServiceLocator.userPrefs.setLoginMethod("google")
                                Analytics.login()
                                // 이제 서버가 받아 준다 — 이 폰으로 알림을 보내도록 적어 둔다
                                ServiceLocator.pushRegistrar.syncInBackground()
                                onDone()
                            }
                            // 서버가 토큰을 거절했다. 대개 설정 문제다 —
                            // 클라이언트 ID 가 서버에 등록되지 않은 경우가 가장 흔하다.
                            is TokenResult.SignInRequired -> error = R.string.login_failed
                            is TokenResult.Unavailable -> error = R.string.login_offline
                        }
                    }

                    // 사용자가 창을 닫았다. 본인이 한 일이므로 오류를 띄우지 않는다.
                    GoogleIdResult.Cancelled -> Unit

                    is GoogleIdResult.NoAccount -> error = R.string.login_no_account
                    is GoogleIdResult.Failed -> error = R.string.login_failed
                }
            } catch (cancelled: kotlinx.coroutines.CancellationException) {
                throw cancelled
            } catch (_: Exception) {
                error = R.string.login_failed
            } finally {
                // 성공하면 화면이 바뀌지만, 그 사이 무엇이 터져도 버튼이
                // 영원히 도는 상태로 남으면 안 된다.
                signingIn = false
            }
        }
    }

    LoginContent(signingIn = signingIn, error = error, onSignIn = ::signIn)
}

/** Production presentation, also rendered directly for loading/error accessibility checks. */
@Composable
@OptIn(androidx.compose.foundation.layout.ExperimentalLayoutApi::class)
internal fun LoginContent(signingIn: Boolean, error: Int?, onSignIn: () -> Unit) {
    val context = LocalContext.current
    Box(Modifier.fillMaxSize()) {
        com.stepup.android.ui.components.RunnerScene(Modifier.fillMaxSize())
        Column(
            Modifier.fillMaxSize()
                .windowInsetsPadding(WindowInsets.safeDrawing)
                .padding(horizontal = com.stepup.android.ui.theme.StepUpDesign.Gutter, vertical = 16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Wordmark(role = com.stepup.android.ui.theme.BrandLogoRole.Launch)
            Box(Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
                androidx.compose.foundation.Image(
                    painter = androidx.compose.ui.res.painterResource(R.drawable.community_warmup),
                    contentDescription = null,
                    modifier = Modifier.fillMaxSize().padding(vertical = 12.dp),
                    contentScale = androidx.compose.ui.layout.ContentScale.Fit,
                )
            }
            // Scroll only when font expansion needs more room; login and legal links remain reachable.
            Column(Modifier.weight(1f, fill = false).verticalScroll(rememberScrollState())) {
                com.stepup.android.ui.components.GlowCard(contentPadding = PaddingValues(24.dp), spacing = 18.dp) {
                    Text(stringResource(R.string.login_headline), color = Snow,
                        style = MaterialTheme.typography.headlineSmall,
                        textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth())
                    GoogleSignInButton(signingIn, onSignIn)
                    error?.let {
                        Text(stringResource(it), color = Alert, fontSize = 14.sp,
                            modifier = Modifier.fillMaxWidth().testTag("login-error")
                                .semantics { liveRegion = LiveRegionMode.Polite },
                            textAlign = TextAlign.Center)
                    }
                    Text(stringResource(R.string.login_terms), color = Silver, fontSize = 14.sp,
                        textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth())
                    androidx.compose.foundation.layout.FlowRow(
                        horizontalArrangement = Arrangement.Center,
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        androidx.compose.material3.TextButton(modifier = Modifier.testTag("login-terms"), onClick = {
                            com.stepup.android.core.ExternalIntents.openUrl(context, "https://stepupcrew.com/terms.html")
                        }) { Text(stringResource(R.string.login_terms_link)) }
                        androidx.compose.material3.TextButton(modifier = Modifier.testTag("login-privacy"), onClick = {
                            com.stepup.android.core.ExternalIntents.openUrl(context, "https://stepupcrew.com/privacy.html")
                        }) { Text(stringResource(R.string.login_privacy_link)) }
                    }
                }
            }
        }
    }
}

@Composable
private fun GoogleSignInButton(signingIn: Boolean, onClick: () -> Unit) {
    androidx.compose.material3.OutlinedButton(
        onClick = onClick, enabled = !signingIn,
        modifier = Modifier.fillMaxWidth().heightIn(min = com.stepup.android.ui.theme.StepUpDesign.PrimaryHeight)
            .testTag("login-google"),
        shape = RoundedCornerShape(50),
        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF747775)),
        colors = androidx.compose.material3.ButtonDefaults.outlinedButtonColors(
            containerColor = Color.White, contentColor = Color(0xFF1F1F1F),
            disabledContainerColor = Color.White, disabledContentColor = Color(0xFF1F1F1F),
        ),
        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 12.dp),
    ) {
        androidx.compose.foundation.Image(
            painter = androidx.compose.ui.res.painterResource(R.drawable.google_g),
            contentDescription = null, modifier = Modifier.size(20.dp),
        )
        Spacer(Modifier.size(10.dp))
        Text(stringResource(if (signingIn) R.string.login_google_progress else R.string.login_google),
            fontSize = 14.sp, fontWeight = FontWeight.Medium,
            fontFamily = androidx.compose.ui.text.font.FontFamily.SansSerif,
            modifier = Modifier.weight(1f), textAlign = TextAlign.Center)
        if (signingIn) CircularProgressIndicator(Modifier.size(18.dp), strokeWidth = 2.dp)
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
