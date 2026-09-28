package com.stepup.android

import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.unit.Density
import androidx.test.platform.app.InstrumentationRegistry
import com.stepup.android.core.ServiceLocator
import com.stepup.android.data.remote.AuthSession
import com.stepup.android.data.remote.AuthSessionStore
import com.stepup.android.data.remote.AuthUser
import com.stepup.android.data.remote.CommunityApi
import com.stepup.android.data.remote.HttpPoster
import com.stepup.android.data.remote.HttpResponse
import com.stepup.android.data.remote.PlaceSearchApi
import com.stepup.android.data.remote.PostRow
import com.stepup.android.data.remote.SessionHolder
import com.stepup.android.data.remote.StepUpServer
import com.stepup.android.data.remote.SupabaseAuth
import com.stepup.android.data.repo.CommunityRepository
import com.stepup.android.data.repo.PlaceSearch
import com.stepup.android.domain.StoryLocalRuns
import com.stepup.android.domain.StoryPlace
import com.stepup.android.service.WalkSessionService
import com.stepup.android.service.WalkSessionState
import com.stepup.android.ui.MainScaffold
import com.stepup.android.ui.Screen
import com.stepup.android.ui.StepPermissions
import com.stepup.android.ui.experience.ExperienceProvider
import com.stepup.android.ui.theme.StepUpTheme
import com.stepup.android.ui.theme.ThemeMode
import java.io.File
import java.net.URLDecoder
import java.time.Instant
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.longOrNull
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

/**
 * 러닝 이야기 글쓰기 — 쉬운 글쓰기 상황별 시안(2026-09-28)의 8개 상황과 지시서의 추가 상태를 실제 앱 화면으로 찍는다.
 *
 * 저장소 · 뷰모델 · 화면은 실제 코드다. 서버만 흉내 낸다([FakeRunsServer]) — 기록 칸(story_runs)은 상황마다 다른
 * 답(서버의 오늘을 9월 28일 · 29일로)을 주고, 글쓰기는 한 번 실패한 뒤 성공한다. 장소 검색도 MapTiler 응답 모양의
 * 고정 답이다. 위치 권한은 CI 스크립트가 거둔 채 시작한다 — "내 주변"이 위치 없이 장소 고르기(07)로 이어진다.
 *
 * 캡처: story-compose/<번호>-<상황>.png. 번호는 시안(01~08)과 같고, 09 이후는 시안에 없는 추가 상태다.
 */
class StoryComposeStatesTest {
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

    private val directory get() = File(compose.activity.getExternalFilesDir(null), "story-compose").apply { mkdirs() }
    private val notes = mutableListOf<String>()
    private lateinit var originalRepository: CommunityRepository
    private lateinit var originalPlaceSearch: PlaceSearch
    private val server = FakeRunsServer()
    @Volatile private var local = StoryLocalRuns()
    @Volatile private var reverseName = "마포대교 남단"

    @After fun restore() {
        WalkSessionService.showStateForTest(WalkSessionState())
        compose.runOnUiThread {
            androidx.core.view.WindowInsetsControllerCompat(compose.activity.window, compose.activity.window.decorView)
                .hide(androidx.core.view.WindowInsetsCompat.Type.ime())
        }
        if (::originalRepository.isInitialized) ServiceLocator.useCommunityForTest(originalRepository, originalPlaceSearch)
        runBlocking {
            ServiceLocator.userPrefs.clearStoryData()
            ServiceLocator.userPrefs.setStoryRegion(null)
        }
        File(directory, "capture-notes.txt").writeText(notes.joinToString("\n"))
    }

    @Test fun eightSituationsAndExtraStates() {
        // 앞 검사(같은 에뮬레이터)가 남긴 러닝 저장본이 있으면 앱이 그 러닝을 되살려 "러닝 중"이 된다 —
        // 기록 칸이 러닝 중 칸으로 바뀌어 01 부터 어긋난다(2026-09-28 PR #59 Experience QA). 시작 전에 비운다.
        clearAnyRunCheckpointForTest()
        WalkSessionService.recovery.value = null
        WalkSessionService.showStateForTest(WalkSessionState())
        runBlocking {
            ServiceLocator.userPrefs.setReducedMotion(true)
            ServiceLocator.userPrefs.setSounds(false)
            ServiceLocator.userPrefs.setHaptics(false)
            ServiceLocator.userPrefs.setGuideSeen()
            ServiceLocator.userPrefs.clearStoryData()
            ServiceLocator.userPrefs.setStoryRegion(StoryPlace("여의도동", "", 37.5267, 126.9238))
        }
        originalRepository = ServiceLocator.communityRepository
        originalPlaceSearch = ServiceLocator.placeSearch
        val repository = CommunityRepository(
            api = CommunityApi(
                StepUpServer(
                    baseUrl = "https://runs.test",
                    apiKey = "sb_publishable_test",
                    sessions = SessionHolder(auth = SupabaseAuth("https://runs.test", "sb_publishable_test", server), store = SignedIn),
                    http = server,
                ),
            ),
            postDao = ServiceLocator.database.postDao(),
            commentDao = ServiceLocator.database.commentDao(),
            notificationDao = ServiceLocator.database.notificationDao(),
            prefs = ServiceLocator.userPrefs,
            owner = { OWNER },
            ownerFlow = flowOf(OWNER),
            localRuns = { _, _ -> local },
        )
        ServiceLocator.useCommunityForTest(
            repository,
            PlaceSearch(PlaceSearchApi(key = "test", fetch = ::geocoding), platform = null, language = { "ko" }),
        )
        compose.activityRule.scenario.onActivity {
            it.enableEdgeToEdge(
                statusBarStyle = SystemBarStyle.dark(android.graphics.Color.TRANSPARENT),
                navigationBarStyle = SystemBarStyle.dark(android.graphics.Color.TRANSPARENT),
            )
        }
        var largeType by mutableStateOf(false)
        compose.setContent {
            val density = LocalDensity.current
            CompositionLocalProvider(LocalDensity provides Density(density.density, if (largeType) 1.3f else 1f)) {
                StepUpTheme(ThemeMode.DARK) { ExperienceProvider { MainScaffold(initialTab = Screen.Community) } }
            }
        }
        try {
            scenes { largeType = it }
        } catch (failure: Throwable) {
            // 실패한 순간의 화면을 남긴다 — 검사가 끝난 뒤 찍는 화면은 앱이 닫힌 뒤라 원인을 보여 주지 못한다
            runCatching { shot("zz-failure", settle = 0) }
            throw failure
        }
    }

    private fun scenes(setLargeType: (Boolean) -> Unit) {
        val context = compose.activity
        awaitTag("stories-tab")
        awaitTag("stories-write")

        // ── 01 한 번도 뛰지 않은 사람 — 코스 없이 장소와 질문만으로 게시 ────────────
        server.runs = ok(runsJson("2026-09-28", total = 0))
        local = StoryLocalRuns()
        openCompose()
        awaitTag("story-record-never")
        awaitTag("story-phrases-BEGINNER")
        assertCount("story-src-RECENT", 0)
        assertCount("story-src-COURSE", 0)
        compose.onNodeWithTag("story-compose-submit").assertIsNotEnabled()
        assertNote(context.getString(R.string.story_note_need_place))
        shot("01-never-run-empty")
        pickFromList("여의도공원")
        tapTag("story-phrase-START")
        awaitTextField("story-compose-text", "러닝을 시작해요.\n처음에는 얼마나 뛰면 좋을까요?")
        compose.onNodeWithTag("story-compose-submit").assertIsEnabled()
        assertNote(context.getString(R.string.story_note_never))
        shot("01-never-run")
        leaveDiscarding()

        // ── 02 4일 이상 지난 기록만 있는 사람 — 지난 코스 첨부만 제한, 글쓰기는 가능 ───────
        runBlocking { ServiceLocator.userPrefs.addStoryRecentPlace(OWNER, StoryPlace("마포대교 남단", "", 37.5305, 126.9352), 5) }
        server.runs = ok(runsJson("2026-09-28", total = 6, last = "2026-09-24T11:00:00Z"))
        local = StoryLocalRuns(count = 6)
        openCompose()
        awaitTag("story-record-old")
        awaitText("마지막 러닝 · 9월 24일 · 4일 전")
        awaitTag("story-phrases-RETURNING")
        awaitTag("story-src-RECENT")
        searchFromRow("도화동", "도화동")
        tapTag("story-phrase-AGAIN")
        awaitTextField("story-compose-text", "다시 달려보려고요.\n며칠 쉬었는데 가볍게 시작해볼게요.")
        assertNote(context.getString(R.string.story_note_old))
        shot("02-only-old-runs")
        leaveDiscarding()

        // ── 03 3일 전 기록이 있는 사람 — 9/28 기준 9/25 기록까지 첨부 ────────────────
        server.runs = ok(runsJson("2026-09-28", total = 7, last = RUN_MAPO.ended, runs = listOf(RUN_MAPO)))
        local = StoryLocalRuns(count = 7)
        reverseName = "마포대교 남단"
        openCompose()
        awaitTag("story-record-attached")
        compose.onNodeWithTag("story-record-date", useUnmergedTree = true).assertTextEquals("9월 25일 · 3일 전")
        compose.onNodeWithTag("story-record-km", useUnmergedTree = true).assertTextEquals("2.10")
        awaitTextField("story-compose-text", "9월 25일 2.10km 달렸어요.")
        tapTag("story-src-COURSE")
        awaitTag("story-place-picker")
        pickRow("마포대교 남단")
        awaitTextField("story-compose-text", "9월 25일 마포대교에서 2.10km 달렸어요.")
        compose.onNodeWithTag("story-compose-place", useUnmergedTree = true).assertTextEquals("마포대교 남단")
        compose.onNodeWithTag("story-compose-submit").assertIsEnabled()
        assertNote(context.getString(R.string.story_note_deadline_today, 3))
        shot("03-three-days-eligible", settle = 1_800)

        // 기록 고르기 — 오늘 · 어제 · 2일 전 · 3일 전으로 거르고, 고른 날만 비면 전체로 되돌린다(지시서 추가 상태)
        tapTag("story-record-pick")
        awaitTag("story-runs-sheet")
        awaitTag("story-run-41")
        shot("09-record-picker", settle = 1_500)
        tapTag("story-runs-day-1")
        awaitTag("story-runs-empty-day")
        shot("09b-record-picker-empty-day")
        tapTag("story-runs-show-all")
        awaitTag("story-run-41")
        tapTag("story-sheet-close")
        // 쓰던 글을 임시저장 — 다음 날(9/29) 다시 연다
        pressBack()
        tapTag("story-leave-save")
        awaitTag("stories-tab")
        val saved = runBlocking { ServiceLocator.userPrefs.storyDraft(OWNER).first() }!!
        assertEquals(41L, saved.run?.run?.id)
        assertEquals("마포대교 남단", saved.place?.name)

        // ── 04 임시글의 코스가 만료된 사람 — 9/29 재진입, 글과 장소는 보존 ─────────────
        server.runs = ok(runsJson("2026-09-29", total = 7, last = RUN_MAPO.ended))
        tapTag("stories-write")
        tapTag("story-resume-continue")
        awaitTag("story-compose")
        awaitTag("story-record-expired")
        awaitText("9월 25일 기록이 4일 전 기록이 됐어요.")
        awaitTextField("story-compose-text", "9월 25일 마포대교에서 2.10km 달렸어요.")
        compose.onNodeWithTag("story-compose-place", useUnmergedTree = true).assertTextEquals("마포대교 남단")
        compose.onNodeWithTag("story-compose-submit").assertIsNotEnabled()
        assertNote(context.getString(R.string.story_note_expired))
        assertCount("story-src-COURSE", 0)
        shot("04-expired-draft")
        // 코스만 빼기 — 본문 · 장소는 그대로, 일반 글로 올릴 수 있다
        tapTag("story-record-remove")
        awaitTag("story-record-old")
        awaitTextField("story-compose-text", "9월 25일 마포대교에서 2.10km 달렸어요.")
        compose.onNodeWithTag("story-compose-submit").assertIsEnabled()
        shot("04b-course-removed")
        leaveDiscarding()

        // ── 05 달렸지만 코스가 없는 사람 — 거리 · 시간은 공유, 장소는 직접 선택 ─────────
        server.runs = ok(runsJson("2026-09-28", total = 8, last = RUN_NO_GPS.ended, runs = listOf(RUN_NO_GPS)))
        local = StoryLocalRuns(count = 8)
        openCompose()
        awaitTag("story-record-summary")
        awaitText("오늘 · 3.24km · 24분 18초")
        awaitTextField("story-compose-text", "9월 28일 3.24km 달렸어요.")
        compose.onNodeWithTag("story-compose-place", useUnmergedTree = true)
            .assertTextEquals(context.getString(R.string.story_compose_place_empty))
        compose.onNodeWithTag("story-compose-submit").assertIsNotEnabled()
        assertNote(context.getString(R.string.story_note_need_place_run))
        assertCount("story-src-COURSE", 0)
        shot("05-run-without-gps")

        // ── 07 현재 위치를 쓸 수 없는 사람 — 권한 없이도 검색 · 지도에서 장소 선택 ────────
        tapTag("story-src-NEARBY")
        if (StepPermissions.hasLocation(context)) {
            notes += "07: location permission was granted on this device — 내 주변 opened the picker; captured the picker instead"
            awaitTag("story-place-picker")
            shot("07-location-unavailable")
            pressBack()
            awaitTag("story-compose")
            tapTag("story-src-SEARCH")
        } else {
            awaitTag("story-noloc-sheet")
            shot("07-location-unavailable")
            tapTag("story-noloc-search")
        }
        awaitTag("story-place-picker")
        compose.onNodeWithTag("story-place-search").performTextInput("한강")
        awaitText("여의도한강공원")
        closeKeyboard()
        pickRow("여의도한강공원")
        awaitTextField("story-compose-text", "9월 28일 여의도한강공원에서 3.24km 달렸어요.")
        compose.onNodeWithTag("story-compose-submit").assertIsEnabled()
        assertNote(context.getString(R.string.story_note_summary))
        shot("07b-place-without-location")
        leaveDiscarding()

        // ── 06 기록을 불러오지 못한 사람 — 기록 없음으로 처리하지 않고 재시도 ──────────────
        server.runs = HttpResponse(503, "busy")
        openCompose()
        awaitTag("story-record-error")
        awaitTag("story-phrases-BEGINNER")
        pickFromList("여의도공원")
        tapTag("story-phrase-QUESTION")
        awaitTextField("story-compose-text", "오늘 날씨에 달리기 괜찮을까요?")
        compose.onNodeWithTag("story-compose-submit").assertIsEnabled()
        assertNote(context.getString(R.string.story_note_error))
        shot("06-record-fetch-error")
        // 다시 불러오기 — 이번엔 서버가 답한다(쓴 글은 그대로)
        server.runs = ok(runsJson("2026-09-28", total = 0))
        tapTag("story-record-retry")
        awaitTag("story-record-never")
        awaitTextField("story-compose-text", "오늘 날씨에 달리기 괜찮을까요?")
        leaveDiscarding()

        // ── 08 글 올리기에 실패한 사람 — 코스 · 장소 · 본문을 그대로 두고 재시도 ─────────────
        server.runs = ok(runsJson("2026-09-28", total = 9, last = RUN_YEOUIDO.ended, runs = listOf(RUN_YEOUIDO)))
        local = StoryLocalRuns(count = 9)
        reverseName = "여의도 한강공원"
        openCompose()
        awaitTag("story-record-attached")
        compose.onNodeWithTag("story-record-date", useUnmergedTree = true).assertTextEquals("오늘 오전 7:12")
        tapTag("story-src-COURSE")
        awaitTag("story-place-picker")
        pickRow("여의도 한강공원")
        awaitTextField("story-compose-text", "9월 28일 여의도에서 3.24km 달렸어요.")
        server.createAnswers += HttpResponse(503, "busy")
        tapTag("story-compose-submit")
        awaitTag("story-publish-error")
        awaitText(context.getString(R.string.story_publish_retry))
        awaitTextField("story-compose-text", "9월 28일 여의도에서 3.24km 달렸어요.")
        awaitTag("story-record-attached")
        assertNote(context.getString(R.string.story_note_failed), tag = "story-publish-error")
        shot("08-post-failed", settle = 1_800)
        server.createAnswers += HttpResponse(200, "701")
        tapTag("story-compose-submit")
        awaitTag("stories-tab")
        awaitTag("story-row-701")
        // 같은 요청 키로 두 번 — 결과를 모른 채 다시 올려도 한 편만 생긴다(서버 0046 이 키로 거른다)
        val creates = server.calls.filter { it.first == "story_create" }.map { it.second }
        assertEquals(2, creates.size)
        assertTrue(creates[0], "\"p_run\":43" in creates[0] && "\"p_run\":43" in creates[1])
        val key = Regex("\"p_client_key\":\"([^\"]+)\"").find(creates[0])!!.groupValues[1]
        assertTrue(creates[1], "\"p_client_key\":\"$key\"" in creates[1])
        shot("08b-posted", settle = 1_500)
        // 읽는 사람에게 보이는 첨부 — 상세의 러닝 카드
        scrollToRow(701)
        compose.onNodeWithTag("story-row-701").performClick()
        awaitTag("story-detail")
        awaitTag("story-run-card")
        shot("10-detail-run-card", settle = 1_800)
        pressBack()
        awaitTag("stories-tab")

        // ── 11 불러오는 중 — 처음 · 오래 쉼으로 나누지 않고 문구 버튼은 빈 자리 ────────────
        val gate = CompletableDeferred<HttpResponse>()
        server.runsGate = gate
        openCompose()
        awaitTag("story-record-loading")
        awaitTag("story-phrases-loading")
        shot("11-loading")
        gate.complete(ok(runsJson("2026-09-28", total = 0)))
        awaitTag("story-record-never")
        leaveDiscarding()

        // ── 12 방금 달린 러닝이 서버 확인 전 — 오래 쉬었다고 하지 않는다 ─────────────────
        server.runs = ok(runsJson("2026-09-28", total = 5, last = "2026-09-20T11:00:00Z"))
        local = StoryLocalRuns(count = 6, pendingRecent = true)
        openCompose()
        awaitTag("story-record-pending")
        shot("12-pending")
        leaveDiscarding()

        // ── 13 러닝 진행 중 — 진행 중인 코스는 아직 붙일 수 없다 ──────────────────────
        WalkSessionService.showStateForTest(WalkSessionState(isActive = true))
        server.runs = ok(runsJson("2026-09-28", total = 5, last = "2026-09-20T11:00:00Z"))
        local = StoryLocalRuns(count = 5)
        openCompose()
        awaitTag("story-record-active")
        shot("13-active-run")
        leaveDiscarding()
        WalkSessionService.showStateForTest(WalkSessionState())

        // ── 큰 글씨(1.3배) — 코스 카드 · 올리기가 잘리지 않는다 ─────────────────────
        server.runs = ok(runsJson("2026-09-28", total = 7, last = RUN_MAPO.ended, runs = listOf(RUN_MAPO)))
        compose.runOnIdle { setLargeType(true) }
        openCompose()
        awaitTag("story-record-attached")
        compose.onNodeWithTag("story-compose-submit").assertIsDisplayed()
        shot("14-large-font", settle = 1_500)
        leaveDiscarding()
        compose.runOnIdle { setLargeType(false) }
    }

    // ── 도우미 ─────────────────────────────────────────────────────

    private fun openCompose() {
        awaitTag("stories-write")
        tapTag("stories-write")
        // 앞 장면의 임시저장이 남았으면 새로 쓴다
        if (compose.onAllNodesWithTag("story-resume-new").fetchSemanticsNodes().isNotEmpty()) tapTag("story-resume-new")
        awaitTag("story-compose")
    }

    /** 뒤로 — 바뀐 것이 있으면 "삭제하고 나가기" */
    private fun leaveDiscarding() {
        closeKeyboard()
        pressBack()
        compose.waitUntil(10_000) {
            compose.onAllNodesWithTag("story-leave-discard").fetchSemanticsNodes().isNotEmpty() ||
                compose.onAllNodesWithTag("stories-tab").fetchSemanticsNodes().isNotEmpty()
        }
        if (compose.onAllNodesWithTag("story-leave-discard").fetchSemanticsNodes().isNotEmpty()) tapTag("story-leave-discard")
        awaitTag("stories-tab")
    }

    /** 장소 줄 → 가까운 장소 목록에서 고른다(버튼을 고른 모습이 남지 않는다) */
    private fun pickFromList(name: String) {
        tapTag("story-compose-pick")
        awaitTag("story-place-picker")
        pickRow(name)
    }

    /** 장소 줄 → 이름으로 찾아 고른다 */
    private fun searchFromRow(query: String, name: String) {
        tapTag("story-compose-pick")
        awaitTag("story-place-picker")
        compose.onNodeWithTag("story-place-search").performTextInput(query)
        awaitText(name)
        closeKeyboard()
        pickRow(name)
    }

    private fun pickRow(name: String) {
        val row = hasTestTag("story-place-row") and hasText(name, substring = true)
        compose.waitUntil(10_000) { compose.onAllNodes(row).fetchSemanticsNodes().isNotEmpty() }
        compose.onAllNodes(row).onFirst().performClick()
        awaitTag("story-place-confirm")
        tapTag("story-place-choose")
        awaitTag("story-compose")
    }

    private fun assertNote(expected: String, tag: String = "story-compose-note") {
        compose.waitUntil(10_000) {
            runCatching { compose.onNodeWithTag(tag, useUnmergedTree = true).assertTextEquals(expected) }.isSuccess
        }
    }

    private fun assertCount(tag: String, count: Int) {
        compose.onAllNodesWithTag(tag).assertCountEquals(count)
    }

    private fun awaitTag(tag: String, timeout: Long = 10_000) {
        compose.waitUntil(timeout) { compose.onAllNodesWithTag(tag, useUnmergedTree = true).fetchSemanticsNodes().isNotEmpty() }
    }

    private fun awaitText(text: String, timeout: Long = 10_000) {
        compose.waitUntil(timeout) {
            compose.onAllNodesWithText(text, substring = true, useUnmergedTree = true).fetchSemanticsNodes().isNotEmpty()
        }
    }

    private fun awaitTextField(tag: String, expected: String) {
        compose.waitUntil(10_000) {
            runCatching { compose.onNodeWithTag(tag).assert(hasText(expected)) }.isSuccess
        }
    }

    private fun scrollToRow(id: Long) {
        compose.waitUntil(10_000) {
            runCatching { compose.onNodeWithTag("stories-list").performScrollToNode(hasTestTag("story-row-$id")) }.isSuccess
        }
    }

    private fun tapTag(tag: String) {
        awaitTag(tag)
        val node = compose.onNodeWithTag(tag)
        runCatching { node.performScrollTo() }
        node.performClick()
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
        Thread.sleep(400)
    }

    /** 실제 화면(시트 · 키보드 창 포함)을 찍는다. 지도 타일이 올 시간을 준다 */
    private fun shot(name: String, settle: Long = 700) {
        compose.waitForIdle()
        InstrumentationRegistry.getInstrumentation().waitForIdleSync()
        Thread.sleep(settle)
        awaitFrameOnScreen(compose.activity)
        android.util.Log.i("StoryComposeStates", "Capturing $name")
        captureDisplay(File(directory, "$name.png"))
    }

    /** MapTiler Geocoding 모양의 고정 답 — 이름 검색과 좌표 → 이름(코스 주변) */
    private fun geocoding(url: String): String? {
        val query = URLDecoder.decode(url.substringAfter("/geocoding/").substringBefore(".json"), "UTF-8")
        val reverse = Regex("^(-?\\d+\\.\\d+),(-?\\d+\\.\\d+)$").find(query)
        return when {
            reverse != null -> {
                val (lng, lat) = reverse.destructured
                """{"type":"FeatureCollection","features":[{"id":"poi.77","text":"$reverseName","place_type":["poi"],"center":[$lng,$lat],
                    "context":[{"id":"place.1","text":"여의도동"},{"id":"county.3244","text":"영등포구"},{"id":"region.1099","text":"서울특별시"}]}]}"""
            }
            query == "도화동" -> DOHWA
            query == "한강" -> HANGANG
            else -> """{"type":"FeatureCollection","features":[]}"""
        }
    }

    private object SignedIn : AuthSessionStore {
        override suspend fun load() = AuthSession(
            accessToken = "token", refreshToken = "refresh", expiresIn = 3600, expiresAt = 9_999_999_999L,
            user = AuthUser(id = "me", isAnonymous = false),
        )
        override suspend fun save(session: AuthSession) = Unit
        override suspend fun clear() = Unit
    }

    /** 러닝 한 줄(서버 story_runs 모양) */
    private data class Run(
        val id: Long, val started: String, val ended: String, val meters: Int, val seconds: Int, val day: String, val route: String,
    ) {
        fun json() = """{"id":$id,"started_at":"$started","ended_at":"$ended","distance_m":$meters,"duration_s":$seconds,"day":"$day","route":"$route"}"""
    }

    private fun ok(body: String) = HttpResponse(200, body)

    private fun runsJson(today: String, total: Int, voided: Int = 0, last: String? = null, runs: List<Run> = emptyList()) =
        """{"today":"$today","total":$total,"voided":$voided,"last_ended_at":${last?.let { "\"$it\"" } ?: "null"},""" +
            """"runs":[${runs.joinToString(",") { it.json() }}]}"""

    /**
     * 서버 흉내 — post_feed · story_runs · story_create. [runsGate] 가 있으면 기록 칸이 그 답을 기다린다(불러오는 중),
     * [createAnswers] 에 쌓인 답을 글쓰기가 차례로 쓴다(없으면 성공). 붙인 러닝은 서버처럼 글에 옮겨 적는다.
     */
    private class FakeRunsServer : HttpPoster {
        private val json = Json { ignoreUnknownKeys = true; encodeDefaults = true }
        private val rows = seedRows().toMutableList()
        val calls = java.util.concurrent.CopyOnWriteArrayList<Pair<String, String>>()
        @Volatile var runs: HttpResponse = HttpResponse(200, """{"today":"2026-09-28","total":0,"runs":[]}""")
        @Volatile var runsGate: CompletableDeferred<HttpResponse>? = null
        val createAnswers = java.util.concurrent.ConcurrentLinkedQueue<HttpResponse>()

        override suspend fun get(url: String, headers: Map<String, String>): HttpResponse = synchronized(this) {
            when {
                "/post_feed" in url -> HttpResponse(200, json.encodeToString(ListSerializer(PostRow.serializer()), rows.sortedByDescending { it.createdAt }))
                "/comment_feed" in url -> HttpResponse(200, "[]")
                else -> HttpResponse(404, "")
            }
        }

        override suspend fun post(url: String, body: String, headers: Map<String, String>): HttpResponse {
            val name = url.substringAfterLast("/rpc/")
            calls += name to body
            if (name == "story_runs") {
                runsGate?.let { gate -> runsGate = null; return gate.await() }
                return runs
            }
            if (name != "story_create") return HttpResponse(404, "")
            val answer = createAnswers.poll() ?: HttpResponse(200, "")
            if (answer.status !in 200..299) return answer
            val args = json.parseToJsonElement(body).jsonObject
            val id = answer.body.trim().toLongOrNull() ?: 700L
            val run = ALL_RUNS.firstOrNull { it.id == args["p_run"]?.jsonPrimitive?.longOrNull }
            synchronized(this) {
                rows += PostRow(
                    id = id, category = "FREE", authorId = "me", author = "나",
                    title = args["p_title"]!!.jsonPrimitive.content, body = args["p_body"]!!.jsonPrimitive.content,
                    place = args["p_place"]!!.jsonPrimitive.content, placeAddress = args["p_place_address"]!!.jsonPrimitive.content,
                    lat = args["p_lat"]!!.jsonPrimitive.content.toDouble(), lng = args["p_lng"]!!.jsonPrimitive.content.toDouble(),
                    createdAt = Instant.now().toString(), mine = true,
                    runStartedAt = run?.started, runEndedAt = run?.ended, runDistanceM = run?.meters, runDurationS = run?.seconds,
                    runRoute = run?.route,
                )
            }
            return HttpResponse(200, "$id")
        }

        private fun seedRows(): List<PostRow> {
            fun ago(minutes: Long) = Instant.now().minusSeconds(minutes * 60).toString()
            return listOf(
                PostRow(id = 301, category = "FREE", authorId = "u-301", author = "민수", title = "여의도공원 한 바퀴, 오늘 바람이 좋아요",
                    body = "\n걷다가 천천히 한 바퀴 달렸어요.", place = "여의도공원", placeAddress = "서울특별시 영등포구 여의공원로",
                    lat = 37.5261, lng = 126.9225, createdAt = ago(12), likes = 8, commentCount = 1),
                PostRow(id = 302, category = "FREE", authorId = "u-302", author = "수진", title = "처음으로 5km 완주했어요!",
                    body = "", place = "여의나루", placeAddress = "서울특별시 영등포구 여의동로",
                    lat = 37.5269, lng = 126.9325, createdAt = ago(25), likes = 24, commentCount = 2),
            )
        }
    }

    companion object {
        private const val OWNER = "account:story-compose-test"

        /** 9월 25일 20:30(한국) 끝 · 2.10km · 16분 48초 · 마포대교 남단 둘레의 코스 */
        private val RUN_MAPO = Run(
            41, "2026-09-25T11:13:12Z", "2026-09-25T11:30:00Z", 2100, 1008, "2026-09-25",
            "37.52830,126.93320;37.52905,126.93405;37.52990,126.93470;37.53040,126.93610;37.53120,126.93690;" +
                "37.53205,126.93760;37.53260,126.93890;37.53340,126.93960;37.53390,126.94080;37.53300,126.94120;" +
                "37.53200,126.94010;37.53110,126.93930;37.53010,126.93820;37.52930,126.93700;37.52860,126.93560",
        )

        /** 오늘 10:24(한국) 끝 · 3.24km · 24분 18초 · 경로 없음 */
        private val RUN_NO_GPS = Run(42, "2026-09-28T01:00:00Z", "2026-09-28T01:24:18Z", 3240, 1458, "2026-09-28", "")

        /** 오늘 07:12(한국) 끝 · 3.24km · 24분 18초 · 여의도 한강공원 코스 */
        private val RUN_YEOUIDO = Run(
            43, "2026-09-27T21:47:42Z", "2026-09-27T22:12:00Z", 3240, 1458, "2026-09-28",
            "37.52610,126.93010;37.52680,126.93150;37.52760,126.93270;37.52850,126.93390;37.52930,126.93520;" +
                "37.53010,126.93660;37.53080,126.93800;37.53010,126.93900;37.52920,126.93780;37.52840,126.93640;" +
                "37.52760,126.93500;37.52690,126.93360;37.52630,126.93200",
        )

        private val ALL_RUNS = listOf(RUN_MAPO, RUN_NO_GPS, RUN_YEOUIDO)

        private const val DOHWA = """{"type":"FeatureCollection","features":[
            {"id":"place.200","text":"도화동","place_type":["place"],"center":[126.9500,37.5395],
             "context":[{"id":"county.3250","text":"마포구"},{"id":"region.1099","text":"서울특별시"}]}]}"""

        private const val HANGANG = """{"type":"FeatureCollection","features":[
            {"id":"poi.1","text":"여의도한강공원","place_type":["poi"],"center":[126.9340,37.5284],
             "context":[{"id":"address.1","text":"여의동로"},{"id":"place.172857","text":"여의도동"},{"id":"county.3244","text":"영등포구"},{"id":"region.1099","text":"서울특별시"}]},
            {"id":"poi.2","text":"여의나루","place_type":["poi"],"center":[126.9325,37.5269],
             "context":[{"id":"address.2","text":"여의동로"},{"id":"county.3244","text":"영등포구"},{"id":"region.1099","text":"서울특별시"}]}]}"""
    }
}
