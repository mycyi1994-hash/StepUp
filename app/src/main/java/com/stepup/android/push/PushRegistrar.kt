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
import kotlinx.coroutines.async
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withTimeoutOrNull

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
    private val preferences: suspend () -> NotifyPrefs,
) {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val preferenceDelivery = NotificationPreferenceSync(preferences) { prefs ->
        api.setPrefs(prefs.push, prefs.goalReminder, prefs.partyInvite, prefs.eventNews) is ServerResult.Ok
    }
    val preferenceSync = preferenceDelivery.state

    /** 이 실행에서 마지막으로 서버에 보낸 푸시 글 언어 — 아직 안 보냈으면 null */
    @Volatile
    private var sentLanguage: String? = null

    /** 화면이 닫혀도 끝까지 가게 앱 수명의 코루틴에서 적는다. */
    fun syncInBackground(token: String? = null) {
        scope.launch {
            // Preference delivery must not depend on FCM token availability.
            preferenceDelivery.sync()
            runCatching { sync(token) }
        }
    }

    /** 알림 설정을 서버에 올린다. 로그인 전이면 조용히 넘어간다 — 다음에 켤 때 다시 올린다. */
    fun syncPrefsInBackground() {
        scope.launch { preferenceDelivery.sync() }
    }

    suspend fun sync(token: String? = null): Boolean {
        val current = token ?: fetchToken() ?: return false
        val lang = currentLanguage()
        // 보내기 전에 적는다 — 못 보냈으면 인터넷이 돌아올 때(NetworkWatch) · 다시 켤 때 다시 보낸다
        sentLanguage = lang
        return api.register(current, lang) is ServerResult.Ok
    }

    /**
     * 화면 언어가 바뀌었으면 푸시 글 언어를 서버에 다시 알린다 — 앱 안에서 언어를 고른 뒤, 시스템의 앱별 언어 · 기기 언어가 바뀌어
     * 화면이 다시 만들어질 때(MainActivity). 이 실행에서 아직 한 번도 보내지 않았으면 넘어간다(앱을 켤 때 보내는 일이 맡는다).
     */
    fun syncIfLanguageChanged() {
        scope.launch {
            val before = sentLanguage ?: return@launch
            if (currentLanguage() != before) runCatching { sync() }
        }
    }

    private suspend fun currentLanguage(): String =
        runCatching { locale() }.getOrDefault("").ifBlank { Locale.getDefault().language }

    /**
     * 로그아웃 — 로그인 표시를 지우기 전에 부른다. 이 폰의 푸시 주소를 서버의 이 계정에서 떼고, 주소도 버린다
     * (다음 계정은 새 주소로 적는다). 연결이 느려도 로그아웃을 붙잡지 않게 잠깐만 기다리고, 실패해도 넘어간다.
     */
    suspend fun unregister(waitMillis: Long = UNREGISTER_WAIT_MS) {
        // 요청은 앱 수명의 코루틴에서 — 기다림이 끝나도 이미 보낸 요청은 끝까지 간다
        val removed = scope.async {
            val token = fetchToken() ?: return@async
            runCatching { api.unregister(token) }
            Unit
        }
        withTimeoutOrNull(waitMillis) { runCatching { removed.await() } }
        runCatching { FirebaseMessaging.getInstance().deleteToken() }
    }

    private suspend fun fetchToken(): String? = runCatching {
        suspendCancellableCoroutine<String?> { cont ->
            FirebaseMessaging.getInstance().token.addOnCompleteListener { task ->
                cont.resume(if (task.isSuccessful) task.result else null)
            }
        }
    }.getOrNull()

    private companion object {
        const val UNREGISTER_WAIT_MS = 3_000L
    }
}
