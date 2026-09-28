package com.stepup.android

import com.stepup.android.domain.Faction
import com.stepup.android.domain.Rarity
import com.stepup.android.domain.ServerStats
import com.stepup.android.domain.ShoeTier
import com.stepup.android.domain.Sneaker
import com.stepup.android.domain.tier
import com.stepup.android.ui.screens.customize.BarStat
import com.stepup.android.ui.screens.customize.StatScale
import com.stepup.android.ui.screens.customize.TierCount
import com.stepup.android.ui.screens.customize.activeFilter
import com.stepup.android.ui.screens.customize.barFraction
import com.stepup.android.ui.screens.customize.ownedRow
import com.stepup.android.ui.screens.customize.resolvePair
import com.stepup.android.ui.screens.customize.statBars
import com.stepup.android.ui.screens.customize.tierCounts
import com.stepup.android.ui.screens.customize.vaultShown
import com.stepup.android.ui.screens.customize.vaultSorted
import com.stepup.android.ui.screens.items.ItemSort
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

/**
 * 신발 화면 확정안(2026-09-28) — 내 신발 막대 · 보유 목록, 신발 보관함 정렬 · 거르기, 보이는 갈래 여섯의 계산.
 * 막대 끝은 서버가 정한 최대 · 상한(0022 economy)이고 모든 신발이 같은 기준을 쓴다 — 값을 100점으로 바꾸지 않는다.
 */
class MyShoesModelTest {

    // ── 보이는 갈래 ───────────────────────────────────────────────

    @Test
    fun `레드라인 · 피니시는 시리즈로 먼저 가르고 나머지는 실제 등급 그대로`() {
        assertEquals(ShoeTier.RARE, shoe(model = 1107, rarity = Rarity.RARE).tier)
        assertEquals(ShoeTier.EPIC, shoe(model = 1201, rarity = Rarity.EPIC).tier)
        assertEquals(ShoeTier.LEGENDARY, shoe(model = 1301, rarity = Rarity.LEGENDARY).tier)
        assertEquals(ShoeTier.REDLINE, shoe(model = 1311, rarity = Rarity.LEGENDARY).tier)
        assertEquals(ShoeTier.FINISH, shoe(model = 1329, rarity = Rarity.LEGENDARY).tier)
        // 예전 52종(모델 번호 없음)은 등급 그대로
        assertEquals(ShoeTier.COMMON, shoe(rarity = Rarity.COMMON).tier)
        assertEquals(ShoeTier.LEGENDARY, shoe(rarity = Rarity.LEGENDARY).tier)
        // 보이는 갈래는 서버 등급을 바꾸지 않는다
        assertEquals(Rarity.LEGENDARY, shoe(model = 1311, rarity = Rarity.LEGENDARY).tier.rarity)
    }

    // ── 막대 ─────────────────────────────────────────────────────

    @Test
    fun `막대 끝은 서버의 최대 · 상한 — 효율 27_5퍼센트 · 에너지 절감 20퍼센트 · 내구도 100`() {
        // 레전더리 최고 효율 1300 + 레벨당 50 × 29 = 2750bps
        assertEquals(27.5, StatScale.EFFICIENCY_MAX_PERCENT, 0.0)
        assertEquals((1300 + 50 * (StatScale.EFFICIENCY_MAX_LEVEL - 1)) / 100.0, StatScale.EFFICIENCY_MAX_PERCENT, 0.0)
        assertEquals(20.0, StatScale.ENERGY_MAX_PERCENT, 0.0)
        assertEquals(100, StatScale.DURABILITY_MAX)
    }

    @Test
    fun `막대 길이는 실제 값 나누기 막대 끝 — 4_8퍼센트를 48퍼센트 길이로 늘이지 않는다`() {
        val bars = statBars(server(efficiencyBps = 480, comfortBps = 750, durability = 92.6))
        assertEquals(listOf(BarStat.EFFICIENCY, BarStat.COMFORT, BarStat.DURABILITY), bars.map { it.stat })
        assertEquals(listOf("+4.8%", "7.5%", "92 / 100"), bars.map { it.value })
        assertEquals(4.8f / 27.5f, bars[0].fraction, 0.0001f)
        assertEquals(7.5f / 20f, bars[1].fraction, 0.0001f)
        assertEquals(0.92f, bars[2].fraction, 0.0001f)
    }

    @Test
    fun `막대는 0 에서 1 사이로 자른다`() {
        assertEquals(0f, barFraction(-3.0, 27.5), 0f)
        assertEquals(1f, barFraction(40.0, 27.5), 0f)
        assertEquals(0f, barFraction(5.0, 0.0), 0f)
        assertEquals(0f, barFraction(Double.NaN, 20.0), 0f)
        // 서버 상한을 넘는 값(있을 수 없지만)도 막대를 넘치지 않는다
        assertEquals(1f, statBars(server(efficiencyBps = 3_000, comfortBps = 2_500, durability = 100.0))[1].fraction, 0f)
    }

    @Test
    fun `예전 신발도 같은 기준 — 행운 줄을 만들지 않는다`() {
        val bars = statBars(shoe(rarity = Rarity.RARE, level = 3, comfort = 1.2, durability = 80))
        assertEquals(3, bars.size)
        assertEquals("80 / 100", bars[2].value)
        assertEquals(0.8f, bars[2].fraction, 0.0001f)
    }

    // ── 내 신발 보유 목록 ────────────────────────────────────────

    @Test
    fun `보유 목록은 켤레마다 한 칸 — 신고 있는 켤레 먼저, 그다음 최근에 받은 순`() {
        val worn = shoe(id = 1, acquired = 10, worn = true)
        val older = shoe(id = 2, acquired = 20)
        val newer = shoe(id = 3, acquired = 30)
        val sameModelA = shoe(id = 4, model = 1201, rarity = Rarity.EPIC, acquired = 25)
        val sameModelB = shoe(id = 5, model = 1201, rarity = Rarity.EPIC, acquired = 25)
        assertEquals(listOf(1L, 3L, 5L, 4L, 2L), ownedRow(listOf(older, sameModelA, worn, newer, sameModelB)).map { it.id })
    }

    @Test
    fun `보는 켤레는 소유 id — 없으면 신고 있는 켤레, 그것도 없으면 첫 켤레`() {
        val list = listOf(shoe(id = 1, acquired = 1), shoe(id = 2, acquired = 2, worn = true), shoe(id = 3, acquired = 3))
        assertEquals(3L, resolvePair(list, 3)?.id)
        assertEquals(2L, resolvePair(list, 99)?.id)
        assertEquals(2L, resolvePair(list, null)?.id)
        assertEquals(3L, resolvePair(list.map { it.copy(equipped = false) }, null)?.id)
        assertNull(resolvePair(emptyList(), null))
    }

    // ── 보관함 ───────────────────────────────────────────────────

    private val vault = listOf(
        shoe(id = 1, rarity = Rarity.COMMON, level = 4, acquired = 1, worn = true),
        shoe(id = 2, model = 1107, rarity = Rarity.RARE, level = 1, acquired = 5),
        shoe(id = 3, model = 1201, rarity = Rarity.EPIC, level = 2, acquired = 4),
        shoe(id = 4, model = 1329, rarity = Rarity.LEGENDARY, level = 1, acquired = 3),
        shoe(id = 5, model = 1311, rarity = Rarity.LEGENDARY, level = 6, acquired = 2),
        shoe(id = 6, model = 1301, rarity = Rarity.LEGENDARY, level = 1, acquired = 6),
    )

    @Test
    fun `정렬 — 최근 획득순 · 등급 높은순(레드라인 · 피니시를 더 높게 매기지 않는다) · 레벨 높은순`() {
        assertEquals(listOf(6L, 2L, 3L, 4L, 5L, 1L), vaultSorted(vault, ItemSort.RECENT).map { it.id })
        // 레전더리 셋(레전더리 · 레드라인 · 피니시는 갈래끼리 모을 뿐) → 에픽 → 레어 → 일반. 착용은 순서를 바꾸지 않는다
        assertEquals(listOf(6L, 5L, 4L, 3L, 2L, 1L), vaultSorted(vault, ItemSort.RARITY).map { it.id })
        assertEquals(listOf(5L, 1L, 3L, 6L, 4L, 2L), vaultSorted(vault, ItemSort.LEVEL).map { it.id })
    }

    @Test
    fun `거르기 칸은 전체와 가진 갈래만 — 수는 실제 켤레 수`() {
        assertEquals(
            listOf(
                TierCount(null, 6), TierCount(ShoeTier.COMMON, 1), TierCount(ShoeTier.RARE, 1), TierCount(ShoeTier.EPIC, 1),
                TierCount(ShoeTier.LEGENDARY, 1), TierCount(ShoeTier.REDLINE, 1), TierCount(ShoeTier.FINISH, 1),
            ),
            tierCounts(vault),
        )
        val noCommon = vault.drop(1) + shoe(id = 7, model = 1202, rarity = Rarity.EPIC, acquired = 9)
        assertEquals(listOf(null, ShoeTier.RARE, ShoeTier.EPIC, ShoeTier.LEGENDARY, ShoeTier.REDLINE, ShoeTier.FINISH),
            tierCounts(noCommon).map { it.tier })
        assertEquals(2, tierCounts(noCommon).first { it.tier == ShoeTier.EPIC }.count)
    }

    @Test
    fun `고른 갈래 신발이 없어지면 전체로 — 거른 결과는 그 갈래만`() {
        assertEquals(ShoeTier.FINISH, activeFilter(vault, "finish"))
        assertNull(activeFilter(vault.filter { it.tier != ShoeTier.FINISH }, "finish"))
        assertNull(activeFilter(vault, "bogus"))
        assertNull(activeFilter(vault, null))
        assertEquals(listOf(4L), vaultShown(vault, ShoeTier.FINISH, ItemSort.RECENT).map { it.id })
        assertEquals(6, vaultShown(vault, null, ItemSort.RECENT).size)
    }

    // ── 도우미 ───────────────────────────────────────────────────

    private fun shoe(
        id: Long = 1,
        model: Int? = null,
        rarity: Rarity = Rarity.COMMON,
        level: Int = 1,
        acquired: Long = 0,
        worn: Boolean = false,
        comfort: Double = 1.0,
        durability: Int = 100,
    ) = Sneaker(
        id = id, faction = Faction.WIND, rarity = rarity, variant = 0, level = level, mintNumber = id.toInt(),
        luck = 1.0, comfort = comfort, durability = durability, equipped = worn, acquiredAt = acquired, modelId = model,
    )

    private fun server(efficiencyBps: Int, comfortBps: Int, durability: Double) = shoe(model = 1107, rarity = Rarity.RARE).copy(
        server = ServerStats(
            origin = "DRAW", efficiencyBps = efficiencyBps, comfortBps = comfortBps, durabilityPts = durability,
            maxLevel = 15, status = "OWNED", chainState = "APP", canWithdraw = false, upgradeCost = 0.0,
            repairCostPerPoint = 0.0, genesisNo = 0,
        ),
    )
}
