package com.stepup.android.data.local

import androidx.room.Room
import androidx.sqlite.driver.bundled.BundledSQLiteDriver
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

/** iOS 에서 Room KMP 가 열리고(KSP 가 만든 생성자 · 번들 SQLite) 같은 표 · DAO 가 돈다 */
class IosDatabaseTest {
    @Test fun opensAndRoundTrips() = runTest {
        val db = Room.inMemoryDatabaseBuilder<AppDatabase>().setDriver(BundledSQLiteDriver()).build()
        val dao = db.claimedEventDao()
        assertNull(dao.byId("e1"))
        dao.insert(ClaimedEventEntity("e1", 1_793_628_440_000L, 12.5))
        // 같은 키는 무시(IGNORE) — 안드로이드와 같은 규칙
        dao.insert(ClaimedEventEntity("e1", 0L, 0.0))
        assertEquals(ClaimedEventEntity("e1", 1_793_628_440_000L, 12.5), dao.byId("e1"))
        db.close()
    }
}
