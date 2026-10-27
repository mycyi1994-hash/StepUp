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
class EconomyApi(private val server: StepUpServer) : com.stepup.android.data.repo.ForgeServer {

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

    /** 체인 기록 설정(0078) — 지갑으로 받기를 켰는가 · 지갑이 연결돼 있는가 */
    suspend fun chainSettings(): ServerResult<ChainSettingsRow> =
        rpc("my_chain_settings", "{}") { serverJson.decodeFromString<ChainSettingsRow>(it) }

    /** 체인 기록의 받는 사람을 내 지갑으로(켬) · 비움(끔). 기본은 꺼짐 */
    suspend fun setChainRecipient(on: Boolean): ServerResult<Boolean> =
        rpc("chain_recipient_set", "{\"p_on\":$on}") { it.trim().toBooleanStrictOrNull() }

    /** 오늘 출석(0078) — 서버가 하루 한 번만 체인 기록 줄에 세운다. 이미 했으면 false */
    suspend fun checkIn(): ServerResult<Unit> = rpc("chain_check_in", "{}") { Unit }

    /** 내 체인 기록(0044) — 최근 것부터. 확정 전에는 거래 번호가 없다 */
    suspend fun chainActivity(limit: Int = 100): ServerResult<List<ChainActivityRow>> =
        rpc("my_chain_activity", "{\"p_limit\":$limit}") { serverJson.decodeFromString<List<ChainActivityRow>>(it) }

    // ── 쓰기 ────────────────────────────────────────────────────────

    /** 무료 뽑기. 새 신발의 번호를 돌려준다. */
    suspend fun drawFree(): ServerResult<Long> = rpc("draw_free", "{}") { it.trim().toLongOrNull() }

    /** 상급 뽑기(0042) — 지갑 첫 연결 선물부터, 그다음 러닝으로 받은 기회. 새 신발의 번호를 돌려준다. */
    suspend fun drawPremium(): ServerResult<Long> = rpc("premium_draw", "{}") { it.trim().toLongOrNull() }

    /** 지갑 연결 문장(0025 · 0067) — 계정 번호와 10분짜리 일회용 확인 번호가 들어 있다. 처음 연결은 2단계 인증이 필요 없다 */
    suspend fun walletLinkChallenge(): ServerResult<String> =
        rpc("wallet_link_challenge", "{}") { runCatching { serverJson.decodeFromString<String>(it) }.getOrNull() }

    /** 뽑기 화면의 현황(0042) — 오늘 무료 · 가입 선물 · 지갑 선물 · 러닝으로 받은 상급 · 모은 거리 */
    suspend fun drawStatus(): ServerResult<com.stepup.android.domain.DrawStatus> =
        rpc("draw_status", "{}") { serverJson.decodeFromString<List<DrawStatusRow>>(it).firstOrNull()?.toDomain() }

    /** 뽑기 확률(0077) — 뽑기가 실제로 쓰는 등급 표(천분율). 로그인 전에도 읽는다 */
    suspend fun drawOdds(): ServerResult<com.stepup.android.domain.DrawOdds> =
        rpc("draw_odds", "{}") { com.stepup.android.domain.DrawOdds.parse(it) }

    /** 예전 강화(SUP 를 내고 레벨 +1). 새 앱의 강화 화면은 부르지 않는다 — 신발 강화는 아래 forge_* 다. 새 레벨을 돌려준다. */
    suspend fun upgrade(id: Long, request: String? = null): ServerResult<Int> =
        rpc("sneaker_upgrade", jsonBody { put("p_id", id); request?.let { put("p_request", it) } }) { it.trim().toIntOrNull() }

    // ── 신발 강화(0054, 규칙 forge-v2) — 재료 신발 3개 · 확률. 응답은 JSON 한 덩어리(ForgeRepository 가 읽는다) ──

    /** 강화 화면을 열 때 — 대상 상태와 재료 후보 */
    override suspend fun forgeMaterials(target: Long): ServerResult<String> =
        rpc("forge_materials", "{\"p_target\":$target}") { it }

    /** 실행 전 견적 — 서버가 판정할 확률 */
    override suspend fun forgeQuote(target: Long, materials: List<Long>): ServerResult<String> =
        rpc("forge_quote", "{\"p_target\":$target,\"p_materials\":${materials.joinToString(",", "[", "]")}}") { it }

    /** 강화 한 번. 같은 [key] 로 다시 보내면 기록된 결과를 그대로 돌려준다 */
    override suspend fun forgeStart(key: String, target: Long, materials: List<Long>, quoteVersion: String): ServerResult<String> =
        rpc(
            "forge_start",
            "{\"p_key\":${key.asJsonString()},\"p_target\":$target," +
                "\"p_materials\":${materials.joinToString(",", "[", "]")},\"p_quote\":${quoteVersion.asJsonString()}}",
        ) { it }

    /** 같은 요청 키의 결과. 기록이 없으면 서버가 그 키를 막고 "접수 안 됨"을 확정한다 */
    override suspend fun forgeResult(key: String): ServerResult<String> =
        rpc("forge_result", "{\"p_key\":${key.asJsonString()}}") { it }

    /**
     * 수리. 고친 뒤의 남은 비율(%)을 돌려준다. [points] 는 고칠 내구도(서버 p_points, 0059) — 화면이 보인 견적만큼만 고치고
     * 치른다. 비우면(null) 서버가 그때 빈 만큼 가득 고친다 — 견적과 실행 사이에 러닝이 올라가 더 닳았으면 견적보다 많이 치렀다.
     * [request] 는 요청 번호 — 응답을 잃은 같은 요청을 다시 보낼 때 같은 번호면 서버가 다시 치르지 않는다(0057)
     */
    suspend fun repair(id: Long, points: Int? = null, request: String? = null): ServerResult<Double> =
        rpc(
            "sneaker_repair",
            jsonBody {
                put("p_id", id)
                points?.let { put("p_points", it) }
                request?.let { put("p_request", it) }
            },
        ) { it.trim().toDoubleOrNull() }

    suspend fun equip(id: Long): ServerResult<Unit> =
        rpc("sneaker_equip", jsonBody { put("p_id", id) }) { Unit }

    /**
     * 스탯 포인트 나누기(0059) — 더할 값이 아니라 나눈 뒤의 합계(효율 · 착화감 · 내구도)를 보낸다. 늘리기만 된다
     * (다시 나누기가 남은 신발만 한 번 줄일 수 있다). 남은 포인트를 돌려준다. [request] 가 같으면 서버가 처음 결과를 돌려준다.
     */
    suspend fun setPoints(id: Long, efficiency: Int, comfort: Int, durability: Int, request: String? = null): ServerResult<Int> =
        rpc(
            "sneaker_points_set",
            jsonBody {
                put("p_id", id)
                put("p_efficiency", efficiency)
                put("p_comfort", comfort)
                put("p_durability", durability)
                request?.let { put("p_request", it) }
            },
        ) { it.trim().toIntOrNull() }

    /** 부스터 구매. 남은 잔고를 돌려준다. */
    suspend fun boostBuy(kind: String, request: String? = null): ServerResult<Double> =
        rpc("boost_buy", jsonBody { put("p_kind", kind); request?.let { put("p_request", it) } }) { it.trim().toDoubleOrNull() }

    /** 하루 목표를 서버에 적는다 — 목표 달성 보너스를 서버가 이 값으로 판정한다. 적힌 값을 돌려준다. */
    suspend fun setDailyGoal(goal: Int): ServerResult<Int> =
        rpc("profile_set_daily_goal", jsonBody { put("p_goal", goal) }) { it.trim().toIntOrNull() }

    /** 오늘 목표 달성 보너스. 서버가 확인한 러닝 걸음으로 판정한다. */
    suspend fun goalClaim(): ServerResult<Double> =
        rpc("goal_claim", "{}") { it.trim().toDoubleOrNull() }

    /**
     * [day](한국 시각 게임의 하루)의 목표 달성 보너스(0068) — 러닝은 시작한 날의 목표에 든다. 자정을 넘긴 러닝 · 자정 뒤에 올린
     * 러닝도 그날 보너스를 받는다. 서버는 최근 7일 안의 날만 받고, 같은 날은 한 번만 준다(goal_claim 과 같은 꼬리표).
     * 서버에 아직 이 함수가 없으면(PostgREST PGRST202 "Could not find the function") 예전처럼 오늘의 보너스를 청구한다.
     */
    suspend fun goalClaimDay(day: java.time.LocalDate): ServerResult<Double> {
        val result = rpc("goal_claim_day", jsonBody { put("p_day", day.toString()) }) { it.trim().toDoubleOrNull() }
        return if (result.missingServerFunction()) goalClaim() else result
    }

    private suspend fun <T> rpc(name: String, body: String, parse: (String) -> T?): ServerResult<T> =
        server.authed { token ->
            server.http.post("${server.restUrl}/rpc/$name", body, server.headers(token))
        }.mapBody(parse)

    companion object {
        /** 게임의 하루 — 한국 시각(서버 economy.game_day) */
        private val GAME_ZONE: java.time.ZoneId = java.time.ZoneId.of("Asia/Seoul")

        /** 서버가 목표 보너스를 받는 지난날 수(0068 goal_claim_day — 오늘과 그 전 7일) */
        const val GOAL_CLAIM_WINDOW_DAYS = 7L

        /**
         * 러닝이 확인받았을 때([signedStartedAt]) 목표 보너스를 청구할 날 — 서버가 받는 최근 7일부터 [now] 의 오늘까지, 오래된 날부터.
         * 러닝은 시작한 날(한국 시각)의 목표에 들고, 오래된 날부터 받아야 뒷날의 연속 일수가 앞날의 보너스를 센다(서버는 그 전날부터
         * 거꾸로 센다 — 0068). 이미 받은 날 · 못 채운 날은 서버가 거절할 뿐 다른 일은 없다(같은 날은 한 번).
         * 아직 올리지 못한 러닝([pendingStartedAt])이 있으면 그 러닝이 시작한 날 뒤는 미룬다 — 먼저 받으면 그날이 빠진 짧은 연속 일수로
         * 굳는다(Codex 리뷰). 그 러닝이 올라가거나 7일 밖으로 나가면(그날은 더 받을 수 없다) 미룬 날을 다음 확인 때 받는다 — 창 전체를
         * 다시 보므로 막혔던 날도 7일 안이면 빠지지 않는다.
         */
        fun goalDays(
            signedStartedAt: Collection<Long>,
            pendingStartedAt: Collection<Long> = emptyList(),
            now: Long = System.currentTimeMillis(),
        ): List<java.time.LocalDate> {
            if (signedStartedAt.isEmpty()) return emptyList()
            val today = gameDay(now)
            val from = today.minusDays(GOAL_CLAIM_WINDOW_DAYS)
            val waiting = pendingStartedAt.map { gameDay(it) }.filter { !it.isBefore(from) }.minOrNull()
            val until = if (waiting == null) today else minOf(today, waiting)
            return generateSequence(from) { it.plusDays(1) }.takeWhile { !it.isAfter(until) }.toList()
        }

        private fun gameDay(epochMs: Long): java.time.LocalDate =
            java.time.Instant.ofEpochMilli(epochMs).atZone(GAME_ZONE).toLocalDate()
    }
}

/** PostgREST 가 이름 · 인자에 맞는 서버 함수를 못 찾았다(PGRST202 — 서버에 새 함수가 아직 없다) */
private fun ServerResult<*>.missingServerFunction(): Boolean =
    this is ServerResult.Rejected && ("Could not find the function" in reason || "PGRST202" in reason)

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
    // ── 0059 — 신발마다 다른 최대 내구도(1 = 10m) · 레벨업 스탯 포인트. 예전 서버는 보내지 않는다(0 · false) ──
    /** 남은 내구도 · 최대 내구도 · 만들 때 고른 최대 내구도(1 = 10m). [durability] 는 남은 비율(%) */
    @SerialName("durability_now") val durabilityNow: Int = 0,
    @SerialName("durability_max") val durabilityMax: Int = 0,
    @SerialName("durability_base") val durabilityBase: Int = 0,
    /** 레벨 1 의 효율 · 착화감(bps) — [efficiencyBps] · [comfortBps] 는 여기에 나눈 포인트를 더한 값 */
    @SerialName("efficiency_base_bps") val efficiencyBaseBps: Int = 0,
    @SerialName("comfort_base_bps") val comfortBaseBps: Int = 0,
    /** 받은 포인트(레벨마다 4) · 남은 포인트 · 효율 · 착화감 · 내구도에 나눈 포인트 */
    @SerialName("points_total") val pointsTotal: Int = 0,
    @SerialName("points_free") val pointsFree: Int = 0,
    @SerialName("points_efficiency") val pointsEfficiency: Int = 0,
    @SerialName("points_comfort") val pointsComfort: Int = 0,
    @SerialName("points_durability") val pointsDurability: Int = 0,
    /** 한 번 처음부터 다시 나눌 수 있다(0059 전에 강화한 신발) */
    @SerialName("points_respec") val pointsRespec: Boolean = false,
    /** 내구도 1(10m) 고치는 값 — [repairCostPerPoint] 는 예전 앱이 읽는 1% 값 */
    @SerialName("repair_cost_per_unit") val repairCostPerUnit: Double = 0.0,
)

@Serializable
data class ChainSettingsRow(
    @SerialName("wallet_recipient") val walletRecipient: Boolean = false,
    @SerialName("wallet_linked") val walletLinked: Boolean = false,
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
