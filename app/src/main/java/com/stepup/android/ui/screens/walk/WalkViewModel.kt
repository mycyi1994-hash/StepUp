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
    sneakerRepository: SneakerRepository,
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

    /** XP 부스터가 활성인지 */
    val xpBoosted: StateFlow<Boolean> = boostRepository.active
        .map { list -> list.any { it.type == BoostType.XP_BOOSTER } }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), false)

    /** 헤더 컴팩트 토큰 표시용 SUP 잔액 */
    val balance: StateFlow<Double?> = rewardRepository.balance
        .map<Double, Double?> { it }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

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
            val id = courseRepository.saveRecorded(name, area, track, shared)
            // 방금 만든 코스를 바로 고른 상태로 둔다 — 만들었으면 다음엔 그걸
            // 뛰려는 것이다.
            if (id != null) courseRepository.select(id)
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

    /** 수동 랩 */
    fun recordLap() = WalkSessionService.recordManualLap()

    /** 목표 거리 — 프로세스 수명이라 화면을 오가도 유지된다 */
    val goalKm: StateFlow<Double> = WalkSessionService.goalKm

    fun setGoalKm(km: Double) {
        WalkSessionService.goalKm.value = km.coerceIn(1.0, 42.2)
    }

    val sensorAvailable: Boolean get() = stepRepository.stepSensorAvailable

    /** 에뮬레이터/센서 미지원 기기 데모용 */
    fun simulateSteps(count: Int) = stepRepository.simulateSteps(count)

    fun clearReward() = WalkSessionService.clearLastReward()

    /** 러닝 권한 안내를 이미 봤는지 */
    val permissionPrimerSeen: StateFlow<Boolean> = ServiceLocator.userPrefs.runPermissionPrimerSeen
        .stateIn(viewModelScope, SharingStarted.Eagerly, false)

    /** 저장된 값을 읽어서 — 화면이 막 열려 [permissionPrimerSeen] 이 아직 기본값일 때 */
    suspend fun permissionPrimerSeenNow(): Boolean =
        ServiceLocator.userPrefs.runPermissionPrimerSeen.first()

    fun markPermissionPrimerSeen() {
        viewModelScope.launch { ServiceLocator.userPrefs.setRunPermissionPrimerSeen() }
    }

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
