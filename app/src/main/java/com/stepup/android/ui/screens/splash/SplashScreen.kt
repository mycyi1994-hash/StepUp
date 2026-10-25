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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.contentDescription
import com.stepup.android.ui.background.Spots
import com.stepup.android.ui.background.TimeOfDay
import com.stepup.android.ui.background.rememberSpotPhoto
import com.stepup.android.ui.components.StepUpMotion
import com.stepup.android.ui.components.StepUpUi
import com.stepup.android.ui.components.Wordmark
import com.stepup.android.ui.experience.LocalMotion
import com.stepup.android.ui.theme.StepUpColors
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
 * 시작 화면(UI 최종 통합안 2026-10-25 · 01_시작화면) — 망원 한강공원 사진 위 워드마크가 왼쪽에서 들어오고, 아래 2dp 로딩 선.
 * 다크블루는 사진이 화면 전체(아래로 짙어짐), 화이트블루는 위쪽 사진이 흰 바닥으로 사라진다.
 * 로딩 선은 실제로 기다리는 동안만 돈다(가짜 진행률 · 시간 · 보상은 없다). 준비하지 못하면(04) 사진 위에 까닭 · 다시 시도.
 * 갤러리 · 시안 검사도 이 화면을 상태만 넣어 그린다. 실제 상태는 위의 초기화에서만 온다.
 */
@Composable
internal fun LaunchScene(stage: LaunchStage, onRetry: () -> Unit = {}) {
    val dark = StepUpColors.dark
    val ui = StepUpUi.colors
    val context = LocalContext.current
    val motion = LocalMotion.current
    val photo = rememberSpotPhoto(
        remember { Spots.byId(context, "mangwon") },
        time = if (dark) TimeOfDay.DUSK else TimeOfDay.DAY,
    )
    val photoAlpha by animateFloatAsState(if (photo != null) 1f else 0f, tween(motion.duration(300)), label = "launchPhoto")
    // 워드마크 — 왼쪽에서 미끄러져 들어온다(500ms, 감속)
    val enter = remember { Animatable(if (motion.reduced) 1f else 0f) }
    LaunchedEffect(Unit) {
        if (!motion.reduced) enter.animateTo(1f, tween(500, delayMillis = 150, easing = StepUpMotion.emphasizedDecelerate))
    }
    val ground = if (dark) Color(0xFF070B14) else Color.White
    Box(Modifier.fillMaxSize().background(ground).testTag("launch-scene")) {
        if (photo != null) {
            Image(
                photo.painter, null, contentScale = ContentScale.Crop, colorFilter = photo.colorFilter,
                modifier = (if (dark) Modifier.fillMaxSize() else Modifier.fillMaxWidth().fillMaxHeight(0.5f))
                    .graphicsLayer { alpha = photoAlpha },
            )
        }
        Box(
            Modifier.fillMaxSize().background(
                if (dark) Brush.verticalGradient(0f to Color.Black.copy(alpha = .08f), .55f to Color.Black.copy(alpha = .18f), 1f to ground.copy(alpha = .78f))
                else Brush.verticalGradient(0f to Color.White.copy(alpha = .08f), .24f to Color.White.copy(alpha = .22f), .40f to Color.White.copy(alpha = .9f), .46f to Color.White, 1f to Color.White),
            ),
        )
        Column(
            Modifier.fillMaxSize().safeDrawingPadding().padding(horizontal = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            BoxWithConstraints(Modifier.weight(1f).fillMaxWidth()) {
                val room = maxHeight
                Column(
                    Modifier.fillMaxWidth().verticalScroll(rememberScrollState()).heightIn(min = room),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    // 시안 — 워드마크 윗변이 화면 높이의 46% 쯤(390 / 851)
                    Spacer(Modifier.height(room * 0.44f))
                    Wordmark(
                        role = BrandLogoRole.Launch,
                        modifier = Modifier.graphicsLayer {
                            alpha = enter.value
                            translationX = (1f - enter.value) * -48.dp.toPx()
                        },
                    )
                    when (stage) {
                        LaunchStage.Loading -> Unit
                        LaunchStage.Error -> {
                            Spacer(Modifier.height(40.dp))
                            Text(
                                stringResource(R.string.onb_launch_failed_title),
                                style = StepUpUi.type.section.copy(fontSize = 24.sp), color = if (dark) Color.White else ui.label,
                                textAlign = TextAlign.Center,
                                modifier = Modifier.testTag("launch-error").semantics { heading(); liveRegion = LiveRegionMode.Polite },
                            )
                            Spacer(Modifier.height(8.dp))
                            Text(
                                stringResource(R.string.onb_launch_failed_body),
                                style = StepUpUi.type.body, color = if (dark) Color.White.copy(alpha = .8f) else ui.secondaryLabel,
                                textAlign = TextAlign.Center,
                            )
                        }
                    }
                    Spacer(Modifier.weight(1f))
                    if (stage == LaunchStage.Loading) {
                        // 실제로 기다리는 동안만 보인다 — 스크린리더에도 준비 중임을 알린다(글자는 화면에 쓰지 않는다)
                        val preparing = stringResource(R.string.splash_preparing)
                        LaunchLoadingLine(
                            Modifier.padding(bottom = 104.dp).testTag("launch-loading")
                                .semantics { contentDescription = preparing; liveRegion = LiveRegionMode.Polite },
                        )
                    }
                }
            }
            if (stage == LaunchStage.Error) {
                // 다시 시도 — 같은 준비 작업을 처음부터 다시 한다(성공으로 치고 넘어가지 않는다)
                BluePlainButton(
                    stringResource(R.string.feed_retry), onRetry,
                    Modifier.widthIn(max = 420.dp).fillMaxWidth().padding(bottom = 16.dp)
                        .testTag("launch-retry"),
                )
            }
        }
    }
}

/** 아래 로딩 선 — 120×2, 길(다크 흰 22% · 라이트 검정 12%) 위로 채움(다크 흰색 · 라이트 블루)이 왼쪽부터 차오르기를 되풀이한다 */
@Composable
private fun LaunchLoadingLine(modifier: Modifier = Modifier) {
    val dark = StepUpColors.dark
    val motion = LocalMotion.current
    val fill = remember { Animatable(if (motion.reduced) .64f else 0f) }
    LaunchedEffect(motion.reduced) {
        if (motion.reduced) return@LaunchedEffect
        while (true) {
            fill.snapTo(0f)
            fill.animateTo(1f, tween(1200, easing = StepUpMotion.countUp))
            kotlinx.coroutines.delay(260)
        }
    }
    Box(
        modifier.size(120.dp, 2.dp).clip(RoundedCornerShape(1.dp))
            .background(if (dark) Color.White.copy(alpha = .22f) else Color.Black.copy(alpha = .12f)),
    ) {
        Box(
            Modifier.fillMaxHeight().fillMaxWidth(fill.value.coerceIn(0f, 1f))
                .background(if (dark) Color.White else StepUpUi.colors.primary),
        )
    }
}
