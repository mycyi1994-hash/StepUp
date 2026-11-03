package com.stepup.android.service

import com.stepup.android.domain.GeoPoint
import com.stepup.android.domain.RunDistance
import com.stepup.android.domain.RunIntegrity
import com.stepup.android.domain.RunOverspeed
import com.stepup.android.domain.RunVerdict
import com.stepup.android.domain.TrackPoint
import com.stepup.android.domain.toGeoPoints

/*
 * 러닝 한 번의 상태(2026-11-03 iOS C — 안드로이드 WalkSessionService.kt 에서 옮겼다, 패키지 그대로).
 * 안드로이드 러닝 서비스와 iOS 러닝 세션이 같은 값을 내고, 러닝 화면(공용)은 이것만 본다.
 */

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
        const val NAME_LENGTH_LIMIT = 80

        /**
         * 출발한 방에서 — 번개 글의 방이면 번개, 크루가 있으면 크루, 둘 다 아니면 모임 없음.
         * [name] 은 대기실이 보인 이름(크루 이름 · 번개 글 제목)이다.
         */
        fun of(room: com.stepup.android.data.repo.PartyState, name: String): StartGroup = when {
            room.flashPostId != null -> StartGroup(RunGroup.FLASH, "", name.trim().take(NAME_LENGTH_LIMIT))
            !room.crewId.isNullOrBlank() -> StartGroup(RunGroup.CREW, room.crewId.orEmpty(), name.trim().take(NAME_LENGTH_LIMIT))
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
    /**
     * 신은 신발로 보상을 받으며 달릴 수 있는 시간(초) — 러닝을 시작할 때 신발의 남은 내구도 × 12초(달린 1분마다 5, 서버 0064 · 0066).
     * 이 시간을 넘긴 뒤의 걸음은 서버가 적립하지 않는다. 서버 신발이 아니거나 아직 못 읽었으면 null. 저장본에 남기지 않는다(되살리면 다시 읽는다)
     */
    val shoeLifeSec: Long? = null,
    /** 방금 끝난 러닝에서 신발 내구도를 다 썼다 — 결과 화면이 "그 뒤 걸음은 보상에 들지 않았어요"를 알린다 */
    val lastShoeWornOut: Boolean = false,
    /**
     * 크루 러닝(서버 0071)의 오늘 공용 체력 — 남은 양 · 총량 · 적립이 멈췄나(0). 크루 러닝이 5초마다 서버에서 받아 알려 준다
     * ([WalkSessionService.setCrewPool]). 잠금화면 알림이 보인다. 크루 러닝이 아니거나 아직 모르면 null(0 이 아니다).
     * 저장본에 남기지 않는다
     */
    val crewPoolRemaining: Double? = null,
    val crewPoolTotal: Double? = null,
    val crewPoolEmpty: Boolean = false,
) {

    /** 신발 내구도를 다 썼다 — 이제부터 걸음은 보상이 없다(2026-10-19 사용자 "내구도 다 소모하면 따로 안내") */
    val shoeWornOut: Boolean get() = shoeLifeSec != null && elapsedSec >= shoeLifeSec

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
fun WalkSessionState.withAutoLaps(): WalkSessionState {
    val km = RunDistance.liveKm(gpsKm, steps)
    var next = laps
    while (next.size < km.toInt()) {
        next = next + RunLap(index = next.size + 1, km = (next.size + 1).toDouble(), splitSec = elapsedSec)
    }
    return if (next === laps) this else copy(laps = next)
}

