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
import com.stepup.android.domain.earningMultiplierWith
import com.stepup.android.domain.energyMultOf
import com.stepup.android.domain.AvatarLook
import com.stepup.android.domain.RewardEconomy
import com.stepup.android.domain.RunnerLevels
import com.stepup.android.domain.RunnerProgress
import com.stepup.android.domain.Sneaker
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.drop
import kotlinx.coroutines.launch
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import java.time.LocalDate
import java.time.ZoneId

@OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
class HomeViewModel(
    private val stepRepository: StepRepository,
    rewardRepository: RewardRepository,
    sneakerRepository: SneakerRepository,
    notificationRepository: NotificationRepository,
    private val prefs: UserPrefs,
    avatarRepository: AvatarRepository,
    /** 오늘의 보상(서버 0118) — 상자 현황 저장소. 서버 경제가 없는 빌드 · 검사에서는 null(예전 홈) */
    private val shoeRules: com.stepup.android.data.repo.ShoeRulesRepository? = null,
    /** 지금 로그인해 있는가 — 오늘의 보상은 로그인한 계정의 서버 상태다 */
    private val signedIn: suspend () -> Boolean = { false },
) : ViewModel() {

    /**
     * 오늘 날짜(기기 시간대). 1분마다 보고 바뀌면 다시 낸다.
     *
     * 기준 시각을 화면을 만들 때 한 번만 정하면, 앱을 켜 둔 채 자정을 넘겼을 때
     * "오늘 번 SUP"에 어제 것이 섞이고 월요일에도 지난주가 "이번 주"로 남는다.
     */
    private val today: Flow<LocalDate> = flow {
        while (true) {
            emit(LocalDate.now())
            delay(60_000)
        }
    }.distinctUntilChanged()

    private fun startOf(day: LocalDate): Long = day.atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli()

    /** 이번 주 월요일 0시(기기 시간대) — 홈의 "이번 주" 요약 기준 */
    private fun startOfWeek(day: LocalDate): Long =
        startOf(day.with(java.time.temporal.TemporalAdjusters.previousOrSame(java.time.DayOfWeek.MONDAY)))

    /**
     * 오늘 러닝·목표 보너스·이벤트로 번 SUP.
     *
     * 원장에서 아직 못 읽었으면 null 이다. 0 으로 두면 앱을 켜자마자 잠깐
     * "오늘 0 SUP"가 보였다가 바뀌는데, 그 순간에는 정말 못 번 것처럼 읽힌다.
     */
    val todayEarned: StateFlow<Double?> = today.flatMapLatest { rewardRepository.earnedSince(startOf(it)) }
        .map<Double, Double?> { it }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    /** 이번 주 러닝 수 · 거리. 읽기 전에는 null */
    val weekRuns: StateFlow<com.stepup.android.data.local.RunTotals?> =
        today.flatMapLatest { stepRepository.observeRunTotalsSince(startOfWeek(it)) }
            .map<com.stepup.android.data.local.RunTotals, com.stepup.android.data.local.RunTotals?> { it }
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    /** 최근 러닝 30개의 시작 자리(최근 것부터) — 홈 사진 카드의 "자주 달리는 곳". 경로가 없는 러닝은 뺀다 */
    val runStarts: StateFlow<List<Pair<Double, Double>>> = stepRepository.recentSessions(30)
        .map { sessions -> sessions.mapNotNull { s -> com.stepup.android.domain.RunTrack.first(s.track)?.let { it.lat to it.lng } } }
        .distinctUntilChanged()
        .flowOn(kotlinx.coroutines.Dispatchers.Default)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    /** 이번 주 번 SUP(원장 기준). 읽기 전에는 null */
    val weekEarned: StateFlow<Double?> = today.flatMapLatest { rewardRepository.earnedSince(startOfWeek(it)) }
        .map<Double, Double?> { it }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    /** 이 계정의 러닝 수(전체) — 첫 러닝 홈(시작·로그인·첫 사용 v1 시안 11)인지 가린다. 읽기 전에는 null */
    val runCount: StateFlow<Int?> = stepRepository.observeSessionCount()
        .map<Int, Int?> { it }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    /** 서버 확인을 기다리는(아직 올리지 못한) 러닝 수 — 오늘의 활동의 "확인 중" */
    val pendingUploads: StateFlow<Int> = stepRepository.observePendingUploadCount()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), 0)

    /** 오늘 시작한 러닝 세션의 운동 시간 합(초) */
    val todayRunSec: StateFlow<Long> = today.flatMapLatest { stepRepository.observeDurationSince(startOf(it)) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), 0L)

    // ── 오늘의 보상(서버 0118) ──────────────────────────────────

    /** 오늘 — 한국 날짜(서버 economy.game_day). 30초마다 보고 바뀌면 다시 낸다(자정에 어제 눈금이 남지 않게) */
    private val gameToday: StateFlow<LocalDate> = flow {
        while (true) {
            emit(com.stepup.android.domain.DailyRewards.gameDay(System.currentTimeMillis()))
            delay(30_000)
        }
    }.distinctUntilChanged()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), com.stepup.android.domain.DailyRewards.gameDay(System.currentTimeMillis()))

    /** 아직 서버가 답하지 않은 이 계정의 러닝(시작 시각 · 걸음) — 홈 막대의 옅은 "확인 중" 구간 */
    private val waitingRuns: Flow<List<Pair<Long, Int>>> = stepRepository.recentSessions(20)
        .map { sessions ->
            sessions.filter {
                it.uploadState == com.stepup.android.data.local.UploadState.PENDING.name ||
                    it.uploadState == com.stepup.android.data.local.UploadState.FAILED.name
            }.map { it.startedAt to it.steps }
        }
        .distinctUntilChanged()

    /** 지금 달리는 러닝(시작 시각 · 걸음) — 이 계정의 러닝일 때만 */
    private val liveRun: Flow<Pair<Long, Int>?> = com.stepup.android.service.WalkSessionService.state
        .map { s -> if (s.isActive && s.startedAt > 0) s.startedAt to s.steps else null }
        .distinctUntilChanged()

    /**
     * 홈 막대 — 서버가 오늘의 보상을 주지 않으면(예전 서버 · 로그인 전 · 서버 없는 빌드) null 이고 홈은 예전 모습(하루 전체 걸음)이다.
     * 숫자 · 눈금 · ✓ · "+SUP" 는 서버 상태에서만, 확인 중 걸음은 옅은 구간으로만(눈금을 채우지 않는다).
     */
    val dailyBar: StateFlow<com.stepup.android.domain.DailyBar?> = run {
        val state = shoeRules?.daily ?: kotlinx.coroutines.flow.MutableStateFlow(null)
        combine(state, gameToday, waitingRuns, liveRun) { daily, today, waiting, live ->
            daily?.let {
                val pending = com.stepup.android.domain.DailyRewardsBar.pendingToday(waiting, live?.first, live?.second ?: 0, today)
                com.stepup.android.domain.DailyRewardsBar.of(it, today, pending)
            }
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)
    }

    /**
     * 막대 아래 "어젯밤 러닝은 어제 걸음으로 셌어요" — 이 계정의 가장 최근 러닝이 오늘(한국 날짜) 자정 전에 시작해 자정 뒤에 끝났을 때, 오늘만
     * (DailyRewardsBar.overnightNote). 무효 · 거절된 러닝은 말하지 않는다
     */
    val overnightRun: StateFlow<Boolean> =
        combine(stepRepository.observeLatestSession(), gameToday) { latest, today ->
            latest != null && com.stepup.android.domain.DailyRewardsBar.overnightNote(
                startedAt = latest.startedAt, endedAt = latest.endedAt,
                counted = latest.verdict != "VOID" && latest.uploadState != com.stepup.android.data.local.UploadState.REJECTED.name,
                today = today,
            )
        }.distinctUntilChanged().stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), false)

    /** 오늘의 보상 서버 상태 그대로 — 오늘의 활동 시트의 이번 주 · 연속 */
    val daily: StateFlow<com.stepup.android.domain.DailyRewards?> =
        shoeRules?.daily ?: kotlinx.coroutines.flow.MutableStateFlow(null)

    /** 러닝이 올라간 뒤 새로 받은 보상 — 홈에 한 줄 */
    val rewardNews: StateFlow<com.stepup.android.domain.RewardNews?> =
        shoeRules?.news ?: kotlinx.coroutines.flow.MutableStateFlow(null)

    fun consumeRewardNews() {
        shoeRules?.consumeNews()
    }

    /** 기존 사용자에게 한 번 — "목표가 러닝 걸음 기준으로 바뀌었어요 · 다시 고르기" */
    val goalRuleNotice: StateFlow<Boolean> = prefs.goalRuleNotice
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), false)

    fun dismissGoalRuleNotice() {
        viewModelScope.launch { runCatching { prefs.dismissGoalRuleNotice() } }
    }

    private var dailyAskedAt = 0L

    /** 홈이 보일 때 · 한국 날짜가 바뀌었을 때 오늘의 보상을 다시 묻는다(30초에 한 번까지) */
    fun refreshDaily(force: Boolean = false) {
        val repo = shoeRules ?: return
        val now = System.currentTimeMillis()
        if (!force && now - dailyAskedAt < 30_000) return
        dailyAskedAt = now
        viewModelScope.launch {
            runCatching {
                repo.restore()
                if (signedIn()) repo.refreshBoxes()
            }
        }
    }

    init {
        // 자정(한국 시각)이 지나면 새 날의 상태를 묻는다 — 그 사이 막대는 "—"
        viewModelScope.launch { gameToday.drop(1).collect { refreshDaily(force = true) } }
    }

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
            get() = RewardEconomy.earnableSteps(
                energy, equipped?.energyEfficiency ?: 1.0,
                // 효율 포인트를 넣은 신발은 그 배수만큼 에너지를 더 쓴다(서버 0119)
                equipped?.let { com.stepup.android.data.repo.shoeRulesNow().energyMultOf(it) } ?: 1.0,
            )

        /**
         * 그 걸음을 다 걸었을 때 받는 SUP 의 예상 — 서버 규칙의 걸음당 SUP × 신은 신발의 효율 지수 ÷ 100(경제 4판). 부스터 · 인원 ·
         * 운은 넣지 않는다(실제 적립은 러닝을 마친 뒤 서버가 정한다)
         */
        val earnableSup: Double
            get() = com.stepup.android.data.repo.shoeRulesNow().let { rules ->
                earnableSteps * rules.pointsPerStep * (equipped?.earningMultiplierWith(rules.stats) ?: 1.0)
            }
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
            maxEnergy = RewardEconomy.maxEnergy(equipped?.level ?: wallet.sneakerLevel, com.stepup.android.data.repo.shoeRulesNow().stats),
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
                    shoeRules = ServiceLocator.shoeRules.takeIf { ServiceLocator.serverEconomyOn },
                    signedIn = { ServiceLocator.sessionHolder.isSignedIn() },
                )
            }
        }
    }
}
