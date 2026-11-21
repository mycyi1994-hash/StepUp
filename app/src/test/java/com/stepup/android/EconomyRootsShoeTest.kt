package com.stepup.android

import com.stepup.android.data.remote.AuthSession
import com.stepup.android.data.remote.AuthSessionStore
import com.stepup.android.data.remote.AuthUser
import com.stepup.android.data.remote.HttpPoster
import com.stepup.android.data.remote.HttpResponse
import com.stepup.android.data.remote.PointsPreviewResult
import com.stepup.android.data.remote.ServerResult
import com.stepup.android.data.remote.SessionHolder
import com.stepup.android.data.remote.ShoePointsPreviewApi
import com.stepup.android.data.remote.StepUpServer
import com.stepup.android.data.remote.SupabaseAuth
import com.stepup.android.domain.Faction
import com.stepup.android.domain.PointsPreview
import com.stepup.android.domain.PointsSplit
import com.stepup.android.domain.Rarity
import com.stepup.android.domain.ServerStats
import com.stepup.android.domain.ShoeRules
import com.stepup.android.domain.Sneaker
import com.stepup.android.domain.StatPoints
import com.stepup.android.domain.StatRule
import com.stepup.android.ui.screens.customize.statBars
import com.stepup.android.ui.screens.items.ForgeReward
import com.stepup.android.ui.screens.items.PointStat
import com.stepup.android.ui.screens.items.PointWorth
import com.stepup.android.ui.screens.items.PointsDraft
import com.stepup.android.ui.screens.items.PointsPhase
import com.stepup.android.ui.screens.items.accepts
import com.stepup.android.ui.screens.items.add
import com.stepup.android.ui.screens.items.energyPerLevel
import com.stepup.android.ui.screens.items.fitted
import com.stepup.android.ui.screens.items.forgeRewards
import com.stepup.android.ui.screens.items.formatApproxSup
import com.stepup.android.ui.screens.items.formatEnergy
import com.stepup.android.ui.screens.items.formatMinutes
import com.stepup.android.ui.screens.items.lifeMinutes
import com.stepup.android.ui.screens.items.minutesPerPointText
import com.stepup.android.ui.screens.items.newStatMeaning
import com.stepup.android.ui.screens.items.pointWorthOf
import com.stepup.android.ui.screens.items.pointsBasisOf
import com.stepup.android.ui.screens.items.recommend
import com.stepup.android.ui.screens.items.recommendAll
import com.stepup.android.ui.screens.items.recommended
import com.stepup.android.ui.screens.items.recommendedAll
import com.stepup.android.ui.screens.items.repairShareOf
import com.stepup.android.ui.screens.items.respecEarned
import com.stepup.android.ui.screens.items.respecEveryOf
import com.stepup.android.ui.screens.items.review
import com.stepup.android.ui.screens.items.startPoints
import com.stepup.android.ui.screens.items.statMeaningRes
import com.stepup.android.ui.screens.items.wholeMinutes
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * 경제의 뿌리 고치기 — 신발 화면(서버 0119, docs/economy-roots-contract-2026-11-21.md).
 * 포인트 미리보기 읽기 · 추천대로 채우기 · 효율 포인트만큼 짧아지는 수명(분) · 레벨당 에너지가 0 이면 에너지 줄 없음 · 5레벨마다 다시 나누기.
 */
class EconomyRootsShoeTest {

    /** 서버가 0119 의 능력치 규칙 묶음을 보냈다 */
    private val roots = ShoeRules.FALLBACK.copy(stats = StatRule.FALLBACK.copy(fromServer = true))

    /** 0119 전 서버 — 묶음이 없다 */
    private val legacy = ShoeRules.FALLBACK.copy(stats = StatRule.LEGACY)

    // ── 미리보기 읽기 ────────────────────────────────────────────

    @Test fun `미리보기 — 계약서의 예를 그대로 읽는다`() {
        val p = PointsPreview.parse(SAMPLE)!!
        assertEquals(PointsPreview.Source.MINE, p.source)
        assertEquals(7, p.runs)
        assertEquals(6, p.days)
        assertEquals(42, p.avgMin)
        assertEquals(77, p.total)
        assertEquals(12, p.free)
        assertTrue(p.respec)
        assertEquals(PointsSplit(40, 5, 20, dayNet = 151.2, lifeMin = 38.5), p.now)
        assertEquals(3, p.wornRuns)
        assertEquals(0, p.energyOutDays)
        assertEquals(PointsPreview.PerPoint(0.42, 0.0, 1.31), p.perPoint)
        assertFalse(p.comfortCapped)
        // 추천은 더할 양, 처음부터 나눈 추천은 절대값
        assertEquals(PointsSplit(2, 0, 10, dayNet = 166.0, lifeMin = 43.1), p.recommend)
        assertEquals(PointsSplit(22, 6, 49, dayNet = 171.3, lifeMin = 54.0), p.recommendAll)
        assertEquals(12, p.recommend!!.sum)
        // 서버가 본 값이 폰 사본과 같을 때만 쓴다
        assertTrue(p.matches(total = 77, efficiency = 40, comfort = 5, durability = 20))
        assertFalse(p.matches(total = 77, efficiency = 41, comfort = 5, durability = 20))
        assertFalse(p.matches(total = 80, efficiency = 40, comfort = 5, durability = 20))
    }

    @Test fun `미리보기 — 빠진 칸은 비우고 읽는다, 기록이 없으면 기본 러닝`() {
        val bare = PointsPreview.parse("""{"ok":true}""")!!
        assertNull(bare.source)
        assertNull(bare.total)
        assertNull(bare.now)
        assertNull(bare.recommend)
        assertNull(bare.recommendAll)
        assertEquals(PointsPreview.PerPoint(null, null, null), bare.perPoint)
        assertFalse(bare.respec)
        // 서버가 안 보낸 칸은 묻지 않는다(맞대어 볼 것이 없다)
        assertTrue(bare.matches(10, 1, 2, 3))

        val fresh = PointsPreview.parse(
            """{"ok":true,"basis":{"source":"DEFAULT","days":0,"runs":0,"avg_min":30},"total":3,"free":3,"respec":false,
               "now":{"efficiency":0,"comfort":0,"durability":0,"day_net":41.5,"life_min":31},
               "per_point":{"efficiency":0.4,"durability":0},"comfort_capped":true,
               "recommend":{"efficiency":3,"comfort":0,"durability":0},"recommend_all":null}""",
        )!!
        assertEquals(PointsPreview.Source.DEFAULT, fresh.source)
        assertEquals(30, fresh.avgMin)
        assertNull(fresh.perPoint.comfort)
        assertEquals(0.0, fresh.perPoint.durability!!, 0.0)
        assertTrue(fresh.comfortCapped)
        assertEquals(PointsSplit(3, 0, 0), fresh.recommend)
        assertNull(fresh.recommendAll)
        // 숫자가 글자 · 소수로 와도, 음수가 와도 읽는다(음수는 0)
        val odd = PointsPreview.parse(
            """{"ok":true,"total":"11","now":{"efficiency":2.0,"comfort":0,"durability":1},
               "per_point":{"efficiency":"0.25","comfort":-3,"durability":1.5},
               "recommend":{"efficiency":-1,"comfort":2,"durability":6}}""",
        )!!
        assertEquals(11, odd.total)
        assertEquals(PointsSplit(2, 0, 1), odd.now)
        assertEquals(PointsPreview.PerPoint(0.25, 0.0, 1.5), odd.perPoint)
        assertEquals(PointsSplit(0, 2, 6), odd.recommend)
        // 세 칸 중 하나라도 빠진 추천은 쓰지 않는다
        assertNull(PointsPreview.parse("""{"ok":true,"recommend":{"efficiency":2,"durability":6}}""")!!.recommend)
    }

    @Test fun `미리보기 — ok 가 아니거나 읽을 수 없으면 없다`() {
        assertNull(PointsPreview.parse("""{"ok":false,"code":"NO_POINTS"}"""))
        assertNull(PointsPreview.parse("""{"code":"NO_POINTS"}"""))
        assertNull(PointsPreview.parse("null"))
        assertNull(PointsPreview.parse("[]"))
        assertNull(PointsPreview.parse(""))
        assertNull(PointsPreview.parse("not json"))
    }

    @Test fun `서버의 대답 — 함수가 없으면 Missing, ok 가 아니면 None, 끊기면 Failed`() {
        assertTrue(ShoePointsPreviewApi.resultOf(ServerResult.Ok(SAMPLE)) is PointsPreviewResult.Ok)
        assertEquals(PointsPreviewResult.None, ShoePointsPreviewApi.resultOf(ServerResult.Ok("""{"ok":false,"code":"NO_POINTS"}""")))
        assertEquals(
            PointsPreviewResult.Missing,
            ShoePointsPreviewApi.resultOf(
                ServerResult.Rejected("Could not find the function public.sneaker_points_preview(p_id) in the schema cache"),
            ),
        )
        assertEquals(PointsPreviewResult.None, ShoePointsPreviewApi.resultOf(ServerResult.Rejected("로그인이 필요합니다")))
        assertEquals(PointsPreviewResult.Failed, ShoePointsPreviewApi.resultOf(ServerResult.Retry("offline")))
        assertEquals(PointsPreviewResult.Failed, ShoePointsPreviewApi.resultOf(ServerResult.SignInRequired("sign in")))
        assertTrue(ShoePointsPreviewApi.missingFunction("PGRST202"))
        assertFalse(ShoePointsPreviewApi.missingFunction("내 신발이 아닙니다"))
    }

    @Test fun `미리보기 요청 — 신발 번호를 보내고, 0119 전 서버의 404 는 Missing`() = runBlocking {
        val ok = Http { HttpResponse(200, SAMPLE) }
        val result = api(ok).preview(42)
        assertEquals(40, (result as PointsPreviewResult.Ok).preview.now!!.efficiency)
        assertEquals("https://test.supabase.co/rest/v1/rpc/sneaker_points_preview" to """{"p_id":42}""", ok.calls.single())

        val missing = Http {
            HttpResponse(
                404,
                """{"code":"PGRST202","details":"Searched for the function public.sneaker_points_preview","hint":null,
                   "message":"Could not find the function public.sneaker_points_preview(p_id) in the schema cache"}""",
            )
        }
        assertEquals(PointsPreviewResult.Missing, api(missing).preview(42))
        assertEquals(PointsPreviewResult.Failed, api(Http { HttpResponse(503, "") }).preview(42))
        assertEquals(PointsPreviewResult.Failed, api(Http { HttpResponse(200, SAMPLE) }, signedIn = false).preview(42))
    }

    // ── 포인트 1 의 값 ───────────────────────────────────────────

    @Test fun `포인트 1 의 값 — 늘면 약 N SUP, 0 이면 늘지 않는다, 착화감 상한`() {
        val p = PointsPreview.parse(SAMPLE)!!
        assertEquals(PointWorth.Gain("0.42"), pointWorthOf(p, PointStat.EFFICIENCY))
        assertEquals(PointWorth.Gain("1.31"), pointWorthOf(p, PointStat.DURABILITY))
        // 0 이면 "+0 SUP"가 아니라 늘지 않는다고 말한다
        assertEquals(PointWorth.Flat, pointWorthOf(p, PointStat.COMFORT))
        assertEquals(PointWorth.ComfortCapped, pointWorthOf(p.copy(comfortCapped = true), PointStat.COMFORT))
        // 상한은 착화감 줄에만
        assertEquals(PointWorth.Gain("0.42"), pointWorthOf(p.copy(comfortCapped = true), PointStat.EFFICIENCY))
        // 서버가 그 칸을 보내지 않았으면 줄이 없다
        assertNull(pointWorthOf(PointsPreview.parse("""{"ok":true}""")!!, PointStat.EFFICIENCY))
        assertEquals("0.42", formatApproxSup(0.42))
        assertEquals("1.3", formatApproxSup(1.3))
        assertEquals("12", formatApproxSup(12.0))
        assertEquals("0.01", formatApproxSup(0.005))
        assertEquals(PointWorth.Flat, pointWorthOf(p.copy(perPoint = p.perPoint.copy(efficiency = 0.004)), PointStat.EFFICIENCY))
    }

    // ── 추천대로 채우기 ──────────────────────────────────────────

    @Test fun `추천대로 나누기 — 이미 나눈 값에 더한다, 그사이 고친 것은 버린다`() {
        val basis = requireNotNull(pointsBasisOf(shoe(total = 11, efficiency = 2, durability = 1), roots))
        val preview = previewOf(total = 11, now = PointsSplit(2, 0, 1), recommend = PointsSplit(2, 0, 6))
        assertTrue(basis.accepts(preview))
        assertEquals(PointsDraft(4, 0, 7), basis.recommended(preview))
        // ± 로 고친 뒤에 눌러도 서버가 본 값(이미 나눈 값)에서 더한다
        val edited = startPoints(basis).add(PointStat.COMFORT).add(PointStat.COMFORT)
        val filled = edited.recommend(preview)
        assertEquals(PointsDraft(4, 0, 7), filled.draft)
        assertEquals(0, basis.free(filled.draft))
        assertFalse(basis.lowers(filled.draft))
        // 초안만 채운다 — 보내는 것은 "포인트 나누기" 뒤 확인에서
        assertTrue(filled is PointsPhase.Editing)
        assertTrue(filled.review() is PointsPhase.Confirm)
        // 이미 추천과 같으면 그대로(같은 장면)
        assertSame(filled, filled.recommend(preview))
    }

    @Test fun `추천은 받은 포인트를 넘지 않는다 — 넘치면 착화감 · 내구도 · 효율 차례로 덜어 낸다`() {
        val basis = requireNotNull(pointsBasisOf(shoe(total = 11, efficiency = 2, durability = 1), roots))
        // 남은 포인트 8 보다 많이 더하라는 추천(낡은 값) — 2 + 5, 0 + 3, 1 + 4 = 15 → 11
        val draft = requireNotNull(basis.recommended(previewOf(total = 11, now = PointsSplit(2, 0, 1), recommend = PointsSplit(5, 3, 4))))
        assertEquals(PointsDraft(7, 0, 4), draft)
        assertEquals(11, draft.sum)
        assertTrue(basis.valid(draft))
        // 이미 나눈 값 아래로는 덜어 내지 않는다
        assertEquals(PointsDraft(2, 0, 1), basis.fitted(PointsDraft(0, 0, 0)))
        // 더할 것이 없는 추천 · 추천이 없는 미리보기 · 미리보기 없음 — 버튼이 없다
        assertNull(basis.recommended(previewOf(total = 11, now = PointsSplit(2, 0, 1), recommend = PointsSplit(0, 0, 0))))
        assertNull(basis.recommended(previewOf(total = 11, now = PointsSplit(2, 0, 1), recommend = null)))
        assertNull(basis.recommended(null))
        val start = startPoints(basis)
        assertSame(start, start.recommend(null))
    }

    @Test fun `추천은 착화감 상한을 넘지 않는다 — 에너지 70퍼센트 절약까지`() {
        // 일반 착화감 꼭대기(절약 4,444bps) — 상한까지 약 85포인트
        val basis = requireNotNull(pointsBasisOf(shoe(total = 200, comfortBase = 4_444, level = 30), roots))
        val draft = requireNotNull(basis.recommended(previewOf(total = 200, now = PointsSplit(0, 0, 0), recommend = PointsSplit(10, 150, 40))))
        assertTrue(draft.comfort in 80..90)
        assertTrue(basis.comfortBps(draft) <= 7_000)
        assertFalse(basis.comfortFits(draft.comfort + 1))
        assertEquals(10, draft.efficiency)
        assertEquals(40, draft.durability)
        assertTrue(basis.valid(draft))
    }

    @Test fun `처음부터 추천대로 — 다시 나눌 수 있는 신발만, 절대값으로 바꾸고 다시 나누기를 쓴다`() {
        val respec = requireNotNull(pointsBasisOf(shoe(total = 16, efficiency = 8, comfort = 8, respec = true, level = 5), roots))
        val preview = previewOf(total = 16, now = PointsSplit(8, 8, 0), recommend = PointsSplit(0, 0, 0), all = PointsSplit(4, 0, 12))
        assertEquals(PointsDraft(4, 0, 12), respec.recommendedAll(preview))
        val moved = startPoints(respec).recommendAll(preview)
        assertEquals(PointsDraft(4, 0, 12), moved.draft)
        // 이미 나눈 값을 줄였다 — 확인 장면이 다시 나누기를 쓴다고 말한다
        assertTrue(respec.lowers(moved.draft))
        assertTrue(respec.valid(moved.draft))
        // 남은 포인트가 없으니 "추천대로 나누기"는 없다
        assertNull(respec.recommended(preview))
        // 절대값이 받은 포인트를 넘으면 맞춘다
        assertEquals(16, respec.recommendedAll(preview.copy(recommendAll = PointsSplit(10, 4, 12)))!!.sum)
        // 다시 나눌 수 없는 신발은 절대값 추천을 쓰지 않는다
        val fixed = requireNotNull(pointsBasisOf(shoe(total = 16, efficiency = 8, comfort = 8, level = 5), roots))
        assertNull(fixed.recommendedAll(preview))
        val start = startPoints(fixed)
        assertSame(start, start.recommendAll(preview))
    }

    @Test fun `낡은 미리보기는 쓰지 않는다 — 서버가 본 값이 폰 사본과 다르면`() {
        val basis = requireNotNull(pointsBasisOf(shoe(total = 11, efficiency = 2, durability = 1), roots))
        val stale = previewOf(total = 11, now = PointsSplit(0, 0, 0), recommend = PointsSplit(2, 0, 6))
        assertFalse(basis.accepts(stale))
        assertNull(basis.recommended(stale))
        assertFalse(basis.accepts(previewOf(total = 14, now = PointsSplit(2, 0, 1), recommend = PointsSplit(2, 0, 6))))
        val start = startPoints(basis)
        assertSame(start, start.recommend(stale))
    }

    // ── 수명(분) — 효율 포인트만큼 빨리 닳는다 ───────────────────

    @Test fun `가득에서 달리는 분 — 효율 포인트는 줄이고 내구도 포인트는 늘린다`() {
        // 레어(포인트 1 = 1.1%) · 뽑은 내구도 200 → 40분
        val basis = requireNotNull(pointsBasisOf(shoe(total = 40, base = 200, rarity = Rarity.RARE, level = 10), roots))
        assertEquals(40.0, basis.lifeMinutes(PointsDraft()), 1e-9)
        // 효율 10 = 닳는 배수 1.11 → 40 ÷ 1.11
        assertEquals(40.0 / 1.11, basis.lifeMinutes(PointsDraft(efficiency = 10)), 1e-9)
        assertEquals("36", formatMinutes(basis.lifeMinutes(PointsDraft(efficiency = 10))))
        // 내구도 10 = 최대 내구도 222 → 44.4분
        assertEquals(44.4, basis.lifeMinutes(PointsDraft(durability = 10)), 1e-9)
        assertEquals("44.4", formatMinutes(basis.lifeMinutes(PointsDraft(durability = 10))))
        // 둘 다 10 — 222 ÷ 5 ÷ 1.11 = 40분(끝자리 때문에 39.9 로 내려가지 않는다)
        assertEquals("40", formatMinutes(basis.lifeMinutes(PointsDraft(efficiency = 10, durability = 10))))
        // 착화감은 수명과 상관없다
        assertEquals(40.0, basis.lifeMinutes(PointsDraft(comfort = 10)), 1e-9)

        // 0119 전 서버 — 효율 포인트는 수명을 건드리지 않는다
        val old = requireNotNull(pointsBasisOf(shoe(total = 40, base = 200, rarity = Rarity.RARE, level = 10), legacy))
        assertEquals(40.0, old.lifeMinutes(PointsDraft(efficiency = 10)), 1e-9)
        assertEquals(44.4, old.lifeMinutes(PointsDraft(efficiency = 10, durability = 10)), 1e-9)
    }

    @Test fun `신발 화면의 분 — 효율 포인트를 넣은 신발은 그 배수만큼 짧다`() {
        // 레어 · 효율 포인트 2(× 1.022) · 남은 1,180 / 최대 1,330
        val pointed = shoe(total = 8, efficiency = 2, comfort = 1, durability = 1, rarity = Rarity.RARE, level = 3, now = 1_180, max = 1_330)
        assertEquals(1_180 / 5.0 / 1.022, lifeMinutes(pointed, 1_180, roots), 1e-9)
        assertEquals(230, wholeMinutes(pointed, 1_180, roots))
        assertEquals(260, wholeMinutes(pointed, 1_330, roots))
        assertEquals("230 / 260", statBars(pointed, roots)[2].value)
        assertEquals("260", statBars(pointed, roots)[2].end)
        // 막대 길이는 남은 ÷ 최대 그대로
        assertEquals(1_180f / 1_330f, statBars(pointed, roots)[2].fraction, 1e-4f)
        // 0119 전 서버 — 예전처럼 내구도 ÷ 5
        assertEquals("236 / 266", statBars(pointed, legacy)[2].value)
        // 효율 포인트가 없는 신발은 어느 규칙에서나 내구도 ÷ 5
        val plain = shoe(total = 8, durability = 8, rarity = Rarity.RARE, level = 3, now = 1_180, max = 1_330)
        assertEquals(236, wholeMinutes(plain, 1_180, roots))
        // 폰에서 올린 예전 신발은 포인트가 있어도 배수 1(서버 eff_point_mult 와 같다)
        val imported = pointed.copy(server = pointed.server!!.copy(origin = "IMPORT"))
        assertEquals(236, wholeMinutes(imported, 1_180, roots))
        // 분 → 글: 내려서 소수 한 자리, 끝의 .0 은 뺀다
        assertEquals("31", formatMinutes(31.0))
        assertEquals("30.9", formatMinutes(30.99))
        assertEquals("1,234.5", formatMinutes(1234.56))
        assertEquals(0, wholeMinutes(-3.0))
        assertEquals(31, wholeMinutes(30.9999999))
    }

    @Test fun `내구도 포인트 1 의 분 — 이미 나눈 효율 포인트만큼 짧다`() {
        // 레어 200 × 1.1% = 2.2(12초 단위) = 0.44분 → 0.4
        val none = requireNotNull(pointsBasisOf(shoe(total = 120, base = 200, rarity = Rarity.RARE, level = 20), roots))
        assertEquals("0.4", none.minutesPerPointText())
        // 효율 100(× 2.1) — 0.44 ÷ 2.1 = 0.209 → 0.2
        val fast = requireNotNull(pointsBasisOf(shoe(total = 120, efficiency = 100, base = 200, rarity = Rarity.RARE, level = 20), roots))
        assertEquals("0.2", fast.minutesPerPointText())
        // 0119 전 서버는 효율 포인트와 상관없다
        val old = requireNotNull(pointsBasisOf(shoe(total = 120, efficiency = 100, base = 200, rarity = Rarity.RARE, level = 20), legacy))
        assertEquals("0.4", old.minutesPerPointText())
    }

    @Test fun `수리 몫 — 내구도 포인트만큼 낮아지고 효율 포인트와는 상관없다`() {
        val plain = shoe(total = 0, rarity = Rarity.RARE, level = 1)
        assertEquals(0.45, repairShareOf(plain, roots), 1e-9)
        // 효율 포인트 — 1점 값은 싸지고 그만큼 빨리 닳는다: 몫은 그대로
        val eff = shoe(total = 50, efficiency = 50, rarity = Rarity.RARE, level = 12)
        assertEquals(0.45, repairShareOf(eff, roots), 1e-9)
        // 내구도 포인트 50(× 1.55) — 가득 수리비는 그대로라 몫이 그만큼 낮다
        val dur = shoe(total = 50, durability = 50, rarity = Rarity.RARE, level = 12)
        assertEquals(0.45 / 1.55, repairShareOf(dur, roots), 1e-9)
        // 0119 전 서버 — 설정 몫 그대로
        assertEquals(0.45, repairShareOf(dur, legacy), 1e-9)
        assertEquals(0.45, repairShareOf(dur.copy(server = null), roots), 1e-9)
    }

    // ── 레벨업 — 에너지 · 다시 나누기 ────────────────────────────

    @Test fun `하루 에너지 줄 — 레벨당 에너지가 0 이면 없다`() {
        // 서버 0119 — 하루 에너지는 레벨과 상관없다
        assertEquals(0.0, energyPerLevel(roots), 0.0)
        assertTrue(forgeRewards(Rarity.RARE, 7, roots).none { it is ForgeReward.Energy })
        assertEquals(listOf(ForgeReward.Points(3)), forgeRewards(Rarity.RARE, 7, roots))
        // 포인트가 맨 앞
        assertTrue(forgeRewards(Rarity.RARE, 10, roots).first() is ForgeReward.Points)
        // 0119 전 서버 — 레벨마다 +2
        assertEquals(2.0, energyPerLevel(legacy), 0.0)
        assertEquals(listOf(ForgeReward.Points(3), ForgeReward.Energy(2.0)), forgeRewards(Rarity.RARE, 7, legacy))
        // 운영자가 레벨마다 +0.5 로 켜면 그 값
        val half = roots.copy(stats = roots.stats.copy(energyPerLevel = 0.5))
        assertEquals(listOf(ForgeReward.Points(3), ForgeReward.Energy(0.5)), forgeRewards(Rarity.RARE, 7, half))
        assertEquals("0.5", formatEnergy(0.5))
        assertEquals("2", formatEnergy(2.0))
        // 이상한 값은 0 으로(줄이 없다)
        assertEquals(0.0, energyPerLevel(roots.copy(stats = roots.stats.copy(energyPerLevel = -1.0))), 0.0)
        assertEquals(0.0, energyPerLevel(roots.copy(stats = roots.stats.copy(energyPerLevel = Double.NaN))), 0.0)
    }

    @Test fun `다시 나누기 — 5레벨 눈금을 지날 때마다 한 번, 규칙을 받았을 때만 말한다`() {
        assertTrue(respecEarned(4, 5, 5))
        assertTrue(respecEarned(9, 10, 5))
        // 한 번에 여러 레벨이 올라 눈금을 지났다
        assertTrue(respecEarned(3, 6, 5))
        assertFalse(respecEarned(5, 6, 5))
        assertFalse(respecEarned(1, 4, 5))
        assertFalse(respecEarned(5, 5, 5))
        // 간격이 0 이면(0119 전 서버) 없다
        assertFalse(respecEarned(4, 5, 0))
        assertEquals(5, respecEveryOf(roots))
        assertEquals(0, respecEveryOf(legacy))
        // 아직 규칙을 못 읽었으면(기본값) 말하지 않는다
        assertEquals(0, respecEveryOf(ShoeRules.FALLBACK))
        assertEquals(0, respecEveryOf(roots.copy(stats = roots.stats.copy(respecEvery = 0))))
    }

    @Test fun `능력치 글 — 서버가 0119 규칙을 보냈을 때만 새 뜻`() {
        assertTrue(newStatMeaning(roots))
        assertFalse(newStatMeaning(legacy))
        // 아직 규칙을 못 읽었으면 예전 글
        assertFalse(newStatMeaning(ShoeRules.FALLBACK))
        // 운영자가 셋(닳는 속도 · 에너지 · 수리비) 가운데 하나라도 껐으면 예전 글 — 새 글은 셋을 함께 말한다
        assertFalse(newStatMeaning(roots.copy(stats = roots.stats.copy(effWearExp = 0.0))))
        assertFalse(newStatMeaning(roots.copy(stats = roots.stats.copy(effEnergyExp = 0.0))))
        assertFalse(newStatMeaning(roots.copy(stats = roots.stats.copy(durRepairExp = 0.0))))
        assertEquals(R.string.shoe_info_efficiency_v2, statMeaningRes(PointStat.EFFICIENCY, true))
        assertEquals(R.string.shoe_info_efficiency, statMeaningRes(PointStat.EFFICIENCY, false))
        assertEquals(R.string.shoe_info_durability_v2, statMeaningRes(PointStat.DURABILITY, true))
        assertEquals(R.string.shoe_info_comfort, statMeaningRes(PointStat.COMFORT, false))
        assertNotNull(statMeaningRes(PointStat.COMFORT, true))
    }

    // ── 도우미 ───────────────────────────────────────────────────

    private fun previewOf(total: Int, now: PointsSplit, recommend: PointsSplit?, all: PointsSplit? = null) = PointsPreview(
        source = PointsPreview.Source.MINE, runs = 7, days = 6, avgMin = 42,
        total = total, free = total - now.sum, respec = all != null, now = now, wornRuns = 0, energyOutDays = 0,
        perPoint = PointsPreview.PerPoint(0.4, 0.0, 1.3), comfortCapped = false, recommend = recommend, recommendAll = all,
    )

    private fun shoe(
        total: Int,
        efficiency: Int = 0,
        comfort: Int = 0,
        durability: Int = 0,
        respec: Boolean = false,
        level: Int = 5,
        effBase: Int = 4_000,
        comfortBase: Int = 2_857,
        base: Int = 155,
        rarity: Rarity = Rarity.COMMON,
        max: Int = ShoeRules.FALLBACK.durabilityWithPoints(base, rarity.id, durability),
        now: Int = max,
    ) = Sneaker(
        id = 7, faction = Faction.WIND, rarity = rarity, variant = 0, level = level, mintNumber = 7,
        luck = 1.0, comfort = 1.0, durability = 100, equipped = false, acquiredAt = 0L,
        server = ServerStats(
            origin = "PAID_DRAW",
            efficiencyBps = ShoeRules.FALLBACK.efficiencyWithPoints(effBase, rarity.id, efficiency),
            comfortBps = ShoeRules.FALLBACK.comfortWithPoints(comfortBase, rarity.id, comfort),
            durabilityPts = Math.round(10_000.0 * now / max) / 100.0,
            maxLevel = 30, status = "OWNED", chainState = "APP", canWithdraw = false, upgradeCost = 0.0,
            repairCostPerPoint = 0.0, genesisNo = 0,
            durabilityNow = now, durabilityMax = max, durabilityBase = base,
            efficiencyBaseBps = effBase, comfortBaseBps = comfortBase,
            points = StatPoints(total, total - efficiency - comfort - durability, efficiency, comfort, durability, respec),
            repairCostPerUnit = 0.0105,
        ),
    )

    private class Store(var value: AuthSession?) : AuthSessionStore {
        override suspend fun load() = value
        override suspend fun save(session: AuthSession) { value = session }
        override suspend fun clear() { value = null }
    }

    private class Http(private val reply: (String) -> HttpResponse) : HttpPoster {
        val calls = mutableListOf<Pair<String, String>>()
        override suspend fun post(url: String, body: String, headers: Map<String, String>): HttpResponse {
            calls += url to body
            return reply(url)
        }
        override suspend fun get(url: String, headers: Map<String, String>) = post(url, "", headers)
    }

    private fun api(http: Http, signedIn: Boolean = true): ShoePointsPreviewApi {
        val session = AuthSession(
            accessToken = "token", refreshToken = "refresh", expiresIn = 3600,
            expiresAt = 9_999_999_999L, user = AuthUser(id = "A", isAnonymous = false),
        )
        val holder = SessionHolder(SupabaseAuth("https://test.supabase.co", "test", http),
            Store(if (signedIn) session else null), now = { 1000L })
        return ShoePointsPreviewApi(StepUpServer("https://test.supabase.co", "test", holder, http))
    }

    private companion object {
        /** docs/economy-roots-contract-2026-11-21.md 3장의 예 */
        const val SAMPLE = """{"ok": true,
 "basis": {"source": "MINE", "days": 6, "runs": 7, "avg_min": 42},
 "total": 77, "free": 12, "respec": true,
 "now": {"efficiency": 40, "comfort": 5, "durability": 20, "day_net": 151.2, "life_min": 38.5, "worn_runs": 3, "energy_out_days": 0},
 "per_point": {"efficiency": 0.42, "comfort": 0.0, "durability": 1.31},
 "life_min_per_point": {"efficiency": -0.3, "durability": 0.46},
 "comfort_capped": false,
 "recommend": {"efficiency": 2, "comfort": 0, "durability": 10, "day_net": 166.0, "life_min": 43.1},
 "recommend_all": {"efficiency": 22, "comfort": 6, "durability": 49, "day_net": 171.3, "life_min": 54.0}}"""
    }
}
