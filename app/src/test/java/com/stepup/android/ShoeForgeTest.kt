package com.stepup.android

import com.stepup.android.data.remote.ServerResult
import com.stepup.android.data.repo.ForgeLoad
import com.stepup.android.data.repo.ForgeOutcome
import com.stepup.android.data.repo.ForgePendingStore
import com.stepup.android.data.repo.ForgeRepository
import com.stepup.android.data.repo.ForgeServer
import com.stepup.android.data.repo.decodePending
import com.stepup.android.data.repo.encodePending
import com.stepup.android.domain.ForgeMaterialBlock
import com.stepup.android.domain.ForgePending
import com.stepup.android.domain.ForgeQuote
import com.stepup.android.domain.ForgeStats
import com.stepup.android.domain.ForgeTargetBlock
import com.stepup.android.domain.MaterialSelection
import com.stepup.android.domain.Rarity
import com.stepup.android.domain.ShoeForge
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * 신발 강화(지시서 v6) — 확률 식 · 재료 선택 · 요청 키 처리. 판정은 서버(0054)가 하고 같은 식을 supabase/tests 가 확인한다.
 */
class ShoeForgeTest {

    // ── 확률 ─────────────────────────────────────────────────────

    @Test
    fun `지시서 예시 — 레전더리 Lv10 에 에픽 Lv10 · 레어 Lv10 · 에픽 Lv1 이면 80_4퍼센트`() {
        val base = ShoeForge.basePermille(10)
        assertEquals(700, base)
        val bonuses = listOf(
            ShoeForge.bonusPermille(Rarity.LEGENDARY, Rarity.EPIC, 10),
            ShoeForge.bonusPermille(Rarity.LEGENDARY, Rarity.RARE, 10),
            ShoeForge.bonusPermille(Rarity.LEGENDARY, Rarity.EPIC, 1),
        )
        assertEquals(listOf(56, 28, 20), bonuses)
        assertEquals(804, ShoeForge.ratePermille(base, bonuses))
        assertEquals("80.4%", ShoeForge.formatRate(804))
        assertEquals(784, ShoeForge.ratePermille(base, bonuses.take(2)))
        assertEquals("+5.6%p", ShoeForge.formatBonus(56))
    }

    @Test
    fun `세 단계 아래는 4분의 1 · 같거나 높은 등급 · 범위 밖 레벨은 0`() {
        assertEquals(5, ShoeForge.bonusPermille(Rarity.LEGENDARY, Rarity.COMMON, 1))
        assertEquals(14, ShoeForge.bonusPermille(Rarity.LEGENDARY, Rarity.COMMON, 10))
        assertEquals(0, ShoeForge.bonusPermille(Rarity.EPIC, Rarity.EPIC, 5))
        assertEquals(0, ShoeForge.bonusPermille(Rarity.RARE, Rarity.EPIC, 5))
        assertEquals(0, ShoeForge.bonusPermille(Rarity.EPIC, Rarity.RARE, 0))
        assertEquals(0, ShoeForge.bonusPermille(Rarity.EPIC, Rarity.RARE, 21))
    }

    @Test
    fun `최종 확률은 100퍼센트를 넘지 않는다`() {
        assertEquals(1000, ShoeForge.ratePermille(ShoeForge.basePermille(1), listOf(96, 96, 96)))
        assertEquals(430, ShoeForge.basePermille(19))
        assertEquals("100.0%", ShoeForge.formatRate(1000))
    }

    // ── 재료 선택 ─────────────────────────────────────────────────

    @Test
    fun `넷째를 누르면 바뀌지 않는다 — 몰래 바꾸지 않는다`() {
        val s = MaterialSelection().openPicker().toggle(1).toggle(2).toggle(3)
        assertEquals(listOf(1L, 2L, 3L), s.draft)
        assertEquals(s, s.toggle(4))
        assertEquals(listOf(1L, 3L), s.toggle(2).draft)
    }

    @Test
    fun `선택창 변경은 넣기 전에는 메인 슬롯에 닿지 않고 닫으면 버린다`() {
        val applied = MaterialSelection().openPicker().toggle(1).apply()
        assertEquals(listOf(1L), applied.applied)
        val editing = applied.openPicker().toggle(2).toggle(1)
        assertEquals(listOf(1L), editing.applied)
        val cancelled = editing.cancel()
        assertEquals(listOf(1L), cancelled.applied)
        assertNull(cancelled.draft)
        assertEquals(listOf(2L), editing.apply().applied)
        // 0개는 넣지 않는다
        assertEquals(listOf(1L), applied.openPicker().toggle(1).apply().applied)
    }

    @Test
    fun `쓸 수 없다고 확인된 것만 뺀다`() {
        val s = MaterialSelection(applied = listOf(1, 2, 3))
        assertEquals(listOf(1L, 3L), s.drop(listOf(2L)).applied)
        assertEquals(listOf(1L, 2L), s.remove(3).applied)
    }

    // ── 요청 키 ───────────────────────────────────────────────────

    @Test
    fun `저장한 요청은 같은 계정에서만 다시 읽힌다`() {
        val p = ForgePending("k-1", 7, listOf(1, 2, 3), 804, 1_000)
        val raw = encodePending(p, "user-a")
        assertEquals(p, decodePending(raw, "user-a"))
        assertNull(decodePending(raw, "user-b"))
        assertNull(decodePending("broken", "user-a"))
    }

    @Test
    fun `보내기 전에 요청 키를 저장하고 결과가 확정되면 지운다`() = runBlocking {
        val server = FakeServer(startReply = ServerResult.Ok(resultJson("SUCCESS")))
        val store = MemoryStore()
        var synced = 0
        server.store = store
        val repo = ForgeRepository(server, store, resync = { synced++ }, newKey = { "key-1" })
        val outcome = repo.start(7, QUOTE)
        assertTrue(outcome is ForgeOutcome.Done && outcome.result.success)
        assertEquals("key-1", server.savedAtSend?.requestKey)
        assertNull(store.value)
        assertEquals(1, synced)
    }

    @Test
    fun `보낸 뒤 응답을 못 받으면 같은 키로만 묻고 그래도 모르면 미확인으로 남긴다`() = runBlocking {
        val server = FakeServer(startReply = ServerResult.Retry("timeout"), resultReply = ServerResult.Retry("offline"))
        val store = MemoryStore()
        val repo = ForgeRepository(server, store, newKey = { "key-2" })
        assertEquals(ForgeOutcome.Unknown, repo.start(7, QUOTE))
        assertEquals(listOf("key-2"), server.resultKeys)
        assertEquals("key-2", store.value?.requestKey)
        // 다시 눌러도 새 요청을 만들지 않고 같은 키의 결과만 묻는다
        server.resultReply = ServerResult.Ok(resultJson("FAILED"))
        val again = repo.start(7, QUOTE)
        assertTrue(again is ForgeOutcome.Done && !again.result.success)
        assertEquals(1, server.starts)
        assertEquals(listOf("key-2", "key-2"), server.resultKeys)
        assertNull(store.value)
    }

    @Test
    fun `서버가 받지 않았다고 확정하면 미보냄 — 재료는 그대로`() = runBlocking {
        val server = FakeServer(startReply = ServerResult.Retry("timeout"),
            resultReply = ServerResult.Ok("""{"status":"NOT_ACCEPTED"}"""))
        val store = MemoryStore()
        val repo = ForgeRepository(server, store)
        assertEquals(ForgeOutcome.NotAccepted, repo.start(7, QUOTE))
        assertNull(store.value)
    }

    @Test
    fun `보내기 전 확인에서 끊기면 아무것도 보내지 않았다`() = runBlocking {
        val server = FakeServer(quoteReply = ServerResult.Retry("offline"))
        val store = MemoryStore()
        val repo = ForgeRepository(server, store)
        assertEquals(ForgeOutcome.NotSent, repo.start(7, QUOTE))
        assertEquals(0, server.starts)
        assertNull(store.value)
    }

    @Test
    fun `견적이 달라졌으면 실행하지 않고 새 견적을 돌려준다`() = runBlocking {
        val server = FakeServer(quoteReply = ServerResult.Ok(quoteJson(version = "v2", rate = 790)))
        val repo = ForgeRepository(server, MemoryStore())
        val outcome = repo.start(7, QUOTE)
        assertTrue(outcome is ForgeOutcome.QuoteChanged && outcome.quote.ratePermille == 790)
        assertEquals(0, server.starts)
    }

    @Test
    fun `쓸 수 없는 재료 · 대상 막힘을 읽는다`() = runBlocking {
        val server = FakeServer(quoteReply = ServerResult.Ok("""{"error":"MATERIAL_UNAVAILABLE","unavailable":[2]}"""))
        val repo = ForgeRepository(server, MemoryStore())
        assertEquals(ForgeOutcome.MaterialsUnavailable(listOf(2L)), repo.quote(7, listOf(1, 2, 3)))
        server.quoteReply = ServerResult.Ok("""{"error":"MAX_LEVEL"}""")
        assertEquals(ForgeOutcome.TargetBlocked(ForgeTargetBlock.MAX_LEVEL), repo.quote(7, listOf(1, 2, 3)))
    }

    @Test
    fun `후보 목록은 소유 id 와 막힌 까닭을 그대로 읽는다`() = runBlocking {
        val server = FakeServer()
        val load = ForgeRepository(server, MemoryStore()).load(7)
        load as ForgeLoad.Ready
        assertEquals(700, load.target.basePermille)
        assertEquals(listOf(11L, 12L), load.materials.map { it.id })
        assertEquals(1207, load.materials[0].shoe.mintNumber)
        assertEquals(ForgeMaterialBlock.EQUIPPED, load.materials[1].block)
        assertTrue(load.materials[0].usable)
    }

    private class MemoryStore : ForgePendingStore {
        var value: ForgePending? = null
        override suspend fun load(): ForgePending? = value
        override suspend fun save(pending: ForgePending?) {
            value = pending
        }
    }

    private class FakeServer(
        var quoteReply: ServerResult<String> = ServerResult.Ok(quoteJson()),
        var startReply: ServerResult<String> = ServerResult.Ok(resultJson("SUCCESS")),
        var resultReply: ServerResult<String> = ServerResult.Retry("offline"),
    ) : ForgeServer {
        var starts = 0
        val resultKeys = mutableListOf<String>()
        var savedAtSend: ForgePending? = null
        var store: MemoryStore? = null

        override suspend fun forgeMaterials(target: Long): ServerResult<String> = ServerResult.Ok(
            """{"target":{"id":7,"rarity":"LEGENDARY","level":10,"max_level":20,"efficiency_bps":800,"comfort_bps":500,"base_permille":700,"block":null},
               "materials":[{"id":11,"rarity":"EPIC","level":10,"model_id":1201,"faction":"FIRE","variant":0,"mint_number":1207,"bonus_permille":56,"block":null},
                            {"id":12,"rarity":"RARE","level":3,"model_id":null,"faction":"WATER","variant":1,"mint_number":88,"bonus_permille":14,"block":"EQUIPPED"}]}""",
        )

        override suspend fun forgeQuote(target: Long, materials: List<Long>): ServerResult<String> = quoteReply

        override suspend fun forgeStart(key: String, target: Long, materials: List<Long>, quoteVersion: String): ServerResult<String> {
            starts++
            savedAtSend = store?.value
            return startReply
        }

        override suspend fun forgeResult(key: String): ServerResult<String> {
            resultKeys += key
            return resultReply
        }
    }

    private companion object {
        val QUOTE = ForgeQuote(804, ForgeStats(10, 20, 800, 500), ForgeStats(11, 20, 850, 520), "v1", listOf(1, 2, 3))

        fun quoteJson(version: String = "v1", rate: Int = 804) =
            """{"rate_permille":$rate,"base_permille":700,"materials":[{"id":1},{"id":2},{"id":3}],
               "before":{"level":10,"max_level":20,"efficiency_bps":800,"comfort_bps":500},
               "after":{"level":11,"max_level":20,"efficiency_bps":850,"comfort_bps":520},
               "rule_version":"forge-v1","quote_version":"$version"}"""

        fun resultJson(status: String) =
            """{"status":"$status","request_key":"k","target_id":7,"material_ids":[1,2,3],"rate_permille":804,
               "level_before":10,"level_after":${if (status == "SUCCESS") 11 else 10},
               "target":{"level":${if (status == "SUCCESS") 11 else 10},"max_level":20,"efficiency_bps":850,"comfort_bps":520}}"""
    }
}
