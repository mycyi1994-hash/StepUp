package com.stepup.android.core

import android.app.Application
import com.stepup.android.BuildConfig

/**
 * 스토어 판(release · storePreview)의 자리 — 테스트 APK 의 스스로 업데이트(src/selfUpdate)는 넣지 않는다.
 * Play 는 Play 밖에서 스스로 업데이트하는 앱을 막고, 자동 검사는 쓰지 않는 코드도 본다(2026-10-27 출시 점검).
 */
object TestUpdates {
    const val enabled: Boolean = false

    /** 이 빌드 표시 — "1.17.0 · abc1234". 커밋을 모르면 버전만. */
    val buildLabel: String
        get() = BuildConfig.VERSION_NAME + BuildConfig.BUILD_COMMIT.takeIf { it.isNotBlank() }?.let { " · $it" }.orEmpty()

    @Suppress("UNUSED_PARAMETER")
    fun attach(app: Application) = Unit
}
