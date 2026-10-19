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
import android.os.PowerManager
import android.os.SystemClock
import androidx.core.app.NotificationCompat
import androidx.core.app.ServiceCompat
import androidx.core.content.ContextCompat
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationCallback
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationResult
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import com.stepup.android.MainActivity
import com.stepup.android.R
import com.stepup.android.core.LocationEngine
import com.stepup.android.core.LocationWarmup
import com.stepup.android.core.ServiceLocator
import com.stepup.android.data.local.WalkSessionEntity
import com.stepup.android.domain.Faction
import com.stepup.android.domain.GeoPoint
import com.stepup.android.domain.RewardEconomy
import com.stepup.android.domain.RunDistance
import com.stepup.android.domain.RunIntegrity
import com.stepup.android.domain.RunOverspeed
import com.stepup.android.domain.RunTrack
import com.stepup.android.domain.RunVerdict
import com.stepup.android.domain.RunStill
import com.stepup.android.domain.TrackFixRule
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
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.launch

/** 랩 스냅샷 — km·splitSec은 랩을 찍은 시점의 "누적" 값. 구간값은 UI에서 이전 랩과의 차로 구한다. */
data class RunLap(
    val index: Int,
    val km: Double,
    val splitSec: Long,
)

/**
 * 1km 경계를 넘을 때마다 자동 랩 — 경계 km 값으로 기록하므로 여러 번 재구성돼도, 화면이 없어도 랩은 정확히 km당 하나다.
 * 거리는 화면 · 결과 · 기록과 같은 기준(GPS 100m 부터 GPS, 그 전엔 걸음 — [RunDistance]). 예전엔 걸음 거리로만 찍어
 * 화면이 1.33km 일 때 "1km" 소리가 났다(2026-10-17 점검). 걸음 · GPS 거리 어느 쪽이 늘어도 부른다.
 */
internal fun WalkSessionState.withAutoLaps(): WalkSessionState {
    val km = RunDistance.liveKm(gpsKm, steps)
    var next = laps
    while (next.size < km.toInt()) {
        next = next + RunLap(index = next.size + 1, km = (next.size + 1).toDouble(), splitSec = elapsedSec)
    }
    return if (next === laps) this else copy(laps = next)
}

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

/**
 * 위치 신호 끊김(시안 L04) 판단. GPS 는 6m 넘게 움직여야 새 점을 주므로, 제자리에 서 있는 것을
 * 끊김으로 보지 않도록 "좌표가 30초 넘게 안 오는데 그동안 40걸음 넘게 걸었다"일 때만 끊김으로 본다.
 */
object GpsSignal {
    const val SILENT_MS = 30_000L
    const val MOVING_STEPS = 40

    fun isLost(sinceFixMs: Long, stepsSinceFix: Int): Boolean = sinceFixMs > SILENT_MS && stepsSinceFix >= MOVING_STEPS
}

/** 워킹 세션의 현재 상태. 화면과 서비스가 공유한다. */
enum class RunSaveStatus { IDLE, SAVING, FAILED }

/**
 * 이 러닝을 연 모임 — 화면이 크루 달리기 · 번개 러닝을 가르는 근거(크루 달리기 v2, 2026-10-16).
 * 정산 인원(partySize) · 크루 순위용 크루(partyCrewId)와 따로 둔다 — 혼자 출발해도 크루 달리기지만 모임 보너스 · 순위는 붙지 않는다.
 */
enum class RunGroup { NONE, CREW, FLASH }

/** 대기실에서 출발한 러닝의 모임 — 서비스를 켤 때 넘긴다([WalkSessionService.start]) */
data class StartGroup(val group: RunGroup, val crewId: String, val name: String) {
    companion object {
        val NONE = StartGroup(RunGroup.NONE, "", "")

        /** 저장본에 남길 이름 길이 — 번개 글 제목이 길어도 저장본 칸(writeUTF)을 넘기지 않게 */
        const val NAME_MAX = 80

        /**
         * 출발한 방에서 — 번개 글의 방이면 번개, 크루가 있으면 크루, 둘 다 아니면 모임 없음.
         * [name] 은 대기실이 보인 이름(크루 이름 · 번개 글 제목)이다.
         */
        fun of(room: com.stepup.android.data.repo.PartyState, name: String): StartGroup = when {
            room.flashPostId != null -> StartGroup(RunGroup.FLASH, "", name.trim().take(NAME_MAX))
            !room.crewId.isNullOrBlank() -> StartGroup(RunGroup.CREW, room.crewId, name.trim().take(NAME_MAX))
            else -> NONE
        }
    }
}

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
    /**
     * 파티런이었다면 어느 크루였는지 — 출발할 때 적어 둔다. 앱이 죽었다 되살아나면 파티 로비
     * 상태가 비어 있으므로, 되살린 러닝은 이 값으로 크루 순위에 들어간다.
     */
    val partyCrewId: String = "",
    /**
     * 이 러닝을 연 모임과 그 크루 · 이름(크루 이름 · 번개 글 제목) — 대기실에서 출발할 때 적고 저장본에도 남긴다.
     * 되살린 러닝 · 혼자 남은 러닝 · 끝난 결과도 같은 모임으로 보이고, 크루에 기록 공유는 이 크루로만 간다. 화면 표시용이다.
     */
    val group: RunGroup = RunGroup.NONE,
    val groupCrewId: String = "",
    val groupName: String = "",
    /** 마지막 세션 정산 결과 (종료 직후 화면 표시용) */
    val lastRewardPoints: Double? = null,
    val lastRewardedSteps: Int = 0,
    val lastSessionSteps: Int = 0,
    val lastPartySize: Int = 1,
    /**
     * 방금 끝난 러닝의 모임 — 결과 · 공유 화면용. [lastGroupSize] 는 함께 출발한 인원(나 포함)으로 정산 인원([lastPartySize])과
     * 다를 수 있다. 다른 계정의 러닝을 되살려 저장했으면 크루([lastGroupCrewId])는 비운다(지금 계정으로 그 크루에 공유하지 않게).
     */
    val lastGroup: RunGroup = RunGroup.NONE,
    val lastGroupCrewId: String = "",
    val lastGroupName: String = "",
    val lastGroupSize: Int = 1,
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
    /**
     * 위치 신호가 끊긴 것 같은가(시안 L04) — 한 번 잡혔던 위치가 한동안 안 오는데 걸음은 계속 늘 때.
     * 시간 · 걸음은 계속 기록된다. 화면에 안내만 한다. 저장본에 남기지 않는다.
     */
    val gpsLost: Boolean = false,
    /**
     * 러너가 오래 서 있다([RunStill] — 걸음 · 경로 점이 30초 넘게 그대로) — "30초 넘게 멈춰 있어요"를 알린다(2026-10-17).
     * 일시정지 · 저장 중에는 false. 화면 표시용이라 저장본에 남기지 않는다.
     */
    val still: Boolean = false,
    /**
     * 지금 위치(화면 표시용) — GPS 가 잡히기 전에는 기지국 · 마지막으로 알던 위치로 먼저 채운다. 달리는 중 지도의 내 점 · 화살표는
     * 경로 끝이 아니라 이 자리를 따라간다(경로 점은 오차보다 크게 움직여야 생겨 몇 초 늦다). 경로가 생긴 뒤에는 흐린 위치(오차 50m
     * 초과)가 와도 30초 동안 옮기지 않는다([TrackFixRule.keepHere]). 경로와 거리에는 들어가지 않는다. 저장본에 남기지 않는다.
     */
    val here: GeoPoint? = null,
    /**
     * [here] 를 GPS 점으로 마지막에 고친 때(ms) — 기지국 · 마지막으로 알던 위치는 0 그대로다. 같이 뛰는 사람에게 보내는 위치가
     * 지금 것인지 가린다(경로 점은 8m 움직여야 생겨 신호등에 서 있으면 낡아 보였다). 저장본에 남기지 않는다.
     */
    val hereAt: Long = 0L,
    /**
     * [here] 가 경로에 쓸 만큼 정확한 위치(오차 50m 안)인가 — 흐린 위치(기지국 등)면 false. 지도의 내 점이 경로 끝에서 멀리 떨어진
     * 흐린 위치로 뛰지 않게 하고(RunTrack.liveHead), 같이 뛰는 사람에게 흐린 위치를 지금 자리처럼 보내지 않는다. 저장본에 남기지 않는다.
     */
    val hereAccurate: Boolean = false,
    /** 마지막으로 걸음이 는 때(폰 시계 ms) — 경로 점이 드문 동안에도 걷는 중인지(지금 속도). 저장본에 남기지 않는다 */
    val movingAt: Long = 0L,
    /** 정확한 위치(ACCESS_FINE_LOCATION)를 허용했는지 — 대략적인 위치만이면 GPS 를 못 쓴다 */
    val precise: Boolean = true,
    /** 휴대폰의 위치 기능 자체가 켜져 있는지 — 꺼져 있으면 권한이 있어도 위치가 오지 않는다 */
    val locationOn: Boolean = true,
    /** GPS로 잰 유효 거리(km). 속도 상한을 넘긴 구간은 빠져 있다. */
    val gpsKm: Double = 0.0,
    /** 이번 세션의 최고 속도(km/h) — 사람 범위 안의 값만 */
    val topSpeedKmh: Double = 0.0,
    /** 사람 속도로 인정된 GPS 구간 수 */
    val validSegments: Int = 0,
    /** 속도 상한을 넘겨 버려진 구간 수 */
    val flaggedSegments: Int = 0,
    /**
     * 마지막으로 구간이 버려진 때(ms) — 러닝 중 "현재 속도"를 모른다고 보이는 근거(화면 표시용).
     * 그 뒤로 사람 속도의 점이 들어오면 다시 속도를 보인다. 저장본에 남기지 않는다.
     */
    val lastFlaggedAt: Long = 0L,
    /**
     * 지금 이어지는 과속([RunOverspeed]) — 사람 한계를 넘는 구간이 끊기지 않고 들어온 첫 때와 마지막 때(벽시계 ms). 없으면 null.
     * 화면이 "N초 뒤 러닝이 멈춰요"를 세는 근거다. 30초를 채우면 서비스가 러닝을 멈춘다. 저장본에 남기지 않는다.
     */
    val overspeed: RunOverspeed.Streak? = null,
    /** 이 러닝이 과속으로 멈추는 중이다(패널티를 적었다) — 저장이 끝나면 [lastOverspeedStopped] 로 넘어간다 */
    val overspeedStopped: Boolean = false,
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
    /**
     * 이번 러닝에 테스트 도구의 "걸음 흉내"를 썼다(테스트 빌드만). 실제 걸음이 아니므로 적립하지 않고 서버에도
     * 올리지 않는다 — 테스트 APK 가 실제 서버에 흉내 걸음으로 SUP 를 쌓던 것을 막는다. 저장본에도 남긴다(판 4) —
     * 남기지 않으면 앱을 죽였다 되살린 러닝에서 표시가 사라져 흉내 걸음이 그대로 올라갔다.
     */
    val simulated: Boolean = false,
    /** 방금 끝난 러닝이 테스트 걸음 때문에 적립 · 업로드에서 빠졌다 — 결과 화면 안내용 */
    val lastSimulated: Boolean = false,
    /** 방금 끝난 러닝을 과속이 30초 이어져 앱이 멈췄다 — 결과 화면이 패널티(내구도 절반 · 10분 러닝 금지)를 알린다 */
    val lastOverspeedStopped: Boolean = false,
) {
    /** 지도에 그리거나 코스로 저장할 때 쓰는 모양만 남긴 경로 */
    val geoTrack: List<GeoPoint> get() = track.toGeoPoints()

    /** 러닝 중 실시간 판정 — 화면에 경고 배지를 띄우는 근거 */
    val liveVerdict: RunVerdict
        get() = if (mockLocation || simulated) {
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
    /** 마지막으로 좌표를 받은 때(부팅 기준 ms)와 그때의 걸음 — 신호 끊김(L04) 판단용 */
    @Volatile private var lastFixElapsed = 0L
    @Volatile private var stepsAtLastFix = 0
    @Volatile private var lastKnownFixElapsed = 0L

    /** 마지막으로 움직인 때(부팅 기준 ms) — 걸음이 늘거나 경로 점이 생긴 때, 시작 · 다시 시작한 때. 오래 서 있음([RunStill]) 판단용 */
    @Volatile private var lastMoveElapsed = 0L

    /** GPS 가 마지막으로 고친 위치의 시각(부팅 기준 ms) — 알림 거리 필터와 상관없이 갱신된다. 없으면 0 */
    private fun lastKnownGpsFixElapsed(): Long = runCatching {
        val lm = locationManager ?: return@runCatching 0L
        lm.getLastKnownLocation(LocationManager.GPS_PROVIDER)?.elapsedRealtimeNanos?.div(1_000_000) ?: 0L
    }.getOrDefault(0L)
    private var stepJob: Job? = null
    private var timerJob: Job? = null
    private var locationManager: LocationManager? = null

    /** 전경 서비스에 위치 종류(location)를 선언했는가 — 시작할 때 위치 권한이 없었으면 false, 허용하면 그때 더한다 */
    @Volatile private var foregroundHasLocation = false

    /**
     * 위치 하나를 러닝에 넣는다 — 구글 위치 서비스(구글 지도와 같은 엔진: GPS · 와이파이 · 기지국) · 위성 GPS · 와이파이 · 기지국
     * 어디서 왔든 여기로 온다(메인 루퍼). 8m 이상 움직였을 때만 경로에 점을 더해 제자리 노이즈로 트랙이 지저분해지는 것을 막는다.
     *
     * 위성 GPS(LocationManager 의 GPS)는 예전 그대로 받는다. 그 밖의 위치는 오차가 클 수 있어 [TrackFixRule] 로 거른다 —
     * 오차가 50m 보다 크면 지도의 지금 자리만 옮기고, 기준점에서 오차(둘 중 큰 쪽)의 두 배보다 멀리 움직였을 때만 경로 · 거리에
     * 넣는다. 실내에서 제자리에 있어도 흔들리는 와이파이 위치가 거리로 쌓이지 않게(2026-10-16 사용자 결정 "그래 다 되도록 만들어").
     * 훨씬 정확한 위치가 기준점의 오차 안에 오면(와이파이 → GPS) 기준점만 옮긴다 — 흐린 출발점에 묶여 거리가 멈춰 있지 않게
     * (2026-10-16 "인식도 잘 안 되고 느려"). 지도의 내 점은 경로 끝이 아니라 받은 위치([WalkSessionState.here])를 따라간다.
     */
    private fun onFix(location: Location) {
        if (isMockLocation(location)) {
            // 가짜 GPS 앱이 넣은 좌표 — 경로에도 거리에도 넣지 않고, 이번 러닝을 표시만 한다
            _state.update { current ->
                if (!current.isActive || current.isPaused) current else current.copy(mockLocation = true)
            }
            return
        }
        val p = GeoPoint(location.latitude, location.longitude)
        val now = System.currentTimeMillis()
        val satellite = location.provider == LocationManager.GPS_PROVIDER
        val accuracy = if (location.hasAccuracy()) location.accuracy else null
        // 위성 GPS 가 잡혔으면 와이파이 · 기지국 위치는 그만 받는다 — 둘을 섞으면 점이 튄다. 와이파이 · 기지국 위치로 잡은 기준점에서
        // 첫 GPS 점까지를 한 구간으로 재지 않는다(그 오차만큼 거저 생긴 거리 · 튄 구간, 2026-10-17 점검)
        if (satellite && networkRegistered) {
            locationManager?.removeUpdates(networkListener)
            networkRegistered = false
            resetAnchor()
        }
        // 위치 신호가 끊겼다 돌아온 첫 점(시안 L04)은 새 구간의 시작이다 — 끊기기 전 마지막 점과 이어
        // 그 사이를 직선 거리로 세지 않는다(지도에서도 그 사이는 잇지 않는다, RunTrack.segmentBreaks)
        if (_state.value.gpsLost) resetAnchor()
        // 흐린 위치(오차 50m 초과 — 기지국 · 아주 흐린 와이파이 · 인터넷 없이 막 잡은 GPS)는 위성 GPS 든 아니든 경로 · 거리에 넣지
        // 않는다. 예전엔 위성 GPS 는 오차를 보지 않아 150m 짜리 점이 기준점이 되면 다음 구간 문턱이 300m 로 뛰었다
        val fix = when {
            !satellite -> TrackFixRule.decide(accuracy, speedAnchor?.let { haversineMeters(it, p) }, speedAnchorAccuracy)
            (accuracy ?: 0f) > TrackFixRule.MAX_ACCURACY_M -> TrackFixRule.Fix.MAP_ONLY
            else -> TrackFixRule.Fix.COMMIT
        }
        val good = fix != TrackFixRule.Fix.MAP_ONLY
        // 흐린 위치는 "잡힌 위치"로 치지 않는다 — 흐린 위치만 오는 동안 거리가 멈추면 "신호 약함"이 뜨게(GpsSignal). 예전엔 흐린
        // 위치도 잡힌 것으로 쳐, 인터넷이 끊긴 뒤 기지국 위치만 오는 동안 거리가 멈춰도 아무 안내가 없었다(2026-10-17 점검)
        if (good) {
            lastFixElapsed = SystemClock.elapsedRealtime()
            stepsAtLastFix = _state.value.steps
        }
        if (fix != TrackFixRule.Fix.COMMIT) {
            // 같은 자리를 더 정확히 알았다(와이파이 → GPS) — 기준점만 이 위치로. 거리 · 경로 점은 더하지 않는다
            if (fix == TrackFixRule.Fix.REFINE) {
                speedAnchor = p
                speedAnchorAt = now
                speedAnchorAccuracy = accuracy
            }
            // 경로가 생긴 뒤의 흐린 위치는 좋은 위치를 받은 지 30초 안이면 지금 자리를 옮기지 않는다 — 지도의 내 점 · 크루에게 보내는
            // 위치가 튀지 않게. "좋은 위치를 받은 때"로 잰다 — 흐린 위치로 지금 자리를 옮긴 때로 재면 30초마다만 움직였다
            if (!good && TrackFixRule.keepHere(_state.value.track.isNotEmpty(), now - goodHereAt)) return
            if (good) goodHereAt = now
            // 지도의 지금 자리만 — 경로 · 거리 · 속도 판정에는 넣지 않는다
            _state.update { current ->
                if (!current.isActive) return@update current
                // 멈춘 동안은 지도의 내 점만 따라간다 — 경로 · 거리 · 잡힘 표시는 그대로(다시 달리면 그 자리부터)
                if (current.isPaused) return@update current.copy(here = p, hereAt = now, hereAccurate = good)
                // 출발점이 흐린 와이파이 위치였으면 더 정확한 자리로 옮긴다 — 아직 점이 하나뿐일 때만(거리는 그대로 0)
                val track = if (fix == TrackFixRule.Fix.REFINE && current.track.size == 1) {
                    listOf(current.track[0].copy(lat = p.lat, lng = p.lng))
                } else {
                    current.track
                }
                current.copy(
                    track = track,
                    gpsFix = current.gpsFix || good,
                    gpsLost = current.gpsLost && !good,
                    here = p,
                    hereAt = now,
                    hereAccurate = good,
                )
            }
            return
        }
        // 속도 판정 기준점은 트랙과 따로 든다. 튄 구간의 점은 트랙에 넣지
        // 않지만 기준점은 옮겨야, 다음 구간이 연쇄로 튀지 않는다.
        val prev = speedAnchor
        val prevAt = speedAnchorAt
        val prevAccuracy = speedAnchorAccuracy
        speedAnchor = p
        speedAnchorAt = now
        speedAnchorAccuracy = accuracy

        val meters = if (prev == null) 0.0 else haversineMeters(prev, p)
        // 어테스터(economy.js)와 같이 반올림한다. 버리면 2.5초 간격이 2초가 되어
        // 서버보다 25% 엄격하게 튄 구간으로 잡힌다.
        val seconds = if (prevAt == 0L) 0L else (now - prevAt + 500) / 1000
        val plausible = prev == null || RunIntegrity.isPlausible(meters, seconds)

        // 걸음 수집기·타이머와 서로 덮어쓰지 않게 원자적으로 갱신한다
        val trackBefore = _state.value.track.size
        if (plausible) goodHereAt = now
        _state.update { current ->
            if (!current.isActive) return@update current
            // 멈춘 동안은 지도의 내 점만 따라간다(기준점은 위에서 옮겼다 — 멈춘 동안 움직인 거리는 다시 달려도 세지 않는다)
            if (current.isPaused) return@update if (plausible) current.copy(here = p, hereAt = now, hereAccurate = true) else current
            if (!plausible) {
                // 사람이 낼 수 없는 속도 — 거리도, 경로도 남기지 않는다. 지도의 내 점 · 크루에게 보내는 위치도 그 튄 자리로 옮기지 않는다.
                // 경고를 띄우고, 30초 이어지면 러닝을 멈춘다(RunOverspeed)
                return@update current.copy(
                    gpsFix = true,
                    gpsLost = false,
                    flaggedSegments = current.flaggedSegments + 1,
                    lastFlaggedAt = now,
                    // 흐린 위치(와이파이 · 기지국)의 튐은 과속으로 세지 않는다 — 실내에서 두 자리를 오가 서 있는 러너가 멈추고
                    // 패널티를 받을 수 있었다(2026-10-19 점검). 거리 · 경로에는 여전히 넣지 않는다
                    overspeed = if (RunOverspeed.counts(accuracy, prevAccuracy)) {
                        RunOverspeed.onFlagged(current.overspeed, now, prev, p)
                    } else {
                        current.overspeed
                    },
                )
            }
            val track = current.track
            val moved = track.isEmpty() ||
                haversineMeters(track.last().toGeoPoint(), p) >= TrackFixRule.MIN_STEP_M
            val counted = prev != null && meters >= RunIntegrity.MIN_SEGMENT_METERS &&
                seconds >= RunIntegrity.MIN_SEGMENT_SEC
            // 폰 시계를 뒤로 돌려도 점의 시각이 거꾸로 가지 않게 — 저장본 검사가 시작 전 · 거꾸로 간
            // 시각을 거절해 그 뒤 저장이 모두 막힌다
            val at = maxOf(now, current.startedAt, track.lastOrNull()?.at ?: 0L)
            current.copy(
                track = if (moved) track + TrackPoint(p.lat, p.lng, at) else track,
                gpsFix = true,
                gpsLost = false,
                here = p,
                hereAt = now,
                hereAccurate = true,
                gpsKm = if (counted) current.gpsKm + meters / 1000 else current.gpsKm,
                validSegments = if (counted) current.validSegments + 1 else current.validSegments,
                topSpeedKmh = RunIntegrity.updateTopSpeed(current.topSpeedKmh, meters, seconds),
                // 사람 속도로 움직인 구간 — 속도를 줄였다, 과속 경고를 거둔다. 제자리 노이즈(5m 아래)는 그대로 둔다
                overspeed = if (counted) null else current.overspeed,
            ).withAutoLaps()
        }
        // 경로 점이 생겼다 — 움직이고 있다(오래 서 있음 판단을 처음부터)
        if (_state.value.track.size > trackBefore) lastMoveElapsed = SystemClock.elapsedRealtime()
        // 과속이 30초 이어졌다 — 러닝을 멈추고 패널티(2026-10-17 사용자 결정)
        if (!plausible && RunOverspeed.shouldStop(_state.value.overspeed)) stopForOverspeed(now)
    }

    /** 과속으로 멈추는 중 — 위치가 계속 와도 한 번만 */
    @Volatile private var overspeedStopping = false

    /**
     * 과속이 30초 이어져 러닝을 멈춘다(2026-10-17 사용자 결정). 패널티(신발 남은 내구도 절반 · 멈춘 때부터 10분 러닝 금지)를
     * 먼저 폰에 적고 나서 평소처럼 저장한다 — 저장이 실패하거나 앱이 죽어도 금지 시간과 서버에 보낼 신고는 남는다.
     * 서버 신고는 올리기 일꾼이 러닝보다 먼저 보낸다(OverspeedReporter). 무효로 끝나 서버에 올라가지 않는 러닝도 신고는 간다.
     */
    private fun stopForOverspeed(stoppedAt: Long) {
        val session = _state.value
        if (overspeedStopping || !session.isActive || session.isPaused || session.saveStatus != RunSaveStatus.IDLE) return
        overspeedStopping = true
        _state.update { if (it.isActive) it.copy(overspeedStopped = true) else it }
        scope.launch(Dispatchers.Main.immediate) {
            try {
                runCatching {
                    val prefs = ServiceLocator.userPrefs
                    prefs.raiseRunBanUntil(stoppedAt + RunOverspeed.BAN_MS)
                    prefs.addPendingOverspeedStop(session.startedAt, stoppedAt, session.recordingOwner)
                }
                stopSession()
            } finally {
                overspeedStopping = false
            }
        }
    }

    /** 위성 GPS(LocationManager) — 구글 Play 서비스가 없는 휴대폰에서만 쓴다 */
    private val locationListener = object : LocationListener {
        override fun onLocationChanged(location: Location) = onFix(location)

        // API 29 이하에서는 아래 셋이 추상 메서드라 반드시 구현해야 한다
        @Deprecated("Deprecated in Java")
        override fun onStatusChanged(provider: String?, status: Int, extras: android.os.Bundle?) = Unit
        override fun onProviderEnabled(provider: String) {
            refreshLocationOn()
            // 러닝 중에 GPS 를 켰다 — 다시 등록해 GPS 를 먼저 받는다(대신 쓰던 와이파이 · 기지국 위치는 그만 받는다)
            if (provider == LocationManager.GPS_PROVIDER && locationManager != null && fusedClient == null) {
                stopLocation()
                // 와이파이 · 기지국 위치의 마지막 점과 첫 GPS 점을 한 구간으로 재지 않는다(튄 구간 · 거저 생긴 거리)
                speedAnchor = null
                speedAnchorAt = 0L
                speedAnchorAccuracy = null
                startLocation()
            }
        }
        override fun onProviderDisabled(provider: String) = refreshLocationOn()
    }

    /**
     * 와이파이 · 기지국 위치(LocationManager) — 구글 Play 서비스가 없는 휴대폰에서, GPS 가 잡히기 전(실내 · 빌딩 사이)이거나
     * GPS 가 없을 때. [onFix] 가 오차를 보고 거른다. 첫 위성 GPS 점이 오면 등록을 푼다.
     */
    private var networkRegistered = false
    private val networkListener = object : LocationListener {
        override fun onLocationChanged(location: Location) = onFix(location)

        @Deprecated("Deprecated in Java")
        override fun onStatusChanged(provider: String?, status: Int, extras: android.os.Bundle?) = Unit
        override fun onProviderEnabled(provider: String) = Unit
        override fun onProviderDisabled(provider: String) = Unit
    }

    /** 구글 위치 서비스(구글 지도와 같은 엔진). 쓰는 동안만 값이 있다 — Play 서비스가 없으면 null 이고 LocationManager 로 받는다 */
    private var fusedClient: FusedLocationProviderClient? = null

    /**
     * 구글 위치 서비스를 켠(다시 건) 때와 마지막으로 위치를 준 때(부팅 기준 ms, 없으면 0) — 오래 아무것도 안 주면 휴대폰 위치도
     * 듣고, 더 오래면 요청을 다시 건다. 처음만 보던 예전에는 러닝 중간에 조용해지면(인터넷이 끊겼다 돌아온 뒤 등) 러닝이 끝날
     * 때까지 위치가 멈춰 있었다(2026-10-17 "다시 연결했을 때 내 위치를 못 잡는다. 아무리 움직여도 그냥 그 자리다").
     */
    private var fusedSince = 0L
    private var fusedLastAt = 0L

    /** 구글 위치 서비스가 조용해 휴대폰 위치(GPS · 와이파이 · 기지국)도 함께 듣는 중 */
    private var backupFeed = false
    private val fusedCallback = object : LocationCallback() {
        override fun onLocationResult(result: LocationResult) {
            fusedLastAt = SystemClock.elapsedRealtime()
            // 구글 위치 서비스가 다시 위치를 준다 — 함께 듣던 휴대폰 위치는 그만(둘을 섞으면 점이 튄다)
            if (backupFeed) stopBackupFeed()
            result.locations.forEach(::onFix)
        }
    }

    private fun refreshLocationOn() {
        val lm = locationManager ?: return
        val on = runCatching { androidx.core.location.LocationManagerCompat.isLocationEnabled(lm) }.getOrDefault(true)
        _state.update { if (it.locationOn == on) it else it.copy(locationOn = on) }
    }

    /** 2분 안에 기기가 알던 가장 새 위치 — 러닝 시작 직후 지도에 먼저 보여 준다 */
    private fun freshLastKnown(lm: LocationManager): GeoPoint? = runCatching {
        listOf(LocationManager.GPS_PROVIDER, LocationManager.NETWORK_PROVIDER, LocationManager.PASSIVE_PROVIDER)
            .mapNotNull { provider ->
                try { lm.getLastKnownLocation(provider) } catch (_: SecurityException) { null } catch (_: IllegalArgumentException) { null }
            }
            .filter { !isMockLocation(it) && System.currentTimeMillis() - it.time <= 2 * 60_000L }
            .maxByOrNull { it.time }
            ?.let { GeoPoint(it.latitude, it.longitude) }
    }.getOrNull()

    private fun hasLocationPermission(): Boolean =
        ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) ==
            PackageManager.PERMISSION_GRANTED ||
            ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) ==
            PackageManager.PERMISSION_GRANTED

    private fun hasFineLocation(): Boolean =
        ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED

    /**
     * 위치를 듣는 동안 CPU 를 깨워 둔다 — 화면이 꺼지면 1초 타이머(신호 약함 · 휴대폰 위치로 바꾸기 · 요청 다시 걸기 · 저장본)가
     * 폰이 잠든 동안 멈춰, 구글 위치 서비스가 조용해진 것을 화면을 켤 때까지 몰랐다 — 그동안 달린 거리가 통째로 빠졌다(2026-10-17 점검).
     */
    private var wakeLock: PowerManager.WakeLock? = null

    private fun holdCpu() {
        if (wakeLock?.isHeld == true) return
        wakeLock = runCatching {
            (getSystemService(POWER_SERVICE) as PowerManager)
                .newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "StepUp:run")
                .apply {
                    setReferenceCounted(false)
                    acquire(WAKE_LOCK_MAX_MS)
                }
        }.getOrNull()
    }

    private fun releaseCpu() {
        wakeLock?.let { lock -> runCatching { if (lock.isHeld) lock.release() } }
        wakeLock = null
    }

    private fun startLocation() {
        if (!hasLocationPermission()) return
        holdCpu()
        val lm = getSystemService(LOCATION_SERVICE) as LocationManager
        locationManager = lm
        val fine = hasFineLocation()
        // 구글 지도와 같은 위치 엔진을 먼저 쓴다 — 실내에서는 와이파이로, 밖에서는 GPS 로 잡힌다. 없으면 휴대폰 위치로
        if (!startFused()) startLocationManagerFeed(lm, fine)
        // 러닝 탭에서 미리 잡아 둔 위치(LocationWarmup) — 10초 안 · 오차 50m 안이면 GPS 가 이미 잡힌 것으로 보고 내 점을 바로 놓는다
        // (2026-10-17 "시작하고도 10초를 찾는다"). 경로 · 거리 · 속도 기준점에는 넣지 않는다 — 러닝 전 위치다
        val warm = LocationWarmup.fresh(TrackFixRule.WARM_MAX_AGE_MS)?.takeIf {
            TrackFixRule.warmFix(if (it.hasAccuracy()) it.accuracy else null, LocationWarmup.ageMs(it))
        }?.takeIf { !_state.value.gpsFix }
        if (warm != null) {
            lastFixElapsed = warm.elapsedRealtimeNanos / 1_000_000
            stepsAtLastFix = _state.value.steps
        }
        // 그만큼 좋지 않아도 미리 잡은 위치(2분 안)가 있으면 내 점부터 놓는다 — 지도 앱처럼 마지막 위치를 바로 보인다
        val seed = if (_state.value.here == null && warm == null) {
            LocationWarmup.fresh(2 * 60_000L)?.let { GeoPoint(it.latitude, it.longitude) } ?: freshLastKnown(lm)
        } else {
            null
        }
        val on = runCatching { androidx.core.location.LocationManagerCompat.isLocationEnabled(lm) }.getOrDefault(true)
        _state.update { current ->
            when {
                !current.isActive -> current
                warm != null && !current.gpsFix -> current.copy(
                    precise = fine, locationOn = on, gpsFix = true,
                    here = GeoPoint(warm.latitude, warm.longitude),
                    hereAt = System.currentTimeMillis() - LocationWarmup.ageMs(warm),
                    hereAccurate = true,
                )
                else -> current.copy(precise = fine, locationOn = on, here = current.here ?: seed)
            }
        }
    }

    /** 구글 위치 서비스로 받기 시작한다. 쓸 수 없으면 false — 휴대폰 위치(LocationManager)로 받는다 */
    private fun startFused(): Boolean {
        if (!LocationEngine.fusedAvailable(this)) return false
        val client = LocationServices.getFusedLocationProviderClient(this)
        return try {
            requestFused(client)
            fusedClient = client
            fusedSince = SystemClock.elapsedRealtime()
            fusedLastAt = 0L
            // 구글 위치 서비스가 아는 마지막 위치 — 첫 위치가 오기 전에 지도를 내 자리에 맞춘다(경로 · 거리에는 넣지 않는다)
            if (_state.value.here == null) {
                client.lastLocation.addOnSuccessListener { last ->
                    if (last == null || isMockLocation(last) || System.currentTimeMillis() - last.time > 2 * 60_000L) return@addOnSuccessListener
                    val p = GeoPoint(last.latitude, last.longitude)
                    _state.update { if (!it.isActive || it.here != null) it else it.copy(here = p) }
                }
            }
            true
        } catch (_: SecurityException) {
            false
        }
    }

    /** 구글 위치 서비스에 위치를 달라고 건다. 권한이 없으면 SecurityException */
    private fun requestFused(client: FusedLocationProviderClient) {
        // 2.5초마다(예전 GPS 와 같다), 거리 거름 없이 — 제자리에서도 위치가 와야 "신호 약함"으로 잘못 보지 않는다.
        // 첫 위치는 정확한 것이 올 때까지 미루지 않는다 — 와이파이 위치라도 먼저 받고, 거리는 onFix 가 거른다
        val request = LocationRequest.Builder(Priority.PRIORITY_HIGH_ACCURACY, 2_500L)
            .setMinUpdateIntervalMillis(1_000L)
            .setWaitForAccurateLocation(false)
            .build()
        client.requestLocationUpdates(request, fusedCallback, mainLooper).addOnFailureListener {
            // Play 서비스가 받지 않았다 — 그사이 멈추지 않았으면 휴대폰 위치로 바꿔 받는다
            if (fusedClient === client) {
                fusedClient = null
                if (!backupFeed) locationManager?.let { lm -> startLocationManagerFeed(lm, hasFineLocation()) }
                backupFeed = false
            }
        }
    }

    /**
     * 구글 위치 서비스가 [FUSED_SILENT_MS] 넘게 아무 위치도 주지 않는다(켠 뒤 처음이든, 러닝 중간이든) — 휴대폰 위치(GPS · 와이파이 ·
     * 기지국)도 함께 듣는다. [FUSED_RENEW_MS] 넘게 조용하면 요청도 다시 건다 — 인터넷이 끊겼다 돌아온 뒤 등 멈춘 채 깨어나지 않는
     * 일이 있다. 구글 위치 서비스가 다시 위치를 주면 휴대폰 위치는 그만 듣는다. 메인 스레드에서 5초마다 부른다.
     */
    private fun startBackupFeedIfSilent() {
        val lm = locationManager ?: return
        val client = fusedClient ?: return
        if (fusedSince == 0L) return
        val now = SystemClock.elapsedRealtime()
        val silentMs = now - maxOf(fusedSince, fusedLastAt)
        if (!backupFeed && silentMs >= FUSED_SILENT_MS) {
            backupFeed = true
            // 와이파이 · 기지국 위치도 함께 — 이미 잡힌 러닝이라도(실내에서 조용해지면 GPS 만으로는 아무것도 오지 않는다)
            startLocationManagerFeed(lm, hasFineLocation(), withNetwork = true)
        }
        if (silentMs >= FUSED_RENEW_MS) {
            runCatching { client.removeLocationUpdates(fusedCallback) }
            if (runCatching { requestFused(client) }.isSuccess) fusedSince = now
        }
    }

    private fun stopBackupFeed() {
        backupFeed = false
        locationManager?.removeUpdates(locationListener)
        if (networkRegistered) locationManager?.removeUpdates(networkListener)
        networkRegistered = false
    }

    /**
     * 휴대폰 위치(LocationManager) — 위성 GPS 와, GPS 가 잡히기 전 · GPS 가 없을 때의 와이파이 · 기지국 위치.
     * [withNetwork] 면 이미 잡힌 러닝이라도 와이파이 · 기지국 위치를 함께 듣는다(첫 위성 GPS 점이 오면 그만 듣는다).
     */
    private fun startLocationManagerFeed(lm: LocationManager, fine: Boolean, withNetwork: Boolean = false) {
        // 둘을 따로 등록한다 — 대략 위치 권한만 있으면(API 30 이하) GPS 등록이 거절되는데, 그 때문에
        // 기지국 위치까지 못 받으면 안 된다. 권한이 그 사이 회수됐다면 위치 없이 진행한다.
        var gps = false
        // GPS 는 꺼져 있어도 등록해 둔다 — 러닝 중에 위치를 켜면 그때부터 점이 들어온다
        // (예전엔 시작할 때 꺼져 있으면 러닝 내내 경로가 비었다)
        if (fine && LocationManager.GPS_PROVIDER in lm.allProviders) {
            gps = runCatching {
                lm.requestLocationUpdates(LocationManager.GPS_PROVIDER, 2_500L, 6f, locationListener, mainLooper)
            }.isSuccess
        }
        val gpsOn = gps && runCatching { lm.isProviderEnabled(LocationManager.GPS_PROVIDER) }.getOrDefault(false)
        val networkOn = runCatching { lm.isProviderEnabled(LocationManager.NETWORK_PROVIDER) }.getOrDefault(false)
        // GPS 첫 위치는 실내 · 빌딩 사이에서 몇 분 걸리기도 한다 — 그동안(또는 GPS 가 없으면 내내) 와이파이 · 기지국 위치로
        if (networkOn && (withNetwork || !gpsOn || !_state.value.gpsFix)) {
            networkRegistered = runCatching {
                lm.requestLocationUpdates(LocationManager.NETWORK_PROVIDER, 3_000L, 0f, networkListener, mainLooper)
            }.isSuccess
        }
    }

    private fun stopLocation() {
        releaseCpu()
        fusedClient?.let { client -> runCatching { client.removeLocationUpdates(fusedCallback) } }
        fusedClient = null
        fusedSince = 0L
        fusedLastAt = 0L
        backupFeed = false
        locationManager?.removeUpdates(locationListener)
        if (networkRegistered) locationManager?.removeUpdates(networkListener)
        networkRegistered = false
        locationManager = null
    }

    /** 속도 판정용 직전 좌표 · 시각 · 그 위치의 오차(m). 위치 콜백(메인 루퍼)에서만 만진다. */
    private var speedAnchor: GeoPoint? = null
    private var speedAnchorAt: Long = 0L
    private var speedAnchorAccuracy: Float? = null

    /** 좋은 위치(오차 50m 안 · 사람 속도)로 지금 자리를 마지막에 옮긴 때(폰 시계 ms, 없으면 0) — 흐린 위치를 30초 동안 버티는 기준(keepHere) */
    private var goodHereAt: Long = 0L

    /** 다음 위치부터 새 구간으로 잰다 — 끊긴 사이 · 다른 위치 공급원에서 넘어온 사이를 한 구간으로 재지 않게 */
    private fun resetAnchor() {
        speedAnchor = null
        speedAnchorAt = 0L
        speedAnchorAccuracy = null
    }

    /** 세션 걸음 집계 기준점. 첫 실측값 방출로 초기화된다(null = 아직 미정). */
    private var lastTodaySteps: Int? = null
    @Volatile private var settling = false
    private var startJob: Job? = null
    private var followupsAttempted = false

    /** 앞선 실행이 저장하다 멈춘(SETTLING) 러닝을 되살려 끝내는 중인가 */
    private var resumedSettling = false
    private var checkpointJob: Job? = null

    /**
     * 달린 시간(멈춘 시간 제외, ms). 1초 타이머의 횟수를 세면 화면이 꺼진 동안 폰이 잠들 때
     * 덜 세져 정상 러닝이 "케이던스 초과"로 무효가 된다 — 폰의 누적 시계로 잰다.
     */
    @Volatile private var activeMs = 0L

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_START -> startSession(
                intent.getIntExtra(EXTRA_PARTY_SIZE, 1).coerceAtLeast(1),
                StartGroup(
                    runCatching { RunGroup.valueOf(intent.getStringExtra(EXTRA_GROUP).orEmpty()) }.getOrDefault(RunGroup.NONE),
                    intent.getStringExtra(EXTRA_GROUP_CREW).orEmpty(),
                    intent.getStringExtra(EXTRA_GROUP_NAME).orEmpty(),
                ),
            )
            ACTION_PAUSE -> setPaused(true)
            ACTION_RESUME -> setPaused(false)
            ACTION_STOP -> stopSession()
            ACTION_DISCARD -> discardSession()
            ACTION_RECOVER -> recoverSession(intent.getBooleanExtra(EXTRA_FINISH, false))
        }
        return START_NOT_STICKY
    }

    private fun startSession(partySize: Int, group: StartGroup) {
        if (_state.value.isActive || startJob?.isActive == true) return
        enterForeground()
        startJob = scope.launch(Dispatchers.Main.immediate) {
            // 끝나지 않은 러닝(앱이 죽어 남은 저장본)이 있으면 새 러닝을 시작하지 않고 먼저 묻는다.
            // 그대로 시작하면 새 러닝은 저장본을 남기지 못해(다른 러닝의 저장본이 자리를 차지) 보호받지 못한다.
            val store = ServiceLocator.runCheckpoints
            val pending = runCatching { store.read() }
                .getOrElse {
                    // 읽을 수 없는 저장본은 지우지 않고 옆으로 치워 둔다 — 새 러닝의 저장을 막지 않게.
                    // 잠깐 못 읽었을 뿐이면(치울 것이 없다) 다시 읽어 그 러닝부터 묻는다. 치우지도 다시
                    // 읽지도 못하면 새 러닝을 시작하지 않는다 — 시작해도 저장본을 남기지 못해 끝낼 때 막힌다.
                    val setAside = runCatching { store.setAsideUnreadable() }.getOrDefault(false)
                    if (setAside) null else runCatching { store.read() }.getOrElse {
                        // 눌러도 아무 일이 없는 것처럼 보이지 않게 이유를 알린다
                        runCatching {
                            android.widget.Toast.makeText(
                                this@WalkSessionService, R.string.run_storage_blocked, android.widget.Toast.LENGTH_LONG,
                            ).show()
                        }
                        startRefusals.value += 1
                        ServiceCompat.stopForeground(this@WalkSessionService, ServiceCompat.STOP_FOREGROUND_REMOVE)
                        stopSelf()
                        return@launch
                    }
                }
            if (pending != null) {
                recovery.value = pending
                startRefusals.value += 1
                ServiceCompat.stopForeground(this@WalkSessionService, ServiceCompat.STOP_FOREGROUND_REMOVE)
                stopSelf()
                return@launch
            }
            // 과속으로 멈춘 뒤 10분은 러닝 금지(2026-10-17 사용자 결정) — 모든 시작(자유 · 챌린지 · 코스 · 크루 · 번개)이 여기를 지난다.
            // 서버도 그 사이에 시작한 러닝은 무효로 둔다(0065)
            val banUntil = runCatching { ServiceLocator.userPrefs.runBanUntil.first() }.getOrDefault(0L)
            val banLeft = RunOverspeed.banLeftMinutes(banUntil, System.currentTimeMillis())
            if (banLeft > 0) {
                runCatching {
                    android.widget.Toast.makeText(
                        this@WalkSessionService,
                        resources.getQuantityString(R.plurals.run_overspeed_ban, banLeft, banLeft),
                        android.widget.Toast.LENGTH_LONG,
                    ).show()
                }
                startRefusals.value += 1
                ServiceCompat.stopForeground(this@WalkSessionService, ServiceCompat.STOP_FOREGROUND_REMOVE)
                stopSelf()
                return@launch
            }
            // Begin tracking only after the recording account has been captured.
            // Finishing later must never read the replacement login account.
            val owner = ServiceLocator.sessionHolder.recordingOwner()
            beginTracking(partySize, owner, group = group)
        }
    }

    /**
     * 앱이 죽어 멈춘 러닝을 저장본에서 되살린다. 멈춰 있던 동안의 시간 · 걸음 · 거리는 더하지 않는다
     * (되살린 러닝은 일시정지 상태로 시작한다). [finish] 면 되살린 뒤 바로 끝내고 저장한다.
     * 저장하던 중에 죽었으면(SETTLING) 묻지 않고 같은 러닝을 다시 저장한다 — 정산 영수증이
     * 같은 러닝을 두 번 적립하지 않게 막는다.
     */
    private fun recoverSession(finish: Boolean) {
        if (_state.value.isActive || startJob?.isActive == true) return
        enterForeground()
        startJob = scope.launch(Dispatchers.Main.immediate) {
            val checkpoint = runCatching { ServiceLocator.runCheckpoints.read() }.getOrNull()
            recovery.value = null
            if (checkpoint == null) {
                ServiceCompat.stopForeground(this@WalkSessionService, ServiceCompat.STOP_FOREGROUND_REMOVE)
                stopSelf()
                return@launch
            }
            goalKm.value = checkpoint.goalKm
            lastCheckpointAt = maxOf(lastCheckpointAt, checkpoint.savedAt)
            if (checkpoint.phase == RunCheckpointPhase.SETTLING) {
                resumedSettling = true
                followupsAttempted = false
                _state.value = checkpoint.state.copy(isPaused = true, saveStatus = RunSaveStatus.IDLE)
                activeMs = checkpoint.state.elapsedSec * 1000
                stopSession()
                return@launch
            }
            // 다른 계정(또는 로그인 전)에서 멈춘 러닝은 이어 달리지 않고 원래 주인 이름으로 저장만 한다
            val sameOwner = checkpoint.state.recordingOwner == ServiceLocator.sessionHolder.recordingOwner()
            beginTracking(checkpoint.state.partySize, checkpoint.state.recordingOwner, checkpoint.pausedForRecovery())
            if (finish || !sameOwner) stopSession()
        }
    }

    private fun enterForeground() {
        resetAnchor()
        goodHereAt = 0L
        createChannel()
        // 위치 권한이 있을 때만 location 타입을 함께 선언한다 —
        // 권한 없이 선언하면 API 34+에서 시작 자체가 거부된다.
        // location 타입은 API 29(Q)부터 있다. 29~33 에서 빼 두면 화면을 끄거나 앱을
        // 벗어나는 순간 GPS 가 멈춰, 멀쩡한 러닝이 "GPS 거리 부족"으로 깎인다.
        val withLocation = hasLocationPermission()
        val fgsType = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q && withLocation) {
            ServiceInfo.FOREGROUND_SERVICE_TYPE_HEALTH or ServiceInfo.FOREGROUND_SERVICE_TYPE_LOCATION
        } else {
            ServiceInfo.FOREGROUND_SERVICE_TYPE_HEALTH
        }
        ServiceCompat.startForeground(
            this,
            NOTIFICATION_ID,
            buildNotification(),
            fgsType,
        )
        foregroundHasLocation = Build.VERSION.SDK_INT < Build.VERSION_CODES.Q || withLocation
    }

    /**
     * 러닝 중에 위치 권한을 허용했다 — 시작할 때 권한이 없어 위치를 켜지 못한 러닝도 지금 GPS 를 켠다. 예전엔 위치를
     * 러닝을 시작할 때 한 번만 켜서, 그 뒤 설정에서 허용해도 그 러닝 내내 "GPS 찾는 중"이었다. 메인 스레드에서 부른다.
     * 전경 서비스에도 위치 종류를 더한다 — 빼 두면 화면을 끄거나 앱을 벗어나는 순간 GPS 가 멈춘다. 앱이 뒤에 있어
     * 더하지 못하면(Android 14+) 다음에 다시 해 본다.
     */
    private fun catchUpLocation() {
        if (!_state.value.isActive || settling || !hasLocationPermission()) return
        if (locationManager == null) startLocation()
        // 러닝 중에 정확한 위치를 허용했거나 대략적인 위치로 바꿨다 — 표시를 맞춘다. 휴대폰 위치로 받는 중이면(Play 서비스 없음)
        // 다시 등록해 GPS 를 받는다(예전엔 시작할 때 한 번만 읽어, 허용해도 그 러닝 내내 "대략적인 위치"였다, 2026-10-17 점검)
        val fine = hasFineLocation()
        if (fine != _state.value.precise) {
            _state.update { if (it.isActive) it.copy(precise = fine) else it }
            if (fine && fusedClient == null && locationManager != null) {
                stopLocation()
                resetAnchor()
                startLocation()
            }
        }
        // 구글 위치 서비스는 위치 기능을 켜고 끄는 것을 알려 주지 않는다 — 여기서 다시 읽는다(꺼짐 안내 · 다시 켬)
        refreshLocationOn()
        startBackupFeedIfSilent()
        if (!foregroundHasLocation) {
            foregroundHasLocation = runCatching {
                ServiceCompat.startForeground(
                    this, NOTIFICATION_ID, buildNotification(),
                    ServiceInfo.FOREGROUND_SERVICE_TYPE_HEALTH or ServiceInfo.FOREGROUND_SERVICE_TYPE_LOCATION,
                )
            }.isSuccess
        }
    }

    private fun beginTracking(
        partySize: Int,
        owner: String,
        restored: WalkSessionState? = null,
        group: StartGroup = StartGroup.NONE,
    ) {
        followupsAttempted = false
        resumedSettling = false
        _state.value = restored ?: WalkSessionState(
            isActive = true,
            startedAt = System.currentTimeMillis(),
            recordingOwner = owner,
            partySize = partySize,
            partyCrewId = if (partySize > 1) ServiceLocator.crewRepository.currentPartyCrewId() else "",
            group = group.group,
            groupCrewId = group.crewId,
            groupName = group.name,
        )
        activeMs = (restored?.elapsedSec ?: 0L) * 1000
        lastMoveElapsed = SystemClock.elapsedRealtime()

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
                // 자정이 지나면 오늘 걸음이 "마지막 측정 이후 걸음"으로 새로 시작한다 — 그 몫도 이 러닝 걸음이다
                // (예전엔 음수라 0 으로 버려, 화면이 꺼진 채 모인 몇 분치 걸음이 사라졌다)
                val delta = if (today < last) today else today - last
                var stepsAfter = -1
                _state.update { current ->
                    if (!current.isActive || current.isPaused || delta <= 0) return@update current
                    val updated = current.copy(steps = current.steps + delta, movingAt = System.currentTimeMillis()).withAutoLaps()
                    stepsAfter = updated.steps
                    updated
                }
                if (stepsAfter >= 0) {
                    // 걸음이 늘었다 — 움직이고 있다(오래 서 있음 판단을 처음부터)
                    lastMoveElapsed = SystemClock.elapsedRealtime()
                    updateNotification()
                }
            }
        }
        timerJob = scope.launch {
            var lastTick = SystemClock.elapsedRealtime()
            var lastCatchUp = lastTick
            while (isActive) {
                delay(1_000)
                val now = SystemClock.elapsedRealtime()
                val delta = (now - lastTick).coerceAtLeast(0L)
                lastTick = now
                val current = _state.value
                // 5초마다 — 멈춘 동안에도(예전엔 멈추면 위치를 켜도 "위치 꺼짐" 띠가 다시 달릴 때까지 남았다, 2026-10-17 점검)
                if (current.isActive && now - lastCatchUp >= CATCH_UP_EVERY_MS) {
                    lastCatchUp = now
                    // 제자리 · 러닝머신에서는 6m 를 못 움직여 좌표 알림이 오지 않는다 — GPS 가 실제로 고친
                    // 마지막 위치(알림 거리와 무관)도 본다. 그것까지 오래됐을 때만 신호가 약하다고 본다
                    lastKnownFixElapsed = lastKnownGpsFixElapsed()
                    // 러닝 중에 위치 권한을 허용했으면 위치를 켜고, 위치 기능 켜짐을 다시 읽는다(위의 catchUpLocation)
                    launch(Dispatchers.Main) { catchUpLocation() }
                }
                if (!current.isActive || current.isPaused) continue
                activeMs += delta
                val seconds = activeMs / 1000
                val fixAt = maxOf(lastFixElapsed, lastKnownFixElapsed)
                // 오래 서 있음 — 걸음 · 경로 점이 30초 넘게 그대로면 알린다(2026-10-17). 저장 중이면 알리지 않는다
                val stillNow = RunStill.isStill(current.saveStatus == RunSaveStatus.IDLE, now - lastMoveElapsed)
                _state.update {
                    if (!it.isActive || it.isPaused) it
                    else it.copy(
                        elapsedSec = seconds,
                        gpsLost = it.gpsFix && it.locationOn && fixAt > 0 &&
                            GpsSignal.isLost(now - fixAt, it.steps - stepsAtLastFix),
                        still = stillNow,
                        // 과속 구간이 10초 넘게 오지 않았다 — 이어진 과속이 아니다(경고를 거둔다)
                        overspeed = RunOverspeed.live(it.overspeed, System.currentTimeMillis()),
                    )
                }
                // 화면이 꺼져 있어도 알림 줄에서 보이게 — 멈춤이 바뀌면 바로, 거리 · 페이스는 몇 초마다(잠금화면 알림)
                if (stillNow != current.still) updateNotification(force = true) else updateNotification()
            }
        }
        startCheckpointLoop()
    }

    /** 5초마다 저장본을 남긴다 — 배터리 절약 · 메모리 부족으로 앱이 죽어도 러닝이 사라지지 않게 */
    private fun startCheckpointLoop() {
        checkpointJob = scope.launch {
            while (isActive) {
                val current = _state.value
                if (current.isActive && current.saveStatus == RunSaveStatus.IDLE) {
                    saveCheckpoint(current, RunCheckpointPhase.RECORDING)
                }
                delay(CHECKPOINT_EVERY_MS)
            }
        }
    }

    /** 마지막으로 저장한 시각 — 폰 시계가 뒤로 가도 저장 시각은 거꾸로 가지 않게 */
    @Volatile private var lastCheckpointAt = 0L

    private suspend fun saveCheckpoint(state: WalkSessionState, phase: RunCheckpointPhase): Boolean =
        runCatching {
            val goal = goalKm.value.takeIf { it.isFinite() && it > 0 } ?: 5.0
            val savedAt = maxOf(System.currentTimeMillis(), state.startedAt, lastCheckpointAt)
            ServiceLocator.runCheckpoints.save(RunCheckpoint(state, goal, savedAt, phase))
            lastCheckpointAt = savedAt
        }.isSuccess

    private fun setPaused(paused: Boolean) {
        // 원자적으로 — 걸음 · 타이머 수집기가 그 사이에 올린 값을 덮어 잃지 않게. 잃으면 저장본보다
        // 작아져 "진행이 거꾸로 갔다"로 이후 저장이 모두 거절된다
        _state.update { current ->
            if (current.isActive && current.saveStatus == RunSaveStatus.IDLE) {
                current.copy(isPaused = paused, still = false, overspeed = null)
            } else {
                current
            }
        }
        // 멈췄다 다시 달리면 그때부터 센다 — 일시정지한 동안은 서 있어도 알리지 않는다
        lastMoveElapsed = SystemClock.elapsedRealtime()
        if (_state.value.isActive) updateNotification(force = true)
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
        _state.update { it.copy(isPaused = true, still = false, overspeed = null, saveStatus = RunSaveStatus.SAVING) }
        val session = _state.value
        stepJob?.cancel()
        timerJob?.cancel()
        checkpointJob?.cancel()
        stopLocation()
        scope.launch {
          try {
            // 저장을 시작한다는 표시를 먼저 남긴다 — 저장 도중 죽으면 다음에 같은 러닝을 다시 저장한다.
            // 표시를 못 남겼으면 저장하지 않는다: 저장 뒤에 죽으면 다음 실행이 이미 저장된 러닝을
            // "이어 달리기"로 되살려 버린다. 저장 실패로 두고 다시 시도하게 한다.
            check(saveCheckpoint(session, RunCheckpointPhase.SETTLING)) { "run checkpoint boundary not saved" }
            // 이 러닝을 시작한 계정이 지금 로그인한 계정과 다르면(되살린 옛 러닝) 지금 계정의 신발 ·
            // 코스 · 크루를 이 러닝에 섞지 않는다
            val foreignOwner = session.recordingOwner != ServiceLocator.sessionHolder.recordingOwner()
            // 파티런이면 정산 시점의 실제 인원을 쓴다 — 러닝 중 거리 이탈로 빠진 인원 반영.
            // 되살린 파티런은 로비 상태가 비어 있다 — 출발할 때 적어 둔 인원 · 크루로 정산한다
            val partyLive = ServiceLocator.crewRepository.party.value.isActive
            val settleSize = if (session.partySize > 1 && partyLive) {
                ServiceLocator.crewRepository.currentPartySize()
            } else {
                session.partySize
            }
            // 크루 러닝이었다면 어느 크루였는지. 크루 순위가 세는 것이 이 값이다.
            // 파티 상태는 아래 finishParty() 에서 결과 화면으로 넘어가므로
            // 지금 읽어 둔다.
            val partyCrewId = when {
                session.partySize <= 1 || foreignOwner -> ""
                partyLive -> ServiceLocator.crewRepository.currentPartyCrewId()
                else -> session.partyCrewId
            }
            // 러닝으로 볼 수 없는 세션은 여기서 걸러진다 — 걸음 0으로 정산해
            // 적립도, 에너지 소모도, 코스 완주도 일어나지 않게 한다.
            // 모의 위치가 한 번이라도 보였으면 러닝으로 치지 않는다
            // 테스트 도구로 흉내 낸 걸음이 섞였으면 적립하지 않는다 — 아래 걸음 0 으로 적혀 서버에도 올라가지 않는다
            val verdict = if (session.mockLocation || session.simulated) {
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
            val boostBps = if (foreignOwner) 0 else ServiceLocator.rewardRepository.equippedBoostBps()
            // 종족도 같은 이유로 정산 전에 읽는다. 이 값이 종족 랭킹에서
            // 이 거리가 어느 편에 쌓일지를 정한다.
            val equippedFaction = if (foreignOwner) null else ServiceLocator.database.sneakerDao().equippedNow()
                ?.factionId?.let { Faction.of(it) }
            // 앞선 실행이 이미 정산했다(정산 뒤 저장본을 지우기 전에 앱이 죽어 되살린 러닝) — 코스 완주 ·
            // 기록 같은 뒤따르는 일을 다시 하지 않는다(코스 완주가 두 번 세어졌다). 이 실행 안에서 저장을
            // 다시 시도하는 경우는 followupsAttempted 가 맡는다 — 여기서 영수증을 보면 앞 시도가 정산만
            // 하고 뒤따르는 일을 못 한 경우까지 건너뛴다.
            val alreadySettled = resumedSettling && ServiceLocator.database.runSettlementDao()
                .find(session.recordingOwner, session.startedAt) != null
            // 코스 완주는 결과 화면이 보인 거리와 같은 기준으로 — GPS 100m 부터 GPS, 그 전엔 걸음(RunDistance). 예전엔 GPS · 걸음 중
            // 큰 쪽이라, 결과는 "97% · 완주 못 함"인데 완주 횟수는 늘었다(2026-10-17 점검). 서버는 경로가 코스를 덮었는지 다시 본다
            val courseKm = RunDistance.liveKm(session.gpsKm, creditedSteps)
            // 완주한 코스는 러닝을 저장하기 **전에** 올릴 거리에 적는다. 정산이 러닝을 저장하는 순간
            // 이미 돌던 올리기 일꾼이 그 러닝을 먼저 가져가면, 뒤에 적은 코스 기록을 못 보고 지나가
            // 코스 보상이 빠졌다. 러닝 시작 시각으로 적으므로 두 번 적혀도 한 줄이다.
            if (verdict.isRewardable && !foreignOwner && !alreadySettled && !followupsAttempted) {
                runCatching {
                    ServiceLocator.courseRepository.finishedCourse(courseKm)?.let { course ->
                        ServiceLocator.userPrefs.addPendingCourseRun(session.startedAt, course.encode())
                    }
                }
            }
            val reward = ServiceLocator.runSettlementRepository.settle(
                WalkSessionEntity(
                    startedAt = session.startedAt,
                    recordingOwner = session.recordingOwner,
                    // 폰 시계를 뒤로 돌렸어도 끝난 시각이 시작보다 앞서지 않게(서버가 영영 거절한다)
                    endedAt = maxOf(System.currentTimeMillis(), session.startedAt + session.elapsedSec * 1000),
                    // 가짜 위치로 무효가 된 러닝은 실제 걸음 · 시간으로 적어 서버에 올린다. 서버도 가짜 위치
                    // 표시(mockLocation)로 무효 처리하고 "최근 무효가 잦으면 적립 보류" 에 센다(예전엔 걸음 0 이라
                    // 올라가지 않았다). 속도 · 케이던스로 폰이 무효로 본 러닝은 튄 점을 경로에서 뺐으므로
                    // 서버가 같은 판정을 다시 할 수 없다 — 예전처럼 0 으로 둔다.
                    // 적립은 아래 정산이 creditedSteps(무효면 0)로만 계산하므로 달라지지 않는다.
                    steps = if (verdict.isRewardable || session.mockLocation) session.steps else 0,
                    durationSec = if (verdict.isRewardable || session.mockLocation) session.elapsedSec else 0,
                    // 결과 · 서버와 같은 거리(경로가 100m 넘으면 GPS 거리, 아니면 걸음 거리 — RunDistance). 예전엔 늘 걸음 거리라
                    // 기록 · 내 정보 · 코칭이 결과 화면과 달랐다(2026-10-17 점검). 무효(걸음 0)면 0
                    distanceMeters = RunDistance.creditedMeters(session.gpsKm, creditedSteps),
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
                    // AI 코칭의 1km 구간 — 랩은 위 거리와 같은 기준(RunDistance)이라 거리가 남는(인정) 러닝만. 무효 러닝은 거리가 0 이라 비운다
                    laps = if (creditedSteps > 0) {
                        com.stepup.android.domain.coaching.LapCodec.encode(
                            session.laps.map { com.stepup.android.domain.coaching.SavedLap(it.index, it.km, it.splitSec) },
                        )
                    } else {
                        ""
                    },
                )
            ) { energyDay ->
                // 신발이 다 닳은 뒤의 걸음 빼기는 닳는 시간(움직인 시간까지만)으로 — 서버 0068 과 같다
                val wearSec = RewardEconomy.wearSeconds(session.elapsedSec, RunDistance.creditedMeters(session.gpsKm, creditedSteps))
                ServiceLocator.rewardRepository.calculateSessionReward(
                    creditedSteps, settleSize, energyDay, activeSec = wearSec, startedAt = session.startedAt,
                )
            }
            // These legacy follow-ups are not durable yet. A same-process save retry
            // must not issue them again after a scheduling failure.
            if (!followupsAttempted && !alreadySettled) {
                followupsAttempted = true
            if (creditedSteps > 0) {
                val km = RunDistance.liveKm(session.gpsKm, creditedSteps)
                runCatching { Analytics.runFinished(km, settleSize) }
            }
            if (verdict.isRewardable && !foreignOwner) {
                // 코스 완주 정산 — 거리 1km당 정량 SUP. 코스 미선택이면 조용히 지나간다.
                runCatching {
                    // 완주 횟수 · (서버 경제가 아니면) 폰 적립. 올릴 코스 기록은 정산 전에 적어 두었다 —
                    // 러닝이 서버에 올라간 뒤 ServerSessionRecorder 가 내고, 서버가 경로로 다시 확인한다.
                    ServiceLocator.courseRepository.grantCompletionIfFinished(courseKm)
                }
                // 랭킹 재료 — 최고 속도
                runCatching { ServiceLocator.userPrefs.recordTopSpeed(session.topSpeedKmh) }
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
                // 결과 · 공유는 출발할 때 적은 모임으로 — 끝난 뒤의 방 상태(닫힘 · 혼자 남음 · 되살린 앱의 빈 방)로 다시 셈하지 않는다
                lastGroup = session.group,
                lastGroupCrewId = if (foreignOwner) "" else session.groupCrewId,
                lastGroupName = session.groupName,
                lastGroupSize = session.partySize,
                lastSimulated = session.simulated,
                lastOverspeedStopped = session.overspeedStopped,
                lastVerdict = verdict,
                lastTopSpeedKmh = session.topSpeedKmh,
                lastGpsKm = session.gpsKm,
                lastElapsedSec = session.elapsedSec,
                lastStartedAt = session.startedAt,
            )
            // 파티런이었다면 로비를 결과 화면으로 전환하고 방에서 나온다. 혼자 남은
            // 방에서 출발했어도(인원 1) 방은 닫아야 한다 — 안 그러면 로비가 계속
            // 뛰는 중으로 남아 위치를 보낸다.
            // 이 러닝이 모임(크루 · 번개)에서 출발한 러닝일 때만 — 대기실을 떠나 따로 켠 자유 러닝을 저장하면서
            // 남의 방을 닫거나(진행자) 방에서 나오지 않게
            if (session.group != RunGroup.NONE && ServiceLocator.crewRepository.party.value.isActive) {
                ServiceLocator.crewRepository.finishParty(session.startedAt, reward.rewardedSteps)
            }
            // 저장까지 끝났다 — 저장본을 지운다(실패해도 다음 실행이 같은 러닝을 다시 저장할 뿐이다)
            runCatching { ServiceLocator.runCheckpoints.clear(session.startedAt, session.recordingOwner) }
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

    /**
     * 저장 없이 끝내기(시안 R07) — 이번 러닝을 정산하지도, 기록하지도 않고 닫는다.
     * 혼자 뛰는 러닝만. 모임 러닝은 방 · 순위가 얽혀 있어 저장하고 마친다.
     * 이 러닝 동안 걸은 걸음은 평소 걸음처럼 오늘 걸음에 그대로 남는다(러닝 보상만 없다).
     */
    private fun discardSession() {
        val current = _state.value
        if (!canDiscard(current) || settling) return
        if (ServiceLocator.crewRepository.party.value.isActive) return
        settling = true
        val saving = checkpointJob
        scope.launch {
            try {
                // 저장본을 먼저 지운다 — 남기면 다음 실행이 이 러닝을 "이어 달리기"로 되살린다.
                // 쓰는 중인 저장이 끝난 뒤에 지워야 지운 뒤 다시 생기지 않는다.
                saving?.cancelAndJoin()
                val cleared = runCatching {
                    ServiceLocator.runCheckpoints.clear(current.startedAt, current.recordingOwner)
                }.isSuccess
                if (!cleared) {
                    // 못 지웠으면 러닝을 그대로 두고(저장본도 다시 남긴다) 화면에 알린다
                    startCheckpointLoop()
                    discardFailures.value = discardFailures.value + 1
                    return@launch
                }
                stepJob?.cancel()
                timerJob?.cancel()
                stopLocation()
                _state.value = WalkSessionState()
                ServiceCompat.stopForeground(this@WalkSessionService, ServiceCompat.STOP_FOREGROUND_REMOVE)
                stopSelf()
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

    private fun createChannel() = RunNotification.createChannel(this)

    /** 러닝 중 알림(잠금화면에 우리 모양 · 안드로이드 16 실시간 알림 — [RunNotification]) */
    private fun buildNotification(): android.app.Notification =
        RunNotification.build(this, _state.value.takeIf { it.isActive } ?: WalkSessionState())

    /** 마지막으로 알림을 다시 쓴 때 — 걸음 · 거리가 바뀔 때마다 쓰지 않게(시간은 알림의 시계가 스스로 흐른다) */
    @Volatile private var lastNotifiedAt = 0L

    private fun updateNotification(force: Boolean = false) {
        val now = SystemClock.elapsedRealtime()
        if (!force && now - lastNotifiedAt < NOTIFY_EVERY_MS) return
        lastNotifiedAt = now
        val manager = getSystemService(NOTIFICATION_SERVICE) as NotificationManager
        manager.notify(NOTIFICATION_ID, buildNotification())
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
                // 화면 · 결과와 같은 거리 — GPS 100m 부터 GPS, 그 전엔 걸음(RunDistance)
                val km = RunDistance.liveKm(current.gpsKm, current.steps)
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
        const val EXTRA_GROUP = "com.stepup.android.extra.GROUP"
        const val EXTRA_GROUP_CREW = "com.stepup.android.extra.GROUP_CREW"
        const val EXTRA_GROUP_NAME = "com.stepup.android.extra.GROUP_NAME"

        const val ACTION_START = "com.stepup.android.action.SESSION_START"
        const val ACTION_PAUSE = "com.stepup.android.action.SESSION_PAUSE"
        const val ACTION_RESUME = "com.stepup.android.action.SESSION_RESUME"
        const val ACTION_STOP = "com.stepup.android.action.SESSION_STOP"
        const val ACTION_DISCARD = "com.stepup.android.action.SESSION_DISCARD"
        const val ACTION_RECOVER = "com.stepup.android.action.SESSION_RECOVER"
        const val EXTRA_FINISH = "com.stepup.android.extra.FINISH"
        private const val CHECKPOINT_EVERY_MS = 5_000L

        /** 구글 위치 서비스가 이만큼 아무 위치도 주지 않으면 휴대폰 위치도 함께 듣는다(ms) */
        private const val FUSED_SILENT_MS = 15_000L

        /** 구글 위치 서비스가 이만큼 아무 위치도 주지 않으면 요청을 다시 건다(ms) — 그 뒤로도 조용하면 이만큼마다 */
        private const val FUSED_RENEW_MS = 30_000L

        /** 위치 권한 · 위치 기능 · 조용한 구글 위치 서비스를 다시 보는 간격(ms) */
        private const val CATCH_UP_EVERY_MS = 5_000L

        /** 러닝 중 CPU 를 깨워 두는 가장 긴 시간(ms) — 놓친 해제가 있어도 이만큼 뒤에는 스스로 풀린다 */
        private const val WAKE_LOCK_MAX_MS = 12 * 60 * 60 * 1000L

        /** 앱이 죽어 멈춘 채 남은 러닝 — 화면이 "이어 달리기 / 여기서 끝내기"를 묻는다 */
        val recovery = MutableStateFlow<RunCheckpoint?>(null)

        /** 남은 러닝 저장본이 있는지 본다. 지금 달리는 중이면 묻지 않는다. */
        suspend fun checkRecovery() {
            if (_state.value.isActive) return
            recovery.value = runCatching { ServiceLocator.runCheckpoints.read() }.getOrNull()
        }

        /** 남은 러닝을 되살린다. [finish] 면 되살린 뒤 바로 끝내고 저장한다. */
        fun recover(context: Context, finish: Boolean) {
            recovery.value = null
            ContextCompat.startForegroundService(
                context,
                intent(context, ACTION_RECOVER).putExtra(EXTRA_FINISH, finish),
            )
        }

        /** 알림을 다시 쓰는 가장 짧은 간격 — 거리 · 페이스 · 걸음 */
        private const val NOTIFY_EVERY_MS = 3_000L
        private const val NOTIFICATION_ID = 1001

        /**
         * @param partySize 파티런 인원(본인 포함). 1이면 개인 러닝.
         * @param group 대기실에서 출발한 러닝이면 그 모임([StartGroup.of]) — 화면 표시용이고 정산에는 쓰지 않는다.
         */
        fun start(context: Context, partySize: Int = 1, group: StartGroup = StartGroup.NONE) {
            val intent = intent(context, ACTION_START)
                .putExtra(EXTRA_PARTY_SIZE, partySize.coerceAtLeast(1))
                .putExtra(EXTRA_GROUP, group.group.name)
                .putExtra(EXTRA_GROUP_CREW, group.crewId)
                .putExtra(EXTRA_GROUP_NAME, group.name)
            ContextCompat.startForegroundService(context, intent)
        }

        fun pause(context: Context) {
            context.startService(intent(context, ACTION_PAUSE))
        }

        fun resume(context: Context) {
            context.startService(intent(context, ACTION_RESUME))
        }

        /** 저장 없이 끝낼 수 있는 러닝인가 — 달리는 중(저장 전), 혼자 뛰는 러닝 */
        fun canDiscard(state: WalkSessionState): Boolean =
            state.isActive && state.saveStatus == RunSaveStatus.IDLE && state.partySize <= 1

        /** 저장 없이 끝내기가 실패한 횟수 — 바뀌면 화면이 알린다(러닝은 그대로 이어진다) */
        val discardFailures = MutableStateFlow(0)

        /**
         * 시작 요청이 러닝을 켜지 않은 횟수 — 끝나지 않은 러닝이 있어 먼저 묻거나([recovery]) 저장 공간을 못 써 알렸다.
         * 바뀌면 자유러닝 시작 연출이 지도를 펼치지 않고 거둔다(실패한 러닝을 시작한 것처럼 보이지 않게).
         */
        val startRefusals = MutableStateFlow(0)

        fun discard(context: Context) {
            context.startService(intent(context, ACTION_DISCARD))
        }

        fun stop(context: Context) {
            context.startService(intent(context, ACTION_STOP))
        }

        /** 테스트 도구의 "걸음 흉내"를 썼다 — 달리는 중인 러닝을 적립 · 업로드에서 뺀다 */
        fun markSimulated() {
            _state.update { if (it.isActive) it.copy(simulated = true) else it }
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
