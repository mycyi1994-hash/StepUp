package com.stepup.android.data.remote

import kotlin.time.Instant

/*
 * 서버 시각 글자(2026-11-03 KMP 3단계 — java.time 대신 kotlin.time). 안드로이드 예전 값과 같다 — jvmTest 의 IsoJvmTest 가 맞대어 본다.
 */

/** java.time.OffsetDateTime.parse(text).toInstant().toEpochMilli() — ISO 시각(끝이 Z 또는 ±hh:mm). 못 읽으면 null */
fun isoOffsetMillis(text: String): Long? = runCatching { Instant.parse(text).toEpochMilliseconds() }.getOrNull()

/** epoch 밀리초를 Postgres 가 읽는 ISO-8601 UTC 문자열로(java.time.Instant.ofEpochMilli(ms).toString() 과 같다) */
fun Long.toIsoInstant(): String = Instant.fromEpochMilliseconds(this).toString()
