package com.stepup.android

import androidx.room.Room
import androidx.test.platform.app.InstrumentationRegistry
import com.stepup.android.data.local.AppDatabase
import com.stepup.android.data.local.NotificationEntity
import com.stepup.android.data.repo.CrewActionResult
import com.stepup.android.data.repo.NotificationRepository
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

    /** 알림·공지 v1 — 연 알림만 읽음, "모두 읽음"은 누른 때까지 보인 알림만. 읽음은 삭제 · 초대 응답과 별개다 */
    @Test fun openingOneReadsOnlyItAndReadAllStopsAtWhatWasShown() = runBlocking {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val db = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java).build()
        try {
            val dao = db.notificationDao()
            val repo = NotificationRepository(dao)
            listOf("REWARD_EARNED", "CREW_INVITE", "EVENT_REWARD").forEachIndexed { index, type ->
                dao.insert(NotificationEntity(timestamp = index.toLong(), type = type, argText = "test",
                    argAmount = 1.0, argExtra = "target", read = false, actioned = false))
            }
            val before = dao.observeAll(100).first()
            val invite = before.single { it.type == "CREW_INVITE" }
            repo.markRead(invite.id)
            // 그 알림의 read 만 — 처리 여부 · 다른 알림은 그대로
            assertEquals(before.map { if (it.id == invite.id) it.copy(read = true) else it }, dao.observeAll(100).first())
            assertEquals(2, dao.observeUnreadCount().first())

            // 누른 때의 가장 최근 알림까지만. 요청 중에 들어온 알림은 새 알림으로 남는다
            val shown = repo.newestId.first()!!
            dao.insert(NotificationEntity(timestamp = 9, type = "PARTY_INVITE", argText = "late", argAmount = 0.0,
                argExtra = "lobby", read = false, actioned = false))
            repo.markReadUpTo(shown)
            val after = dao.observeAll(100).first()
            assertEquals(4, after.size)
            assertFalse(after.single { it.type == "PARTY_INVITE" }.read)
            assertTrue(after.filter { it.type != "PARTY_INVITE" }.all { it.read })
            assertTrue("읽음이 초대에 답하지 않는다", after.none { it.actioned })
            assertEquals(1, dao.observeUnreadCount().first())

            // 거절은 그 초대 알림 하나만 지운다. 초대가 아닌 알림은 거절로 지우지 않는다
            repo.decline(invite)
            repo.decline(after.single { it.type == "REWARD_EARNED" })
            assertEquals(listOf("PARTY_INVITE", "EVENT_REWARD", "REWARD_EARNED"), dao.observeAll(100).first().map { it.type })
        } finally { db.close() }
    }

    /** 읽음은 앱을 다시 켜도 남는다(같은 파일 데이터베이스를 닫았다 다시 연다) */
    @Test fun readStateSurvivesRestart() = runBlocking {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val name = "notification-restart-test.db"
        context.deleteDatabase(name)
        try {
            val first = Room.databaseBuilder(context, AppDatabase::class.java, name).build()
            val id = try {
                first.notificationDao().insert(NotificationEntity(timestamp = 1, type = "REWARD_EARNED", argText = "t",
                    argAmount = 1.0, argExtra = "", read = false, actioned = false))
                first.notificationDao().insert(NotificationEntity(timestamp = 2, type = "CREW_INVITE", argText = "t",
                    argAmount = 0.0, argExtra = "crew", read = false, actioned = false))
                val invite = first.notificationDao().observeAll(100).first().single { it.type == "CREW_INVITE" }
                NotificationRepository(first.notificationDao()).markRead(invite.id)
                invite.id
            } finally { first.close() }
            val again = Room.databaseBuilder(context, AppDatabase::class.java, name).build()
            try {
                val rows = again.notificationDao().observeAll(100).first()
                assertTrue(rows.single { it.id == id }.read)
                assertFalse(rows.single { it.id == id }.actioned)
                assertFalse(rows.single { it.type == "REWARD_EARNED" }.read)
            } finally { again.close() }
        } finally {
            context.deleteDatabase(name)
        }
    }
}
