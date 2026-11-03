package com.stepup.android.data.local

import androidx.room.immediateTransaction
import androidx.room.useWriterConnection

actual suspend fun <R> AppDatabase.inTransaction(block: suspend () -> R): R =
    useWriterConnection { connection -> connection.immediateTransaction { block() } }
