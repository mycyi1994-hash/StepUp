package com.stepup.android

import android.content.Context
import android.graphics.Color
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.stepup.android.core.AppLocale
import com.stepup.android.core.ServiceLocator
import com.stepup.android.ui.StepPermissions
import com.stepup.android.ui.StepUpRoot
import com.stepup.android.ui.theme.StepUpTheme

class MainActivity : ComponentActivity() {

    private companion object {
        /** 아이콘을 어둡게 못 쓰는 기기에서 시스템 바 뒤에 까는 옅은 판 */
        const val SCRIM = 0x33000000
    }

    /** 선택한 언어로 리소스를 읽도록 액티비티 컨텍스트를 감싼다 (API 33 미만) */
    override fun attachBaseContext(newBase: Context) {
        super.attachBaseContext(AppLocale.wrap(newBase))
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // Android 13+에서는 OS가 앱 언어를 기억하므로 그 값을 정본으로 삼는다.
        AppLocale.syncFromSystem(this)
        // 화이트 라이트 테마 고정 — 시스템 설정과 무관하게 어두운 상태바 아이콘을
        // 쓴다. 바닥이 흰색이라 밝은 아이콘이면 시계와 배터리가 안 보인다.
        //
        // light(...) 의 둘째 인자는 아이콘을 어둡게 못 쓰는 구형 기기(API 26 미만
        // 상태바 / API 27 미만 내비바)에서 대신 깔 반투명 판이다. 투명으로 두면
        // 그 기기에서 흰 바탕에 흰 아이콘이 된다.
        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.light(Color.TRANSPARENT, SCRIM),
            navigationBarStyle = SystemBarStyle.light(Color.TRANSPARENT, SCRIM),
        )
        // 이미 권한이 있으면 바로 추적 시작 (첫 요청은 StepUpRoot에서 처리)
        if (StepPermissions.hasActivityRecognition(this)) {
            ServiceLocator.stepRepository.startTracking()
        }
        setContent {
            StepUpTheme {
                StepUpRoot()
            }
        }
    }
}
