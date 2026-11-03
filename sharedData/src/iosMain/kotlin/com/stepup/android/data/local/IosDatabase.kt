@file:OptIn(kotlinx.cinterop.ExperimentalForeignApi::class)

package com.stepup.android.data.local

import androidx.room.Room
import androidx.sqlite.driver.bundled.BundledSQLiteDriver
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import platform.Foundation.NSDocumentDirectory
import platform.Foundation.NSFileManager
import platform.Foundation.NSUserDomainMask

/**
 * iOS 의 폰 DB — 앱 문서 폴더의 strideup.db(안드로이드와 같은 이름 · 같은 표 · 판 19). 새로 깐 앱이라 옛 판 길은 없다.
 * 판을 올리면(안드로이드 AppMigrations 에 길을 더할 때) iOS 도 같은 길을 연결(SQLiteConnection)로 더한다.
 */
fun buildIosDatabase(): AppDatabase {
    val dir = NSFileManager.defaultManager.URLForDirectory(NSDocumentDirectory, NSUserDomainMask, null, true, null)?.path
        ?: error("문서 폴더를 찾지 못했습니다")
    return Room.databaseBuilder<AppDatabase>(name = "$dir/strideup.db")
        .setDriver(BundledSQLiteDriver())
        .setQueryCoroutineContext(Dispatchers.IO)
        .build()
}
