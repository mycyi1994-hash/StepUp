package com.stepup.android.push

import com.google.firebase.messaging.FirebaseMessaging
import com.stepup.android.data.prefs.NotifyPrefs
import com.stepup.android.data.remote.PushApi
import com.stepup.android.data.remote.ServerResult
import java.util.Locale
import kotlin.coroutines.resume
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine

/**
 * 이 폰의 푸시 주소(FCM 토큰)를 서버에 적어 둔다.
 *
 * 앱을 켤 때, 로그인한 직후, 토큰이 바뀔 때 부른다. 로그인 전이면 서버가
 * 받아 주지 않으므로 조용히 넘어간다 — 로그인하면 다시 부른다.
 * Play 서비스가 없는 기기(일부 에뮬레이터)에서는 토큰을 못 받는다. 그래도
 * 앱은 알림 없이 그대로 돈다.
 */
class PushRegistrar(
    private val api: PushApi,
    private val locale: suspend () -> String,
) {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    /** 화면이 닫혀도 끝까지 가게 앱 수명의 코루틴에서 적는다. */
    fun syncInBackground(token: String? = null) {
        scope.launch { runCatching { sync(token) } }
    }

    /** 알림 설정을 서버에 올린다. 로그인 전이면 조용히 넘어간다 — 다음에 켤 때 다시 올린다. */
    fun syncPrefsInBackground(prefs: NotifyPrefs) {
        scope.launch {
            runCatching { api.setPrefs(prefs.push, prefs.goalReminder, prefs.partyInvite, prefs.eventNews) }
        }
    }

    suspend fun sync(token: String? = null): Boolean {
        val current = token ?: fetchToken() ?: return false
        val lang = runCatching { locale() }.getOrDefault("").ifBlank { Locale.getDefault().language }
        return api.register(current, lang) is ServerResult.Ok
    }

    private suspend fun fetchToken(): String? = runCatching {
        suspendCancellableCoroutine<String?> { cont ->
            FirebaseMessaging.getInstance().token.addOnCompleteListener { task ->
                cont.resume(if (task.isSuccessful) task.result else null)
            }
        }
    }.getOrNull()
}
