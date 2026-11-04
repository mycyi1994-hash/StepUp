package com.stepup.android.ui.screens.login

import com.stepup.android.data.remote.GoogleIdResult
import com.stepup.android.data.remote.TokenResult




/** 로그인 결과 → 다음 장면. 화면 없이 판단만 한다 */
sealed interface LoginStep {
    /** 계정을 골랐다 — 서버 확인(05)으로 */
    data class Verify(val idToken: String, val nonce: String) : LoginStep

    /** 서버가 세션을 줬다 — 그때만 로그인 표시를 남기고 홈으로 간다 */
    data object SignedIn : LoginStep

    /** 로그인 화면에 남는다 — 안내 한 줄(없으면 조용히 01) */
    data class Stay(val notice: LoginNotice?) : LoginStep
}

internal object LoginRules {
    /**
     * 계정 선택 창의 결과.
     * 취소는 오류가 아니다 — 안내 없이 01(다시 로그인하러 온 경우라면 그 안내 그대로)로 조용히 돌아간다.
     */
    fun afterPick(result: GoogleIdResult, base: LoginNotice?): LoginStep = when (result) {
        is GoogleIdResult.Ok -> LoginStep.Verify(result.idToken, result.nonce)
        GoogleIdResult.Cancelled -> LoginStep.Stay(base)
        is GoogleIdResult.NoAccount -> LoginStep.Stay(LoginNotice.NoAccount)
        is GoogleIdResult.Failed -> LoginStep.Stay(LoginNotice.Failed)
    }

    /** 서버 확인 결과 — 서버가 세션을 준 뒤에만 로그인 완료다. 통신 오류는 연결 안내(06)이지 계정 만료가 아니다 */
    fun afterServer(result: TokenResult): LoginStep = when (result) {
        is TokenResult.Ok -> LoginStep.SignedIn
        is TokenResult.Unavailable -> LoginStep.Stay(LoginNotice.Offline)
        is TokenResult.SignInRequired -> LoginStep.Stay(LoginNotice.Failed)
    }

    /** 다시 로그인하러 왔는지에 따른 기본 안내 — 이유가 없으면 일반 로그인 화면(01) */
    fun base(signInAgain: Boolean): LoginNotice? = if (signInAgain) LoginNotice.SignInAgain else null
}
