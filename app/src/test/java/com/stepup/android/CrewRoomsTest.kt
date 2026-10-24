package com.stepup.android

import com.stepup.android.data.remote.AuthSession
import com.stepup.android.data.remote.AuthSessionStore
import com.stepup.android.data.remote.AuthUser
import com.stepup.android.data.remote.HttpPoster
import com.stepup.android.data.remote.HttpResponse
import com.stepup.android.data.remote.PartyApi
import com.stepup.android.data.remote.ServerResult
import com.stepup.android.data.remote.SessionHolder
import com.stepup.android.data.remote.StepUpServer
import com.stepup.android.data.remote.SupabaseAuth
import com.stepup.android.data.repo.PartyMember
import com.stepup.android.data.repo.PartyPhase
import com.stepup.android.data.repo.PartyProblem
import com.stepup.android.data.repo.PartyState
import com.stepup.android.data.repo.joinProblem
import com.stepup.android.data.repo.leaveLateRoom
import com.stepup.android.data.repo.toDomain
import com.stepup.android.domain.CrewRoom
import com.stepup.android.domain.CrewRoomRules
import com.stepup.android.domain.GeoPoint
import com.stepup.android.domain.RoomCourse
import com.stepup.android.domain.RunCourse
import com.stepup.android.ui.screens.walk.FreeRunStartMotion
import com.stepup.android.ui.screens.walk.LobbyTerminal
import com.stepup.android.ui.screens.walk.LobbyTunnelMotion
import com.stepup.android.ui.screens.walk.crewLaunchClock
import com.stepup.android.ui.screens.walk.lobbyTerminal
import com.stepup.android.ui.screens.walk.poolNumber
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * 크루 러닝 대기실(서버 0072, 2026-10-24 핸드오프) — 열린 대기실 · 러닝 만들기 · 코스 · 출발 판 맞추기의 앱 쪽.
 * 서버 답 읽기 · 서버로 보내는 몸통 · 이름 · 경로 규칙(서버 economy.party_room_name · party_course_check 와 같다) ·
 * 고른 방의 막힘 까닭 · 대기실 터널 · 출발 연출 시계.
 */
class CrewRoomsTest {

    private class FakeHttp(private val answer: HttpResponse) : HttpPoster {
        var lastBody: String = ""
        var lastUrl: String = ""

        override suspend fun post(url: String, body: String, headers: Map<String, String>): HttpResponse {
            lastUrl = url
            lastBody = body
            return answer
        }

        override suspend fun get(url: String, headers: Map<String, String>) = post(url, "", headers)
    }

    private class LoggedIn : AuthSessionStore {
        override suspend fun load() = AuthSession(
            accessToken = "token", refreshToken = "refresh", expiresIn = 3600, expiresAt = 9_999_999_999L,
            user = AuthUser(id = "me", isAnonymous = false),
        )

        override suspend fun save(session: AuthSession) = Unit
        override suspend fun clear() = Unit
    }

    private fun api(http: FakeHttp) = PartyApi(
        StepUpServer(
            baseUrl = "https://test.supabase.co", apiKey = "sb_publishable_test",
            sessions = SessionHolder(SupabaseAuth("https://test.supabase.co", "sb_publishable_test", http), LoggedIn(), now = { 1_000L }),
            http = http,
        ),
    )

    /** party_rooms 가 실제로 내려주는 모양 — 내 방 하나 · 남의 방 하나 · 내보내진 방 하나 */
    private val rooms = """
        {"server_now":"2026-10-24T10:00:00+00:00","capacity":20,"done_today":false,
         "rooms":[
           {"id":11,"name":"여의도 퇴근런","status":"LOBBY","host_id":"u-h","host_name":"하나",
            "created_at":"2026-10-24T09:30:00+00:00","starts_at":null,"members_count":6,"ready_count":5,"capacity":20,
            "course_name":"여의도 강변 5km","course_km":5.000,"course_rev":2,"course_track":"37.526,126.923;37.5281,126.9311",
            "mine":true,"kicked":false},
           {"id":12,"name":"아침 런","status":"LOBBY","host_id":"u-x","host_name":"둘",
            "created_at":"2026-10-24T09:50:00+00:00","members_count":20,"ready_count":3,"capacity":20,
            "course_name":null,"course_km":null,"course_rev":0,"course_track":null,"mine":false,"kicked":false},
           {"id":13,"name":"저녁 런","status":"LOBBY","host_id":"u-y","host_name":"셋",
            "created_at":"2026-10-24T09:55:00+00:00","members_count":2,"ready_count":0,"capacity":20,
            "course_name":"코스","course_km":1,"course_rev":1,"course_track":"37.5,127.0;37.6,127.1","mine":false,"kicked":true}
         ]}
    """.trimIndent()

    @Test
    fun `열린 대기실 목록을 서버 이름 그대로 읽는다`() = runBlocking {
        val http = FakeHttp(HttpResponse(200, rooms))
        val result = api(http).rooms("00000000-0000-0000-0000-0000000000cc")
        assertTrue(result is ServerResult.Ok)
        val row = (result as ServerResult.Ok).value
        assertTrue(http.lastUrl.endsWith("/rpc/party_rooms"))
        assertTrue(http.lastBody.contains("\"p_crew\":\"00000000-0000-0000-0000-0000000000cc\""))
        assertEquals(3, row.rooms.size)
        assertFalse(row.doneToday)
        val mine = row.rooms[0].toDomain(offsetMs = 0)
        assertEquals("여의도 퇴근런", mine.name)
        assertEquals(2, mine.course?.rev)
        assertEquals(2, mine.course?.points?.size)
        assertEquals(5.0, mine.course!!.km, 1e-9)
        assertTrue(mine.mine)
        // 코스 없는 방(예전 · 판 0)은 코스를 두지 않는다
        assertNull(row.rooms[1].toDomain(0).course)
        assertTrue(row.rooms[1].toDomain(0).full)
    }

    @Test
    fun `목록 순서 - 내 방 먼저, 막 열린 방, 내보내진 방 · 출발한 방은 빼고`() = runBlocking {
        val row = (api(FakeHttp(HttpResponse(200, rooms))).rooms("c") as ServerResult.Ok).value
        val ordered = CrewRoomRules.ordered(row.rooms.map { it.toDomain(0) } + room(14, status = "RUNNING"))
        assertEquals(listOf(11L, 12L), ordered.map { it.id })
    }

    private fun room(id: Long, status: String = "LOBBY", mine: Boolean = false, created: Long = 0L) = CrewRoom(
        id = id, name = "방$id", status = status, hostId = "h", hostName = "h", createdAtMs = created, members = 2, ready = 1,
        capacity = 20, course = null, mine = mine, kicked = false,
    )

    @Test
    fun `러닝 만들기는 이름 · 코스 · 요청 번호를 보낸다`() = runBlocking {
        val http = FakeHttp(HttpResponse(200, "42"))
        val course = RoomCourse(0, "여의도 강변", 5.2, listOf(GeoPoint(37.526, 126.923), GeoPoint(37.5281, 126.9311)))
        val result = api(http).create("c", "여의도 퇴근런", course.name, course.km, course.encodeTrack(), "req-1")
        assertEquals(42L, (result as ServerResult.Ok).value)
        assertTrue(http.lastUrl.endsWith("/rpc/party_create"))
        for (part in listOf("\"p_name\":\"여의도 퇴근런\"", "\"p_course_name\":\"여의도 강변\"", "\"p_course_km\":5.2",
            "\"p_course_track\":\"37.526,126.923;37.5281,126.9311\"", "\"p_request\":\"req-1\"")) {
            assertTrue(part, http.lastBody.contains(part))
        }
    }

    @Test
    fun `고른 방 들어가기 · 코스 바꾸기 · 판 맞춰 출발`() = runBlocking {
        val join = FakeHttp(HttpResponse(200, "11"))
        assertEquals(11L, (api(join).join(11) as ServerResult.Ok).value)
        assertTrue(join.lastUrl.endsWith("/rpc/party_join"))
        val set = FakeHttp(HttpResponse(200, "3"))
        assertEquals(3, (api(set).setCourse(11, "선유도", 2.0, "37.5,126.9;37.6,126.8") as ServerResult.Ok).value)
        assertTrue(set.lastUrl.endsWith("/rpc/party_set_course"))
        val start = FakeHttp(HttpResponse(200, ""))
        assertTrue(api(start).startRev(11, 3) is ServerResult.Ok)
        assertTrue(start.lastUrl.endsWith("/rpc/party_start_rev"))
        assertTrue(start.lastBody.contains("\"p_course_rev\":3"))
        val course = FakeHttp(HttpResponse(200, """{"course_rev":3,"course_name":"선유도","course_km":2.0,"course_track":"37.5,126.9;37.6,126.8"}"""))
        val got = (api(course).course(11) as ServerResult.Ok).value
        assertEquals(3, got.courseRev)
        assertEquals(2, RunCourse.decode(got.courseTrack!!).size)
    }

    @Test
    fun `방 상태는 이름 · 코스 판을 함께 읽고 경로는 싣지 않는다`() = runBlocking {
        val state = """
            {"id":11,"status":"LOBBY","host_id":"u-h","crew_id":"c","flash_post_id":null,"starts_at":null,
             "server_now":"2026-10-24T10:00:00+00:00","name":"여의도 퇴근런","created_at":"2026-10-24T09:30:00+00:00",
             "course_name":"여의도 강변 5km","course_km":5.0,"course_rev":2,"members":[]}
        """.trimIndent()
        val row = (api(FakeHttp(HttpResponse(200, state))).state(11) as ServerResult.Ok).value
        assertEquals("여의도 퇴근런", row.name)
        assertEquals(2, row.courseRev)
        assertEquals("여의도 강변 5km", row.courseName)
    }

    @Test
    fun `방 이름 규칙은 서버와 같다 - 공백 정리 · 1 ~ 30자(글자로 센다)`() {
        assertEquals("여의도 퇴근런", CrewRoomRules.normalizeName("  여의도   퇴근런 "))
        assertFalse(CrewRoomRules.nameValid("   "))
        assertTrue(CrewRoomRules.nameValid("가".repeat(30)))
        assertFalse(CrewRoomRules.nameValid("가".repeat(31)))
        // 이모지는 글자 하나(코드 포인트) — 서버 char_length 와 같다
        assertEquals(2, CrewRoomRules.nameLength("🏃런"))
    }

    @Test
    fun `경로는 지수 없이 소수 6자리 · 서버 한도 안으로 줄인다`() {
        val tiny = RoomCourse(0, "c", 1.0, listOf(GeoPoint(0.00001, -0.0000001), GeoPoint(37.1234567, 127.0)))
        assertEquals("0.00001,0;37.123457,127", tiny.encodeTrack())
        val pattern = Regex("^-?[0-9]+(\\.[0-9]+)?,-?[0-9]+(\\.[0-9]+)?(;-?[0-9]+(\\.[0-9]+)?,-?[0-9]+(\\.[0-9]+)?)+$")
        assertTrue(pattern.matches(tiny.encodeTrack()))
        val long = (0 until 10_000).map { GeoPoint(37.0 + it * 1e-5, 127.0 + it * 1e-5) }
        val fitted = CrewRoomRules.fitTrack(long)
        assertEquals(CrewRoomRules.TRACK_MAX_POINTS, fitted.size)
        assertEquals(long.first(), fitted.first())
        assertEquals(long.last(), fitted.last())
        val course = RoomCourse.of(RunCourse(1, "가".repeat(80), "", 5.0, 0, long, "", true, false, 0, false, 0, 0))
        assertTrue(course.encodeTrack().length <= CrewRoomRules.TRACK_MAX_CHARS)
        assertEquals(CrewRoomRules.COURSE_NAME_MAX, course.name.length)
    }

    @Test
    fun `고른 방에 들어가다 막힌 까닭 - 찼다 · 출발했다 · 닫혔다 · 그 밖`() {
        assertEquals(PartyProblem.FULL, joinProblem("room_full"))
        assertEquals(PartyProblem.STARTED, joinProblem("room_started"))
        assertEquals(PartyProblem.CLOSED, joinProblem("room_closed"))
        assertEquals(PartyProblem.REMOVED, joinProblem("진행자가 내보낸 방입니다"))
    }

    private fun member(id: String, me: Boolean = false, host: Boolean = false, ready: Boolean = false) =
        PartyMember(id, id, ready = ready, isMe = me, isHost = host)

    @Test
    fun `참가자에게만 바뀐 코스를 알린다 - 본 판보다 높을 때`() {
        val base = PartyState(
            phase = PartyPhase.LOBBY, partyId = 11, crewId = "c", pickedRoom = 11,
            members = listOf(member("h", host = true), member("me", me = true)),
            course = RoomCourse(3, "선유도", 2.0), courseRev = 3, courseSeenRev = 2,
        )
        assertTrue(base.courseChanged)
        assertFalse(base.copy(courseSeenRev = 3).courseChanged)
        // 진행자 본인 · 출발 뒤에는 알리지 않는다
        assertFalse(base.copy(members = listOf(member("me", me = true, host = true))).courseChanged)
        assertFalse(base.copy(phase = PartyPhase.COUNTDOWN).courseChanged)
        // 진행자가 코스를 뺐다(0073) — 코스는 없어도 판이 올랐으니 알린다
        assertTrue(base.copy(course = null, courseRev = 4, courseSeenRev = 3).courseChanged)
        // 코스 없이 연 방(판 0)에 막 들어왔다 — 알릴 것이 없다
        assertFalse(base.copy(course = null, courseRev = 0, courseSeenRev = 0).courseChanged)
    }

    @Test
    fun `코스 없이 대기실을 연다 · 코스를 뺀다 - 코스 칸을 모두 null 로 보낸다(0073)`() = runBlocking {
        val create = FakeHttp(HttpResponse(200, "43"))
        assertEquals(43L, (api(create).create("c", "한강 자유런", null, null, null, "req-2") as ServerResult.Ok).value)
        for (part in listOf("\"p_name\":\"한강 자유런\"", "\"p_course_name\":null", "\"p_course_km\":null",
            "\"p_course_track\":null", "\"p_request\":\"req-2\"")) {
            assertTrue(part, create.lastBody.contains(part))
        }
        val remove = FakeHttp(HttpResponse(200, "4"))
        assertEquals(4, (api(remove).setCourse(11, null, null, null) as ServerResult.Ok).value)
        assertTrue(remove.lastUrl.endsWith("/rpc/party_set_course"))
        for (part in listOf("\"p_party\":11", "\"p_course_name\":null", "\"p_course_km\":null", "\"p_course_track\":null")) {
            assertTrue(part, remove.lastBody.contains(part))
        }
    }

    @Test
    fun `지도에 그린 코스 - 점 사이 거리의 합 · 점 둘 이상 · 0 보다 길고 100km 안`() {
        // 위도 0.01도 ≈ 1.112km — 꺾은선은 두 구간의 합
        val a = GeoPoint(37.50, 127.0)
        val b = GeoPoint(37.51, 127.0)
        val c = GeoPoint(37.52, 127.0)
        assertEquals(1.112, CrewRoomRules.pathKm(listOf(a, b)), 0.002)
        assertEquals(2.224, CrewRoomRules.pathKm(listOf(a, b, c)), 0.004)
        assertEquals(0.0, CrewRoomRules.pathKm(listOf(a)), 0.0)
        assertTrue(CrewRoomRules.drawnUsable(listOf(a, b)))
        assertFalse(CrewRoomRules.drawnUsable(listOf(a)))
        // 같은 자리를 두 번 — 거리 0 은 코스가 아니다
        assertFalse(CrewRoomRules.drawnUsable(listOf(a, a)))
        // 100km 넘게(위도 1도 ≈ 111km)는 서버가 받지 않는다
        assertFalse(CrewRoomRules.drawnUsable(listOf(GeoPoint(37.0, 127.0), GeoPoint(38.0, 127.0))))
        // 점이 너무 많으면 받지 않는다
        val many = (0..CrewRoomRules.DRAW_MAX_POINTS).map { GeoPoint(37.5 + it * 1e-4, 127.0) }
        assertFalse(CrewRoomRules.drawnUsable(many))
        val drawn = RoomCourse.drawn("  내가 그린 코스 ", listOf(a, b, c, a))
        assertEquals("내가 그린 코스", drawn.name)
        assertEquals(0, drawn.rev)
        assertEquals(4, drawn.points.size)
        assertEquals(CrewRoomRules.pathKm(listOf(a, b, c, a)), drawn.km, 1e-9)
        // 서버 경로 정규식이 받는 꼴
        val pattern = Regex("^-?[0-9]+(\\.[0-9]+)?,-?[0-9]+(\\.[0-9]+)?(;-?[0-9]+(\\.[0-9]+)?,-?[0-9]+(\\.[0-9]+)?)+$")
        assertTrue(pattern.matches(drawn.encodeTrack()))
    }

    @Test
    fun `대기실 대신 한 화면 - 고른 방의 막힘 · 들어가기 전 연결 실패만`() {
        val picked = PartyState(phase = PartyPhase.LOBBY, crewId = "c", pickedRoom = 11)
        assertEquals(LobbyTerminal.FULL, lobbyTerminal(picked.copy(problem = PartyProblem.FULL)))
        assertEquals(LobbyTerminal.STARTED, lobbyTerminal(picked.copy(problem = PartyProblem.STARTED)))
        assertEquals(LobbyTerminal.REMOVED, lobbyTerminal(picked.copy(problem = PartyProblem.REMOVED)))
        // 방 번호를 받기 전 연결 실패 = 불러오기 실패(69). 방 안에서 끊긴 것은 방 위 안내(방 화면을 그대로 둔다)
        assertEquals(LobbyTerminal.FAILED, lobbyTerminal(picked.copy(problem = PartyProblem.NETWORK)))
        assertNull(lobbyTerminal(picked.copy(partyId = 11, problem = PartyProblem.NETWORK)))
        assertNull(lobbyTerminal(picked.copy(partyId = 11, problem = PartyProblem.FAILED)))
        assertEquals(LobbyTerminal.MISSED, lobbyTerminal(picked.copy(problem = PartyProblem.MISSED_START)))
    }

    @Test
    fun `답을 늦게 받은 방 - 지금 고른 방이 아니면 나온다`() {
        val now = PartyState(phase = PartyPhase.LOBBY, crewId = "c", pickedRoom = 12)
        assertTrue(leaveLateRoom(now, "c", null, room = 11))
        assertFalse(leaveLateRoom(now, "c", null, room = 12))
    }

    @Test
    fun `공용 체력 숫자 - 천 단위 쉼표 · 소수 한 자리에서 버림`() {
        assertEquals("1,200", poolNumber(1200.0))
        assertEquals("720", poolNumber(720.0))
        assertEquals("1,428", poolNumber(1428.0))
        assertEquals("106.6", poolNumber(106.66666))
        assertEquals("0", poolNumber(-3.0))
    }

    @Test
    fun `대기실 터널 - 두 겹이 11초마다 다가오고 빛은 4점2초 왕복`() {
        // 0% 1 · 0 → 14% 1.03 · 1 → 70% 1.15 · 1 → 100% 1.23 · 0
        fun at(fraction: Double) = LobbyTunnelMotion.push((fraction * LobbyTunnelMotion.PUSH_MS).toLong(), 0f)
        assertEquals(1f, at(0.0).first, 1e-4f)
        assertEquals(0f, at(0.0).second, 1e-4f)
        assertEquals(1.03f, at(0.14).first, 1e-3f)
        assertEquals(1f, at(0.14).second, 1e-3f)
        assertEquals(1.15f, at(0.70).first, 1e-3f)
        assertEquals(1.23f, at(0.9999).first, 1e-2f)
        assertEquals(0f, at(0.9999).second, 1e-2f)
        // 두 겹은 위상이 반 바퀴 어긋난다(시작 10% · 60%)
        val a = LobbyTunnelMotion.push(0, LobbyTunnelMotion.PHASE_A)
        val b = LobbyTunnelMotion.push((0.5 * LobbyTunnelMotion.PUSH_MS).toLong(), LobbyTunnelMotion.PHASE_A)
        assertEquals(LobbyTunnelMotion.push(0, LobbyTunnelMotion.PHASE_B), b)
        assertTrue(a != b)
        // 왼쪽 빛 x +60 → -30 · 회전 -20 → -30 · 세기 .3 → 1, 오른쪽은 거울
        for (t in 0L..8_400L step 700L) {
            val l = LobbyTunnelMotion.side(t, gentle = false, left = true)
            val r = LobbyTunnelMotion.side(t, gentle = false, left = false)
            assertTrue(l.dx in -30f..60f)
            assertTrue(l.degrees in -30f..-20f)
            assertEquals(-l.dx, r.dx, 1e-4f)
            assertEquals(-l.degrees, r.degrees, 1e-4f)
            assertTrue(l.alpha in 0.3f..1f)
        }
    }

    @Test
    fun `모션 줄이기면 이동 · 회전 없이 빛의 세기만`() {
        for (t in 0L..20_000L step 1_000L) {
            val s = LobbyTunnelMotion.side(t, gentle = true, left = true)
            assertEquals(0f, s.dx, 0f)
            assertEquals(0f, s.degrees, 0f)
            assertTrue(s.alpha in 0.25f..0.65f)
            assertTrue(LobbyTunnelMotion.glow(t, gentle = true) in 0.4f..1f)
        }
    }

    @Test
    fun `출발 연출 시계 - 서버 출발 시각이 펼침 시작이다`() {
        val start = 100_000L
        assertEquals(FreeRunStartMotion.REVEAL_AT, crewLaunchClock(start, start))
        assertEquals(0L, crewLaunchClock(start, start - FreeRunStartMotion.REVEAL_AT))
        // 6초 앞서 정한 출발 — 처음 2.24초는 3 전(출발 신호를 맞추는 중)
        assertTrue(crewLaunchClock(start, start - 6_000L) < 0)
        assertEquals(3, FreeRunStartMotion.digitAt(crewLaunchClock(start, start - FreeRunStartMotion.REVEAL_AT + 10)))
        assertEquals(0, FreeRunStartMotion.digitAt(crewLaunchClock(start, start - 700)))
    }
}
