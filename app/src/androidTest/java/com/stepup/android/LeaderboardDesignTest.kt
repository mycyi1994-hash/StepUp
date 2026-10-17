package com.stepup.android

import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.requiredSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.assertTextContains
import androidx.compose.ui.test.hasAnyAncestor
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import androidx.test.platform.app.InstrumentationRegistry
import com.stepup.android.domain.LeaderLoad
import com.stepup.android.ui.experience.LocalMotion
import com.stepup.android.ui.experience.MotionPreferences
import com.stepup.android.ui.screens.leaderboard.LeaderBoardKind
import com.stepup.android.ui.screens.leaderboard.LeaderboardScreen
import com.stepup.android.ui.screens.leaderboard.LeaderboardViewModel
import com.stepup.android.ui.theme.StepUpTheme
import com.stepup.android.ui.theme.ThemeMode
import java.io.File
import org.junit.Rule
import org.junit.Test

/**
 * 리더보드(2026-10-17 전달본) — 크루 · 개인 · 주간 포인트 세 판과 시트 · 상태를 실제로 그려 본다.
 *
 * 순위는 개발용 고정 값([FixtureLeaderboards] — 시안의 예시 이름 · 수치)이다. 주간 포인트의 실제 앱 상태는 '준비 중'(서버 순위 없음)이고
 * 고정 값 화면은 모양만 확인한다. 장면은 leaderboard/ 에 남는다(L01~ 장면, M01~ 배경이 흐르는 프레임).
 */
class LeaderboardDesignTest {
    // 앞 검사가 앱 언어를 바꿔 두었을 수 있다 — 한국어로 찍고 끝나면 되돌린다(RunJourneyDesignTest 와 같은 방식)
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

    /** 화면이 실제로 쓰는 언어의 글(검사는 글자를 지어 맞추지 않는다) */
    private fun text(id: Int): String = compose.activity.getString(id)

    private val directory get() = File(compose.activity.getExternalFilesDir(null), "leaderboard").apply { mkdirs() }

    private data class Viewport(val width: Int, val height: Int, val font: Float = 1f, val mode: ThemeMode = ThemeMode.DARK)

    private var viewport by mutableStateOf(Viewport(390, 844))
    private var reduced by mutableStateOf(true)
    private var scene by mutableStateOf<(@Composable () -> Unit)?>(null)

    private fun open(source: FixtureLeaderboards, initial: LeaderBoardKind = LeaderBoardKind.CREW, tag: String) {
        compose.runOnIdle {
            val model = LeaderboardViewModel(source)
            scene = { androidx.compose.runtime.key(model) { LeaderboardScreen(onBack = {}, initial = initial, viewModel = model) } }
        }
        awaitTag(tag)
    }

    @Test fun screens() {
        edgeToEdge()
        compose.setContent { Frame { scene?.invoke() } }

        // L01 크루 — 1위 블루 면 · 2 · 3위 나란히 · 4위부터 목록 · 아래 우리 크루
        open(FixtureLeaderboards(), tag = "lb-crew-winner")
        awaitTag("lb-crew-podium-2")
        awaitTag("lb-crew-podium-3")
        awaitTag("lb-crew-footer")
        compose.onNodeWithTag("lb-crew-winner-name", useUnmergedTree = true).assertTextContains("서울러닝클럽")
        compose.onNodeWithTag("lb-crew-footer-rank", useUnmergedTree = true).assertTextContains("12")
        compose.onNodeWithTag("lb-criterion", useUnmergedTree = true).assertTextContains(text(com.stepup.android.R.string.lb_criterion_crew))
        shot("L01-crew")
        // L02 우리 크루 앞뒤 — 시트가 열린 동안 배경은 멈춘다
        tap("lb-crew-footer")
        awaitTag("lb-sheet-crew_nearby")
        awaitTag("lb-row-me")
        shot("L02-crew-nearby")
        tap("lb-sheet-close")
        awaitGone("lb-sheet-crew_nearby")
        // L03 순위 기준
        tap("lb-info")
        awaitTag("lb-sheet-rules")
        shot("L03-crew-rules")
        tap("lb-sheet-close")
        awaitGone("lb-sheet-rules")

        // L04 개인 — 큰 내 순위 · 지난주 대비 · 바로 앞과의 차이 · 내 주변
        tap("lb-board-1")
        awaitTag("lb-personal-hero")
        compose.onNodeWithTag("lb-personal-rank", useUnmergedTree = true).assertTextContains("127")
        compose.onNodeWithTag("lb-personal-gap", useUnmergedTree = true).assertTextContains("0.8")
        awaitTag("lb-row-me")
        shot("L04-personal")
        // L05 상위 러너 — 목록만 바뀌고 큰 내 순위는 그대로
        tap("lb-runner-list-1")
        awaitTag("lb-row-1")
        awaitGone("lb-row-me")
        compose.onNodeWithTag("lb-personal-rank", useUnmergedTree = true).assertTextContains("127")
        shot("L05-personal-top")
        // L06 이번 주 나의 러닝 — 아래 줄의 합(32.6km · 4회)과 같은 기록 넷
        tap("lb-personal-footer")
        awaitTag("lb-sheet-my_runs")
        awaitTag("lb-run-3")
        shot("L06-personal-runs")
        tap("lb-sheet-close")
        awaitGone("lb-sheet-my_runs")

        // L07 주간 포인트(고정 값) — 이번 주 적립 · 내 순위 · 1위 줄 · 바로 앞과의 차이
        tap("lb-board-2")
        awaitTag("lb-points-hero")
        compose.onNodeWithTag("lb-points-mine", useUnmergedTree = true).assertTextContains("2,680")
        compose.onNodeWithTag("lb-points-rank-value", useUnmergedTree = true).assertTextContains("84")
        compose.onNodeWithTag("lb-points-gap", useUnmergedTree = true).assertTextContains("80")
        shot("L07-points")
        // L08 우리 크루 범위 — 목록 · 내 순위 · 차이가 함께 바뀐다
        tap("lb-scope-1")
        compose.waitUntil(15_000) {
            runCatching { compose.onNodeWithTag("lb-points-rank-value", useUnmergedTree = true).assertTextContains("8") }.isSuccess
        }
        compose.onNodeWithTag("lb-points-gap", useUnmergedTree = true).assertTextContains("80")
        shot("L08-points-crew")
        tap("lb-points-footer")
        awaitTag("lb-sheet-point_nearby")
        awaitTag("lb-row-me")
        shot("L09-points-nearby")
        tap("lb-sheet-close")
        awaitGone("lb-sheet-point_nearby")

        // L10 실제 앱의 주간 포인트 — 서버 순위가 없어 '준비 중'(지어낸 숫자 없음)
        open(FixtureLeaderboards(points = { LeaderLoad.Unavailable }), LeaderBoardKind.POINTS, "lb-unavailable")
        check(!exists("lb-points-footer") && !exists("lb-points-hero")) { "준비 중인 포인트 판에 숫자가 보이면 안 된다" }
        shot("L10-points-unavailable")
        // ⓘ 순위 기준도 준비 중이라고만 한다 — 정하지 않은 포인트 기준을 설명하지 않는다(2026-10-17 결정)
        tap("lb-info")
        awaitTag("lb-sheet-rules")
        compose.onNode(
            hasText(text(com.stepup.android.R.string.lb_points_unavailable_title)) and hasAnyAncestor(hasTestTag("lb-rule-0")),
            useUnmergedTree = true,
        ).assertExists()
        check(compose.onAllNodesWithText(text(com.stepup.android.R.string.lb_rules_points_title), useUnmergedTree = true).fetchSemanticsNodes().isEmpty()) {
            "준비 중인 포인트 판의 순위 기준에 포인트 기준이 보이면 안 된다"
        }
        shot("L10-points-unavailable-rules")
        tap("lb-sheet-close")
        awaitGone("lb-sheet-rules")
        // L11 이번 주 아직 달리지 않음 — 내 순위 없이 상위 러너만
        open(FixtureLeaderboards(runners = LeaderLoad.Ready(LeaderboardSamples.runners(ranked = false))), LeaderBoardKind.PERSONAL, "lb-personal-unranked")
        check(!exists("lb-personal-hero") && !exists("lb-runner-list-0")) { "순위가 없으면 큰 순위 · 내 주변이 없다" }
        shot("L11-personal-unranked")
        // L12 크루 없음
        open(FixtureLeaderboards(crews = LeaderLoad.Ready(LeaderboardSamples.crews(mine = false))), tag = "lb-crew-footer")
        compose.onNodeWithTag("lb-crew-footer")
            .assertTextContains(text(com.stepup.android.R.string.lb_crew_none_title), substring = true)
        shot("L12-crew-no-crew")
        // L13 통신 실패 — 다시 시도
        open(FixtureLeaderboards(crews = LeaderLoad.Failed(offline = true)), tag = "lb-failed")
        awaitTag("lb-failed-action")
        shot("L13-crew-failed")

        // L14 작은 화면 · L15 큰 글씨 · L16 밝은 테마(리더보드는 늘 남색)
        compose.runOnIdle { viewport = Viewport(360, 740) }
        open(FixtureLeaderboards(), tag = "lb-crew-winner")
        shot("L14-crew-360")
        compose.runOnIdle { viewport = Viewport(360, 740, font = 1.3f) }
        open(FixtureLeaderboards(), LeaderBoardKind.PERSONAL, "lb-personal-hero")
        shot("L15-personal-360-font130")
        compose.runOnIdle { viewport = Viewport(390, 844, mode = ThemeMode.LIGHT) }
        open(FixtureLeaderboards(), LeaderBoardKind.POINTS, "lb-points-hero")
        shot("L16-points-light-theme")
    }

    /** 배경이 흐르는 프레임 — 시계를 손으로 넘겨 같은 화면의 빛 · 1위 띠가 움직이는지 본다(M01~M04) */
    @Test fun motionFrames() {
        edgeToEdge()
        compose.mainClock.autoAdvance = false
        reduced = false
        compose.setContent { Frame { scene?.invoke() } }
        compose.runOnUiThread {
            val model = LeaderboardViewModel(FixtureLeaderboards())
            scene = { LeaderboardScreen(onBack = {}, viewModel = model) }
        }
        // 시계를 멈춰 두었으니(배경이 끝없이 돈다) 기다림도 시계를 손으로 넘기며 — 순위가 오면 다음 프레임에 그려진다
        var ready = false
        repeat(150) {
            if (!ready) {
                compose.mainClock.advanceTimeBy(100)
                ready = exists("lb-crew-winner")
            }
        }
        check(ready) { "크루 1위 면이 그려지지 않았다" }
        listOf(0L, 1_500L, 1_500L, 1_500L).forEachIndexed { i, step ->
            compose.mainClock.advanceTimeBy(step)
            awaitFrameOnScreen(compose.activity)
            captureDisplay(File(directory, "M0${i + 1}-crew-motion.png"))
        }
    }

    @Composable private fun Frame(content: @Composable () -> Unit) {
        BoxWithConstraints(Modifier.fillMaxSize().background(Color.Black), contentAlignment = Alignment.TopCenter) {
            val v = viewport
            val px = minOf(constraints.maxHeight / v.height.toFloat(), constraints.maxWidth / v.width.toFloat())
            CompositionLocalProvider(
                androidx.compose.ui.platform.LocalDensity provides Density(px, v.font),
                LocalMotion provides MotionPreferences(reduced = reduced, active = true),
            ) {
                StepUpTheme(v.mode) {
                    Box(Modifier.requiredSize(v.width.dp, v.height.dp).testTag("lb-frame")) { content() }
                }
            }
        }
    }

    private fun exists(tag: String): Boolean = compose.onAllNodesWithTag(tag, useUnmergedTree = true).fetchSemanticsNodes().isNotEmpty()

    private fun awaitTag(tag: String, timeout: Long = 15_000) = compose.waitUntil(timeout) { exists(tag) }

    private fun awaitGone(tag: String, timeout: Long = 15_000) = compose.waitUntil(timeout) { !exists(tag) }

    private fun tap(tag: String) {
        awaitTag(tag)
        runCatching { compose.onNodeWithTag(tag).performScrollTo() }
        compose.onNodeWithTag(tag).performClick()
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
        awaitFrameOnScreen(compose.activity)
        captureDisplay(File(directory, "$name.png"))
    }
}
