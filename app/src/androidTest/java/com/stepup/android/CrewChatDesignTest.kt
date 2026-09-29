package com.stepup.android

import android.net.Uri
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
import com.stepup.android.data.remote.HttpPoster
import com.stepup.android.data.remote.HttpResponse
import com.stepup.android.data.remote.PartyApi
import com.stepup.android.data.remote.SessionHolder
import com.stepup.android.data.remote.StepUpServer
import com.stepup.android.data.remote.SupabaseAuth
import com.stepup.android.data.repo.CrewCardRepository
import com.stepup.android.data.repo.CrewChatRepository
import com.stepup.android.data.repo.CrewImageStore
import com.stepup.android.data.repo.CrewRepository
import com.stepup.android.ui.MainScaffold
import com.stepup.android.ui.Screen
import com.stepup.android.ui.experience.ExperienceProvider
import com.stepup.android.ui.screens.community.chat.ChatDeviceNotificationsForTest
import com.stepup.android.ui.screens.community.chat.ChatPhotoPickerForTest
import com.stepup.android.ui.theme.StepUpTheme
import com.stepup.android.ui.theme.ThemeMode
import java.io.File
import java.net.URLDecoder
import java.time.LocalDateTime
import java.time.ZoneId
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.CopyOnWriteArrayList
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.booleanOrNull
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.jsonArray
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
 * 크루 채팅(2026-09-29) — 커뮤니티 세 번째 탭 · 대화 · 정보 · 공지 · 검색 · 사진 · 관리를 실제 앱 화면으로 찍는다.
 *
 * 저장소 · 뷰모델 · 화면은 실제 코드다. 서버만 흉내 낸다([FakeChatServer]) — crew_chat_* 함수와 crew_roster ·
 * crew_person · crew_member_remove 가 0048 과 같은 모양의 답을 준다. 이름 · 대화 · 인원은 시안 패키지의 예시(퇴근런 ·
 * 준호 · 도윤 · 지연 · 민수)를 검사 자료로만 쓴다(앱 코드에는 없다). 같은 대화를 도윤(크루원) · 준호(크루장) 시점으로 연다.
 *
 * 캡처: crew-chat/<시안 번호>-<장면>.png — 번호는 시안(01~44)과 같다.
 */
class CrewChatDesignTest {
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

    private val directory get() = File(compose.activity.getExternalFilesDir(null), "crew-chat").apply { mkdirs() }
    private val notes = CopyOnWriteArrayList<String>()
    private lateinit var originalCards: CrewCardRepository
    private lateinit var originalChat: CrewChatRepository
    private lateinit var originalHome: com.stepup.android.data.repo.CrewHomeRepository
    private val server = FakeChatServer()

    @After fun restore() {
        ChatPhotoPickerForTest.answer = null
        ChatPhotoPickerForTest.unavailable = false
        ChatDeviceNotificationsForTest.enabled = null
        if (::originalCards.isInitialized) ServiceLocator.useCrewCardsForTest(originalCards)
        if (::originalChat.isInitialized) ServiceLocator.useCrewChatForTest(originalChat)
        if (::originalHome.isInitialized) ServiceLocator.useCrewHomeForTest(originalHome)
        runCatching { File(directory, "capture-notes-${System.currentTimeMillis()}.txt").writeText(notes.joinToString("\n")) }
    }

    // ─────────────────────────────────────────────────────────────
    // 크루원(도윤) — 목록 · 불러오기 실패 · 대화 · 메뉴 · 답장 · 삭제 · 전송 실패 · 재접속 · 새 메시지
    // ─────────────────────────────────────────────────────────────

    @Test fun memberRoom() {
        server.seed(me = DOYUN)
        launch(DOYUN)
        guard {
            awaitTag("chat-row-afterwork")
            awaitTag("chat-row-hanbakwi")
            shot("01-chat-tab", settle = 1_500)

            // 36 — 첫 불러오기 실패(목록의 제목 · 이미지는 그대로), 다시 불러오기
            server.syncStatus = 503
            tapTag("chat-row-afterwork")
            awaitTag("chat-room-error")
            shot("36-room-error")
            server.syncStatus = 200
            tapTag("chat-room-error-retry")
            awaitText("천천히 오세요. 같이 출발해요.")
            awaitTag("chat-pinned")
            shot("02-member-room", settle = 1_500)
            // 크루원에게는 공지 관리가 없다
            assertTrue(compose.onAllNodesWithTag("chat-pinned-manage", useUnmergedTree = true).fetchSemanticsNodes().isEmpty())

            longPress("저도 오늘 함께할게요!")
            awaitTag("chat-menu")
            assertNone("chat-menu-hide")
            assertNone("chat-menu-notice")
            shot("14-message-menu")
            tapTag("crew-sheet-close")

            longPress("좋아요! 저는 5분 정도")
            awaitTag("chat-menu-mine")
            awaitTag("chat-menu-delete")
            shot("15-own-message-menu")
            tapTag("crew-sheet-close")

            // 16 답장 — 원문 이름 · 일부, X 는 답장 연결만 취소(쓴 글은 그대로)
            longPress("오늘은 공덕역 2번 출구에서")
            tapTag("chat-menu-reply")
            awaitTag("chat-reply-bar")
            typeInto("chat-input", "네, 2번 출구에서 뵐게요.")
            closeKeyboard()
            shot("16-reply")
            tapTag("chat-reply-cancel")
            assertNone("chat-reply-bar")
            compose.onNodeWithTag("chat-input").assert(hasText("네, 2번 출구에서 뵐게요."))
            typeInto("chat-input", "")
            closeKeyboard()

            // 17 → 18 내 메시지 삭제 — 자리는 그대로 "삭제된 메시지예요."
            longPress("좋아요! 저는 5분 정도")
            tapTag("chat-menu-delete")
            awaitTag("chat-delete")
            shot("17-delete-message")
            tapTag("chat-delete-yes")
            awaitText("삭제된 메시지예요.")
            shot("18-message-deleted")

            // 19 전송 실패 — 내용은 남고 재전송 · 지우기. 재전송은 같은 요청 키(서버에는 하나)
            server.sendStatus = 400
            typeInto("chat-input", "좋아요! 저는 5분 정도 먼저 가 있을게요.")
            tapTag("chat-send")
            awaitTag("chat-failed")
            closeKeyboard()
            shot("19-send-failed")
            server.sendStatus = 200
            tapTag("chat-retry")
            awaitGone("chat-failed")
            assertEquals(1, server.sentBy(DOYUN, "좋아요! 저는 5분 정도 먼저 가 있을게요."))

            // 20 연결이 끊김 — 보낸 것은 전송 대기, 쓰던 글은 남고 보내기 · 사진은 잠김
            server.syncStatus = 503
            server.sendStatus = 503
            server.storeWhenLost = true
            typeInto("chat-input", "곧 도착해요!")
            tapTag("chat-send")
            awaitTag("chat-reconnecting")
            awaitTag("chat-pending")
            typeInto("chat-input", "저도 곧 도착해요.")
            closeKeyboard()
            shot("20-reconnecting")
            // 다시 이어지면 요청 키로 확인 — 서버가 받은 것은 한 번만 보인다(다시 보내지 않는다)
            server.syncStatus = 200
            server.sendStatus = 200
            server.storeWhenLost = false
            awaitGone("chat-reconnecting", 20_000)
            awaitGone("chat-pending", 20_000)
            assertEquals(1, compose.onAllNodesWithText("곧 도착해요!", useUnmergedTree = true).fetchSemanticsNodes().size)
            assertEquals(1, server.sentBy(DOYUN, "곧 도착해요!"))
            compose.onNodeWithTag("chat-input").assert(hasText("저도 곧 도착해요."))
            shot("20-reconnected")
            typeInto("chat-input", "")
            closeKeyboard()

            // 21 이전 대화를 보는 중 — 새 메시지가 와도 위치를 두고 버튼
            repeat(3) { compose.onNodeWithTag("chat-messages").performTouchInput { swipeDown() } }
            compose.waitForIdle()
            Thread.sleep(800)
            server.post(JUNHO, "준호", "저는 벌써 출발했어요.")
            server.post(JIYEON, "지연", "저도 지금 나가요!")
            server.post(MINSU, "민수", "5분 늦어요, 먼저 출발해 주세요.")
            awaitTag("chat-new-messages", 20_000)
            shot("21-new-messages")
            tapTag("chat-new-messages")
            awaitGone("chat-new-messages")
            awaitText("5분 늦어요, 먼저 출발해 주세요.")

            // 보낸 답장 — 원문 참조가 남는다
            longPress("오늘은 공덕역 2번 출구에서")
            tapTag("chat-menu-reply")
            typeInto("chat-input", "네, 2번 출구에서 뵐게요.")
            tapTag("chat-send")
            awaitTag("chat-quote")
            closeKeyboard()
            shot("16-reply-sent")
        }
    }

    // ─────────────────────────────────────────────────────────────
    // 크루원 — 사진(25 · 26 · 42 · 27 · 41) · 신고(28 · 29)
    // ─────────────────────────────────────────────────────────────

    @Test fun memberPhotoAndReport() {
        server.seed(me = DOYUN)
        launch(DOYUN)
        guard {
            openRoom("afterwork")
            tapTag("chat-attach")
            awaitTag("chat-attach-sheet")
            shot("25-attachment")
            ChatPhotoPickerForTest.answer = Uri.fromFile(asset("crew/afterwork.png", "chat-pick.png"))
            tapTag("chat-attach-pick")
            awaitTag("chat-photo-preview")
            typeInto("chat-photo-caption", "크루 사진 이걸로 어때요?")
            closeKeyboard()
            shot("26-image-preview", settle = 1_200)

            // 42 — 사진 · 설명은 그대로, 같은 요청 키로 다시
            server.sendStatus = 503
            tapTag("chat-photo-send")
            awaitTag("chat-photo-failed")
            shot("42-photo-error")
            server.sendStatus = 200
            tapTag("chat-photo-retry")
            awaitTag("chat-room")
            awaitTag("chat-photo")
            awaitText("크루 사진 이걸로 어때요?")
            shot("27-photo-sent", settle = 1_500)
            assertEquals(1, server.sentBy(DOYUN, "크루 사진 이걸로 어때요?"))

            // 41 받은 사진 — 보내기 버튼이 없다
            compose.onAllNodesWithTag("chat-photo", useUnmergedTree = true).onLast().performClick()
            awaitTag("chat-viewer")
            awaitTag("chat-viewer-image")
            assertNone("chat-photo-send")
            shot("41-image-viewer", settle = 1_200)
            tapTag("chat-viewer-room")
            awaitTag("chat-room")

            // 28 → 29 신고 — 사유를 골라야 보낸다(사진을 보낸 뒤에도 화면에 있는 크루장의 메시지)
            longPress("천천히 오세요. 같이 출발해요.")
            tapTag("chat-menu-report")
            awaitTag("chat-report")
            tapTag("chat-report-abuse")
            shot("28-report")
            tapTag("chat-report-send")
            awaitTag("chat-reported")
            shot("29-report-sent")
            assertEquals("ABUSE", server.reports.single().second)
            tapTag("crew-result-button")
            awaitTag("chat-room")
        }
    }

    // ─────────────────────────────────────────────────────────────
    // 크루원 — 정보(04) · 알림(24 · 37) · 크루원(06 · 07) · 공지(08 · 09) · 검색(22 · 23) · 혼자인 새 크루(30)
    // ─────────────────────────────────────────────────────────────

    @Test fun memberInfo() {
        server.seed(me = DOYUN)
        launch(DOYUN)
        guard {
            openRoom("afterwork")
            tapTag("chat-room-more")
            awaitTag("chat-info")
            awaitTag("chat-info-leave")
            assertNone("chat-info-write")
            shot("04-member-info", settle = 1_200)

            ChatDeviceNotificationsForTest.enabled = false
            tapTag("chat-info-notify")
            awaitTag("chat-notify")
            shot("24-notifications")
            tapTag("chat-notify-on")
            tapTag("chat-notify-apply")
            awaitTag("chat-os")
            shot("37-notification-permission")
            tapTag("chat-os-later")
            awaitGone("chat-os")

            // 06 크루원 — 내 크루 홈(4번)부터 채팅방 정보의 크루원 보기는 홈의 크루원(04)
            tapTag("chat-info-members")
            awaitTag("home-members-page")
            awaitTag("home-member-$MINSU")
            shot("06-members")
            pressBack()
            awaitTag("chat-info")
            // 07 크루원 프로필 — 대화에서 민수의 얼굴을 누르면
            pressBack()
            server.post(MINSU, "민수", "저도 7시 반에 갈게요.")
            awaitText("저도 7시 반에 갈게요.")
            tapFace("민")
            awaitTag("chat-member")
            awaitTag("chat-member-public")
            assertNone("chat-member-remove")
            shot("07-member-profile")
            pressBack()
            tapTag("chat-room-more")
            awaitTag("chat-info")

            tapTag("chat-info-notices")
            awaitTag("chat-notices-pinned")
            shot("09-notices")
            tapTag("chat-notices-pinned")
            awaitTag("chat-notice-title")
            awaitTag("chat-notice-body")
            shot("08-notice-detail")
            pressBack()
            pressBack()

            // 22 → 23 검색 — 이 방의 허용된 내역만. 결과를 누르면 그 메시지로
            tapTag("chat-info-search")
            awaitTag("chat-search")
            typeInto("chat-search-field", "공덕역")
            awaitTag("chat-search-results")
            closeKeyboard()
            shot("22-search")
            typeInto("chat-search-field", "내일 아침")
            awaitTag("chat-search-empty")
            closeKeyboard()
            shot("23-search-empty")
            typeInto("chat-search-field", "공덕역에서 출발")
            awaitTag("chat-search-results")
            closeKeyboard()
            compose.onAllNodesWithTag("chat-search-row").onFirst().performClick()
            awaitTag("chat-room")
            awaitText("공덕역에서 출발하나요?")
            shot("22-search-to-message")

            // 30 혼자 있는 새 크루 — 인사를 남길 수 있다(뒤로 가면 대화 목록)
            pressBack()
            awaitTag("chat-row-hanbakwi")
            tapTag("chat-row-hanbakwi")
            awaitTag("chat-solo")
            typeInto("chat-input", "안녕하세요! 같이 달려요.")
            closeKeyboard()
            shot("30-first-chat", settle = 1_200)
        }
    }

    // ─────────────────────────────────────────────────────────────
    // 크루원 — 참여 종료(32) · 가입한 크루가 없음(31)
    // ─────────────────────────────────────────────────────────────

    @Test fun memberEnded() {
        server.seed(me = DOYUN)
        launch(DOYUN)
        guard {
            openRoom("afterwork")
            // 크루장이 내보냈다 · 크루가 해산됐다 — 다음 확인에서 32, 그 방의 대화는 이 폰에서도 지운다
            server.leave("afterwork", DOYUN)
            server.dissolve("hanbakwi")
            awaitTag("chat-ended", 20_000)
            shot("32-access-lost")
            assertTrue(ServiceLocator.crewChat.snapshot("afterwork") == null)
            tapTag("chat-ended-list")
            awaitTag("chat-none")
            shot("31-no-crew")
            assertNone("chat-row-afterwork")
        }
    }

    // ─────────────────────────────────────────────────────────────
    // 크루장(준호) — 03 · 38 · 39 · 40 · 11 · 12 · 05 · 10 · 43 · 13 · 44 · 33 · 34 · 35
    // ─────────────────────────────────────────────────────────────

    @Test fun ownerRoom() {
        server.seed(me = JUNHO)
        launch(JUNHO)
        guard {
            openRoom("afterwork")
            awaitTag("chat-room-owner")
            awaitTag("chat-pinned-manage")
            shot("03-owner-room", settle = 1_500)

            longPress("저도 오늘 함께할게요!")
            awaitTag("chat-menu-owner")
            awaitTag("chat-menu-hide")
            awaitTag("chat-menu-notice")
            shot("38-owner-message-menu")
            tapTag("chat-menu-hide")
            awaitTag("chat-hide")
            shot("39-hide-message")
            tapTag("chat-hide-yes")
            awaitText("크루장이 숨긴 메시지예요.")
            shot("40-message-hidden")

            // 11 수정 — 그 공지의 내용 그대로 · 12 삭제 확인(취소하면 그대로)
            tapTag("chat-pinned-manage")
            awaitTag("chat-notice-edit")
            awaitTextField("chat-notice-title-field", "오늘 19:30 · 공덕역 2번 출구")
            shot("11-edit-notice")
            tapTag("chat-notice-delete")
            awaitTag("chat-notice-delete-confirm")
            shot("12-delete-notice")
            tapTag("crew-sheet-close")
            pressBack()
            awaitTag("chat-room-owner")

            tapTag("chat-room-more")
            awaitTag("chat-info-owner")
            awaitTag("chat-info-write")
            shot("05-owner-info", settle = 1_200)

            // 10 → 43 → 13 공지 등록 — 실패해도 쓴 내용과 기존 공지가 그대로, 다시 저장하면 대화로
            tapTag("chat-info-write")
            awaitTag("chat-notice-write")
            typeInto("chat-notice-title-field", "오늘은 러닝을 쉬어요")
            typeInto("chat-notice-body-field", "오늘은 비가 와서 러닝을 쉬어요.\n\n다음 모임은 목요일 저녁 7시 30분,\n공덕역 2번 출구에서 만나요.")
            closeKeyboard()
            shot("10-write-notice")
            server.noticeStatus = 503
            tapTag("chat-notice-save")
            awaitTag("chat-notice-failed")
            shot("43-notice-save-error")
            assertEquals("오늘 19:30 · 공덕역 2번 출구", server.pinnedTitle("afterwork"))
            server.noticeStatus = 200
            tapTag("chat-notice-retry")
            awaitTag("chat-room-owner")
            awaitText("새 공지를 등록했어요")
            awaitText("오늘은 러닝을 쉬어요")
            shot("13-notice-published")
            assertEquals(1, compose.onAllNodesWithText("새 공지를 등록했어요", substring = true, useUnmergedTree = true).fetchSemanticsNodes().size)

            // 44 작성 중인 공지 — 게시하지 않고 초안으로
            tapTag("chat-room-more")
            tapTag("chat-info-write")
            awaitTag("chat-notice-write")
            typeInto("chat-notice-title-field", "토요일 아침 러닝")
            closeKeyboard()
            pressBack()
            awaitTag("chat-notice-draft")
            shot("44-notice-draft")
            tapTag("chat-notice-keep")
            awaitTag("chat-info-owner")

            // 33 → 34 → 35 크루원 내보내기 — 대화에서 민수의 얼굴을 누르면. 25명에서 24명, 대화에 한 줄
            // (채팅방 정보의 크루원 보기 · 멤버 관리는 4번부터 기존 멤버 관리(55)로 간다)
            pressBack()
            awaitTag("chat-room-owner")
            server.post(MINSU, "민수", "저 오늘은 조금 늦어요.")
            awaitText("저 오늘은 조금 늦어요.")
            tapFace("민")
            awaitTag("chat-member-owner")
            awaitTag("chat-member-remove")
            shot("33-owner-profile")
            tapTag("chat-member-remove")
            awaitTag("chat-remove")
            shot("34-remove-member")
            tapTag("chat-remove-yes")
            awaitTag("chat-room-owner")
            awaitText("민수 님이 크루에서 나갔어요.")
            awaitText("크루원 24명")
            shot("35-member-removed")
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
            baseUrl = "https://chat.test",
            apiKey = "sb_publishable_test",
            sessions = SessionHolder(auth = SupabaseAuth("https://chat.test", "sb_publishable_test", server), store = SignedIn(me)),
            http = server,
        )
        val crewApi = CrewApi(stepUp)
        val db = ServiceLocator.database
        val crews = CrewRepository(crewApi, db.crewDao(), db.crewInfoDao(), db.walkSessionDao(), ServiceLocator.rewardRepository, PartyApi(stepUp))
        val owner = "account:$me"
        val cards = CrewCardRepository(
            api = crewApi,
            crews = crews,
            images = CrewImageStore(fetch = crewApi::image, dir = File(context.cacheDir, "chat-test-images").apply { deleteRecursively() }),
            prefs = ServiceLocator.userPrefs,
            owner = { owner },
            ownerFlow = flowOf(owner),
            draftDir = File(context.filesDir, "chat_test_drafts").apply { deleteRecursively() },
        )
        val chat = CrewChatRepository(
            api = CrewChatApi(stepUp),
            crewApi = crewApi,
            prefs = ServiceLocator.userPrefs,
            owner = { owner },
            photoDir = File(context.cacheDir, "chat-test-photos").apply { deleteRecursively() },
        )
        originalCards = ServiceLocator.crewCards
        originalChat = ServiceLocator.crewChat
        originalHome = ServiceLocator.crewHome
        ServiceLocator.useCrewCardsForTest(cards)
        ServiceLocator.useCrewChatForTest(chat)
        // 채팅방 정보의 크루원 보기(4번 홈의 크루원) — "나"를 이 검사의 사람으로
        ServiceLocator.useCrewHomeForTest(
            com.stepup.android.data.repo.CrewHomeRepository(com.stepup.android.data.remote.CrewHomeApi(stepUp), owner = { owner }),
        )
        server.images["afterwork"] = asset64("crew/afterwork.png")
        compose.activityRule.scenario.onActivity {
            it.enableEdgeToEdge(
                statusBarStyle = SystemBarStyle.dark(android.graphics.Color.TRANSPARENT),
                navigationBarStyle = SystemBarStyle.dark(android.graphics.Color.TRANSPARENT),
            )
        }
        compose.setContent { StepUpTheme(ThemeMode.DARK) { ExperienceProvider { MainScaffold(initialTab = Screen.Community) } } }
        awaitTag("community-tab-chat")
        tapTag("community-tab-chat")
        awaitTag("chat-list")
    }

    private fun openRoom(id: String) {
        awaitTag("chat-row-$id")
        tapTag("chat-row-$id")
        // 크루원 방은 chat-room, 크루장 방은 chat-room-owner — 둘 다 대화 목록이 있다
        awaitTag("chat-messages", 20_000)
        awaitText("천천히 오세요. 같이 출발해요.")
    }

    /** 실패한 순간의 화면을 남기고 다시 던진다 */
    private fun guard(body: () -> Unit) {
        try {
            body()
        } catch (failure: Throwable) {
            runCatching { shot("zz-failure-${System.currentTimeMillis()}", settle = 0) }
            runCatching { File(directory, "zz-failure-tree.txt").writeText(compose.onAllNodes(isRoot()).printToString()) }
            throw failure
        }
    }

    /** 말풍선을 길게 누른다 — 글자가 든 말풍선(누르는 곳) */
    private fun longPress(text: String) {
        awaitText(text)
        compose.onAllNodesWithText(text, substring = true).onFirst().performTouchInput { longClick() }
        compose.waitForIdle()
    }

    /** 대화의 얼굴(첫 글자)을 누른다 — 가장 아래(최근) 것 */
    private fun tapFace(initial: String) {
        val matcher = hasTestTag("chat-face") and hasText(initial)
        val end = android.os.SystemClock.uptimeMillis() + 20_000
        while (compose.onAllNodes(matcher).fetchSemanticsNodes().isEmpty()) {
            if (android.os.SystemClock.uptimeMillis() > end) throw AssertionError("face '$initial' did not appear")
            compose.waitForIdle()
            Thread.sleep(40)
        }
        compose.onAllNodes(matcher).onLast().performClick()
        compose.waitForIdle()
    }

    private fun assertNone(tag: String) {
        assertTrue("'$tag' should not be shown", compose.onAllNodesWithTag(tag, useUnmergedTree = true).fetchSemanticsNodes().isEmpty())
    }

    private fun awaitTag(tag: String, timeout: Long = 20_000) {
        val end = android.os.SystemClock.uptimeMillis() + timeout
        while (compose.onAllNodesWithTag(tag, useUnmergedTree = true).fetchSemanticsNodes().isEmpty()) {
            if (android.os.SystemClock.uptimeMillis() > end) {
                val tags = compose.onAllNodes(SemanticsMatcher("chat tag") { node ->
                    node.config.getOrNull(SemanticsProperties.TestTag)?.let { it.startsWith("chat-") || it.startsWith("crew-") } == true
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

    private fun awaitTextField(tag: String, expected: String) {
        val end = android.os.SystemClock.uptimeMillis() + 20_000
        while (runCatching { compose.onNodeWithTag(tag).assert(hasText(expected)) }.isFailure) {
            if (android.os.SystemClock.uptimeMillis() > end) throw AssertionError("'$tag' never read '$expected'")
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
        android.util.Log.i("CrewChat", "Capturing $name")
        captureDisplay(File(directory, "$name.png"))
        notes += name
    }

    private fun asset(path: String, name: String): File {
        val out = File(compose.activity.cacheDir, name)
        InstrumentationRegistry.getInstrumentation().context.assets.open(path).use { input -> out.outputStream().use { input.copyTo(it) } }
        return out
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
     * 0048 과 같은 모양의 답 — 방 목록 · 따라오기(rev) · 이전 대화 · 보내기(요청 키로 한 번) · 확인 · 읽음 · 삭제 · 숨김 ·
     * 사진 · 검색 · 신고 · 알림 · 공지 · 크루원. 지금 멤버가 아니면 chat_not_member, 크루장만 할 일은 "크루장만".
     */
    private class FakeChatServer : HttpPoster {
        class Person(val id: String, val name: String, var read: Long, val joinedAt: String = "2026-08-03T01:00:00Z", val weekKm: Double = 0.0)

        class Notice(val id: Long, var title: String, var body: String, var pinned: Boolean, val authorId: String, val authorName: String, val createdAt: String)

        class Msg(
            val id: Long,
            val seq: Long,
            var rev: Long,
            val authorId: String?,
            val authorName: String?,
            val kind: String,
            val body: String?,
            val replyTo: Long? = null,
            val event: String? = null,
            val eventName: String? = null,
            val clientId: String? = null,
            var state: String = "VISIBLE",
            val createdAt: String,
            val image: String? = null,
        )

        class Room(val id: String, val name: String, var ownerId: String, val bg: Int, val hasImage: Boolean) {
            val members = CopyOnWriteArrayList<Person>()
            val messages = CopyOnWriteArrayList<Msg>()
            val notices = CopyOnWriteArrayList<Notice>()
            val notify = ConcurrentHashMap<String, Boolean>()
            @Volatile var lastRev = 0L
        }

        private val json = Json { ignoreUnknownKeys = true }
        val rooms = ConcurrentHashMap<String, Room>()
        val images = ConcurrentHashMap<String, String>()
        val reports = CopyOnWriteArrayList<Pair<Long, String>>()
        @Volatile var me = DOYUN
        @Volatile var syncStatus = 200
        @Volatile var sendStatus = 200
        /** 보내기를 서버가 받았지만 답이 사라졌다(503) */
        @Volatile var storeWhenLost = false
        @Volatile var noticeStatus = 200
        private var nextId = 5_000L
        private var nextNotice = 70L

        private fun at(day: Int, hour: Int, minute: Int): String =
            LocalDateTime.of(2026, 9, day, hour, minute).atZone(ZoneId.systemDefault()).toOffsetDateTime().toString()

        private fun now(): String = java.time.OffsetDateTime.now().toString()

        /**
         * 퇴근런(25명 · 크루장 준호 · 고정 공지 하나 · 지난 공지 하나)과 도윤이 혼자 만든 공덕 한바퀴.
         * 읽은 위치는 시안의 숫자대로 — 도윤의 "좋아요!"를 안 읽은 7명, 준호의 첫 메시지를 안 읽은 3명 · 마지막을 안 읽은 9명.
         */
        fun seed(me: String) {
            this.me = me
            rooms.clear(); reports.clear()
            val afterwork = Room("afterwork", "퇴근런", JUNHO, bg = 1, hasImage = true)
            // 시안의 퇴근런 25명 — 준호 · 도윤 · 민수 · 지연과 21명
            val names = listOf("서연", "현우", "지우", "하린", "유진", "태오", "수아", "민재", "예린", "도현", "시우", "서아", "준서", "하은", "지호", "채원", "건우", "다은", "은우", "소윤", "지안")
            afterwork.members += Person(JUNHO, "준호", 104, joinedAt = "2026-08-01T01:00:00Z", weekKm = 24.0)
            afterwork.members += Person(DOYUN, "도윤", if (me == DOYUN) 30 else 104, weekKm = 8.2)
            afterwork.members += Person(MINSU, "민수", 30, weekKm = 12.4)
            afterwork.members += Person(JIYEON, "지연", 104, weekKm = 16.0)
            // 첫 메시지(101)를 안 읽은 사람 — 민수 포함 셋, 101 · 102 까지 읽은 넷, 103 까지 읽은 둘, 나머지는 모두 읽었다
            names.forEachIndexed { index, name ->
                val read = when {
                    index < 2 -> 30L
                    index < 6 -> 101L + (index % 2)
                    index < 8 -> 103L
                    else -> 104L
                }
                afterwork.members += Person("u-${index + 10}", name, read)
            }
            // 지난 대화 — 이전 대화로 올려 볼 만큼(21), 검색되는 민수의 질문(22)
            val older = listOf(
                Triple(MINSU, "민수", "공덕역에서 출발하나요?"),
                Triple(JUNHO, "준호", "네, 2번 출구 앞이에요."),
                Triple(JIYEON, "지연", "이번 주는 화·목 두 번이죠?"),
                Triple(JUNHO, "준호", "맞아요. 비 오면 공지로 알려드릴게요."),
            )
            var seq = 1L
            for (day in 25..27) {
                for ((index, line) in older.withIndex()) {
                    val (id, name, text) = line
                    val body = if (day == 25) text else "$text ${day}일"
                    afterwork.messages += Msg(seq, seq, seq, id, name, "TEXT", body, createdAt = at(day, 18, 40 + index * 3))
                    seq++
                }
                for (extra in 0 until 5) {
                    val who = listOf(JIYEON to "지연", MINSU to "민수", JUNHO to "준호")[extra % 3]
                    afterwork.messages += Msg(seq, seq, seq, who.first, who.second, "TEXT", "${day}일 러닝 이야기 ${extra + 1}", createdAt = at(day, 20, extra * 4))
                    seq++
                }
            }
            afterwork.messages += Msg(101, 101, 101, JUNHO, "준호", "TEXT", "오늘은 공덕역 2번 출구에서 만나요. 7시 30분에 출발해요!", createdAt = at(28, 19, 5))
            afterwork.messages += Msg(102, 102, 102, JIYEON, "지연", "TEXT", "저도 오늘 함께할게요!", createdAt = at(28, 19, 7))
            afterwork.messages += Msg(103, 103, 103, DOYUN, "도윤", "TEXT", "좋아요! 저는 5분 정도 먼저 가 있을게요.", clientId = "seed-103", createdAt = at(28, 19, 9))
            afterwork.messages += Msg(104, 104, 104, JUNHO, "준호", "TEXT", "천천히 오세요. 같이 출발해요.", createdAt = at(28, 19, 12))
            afterwork.lastRev = 104
            afterwork.notices += Notice(
                61, "오늘 19:30 · 공덕역 2번 출구",
                "오늘 저녁 7시 30분에 출발해요.\n\n모이는 곳은 공덕역 2번 출구예요.\n처음 오시는 분도 편하게 함께해요.\n\n천천히 3km를 달리고 돌아올게요.",
                pinned = true, authorId = JUNHO, authorName = "준호", createdAt = at(28, 18, 30),
            )
            afterwork.notices += Notice(60, "이번 주 러닝 일정", "화·목 저녁에 함께 달려요.", pinned = false, authorId = JUNHO, authorName = "준호", createdAt = at(25, 12, 0))
            rooms[afterwork.id] = afterwork

            val hanbakwi = Room("hanbakwi", "공덕 한바퀴", DOYUN, bg = 0, hasImage = false)
            hanbakwi.members += Person(DOYUN, "도윤", 0)
            rooms[hanbakwi.id] = hanbakwi
        }

        /** 보이는(지우지 않은) 메시지 중 이 사람이 보낸 같은 글 — 재전송이 두 개가 되지 않았는지 */
        fun sentBy(user: String, body: String): Int =
            rooms.values.sumOf { room -> room.messages.count { it.authorId == user && it.body == body && it.state == "VISIBLE" } }

        fun pinnedTitle(crew: String): String? = rooms[crew]?.notices?.firstOrNull { it.pinned }?.title

        fun post(user: String, name: String, body: String) {
            val room = rooms.getValue("afterwork")
            synchronized(room) {
                val rev = ++room.lastRev
                room.messages += Msg(nextId++, rev, rev, user, name, "TEXT", body, createdAt = now())
            }
        }

        fun leave(crew: String, user: String) {
            val room = rooms[crew] ?: return
            synchronized(room) {
                val person = room.members.firstOrNull { it.id == user } ?: return
                room.members.remove(person)
                val rev = ++room.lastRev
                room.messages += Msg(nextId++, rev, rev, null, null, "SYSTEM", null, event = "MEMBER_LEFT", eventName = person.name, createdAt = now())
            }
        }

        fun dissolve(crew: String) {
            rooms.remove(crew)
        }

        private fun Room.member(user: String) = members.any { it.id == user }

        private fun msgJson(room: Room, m: Msg): JsonObject = buildJsonObject {
            put("id", m.id); put("seq", m.seq); put("rev", m.rev)
            put("author_id", m.authorId); put("author_name", m.authorName)
            put("kind", m.kind); put("state", m.state)
            put("body", if (m.state == "VISIBLE") m.body else null)
            put("has_image", m.kind == "IMAGE" && m.state == "VISIBLE")
            put("event", m.event); put("event_name", m.eventName)
            m.replyTo?.let { rid ->
                room.messages.firstOrNull { it.id == rid }?.let { r ->
                    put("reply", buildJsonObject {
                        put("id", r.id); put("seq", r.seq); put("author_id", r.authorId); put("author_name", r.authorName)
                        put("kind", r.kind); put("state", r.state); put("body", if (r.state == "VISIBLE") r.body?.take(120) else null)
                    })
                }
            }
            put("client_id", if (m.authorId == me) m.clientId else null)
            put("can_delete", m.authorId == me && m.state == "VISIBLE" && m.kind != "SYSTEM")
            put("created_at", m.createdAt)
        }

        private fun noticeJson(room: Room, n: Notice): JsonObject = buildJsonObject {
            put("id", n.id); put("crew_id", room.id); put("title", n.title); put("body", n.body); put("pinned", n.pinned)
            put("author_id", n.authorId); put("author_name", n.authorName); put("created_at", n.createdAt); put("updated_at", n.createdAt)
        }

        private fun metaJson(room: Room): JsonObject = buildJsonObject {
            put("crew_id", room.id); put("name", room.name); put("image_bg", room.bg); put("image_ver", if (room.hasImage) 1 else 0)
            put("has_image", room.hasImage); put("owner_id", room.ownerId); put("role", if (room.ownerId == me) "OWNER" else "MEMBER")
            put("member_count", room.members.size); put("last_rev", room.lastRev); put("notify", room.notify[me] ?: true)
            put("notice_count", room.notices.size)
            room.notices.firstOrNull { it.pinned }?.let { put("pinned", noticeJson(room, it)) }
            put("my_read_seq", room.members.firstOrNull { it.id == me }?.read ?: 0L)
            put("reads", JsonArray(room.members.map { p -> buildJsonObject { put("user_id", p.id); put("seq", p.read); put("joined_at", p.joinedAt) } }))
        }

        private fun roomJson(room: Room): JsonObject = buildJsonObject {
            put("crew_id", room.id); put("name", room.name); put("image_bg", room.bg); put("image_ver", if (room.hasImage) 1 else 0)
            put("has_image", room.hasImage); put("owner_id", room.ownerId); put("role", if (room.ownerId == me) "OWNER" else "MEMBER")
            put("member_count", room.members.size)
            room.messages.maxByOrNull { it.seq }?.let { put("last", msgJson(room, it)) }
            val read = room.members.firstOrNull { it.id == me }?.read ?: 0L
            put("unread", room.messages.count { it.seq > read && it.authorId != me && it.kind != "SYSTEM" })
            put("created_at", "2026-09-01T01:00:00Z")
        }

        private fun ok(body: Any): HttpResponse = HttpResponse(200, body.toString())
        private fun refuse(message: String, status: Int = 400) = HttpResponse(status, """{"message":"$message"}""")

        override suspend fun get(url: String, headers: Map<String, String>): HttpResponse = when {
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
            fun roomArg(): Room? = rooms[text("p_crew")]
            fun messageArg(): Pair<Room, Msg>? {
                val id = long("p_message") ?: return null
                for (room in rooms.values) room.messages.firstOrNull { it.id == id }?.let { return room to it }
                return null
            }
            return when (name) {
                "crew_chat_rooms" -> ok(JsonArray(rooms.values.filter { it.member(me) }.sortedByDescending { it.lastRev }.map(::roomJson)))
                "crew_chat_sync" -> {
                    val room = roomArg()?.takeIf { it.member(me) } ?: return refuse("chat_not_member", 403)
                    if (syncStatus != 200) return refuse("unavailable", syncStatus)
                    val since = long("p_since_rev")
                    val reset = since == null || since > room.lastRev || room.lastRev - since > 300
                    val messages = if (reset) room.messages.sortedBy { it.seq }.takeLast(50) else room.messages.filter { it.rev > since!! }.sortedBy { it.seq }
                    ok(buildJsonObject {
                        put("room", metaJson(room))
                        put("messages", JsonArray(messages.map { msgJson(room, it) }))
                        put("reset", reset)
                        put("last_rev", room.lastRev)
                    })
                }
                "crew_chat_history" -> {
                    val room = roomArg()?.takeIf { it.member(me) } ?: return refuse("chat_not_member", 403)
                    val before = long("p_before_seq") ?: Long.MAX_VALUE
                    val limit = long("p_limit")?.toInt() ?: 50
                    ok(JsonArray(room.messages.filter { it.seq < before }.sortedBy { it.seq }.takeLast(limit).map { msgJson(room, it) }))
                }
                "crew_chat_send" -> {
                    val room = roomArg()?.takeIf { it.member(me) } ?: return refuse("chat_not_member", 403)
                    val client = text("p_client_id").orEmpty()
                    room.messages.firstOrNull { it.authorId == me && it.clientId == client }?.let { return ok(msgJson(room, it)) }
                    if (sendStatus == 400) return refuse("invalid:body")
                    if (sendStatus != 200 && !storeWhenLost) return refuse("unavailable", sendStatus)
                    val image = text("p_image")
                    val stored = synchronized(room) {
                        val rev = ++room.lastRev
                        val person = room.members.first { it.id == me }
                        Msg(
                            nextId++, rev, rev, me, person.name, if (image != null) "IMAGE" else "TEXT", text("p_body")?.takeIf { it.isNotBlank() },
                            replyTo = long("p_reply_to"), clientId = client, createdAt = now(), image = image,
                        ).also { room.messages += it; person.read = rev }
                    }
                    if (sendStatus != 200) return refuse("unavailable", sendStatus)
                    ok(msgJson(room, stored))
                }
                "crew_chat_confirm" -> {
                    val room = roomArg()?.takeIf { it.member(me) } ?: return refuse("chat_not_member", 403)
                    val ids = args["p_client_ids"]?.jsonArray?.map { it.jsonPrimitive.content }.orEmpty().toSet()
                    ok(JsonArray(room.messages.filter { it.authorId == me && it.clientId in ids }.map { msgJson(room, it) }))
                }
                "crew_chat_read" -> {
                    val room = roomArg()?.takeIf { it.member(me) } ?: return refuse("chat_not_member", 403)
                    val seq = long("p_seq") ?: 0L
                    val person = room.members.first { it.id == me }
                    person.read = maxOf(person.read, minOf(seq, room.lastRev))
                    ok(person.read)
                }
                "crew_chat_delete" -> {
                    val (room, m) = messageArg() ?: return refuse("message_missing")
                    if (!room.member(me)) return refuse("chat_not_member", 403)
                    if (m.authorId != me) return refuse("not_author", 403)
                    synchronized(room) {
                        m.state = "DELETED"; m.rev = ++room.lastRev
                        room.messages.filter { it.replyTo == m.id }.forEach { it.rev = ++room.lastRev }
                    }
                    ok(msgJson(room, m))
                }
                "crew_chat_hide" -> {
                    val (room, m) = messageArg() ?: return refuse("message_missing")
                    if (!room.member(me)) return refuse("chat_not_member", 403)
                    if (room.ownerId != me) return refuse("크루장만 할 수 있습니다", 403)
                    if (m.authorId == me) return refuse("own_message")
                    synchronized(room) {
                        m.state = "HIDDEN"; m.rev = ++room.lastRev
                        room.messages.filter { it.replyTo == m.id }.forEach { it.rev = ++room.lastRev }
                    }
                    ok(msgJson(room, m))
                }
                "crew_chat_image" -> {
                    val (room, m) = messageArg() ?: return refuse("message_missing")
                    if (!room.member(me)) return refuse("chat_not_member", 403)
                    if (m.state != "VISIBLE" || m.image == null) return ok("null")
                    ok(JsonPrimitive(m.image))
                }
                "crew_chat_search" -> {
                    val room = roomArg()?.takeIf { it.member(me) } ?: return refuse("chat_not_member", 403)
                    val query = text("p_query").orEmpty()
                    ok(JsonArray(room.messages.filter { it.state == "VISIBLE" && it.kind != "SYSTEM" && it.body?.contains(query) == true }
                        .sortedByDescending { it.seq }.take(50).map { msgJson(room, it) }))
                }
                "crew_chat_report" -> {
                    val (room, m) = messageArg() ?: return refuse("message_missing")
                    if (!room.member(me)) return refuse("chat_not_member", 403)
                    reports += m.id to text("p_reason").orEmpty()
                    HttpResponse(204, "")
                }
                "crew_chat_notify_set" -> {
                    val room = roomArg()?.takeIf { it.member(me) } ?: return refuse("chat_not_member", 403)
                    val on = args["p_on"]?.jsonPrimitive?.booleanOrNull ?: true
                    room.notify[me] = on
                    ok(on)
                }
                "crew_chat_notices" -> {
                    val room = roomArg()?.takeIf { it.member(me) } ?: return refuse("chat_not_member", 403)
                    ok(JsonArray(room.notices.sortedWith(compareByDescending<Notice> { it.pinned }.thenByDescending { it.id }).map { noticeJson(room, it) }))
                }
                "crew_chat_notice_save" -> {
                    val room = roomArg()?.takeIf { it.member(me) } ?: return refuse("chat_not_member", 403)
                    if (room.ownerId != me) return refuse("크루장만 할 수 있습니다", 403)
                    if (noticeStatus != 200) return refuse("unavailable", noticeStatus)
                    val pinned = args["p_pinned"]?.jsonPrimitive?.booleanOrNull ?: false
                    val id = long("p_notice")
                    val notice = synchronized(room) {
                        if (pinned) room.notices.forEach { it.pinned = false }
                        if (id == null) {
                            val author = room.members.first { it.id == me }
                            Notice(nextNotice++, text("p_title").orEmpty(), text("p_body").orEmpty(), pinned, me, author.name, now()).also {
                                room.notices += it
                                val rev = ++room.lastRev
                                room.messages += Msg(nextId++, rev, rev, null, null, "SYSTEM", null, event = "NOTICE_CREATED", eventName = author.name, createdAt = now())
                            }
                        } else {
                            val existing = room.notices.firstOrNull { it.id == id } ?: return refuse("notice_missing")
                            existing.title = text("p_title").orEmpty(); existing.body = text("p_body").orEmpty(); existing.pinned = pinned
                            room.lastRev++
                            existing
                        }
                    }
                    ok(noticeJson(room, notice))
                }
                "crew_chat_notice_delete" -> {
                    val id = long("p_notice")
                    val room = rooms.values.firstOrNull { r -> r.notices.any { it.id == id } } ?: return refuse("notice_missing")
                    if (room.ownerId != me) return refuse("크루장만 할 수 있습니다", 403)
                    room.notices.removeAll { it.id == id }
                    room.lastRev++
                    HttpResponse(204, "")
                }
                "crew_roster" -> {
                    val room = roomArg() ?: return refuse("crew_missing")
                    val ordered = room.members.sortedWith(compareByDescending<Person> { it.id == room.ownerId })
                    ok(JsonArray(ordered.map { p ->
                        buildJsonObject {
                            put("user_id", p.id); put("name", p.name); put("role", if (p.id == room.ownerId) "OWNER" else "MEMBER")
                            put("joined_at", p.joinedAt); put("week_km", p.weekKm); put("week_runs", if (p.weekKm > 0) 1 else 0)
                        }
                    }))
                }
                "crew_person" -> {
                    val room = roomArg() ?: return refuse("crew_missing")
                    val user = text("p_user").orEmpty()
                    val person = room.members.firstOrNull { it.id == user }
                    ok(buildJsonObject {
                        put("user_id", user); put("name", person?.name ?: "민수")
                        put("role", when { person == null -> "NONE"; user == room.ownerId -> "OWNER"; else -> "MEMBER" })
                        put("joined_at", person?.joinedAt); put("week_km", person?.weekKm ?: 0.0)
                    })
                }
                "crew_member_remove" -> {
                    val room = roomArg() ?: return refuse("crew_missing")
                    if (room.ownerId != me) return refuse("크루장만 할 수 있습니다", 403)
                    val user = text("p_user").orEmpty()
                    if (user == me || !room.member(user)) return refuse("target_not_member")
                    leave(room.id, user)
                    HttpResponse(204, "")
                }
                "crew_leave" -> {
                    val room = roomArg() ?: return refuse("crew_missing")
                    leave(room.id, me)
                    HttpResponse(204, "")
                }
                else -> HttpResponse(404, """{"message":"unknown $name"}""")
            }
        }
    }

    companion object {
        private const val DOYUN = "u-doyun"
        private const val JUNHO = "u-junho"
        private const val MINSU = "u-minsu"
        private const val JIYEON = "u-jiyeon"
    }
}
