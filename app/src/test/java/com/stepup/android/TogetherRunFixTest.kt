package com.stepup.android

import com.stepup.android.data.local.UploadState
import com.stepup.android.data.local.WalkSessionEntity
import com.stepup.android.data.remote.CrewRunLiveRow
import com.stepup.android.data.remote.serverJson
import com.stepup.android.data.repo.isGroupRun
import com.stepup.android.domain.CrewAlone
import com.stepup.android.domain.CrewAloneGrace
import com.stepup.android.domain.CrewEarning
import com.stepup.android.domain.CrewEmptySteps
import com.stepup.android.domain.CrewEndRetry
import com.stepup.android.domain.CrewRunLive
import com.stepup.android.ui.screens.walk.CrewEnd
import com.stepup.android.ui.screens.walk.ResultReward
import com.stepup.android.ui.screens.walk.Settle
import com.stepup.android.ui.screens.walk.crewEndNote
import com.stepup.android.ui.screens.walk.crewEndsOther
import com.stepup.android.ui.screens.walk.crewMoneyLines
import com.stepup.android.ui.screens.walk.crewOutcomeReady
import com.stepup.android.ui.screens.walk.crewResultReward
import kotlinx.serialization.decodeFromString
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * 함께 뛰기 점검 수정(2026-11-18) — 달리는 중 · 결과의 판단: 마지막 한 사람 대기 · 공용 체력 0 판단과 그동안의 걸음 ·
 * "끝냄" 다시 보내기 · 크루 결과의 보상 줄 · 마칠까요 안내.
 */
class TogetherRunFixTest {

    private fun live(
        state: String = "RUNNING",
        terminal: String? = null,
        earning: String = "ACCRUING",
        notEnded: Int = 3,
        pool: String? = """{"day":"2026-11-18","total":816,"remaining":612,"consumed":204,"contributors":3,"active_runners":3,
            "exhausted":false,"low":false,"version":4,"as_of":"2026-11-18T10:00:00+00:00"}""",
        extra: String = "",
    ): CrewRunLive = serverJson.decodeFromString<CrewRunLiveRow>(
        """{"crew_run":true,"server_now":"2026-11-18T10:30:00+00:00","party_id":5,"crew_id":"c1","day":"2026-11-18",
           "started_at":"2026-11-18T10:00:06+00:00","state":"$state","terminal_reason":${terminal?.let { "\"$it\"" } ?: "null"},
           "contribution":136,"consumed":11.25,"active_sec":135,"participants":3,"not_ended":$notEnded,"room_active_runners":2,
           ${pool?.let { "\"pool\":$it," }.orEmpty()}
           "bonus":{"status":"CONFIRMED","count":3,"pct":0.1},"level":{"level":2,"pct":0.01},"earning_state":"$earning"$extra}""",
    ).toDomain()!!

    // ── 마지막 한 사람: 바로 멈추지 않고 서버가 적립하는 데까지 ─────────────────

    @Test
    fun `서버가 알려 준 적립 끝까지 남은 시간만큼 기다린다`() {
        // 서버 시각 10:30:00 · 적립은 10:30:42 까지 — 폰 시계와 상관없이 42초
        val ended = live(
            state = "ENDED", terminal = "LAST_PARTICIPANT_END", earning = "ENDED", notEnded = 0,
            extra = ",\"ended_at\":\"2026-11-18T10:29:42+00:00\",\"reward_until\":\"2026-11-18T10:30:42+00:00\"",
        )
        assertTrue(ended.endedAlone)
        assertEquals(42_000L, CrewAlone.leftMs(ended.rewardUntilMs, ended.serverNowMs))
        assertEquals(ended.rewardUntilMs!! - 60_000L, ended.endedAtMs)
    }

    @Test
    fun `적립 끝을 모르면 처음 안 때부터 60초, 이미 지났으면 바로 멈춘다`() {
        // 예전 서버 — 두 칸이 없다
        val old = live(state = "ENDED", terminal = "LAST_PARTICIPANT_END", earning = "ENDED", notEnded = 0)
        assertNull(old.rewardUntilMs)
        assertNull(old.endedAtMs)
        assertEquals(CrewAlone.GRACE_MS, CrewAlone.leftMs(old.rewardUntilMs, old.serverNowMs))
        // 한참 끊겼다 돌아왔다 — 적립 끝이 이미 지났다
        assertEquals(0L, CrewAlone.leftMs(rewardUntilMs = 1_000L, serverNowMs = 500_000L))
        // 서버 답이 이상해도 60초를 넘겨 기다리지 않는다
        assertEquals(CrewAlone.GRACE_MS, CrewAlone.leftMs(rewardUntilMs = 900_000L, serverNowMs = 0L))
    }

    @Test
    fun `남은 초는 올림으로 세고 지나면 0 이다`() {
        val grace = CrewAloneGrace(startedAt = 7L, stopAtElapsedMs = 100_000L)
        assertEquals(60, grace.leftSec(40_000L))
        assertEquals(1, grace.leftSec(99_500L))
        assertEquals(0, grace.leftSec(100_000L))
        assertEquals(0, grace.leftSec(250_000L))
        assertEquals(42, CrewAlone.seconds(41_001L))
    }

    @Test
    fun `내가 달리는 중이고 끝나지 않은 사람이 둘이면 내가 마칠 때 다른 사람도 끝난다`() {
        assertTrue(crewEndsOther(live(notEnded = 2)))
        assertFalse(crewEndsOther(live(notEnded = 3)))
        assertFalse(crewEndsOther(live(notEnded = 1)))
        // 이미 끝난 내 참여 · 서버 상태를 모를 때는 그렇게 말하지 않는다
        assertFalse(crewEndsOther(live(state = "ENDED", terminal = "LAST_PARTICIPANT_END", earning = "ENDED", notEnded = 2)))
        assertFalse(crewEndsOther(null))

        assertEquals(R.string.run_crew_end_last_note, crewEndNote(CrewEnd.Member, endsOther = true))
        assertEquals(R.string.run_crew_end_last_note, crewEndNote(CrewEnd.Host, endsOther = true))
        assertEquals(R.string.run_crew_end_last_note, crewEndNote(CrewEnd.Alone, endsOther = true))
        assertEquals(R.string.run_crew_end_host_note, crewEndNote(CrewEnd.Host, endsOther = false))
        assertEquals(R.string.run_crew_end_member_note, crewEndNote(CrewEnd.Member, endsOther = false))
    }

    // ── 공용 체력이 0 인가 · 그동안의 걸음 ─────────────────────────────────

    @Test
    fun `서버가 다 썼다고 했거나 총량이 0 이거나 흘려 본 남은 양이 0 이하면 비었다`() {
        val normal = live()
        assertFalse(normal.poolEmptyAfter(0L))
        // 3명이 12초에 1씩 — 612 는 2448초 뒤에 0
        assertFalse(normal.poolEmptyAfter(2_447_000L))
        assertTrue(normal.poolEmptyAfter(2_448_000L))
        // 서버가 다 썼다고 했다
        assertTrue(live(earning = "POOL_EMPTY").poolEmptyAfter(0L))
        // 기여 없이 출발 — 총량 0(예전 서버는 다 썼다고 하지 않았다)
        val zero = live(
            pool = """{"day":"2026-11-18","total":0,"remaining":0,"consumed":0,"contributors":2,"active_runners":2,
                "exhausted":false,"low":false,"version":1}""",
        )
        assertEquals(CrewEarning.ACCRUING, zero.earning)
        assertTrue(zero.poolEmptyAfter(0L))
        // 풀을 모르면 비었다고 하지 않는다
        assertFalse(live(pool = null).poolEmptyAfter(10_000_000L))
        // 쓰는 사람이 없으면 흘려 보지 않는다
        val idle = live(
            pool = """{"day":"2026-11-18","total":816,"remaining":5,"consumed":811,"contributors":3,"active_runners":0,
                "exhausted":false,"low":true,"version":9}""",
        )
        assertFalse(idle.poolEmptyAfter(10_000_000L))
    }

    @Test
    fun `공용 체력이 0 이던 동안의 걸음만 예상에서 뺀다`() {
        var steps = CrewEmptySteps(startedAt = 42L)
        assertFalse(steps.emptyNow)
        assertEquals(1_000, steps.earningSteps(1_000))

        // 1,000걸음에서 0 이 됐다 — 그 뒤 걸음은 세지 않는다
        steps = steps.next(1_000, empty = true)
        assertTrue(steps.emptyNow)
        assertEquals(1_000, steps.earningSteps(1_600))
        // 그대로면 바뀌지 않는다(같은 값 — 다시 적지 않는다)
        assertSame(steps, steps.next(1_600, empty = true))

        // 1,600걸음에서 다시 채워졌다(다른 방이 출발) — 600걸음을 빼고 그 뒤는 다시 센다
        steps = steps.next(1_600, empty = false)
        assertFalse(steps.emptyNow)
        assertEquals(600, steps.excluded)
        assertEquals(1_400, steps.earningSteps(2_000))
        assertSame(steps, steps.next(2_000, empty = false))

        // 두 번째 빈 구간
        steps = steps.next(2_000, empty = true).next(2_300, empty = false)
        assertEquals(900, steps.excluded)
        assertEquals(1_600, steps.earningSteps(2_500))
        assertEquals(42L, steps.startedAt)
    }

    // ── "끝냄"은 닿을 때까지 ───────────────────────────────────────────────

    @Test
    fun `끝냄은 3초부터 두 배씩 60초까지 벌려 10분 안에서 다시 보낸다`() {
        assertEquals(listOf(3_000L, 6_000L, 12_000L, 24_000L, 48_000L, 60_000L, 60_000L), (0..6).map { CrewEndRetry.delayMs(it) })
        assertEquals(60_000L, CrewEndRetry.delayMs(40))
        assertFalse(CrewEndRetry.over(spentMs = 0L, attempt = 0))
        assertFalse(CrewEndRetry.over(spentMs = CrewEndRetry.WINDOW_MS - 60_000L, attempt = 9))
        assertTrue(CrewEndRetry.over(spentMs = CrewEndRetry.WINDOW_MS - 59_999L, attempt = 9))
    }

    // ── 결과: 확인 전에는 금액을 적지 않는다 · 0 이어도 크루 줄을 읽는다 ─────────────

    @Test
    fun `크루 러닝 결과는 확인 전에 이 폰이 셈한 금액을 보이지 않는다`() {
        val pending = ResultReward("예상 보상", "+12.40", Settle.PENDING, notes = listOf("다시 로그인해 주세요"), signIn = true)
        val shown = crewResultReward(pending, label = "보상", pendingNote = "크루 보상은 정산이 끝나면 보여요.")
        assertEquals("", shown.amount)
        assertEquals("보상", shown.label)
        assertEquals(Settle.PENDING, shown.settle)
        assertEquals(listOf("크루 보상은 정산이 끝나면 보여요.", "다시 로그인해 주세요"), shown.notes)
        assertTrue(shown.signIn)

        // 확인된 금액 · 무효 · 미지급은 그대로
        val done = ResultReward("보상", "+8.10", Settle.DONE)
        assertSame(done, crewResultReward(done, "보상", "크루 보상은 정산이 끝나면 보여요."))
        val unpaid = ResultReward("보상", "0", Settle.NOT_PAID, listOf("이번 러닝은 적립이 없어요"))
        assertSame(unpaid, crewResultReward(unpaid, "보상", "크루 보상은 정산이 끝나면 보여요."))
        val void = ResultReward("보상", "0", Settle.VOID)
        assertSame(void, crewResultReward(void, "보상", "크루 보상은 정산이 끝나면 보여요."))
    }

    @Test
    fun `크루 러닝 한 줄은 러닝이 기록됐으면 적립이 0 이어도 읽는다`() {
        assertTrue(crewOutcomeReady(UploadState.SIGNED.name))
        assertFalse(crewOutcomeReady(UploadState.PENDING.name))
        assertFalse(crewOutcomeReady(UploadState.FAILED.name))
        assertFalse(crewOutcomeReady(UploadState.REJECTED.name))
        assertFalse(crewOutcomeReady(null))
        // 금액 줄은 적립이 있을 때만 — 0 이면 까닭 줄만
        assertTrue(crewMoneyLines(0.01))
        assertFalse(crewMoneyLines(0.0))
        assertFalse(crewMoneyLines(null))
    }

    // ── 올리기 차례: 모임 러닝 ─────────────────────────────────────────────

    @Test
    fun `크루가 적혔거나 인원이 둘 이상이면 모임 러닝이다`() {
        fun row(partySize: Int, crewId: String) = WalkSessionEntity(
            startedAt = 1_000L, endedAt = 2_000L, steps = 10, durationSec = 1, distanceMeters = 7.0, calories = 0.4,
            pointsEarned = 0.0, partySize = partySize, crewId = crewId,
        )
        assertFalse(row(1, "").isGroupRun())
        assertTrue(row(2, "").isGroupRun())
        // 혼자 남아 끝낸 크루 러닝(끝낼 때 인원 1)도 크루가 적혀 있다
        assertTrue(row(1, "crew-1").isGroupRun())
    }
}
