package com.stepup.android

import com.stepup.android.core.InviteLinks
import com.stepup.android.data.prefs.RunItemBinding
import com.stepup.android.data.remote.ItemsApi
import com.stepup.android.data.remote.ServerResult
import com.stepup.android.data.repo.ItemsRepository
import com.stepup.android.domain.Bag
import com.stepup.android.domain.BagItem
import com.stepup.android.domain.ItemKind
import com.stepup.android.domain.RunItemPicks
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/** 아이템(서버 0106) — 출발 전 고르기 규칙 · 가방 읽기 · 러닝에 묶기 · 푸시 링크 */
class ItemsTest {

    private val bag = Bag(
        items = listOf(
            BagItem(ItemKind.RUN_BOOSTER, 1.5, 2),
            BagItem(ItemKind.REWARD_DOUBLE, 2.0, 1),
            BagItem(ItemKind.ENERGY_DRINK, 0.5, 1),
            BagItem(ItemKind.DURABILITY_RESET, null, 0),
            BagItem(ItemKind.ENERGY_RESET, null, 3),
        ),
        event = null,
        boostUntilMillis = null,
    )

    @Test fun `same group is exclusive - booster and double never together`() {
        var picked = RunItemPicks.toggle(emptySet(), ItemKind.RUN_BOOSTER)
        picked = RunItemPicks.toggle(picked, ItemKind.ENERGY_DRINK)
        assertEquals(setOf(ItemKind.RUN_BOOSTER, ItemKind.ENERGY_DRINK), picked)
        picked = RunItemPicks.toggle(picked, ItemKind.REWARD_DOUBLE)
        assertEquals(setOf(ItemKind.REWARD_DOUBLE, ItemKind.ENERGY_DRINK), picked)
        picked = RunItemPicks.toggle(picked, ItemKind.REWARD_DOUBLE)
        assertEquals(setOf(ItemKind.ENERGY_DRINK), picked)
        // 바로 쓰는 아이템은 러닝에 고르지 않는다
        assertEquals(picked, RunItemPicks.toggle(picked, ItemKind.DURABILITY_RESET))
    }

    @Test fun `picks need an owned item and show the server values`() {
        val picked = setOf(ItemKind.REWARD_DOUBLE, ItemKind.ENERGY_DRINK)
        assertEquals(2.0, RunItemPicks.rewardMult(picked, bag), 0.0)
        assertEquals(0.5, RunItemPicks.energyFactor(picked, bag), 0.0)
        assertEquals(1.0, RunItemPicks.rewardMult(emptySet(), bag), 0.0)
        val none = bag.copy(items = bag.items.map { if (it.kind == ItemKind.REWARD_DOUBLE) it.copy(qty = 0) else it })
        assertEquals(setOf(ItemKind.ENERGY_DRINK), RunItemPicks.keepOwned(picked, none))
        assertEquals(listOf(ItemKind.RUN_BOOSTER, ItemKind.REWARD_DOUBLE, ItemKind.ENERGY_DRINK), bag.runPicks.map { it.kind })
        assertEquals(7, bag.total)
    }

    @Test fun `bag json from items_me - unknown kinds are dropped, event and boost times parsed`() {
        val body = """{"items":[{"kind":"RUN_BOOSTER","group":"REWARD","mode":"RUN","value":1.5,"qty":2},
            {"kind":"FUTURE_THING","group":"X","mode":"NOW","value":null,"qty":9},
            {"kind":"ENERGY_RESET","group":"DAILY","mode":"NOW","value":null,"qty":0}],
            "event":{"id":3,"title":"주말 2배","mult":2,"starts_at":"2026-11-17T06:00:00+00:00","ends_at":"2026-11-18T06:35:00.123456+00:00"},
            "boost_until":null}"""
        val parsed = requireNotNull(ItemsApi.parseBag(body))
        assertEquals(listOf(ItemKind.RUN_BOOSTER, ItemKind.ENERGY_RESET), parsed.items.map { it.kind })
        assertEquals(2, parsed.qty(ItemKind.RUN_BOOSTER))
        assertEquals(2.0, parsed.event!!.mult, 0.0)
        assertEquals(java.time.Instant.parse("2026-11-18T06:35:00.123Z").toEpochMilli(), parsed.event!!.endsAtMillis)
        assertNull(parsed.boostUntilMillis)
        assertNull(requireNotNull(ItemsApi.parseBag("""{"items":[],"event":null}""")).event)
    }

    @Test fun `push links open the items tab and the run tab`() {
        assertEquals(InviteLinks.PushTarget.Bag, InviteLinks.pushTargetOf("bag"))
        assertEquals(InviteLinks.PushTarget.RunTab, InviteLinks.pushTargetOf("run"))
        assertNull(InviteLinks.pushTargetOf("items"))
    }

    @Test fun `pending run items round trip and ignore bad lines`() {
        val rows = listOf(
            RunItemBinding(1_000L, "account:abc", listOf("ENERGY_DRINK", "RUN_BOOSTER")),
            RunItemBinding(2_000L, "legacy", listOf("REWARD_DOUBLE")),
        )
        assertEquals(rows, RunItemBinding.decodeAll(RunItemBinding.encodeAll(rows)))
        assertEquals(emptyList<RunItemBinding>(), RunItemBinding.decodeAll("x\ty\tz\n3000\tacc\tbad kind;"))
    }

    private class Fake {
        val pending = mutableListOf<RunItemBinding>()
        val bound = mutableListOf<Pair<Long, List<String>>>()
        var answer: ServerResult<Unit> = ServerResult.Ok(Unit)
        var bagNow: Bag? = null

        fun repo() = ItemsRepository(
            bagCall = { bagNow?.let { ServerResult.Ok(it) } ?: ServerResult.Retry("no") },
            useCall = { _, _, _ -> ServerResult.Ok(0) },
            bindCall = { at, kinds, _ -> bound += at to kinds; answer },
            addPending = { row -> pending.removeAll { it.startedAt == row.startedAt }; pending += row },
            readPending = { pending.toList() },
            takePending = { at -> pending.removeAll { it.startedAt == at } },
        )
    }

    @Test fun `starting a run binds the picks once and clears them`() = runBlocking {
        val fake = Fake().apply { bagNow = bag }
        val repo = fake.repo()
        repo.refresh()
        repo.toggle(ItemKind.RUN_BOOSTER, now = 10_000L)
        repo.toggle(ItemKind.ENERGY_DRINK, now = 10_000L)
        repo.startRun(startedAt = 20_000L, owner = "account:u1")
        assertEquals(listOf(20_000L to listOf("ENERGY_DRINK", "RUN_BOOSTER")), fake.bound)
        assertTrue(fake.pending.isEmpty())
        assertTrue(repo.picks.value.isEmpty())
        // 고른 것이 없으면 다음 러닝에는 아무것도 묶지 않는다
        repo.startRun(startedAt = 30_000L, owner = "account:u1")
        assertEquals(1, fake.bound.size)
    }

    @Test fun `no signal keeps the binding for the upload worker, other accounts wait`() = runBlocking {
        val fake = Fake().apply { bagNow = bag; answer = ServerResult.Retry("offline") }
        val repo = fake.repo()
        repo.refresh()
        repo.toggle(ItemKind.REWARD_DOUBLE, now = 0L)
        repo.startRun(startedAt = 1_000L, owner = "account:u1")
        assertEquals(1, fake.pending.size)
        assertFalse(repo.flush("account:u1"))
        // 다른 계정으로 로그인해 있으면 보내지 않고 남긴다(러닝 올리기도 막지 않는다)
        assertTrue(repo.flush("account:someone-else"))
        assertEquals(1, fake.pending.size)
        fake.answer = ServerResult.Ok(Unit)
        assertTrue(repo.flush("account:u1"))
        assertTrue(fake.pending.isEmpty())
    }

    @Test fun `server refusal drops the binding, missing server function keeps it`() = runBlocking {
        val fake = Fake().apply { bagNow = bag }
        val repo = fake.repo()
        fake.pending += RunItemBinding(5L, "account:u1", listOf("RUN_BOOSTER"))
        fake.answer = ServerResult.Rejected("Could not find the function public.run_items_use")
        assertTrue(repo.flush("account:u1"))
        assertEquals(1, fake.pending.size)
        fake.answer = ServerResult.Rejected("아이템이 없습니다: RUN_BOOSTER")
        assertTrue(repo.flush("account:u1"))
        assertTrue(fake.pending.isEmpty())
    }

    @Test fun `stale picks are not spent on a much later run`() = runBlocking {
        val fake = Fake().apply { bagNow = bag }
        val repo = fake.repo()
        repo.refresh()
        repo.toggle(ItemKind.RUN_BOOSTER, now = 0L)
        repo.startRun(startedAt = 11 * 60 * 1000L, owner = "account:u1")
        assertTrue(fake.bound.isEmpty())
        assertTrue(fake.pending.isEmpty())
        assertNotNull(repo.bag.value)
    }
}
