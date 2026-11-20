package com.stepup.android.domain

import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.booleanOrNull
import kotlinx.serialization.json.doubleOrNull
import kotlinx.serialization.json.intOrNull

/**
 * 포인트 미리보기(서버 0119 `sneaker_points_preview` — docs/economy-roots-contract-2026-11-21.md 3장).
 *
 * 서버가 내 지난 14일 혼자 러닝으로 셈한 값이다: 능력치마다 "1포인트를 더 넣으면 하루에 약 +N SUP", 남은 포인트를 나누는 추천,
 * 다시 나눌 수 있을 때는 모든 포인트를 처음부터 나눈 추천. **어림값이다** — 화면은 늘 "약"을 붙이고, 받은 SUP 처럼 쓰지 않는다.
 * 빠진 칸은 null(그 줄을 보이지 않는다). 0119 전 서버에는 함수가 없다 — 그때는 이 값이 없다(미리보기 칸을 숨긴다).
 */
data class PointsPreview(
    /** 무엇으로 셈했나 — 내 기록 / 기록이 없어 기본 러닝. 모르면 null(기준 줄을 보이지 않는다) */
    val source: Source?,
    /** 셈에 든 러닝 수 · 달린 날 수(내 기록일 때) */
    val runs: Int,
    val days: Int,
    /** 한 번에 달린 평균 분(기본 러닝이면 30) */
    val avgMin: Int?,
    /** 서버가 본 받은 포인트 · 남은 포인트 — 폰 사본과 다르면 미리보기를 쓰지 않는다([PointsPreview.matches]) */
    val total: Int?,
    val free: Int?,
    val respec: Boolean,
    /** 서버가 본 지금 배분(+ 하루에 남는 SUP · 가득에서 달리는 분) */
    val now: PointsSplit?,
    /** 지난 러닝 중 신발 수명보다 길었던 횟수 · 에너지가 먼저 끝난 날 수 */
    val wornRuns: Int,
    val energyOutDays: Int,
    /** 그 능력치에 1포인트를 더 넣으면 하루에 약 +N SUP(0 = 지금 내 기록으로는 늘지 않는다). 모르면 null */
    val perPoint: PerPoint,
    /** 착화감이 상한(에너지 70% 절약)에 닿았다 */
    val comfortCapped: Boolean,
    /** 남은 포인트를 나누는 추천 — **더할 양** */
    val recommend: PointsSplit?,
    /** 다시 나눌 수 있을 때만 — 모든 포인트를 처음부터 나눈 값(**절대값**) */
    val recommendAll: PointsSplit?,
) {
    enum class Source { MINE, DEFAULT }

    data class PerPoint(val efficiency: Double?, val comfort: Double?, val durability: Double?)

    /** 폰 사본의 받은 포인트 · 나눈 포인트와 서버가 본 값이 같은가 — 다르면(낡은 사본) 값 · 추천을 보이지 않는다. 서버가 안 보낸 칸은 묻지 않는다 */
    fun matches(total: Int, efficiency: Int, comfort: Int, durability: Int): Boolean =
        (this.total == null || this.total == total) &&
            (now == null || (now.efficiency == efficiency && now.comfort == comfort && now.durability == durability))

    companion object {
        /** 응답 글 → 미리보기. JSON 이 아니거나 `ok` 가 true 가 아니면(내 신발이 아님 · 예전 신발) null */
        fun parse(body: String): PointsPreview? = runCatching {
            val o = Json.parseToJsonElement(body) as? JsonObject ?: return@runCatching null
            if (o.bool("ok") != true) return@runCatching null
            val basis = o.obj("basis")
            val now = o.obj("now")
            val per = o.obj("per_point")
            PointsPreview(
                source = when (basis?.str("source")) {
                    "MINE" -> Source.MINE
                    "DEFAULT" -> Source.DEFAULT
                    else -> null
                },
                runs = (basis?.int("runs") ?: 0).coerceAtLeast(0),
                days = (basis?.int("days") ?: 0).coerceAtLeast(0),
                avgMin = basis?.int("avg_min")?.takeIf { it > 0 },
                total = o.int("total")?.takeIf { it >= 0 },
                free = o.int("free")?.takeIf { it >= 0 },
                respec = o.bool("respec") ?: false,
                now = now?.let(::split),
                wornRuns = (now?.int("worn_runs") ?: 0).coerceAtLeast(0),
                energyOutDays = (now?.int("energy_out_days") ?: 0).coerceAtLeast(0),
                perPoint = PerPoint(
                    efficiency = per?.amount("efficiency"),
                    comfort = per?.amount("comfort"),
                    durability = per?.amount("durability"),
                ),
                comfortCapped = o.bool("comfort_capped") ?: false,
                recommend = o.obj("recommend")?.let(::split),
                recommendAll = o.obj("recommend_all")?.let(::split),
            )
        }.getOrNull()

        /** 배분 한 벌 — 세 칸이 모두 있어야 읽는다(하나라도 빠지면 그 추천을 쓰지 않는다). 음수는 0 */
        private fun split(o: JsonObject): PointsSplit? {
            val e = o.int("efficiency") ?: return null
            val c = o.int("comfort") ?: return null
            val d = o.int("durability") ?: return null
            return PointsSplit(
                efficiency = e.coerceAtLeast(0), comfort = c.coerceAtLeast(0), durability = d.coerceAtLeast(0),
                dayNet = o.dbl("day_net"), lifeMin = o.dbl("life_min")?.takeIf { it >= 0 },
            )
        }
    }
}

/** 효율 · 착화감 · 내구도에 나눈 포인트(+ 그 배분으로 달린 날 하루에 남는 SUP · 가득에서 달리는 분 — 어림값) */
data class PointsSplit(
    val efficiency: Int,
    val comfort: Int,
    val durability: Int,
    val dayNet: Double? = null,
    val lifeMin: Double? = null,
) {
    val sum: Int get() = efficiency + comfort + durability
}

private fun JsonObject.prim(k: String): JsonPrimitive? = (this[k] as? JsonPrimitive)?.takeIf { it !is JsonNull }
private fun JsonObject.obj(k: String): JsonObject? = this[k] as? JsonObject
private fun JsonObject.str(k: String): String? = prim(k)?.takeIf { it.isString }?.content
private fun JsonObject.bool(k: String): Boolean? = prim(k)?.booleanOrNull
private fun JsonObject.dbl(k: String): Double? = prim(k)?.let { it.doubleOrNull ?: it.content.toDoubleOrNull() }?.takeIf { it.isFinite() }
private fun JsonObject.int(k: String): Int? = prim(k)?.let { it.intOrNull ?: it.doubleOrNull?.takeIf { d -> d.isFinite() }?.toInt() }

/** 하루에 늘어나는 SUP — 음수 · 잘못된 값은 0(늘지 않는다) */
private fun JsonObject.amount(k: String): Double? = dbl(k)?.coerceAtLeast(0.0)
