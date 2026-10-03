package com.stepup.android.ui.screens.splash

import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.*
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
import androidx.compose.ui.unit.sp
import com.stepup.android.R
import com.stepup.android.core.ServiceLocator
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import com.stepup.android.ui.components.RunBackdrop
import com.stepup.android.ui.components.RunSpinner
import com.stepup.android.ui.components.Wordmark
import com.stepup.android.ui.components.runTextStyle
import com.stepup.android.ui.components.runTone
import com.stepup.android.ui.screens.onboarding.BluePlainButton
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
 * 시작 화면(파란 톤 v4 ONB03 · ONB04) — 남색 바탕 가운데 원본 로고와 그 뒤 파란 빛, 아래로 준비 상태 한 줄(03) 또는
 * 준비하지 못함 · 다시 시도(04). 가짜 진행률 · 시간 · 보상은 없다 — 도는 표시는 실제로 기다리는 동안만 보인다.
 * 갤러리 · 시안 검사도 이 화면을 상태만 넣어 그린다. 실제 상태는 위의 초기화에서만 온다.
 */
@Composable
internal fun LaunchScene(stage: LaunchStage, onRetry: () -> Unit = {}) {
    val t = runTone()
    Box(Modifier.fillMaxSize().testTag("launch-scene")) {
        RunBackdrop(Modifier.fillMaxSize())
        // 로고 뒤 파란 빛 — 화면 가운데에서 번진다
        Box(
            Modifier.fillMaxSize().drawBehind {
                drawCircle(
                    Brush.radialGradient(
                        0f to t.cobalt.copy(alpha = if (t.dark) 0.55f else 0.22f), 1f to Color.Transparent,
                        center = Offset(size.width / 2f, size.height * 0.42f), radius = size.width * 0.62f,
                    ),
                    radius = size.width * 0.62f, center = Offset(size.width / 2f, size.height * 0.42f),
                )
            },
        )
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
                    Spacer(Modifier.height(64.dp))
                    when (stage) {
                        LaunchStage.Loading -> {
                            RunSpinner(Modifier.size(40.dp).testTag("launch-loading"))
                            Spacer(Modifier.height(22.dp))
                            // 실제로 기다리는 동안만 보인다 — 스크린리더에도 준비 중임을 알린다
                            Text(
                                stringResource(R.string.splash_preparing),
                                style = runTextStyle(18.sp, t.text, FontWeight.Medium, 1.45f),
                                textAlign = TextAlign.Center,
                                modifier = Modifier.semantics { liveRegion = LiveRegionMode.Polite },
                            )
                        }
                        LaunchStage.Error -> {
                            Text(
                                stringResource(R.string.onb_launch_failed_title),
                                style = runTextStyle(26.sp, t.text, FontWeight.ExtraBold, 1.3f),
                                textAlign = TextAlign.Center,
                                modifier = Modifier.testTag("launch-error").semantics { heading(); liveRegion = LiveRegionMode.Polite },
                            )
                            Spacer(Modifier.height(10.dp))
                            Text(
                                stringResource(R.string.onb_launch_failed_body),
                                style = runTextStyle(17.sp, t.label, FontWeight.Medium, 1.45f),
                                textAlign = TextAlign.Center,
                            )
                        }
                    }
                }
            }
            if (stage == LaunchStage.Error) {
                // 다시 시도 — 같은 준비 작업을 처음부터 다시 한다(성공으로 치고 넘어가지 않는다)
                BluePlainButton(
                    stringResource(R.string.feed_retry), onRetry,
                    Modifier.widthIn(max = 420.dp).fillMaxWidth().padding(horizontal = 24.dp).padding(bottom = 20.dp)
                        .testTag("launch-retry"),
                )
            }
        }
    }
}
