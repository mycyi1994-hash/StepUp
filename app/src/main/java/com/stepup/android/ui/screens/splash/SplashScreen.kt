package com.stepup.android.ui.screens.splash

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.stepup.android.R
import com.stepup.android.core.ServiceLocator
import com.stepup.android.ui.components.PrimaryCta
import com.stepup.android.ui.components.RunnerScene
import com.stepup.android.ui.components.Wordmark
import com.stepup.android.ui.experience.LocalMotion
import com.stepup.android.ui.theme.BrandLogoRole
import com.stepup.android.ui.theme.Night
import com.stepup.android.ui.theme.Silver
import com.stepup.android.ui.theme.StepUpDesign
import com.stepup.android.ui.theme.Volt
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withTimeoutOrNull

internal enum class LaunchStage { Loading, Reveal, Error }

/** Real readiness gates the reveal. A failed or timed-out preparation remains retryable. */
@Composable
fun SplashScreen(onReady: () -> Unit) {
    var stage by remember { mutableStateOf(LaunchStage.Loading) }
    var attempt by remember { mutableIntStateOf(0) }
    val ready by rememberUpdatedState(onReady)
    val motion = LocalMotion.current
    LaunchedEffect(attempt) {
        stage = LaunchStage.Loading
        val prepared = try {
            withTimeoutOrNull(14_000) {
                ServiceLocator.userPrefs.ensureRunnerUid()
                ServiceLocator.sneakerRepository.ensureStarter()
                ServiceLocator.boostRepository.purgeExpired()
                ServiceLocator.crewRepository.clearLegacy()
                ServiceLocator.notificationRepository.purgeLegacyInvites()
                ServiceLocator.communityRepository.clearLegacy()
                ServiceLocator.courseRepository.ensureSeeded()
                ServiceLocator.notificationRepository.seedWelcome()
                ServiceLocator.rewardRepository.balance.first()
                ServiceLocator.stepRepository.dailyGoal.first()
                ServiceLocator.sneakerRepository.equipped.first()
                true
            } ?: false
        } catch (cancelled: CancellationException) {
            throw cancelled
        } catch (_: Exception) {
            false
        }
        if (prepared) {
            stage = LaunchStage.Reveal
            delay(motion.duration(650).toLong())
            ready()
        } else {
            stage = LaunchStage.Error
        }
    }
    LaunchScene(stage, onRetry = { attempt++ })
}

/** Also used by deterministic gallery fixtures; production state comes only from initialization. */
@Composable
internal fun LaunchScene(stage: LaunchStage, onRetry: () -> Unit = {}) {
    val motion = LocalMotion.current
    val reveal by animateFloatAsState(
        if (stage == LaunchStage.Reveal) 1f else 0f,
        tween(motion.duration(480)), label = "launchReveal",
    )
    Box(Modifier.fillMaxSize().background(Night).testTag("launch-scene")) {
        RunnerScene(Modifier.fillMaxSize().graphicsLayer { alpha = reveal * 0.65f })
        Column(
            Modifier.align(Alignment.Center).padding(horizontal = StepUpDesign.Gutter),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Wordmark(role = BrandLogoRole.Launch)
            if (stage == LaunchStage.Loading) {
                Spacer(Modifier.height(32.dp))
                CircularProgressIndicator(
                    Modifier.size(24.dp).testTag("launch-loading"),
                    color = Volt, strokeWidth = 2.dp,
                )
            }
        }
        Column(
            Modifier.align(Alignment.BottomCenter).navigationBarsPadding()
                .padding(horizontal = StepUpDesign.Gutter, vertical = 40.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(20.dp),
        ) {
            if (stage == LaunchStage.Error) {
                Text(
                    stringResource(R.string.splash_prepare_failed),
                    color = Silver, textAlign = TextAlign.Center,
                )
                PrimaryCta(text = stringResource(R.string.feed_retry), onClick = onRetry)
            } else if (stage == LaunchStage.Loading) {
                Text(
                    stringResource(R.string.splash_preparing),
                    color = Silver, textAlign = TextAlign.Center,
                )
            }
        }
    }
}
