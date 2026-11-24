package com.stepup.android.shots

import androidx.activity.ComponentActivity
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.material3.Text
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.stepup.android.domain.DailyRewards
import com.stepup.android.domain.DailyRewardsBar
import com.stepup.android.domain.Faction
import com.stepup.android.domain.Rarity
import com.stepup.android.domain.RewardTick
import com.stepup.android.domain.RewardTickKind
import com.stepup.android.domain.Sneaker
import com.stepup.android.domain.StepSurgeStatus
import com.stepup.android.ui.components.MainHeader
import com.stepup.android.ui.components.RunButton
import com.stepup.android.ui.components.RunSheetPanel
import com.stepup.android.ui.components.runNumberStyle
import com.stepup.android.ui.components.runTone
import com.stepup.android.ui.screens.home.GpsReadiness
import com.stepup.android.ui.screens.home.HomeContent
import com.stepup.android.ui.screens.home.TodayActivity
import com.stepup.android.ui.screens.walk.FreeRunShoeCheckContent
import com.stepup.android.ui.screens.walk.RunStartMenuContent
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import java.time.LocalDate

/** 재디자인 A 그룹(01 러닝 홈 · 02 오늘의 활동 · 03 러닝 시작 · 04 · 04b 출발 전 신발 확인) — 시안과 같은 값으로 그린다 */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [35], qualifiers = "ko-rKR-w393dp-h852dp-xxhdpi")
class GroupAShotsTest {
    @get:Rule val compose = createAndroidComposeRule<ComponentActivity>()

    @Before fun only() = Shots.require()

    private val today: LocalDate get() = LocalDate.now(DailyRewards.GAME_ZONE)

    private fun daily() = DailyRewards(
        rule = DailyRewards.RULE_REWARD_STEPS, day = today, rewardStepsToday = 0, goal = 8_000, goalNext = null,
        goalPaid = null, goalBonus = 20.0, streak = 1, streakBest = 0, streakBoxIn = 6,
        ticks = listOf(
            RewardTick(2_000, RewardTickKind.BOX, false),
            RewardTick(4_000, RewardTickKind.BOX, false),
            RewardTick(8_000, RewardTickKind.GOAL, false),
        ),
        surge = StepSurgeStatus(0, 80_000, 250.0, false), recent = emptyList(),
        stepsWeek = 0, weekTarget = 25_000, weekGot = false,
    )

    private fun shoe(durability: Int) = Sneaker(
        id = 9_401, faction = Faction.FIRE, rarity = Rarity.COMMON, variant = 0, level = 1, mintNumber = 12,
        luck = 1.0, comfort = 1.0, durability = durability, equipped = true, acquiredAt = 1_790_000_000_000L,
        server = com.stepup.android.domain.ServerStats(
            origin = "FREE", efficiencyBps = 0, comfortBps = 0, durabilityPts = durability.toDouble(), maxLevel = 20,
            status = "ACTIVE", chainState = "NONE", canWithdraw = false, upgradeCost = 0.0, repairCostPerPoint = 1.0, genesisNo = 0,
            durabilityNow = 155 * durability / 100, durabilityMax = 155, durabilityBase = 155,
        ),
    )

    private fun each(name: String, body: @androidx.compose.runtime.Composable () -> Unit, dark: Boolean, size: ShotSize) =
        compose.shot(name, dark, size, body)

    // ── 01 러닝 홈 ──
    private val home: @androidx.compose.runtime.Composable () -> Unit = {
        Column(Modifier.fillMaxSize().statusBarsPadding()) {
            MainHeader(0.0, {}, Modifier.padding(horizontal = 20.dp), onOpenNotifications = {})
            Box(Modifier.weight(1f)) {
                HomeContent(
                    loaded = true, todaySteps = 0, goal = 8_000, hasPermission = true, firstHome = false, running = false,
                    largeText = false, earned = 0.0, km = 0.0, weekRuns = 0, onStartRun = {}, onRequestPermission = {},
                    onOpenDetails = {}, photoIndex = 2, gps = GpsReadiness.READY,
                    bar = DailyRewardsBar.of(daily(), today, 0), goalNotice = true,
                )
            }
        }
    }

    @Test fun home_light() = each("01_home", home, false, ShotSizes[0])
    @Test fun home_dark() = each("01_home", home, true, ShotSizes[0])
    @Test fun home_small() = each("01_home", home, false, ShotSizes[1])

    // ── 02 오늘의 활동 시트 ──
    private val activity: @androidx.compose.runtime.Composable () -> Unit = {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.BottomCenter) {
            RunSheetPanel(onClose = {}, modifier = Modifier.statusBarsPadding().padding(top = 24.dp)) {
                Column(Modifier.fillMaxWidth().weight(1f, fill = false).verticalScroll(rememberScrollState())) {
                    TodayActivity(
                        steps = 0, goal = 8_000, loaded = true, hasPermission = true, km = 0.0, runSec = 0, earned = 0.0,
                        pending = false, onRequestPermission = {}, bar = DailyRewardsBar.of(daily(), today, 0), daily = daily(),
                    )
                }
                Spacer(Modifier.height(14.dp))
                RunButton("확인", {})
            }
        }
    }

    @Test fun activity_light() = each("02_activity", activity, false, ShotSizes[0])
    @Test fun activity_dark() = each("02_activity", activity, true, ShotSizes[0])
    @Test fun activity_small() = each("02_activity", activity, false, ShotSizes[1])

    // ── 03 러닝 시작 메뉴 ──
    private val menu: @androidx.compose.runtime.Composable () -> Unit = {
        RunStartMenuContent(
            onBack = {}, onFreeRun = {}, onGoals = {}, onDiet = null, onCrew = {}, onCourse = null, balance = 0.0,
            onOpenWallet = {}, onCoaching = {}, onLeaderboard = {},
        )
    }

    @Test fun menu_light() = each("03_menu", menu, false, ShotSizes[0])
    @Test fun menu_dark() = each("03_menu", menu, true, ShotSizes[0])
    @Test fun menu_small() = each("03_menu", menu, false, ShotSizes[1])

    // ── 04 · 04b 출발 전 신발 확인 ──
    private fun check(durability: Int): @androidx.compose.runtime.Composable () -> Unit = {
        Box(Modifier.fillMaxSize()) {
            Text("0.00", style = runNumberStyle(110.sp, runTone().text), modifier = Modifier.statusBarsPadding().padding(start = 16.dp, top = 60.dp))
            val s = shoe(durability)
            FreeRunShoeCheckContent(s, {}, {}, shoes = listOf(s), onRepair = {}, energySteps = 20_000)
        }
    }

    @Test fun check_light() = each("04_check", check(100), false, ShotSizes[0])
    @Test fun check_dark() = each("04_check", check(100), true, ShotSizes[0])
    @Test fun check_small() = each("04_check", check(100), false, ShotSizes[1])
    @Test fun worn_light() = each("04b_worn", check(0), false, ShotSizes[0])
    @Test fun worn_dark() = each("04b_worn", check(0), true, ShotSizes[0])
    @Test fun worn_small() = each("04b_worn", check(0), false, ShotSizes[1])
}

