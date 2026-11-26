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
 * 아이템 탭(2026-11-17, 서버 0106 — 시안 "B 확정 · 아이템 탭"). 위에서부터 보상 이벤트 띠(켜져 있을 때) · 러닝할 때 고르는 아이템 ·
 * 바로 쓰는 아이템. 주 버튼은 하나 — 바로 쓰는 아이템을 골랐을 때 아래에 뜨는 "사용하기". 러닝 아이템은 여기서 쓰지 않고
 * 출발 전 점검에서 고른다(누르면 그 안내만). 개수 · 효과 값은 서버 값 그대로다(지어내지 않는다 — 못 읽으면 다시 시도).
 */
@Composable
fun BagScreen() {
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
    val usedFormat = stringResource(R.string.bag_used)

    Box(Modifier.fillMaxSize().testTag("bag-screen")) {
        val current = bag
        BagList(
            current = current, loading = loading, failed = failed, selected = selected,
            bottomRoom = if (selected != null) 120.dp else 28.dp,
            onSelect = { kind -> selected = kind; request = null; message = null },
            onRetry = { scope.launch { load() } },
        )

        // 고른 아이템 — 바로 쓰는 것은 "사용하기"(주 버튼 하나), 러닝 아이템은 출발 전 점검 안내만
        val pick = selected
        if (current != null && pick != null) {
            val qty = current.qty(pick)
            val worn = shoe
            val left = energy
            val note = message
            val detail = when {
                note != null -> note
                // 남은 비율(%) — 서버 신발이면 서버 값(durabilityPts), 아니면 폰 사본의 %
                pick == ItemKind.DURABILITY_RESET && worn != null ->
                    stringResource(R.string.bag_use_durability_detail, (worn.server?.durabilityPts ?: worn.durability.toDouble()).toInt().coerceIn(0, 100))
                pick == ItemKind.ENERGY_RESET && left != null ->
                    stringResource(R.string.bag_use_energy_detail, formatValue(Math.round(left * 10) / 10.0))
                pick.runPick -> stringResource(R.string.bag_run_item_note)
                else -> effectText(pick, current.value(pick))
            }
            Column(
                Modifier.align(Alignment.BottomCenter).fillMaxWidth().padding(horizontal = 12.dp, vertical = 12.dp)
                    .clip(RoundedCornerShape(26.dp)).background(StepUpUi.colors.elevated)
                    .border(1.dp, StepUpUi.colors.rim, RoundedCornerShape(26.dp))
                    .padding(start = 18.dp, end = 10.dp, top = 12.dp, bottom = 12.dp)
                    .testTag("bag-use-bar"),
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f).padding(end = 10.dp)) {
                        Text(itemName(pick), style = StepUpUi.type.rowTitle, color = StepUpUi.colors.label, maxLines = 1, overflow = TextOverflow.Ellipsis)
                        Text(detail, style = StepUpUi.type.rowSubtitle, color = StepUpUi.colors.secondaryLabel, modifier = Modifier.padding(top = 2.dp))
                    }
                    if (!pick.runPick) {
                        PrimaryButton(
                            stringResource(R.string.bag_use),
                            onClick = {
                                if (using) return@PrimaryButton
                                using = true
                                val id = request ?: UUID.randomUUID().toString().also { request = it }
                                scope.launch {
                                    val result = items.use(pick, id, null)
                                    using = false
                                    message = when (result) {
                                        is ServerResult.Ok -> { request = null; usedFormat.format(result.value) }
                                        is ServerResult.Rejected -> {
                                            request = null
                                            when {
                                                "가득" in result.reason -> errFull
                                                "에너지" in result.reason -> errEnergy
                                                "신발" in result.reason -> errShoe
                                                "없습니다" in result.reason -> errNone
                                                else -> errGeneric
                                            }
                                        }
                                        // 끊겼다 — 같은 번호를 두고 다시 누르면 서버가 한 번만 쓴다
                                        else -> errGeneric
                                    }
                                }
                            },
                            state = when {
                                using -> ButtonState.Loading
                                qty <= 0 -> ButtonState.Disabled
                                else -> ButtonState.Idle
                            },
                            arrow = false,
                            modifier = Modifier.testTag("bag-use"),
                        )
                    }
                }
            }
        }
    }
}

/**
 * 아이템 탭 본문(v12 디자인 시제품 2026-11-26) — 위 하늘색 칸(고른 러닝 아이템을 크게 · 출발 전 점검에서 골라 쓴다는 한 줄),
 * "러닝할 때 고르는 아이템" 흰 카드 셋(그림 · ×개수 · 아래 이름 · 효과), "바로 쓰는 아이템" 줄(그림 · 이름 ×개수 · 효과 · 쓰기).
 * 고르기 · 쓰기는 예전 그대로 — 러닝 아이템은 출발 전 점검에서 고르고, 바로 쓰는 것은 아래 "사용하기" 줄로 쓴다.
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
) {
    val c = StepUpUi.colors
    LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(bottom = bottomRoom)) {
        item {
            // 하늘색 칸 — 제목 · 고른(없으면 가진 첫) 러닝 아이템
            val heroKind = selected?.takeIf { it.runPick }
                ?: current?.runPicks?.firstOrNull()?.kind
            Box(
                Modifier.fillMaxWidth().background(if (c.dark) Color(0xFF16294A) else Color(0xFFDCECFB))
                    .padding(horizontal = StepUpUi.Gutter + 4.dp).padding(top = 10.dp, bottom = 22.dp).testTag("bag-hero"),
            ) {
                Column(Modifier.fillMaxWidth(.58f)) {
                    Text(stringResource(R.string.tab_items), style = StepUpUi.type.largeTitle.copy(fontSize = 30.sp), color = c.label,
                        modifier = Modifier.semantics { heading() })
                    Spacer(Modifier.height(18.dp))
                    Text(stringResource(R.string.v12_bag_next), color = c.secondaryLabel, fontSize = 14.sp)
                    Spacer(Modifier.height(4.dp))
                    if (heroKind != null) {
                        Text(itemName(heroKind), color = c.label, fontSize = 22.sp, fontWeight = FontWeight.ExtraBold, maxLines = 1)
                        Text(effectText(heroKind, current?.value(heroKind)), color = c.secondaryLabel, fontSize = 14.sp)
                    } else {
                        Text(stringResource(R.string.v12_bag_none), color = c.label, fontSize = 20.sp, fontWeight = FontWeight.ExtraBold)
                    }
                    Spacer(Modifier.height(6.dp))
                    Text(stringResource(R.string.v12_bag_pick_hint), color = c.accentText, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                }
                if (heroKind != null) {
                    Image(
                        painterResource(itemArt(heroKind)), null,
                        Modifier.align(Alignment.CenterEnd).fillMaxWidth(.4f).aspectRatio(1f),
                    )
                }
            }
        }
        current?.event?.let { event -> item { EventBand(event.mult, event.endsAtMillis) } }
        when {
            current == null && loading -> item {
                Box(Modifier.fillMaxWidth().padding(top = 80.dp), contentAlignment = Alignment.Center) { StepUpSpinner() }
            }
            current == null && failed -> item {
                Column(Modifier.padding(horizontal = StepUpUi.Gutter, vertical = 40.dp)) {
                    ErrorNote(stringResource(R.string.bag_load_failed), null)
                    SecondaryButton(stringResource(R.string.bag_retry), onRetry, Modifier.fillMaxWidth().padding(top = 14.dp).testTag("bag-retry"))
                }
            }
            current != null -> {
                if (current.total == 0) {
                    item { EmptyLine(stringResource(R.string.bag_empty), Modifier.padding(horizontal = StepUpUi.Gutter, vertical = 12.dp)) }
                }
                item {
                    SectionHead(stringResource(R.string.bag_section_run), null)
                    Row(Modifier.fillMaxWidth().padding(horizontal = StepUpUi.Gutter), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        ItemKind.entries.filter { it.runPick }.forEach { kind ->
                            val item = current.items.firstOrNull { it.kind == kind } ?: BagItem(kind, null, 0)
                            Column(Modifier.weight(1f)) {
                                V12ItemCard(item, selected == kind) { onSelect(kind) }
                                Spacer(Modifier.height(8.dp))
                                Text(itemName(kind), color = c.label, fontSize = 14.sp, fontWeight = FontWeight.Bold, maxLines = 1)
                                Text(effectText(kind, item.value), color = c.secondaryLabel, fontSize = 12.sp, maxLines = 1)
                            }
                        }
                    }
                }
                item {
                    SectionHead(stringResource(R.string.bag_section_now), null)
                    Column(Modifier.padding(horizontal = StepUpUi.Gutter)) {
                        ItemKind.entries.filter { !it.runPick }.forEach { kind ->
                            val item = current.items.firstOrNull { it.kind == kind } ?: BagItem(kind, null, 0)
                            V12InstantRow(item, selected == kind) { onSelect(kind) }
                        }
                    }
                }
            }
        }
    }
}

/** 러닝 아이템 흰 카드 — 그림 · 오른쪽 아래 ×개수. 고르면 파란 테두리, 없으면 흐리게 */
@Composable
private fun V12ItemCard(item: BagItem, selected: Boolean, onClick: () -> Unit) {
    val c = StepUpUi.colors
    val shape = RoundedCornerShape(16.dp)
    Box(
        Modifier.fillMaxWidth().aspectRatio(.9f).clip(shape).background(if (c.dark) Color(0xFF172236) else Color(0xFFEFF2F7))
            .border(if (selected) 2.dp else 0.dp, if (selected) c.primary else Color.Transparent, shape)
            .feedbackClickable(role = Role.Button, onClick = onClick)
            .semantics { this.selected = selected }.testTag("bag-item-${item.kind.id}"),
        contentAlignment = Alignment.Center,
    ) {
        Image(painterResource(itemArt(item.kind)), null, Modifier.fillMaxWidth(.82f).aspectRatio(1f).alpha(if (item.qty > 0) 1f else .4f))
        Text(
            if (item.qty > 0) stringResource(R.string.item_qty, item.qty) else stringResource(R.string.v12_bag_zero),
            color = c.label, fontSize = 12.sp, fontWeight = FontWeight.Bold,
            modifier = Modifier.align(Alignment.BottomEnd).padding(8.dp),
        )
    }
}

/** 바로 쓰는 아이템 한 줄 — 그림 · 이름 ×개수 · 효과 · 오른쪽 "쓰기"(없으면 "없음") */
@Composable
private fun V12InstantRow(item: BagItem, selected: Boolean, onClick: () -> Unit) {
    val c = StepUpUi.colors
    Row(
        Modifier.fillMaxWidth().heightIn(min = 72.dp).clip(RoundedCornerShape(14.dp))
            .background(if (selected) c.primary.copy(alpha = .08f) else Color.Transparent)
            .feedbackClickable(role = Role.Button, onClick = onClick).padding(vertical = 8.dp, horizontal = 4.dp)
            .testTag("bag-item-${item.kind.id}"),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Image(painterResource(itemArt(item.kind)), null, Modifier.size(54.dp).alpha(if (item.qty > 0) 1f else .4f))
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(itemName(item.kind), color = c.label, fontSize = 16.sp, fontWeight = FontWeight.Bold, maxLines = 1)
                if (item.qty > 0) {
                    Spacer(Modifier.width(6.dp))
                    Text(stringResource(R.string.item_qty, item.qty), color = c.secondaryLabel, fontSize = 14.sp)
                }
            }
            Text(effectText(item.kind, item.value), color = c.secondaryLabel, fontSize = 13.sp, maxLines = 1)
        }
        if (item.qty > 0) {
            Text(
                stringResource(R.string.v12_bag_use), color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.Bold,
                modifier = Modifier.clip(RoundedCornerShape(16.dp)).background(c.primary).padding(horizontal = 16.dp, vertical = 7.dp),
            )
        } else {
            Text(stringResource(R.string.v12_bag_zero), color = c.tertiaryLabel, fontSize = 14.sp)
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

/** 보상 이벤트 띠 — 운영자가 연 이벤트가 켜져 있을 때만(0106). 누르는 곳은 없다 */
@Composable
private fun EventBand(mult: Double, endsAt: Long) {
    val until = remember(endsAt) {
        DateTimeFormatter.ofLocalizedDateTime(FormatStyle.SHORT).format(Instant.ofEpochMilli(endsAt).atZone(ZoneId.systemDefault()))
    }
    Row(
        Modifier.padding(start = StepUpUi.Gutter, end = StepUpUi.Gutter, top = 14.dp).fillMaxWidth().heightIn(min = 56.dp)
            .clip(RoundedCornerShape(18.dp))
            .background(Brush.horizontalGradient(listOf(Color(0xFF2A62F2), Color(0xFF4D83FF), Color(0xFF7FA8FF))))
            .padding(horizontal = 16.dp, vertical = 8.dp)
            .testTag("bag-event"),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text("×" + formatValue(mult), style = StepUpUi.type.numberRow.copy(fontSize = 30.sp), color = Color.White)
        Column(Modifier.padding(start = 12.dp)) {
            Text(stringResource(R.string.bag_event_title, formatValue(mult)), color = Color.White, style = StepUpUi.type.rowTitle.copy(fontSize = 15.5.sp))
            Text(stringResource(R.string.bag_event_until, until), color = Color(0xFFE4EDFF), style = StepUpUi.type.caption)
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
