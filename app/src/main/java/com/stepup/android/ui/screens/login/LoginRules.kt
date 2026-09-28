package com.stepup.android.ui.screens.login

import com.stepup.android.data.remote.GoogleIdResult
import com.stepup.android.data.remote.TokenResult

/** 로그인 화면 버튼 위 한 줄 안내(시안 06~09) */
enum class LoginNotice {
    /** 06 서버에 닿지 못했다 — 로그인 상태를 바꾸지 않고 다시 누르게 한다. 계정 만료로 단정하지 않는다 */
    Offline,

    /** 07 계정 선택 · 서버 확인이 실패했다 */
    Failed,

    /** 08 이 기기에 쓸 수 있는 Google 계정이 없다 */
    NoAccount,

    /** 09 로그인했던 사람이 다시 로그인해야 한다 — 이유를 아는 경우에만 */
    SignInAgain,
}

/** 버튼이 어느 단계인가 — 계정 선택 창이 떠 있는 동안(Picking)은 누를 수 없고, 계정을 고른 뒤 서버 확인(05)은 "로그인 중" */
enum class LoginPhase { Idle, Picking, Verifying }

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
