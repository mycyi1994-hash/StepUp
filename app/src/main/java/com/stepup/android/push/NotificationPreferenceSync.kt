package com.stepup.android.push

import com.stepup.android.data.prefs.NotifyPrefs
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

enum class NotificationSyncState { Idle, Sending, Synced, Pending }

/** Serialize delivery, reading persisted choices only when their request can start. */
internal class NotificationPreferenceSync(
    private val read: suspend () -> NotifyPrefs,
    private val send: suspend (NotifyPrefs) -> Boolean,
) {
    private val mutex = Mutex()
    private val _state = MutableStateFlow(NotificationSyncState.Idle)
    val state = _state.asStateFlow()

    suspend fun sync() = mutex.withLock {
        _state.value = NotificationSyncState.Sending
        try {
            _state.value = if (send(read())) NotificationSyncState.Synced
                else NotificationSyncState.Pending
        } catch (cancelled: CancellationException) {
            _state.value = NotificationSyncState.Pending
            throw cancelled
        } catch (_: Exception) {
            // A failed send must neither erase stored choices nor claim acknowledgement.
            _state.value = NotificationSyncState.Pending
        }
    }
}
