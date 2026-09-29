package com.stepup.android

import com.stepup.android.data.remote.AuthSession
import com.stepup.android.data.remote.AuthSessionStore
import com.stepup.android.data.remote.AuthUser
import com.stepup.android.data.remote.ChatNoticeRow
import com.stepup.android.data.remote.CrewChatApi
import com.stepup.android.data.remote.CrewHomeApi
import com.stepup.android.data.remote.CrewHomeRow
import com.stepup.android.data.remote.HttpPoster
import com.stepup.android.data.remote.HttpResponse
import com.stepup.android.data.remote.SessionHolder
import com.stepup.android.data.remote.StepUpServer
import com.stepup.android.data.remote.SupabaseAuth
import com.stepup.android.data.remote.WeekRow
import com.stepup.android.data.remote.serverJson
import com.stepup.android.data.repo.CrewHomeRepository
import com.stepup.android.data.repo.HomeOutcome
import com.stepup.android.data.repo.HomeProblem
import com.stepup.android.data.repo.homeRejection
import com.stepup.android.domain.CrewHomeRules
import com.stepup.android.domain.CrewRunsScope
import com.stepup.android.domain.GeoPoint
import com.stepup.android.domain.MeetingResponse
import com.stepup.android.ui.AppChromePolicy
import com.stepup.android.ui.screens.community.home.CrewHomeRoutes
import com.stepup.android.ui.screens.community.home.endsWithConsonant
import java.time.LocalDate
import java.time.ZoneOffset
import kotlinx.coroutines.async
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * 내 크루 홈(확정 4번) — 서버 답 읽기(0049 모양 그대로), 참석 응답이 홈에 반영되는 방식, 실패 · 접근 종료의 결말,
 * 주간 기록의 오지 않은 날, 막대 · 페이스 · 코스, 하단 탭 규칙.
 */
class CrewHomeTest {

    /** 주소마다 답을 고른다 — 마지막으로 보낸 몸통을 기억한다 */
    private class Router : HttpPoster {
        val answers = mutableMapOf<String, HttpResponse>()
        val bodies = mutableMapOf<String, String>()

        override suspend fun post(url: String, body: String, headers: Map<String, String>): HttpResponse {
            val name = url.substringAfterLast('/')
            bodies[name] = body
            return answers[name] ?: HttpResponse(500, "{}")
        }

        override suspend fun get(url: String, headers: Map<String, String>) = post(url, "", headers)
    }

    private class LoggedIn : AuthSessionStore {
        override suspend fun load() = AuthSession(
            accessToken = "token", refreshToken = "refresh", expiresIn = 3600, expiresAt = 9_999_999_999L,
            user = AuthUser(id = "u-doyun", isAnonymous = false),
        )

        override suspend fun save(session: AuthSession) = Unit
        override suspend fun clear() = Unit
    }

    private fun server(http: HttpPoster) = StepUpServer(
        baseUrl = "https://test.supabase.co",
        apiKey = "sb_publishable_test",
        sessions = SessionHolder(auth = SupabaseAuth("https://test.supabase.co", "sb_publishable_test", http), store = LoggedIn(), now = { 1_000L }),
        http = http,
    )

    private fun meetingJson(attendees: Int, response: String?) = """
        {"id":501,"crew_id":"c1","title":"퇴근 후, 가볍게 3km","body":"처음 오셔도 괜찮아요.","place":"공덕역 2번 출구",
         "lat":37.5446,"lng":126.9515,"distance_km":3,"meet_at":"2026-09-29T10:30:00+00:00","capacity":10,
         "host_id":"u-junho","host_name":"준호","host_owner":true,"attendees":$attendees,
         "faces":[{"user_id":"u-junho","name":"준호"},{"user_id":"u-jiyeon","name":"지연"}],
         "my_response":${response?.let { "\"$it\"" } ?: "null"},"open":true}
    """.trimIndent()

    private fun homeJson(meeting: String?) = """
        {"crew_id":"c1","role":"MEMBER","owner_id":"u-junho","member_count":25,"unread":3,
         "meeting":${meeting ?: "null"},
         "week":{"week_start":"2026-09-28","km":126.0,"runners":9,"goal_km":160},
         "notice":{"id":61,"crew_id":"c1","title":"오늘도 천천히 3km 함께 달려요","body":"오늘 저녁 7시 30분에 만나요.","pinned":true,
                   "author_id":"u-junho","author_name":"준호","created_at":"2026-09-29T09:00:00+00:00","updated_at":"2026-09-29T09:00:00+00:00",
                   "meeting":{"id":501,"title":"퇴근 후, 가볍게 3km","place":"공덕역 2번 출구","meet_at":"2026-09-29T10:30:00+00:00"}}}
    """.trimIndent()

    @Test
    fun `홈 답을 읽는다 — 역할 · 미확인 · 모임 · 주간 · 공지에 이은 모임`() {
        val home = serverJson.decodeFromString<CrewHomeRow>(homeJson(meetingJson(8, null))).toDomain()
        assertFalse(home.owner)
        assertEquals(25, home.memberCount)
        assertEquals(3, home.unread)
        assertEquals(8, home.meeting?.attendees)
        assertNull(home.meeting?.myResponse)
        assertEquals(GeoPoint(37.5446, 126.9515), home.meeting?.point)
        assertEquals(10, home.meeting?.capacity)
        assertEquals(79, home.progress?.percent)
        assertEquals(501L, home.notice?.meeting?.id)
        assertEquals("공덕역 2번 출구", home.notice?.meeting?.place)
    }

    @Test
    fun `모임이 없어도 주간 기록은 그대로 · 목표가 없으면 나누지 않는다`() {
        val row = serverJson.decodeFromString<CrewHomeRow>(
            """{"crew_id":"c1","role":"OWNER","owner_id":"u-doyun","member_count":1,"unread":0,"meeting":null,
                "week":{"week_start":"2026-09-28","km":12.5,"runners":1,"goal_km":null},"notice":null}""",
        )
        val home = row.toDomain()
        assertTrue(home.owner)
        assertNull(home.meeting)
        assertNull(home.notice)
        assertEquals(12.5, home.weekKm, 1e-9)
        assertNull(home.progress)
    }

    @Test
    fun `주간 기록 — 오지 않은 요일은 null 로 남고(0km 와 다르다) 모자란 칸은 채운다`() {
        val week = serverJson.decodeFromString<WeekRow>(
            """{"week_start":"2026-09-28","this_week":true,"goal_km":160,"km":126.0,"runners":9,
                "days":[61.2,64.8,null,null,null,null,null],
                "members":[{"user_id":"u-jiyeon","name":"지연","km":18.6,"runs":3,"last_km":5.2,"last_at":"2026-09-29T09:15:00+00:00"}],
                "weeks":["2026-09-28","2026-09-21","2026-09-14"]}""",
        ).toDomain()!!
        assertEquals(LocalDate.of(2026, 9, 28), week.start)
        assertEquals(LocalDate.of(2026, 10, 4), week.end)
        assertEquals(listOf(61.2, 64.8, null, null, null, null, null), week.days)
        assertEquals(34.0, week.progress!!.remainingKm, 1e-9)
        assertEquals(3, week.weeks.size)
        assertFalse(week.empty)
        val short = serverJson.decodeFromString<WeekRow>("""{"week_start":"2026-09-21","days":[20,18,0]}""").toDomain()!!
        assertEquals(listOf(20.0, 18.0, 0.0, null, null, null, null), short.days)
        assertTrue(short.empty)
    }

    @Test
    fun `막대는 가장 큰 날에 맞추고 목표를 넘어도 진행 바는 100 퍼센트까지`() {
        val bars = CrewHomeRules.bars(listOf(20.0, 18.0, 0.0, 28.0, 22.0, 34.0, 20.0))
        assertEquals(1f, bars[5]!!, 1e-6f)
        assertEquals(0f, bars[2]!!, 1e-6f)
        assertEquals(listOf(null, null), CrewHomeRules.bars(listOf(null, null)))
        val over = com.stepup.android.domain.CrewGoalProgress(100, 142.0)
        assertEquals(142, over.percent)
        assertEquals(1f, over.fraction, 1e-6f)
    }

    @Test
    fun `러닝 기록 — 시간 · 평균 페이스 · 서버 코스 글자`() {
        assertEquals("36:24", CrewHomeRules.clock(2184))
        assertEquals("1:02:03", CrewHomeRules.clock(3723))
        assertEquals(420, CrewHomeRules.pace(5200.0, 2184))
        assertEquals("7'00\"", CrewHomeRules.paceText(420))
        assertNull(CrewHomeRules.pace(20.0, 60))
        assertEquals(listOf(GeoPoint(37.50270, 127.0), GeoPoint(37.5153, 127.0)), CrewHomeRules.route("37.50270,127.00000;x,1;37.5153,127"))
        assertTrue(CrewHomeRules.route("").isEmpty())
    }

    @Test
    fun `모임 날짜는 오늘 · 내일 · 그 밖`() {
        val zone = ZoneOffset.UTC
        val now = LocalDate.of(2026, 9, 29).atTime(9, 0).toInstant(zone).toEpochMilli()
        val today = LocalDate.of(2026, 9, 29).atTime(19, 30).toInstant(zone).toEpochMilli()
        val tomorrow = LocalDate.of(2026, 9, 30).atTime(7, 0).toInstant(zone).toEpochMilli()
        val later = LocalDate.of(2026, 10, 2).atTime(7, 0).toInstant(zone).toEpochMilli()
        assertEquals(CrewHomeRules.When.TODAY, CrewHomeRules.whenOf(today, now, zone))
        assertEquals(CrewHomeRules.When.TOMORROW, CrewHomeRules.whenOf(tomorrow, now, zone))
        assertEquals(CrewHomeRules.When.LATER, CrewHomeRules.whenOf(later, now, zone))
    }

    @Test
    fun `이름 검색은 이름 안 어디든 · 빈 검색어는 모두`() {
        assertTrue(CrewHomeRules.matches("지연", "지"))
        assertTrue(CrewHomeRules.matches("Jiyeon", " yeon "))
        assertFalse(CrewHomeRules.matches("민수", "지연"))
        assertTrue(CrewHomeRules.matches("민수", "  "))
        assertTrue(CrewHomeRules.inWeek(LocalDate.of(2026, 10, 4), LocalDate.of(2026, 9, 28)))
        assertFalse(CrewHomeRules.inWeek(LocalDate.of(2026, 10, 5), LocalDate.of(2026, 9, 28)))
    }

    @Test
    fun `서버 거절의 결말 — 접근 종료 · 취소 · 마감 · 정원`() {
        assertEquals(HomeOutcome.Ended, homeRejection("crew_not_member"))
        assertEquals(HomeOutcome.Ended, homeRejection("chat_not_member"))
        assertEquals(HomeOutcome.Missing, homeRejection("meeting_missing"))
        assertEquals(HomeOutcome.Missing, homeRejection("run_missing"))
        assertEquals(HomeOutcome.Failed(HomeProblem.CLOSED), homeRejection("meeting_closed"))
        assertEquals(HomeOutcome.Failed(HomeProblem.FULL), homeRejection("정원이 찼습니다 (10명)"))
        assertEquals(HomeOutcome.Failed(HomeProblem.OTHER), homeRejection("invalid:week"))
    }

    @Test
    fun `참석이 저장되면 서버가 센 인원으로 홈의 모임도 바뀌고, 실패하면 그대로다`() = runBlocking {
        val http = Router()
        http.answers["crew_home"] = HttpResponse(200, homeJson(meetingJson(8, null)))
        val repo = CrewHomeRepository(CrewHomeApi(server(http)), owner = { "account:u-doyun" })
        assertTrue(repo.loadHome("c1") is HomeOutcome.Ok)
        assertEquals(8, repo.homeNow("c1")?.meeting?.attendees)

        http.answers["crew_meeting_respond"] = HttpResponse(200, meetingJson(9, "YES"))
        val yes = repo.respond("c1", 501, attend = true)
        assertTrue(yes is HomeOutcome.Ok)
        assertTrue(http.bodies["crew_meeting_respond"]!!.contains("\"p_attend\":true"))
        assertEquals(9, repo.homeNow("c1")?.meeting?.attendees)
        assertEquals(MeetingResponse.YES, repo.homeNow("c1")?.meeting?.myResponse)

        // 저장 실패(연결) — 이전 응답 · 인원이 그대로
        http.answers["crew_meeting_respond"] = HttpResponse(503, "{}")
        assertEquals(HomeOutcome.Failed(HomeProblem.NETWORK), repo.respond("c1", 501, attend = false))
        assertEquals(9, repo.homeNow("c1")?.meeting?.attendees)
        assertEquals(MeetingResponse.YES, repo.homeNow("c1")?.meeting?.myResponse)

        // 모임이 취소(삭제)됐다 — 홈에 남기지 않는다
        http.answers["crew_meeting_respond"] = HttpResponse(400, """{"message":"meeting_missing"}""")
        assertEquals(HomeOutcome.Missing, repo.respond("c1", 501, attend = false))
        assertNull(repo.homeNow("c1")?.meeting)
    }

    @Test
    fun `참석 저장 전에 시작한 모임 읽기가 늦게 와도 저장된 응답과 인원을 되돌리지 않는다`() = runBlocking {
        val gate = kotlinx.coroutines.CompletableDeferred<Unit>()
        val http = object : HttpPoster {
            val router = Router()
            override suspend fun post(url: String, body: String, headers: Map<String, String>): HttpResponse {
                // 모임 읽기는 참석 저장이 끝날 때까지 붙잡아 둔다 — 저장 전의 값(8명 · 미응답)을 늦게 돌려준다
                if (url.endsWith("/crew_meeting")) gate.await()
                return router.post(url, body, headers)
            }
            override suspend fun get(url: String, headers: Map<String, String>) = post(url, "", headers)
        }
        http.router.answers["crew_home"] = HttpResponse(200, homeJson(meetingJson(8, null)))
        http.router.answers["crew_meeting"] = HttpResponse(200, meetingJson(8, null))
        http.router.answers["crew_meeting_respond"] = HttpResponse(200, meetingJson(9, "YES"))
        val repo = CrewHomeRepository(CrewHomeApi(server(http)), owner = { "account:u-doyun" })
        assertTrue(repo.loadHome("c1") is HomeOutcome.Ok)

        val slowRead = async { repo.meeting("c1", 501) }
        kotlinx.coroutines.yield()
        assertTrue(repo.respond("c1", 501, attend = true) is HomeOutcome.Ok)
        gate.complete(Unit)
        val read = slowRead.await()

        assertEquals(9, (read as HomeOutcome.Ok).value.attendees)
        assertEquals(MeetingResponse.YES, read.value.myResponse)
        assertEquals(9, repo.homeNow("c1")?.meeting?.attendees)
        assertEquals(MeetingResponse.YES, repo.homeNow("c1")?.meeting?.myResponse)

        // 저장 뒤에 시작한 읽기는 서버 값 그대로(다른 사람이 참석해 10명)
        http.router.answers["crew_meeting"] = HttpResponse(200, meetingJson(10, "YES"))
        assertEquals(10, (repo.meeting("c1", 501) as HomeOutcome.Ok).value.attendees)
        assertEquals(10, repo.homeNow("c1")?.meeting?.attendees)
    }

    @Test
    fun `멤버가 아니게 되면 그 크루의 홈을 지우고 알린다`() = runBlocking {
        val http = Router()
        val ended = mutableListOf<String>()
        http.answers["crew_home"] = HttpResponse(200, homeJson(null))
        val repo = CrewHomeRepository(CrewHomeApi(server(http)), owner = { "account:u-doyun" }, onEnded = { ended += it })
        repo.loadHome("c1")
        http.answers["crew_week"] = HttpResponse(403, """{"message":"crew_not_member"}""")
        assertEquals(HomeOutcome.Ended, repo.week("c1", null))
        assertNull(repo.homeNow("c1"))
        assertEquals(listOf("c1"), ended)
    }

    @Test
    fun `최근 러닝이 없으면 비어 있는 성공 · 이어 읽기는 앞 장 마지막 줄 다음부터`() = runBlocking {
        val http = Router()
        val repo = CrewHomeRepository(CrewHomeApi(server(http)), owner = { "account:u-doyun" })
        http.answers["crew_member_last_run"] = HttpResponse(200, "null")
        assertEquals(HomeOutcome.Ok(null), repo.lastRun("c1", "u-minsu"))

        http.answers["crew_runs"] = HttpResponse(
            200,
            """[{"id":71,"user_id":"u-jiyeon","name":"지연","distance_m":5200,"duration_s":2184,
                 "started_at":"2026-09-29T08:39:00+00:00","ended_at":"2026-09-29T09:15:00.123456+00:00"}]""",
        )
        val first = repo.runs("c1", CrewRunsScope(week = LocalDate.of(2026, 9, 28)), after = null)
        val run = (first as HomeOutcome.Ok).value.single()
        assertEquals(420, run.paceSecPerKm)
        repo.runs("c1", CrewRunsScope(week = LocalDate.of(2026, 9, 28)), after = run)
        val body = http.bodies["crew_runs"]!!
        assertTrue(body, body.contains("\"p_before_at\":\"2026-09-29T09:15:00.123456+00:00\""))
        assertTrue(body, body.contains("\"p_before_id\":71"))
        assertTrue(body, body.contains("\"p_week\":\"2026-09-28\""))
    }

    @Test
    fun `공지에 모임을 이을 때만 모임 값을 보낸다(예전 방식 저장은 그대로)`() = runBlocking {
        val http = Router()
        val api = CrewChatApi(server(http))
        http.answers["crew_chat_notice_save"] = HttpResponse(200, """{"id":61,"crew_id":"c1","title":"t","body":"","pinned":true}""")
        api.noticeSave("c1", null, "t", "", true, null)
        assertFalse(http.bodies["crew_chat_notice_save"]!!.contains("p_meeting"))
        api.noticeSave("c1", 61, "t", "", true, null, meetingId = 501, meetingChange = true)
        assertTrue(http.bodies["crew_chat_notice_save"]!!.contains("\"p_meeting\":501"))
        assertTrue(http.bodies["crew_chat_notice_save"]!!.contains("\"p_meeting_change\":true"))
        api.noticeSave("c1", 61, "t", "", true, null, meetingId = null, meetingChange = true)
        assertTrue(http.bodies["crew_chat_notice_save"]!!.contains("\"p_meeting\":null"))
        val notice = serverJson.decodeFromString<ChatNoticeRow>("""{"id":60,"title":"처음 오시는 분들께","meeting":null}""").toDomain()
        assertNull(notice.meeting)
    }

    @Test
    fun `크루 이름의 받침으로 "이 · 가"를 고른다`() {
        assertTrue(endsWithConsonant("퇴근런"))
        assertFalse(endsWithConsonant("공덕크루"))
        assertTrue(endsWithConsonant("Runners"))
    }

    @Test
    fun `크루 홈 화면은 하단 탭 없이 커뮤니티에 속한다`() {
        // 사람 · 참여 기록 길은 android.net.Uri 로 만들어(JVM 검사에서는 없다) 모양 그대로 적는다
        for (route in listOf(
            CrewHomeRoutes.home("c1"), CrewHomeRoutes.intro("c1"), CrewHomeRoutes.meeting("c1", 501), CrewHomeRoutes.attendees("c1", 501),
            CrewHomeRoutes.place("c1", 501), CrewHomeRoutes.week("c1"), "crew/home/c1/runs?week=2026-09-28&day=&userId=&name=",
            "crew/home/c1/person/u-a", CrewHomeRoutes.run("c1", 71), CrewHomeRoutes.ENDED,
        )) {
            val destination = AppChromePolicy.destination(route)
            assertEquals(route, com.stepup.android.ui.Screen.Community, destination?.parent)
            assertFalse(route, destination!!.showBottomBar)
        }
    }
}
