package com.stepup.android.data.remote

import com.stepup.android.domain.coaching.CoachCandidate
import com.stepup.android.domain.coaching.CoachFact
import com.stepup.android.domain.coaching.CoachKind
import com.stepup.android.domain.coaching.CoachResult
import com.stepup.android.domain.coaching.CoachServerState
import com.stepup.android.domain.coaching.CoachStatus
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.booleanOrNull
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.longOrNull
import kotlinx.serialization.json.put
import kotlinx.serialization.json.putJsonArray
import kotlinx.serialization.json.add

/**
 * AI 코칭(서버 0060 coaching_*). 앱은 러닝을 "시작 시각"으로만 가리킨다 — 서버가 토큰의 사용자와 시작 시각으로 자기 표에서
 * 찾는다. 로컬 기록 번호 · 사용자 번호 · 앱이 계산한 숫자는 보내지 않는다(구간 랩만 — 서버가 자기 러닝과 맞대어 본다).
 *
 * 모든 호출은 [expectedUserId] 의 토큰으로만 한다 — 그 사이 계정이 바뀌었으면 보내지 않고 SignInRequired 다
 * (이전 계정의 요청 · 응답이 새 계정 화면에 붙지 않게).
 */
class CoachingApi(private val server: StepUpServer) {

    val isConfigured: Boolean get() = server.isConfigured

    suspend fun state(userId: String, zone: String): ServerResult<CoachServerSnapshot> =
        rpc(userId, "coaching_state", jsonBody { put("p_tz", zone) }) { CoachServerSnapshot.parse(it) }

    suspend fun grantConsent(userId: String, version: String): ServerResult<Unit> =
        rpc(userId, "coaching_consent_grant", jsonBody { put("p_version", version) }) { }

    /** 해제 — 대기 · 진행 작업 취소, [deleteResults] 면 저장된 분석도. 지운 분석 수를 돌려준다 */
    suspend fun withdraw(userId: String, deleteResults: Boolean): ServerResult<Int> =
        rpc(userId, "coaching_consent_withdraw", jsonBody { put("p_delete_results", deleteResults) }) {
            (it.obj()?.get("deleted_results") as? JsonPrimitive)?.longOrNull?.toInt() ?: 0
        }

    suspend fun savePrefs(
        userId: String,
        goal: String?,
        runsPerWeek: Int?,
        minutes: Int?,
        autoAnalysis: Boolean,
        weeklyReport: Boolean,
        expectedRevision: Long,
    ): ServerResult<CoachPrefsSave> =
        rpc(
            userId,
            "coaching_prefs_save",
            jsonBody {
                put("p_goal", goal ?: JsonNull)
                put("p_runs", runsPerWeek ?: JsonNull)
                put("p_minutes", minutes ?: JsonNull)
                put("p_auto", autoAnalysis)
                put("p_weekly", weeklyReport)
                put("p_expected_revision", expectedRevision)
            },
        ) { CoachPrefsSave.parse(it) }

    /** 러닝의 1km 랩(누적 활성 초) — stored · exists 면 서버가 받았다. 그 밖은 까닭 글자 */
    suspend fun putSplits(userId: String, startedAt: Long, laps: List<Long>): ServerResult<String> {
        val body = buildJsonObject {
            put("p_started_at", startedAt.toIsoInstant())
            putJsonArray("p_laps") { laps.forEach { add(it) } }
        }.toString()
        return rpc(userId, "coaching_splits_put", body) { serverJson.parseToJsonElement(it).let { el -> (el as? JsonPrimitive)?.contentOrNull } }
    }

    suspend fun run(userId: String, startedAt: Long, locale: String, request: Boolean): ServerResult<CoachStatus> =
        rpc(userId, "coaching_run", jsonBody {
            put("p_started_at", startedAt.toIsoInstant())
            put("p_locale", locale)
            put("p_request", request)
        }) { parseStatus(it) }

    suspend fun week(userId: String, monday: java.time.LocalDate, zone: String, locale: String, request: Boolean): ServerResult<CoachStatus> =
        rpc(userId, "coaching_week", jsonBody {
            put("p_week_start", monday.toString())
            put("p_tz", zone)
            put("p_locale", locale)
            put("p_request", request)
        }) { parseStatus(it) }

    suspend fun recommend(userId: String, zone: String, locale: String, request: Boolean): ServerResult<CoachStatus> =
        rpc(userId, "coaching_recommend", jsonBody {
            put("p_tz", zone)
            put("p_locale", locale)
            put("p_request", request)
        }) { parseStatus(it) }

    suspend fun retry(userId: String, jobId: Long): ServerResult<CoachStatus> =
        rpc(userId, "coaching_job_retry", jsonBody { put("p_job_id", jobId) }) { parseStatus(it) }

    suspend fun applyPace(userId: String, resultId: Long, expectedRevision: Long): ServerResult<CoachPrefsSave> =
        rpc(userId, "coaching_apply_pace", jsonBody {
            put("p_result_id", resultId)
            put("p_expected_revision", expectedRevision)
        }) { CoachPrefsSave.parse(it) }

    suspend fun applyPlan(userId: String, monday: java.time.LocalDate, zone: String, expectedRevision: Long): ServerResult<CoachPlanApply> =
        rpc(userId, "coaching_apply_plan", jsonBody {
            put("p_week_start", monday.toString())
            put("p_tz", zone)
            put("p_expected_revision", expectedRevision)
        }) { CoachPlanApply.parse(it) }

    private suspend fun <T> rpc(userId: String, name: String, body: String, parse: (String) -> T?): ServerResult<T> =
        server.authed(expectedUserId = userId) { token ->
            server.http.post("${server.restUrl}/rpc/$name", body, server.headers(token))
        }.mapBody(parse)
}

/** 서버의 코칭 상태(coaching_state) — 켜져 있나 · 동의 · 선호 · 적용한 계획 */
data class CoachServerSnapshot(
    val enabled: Boolean,
    val provider: String,
    val consentVersion: String,
    val consented: Boolean,
    val prefs: CoachServerPrefs?,
    val plans: List<CoachPlanApplied>,
) {
    companion object {
        fun parse(text: String): CoachServerSnapshot? {
            val o = text.obj() ?: return null
            val consent = o["consent"] as? JsonObject
            return CoachServerSnapshot(
                enabled = o.bool("enabled") ?: false,
                provider = o.string("provider") ?: "none",
                consentVersion = o.string("consent_version") ?: return null,
                consented = consent?.bool("current") == true,
                prefs = (o["prefs"] as? JsonObject)?.let(CoachServerPrefs::of),
                plans = (o["plans"] as? JsonArray).orEmpty().mapNotNull { (it as? JsonObject)?.let(CoachPlanApplied::of) },
            )
        }
    }
}

/** 서버에 저장된 선호 — 고르지 않은 칸은 null. [revision] 으로 엇갈린 저장을 가린다 */
data class CoachServerPrefs(
    val goal: String?,
    val runsPerWeek: Int?,
    val minutes: Int?,
    val autoAnalysis: Boolean,
    val weeklyReport: Boolean,
    val targetPaceSec: Int?,
    val revision: Long,
) {
    companion object {
        fun of(o: JsonObject) = CoachServerPrefs(
            goal = o.string("goal"),
            runsPerWeek = o.long("runs_per_week")?.toInt(),
            minutes = o.long("minutes")?.toInt(),
            autoAnalysis = o.bool("auto_analysis") ?: false,
            weeklyReport = o.bool("weekly_report") ?: false,
            targetPaceSec = o.long("target_pace_sec")?.toInt(),
            revision = o.long("revision") ?: 0,
        )
    }
}

/** 선호 저장 · 목표 적용의 답 — [conflict] 면 다른 기기가 먼저 바꿨다(지금 서버 값이 [prefs]) */
data class CoachPrefsSave(val ok: Boolean, val conflict: Boolean, val prefs: CoachServerPrefs?) {
    companion object {
        fun parse(text: String): CoachPrefsSave? {
            val o = text.obj() ?: return null
            return CoachPrefsSave(o.bool("ok") ?: false, o.bool("conflict") ?: false, (o["prefs"] as? JsonObject)?.let(CoachServerPrefs::of))
        }
    }
}

/** 적용한 주간 계획 — 날짜와 템플릿 id(서버가 지금 선호로 만든 것) */
data class CoachPlanApplied(val weekStart: java.time.LocalDate, val days: List<Pair<java.time.LocalDate, String>>) {
    companion object {
        fun of(o: JsonObject): CoachPlanApplied? {
            val start = o.string("week_start")?.let { runCatching { java.time.LocalDate.parse(it) }.getOrNull() } ?: return null
            val days = (o["days"] as? JsonArray).orEmpty().mapNotNull { day ->
                val d = day as? JsonObject ?: return@mapNotNull null
                val date = d.string("date")?.let { runCatching { java.time.LocalDate.parse(it) }.getOrNull() } ?: return@mapNotNull null
                date to (d.string("template") ?: return@mapNotNull null)
            }
            return CoachPlanApplied(start, days)
        }
    }
}

/** 계획 적용의 답 — [conflict] 면 다른 기기가 선호를 먼저 바꿨다(적용하지 않았다) */
data class CoachPlanApply(val plan: CoachPlanApplied?, val conflict: Boolean) {
    companion object {
        fun parse(text: String): CoachPlanApply? {
            val o = text.obj() ?: return null
            if (o.bool("conflict") == true) return CoachPlanApply(null, conflict = true)
            return CoachPlanApplied.of(o)?.let { CoachPlanApply(it, conflict = false) }
        }
    }
}

/** 상태 · 결과 JSON → 도메인 값. 모르는 상태 글자는 이해하지 못한 답(null)이다 */
internal fun parseStatus(text: String): CoachStatus? {
    val o = text.obj() ?: return null
    val state = CoachServerState.of(o.string("state")) ?: return null
    return CoachStatus(
        state = state,
        jobId = o.long("job_id"),
        reason = o.string("reason"),
        retryable = o.bool("retryable") ?: false,
        resetAt = o.string("reset_at")?.let(::parseInstantMillis),
        result = (o["result"] as? JsonObject)?.let(::parseResult),
        stale = o.bool("stale") ?: false,
    )
}

internal fun parseResult(o: JsonObject): CoachResult? {
    fun items(key: String): List<Pair<String, Map<String, Long>>> = (o[key] as? JsonArray).orEmpty().mapNotNull { item ->
        val entry = item as? JsonObject ?: return@mapNotNull null
        val id = entry.string("id") ?: return@mapNotNull null
        val values = (entry["values"] as? JsonObject).orEmpty()
            .mapNotNull { (k, v) -> (v as? JsonPrimitive)?.longOrNull?.let { number -> k to number } }.toMap()
        id to values
    }
    return CoachResult(
        id = o.long("id") ?: return null,
        kind = CoachKind.of(o.string("kind")) ?: return null,
        generatedAt = o.string("generated_at")?.let(::parseInstantMillis) ?: return null,
        locale = o.string("locale") ?: return null,
        facts = items("facts").map { (id, v) -> CoachFact(id, v) },
        factId = o.string("fact_id"),
        explanation = o.string("explanation"),
        candidates = items("candidates").map { (id, v) -> CoachCandidate(id, v) },
        candidateId = o.string("candidate_id"),
        actionReason = o.string("action_reason"),
        limitation = o.string("limitation"),
        provider = o.string("provider") ?: return null,
    )
}

/** Postgres timestamptz 글자(2026-10-16 07:12:03.123+00 · ISO) → epoch ms */
internal fun parseInstantMillis(text: String): Long? = runCatching {
    java.time.OffsetDateTime.parse(text.replace(' ', 'T').let { if (Regex("[+-]\\d\\d$").containsMatchIn(it)) "$it:00" else it })
        .toInstant().toEpochMilli()
}.getOrNull()

private fun String.obj(): JsonObject? = runCatching { serverJson.parseToJsonElement(this) as? JsonObject }.getOrNull()
private fun JsonObject.prim(key: String): JsonPrimitive? = (this[key] as? JsonPrimitive)?.takeUnless { it is JsonNull }
private fun JsonObject.string(key: String): String? = prim(key)?.contentOrNull
private fun JsonObject.long(key: String): Long? = prim(key)?.longOrNull
private fun JsonObject.bool(key: String): Boolean? = prim(key)?.booleanOrNull
private fun JsonArray?.orEmpty(): List<JsonElement> = this ?: emptyList()
private fun JsonObject?.orEmpty(): Map<String, JsonElement> = this ?: emptyMap()
