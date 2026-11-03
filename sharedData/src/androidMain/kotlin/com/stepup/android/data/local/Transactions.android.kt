package com.stepup.android.data.local

import androidx.room.withTransaction

actual suspend fun <R> AppDatabase.inTransaction(block: suspend () -> R): R = withTransaction { block() }
