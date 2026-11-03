package com.stepup.android.core

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

/** 알림 설정을 서버에 보낸 상태(안드로이드 push/NotificationPreferenceSync.kt 와 같은 값) */
enum class NotificationSyncState { Idle, Sending, Synced, Pending }

/**
 * 알림 설정을 서버에 보내는 쪽(안드로이드 PushRegistrar 의 화면 쪽 자리, 2026-11-03 iOS C4).
 * iOS 는 푸시(APNs)를 붙이기 전까지 [NoPush] — 설정은 폰에만 저장하고 서버로 보낼 것이 없다(Idle).
 */
interface PushPreferences {
    val preferenceSync: StateFlow<NotificationSyncState>
    fun syncPrefsInBackground()
}

object NoPush : PushPreferences {
    override val preferenceSync: StateFlow<NotificationSyncState> = MutableStateFlow(NotificationSyncState.Idle)
    override fun syncPrefsInBackground() = Unit
}

/** 휴대폰(OS)이 이 앱의 알림을 허락했는가 — 앱이 앞에 올 때마다 다시 본다 */
@androidx.compose.runtime.Composable
expect fun rememberNotificationsAllowed(): Boolean
