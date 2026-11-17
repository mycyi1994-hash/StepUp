package com.stepup.android

import com.stepup.android.core.InviteLinks
import com.stepup.android.data.prefs.RunItemBinding
import com.stepup.android.data.remote.ItemsApi
import com.stepup.android.data.remote.ServerResult
import com.stepup.android.data.repo.ItemsRepository
import com.stepup.android.domain.Bag
import com.stepup.android.domain.BagItem
import com.stepup.android.domain.ItemKind
import com.stepup.android.domain.RewardEvent
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

        fun repo(now: () -> Long = { System.currentTimeMillis() }) = ItemsRepository(
            bagCall = { bagNow?.let { ServerResult.Ok(it) } ?: ServerResult.Retry("no") },
            useCall = { _, _, _ -> ServerResult.Ok(0) },
            bindCall = { at, kinds, _ -> bound += at to kinds; answer },
            addPending = { row -> pending.removeAll { it.startedAt == row.startedAt }; pending += row },
            readPending = { pending.toList() },
            takePending = { at -> pending.removeAll { it.startedAt == at } },
            now = now,
        )
    }

    @Test fun `starting a run binds the picks once and clears them`() = runBlocking {
        val fake = Fake().apply { bagNow = bag }
        val repo = fake.repo()
        repo.refresh()
        repo.toggle(ItemKind.RUN_BOOSTER, now = 10_000L)
        repo.toggle(ItemKind.ENERGY_DRINK, now = 10_000L)
        repo.arm(now = 15_000L)
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
        repo.arm(now = 500L)
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
        repo.arm(now = 0L)
        repo.startRun(startedAt = 11 * 60 * 1000L, owner = "account:u1")
        assertTrue(fake.bound.isEmpty())
        assertTrue(fake.pending.isEmpty())
        assertNotNull(repo.bag.value)
    }

    // 2026-11-17 점검 — 고른 것은 점검에서 "출발"을 누른 그 출발에만 실린다
    @Test fun `picks ride only the start they were armed for`() = runBlocking {
        val fake = Fake().apply { bagNow = bag }
        val repo = fake.repo()
        repo.refresh()
        // 고르기만 하고 출발을 누르지 않았다(점검을 떠남) — 그 뒤의 러닝에 쓰지 않는다
        repo.toggle(ItemKind.RUN_BOOSTER, now = 0L)
        repo.startRun(startedAt = 5_000L, owner = "account:u1")
        assertTrue(fake.bound.isEmpty())
        assertTrue(repo.picks.value.isEmpty())
        // 출발을 눌렀다가 그만뒀다(3-2-1 취소 · 권한 안내 닫기) — 비워져 다음 러닝에 쓰지 않는다
        repo.toggle(ItemKind.RUN_BOOSTER, now = 10_000L)
        repo.arm(now = 11_000L)
        repo.clearPicks()
        repo.startRun(startedAt = 12_000L, owner = "account:u1")
        assertTrue(fake.bound.isEmpty())
        // 점검 없이 켜진 러닝(크루 · 번개 대기실 출발)에는 쓰지 않고 비운다
        repo.toggle(ItemKind.ENERGY_DRINK, now = 20_000L)
        repo.arm(now = 21_000L)
        repo.startRun(startedAt = 22_000L, owner = "account:u1", fromCheck = false)
        assertTrue(fake.bound.isEmpty())
        assertTrue(repo.picks.value.isEmpty())
        // 점검을 오래 열어 두었다가(고른 지 20분) 출발을 누르면 쓰인다 — 시간은 누른 때부터 잰다
        repo.toggle(ItemKind.RUN_BOOSTER, now = 30_000L)
        repo.arm(now = 30_000L + 20 * 60 * 1000L)
        repo.startRun(startedAt = 30_000L + 20 * 60 * 1000L + 4_000L, owner = "account:u1")
        assertEquals(1, fake.bound.size)
    }

    @Test fun `a reward item that cannot raise the multiplier cannot be picked`() = runBlocking {
        val now = 1_000_000L
        // 보상 2배 이벤트가 켜져 있다 — 2배권 · 부스터(×1.5)는 써도 늘지 않는다. 에너지 드링크는 그대로 고른다
        val live = bag.copy(event = RewardEvent("2배", 2.0, endsAtMillis = now + 60_000L))
        assertEquals(2.0, live.activeRewardMult(now), 0.0)
        assertTrue(RunItemPicks.covered(ItemKind.REWARD_DOUBLE, live, now))
        assertTrue(RunItemPicks.covered(ItemKind.RUN_BOOSTER, live, now))
        assertFalse(RunItemPicks.covered(ItemKind.ENERGY_DRINK, live, now))
        // 이벤트가 끝났으면 다시 고를 수 있다
        assertFalse(RunItemPicks.covered(ItemKind.REWARD_DOUBLE, live, now + 61_000L))
        // 이벤트가 ×1.5 면 2배권은 더 크니 쓸모가 있다(부스터 ×1.5 는 같아서 쓸모없다)
        val small = bag.copy(event = RewardEvent("1.5배", 1.5, endsAtMillis = now + 60_000L))
        assertFalse(RunItemPicks.covered(ItemKind.REWARD_DOUBLE, small, now))
        assertTrue(RunItemPicks.covered(ItemKind.RUN_BOOSTER, small, now))
        // 24시간 부스터(×2)도 같다
        val boosted = bag.copy(boostUntilMillis = now + 60_000L)
        assertTrue(RunItemPicks.covered(ItemKind.REWARD_DOUBLE, boosted, now))
        val fake = Fake().apply { bagNow = live }
        val repo = fake.repo()
        repo.refresh()
        repo.toggle(ItemKind.REWARD_DOUBLE, now = now)
        repo.toggle(ItemKind.ENERGY_DRINK, now = now)
        assertEquals(setOf(ItemKind.ENERGY_DRINK), repo.picks.value)
    }

    @Test fun `a fast phone clock keeps the binding for later`() = runBlocking {
        val fake = Fake().apply { bagNow = bag }
        var clock = 1_000_000L
        val repo = fake.repo(now = { clock })
        repo.refresh()
        // 폰 시계가 앞서 있어 서버가 시작 시각을 받지 않는다 — 버리지 않고 러닝보다 먼저 다시 보낸다
        fake.pending += RunItemBinding(clock, "account:u1", listOf("RUN_BOOSTER"))
        fake.answer = ServerResult.Rejected("러닝 시작 시각이 올바르지 않습니다")
        assertFalse(repo.flush("account:u1"))
        assertEquals(1, fake.pending.size)
        fake.answer = ServerResult.Ok(Unit)
        assertTrue(repo.flush("account:u1"))
        assertTrue(fake.pending.isEmpty())
        // 하루 넘게 지난 줄은 시계 탓이 아니다 — 버린다
        fake.pending += RunItemBinding(clock - 2 * 24 * 60 * 60 * 1000L, "account:u1", listOf("RUN_BOOSTER"))
        fake.answer = ServerResult.Rejected("러닝 시작 시각이 올바르지 않습니다")
        assertTrue(repo.flush("account:u1"))
        assertTrue(fake.pending.isEmpty())
    }

    @Test fun `logout clears the bag and the picks`() = runBlocking {
        val fake = Fake().apply { bagNow = bag }
        val repo = fake.repo()
        repo.refresh()
        repo.toggle(ItemKind.RUN_BOOSTER, now = 0L)
        repo.clear()
        assertTrue(repo.picks.value.isEmpty())
        assertEquals(null, repo.bag.value)
    }
}
