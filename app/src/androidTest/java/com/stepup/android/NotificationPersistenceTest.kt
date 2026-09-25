package com.stepup.android

import androidx.room.Room
import androidx.test.platform.app.InstrumentationRegistry
import com.stepup.android.data.local.AppDatabase
import com.stepup.android.data.local.NotificationEntity
import com.stepup.android.data.repo.CrewActionResult
import com.stepup.android.data.repo.acceptCrewInvitation
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Test

class NotificationPersistenceTest {
    @Test fun failedInviteCanRetryAndOnlyConfirmedResponsesConsumeIt() = runBlocking {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val db = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java).build()
        try {
            val dao = db.notificationDao()
            dao.insert(NotificationEntity(timestamp = 1, type = "CREW_INVITE", argText = "crew",
                argAmount = 0.0, argExtra = "crew-id", read = false, actioned = false))
            val invitation = dao.observeAll(100).first().single()
            val failed = CrewActionResult.Failed("Offline")
            assertEquals(failed, acceptCrewInvitation(dao, invitation) { failed })
            assertEquals(invitation, dao.observeAll(100).first().single())
            var threw = false
            try { acceptCrewInvitation(dao, invitation) { error("Transport failed") } }
            catch (_: IllegalStateException) { threw = true }
            assertTrue(threw)
            assertEquals(invitation, dao.observeAll(100).first().single())
            assertTrue(acceptCrewInvitation(dao, invitation.copy(argExtra = "")) {
                error("Missing target must not reach the server")
            } is CrewActionResult.Failed)
            assertEquals(CrewActionResult.Requested, acceptCrewInvitation(dao, invitation) { target ->
                assertEquals("crew-id", target)
                CrewActionResult.Requested
            })
            assertEquals(invitation.copy(read = true, actioned = true), dao.observeAll(100).first().single())
            assertEquals(CrewActionResult.Done, acceptCrewInvitation(dao, invitation.copy(actioned = true)) {
                error("An already consumed invitation must not submit again")
            })
        } finally { db.close() }
    }

    @Test fun readingPreservesHistoryAndPendingActions() = runBlocking {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val db = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java).build()
        try {
            val dao = db.notificationDao()
            listOf("REWARD_EARNED", "CREW_INVITE", "PARTY_INVITE", "EVENT_REWARD").forEachIndexed { index, type ->
                dao.insert(NotificationEntity(timestamp = index.toLong(), type = type,
                    argText = "test", argAmount = 3.0, argExtra = "target", read = false,
                    actioned = index == 0))
            }
            val before = dao.observeAll(100).first()
            assertEquals(4, dao.observeUnreadCount().first())
            dao.markAllRead()
            dao.markAllRead() // Repeat visits cannot remove history or accept an invitation.
            assertEquals(before.map { it.copy(read = true) }, dao.observeAll(100).first())
            assertEquals(0, dao.observeUnreadCount().first())
        } finally { db.close() }
    }
}
