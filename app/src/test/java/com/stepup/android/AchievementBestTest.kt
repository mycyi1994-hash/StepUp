package com.stepup.android

import com.stepup.android.domain.AchievementBook
import com.stepup.android.domain.AchievementCategory
import com.stepup.android.domain.AchievementMetrics
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * 업적은 한 번 열리면 다시 잠기지 않는다(2026-10-17 점검) — 지금 값과 그 계정이 닿은 가장 큰 값 중 큰 쪽으로 연다.
 * 스트릭이 끊기거나 크루를 나가거나 강화 재료로 신발을 태우거나, 연결 없이 0 을 읽어도 그대로다.
 */
class AchievementBestTest {

    private fun tiers(list: List<com.stepup.android.domain.Achievement>, category: AchievementCategory) =
        list.filter { it.category == category }

    @Test
    fun `지금 값이 줄어도 한 번 닿은 티어는 잠기지 않는다`() {
        // 스트릭이 끊겨 0, 크루를 나가 0 — 예전엔 열렸던 업적이 다시 잠겼다
        val now = AchievementMetrics(streak = 0, crews = 0)
        val best = mapOf("STREAK" to 7.0, "CREW" to 2.0)
        val book = AchievementBook.build(now, best)

        val streak = tiers(book, AchievementCategory.STREAK)
        assertEquals(listOf(true, true, true, true, false, false, false, false), streak.map { it.unlocked })
        // 다음 티어(14일)의 진행 막대도 뒤로 가지 않는다
        assertEquals(0.5f, streak[4].progress, 0.0001f)
        assertEquals(7.0, streak[4].current, 0.0)

        val crew = tiers(book, AchievementCategory.CREW)
        assertEquals(listOf(true, true, false, false), crew.map { it.unlocked })
    }

    @Test
    fun `지금 값이 더 크면 지금 값으로 연다`() {
        val book = AchievementBook.build(AchievementMetrics(sessions = 12), mapOf("SESSIONS" to 3.0))
        val sessions = tiers(book, AchievementCategory.SESSIONS)
        assertEquals(listOf(true, true, true, false), sessions.take(4).map { it.unlocked })
        assertEquals(12.0, sessions.first().current, 0.0)
    }

    @Test
    fun `최고값이 없으면 예전처럼 지금 값만 본다`() {
        val metrics = AchievementMetrics(steps = 6_000, posts = 1)
        assertEquals(AchievementBook.build(metrics), AchievementBook.build(metrics, emptyMap()))
        assertEquals(100, AchievementBook.build(metrics).size)
    }

    @Test
    fun `최고값은 믿을 수 있는 카테고리에서 더 클 때만 오른다`() {
        val metrics = AchievementMetrics(steps = 5_000, streak = 2, crews = 3, supEarned = 120.0)
        val best = mapOf("STEPS" to 9_000.0, "STREAK" to 1.0)
        // 크루 목록을 받지 못했고 경제 사본도 이 계정 것이 아니다
        val trusted = AchievementBook.trusted(economyMine = false, crewsLoaded = false, postsLoaded = false)

        val raised = AchievementBook.raised(metrics, best, trusted)
        // 걸음은 이미 더 큰 값이 있고, 크루 · SUP 는 믿을 수 없어 적지 않는다
        assertEquals(mapOf("STREAK" to 2.0), raised)
    }

    @Test
    fun `받지 못한 크루 · 글과 다른 계정의 경제 사본은 적지 않는다`() {
        val none = AchievementBook.trusted(economyMine = false, crewsLoaded = false, postsLoaded = false)
        assertTrue(AchievementCategory.STEPS in none)
        assertTrue(AchievementCategory.SESSIONS in none)
        assertTrue(AchievementCategory.STREAK in none)
        listOf(
            AchievementCategory.SUP, AchievementCategory.COLLECTION, AchievementCategory.LEVEL, AchievementCategory.UPGRADE,
            AchievementCategory.CREW, AchievementCategory.POST,
        ).forEach { assertFalse("$it", it in none) }

        val all = AchievementBook.trusted(economyMine = true, crewsLoaded = true, postsLoaded = true)
        assertEquals(AchievementCategory.entries.toSet(), all)
    }

    @Test
    fun `연결 없이 읽은 0 은 최고값을 내리지 않는다`() {
        val trusted = AchievementBook.trusted(economyMine = true, crewsLoaded = true, postsLoaded = true)
        val best = mapOf("CREW" to 2.0, "POST" to 5.0)
        assertTrue(AchievementBook.raised(AchievementMetrics(), best, trusted).isEmpty())
        assertEquals(best, AchievementBook.mergeBest(best, mapOf("CREW" to 0.0, "POST" to 1.0)))
    }

    @Test
    fun `합치면 카테고리마다 큰 쪽이다`() {
        val merged = AchievementBook.mergeBest(
            mapOf("STEPS" to 100.0, "CREW" to 2.0),
            mapOf("STEPS" to 250.0, "CREW" to 1.0, "LEVEL" to 4.0, "POST" to Double.NaN),
        )
        assertEquals(mapOf("STEPS" to 250.0, "CREW" to 2.0, "LEVEL" to 4.0), merged)
    }

    @Test
    fun `저장 글자를 되읽으면 같은 값이다 - 깨진 칸은 건너뛴다`() {
        val best = mapOf("STEPS" to 1_234_567.0, "DISTANCE" to 12.5, "STREAK" to 7.0)
        assertEquals(best, AchievementBook.decodeBest(AchievementBook.encodeBest(best)))

        assertEquals(emptyMap<String, Double>(), AchievementBook.decodeBest(null))
        assertEquals(emptyMap<String, Double>(), AchievementBook.decodeBest(""))
        assertEquals(mapOf("CREW" to 2.0), AchievementBook.decodeBest("STEPS=abc;=3;CREW=2.0;LEVEL;POST=-1"))
    }
}
