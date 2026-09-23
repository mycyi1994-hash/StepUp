package com.stepup.android

import android.app.Application
import com.stepup.android.core.AppLocale
import com.stepup.android.core.AppTheme
import com.stepup.android.core.ServiceLocator
import com.stepup.android.push.PushService
import kotlinx.coroutines.runBlocking

class StepUpApp : Application() {

    override fun onCreate() {
        super.onCreate()
        ServiceLocator.init(this)
        // 첫 화면이 그려지기 전에 언어를 확정해야 리소스가 한 번에 맞는 언어로 읽힌다.
        // DataStore 한 건 읽기라 시작 시간에 미치는 영향은 미미하다.
        // 저장소를 못 읽더라도 언어 하나 때문에 앱이 죽으면 안 되므로 기기 설정으로 넘어간다.
        val saved = runCatching { runBlocking { ServiceLocator.userPrefs.languageNow() } }
            .getOrDefault(AppLocale.SYSTEM)
        AppLocale.bootstrap(this, saved)
        // 테마도 같은 이유로 첫 프레임 전에 확정한다. 나중에 정하면 어둡게
        // 쓰는 사람이 앱을 켤 때마다 흰 화면이 한 번 번쩍인다.
        val theme = runCatching { runBlocking { ServiceLocator.userPrefs.themeModeNow() } }
            .getOrDefault("")
        AppTheme.bootstrap(theme)
        // 푸시 주소를 서버에 적어 둔다. 로그인 전이면 서버가 받지 않고, 로그인하면
        // 로그인 화면이 다시 부른다. 네트워크를 쓰므로 첫 화면을 기다리게 하지 않는다.
        PushService.createChannel(this)
        ServiceLocator.pushRegistrar.syncInBackground()
    }
}
