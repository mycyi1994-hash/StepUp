package com.stepup.android.core

import android.content.Context
import android.content.Intent
import androidx.core.content.edit
import java.security.SecureRandom

/**
 * 우리 알림이 싣는 표 — 이 설치에서만 아는 무작위 값(2026-11-22 보안 점검).
 *
 * MainActivity 는 다른 앱에 열려 있어(런처 · App Links) 아무 앱 · 웹 페이지나 알림과 같은 링크 칸(`push_link` · `link`)을 실어
 * 보낼 수 있었다. 그래서 우리가 만든 알림의 인텐트([stamp])에만 이 표를 싣고, 표가 맞을 때만 링크 칸을 읽는다
 * ([InviteLinks.trustsExtras]). 앱이 뒤에 있을 때 FCM 이 직접 띄우는 알림은 PushService 가 받은 메시지에 표를 더해 둔다.
 * 표는 앱 저장소(다른 앱은 못 읽는다)에 한 번 만들고 바꾸지 않는다 — 앞서 띄운 알림도 그대로 맞는다.
 */
object LinkToken {

    /** 인텐트에 싣는 이름 */
    const val EXTRA = "stepup_tap"

    private const val FILE = "stepup_link_token"
    private const val KEY = "token"

    @Volatile
    private var cached: String? = null

    /** 이 설치의 표 — 처음 부르면 만든다 */
    fun of(context: Context): String {
        cached?.let { return it }
        synchronized(this) {
            cached?.let { return it }
            val prefs = context.applicationContext.getSharedPreferences(FILE, Context.MODE_PRIVATE)
            val token = prefs.getString(KEY, null)?.takeIf { it.length >= TOKEN_BYTES * 2 }
                ?: newToken().also { fresh -> prefs.edit(commit = true) { putString(KEY, fresh) } }
            cached = token
            return token
        }
    }

    /** 우리 알림이 여는 인텐트에 표를 싣는다 */
    fun stamp(context: Context, intent: Intent): Intent = intent.putExtra(EXTRA, of(context))

    private fun newToken(): String {
        val bytes = ByteArray(TOKEN_BYTES).also { SecureRandom().nextBytes(it) }
        return bytes.joinToString("") { "%02x".format(it) }
    }

    private const val TOKEN_BYTES = 32
}
