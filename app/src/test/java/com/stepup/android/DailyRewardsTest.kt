package com.stepup.android

import com.stepup.android.data.prefs.UserPrefs
import com.stepup.android.data.remote.ServerResult
import com.stepup.android.data.repo.DailyRewardsStore
import com.stepup.android.data.repo.SavedBoxStatus
import com.stepup.android.data.repo.ShoeRulesRepository
import com.stepup.android.domain.AchievementBook
import com.stepup.android.domain.DailyRewards
import com.stepup.android.domain.DailyRewardsBar
import com.stepup.android.domain.DailyRewardsNews
import com.stepup.android.domain.RecentReward
import com.stepup.android.domain.RecentRewardKind
import com.stepup.android.domain.RewardTick
import com.stepup.android.domain.RewardTickKind
import com.stepup.android.domain.RunVerdictReason
import com.stepup.android.domain.ShoeRules
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate
import java.time.ZonedDateTime

/**
 * 오늘의 보상(서버 0118, docs/forge-xp-daily-contract-2026-11-19.md) — box_status 새 칸 읽기 · 눈금 자리 · 확인 중 구간 ·
 * "새로 받은 것" 가리기 · 목표 범위 맞춤 · 폰에 남긴 상태. 값은 서버가 준 것이고 여기서는 무엇을 어디에 보일지만 고른다.
 */
class DailyRewardsTest {

    private val today = LocalDate.of(2026, 11, 19)

    /** 2026-11-19 18:00 KST(= 09:00 UTC) */
    private val noonish = ZonedDateTime.of(2026, 11, 19, 18, 0, 0, 0, DailyRewards.GAME_ZONE).toInstant().toEpochMilli()

    private val body = """
        {"daily_free":1,"steps_today":3240,"run_boxes":[{"steps":2000,"got":true},{"steps":4000,"got":false}],
         "steps_week":21000,"week_target":25000,"week_got":false,"streak_days":7,"levels_got":[5],
         "normal_price":40,"normal_limit":3,"normal_bought":0,"premium_price":600,"premium_limit":2,"premium_bought":0,
         "free_left":2,"gift_left":0,"balance":120.5,
         "rule":"REWARD_STEPS","reward_steps_today":3240,"day":"2026-11-19",
         "goal":8000,"goal_next":null,"goal_paid":null,"goal_bonus":24.0,
         "streak":3,"streak_best":9,"streak_box_in":4,
         "ticks":[{"steps":2000,"kind":"BOX","got":true},{"steps":4000,"kind":"BOX","got":false},{"steps":8000,"kind":"GOAL","got":false}],
         "surge":{"steps":21000,"target":80000,"reward":250,"claimed":false},
         "recent":[{"day":"2026-11-19","kind":"RUN_BOX","amount":null,"at":"2026-11-19T09:12:00+00"},
                   {"day":"2026-11-18","kind":"GOAL","amount":22.0,"at":"2026-11-18 11:00:00.123+00"}]}
    """.trimIndent()

    // ── 읽기 ──

    @Test fun `box_status 새 칸을 그대로 읽는다`() {
        val d = DailyRewards.parse(body, noonish)!!
        assertEquals(DailyRewards.RULE_REWARD_STEPS, d.rule)
        assertEquals(today, d.day)
        assertEquals(3240, d.rewardStepsToday)
        assertEquals(8000, d.goal)
        assertNull(d.goalNext)
        assertNull(d.goalPaid)
        assertEquals(24.0, d.goalBonus!!, 1e-9)
        assertEquals(3, d.streak)
        assertEquals(9, d.streakBest)
        assertEquals(4, d.streakBoxIn)
        assertEquals(listOf(2000 to RewardTickKind.BOX, 4000 to RewardTickKind.BOX, 8000 to RewardTickKind.GOAL), d.ticks.map { it.steps to it.kind })
        assertEquals(listOf(true, false, false), d.ticks.map { it.got })
        assertEquals(80000L, d.surge!!.target)
        assertEquals(250.0, d.surge!!.reward, 1e-9)
        assertEquals(21000, d.stepsWeek)
        assertEquals(25000, d.weekTarget)
        // 받은 시각은 "+00" · 공백 · 소수 초도 읽는다
        assertEquals(ZonedDateTime.of(2026, 11, 19, 9, 12, 0, 0, java.time.ZoneOffset.UTC).toInstant().toEpochMilli(), d.recent[0].atMs)
        assertNotNull(d.recent[1].atMs)
        assertEquals(22.0, d.recent[1].amount!!, 1e-9)
        assertNull(d.recent[0].amount)
    }

    @Test fun `예전 서버 · 되돌리기 스위치면 오늘의 보상은 없다(홈은 예전 모습)`() {
        val old = """{"daily_free":1,"steps_today":3240,"run_boxes":[],"steps_week":0,"week_target":25000}"""
        assertNull(DailyRewards.parse(old, noonish))
        assertNull(DailyRewards.parse(body.replace("\"rule\":\"REWARD_STEPS\"", "\"rule\":\"OLD\""), noonish))
        assertNull(DailyRewards.parse("not json", noonish))
    }

    @Test fun `서버가 날을 보내지 않으면 받은 시각의 한국 날짜 · 눈금이 없으면 상자와 목표로`() {
        val noDay = body.replace("\"day\":\"2026-11-19\",", "").replace(Regex("\"ticks\":\\[[^]]*],"), "")
        // 2026-11-19 23:30 UTC = 2026-11-20 08:30 KST
        val lateUtc = ZonedDateTime.of(2026, 11, 19, 23, 30, 0, 0, java.time.ZoneOffset.UTC).toInstant().toEpochMilli()
        val d = DailyRewards.parse(noDay, lateUtc)!!
        assertEquals(LocalDate.of(2026, 11, 20), d.day)
        assertEquals(listOf(2000, 4000, 8000), d.ticks.map { it.steps })
        assertEquals(RewardTickKind.GOAL, d.ticks.last().kind)
        assertFalse(d.ticks.last().got)
    }

    // ── 눈금 자리 ──

    @Test fun `구간은 같은 너비 — 목표 8,000 이면 셋, 3,240 은 둘째 구간 가운데쯤`() {
        val slots = DailyRewardsBar.slots(DailyRewards.parse(body, noonish)!!.ticks)
        assertEquals(listOf(1f / 3, 2f / 3, 1f), slots.map { it.position })
        assertEquals(0f, DailyRewardsBar.fraction(0, slots), 1e-6f)
        assertEquals(1f / 3, DailyRewardsBar.fraction(2000, slots), 1e-6f)
        assertEquals(1f / 3 + (1f / 3) * 0.62f, DailyRewardsBar.fraction(3240, slots), 1e-5f)
        assertEquals(1f, DailyRewardsBar.fraction(8000, slots), 1e-6f)
        assertEquals(1f, DailyRewardsBar.fraction(30000, slots), 1e-6f)
    }

    @Test fun `목표 4,000 은 둘째 상자와 하나로 합친다`() {
        val ticks = listOf(
            RewardTick(2000, RewardTickKind.BOX, true), RewardTick(4000, RewardTickKind.BOX, false), RewardTick(4000, RewardTickKind.GOAL, false),
        )
        val slots = DailyRewardsBar.slots(ticks)
        assertEquals(2, slots.size)
        assertEquals(listOf(0.5f, 1f), slots.map { it.position })
        assertTrue(slots[1].box && slots[1].goal)
        assertFalse(slots[1].got)
        // 한쪽만 받았으면 그 자리는 아직(✓ 없음)
        assertFalse(DailyRewardsBar.slots(ticks.mapIndexed { i, t -> if (i == 1) t.copy(got = true) else t })[1].got)
    }

    @Test fun `목표 20,000 이어도 눈금이 한쪽에 몰리지 않는다`() {
        val ticks = listOf(
            RewardTick(2000, RewardTickKind.BOX, false), RewardTick(4000, RewardTickKind.BOX, false), RewardTick(20000, RewardTickKind.GOAL, false),
        )
        val slots = DailyRewardsBar.slots(ticks)
        assertEquals(listOf(1f / 3, 2f / 3, 1f), slots.map { it.position })
        // 4,000 → 20,000 구간도 같은 너비 — 12,000 은 그 구간의 절반
        assertEquals(2f / 3 + (1f / 3) * 0.5f, DailyRewardsBar.fraction(12000, slots), 1e-5f)
    }

    // ── 막대 ──

    @Test fun `확인 중 걸음은 옅은 구간만 — 눈금 받음 · 다음 보상은 서버 걸음으로`() {
        val d = DailyRewards.parse(body, noonish)!!
        val bar = DailyRewardsBar.of(d, today, pendingSteps = 1500)
        assertTrue(bar.known)
        assertEquals(3240, bar.rewardSteps)
        assertEquals(1500, bar.pendingSteps)
        assertTrue(bar.pendingFraction > bar.confirmedFraction)
        // 3,240 + 1,500 은 4,000 을 넘지만 둘째 상자는 받지 않았다(서버가 아직)
        assertEquals(listOf(true, false, false), bar.slots.map { it.got })
        assertEquals(760, bar.next!!.stepsLeft)
        assertTrue(bar.next!!.box)
        assertFalse(bar.next!!.goal)
        assertFalse(bar.allGot)
        assertFalse(bar.waiting)
    }

    @Test fun `다음 보상이 목표면 서버의 보너스 · 모두 받았으면 다음이 없다`() {
        val d = DailyRewards.parse(body, noonish)!!
        val past4000 = d.copy(rewardStepsToday = 5000, ticks = d.ticks.map { if (it.steps <= 4000) it.copy(got = true) else it })
        val next = DailyRewardsBar.of(past4000, today, 0).next!!
        assertEquals(3000, next.stepsLeft)
        assertTrue(next.goal)
        assertEquals(24.0, next.bonus!!, 1e-9)
        val done = past4000.copy(rewardStepsToday = 8200, goalPaid = 24.0, ticks = d.ticks.map { it.copy(got = true) })
        val bar = DailyRewardsBar.of(done, today, 0)
        assertNull(bar.next)
        assertTrue(bar.allGot)
        assertEquals(1f, bar.confirmedFraction, 1e-6f)
    }

    @Test fun `문턱은 넘었는데 서버가 아직 주지 않았으면 확인 중`() {
        val d = DailyRewards.parse(body, noonish)!!.copy(rewardStepsToday = 4100)
        val bar = DailyRewardsBar.of(d, today, 0)
        assertTrue(bar.waiting)
        // 기다리는 눈금은 건너뛰고 그다음(목표)을 알린다
        assertEquals(3900, bar.next!!.stepsLeft)
    }

    @Test fun `어제 받은 상태는 숫자 · 받음 없이 "—"`() {
        val d = DailyRewards.parse(body, noonish)!!
        val bar = DailyRewardsBar.of(d, today.plusDays(1), pendingSteps = 900)
        assertFalse(bar.known)
        assertEquals(0, bar.rewardSteps)
        assertEquals(0f, bar.confirmedFraction, 0f)
        assertEquals(0, bar.pendingSteps)
        assertTrue(bar.slots.none { it.got })
        assertNull(bar.next)
        assertNull(bar.streak)
        assertNull(bar.goalPaid)
    }

    @Test fun `확인 중 걸음은 오늘(한국 날짜) 시작한 러닝만`() {
        val yesterdayLate = ZonedDateTime.of(2026, 11, 18, 23, 50, 0, 0, DailyRewards.GAME_ZONE).toInstant().toEpochMilli()
        val morning = ZonedDateTime.of(2026, 11, 19, 7, 0, 0, 0, DailyRewards.GAME_ZONE).toInstant().toEpochMilli()
        val runs = listOf(yesterdayLate to 3000, morning to 1200)
        assertEquals(1200, DailyRewardsBar.pendingToday(runs, liveStartedAt = null, liveSteps = 0, today = today))
        assertEquals(1200 + 450, DailyRewardsBar.pendingToday(runs, liveStartedAt = morning + 3_600_000, liveSteps = 450, today = today))
        // 자정 전에 시작한 러닝은 어제 것(출발한 날)
        assertEquals(1200, DailyRewardsBar.pendingToday(runs, liveStartedAt = yesterdayLate, liveSteps = 450, today = today))
    }

    @Test fun `SUP 글자 · 연속 칸`() {
        assertEquals("24", DailyRewardsBar.supText(24.0))
        assertEquals("20.5", DailyRewardsBar.supText(20.5))
        assertEquals("16.25", DailyRewardsBar.supText(16.25))
        assertEquals("16.25", DailyRewardsBar.supText(16.259))
        assertEquals(0, DailyRewardsBar.streakInWeek(1, paidToday = false))
        assertEquals(3, DailyRewardsBar.streakInWeek(3, paidToday = true))
        assertEquals(2, DailyRewardsBar.streakInWeek(3, paidToday = false))
        assertEquals(7, DailyRewardsBar.streakInWeek(7, paidToday = true))
        assertEquals(1, DailyRewardsBar.streakInWeek(8, paidToday = true))
        assertEquals(7, DailyRewardsBar.streakInWeek(14, paidToday = true))
        // 지금까지 이어진 연속 — 서버 streak 은 "오늘 채우면 며칠째"
        val d = DailyRewards.parse(body, noonish)!!
        assertEquals(2, d.streakNow(today))
        assertEquals(3, d.copy(goalPaid = 24.0).streakNow(today))
        assertNull(d.streakNow(today.plusDays(1)))
    }

    // ── 새로 받은 것 ──

    @Test fun `처음 보는 계정은 알리지 않고 본 뒤의 것만 — 레벨 상자는 러닝 뒤에 알리지 않는다`() {
        val at = 1_000_000L
        val recent = listOf(
            RecentReward(today, RecentRewardKind.RUN_BOX, null, at),
            RecentReward(today, RecentRewardKind.RUN_BOX, null, at + 10),
            RecentReward(today, RecentRewardKind.LEVEL_BOX, null, at + 20),
            RecentReward(today.minusDays(1), RecentRewardKind.GOAL, 22.0, at - 100),
        )
        assertTrue(DailyRewardsNews.fresh(recent, seenAt = null).isEmpty())
        assertEquals(at + 20, DailyRewardsNews.latestAt(recent))
        val fresh = DailyRewardsNews.fresh(recent, seenAt = at - 200)
        assertEquals(3, fresh.size)
        val state = DailyRewards.parse(body, noonish)!!
        val news = DailyRewardsNews.of(fresh, state, today)!!
        // 지난날 먼저 — 어제 목표 보너스, 오늘 러닝 상자 2개
        assertEquals(listOf(RecentRewardKind.GOAL, RecentRewardKind.RUN_BOX), news.items.map { it.kind })
        assertEquals(1L, DailyRewardsNews.daysAgo(news.items[0], today))
        assertEquals(2, news.items[1].count)
        assertEquals(22.0, news.items[0].amount!!, 1e-9)
        // 오늘 목표 보너스를 아직 받지 않았으면 연속 일수를 붙이지 않는다
        assertNull(news.streak)
        assertEquals(3, DailyRewardsNews.of(fresh, state.copy(goalPaid = 24.0), today)!!.streak)
        assertNull(DailyRewardsNews.of(emptyList(), state, today))
    }

    @Test fun `러닝 뒤 상태를 다시 읽고 새로 받은 것만 한 번`() = runBlocking {
        val store = MemoryStore()
        var raw = body
        val repo = ShoeRulesRepository(
            fetchRules = { ServerResult.Ok(ShoeRules.FALLBACK) },
            fetchBoxes = { ServerResult.Retry("unused") },
            fetchBoxesRaw = { ServerResult.Ok(raw) },
            store = store,
            owner = { "account:a" },
            clock = { noonish },
        )
        // 처음 — 지금까지 받은 것을 본 것으로 적고 알리지 않는다
        assertNull(repo.refreshForNews())
        assertEquals(3240, repo.daily.value!!.rewardStepsToday)
        assertEquals(3240, repo.boxes.value!!.stepsToday)
        assertNotNull(store.seen["account:a"])
        // 새 러닝 상자 하나 — 그것만
        raw = body.replace(
            "\"recent\":[",
            "\"recent\":[{\"day\":\"2026-11-19\",\"kind\":\"RUN_BOX\",\"amount\":null,\"at\":\"2026-11-19T09:30:00+00\"},",
        )
        val news = repo.refreshForNews()!!
        assertEquals(listOf(RecentRewardKind.RUN_BOX), news.items.map { it.kind })
        assertEquals(1, news.items.single().count)
        // 같은 것은 다시 알리지 않는다
        assertNull(repo.refreshForNews())
        // 남긴 글은 계정마다
        assertEquals("account:a", store.owner)
    }

    @Test fun `폰에 남긴 상태를 되살린다 — 상자 현황은 오늘 받은 것만, 다른 계정 것은 읽지 않는다`() = runBlocking {
        val yesterday = noonish - 86_400_000L
        val store = MemoryStore().apply { owner = "account:a"; saved = SavedBoxStatus(body.replace("\"reward_steps_today\":3240,\"day\":\"2026-11-19\"", "\"reward_steps_today\":3240,\"day\":\"2026-11-18\""), yesterday) }
        val repo = ShoeRulesRepository(
            fetchRules = { ServerResult.Ok(ShoeRules.FALLBACK) },
            fetchBoxes = { ServerResult.Retry("unused") },
            fetchBoxesRaw = { ServerResult.Retry("offline") },
            store = store,
            owner = { "account:a" },
            clock = { noonish },
        )
        repo.restore()
        val d = repo.daily.value!!
        assertFalse(d.isFor(today))
        assertFalse(DailyRewardsBar.of(d, today, 0).known)
        assertNull(repo.boxes.value)

        val other = ShoeRulesRepository(
            fetchRules = { ServerResult.Ok(ShoeRules.FALLBACK) },
            fetchBoxes = { ServerResult.Retry("unused") },
            fetchBoxesRaw = { ServerResult.Retry("offline") },
            store = store,
            owner = { "account:b" },
            clock = { noonish },
        )
        other.restore()
        assertNull(other.daily.value)
    }

    // ── 목표 · 까닭 · 업적 ──

    @Test fun `목표는 4,000 ~ 20,000 · 500 단위 — 새 설치 6,000 · 예전 사용자 8,000`() {
        assertEquals(6000, UserPrefs.goalOf(null, legacyDefault = false))
        assertEquals(8000, UserPrefs.goalOf(null, legacyDefault = true))
        assertEquals(4000, UserPrefs.goalOf(3000, legacyDefault = false))
        assertEquals(4000, UserPrefs.goalOf(3500, legacyDefault = true))
        assertEquals(20000, UserPrefs.goalOf(30000, legacyDefault = false))
        assertEquals(12000, UserPrefs.goalOf(12000, legacyDefault = false))
        assertEquals(6500, UserPrefs.goalOf(6250, legacyDefault = false))
    }

    @Test fun `경로가 없어 보상 걸음에 들지 않은 까닭 — 서버 글 그대로`() {
        val note = RunVerdictReason.note("SIGNED", 1.2, "경로가 확인되지 않아 보상 걸음에 들지 않았습니다", "")!!
        assertFalse(note.zero)
        assertEquals(listOf(RunVerdictReason.Kind.NO_ROUTE_REWARD), note.kinds)
    }

    @Test fun `목표 연속 업적은 서버 연속 — 예전에 연 값은 다시 잠기지 않는다`() {
        assertEquals(9, AchievementBook.streakCount(localBest = 4, serverBest = 9))
        assertEquals(12, AchievementBook.streakCount(localBest = 12, serverBest = 9))
        assertEquals(4, AchievementBook.streakCount(localBest = 4, serverBest = null))
    }

    private class MemoryStore : DailyRewardsStore {
        var owner: String? = null
        var saved: SavedBoxStatus? = null
        val seen = mutableMapOf<String, Long>()
        override suspend fun read(owner: String): SavedBoxStatus? = saved.takeIf { this.owner == owner }
        override suspend fun write(owner: String, raw: String, fetchedAt: Long) {
            this.owner = owner
            saved = SavedBoxStatus(raw, fetchedAt)
        }
        override suspend fun seenAt(owner: String): Long? = seen[owner]
        override suspend fun setSeenAt(owner: String, at: Long) {
            seen[owner] = at
        }
    }
}
