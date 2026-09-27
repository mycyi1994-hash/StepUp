package com.stepup.android

import androidx.activity.ComponentActivity
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import com.stepup.android.core.ServiceLocator
import com.stepup.android.ui.MainScaffold
import com.stepup.android.ui.Routes
import com.stepup.android.ui.experience.ExperienceProvider
import com.stepup.android.ui.theme.StepUpTheme
import com.stepup.android.ui.theme.ThemeMode
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

class NotificationNavigationTest {
    @get:Rule val compose = createAndroidComposeRule<ComponentActivity>()

    @Test fun legacyRewardOpensChallengesWithoutCreditingBalance() {
        val before = runBlocking {
            ServiceLocator.userPrefs.setReducedMotion(true)
            ServiceLocator.userPrefs.setGuideSeen()
            ServiceLocator.sneakerRepository.ensureStarter()
            ServiceLocator.database.notificationDao().clear()
            TestData.seedWelcomeNotification()
            ServiceLocator.database.rewardDao().balanceNow()
        }
        compose.setContent {
            StepUpTheme(ThemeMode.DARK) {
                ExperienceProvider { MainScaffold(initialRoute = Routes.NOTIFICATIONS) }
            }
        }
        val title = compose.activity.getString(R.string.reward_prev_title)
        compose.waitUntil(5_000) {
            compose.onAllNodesWithText(title).fetchSemanticsNodes().isNotEmpty()
        }
        // 알림함에 들어온 것만으로는 읽음이 되지 않는다
        runBlocking { assertEquals(1, ServiceLocator.database.notificationDao().observeUnreadCount().first()) }
        compose.onNodeWithText(title).performClick()
        compose.waitUntil(5_000) {
            compose.onAllNodesWithTag("sheet-unverified-reward").fetchSemanticsNodes().isNotEmpty()
        }
        compose.onNodeWithText(compose.activity.getString(R.string.reward_check_title)).assertIsDisplayed()
        compose.onNodeWithTag("sheet-open-challenges").performClick()
        compose.waitUntil(5_000) {
            compose.onAllNodesWithTag("challenge-primary-action").fetchSemanticsNodes().isNotEmpty()
        }
        compose.onNodeWithTag("challenge-primary-action").assertIsDisplayed()
        runBlocking {
            assertEquals(before, ServiceLocator.database.rewardDao().balanceNow(), 0.0)
            assertEquals(1, ServiceLocator.database.notificationDao().count())
            // 열어 본 것만 남는다 — 보상을 받은 것(처리)으로 바뀌지 않는다
            val row = ServiceLocator.database.notificationDao().observeAll(100).first().single()
            assertEquals(true, row.read)
            assertEquals(false, row.actioned)
        }
    }
}
