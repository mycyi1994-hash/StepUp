package com.stepup.android.core

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent

/**
 * 테스트 APK 가 새 버전으로 깔린 직후(MY_PACKAGE_REPLACED) — 이 앱이 연 업데이트면 "업데이트했어요" 알림.
 * test-apk 빌드의 매니페스트(src/debug)에만 적혀 있다.
 */
class TestUpdateReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        when (intent.action) {
            Intent.ACTION_MY_PACKAGE_REPLACED -> TestUpdates.onReplaced(context.applicationContext)
        }
    }
}
