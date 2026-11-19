package com.stepup.android

import com.stepup.android.data.local.AppDatabase
import com.stepup.android.data.local.SneakerEntity
import com.stepup.android.data.remote.ShoeXp
import com.stepup.android.data.remote.ShoeXpApi
import com.stepup.android.data.remote.ShoeXpResult
import com.stepup.android.data.repo.EconomySync
import com.stepup.android.domain.Faction
import com.stepup.android.domain.ForgeXpRule
import com.stepup.android.domain.GoalRule
import com.stepup.android.domain.Rarity
import com.stepup.android.domain.ServerStats
import com.stepup.android.domain.ShoeRules
import com.stepup.android.domain.Sneaker
import com.stepup.android.domain.StatPoints
import com.stepup.android.domain.nextLevelPercentText
import com.stepup.android.domain.nextLevelTenths
import com.stepup.android.ui.screens.items.DetailStat
import com.stepup.android.ui.screens.items.PointsBasis
import com.stepup.android.ui.screens.items.PointsDraft
import com.stepup.android.ui.screens.items.detailStatRows
import com.stepup.android.ui.screens.items.firstPointsOf
import com.stepup.android.ui.screens.items.minutesPerPointText
import com.stepup.android.ui.screens.items.rateText
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * 강화 경험치 줄 · 능력치 ⓘ · 폰 DB 판 23(2026-11-19, 서버 0118) — docs/forge-xp-daily-spec-2026-11-19.md 5.1 · 5.2.
 * %는 서버와 같은 식 floor(xp × 1000 ÷ need) ÷ 10, 100.0% 는 보이지 않는다.
 */
class ShoeXpInfoTest {

    // ── 다음 레벨까지 % ──────────────────────────────────────────

    @Test fun `다음 레벨까지 — 서버 식 그대로 버려서 소수 한 자리`() {
        // 계약서의 예: xp 1200 · need 3020 → 39.7
        assertEquals("39.7", shoe(xp = 1200, need = 3020).nextLevelPercentText)
        // 2000 ÷ 2880 = 69.44… → 69.4
        assertEquals("69.4", shoe(xp = 2000, need = 2880).nextLevelPercentText)
        // 65.36… → 65.3(반올림 65.4 가 아니다)
        assertEquals(653, shoe(xp = 1882, need = 2880).nextLevelTenths)
        assertEquals("0.0", shoe(xp = 0, need = 2880).nextLevelPercentText)
    }

    @Test fun `100_0 퍼센트는 보이지 않는다 — 통이 다 차기 직전은 99_9`() {
        assertEquals("99.9", shoe(xp = 2879, need = 2880).nextLevelPercentText)
        // 서버가 아직 레벨을 올리지 않은 이상한 값이어도 100.0 을 보이지 않는다
        assertEquals(999, shoe(xp = 2880, need = 2880).nextLevelTenths)
        assertEquals(999, shoe(xp = 9000, need = 2880).nextLevelTenths)
        assertEquals(0, shoe(xp = -5, need = 2880).nextLevelTenths)
    }

    @Test fun `모르거나 만렙이면 줄이 없다`() {
        assertNull(shoe(xp = null, need = null).nextLevelTenths)
        assertNull(shoe(xp = 1200, need = null).nextLevelTenths)
        assertNull(shoe(xp = null, need = 3020).nextLevelTenths)
        assertNull(shoe(xp = 0, need = 0).nextLevelTenths)
        assertNull(shoe(xp = 100, need = 3020, level = 20, maxLevel = 20).nextLevelTenths)
        assertNull(shoe(xp = 100, need = 3020).copy(server = null).nextLevelTenths)
    }

    @Test fun `상세 네 줄 — 레벨 칸에 다음 레벨까지, ⓘ 는 효율 · 착화감 · 내구도`() {
        val rows = detailStatRows(shoe(xp = 1200, need = 3020))
        assertEquals(DetailStat.LEVEL, rows[0].stat)
        assertEquals("39.7", rows[0].nextLevelPercent)
        assertFalse(rows[0].explained)
        assertEquals(listOf(true, true, true), rows.drop(1).map { it.explained })
        assertTrue(rows.drop(1).all { it.nextLevelPercent == null })
        assertNull(detailStatRows(shoe(xp = null, need = null))[0].nextLevelPercent)
    }

    // ── ⓘ 숫자 ───────────────────────────────────────────────────

    @Test fun `포인트 1 의 폭 — 등급의 point_rate_bps 그대로`() {
        assertEquals("1", basis("COMMON", 150).rateText())
        assertEquals("1.1", basis("RARE", 150).rateText())
        assertEquals("1.25", basis("EPIC", 250).rateText())
        assertEquals("1.8", basis("FINISH", 500).rateText())
    }

    @Test fun `내구도 포인트 1 의 분 — 뽑은 최대 내구도 × 폭 ÷ 5, 내려서 소수 한 자리`() {
        // 일반 150 × 1% = 1.5 (12초 단위) = 0.3분
        assertEquals("0.3", basis("COMMON", 150).minutesPerPointText())
        // 레어 150 × 1.1% = 1.65 = 0.33분 → 0.3
        assertEquals("0.3", basis("RARE", 150).minutesPerPointText())
        // 레전더리 485 × 1.4% = 6.79 = 1.358분 → 1.3(올리지 않는다)
        assertEquals("1.3", basis("LEGENDARY", 485).minutesPerPointText())
        // 피니시 500 × 1.8% = 9 = 1.8분, 일반 500 × 1% = 5 = 1분(끝의 .0 은 뺀다)
        assertEquals("1.8", basis("FINISH", 500).minutesPerPointText())
        assertEquals("1", basis("COMMON", 500).minutesPerPointText())
        // 뽑은 값을 모르면 그 등급의 가장 낮은 값(일반 110 × 1% = 1.1 = 0.22분)
        assertEquals("0.2", basis("COMMON", 0).minutesPerPointText())
    }

    @Test fun `처음 포인트가 생긴 사람 — 아무 신발에도 나눈 적이 없을 때만`() {
        assertTrue(firstPointsOf(listOf(pointsShoe(total = 3), pointsShoe(total = 0))))
        assertFalse(firstPointsOf(listOf(pointsShoe(total = 3), pointsShoe(total = 5, efficiency = 1))))
        // 포인트가 있는 신발이 없으면 아니다(나눌 것이 없다)
        assertFalse(firstPointsOf(listOf(pointsShoe(total = 0))))
        assertFalse(firstPointsOf(emptyList()))
    }

    // ── 규칙(서버 shoe_rules_v4 의 forge · goal) ─────────────────

    @Test fun `서버 규칙 — forge · goal 묶음을 읽고, 없으면 기본값(fromServer false)`() {
        val withBlocks = rulesBody(
            ""","forge":{"rule_version":"forge-xp-1","step":3.1,"need":2.88,"spec":[0.7,1.3],"crit_chance":0.1,"crit_mult":2,
            "inherit":0.5,"sup_per_unit":24,"max_materials":10},"goal":{"min":4000,"max":20000,"default":6000,"step":500}""",
        )
        val parsed = ShoeRules.parse(withBlocks)!!
        assertTrue(parsed.forge.fromServer)
        assertTrue(parsed.goal.fromServer)
        assertEquals(ForgeXpRule.FALLBACK, parsed.forge.copy(fromServer = false))
        assertEquals(GoalRule.FALLBACK, parsed.goal.copy(fromServer = false))
        assertEquals(0.7, parsed.forge.specLo, 0.0)
        assertEquals(1.3, parsed.forge.specHi, 0.0)

        val changed = ShoeRules.parse(rulesBody(""","forge":{"step":3.5,"max_materials":6},"goal":{"min":5000,"max":15000,"default":3000}"""))!!
        assertEquals(3.5, changed.forge.step, 0.0)
        assertEquals(6, changed.forge.maxMaterials)
        assertEquals(2.88, changed.forge.need, 0.0)
        assertEquals(5000, changed.goal.min)
        // 기본 목표는 범위 안으로
        assertEquals(5000, changed.goal.default)

        val old = ShoeRules.parse(rulesBody(""))!!
        assertFalse(old.forge.fromServer)
        assertFalse(old.goal.fromServer)
        assertEquals(ForgeXpRule.FALLBACK, old.forge)
        // 범위가 뒤집힌 goal 은 버린다
        assertEquals(GoalRule.FALLBACK, ShoeRules.parse(rulesBody(""","goal":{"min":9000,"max":4000}"""))!!.goal)
    }

    @Test fun `목표 맞추기 — 서버 goal_clamp 처럼 500 단위 반올림 · 범위 안`() {
        val g = GoalRule.FALLBACK
        assertEquals(4000, g.fit(1000))
        assertEquals(4000, g.fit(3999))
        assertEquals(4500, g.fit(4250))
        assertEquals(8000, g.fit(8100))
        assertEquals(20000, g.fit(30000))
    }

    @Test fun `FALLBACK — 예전 강화의 재료는 내구도 가득만(0118)`() {
        assertEquals(100.0, ShoeRules.FALLBACK.forgeMaterialMinDurability, 0.0)
        assertEquals(ForgeXpRule.FALLBACK, ShoeRules.FALLBACK.forge)
        assertEquals(GoalRule.FALLBACK, ShoeRules.FALLBACK.goal)
    }

    // ── my_shoe_xp 읽기 · 동기화 ─────────────────────────────────

    @Test fun `my_shoe_xp — 신발마다 통, 만렙은 통 크기 null, numeric 도 읽는다`() {
        val parsed = ShoeXpApi.parse(
            """[{"id":1,"xp":1200,"need":3020,"pct":39.7},{"id":2,"xp":0,"need":null,"pct":null},{"id":3,"xp":250.0,"need":2880.0}]""",
        )!!
        assertEquals(ShoeXp(1200, 3020), parsed[1L])
        assertEquals(ShoeXp(0, null), parsed[2L])
        assertEquals(ShoeXp(250, 2880), parsed[3L])
        assertEquals(emptyMap<Long, ShoeXp>(), ShoeXpApi.parse("[]"))
        assertNull(ShoeXpApi.parse("""{"error":"x"}"""))
        assertTrue(ShoeXpApi.missingFunction("Could not find the function public.my_shoe_xp without parameters in the schema cache"))
        assertFalse(ShoeXpApi.missingFunction("로그인이 필요합니다"))
    }

    @Test fun `동기화 — 받으면 서버 값, 함수가 없으면 비우고, 못 받으면 레벨이 같을 때만 지난 값`() {
        val ok = ShoeXpResult.Ok(mapOf(1L to ShoeXp(1200, 3020), 2L to ShoeXp(0, null)))
        val before = entity(level = 3, xp = 900, need = 3020)
        assertEquals(1200 to 3020, EconomySync.forgeXpOf(ok, 1, 3, before))
        assertEquals(0 to null, EconomySync.forgeXpOf(ok, 2, 20, null))
        // 목록과 통 사이에 생긴 신발 — 모른다
        assertEquals(null to null, EconomySync.forgeXpOf(ok, 9, 1, null))
        assertEquals(null to null, EconomySync.forgeXpOf(ShoeXpResult.Missing, 1, 3, before))
        assertEquals(900 to 3020, EconomySync.forgeXpOf(ShoeXpResult.Failed, 1, 3, before))
        assertEquals(null to null, EconomySync.forgeXpOf(ShoeXpResult.Failed, 1, 4, before))
        assertEquals(null to null, EconomySync.forgeXpOf(ShoeXpResult.Failed, 1, 3, null))
    }

    // ── 폰 DB 판 23 ──────────────────────────────────────────────

    @Test fun `폰 DB 판 23 — 통 두 칸을 비운 채(null) 더하고 마이그레이션 목록에 있다`() {
        // @Database 는 실행 중에 읽히지 않는다(CLASS 보존) — 빌드 스크립트처럼 원본에서 판을 읽는다
        val source = listOf(
            java.io.File("src/main/java/com/stepup/android/data/local/AppDatabase.kt"),
            java.io.File("app/src/main/java/com/stepup/android/data/local/AppDatabase.kt"),
        ).first { it.exists() }.readText()
        val version = Regex("""(?m)^\s*version\s*=\s*(\d+)\s*,?\s*$""").find(source)!!.groupValues[1].toInt()
        assertEquals(23, version)
        assertTrue(AppDatabase.MIGRATION_22_23 in AppDatabase.MIGRATIONS)
        assertEquals(22, AppDatabase.MIGRATION_22_23.startVersion)
        assertEquals(23, AppDatabase.MIGRATION_22_23.endVersion)
        // 판마다 한 칸씩 빠짐없이 이어진다(6 → 23)
        assertEquals((6 until version).toList(), AppDatabase.MIGRATIONS.map { it.startVersion })
        assertTrue(AppDatabase.MIGRATIONS.all { it.endVersion == it.startVersion + 1 })

        val sql = mutableListOf<String>()
        val db = java.lang.reflect.Proxy.newProxyInstance(
            javaClass.classLoader, arrayOf(androidx.sqlite.db.SupportSQLiteDatabase::class.java),
        ) { _, method, args ->
            if (method.name == "execSQL") sql += args!![0] as String
            null
        } as androidx.sqlite.db.SupportSQLiteDatabase
        AppDatabase.MIGRATION_22_23.migrate(db)
        assertEquals(
            listOf("ALTER TABLE sneakers ADD COLUMN forgeXp INTEGER", "ALTER TABLE sneakers ADD COLUMN forgeNeed INTEGER"),
            sql,
        )
        // 새 줄도 모르는 채로 시작한다
        assertNull(entity(level = 1).forgeXp)
        assertNull(entity(level = 1).forgeNeed)
    }

    // ── 도우미 ──────────────────────────────────────────────────

    private fun shoe(xp: Int?, need: Int?, level: Int = 3, maxLevel: Int = 20) = Sneaker(
        id = 1, faction = Faction.WIND, rarity = Rarity.RARE, variant = 0, level = level, mintNumber = 1,
        luck = 1.0, comfort = 1.0, durability = 100, equipped = false, acquiredAt = 0L,
        server = ServerStats(
            origin = "PAID_DRAW", efficiencyBps = 11000, comfortBps = 2500, durabilityPts = 100.0,
            maxLevel = maxLevel, status = "OWNED", chainState = "APP", canWithdraw = false, upgradeCost = 0.0,
            repairCostPerPoint = 0.0, genesisNo = 0, forgeXp = xp, forgeNeed = need,
        ),
    )

    private fun basis(rarity: String, durabilityBase: Int) = PointsBasis(
        shoeId = 1, total = 4, saved = PointsDraft(), respec = false,
        efficiencyBaseBps = 4000, comfortBaseBps = 2857, durabilityBase = durabilityBase, durabilityNow = durabilityBase,
        rarity = rarity, rules = ShoeRules.FALLBACK,
    )

    private fun pointsShoe(total: Int, efficiency: Int = 0) = Sneaker(
        id = total.toLong() + efficiency * 100L, faction = Faction.WIND, rarity = Rarity.COMMON, variant = 0, level = 3, mintNumber = 1,
        luck = 1.0, comfort = 1.0, durability = 100, equipped = false, acquiredAt = 0L,
        server = ServerStats(
            origin = "PAID_DRAW", efficiencyBps = 4000, comfortBps = 2857, durabilityPts = 100.0,
            maxLevel = 20, status = "OWNED", chainState = "APP", canWithdraw = false, upgradeCost = 0.0,
            repairCostPerPoint = 0.0, genesisNo = 0, durabilityNow = 155, durabilityMax = 155, durabilityBase = 155,
            efficiencyBaseBps = 4000, comfortBaseBps = 2857,
            points = StatPoints(total = total, free = total - efficiency, efficiency = efficiency, comfort = 0, durability = 0, respec = false),
        ),
    )

    private fun entity(level: Int, xp: Int? = null, need: Int? = null) = SneakerEntity(
        id = 1, factionId = "NONE", rarity = "RARE", variant = 0, level = level, mintNumber = 1, luck = 1.0, comfort = 1.0,
        durability = 100, equipped = false, acquiredAt = 0L, serverId = 1, origin = "PAID_DRAW", forgeXp = xp, forgeNeed = need,
    )

    private fun rulesBody(extra: String): String {
        val r = ShoeRules.FALLBACK
        return """{"points_per_step":0.0118,"repair_share":0.45,"comfort_cap_bps":7000,"forge_material_min_durability":100,"grades":[""" +
            r.grades.joinToString(",") { g ->
                """{"rarity":"${g.rarity}","efficiency":[${g.efficiencyLo},${g.efficiencyHi}],"comfort":[${g.comfortLo},${g.comfortHi}],""" +
                    """"durability":[${g.durabilityLo},${g.durabilityHi}],"points_per_level":${g.pointsPerLevel},""" +
                    """"points_milestone":${g.pointsMilestone},"point_rate_bps":${g.pointRateBps},"max_level":${g.maxLevel}}"""
            } + "]" + extra + "}"
    }
}
