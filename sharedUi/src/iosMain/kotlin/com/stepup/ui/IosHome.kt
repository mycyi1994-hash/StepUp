@file:OptIn(kotlinx.cinterop.ExperimentalForeignApi::class)

package com.stepup.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import com.stepup.android.domain.RewardEconomy
import com.stepup.android.ui.screens.home.GpsReadiness
import com.stepup.android.ui.screens.home.HomeContent
import com.stepup.android.ui.screens.home.RunPhotos
import com.stepup.android.ui.theme.StepUpColors
import kotlin.coroutines.resume
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import platform.CoreLocation.CLLocationManager
import platform.CoreLocation.kCLAuthorizationStatusAuthorizedAlways
import platform.CoreLocation.kCLAuthorizationStatusAuthorizedWhenInUse
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

/**
 * iOS 러닝 탭 첫 화면 — 안드로이드와 같은 HomeContent. 오늘 걸음(Core Motion) · 목표(설정) · 거리(걸음 기준, 안드로이드와 같은 셈) ·
 * GPS 준비(위치 권한). 오늘의 SUP · 이번 주 러닝 수는 서버 · 기록을 붙일 때 채운다(그 전에는 "—").
 */
@Composable
fun IosHome(onStartRun: () -> Unit) {
    val goal by IosApp.prefs.dailyGoal.collectAsState(initial = 0)
    var steps by remember { mutableStateOf<Int?>(null) }
    var stepsAllowed by remember { mutableStateOf(IosSteps.allowed()) }
    LaunchedEffect(Unit) {
        while (true) {
            steps = IosSteps.today() ?: steps
            stepsAllowed = IosSteps.allowed()
            delay(5_000)
        }
    }
    val gps = when (CLLocationManager.authorizationStatus()) {
        kCLAuthorizationStatusAuthorizedWhenInUse, kCLAuthorizationStatusAuthorizedAlways -> GpsReadiness.READY
        else -> GpsReadiness.NO_PERMISSION
    }
    val photoIndex = remember { kotlin.random.Random.nextInt(RunPhotos.names.size) }
    val loaded = steps != null && goal > 0
    // 아래 안전 영역은 하단 탭(IosMain)이 받는다
    Box(Modifier.fillMaxSize().background(StepUpColors.night).statusBarsPadding()) {
        HomeContent(
            loaded = loaded, todaySteps = steps ?: 0, goal = goal,
            hasPermission = stepsAllowed, firstHome = false, running = false,
            largeText = LocalDensity.current.fontScale > 1.2f,
            earned = null, km = steps?.let { RewardEconomy.distanceMeters(it) / 1000.0 }, weekRuns = null,
            onStartRun = onStartRun, onRequestPermission = { }, onOpenDetails = { },
            photoIndex = photoIndex, gps = gps,
        )
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
