package com.stepup.android.ui.screens.splash

import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import com.stepup.android.R
import com.stepup.android.core.ServiceLocator
import com.stepup.android.ui.components.OnboardingPrimaryButton
import com.stepup.android.ui.components.S2Stage
import com.stepup.android.ui.components.Wordmark
import com.stepup.android.ui.components.settingsPalette
import com.stepup.android.ui.theme.BrandLogoRole
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withTimeoutOrNull

/** 앱 준비 중(시안 03) · 준비하지 못함(시안 04) */
internal enum class LaunchStage { Loading, Error }

/**
 * 실제 준비가 끝나야 다음 화면으로 간다. 실패하거나 14초 안에 끝나지 않으면 다시 시도할 수 있게 둔다.
 *
 * 준비가 끝나면 곧바로 넘긴다 — 장식용 최소 대기 시간을 두지 않는다(들어가는 불투명도 전환은 StepUpRoot 가 한다).
 */
@Composable
fun SplashScreen(onReady: () -> Unit) {
    var stage by remember { mutableStateOf(LaunchStage.Loading) }
    var attempt by remember { mutableIntStateOf(0) }
    val ready by rememberUpdatedState(onReady)
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
        if (prepared) ready() else stage = LaunchStage.Error
    }
    LaunchScene(stage, onRetry = { attempt++ })
}

/**
 * 시작 화면 — 가운데 로고, 아래로 준비 상태 한 줄(03) 또는 준비하지 못함 · 다시 시도(04).
 * 갤러리 · 시안 검사도 이 화면을 상태만 넣어 그린다. 실제 상태는 위의 초기화에서만 온다.
 */
@Composable
internal fun LaunchScene(stage: LaunchStage, onRetry: () -> Unit = {}) {
    val p = settingsPalette()
    Box(Modifier.fillMaxSize().testTag("launch-scene")) {
        S2Stage(Modifier.fillMaxSize())
        Column(
            Modifier.fillMaxSize().safeDrawingPadding().padding(horizontal = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            BoxWithConstraints(Modifier.weight(1f).fillMaxWidth()) {
                Column(
                    Modifier.fillMaxWidth().verticalScroll(rememberScrollState()).heightIn(min = maxHeight).padding(vertical = 24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center,
                ) {
                    Wordmark(role = BrandLogoRole.Launch)
                    Spacer(Modifier.height(40.dp))
                    when (stage) {
                        LaunchStage.Loading -> {
                            CircularProgressIndicator(
                                Modifier.size(24.dp).testTag("launch-loading"), color = p.accent, strokeWidth = 2.dp,
                                trackColor = p.accent.copy(alpha = 0.2f),
                            )
                            Spacer(Modifier.height(20.dp))
                            // 실제로 기다리는 동안만 보인다 — 스크린리더에도 준비 중임을 알린다
                            Text(
                                stringResource(R.string.splash_preparing), color = p.secondary, fontSize = 15.sp,
                                textAlign = TextAlign.Center, lineHeight = 1.45.em,
                                modifier = Modifier.semantics { liveRegion = LiveRegionMode.Polite },
                            )
                        }
                        LaunchStage.Error -> {
                            Text(
                                stringResource(R.string.onb_launch_failed_title), color = p.text, fontSize = 22.sp,
                                fontWeight = FontWeight.SemiBold, textAlign = TextAlign.Center, lineHeight = 1.35.em,
                                modifier = Modifier.testTag("launch-error").semantics { heading(); liveRegion = LiveRegionMode.Polite },
                            )
                            Spacer(Modifier.height(12.dp))
                            Text(
                                stringResource(R.string.onb_launch_failed_body), color = p.secondary, fontSize = 15.sp,
                                textAlign = TextAlign.Center, lineHeight = 1.45.em,
                            )
                        }
                    }
                }
            }
            if (stage == LaunchStage.Error) {
                // 다시 시도 — 같은 준비 작업을 처음부터 다시 한다(성공으로 치고 넘어가지 않는다)
                OnboardingPrimaryButton(
                    stringResource(R.string.feed_retry), onRetry,
                    Modifier.fillMaxWidth().padding(bottom = 16.dp).testTag("launch-retry"),
                )
            }
        }
    }
}
