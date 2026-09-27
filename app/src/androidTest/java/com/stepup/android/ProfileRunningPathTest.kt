package com.stepup.android

import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.requiredWidth
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import androidx.test.platform.app.InstrumentationRegistry
import com.stepup.android.core.ServiceLocator
import com.stepup.android.data.local.WalkSessionEntity
import com.stepup.android.ui.MainScaffold
import com.stepup.android.ui.Screen
import com.stepup.android.ui.components.S2Stage
import com.stepup.android.ui.experience.ExperienceProvider
import com.stepup.android.ui.screens.profile.ProfileHome
import com.stepup.android.ui.screens.profile.ProfileViewModel
import com.stepup.android.ui.screens.profile.RunRecordState
import com.stepup.android.ui.theme.StepUpDesign
import com.stepup.android.ui.theme.StepUpTheme
import com.stepup.android.ui.theme.ThemeMode
import java.io.File
import java.util.Locale
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

/**
 * 내 정보 첫 화면 — 러닝 패스(2026-09-27). 실제 앱 셸에서 버튼마다 이동을 확인하고, 카드 상태를 찍는다.
 *
 * 거리 · 횟수는 앱 DB 의 러닝 세션 합계다. 첫 테스트는 세션 8개(합 34.2km)를 잠깐 넣었다가 끝에 지운다
 * (업로드되지 않게 REJECTED 로 넣는다). 같은 에뮬레이터에서 앞서 돈 기기 테스트가 남긴 세션이 있을 수 있어
 * 넣기 전 합계에 더한 값을 기대한다. 상태 테스트는 같은 화면 부품에 상태만 바꿔 넣는다.
 */
class ProfileRunningPathTest {
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

    private val directory get() = File(compose.activity.getExternalFilesDir(null), "profile-running-path").apply { mkdirs() }

    @Test fun runningPathOpensEveryDestination() {
        val before = runBlocking {
            ServiceLocator.userPrefs.setReducedMotion(true)
            ServiceLocator.userPrefs.setSounds(false)
            ServiceLocator.userPrefs.setHaptics(false)
            ServiceLocator.userPrefs.setGuideSeen()
            val totals = ServiceLocator.stepRepository.observeRunTotals().first()
            val dao = ServiceLocator.database.walkSessionDao()
            // 8회 · 34.2km — 시안과 같은 크기의 기록으로 실제 집계 경로를 탄다
            RUNS.forEachIndexed { i, meters ->
                dao.insert(WalkSessionEntity(startedAt = MARKER + i * 60_000L, endedAt = MARKER + i * 60_000L + 1_800_000L,
                    steps = 4000, durationSec = 1800, distanceMeters = meters, calories = 0.0, pointsEarned = 0.0,
                    uploadState = "REJECTED", verdict = "VOID"))
            }
            totals
        }
        try {
            edgeToEdge()
            compose.setContent {
                StepUpTheme(ThemeMode.DARK) { ExperienceProvider { MainScaffold(initialTab = Screen.Profile) } }
            }
            val context = compose.activity
            val runs = before.runs + RUNS.size
            val km = String.format(Locale.ROOT, "%,.1f", (before.meters + RUNS.sum()) / 1000)
            awaitTag("profile-record-card")
            compose.waitUntil(10_000) {
                runCatching { compose.onNodeWithTag("profile-distance", useUnmergedTree = true).assertTextEquals(km) }.isSuccess
            }
            compose.onNodeWithTag("profile-runs", useUnmergedTree = true)
                .assertTextEquals(context.resources.getQuantityString(R.plurals.me_path_runs, runs, runs))
            if (before.runs == 0) {
                // 앞선 기록이 없으면 시안과 같은 값이다
                compose.onNodeWithTag("profile-distance", useUnmergedTree = true).assertTextEquals("34.2")
                compose.onNodeWithTag("profile-runs", useUnmergedTree = true).assertTextEquals("8번의 러닝을 기록했어요.")
            }
            // 순서: 프로필 → 카드 → 기록 보기 → 챌린지 · 지갑 · 설정
            val order = listOf("profile-edit", "profile-record-card", "profile-records", "profile-challenges", "profile-wallet", "profile-settings")
                .map { compose.onNodeWithTag(it).fetchSemanticsNode().boundsInRoot.top }
            assertTrue("running-path order $order", order == order.sorted())
            shot("01-profile")

            tapTag("profile-edit")
            compose.onNodeWithText(context.getString(R.string.profile_edit_profile)).assertIsDisplayed()
            shot("02-edit-profile")
            // 이름 칸에 글자판이 떠 있으면 첫 뒤로 가기는 글자판만 닫는다 — 창이 닫힐 때까지
            val editTitle = context.getString(R.string.profile_edit_profile)
            repeat(3) {
                if (compose.onAllNodesWithText(editTitle).fetchSemanticsNodes().isNotEmpty()) pressBack()
            }
            compose.onAllNodesWithText(editTitle).assertCountEquals(0)
            awaitTag("profile-records")

            // 내 러닝 기록(2026-09-28 전달본) — 이번 달 목록. 예전 기록 · 분석은 통계 안쪽 링크로
            tapTag("profile-records")
            awaitTag("records-period")
            shot("03-records")
            pressBack()
            awaitTag("profile-challenges")

            tapTag("profile-challenges")
            awaitTag("challenge-history-summary")
            shot("04-challenges")
            pressBack()
            awaitTag("profile-wallet")

            tapTag("profile-wallet")
            awaitText(context.getString(R.string.settings_wallet))
            shot("05-wallet")
            pressBack()
            awaitTag("profile-settings")

            tapTag("profile-settings")
            awaitText(context.getString(R.string.set_experience), scroll = true)
            compose.onAllNodesWithTag("profile-record-card").assertCountEquals(0)
            shot("06-settings")
            pressBack()
            awaitTag("profile-record-card")
            compose.onNodeWithTag("profile-settings").assertIsDisplayed()
        } finally {
            ServiceLocator.database.openHelper.writableDatabase
                .execSQL("DELETE FROM walk_sessions WHERE startedAt >= $MARKER")
        }
    }

    @Test fun runningPathStates() {
        var records by mutableStateOf<RunRecordState>(RunRecordState.Ready(0.0, 0))
        var profile by mutableStateOf(ProfileViewModel.UiState(balance = 83.07, loaded = true))
        var large by mutableStateOf(false)
        var light by mutableStateOf(false)
        var narrow by mutableStateOf(false)
        edgeToEdge()
        compose.setContent {
            val density = LocalDensity.current
            CompositionLocalProvider(LocalDensity provides Density(density.density, if (large) 1.3f else 1f)) {
                StepUpTheme(if (light) ThemeMode.LIGHT else ThemeMode.DARK) {
                    ExperienceProvider {
                        Box(Modifier.fillMaxSize()) {
                            S2Stage(Modifier.fillMaxSize())
                            LazyColumn(
                                Modifier.then(if (narrow) Modifier.requiredWidth(320.dp) else Modifier.fillMaxSize())
                                    .align(Alignment.TopCenter).statusBarsPadding(),
                                contentPadding = PaddingValues(horizontal = StepUpDesign.Gutter, vertical = 12.dp),
                            ) {
                                item {
                                    ProfileHome(
                                        state = profile, records = records, onRetryRecords = {}, onEditProfile = {},
                                        onOpenRecords = {}, onOpenChallenges = {}, onOpenWallet = {}, onOpenSettings = {},
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
        fun state(name: String, check: () -> Unit = {}) {
            compose.waitForIdle()
            check()
            shot(name)
        }
        state("11-empty") {
            compose.onNodeWithTag("profile-distance", useUnmergedTree = true).assertTextEquals("0.0")
            compose.onNodeWithTag("profile-runs", useUnmergedTree = true).assertTextEquals("아직 기록한 러닝이 없어요.")
        }
        compose.runOnIdle { records = RunRecordState.Loading }
        state("12-loading") {
            // 읽기 전에는 0 을 보이지 않는다
            compose.onNodeWithTag("profile-distance", useUnmergedTree = true).assertTextEquals("—")
        }
        compose.runOnIdle { records = RunRecordState.Failed }
        state("13-failed") {
            compose.onNodeWithTag("profile-record-failed", useUnmergedTree = true).assertIsDisplayed()
            compose.onNodeWithTag("profile-record-retry").assertHasClickAction()
            compose.onAllNodesWithTag("profile-distance", useUnmergedTree = true).assertCountEquals(0)
        }
        compose.runOnIdle {
            records = RunRecordState.Ready(12_345_600.0, 1284)
            profile = profile.copy(nickname = "아주아주긴닉네임을가진새벽러너님입니다", balance = 1_234_567.8912)
        }
        state("14-long-name-big-number") { assertDistanceFitsCard() }
        compose.runOnIdle { records = RunRecordState.Ready(34_200.0, 8); large = true }
        state("15-large-font") { assertDistanceFitsCard() }
        compose.runOnIdle { large = false; narrow = true }
        state("16-narrow-320") { assertDistanceFitsCard() }
        compose.runOnIdle { narrow = false; light = true; profile = profile.copy(nickname = "", balance = 83.07) }
        state("17-light-theme")
    }

    // ── 도우미 ─────────────────────────────────────────────────────

    /** 거리 숫자가 카드 안에 온전히 있다(잘리지 않는다) */
    private fun assertDistanceFitsCard() {
        val card = compose.onNodeWithTag("profile-record-card").fetchSemanticsNode().boundsInRoot
        val number = compose.onNodeWithTag("profile-distance", useUnmergedTree = true).fetchSemanticsNode().boundsInRoot
        assertTrue("distance inside card: number=$number card=$card",
            number.left >= card.left - 1 && number.right <= card.right + 1 && number.bottom <= card.bottom + 1)
    }

    private fun edgeToEdge() {
        runBlocking {
            ServiceLocator.userPrefs.setReducedMotion(true)
            ServiceLocator.userPrefs.setSounds(false)
            ServiceLocator.userPrefs.setHaptics(false)
        }
        compose.activityRule.scenario.onActivity {
            it.enableEdgeToEdge(
                statusBarStyle = SystemBarStyle.dark(android.graphics.Color.TRANSPARENT),
                navigationBarStyle = SystemBarStyle.dark(android.graphics.Color.TRANSPARENT),
            )
        }
    }

    private fun awaitTag(tag: String) {
        compose.waitUntil(10_000) { compose.onAllNodesWithTag(tag).fetchSemanticsNodes().isNotEmpty() }
    }

    private fun awaitText(text: String, scroll: Boolean = false) {
        compose.waitUntil(10_000) {
            if (scroll) runCatching { compose.onAllNodes(hasScrollAction())[0].performScrollToNode(hasText(text)) }
            compose.onAllNodesWithText(text).fetchSemanticsNodes().isNotEmpty()
        }
    }

    private fun tapTag(tag: String) {
        awaitTag(tag)
        val node = compose.onNodeWithTag(tag)
        runCatching { node.performScrollTo() }
        node.performClick()
        compose.waitForIdle()
    }

    // 실제 뒤로 가기 키 — 대화상자 창이 먼저 받는다(ChromeNavigationTest 와 같은 방식)
    private fun pressBack() {
        InstrumentationRegistry.getInstrumentation().sendKeyDownUpSync(android.view.KeyEvent.KEYCODE_BACK)
        compose.waitForIdle()
    }

    private fun shot(name: String) {
        compose.waitForIdle()
        InstrumentationRegistry.getInstrumentation().waitForIdleSync()
        Thread.sleep(700)
        captureDisplay(File(directory, "$name.png"))
    }

    companion object {
        /** 2100-01-01 — 이 테스트가 넣은 세션만 골라 지운다 */
        private const val MARKER = 4_102_444_800_000L

        /** 넣는 세션의 거리(m) — 합 34.2km */
        private val RUNS = listOf(3200.0, 4100.0, 5000.0, 3800.0, 4600.0, 5200.0, 3900.0, 4400.0)
    }
}
