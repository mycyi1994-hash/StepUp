package com.stepup.android

import androidx.room.Room
import androidx.test.platform.app.InstrumentationRegistry
import com.stepup.android.data.local.AppDatabase
import com.stepup.android.data.local.NotificationEntity
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Test

class NotificationPersistenceTest {
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
