package com.stepup.android

import com.stepup.android.domain.BoxStatus
import com.stepup.android.domain.RepairV2Result
import com.stepup.android.domain.ShoeIndex
import com.stepup.android.domain.ShoeQuality
import com.stepup.android.domain.ShoeRules
import com.stepup.android.domain.ShoeType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/** 경제 4판(서버 0116) 규칙 모델 — 서버 검사(schema_test "경제 4판")와 같은 숫자를 낸다 */
class EconomyV4RulesTest {
    private val r = ShoeRules.FALLBACK

    @Test fun `레벨업 포인트 — 등급마다 레벨마다 2 · 3 · 4 · 5 · 6 · 8, 5레벨마다 보너스`() {
        assertEquals(2 * 19 + 3 * 4, r.pointsTotal("COMMON", 20))
        assertEquals(5 * 26 + 8 * 5, r.pointsTotal("LEGENDARY", 27))
        assertEquals(8 * 29 + 12 * 6, r.pointsTotal("FINISH", 30))
        assertEquals(0, r.pointsTotal("FINISH", 1))
        assertEquals(5 + 8, r.pointsAt("LEGENDARY", 10))
        assertEquals(5, r.pointsAt("LEGENDARY", 11))
    }

    @Test fun `포인트 1 = 뽑은 값의 등급 폭 — 효율 · 착화감 · 내구도`() {
        // 가운데 일반(효율 140)에 포인트 10 = +10% → 154
        assertEquals(5400, r.efficiencyWithPoints(4000, "COMMON", 10))
        // 지수 100 착화감에 +100% → 에너지 절반(5,000 bps)
        assertEquals(5000, r.comfortWithPoints(0, "COMMON", 100))
        assertEquals(171, r.durabilityWithPoints(155, "COMMON", 10))
        assertEquals(590, r.durabilityWithPoints(500, "FINISH", 10))
    }

    @Test fun `지수 — 효율 · 착화감 · 분`() {
        assertEquals(140.0, ShoeIndex.efficiency(4000), 1e-9)
        assertEquals(140.0, ShoeIndex.comfort(2857), 0.01)
        assertEquals(31.0, ShoeIndex.minutes(155), 1e-9)
    }

    @Test fun `수리 단가 — 걸어서 버는 돈의 45%`() {
        val unit = r.repairUnit(4000)
        assertEquals(0.45 * 110 * 0.0118 * 1.4 / 5, unit, 1e-12)
        assertEquals(1.5, r.repairUnit(11000) / unit, 1e-9)
    }

    @Test fun `별 · 유형 — 가운데는 ★★, 한쪽이 높으면 그 유형`() {
        val mid = ShoeQuality.of(r, "COMMON", 4000, 2857, 155)
        assertEquals(2, mid.stars)
        assertEquals(ShoeType.BALANCED, mid.type)
        val top = ShoeQuality.of(r, "COMMON", 8000, 4444, 200)
        assertEquals(3, top.stars)
        val eff = ShoeQuality.of(r, "COMMON", 8000, 0, 110)
        assertEquals(ShoeType.EFFICIENCY, eff.type)
        assertEquals(1, eff.stars)
    }

    @Test fun `서버 규칙 읽기 — 등급 여섯이 없으면 버린다`() {
        val body = """{"points_per_step":0.0118,"repair_share":0.45,"reward_luck":[0.7,1,1.3],"wear_luck":[0.667,0.889,1.444],
            "comfort_cap_bps":7000,"free_repair_hours":48,"free_repair_ready":true,"free_repair_next":null,
            "forge_material_min_durability":50,"boost_shop_enabled":false,"grades":[""" +
            r.grades.joinToString(",") { g ->
                """{"rarity":"${g.rarity}","efficiency":[${g.efficiencyLo},${g.efficiencyHi}],"comfort":[${g.comfortLo},${g.comfortHi}],""" +
                    """"durability":[${g.durabilityLo},${g.durabilityHi}],"points_per_level":${g.pointsPerLevel},""" +
                    """"points_milestone":${g.pointsMilestone},"point_rate_bps":${g.pointRateBps},"max_level":${g.maxLevel}}"""
            } + "]}"
        val parsed = ShoeRules.parse(body)
        assertNotNull(parsed)
        assertEquals(r.grades, parsed!!.grades)
        assertTrue(parsed.fromServer)
        assertEquals(true, parsed.freeRepairReady)
        assertNull(ShoeRules.parse("""{"grades":[]}"""))
    }

    @Test fun `새 수리 결과 · 상자 현황 읽기`() {
        val changed = RepairV2Result.parse("""{"ok":false,"code":"QUOTE_CHANGED","cost":7.4,"free":false,"units":50}""")!!
        assertFalse(changed.ok)
        assertEquals("QUOTE_CHANGED", changed.code)
        assertEquals(7.4, changed.cost!!, 1e-9)
        val done = RepairV2Result.parse("""{"ok":true,"durability":100,"units":155,"cost":0,"free":true,"balance":12.5,
            "free_next":"2026-11-20T10:00:00+00:00"}""")!!
        assertTrue(done.free)
        assertNotNull(done.freeNextMs)
        val box = BoxStatus.parse("""{"daily_free":1,"steps_today":2400,"run_boxes":[{"steps":2000,"got":true},{"steps":4000,"got":false}],
            "steps_week":9000,"week_target":25000,"week_got":false,"streak_days":7,"levels_got":[5],"normal_price":40,"normal_limit":3,
            "normal_bought":1,"premium_price":600,"premium_limit":2,"premium_bought":0,"free_left":2,"gift_left":1,"balance":100}""")!!
        assertEquals(listOf(2000 to true, 4000 to false), box.runBoxes)
        assertTrue(box.normalCanBuy)
        assertFalse(box.premiumCanBuy)
        assertEquals(listOf(5), box.levelsGot)
    }
}
