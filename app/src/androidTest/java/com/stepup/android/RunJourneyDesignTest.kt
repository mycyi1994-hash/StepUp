package com.stepup.android

import android.Manifest
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.requiredSize
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.rule.GrantPermissionRule
import com.stepup.android.core.ServiceLocator
import com.stepup.android.data.local.WalkSessionEntity
import com.stepup.android.data.repo.EconomySyncState
import com.stepup.android.domain.GeoPoint
import com.stepup.android.domain.GoalAttempt
import com.stepup.android.domain.RunExperience
import com.stepup.android.domain.RunGoal
import com.stepup.android.domain.RunPlan
import com.stepup.android.domain.RunPlans
import com.stepup.android.domain.ShoeCatalog
import com.stepup.android.domain.ShoeTier
import com.stepup.android.domain.TrackPoint
import com.stepup.android.domain.RunVerdict
import com.stepup.android.service.RunSaveStatus
import com.stepup.android.service.WalkSessionService
import com.stepup.android.service.WalkSessionState
import com.stepup.android.ui.MainScaffold
import com.stepup.android.ui.Routes
import com.stepup.android.ui.Screen
import com.stepup.android.ui.components.ShoeGradeBadge
import com.stepup.android.ui.components.ShoeNameWithBadge
import com.stepup.android.ui.experience.ExperienceProvider
import com.stepup.android.ui.screens.walk.CrewNotifyBody
import com.stepup.android.ui.theme.StepUpTheme
import com.stepup.android.ui.theme.ThemeMode
import java.io.File
import kotlin.math.roundToInt
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

/**
 * 러닝 전체 리메이크(2026-10-02 러닝 109장 전달본) — 실제 앱(MainScaffold)에서 러닝 홈 · 시작 · 달리는 중 · 시트 · 저장 · 결과 ·
 * 챌린지 · 다이어트 상태를 차례로 띄워 찍는다. 캡처 이름은 전달본 화면 번호(HOME · E01 · U01 · R02 …)다 — `run-journey/`.
 *
 * 러닝 상태는 표시용 값이다(서비스를 켜지 않는다 — [WalkSessionService.showStateForTest]). 서버 확인 상태(E11)는 이 검사가 넣은
 * 저장 줄 하나로만 보이고 끝나면 지운다.
 *
 * 자유 러닝(2026-10-14 v2)은 보상 UI 가 없는 지도 중심 화면이다 — 러닝 중 · 시트 · 결과 · 공유에 보상 칸 · 정산 표시 · SUP 글이 없고,
 * 결과는 보상 상태(확인 중 · 무효 · 미지급 · 서버 확인 · 로그아웃)와 상관없이 같은 모습인지 본다. 보상 칸(E03 · E10 · E11 · E08 · E09)과
 * 위치 끊김 시트(L04) · 저장 실패 시트는 챌린지 러닝에서 찍고, 보상은 서버 확인 전에는 "예상 · 확인 중"으로만 보이는지 본다.
 */
class RunJourneyDesignTest {
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
    @get:Rule(order = 1) val permissions: GrantPermissionRule = GrantPermissionRule.grant(
        Manifest.permission.ACTIVITY_RECOGNITION,
        Manifest.permission.ACCESS_FINE_LOCATION,
        Manifest.permission.ACCESS_COARSE_LOCATION,
        Manifest.permission.POST_NOTIFICATIONS,
    )
    @get:Rule(order = 2) val compose = createAndroidComposeRule<ComponentActivity>()

    private val directory get() = File(compose.activity.getExternalFilesDir(null), "run-journey").apply { mkdirs() }

    private data class Viewport(val width: Int, val height: Int, val font: Float = 1f, val mode: ThemeMode = ThemeMode.DARK) {
        val label get() = "${width}x$height" + (if (font != 1f) "-font${(font * 100).roundToInt()}" else "") +
            (if (mode == ThemeMode.LIGHT) "-light" else "")
    }

    private var viewport by mutableStateOf(Viewport(390, 844))
    /** 검사 전 서버 동기화 표시([seed] 가 읽는다) — 끝나면 되돌린다 */
    private var syncBefore: EconomySyncState? = null
    private var originalEquipped: Long? = null
    private var added: Long? = null

    /** 새 대기실 만들기(2026-10-26) — 코스를 주면 직접 설정, 없으면 자유 러닝 */
    @androidx.compose.runtime.Composable
    private fun createForm(
        name: String,
        course: com.stepup.android.domain.RoomCourse?,
        problem: com.stepup.android.ui.screens.walk.CreateProblem?,
        mode: com.stepup.android.domain.RoomMode = com.stepup.android.domain.RoomMode.TOGETHER,
    ) = com.stepup.android.ui.screens.walk.CrewRoomCreateContent(
        crewName = "StepUp Crew", name = name, onName = {},
        courseChoice = if (course != null) com.stepup.android.ui.screens.walk.CourseChoice.SET else com.stepup.android.ui.screens.walk.CourseChoice.FREE,
        onCourseChoice = {}, course = course, onPickCourse = {}, mode = mode, onMode = {},
        meet = "", onMeet = {}, planAt = com.stepup.android.ui.screens.walk.defaultPlanAt(), onPlanAt = {},
        capacity = 6, onCapacity = {}, busy = false, problem = problem, onBack = {}, onOpen = {},
    )

    @Test fun screens() {
        seed()
        try {
            edgeToEdge()
            compose.setContent { DeviceFrame { MainScaffold() } }

            // HOME · E01 — 러닝 홈(UI 최종 통합안: 오늘 걸음 · 자주 달리는 곳 사진 카드 · 숫자 줄 · 둥근 러닝 시작)과 오늘의 활동 시트
            awaitTag("home-start-run")
            awaitTag("home-spot")
            compose.onNodeWithTag("home-stats").assertExists()
            // 신발 전시장 · 신발 줄은 홈에 없다(신발 탭으로)
            compose.onAllNodesWithTag("home-shoe-name", useUnmergedTree = true).assertCountEquals(0)
            shot("HOME")
            // 신발 이름 바꾸기(2026-10-17) — 신발 탭 연필로 옮겼다. 이 폰에만 적고, 원래 이름으로 돌릴 수 있다
            val shoeId = requireNotNull(added)
            compose.onNodeWithTag("nav-label-${Screen.Customize.route}", useUnmergedTree = true).performClick()
            awaitTag("shoe-hero-name")
            tap("shoe-rename")
            awaitTag("shoe-rename-sheet")
            compose.onNodeWithTag("shoe-rename-field").performTextClearance()
            compose.onNodeWithTag("shoe-rename-field").performTextInput("나의 아주 긴 첫 레이싱화 이름이에요 정말로")
            shot("SHOES-rename")
            tap("shoe-rename-save")
            awaitGone("shoe-rename-sheet")
            // 20자에서 자른다 — 그리고 두 줄 안에서 글자를 줄여 다 보인다(… 없이)
            val saved = runBlocking { ServiceLocator.userPrefs.shoeNicknames.first()[shoeId] }
            assertEquals("나의 아주 긴 첫 레이싱화 이름이에요", saved)
            compose.waitUntil(5_000) {
                runCatching { compose.onNodeWithTag("shoe-hero-name", useUnmergedTree = true).assertTextEquals(saved!!) }.isSuccess
            }
            val layouts = mutableListOf<androidx.compose.ui.text.TextLayoutResult>()
            compose.onNodeWithTag("shoe-hero-name", useUnmergedTree = true)
                .performSemanticsAction(androidx.compose.ui.semantics.SemanticsActions.GetTextLayoutResult) { it(layouts) }
            assertTrue("long shoe names stay whole: ${layouts.map { it.lineCount }}", layouts.none { it.hasVisualOverflow })
            shot("SHOES-renamed")
            tap("shoe-rename")
            awaitTag("shoe-rename-reset")
            tap("shoe-rename-reset")
            awaitGone("shoe-rename-sheet")
            compose.waitUntil(5_000) { runBlocking { ServiceLocator.userPrefs.shoeNicknames.first()[shoeId] } == null }
            compose.onNodeWithTag("nav-label-${Screen.Run.route}", useUnmergedTree = true).performClick()
            awaitTag("home-start-run")
            tap("home-steps")
            awaitTag("home-details")
            shot("E01")
            back()
            awaitGone("home-details")

            // U01 · U02 — 러닝 시작(다섯 방법)과 러닝 챌린지
            tap("home-start-run")
            awaitTag("run-menu-free")
            compose.onNodeWithTag("sup-balance", useUnmergedTree = true).assertExists()
            shot("U01")
            tap("run-menu-goals")
            awaitTag("run-goals")
            shot("U02")
            // C03 — 지난 도전(이 검사가 넣은 도전 셋 — 끝나면 지운다)
            runBlocking {
                val day = 86_400_000L
                val at = System.currentTimeMillis()
                ServiceLocator.userPrefs.clearGoalAttempts()
                listOf(
                    GoalAttempt(at - 4 * day, RunGoal.TEN_MIN, true, 600, 1.28),
                    GoalAttempt(at - 6 * day, RunGoal.ONE_KM, true, 412, 1.0),
                    GoalAttempt(at - 7 * day, RunGoal.THREE_KM, true, 1_260, 3.0),
                ).forEach { ServiceLocator.userPrefs.addGoalAttempt(it) }
            }
            tap("run-goals-history")
            awaitTag("run-goal-history")
            shot("C03")
            back()
            awaitTag("run-goals")
            back()
            awaitTag("run-menu-free")
            back()
            awaitTag("home-start-run")

            // R02 — 자유 러닝 중(2026-10-14 v2: 지도 가득 · 형광 초록 경로 · 아래 작은 기록 판). 보상 칸 · SUP 는 없다
            val now = System.currentTimeMillis()
            RunPlans.set(RunPlan.Free)
            WalkSessionService.showStateForTest(running(now))
            tap("home-start-run")
            awaitTag("run-live")
            compose.onNodeWithTag("run-hero-value", useUnmergedTree = true).assertTextEquals("12:30")
            compose.onNodeWithTag("run-distance-value", useUnmergedTree = true).assertTextEquals("1.56")
            compose.onNodeWithTag("run-speed-value", useUnmergedTree = true).assertExists()
            // 신발 내구도(seed — 92)는 러닝 정보(E03)에서 본다 — 러닝 화면 숫자 줄은 시간 · 페이스 · 속도(UI 최종 통합안)
            compose.onNodeWithTag("run-status", useUnmergedTree = true).assertTextEquals(label(R.string.run_free_chip_running))
            compose.onNodeWithTag("run-primary-action").assertIsDisplayed().assertTextContains(label(R.string.run_pause))
            // 달리는 중에는 종료가 없다 — 화면 잠금 · 일시정지 · 지도. 종료는 멈춘 뒤 길게 눌러서
            compose.onAllNodesWithTag("run-finish").assertCountEquals(0)
            compose.onNodeWithTag("run-lock").assertIsDisplayed()
            compose.onAllNodesWithTag("run-details-reward", useUnmergedTree = true).assertCountEquals(0)
            assertNoRewardText("run-live")
            assertRunFits("390x844")
            // 지도는 위, 숫자 판은 그 아래(2026-10-26 시안 active)
            assertMapShare("390x844")
            shot("R02", settle = 2_500)

            // E06 — 속도 측정 오류 띠 · 측정 무효 띠(가짜 위치). 믿을 수 없는 속도 · 페이스는 "—", 보상 · 적립 안내는 없다
            WalkSessionService.showStateForTest(running(now, flagged = 2, flaggedAt = now))
            awaitTag("run-speed-banner")
            assertUnknownSpeed()
            assertNoRewardText("run-live")
            shot("E06")
            WalkSessionService.showStateForTest(running(now).copy(mockLocation = true))
            awaitTag("run-void-banner")
            assertUnknownSpeed()
            assertNoRewardText("run-live")
            shot("E06b-void")

            // R02_TIME — 달리는 중 휴대폰 위치를 껐다(거리 · 페이스는 재지 않는다). 위치 없이 시작하는 길은 없다(2026-10-16)
            WalkSessionService.showStateForTest(timeOnly(now))
            awaitTag("run-no-location")
            compose.onNodeWithTag("run-distance-value", useUnmergedTree = true).assertTextEquals("—")
            compose.onNodeWithTag("run-speed-value", useUnmergedTree = true).assertTextEquals("—")
            // 경로가 없으니 지도 크게 보기도 없다
            compose.onAllNodesWithTag("run-map-expand").assertCountEquals(0)
            shot("R02_TIME")

            // L04 — 러닝 중 위치 신호가 끊김. 자유 러닝은 시트를 열지 않고 지도 위 띠로 알린다(시간은 계속, 끊긴 구간 거리는 더하지 않는다)
            // (시트 L04 는 아래 챌린지 러닝에서 찍는다)
            WalkSessionService.showStateForTest(running(now).copy(gpsLost = true))
            awaitTag("run-gps-weak")
            compose.onNodeWithTag("run-speed-value", useUnmergedTree = true).assertTextEquals("—")
            shot("L04-free")
            // 기다린 뒤에도 시트는 열리지 않는다
            compose.onAllNodesWithTag("run-gps-lost", useUnmergedTree = true).assertCountEquals(0)

            // E03 — 자유 러닝의 러닝 정보(목표 · 코스 · 현재 속도 · 내구도). 예상 SUP 칸은 없다(예상 SUP 가 있는 E03 은 챌린지 러닝에서)
            WalkSessionService.showStateForTest(running(now))
            awaitGone("run-gps-weak")
            tap("run-details-open")
            awaitTag("run-details")
            compose.onAllNodesWithTag("run-details-reward", useUnmergedTree = true).assertCountEquals(0)
            compose.onAllNodesWithTag("run-details-estimate", useUnmergedTree = true).assertCountEquals(0)
            compose.onNodeWithTag("run-details-goal-edit").assertExists()
            compose.onNodeWithTag("run-speed", useUnmergedTree = true).assertExists()
            compose.onNodeWithTag("run-durability", useUnmergedTree = true).assertTextEquals("92")
            assertNoRewardText("run-details")
            shot("E03-free")
            tap("run-details-close")
            awaitGone("run-details")

            // K07 — 러닝 중 전체 지도
            tap("run-map-expand")
            awaitTag("run-full-map")
            shot("K07", settle = 2_500)
            tap("run-full-map-close")
            awaitGone("run-full-map")

            // R03 — 일시정지(일시정지 띠 · 다시 시작). 멈춰 있으니 속도 0.0, 페이스는 "—"
            WalkSessionService.showStateForTest(running(now).copy(isPaused = true))
            eventually { compose.onNodeWithTag("run-primary-action").assertTextContains(label(R.string.run_resume_short)) }
            awaitTag("run-paused-band")
            compose.onNodeWithTag("run-pace-value", useUnmergedTree = true).assertTextEquals("—")
            compose.onNodeWithTag("run-speed-value", useUnmergedTree = true).assertTextEquals("0.0")
            shot("R03")

            // E02 — 러닝 목표 수정(멈춘 채로)
            tap("run-details-open")
            tap("run-details-goal-edit")
            awaitTag("run-goal-value")
            shot("E02")
            tap("run-goal-cancel")
            awaitGone("run-goal-value")

            // R04 — 종료는 1초 길게(확인창 없음). 짧게 누르면 "길게 눌러 종료해요" 말풍선만
            tap("run-finish")
            awaitTag("run-hold-tip")
            compose.onAllNodesWithTag("run-end-dialog").assertCountEquals(0)
            shot("R04")
            // R07 — 저장하지 않고 종료는 멈춘 뒤 러닝 정보 안에 — 확인 시트(돌아가기 · 저장 없이 종료), 돌아가면 멈춘 러닝
            tap("run-details-open")
            awaitTag("run-details-discard")
            tap("run-details-discard")
            awaitTag("run-discard-dialog")
            compose.onNodeWithTag("run-discard-confirm").assertTextContains(label(R.string.run_free_discard_confirm))
            compose.onNodeWithTag("run-discard-back").assertTextContains(label(R.string.runflow_discard_back))
            shot("R07")
            tap("run-discard-back")
            awaitGone("run-discard-dialog")
            awaitTag("run-finish")

            // E04 · S01 — 저장 중 · 저장 실패(기록은 화면에 남고 같은 러닝으로 다시 저장)
            WalkSessionService.showStateForTest(running(now).copy(isPaused = true, saveStatus = RunSaveStatus.SAVING))
            awaitTag("run-saving")
            compose.onNodeWithTag("run-primary-action").assertIsNotEnabled().assertTextContains(label(R.string.run_saving_title))
            shot("E04")
            // 자유 러닝의 저장 실패는 시트 없이 — 위 노란 띠 · 기록은 화면에 남아 있어요 · 다시 저장 | 화면에 머물기
            WalkSessionService.showStateForTest(running(now).copy(isPaused = true, saveStatus = RunSaveStatus.FAILED))
            awaitTag("run-save-stay")
            awaitTag("run-save-error")
            compose.onAllNodesWithTag("run-save-failed-dialog", useUnmergedTree = true).assertCountEquals(0)
            compose.onNodeWithTag("run-save-kept", useUnmergedTree = true).assertExists()
            compose.onNodeWithTag("run-primary-action").assertIsEnabled().assertTextContains(label(R.string.runflow_save_again))
            // 저장 실패 중에는 종료 · 러닝 정보 버튼을 두지 않는다
            compose.onAllNodesWithTag("run-finish").assertCountEquals(0)
            compose.onAllNodesWithTag("run-details-open").assertCountEquals(0)
            shot("S01")
            // 화면에 머물기는 한 번 누르면 숨고, 저장하지 못했다는 띠와 다시 저장은 남는다
            tap("run-save-stay")
            awaitGone("run-save-stay")
            compose.onNodeWithTag("run-save-error").assertIsDisplayed()
            compose.onNodeWithTag("run-primary-action").assertIsEnabled()
            shot("S01b-unsaved")

            // R05 — 자유 러닝 결과(지도 가득 · 러닝 완료 · 기록 저장 완료 · 기록 판 · 완료 | 공유). 보상 칸 · 정산 표시 · SUP 는 없다
            // (보상 칸이 있는 결과 E10 · E11 · E08 · E09 는 아래 챌린지 러닝 결과에서 찍는다)
            WalkSessionService.showStateForTest(finished(now))
            awaitTag("run-result")
            signedIn()
            assertFreeResult()
            assertResultFits("390x844")
            shot("R05", settle = 2_500)

            // 보상 상태가 달라도 자유 러닝 결과는 같은 모습 — 지급 제외(무효 판정) · 미지급(걸음이 없다)
            for ((name, state) in listOf(
                "R05-void" to finished(now).copy(lastVerdict = RunVerdict.VOID),
                "R05-unpaid" to finished(now).copy(lastSessionSteps = 0, lastRewardPoints = 0.0),
            )) {
                WalkSessionService.showStateForTest(state)
                // 바뀐 상태가 그려질 때까지 기다린 뒤(shot) 같은 모습인지 본다
                shot(name)
                assertFreeResult()
            }
            // 서버가 확인한 금액(이 검사가 넣은 저장 줄 — 끝나면 지운다)
            WalkSessionService.showStateForTest(finished(now))
            signedRow(now, "0.80")
            try {
                shot("R05-signed", settle = 1_500)
                assertFreeResult()
            } finally {
                dropRow(now)
            }
            // 로그아웃(다시 로그인 안내도 자유 러닝 결과에는 없다) — 끝나면 동기화 표시를 되돌린다
            ServiceLocator.economySync.showStateForTest(EconomySyncState.SIGNED_OUT)
            try {
                shot("R05-signed-out")
                assertFreeResult()
            } finally {
                ServiceLocator.economySync.showStateForTest(EconomySyncState.SYNCED)
            }

            // R06 — 저장한 기록 삭제 확인(누르기 전에는 지우지 않는다). 덜 쓰는 기록 보기 · 삭제는 ⋯ 안에
            tap("run-result-more")
            awaitTag("run-result-more-sheet")
            compose.onNodeWithTag("run-result-records").assertExists()
            assertNoRewardText("run-result-more-sheet")
            shot("R05-more")
            tap("run-result-delete")
            awaitTag("run-delete-sheet")
            shot("R06")
            tap("run-delete-cancel")
            awaitGone("run-delete-sheet")

            // 14 오른쪽 — 기록 삭제 실패. 서버 확인을 기다리는 저장 줄은 지우지 않는다(이 검사가 넣은 줄 — 끝나면 지운다)
            pendingRow(now)
            try {
                tap("run-result-more")
                tap("run-result-delete")
                tap("run-delete-confirm")
                awaitTag("run-delete-failed")
                awaitTag("run-delete-retry")
                awaitTag("run-delete-back")
                // 실패 중에는 ⋯ 를 두지 않는다 — 다시 시도하거나 돌아간다
                compose.onAllNodesWithTag("run-result-more").assertCountEquals(0)
                assertTrue("record kept after a failed delete",
                    runBlocking { ServiceLocator.runRecordsRepository.idForStart(now - 750_000) } != null)
                shot("R06b-delete-failed")
                tap("run-delete-back")
                awaitGone("run-delete-failed")
                awaitTag("run-result-more")
            } finally {
                dropRow(now)
            }

            // E05 — 공유 미리보기: 경로 포함은 처음에 꺼져 있다(그림에 길 · 지도가 없다). 보상(SUP)은 그림 · 글에 없다
            tap("run-result-share")
            awaitTag("run-share")
            compose.onNodeWithTag("run-share-route").assertIsOff()
            eventually { compose.onNodeWithTag("run-share-send").assertIsEnabled() }
            assertNoRewardText("run-share")
            shot("E05")
            tap("run-share-route")
            compose.onNodeWithTag("run-share-route").assertIsOn()
            eventually { compose.onNodeWithTag("run-share-send").assertIsEnabled() }
            shot("E05b-route")
            tap("run-share-cancel")
            awaitTag("run-result")

            // 14 왼쪽 — 저장된 경로가 없는 자유 러닝 결과. 지도를 꾸미지 않고, GPS 거리가 없으면 "—"(걸음으로 셈한 거리로 채우지 않는다)
            WalkSessionService.showStateForTest(finished(now).copy(track = emptyList(), lastGpsKm = 0.0))
            awaitTag("run-result-no-route")
            compose.onAllNodesWithTag("run-result-map", useUnmergedTree = true).assertCountEquals(0)
            compose.onNodeWithTag("run-result-time", useUnmergedTree = true).assertTextEquals("12:30")
            compose.onNodeWithTag("run-result-distance", useUnmergedTree = true).assertTextEquals("—")
            compose.onNodeWithTag("run-result-pace", useUnmergedTree = true).assertTextEquals("—")
            compose.onNodeWithTag("run-result-speed", useUnmergedTree = true).assertTextEquals("—")
                assertNoRewardResult()
            shot("R05-no-route")

            // U03 · L04 · E03 · S01-goal · C04 · C05 · C01 · C02 — 러닝 챌린지(자유 러닝이 아니라 예전 러닝 화면 그대로)
            val tenMin = challenge(RunGoal.TEN_MIN, now + 1, elapsed = 384, km = 0.82)
            shot("U03", settle = 2_000)
            // L04 — 자유 러닝이 아닌 러닝은 위치 신호가 끊기면 시트로 한 번 알린다
            WalkSessionService.showStateForTest(tenMin.copy(gpsLost = true))
            awaitTag("run-gps-lost")
            shot("L04")
            tap("run-gps-time-only")
            awaitGone("run-gps-lost")
            // E03 — 러닝 상세 정보(목표 · 예상 SUP 확인 중 · 코스 · 현재 속도 · 내구도)
            WalkSessionService.showStateForTest(tenMin)
            awaitGone("run-gps-lost")
            tap("run-details-open")
            awaitTag("run-details")
            compose.onNodeWithTag("run-details-estimate", useUnmergedTree = true).assertTextContains("+", substring = true)
            shot("E03")
            tap("run-details-close")
            awaitGone("run-details")
            // S01 — 자유 러닝이 아닌 러닝의 저장 실패는 시트로 알린다(기록은 화면에 남고 같은 러닝으로 다시 저장)
            WalkSessionService.showStateForTest(tenMin.copy(isPaused = true, saveStatus = RunSaveStatus.FAILED))
            awaitTag("run-save-failed-dialog")
            shot("S01-goal")
            tap("run-save-stay")
            awaitGone("run-save-failed-dialog")
            awaitTag("run-save-error")
            challenge(RunGoal.ONE_KM, now + 2, elapsed = 312, km = 0.64)
            shot("C04", settle = 2_000)
            challenge(RunGoal.THREE_KM, now + 3, elapsed = 936, km = 1.92)
            shot("C05", settle = 2_000)
            challenge(RunGoal.TEN_MIN, now + 4, elapsed = 600, km = 1.28)
            awaitTag("run-goal-reached")
            shot("C01")
            tap("run-goal-reached-more")
            awaitGone("run-goal-reached")
            WalkSessionService.showStateForTest(finished(now + 4).copy(lastElapsedSec = 384, lastGpsKm = 0.82))
            awaitTag("run-result")
            signedIn()
            compose.onNodeWithTag("run-result-note", useUnmergedTree = true).assertExists()
            shot("C02", settle = 1_500)

            // E10 · E11 · E08 · E09 — 보상 칸은 자유 러닝이 아닌 러닝의 결과에만 있다. 같은 챌린지 러닝(10분 · 시작 시각이 같다)의 결과로 찍는다
            val goalRun = now + 4
            // E10 — 보상 확인 중(서버 확인 전: "예상 +0.80 SUP · 확인 중")
            WalkSessionService.showStateForTest(finished(goalRun))
            awaitTag("run-result-settle-pending")
            signedIn()
            // C02(0.82km)에서 바뀐 결과가 그려질 때까지
            eventually { compose.onNodeWithTag("run-result-distance", useUnmergedTree = true).assertTextEquals("1.56") }
            compose.onAllNodesWithTag("run-result-settle-done", useUnmergedTree = true).assertCountEquals(0)
            compose.onNode(hasText("+0.8", substring = true) and hasAnyAncestor(hasTestTag("run-result-reward")), useUnmergedTree = true)
                .assertExists()
            compose.onNode(hasTestTag("tier-badge-rare") and hasAnyAncestor(hasTestTag("run-result-shoe")), useUnmergedTree = true)
                .assertExists()
            assertResultFits("390x844")
            shot("E10", settle = 2_500)

            // E11 — 서버가 확인한 금액(이 검사가 넣은 저장 줄 — 끝나면 지운다)
            signedRow(goalRun, "0.80")
            try {
                awaitTag("run-result-settle-done")
                shot("E11", settle = 1_500)
            } finally {
                dropRow(goalRun)
            }

            // E08 · E09 — 지급 제외(까닭 자세히) · 미지급(걸음이 없어 올릴 것이 없다)
            WalkSessionService.showStateForTest(finished(goalRun).copy(lastVerdict = RunVerdict.VOID))
            awaitTag("run-result-settle-void")
            compose.onAllNodesWithTag("run-result-reward").assertCountEquals(0)
            shot("E08")
            // 까닭 자세히는 넘기는 결과 안에 있다 — 화면에 보이게 넘긴 뒤 누른다
            compose.onNodeWithTag("run-result-void-reason").performScrollTo()
            tap("run-result-void-reason")
            awaitTag("run-sheet-close")
            shot("E08b-reason")
            back()
            awaitGone("run-sheet-close")
            WalkSessionService.showStateForTest(finished(goalRun).copy(lastSessionSteps = 0, lastRewardPoints = 0.0))
            awaitTag("run-result-settle-rejected")
            shot("E09")

            // D07 · D08 · D09 · D11 · D12 · D10 · D13 · S02 — 다이어트 모드(3분 + (1분 러닝 + 2분 걷기)×3 + 3분 = 15분)
            val diet = now + 5
            RunPlans.set(RunPlan.Diet(RunExperience.FIRST))
            for ((name, elapsed) in listOf("D07" to 36L, "D08" to 208L, "D09" to 280L, "D11" to 780L)) {
                WalkSessionService.showStateForTest(dietRun(diet, elapsed))
                awaitTag("run-diet-next")
                shot(name, settle = 1_500)
            }
            WalkSessionService.showStateForTest(dietRun(diet, 208).copy(isPaused = true))
            eventually { compose.onNodeWithTag("run-primary-action").assertTextContains(label(R.string.run_resume)) }
            shot("D12")
            WalkSessionService.showStateForTest(dietRun(diet, 900).copy(isPaused = true))
            awaitTag("run-diet-done")
            shot("D10")
            WalkSessionService.showStateForTest(finished(diet).copy(lastElapsedSec = 380, lastGpsKm = 0.42))
            awaitTag("run-result")
            signedIn()
            shot("D13", settle = 1_500)
            WalkSessionService.showStateForTest(finished(diet).copy(lastElapsedSec = 900, lastGpsKm = 1.15))
            awaitTag("run-result")
            shot("S02", settle = 1_500)
        } finally {
            runBlocking {
                ServiceLocator.userPrefs.clearGoalAttempts()
                added?.let { ServiceLocator.userPrefs.setShoeNickname(it, null) }
            }
            WalkSessionService.showStateForTest(WalkSessionState())
            RunPlans.clear()
            syncBefore?.let { ServiceLocator.economySync.showStateForTest(it) }
            restore()
        }
    }

    /**
     * 360×800 · 390×844 · 412×915 · 큰 글씨 · 밝은 테마 — 자유 러닝 중은 큰 시간이 버튼 위 · 두 버튼이 화면 안(390×844 · 글자 100% 는
     * 지도가 러닝 화면 높이의 68% 이상), 결과는 완료 · 공유가 화면 안이고 보상 칸 · SUP 가 없다
     */
    /** 가장 긴 이름(20자, 꽉 찬 한글) — 360dp · 큰 글씨에서도 신발 탭 신발 이름이(UI 최종 통합안에서 신발 탭으로 옮겼다) 넘치지 않고 다 보인다(2026-10-17) */
    @Test fun longShoeNameFits() {
        seed()
        val shoeId = requireNotNull(added)
        val longest = "가나다라마바사아자차카타파하가나다라마바"
        try {
            runBlocking { ServiceLocator.userPrefs.setShoeNickname(shoeId, longest) }
            edgeToEdge()
            compose.setContent { DeviceFrame { MainScaffold(initialTab = Screen.Customize) } }
            for (next in listOf(Viewport(360, 800), Viewport(360, 800, font = 1.3f), Viewport(412, 915))) {
                compose.runOnIdle { viewport = next }
                compose.waitForIdle()
                awaitTag("shoe-hero-name")
                compose.waitUntil(5_000) {
                    runCatching { compose.onNodeWithTag("shoe-hero-name", useUnmergedTree = true).assertTextEquals(longest) }.isSuccess
                }
                val layouts = mutableListOf<androidx.compose.ui.text.TextLayoutResult>()
                compose.onNodeWithTag("shoe-hero-name", useUnmergedTree = true)
                    .performSemanticsAction(androidx.compose.ui.semantics.SemanticsActions.GetTextLayoutResult) { it(layouts) }
                assertTrue("${next.label}: the whole name shows (${layouts.map { it.lineCount }})", layouts.none { it.hasVisualOverflow })
                compose.onNodeWithTag("shoe-rename").assertIsDisplayed()
                shot("SHOES-long-name-${next.label}")
            }
        } finally {
            runBlocking { ServiceLocator.userPrefs.setShoeNickname(shoeId, null) }
            syncBefore?.let { ServiceLocator.economySync.showStateForTest(it) }
            restore()
        }
    }

    @Test fun fitsAtDeviceSizes() {
        seed()
        try {
            edgeToEdge()
            compose.setContent { DeviceFrame { MainScaffold(initialRoute = Routes.RUN) } }
            val now = System.currentTimeMillis()
            RunPlans.set(RunPlan.Free)
            for (next in listOf(
                Viewport(360, 800), Viewport(390, 844), Viewport(412, 915),
                Viewport(360, 800, font = 1.3f), Viewport(390, 844, mode = ThemeMode.LIGHT),
            )) {
                compose.runOnIdle { viewport = next }
                compose.waitForIdle()
                WalkSessionService.showStateForTest(running(now))
                awaitTag("run-live")
                assertRunFits(next.label)
                if (next.width == 390 && next.height == 844 && next.font == 1f) assertMapShare(next.label)
                shot("f-${next.label}-run")
                WalkSessionService.showStateForTest(finished(now))
                awaitTag("run-result")
                signedIn()
                assertResultFits(next.label)
                assertNoRewardResult()
                shot("f-${next.label}-result", settle = 1_200)
            }
        } finally {
            WalkSessionService.showStateForTest(WalkSessionState())
            RunPlans.clear()
            syncBefore?.let { ServiceLocator.economySync.showStateForTest(it) }
            restore()
        }
    }

    /** U05 · D01 · D02 · D03 · U06 · D06 — 다이어트 모드 입력(휴대폰 숫자 자판)과 러닝 방법(실제 앱) */
    @Test fun diet() {
        seed()
        try {
            edgeToEdge()
            compose.setContent { DeviceFrame { MainScaffold(initialRoute = Routes.RUN_DIET) } }
            awaitTag("diet-input")
            // 시안 값(170cm · 70kg · 처음이에요)을 휴대폰 숫자 자판으로 적는다
            compose.onNodeWithTag("diet-height").performTextClearance()
            compose.onNodeWithTag("diet-height").performTextInput("170")
            compose.onNodeWithTag("diet-weight").performTextClearance()
            compose.onNodeWithTag("diet-weight").performTextInput("70")
            awaitTag("diet-input-done")
            shot("D01", settle = 1_500)
            tap("diet-input-done")
            awaitGone("diet-input-done")
            tap("diet-exp-first")
            shot("U05")
            // D02 — 키를 지우면 칸 아래 안내, 다음 버튼은 누를 수 없다
            compose.onNodeWithTag("diet-height").performTextClearance()
            awaitTag("diet-input-error")
            tap("diet-input-done")
            compose.onNodeWithTag("diet-input-next").assertIsNotEnabled()
            shot("D02")
            compose.onNodeWithTag("diet-height").performTextInput("170")
            tap("diet-input-done")
            // D03 — 적던 중 나가려 하면 한 번 묻는다. 키보드가 떠 있으면 뒤로 가기는 키보드만 닫으므로 먼저 닫는다
            // (숫자를 다시 적으면 키보드가 다시 뜬다 — 느린 에뮬레이터에서 "완료"보다 늦게 떠 뒤로 가기를 먹었다, Experience QA 40069908782)
            hideKeyboard()
            back()
            awaitTag("diet-leave-dialog")
            shot("D03")
            tap("diet-leave-stay")
            awaitGone("diet-leave-dialog")
            // U06 — 러닝 방법 추천(15분 = 준비 3분 + (러닝 1분 + 걷기 2분) × 3 + 마무리 3분)
            tap("diet-input-next")
            awaitTag("diet-plan")
            compose.onNodeWithTag("diet-plan-minutes", useUnmergedTree = true).assertTextEquals(label(R.string.run_diet_min_big, 15))
            shot("U06")
            // D06 — 몸 정보 · 경험 수정
            tap("diet-plan-edit")
            awaitTag("diet-input")
            shot("D06")
            back()
            awaitTag("diet-plan")
        } finally {
            restore()
        }
    }

    /** K01 · K03 — 추천 코스(실제 앱): 찾는 중, 그리고 가까운 코스가 없을 때(에뮬레이터 자리에는 저장 · 게시판 코스가 없다) */
    @Test fun courseRec() {
        seed()
        try {
            edgeToEdge()
            compose.setContent { DeviceFrame { MainScaffold(initialRoute = Routes.RUN_COURSE) } }
            awaitTag("run-course-rec")
            // 기기에서 지나가는 대로 — 자리 확인 중(L03) · 자리를 알고 코스를 찾는 중(K01). 늘 같은 모습은 states() 가 찍는다
            if (exists("run-course-locating")) shot("L03-device")
            compose.waitUntil(25_000) { exists("run-course-finding") || exists("run-course-none") || exists("run-course-start") }
            if (exists("run-course-finding")) shot("K01-device")
            compose.waitUntil(25_000) { exists("run-course-none") || exists("run-course-start") }
            shot(if (exists("run-course-none")) "K03" else "K02-device", settle = 2_000)
        } finally {
            restore()
        }
    }

    /** K09 · K13 · K14 · K12 · K10 · K11 · K16 — 코스 허브(실제 앱, 앱이 심은 공원 코스) */
    @Test fun courseHub() {
        seed()
        val repo = ServiceLocator.courseRepository
        try {
            runBlocking { repo.ensureSeeded() }
            val list = runBlocking { repo.courses.first().filter { it.hasTrack } }
            val first = list.first()
            runBlocking { repo.select(first.id) }
            edgeToEdge()
            compose.setContent { DeviceFrame { MainScaffold(initialRoute = Routes.COURSES) } }
            awaitTag("courses-title")
            awaitTag("course-pick-${first.id}")
            compose.onNodeWithTag("courses-run").assertIsEnabled()
            shot("K09", settle = 2_500)
            // K14 — 고른 코스를 누르면 해제할지 묻는다
            tap("course-pick-${first.id}")
            awaitTag("course-clear-sheet")
            shot("K14")
            tap("course-confirm-cancel")
            awaitGone("course-clear-sheet")
            // K13 — 다른 코스를 누르면 고를지 묻는다
            list.getOrNull(1)?.let { second ->
                compose.onNodeWithTag("courses-list").performScrollToNode(hasTestTag("course-pick-${second.id}"))
                tap("course-pick-${second.id}")
                awaitTag("course-apply-sheet")
                shot("K13", settle = 1_800)
                tap("course-confirm-cancel")
                awaitGone("course-apply-sheet")
            }
            // K12 — 코스 순위(서버 기록 — 로그인 전이면 그 까닭)
            compose.onNodeWithTag("courses-list").performScrollToNode(hasTestTag("course-rank-${first.id}"))
            tap("course-rank-${first.id}")
            awaitTag("course-ranking")
            shot("K12", settle = 2_500)
            tap("course-ranking-close")
            awaitGone("course-ranking")
            // K10 · K11 · K16
            tap("courses-tab-1")
            awaitTag("course-recorder")
            shot("K10")
            tap("courses-tab-2")
            compose.waitUntil(15_000) { !exists("courses-board-loading") }
            shot("K11", settle = 2_500)
            tap("courses-upload")
            awaitTag("course-upload-sheet")
            shot("K16")
            tap("course-upload-cancel")
        } finally {
            runBlocking { repo.clearSelection() }
            restore()
        }
    }

    /** K04 · K07 · K05 · K08 · K06 · K15 — 코스를 고르고 달리는 중 · 전체 지도 · 코스 이탈 · 자유 러닝 전환 · 결과 · 달린 코스 저장 */
    @Test fun courseRun() {
        seed()
        val repo = ServiceLocator.courseRepository
        try {
            runBlocking { repo.ensureSeeded() }
            val course = runBlocking { repo.courses.first().first { it.hasTrack } }
            runBlocking { repo.select(course.id) }
            RunPlans.set(RunPlan.Free)
            val now = System.currentTimeMillis()
            WalkSessionService.showStateForTest(onCourse(course, now))
            edgeToEdge()
            compose.setContent { DeviceFrame { MainScaffold(initialRoute = Routes.RUN) } }
            awaitTag("run-live")
            awaitTag("run-goal-bar")
            shot("K04", settle = 2_500)
            tap("run-map-expand")
            awaitTag("run-full-map")
            shot("K07", settle = 2_500)
            tap("run-full-map-close")
            awaitGone("run-full-map")
            // K05 — 코스 선에서 60m 넘게 15초 넘게 떨어지면 한 번 알린다(기록은 그대로)
            val start = course.points.first()
            WalkSessionService.showStateForTest(onCourse(course, now).copy(here = GeoPoint(start.lat + 0.0025, start.lng + 0.0025)))
            awaitTag("run-course-off-map", timeout = 30_000)
            shot("K05")
            tap("run-course-off-free")
            awaitTag("run-course-free-ok")
            shot("K08")
            tap("run-course-free-cancel")
            awaitGone("run-course-free-ok")
            // K06 — 코스 결과
            WalkSessionService.showStateForTest(
                WalkSessionState(
                    lastRewardPoints = 0.9, lastSessionSteps = 3_100, lastRewardedSteps = 3_100,
                    lastElapsedSec = 1_200, lastGpsKm = course.distanceKm, lastStartedAt = now - 1_200_000,
                    track = course.points.mapIndexed { i, p -> TrackPoint(p.lat, p.lng, now - 1_200_000 + i * 5_000L) },
                ),
            )
            awaitTag("run-result")
            signedIn()
            shot("K06", settle = 2_500)
            // K15 — "코스 만들기"로 달린 뒤 저장
            runBlocking { repo.beginRecording() }
            WalkSessionService.lastTrack.value = course.points
            awaitTag("course-save-sheet")
            shot("K15", settle = 2_500)
            tap("course-save-skip")
            awaitGone("course-save-sheet")
        } finally {
            WalkSessionService.showStateForTest(WalkSessionState())
            WalkSessionService.lastTrack.value = emptyList()
            runBlocking {
                repo.cancelRecording()
                repo.clearSelection()
            }
            RunPlans.clear()
            syncBefore?.let { ServiceLocator.economySync.showStateForTest(it) }
            restore()
        }
    }

    /** 앱에서 잠깐만 지나가거나 서버 없이 만들 수 없는 상태 — 같은 화면 부품을 그 상태로 그려 찍는다(D04 · D05 · R01 · E07 · F01~F07 · L03 · K01 · K17 · K18 · U04 · K02) */
    @Test fun states() {
        seed()
        val repo = ServiceLocator.courseRepository
        try {
            runBlocking { repo.ensureSeeded() }
            val courses = runBlocking { repo.courses.first().filter { it.hasTrack } }
            var scene by mutableStateOf<(@Composable () -> Unit)?>(null)
            edgeToEdge()
            compose.setContent { DeviceFrame { Box(Modifier.fillMaxSize()) { scene?.invoke() } } }
            fun show(name: String, tag: String, content: @Composable () -> Unit) {
                compose.runOnIdle { scene = content }
                awaitTag(tag)
                shot(name, settle = 2_000)
            }
            show("D04", "diet-preparing") { com.stepup.android.ui.screens.walk.DietPreparingContent {} }
            show("D05", "diet-failed") { com.stepup.android.ui.screens.walk.DietFailedContent({}, {}) }
            // R01 — 3-2-1(앱이 심은 공원 코스 출발점을 지금 자리로 — 실제 지도 타일)
            val spot = courses.first().points.first().let { GeoPoint(it.lat, it.lng) }
            show("R01", "run-countdown-digit") {
                com.stepup.android.ui.screens.walk.RunCountdownStage(
                    title = label(R.string.runflow_free), subtitle = null, kicker = label(R.string.run_countdown_soon), digit = 3,
                    caption = label(R.string.run_countdown_gps_on), here = spot, onCancel = {},
                )
            }
            // E07 — 휴대폰 위치 기능 꺼짐(앱 권한은 있음). 위치가 없으니 지도를 그리지 않는다. 위치 없이 시작하는 버튼은 없다(2026-10-16)
            show("E07", "run-location-off-gate") {
                com.stepup.android.ui.screens.walk.RunLocationOffContent(label(R.string.runflow_free), {}, {})
            }
            compose.onNodeWithTag("run-location-off-open").assertIsDisplayed()
            compose.onAllNodesWithTag("run-location-off-time").assertCountEquals(0)
            // F01~F07 — 자유러닝 시작(2026-10-17 전달본): 신발 점검 → 3·2·1·0 → StepUP! → 지도 펼침. 시각을 고정해 그 순간을 찍는다
            val checkShoe = com.stepup.android.domain.Sneaker(
                id = 9_401, faction = com.stepup.android.domain.Faction.WIND, rarity = com.stepup.android.domain.Rarity.RARE,
                variant = 0, level = 3, mintNumber = 12, luck = 1.0, comfort = 1.0, durability = 92, equipped = true,
                acquiredAt = 1_790_000_000_000L, modelId = 1201,
            )
            show("F01-shoe-check", "run-shoe-check") {
                com.stepup.android.ui.screens.walk.FreeRunShoeCheckContent(checkShoe, {}, {})
            }
            compose.onNodeWithTag("run-shoe-check-durability-value", useUnmergedTree = true).assertTextEquals("92%")
            fun stage(name: String, clock: Long, reveal: Long? = null, reduced: Boolean = false) = show(name, "run-start-sequence") {
                Box(Modifier.fillMaxSize()) {
                    // 펼침 아래의 러닝 화면 자리 — 실제 지도(지금 자리)
                    com.stepup.android.ui.components.LiveRouteMap(points = listOf(spot), modifier = Modifier.fillMaxSize(), follow = true, live = true)
                    com.stepup.android.ui.screens.walk.FreeRunStartStage(clock = { clock }, revealSince = { reveal }, reduced = reduced)
                }
            }
            stage("F02-count-3", 400)
            compose.onNodeWithTag("run-countdown-digit", useUnmergedTree = true).assertTextEquals("3")
            stage("F03-count-0", 3_100)
            compose.onNodeWithTag("run-countdown-digit", useUnmergedTree = true).assertTextEquals("0")
            stage("F04-launch-cross", 3_180 + 120)
            stage("F05-stepup", 3_700)
            stage("F06-reveal", 3_760 + 300, reveal = 300)
            stage("F07-reduced-cross", 3_180 + 100, reduced = true)
            // L03 — 추천 코스 전 지금 자리 확인 중(자리를 모르면 지도에 아무 자리도 그리지 않는다) · K01 — 자리를 알고 코스를 찾는 중
            show("L03", "run-course-locating") {
                com.stepup.android.ui.screens.walk.CourseRecommendContent(
                    com.stepup.android.ui.screens.walk.CourseRecUi.Locating, null, {}, {}, {}, {}, {}, {},
                )
            }
            show("K01", "run-course-finding") {
                com.stepup.android.ui.screens.walk.CourseRecommendContent(
                    com.stepup.android.ui.screens.walk.CourseRecUi.Finding, spot, {}, {}, {}, {}, {}, {},
                )
            }
            // K17 · K18 — 코스 게시판이 비었을 때 · 못 불러왔을 때(CI 기기는 로그인 전이라 게시판을 읽지 못한다)
            show("K17", "courses-board-empty") {
                com.stepup.android.ui.components.RunPage(onBack = {}, title = label(R.string.courses_board)) {
                    com.stepup.android.ui.screens.walk.BoardEmpty {}
                }
            }
            show("K18", "courses-board-failed") {
                com.stepup.android.ui.components.RunPage(onBack = {}, title = label(R.string.courses_board)) {
                    com.stepup.android.ui.screens.walk.BoardFailed(signIn = false, onRetry = {}, onMine = {})
                }
            }
            // 추천 코스 — 앱이 심은 공원 코스를 그 코스 출발점 가까이에 서 있는 것처럼(에뮬레이터 자리와 무관한 화면 확인용)
            courses.take(2).forEachIndexed { i, course ->
                val here = course.points.first().let { GeoPoint(it.lat + 0.002, it.lng) }
                val pick = com.stepup.android.domain.CourseRecommendations.near(here, listOf(course), radius = 5_000.0).first()
                show(if (i == 0) "U04" else "K02", "run-course-card") {
                    com.stepup.android.ui.screens.walk.CourseRecommendContent(
                        com.stepup.android.ui.screens.walk.CourseRecUi.Found(
                            pick, com.stepup.android.domain.CourseRecommendations.minutes(course.distanceKm), i + 1, 2,
                        ),
                        here, {}, {}, {}, {}, {}, {},
                    )
                }
            }
        } finally {
            restore()
        }
    }

    /**
     * CR — 크루 달리기 v2(2026-10-16 전달본). 입구(고른 크루 · 크루 없음) · 크루 선택 · 대기실(크루원 준비 전 · 준비 완료 · 진행자 · 혼자) ·
     * 출발 확인 · 준비 전 확인 · 크루에게 알리기 · 3-2-1 · 기록 공유는 화면 부품에 예시 크루를 넣어 찍고, 같이 달리는 중 · 함께 달리는 사람 ·
     * 일시정지 · 마치기 · 연결 끊김 · 방 닫힘 · 되살린 러닝 · 결과는 실제 앱(MainScaffold)에서 표시용 러닝 · 방 상태로 찍는다(서버 · 채팅에
     * 보내지 않는다). 러닝의 모임(크루 · 번개)은 출발할 때 적은 값으로 보이는지 — 혼자 출발 · 되살린 러닝 · 번개러닝 이름표를 함께 본다.
     */
    @Test fun crew() {
        seed()
        val repo = ServiceLocator.crewRepository
        val name = "여의도 퇴근런"
        val crews = listOf(
            com.stepup.android.data.repo.Crew(id = "c1", monogram = "YD", name = name, tagline = "함께 달리는 더 특별한 퇴근길",
                area = "여의도", memberCount = 30, roster = emptyList(), owned = false, joined = true),
            com.stepup.android.data.repo.Crew(id = "c2", monogram = "HS", name = "한강 새벽런", tagline = "", area = "",
                memberCount = 18, roster = emptyList(), owned = false, joined = true),
            com.stepup.android.data.repo.Crew(id = "c3", monogram = "WK", name = "주말 한 바퀴", tagline = "", area = "",
                memberCount = 12, roster = emptyList(), owned = false, joined = true),
        )
        fun member(id: String, who: String, ready: Boolean, me: Boolean = false, host: Boolean = false) =
            com.stepup.android.data.repo.PartyMember(id, who, ready = ready, isMe = me, isHost = host)
        val members = listOf(
            member("h", "준호", ready = true, host = true), member("me", "도윤", ready = false, me = true),
            member("a", "윤호", ready = false), member("b", "혜진", ready = false),
        )
        val lobby = com.stepup.android.data.repo.PartyState(
            phase = com.stepup.android.data.repo.PartyPhase.LOBBY, partyId = 1L, crewId = "c1", crewName = name, members = members,
        )
        val hosting = lobby.copy(members = listOf(
            member("me", "준호", ready = true, me = true, host = true), member("a", "도윤", ready = true),
            member("b", "윤호", ready = false), member("c", "혜진", ready = false),
        ))
        val alone = lobby.copy(members = listOf(member("me", "도윤", ready = true, me = true, host = true)))
        try {
            var scene by mutableStateOf<(@Composable () -> Unit)?>(null)
            edgeToEdge()
            compose.setContent { DeviceFrame { Box(Modifier.fillMaxSize()) { scene?.invoke() } } }
            fun show(shotName: String, tag: String, content: @Composable () -> Unit) {
                compose.runOnIdle { scene = content }
                awaitTag(tag)
                shot(shotName, settle = 1_500)
            }
            // 장면만 두고 찍지 않는다(시트를 연 뒤 찍을 때)
            fun put(tag: String, content: @Composable () -> Unit) {
                compose.runOnIdle { scene = content }
                awaitTag(tag)
            }
            val entry = @Composable { ui: com.stepup.android.ui.screens.walk.CrewEntryUi ->
                com.stepup.android.ui.screens.walk.CrewRunEntryContent(ui, {}, {}, {}, {}, {}, {}, {}, {})
            }
            val room = @Composable { state: com.stepup.android.data.repo.PartyState ->
                com.stepup.android.ui.screens.community.PartyLobbyContent(
                    state, state.crewName, null, canNotify = true, here = null,
                    onBack = {}, onReady = {}, onCancelReady = {}, onStart = {}, onShare = {}, onKick = {}, onNotify = {},
                    onRetry = {}, onLeave = {}, onDismissResult = {},
                )
            }
            // CR03 자리 · CR14 · CR19 — 입구는 대기실을 열지 않는다(누르기 전)
            show("CR03", "crew-entry") { entry(com.stepup.android.ui.screens.walk.CrewEntryUi.Ready(crews[0], crews)) }
            compose.onNodeWithTag("crew-entry-change").assertIsDisplayed()
            show("CR14", "crew-entry-none") { entry(com.stepup.android.ui.screens.walk.CrewEntryUi.NoCrew) }
            show("CR19", "crew-pick-sheet") {
                entry(com.stepup.android.ui.screens.walk.CrewEntryUi.Ready(crews[0], crews))
                com.stepup.android.ui.screens.walk.CrewPickSheet(crews, "c1", {}, {})
            }
            // CR04 · CR05 · CR06 · CR15 — 같은 대기실, 내 역할 · 준비에 따라 버튼만 바뀐다
            show("CR04", "party-ready") { room(lobby) }
            show("CR05", "party-waiting") { room(lobby.copy(members = members.map { if (it.isMe) it.copy(ready = true) else it })) }
            show("CR06", "party-start") { room(hosting) }
            // 진행자는 "N명과 출발"(준비한 사람 수 — 2026-10-24 듀얼 레일)
            compose.onNodeWithTag("party-start").assertTextContains(label(R.string.crew5_lobby_start, 2))
            show("CR15", "party-alone") { room(alone) }
            // 크루 러닝은 2명 이상 준비해야 출발한다(서버 0071) — 혼자면 출발 버튼이 꺼지고 까닭 한 줄
            compose.onNodeWithTag("party-start").assertIsNotEnabled()
            compose.onNodeWithTag("party-need-two").assertIsDisplayed()
            // CR07 — 준비 전 인원을 두고 출발할지
            show("CR07", "crew-start-confirm") {
                room(hosting)
                com.stepup.android.ui.screens.walk.CrewStartConfirmSheet(2, 2, {}, {})
            }
            // CR17 — 준비 전 확인: 위치 사용과 위치 공유(기본 끔)는 따로
            show("CR17", "crew-ready-check") {
                room(lobby)
                com.stepup.android.ui.screens.walk.CrewReadyCheckSheet(locationAllowed = false, share = false, {}, {}, {})
            }
            compose.onNodeWithTag("crew-ready-check-share").assertIsOff()
            // CR20 — 크루에게 알리기: 보낼 곳 · 내용을 먼저(보내기 전)
            show("CR20", "crew-notify-preview") {
                room(alone)
                com.stepup.android.ui.components.RunSheet(onDismiss = {}) {
                    CrewNotifyBody(name, label(R.string.run_cr_notify_message, name, 1),
                        com.stepup.android.ui.screens.walk.CrewSend.Idle, {}, {})
                }
            }
            // CR08 — 서버가 정한 시각에 모두 함께(자유 러닝과 같은 3 · 2 · 1 · 0 · StepUP! — 앱 맨 위 겹이 서버 시각에 맞춰 그린다)
            show("CR08", "party-countdown") {
                Box(Modifier.fillMaxSize().testTag("party-countdown")) {
                    com.stepup.android.ui.screens.walk.FreeRunStartStage(clock = { 400 }, revealSince = { null }, reduced = false)
                }
            }
            // CR21 — 크루에 기록 공유(경로는 처음에 꺼져 있다)
            show("CR21", "crew-share") {
                com.stepup.android.ui.screens.walk.CrewShareContent(
                    name, label(R.string.run_cr_share_message, "1.56", "12:30", "8'01\""),
                    includeRoute = false, hasRoute = true, preview = null, state = com.stepup.android.ui.screens.walk.CrewSend.Idle,
                    onRoute = {}, onShare = {}, onClose = {}, monogram = "YD",
                )
            }
            compose.onNodeWithTag("crew-share-route-toggle").assertIsOff()
            // 미리보기는 하나 — 보낼 글 그대로(경로를 끄면 그림이 없다)
            compose.onNodeWithTag("crew-share-text", useUnmergedTree = true)
                .assertTextEquals(label(R.string.run_cr_share_message, "1.56", "12:30", "8'01\""))
            compose.onAllNodesWithTag("crew-share-image", useUnmergedTree = true).assertCountEquals(0)

            // ── 2026-10-16 크루 전달본 검수 장면 ──
            // 대기실 — 3×2 그리드는 6명까지, 더 많으면 다섯 + "+N"(전체 명단). 긴 이름은 한 줄에서 줄인다
            val crowd = lobby.copy(members = listOf(
                member("h", "준호", ready = true, host = true), member("me", "도윤", ready = true, me = true),
                member("a", "윤호", ready = true), member("b", "혜진", ready = false),
                member("c", "알렉산드라 크리스티나 김", ready = true), member("d", "서윤", ready = false),
                member("e", "민지", ready = true), member("f", "지훈", ready = false),
            ))
            put("party-more") { room(crowd) }
            // 준비 칩은 같은 명단에서 센다(8명 중 5명). 내가 준비를 마쳤으면 "준비 완료"가 없다
            compose.onNodeWithTag("party-ready-count", useUnmergedTree = true).assertTextEquals(label(R.string.crew5_lobby_ready_chip, 5, 8))
            compose.onAllNodesWithTag("party-ready", useUnmergedTree = true).assertCountEquals(0)
            compose.onNodeWithTag("party-waiting").assertExists()
            shot("CR04-many", settle = 1_500)
            tap("party-more")
            awaitTag("party-roster-list")
            shot("CR04-roster")
            // 함께 달린 인원 기준(서버 규칙)은 열어 볼 때만
            put("party-rule-open") { room(lobby) }
            tap("party-rule-open")
            awaitTag("party-rule-sheet")
            shot("CR04-rule")

            // 결과 — 보상 네 상태(예상 · 확인 중은 CR13). 화면 부품에는 지도가 없어 "경로 없음" 자리도 함께 본다
            val resultUi = com.stepup.android.ui.screens.walk.CrewResultUi(
                crewName = name, monogram = "YD", started = 4, distance = "1.56", time = "12:30", pace = "8'01\"", note = null, basis = null,
            )
            val result = @Composable { reward: com.stepup.android.ui.screens.walk.ResultReward ->
                com.stepup.android.ui.screens.walk.CrewRunResult(
                    resultUi, reward, map = null, onReason = {}, onHistory = {}, onShare = {}, shareLabel = label(R.string.run_crew_share),
                    onDone = {}, onRecords = {}, onDelete = {},
                )
            }
            val pending = com.stepup.android.ui.screens.walk.ResultReward(
                label(R.string.result_estimated_reward), "+0.80", com.stepup.android.ui.screens.walk.Settle.PENDING,
            )
            show("CR13-done", "run-result-settle-done") {
                result(pending.copy(label = label(R.string.result_reward), settle = com.stepup.android.ui.screens.walk.Settle.DONE))
            }
            show("CR13-void", "run-result-void-reason") {
                result(com.stepup.android.ui.screens.walk.ResultReward(label(R.string.result_reward), "0",
                    com.stepup.android.ui.screens.walk.Settle.VOID, listOf(label(R.string.run_void_body))))
            }
            compose.onAllNodesWithTag("run-result-reward", useUnmergedTree = true).assertCountEquals(0)
            show("CR13-unpaid", "run-result-history") {
                result(com.stepup.android.ui.screens.walk.ResultReward(label(R.string.result_reward), "0",
                    com.stepup.android.ui.screens.walk.Settle.NOT_PAID, listOf(label(R.string.finish_no_reward))))
            }
            show("CR13-sign-in", "run-result-reward-note") {
                result(pending.copy(notes = listOf(label(R.string.finish_sign_in_needed)), signIn = true))
            }
            compose.onNodeWithTag("run-result-no-route", useUnmergedTree = true).assertExists()
            // 늘 붙던 보상 설명은 누를 때만(시트)
            tap("run-result-reward-open")
            awaitTag("run-result-reward-sheet")
            shot("CR13-reward-sheet")

            // 공유 — 보내는 중 · 보냄(크루 채팅 보기) · 실패(같은 내용으로 다시) · 경로 없음(켤 수 없고 까닭)
            val shareMessage = label(R.string.run_cr_share_message, "1.56", "12:30", "8'01\"")
            val share = @Composable { state: com.stepup.android.ui.screens.walk.CrewSend, hasRoute: Boolean ->
                com.stepup.android.ui.screens.walk.CrewShareContent(
                    name, shareMessage, includeRoute = false, hasRoute = hasRoute, preview = null, state = state,
                    onRoute = {}, onShare = {}, onClose = {}, monogram = "YD", onOpenChat = {},
                )
            }
            show("CR21-sending", "crew-share-send") { share(com.stepup.android.ui.screens.walk.CrewSend.Sending, true) }
            show("CR21-sent", "crew-share-sent") { share(com.stepup.android.ui.screens.walk.CrewSend.Sent, true) }
            compose.onNodeWithTag("crew-share-chat").assertExists()
            show("CR21-failed", "crew-share-failed") { share(com.stepup.android.ui.screens.walk.CrewSend.Failed, true) }
            show("CR21-no-route", "crew-share-route") { share(com.stepup.android.ui.screens.walk.CrewSend.Idle, false) }
            compose.onNodeWithTag("crew-share-route-toggle").assertIsNotEnabled()

            // 작은 화면 · 큰 글씨 · 밝은 테마 — 대기실 · 결과 · 공유의 아래 행동이 화면 안
            for (v in listOf(
                Viewport(360, 640), Viewport(320, 640), Viewport(390, 844, font = 1.3f), Viewport(390, 844, mode = ThemeMode.LIGHT),
            )) {
                viewport = v
                show("CR04-${v.label}", "party-ready") { room(lobby) }
                assertInFrame("party-ready", "CR04-${v.label}")
                show("CR13-${v.label}", "run-result-done") { result(pending) }
                assertInFrame("run-result-share", "CR13-${v.label}")
                assertInFrame("run-result-done", "CR13-${v.label}")
                show("CR21-${v.label}", "crew-share-send") { share(com.stepup.android.ui.screens.walk.CrewSend.Idle, true) }
                assertInFrame("crew-share-send", "CR21-${v.label}")
            }
            viewport = Viewport(390, 844)

            // CR09 · CR10 · CR11 · CR12 · CR18 · CR13 — 실제 앱에서 같이 달리는 중(표시용 러닝 · 방)
            val now = System.currentTimeMillis()
            val here = GeoPoint(37.5181, 126.9518)
            val together = lobby.copy(
                phase = com.stepup.android.data.repo.PartyPhase.RUNNING,
                members = listOf(
                    member("me", "도윤", ready = true, me = true).copy(sharing = true, km = 1.56, point = here),
                    member("h", "준호", ready = true, host = true).copy(sharing = true, km = 1.62, point = GeoPoint(37.5196, 126.9490)),
                    member("a", "지연", ready = true),
                    member("b", "혜진", ready = true).copy(sharing = true, km = 1.30, point = GeoPoint(37.5222, 126.9431)),
                ),
            )
            // 대기실에서 출발한 러닝 — 그 크루 · 이름이 러닝에 적혀 있다(출발할 때)
            val crewRun = running(now).copy(
                partySize = 4, group = com.stepup.android.service.RunGroup.CREW, groupCrewId = "c1", groupName = name,
            )
            RunPlans.set(RunPlan.Free)
            repo.showPartyForTest(together)
            WalkSessionService.showStateForTest(crewRun)
            compose.runOnIdle { scene = { MainScaffold() } }
            tap("home-start-run")
            awaitTag("run-together-row")
            compose.onNodeWithTag("crew-badge", useUnmergedTree = true).assertExists()
            compose.onNodeWithTag("crew-header-title", useUnmergedTree = true).assertTextEquals(name)
            // 참여 인원만 적는다 — "모두 달리는 중"이라 하지 않는다
            compose.onNodeWithTag("crew-header-sub", useUnmergedTree = true).assertTextEquals(label(R.string.run_cr_joined, 4))
            assertRunFits("CR09")
            shot("CR09", settle = 2_500)
            // 예상 보상 · 내구도는 정상 화면에 두지 않고 러닝 정보(E03)에서 본다
            tap("run-details-open")
            awaitTag("run-details")
            compose.onNodeWithTag("run-details-estimate", useUnmergedTree = true).assertExists()
            shot("CR09-details")
            tap("run-details-close")
            awaitGone("run-details")
            tap("run-together-row")
            awaitTag("run-together-list")
            // 위치를 공유하지 않는 사람은 거리 대신 "비공개"
            compose.onNodeWithTag("run-together-km-a", useUnmergedTree = true).assertTextEquals(label(R.string.run_together_hidden))
            shot("CR10")
            tap("run-together-close")
            awaitGone("run-together-list")
            WalkSessionService.showStateForTest(crewRun.copy(isPaused = true))
            awaitTag("run-crew-others")
            shot("CR11")
            tap("run-finish")
            awaitTag("run-end-dialog")
            // 참가자가 마치면 방에서 나온다 — 진행자 안내(방이 닫힌다)가 아니다
            awaitTag("run-crew-end-member")
            shot("CR12")
            back()
            awaitGone("run-end-dialog")
            WalkSessionService.showStateForTest(crewRun)
            repo.showPartyForTest(together.copy(liveOffline = true))
            awaitTag("run-crew-offline")
            // 연결이 끊긴 동안 옛 명단 · 위치를 보이지 않는다
            compose.onAllNodesWithTag("run-together-row", useUnmergedTree = true).assertCountEquals(0)
            shot("CR18")
            // 진행자가 마쳐 방이 닫혔다 — 남은 사람의 러닝은 이어지고, 옛 명단 · 위치는 보이지 않는다
            repo.showPartyForTest(together.copy(roomClosed = true))
            awaitTag("run-crew-closed")
            compose.onAllNodesWithTag("run-together-row", useUnmergedTree = true).assertCountEquals(0)
            shot("CR-closed")
            // 앱이 다시 켜져 방 상태가 비었다(되살린 러닝) — 여전히 크루 달리기(자유 러닝 v2 가 아니다)
            repo.showPartyForTest(com.stepup.android.data.repo.PartyState())
            WalkSessionService.showStateForTest(crewRun.copy(isPaused = true))
            awaitTag("run-crew-detached")
            compose.onNodeWithTag("crew-badge", useUnmergedTree = true).assertExists()
            compose.onNodeWithTag("crew-header-title", useUnmergedTree = true).assertTextEquals(name)
            shot("CR-restored")
            // 좁은 화면 · 큰 글씨에서도 일시정지 · 종료가 화면 안
            repo.showPartyForTest(together)
            WalkSessionService.showStateForTest(crewRun)
            for (v in listOf(Viewport(360, 640), Viewport(320, 640), Viewport(390, 844, font = 1.3f))) {
                viewport = v
                awaitTag("home-start-run")
                tap("home-start-run")
                awaitTag("run-together-row")
                assertRunFits("CR09-${v.label}")
                shot("CR09-${v.label}", settle = 1_500)
            }
            viewport = Viewport(390, 844)
            awaitTag("home-start-run")
            tap("home-start-run")
            awaitTag("run-together-row")
            // 번개러닝은 크루 달리기가 아니다 — 예전 화면, 이름표 "번개러닝"(크루 달리기 배지 없음)
            val flashRoom = together.copy(crewId = null, flashPostId = 101L, crewName = "한강 번개")
            repo.showPartyForTest(flashRoom)
            WalkSessionService.showStateForTest(
                running(now).copy(partySize = 3, group = com.stepup.android.service.RunGroup.FLASH, groupName = "한강 번개"),
            )
            awaitGone("crew-badge")
            compose.onAllNodesWithText(label(R.string.post_cat_flash), useUnmergedTree = true).fetchSemanticsNodes().let {
                assertTrue("flash run is labelled as a flash run", it.isNotEmpty())
            }
            compose.onAllNodesWithText(label(R.string.run_crew_title), useUnmergedTree = true).assertCountEquals(0)
            shot("CR-flash")
            // CR13 — 내 크루 러닝 저장 완료: 함께 출발한 인원 · 크루에 기록 공유. 방이 끝난 뒤에도 출발할 때 적은 크루로
            repo.showPartyForTest(together.copy(phase = com.stepup.android.data.repo.PartyPhase.FINISHED, resultStartedAt = now - 750_000))
            val crewDone = finished(now).copy(
                lastPartySize = 4, lastGroup = com.stepup.android.service.RunGroup.CREW, lastGroupCrewId = "c1", lastGroupName = name,
                lastGroupSize = 4,
            )
            WalkSessionService.showStateForTest(crewDone)
            awaitTag("run-result-crew")
            signedIn()
            compose.onNodeWithTag("run-result-share").assertTextContains(label(R.string.run_crew_share))
            // 크루 이름 · 함께 출발한 인원(출발 명단) — 큰 완료 제목 · 체크는 두지 않는다
            compose.onNodeWithTag("run-result-crew-name", useUnmergedTree = true).assertTextEquals(name)
            compose.onNodeWithTag("run-result-crew", useUnmergedTree = true).assertTextEquals(label(R.string.run_crew_started_n, 4))
            compose.onAllNodesWithTag("run-result-title", useUnmergedTree = true).assertCountEquals(0)
            // 저장 표시는 한 번
            compose.onAllNodesWithTag("run-result-saved", useUnmergedTree = true).assertCountEquals(1)
            // 보상은 서버가 확인하기 전 "예상 · 확인 중"
            compose.onNodeWithTag("run-result-settle-pending", useUnmergedTree = true).assertExists()
            assertResultFits("CR13")
            shot("CR13")
            tap("run-result-share")
            awaitTag("crew-share")
            compose.onNodeWithTag("crew-share-route-toggle").assertIsOff()
            shot("CR21-app")
            // 경로를 켜면 실제로 만든 경로 그림(보내는 그림과 같은 것)이 미리보기에 함께
            tap("crew-share-route-toggle")
            awaitTag("crew-share-image", timeout = 20_000)
            shot("CR21-app-route")
            back()
            awaitGone("crew-share")
            // 혼자 출발한 크루 달리기(정산 인원 1) — 결과도 크루 달리기, 그 크루에 기록 공유
            repo.showPartyForTest(com.stepup.android.data.repo.PartyState())
            WalkSessionService.showStateForTest(crewDone.copy(lastPartySize = 1, lastGroupSize = 1))
            awaitTag("run-result-crew")
            compose.onNodeWithTag("run-result-crew", useUnmergedTree = true).assertTextEquals(label(R.string.run_crew_started_solo))
            compose.onNodeWithTag("run-result-share").assertTextContains(label(R.string.run_crew_share))
            shot("CR13-solo")
            // 기록 저장 중 · 저장 실패 — 크루 화면 그대로(저장 중에는 버튼 하나, 실패하면 같은 러닝으로 다시 저장)
            repo.showPartyForTest(together)
            WalkSessionService.showStateForTest(crewRun.copy(isPaused = true, saveStatus = RunSaveStatus.SAVING))
            awaitTag("run-live")
            awaitGone("run-result")
            compose.onAllNodesWithTag("run-finish", useUnmergedTree = true).assertCountEquals(0)
            shot("CR-saving")
            WalkSessionService.showStateForTest(crewRun.copy(isPaused = true, saveStatus = RunSaveStatus.FAILED))
            awaitTag("run-save-error")
            shot("CR-save-failed")
        } finally {
            repo.showPartyForTest(com.stepup.android.data.repo.PartyState())
            WalkSessionService.showStateForTest(WalkSessionState())
            restore()
        }
    }

    /** 등급 배지 여섯 — 채운 색 · 흰 글자(어두운 · 밝은 바탕), 긴 이름은 마지막 줄 끝에 */
    @Test fun badges() {
        edgeToEdge()
        compose.setContent {
            Column(Modifier.fillMaxSize().systemBarsPadding()) {
                for (mode in listOf(ThemeMode.DARK, ThemeMode.LIGHT)) {
                    StepUpTheme(mode) {
                        val face = if (mode == ThemeMode.DARK) Color(0xFF041C33) else Color(0xFFF2F5FA)
                        val ink = if (mode == ThemeMode.DARK) Color.White else Color(0xFF0E1A33)
                        Column(
                            Modifier.fillMaxWidth().background(face).padding(20.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp),
                        ) {
                            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                                ShoeTier.entries.forEach { ShoeGradeBadge(it) }
                            }
                            ShoeNameWithBadge(
                                name = "스플릿 힐 로드 러너", tier = ShoeTier.RARE,
                                style = TextStyle(color = ink, fontSize = 16.sp, fontWeight = FontWeight.Bold),
                                modifier = Modifier.width(300.dp).testTag("badge-sample-${mode.name.lowercase()}"),
                            )
                            ShoeNameWithBadge(
                                name = "카본 마라톤 트레이닝 슈 프로 에디션 레드라인", tier = ShoeTier.REDLINE,
                                style = TextStyle(color = ink, fontSize = 22.sp, fontWeight = FontWeight.Bold),
                                maxLines = 2, modifier = Modifier.width(260.dp),
                            )
                            Text(if (mode == ThemeMode.DARK) "dark" else "light", color = ink.copy(alpha = 0.5f), fontSize = 11.sp)
                        }
                    }
                }
            }
        }
        awaitTag("tier-badge-finish")
        ShoeTier.entries.forEach { tier ->
            val badge = compose.onAllNodesWithTag("tier-badge-${tier.key}", useUnmergedTree = true).fetchSemanticsNodes().first()
            val heightDp = badge.boundsInRoot.height / compose.density.density
            assertTrue("badge ${tier.key} is 24–28dp high: $heightDp", heightDp in 23.5f..28.5f)
        }
        shot("j04-badges")
    }

    /**
     * C — 크루 러닝 3번 듀얼 레일(2026-10-24 핸드오프, 서버 0072). 크루 홈(01) · 열린 대기실(02) · 러닝 만들기(03 · 06) · 코스(04 · 05 · 09) ·
     * 대기실(10 ~ 13 · 16 · 22 · 23, 사진 터널 — 모션 줄이기라 정지 사진) · 시트(14 · 15 · 21 · 24 ~ 34 · 40) · 출발(35 · 36) ·
     * 예외(61 · 68 · 69 · 70)를 화면 부품에 예시 값을 넣어 찍는다(서버에 보내지 않는다). 캡처 이름은 장면 번호(C10-host-lobby …).
     * 터널은 대기실에만 있고(lobby-tunnel) 홈 · 목록 · 만들기 · 코스에는 없는지도 본다.
     */
    @Test fun crewRooms() {
        seed()
        val name = "여의도 퇴근런"
        val crew = com.stepup.android.data.repo.Crew(id = "c1", monogram = "YD", name = "퇴근런", tagline = "함께 달릴수록 강해지는 우리",
            area = "여의도", memberCount = 30, roster = emptyList(), owned = false, joined = true)
        val repo = ServiceLocator.courseRepository
        runBlocking { repo.ensureSeeded() }
        val courses = runBlocking { repo.courses.first().filter { it.hasTrack } }
        val roomCourse = com.stepup.android.domain.RoomCourse.of(courses.first()).copy(rev = 2)
        val now = System.currentTimeMillis()
        val day = java.time.LocalDate.now(com.stepup.android.domain.CrewHomeRules.KST)
        val pool = com.stepup.android.domain.CrewPoolSnap(day, total = 1200.0, remaining = 720.0, consumed = 480.0, contributors = 12,
            activeRunners = 0, exhausted = false, low = false, version = 1, asOfMs = now)
        val mine = com.stepup.android.domain.CrewRunMine(usedToday = false, state = null, crewId = null, crewName = null, partyId = null,
            sessionId = null, startedAtMs = null, terminalReason = null, contribution = 0.0, consumed = 0.0, distanceM = null, totalSup = null,
            nextEligibleAtMs = null)
        val today = com.stepup.android.domain.CrewRunToday(crewId = "c1", day = day, serverNowMs = now, resetsAtMs = now + 3_600_000,
            pool = pool, mine = mine, crewLevel = 8, runKm = 859.0, nextLevelKm = 900.0, staminaBonus = 0.42, rewardBonus = 0.07,
            levelEvent = null)
        fun server(e: Int, c: Int, max: Int) = com.stepup.android.domain.ServerStats(origin = "DRAW", efficiencyBps = e, comfortBps = c,
            durabilityPts = 80.0, maxLevel = 20, status = "OWNED", chainState = "APP", canWithdraw = true, upgradeCost = 0.0,
            repairCostPerPoint = 0.0, genesisNo = 0, durabilityNow = max * 8 / 10, durabilityMax = max)
        val shoeA = com.stepup.android.domain.Sneaker(id = 9_501, faction = com.stepup.android.domain.Faction.WIND,
            rarity = com.stepup.android.domain.Rarity.RARE, variant = 0, level = 3, mintNumber = 1201, luck = 1.0, comfort = 1.0, durability = 80,
            equipped = true, acquiredAt = now, server = server(1600, 1200, 100), modelId = 1201)
        val shoeB = shoeA.copy(id = 9_502, mintNumber = 1248, equipped = false, server = server(1200, 1800, 120), modelId = 1107)
        val shoes = listOf(shoeA, shoeB)
        fun member(id: String, who: String, ready: Boolean, me: Boolean = false, host: Boolean = false) =
            com.stepup.android.data.repo.PartyMember(id, who, ready = ready, isMe = me, isHost = host)
        val six = listOf(
            member("me", "나", ready = true, me = true, host = true), member("a", "도윤", ready = true), member("b", "지연", ready = true),
            member("c", "혜진", ready = true), member("d", "민수", ready = true), member("e", "서연", ready = true),
        )
        val hostLobby = com.stepup.android.data.repo.PartyState(
            phase = com.stepup.android.data.repo.PartyPhase.LOBBY, partyId = 1L, crewId = "c1", crewName = crew.name, members = six,
            pickedRoom = 1L, roomName = name, roomCreatedAt = now - 600_000, course = roomCourse, courseRev = 2, courseSeenRev = 2,
        )
        val memberLobby = hostLobby.copy(members = listOf(
            member("h", "도윤", ready = true, host = true), member("me", "나", ready = false, me = true), member("b", "지연", ready = true),
            member("c", "혜진", ready = true), member("d", "민수", ready = true), member("e", "서연", ready = true),
        ))
        fun crewRoom(ready: Int, total: Int) = com.stepup.android.domain.CrewRunRoom(
            partyId = 1L, crewId = "c1", status = "LOBBY", capacity = 20, crewLevel = 8, staminaBonus = 0.42, rewardBonus = 0.07,
            membersCount = total, readyCount = ready, bonusCount = ready, bonusPct = com.stepup.android.domain.CrewRunMath.headcountBonus(ready),
            nextBonusPct = com.stepup.android.domain.CrewRunMath.headcountBonus(ready + 1), myContribution = 142.0,
            pool = pool,
            members = listOf(com.stepup.android.domain.CrewRoomMember(userId = "me", name = "나", isMe = true, isHost = true, ready = true,
                crewMember = true, ranToday = false, sneakerId = shoeA.id, rarity = "RARE", shoeLevel = 3, modelId = 1201,
                durMax = 100, durNow = 80)),
        )
        try {
            var scene by mutableStateOf<(@Composable () -> Unit)?>(null)
            edgeToEdge()
            compose.setContent { DeviceFrame { Box(Modifier.fillMaxSize()) { scene?.invoke() } } }
            fun show(shotName: String, tag: String, content: @Composable () -> Unit) {
                compose.runOnIdle { scene = content }
                awaitTag(tag)
                shot(shotName, settle = 1_500)
            }
            fun put(tag: String, content: @Composable () -> Unit) {
                compose.runOnIdle { scene = content }
                awaitTag(tag)
            }
            val lobby = @Composable { state: com.stepup.android.data.repo.PartyState, room: com.stepup.android.domain.CrewRunRoom? ->
                com.stepup.android.ui.screens.community.PartyLobbyContent(
                    state, state.crewName, null, canNotify = true, here = null,
                    onBack = {}, onReady = {}, onCancelReady = {}, onStart = {}, onShare = {}, onKick = {}, onNotify = {},
                    onRetry = {}, onLeave = {}, onDismissResult = {}, crewToday = today, room = room, shoes = shoes,
                )
            }
            // 01 크루 홈 — 오늘 크루 내구도 · 다음 목표(듀얼 레일) · 진행 중인 대기실 · 크루 러닝 기록 · 크루 러닝 시작. 사진 터널 없음
            show("C01-home", "crew-entry") {
                com.stepup.android.ui.screens.walk.CrewRunEntryContent(
                    com.stepup.android.ui.screens.walk.CrewEntryUi.Ready(crew, listOf(crew)), {}, {}, {}, {}, {}, {}, {}, {},
                    today = today, roomsCount = 1,
                )
            }
            compose.onNodeWithTag("crew-home-pool-value", useUnmergedTree = true).assertTextEquals("720")
            compose.onAllNodesWithTag("lobby-tunnel", useUnmergedTree = true).assertCountEquals(0)
            // 크루원에게는 크루 관리 줄이 없다 · 크루장에게는 있다(기다리는 가입 신청 수와 함께 — 2026-10-24 "크루장이 할 수 있는 탭")
            compose.onAllNodesWithTag("crew-home-manage", useUnmergedTree = true).assertCountEquals(0)
            show("C01-home-owner", "crew-home-manage") {
                com.stepup.android.ui.screens.walk.CrewRunEntryContent(
                    com.stepup.android.ui.screens.walk.CrewEntryUi.Ready(crew.copy(owned = true, pendingCount = 2), listOf(crew)),
                    {}, {}, {}, {}, {}, {}, {}, {},
                    today = today, roomsCount = 1,
                )
            }
            compose.onNodeWithTag("crew-home-manage").assertTextContains(label(R.string.crew_pending_badge, 2), substring = true)
            // 02 열린 대기실 — 내 방 · 남의 방 · 가득 찬 방. 비었을 때 · 못 불러왔을 때 · 오늘 이미 했을 때
            val rooms = com.stepup.android.domain.CrewRooms(
                rooms = listOf(
                    com.stepup.android.domain.CrewRoom(1, name, "LOBBY", "h", "도윤", now - 600_000, 6, 5, 20, roomCourse, mine = false, kicked = false),
                    com.stepup.android.domain.CrewRoom(2, "아침 한 바퀴", "LOBBY", "x", "지훈", now - 300_000, 20, 12, 20, null, mine = false, kicked = false),
                ),
                capacity = 20, doneToday = false,
            )
            show("C02-room-list", "crew-rooms") {
                com.stepup.android.ui.screens.walk.CrewRoomsContent(com.stepup.android.ui.screens.walk.RoomsUi.Ready(rooms), crew.name, {}, {}, {}, {})
            }
            compose.onAllNodesWithTag("lobby-tunnel", useUnmergedTree = true).assertCountEquals(0)
            compose.onNodeWithTag("crew-rooms-new").assertIsEnabled()
            // 코스 없는 방(0073) — 지도 대신 "자유 코스 · 각자 원하는 길로"
            compose.onNodeWithTag("crew-room-free", useUnmergedTree = true).assertExists()
            show("C02-room-list-empty", "crew-rooms-empty") {
                com.stepup.android.ui.screens.walk.CrewRoomsContent(
                    com.stepup.android.ui.screens.walk.RoomsUi.Ready(rooms.copy(rooms = emptyList())), crew.name, {}, {}, {}, {},
                )
            }
            show("C02-room-list-failed", "crew-rooms-failed") {
                com.stepup.android.ui.screens.walk.CrewRoomsContent(com.stepup.android.ui.screens.walk.RoomsUi.Failed(false), crew.name, {}, {}, {}, {})
            }
            show("C61-used", "crew-rooms-done-today") {
                com.stepup.android.ui.screens.walk.CrewRoomsContent(
                    com.stepup.android.ui.screens.walk.RoomsUi.Ready(rooms.copy(doneToday = true)), crew.name, {}, {}, {}, {},
                )
            }
            compose.onNodeWithTag("crew-rooms-new").assertIsNotEnabled()
            // 03 · 06 러닝 만들기 — 이름이 없으면 열 수 없다. 코스는 골라도 되고 안 골라도 된다(0073 — 자유 코스). 고른 뒤 이름 · 코스 · 내 기여
            show("C03-create", "crew-create") {
                createForm("", null, null)
            }
            compose.onNodeWithTag("crew-create-open").assertIsNotEnabled()
            show("C03-create-free", "crew-create-free") {
                createForm(name, null, null)
            }
            compose.onNodeWithTag("crew-create-open").assertIsEnabled()
            show("C06-create-selected", "crew-create-selected") {
                createForm(name, roomCourse, null)
            }
            compose.onNodeWithTag("crew-create-open").assertIsEnabled()
            compose.onNodeWithTag("crew-create-change-course").assertExists()
            // 2026-10-26 원격 뛰기 — 만나는 곳 대신 "각자의 현재 위치에서 시작"
            show("C06-create-remote", "crew-create-selected") {
                createForm(name, roomCourse, null, com.stepup.android.domain.RoomMode.REMOTE)
            }
            compose.onNodeWithTag("crew-create-start-place").assertExists()
            compose.onNodeWithTag("crew-create-meet").assertDoesNotExist()
            show("C06-create-failed", "crew-create-done-today") {
                createForm(name, roomCourse, com.stepup.android.ui.screens.walk.CreateProblem.DoneToday)
            }
            // 04 · 09 코스 고르기 · 05 상세(지도 — "이 코스로 달리기"를 눌러야 고른다)
            show("C04-courses", "crew-courses") {
                com.stepup.android.ui.screens.walk.CrewCoursePickerContent(null, mine = emptyList(), shared = courses,
                    sync = com.stepup.android.data.repo.BoardSyncState.Ready, onBack = {}, onOpen = {}, onRetry = {})
            }
            // 맨 위 지도에서 직접 그리기(2026-10-24 "코스를 내가 선택할 수가 없네") — 저장한 코스가 없어도 바로 만든다
            compose.onNodeWithTag("crew-courses-draw").assertExists()
            // 열린 길(끝 ≠ 출발) — 기본 코스는 한 바퀴라 끝이 출발과 같아 "출발점으로 잇기"가 꺼진다(Experience QA 40082364533)
            val origin = courses.first().points.first()
            val drawnPoints = listOf(
                origin,
                com.stepup.android.domain.GeoPoint(origin.lat + 0.0020, origin.lng + 0.0005),
                com.stepup.android.domain.GeoPoint(origin.lat + 0.0032, origin.lng + 0.0030),
                com.stepup.android.domain.GeoPoint(origin.lat + 0.0015, origin.lng + 0.0048),
                com.stepup.android.domain.GeoPoint(origin.lat - 0.0004, origin.lng + 0.0036),
            )
            show("C04-draw", "crew-draw") {
                com.stepup.android.ui.screens.walk.CrewCourseDrawContent(
                    points = emptyList(), here = drawnPoints.first(), center = drawnPoints.first(),
                    onAdd = {}, onUndo = {}, onLoop = {}, onClear = {}, onBack = {}, onUse = {},
                )
            }
            compose.onNodeWithTag("crew-draw-use").assertIsNotEnabled()
            show("C04-draw-drawing", "crew-draw-drawing") {
                com.stepup.android.ui.screens.walk.CrewCourseDrawContent(
                    points = drawnPoints, here = drawnPoints.first(), center = drawnPoints.first(),
                    onAdd = {}, onUndo = {}, onLoop = {}, onClear = {}, onBack = {}, onUse = {},
                )
            }
            compose.onNodeWithTag("crew-draw-use").assertIsEnabled()
            compose.onNodeWithTag("crew-draw-loop").assertIsEnabled()
            // 지도를 누르면 그 자리에 점이 하나 더 — 실제 지도 위 누름이 좌표로 바뀐다. 되돌리기는 마지막 점만 뺀다
            var tapped by mutableStateOf(drawnPoints.take(2))
            put("crew-draw-drawing") {
                com.stepup.android.ui.screens.walk.CrewCourseDrawContent(
                    points = tapped, here = null, center = drawnPoints.first(),
                    onAdd = { tapped = tapped + it }, onUndo = { tapped = tapped.dropLast(1) }, onLoop = {}, onClear = {}, onBack = {}, onUse = {},
                )
            }
            compose.onNodeWithTag("crew-draw-map").performTouchInput { click(center) }
            compose.waitUntil(5_000) { tapped.size == 3 }
            tap("crew-draw-undo")
            compose.waitUntil(5_000) { tapped.size == 2 }
            show("C09-course-empty", "crew-courses-empty") {
                com.stepup.android.ui.screens.walk.CrewCoursePickerContent(null, mine = emptyList(), shared = emptyList(),
                    sync = com.stepup.android.data.repo.BoardSyncState.Failed("offline"), onBack = {}, onOpen = {}, onRetry = {})
            }
            show("C05-course-detail", "crew-course-detail") {
                com.stepup.android.ui.screens.walk.CrewCoursePickerContent(null, mine = emptyList(), shared = courses,
                    sync = com.stepup.android.data.repo.BoardSyncState.Ready, onBack = {}, onOpen = {}, onRetry = {})
                com.stepup.android.ui.screens.walk.CrewCourseDetailSheet(roomCourse, courses.first().area, onPick = {}, onDismiss = {})
            }
            // 10 · 11 · 12 · 13 진행자 대기실 — 사진 터널은 대기실에만, 크루원 · 내 신발은 같은 듀얼 레일
            show("C10-host-lobby", "party-start") { lobby(hostLobby, crewRoom(6, 6)) }
            assertShoeVisible("C10-host-lobby")
            compose.onNodeWithTag("lobby-tunnel", useUnmergedTree = true).assertExists()
            compose.onNodeWithTag("party-start").assertTextContains(label(R.string.crew5_lobby_start, 6))
            compose.onNodeWithTag("party-lobby-title", useUnmergedTree = true).assertTextEquals(name)
            val waiting = hostLobby.copy(members = six.mapIndexed { i, m -> if (i == 5) m.copy(ready = false) else m })
            show("C11-host-waiting", "party-start") { lobby(waiting, crewRoom(5, 6)) }
            show("C12-solo", "party-alone") { lobby(hostLobby.copy(members = six.take(1)), crewRoom(1, 1)) }
            assertShoeVisible("C12-solo")
            compose.onNodeWithTag("party-start").assertIsNotEnabled()
            val twenty = six + (1..14).map { member("m$it", "크루$it", ready = it % 3 != 0) }
            show("C13-full", "party-more") { lobby(hostLobby.copy(members = twenty), crewRoom(twenty.count { it.ready }, 20)) }
            // 14 참가자 관리 · 15 내보내기 확인 · 21 진행자 나가기
            put("party-menu") { lobby(hostLobby, crewRoom(6, 6)) }
            tap("party-menu")
            tap("party-menu-roster")
            awaitTag("party-roster-list")
            shot("C14-host-roster")
            tap("party-kick-a")
            awaitTag("party-kick-sheet")
            shot("C15-kick")
            show("C16-removed-host", "party-start") { lobby(hostLobby.copy(members = six.filterNot { it.id == "a" }), crewRoom(5, 5)) }
            put("party-menu") { lobby(hostLobby, crewRoom(6, 6)) }
            tap("party-menu")
            tap("party-menu-leave")
            awaitTag("party-leave-host")
            shot("C21-host-leave")
            // 07 진행자 코스 바꾸기 · 33 출발 확인 · 34 준비된 인원만
            put("party-course") { lobby(hostLobby, crewRoom(6, 6)) }
            tap("party-course")
            awaitTag("party-course-change")
            shot("C07-course-change")
            // 진행자는 코스를 뺄 수도 있다(0073 — 자유 코스로)
            compose.onNodeWithTag("party-course-change-remove", useUnmergedTree = true).assertExists()
            // 코스 없는 방(자유 코스) — 진행자는 코스 칸을 눌러 코스를 정하고, 참가자는 보기만
            show("C10-free-host", "party-course-free") { lobby(hostLobby.copy(course = null, courseRev = 0, courseSeenRev = 0), crewRoom(6, 6)) }
            compose.onNodeWithTag("party-course-set", useUnmergedTree = true).assertExists()
            assertShoeVisible("C10-free-host")
            show("C22-free-member", "party-course-free") { lobby(memberLobby.copy(course = null, courseRev = 0, courseSeenRev = 0), crewRoom(5, 6)) }
            compose.onAllNodesWithTag("party-course-set", useUnmergedTree = true).assertCountEquals(0)
            put("party-start") { lobby(hostLobby, crewRoom(6, 6)) }
            tap("party-start")
            awaitTag("party-start-confirm")
            shot("C33-start-confirm")
            put("party-start") { lobby(waiting, crewRoom(5, 6)) }
            tap("party-start")
            awaitTag("party-partial-start")
            shot("C34-partial-start")
            // 22 · 23 참가자 — 출발 · 내보내기 · 코스 바꾸기 없음, 준비 완료 → 진행자 대기 + 준비 취소
            show("C22-member-lobby", "party-ready") { lobby(memberLobby, crewRoom(5, 6)) }
            compose.onAllNodesWithTag("party-start", useUnmergedTree = true).assertCountEquals(0)
            compose.onAllNodesWithTag("party-menu", useUnmergedTree = true).assertCountEquals(0)
            show("C23-member-ready", "party-waiting") {
                lobby(memberLobby.copy(members = memberLobby.members.map { if (it.isMe) it.copy(ready = true) else it }), crewRoom(6, 6))
            }
            compose.onNodeWithTag("party-waiting").assertIsNotEnabled()
            assertShoeVisible("C23-member-ready")
            // 24 신발 · 25 코스 보기 · 26 내구도 · 27 목표 · 28 레벨 · 30 규칙 · 29 보너스 · 31 함께 달릴 크루원 · 32 참가자 나가기
            put("party-shoe-change") { lobby(memberLobby, crewRoom(5, 6)) }
            tap("party-shoe-change")
            awaitTag("party-shoe-sheet")
            shot("C24-shoes")
            put("party-course") { lobby(memberLobby, crewRoom(5, 6)) }
            tap("party-course")
            awaitTag("party-course-view")
            shot("C25-course-view")
            put("party-pool") { lobby(memberLobby, crewRoom(5, 6)) }
            tap("party-pool")
            awaitTag("party-stamina-sheet")
            shot("C26-stamina")
            put("party-goal") { lobby(memberLobby, crewRoom(5, 6)) }
            tap("party-goal")
            awaitTag("party-goal-sheet")
            shot("C27-goal")
            tap("party-goal-levels")
            awaitTag("party-levels-sheet")
            shot("C28-levels")
            put("party-rule-open") { lobby(memberLobby, crewRoom(5, 6)) }
            tap("party-rule-open")
            awaitTag("party-rule-sheet")
            shot("C30-rules")
            tap("party-rule-bonus")
            awaitTag("party-bonus-sheet")
            shot("C29-bonus")
            put("party-counts") { lobby(memberLobby, crewRoom(5, 6)) }
            tap("party-counts")
            awaitTag("party-roster-list")
            compose.onAllNodesWithTag("party-kick-h", useUnmergedTree = true).assertCountEquals(0)
            shot("C31-member-roster")
            put("party-ready") { lobby(memberLobby, crewRoom(5, 6)) }
            back()
            awaitTag("party-leave-member")
            shot("C32-member-leave")
            // 08 코스가 바뀌었다(참가자 — 본 판보다 높은 판)
            show("C08-course-updated", "party-course-updated") {
                lobby(memberLobby.copy(course = roomCourse.copy(rev = 3), courseRev = 3, courseSeenRev = 2), crewRoom(5, 6))
            }
            // 진행자가 코스를 뺐다(0073) — 자유 코스로 바뀐 것만 알린다
            show("C08-course-removed", "party-course-removed") {
                lobby(memberLobby.copy(course = null, courseRev = 3, courseSeenRev = 2), crewRoom(5, 6))
            }
            // 40 인증 전 안내는 없다(2026-10-26 — 휴대폰 인증을 쓰지 않는다)
            // 35 출발 신호 맞추는 중 · 36 3 · 2 · 1 · 0 · StepUP!(자유 러닝과 같은 무대)
            show("C35-starting", "party-starting-stage") { com.stepup.android.ui.screens.walk.CrewStartingStage() }
            show("C36-countdown", "run-start-sequence") {
                com.stepup.android.ui.screens.walk.FreeRunStartStage(clock = { 1_400 }, revealSince = { null }, reduced = false)
            }
            // 68 불러오는 중(정지 터널) · 69 불러오기 실패 · 70 나옴 · 출발함 · 찼음
            show("C68-loading", "party-loading") { lobby(hostLobby.copy(partyId = null, members = emptyList()), null) }
            compose.onNodeWithTag("lobby-tunnel-still", useUnmergedTree = true).assertExists()
            show("C69-load-failed", "party-terminal-failed") {
                lobby(hostLobby.copy(partyId = null, problem = com.stepup.android.data.repo.PartyProblem.NETWORK), null)
            }
            show("C70-removed", "party-terminal-removed") {
                lobby(hostLobby.copy(partyId = null, problem = com.stepup.android.data.repo.PartyProblem.REMOVED), null)
            }
            show("C70-started", "party-terminal-started") {
                lobby(hostLobby.copy(partyId = null, problem = com.stepup.android.data.repo.PartyProblem.STARTED), null)
            }
            show("C70-full", "party-terminal-full") {
                lobby(hostLobby.copy(partyId = null, problem = com.stepup.android.data.repo.PartyProblem.FULL), null)
            }
            // 작은 화면 · 큰 글씨 · 밝은 테마 — 대기실 주 버튼 · 목록 · 만들기 버튼이 화면 안
            for (v in listOf(Viewport(360, 640), Viewport(320, 640), Viewport(390, 844, font = 1.3f), Viewport(390, 844, mode = ThemeMode.LIGHT))) {
                viewport = v
                show("C10-${v.label}", "party-start") { lobby(hostLobby, crewRoom(6, 6)) }
                assertInFrame("party-start", "C10-${v.label}")
                show("C22-${v.label}", "party-ready") { lobby(memberLobby, crewRoom(5, 6)) }
                assertInFrame("party-ready", "C22-${v.label}")
                show("C02-${v.label}", "crew-rooms") {
                    com.stepup.android.ui.screens.walk.CrewRoomsContent(com.stepup.android.ui.screens.walk.RoomsUi.Ready(rooms), crew.name, {}, {}, {}, {})
                }
                assertInFrame("crew-rooms-new", "C02-${v.label}")
                show("C06-${v.label}", "crew-create-selected") {
                    createForm(name, roomCourse, null)
                }
                assertInFrame("crew-create-open", "C06-${v.label}")
                show("C01-${v.label}", "crew-entry") {
                    com.stepup.android.ui.screens.walk.CrewRunEntryContent(
                        com.stepup.android.ui.screens.walk.CrewEntryUi.Ready(crew, listOf(crew)), {}, {}, {}, {}, {}, {}, {}, {},
                        today = today, roomsCount = 1,
                    )
                }
                assertInFrame("crew-entry-enter", "C01-${v.label}")
            }
            viewport = Viewport(390, 844)
        } finally {
            restore()
        }
    }

    /**
     * 대기실 사진 터널의 생명주기(2026-10-24 T66) — 모션을 켜고 시계를 손으로 넘긴다(배경이 끝없이 돈다).
     * 대기실만 보이면 사진 · 빛이 흐르고(C10-motion-1 ~ 3), 시트가 뜨면 그 자리에 멈추고(C08-paused-1 · 2),
     * 모션 줄이기면 정지 사진이다(C10-reduced-1 · 2). 화면 위쪽 띠(상태 표시줄 아래 ~ 시트 위)의 픽셀을 맞대어 본다.
     *
     * 기본 화면 갤러리(tools/run_screen_gallery.sh)에서는 돌리지 않는다 — CI 에뮬레이터는 GPU 를 소프트웨어로 흉내 내(-gpu swiftshader)
     * 움직이는 배경 한 장에 0.7초가 걸렸고, 오래 돈 에뮬레이터가 이 검사 중에 멈춰 뒤 검사까지 잃었다(Experience QA 40069908782).
     * 실제 기기나 GPU 가 있는 에뮬레이터에서 이 검사만 따로 돌린다:
     * ./gradlew :app:connectedDebugAndroidTest -Pandroid.testInstrumentationRunnerArguments.class=com.stepup.android.RunJourneyDesignTest#crewLobbyMotion
     */
    @Test fun crewLobbyMotion() {
        seed()
        var reducedMotion by mutableStateOf(false)
        var scene by mutableStateOf<(@Composable () -> Unit)?>(null)
        try {
            edgeToEdge()
            compose.mainClock.autoAdvance = false
            compose.setContent {
                DeviceFrame {
                    CompositionLocalProvider(
                        com.stepup.android.ui.experience.LocalMotion provides
                            com.stepup.android.ui.experience.MotionPreferences(reduced = reducedMotion, active = true),
                    ) {
                        Box(Modifier.fillMaxSize()) { scene?.invoke() }
                    }
                }
            }
            // 참가자 자리 — 진행자가 코스를 바꾸면(판이 오르면) 코스 바뀜 시트가 저절로 뜬다(08)
            val members = listOf(
                com.stepup.android.data.repo.PartyMember("h", "도윤", ready = true, isMe = false, isHost = true),
                com.stepup.android.data.repo.PartyMember("me", "나", ready = false, isMe = true, isHost = false),
                com.stepup.android.data.repo.PartyMember("b", "지연", ready = true, isMe = false, isHost = false),
            )
            val course = com.stepup.android.domain.RoomCourse(rev = 2, name = "여의도 한강 5K", km = 5.0)
            val party = com.stepup.android.data.repo.PartyState(
                phase = com.stepup.android.data.repo.PartyPhase.LOBBY, partyId = 1L, crewId = "c1", crewName = "퇴근런", members = members,
                pickedRoom = 1L, roomName = "여의도 퇴근런", roomCreatedAt = System.currentTimeMillis() - 600_000, course = course, courseRev = 2,
                courseSeenRev = 2,
            )
            fun lobby(state: com.stepup.android.data.repo.PartyState): @Composable () -> Unit = {
                com.stepup.android.ui.screens.community.PartyLobbyContent(
                    state, state.crewName, null, canNotify = false, here = null,
                    onBack = {}, onReady = {}, onCancelReady = {}, onStart = {}, onShare = {}, onKick = {}, onNotify = {},
                    onRetry = {}, onLeave = {}, onDismissResult = {},
                )
            }
            compose.runOnUiThread { scene = lobby(party) }
            advanceUntil("party-pool")
            compose.onNodeWithTag("lobby-tunnel", useUnmergedTree = true).assertExists()
            // 대기실만 보일 때 — 1.5초마다 사진 · 빛이 달라진다
            val running = (1..3).map { i ->
                compose.mainClock.advanceTimeBy(1_500)
                bandOf("C10-motion-$i")
            }
            for (i in 0 until running.lastIndex) {
                val share = changedShare(running[i], running[i + 1])
                check(share > MOVING_SHARE) { "대기실 배경이 흐르지 않는다(C10-motion-${i + 1} → ${i + 2}: 바뀐 픽셀 ${share * 100}%)" }
            }
            // 시트가 뜨면 멈춘다 — 부모는 어둡게, 그 자리에
            compose.runOnUiThread { scene = lobby(party.copy(course = course.copy(rev = 3), courseRev = 3)) }
            advanceUntil("party-course-updated")
            compose.mainClock.advanceTimeBy(1_500)
            // 시트 창이 열리는 시스템 애니메이션(실제 시계) — 앱 화면은 시계를 멈춰 두었으니 그대로다
            Thread.sleep(1_000)
            val paused = bandOf("C08-paused-1")
            compose.mainClock.advanceTimeBy(3_000)
            val pausedLater = bandOf("C08-paused-2")
            val pausedShare = changedShare(paused, pausedLater)
            check(pausedShare < STILL_SHARE) { "시트 아래 대기실 배경이 멈추지 않았다(바뀐 픽셀 ${pausedShare * 100}%)" }
            // 모션 줄이기 — 시트 없이도 정지 사진(이동 · 확대 · 회전 없음)
            compose.runOnUiThread { scene = null }
            compose.mainClock.advanceTimeBy(500)
            Thread.sleep(1_000)
            compose.runOnUiThread {
                reducedMotion = true
                scene = lobby(party)
            }
            advanceUntil("party-pool")
            compose.mainClock.advanceTimeBy(500)
            val reduced = bandOf("C10-reduced-1")
            compose.mainClock.advanceTimeBy(3_000)
            val reducedLater = bandOf("C10-reduced-2")
            val reducedShare = changedShare(reduced, reducedLater)
            check(reducedShare < STILL_SHARE) { "모션 줄이기인데 대기실 배경이 움직였다(바뀐 픽셀 ${reducedShare * 100}%)" }
        } finally {
            compose.mainClock.autoAdvance = true
            restore()
        }
    }

    /** 시계를 멈춰 둔 동안의 기다림 — 시계를 손으로 넘기며 [tag] 가 생길 때까지 */
    private fun advanceUntil(tag: String) {
        repeat(150) {
            if (exists(tag)) return
            compose.mainClock.advanceTimeBy(100)
        }
        check(exists(tag)) { "$tag 이(가) 나오지 않았다" }
    }

    /**
     * 지금 화면을 [name].png 로 남기고 화면 위쪽 띠(높이 7% ~ 17% — 상태 표시줄 아래, 시트 위)의 픽셀을 돌려준다.
     * 시계를 멈춰 두었으니 기다려도 화면은 그대로다 — 창이 열리는 시스템 애니메이션만 기다린다.
     */
    private fun bandOf(name: String): IntArray {
        compose.waitForIdle()
        awaitFrameOnScreen(compose.activity)
        Thread.sleep(400)
        val shot = InstrumentationRegistry.getInstrumentation().uiAutomation.takeScreenshot() ?: error("캡처하지 못했다: $name")
        val bitmap = shot.copy(android.graphics.Bitmap.Config.ARGB_8888, false)
        shot.recycle()
        try {
            File(directory, "$name.png").outputStream().use { stream ->
                check(bitmap.compress(android.graphics.Bitmap.CompressFormat.PNG, 100, stream)) { "캡처를 쓰지 못했다: $name" }
            }
            val top = (bitmap.height * 0.07f).toInt()
            val rows = (bitmap.height * 0.10f).toInt()
            return IntArray(bitmap.width * rows).also { bitmap.getPixels(it, 0, bitmap.width, 0, top, bitmap.width, rows) }
        } finally {
            bitmap.recycle()
        }
    }

    /** 두 띠에서 눈에 띄게(채널 차이 6 넘게) 바뀐 픽셀의 몫 */
    private fun changedShare(a: IntArray, b: IntArray): Double {
        check(a.size == b.size) { "띠 크기가 다르다" }
        var changed = 0
        for (i in a.indices) {
            val x = a[i]
            val y = b[i]
            val d = maxOf(
                kotlin.math.abs(((x shr 16) and 0xFF) - ((y shr 16) and 0xFF)),
                kotlin.math.abs(((x shr 8) and 0xFF) - ((y shr 8) and 0xFF)),
                kotlin.math.abs((x and 0xFF) - (y and 0xFF)),
            )
            if (d > 6) changed++
        }
        return changed.toDouble() / a.size
    }

    // ── 표시용 러닝 ──────────────────────────────────────────────

    /** 12:30 · 1.56km 를 달리는 중 — 마지막 점은 방금(현재 속도를 잰다) */
    private fun running(now: Long, flagged: Int = 0, flaggedAt: Long = 0L) = WalkSessionState(
        isActive = true, steps = 2_100, elapsedSec = 750, startedAt = now - 750_000,
        gpsFix = true, gpsKm = 1.56, validSegments = 120, flaggedSegments = flagged, lastFlaggedAt = flaggedAt,
        track = route(now - 750_000, now - 2_000),
    )

    /** 달리는 중 휴대폰 위치를 껐다 — 경로가 없다(시간 · 걸음만). 시작은 GPS 가 잡혀야 한다 */
    private fun timeOnly(now: Long) = WalkSessionState(
        isActive = true, steps = 2_100, elapsedSec = 750, startedAt = now - 750_000,
        gpsFix = false, locationOn = false,
    )

    /** 방금 끝난 러닝 — 1.56km · 12:30 · 폰이 셈한 예상 0.80 SUP(서버 확인 전). 자유 러닝 결과는 이 보상 값을 보이지 않는다 */
    private fun finished(now: Long) = WalkSessionState(
        lastRewardPoints = 0.8, lastSessionSteps = 2_100, lastRewardedSteps = 2_100,
        lastElapsedSec = 750, lastGpsKm = 1.56, lastStartedAt = now - 750_000,
        track = route(now - 750_000, now - 2_000),
    )

    /**
     * 러닝 챌린지 하나를 고르고 그 러닝을 달리는 중 — 보인 상태를 돌려준다(같은 러닝으로 위치 끊김 · 저장 실패를 찍는다).
     * 계획은 처음 만난 러닝(시작 시각 [now] - 750 000)에 묶인다 — 같은 시작 시각의 결과([finished])도 챌린지 러닝 결과다.
     */
    private fun challenge(goal: RunGoal, now: Long, elapsed: Long, km: Double): WalkSessionState {
        RunPlans.set(RunPlan.Goal(goal))
        val state = WalkSessionState(
            isActive = true, steps = (km * 1_350).toInt(), elapsedSec = elapsed, startedAt = now - 750_000,
            gpsFix = true, gpsKm = km, validSegments = 120, track = route(now - 750_000, now - 2_000),
        )
        WalkSessionService.showStateForTest(state)
        awaitTag("run-goal-bar")
        return state
    }

    /** 다이어트 루틴 — [elapsed] 초째(구간은 운동 시간에서 계산한다) */
    private fun dietRun(startedAt: Long, elapsed: Long) = WalkSessionState(
        isActive = true, steps = (elapsed * 2).toInt(), elapsedSec = elapsed, startedAt = startedAt - 750_000,
        gpsFix = true, gpsKm = elapsed / 900.0 * 1.15, validSegments = 60, track = route(startedAt - 750_000, startedAt - 2_000),
    )

    /** 고른 코스를 08:12 동안 달리는 중 — 코스의 앞쪽 점들을 지나왔다(마지막 점은 방금) */
    private fun onCourse(course: com.stepup.android.domain.RunCourse, now: Long): WalkSessionState {
        val passed = course.points.take((course.points.size * 0.4).toInt().coerceAtLeast(2))
        val start = now - 492_000
        val track = passed.mapIndexed { i, p -> TrackPoint(p.lat, p.lng, start + (now - 2_000 - start) * i / (passed.size - 1).coerceAtLeast(1)) }
        return WalkSessionState(
            isActive = true, steps = 1_300, elapsedSec = 492, startedAt = start, gpsFix = true,
            gpsKm = course.distanceKm * 0.4, validSegments = 60, track = track, here = passed.last(),
            // 정확한 위치로 받은 자리 — 코스 이탈(K05)은 흐린 위치로는 보지 않는다(2026-10-19)
            hereAccurate = true,
        )
    }

    /** 한강 따라 남동쪽으로 — 시각은 [from]..[to] 에 고르게(마지막 몇 점은 10초 간격 · 약 2.2m/s) */
    private fun route(from: Long, to: Long): List<TrackPoint> {
        val path = listOf(
            GeoPoint(37.5283, 126.9326), GeoPoint(37.5269, 126.9349), GeoPoint(37.5251, 126.9371),
            GeoPoint(37.5236, 126.9398), GeoPoint(37.5222, 126.9431), GeoPoint(37.5210, 126.9462),
            GeoPoint(37.5196, 126.9490), GeoPoint(37.5181, 126.9518),
        )
        val body = path.dropLast(1).mapIndexed { i, p -> TrackPoint(p.lat, p.lng, from + (to - 60_000 - from) * i / (path.size - 2)) }
        val last = path.last()
        val tail = (0..5).map { k ->
            TrackPoint(last.lat - 0.00018 * (5 - k), last.lng - 0.00018 * (5 - k), to - 10_000L * (5 - k))
        }
        return body + tail
    }

    /** 서버가 확인한 저장 줄(E11) — 결과 화면은 그 러닝의 시작 시각으로 줄을 찾는다 */
    private fun signedRow(now: Long, amount: String) = runBlocking {
        val startedAt = now - 750_000
        ServiceLocator.database.walkSessionDao().insert(
            WalkSessionEntity(
                startedAt = startedAt, endedAt = now, steps = 2_100, durationSec = 750, distanceMeters = 1_560.0,
                calories = 0.0, pointsEarned = amount.toDouble(), uploadState = "SIGNED", claimAmount = amount,
            ),
        )
    }

    /**
     * 서버 확인을 기다리는 저장 줄(14 오른쪽) — 걸음이 있고 아직 올리지 않은 러닝은 이 휴대폰에서 지우지 않는다(지우면 확인이 끝나지 않는다).
     * 지금 주인(로그인 전 CI 기기는 손님)의 줄이라야 확인을 기다리는 줄이다 — 주인이 정해지기 전(legacy) 줄은 계정 · 손님이 올리지 않아
     * 지울 수 있다. 손님의 올리기는 로그인이 필요해 이 줄을 건드리지 않는다. [dropRow] 로 지운다.
     */
    private fun pendingRow(now: Long) = runBlocking {
        val owner = ServiceLocator.sessionHolder.recordingOwner()
        ServiceLocator.database.walkSessionDao().insert(
            WalkSessionEntity(
                startedAt = now - 750_000, endedAt = now, steps = 2_100, durationSec = 750, distanceMeters = 1_560.0,
                calories = 0.0, pointsEarned = 0.0, uploadState = "PENDING", recordingOwner = owner,
            ),
        )
    }

    private fun dropRow(now: Long) = runBlocking(kotlinx.coroutines.Dispatchers.IO) {
        // 이 검사가 넣은 줄 — 옛 기록(legacy) 줄([signedRow])과 지금 주인의 줄([pendingRow])
        val owner = ServiceLocator.sessionHolder.recordingOwner()
        // Room 거래 안에서 지워야 그 줄을 지켜보는 화면도 지운 것을 안다(밖에서 지우면 다음 쓰기 전까지 옛 값이 남는다)
        ServiceLocator.database.runInTransaction {
            ServiceLocator.database.openHelper.writableDatabase.execSQL(
                "DELETE FROM walk_sessions WHERE startedAt = ? AND recordingOwner IN ('legacy', ?)",
                arrayOf<Any>(now - 750_000, owner),
            )
        }
    }

    // ── 검사 ─────────────────────────────────────────────────────

    /** 러닝 중 — 큰 시간이 버튼 위, 두 버튼이 화면 안 */
    private fun assertRunFits(where: String) {
        compose.waitForIdle()
        val hero = bounds("run-hero-value")
        val action = bounds("run-primary-action")
        val map = bounds("run-map-expand")
        val limit = frameBottom()
        assertTrue("run time above the buttons at $where: $hero / $action", hero.bottom <= action.top)
        assertTrue("pause inside the screen at $where: $action / $limit", action.bottom <= limit + 1)
        assertTrue("map button inside the screen at $where: $map / $limit", map.bottom <= limit + 1)
    }

    /**
     * 러닝 결과 — 완료(처음 화면으로) · 공유가 늘 화면 안. 공유는 완료와 한 줄(자유 러닝)이거나 그 위(다른 러닝)다.
     * 자유 러닝 결과의 기록 보기는 ⋯ 안으로 옮겼다(화면 아래에 없다).
     */
    private fun assertResultFits(where: String) {
        compose.waitForIdle()
        val done = bounds("run-result-done")
        val share = bounds("run-result-share")
        val frame = bounds(FRAME)
        assertTrue("done inside the screen at $where: $done / $frame", done.top >= frame.top - 1 && done.bottom <= frame.bottom + 1)
        assertTrue("share inside the screen at $where: $share / $frame", share.top >= frame.top - 1 && share.bottom <= frame.bottom + 1)
        assertTrue("share above or level with done at $where: $share / $done", share.top <= done.top + 1)
    }

    /** 크루 화면의 아래 행동 하나가 화면(틀) 안에 다 보이는지 — 작은 화면 · 큰 글씨에서 잘리지 않게 */
    /**
     * 대기실의 내 신발 카드가 아래 버튼 칸에 가리지 않는다(390 × 844 — 시안 10 · 12 · 23). 스크롤 안의 카드는 boundsInRoot 가 잘린 만큼만 주므로
     * 자른 적 없는 자리(positionInRoot + 크기)로 본다
     */
    private fun assertShoeVisible(where: String) {
        compose.waitForIdle()
        val shoe = compose.onNodeWithTag("party-shoe", useUnmergedTree = true).fetchSemanticsNode()
        val bottom = shoe.positionInRoot.y + shoe.size.height
        val footer = bounds("party-footer")
        assertTrue("my shoe card above the buttons at $where: card bottom $bottom / buttons top ${footer.top}", bottom <= footer.top + 1)
    }

    private fun assertInFrame(tag: String, where: String) {
        compose.waitForIdle()
        val box = bounds(tag)
        val frame = bounds(FRAME)
        assertTrue("$tag inside the screen at $where: $box / $frame", box.top >= frame.top - 1 && box.bottom <= frame.bottom + 1)
    }

    /**
     * 자유 러닝 중 — 지도가 러닝 화면 높이의 68% 이상(390×844 · 글자 100% 에서만 본다).
     * 러닝 화면은 앱 틀(Scaffold)이 상태 막대 · 탐색 막대 안쪽에 둔다 — 그 높이(run-live)로 나눈다. 틀(844dp) 전체로 나누면
     * 에뮬레이터의 탐색 막대 방식(세 버튼 · 제스처)에 따라 값이 달라져 같은 화면도 붙었다 떨어졌다 한다(틀 기준 값은 글에 함께 적는다).
     */
    private fun assertMapShare(where: String) {
        compose.waitForIdle()
        val map = bounds("run-live-map")
        val distance = bounds("run-distance-value")
        val time = bounds("run-hero-value")
        // 2026-10-26 시안 active — 지도가 위를 채우고 숫자(거리 · 시간)는 그 아래 판에
        assertTrue("map has room at $where: $map", map.height >= 240f)
        assertTrue("map sits above the numbers at $where: $distance / $map / $time",
            map.bottom <= distance.top + 1 && map.bottom <= time.top + 1)
    }

    /** 믿을 수 없는 지금 속도 · 페이스는 "—"(속도 측정 오류 · 측정 무효) */
    private fun assertUnknownSpeed() {
        compose.onNodeWithTag("run-speed-value", useUnmergedTree = true).assertTextEquals("—")
        compose.onNodeWithTag("run-pace-value", useUnmergedTree = true).assertTextEquals("—")
    }

    /**
     * 자유 러닝 화면에 보상 글이 없다 — [root] 안의 글과 화면 낭독 글 모두에서 SUP 와, 예전 화면의 보상 · 적립 · 정산 안내 글을 찾는다.
     * 앱 언어는 규칙(appLanguage)이 한국어로 두지만, 낱말을 적지 않고 앱의 글(label)로 찾는다.
     */
    private fun assertNoRewardText(root: String) {
        val inside = hasAnyAncestor(hasTestTag(root))
        val words = listOf("SUP") + listOf(
            R.string.run_live_void_title, R.string.run_void_banner, R.string.run_estimate_title, R.string.run_reward_note,
            R.string.run_reward_void, R.string.run_reward_void_note, R.string.run_reward_not_paid, R.string.run_reward_done_note,
        ).map { label(it) }
        for (word in words) {
            compose.onAllNodes((hasText(word, substring = true) or hasContentDescription(word, substring = true)) and inside, useUnmergedTree = true)
                .assertCountEquals(0)
        }
    }

    /** 자유 러닝 결과에 없는 칸 — 보상 · 정산 · 신발 칸, 예전 결과의 안내 줄과 넘기는 판. SUP 글도 없다 */
    private fun assertNoRewardResult() {
        for (tag in FREE_RESULT_ABSENT) compose.onAllNodesWithTag(tag, useUnmergedTree = true).assertCountEquals(0)
        assertNoRewardText("run-result")
    }

    /**
     * 자유 러닝 결과(1.56km · 12:30) — 보상 상태와 상관없이 같은 모습: 지도 · 러닝 완료 · 기록 저장 완료 · 기록 판(GPS 거리 · 내구도) ·
     * 완료 | 공유. 덜 쓰는 기록 보기 · 삭제는 ⋯ 안에만 있다.
     */
    private fun assertFreeResult() {
        compose.onNodeWithTag("run-result-map", useUnmergedTree = true).assertExists()
        compose.onNodeWithTag("run-result-title", useUnmergedTree = true).assertTextEquals(label(R.string.finish_title))
        compose.onNodeWithTag("run-result-saved", useUnmergedTree = true).assertTextEquals(label(R.string.run_free_record_saved))
        compose.onNodeWithTag("run-result-time", useUnmergedTree = true).assertTextEquals("12:30")
        // 평균 페이스 12:30 ÷ 1.56km = 8분 00초/km · 평균 속도 7.5km/h — GPS 거리로만 셈한다
        compose.onNodeWithTag("run-result-pace", useUnmergedTree = true).assertTextEquals("8′00″")
        compose.onNodeWithTag("run-result-distance", useUnmergedTree = true).assertTextEquals("1.56")
        compose.onNodeWithTag("run-result-speed", useUnmergedTree = true).assertTextEquals("7.5")
        compose.onNodeWithTag("run-result-durability", useUnmergedTree = true).assertTextEquals("92")
        compose.onNodeWithTag("run-result-done").assertTextContains(label(R.string.finish_done))
        compose.onNodeWithTag("run-result-share").assertTextContains(label(R.string.run_share_record))
        compose.onNodeWithTag("run-result-more").assertExists()
        compose.onAllNodesWithTag("run-result-records", useUnmergedTree = true).assertCountEquals(0)
        compose.onAllNodesWithTag("run-result-delete", useUnmergedTree = true).assertCountEquals(0)
        assertNoRewardResult()
    }

    /** 앱 셸을 [viewport] 크기(dp) · 글자 배율 · 테마로 — 에뮬레이터 창 안에 그 크기의 화면을 만든다 */
    @Composable private fun DeviceFrame(content: @Composable () -> Unit) {
        BoxWithConstraints(Modifier.fillMaxSize().background(Color.Black), contentAlignment = Alignment.TopCenter) {
            val v = viewport
            val px = minOf(constraints.maxHeight / v.height.toFloat(), constraints.maxWidth / v.width.toFloat())
            CompositionLocalProvider(androidx.compose.ui.platform.LocalDensity provides Density(px, v.font)) {
                StepUpTheme(v.mode) {
                    ExperienceProvider {
                        Box(Modifier.requiredSize(v.width.dp, v.height.dp).testTag(FRAME)) {
                            key(v) { content() }
                        }
                    }
                }
            }
        }
    }

    /** 신고 있는 신발을 레어 새 도감 신발(1107 · 내구도 92)로 — 끝나면 원래 신발로 되돌리고 지운다 */
    private fun seed() = runBlocking {
        syncBefore = ServiceLocator.economySync.state.value
        // 자유 러닝 화면을 본다 — 앞 테스트가 남긴 코스 선택 · 코스 녹화 · 이벤트 챌린지가 있으면 예전 화면이 된다
        ServiceLocator.courseRepository.clearSelection()
        ServiceLocator.courseRepository.cancelRecording()
        com.stepup.android.ui.screens.events.ChallengeRunFocus.clear()
        ServiceLocator.userPrefs.setReducedMotion(true)
        ServiceLocator.userPrefs.setSounds(false)
        ServiceLocator.userPrefs.setHaptics(false)
        ServiceLocator.userPrefs.setGuideSeen()
        ServiceLocator.sneakerRepository.ensureStarter()
        ServiceLocator.stepRepository.simulateSteps((6_840 - ServiceLocator.stepRepository.todaySteps.value).coerceAtLeast(0))
        val dao = ServiceLocator.database.sneakerDao()
        val base = requireNotNull(dao.equippedNow() ?: dao.allNow().firstOrNull())
        originalEquipped = base.id
        val id = dao.insert(
            base.copy(
                id = 0, rarity = requireNotNull(ShoeCatalog.of(1107)).rarity.id, variant = 0, level = 3,
                mintNumber = dao.maxMintNumber() + 1, equipped = false, acquiredAt = System.currentTimeMillis(),
                serverId = 0, origin = "", modelId = 1107, tokenId = 0, durability = 92, durabilityPts = 92.0,
            ),
        )
        added = id
        ServiceLocator.sneakerRepository.equip(id)
    }

    private fun restore() = runBlocking {
        val dao = ServiceLocator.database.sneakerDao()
        originalEquipped?.let { ServiceLocator.sneakerRepository.equip(it) }
        added?.let { id -> dao.byId(id)?.let { dao.delete(it) } }
    }

    private fun label(id: Int): String = compose.activity.getString(id)

    private fun label(id: Int, vararg args: Any): String = compose.activity.getString(id, *args)

    /**
     * CI 에뮬레이터는 로그인 전이라 러닝 완료에 "다시 로그인"이 붙는다 — 시안(로그인한 사람의 정산 대기)을 보려고 동기화 표시만 바꾼다.
     * 뒤에서 도는 동기화가 되돌려 놓을 수 있어 까닭 줄이 사라질 때까지 다시 둔다.
     */
    private fun signedIn() {
        eventually {
            ServiceLocator.economySync.showStateForTest(EconomySyncState.SYNCED)
            compose.waitForIdle()
            compose.onAllNodesWithTag("run-result-reward-note", useUnmergedTree = true).assertCountEquals(0)
        }
    }

    private fun frameBottom(): Float = bounds(FRAME).bottom

    private fun bounds(tag: String): Rect = compose.onNodeWithTag(tag, useUnmergedTree = true).fetchSemanticsNode().boundsInRoot

    private fun exists(tag: String): Boolean = compose.onAllNodesWithTag(tag, useUnmergedTree = true).fetchSemanticsNodes().isNotEmpty()

    private fun awaitTag(tag: String, timeout: Long = 10_000) = compose.waitUntil(timeout) { exists(tag) }

    private fun awaitGone(tag: String, timeout: Long = 10_000) = compose.waitUntil(timeout) { !exists(tag) }

    private fun eventually(timeout: Long = 10_000, check: () -> Unit) {
        compose.waitUntil(timeout) { runCatching(check).isSuccess }
        check()
    }

    private fun tap(tag: String) {
        awaitTag(tag)
        compose.onNodeWithTag(tag).performClick()
        compose.waitForIdle()
    }

    /** 키보드를 닫고 다 닫힐 때까지 기다린다 */
    private fun hideKeyboard() {
        compose.runOnUiThread {
            androidx.core.view.WindowInsetsControllerCompat(compose.activity.window, compose.activity.window.decorView)
                .hide(androidx.core.view.WindowInsetsCompat.Type.ime())
        }
        compose.waitUntil(5_000) {
            androidx.core.view.ViewCompat.getRootWindowInsets(compose.activity.window.decorView)
                ?.isVisible(androidx.core.view.WindowInsetsCompat.Type.ime()) != true
        }
    }

    private fun back() {
        InstrumentationRegistry.getInstrumentation().sendKeyDownUpSync(android.view.KeyEvent.KEYCODE_BACK)
        compose.waitForIdle()
    }

    private fun edgeToEdge() {
        compose.activityRule.scenario.onActivity {
            it.enableEdgeToEdge(
                statusBarStyle = SystemBarStyle.dark(android.graphics.Color.TRANSPARENT),
                navigationBarStyle = SystemBarStyle.dark(android.graphics.Color.TRANSPARENT),
            )
        }
    }

    private fun shot(name: String, settle: Long = 900) {
        compose.waitForIdle()
        InstrumentationRegistry.getInstrumentation().waitForIdleSync()
        Thread.sleep(settle)
        captureDisplay(File(directory, "$name.png"))
    }

    private companion object {
        const val FRAME = "run-frame"

        /** 대기실 배경이 흐른다 — 1.5초 뒤 위쪽 띠에서 바뀐 픽셀이 이보다 많다(사진 두 겹의 확대 · 투명도 · 옆 빛) */
        const val MOVING_SHARE = 0.02

        /** 대기실 배경이 멈췄다 — 바뀐 픽셀이 이보다 적다 */
        const val STILL_SHARE = 0.002

        /** 자유 러닝 결과에는 없다(다른 러닝 결과에만) — 보상 칸 · 정산 표시 · 신발 칸 · 안내 줄 · 넘기는 판 */
        val FREE_RESULT_ABSENT = listOf(
            "run-result-reward", "run-result-reward-card", "run-result-reward-label", "run-result-reward-note",
            "run-result-settle-pending", "run-result-settle-done", "run-result-settle-void", "run-result-settle-rejected",
            "run-result-void-reason", "run-result-shoe", "run-result-note", "run-result-basis", "run-result-message",
            "run-result-scroll",
        )
    }
}
