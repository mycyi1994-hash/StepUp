package com.stepup.android

import com.stepup.android.data.remote.ServerResult
import com.stepup.android.data.repo.ForgeLoad
import com.stepup.android.data.repo.ForgeOutcome
import com.stepup.android.data.repo.ForgePendingStore
import com.stepup.android.data.repo.ForgeRepository
import com.stepup.android.data.repo.ForgeServer
import com.stepup.android.data.repo.ForgeXpServer
import com.stepup.android.data.repo.decodePending
import com.stepup.android.data.repo.encodePending
import com.stepup.android.data.repo.pendingFor
import com.stepup.android.data.repo.upsertPending
import com.stepup.android.domain.DrawKind
import com.stepup.android.domain.Faction
import com.stepup.android.domain.ForgeBar
import com.stepup.android.domain.ForgeKind
import com.stepup.android.domain.ForgeMaterialBlock
import com.stepup.android.domain.ForgePending
import com.stepup.android.domain.ForgePrecious
import com.stepup.android.domain.ForgeTargetBlock
import com.stepup.android.domain.ForgeXp
import com.stepup.android.domain.Rarity
import com.stepup.android.domain.ServerStats
import com.stepup.android.domain.ShoeRules
import com.stepup.android.domain.Sneaker
import com.stepup.android.domain.XpCatalog
import com.stepup.android.domain.XpCrit
import com.stepup.android.domain.XpMaterial
import com.stepup.android.domain.XpResult
import com.stepup.android.domain.XpSelection
import com.stepup.android.domain.XpTarget
import com.stepup.android.domain.bulkPick
import com.stepup.android.domain.drawForgeHintTenths
import com.stepup.android.domain.preciousPicked
import com.stepup.android.domain.preview
import com.stepup.android.ui.screens.gacha.PendingDraw
import com.stepup.android.ui.screens.items.formatCap
import com.stepup.android.ui.screens.items.targetStats
import com.stepup.android.ui.screens.items.unrevealedDrawShoes
import com.stepup.android.ui.screens.items.xpPointsGain
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * 경험치 강화(서버 0118, 규칙 forge-xp-1 — docs/forge-xp-daily-spec-2026-11-19.md 2장 · 계약 docs/forge-xp-daily-contract-2026-11-19.md).
 * 값 · %는 서버가 정하고 앱은 그린다 — 여기서는 %의 버림 · 100.0% 막기 · 남는 신발 넣기 · 아까운 재료 · 뽑기 결과 줄 · 서버 답 읽기 ·
 * 걸려 있는 요청(예전 줄 포함) · 응답을 잃었을 때 같은 번호로만 다시 묻기를 본다. 예시 숫자는 계약 문서의 예시다.
 */
class ShoeForgeXpTest {

    // ── % ─────────────────────────────────────────────────────

    @Test
    fun `퍼센트는 서버와 같은 식으로 버린다 — 1200 나누기 3020 은 39_7퍼센트`() {
        assertEquals(397, ForgeXp.tenths(1200, 3020))
        assertEquals("39.7%", ForgeXp.formatPct(397))
        // 일반 ★★ 가득 1켤레(1,000) · 레어 Lv1 통 2,880 → 34.72… → 34.7%
        assertEquals(347, ForgeXp.tenths(1000, 2880))
        assertEquals("+34.7%", ForgeXp.formatGain(347))
        // 2.3 의 예 — Lv5 재료(1,000 + 6,030) 는 7,030 ÷ 2,880 = 244.09… → 버려서 244.0%(기획서 표의 244.1 은 반올림한 값)
        assertEquals(2440, ForgeXp.tenths(7030, 2880))
        assertNull(ForgeXp.tenths(100, null))
        assertEquals(333, ForgeXp.tenthsOf(33.3))
        assertEquals(1, ForgeXp.tenthsOf(0.1))
    }

    @Test
    fun `레벨이 오르지 않은 막대는 100_0퍼센트를 보이지 않는다`() {
        assertEquals(999, ForgeXp.tenths(2879, 2880))
        assertEquals(999, ForgeXp.barTenths(1000))
        assertEquals(1000, ForgeXp.barTenths(1000, leveledUp = true))
        // 서버가 100.0 을 보냈어도(있어선 안 되는 값) 막대는 99.9%
        assertEquals(999, ForgeBar(3, 3020, 3020, 1000).shownTenths)
        assertNull(ForgeBar(20, 0, null, null).shownTenths)
        assertEquals(1f, ForgeBar(20, 0, null, null).fraction)
        // 서버 %가 없으면 같은 식으로
        assertEquals(397, ForgeBar(3, 1200, 3020, null).shownTenths)
    }

    @Test
    fun `대성공 확률 · 배수 · 상한 배수는 서버 값 그대로 적는다`() {
        assertEquals("10%", ForgeXp.formatChance(0.10))
        assertEquals("12.5%", ForgeXp.formatChance(0.125))
        assertEquals("2", ForgeXp.formatMult(2.0))
        assertEquals("1.5", ForgeXp.formatMult(1.5))
        assertEquals("1.36", formatCap(1.36))
        assertEquals("1", formatCap(1.0))
        assertEquals("1.4", formatCap(1.40))
    }

    // ── 고르기 ─────────────────────────────────────────────────

    @Test
    fun `남는 신발 넣기는 아까운 · 막힌 · 값 없는 신발을 고르지 않고 값이 낮은 것부터 통이 찰 때까지`() {
        val catalog = catalog(
            xp = 1200, need = 3020,
            item(11, value = 1007),
            item(12, value = 880),
            item(13, value = 3100, precious = listOf(ForgePrecious.HIGHER_GRADE)),
            item(14, value = 1180),
            item(15, value = 500, block = ForgeMaterialBlock.EQUIPPED),
            item(16, value = 0, block = ForgeMaterialBlock.NO_VALUE),
            item(17, value = 950),
        )
        // 880 + 950 = 1,830 → 1,200 + 1,830 = 3,030 ≥ 3,020 에서 멈춘다
        assertEquals(listOf(12L, 17L), catalog.bulkPick(emptyList()))
        // 이 폰에서 이름을 붙인 신발도 고르지 않는다
        assertEquals(listOf(17L, 11L), catalog.bulkPick(emptyList(), nicknames = mapOf(12L to "출근용")))
        // 지금 고른 것(아까운 신발이라도 사용자가 고른 것)은 그대로 두고 더한다
        assertEquals(listOf(13L), catalog.bulkPick(listOf(13L)))
        // 11(1,007) 을 고른 채 — 2,207 + 880 = 3,087 ≥ 3,020
        assertEquals(listOf(11L, 12L), catalog.bulkPick(listOf(11L)))
    }

    @Test
    fun `남는 신발 넣기는 한 번에 넣는 상한을 넘지 않고 만렙이면 그대로`() {
        val many = (1L..15L).map { item(100 + it, value = 100) }
        val c = catalog(xp = 0, need = 10_000, *many.toTypedArray(), max = 10)
        assertEquals(10, c.bulkPick(emptyList()).size)
        val maxed = c.copy(target = c.target.copy(bar = ForgeBar(20, 0, null, null)))
        assertEquals(emptyList<Long>(), maxed.bulkPick(emptyList()))
    }

    @Test
    fun `선택은 상한까지 — 넘치면 바꾸지 않는다`() {
        var s = XpSelection(max = 2)
        s = s.toggle(1).toggle(2)
        assertTrue(s.full)
        assertEquals(s, s.toggle(3))
        assertEquals(listOf(2L), s.toggle(1).ids)
        assertEquals(listOf(2L), s.drop(listOf(1L)).ids)
        assertEquals(listOf(1L), XpSelection(listOf(1, 2, 3)).withMax(1).ids)
    }

    @Test
    fun `견적 전 미리보기는 서버 값만 더한다 — 통 안이면 그 값, 차면 레벨업만`() {
        val c = catalog(xp = 1200, need = 3020, item(11, value = 1007), item(12, value = 2000))
        val one = c.preview(listOf(11L))!!
        assertEquals(333, one.gainTenths)
        assertEquals(730, one.afterTenths)
        assertFalse(one.fills)
        val both = c.preview(listOf(11L, 12L))!!
        assertTrue(both.fills)
        assertEquals(1000, both.afterTenths)
        assertNull(c.preview(emptyList()))
    }

    @Test
    fun `아까운 재료의 까닭 — 서버 까닭에 이름 붙인 신발을 더한다`() {
        val c = catalog(xp = 0, need = 2880, item(11, value = 1000, precious = listOf(ForgePrecious.STARS3)), item(12, value = 1000))
        val picks = c.preciousPicked(listOf(11L, 12L), nicknames = mapOf(12L to "첫 마라톤"))
        assertEquals(listOf(11L, 12L), picks.map { it.first.id })
        assertEquals(listOf(ForgePrecious.STARS3), picks[0].second)
        assertEquals(listOf(ForgePrecious.NICKNAMED), picks[1].second)
        assertTrue(c.preciousPicked(listOf(12L)).isEmpty())
        assertEquals(ForgePrecious.OTHER, ForgePrecious.of("SOMETHING_NEW"))
        assertEquals(ForgePrecious.TOP_LEVEL, ForgePrecious.of("TOP_LEVEL"))
    }

    @Test
    fun `앱이 아는 까닭을 붙이면 쓸 수 없는 줄로 내려간다 — 서버 까닭이 먼저`() {
        val c = catalog(xp = 0, need = 2880, item(11, value = 500), item(12, value = 700), item(13, value = 900, block = ForgeMaterialBlock.LISTED))
        val marked = c.withAppBlocks(mapOf(11L to ForgeMaterialBlock.PENDING_RUN, 13L to ForgeMaterialBlock.UNREVEALED))
        assertEquals(listOf(12L, 11L, 13L), marked.items.map { it.id })
        assertEquals(ForgeMaterialBlock.PENDING_RUN, marked.item(11)?.block)
        assertEquals(ForgeMaterialBlock.LISTED, marked.item(13)?.block)
        // 서버 글자로는 앱만 아는 까닭을 받지 않는다
        assertEquals(ForgeMaterialBlock.UNKNOWN, ForgeMaterialBlock.of("PENDING_RUN"))
        assertEquals(ForgeMaterialBlock.IN_ROOM, ForgeMaterialBlock.of("IN_ROOM"))
    }

    // ── 뽑기 결과의 "재료로 쓰면" 줄 ─────────────────────────────

    @Test
    fun `뽑기 결과 줄은 서버 값 — 첫 신발 · 만렙 · 걸린 강화 · 아까운 신발이면 띄우지 않는다`() {
        val equipped = shoe(1, Rarity.RARE, level = 3, origin = "FREE_DRAW", equipped = true)
        val c = catalog(xp = 1200, need = 3020, item(9, value = 1007, tenths = 333), targetId = 1)
        assertEquals(333, drawForgeHintTenths(equipped, 9, c, forgePending = false))
        assertNull(drawForgeHintTenths(equipped, 9, c, forgePending = true))
        assertNull(drawForgeHintTenths(equipped, 9, null, forgePending = false))
        assertNull(drawForgeHintTenths(equipped.copy(server = equipped.server!!.copy(origin = "STARTER")), 9, c, false))
        assertNull(drawForgeHintTenths(equipped, 9, c.copy(target = c.target.copy(bar = ForgeBar(20, 0, null, null))), false))
        assertNull(drawForgeHintTenths(equipped, 9, c, false, nicknames = mapOf(9L to "새 신발")))
        val precious = catalog(xp = 0, need = 3020, item(9, value = 3100, tenths = 1026, precious = listOf(ForgePrecious.HIGHER_GRADE)), targetId = 1)
        assertNull(drawForgeHintTenths(equipped, 9, precious, false))
        val blocked = catalog(xp = 0, need = 3020, item(9, value = 1000, block = ForgeMaterialBlock.LISTED), targetId = 1)
        assertNull(drawForgeHintTenths(equipped, 9, blocked, false))
        assertNull(drawForgeHintTenths(null, 9, c, false))
    }

    @Test
    fun `열어 보지 않은 뽑기 신발 — 같은 계정 · 하루 안 · 번호를 받았으면 그 신발, 아니면 그 뒤 그 종류`() {
        val shoes = listOf(
            shoe(5, Rarity.COMMON, origin = "FREE_DRAW"),
            shoe(21, Rarity.COMMON, origin = "FREE_DRAW"),
            shoe(22, Rarity.RARE, origin = "BONUS_DRAW"),
        )
        val now = 10_000_000L
        val marker = PendingDraw(DrawKind.FREE, newestBefore = 20, leftBefore = 3, startedAt = now - 1_000, account = "a")
        assertEquals(setOf(21L), unrevealedDrawShoes(marker, shoes, "a", now))
        assertEquals(setOf(22L), unrevealedDrawShoes(marker.copy(kind = DrawKind.PREMIUM), shoes, "a", now))
        assertEquals(setOf(5L), unrevealedDrawShoes(marker.copy(shoeId = 5), shoes, "a", now))
        assertEquals(emptySet<Long>(), unrevealedDrawShoes(marker, shoes, "b", now))
        assertEquals(emptySet<Long>(), unrevealedDrawShoes(marker, shoes, "a", now + 2L * 24 * 60 * 60 * 1000))
    }

    @Test
    fun `레벨이 오르면 받는 포인트는 오른 레벨마다 서버 표로`() {
        val rules = ShoeRules.FALLBACK
        val one = (4..4).sumOf { rules.pointsAt("RARE", it) }
        assertEquals(one, xpPointsGain(Rarity.RARE, 3, 4, rules))
        assertEquals((4..6).sumOf { rules.pointsAt("RARE", it) }, xpPointsGain(Rarity.RARE, 3, 6, rules))
        assertEquals(0, xpPointsGain(Rarity.RARE, 3, 3, rules))
        val stats = targetStats(shoe(1, Rarity.RARE, level = 3), level = 4, maxLevel = 20)!!
        assertEquals(4, stats.level)
        assertEquals(20, stats.maxLevel)
        assertEquals(800, stats.efficiencyBps)
    }

    // ── 서버 답 읽기 ───────────────────────────────────────────

    @Test
    fun `재료 목록은 계약 모양 그대로 읽는다 — 값 · % · 별 · 막힌 까닭 · 아까운 까닭 · 대성공`() = runBlocking {
        val repo = ForgeRepository(null, MemoryStore(), xp = FakeXp())
        val load = repo.load(1) as ForgeLoad.XpReady
        val c = load.catalog
        assertEquals(397, c.target.bar.shownTenths)
        assertEquals(3020, c.target.bar.need)
        assertEquals(Rarity.RARE, c.target.rarity)
        assertEquals(20, c.target.maxLevel)
        assertEquals(10, c.maxMaterials)
        assertEquals(XpCrit(0.10, 2.0), c.crit)
        val first = c.items.first()
        assertEquals(9L, first.id)
        assertEquals(1007, first.value)
        assertEquals(333, first.tenths)
        assertEquals(2, first.stars)
        assertNull(first.block)
        val equipped = c.item(10)!!
        assertEquals(ForgeMaterialBlock.EQUIPPED, equipped.block)
        assertFalse(equipped.usable)
        assertEquals(listOf(ForgePrecious.LEVELED, ForgePrecious.TOP_LEVEL), c.item(12)!!.precious)
    }

    @Test
    fun `대상 거절은 막힘으로 · 서버에 함수가 없으면 예전 강화로`() = runBlocking {
        val xp = FakeXp(materialsReply = ServerResult.Ok("""{"error":"RUNNING"}"""))
        assertEquals(ForgeLoad.XpBlocked(ForgeTargetBlock.RUNNING), ForgeRepository(null, MemoryStore(), xp = xp).load(1))
        xp.materialsReply = ServerResult.Ok("""{"error":"MAX_LEVEL"}""")
        assertEquals(ForgeLoad.XpBlocked(ForgeTargetBlock.MAX_LEVEL), ForgeRepository(null, MemoryStore(), xp = xp).load(1))
        // PGRST202 — 예전 forge_materials 로
        xp.materialsReply = ServerResult.Rejected("Could not find the function public.forge_xp_materials(p_target) in the schema cache")
        val old = OldServer()
        val repo = ForgeRepository(old, MemoryStore(), xp = xp)
        assertTrue(repo.load(7) is ForgeLoad.Ready)
        assertFalse(repo.xpAvailable)
        // 한 번 없다고 확인하면 다시 묻지 않는다
        xp.materialsCalls = 0
        repo.load(7)
        assertEquals(0, xp.materialsCalls)
        assertNull(repo.xpCatalog(7))
    }

    @Test
    fun `견적은 최소값 · 에너지 · 상한 앞뒤 · 만렙 넘침을 읽는다`() = runBlocking {
        val repo = ForgeRepository(null, MemoryStore(), xp = FakeXp())
        val q = (repo.xpQuote(1, listOf(9)) as ForgeOutcome.XpQuoted).quote
        assertEquals("md5-a", q.quoteVersion)
        assertEquals(1, q.levels)
        assertEquals(4, q.after.level)
        assertEquals(101, q.after.shownTenths)
        assertEquals(2140, q.gain)
        assertEquals(28, q.energyBefore)
        assertEquals(26, q.energyAfter)
        assertEquals(1.36, q.capBefore!!, 1e-9)
        assertFalse(q.overflowPossible)
        assertTrue(q.covers(listOf(9L)))
    }

    @Test
    fun `견적 거절 코드마다 결말이 따로 있다`() = runBlocking {
        val xp = FakeXp()
        val repo = ForgeRepository(null, MemoryStore(), xp = xp)
        xp.quoteReply = ServerResult.Ok("""{"error":"MATERIAL_UNAVAILABLE","unavailable":[{"id":9,"block":"IN_ROOM"},{"id":10,"block":"EQUIPPED"}]}""")
        assertEquals(
            ForgeOutcome.MaterialsUnavailable(listOf(9L, 10L), mapOf(9L to ForgeMaterialBlock.IN_ROOM, 10L to ForgeMaterialBlock.EQUIPPED)),
            repo.xpQuote(1, listOf(9, 10)),
        )
        xp.quoteReply = ServerResult.Ok("""{"error":"NO_VALUE","ids":[11]}""")
        assertEquals(ForgeOutcome.NoValue(listOf(11L)), repo.xpQuote(1, listOf(11)))
        xp.quoteReply = ServerResult.Ok("""{"error":"MATERIALS_INVALID"}""")
        assertEquals(ForgeOutcome.MaterialsInvalid, repo.xpQuote(1, emptyList()))
        xp.quoteReply = ServerResult.Ok("""{"error":"TARGET_UNAVAILABLE"}""")
        assertEquals(ForgeOutcome.TargetBlocked(ForgeTargetBlock.TARGET_UNAVAILABLE), repo.xpQuote(1, listOf(9)))
        xp.quoteReply = ServerResult.Ok("""{"error":"SOMETHING_NEW"}""")
        assertEquals(ForgeOutcome.Rejected("SOMETHING_NEW"), repo.xpQuote(1, listOf(9)))
        xp.quoteReply = ServerResult.Retry("offline")
        assertEquals(ForgeOutcome.NotSent, repo.xpQuote(1, listOf(9)))
    }

    // ── 넣기 · 걸려 있는 요청 ───────────────────────────────────

    @Test
    fun `넣기는 보내기 전에 종류 · 견적 서명과 함께 요청을 적고, 결과가 오면 지우고 통을 폰에 적는다`() = runBlocking {
        val xp = FakeXp()
        val store = MemoryStore()
        xp.store = store
        var synced = 0
        val settled = mutableListOf<XpResult>()
        val repo = ForgeRepository(null, store, resync = { synced++ }, newKey = { "key-x" }, xp = xp, onXpDone = { settled += it })
        val quote = (repo.xpQuote(1, listOf(9)) as ForgeOutcome.XpQuoted).quote
        val outcome = repo.xpAdd(1, quote) as ForgeOutcome.XpDone
        assertEquals(ForgeKind.XP, xp.savedAtSend?.kind)
        assertEquals("md5-a", xp.savedAtSend?.quoteVersion)
        assertEquals("key-x", xp.addKeys.single())
        assertEquals(1, outcome.result.levels)
        assertEquals(listOf(5), outcome.result.levelBoxes)
        assertEquals(1, outcome.result.crits)
        assertTrue(outcome.result.materials.single().crit)
        assertEquals(135, outcome.result.after.shownTenths)
        assertEquals(427, settled.single().after.xp)
        assertEquals(3140, settled.single().after.need)
        assertNull(store.value)
        assertEquals(1, synced)
    }

    @Test
    fun `보내기 직전 견적이 바뀌었으면 넣지 않는다 — 요청도 적지 않는다`() = runBlocking {
        val xp = FakeXp()
        val repo = ForgeRepository(null, MemoryStore(), xp = xp)
        val quote = (repo.xpQuote(1, listOf(9)) as ForgeOutcome.XpQuoted).quote
        xp.quoteReply = ServerResult.Ok(quoteJson(version = "md5-b"))
        val outcome = repo.xpAdd(1, quote)
        assertTrue(outcome is ForgeOutcome.XpQuoteChanged && outcome.quote?.quoteVersion == "md5-b")
        assertTrue(xp.addKeys.isEmpty())
    }

    @Test
    fun `서버가 넣기 직전 견적이 바뀌었다고 하면 요청을 지운다`() = runBlocking {
        val xp = FakeXp(addReply = ServerResult.Ok("""{"error":"QUOTE_CHANGED","quote":${quoteJson(version = "md5-c")}}"""))
        val store = MemoryStore()
        val repo = ForgeRepository(null, store, xp = xp)
        val quote = (repo.xpQuote(1, listOf(9)) as ForgeOutcome.XpQuoted).quote
        val outcome = repo.xpAdd(1, quote)
        assertTrue(outcome is ForgeOutcome.XpQuoteChanged && outcome.quote?.quoteVersion == "md5-c")
        assertNull(store.value)
    }

    @Test
    fun `응답을 잃으면 결과를 묻고, 기록이 없으면 같은 번호로 한 번 더 보낸다(같은 번호는 한 번만 된다)`() = runBlocking {
        val xp = FakeXp(addReply = ServerResult.Retry("timeout"))
        val store = MemoryStore()
        val repo = ForgeRepository(null, store, newKey = { "key-y" }, xp = xp)
        val quote = (repo.xpQuote(1, listOf(9)) as ForgeOutcome.XpQuoted).quote
        // 결과도 못 읽으면 미확인 — 요청은 남는다
        xp.resultReply = ServerResult.Retry("offline")
        assertEquals(ForgeOutcome.Unknown, repo.xpAdd(1, quote))
        assertEquals("key-y", store.value?.requestKey)
        // 다시 확인: 서버에 기록이 없다(null) → 같은 번호 · 같은 견적으로 다시 보내 넣는다
        xp.resultReply = ServerResult.Ok("null")
        xp.addReply = ServerResult.Ok(addJson())
        val done = repo.check(store.value!!)
        assertTrue(done is ForgeOutcome.XpDone)
        assertEquals(listOf("key-y", "key-y"), xp.addKeys)
        assertEquals(listOf("md5-a", "md5-a"), xp.addQuotes)
        assertNull(store.value)
    }

    @Test
    fun `걸려 있는 요청이 있으면 새로 넣지 않고 그 결과부터 묻는다`() = runBlocking {
        val xp = FakeXp(resultReply = ServerResult.Ok(addJson()))
        val store = MemoryStore()
        store.value = ForgePending("key-old", 1, listOf(9), 0, 1_000, ForgeKind.XP, "md5-a")
        val repo = ForgeRepository(null, store, xp = xp)
        val quote = (repo.xpQuote(1, listOf(9)) as ForgeOutcome.XpQuoted).quote
        assertTrue(repo.xpAdd(1, quote) is ForgeOutcome.XpDone)
        assertEquals(listOf("key-old"), xp.resultKeys)
        assertTrue(xp.addKeys.isEmpty())
    }

    @Test
    fun `1_20 에서 보낸 예전 요청은 예전 결과(성공 · 실패) 그대로 — 경험치 서버가 있어도`() = runBlocking {
        val old = OldServer()
        val xp = FakeXp()
        val store = MemoryStore()
        // 1.20.x 가 적은 여섯 칸 줄
        val raw = "user-a|key-old|7|1,2,3|806|1000"
        store.value = decodePending(raw, "user-a")
        assertEquals(ForgeKind.CHANCE, store.value?.kind)
        val repo = ForgeRepository(old, store, xp = xp)
        val outcome = repo.check(store.value!!)
        assertTrue(outcome is ForgeOutcome.Done && !outcome.result.success)
        assertEquals(listOf("key-old"), old.resultKeys)
        assertTrue(xp.resultKeys.isEmpty())
        assertNull(store.value)
    }

    @Test
    fun `저장 줄 — 경험치 요청은 종류 · 서명까지 여덟 칸, 예전 요청은 예전 여섯 칸 그대로`() {
        val xpPending = ForgePending("k-1", 7, listOf(1, 2, 3, 4), 0, 1_000, ForgeKind.XP, "abc123")
        val line = encodePending(xpPending, "user-a")
        assertEquals("user-a|k-1|7|1,2,3,4|0|1000|XP|abc123", line)
        assertEquals(xpPending, decodePending(line, "user-a"))
        assertNull(decodePending(line, "user-b"))
        val chance = ForgePending("k-2", 7, listOf(1, 2, 3), 806, 2_000)
        assertEquals("user-b|k-2|7|1,2,3|806|2000", encodePending(chance, "user-b"))
        // 서명에 칸 나눔 글자가 섞이면 적지 않는다(다시 보내지 않고 결과만 묻는다)
        assertEquals("", decodePending(encodePending(xpPending.copy(quoteVersion = "a|b"), "u"), "u")?.quoteVersion)
        // 계정마다 한 줄 — 경험치 · 예전 줄이 섞여도
        var all = upsertPending(null, "user-a", xpPending)
        all = upsertPending(all, "user-b", chance)
        assertEquals(xpPending, pendingFor(all, "user-a"))
        assertEquals(chance, pendingFor(all, "user-b"))
        assertNull(decodePending("user-a|k|7|1|0|1000|WHAT|x", "user-a"))
    }

    // ── 흉내 ────────────────────────────────────────────────────

    private class MemoryStore : ForgePendingStore {
        var value: ForgePending? = null
        override suspend fun load(): ForgePending? = value
        override suspend fun save(pending: ForgePending?) {
            value = pending
        }
    }

    private class FakeXp(
        var materialsReply: ServerResult<String> = ServerResult.Ok(MATERIALS),
        var quoteReply: ServerResult<String> = ServerResult.Ok(quoteJson()),
        var addReply: ServerResult<String> = ServerResult.Ok(addJson()),
        var resultReply: ServerResult<String> = ServerResult.Ok("null"),
    ) : ForgeXpServer {
        var materialsCalls = 0
        val addKeys = mutableListOf<String>()
        val addQuotes = mutableListOf<String>()
        val resultKeys = mutableListOf<String>()
        var store: MemoryStore? = null
        var savedAtSend: ForgePending? = null

        override suspend fun forgeXpMaterials(target: Long): ServerResult<String> {
            materialsCalls++
            return materialsReply
        }

        override suspend fun forgeXpQuote(target: Long, materials: List<Long>): ServerResult<String> = quoteReply

        override suspend fun forgeXpAdd(key: String, target: Long, materials: List<Long>, quoteVersion: String): ServerResult<String> {
            addKeys += key
            addQuotes += quoteVersion
            savedAtSend = store?.value
            return addReply
        }

        override suspend fun forgeXpResult(key: String): ServerResult<String> {
            resultKeys += key
            return resultReply
        }
    }

    /** 예전 확률 강화(0054) 서버 — 결과는 실패 */
    private class OldServer : ForgeServer {
        val resultKeys = mutableListOf<String>()
        override suspend fun forgeMaterials(target: Long): ServerResult<String> = ServerResult.Ok(
            """{"target":{"id":7,"rarity":"LEGENDARY","level":10,"max_level":20,"efficiency_bps":800,"comfort_bps":500,"base_permille":700,"block":null},
               "materials":[]}""",
        )
        override suspend fun forgeQuote(target: Long, materials: List<Long>): ServerResult<String> = ServerResult.Retry("x")
        override suspend fun forgeStart(key: String, target: Long, materials: List<Long>, quoteVersion: String): ServerResult<String> =
            ServerResult.Retry("x")
        override suspend fun forgeResult(key: String): ServerResult<String> {
            resultKeys += key
            return ServerResult.Ok(
                """{"status":"FAILED","request_key":"$key","target_id":7,"material_ids":[1,2,3],"rate_permille":806,
                   "level_before":10,"level_after":10,"target":{"level":10,"max_level":20,"efficiency_bps":800,"comfort_bps":500}}""",
            )
        }
    }

    private companion object {
        val MATERIALS = """{ "ok": true, "rule_version": "forge-xp-1",
  "target": {"id": 1, "rarity": "RARE", "level": 3, "max_level": 20, "xp": 1200, "need": 3020, "pct": 39.7},
  "items": [ {"id": 9, "rarity": "COMMON", "level": 1, "model_id": null, "variant": 2,
              "q": 0.512, "stars": 2, "durability_pct": 100.0,
              "own": 1007, "inherit": 0, "repair_part": 0, "value": 1007, "pct": 33.3,
              "block": null, "precious": []},
             {"id": 12, "rarity": "COMMON", "level": 5, "model_id": 1101, "variant": 0,
              "q": 0.4, "stars": 1, "durability_pct": 80.5,
              "own": 900, "inherit": 6030, "repair_part": 100, "value": 6930, "pct": 229.4,
              "block": null, "precious": ["LEVELED", "TOP_LEVEL"]},
             {"id": 10, "rarity": "RARE", "level": 1, "model_id": null, "variant": 1,
              "q": 0.7, "stars": 3, "durability_pct": 100.0,
              "own": 3410, "inherit": 0, "repair_part": 0, "value": 3410, "pct": 112.9,
              "block": "EQUIPPED", "precious": ["STARS3"]} ],
  "crit": {"chance": 0.10, "mult": 2}, "max_materials": 10 }"""

        fun quoteJson(version: String = "md5-a") = """{ "ok": true, "rule_version": "forge-xp-1", "quote_version": "$version",
  "before": {"level": 3, "xp": 1200, "need": 3020, "pct": 39.7},
  "after":  {"level": 4, "xp": 320, "need": 3140, "pct": 10.1, "levels": 1, "overflow": 0},
  "gain": 2140,
  "materials": [{"id": 9, "own": 1007, "inherit": 0, "value": 1007, "pct": 33.3}],
  "crit": {"chance": 0.10, "mult": 2},
  "energy": {"before": 28, "after": 26}, "cap": {"before": 1.36, "after": 1.36},
  "max_level": 20, "overflow_possible": false }"""

        fun addJson() = """{ "ok": true, "kind": "XP", "request_key": "k", "target_id": 1,
  "level_before": 3, "level_after": 4, "levels": 1,
  "gain": 2247, "xp_after": 427, "need_after": 3140, "pct_after": 13.5, "overflow": 0,
  "materials": [{"id": 9, "own": 1007, "inherit": 0, "crit": true, "value": 2014}],
  "crits": 1, "level_boxes": [5] }"""

        fun server(origin: String = "FREE_DRAW") = ServerStats(
            origin = origin, efficiencyBps = 800, comfortBps = 500, durabilityPts = 100.0, maxLevel = 20,
            status = "OWNED", chainState = "APP", canWithdraw = false, upgradeCost = 0.0, repairCostPerPoint = 0.0, genesisNo = 0,
        )

        fun shoe(id: Long, rarity: Rarity, level: Int = 1, origin: String = "FREE_DRAW", equipped: Boolean = false) = Sneaker(
            id = id, faction = Faction.WIND, rarity = rarity, variant = 0, level = level, mintNumber = id.toInt(),
            luck = 1.0, comfort = 1.0, durability = 100, equipped = equipped, acquiredAt = 0L, server = server(origin),
        )

        fun item(
            id: Long,
            value: Int,
            tenths: Int = value * 1000 / 3020,
            block: ForgeMaterialBlock? = null,
            precious: List<ForgePrecious> = emptyList(),
        ) = XpMaterial(
            shoe = shoe(id, Rarity.COMMON), stars = 2, durabilityPct = 100.0, own = value, inherit = 0, repairPart = 0,
            value = value, tenths = tenths, block = block, precious = precious,
        )

        fun catalog(xp: Int, need: Int, vararg items: XpMaterial, max: Int = 10, targetId: Long = 1) = XpCatalog(
            target = XpTarget(targetId, Rarity.RARE, 20, ForgeBar(3, xp, need, ForgeXp.tenths(xp, need))),
            items = items.toList(), crit = XpCrit(0.1, 2.0), maxMaterials = max, ruleVersion = ForgeXp.RULE_VERSION,
        )
    }
}
