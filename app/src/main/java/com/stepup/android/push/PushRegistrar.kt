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
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

enum class NotificationSyncState { Idle, Sending, Synced, Pending }

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
    private val prefsMutex = Mutex()
    private val _preferenceSync = MutableStateFlow(NotificationSyncState.Idle)
    val preferenceSync = _preferenceSync.asStateFlow()

    /** 화면이 닫혀도 끝까지 가게 앱 수명의 코루틴에서 적는다. */
    fun syncInBackground(token: String? = null) {
        scope.launch {
            // Preference delivery must not depend on FCM token availability.
            syncPreferences()
            runCatching { sync(token) }
        }
    }

    /** 알림 설정을 서버에 올린다. 로그인 전이면 조용히 넘어간다 — 다음에 켤 때 다시 올린다. */
    fun syncPrefsInBackground() {
        scope.launch { syncPreferences() }
    }

    private suspend fun syncPreferences() = prefsMutex.withLock {
        _preferenceSync.value = NotificationSyncState.Sending
        try {
            // Read after acquiring the lock so queued calls send the latest stored choice.
            val prefs = preferences()
            val result = api.setPrefs(prefs.push, prefs.goalReminder, prefs.partyInvite, prefs.eventNews)
            _preferenceSync.value = if (result is ServerResult.Ok) {
                NotificationSyncState.Synced
            } else {
                NotificationSyncState.Pending
            }
        } catch (cancelled: CancellationException) {
            _preferenceSync.value = NotificationSyncState.Pending
            throw cancelled
        } catch (_: Exception) {
            // Local preferences survive; startup/login or another edit retries delivery.
            _preferenceSync.value = NotificationSyncState.Pending
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
