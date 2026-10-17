package com.stepup.android

import com.stepup.android.data.prefs.OverspeedStop
import com.stepup.android.data.remote.ServerResult
import com.stepup.android.data.repo.OverspeedReporter
import com.stepup.android.domain.RunOverspeed
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * 과속이 30초 이어지면 러닝을 멈추고 패널티(2026-10-17 사용자 결정 "애초에 경고가 뜨고 그렇게 유지되면 강제로 멈추고 패널티") —
 * 기준 25km/h · 30초 · 남은 내구도 절반(서버 0065) · 10분 러닝 금지.
 */
class RunOverspeedTest {

    /** [start] 부터 [everyMs] 마다 과속 구간이 [count] 번 들어왔다 */
    private fun flagged(start: Long, everyMs: Long, count: Int): RunOverspeed.Streak? =
        (0 until count).fold(null as RunOverspeed.Streak?) { streak, i -> RunOverspeed.onFlagged(streak, start + everyMs * i) }

    @Test fun `과속 구간이 30초 이어지면 멈춘다`() {
        // 차 — 2.5초마다 과속 구간
        assertFalse(RunOverspeed.shouldStop(flagged(1_000L, 2_500L, 12))) // 27.5초
        assertTrue(RunOverspeed.shouldStop(flagged(1_000L, 2_500L, 13))) // 30초
        assertEquals(1_000L, flagged(1_000L, 2_500L, 13)!!.since)
    }

    @Test fun `과속 구간 사이가 10초 넘게 벌어지면 처음부터 다시 센다`() {
        // 서 있는 러너에게 GPS 튐이 30초 떨어져 두 번 — 멈추지 않는다
        val twoGlitches = RunOverspeed.onFlagged(RunOverspeed.onFlagged(null, 0L), 30_000L)
        assertEquals(RunOverspeed.Streak(30_000L, 30_000L), twoGlitches)
        assertFalse(RunOverspeed.shouldStop(twoGlitches))
        // 10초까지는 이어진 것
        val within = RunOverspeed.onFlagged(RunOverspeed.onFlagged(null, 0L), 10_000L)
        assertEquals(RunOverspeed.Streak(0L, 10_000L), within)
        // 시계가 뒤로 갔으면 새로 센다
        assertEquals(RunOverspeed.Streak(5_000L, 5_000L), RunOverspeed.onFlagged(RunOverspeed.Streak(0L, 8_000L), 5_000L))
    }

    @Test fun `과속 구간이 오지 않는 시간만으로는 멈추지 않고, 10초 지나면 경고를 거둔다`() {
        val streak = RunOverspeed.onFlagged(null, 0L)
        assertFalse(RunOverspeed.shouldStop(streak))
        assertEquals(streak, RunOverspeed.live(streak, 10_000L))
        assertNull(RunOverspeed.live(streak, 10_001L))
        assertNull(RunOverspeed.live(null, 0L))
    }

    @Test fun `멈추기까지 남은 초를 올림으로 센다`() {
        val streak = RunOverspeed.Streak(since = 0L, lastAt = 0L)
        assertEquals(30, RunOverspeed.secondsLeft(streak, 0L))
        assertEquals(30, RunOverspeed.secondsLeft(streak, 1L))
        assertEquals(1, RunOverspeed.secondsLeft(streak, 29_500L))
        assertEquals(0, RunOverspeed.secondsLeft(streak, 31_000L))
    }

    @Test fun `러닝 금지는 10분, 남은 분은 올림`() {
        assertEquals(10 * 60_000L, RunOverspeed.BAN_MS)
        val until = 1_000L + RunOverspeed.BAN_MS
        assertEquals(10, RunOverspeed.banLeftMinutes(until, 1_000L))
        assertEquals(1, RunOverspeed.banLeftMinutes(until, until - 1L))
        assertEquals(0, RunOverspeed.banLeftMinutes(until, until))
        assertEquals(0, RunOverspeed.banLeftMinutes(0L, 1_000L))
    }

    @Test fun `서버에 알릴 과속 정지는 줄로 적고 읽는다`() {
        val rows = listOf(OverspeedStop(1L, 2L, "account:u1"), OverspeedStop(3L, 4L, "legacy"))
        assertEquals(rows, OverspeedStop.decodeAll(OverspeedStop.encodeAll(rows)))
        assertEquals(emptyList<OverspeedStop>(), OverspeedStop.decodeAll(null))
        assertEquals(emptyList<OverspeedStop>(), OverspeedStop.decodeAll("x\t2\taccount:u1\n5\t6"))
    }

    private class FakeOutbox(rows: List<OverspeedStop>) {
        val rows = rows.toMutableList()
        val sent = mutableListOf<Pair<Long, String>>()
    }

    private fun reporter(box: FakeOutbox, owner: String, answer: (Long) -> ServerResult<Unit>, reported: MutableList<Unit> = mutableListOf()) =
        OverspeedReporter(
            read = { box.rows.toList() },
            acknowledge = { started -> box.rows.removeAll { it.startedAt == started } },
            currentOwner = { owner },
            report = { started, _, userId -> box.sent += started to userId; answer(started) },
            onReported = { reported += Unit },
        )

    @Test fun `받으면 지우고, 끊기면 남겨 다시 시도한다`() = runBlocking {
        val box = FakeOutbox(listOf(OverspeedStop(1L, 2L, "account:u1"), OverspeedStop(3L, 4L, "account:u1")))
        val reported = mutableListOf<Unit>()
        val clear = reporter(box, "account:u1", { if (it == 1L) ServerResult.Ok(Unit) else ServerResult.Retry("끊김") }, reported).flush()
        assertFalse(clear)
        assertEquals(listOf(OverspeedStop(3L, 4L, "account:u1")), box.rows)
        assertEquals(listOf(1L to "u1", 3L to "u1"), box.sent)
        assertEquals(1, reported.size)
    }

    @Test fun `다른 계정의 신고는 남겨 두고 막지 않는다, 로그인 전 러닝 · 서버 거절은 지운다`() = runBlocking {
        val box = FakeOutbox(listOf(
            OverspeedStop(1L, 2L, "account:other"),
            OverspeedStop(3L, 4L, "legacy"),
            OverspeedStop(5L, 6L, "account:u1"),
        ))
        // legacy 계정으로 로그인 전 러닝 — 깎을 서버 신발이 없다
        assertTrue(reporter(box, "legacy", { ServerResult.Ok(Unit) }).flush())
        assertEquals(listOf(1L, 5L), box.rows.map { it.startedAt })
        assertTrue(box.sent.isEmpty())
        assertTrue(reporter(box, "account:u1", { ServerResult.Rejected("너무 오래된 러닝입니다") }).flush())
        assertEquals(listOf(1L), box.rows.map { it.startedAt })
        // 서버에 아직 함수가 없으면(서버 0065 전) 지우지 않는다
        box.rows += OverspeedStop(7L, 8L, "account:u1")
        assertTrue(reporter(box, "account:u1", {
            ServerResult.Rejected("Could not find the function public.report_overspeed_stop(p_started_at, p_stopped_at)")
        }).flush())
        assertEquals(listOf(1L, 7L), box.rows.map { it.startedAt })
        box.rows.removeAll { it.startedAt == 7L }
        // 다시 로그인해야 하면 남겨 두지만 러닝 올리기를 막지 않는다(올리기도 같은 까닭으로 멈춘다)
        assertTrue(reporter(box, "account:other", { ServerResult.SignInRequired("로그인") }).flush())
        assertEquals(listOf(1L), box.rows.map { it.startedAt })
    }
}
