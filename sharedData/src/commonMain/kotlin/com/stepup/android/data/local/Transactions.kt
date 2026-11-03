package com.stepup.android.data.local

/**
 * 한 번에 쓰기(트랜잭션) — 안드로이드는 예전 그대로 Room 의 withTransaction, iOS 는 Room KMP 의 쓰기 연결 + 즉시 트랜잭션.
 * 저장소 코드(공용)는 이것만 부른다.
 */
expect suspend fun <R> AppDatabase.inTransaction(block: suspend () -> R): R
