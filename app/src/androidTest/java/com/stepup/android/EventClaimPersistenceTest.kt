package com.stepup.android

import androidx.room.Room
import androidx.test.platform.app.InstrumentationRegistry
import com.stepup.android.data.local.AppDatabase
import com.stepup.android.data.local.ClaimedEventEntity
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Test

class EventClaimPersistenceTest {
    @Test fun walletTotalsIncludeEntriesOutsideRecentHistory() = runBlocking {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val db = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java).build()
        try {
            val dao = db.rewardDao()
            assertEquals(0.0, dao.observeTotals().first().balance, 0.0)
            repeat(125) { index ->
                dao.insert(com.stepup.android.data.local.RewardEntity(timestamp = index.toLong(),
                    type = "EARN_EVENT", amount = 2.0, description = "test receipt"))
            }
            dao.insert(com.stepup.android.data.local.RewardEntity(timestamp = 200,
                type = "SPEND_UPGRADE", amount = -25.0, description = "test purchase"))
            assertEquals(100, dao.observeLedger(100).first().size)
            val totals = dao.observeTotals().first()
            assertEquals(250.0, totals.earned, 0.0)
            assertEquals(25.0, totals.spent, 0.0)
            assertEquals(225.0, totals.balance, 0.0)
        } finally { db.close() }
    }

    @Test fun receiptCreditAndNotificationRollbackAndRetryAsOneUnit() = runBlocking {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val db = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java).build()
        try {
            val dao = db.claimedEventDao()
            val claim = ClaimedEventEntity("step_surge:2026-W39", 1, 250.0)
            // Fail AFTER the marker and credit were written, as a disk/database error could.
            db.openHelper.writableDatabase.execSQL("""
                CREATE TRIGGER fail_event_notification BEFORE INSERT ON notifications
                BEGIN SELECT RAISE(ABORT, 'injected notification write failure'); END
            """.trimIndent())
            assertTrue(runCatching { dao.recordPaidClaim(claim, "step_surge") }.isFailure)
            assertNull(dao.byId(claim.eventId))
            assertEquals(0.0, db.rewardDao().balanceNow(), 0.0)
            assertTrue(db.notificationDao().observeAll(100).first().isEmpty())
            db.openHelper.writableDatabase.execSQL("DROP TRIGGER fail_event_notification")
            val writes = coroutineScope {
                (1..10).map { async { dao.recordPaidClaim(claim, "step_surge") } }.awaitAll()
            }
            assertEquals(1, writes.count { it })
            assertEquals(250.0, db.rewardDao().balanceNow(), 0.0)
            assertEquals(1, db.notificationDao().observeAll(100).first().size)
            assertEquals(claim, dao.byId(claim.eventId))
            for (invalid in listOf(Double.NaN, Double.POSITIVE_INFINITY, -1.0, 0.0)) {
                assertTrue(runCatching { dao.recordPaidClaim(claim.copy(eventId = "invalid", amount = invalid), "invalid") }.isFailure)
            }
            assertNull(dao.byId("invalid"))
            assertEquals(250.0, db.rewardDao().balanceNow(), 0.0)
        } finally { db.close() }
    }
}
