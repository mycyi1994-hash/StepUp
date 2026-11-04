package com.stepup.android.ui.screens.walk

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.stepup.android.core.ServiceLocator
import kotlinx.coroutines.flow.combine
import com.stepup.android.domain.AvatarLook
import com.stepup.android.data.repo.BoostRepository
import com.stepup.android.data.repo.CourseRepository
import com.stepup.android.data.repo.RewardRepository
import com.stepup.android.data.repo.SneakerRepository
import com.stepup.android.data.repo.StepRepository
import com.stepup.android.domain.BoostType
import com.stepup.android.domain.GeoPoint
import com.stepup.android.domain.RunCourse
import com.stepup.android.domain.Sneaker
import com.stepup.android.service.RunLap
import com.stepup.android.service.WalkSessionService
import com.stepup.android.service.WalkSessionState
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class WalkViewModel(
    private val stepRepository: StepRepository,
    rewardRepository: RewardRepository,
    private val sneakerRepository: SneakerRepository,
    boostRepository: BoostRepository,
    private val courseRepository: CourseRepository,
) : ViewModel() {

    /** 지금 달리기로 고른 코스 — 지도 카드와 완주 보상 표시에 쓴다 */
    val selectedCourse: StateFlow<RunCourse?> = courseRepository.selectedCourse
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    val session: StateFlow<WalkSessionState> = WalkSessionService.state

    val energy: StateFlow<Double> = stepRepository.energy
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), 0.0)

    val sneakerLevel: StateFlow<Int> = rewardRepository.sneakerLevel
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), 1)

    val equipped: StateFlow<Sneaker?> = sneakerRepository.equipped
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    /** 내 신발 — 자유러닝 시작 전 신발 교체(2026-10-26). 신는 중인 신발 먼저, 그다음 높은 등급 · 레벨 */
    val shoes: StateFlow<List<Sneaker>> = sneakerRepository.inventory
        .map { list ->
            list.sortedWith(
                compareByDescending<Sneaker> { it.equipped }.thenByDescending { it.rarity.ordinal }.thenByDescending { it.level },
            )
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    /** 신발을 바꾸는 중 — 두 번 보내지 않는다 */
    val wearing = kotlinx.coroutines.flow.MutableStateFlow(false)

    /**
     * 이 신발을 신는다 — 신발 탭과 같은 길([SneakerRepository.equipOnServer]). 실제 착용이 이 신발로 바뀐 것을 본 뒤에야 [onResult] true.
     * 응답을 못 받았으면(연결) 서버 값을 다시 맞추고, 착용이 바뀌지 않았으면 false.
     */
    fun wear(id: Long, onResult: (Boolean) -> Unit) {
        if (wearing.value) return
        // 러닝이 켜진 뒤에는 바꾸지 않는다 — 점검의 신발 교체는 러닝 전이라 그대로 된다
        if (com.stepup.android.data.repo.shoeChangeBlockedByRun()) {
            onResult(false)
            return
        }
        wearing.value = true
        viewModelScope.launch {
            val worn = try {
                val outcome = sneakerRepository.equipOnServer(id)
                if (outcome == com.stepup.android.data.repo.EconomyOutcome.Ok || outcome == com.stepup.android.data.repo.EconomyOutcome.Offline) {
                    kotlinx.coroutines.withTimeoutOrNull(WEAR_WAIT_MS) { sneakerRepository.equipped.first { it?.id == id } } != null
                } else false
            } catch (cancelled: kotlinx.coroutines.CancellationException) {
                wearing.value = false
                throw cancelled
            } catch (_: Exception) {
                false
            }
            if (!worn) ServiceLocator.refreshEconomyInBackground()
            com.stepup.android.ui.experience.ExperienceEvents.emit(
                if (worn) com.stepup.android.ui.experience.FeedbackCue.Equip else com.stepup.android.ui.experience.FeedbackCue.Error,
            )
            wearing.value = false
            onResult(worn)
        }
    }

    /** XP 부스터가 활성인지 */
    val xpBoosted: StateFlow<Boolean> = boostRepository.active
        .map { list -> list.any { it.type == BoostType.XP_BOOSTER } }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), false)

    /** 헤더 컴팩트 토큰 표시용 SUP 잔액 */
    /**
     * 결과 화면의 잔고. 방금 끝난 러닝을 서버가 확인한 뒤 원장 동기화가 아직 성공하지 않았으면
     * 서버가 돌려준 잔고를 쓴다 — "+N" 옆에 적립 전 잔고가 보이지 않게.
     */
    val balance: StateFlow<Double?> = combine(
        rewardRepository.balance,
        com.stepup.android.data.repo.RecordedBalances.byStart,
        ServiceLocator.economySync.syncedFrom,
        WalkSessionService.state,
    ) { ledger, recorded, syncedFrom, state ->
        // 지금 로그인한 계정이 올린 러닝일 때만 — 계정을 바꾼 뒤 앞 계정의 잔고를 보이지 않게
        val me = runCatching { ServiceLocator.sessionHolder.currentUserId() }.getOrNull()
        com.stepup.android.data.repo.RecordedBalances.resultBalance(ledger, recorded[state.lastStartedAt], syncedFrom, me)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    /**
     * 랩은 세션 상태의 일부로 서비스가 소유한다.
     * 화면(뷰모델)이 죽었다 살아나도 랩이 유지되고, 세션 시작 시 함께 초기화된다.
     */
    val laps: StateFlow<List<RunLap>> = WalkSessionService.state
        .map { it.laps }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    // ── 코스 녹화 ───────────────────────────────────────────────
    //
    // "코스 만들기"를 누르면 여기로 보내진다. 러닝을 끝내면 방금 지나온 길이
    // 저장 창과 함께 올라온다 — 코스는 손으로 그은 선이 아니라 실제로 뛴
    // 길이라야 남이 받아서 뛸 수 있다.

    /** 이번 러닝이 코스 녹화인가 */
    val courseRecording: StateFlow<Boolean> = courseRepository.recording
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), false)

    /** 방금 끝난 러닝의 GPS 트랙 — 저장 창의 재료 */
    val lastTrack: StateFlow<List<GeoPoint>> = WalkSessionService.lastTrack

    fun saveRecordedCourse(name: String, area: String, shared: Boolean) {
        val track = WalkSessionService.lastTrack.value
        viewModelScope.launch {
            // 저장만 한다 — 고르지 않는다. 예전엔 방금 만든 코스를 골라 두어, 그 뒤의 모든 자유 러닝이 코스 러닝(코스 안내 ·
            // 이탈 알림 · "한 바퀴" 결과)으로 바뀌었고 이 러닝의 결과도 완주처럼 보였다(2026-10-17 점검). 달리려면 코스 허브의
            // 내 코스에서 고른다
            courseRepository.saveRecorded(name, area, track, shared)
            WalkSessionService.clearLastTrack()
        }
    }

    /** 저장하지 않고 녹화를 끝낸다 */
    fun cancelRecording() {
        viewModelScope.launch {
            courseRepository.cancelRecording()
            WalkSessionService.clearLastTrack()
        }
    }

    /** 코스 안내만 끈다(시안 K08) — 러닝 · 시간 · 거리 · 경로는 그대로 이어진다 */
    fun clearCourse() {
        viewModelScope.launch { courseRepository.clearSelection() }
    }

    /** 수동 랩 */
    fun recordLap() = WalkSessionService.recordManualLap()

    /** 목표 거리 — 프로세스 수명이라 화면을 오가도 유지된다 */
    val goalKm: StateFlow<Double> = WalkSessionService.goalKm

    fun setGoalKm(km: Double) {
        WalkSessionService.goalKm.value = km.coerceIn(1.0, 42.2)
    }

    val sensorAvailable: Boolean get() = stepRepository.stepSensorAvailable

    /** 에뮬레이터/센서 미지원 기기 데모용 */
    fun simulateSteps(count: Int) {
        // 흉내 걸음이 섞인 러닝은 실제 서버에 적립을 받지 않게 표시한다
        WalkSessionService.markSimulated()
        stepRepository.simulateSteps(count)
    }

    fun clearReward() = WalkSessionService.clearLastReward()

    /**
     * 방금 끝난 러닝이 서버에서 어디까지 확인됐는가.
     *
     * 완료 화면의 머리말이 이 값을 따른다. 서명을 받기 전에는 "적립 완료"라고
     * 적지 않는다 — 앱 안에는 적혔어도, 서버가 인정한 것은 아직 아니다.
     * 저장된 줄을 [WalkSessionState.lastStartedAt] 으로 찾는다.
     */
    val lastUpload: StateFlow<String?> = combine(
        WalkSessionService.state,
        stepRepository.recentSessions(5),
    ) { state, rows ->
        if (state.lastRewardPoints == null || state.lastStartedAt == 0L) {
            null
        } else {
            rows.firstOrNull { it.startedAt == state.lastStartedAt }?.uploadState
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    /**
     * 방금 끝난 러닝의 신발 닳음이 이 폰의 신발 사본에 들어왔는가 — 서버가 이 러닝을 확인한(SIGNED) **뒤에 시작한** 동기화가
     * 성공했을 때만. 그 전까지 결과 화면은 미리 뺀 내구도를 보인다 — 예전엔 확인되자마자 러닝 전 값으로 되돌아갔다가 동기화 뒤에야
     * 줄었다(2026-11-03 점검). 서버가 거절한 러닝은 닳지 않으므로 사본 그대로다.
     */
    val lastShoeSettled: StateFlow<Boolean> = combine(
        WalkSessionService.state,
        stepRepository.recentSessions(5),
        ServiceLocator.economySync.syncedFrom,
    ) { state, rows, syncedFrom ->
        val row = rows.firstOrNull { it.startedAt == state.lastStartedAt }
        row != null && shoeWearSettled(row, syncedFrom)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), false)

    /**
     * 이 계정의 러닝 가운데 서버가 아직 확인하지 않았거나, 확인했어도 그 뒤 동기화가 아직 없는 러닝이 신발을 닳게 할 양(내구도 점) —
     * 출발 전 점검이 신은 신발에서 빼서 보인다(화면에만 — 서버에 보내지 않는다). 러닝 줄에는 신은 신발이 남지 않아 지금 신은 신발에서 뺀다.
     */
    val pendingShoeWear: StateFlow<Int> = combine(
        stepRepository.recentSessions(PENDING_WEAR_SCAN),
        ServiceLocator.economySync.syncedFrom,
    ) { rows, syncedFrom ->
        pendingShoeWearOf(rows, syncedFrom, System.currentTimeMillis())
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), 0)

    /**
     * 서버가 확인한 이번 러닝의 적립액. 확인 전이면 null — 완료 화면은 확인된 뒤에만 금액을 보인다.
     * 폰이 계산한 값은 예상치일 뿐이라 여기 쓰지 않는다.
     */
    val lastServerPoints: StateFlow<Double?> = combine(
        WalkSessionService.state,
        stepRepository.recentSessions(5),
    ) { state, rows ->
        if (state.lastStartedAt == 0L) {
            null
        } else {
            rows.firstOrNull { it.startedAt == state.lastStartedAt }
                ?.takeIf { it.uploadState == com.stepup.android.data.local.UploadState.SIGNED.name }
                ?.claimAmount?.toDoubleOrNull()
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    /** 완료 화면의 캐릭터 — 홈 · 꾸미기와 같은 모습 */
    val look: StateFlow<AvatarLook?> = ServiceLocator.avatarRepository.look
        .map<AvatarLook, AvatarLook?> { it }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    /** 완료 화면의 오늘 목표 진행 */
    val todaySteps: StateFlow<Int> = stepRepository.todaySteps
    val dailyGoal: StateFlow<Int> = stepRepository.dailyGoal
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), 0)

    companion object {
        /** 착용을 보낸 뒤 목록이 따라오기를 기다리는 가장 긴 시간(ms) */
        private const val WEAR_WAIT_MS = 5_000L

        val Factory = viewModelFactory {
            initializer {
                WalkViewModel(
                    ServiceLocator.stepRepository,
                    ServiceLocator.rewardRepository,
                    ServiceLocator.sneakerRepository,
                    ServiceLocator.boostRepository,
                    ServiceLocator.courseRepository,
                )
            }
        }
    }
}

/** [WalkViewModel.pendingShoeWear] 가 훑는 최근 러닝 수 */
internal const val PENDING_WEAR_SCAN = 20

/** 서버가 7일 지난 러닝은 받지 않는다 — 그보다 오래된 줄은 올라가지 않아 닳지도 않는다 */
private const val PENDING_WEAR_WINDOW_MS = 7L * 24 * 60 * 60 * 1000

/**
 * 이 러닝의 신발 닳음이 폰의 신발 사본에 이미 들었는가. 확인(SIGNED)된 뒤 시작한 동기화([syncedFrom] — 마지막으로 성공한 동기화가
 * 시작된 시각)가 있으면 들었다. 확인 시각은 [com.stepup.android.data.local.WalkSessionEntity.uploadAttemptedAt](확인할 때 적는다).
 * 거절된 러닝은 서버가 닳게 하지 않는다 — 처음부터 들 것이 없다.
 */
internal fun shoeWearSettled(row: com.stepup.android.data.local.WalkSessionEntity, syncedFrom: Long): Boolean =
    when (row.uploadState) {
        com.stepup.android.data.local.UploadState.REJECTED.name -> true
        com.stepup.android.data.local.UploadState.SIGNED.name -> syncedFrom > row.uploadAttemptedAt
        else -> false
    }

/**
 * 아직 사본에 들지 않은 러닝들의 닳음 합(러닝마다 [com.stepup.android.ui.screens.items.durabilityWear] — 서버가 러닝마다 뺀다).
 * - 확인 전(PENDING · FAILED): 올라갈 러닝만 — 시간이 있고, 가짜 위치(서버가 무효로 본다)가 아니고, 7일 안이고, 계정이 있는 줄.
 * - 확인됨(SIGNED): 이 앱이 켜진 뒤 동기화가 한 번이라도 성공했고([syncedFrom] > 0) 그 동기화가 확인보다 앞설 때만. 앱을 막 켜
 *   동기화 전이면 사본이 이미 그 닳음을 담았을 수 있다 — 두 번 빼지 않게 세지 않는다.
 */
internal fun pendingShoeWearOf(rows: List<com.stepup.android.data.local.WalkSessionEntity>, syncedFrom: Long, now: Long): Int =
    rows.sumOf { row ->
        val unsettled = when (row.uploadState) {
            com.stepup.android.data.local.UploadState.PENDING.name,
            com.stepup.android.data.local.UploadState.FAILED.name ->
                row.recordingOwner != com.stepup.android.domain.RecordingOwner.LEGACY && !row.mockLocation &&
                    row.startedAt >= now - PENDING_WEAR_WINDOW_MS &&
                    (row.steps > 0 || row.startedAt >= com.stepup.android.data.local.ZERO_STEP_UPLOAD_FROM_MS)
            com.stepup.android.data.local.UploadState.SIGNED.name -> syncedFrom > 0 && !shoeWearSettled(row, syncedFrom)
            else -> false
        }
        if (unsettled) com.stepup.android.ui.screens.items.durabilityWear(row.durationSec) else 0
    }
