package com.stepup.android.data.remote

import com.stepup.android.domain.CrewHeadcountStep
import com.stepup.android.domain.CrewLevelStep
import com.stepup.android.domain.CrewRoomMember
import com.stepup.android.domain.CrewRunMine
import com.stepup.android.domain.CrewRunOutcome
import com.stepup.android.domain.CrewRunRoom
import com.stepup.android.domain.CrewRunRules
import com.stepup.android.domain.CrewRunToday
import java.time.LocalDate
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonNull

/**
 * 크루 러닝 3판(서버 0070) — 오늘 크루 러닝(crew_run_today) · 방 미리보기(crew_run_room) · 결과(crew_run_result) ·
 * 대기실 신발 고르기(party_pick_shoe). 값은 서버가 센 그대로다.
 */
class CrewRunApi(private val server: StepUpServer) {

    suspend fun today(crewId: String): ServerResult<CrewRunTodayRow> =
        rpc("crew_run_today", jsonBody { put("p_crew", crewId) }) { serverJson.decodeFromString<CrewRunTodayRow>(it) }

    suspend fun room(partyId: Long): ServerResult<CrewRunRoomRow> =
        rpc("crew_run_room", jsonBody { put("p_party", partyId) }) { serverJson.decodeFromString<CrewRunRoomRow>(it) }

    /** 크루 러닝이 아니었으면 Ok(null) */
    suspend fun result(startedAtMillis: Long): ServerResult<CrewHomeApi.Maybe<CrewRunResultRow>> =
        rpc("crew_run_result", jsonBody { put("p_started_at", startedAtMillis.toIsoInstant()) }) {
            CrewHomeApi.Maybe(if (it.isBlank() || it.trim() == "null") null else serverJson.decodeFromString<CrewRunResultRow>(it))
        }

    /** 대기실에서 크루 러닝에 신을 신발 — null 이면 신은 신발로 */
    suspend fun pickShoe(partyId: Long, sneakerId: Long?): ServerResult<Unit> =
        rpc(
            "party_pick_shoe",
            jsonBody {
                put("p_party", partyId)
                put("p_sneaker", sneakerId ?: JsonNull)
            },
        ) { }

    private suspend fun <T> rpc(name: String, body: String, parse: (String) -> T?): ServerResult<T> =
        server.authed { token ->
            server.http.post("${server.restUrl}/rpc/$name", body, server.headers(token))
        }.mapBody(parse)
}

@Serializable
data class CrewHeadcountRow(val n: Int = 1, val bonus: Double = 0.0)

@Serializable
data class CrewLevelRuleRow(val level: Int = 1, val km: Double = 0.0, val stamina: Double = 0.0, val reward: Double = 0.0)

@Serializable
data class CrewRunRulesRow(
    @SerialName("room_max") val roomMax: Int = 20,
    @SerialName("near_m") val nearM: Int = 500,
    @SerialName("confirm_sec") val confirmSec: Int = 120,
    @SerialName("alone_grace_sec") val aloneGraceSec: Int = 60,
    @SerialName("drain_per_min") val drainPerMin: Double = 5.0,
    val headcount: List<CrewHeadcountRow> = emptyList(),
    val levels: List<CrewLevelRuleRow> = emptyList(),
) {
    fun toDomain(): CrewRunRules {
        val base = CrewRunRules()
        return CrewRunRules(
            roomMax = roomMax, nearM = nearM, confirmSec = confirmSec, aloneGraceSec = aloneGraceSec, drainPerMin = drainPerMin,
            headcount = headcount.takeIf { it.isNotEmpty() }?.map { CrewHeadcountStep(it.n, it.bonus) } ?: base.headcount,
            levels = levels.takeIf { it.isNotEmpty() }?.map { CrewLevelStep(it.level, it.km, it.stamina, it.reward) } ?: base.levels,
        )
    }
}

@Serializable
data class CrewRunMineRow(
    val day: String = "",
    @SerialName("crew_id") val crewId: String? = null,
    @SerialName("session_id") val sessionId: Long? = null,
    val runners: Int = 0,
    @SerialName("crew_level") val crewLevel: Int = 1,
    val pool: Double = 0.0,
    val share: Double = 0.0,
    @SerialName("limit_sec") val limitSec: Int = 0,
    val used: Double = 0.0,
    @SerialName("together_sec") val togetherSec: Int = 0,
    @SerialName("headcount_bonus") val headcountBonus: Double = 0.0,
    @SerialName("level_bonus") val levelBonus: Double = 0.0,
    @SerialName("sneaker_id") val sneakerId: Long? = null,
) {
    fun toDomain(): CrewRunMine? {
        val date = runCatching { LocalDate.parse(day) }.getOrNull() ?: return null
        return CrewRunMine(
            day = date, crewId = crewId, sessionId = sessionId, runners = runners, crewLevel = crewLevel, pool = pool, share = share,
            limitSec = limitSec, used = used, togetherSec = togetherSec, headcountBonus = headcountBonus, levelBonus = levelBonus,
            sneakerId = sneakerId,
        )
    }
}

@Serializable
data class CrewRunTodayCountRow(val runners: Int = 0, val pool: Double = 0.0, val used: Double = 0.0)

@Serializable
data class CrewRunTodayRow(
    val day: String = "",
    @SerialName("ran_today") val ranToday: Boolean = false,
    val mine: CrewRunMineRow? = null,
    @SerialName("crew_level") val crewLevel: Int? = null,
    @SerialName("run_km") val runKm: Double = 0.0,
    @SerialName("next_level_km") val nextLevelKm: Double? = null,
    @SerialName("stamina_bonus") val staminaBonus: Double = 0.0,
    @SerialName("reward_bonus") val rewardBonus: Double = 0.0,
    val today: CrewRunTodayCountRow = CrewRunTodayCountRow(),
    val rules: CrewRunRulesRow = CrewRunRulesRow(),
) {
    /** 날짜를 읽지 못하면 null — 어느 날의 값인지 모르는 것은 보이지 않는다 */
    fun toDomain(crewId: String): CrewRunToday? {
        val date = runCatching { LocalDate.parse(day) }.getOrNull() ?: return null
        return CrewRunToday(
            crewId = crewId, day = date, ranToday = ranToday, mine = mine?.toDomain(), crewLevel = crewLevel, runKm = runKm,
            nextLevelKm = nextLevelKm, staminaBonus = staminaBonus, rewardBonus = rewardBonus, todayRunners = today.runners,
            todayPool = today.pool, todayUsed = today.used, rules = rules.toDomain(),
        )
    }
}

@Serializable
data class CrewRoomMemberRow(
    @SerialName("user_id") val userId: String = "",
    val name: String? = null,
    @SerialName("is_me") val isMe: Boolean = false,
    @SerialName("is_host") val isHost: Boolean = false,
    @SerialName("crew_member") val crewMember: Boolean = false,
    @SerialName("ran_today") val ranToday: Boolean = false,
    @SerialName("sneaker_id") val sneakerId: Long? = null,
    val rarity: String? = null,
    @SerialName("shoe_level") val shoeLevel: Int? = null,
    @SerialName("dur_max") val durMax: Int = 0,
)

@Serializable
data class CrewRunRoomRow(
    @SerialName("party_id") val partyId: Long = 0,
    @SerialName("crew_id") val crewId: String? = null,
    val status: String = "",
    @SerialName("crew_level") val crewLevel: Int = 1,
    @SerialName("stamina_bonus") val staminaBonus: Double = 0.0,
    @SerialName("reward_bonus") val rewardBonus: Double = 0.0,
    val runners: Int = 0,
    val pool: Double = 0.0,
    val share: Double = 0.0,
    @SerialName("share_sec") val shareSec: Int = 0,
    @SerialName("headcount_bonus") val headcountBonus: Double = 0.0,
    val members: List<CrewRoomMemberRow> = emptyList(),
    val rules: CrewRunRulesRow = CrewRunRulesRow(),
) {
    fun toDomain() = CrewRunRoom(
        partyId = partyId, crewId = crewId, status = status, crewLevel = crewLevel, staminaBonus = staminaBonus,
        rewardBonus = rewardBonus, runners = runners, pool = pool, share = share, shareSec = shareSec,
        headcountBonus = headcountBonus,
        members = members.map {
            CrewRoomMember(
                userId = it.userId, name = it.name.orEmpty(), isMe = it.isMe, isHost = it.isHost, crewMember = it.crewMember,
                ranToday = it.ranToday, sneakerId = it.sneakerId, rarity = it.rarity, shoeLevel = it.shoeLevel, durMax = it.durMax,
            )
        },
        rules = rules.toDomain(),
    )
}

@Serializable
data class CrewRunResultRow(
    val day: String = "",
    @SerialName("crew_id") val crewId: String? = null,
    @SerialName("session_id") val sessionId: Long? = null,
    val runners: Int = 0,
    @SerialName("crew_level") val crewLevel: Int = 1,
    val pool: Double = 0.0,
    val share: Double = 0.0,
    @SerialName("limit_sec") val limitSec: Int = 0,
    val used: Double = 0.0,
    @SerialName("together_sec") val togetherSec: Int = 0,
    @SerialName("headcount_bonus") val headcountBonus: Double = 0.0,
    @SerialName("level_bonus") val levelBonus: Double = 0.0,
    @SerialName("sneaker_id") val sneakerId: Long? = null,
    val points: Double = 0.0,
    val reason: String? = null,
    @SerialName("crew_km") val crewKm: Double = 0.0,
    @SerialName("crew_name") val crewName: String? = null,
    @SerialName("crew_level_now") val crewLevelNow: Int? = null,
    @SerialName("crew_run_km") val crewRunKm: Double = 0.0,
    @SerialName("next_level_km") val nextLevelKm: Double? = null,
) {
    fun toDomain(): CrewRunOutcome? {
        val mine = CrewRunMineRow(
            day, crewId, sessionId, runners, crewLevel, pool, share, limitSec, used, togetherSec, headcountBonus, levelBonus, sneakerId,
        ).toDomain() ?: return null
        return CrewRunOutcome(
            mine = mine, points = points, reason = reason.orEmpty(), crewKm = crewKm, crewName = crewName.orEmpty(),
            crewLevelNow = crewLevelNow, crewRunKm = crewRunKm, nextLevelKm = nextLevelKm,
        )
    }
}
