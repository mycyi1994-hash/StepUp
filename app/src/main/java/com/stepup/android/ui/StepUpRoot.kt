package com.stepup.android.ui


import androidx.compose.animation.*
import androidx.compose.animation.core.tween
import com.stepup.android.ui.experience.*
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import kotlinx.coroutines.launch
import com.stepup.android.core.blocked
import com.stepup.android.core.ServiceLocator
import com.stepup.android.ui.components.NightCanvas
import com.stepup.android.ui.screens.login.LoginScreen
import com.stepup.android.ui.screens.splash.SplashScreen


@Composable
fun StepUpRoot() {
    var ready by rememberSaveable { mutableStateOf(false) }
    // 로그인/가이드는 DataStore 값이 로드될 때까지 null — 스플래시가 그 시간을 가려준다.
    val loginMethod by ServiceLocator.userPrefs.loginMethod
        .collectAsState(initial = null)
    val guideSeen by ServiceLocator.userPrefs.guideSeen
        .collectAsState(initial = null)
    val setupSeen by ServiceLocator.userPrefs.s2SetupSeen
        .collectAsState(initial = null)

    // 로그인 표시와 실제 세션이 어긋나면 로그인 화면을 다시 띄운다.
    //
    // 표시만 보고 통과시키면, 세션을 잃은 사람이 로그인돼 있다고 믿으면서
    // 아무것도 서버에 안 올라가는 상태로 계속 뛰게 된다. 조용히 기록을
    // 잃는 것보다 한 번 더 로그인하는 편이 낫다.
    var sessionChecked by rememberSaveable { mutableStateOf(false) }
    LaunchedEffect(loginMethod) {
        if (loginMethod?.isNotEmpty() == true && !ServiceLocator.sessionHolder.isSignedIn()) {
            // Google 로 로그인했던 사람이다 — 로그인 화면이 "다시 로그인해 주세요"(시작·로그인·첫 사용 v1 시안 09)로 알린다
            if (loginMethod == "google") ServiceLocator.userPrefs.setSignInAgain(true)
            ServiceLocator.userPrefs.setLoginMethod("")
        }
        sessionChecked = true
    }

    // 접속 코드 문(0079) — 로그인 · 계정이 바뀔 때와 앱으로 돌아올 때마다 다시 묻는다. 막힌 사람은 코드 입력 화면만 본다
    val accessContext = androidx.compose.ui.platform.LocalContext.current
    val access by com.stepup.android.core.AccessGate.state.collectAsState()
    LaunchedEffect(loginMethod, sessionChecked) {
        if (loginMethod != null && sessionChecked) com.stepup.android.core.AccessGate.refresh()
    }
    val accessScope = rememberCoroutineScope()
    androidx.lifecycle.compose.LifecycleResumeEffect(Unit) {
        val job = accessScope.launch { com.stepup.android.core.AccessGate.refresh() }
        onPauseOrDispose { job.cancel() }
    }

    Box(Modifier.fillMaxSize()) {
        NightCanvas(Modifier.fillMaxSize())

        val blocked = access?.blocked == true
        val stage = when {
            !ready || loginMethod == null || guideSeen == null || setupSeen == null || !sessionChecked -> 0
            loginMethod!!.isEmpty() -> 1
            blocked -> 4
            // S2 첫 설정은 새로 온 사람(가이드를 아직 안 본 사람)에게만 — 기존 사용자를 다시 붙잡지 않는다
            guideSeen == false && setupSeen == false -> 3
            else -> 2
        }
        // 앱 진입은 불투명도 180ms(시작·로그인·첫 사용 v1) — 동작 줄이기에서는 바로 바뀐다
        Crossfade(stage, animationSpec = tween(LocalMotion.current.duration(180)), label = "entryStage") { visible ->
        when (visible) {
            0 ->
                SplashScreen(onReady = { ready = true })

            1 -> LoginScreen(onDone = {})

            3 -> com.stepup.android.ui.screens.setup.S2SetupFlow(onDone = {})

            4 -> com.stepup.android.ui.screens.access.AccessCodeScreen(
                status = access ?: com.stepup.android.data.remote.AccessStatusRow(),
                signedIn = loginMethod == "google",
                onGranted = { accessScope.launch { com.stepup.android.core.AccessGate.refresh() } },
                onSignOut = {
                    // 다른 계정으로 — 설정의 로그아웃과 같은 정리(앞 계정의 사본 · 접속 권한 · 로그인을 이 폰에서 지운다)
                    accessScope.launch {
                        kotlinx.coroutines.withContext(kotlinx.coroutines.NonCancellable) {
                            com.stepup.android.ui.screens.settings.forgetAccountOnPhone()
                        }
                    }
                },
            )

            else -> MainScaffold(startTour = guideSeen == false)
        }
        }
        // 크루 출발 연출 — 대기실에서 러닝 화면으로 길이 바뀌어도 3 · 2 · 1 · 0 · StepUP! · 펼침이 끊기지 않게 맨 위에 둔다
        if (stage == 2) com.stepup.android.ui.screens.walk.CrewLaunchOverlay()
        // 테스트 APK 새 버전 알림 — 스플래시가 끝난 뒤에만(debug 빌드 · 테스트 중이 아닐 때)
        if (stage != 0) com.stepup.android.ui.components.TestUpdatePrompt()
    }
}
