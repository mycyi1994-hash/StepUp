package com.stepup.android.data.repo

import androidx.room.withTransaction
import com.stepup.android.data.local.AppDatabase
import com.stepup.android.data.local.BoostEntity
import com.stepup.android.data.local.RewardEntity
import com.stepup.android.data.local.SneakerEntity
import com.stepup.android.data.prefs.UserPrefs
import com.stepup.android.data.remote.EconomyApi
import com.stepup.android.data.remote.LedgerEntryRow
import com.stepup.android.data.remote.MarketApi
import com.stepup.android.data.remote.ServerResult
import com.stepup.android.data.remote.ServerSneakerRow
import com.stepup.android.data.remote.SessionHolder
import com.stepup.android.data.remote.TokenResult
import java.time.Instant
import java.time.LocalDate
import java.time.OffsetDateTime
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

/** 마지막으로 서버와 맞춘 결과 — 화면이 "서버 값인지"를 말할 때 쓴다 */
enum class EconomySyncState { UNKNOWN, SYNCED, SIGNED_OUT, OFFLINE }

/**
 * 폰의 잔고 · 신발 · 부스터 · 에너지를 서버 값으로 맞춘다 (A안: 서버가 정본).
 *
 * 폰의 표(rewards · sneakers · boosts)는 서버의 사본이다. 맞출 때마다 **통째로** 갈아
 * 끼운다 — 조금씩 더하면 한 번 어긋난 것이 계속 남는다. 서버에서 모두 받아 온 뒤에만
 * 갈아 끼우므로, 중간에 끊겨도 반쯤 비는 일은 없다.
 *
 * 폰에만 있던 옛 신발은 로그인한 계정으로 처음 맞출 때 한 번 서버에 올린다(IMPORT).
 * 서버는 그 값을 믿지 않고 일반 1레벨로 친다 — 기념으로 남는다.
 */
class EconomySync(
    private val api: EconomyApi,
    private val market: MarketApi,
    private val db: AppDatabase,
    private val prefs: UserPrefs,
    private val sessions: SessionHolder,
    private val now: () -> Long = System::currentTimeMillis,
) {
    private val lock = Mutex()
    private var bootstrapped: String? = null

    private val _state = MutableStateFlow(EconomySyncState.UNKNOWN)
    val state: StateFlow<EconomySyncState> = _state

    suspend fun refresh(): ServerResult<Unit> = lock.withLock {
        val result = refreshLocked()
        _state.value = when (result) {
            is ServerResult.Ok -> EconomySyncState.SYNCED
            is ServerResult.SignInRequired -> EconomySyncState.SIGNED_OUT
            else -> EconomySyncState.OFFLINE
        }
        result
    }

    private suspend fun refreshLocked(): ServerResult<Unit> {
        if (!api.isConfigured) return ServerResult.Retry("서버 주소가 설정되지 않았습니다")
        val userId = when (val t = sessions.accessToken()) {
            is TokenResult.Ok -> t.userId ?: return ServerResult.SignInRequired("로그인이 필요합니다")
            is TokenResult.Unavailable -> return ServerResult.Retry(t.reason)
            is TokenResult.SignInRequired -> return ServerResult.SignInRequired(t.reason)
        }

        // 다른 계정으로 다시 로그인했다 — 앞 계정의 사본(잔고 · 신발 · 부스터 · 받은 도전 · 알림)을 먼저 지운다.
        // 안 지우면 새 계정이 앞 계정의 잔고를 보고, 앞 계정이 받은 도전을 "이미 받음"으로 못 받는다.
        val owner = prefs.economyOwner()
        if (owner != null && owner != userId) {
            clearCopy()
            db.notificationDao().clear()
        }
        if (owner != userId) prefs.setEconomyOwner(userId)

        if (bootstrapped != userId) {
            api.bootstrap().failure()?.let { return it }
            // 폰의 하루 목표를 서버에 맞춘다 — 목표 보너스를 서버가 이 값으로 판정한다(0030).
            // 목표를 바꿀 때 보내지 못했어도(연결 없음) 여기서 따라간다. 실패해도 동기화는 계속한다.
            api.setDailyGoal(prefs.dailyGoal.first())
            bootstrapped = userId
        }
        importLegacy(userId)?.let { return it }

        val economy = when (val r = api.economy()) { is ServerResult.Ok -> r.value; else -> return r.cast() }
        val sneakers = when (val r = api.sneakers()) { is ServerResult.Ok -> r.value; else -> return r.cast() }
        val grants = when (val r = api.drawGrants()) { is ServerResult.Ok -> r.value; else -> return r.cast() }
        val boosts = when (val r = api.boosts(Instant.ofEpochMilli(now() - BOOST_KEEP_MILLIS).toString())) {
            is ServerResult.Ok -> r.value
            else -> return r.cast()
        }
        val ledger = mutableListOf<LedgerEntryRow>()
        var after = 0L
        var complete = false
        for (pageNo in 0 until MAX_LEDGER_PAGES) {
            val page = when (val r = api.ledgerPage(after, LEDGER_PAGE)) { is ServerResult.Ok -> r.value; else -> return r.cast() }
            ledger += page
            if (page.size < LEDGER_PAGE) {
                complete = true
                break
            }
            after = page.last().id
        }

        // 받아 오는 사이에 다른 계정으로 바뀌었으면 쓰지 않는다 — 두 계정의 잔고 · 신발이 섞이지 않게.
        // 새 계정은 로그인하면서 다시 맞춘다.
        val stillSame = (sessions.accessToken() as? TokenResult.Ok)?.userId == userId
        if (!stillSame) return ServerResult.Retry("계정이 바뀌어 다시 맞춥니다")

        val previous = db.sneakerDao().allNow().associateBy { it.serverId }
        db.withTransaction {
            db.sneakerDao().deleteAll()
            db.sneakerDao().insertAll(sneakers.map { it.toEntity(previous[it.id]?.acquiredAt ?: now()) })
            db.rewardDao().deleteAll()
            db.rewardDao().insertAll(ledgerRows(ledger, economy.balance, complete))
            db.boostDao().deleteAll()
            db.boostDao().insertAll(boosts.map {
                BoostEntity(type = it.kind, activatedAt = it.startsAt.toMillis(), expiresAt = it.endsAt.toMillis())
            })
        }
        val left = { kind: String -> grants.filter { it.kind == kind }.sumOf { (it.granted - it.used).coerceAtLeast(0) } }
        prefs.setServerEconomy(
            today = runCatching { LocalDate.parse(economy.gameDay) }.getOrElse { LocalDate.now(java.time.ZoneId.of("Asia/Seoul")) }.toEpochDay(),
            energyLeft = economy.energyLeft,
            energyMax = economy.energyMax,
            freeDraws = left("FREE"),
            bonusDraws = left("BONUS"),
        )
        return ServerResult.Ok(Unit)
    }

    /** 계정을 지웠을 때 — 그 계정의 사본(신발 · 원장 · 부스터 · 에너지 · 뽑기 횟수)을 폰에서 지운다 */
    suspend fun clearLocal() = lock.withLock {
        clearCopy()
        // 알림에도 그 계정의 적립 · 구매 금액이 남는다 — 다음에 로그인하는 계정에 보이지 않게
        db.notificationDao().clear()
        prefs.setEconomyOwner(null)
        _state.value = EconomySyncState.SIGNED_OUT
    }

    private suspend fun clearCopy() {
        db.withTransaction {
            db.sneakerDao().deleteAll()
            db.rewardDao().deleteAll()
            db.boostDao().deleteAll()
            db.claimedEventDao().deleteAll()
        }
        prefs.clearServerEconomy()
        bootstrapped = null
    }

    /**
     * 폰에만 있던 옛 신발(서버 번호가 없는 것)을 이 계정으로 한 번 올린다.
     * 서버 상한을 넘거나 거절된 신발은 올리지 못한 채 끝난다 — 적립 효과가 없는 기념품이라
     * 다시 시도하지 않는다. 연결이 끊겼을 때만 다음에 다시 한다.
     */
    private suspend fun importLegacy(userId: String): ServerResult<Unit>? {
        if (prefs.legacyEconomyImported(userId)) return null
        for (local in db.sneakerDao().allNow()) {
            if (local.serverId != 0L || local.origin.isNotBlank()) continue
            // 로그인 전에 받은 기본 첫 신발은 올리지 않는다 — 서버가 첫 신발을 따로 준다
            if (local.isDefaultStarter()) continue
            when (val r = market.import(
                localId = local.id, faction = local.factionId, rarity = local.rarity, variant = local.variant,
                level = local.level, luck = local.luck, comfort = local.comfort, durability = local.durability,
            )) {
                is ServerResult.Retry -> return r.cast()
                is ServerResult.SignInRequired -> return r.cast()
                else -> Unit
            }
        }
        prefs.setLegacyEconomyImported(userId)
        return null
    }

    /**
     * 서버 원장 줄을 폰 원장으로. 원장을 끝까지 받았으면 줄의 합이 곧 잔고다 — 그 사이에 새 줄이
     * 생겼어도 받아 온 줄이 더 최신이다. 너무 길어 다 못 받았을 때만 맨 앞에 나머지 합을 한 줄로 둔다.
     */
    private fun ledgerRows(rows: List<LedgerEntryRow>, balance: Double, complete: Boolean): List<RewardEntity> {
        val mirrored = rows.map {
            RewardEntity(timestamp = it.occurredAt.toMillis(), type = it.kind, amount = it.amount, description = it.description)
        }
        if (complete) return mirrored
        val gap = balance - mirrored.sumOf { it.amount }
        if (kotlin.math.abs(gap) < 0.00005) return mirrored
        val first = mirrored.minOfOrNull { it.timestamp } ?: now()
        return listOf(RewardEntity(timestamp = first - 1, type = CARRIED_OVER, amount = gap, description = "이전 기록 합계")) + mirrored
    }

    private fun ServerSneakerRow.toEntity(acquiredAt: Long) = SneakerEntity(
        id = id,
        factionId = faction,
        rarity = rarity,
        variant = variant,
        level = level,
        mintNumber = mintNumber.toInt(),
        luck = 1.0,
        comfort = 1.0 + comfortBps / 10_000.0,
        durability = durability.toInt(),
        equipped = equipped,
        acquiredAt = acquiredAt,
        serverId = id,
        origin = origin,
        efficiencyBps = efficiencyBps,
        comfortBps = comfortBps,
        durabilityPts = durability,
        maxLevel = maxLevel,
        status = status,
        chainState = chainState,
        canWithdraw = canWithdraw,
        serverUpgradeCost = upgradeCost,
        repairCostPerPoint = repairCostPerPoint,
        genesisNo = genesisNo ?: 0,
    )

    companion object {
        /** 받아 온 원장이 잔고의 일부뿐일 때 앞에 두는 줄의 종류 */
        const val CARRIED_OVER = "CARRIED_OVER"
        private const val LEDGER_PAGE = 1000
        private const val MAX_LEDGER_PAGES = 20
        private const val BOOST_KEEP_MILLIS = 2 * 24 * 60 * 60 * 1000L
    }
}

private fun <T> ServerResult<T>.failure(): ServerResult<Unit>? = when (this) {
    is ServerResult.Ok -> null
    is ServerResult.Rejected -> this
    is ServerResult.Retry -> this
    is ServerResult.SignInRequired -> this
}

@Suppress("UNCHECKED_CAST")
private fun <T> ServerResult<*>.cast(): ServerResult<T> = this as ServerResult<T>

private fun String.toMillis(): Long =
    runCatching { OffsetDateTime.parse(this).toInstant().toEpochMilli() }.getOrElse { System.currentTimeMillis() }

/** 로그인 전 폰이 준 기본 첫 신발(바람 · 일반 · 1번 · 1레벨) */
private fun SneakerEntity.isDefaultStarter(): Boolean =
    factionId == "WIND" && rarity == "COMMON" && variant == 0 && level <= 1 && mintNumber == 1
