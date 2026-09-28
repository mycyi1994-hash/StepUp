package com.stepup.android

import com.stepup.android.data.prefs.CrewDraftCodec
import com.stepup.android.data.remote.AuthSession
import com.stepup.android.data.remote.AuthSessionStore
import com.stepup.android.data.remote.AuthUser
import com.stepup.android.data.remote.CrewApi
import com.stepup.android.data.remote.HttpPoster
import com.stepup.android.data.remote.HttpResponse
import com.stepup.android.data.remote.ServerResult
import com.stepup.android.data.remote.SessionHolder
import com.stepup.android.data.remote.StepUpServer
import com.stepup.android.data.remote.SupabaseAuth
import com.stepup.android.data.repo.CrewApplied
import com.stepup.android.data.repo.toCard
import com.stepup.android.domain.CrewApplicationStatus
import com.stepup.android.domain.CrewArea
import com.stepup.android.domain.CrewCapacityProblem
import com.stepup.android.domain.CrewCard
import com.stepup.android.domain.CrewDistance
import com.stepup.android.domain.CrewDraft
import com.stepup.android.domain.CrewDraftMode
import com.stepup.android.domain.CrewFooter
import com.stepup.android.domain.CrewGoalChoice
import com.stepup.android.domain.CrewGoalProgress
import com.stepup.android.domain.CrewImageChoice
import com.stepup.android.domain.CrewListQuery
import com.stepup.android.domain.CrewMood
import com.stepup.android.domain.CrewProblem
import com.stepup.android.domain.CrewRole
import com.stepup.android.domain.CrewRules
import com.stepup.android.domain.CrewSchedule
import com.stepup.android.domain.CrewSort
import com.stepup.android.domain.GeoPoint
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * 크루 명함형(확정 2번) — 화면이 서버 값에서 읽어 내는 규칙. 지시서의 확인 항목(달성률 · 남은 거리 · 100% 넘는 바 ·
 * 목표 없음 · 정원 · 역할별 아래 버튼 · 지역 범위 · 정렬 · 초안 보관)을 숫자로 확인한다.
 */
class CrewCardsTest {

    private fun card(
        id: String,
        name: String = id,
        role: CrewRole = CrewRole.VISITOR,
        lat: Double? = null,
        lng: Double? = null,
        members: Int = 1,
        capacity: Int? = null,
        recruiting: Boolean = true,
        openJoin: Boolean = false,
        weekKm: Double = 0.0,
        changedAt: Long = 0L,
        goalKm: Int? = null,
    ) = CrewCard(
        id = id, name = name, lat = lat, lng = lng, memberCount = members, capacity = capacity, recruiting = recruiting,
        openJoin = openJoin, weekKm = weekKm, recruitChangedAt = changedAt, goalKm = goalKm,
        owned = role == CrewRole.OWNER, joined = role == CrewRole.MEMBER || role == CrewRole.OWNER, requested = role == CrewRole.PENDING,
    )

    // ── 주간 목표 ──

    @Test fun `달성률은 반올림하고 남은 거리는 목표에서 뺀다`() {
        val before = CrewGoalProgress(160, 126.0)
        assertEquals(79, before.percent)
        assertEquals(34.0, before.remainingKm, 1e-9)
        // 목표를 200km 로 바꾸면 누적 거리는 그대로 63%, 74km 남음(시안 83)
        val after = CrewGoalProgress(200, 126.0)
        assertEquals(63, after.percent)
        assertEquals(74.0, after.remainingKm, 1e-9)
        assertFalse(after.reached)
    }

    @Test fun `100퍼센트를 넘으면 숫자는 그대로, 바는 끝까지만`() {
        val over = CrewGoalProgress(100, 131.4)
        assertEquals(131, over.percent)
        assertEquals(1f, over.fraction)
        assertEquals(0.0, over.remainingKm, 1e-9)
        assertTrue(over.reached)
    }

    @Test fun `목표가 없으면 달성률을 만들지 않는다(나누지 않는다)`() {
        assertNull(CrewRules.progress(card("a", goalKm = null, weekKm = 42.0)))
        assertEquals(21, CrewRules.progress(card("a", goalKm = 200, weekKm = 42.0))!!.percent)
    }

    @Test fun `거리 글자 — 100km 이상은 정수, 그 아래는 소수 한 자리`() {
        assertEquals("126", CrewRules.km(126.0))
        assertEquals("126", CrewRules.km(126.4))
        assertEquals("17.6", CrewRules.km(17.6))
        assertEquals("34", CrewRules.km(34.0))
        assertEquals("0", CrewRules.km(-3.0))
    }

    // ── 정원 · 아래 버튼 ──

    @Test fun `정원은 숫자만, 지금 인원보다 작을 수 없다`() {
        assertEquals(CrewCapacityProblem.EMPTY, CrewRules.capacityProblem("", 24))
        assertEquals(CrewCapacityProblem.INVALID, CrewRules.capacityProblem("0", 24))
        assertEquals(CrewCapacityProblem.INVALID, CrewRules.capacityProblem("스물", 24))
        assertEquals(CrewCapacityProblem.BELOW_MEMBERS, CrewRules.capacityProblem("20", 24))
        assertNull(CrewRules.capacityProblem("24", 24))
        assertNull(CrewRules.capacityProblem("30", 24))
    }

    @Test fun `아래 큰 버튼은 역할과 모집 상태를 따로 본다`() {
        assertEquals(CrewFooter.APPLY, CrewRules.footer(card("a", members = 24, capacity = 30)))
        assertEquals(CrewFooter.PAUSED, CrewRules.footer(card("a", recruiting = false)))
        assertEquals(CrewFooter.FULL, CrewRules.footer(card("a", members = 30, capacity = 30)))
        assertEquals(CrewFooter.JOIN_NOW, CrewRules.footer(card("a", openJoin = true)))
        assertEquals(CrewFooter.PENDING, CrewRules.footer(card("a", role = CrewRole.PENDING, members = 30, capacity = 30)))
        assertEquals(CrewFooter.MEMBER, CrewRules.footer(card("a", role = CrewRole.MEMBER, recruiting = false)))
        assertEquals(CrewFooter.OWNER, CrewRules.footer(card("a", role = CrewRole.OWNER)))
        assertEquals(CrewFooter.OWNER_PAUSED, CrewRules.footer(card("a", role = CrewRole.OWNER, recruiting = false)))
        // 정원이 없는 예전 크루는 마감되지 않는다
        assertFalse(card("a", members = 999, capacity = null).full)
    }

    // ── 목록 ──

    @Test fun `지역 범위 밖이나 위치 없는 크루는 빼고, 내 크루는 남긴다`() {
        val here = GeoPoint(37.5446, 126.9515) // 공덕
        val near = card("near", lat = 37.5460, lng = 126.9500)          // 약 0.2km
        val mid = card("mid", lat = 37.5700, lng = 126.9760)            // 약 3.6km
        val far = card("far", lat = 37.4979, lng = 127.0276)            // 약 8.6km
        val noPlace = card("noplace")
        val mine = card("mine", role = CrewRole.MEMBER, lat = 37.4979, lng = 127.0276)
        val visible = CrewRules.visible(listOf(far, mid, near, noPlace, mine), CrewListQuery(here, radiusKm = 5, sort = CrewSort.NEAR))
        assertEquals(listOf("near", "mid", "mine"), visible.map { it.id })
        // 1km 로 좁히면 가까운 크루와 내 크루만
        assertEquals(listOf("near", "mine"), CrewRules.visible(listOf(far, mid, near, noPlace, mine), CrewListQuery(here, 1)).map { it.id })
        // 기준 위치가 없으면 모든 크루(최근 모집순)
        assertEquals(5, CrewRules.visible(listOf(far, mid, near, noPlace, mine), CrewListQuery(null)).size)
    }

    @Test fun `정렬 — 활동 많은 순은 이번 주 거리, 최근 모집순은 모집 시작 시각`() {
        val a = card("a", weekKm = 10.0, changedAt = 3)
        val b = card("b", weekKm = 30.0, changedAt = 1)
        val c = card("c", weekKm = 20.0, changedAt = 2)
        assertEquals(listOf("b", "c", "a"), CrewRules.sort(listOf(a, b, c), CrewListQuery(null, sort = CrewSort.ACTIVE)).map { it.id })
        assertEquals(listOf("a", "c", "b"), CrewRules.sort(listOf(a, b, c), CrewListQuery(null, sort = CrewSort.RECENT)).map { it.id })
    }

    // ── 서버 줄 · 실패 이유 ──

    @Test fun `서버가 알려 준 실패를 고칠 칸으로 읽는다`() {
        assertEquals(CrewProblem.CREW_FULL, CrewProblem.of("crew_full"))
        assertEquals(CrewProblem.NAME, CrewProblem.of("invalid:name"))
        assertEquals(CrewProblem.CAPACITY_BELOW_MEMBERS, CrewProblem.of("capacity_below_members"))
        assertEquals(CrewProblem.CREW_CLOSED, CrewProblem.of("crew_closed"))
        assertEquals(CrewProblem.OTHER, CrewProblem.of("something_new"))
    }

    @Test fun `crew_feed 의 명함 칸을 읽는다 — 레벨이 비면 새 크루, 목표가 비면 목표 없음`() = runBlocking {
        val row = """
            [{"id":"c1","owner_id":"u1","name":"공덕 한바퀴","monogram":"공","tagline":"퇴근길에 함께 한 바퀴","area":"공덕동",
              "lat":37.5446,"lng":126.9515,"join_policy":"APPROVAL","created_at":"2026-09-28T01:00:00+00:00",
              "member_count":24,"roster":["준호"],"joined":false,"requested":true,"pending_count":0,"owned":false,
              "leader_name":"준호","leader_note":"처음이어도 편하게","image_bg":2,"image_ver":3,"has_image":true,
              "meet_days":10,"meet_time":1170,"run_distance":"D3_5","moods":["EASY","BEGINNER","UNKNOWN"],
              "capacity":30,"recruiting":true,"recruit_changed_at":"2026-09-27T01:00:00+00:00","weekly_goal_km":null,
              "level":null,"week_km":126.0,"week_runners":9,
              "my_application_id":41,"my_application_status":"PENDING","my_application_seen":false}]
        """.trimIndent()
        val result = api(FakeHttp(HttpResponse(200, row))).crews()
        val crew = (result as ServerResult.Ok).value.single().toCard()
        assertEquals("공덕 한바퀴", crew.name)
        assertEquals(CrewRole.PENDING, crew.role)
        assertEquals(CrewSchedule(10, 1170), crew.schedule) // 화 · 목 19:30
        assertEquals(listOf(1, 3), crew.schedule.dayIndexes)
        assertEquals(CrewDistance.D3_5, crew.distance)
        assertEquals(listOf(CrewMood.EASY, CrewMood.BEGINNER), crew.moods) // 모르는 값은 버린다
        assertEquals(30, crew.capacity)
        assertNull(crew.level)
        assertNull(crew.goalKm)
        assertNull(CrewRules.progress(crew))
        assertEquals(2, crew.bg)
        assertEquals(41L, crew.myApplicationId)
        assertEquals(CrewApplicationStatus.PENDING, crew.myApplicationStatus)
        assertNull(crew.unseenResult)
    }

    @Test fun `가입 신청은 고른 문구와 한마디를 따로 보낸다`() = runBlocking {
        val http = FakeHttp(HttpResponse(200, """{"result":"PENDING","application_id":7,"duplicate":false}"""))
        val result = api(http).apply("c1", listOf("BEGINNER", "EASY"), "퇴근 후 함께 뛰고 싶어요.", "key-1")
        assertTrue(result is ServerResult.Ok)
        assertTrue(http.lastUrl.endsWith("/rpc/crew_apply"))
        assertEquals(
            """{"p_crew":"c1","p_phrases":["BEGINNER","EASY"],"p_message":"퇴근 후 함께 뛰고 싶어요.","p_client_key":"key-1"}""",
            http.lastBody,
        )
    }

    @Test fun `이름 이미지는 빈칸에서 두 줄로 나누고 글자 중간에서 끊지 않는다`() {
        assertEquals("공덕\n한바퀴", CrewRules.nameImageText("공덕 한바퀴"))
        assertEquals("한강 새벽\n러닝 크루", CrewRules.nameImageText("  한강  새벽 러닝 크루 "))
        assertEquals("퇴근런", CrewRules.nameImageText("퇴근런"))
        assertEquals("새벽\n러닝", CrewRules.nameImageText("새벽 러닝"))
        assertEquals("A B", CrewRules.nameImageText("A B"))
        assertEquals("천천히걸음모임", CrewRules.nameImageText("천천히걸음모임"))
        assertEquals("·", CrewRules.nameImageText("  "))
    }

    @Test fun `같은 요청 키로 다시 보낸 신청 — 미승인 · 취소를 가입으로 보지 않는다`() {
        assertEquals(CrewApplied.Pending(7), CrewApplied.of("PENDING", 7))
        assertEquals(CrewApplied.Member, CrewApplied.of("MEMBER", null))
        assertEquals(CrewApplied.Declined(7), CrewApplied.of("DECLINED", 7))
        assertEquals(CrewApplied.Canceled, CrewApplied.of("CANCELED", 7))
        assertNull(CrewApplied.of("PENDING", null))
        assertNull(CrewApplied.of("", 7))
    }

    @Test fun `목표를 비우면 null 로 보낸다`() = runBlocking {
        val http = FakeHttp(HttpResponse(204, ""))
        api(http).setGoal("c1", null)
        assertEquals("""{"p_crew":"c1","p_goal_km":null}""", http.lastBody)
    }

    // ── 초안 ──

    @Test fun `만들기 초안은 단계를 오가도 같은 값으로 남는다`() {
        val draft = CrewDraft(
            mode = CrewDraftMode.CREATE,
            name = "공덕 한바퀴",
            tagline = "퇴근길에 함께 한 바퀴",
            leaderNote = "처음이어도 편하게 함께해요.\n천천히, 꾸준히 같이 달려요.",
            image = CrewImageChoice.Named(1),
            area = CrewArea("공덕동", "서울 마포구", 37.5446, 126.9515),
            schedule = CrewSchedule(10, 1170),
            distance = CrewDistance.D3_5,
            moods = listOf(CrewMood.EASY, CrewMood.BEGINNER),
            capacity = "20",
            goal = CrewGoalChoice.Km(120, custom = true),
            recruiting = true,
            step = 2,
            clientKey = "11111111-2222-3333-4444-555555555555",
        )
        val back = CrewDraftCodec.decode(CrewDraftCodec.encode(draft))!!
        assertEquals(draft.copy(savedAt = back.savedAt), back)
        assertEquals(CrewDraft.KEY_CREATE, back.key)
        // 사진 파일이 사라졌으면 이름 이미지로 돌아간다(없는 사진을 보이지 않는다)
        val lost = CrewDraftCodec.decode(CrewDraftCodec.encode(draft.copy(image = CrewImageChoice.Photo("/nope/photo-1.jpg"))))!!
        assertTrue(lost.image is CrewImageChoice.Named)
    }

    @Test fun `수정 초안은 크루마다 따로 남는다`() {
        val profile = CrewDraft.edit(CrewDraftMode.EDIT_PROFILE, card("c1", name = "퇴근런", goalKm = 160))
        assertEquals("EDIT_PROFILE:c1", profile.key)
        assertEquals(CrewImageChoice.Server, profile.image)
        assertEquals(CrewGoalChoice.Km(160, custom = true), profile.goal)
        assertEquals(CrewGoalChoice.Km(100, custom = false), CrewDraft.edit(CrewDraftMode.EDIT_RECRUIT, card("c1", goalKm = 100)).goal)
    }

    // ── 도우미 ──

    private class FakeHttp(private val answer: HttpResponse) : HttpPoster {
        var lastBody = ""
        var lastUrl = ""
        override suspend fun post(url: String, body: String, headers: Map<String, String>): HttpResponse {
            lastUrl = url
            lastBody = body
            return answer
        }
        override suspend fun get(url: String, headers: Map<String, String>) = post(url, "", headers)
    }

    private object LoggedIn : AuthSessionStore {
        override suspend fun load() = AuthSession(
            accessToken = "token", refreshToken = "refresh", expiresIn = 3600, expiresAt = 9_999_999_999L,
            user = AuthUser(id = "me", isAnonymous = false),
        )
        override suspend fun save(session: AuthSession) = Unit
        override suspend fun clear() = Unit
    }

    private fun api(http: FakeHttp) = CrewApi(
        StepUpServer(
            baseUrl = "https://test.supabase.co",
            apiKey = "sb_publishable_test",
            sessions = SessionHolder(auth = SupabaseAuth("https://test.supabase.co", "sb_publishable_test", http), store = LoggedIn, now = { 1_000L }),
            http = http,
        ),
    )
}
