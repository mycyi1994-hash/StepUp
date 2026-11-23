package com.stepup.android

import com.stepup.android.data.remote.AppVersionHeader
import com.stepup.android.data.remote.AuthSession
import com.stepup.android.data.remote.AuthSessionStore
import com.stepup.android.data.remote.AuthUser
import com.stepup.android.data.remote.HttpPoster
import com.stepup.android.data.remote.HttpResponse
import com.stepup.android.data.remote.ServerResult
import com.stepup.android.data.remote.SessionHolder
import com.stepup.android.data.remote.StepUpServer
import com.stepup.android.data.remote.SupabaseAuth
import com.stepup.android.data.remote.UPDATE_REQUIRED_REASON
import com.stepup.android.data.remote.isUpdateRequired
import com.stepup.android.data.remote.toResult
import com.stepup.android.domain.CrewBonus
import com.stepup.android.domain.CrewBonusStatus
import com.stepup.android.domain.CrewRunOutcome
import com.stepup.android.domain.DailyMaxRule
import com.stepup.android.domain.Faction
import com.stepup.android.domain.Rarity
import com.stepup.android.domain.RunVerdictReason
import com.stepup.android.domain.ServerStats
import com.stepup.android.domain.ShoeRules
import com.stepup.android.domain.Sneaker
import com.stepup.android.domain.TogetherRule
import com.stepup.android.ui.screens.items.shoeDailyMax
import com.stepup.android.ui.screens.splash.storeWebUrl
import com.stepup.android.ui.screens.walk.BONUS_NOT_TOGETHER
import com.stepup.android.ui.screens.walk.TogetherLobbyLine
import com.stepup.android.ui.screens.walk.TogetherResultLine
import com.stepup.android.ui.screens.walk.togetherLobbyLine
import com.stepup.android.ui.screens.walk.togetherResultLine
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/** 함께 달린 사람 · 하루 최대 SUP · 강제 업데이트(서버 0120, docs/together-bonus-fix-spec-2026-11-21.md) */
class TogetherBonusTest {

    private val r = ShoeRules.FALLBACK

    /** 서버 `shoe_rules_v4()` 의 글 — [extra] 는 맨 끝에 붙는 묶음들(",\"together\":{…}") */
    private fun rulesBody(extra: String = ""): String =
        """{"points_per_step":0.0118,"repair_share":0.45,"reward_luck":[0.7,1,1.3],"wear_luck":[0.667,0.889,1.444],
            "comfort_cap_bps":7000,"free_repair_hours":48,"forge_material_min_durability":100,"boost_shop_enabled":false,"grades":[""" +
            r.grades.joinToString(",") { g ->
                """{"rarity":"${g.rarity}","efficiency":[${g.efficiencyLo},${g.efficiencyHi}],"comfort":[${g.comfortLo},${g.comfortHi}],""" +
                    """"durability":[${g.durabilityLo},${g.durabilityHi}],"points_per_level":${g.pointsPerLevel},""" +
                    """"points_milestone":${g.pointsMilestone},"point_rate_bps":${g.pointRateBps},"max_level":${g.maxLevel}}"""
            } + "]" + extra + "}"

    // ── 규칙 묶음 읽기 ──

    @Test fun `together · daily_max 묶음을 서버 값 그대로 읽는다`() {
        val parsed = ShoeRules.parse(
            rulesBody(
                ""","together":{"rule":"ACTIVE","min_minutes":12,"min_steps":1800,"goal_share":0.6,"topup_hours":36},""" +
                    """"daily_max":{"rule":"HOUR","hour_steps":9000,"hour_mult":3.5,"goal_included":false}""",
            ),
        )!!
        assertEquals(TogetherRule("ACTIVE", 12, 1800, 0.6, 36, fromServer = true), parsed.together)
        assertEquals(DailyMaxRule("HOUR", 9000, 3.5, goalIncluded = false, fromServer = true), parsed.dailyMax)
        assertTrue(parsed.together.active)
        assertTrue(parsed.dailyMax.hour)
    }

    @Test fun `번개 규칙은 크루와 따로 — flash_rule 이 없으면 크루 규칙을 따른다`() {
        val split = ShoeRules.parse(rulesBody(""","together":{"rule":"ACTIVE","flash_rule":"legacy","min_minutes":10}"""))!!.together
        assertTrue(split.active)
        assertFalse(split.forFlash().active)
        assertNull(togetherLobbyLine(split.forFlash(), 5.0, course = false))
        assertEquals(TogetherLobbyLine.Half(2.5, 10, false), togetherLobbyLine(split.copy(flashRule = "ACTIVE").forFlash(), 5.0, course = false))
        val same = ShoeRules.parse(rulesBody(""","together":{"rule":"ACTIVE","min_minutes":10}"""))!!.together
        assertTrue(same.forFlash().active)
    }

    @Test fun `크루 효율 8 대 2 — blend_share 를 읽고, 칸이 없으면 섞지 않는다`() {
        val on = ShoeRules.parse(rulesBody(""","together":{"rule":"ACTIVE","blend_share":0.2}"""))!!.together
        assertEquals(0.2, on.blendShare, 1e-9)
        assertTrue(on.blends)
        val missing = ShoeRules.parse(rulesBody(""","together":{"rule":"ACTIVE"}"""))!!.together
        assertFalse(missing.blends)
        val legacy = ShoeRules.parse(rulesBody(""","together":{"rule":"LEGACY","blend_share":0.2}"""))!!.together
        assertFalse(legacy.blends)
        assertEquals(0.5, ShoeRules.parse(rulesBody(""","together":{"rule":"ACTIVE","blend_share":0.9}"""))!!.together.blendShare, 1e-9)
        assertFalse(TogetherRule.FALLBACK.blends)
        assertEquals(0.2, TogetherRule.FALLBACK.blendShare, 1e-9)
    }

    @Test fun `묶음이 없는 서버(0120 전)는 예전 규칙 — 함께 출발한 모두 · 예전 하루 금액 상한`() {
        val parsed = ShoeRules.parse(rulesBody())!!
        assertEquals(TogetherRule.LEGACY, parsed.together)
        assertEquals(DailyMaxRule.LEGACY, parsed.dailyMax)
        assertFalse(parsed.together.active)
        assertNull(parsed.dailyMaxSup(4000))
        // 서버를 읽기 전(FALLBACK)은 서버 0120 의 기본값 — 다만 서버 값이 아니라고 적어 둔다
        assertEquals(TogetherRule("ACTIVE", 10, 1500, 0.5, 24, blendShare = 0.2), ShoeRules.FALLBACK.together)
        assertEquals(DailyMaxRule("HOUR", 9900, 4.0, true), ShoeRules.FALLBACK.dailyMax)
        assertFalse(ShoeRules.FALLBACK.together.fromServer)
        assertFalse(ShoeRules.FALLBACK.dailyMax.fromServer)
    }

    @Test fun `빠진 칸은 기본값 · 규칙 글자는 대문자로 · 어긋난 값은 범위 안으로`() {
        val parsed = ShoeRules.parse(
            rulesBody(""","together":{"rule":"legacy","min_minutes":10},"daily_max":{"hour_mult":"4","goal_share":2}"""),
        )!!
        assertEquals("LEGACY", parsed.together.rule)
        assertFalse(parsed.together.active)
        assertEquals(1500, parsed.together.minSteps)
        assertEquals(0.5, parsed.together.goalShare, 1e-9)
        assertEquals(24, parsed.together.topupHours)
        assertTrue(parsed.together.fromServer)
        // 규칙 글자가 빠졌으면 서버 0120 의 기본값(HOUR) · 걸음도 기본값
        assertEquals("HOUR", parsed.dailyMax.rule)
        assertEquals(9900, parsed.dailyMax.hourSteps)
        assertEquals(4.0, parsed.dailyMax.hourMult, 1e-9)
        // 몫은 0 ~ 1
        val clamped = ShoeRules.parse(rulesBody(""","together":{"goal_share":1.7}"""))!!
        assertEquals(1.0, clamped.together.goalShare, 1e-9)
        // 묶음이 아닌 값은 없는 것으로(예전 규칙)
        assertEquals(TogetherRule.LEGACY, ShoeRules.parse(rulesBody(""","together":"ACTIVE""""))!!.together)
    }

    // ── 하루 최대 SUP ──

    @Test fun `일반 첫 신발(효율 +40%)의 하루 최대 = 4 × 9,900 × 0_0118 × 1_4`() {
        val rules = ShoeRules.parse(rulesBody(""","daily_max":{"rule":"HOUR","hour_steps":9900,"hour_mult":4}"""))!!
        // 4 × 9900 × 0.0118 = 467.28 · × 1.4 = 654.192 (서버 economy.daily_max_of 도 소수 넷째 자리까지)
        assertEquals(654.192, rules.dailyMaxSup(4000)!!, 1e-9)
        assertEquals(467.28, rules.dailyMaxSup(0)!!, 1e-9)
        // 포인트를 넣은 효율(실효 bps)로 커진다
        assertTrue(rules.dailyMaxSup(rules.efficiencyWithPoints(4000, "COMMON", 10))!! > 654.192)
        // 예전 규칙(CAP)이면 없다
        assertNull(rules.copy(dailyMax = DailyMaxRule.LEGACY).dailyMaxSup(4000))
    }

    private fun shoe(effBps: Int?) = Sneaker(
        id = 1, faction = Faction.WIND, rarity = Rarity.COMMON, variant = 0, level = 1, mintNumber = 1,
        luck = 1.0, comfort = 1.0, durability = 100, equipped = true, acquiredAt = 0L, modelId = 1107,
    ).let { s ->
        if (effBps == null) s else s.copy(
            server = ServerStats(
                origin = "DRAW", efficiencyBps = effBps, comfortBps = 0, durabilityPts = 100.0, maxLevel = 20, status = "OWNED",
                chainState = "APP", canWithdraw = false, upgradeCost = 0.0, repairCostPerPoint = 0.0, genesisNo = 0,
            ),
        )
    }

    @Test fun `신발 화면의 하루 최대는 서버에서 받은 하루 최대 규칙 · 서버 신발일 때만`() {
        val served = ShoeRules.parse(rulesBody(""","daily_max":{"rule":"HOUR","hour_steps":9900,"hour_mult":4}"""))!!
        assertEquals(654.192, shoeDailyMax(shoe(4000), served)!!, 1e-9)
        // 폰에만 있던 신발 · 서버를 아직 못 읽음(FALLBACK) · 예전 서버(CAP)는 줄을 두지 않는다
        assertNull(shoeDailyMax(shoe(null), served))
        assertNull(shoeDailyMax(shoe(4000), ShoeRules.FALLBACK))
        assertNull(shoeDailyMax(shoe(4000), ShoeRules.parse(rulesBody())!!))
    }

    // ── 판정 까닭 ──

    @Test fun `하루 최대 SUP 에 걸린 러닝은 그 까닭 한 줄 — 예전 까닭도 그대로`() {
        val note = RunVerdictReason.note("SIGNED", 12.5, "하루 최대 SUP 에 걸렸습니다", "")!!
        assertFalse(note.zero)
        assertEquals(listOf(RunVerdictReason.Kind.DAILY_MAX), note.kinds)
        val zero = RunVerdictReason.note("SIGNED", 0.0, "신발이 다 닳은 뒤의 걸음은 일부만 적립했습니다 · 하루 최대 SUP 에 걸렸습니다", "")!!
        assertTrue(zero.zero)
        assertEquals(listOf(RunVerdictReason.Kind.WORN_PARTIAL, RunVerdictReason.Kind.DAILY_MAX), zero.kinds)
        assertEquals(listOf(RunVerdictReason.Kind.DAILY_SUP), RunVerdictReason.kinds("하루 적립 금액 상한에 걸렸습니다"))
    }

    // ── 강제 업데이트 ──

    @Test fun `426 · update_required 를 알아본다 — 다른 거절과 헷갈리지 않는다`() {
        val gate = HttpResponse(
            426, """{"code":"PT426","details":"새 판으로 업데이트해야 합니다","hint":"update_required","message":"update_required"}""",
        )
        assertTrue(gate.isUpdateRequired())
        // 상태 번호가 바뀌어 와도 PostgREST 글의 message · hint 로
        assertTrue(HttpResponse(400, """{"code":"PT426","message":"x","hint":"update_required"}""").isUpdateRequired())
        // 다른 거절 · 낱말이 다른 칸에 섞인 글 · 접속 코드 문 · 성공은 아니다
        assertFalse(HttpResponse(400, """{"message":"크루장만 할 수 있습니다","details":"update_required 아님"}""").isUpdateRequired())
        assertFalse(HttpResponse(403, """{"code":"42501","message":"access_required"}""").isUpdateRequired())
        assertFalse(HttpResponse(200, """[{"note":"update_required"}]""").isUpdateRequired())
        // 로그인 없이 · 로그인 자격으로 여는 길 모두 거절로 굳히지 않는다
        assertEquals(ServerResult.Retry(UPDATE_REQUIRED_REASON), gate.toResult())
    }

    private class Store(var value: AuthSession?) : AuthSessionStore {
        override suspend fun load() = value
        override suspend fun save(session: AuthSession) { value = session }
        override suspend fun clear() { value = null }
    }

    private class Http(var answer: HttpResponse) : HttpPoster {
        var calls = 0
        var lastHeaders: Map<String, String> = emptyMap()
        override suspend fun post(url: String, body: String, headers: Map<String, String>): HttpResponse {
            calls++
            lastHeaders = headers
            return answer
        }
        override suspend fun get(url: String, headers: Map<String, String>): HttpResponse {
            calls++
            lastHeaders = headers
            return answer
        }
    }

    private fun server(http: Http, version: Int = 30, clock: () -> Long = { 0L }): StepUpServer {
        val session = AuthSession(
            accessToken = "token", refreshToken = "refresh", expiresIn = 3600,
            expiresAt = 9_999_999_999L, user = AuthUser(id = "A", isAnonymous = false),
        )
        val holder = SessionHolder(SupabaseAuth("https://test.supabase.co", "test", http), Store(session), now = { 1000L })
        return StepUpServer("https://test.supabase.co", "test", holder, http, appVersion = version, clockMs = clock)
    }

    @Test fun `머리글은 스토어 번호 — versionCode 의 끝 세 자리`() = runBlocking {
        assertEquals(30, AppVersionHeader.storeNumber(30))
        // 테스트 · 개발 빌드는 데이터베이스 판 × 1000 + 스토어 번호
        assertEquals(30, AppVersionHeader.storeNumber(23_030))
        assertEquals(0, AppVersionHeader.storeNumber(0))
        assertEquals(AppVersionHeader.storeNumber(BuildConfig.VERSION_CODE), BuildConfig.VERSION_CODE % 1000)
        assertEquals("x-stepup-version", AppVersionHeader.NAME)
        val http = Http(HttpResponse(200, "[]"))
        val s = server(http, version = 30)
        assertEquals("30", s.headers("token")["x-stepup-version"])
        assertEquals("30", s.anonHeaders()["x-stepup-version"])
        // 실제로 보내는 요청에도 붙는다(로그인 · 로그인 없이)
        s.authed { token -> http.post("https://test.supabase.co/rest/v1/rpc/x", "{}", s.headers(token)) }
        assertEquals("30", http.lastHeaders["x-stepup-version"])
        s.anonGet("https://test.supabase.co/rest/v1/news_items")
        assertEquals("30", http.lastHeaders["x-stepup-version"])
    }

    @Test fun `426 이면 업데이트 화면 — 몇 분 동안은 서버에 보내지 않고, 다시 받아 주면 풀린다`() = runBlocking {
        var now = 0L
        val http = Http(HttpResponse(426, """{"code":"PT426","message":"update_required","hint":"update_required"}"""))
        val s = server(http, clock = { now })
        val call: suspend (String) -> HttpResponse = { token -> http.post("https://test.supabase.co/rest/v1/rpc/x", "{}", s.headers(token)) }
        assertFalse(s.updateRequired.value)
        assertEquals(ServerResult.Retry(UPDATE_REQUIRED_REASON), s.authed(call = call))
        assertTrue(s.updateRequired.value)
        assertEquals(1, http.calls)
        // 기다리는 동안은 보내지 않는다(서버를 두드리지 않게) — 같은 "나중에 다시"
        now += StepUpServer.UPDATE_RECHECK_MS - 1
        assertEquals(ServerResult.Retry(UPDATE_REQUIRED_REASON), s.authed(call = call))
        assertEquals(ServerResult.Retry(UPDATE_REQUIRED_REASON), s.openPost("https://test.supabase.co/rest/v1/rpc/y", "{}"))
        assertEquals(1, http.calls)
        // 시간이 지나면 한 번 다시 묻는다 — 운영자가 판을 내렸으면 풀린다
        now += 2
        http.answer = HttpResponse(200, "{}")
        assertEquals(ServerResult.Ok("{}"), s.authed(call = call))
        assertEquals(2, http.calls)
        assertFalse(s.updateRequired.value)
    }

    @Test fun `다른 거절은 업데이트 화면을 띄우지 않는다`() = runBlocking {
        val http = Http(HttpResponse(400, """{"message":"크루장만 할 수 있습니다"}"""))
        val s = server(http)
        assertEquals(ServerResult.Rejected("크루장만 할 수 있습니다"), s.authed { http.answer })
        assertFalse(s.updateRequired.value)
    }

    @Test fun `업데이트는 Play 의 이 앱으로 — Play 앱이 없을 때 웹 주소`() {
        assertEquals("https://play.google.com/store/apps/details?id=com.stepup.android", storeWebUrl("com.stepup.android"))
    }

    // ── 대기실 · 결과 한 줄 ──

    private val active = TogetherRule("ACTIVE", 10, 1500, 0.5, 24, fromServer = true)

    @Test fun `대기실 한 줄 — 코스 절반 · 자유 코스는 분만 · 예전 규칙과 모르는 값은 없음`() {
        assertEquals(TogetherLobbyLine.Half(2.5, 10, course = true), togetherLobbyLine(active, 5.0, course = true))
        assertEquals(TogetherLobbyLine.Minutes(10), togetherLobbyLine(active, 0.0, course = true))
        // 번개 — 글의 거리
        assertEquals(TogetherLobbyLine.Half(1.5, 10, course = false), togetherLobbyLine(active, 3.0, course = false))
        // 몫이 절반이 아니면 거리만
        assertEquals(TogetherLobbyLine.Km(3.0, 10), togetherLobbyLine(active.copy(goalShare = 0.6), 5.0, course = true))
        // 목표를 아직 모름(글을 못 읽음) · 예전 규칙 · 서버에서 받지 않은 규칙
        assertNull(togetherLobbyLine(active, null, course = false))
        assertNull(togetherLobbyLine(TogetherRule.LEGACY.copy(fromServer = true), 5.0, course = true))
        assertNull(togetherLobbyLine(TogetherRule.FALLBACK, 5.0, course = true))
        assertNull(togetherLobbyLine(null, 5.0, course = true))
    }

    private fun outcome(
        reason: String? = null,
        status: CrewBonusStatus = CrewBonusStatus.CONFIRMED,
        count: Int = 1,
        total: Double? = 40.0,
        verdict: String = "CLEAN",
        courseKm: Double? = null,
        settled: Boolean = true,
    ) = CrewRunOutcome(
        settled = settled, crewId = "c", crewName = "크루", verdict = verdict, reason = "", terminal = null, participants = 3,
        base = 40.0, headcountPct = 0.0, headcount = 0.0, levelPct = 0.0, level = 0.0, total = total,
        bonus = CrewBonus(status, null, count, 0.0, reason), contribution = 150.0, consumed = 100.0, activeSec = 1800.0, pool = null,
        xpM = null, xpApplied = true, levelBefore = 1, levelAfter = 1, runKm = null, nextLevelKm = null, nextEligibleAtMs = null,
        courseKm = courseKm,
    )

    @Test fun `크루 결과 — 함께 달린 인원에 못 들면 차분한 한 줄, 아니면 더하는 시간 안에만 N명`() {
        val start = 1_800_000_000_000L
        val hour = 3_600_000L
        assertEquals(
            TogetherResultLine.NotTogether(10, course = true),
            togetherResultLine(active, outcome(reason = BONUS_NOT_TOGETHER, courseKm = 5.0), start, start + hour),
        )
        assertEquals(
            TogetherResultLine.NotTogether(10, course = false),
            togetherResultLine(active, outcome(reason = BONUS_NOT_TOGETHER), start, start + 30 * hour),
        )
        assertEquals(TogetherResultLine.Counting(3), togetherResultLine(active, outcome(count = 3), start, start + hour))
        assertEquals(TogetherResultLine.Counting(1), togetherResultLine(active, outcome(reason = "TOO_FEW"), start, start + hour))
        // 24시간이 지나면 더해지지 않는다 — 줄을 두지 않는다
        assertNull(togetherResultLine(active, outcome(count = 3), start, start + 24 * hour))
        // 서버가 아직 나누지 않음 · 판정 전 · 무효 · 적립 0 · 예전 규칙 · 서버에서 받지 않은 규칙
        assertNull(togetherResultLine(active, outcome(settled = false), start, start + hour))
        assertNull(togetherResultLine(active, outcome(status = CrewBonusStatus.PENDING), start, start + hour))
        assertNull(togetherResultLine(active, outcome(verdict = "VOID"), start, start + hour))
        assertNull(togetherResultLine(active, outcome(total = 0.0), start, start + hour))
        assertNull(togetherResultLine(TogetherRule.LEGACY.copy(fromServer = true), outcome(count = 3), start, start + hour))
        assertNull(togetherResultLine(TogetherRule.FALLBACK, outcome(count = 3), start, start + hour))
    }
}
