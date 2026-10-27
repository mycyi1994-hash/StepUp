package com.stepup.android.ui.screens.splash

import android.app.Activity
import android.provider.Settings
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.view.WindowCompat
import com.stepup.android.R
import com.stepup.android.core.ServiceLocator
import com.stepup.android.ui.components.StepUpMotion
import com.stepup.android.ui.components.StepUpUi
import com.stepup.android.ui.experience.LocalFeedback
import com.stepup.android.ui.experience.LocalMotion
import com.stepup.android.ui.screens.onboarding.BluePlainButton
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withTimeoutOrNull
import kotlin.math.roundToInt

/** 앱 준비 중(시안 03) · 준비하지 못함(시안 04) */
internal enum class LaunchStage { Loading, Error }

/** 인트로 영상 재생 — 소리를 낼지와 끝났을 때 할 일 */
internal class IntroPlayback(val sound: Boolean, val onEnd: () -> Unit)

/** 인트로를 어떻게 보일지 — 설정을 읽기 전(바탕만) · 영상 · 마지막 장면 그림 */
private enum class IntroPlan { Deciding, Play, Still }

/**
 * 실제 준비와 인트로 영상(4.6초)이 둘 다 끝나야 다음 화면으로 간다. 준비가 실패하거나 14초 안에 끝나지 않으면 다시 시도할 수 있게 둔다.
 *
 * 준비가 끝난 뒤에는 화면을 누르면 인트로를 건너뛴다. 동작 줄이기면 영상 없이 마지막 장면 그림만(준비가 끝나면 바로 넘어간다).
 * 소리는 설정의 효과음이 켜져 있고 기기가 조용히 하라고 하지 않을 때만 — 무음 · 진동 · 방해 금지 · 다른 음악 중이면 소리 없이.
 */
@Composable
fun SplashScreen(onReady: () -> Unit) {
    var stage by remember { mutableStateOf(LaunchStage.Loading) }
    var attempt by remember { mutableIntStateOf(0) }
    var prepared by remember { mutableStateOf(false) }
    // 화면을 돌려도 인트로를 처음부터 다시 틀지 않는다
    var introDone by rememberSaveable { mutableStateOf(false) }
    var plan by remember { mutableStateOf(IntroPlan.Deciding) }
    var sound by remember { mutableStateOf(false) }
    val ready by rememberUpdatedState(onReady)
    val context = LocalContext.current
    val feedback = LocalFeedback.current
    LaunchedEffect(Unit) {
        if (introDone) {
            plan = IntroPlan.Still
            return@LaunchedEffect
        }
        val prefs = withTimeoutOrNull(1_500) { ServiceLocator.userPrefs.experience.first() }
        val systemReduced = Settings.Global.getFloat(context.contentResolver, Settings.Global.ANIMATOR_DURATION_SCALE, 1f) == 0f
        if (systemReduced || prefs?.reducedMotion == true) {
            plan = IntroPlan.Still
            introDone = true
        } else {
            sound = prefs?.sounds == true && feedback?.deviceAllowsSound() == true
            plan = IntroPlan.Play
        }
    }
    // 영상이 어떤 까닭으로 끝을 알리지 못해도 붙잡혀 있지 않게(영상 4.6초 + 여유)
    LaunchedEffect(plan) {
        if (plan == IntroPlan.Play) {
            kotlinx.coroutines.delay(8_000)
            introDone = true
        }
    }
    LaunchedEffect(attempt) {
        stage = LaunchStage.Loading
        val done = try {
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
        if (done) prepared = true else stage = LaunchStage.Error
    }
    LaunchedEffect(prepared, introDone) {
        if (prepared && introDone) ready()
    }
    if (plan == IntroPlan.Deciding) {
        Box(Modifier.fillMaxSize().background(LaunchIntroFrame.Ground))
        return
    }
    // 영상이 끝난 뒤 바로 넘어가면 멈춘 마지막 장면을 그대로 둔다 — 기다릴 때 · 실패했을 때만 그림 + 진짜 로딩 줄로 바꾼다.
    // 한 번 그림으로 바꾸면 영상으로 돌아가지 않는다(다시 틀지 않게)
    val waitingAfterIntro = introDone && (!prepared || stage == LaunchStage.Error)
    var videoGone by remember { mutableStateOf(false) }
    SideEffect { if (waitingAfterIntro) videoGone = true }
    val keepVideo = plan == IntroPlan.Play && !videoGone && !waitingAfterIntro
    LaunchScene(
        stage = if (introDone) stage else LaunchStage.Loading,
        onRetry = { attempt++ },
        intro = if (keepVideo) IntroPlayback(sound) { introDone = true } else null,
        onSkip = if (prepared && !introDone) ({ introDone = true }) else null,
    )
}

/**
 * 시작 화면(2026-10-27 인트로 영상) — 밤 다리 · 발자국 · STEPUP 로고. 테마와 상관없이 늘 이 어두운 장면이다.
 * [intro] 가 있으면 영상을, 없으면 마지막 장면 그림(로딩 줄을 지운 것) 위에 진짜 로딩 줄을 같은 자리에 그린다.
 * 로딩 줄은 실제로 기다리는 동안만 돈다(가짜 진행률 · 시간 · 보상은 없다). 준비하지 못하면(04) 그림 위에 까닭 · 다시 시도.
 * 갤러리 · 시안 검사도 이 화면을 상태만 넣어 그린다. 실제 상태는 위의 초기화에서만 온다.
 */
@Composable
internal fun LaunchScene(
    stage: LaunchStage,
    onRetry: () -> Unit = {},
    intro: IntroPlayback? = null,
    onSkip: (() -> Unit)? = null,
) {
    LightSystemBarIcons()
    val skip by rememberUpdatedState(onSkip)
    BoxWithConstraints(
        Modifier.fillMaxSize().background(LaunchIntroFrame.Ground).testTag("launch-scene")
            .pointerInput(Unit) { detectTapGestures { skip?.invoke() } },
    ) {
        val boxW = constraints.maxWidth.toFloat()
        val boxH = constraints.maxHeight.toFloat()
        if (intro != null) {
            LaunchIntroVideo(intro.sound, intro.onEnd, Modifier.fillMaxSize().testTag("launch-intro"))
        } else {
            Image(
                painterResource(R.drawable.launch_intro_still), null,
                contentScale = if (LaunchIntroFrame.fills(boxW, boxH)) ContentScale.Crop else ContentScale.Fit,
                modifier = Modifier.fillMaxSize(),
            )
            if (stage == LaunchStage.Loading) {
                // 영상 속 로딩 줄 자리 — 그림이 놓인 크기에서 다시 잰다
                val density = LocalDensity.current
                val scale = LaunchIntroFrame.scale(boxW, boxH)
                val shownW = LaunchIntroFrame.WIDTH * scale
                val shownH = LaunchIntroFrame.HEIGHT * scale
                val lineW = shownW * LaunchIntroFrame.LINE_WIDTH
                val lineH = with(density) { 2.dp.toPx() }
                val lineY = ((boxH - shownH) / 2f + shownH * LaunchIntroFrame.LINE_Y)
                    .coerceAtMost(boxH - with(density) { 48.dp.toPx() }) - lineH / 2f
                // 실제로 기다리는 동안만 보인다 — 스크린리더에도 준비 중임을 알린다(글자는 화면에 쓰지 않는다)
                val preparing = stringResource(R.string.splash_preparing)
                LaunchLoadingLine(
                    Modifier
                        .offset { IntOffset(((boxW - lineW) / 2f).roundToInt(), lineY.roundToInt()) }
                        .size(with(density) { lineW.toDp() }, 2.dp)
                        .testTag("launch-loading")
                        .semantics { contentDescription = preparing; liveRegion = LiveRegionMode.Polite },
                )
            }
        }
        if (stage == LaunchStage.Error) {
            Box(Modifier.fillMaxSize().background(Color.Black.copy(alpha = .45f)))
            Column(
                Modifier.fillMaxSize().safeDrawingPadding().padding(horizontal = 24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Column(
                    Modifier.weight(1f).fillMaxWidth().verticalScroll(rememberScrollState()),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center,
                ) {
                    // 로고(화면 높이 절반쯤) 아래에 까닭을 둔다
                    Spacer(Modifier.height(with(LocalDensity.current) { (boxH * 0.56f).toDp() }))
                    Text(
                        stringResource(R.string.onb_launch_failed_title),
                        style = StepUpUi.type.section.copy(fontSize = 24.sp), color = Color.White,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.testTag("launch-error").semantics { heading(); liveRegion = LiveRegionMode.Polite },
                    )
                    Spacer(Modifier.height(8.dp))
                    Text(
                        stringResource(R.string.onb_launch_failed_body),
                        style = StepUpUi.type.body, color = Color.White.copy(alpha = .8f),
                        textAlign = TextAlign.Center,
                    )
                }
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

/** 시작 화면은 늘 어두워 상태바 · 내비바 아이콘을 밝게 — 떠나면 앞의 값으로 되돌린다 */
@Composable
private fun LightSystemBarIcons() {
    val view = LocalView.current
    if (view.isInEditMode) return
    DisposableEffect(view) {
        val window = (view.context as? Activity)?.window
        val controller = window?.let { WindowCompat.getInsetsController(it, view) }
        val status = controller?.isAppearanceLightStatusBars
        val navigation = controller?.isAppearanceLightNavigationBars
        controller?.isAppearanceLightStatusBars = false
        controller?.isAppearanceLightNavigationBars = false
        onDispose {
            if (controller != null) {
                status?.let { controller.isAppearanceLightStatusBars = it }
                navigation?.let { controller.isAppearanceLightNavigationBars = it }
            }
        }
    }
}

/** 아래 로딩 줄 — 영상 속 줄 자리 · 너비, 길(흰 22%) 위로 흰 채움이 왼쪽부터 차오르기를 되풀이한다 */
@Composable
private fun LaunchLoadingLine(modifier: Modifier = Modifier) {
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
    Box(modifier.clip(RoundedCornerShape(1.dp)).background(Color.White.copy(alpha = .22f))) {
        Box(Modifier.fillMaxHeight().fillMaxWidth(fill.value.coerceIn(0f, 1f)).background(Color.White))
    }
}
