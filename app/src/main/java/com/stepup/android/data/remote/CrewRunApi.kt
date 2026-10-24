package com.stepup.android.data.remote

import com.stepup.android.domain.CrewBonus
import com.stepup.android.domain.CrewBonusStatus
import com.stepup.android.domain.CrewEarning
import com.stepup.android.domain.CrewHeadcountStep
import com.stepup.android.domain.CrewLevelEvent
import com.stepup.android.domain.CrewLevelStep
import com.stepup.android.domain.CrewPoolSnap
import com.stepup.android.domain.CrewRoomMember
import com.stepup.android.domain.CrewRunLive
import com.stepup.android.domain.CrewRunMine
import com.stepup.android.domain.CrewRunOutcome
import com.stepup.android.domain.CrewRunRoom
import com.stepup.android.domain.CrewRunRules
import com.stepup.android.domain.CrewRunState
import com.stepup.android.domain.CrewRunToday
import com.stepup.android.domain.CrewTerminal
import java.time.LocalDate
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonNull

/**
 * 크루 러닝(서버 0071) — 오늘 크루 러닝(crew_run_today) · 대기실(crew_run_room) · 달리는 동안 상태 알림(crew_run_beat) ·
 * 결과(crew_run_result) · 대기실 신발 고르기(party_pick_shoe). 값은 서버가 센 그대로다.
 */
class CrewRunApi(private val server: StepUpServer) {

    suspend fun today(crewId: String): ServerResult<CrewRunTodayRow> =
        rpc("crew_run_today", jsonBody { put("p_crew", crewId) }) { serverJson.decodeFromString<CrewRunTodayRow>(it) }

    suspend fun room(partyId: Long): ServerResult<CrewRunRoomRow> =
        rpc("crew_run_room", jsonBody { put("p_party", partyId) }) { serverJson.decodeFromString<CrewRunRoomRow>(it) }

    /** 달리는 동안 내 상태(RUNNING · PAUSED · ENDED)를 알리고 공용 체력 · 인원 판정을 받는다 */
    suspend fun beat(partyId: Long, state: CrewRunState): ServerResult<CrewRunLiveRow> =
        rpc(
            "crew_run_beat",
            jsonBody {
                put("p_party", partyId)
                put("p_state", state.name)
            },
        ) { serverJson.decodeFromString<CrewRunLiveRow>(it) }

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

private fun String?.day(): LocalDate? = this?.takeIf { it.isNotBlank() }?.let { runCatching { LocalDate.parse(it) }.getOrNull() }

private fun String?.ms(): Long? = this?.takeIf { it.isNotBlank() }?.chatMillis()?.takeIf { it > 0 }

@Serializable
data class CrewHeadcountRow(val n: Int = 1, val bonus: Double = 0.0)

@Serializable
data class CrewLevelRuleRow(val level: Int = 1, val km: Double = 0.0, val stamina: Double = 0.0, val reward: Double = 0.0)

@Serializable
data class CrewRunRulesRow(
    @SerialName("room_max") val roomMax: Int = 20,
    @SerialName("near_m") val nearM: Int = 500,
    @SerialName("confirm_sec") val confirmSec: Int = 120,
    @SerialName("drain_per_min") val drainPerMin: Double = 5.0,
    @SerialName("requires_phone") val requiresPhone: Boolean = true,
    val headcount: List<CrewHeadcountRow> = emptyList(),
    val levels: List<CrewLevelRuleRow> = emptyList(),
) {
    fun toDomain(): CrewRunRules {
        val base = CrewRunRules()
        return CrewRunRules(
            roomMax = roomMax, nearM = nearM, confirmSec = confirmSec, drainPerMin = drainPerMin, requiresPhone = requiresPhone,
            headcount = headcount.takeIf { it.isNotEmpty() }?.map { CrewHeadcountStep(it.n, it.bonus) } ?: base.headcount,
            levels = levels.takeIf { it.isNotEmpty() }?.map { CrewLevelStep(it.level, it.km, it.stamina, it.reward) } ?: base.levels,
        )
    }
}

@Serializable
data class CrewPoolRow(
    val day: String? = null,
    val total: Double = 0.0,
    val remaining: Double = 0.0,
    val consumed: Double = 0.0,
    val contributors: Int = 0,
    @SerialName("active_runners") val activeRunners: Int = 0,
    val exhausted: Boolean = false,
    val low: Boolean = false,
    val version: Long = 0,
    @SerialName("as_of") val asOf: String? = null,
) {
    /** 날짜를 읽지 못하면 null — 어느 날의 풀인지 모르는 값은 보이지 않는다(0 으로 바꾸지 않는다) */
    fun toDomain(receivedAtMs: Long = System.currentTimeMillis()): CrewPoolSnap? {
        val date = day.day() ?: return null
        return CrewPoolSnap(
            day = date, total = total, remaining = remaining, consumed = consumed, contributors = contributors,
            activeRunners = activeRunners, exhausted = exhausted, low = low, version = version,
            // 표시 보간의 기준 — 받은 때(폰 단조 시계와 섞지 않게 벽시계로 받은 순간)
            asOfMs = receivedAtMs,
        )
    }
}

@Serializable
data class CrewRunMineRow(
    @SerialName("used_today") val usedToday: Boolean = false,
    val state: String? = null,
    @SerialName("crew_id") val crewId: String? = null,
    @SerialName("crew_name") val crewName: String? = null,
    @SerialName("party_id") val partyId: Long? = null,
    @SerialName("session_id") val sessionId: Long? = null,
    @SerialName("started_at") val startedAt: String? = null,
    @SerialName("terminal_reason") val terminalReason: String? = null,
    val contribution: Double? = null,
    val consumed: Double? = null,
    @SerialName("distance_m") val distanceM: Double? = null,
    @SerialName("total_sup") val totalSup: Double? = null,
    @SerialName("next_eligible_at") val nextEligibleAt: String? = null,
) {
    fun toDomain() = CrewRunMine(
        usedToday = usedToday, state = CrewRunState.of(state), crewId = crewId, crewName = crewName, partyId = partyId,
        sessionId = sessionId, startedAtMs = startedAt.ms(), terminalReason = CrewTerminal.of(terminalReason),
        contribution = contribution ?: 0.0, consumed = consumed ?: 0.0, distanceM = distanceM, totalSup = totalSup,
        nextEligibleAtMs = nextEligibleAt.ms(),
    )
}

@Serializable
data class CrewLevelEventRow(val level: Int = 1, @SerialName("reached_at") val reachedAt: String? = null, @SerialName("run_km") val runKm: Double? = null)

@Serializable
data class CrewRunTodayRow(
    @SerialName("server_now") val serverNow: String? = null,
    val day: String = "",
    @SerialName("resets_at") val resetsAt: String? = null,
    val pool: CrewPoolRow? = null,
    val mine: CrewRunMineRow = CrewRunMineRow(),
    val verified: Boolean = false,
    @SerialName("crew_level") val crewLevel: Int? = null,
    @SerialName("run_km") val runKm: Double = 0.0,
    @SerialName("next_level_km") val nextLevelKm: Double? = null,
    @SerialName("stamina_bonus") val staminaBonus: Double = 0.0,
    @SerialName("reward_bonus") val rewardBonus: Double = 0.0,
    @SerialName("level_event") val levelEvent: CrewLevelEventRow? = null,
    val rules: CrewRunRulesRow = CrewRunRulesRow(),
) {
    /** 날짜 · 풀을 읽지 못하면 null — 0/0 이나 참여 완료로 꾸미지 않는다 */
    fun toDomain(crewId: String): CrewRunToday? {
        val date = day.day() ?: return null
        val now = System.currentTimeMillis()
        return CrewRunToday(
            crewId = crewId, day = date, serverNowMs = serverNow.ms() ?: now, resetsAtMs = resetsAt.ms() ?: 0L,
            pool = pool?.toDomain(now) ?: return null, mine = mine.toDomain(), verified = verified,
            crewLevel = crewLevel, runKm = runKm, nextLevelKm = nextLevelKm, staminaBonus = staminaBonus, rewardBonus = rewardBonus,
            levelEvent = levelEvent?.let { CrewLevelEvent(it.level, it.reachedAt.ms() ?: 0L, it.runKm) },
            rules = rules.toDomain(),
        )
    }
}

@Serializable
data class CrewRoomMemberRow(
    @SerialName("user_id") val userId: String = "",
    val name: String? = null,
    @SerialName("is_me") val isMe: Boolean = false,
    @SerialName("is_host") val isHost: Boolean = false,
    val ready: Boolean = false,
    @SerialName("crew_member") val crewMember: Boolean = false,
    @SerialName("ran_today") val ranToday: Boolean = false,
    val verified: Boolean = false,
    @SerialName("sneaker_id") val sneakerId: Long? = null,
    val rarity: String? = null,
    @SerialName("shoe_level") val shoeLevel: Int? = null,
    @SerialName("model_id") val modelId: Int? = null,
    @SerialName("dur_max") val durMax: Int = 0,
    @SerialName("dur_now") val durNow: Int? = null,
)

@Serializable
data class CrewBonusPreviewRow(val count: Int = 0, val pct: Double = 0.0, @SerialName("next_pct") val nextPct: Double = 0.0)

@Serializable
data class CrewRunRoomRow(
    @SerialName("party_id") val partyId: Long = 0,
    @SerialName("crew_id") val crewId: String? = null,
    val status: String = "",
    val capacity: Int = 20,
    @SerialName("crew_level") val crewLevel: Int = 1,
    @SerialName("stamina_bonus") val staminaBonus: Double = 0.0,
    @SerialName("reward_bonus") val rewardBonus: Double = 0.0,
    @SerialName("members_count") val membersCount: Int = 0,
    @SerialName("ready_count") val readyCount: Int = 0,
    @SerialName("bonus_preview") val bonusPreview: CrewBonusPreviewRow = CrewBonusPreviewRow(),
    @SerialName("my_contribution") val myContribution: Double = 0.0,
    @SerialName("my_verified") val myVerified: Boolean = false,
    val pool: CrewPoolRow? = null,
    val members: List<CrewRoomMemberRow> = emptyList(),
    val rules: CrewRunRulesRow = CrewRunRulesRow(),
) {
    fun toDomain() = CrewRunRoom(
        partyId = partyId, crewId = crewId, status = status, capacity = capacity, crewLevel = crewLevel,
        staminaBonus = staminaBonus, rewardBonus = rewardBonus, membersCount = membersCount, readyCount = readyCount,
        bonusCount = bonusPreview.count, bonusPct = bonusPreview.pct, nextBonusPct = bonusPreview.nextPct,
        myContribution = myContribution, myVerified = myVerified, pool = pool?.toDomain(),
        members = members.map {
            CrewRoomMember(
                userId = it.userId, name = it.name.orEmpty(), isMe = it.isMe, isHost = it.isHost, ready = it.ready,
                crewMember = it.crewMember, ranToday = it.ranToday, verified = it.verified, sneakerId = it.sneakerId,
                rarity = it.rarity, shoeLevel = it.shoeLevel, modelId = it.modelId, durMax = it.durMax, durNow = it.durNow,
            )
        },
        rules = rules.toDomain(),
    )
}

@Serializable
data class CrewBonusRow(
    val status: String? = null,
    @SerialName("check_at") val checkAt: String? = null,
    val count: Int = 0,
    val pct: Double = 0.0,
    val reason: String? = null,
    val verified: Boolean = false,
) {
    fun toDomain() = CrewBonus(CrewBonusStatus.of(status), checkAt.ms(), count, pct, reason, verified)
}

@Serializable
data class CrewLevelPctRow(val level: Int = 1, val pct: Double = 0.0)

@Serializable
data class CrewRunLiveRow(
    @SerialName("crew_run") val crewRun: Boolean = false,
    @SerialName("server_now") val serverNow: String? = null,
    @SerialName("party_id") val partyId: Long = 0,
    @SerialName("crew_id") val crewId: String? = null,
    val day: String? = null,
    @SerialName("started_at") val startedAt: String? = null,
    val state: String? = null,
    @SerialName("terminal_reason") val terminalReason: String? = null,
    val contribution: Double = 0.0,
    val consumed: Double = 0.0,
    @SerialName("active_sec") val activeSec: Double = 0.0,
    val pool: CrewPoolRow? = null,
    val participants: Int = 0,
    @SerialName("not_ended") val notEnded: Int = 0,
    @SerialName("room_active_runners") val roomActiveRunners: Int = 0,
    val bonus: CrewBonusRow = CrewBonusRow(),
    val level: CrewLevelPctRow = CrewLevelPctRow(),
    @SerialName("earning_state") val earningState: String? = null,
) {
    /** 크루 러닝이 아니면(서버가 이 방의 참여를 모른다) null */
    fun toDomain(): CrewRunLive? {
        if (!crewRun) return null
        val now = System.currentTimeMillis()
        return CrewRunLive(
            partyId = partyId, crewId = crewId, day = day.day(), startedAtMs = startedAt.ms(),
            state = CrewRunState.of(state) ?: return null, terminal = CrewTerminal.of(terminalReason),
            contribution = contribution, consumed = consumed, activeSec = activeSec, pool = pool?.toDomain(now),
            participants = participants, notEnded = notEnded, roomActiveRunners = roomActiveRunners,
            bonus = bonus.toDomain(), crewLevel = level.level, levelPct = level.pct, earning = CrewEarning.of(earningState),
            serverNowMs = serverNow.ms() ?: now,
        )
    }
}

@Serializable
data class CrewRewardRow(
    val base: Double? = null,
    @SerialName("headcount_pct") val headcountPct: Double = 0.0,
    val headcount: Double? = null,
    @SerialName("level_pct") val levelPct: Double = 0.0,
    val level: Double? = null,
    val total: Double? = null,
)

@Serializable
data class CrewPoolUsageRow(
    val contribution: Double = 0.0,
    val consumed: Double = 0.0,
    @SerialName("active_sec") val activeSec: Double = 0.0,
    val pool: CrewPoolRow? = null,
)

@Serializable
data class CrewXpRow(
    @SerialName("distance_m") val distanceM: Double? = null,
    val applied: Boolean = false,
    @SerialName("level_before") val levelBefore: Int? = null,
    @SerialName("level_after") val levelAfter: Int? = null,
    @SerialName("run_km") val runKm: Double? = null,
    @SerialName("next_level_km") val nextLevelKm: Double? = null,
)

@Serializable
data class CrewRunResultRow(
    val settled: Boolean = false,
    @SerialName("crew_id") val crewId: String? = null,
    @SerialName("crew_name") val crewName: String? = null,
    val verdict: String? = null,
    val reason: String? = null,
    @SerialName("terminal_reason") val terminalReason: String? = null,
    val participants: Int = 0,
    val reward: CrewRewardRow = CrewRewardRow(),
    val bonus: CrewBonusRow = CrewBonusRow(),
    @SerialName("pool_usage") val poolUsage: CrewPoolUsageRow = CrewPoolUsageRow(),
    val xp: CrewXpRow = CrewXpRow(),
    @SerialName("next_eligible_at") val nextEligibleAt: String? = null,
    /** 그 러닝의 대기실 이름 · 코스(0072 — 출발 때 적은 값). 예전 방이면 null */
    @SerialName("party_name") val partyName: String? = null,
    @SerialName("course_name") val courseName: String? = null,
    @SerialName("course_km") val courseKm: Double? = null,
) {
    fun toDomain() = CrewRunOutcome(
        settled = settled, crewId = crewId, crewName = crewName.orEmpty(), verdict = verdict.orEmpty(), reason = reason.orEmpty(),
        terminal = CrewTerminal.of(terminalReason), participants = participants,
        base = reward.base, headcountPct = reward.headcountPct, headcount = reward.headcount,
        levelPct = reward.levelPct, level = reward.level, total = reward.total, bonus = bonus.toDomain(),
        contribution = poolUsage.contribution, consumed = poolUsage.consumed, activeSec = poolUsage.activeSec,
        pool = poolUsage.pool?.toDomain(), xpM = xp.distanceM, xpApplied = xp.applied, levelBefore = xp.levelBefore,
        levelAfter = xp.levelAfter, runKm = xp.runKm, nextLevelKm = xp.nextLevelKm, nextEligibleAtMs = nextEligibleAt.ms(),
        partyName = partyName?.takeIf { it.isNotBlank() }, courseName = courseName?.takeIf { it.isNotBlank() }, courseKm = courseKm,
    )
}
