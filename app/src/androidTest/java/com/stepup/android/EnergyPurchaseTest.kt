package com.stepup.android

import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.room.Room
import androidx.test.platform.app.InstrumentationRegistry
import com.stepup.android.data.local.AppDatabase
import com.stepup.android.data.prefs.UserPrefs
import com.stepup.android.data.repo.BoostRepository
import com.stepup.android.data.repo.RewardRepository
import com.stepup.android.domain.BoostType
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.job
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Test
import java.io.File
import java.time.LocalDate
import java.util.UUID

class EnergyPurchaseTest {
    @Test fun interruptedDeliveryRetriesWithoutAnotherDebitOrEnergyCredit() = runBlocking {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        var scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
        val fixtureId = UUID.randomUUID()
        val file = File(context.cacheDir, "energy-test-$fixtureId.preferences_pb")
        val databaseName = "energy-test-$fixtureId.db"
        var store = PreferenceDataStoreFactory.create(scope = scope, produceFile = { file })
        var prefs = UserPrefs(context, store)
        var db = Room.databaseBuilder(context, AppDatabase::class.java, databaseName).build()
        try {
            val rewards = RewardRepository(db.rewardDao(), db.sneakerDao(), db.boostDao(), db.notificationDao(), prefs)
            val boosts = BoostRepository(db, rewards, prefs)
            val today = LocalDate.now().toEpochDay()
            prefs.consumeEnergy(today, 100.0)
            rewards.credit("EARN_EVENT", 100.0, "test fixture")
            db.openHelper.writableDatabase.execSQL("""
                CREATE TRIGGER fail_energy_receipt BEFORE INSERT ON energy_purchases
                BEGIN SELECT RAISE(ABORT, 'receipt failure'); END
            """.trimIndent())
            assertTrue(runCatching { boosts.purchase(BoostType.ENERGY_CELL) }.isFailure)
            assertEquals(100.0, db.rewardDao().balanceNow(), 0.0)
            assertEquals(0.0, prefs.energy.first(), 0.0)
            db.openHelper.writableDatabase.execSQL("DROP TRIGGER fail_energy_receipt")
            db.openHelper.writableDatabase.execSQL("""
                CREATE TRIGGER fail_energy_ack BEFORE INSERT ON notifications
                BEGIN SELECT RAISE(ABORT, 'acknowledgement failure'); END
            """.trimIndent())
            assertTrue(runCatching { boosts.purchase(BoostType.ENERGY_CELL) }.isFailure)
            assertEquals(50.0, db.rewardDao().balanceNow(), 0.0)
            assertEquals(2.0, prefs.energy.first(), 0.0)
            assertEquals(1, db.energyPurchaseDao().pending().size)
            prefs.consumeEnergy(today, 1.0) // Activity after delivery must not be undone by replay.
            db.openHelper.writableDatabase.execSQL("DROP TRIGGER fail_energy_ack")
            // Reopen both persistence layers from disk, rather than reusing their caches.
            db.close()
            scope.coroutineContext.job.cancelAndJoin()
            scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
            store = PreferenceDataStoreFactory.create(scope = scope, produceFile = { file })
            prefs = UserPrefs(context, store)
            db = Room.databaseBuilder(context, AppDatabase::class.java, databaseName).build()
            val reopenedRewards = RewardRepository(db.rewardDao(), db.sneakerDao(), db.boostDao(), db.notificationDao(), prefs)
            val recreated = BoostRepository(db, reopenedRewards, prefs)
            assertNull(recreated.purchase(BoostType.ENERGY_CELL)) // Retries the pending receipt.
            recreated.recoverEnergyPurchases()
            assertEquals(50.0, db.rewardDao().balanceNow(), 0.0)
            assertEquals(1.0, prefs.energy.first(), 0.0)
            assertTrue(db.energyPurchaseDao().pending().isEmpty())
            assertEquals(1, db.notificationDao().count())
        } finally {
            db.close()
            scope.coroutineContext.job.cancelAndJoin()
            context.deleteDatabase(databaseName)
            file.delete()
        }
    }
}
