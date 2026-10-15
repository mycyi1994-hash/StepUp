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

    @Test fun screens() {
        seed()
        try {
            edgeToEdge()
            compose.setContent { DeviceFrame { MainScaffold() } }

            // HOME · E01 — 러닝 홈(전시장 · 신발 줄 · 러닝 시작 · 오늘 걸음)과 오늘의 활동 시트
            awaitTag("home-start-run")
            awaitTag("home-shoe-name")
            // 시안 HOME — 이름 아래 줄에 등급 배지 · Lv(이름 글 안이 아니다)
            compose.onNode(hasTestTag("tier-badge-rare") and hasAnyAncestor(hasTestTag("home-shoe-info")), useUnmergedTree = true)
                .assertExists()
            compose.onNodeWithTag("home-stat-durability-value", useUnmergedTree = true).assertTextContains("92", substring = true)
            shot("HOME")
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
            // 내구도는 신고 있는 신발(seed — 내구도 92)
            compose.onNodeWithTag("run-durability-value", useUnmergedTree = true).assertTextEquals("92")
            compose.onNode(hasText(label(R.string.run_free_chip_running)) and hasAnyAncestor(hasTestTag("run-status")), useUnmergedTree = true)
                .assertExists()
            compose.onNodeWithTag("run-primary-action").assertIsDisplayed().assertTextContains(label(R.string.run_pause))
            compose.onNodeWithTag("run-finish").assertIsDisplayed().assertTextContains(label(R.string.run_free_stop))
            compose.onAllNodesWithTag("run-details-reward", useUnmergedTree = true).assertCountEquals(0)
            assertNoRewardText("run-live")
            assertRunFits("390x844")
            // 이 검사는 390×844 · 글자 100%(viewport 기본값)에서 돈다 — 지도가 러닝 화면 높이의 68% 이상
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

            // R02_TIME — 위치 없이 기록(거리 · 페이스는 재지 않는다)
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
            eventually { compose.onNodeWithTag("run-primary-action").assertTextContains(label(R.string.run_free_resume)) }
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

            // R04 · R07 — 러닝을 마칠까요?(저장하고 종료 · 계속 달리기 · 저장하지 않고 종료) · 저장하지 않고 종료할까요?(뒤 화면은 멈춘 채)
            tap("run-finish")
            awaitTag("run-end-dialog")
            compose.onNodeWithTag("run-end-line", useUnmergedTree = true).assertTextContains("12:30", substring = true)
            compose.onNodeWithTag("run-end-save").assertTextContains(label(R.string.run_free_save_finish))
            compose.onNodeWithTag("run-end-continue").assertTextContains(label(R.string.run_continue))
            compose.onNodeWithTag("run-end-discard").assertTextContains(label(R.string.run_free_discard))
            // 자유 러닝의 마칠까요 시트에는 닫기(X)가 없다 — 바깥 · 뒤로 가기로 닫는다
            compose.onAllNodesWithTag("run-sheet-close", useUnmergedTree = true).assertCountEquals(0)
            shot("R04")
            tap("run-end-discard")
            awaitTag("run-discard-dialog")
            compose.onNodeWithTag("run-discard-confirm").assertTextContains(label(R.string.run_free_discard_confirm))
            compose.onNodeWithTag("run-discard-back").assertTextContains(label(R.string.runflow_discard_back))
            shot("R07")
            tap("run-discard-back")
            awaitGone("run-discard-dialog")
            // 돌아가기를 누르면 마칠까요(R04)로 돌아간다 — 뒤로 가기로 닫으면 멈춘 러닝
            awaitTag("run-end-dialog")
            back()
            awaitGone("run-end-dialog")

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
            compose.onNodeWithTag("run-result-durability", useUnmergedTree = true).assertTextEquals("92")
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
            runBlocking { ServiceLocator.userPrefs.clearGoalAttempts() }
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
            // D03 — 적던 중 나가려 하면 한 번 묻는다
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

    /** 앱에서 잠깐만 지나가거나 서버 없이 만들 수 없는 상태 — 같은 화면 부품을 그 상태로 그려 찍는다(D04 · D05 · R01 · E07 · L03 · K01 · K17 · K18 · U04 · K02) */
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
            // E07 — 휴대폰 위치 기능 꺼짐(앱 권한은 있음). 위치가 없으니 지도를 그리지 않는다
            show("E07", "run-location-off-gate") {
                com.stepup.android.ui.screens.walk.RunLocationOffContent(label(R.string.runflow_free), {}, {}, {})
            }
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
            compose.onNodeWithTag("party-start").assertTextContains(label(R.string.run_crew_start_n, 2))
            // 크루 대기실은 크루 이름 머리와 정확히 "크루 달리기" 배지
            compose.onNodeWithTag("crew-badge", useUnmergedTree = true).assertExists()
            show("CR15", "party-alone") { room(alone) }
            compose.onNodeWithTag("party-start").assertTextContains(label(R.string.run_cr_start_alone))
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
            // CR08 — 서버가 정한 시각에 모두 함께
            show("CR08", "party-countdown") {
                room(hosting.copy(phase = com.stepup.android.data.repo.PartyPhase.COUNTDOWN, countdown = 3))
            }
            // CR21 — 크루에 기록 공유(경로는 처음에 꺼져 있다)
            show("CR21", "crew-share") {
                com.stepup.android.ui.screens.walk.CrewShareContent(
                    name, "1.56", "12:30", "8'01\"", label(R.string.run_cr_share_message, "1.56", "12:30", "8'01\""),
                    includeRoute = false, hasRoute = true, preview = null, state = com.stepup.android.ui.screens.walk.CrewSend.Idle,
                    onRoute = {}, onShare = {}, onClose = {},
                )
            }
            compose.onNodeWithTag("crew-share-route-toggle").assertIsOff()

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
            compose.onNodeWithTag("run-result-crew", useUnmergedTree = true).assertTextEquals(label(R.string.run_crew_result_line, name, 4))
            // 보상은 서버가 확인하기 전 "예상 · 확인 중"
            compose.onNodeWithTag("run-result-settle-pending", useUnmergedTree = true).assertExists()
            assertResultFits("CR13")
            shot("CR13")
            tap("run-result-share")
            awaitTag("crew-share")
            compose.onNodeWithTag("crew-share-route-toggle").assertIsOff()
            shot("CR21-app")
            back()
            awaitGone("crew-share")
            // 혼자 출발한 크루 달리기(정산 인원 1) — 결과도 크루 달리기, 그 크루에 기록 공유
            repo.showPartyForTest(com.stepup.android.data.repo.PartyState())
            WalkSessionService.showStateForTest(crewDone.copy(lastPartySize = 1, lastGroupSize = 1))
            awaitTag("run-result-crew")
            compose.onNodeWithTag("run-result-title", useUnmergedTree = true).assertTextEquals(label(R.string.run_crew_result_solo))
            compose.onNodeWithTag("run-result-share").assertTextContains(label(R.string.run_crew_share))
            shot("CR13-solo")
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

    // ── 표시용 러닝 ──────────────────────────────────────────────

    /** 12:30 · 1.56km 를 달리는 중 — 마지막 점은 방금(현재 속도를 잰다) */
    private fun running(now: Long, flagged: Int = 0, flaggedAt: Long = 0L) = WalkSessionState(
        isActive = true, steps = 2_100, elapsedSec = 750, startedAt = now - 750_000,
        gpsFix = true, gpsKm = 1.56, validSegments = 120, flaggedSegments = flagged, lastFlaggedAt = flaggedAt,
        track = route(now - 750_000, now - 2_000),
    )

    /** 위치 없이 기록 중 — 휴대폰 위치가 꺼져 있고 경로가 없다(시간 · 걸음만) */
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
        val finish = bounds("run-finish")
        val limit = frameBottom()
        assertTrue("run time above the buttons at $where: $hero / $action", hero.bottom <= action.top)
        assertTrue("pause inside the screen at $where: $action / $limit", action.bottom <= limit + 1)
        assertTrue("finish inside the screen at $where: $finish / $limit", finish.bottom <= limit + 1)
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

    /**
     * 자유 러닝 중 — 지도가 러닝 화면 높이의 68% 이상(390×844 · 글자 100% 에서만 본다).
     * 러닝 화면은 앱 틀(Scaffold)이 상태 막대 · 탐색 막대 안쪽에 둔다 — 그 높이(run-live)로 나눈다. 틀(844dp) 전체로 나누면
     * 에뮬레이터의 탐색 막대 방식(세 버튼 · 제스처)에 따라 값이 달라져 같은 화면도 붙었다 떨어졌다 한다(틀 기준 값은 글에 함께 적는다).
     */
    private fun assertMapShare(where: String) {
        compose.waitForIdle()
        val map = bounds("run-live-map")
        val screen = bounds("run-live")
        val frame = bounds(FRAME)
        val share = map.height / screen.height
        assertTrue(
            "map is at least 68% of the run screen at $where: $share (map $map / screen $screen; of the frame ${map.height / frame.height})",
            share >= 0.68f,
        )
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
        compose.onNodeWithTag("run-result-share").assertTextContains(label(R.string.run_free_share))
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

        /** 자유 러닝 결과에는 없다(다른 러닝 결과에만) — 보상 칸 · 정산 표시 · 신발 칸 · 안내 줄 · 넘기는 판 */
        val FREE_RESULT_ABSENT = listOf(
            "run-result-reward", "run-result-reward-card", "run-result-reward-label", "run-result-reward-note",
            "run-result-settle-pending", "run-result-settle-done", "run-result-settle-void", "run-result-settle-rejected",
            "run-result-void-reason", "run-result-shoe", "run-result-note", "run-result-basis", "run-result-message",
            "run-result-scroll",
        )
    }
}
