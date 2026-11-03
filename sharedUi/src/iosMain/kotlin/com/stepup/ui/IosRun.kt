@file:OptIn(kotlinx.cinterop.ExperimentalForeignApi::class)

package com.stepup.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import com.stepup.android.data.local.WalkSessionEntity
import com.stepup.android.domain.RewardEconomy
import com.stepup.android.domain.RunEngine
import com.stepup.android.domain.RunTrack
import com.stepup.android.domain.epochMillis
import com.stepup.android.domain.zeroPad
import com.stepup.android.ui.screens.walk.FreeDistance
import com.stepup.android.ui.screens.walk.FreeLiveMode
import com.stepup.android.ui.screens.walk.FreeRunLive
import com.stepup.android.ui.screens.walk.FreeRunNumbers
import com.stepup.android.ui.screens.walk.FreeRunResult
import com.stepup.android.ui.screens.walk.averageSpeedKmh
import com.stepup.android.ui.screens.walk.freeAveragePaceSec
import com.stepup.android.ui.screens.walk.freePaceSec
import com.stepup.android.ui.theme.StepUpColors
import kotlinx.cinterop.useContents
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import platform.CoreLocation.CLActivityTypeFitness
import platform.CoreLocation.CLLocation
import platform.CoreLocation.CLLocationManager
import platform.CoreLocation.CLLocationManagerDelegateProtocol
import platform.CoreLocation.kCLLocationAccuracyBest
import platform.CoreMotion.CMPedometer
import platform.Foundation.NSDate
import platform.Foundation.timeIntervalSince1970
import platform.darwin.NSObject

/**
 * iOS 의 러닝 한 번(안드로이드 WalkSessionService 자리) — Core Location(위치) · Core Motion(걸음) · 1초 시계를 공용 셈 [RunEngine] 에 넣는다.
 * 위치는 "사용하는 동안" 권한으로 받고 러닝 중에는 백그라운드에서도 받는다(UIBackgroundModes location · 파란 표시).
 * 끝나면 같은 폰 DB(strideup.db)의 walk_sessions 에 안드로이드와 같은 모양으로 남긴다 — 올리기는 IosUploads(공용 UploadPass)가 한다.
 */
object IosRunSession {
    private var engine: RunEngine? = null
    private val _state = MutableStateFlow<RunEngine.State?>(null)
    val state: StateFlow<RunEngine.State?> = _state

    private val pedometer = CMPedometer()
    private val manager = CLLocationManager()
    private val delegate = object : NSObject(), CLLocationManagerDelegateProtocol {
        override fun locationManager(manager: CLLocationManager, didUpdateLocations: List<*>) {
            val e = engine ?: return
            didUpdateLocations.filterIsInstance<CLLocation>().forEach { loc ->
                val (lat, lng) = loc.coordinate.useContents { latitude to longitude }
                val accuracy = loc.horizontalAccuracy.takeIf { it >= 0 }?.toFloat()
                val at = (loc.timestamp.timeIntervalSince1970 * 1000).toLong()
                e.fix(lat, lng, accuracy, at)
            }
            publish()
        }
    }

    val active: Boolean get() = engine != null

    fun start() {
        if (engine != null) return
        val now = epochMillis()
        engine = RunEngine(now)
        manager.delegate = delegate
        manager.desiredAccuracy = kCLLocationAccuracyBest
        manager.activityType = CLActivityTypeFitness
        manager.requestWhenInUseAuthorization()
        manager.allowsBackgroundLocationUpdates = true
        manager.showsBackgroundLocationIndicator = true
        manager.startUpdatingLocation()
        if (CMPedometer.isStepCountingAvailable()) {
            pedometer.startPedometerUpdatesFromDate(NSDate()) { data, _ ->
                val steps = data?.numberOfSteps?.intValue ?: return@startPedometerUpdatesFromDate
                engine?.steps(steps, epochMillis())
                publish()
            }
        }
        publish()
    }

    fun tick() {
        engine?.tick(epochMillis())
        publish()
    }

    fun pause() {
        engine?.pause(epochMillis())
        publish()
    }

    fun resume() {
        engine?.resume(epochMillis())
        publish()
    }

    /** 마친다 — 기록을 폰 DB 에 남기고 결과를 돌려준다. 시간이 없는 러닝(0초)은 남기지 않는다 */
    suspend fun stop(): Finished? {
        val e = engine ?: return null
        val now = epochMillis()
        e.tick(now)
        manager.stopUpdatingLocation()
        pedometer.stopPedometerUpdates()
        engine = null
        val s = e.state(now)
        _state.value = null
        if (s.elapsedSec <= 0) return null
        val meters = e.creditedMeters()
        IosApp.db.walkSessionDao().insert(
            WalkSessionEntity(
                startedAt = s.startedAt, endedAt = now, steps = s.steps, durationSec = s.elapsedSec,
                distanceMeters = meters, calories = RewardEconomy.calories(s.steps), pointsEarned = 0.0,
                recordingOwner = IosApp.sessions.recordingOwner(),
                track = RunTrack.encode(s.track),
            ),
        )
        return Finished(s, meters)
    }

    private fun publish() {
        _state.value = engine?.state(epochMillis())
    }

    data class Finished(val state: RunEngine.State, val meters: Double)
}

/** 안드로이드 formatDuration 과 같은 글자(1:02:03 · 12:34) */
private fun clock(totalSec: Long): String {
    val h = totalSec / 3600
    val m = (totalSec % 3600) / 60
    val s = totalSec % 60
    return if (h > 0) "$h:${zeroPad(m, 2)}:${zeroPad(s, 2)}" else "${zeroPad(m, 2)}:${zeroPad(s, 2)}"
}

/**
 * iOS 자유 러닝 화면 — 안드로이드와 같은 FreeRunLive · FreeRunResult. 지도 칸은 지도를 옮길 때까지 남색 판.
 * 1초 길게 눌러 마치면 기록을 남기고 결과를 보인다.
 */
@Composable
fun IosFreeRun(onClose: () -> Unit) {
    val state by IosRunSession.state.collectAsState()
    val scope = rememberCoroutineScope()
    var finished by remember { mutableStateOf<IosRunSession.Finished?>(null) }
    var saving by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) {
        if (!IosRunSession.active && finished == null) IosRunSession.start()
        while (true) {
            IosRunSession.tick()
            delay(1_000)
        }
    }
    val mapSlot: @Composable androidx.compose.foundation.layout.BoxScope.() -> Unit = {
        Box(Modifier.fillMaxSize().background(Color(0xFF0B1A33)))
    }
    Box(Modifier.fillMaxSize().background(StepUpColors.night).safeDrawingPadding()) {
        val done = finished
        if (done != null) {
            val km = (done.meters / 1000).takeIf { it > 0 }
            FreeRunResult(
                numbers = FreeRunNumbers(
                    time = clock(done.state.elapsedSec),
                    paceSec = freeAveragePaceSec(km, done.state.elapsedSec),
                    distance = km?.let { FreeDistance.Km(it) } ?: FreeDistance.Unknown,
                    speedKmh = km?.let { averageSpeedKmh(it, done.state.elapsedSec) },
                    durability = null, average = true,
                ),
                map = null,
                onDone = onClose, onShare = {}, onRecords = onClose, onDelete = onClose,
                startedAt = done.state.startedAt,
            )
        } else {
            val s = state
            val paused = s?.paused == true
            val speed = s?.let { st -> averageSpeedKmh(st.liveKm, st.elapsedSec) }
            FreeRunLive(
                mode = if (saving) FreeLiveMode.Saving else FreeLiveMode.Live,
                numbers = FreeRunNumbers(
                    time = clock(s?.elapsedSec ?: 0), paceSec = freePaceSec(speed),
                    distance = FreeDistance.Km(s?.liveKm ?: 0.0), speedKmh = speed, durability = null,
                ),
                map = mapSlot,
                onPrimary = { if (paused) IosRunSession.resume() else IosRunSession.pause() },
                onSecondary = {},
                paused = paused,
                gpsOk = s?.here != null,
                onHoldEnd = {
                    if (saving) return@FreeRunLive
                    saving = true
                    scope.launch {
                        val result = runCatching { IosRunSession.stop() }.getOrNull()
                        saving = false
                        if (result == null) onClose() else finished = result
                    }
                },
            )
        }
    }
}
