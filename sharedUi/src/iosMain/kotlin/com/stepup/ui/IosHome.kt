@file:OptIn(kotlinx.cinterop.ExperimentalForeignApi::class)

package com.stepup.ui

import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import kotlin.coroutines.resume
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import platform.CoreMotion.CMAuthorizationStatusAuthorized
import platform.CoreMotion.CMAuthorizationStatusNotDetermined
import platform.CoreMotion.CMPedometer
import platform.Foundation.NSCalendar
import platform.Foundation.NSDate

/** iOS 의 걸음 — Core Motion(CMPedometer). 안드로이드 걸음 센서 자리 */
object IosSteps {
    private val pedometer = CMPedometer()

    /** 걸음 권한 — 아직 묻지 않았으면 처음 걸음을 물을 때 시스템이 묻는다(NSMotionUsageDescription) */
    fun allowed(): Boolean = CMPedometer.authorizationStatus().let { it == CMAuthorizationStatusAuthorized || it == CMAuthorizationStatusNotDetermined }

    /** 오늘 0시(폰 시간대)부터 지금까지 걸음 — 못 읽으면 null */
    suspend fun today(): Int? {
        if (!CMPedometer.isStepCountingAvailable()) return null
        val start = NSCalendar.currentCalendar.startOfDayForDate(NSDate())
        return suspendCancellableCoroutine { c ->
            pedometer.queryPedometerDataFromDate(start, toDate = NSDate()) { data, error ->
                if (c.isActive) c.resume(if (error != null) null else data?.numberOfSteps?.intValue)
            }
        }
    }
}

/** 오늘 걸음(안드로이드 StepTracker 자리) — Core Motion 을 5초마다 읽어 공용 StepRepository 에 낸다 */
object IosStepSource : com.stepup.android.data.repo.DailyStepSource {
    private val _today = kotlinx.coroutines.flow.MutableStateFlow(0)
    override val todaySteps: kotlinx.coroutines.flow.StateFlow<Int> = _today
    override val isAvailable: Boolean get() = CMPedometer.isStepCountingAvailable()
    private var started = false
    private val scope = kotlinx.coroutines.CoroutineScope(kotlinx.coroutines.SupervisorJob() + kotlinx.coroutines.Dispatchers.Main)

    override fun start() {
        if (started) return
        started = true
        scope.launch {
            while (true) {
                IosSteps.today()?.let { _today.value = it }
                delay(5_000)
            }
        }
    }

    override fun simulateSteps(count: Int) {
        _today.value += count
    }
}
