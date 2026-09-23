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
        val explanation = compose.activity.getString(R.string.notif_reward_unverified)
        compose.waitUntil(5_000) {
            compose.onAllNodesWithText(explanation).fetchSemanticsNodes().isNotEmpty()
        }
        compose.onNodeWithText(explanation).assertIsDisplayed()
        compose.onNodeWithText(compose.activity.getString(R.string.challenge_title)).performClick()
        compose.onNodeWithTag("challenge-primary-action").assertIsDisplayed()
        runBlocking {
            assertEquals(before, ServiceLocator.database.rewardDao().balanceNow(), 0.0)
            assertEquals(1, ServiceLocator.database.notificationDao().count())
        }
    }
}
