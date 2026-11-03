package com.stepup.ui

import com.stepup.android.data.remote.KeychainSessionStore
import com.stepup.android.data.remote.SessionHolder
import com.stepup.android.data.remote.StepUpServer
import com.stepup.android.data.remote.SupabaseAuth
import platform.Foundation.NSBundle

/**
 * iOS 앱이 한 번 만들어 같이 쓰는 것(안드로이드 ServiceLocator 자리) — 서버 주소 · 공개 키는 Info.plist(Config.xcconfig)에서.
 * 공개 키(sb_publishable)만 앱에 넣는다. 서버 비밀 키는 앱에 넣지 않는다(CLAUDE.md "지킬 것").
 */
object IosApp {
    private fun plist(key: String): String =
        (NSBundle.mainBundle.objectForInfoDictionaryKey(key) as? String)?.trim().orEmpty()

    val supabaseUrl: String by lazy { plist("StepUpSupabaseUrl") }
    val supabaseKey: String by lazy { plist("StepUpSupabaseKey") }
    val attesterUrl: String by lazy { plist("StepUpAttesterUrl") }

    val sessionStore by lazy { KeychainSessionStore() }
    val auth by lazy { SupabaseAuth(supabaseUrl, supabaseKey) }
    val sessions by lazy { SessionHolder(auth, sessionStore) }
    val server by lazy { StepUpServer(supabaseUrl, supabaseKey, sessions) }
    /** 설정(안드로이드와 같은 UserPrefs — 문서 폴더의 strideup_prefs) */
    val prefs by lazy { com.stepup.android.data.prefs.createIosUserPrefs() }
}
