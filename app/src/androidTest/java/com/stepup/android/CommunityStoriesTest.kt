package com.stepup.android

import androidx.compose.ui.semantics.getOrNull
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
import androidx.test.platform.app.InstrumentationRegistry
import com.stepup.android.core.ServiceLocator
import com.stepup.android.data.remote.AuthSession
import com.stepup.android.data.remote.AuthSessionStore
import com.stepup.android.data.remote.AuthUser
import com.stepup.android.data.remote.CommentRow
import com.stepup.android.data.remote.CommunityApi
import com.stepup.android.data.remote.HttpPoster
import com.stepup.android.data.remote.HttpResponse
import com.stepup.android.data.remote.PlaceSearchApi
import com.stepup.android.data.remote.PostRow
import com.stepup.android.data.remote.SessionHolder
import com.stepup.android.data.remote.StepUpServer
import com.stepup.android.data.remote.SupabaseAuth
import com.stepup.android.data.repo.BoardSyncState
import com.stepup.android.data.repo.CommunityRepository
import com.stepup.android.data.repo.PlaceSearch
import com.stepup.android.ui.MainScaffold
import com.stepup.android.ui.Screen
import com.stepup.android.ui.StepPermissions
import com.stepup.android.ui.experience.ExperienceProvider
import com.stepup.android.ui.theme.StepUpTheme
import com.stepup.android.ui.theme.ThemeMode
import androidx.compose.ui.unit.Density
import java.io.File
import java.net.URLDecoder
import java.time.Instant
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.double
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.long
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

/**
 * 동네 이야기(목록형 커뮤니티) — 시안의 31개 장면(screens.json 번호)을 실제 앱 화면으로 따라가며 찍는다.
 *
 * 저장소 · 뷰모델 · 화면은 실제 코드다. 서버만 흉내 낸다([FakeStoryServer]) — 글 · 댓글을 내려주고,
 * 글쓰기는 일부러 붙잡았다가 실패 · 성공을 돌려준다. 장소 검색도 MapTiler 응답 모양 그대로의 고정 답을
 * 쓴다(에뮬레이터 네트워크와 상관없이 같은 장면). 실제 서버 · 실제 검색은 단위 테스트와 schema 테스트가 본다.
 *
 * 위치 권한은 CI 스크립트가 거둔 채 시작한다 — 권한 안내 → 지역 직접 선택 경로로 목록을 연다.
 */
class CommunityStoriesTest {
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

    private val directory get() = File(compose.activity.getExternalFilesDir(null), "community-stories").apply { mkdirs() }
    private val notes = mutableListOf<String>()
    private lateinit var originalRepository: CommunityRepository
    private lateinit var originalPlaceSearch: PlaceSearch
    private val server = FakeStoryServer()

    @After fun restore() {
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

    @Test fun listDetailWriteManageAndStates() {
        runBlocking {
            ServiceLocator.userPrefs.setReducedMotion(true)
            ServiceLocator.userPrefs.setSounds(false)
            ServiceLocator.userPrefs.setHaptics(false)
            ServiceLocator.userPrefs.setGuideSeen()
            ServiceLocator.userPrefs.clearStoryData()
            ServiceLocator.userPrefs.setStoryRegion(null)
        }
        originalRepository = ServiceLocator.communityRepository
        originalPlaceSearch = ServiceLocator.placeSearch
        val repository = CommunityRepository(
            api = CommunityApi(
                StepUpServer(
                    baseUrl = "https://stories.test",
                    apiKey = "sb_publishable_test",
                    sessions = SessionHolder(
                        auth = SupabaseAuth("https://stories.test", "sb_publishable_test", server),
                        store = SignedIn,
                    ),
                    http = server,
                ),
            ),
            postDao = ServiceLocator.database.postDao(),
            commentDao = ServiceLocator.database.commentDao(),
            notificationDao = ServiceLocator.database.notificationDao(),
            prefs = ServiceLocator.userPrefs,
            owner = { OWNER },
            ownerFlow = flowOf(OWNER),
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
        val context = compose.activity

        // ── 26 · 27 권한 안내 → 지역 직접 선택 ──────────────────────────
        awaitTag("stories-tab")
        if (compose.onAllNodesWithTag("stories-location-prompt").fetchSemanticsNodes().isNotEmpty()) {
            shot("26-permission")
            if (StepPermissions.hasLocation(context)) notes += "26-permission: location was granted — the prompt shows the locating state"
            tapTag("stories-choose-region")
        } else {
            notes += "26-permission: location already known — opened 내 주변 from the area label instead"
            tapTag("stories-area")
            awaitTag("story-location")
            shot("26-permission-location-screen")
            tapTag("stories-choose-region")
        }
        awaitTag("story-region")
        compose.onNodeWithTag("story-region-search").performTextInput("여의도")
        awaitText("여의도동")
        shot("27-region")
        compose.onNodeWithText("여의도동").performClick()

        // ── 01 목록 · 02 펼침 ─────────────────────────────────────────
        awaitTag("story-row-301")
        compose.onNodeWithText(context.getString(R.string.story_range_from_region, "1km")).assertIsDisplayed()
        compose.onNodeWithText(context.getString(R.string.story_blue_area_title, "여의도동")).assertIsDisplayed()
        // 1km 밖(샛강, 약 1.05km)과 번개 글은 목록에 없다 — 끝(장소 없는 이전 글)까지 내려가 확인한다
        compose.onNodeWithTag("stories-list").performScrollToNode(hasTestTag("story-row-306"))
        compose.onAllNodesWithTag("story-row-305").assertCountEquals(0)
        compose.onAllNodesWithTag("story-row-307").assertCountEquals(0)
        compose.onNodeWithTag("stories-list").performScrollToIndex(0)
        // 제목과 썸네일은 서로 다른 터치 영역이다
        compose.onNodeWithTag("story-row-301").assertHasClickAction()
        compose.onNodeWithTag("story-thumb-301").assertHasClickAction()
        shot("01-list", settle = 1_800)

        // ── 17 범위 — 고르다 닫으면 그대로, "이 범위로 보기"를 눌러야 바뀐다 ───────────
        tapTag("stories-range")
        awaitTag("story-range-apply")
        tapTag("story-range-M500")
        shot("17-radius")
        tapTag("story-sheet-close")
        compose.onNodeWithText(context.getString(R.string.story_range_from_region, "1km")).assertIsDisplayed()
        tapTag("stories-range")
        tapTag("story-range-KM3")
        tapTag("story-range-apply")
        compose.onNodeWithText(context.getString(R.string.story_range_from_region, "3km")).assertIsDisplayed()
        // 목록 → 상세 → 뒤로: 스크롤 위치와 범위가 그대로다
        openRow(305)
        awaitTag("story-detail")
        pressBack()
        awaitTag("stories-tab")
        compose.onNodeWithTag("story-row-305").assertIsDisplayed()
        // 범위 줄은 목록 맨 위(지역 · 작은 지도)와 함께 올라간다 — 맨 위로 돌아가 그대로인지 본다
        compose.onNodeWithTag("stories-list").performScrollToIndex(0)
        compose.onNodeWithText(context.getString(R.string.story_range_from_region, "3km")).assertIsDisplayed()
        shot("17-radius-applied-3km", settle = 1_500)

        // ── 14 · 15 · 16 지도 → 장소 → 같은 화면 아래 목록이 그 장소로 걸러진다(CM05 · CM06) ─────────
        tapTag("stories-open-map")
        awaitTag("story-map")
        shot("14-map-overview", settle = 2_000)
        compose.onNodeWithTag("story-map-list").performScrollToNode(hasTestTag("story-thumb-302"))
        tapTag("story-thumb-302")
        awaitTag("story-map-selected")
        compose.onNodeWithTag("story-map-selected").assert(hasText("여의나루", substring = true))
        compose.onAllNodesWithTag("story-row-301").assertCountEquals(0)
        shot("15-map-place", settle = 1_500)
        // "전체 장소"는 장소 필터만 푼다 — "이 장소 글 보기" 같은 다음 단계는 없다
        tapTag("story-map-all")
        compose.waitUntil(5_000) { compose.onAllNodesWithTag("story-map-selected").fetchSemanticsNodes().isEmpty() }
        // 뒤로 가기는 들어온 화면(목록)으로 — 고른 장소가 있어도 지도 안에서 한 번 더 머물지 않는다
        pressBack()
        awaitTag("stories-tab")
        scrollToRow(302)
        tapTag("story-thumb-302")
        awaitTag("story-map-selected")
        compose.onNodeWithTag("story-map-selected").assert(hasText("여의나루", substring = true))
        shot("16-place-feed", settle = 1_500)
        pressBack()
        awaitTag("stories-tab")
        awaitTag("story-row-301")

        // ── 03 · 04 · 05 상세 · 댓글 ───────────────────────────────────
        openRow(302)
        awaitTag("story-detail")
        awaitText("첫 완주 축하해요!")
        compose.onNodeWithTag("story-comment-count").assertTextEquals(context.getString(R.string.story_comment_count, 2))
        shot("03-detail", settle = 1_200)
        compose.onNodeWithTag("story-comment-send").assertIsNotEnabled()
        compose.onNodeWithTag("story-comment-input").performClick()
        compose.onNodeWithTag("story-comment-input").performTextInput("저도 다음 주에 도전해볼게요!")
        compose.onNodeWithTag("story-comment-send").assertIsEnabled()
        shot("04-comment-entry", settle = 1_200)
        tapTag("story-comment-send")
        // 성공해야 입력이 비고, 새 댓글과 개수가 함께 바뀐다
        waitFor("comment count 3") {
            runCatching {
                compose.onNodeWithTag("story-comment-count").assertTextEquals(context.getString(R.string.story_comment_count, 3))
            }.isSuccess
        }
        awaitTextField("story-comment-input", "")
        closeKeyboard()
        compose.onNodeWithTag("story-detail-list").performScrollToNode(hasTestTag("story-comment-4100"))
        shot("05-comment-added")

        // ── 21 · 22 · 23 다른 글 메뉴 → 신고 ─────────────────────────────
        tapTag("story-menu")
        awaitTag("story-menu-report")
        shot("21-other-menu")
        tapTag("story-menu-report")
        awaitTag("story-report-submit")
        compose.onNodeWithTag("story-report-submit").assertIsNotEnabled()
        tapTag("story-report-PRIVACY")
        shot("22-report")
        tapTag("story-report-submit")
        awaitTag("story-reported")
        shot("23-reported")
        // 시안의 "개인정보 노출"은 서버 사유 목록에 없어 OTHER 로, 원래 사유는 note 로 간다
        val report = server.calls.last { it.first == "content_report" }.second
        assertTrue(report, "\"p_reason\":\"OTHER\"" in report && "\"p_note\":\"PRIVACY\"" in report)
        tapTag("story-reported-ok")
        pressBack()
        awaitTag("stories-tab")

        // ── 숨기기 → 되돌리기(차단과 다르다) ─────────────────────────────
        openRow(301)
        awaitTag("story-detail")
        tapTag("story-menu")
        tapTag("story-menu-hide")
        awaitTag("story-toast")
        waitFor("story-row-301 removed") { compose.onAllNodesWithTag("story-row-301").fetchSemanticsNodes().isEmpty() }
        shot("21-hidden-with-undo")
        compose.onNodeWithText(context.getString(R.string.story_undo)).performClick()
        scrollToRow(301)
        assertTrue("hiding never blocks the author", server.calls.none { it.first == "user_block" })

        // ── 18 · 20 · 19 · 24 내 글 관리 · 수정 · 작성 중 나가기 ─────────────
        openRow(304)
        awaitTag("story-detail")
        tapTag("story-menu")
        awaitTag("story-menu-edit")
        shot("18-own-menu")
        tapTag("story-menu-delete")
        awaitTag("story-confirm-dialog")
        shot("20-delete-confirm")
        compose.onNodeWithText(context.getString(R.string.common_cancel)).performClick()
        compose.onNodeWithTag("story-detail-title").assertTextEquals("비 오는 날 러닝화 추천해 주세요")
        tapTag("story-menu")
        tapTag("story-menu-edit")
        awaitTag("story-compose")
        awaitTextField("story-compose-text", "비 오는 날 러닝화 추천해 주세요")
        compose.onNodeWithTag("story-compose-place", useUnmergedTree = true).assertTextEquals("여의도공원")
        shot("19-edit")
        compose.onNodeWithTag("story-compose-text").performTextReplacement("비 오는 날 러닝화 추천해 주세요\n쿠션 좋은 걸로요.")
        pressBack()
        awaitTag("story-leave-save")
        shot("24-leave-draft")
        tapTag("story-leave-save")
        awaitTag("story-detail")
        // 화면 상태가 아니라 앱 저장소(DataStore)에 남는다 — 앱을 다시 켜도 이 값을 읽는다
        val saved = runBlocking { ServiceLocator.userPrefs.storyDraft(OWNER).first() }!!
        assertEquals("비 오는 날 러닝화 추천해 주세요\n쿠션 좋은 걸로요.", saved.text)
        assertEquals(304L, saved.editingId)
        assertEquals("여의도공원", saved.place?.name)
        pressBack()
        awaitTag("stories-tab")

        // ── 25 임시저장 이어쓰기 → 수정 완료(같은 글 번호) ─────────────────────
        tapTag("stories-write")
        awaitTag("story-resume-continue")
        shot("25-resume-draft")
        tapTag("story-resume-continue")
        awaitTag("story-compose")
        awaitTextField("story-compose-text", "비 오는 날 러닝화 추천해 주세요\n쿠션 좋은 걸로요.")
        tapTag("story-compose-submit")
        awaitTag("stories-tab")
        val update = server.calls.last { it.first == "story_update" }.second
        assertTrue(update, "\"p_post\":304" in update)
        assertTrue("an edit never creates a new post", server.calls.none { it.first == "story_create" })

        // ── 06 · 07 · 08 · 10 · 09 · 11 새 글 · 장소 고르기 ─────────────────────
        tapTag("stories-write")
        awaitTag("story-compose")
        compose.onNodeWithTag("story-compose-submit").assertIsNotEnabled()
        shot("06-compose-empty")
        compose.onNodeWithTag("story-compose-text").performClick()
        compose.onNodeWithTag("story-compose-text").performTextInput("한강에서 저녁 러닝 같이 하실 분\n7시에 여의나루역 2번 출구 근처에서 천천히 5km 뛰어요.")
        closeKeyboard()
        shot("07-compose-filled")
        tapTag("story-compose-pick")
        awaitTag("story-place-picker")
        shot("08-place-picker")
        compose.onNodeWithTag("story-place-search").performTextInput("달빛러닝공원")
        awaitTag("story-place-empty")
        shot("10-place-empty")
        compose.onNodeWithText(context.getString(R.string.story_place_clear)).performClick()
        compose.onNodeWithTag("story-place-search").performTextInput("한강")
        awaitText("여의도한강공원")
        closeKeyboard()
        shot("09-place-search")
        // 검색 결과는 한 번 누르면 바로 정해진다 — 확인 화면을 거치지 않는다(CM14)
        compose.onNodeWithText("여의도한강공원").performClick()
        awaitTag("story-compose")
        shot("11-place-picked", settle = 1_200)
        compose.onNodeWithTag("story-compose-place", useUnmergedTree = true).assertTextEquals("여의도한강공원")
        // 장소를 고르러 다녀와도 본문은 그대로다
        awaitTextField("story-compose-text", "한강에서 저녁 러닝 같이 하실 분\n7시에 여의나루역 2번 출구 근처에서 천천히 5km 뛰어요.")

        // ── 12 게시 중 → 31 게시 실패 → 13 게시 완료 ─────────────────────────
        val gate = CompletableDeferred<HttpResponse>()
        server.createGate = gate
        tapTag("story-compose-submit")
        awaitText(context.getString(R.string.story_publishing))
        compose.onNodeWithTag("story-compose-submit").assertIsNotEnabled()
        shot("12-publish-pending")
        gate.complete(HttpResponse(503, "busy"))
        awaitTag("story-publish-error")
        awaitTextField("story-compose-text", "한강에서 저녁 러닝 같이 하실 분\n7시에 여의나루역 2번 출구 근처에서 천천히 5km 뛰어요.")
        shot("31-publish-error")
        tapTag("story-compose-submit")
        awaitTag("stories-tab")
        awaitTag("story-row-501")
        awaitTag("story-toast")
        val created = server.calls.last { it.first == "story_create" }.second
        assertTrue(created, "\"p_title\":\"한강에서 저녁 러닝 같이 하실 분\"" in created)
        assertTrue(created, "\"p_place\":\"여의도한강공원\"" in created)
        // 실패한 한 번은 글을 만들지 않았고, 다시 올리기로 한 편만 생겼다
        assertEquals(2, server.calls.count { it.first == "story_create" })
        assertEquals(1, server.created)
        shot("13-posted", settle = 1_500)
        // 확인 메시지는 몇 초 뒤 저절로 닫힌다 — 테스트 시계는 멈춰 있어 그만큼 보낸다(다음 장면에 남지 않게)
        compose.mainClock.advanceTimeBy(4_000)
        compose.waitUntil(5_000) { compose.onAllNodesWithTag("story-toast").fetchSemanticsNodes().isEmpty() }

        // 범위를 기본(1km)으로 돌린다 — 빈 목록에 "더 넓은 범위에서 보기"가 보이게
        tapTag("stories-range")
        tapTag("story-range-KM1")
        tapTag("story-range-apply")
        compose.onNodeWithText(context.getString(R.string.story_range_from_region, "1km")).assertIsDisplayed()

        // ── 28 빈 목록 · 29 불러오는 중 · 30 연결 오류 ──────────────────────
        ServiceLocator.communityRepository.showForTest(emptyList())
        awaitTag("stories-empty")
        // 글이 없을 때는 떠 있는 글쓰기 대신 "첫 글 남기기" 하나(시안 28)
        compose.onAllNodesWithTag("stories-write").assertCountEquals(0)
        shot("28-empty", settle = 1_200)
        ServiceLocator.communityRepository.showBoardStateForTest(BoardSyncState.Loading)
        awaitTag("stories-loading")
        shot("29-loading")
        ServiceLocator.communityRepository.showBoardStateForTest(BoardSyncState.Failed("offline"))
        awaitTag("stories-error")
        shot("30-network-error")
        tapTag("stories-retry")
        awaitTag("story-row-501")

        // ── 큰 글씨(1.3배) — 핵심 행동이 잘리지 않는다 ─────────────────────
        compose.runOnIdle { largeType = true }
        awaitTag("story-row-501")
        compose.onNodeWithTag("stories-write").assertIsDisplayed()
        shot("01-list-large-font", settle = 1_200)
        tapTag("stories-write")
        awaitTag("story-compose")
        compose.onNodeWithTag("story-compose-submit").assertIsDisplayed()
        shot("06-compose-large-font")
        pressBack()
        compose.runOnIdle { largeType = false }
    }

    // ── 도우미 ─────────────────────────────────────────────────────

    private fun awaitTag(tag: String, timeout: Long = 10_000) {
        val present = { compose.onAllNodesWithTag(tag, useUnmergedTree = true).fetchSemanticsNodes().isNotEmpty() }
        // 먼저 그대로 기다리고, 그래도 없으면(화면 밖 목록 줄) 한 번 넘겨서 찾는다 — 넘기기가 다른 화면의 기다림을 흔들지 않게
        if (runCatching { compose.waitUntil((timeout * 2 / 5).coerceAtLeast(2_000)) { present() } }.isSuccess) return
        reveal(tag)
        waitFor("tag '$tag'", (timeout * 3 / 5).coerceAtLeast(2_000)) { present() }
    }

    /** 화면 밖이라 아직 만들어지지 않은 목록 줄(LazyColumn)이면 넘길 수 있는 목록을 그 줄까지 넘긴다 */
    private fun reveal(tag: String) {
        if (compose.onAllNodesWithTag(tag, useUnmergedTree = true).fetchSemanticsNodes().isNotEmpty()) return
        val lists = compose.onAllNodes(androidx.compose.ui.test.hasScrollToNodeAction())
        val count = lists.fetchSemanticsNodes().size
        for (i in 0 until count) {
            if (runCatching { lists[i].performScrollToNode(androidx.compose.ui.test.hasTestTag(tag)) }.isSuccess) return
        }
    }

    /** 기다리다 못 찾으면 무엇을 기다렸는지와 지금 보이는 꼬리표를 남긴다(CI 로그에 스택이 잘려도 어디서 멈췄는지 알 수 있게) */
    private fun waitFor(what: String, timeout: Long = 10_000, condition: () -> Boolean) {
        try {
            compose.waitUntil(timeout) { condition() }
        } catch (e: androidx.compose.ui.test.ComposeTimeoutException) {
            val tags = compose.onAllNodes(androidx.compose.ui.test.SemanticsMatcher("tagged") {
                it.config.getOrNull(androidx.compose.ui.semantics.SemanticsProperties.TestTag) != null
            }, useUnmergedTree = true).fetchSemanticsNodes()
                .mapNotNull { it.config.getOrNull(androidx.compose.ui.semantics.SemanticsProperties.TestTag) }.distinct().take(40)
            throw AssertionError("$what did not happen in ${timeout}ms (tags now: $tags)", e)
        }
    }

    private fun awaitText(text: String, tag: String? = null, timeout: Long = 10_000) {
        waitFor("text '$text'", timeout) { compose.onAllNodesWithText(text, substring = true).fetchSemanticsNodes().isNotEmpty() }
        if (tag != null) awaitTag(tag, timeout)
    }

    private fun awaitTextField(tag: String, expected: String) {
        waitFor("field '$tag' = '$expected'") {
            runCatching { compose.onNodeWithTag(tag).assert(hasText(expected)) }.isSuccess
        }
    }

    /** 목록을 그 글까지 내린다 — 화면 밖 줄은 아직 만들어지지 않았다 */
    private fun scrollToRow(id: Long) {
        waitFor("scroll to story-row-$id") {
            runCatching { compose.onNodeWithTag("stories-list").performScrollToNode(hasTestTag("story-row-$id")) }.isSuccess
        }
    }

    private fun openRow(id: Long) {
        scrollToRow(id)
        compose.onNodeWithTag("story-row-$id").performClick()
        compose.waitForIdle()
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

    /** 실제 화면(시트 · 대화상자 · 키보드 창 포함)을 찍는다. 지도 타일이 올 시간을 준다 */
    private fun shot(name: String, settle: Long = 700) {
        compose.waitForIdle()
        InstrumentationRegistry.getInstrumentation().waitForIdleSync()
        Thread.sleep(settle)
        android.util.Log.i("CommunityStories", "Capturing $name")
        captureDisplay(File(directory, "$name.png"))
    }

    /** MapTiler Geocoding 응답 모양 그대로의 고정 답 — 실제 응답은 PlaceSearchApiTest 가 본다 */
    private fun geocoding(url: String): String? {
        val query = URLDecoder.decode(url.substringAfter("/geocoding/").substringBefore(".json"), "UTF-8")
        return when {
            "types=place,municipality" in url && query == "여의도" -> REGIONS
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

    /**
     * 동네 이야기 서버 흉내 — post_feed · comment_feed 와 story_create · story_update · 좋아요 · 댓글 ·
     * 삭제 · 신고. [createGate] 가 있으면 글쓰기가 그 답을 기다린다(게시 중 장면).
     */
    private class FakeStoryServer : HttpPoster {
        private val json = Json { ignoreUnknownKeys = true; encodeDefaults = true }
        private val rows = seedRows().toMutableList()
        private val comments = seedComments().toMutableMap()
        private var nextPost = 501L
        private var nextComment = 4100L
        val calls = java.util.concurrent.CopyOnWriteArrayList<Pair<String, String>>()
        @Volatile var created = 0
        @Volatile var createGate: CompletableDeferred<HttpResponse>? = null

        override suspend fun get(url: String, headers: Map<String, String>): HttpResponse = synchronized(this) {
            when {
                "/post_feed" in url -> HttpResponse(200, json.encodeToString(ListSerializer(PostRow.serializer()), rows.sortedByDescending { it.createdAt }))
                "/comment_feed" in url -> {
                    val id = Regex("post_id=eq\\.(\\d+)").find(url)!!.groupValues[1].toLong()
                    HttpResponse(200, json.encodeToString(ListSerializer(CommentRow.serializer()), comments[id].orEmpty()))
                }
                else -> HttpResponse(404, "")
            }
        }

        override suspend fun post(url: String, body: String, headers: Map<String, String>): HttpResponse {
            val name = url.substringAfterLast("/rpc/")
            calls += name to body
            val args = runCatching { json.parseToJsonElement(body).jsonObject }.getOrNull()
            if (name == "story_create") createGate?.let { gate -> createGate = null; return gate.await() }
            return synchronized(this) {
                when (name) {
                    "story_create" -> {
                        created++
                        val id = nextPost++
                        rows += PostRow(
                            id = id, category = "FREE", authorId = "me", author = "나",
                            title = args!!.string("p_title"), body = args.string("p_body"),
                            place = args.string("p_place"), placeAddress = args.string("p_place_address"),
                            lat = args["p_lat"]!!.jsonPrimitive.double, lng = args["p_lng"]!!.jsonPrimitive.double,
                            createdAt = Instant.now().toString(), mine = true,
                        )
                        HttpResponse(200, "$id")
                    }
                    "story_update" -> {
                        val id = args!!["p_post"]!!.jsonPrimitive.long
                        val index = rows.indexOfFirst { it.id == id }
                        rows[index] = rows[index].copy(
                            title = args.string("p_title"), body = args.string("p_body"), place = args.string("p_place"),
                            placeAddress = args.string("p_place_address"),
                            lat = args["p_lat"]!!.jsonPrimitive.double, lng = args["p_lng"]!!.jsonPrimitive.double,
                        )
                        HttpResponse(204, "")
                    }
                    "post_toggle_like" -> {
                        val id = args!!["p_post"]!!.jsonPrimitive.long
                        val index = rows.indexOfFirst { it.id == id }
                        val liked = !rows[index].liked
                        rows[index] = rows[index].copy(liked = liked, likes = rows[index].likes + if (liked) 1 else -1)
                        HttpResponse(200, "$liked")
                    }
                    "comment_create" -> {
                        val postId = args!!["p_post"]!!.jsonPrimitive.long
                        val id = nextComment++
                        comments[postId] = comments[postId].orEmpty() + CommentRow(
                            id = id, postId = postId, authorId = "me", author = "나", body = args.string("p_body"),
                            createdAt = Instant.now().toString(), mine = true,
                        )
                        val index = rows.indexOfFirst { it.id == postId }
                        rows[index] = rows[index].copy(commentCount = rows[index].commentCount + 1)
                        HttpResponse(200, "$id")
                    }
                    "post_delete" -> {
                        rows.removeAll { it.id == args!!["p_post"]!!.jsonPrimitive.long }
                        HttpResponse(204, "")
                    }
                    "content_report" -> HttpResponse(204, "")
                    // 러닝 이야기의 기록 칸(0046) — 이 묶음은 러닝 기록이 없는 계정으로 쓴다(상황별은 StoryComposeStatesTest)
                    "story_runs" -> HttpResponse(200, """{"today":"2026-09-28","total":0,"voided":0,"last_ended_at":null,"runs":[]}""")
                    else -> HttpResponse(404, "")
                }
            }
        }

        private fun kotlinx.serialization.json.JsonObject.string(key: String) = this[key]!!.jsonPrimitive.content

        private fun seedRows(): List<PostRow> {
            fun ago(minutes: Long) = Instant.now().minusSeconds(minutes * 60).toString()
            fun story(
                id: Long, author: String, title: String, body: String, place: String, address: String,
                lat: Double, lng: Double, minutes: Long, likes: Int, commentCount: Int, liked: Boolean = false, mine: Boolean = false,
            ) = PostRow(
                id = id, category = "FREE", authorId = if (mine) "me" else "u-$id", author = author, title = title, body = body,
                place = place, placeAddress = address, lat = lat, lng = lng, createdAt = ago(minutes),
                likes = likes, commentCount = commentCount, liked = liked, mine = mine,
            )
            return listOf(
                story(301, "민수", "여의도공원 한 바퀴, 오늘 바람이 좋아요", "\n걷다가 천천히 한 바퀴 달렸어요.",
                    "여의도공원", "서울특별시 영등포구 여의공원로", 37.5261, 126.9225, 12, 8, 1),
                story(302, "수진", "처음으로 5km 완주했어요!", "\n느리게 뛰어도 끝까지 가니까 좋네요.\n다음 주에는 6km 도전!",
                    "여의나루", "서울특별시 영등포구 여의동로", 37.5269, 126.9325, 25, 24, 2, liked = true),
                story(303, "하준", "Sunrise 10K 코스 공유합니다 — 여의도 한강공원에서 마포대교까지 왕복하는 코스예요",
                    "\n물 챙기세요. 편의점은 반환점 근처에 있어요.", "여의도한강공원", "서울특별시 영등포구 여의동로",
                    37.5284, 126.9340, 60, 5, 0),
                story(304, "나", "비 오는 날 러닝화 추천해 주세요", "", "여의도공원", "서울특별시 영등포구 여의공원로",
                    37.5261, 126.9225, 120, 2, 0, mine = true),
                story(305, "지우", "샛강 산책로, 오늘은 한산하네요", "\n공원 한 바퀴부터 가볍게 시작해요.",
                    "샛강생태공원", "서울특별시 영등포구 여의도동", 37.5176, 126.9207, 180, 3, 0),
                // 목록형 이전에 장소 없이 쓴 글 — 지도 · 거리에 들어가지 않고 목록 끝에 남는다
                PostRow(id = 306, category = "FREE", authorId = "u-306", author = "도윤", title = "러닝 초보 질문 있어요",
                    body = "처음엔 몇 분씩 뛰는 게 좋을까요?", createdAt = ago(1_440), likes = 1, commentCount = 0),
                // 번개는 "함께 뛰기"의 것이다 — 동네 이야기에 섞이지 않는다
                PostRow(id = 307, category = "FLASH", authorId = "u-307", author = "Sora", title = "오늘 저녁 7시 번개",
                    place = "여의도 3문", lat = 37.5262, lng = 126.9230, distanceKm = 5.0, meetAt = ago(-120), capacity = 8,
                    createdAt = ago(5), joinedCount = 3),
            )
        }

        private fun seedComments(): Map<Long, List<CommentRow>> {
            fun ago(minutes: Long) = Instant.now().minusSeconds(minutes * 60).toString()
            return mapOf(
                301L to listOf(CommentRow(4003, 301, 0, "u-302", "수진", "바람 좋은 날이었죠.", ago(8))),
                302L to listOf(
                    CommentRow(4001, 302, 0, "u-ara", "아라", "첫 완주 축하해요!", ago(20)),
                    CommentRow(4002, 302, 0, "u-bo", "보현", "저도 다음 주에 같이 뛰어요.", ago(15)),
                ),
            )
        }
    }

    companion object {
        private const val OWNER = "account:story-test"

        private const val REGIONS = """{"type":"FeatureCollection","features":[
            {"id":"place.172857","text":"여의도동","place_type":["place"],"center":[126.9238,37.5267],
             "context":[{"id":"municipality.114175","text":"여의동"},{"id":"county.3244","text":"영등포구"},{"id":"region.1099","text":"서울특별시"}]},
            {"id":"place.172850","text":"영등포동","place_type":["place"],"center":[126.9059,37.5160],
             "context":[{"id":"county.3244","text":"영등포구"},{"id":"region.1099","text":"서울특별시"}]},
            {"id":"place.172851","text":"문래동","place_type":["place"],"center":[126.8949,37.5176],
             "context":[{"id":"county.3244","text":"영등포구"},{"id":"region.1099","text":"서울특별시"}]}]}"""

        private const val HANGANG = """{"type":"FeatureCollection","features":[
            {"id":"poi.1","text":"여의도한강공원","place_type":["poi"],"center":[126.9340,37.5284],
             "context":[{"id":"address.1","text":"여의동로"},{"id":"place.172857","text":"여의도동"},{"id":"county.3244","text":"영등포구"},{"id":"region.1099","text":"서울특별시"}]},
            {"id":"poi.2","text":"여의나루","place_type":["poi"],"center":[126.9325,37.5269],
             "context":[{"id":"address.2","text":"여의동로"},{"id":"county.3244","text":"영등포구"},{"id":"region.1099","text":"서울특별시"}]},
            {"id":"address.3","text":"한강 자전거길","place_type":["address"],"center":[126.9430,37.5349],
             "context":[{"id":"place.9","text":"마포동"},{"id":"county.3250","text":"마포구"},{"id":"region.1099","text":"서울특별시"}]}]}"""
    }
}
