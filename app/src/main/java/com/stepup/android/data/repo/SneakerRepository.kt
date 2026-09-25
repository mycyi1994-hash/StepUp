package com.stepup.android.data.repo

import com.stepup.android.data.local.NotificationType
import com.stepup.android.data.local.RewardType
import com.stepup.android.data.local.AppDatabase
import androidx.room.withTransaction
import com.stepup.android.domain.Faction
import com.stepup.android.domain.RewardEconomy
import com.stepup.android.domain.Sneaker
import com.stepup.android.domain.SneakerMint
import com.stepup.android.domain.TOTAL_COLLECTION
import kotlin.random.Random
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

/**
 * 스니커즈 인벤토리 — 뽑기 · 착용 · 강화 · 수리.
 *
 * 서버 경제가 연결돼 있으면([economy] · [sync]) 모든 변경은 서버 함수가 하고, 폰의 목록은
 * 그 뒤 서버에서 다시 받아 온다(A안). 서버 주소가 없는 빌드에서만 예전처럼 폰 안에서 한다.
 */
class SneakerRepository(
    private val database: AppDatabase,
    private val rewardRepository: RewardRepository,
    private val economy: com.stepup.android.data.remote.EconomyApi? = null,
    private val sync: EconomySync? = null,
    private val prefs: com.stepup.android.data.prefs.UserPrefs? = null,
) {
    private val sneakerDao = database.sneakerDao()

    private val serverEconomy: Boolean get() = economy != null && sync != null

    /** 남은 무료 뽑기 — 서버 경제일 때만 0 이 아닐 수 있다 */
    val freeDrawsLeft: Flow<Int> = prefs?.freeDrawsLeft ?: kotlinx.coroutines.flow.flowOf(0)

    /** 지갑 연결로 받은 보너스 뽑기 — 지갑 페이지에서 쓴다 */
    val bonusDrawsLeft: Flow<Int> = prefs?.bonusDrawsLeft ?: kotlinx.coroutines.flow.flowOf(0)

    /** 서버 요청 뒤 목록을 다시 받는다. 받지 못해도 요청 자체는 이미 끝났다. */
    private suspend fun resync() {
        sync?.refresh()
    }

    /**
     * 요청 결과가 성공이거나, 응답을 못 받았을 때(보냈는지 모름)는 서버 값을 다시 받는다 —
     * 응답만 잃고 서버에서는 이미 처리됐으면 잔고 · 신발이 그걸 보여 줘야 다시 누르지 않는다.
     */
    private suspend fun afterWrite(outcome: EconomyOutcome): EconomyOutcome {
        if (outcome == EconomyOutcome.Ok || outcome == EconomyOutcome.Offline) resync()
        return outcome
    }

    /** 착용. 서버 신발이면 서버에서, 폰에만 있던 옛 신발(로그인 전)이면 폰에서. */
    suspend fun equipOnServer(id: Long): EconomyOutcome {
        val local = sneakerDao.byId(id) ?: return EconomyOutcome.Rejected("없는 신발입니다")
        val api = economy
        if (!serverEconomy || api == null || local.origin.isBlank()) {
            return if (sneakerDao.equipExclusively(id) > 0) EconomyOutcome.Ok else EconomyOutcome.Rejected("")
        }
        return afterWrite(api.equip(local.serverId.takeIf { it != 0L } ?: id).toEconomyOutcome())
    }

    /** 강화 (서버). 새 레벨이 적용된 신발을 함께 돌려준다. */
    suspend fun upgradeOnServer(id: Long): Pair<EconomyOutcome, Sneaker?> {
        val api = economy ?: return upgrade(id).let { (if (it != null) EconomyOutcome.Ok else EconomyOutcome.NotEnoughBalance) to it }
        val local = sneakerDao.byId(id) ?: return EconomyOutcome.Rejected("없는 신발입니다") to null
        if (local.origin.isBlank()) return EconomyOutcome.SignInRequired to null
        val outcome = afterWrite(api.upgrade(local.serverId.takeIf { it != 0L } ?: id).toEconomyOutcome())
        if (outcome != EconomyOutcome.Ok) return outcome to null
        return outcome to sneakerDao.byId(id)?.toDomain()
    }

    /** 수리 — 내구도를 가득 채운다. 치른 SUP 는 소각된다. */
    suspend fun repairOnServer(id: Long): EconomyOutcome {
        val api = economy ?: return EconomyOutcome.SignInRequired
        val local = sneakerDao.byId(id) ?: return EconomyOutcome.Rejected("없는 신발입니다")
        if (local.origin.isBlank()) return EconomyOutcome.SignInRequired
        return afterWrite(api.repair(local.serverId.takeIf { it != 0L } ?: id).toEconomyOutcome())
    }

    /**
     * 뽑기 (서버). 무료 뽑기가 남았으면 그것부터 쓰고, 없으면 SUP 로 뽑는다.
     * 결과 신발은 서버가 정한 것을 다시 받아 와 돌려준다. 뽑기는 됐는데 목록을 아직 못 받았으면
     * (Ok, null) — 실패가 아니다. 화면은 "뽑았어요, 불러오는 중"이라고 해야 다시 누르지 않는다.
     */
    suspend fun drawOnServer(): Pair<EconomyOutcome, Sneaker?> {
        val api = economy ?: return mint().let { (if (it != null) EconomyOutcome.Ok else EconomyOutcome.NotEnoughBalance) to it }
        val free = prefs?.freeDrawsLeft?.first() ?: 0
        val result = if (free > 0) api.drawFree() else api.drawPaid()
        // 다른 기기에서 무료 뽑기를 다 썼으면 SUP 뽑기로 넘어가지 않는다 — 돈이 나가는 일은 한 번 더 누르게 한다
        val newId = when (result) {
            is com.stepup.android.data.remote.ServerResult.Ok -> result.value
            else -> {
                val outcome = result.toEconomyOutcome()
                if (outcome == EconomyOutcome.NoFreeDraws || outcome == EconomyOutcome.Offline) resync()
                return outcome to null
            }
        }
        resync()
        return EconomyOutcome.Ok to sneakerDao.byId(newId)?.toDomain()
    }

    val inventory: Flow<List<Sneaker>> =
        sneakerDao.observeAll().map { list -> list.map { it.toDomain() } }

    val equipped: Flow<Sneaker?> =
        sneakerDao.observeEquipped().map { it?.toDomain() }

    val ownedCount: Flow<Int> = sneakerDao.observeCount()

    /** 첫 실행 시 스타터 스니커즈를 지급한다. 이미 있으면 아무것도 하지 않는다. */
    suspend fun ensureStarter() = database.withTransaction {
        if (sneakerDao.count() > 0) return@withTransaction
        sneakerDao.insert(SneakerMint.starter().toEntity())
    }

    suspend fun equip(id: Long): Boolean = sneakerDao.equipExclusively(id) > 0

    /** 강화. 잔액 부족이거나 최대 레벨이면 null. */
    suspend fun upgrade(id: Long): Sneaker? = database.withTransaction {
        val entity = sneakerDao.byId(id) ?: return@withTransaction null
        val current = entity.toDomain()
        if (!current.canUpgrade) return@withTransaction null
        val cost = current.upgradeCost
        val ok = rewardRepository.spend(
            RewardType.SPEND_UPGRADE,
            cost,
            "${current.displayName} Lv.${current.level} → Lv.${current.level + 1}",
        )
        if (!ok) return@withTransaction null
        val upgraded = entity.copy(
            level = entity.level + 1,
            // 강화하면 스탯도 소폭 상승한다
            comfort = entity.comfort + 0.02,
            luck = entity.luck + 0.02,
        )
        sneakerDao.update(upgraded)
        rewardRepository.notify(
            NotificationType.SNEAKER_UPGRADED,
            current.slotKey,
            (entity.level + 1).toDouble(),
        )
        upgraded.toDomain()
    }

    /**
     * 새 스니커즈 민팅. 착용 중인 스니커즈의 행운 스탯이 상위 희귀도 확률을 올린다.
     * 잔액 부족이면 null.
     */
    suspend fun mint(): Sneaker? = database.withTransaction {
        val cost = RewardEconomy.MINT_COST
        val luck = sneakerDao.equippedNow()?.luck?.minus(1.0)?.coerceAtLeast(0.0) ?: 0.0
        if (!rewardRepository.spend(RewardType.SPEND_MINT, cost, "스니커즈 민팅")) return@withTransaction null

        val mintNumber = sneakerDao.maxMintNumber() + 1
        val minted = SneakerMint.mint(
            random = Random(System.nanoTime()),
            mintNumber = mintNumber,
            luck = luck,
        )
        val newId = sneakerDao.insert(minted.toEntity())
        rewardRepository.notify(
            NotificationType.SNEAKER_MINTED,
            minted.slotKey,
            minted.rarity.ordinal.toDouble(),
        )
        minted.copy(id = newId)
    }

    /** 도감 진행도 — 보유한 (속성 × 등급 × 변형) 조합 수 */
    val collectionProgress: Flow<Pair<Int, Int>> = inventory.map { list ->
        list.map { it.slotKey }.toSet().size to TOTAL_COLLECTION
    }

    /** 속성별 수집 현황 — 도감 탭에서 "불 3/11" 처럼 쓴다 */
    val factionProgress: Flow<Map<Faction, Int>> = inventory.map { list ->
        Faction.entries.associateWith { f ->
            list.filter { it.faction == f }.map { it.slotKey }.toSet().size
        }
    }
}
