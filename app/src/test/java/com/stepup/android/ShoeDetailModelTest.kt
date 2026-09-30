package com.stepup.android

import com.stepup.android.data.repo.EconomyOutcome
import com.stepup.android.domain.Faction
import com.stepup.android.domain.Rarity
import com.stepup.android.domain.ServerStats
import com.stepup.android.domain.Sneaker
import com.stepup.android.ui.components.sneakerArtRes
import com.stepup.android.ui.screens.items.ChainMark
import com.stepup.android.ui.screens.items.EquipFailure
import com.stepup.android.ui.screens.items.EquipResult
import com.stepup.android.ui.screens.items.OwnedLoad
import com.stepup.android.ui.screens.items.ShoeDetailState
import com.stepup.android.ui.screens.items.ShoeStat
import com.stepup.android.ui.screens.items.bonusPercent
import com.stepup.android.ui.screens.items.chainMarkOf
import com.stepup.android.ui.screens.items.classifyEquip
import com.stepup.android.ui.screens.items.DetailStat
import com.stepup.android.ui.screens.items.detailStatRows
import com.stepup.android.ui.screens.items.detailStateOf
import com.stepup.android.ui.screens.items.formatShoeNumber
import com.stepup.android.ui.screens.items.energySavingPercent
import com.stepup.android.ui.screens.items.formatBonus
import com.stepup.android.ui.screens.items.formatDurability
import com.stepup.android.ui.screens.items.formatPercent
import com.stepup.android.ui.screens.items.ownedGroups
import com.stepup.android.ui.screens.items.pickInGroup
import com.stepup.android.ui.screens.items.representativeOf
import com.stepup.android.ui.screens.items.resolveSelection
import com.stepup.android.ui.screens.items.shoeStats
import com.stepup.android.ui.screens.items.withObjectParticle
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * 보유 신발 상세 v1(docs/redesign/shoe-detail-v1) — 화면이 쓰는 순수 계산.
 * 고르기는 소유 id 로만(착용과 따로), 수치 형식은 "+0.45%" · "7.5%", 에너지 절감은 기존 계산 그대로,
 * 착용 결말은 저장된 실제 착용으로만 "신었어요", 목록 상태는 읽는 중 · 실패 · 없음 · 있음을 가른다.
 */
class ShoeDetailModelTest {

    // ── 수치 형식 ──────────────────────────────────────────────

    @Test
    fun `보너스는 늘 +와 % — 배율이나 SUP 수량으로 쓰지 않는다`() {
        assertEquals("+0.45%", formatBonus(0.45))
        assertEquals("+0.35%", formatBonus(0.35))
        assertEquals("+2.55%", formatBonus(2.55))
        assertEquals("+1.5%", formatBonus(1.5))
        assertEquals("+0.0%", formatBonus(0.0))
        // 부동소수 끝자리 — 0.45 가 0.4499999… 여도 0.45
        assertEquals("+0.45%", formatBonus(0.1 + 0.35))
    }

    @Test
    fun `에너지 절감은 소수 한두 자리 %`() {
        assertEquals("7.5%", formatPercent(7.5))
        assertEquals("6.0%", formatPercent(6.0))
        assertEquals("4.5%", formatPercent(4.5))
        assertEquals("15.0%", formatPercent(15.0))
        assertEquals("0.0%", formatPercent(-0.0))
    }

    @Test
    fun `예전 신발 에너지 절감 — 기존 계산(착화감 기준, 최대 15%) 그대로`() {
        // 전달본 예시 데이터가 이 공식에 맞춰져 있다(착화감 1.20 → 7.5%, 1.16 → 6.0%, 1.12 → 4.5%)
        assertEquals("7.5%", formatPercent(energySavingPercent(legacy(comfort = 1.20))))
        assertEquals("6.0%", formatPercent(energySavingPercent(legacy(comfort = 1.16))))
        assertEquals("4.5%", formatPercent(energySavingPercent(legacy(comfort = 1.12))))
        assertEquals(15.0, energySavingPercent(legacy(comfort = 1.60)), 1e-9)
        val shoe = legacy(comfort = 1.20)
        assertEquals((1.0 - shoe.energyEfficiency) * 100.0, energySavingPercent(shoe), 1e-12)
    }

    @Test
    fun `서버 신발 — 에너지 절감은 착화감(bps), 보너스는 효율(bps)`() {
        val shoe = server(efficiencyBps = 45, comfortBps = 750)
        assertEquals("7.5%", formatPercent(energySavingPercent(shoe)))
        assertEquals((1.0 - shoe.energyEfficiency) * 100.0, energySavingPercent(shoe), 1e-12)
        assertEquals("+0.45%", formatBonus(bonusPercent(shoe)))
        assertEquals("+1.5%", formatBonus(bonusPercent(server(efficiencyBps = 150))))
    }

    @Test
    fun `예전 신발 보너스 — 도감 값 + 레벨 한 칸에 0점5(Sneaker boostPercent)`() {
        // 바람 일반 0 — WND-010 클라우드 러너 0.40
        assertEquals("+0.4%", formatBonus(bonusPercent(legacy(level = 1))))
        assertEquals("+1.4%", formatBonus(bonusPercent(legacy(level = 3))))
        assertEquals(legacy(level = 3).boostPercent, bonusPercent(legacy(level = 3)), 1e-12)
    }

    @Test
    fun `능력치 시트 — 있는 값만, 서버 신발은 자리 값(행운 1점00)을 보이지 않는다`() {
        val old = shoeStats(legacy(comfort = 1.20, luck = 1.08))
        assertEquals(listOf(ShoeStat.BONUS, ShoeStat.ENERGY, ShoeStat.LUCK, ShoeStat.COMFORT, ShoeStat.DURABILITY), old.map { it.stat })
        assertEquals(listOf("+0.4%", "7.5%", "1.08", "1.20", "100 / 100"), old.map { it.text })
        val fromServer = shoeStats(server(efficiencyBps = 45, comfortBps = 750, durability = 87.0))
        assertEquals(listOf(ShoeStat.BONUS, ShoeStat.ENERGY, ShoeStat.DURABILITY), fromServer.map { it.stat })
        assertEquals(listOf("+0.45%", "7.5%", "87 / 100"), fromServer.map { it.text })
    }

    @Test
    fun `내구도는 100 기준, 서버 소수는 내려서 — 수리할 곳을 가리지 않는다`() {
        assertEquals("100 / 100", formatDurability(legacy()))
        assertEquals("99 / 100", formatDurability(server(durability = 99.6)))
        assertEquals("0 / 100", formatDurability(server(durability = 0.4)))
    }

    // ── 체인 ───────────────────────────────────────────────────

    @Test
    fun `체인 줄 — 토큰이 없으면 아직 체인에 없음, v3 금고는 1,000,001 부터`() {
        assertEquals(ChainMark.None, chainMarkOf(null))
        assertEquals(ChainMark.None, chainMarkOf(0))
        assertEquals(ChainMark.Token(42), chainMarkOf(42))
        assertEquals(ChainMark.Token(1_000_000), chainMarkOf(1_000_000))
        assertEquals(ChainMark.Vault(1_000_001), chainMarkOf(1_000_001))
    }

    // ── 보유 목록 · 고르기 ─────────────────────────────────────

    /** 전달본의 모양(세 모델 · 네 켤레, 한 모델은 두 켤레) — 이름 · 번호는 예시가 아니라 여기서 만든 값 */
    private val city = legacy(id = 11, faction = Faction.WIND, variant = 0, mint = 1, equipped = true, acquired = 1_000)
    private val river2 = legacy(id = 12, faction = Faction.WATER, variant = 1, mint = 2, acquired = 2_000)
    private val night = legacy(id = 13, faction = Faction.FIRE, variant = 2, mint = 3, acquired = 2_500)
    private val river4 = legacy(id = 14, faction = Faction.WATER, variant = 1, mint = 4, level = 2, acquired = 3_000)
    private val owned = listOf(city, river2, night, river4)

    @Test
    fun `같은 모델은 한 칸 — 켤레는 신발 번호 순, 칸 수가 아니라 켤레 수를 센다`() {
        val groups = ownedGroups(owned)
        assertEquals(3, groups.size)
        assertEquals(4, groups.sumOf { it.count })
        val river = groups.single { it.count == 2 }
        assertEquals(listOf(12L, 14L), river.copies.map { it.id })
        // 대표 — 신고 있는 켤레가 없으면 기존 보관함 규칙(레벨 · 번호가 가장 큰 켤레)
        assertEquals(14L, river.representative.id)
        assertEquals(12L, representativeOf(listOf(river2.copy(equipped = true), river4)).id)
    }

    @Test
    fun `착용을 바꿔도 칸 순서는 그대로 — 돌아왔을 때 가로 목록 자리가 맞는다`() {
        val before = ownedGroups(owned).map { it.key }
        val after = ownedGroups(listOf(city.copy(equipped = false), river2.copy(equipped = true), night, river4)).map { it.key }
        assertEquals(before, after)
    }

    @Test
    fun `고르기는 소유 id — 목록이 바뀌어도 같은 켤레, 없어지면 신고 있는 켤레`() {
        val groups = ownedGroups(owned)
        assertEquals(12L, resolveSelection(groups, 12L)?.id)
        // 새 신발이 앞에 들어와 칸 순서가 밀려도 예전 인덱스의 다른 신발을 고르지 않는다
        val newer = legacy(id = 20, faction = Faction.LIGHTNING, rarity = Rarity.LEGENDARY, variant = 0, mint = 5, acquired = 9_000)
        val moved = ownedGroups(owned + newer)
        assertEquals(newer.id, moved.first().representative.id)
        assertEquals(12L, resolveSelection(moved, 12L)?.id)
        // 팔거나 지워져 없는 id — 신고 있는 켤레로
        assertEquals(city.id, resolveSelection(groups, 999L)?.id)
        // 아무것도 신지 않았으면(17) 첫 칸의 대표
        val bare = ownedGroups(owned.map { it.copy(equipped = false) })
        assertEquals(bare.first().representative.id, resolveSelection(bare, null)?.id)
        assertNull(resolveSelection(emptyList(), 12L))
    }

    @Test
    fun `칸을 누르면 그 모델에서 보던 켤레를 지키고, 아니면 대표 — 착용은 그대로`() {
        val river = ownedGroups(owned).single { it.count == 2 }
        assertEquals(12L, pickInGroup(river, 12L))
        assertEquals(14L, pickInGroup(river, city.id))
        assertTrue(owned.single { it.equipped }.id == city.id)
    }

    // ── 상세 상태 ──────────────────────────────────────────────

    @Test
    fun `상세 상태 — 읽는 중 · 실패 · 없는 신발 · 신발(착용 없음 포함)을 가른다`() {
        assertEquals(ShoeDetailState.Loading, detailStateOf(OwnedLoad.Loading, 12))
        assertEquals(ShoeDetailState.Failed, detailStateOf(OwnedLoad.Failed, 12))
        // 다 읽은 뒤에만 없는 신발 — 빈 목록도 읽기가 끝난 것이다
        assertEquals(ShoeDetailState.NotFound, detailStateOf(OwnedLoad.Ready(owned), 999))
        assertEquals(ShoeDetailState.NotFound, detailStateOf(OwnedLoad.Ready(emptyList()), 12))
        val ready = detailStateOf(OwnedLoad.Ready(owned), 12) as ShoeDetailState.Ready
        assertEquals(12L, ready.shoe.id)
        assertEquals(city.id, ready.wearing?.id)
        val first = detailStateOf(OwnedLoad.Ready(owned.map { it.copy(equipped = false) }), 12) as ShoeDetailState.Ready
        assertNull(first.wearing)
    }

    // ── 착용 결말 ──────────────────────────────────────────────

    @Test
    fun `저장된 착용이 고른 켤레일 때만 신었어요`() {
        assertEquals(EquipResult.Worn(12), classifyEquip(12, EconomyOutcome.Ok, storedWornId = 12))
        // 연결이 끊겼어도 다시 받아 온 값이 그 켤레면 신은 것이다
        assertEquals(EquipResult.Worn(12), classifyEquip(12, EconomyOutcome.Offline, storedWornId = 12))
    }

    @Test
    fun `성공 신호만으로 신었어요라고 하지 않는다 — 확인하지 못했어요`() {
        val result = classifyEquip(12, EconomyOutcome.Ok, storedWornId = 11) as EquipResult.NotWorn
        assertEquals(EquipFailure.UNCONFIRMED, result.reason)
        assertFalse(result.confirmed)
        assertEquals(11L, result.keptId)
    }

    @Test
    fun `거절 · 로그인 필요는 기존 착용 그대로라고 말한다, 연결 끊김은 단정하지 않는다`() {
        val rejected = classifyEquip(12, EconomyOutcome.Rejected("판매 중"), storedWornId = 11) as EquipResult.NotWorn
        assertEquals(EquipFailure.REJECTED, rejected.reason)
        assertTrue(rejected.confirmed)
        assertEquals(11L, rejected.keptId)
        val signIn = classifyEquip(12, EconomyOutcome.SignInRequired, storedWornId = null) as EquipResult.NotWorn
        assertEquals(EquipFailure.SIGN_IN, signIn.reason)
        assertNull(signIn.keptId)
        val offline = classifyEquip(12, EconomyOutcome.Offline, storedWornId = 11) as EquipResult.NotWorn
        assertEquals(EquipFailure.OFFLINE, offline.reason)
        assertFalse(offline.confirmed)
    }

    // ── 그림 · 문구 ────────────────────────────────────────────

    @Test
    fun `그림 — 새 도감은 모델 번호로, 모르는 모델은 다른 신발 그림으로 채우지 않는다`() {
        assertEquals(R.drawable.shoe_1101, sneakerArtRes(legacy().copy(modelId = 1101)))
        assertNull(sneakerArtRes(legacy().copy(modelId = 1999)))
        // 예전 52종 — 속성 × 변형(바람 일반 0 = WND-010)
        assertEquals(R.drawable.sneaker_wind_10, sneakerArtRes(legacy()))
    }

    // ── 상세 본문 네 줄(카툰 입체형) ─────────────────────────────

    @Test
    fun `상세 네 줄은 레벨 · 효율 · 착화감 · 내구도 순서, 실제 값과 막대 길이`() {
        val shoe = server(efficiencyBps = 800, comfortBps = 580, durability = 92.6).copy(level = 5)
        val rows = detailStatRows(shoe)
        assertEquals(listOf(DetailStat.LEVEL, DetailStat.EFFICIENCY, DetailStat.COMFORT, DetailStat.DURABILITY), rows.map { it.stat })
        // 레벨 상한은 서버의 양수 상한(여기서는 10)
        assertEquals("5 / 10", rows[0].value)
        assertEquals(0.5f, rows[0].fraction, 0.0001f)
        assertEquals("+8.0%", rows[1].value)
        assertEquals((8.0 / 27.5).toFloat(), rows[1].fraction, 0.0001f)
        assertEquals(formatPercent(energySavingPercent(shoe)), rows[2].value)
        assertEquals((energySavingPercent(shoe) / 20.0).toFloat(), rows[2].fraction, 0.0001f)
        // 서버 내구도는 내려서 — 92.6 은 92
        assertEquals("92 / 100", rows[3].value)
        assertEquals(0.92f, rows[3].fraction, 0.0001f)
    }

    @Test
    fun `서버 상한이 없으면 등급 기본 레벨 상한, 막대는 0 에서 1 사이`() {
        val rare = legacy(rarity = Rarity.RARE, level = 5)
        assertEquals("5 / 15", detailStatRows(rare)[0].value)
        assertEquals(5f / 15f, detailStatRows(rare)[0].fraction, 0.0001f)
        assertEquals("30 / 30", detailStatRows(legacy(rarity = Rarity.LEGENDARY, level = 30))[0].value)
        assertEquals(1f, detailStatRows(legacy(rarity = Rarity.LEGENDARY, level = 30))[0].fraction, 0f)
        // 상한을 넘는 값 · 음수 내구도는 막대 끝 · 0 에서 멈춘다
        assertEquals(1f, detailStatRows(server(efficiencyBps = 4_000))[1].fraction, 0f)
        assertEquals(0f, detailStatRows(server(durability = -3.0))[3].fraction, 0f)
    }

    @Test
    fun `신발 번호는 네 자리까지 0 을 채우고 길면 자르지 않는다`() {
        assertEquals("No. 0007", formatShoeNumber(7))
        assertEquals("No. 0123", formatShoeNumber(123))
        assertEquals("No. 12345", formatShoeNumber(12_345))
    }

    @Test
    fun `한국어 목적격 조사`() {
        assertEquals("새벽 강변을", withObjectParticle("새벽 강변"))
        assertEquals("시티 스프린트를", withObjectParticle("시티 스프린트"))
        assertEquals("클라우드 러너를", withObjectParticle("클라우드 러너"))
        assertEquals("레드라인 100을", withObjectParticle("레드라인 100"))
        assertEquals("신발 모델 1402를", withObjectParticle("신발 모델 1402"))
        assertEquals("Redline을(를)", withObjectParticle("Redline"))
    }

    // ── 도우미 ─────────────────────────────────────────────────

    private fun legacy(
        id: Long = 1,
        faction: Faction = Faction.WIND,
        rarity: Rarity = Rarity.COMMON,
        variant: Int = 0,
        level: Int = 1,
        mint: Int = 1,
        luck: Double = 1.05,
        comfort: Double = 1.12,
        equipped: Boolean = false,
        acquired: Long = 0,
    ) = Sneaker(
        id = id, faction = faction, rarity = rarity, variant = variant, level = level, mintNumber = mint,
        luck = luck, comfort = comfort, durability = 100, equipped = equipped, acquiredAt = acquired,
    )

    private fun server(efficiencyBps: Int = 45, comfortBps: Int = 750, durability: Double = 100.0) =
        legacy(comfort = 1.0 + comfortBps / 10_000.0, luck = 1.0).copy(
            server = ServerStats(
                origin = "DRAW", efficiencyBps = efficiencyBps, comfortBps = comfortBps, durabilityPts = durability,
                maxLevel = 10, status = "OWNED", chainState = "APP", canWithdraw = false, upgradeCost = 0.0,
                repairCostPerPoint = 0.0, genesisNo = 0,
            ),
        )
}
