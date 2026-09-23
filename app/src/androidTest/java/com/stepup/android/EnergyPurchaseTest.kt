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
import kotlinx.coroutines.cancel
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
        val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
        val file = File(context.cacheDir, "energy-test-${UUID.randomUUID()}.preferences_pb")
        val store = PreferenceDataStoreFactory.create(scope = scope, produceFile = { file })
        val prefs = UserPrefs(context, store)
        val db = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java).build()
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
            val recreated = BoostRepository(db, rewards, UserPrefs(context, store))
            assertNull(recreated.purchase(BoostType.ENERGY_CELL)) // Retries the pending receipt.
            recreated.recoverEnergyPurchases()
            assertEquals(50.0, db.rewardDao().balanceNow(), 0.0)
            assertEquals(1.0, prefs.energy.first(), 0.0)
            assertTrue(db.energyPurchaseDao().pending().isEmpty())
            assertEquals(1, db.notificationDao().count())
        } finally {
            db.close()
            scope.cancel()
        }
    }
}
