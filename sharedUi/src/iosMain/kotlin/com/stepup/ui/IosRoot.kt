package com.stepup.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import com.stepup.android.data.remote.IosAuth
import com.stepup.android.data.remote.IosSignInResult
import com.stepup.android.ui.theme.StepUpTheme
import com.stepup.android.ui.screens.login.LoginContent
import com.stepup.android.ui.screens.login.LoginNotice
import com.stepup.android.ui.screens.login.LoginPhase
import kotlinx.coroutines.launch

/** iOS 앱의 차례 — 세션 확인 중 → 로그인 → 앱 안(하단 탭은 다음 단계에서 옮긴다) */
private enum class Stage { Checking, SignedOut, SignedIn }

/**
 * iOS 맨 위 화면(안드로이드 StepUpRoot 의 로그인 단계 자리). 로그인 화면은 안드로이드와 같은 LoginContent 에 Apple 버튼을 더한 것.
 * 결과 안내도 안드로이드와 같다 — 취소는 아무것도 띄우지 않고, 서버가 거절하면 Failed, 닿지 못하면 Offline, 설정이 없으면 Failed.
 */
@Composable
fun IosRoot() = StepUpTheme {
    var stage by remember { mutableStateOf(Stage.Checking) }
    var phase by remember { mutableStateOf(LoginPhase.Idle) }
    var notice by remember { mutableStateOf<LoginNotice?>(null) }
    val scope = rememberCoroutineScope()
    LaunchedEffect(Unit) {
        stage = if (IosApp.sessionStore.load() != null) Stage.SignedIn else Stage.SignedOut
    }

    fun signIn(apple: Boolean) {
        if (phase != LoginPhase.Idle) return
        phase = LoginPhase.Picking
        notice = null
        scope.launch {
            try {
                val result = if (apple) IosAuth.signInWithApple(IosApp.auth, IosApp.sessionStore)
                else IosAuth.signInWithGoogle(IosApp.auth, IosApp.sessionStore)
                when (result) {
                    is IosSignInResult.Ok -> stage = Stage.SignedIn
                    IosSignInResult.Cancelled -> Unit
                    is IosSignInResult.NotConfigured -> notice = LoginNotice.Failed
                    is IosSignInResult.Failed -> notice = if (result.retry) LoginNotice.Offline else LoginNotice.Failed
                }
            } finally {
                phase = LoginPhase.Idle
            }
        }
    }

    when (stage) {
        Stage.Checking -> Box(Modifier.fillMaxSize().background(com.stepup.android.ui.theme.StepUpColors.night))
        Stage.SignedOut -> LoginContent(phase = phase, notice = notice, onSignIn = { signIn(apple = false) }, onApple = { signIn(apple = true) })
        Stage.SignedIn -> StepUpRoot()
    }
}
