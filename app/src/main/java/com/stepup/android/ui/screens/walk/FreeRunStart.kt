package com.stepup.android.ui.screens.walk

import android.content.Context
import androidx.activity.compose.BackHandler
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.Warning
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.layout
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.platform.LocalWindowInfo
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.onClick
import androidx.compose.ui.semantics.onLongClick
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.progressBarRangeInfo
import androidx.compose.ui.semantics.ProgressBarRangeInfo
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.PlatformTextStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.LineHeightStyle
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.compose.ui.window.DialogWindowProvider
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.stepup.android.R
import com.stepup.android.domain.GeoPoint
import com.stepup.android.domain.Sneaker
import com.stepup.android.domain.tier
import com.stepup.android.ui.components.MapTiles
import com.stepup.android.ui.components.RunStartWordmark
import com.stepup.android.ui.components.prewarmRunStartWordmark
import com.stepup.android.ui.components.ShoeArtFull
import com.stepup.android.ui.components.ShoeArtThumbnail
import com.stepup.android.ui.components.TilePlan
import com.stepup.android.ui.components.label
import com.stepup.android.ui.components.rememberCurrentLocation
import com.stepup.android.ui.experience.LocalMotion
import com.stepup.android.ui.experience.feedbackClickable
import com.stepup.android.ui.screens.items.LOW_DURABILITY_WARN_MIN
import com.stepup.android.ui.screens.items.durabilityEarnMinutes
import com.stepup.android.ui.screens.items.durabilityPercent
import com.stepup.android.ui.screens.items.shoeName
import com.stepup.android.ui.theme.StepUpNumbers
import com.stepup.android.ui.theme.StepUpSans
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

/*
 * 자유러닝 시작 플로(2026-10-17 전달본 — docs/redesign/free-run-start-2026-10-17).
 *
 * 자유러닝 시작 → 신발 점검(SHOE_CHECK) → 출발 확인 → 권한 · 위치 기능(PREPARING, 기존 안내) → 3 · 2 · 1 · 0(COUNTDOWN)
 * → StepUP!(LAUNCH) → 가운데에서 지도가 펼쳐짐(REVEAL — 여기서 러닝 기록을 시작한다) → 기존 자유 러닝 화면(RUNNING).
 * 카운트다운에는 숫자와 원호만 둔다 — 제목 · 안내 · 취소 버튼은 최종안에서 뺐다. 기록 전 그만두기는 시스템 뒤로 가기.
 * 시간 · 거리 · 위치 · 일시정지 · 종료 · 신발 · 보상은 기존 러닝 서비스 그대로다 — 여기서는 시작 순간만 정한다.
 */

internal val StartNavy = Color(0xFF061B30)
private val StartAccent = Color(0xFF1450FF)
private val StartArcTip = Color(0xFF72A6FF)
private val StartRevealLead = Color(0xFF5287FF)
private val StartTrack = Color(0x21A9C2E4)
internal val StartGlow = Color(0x1F1450FF)

private val CheckBackdrop = Color(0xBA020B16)
private val CheckFace = Color(0xFF102940)
private val CheckEdge = Color(0xFF32516D)
private val CheckClose = Color(0xFFAAC0D8)
private val CheckSubtle = Color(0xFFA9C5E7)
private val CheckLabel = Color(0xFFAFC2DA)
private val CheckValue = Color(0xFF60D5EE)
private val CheckMeterTrack = Color(0xFF294663)
private val CheckMeterFill = Color(0xFF4ED2E8)
private val CheckReadyIcon = Color(0xFF61D6B6)
private val CheckReadyText = Color(0xFFADC4DA)
private val CheckWarnIcon = Color(0xFFFFC65C)
private val CheckWarnText = Color(0xFFFFE2A8)
private val CheckBack = Color(0xFFB5C6DA)
private val CheckFloor = Color(0x55436B8B)

private val CheckShape = RoundedCornerShape(25.dp)
private val CheckButtonShape = RoundedCornerShape(15.dp)

// ── 신발 점검(SHOE_CHECK) ───────────────────────────────────────────

/**
 * 출발 전 신발 점검 — 신고 있는 신발의 그림 · 등급 · 레벨 · 남은 내구도(실제 값). "이 신발로 출발"만 카운트다운으로 간다.
 * 돌아가기 · 닫기 · 뒤로는 아무것도 시작하지 않는다. 신발을 신지 않았으면 그 사실을 알리고(지금처럼 신발 없이도 달릴 수 있다)
 * "러닝 시작"으로 같은 흐름을 잇는다.
 * 버튼은 셋 — 이 신발로 출발 · 돌아가기 · 신발 교체(2026-10-26). 신발 교체나 신발 그림을 누르면 내 신발 목록이 열리고,
 * 신발은 1초 꾹 눌러야 골라진다(누르는 동안 줄이 차오르고 살짝 작아진다 — 스치듯 눌러 바뀌지 않게).
 */
@Composable
internal fun FreeRunShoeCheck(
    shoe: Sneaker?,
    shoes: List<Sneaker>,
    wearing: Boolean,
    onWear: (Long, (Boolean) -> Unit) -> Unit,
    onStart: () -> Unit,
    onCancel: () -> Unit,
) {
    Dialog(
        onDismissRequest = onCancel,
        properties = DialogProperties(usePlatformDefaultWidth = false, decorFitsSystemWindows = false, dismissOnClickOutside = false),
    ) {
        // 창이 깔아 주는 회색 대신 시안의 뒷면(#020B16 · 73%)을 직접 그린다
        val window = (LocalView.current.parent as? DialogWindowProvider)?.window
        SideEffect { window?.setDimAmount(0f) }
        FreeRunShoeCheckContent(shoe, onStart, onCancel, shoes = shoes, wearing = wearing, onWear = onWear)
    }
}

/** [FreeRunShoeCheck] 의 화면(뒷면 + 가운데 카드) — 시안 검사가 창 없이 그린다 */
@Composable
internal fun FreeRunShoeCheckContent(
    shoe: Sneaker?,
    onStart: () -> Unit,
    onCancel: () -> Unit,
    modifier: Modifier = Modifier,
    shoes: List<Sneaker> = emptyList(),
    wearing: Boolean = false,
    onWear: (Long, (Boolean) -> Unit) -> Unit = { _, _ -> },
) {
    val reduced = LocalMotion.current.reduced
    // 신발 고르기를 열었나 — 같은 카드 안에서 목록으로 바뀐다. 뒤로 가기는 점검으로 돌아온다
    var picking by rememberSaveable { mutableStateOf(false) }
    var pickFailed by remember { mutableStateOf(false) }
    val canChange = shoes.isNotEmpty()
    BackHandler(enabled = picking) { picking = false }
    val shown = remember { Animatable(if (reduced) 1f else 0f) }
    val context = LocalContext.current
    LaunchedEffect(Unit) {
        // 카운트다운 전에 StepUP! 그림을 뒤에서 풀어 둔다(전환 중에 풀지 않게)
        prewarmRunStartWordmark(context)
        shown.animateTo(1f, tween(250, easing = FreeRunStartMotion.ExitEnter))
    }
    // 출발은 한 번만 — 연타 · 겹친 누름에도 시작 요청은 하나다
    var fired by remember { mutableStateOf(false) }
    val start = {
        if (!fired) {
            fired = true
            onStart()
        }
    }
    BoxWithConstraints(modifier.fillMaxSize().testTag("run-shoe-check"), contentAlignment = Alignment.Center) {
        val compact = maxWidth < 380.dp
        // 뒷면 — 누르면 아무 일도 없다(점검은 버튼으로만 닫는다). 아래 러닝 화면을 누르지 않게 막는다
        Box(
            Modifier.matchParentSize().graphicsLayer { alpha = shown.value }.background(CheckBackdrop)
                .pointerInput(Unit) { awaitPointerEventScope { while (true) awaitPointerEvent() } },
        )
        Box(
            Modifier.safeDrawingPadding().padding(if (compact) 16.dp else 22.dp)
                .widthIn(max = 316.dp).fillMaxWidth().heightIn(max = maxHeight)
                .graphicsLayer {
                    alpha = shown.value
                    val s = if (reduced) 1f else 0.96f + 0.04f * shown.value
                    scaleX = s
                    scaleY = s
                }
                .clip(CheckShape).background(CheckFace, CheckShape).border(1.dp, CheckEdge, CheckShape),
        ) {
            val side = if (compact) 19.dp else 22.dp
            Column(
                Modifier.verticalScroll(rememberScrollState())
                    .padding(start = side, end = side, top = 27.dp, bottom = 21.dp),
            ) {
                if (picking) {
                    ShoePickList(
                        shoes = shoes, current = shoe?.id, wearing = wearing, failed = pickFailed,
                        onPick = { id ->
                            if (id == shoe?.id) {
                                picking = false
                            } else {
                                pickFailed = false
                                onWear(id) { worn -> if (worn) picking = false else pickFailed = true }
                            }
                        },
                        onBack = { picking = false },
                    )
                } else {
                    Text(
                        stringResource(R.string.run_free_check_title),
                        style = checkText(if (compact) 24.sp else 25.sp, Color.White, FontWeight.Bold, 1.33f, (-0.036).em),
                        modifier = Modifier.padding(end = 36.dp).semantics { heading() }.testTag("run-shoe-check-title"),
                    )
                    if (shoe != null) {
                        CheckShoe(shoe, onClick = if (canChange) ({ picking = true }) else null)
                        CheckShoeFacts(shoe)
                    } else {
                        CheckNoShoe()
                    }
                    // 이 신발로 출발(주 버튼) — 아래 돌아가기 · 신발 교체
                    Box(
                        Modifier.padding(top = 29.dp).fillMaxWidth().heightIn(min = 54.dp).clip(CheckButtonShape)
                            .background(if (wearing) CheckMeterTrack else StartAccent, CheckButtonShape)
                            .feedbackClickable(enabled = !wearing, onClick = start).padding(horizontal = 12.dp, vertical = 8.dp)
                            .testTag("run-shoe-check-go"),
                        contentAlignment = Alignment.Center,
                    ) {
                        Text(
                            stringResource(if (shoe != null) R.string.run_free_check_go else R.string.home_start_run),
                            style = checkText(16.sp, Color.White, FontWeight.SemiBold), maxLines = 2, overflow = TextOverflow.Ellipsis,
                        )
                    }
                    Row(Modifier.fillMaxWidth().padding(top = 10.dp), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        CheckSecondaryButton(stringResource(R.string.run_free_check_back), onCancel, "run-shoe-check-back", Modifier.weight(1f))
                        if (canChange) {
                            CheckSecondaryButton(
                                stringResource(R.string.run_free_check_change), { picking = true }, "run-shoe-check-change", Modifier.weight(1f),
                                outlined = true,
                            )
                        }
                    }
                }
            }
            val closeLabel = stringResource(R.string.run_free_check_close)
            Box(
                Modifier.align(Alignment.TopEnd).padding(top = 12.dp, end = 12.dp).size(44.dp)
                    .clip(RoundedCornerShape(22.dp)).feedbackClickable(onClick = onCancel)
                    .semantics { contentDescription = closeLabel }.testTag("run-shoe-check-close"),
                contentAlignment = Alignment.Center,
            ) {
                Icon(Icons.Filled.Close, contentDescription = null, tint = CheckClose, modifier = Modifier.size(20.dp))
            }
        }
    }
}

/** 신발 그림 — 카드 안쪽 여백까지 조금 넓혀(좌우 12dp) 그림자 위에 놓는다. 그림의 보이는 부분만 맞춘다 */
@Composable
private fun CheckShoe(shoe: Sneaker, onClick: (() -> Unit)?) {
    Box(
        Modifier.padding(top = 10.dp, bottom = 3.dp).bleed(12.dp).height(174.dp)
            .then(if (onClick != null) Modifier.clip(RoundedCornerShape(18.dp)).feedbackClickable(onClick = onClick) else Modifier)
            .drawBehind {
                val rx = size.width * 0.45f
                val ry = size.height * 0.10f
                val c = Offset(size.width / 2f, size.height * 0.91f)
                scale(rx / ry, 1f, pivot = c) {
                    drawCircle(Brush.radialGradient(listOf(CheckFloor, Color.Transparent), center = c, radius = ry), radius = ry, center = c)
                }
            }
            .testTag("run-shoe-check-art"),
    ) {
        ShoeArtThumbnail(
            shoe, Modifier.align(Alignment.TopCenter).padding(top = 13.dp).fillMaxWidth(0.83f).height(139.dp),
            failedTint = CheckSubtle, sample = ShoeArtFull,
        )
    }
}

/** 이름 줄(등급 · 레벨) · 남은 내구도 막대 · 준비 줄 */
@Composable
private fun CheckShoeFacts(shoe: Sneaker) {
    val points = durabilityPercent(shoe)
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Text(stringResource(R.string.run_free_check_equipped), style = checkText(17.sp, Color.White, FontWeight.SemiBold),
            modifier = Modifier.weight(1f), maxLines = 2, overflow = TextOverflow.Ellipsis)
        Spacer(Modifier.width(10.dp))
        Text(
            stringResource(R.string.run_free_check_grade_level, shoe.tier.label(), shoe.level),
            style = checkText(12.sp, CheckSubtle, FontWeight.Medium), maxLines = 1,
            modifier = Modifier.testTag("run-shoe-check-grade"),
        )
    }
    val durabilityLabel = stringResource(R.string.run_free_check_durability)
    Column(
        Modifier.fillMaxWidth().padding(top = 26.dp).semantics(mergeDescendants = true) {
            contentDescription = "$durabilityLabel $points%"
            progressBarRangeInfo = ProgressBarRangeInfo(points / 100f, 0f..1f)
        }.testTag("run-shoe-check-durability"),
    ) {
        Row(Modifier.fillMaxWidth().padding(bottom = 10.dp), verticalAlignment = Alignment.CenterVertically) {
            Text(durabilityLabel, style = checkText(13.sp, CheckLabel), modifier = Modifier.weight(1f))
            Text("$points%", style = TextStyle(fontFamily = StepUpNumbers, fontWeight = FontWeight.SemiBold, fontSize = 22.sp,
                color = CheckValue, lineHeight = 1.em, fontFeatureSettings = "tnum"),
                modifier = Modifier.testTag("run-shoe-check-durability-value"))
        }
        Box(Modifier.fillMaxWidth().height(7.dp).clip(RoundedCornerShape(8.dp)).background(CheckMeterTrack)) {
            Box(Modifier.fillMaxWidth(points / 100f).height(7.dp).clip(RoundedCornerShape(8.dp)).background(CheckMeterFill))
        }
    }
    // 내구도가 다 닳았거나 곧 닳으면 출발 전에 알린다 — 다 닳은 뒤의 걸음은 적립하지 않는다(서버 0066). 예전엔 0% 에서도
    // "달릴 준비가 됐어요"라, 보상 0 의 까닭을 결과에서도 몰랐다(2026-10-17 점검)
    val worn = (shoe.server?.durabilityFactor ?: 1.0) <= 0.0
    val earnMinutes = durabilityEarnMinutes(shoe)?.takeIf { it < LOW_DURABILITY_WARN_MIN }
    if (worn || earnMinutes != null) {
        Row(Modifier.fillMaxWidth().padding(top = 14.dp).testTag("run-shoe-check-low-durability"), verticalAlignment = Alignment.Top) {
            Icon(Icons.Outlined.Warning, contentDescription = null, tint = CheckWarnIcon, modifier = Modifier.padding(top = 2.dp).size(15.dp))
            Spacer(Modifier.width(7.dp))
            Text(
                if (worn || earnMinutes == null) {
                    stringResource(R.string.run_free_check_low_durability_none)
                } else {
                    stringResource(R.string.run_free_check_low_durability_minutes, earnMinutes.coerceAtLeast(1))
                },
                style = checkText(12.sp, CheckWarnText),
            )
        }
    } else {
        Row(Modifier.fillMaxWidth().padding(top = 14.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Outlined.CheckCircle, contentDescription = null, tint = CheckReadyIcon, modifier = Modifier.size(15.dp))
            Spacer(Modifier.width(7.dp))
            Text(stringResource(R.string.run_free_check_ready), style = checkText(12.sp, CheckReadyText))
        }
    }
}

/** 신발을 신지 않았다 — 지금처럼 신발 없이도 달릴 수 있다. 그 사실만 알린다 */
@Composable
private fun CheckNoShoe() {
    Column(Modifier.fillMaxWidth().padding(top = 28.dp, bottom = 4.dp).testTag("run-shoe-check-none")) {
        Text(stringResource(R.string.home_no_shoe_title), style = checkText(17.sp, Color.White, FontWeight.SemiBold))
        Spacer(Modifier.height(6.dp))
        Text(stringResource(R.string.home_no_shoe_body), style = checkText(13.sp, CheckLabel))
    }
}

/** 점검 카드의 보조 버튼(돌아가기 · 신발 교체) — 같은 크기 */
@Composable
private fun CheckSecondaryButton(text: String, onClick: () -> Unit, tag: String, modifier: Modifier, outlined: Boolean = false) {
    Box(
        modifier.heightIn(min = 50.dp).clip(CheckButtonShape)
            .then(if (outlined) Modifier.border(1.dp, CheckEdge, CheckButtonShape) else Modifier)
            .feedbackClickable(onClick = onClick).padding(horizontal = 8.dp, vertical = 6.dp).testTag(tag),
        contentAlignment = Alignment.Center,
    ) {
        Text(text, style = checkText(14.sp, if (outlined) Color.White else CheckBack, FontWeight.Medium), maxLines = 2,
            overflow = TextOverflow.Ellipsis)
    }
}

/**
 * 신발 고르기 — 내 신발 목록. 신발은 1초 꾹 눌러야 골라진다: 누르는 동안 줄이 왼쪽부터 파랗게 차고 살짝 작아지며,
 * 다 차면 진동과 함께 그 신발을 신는다(신발 탭과 같은 착용 — 서버가 바뀐 것을 확인한 뒤 점검으로 돌아온다).
 * 짧게 누르면 아무것도 바뀌지 않고 "1초 꾹 눌러 골라요"가 한 번 흔들린다.
 */
@Composable
private fun ShoePickList(
    shoes: List<Sneaker>,
    current: Long?,
    wearing: Boolean,
    failed: Boolean,
    onPick: (Long) -> Unit,
    onBack: () -> Unit,
) {
    var nudge by remember { mutableIntStateOf(0) }
    val shake = remember { Animatable(0f) }
    LaunchedEffect(nudge) {
        if (nudge > 0) {
            for (x in listOf(6f, -5f, 3f, 0f)) shake.animateTo(x, tween(55))
        }
    }
    Text(
        stringResource(R.string.run_shoe_pick_title),
        style = checkText(22.sp, Color.White, FontWeight.Bold, 1.33f, (-0.036).em),
        modifier = Modifier.padding(end = 36.dp).semantics { heading() }.testTag("run-shoe-pick-title"),
    )
    Spacer(Modifier.height(4.dp))
    Text(
        stringResource(if (failed) R.string.run_shoe_pick_failed else R.string.run_shoe_pick_hint),
        style = checkText(13.sp, if (failed) CheckWarnText else CheckSubtle),
        modifier = Modifier.graphicsLayer { translationX = shake.value * density }
            .semantics { liveRegion = LiveRegionMode.Polite }.testTag("run-shoe-pick-hint"),
    )
    Spacer(Modifier.height(14.dp))
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        for (item in shoes) {
            ShoePickRow(
                shoe = item, worn = item.id == current, enabled = !wearing,
                onPick = { onPick(item.id) }, onTap = { nudge++ },
            )
        }
    }
    CheckSecondaryButton(
        stringResource(R.string.run_free_check_back), onBack, "run-shoe-pick-back",
        Modifier.padding(top = 16.dp).fillMaxWidth(),
    )
}

/** 신발 한 줄 — 그림 · 이름 · 등급 · 레벨 · 남은 내구도. 1초 꾹 눌러 고른다 */
@Composable
private fun ShoePickRow(shoe: Sneaker, worn: Boolean, enabled: Boolean, onPick: () -> Unit, onTap: () -> Unit) {
    val view = LocalView.current
    val reduced = LocalMotion.current.reduced
    val progress = remember { Animatable(0f) }
    val pick by rememberUpdatedState(onPick)
    val tap by rememberUpdatedState(onTap)
    val shape = RoundedCornerShape(16.dp)
    val name = shoe.shoeName()
    val grade = stringResource(R.string.run_free_check_grade_level, shoe.tier.label(), shoe.level)
    val points = durabilityPercent(shoe)
    val holdLabel = stringResource(R.string.run_shoe_pick_hint)
    androidx.compose.runtime.LaunchedEffect(enabled) { if (!enabled) progress.snapTo(0f) }
    val scope = androidx.compose.runtime.rememberCoroutineScope()
    Row(
        Modifier.fillMaxWidth().heightIn(min = 72.dp)
            .graphicsLayer {
                val k = if (reduced) 1f else 1f - 0.035f * progress.value
                scaleX = k
                scaleY = k
            }
            .clip(shape)
            .background(if (worn) CheckEdge.copy(alpha = .55f) else Color(0x14FFFFFF), shape)
            .border(1.dp, if (worn) StartArcTip else CheckEdge, shape)
            .drawBehind { drawRect(StartAccent.copy(alpha = .45f), size = size.copy(width = size.width * progress.value)) }
            .semantics(mergeDescendants = true) {
                role = androidx.compose.ui.semantics.Role.Button
                contentDescription = "$name · $grade"
                onClick(label = holdLabel) { tap(); true }
                if (enabled) onLongClick(label = name) { pick(); true }
            }
            .pointerInput(enabled) {
                if (!enabled) return@pointerInput
                detectTapGestures(onPress = {
                    view.performHapticFeedback(android.view.HapticFeedbackConstants.VIRTUAL_KEY)
                    var done = false
                    val job = scope.launch {
                        progress.animateTo(1f, tween(PICK_HOLD_MS, easing = androidx.compose.animation.core.LinearEasing))
                        done = true
                        view.performHapticFeedback(android.view.HapticFeedbackConstants.LONG_PRESS)
                        pick()
                    }
                    tryAwaitRelease()
                    if (!done) {
                        job.cancel()
                        scope.launch { progress.animateTo(0f, tween(180)) }
                        tap()
                    } else {
                        scope.launch { progress.animateTo(0f, tween(250)) }
                    }
                })
            }
            .padding(horizontal = 10.dp, vertical = 8.dp)
            .testTag("run-shoe-pick-${shoe.id}"),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        ShoeArtThumbnail(shoe, Modifier.size(width = 64.dp, height = 48.dp), failedTint = CheckSubtle)
        Spacer(Modifier.width(10.dp))
        Column(Modifier.weight(1f)) {
            Text(name, style = checkText(15.sp, Color.White, FontWeight.SemiBold), maxLines = 1, overflow = TextOverflow.Ellipsis)
            Text(grade, style = checkText(12.sp, CheckSubtle), maxLines = 1)
        }
        Spacer(Modifier.width(8.dp))
        Column(horizontalAlignment = Alignment.End) {
            if (worn) {
                Text(stringResource(R.string.run_shoe_pick_wearing), style = checkText(11.sp, StartArcTip, FontWeight.SemiBold), maxLines = 1)
            }
            Text("$points%", style = TextStyle(fontFamily = StepUpNumbers, fontWeight = FontWeight.SemiBold, fontSize = 17.sp,
                color = CheckValue, fontFeatureSettings = "tnum"))
        }
    }
}

/** 신발을 고르는 데 누르고 있어야 하는 시간(ms) — 2026-10-26 사용자 "1초 꾹" */
private const val PICK_HOLD_MS = 1_000

private fun checkText(
    size: androidx.compose.ui.unit.TextUnit,
    color: Color,
    weight: FontWeight = FontWeight.Normal,
    height: Float = 1.45f,
    spacing: androidx.compose.ui.unit.TextUnit = (-0.01).em,
) = TextStyle(fontFamily = StepUpSans, fontWeight = weight, fontSize = size, color = color, lineHeight = height.em, letterSpacing = spacing)

/** 부모의 안쪽 여백 밖으로 좌우 [by] 만큼 넓힌다(시안의 margin: 0 -12px) */
private fun Modifier.bleed(by: Dp): Modifier = layout { measurable, constraints ->
    val extra = (by.roundToPx() * 2)
    val width = (constraints.maxWidth + extra).coerceAtLeast(0)
    val placeable = measurable.measure(constraints.copy(minWidth = width, maxWidth = width))
    layout(constraints.maxWidth, placeable.height) { placeable.place(-by.roundToPx(), 0) }
}

// ── 3 · 2 · 1 · 0 → StepUP! → 지도 펼침 ─────────────────────────────

/**
 * 시퀀스 하나의 주인 — 단조 증가 시간(화면 프레임 시계)으로 COUNTDOWN → LAUNCH → REVEAL 을 한 번에 몬다.
 *
 * - 시각은 앱이 화면에 있을 때만 흐른다. 멈췄다 돌아오면 그 자리부터 잇는다(백그라운드에서 러닝 서비스를 켜지 않는다 —
 *   안드로이드 12+ 가 막는다). 화면을 돌려도 시각 · 시작 요청을 기억해 다시 세거나 두 번 시작하지 않는다.
 * - 러닝 기록은 REVEAL 진입(3,760ms)에서 한 번만 [onStartSession] 으로 시작한다. 러닝이 실제로 켜진 것을 본 뒤에 지도를 펼친다.
 *   시작이 거절되면(끝나지 않은 러닝 · 저장 공간) · 오래 켜지지 않으면 [onFailed] — 기존 안내로 돌아간다.
 * - 기록 전 시스템 뒤로 가기는 [onCancel](아무것도 시작하지 않는다). 시작을 요청한 뒤로는 달리는 중으로 본다 —
 *   뒤로 가기는 기존 러닝 화면의 규칙(일시정지)을 따른다.
 * - 이미 달리는 러닝이 있으면(되살린 러닝 · 알림에서 켠 러닝) 카운트다운을 보이지 않고 바로 [onFinished].
 * - 3-2-1 동안 위치를 미리 찾고(러닝 서비스가 이어 받는다) 지금 자리의 지도 타일을 받아 둔다 — 어느 것도 기다리지 않는다.
 */
@Composable
internal fun FreeRunStartSequence(
    sessionActive: Boolean,
    startRefusals: Int,
    onStartSession: () -> Boolean,
    onFinished: () -> Unit,
    onFailed: () -> Unit,
    onCancel: () -> Unit,
) {
    val motion = LocalMotion.current
    // 시작할 때의 모션 설정으로 끝까지 — 도중에 바뀌어도 한 흐름이다
    val reduced = rememberSaveable { motion.reduced }
    var clock by rememberSaveable { mutableLongStateOf(0L) }
    var requestedAt by rememberSaveable { mutableLongStateOf(-1L) }
    var refusalsAtRequest by rememberSaveable { mutableIntStateOf(0) }
    var revealAt by rememberSaveable { mutableLongStateOf(-1L) }
    val active by rememberUpdatedState(sessionActive)
    val refusals by rememberUpdatedState(startRefusals)
    val start by rememberUpdatedState(onStartSession)
    val finished by rememberUpdatedState(onFinished)
    val failed by rememberUpdatedState(onFailed)
    val lifecycle = LocalLifecycleOwner.current.lifecycle

    LaunchedEffect(Unit) {
        if (active && requestedAt < 0) {
            finished()
            return@LaunchedEffect
        }
        var last = withFrameNanos { it }
        while (true) {
            val now = withFrameNanos { it }
            val step = ((now - last) / 1_000_000L).coerceIn(0L, FreeRunStartMotion.MAX_FRAME_MS)
            last = now
            if (!lifecycle.currentState.isAtLeast(Lifecycle.State.RESUMED)) continue
            clock += step
            when {
                requestedAt < 0 -> if (clock >= FreeRunStartMotion.REVEAL_AT) {
                    requestedAt = clock
                    refusalsAtRequest = refusals
                    if (!start()) {
                        failed()
                        return@LaunchedEffect
                    }
                }
                revealAt < 0 -> when {
                    active -> revealAt = clock
                    refusals != refusalsAtRequest || clock - requestedAt >= FreeRunStartMotion.START_TIMEOUT_MS -> {
                        failed()
                        return@LaunchedEffect
                    }
                }
                clock - revealAt >= FreeRunStartMotion.revealDuration(reduced) -> {
                    finished()
                    return@LaunchedEffect
                }
            }
        }
    }
    // 기록 전 — 시작 취소(아무것도 시작하지 않는다)
    BackHandler(enabled = requestedAt < 0) { onCancel() }
    // 시작을 요청했고 러닝이 켜지는 사이 — 화면을 닫지 않는다(켜진 뒤로는 러닝 화면의 뒤로 가기 규칙)
    BackHandler(enabled = requestedAt >= 0 && !sessionActive) { }

    // 지금 자리를 미리 찾고 그 둘레 지도 타일을 받아 둔다 — 펼친 지도가 바로 그려지게(기다리지 않는다)
    val here = rememberCurrentLocation(enabled = !sessionActive)
    val context = LocalContext.current
    val density = LocalDensity.current.density
    val window = LocalWindowInfo.current.containerSize
    val latestHere by rememberUpdatedState(here)
    LaunchedEffect(Unit) {
        // 처음 자리를 안 순간 한 번 — 펼친 뒤 위치 받기를 멈춰도(러닝 서비스가 이어 받는다) 받던 타일은 끝까지 받는다
        val center = snapshotFlow { latestHere }.filterNotNull().first()
        // 러닝 화면 지도는 화면 위쪽 약 70%(아래는 기록 판)
        runCatching { prefetchRunMapTiles(context, center, window.width, (window.height * 0.7f).toInt(), density) }
    }

    FreeRunStartStage(
        clock = { clock },
        revealSince = { if (revealAt >= 0) clock - revealAt else null },
        reduced = reduced,
    )
}

/**
 * 3·2·1·0 → StepUP! → 지도 펼침의 그림 — 시각만 받아 그린다(상태 없음). [clock] 은 3 이 보인 때부터(ms),
 * [revealSince] 는 지도 펼침이 시작된 때부터(ms, 아직이면 null). 시안 검사가 같은 그림을 고정 시각으로 찍는다.
 *
 * 한 판(같은 네이비 바탕) 위에서 숫자 · 원호 묶음이 빠지는 300ms 동안 투명한 StepUP! 겹이 함께 들어온다 — 둘 다 없는 프레임이 없다.
 * 펼침은 이 판 위에 선행 파랑 · 강조 파랑 원을 키우고, 마지막 원만큼 판에 구멍을 내 아래의 러닝 화면(이미 기록 중)을 드러낸다.
 */
@Composable
internal fun FreeRunStartStage(
    clock: () -> Long,
    revealSince: () -> Long?,
    reduced: Boolean,
    modifier: Modifier = Modifier,
) {
    // 기억해 둔 계산이 처음 받은 시계에 묶이지 않게 — 시계(함수)가 바뀌면 그 시계로 다시 센다(시각을 고정해 찍는 검사가 장면마다 새 시계를 준다)
    val currentClock by rememberUpdatedState(clock)
    val digit by remember { derivedStateOf { FreeRunStartMotion.digitAt(currentClock()) } }
    val launched by remember { derivedStateOf { currentClock() >= FreeRunStartMotion.LAUNCH_AT } }
    val wordmark = stringResource(R.string.run_free_start_wordmark)
    BoxWithConstraints(
        modifier.fillMaxSize()
            .graphicsLayer {
                compositingStrategy = CompositingStrategy.Offscreen
                // 모션 감소 — 지도는 원형 확장 없이 200ms 교차 페이드(아래의 러닝 화면이 드러난다)
                alpha = if (reduced) {
                    revealSince()?.let { 1f - (it.toFloat() / FreeRunStartMotion.REDUCED_FADE_MS).coerceIn(0f, 1f) } ?: 1f
                } else {
                    1f
                }
            }
            .drawWithContent {
                drawContent()
                if (reduced) return@drawWithContent
                val since = revealSince() ?: return@drawWithContent
                val radius = FreeRunStartMotion.maxRadius(size.width, size.height, 12.dp.toPx())
                fun r(layer: FreeRunStartMotion.Layer) = radius * FreeRunStartMotion.revealFraction(layer, since)
                drawCircle(StartRevealLead, r(FreeRunStartMotion.Layer.LEAD), center)
                drawCircle(StartAccent, r(FreeRunStartMotion.Layer.ACCENT), center)
                // 러닝 화면 층 — 이 판에 구멍을 내 아래(이미 기록 중인 자유 러닝 화면)를 드러낸다
                drawCircle(Color.Black, r(FreeRunStartMotion.Layer.SCREEN), center, blendMode = BlendMode.Clear)
            }
            // 연출 동안 아래 화면을 누르지 않게 막는다
            .pointerInput(Unit) { awaitPointerEventScope { while (true) awaitPointerEvent() } }
            .testTag("run-start-sequence"),
    ) {
        val compact = maxWidth < 380.dp
        val baseDiameter = if (compact) 224.dp else 244.dp
        val diameter = minOf(baseDiameter, maxWidth - 48.dp, maxHeight * 0.5f)
        val numberSize = (if (compact) 142.dp else 154.dp) * (diameter / baseDiameter)
        val numberStyle = with(LocalDensity.current) {
            TextStyle(
                fontFamily = StepUpNumbers, fontWeight = FontWeight.ExtraBold, fontStyle = FontStyle.Italic,
                fontSize = numberSize.toSp(), lineHeight = 1.em, letterSpacing = (-6).dp.toSp(), color = Color.White,
                fontFeatureSettings = "tnum", platformStyle = PlatformTextStyle(includeFontPadding = false),
                lineHeightStyle = LineHeightStyle(LineHeightStyle.Alignment.Center, LineHeightStyle.Trim.Both),
            )
        }
        // 바탕 — 딥 네이비, 가운데 옅은 파란 빛(타원 90% × 46%, 높이 48%)
        Canvas(Modifier.fillMaxSize()) {
            drawRect(StartNavy)
            val c = Offset(size.width / 2f, size.height * 0.48f)
            val rx = size.width * 0.9f
            val ry = size.height * 0.46f
            scale(rx / ry, 1f, pivot = c) {
                drawCircle(
                    Brush.radialGradient(0f to StartGlow, 0.8f to StartGlow.copy(alpha = 0f), center = c, radius = ry),
                    radius = ry, center = c,
                )
            }
        }
        // 숫자 · 원호 묶음 — 원호는 3 부터 로고 교차까지 한 번 이어 돈다(숫자마다 다시 돌지 않는다)
        Box(
            Modifier.align(Alignment.Center).size(diameter).graphicsLayer {
                val look = FreeRunStartMotion.countExit(clock() - FreeRunStartMotion.LAUNCH_AT, reduced)
                alpha = look.alpha
                translationY = look.dy.dp.toPx()
                scaleX = look.scale
                scaleY = look.scale
            },
        ) {
            Canvas(Modifier.fillMaxSize()) {
                val outer = size.minDimension / 2f
                // 얇은 트랙(안쪽 1px, 2px 테)
                drawCircle(StartTrack, radius = outer - 2.dp.toPx(), style = Stroke(2.dp.toPx()))
                // 모션 감소면 도는 원호를 그리지 않는다(시안과 같다)
                if (!reduced) {
                    val sweep = Brush.sweepGradient(
                        0f to Color.Transparent,
                        240f / 360f to Color.Transparent,
                        250f / 360f to StartAccent.copy(alpha = 0x22 / 255f),
                        350f / 360f to StartAccent,
                        1f to StartArcTip,
                        center = center,
                    )
                    // conic-gradient(from 0deg) 은 12시에서 시작한다 — 3시에서 시작하는 sweep 을 -90도 돌려 맞춘다
                    rotate(FreeRunStartMotion.arcDegrees(clock()) - 90f) {
                        drawCircle(sweep, radius = outer - 4.dp.toPx(), style = Stroke(8.dp.toPx()))
                    }
                }
            }
            Text(
                digit.toString(), style = numberStyle, maxLines = 1, softWrap = false,
                modifier = Modifier.align(Alignment.Center).offset(x = (-8).dp, y = 3.5.dp)
                    .wrapContentWidth(unbounded = true)
                    .graphicsLayer {
                        val t = clock()
                        val look = FreeRunStartMotion.numberEnter(t - FreeRunStartMotion.digitOnset(t), reduced)
                        alpha = look.alpha
                        translationY = look.dy.dp.toPx()
                        scaleX = look.scale
                        scaleY = look.scale
                    }
                    .semantics { liveRegion = LiveRegionMode.Polite }
                    .testTag("run-countdown-digit"),
            )
        }
        // StepUP — 실제 워드마크 그림(투명한 겹, 카드 · 테두리 없음)
        val groupWidth = minOf(maxWidth - 48.dp, 296.dp)
        Row(
            Modifier.align(Alignment.Center).width(groupWidth).height(82.dp)
                .graphicsLayer {
                    val since = revealSince()
                    val look = if (since != null) {
                        FreeRunStartMotion.wordmarkExit(since, reduced)
                    } else {
                        FreeRunStartMotion.wordmarkEnter(clock() - FreeRunStartMotion.LAUNCH_AT, reduced)
                    }
                    alpha = look.alpha
                    translationX = look.dx.dp.toPx()
                    scaleX = look.scale
                    scaleY = look.scale
                }
                .semantics {
                    if (launched) {
                        contentDescription = wordmark
                        liveRegion = LiveRegionMode.Polite
                    }
                }
                .testTag("run-start-wordmark"),
            horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterHorizontally),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            // 워드마크 그림만(2026-10-18 사용자 "UP 옆 흰 일직선 줄 — 없애") — 시안의 굵은 이탤릭 "!"는 로고보다 길어 세로 줄로 보였다
            RunStartWordmark(groupWidth)
        }
    }
}

/**
 * 펼칠 지도의 타일을 미리 받는다 — 러닝 화면 지도(지금 자리 한가운데, 따라가기 줌)와 같은 계획. 끝나기를 기다리는 곳은 없다.
 * 받은 타일은 지도 캐시(메모리 · 디스크)에 남고, 펼친 지도가 그대로 쓴다. 못 받은 칸은 지도가 늘 하던 대로 다시 받는다.
 */
internal suspend fun prefetchRunMapTiles(context: Context, here: GeoPoint, widthPx: Int, heightPx: Int, density: Float) {
    if (widthPx <= 0 || heightPx <= 0) return
    val plan = TilePlan.of(listOf(here), widthPx, heightPx, density, zooms = MapTiles.FOLLOW_ZOOMS)
    val wanted = buildList {
        for (ty in plan.minTileY..plan.maxTileY) {
            for (tx in plan.minTileX..plan.maxTileX) add(plan.wrapX(tx) to ty)
        }
    }.take(MapTiles.MAX_TILES)
    for (chunk in wanted.chunked(4)) {
        coroutineScope { chunk.map { (tx, ty) -> async { MapTiles.load(context, plan.zoom, tx, ty) } }.awaitAll() }
    }
}
