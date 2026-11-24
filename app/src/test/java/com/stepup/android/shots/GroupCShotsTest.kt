package com.stepup.android.shots

import androidx.activity.ComponentActivity
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.unit.dp
import com.stepup.android.data.local.RewardTotals
import com.stepup.android.domain.Bag
import com.stepup.android.domain.BagItem
import com.stepup.android.domain.DrawOdds
import com.stepup.android.domain.DrawStatus
import com.stepup.android.domain.ItemKind
import com.stepup.android.domain.Rarity
import com.stepup.android.ui.components.MainHeader
import com.stepup.android.ui.screens.gacha.DrawActions
import com.stepup.android.ui.screens.gacha.DrawHome
import com.stepup.android.ui.screens.items.BagContent
import com.stepup.android.ui.screens.notifications.InboxLoad
import com.stepup.android.ui.screens.notifications.NotificationsContent
import com.stepup.android.ui.screens.notifications.NotificationsUi
import com.stepup.android.ui.screens.rewards.HistoryLoad
import com.stepup.android.ui.screens.rewards.LedgerFilter
import com.stepup.android.ui.screens.rewards.TotalsLoad
import com.stepup.android.ui.screens.rewards.WalletContent
import com.stepup.android.ui.screens.rewards.Web3State
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

/** 그룹 C(07 아이템 · 08 뽑기 · 12 지갑 · 13 알림함) — 시안과 같은 값으로 그린다. `-Pshots=1` 일 때만 돈다 */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [35], qualifiers = "ko-w393dp-h852dp-xxhdpi")
class GroupCShotsTest {
    @get:Rule val compose = createAndroidComposeRule<ComponentActivity>()

    @Before fun only() = Shots.require()

    @Composable
    private fun Header() {
        MainHeader(balance = 0.0, onOpenWallet = {}, modifier = Modifier.padding(start = 20.dp, end = 8.dp), onOpenNotifications = {})
    }

    @Composable
    private fun Items() {
        Column {
            Header()
            Column(Modifier.weight(1f)) {
                BagContent(
                    bag = Bag(
                        ItemKind.entries.map { BagItem(it, if (it == ItemKind.RUN_BOOSTER) 1.5 else if (it == ItemKind.REWARD_DOUBLE) 2.0 else if (it == ItemKind.ENERGY_DRINK) .5 else null, 0) },
                        event = null, boostUntilMillis = null,
                    ),
                    loading = false, failed = false, selected = null, onSelect = {}, onRetry = {},
                    shoePercent = null, energy = null, message = null, using = false, onUse = {},
                )
            }
        }
    }

    @Composable
    private fun Draw() {
        Column {
            Header()
            Column(Modifier.weight(1f)) {
                DrawHome(
                    bottomInset = 0.dp,
                    status = DrawStatus(
                        dailyLeft = 1, dailyTotal = 1, signupLeft = 15, signupGranted = 15, walletLinked = false, giftOnLink = 10,
                        giftLeft = 0, runLeft = 0, genesisLeft = 0, runProgressMeters = 0.0, runStepMeters = 0, runToday = 0,
                        runDailyCap = 0, chainPaused = false,
                    ),
                    loading = false, signedOut = false, pending = null, actions = DrawActions(), onSheet = {},
                    web3 = true,
                    knownOdds = DrawOdds(
                        free = listOf(Rarity.COMMON to 790, Rarity.RARE to 200, Rarity.EPIC to 10),
                        premium = listOf(Rarity.RARE to 600, Rarity.EPIC to 280, Rarity.LEGENDARY to 90, Rarity.REDLINE to 25, Rarity.FINISH to 5),
                    ),
                )
            }
        }
    }

    @Composable
    private fun Wallet() {
        WalletContent(
            totals = TotalsLoad.Ready(RewardTotals(0.0, 0.0, 0.0)),
            history = HistoryLoad.Ready(LedgerFilter.ALL, emptyList(), more = false),
            filter = LedgerFilter.ALL,
            web3 = Web3State.NotLinked,
            status = DrawStatus(
                dailyLeft = 1, dailyTotal = 1, signupLeft = 15, signupGranted = 15, walletLinked = false, giftOnLink = 10,
                giftLeft = 0, runLeft = 0, genesisLeft = 0, runProgressMeters = 0.0, runStepMeters = 0, runToday = 0,
                runDailyCap = 0, chainPaused = false,
            ),
            web3Enabled = true, walletLinkEnabled = true,
        )
    }

    @Composable
    private fun Inbox() {
        NotificationsContent(NotificationsUi(inbox = InboxLoad.Ready(emptyList(), 0)))
    }

    @Test fun items_light() = compose.shot("07_items", false, ShotSizes[0]) { Items() }
    @Test fun items_dark() = compose.shot("07_items", true, ShotSizes[0]) { Items() }
    @Test fun items_small() = compose.shot("07_items", false, ShotSizes[1]) { Items() }
    @Test fun items_small_dark() = compose.shot("07_items", true, ShotSizes[1]) { Items() }

    @Test fun draw_light() = compose.shot("08_draw", false, ShotSizes[0]) { Draw() }
    @Test fun draw_dark() = compose.shot("08_draw", true, ShotSizes[0]) { Draw() }
    @Test fun draw_small() = compose.shot("08_draw", false, ShotSizes[1]) { Draw() }
    @Test fun draw_small_dark() = compose.shot("08_draw", true, ShotSizes[1]) { Draw() }

    @Test fun wallet_light() = compose.shot("12_wallet", false, ShotSizes[0]) { Wallet() }
    @Test fun wallet_dark() = compose.shot("12_wallet", true, ShotSizes[0]) { Wallet() }
    @Test fun wallet_small() = compose.shot("12_wallet", false, ShotSizes[1]) { Wallet() }
    @Test fun wallet_small_dark() = compose.shot("12_wallet", true, ShotSizes[1]) { Wallet() }

    @Test fun inbox_light() = compose.shot("13_inbox", false, ShotSizes[0]) { Inbox() }
    @Test fun inbox_dark() = compose.shot("13_inbox", true, ShotSizes[0]) { Inbox() }
    @Test fun inbox_small() = compose.shot("13_inbox", false, ShotSizes[1]) { Inbox() }
    @Test fun inbox_small_dark() = compose.shot("13_inbox", true, ShotSizes[1]) { Inbox() }
}
