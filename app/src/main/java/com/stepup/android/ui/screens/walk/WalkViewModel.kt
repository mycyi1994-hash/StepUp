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
