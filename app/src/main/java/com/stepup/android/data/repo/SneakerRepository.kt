package com.stepup.android.data.repo

import com.stepup.android.data.local.NotificationType
import com.stepup.android.data.local.RewardType
import com.stepup.android.data.local.AppDatabase
import com.stepup.android.data.local.SneakerEntity
import androidx.room.withTransaction
import com.stepup.android.domain.Faction
import com.stepup.android.domain.RewardEconomy
import com.stepup.android.domain.ShoeCatalog
import com.stepup.android.domain.Sneaker
import com.stepup.android.domain.SneakerMint
import com.stepup.android.domain.TOTAL_COLLECTION
import kotlin.random.Random
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

/**
 * 지금 러닝이 켜져 있는가 — 켜져 있으면 신발을 바꾸지 않는다. 서버(record_session)는 러닝을 올리는 때 신고 있는 신발로
 * 내구도 · 적립을 매겨, 달리는 중에 바꾸면 달린 신발이 아닌 신발이 닳는다(2026-11-03 임시 막음). 러닝 전 점검의 신발 교체는
 * 러닝이 켜지기 전이라 그대로 된다.
 */
fun shoeChangeBlockedByRun(): Boolean =
    runCatching { com.stepup.android.service.WalkSessionService.state.value.isActive }.getOrDefault(false)

/** [SneakerRepository.equipOnServer] 가 러닝 중이라 바꾸지 않았을 때의 까닭(기록용 — 화면 글자가 아니다) */
const val RUN_ACTIVE_REASON = "run_active"

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

    /** 서버 요청 뒤 목록을 다시 받는다. 받지 못해도 요청 자체는 이미 끝났다. 다시 받았으면 true */
    private suspend fun resync(): Boolean = sync?.refresh() is com.stepup.android.data.remote.ServerResult.Ok

    /**
     * 요청 결과가 성공이거나, 응답을 못 받았을 때(보냈는지 모름)는 서버 값을 다시 받는다 —
     * 응답만 잃고 서버에서는 이미 처리됐으면 잔고 · 신발이 그걸 보여 줘야 다시 누르지 않는다.
     * [afterRejection] 이면 서버가 거절했을 때도 다시 받는다 — 폰 사본이 낡아 거절됐으면(이미 나눈 포인트 · 이미 가득 찬 내구도 ·
     * 다른 폰에서 바꾼 착용) 낡은 사본으로 같은 거절을 되풀이하지 않게(2026-11-17 점검 #17). 로그인이 필요하면 받을 수 없다.
     */
    private suspend fun afterWrite(outcome: EconomyOutcome, afterRejection: Boolean = false): EconomyOutcome =
        afterWriteChecked(outcome, afterRejection).first

    /** [afterWrite] 에 더해 실제로 다시 받았는가(화면이 "최신 값으로 다시 불러왔어요"라고 써도 되는가) */
    private suspend fun afterWriteChecked(outcome: EconomyOutcome, afterRejection: Boolean): Pair<EconomyOutcome, Boolean> {
        val again = when (outcome) {
            EconomyOutcome.Ok, EconomyOutcome.Offline -> true
            EconomyOutcome.SignInRequired -> false
            else -> afterRejection
        }
        return outcome to (again && resync())
    }

    /**
     * 착용. 서버 신발이면 서버에서, 폰에만 있던 옛 신발(로그인 전)이면 폰에서.
     * 달리는 중에는 바꾸지 않는다([shoeChangeBlockedByRun]) — 화면이 먼저 막고 까닭을 말한다. 여기는 마지막 울타리다.
     */
    suspend fun equipOnServer(id: Long): EconomyOutcome {
        if (shoeChangeBlockedByRun()) return EconomyOutcome.Rejected(RUN_ACTIVE_REASON)
        val local = sneakerDao.byId(id) ?: return EconomyOutcome.Rejected("없는 신발입니다")
        val api = economy
        if (!serverEconomy || api == null || local.origin.isBlank()) {
            return if (sneakerDao.equipExclusively(id) > 0) EconomyOutcome.Ok else EconomyOutcome.Rejected("")
        }
        return afterWrite(api.equip(local.serverId.takeIf { it != 0L } ?: id).toEconomyOutcome(), afterRejection = true)
    }

    /** 강화 (서버). 새 레벨이 적용된 신발을 함께 돌려준다. */
    suspend fun upgradeOnServer(id: Long): Pair<EconomyOutcome, Sneaker?> {
        val api = economy ?: return upgrade(id).let { (if (it != null) EconomyOutcome.Ok else EconomyOutcome.NotEnoughBalance) to it }
        val local = sneakerDao.byId(id) ?: return EconomyOutcome.Rejected("없는 신발입니다") to null
        if (local.origin.isBlank()) return EconomyOutcome.SignInRequired to null
        val outcome = afterWrite(
            payOnce("UPGRADE:$id") { key -> api.upgrade(local.serverId.takeIf { it != 0L } ?: id, key).toEconomyOutcome() },
        )
        if (outcome != EconomyOutcome.Ok) return outcome to null
        return outcome to sneakerDao.byId(id)?.toDomain()
    }

    /** 수리 — 내구도를 가득 채운다(무료 수리가 남아 있으면 0 SUP, 0116). 치른 SUP 는 소각된다. */
    suspend fun repairOnServer(id: Long): EconomyOutcome = repairChecked(id, points = null, expected = null).outcome

    /**
     * 수리(경제 4판, 서버 0116 sneaker_repair_v2). [points] 는 고칠 내구도(가진 만큼 수리 — 비우면 견적에 보인 만큼, 곧 가득),
     * [expected] 는 화면이 보인 수리비 — 서버 값이 다르면 고치지 않고 새 값을 돌려준다([RepairReply.quoteChanged], 치르지 않았다).
     * 이틀에 한 번 무료 수리가 남아 있으면 서버가 [points] 와 상관없이 한 켤레를 가득 0 SUP 로 고친다.
     * 서버에 새 함수가 없으면(PGRST202 — 예전 서버) 예전 수리(sneaker_repair)로 보낸다 — 그때는 [expected] 를 맞대어 보지 못한다.
     */
    suspend fun repairChecked(id: Long, points: Int?, expected: Double?): RepairReply {
        val api = economy ?: return RepairReply(EconomyOutcome.SignInRequired)
        val local = sneakerDao.byId(id) ?: return RepairReply(EconomyOutcome.Rejected("없는 신발입니다"))
        if (local.origin.isBlank()) return RepairReply(EconomyOutcome.SignInRequired)
        // 견적에 보인 만큼만 고친다(최대 − 남은 내구도, 서버 p_points) — 견적과 실행 사이에 러닝이 올라가 더 닳았어도 보인 값보다
        // 많이 치르지 않는다. 최대 내구도를 보내지 않는 예전 서버(0)는 예전처럼 가득(null)
        val full = if (local.durabilityMax > 0) local.durabilityMax - local.durabilityNow.coerceIn(0, local.durabilityMax) else null
        val send = when {
            full == null -> null
            points == null -> full
            // 가진 만큼 수리 — 가득보다 많이 보내지 않는다
            else -> points.coerceIn(0, full)
        }
        val serverId = local.serverId.takeIf { it != 0L } ?: id
        var reply: com.stepup.android.domain.RepairV2Result? = null
        val outcome = payOnce(repairAction(local, send.takeIf { it != full }, expected)) { key ->
            val v2 = api.repairV2(serverId, points = send, request = key, expected = expected)
            if (v2 is com.stepup.android.data.remote.ServerResult.Rejected &&
                ("Could not find the function" in v2.reason || "PGRST202" in v2.reason)
            ) {
                api.repair(serverId, points = send, request = key).toEconomyOutcome()
            } else when (v2) {
                is com.stepup.android.data.remote.ServerResult.Ok -> {
                    reply = v2.value
                    when {
                        v2.value.ok -> EconomyOutcome.Ok
                        v2.value.code == QUOTE_CHANGED -> EconomyOutcome.Rejected(QUOTE_CHANGED)
                        else -> EconomyOutcome.Rejected(v2.value.code.orEmpty())
                    }
                }
                else -> v2.toEconomyOutcome()
            }
        }
        // 바뀐 값(QUOTE_CHANGED)이면 서버 값을 다시 받는다 — 신발 목록의 수리 단가 · 무료 여부가 새 값이 되어 다시 확인받을 수 있다
        val after = afterWrite(outcome, afterRejection = true)
        val got = reply
        return RepairReply(
            outcome = after,
            quoteChanged = got?.takeIf { !it.ok && it.code == QUOTE_CHANGED },
            result = got?.takeIf { it.ok },
        )
    }

    /**
     * 스탯 포인트 나누기(0059) — 나눈 뒤의 합계(효율 · 착화감 · 내구도)를 보낸다. 늘리기만 된다.
     * 응답을 잃은 같은 나누기를 다시 보내면 같은 번호라 서버가 한 번만 적는다. 값을 바꿔 보내면 새 번호다.
     */
    suspend fun setPointsOnServer(id: Long, efficiency: Int, comfort: Int, durability: Int): EconomyOutcome =
        setPointsOnServerChecked(id, efficiency, comfort, durability).first

    /**
     * [setPointsOnServer] 에 더해 그 뒤 서버 값을 실제로 다시 받았는가 — 거절됐을 때 시트가 "최신 값으로 다시 불러왔어요"는
     * 다시 받았을 때만 쓴다.
     */
    suspend fun setPointsOnServerChecked(id: Long, efficiency: Int, comfort: Int, durability: Int): Pair<EconomyOutcome, Boolean> {
        val api = economy ?: return EconomyOutcome.SignInRequired to false
        val local = sneakerDao.byId(id) ?: return EconomyOutcome.Rejected("없는 신발입니다") to false
        if (local.origin.isBlank()) return EconomyOutcome.SignInRequired to false
        return afterWriteChecked(
            payOnce("POINTS:$id:$efficiency/$comfort/$durability") { key ->
                api.setPoints(local.serverId.takeIf { it != 0L } ?: id, efficiency, comfort, durability, key).toEconomyOutcome()
            },
            afterRejection = true,
        )
    }

    /**
     * 값을 치르는 요청에 번호를 붙인다. 응답을 잃은(연결이 끊긴) 요청을 다시 보내면 같은 번호로 보내 서버가 한 번만
     * 치른다(0057). 답을 받으면 번호를 버린다 — 다음 강화 · 수리는 새 요청이다(답을 잃은 번호도 10분 뒤에는 버린다).
     */
    private suspend fun payOnce(action: String, call: suspend (String) -> EconomyOutcome): EconomyOutcome {
        val key = requestKeys.keyFor(action)
        return call(key).also { if (it != EconomyOutcome.Offline) requestKeys.done(action, key) }
    }

    private val requestKeys = RequestKeys()

    /**
     * 뽑기 (서버, 2026-09-27 부터 모두 무료). [kind] 칸의 기회를 한 번 쓴다 — 무료는 오늘 몫 → 가입 선물,
     * 상급은 지갑 선물 → 러닝으로 받은 기회 순서로 서버가 고른다. SUP 는 나가지 않는다.
     * 결과 신발은 서버가 정한 것을 다시 받아 와 돌려준다. 뽑기는 됐는데 목록을 아직 못 받았으면
     * (Ok, null) — 실패가 아니다. 화면은 "뽑았어요, 불러오는 중"이라고 해야 다시 누르지 않는다.
     */
    suspend fun drawOnServer(kind: com.stepup.android.domain.DrawKind): Pair<EconomyOutcome, Sneaker?> {
        // 서버 없이는 뽑지 않는다 — 폰이 수를 주거나 줄이지 않는다
        val api = economy ?: return EconomyOutcome.SignInRequired to null
        val result = when (kind) {
            com.stepup.android.domain.DrawKind.FREE -> api.drawFree()
            com.stepup.android.domain.DrawKind.PREMIUM -> api.drawPremium()
        }
        val newId = when (result) {
            is com.stepup.android.data.remote.ServerResult.Ok -> result.value
            else -> {
                val outcome = result.toEconomyOutcome()
                if (outcome != EconomyOutcome.SignInRequired) resync()
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

    /** 도감 진행도 — 지금 뽑기에서 나오는 새 도감(0045, 70종) 중 가진 모델 수 */
    val collectionProgress: Flow<Pair<Int, Int>> = inventory.map { list ->
        list.mapNotNull { it.catalogModel?.id }.toSet().size to ShoeCatalog.models.size
    }

    /** 속성별 수집 현황 — "불 3/13". 속성은 예전 52종에만 있다(새 도감 신발은 세지 않는다) */
    val factionProgress: Flow<Map<Faction, Int>> = inventory.map { list ->
        Faction.entries.associateWith { f ->
            list.filter { it.catalogModel == null && it.faction == f }.map { it.slotKey }.toSet().size
        }
    }
}

/**
 * 수리 요청 번호의 이름 — 고치기 전 내구도(남은 · 최대 · 남은 비율)를 넣는다. 같은 상태에서 다시 누르면 같은 번호라 서버가
 * 한 번만 치른다. 응답을 잃은 수리가 실제로 됐고 그 뒤 달려 다시 닳았으면 새 수리다 — 신발 번호만 쓰면 그 10분 동안의 새 수리가
 * 옛 영수증을 받아, 고치지도 치르지도 않은 채 "됐어요"로 보이고 결과 확인에 머물렀다. 남은 비율은 최대 내구도를 보내지 않는
 * 예전 서버(남은 · 최대가 0)에서도 나뉘게 한다.
 */
internal fun repairAction(shoe: SneakerEntity, points: Int? = null, expected: Double? = null): String =
    "REPAIR:${shoe.id}:${shoe.durabilityNow}/${shoe.durabilityMax}/${shoe.durabilityPts}" +
        // 가진 만큼 수리 · 확인한 금액이 다르면 다른 요청이다 — 응답을 잃은 부분 수리 뒤 가득 수리를 보내면 옛 영수증을 받지 않게
        (points?.let { ":p$it" } ?: "") + (expected?.let { ":e$it" } ?: "")

/** 서버가 보인 값과 실제 수리비가 달라 고치지 않았다(0116 sneaker_repair_v2) */
const val QUOTE_CHANGED = "QUOTE_CHANGED"

/**
 * 수리 한 번의 대답 — [outcome] 은 지금까지의 결말(성공 · 거절 · 연결 끊김 …). [quoteChanged] 면 서버 수리비가 보인 값과 달라
 * 고치지도 치르지도 않았다(새 값 · 무료 여부 · 고칠 양). [result] 는 서버가 고친 결과(새 서버만 — 예전 서버 · 같은 요청의 다시 받기는 일부만).
 */
data class RepairReply(
    val outcome: EconomyOutcome,
    val quoteChanged: com.stepup.android.domain.RepairV2Result? = null,
    val result: com.stepup.android.domain.RepairV2Result? = null,
)
