package com.stepup.android.service

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.content.pm.ServiceInfo
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat
import androidx.core.app.ServiceCompat
import androidx.core.content.ContextCompat
import com.stepup.android.MainActivity
import com.stepup.android.R
import com.stepup.android.core.ServiceLocator
import com.stepup.android.data.local.WalkSessionEntity
import com.stepup.android.domain.Faction
import com.stepup.android.domain.GeoPoint
import com.stepup.android.domain.RewardEconomy
import com.stepup.android.domain.RunIntegrity
import com.stepup.android.domain.RunTrack
import com.stepup.android.domain.RunVerdict
import com.stepup.android.domain.TrackPoint
import com.stepup.android.domain.haversineMeters
import com.stepup.android.domain.simplify
import com.stepup.android.domain.toGeoPoints
import com.stepup.android.sync.SessionUploadWorker
import com.stepup.android.core.Analytics
import java.time.LocalDate
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

/** 랩 스냅샷 — km·splitSec은 랩을 찍은 시점의 "누적" 값. 구간값은 UI에서 이전 랩과의 차로 구한다. */
data class RunLap(
    val index: Int,
    val km: Double,
    val splitSec: Long,
)

/**
 * 이 좌표가 모의 위치(개발자 옵션의 "모의 위치 앱", 가짜 GPS 앱)에서 왔는가.
 * API 31 부터는 `isMock`, 그 전에는 `isFromMockProvider` 가 같은 표시다.
 */
internal fun isMockLocation(location: Location): Boolean =
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        location.isMock
    } else {
        @Suppress("DEPRECATION")
        location.isFromMockProvider
    }

/** 워킹 세션의 현재 상태. 화면과 서비스가 공유한다. */
enum class RunSaveStatus { IDLE, SAVING, FAILED }

data class WalkSessionState(
    val isActive: Boolean = false,
    val isPaused: Boolean = false,
    val saveStatus: RunSaveStatus = RunSaveStatus.IDLE,
    val steps: Int = 0,
    val elapsedSec: Long = 0,
    val startedAt: Long = 0,
    val recordingOwner: String = com.stepup.android.domain.RecordingOwner.LEGACY,
    /** 파티런 인원 (본인 포함). 1이면 개인 러닝. */
    val partySize: Int = 1,
    /** 마지막 세션 정산 결과 (종료 직후 화면 표시용) */
    val lastRewardPoints: Double? = null,
    val lastRewardedSteps: Int = 0,
    val lastSessionSteps: Int = 0,
    val lastPartySize: Int = 1,
    /**
     * 이번 세션의 랩 기록. 화면이 아니라 세션과 함께 살아서,
     * 러닝 중 화면을 나갔다 돌아와도 랩이 사라지거나 꼬이지 않는다.
     */
    val laps: List<RunLap> = emptyList(),
    /**
     * GPS로 기록한 이번 세션의 실제 경로 — 사람 속도로 인정된 구간만 담긴다.
     *
     * 좌표마다 시각이 붙어 있다. 시각이 없으면 구간 속도를 다시 계산할 수 없고,
     * 그러면 이 세션이 사람이 뛴 것인지 서버가 판정할 방법이 없다.
     */
    val track: List<TrackPoint> = emptyList(),
    /** 최근에 GPS 좌표를 받았는지 (지도 카드 GPS 배지) */
    val gpsFix: Boolean = false,
    /** GPS로 잰 유효 거리(km). 속도 상한을 넘긴 구간은 빠져 있다. */
    val gpsKm: Double = 0.0,
    /** 이번 세션의 최고 속도(km/h) — 사람 범위 안의 값만 */
    val topSpeedKmh: Double = 0.0,
    /** 사람 속도로 인정된 GPS 구간 수 */
    val validSegments: Int = 0,
    /** 속도 상한을 넘겨 버려진 구간 수 */
    val flaggedSegments: Int = 0,
    /** 마지막 세션의 판정 (종료 직후 화면 표시용) */
    val lastVerdict: RunVerdict = RunVerdict.CLEAN,
    val lastTopSpeedKmh: Double = 0.0,
    val lastGpsKm: Double = 0.0,
    /**
     * 방금 끝난 러닝의 운동 시간(초)과 시작 시각 — 완료 화면에 보이기만 한다.
     *
     * 시작 시각은 저장된 세션 줄을 찾는 열쇠다. 완료 화면이 "서버 확인" 상태를
     * 그 러닝의 줄에서 읽어야 한다 — 가장 최근 줄을 아무거나 읽으면 지난 러닝의
     * 상태를 이번 것처럼 보여 줄 수 있다.
     */
    val lastElapsedSec: Long = 0,
    val lastStartedAt: Long = 0,
    /**
     * 이번 러닝 중 폰이 모의 위치(가짜 GPS 앱)를 한 번이라도 알려 왔는가.
     * 그런 러닝은 적립하지 않는다 — 서버도 같은 이유로 무효 처리한다.
     */
    val mockLocation: Boolean = false,
) {
    /** 지도에 그리거나 코스로 저장할 때 쓰는 모양만 남긴 경로 */
    val geoTrack: List<GeoPoint> get() = track.toGeoPoints()

    /** 러닝 중 실시간 판정 — 화면에 경고 배지를 띄우는 근거 */
    val liveVerdict: RunVerdict
        get() = if (mockLocation) {
            RunVerdict.VOID
        } else {
            RunIntegrity.verdict(validSegments, flaggedSegments, steps, elapsedSec)
        }
}

/**
 * 워킹 세션을 추적하는 포그라운드 서비스(health 타입).
 * StepTracker의 오늘 걸음 수 증가분을 세션 걸음으로 집계하고,
 * 종료 시 RewardRepository로 정산한다.
 */
class WalkSessionService : Service() {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private var stepJob: Job? = null
    private var timerJob: Job? = null
    private var locationManager: LocationManager? = null

    /**
     * GPS 리스너 — 8m 이상 움직였을 때만 경로에 점을 추가해
     * 제자리 노이즈로 트랙이 지저분해지는 것을 막는다.
     */
    private val locationListener = object : LocationListener {
        override fun onLocationChanged(location: Location) {
            if (isMockLocation(location)) {
                // 가짜 GPS 앱이 넣은 좌표 — 경로에도 거리에도 넣지 않고, 이번 러닝을 표시만 한다
                _state.update { current ->
                    if (!current.isActive || current.isPaused) current else current.copy(mockLocation = true)
                }
                return
            }
            val p = GeoPoint(location.latitude, location.longitude)
            val now = System.currentTimeMillis()
            // 속도 판정 기준점은 트랙과 따로 든다. 튄 구간의 점은 트랙에 넣지
            // 않지만 기준점은 옮겨야, 다음 구간이 연쇄로 튀지 않는다.
            val prev = speedAnchor
            val prevAt = speedAnchorAt
            speedAnchor = p
            speedAnchorAt = now

            val meters = if (prev == null) 0.0 else haversineMeters(prev, p)
            // 어테스터(economy.js)와 같이 반올림한다. 버리면 2.5초 간격이 2초가 되어
            // 서버보다 25% 엄격하게 튄 구간으로 잡힌다.
            val seconds = if (prevAt == 0L) 0L else (now - prevAt + 500) / 1000
            val plausible = prev == null || RunIntegrity.isPlausible(meters, seconds)

            // 걸음 수집기·타이머와 서로 덮어쓰지 않게 원자적으로 갱신한다
            _state.update { current ->
                if (!current.isActive || current.isPaused) return@update current
                if (!plausible) {
                    // 사람이 낼 수 없는 속도 — 거리도, 경로도 남기지 않는다
                    return@update current.copy(
                        gpsFix = true,
                        flaggedSegments = current.flaggedSegments + 1,
                    )
                }
                val track = current.track
                val moved = track.isEmpty() ||
                    haversineMeters(track.last().toGeoPoint(), p) >= 8.0
                val counted = prev != null && meters >= RunIntegrity.MIN_SEGMENT_METERS &&
                    seconds >= RunIntegrity.MIN_SEGMENT_SEC
                current.copy(
                    track = if (moved) track + TrackPoint(p.lat, p.lng, now) else track,
                    gpsFix = true,
                    gpsKm = if (counted) current.gpsKm + meters / 1000 else current.gpsKm,
                    validSegments = if (counted) current.validSegments + 1 else current.validSegments,
                    topSpeedKmh = RunIntegrity.updateTopSpeed(current.topSpeedKmh, meters, seconds),
                )
            }
        }

        // API 29 이하에서는 아래 셋이 추상 메서드라 반드시 구현해야 한다
        @Deprecated("Deprecated in Java")
        override fun onStatusChanged(provider: String?, status: Int, extras: android.os.Bundle?) = Unit
        override fun onProviderEnabled(provider: String) = Unit
        override fun onProviderDisabled(provider: String) = Unit
    }

    private fun hasLocationPermission(): Boolean =
        ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) ==
            PackageManager.PERMISSION_GRANTED ||
            ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) ==
            PackageManager.PERMISSION_GRANTED

    private fun startLocation() {
        if (!hasLocationPermission()) return
        val lm = getSystemService(LOCATION_SERVICE) as LocationManager
        locationManager = lm
        try {
            if (lm.isProviderEnabled(LocationManager.GPS_PROVIDER)) {
                lm.requestLocationUpdates(
                    LocationManager.GPS_PROVIDER, 2_500L, 6f, locationListener, mainLooper,
                )
            } else if (lm.isProviderEnabled(LocationManager.NETWORK_PROVIDER)) {
                lm.requestLocationUpdates(
                    LocationManager.NETWORK_PROVIDER, 4_000L, 10f, locationListener, mainLooper,
                )
            }
        } catch (_: SecurityException) {
            // 권한이 그 사이 회수됐다면 GPS 없이 진행한다
        }
    }

    private fun stopLocation() {
        locationManager?.removeUpdates(locationListener)
        locationManager = null
    }

    /** 속도 판정용 직전 좌표와 시각. GPS 리스너(메인 루퍼)에서만 만진다. */
    private var speedAnchor: GeoPoint? = null
    private var speedAnchorAt: Long = 0L

    /** 세션 걸음 집계 기준점. 첫 실측값 방출로 초기화된다(null = 아직 미정). */
    private var lastTodaySteps: Int? = null
    @Volatile private var settling = false
    private var startJob: Job? = null
    private var followupsAttempted = false

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_START -> startSession(intent.getIntExtra(EXTRA_PARTY_SIZE, 1).coerceAtLeast(1))
            ACTION_PAUSE -> setPaused(true)
            ACTION_RESUME -> setPaused(false)
            ACTION_STOP -> stopSession()
        }
        return START_NOT_STICKY
    }

    private fun startSession(partySize: Int) {
        if (_state.value.isActive || startJob?.isActive == true) return
        speedAnchor = null
        speedAnchorAt = 0L
        createChannel()
        // 위치 권한이 있을 때만 location 타입을 함께 선언한다 —
        // 권한 없이 선언하면 API 34+에서 시작 자체가 거부된다.
        // location 타입은 API 29(Q)부터 있다. 29~33 에서 빼 두면 화면을 끄거나 앱을
        // 벗어나는 순간 GPS 가 멈춰, 멀쩡한 러닝이 "GPS 거리 부족"으로 깎인다.
        val fgsType = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q && hasLocationPermission()) {
            ServiceInfo.FOREGROUND_SERVICE_TYPE_HEALTH or ServiceInfo.FOREGROUND_SERVICE_TYPE_LOCATION
        } else {
            ServiceInfo.FOREGROUND_SERVICE_TYPE_HEALTH
        }
        ServiceCompat.startForeground(
            this,
            NOTIFICATION_ID,
            buildNotification(0),
            fgsType,
        )
        startJob = scope.launch(Dispatchers.Main.immediate) {
            // Begin tracking only after the recording account has been captured.
            // Finishing later must never read the replacement login account.
            val owner = ServiceLocator.sessionHolder.recordingOwner()
            beginTracking(partySize, owner)
        }
    }

    private fun beginTracking(partySize: Int, owner: String) {
        followupsAttempted = false
        _state.value = WalkSessionState(
            isActive = true,
            startedAt = System.currentTimeMillis(),
            recordingOwner = owner,
            partySize = partySize,
        )

        // 일별 기록/목표 보너스 수집기까지 함께 보장한다 (중복 호출에 안전).
        ServiceLocator.stepRepository.startTracking()
        startLocation()
        val tracker = ServiceLocator.stepTracker
        lastTodaySteps = null

        stepJob = scope.launch {
            tracker.todaySteps.collect { today ->
                // StateFlow 초기값(0)은 실측이 아니므로, 첫 실측값을 기준점으로만 쓰고
                // 그 이후 증가분만 세션 걸음으로 인정한다. 세션 전에 걸은 오늘 걸음이
                // 세션 적립으로 흘러들어오는 것을 막는다.
                if (!tracker.hasReading) return@collect
                val last = lastTodaySteps
                lastTodaySteps = today
                if (last == null) return@collect
                val delta = (today - last).coerceAtLeast(0)
                var stepsAfter = -1
                _state.update { current ->
                    if (!current.isActive || current.isPaused || delta <= 0) return@update current
                    var updated = current.copy(steps = current.steps + delta)
                    // 1km 경계를 넘을 때마다 자동 랩 — 경계 km 값으로 기록하므로
                    // 여러 번 재구성돼도, 화면이 없어도 랩은 정확히 km당 하나다.
                    val km = RewardEconomy.distanceMeters(updated.steps) / 1000
                    var laps = updated.laps
                    while (laps.size < km.toInt()) {
                        laps = laps + RunLap(
                            index = laps.size + 1,
                            km = (laps.size + 1).toDouble(),
                            splitSec = updated.elapsedSec,
                        )
                    }
                    if (laps !== updated.laps) updated = updated.copy(laps = laps)
                    stepsAfter = updated.steps
                    updated
                }
                if (stepsAfter >= 0) updateNotification(stepsAfter)
            }
        }
        timerJob = scope.launch {
            while (isActive) {
                delay(1_000)
                _state.update { current ->
                    if (current.isActive && !current.isPaused) {
                        current.copy(elapsedSec = current.elapsedSec + 1)
                    } else {
                        current
                    }
                }
            }
        }
    }

    private fun setPaused(paused: Boolean) {
        val current = _state.value
        if (current.isActive && current.saveStatus == RunSaveStatus.IDLE) {
            _state.value = current.copy(isPaused = paused)
        }
    }

    private fun stopSession() {
        val current = _state.value
        if (!current.isActive || settling) {
            if (!current.isActive) {
                startJob?.cancel()
                stopSelf()
            }
            return
        }
        settling = true
        _state.update { it.copy(isPaused = true, saveStatus = RunSaveStatus.SAVING) }
        val session = _state.value
        stepJob?.cancel()
        timerJob?.cancel()
        stopLocation()
        scope.launch {
          try {
            // 파티런이면 정산 시점의 실제 인원을 쓴다 — 러닝 중 거리 이탈로 빠진 인원 반영.
            val settleSize = if (session.partySize > 1) {
                ServiceLocator.crewRepository.currentPartySize()
            } else {
                session.partySize
            }
            // 크루 러닝이었다면 어느 크루였는지. 크루 순위가 세는 것이 이 값이다.
            // 파티 상태는 아래 finishParty() 에서 결과 화면으로 넘어가므로
            // 지금 읽어 둔다.
            val partyCrewId = if (session.partySize > 1) {
                ServiceLocator.crewRepository.currentPartyCrewId()
            } else {
                ""
            }
            // 러닝으로 볼 수 없는 세션은 여기서 걸러진다 — 걸음 0으로 정산해
            // 적립도, 에너지 소모도, 코스 완주도 일어나지 않게 한다.
            // 모의 위치가 한 번이라도 보였으면 러닝으로 치지 않는다
            val verdict = if (session.mockLocation) {
                RunVerdict.VOID
            } else {
                RunIntegrity.verdict(
                    validSegments = session.validSegments,
                    flaggedSegments = session.flaggedSegments,
                    steps = session.steps,
                    elapsedSec = session.elapsedSec,
                )
            }
            val creditedSteps = if (verdict.isRewardable) session.steps else 0

            // 기준점을 **먼저** 올린다. 지급하고 나서 올리면 그 사이에 프로세스가
            // 죽었을 때 백그라운드가 같은 걸음을 다시 지급한다. 순서가 곧 안전장치다.
            //
            // 더하지 않고 "지금 이 순간의 오늘 걸음 수"로 못 박는 이유는 두 가지다.
            //  - 자정을 넘긴 세션이 어제 몫까지 오늘 기준점에 얹으면, 오늘 처음
            //    걷는 그만큼이 통째로 사라진다.
            //  - 일시정지 중 걸은 몫은 session.steps에 안 잡히므로, 더하기만으로는
            //    기준점이 모자라 그 몫을 백그라운드가 다시 지급한다.
            ServiceLocator.userPrefs.raiseAccountedTo(
                LocalDate.now().toEpochDay(),
                ServiceLocator.stepRepository.todaySteps.value,
            )

            // 부스트는 정산 **전에** 읽는다. 정산이 신발이나 크루 상태를 건드릴
            // 수 있으므로, 청구서에 적힐 값은 적립을 계산할 때 쓴 값이어야 한다.
            val boostBps = ServiceLocator.rewardRepository.equippedBoostBps()
            // 종족도 같은 이유로 정산 전에 읽는다. 이 값이 종족 랭킹에서
            // 이 거리가 어느 편에 쌓일지를 정한다.
            val equippedFaction = ServiceLocator.database.sneakerDao().equippedNow()
                ?.factionId?.let { Faction.of(it) }
            val reward = ServiceLocator.runSettlementRepository.settle(
                WalkSessionEntity(
                    startedAt = session.startedAt,
                    recordingOwner = session.recordingOwner,
                    endedAt = System.currentTimeMillis(),
                    steps = creditedSteps,
                    durationSec = if (verdict.isRewardable) session.elapsedSec else 0,
                    distanceMeters = RewardEconomy.distanceMeters(creditedSteps),
                    calories = RewardEconomy.calories(creditedSteps),
                    pointsEarned = 0.0, // Replaced by the committed settlement calculation.
                    // 경로는 판정에 쓰이므로 화면용으로 솎아내기 전 원본을 남긴다.
                    // 점을 걷어내면 그만큼 구간이 길어져 서버가 다시 계산할
                    // 속도가 실제와 달라진다.
                    track = RunTrack.encode(session.track),
                    boostBps = boostBps,
                    partySize = settleSize,
                    faction = equippedFaction?.id.orEmpty(),
                    crewId = partyCrewId,
                    mockLocation = session.mockLocation,
                )
            ) { energyDay -> ServiceLocator.rewardRepository.calculateSessionReward(creditedSteps, settleSize, energyDay) }
            // These legacy follow-ups are not durable yet. A same-process save retry
            // must not issue them again after a scheduling failure.
            if (!followupsAttempted) {
                followupsAttempted = true
            if (creditedSteps > 0) {
                val km = if (session.gpsKm > 0.0) session.gpsKm else RewardEconomy.distanceMeters(creditedSteps) / 1000
                runCatching { Analytics.runFinished(km, settleSize) }
            }
            if (verdict.isRewardable) {
                // 코스 완주 정산 — 거리 1km당 정량 SUP. 코스 미선택이면 조용히 지나간다.
                runCatching {
                    val finished = ServiceLocator.courseRepository.grantCompletionIfFinished(
                        RewardEconomy.distanceMeters(creditedSteps) / 1000,
                    )
                    // 완주한 코스는 러닝이 서버에 올라간 뒤 코스 기록으로 낸다
                    // (ServerSessionRecorder). 서버가 경로로 다시 확인한다.
                    if (finished != null) {
                        ServiceLocator.userPrefs.addPendingCourseRun(session.startedAt, finished.encode())
                    }
                }
                // 랭킹 재료 — 최고 속도와, 착용 신발의 종족별 누적 거리.
                // 거리는 GPS 실측이 있으면 그걸 쓰고, 없으면 걸음 환산으로 대체한다.
                runCatching {
                    val prefs = ServiceLocator.userPrefs
                    prefs.recordTopSpeed(session.topSpeedKmh)
                    val km = if (session.gpsKm > 0.0) {
                        session.gpsKm
                    } else {
                        RewardEconomy.distanceMeters(creditedSteps) / 1000
                    }
                    if (km > 0.0 && equippedFaction != null) prefs.addFactionKm(equippedFaction, km)
                }
            }
            }
            // 방금 달린 트랙을 남겨 "코스 만들기"의 재료로 쓴다
            if (session.track.size >= 2) {
                lastTrack.value = session.geoTrack.simplify()
            }
            // 증명 서버로 올릴 차례를 잡아 둔다. 여기서 직접 보내지 않는 것은
            // 러닝이 끝나는 곳에 신호가 있다는 보장이 없기 때문이다 —
            // 일꾼이 연결이 돌아올 때까지 기다렸다 보낸다.
            SessionUploadWorker.schedule(this@WalkSessionService)
            _state.value = WalkSessionState(
                // Keep this session's route available to its result/share card.
                track = session.track,
                lastRewardPoints = reward.points,
                lastRewardedSteps = reward.rewardedSteps,
                lastSessionSteps = session.steps,
                lastPartySize = settleSize,
                lastVerdict = verdict,
                lastTopSpeedKmh = session.topSpeedKmh,
                lastGpsKm = session.gpsKm,
                lastElapsedSec = session.elapsedSec,
                lastStartedAt = session.startedAt,
            )
            // 파티런이었다면 로비를 결과 화면으로 전환하고 방에서 나온다. 혼자 남은
            // 방에서 출발했어도(인원 1) 방은 닫아야 한다 — 안 그러면 로비가 계속
            // 뛰는 중으로 남아 위치를 보낸다.
            if (ServiceLocator.crewRepository.party.value.isActive) {
                ServiceLocator.crewRepository.finishParty(reward.points, reward.rewardedSteps)
            }
            settling = false
            ServiceCompat.stopForeground(this@WalkSessionService, ServiceCompat.STOP_FOREGROUND_REMOVE)
            stopSelf()
          } catch (cancelled: CancellationException) {
            throw cancelled
          } catch (_: Exception) {
            // Preserve the stopped run. A retry uses the same identity and durable receipt.
            // Never resume collecting steps into a run whose settlement may already exist.
            _state.update { if (it.isActive) it.copy(isPaused = true, saveStatus = RunSaveStatus.FAILED) else it }
          } finally {
            settling = false
          }
        }
    }

    override fun onDestroy() {
        stopLocation()
        scope.cancel()
        super.onDestroy()
    }

    private fun createChannel() {
        val manager = getSystemService(NOTIFICATION_SERVICE) as NotificationManager
        val channel = NotificationChannel(
            CHANNEL_ID,
            getString(R.string.notification_channel_walk),
            NotificationManager.IMPORTANCE_LOW,
        )
        manager.createNotificationChannel(channel)
    }

    private fun buildNotification(steps: Int): android.app.Notification {
        val launchIntent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val contentIntent = PendingIntent.getActivity(
            this,
            0,
            launchIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_stat_walk)
            .setContentTitle(getString(R.string.notification_walk_title))
            .setContentText(getString(R.string.notification_steps, steps))
            .setContentIntent(contentIntent)
            .setOngoing(true)
            .setOnlyAlertOnce(true)
            .build()
    }

    private fun updateNotification(steps: Int) {
        val manager = getSystemService(NOTIFICATION_SERVICE) as NotificationManager
        manager.notify(NOTIFICATION_ID, buildNotification(steps))
    }

    companion object {
        private val _state = MutableStateFlow(WalkSessionState())
        val state: StateFlow<WalkSessionState> = _state

        /**
         * 화면 검사 전용 — 러닝 중 · 러닝 완료 화면을 실제 세션 없이 그려 본다.
         *
         * 적립 · 저장 · 업로드는 하나도 일어나지 않는다. 화면이 읽는 상태만 바꾼다.
         * 앱 코드에서는 부르지 않는다.
         */
        @androidx.annotation.VisibleForTesting
        fun showStateForTest(state: WalkSessionState) {
            _state.value = state
        }

        /** 러닝 목표 거리(km). 화면이 아니라 프로세스에 살아서 화면을 오가도 유지된다. */
        val goalKm = MutableStateFlow(5.0)

        /** 마지막 세션의 GPS 트랙 — "코스 만들기"의 재료 */
        val lastTrack = MutableStateFlow<List<GeoPoint>>(emptyList())

        /**
         * 트랙을 다 쓰고 비운다.
         *
         * 코스로 저장했거나 저장하지 않기로 했을 때 부른다. 비우지 않으면
         * 저장 창이 다음에 러닝 화면을 열 때마다 다시 올라온다.
         */
        fun clearLastTrack() {
            lastTrack.value = emptyList()
        }

        /** 수동 랩 — 마지막 랩에서 50m 이상 나아갔을 때만 추가한다. */
        fun recordManualLap() {
            _state.update { current ->
                if (!current.isActive || current.isPaused) return@update current
                val km = RewardEconomy.distanceMeters(current.steps) / 1000
                val lastKm = current.laps.lastOrNull()?.km ?: 0.0
                if (km < lastKm + 0.05) return@update current
                current.copy(
                    laps = current.laps + RunLap(
                        index = current.laps.size + 1,
                        km = km,
                        splitSec = current.elapsedSec,
                    ),
                )
            }
        }

        const val EXTRA_PARTY_SIZE = "com.stepup.android.extra.PARTY_SIZE"

        const val ACTION_START = "com.stepup.android.action.SESSION_START"
        const val ACTION_PAUSE = "com.stepup.android.action.SESSION_PAUSE"
        const val ACTION_RESUME = "com.stepup.android.action.SESSION_RESUME"
        const val ACTION_STOP = "com.stepup.android.action.SESSION_STOP"

        private const val CHANNEL_ID = "walk_session"
        private const val NOTIFICATION_ID = 1001

        /** @param partySize 파티런 인원(본인 포함). 1이면 개인 러닝. */
        fun start(context: Context, partySize: Int = 1) {
            val intent = intent(context, ACTION_START)
                .putExtra(EXTRA_PARTY_SIZE, partySize.coerceAtLeast(1))
            ContextCompat.startForegroundService(context, intent)
        }

        fun pause(context: Context) {
            context.startService(intent(context, ACTION_PAUSE))
        }

        fun resume(context: Context) {
            context.startService(intent(context, ACTION_RESUME))
        }

        fun stop(context: Context) {
            context.startService(intent(context, ACTION_STOP))
        }

        /** 종료 정산 카드 노출 후 초기 상태로 되돌린다. */
        fun clearLastReward() {
            val current = _state.value
            if (!current.isActive && current.lastRewardPoints != null) {
                _state.value = WalkSessionState()
            }
        }

        private fun intent(context: Context, action: String): Intent =
            Intent(context, WalkSessionService::class.java).setAction(action)
    }
}
