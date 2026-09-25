package com.stepup.android.ui.screens.events

import com.stepup.android.ui.experience.ExperienceEvents
import com.stepup.android.ui.experience.FeedbackCue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.stepup.android.core.ServiceLocator
import java.time.ZoneId
import java.time.LocalDate
import java.time.Instant
import kotlinx.coroutines.flow.combine
import com.stepup.android.data.local.RewardType
import com.stepup.android.data.repo.EventClaimResult
import com.stepup.android.data.repo.EventDef
import com.stepup.android.data.repo.EventRepository
import com.stepup.android.data.repo.RewardRepository
import com.stepup.android.data.repo.StepRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/** 오늘의 도전 상태 — 걸음 · 목표 · 오늘 나간 보너스 */
data class DailyChallenge(val steps: Int, val goal: Int, val paidToday: Double) {
    val fraction: Float get() = if (goal > 0) (steps.toFloat() / goal).coerceIn(0f, 1f) else 0f
    val done: Boolean get() = goal in 1..steps
}

/** 수령 시도 결과 (일회성) */
sealed interface ClaimResult {
    data class Success(val amount: Double) : ClaimResult
    data object NotFinished : ClaimResult
    data object AlreadyClaimed : ClaimResult
    data object SignInRequired : ClaimResult
    data object Failed : ClaimResult
}

class EventsViewModel(
    stepRepository: StepRepository,
    rewardRepository: RewardRepository,
    private val eventRepository: EventRepository,
    avatarRepository: com.stepup.android.data.repo.AvatarRepository,
) : ViewModel() {
    val look = avatarRepository.look.map<com.stepup.android.domain.AvatarLook, com.stepup.android.domain.AvatarLook?> { it }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)
    val claimingId = MutableStateFlow<String?>(null)

    val balance: StateFlow<Double?> = rewardRepository.balance
        .map<Double, Double?> { it }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    /**
     * 주간 챌린지(Step Surge) 진행도 — 서버가 확인한 이번 주(한국 시각) 러닝 걸음.
     * 폰 만보기 걸음을 보이면 100% 인데도 서버가 "아직"이라며 거절한다. 못 받으면 null(—).
     */
    private val _weekSteps = MutableStateFlow<Long?>(null)
    val weekSteps: StateFlow<Long?> = _weekSteps

    val claimedIds: StateFlow<Set<String>?> = eventRepository.claimedIds
        .map<Set<String>, Set<String>?> { it }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    private val startOfToday: Long =
        LocalDate.now().atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli()

    /**
     * 오늘의 도전 — 하루 걸음 목표.
     *
     * 보너스는 목표를 넘기는 순간 저절로 적립된다(StepRepository). 그래서
     * 여기에는 "받기" 버튼이 없고, 원장에 오늘 보너스가 적혔는지로 지급
     * 여부를 보여 준다.
     */
    val daily: StateFlow<DailyChallenge?> = combine(
        stepRepository.todaySteps,
        stepRepository.dailyGoal,
        rewardRepository.sumOfTypeSince(RewardType.BONUS_GOAL, startOfToday),
    ) { steps, goal, paid ->
        DailyChallenge(steps = steps, goal = goal, paidToday = paid)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    /** 나이트 러너 — 서버가 잰, 한국 시각 저녁 8시 이후에 시작한 러닝의 거리 합(km). 못 받으면 null(—). */
    private val _nightKm = MutableStateFlow<Double?>(null)
    val nightKm: StateFlow<Double?> = _nightKm

    init {
        refreshProgress()
    }

    /** 서버의 도전 진행값을 다시 받는다 — 화면을 열 때와 받기를 누른 뒤 */
    fun refreshProgress() {
        viewModelScope.launch {
            runCatching { eventRepository.serverProgress(com.stepup.android.data.repo.Events.STEP_SURGE) }
                .getOrNull()?.let { _weekSteps.value = it.toLong() }
            runCatching { eventRepository.serverProgress(com.stepup.android.data.repo.Events.NIGHT_QUEST) }
                .getOrNull()?.let { _nightKm.value = it }
        }
    }

    val claimResult = MutableStateFlow<ClaimResult?>(null)

    fun claim(def: EventDef, progress: Float) {
        if (claimingId.value != null) return
        claimingId.value = def.id
        viewModelScope.launch {
          try {
            // 서버가 목표를 다시 재고 지급한다. 지급이 확인된 뒤에만 "받음"이 된다.
            val result = when (val r = eventRepository.claim(def, progress)) {
                is EventClaimResult.Paid -> ClaimResult.Success(r.amount)
                EventClaimResult.NotFinished -> ClaimResult.NotFinished
                EventClaimResult.AlreadyClaimed -> ClaimResult.AlreadyClaimed
                EventClaimResult.SignInRequired -> ClaimResult.SignInRequired
                EventClaimResult.Failed -> ClaimResult.Failed
            }
            ExperienceEvents.emit(if (result is ClaimResult.Success) FeedbackCue.Reward else FeedbackCue.Error)
            claimResult.value = result
          } catch (cancelled: kotlinx.coroutines.CancellationException) {
              throw cancelled
          } catch (_: Exception) {
              claimResult.value = ClaimResult.Failed
          } finally {
              claimingId.value = null
              refreshProgress()
          }
        }
    }

    fun consumeClaimResult() {
        claimResult.value = null
    }

    companion object {
        /** 나이트 러너가 세는 시작 시각 — 저녁 8시 */
        const val NIGHT_FROM_HOUR = 20

        val Factory = viewModelFactory {
            initializer {
                EventsViewModel(
                    ServiceLocator.stepRepository,
                    ServiceLocator.rewardRepository,
                    ServiceLocator.eventRepository,
                    ServiceLocator.avatarRepository,
                )
            }
        }
    }
}
