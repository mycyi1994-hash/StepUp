package com.stepup.android

import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.room.Room
import androidx.test.platform.app.InstrumentationRegistry
import com.stepup.android.data.local.AppDatabase
import com.stepup.android.data.local.WalkSessionEntity
import com.stepup.android.data.prefs.UserPrefs
import com.stepup.android.data.repo.RunSettlementRepository
import com.stepup.android.domain.SessionReward
import java.io.File
import java.time.LocalDate
import java.util.UUID
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.first
import org.junit.Assert.*
import org.junit.Test

class RunSettlementPersistenceTest {
    private fun run() = WalkSessionEntity(startedAt = 1_000, endedAt = 501_000,
        recordingOwner = "account:runner-a", steps = 500, durationSec = 500,
        distanceMeters = 350.0, calories = 20.0, pointsEarned = 0.0)

    @Test fun failedReceiptRollsBackAllRowsAndConcurrentFinishCommitsOnce() = runBlocking {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
        val file = File(context.cacheDir, "run-settlement-${UUID.randomUUID()}.preferences_pb")
        val prefs = UserPrefs(context, PreferenceDataStoreFactory.create(scope = scope, produceFile = { file }))
        val db = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java).build()
        try {
            val repo = RunSettlementRepository(db, prefs)
            val initialEnergy = prefs.energy.first()
            val result = SessionReward(500, 3.0, 1.0)
            db.openHelper.writableDatabase.execSQL("""
                CREATE TRIGGER fail_run_receipt BEFORE INSERT ON run_settlements
                BEGIN SELECT RAISE(ABORT, 'receipt unavailable'); END
            """.trimIndent())
            assertTrue(runCatching { repo.settle(run()) { result } }.isFailure)
            assertEquals(0, db.walkSessionDao().observeSessionCount().first())
            assertEquals(0.0, db.rewardDao().balanceNow(), 0.0)
            assertEquals(0, db.notificationDao().count())
            assertEquals(initialEnergy, prefs.energy.first(), 0.0)
            db.openHelper.writableDatabase.execSQL("DROP TRIGGER fail_run_receipt")
            var calculations = 0
            val results = (1..8).map {
                async(Dispatchers.IO) { repo.settle(run()) { calculations++; result } }
            }.awaitAll()
            assertTrue(results.all { it == result })
            assertEquals(1, calculations)
            assertEquals(1, db.walkSessionDao().observeSessionCount().first())
            assertEquals(3.0, db.rewardDao().balanceNow(), 0.0)
            assertEquals(1, db.notificationDao().count())
            assertEquals(initialEnergy - 1.0, prefs.energy.first(), 0.0)
        } finally {
            db.close(); scope.coroutineContext.job.cancelAndJoin(); file.delete()
        }
    }

    @Test fun energyAckFailureReopensWithoutAnotherCreditOrDebit() = runBlocking {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val id = UUID.randomUUID()
        val dbName = "run-settlement-$id.db"
        val file = File(context.cacheDir, "run-settlement-$id.preferences_pb")
        var scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
        fun prefs() = UserPrefs(context, PreferenceDataStoreFactory.create(scope = scope, produceFile = { file }))
        var preferences = prefs()
        var db = Room.databaseBuilder(context, AppDatabase::class.java, dbName).build()
        try {
            val initialEnergy = preferences.energy.first()
            val result = SessionReward(500, 3.0, 1.0)
            db.openHelper.writableDatabase.execSQL("""
                CREATE TRIGGER fail_run_ack BEFORE UPDATE ON run_settlements
                BEGIN SELECT RAISE(ABORT, 'ack unavailable'); END
            """.trimIndent())
            assertTrue(runCatching { RunSettlementRepository(db, preferences).settle(run()) { result } }.isFailure)
            assertEquals(1, db.runSettlementDao().pendingEnergy().size)
            assertEquals(3.0, db.rewardDao().balanceNow(), 0.0)
            assertEquals(initialEnergy - 1.0, preferences.energy.first(), 0.0)
            preferences.consumeEnergy(LocalDate.now().toEpochDay(), 0.25)
            db.openHelper.writableDatabase.execSQL("DROP TRIGGER fail_run_ack")
            db.close(); scope.coroutineContext.job.cancelAndJoin()
            scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
            preferences = prefs()
            db = Room.databaseBuilder(context, AppDatabase::class.java, dbName).build()
            val repo = RunSettlementRepository(db, preferences)
            assertEquals(result, repo.settle(run()) { error("Replayed run must not recalculate") })
            repo.recoverEnergy()
            assertEquals(1, db.walkSessionDao().observeSessionCount().first())
            assertEquals(1, db.notificationDao().count())
            assertEquals(3.0, db.rewardDao().balanceNow(), 0.0)
            assertEquals(initialEnergy - 1.25, preferences.energy.first(), 0.0)
            assertTrue(db.runSettlementDao().pendingEnergy().isEmpty())
            // An older day's delayed receipt must not consume a later day's refill.
            preferences.consumeRunEnergy("old-day", LocalDate.now().toEpochDay() - 1, 2.0)
            assertEquals(initialEnergy - 1.25, preferences.energy.first(), 0.0)
        } finally {
            db.close(); scope.coroutineContext.job.cancelAndJoin()
            context.deleteDatabase(dbName); file.delete()
        }
    }
}
