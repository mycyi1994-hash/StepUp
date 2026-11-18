package com.stepup.android

import com.stepup.android.data.remote.AuthUser
import com.stepup.android.domain.BoxStatus
import com.stepup.android.domain.Faction
import com.stepup.android.domain.Rarity
import com.stepup.android.domain.RunLuck
import com.stepup.android.domain.ServerStats
import com.stepup.android.domain.Sneaker
import com.stepup.android.ui.screens.gacha.BoxBuyBlock
import com.stepup.android.ui.screens.gacha.BoxKind
import com.stepup.android.ui.screens.gacha.BoxRefusal
import com.stepup.android.ui.screens.gacha.boxRefusal
import com.stepup.android.ui.screens.gacha.buyView
import com.stepup.android.ui.screens.gacha.runBoxViews
import com.stepup.android.ui.screens.gacha.weekFraction
import com.stepup.android.ui.screens.settings.SocialAccount
import com.stepup.android.ui.screens.settings.socialAccountOf
import com.stepup.android.ui.screens.walk.ShoeCheckLine
import com.stepup.android.ui.screens.walk.energyUsedUp
import com.stepup.android.ui.screens.walk.shoeCheckLine
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * 경제 4판(서버 0116) 앱 쪽 — 상자 받는 곳의 화면 값, 보상의 운 글, 출발 전 점검의 적립 분 · 적립 한도, 이은 소셜 계정.
 * 값은 모두 서버가 준 것이고 여기서는 어느 글 · 상태를 보일지만 고른다(서버 규칙 자체는 supabase/tests 가 본다).
 */
class EconomyV4BoxesTest {

    private val boxes = BoxStatus(
        dailyFree = 1, stepsToday = 2_600, runBoxes = listOf(4_000 to false, 2_000 to true),
        stepsWeek = 12_300, weekTarget = 25_000, weekGot = false, streakDays = 7, levelsGot = listOf(10, 5),
        normalPrice = 40.0, normalLimit = 3, normalBought = 1, premiumPrice = 600.0, premiumLimit = 2, premiumBought = 0,
        freeLeft = 4, giftLeft = 2, balance = 120.5,
    )

    // ── 상자 받는 곳 ──

    @Test fun `러닝 상자는 문턱 차례로 · 받은 것은 가득 · 못 받은 것은 오늘 걸음까지`() {
        val runs = boxes.runBoxViews()
        assertEquals(listOf(2_000, 4_000), runs.map { it.steps })
        assertTrue(runs[0].got)
        assertEquals(1f, runs[0].fraction, 0.0001f)
        assertFalse(runs[1].got)
        assertEquals(2_600, runs[1].progress)
        assertEquals(0.65f, runs[1].fraction, 0.0001f)
        // 오늘 걸음이 문턱을 넘었는데 아직 받지 않았다고 서버가 말하면 — 문턱에서 멈춘다(넘친 수를 적지 않는다)
        val over = boxes.copy(stepsToday = 9_000, runBoxes = listOf(4_000 to false)).runBoxViews().single()
        assertEquals(4_000, over.progress)
        assertFalse(over.got)
    }

    @Test fun `이번 주 상급 상자의 몫 — 받았으면 가득, 목표가 없으면 0`() {
        assertEquals(12_300f / 25_000f, boxes.weekFraction, 0.0001f)
        assertEquals(1f, boxes.copy(weekGot = true).weekFraction, 0f)
        assertEquals(0f, boxes.copy(weekTarget = 0).weekFraction, 0f)
        assertEquals(1f, boxes.copy(stepsWeek = 40_000).weekFraction, 0f)
    }

    @Test fun `사기 칸은 서버 현황으로만 — 팔지 않음 · 한도 · 잔고 · 살 수 있음`() {
        val normal = boxes.buyView(BoxKind.NORMAL)
        assertNull(normal.block)
        assertTrue(normal.canBuy)
        assertEquals(1, normal.bought)
        assertEquals(3, normal.limit)
        // 잔고 120.5 < 600 — 상급은 못 산다
        assertEquals(BoxBuyBlock.BALANCE, boxes.buyView(BoxKind.PREMIUM).block)
        assertEquals(BoxBuyBlock.LIMIT, boxes.copy(normalBought = 3).buyView(BoxKind.NORMAL).block)
        // 한도가 잔고보다 먼저 — 다 샀으면 잔고가 모자라도 "다 샀어요"
        assertEquals(BoxBuyBlock.LIMIT, boxes.copy(premiumBought = 2).buyView(BoxKind.PREMIUM).block)
        assertEquals(BoxBuyBlock.NOT_SELLING, boxes.copy(normalPrice = 0.0).buyView(BoxKind.NORMAL).block)
        assertEquals(BoxBuyBlock.NOT_SELLING, boxes.copy(premiumLimit = 0).buyView(BoxKind.PREMIUM).block)
        assertTrue(boxes.copy(balance = 600.0).buyView(BoxKind.PREMIUM).canBuy)
    }

    @Test fun `사기 거절은 서버 글로 가른다`() {
        assertEquals(BoxRefusal.NOT_ENOUGH, boxRefusal("SUP가 부족합니다 (보유 12.0000, 필요 40)"))
        assertEquals(BoxRefusal.LIMIT, boxRefusal("오늘은 상자를 3개까지 살 수 있어요"))
        assertEquals(BoxRefusal.LIMIT, boxRefusal("상급 상자는 한 주에 2개까지 살 수 있어요"))
        assertEquals(BoxRefusal.NOT_SELLING, boxRefusal("지금은 상자를 팔지 않아요"))
        assertEquals(BoxRefusal.SIGN_IN, boxRefusal("로그인이 필요합니다"))
        assertEquals(BoxRefusal.OTHER, boxRefusal("없는 상자입니다"))
    }

    @Test fun `상자 현황을 서버 모양 그대로 읽는다`() {
        val body = """{"day":"2026-11-18","daily_free":1,"steps_today":2100,
            "run_boxes":[{"steps":2000,"got":true},{"steps":4000,"got":false}],
            "week_start":"2026-11-16","steps_week":2100,"week_target":25000,"week_got":false,"streak_days":7,
            "levels_got":[5],"normal_price":40,"normal_limit":3,"normal_bought":0,
            "premium_price":600,"premium_limit":2,"premium_bought":1,"free_left":3,"gift_left":1,"balance":"640.2500"}"""
        val read = BoxStatus.parse(body)!!
        assertEquals(listOf(2_000 to true, 4_000 to false), read.runBoxes)
        assertEquals(1, read.giftLeft)
        assertEquals(640.25, read.balance, 0.0001)
        assertTrue(read.buyView(BoxKind.PREMIUM).canBuy)
    }

    // ── 보상의 운 ──

    @Test fun `운은 소수 둘째 자리로 · 평소는 0점98 ~ 1점02`() {
        assertEquals("×1.12", RunLuck.factor(1.12))
        assertEquals("×0.70", RunLuck.factor(0.7))
        assertEquals("×1.30", RunLuck.factor(1.2999))
        assertEquals(RunLuck.Tone.GOOD, RunLuck.tone(1.12))
        assertEquals(RunLuck.Tone.GOOD, RunLuck.tone(1.03))
        assertEquals(RunLuck.Tone.EVEN, RunLuck.tone(1.02))
        assertEquals(RunLuck.Tone.EVEN, RunLuck.tone(1.0))
        assertEquals(RunLuck.Tone.EVEN, RunLuck.tone(0.98))
        assertEquals(RunLuck.Tone.LOW, RunLuck.tone(0.97))
        assertEquals(RunLuck.Tone.LOW, RunLuck.tone(0.7))
    }

    @Test fun `운은 서버가 확인하고 적립이 있는 러닝에만`() {
        assertEquals(1.12, RunLuck.shown(1.12, 25.0, signed = true)!!, 0.0)
        // 확인 전 · 무효(예전 서버 · 0116 전은 null) · 적립 0 · 이상한 값은 보이지 않는다
        assertNull(RunLuck.shown(1.12, 25.0, signed = false))
        assertNull(RunLuck.shown(null, 25.0, signed = true))
        assertNull(RunLuck.shown(1.12, 0.0, signed = true))
        assertNull(RunLuck.shown(1.12, null, signed = true))
        assertNull(RunLuck.shown(Double.NaN, 25.0, signed = true))
    }

    // ── 출발 전 점검 ──

    private fun shoe(now: Int, max: Int, factor: Double = if (max > 0) now * 100.0 / max else 100.0) = Sneaker(
        id = 1, faction = Faction.WIND, rarity = Rarity.COMMON, variant = 0, level = 1, mintNumber = 1,
        luck = 1.0, comfort = 1.0, durability = 100, equipped = true, acquiredAt = 0L, modelId = 1107,
    ).copy(
        server = ServerStats(
            origin = "DRAW", efficiencyBps = 0, comfortBps = 0, durabilityPts = factor,
            maxLevel = 20, status = "OWNED", chainState = "APP", canWithdraw = false, upgradeCost = 0.0,
            repairCostPerPoint = 0.0, genesisNo = 0, durabilityNow = now, durabilityMax = max, durabilityBase = max,
        ),
    )

    @Test fun `가득 찬 새 신발도 적립되는 분을 알린다`() {
        // 4판 가운데 일반 신발 155(31분) — 가득
        assertEquals(ShoeCheckLine.Earns(31), shoeCheckLine(shoe(now = 155, max = 155)))
        // 반쯤 남은 신발(80 → 16분)도 분으로
        assertEquals(ShoeCheckLine.Earns(16), shoeCheckLine(shoe(now = 80, max = 155)))
    }

    @Test fun `곧 닳는 신발 · 다 닳은 신발 · 분을 모르는 신발`() {
        // 기준(그 신발 수명의 절반 · 10분 가운데 작은 것)보다 짧으면 경고 — 4분
        assertEquals(ShoeCheckLine.Low(4), shoeCheckLine(shoe(now = 20, max = 155)))
        assertEquals(ShoeCheckLine.WornOut, shoeCheckLine(shoe(now = 0, max = 155)))
        // 최대 내구도를 모르는 신발(예전 서버) — "달릴 준비가 됐어요"
        val legacy = Sneaker(
            id = 2, faction = Faction.WIND, rarity = Rarity.COMMON, variant = 0, level = 1, mintNumber = 1,
            luck = 1.0, comfort = 1.0, durability = 100, equipped = true, acquiredAt = 0L,
        )
        assertEquals(ShoeCheckLine.Ready, shoeCheckLine(legacy))
    }

    @Test fun `적립 한도는 읽은 에너지로만 — 읽기 전에는 단정하지 않는다`() {
        assertFalse(energyUsedUp(null))
        assertTrue(energyUsedUp(0.0))
        assertFalse(energyUsedUp(2.0))
    }

    // ── 연결된 계정 ──

    @Test fun `이은 소셜 계정은 구글 · 이메일 그대로`() {
        assertEquals(SocialAccount("runner@example.org"), socialAccountOf(AuthUser(id = "u", email = " runner@example.org ")))
        assertEquals(SocialAccount(null), socialAccountOf(AuthUser(id = "u", email = "")))
        assertNull(socialAccountOf(AuthUser(id = "u", email = "a@b.c", isAnonymous = true)))
        assertNull(socialAccountOf(null))
    }
}
