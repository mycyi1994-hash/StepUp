package com.stepup.android

import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.stepup.android.core.AppLocale
import com.stepup.android.core.AppTheme
import com.stepup.android.service.WalkSessionService
import com.stepup.android.ui.screens.walk.LockRunPanel
import com.stepup.android.ui.screens.walk.dismissLock
import com.stepup.android.ui.screens.walk.rememberDeviceLocked
import com.stepup.android.ui.screens.walk.showOverLockScreen
import com.stepup.android.ui.theme.StepUpTheme

/**
 * 잠금 화면의 러닝 현황만 보이는 화면 — 러닝 알림을 누르면 여기로 온다(2026-10-26).
 *
 * 앱을 다른 앱 뒤에 둔 채 잠갔으면 안드로이드가 앱을 잠금 화면 위로 스스로 올리지 못한다(뒤에 있는 앱의 화면 열기 제한).
 * 그때 잠금 화면의 러닝 알림을 누르면 잠금을 풀지 않고 이 화면이 가운데 현황을 보인다 — 매니페스트의 showWhenLocked 는 이 화면에만 있다.
 * 잠겨 있지 않거나 달리는 중이 아니면 바로 앱(MainActivity)을 연다. 앱 화면 · 계정 정보는 여기 없다.
 */
class LockRunActivity : ComponentActivity() {

    override fun attachBaseContext(newBase: Context) {
        super.attachBaseContext(AppLocale.wrap(newBase))
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        showOverLockScreen(true)
        val keyguard = getSystemService(android.app.KeyguardManager::class.java)
        if (!WalkSessionService.state.value.isActive || keyguard?.isKeyguardLocked != true) {
            openApp()
            return
        }
        window.setBackgroundDrawable(ColorDrawable(Color.BLACK))
        setContent {
            val session by WalkSessionService.state.collectAsStateWithLifecycle()
            val locked = rememberDeviceLocked(watch = true)
            // 러닝이 끝났거나 잠금을 풀었다 — 앱으로
            LaunchedEffect(session.isActive, locked) {
                if (!session.isActive || !locked) openApp()
            }
            BackHandler { finish() }
            StepUpTheme(AppTheme.mode) {
                LockRunPanel(
                    state = session,
                    onToggle = {
                        if (session.isPaused) WalkSessionService.resume(this) else WalkSessionService.pause(this)
                    },
                    onOpen = { dismissLock { openApp() } },
                )
            }
        }
    }

    private fun openApp() {
        if (isFinishing) return
        startActivity(
            Intent(this, MainActivity::class.java)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP),
        )
        finish()
        @Suppress("DEPRECATION")
        overridePendingTransition(0, 0)
    }
}
