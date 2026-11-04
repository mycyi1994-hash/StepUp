package com.stepup.android.ui.screens.login

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import com.stepup.android.core.Analytics
import com.stepup.android.core.ServiceLocator
import com.stepup.android.sync.SessionUploadWorker
import kotlinx.coroutines.launch

/*
 * 안드로이드 로그인 — 구글 계정 선택(Credential Manager) → 서버 세션. 화면 모양은 공용 LoginContent(sharedUi).
 */

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

private fun Context.findActivity(): Activity? {
    var current: Context? = this
    while (current is ContextWrapper) {
        if (current is Activity) return current
        current = current.baseContext
    }
    return null
}
