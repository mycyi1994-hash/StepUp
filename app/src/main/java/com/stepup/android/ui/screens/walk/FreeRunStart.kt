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
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowRight
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
import androidx.compose.ui.semantics.selected
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
import com.stepup.android.ui.screens.items.durabilityPercent
import com.stepup.android.ui.screens.items.rememberShoeRules
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
    /** 다 닳은 신발의 "수리" — 이 신발의 수리 시트를 연다(null 이면 줄을 두지 않는다) */
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
    // 13차(2026-11-26 S-399) — 가운데 팝업 대신 아래에서 올라오는 시트(지도 판 위로). 닫기는 손잡이 · X
    com.stepup.android.ui.components.RunSheet(onDismiss = onCancel, closeTag = "run-shoe-check-close") {
        FreeRunShoeCheckContent(
            shoe, onStart, onCancel, shoes = shoes, wearing = wearing, onWear = onWear, onRepair = onRepair,
            runItems = runItems, pickedItems = pickedItems, onPickItem = onPickItem,
            coveredRewardMult = coveredRewardMult, energyUsedUp = energyUsedUp, energySteps = energySteps,
        )
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
    onRepair: ((Long) -> Unit)? = null,
    runItems: List<com.stepup.android.domain.BagItem> = emptyList(),
    pickedItems: Set<com.stepup.android.domain.ItemKind> = emptySet(),
    onPickItem: (com.stepup.android.domain.ItemKind) -> Unit = {},
    coveredRewardMult: Double = 1.0,
    energyUsedUp: Boolean = false,
    energySteps: Int? = null,
) {
    // 신발 교체(S-400) · 아이템 고르기(S-401)는 같은 시트 안에서 바뀐다. 뒤로 가기는 확인으로 돌아온다
    var picking by rememberSaveable { mutableStateOf(false) }
    var itemsOpen by rememberSaveable { mutableStateOf(false) }
    var pickFailed by remember { mutableStateOf(false) }
    val canChange = shoes.isNotEmpty()
    BackHandler(enabled = picking || itemsOpen) { picking = false; itemsOpen = false }
    val context = LocalContext.current
    // StepUP! 그림을 뒤에서 풀어 둔다(전환 중에 풀지 않게)
    LaunchedEffect(Unit) { prewarmRunStartWordmark(context) }
    // 출발은 한 번만 — 연타 · 겹친 누름에도 시작 요청은 하나다
    var fired by remember { mutableStateOf(false) }
    val start = {
        if (!fired) {
            fired = true
            onStart()
        }
    }
    val c = com.stepup.android.ui.components.StepUpUi.colors
    Column(modifier.fillMaxWidth().testTag("run-shoe-check")) {
        when {
            picking -> V13ShoePick(
                shoes = shoes, current = shoe?.id, wearing = wearing, failed = pickFailed,
                onPick = { id ->
                    if (id == shoe?.id) {
                        picking = false
                    } else {
                        pickFailed = false
                        onWear(id) { worn -> if (worn) picking = false else pickFailed = true }
                    }
                },
            )
            itemsOpen -> {
                Text(stringResource(R.string.run_items_title), color = c.label, fontSize = 22.sp, fontWeight = FontWeight.ExtraBold,
                    modifier = Modifier.semantics { heading() })
                RunItemPicker(runItems, pickedItems, onPickItem, enabled = !wearing, coveredRewardMult = coveredRewardMult)
                Spacer(Modifier.height(20.dp))
                com.stepup.android.ui.components.PrimaryButton(
                    stringResource(R.string.v13_items_done), { itemsOpen = false }, arrow = false,
                    modifier = Modifier.fillMaxWidth().testTag("run-items-done"),
                )
            }
            else -> {
                Text(
                    stringResource(R.string.v13_check_title), color = c.label, fontSize = 22.sp, fontWeight = FontWeight.ExtraBold,
                    modifier = Modifier.padding(end = 40.dp).semantics { heading() }.testTag("run-shoe-check-title"),
                )
                Spacer(Modifier.height(16.dp))
                if (shoe != null) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        com.stepup.android.ui.components.V12ShoeCard(
                            shoe, Modifier.width(104.dp).testTag("run-shoe-check-card"), hero = false, mini = true,
                            onClick = if (canChange) ({ picking = true }) else null,
                        )
                        Spacer(Modifier.width(16.dp))
                        Column(Modifier.weight(1f)) {
                            Text(stringResource(R.string.v13_check_shoe_caption), color = c.secondaryLabel, fontSize = 14.sp)
                            Text(shoe.shoeName(), color = c.label, fontSize = 22.sp, fontWeight = FontWeight.ExtraBold, maxLines = 2,
                                overflow = TextOverflow.Ellipsis, modifier = Modifier.testTag("run-shoe-check-name"))
                            Text(
                                stringResource(R.string.run_free_check_grade_level, shoe.tier.label(), shoe.level),
                                color = c.secondaryLabel, fontSize = 13.sp, modifier = Modifier.testTag("run-shoe-check-grade"),
                            )
                            if (canChange) {
                                Spacer(Modifier.height(10.dp))
                                Text(
                                    stringResource(R.string.v13_check_change) + " ›", color = c.accentText, fontSize = 15.sp, fontWeight = FontWeight.Bold,
                                    modifier = Modifier.clip(RoundedCornerShape(8.dp)).feedbackClickable(enabled = !wearing) { picking = true }
                                        .padding(vertical = 6.dp).testTag("run-shoe-check-change"),
                                )
                            }
                        }
                    }
                    Spacer(Modifier.height(18.dp))
                    CheckShoeFacts(shoe, onRepair = onRepair?.let { repair -> { repair(shoe.id) } }.takeIf { !wearing }, energySteps = energySteps)
                } else {
                    CheckNoShoe()
                }
                // 오늘 적립 한도를 다 썼다 — 출발은 그대로 된다
                if (energyUsedUp) CheckEnergyUsedUp()
                // 이번 러닝에 쓸 아이템(0106) — 한 줄, 누르면 고르기(S-401)
                if (runItems.isNotEmpty()) {
                    androidx.compose.foundation.layout.Box(Modifier.fillMaxWidth().padding(top = 14.dp).height(1.dp).background(c.separator))
                    val picked = runItems.filter { it.kind in pickedItems }
                    Row(
                        Modifier.fillMaxWidth().heightIn(min = 56.dp).clip(RoundedCornerShape(10.dp))
                            .feedbackClickable(enabled = !wearing) { itemsOpen = true }.testTag("run-items-row"),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(stringResource(R.string.v13_check_items), color = c.label, fontSize = 16.sp, fontWeight = FontWeight.SemiBold,
                            modifier = Modifier.weight(1f))
                        picked.firstOrNull()?.let { item ->
                            androidx.compose.foundation.Image(
                                androidx.compose.ui.res.painterResource(com.stepup.android.ui.screens.items.itemArt(item.kind)),
                                contentDescription = null, modifier = Modifier.size(32.dp),
                            )
                            Spacer(Modifier.width(6.dp))
                        }
                        Text(
                            when {
                                picked.isEmpty() -> stringResource(R.string.v13_check_items_none)
                                picked.size == 1 -> com.stepup.android.ui.screens.items.itemName(picked.first().kind)
                                else -> com.stepup.android.ui.screens.items.itemName(picked.first().kind) + " +" + (picked.size - 1)
                            },
                            color = if (picked.isEmpty()) c.secondaryLabel else c.label, fontSize = 15.sp, fontWeight = FontWeight.SemiBold,
                            maxLines = 1,
                        )
                        Icon(androidx.compose.material.icons.Icons.AutoMirrored.Rounded.KeyboardArrowRight, null, tint = c.tertiaryLabel, modifier = Modifier.size(22.dp))
                    }
                }
                Spacer(Modifier.height(16.dp))
                com.stepup.android.ui.components.PrimaryButton(
                    stringResource(if (shoe != null) R.string.run_free_check_go else R.string.home_start_run), start,
                    state = if (wearing) com.stepup.android.ui.components.ButtonState.Disabled else com.stepup.android.ui.components.ButtonState.Idle,
                    modifier = Modifier.fillMaxWidth().testTag("run-shoe-check-go"),
                )
            }
        }
    }
}

/**
 * 신발 교체(13차 S-400) — 내 신발 줄(작은 카드 · 이름 · 내구도 막대 · 남은 / 가득 분), 신고 있는 신발은 "✓ 착용 중".
 * 줄을 눌러 고르고 아래 "○○로 교체"로 신는다(예전 1초 꾹 누르기 대신 — 고른 뒤 한 번 더 누르니 스치듯 바뀌지 않는다).
 */
@Composable
private fun V13ShoePick(shoes: List<Sneaker>, current: Long?, wearing: Boolean, failed: Boolean, onPick: (Long) -> Unit) {
    val c = com.stepup.android.ui.components.StepUpUi.colors
    val rules = rememberShoeRules()
    var chosen by rememberSaveable { mutableStateOf(current) }
    Text(stringResource(R.string.run_shoe_pick_title), color = c.label, fontSize = 22.sp, fontWeight = FontWeight.ExtraBold,
        modifier = Modifier.padding(end = 40.dp).semantics { heading() }.testTag("run-shoe-pick-title"))
    if (failed) {
        Text(stringResource(R.string.run_shoe_pick_failed), color = c.live, fontSize = 13.sp,
            modifier = Modifier.padding(top = 4.dp).semantics { liveRegion = LiveRegionMode.Polite }.testTag("run-shoe-pick-hint"))
    }
    Spacer(Modifier.height(12.dp))
    Column(Modifier.heightIn(max = 420.dp).verticalScroll(rememberScrollState())) {
        shoes.forEach { item ->
            val on = item.id == chosen
            Row(
                Modifier.fillMaxWidth().clip(RoundedCornerShape(14.dp))
                    .feedbackClickable(enabled = !wearing, role = androidx.compose.ui.semantics.Role.RadioButton) { chosen = item.id }
                    .semantics { selected = on }
                    .padding(vertical = 10.dp).testTag("run-shoe-pick-${item.id}"),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                androidx.compose.foundation.layout.Box(
                    if (on) Modifier.border(2.dp, c.primary, RoundedCornerShape(12.dp)).padding(2.dp) else Modifier.padding(4.dp),
                ) {
                    com.stepup.android.ui.components.V12ShoeCard(item, Modifier.width(76.dp), hero = false, mini = true)
                }
                Spacer(Modifier.width(14.dp))
                Column(Modifier.weight(1f)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(item.shoeName(), color = c.label, fontSize = 17.sp, fontWeight = FontWeight.Bold, maxLines = 1,
                            overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f))
                        if (item.id == current) {
                            Text("✓ " + stringResource(R.string.run_shoe_pick_wearing), color = c.accentText, fontSize = 13.sp,
                                fontWeight = FontWeight.Bold)
                        }
                    }
                    Spacer(Modifier.height(10.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        val f = durabilityPercent(item) / 100f
                        androidx.compose.foundation.layout.Box(Modifier.weight(1f).height(6.dp).clip(RoundedCornerShape(3.dp)).background(c.fill)) {
                            if (f > 0f) androidx.compose.foundation.layout.Box(Modifier.fillMaxWidth(f).height(6.dp)
                                .clip(RoundedCornerShape(3.dp)).background(c.secondaryLabel))
                        }
                        Spacer(Modifier.width(12.dp))
                        Text(com.stepup.android.ui.screens.items.durabilityText(item, rules), color = c.label, fontSize = 15.sp,
                            fontWeight = FontWeight.Bold, maxLines = 1)
                    }
                }
            }
        }
    }
    Spacer(Modifier.height(16.dp))
    val target = shoes.firstOrNull { it.id == chosen }
    val changing = target != null && target.id != current
    com.stepup.android.ui.components.PrimaryButton(
        if (changing) stringResource(R.string.v13_check_change_to, withRo(target!!.shoeName())) else stringResource(R.string.v13_items_done),
        { chosen?.let(onPick) },
        state = when {
            wearing -> com.stepup.android.ui.components.ButtonState.Loading
            else -> com.stepup.android.ui.components.ButtonState.Idle
        },
        modifier = Modifier.fillMaxWidth().testTag("run-shoe-pick-confirm"),
    )
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

/** 이름 줄(등급 · 레벨) · 남은 내구도 막대 · 준비 줄. 다 닳았으면 경고 줄 아래 "수리하기"([onRepair]) — 주 버튼은 그대로 하나다 */
@Composable
private fun CheckShoeFacts(shoe: Sneaker, onRepair: (() -> Unit)? = null, energySteps: Int? = null) {
    // 13차(S-399) — 내구도 줄("18 / 31분" + 수리) · 막대 · 한 줄("18분 뒤부터 보상 30%" — 신발 수명과 오늘 에너지 가운데 먼저 끝나는 쪽, runCheckLine)
    val c = com.stepup.android.ui.components.StepUpUi.colors
    val rules = rememberShoeRules()
    val points = durabilityPercent(shoe)
    val line = runCheckLine(shoe, rules, energySteps)
    val worn = line == ShoeCheckLine.WornOut || line == ShoeCheckLine.WornNoEnergy
    val urgent = worn || line is ShoeCheckLine.Low
    val durabilityLabel = stringResource(R.string.run_free_check_durability)
    Column(
        Modifier.fillMaxWidth().semantics(mergeDescendants = true) {
            contentDescription = "$durabilityLabel $points%"
            progressBarRangeInfo = ProgressBarRangeInfo(points / 100f, 0f..1f)
        }.testTag("run-shoe-check-durability"),
    ) {
        Row(Modifier.fillMaxWidth().heightIn(min = 36.dp), verticalAlignment = Alignment.CenterVertically) {
            Text(stringResource(R.string.sdv_stat_durability), color = c.label, fontSize = 16.sp, fontWeight = FontWeight.SemiBold,
                modifier = Modifier.weight(1f))
            Text(com.stepup.android.ui.screens.items.durabilityText(shoe, rules), color = if (urgent) c.live else c.label, fontSize = 16.sp,
                fontWeight = FontWeight.Bold, modifier = Modifier.testTag("run-shoe-check-durability-value"))
            if (onRepair != null && com.stepup.android.ui.screens.items.repairable(shoe)) {
                Spacer(Modifier.width(8.dp))
                androidx.compose.foundation.layout.Box(Modifier.testTag("run-shoe-check-repair")) {
                    com.stepup.android.ui.screens.items.V12RepairChip(urgent, true, onRepair)
                }
            }
        }
        Spacer(Modifier.height(8.dp))
        androidx.compose.foundation.layout.Box(Modifier.fillMaxWidth().height(6.dp).clip(RoundedCornerShape(3.dp)).background(c.fill)) {
            if (points > 0) androidx.compose.foundation.layout.Box(Modifier.fillMaxWidth(points / 100f).height(6.dp).clip(RoundedCornerShape(3.dp))
                .background(if (urgent) Color(0xFFFF7A1A) else c.secondaryLabel))
        }
    }
    Text(
        shoeCheckLineText(line, rules), color = if (urgent) c.live else c.secondaryLabel, fontSize = 14.sp,
        modifier = Modifier.padding(top = 10.dp).testTag(if (urgent) "run-shoe-check-low-durability" else "run-shoe-check-earn"),
    )
}

/** 이 앱이 배터리 최적화(절전) 대상인가 — 그러면 폰에 따라(삼성 · 샤오미 등) 화면이 꺼진 뒤 위치 · 걸음이 늦게 오거나 끊긴다 */
internal fun batteryRestricted(context: android.content.Context): Boolean =
    runCatching {
        val pm = context.getSystemService(android.content.Context.POWER_SERVICE) as android.os.PowerManager
        pm.isIgnoringBatteryOptimizations(context.packageName).not()
    }.getOrDefault(false)

/**
 * 배터리 안내(2026-11-26 테스터 "화면을 띄운 상태에서 껐더니 측정이 안 됐다 · 백그라운드 설정 문제 같다") — 절전 대상이면
 * "화면을 꺼도 끊기지 않게 배터리 사용을 「제한 없음」으로" 한 줄과 "설정 열기"(앱 정보 › 배터리). 앱으로 돌아오면 다시 읽어,
 * 바꿨으면 줄이 사라진다. 출발을 막지 않는다. 예외 요청 권한(REQUEST_IGNORE_BATTERY_OPTIMIZATIONS)은 쓰지 않는다(Play 정책).
 */
/**
 * 배터리 안내(13차 — 처음 한 번, 출발 전 신발 확인 앞에서). 이 앱이 절전 대상일 때만 뜬다: "화면을 꺼도 기록이 끊기지 않게 배터리 사용을
 * 「제한 없음」으로" + "설정 열기"(앱 정보) · "다음에". 어느 쪽이든 다시 띄우지 않는다. 출발을 막지 않는다 — 닫으면 [onDone] 으로 이어진다.
 * 예외 요청 권한(REQUEST_IGNORE_BATTERY_OPTIMIZATIONS)은 쓰지 않는다(Play 정책).
 */
@Composable
internal fun BatteryOnceSheet(onDone: () -> Unit) {
    val context = LocalContext.current
    val c = com.stepup.android.ui.components.StepUpUi.colors
    com.stepup.android.ui.components.RunSheet(onDismiss = onDone, modifier = Modifier.testTag("run-battery-sheet")) {
        Text(stringResource(R.string.v13_battery_title), color = c.label, fontSize = 22.sp, fontWeight = FontWeight.ExtraBold,
            modifier = Modifier.padding(end = 40.dp).semantics { heading() })
        Spacer(Modifier.height(8.dp))
        Text(stringResource(R.string.run_battery_hint), color = c.secondaryLabel, fontSize = 15.sp)
        Spacer(Modifier.height(20.dp))
        com.stepup.android.ui.components.PrimaryButton(
            stringResource(R.string.run_battery_open), {
                com.stepup.android.core.ExternalIntents.openAppSettings(context)
                onDone()
            }, arrow = false, modifier = Modifier.fillMaxWidth().testTag("run-battery-open"),
        )
        Text(
            stringResource(R.string.v13_battery_later), color = c.secondaryLabel, fontSize = 15.sp, fontWeight = FontWeight.SemiBold,
            modifier = Modifier.align(Alignment.CenterHorizontally).padding(top = 6.dp).clip(RoundedCornerShape(10.dp))
                .feedbackClickable(onClick = onDone).padding(horizontal = 16.dp, vertical = 12.dp).testTag("run-battery-later"),
        )
    }
}

/** 오늘 적립 한도를 다 썼다 — 이 러닝은 적립이 없다(기록은 남는다). 경고 줄과 같은 모양, 버튼은 더하지 않는다 */
@Composable
private fun CheckEnergyUsedUp() {
    Row(Modifier.fillMaxWidth().padding(top = 14.dp).testTag("run-shoe-check-energy-empty"), verticalAlignment = Alignment.Top) {
        Icon(Icons.Outlined.Warning, contentDescription = null, tint = CheckWarnIcon, modifier = Modifier.padding(top = 2.dp).size(15.dp))
        Spacer(Modifier.width(7.dp))
        Text(stringResource(R.string.run_check_energy_empty), style = checkText(12.sp, CheckWarnText))
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
/**
 * 출발 전 아이템 고르기(2026-11-17 사용자 "출발 전에 고르기 · 같은 종류는 겹쳐서 못 쓴다", 서버 0106).
 * 작은 칸 셋까지 — 그림 · 이름 · 보유 개수, 고른 칸은 파란 테두리 + 체크. 아래 한 줄에 고른 것의 효과를 모아 보인다.
 */
@Composable
private fun RunItemPicker(
    items: List<com.stepup.android.domain.BagItem>,
    picked: Set<com.stepup.android.domain.ItemKind>,
    onPick: (com.stepup.android.domain.ItemKind) -> Unit,
    enabled: Boolean,
    coveredRewardMult: Double = 1.0,
) {
    // 이미 켜진 배수(보상 이벤트 · 24시간 부스터)와 같거나 작은 배수 아이템 — 서버는 큰 것 하나만 쓰므로 고르면 아이템만 사라진다
    fun covered(item: com.stepup.android.domain.BagItem): Boolean =
        item.kind.group == "REWARD" && coveredRewardMult > 1.0 && (item.value ?: 1.0) <= coveredRewardMult
    Column(Modifier.padding(top = 18.dp).fillMaxWidth().testTag("run-items")) {
        Row(verticalAlignment = Alignment.Bottom) {
            Text(
                stringResource(R.string.run_items_title), style = checkText(15.sp, Color.White, FontWeight.Bold),
                modifier = Modifier.weight(1f).semantics { heading() },
            )
            Text(stringResource(R.string.run_items_hint), style = checkText(12.sp, CheckBack, FontWeight.Normal))
        }
        Row(Modifier.padding(top = 10.dp).fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            items.take(3).forEach { item ->
                val blocked = covered(item)
                val on = item.kind in picked && !blocked
                val shape = RoundedCornerShape(16.dp)
                Column(
                    Modifier.weight(1f).clip(shape)
                        .alpha(if (blocked) 0.42f else 1f)
                        .background(if (on) Color(0xFF1A2A52) else Color(0xFF0D1426), shape)
                        .border(if (on) 2.dp else 1.dp, if (on) Color(0xFF6C9BFF) else CheckEdge, shape)
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
                        style = checkText(11.5.sp, Color.White, FontWeight.SemiBold), maxLines = 2,
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                    )
                    Text(
                        stringResource(R.string.run_items_owned, item.qty),
                        style = checkText(11.sp, if (on) Color(0xFFADC8FF) else CheckBack, FontWeight.Normal),
                    )
                }
            }
        }
        if (items.take(3).any(::covered)) {
            Text(
                stringResource(R.string.run_items_covered, com.stepup.android.ui.screens.items.formatValue(coveredRewardMult)),
                style = checkText(12.sp, CheckBack, FontWeight.Normal),
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
                style = checkText(12.sp, CheckReadyText, FontWeight.Normal), modifier = Modifier.padding(top = 8.dp),
            )
        }
    }
}

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
 * - 러닝 기록은 이 화면이 뜨는 순간(시작을 누른 순간) 한 번만 [onStartSession] 으로 시작한다(2026-11-26 — 예전엔 3·2·1 뒤 펼침에서).
 *   연출은 StepUP! 부터([FreeRunStartMotion.QUICK_START_AT]) 보이고, 러닝이 실제로 켜진 것을 본 뒤에 지도를 펼친다.
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
    var clock by rememberSaveable { mutableLongStateOf(FreeRunStartMotion.QUICK_START_AT) }
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
        // 누른 순간 기록을 켠다 — 이 화면은 "이 신발로 출발"을 누른 바로 뒤라 앱이 앞에 있다(러닝 서비스를 켤 수 있다).
        // 그 뒤로 화면을 끄거나 다른 앱으로 넘어가도 기록은 이어진다. 연출은 앱이 보일 때만 나아간다
        if (requestedAt < 0) {
            requestedAt = clock
            refusalsAtRequest = refusals
            if (!start()) {
                failed()
                return@LaunchedEffect
            }
        }
        var last = withFrameNanos { it }
        while (true) {
            val now = withFrameNanos { it }
            val step = ((now - last) / 1_000_000L).coerceIn(0L, FreeRunStartMotion.MAX_FRAME_MS)
            last = now
            if (!lifecycle.currentState.isAtLeast(Lifecycle.State.RESUMED)) continue
            clock += step
            when {
                revealAt < 0 -> when {
                    active && clock >= FreeRunStartMotion.REVEAL_AT -> revealAt = clock
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
        // 숫자 · 원호 묶음 — 원호는 3 부터 로고 교차까지 한 번 이어 돈다(숫자마다 다시 돌지 않는다). 다 빠진 뒤로는 그리지 않는다 —
        // 누르자마자 시작(2026-11-26)은 StepUP! 부터 보여, 투명한 숫자를 화면 읽기가 "0"으로 읽지 않게
        val countGone by remember { derivedStateOf { clock() >= FreeRunStartMotion.LAUNCH_AT + FreeRunStartMotion.LAUNCH_MS } }
        if (!countGone) Box(
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

/** 한국어 "로 / 으로" — 받침이 없거나 ㄹ 받침이면 "로". 한국어 화면에서만 붙인다(다른 언어는 문자열이 문장을 짠다) */
@Composable
private fun withRo(word: String): String {
    val korean = androidx.compose.ui.platform.LocalConfiguration.current.locales[0]?.language == "ko"
    if (!korean) return word
    val last = word.trim().lastOrNull() ?: return word
    if (last !in '가'..'힣') return word + "(으)로"
    val jong = (last - '가') % 28
    return word + if (jong == 0 || jong == 8) "로" else "으로"
}
