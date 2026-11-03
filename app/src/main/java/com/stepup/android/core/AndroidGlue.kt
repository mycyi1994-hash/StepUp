package com.stepup.android.core

import android.app.LocaleManager
import android.content.Context
import android.content.Intent
import android.content.res.Configuration
import android.os.Build
import android.os.LocaleList
import com.stepup.android.data.remote.AccessStatusRow
import com.stepup.android.push.PushService
import java.util.Locale

/*
 * 공용 화면(sharedUi)의 core 값에 안드로이드만의 일을 잇는 곳(2026-11-03 C8) — 화면 코드는 공용 AppLocale · InviteLinks · AccessGate 를 쓰고,
 * 액티비티 · 인텐트 · 설정 파일을 다루는 일만 여기 둔다(예전 app/core 의 같은 이름 함수들 그대로).
 */

/** 앱 언어 — 공용 [AppLocale] 의 값을 안드로이드에 적용한다 */
object AndroidLocale {
    /** 앱 시작 시 저장된 선택을 프로세스에 적용한다 */
    fun bootstrap(context: Context, saved: String) {
        AppLocale.bootstrap(saved)
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) applyLanguageToProcess(context, AppLocale.tag)
        // 공용 화면 글자는 프로세스 기본 언어로 고른다 — 13+ 도 앱 언어로 맞춘다
        else setProcessLocale(AppLocale.tag)
    }

    /** 시스템이 기억하고 있는 값을 읽어 언어를 맞춘다 (API 33+ 전용) */
    fun syncFromSystem(context: Context) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) return
        val locales = context.getSystemService(LocaleManager::class.java)?.applicationLocales
        AppLocale.bootstrap(locales?.takeIf { !it.isEmpty }?.get(0)?.language.orEmpty())
        setProcessLocale(AppLocale.tag)
    }

    /** 액티비티가 붙기 전 베이스 컨텍스트를 선택한 언어로 감싼다 (API 33 미만) */
    fun wrap(base: Context): Context {
        val tag = AppLocale.tag
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU || tag.isEmpty()) return base
        val config = Configuration(base.resources.configuration)
        val locale = Locale.forLanguageTag(tag)
        config.setLocale(locale)
        config.setLocales(LocaleList(locale))
        return base.createConfigurationContext(config)
    }
}

/** 앱을 연 인텐트를 본다. 초대 링크나 크루 알림이면 그 크루를, 크루 채팅 알림이면 그 방을 연다 */
fun InviteLinks.handle(intent: Intent?) {
    intent ?: return
    // 앱이 켜져 있을 때 받은 알림은 PushService 가 EXTRA_LINK 로, 꺼져 있을 때는 안드로이드가 data 칸("link")으로 싣는다
    val push = intent.getStringExtra(PushService.EXTRA_LINK) ?: intent.getStringExtra("link")
    handle(intent.dataString, push)
}

/** 접속 문 상태를 이 폰에 남긴다(설정 파일 stepup_access — 예전 AccessGate 와 같은 이름 · 같은 칸) */
object AndroidAccess {
    private const val FILE = "stepup_access"

    /** 앱이 켜질 때 — 마지막으로 안 상태를 먼저 보이고, 받은 상태를 남기게 잇는다 */
    fun load(context: Context) {
        val p = context.getSharedPreferences(FILE, Context.MODE_PRIVATE)
        AccessGate.persist = { row ->
            context.getSharedPreferences(FILE, Context.MODE_PRIVATE).edit()
                .putString("mode", row.mode).putBoolean("granted", row.granted).apply()
        }
        val mode = p.getString("mode", null) ?: return
        AccessGate.set(AccessStatusRow(mode = mode, granted = p.getBoolean("granted", true)))
    }

    /** 로그아웃 · 다른 계정 — 앞 계정의 권한을 들고 있지 않게 */
    fun forget(context: Context) {
        AccessGate.forget()
        context.getSharedPreferences(FILE, Context.MODE_PRIVATE).edit().remove("granted").apply()
    }
}

/**
 * 러닝에 필요한데 아직 허용되지 않은 권한(안드로이드 이름) — [api] 판 기준. 걸음(29+) · 알림(33+) · 위치(정확 · 대략 함께).
 * 공용 화면은 권한 창을 하나씩 띄우고, 이 목록은 판별 규칙 검사(StepPermissionsTest)가 본다.
 */
fun com.stepup.android.ui.StepPermissions.requiredPermissions(api: Int, granted: (String) -> Boolean): Array<String> {
    val needed = mutableListOf<String>()
    if (api >= Build.VERSION_CODES.Q && !granted(android.Manifest.permission.ACTIVITY_RECOGNITION)) {
        needed += android.Manifest.permission.ACTIVITY_RECOGNITION
    }
    if (api >= Build.VERSION_CODES.TIRAMISU && !granted(android.Manifest.permission.POST_NOTIFICATIONS)) {
        needed += android.Manifest.permission.POST_NOTIFICATIONS
    }
    if (!granted(android.Manifest.permission.ACCESS_FINE_LOCATION) && !granted(android.Manifest.permission.ACCESS_COARSE_LOCATION)) {
        // Android 12+ may ignore FINE by itself. Respect an existing approximate grant.
        needed += android.Manifest.permission.ACCESS_FINE_LOCATION
        needed += android.Manifest.permission.ACCESS_COARSE_LOCATION
    }
    return needed.toTypedArray()
}
