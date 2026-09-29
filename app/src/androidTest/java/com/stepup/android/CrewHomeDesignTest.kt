package com.stepup.android

import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.enableEdgeToEdge
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.semantics.getOrNull
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.test.platform.app.InstrumentationRegistry
import com.stepup.android.core.ServiceLocator
import com.stepup.android.data.remote.AuthSession
import com.stepup.android.data.remote.AuthSessionStore
import com.stepup.android.data.remote.AuthUser
import com.stepup.android.data.remote.CrewApi
import com.stepup.android.data.remote.CrewChatApi
import com.stepup.android.data.remote.CrewHomeApi
import com.stepup.android.data.remote.HttpPoster
import com.stepup.android.data.remote.HttpResponse
import com.stepup.android.data.remote.PartyApi
import com.stepup.android.data.remote.SessionHolder
import com.stepup.android.data.remote.StepUpServer
import com.stepup.android.data.remote.SupabaseAuth
import com.stepup.android.data.repo.CrewCardRepository
import com.stepup.android.data.repo.CrewChatRepository
import com.stepup.android.data.repo.CrewHomeRepository
import com.stepup.android.data.repo.CrewImageStore
import com.stepup.android.data.repo.CrewRepository
import com.stepup.android.domain.CrewHomeRules
import com.stepup.android.ui.MainScaffold
import com.stepup.android.ui.Screen
import com.stepup.android.ui.experience.ExperienceProvider
import com.stepup.android.ui.theme.StepUpTheme
import com.stepup.android.ui.theme.ThemeMode
import java.io.File
import java.net.URLDecoder
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.temporal.TemporalAdjusters
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.CopyOnWriteArrayList
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.booleanOrNull
import kotlinx.serialization.json.buildJsonArray
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.longOrNull
import kotlinx.serialization.json.put
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

/**
 * 내 크루 홈(확정 4번, 2026-09-29) — 홈과 여덟 가지 상세(소개 · 레벨 · 크루원 · 크루장 · 채팅 · 다음 러닝 · 주간 기록 · 공지)를
 * 실제 앱 화면으로 찍는다.
 *
 * 저장소 · 뷰모델 · 화면은 실제 코드다. 서버만 흉내 낸다([FakeHomeServer]) — crew_home · crew_meeting* · crew_week ·
 * crew_runs · crew_run · crew_member_last_run 이 0049 와 같은 모양, crew_feed · crew_roster · crew_person · crew_level ·
 * crew_chat_* 가 기존과 같은 모양의 답을 준다. 이름 · 숫자는 시안 패키지의 예시(퇴근런 · 준호 · 도윤 · 지연 · 8명 · 126km)를
 * 검사 자료로만 쓴다(앱 코드에는 없다). 참석 인원은 서버(가짜)가 응답을 바꿔 적은 뒤 센 값이다 — 앱이 더하지 않는다.
 *
 * 캡처: crew-home/<시안 번호>-<장면>.png — 번호는 시안(00~31)과 같다.
 */
class CrewHomeDesignTest {
    @get:Rule(order = 0) val appLanguage = object : org.junit.rules.ExternalResource() {
        private var previous = ""
        override fun before() {
            val instrumentation = InstrumentationRegistry.getInstrumentation()
            val context = instrumentation.targetContext
            com.stepup.android.core.AppLocale.syncFromSystem(context)
            previous = com.stepup.android.core.AppLocale.tag
            instrumentation.runOnMainSync { com.stepup.android.core.AppLocale.change(context, "ko") }
        }
        override fun after() {
            val instrumentation = InstrumentationRegistry.getInstrumentation()
            instrumentation.runOnMainSync { com.stepup.android.core.AppLocale.change(instrumentation.targetContext, previous) }
        }
    }
    @get:Rule(order = 1) val compose = createAndroidComposeRule<ComponentActivity>()

    private val directory get() = File(compose.activity.getExternalFilesDir(null), "crew-home").apply { mkdirs() }
    private val notes = CopyOnWriteArrayList<String>()
    private lateinit var originalCards: CrewCardRepository
    private lateinit var originalChat: CrewChatRepository
    private lateinit var originalHome: CrewHomeRepository
    private val server = FakeHomeServer()

    @After fun restore() {
        if (::originalCards.isInitialized) ServiceLocator.useCrewCardsForTest(originalCards)
        if (::originalChat.isInitialized) ServiceLocator.useCrewChatForTest(originalChat)
        if (::originalHome.isInitialized) ServiceLocator.useCrewHomeForTest(originalHome)
        runCatching { File(directory, "capture-notes-${System.currentTimeMillis()}.txt").writeText(notes.joinToString("\n")) }
    }

    // ─────────────────────────────────────────────────────────────
    // 크루원(도윤) — 홈 · 소개 · 레벨 · 크루원 · 프로필 · 모임 · 참석 · 장소 · 주간 기록 · 참여 기록 · 러닝 기록 · 공지
    // ─────────────────────────────────────────────────────────────

    @Test fun memberHome() {
        server.seed(me = DOYUN)
        launch(DOYUN)
        guard {
            openHome()
            awaitText("공덕역 2번 출구 · 3km")
            awaitText("8명 참석")
            awaitTag("chat-unread")
            shot("00-home", settle = 1_500)

            tapTag("crew-more")
            awaitTag("home-menu")
            awaitTag("home-menu-leave")
            shot("22-home-menu")
            tapTag("crew-sheet-close")
            awaitGone("home-menu")

            // 01 → 02 크루 소개 · 대표 이미지
            tapTag("home-masthead")
            awaitTag("home-intro")
            awaitTag("home-intro-area")
            shot("01-introduction", settle = 1_200)
            tapTag("home-intro-image")
            awaitTag("crew-image-view")
            shot("02-crew-image", settle = 1_000)
            pressBack()
            pressBack()

            // 03 레벨 — 승급 기준은 준비 중(계산하지 않는다)
            tapTag("home-level")
            awaitTag("home-level-page")
            awaitText("승급 기준 준비 중")
            awaitText("126km")
            shot("03-level")
            pressBack()

            // 04 크루원 → 27 이름 검색 → 06 크루원(지연) · 05 크루장
            tapTag("home-members")
            awaitTag("home-member-$JIYEON")
            awaitText("나 · 크루원")
            shot("04-members")
            typeInto("home-members-search", "지연")
            awaitTag("home-members-found")
            closeKeyboard()
            shot("27-member-search")
            typeInto("home-members-search", "지훈")
            awaitTag("home-members-none")
            closeKeyboard()
            shot("27b-member-search-none")
            typeInto("home-members-search", "지연")
            awaitTag("home-member-$JIYEON")
            closeKeyboard()
            tapTag("home-member-$JIYEON")
            awaitTag("home-person")
            awaitTag("home-person-run")
            shot("06-member")
            // 공개한 러닝 기록 → 19
            tapTag("home-person-run")
            awaitTag("home-run-km")
            shot("19b-run-from-profile", settle = 1_500)
            pressBack()
            pressBack()
            pressBack()
            tapTag("home-leader")
            awaitTag("home-person-leader")
            awaitTag("home-person-note")
            shot("05-leader")
            pressBack()

            // 09 다음 러닝 → 저장 중 → 11 참석(9명) → 28 홈
            tapTag("home-rsvp")
            awaitTag("home-meeting")
            awaitText("8명 참석")
            shot("09-meeting", settle = 1_500)
            server.respondDelay = 2_500
            tapTag("home-meeting-yes-button")
            awaitText("저장 중…")
            shot("09b-saving", settle = 200)
            awaitTag("home-meeting-yes", 20_000)
            server.respondDelay = 0
            awaitText("9명 참석")
            shot("11-meeting-yes", settle = 1_200)

            tapTag("home-meeting-attendees")
            awaitTag("home-attendees-title")
            awaitText("함께 달릴 9명")
            awaitTag("home-attendee-$DOYUN")
            shot("13-attendees")
            pressBack()

            tapTag("home-meeting-place")
            awaitTag("home-place")
            awaitTag("home-meeting-map")
            shot("14-meeting-place", settle = 2_500)
            pressBack()

            pressBack()
            awaitText("9명 참석")
            awaitText("참석 예정 ✓")
            shot("28-home-yes", settle = 1_200)

            // 10 참석 여부 변경 → 25 저장 실패(9명 · 참석 그대로) → 12 불참(8명) → 29 홈
            tapTag("home-rsvp")
            awaitTag("home-meeting-yes")
            tapTag("home-meeting-change")
            awaitTag("home-attendance")
            shot("10-attendance")
            server.respondStatus = 503
            tapTag("home-attendance-no")
            awaitTag("home-attendance-error-text")
            shot("25-attendance-error")
            assertEquals("실패하면 서버의 응답이 그대로", "YES", server.responseOf(DOYUN))
            server.respondStatus = 200
            tapTag("home-attendance-no")
            awaitTag("home-meeting-no")
            awaitText("8명 참석")
            shot("12-meeting-no", settle = 1_200)
            pressBack()
            awaitText("불참 예정")
            awaitText("8명 참석")
            shot("29-home-no", settle = 1_200)

            // 15 주간 기록 → 16 주간 선택 → 17 지난주 → 24 기록 없는 주
            tapTag("home-week")
            awaitTag("home-week-page")
            awaitTag("home-week-percent")
            shot("15-weekly", settle = 1_200)
            tapTag("home-week-range")
            awaitTag("home-week-select")
            shot("16-week-selector")
            tapTag("home-week-pick-1")
            awaitText("142")
            shot("17-weekly-previous", settle = 1_200)
            tapTag("home-week-range")
            tapTag("home-week-pick-2")
            awaitTag("home-week-empty")
            shot("24-weekly-empty", settle = 1_200)
            tapTag("home-week-range")
            tapTag("home-week-pick-0")
            awaitTag("home-week-percent")

            // 18 오늘의 참여 기록 → 19 러닝 기록
            tapTag("home-week-day-${server.todayIndex()}")
            awaitTag("home-runs")
            awaitTag("home-run-row-${FakeHomeServer.RUN_JIYEON}")
            shot("18-day-records", settle = 1_000)
            tapTag("home-run-row-${FakeHomeServer.RUN_JIYEON}")
            awaitTag("home-run-km")
            awaitText("7'00\"")
            shot("19-run-record", settle = 2_500)
            tapTag("home-run-back")
            awaitTag("home-runs")
            pressBack()
            pressBack()

            // 20 공지(이은 모임) → 모임 → 공지로 돌아온다
            tapTag("home-notice")
            awaitTag("chat-notice")
            awaitTag("chat-notice-meeting-label")
            awaitTag("chat-notice-meeting")
            shot("20-notice", settle = 1_000)
            tapTag("chat-notice-meeting")
            awaitTag("home-meeting-no")
            pressBack()
            awaitTag("chat-notice")
            pressBack()

            // 21 공지 모아보기 — "처음 오시는 분들께"는 그 공지로(모임 버튼 없음)
            tapTag("crew-more")
            tapTag("home-menu-notices")
            awaitTag("chat-notices-pinned")
            shot("21-notices")
            tapTag("chat-notices-row")
            awaitTag("chat-notice-title")
            awaitText("처음 오시는 분들께")
            assertNone("chat-notice-meeting")
            shot("20b-notice-plain")
        }
    }

    // ─────────────────────────────────────────────────────────────
    // 상태 — 26 불러오기 실패 · 23 모임 없음 · 채팅 정보의 크루 상세 보기 · 크루 나가기 · 접근 종료
    // ─────────────────────────────────────────────────────────────

    @Test fun memberStates() {
        server.seed(me = DOYUN)
        server.homeStatus = 503
        launch(DOYUN)
        guard {
            awaitTag("crew-card-afterwork")
            tapTag("crew-card-afterwork")
            awaitTag("home-error")
            shot("26-load-error")
            server.homeStatus = 200
            server.meeting = false
            server.weekKm = 0.0
            tapTag("home-error-retry")
            awaitTag("home-no-meeting")
            awaitTag("home-week-waiting")
            shot("23-home-empty", settle = 1_500)

            // 07 · 08 — 채팅 → 정보 → 크루 상세 보기는 홈(쌓지 않고 돌아온다)
            tapTag("home-chat")
            awaitTag("chat-room")
            awaitText("오늘은 공덕역 2번 출구에서 만나요.")
            shot("07-chat-member", settle = 1_500)
            tapTag("chat-room-more")
            awaitTag("chat-info")
            shot("08-chat-info")
            tapTag("chat-info-crew")
            awaitTag("crew-home")

            // 61 크루 나가기(더보기) — 확인 뒤 크루 목록
            tapTag("crew-more")
            tapTag("home-menu-leave")
            awaitTag("home-leave")
            shot("22b-leave-confirm")
            tapTag("crew-sheet-close")
            awaitGone("home-leave")

            // 내보내졌다 — 다시 읽으면 접근 종료(기존 66)
            server.kick(DOYUN)
            pressBack()
            pressBack()
            pressBack()
            awaitTag("crew-card-afterwork")
            tapTag("crew-card-afterwork")
            awaitTag("crew-missing")
            shot("32-access-ended")
        }
    }

    // ─────────────────────────────────────────────────────────────
    // 크루장(준호) — 홈 · 더보기(크루 관리) · 채팅 정보 · 공지에 모임 잇기
    // ─────────────────────────────────────────────────────────────

    @Test fun ownerHome() {
        server.seed(me = JUNHO)
        launch(JUNHO)
        guard {
            openHome(owner = true)
            shot("00b-home-owner", settle = 1_500)
            tapTag("crew-more")
            awaitTag("home-menu-manage")
            shot("22c-home-menu-owner")
            tapTag("crew-sheet-close")
            awaitGone("home-menu")

            tapTag("home-chat")
            awaitTag("chat-room-owner")
            shot("30-chat-owner", settle = 1_500)
            tapTag("chat-room-more")
            awaitTag("chat-info-owner")
            shot("31-chat-info-owner")
            pressBack()
            pressBack()
            awaitTag("crew-home-owner")

            // 공지 수정에서 모임 잇기 — 저장하면 공지에 모임 버튼
            tapTag("home-notice")
            awaitTag("chat-notice")
            tapTag("crew-more")
            awaitTag("chat-notice-edit")
            tapTag("chat-notice-meeting-field")
            awaitTag("chat-notice-meetings")
            awaitTag("chat-notice-meeting-${FakeHomeServer.MEETING_ID}")
            shot("20c-notice-meeting-pick")
            tapTag("chat-notice-meeting-none")
            awaitGone("chat-notice-meetings")
            tapTag("chat-notice-save")
            awaitTag("chat-room-owner")
            assertEquals("모임 연결을 풀면 서버에 없음으로", null, server.noticeMeeting(61))
        }
    }

    // ─────────────────────────────────────────────────────────────
    // 준비 · 도우미
    // ─────────────────────────────────────────────────────────────

    private fun launch(me: String) {
        clearAnyRunCheckpointForTest()
        runBlocking {
            ServiceLocator.userPrefs.setReducedMotion(true)
            ServiceLocator.userPrefs.setSounds(false)
            ServiceLocator.userPrefs.setHaptics(false)
            ServiceLocator.userPrefs.setGuideSeen()
        }
        val context = compose.activity
        val stepUp = StepUpServer(
            baseUrl = "https://home.test",
            apiKey = "sb_publishable_test",
            sessions = SessionHolder(auth = SupabaseAuth("https://home.test", "sb_publishable_test", server), store = SignedIn(me)),
            http = server,
        )
        val crewApi = CrewApi(stepUp)
        val db = ServiceLocator.database
        val crews = CrewRepository(crewApi, db.crewDao(), db.crewInfoDao(), db.walkSessionDao(), ServiceLocator.rewardRepository, PartyApi(stepUp))
        val owner = "account:$me"
        val cards = CrewCardRepository(
            api = crewApi,
            crews = crews,
            images = CrewImageStore(fetch = crewApi::image, dir = File(context.cacheDir, "home-test-images").apply { deleteRecursively() }),
            prefs = ServiceLocator.userPrefs,
            owner = { owner },
            ownerFlow = flowOf(owner),
            draftDir = File(context.filesDir, "home_test_drafts").apply { deleteRecursively() },
        )
        val chat = CrewChatRepository(
            api = CrewChatApi(stepUp),
            crewApi = crewApi,
            prefs = ServiceLocator.userPrefs,
            owner = { owner },
            photoDir = File(context.cacheDir, "home-test-photos").apply { deleteRecursively() },
        )
        val home = CrewHomeRepository(CrewHomeApi(stepUp), owner = { owner }, onEnded = { chat.forget(it) })
        originalCards = ServiceLocator.crewCards
        originalChat = ServiceLocator.crewChat
        originalHome = ServiceLocator.crewHome
        ServiceLocator.useCrewCardsForTest(cards)
        ServiceLocator.useCrewChatForTest(chat)
        ServiceLocator.useCrewHomeForTest(home)
        runBlocking { ServiceLocator.userPrefs.setChatNoticeDraft(owner, "afterwork:61", null) }
        server.images["afterwork"] = asset64("crew/afterwork.png")
        compose.activityRule.scenario.onActivity {
            it.enableEdgeToEdge(
                statusBarStyle = SystemBarStyle.dark(android.graphics.Color.TRANSPARENT),
                navigationBarStyle = SystemBarStyle.dark(android.graphics.Color.TRANSPARENT),
            )
        }
        compose.setContent { StepUpTheme(ThemeMode.DARK) { ExperienceProvider { MainScaffold(initialTab = Screen.Community) } } }
        awaitTag("community-tab-crews")
        tapTag("community-tab-crews")
        awaitTag("crew-list")
    }

    /** 커뮤니티 › 크루 모집의 가입한 크루 → 내 크루 홈 */
    private fun openHome(owner: Boolean = false) {
        awaitTag("crew-card-afterwork")
        tapTag("crew-card-afterwork")
        awaitTag(if (owner) "crew-home-owner" else "crew-home", 20_000)
        awaitTag("home-masthead")
    }

    private fun guard(body: () -> Unit) {
        try {
            body()
        } catch (failure: Throwable) {
            runCatching { shot("zz-failure-${System.currentTimeMillis()}", settle = 0) }
            runCatching { File(directory, "zz-failure-tree.txt").writeText(compose.onAllNodes(isRoot()).printToString()) }
            throw failure
        }
    }

    private fun assertNone(tag: String) {
        assertTrue("'$tag' should not be shown", compose.onAllNodesWithTag(tag, useUnmergedTree = true).fetchSemanticsNodes().isEmpty())
    }

    private fun awaitTag(tag: String, timeout: Long = 20_000) {
        val end = android.os.SystemClock.uptimeMillis() + timeout
        while (compose.onAllNodesWithTag(tag, useUnmergedTree = true).fetchSemanticsNodes().isEmpty()) {
            if (android.os.SystemClock.uptimeMillis() > end) {
                val tags = compose.onAllNodes(SemanticsMatcher("home tag") { node ->
                    node.config.getOrNull(SemanticsProperties.TestTag)?.let { it.startsWith("home-") || it.startsWith("crew-") || it.startsWith("chat-") } == true
                }, useUnmergedTree = true).fetchSemanticsNodes().mapNotNull { it.config.getOrNull(SemanticsProperties.TestTag) }
                throw AssertionError("'$tag' did not appear in ${timeout}ms (tags now: ${tags.distinct().take(40)})")
            }
            compose.waitForIdle()
            Thread.sleep(40)
        }
    }

    private fun awaitGone(tag: String, timeout: Long = 20_000) {
        val end = android.os.SystemClock.uptimeMillis() + timeout
        while (compose.onAllNodesWithTag(tag, useUnmergedTree = true).fetchSemanticsNodes().isNotEmpty()) {
            if (android.os.SystemClock.uptimeMillis() > end) throw AssertionError("'$tag' still shown after ${timeout}ms")
            compose.waitForIdle()
            Thread.sleep(40)
        }
    }

    private fun awaitText(text: String, timeout: Long = 20_000) {
        val end = android.os.SystemClock.uptimeMillis() + timeout
        while (compose.onAllNodesWithText(text, substring = true, useUnmergedTree = true).fetchSemanticsNodes().isEmpty()) {
            if (android.os.SystemClock.uptimeMillis() > end) throw AssertionError("'$text' did not appear in ${timeout}ms")
            compose.waitForIdle()
            Thread.sleep(40)
        }
    }

    private fun tapTag(tag: String) {
        awaitTag(tag)
        val node = compose.onAllNodesWithTag(tag).onFirst()
        runCatching { node.performScrollTo() }
        node.performClick()
        compose.waitForIdle()
    }

    private fun typeInto(tag: String, text: String) {
        awaitTag(tag)
        val node = compose.onNodeWithTag(tag)
        runCatching { node.performScrollTo() }
        node.performClick()
        node.performTextReplacement(text)
        compose.waitForIdle()
    }

    private fun pressBack() {
        compose.activityRule.scenario.onActivity { it.onBackPressedDispatcher.onBackPressed() }
        compose.waitForIdle()
    }

    private fun closeKeyboard() {
        compose.runOnUiThread {
            androidx.core.view.WindowInsetsControllerCompat(compose.activity.window, compose.activity.window.decorView)
                .hide(androidx.core.view.WindowInsetsCompat.Type.ime())
        }
        compose.waitForIdle()
        Thread.sleep(500)
    }

    private fun shot(name: String, settle: Long = 700) {
        compose.waitForIdle()
        InstrumentationRegistry.getInstrumentation().waitForIdleSync()
        Thread.sleep(settle)
        awaitFrameOnScreen(compose.activity)
        android.util.Log.i("CrewHome", "Capturing $name")
        captureDisplay(File(directory, "$name.png"))
        notes += name
    }

    private fun asset64(path: String): String = InstrumentationRegistry.getInstrumentation().context.assets.open(path).use {
        android.util.Base64.encodeToString(it.readBytes(), android.util.Base64.NO_WRAP)
    }

    private class SignedIn(private val userId: String) : AuthSessionStore {
        override suspend fun load() = AuthSession(
            accessToken = "token", refreshToken = "refresh", expiresIn = 3600, expiresAt = 9_999_999_999L,
            user = AuthUser(id = userId, isAnonymous = false),
        )
        override suspend fun save(session: AuthSession) = Unit
        override suspend fun clear() = Unit
    }

    /**
     * 0049 와 같은 모양의 답 — 홈 · 모임 · 참석(사람마다 응답 하나로 바꿔 적고 센 인원) · 참석자 · 주간 · 참여 기록 · 러닝 ·
     * 최근 러닝, 기존 crew_feed · crew_roster · crew_person · crew_level · crew_leave · crew_chat_*.
     * 지금 멤버가 아니면 crew_not_member(채팅은 chat_not_member).
     */
    private class FakeHomeServer : HttpPoster {
        class Person(val id: String, val name: String, val joinedAt: String = "2026-08-03T01:00:00Z")

        class Run(val id: Long, val userId: String, val name: String, val meters: Double, val seconds: Int, val end: LocalDateTime)

        private val json = Json { ignoreUnknownKeys = true }
        private val zone: ZoneId = ZoneId.systemDefault()
        val images = ConcurrentHashMap<String, String>()
        val members = CopyOnWriteArrayList<Person>()
        private val responses = ConcurrentHashMap<String, String>()
        private val runs = CopyOnWriteArrayList<Run>()
        private val noticeMeetings = ConcurrentHashMap<Long, Long>()
        @Volatile var me = DOYUN
        @Volatile var homeStatus = 200
        @Volatile var respondStatus = 200
        @Volatile var respondDelay = 0L
        @Volatile var meeting = true
        @Volatile var weekKm = 126.0

        private val monday: LocalDate get() = LocalDate.now(zone).with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY))

        fun todayIndex(): Int = (LocalDate.now(zone).toEpochDay() - monday.toEpochDay()).toInt()

        fun responseOf(user: String): String? = responses[user]

        fun noticeMeeting(id: Long): Long? = noticeMeetings[id]

        fun kick(user: String) {
            members.removeAll { it.id == user }
        }

        private fun iso(time: LocalDateTime): String = time.atZone(zone).toOffsetDateTime().toString()

        /** 퇴근런 25명 · 크루장 준호 · 오늘 19:30 공덕역 2번 출구 3km(8명 참석, 도윤은 미응답) · 이번 주 126 / 160km */
        fun seed(me: String) {
            this.me = me
            members.clear(); responses.clear(); runs.clear(); noticeMeetings.clear()
            homeStatus = 200; respondStatus = 200; respondDelay = 0; meeting = true; weekKm = 126.0
            members += Person(JUNHO, "준호", "2026-08-01T01:00:00Z")
            members += Person(DOYUN, "도윤", "2026-09-02T01:00:00Z")
            members += Person(JIYEON, "지연", "2026-08-12T01:00:00Z")
            members += Person(MINSU, "민수")
            listOf("서연", "현우", "유진", "태훈", "소희", "지우", "하린", "태오", "수아", "민재", "예린", "도현", "시우", "서아", "준서", "하은", "지호", "채원", "건우", "다은", "은우")
                .forEachIndexed { index, name -> members += Person("u-${index + 10}", name) }
            // 참석 — 진행자(준호)와 일곱 명
            responses[JUNHO] = "YES"
            listOf(JIYEON, MINSU, "u-10", "u-11", "u-12", "u-13", "u-14").forEach { responses[it] = "YES" }
            noticeMeetings[61] = MEETING_ID
            val today = LocalDate.now(zone)
            runs += Run(RUN_JIYEON, JIYEON, "지연", 5200.0, 2184, today.atTime(18, 15))
            runs += Run(72, JUNHO, "준호", 4800.0, 2016, today.atTime(18, 0))
            runs += Run(73, MINSU, "민수", 3400.0, 1500, today.atTime(17, 40))
            runs += Run(74, "u-11", "현우", 6000.0, 2280, today.atTime(17, 20))
            runs += Run(75, "u-10", "서연", 4200.0, 1860, today.atTime(16, 50))
            runs += Run(76, "u-12", "유진", 5600.0, 2340, today.atTime(16, 15))
            runs += Run(77, JIYEON, "지연", 6000.0, 2400, monday.minusDays(1).atTime(7, 10))
        }

        private fun member(user: String) = members.any { it.id == user }

        private fun meetingJson(): JsonObject {
            val going = members.filter { responses[it.id] == "YES" }
            val ordered = going.sortedByDescending { it.id == JUNHO }
            return buildJsonObject {
                put("id", MEETING_ID); put("crew_id", "afterwork"); put("title", "퇴근 후, 가볍게 3km")
                put("body", "처음 오셔도 괜찮아요. 물 한 병 챙겨주세요.\n늦으시면 크루 채팅에 알려주세요.")
                put("place", "공덕역 2번 출구"); put("lat", 37.5446); put("lng", 126.9515); put("distance_km", 3.0)
                put("meet_at", iso(LocalDate.now(zone).atTime(19, 30))); put("capacity", 0)
                put("host_id", JUNHO); put("host_name", "준호"); put("host_owner", true); put("attendees", going.size)
                put("faces", JsonArray(ordered.take(3).map { p -> buildJsonObject { put("user_id", p.id); put("name", p.name) } }))
                put("my_response", responses[me]); put("open", true)
            }
        }

        private fun noticeJson(id: Long): JsonObject = buildJsonObject {
            val first = id == 61L
            put("id", id); put("crew_id", "afterwork")
            put("title", if (first) "오늘도 천천히 3km 함께 달려요" else "처음 오시는 분들께")
            put("body", if (first) "오늘 저녁 7시 30분에 만나요.\n\n공덕역 2번 출구 앞에서 출발해서\n천천히 3km를 달리고 돌아올게요.\n\n처음 오시는 분도 편하게 함께해요.\n물 한 병 챙기시고, 늦으시면\n크루 채팅에 알려주세요." else "만나는 장소와 준비물을 확인해 주세요.")
            put("pinned", first); put("author_id", JUNHO); put("author_name", "준호")
            val at = if (first) iso(LocalDate.now(zone).atTime(18, 0)) else iso(LocalDate.now(zone).minusDays(5).atTime(19, 0))
            put("created_at", at); put("updated_at", at)
            val linked = noticeMeetings[id]
            if (linked != null && meeting) {
                put("meeting", buildJsonObject {
                    put("id", linked); put("title", "퇴근 후, 가볍게 3km"); put("place", "공덕역 2번 출구"); put("meet_at", iso(LocalDate.now(zone).atTime(19, 30)))
                })
            } else {
                put("meeting", JsonNull)
            }
        }

        private fun feedRow(): JsonObject = buildJsonObject {
            put("id", "afterwork"); put("owner_id", JUNHO); put("name", "퇴근런"); put("monogram", "퇴"); put("tagline", "퇴근 후\n가볍게 한 바퀴")
            put("area", "공덕"); put("lat", 37.5446); put("lng", 126.9515); put("join_policy", "APPROVAL"); put("created_at", "2026-08-01T01:00:00Z")
            put("member_count", members.size); put("roster", buildJsonArray { add(JsonPrimitive("준호")) })
            put("joined", member(me)); put("requested", false); put("pending_count", 0); put("owned", me == JUNHO)
            put("leader_name", "준호"); put("leader_note", "빠르게보다 꾸준하게.\n퇴근 후 함께 달리는 시간을 만들어요.")
            put("image_bg", 1); put("image_ver", 1); put("has_image", true)
            put("meet_days", 0b1010); put("meet_time", 19 * 60 + 30); put("run_distance", "D3_5")
            put("moods", JsonArray(listOf(JsonPrimitive("EASY"), JsonPrimitive("BEGINNER")))); put("capacity", 30); put("recruiting", true)
            put("recruit_changed_at", "2026-09-27T09:00:00Z"); put("weekly_goal_km", 160); put("level", 7)
            put("week_km", weekKm); put("week_runners", if (weekKm > 0) 9 else 0)
        }

        private fun weekJson(start: LocalDate): JsonObject {
            val thisWeek = start == monday
            val today = todayIndex()
            val days: List<Double?> = when {
                thisWeek && weekKm <= 0 -> List(7) { if (it <= today) 0.0 else null }
                thisWeek -> List(7) { i -> when { i > today -> null; today == 0 -> 126.0; i == 0 -> 61.2; i == today -> 64.8; else -> 0.0 } }
                start == monday.minusDays(7) -> listOf(20.0, 18.0, 0.0, 28.0, 22.0, 34.0, 20.0)
                else -> List(7) { 0.0 }
            }
            val km = days.filterNotNull().sum()
            return buildJsonObject {
                put("week_start", start.toString()); put("this_week", thisWeek); put("goal_km", 160); put("km", km)
                put("runners", if (km <= 0) 0 else if (thisWeek) 9 else 11)
                put("days", JsonArray(days.map { if (it == null) JsonNull else JsonPrimitive(it) }))
                val people = if (km <= 0) emptyList() else if (thisWeek) {
                    listOf(Triple(JIYEON, "지연", 18.6), Triple(JUNHO, "준호", 16.2), Triple(MINSU, "민수", 14.0), Triple("u-11", "현우", 12.0))
                } else {
                    listOf(Triple(JIYEON, "지연", 22.0), Triple(JUNHO, "준호", 20.0), Triple(MINSU, "민수", 16.0))
                }
                put("members", JsonArray(people.map { (id, name, total) ->
                    val last = runs.filter { it.userId == id }.maxByOrNull { it.end }
                    buildJsonObject {
                        put("user_id", id); put("name", name); put("km", total); put("runs", 3)
                        put("last_km", (last?.meters ?: 6000.0) / 1000.0); put("last_at", iso(last?.end ?: start.plusDays(6).atTime(7, 0)))
                    }
                }))
                put("weeks", JsonArray(listOf(monday, monday.minusDays(7), monday.minusDays(14)).map { JsonPrimitive(it.toString()) }))
            }
        }

        private fun runJson(run: Run, route: Boolean = false): JsonObject = buildJsonObject {
            put("id", run.id); put("crew_id", "afterwork"); put("user_id", run.userId); put("name", run.name)
            put("distance_m", run.meters); put("duration_s", run.seconds)
            put("started_at", iso(run.end.minusSeconds(run.seconds.toLong()))); put("ended_at", iso(run.end))
            if (route) {
                // 공덕역 주변을 한 바퀴 — 서버가 처음과 끝 300m 를 뗀 모양(시작 · 끝이 한 점에 모이지 않는다)
                val points = (0 until 48).map { i ->
                    val t = i / 47.0 * 1.7 * Math.PI + 0.3
                    "%.5f,%.5f".format(37.5446 + 0.0042 * Math.sin(t), 126.9515 + 0.0058 * Math.cos(t))
                }
                put("route", points.joinToString(";"))
            }
        }

        private fun ok(body: Any): HttpResponse = HttpResponse(200, body.toString())
        private fun refuse(message: String, status: Int = 400) = HttpResponse(status, """{"message":"$message"}""")

        override suspend fun get(url: String, headers: Map<String, String>): HttpResponse = when {
            "/crew_feed" in url -> HttpResponse(200, JsonArray(listOf(feedRow())).toString())
            "/crew_images" in url -> {
                val id = Regex("crew_id=eq\\.([^&]+)").find(url)?.groupValues?.get(1)?.let { URLDecoder.decode(it, "UTF-8") }
                val data = id?.let { images[it] }
                HttpResponse(200, if (data == null) "[]" else """[{"data":"$data"}]""")
            }
            else -> HttpResponse(200, "[]")
        }

        override suspend fun post(url: String, body: String, headers: Map<String, String>): HttpResponse {
            val name = url.substringAfterLast("/rpc/")
            val args = runCatching { json.parseToJsonElement(body).jsonObject }.getOrDefault(JsonObject(emptyMap()))
            fun text(key: String) = args[key]?.takeIf { it !is JsonNull }?.jsonPrimitive?.content
            fun long(key: String) = args[key]?.takeIf { it !is JsonNull }?.jsonPrimitive?.longOrNull
            val inCrew = member(me)
            return when (name) {
                "crew_home" -> {
                    if (!inCrew) return refuse("crew_not_member", 403)
                    if (homeStatus != 200) return refuse("unavailable", homeStatus)
                    ok(buildJsonObject {
                        put("crew_id", "afterwork"); put("role", if (me == JUNHO) "OWNER" else "MEMBER"); put("owner_id", JUNHO)
                        put("member_count", members.size); put("unread", 3)
                        put("meeting", if (meeting) meetingJson() else JsonNull)
                        put("week", buildJsonObject {
                            put("week_start", monday.toString()); put("km", weekKm); put("runners", if (weekKm > 0) 9 else 0); put("goal_km", 160)
                        })
                        put("notice", noticeJson(61))
                    })
                }
                "crew_meeting" -> {
                    if (!inCrew) return refuse("crew_not_member", 403)
                    if (!meeting || long("p_post") != MEETING_ID) return refuse("meeting_missing")
                    ok(meetingJson())
                }
                "crew_meeting_respond" -> {
                    if (!inCrew) return refuse("crew_not_member", 403)
                    if (respondDelay > 0) delay(respondDelay)
                    if (respondStatus != 200) return refuse("unavailable", respondStatus)
                    if (!meeting || long("p_post") != MEETING_ID) return refuse("meeting_missing")
                    val attend = args["p_attend"]?.jsonPrimitive?.booleanOrNull ?: return refuse("invalid:response")
                    responses[me] = if (attend) "YES" else "NO"
                    ok(meetingJson())
                }
                "crew_meeting_attendees" -> {
                    if (!inCrew) return refuse("crew_not_member", 403)
                    val going = members.filter { responses[it.id] == "YES" }.sortedByDescending { it.id == JUNHO }
                    ok(JsonArray(going.map { p ->
                        buildJsonObject { put("user_id", p.id); put("name", p.name); put("host", p.id == JUNHO); put("owner", p.id == JUNHO) }
                    }))
                }
                "crew_meetings_upcoming" -> {
                    if (!inCrew) return refuse("crew_not_member", 403)
                    ok(JsonArray(if (meeting) listOf(meetingJson()) else emptyList()))
                }
                "crew_week" -> {
                    if (!inCrew) return refuse("crew_not_member", 403)
                    val start = text("p_week")?.let(LocalDate::parse) ?: monday
                    ok(weekJson(start))
                }
                "crew_runs" -> {
                    if (!inCrew) return refuse("crew_not_member", 403)
                    val start = text("p_week")?.let(LocalDate::parse) ?: monday
                    val day = text("p_day")?.let(LocalDate::parse)
                    val user = text("p_user")
                    val before = long("p_before_id")
                    val shown = runs.filter { r ->
                        val d = r.end.toLocalDate()
                        !d.isBefore(start) && !d.isAfter(start.plusDays(6)) && (day == null || d == day) && (user == null || r.userId == user) &&
                            (before == null || r.id < before)
                    }.sortedByDescending { it.end }
                    ok(JsonArray(shown.map { runJson(it) }))
                }
                "crew_member_last_run" -> {
                    if (!inCrew) return refuse("crew_not_member", 403)
                    val user = text("p_user")
                    runs.filter { it.userId == user }.maxByOrNull { it.end }?.let { ok(runJson(it)) } ?: ok("null")
                }
                "crew_run" -> {
                    if (!inCrew) return refuse("crew_not_member", 403)
                    runs.firstOrNull { it.id == long("p_run") }?.let { ok(runJson(it, route = true)) } ?: refuse("run_missing")
                }
                "crew_roster" -> ok(JsonArray(members.sortedByDescending { it.id == JUNHO }.map { p ->
                    buildJsonObject {
                        put("user_id", p.id); put("name", p.name); put("role", if (p.id == JUNHO) "OWNER" else "MEMBER")
                        put("joined_at", p.joinedAt); put("week_km", 0.0); put("week_runs", 0)
                    }
                }))
                "crew_person" -> {
                    val user = text("p_user").orEmpty()
                    val person = members.firstOrNull { it.id == user }
                    ok(buildJsonObject {
                        put("user_id", user); put("name", person?.name ?: "러너")
                        put("role", when { person == null -> "NONE"; user == JUNHO -> "OWNER"; else -> "MEMBER" })
                        put("joined_at", person?.joinedAt); put("week_km", 0.0)
                    })
                }
                "crew_level" -> ok(buildJsonObject { put("level", 7); put("week_km", weekKm); put("week_runners", if (weekKm > 0) 9 else 0); put("member_count", members.size) })
                "crew_leave" -> {
                    kick(me)
                    HttpResponse(204, "")
                }
                "crew_chat_rooms" -> ok(JsonArray(if (inCrew) listOf(buildJsonObject {
                    put("crew_id", "afterwork"); put("name", "퇴근런"); put("image_bg", 1); put("image_ver", 1); put("has_image", true)
                    put("owner_id", JUNHO); put("role", if (me == JUNHO) "OWNER" else "MEMBER"); put("member_count", members.size); put("unread", 3)
                    put("created_at", "2026-08-01T01:00:00Z")
                }) else emptyList()))
                "crew_chat_sync" -> {
                    if (!inCrew) return refuse("chat_not_member", 403)
                    ok(buildJsonObject {
                        put("room", buildJsonObject {
                            put("crew_id", "afterwork"); put("name", "퇴근런"); put("image_bg", 1); put("image_ver", 1); put("has_image", true)
                            put("owner_id", JUNHO); put("role", if (me == JUNHO) "OWNER" else "MEMBER"); put("member_count", members.size)
                            put("last_rev", 3); put("notify", true); put("notice_count", 2); put("pinned", noticeJson(61)); put("my_read_seq", 3)
                        })
                        put("messages", JsonArray(listOf(
                            Triple(1L, JUNHO, "오늘은 공덕역 2번 출구에서 만나요. 7시 30분에 출발해요!"),
                            Triple(2L, JIYEON, "저도 오늘 함께할게요!"),
                            Triple(3L, JUNHO, "천천히 오세요. 같이 출발해요."),
                        ).map { (id, author, text) ->
                            buildJsonObject {
                                put("id", id); put("seq", id); put("rev", id); put("author_id", author); put("author_name", if (author == JUNHO) "준호" else "지연")
                                put("kind", "TEXT"); put("state", "VISIBLE"); put("body", text); put("has_image", false)
                                put("can_delete", author == me); put("created_at", iso(LocalDate.now(zone).atTime(19, 5 + id.toInt() * 2)))
                            }
                        }))
                        put("reset", true); put("last_rev", 3)
                    })
                }
                "crew_chat_history" -> ok("[]")
                "crew_chat_read" -> ok(3)
                "crew_chat_notices" -> {
                    if (!inCrew) return refuse("chat_not_member", 403)
                    ok(JsonArray(listOf(noticeJson(61), noticeJson(60))))
                }
                "crew_chat_notice" -> {
                    if (!inCrew) return refuse("chat_not_member", 403)
                    val id = long("p_notice") ?: return refuse("notice_missing")
                    if (id != 61L && id != 60L) return refuse("notice_missing")
                    ok(noticeJson(id))
                }
                "crew_chat_notice_save" -> {
                    if (!inCrew) return refuse("chat_not_member", 403)
                    if (me != JUNHO) return refuse("크루장만 할 수 있습니다", 403)
                    val id = long("p_notice") ?: 62L
                    if (args["p_meeting_change"]?.jsonPrimitive?.booleanOrNull == true) {
                        val linked = long("p_meeting")
                        if (linked == null) noticeMeetings.remove(id) else noticeMeetings[id] = linked
                    }
                    ok(noticeJson(id))
                }
                else -> HttpResponse(404, """{"message":"unknown $name"}""")
            }
        }

        companion object {
            const val MEETING_ID = 501L
            const val RUN_JIYEON = 71L
        }
    }

    companion object {
        private const val DOYUN = "u-doyun"
        private const val JUNHO = "u-junho"
        private const val MINSU = "u-minsu"
        private const val JIYEON = "u-jiyeon"
    }
}
