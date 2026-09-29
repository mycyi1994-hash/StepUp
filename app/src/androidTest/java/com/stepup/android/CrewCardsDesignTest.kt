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
import com.stepup.android.data.remote.HttpPoster
import com.stepup.android.data.remote.HttpResponse
import com.stepup.android.data.remote.PartyApi
import com.stepup.android.data.remote.PlaceSearchApi
import com.stepup.android.data.remote.SessionHolder
import com.stepup.android.data.remote.StepUpServer
import com.stepup.android.data.remote.SupabaseAuth
import com.stepup.android.data.repo.CrewCardRepository
import com.stepup.android.data.repo.CrewImageStore
import com.stepup.android.data.repo.CrewRepository
import com.stepup.android.data.repo.PlaceSearch
import com.stepup.android.domain.CrewArea
import com.stepup.android.ui.MainScaffold
import com.stepup.android.ui.Screen
import com.stepup.android.ui.experience.ExperienceProvider
import com.stepup.android.ui.screens.community.crew.CrewPhotoPickerForTest
import com.stepup.android.ui.theme.StepUpTheme
import com.stepup.android.ui.theme.ThemeMode
import java.io.File
import java.net.URLDecoder
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.ConcurrentLinkedQueue
import java.util.concurrent.CopyOnWriteArrayList
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonArray
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.booleanOrNull
import kotlinx.serialization.json.intOrNull
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
 * 크루 명함형(확정 2번, 2026-09-28) — 목록 · 상세 · 가입 신청 · 만들기 · 관리 화면을 실제 앱 화면으로 찍는다.
 *
 * 저장소 · 뷰모델 · 화면은 실제 코드다. 서버만 흉내 낸다([FakeCrewServer]) — crew_feed · crew_images 와 크루 함수들이
 * 서버와 같은 모양의 답을 준다. 크루 이름 · 숫자 · 사진은 시안 패키지(04-crew-images · crew-examples.json)의 예시를
 * 검사 자료로만 쓴다(앱 코드에는 없다). OS 사진 선택기는 다른 앱 화면이라 [CrewPhotoPickerForTest] 로 고른 사진을 넘긴다.
 *
 * 캡처: crew-cards/<시안 번호>-<장면>.png — 번호는 시안(01~90)과 같다.
 */
class CrewCardsDesignTest {
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

    private val directory get() = File(compose.activity.getExternalFilesDir(null), "crew-cards").apply { mkdirs() }
    private val notes = CopyOnWriteArrayList<String>()
    private lateinit var originalCards: CrewCardRepository
    private lateinit var originalPlaces: PlaceSearch
    private val server = FakeCrewServer()

    @After fun restore() {
        CrewPhotoPickerForTest.answer = null
        CrewPhotoPickerForTest.unavailable = false
        if (::originalCards.isInitialized) ServiceLocator.useCrewCardsForTest(originalCards)
        if (::originalPlaces.isInitialized) ServiceLocator.useCommunityForTest(ServiceLocator.communityRepository, originalPlaces)
        runBlocking { ServiceLocator.userPrefs.clearCrewData() }
        runCatching { File(directory, "capture-notes-${System.currentTimeMillis()}.txt").writeText(notes.joinToString("\n")) }
    }

    // ─────────────────────────────────────────────────────────────
    // 둘러보기 · 가입하기(01–24 · 64–68 · 70 · 73 · 82 · 87)
    // ─────────────────────────────────────────────────────────────

    @Test fun listDetailAndJoin() {
        server.seedBrowse()
        launch()
        guard {
            awaitTag("crew-card-afterwork")
            awaitTag("crew-card-sunrise")
            shot("01-list", settle = 1_500)

            tapTag("crew-region")
            awaitTag("crew-range-sheet")
            shot("02-region")
            tapTag("crew-sheet-close")

            tapTag("crew-sort")
            awaitTag("crew-sort-sheet")
            shot("04-sort")
            tapTag("crew-sheet-close")

            // 카드 안의 레벨 — 카드 전체(상세)로 가지 않고 레벨 안내만
            tapIn("crew-card-afterwork", "crew-card-level")
            awaitTag("crew-level-sheet")
            awaitText("이번 주에는 9명이 126km를 달렸어요.")
            shot("11-level")
            tapTag("crew-sheet-close")
            assertTrue(compose.onAllNodesWithTag("crew-detail").fetchSemanticsNodes().isEmpty())

            // 08 상세(방문자)
            tapIn("crew-card-afterwork", "crew-card-open")
            awaitTag("crew-detail")
            awaitTag("crew-detail-primary")
            shot("08-detail", settle = 1_200)
            scrollShot("08-detail-bottom")

            tapTag("crew-detail-image")
            awaitTag("crew-image-view")
            shot("09-image", settle = 1_000)
            pressBack()

            tapTag("crew-detail-leader")
            awaitTag("crew-leader")
            shot("10-leader")
            pressBack()

            tapTag("crew-detail-members-row")
            awaitTag("crew-members")
            awaitTag("crew-roster-u-minsu")
            shot("12-members")
            pressBack()

            tapTag("crew-detail-goal")
            awaitTag("crew-goal")
            awaitTag("crew-goal-percent")
            assertTextOf("crew-goal-percent", "79%")
            shot("13-goal")
            tapTag("crew-goal-participants")
            awaitTag("crew-participants")
            shot("82-weekly-participants")
            pressBack()
            pressBack()

            tapTag("crew-more")
            awaitTag("crew-visitor-menu")
            shot("73-visitor-menu")
            tapTag("crew-menu-report")
            awaitTag("crew-report-sheet")
            shot("64-report")
            tapTag("crew-sheet-close")

            // 14 가입 신청 → 17 전송 실패(입력은 그대로) → 15 신청 완료
            tapTag("crew-detail-primary")
            awaitTag("crew-join")
            tapTag("crew-join-phrase-AFTERWORK")
            typeInto("crew-join-message", "퇴근 후 함께 뛰고 싶어요.\n천천히, 꾸준히 같이 달려요.")
            closeKeyboard()
            shot("14-join")
            server.applyAnswers.add(HttpResponse(503, """{"message":"unavailable"}"""))
            tapTag("crew-join-send")
            awaitTag("crew-join-error")
            shot("17-join-error")
            tapTag("crew-join-send")
            awaitTag("crew-pending")
            awaitTag("crew-sent-message")
            shot("15-pending")
            assertEquals(2, server.calls.count { it.first == "crew_apply" })
            // 같은 신청서 — 다시 보내도 같은 요청 키
            val keys = server.calls.filter { it.first == "crew_apply" }.map { Json.parseToJsonElement(it.second).jsonObject["p_client_key"].toString() }
            assertEquals(1, keys.distinct().size)

            tapTag("crew-pending-cancel")
            awaitTag("crew-cancel")
            shot("16-cancel-request")
            tapTag("crew-sheet-close")
            tapTag("crew-pending-back")
            awaitTag("crew-detail")
            awaitText("크루장이 신청을 확인하고 있어요.")
            shot("20-detail-pending")

            // 크루장이 승인했다 — 푸시 없이 앱 안에서 다시 읽은 결과(18 → 87)
            server.decide("afterwork", approve = true)
            pressBack()
            awaitTag("crew-list")
            runBlocking { ServiceLocator.crewCards.refresh() }
            awaitTag("crew-result-banner-afterwork")
            shot("18-result-banner")
            tapTag("crew-result-banner-afterwork")
            awaitTag("crew-result-approved")
            shot("18-approved")
            tapTag("crew-result-button")
            awaitTag("crew-detail")
            awaitText("함께 달리는 크루예요.")
            shot("87-new-member-detail")
            tapTag("crew-more")
            awaitTag("crew-member-menu")
            shot("60-member-menu")
            tapTag("crew-menu-leave")
            awaitTag("crew-leave")
            shot("61-leave-crew")
            tapTag("crew-sheet-close")
            pressBack()

            // 22 모집 쉼 · 23 정원 마감 · 70 다른 크루(해뜨런)
            server.crew("sunrise") { recruiting = false }
            openCrew("sunrise")
            awaitText("지금은 모집을 쉬고 있어요")
            shot("22-detail-closed")
            pressBack()
            server.crew("sunrise") { recruiting = true; members = 24 }
            openCrew("sunrise")
            awaitText("정원이 가득 찼어요")
            shot("23-detail-full")
            pressBack()
            server.crew("sunrise") { members = 18 }
            openCrew("sunrise")
            awaitText("크루장이 확인한 뒤 가입할 수 있어요.")
            shot("70-sunrise-detail")

            // 24 신청하는 사이 정원이 찼다 — 한마디는 남아 있다
            tapTag("crew-detail-primary")
            awaitTag("crew-join")
            tapTag("crew-join-phrase-STEADY")
            server.applyAnswers.add(HttpResponse(400, """{"message":"crew_full"}"""))
            tapTag("crew-join-send")
            awaitTag("crew-unavailable")
            shot("24-join-unavailable")
            tapTag("crew-sheet-close")
            pressBack()
            pressBack()

            // 68 레벨을 확인할 수 없음
            server.levelFails = true
            tapIn("crew-card-sunrise", "crew-card-level")
            awaitTag("crew-level-error")
            shot("68-level-unavailable")
            tapTag("crew-sheet-close")
            server.levelFails = false

            // 67 불러오는 중 · 66 볼 수 없는 크루 — 목록에 없는 크루를 초대 링크로 연다
            server.detailGate = kotlinx.coroutines.CompletableDeferred()
            val goneCrew = "00000000-0000-4000-8000-000000000066"
            server.gone += goneCrew
            com.stepup.android.core.InviteLinks.handle(
                android.content.Intent(android.content.Intent.ACTION_VIEW, Uri.parse(com.stepup.android.core.InviteLinks.crewLink(goneCrew))),
            )
            awaitTag("crew-detail-loading")
            shot("67-detail-loading")
            server.detailGate?.complete(Unit)
            awaitTag("crew-missing")
            shot("66-missing")
            tapTag("crew-missing-list")
            awaitTag("crew-list")
        }
    }

    @Test fun listStates() {
        server.seedBrowse()
        server.feedGate = kotlinx.coroutines.CompletableDeferred()
        launch(waitForCards = false)
        guard {
            awaitTag("crew-list-loading")
            shot("06-list-loading")
            server.feedStatus = 503
            server.feedGate?.complete(Unit)
            awaitTag("crew-list-error")
            shot("07-list-error")
            // 다시 불러와도 고른 지역과 정렬은 그대로
            server.feedStatus = 200
            server.crews.clear()
            tapTag("crew-list-retry")
            awaitTag("crew-list-empty")
            shot("05-list-empty")
            tapTag("crew-list-widen")
            awaitTag("crew-range-sheet")
            tapTag("crew-range-area")
            awaitTag("crew-region-search")
            typeInto("crew-region-input", "없는동네")
            awaitTag("crew-region-empty")
            closeKeyboard()
            shot("72-region-empty")
            // 위치 권한이 없을 때만 71(묻기)이 뜬다 — CI 는 이 묶음 전에 권한을 거둔다
            if (!com.stepup.android.ui.StepPermissions.hasLocation(compose.activity)) {
                tapTag("crew-region-here")
                awaitTag("crew-location-sheet")
                shot("71-location-permission")
                tapTag("crew-location-search")
            }
            typeInto("crew-region-input", "공덕")
            awaitTag("crew-region-result")
            closeKeyboard()
            shot("03-region-search")
        }
    }

    // ─────────────────────────────────────────────────────────────
    // 만들기(25–39 · 69 · 77 · 78 · 88)
    // ─────────────────────────────────────────────────────────────

    @Test fun createCrew() {
        server.seedBrowse()
        launch()
        guard {
            // 운영 중인 크루가 없으면 "크루 모집하기"는 바로 만들기 1단계
            tapTag("crew-recruit")
            awaitTag("crew-draft-create-identity")
            typeInto("crew-draft-name", "공덕 한바퀴")
            typeInto("crew-draft-tagline", "퇴근길에 함께 한 바퀴")
            typeInto("crew-draft-note", "처음이어도 편하게 함께해요.\n천천히, 꾸준히 같이 달려요.")
            closeKeyboard()
            scrollToTop()
            shot("26-create-identity")

            // 38 나가기 → 저장 후 나가기 → 39 이어 쓰기 · 78 초안 지우기
            pressBack()
            awaitTag("crew-leave-draft")
            shot("38-leave-draft")
            tapTag("crew-leave-draft-yes")
            awaitTag("crew-list")
            tapTag("crew-recruit")
            awaitTag("crew-resume-sheet")
            shot("39-resume-draft")
            tapTag("crew-resume-new")
            awaitTag("crew-discard")
            shot("78-discard-draft")
            tapTag("crew-sheet-close")
            tapTag("crew-recruit")
            awaitTag("crew-resume-sheet")
            tapTag("crew-resume-continue")
            awaitTag("crew-draft-create-identity")
            awaitTextField("crew-draft-name", "공덕 한바퀴")

            // 27 대표 이미지 방법 · 69 사진 접근 불가 · 29 이미지 처리 실패 · 28 맞추기 · 30 기본 이미지
            tapTag("crew-draft-image")
            awaitTag("crew-image-sheet")
            shot("27-image-sheet")
            CrewPhotoPickerForTest.unavailable = true
            tapTag("crew-image-photos")
            awaitTag("crew-photo-denied")
            shot("69-photo-permission")
            tapTag("crew-sheet-close")
            CrewPhotoPickerForTest.unavailable = false
            CrewPhotoPickerForTest.answer = Uri.fromFile(File(compose.activity.cacheDir, "not-an-image.jpg").apply { writeText("not an image") })
            tapTag("crew-draft-image")
            tapTag("crew-image-photos")
            awaitTag("crew-photo-broken")
            shot("29-image-error")
            tapTag("crew-sheet-close")
            CrewPhotoPickerForTest.answer = Uri.fromFile(asset("crew/afterwork.png", "crew-pick.png"))
            tapTag("crew-draft-image")
            tapTag("crew-image-photos")
            awaitTag("crew-crop-frame")
            awaitTag("crew-crop-use")
            shot("28-image-crop", settle = 1_000)
            // 맞추기 없이 뒤로 — 이미지는 그대로
            pressBack()
            awaitTag("crew-draft-create-identity")
            tapTag("crew-draft-image")
            tapTag("crew-image-named")
            awaitTag("crew-named-images")
            tapTag("crew-named-1")
            shot("30-basic-images")
            tapTag("crew-named-use")
            awaitTag("crew-draft-create-identity")
            CrewPhotoPickerForTest.answer = null

            // 2단계 — 지역 · 일정(32) · 거리 · 분위기
            tapTag("crew-draft-primary")
            awaitTag("crew-draft-create-running")
            tapTag("crew-draft-area")
            awaitTag("crew-region-search")
            typeInto("crew-region-input", "공덕")
            awaitTag("crew-region-result")
            closeKeyboard()
            compose.onAllNodesWithTag("crew-region-result").onFirst().performClick()
            awaitTag("crew-draft-create-running")
            awaitText("공덕동")
            tapTag("crew-draft-schedule")
            awaitTag("crew-schedule-sheet")
            tapTag("crew-schedule-day-1")
            tapTag("crew-schedule-day-3")
            shot("32-schedule")
            tapTag("crew-schedule-apply")
            tapTag("crew-draft-distance-D3_5")
            tapTag("crew-draft-mood-EASY")
            tapTag("crew-draft-mood-BEGINNER")
            shot("31-create-running")

            // 3단계 — 정원 · 목표(88 직접 입력) · 모집 상태
            tapTag("crew-draft-primary")
            awaitTag("crew-draft-create-recruit")
            typeInto("crew-draft-capacity", "20")
            closeKeyboard()
            tapTag("crew-draft-goal-100")
            shot("33-create-recruit")
            tapTag("crew-draft-goal-custom")
            awaitTag("crew-custom-goal")
            typeInto("crew-custom-goal-input", "120")
            closeKeyboard()
            shot("88-custom-goal")
            tapTag("crew-sheet-close")
            tapTag("crew-draft-goal-100")

            // 34 미리보기 → 37 저장 실패(입력 보존) → 35 만들었어요 → 77 새 크루 관리
            tapTag("crew-draft-primary")
            awaitTag("crew-draft-create-preview")
            awaitTag("crew-preview-card")
            shot("34-create-preview")
            server.createAnswers.add(HttpResponse(503, """{"message":"unavailable"}"""))
            tapTag("crew-draft-primary")
            awaitTag("crew-footer-caption")
            shot("37-save-error")
            tapTag("crew-draft-primary")
            awaitTag("crew-created")
            shot("35-created")
            val keys = server.calls.filter { it.first == "crew_create_card" }
                .map { Json.parseToJsonElement(it.second).jsonObject["p_client_key"].toString() }
            assertEquals(2, keys.size)
            assertEquals(1, keys.distinct().size)
            tapTag("crew-result-button")
            awaitTag("crew-manage-created")
            shot("77-created-manage")
            tapTag("crew-manage-list")
            awaitTag("crew-list")
            awaitTag("crew-card-new-crew")

            // 25 모집할 크루 선택(운영 중인 크루가 생겼다)
            tapTag("crew-recruit")
            awaitTag("crew-recruit-entry")
            shot("25-recruit-entry")
        }
    }

    // ─────────────────────────────────────────────────────────────
    // 크루장 관리(40–63 · 74–76 · 79–81 · 83–86 · 89 · 90)
    // ─────────────────────────────────────────────────────────────

    @Test fun manageCrew() {
        server.seedOwner()
        launch()
        guard {
            // 가입한 크루(크루장 포함)는 목록에서 내 크루 홈(확정 4번, #62)으로 열린다 — 크루 관리는 홈 더보기 안
            awaitTag("crew-list")
            tapIn("crew-card-afterwork", "crew-card-open")
            awaitTag("home-masthead")
            shot("76-owner-home")
            tapTag("crew-more")
            tapTag("home-menu-manage")
            awaitTag("crew-manage")
            shot("40-manage")

            // 48 신청 목록 → 49 신청서 → 50 승인 확인 → 51 결과 → 79 남은 신청
            tapTag("crew-manage-requests")
            awaitTag("crew-requests")
            awaitTag("crew-request-701")
            shot("48-requests")
            tapTag("crew-request-701")
            awaitTag("crew-review")
            awaitTag("crew-review-note")
            shot("49-request-review")
            tapTag("crew-review-profile")
            awaitTag("crew-person")
            awaitTag("crew-applicant-note")
            shot("85-applicant-profile")
            pressBack()
            tapTag("crew-review-approve")
            awaitTag("crew-approve")
            shot("50-approve-confirm")
            tapTag("crew-approve-yes")
            awaitTag("crew-decided-approved")
            shot("51-approved-result")
            tapTag("crew-result-button")
            awaitTag("crew-requests")
            awaitTag("crew-request-702")
            shot("79-requests-updated")

            // 80 처리 실패 · 81 정원 초과 · 52 미승인 확인 · 53 결과 · 54 빈 목록
            tapTag("crew-request-702")
            awaitTag("crew-review")
            server.decideAnswers.add(HttpResponse(503, """{"message":"unavailable"}"""))
            tapTag("crew-review-approve")
            tapTag("crew-approve-yes")
            awaitTag("crew-action-error")
            shot("80-action-error")
            tapTag("crew-sheet-close")
            server.decideAnswers.add(HttpResponse(400, """{"message":"crew_full"}"""))
            tapTag("crew-review-approve")
            tapTag("crew-approve-yes")
            awaitTag("crew-approve-full")
            shot("81-approve-full")
            tapTag("crew-sheet-close")
            tapTag("crew-review-decline")
            awaitTag("crew-decline")
            shot("52-decline-confirm")
            tapTag("crew-decline-yes")
            awaitTag("crew-decided-declined")
            shot("53-declined-result")
            tapTag("crew-result-button")
            awaitTag("crew-requests-empty")
            shot("54-requests-empty")
            pressBack()

            // 55 멤버 관리 → 84 멤버 프로필(크루장이 볼 때) → 75 관리 메뉴 → 57 내보내기
            tapTag("crew-manage-members")
            awaitTag("crew-members-manage")
            awaitTag("crew-roster-u-minsu")
            shot("55-members-manage")
            tapTag("crew-roster-u-minsu")
            awaitTag("crew-person")
            shot("84-member-owner-profile")
            tapTag("crew-more")
            awaitTag("crew-member-actions")
            shot("75-member-actions")
            tapTag("crew-member-remove")
            awaitTag("crew-remove")
            shot("57-remove-member")
            tapTag("crew-sheet-close")
            pressBack()
            pressBack()

            // 46 주간 목표 수정 → 47 확인 → 83 바뀐 달성률(126/200 = 63%, 74km 남음)
            tapTag("crew-manage-goal")
            awaitTag("crew-goal-edit")
            tapTag("crew-goal-chip-200")
            closeKeyboard()
            shot("46-goal-edit")
            tapTag("crew-goal-change")
            awaitTag("crew-goal-confirm")
            shot("47-goal-confirm")
            tapTag("crew-goal-confirm-yes")
            awaitTag("crew-goal")
            assertTextOf("crew-goal-percent", "63%")
            awaitText("목표까지 74km 남았어요.")
            shot("83-goal-updated")
            pressBack()

            // 41 소개 수정 · 42 모임 정보 수정 · 43 모집 설정
            tapTag("crew-manage-profile")
            awaitTag("crew-draft-edit_profile-identity")
            shot("41-edit-profile", settle = 1_000)
            pressBack()
            tapTag("crew-manage-running")
            awaitTag("crew-draft-edit_running-running")
            shot("42-edit-running")
            pressBack()
            tapTag("crew-manage-recruit")
            awaitTag("crew-draft-edit_recruit-recruit")
            typeInto("crew-draft-capacity", "20")
            closeKeyboard()
            awaitTag("crew-draft-capacity-help")
            shot("43-recruit-settings")
            pressBack()
            awaitTag("crew-leave-draft")
            tapTag("crew-sheet-close")
            typeInto("crew-draft-capacity", "30")
            closeKeyboard()
            pressBack()
            if (compose.onAllNodesWithTag("crew-leave-draft").fetchSemanticsNodes().isNotEmpty()) tapTag("crew-sheet-close")
            // 바꾼 것이 원래 값과 같으면 묻지 않고 나간다
            awaitTag("crew-manage")

            // 74 운영 설정 → 44 모집 멈춤 → 89 · 90 → 45 다시 시작 · 62 크루장 탈퇴 · 63 해산
            tapTag("crew-manage-settings")
            awaitTag("crew-settings")
            shot("74-owner-settings")
            tapTag("crew-settings-pause")
            awaitTag("crew-pause")
            shot("44-close-recruit")
            tapTag("crew-pause-yes")
            awaitTag("crew-detail")
            awaitText("모집을 쉬고 있어요. 기존 멤버는 그대로예요.")
            shot("89-owner-paused")
            tapTag("crew-more")
            awaitTag("crew-settings")
            awaitTag("crew-settings-resume")
            shot("90-owner-paused-settings")
            tapTag("crew-settings-resume")
            awaitTag("crew-resume-recruit")
            shot("45-open-recruit")
            tapTag("crew-sheet-close")
            tapTag("crew-settings-leave")
            awaitTag("crew-owner-exit")
            shot("62-owner-exit")
            tapTag("crew-sheet-close")
            tapTag("crew-settings-dissolve")
            awaitTag("crew-dissolve")
            shot("63-dissolve")
            tapTag("crew-sheet-close")

            // 58 새 크루장 선택 → 59 확인 → 86 넘긴 뒤(멤버로 보는 상세)
            tapTag("crew-settings-transfer")
            awaitTag("crew-transfer")
            awaitTag("crew-transfer-u-minsu")
            shot("58-transfer-select")
            tapTag("crew-transfer-u-minsu")
            awaitTag("crew-transfer-confirm")
            shot("59-transfer-confirm")
            tapTag("crew-transfer-confirm-yes")
            awaitTag("crew-detail")
            awaitText("크루장 민수")
            shot("86-after-transfer")
        }
    }

    // ─────────────────────────────────────────────────────────────
    // 준비 · 도우미
    // ─────────────────────────────────────────────────────────────

    private fun launch(waitForCards: Boolean = true) {
        clearAnyRunCheckpointForTest()
        runBlocking {
            ServiceLocator.userPrefs.setReducedMotion(true)
            ServiceLocator.userPrefs.setSounds(false)
            ServiceLocator.userPrefs.setHaptics(false)
            ServiceLocator.userPrefs.setGuideSeen()
            ServiceLocator.userPrefs.clearCrewData()
        }
        val context = compose.activity
        val stepUp = StepUpServer(
            baseUrl = "https://crew.test",
            apiKey = "sb_publishable_test",
            sessions = SessionHolder(auth = SupabaseAuth("https://crew.test", "sb_publishable_test", server), store = SignedIn),
            http = server,
        )
        val api = CrewApi(stepUp)
        val db = ServiceLocator.database
        val crews = CrewRepository(api, db.crewDao(), db.crewInfoDao(), db.walkSessionDao(), ServiceLocator.rewardRepository, PartyApi(stepUp))
        val repository = CrewCardRepository(
            api = api,
            crews = crews,
            images = CrewImageStore(fetch = api::image, dir = File(context.cacheDir, "crew-test-images").apply { deleteRecursively() }),
            prefs = ServiceLocator.userPrefs,
            owner = { OWNER },
            ownerFlow = flowOf(OWNER),
            draftDir = File(context.filesDir, "crew_drafts_test").apply { deleteRecursively() },
        )
        originalCards = ServiceLocator.crewCards
        originalPlaces = ServiceLocator.placeSearch
        ServiceLocator.useCrewCardsForTest(repository)
        ServiceLocator.useCommunityForTest(
            ServiceLocator.communityRepository,
            PlaceSearch(PlaceSearchApi(key = "test", fetch = ::geocoding), platform = null, language = { "ko" }),
        )
        server.images["afterwork"] = asset64("crew/afterwork.png")
        server.images["sunrise"] = asset64("crew/sunrise.png")
        // 목록 기준 — 도화동(반경 3km)
        runBlocking {
            repository.setRegion(CrewArea("도화동", "서울 마포구", 37.5395, 126.9500))
            repository.setRadius(3)
        }
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
        if (waitForCards) awaitTag("crew-card-afterwork")
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

    private fun openCrew(id: String) {
        awaitTag("crew-list")
        tapIn("crew-card-$id", "crew-card-open")
        awaitTag("crew-detail")
        awaitTag("crew-detail-primary")
    }

    /** 카드 안의 누르는 곳 — 목록에서 그 카드까지 굴린 뒤 누른다 */
    private fun tapIn(card: String, inner: String) {
        awaitTag("crew-list-scroll")
        runCatching { compose.onNodeWithTag("crew-list-scroll").performScrollToNode(hasTestTag(card)) }
        awaitTag(card)
        compose.onNode(hasTestTag(inner) and hasAnyAncestor(hasTestTag(card)), useUnmergedTree = true).performClick()
        compose.waitForIdle()
    }

    private fun awaitTag(tag: String, timeout: Long = 20_000) {
        val end = android.os.SystemClock.uptimeMillis() + timeout
        while (compose.onAllNodesWithTag(tag, useUnmergedTree = true).fetchSemanticsNodes().isEmpty()) {
            if (android.os.SystemClock.uptimeMillis() > end) {
                val tags = compose.onAllNodes(SemanticsMatcher("crew tag") { node ->
                    node.config.getOrNull(SemanticsProperties.TestTag)?.startsWith("crew-") == true
                }, useUnmergedTree = true).fetchSemanticsNodes().mapNotNull { it.config.getOrNull(SemanticsProperties.TestTag) }
                throw AssertionError("'$tag' did not appear in ${timeout}ms (crew tags now: ${tags.take(30)})")
            }
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

    private fun assertTextOf(tag: String, expected: String) {
        compose.onNodeWithTag(tag, useUnmergedTree = true).assertTextEquals(expected)
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

    private fun scrollToTop() {
        compose.onAllNodes(hasScrollAction()).fetchSemanticsNodes().forEachIndexed { index, _ ->
            runCatching { compose.onAllNodes(hasScrollAction())[index].performTouchInput { swipeDown() } }
        }
        compose.waitForIdle()
    }

    /** 상세 아래쪽까지 한 번 굴려 찍고 위로 돌린다 */
    private fun scrollShot(name: String) {
        val scroll = compose.onAllNodes(hasScrollAction()).onFirst()
        scroll.performTouchInput { swipeUp() }
        shot(name)
        scroll.performTouchInput { swipeDown() }
        scroll.performTouchInput { swipeDown() }
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

    private fun shot(name: String, settle: Long = 700) {
        compose.waitForIdle()
        InstrumentationRegistry.getInstrumentation().waitForIdleSync()
        Thread.sleep(settle)
        awaitFrameOnScreen(compose.activity)
        android.util.Log.i("CrewCards", "Capturing $name")
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

    /** MapTiler Geocoding 모양의 고정 답 */
    private fun geocoding(url: String): String? {
        val query = URLDecoder.decode(url.substringAfter("/geocoding/").substringBefore(".json"), "UTF-8")
        return when (query) {
            "공덕" -> GONGDEOK
            else -> """{"type":"FeatureCollection","features":[]}"""
        }
    }

    private object SignedIn : AuthSessionStore {
        override suspend fun load() = AuthSession(
            accessToken = "token", refreshToken = "refresh", expiresIn = 3600, expiresAt = 9_999_999_999L,
            user = AuthUser(id = OWNER_ID, isAnonymous = false),
        )
        override suspend fun save(session: AuthSession) = Unit
        override suspend fun clear() = Unit
    }

    /** 서버와 같은 모양의 답 — crew_feed · crew_images · 크루 함수들 */
    private class FakeCrewServer : HttpPoster {
        class Crew(
            val id: String,
            var name: String,
            var tagline: String,
            var note: String = "",
            var leader: String,
            var leaderId: String,
            var bg: Int? = null,
            var hasImage: Boolean = false,
            var imageVer: Int = 0,
            var area: String,
            var lat: Double,
            var lng: Double,
            var days: Int = 0,
            var time: Int? = null,
            var distance: String? = null,
            var moods: List<String> = emptyList(),
            var members: Int,
            var capacity: Int?,
            var recruiting: Boolean = true,
            var goal: Int? = null,
            var level: Int? = null,
            var weekKm: Double = 0.0,
            var runners: Int = 0,
            var joined: Boolean = false,
            var requested: Boolean = false,
            var owned: Boolean = false,
            var pending: Int = 0,
            var policy: String = "APPROVAL",
            var appId: Long? = null,
            var appStatus: String? = null,
            var appSeen: Boolean = false,
            var changedAt: String = "2026-09-27T01:00:00Z",
        ) {
            fun json(): JsonObject = buildJsonObject {
                put("id", id); put("owner_id", leaderId); put("name", name); put("monogram", name.take(1)); put("tagline", tagline)
                put("area", area); put("lat", lat); put("lng", lng); put("join_policy", policy); put("created_at", "2026-09-01T01:00:00Z")
                put("member_count", members); put("roster", buildJsonArray { add(JsonPrimitive(leader)) })
                put("joined", joined); put("requested", requested); put("pending_count", pending); put("owned", owned)
                put("leader_name", leader); put("leader_note", note); put("image_bg", bg); put("image_ver", imageVer); put("has_image", hasImage)
                put("meet_days", days); put("meet_time", time); put("run_distance", distance)
                put("moods", JsonArray(moods.map(::JsonPrimitive))); put("capacity", capacity); put("recruiting", recruiting)
                put("recruit_changed_at", changedAt); put("weekly_goal_km", goal); put("level", level)
                put("week_km", weekKm); put("week_runners", runners)
                put("my_application_id", appId); put("my_application_status", appStatus); put("my_application_seen", appSeen)
            }
        }

        class Member(val id: String, val name: String, val owner: Boolean, val weekKm: Double, val runs: Int)

        class Application(val id: Long, val crewId: String, val userId: String, val name: String, var status: String, val phrases: List<String>, val message: String)

        private val json = Json { ignoreUnknownKeys = true }
        val crews = ConcurrentHashMap<String, Crew>()
        val gone = CopyOnWriteArrayList<String>()
        val images = ConcurrentHashMap<String, String>()
        val rosters = ConcurrentHashMap<String, MutableList<Member>>()
        val applications = ConcurrentHashMap<Long, Application>()
        val calls = CopyOnWriteArrayList<Pair<String, String>>()
        val applyAnswers = ConcurrentLinkedQueue<HttpResponse>()
        val createAnswers = ConcurrentLinkedQueue<HttpResponse>()
        val decideAnswers = ConcurrentLinkedQueue<HttpResponse>()
        @Volatile var feedStatus = 200
        @Volatile var feedGate: kotlinx.coroutines.CompletableDeferred<Unit>? = null
        @Volatile var detailGate: kotlinx.coroutines.CompletableDeferred<Unit>? = null
        @Volatile var levelFails = false
        private var nextApplication = 900L

        fun crew(id: String, change: Crew.() -> Unit) { crews.getValue(id).change() }

        /**
         * 둘러보기 — 퇴근런(사진 · Lv.7 · 24/30 · 126/160km) · 해뜨런(사진 · Lv.12) · 천천히 걸음(레벨 · 목표 없음 → 새 크루).
         * 도화동에서 가까운 순으로 0.6 · 2.0 · 2.4km — 시안 01 처럼 퇴근런 다음이 해뜨런이다(작은 화면에서도 둘째 카드가 보인다).
         */
        fun seedBrowse() {
            crews.clear(); rosters.clear(); applications.clear()
            crews["afterwork"] = Crew(
                id = "afterwork", name = "퇴근런", tagline = "퇴근 후 가볍게 한 바퀴",
                note = "퇴근 후에는 일 얘기 잠깐 내려두고,\n대화할 수 있는 속도로 함께 달려요.", leader = "준호", leaderId = "u-junho",
                hasImage = true, imageVer = 1, bg = 1, area = "공덕", lat = 37.5446, lng = 126.9515, days = 0b1010, time = 19 * 60 + 30,
                distance = "D3_5", moods = listOf("EASY", "BEGINNER"), members = 24, capacity = 30, goal = 160, level = 7,
                weekKm = 126.0, runners = 9, changedAt = "2026-09-27T09:00:00Z",
            )
            crews["sunrise"] = Crew(
                id = "sunrise", name = "해뜨런", tagline = "도시가 깨기 전에, 먼저", note = "한강의 아침을 같이 시작해요.\n가볍게 5km를 달리고 각자의 하루로.",
                leader = "서윤", leaderId = "u-seoyun", hasImage = true, imageVer = 1, bg = 3, area = "여의도", lat = 37.5270, lng = 126.9330,
                days = 0b10101, time = 6 * 60, distance = "D5P", moods = listOf("RECORD"), members = 18, capacity = 24, goal = 200, level = 12,
                weekKm = 162.0, runners = 11, changedAt = "2026-09-26T09:00:00Z",
            )
            crews["slowsteps"] = Crew(
                id = "slowsteps", name = "천천히 걸음", tagline = "걷다가 뛰다가", leader = "하린", leaderId = "u-harin", bg = 0,
                area = "마포", lat = 37.5530, lng = 126.9290, days = 0b1000000, time = 8 * 60, distance = "D1_3",
                moods = listOf("WALK_FIRST", "BEGINNER"), members = 3, capacity = 10, changedAt = "2026-09-25T09:00:00Z",
            )
            rosters["afterwork"] = mutableListOf(
                Member("u-junho", "준호", true, 24.0, 3), Member("u-minsu", "민수", false, 18.0, 2), Member("u-jiyeon", "지연", false, 16.0, 2),
                Member("u-hyunwoo", "현우", false, 15.0, 2), Member("u-seoyeon", "서연", false, 13.0, 1), Member("u-dohyun", "도현", false, 0.0, 0),
            )
            rosters["sunrise"] = mutableListOf(Member("u-seoyun", "서윤", true, 20.0, 3))
        }

        /** 크루장 — 퇴근런을 내가 운영한다. 기다리는 신청 둘 */
        fun seedOwner() {
            seedBrowse()
            crew("afterwork") { owned = true; joined = true; leader = "나"; leaderId = OWNER_ID; pending = 2 }
            rosters["afterwork"] = mutableListOf(
                Member(OWNER_ID, "나", true, 24.0, 3), Member("u-minsu", "민수", false, 18.0, 2), Member("u-jiyeon", "지연", false, 16.0, 2),
                Member("u-hyunwoo", "현우", false, 15.0, 2), Member("u-seoyeon", "서연", false, 13.0, 1),
            )
            applications[701] = Application(701, "afterwork", "u-doyun", "도윤", "PENDING", listOf("AFTERWORK"), "퇴근 후 함께 뛰고 싶어요.\n천천히, 꾸준히 같이 달려요.")
            applications[702] = Application(702, "afterwork", "u-jiwoo", "지우", "PENDING", listOf("BEGINNER"), "러닝이 처음인데 같이 시작하고 싶어요.")
        }

        /** 크루장이 내 신청을 정했다 */
        fun decide(crewId: String, approve: Boolean) {
            val crew = crews.getValue(crewId)
            val app = crew.appId?.let { applications[it] } ?: return
            app.status = if (approve) "APPROVED" else "DECLINED"
            crew.appStatus = app.status
            crew.appSeen = false
            crew.requested = false
            if (approve) { crew.joined = true; crew.members += 1 }
        }

        override suspend fun get(url: String, headers: Map<String, String>): HttpResponse {
            calls += "GET" to url
            return when {
                "/crew_feed" in url -> {
                    val id = Regex("[?&]id=eq\\.([^&]+)").find(url)?.groupValues?.get(1)?.let { URLDecoder.decode(it, "UTF-8") }
                    if (id != null) {
                        detailGate?.let { gate -> gate.await(); detailGate = null }
                        if (id in gone) return HttpResponse(200, "[]")
                        HttpResponse(200, JsonArray(listOfNotNull(crews[id]?.json())).toString())
                    } else {
                        feedGate?.let { gate -> gate.await(); feedGate = null }
                        if (feedStatus != 200) return HttpResponse(feedStatus, """{"message":"unavailable"}""")
                        HttpResponse(200, JsonArray(crews.values.filter { it.id !in gone }.map { it.json() }).toString())
                    }
                }
                "/crew_images" in url -> {
                    val id = Regex("crew_id=eq\\.([^&]+)").find(url)?.groupValues?.get(1)?.let { URLDecoder.decode(it, "UTF-8") }
                    val data = id?.let { images[it] }
                    HttpResponse(200, if (data == null) "[]" else """[{"data":"$data"}]""")
                }
                else -> HttpResponse(404, "")
            }
        }

        override suspend fun post(url: String, body: String, headers: Map<String, String>): HttpResponse {
            val name = url.substringAfterLast("/rpc/")
            calls += name to body
            val args = runCatching { json.parseToJsonElement(body).jsonObject }.getOrDefault(JsonObject(emptyMap()))
            fun text(key: String) = args[key]?.jsonPrimitive?.content.orEmpty()
            fun crewArg() = crews[text("p_crew")]
            return when (name) {
                "crew_level" -> {
                    if (levelFails) return HttpResponse(503, """{"message":"unavailable"}""")
                    val c = crewArg() ?: return HttpResponse(400, """{"message":"crew_missing"}""")
                    ok(buildJsonObject { put("level", c.level); put("week_km", c.weekKm); put("week_runners", c.runners); put("member_count", c.members) })
                }
                "crew_roster" -> ok(JsonArray(rosters[text("p_crew")].orEmpty().map { m ->
                    buildJsonObject {
                        put("user_id", m.id); put("name", m.name); put("role", if (m.owner) "OWNER" else "MEMBER")
                        put("joined_at", "2026-08-01T01:00:00Z"); put("week_km", m.weekKm); put("week_runs", m.runs)
                    }
                }))
                "crew_person" -> {
                    val user = text("p_user")
                    val member = rosters[text("p_crew")].orEmpty().firstOrNull { it.id == user }
                    val app = applications.values.firstOrNull { it.userId == user && it.status == "PENDING" }
                    ok(buildJsonObject {
                        put("user_id", user)
                        put("name", member?.name ?: app?.name.orEmpty())
                        put("role", when { member?.owner == true -> "OWNER"; member != null -> "MEMBER"; app != null -> "APPLICANT"; else -> "NONE" })
                        put("joined_at", if (member != null) "2026-08-01T01:00:00Z" else null)
                        put("week_km", member?.weekKm ?: 0.0)
                        if (app != null && member == null) {
                            put("application", buildJsonObject {
                                put("id", app.id); put("phrases", JsonArray(app.phrases.map(::JsonPrimitive))); put("message", app.message)
                                put("created_at", "2026-09-28T01:00:00Z")
                            })
                        }
                    })
                }
                "crew_apply" -> {
                    applyAnswers.poll()?.let { return it }
                    val c = crewArg() ?: return HttpResponse(400, """{"message":"crew_missing"}""")
                    val id = nextApplication++
                    applications[id] = Application(id, c.id, OWNER_ID, "나", "PENDING",
                        args["p_phrases"]?.jsonArray?.map { it.jsonPrimitive.content }.orEmpty(), text("p_message"))
                    c.requested = true; c.appId = id; c.appStatus = "PENDING"; c.appSeen = false
                    ok(buildJsonObject { put("result", "PENDING"); put("application_id", id); put("duplicate", false) })
                }
                "crew_application" -> {
                    val app = applications[args["p_application"]?.jsonPrimitive?.longOrNull ?: -1] ?: return HttpResponse(400, """{"message":"application_missing"}""")
                    ok(appJson(app))
                }
                "crew_application_cancel" -> {
                    val app = applications[args["p_application"]?.jsonPrimitive?.longOrNull ?: -1] ?: return HttpResponse(400, """{"message":"application_missing"}""")
                    app.status = "CANCELED"
                    crews[app.crewId]?.apply { requested = false; appStatus = "CANCELED" }
                    ok(buildJsonObject { put("status", "CANCELED") })
                }
                "crew_application_seen" -> {
                    val app = applications[args["p_application"]?.jsonPrimitive?.longOrNull ?: -1]
                    if (app != null) crews[app.crewId]?.appSeen = true
                    HttpResponse(204, "")
                }
                "crew_pending_applications" ->
                    ok(JsonArray(applications.values.filter { it.crewId == text("p_crew") && it.status == "PENDING" }.sortedBy { it.id }.map(::appJson)))
                "crew_application_decide" -> {
                    decideAnswers.poll()?.let { return it }
                    val app = applications[args["p_application"]?.jsonPrimitive?.longOrNull ?: -1] ?: return HttpResponse(400, """{"message":"application_missing"}""")
                    val approve = args["p_approve"]?.jsonPrimitive?.booleanOrNull == true
                    val c = crews.getValue(app.crewId)
                    app.status = if (approve) "APPROVED" else "DECLINED"
                    c.pending = (c.pending - 1).coerceAtLeast(0)
                    if (approve) {
                        c.members += 1
                        rosters.getOrPut(c.id) { mutableListOf() }.add(Member(app.userId, app.name, false, 0.0, 0))
                    }
                    ok(buildJsonObject { put("status", app.status); put("member_count", c.members); put("capacity", c.capacity); put("pending_count", c.pending) })
                }
                "crew_set_recruiting" -> {
                    crewArg()?.recruiting = args["p_open"]?.jsonPrimitive?.booleanOrNull == true
                    HttpResponse(204, "")
                }
                "crew_set_goal" -> {
                    val c = crewArg() ?: return HttpResponse(400, """{"message":"crew_missing"}""")
                    c.goal = args["p_goal_km"]?.jsonPrimitive?.intOrNull
                    ok(buildJsonObject { put("weekly_goal_km", c.goal); put("week_km", c.weekKm); put("week_runners", c.runners) })
                }
                "crew_update_recruit" -> {
                    val c = crewArg() ?: return HttpResponse(400, """{"message":"crew_missing"}""")
                    val capacity = args["p_capacity"]?.jsonPrimitive?.intOrNull ?: return HttpResponse(400, """{"message":"invalid:capacity"}""")
                    if (capacity < c.members) return HttpResponse(400, """{"message":"capacity_below_members"}""")
                    c.capacity = capacity
                    c.recruiting = args["p_recruiting"]?.jsonPrimitive?.booleanOrNull == true
                    if (args["p_goal_change"]?.jsonPrimitive?.booleanOrNull == true) c.goal = args["p_goal_km"]?.jsonPrimitive?.intOrNull
                    HttpResponse(204, "")
                }
                "crew_update_profile", "crew_update_running" -> {
                    val c = crewArg() ?: return HttpResponse(400, """{"message":"crew_missing"}""")
                    if (name == "crew_update_profile") {
                        c.name = text("p_name"); c.tagline = text("p_tagline"); c.note = text("p_leader_note")
                        ok(buildJsonObject { put("image_ver", c.imageVer); put("has_image", c.hasImage) })
                    } else {
                        HttpResponse(204, "")
                    }
                }
                "crew_create_card" -> {
                    createAnswers.poll()?.let { return it }
                    crews["new-crew"] = Crew(
                        id = "new-crew", name = text("p_name"), tagline = text("p_tagline"), note = text("p_leader_note"), leader = "나",
                        leaderId = OWNER_ID, bg = args["p_image_bg"]?.jsonPrimitive?.intOrNull, area = text("p_area"),
                        lat = args["p_lat"]?.jsonPrimitive?.content?.toDoubleOrNull() ?: 37.5446,
                        lng = args["p_lng"]?.jsonPrimitive?.content?.toDoubleOrNull() ?: 126.9515,
                        days = args["p_meet_days"]?.jsonPrimitive?.intOrNull ?: 0, time = args["p_meet_time"]?.jsonPrimitive?.intOrNull,
                        distance = args["p_distance"]?.jsonPrimitive?.content?.takeIf { it != "null" },
                        moods = args["p_moods"]?.jsonArray?.map { it.jsonPrimitive.content }.orEmpty(),
                        members = 1, capacity = args["p_capacity"]?.jsonPrimitive?.intOrNull, goal = args["p_goal_km"]?.jsonPrimitive?.intOrNull,
                        owned = true, joined = true, changedAt = "2026-09-28T09:00:00Z",
                    )
                    rosters["new-crew"] = mutableListOf(Member(OWNER_ID, "나", true, 0.0, 0))
                    HttpResponse(200, "\"new-crew\"")
                }
                "crew_transfer_owner" -> {
                    val c = crewArg() ?: return HttpResponse(400, """{"message":"crew_missing"}""")
                    val next = rosters[c.id].orEmpty().firstOrNull { it.id == text("p_user") } ?: return HttpResponse(400, """{"message":"target_not_member"}""")
                    c.owned = false; c.leader = next.name; c.leaderId = next.id; c.pending = 0
                    rosters[c.id] = rosters.getValue(c.id).map { Member(it.id, it.name, it.id == next.id, it.weekKm, it.runs) }.toMutableList()
                    HttpResponse(204, "")
                }
                "crew_member_remove" -> {
                    val c = crewArg() ?: return HttpResponse(400, """{"message":"crew_missing"}""")
                    rosters[c.id]?.removeAll { it.id == text("p_user") }
                    c.members -= 1
                    HttpResponse(204, "")
                }
                // 내 크루 홈(0049) — 가입한 크루만. 모임 · 공지 없이 이 검사에 필요한 만큼
                "crew_home" -> {
                    val c = crewArg() ?: return HttpResponse(400, """{"message":"crew_missing"}""")
                    if (!c.joined || c.id in gone) return HttpResponse(403, """{"message":"crew_not_member"}""")
                    ok(buildJsonObject {
                        put("crew_id", c.id); put("role", if (c.owned) "OWNER" else "MEMBER"); put("owner_id", c.leaderId)
                        put("member_count", c.members); put("unread", 0)
                    })
                }
                "crew_leave" -> { crewArg()?.apply { joined = false; members -= 1 }; HttpResponse(204, "") }
                "crew_dissolve" -> { gone += text("p_crew"); HttpResponse(204, "") }
                "content_report" -> HttpResponse(204, "")
                else -> HttpResponse(404, "")
            }
        }

        private fun appJson(app: Application) = buildJsonObject {
            put("id", app.id); put("crew_id", app.crewId); put("user_id", app.userId); put("name", app.name); put("status", app.status)
            put("phrases", JsonArray(app.phrases.map(::JsonPrimitive))); put("message", app.message)
            put("created_at", "2026-09-28T01:00:00Z"); put("decided_at", null); put("seen", false)
        }

        private fun ok(element: kotlinx.serialization.json.JsonElement) = HttpResponse(200, element.toString())
    }

    companion object {
        private const val OWNER = "account:crew-cards-test"
        private const val OWNER_ID = "u-me"

        private const val GONGDEOK = """{"type":"FeatureCollection","features":[
            {"id":"place.210","text":"공덕동","place_type":["place"],"center":[126.9515,37.5446],
             "context":[{"id":"county.3250","text":"마포구"},{"id":"region.1099","text":"서울특별시"}]},
            {"id":"poi.211","text":"공덕역 주변","place_type":["poi"],"center":[126.9519,37.5440],
             "context":[{"id":"county.3250","text":"마포구"},{"id":"region.1099","text":"서울특별시"}]}]}"""
    }
}
