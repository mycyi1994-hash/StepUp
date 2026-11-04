package com.stepup.android.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import com.stepup.android.core.ServiceLocator
import com.stepup.android.service.WalkSessionService
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

/**
 * 출입증 갱신이 거절됐다(refresh token 이 죽음 — [com.stepup.android.data.remote.SessionHolder.signInRequired]) —
 * 로그인 화면으로 보내 "다시 로그인해 주세요"를 보인다. 달리는 중이면 러닝이 끝난 뒤에 보낸다(러닝을 끊지 않는다 —
 * 기록은 폰에 남아 다시 로그인하면 올라간다). 그동안 요청은 죽은 토큰을 다시 보내지 않고 "로그인 필요"로 끝난다.
 */
@Composable
fun SessionExpiryWatch() {
    LaunchedEffect(Unit) {
        combine(
            ServiceLocator.sessionHolder.signInRequired,
            WalkSessionService.state.map { it.isActive }.distinctUntilChanged(),
        ) { dead, running -> shouldReturnToSignIn(dead, running) }
            .distinctUntilChanged()
            .filter { it }
            .collect {
                // 이미 로그인 화면이면(로그인 표시 없음) 그대로 둔다
                if (ServiceLocator.userPrefs.loginMethod.first().isEmpty()) return@collect
                ServiceLocator.userPrefs.setSignInAgain(true)
                ServiceLocator.userPrefs.setLoginMethod("")
            }
    }
}

/** 다시 로그인하러 보낼 때 — 갱신이 거절됐고 달리는 중이 아닐 때만 */
internal fun shouldReturnToSignIn(refreshRejected: Boolean, running: Boolean): Boolean = refreshRejected && !running
