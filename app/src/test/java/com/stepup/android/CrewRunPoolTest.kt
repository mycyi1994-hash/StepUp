package com.stepup.android

import com.stepup.android.data.remote.CrewRunResultRow
import com.stepup.android.data.remote.CrewRunRoomRow
import com.stepup.android.data.remote.CrewRunTodayRow
import com.stepup.android.data.remote.serverJson
import com.stepup.android.data.remote.CrewRunLiveRow
import com.stepup.android.domain.CrewBonusStatus
import com.stepup.android.domain.CrewEarning
import com.stepup.android.domain.CrewPoolSnap
import com.stepup.android.domain.CrewTerminal
import com.stepup.android.domain.CrewRunMath
import java.time.LocalDate
import kotlinx.serialization.decodeFromString
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * 크루 러닝(서버 0071) — 앱의 셈이 서버 함수(economy.crew_headcount_bonus · crew_level_stamina · crew_level_reward ·
 * crew_level_km)와 같은지, 서버 답(crew_run_today · crew_run_room · crew_run_beat · crew_run_result) 읽기, 공용 체력 보간 · 보상 나눔.
 */
class CrewRunPoolTest {

    @Test
    fun `인원 보너스는 서버 표와 같다`() {
        val expected = listOf(0.0, 0.05, 0.10, 0.15, 0.20, 0.50, 0.52, 0.54, 0.56, 0.58, 0.60, 0.62, 0.64, 0.66, 0.68, 0.70, 0.72, 0.74, 0.76, 1.50, 1.50)
        expected.forEachIndexed { i, bonus -> assertEquals("${i + 1}명", bonus, CrewRunMath.headcountBonus(i + 1), 1e-9) }
        assertEquals(0.0, CrewRunMath.headcountBonus(0), 0.0)
    }

    @Test
    fun `크루 레벨 1 ~ 20 과 보너스`() {
        assertEquals(1, CrewRunMath.levelFor(0.0))
        assertEquals(1, CrewRunMath.levelFor(19.999))
        assertEquals(2, CrewRunMath.levelFor(20.0))
        assertEquals(4, CrewRunMath.levelFor(100.0))
        assertEquals(19, CrewRunMath.levelFor(9999.999))
        assertEquals(20, CrewRunMath.levelFor(10000.0))
        assertEquals(20, CrewRunMath.levelFor(1_000_000.0))
        assertEquals(20, CrewRunMath.levelTable().size)
        assertEquals(0.0, CrewRunMath.levelStamina(null), 0.0)
        assertEquals(1.14, CrewRunMath.levelStamina(20), 1e-9)
        assertEquals(0.19, CrewRunMath.levelReward(20), 1e-9)
    }

    @Test
    fun `기여 · 보상 나눔 · 표시`() {
        // 핸드오프 예: 최대 내구도 100 · Lv.7 → +136
        assertEquals(136.0, CrewRunMath.contribution(100, 7), 1e-9)
        // 48 × (1 + 50% + 6%) = 74.88 — 인원 · 레벨은 더한다(곱하지 않는다)
        val split = CrewRunMath.split(48.0, 0.50, 0.06)
        assertEquals(24.0, split.headcount, 1e-9)
        assertEquals(2.88, split.level, 1e-9)
        assertEquals(74.88, split.total, 1e-9)
        assertEquals("16.6", CrewRunMath.amount(16.6666))
        assertEquals("0", CrewRunMath.amount(-3.0))
        assertEquals("15", CrewRunMath.percent(0.15))
        assertEquals("114", CrewRunMath.percent(1.14))
        assertEquals(0f, CrewRunMath.fraction(0.0, 0.0))
        assertTrue(CrewRunMath.low(326.4, 1632.0))
        assertFalse(CrewRunMath.low(0.0, 1632.0))
        // Lv7 · 520km → 450 ~ 650 구간의 35%
        assertEquals(0.35f, CrewRunMath.levelProgress(520.0, 7, 650.0, CrewRunMath.levelTable()), 1e-6f)
        assertEquals(1f, CrewRunMath.levelProgress(10_500.0, 20, null, CrewRunMath.levelTable()))
    }

    @Test
    fun `공용 체력 보간 — 지금 쓰는 사람 수만큼만 줄고 0 아래로 가지 않는다`() {
        val snap = CrewPoolSnap(LocalDate.of(2026, 10, 22), 816.0, 612.0, 204.0, 6, 3, false, false, 1, asOfMs = 1_000_000L)
        // 3명 × 60초 ÷ 12 = 15
        assertEquals(597.0, snap.remainingAt(1_060_000L), 1e-9)
        assertEquals(0.0, snap.remainingAt(1_000_000L + 10_000_000L), 0.0)
        assertEquals(612.0, snap.copy(activeRunners = 0).remainingAt(1_060_000L), 0.0)
        assertTrue(snap.copy(total = 0.0, remaining = 0.0).fresh)
        assertFalse(snap.copy(remaining = 0.0, exhausted = true).fresh)
        // 서버 시각으로 2분 확인까지 남은 초 — 받은 뒤 흐른 시간도 뺀다
        assertEquals(30L, CrewRunMath.checkLeftSec(160_000L, 120_000L, 5_000L, 15_000L))
        assertEquals(0L, CrewRunMath.checkLeftSec(160_000L, 200_000L, 5_000L, 5_000L))
        assertNull(CrewRunMath.checkLeftSec(null, 0L, 0L, 0L))
    }

    @Test
    fun `서버 답을 읽는다`() {
        val today = serverJson.decodeFromString<CrewRunTodayRow>(
            """{"server_now":"2026-10-22T03:00:00+00:00","day":"2026-10-22","resets_at":"2026-10-22T15:00:00+00:00",
               "pool":{"day":"2026-10-22","total":816,"remaining":612,"consumed":204,"contributors":6,"active_runners":3,
                       "exhausted":false,"low":false,"version":4,"as_of":"2026-10-22T03:00:00+00:00"},
               "mine":{"used_today":true,"state":"ENDED","crew_id":"c2","crew_name":"퇴근런","contribution":136,"consumed":106.7},
               "verified":false,"crew_level":7,"run_km":523.2,"next_level_km":650,"stamina_bonus":0.36,"reward_bonus":0.06,
               "level_event":{"level":7,"reached_at":"2026-10-21T12:00:00+00:00","run_km":451},
               "rules":{"room_max":20,"near_m":500,"confirm_sec":120,"drain_per_min":5,"requires_phone":true,
                        "headcount":[{"n":1,"bonus":0},{"n":2,"bonus":0.05}],"levels":[{"level":1,"km":0,"stamina":0,"reward":0}]}}""",
        ).toDomain("c1")!!
        assertEquals(612.0, today.pool.remaining, 0.0)
        assertEquals(6, today.pool.contributors)
        assertTrue(today.mine.usedToday)
        assertFalse(today.mine.active)
        // 다른 크루에서 했어도 하루 한 번
        assertTrue(today.usedElsewhere)
        assertFalse(today.verified)
        assertEquals(7, today.levelEvent!!.level)
        assertEquals(0.05, today.rules.headcountBonus(2), 0.0)
        // 날짜나 풀을 모르면 꾸미지 않는다
        assertNull(serverJson.decodeFromString<CrewRunTodayRow>("""{"day":""}""").toDomain("c1"))
        assertNull(serverJson.decodeFromString<CrewRunTodayRow>("""{"day":"2026-10-22"}""").toDomain("c1"))

        val room = serverJson.decodeFromString<CrewRunRoomRow>(
            """{"party_id":5,"crew_id":"c1","status":"LOBBY","capacity":20,"crew_level":7,"stamina_bonus":0.36,"reward_bonus":0.06,
               "members_count":6,"ready_count":6,"bonus_preview":{"count":5,"pct":0.20,"next_pct":0.50},
               "my_contribution":136,"my_verified":true,
               "members":[{"user_id":"u1","name":"레벨장","is_me":true,"is_host":true,"ready":true,"crew_member":true,
                           "ran_today":false,"verified":true,"sneaker_id":12,"rarity":"EPIC","shoe_level":1,"dur_max":100,"dur_now":75}]}""",
        ).toDomain()
        assertEquals(12L, room.me!!.sneakerId)
        assertEquals(5, room.bonusCount)
        assertEquals(136.0, room.myContribution, 0.0)
        assertNull(room.pool)

        val live = serverJson.decodeFromString<CrewRunLiveRow>(
            """{"crew_run":true,"server_now":"2026-10-22T03:02:15+00:00","party_id":5,"crew_id":"c1","day":"2026-10-22",
               "started_at":"2026-10-22T03:00:00+00:00","state":"ENDED","terminal_reason":"LAST_PARTICIPANT_END",
               "contribution":136,"consumed":11.25,"active_sec":135,"participants":6,"not_ended":0,"room_active_runners":0,
               "bonus":{"status":"CONFIRMED","check_at":"2026-10-22T03:02:15+00:00","count":5,"pct":0.2,"verified":true},
               "level":{"level":7,"pct":0.06},"earning_state":"ENDED"}""",
        ).toDomain()!!
        assertTrue(live.endedAlone)
        assertEquals(CrewBonusStatus.CONFIRMED, live.bonus.status)
        assertEquals(CrewEarning.ENDED, live.earning)
        assertEquals(0.06, live.levelPct, 0.0)
        assertNull(serverJson.decodeFromString<CrewRunLiveRow>("""{"crew_run":false}""").toDomain())

        val result = serverJson.decodeFromString<CrewRunResultRow>(
            """{"settled":true,"crew_id":"c1","crew_name":"퇴근런","verdict":"VALID","reason":"크루 체력을 다 썼습니다",
               "terminal_reason":"RECORDED","participants":6,
               "reward":{"base":48,"headcount_pct":0.5,"headcount":24,"level_pct":0.06,"level":2.88,"total":74.88},
               "bonus":{"status":"CONFIRMED","count":6,"pct":0.5,"verified":true},
               "pool_usage":{"contribution":136,"consumed":106.7,"active_sec":1280},
               "xp":{"distance_m":3200,"applied":true,"level_before":7,"level_after":8,"run_km":652.0,"next_level_km":900}}""",
        ).toDomain()
        assertTrue(result.settled)
        assertTrue(result.poolEmpty)
        assertTrue(result.levelUp)
        assertEquals(CrewTerminal.RECORDED, result.terminal)
        assertEquals(74.88, result.total!!, 0.0)
        assertFalse(result.invalid)
    }
}
