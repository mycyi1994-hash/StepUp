package com.stepup.android.ui.screens.gacha

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.relocation.BringIntoViewRequester
import androidx.compose.foundation.relocation.bringIntoViewRequester
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.min
import androidx.compose.ui.unit.sp
import com.stepup.android.R
import com.stepup.android.domain.DrawDistance
import com.stepup.android.domain.DrawKind
import com.stepup.android.domain.DrawStatus
import com.stepup.android.domain.Sneaker
import com.stepup.android.ui.components.SecondaryHeader
import com.stepup.android.ui.components.SettingsToast
import com.stepup.android.ui.experience.FeedbackCue
import com.stepup.android.ui.experience.LocalFeedback
import com.stepup.android.ui.experience.LocalMotion
import com.stepup.android.ui.theme.StepUpDesign
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/** 화면에서 누르는 것 — 앱 셸(StepUpRoot)이 서버 · 이동과 잇는다 */
@Stable
class DrawActions(
    /** 두 칸의 뒤로 — 들어온 곳(보통 신발 탭의 내 신발)으로 */
    val onBack: () -> Unit = {},
    val onDraw: (DrawKind) -> Unit = {},
    /** 기존 웹 지갑 페이지(서명 · 2단계 인증)에서 연결한다. 앱이 아는 것은 돌아와 읽은 서버의 연결 여부뿐이다 */
    val onConnectWallet: () -> Unit = {},
    val onRetry: () -> Unit = {},
    val onCheckPending: () -> Unit = {},
    val onLeaveFlow: () -> Unit = {},
    val onFinishOpening: () -> Unit = {},
    val onCloseResult: () -> Unit = {},
    /** 신발 탭(내 신발)으로 — 받은 신발 번호가 있으면 그 신발을 고른 채로(착용은 바꾸지 않는다) */
    val onOpenShoes: (Long?) -> Unit = {},
    /** 기존 자유 러닝 시작 흐름 */
    val onStartRun: () -> Unit = {},
    val onNoticeDone: () -> Unit = {},
)

/** 두 칸 위에서 여닫는 시트 — 여닫아도 기회를 쓰지 않는다(03 · 04 · 05 · 06 · 16 · 17) */
enum class DrawSheet { FreeChances, PremiumChances, Rules, WalletBenefit, FreeEmpty, RunChances }

/**
 * 신발 뽑기 v2(2026-09-28 전달본, docs/redesign/shoe-draw-v2) — 신발 탭의 "뽑기"에서 들어오는 화면.
 *
 * 위아래 두 칸(무료 · 상급)에 칸마다 제목 · 지급 조건 · 남은 기회 · 내역 링크 · 버튼 하나. 칸 바탕은 누르는 곳이 아니다.
 * 버튼을 누르면 결과 확인 중(10) → 서버가 결과를 확인한 뒤에만 상자 열기(11) → 결과(12 · 13 · 14). 답을 받지 못하면
 * 새로 뽑지 않고 확인한다(20), 뒤로 가면 "결과 확인"이 남는다(26). 수 · 연결 상태 · 신발은 모두 서버 값이다 — 가격 · SUP ·
 * 확률 · 나올 수 있는 신발 목록은 없다.
 *
 * [initialSheet] · [openingAt] 는 기기 검사가 한 장면을 바로 찍을 때만 쓴다.
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun MysteryBoxScreen(
    state: DrawScreenState,
    modifier: Modifier = Modifier,
    flow: DrawFlow = DrawFlow.Home,
    pending: DrawPending? = null,
    notice: DrawNotice? = null,
    actions: DrawActions = DrawActions(),
    initialSheet: DrawSheet? = null,
    openingAt: Float? = null,
) {
    val feedback = LocalFeedback.current
    LaunchedEffect(feedback) { feedback?.play(FeedbackCue.DrawEnter) }
    val motion = LocalMotion.current
    val status = (state as? DrawScreenState.Ready)?.status
    var sheet by rememberSaveable { mutableStateOf(initialSheet) }
    val premiumFocus = remember { FocusRequester() }
    val premiumInView = remember { BringIntoViewRequester() }
    val scope = rememberCoroutineScope()

    BackHandler(enabled = flow !is DrawFlow.Home) {
        when (flow) {
            is DrawFlow.Requesting, is DrawFlow.Checking -> actions.onLeaveFlow()
            is DrawFlow.Opening -> actions.onFinishOpening()
            is DrawFlow.Result -> actions.onCloseResult()
            DrawFlow.Home -> Unit
        }
    }

    // 바탕은 앱 셸의 공통 바탕(CommerceBackdrop — 짙은 남색, 오른쪽 위가 밝다)이 상태 막대 밑까지 깐다
    Box(modifier.fillMaxSize()) {
        AnimatedContent(
            targetState = flow,
            contentKey = { it::class },
            transitionSpec = { fadeIn(tween(motion.duration(200))) togetherWith fadeOut(tween(motion.duration(200))) },
            label = "drawFlow",
        ) { current ->
            when (current) {
                DrawFlow.Home -> DrawHome(
                    state = state, pending = pending, actions = actions, onSheet = { sheet = it },
                    premiumFocus = premiumFocus, premiumInView = premiumInView,
                )
                is DrawFlow.Requesting -> DrawRequestScreen(current.kind, onBack = actions.onLeaveFlow)
                is DrawFlow.Opening -> DrawOpeningScreen(current.result, onFinish = actions.onFinishOpening, frozenAt = openingAt)
                is DrawFlow.Result -> DrawResultScreen(
                    result = current.result,
                    canDrawAgain = pending == null && current.result.left.let { it != null && it > 0 } &&
                        status?.canDraw(current.result.kind) == true,
                    onOpenShoes = { actions.onOpenShoes(current.result.shoe.id) },
                    onDrawAgain = { actions.onDraw(current.result.kind) },
                    onClose = actions.onCloseResult,
                )
                is DrawFlow.Checking -> DrawCheckScreen(current, onCheck = actions.onCheckPending, onBack = actions.onLeaveFlow)
            }
        }
        // 18 러닝 반영 · 다시 연결 — 서버 값이 바뀐 것을 확인했을 때만 잠깐
        val toast = when (notice) {
            is DrawNotice.RunReward -> stringResource(R.string.dv2_run_reward, notice.added)
            DrawNotice.Relinked -> stringResource(R.string.dv2_relinked)
            else -> null
        }
        LaunchedEffect(notice) {
            if (notice is DrawNotice.RunReward || notice == DrawNotice.Relinked) {
                delay(3_200)
                actions.onNoticeDone()
            }
        }
        SettingsToast(
            toast,
            Modifier.align(Alignment.BottomCenter).navigationBarsPadding()
                .padding(horizontal = StepUpDesign.Gutter, vertical = 16.dp).testTag("draw-toast"),
        )
    }

    // ── 시트 — 두 칸 위에서만 ─────────────────────────────────────────
    if (flow != DrawFlow.Home) return
    val close = { sheet = null }
    when (notice) {
        is DrawNotice.NotStarted -> {
            LaunchedEffect(notice) { feedback?.play(FeedbackCue.DrawFail) }
            NotStartedSheet(
                stop = notice.stop,
                canRetry = notice.stop.retryable && pending == null && status?.canDraw(notice.kind) == true,
                onRetry = {
                    actions.onNoticeDone()
                    actions.onDraw(notice.kind)
                },
                onClose = actions.onNoticeDone,
            )
            return
        }
        is DrawNotice.Linked -> {
            WalletLinkedSheet(
                gift = notice.gift,
                canDraw = pending == null && status?.canDraw(DrawKind.PREMIUM) == true,
                onDraw = {
                    actions.onNoticeDone()
                    actions.onDraw(DrawKind.PREMIUM)
                },
                onClose = actions.onNoticeDone,
            )
            return
        }
        DrawNotice.LoadFailed -> {
            LoadFailedSheet(
                onReload = {
                    actions.onNoticeDone()
                    actions.onRetry()
                },
                onBack = {
                    actions.onNoticeDone()
                    actions.onBack()
                },
                onClose = actions.onNoticeDone,
            )
            return
        }
        else -> Unit
    }
    if (status == null) return
    val canFree = pending == null && status.canDraw(DrawKind.FREE)
    val canPremium = pending == null && status.canDraw(DrawKind.PREMIUM)
    when (sheet) {
        DrawSheet.FreeChances -> FreeChancesSheet(status, canFree, onDraw = { close(); actions.onDraw(DrawKind.FREE) }, onClose = close)
        DrawSheet.PremiumChances -> PremiumChancesSheet(
            status, canPremium, onDraw = { close(); actions.onDraw(DrawKind.PREMIUM) }, onClose = close,
        )
        DrawSheet.Rules -> RulesSheet(status, onClose = close)
        DrawSheet.WalletBenefit -> WalletBenefitSheet(status, onConnect = { close(); actions.onConnectWallet() }, onClose = close)
        DrawSheet.FreeEmpty -> FreeEmptySheet(
            status,
            onSeePremium = {
                close()
                // 16 → 상급 칸으로 초점을 옮긴다
                scope.launch {
                    delay(250)
                    premiumInView.bringIntoView()
                    runCatching { premiumFocus.requestFocus() }
                }
            },
            onClose = close,
        )
        DrawSheet.RunChances -> RunChancesSheet(status, onStartRun = { close(); actions.onStartRun() }, onClose = close)
        null -> Unit
    }
}

// ── 두 칸(01 · 02 · 15 · 22 · 23 · 24 · 26) ───────────────────────────

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun DrawHome(
    state: DrawScreenState,
    pending: DrawPending?,
    actions: DrawActions,
    onSheet: (DrawSheet) -> Unit,
    premiumFocus: FocusRequester,
    premiumInView: BringIntoViewRequester,
) {
    val p = drawPalette()
    val status = (state as? DrawScreenState.Ready)?.status
    Column(Modifier.fillMaxSize()) {
        SecondaryHeader(
            onBack = actions.onBack, balance = null, onOpenWallet = null, title = stringResource(R.string.dv2_title),
            modifier = Modifier.padding(horizontal = StepUpDesign.Gutter),
        )
        Column(
            Modifier.weight(1f).fillMaxWidth().verticalScroll(rememberScrollState())
                .padding(horizontal = StepUpDesign.Gutter).testTag("draw-home"),
        ) {
            Text(
                stringResource(
                    when (state) {
                        DrawScreenState.Loading -> R.string.dv2_subtitle_loading
                        DrawScreenState.SignedOut -> R.string.dv2_subtitle_signed_out
                        else -> R.string.dv2_subtitle
                    },
                ),
                color = p.secondary, fontSize = 14.sp, textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth().padding(top = 2.dp, bottom = 16.dp).testTag("draw-subtitle"),
            )
            FreeCompartment(state, status, pending, actions, onSheet)
            Spacer(Modifier.height(18.dp))
            PremiumCompartment(
                state, status, pending, actions, onSheet,
                Modifier.bringIntoViewRequester(premiumInView), Modifier.focusRequester(premiumFocus),
            )
            if (status != null) {
                DrawLink(
                    stringResource(R.string.dv2_rules_link), onClick = { onSheet(DrawSheet.Rules) },
                    modifier = Modifier.align(Alignment.CenterHorizontally).padding(top = 14.dp).testTag("draw-rules"),
                    fontSize = 15f,
                )
            }
            Spacer(Modifier.height(24.dp))
        }
    }
}

/** 무료 칸 — 첫 가입 · 매일 무료. 지갑이 필요 없다 */
@Composable
private fun FreeCompartment(
    state: DrawScreenState,
    status: DrawStatus?,
    pending: DrawPending?,
    actions: DrawActions,
    onSheet: (DrawSheet) -> Unit,
) {
    val p = drawPalette()
    val rule = status?.let {
        when {
            it.signupGranted > 0 -> stringResource(R.string.dv2_free_rule, it.signupGranted, it.dailyTotal)
            it.dailyTotal > 0 -> stringResource(R.string.dv2_free_rule_daily, it.dailyTotal)
            else -> null
        }
    }
    Compartment(
        title = stringResource(R.string.dv2_free), rule = rule, surface = p.freeSurface, tag = "draw-free",
        body = {
            when {
                state == DrawScreenState.Loading -> CountSkeleton()
                state == DrawScreenState.SignedOut -> Note(stringResource(R.string.dv2_signed_out_note))
                state == DrawScreenState.Failed || status == null -> Note(stringResource(R.string.dv2_failed_inline), tag = "draw-free-failed")
                pending?.kind == DrawKind.FREE -> PendingCount()
                else -> {
                    Count(stringResource(R.string.dv2_left), stringResource(R.string.dv2_count, status.freeLeft), "draw-free-left")
                    DrawLink(
                        stringResource(R.string.dv2_history), chevron = true,
                        onClick = { onSheet(if (status.freeLeft > 0) DrawSheet.FreeChances else DrawSheet.FreeEmpty) },
                        modifier = Modifier.testTag("draw-free-history"),
                    )
                }
            }
        },
        action = {
            val tag = Modifier.testTag("draw-free-action")
            when {
                state == DrawScreenState.Loading -> DrawButton(stringResource(R.string.dv2_action_loading), {}, tag, DrawButtonStyle.Idle)
                state == DrawScreenState.SignedOut -> DrawButton(stringResource(R.string.dv2_action_sign_in), {}, tag, DrawButtonStyle.Idle)
                state == DrawScreenState.Failed || status == null ->
                    DrawButton(stringResource(R.string.dv2_action_reload), actions.onRetry, tag, DrawButtonStyle.Quiet)
                pending != null -> DrawButton(stringResource(R.string.dv2_action_check), actions.onCheckPending, tag, DrawButtonStyle.Quiet)
                status.freeLeft > 0 -> DrawButton(stringResource(R.string.dv2_action_free), { actions.onDraw(DrawKind.FREE) }, tag)
                else -> DrawButton(stringResource(R.string.dv2_action_free_info), { onSheet(DrawSheet.FreeEmpty) }, tag, DrawButtonStyle.Quiet)
            }
        },
    )
}

/** 상급 칸 — 지갑 연결 상태(서버 값)에 맞는 행동 하나 */
@Composable
private fun PremiumCompartment(
    state: DrawScreenState,
    status: DrawStatus?,
    pending: DrawPending?,
    actions: DrawActions,
    onSheet: (DrawSheet) -> Unit,
    modifier: Modifier,
    actionModifier: Modifier,
) {
    val p = drawPalette()
    val step = status?.let { DrawDistance.stepKm(it.runStepMeters) }
    val rule = status?.let {
        if (it.giftOnLink > 0) stringResource(R.string.dv2_premium_rule, it.giftOnLink, step.orEmpty())
        else stringResource(R.string.dv2_premium_rule_run, step.orEmpty())
    }
    val mode = status?.premiumMode()
    Compartment(
        title = stringResource(R.string.dv2_premium), rule = rule, surface = p.premiumSurface, tag = "draw-premium",
        modifier = modifier,
        body = {
            when {
                state == DrawScreenState.Loading -> CountSkeleton()
                state == DrawScreenState.SignedOut -> Note(stringResource(R.string.dv2_signed_out_note))
                state == DrawScreenState.Failed || status == null ->
                    Note(stringResource(R.string.dv2_failed_inline), tag = "draw-premium-failed")
                pending?.kind == DrawKind.PREMIUM -> PendingCount()
                mode == PremiumMode.Connect -> {
                    Count(stringResource(R.string.dv2_first_link), stringResource(R.string.dv2_count_plus, status.giftOnLink), "draw-premium-gift")
                    Note(stringResource(R.string.dv2_first_link_note))
                }
                mode == PremiumMode.Reconnect -> {
                    Count(stringResource(R.string.dv2_kept_premium), stringResource(R.string.dv2_count, status.premiumLeft), "draw-premium-left")
                    Note(stringResource(R.string.dv2_reconnect_note))
                }
                mode == PremiumMode.Paused -> {
                    Count(stringResource(R.string.dv2_left), stringResource(R.string.dv2_count, status.premiumLeft), "draw-premium-left")
                    Note(stringResource(R.string.dv2_paused_note))
                }
                else -> {
                    Count(stringResource(R.string.dv2_left), stringResource(R.string.dv2_count, status.premiumLeft), "draw-premium-left")
                    DrawLink(
                        if (status.runCapReached) stringResource(R.string.dv2_run_capped)
                        else stringResource(R.string.dv2_next_premium, DrawDistance.remainingKm(status.metersToNextPremium)),
                        chevron = true,
                        onClick = { onSheet(if (mode == PremiumMode.Ready) DrawSheet.PremiumChances else DrawSheet.RunChances) },
                        modifier = Modifier.testTag("draw-premium-next"),
                    )
                }
            }
        },
        action = {
            val tag = actionModifier.testTag("draw-premium-action")
            when {
                state == DrawScreenState.Loading -> DrawButton(stringResource(R.string.dv2_action_loading), {}, tag, DrawButtonStyle.Idle)
                state == DrawScreenState.SignedOut -> DrawButton(stringResource(R.string.dv2_action_sign_in), {}, tag, DrawButtonStyle.Idle)
                state == DrawScreenState.Failed || status == null ->
                    DrawButton(stringResource(R.string.dv2_action_reload), actions.onRetry, tag, DrawButtonStyle.Quiet)
                pending != null -> DrawButton(stringResource(R.string.dv2_action_check), actions.onCheckPending, tag, DrawButtonStyle.Quiet)
                mode == PremiumMode.Connect ->
                    DrawButton(stringResource(R.string.dv2_action_connect, status.giftOnLink), { onSheet(DrawSheet.WalletBenefit) }, tag)
                // 24 — 선물은 다시 주지 않는다. 연결은 기존 웹 지갑 페이지에서
                mode == PremiumMode.Reconnect -> DrawButton(stringResource(R.string.dv2_action_reconnect), actions.onConnectWallet, tag)
                mode == PremiumMode.Paused -> DrawButton(stringResource(R.string.dv2_action_paused), {}, tag, DrawButtonStyle.Idle)
                mode == PremiumMode.Ready -> DrawButton(stringResource(R.string.dv2_action_premium), { actions.onDraw(DrawKind.PREMIUM) }, tag)
                else -> DrawButton(stringResource(R.string.dv2_action_run), { onSheet(DrawSheet.RunChances) }, tag, DrawButtonStyle.Quiet)
            }
        },
    )
}

/** 칸 한 개 — 바탕은 누르는 곳이 아니다. 제목 → 조건 → 남은 기회 → 버튼 순서로 읽힌다 */
@Composable
private fun Compartment(
    title: String,
    rule: String?,
    surface: Color,
    tag: String,
    modifier: Modifier = Modifier,
    body: @Composable ColumnScope.() -> Unit,
    action: @Composable () -> Unit,
) {
    val p = drawPalette()
    val largeText = LocalDensity.current.fontScale > 1.3f
    Column(modifier.fillMaxWidth().clip(RoundedCornerShape(24.dp)).background(surface).padding(20.dp).testTag(tag)) {
        Text(
            title, color = p.text, fontSize = 22.sp, fontWeight = FontWeight.Bold,
            modifier = Modifier.semantics { heading() },
        )
        if (rule != null) {
            Text(rule, color = p.secondary, fontSize = 13.sp, modifier = Modifier.padding(top = 6.dp))
        }
        BoxWithConstraints(Modifier.fillMaxWidth().padding(top = 14.dp)) {
            // 좁은 폭 · 큰 글씨에서는 상자 그림을 빼고 수에 폭을 다 준다
            val showArt = !largeText && maxWidth >= 250.dp
            val artWidth = min(maxWidth * 0.42f, 138.dp)
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f), content = body)
                if (showArt) {
                    Image(
                        painterResource(R.drawable.draw_shoebox_stage), contentDescription = null,
                        contentScale = ContentScale.Fit,
                        modifier = Modifier.width(artWidth).aspectRatio(1170f / 1028f),
                    )
                }
            }
        }
        Spacer(Modifier.height(16.dp))
        action()
    }
}

/** 남은 기회 — 작은 이름과 큰 수를 한 번에 읽는다 */
@Composable
private fun Count(label: String, value: String, tag: String) {
    val p = drawPalette()
    Column(Modifier.semantics(mergeDescendants = true) {}) {
        Text(label, color = p.secondary, fontSize = 13.sp)
        Text(
            value, color = p.text, fontSize = 36.sp, fontWeight = FontWeight.Bold, letterSpacing = (-0.8).sp,
            maxLines = 1, modifier = Modifier.padding(top = 2.dp).testTag(tag),
        )
    }
}

/** 26 — 뽑은 신발 확인 중(결과가 준비되면 "결과 확인"으로 본다) */
@Composable
private fun PendingCount() {
    Count(stringResource(R.string.dv2_pending_label), stringResource(R.string.dv2_pending_value), "draw-pending")
    Note(stringResource(R.string.dv2_pending_note))
}

@Composable
private fun Note(text: String, tag: String? = null) {
    val p = drawPalette()
    Text(
        text, color = p.secondary, fontSize = 13.sp,
        modifier = Modifier.padding(top = 6.dp).then(if (tag != null) Modifier.testTag(tag) else Modifier),
    )
}

/** 22 — 불러오는 동안은 자리만(0 을 임시로 보이지 않는다) */
@Composable
private fun CountSkeleton() {
    val p = drawPalette()
    Column(Modifier.testTag("draw-skeleton")) {
        Box(Modifier.size(width = 80.dp, height = 16.dp).clip(RoundedCornerShape(8.dp)).background(p.skeleton))
        Spacer(Modifier.height(18.dp))
        Box(Modifier.size(width = 92.dp, height = 38.dp).clip(RoundedCornerShape(12.dp)).background(p.skeleton))
    }
}

// ── 예전 이름 ────────────────────────────────────────────────────

/**
 * 뽑기 결과(12) — 예전 이름 그대로 둔다(등급 프레임 · 도감 기기 검사가 부른다). 이제 창이 아니라 결과 화면을 그린다.
 * 신발은 서버가 준 그 신발이고, 착용은 바꾸지 않는다.
 */
@Composable
fun DrawResultDialog(sneaker: Sneaker, onOpenShoes: () -> Unit, onClose: () -> Unit) {
    Box(Modifier.fillMaxSize()) {
        DrawBackdrop(Modifier.fillMaxSize())
        Box(Modifier.fillMaxSize().systemBarsPadding()) {
            DrawResultScreen(
                result = DrawnShoe(DrawKind.FREE, sneaker), canDrawAgain = false,
                onOpenShoes = onOpenShoes, onDrawAgain = {}, onClose = onClose,
            )
        }
    }
}
