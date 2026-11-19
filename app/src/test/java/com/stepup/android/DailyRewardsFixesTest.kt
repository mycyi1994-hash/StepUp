package com.stepup.android

import com.stepup.android.data.remote.ServerResult
import com.stepup.android.data.repo.ShoeRulesRepository
import com.stepup.android.domain.DailyGoalSync
import com.stepup.android.domain.DailyRewards
import com.stepup.android.domain.DailyRewardsBar
import com.stepup.android.domain.GoalSyncAction
import com.stepup.android.domain.RunVerdictReason
import com.stepup.android.domain.ServerGoal
import com.stepup.android.domain.ShoeRules
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate
import java.time.ZonedDateTime

/**
 * 오늘의 보상 고침(2026-11-19) — 동기화 때 목표를 덮지 않기 · 놓친 목표 보너스 다시 청구 · 설명 한 줄 셋의 조건.
 * 값은 서버가 준 것이고 여기서는 무엇을 할지 · 언제 보일지만 고른다.
 */
class DailyRewardsFixesTest {

    private val today = LocalDate.of(2026, 11, 19)

    private fun kst(day: Int, hour: Int, minute: Int = 0): Long =
        ZonedDateTime.of(2026, 11, day, hour, minute, 0, 0, DailyRewards.GAME_ZONE).toInstant().toEpochMilli()

    private val evening = kst(19, 18)

    private fun body(reward: Int, goal: Int, paid: Double? = null, goalNext: Int? = null, rule: String? = "REWARD_STEPS", day: String = "2026-11-19") =
        buildString {
            append("""{"steps_today":$reward,"run_boxes":[],"balance":10,""")
            if (rule != null) append(""""rule":"$rule",""")
            append(""""reward_steps_today":$reward,"day":"$day","goal":$goal,"goal_next":${goalNext ?: "null"},""")
            append(""""goal_paid":${paid ?: "null"},"goal_bonus":15.0,"streak":2,"ticks":[{"steps":$goal,"kind":"GOAL","got":${paid != null}}]}""")
        }

    // ── 1. 동기화 때 목표: 보낼지 받아 올지 ──

    @Test fun `바꾼 목표가 닿지 않았으면 서버 상태와 상관없이 보낸다`() {
        assertEquals(GoalSyncAction.Push, DailyGoalSync.action(unsent = true, server = null, phoneGoal = 9000))
        assertEquals(GoalSyncAction.Push, DailyGoalSync.action(true, ServerGoal("REWARD_STEPS", 6000, null), 9000))
    }

    @Test fun `새 폰 · 다시 깐 폰은 서버 목표를 받아 온다(내일 목표가 있으면 그것)`() {
        // 새 설치의 기본 6,000 이 사람이 고른 10,000 을 덮지 않는다
        assertEquals(GoalSyncAction.Adopt(10000), DailyGoalSync.action(false, ServerGoal("REWARD_STEPS", 10000, null), 6000))
        // 오늘 이미 달려 바꾼 목표가 내일부터면 고른 목표(goal_next)를 폰에
        assertEquals(GoalSyncAction.Adopt(12000), DailyGoalSync.action(false, ServerGoal("REWARD_STEPS", 8000, 12000), 8000))
        assertEquals(GoalSyncAction.Keep, DailyGoalSync.action(false, ServerGoal("REWARD_STEPS", 8000, 12000), 12000))
        assertEquals(GoalSyncAction.Keep, DailyGoalSync.action(false, ServerGoal("REWARD_STEPS", 8000, null), 8000))
        // 목표 칸이 없으면 덮지도 받지도 않는다
        assertEquals(GoalSyncAction.Keep, DailyGoalSync.action(false, ServerGoal("REWARD_STEPS", null, null), 8000))
    }

    @Test fun `서버를 못 읽으면 기다리고 예전 서버 · 되돌린 서버는 예전처럼 보낸다`() {
        assertEquals(GoalSyncAction.Wait, DailyGoalSync.action(false, null, 6000))
        assertEquals(GoalSyncAction.Push, DailyGoalSync.action(false, ServerGoal.NO_RULE, 6000))
        assertEquals(GoalSyncAction.Push, DailyGoalSync.action(false, ServerGoal("OLD", 10000, null), 6000))
    }

    @Test fun `box_status 글에서 서버 목표를 읽는다`() {
        assertEquals(ServerGoal("REWARD_STEPS", 8000, 12000), ServerGoal.parse(body(3000, 8000, goalNext = 12000)))
        // 예전 서버 — 규칙 · 목표 칸이 없다
        assertEquals(ServerGoal(null, null, null), ServerGoal.parse("""{"steps_today":10,"run_boxes":[]}"""))
        assertNull(ServerGoal.parse("not json"))
        assertEquals(12000, ServerGoal("REWARD_STEPS", 8000, 12000).chosen)
        assertEquals(8000, ServerGoal("REWARD_STEPS", 8000, null).chosen)
    }

    @Test fun `닿지 않은 목표 표시는 그 계정 · 로그인 전에 바꾼 것만`() {
        assertFalse(DailyGoalSync.unsentFor(null, "u1"))
        assertTrue(DailyGoalSync.unsentFor(DailyGoalSync.unsentMark(null, 9000), "u1"))
        assertTrue(DailyGoalSync.unsentFor(DailyGoalSync.unsentMark("u1", 9000), "u1"))
        assertFalse(DailyGoalSync.unsentFor(DailyGoalSync.unsentMark("u2", 9000), "u1"))
        assertEquals("u1\n9000", DailyGoalSync.unsentMark("u1", 9000))
    }

    // ── 2. 놓친 목표 보너스 다시 청구 ──

    @Test fun `확인받은 러닝이 시작한 날을 한국 날짜로 · 오래된 날부터 · 7일 안만`() {
        val days = DailyGoalSync.signedRunDays(
            listOf(kst(19, 9), kst(18, 23, 50), kst(19, 7), kst(10, 12), 0L),
            now = evening,
        )
        assertEquals(listOf(LocalDate.of(2026, 11, 18), today), days)
        assertEquals(emptyList<LocalDate>(), DailyGoalSync.signedRunDays(emptyList(), now = evening))
    }

    @Test fun `목표를 채웠는데 보너스가 없을 때만 오늘을 청구한다`() {
        val missed = DailyRewards.parse(body(8200, 8000), evening)
        assertTrue(DailyGoalSync.missedToday(missed, today))
        assertFalse(DailyGoalSync.missedToday(DailyRewards.parse(body(8200, 8000, paid = 20.0), evening), today))
        assertFalse(DailyGoalSync.missedToday(DailyRewards.parse(body(7999, 8000), evening), today))
        // 어제 상태 · 상태 없음
        assertFalse(DailyGoalSync.missedToday(DailyRewards.parse(body(8200, 8000, day = "2026-11-18"), evening), today))
        assertFalse(DailyGoalSync.missedToday(null, today))
        // 규칙을 모르는 상태(예전 앱이 읽은 칸)는 청구하지 않는다
        assertFalse(DailyGoalSync.missedToday(missed!!.copy(rule = null), today))
    }

    @Test fun `상자 현황을 읽다 놓친 보너스를 보면 계정 · 날마다 한 번만 청구한다`() = runBlocking {
        var raw = body(8200, 8000)
        var fetches = 0
        val claims = mutableListOf<LocalDate>()
        var paidCalls = 0
        var who = "account:a"
        val repo = ShoeRulesRepository(
            fetchRules = { ServerResult.Ok(ShoeRules.FALLBACK) },
            fetchBoxes = { ServerResult.Retry("unused") },
            fetchBoxesRaw = { fetches++; ServerResult.Ok(raw) },
            owner = { who },
            clock = { evening },
            claimGoalDay = { day -> claims += day; raw = body(8200, 8000, paid = 20.0); true },
            onGoalPaid = { paidCalls++ },
        )
        repo.refreshBoxes()
        assertEquals(listOf(today), claims)
        // 청구한 뒤 상태를 다시 읽는다 — 받은 보너스는 서버 상태가 말할 때만 보인다
        assertEquals(2, fetches)
        assertEquals(20.0, repo.daily.value!!.goalPaid!!, 1e-9)
        assertEquals(1, paidCalls)
        // 청구가 거절돼(이미 받음 · 못 채움) 상태가 그대로여도 다시 청구하지 않는다
        raw = body(8200, 8000)
        repo.refreshBoxes()
        assertEquals(1, claims.size)
        // 다른 계정은 따로
        who = "account:b"
        repo.refreshBoxes()
        assertEquals(2, claims.size)
    }

    @Test fun `받았거나 못 채웠으면 청구하지 않는다`() = runBlocking {
        var claims = 0
        for (raw in listOf(body(8200, 8000, paid = 20.0), body(3000, 8000), body(9000, 8000, rule = null))) {
            val repo = ShoeRulesRepository(
                fetchRules = { ServerResult.Ok(ShoeRules.FALLBACK) },
                fetchBoxes = { ServerResult.Retry("unused") },
                fetchBoxesRaw = { ServerResult.Ok(raw) },
                owner = { "account:a" },
                clock = { evening },
                claimGoalDay = { claims++; true },
            )
            repo.refreshBoxes()
        }
        assertEquals(0, claims)
    }

    @Test fun `동기화용 목표 읽기 — 못 읽으면 null · 0118 을 읽지 않는 빌드는 규칙 없음`() = runBlocking {
        val offline = ShoeRulesRepository(
            fetchRules = { ServerResult.Ok(ShoeRules.FALLBACK) },
            fetchBoxes = { ServerResult.Retry("unused") },
            fetchBoxesRaw = { ServerResult.Retry("offline") },
        )
        assertNull(offline.refreshGoal())
        // 함수가 없는 예전 서버(거절) — 예전처럼 폰 목표를 보낸다
        val refused = ShoeRulesRepository(
            fetchRules = { ServerResult.Ok(ShoeRules.FALLBACK) },
            fetchBoxes = { ServerResult.Retry("unused") },
            fetchBoxesRaw = { ServerResult.Rejected("Could not find the function public.box_status") },
        )
        assertEquals(ServerGoal.NO_RULE, refused.refreshGoal())
        val old = ShoeRulesRepository(fetchRules = { ServerResult.Ok(ShoeRules.FALLBACK) }, fetchBoxes = { ServerResult.Retry("unused") })
        assertEquals(ServerGoal.NO_RULE, old.refreshGoal())
        val fresh = ShoeRulesRepository(
            fetchRules = { ServerResult.Ok(ShoeRules.FALLBACK) },
            fetchBoxes = { ServerResult.Retry("unused") },
            fetchBoxesRaw = { ServerResult.Ok(body(0, 10000, goalNext = 12000)) },
            clock = { evening },
        )
        assertEquals(ServerGoal("REWARD_STEPS", 10000, 12000), fresh.refreshGoal())
        // 같은 글로 홈 막대도 바뀐다
        assertEquals(10000, fresh.daily.value!!.goal)
    }

    // ── 3. 설명 한 줄 ──

    @Test fun `크루 체력을 다 썼다는 서버 까닭이 있을 때만(보상 걸음 규칙에서)`() {
        assertTrue(RunVerdictReason.crewStaminaOut("크루 체력을 다 썼습니다", rewardSteps = true))
        assertTrue(RunVerdictReason.crewStaminaOut("하루 적립 상한에 걸렸습니다 · 크루 체력을 다 썼습니다", rewardSteps = true))
        assertFalse(RunVerdictReason.crewStaminaOut("크루 체력을 다 썼습니다", rewardSteps = false))
        assertFalse(RunVerdictReason.crewStaminaOut("에너지를 다 썼습니다", rewardSteps = true))
        assertFalse(RunVerdictReason.crewStaminaOut(null, rewardSteps = true))
        assertFalse(RunVerdictReason.crewStaminaOut("", rewardSteps = true))
    }

    @Test fun `하루 전체 걸음이 보상 걸음보다 2,000 이상 많을 때만 실내 걸음 안내`() {
        val bar = DailyRewardsBar.of(DailyRewards.parse(body(3000, 8000), evening), today, pendingSteps = 0)
        assertTrue(DailyRewardsBar.offRunNote(bar, allDaySteps = 5000, allDayKnown = true))
        assertFalse(DailyRewardsBar.offRunNote(bar, allDaySteps = 4999, allDayKnown = true))
        // 하루 전체 걸음을 아직 못 읽었다 · 걸음 권한 없음
        assertFalse(DailyRewardsBar.offRunNote(bar, allDaySteps = 9000, allDayKnown = false))
        // 아직 올라가는 러닝의 걸음(확인 중)은 빼고 본다
        val pending = DailyRewardsBar.of(DailyRewards.parse(body(3000, 8000), evening), today, pendingSteps = 1500)
        assertFalse(DailyRewardsBar.offRunNote(pending, allDaySteps = 6000, allDayKnown = true))
        assertTrue(DailyRewardsBar.offRunNote(pending, allDaySteps = 6500, allDayKnown = true))
        // 오늘 상태를 모르면(어제 상태) 말하지 않는다
        val stale = DailyRewardsBar.of(DailyRewards.parse(body(0, 8000, day = "2026-11-18"), evening), today, 0)
        assertFalse(DailyRewardsBar.offRunNote(stale, allDaySteps = 9000, allDayKnown = true))
    }

    @Test fun `어젯밤 시작해 오늘 끝난 러닝일 때만, 오늘만`() {
        assertTrue(DailyRewardsBar.overnightNote(kst(18, 23, 30), kst(19, 0, 20), counted = true, today = today))
        // 오늘 시작 · 어제 안에 끝남 · 이틀 전 시작해 어제 끝남
        assertFalse(DailyRewardsBar.overnightNote(kst(19, 0, 10), kst(19, 0, 50), counted = true, today = today))
        assertFalse(DailyRewardsBar.overnightNote(kst(18, 22, 0), kst(18, 23, 0), counted = true, today = today))
        assertFalse(DailyRewardsBar.overnightNote(kst(17, 23, 30), kst(18, 0, 20), counted = true, today = today))
        // 다음 날이 되면 사라진다
        assertFalse(DailyRewardsBar.overnightNote(kst(18, 23, 30), kst(19, 0, 20), counted = true, today = today.plusDays(1)))
        // 무효 · 거절된 러닝은 어디에도 세지 않는다
        assertFalse(DailyRewardsBar.overnightNote(kst(18, 23, 30), kst(19, 0, 20), counted = false, today = today))
    }
}
