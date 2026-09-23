package com.stepup.android

import androidx.room.Room
import androidx.test.platform.app.InstrumentationRegistry
import com.stepup.android.data.local.AppDatabase
import com.stepup.android.data.local.DailyStepsEntity
import com.stepup.android.data.local.WalkSessionEntity
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Test

class RunTotalsTest {
    @Test fun challengeRecordsExcludeUnconfirmedFlaggedAndVoidedRuns() = runBlocking {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val db = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java).build()
        try {
            val dao = db.walkSessionDao()
            val base = WalkSessionEntity(startedAt = 1, endedAt = 1000, steps = 1000,
                durationSec = 600, distanceMeters = 1500.0, calories = 0.0, pointsEarned = 0.0)
            dao.insert(base) // pending
            dao.insert(base.copy(uploadState = "REJECTED", verdict = "VOID"))
            dao.insert(base.copy(uploadState = "SIGNED", verdict = "FLAGGED"))
            dao.insert(base.copy(uploadState = "SIGNED", verdict = "VOID"))
            dao.insert(base.copy(uploadState = "SIGNED", verdict = "CLEAN"))
            val verified = dao.observeVerifiedSessions().first()
            assertEquals(1, verified.size)
            assertEquals("CLEAN", verified.single().verdict)
            assertEquals(5, dao.observeRunTotals().first().runs) // history is preserved
        } finally { db.close() }
    }
    @Test fun totalsUseEverySavedRunAndExcludePassiveSteps() = runBlocking {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val db = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java).build()
        try {
            val dao = db.walkSessionDao()
            assertEquals(0, dao.observeRunTotals().first().runs)
            db.stepDao().upsert(DailyStepsEntity(1, 100000, 8000, 1))
            // More records than the recent-history page limit; never silently truncate totals.
            repeat(205) { i ->
                dao.insert(WalkSessionEntity(startedAt = i.toLong(), endedAt = i + 1000L,
                    steps = 1000, durationSec = 600, distanceMeters = 1500.0,
                    calories = 0.0, pointsEarned = 0.0))
            }
            val totals = dao.observeRunTotals().first()
            assertEquals(205, totals.runs)
            assertEquals(307500.0, totals.meters, 0.001)
        } finally { db.close() }
    }
}
