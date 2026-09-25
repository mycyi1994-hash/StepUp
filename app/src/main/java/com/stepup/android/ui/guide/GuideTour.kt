package com.stepup.android.ui.guide

import androidx.activity.compose.BackHandler
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.boundsInRoot
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.stepup.android.R
import com.stepup.android.ui.components.GhostButton
import com.stepup.android.ui.components.VoltButton
import com.stepup.android.ui.theme.Overlay
import com.stepup.android.ui.theme.Scrim
import com.stepup.android.ui.theme.ScrimAlpha
import com.stepup.android.ui.theme.Silver
import com.stepup.android.ui.theme.Snow
import com.stepup.android.ui.theme.VoltSoft

/**
 * 스포트라이트 가이드 투어.
 *
 * 실제 화면 위에 어두운 오버레이를 깔고, 설명하려는 요소만 밝게 뚫어
 * 그 옆에 설명 창을 띄운다. 스텝이 다른 탭으로 넘어가면 탭도 자동 전환된다.
 */

data class GuideStep(
    /**
     * [Modifier.guideTarget]로 등록한 대상 키.
     * 대상의 배치가 준비되지 않았으면 설명만 보여준다.
     */
    val key: String,
    /** 이 스텝이 속한 하단 탭 라우트 */
    val tabRoute: String,
    val titleRes: Int,
    val bodyRes: Int,
)

object GuideTour {

    var active by mutableStateOf(false)
        private set

    var stepIndex by mutableIntStateOf(0)
        private set

    /** 화면 요소들이 자기 위치를 등록하는 곳 (루트 좌표) */
    val bounds = mutableStateMapOf<String, Rect>()

    object Targets {
        const val HOME_STEPS = "home_steps"
        const val HOME_TOKEN = "home_token"
        const val HOME_ENERGY = "home_energy"
        const val HOME_START_RUN = "home_start_run"
        const val COMMUNITY_SEGMENTS = "community_segments"
        const val COMMUNITY_RANKING = "community_ranking"
        const val COMMUNITY_WRITE = "community_write"
        const val ITEMS_EQUIPPED = "items_equipped"
        const val ITEMS_MINT = "items_mint"
        const val ITEMS_COLLECTION = "items_collection"
        const val EVENTS_FEATURED = "events_featured"
        /** 러닝 홈의 챌린지 · 소식 바로가기 */
        const val HOME_SHORTCUTS = "home_shortcuts"
        /** 꾸미기의 캐릭터 미리보기 */
        const val CUSTOMIZE_PREVIEW = "customize_preview"
        const val PROFILE_AVATAR = "profile_avatar"
        const val PROFILE_ACHIEVEMENTS = "profile_achievements"

        /**
         * 하단 탭 버튼.
         *
         * 기능을 설명하기 전에 **그 기능이 어느 버튼 안에 있는지**부터
         * 보여주기 위한 것이다. 설명만 읽고 화면을 덮으면, 다음에 다시
         * 찾아가지 못해서 그 기능은 없는 것이나 같아진다.
         */
        fun tab(route: String) = "tab_$route"
    }

    /** Three entry points for the character-free version. */
    val steps: List<GuideStep> = listOf(
        // One useful action per main destination; advanced features remain in the app.
        GuideStep(Targets.HOME_START_RUN, "home", R.string.tour3_title, R.string.tour3_body),
        GuideStep(Targets.tab("customize"), "customize", R.string.tour_customize_title, R.string.tour_customize_body),
        GuideStep(Targets.tab("profile"), "profile", R.string.tour11_title, R.string.tour11_body),
    )

    val current: GuideStep? get() = if (active) steps.getOrNull(stepIndex) else null

    fun start() {
        // 이전 실행에서 남은 좌표로 엉뚱한 곳에 구멍이 뚫리지 않게 비운다
        bounds.clear()
        stepIndex = 0
        active = true
    }

    /** 다음 스텝. 마지막이었다면 false를 돌려주고 투어를 끝낸다. */
    fun advance(): Boolean {
        if (stepIndex < steps.lastIndex) {
            stepIndex++
            return true
        }
        active = false
        return false
    }

    /** 이전 스텝. 첫 스텝이면 아무 일도 하지 않고 false. */
    fun back(): Boolean {
        if (stepIndex == 0) return false
        stepIndex--
        return true
    }

    fun stop() {
        active = false
    }
}

/**
 * 이 요소를 가이드 투어 스포트라이트 대상으로 등록한다.
 * 화면에서 사라지면 등록도 해제해, 옛 좌표에 구멍이 뚫리는 일을 막는다.
 */
fun Modifier.guideTarget(key: String): Modifier = composed {
    DisposableEffect(key) {
        onDispose { GuideTour.bounds.remove(key) }
    }
    Modifier.onGloballyPositioned { GuideTour.bounds[key] = it.boundsInRoot() }
}

/**
 * 투어 오버레이 — MainScaffold 최상단에 올린다.
 *
 * @param onSwitchTab 스텝의 탭으로 전환
 * @param onFinished 투어 종료(완주·건너뛰기 모두)
 */
@Composable
fun GuideOverlay(
    onSwitchTab: (String) -> Unit,
    onFinished: () -> Unit,
) {
    val step = GuideTour.current ?: return
    val isLast = GuideTour.stepIndex == GuideTour.steps.lastIndex

    // 투어 중 뒤로가기는 이전 스텝으로. 첫 스텝에서만 투어를 닫는다.
    //
    // 예전에는 어느 스텝에서든 뒤로가기가 투어를 통째로 끝냈다. 한 장을
    // 놓쳐서 돌아가려던 사람이 투어를 잃는 것은 손해가 너무 크다.
    BackHandler {
        if (!GuideTour.back()) {
            GuideTour.stop()
            onFinished()
        }
    }

    // 스텝의 탭으로 자동 전환
    LaunchedEffect(step.tabRoute) { onSwitchTab(step.tabRoute) }

    val target: Rect? = GuideTour.bounds[step.key]
    val screenHeightDp = LocalConfiguration.current.screenHeightDp.toFloat()

    // 테두리가 천천히 숨 쉰다. 정지한 사각형은 배경으로 읽히지만, 움직이는
    // 것은 눈이 먼저 찾는다 — "여기를 보세요"를 글로 쓰지 않고 전달하는 방법이다.
    val pulse = if (com.stepup.android.ui.experience.LocalMotion.current.decorative) {
        val animated by rememberInfiniteTransition(label = "guidePulse").animateFloat(
            initialValue = 0f, targetValue = 1f,
            animationSpec = infiniteRepeatable(animation = tween(1400, easing = LinearEasing), repeatMode = RepeatMode.Restart),
            label = "guidePulseValue",
        )
        animated
    } else 0f

    fun finish() {
        GuideTour.stop()
        onFinished()
    }

    Box(Modifier.fillMaxSize()) {
        // 딤 + 대상 구멍 — 탭하면 다음으로
        Canvas(
            Modifier
                .fillMaxSize()
                .graphicsLayer(compositingStrategy = CompositingStrategy.Offscreen)
                .pointerInput(step.key) {
                    detectTapGestures {
                        if (!GuideTour.advance()) onFinished()
                    }
                },
        ) {
            // 막은 두 테마 모두 어둡다. 예전에는 Snow 를 썼는데, 그 색은
            // 테마를 따라 뒤집히는 **글자색**이라 어두운 테마에서 화면이
            // 흰 막으로 덮였다. 그 위에 얹힌 흰 버튼들이 그대로 사라졌다.
            drawRect(Scrim.copy(alpha = ScrimAlpha))
            if (target != null) {
                val pad = 7.dp.toPx()
                drawRoundRect(
                    color = Color.Transparent,
                    topLeft = Offset(target.left - pad, target.top - pad),
                    size = Size(target.width + pad * 2, target.height + pad * 2),
                    cornerRadius = CornerRadius(22.dp.toPx()),
                    blendMode = BlendMode.Clear,
                )
            }
        }

        // 대상 주위 볼트 테두리 + 퍼져 나가는 고리
        if (target != null) {
            Canvas(Modifier.fillMaxSize()) {
                val pad = 7.dp.toPx()
                drawRoundRect(
                    // 어두운 막 위에서는 밝은 쪽 파랑이라야 테두리가 보인다
                    color = VoltSoft,
                    topLeft = Offset(target.left - pad, target.top - pad),
                    size = Size(target.width + pad * 2, target.height + pad * 2),
                    cornerRadius = CornerRadius(22.dp.toPx()),
                    style = Stroke(width = 2.dp.toPx()),
                )
                // 물결처럼 한 겹 더 퍼졌다 사라진다
                val spread = pad + 14.dp.toPx() * pulse
                drawRoundRect(
                    color = VoltSoft.copy(alpha = 0.55f * (1f - pulse)),
                    topLeft = Offset(target.left - spread, target.top - spread),
                    size = Size(target.width + spread * 2, target.height + spread * 2),
                    cornerRadius = CornerRadius(22.dp.toPx() + 14.dp.toPx() * pulse),
                    style = Stroke(width = 2.dp.toPx()),
                )
            }
        }

        val density = androidx.compose.ui.platform.LocalDensity.current
        val targetInLowerHalf = target?.let { with(density) { it.center.y.toDp().value } > screenHeightDp * .5f } ?: true
        Column(
            Modifier.align(if (targetInLowerHalf) Alignment.TopCenter else Alignment.BottomCenter)
                .navigationBarsPadding()
                .padding(start = 20.dp, end = 20.dp, top = 56.dp, bottom = 24.dp)
                .widthIn(max = 560.dp).fillMaxWidth()
                .heightIn(max = (screenHeightDp * .68f).dp)
                .clip(RoundedCornerShape(24.dp)).background(Overlay)
                .border(1.dp, VoltSoft.copy(alpha = .55f), RoundedCornerShape(24.dp))
                .padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Text(stringResource(step.titleRes), modifier = Modifier.weight(1f).testTag("guide-step-title"),
                    style = MaterialTheme.typography.titleLarge, color = Snow)
                Text("${GuideTour.stepIndex + 1} / ${GuideTour.steps.size}",
                    style = MaterialTheme.typography.labelLarge, color = Silver, modifier = Modifier.padding(start = 12.dp))
            }
            Text(stringResource(step.bodyRes), style = MaterialTheme.typography.bodyLarge, color = Silver,
                modifier = Modifier.weight(1f, fill = false).verticalScroll(rememberScrollState()))
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                GuideTour.steps.forEachIndexed { index, _ ->
                    Box(Modifier.weight(1f).height(4.dp).clip(RoundedCornerShape(2.dp))
                        .background(if (index <= GuideTour.stepIndex) com.stepup.android.ui.theme.Volt else Silver.copy(alpha = 0.25f)))
                }
            }
            com.stepup.android.ui.components.PrimaryCta(
                text = stringResource(if (isLast) R.string.guide_start else R.string.guide_next),
                onClick = { if (!GuideTour.advance()) onFinished() }, modifier = Modifier.fillMaxWidth(),
            )
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                GhostButton(stringResource(R.string.guide_prev), onClick = { GuideTour.back() },
                    enabled = GuideTour.stepIndex > 0, modifier = Modifier.weight(1f))
                if (!isLast) {
                    GhostButton(stringResource(R.string.guide_skip), onClick = { finish() },
                        accent = Silver, modifier = Modifier.weight(1f))
                }
            }
        }
    }
}
