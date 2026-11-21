package com.stepup.android.domain

import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.booleanOrNull
import kotlinx.serialization.json.doubleOrNull
import kotlinx.serialization.json.intOrNull
import kotlinx.serialization.json.longOrNull
import java.time.Instant
import java.time.LocalDate
import java.time.OffsetDateTime
import java.time.ZoneId

/**
 * 오늘의 보상(서버 0118, 2026-11-19) — 홈 막대 · 저녁 알림 · 업적 · 이벤트 화면이 함께 읽는 서버 상태.
 *
 * 정본은 서버 `box_status` 의 새 칸(reward_steps_today · goal · ticks · streak · surge · recent …,
 * docs/forge-xp-daily-contract-2026-11-19.md). 보상 걸음은 서버만 안다(경로 · 무효 · 하루 48,000 · 다른 폰의 러닝. 신발 내구도는
 * 서버 0119 부터 보지 않는다 — 경로로 확인된 걸음은 신발이 닳았어도 모두 센다, 그 전 서버는 내구도가 남은 동안만) —
 * 앱은 받은 값을 그릴 뿐 스스로 "받음"을 정하지 않는다. 눈금 · ✓ · "+SUP" 는 모두 서버 값이다.
 * 서버에 0118 이 없거나(새 칸이 없다) 서버가 되돌리기 스위치를 켰으면(rule = "OLD") [parse] 가 null 을 돌려주고 화면은 예전 모습 그대로다.
 *
 * 날은 한국 날짜(서버 economy.game_day). 받은 상태의 [day] 가 오늘이 아니면 화면은 숫자 대신 "—" 를 보인다(자정 · 앱을 다시 켠 뒤
 * 어제 눈금이 남지 않게).
 */
data class DailyRewards(
    /** 서버가 세는 걸음의 규칙 — "REWARD_STEPS"(0118). 되돌리면 다른 값 */
    val rule: String?,
    /** 이 상태의 한국 날짜 */
    val day: LocalDate,
    /** 오늘 보상 걸음(서버가 확인한 러닝 걸음) */
    val rewardStepsToday: Int,
    /** 오늘의 목표 */
    val goal: Int,
    /** 내일부터 바뀔 목표 — 바꾼 것이 없으면 null */
    val goalNext: Int?,
    /** 오늘 받은 목표 보너스(SUP) — 아직이면 null */
    val goalPaid: Double?,
    /** 오늘 채우면 받을 목표 보너스(SUP, 연속 포함) */
    val goalBonus: Double?,
    /** 오늘 채우면 며칠째(받았으면 오늘까지 며칠째) */
    val streak: Int?,
    /** 가장 길었던 연속 */
    val streakBest: Int?,
    /** 날마다 채우면 연속 상급 상자까지 남은 날(0 = 오늘 채우면) */
    val streakBoxIn: Int?,
    /** 막대의 눈금 — 서버가 준다(설정이 바뀌어도 앱은 그대로) */
    val ticks: List<RewardTick>,
    /** 스텝 서지(한 주 걸음 → SUP) */
    val surge: StepSurgeStatus?,
    /** 최근 3일에 받은 것 — 앱이 "새로 받은 것"을 가린다 */
    val recent: List<RecentReward>,
    /** 이번 주 보상 걸음 · 주간 상급 상자 문턱 · 받았는지(예전 칸 그대로) */
    val stepsWeek: Int,
    val weekTarget: Int,
    val weekGot: Boolean,
) {
    fun isFor(today: LocalDate): Boolean = day == today

    /** 지금까지 이어진 목표 연속(오늘 받았으면 오늘까지, 아직이면 어제까지) — 오늘(한국 날짜) 상태가 아니면 null */
    fun streakNow(today: LocalDate): Int? =
        if (!isFor(today)) null else streak?.let { if (goalPaid != null) it else (it - 1).coerceAtLeast(0) }

    companion object {
        /** 서버의 하루 — 한국 시각 */
        val GAME_ZONE: ZoneId = ZoneId.of("Asia/Seoul")

        /** 0118 이 보내는 규칙 이름 — 이 규칙이면 목표 보너스는 러닝을 기록할 때 서버가 준다(앱이 청구하지 않는다) */
        const val RULE_REWARD_STEPS = "REWARD_STEPS"

        /** 서버의 되돌리기 스위치(reward_steps_rule = 'OLD') — 앱은 예전 모습(예전 서버처럼)으로 본다 */
        const val RULE_OLD = "OLD"

        fun gameDay(epochMs: Long): LocalDate = Instant.ofEpochMilli(epochMs).atZone(GAME_ZONE).toLocalDate()

        /**
         * `box_status` 응답 그대로 → 오늘의 보상. 0118 의 칸(reward_steps_today)이 없으면 null(예전 서버 — 화면은 예전 모습).
         * [fetchedAtMs] 는 받은 시각 — 서버가 `day` 를 보내지 않으면 받은 시각의 한국 날짜로 본다.
         */
        fun parse(body: String, fetchedAtMs: Long): DailyRewards? = runCatching {
            val o = Json.parseToJsonElement(body) as? JsonObject ?: return@runCatching null
            val reward = o.int("reward_steps_today") ?: return@runCatching null
            // 서버가 되돌리기 스위치를 켰다(reward_steps_rule = 'OLD') — 앱은 예전 모습 그대로
            if (o.str("rule") == RULE_OLD) return@runCatching null
            val day = o.str("day")?.let { runCatching { LocalDate.parse(it.take(10)) }.getOrNull() } ?: gameDay(fetchedAtMs)
            val goal = o.int("goal") ?: 0
            val goalPaid = o.dbl("goal_paid")
            val ticks = (o["ticks"] as? JsonArray)?.mapNotNull { e ->
                val t = e as? JsonObject ?: return@mapNotNull null
                val steps = t.int("steps") ?: return@mapNotNull null
                val kind = when (t.str("kind")) {
                    "BOX" -> RewardTickKind.BOX
                    "GOAL" -> RewardTickKind.GOAL
                    else -> return@mapNotNull null
                }
                RewardTick(steps, kind, t.bool("got") ?: false)
            } ?: legacyTicks(o, goal, goalPaid)
            val surge = (o["surge"] as? JsonObject)?.let { s ->
                val target = s.long("target") ?: return@let null
                StepSurgeStatus(
                    steps = s.long("steps") ?: 0, target = target,
                    reward = s.dbl("reward") ?: 0.0, claimed = s.bool("claimed") ?: false,
                )
            }
            val recent = (o["recent"] as? JsonArray)?.mapNotNull { e ->
                val r = e as? JsonObject ?: return@mapNotNull null
                val kind = RecentRewardKind.of(r.str("kind")) ?: return@mapNotNull null
                RecentReward(
                    day = r.str("day")?.let { runCatching { LocalDate.parse(it.take(10)) }.getOrNull() },
                    kind = kind,
                    amount = r.dbl("amount"),
                    atMs = r.str("at")?.let(::serverMillis),
                )
            }.orEmpty()
            DailyRewards(
                rule = o.str("rule"),
                day = day,
                rewardStepsToday = reward.coerceAtLeast(0),
                goal = goal,
                goalNext = o.int("goal_next")?.takeIf { it > 0 },
                goalPaid = goalPaid,
                goalBonus = o.dbl("goal_bonus"),
                streak = o.int("streak"),
                streakBest = o.int("streak_best"),
                streakBoxIn = o.int("streak_box_in"),
                ticks = ticks,
                surge = surge,
                recent = recent,
                stepsWeek = o.int("steps_week") ?: 0,
                weekTarget = o.int("week_target") ?: 0,
                weekGot = o.bool("week_got") ?: false,
            )
        }.getOrNull()

        /** 서버가 눈금을 보내지 않으면 — 러닝 상자 칸(run_boxes)과 목표 하나로(받았는지는 서버 값 그대로) */
        private fun legacyTicks(o: JsonObject, goal: Int, goalPaid: Double?): List<RewardTick> {
            val boxes = (o["run_boxes"] as? JsonArray)?.mapNotNull { e ->
                val b = e as? JsonObject ?: return@mapNotNull null
                RewardTick(b.int("steps") ?: return@mapNotNull null, RewardTickKind.BOX, b.bool("got") ?: false)
            }.orEmpty()
            return if (goal > 0) boxes + RewardTick(goal, RewardTickKind.GOAL, goalPaid != null) else boxes
        }

        /** 서버 시각(timestamptz 글자 — "2026-11-19T09:12:00+00" · "2026-11-19 09:12:00.123+09:00") → 벽시계 ms */
        fun serverMillis(raw: String): Long? {
            val iso = raw.trim().replace(' ', 'T')
            val withMinutes = if (Regex("[+-]\\d\\d$").containsMatchIn(iso)) "$iso:00" else iso
            return runCatching { OffsetDateTime.parse(withMinutes).toInstant().toEpochMilli() }
                .recoverCatching { Instant.parse(iso).toEpochMilli() }
                .getOrNull()
        }
    }
}

enum class RewardTickKind { BOX, GOAL }

/** 막대의 눈금 하나 — 서버가 준 문턱 · 종류 · 받았는지 */
data class RewardTick(val steps: Int, val kind: RewardTickKind, val got: Boolean)

/** 스텝 서지 — 한 주 보상 걸음 · 문턱 · 상(SUP) · 받았는지 */
data class StepSurgeStatus(val steps: Long, val target: Long, val reward: Double, val claimed: Boolean) {
    val fraction: Float get() = if (claimed) 1f else if (target <= 0) 0f else (steps.toFloat() / target).coerceIn(0f, 1f)
}

enum class RecentRewardKind {
    RUN_BOX, WEEK_BOX, STREAK_BOX, LEVEL_BOX, GOAL;

    companion object {
        fun of(raw: String?): RecentRewardKind? = entries.firstOrNull { it.name == raw }
    }
}

/** 최근에 받은 것 하나 — 그날(한국 날짜) · 종류 · SUP(목표 보너스만) · 받은 시각 */
data class RecentReward(val day: LocalDate?, val kind: RecentRewardKind, val amount: Double?, val atMs: Long?)

/** 막대의 눈금 자리 — 같은 걸음의 눈금은 하나로 합친다(목표 4,000 = 둘째 상자) */
data class TickSlot(
    val steps: Int,
    val box: Boolean,
    val goal: Boolean,
    /** 이 자리의 보상을 모두 받았다(서버) */
    val got: Boolean,
    /** 막대 위 자리(0 ~ 1) — 구간(0 → 첫 눈금 → … → 마지막)을 같은 너비로 */
    val position: Float,
)

/** 다음 보상 — 남은 걸음 · 무엇(상자 · 목표 보너스) */
data class NextReward(val stepsLeft: Int, val box: Boolean, val goal: Boolean, val bonus: Double?)

/** 홈 막대 하나를 그릴 값 — [DailyRewardsBar.of] 가 만든다 */
data class DailyBar(
    /** 오늘(한국 날짜)의 서버 상태가 있다 — 아니면 숫자 대신 "—" */
    val known: Boolean,
    val rewardSteps: Int,
    val goal: Int,
    val goalNext: Int?,
    val slots: List<TickSlot>,
    /** 서버가 확인한 걸음까지의 막대 몫 */
    val confirmedFraction: Float,
    /** 확인 중(올리는 중 · 달리는 중) 걸음까지 더한 몫 — [confirmedFraction] 이상. 눈금을 채우지 않는다 */
    val pendingFraction: Float,
    val pendingSteps: Int,
    /** 다음 보상 — 모두 받았거나 모르면 null */
    val next: NextReward?,
    /** 오늘 눈금을 모두 받았다(서버) */
    val allGot: Boolean,
    /** 문턱은 넘었는데 서버가 아직 받았다고 하지 않은 눈금이 있다(확인 중) */
    val waiting: Boolean,
    val goalPaid: Double?,
    val goalBonus: Double?,
    val streak: Int?,
    val streakBoxIn: Int?,
)

object DailyRewardsBar {

    /** 눈금 자리 — 걸음 순, 같은 걸음은 하나로, 0 이하는 버린다. 구간마다 같은 너비라 목표 20,000 이어도 눈금이 한쪽에 몰리지 않는다 */
    fun slots(ticks: List<RewardTick>): List<TickSlot> {
        val groups = ticks.filter { it.steps > 0 }.groupBy { it.steps }.toSortedMap()
        val n = groups.size
        return groups.entries.mapIndexed { i, (steps, same) ->
            TickSlot(
                steps = steps,
                box = same.any { it.kind == RewardTickKind.BOX },
                goal = same.any { it.kind == RewardTickKind.GOAL },
                got = same.all { it.got },
                position = (i + 1).toFloat() / n,
            )
        }
    }

    /** 걸음 → 막대 몫. 눈금 사이는 곧게 잇는다(구간마다 같은 너비). 눈금이 없으면 [goal] 까지 곧게 */
    fun fraction(steps: Int, slots: List<TickSlot>, goal: Int = 0): Float {
        if (steps <= 0) return 0f
        if (slots.isEmpty()) return if (goal > 0) (steps.toFloat() / goal).coerceIn(0f, 1f) else 0f
        var lowSteps = 0
        var lowPos = 0f
        for (slot in slots) {
            if (steps < slot.steps) {
                val span = (slot.steps - lowSteps).coerceAtLeast(1)
                return lowPos + (slot.position - lowPos) * (steps - lowSteps).toFloat() / span
            }
            lowSteps = slot.steps
            lowPos = slot.position
        }
        return 1f
    }

    /**
     * 막대 값. [state] 가 null 이거나 오늘(한국 날짜) 것이 아니면 known = false(숫자 · 눈금 받음 표시 없이).
     * [pendingSteps] 는 확인 중인 걸음(아직 올리지 못한 오늘 러닝 + 지금 달리는 러닝) — 옅은 구간으로만 보이고 눈금 · ✓ 를 바꾸지 않는다.
     */
    fun of(state: DailyRewards?, today: LocalDate, pendingSteps: Int): DailyBar {
        val known = state != null && state.isFor(today)
        val ticks = if (known) state!!.ticks else state?.ticks?.map { it.copy(got = false) }.orEmpty()
        val slots = slots(ticks)
        val goal = state?.goal ?: 0
        val reward = if (known) state!!.rewardStepsToday else 0
        val pending = pendingSteps.coerceAtLeast(0)
        val confirmed = if (known) fraction(reward, slots, goal) else 0f
        val withPending = if (known) fraction(reward + pending, slots, goal).coerceAtLeast(confirmed) else 0f
        val open = slots.filter { !it.got }
        val waiting = known && open.any { it.steps <= reward }
        val next = if (!known) null else open.firstOrNull { it.steps > reward }?.let { slot ->
            NextReward(
                stepsLeft = slot.steps - reward, box = slot.box, goal = slot.goal,
                bonus = if (slot.goal) state!!.goalBonus else null,
            )
        }
        return DailyBar(
            known = known,
            rewardSteps = reward,
            goal = goal,
            goalNext = state?.goalNext,
            slots = slots.map { if (known) it else it.copy(got = false) },
            confirmedFraction = confirmed,
            pendingFraction = withPending,
            pendingSteps = if (known) pending else 0,
            next = next,
            allGot = known && slots.isNotEmpty() && open.isEmpty(),
            waiting = waiting,
            goalPaid = if (known) state!!.goalPaid else null,
            goalBonus = state?.goalBonus,
            streak = if (known) state!!.streak else null,
            streakBoxIn = if (known) state!!.streakBoxIn else null,
        )
    }

    /** SUP 글자 — 소수 둘째 자리까지 내리고 끝의 0 은 뺀다(24 · 20.5 · 16.25) */
    fun supText(value: Double): String {
        if (!value.isFinite()) return "—"
        val down = java.math.BigDecimal.valueOf(value).setScale(4, java.math.RoundingMode.HALF_UP)
            .setScale(2, java.math.RoundingMode.DOWN).stripTrailingZeros()
        return if (down.scale() <= 0) "%,d".format(down.toLong()) else "%,.${down.scale()}f".format(down)
    }

    /**
     * 연속 칸의 "7일 중 며칠" — 지금까지 채운 연속(오늘 받았으면 [streak], 아직이면 [streak] − 1, 서버 streak 은 "오늘 채우면 며칠째")을
     * 7일 묶음으로(7 · 14 일째는 7, 8 일째는 1)
     */
    fun streakInWeek(streak: Int, paidToday: Boolean): Int {
        val done = (if (paidToday) streak else streak - 1).coerceAtLeast(0)
        return if (done == 0) 0 else (done - 1) % 7 + 1
    }

    /** 목표 하나의 하루 기본 보너스(연속 빼고) — 목표 1,000보마다 2.5 SUP(서버 goal_bonus 의 바탕, RewardEconomy.goalBaseBonus) */
    fun goalBaseBonus(goal: Int): Double = RewardEconomy.goalBaseBonus(goal)

    /** 확인 중 걸음 — 오늘(한국 날짜) 시작한, 아직 서버가 답하지 않은 러닝의 걸음 + 지금 달리는 러닝(오늘 시작했을 때) */
    fun pendingToday(runs: List<Pair<Long, Int>>, liveStartedAt: Long?, liveSteps: Int, today: LocalDate): Int {
        val waiting = runs.filter { (startedAt, _) -> DailyRewards.gameDay(startedAt) == today }.sumOf { it.second.coerceAtLeast(0) }
        val live = if (liveStartedAt != null && liveStartedAt > 0 && DailyRewards.gameDay(liveStartedAt) == today) liveSteps.coerceAtLeast(0) else 0
        return waiting + live
    }

    /** 하루 전체 걸음이 보상 걸음(+ 확인 중 걸음)보다 이만큼 많으면 "실내 · 경로 없는 걸음은 빠져요" 한 줄 */
    const val OFF_RUN_GAP = 2_000

    /**
     * 막대 아래 "실내 · 경로 없는 걸음은 상자 · 목표에 들지 않아요" — 오늘(한국 날짜) 상태를 알고, 폰이 센 하루 전체 걸음([allDaySteps],
     * 읽었을 때만 — [allDayKnown])이 서버의 보상 걸음과 확인 중 걸음을 더한 것보다 [OFF_RUN_GAP] 이상 많을 때만. 확인 중 걸음을 빼는 것은
     * 아직 올라가는 러닝의 걸음 때문에 잠깐 뜨지 않게
     */
    fun offRunNote(bar: DailyBar, allDaySteps: Int, allDayKnown: Boolean): Boolean =
        bar.known && allDayKnown && allDaySteps - bar.rewardSteps - bar.pendingSteps >= OFF_RUN_GAP

    /**
     * 막대 아래 "어젯밤 러닝은 어제 걸음으로 셌어요" — 이 계정의 가장 최근 러닝이 오늘(한국 날짜) 자정 전에 시작해 자정 뒤에 끝났을 때, 오늘만.
     * 서버는 러닝을 시작한 날에 센다(economy.game_day(started_at)). 무효 · 거절된 러닝은 어디에도 세지 않으므로 말하지 않는다
     */
    fun overnightNote(startedAt: Long, endedAt: Long, counted: Boolean, today: LocalDate): Boolean =
        counted && startedAt > 0 && endedAt > startedAt &&
            DailyRewards.gameDay(startedAt).isBefore(today) && DailyRewards.gameDay(endedAt) == today
}

/** 서버에 적힌 목표(box_status `rule` · `goal` · `goal_next`) — 동기화 때 폰 목표를 맞추는 데 쓴다 */
data class ServerGoal(val rule: String?, val goal: Int?, val goalNext: Int?) {
    /** 사람이 고른 목표 — 내일부터 바뀔 목표가 있으면 그것, 없으면 오늘 목표 */
    val chosen: Int? get() = goalNext?.takeIf { it > 0 } ?: goal?.takeIf { it > 0 }

    companion object {
        /** 0118 을 읽지 않는 빌드 · 예전 서버 — 규칙을 모른다(예전처럼 폰 목표를 보낸다) */
        val NO_RULE = ServerGoal(rule = null, goal = null, goalNext = null)

        /** `box_status` 글 → 서버 목표. 글이 JSON 객체가 아니면 null(읽지 못했다) */
        fun parse(body: String): ServerGoal? = runCatching {
            val o = Json.parseToJsonElement(body) as? JsonObject ?: return@runCatching null
            ServerGoal(rule = o.str("rule"), goal = o.int("goal"), goalNext = o.int("goal_next"))
        }.getOrNull()
    }
}

/** 동기화 때 폰 목표를 어떻게 맞출지 — [DailyGoalSync.action] */
sealed interface GoalSyncAction {
    /** 폰 목표를 서버에 보낸다(이 폰에서 바꾼 목표가 아직 닿지 않았다 · 예전 서버) */
    data object Push : GoalSyncAction

    /** 서버 목표를 폰에 적는다(새 폰 · 다시 깐 폰 · 다른 폰에서 바꾼 목표) */
    data class Adopt(val goal: Int) : GoalSyncAction

    /** 같다 — 할 일 없음 */
    data object Keep : GoalSyncAction

    /** 서버 목표를 읽지 못했다 — 덮어쓰지 않고 다음 동기화에서 다시 */
    data object Wait : GoalSyncAction
}

/**
 * 하루 러닝 목표 · 목표 보너스를 서버와 맞추는 규칙(오늘의 보상, 서버 0118) — EconomySync · ShoeRulesRepository · ServiceLocator 가 쓴다.
 *
 * 서버의 목표가 정본이다. 예전엔 동기화 때마다 폰 목표를 서버에 보내, 새 폰 · 다시 깐 폰(기본 6,000)이나 목표가 옛 값인 둘째 폰이
 * 사람이 고른 목표를 덮었다(바뀐 목표는 다음 날부터라 연속이 끊기기도 했다). 이제 이 폰에서 바꾼 목표가 서버에 닿지 않았을 때만 보내고,
 * 아니면 서버 목표를 받아 온다.
 */
object DailyGoalSync {

    /**
     * @param unsent 이 폰(이 계정)에서 바꾼 목표가 아직 서버에 닿지 않았다
     * @param server 방금 읽은 서버 목표 — 못 읽었으면 null
     * @param phoneGoal 지금 폰의 목표
     */
    fun action(unsent: Boolean, server: ServerGoal?, phoneGoal: Int): GoalSyncAction = when {
        unsent -> GoalSyncAction.Push
        server == null -> GoalSyncAction.Wait
        // 예전 서버(rule 이 없다) · 되돌린 서버(OLD) — 예전처럼 폰 목표를 보낸다
        server.rule != DailyRewards.RULE_REWARD_STEPS -> GoalSyncAction.Push
        else -> server.chosen?.takeIf { it != phoneGoal }?.let { GoalSyncAction.Adopt(it) } ?: GoalSyncAction.Keep
    }

    /** 바꾼 목표가 아직 닿지 않았다는 표시("계정\n목표") — 이 계정 것이거나 로그인 전에 바꾼 것일 때만 보낸다 */
    fun unsentFor(mark: String?, userId: String): Boolean {
        if (mark == null) return false
        val owner = mark.substringBefore('\n', missingDelimiterValue = "")
        return owner.isEmpty() || owner == userId
    }

    fun unsentMark(owner: String?, goal: Int): String = "${owner.orEmpty()}\n$goal"

    /**
     * 확인받은 러닝마다 그 러닝이 시작한 날(한국 날짜)의 목표 보너스를 한 번 더 청구할 날 — 서버가 러닝을 기록할 때 주지만(0118) 그 단계가
     * 실패하면 아무도 다시 하지 않는다. 서버가 받는 최근 7일 안의 날만, 오래된 날부터(뒷날의 연속이 앞날을 센다). 이미 받은 날(23505) ·
     * 못 채운 날(23514)은 서버가 거절할 뿐이다
     */
    fun signedRunDays(signedStartedAt: Collection<Long>, now: Long = System.currentTimeMillis(), windowDays: Long = 7): List<LocalDate> {
        val today = DailyRewards.gameDay(now)
        val from = today.minusDays(windowDays)
        return signedStartedAt.filter { it > 0 }.map { DailyRewards.gameDay(it) }
            .filter { !it.isBefore(from) && !it.isAfter(today) }.distinct().sorted()
    }

    /**
     * 오늘 목표를 채웠는데 보너스가 없다 — 서버 상태가 오늘(한국 날짜) 것이고 규칙이 보상 걸음이며 보상 걸음이 목표 이상인데 받은 보너스가 없다.
     * 이때 한 번(계정 · 날마다, 앱이 켜진 동안) 오늘을 청구해 본다. 금액은 서버 상태가 받았다고 할 때만 보인다
     */
    fun missedToday(state: DailyRewards?, today: LocalDate): Boolean =
        state != null && state.rule == DailyRewards.RULE_REWARD_STEPS && state.isFor(today) &&
            state.goal > 0 && state.rewardStepsToday >= state.goal && state.goalPaid == null
}

/** 새로 받은 보상 한 묶음 — 그날 · 종류마다 몇 개(목표 보너스는 SUP) */
data class RewardNewsItem(val day: LocalDate?, val kind: RecentRewardKind, val count: Int, val amount: Double?)

/** 러닝이 올라간 뒤 알릴 것 — [streak] 는 오늘 목표 보너스의 연속 일수(서버 값, 오늘 것일 때만) */
data class RewardNews(val items: List<RewardNewsItem>, val today: LocalDate, val streak: Int?)

object DailyRewardsNews {

    /** 러닝을 올린 뒤 알리는 종류 — 신발 레벨 상자는 강화 화면이 보인다 */
    val RUN_KINDS: Set<RecentRewardKind> = setOf(
        RecentRewardKind.RUN_BOX, RecentRewardKind.WEEK_BOX, RecentRewardKind.STREAK_BOX, RecentRewardKind.GOAL,
    )

    /** 받은 것 중 가장 늦은 시각 — 본 것으로 적어 둔다 */
    fun latestAt(recent: List<RecentReward>): Long? = recent.mapNotNull { it.atMs }.maxOrNull()

    /**
     * [seenAt](이 계정이 마지막으로 본 받은 시각) 뒤에 받은 것. [seenAt] 이 null(이 폰에서 처음 본다)이면 없음 —
     * 새 판을 처음 켠 날 지난 사흘 것을 한꺼번에 알리지 않는다. 받은 시각이 없는 줄은 가리지 못해 알리지 않는다.
     */
    fun fresh(recent: List<RecentReward>, seenAt: Long?, kinds: Set<RecentRewardKind> = RUN_KINDS): List<RecentReward> {
        if (seenAt == null) return emptyList()
        return recent.filter { it.kind in kinds && (it.atMs ?: Long.MIN_VALUE) > seenAt }.sortedBy { it.atMs }
    }

    /** 새로 받은 것을 그날 · 종류로 묶는다(날 순 — 지난날 먼저, 같은 날은 상자 · 목표 순) */
    fun of(fresh: List<RecentReward>, state: DailyRewards?, today: LocalDate): RewardNews? {
        if (fresh.isEmpty()) return null
        val items = fresh.groupBy { it.day to it.kind }.map { (key, rows) ->
            RewardNewsItem(
                day = key.first, kind = key.second, count = rows.size,
                amount = rows.mapNotNull { it.amount }.takeIf { it.isNotEmpty() }?.sum(),
            )
        }.sortedWith(compareBy<RewardNewsItem>({ it.day ?: today }, { it.kind.ordinal }))
        val streak = state?.takeIf { it.isFor(today) && it.goalPaid != null }?.streak
        return RewardNews(items, today, streak)
    }

    /** 그날이 오늘에서 며칠 전인가(0 = 오늘 · 1 = 어제). 날을 모르면 0 */
    fun daysAgo(item: RewardNewsItem, today: LocalDate): Long =
        item.day?.let { java.time.temporal.ChronoUnit.DAYS.between(it, today) }?.coerceAtLeast(0) ?: 0
}

private fun JsonObject.prim(k: String): JsonPrimitive? = (this[k] as? JsonPrimitive)?.takeIf { it !is JsonNull }
private fun JsonObject.str(k: String): String? = prim(k)?.content
private fun JsonObject.int(k: String): Int? = prim(k)?.let { it.intOrNull ?: it.doubleOrNull?.toInt() ?: it.content.toDoubleOrNull()?.toInt() }
private fun JsonObject.long(k: String): Long? = prim(k)?.let { it.longOrNull ?: it.doubleOrNull?.toLong() ?: it.content.toDoubleOrNull()?.toLong() }
private fun JsonObject.dbl(k: String): Double? = prim(k)?.let { it.doubleOrNull ?: it.content.toDoubleOrNull() }
private fun JsonObject.bool(k: String): Boolean? = prim(k)?.booleanOrNull
