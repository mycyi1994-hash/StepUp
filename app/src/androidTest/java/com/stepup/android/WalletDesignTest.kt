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
import com.stepup.android.data.local.RewardEntity
import com.stepup.android.data.local.RewardTotals
import com.stepup.android.data.local.RewardType
import com.stepup.android.domain.DrawStatus
import com.stepup.android.ui.MainScaffold
import com.stepup.android.ui.Screen
import com.stepup.android.ui.components.CommerceBackdrop
import com.stepup.android.ui.experience.ExperienceProvider
import com.stepup.android.ui.screens.rewards.HistoryLoad
import com.stepup.android.ui.screens.rewards.LedgerFilter
import com.stepup.android.ui.screens.rewards.TotalsLoad
import com.stepup.android.ui.screens.rewards.WalletContent
import com.stepup.android.ui.screens.rewards.Web3State
import com.stepup.android.ui.screens.rewards.amountText
import com.stepup.android.ui.theme.StepUpTheme
import com.stepup.android.ui.theme.ThemeMode
import java.io.File
import java.time.LocalDateTime
import java.time.ZoneId
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

/**
 * 지갑 v1(2026-09-28 전달본, docs/redesign/wallet-v1) — 장면 번호는 시안의 01~16 그대로.
 *
 * [walletInTheApp] 은 앱 셸 안에서 내 정보 → 지갑으로 간다: 잔액은 이 기기 원장 전체 그대로, 거르개를 바꿔도 같다.
 * 첫 쪽(30줄) 밖의 사용 내역도 "사용"에서 찾고, 줄을 누르면 그 줄의 금액 · 구분만, 닫으면 거르개 그대로다.
 * 이 테스트가 넣은 원장 줄은 끝에 지운다.
 * [walletStates] 는 시안의 예시 원장(103.07 − 20.00 = 83.07 — 테스트 안에서만)과 기기에서 만들 수 없는 상태를 화면에 바로 넣어 찍는다.
 */
class WalletDesignTest {
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

    private val directory get() = File(compose.activity.getExternalFilesDir(null), "wallet-v1").apply { mkdirs() }

    @Test fun walletInTheApp() {
        val db = ServiceLocator.database
        val sql = db.openHelper.writableDatabase
        sql.execSQL("DELETE FROM rewards WHERE description LIKE '$MARK%'")
        val now = System.currentTimeMillis()
        runBlocking {
            val dao = db.rewardDao()
            // 오래된 사용 한 줄 + 그 뒤의 적립 35줄 — 사용 내역이 첫 쪽(30줄) 밖에 있다
            dao.insert(RewardEntity(timestamp = now - 40 * DAY, type = RewardType.SPEND_UPGRADE, amount = -20.0, description = "$MARK 신발 강화"))
            repeat(35) { i ->
                dao.insert(RewardEntity(timestamp = now - (35 - i) * MINUTE, type = RewardType.EARN_WALK, amount = 1.25, description = "$MARK 걷기 $i"))
            }
        }
        fun idOf(description: String): Long =
            db.query("SELECT id FROM rewards WHERE description = ?", arrayOf(description)).use { it.moveToFirst(); it.getLong(0) }
        val spendId = idOf("$MARK 신발 강화")
        val newestId = idOf("$MARK 걷기 34")
        val balance = runBlocking { db.rewardDao().balanceNow() }
        try {
            edgeToEdge()
            compose.setContent { StepUpTheme(ThemeMode.DARK) { ExperienceProvider { MainScaffold(initialTab = Screen.Profile) } } }
            tap("profile-wallet")
            awaitTag("wl-list")
            awaitTag("wl-balance")
            // 잔액 — 이 기기 원장 전체의 합(예시 숫자가 아니다)
            compose.onNodeWithTag("wl-balance").assertTextContains(amountText(balance, signed = false))
            compose.onNodeWithTag("wl-filter-all").assertIsSelected()
            scrollToRow("wl-row-$newestId")
            compose.onNodeWithTag("wl-list").performScrollToIndex(0)
            shot("01-in-app-wallet")

            // 04 — 사용: 첫 쪽 밖의 사용 내역도 찾는다. 잔액은 그대로
            tap("wl-filter-spent")
            compose.onNodeWithTag("wl-filter-spent").assertIsSelected()
            compose.onNodeWithTag("wl-balance").assertTextContains(amountText(balance, signed = false))
            scrollToRow("wl-row-$spendId")
            compose.onAllNodesWithTag("wl-row-$newestId").assertCountEquals(0)
            shot("04-in-app-spent")

            // 06 — 줄을 누르면 그 원장 줄의 금액 · 구분 · 내용. 닫으면 거르개 그대로
            tap("wl-row-$spendId")
            awaitTag("wl-entry-sheet")
            compose.onNodeWithTag("wl-entry-amount", useUnmergedTree = false).assertTextContains("−20.00")
            compose.onNodeWithTag("wl-entry-note").assertTextEquals("$MARK 신발 강화")
            shot("06-in-app-spent-detail")
            tap("wl-entry-ok")
            awaitGone("wl-entry-sheet")
            compose.onNodeWithTag("wl-filter-spent").assertIsSelected()

            // 03 — 적립: 양수만
            compose.onNodeWithTag("wl-list").performScrollToIndex(0)
            tap("wl-filter-earned")
            scrollToRow("wl-row-$newestId")
            compose.onAllNodesWithTag("wl-row-$spendId").assertCountEquals(0)
            compose.onNodeWithTag("wl-list").performScrollToIndex(0)
            shot("03-in-app-earned")

            // 13 — SUP 안내, 뒤로 가기는 시트만 닫는다
            tap("wl-info")
            awaitTag("wl-info-sheet")
            shot("13-in-app-info")
            back()
            awaitGone("wl-info-sheet")
            compose.onNodeWithTag("wl-filter-earned").assertIsSelected()

            // 전체 — 아래로 읽으면 다음 쪽을 이어 읽어 첫 쪽(30줄) 밖의 사용 내역까지 간다
            compose.onNodeWithTag("wl-list").performScrollToIndex(0)
            tap("wl-filter-all")
            scrollToRow("wl-row-$newestId")
            scrollToRow("wl-row-$spendId")
            shot("16-in-app-older")

            // 뒤로 — 내 정보
            back()
            awaitTag("profile-wallet")
        } finally {
            sql.execSQL("DELETE FROM rewards WHERE description LIKE '$MARK%'")
        }
    }

    @Test fun walletStates() {
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
        val ready = TotalsLoad.Ready(TOTALS)
        val all = HistoryLoad.Ready(LedgerFilter.ALL, SAMPLE, more = false)
        val earned = HistoryLoad.Ready(LedgerFilter.EARNED, SAMPLE.filter { it.amount > 0 }, more = false)
        val spent = HistoryLoad.Ready(LedgerFilter.SPENT, SAMPLE.filter { it.amount < 0 }, more = false)
        fun wallet(
            totals: TotalsLoad = ready,
            history: HistoryLoad = all,
            web3: Web3State = Web3State.NotLinked,
            status: DrawStatus? = STATUS,
            sheet: String? = null,
            entry: RewardEntity? = null,
        ): @Composable () -> Unit = {
            WalletContent(
                totals = totals, history = history, filter = (history as? HistoryLoad.Ready)?.filter ?: LedgerFilter.ALL,
                web3 = web3, status = status, zone = SEOUL, thisYear = 2026, initialSheet = sheet, initialEntry = entry,
            )
        }

        show("s01-wallet-unlinked", "wl-row-7", wallet())
        compose.onNodeWithTag("wl-balance").assertTextContains("83.07")
        compose.onNodeWithTag("wl-earned").assertTextContains("103.07")
        compose.onNodeWithTag("wl-spent").assertTextContains("20.00")
        compose.onNodeWithTag("wl-web3").assertHasClickAction()
        show("s02-wallet-linked", "wl-row-7", wallet(web3 = Web3State.Linked))
        show("s03-earned-filter", "wl-row-7", wallet(history = earned))
        compose.onNodeWithTag("wl-filter-earned").assertIsSelected()
        compose.onNodeWithTag("wl-filter-all").assertIsNotSelected()
        show("s04-spent-filter", "wl-row-4", wallet(history = spent))
        show("s05-earned-detail", "wl-entry-sheet", wallet(entry = SAMPLE.first()))
        compose.onNodeWithTag("wl-entry-amount").assertTextContains("+3.50")
        show("s06-spent-detail", "wl-entry-sheet", wallet(history = spent, entry = SAMPLE.first { it.amount < 0 }))
        compose.onNodeWithTag("wl-entry-amount").assertTextContains("−20.00")
        show("s07-first-empty", "wl-empty", wallet(totals = TotalsLoad.Ready(RewardTotals(0.0, 0.0, 0.0)),
            history = HistoryLoad.Ready(LedgerFilter.ALL, emptyList(), more = false)))
        compose.onNodeWithTag("wl-balance").assertTextContains("0.00")
        show("s08-no-spend-history", "wl-empty-filtered", wallet(history = HistoryLoad.Ready(LedgerFilter.SPENT, emptyList(), more = false)))
        compose.onNodeWithTag("wl-state-action").assertHasClickAction()
        show("s09-wallet-loading", "wl-history-loading", wallet(totals = TotalsLoad.Loading, history = HistoryLoad.Loading, web3 = Web3State.Checking))
        compose.onNodeWithTag("wl-balance-loading").assertExists()
        compose.onAllNodesWithTag("wl-balance").assertCountEquals(0)
        show("s10-wallet-error", "wl-failed", wallet(totals = TotalsLoad.Failed, history = HistoryLoad.Failed))
        compose.onAllNodesWithTag("wl-balance").assertCountEquals(0)
        show("s11-history-error", "wl-history-failed", wallet(history = HistoryLoad.Failed))
        compose.onNodeWithTag("wl-balance").assertTextContains("83.07")
        show("s12-connection-unavailable", "wl-web3", wallet(web3 = Web3State.Unavailable))
        compose.onNodeWithTag("wl-web3").assertHasNoClickAction()
        show("s13-sup-info", "wl-info-sheet", wallet(sheet = "Info"))
        show("s14-connected-sheet", "wl-linked-sheet", wallet(web3 = Web3State.Linked, sheet = "Linked"))
        compose.onNodeWithTag("wl-open-draw").assertHasClickAction()
        compose.onNodeWithTag("wl-open-wallet-page").assertHasClickAction()
        // 16 — 다음 쪽을 읽지 못했다: 읽은 줄 · 자리는 그대로, 아래에서 다시
        compose.runOnIdle { scene = wallet(history = HistoryLoad.Ready(LedgerFilter.ALL, SAMPLE + OLDER, more = true, pageFailed = true)) }
        awaitTag("wl-row-7")
        compose.onNodeWithTag("wl-list").performScrollToNode(hasTestTag("wl-page-failed"))
        compose.onNodeWithTag("wl-page-retry").assertHasClickAction()
        shot("s16-older-history-error")
        // 연결 혜택(기존 뽑기 전달본 06) — 숫자는 서버가 준 것만
        show("s17-benefit-sheet", "wl-benefit-sheet", wallet(sheet = "Benefit"))
        compose.onNodeWithTag("wl-connect").assertHasClickAction()
        show("s18-web3-unknown", "wl-web3-retry", wallet(web3 = Web3State.Unknown))
        show("s19-signed-out", "wl-web3", wallet(web3 = Web3State.SignedOut))
        compose.onNodeWithTag("wl-web3").assertHasNoClickAction()
        show("s20-stale-history", "wl-stale", wallet(history = all.copy(stale = true)))

        compose.runOnIdle { light = true }
        show("s30-light", "wl-row-7", wallet())
        show("s30b-light-detail", "wl-entry-sheet", wallet(entry = SAMPLE.first { it.amount < 0 }))
        compose.runOnIdle { light = false; large = true }
        show("s31-large-font", "wl-row-7", wallet(totals = TotalsLoad.Ready(RewardTotals(1_234_567.89, 1_254_567.89, 20_000.0))))
        compose.runOnIdle { large = false; narrow = true }
        show("s32-narrow", "wl-row-7", wallet(totals = TotalsLoad.Ready(RewardTotals(1_234_567.89, 1_254_567.89, 20_000.0))))
        show("s33-narrow-detail", "wl-entry-sheet", wallet(entry = LONG_NOTE))
    }

    // ── 도우미 ─────────────────────────────────────────────────────

    private var scene by mutableStateOf<@Composable () -> Unit>({})

    private fun show(name: String, readyTag: String, content: @Composable () -> Unit) {
        compose.runOnIdle { scene = content }
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

    /** 목록을 아래로 읽어(다음 쪽을 이어 읽으며) 그 줄까지 간다 */
    private fun scrollToRow(tag: String) {
        var found = false
        repeat(20) {
            if (!found) {
                found = runCatching { compose.onNodeWithTag("wl-list").performScrollToNode(hasTestTag(tag)) }.isSuccess
                if (!found) {
                    compose.waitForIdle()
                    Thread.sleep(300)
                }
            }
        }
        assertTrue("$tag reachable", found)
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
        const val MARK = "wallet-test"
        const val MINUTE = 60 * 1000L
        const val DAY = 24 * 60 * MINUTE
        val SEOUL: ZoneId = ZoneId.of("Asia/Seoul")

        fun at(y: Int, m: Int, d: Int, h: Int, min: Int): Long = LocalDateTime.of(y, m, d, h, min).atZone(SEOUL).toInstant().toEpochMilli()

        /** 시안의 예시 원장(sample-ledger.json, isMock) — 테스트 화면에만 넣는다 */
        val SAMPLE = listOf(
            RewardEntity(7, at(2026, 9, 28, 7, 50), RewardType.EARN_WALK, 3.5, "아침 러닝으로 적립한 SUP"),
            RewardEntity(6, at(2026, 9, 27, 19, 42), RewardType.EARN_WALK, 1.9, "저녁 걷기로 적립한 SUP"),
            RewardEntity(5, at(2026, 9, 27, 19, 42), RewardType.BONUS_GOAL, 9.47, "일일 목표 달성 보너스"),
            RewardEntity(4, at(2026, 9, 26, 20, 10), RewardType.SPEND_UPGRADE, -20.0, "이전에 이용한 신발 강화 내역"),
            RewardEntity(3, at(2026, 9, 25, 8, 12), RewardType.EARN_WALK, 18.2, "걷기·러닝으로 적립한 SUP"),
            RewardEntity(2, at(2026, 9, 24, 19, 30), RewardType.EARN_WALK, 20.0, "걷기·러닝으로 적립한 SUP"),
            RewardEntity(1, at(2026, 9, 23, 9, 0), RewardType.EARN_EVENT, 50.0, "이벤트로 적립한 SUP"),
        )
        val TOTALS = RewardTotals(balance = 83.07, earned = 103.07, spent = 20.0)

        /** 지난해 줄 — 목록에서도 연도까지 */
        val OLDER = listOf(
            RewardEntity(-1, at(2025, 12, 30, 18, 5), RewardType.EARN_WALK, 2.4, "걷기·러닝으로 적립한 SUP"),
            RewardEntity(-2, at(2025, 12, 28, 7, 40), "SOMETHING_NEW", 1.0, "새 종류의 원장 줄"),
        )
        val LONG_NOTE = RewardEntity(8, at(2026, 9, 22, 21, 15), RewardType.EARN_EVENT, 12.34,
            "주간 챌린지 완주 보상으로 적립한 SUP — 긴 설명은 줄을 바꿔 다 보이고, 시트 안에서 스크롤돼요. 버튼은 화면 안에 남아요.")

        val STATUS = DrawStatus(dailyLeft = 3, dailyTotal = 3, signupLeft = 10, signupGranted = 10, walletLinked = false,
            giftOnLink = 10, giftLeft = 0, runLeft = 0, genesisLeft = 0, runProgressMeters = 0.0, runStepMeters = 1000,
            runToday = 0, runDailyCap = 10, chainPaused = false)
    }
}
