package com.stepup.android.`data`.local

import androidx.room.RoomDatabaseConstructor

public actual object AppDatabaseConstructor : RoomDatabaseConstructor<AppDatabase> {
  actual override fun initialize(): AppDatabase = com.stepup.android.`data`.local.AppDatabase_Impl()
}
