package com.stepup.android.ui.screens.gacha

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.stepup.android.R
import com.stepup.android.domain.DrawKind
import com.stepup.android.domain.Sneaker
import com.stepup.android.ui.experience.FeedbackCue
import com.stepup.android.ui.experience.LocalFeedback
import com.stepup.android.ui.experience.LocalMotion
import com.stepup.android.ui.theme.StepUpDesign
import kotlinx.coroutines.delay

/** 화면에서 누르는 것 — 앱 셸(StepUpRoot)이 서버 · 이동과 잇는다 */
@Stable
class DrawActions(
    /** 불러오기 실패 시트의 "내 신발로 돌아가기" — 신발 탭(내 신발)으로 */
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
    /** 22 로그인 전 — 로그인 화면으로. 로그인할 수 없는 빌드(서버 설정 없음)면 null(버튼을 누를 수 없다) */
    val onSignIn: (() -> Unit)? = null,
)

/** 짧은 알림(DRAW18)이 보이는 동안 메인 아래에 비우는 자리 — 알림 48dp + 하단 탭과의 틈 12dp */
private val ToastRoom = 60.dp

/** 메인 위에서 여닫는 안내창 — 여닫아도 기회를 쓰지 않는다(10 · 13 · 14 · 15 · 16 · 17) */
enum class DrawSheet { FreeChances, PremiumChances, Rules, WalletBenefit, FreeEmpty, RunChances }

/**
 * 신발 뽑기 — 하단 가운데 "뽑기" 탭(2026-10-03 파란 톤 통합 전달본 v4, stepup-draw-blue-claude-v19 · DRAW00~33).
 * 무료 패널과 상급 패널을 한 화면에 함께 두고(예전 무료 / 상급 글자 탭 대신), 각 패널에 상자 · 남은 수 · 주 버튼 하나. 로고 · 잔액 머리와
 * 하단 탭은 앱 셸이 그린다. 뽑기를 누르면 결과 확인 중(DRAW10) → 서버가 결과를 확인한 뒤에만 상자 열기(DRAW11) → 결과(DRAW12 · 13 · 14 · 33)
 * — 이 동안은 하단 탭만 걷는다. 답을 받지 못하면 새로 뽑지 않고 확인한다(DRAW20), 뒤로 가면 두 패널 모두 "결과 확인"이 남는다(DRAW26).
 * 수 · 연결 상태 · 신발은 모두 서버 값이다 — 가격 · SUP 결제 · 확률 · 나올 수 있는 신발 목록은 없다.
 *
 * [initialSheet] · [initialTab] · [openingAt] 는 기기 검사가 한 장면을 바로 찍을 때만 쓴다([initialTab] 이 상급이면 상급 패널을 화면 안으로).
 */
@Composable
fun MysteryBoxScreen(
    state: DrawScreenState,
    modifier: Modifier = Modifier,
    flow: DrawFlow = DrawFlow.Home,
    pending: DrawPending? = null,
    notice: DrawNotice? = null,
    actions: DrawActions = DrawActions(),
    initialSheet: DrawSheet? = null,
    initialTab: DrawKind? = null,
    openingAt: Float? = null,
) {
    val feedback = LocalFeedback.current
    LaunchedEffect(feedback) { feedback?.play(FeedbackCue.DrawEnter) }
    val motion = LocalMotion.current
    val status = (state as? DrawScreenState.Ready)?.status
    var sheet by rememberSaveable { mutableStateOf(initialSheet) }
    // 상급 패널을 화면 안으로 옮길 때마다 하나씩 는다(예전에는 상급 탭을 골랐다) — 상급 요청의 결과 확인 · 시작 실패 · 지갑 연결 확인
    var premiumFocus by remember { mutableIntStateOf(if (initialTab == DrawKind.PREMIUM) 1 else 0) }
    LaunchedEffect(pending?.kind) { if (pending?.kind == DrawKind.PREMIUM) premiumFocus++ }
    LaunchedEffect(notice) {
        val premium = notice is DrawNotice.Linked || (notice is DrawNotice.NotStarted && notice.kind == DrawKind.PREMIUM)
        if (premium) premiumFocus++
    }

    BackHandler(enabled = flow !is DrawFlow.Home) {
        when (flow) {
            is DrawFlow.Requesting, is DrawFlow.Checking -> actions.onLeaveFlow()
            is DrawFlow.Opening -> actions.onFinishOpening()
            is DrawFlow.Result -> actions.onCloseResult()
            DrawFlow.Home -> Unit
        }
    }

    // DRAW18 러닝 반영 · 다시 연결 — 서버 값이 바뀐 것을 확인했을 때만 잠깐(하단 탭 위). 보이는 동안은 메인 아래에 자리를 비워
    // 알림이 버튼을 가리지 않는다
    val toast = when (notice) {
        is DrawNotice.RunReward -> stringResource(R.string.dv2_run_reward, notice.added)
        DrawNotice.Relinked -> stringResource(R.string.dv2_relinked)
        else -> null
    }
    val toastRoom by animateDpAsState(
        if (toast != null) ToastRoom else 0.dp, tween(motion.duration(200)), label = "drawToastRoom",
    )

    // 바탕은 앱 셸의 공통 바탕이 상태 막대 밑까지 깐다
    Box(modifier.fillMaxSize()) {
        AnimatedContent(
            targetState = flow,
            contentKey = { it::class },
            transitionSpec = { fadeIn(tween(motion.duration(200))) togetherWith fadeOut(tween(motion.duration(200))) },
            label = "drawFlow",
        ) { current ->
            when (current) {
                DrawFlow.Home -> DrawHome(
                    bottomInset = toastRoom,
                    status = status,
                    loading = state == DrawScreenState.Loading,
                    signedOut = state == DrawScreenState.SignedOut,
                    pending = pending,
                    actions = actions,
                    onSheet = { sheet = it },
                    premiumFocus = premiumFocus,
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
        LaunchedEffect(notice) {
            if (notice is DrawNotice.RunReward || notice == DrawNotice.Relinked) {
                delay(3_200)
                actions.onNoticeDone()
            }
        }
        DrawToast(
            toast,
            Modifier.align(Alignment.BottomCenter).padding(horizontal = StepUpDesign.Gutter).padding(bottom = 12.dp).testTag("draw-toast"),
        )
    }

    // ── 안내창 — 메인 위에서만 ─────────────────────────────────────────
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
    val toRules = { sheet = DrawSheet.Rules }
    when (sheet) {
        DrawSheet.FreeChances -> FreeChancesSheet(status, canFree, onDraw = { close(); actions.onDraw(DrawKind.FREE) }, onRules = toRules, onClose = close)
        DrawSheet.PremiumChances -> PremiumChancesSheet(
            status, canPremium, onDraw = { close(); actions.onDraw(DrawKind.PREMIUM) }, onRules = toRules, onClose = close,
        )
        DrawSheet.Rules -> RulesSheet(status, onClose = close)
        DrawSheet.WalletBenefit -> WalletBenefitSheet(status, onConnect = { close(); actions.onConnectWallet() }, onClose = close)
        // DRAW16 → "상급 뽑기 보기"는 같은 화면의 상급 패널로(글자 탭을 새로 만들지 않는다)
        DrawSheet.FreeEmpty -> FreeEmptySheet(status, onSeePremium = { close(); premiumFocus++ }, onClose = close)
        DrawSheet.RunChances -> RunChancesSheet(status, onStartRun = { close(); actions.onStartRun() }, onClose = close)
        null -> Unit
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
