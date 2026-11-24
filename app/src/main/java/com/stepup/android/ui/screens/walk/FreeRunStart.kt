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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import com.stepup.android.ui.components.PlainBar
import com.stepup.android.ui.components.RunButton
import com.stepup.android.ui.components.RunSheetPanel
import com.stepup.android.ui.components.runTextStyle
import com.stepup.android.ui.components.runTone
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
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
import androidx.compose.ui.draw.alpha
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
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.text.PlatformTextStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.LineHeightStyle
import androidx.compose.ui.text.style.TextOverflow
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
import com.stepup.android.ui.screens.items.durabilityPercent
import com.stepup.android.ui.screens.items.rememberShoeRules
import com.stepup.android.ui.screens.items.shoeName
import com.stepup.android.ui.theme.StepUpNumbers
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

// ── 신발 점검(SHOE_CHECK) ───────────────────────────────────────────

/**
 * 출발 전 신발 점검(2026-11-24 재디자인 04 · 04b) — 테마를 따라가는 아래 시트. 신고 있는 신발의 그림 · 이름 · 등급 · 레벨 ·
 * 남은 내구도(실제 값)와 적립 한 줄. 주 버튼 "이 신발로 출발"만 카운트다운으로 간다. 시트의 × · 뒤로는 아무것도 시작하지 않는다.
 * 신발을 신지 않았으면 그 사실을 알리고(지금처럼 신발 없이도 달릴 수 있다) "러닝 시작"으로 같은 흐름을 잇는다.
 * 다 닳은 신발이면 주 버튼이 "수리하고 출발"(이 화면의 수리 시트 — 고치고 닫으면 점검으로 돌아온다)이고 "그대로 출발"은 글자 버튼이다.
 * 신발 교체(글자 버튼)나 신발 그림을 누르면 내 신발 목록이 열리고, 신발은 1초 꾹 눌러야 골라진다
 * (누르는 동안 줄이 차오르고 살짝 작아진다 — 스치듯 눌러 바뀌지 않게).
 */
@Composable
internal fun FreeRunShoeCheck(
    shoe: Sneaker?,
    shoes: List<Sneaker>,
    wearing: Boolean,
    onWear: (Long, (Boolean) -> Unit) -> Unit,
    onStart: () -> Unit,
    onCancel: () -> Unit,
    /** 다 닳은 신발의 "수리" — 이 신발의 수리 시트를 연다(null 이면 수리 버튼을 두지 않는다) */
    onRepair: ((Long) -> Unit)? = null,
    /** 이번 러닝에 고를 수 있는 아이템(가진 것만, 서버 0106) — 비어 있으면 줄을 두지 않는다 */
    runItems: List<com.stepup.android.domain.BagItem> = emptyList(),
    pickedItems: Set<com.stepup.android.domain.ItemKind> = emptySet(),
    onPickItem: (com.stepup.android.domain.ItemKind) -> Unit = {},
    /** 아이템 없이도 이미 켜진 적립 배수(보상 이벤트 · 24시간 부스터) — 이보다 크지 않은 배수 아이템은 고르지 못한다 */
    coveredRewardMult: Double = 1.0,
    /** 오늘 적립 한도(에너지)를 다 썼다 — 이 러닝은 적립이 없다는 한 줄(출발은 그대로 된다) */
    energyUsedUp: Boolean = false,
    /** 오늘 남은 에너지로 이 신발이 적립할 걸음([todayEarnableSteps]) — 모르면 null(신발 줄이 적립 분을 약속하지 않는다) */
    energySteps: Int? = null,
) {
    // 잠금 화면 덮개가 덮고 있으면 창을 띄우지 않는다(LockCover)
    if (com.stepup.android.ui.components.LockCover.hidesWindows()) return
    Dialog(
        onDismissRequest = onCancel,
        properties = DialogProperties(usePlatformDefaultWidth = false, decorFitsSystemWindows = false, dismissOnClickOutside = false),
    ) {
        // 창이 깔아 주는 회색 대신 시트 뒷면을 직접 그린다
        val window = (LocalView.current.parent as? DialogWindowProvider)?.window
        SideEffect { window?.setDimAmount(0f) }
        FreeRunShoeCheckContent(
            shoe, onStart, onCancel, shoes = shoes, wearing = wearing, onWear = onWear, onRepair = onRepair,
            runItems = runItems, pickedItems = pickedItems, onPickItem = onPickItem,
            coveredRewardMult = coveredRewardMult, energyUsedUp = energyUsedUp, energySteps = energySteps,
        )
    }
}

/** [FreeRunShoeCheck] 의 화면(뒷면 + 아래 시트) — 시안 검사가 창 없이 그린다 */
@Composable
internal fun FreeRunShoeCheckContent(
    shoe: Sneaker?,
    onStart: () -> Unit,
    onCancel: () -> Unit,
    modifier: Modifier = Modifier,
    shoes: List<Sneaker> = emptyList(),
    wearing: Boolean = false,
    onWear: (Long, (Boolean) -> Unit) -> Unit = { _, _ -> },
    onRepair: ((Long) -> Unit)? = null,
    runItems: List<com.stepup.android.domain.BagItem> = emptyList(),
    pickedItems: Set<com.stepup.android.domain.ItemKind> = emptySet(),
    onPickItem: (com.stepup.android.domain.ItemKind) -> Unit = {},
    coveredRewardMult: Double = 1.0,
    energyUsedUp: Boolean = false,
    energySteps: Int? = null,
) {
    val reduced = LocalMotion.current.reduced
    val t = runTone()
    // 신발 고르기를 열었나 — 같은 시트 안에서 목록으로 바뀐다. 뒤로 가기는 점검으로 돌아온다
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
    Box(modifier.fillMaxSize().testTag("run-shoe-check"), contentAlignment = Alignment.BottomCenter) {
        // 뒷면 — 누르면 아무 일도 없다(점검은 버튼 · × 로만 닫는다). 아래 러닝 화면을 누르지 않게 막는다
        Box(
            Modifier.matchParentSize().graphicsLayer { alpha = shown.value }.background(t.scrim)
                .pointerInput(Unit) { awaitPointerEventScope { while (true) awaitPointerEvent() } },
        )
        val lift = with(LocalDensity.current) { 48.dp.toPx() }
        RunSheetPanel(
            onClose = onCancel,
            closeTag = "run-shoe-check-close",
            modifier = Modifier.statusBarsPadding().padding(top = 24.dp).widthIn(max = 560.dp)
                .graphicsLayer {
                    alpha = shown.value
                    translationY = if (reduced) 0f else (1f - shown.value) * lift
                },
        ) {
            val worn = shoe?.let {
                val line = runCheckLine(it, rememberShoeRules(), energySteps)
                line == ShoeCheckLine.WornOut || line == ShoeCheckLine.WornNoEnergy
            } == true
            // 다 닳았고 이 화면에서 고칠 수 있다 — 주 버튼이 수리(신발을 바꾸는 중에는 고치지 않는다)
            val repair: (() -> Unit)? = if (shoe != null && worn && !wearing) onRepair?.let { r -> { r(shoe.id) } } else null
            Column(Modifier.fillMaxWidth().verticalScroll(rememberScrollState())) {
                if (picking) {
                    ShoePickList(
                        shoes = shoes, current = shoe?.id, wearing = wearing, failed = pickFailed,
                        onPick = { id ->
                            if (id == shoe?.id) {
                                picking = false
                            } else {
                                pickFailed = false
                                onWear(id) { ok -> if (ok) picking = false else pickFailed = true }
                            }
                        },
                        onBack = { picking = false },
                    )
                } else {
                    Text(
                        stringResource(R.string.rd_a_check_title),
                        style = runTextStyle(22.sp, t.text, FontWeight.Bold, 1.3f),
                        modifier = Modifier.padding(end = 48.dp).semantics { heading() }.testTag("run-shoe-check-title"),
                    )
                    if (shoe != null) {
                        CheckShoe(shoe, onClick = if (canChange) ({ picking = true }) else null)
                        CheckShoeFacts(shoe, energySteps = energySteps)
                    } else {
                        CheckNoShoe()
                    }
                    // 오늘 적립 한도를 다 썼다(경제 4판 — 하루 에너지). 출발은 그대로 된다
                    if (energyUsedUp) CheckEnergyUsedUp()
                    // 이번 러닝에 쓸 아이템(0106) — 가진 것만, 같은 묶음(적립 배수 · 에너지)은 하나만. 출발하면 그 러닝에 묶여 빠진다
                    if (runItems.isNotEmpty()) {
                        RunItemPicker(runItems, pickedItems, onPickItem, enabled = !wearing, coveredRewardMult = coveredRewardMult)
                    }
                    Spacer(Modifier.height(20.dp))
                    if (repair != null) {
                        // 다 닳은 신발 — 주 버튼 "수리하고 출발"(수리 시트를 연다), 아래 "그대로 출발" · "신발 교체"
                        RunButton(stringResource(R.string.rd_a_check_repair_go), repair, modifier = Modifier.testTag("run-shoe-check-repair"))
                        Row(Modifier.fillMaxWidth().padding(top = 4.dp), horizontalArrangement = Arrangement.Center) {
                            CheckTextButton(stringResource(R.string.rd_a_check_go_anyway), start, "run-shoe-check-go")
                            if (canChange) CheckTextButton(stringResource(R.string.run_free_check_change), { picking = true }, "run-shoe-check-change")
                        }
                    } else {
                        // 이 신발로 출발(주 버튼) — 아래 가운데 "신발 교체"
                        RunButton(
                            stringResource(if (shoe != null) R.string.run_free_check_go else R.string.home_start_run), start,
                            enabled = !wearing, modifier = Modifier.testTag("run-shoe-check-go"),
                        )
                        if (canChange) {
                            Box(Modifier.fillMaxWidth().padding(top = 4.dp), contentAlignment = Alignment.Center) {
                                CheckTextButton(stringResource(R.string.run_free_check_change), { picking = true }, "run-shoe-check-change")
                            }
                        }
                    }
                }
            }
        }
    }
}

/** 시트 아래 회색 글자 버튼(신발 교체 · 그대로 출발) */
@Composable
private fun CheckTextButton(text: String, onClick: () -> Unit, tag: String, enabled: Boolean = true) {
    val t = runTone()
    Box(
        Modifier.heightIn(min = 48.dp).clip(RoundedCornerShape(12.dp)).feedbackClickable(enabled = enabled, onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 8.dp).testTag(tag),
        contentAlignment = Alignment.Center,
    ) {
        Text(text, style = runTextStyle(15.5.sp, if (enabled) t.label else t.muted, FontWeight.SemiBold), maxLines = 1, softWrap = false)
    }
}

/** 신발 그림(112) · 가운데 이름 · 등급 · 레벨. 그림을 누르면 신발 고르기([onClick]) */
@Composable
private fun CheckShoe(shoe: Sneaker, onClick: (() -> Unit)?) {
    val t = runTone()
    Column(Modifier.fillMaxWidth().padding(top = 8.dp), horizontalAlignment = Alignment.CenterHorizontally) {
        ShoeArtThumbnail(
            shoe,
            Modifier.fillMaxWidth(0.72f).height(112.dp)
                .then(if (onClick != null) Modifier.clip(RoundedCornerShape(18.dp)).feedbackClickable(onClick = onClick) else Modifier)
                .testTag("run-shoe-check-art"),
            failedTint = t.muted, sample = ShoeArtFull,
        )
        Spacer(Modifier.height(10.dp))
        Text(
            shoe.shoeName(), style = runTextStyle(20.sp, t.text, FontWeight.Bold, 1.25f), maxLines = 1, overflow = TextOverflow.Ellipsis,
            textAlign = androidx.compose.ui.text.style.TextAlign.Center,
        )
        Text(
            stringResource(R.string.run_free_check_grade_level, shoe.tier.label(), shoe.level),
            style = runTextStyle(14.sp, t.label, FontWeight.Medium), maxLines = 1,
            modifier = Modifier.testTag("run-shoe-check-grade"),
        )
    }
}

/**
 * 남은 내구도 — 테두리 카드 한 칸: "남은 내구도" · 값(넉넉하면 dur 색, 곧 닳으면 gold, 다 닳았으면 빨강) · 가는 막대 · 적립 한 줄.
 * 적립 한 줄은 [runCheckLine] 그대로 — 신발 수명과 오늘 남은 에너지 가운데 먼저 끝나는 쪽(2026-11-22 경제 점검), 에너지를 모르면 적립 분을
 * 말하지 않는다. 닳은 · 곧 닳는 신발은 출발 전에 알린다(서버 0119 — 닳은 신발도 출발은 막지 않는다: 걸음은 세고 N%만 적립).
 * 신발 탭 · 상세의 수리 강조(repairUrgent)와 같은 규칙이다.
 */
@Composable
private fun CheckShoeFacts(shoe: Sneaker, energySteps: Int? = null) {
    val t = runTone()
    val points = durabilityPercent(shoe)
    val rules = rememberShoeRules()
    val line = runCheckLine(shoe, rules, energySteps)
    val worn = line == ShoeCheckLine.WornOut || line == ShoeCheckLine.WornNoEnergy
    val low = line is ShoeCheckLine.Low
    val tone = when {
        worn -> t.dangerText
        low -> t.gold
        else -> t.dur
    }
    val durabilityLabel = stringResource(R.string.run_free_check_durability)
    val shape = RoundedCornerShape(22.dp)
    Column(
        Modifier.fillMaxWidth().padding(top = 18.dp).clip(shape).border(1.dp, t.divider, shape)
            .padding(horizontal = 18.dp, vertical = 16.dp),
    ) {
        Column(
            Modifier.fillMaxWidth().semantics(mergeDescendants = true) {
                contentDescription = "$durabilityLabel $points%"
                progressBarRangeInfo = ProgressBarRangeInfo(points / 100f, 0f..1f)
            }.testTag("run-shoe-check-durability"),
        ) {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Text(
                    durabilityLabel, style = runTextStyle(15.5.sp, t.text, FontWeight.SemiBold, 1.3f),
                    maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f),
                )
                Spacer(Modifier.width(12.dp))
                Text(
                    "$points%", style = runTextStyle(18.sp, tone, FontWeight.Bold, 1.2f).copy(fontFeatureSettings = "tnum"),
                    maxLines = 1, softWrap = false, modifier = Modifier.testTag("run-shoe-check-durability-value"),
                )
            }
            Spacer(Modifier.height(9.dp))
            PlainBar(points / 100f, tone, Modifier.fillMaxWidth(), height = 4.dp)
        }
        Spacer(Modifier.height(9.dp))
        val text = shoeCheckLineText(line, rules)
        if (worn || low) {
            Text(text, style = runTextStyle(13.5.sp, t.label, FontWeight.Medium, 1.4f), modifier = Modifier.testTag("run-shoe-check-low-durability"))
        } else {
            Box(Modifier.testTag("run-shoe-check-ready")) {
                Text(text, style = runTextStyle(13.5.sp, t.label, FontWeight.Medium, 1.4f), modifier = Modifier.testTag("run-shoe-check-earn"))
            }
        }
    }
}

/** 오늘 적립 한도를 다 썼다 — 이 러닝은 적립이 없다(기록은 남는다). 카드 밑 한 줄(주의 색), 버튼은 더하지 않는다 */
@Composable
private fun CheckEnergyUsedUp() {
    val t = runTone()
    Text(
        stringResource(R.string.run_check_energy_empty), style = runTextStyle(13.5.sp, t.gold, FontWeight.SemiBold, 1.4f),
        modifier = Modifier.fillMaxWidth().padding(top = 10.dp, start = 4.dp).testTag("run-shoe-check-energy-empty"),
    )
}

/** 신발을 신지 않았다 — 지금처럼 신발 없이도 달릴 수 있다. 그 사실만 알린다 */
@Composable
private fun CheckNoShoe() {
    val t = runTone()
    Column(Modifier.fillMaxWidth().padding(top = 20.dp, bottom = 4.dp).testTag("run-shoe-check-none")) {
        Text(stringResource(R.string.home_no_shoe_title), style = runTextStyle(17.sp, t.text, FontWeight.SemiBold))
        Spacer(Modifier.height(6.dp))
        Text(stringResource(R.string.home_no_shoe_body), style = runTextStyle(14.sp, t.label, FontWeight.Medium, 1.45f))
    }
}

/**
 * 출발 전 아이템 고르기(2026-11-17 사용자 "출발 전에 고르기 · 같은 종류는 겹쳐서 못 쓴다", 서버 0106).
 * 작은 칸 셋까지 — 그림 · 이름 · 보유 개수, 고른 칸은 파란 테두리. 아래 한 줄에 고른 것의 효과를 모아 보인다.
 */
@Composable
private fun RunItemPicker(
    items: List<com.stepup.android.domain.BagItem>,
    picked: Set<com.stepup.android.domain.ItemKind>,
    onPick: (com.stepup.android.domain.ItemKind) -> Unit,
    enabled: Boolean,
    coveredRewardMult: Double = 1.0,
) {
    val t = runTone()
    // 이미 켜진 배수(보상 이벤트 · 24시간 부스터)와 같거나 작은 배수 아이템 — 서버는 큰 것 하나만 쓰므로 고르면 아이템만 사라진다
    fun covered(item: com.stepup.android.domain.BagItem): Boolean =
        item.kind.group == "REWARD" && coveredRewardMult > 1.0 && (item.value ?: 1.0) <= coveredRewardMult
    Column(Modifier.padding(top = 18.dp).fillMaxWidth().testTag("run-items")) {
        Row(verticalAlignment = Alignment.Bottom) {
            Text(
                stringResource(R.string.run_items_title), style = runTextStyle(15.5.sp, t.text, FontWeight.SemiBold),
                modifier = Modifier.weight(1f).semantics { heading() },
            )
            Text(stringResource(R.string.run_items_hint), style = runTextStyle(12.5.sp, t.label, FontWeight.Medium))
        }
        Row(Modifier.padding(top = 10.dp).fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            items.take(3).forEach { item ->
                val blocked = covered(item)
                val on = item.kind in picked && !blocked
                val shape = RoundedCornerShape(16.dp)
                Column(
                    Modifier.weight(1f).clip(shape)
                        .alpha(if (blocked) 0.42f else 1f)
                        .background(t.inset, shape)
                        .then(if (on) Modifier.border(2.dp, t.cobalt, shape) else Modifier)
                        .feedbackClickable(enabled = enabled && !blocked, role = androidx.compose.ui.semantics.Role.Checkbox) { onPick(item.kind) }
                        .semantics { selected = on }
                        .padding(vertical = 8.dp, horizontal = 4.dp)
                        .testTag("run-item-${item.kind.id}"),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    androidx.compose.foundation.Image(
                        androidx.compose.ui.res.painterResource(com.stepup.android.ui.screens.items.itemArt(item.kind)),
                        contentDescription = null, modifier = Modifier.size(52.dp),
                    )
                    Text(
                        com.stepup.android.ui.screens.items.itemName(item.kind),
                        style = runTextStyle(12.sp, t.text, FontWeight.SemiBold), maxLines = 2,
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                    )
                    Text(
                        stringResource(R.string.run_items_owned, item.qty),
                        style = runTextStyle(11.5.sp, if (on) t.cobaltText else t.label, FontWeight.Medium),
                    )
                }
            }
        }
        if (items.take(3).any(::covered)) {
            Text(
                stringResource(R.string.run_items_covered, com.stepup.android.ui.screens.items.formatValue(coveredRewardMult)),
                style = runTextStyle(12.5.sp, t.label, FontWeight.Medium),
                modifier = Modifier.padding(top = 8.dp).testTag("run-items-covered"),
            )
        }
        val usable = picked.filter { kind -> items.none { it.kind == kind && covered(it) } }
        if (usable.isNotEmpty()) {
            // map 은 인라인이라 그 안에서 문구(컴포저블)를 읽을 수 있다 — 모은 뒤 잇는다
            val effects = usable.sortedBy { it.ordinal }.map { kind ->
                com.stepup.android.ui.screens.items.effectText(kind, items.firstOrNull { it.kind == kind }?.value)
            }
            Text(
                effects.joinToString(" · ") + " — " + stringResource(R.string.run_items_note),
                style = runTextStyle(12.5.sp, t.cobaltText, FontWeight.Medium), modifier = Modifier.padding(top = 8.dp),
            )
        }
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
    val t = runTone()
    var nudge by remember { mutableIntStateOf(0) }
    val shake = remember { Animatable(0f) }
    LaunchedEffect(nudge) {
        if (nudge > 0) {
            for (x in listOf(6f, -5f, 3f, 0f)) shake.animateTo(x, tween(55))
        }
    }
    Text(
        stringResource(R.string.run_shoe_pick_title),
        style = runTextStyle(22.sp, t.text, FontWeight.Bold, 1.3f),
        modifier = Modifier.padding(end = 48.dp).semantics { heading() }.testTag("run-shoe-pick-title"),
    )
    Spacer(Modifier.height(4.dp))
    Text(
        stringResource(if (failed) R.string.run_shoe_pick_failed else R.string.run_shoe_pick_hint),
        style = runTextStyle(13.5.sp, if (failed) t.dangerText else t.label, FontWeight.Medium),
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
    Box(Modifier.fillMaxWidth().padding(top = 10.dp), contentAlignment = Alignment.Center) {
        CheckTextButton(stringResource(R.string.run_free_check_back), onBack, "run-shoe-pick-back")
    }
}

/** 신발 한 줄 — 그림 · 이름 · 등급 · 레벨 · 남은 내구도. 1초 꾹 눌러 고른다 */
@Composable
private fun ShoePickRow(shoe: Sneaker, worn: Boolean, enabled: Boolean, onPick: () -> Unit, onTap: () -> Unit) {
    val t = runTone()
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
            .background(t.inset, shape)
            .then(if (worn) Modifier.border(2.dp, t.cobalt, shape) else Modifier)
            .drawBehind { drawRect(t.cobalt.copy(alpha = .3f), size = size.copy(width = size.width * progress.value)) }
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
        ShoeArtThumbnail(shoe, Modifier.size(width = 64.dp, height = 48.dp), failedTint = t.muted)
        Spacer(Modifier.width(10.dp))
        Column(Modifier.weight(1f)) {
            Text(name, style = runTextStyle(15.5.sp, t.text, FontWeight.SemiBold), maxLines = 1, overflow = TextOverflow.Ellipsis)
            Text(grade, style = runTextStyle(12.5.sp, t.label, FontWeight.Medium), maxLines = 1)
        }
        Spacer(Modifier.width(8.dp))
        Column(horizontalAlignment = Alignment.End) {
            if (worn) {
                Text(stringResource(R.string.run_shoe_pick_wearing), style = runTextStyle(11.5.sp, t.cobaltText, FontWeight.SemiBold), maxLines = 1)
            }
            Text(
                "$points%", style = runTextStyle(16.sp, t.text, FontWeight.Bold, 1.2f).copy(fontFeatureSettings = "tnum"),
                maxLines = 1, softWrap = false,
            )
        }
    }
}

/** 신발을 고르는 데 누르고 있어야 하는 시간(ms) — 2026-10-26 사용자 "1초 꾹" */
private const val PICK_HOLD_MS = 1_000

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
