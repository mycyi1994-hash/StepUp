package com.stepup.android

import com.stepup.android.data.remote.CrewBoardRow
import com.stepup.android.data.remote.RunnerBoardRow
import com.stepup.android.data.remote.serverJson
import com.stepup.android.data.repo.toBoard
import com.stepup.android.domain.LeaderboardMath
import com.stepup.android.ui.screens.leaderboard.LeaderboardMotion
import java.time.LocalDate
import java.time.ZoneId
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/** 리더보드(2026-10-17) — 화면이 더 계산하는 값 · 서버 응답 읽기 · 배경 움직임 수치 */
class LeaderboardTest {

    // ── 화면이 더 계산하는 값 ──

    @Test fun `지난주 대비 — 오르면 양수, 내려가면 음수, 같으면 0`() {
        assertEquals(12, LeaderboardMath.rankRise(127, 139))
        assertEquals(-3, LeaderboardMath.rankRise(15, 12))
        assertEquals(0, LeaderboardMath.rankRise(4, 4))
    }

    @Test fun `지난주 순위가 없으면 변화를 만들지 않는다`() {
        assertNull(LeaderboardMath.rankRise(127, null))
        assertNull(LeaderboardMath.rankRise(null, 3))
        // 0 · 음수 순위는 서버가 주지 않는다 — 와도 계산하지 않는다
        assertNull(LeaderboardMath.rankRise(0, 3))
    }

    @Test fun `바로 앞과의 차이 — 같은 집계의 두 값으로 빼고, 1위는 차이가 없다`() {
        assertEquals(0.8, LeaderboardMath.gapToNext(32.6, 33.4)!!, 1e-9)
        assertEquals(0.0, LeaderboardMath.gapToNext(18.4, 18.4)!!, 0.0)
        assertNull(LeaderboardMath.gapToNext(128.4, null))
    }

    @Test fun `작은 차이를 0 으로 보이지 않는다 — 올려서 한 자리`() {
        assertEquals("0.1", LeaderboardMath.gapKm(0.04))
        assertEquals("0.8", LeaderboardMath.gapKm(0.8))
        assertEquals("0.9", LeaderboardMath.gapKm(0.81))
        assertEquals("0.0", LeaderboardMath.gapKm(0.0))
    }

    @Test fun `지난주 적립이 없으면 증감을 말하지 않는다(0 으로 치지 않는다)`() {
        assertEquals(310.0, LeaderboardMath.delta(2680.0, 2370.0)!!, 1e-9)
        assertNull(LeaderboardMath.delta(2680.0, null))
    }

    @Test fun `표시 — km 한 자리, 포인트는 천 단위 쉼표 정수`() {
        assertEquals("32.6", LeaderboardMath.km(32.6))
        assertEquals("1,700.0".replace(",", ""), LeaderboardMath.km(1700.0))
        assertEquals("2,680", LeaderboardMath.points(2680.0))
        assertEquals("9,840", LeaderboardMath.points(9840.4))
    }

    @Test fun `사람 머리글자 — 한글 이름은 첫 글자, 로마자는 서버 머리글자`() {
        assertEquals("가", LeaderboardMath.personMark("가나", "가나"))
        assertEquals("AK", LeaderboardMath.personMark("Ara Kim", "AK"))
        assertEquals("J", LeaderboardMath.personMark("jay", null))
    }

    // ── 서버 응답(0061)을 화면 모양으로 ──

    @Test fun `개인 순위 — 기간 · 내 줄 · 바로 앞 · 이번 주 러닝을 서버 값 그대로 읽는다`() {
        val json = """
            {"week_start":"2026-10-12","week_end":"2026-10-19","timezone":"Asia/Seoul","as_of":"2026-10-16T09:00:00.123+00:00",
             "total":3,
             "me":{"user_id":"u3","name":"다온","monogram":"다온","rank":3,"previous_rank":1,"meters":800000,"runs":1,"is_me":true},
             "ahead":{"user_id":"u2","name":"나래","monogram":"나래","rank":2,"previous_rank":null,"meters":800000,"runs":1,"is_me":false},
             "nearby":[{"user_id":"u1","name":"가람","monogram":"가람","rank":1,"previous_rank":3,"meters":900000,"runs":1,"is_me":false}],
             "top":[], "my_name":"다온", "my_meters":800000, "my_runs":1,
             "my_run_list":[{"started_at":"2026-10-14T21:30:00+00:00","meters":800000}]}
        """.trimIndent()
        val board = serverJson.decodeFromString<RunnerBoardRow>(json).toBoard()
        assertEquals(LocalDate.of(2026, 10, 12), board.period.startDate)
        assertEquals(LocalDate.of(2026, 10, 18), board.period.lastDate)
        assertEquals(3, board.me!!.rank)
        assertEquals(800.0, board.me!!.metric, 1e-9)
        assertEquals("다", board.me!!.mark)
        assertNull(board.ahead!!.previousRank)
        // 같은 거리 — 차이 0(동점)
        assertEquals(0.0, LeaderboardMath.gapToNext(board.me!!.metric, board.ahead!!.metric)!!, 0.0)
        assertEquals("다온", board.myName)
        // 화요일 21:30 UTC 는 한국 시간 수요일 06:30 — 시트의 날짜는 그 주의 시간대로
        val day = board.myRunList.single().startedAt.atZone(ZoneId.of(board.period.timezone)).toLocalDate()
        assertEquals(LocalDate.of(2026, 10, 15), day)
    }

    @Test fun `순위가 없는 나 — 내 줄 · 바로 앞이 없고 이번 주 합은 0`() {
        val json = """{"week_start":"2026-10-12","week_end":"2026-10-19","total":0,"me":null,"ahead":null,"nearby":[],"top":[],
            "my_name":"라온","my_meters":0,"my_runs":0,"my_run_list":[]}"""
        val board = serverJson.decodeFromString<RunnerBoardRow>(json).toBoard()
        assertNull(board.me)
        assertNull(board.ahead)
        assertEquals(0.0, board.myKm, 0.0)
        assertTrue(board.top.isEmpty())
    }

    @Test fun `크루 순위 — 우리 크루가 이번 주 기록이 없으면 순위 없이, 사진이 있으면 그 판`() {
        val json = """
            {"week_start":"2026-10-12","week_end":"2026-10-19","timezone":"Asia/Seoul","total":1,
             "top":[{"crew_id":"a","name":"주간A","monogram":"주","image_bg":2,"image_ver":5,"has_image":true,"rank":1,
                     "previous_rank":2,"meters":1700000,"runners":2,"is_mine":false}],
             "my_crew":{"crew_id":"c","name":"주간C","monogram":"주","image_bg":0,"image_ver":0,"has_image":false,"rank":null,
                        "previous_rank":null,"meters":0,"runners":0,"is_mine":true},
             "nearby":[]}
        """.trimIndent()
        val board = serverJson.decodeFromString<CrewBoardRow>(json).toBoard()
        val first = board.top.single()
        assertEquals(1700.0, first.metric, 1e-9)
        assertEquals(2, first.participants)
        assertEquals(5, first.photoVersion)
        assertEquals(1, LeaderboardMath.rankRise(first.rank, first.previousRank))
        assertNull(board.mine!!.rank)
        assertNull(board.mine!!.photoVersion)
        assertTrue(board.mine!!.isMe)
    }

    // ── 배경 움직임(motion-spec.json) ──

    @Test fun `트랙 빛 — 18초에 한 바퀴, 줄마다 -1 · -7 · -13초 위상`() {
        assertEquals(20f, LeaderboardMotion.lightDegrees(0, 0), 1e-3f)
        assertEquals(140f, LeaderboardMotion.lightDegrees(0, 1), 1e-3f)
        assertEquals(260f, LeaderboardMotion.lightDegrees(0, 2), 1e-3f)
        assertEquals(LeaderboardMotion.lightDegrees(1_000, 0), LeaderboardMotion.lightDegrees(19_000, 0), 1e-3f)
    }

    @Test fun `부드러운 빛 — 16초 왕복, 끝에서 투명도 1 · (-12, 10), 모션 감소면 0·7 고정`() {
        // 위상 -5초 → 시계 11초에 한쪽 끝(앞으로 16초 지점)
        val (alpha, dx, dy) = LeaderboardMotion.glow(11_000, reduced = false)
        assertEquals(1f, alpha, 1e-3f)
        assertEquals(-12f, dx, 1e-3f)
        assertEquals(10f, dy, 1e-3f)
        // 다시 16초 뒤에는 처음 자리(투명도 .58)
        assertEquals(0.58f, LeaderboardMotion.glow(27_000, reduced = false).first, 1e-3f)
        assertEquals(0.7f, LeaderboardMotion.glow(5_000, reduced = true).first, 0f)
    }

    @Test fun `1위 빛 띠 — 10초 주기, 10 · 50 · 90% 에서 투명도 0·2 · 0·65 · 0·2`() {
        // 위상 -3초 → 시계 t 에서 진행은 (t + 3초) / 10초
        assertEquals(0.2f, LeaderboardMotion.sheen(-2_000).second, 1e-3f)
        assertEquals(0.65f, LeaderboardMotion.sheen(2_000).second, 1e-3f)
        assertEquals(0.2f, LeaderboardMotion.sheen(6_000).second, 1e-3f)
        assertEquals(0f, LeaderboardMotion.sheen(-3_000).second, 1e-3f)
    }
}
