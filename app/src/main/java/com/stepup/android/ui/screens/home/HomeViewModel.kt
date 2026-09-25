package com.stepup.android.ui.screens.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.stepup.android.core.ServiceLocator
import com.stepup.android.data.local.DailyStepsEntity
import com.stepup.android.data.prefs.UserPrefs
import com.stepup.android.data.repo.AvatarRepository
import com.stepup.android.data.repo.NotificationRepository
import com.stepup.android.data.repo.RewardRepository
import com.stepup.android.data.repo.SneakerRepository
import com.stepup.android.data.repo.StepRepository
import com.stepup.android.domain.AvatarLook
import com.stepup.android.domain.RewardEconomy
import com.stepup.android.domain.RunnerLevels
import com.stepup.android.domain.RunnerProgress
import com.stepup.android.domain.Sneaker
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import java.time.LocalDate
import java.time.ZoneId

class HomeViewModel(
    private val stepRepository: StepRepository,
    rewardRepository: RewardRepository,
    sneakerRepository: SneakerRepository,
    notificationRepository: NotificationRepository,
    prefs: UserPrefs,
    avatarRepository: AvatarRepository,
) : ViewModel() {

    /** 오늘 0시(기기 시간대). 오늘 번 포인트와 오늘 운동 시간의 기준이다. */
    private val startOfToday: Long =
        LocalDate.now().atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli()

    /**
     * 오늘 러닝·목표 보너스·이벤트로 번 SUP.
     *
     * 원장에서 아직 못 읽었으면 null 이다. 0 으로 두면 앱을 켜자마자 잠깐
     * "오늘 0 SUP"가 보였다가 바뀌는데, 그 순간에는 정말 못 번 것처럼 읽힌다.
     */
    val todayEarned: StateFlow<Double?> = rewardRepository.earnedSince(startOfToday)
        .map<Double, Double?> { it }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    /** 오늘 시작한 러닝 세션의 운동 시간 합(초) */
    val todayRunSec: StateFlow<Long> = stepRepository.observeDurationSince(startOfToday)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), 0L)

    /** 캐릭터가 지금 입고 있는 것 — 꾸미기 · 내 정보와 같은 값 */
    val look: StateFlow<AvatarLook?> = avatarRepository.look
        .map<AvatarLook, AvatarLook?> { it }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    data class UiState(
        val todaySteps: Int = 0,
        val goal: Int = UserPrefs.DEFAULT_GOAL,
        val energy: Double = 0.0,
        val maxEnergy: Double = RewardEconomy.BASE_MAX_ENERGY,
        val balance: Double = 0.0,
        val streak: Int = 0,
        val sneakerLevel: Int = 1,
        val lifetimeSteps: Long = 0,
        val week: List<DailyStepsEntity> = emptyList(),
        val sensorAvailable: Boolean = true,
        val equipped: Sneaker? = null,
        val avatarId: Int = 0,
        val avatarRev: Int = 0,
        /** 프로필에서 정한 닉네임. 비어 있으면 화면이 기본 호칭을 쓴다. */
        val nickname: String = "",
        /**
         * 실제 값이 한 번이라도 도착했는가.
         *
         * 이 클래스의 기본값은 화면이 처음 그려질 때 쓰는 자리끼우개다.
         * 그 상태의 에너지 0을 "다 썼다"로 그리면, 앱을 켜자마자 잠깐
         * 빈 게이지가 보였다가 채워진다. 도착 전에는 비율을 짓지 않는다.
         */
        val loaded: Boolean = false,
    ) {
        val energyPercent: Int
            get() = if (maxEnergy > 0) ((energy / maxEnergy) * 100).toInt().coerceIn(0, 100) else 0

        /**
         * 남은 에너지로 아직 적립할 수 있는 걸음.
         *
         * 에너지 %는 결국 이 숫자의 다른 표현이다. 에너지 한 칸이 600보이고,
         * 신발의 에너지 절감이 붙으면 같은 칸으로 더 걸을 수 있다.
         */
        val earnableSteps: Int
            get() = RewardEconomy.earnableSteps(energy, equipped?.energyEfficiency ?: 1.0)

        /** 그 걸음을 다 걸었을 때 받는 SUP — 신발 부스트와 종족 배율 포함 전 기본값 */
        val earnableSup: Double
            get() = earnableSteps * RewardEconomy.POINTS_PER_STEP
        val goalPercent: Int
            get() = if (goal > 0) (todaySteps * 100 / goal) else 0

        /** 러너 레벨 — 누적으로 걸은 거리가 곧 경험치다 */
        val runner: RunnerProgress get() = RunnerLevels.ofSteps(lifetimeSteps)
        val level: Int get() = runner.level
    }

    private data class Identity(
        val avatarId: Int,
        val avatarRev: Int,
        val nickname: String,
    )

    private data class Wallet(
        val balance: Double,
        val streak: Int,
        val sneakerLevel: Int,
        val lifetimeSteps: Long,
    )

    val unreadCount: StateFlow<Int> = notificationRepository.unreadCount
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), 0)

    val uiState: StateFlow<UiState> = combine(
        combine(
            stepRepository.todaySteps,
            stepRepository.dailyGoal,
            stepRepository.energy,
        ) { steps, goal, energy -> Triple(steps, goal, energy) },
        combine(
            rewardRepository.balance,
            stepRepository.streak,
            rewardRepository.sneakerLevel,
            stepRepository.observeLifetimeSteps(),
        ) { balance, streak, level, lifetime -> Wallet(balance, streak, level, lifetime) },
        stepRepository.observeWeek(),
        sneakerRepository.equipped,
        combine(prefs.avatarId, prefs.avatarRev, prefs.nickname) { id, rev, nick ->
            Identity(id, rev, nick)
        },
    ) { (steps, goal, energy), wallet, week, equipped, identity ->
        UiState(
            todaySteps = steps,
            goal = goal,
            energy = energy,
            maxEnergy = RewardEconomy.maxEnergy(equipped?.level ?: wallet.sneakerLevel),
            balance = wallet.balance,
            streak = wallet.streak,
            sneakerLevel = wallet.sneakerLevel,
            lifetimeSteps = wallet.lifetimeSteps,
            week = week,
            sensorAvailable = stepRepository.stepSensorAvailable,
            equipped = equipped,
            avatarId = identity.avatarId,
            avatarRev = identity.avatarRev,
            nickname = identity.nickname,
            loaded = true,
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), UiState())

    /** 홈 화면의 권한 카드에서 권한이 허용됐을 때 호출 */
    fun onPermissionGranted() = stepRepository.startTracking()

    companion object {
        val Factory = viewModelFactory {
            initializer {
                HomeViewModel(
                    ServiceLocator.stepRepository,
                    ServiceLocator.rewardRepository,
                    ServiceLocator.sneakerRepository,
                    ServiceLocator.notificationRepository,
                    ServiceLocator.userPrefs,
                    ServiceLocator.avatarRepository,
                )
            }
        }
    }
}
