package com.stepup.android

import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.requiredWidth
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
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
import com.stepup.android.domain.ChainRecord
import com.stepup.android.domain.Faction
import com.stepup.android.domain.Rarity
import com.stepup.android.domain.Sneaker
import com.stepup.android.ui.MainScaffold
import com.stepup.android.ui.Screen
import com.stepup.android.ui.components.CommerceBackdrop
import com.stepup.android.ui.experience.ExperienceProvider
import com.stepup.android.ui.screens.rewards.ChainActivityContent
import com.stepup.android.ui.screens.rewards.ChainActivityLoad
import com.stepup.android.ui.theme.StepUpTheme
import com.stepup.android.ui.theme.ThemeMode
import java.io.File
import java.time.LocalDateTime
import java.time.ZoneId
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

/**
 * 체인 기록(내 정보 › 지갑 › 체인 기록) — 러닝 증명 · 배지 · 코스 완주 · 신발 발행 · 스탯 갱신이 체인에 어떻게 올라갔는지.
 *
 * [chainInTheApp] 은 앱 셸 안에서 내 정보 → 지갑 → 체인 기록으로 간다(이 기기의 실제 상태 그대로 — 로그인 전이면 로그인 안내).
 * [chainStates] 는 예시 기록을 화면에 바로 넣어 찍는다(테스트 안에서만 — 앱은 서버가 준 기록만 보인다).
 */
class ChainActivityDesignTest {
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

    private val directory get() = File(compose.activity.getExternalFilesDir(null), "chain-activity").apply { mkdirs() }

    @Test fun chainInTheApp() {
        edgeToEdge()
        compose.setContent { StepUpTheme(ThemeMode.DARK) { ExperienceProvider { MainScaffold(initialTab = Screen.Profile) } } }
        tap("profile-wallet")
        awaitTag("wl-list")
        compose.onNodeWithTag("wl-list").performScrollToNode(hasTestTag("wl-chain"))
        shot("01-wallet-chain-row")
        tap("wl-chain")
        compose.waitUntil(10_000) {
            listOf("ca-list", "ca-signed-out", "ca-failed").any { compose.onAllNodesWithTag(it, useUnmergedTree = true).fetchSemanticsNodes().isNotEmpty() }
        }
        shot("02-in-app-chain")
        tap("ca-guide")
        awaitTag("ca-guide-sheet")
        shot("03-in-app-guide")
        back()
        awaitGone("ca-guide-sheet")
        back()
        awaitTag("wl-list")
    }

    @Test fun chainStates() {
        var light by mutableStateOf(false)
        var large by mutableStateOf(false)
        var narrow by mutableStateOf(false)
        edgeToEdge()
        compose.setContent {
            val density = LocalDensity.current
            CompositionLocalProvider(LocalDensity provides Density(density.density, if (large) 1.3f else 1f)) {
                StepUpTheme(if (light) ThemeMode.LIGHT else ThemeMode.DARK) {
                    ExperienceProvider {
                        Box(Modifier.fillMaxSize()) {
                            CommerceBackdrop(Modifier.fillMaxSize())
                            Box(
                                Modifier.then(if (narrow) Modifier.requiredWidth(320.dp).fillMaxHeight() else Modifier.fillMaxSize())
                                    .align(Alignment.TopCenter).statusBarsPadding(),
                            ) { scene() }
                        }
                    }
                }
            }
        }
        fun chain(state: ChainActivityLoad, guide: Boolean = false): @Composable () -> Unit = {
            ChainActivityContent(state = state, sneakers = SHOES, zone = SEOUL, thisYear = 2026, initialGuide = guide)
        }
        val ready = ChainActivityLoad.Ready(SAMPLE)
        show("s01-records", "ca-row-6", chain(ready))
        compose.onNodeWithTag("ca-count-confirmed").assertTextContains("3", substring = true)
        compose.onNodeWithTag("ca-count-pending").assertTextContains("2", substring = true)
        // 확정된 줄만 익스플로러로 연다 — 대기 · 보내지 않음은 누를 것이 없다
        compose.onNodeWithTag("ca-row-6").assertHasClickAction()
        compose.onNodeWithTag("ca-row-5").assertHasNoClickAction()
        compose.onNodeWithTag("ca-row-1").assertHasNoClickAction()
        show("s02-guide", "ca-guide-sheet", chain(ready, guide = true))
        show("s03-empty", "ca-empty", chain(ChainActivityLoad.Ready(emptyList())))
        show("s04-loading", "ca-loading", chain(ChainActivityLoad.Loading))
        compose.onAllNodesWithTag("ca-count-confirmed").assertCountEquals(0)
        show("s05-failed", "ca-failed", chain(ChainActivityLoad.Failed))
        show("s06-signed-out", "ca-signed-out", chain(ChainActivityLoad.SignedOut))
        compose.runOnIdle { light = true }
        show("s07-light", "ca-row-6", chain(ready))
        show("s07b-light-guide", "ca-guide-sheet", chain(ready, guide = true))
        compose.runOnIdle { light = false; large = true }
        show("s08-large-font", "ca-row-6", chain(ready))
        compose.runOnIdle { large = false; narrow = true }
        show("s09-narrow", "ca-row-6", chain(ready))
        assertEquals(6, SAMPLE.size)
    }

    // ── 도우미 ─────────────────────────────────────────────────────

    private var scene by mutableStateOf<@Composable () -> Unit>({})

    private fun show(name: String, readyTag: String, content: @Composable () -> Unit) {
        compose.runOnIdle { scene = { key(name) { content() } } }
        awaitTag(readyTag)
        shot(name)
    }

    private fun tap(tag: String) {
        awaitTag(tag)
        val node = compose.onNodeWithTag(tag)
        runCatching { node.performScrollTo() }
        node.performClick()
        compose.waitForIdle()
    }

    private fun back() {
        InstrumentationRegistry.getInstrumentation().sendKeyDownUpSync(android.view.KeyEvent.KEYCODE_BACK)
        compose.waitForIdle()
    }

    private fun edgeToEdge() {
        runBlocking {
            ServiceLocator.userPrefs.setReducedMotion(true)
            ServiceLocator.userPrefs.setSounds(false)
            ServiceLocator.userPrefs.setHaptics(false)
            ServiceLocator.userPrefs.setGuideSeen()
        }
        compose.activityRule.scenario.onActivity {
            it.enableEdgeToEdge(
                statusBarStyle = SystemBarStyle.dark(android.graphics.Color.TRANSPARENT),
                navigationBarStyle = SystemBarStyle.dark(android.graphics.Color.TRANSPARENT),
            )
        }
    }

    private fun awaitTag(tag: String) {
        compose.waitUntil(10_000) { compose.onAllNodesWithTag(tag, useUnmergedTree = true).fetchSemanticsNodes().isNotEmpty() }
    }

    private fun awaitGone(tag: String) {
        compose.waitUntil(10_000) { compose.onAllNodesWithTag(tag, useUnmergedTree = true).fetchSemanticsNodes().isEmpty() }
    }

    private fun shot(name: String, settle: Long = 700) {
        compose.waitForIdle()
        InstrumentationRegistry.getInstrumentation().waitForIdleSync()
        Thread.sleep(settle)
        captureDisplay(File(directory, "$name.png"))
    }

    private companion object {
        val SEOUL: ZoneId = ZoneId.of("Asia/Seoul")

        fun at(y: Int, m: Int, d: Int, h: Int, min: Int): Long = LocalDateTime.of(y, m, d, h, min).atZone(SEOUL).toInstant().toEpochMilli()

        private fun tx(n: Int) = "0x" + n.toString(16).padStart(2, '0').repeat(32)

        /** 예시 신발 — 새 도감 레전더리 한 켤레(테스트 화면에만) */
        val SHOES = mapOf(
            41L to Sneaker(
                id = 41, faction = Faction.WIND, rarity = Rarity.LEGENDARY, variant = 0, level = 3, mintNumber = 7, luck = 1.0,
                comfort = 1.0, durability = 100, equipped = false, acquiredAt = 0, modelId = 1311,
            ),
        )

        /** 예시 기록(테스트 화면에만) — 확정 3 · 대기 2 · 보내지 않음 1 */
        val SAMPLE = listOf(
            ChainRecord(6, ChainRecord.Kind.RUN_PROOF, ChainRecord.Status.CONFIRMED, distanceM = 5240, durationSec = 1925,
                txHash = tx(0xa1), createdAt = at(2026, 9, 28, 7, 52), confirmedAt = at(2026, 9, 28, 7, 56)),
            ChainRecord(5, ChainRecord.Kind.VAULT_MINT, ChainRecord.Status.PENDING, sneakerId = 41, createdAt = at(2026, 9, 28, 7, 58)),
            ChainRecord(4, ChainRecord.Kind.BADGE, ChainRecord.Status.CONFIRMED, badge = "DISTANCE_KM", badgeValue = 10,
                txHash = tx(0xb2), createdAt = at(2026, 9, 28, 7, 52), confirmedAt = at(2026, 9, 28, 7, 56)),
            ChainRecord(3, ChainRecord.Kind.STATS_SYNC, ChainRecord.Status.PENDING, sneakerId = 41, createdAt = at(2026, 9, 27, 21, 3)),
            ChainRecord(2, ChainRecord.Kind.COURSE_RUN, ChainRecord.Status.CONFIRMED, distanceM = 3100, durationSec = 1180,
                txHash = tx(0xc3), createdAt = at(2026, 9, 27, 19, 20), confirmedAt = at(2026, 9, 27, 19, 24)),
            ChainRecord(1, ChainRecord.Kind.RUN_PROOF, ChainRecord.Status.CANCELLED, distanceM = 1200, durationSec = 700,
                createdAt = at(2025, 12, 30, 8, 0)),
        )
    }
}
