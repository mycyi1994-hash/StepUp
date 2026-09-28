package com.stepup.android

import com.stepup.android.data.prefs.StoryRunJson
import com.stepup.android.data.remote.AuthSession
import com.stepup.android.data.remote.AuthSessionStore
import com.stepup.android.data.remote.AuthUser
import com.stepup.android.data.remote.CommunityApi
import com.stepup.android.data.remote.HttpPoster
import com.stepup.android.data.remote.HttpResponse
import com.stepup.android.data.remote.ServerResult
import com.stepup.android.data.remote.SessionHolder
import com.stepup.android.data.remote.StepUpServer
import com.stepup.android.data.remote.SupabaseAuth
import com.stepup.android.data.repo.toDomain
import com.stepup.android.domain.GeoPoint
import com.stepup.android.domain.StoryAttachment
import com.stepup.android.domain.StoryComposeRules
import com.stepup.android.domain.StoryLocalRuns
import com.stepup.android.domain.StoryPhraseSet
import com.stepup.android.domain.StoryPlaceSource
import com.stepup.android.domain.StoryRecordCard
import com.stepup.android.domain.StoryRecords
import com.stepup.android.domain.StoryRun
import com.stepup.android.domain.StoryRunOptions
import com.stepup.android.domain.StoryRunProblem
import com.stepup.android.domain.StoryRunRules
import java.time.LocalDate
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * 러닝 이야기(쉬운 글쓰기 상황별 시안 2026-09-28) — 3일 규칙 · 여덟 상황 · 한 줄 이야기 · 서버와 주고받는 모양.
 *
 * 9월 28일에는 9월 25일 기록을 붙일 수 있고, 9월 29일에 초안을 열면 같은 첨부가 만료된다. 조회 실패를 기록 없음으로
 * 다루지 않고, 경로 없는 기록에 코스 버튼을 만들지 않고, 사용자가 쓴 글을 덮어쓰지 않는다.
 */
class StoryRunsTest {

    private val sep28 = LocalDate.of(2026, 9, 28)
    private val sep29 = LocalDate.of(2026, 9, 29)
    private val sep25 = LocalDate.of(2026, 9, 25)
    private val route = listOf(GeoPoint(37.531, 126.931), GeoPoint(37.533, 126.935), GeoPoint(37.536, 126.939))

    private fun run(id: Long, day: LocalDate, withRoute: Boolean = true) = StoryRun(
        id = id, startedAt = 0, endedAt = 1, distanceMeters = 2100, durationSec = 1008, day = day,
        route = if (withRoute) route else emptyList(),
    )

    private fun ready(
        today: LocalDate = sep28,
        total: Int = 0,
        voided: Int = 0,
        lastDay: LocalDate? = null,
        runs: List<StoryRun> = emptyList(),
        local: StoryLocalRuns = StoryLocalRuns(),
    ) = StoryRecords.Ready(
        StoryRunOptions(today, total, voided, lastDay?.atTime(20, 0)?.atZone(StoryRunRules.ZONE)?.toInstant()?.toEpochMilli(), runs),
        local,
    )

    private fun card(
        attachment: StoryAttachment? = null,
        records: StoryRecords = ready(),
        problem: StoryRunProblem? = null,
        active: Boolean = false,
        deviceToday: LocalDate = sep28,
    ) = StoryComposeRules.card(attachment, problem, records, active, deviceToday)

    // ── 3일 ──────────────────────────────────────────────────────

    @Test fun `9월 28일에는 9월 25일 기록까지 붙일 수 있고 24일은 안 된다`() {
        assertTrue(StoryRunRules.attachable(sep25, sep28))
        assertTrue(StoryRunRules.attachable(sep28, sep28))
        assertFalse(StoryRunRules.attachable(LocalDate.of(2026, 9, 24), sep28))
        assertEquals(sep28, StoryRunRules.lastAttachDay(sep25))
    }

    @Test fun `9월 29일에는 같은 25일 기록이 4일 전이 되어 새로 붙일 수 없다`() {
        assertEquals(4L, StoryRunRules.daysAgo(sep25, sep29))
        assertFalse(StoryRunRules.attachable(sep25, sep29))
    }

    @Test fun `미래 날짜의 기록은 붙이지 않는다`() {
        assertFalse(StoryRunRules.attachable(sep29, sep28))
    }

    @Test fun `날짜는 한국 시간의 끝난 날로 센다`() {
        // 9월 25일 23:50 KST = 14:50 UTC — 기기 시간대와 상관없이 25일
        val ended = java.time.ZonedDateTime.of(2026, 9, 25, 23, 50, 0, 0, StoryRunRules.ZONE).toInstant().toEpochMilli()
        assertEquals(sep25, StoryRunRules.dayOf(ended))
    }

    // ── 여덟 상황 ───────────────────────────────────────────────────

    @Test fun `한 번도 뛰지 않은 사람 — 서버 0 · 폰에도 없음`() {
        assertEquals(StoryRecordCard.Never, card(records = ready(total = 0)))
        assertEquals(StoryPhraseSet.BEGINNER, StoryComposeRules.phraseSet(StoryRecordCard.Never))
    }

    @Test fun `서버엔 없지만 폰에 러닝이 있으면 처음이라 단정하지 않는다`() {
        assertEquals(StoryRecordCard.Neutral, card(records = ready(local = StoryLocalRuns(count = 2))))
        assertEquals(StoryRecordCard.Neutral, card(records = ready(voided = 1)))
    }

    @Test fun `4일 이상 지난 기록만 있는 사람 — 마지막 날짜와 며칠 전`() {
        val shown = card(records = ready(total = 6, lastDay = LocalDate.of(2026, 9, 24)))
        assertEquals(StoryRecordCard.Old(LocalDate.of(2026, 9, 24), 4), shown)
        assertEquals(StoryPhraseSet.RETURNING, StoryComposeRules.phraseSet(shown))
    }

    @Test fun `마지막 러닝이 3일 안인데 붙일 기록이 없으면 오래 쉬었다고 하지 않는다`() {
        assertEquals(StoryRecordCard.Neutral, card(records = ready(total = 3, lastDay = sep28)))
    }

    @Test fun `3일 전 기록이 있는 사람 — 붙어 있으면 코스 카드, 서버 목록의 값으로`() {
        val fresh = run(7, sep25).copy(distanceMeters = 2104)
        val shown = card(StoryAttachment(run(7, sep25)), ready(runs = listOf(fresh)))
        assertEquals(StoryRecordCard.Attached(StoryAttachment(fresh), 3), shown)
        assertEquals(StoryPhraseSet.RECENT, StoryComposeRules.phraseSet(shown))
    }

    @Test fun `임시글의 코스가 만료된 사람 — 조회에 실패해도 날짜로 안다`() {
        val attached = StoryAttachment(run(7, sep25))
        assertEquals(StoryRecordCard.Expired(attached.run, 4), card(attached, ready(today = sep29)))
        assertEquals(StoryRecordCard.Expired(attached.run, 4), card(attached, StoryRecords.Failed, deviceToday = sep29))
        assertEquals(StoryPhraseSet.RECENT, StoryComposeRules.phraseSet(StoryRecordCard.Expired(attached.run, 4)))
    }

    @Test fun `달렸지만 경로가 없는 기록은 코스 주변 버튼을 만들지 않는다`() {
        val summary = StoryAttachment(run(8, sep28, withRoute = false))
        val shown = card(summary, ready(runs = listOf(summary.run)))
        assertTrue(shown is StoryRecordCard.Attached)
        assertEquals(listOf(StoryPlaceSource.NEARBY, StoryPlaceSource.RECENT, StoryPlaceSource.SEARCH),
            StoryComposeRules.placeSources(shown, hasRecent = true))
    }

    @Test fun `기록을 불러오지 못한 사람 — 기록 없음으로 다루지 않는다`() {
        assertEquals(StoryRecordCard.FetchError, card(records = StoryRecords.Failed))
        assertEquals(StoryPhraseSet.BEGINNER, StoryComposeRules.phraseSet(StoryRecordCard.FetchError))
    }

    @Test fun `불러오는 중에는 처음 · 오래 쉼으로 나누지 않고 붙인 것도 지우지 않는다`() {
        assertEquals(StoryRecordCard.Loading, card(records = StoryRecords.Loading))
        assertNull(StoryComposeRules.phraseSet(StoryRecordCard.Loading))
        val attached = StoryAttachment(run(7, sep28))
        assertTrue(card(attached, StoryRecords.Loading) is StoryRecordCard.Attached)
    }

    @Test fun `조회 목록에 없는 기록(무효 · 다른 계정)은 붙일 수 없다`() {
        assertEquals(StoryRecordCard.Invalid(run(9, sep28)), card(StoryAttachment(run(9, sep28)), ready(runs = listOf(run(7, sep28)))))
    }

    @Test fun `서버가 게시 때 기간 초과라고 하면 만료로 보인다`() {
        val attached = StoryAttachment(run(7, sep25))
        assertTrue(card(attached, ready(runs = listOf(attached.run)), StoryRunProblem.EXPIRED) is StoryRecordCard.Expired)
    }

    @Test fun `이미 올린 글의 첨부는 기간이 지나도 그대로다`() {
        val kept = StoryAttachment(run(0, LocalDate.of(2026, 9, 1)), kept = true)
        assertEquals(StoryRecordCard.Attached(kept, 27), card(kept, ready()))
    }

    @Test fun `러닝이 진행 중이면 붙일 기록 대신 러닝 화면으로 돌아가게 한다`() {
        assertEquals(StoryRecordCard.ActiveRun, card(records = ready(), active = true))
        assertTrue(card(records = ready(runs = listOf(run(7, sep28))), active = true) is StoryRecordCard.Available)
    }

    @Test fun `방금 달린 러닝이 서버 확인 전이면 확인 중으로 보인다`() {
        assertEquals(StoryRecordCard.Pending, card(records = ready(total = 5, lastDay = LocalDate.of(2026, 9, 20),
            local = StoryLocalRuns(count = 6, pendingRecent = true))))
    }

    // ── 장소 버튼 · 올리기 ─────────────────────────────────────────

    @Test fun `장소 버튼은 코스 주변 · 내 주변 · 최근 장소 · 직접 검색 순으로 셋까지`() {
        val attached = card(StoryAttachment(run(7, sep28)), ready(runs = listOf(run(7, sep28))))
        assertEquals(listOf(StoryPlaceSource.COURSE, StoryPlaceSource.NEARBY, StoryPlaceSource.RECENT),
            StoryComposeRules.placeSources(attached, hasRecent = true))
        assertEquals(listOf(StoryPlaceSource.NEARBY, StoryPlaceSource.SEARCH),
            StoryComposeRules.placeSources(StoryRecordCard.Never, hasRecent = false))
        // 만료된 첨부에는 코스 주변이 없다
        assertEquals(listOf(StoryPlaceSource.NEARBY, StoryPlaceSource.RECENT, StoryPlaceSource.SEARCH),
            StoryComposeRules.placeSources(StoryRecordCard.Expired(run(7, sep25), 4), hasRecent = true))
    }

    @Test fun `장소와 본문이 있으면 코스 없이도 올릴 수 있다`() {
        assertTrue(StoryComposeRules.canPost(StoryRecordCard.FetchError, true, "오늘 날씨에 달리기 괜찮을까요?", ""))
        assertTrue(StoryComposeRules.canPost(StoryRecordCard.Never, true, "러닝을 시작해요.", ""))
        assertFalse("장소가 없으면 막는다", StoryComposeRules.canPost(StoryRecordCard.Never, false, "글", ""))
        assertFalse("입력이 비면 막는다", StoryComposeRules.canPost(StoryRecordCard.Never, true, "  ", ""))
    }

    @Test fun `유효한 기록만 붙였으면 기록 문장으로 올릴 수 있고 만료 · 무효 첨부는 정리해야 한다`() {
        val attached = StoryRecordCard.Attached(StoryAttachment(run(7, sep28)), 0)
        assertTrue(StoryComposeRules.canPost(attached, true, "", "9월 28일 2.10km 달렸어요."))
        assertFalse(StoryComposeRules.canPost(StoryRecordCard.Expired(run(7, sep25), 4), true, "9월 25일 마포대교에서 2.10km 달렸어요.", ""))
        assertFalse(StoryComposeRules.canPost(StoryRecordCard.Invalid(run(7, sep28)), true, "글", ""))
    }

    // ── 한 줄 이야기 ───────────────────────────────────────────────

    @Test fun `빈 본문이나 앱이 넣은 글 그대로면 문구로 바꾼다`() {
        val start = "러닝을 시작해요.\n처음에는 얼마나 뛰면 좋을까요?"
        assertEquals(start, StoryComposeRules.insertStarter("", "", start)?.text)
        val swapped = StoryComposeRules.insertStarter(start, start, "오늘 날씨에 달리기 괜찮을까요?")
        assertEquals("오늘 날씨에 달리기 괜찮을까요?", swapped?.text)
        assertEquals("오늘 날씨에 달리기 괜찮을까요?", swapped?.autoText)
    }

    @Test fun `사용자가 쓴 글은 덮어쓰지 않고 한 줄 아래 붙이며 같은 문구를 두 번 넣지 않는다`() {
        val mine = "퇴근하고 한강에 왔어요"
        val added = StoryComposeRules.insertStarter(mine, "", "오늘 날씨에 달리기 괜찮을까요?")
        assertEquals("퇴근하고 한강에 왔어요\n오늘 날씨에 달리기 괜찮을까요?", added?.text)
        assertEquals("", added?.autoText)
        assertNull(StoryComposeRules.insertStarter(added!!.text, "", "오늘 날씨에 달리기 괜찮을까요?"))
    }

    @Test fun `문장에 넣는 장소 · 거리 · 페이스`() {
        assertEquals("마포대교", StoryComposeRules.shortPlace("마포대교 남단"))
        assertEquals("여의도", StoryComposeRules.shortPlace("여의도 한강공원"))
        assertEquals("여의도공원", StoryComposeRules.shortPlace("여의도공원"))
        assertEquals("2.10", StoryComposeRules.km(2100))
        assertEquals("3.24", StoryComposeRules.km(3240))
        assertEquals("8′00″/km", StoryComposeRules.pace(run(1, sep25).paceSecPerKm))
        assertEquals("7′30″/km", StoryComposeRules.pace(1458.0 / 3.24))
        assertNull(StoryComposeRules.pace(null))
    }

    // ── 임시저장 · 서버 ────────────────────────────────────────────

    @Test fun `붙인 러닝은 저장 상태에서 날짜 · 코스 · 유지 표시까지 그대로 돌아온다`() {
        val attachment = StoryAttachment(run(7, sep25), kept = false)
        assertEquals(attachment, StoryRunJson.decode(StoryRunJson.encode(attachment)))
        val kept = StoryAttachment(run(0, sep25, withRoute = false), kept = true)
        assertEquals(kept, StoryRunJson.decode(StoryRunJson.encode(kept)))
        assertNull(StoryRunJson.decode(""))
        assertEquals("", StoryRunJson.encode(null))
    }

    private class FakeHttp(private val answer: (String, String) -> HttpResponse) : HttpPoster {
        val bodies = mutableListOf<String>()
        var lastUrl = ""
        override suspend fun post(url: String, body: String, headers: Map<String, String>): HttpResponse {
            lastUrl = url
            bodies += body
            return answer(url, body)
        }
        override suspend fun get(url: String, headers: Map<String, String>) = post(url, "", headers)
    }

    private object LoggedIn : AuthSessionStore {
        override suspend fun load() = AuthSession("token", "refresh", 3600, 9_999_999_999L, AuthUser(id = "me"))
        override suspend fun save(session: AuthSession) = Unit
        override suspend fun clear() = Unit
    }

    private fun api(http: FakeHttp) = CommunityApi(
        StepUpServer(
            baseUrl = "https://test.supabase.co",
            apiKey = "sb_publishable_test",
            sessions = SessionHolder(SupabaseAuth("https://test.supabase.co", "sb_publishable_test", http), LoggedIn, now = { 1_000L }),
            http = http,
        ),
    )

    @Test fun `기록 칸은 서버의 오늘 · 전체 수 · 붙일 수 있는 러닝을 읽는다`() = runBlocking {
        val body = """{"today":"2026-09-28","total":6,"voided":1,"last_ended_at":"2026-09-25T11:30:00.123+00:00",
            "runs":[{"id":41,"started_at":"2026-09-25T11:13:00+00:00","ended_at":"2026-09-25T11:30:00+00:00",
                     "distance_m":2105,"duration_s":1008,"day":"2026-09-25","route":"37.53100,126.93100;37.53300,126.93500"},
                    {"id":42,"started_at":"2026-09-28T01:00:00+00:00","ended_at":"2026-09-28T01:24:18+00:00",
                     "distance_m":3240,"duration_s":1458,"day":"2026-09-28","route":""}]}"""
        val http = FakeHttp { _, _ -> HttpResponse(200, body) }
        val options = (api(http).storyRuns() as ServerResult.Ok).value.toDomain()!!
        assertTrue(http.lastUrl.endsWith("/rpc/story_runs"))
        assertEquals(sep28, options.today)
        assertEquals(6, options.total)
        assertEquals(1, options.voided)
        assertEquals(listOf(41L, 42L), options.runs.map { it.id })
        assertEquals(sep25, options.runs[0].day)
        assertEquals(listOf(GeoPoint(37.531, 126.931), GeoPoint(37.533, 126.935)), options.runs[0].route)
        assertFalse("경로 없는 러닝에는 코스 그림이 없다", options.runs[1].hasRoute)
    }

    @Test fun `러닝을 붙여 쓰면 서버 번호와 요청 키를 함께 보낸다`() = runBlocking {
        val http = FakeHttp { _, _ -> HttpResponse(200, "601") }
        val result = api(http).createStory("오늘 여의도에서 3.24km 달렸어요.", "", "여의도 한강공원", "", 37.528, 126.934,
            runId = 42, clientKey = "aaaaaaaa-0000-0000-0000-000000000046")
        assertEquals(ServerResult.Ok(601L), result)
        val sent = http.bodies.single()
        assertTrue(sent, sent.startsWith("""{"p_run":42,"p_client_key":"aaaaaaaa-0000-0000-0000-000000000046","p_title":"""))
    }

    @Test fun `서버에 새 함수가 아직 없으면 러닝 없는 글은 예전 모양으로 다시 보낸다`() = runBlocking {
        val http = FakeHttp { _, body ->
            if ("p_client_key" in body) HttpResponse(404, """{"code":"PGRST202","message":"Could not find the function public.story_create(p_body, p_client_key, p_lat, p_lng, p_place, p_place_address, p_title) in the schema cache"}""")
            else HttpResponse(200, "602")
        }
        val result = api(http).createStory("글", "", "여의도공원", "", 37.5, 126.9, clientKey = "k")
        assertEquals(ServerResult.Ok(602L), result)
        assertEquals(2, http.bodies.size)
        assertFalse(http.bodies[1].contains("p_client_key"))
    }

    @Test fun `고칠 때 러닝을 건드리지 않으면 예전 모양, 빼면 p_run 비우고 p_run_change`() = runBlocking {
        val http = FakeHttp { _, _ -> HttpResponse(204, "") }
        api(http).updateStory(304, "제목", "", "여의도공원", "", 37.5261, 126.9225)
        assertFalse(http.bodies.last().contains("p_run"))
        api(http).updateStory(304, "제목", "", "여의도공원", "", 37.5261, 126.9225, runChange = true, runId = null)
        assertTrue(http.bodies.last(), http.bodies.last().startsWith("""{"p_post":304,"p_run":null,"p_run_change":true,"p_title":"제목""""))
    }

    @Test fun `글 줄의 러닝(0046)을 읽고 예전 서버 · 첨부 없는 글은 null`() = runBlocking {
        val feed = """
            [{"id":1,"category":"FREE","title":"9월 25일 마포대교에서 2.10km 달렸어요.","place":"마포대교 남단",
              "lat":37.53,"lng":126.93,"created_at":"2026-09-25T12:00:00Z",
              "run_started_at":"2026-09-25T11:13:00+00:00","run_ended_at":"2026-09-25T11:30:00+00:00",
              "run_distance_m":2105,"run_duration_s":1008,"run_route":"37.531,126.931;37.533,126.935"},
             {"id":2,"category":"FREE","title":"오늘 3.24km 달렸어요.","created_at":"2026-09-28T02:00:00Z",
              "run_started_at":"2026-09-28T01:00:00+00:00","run_ended_at":"2026-09-28T01:24:18+00:00",
              "run_distance_m":3240,"run_duration_s":1458,"run_route":""},
             {"id":3,"category":"FREE","title":"예전 글","created_at":"2026-09-27T07:00:00+00:00"}]
        """.trimIndent()
        val posts = (api(FakeHttp { _, _ -> HttpResponse(200, feed) }).posts() as ServerResult.Ok).value.map { it.toDomain() }
        assertEquals(2105, posts[0].run?.distanceMeters)
        assertEquals(sep25, posts[0].run?.day)
        assertTrue(posts[0].run!!.hasRoute)
        assertFalse(posts[1].run!!.hasRoute)
        assertEquals(1458, posts[1].run?.durationSec)
        assertNull(posts[2].run)
    }
}
