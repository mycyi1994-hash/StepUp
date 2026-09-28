package com.stepup.android.data.remote

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.decodeFromString

/**
 * 서버 경제(0022~0024) — 잔고 · 신발 · 에너지 · 뽑기 · 강화 · 수리 · 부스터.
 *
 * 정본은 서버다. 폰은 여기서 받은 값을 화면에 비출 뿐 스스로 적립하거나 차감하지
 * 않는다. 쓰는 일은 전부 서버 함수가 하고, 폰은 그 결과를 다시 읽어 온다.
 */
class EconomyApi(private val server: StepUpServer) {

    val isConfigured: Boolean get() = server.isConfigured

    // ── 읽기 ────────────────────────────────────────────────────────

    /** 첫 신발 · 무료 뽑기 10회를 챙긴다. 여러 번 불러도 한 번만 일어난다. */
    suspend fun bootstrap(): ServerResult<Unit> = rpc("economy_bootstrap", "{}") { Unit }

    suspend fun economy(): ServerResult<EconomyRow> =
        rpc("my_economy", "{}") { serverJson.decodeFromString<List<EconomyRow>>(it).firstOrNull() }

    suspend fun sneakers(): ServerResult<List<ServerSneakerRow>> =
        rpc("my_sneakers", "{}") { serverJson.decodeFromString<List<ServerSneakerRow>>(it) }

    /** 원장 한 쪽. [afterId] 다음 줄부터 번호 순으로. */
    suspend fun ledgerPage(afterId: Long, limit: Int = 1000): ServerResult<List<LedgerEntryRow>> =
        server.authed { token ->
            server.http.get(
                "${server.restUrl}/sup_ledger?select=id,kind,amount,description,occurred_at" +
                    "&id=gt.$afterId&order=id.asc&limit=$limit",
                server.headers(token),
            )
        }.mapBody { serverJson.decodeFromString<List<LedgerEntryRow>>(it) }

    suspend fun drawGrants(): ServerResult<List<DrawGrantRow>> =
        server.authed { token ->
            server.http.get("${server.restUrl}/draw_grants?select=kind,granted,used", server.headers(token))
        }.mapBody { serverJson.decodeFromString<List<DrawGrantRow>>(it) }

    /** 최근 이틀 안에 끝난 것까지 — 스트릭 보호막은 끝난 뒤에도 어제를 덮었는지 본다 */
    suspend fun boosts(sinceIso: String): ServerResult<List<BoostRow>> =
        server.authed { token ->
            server.http.get(
                "${server.restUrl}/boosts?select=id,kind,starts_at,ends_at&ends_at=gte.$sinceIso&order=id.asc",
                server.headers(token),
            )
        }.mapBody { serverJson.decodeFromString<List<BoostRow>>(it) }

    /** 내 체인 기록(0044) — 최근 것부터. 확정 전에는 거래 번호가 없다 */
    suspend fun chainActivity(limit: Int = 100): ServerResult<List<ChainActivityRow>> =
        rpc("my_chain_activity", "{\"p_limit\":$limit}") { serverJson.decodeFromString<List<ChainActivityRow>>(it) }

    // ── 쓰기 ────────────────────────────────────────────────────────

    /** 무료 뽑기. 새 신발의 번호를 돌려준다. */
    suspend fun drawFree(): ServerResult<Long> = rpc("draw_free", "{}") { it.trim().toLongOrNull() }

    /** 상급 뽑기(0042) — 지갑 첫 연결 선물부터, 그다음 러닝으로 받은 기회. 새 신발의 번호를 돌려준다. */
    suspend fun drawPremium(): ServerResult<Long> = rpc("premium_draw", "{}") { it.trim().toLongOrNull() }

    /** 뽑기 화면의 현황(0042) — 오늘 무료 · 가입 선물 · 지갑 선물 · 러닝으로 받은 상급 · 모은 거리 */
    suspend fun drawStatus(): ServerResult<com.stepup.android.domain.DrawStatus> =
        rpc("draw_status", "{}") { serverJson.decodeFromString<List<DrawStatusRow>>(it).firstOrNull()?.toDomain() }

    /** 강화. 새 레벨을 돌려준다. */
    suspend fun upgrade(id: Long): ServerResult<Int> =
        rpc("sneaker_upgrade", jsonBody { put("p_id", id) }) { it.trim().toIntOrNull() }

    /** 내구도를 가득 채운다. 채운 뒤의 내구도를 돌려준다. */
    suspend fun repair(id: Long): ServerResult<Double> =
        rpc("sneaker_repair", jsonBody { put("p_id", id) }) { it.trim().toDoubleOrNull() }

    suspend fun equip(id: Long): ServerResult<Unit> =
        rpc("sneaker_equip", jsonBody { put("p_id", id) }) { Unit }

    /** 부스터 구매. 남은 잔고를 돌려준다. */
    suspend fun boostBuy(kind: String): ServerResult<Double> =
        rpc("boost_buy", jsonBody { put("p_kind", kind) }) { it.trim().toDoubleOrNull() }

    /** 하루 목표를 서버에 적는다 — 목표 달성 보너스를 서버가 이 값으로 판정한다. 적힌 값을 돌려준다. */
    suspend fun setDailyGoal(goal: Int): ServerResult<Int> =
        rpc("profile_set_daily_goal", jsonBody { put("p_goal", goal) }) { it.trim().toIntOrNull() }

    /** 오늘 목표 달성 보너스. 서버가 확인한 러닝 걸음으로 판정한다. */
    suspend fun goalClaim(): ServerResult<Double> =
        rpc("goal_claim", "{}") { it.trim().toDoubleOrNull() }

    private suspend fun <T> rpc(name: String, body: String, parse: (String) -> T?): ServerResult<T> =
        server.authed { token ->
            server.http.post("${server.restUrl}/rpc/$name", body, server.headers(token))
        }.mapBody(parse)
}

@Serializable
data class EconomyRow(
    val balance: Double,
    @SerialName("energy_max") val energyMax: Double,
    @SerialName("energy_left") val energyLeft: Double,
    @SerialName("xp_booster_until") val xpBoosterUntil: String? = null,
    @SerialName("streak_shield_until") val streakShieldUntil: String? = null,
    @SerialName("equipped_id") val equippedId: Long? = null,
    @SerialName("game_day") val gameDay: String = "",
)

@Serializable
data class ServerSneakerRow(
    val id: Long,
    val faction: String,
    val rarity: String,
    val variant: Int,
    val level: Int,
    @SerialName("max_level") val maxLevel: Int,
    @SerialName("efficiency_bps") val efficiencyBps: Int,
    @SerialName("comfort_bps") val comfortBps: Int,
    val durability: Double,
    val equipped: Boolean,
    val origin: String,
    @SerialName("chain_state") val chainState: String = "APP",
    val status: String = "OWNED",
    @SerialName("mint_number") val mintNumber: Long = 0,
    @SerialName("genesis_no") val genesisNo: Int? = null,
    @SerialName("km_run") val kmRun: Double = 0.0,
    @SerialName("lock_km") val lockKm: Double = 0.0,
    @SerialName("can_withdraw") val canWithdraw: Boolean = false,
    @SerialName("upgrade_cost") val upgradeCost: Double = 0.0,
    @SerialName("repair_cost_per_point") val repairCostPerPoint: Double = 0.0,
    /** 새 도감(0045) 번호 — 비어 있으면 예전 52종(속성 × 변형) */
    @SerialName("model_id") val modelId: Int? = null,
    /** 체인 토큰 번호 — v2(1~) · v3(1,000,001~). 아직 체인에 없으면 null */
    @SerialName("token_id") val tokenId: Long? = null,
)

@Serializable
data class ChainActivityRow(
    val id: Long,
    val kind: String,
    val status: String,
    @SerialName("distance_m") val distanceM: Int? = null,
    @SerialName("duration_sec") val durationSec: Int? = null,
    val badge: String? = null,
    @SerialName("badge_value") val badgeValue: Int? = null,
    @SerialName("sneaker_id") val sneakerId: Long? = null,
    @SerialName("tx_hash") val txHash: String? = null,
    @SerialName("created_at") val createdAt: String,
    @SerialName("confirmed_at") val confirmedAt: String? = null,
) {
    fun toDomain() = com.stepup.android.domain.ChainRecord(
        id = id,
        kind = com.stepup.android.domain.ChainRecord.kindOf(kind),
        status = com.stepup.android.domain.ChainRecord.statusOf(status),
        distanceM = distanceM,
        durationSec = durationSec,
        badge = badge,
        badgeValue = badgeValue,
        sneakerId = sneakerId,
        txHash = txHash,
        createdAt = createdAt.isoMillis() ?: 0L,
        confirmedAt = confirmedAt?.isoMillis(),
    )
}

private fun String.isoMillis(): Long? =
    runCatching { java.time.OffsetDateTime.parse(this).toInstant().toEpochMilli() }.getOrNull()

@Serializable
data class LedgerEntryRow(
    val id: Long,
    val kind: String,
    val amount: Double,
    val description: String = "",
    @SerialName("occurred_at") val occurredAt: String,
)

@Serializable
data class DrawGrantRow(val kind: String, val granted: Int, val used: Int)

/** draw_status(0042) 한 줄 — 모두 남은 수다 */
@Serializable
data class DrawStatusRow(
    @SerialName("daily_left") val dailyLeft: Int = 0,
    @SerialName("daily_total") val dailyTotal: Int = 0,
    @SerialName("signup_left") val signupLeft: Int = 0,
    @SerialName("signup_granted") val signupGranted: Int = 0,
    @SerialName("wallet_linked") val walletLinked: Boolean = false,
    @SerialName("gift_on_link") val giftOnLink: Int = 0,
    @SerialName("gift_left") val giftLeft: Int = 0,
    @SerialName("run_left") val runLeft: Int = 0,
    @SerialName("genesis_left") val genesisLeft: Int = 0,
    @SerialName("run_progress_m") val runProgressM: Double = 0.0,
    @SerialName("run_step_m") val runStepM: Int = 1000,
    @SerialName("run_today") val runToday: Int = 0,
    @SerialName("run_daily_cap") val runDailyCap: Int = 0,
    @SerialName("chain_paused") val chainPaused: Boolean = false,
) {
    fun toDomain() = com.stepup.android.domain.DrawStatus(
        dailyLeft = dailyLeft, dailyTotal = dailyTotal, signupLeft = signupLeft, signupGranted = signupGranted,
        walletLinked = walletLinked, giftOnLink = giftOnLink, giftLeft = giftLeft, runLeft = runLeft,
        genesisLeft = genesisLeft, runProgressMeters = runProgressM, runStepMeters = runStepM,
        runToday = runToday, runDailyCap = runDailyCap, chainPaused = chainPaused,
    )
}

@Serializable
data class BoostRow(
    val id: Long,
    val kind: String,
    @SerialName("starts_at") val startsAt: String,
    @SerialName("ends_at") val endsAt: String,
)
