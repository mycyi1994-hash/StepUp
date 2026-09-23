package com.stepup.android.data.remote

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.decodeFromString

/**
 * NFT 마켓 — 서버와 말을 주고받는 쪽.
 *
 * 표에 직접 쓰지 않는다. 앱에 박힌 anon 키는 누구나 꺼낼 수 있으므로, 쓰는
 * 일은 전부 서버 함수(`supabase/migrations/0007_market.sql`)가 한다. 여기서는
 * 그 함수를 부르고 결과를 옮길 뿐이다.
 *
 * 읽는 것은 뷰에서 바로 가져온다. 시세판·매물·입찰은 남의 것도 봐야 하고,
 * 그것은 가려 둘 정보가 아니다.
 */
class MarketApi(private val server: StepUpServer) {

    // ── 읽기 ────────────────────────────────────────────────────────

    /** 모델별 시세 한 줄씩. 거래가 많은 순으로 온다. */
    suspend fun quotes(): ServerResult<List<QuoteRow>> =
        server.authed { token ->
            server.http.get(
                "${server.restUrl}/market_quotes?select=*" +
                    "&order=volume_24h.desc,supply.desc,rarity.asc",
                server.headers(token),
            )
        }.mapBody { serverJson.decodeFromString<List<QuoteRow>>(it) }

    /** 한 모델의 매물. 싼 것이 위로 온다 — 살 사람이 먼저 보는 값이다. */
    suspend fun asks(model: ModelKey): ServerResult<List<AskRow>> =
        server.authed { token ->
            server.http.get(
                "${server.restUrl}/market_asks?select=*&${model.filter()}&order=price.asc",
                server.headers(token),
            )
        }.mapBody { serverJson.decodeFromString<List<AskRow>>(it) }

    /** 한 모델의 구매 입찰. 비싼 것이 위로 — 팔 사람이 먼저 보는 값이다. */
    suspend fun bids(model: ModelKey): ServerResult<List<BidRow>> =
        server.authed { token ->
            server.http.get(
                "${server.restUrl}/market_bid_book?select=*&${model.filter()}&order=price.desc",
                server.headers(token),
            )
        }.mapBody { serverJson.decodeFromString<List<BidRow>>(it) }

    /** 체결 내역. 시세가 어떻게 움직였는지는 이것뿐이다. */
    suspend fun history(model: ModelKey, limit: Int = 30): ServerResult<List<TradeRow>> =
        server.authed { token ->
            server.http.get(
                "${server.restUrl}/market_trades" +
                    "?select=id,faction,rarity,variant,level,price,kind,traded_at" +
                    "&${model.filter()}&order=traded_at.desc&limit=$limit",
                server.headers(token),
            )
        }.mapBody { serverJson.decodeFromString<List<TradeRow>>(it) }

    /** 거래소가 아는 내 스니커즈. 판매 중이면 그 매물도 함께 온다. */
    suspend fun mySneakers(): ServerResult<List<MySneakerRow>> =
        rpc("market_my_sneakers", "{}") { serverJson.decodeFromString<List<MySneakerRow>>(it) }

    suspend fun myBids(): ServerResult<List<MyBidRow>> =
        rpc("market_my_bids", "{}") { serverJson.decodeFromString<List<MyBidRow>>(it) }

    /**
     * 거래로 오간 SUP 줄들. [afterId] 다음 것만 온다.
     *
     * 화면이 보여 주는 잔고는 폰의 원장이고 거래는 서버 원장에서 일어난다.
     * 그 차이를 메우려면 서버가 적은 줄을 폰으로 옮겨 적어야 하는데, 두 번
     * 옮기면 잔고가 늘어난다. 그래서 번호로 잘라 가져온다.
     */
    suspend fun ledgerSince(afterId: Long): ServerResult<List<LedgerRow>> =
        server.authed { token ->
            server.http.get(
                "${server.restUrl}/sup_ledger?select=id,kind,amount,description,occurred_at" +
                    "&kind=in.(ESCROW_LOCK,ESCROW_UNLOCK,TRADE_BUY,TRADE_SELL,TRADE_FEE)" +
                    "&id=gt.$afterId&order=id.asc&limit=200",
                server.headers(token),
            )
        }.mapBody { serverJson.decodeFromString<List<LedgerRow>>(it) }

    suspend fun myTrades(limit: Int = 30): ServerResult<List<MyTradeRow>> =
        rpc("market_my_trades", jsonBody { put("p_limit", limit) }) {
            serverJson.decodeFromString<List<MyTradeRow>>(it)
        }

    // ── 쓰기 ────────────────────────────────────────────────────────

    /**
     * 폰의 신발을 거래소 장부에 올린다.
     *
     * 팔려고 할 때만 부른다. 열어 보기만 해도 다 올려 버리면, 팔 생각이
     * 없던 신발까지 서버로 보내는 셈이고 계정당 상한도 금세 찬다.
     */
    suspend fun import(
        localId: Long,
        faction: String,
        rarity: String,
        variant: Int,
        level: Int,
        luck: Double,
        comfort: Double,
        durability: Int,
    ): ServerResult<Long> = rpc(
        "market_import",
        jsonBody {
            put("p_local_id", localId)
            put("p_faction", faction)
            put("p_rarity", rarity)
            put("p_variant", variant)
            put("p_level", level)
            put("p_luck", luck)
            put("p_comfort", comfort)
            put("p_durability", durability)
        },
    ) { it.trim().toLongOrNull() }

    /**
     * 판다고 내놓는다.
     *
     * 이미 그 값 이상을 부른 사람이 있으면 매물로 서지 않고 바로 팔린다.
     * 그때는 매물 번호가 없으므로 null 이 온다 — 없는 것이 아니라 팔린 것이다.
     */
    suspend fun list(sneakerId: Long, price: Double): ServerResult<Long?> = rpcNullable(
        "market_list",
        jsonBody {
            put("p_sneaker_id", sneakerId)
            put("p_price", price)
        },
    )

    suspend fun cancelListing(listingId: Long): ServerResult<Unit> =
        rpcVoid("market_cancel_listing", jsonBody { put("p_listing_id", listingId) })

    suspend fun buyNow(listingId: Long): ServerResult<Long?> =
        rpcNullable("market_buy_now", jsonBody { put("p_listing_id", listingId) })

    /** 모델에 값을 건다. 살 수 있는 매물이 이미 있으면 기다리지 않고 산다. */
    suspend fun bid(
        model: ModelKey,
        minLevel: Int,
        price: Double,
    ): ServerResult<Long?> = rpcNullable(
        "market_bid",
        jsonBody {
            put("p_faction", model.faction)
            put("p_rarity", model.rarity)
            put("p_variant", model.variant)
            put("p_min_level", minLevel)
            put("p_price", price)
        },
    )

    suspend fun cancelBid(bidId: Long): ServerResult<Unit> =
        rpcVoid("market_cancel_bid", jsonBody { put("p_bid_id", bidId) })

    suspend fun sellNow(sneakerId: Long, bidId: Long): ServerResult<Long?> = rpcNullable(
        "market_sell_now",
        jsonBody {
            put("p_sneaker_id", sneakerId)
            put("p_bid_id", bidId)
        },
    )

    // ── 공통 ────────────────────────────────────────────────────────

    private suspend fun <T> rpc(
        name: String,
        body: String,
        parse: (String) -> T?,
    ): ServerResult<T> =
        server.authed { token ->
            server.http.post("${server.restUrl}/rpc/$name", body, server.headers(token))
        }.mapBody(parse)

    /**
     * 숫자 하나를 돌려주되 null 일 수 있는 함수.
     *
     * [mapBody] 는 null 을 "못 읽었다"로 치므로 그대로 쓸 수 없다. 여기서는
     * null 이 뜻이 있는 답(바로 체결됐다)이라 한 겹 감싼다.
     */
    private suspend fun rpcNullable(name: String, body: String): ServerResult<Long?> =
        rpc(name, body) { text ->
            // "null" 이거나 빈 몸통이면 번호가 없는 것이다
            Box(text.trim().takeIf { it.isNotEmpty() && it != "null" }?.toLongOrNull())
        }.let { result ->
            when (result) {
                is ServerResult.Ok -> ServerResult.Ok(result.value.value)
                is ServerResult.Rejected -> result
                is ServerResult.Retry -> result
                is ServerResult.SignInRequired -> result
            }
        }

    /** 돌려줄 것이 없는 함수. 몸통이 비어 있어도 성공이다. */
    private suspend fun rpcVoid(name: String, body: String): ServerResult<Unit> =
        rpc(name, body) { Unit }

    /** null 을 "못 읽었다"와 구분하려고 한 번 싸는 그릇 */
    private class Box(val value: Long?)
}

/** 모델 하나 — 속성 × 등급 × 변형. 시세는 이 단위로 매겨진다. */
data class ModelKey(val faction: String, val rarity: String, val variant: Int) {
    internal fun filter(): String = "faction=eq.$faction&rarity=eq.$rarity&variant=eq.$variant"
}

@Serializable
data class QuoteRow(
    val faction: String,
    val rarity: String,
    val variant: Int,
    val supply: Int,
    val listed: Int,
    /** 즉시 구매가 — 가장 싼 매물. 매물이 없으면 null */
    val ask: Double? = null,
    /** 즉시 판매가 — 가장 비싼 구매 입찰. 없으면 null */
    val bid: Double? = null,
    @SerialName("bid_count") val bidCount: Int = 0,
    @SerialName("last_price") val lastPrice: Double? = null,
    @SerialName("trades_24h") val trades24h: Int = 0,
    @SerialName("volume_24h") val volume24h: Double = 0.0,
)

@Serializable
data class AskRow(
    @SerialName("listing_id") val listingId: Long,
    val price: Double,
    @SerialName("seller_id") val sellerId: String,
    @SerialName("sneaker_id") val sneakerId: Long,
    val faction: String,
    val rarity: String,
    val variant: Int,
    val level: Int,
    @SerialName("mint_number") val mintNumber: Long,
    val durability: Int,
)

@Serializable
data class BidRow(
    @SerialName("bid_id") val bidId: Long,
    @SerialName("buyer_id") val buyerId: String,
    val faction: String,
    val rarity: String,
    val variant: Int,
    @SerialName("min_level") val minLevel: Int,
    val price: Double,
)

@Serializable
data class TradeRow(
    val id: Long,
    val faction: String,
    val rarity: String,
    val variant: Int,
    val level: Int,
    val price: Double,
    /** BY_ASK = 매물이 먼저 있었다, BY_BID = 구매 입찰이 먼저 있었다 */
    val kind: String,
    @SerialName("traded_at") val tradedAt: String,
)

@Serializable
data class MySneakerRow(
    val id: Long,
    /** 이 폰에서 올린 것이면 폰 안의 번호. 사서 받은 것이면 null */
    @SerialName("local_id") val localId: Long? = null,
    val faction: String,
    val rarity: String,
    val variant: Int,
    val level: Int,
    @SerialName("mint_number") val mintNumber: Long,
    val luck: Double = 1.0,
    val comfort: Double = 1.0,
    val durability: Int,
    /** OWNED · LISTED */
    val status: String,
    @SerialName("listing_id") val listingId: Long? = null,
    @SerialName("listing_price") val listingPrice: Double? = null,
)

@Serializable
data class LedgerRow(
    val id: Long,
    val kind: String,
    val amount: Double,
    val description: String = "",
    @SerialName("occurred_at") val occurredAt: String,
)

@Serializable
data class MyBidRow(
    val id: Long,
    val faction: String,
    val rarity: String,
    val variant: Int,
    @SerialName("min_level") val minLevel: Int,
    val price: Double,
)

@Serializable
data class MyTradeRow(
    val id: Long,
    val faction: String,
    val rarity: String,
    val variant: Int,
    val level: Int,
    val price: Double,
    val fee: Double,
    /** true = 내가 팔았다, false = 내가 샀다 */
    val sold: Boolean,
    @SerialName("traded_at") val tradedAt: String,
)
