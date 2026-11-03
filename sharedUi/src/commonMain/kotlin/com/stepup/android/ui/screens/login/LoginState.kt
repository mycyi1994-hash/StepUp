package com.stepup.android.ui.screens.login

/* 안드로이드 LoginRules.kt 의 화면 상태 둘(같은 이름 · 같은 값). 로그인 판단은 기기마다(안드로이드 LoginRules · iOS IosLogin) */

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
