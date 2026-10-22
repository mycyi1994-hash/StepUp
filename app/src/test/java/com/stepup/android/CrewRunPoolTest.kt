package com.stepup.android

import com.stepup.android.data.remote.CrewRunResultRow
import com.stepup.android.data.remote.CrewRunRoomRow
import com.stepup.android.data.remote.CrewRunTodayRow
import com.stepup.android.data.remote.serverJson
import com.stepup.android.domain.CrewPoolStage
import com.stepup.android.domain.CrewRunMath
import com.stepup.android.domain.GeoPoint
import kotlinx.serialization.decodeFromString
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * 크루 러닝 3판(서버 0070) — 앱의 셈이 서버 함수(economy.crew_headcount_bonus · crew_level_stamina · crew_level_reward ·
 * crew_level_km)와 같은지, 서버 답(crew_run_today · crew_run_room · crew_run_result) 읽기, 달리는 중 몫 · 단계.
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
        assertEquals(1.11, CrewRunMath.multiplier(3, 2), 1e-9)
    }

    @Test
    fun `크루 체력 · 몫 · 달리는 중 단계`() {
        val pool = CrewRunMath.pool(150.0, 2)
        assertEquals(159.0, pool, 1e-9)
        assertEquals(636, CrewRunMath.shareSec(pool, 3))
        assertEquals(0, CrewRunMath.shareSec(pool, 0))
        assertEquals(CrewPoolStage.OK, CrewRunMath.stage(636, 300))
        assertEquals(CrewPoolStage.LOW, CrewRunMath.stage(636, 520))
        assertEquals(CrewPoolStage.EMPTY, CrewRunMath.stage(636, 636))
        assertEquals(1590, CrewRunMath.stepsWithinShare(3000, 636, 1200))
        assertEquals(1000, CrewRunMath.stepsWithinShare(1000, 636, 300))
        assertEquals(25.0, CrewRunMath.usedFor(300), 1e-9)
        assertEquals("16.6", CrewRunMath.amount(16.6666))
        assertEquals("15", CrewRunMath.percent(0.15))
        assertEquals("114", CrewRunMath.percent(1.14))
    }

    @Test
    fun `잠금화면 알림의 남은 크루 체력 시간`() {
        assertEquals("10:36", com.stepup.android.domain.RunNotificationText.clock(636))
        assertEquals("0:00", com.stepup.android.domain.RunNotificationText.clock(-5))
        assertEquals("1:02:03", com.stepup.android.domain.RunNotificationText.clock(3723))
        val state = com.stepup.android.service.WalkSessionState(isActive = true, elapsedSec = 600, crewShareSec = 636)
        assertEquals(36L, state.crewPoolLeftSec)
        assertNull(com.stepup.android.service.WalkSessionState(isActive = true, elapsedSec = 600).crewPoolLeftSec)
        assertEquals(0L, state.copy(elapsedSec = 700).crewPoolLeftSec)
    }

    @Test
    fun `혼자 남음은 위치를 보이는 크루원이 모두 곁 밖일 때만`() {
        val me = GeoPoint(37.5, 127.0)
        assertFalse(CrewRunMath.aloneNow(me, emptyList(), 500))
        assertFalse(CrewRunMath.aloneNow(me, listOf(GeoPoint(37.5036, 127.0)), 500))
        assertTrue(CrewRunMath.aloneNow(me, listOf(GeoPoint(37.51, 127.0)), 500))
        assertFalse(CrewRunMath.aloneNow(null, listOf(GeoPoint(37.51, 127.0)), 500))
    }

    @Test
    fun `서버 답을 읽는다`() {
        val today = serverJson.decodeFromString<CrewRunTodayRow>(
            """{"day":"2026-10-21","resets_at":"2026-10-21T15:00:00+00:00","ran_today":true,
               "mine":{"day":"2026-10-21","crew_id":"c1","session_id":7,"runners":3,"crew_level":2,"pool":159.00,"share":53.00,
                       "limit_sec":636,"used":25.00,"together_sec":300,"headcount_bonus":0.10,"level_bonus":0.01,"sneaker_id":9},
               "crew_level":3,"run_km":55.5,"next_level_km":100,"stamina_bonus":0.12,"reward_bonus":0.02,
               "today":{"runners":1,"pool":53,"used":25},
               "rules":{"room_max":20,"near_m":500,"confirm_sec":120,"alone_grace_sec":60,"drain_per_min":5,
                        "headcount":[{"n":1,"bonus":0},{"n":2,"bonus":0.05}],"levels":[{"level":1,"km":0,"stamina":0,"reward":0}]}}""",
        ).toDomain("c1")!!
        assertTrue(today.ranToday)
        assertEquals(53.0, today.mine!!.share, 0.0)
        assertEquals(3, today.crewLevel)
        assertEquals(0.05, today.rules.headcountBonus(2), 0.0)
        assertNull(serverJson.decodeFromString<CrewRunTodayRow>("""{"day":""}""").toDomain("c1"))

        val room = serverJson.decodeFromString<CrewRunRoomRow>(
            """{"party_id":5,"crew_id":"c1","status":"LOBBY","crew_level":2,"stamina_bonus":0.06,"reward_bonus":0.01,
               "runners":4,"pool":477,"share":119.25,"share_sec":1431,"headcount_bonus":0.15,
               "members":[{"user_id":"u1","name":"레벨장","is_me":true,"is_host":true,"crew_member":true,"ran_today":false,
                           "sneaker_id":12,"rarity":"EPIC","shoe_level":1,"dur_max":90}]}""",
        ).toDomain()
        assertEquals(12L, room.me!!.sneakerId)
        assertEquals(1.16, room.multiplier, 1e-9)

        val result = serverJson.decodeFromString<CrewRunResultRow>(
            """{"day":"2026-10-21","runners":3,"crew_level":2,"pool":159,"share":53,"limit_sec":636,"used":53,"together_sec":1200,
               "headcount_bonus":0.1,"level_bonus":0.01,"points":17.649,"reason":"크루 체력을 다 썼습니다","crew_km":2.664,
               "crew_name":"크루","crew_level_now":3,"crew_run_km":55.5,"next_level_km":100}""",
        ).toDomain()!!
        assertTrue(result.poolEmpty)
        assertFalse(result.aloneEnded)
        assertTrue(result.levelUp)
    }
}
