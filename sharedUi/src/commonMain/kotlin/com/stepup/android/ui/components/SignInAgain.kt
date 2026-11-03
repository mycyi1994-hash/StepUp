package com.stepup.android.ui.components

import com.stepup.android.core.AppServices

/**
 * 다시 로그인하러 간다(안드로이드 SignInAgainButton.returnToSignIn 과 같다) — 이유를 남기고 로그인 방법을 지운 뒤 앱 뿌리가 로그인 화면으로 보낸다.
 * 달리던 러닝을 멈추는 일 · 화면 옮기기는 앱마다 다르니 [AppServices.onReturnToSignIn] 이 한다.
 */
suspend fun returnToSignIn() {
    AppServices.onReturnToSignIn()
    AppServices.userPrefs.setSignInAgain(true)
    AppServices.userPrefs.setLoginMethod("")
}
