package com.stepup.android

import androidx.room.Room
import androidx.test.platform.app.InstrumentationRegistry
import com.stepup.android.data.local.AppDatabase
import com.stepup.android.data.repo.toEntity
import com.stepup.android.domain.SneakerMint
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.flow.take
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.withTimeout
import org.junit.Assert.*
import org.junit.Test

class EquipmentPersistenceTest {
    @Test fun activeBoostExpiresWhileScreenRemainsSubscribedWithoutDatabaseWrites() = runBlocking {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val db = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java).build()
        try {
            val prefs = com.stepup.android.data.prefs.UserPrefs(context)
            val rewards = com.stepup.android.data.repo.RewardRepository(db.rewardDao(), db.sneakerDao(),
                db.boostDao(), db.notificationDao(), prefs)
            val boosts = com.stepup.android.data.repo.BoostRepository(db, rewards, prefs)
            val type = com.stepup.android.domain.BoostType.XP_BOOSTER
            val now = System.currentTimeMillis()
            db.boostDao().insert(com.stepup.android.data.local.BoostEntity(
                type = type.id, activatedAt = now, expiresAt = now + 3000L))
            val states = withTimeout(10000L) { boosts.active.take(2).toList() }
            assertEquals(type, states.first().single().type)
            assertTrue(states.last().isEmpty())
            assertFalse(boosts.isActive(type))
        } finally { db.close() }
    }

    @Test fun timedBoostPurchaseRollsBackAndCompetesWithShoesForTheSameBalance() = runBlocking {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val db = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java).build()
        try {
            val prefs = com.stepup.android.data.prefs.UserPrefs(context)
            val rewards = com.stepup.android.data.repo.RewardRepository(db.rewardDao(), db.sneakerDao(),
                db.boostDao(), db.notificationDao(), prefs)
            val shoes = com.stepup.android.data.repo.SneakerRepository(db, rewards)
            val boosts = com.stepup.android.data.repo.BoostRepository(db, rewards, prefs)
            val type = com.stepup.android.domain.BoostType.XP_BOOSTER
            shoes.ensureStarter()
            rewards.credit("EARN_EVENT", 500.0, "test fixture")
            db.openHelper.writableDatabase.execSQL("""
                CREATE TRIGGER fail_boost_notice BEFORE INSERT ON notifications
                BEGIN SELECT RAISE(ABORT, 'injected boost notification failure'); END
            """.trimIndent())
            assertTrue(runCatching { boosts.purchase(type) }.isFailure)
            assertFalse(boosts.isActive(type))
            assertEquals(500.0, db.rewardDao().balanceNow(), 0.0)
            db.openHelper.writableDatabase.execSQL("DROP TRIGGER fail_boost_notice")
            coroutineScope {
                val mint = async { shoes.mint() }
                val boost = async { boosts.purchase(type) }
                mint.await()
                boost.await()
            }
            val hasBoost = boosts.isActive(type)
            assertEquals(if (hasBoost) 1 else 2, db.sneakerDao().count())
            assertEquals(if (hasBoost) 300.0 else 0.0, db.rewardDao().balanceNow(), 0.0)
            assertEquals(1, db.notificationDao().count())
            if (hasBoost) {
                assertEquals(com.stepup.android.data.repo.PurchaseError.ALREADY_ACTIVE, boosts.purchase(type))
                assertEquals(300.0, db.rewardDao().balanceNow(), 0.0)
            }
        } finally { db.close() }
    }

    @Test fun purchasesRollbackOnNotificationFailureAndCannotOverspendConcurrently() = runBlocking {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val db = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java).build()
        try {
            val rewards = com.stepup.android.data.repo.RewardRepository(db.rewardDao(), db.sneakerDao(),
                db.boostDao(), db.notificationDao(), com.stepup.android.data.prefs.UserPrefs(context))
            val repository = com.stepup.android.data.repo.SneakerRepository(db, rewards)
            coroutineScope { (1..8).map { async { repository.ensureStarter() } }.awaitAll() }
            assertEquals(1, db.sneakerDao().count())
            val original = db.sneakerDao().allNow().single()
            rewards.credit("EARN_EVENT", 500.0, "test fixture")
            db.openHelper.writableDatabase.execSQL("""
                CREATE TRIGGER fail_purchase_notice BEFORE INSERT ON notifications
                BEGIN SELECT RAISE(ABORT, 'injected purchase notification failure'); END
            """.trimIndent())
            assertTrue(runCatching { repository.upgrade(original.id) }.isFailure)
            assertEquals(original, db.sneakerDao().byId(original.id))
            assertEquals(500.0, db.rewardDao().balanceNow(), 0.0)
            assertTrue(runCatching { repository.mint() }.isFailure)
            assertEquals(1, db.sneakerDao().count())
            assertEquals(500.0, db.rewardDao().balanceNow(), 0.0)
            db.openHelper.writableDatabase.execSQL("DROP TRIGGER fail_purchase_notice")
            val results = coroutineScope { (1..8).map { async { repository.mint() } }.awaitAll() }
            assertEquals(1, results.count { it != null })
            assertEquals(2, db.sneakerDao().count())
            assertEquals(0.0, db.rewardDao().balanceNow(), 0.0)
            assertEquals(1, db.notificationDao().count())
            assertEquals(original, db.sneakerDao().byId(original.id))
        } finally { db.close() }
    }

    @Test fun equipmentIsExclusiveSurvivesReopenAndRejectsMissingTargets() = runBlocking {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val name = "equipment-persistence-test.db"
        context.deleteDatabase(name)
        fun open() = Room.databaseBuilder(context, AppDatabase::class.java, name).build()
        var db = open()
        try {
            val dao = db.sneakerDao()
            val first = dao.insert(SneakerMint.starter().toEntity())
            val second = dao.insert(SneakerMint.starter().toEntity().copy(equipped = false, level = 7))
            assertTrue(dao.equipExclusively(second) > 0)
            assertEquals(listOf(second), dao.allNow().filter { it.equipped }.map { it.id })
            assertEquals(0, dao.equipExclusively(Long.MAX_VALUE))
            assertEquals(second, dao.equippedNow()!!.id)
            // A storage failure must not unequip the previous shoe or partially apply the new one.
            db.openHelper.writableDatabase.execSQL("""
                CREATE TRIGGER fail_equipment BEFORE UPDATE ON sneakers
                BEGIN SELECT RAISE(ABORT, 'injected equipment storage failure'); END
            """.trimIndent())
            assertTrue(runCatching { dao.equipExclusively(first) }.isFailure)
            assertEquals(second, dao.equippedNow()!!.id)
            assertEquals(1, dao.allNow().count { it.equipped })
            db.openHelper.writableDatabase.execSQL("DROP TRIGGER fail_equipment")
            assertTrue(dao.equipExclusively(first) > 0)
            assertEquals(first, dao.equippedNow()!!.id)
            // Rapid independent selection requests still leave exactly one equipped row.
            coroutineScope {
                (0 until 20).map { index -> async {
                    dao.equipExclusively(if (index % 2 == 0) first else second)
                } }.awaitAll()
            }
            assertEquals(1, dao.allNow().count { it.equipped })
            dao.equipExclusively(second)
            db.close()
            db = open()
            assertEquals(second, db.sneakerDao().equippedNow()!!.id)
            assertEquals(7, db.sneakerDao().byId(second)!!.level)
            assertEquals(2, db.sneakerDao().allNow().size)
        } finally {
            db.close()
            context.deleteDatabase(name)
        }
    }
}
