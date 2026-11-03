package com.stepup.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import com.stepup.android.data.remote.AccessApi
import com.stepup.android.data.remote.AccessStatusRow
import com.stepup.android.data.remote.IosAuth
import com.stepup.android.data.remote.ServerResult
import com.stepup.android.ui.screens.access.AccessCodeScreen
import com.stepup.android.data.remote.IosSignInResult
import com.stepup.android.ui.theme.StepUpTheme
import com.stepup.android.ui.screens.login.LoginContent
import com.stepup.android.ui.screens.login.LoginNotice
import com.stepup.android.ui.screens.login.LoginPhase
import kotlinx.coroutines.launch

/** iOS 앱의 차례 — 세션 확인 중 → 로그인 → 앱 안(하단 탭은 다음 단계에서 옮긴다) */
private enum class Stage { Checking, SignedOut, AccessCode, SignedIn, ShoeCheck, Starting, Running }

/**
 * iOS 맨 위 화면(안드로이드 StepUpRoot 의 로그인 단계 자리). 로그인 화면은 안드로이드와 같은 LoginContent 에 Apple 버튼을 더한 것.
 * 결과 안내도 안드로이드와 같다 — 취소는 아무것도 띄우지 않고, 서버가 거절하면 Failed, 닿지 못하면 Offline, 설정이 없으면 Failed.
 */
@Composable
fun IosRoot() = StepUpTheme { Box(Modifier.fillMaxSize()) { IosRootContent(); com.stepup.android.ui.components.ToastHost() } }

@Composable
private fun IosRootContent() {
    var stage by remember { mutableStateOf(Stage.Checking) }
    var phase by remember { mutableStateOf(LoginPhase.Idle) }
    var notice by remember { mutableStateOf<LoginNotice?>(null) }
    val scope = rememberCoroutineScope()
    var access by remember { mutableStateOf<AccessStatusRow?>(null) }
    val accessApi = remember { AccessApi(IosApp.server) }

    /** 로그인한 사람의 문 — 안드로이드 AccessGate 와 같은 규칙: 문이 OPEN 이 아니고 권한이 없으면 접속 코드. 못 물으면 들여보낸다(서버가 막는다) */
    suspend fun afterSignIn() {
        val status = (accessApi.status() as? ServerResult.Ok)?.value
        access = status
        stage = if (status != null && status.mode != "OPEN" && !status.granted) Stage.AccessCode else Stage.SignedIn
        // 신발 · 잔고를 서버에서 받아 둔다(안드로이드 EconomySync 와 같다) — 못 받아도 앱은 열린다
        if (stage == Stage.SignedIn) scope.launch { runCatching { IosApp.economySync.refresh() } }
    }

    LaunchedEffect(Unit) {
        // 러닝 올리기를 이어 둔다 — 앞에 올 때 · 백그라운드 작업이 같은 규칙(UploadPass)으로 올린다
        IosApp.configureMap()
        IosApp.connectServices(onReturnToSignIn = {
            IosRunSession.stop()
            IosAuth.signOut(IosApp.sessionStore)
            stage = Stage.SignedOut
        })
        IosApp.connectUploads()
        if (IosApp.sessionStore.load() != null) afterSignIn() else stage = Stage.SignedOut
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
                    is IosSignInResult.Ok -> afterSignIn()
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
        Stage.AccessCode -> AccessCodeScreen(
            status = access ?: AccessStatusRow(mode = "CLOSED_BETA", granted = false),
            signedIn = true,
            onGranted = { scope.launch { afterSignIn() } },
            onSignOut = { scope.launch { IosAuth.signOut(IosApp.sessionStore); stage = Stage.SignedOut } },
            redeem = { accessApi.redeem(it) },
            showApple = true,
        )
        Stage.SignedIn, Stage.ShoeCheck -> Box(Modifier.fillMaxSize()) {
            IosMain(onStartRun = { stage = Stage.ShoeCheck })
            // 자유 러닝 신발 점검 — 안드로이드와 같은 점검창(이 신발로 출발 · 돌아가기 · 신발 교체, 신발은 1초 꾹)
            if (stage == Stage.ShoeCheck) IosShoeCheck(onStart = { stage = Stage.Starting }, onCancel = { stage = Stage.SignedIn })
        }
        Stage.Starting, Stage.Running -> {
            val session by IosRunSession.state.collectAsState()
            Box(Modifier.fillMaxSize()) {
                if (session != null || stage == Stage.Running) {
                    IosFreeRun(onClose = {
                        stage = Stage.SignedIn
                        // 방금 끝난 러닝을 바로 올려 본다(안 되면 다음 차례에)
                        IosUploads.run { }
                    })
                }
                // 3 · 2 · 1 · 0 → StepUP! → 지도 펼침 — 안드로이드와 같은 연출. 펼칠 때 러닝을 켠다
                if (stage == Stage.Starting) {
                    com.stepup.android.ui.screens.walk.FreeRunStartSequence(
                        sessionActive = session != null,
                        startRefusals = 0,
                        onStartSession = { IosRunSession.start(); true },
                        onFinished = { stage = Stage.Running },
                        onFailed = { stage = Stage.SignedIn },
                        onCancel = { stage = Stage.SignedIn },
                    )
                }
            }
        }
    }
}
