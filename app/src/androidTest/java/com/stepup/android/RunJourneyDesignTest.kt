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
import androidx.compose.ui.semantics.SemanticsProperties
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
import com.stepup.android.data.repo.EconomySyncState
import com.stepup.android.domain.GeoPoint
import com.stepup.android.domain.RunVerdict
import com.stepup.android.domain.ShoeCatalog
import com.stepup.android.domain.ShoeTier
import com.stepup.android.domain.TrackPoint
import com.stepup.android.service.RunSaveStatus
import com.stepup.android.service.WalkSessionService
import com.stepup.android.service.WalkSessionState
import com.stepup.android.ui.MainScaffold
import com.stepup.android.ui.Routes
import com.stepup.android.ui.components.ShoeGradeBadge
import com.stepup.android.ui.components.ShoeNameWithBadge
import com.stepup.android.ui.experience.ExperienceProvider
import com.stepup.android.ui.theme.StepUpTheme
import com.stepup.android.ui.theme.ThemeMode
import java.io.File
import kotlin.math.roundToInt
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

/**
 * 러닝 홈 · 러닝 중 · 러닝 완료 + 공통 등급 배지(2026-09-29 전달본) — design/run-journey-2026-09-29 · design/grade-badges-2026-09-29,
 * 캡처는 `run-journey/`(j01 홈 · j02 러닝 중 · j03 러닝 완료 · j04 배지, f-* 기기 크기).
 *
 * 러닝 상태는 표시용 값이다(서비스를 켜지 않는다 — [WalkSessionService.showStateForTest]). 보상은 서버 확인 전이라
 * "예상 보상 · 정산 대기"로만 보이는지 본다.
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

            // 02 메인 — 알림 종 · 오늘의 걸음 · 신발 카드(이름 끝 채운 배지) · 효율 / 착화감 / 내구도 · 러닝 시작 · 기록 보기 | 코스 찾기
            awaitTag("home-start-run")
            awaitTag("home-shoe-name")
            compose.onNodeWithTag("header-notifications", useUnmergedTree = true).assertExists()
            compose.onNode(hasTestTag("tier-badge-rare") and hasAnyAncestor(hasTestTag("home-shoe-name")), useUnmergedTree = true)
                .assertExists()
            // 효율 · 착화감 · 내구도 — 거리 · 운동 시간 줄은 없다
            compose.onNodeWithTag("home-stat-durability-value", useUnmergedTree = true).assertTextEquals("92 / 100")
            compose.onNodeWithTag("home-open-records").assertExists()
            compose.onNodeWithTag("home-open-courses").assertExists()
            shot("j01-home")
            tap("home-steps")
            awaitTag("home-details")
            shot("j01b-home-details")
            back()
            awaitGone("home-details")

            // 01 러닝 중 — 속도 측정 오류(노란 띠 · 현재 속도 "—" 와 경고) · 러닝 시간 | 달린 거리 · 현재 속도 | 내구도 · 일시정지 | 종료
            val now = System.currentTimeMillis()
            WalkSessionService.showStateForTest(running(now, flaggedAt = now))
            compose.onNodeWithTag("home-start-run").performClick()
            awaitTag("run-bottom-panel")
            awaitTag("run-speed-banner")
            compose.onNodeWithTag("run-speed-warning", useUnmergedTree = true).assertExists()
            compose.onNodeWithTag("run-speed-value", useUnmergedTree = true).assertTextEquals("—")
            compose.onNodeWithTag("run-durability-value", useUnmergedTree = true).assertTextEquals("92")
            compose.onNodeWithTag("run-primary-action").assertIsDisplayed().assertTextContains(label(R.string.cd_pause))
            compose.onNodeWithTag("run-finish").assertIsDisplayed().assertTextContains(label(R.string.cd_stop))
            // 평균 페이스 · 예상 보상은 러닝 중 화면에 없다
            compose.onAllNodesWithText(label(R.string.run_avg_pace)).assertCountEquals(0)
            compose.onAllNodesWithText(label(R.string.result_estimated_reward)).assertCountEquals(0)
            assertRunFits("390x844")
            shot("j02-run-speed-error")

            // 속도가 잡혔을 때 — 띠는 이 러닝에 버려진 구간이 있으면 남고(확인 중), 현재 속도는 수로
            WalkSessionService.showStateForTest(running(now, flaggedAt = 0L, flagged = 0))
            awaitGone("run-speed-banner")
            eventually { compose.onNodeWithTag("run-speed-value", useUnmergedTree = true).assert(hasText(".", substring = true)) }
            compose.onAllNodesWithTag("run-speed-warning", useUnmergedTree = true).assertCountEquals(0)
            shot("j02b-run-active")

            WalkSessionService.showStateForTest(running(now, flaggedAt = 0L, flagged = 0).copy(isPaused = true))
            eventually { compose.onNodeWithTag("run-primary-action").assertTextContains(label(R.string.runflow_resume)) }
            shot("j02c-run-paused")

            WalkSessionService.showStateForTest(running(now, flaggedAt = 0L, flagged = 0).copy(mockLocation = true))
            awaitTag("run-void-banner")
            shot("j02d-run-void")

            WalkSessionService.showStateForTest(
                WalkSessionState(isActive = true, steps = 180, elapsedSec = 95, startedAt = now - 95_000),
            )
            awaitTag("run-gps-status")
            shot("j02e-run-no-gps")

            // 03 러닝 완료 — 로그인한 사람의 서버 확인 전(시안): 예상 보상 +2.4 SUP · 정산 대기(적립 완료라고 쓰지 않는다)
            WalkSessionService.showStateForTest(finished(now))
            awaitTag("run-result-card")
            signedIn()
            compose.onNodeWithTag("run-result-distance", useUnmergedTree = true).assertTextEquals("3.24")
            compose.onNodeWithTag("run-result-reward-label", useUnmergedTree = true).assertTextEquals(label(R.string.result_estimated_reward))
            compose.onNode(hasText("+2.4") and hasAnyAncestor(hasTestTag("run-result-reward")), useUnmergedTree = true).assertExists()
            compose.onNodeWithTag("run-result-settle-pending", useUnmergedTree = true).assertExists()
            compose.onAllNodesWithText(label(R.string.result_settle_done)).assertCountEquals(0)
            compose.onNode(hasTestTag("tier-badge-rare") and hasAnyAncestor(hasTestTag("run-result-shoe-name")), useUnmergedTree = true)
                .assertExists()
            compose.onNodeWithTag("brand-wordmark-card", useUnmergedTree = true).assertExists()
            compose.onNodeWithTag("run-result-save").assertIsDisplayed().assertHasClickAction()
            compose.onNodeWithTag("run-result-share").assertIsDisplayed().assertHasClickAction()
            compose.onNodeWithTag("run-result-done").assertIsDisplayed().assertTextContains(label(R.string.finish_done))
            assertResultFits("390x844")
            shot("j03-result-pending", settle = 1_500)

            WalkSessionService.showStateForTest(finished(now).copy(lastVerdict = RunVerdict.VOID))
            awaitTag("run-result-settle-void")
            compose.onNode(hasText("0") and hasAnyAncestor(hasTestTag("run-result-reward")), useUnmergedTree = true).assertExists()
            shot("j03b-result-void", settle = 1_200)

            // 로그인이 풀린 채 끝난 러닝 — 정산 대기 아래에 까닭 · 다시 로그인(카드가 길어져 넘길 수 있다). 완료는 화면 안
            ServiceLocator.economySync.showStateForTest(EconomySyncState.SIGNED_OUT)
            WalkSessionService.showStateForTest(finished(now))
            awaitTag("run-result-reward-note")
            compose.onNodeWithTag("run-result-settle-pending", useUnmergedTree = true).assertExists()
            compose.onNode(hasText(label(R.string.session_sign_in_again)) and hasAnyAncestor(hasTestTag("run-result-card")))
                .assertHasClickAction()
            assertResultFits("390x844", strict = false)
            compose.onNodeWithTag("run-result-reward-note", useUnmergedTree = true).performScrollTo()
            shot("j03c-result-signed-out", settle = 1_200)
        } finally {
            WalkSessionService.showStateForTest(WalkSessionState())
            syncBefore?.let { ServiceLocator.economySync.showStateForTest(it) }
            restore()
        }
    }

    /** 360×800 · 390×844 · 412×915 · 큰 글씨 · 밝은 테마 — 러닝 중은 스크롤 없이 두 버튼이 화면 안, 결과는 완료 버튼이 화면 안 */
    @Test fun fitsAtDeviceSizes() {
        seed()
        try {
            edgeToEdge()
            compose.setContent { DeviceFrame { MainScaffold(initialRoute = Routes.RUN) } }
            val now = System.currentTimeMillis()
            for (next in listOf(
                Viewport(360, 800), Viewport(390, 844), Viewport(412, 915),
                Viewport(360, 800, font = 1.3f), Viewport(390, 844, mode = ThemeMode.LIGHT),
            )) {
                compose.runOnIdle { viewport = next }
                compose.waitForIdle()
                WalkSessionService.showStateForTest(running(now, flaggedAt = now))
                awaitTag("run-bottom-panel")
                assertRunFits(next.label, strict = next.font == 1f)
                shot("f-${next.label}-run")
                WalkSessionService.showStateForTest(finished(now))
                awaitTag("run-result-card")
                signedIn()
                assertResultFits(next.label, strict = next.font == 1f && next.height >= 844)
                shot("f-${next.label}-result", settle = 1_200)
            }
        } finally {
            WalkSessionService.showStateForTest(WalkSessionState())
            syncBefore?.let { ServiceLocator.economySync.showStateForTest(it) }
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

    /** 24:18 · 3.24km 를 달리는 중 — 마지막 점은 방금(현재 속도를 잰다). [flaggedAt] 이 마지막 점보다 늦으면 방금 읽은 속도가 버려진 것 */
    private fun running(now: Long, flaggedAt: Long, flagged: Int = 2) = WalkSessionState(
        isActive = true, steps = 4_380, elapsedSec = 1_458, startedAt = now - 1_458_000,
        gpsFix = true, gpsKm = 3.24, validSegments = 180, flaggedSegments = flagged, lastFlaggedAt = flaggedAt,
        track = route(now - 1_458_000, now - 2_000),
    )

    /** 방금 끝난 러닝 — 3.24km · 24:18 · 폰이 셈한 예상 2.4 SUP(서버 확인 전) */
    private fun finished(now: Long) = WalkSessionState(
        lastRewardPoints = 2.4, lastSessionSteps = 4_380, lastRewardedSteps = 4_380,
        lastElapsedSec = 1_458, lastGpsKm = 3.24, lastStartedAt = now - 1_458_000,
        track = route(now - 1_458_000, now - 2_000),
    )

    /** 한강 따라 남동쪽으로 — 시각은 [from]..[to] 에 고르게(마지막 몇 점은 10초 간격 · 약 2.2m/s) */
    private fun route(from: Long, to: Long): List<TrackPoint> {
        val path = listOf(
            GeoPoint(37.5340, 126.9200), GeoPoint(37.5320, 126.9235), GeoPoint(37.5302, 126.9262),
            GeoPoint(37.5290, 126.9301), GeoPoint(37.5271, 126.9335), GeoPoint(37.5256, 126.9372),
            GeoPoint(37.5240, 126.9410), GeoPoint(37.5226, 126.9446),
        )
        val body = path.dropLast(1).mapIndexed { i, p -> TrackPoint(p.lat, p.lng, from + (to - 60_000 - from) * i / (path.size - 2)) }
        val last = path.last()
        val tail = (0..5).map { k ->
            TrackPoint(last.lat - 0.00018 * (5 - k), last.lng - 0.00018 * (5 - k), to - 10_000L * (5 - k))
        }
        return body + tail
    }

    // ── 검사 ─────────────────────────────────────────────────────

    /** 러닝 중 — 러닝 시간이 버튼 위, 두 버튼이 화면 안. [strict] 면 겹쳐 쌓지 않고 한 화면(스크롤 0) */
    private fun assertRunFits(where: String, strict: Boolean = true) {
        compose.waitForIdle()
        val hero = bounds("run-hero-value")
        val action = bounds("run-primary-action")
        val finish = bounds("run-finish")
        val limit = frameBottom()
        assertTrue("run time above the buttons at $where: $hero / $action", hero.bottom <= action.top)
        assertTrue("pause inside the screen at $where: $action / $limit", action.bottom <= limit + 1)
        assertTrue("stop inside the screen at $where: $finish / $limit", finish.bottom <= limit + 1)
        if (strict) {
            val scroll = compose.onNodeWithTag("run-overlay-scroll").fetchSemanticsNode().config[SemanticsProperties.VerticalScrollAxisRange]
            assertEquals("the run screen must not scroll at $where", 0f, scroll.maxValue(), 1f)
            val panel = bounds("run-bottom-panel")
            assertTrue("bottom panel above the buttons at $where: $panel / $action", panel.bottom <= action.top + 1)
        }
    }

    /**
     * 러닝 완료 — 완료 버튼이 화면 안, 지도는 최소 높이 이상. [strict] 면 카드가 한 화면(스크롤 0) — 시안 상태(로그인 · 정산 대기)에서 본다.
     * 보상 줄 아래 까닭 · 다시 로그인이 붙는 모습(로그인이 풀림 · j03c)은 그만큼 길어져 넘길 수 있다.
     */
    private fun assertResultFits(where: String, strict: Boolean = true) {
        compose.waitForIdle()
        val done = bounds("run-result-done")
        assertTrue("done inside the screen at $where: $done / ${frameBottom()}", done.bottom <= frameBottom() + 1)
        if (exists("run-result-map")) {
            val dpPx = bounds(FRAME).height / viewport.height
            val map = bounds("run-result-map")
            assertTrue("the map keeps its minimum at $where: $map", map.height >= 139f * dpPx)
        }
        if (strict) {
            val scroll = compose.onNodeWithTag("run-result-scroll").fetchSemanticsNode().config[SemanticsProperties.VerticalScrollAxisRange]
            assertEquals("the run result must not scroll at $where", 0f, scroll.maxValue(), 1f)
        }
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
    }
}
