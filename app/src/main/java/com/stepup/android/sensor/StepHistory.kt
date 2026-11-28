package com.stepup.android.sensor

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.content.ContextCompat
import com.google.android.gms.common.ConnectionResult
import com.google.android.gms.common.GoogleApiAvailability
import com.google.android.gms.fitness.FitnessLocal
import com.google.android.gms.fitness.LocalRecordingClient
import com.google.android.gms.fitness.data.LocalDataType
import com.google.android.gms.fitness.data.LocalField
import com.google.android.gms.fitness.request.LocalDataReadRequest
import com.google.android.gms.tasks.Task
import java.time.LocalDate
import java.time.ZoneId
import java.util.concurrent.TimeUnit
import kotlin.coroutines.resume
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withTimeoutOrNull

/**
 * 앱을 켜지 않은 날의 걸음(2026-11-26 테스터 "토스 만보기는 앱을 안 켜도 걸음이 쌓인다 · 스텝업은 한 번이라도 켜야 그날 트래킹").
 *
 * 걸음 센서(TYPE_STEP_COUNTER)는 앱이 살아 있을 때만 읽혀, 앱을 연 적 없는 날의 걸음과 하루 넘게 비운 뒤 오늘 앱을 열기 전에 걸은 걸음이
 * 빠졌다([StepTracker] — 어제 잰 적이 없으면 넘겨받지 않는다). 구글 Play 서비스의 기록(Recording API — 기기 안에만 남고 계정 · 서버가 없다,
 * 열흘 보관)이 앱이 꺼져 있는 동안에도 걸음을 적어 두므로, 걸음 일꾼([DailyStepsWorker])과 앱이 켜질 때 그날그날의 합을 읽어 폰 기록에 채운다.
 *
 * 걸음 권한(ACTIVITY_RECOGNITION)이 있어야 하고, Play 서비스가 없거나 낡았으면 아무것도 하지 않는다(센서 셈 그대로).
 */
object StepHistory {

    /** Recording API 가 남기는 날수 — 이보다 앞은 읽지 않는다 */
    const val KEEP_DAYS = 10

    /** 쓸 수 있다 — 걸음 권한 · 새 Play 서비스 */
    fun available(context: Context): Boolean {
        val permitted = Build.VERSION.SDK_INT < Build.VERSION_CODES.Q ||
            ContextCompat.checkSelfPermission(context, Manifest.permission.ACTIVITY_RECOGNITION) == PackageManager.PERMISSION_GRANTED
        if (!permitted) return false
        return runCatching {
            GoogleApiAvailability.getInstance().isGooglePlayServicesAvailable(
                context, LocalRecordingClient.LOCAL_RECORDING_CLIENT_MIN_VERSION_CODE,
            ) == ConnectionResult.SUCCESS
        }.getOrDefault(false)
    }

    /** 걸음 기록을 켠다 — 여러 번 불러도 하나. 권한은 [available] 이 본다 · 그사이 거뒀으면 SecurityException 을 받는다 */
    @android.annotation.SuppressLint("MissingPermission")
    suspend fun subscribe(context: Context): Boolean {
        if (!available(context)) return false
        val client = runCatching { FitnessLocal.getLocalRecordingClient(context) }.getOrNull() ?: return false
        return try {
            client.subscribe(LocalDataType.TYPE_STEP_COUNT_DELTA).awaitDone(TIMEOUT_MS)
        } catch (_: SecurityException) {
            false
        }
    }

    /**
     * 지난 [days]일(오늘 포함)의 날마다 걸음 — 이 폰의 날짜(epochDay) → 걸음. 기록이 없는 날은 빠진다. 못 읽으면 빈 것.
     * 날은 이 폰 시간대의 자정부터 센다(앱의 하루 걸음과 같은 날).
     */
    @android.annotation.SuppressLint("MissingPermission")
    suspend fun readDays(context: Context, days: Int = KEEP_DAYS, zone: ZoneId = ZoneId.systemDefault()): Map<Long, Int> {
        if (!available(context)) return emptyMap()
        val client = runCatching { FitnessLocal.getLocalRecordingClient(context) }.getOrNull() ?: return emptyMap()
        val today = LocalDate.now(zone)
        val first = today.minusDays((days.coerceIn(1, KEEP_DAYS) - 1).toLong())
        val start = first.atStartOfDay(zone).toEpochSecond()
        val end = System.currentTimeMillis() / 1000
        if (end <= start) return emptyMap()
        val request = runCatching {
            LocalDataReadRequest.Builder()
                .aggregate(LocalDataType.TYPE_STEP_COUNT_DELTA)
                .bucketByTime(1, TimeUnit.DAYS)
                .setTimeRange(start, end, TimeUnit.SECONDS)
                .build()
        }.getOrNull() ?: return emptyMap()
        val response = try {
            client.readData(request).awaitOrNull(TIMEOUT_MS)
        } catch (_: SecurityException) {
            null
        } ?: return emptyMap()
        val out = HashMap<Long, Int>()
        runCatching {
            for (bucket in response.buckets) {
                var steps = 0L
                for (set in bucket.dataSets) for (point in set.dataPoints) {
                    steps += runCatching { point.getValue(LocalField.FIELD_STEPS).asInt().toLong() }.getOrDefault(0L)
                }
                if (steps <= 0) continue
                // 칸의 시작 시각이 그 날 — 1일 칸은 위의 자정부터 나뉜다
                val day = java.time.Instant.ofEpochSecond(bucket.getStartTime(TimeUnit.SECONDS)).atZone(zone).toLocalDate().toEpochDay()
                out[day] = (out[day] ?: 0) + steps.coerceAtMost(Int.MAX_VALUE.toLong()).toInt()
            }
        }
        return out
    }

    private const val TIMEOUT_MS = 15_000L
}

/** Play 서비스 작업이 끝나기를 기다린다 — 됐으면 true(값 없는 작업용) */
private suspend fun <T> Task<T>.awaitDone(timeoutMs: Long): Boolean = withTimeoutOrNull(timeoutMs) {
    suspendCancellableCoroutine { cont ->
        addOnSuccessListener { if (cont.isActive) cont.resume(true) }
        addOnFailureListener { if (cont.isActive) cont.resume(false) }
        addOnCanceledListener { if (cont.isActive) cont.resume(false) }
    }
} ?: false

/** Play 서비스 작업을 기다린다 — 실패 · 시간 넘김은 null */
private suspend fun <T> Task<T>.awaitOrNull(timeoutMs: Long): T? = withTimeoutOrNull(timeoutMs) {
    suspendCancellableCoroutine { cont ->
        addOnSuccessListener { value -> if (cont.isActive) cont.resume(value) }
        addOnFailureListener { if (cont.isActive) cont.resume(null) }
        addOnCanceledListener { if (cont.isActive) cont.resume(null) }
    }
}
