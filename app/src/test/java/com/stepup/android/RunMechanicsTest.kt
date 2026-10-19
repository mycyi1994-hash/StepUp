package com.stepup.android

import com.stepup.android.domain.Faction
import com.stepup.android.domain.GeoPoint
import com.stepup.android.domain.Rarity
import com.stepup.android.domain.RewardEconomy
import com.stepup.android.domain.RunDistance
import com.stepup.android.domain.RunOverspeed
import com.stepup.android.domain.ServerStats
import com.stepup.android.domain.Sneaker
import com.stepup.android.domain.TrackPoint
import com.stepup.android.domain.liveHead
import com.stepup.android.service.WalkSessionState
import com.stepup.android.service.withAutoLaps
import com.stepup.android.ui.screens.items.durabilityEarnMinutes
import com.stepup.android.ui.screens.items.durabilityWear
import com.stepup.android.ui.screens.walk.currentSpeedKmh
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * 러닝 메카닉 점검(2026-10-17~19) — 거리 하나의 기준 · 랩 · 움직인 시간만 닳는 내구도 · 드문 점에서의 속도 · 흐린 위치의 내 점 ·
 * 위치 튐으로 걸리는 과속 멈춤 · 출발 전 남은 보상 시간.
 */
class RunMechanicsTest {

    @Test
    fun `거리는 경로가 100m 를 넘으면 GPS, 그 전엔 걸음 거리 - 서버와 같은 규칙`() {
        // 걸음 1,000(762m) · GPS 50m(실내 떨림) — 걸음 거리
        assertEquals(0.762, RunDistance.liveKm(0.05, 1_000), 1e-9)
        assertEquals(762.0, RunDistance.creditedMeters(0.05, 1_000), 1e-9)
        // GPS 5km · 걸음 6,000 — GPS 거리
        assertEquals(5.0, RunDistance.liveKm(5.0, 6_000), 1e-9)
        assertEquals(5_000.0, RunDistance.creditedMeters(5.0, 6_000), 1e-9)
        // 걸음 없이 탈것으로 간 거리는 걸음 거리의 3배 + 100m 까지만 친다(record_session 의 v_distance_m)
        assertEquals(100 * 0.762 * 3 + 100, RunDistance.creditedMeters(3.0, 100), 1e-9)
        assertEquals(0.0, RunDistance.creditedMeters(3.0, 0), 1e-9)
    }

    @Test
    fun `GPS 가 잡히기 시작해도 100m 전에는 걸음 거리 그대로 - 거리가 거꾸로 가지 않는다`() {
        // 러닝머신 2.5km(걸음 3,281) 에서 와이파이 떨림 40m 가 GPS 로 세어져도 2.50km 그대로
        assertEquals(2.5, RunDistance.liveKm(0.04, 3_281), 0.001)
    }

    @Test
    fun `1km 랩은 화면 거리와 같은 기준으로 찍힌다`() {
        // 걸음으로 1.3km(1,706걸음) 를 갔지만 GPS 로는 0.9km — GPS 기준이라 아직 랩이 없다
        val walking = WalkSessionState(isActive = true, steps = 1_706, gpsKm = 0.9, elapsedSec = 600).withAutoLaps()
        assertEquals(0, walking.laps.size)
        // GPS 2.05km — 랩 둘(1km · 2km), 찍은 때의 달린 시간
        val running = walking.copy(gpsKm = 2.05, elapsedSec = 720).withAutoLaps()
        assertEquals(listOf(1.0, 2.0), running.laps.map { it.km })
        assertEquals(listOf(720L, 720L), running.laps.map { it.splitSec })
        // 다시 불러도 늘지 않는다
        assertEquals(running.laps, running.withAutoLaps().laps)
    }

    @Test
    fun `내구도는 움직인 시간만 1분마다 5 - 켜 둔 채 서 있으면 닳지 않는다`() {
        // 30분 · 5km — 그대로 150
        assertEquals(150, durabilityWear(1_800, 5_000.0))
        // 30분 · 0.6km(카페에 앉아 있었다) — 0.6km ÷ 0.5m/s = 20분 → 100
        assertEquals(100, durabilityWear(1_800, 600.0))
        // 8시간 켜 둔 채 주머니에 · 거리 0 — 닳지 않는다
        assertEquals(0, durabilityWear(8 * 3_600, 0.0))
        // 거리를 모르면(예전 부름) 시간 그대로
        assertEquals(150, durabilityWear(1_800))
    }

    @Test
    fun `점이 드문 동안에도 걸음이 늘면 지금 속도를 0 으로 떨어뜨리지 않는다`() {
        val start = 1_000_000L
        // 36초마다 40m — 시속 4km
        val track = listOf(
            TrackPoint(37.5, 127.0, start),
            TrackPoint(37.5 + 40 / 111_195.0, 127.0, start + 36_000),
        )
        val now = track.last().at + 25_000
        // 걸음도 25초째 그대로면 멈춰 선 것
        assertEquals(0.0, currentSpeedKmh(track, now, 0L, movingAt = track.last().at)!!, 0.0)
        // 걸음이 방금 늘었으면 마지막 속도(약 4km/h)
        assertEquals(4.0, currentSpeedKmh(track, now, 0L, movingAt = now - 2_000)!!, 0.1)
        // 1분 넘게 새 점이 없으면 걷고 있어도 모른다(신호 약함)
        assertNull(currentSpeedKmh(track, track.last().at + 61_000, 0L, movingAt = track.last().at + 60_000))
    }

    @Test
    fun `흐린 위치로는 내 점을 경로 끝에서 멀리 옮기지 않는다`() {
        val end = TrackPoint(37.5284, 126.9326, 1_005_000L)
        val far = GeoPoint(37.5300, 126.9326) // 약 180m
        val later = end.at + 25_000
        assertEquals(far, liveHead(listOf(end), far, later, hereAccurate = true))
        assertNull(liveHead(listOf(end), far, later, hereAccurate = false))
        // 가까운 흐린 위치는 그대로 따라간다
        val near = GeoPoint(37.52848, 126.9326)
        assertEquals(near, liveHead(listOf(end), near, later, hereAccurate = false))
    }

    @Test
    fun `흐린 위치의 튐은 과속으로 세지 않는다`() {
        assertTrue(RunOverspeed.counts(8f, 12f))
        // 와이파이 위치로 튀었다 · 와이파이 기준점에서 GPS 로 · 오차를 모른다
        assertFalse(RunOverspeed.counts(35f, 8f))
        assertFalse(RunOverspeed.counts(8f, 35f))
        assertFalse(RunOverspeed.counts(null, 8f))
    }

    @Test
    fun `두 자리를 오간 튐은 30초가 지나도 멈추지 않고 실제로 멀어져야 멈춘다`() {
        val a = GeoPoint(37.5284, 126.9326)
        val b = GeoPoint(37.5284 + 60 / 111_195.0, 126.9326) // 60m 북쪽
        // 실내 — 2초마다 A ↔ B(오차 15m 와이파이가 오간다) 40초
        var noise: RunOverspeed.Streak? = null
        for (i in 0..20) {
            val (from, to) = if (i % 2 == 0) a to b else b to a
            noise = RunOverspeed.onFlagged(noise, 1_000L + i * 2_000L, from, to)
            assertFalse(RunOverspeed.shouldStop(noise))
        }
        // 30초째에 처음부터 다시 셌다(경고만 남는다)
        assertTrue(noise!!.since > 1_000L)
        // 차 — 2초마다 북쪽으로 20m(시속 36km). 30초 · 320m 면 멈춘다
        var car: RunOverspeed.Streak? = null
        for (i in 0..15) {
            val from = GeoPoint(a.lat + i * 20 / 111_195.0, a.lng)
            val to = GeoPoint(a.lat + (i + 1) * 20 / 111_195.0, a.lng)
            car = RunOverspeed.onFlagged(car, 1_000L + i * 2_000L, from, to)
        }
        assertTrue(RunOverspeed.shouldStop(car))
        assertEquals(1_000L, car!!.since)
    }

    @Test
    fun `출발 전 점검의 남은 보상 시간 - 내구도 1 은 12초`() {
        fun shoe(now: Int, max: Int) = Sneaker(
            id = 1, faction = Faction.WIND, rarity = Rarity.COMMON, variant = 0, level = 1, mintNumber = 1,
            luck = 1.0, comfort = 1.0, durability = 100, equipped = true, acquiredAt = 0L, modelId = 1107,
        ).copy(
            server = ServerStats(
                origin = "DRAW", efficiencyBps = 0, comfortBps = 0, durabilityPts = if (max > 0) now * 100.0 / max else 100.0,
                maxLevel = 20, status = "OWNED", chainState = "APP", canWithdraw = false, upgradeCost = 0.0,
                repairCostPerPoint = 0.0, genesisNo = 0, durabilityNow = now, durabilityMax = max, durabilityBase = max,
            ),
        )
        // 새 일반 신발 50 — 10분
        assertEquals(10, durabilityEarnMinutes(shoe(50, 50)))
        // 22 — 4분 24초 → 4분
        assertEquals(4, durabilityEarnMinutes(shoe(22, 50)))
        assertEquals(0, durabilityEarnMinutes(shoe(0, 50)))
        // 최대 내구도를 모르는 신발 — 모른다
        assertNull(durabilityEarnMinutes(shoe(0, 0)))
    }

    @Test
    fun `닳는 시간과 다 닳은 뒤 걸음 빼기는 같은 움직인 시간으로 잰다`() {
        // 60분 켜 둠 · 움직인 거리 600m — 닳는 시간은 600m ÷ 0.5m/s = 20분
        assertEquals(1_200L, RewardEconomy.wearSeconds(3_600, 600.0))
        // 30분짜리 신발(150) — 움직인 20분으로는 다 닳지 않아 걸음을 빼지 않는다(서버 0068)
        assertEquals(2_000, RewardEconomy.stepsWithinDurability(2_000, 150, RewardEconomy.wearSeconds(3_600, 600.0)))
        // 켜 둔 시간(60분)으로 재면 절반을 잃었다(0066)
        assertEquals(1_000, RewardEconomy.stepsWithinDurability(2_000, 150, 3_600))
        // 거리를 모르면 시간 그대로
        assertEquals(3_600L, RewardEconomy.wearSeconds(3_600, Double.MAX_VALUE))
    }
}
