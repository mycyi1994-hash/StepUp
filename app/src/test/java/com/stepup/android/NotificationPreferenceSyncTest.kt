package com.stepup.android

import com.stepup.android.data.prefs.NotifyPrefs
import com.stepup.android.push.NotificationPreferenceSync
import com.stepup.android.push.NotificationSyncState
import java.io.IOException
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class NotificationPreferenceSyncTest {
    @Test(timeout = 10_000) fun rejectedAndOfflineSendsRemainPendingUntilAcknowledged() = runBlocking {
        val saved = NotifyPrefs().copy(push = false, eventNews = false)
        var attempt = 0
        val sync = NotificationPreferenceSync({ saved }) { sent ->
            assertEquals(saved, sent)
            when (attempt++) {
                0 -> false
                1 -> throw IOException("offline")
                else -> true
            }
        }
        sync.sync()
        assertEquals(NotificationSyncState.Pending, sync.state.value)
        sync.sync()
        assertEquals(NotificationSyncState.Pending, sync.state.value)
        sync.sync()
        assertEquals(NotificationSyncState.Synced, sync.state.value)
        assertEquals(3, attempt)
    }

    @Test(timeout = 10_000) fun queuedDeliveryReadsNewestPersistedChoicesAfterEarlierRequestFinishes() = runBlocking {
        var saved = NotifyPrefs()
        val sent = mutableListOf<NotifyPrefs>()
        val releaseFirst = CompletableDeferred<Unit>()
        val sync = NotificationPreferenceSync({ saved }) { prefs ->
            sent += prefs
            if (sent.size == 1) releaseFirst.await()
            true
        }
        val first = launch(start = CoroutineStart.UNDISPATCHED) { sync.sync() }
        saved = saved.copy(push = false)
        val queued = launch(start = CoroutineStart.UNDISPATCHED) { sync.sync() }
        saved = saved.copy(eventNews = false, partyInvite = false)
        assertEquals(1, sent.size)
        assertEquals(NotificationSyncState.Sending, sync.state.value)
        releaseFirst.complete(Unit)
        first.join()
        queued.join()
        assertEquals(listOf(NotifyPrefs(), saved), sent)
        assertEquals(NotificationSyncState.Synced, sync.state.value)
    }

    @Test(timeout = 10_000) fun cancellingDeliveryReleasesLockAndAllowsRetryWithoutClaimingSuccess() = runBlocking {
        var cancelRequest = true
        val sync = NotificationPreferenceSync({ NotifyPrefs() }) {
            if (cancelRequest) awaitCancellation()
            true
        }
        val request = launch(start = CoroutineStart.UNDISPATCHED) { sync.sync() }
        request.cancelAndJoin()
        assertTrue(request.isCancelled)
        assertEquals(NotificationSyncState.Pending, sync.state.value)
        cancelRequest = false
        sync.sync()
        assertEquals(NotificationSyncState.Synced, sync.state.value)
    }
}
