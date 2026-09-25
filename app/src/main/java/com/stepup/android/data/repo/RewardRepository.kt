package com.stepup.android.data.repo

import com.stepup.android.data.local.BoostDao
import com.stepup.android.data.local.NotificationDao
import com.stepup.android.data.local.NotificationEntity
import com.stepup.android.data.local.NotificationType
import com.stepup.android.data.local.RewardDao
import com.stepup.android.data.local.RewardEntity
import com.stepup.android.data.local.RewardType
import com.stepup.android.data.local.SneakerDao
import com.stepup.android.data.prefs.UserPrefs
import com.stepup.android.domain.BoostType
import com.stepup.android.domain.Faction
import com.stepup.android.domain.Rarity
import com.stepup.android.domain.RewardEconomy
import com.stepup.android.domain.SessionReward
import com.stepup.android.domain.Sneaker
import java.time.LocalDate
import java.time.ZoneId
import kotlin.math.roundToInt
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch

/** SUP 포인트 원장, 에너지, 세션 정산을 관리한다. */
class RewardRepository(
    private val rewardDao: RewardDao,
    private val sneakerDao: SneakerDao,
    private val boostDao: BoostDao,
    private val notificationDao: NotificationDao,
    private val prefs: UserPrefs,
    private val recoverRunEnergy: suspend () -> Unit = {},
    /**
     * 서버 경제(A안). 켜져 있으면 폰은 적립 · 에너지를 스스로 적지 않는다 — 잔고는 서버 원장의
     * 사본이다(EconomySync). 서버 주소가 없는 빌드에서만 꺼진다.
     */
    private val serverEconomy: Boolean = false,
    /** 오늘 목표 보너스를 서버에 청구한다(goal_claim). 서버가 확인한 러닝 걸음으로 판정한다 */
    private val claimGoalOnServer: suspend () -> Unit = {},
) {

    val balance: Flow<Double> = rewardDao.observeBalance()
    val totals: Flow<com.stepup.android.data.local.RewardTotals> = rewardDao.observeTotals()

    val sneakerLevel: Flow<Int> = prefs.sneakerLevel

    fun ledger(limit: Int = 100): Flow<List<RewardEntity>> = rewardDao.observeLedger(limit)

    /** [fromMillis] 이후 러닝·보너스·이벤트로 번 SUP. 거래 대금은 빠진다. */
    fun earnedSince(fromMillis: Long): Flow<Double> = rewardDao.observeEarnedSince(fromMillis)

    /** [type] 한 종류가 [fromMillis] 이후 적힌 합 */
    fun sumOfTypeSince(type: String, fromMillis: Long): Flow<Double> =
        rewardDao.observeSumOfTypeSince(type, fromMillis)

    suspend fun balanceNow(): Double = rewardDao.balanceNow()

    // ── 범용 적립 / 차감 ─────────────────────────────────────

    suspend fun credit(type: String, amount: Double, description: String) {
        if (amount <= 0) return
        rewardDao.insert(
            RewardEntity(
                timestamp = System.currentTimeMillis(),
                type = type,
                amount = amount,
                description = description,
            )
        )
    }

    /** 잔액이 부족하면 false. 성공 시 음수 원장을 남긴다. 확인과 기록은 한 트랜잭션이다. */
    suspend fun spend(type: String, amount: Double, description: String): Boolean {
        if (amount <= 0) return true
        return rewardDao.spendIfEnough(
            RewardEntity(
                timestamp = System.currentTimeMillis(),
                type = type,
                amount = -amount,
                description = description,
            )
        )
    }

    /** 에너지 상한을 지금 신은 신발 레벨로 맞춘다. 에너지를 읽고 쓰기 전에 부른다. */
    suspend fun syncEnergyCap() {
        prefs.setEnergyCapLevel(sneakerDao.equippedNow()?.level)
    }

    /** 신발을 갈아 신거나 강화하면 에너지 상한도 따라가게 한다(화면 표시용). 앱이 한 번 부른다. */
    fun keepEnergyCapInSync(scope: CoroutineScope) {
        scope.launch {
            sneakerDao.observeEquipped().map { it?.level }.distinctUntilChanged()
                .collect { prefs.setEnergyCapLevel(it) }
        }
    }

    /** [day](epochDay) 하루 중 스트릭 보호막이 켜져 있던 때가 있었는가 */
    suspend fun streakShieldCovered(day: Long): Boolean {
        val zone = ZoneId.systemDefault()
        val from = LocalDate.ofEpochDay(day).atStartOfDay(zone).toInstant().toEpochMilli()
        val to = LocalDate.ofEpochDay(day + 1).atStartOfDay(zone).toInstant().toEpochMilli()
        return boostDao.activeDuring(BoostType.STREAK_SHIELD.id, from, to) != null
    }

    suspend fun notify(
        type: String,
        argText: String = "",
        argAmount: Double = 0.0,
        argExtra: String = "",
    ) {
        notificationDao.insert(
            NotificationEntity(
                timestamp = System.currentTimeMillis(),
                type = type,
                argText = argText,
                argAmount = argAmount,
                argExtra = argExtra,
                read = false,
                actioned = false,
            )
        )
    }

    // ── 일일 목표 ────────────────────────────────────────────

    /**
     * 일일 목표 달성 보너스 적립.
     *
     * @param streak 오늘까지의 연속 달성 일수 (1 이상)
     * @param dailyGoal 그 사람이 정한 목표 — 높게 잡을수록 보너스가 크다
     */
    suspend fun creditGoalBonus(streak: Int, dailyGoal: Int) {
        if (serverEconomy) {
            // 서버가 오늘 확인한 러닝 걸음으로 판정한다. 아직 모자라면 거절되고, 러닝이 올라간 뒤 다시 청구된다.
            claimGoalOnServer()
            return
        }
        val amount = RewardEconomy.goalBonus(streak, dailyGoal)
        credit(RewardType.BONUS_GOAL, amount, "일일 목표 달성 보너스 (연속 ${streak}일)")
        notify(NotificationType.GOAL_REACHED, argText = streak.toString(), argAmount = amount)
    }

    // ── 세션 정산 ────────────────────────────────────────────

    /**
     * 러닝 세션 종료 정산.
     * 착용 스니커즈의 효율·착화감, 파티 인원, 활성 XP 부스터를 모두 반영한다.
     */
    /**
     * 지금 신고 있는 스니커즈의 적립 부스트를 bps로 돌려준다. 1780 = +17.8%.
     *
     * 앱은 배율(1.178)로 계산하지만 컨트랙트와 어테스터는 bps 정수를 쓴다.
     * 소수 배율을 그대로 체인에 보낼 수 없으니 여기서 한 번만 변환해 둔다.
     * 신발이 없으면 레벨 기반 배율이 대신 쓰이고, 그것도 없으면 0이다.
     */
    suspend fun equippedBoostBps(): Int {
        val multiplier = sneakerDao.equippedNow()?.toDomain()?.earningMultiplier
            ?: RewardEconomy.sneakerMultiplier(prefs.sneakerLevel.first())
        return ((multiplier - 1.0) * 10_000).roundToInt().coerceAtLeast(0)
    }

    /** Calculation only; RunSettlementRepository commits the record and local effects together. */
    suspend fun calculateSessionReward(steps: Int, partySize: Int = 1, today: Long = LocalDate.now().toEpochDay()): SessionReward {
        syncEnergyCap()
        val energyRemaining = prefs.currentEnergy(today)
        val equipped = sneakerDao.equippedNow()?.toDomain()

        val earningMultiplier = equipped?.earningMultiplier
            ?: RewardEconomy.sneakerMultiplier(prefs.sneakerLevel.first())
        val energyEfficiency = equipped?.energyEfficiency ?: 1.0

        val now = System.currentTimeMillis()
        val xpBoosted = boostDao.activeOf(BoostType.XP_BOOSTER.id, now) != null
        val boostMultiplier = if (xpBoosted) RewardEconomy.XP_BOOST_MULTIPLIER else 1.0

        val reward = RewardEconomy.sessionReward(
            walkedSteps = steps,
            energyRemaining = energyRemaining,
            earningMultiplier = earningMultiplier,
            energyEfficiency = energyEfficiency,
            partyMultiplier = RewardEconomy.partyMultiplier(partySize),
            boostMultiplier = boostMultiplier,
        )

        return reward
    }

    /**
     * 백그라운드 걸음 정산.
     *
     * 러닝 세션을 켜지 않고 걸은 걸음도 적립한다. 사람은 하루 종일 앱을 열어두지
     * 않으므로, 세션 중에만 적립하면 실제로 움직인 대부분이 버려진다.
     *
     * 세션 적립과 다른 점은 두 가지다.
     *  - 파티런 배율이 붙지 않는다. 같이 뛴 게 아니기 때문이다.
     *  - GPS 속도 검증을 통과하지 않는다. 세션이 아니면 GPS를 켜지 않는다.
     *    대신 [RewardEconomy]의 에너지 상한이 그대로 걸리고, 케이던스가 사람
     *    범위를 벗어나면(폰 흔들기) 호출부에서 걸러진다.
     *
     * 알림은 띄우지 않는다. 하루에 열 번 넘게 울리면 그건 알림이 아니라 소음이다.
     */
    suspend fun settleBackground(steps: Int): SessionReward {
        // 서버 경제에서는 GPS 러닝으로 확인된 걸음만 적립된다. 평소 걸음은 적립하지 않는다.
        if (steps <= 0 || serverEconomy) return SessionReward(0, 0.0, 0.0)
        recoverRunEnergy()
        val today = LocalDate.now().toEpochDay()
        syncEnergyCap()
        val energyRemaining = prefs.currentEnergy(today)
        val equipped = sneakerDao.equippedNow()?.toDomain()

        val earningMultiplier = equipped?.earningMultiplier
            ?: RewardEconomy.sneakerMultiplier(prefs.sneakerLevel.first())
        val energyEfficiency = equipped?.energyEfficiency ?: 1.0

        val now = System.currentTimeMillis()
        val xpBoosted = boostDao.activeOf(BoostType.XP_BOOSTER.id, now) != null
        val boostMultiplier = if (xpBoosted) RewardEconomy.XP_BOOST_MULTIPLIER else 1.0

        val reward = RewardEconomy.sessionReward(
            walkedSteps = steps,
            energyRemaining = energyRemaining,
            earningMultiplier = earningMultiplier,
            energyEfficiency = energyEfficiency,
            boostMultiplier = boostMultiplier,
        )

        if (reward.points > 0) {
            credit(RewardType.EARN_WALK, reward.points, "일상 걸음 적립 (${reward.rewardedSteps}보)")
        }
        if (reward.energyUsed > 0) {
            prefs.consumeEnergy(today, reward.energyUsed)
        }
        return reward
    }

    /** 레거시 레벨 기반 업그레이드 (스니커즈 인벤토리가 비어 있을 때의 폴백) */
    suspend fun upgradeSneaker(): Boolean {
        val level = prefs.sneakerLevel.first()
        val cost = RewardEconomy.upgradeCost(level)
        if (!spend(RewardType.SPEND_UPGRADE, cost, "스니커즈 Lv.$level → Lv.${level + 1}")) return false
        prefs.setSneakerLevel(level + 1)
        return true
    }
}

/** Room 엔티티 → 도메인 모델 */
fun com.stepup.android.data.local.SneakerEntity.toDomain(): Sneaker {
    val r = Rarity.of(rarity)
    return Sneaker(
        id = id,
        faction = Faction.of(factionId),
        rarity = r,
        variant = variant.coerceIn(0, r.variantCount - 1),
        level = level,
        mintNumber = mintNumber,
        luck = luck,
        comfort = comfort,
        durability = durability,
        equipped = equipped,
        acquiredAt = acquiredAt,
        server = if (origin.isBlank()) null else com.stepup.android.domain.ServerStats(
            origin = origin,
            efficiencyBps = efficiencyBps,
            comfortBps = comfortBps,
            durabilityPts = durabilityPts,
            maxLevel = maxLevel,
            status = status,
            chainState = chainState,
            canWithdraw = canWithdraw,
            upgradeCost = serverUpgradeCost,
            repairCostPerPoint = repairCostPerPoint,
            genesisNo = genesisNo,
        ),
    )
}

/** 도메인 모델 → Room 엔티티 */
fun Sneaker.toEntity(): com.stepup.android.data.local.SneakerEntity =
    com.stepup.android.data.local.SneakerEntity(
        id = id,
        factionId = faction.id,
        rarity = rarity.id,
        variant = variant,
        level = level,
        mintNumber = mintNumber,
        luck = luck,
        comfort = comfort,
        durability = durability,
        equipped = equipped,
        acquiredAt = acquiredAt,
        origin = server?.origin.orEmpty(),
        efficiencyBps = server?.efficiencyBps ?: 0,
        comfortBps = server?.comfortBps ?: 0,
        durabilityPts = server?.durabilityPts ?: 100.0,
        maxLevel = server?.maxLevel ?: 0,
        status = server?.status ?: "OWNED",
        chainState = server?.chainState ?: "APP",
        canWithdraw = server?.canWithdraw ?: false,
        serverUpgradeCost = server?.upgradeCost ?: 0.0,
        repairCostPerPoint = server?.repairCostPerPoint ?: 0.0,
        genesisNo = server?.genesisNo ?: 0,
    )
