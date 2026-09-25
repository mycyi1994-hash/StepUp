package com.stepup.android

import android.content.Context
import android.content.Intent
import android.content.res.Configuration
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.LaunchedEffect
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.stepup.android.core.AppLocale
import com.stepup.android.core.AppTheme
import com.stepup.android.core.InviteLinks
import com.stepup.android.core.ServiceLocator
import com.stepup.android.ui.StepPermissions
import com.stepup.android.ui.StepUpRoot
import com.stepup.android.ui.theme.StepUpTheme

class MainActivity : ComponentActivity() {

    /**
     * 상태바·내비바를 지금 테마에 맞춘다.
     *
     * light(...) 의 둘째 인자는 아이콘을 어둡게 못 쓰는 구형 기기(API 26 미만
     * 상태바 / API 27 미만 내비바)에서 대신 깔 반투명 판이다. 투명으로 두면
     * 그 기기에서 흰 바탕에 흰 아이콘이 된다.
     */
    private fun applySystemBars(dark: Boolean) {
        enableEdgeToEdge(
            statusBarStyle = if (dark) {
                SystemBarStyle.dark(Color.TRANSPARENT)
            } else {
                SystemBarStyle.light(Color.TRANSPARENT, SCRIM)
            },
            navigationBarStyle = if (dark) {
                SystemBarStyle.dark(Color.TRANSPARENT)
            } else {
                SystemBarStyle.light(Color.TRANSPARENT, SCRIM)
            },
        )
    }

    private companion object {
        /** 아이콘을 어둡게 못 쓰는 기기에서 시스템 바 뒤에 까는 옅은 판 */
        const val SCRIM = 0x33000000

        // 첫 프레임 전의 창 바닥. ui/theme/Color.kt 의 night 와 값을 맞춘다.
        const val WINDOW_LIGHT = 0xFFFFFFFF.toInt()
        const val WINDOW_DARK = 0xFF060A12.toInt()
    }

    /** 앱이 이미 떠 있을 때 초대 링크를 누르면 여기로 온다(singleTop). */
    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        InviteLinks.handle(intent)
    }

    /** 선택한 언어로 리소스를 읽도록 액티비티 컨텍스트를 감싼다 (API 33 미만) */
    override fun attachBaseContext(newBase: Context) {
        super.attachBaseContext(AppLocale.wrap(newBase))
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // Android 13+에서는 OS가 앱 언어를 기억하므로 그 값을 정본으로 삼는다.
        AppLocale.syncFromSystem(this)
        // 창 바닥을 테마 색으로 먼저 칠한다. themes.xml 의 값은 하나뿐이라,
        // 어둡게 쓰는 사람은 Compose 가 첫 프레임을 그리기 전까지 흰 화면을
        // 한 번 보게 된다.
        val startDark = AppTheme.mode.isDark(
            resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK ==
                Configuration.UI_MODE_NIGHT_YES,
        )
        window.setBackgroundDrawable(ColorDrawable(if (startDark) WINDOW_DARK else WINDOW_LIGHT))

        // 초대 링크나 크루 알림으로 들어왔으면 그 크루를 연다(로그인 뒤에).
        // 화면 회전·언어·테마 변경으로 다시 만들어질 때는 같은 intent 가 또 오므로
        // 처음 만들 때만 읽는다 — 안 그러면 이미 본 크루로 매번 다시 끌려간다.
        if (savedInstanceState == null) InviteLinks.handle(intent)

        // 이미 권한이 있으면 바로 추적 시작 (첫 요청은 StepUpRoot에서 처리)
        if (StepPermissions.hasActivityRecognition(this)) {
            ServiceLocator.stepRepository.startTracking()
        }
        setContent {
            val mode = AppTheme.mode
            val dark = mode.isDark(isSystemInDarkTheme())
            // 상태바 아이콘은 바닥과 반대여야 한다. 밝은 바탕에 밝은 아이콘이면
            // 시계와 배터리가 안 보인다. 테마를 바꾸는 즉시 따라가야 하므로
            // onCreate 가 아니라 여기서, dark 가 바뀔 때마다 다시 건다.
            LaunchedEffect(dark) { applySystemBars(dark) }
            StepUpTheme(mode) {
                com.stepup.android.ui.experience.ExperienceProvider {
                    StepUpRoot()
                }
            }
        }
    }
}
