package com.stepup.android.ui.screens.splash

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
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
                ServiceLocator.boostRepository.recoverEnergyPurchases()
                ServiceLocator.boostRepository.purgeExpired()
                ServiceLocator.crewRepository.clearLegacy()
                ServiceLocator.notificationRepository.purgeLegacyInvites()
                ServiceLocator.communityRepository.clearLegacy()
                ServiceLocator.courseRepository.ensureSeeded()
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
            Modifier.fillMaxSize().safeDrawingPadding().padding(StepUpDesign.Gutter),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            BoxWithConstraints(Modifier.weight(1f).fillMaxWidth()) {
                Column(
                    Modifier.fillMaxWidth().verticalScroll(rememberScrollState()).heightIn(min = maxHeight).padding(vertical = 24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center,
                ) {
                    Wordmark(role = BrandLogoRole.Launch)
                    Spacer(Modifier.height(32.dp))
                    when (stage) {
                        LaunchStage.Loading -> {
                            CircularProgressIndicator(Modifier.size(28.dp).testTag("launch-loading"), color = Volt, strokeWidth = 2.dp)
                            Spacer(Modifier.height(20.dp))
                            Text(stringResource(R.string.splash_preparing), style = MaterialTheme.typography.bodyLarge, color = Silver, textAlign = TextAlign.Center)
                        }
                        LaunchStage.Error -> Text(stringResource(R.string.splash_prepare_failed), style = MaterialTheme.typography.bodyLarge, color = Silver, textAlign = TextAlign.Center)
                        LaunchStage.Reveal -> Unit
                    }
                }
            }
            if (stage == LaunchStage.Error) {
                PrimaryCta(text = stringResource(R.string.feed_retry), onClick = onRetry, modifier = Modifier.padding(bottom = 16.dp))
            }
        }
    }
}
