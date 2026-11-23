package com.stepup.android.data.repo

import com.stepup.android.data.remote.ServerResult
import com.stepup.android.data.remote.isMissingForgeXp
import com.stepup.android.domain.Faction
import com.stepup.android.domain.ForgeBar
import com.stepup.android.domain.ForgeKind
import com.stepup.android.domain.ForgeMaterial
import com.stepup.android.domain.ForgeMaterialBlock
import com.stepup.android.domain.ForgePending
import com.stepup.android.domain.ForgeQuote
import com.stepup.android.domain.ForgeResult
import com.stepup.android.domain.ForgeStats
import com.stepup.android.domain.ForgeTarget
import com.stepup.android.domain.ForgePrecious
import com.stepup.android.domain.ForgeTargetBlock
import com.stepup.android.domain.ForgeXp
import com.stepup.android.domain.Rarity
import com.stepup.android.domain.Sneaker
import com.stepup.android.domain.XpCatalog
import com.stepup.android.domain.XpCrit
import com.stepup.android.domain.XpMaterial
import com.stepup.android.domain.XpQuote
import com.stepup.android.domain.XpQuoteMaterial
import com.stepup.android.domain.XpResult
import com.stepup.android.domain.XpResultMaterial
import com.stepup.android.domain.XpTarget
import java.util.UUID
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.booleanOrNull
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.doubleOrNull
import kotlinx.serialization.json.intOrNull
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.longOrNull

/** 서버의 강화 함수(0054, 규칙 forge-v2) — EconomyApi 가 구현한다. 검사에서는 흉내 낸 서버를 넣는다 */
interface ForgeServer {
    suspend fun forgeMaterials(target: Long): ServerResult<String>
    suspend fun forgeQuote(target: Long, materials: List<Long>): ServerResult<String>
    suspend fun forgeStart(key: String, target: Long, materials: List<Long>, quoteVersion: String): ServerResult<String>
    suspend fun forgeResult(key: String): ServerResult<String>
}

/** 서버의 경험치 강화 함수(0118, 규칙 forge-xp-1) — ForgeXpApi 가 구현한다. 검사에서는 흉내 낸 서버를 넣는다 */
interface ForgeXpServer {
    suspend fun forgeXpMaterials(target: Long): ServerResult<String>
    suspend fun forgeXpQuote(target: Long, materials: List<Long>): ServerResult<String>
    suspend fun forgeXpAdd(key: String, target: Long, materials: List<Long>, quoteVersion: String): ServerResult<String>
    suspend fun forgeXpResult(key: String): ServerResult<String>
}

/** 보낸 요청의 저장 자리 — 앱을 다시 켜도 같은 요청 키로 결과만 묻는다 */
interface ForgePendingStore {
    suspend fun load(): ForgePending?
    suspend fun save(pending: ForgePending?)

    /** 결과를 아직 모르는 요청 — 신발 상세가 "강화 결과 확인"과 막힘을 정한다 */
    val pending: Flow<ForgePending?> get() = flowOf(null)
}

/** 강화 화면을 열 때 */
sealed interface ForgeLoad {
    /** 예전 확률 강화(서버에 0118 이 없다) */
    data class Ready(val target: ForgeTarget, val materials: List<ForgeMaterial>) : ForgeLoad

    /** 경험치 강화(0118) */
    data class XpReady(val catalog: XpCatalog) : ForgeLoad

    /** 경험치 강화 — 대상을 강화할 수 없다(서버 거절: 없어짐 · 쓸 수 없음 · 예전 신발 · 만렙 · 크루 러닝 중) */
    data class XpBlocked(val block: ForgeTargetBlock) : ForgeLoad
    data object SignInRequired : ForgeLoad
    data object Failed : ForgeLoad
}

/** 견적 · 실행 · 결과 조회의 결말 */
sealed interface ForgeOutcome {
    data class Quoted(val quote: ForgeQuote) : ForgeOutcome

    /** 확률 판정까지 끝났다 — 이때만 재료 소각 완료를 말한다 */
    data class Done(val result: ForgeResult) : ForgeOutcome

    /** 서버가 이 요청 키를 받지 않았다고 확정했다 — 재료는 그대로다 */
    data object NotAccepted : ForgeOutcome

    /** 보내기 전에 끊겼다 — 요청을 보내지 않았다(14) */
    data object NotSent : ForgeOutcome

    /** 보냈는지 · 처리됐는지 모른다(15) — 같은 키로 결과만 묻는다 */
    data object Unknown : ForgeOutcome

    /** 실행 직전 다시 보니 견적이 달라졌다 — 새 견적으로 다시 확인받는다 */
    data class QuoteChanged(val quote: ForgeQuote) : ForgeOutcome

    /** 재료 중 일부를 실제로 쓸 수 없다(12) — 그것만 뺀다. [blocks] 는 경험치 강화가 함께 준 까닭(예전 서버는 비어 있다) */
    data class MaterialsUnavailable(
        val ids: List<Long>,
        val blocks: Map<Long, ForgeMaterialBlock> = emptyMap(),
    ) : ForgeOutcome

    /** 대상이 바뀌었다(13 · 18) */
    data class TargetBlocked(val block: ForgeTargetBlock) : ForgeOutcome

    data object SignInRequired : ForgeOutcome

    /** 그 밖의 거절 — 확률 실패가 아니다. [reason] 은 서버 거절 코드(화면이 코드마다 글을 고른다) */
    data class Rejected(val reason: String) : ForgeOutcome

    // ── 경험치 강화(0118) ──

    /** 견적 — [XpQuote.after] 는 대성공 없이 받는 최소 */
    data class XpQuoted(val quote: XpQuote) : ForgeOutcome

    /** 넣기가 끝났다 — 실패는 없다. 이때만 재료가 탔다고 말한다 */
    data class XpDone(val result: XpResult) : ForgeOutcome

    /** 보내기 직전 · 서버가 넣기 전에 다시 보니 견적이 달라졌다 — 넣지 않았다. [quote] 는 새 견적(서버가 주지 않으면 null) */
    data class XpQuoteChanged(val quote: XpQuote?) : ForgeOutcome

    /** 닳아서 값이 0 인 재료(그것만 뺀다) — 수리하면 다시 넣을 수 있다 */
    data class NoValue(val ids: List<Long>) : ForgeOutcome

    /** 재료 목록이 맞지 않는다(0켤레 · 상한 넘음 · 같은 신발 두 번) */
    data object MaterialsInvalid : ForgeOutcome

    /** 같은 요청 번호로 다른 대상 · 재료를 보냈다 — 넣지 않았다 */
    data object KeyReused : ForgeOutcome
}

/**
 * 신발 강화 — 서버 계약을 화면의 말로 옮긴다. 클라이언트는 성공 · 대성공을 굴리지 않고, 신발을 먼저 지우지 않는다.
 *
 * 경험치 강화(0118 forge_xp_*)가 먼저다. 서버에 그 함수가 없으면(PGRST202) 예전 확률 강화(0054 forge_*)로 돈다 — 한 번 없다고 확인하면
 * 이 앱이 켜져 있는 동안은 묻지 않는다.
 *
 * 실행 순서(둘 다): 견적을 다시 받아(보내기 전 연결 확인을 겸한다 — 여기서 끊기면 "보내지 않음") → 요청 키를 저장 → 보낸다.
 * 응답을 잃으면 같은 키로 결과를 묻는다. 경험치 강화는 서버에 기록이 없으면(아직 닿지 않았다) 같은 키로 한 번 더 보낸다 — 같은 키는
 * 서버가 한 번만 처리하고, 견적이 바뀌었으면 넣지 않는다(QUOTE_CHANGED). 그래도 모르면 [ForgeOutcome.Unknown].
 * 결과가 확정되면(넣음 · 성공 · 실패 · 접수 안 됨 · 거절) 저장한 키를 지우고 서버 목록을 다시 받는다. 계정마다 걸려 있는 요청은 하나다.
 */
class ForgeRepository(
    private val server: ForgeServer?,
    private val pendingStore: ForgePendingStore,
    private val resync: suspend () -> Unit = {},
    private val now: () -> Long = System::currentTimeMillis,
    private val newKey: () -> String = { UUID.randomUUID().toString() },
    /** 경험치 강화(0118). 없으면(검사 · 서버 경제가 없는 빌드) 예전 강화만 */
    private val xp: ForgeXpServer? = null,
    /**
     * 넣기가 확정된 뒤(서버 결과) — 폰의 신발 사본에 대상의 통(넣은 뒤 점수 · 통 크기)을 바로 적는다. 신발 목록을 다시 받지 못해도
     * 신발 카드 · 상세의 경험치 줄이 맞게(목록 다시 받기 [resync] 는 그대로 한다)
     */
    private val onXpDone: suspend (XpResult) -> Unit = {},
) {
    /** 서버에 경험치 강화가 없다고 확인했다(PGRST202) — 이 앱이 켜져 있는 동안은 예전 강화로 */
    @Volatile private var xpMissing = false

    suspend fun pending(): ForgePending? = pendingStore.load()

    /** 결과를 아직 모르는 요청(없으면 null) — 상세 · 강화 화면이 같은 요청을 본다 */
    val pendingFlow: Flow<ForgePending?> get() = pendingStore.pending

    /** 경험치 강화를 쓸 수 있을 것 같은가(서버에 없다고 확인하기 전까지는 그렇다고 본다) */
    val xpAvailable: Boolean get() = xp != null && !xpMissing

    /** 강화 화면을 연다 — 경험치 강화가 있으면 그것, 없으면 예전 강화 */
    suspend fun load(targetId: Long): ForgeLoad {
        val api = xp?.takeIf { !xpMissing }
        if (api != null) {
            val r = api.forgeXpMaterials(targetId)
            if (r.isMissingForgeXp()) {
                xpMissing = true
            } else {
                return when (r) {
                    is ServerResult.Ok -> runCatching { parseXpLoad(r.value) }.getOrNull() ?: ForgeLoad.Failed
                    is ServerResult.SignInRequired -> ForgeLoad.SignInRequired
                    else -> ForgeLoad.Failed
                }
            }
        }
        return loadChance(targetId)
    }

    private suspend fun loadChance(targetId: Long): ForgeLoad {
        val api = server ?: return ForgeLoad.SignInRequired
        return when (val r = api.forgeMaterials(targetId)) {
            is ServerResult.Ok -> runCatching { parseLoad(r.value) }.getOrNull() ?: ForgeLoad.Failed
            is ServerResult.SignInRequired -> ForgeLoad.SignInRequired
            else -> ForgeLoad.Failed
        }
    }

    /**
     * 뽑기 결과의 "재료로 쓰면 ○○ +N%" 줄 — 대상 [targetId] 의 재료 목록(서버 값)을 읽는다. 서버에 경험치 강화가 없거나 읽지 못하면 null
     * (줄을 띄우지 않는다). 아무것도 바꾸지 않는다.
     */
    suspend fun xpCatalog(targetId: Long): XpCatalog? {
        val api = xp?.takeIf { !xpMissing } ?: return null
        val r = api.forgeXpMaterials(targetId)
        if (r.isMissingForgeXp()) {
            xpMissing = true
            return null
        }
        val load = (r as? ServerResult.Ok)?.let { runCatching { parseXpLoad(it.value) }.getOrNull() }
        return (load as? ForgeLoad.XpReady)?.catalog
    }

    /** 3개를 고른 뒤의 견적. 끊기면 [ForgeOutcome.NotSent] — 아무것도 보내지 않았다 */
    suspend fun quote(targetId: Long, materials: List<Long>): ForgeOutcome {
        val api = server ?: return ForgeOutcome.SignInRequired
        return when (val r = api.forgeQuote(targetId, materials)) {
            is ServerResult.Ok -> parseQuoteOrError(r.value, materials)
            is ServerResult.SignInRequired -> ForgeOutcome.SignInRequired
            is ServerResult.Retry -> ForgeOutcome.NotSent
            is ServerResult.Rejected -> ForgeOutcome.Rejected(r.reason)
        }
    }

    /**
     * 확인한 [confirmed] 견적으로 강화한다. 보내기 전에 견적을 다시 받아 달라졌으면 실행하지 않는다.
     * 저장된 미확인 요청이 있으면 새 요청을 만들지 않는다 — 그 결과부터 확인한다.
     */
    suspend fun start(targetId: Long, confirmed: ForgeQuote): ForgeOutcome {
        val api = server ?: return ForgeOutcome.SignInRequired
        pendingStore.load()?.let { return check(it) }
        // 보내기 전 확인 — 여기서 끊기면 요청을 보내지 않았다는 것이 확실하다
        when (val fresh = quote(targetId, confirmed.materialIds)) {
            is ForgeOutcome.Quoted -> if (fresh.quote.quoteVersion != confirmed.quoteVersion) {
                return ForgeOutcome.QuoteChanged(fresh.quote)
            }
            else -> return fresh
        }
        val pending = ForgePending(newKey(), targetId, confirmed.materialIds, confirmed.ratePermille, now())
        pendingStore.save(pending)
        val sent = api.forgeStart(pending.requestKey, targetId, confirmed.materialIds, confirmed.quoteVersion)
        val outcome = when (sent) {
            is ServerResult.Ok -> runCatching { parseResultOrError(sent.value, confirmed.materialIds) }.getOrNull()
            else -> null
        }
        return if (outcome == null) recheck(pending) else settle(outcome)
    }

    // ── 경험치 강화 ───────────────────────────────────────────────

    /** 고른 재료(1 ~ 상한)의 견적 — 대성공 없이 받는 최소. 끊기면 [ForgeOutcome.NotSent] */
    suspend fun xpQuote(targetId: Long, materials: List<Long>): ForgeOutcome {
        val api = xp ?: return ForgeOutcome.SignInRequired
        return when (val r = api.forgeXpQuote(targetId, materials)) {
            is ServerResult.Ok -> parseXpQuoteOrError(r.value, materials)
            is ServerResult.SignInRequired -> ForgeOutcome.SignInRequired
            is ServerResult.Retry -> ForgeOutcome.NotSent
            is ServerResult.Rejected -> ForgeOutcome.Rejected(r.reason)
        }
    }

    /**
     * 확인한 [confirmed] 견적으로 재료를 넣는다. 보내기 전에 견적을 다시 받아 달라졌으면 넣지 않는다([ForgeOutcome.XpQuoteChanged]).
     * 저장된 미확인 요청이 있으면(예전 강화의 요청이라도) 새 요청을 만들지 않는다 — 그 결과부터 확인한다.
     */
    suspend fun xpAdd(targetId: Long, confirmed: XpQuote): ForgeOutcome {
        val api = xp ?: return ForgeOutcome.SignInRequired
        pendingStore.load()?.let { return check(it) }
        when (val fresh = xpQuote(targetId, confirmed.materialIds)) {
            is ForgeOutcome.XpQuoted -> if (fresh.quote.quoteVersion != confirmed.quoteVersion) {
                return ForgeOutcome.XpQuoteChanged(fresh.quote)
            }
            else -> return fresh
        }
        val pending = ForgePending(
            requestKey = newKey(), targetId = targetId, materialIds = confirmed.materialIds, ratePermille = 0, createdAt = now(),
            kind = ForgeKind.XP, quoteVersion = confirmed.quoteVersion,
        )
        pendingStore.save(pending)
        val outcome = sendXp(api, pending)
        return if (outcome == null) recheck(pending) else settle(outcome)
    }

    /** 같은 요청 번호로 보낸다 — 읽지 못한 답(끊김 · 알 수 없는 모양)은 null */
    private suspend fun sendXp(api: ForgeXpServer, pending: ForgePending): ForgeOutcome? =
        when (val sent = api.forgeXpAdd(pending.requestKey, pending.targetId, pending.materialIds, pending.quoteVersion)) {
            is ServerResult.Ok -> runCatching { parseXpResultOrError(sent.value) }.getOrNull()
            // 서버가 거절했다(4xx) — 넣지 않았다
            is ServerResult.Rejected -> if (sent.isMissingForgeXp()) null else ForgeOutcome.Rejected(sent.reason)
            else -> null
        }

    /** 15 "결과 다시 확인" — 같은 요청 키의 결과만 묻는다. 새 요청을 만들지 않는다 */
    suspend fun check(pending: ForgePending): ForgeOutcome = recheck(pending)

    private suspend fun recheck(pending: ForgePending): ForgeOutcome =
        if (pending.kind == ForgeKind.XP) recheckXp(pending) else recheckChance(pending)

    private suspend fun recheckChance(pending: ForgePending): ForgeOutcome {
        val api = server ?: return ForgeOutcome.Unknown
        val outcome = when (val r = api.forgeResult(pending.requestKey)) {
            is ServerResult.Ok -> runCatching { parseResultOrError(r.value, pending.materialIds) }.getOrNull()
            else -> null
        } ?: return ForgeOutcome.Unknown
        return settle(outcome)
    }

    /**
     * 경험치 강화의 결과 — 서버에 기록이 있으면 그 결과. 없으면(`null` — 요청이 아직 닿지 않았다) 같은 번호로 한 번 더 보낸다:
     * 같은 번호는 서버가 한 번만 처리하고, 그사이 값이 바뀌었으면 넣지 않는다. 견적 서명을 모르면(저장 줄이 깨졌다) 더 보내지 않는다.
     */
    private suspend fun recheckXp(pending: ForgePending): ForgeOutcome {
        val api = xp ?: return ForgeOutcome.Unknown
        val r = api.forgeXpResult(pending.requestKey)
        val body = (r as? ServerResult.Ok)?.value?.trim() ?: return ForgeOutcome.Unknown
        val outcome = if (body.isEmpty() || body == "null") {
            if (pending.quoteVersion.isBlank()) return ForgeOutcome.Unknown
            sendXp(api, pending)
        } else {
            runCatching { parseXpResultOrError(body) }.getOrNull()
        } ?: return ForgeOutcome.Unknown
        return settle(outcome)
    }

    /** 확정된 결말이면 저장한 키를 지우고 서버 목록을 맞춘다 */
    private suspend fun settle(outcome: ForgeOutcome): ForgeOutcome {
        if (outcome !is ForgeOutcome.Unknown) {
            pendingStore.save(null)
            if (outcome is ForgeOutcome.XpDone) runCatching { onXpDone(outcome.result) }
            if (outcome is ForgeOutcome.Done || outcome is ForgeOutcome.XpDone) runCatching { resync() }
        }
        return outcome
    }

    // ── 응답 읽기 ─────────────────────────────────────────────────

    private fun obj(body: String): JsonObject = Json.parseToJsonElement(body.trim()).jsonObject

    private fun JsonObject.str(key: String): String? = (this[key] as? JsonPrimitive)?.takeIf { it !is JsonNull }?.contentOrNull
    private fun JsonObject.int(key: String): Int? = (this[key] as? JsonPrimitive)?.intOrNull
    private fun JsonObject.long(key: String): Long? = (this[key] as? JsonPrimitive)?.longOrNull
    private fun JsonObject.dbl(key: String): Double? = (this[key] as? JsonPrimitive)?.doubleOrNull
    private fun JsonObject.bool(key: String): Boolean? = (this[key] as? JsonPrimitive)?.booleanOrNull

    /** 점수 — 정수로 오지만 numeric(1007.0)이어도 버려서 읽는다(부풀리지 않게) */
    private fun JsonObject.points(key: String): Int? =
        int(key) ?: dbl(key)?.takeIf { it.isFinite() }?.let { kotlin.math.floor(it).toInt() }
    private fun JsonObject.child(key: String): JsonObject? = this[key] as? JsonObject
    private fun JsonObject.ids(key: String): List<Long> =
        (this[key] as? JsonArray)?.mapNotNull { (it as? JsonPrimitive)?.longOrNull ?: (it as? JsonObject)?.long("id") }.orEmpty()

    private fun stats(o: JsonObject?): ForgeStats? {
        o ?: return null
        return ForgeStats(
            level = o.int("level") ?: return null,
            maxLevel = o.int("max_level") ?: return null,
            efficiencyBps = o.int("efficiency_bps") ?: 0,
            comfortBps = o.int("comfort_bps") ?: 0,
            pointsTotal = o.int("points_total") ?: 0,
            pointsFree = o.int("points_free") ?: 0,
        )
    }

    internal fun parseLoad(body: String): ForgeLoad {
        val root = obj(body)
        val t = root.child("target") ?: return ForgeLoad.Failed
        val block = ForgeTargetBlock.of(t.str("block"))
        val id = t.long("id") ?: return ForgeLoad.Failed
        if (block == ForgeTargetBlock.TARGET_GONE) {
            return ForgeLoad.Ready(
                ForgeTarget(id, Rarity.COMMON, ForgeStats(0, 0, 0, 0), 0, block), emptyList(),
            )
        }
        val target = ForgeTarget(
            id = id,
            rarity = Rarity.of(t.str("rarity") ?: return ForgeLoad.Failed),
            stats = stats(t) ?: return ForgeLoad.Failed,
            basePermille = t.int("base_permille") ?: return ForgeLoad.Failed,
            block = block,
        )
        val materials = (root["materials"] as? JsonArray).orEmpty().map { element ->
            val m = element.jsonObject
            val rarity = Rarity.of(m.str("rarity").orEmpty())
            val model = m.int("model_id")
            ForgeMaterial(
                shoe = Sneaker(
                    id = m.long("id") ?: throw IllegalStateException("id"),
                    faction = Faction.of(m.str("faction").orEmpty()),
                    rarity = rarity,
                    variant = m.int("variant") ?: 0,
                    level = m.int("level") ?: throw IllegalStateException("level"),
                    mintNumber = (m.long("mint_number") ?: 0L).toInt(),
                    luck = 1.0,
                    comfort = 1.0,
                    durability = 100,
                    equipped = false,
                    acquiredAt = 0L,
                    modelId = model,
                ),
                bonusPermille = m.int("bonus_permille") ?: 0,
                block = ForgeMaterialBlock.of(m.str("block")),
            )
        }
        return ForgeLoad.Ready(target, materials)
    }

    private fun errorOf(root: JsonObject): ForgeOutcome? {
        val code = root.str("error") ?: return null
        ForgeTargetBlock.of(code)?.let { return ForgeOutcome.TargetBlocked(it) }
        return when (code) {
            "MATERIAL_UNAVAILABLE" -> ForgeOutcome.MaterialsUnavailable(root.ids("unavailable"))
            "QUOTE_CHANGED" -> root.child("quote")?.let { q -> quoteOf(q)?.let { ForgeOutcome.QuoteChanged(it) } }
                ?: ForgeOutcome.Rejected(code)
            else -> ForgeOutcome.Rejected(code)
        }
    }

    private fun quoteOf(q: JsonObject, materials: List<Long>? = null): ForgeQuote? {
        return ForgeQuote(
            ratePermille = q.int("rate_permille") ?: return null,
            before = stats(q.child("before")) ?: return null,
            after = stats(q.child("after")) ?: return null,
            quoteVersion = q.str("quote_version") ?: return null,
            materialIds = materials ?: (q["materials"] as? JsonArray)?.mapNotNull { it.jsonObject.long("id") }.orEmpty(),
        )
    }

    internal fun parseQuoteOrError(body: String, materials: List<Long>): ForgeOutcome {
        val root = runCatching { obj(body) }.getOrNull() ?: return ForgeOutcome.Rejected("unreadable")
        errorOf(root)?.let { return it }
        return quoteOf(root, materials)?.let { ForgeOutcome.Quoted(it) } ?: ForgeOutcome.Rejected("unreadable")
    }

    internal fun parseResultOrError(body: String, materials: List<Long>): ForgeOutcome? {
        val root = obj(body)
        errorOf(root)?.let { return it }
        return when (root.str("status")) {
            "SUCCESS", "FAILED" -> ForgeOutcome.Done(
                ForgeResult(
                    success = root.str("status") == "SUCCESS",
                    targetId = root.long("target_id") ?: return null,
                    materialIds = root.ids("material_ids").ifEmpty { materials },
                    ratePermille = root.int("rate_permille") ?: return null,
                    levelBefore = root.int("level_before") ?: return null,
                    levelAfter = root.int("level_after") ?: return null,
                    target = stats(root.child("target")),
                ),
            )
            "NOT_ACCEPTED" -> ForgeOutcome.NotAccepted
            else -> null
        }
    }

    // ── 경험치 강화 응답 읽기(0118 — docs/forge-xp-daily-contract-2026-11-19.md) ──────────────

    private fun bar(o: JsonObject?, fallbackLevel: Int? = null): ForgeBar? {
        o ?: return null
        val level = o.int("level") ?: fallbackLevel ?: return null
        val need = o.points("need")?.takeIf { it > 0 }
        val xpPoints = o.points("xp") ?: 0
        return ForgeBar(level, xpPoints.coerceAtLeast(0), need, if (need == null) null else ForgeXp.tenthsOf(o.dbl("pct")))
    }

    private fun crit(o: JsonObject?): XpCrit? {
        o ?: return null
        val chance = o.dbl("chance") ?: return null
        return XpCrit(chance, o.dbl("mult") ?: 1.0)
    }

    private fun xpTargetBlock(root: JsonObject): ForgeTargetBlock? = ForgeTargetBlock.of(root.str("error"))

    internal fun parseXpLoad(body: String): ForgeLoad {
        val root = obj(body)
        xpTargetBlock(root)?.let { return ForgeLoad.XpBlocked(it) }
        if (root.str("error") != null) return ForgeLoad.Failed
        val t = root.child("target") ?: return ForgeLoad.Failed
        val rarity = Rarity.of(t.str("rarity") ?: return ForgeLoad.Failed)
        val target = XpTarget(
            id = t.long("id") ?: return ForgeLoad.Failed,
            rarity = rarity,
            maxLevel = t.int("max_level") ?: rarity.maxLevel,
            bar = bar(t) ?: return ForgeLoad.Failed,
        )
        val items = (root["items"] as? JsonArray).orEmpty().mapNotNull { element ->
            val m = element as? JsonObject ?: return@mapNotNull null
            val id = m.long("id") ?: return@mapNotNull null
            val itemRarity = Rarity.of(m.str("rarity").orEmpty())
            XpMaterial(
                shoe = Sneaker(
                    id = id,
                    faction = Faction.of(m.str("faction").orEmpty()),
                    rarity = itemRarity,
                    variant = (m.int("variant") ?: 0).coerceIn(0, itemRarity.variantCount - 1),
                    level = m.int("level") ?: 1,
                    mintNumber = (m.long("mint_number") ?: 0L).toInt(),
                    luck = 1.0,
                    comfort = 1.0,
                    durability = m.dbl("durability_pct")?.toInt() ?: 100,
                    equipped = m.str("block") == "EQUIPPED",
                    acquiredAt = 0L,
                    modelId = m.int("model_id"),
                ),
                stars = m.int("stars")?.takeIf { it in 1..3 },
                durabilityPct = m.dbl("durability_pct"),
                own = m.points("own") ?: 0,
                inherit = m.points("inherit") ?: 0,
                repairPart = m.points("repair_part") ?: 0,
                value = (m.points("value") ?: 0).coerceAtLeast(0),
                tenths = ForgeXp.tenthsOf(m.dbl("pct")) ?: ForgeXp.tenths(m.points("value") ?: 0, target.bar.need) ?: 0,
                block = ForgeMaterialBlock.of(m.str("block")),
                precious = (m["precious"] as? JsonArray).orEmpty()
                    .mapNotNull { (it as? JsonPrimitive)?.contentOrNull?.let(ForgePrecious::of) }.distinct(),
            )
        }
        return ForgeLoad.XpReady(
            XpCatalog(
                target = target,
                items = items,
                crit = crit(root.child("crit")),
                maxMaterials = (root.int("max_materials") ?: ForgeXp.DEFAULT_MAX_MATERIALS).coerceAtLeast(1),
                ruleVersion = root.str("rule_version") ?: ForgeXp.RULE_VERSION,
            ),
        )
    }

    private fun xpQuoteOf(q: JsonObject, materials: List<Long>? = null): XpQuote? {
        val before = bar(q.child("before")) ?: return null
        val afterObj = q.child("after") ?: return null
        val after = bar(afterObj) ?: return null
        val quoted = (q["materials"] as? JsonArray).orEmpty().mapNotNull { element ->
            val m = element as? JsonObject ?: return@mapNotNull null
            XpQuoteMaterial(
                id = m.long("id") ?: return@mapNotNull null,
                own = m.points("own") ?: 0,
                inherit = m.points("inherit") ?: 0,
                value = m.points("value") ?: 0,
                tenths = ForgeXp.tenthsOf(m.dbl("pct")),
            )
        }
        val energy = q.child("energy")
        val cap = q.child("cap")
        return XpQuote(
            quoteVersion = q.str("quote_version") ?: return null,
            before = before,
            after = after,
            levels = (afterObj.int("levels") ?: (after.level - before.level)).coerceAtLeast(0),
            overflow = (afterObj.points("overflow") ?: 0).coerceAtLeast(0),
            gain = (q.points("gain") ?: 0).coerceAtLeast(0),
            materials = quoted,
            crit = crit(q.child("crit")),
            energyBefore = energy?.points("before"),
            energyAfter = energy?.points("after"),
            capBefore = cap?.dbl("before"),
            capAfter = cap?.dbl("after"),
            maxLevel = q.int("max_level") ?: after.level,
            overflowPossible = q.bool("overflow_possible") ?: false,
            materialIds = materials ?: quoted.map { it.id },
        )
    }

    /** 경험치 강화의 거절 코드 → 결말. 모르는 코드는 [ForgeOutcome.Rejected](화면이 "처리하지 못했어요" — 연결 끊김으로 읽지 않는다) */
    private fun xpErrorOf(root: JsonObject): ForgeOutcome? {
        val code = root.str("error") ?: return null
        ForgeTargetBlock.of(code)?.let { return ForgeOutcome.TargetBlocked(it) }
        return when (code) {
            "MATERIAL_UNAVAILABLE" -> {
                val blocks = (root["unavailable"] as? JsonArray).orEmpty().mapNotNull { element ->
                    val o = element as? JsonObject ?: return@mapNotNull null
                    val id = o.long("id") ?: return@mapNotNull null
                    id to (ForgeMaterialBlock.of(o.str("block")) ?: ForgeMaterialBlock.UNKNOWN)
                }.toMap()
                ForgeOutcome.MaterialsUnavailable(root.ids("unavailable"), blocks)
            }
            "NO_VALUE" -> ForgeOutcome.NoValue(root.ids("ids"))
            "MATERIALS_INVALID" -> ForgeOutcome.MaterialsInvalid
            "KEY_REUSED" -> ForgeOutcome.KeyReused
            "QUOTE_CHANGED" -> ForgeOutcome.XpQuoteChanged(root.child("quote")?.let { xpQuoteOf(it) })
            else -> ForgeOutcome.Rejected(code)
        }
    }

    internal fun parseXpQuoteOrError(body: String, materials: List<Long>): ForgeOutcome {
        val root = runCatching { obj(body) }.getOrNull() ?: return ForgeOutcome.Rejected("unreadable")
        xpErrorOf(root)?.let { return it }
        return xpQuoteOf(root, materials)?.let { ForgeOutcome.XpQuoted(it) } ?: ForgeOutcome.Rejected("unreadable")
    }

    /** 넣기 · 결과 조회의 답. 읽지 못하면 null(보냈는지 모른다 — 같은 번호로 다시 묻는다) */
    internal fun parseXpResultOrError(body: String): ForgeOutcome? {
        val root = obj(body)
        xpErrorOf(root)?.let { return it }
        val targetId = root.long("target_id") ?: return null
        val levelBefore = root.int("level_before") ?: return null
        val levelAfter = root.int("level_after") ?: return null
        val needAfter = root.points("need_after")?.takeIf { it > 0 }
        val xpAfter = (root.points("xp_after") ?: 0).coerceAtLeast(0)
        val materials = (root["materials"] as? JsonArray).orEmpty().mapNotNull { element ->
            val m = element as? JsonObject ?: return@mapNotNull null
            XpResultMaterial(
                id = m.long("id") ?: return@mapNotNull null,
                own = m.points("own") ?: 0,
                inherit = m.points("inherit") ?: 0,
                crit = m.bool("crit") ?: false,
                value = m.points("value") ?: 0,
            )
        }
        return ForgeOutcome.XpDone(
            XpResult(
                targetId = targetId,
                levelBefore = levelBefore,
                levelAfter = levelAfter,
                levels = (root.int("levels") ?: (levelAfter - levelBefore)).coerceAtLeast(0),
                gain = (root.points("gain") ?: 0).coerceAtLeast(0),
                after = ForgeBar(levelAfter, xpAfter, needAfter, if (needAfter == null) null else ForgeXp.tenthsOf(root.dbl("pct_after"))),
                overflow = (root.points("overflow") ?: 0).coerceAtLeast(0),
                materials = materials,
                crits = root.int("crits") ?: materials.count { it.crit },
                levelBoxes = (root["level_boxes"] as? JsonArray).orEmpty().mapNotNull { (it as? JsonPrimitive)?.intOrNull },
            ),
        )
    }
}

/**
 * 저장 형식 — 계정마다 한 줄. 예전 확률 강화는 "계정|키|대상|재료,재료,재료|확률|시각"(1.20.x 와 같은 여섯 칸 — 1.20.x 가 보낸 요청도
 * 그대로 읽는다), 경험치 강화는 끝에 "|XP|견적 서명"을 더한 여덟 칸. 한 폰에서 계정을 바꿔도 다른 계정의 결과 모르는 요청을
 * 덮어쓰거나 지우지 않는다(그 계정으로 돌아오면 같은 요청 키로 결과를 다시 묻는다 — 재료가 탔는지 확인할 길을 잃지 않게).
 */
class PrefsForgePendingStore(
    private val prefs: com.stepup.android.data.prefs.UserPrefs,
) : ForgePendingStore {
    override suspend fun load(): ForgePending? = pendingFor(prefs.forgePending(), prefs.economyOwner().orEmpty())

    override suspend fun save(pending: ForgePending?) {
        val owner = prefs.economyOwner().orEmpty()
        prefs.updateForgePending { all -> upsertPending(all, owner, pending) }
    }

    override val pending: Flow<ForgePending?>
        get() = prefs.forgePendingFlow.map { (raw, owner) -> pendingFor(raw, owner.orEmpty()) }
}

/** 저장된 줄들 가운데 이 계정의 요청 */
internal fun pendingFor(all: String?, owner: String): ForgePending? =
    all.orEmpty().split('\n').firstNotNullOfOrNull { line -> decodePending(line, owner) }

/** 이 계정의 줄만 바꾸거나([pending]) 지운다(null). 다른 계정의 줄은 그대로. 남은 줄이 없으면 null */
internal fun upsertPending(all: String?, owner: String, pending: ForgePending?): String? {
    val others = all.orEmpty().split('\n').filter { it.isNotBlank() && it.substringBefore('|') != owner }
    val lines = if (pending != null) others + encodePending(pending, owner) else others
    return lines.joinToString("\n").ifEmpty { null }
}

internal fun encodePending(p: ForgePending, owner: String): String {
    val base = listOf(owner, p.requestKey, p.targetId, p.materialIds.joinToString(","), p.ratePermille, p.createdAt)
    if (p.kind == ForgeKind.CHANCE) return base.joinToString("|")
    // 견적 서명에 칸 나눔 글자가 섞이면 적지 않는다 — 그때는 다시 보내지 않고 결과만 묻는다
    val quote = p.quoteVersion.takeIf { '|' !in it && '\n' !in it }.orEmpty()
    return (base + listOf(p.kind.name, quote)).joinToString("|")
}

/** 다른 계정이 남긴 요청이면 null — 그 계정으로 다시 로그인하면 다시 보인다. 종류 칸이 없는 줄(예전 앱)은 확률 강화 */
internal fun decodePending(raw: String, owner: String): ForgePending? {
    val parts = raw.split('|')
    if ((parts.size != 6 && parts.size != 8) || parts[0] != owner) return null
    val kind = if (parts.size == 8) ForgeKind.entries.firstOrNull { it.name == parts[6] } ?: return null else ForgeKind.CHANCE
    return ForgePending(
        requestKey = parts[1].takeIf { it.isNotBlank() } ?: return null,
        targetId = parts[2].toLongOrNull() ?: return null,
        materialIds = parts[3].split(',').mapNotNull { it.toLongOrNull() },
        ratePermille = parts[4].toIntOrNull() ?: 0,
        createdAt = parts[5].toLongOrNull() ?: 0L,
        kind = kind,
        quoteVersion = if (parts.size == 8) parts[7] else "",
    )
}
