package com.stepup.android.data.repo

import com.stepup.android.data.local.RewardDao
import com.stepup.android.data.local.RewardEntity
import com.stepup.android.data.local.SneakerDao
import com.stepup.android.data.local.SneakerEntity
import com.stepup.android.data.prefs.UserPrefs
import com.stepup.android.data.remote.AskRow
import com.stepup.android.data.remote.BidRow
import com.stepup.android.data.remote.MarketApi
import com.stepup.android.data.remote.ModelKey
import com.stepup.android.data.remote.MyBidRow
import com.stepup.android.data.remote.MySneakerRow
import com.stepup.android.data.remote.MyTradeRow
import com.stepup.android.data.remote.QuoteRow
import com.stepup.android.data.remote.ServerResult
import com.stepup.android.data.remote.StepUpServer
import com.stepup.android.data.remote.TradeRow
import java.time.OffsetDateTime
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

/**
 * 거래가 어떻게 끝났는지.
 *
 * 서버가 보낸 거절 사유를 그대로 들고 온다. "SUP 가 모자랍니다"처럼 사람이
 * 읽을 수 있는 문장이 서버 함수에 적혀 있고, 여기서 다시 지어내면 진짜
 * 이유가 가려진다.
 */
sealed interface MarketOutcome {
    /** @property traded 기다리지 않고 바로 체결됐는가 */
    data class Ok(val traded: Boolean) : MarketOutcome

    data class Failed(val reason: String) : MarketOutcome

    /**
     * 신고 있는 신발을 팔려고 했다.
     *
     * 서버는 "신고 있다"를 모른다 — 착용은 폰 안의 일이다. 그래서 이 판단은
     * 앱이 하고, 할 말도 앱의 문자열에서 꺼내야 번역이 된다.
     */
    data object Equipped : MarketOutcome

    data object SignInRequired : MarketOutcome

    data class Offline(val reason: String) : MarketOutcome
}

/** 한 모델의 장부 — 매물 · 구매 입찰 · 체결 내역 */
data class ModelBook(
    val asks: List<AskRow>,
    val bids: List<BidRow>,
    val history: List<TradeRow>,
)

/** 내 거래 현황 */
data class MyMarket(
    val sneakers: List<MySneakerRow>,
    val bids: List<MyBidRow>,
    val trades: List<MyTradeRow>,
)

/**
 * NFT 마켓.
 *
 * ── 두 원장을 맞추는 일 ──
 *
 * 화면이 보여 주는 SUP 는 폰의 원장이고, 거래는 서버 원장에서 일어난다.
 * 둘을 그냥 두면 신발을 팔았는데 잔고가 그대로인 화면이 된다.
 *
 * 그래서 거래로 오간 줄을 폰으로 옮겨 적는다. 두 번 적으면 잔고가 늘어나므로
 * 어디까지 옮겼는지를 번호로 기억한다([UserPrefs.marketLedgerCursor]).
 *
 * ── 신발이 오가는 일 ──
 *
 * 소유자를 정하는 것은 서버다. 폰의 목록은 그 사본이므로, 열 때마다 맞춰
 * 본다 — 서버에는 있는데 폰에 없으면 산 것이고, 폰에는 있는데 서버의 내
 * 목록에 없으면 판 것이다.
 */
class MarketRepository(
    private val api: MarketApi,
    private val server: StepUpServer,
    private val sneakerDao: SneakerDao,
    private val rewardDao: RewardDao,
    private val prefs: UserPrefs,
    /** 서버 경제. 있으면 신발 · 원장을 통째로 서버 값으로 맞춘다(거래 줄만 따로 옮기지 않는다) */
    private val economySync: EconomySync? = null,
) {

    // ── 보기 ────────────────────────────────────────────────────────

    suspend fun quotes(): ServerResult<List<QuoteRow>> = api.quotes()

    /** 폰이 아는 내 스니커즈. 팔 것을 고를 때 쓴다. */
    fun inventory(): Flow<List<SneakerEntity>> = sneakerDao.observeAll()

    /**
     * 한 모델의 장부를 한 번에.
     *
     * 세 번 나눠 물으면 그 사이에 팔려서 앞뒤가 안 맞는 화면이 된다 —
     * 매물 목록에는 있는데 체결 내역에도 있는 식이다. 한 번에 받아 한 번에
     * 갈아 끼운다.
     */
    suspend fun book(model: ModelKey): ServerResult<ModelBook> {
        val asks = api.asks(model)
        if (asks !is ServerResult.Ok) return asks.carry()
        val bids = api.bids(model)
        if (bids !is ServerResult.Ok) return bids.carry()
        val history = api.history(model)
        if (history !is ServerResult.Ok) return history.carry()
        return ServerResult.Ok(ModelBook(asks.value, bids.value, history.value))
    }

    suspend fun mine(): ServerResult<MyMarket> {
        val sneakers = api.mySneakers()
        if (sneakers !is ServerResult.Ok) return sneakers.carry()
        val bids = api.myBids()
        if (bids !is ServerResult.Ok) return bids.carry()
        val trades = api.myTrades()
        if (trades !is ServerResult.Ok) return trades.carry()
        return ServerResult.Ok(MyMarket(sneakers.value, bids.value, trades.value))
    }

    /**
     * 거래에 쓸 수 있는 SUP.
     *
     * 폰에 보이는 잔고와 다를 수 있다. 폰의 잔고에는 아직 서버로 올라가지
     * 않은 러닝이 섞여 있고, 거래는 서버가 아는 만큼만 할 수 있다. 다른
     * 숫자를 감추는 대신 화면에 그대로 보여 준다.
     */
    suspend fun tradableBalance(): ServerResult<Double> = server.balance()

    // ── 하기 ────────────────────────────────────────────────────────

    /**
     * 내 신발을 판다고 내놓는다.
     *
     * 거래소가 모르는 신발이면 먼저 장부에 올린다. 열어 보기만 해도 전부
     * 올려 버리면 팔 생각도 없던 신발이 서버로 가므로, **파는 순간**에만
     * 올린다.
     */
    suspend fun listForSale(localId: Long, price: Double): MarketOutcome {
        val local = sneakerDao.byId(localId) ?: return MarketOutcome.Failed("없는 스니커즈입니다")
        if (local.equipped) return MarketOutcome.Equipped

        val serverId = ensureRegistered(local) ?: return lastProblem
        return when (val result = api.list(serverId, price)) {
            is ServerResult.Ok -> {
                sync()
                MarketOutcome.Ok(traded = result.value == null)
            }
            else -> result.toOutcome()
        }
    }

    suspend fun cancelListing(listingId: Long): MarketOutcome =
        api.cancelListing(listingId).finish()

    suspend fun buy(listingId: Long): MarketOutcome =
        when (val result = api.buyNow(listingId)) {
            is ServerResult.Ok -> {
                sync()
                MarketOutcome.Ok(traded = true)
            }
            else -> result.toOutcome()
        }

    suspend fun placeBid(model: ModelKey, minLevel: Int, price: Double): MarketOutcome =
        when (val result = api.bid(model, minLevel, price)) {
            is ServerResult.Ok -> {
                sync()
                // 번호가 없으면 기다리지 않고 바로 산 것이다
                MarketOutcome.Ok(traded = result.value == null)
            }
            else -> result.toOutcome()
        }

    suspend fun cancelBid(bidId: Long): MarketOutcome = api.cancelBid(bidId).finish()

    /** 걸려 있는 구매 입찰에 내 신발을 판다. */
    suspend fun sellInto(localId: Long, bidId: Long): MarketOutcome {
        val local = sneakerDao.byId(localId) ?: return MarketOutcome.Failed("없는 스니커즈입니다")
        if (local.equipped) return MarketOutcome.Equipped

        val serverId = ensureRegistered(local) ?: return lastProblem
        return when (val result = api.sellNow(serverId, bidId)) {
            is ServerResult.Ok -> {
                sync()
                MarketOutcome.Ok(traded = true)
            }
            else -> result.toOutcome()
        }
    }

    // ── 맞춰 보기 ───────────────────────────────────────────────────

    /**
     * 서버의 장부와 폰의 목록·원장을 맞춘다.
     *
     * 실패해도 조용히 넘어간다. 맞추지 못한 것은 다음에 열 때 맞춰지고,
     * 그동안 화면이 멈추는 것보다는 낫다.
     */
    suspend fun sync() = syncLock.withLock {
        val economy = economySync
        if (economy != null) {
            economy.refresh()
            return@withLock
        }
        reconcileSneakers()
        mirrorLedger()
    }

    // 화면 여러 곳(아이템·거래소·모델 화면)이 동시에 부른다. 겹치면 둘이 같은
    // 커서를 읽어 같은 거래 줄을 두 번 적고(잔고가 늘어난다), 산 신발도 두 켤레가 된다.
    private val syncLock = Mutex()

    private suspend fun reconcileSneakers() {
        val mine = api.mySneakers()
        if (mine !is ServerResult.Ok) return
        val byServerId = mine.value.associateBy { it.id }

        // 1) 서버에는 있는데 폰에 없다 → 사서 받은 신발이다
        for (row in mine.value) {
            if (sneakerDao.byServerId(row.id) != null) continue
            // 이 폰에서 올렸는데 응답을 못 받아 번호를 못 적은 신발이면 번호만 잇는다.
            // 새로 넣으면 같은 신발이 두 켤레가 된다.
            val uploaded = row.localId?.let { sneakerDao.byId(it) }
            if (uploaded != null && uploaded.serverId == 0L &&
                uploaded.factionId == row.faction && uploaded.rarity == row.rarity &&
                uploaded.variant == row.variant
            ) {
                sneakerDao.setServerId(uploaded.id, row.id)
                continue
            }
            sneakerDao.insert(
                SneakerEntity(
                    factionId = row.faction,
                    rarity = row.rarity,
                    variant = row.variant,
                    level = row.level,
                    // 거래소 번호를 그대로 쓴다. 폰에서 매긴 번호는 다른
                    // 사람의 신발에는 뜻이 없다.
                    mintNumber = row.mintNumber.toInt(),
                    luck = row.luck,
                    comfort = row.comfort,
                    durability = row.durability,
                    equipped = false,
                    acquiredAt = System.currentTimeMillis(),
                    serverId = row.id,
                )
            )
        }

        // 2) 폰에는 있는데 서버의 내 목록에 없다 → 팔린 신발이다
        for (local in sneakerDao.registered()) {
            if (byServerId.containsKey(local.serverId)) continue
            sneakerDao.delete(local)
        }
    }

    /**
     * 서버 원장의 거래 줄을 폰의 원장으로 옮겨 적는다.
     *
     * 옮긴 줄의 번호를 기억해 두고 그다음부터만 가져온다. 같은 줄을 두 번
     * 적으면 잔고가 실제보다 늘어나고, 늘어난 잔고로는 살 수 없는 것을
     * 살 수 있다고 착각하게 된다.
     */
    private suspend fun mirrorLedger() {
        val cursor = prefs.marketLedgerCursor.first()
        val rows = api.ledgerSince(cursor)
        if (rows !is ServerResult.Ok || rows.value.isEmpty()) return

        for (row in rows.value) {
            rewardDao.insert(
                RewardEntity(
                    timestamp = row.occurredAt.toMillis(),
                    type = row.kind,
                    amount = row.amount,
                    description = row.description,
                )
            )
        }
        prefs.setMarketLedgerCursor(rows.value.maxOf { it.id })
    }

    /**
     * 거래소가 이 신발을 알게 한다. 이미 알면 그 번호를 그대로 준다.
     *
     * @return 거래소 번호. 올리지 못했으면 null 이고 이유는 [lastProblem] 에 있다.
     */
    private suspend fun ensureRegistered(local: SneakerEntity): Long? {
        if (local.serverId != 0L) return local.serverId

        val result = api.import(
            localId = local.id,
            faction = local.factionId,
            rarity = local.rarity,
            variant = local.variant,
            level = local.level,
            luck = local.luck,
            comfort = local.comfort,
            durability = local.durability,
        )
        return when (result) {
            is ServerResult.Ok -> {
                sneakerDao.setServerId(local.id, result.value)
                result.value
            }
            else -> {
                lastProblem = result.toOutcome()
                null
            }
        }
    }

    /**
     * 방금 실패한 이유.
     *
     * [ensureRegistered] 가 null 을 돌려줄 때 그 까닭을 함께 넘기려고 둔다.
     * 한 번에 한 가지 일만 하므로 덮어써도 섞이지 않는다.
     */
    private var lastProblem: MarketOutcome = MarketOutcome.Failed("알 수 없는 문제")

    // ── 옮기기 ──────────────────────────────────────────────────────

    private fun ServerResult<*>.toOutcome(): MarketOutcome = when (this) {
        is ServerResult.Ok -> MarketOutcome.Ok(traded = false)
        is ServerResult.Rejected -> MarketOutcome.Failed(reason)
        is ServerResult.Retry -> MarketOutcome.Offline(reason)
        is ServerResult.SignInRequired -> MarketOutcome.SignInRequired
    }

    private suspend fun ServerResult<Unit>.finish(): MarketOutcome = when (this) {
        is ServerResult.Ok -> {
            sync()
            MarketOutcome.Ok(traded = false)
        }
        else -> toOutcome()
    }

    /** 성공이 아닌 결말을 다른 형태의 결말로 그대로 옮긴다. */
    @Suppress("UNCHECKED_CAST")
    private fun <T> ServerResult<*>.carry(): ServerResult<T> = this as ServerResult<T>
}

/**
 * 서버가 보낸 시각을 밀리초로.
 *
 * PostgREST 는 "2026-09-21T07:30:06.12+00:00" 처럼 시차를 붙여 보낸다.
 * Instant.parse 는 끝이 Z 인 것만 읽으므로 여기서는 쓸 수 없다.
 * 못 읽으면 지금으로 친다 — 시각 하나 때문에 원장 줄을 버릴 이유는 없다.
 */
private fun String.toMillis(): Long =
    runCatching { OffsetDateTime.parse(this).toInstant().toEpochMilli() }
        .getOrElse { System.currentTimeMillis() }
