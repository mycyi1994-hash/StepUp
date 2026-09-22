package com.stepup.android

import android.database.sqlite.SQLiteDatabase
import androidx.room.Room
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.stepup.android.data.local.AppDatabase
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class DatabaseMigrationTest {
    private val context get() = InstrumentationRegistry.getInstrumentation().targetContext

    @Test fun versionSixUpgradePreservesRecordsBalanceAndInventory() {
        val name = "migration-v6-test.db"
        context.deleteDatabase(name)
        val path = context.getDatabasePath(name).apply { parentFile!!.mkdirs() }
        val fixture = InstrumentationRegistry.getInstrumentation().context.assets
            .open("schema-v6.sql").bufferedReader().use { it.readText() }
        SQLiteDatabase.openOrCreateDatabase(path, null).use { db ->
            fixture.lineSequence().filter { it.startsWith("CREATE TABLE") }.forEach(db::execSQL)
            db.execSQL("INSERT INTO daily_steps VALUES (20000, 12840, 8000, 12345)")
            db.execSQL("INSERT INTO rewards VALUES (41, 12345, 'EARN_WALK', 82.82, 'Retained reward')")
            db.execSQL("INSERT INTO walk_sessions VALUES (17, 1000, 3601000, 9000, 3600, 6840.0, 360.0, 62.82)")
            db.execSQL("INSERT INTO sneakers VALUES (9, 'WIND', 'RARE', 1, 7, 77, 1.15, 1.12, 95, 1, 12345)")
            db.version = 6
        }
        val db = Room.databaseBuilder(context, AppDatabase::class.java, name)
            .addMigrations(*AppDatabase.MIGRATIONS).build()
        try {
            val sql = db.openHelper.writableDatabase // Room validates the complete new schema.
            sql.query("SELECT steps FROM daily_steps WHERE epochDay=20000").use {
                assertTrue(it.moveToFirst()); assertEquals(12840, it.getInt(0))
            }
            sql.query("SELECT amount, description FROM rewards WHERE id=41").use {
                assertTrue(it.moveToFirst()); assertEquals(82.82, it.getDouble(0), 0.001)
                assertEquals("Retained reward", it.getString(1))
            }
            sql.query("SELECT steps, pointsEarned, uploadState, track FROM walk_sessions WHERE id=17").use {
                assertTrue(it.moveToFirst()); assertEquals(9000, it.getInt(0))
                assertEquals(62.82, it.getDouble(1), 0.001)
                assertEquals("REJECTED", it.getString(2)); assertEquals("", it.getString(3))
            }
            sql.query("SELECT level, mintNumber, luck, serverId FROM sneakers WHERE id=9").use {
                assertTrue(it.moveToFirst()); assertEquals(7, it.getInt(0)); assertEquals(77, it.getInt(1))
                assertEquals(1.15, it.getDouble(2), 0.001); assertEquals(0L, it.getLong(3))
            }
        } finally { db.close(); context.deleteDatabase(name) }
    }

    @Test fun unsupportedVersionLeavesOriginalDataIntact() {
        val name = "migration-unknown-test.db"
        context.deleteDatabase(name)
        val path = context.getDatabasePath(name).apply { parentFile!!.mkdirs() }
        SQLiteDatabase.openOrCreateDatabase(path, null).use { db ->
            db.execSQL("CREATE TABLE retained_record (value TEXT NOT NULL)")
            db.execSQL("INSERT INTO retained_record VALUES ('do not erase')")
            db.version = 99
        }
        val db = Room.databaseBuilder(context, AppDatabase::class.java, name)
            .addMigrations(*AppDatabase.MIGRATIONS).build()
        try {
            try { db.openHelper.writableDatabase; fail("Unsupported schema must not be reset") }
            catch (_: IllegalStateException) { /* expected: no destructive fallback */ }
        } finally { db.close() }
        SQLiteDatabase.openDatabase(path.path, null, SQLiteDatabase.OPEN_READONLY).use { raw ->
            assertEquals(99, raw.version)
            raw.rawQuery("SELECT value FROM retained_record", null).use {
                assertTrue(it.moveToFirst()); assertEquals("do not erase", it.getString(0))
            }
        }
        context.deleteDatabase(name)
    }
}
