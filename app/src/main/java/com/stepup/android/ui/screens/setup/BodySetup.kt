package com.stepup.android.ui.screens.setup

import com.stepup.android.BuildConfig
import com.stepup.android.ui.screens.walk.RunMenuLocks

/**
 * 첫 설정(키 · 몸무게 · 목표 몸무게 · 모드)을 묻고, 설정에 신체 정보 · 모드 줄을 둘지.
 *
 * 그 값을 쓰는 곳은 다이어트 러닝뿐이다(모드는 첫 설정 · 설정 화면만 읽는다). 스토어 판에서 다이어트가 잠겨 있으면
 * (러닝 메뉴에서 숨는다) 쓰지도 않을 몸 정보를 묻지 않는다. 테스트 · 개발 판(잠긴 카드를 자물쇠로 보인다)은 그대로 묻는다.
 * 다이어트를 열면(RunMenuLocks.DIET = false) 스토어 판도 다시 묻는다. 이미 저장한 값은 지우지 않는다.
 */
object BodySetup {

    fun asked(store: Boolean, dietLocked: Boolean): Boolean = !(store && dietLocked)

    /** 이 빌드에서 — 스토어 판은 BuildConfig.WEB3 = false */
    val askedHere: Boolean
        get() = asked(store = !BuildConfig.WEB3, dietLocked = RunMenuLocks.DIET)
}
