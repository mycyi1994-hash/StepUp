package com.stepup.android.ui.screens.items

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.stepup.android.R
import com.stepup.android.core.ServiceLocator
import com.stepup.android.data.remote.ServerResult
import com.stepup.android.domain.BagItem
import com.stepup.android.domain.ItemKind
import com.stepup.android.ui.components.ButtonState
import com.stepup.android.ui.components.EmptyLine
import com.stepup.android.ui.components.ErrorNote
import com.stepup.android.ui.components.LargeTitle
import com.stepup.android.ui.components.PrimaryButton
import com.stepup.android.ui.components.SecondaryButton
import com.stepup.android.ui.components.StepUpSpinner
import com.stepup.android.ui.components.StepUpUi
import com.stepup.android.ui.experience.feedbackClickable
import kotlinx.coroutines.launch
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle
import java.util.UUID
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.Spacer
import com.stepup.android.domain.Bag

/**
 * 아이템 탭(2026-11-17, 서버 0106 · 13차 S-355 · S-356). 제목 "아이템"과 오른쪽 "N종 · N개", 보상 이벤트 띠(켜져 있을 때),
 * 러닝할 때 쓰는 아이템 큰 그림 카드 셋, 바로 쓰는 아이템 큰 그림 카드 둘. 카드를 누르면 아래 시트(S-357 · S-358 · S-411) —
 * 러닝 아이템은 보유 · 쓰는 곳 · 한 러닝에 하나와 "러닝 시작"(쓰는 것은 출발 전 신발 확인에서 고른다), 바로 쓰는 것은
 * 쓰면 어떻게 되는지와 "내구도 채우기 · 1개" 같은 주 버튼 하나. 개수 · 효과 값은 서버 값 그대로다(못 읽으면 다시 시도).
 */
@Composable
fun BagScreen(onStartRun: () -> Unit = {}, onOpenShoes: () -> Unit = {}) {
    val items = ServiceLocator.itemsRepository
    val bag by items.bag.collectAsStateWithLifecycle()
    val scope = rememberCoroutineScope()
    var loading by remember { mutableStateOf(bag == null) }
    var failed by remember { mutableStateOf(false) }
    var selected by remember { mutableStateOf<ItemKind?>(null) }
    var using by remember { mutableStateOf(false) }
    var message by remember { mutableStateOf<String?>(null) }
    // 한 번 누른 사용 — 응답을 잃고 다시 눌러도 같은 번호면 서버가 한 번만 쓴다. 다른 아이템을 고르면 새 번호
    var request by remember { mutableStateOf<String?>(null) }
    // 13차 S-440 · S-441 — 쓴 뒤 아래 알림(쓴 것 · 남은 수 / 실패 · 다시 시도). 다시 시도는 같은 요청 번호로
    var toast by remember { mutableStateOf<BagToast?>(null) }
    LaunchedEffect(toast) { if (toast != null) { kotlinx.coroutines.delay(if (toast?.retry != null) 6_000 else 3_000); toast = null } }

    suspend fun load() {
        loading = bag == null
        failed = false
        val result = items.refresh()
        loading = false
        failed = result !is ServerResult.Ok && bag == null
    }
    LaunchedEffect(Unit) { load() }

    val shoe by ServiceLocator.sneakerRepository.equipped.collectAsStateWithLifecycle(initialValue = null)
    // 서버가 준 하루 에너지를 아직 받지 못했으면 null — 폰 기본값(10)으로 "남은 에너지 N칸"을 보이지 않는다(2026-11-22 경제 점검)
    val energy by ServiceLocator.stepRepository.energyKnown.collectAsStateWithLifecycle(initialValue = null)

    val errFull = stringResource(R.string.bag_err_full)
    val errEnergy = stringResource(R.string.bag_err_no_energy)
    val errShoe = stringResource(R.string.bag_err_no_shoe)
    val errNone = stringResource(R.string.bag_err_none)
    val errGeneric = stringResource(R.string.bag_err_generic)
    val usedToast = stringResource(R.string.v13_bag_used_toast)
    val failedToast = stringResource(R.string.v13_bag_use_failed)

    Box(Modifier.fillMaxSize().testTag("bag-screen")) {
        val current = bag
        BagList(
            current = current, loading = loading, failed = failed, selected = selected,
            bottomRoom = 28.dp,
            onSelect = { kind -> selected = kind; request = null; message = null },
            onRetry = { scope.launch { load() } },
            hasShoe = shoe != null, onOpenShoes = onOpenShoes,
        )
        toast?.let { t ->
            BagToastBar(t, Modifier.align(Alignment.BottomCenter).padding(horizontal = StepUpUi.Gutter).padding(bottom = 14.dp)) {
                toast = null
                t.retry?.invoke()
            }
        }
    }

    val pick = selected
    val current = bag
    if (current != null && pick != null) {
        val qty = current.qty(pick)
        val itemNameText = itemName(pick)
        val close = { if (!using) { selected = null; message = null } }
        com.stepup.android.ui.components.RunSheet(onDismiss = close, modifier = Modifier.testTag("bag-sheet"), dismissible = !using) {
            BagSheetHead(pick, current.value(pick))
            Spacer(Modifier.height(14.dp))
            if (pick.runPick) {
                BagSheetLine(stringResource(R.string.v13_bag_have), stringResource(R.string.v13_bag_n, qty), "bag-sheet-have")
                BagSheetLine(stringResource(R.string.v13_bag_where), stringResource(R.string.v13_bag_where_run))
                BagSheetLine(stringResource(R.string.v13_bag_per_run), stringResource(R.string.v13_bag_per_run_value))
                Spacer(Modifier.height(16.dp))
                PrimaryButton(
                    stringResource(R.string.v13_bag_start_run),
                    onClick = { selected = null; onStartRun() },
                    modifier = Modifier.fillMaxWidth().testTag("bag-start-run"),
                )
            } else {
                val worn = shoe
                val left = energy
                // 쓰면 어떻게 되는지 — 신은 신발 내구도(서버 값) · 남은 에너지(서버 값). 모르면 효과 글만
                val change = when {
                    pick == ItemKind.DURABILITY_RESET && worn != null ->
                        stringResource(R.string.bag_use_durability_detail, (worn.server?.durabilityPts ?: worn.durability.toDouble()).toInt().coerceIn(0, 100))
                    pick == ItemKind.ENERGY_RESET && left != null ->
                        stringResource(R.string.bag_use_energy_detail, formatValue(Math.round(left * 10) / 10.0))
                    else -> effectText(pick, current.value(pick))
                }
                BagSheetLine(stringResource(R.string.v13_bag_have), stringResource(R.string.v13_bag_n, qty), "bag-sheet-have")
                Text(
                    change, style = StepUpUi.type.rowSubtitle, color = StepUpUi.colors.secondaryLabel,
                    modifier = Modifier.fillMaxWidth().padding(top = 6.dp).testTag("bag-sheet-change"),
                )
                if (qty > 0) {
                    Text(
                        stringResource(R.string.v13_bag_after, itemName(pick), qty - 1), style = StepUpUi.type.caption,
                        color = StepUpUi.colors.tertiaryLabel, modifier = Modifier.fillMaxWidth().padding(top = 4.dp),
                    )
                }
                message?.let {
                    Text(it, style = StepUpUi.type.rowSubtitle, color = StepUpUi.colors.label,
                        modifier = Modifier.fillMaxWidth().padding(top = 10.dp).testTag("bag-sheet-message"))
                }
                Spacer(Modifier.height(16.dp))
                PrimaryButton(
                    stringResource(
                        if (pick == ItemKind.DURABILITY_RESET) R.string.v13_bag_fill_durability else R.string.v13_bag_fill_energy,
                    ) + " · " + stringResource(R.string.v13_bag_n, 1),
                    onClick = {
                        if (using) return@PrimaryButton
                        using = true
                        val id = request ?: UUID.randomUUID().toString().also { request = it }
                        val kind = pick
                        val name = itemNameText
                        fun send(key: String) {
                            scope.launch {
                                val result = items.use(kind, key, null)
                                using = false
                                when (result) {
                                    is ServerResult.Ok -> {
                                        request = null
                                        selected = null
                                        message = null
                                        toast = BagToast(usedToast.format(name, result.value))
                                    }
                                    is ServerResult.Rejected -> {
                                        request = null
                                        message = when {
                                            "가득" in result.reason -> errFull
                                            "에너지" in result.reason -> errEnergy
                                            "신발" in result.reason -> errShoe
                                            "없습니다" in result.reason -> errNone
                                            else -> errGeneric
                                        }
                                    }
                                    // 끊겼다 — 같은 번호로 다시 보내면 서버가 한 번만 쓴다
                                    else -> {
                                        selected = null
                                        toast = BagToast(failedToast, retry = { using = true; send(key) })
                                    }
                                }
                            }
                        }
                        send(id)
                    },
                    state = when {
                        using -> ButtonState.Loading
                        qty <= 0 -> ButtonState.Disabled
                        else -> ButtonState.Idle
                    },
                    arrow = false,
                    modifier = Modifier.fillMaxWidth().testTag("bag-use"),
                )
            }
        }
    }
}

/** 시트 머리 — 그림(남색 칸) · 이름 · 효과 */
@Composable
private fun BagSheetHead(kind: ItemKind, value: Double?) {
    Row(Modifier.fillMaxWidth().padding(end = 40.dp), verticalAlignment = Alignment.CenterVertically) {
        Box(
            Modifier.size(72.dp).clip(RoundedCornerShape(16.dp)).background(Brush.verticalGradient(listOf(Color(0xFF1B2C5C), Color(0xFF101A3A)))),
            contentAlignment = Alignment.Center,
        ) { Image(painterResource(itemArt(kind)), null, Modifier.fillMaxSize(.86f)) }
        Spacer(Modifier.width(14.dp))
        Column {
            Text(itemName(kind), style = StepUpUi.type.largeTitle.copy(fontSize = 22.sp), color = StepUpUi.colors.label,
                modifier = Modifier.semantics { heading() })
            Text(effectText(kind, value), style = StepUpUi.type.rowSubtitle, color = StepUpUi.colors.secondaryLabel)
        }
    }
}

@Composable
private fun BagSheetLine(label: String, value: String, tag: String? = null) {
    Row(
        Modifier.fillMaxWidth().heightIn(min = 48.dp).semantics(mergeDescendants = true) {}
            .then(if (tag != null) Modifier.testTag(tag) else Modifier),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(label, style = StepUpUi.type.rowSubtitle, color = StepUpUi.colors.secondaryLabel, modifier = Modifier.weight(1f))
        Text(value, style = StepUpUi.type.rowTitle, color = StepUpUi.colors.label)
    }
}

/**
 * 아이템 탭 본문(13차 S-355 · S-356) — 제목 · "N종 · N개", 러닝할 때 쓰는 아이템(큰 그림 카드 셋), 바로 쓰는 아이템(큰 그림 카드 둘 · 쓰기).
 * 모두 없으면 제목 아래 "아직 없음 · 이벤트 보상으로 받아요". 개수가 0 인 카드는 흐린 그림 · "없음".
 */
@Composable
internal fun BagList(
    current: Bag?,
    loading: Boolean,
    failed: Boolean,
    selected: ItemKind?,
    bottomRoom: androidx.compose.ui.unit.Dp,
    onSelect: (ItemKind) -> Unit,
    onRetry: () -> Unit,
    /** 신은 신발이 있는가 — 없으면 내구도 초기화권 칸이 "신은 신발 없음 · 신발 고르기"(S-439) */
    hasShoe: Boolean = true,
    onOpenShoes: () -> Unit = {},
) {
    val c = StepUpUi.colors
    LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(bottom = bottomRoom)) {
        item {
            Row(
                Modifier.fillMaxWidth().padding(horizontal = StepUpUi.TitleGutter).padding(top = 10.dp).testTag("bag-head"),
                verticalAlignment = Alignment.Bottom,
            ) {
                Text(stringResource(R.string.tab_items), style = StepUpUi.type.largeTitle.copy(fontSize = 30.sp), color = c.label,
                    modifier = Modifier.weight(1f).semantics { heading() })
                if (current != null && current.total > 0) {
                    Text(
                        stringResource(R.string.v13_bag_count, current.items.count { it.qty > 0 }, current.total),
                        style = StepUpUi.type.caption, color = c.secondaryLabel, modifier = Modifier.padding(bottom = 6.dp).testTag("bag-count"),
                    )
                }
            }
            current?.event?.let { event -> EventLine(event.mult, event.endsAtMillis) }
            if (current != null && current.total == 0) {
                Text(stringResource(R.string.v13_bag_empty), style = StepUpUi.type.rowSubtitle, color = c.secondaryLabel,
                    modifier = Modifier.padding(horizontal = StepUpUi.TitleGutter).padding(top = 4.dp).testTag("bag-empty"))
            }
            if (current == null && failed && !loading) {
                // S-438 — 못 읽었다: 제목 아래 한 줄 + 다시 시도, 칸은 수 없이("–")
                Row(Modifier.fillMaxWidth().padding(horizontal = StepUpUi.TitleGutter).padding(top = 4.dp), verticalAlignment = Alignment.CenterVertically) {
                    Text(stringResource(R.string.v13_bag_load_failed), style = StepUpUi.type.rowSubtitle, color = c.label,
                        modifier = Modifier.weight(1f).testTag("bag-load-failed"))
                    Text(stringResource(R.string.bag_retry), color = c.accentText, fontSize = 14.sp, fontWeight = FontWeight.Bold,
                        modifier = Modifier.clip(RoundedCornerShape(50)).background(c.fill).feedbackClickable(role = Role.Button, onClick = onRetry)
                            .padding(horizontal = 14.dp, vertical = 7.dp).testTag("bag-retry"))
                }
            }
        }
        when {
            current == null && loading -> item {
                Box(Modifier.fillMaxWidth().padding(top = 80.dp), contentAlignment = Alignment.Center) { StepUpSpinner() }
            }
            current != null || failed -> {
                // 못 읽었으면 칸만(수 "–", 누르지 못함)
                fun itemOf(kind: ItemKind): BagItem = current?.items?.firstOrNull { it.kind == kind } ?: BagItem(kind, null, if (current == null) -1 else 0)
                item {
                    SectionHead(stringResource(R.string.v13_bag_section_run), stringResource(R.string.bag_section_run_hint))
                    Row(Modifier.fillMaxWidth().padding(horizontal = StepUpUi.Gutter), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        ItemKind.entries.filter { it.runPick }.forEach { kind ->
                            val item = itemOf(kind)
                            V13ItemCard(item, selected == kind, wide = false, Modifier.weight(1f)) { onSelect(kind) }
                        }
                    }
                }
                item {
                    SectionHead(stringResource(R.string.bag_section_now), null)
                    Row(Modifier.fillMaxWidth().padding(horizontal = StepUpUi.Gutter), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        ItemKind.entries.filter { !it.runPick }.forEach { kind ->
                            val item = itemOf(kind)
                            val noShoe = kind == ItemKind.DURABILITY_RESET && !hasShoe && item.qty > 0
                            V13ItemCard(item, selected == kind, wide = true, Modifier.weight(1f), noShoe = noShoe, onOpenShoes = onOpenShoes) { onSelect(kind) }
                        }
                    }
                }
            }
        }
    }
}

/**
 * 13차 아이템 카드 — 남색 면에 큰 그림, 오른쪽 위 ×개수(없으면 "없음"), 아래 이름 · 효과. 바로 쓰는 것([wide])은 오른쪽 아래 "쓰기".
 * 그림이 어두운 바탕에 맞춰 그려져 카드는 두 테마 모두 남색이다.
 */
@Composable
private fun V13ItemCard(
    item: BagItem,
    selected: Boolean,
    wide: Boolean,
    modifier: Modifier,
    noShoe: Boolean = false,
    onOpenShoes: () -> Unit = {},
    onClick: () -> Unit,
) {
    val shape = RoundedCornerShape(18.dp)
    val has = item.qty > 0
    val unknown = item.qty < 0
    Box(
        modifier.aspectRatio(if (wide) .98f else .7f).clip(shape)
            .background(Brush.verticalGradient(listOf(Color(0xFF22346A), Color(0xFF131D42))))
            .border(if (selected) 2.dp else 1.dp, if (selected) Color(0xFF6C9BFF) else Color(0x33FFFFFF), shape)
            .feedbackClickable(enabled = !unknown, role = Role.Button, onClick = onClick)
            .semantics { this.selected = selected }.testTag("bag-item-${item.kind.id}"),
    ) {
        Text(
            when {
                unknown -> "–"
                has -> stringResource(R.string.item_qty, item.qty)
                else -> stringResource(R.string.v12_bag_zero)
            },
            color = Color.White.copy(alpha = if (has) 1f else .6f), fontSize = 12.sp, fontWeight = FontWeight.Bold,
            modifier = Modifier.align(Alignment.TopEnd).padding(8.dp),
        )
        Column(Modifier.fillMaxSize().padding(10.dp)) {
            Box(Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
                Image(
                    painterResource(itemArt(item.kind)), null,
                    Modifier.fillMaxWidth(if (wide) .7f else .92f).aspectRatio(1f).alpha(if (has) 1f else .35f),
                )
            }
            Row(verticalAlignment = Alignment.Bottom) {
                Column(Modifier.weight(1f)) {
                    Text(
                        itemName(item.kind), color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.ExtraBold, maxLines = 2,
                        lineHeight = 17.sp,
                    )
                    Text(
                        if (noShoe) stringResource(R.string.v13_bag_no_shoe) else effectText(item.kind, item.value),
                        color = Color(0xFFB8C4E0), fontSize = 11.sp, maxLines = 1,
                    )
                }
                if (noShoe) {
                    Text(
                        stringResource(R.string.v13_bag_pick_shoe), color = Color(0xFF13204A), fontSize = 12.sp, fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        modifier = Modifier.clip(RoundedCornerShape(14.dp)).background(Color.White)
                            .feedbackClickable(role = Role.Button, onClick = onOpenShoes)
                            .padding(horizontal = 10.dp, vertical = 5.dp).testTag("bag-pick-shoe"),
                    )
                } else if (wide && has) {
                    Text(
                        stringResource(R.string.v12_bag_use), color = Color(0xFF13204A), fontSize = 13.sp, fontWeight = FontWeight.Bold,
                        modifier = Modifier.clip(RoundedCornerShape(14.dp)).background(Color.White).padding(horizontal = 12.dp, vertical = 5.dp),
                    )
                }
            }
        }
    }
}

@Composable
private fun SectionHead(title: String, hint: String?) {
    Row(
        Modifier.fillMaxWidth().padding(start = StepUpUi.TitleGutter, end = StepUpUi.TitleGutter, top = 22.dp, bottom = 10.dp),
        verticalAlignment = Alignment.Bottom,
    ) {
        Text(title, style = StepUpUi.type.rowTitle, color = StepUpUi.colors.label, modifier = Modifier.weight(1f).semantics { heading() })
        if (hint != null) Text(hint, style = StepUpUi.type.caption, color = StepUpUi.colors.tertiaryLabel)
    }
}

/** 보상 이벤트 한 줄(13차 S-437) — "⚡ 11월 30일까지 러닝 보상 ×1.5". 운영자가 연 이벤트가 켜져 있을 때만(0106) */
@Composable
private fun EventLine(mult: Double, endsAt: Long) {
    val pattern = stringResource(R.string.v13_bag_event_date)
    val locale = androidx.compose.ui.platform.LocalConfiguration.current.locales[0]
    val until = remember(endsAt, pattern, locale) {
        DateTimeFormatter.ofPattern(pattern, locale).format(Instant.ofEpochMilli(endsAt).atZone(ZoneId.systemDefault()))
    }
    Row(
        Modifier.padding(horizontal = StepUpUi.TitleGutter).padding(top = 4.dp).testTag("bag-event"),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(androidx.compose.material.icons.Icons.Filled.Bolt, null, tint = StepUpUi.colors.accentText, modifier = Modifier.size(16.dp))
        Text(stringResource(R.string.v13_bag_event, until, formatValue(mult)), color = StepUpUi.colors.accentText,
            fontSize = 14.sp, fontWeight = FontWeight.SemiBold, modifier = Modifier.padding(start = 4.dp))
    }
}

/** 아래 알림 한 줄 — [retry] 가 있으면 오른쪽 "다시 시도" */
internal data class BagToast(val text: String, val retry: (() -> Unit)? = null)

@Composable
private fun BagToastBar(toast: BagToast, modifier: Modifier, onAction: () -> Unit) {
    val c = StepUpUi.colors
    Row(
        modifier.fillMaxWidth().heightIn(min = 52.dp).clip(RoundedCornerShape(14.dp)).background(c.elevated)
            .border(1.dp, c.separator, RoundedCornerShape(14.dp)).padding(horizontal = 16.dp, vertical = 8.dp)
            .semantics { liveRegion = androidx.compose.ui.semantics.LiveRegionMode.Polite }.testTag("bag-toast"),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(toast.text, color = c.label, fontSize = 15.sp, fontWeight = FontWeight.SemiBold, modifier = Modifier.weight(1f))
        if (toast.retry != null) {
            Text(stringResource(R.string.bag_retry), color = c.accentText, fontSize = 15.sp, fontWeight = FontWeight.Bold,
                modifier = Modifier.clip(RoundedCornerShape(8.dp)).feedbackClickable(role = Role.Button, onClick = onAction)
                    .padding(6.dp).testTag("bag-toast-retry"))
        }
    }
}

/**
 * 아이템 칸 — 그림 · 이름 · 효과 · 개수. 그림이 어두운 바탕에 맞춰 그려져 칸은 두 테마 모두 어두운 남색이다(시안 D 와 같다).
 * 개수가 0 이면 흐리게(고를 수는 있다 — 설명을 보려고).
 */
@Composable
internal fun ItemTile(item: BagItem, selected: Boolean, tall: Boolean, modifier: Modifier = Modifier, onClick: () -> Unit) {
    val shape = RoundedCornerShape(22.dp)
    val glow = itemGlow(item.kind)
    Box(
        modifier
            .heightIn(min = if (tall) 166.dp else 150.dp)
            .clip(shape)
            .background(Brush.verticalGradient(listOf(Color(0xFF141C32), Color(0xFF080C18))))
            .background(Brush.radialGradient(listOf(glow.copy(alpha = if (selected) .55f else .32f), Color.Transparent)))
            .border(if (selected) 2.dp else 1.dp, if (selected) Color(0xFF6C9BFF) else Color(0x33FFFFFF), shape)
            .feedbackClickable(role = Role.Button, onClick = onClick)
            .semantics { this.selected = selected }
            .testTag("bag-item-${item.kind.id}"),
    ) {
        Column(
            Modifier.fillMaxSize().padding(start = 6.dp, end = 6.dp, top = 6.dp, bottom = 10.dp).alpha(if (item.qty > 0) 1f else .5f),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            Image(
                painterResource(itemArt(item.kind)), contentDescription = null,
                modifier = Modifier.fillMaxWidth(if (tall) .82f else .9f).aspectRatio(1f),
            )
            Text(
                itemName(item.kind), color = Color.White, textAlign = TextAlign.Center, maxLines = 2,
                style = StepUpUi.type.rowTitle.copy(fontSize = if (tall) 15.sp else 13.5.sp, lineHeight = if (tall) 19.sp else 17.sp),
            )
            Text(
                effectText(item.kind, item.value), color = glow.copy(alpha = 1f).let { lighten(it) }, textAlign = TextAlign.Center,
                style = StepUpUi.type.caption.copy(fontWeight = FontWeight.Bold, fontSize = 12.sp), maxLines = 1,
                modifier = Modifier.padding(top = 2.dp),
            )
        }
        Text(
            stringResource(R.string.item_qty, item.qty),
            color = Color.White, style = StepUpUi.type.numberRow.copy(fontSize = 17.sp),
            modifier = Modifier.align(Alignment.TopEnd).padding(8.dp)
                .clip(RoundedCornerShape(12.dp)).background(Color(0x8C000000)).padding(horizontal = 8.dp, vertical = 3.dp),
        )
        if (selected) {
            Box(
                Modifier.align(Alignment.TopStart).padding(8.dp).size(24.dp).clip(CircleShape).background(Color.White),
                contentAlignment = Alignment.Center,
            ) { Icon(Icons.Filled.Check, contentDescription = null, tint = Color(0xFF1B5BF0), modifier = Modifier.size(15.dp)) }
        }
    }
}

internal fun itemArt(kind: ItemKind): Int = when (kind) {
    ItemKind.RUN_BOOSTER -> R.drawable.item_run_booster
    ItemKind.REWARD_DOUBLE -> R.drawable.item_reward_double
    ItemKind.ENERGY_DRINK -> R.drawable.item_energy_drink
    ItemKind.DURABILITY_RESET -> R.drawable.item_durability_reset
    ItemKind.ENERGY_RESET -> R.drawable.item_energy_reset
}

private fun itemGlow(kind: ItemKind): Color = when (kind) {
    ItemKind.RUN_BOOSTER -> Color(0xFFAAF03C)
    ItemKind.REWARD_DOUBLE -> Color(0xFFFFBE3C)
    ItemKind.ENERGY_DRINK -> Color(0xFF2882FF)
    ItemKind.DURABILITY_RESET -> Color(0xFFFF5050)
    ItemKind.ENERGY_RESET -> Color(0xFF5A78FF)
}

/** 효과 글자색 — 빛 색을 밝게(어두운 칸 위에서 읽히게) */
private fun lighten(c: Color): Color = Color(
    red = c.red + (1f - c.red) * .45f, green = c.green + (1f - c.green) * .45f, blue = c.blue + (1f - c.blue) * .45f,
)

@Composable
internal fun itemName(kind: ItemKind): String = stringResource(
    when (kind) {
        ItemKind.RUN_BOOSTER -> R.string.item_name_run_booster
        ItemKind.REWARD_DOUBLE -> R.string.item_name_reward_double
        ItemKind.ENERGY_DRINK -> R.string.item_name_energy_drink
        ItemKind.DURABILITY_RESET -> R.string.item_name_durability_reset
        ItemKind.ENERGY_RESET -> R.string.item_name_energy_reset
    },
)

/** 효과 한 줄 — 배수 · 비율은 서버 값(value)으로. 값이 없으면 기본 문구만 */
@Composable
internal fun effectText(kind: ItemKind, value: Double?): String = when (kind) {
    ItemKind.RUN_BOOSTER, ItemKind.REWARD_DOUBLE -> stringResource(R.string.item_effect_reward, formatValue(value ?: if (kind == ItemKind.REWARD_DOUBLE) 2.0 else 1.5))
    ItemKind.ENERGY_DRINK -> stringResource(R.string.item_effect_energy, ((value ?: .5) * 100).toInt())
    ItemKind.DURABILITY_RESET -> stringResource(R.string.item_effect_durability)
    ItemKind.ENERGY_RESET -> stringResource(R.string.item_effect_energy_reset)
}

/** 1.5 → "1.5", 2.0 → "2" */
internal fun formatValue(v: Double): String =
    if (v == Math.floor(v)) v.toLong().toString() else String.format(java.util.Locale.US, "%.1f", v).trimEnd('0').trimEnd('.')
