package com.stepup.android.data.repo

import com.stepup.android.data.remote.ServerResult
import com.stepup.android.domain.Faction
import com.stepup.android.domain.ForgeMaterial
import com.stepup.android.domain.ForgeMaterialBlock
import com.stepup.android.domain.ForgePending
import com.stepup.android.domain.ForgeQuote
import com.stepup.android.domain.ForgeResult
import com.stepup.android.domain.ForgeStats
import com.stepup.android.domain.ForgeTarget
import com.stepup.android.domain.ForgeTargetBlock
import com.stepup.android.domain.Rarity
import com.stepup.android.domain.Sneaker
import java.util.UUID
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.contentOrNull
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

/** 보낸 요청의 저장 자리 — 앱을 다시 켜도 같은 요청 키로 결과만 묻는다 */
interface ForgePendingStore {
    suspend fun load(): ForgePending?
    suspend fun save(pending: ForgePending?)

    /** 결과를 아직 모르는 요청 — 신발 상세가 "강화 결과 확인"과 막힘을 정한다 */
    val pending: Flow<ForgePending?> get() = flowOf(null)
}

/** 강화 화면을 열 때 */
sealed interface ForgeLoad {
    data class Ready(val target: ForgeTarget, val materials: List<ForgeMaterial>) : ForgeLoad
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

    /** 재료 중 일부를 실제로 쓸 수 없다(12) — 그것만 뺀다 */
    data class MaterialsUnavailable(val ids: List<Long>) : ForgeOutcome

    /** 대상이 바뀌었다(13 · 18) */
    data class TargetBlocked(val block: ForgeTargetBlock) : ForgeOutcome

    data object SignInRequired : ForgeOutcome

    /** 그 밖의 거절 — 확률 실패가 아니다 */
    data class Rejected(val reason: String) : ForgeOutcome
}

/**
 * 신발 강화 — 서버 계약(0054 forge_*)을 화면의 말로 옮긴다. 클라이언트는 성공을 굴리지 않고, 신발을 먼저 지우지 않는다.
 *
 * 실행 순서: 견적을 다시 받아(보내기 전 연결 확인을 겸한다 — 여기서 끊기면 "보내지 않음") → 요청 키를 저장 →
 * forge_start. 응답을 잃으면 같은 키로 forge_result 를 한 번 묻고, 그래도 모르면 [ForgeOutcome.Unknown].
 * 결과가 확정되면(성공 · 실패 · 접수 안 됨) 저장한 키를 지우고 서버 목록을 다시 받는다.
 */
class ForgeRepository(
    private val server: ForgeServer?,
    private val pendingStore: ForgePendingStore,
    private val resync: suspend () -> Unit = {},
    private val now: () -> Long = System::currentTimeMillis,
    private val newKey: () -> String = { UUID.randomUUID().toString() },
) {
    suspend fun pending(): ForgePending? = pendingStore.load()

    /** 결과를 아직 모르는 요청(없으면 null) — 상세 · 강화 화면이 같은 요청을 본다 */
    val pendingFlow: Flow<ForgePending?> get() = pendingStore.pending

    suspend fun load(targetId: Long): ForgeLoad {
        val api = server ?: return ForgeLoad.SignInRequired
        return when (val r = api.forgeMaterials(targetId)) {
            is ServerResult.Ok -> runCatching { parseLoad(r.value) }.getOrNull() ?: ForgeLoad.Failed
            is ServerResult.SignInRequired -> ForgeLoad.SignInRequired
            else -> ForgeLoad.Failed
        }
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

    /** 15 "결과 다시 확인" — 같은 요청 키의 결과만 묻는다. 새로 강화하지 않는다 */
    suspend fun check(pending: ForgePending): ForgeOutcome = recheck(pending)

    private suspend fun recheck(pending: ForgePending): ForgeOutcome {
        val api = server ?: return ForgeOutcome.Unknown
        val outcome = when (val r = api.forgeResult(pending.requestKey)) {
            is ServerResult.Ok -> runCatching { parseResultOrError(r.value, pending.materialIds) }.getOrNull()
            else -> null
        } ?: return ForgeOutcome.Unknown
        return settle(outcome)
    }

    /** 확정된 결말이면 저장한 키를 지우고 서버 목록을 맞춘다 */
    private suspend fun settle(outcome: ForgeOutcome): ForgeOutcome {
        if (outcome !is ForgeOutcome.Unknown) {
            pendingStore.save(null)
            if (outcome is ForgeOutcome.Done) runCatching { resync() }
        }
        return outcome
    }

    // ── 응답 읽기 ─────────────────────────────────────────────────

    private fun obj(body: String): JsonObject = Json.parseToJsonElement(body.trim()).jsonObject

    private fun JsonObject.str(key: String): String? = (this[key] as? JsonPrimitive)?.contentOrNull
    private fun JsonObject.int(key: String): Int? = (this[key] as? JsonPrimitive)?.intOrNull
    private fun JsonObject.long(key: String): Long? = (this[key] as? JsonPrimitive)?.longOrNull
    private fun JsonObject.child(key: String): JsonObject? = this[key] as? JsonObject
    private fun JsonObject.ids(key: String): List<Long> =
        (this[key] as? JsonArray)?.mapNotNull { it.jsonPrimitive.longOrNull }.orEmpty()

    private fun stats(o: JsonObject?): ForgeStats? {
        o ?: return null
        return ForgeStats(
            level = o.int("level") ?: return null,
            maxLevel = o.int("max_level") ?: return null,
            efficiencyBps = o.int("efficiency_bps") ?: 0,
            comfortBps = o.int("comfort_bps") ?: 0,
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
}

/**
 * 저장 형식 — 계정마다 한 줄 "계정|키|대상|재료,재료,재료|확률|시각". 한 폰에서 계정을 바꿔도 다른 계정의 결과 모르는 요청을
 * 덮어쓰거나 지우지 않는다(그 계정으로 돌아오면 같은 요청 키로 결과를 다시 묻는다 — 재료 3켤레가 탔는지 확인할 길을 잃지 않게).
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

internal fun encodePending(p: ForgePending, owner: String): String =
    listOf(owner, p.requestKey, p.targetId, p.materialIds.joinToString(","), p.ratePermille, p.createdAt).joinToString("|")

/** 다른 계정이 남긴 요청이면 null — 그 계정으로 다시 로그인하면 다시 보인다 */
internal fun decodePending(raw: String, owner: String): ForgePending? {
    val parts = raw.split('|')
    if (parts.size != 6 || parts[0] != owner) return null
    return ForgePending(
        requestKey = parts[1].takeIf { it.isNotBlank() } ?: return null,
        targetId = parts[2].toLongOrNull() ?: return null,
        materialIds = parts[3].split(',').mapNotNull { it.toLongOrNull() },
        ratePermille = parts[4].toIntOrNull() ?: 0,
        createdAt = parts[5].toLongOrNull() ?: 0L,
    )
}
