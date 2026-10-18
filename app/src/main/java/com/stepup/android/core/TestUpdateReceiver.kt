package com.stepup.android.core

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent

/**
 * 테스트 APK 스스로 업데이트의 결과를 받는다 — 설치 세션의 상태(확인 화면 · 성공 · 취소 · 실패)와,
 * 새 버전이 깔린 직후의 MY_PACKAGE_REPLACED. test-apk 빌드의 매니페스트(src/debug)에만 적혀 있다.
 */
class TestUpdateReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        when (intent.action) {
            TestUpdates.ACTION_INSTALL_STATUS -> TestUpdates.onInstallStatus(context.applicationContext, intent)
            Intent.ACTION_MY_PACKAGE_REPLACED -> TestUpdates.onReplaced(context.applicationContext)
        }
    }
}
