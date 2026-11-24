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
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
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
import com.stepup.android.ui.components.KoreanLineBreak
import com.stepup.android.ui.components.LargeTitle
import com.stepup.android.ui.components.PlainEmptyState
import com.stepup.android.ui.components.PrimaryButton
import com.stepup.android.ui.components.RunTextAction
import com.stepup.android.ui.components.StepUpSpinner
import com.stepup.android.ui.components.StepUpUi
import com.stepup.android.ui.components.runNumberStyle
import com.stepup.android.ui.components.runTextStyle
import com.stepup.android.ui.components.runTone
import com.stepup.android.ui.experience.feedbackClickable
import kotlinx.coroutines.launch
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle
import java.util.UUID

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
        LazyColumn(
            Modifier.fillMaxSize(),
            contentPadding = PaddingValues(top = 6.dp, bottom = if (selected != null) 120.dp else 28.dp),
        ) {
            item {
                // 아무것도 없으면 개수 줄은 아래 빈 카드가 대신한다(2026-11-24 재디자인)
                LargeTitle(
                    stringResource(R.string.tab_items),
                    subtitle = current?.takeIf { it.total > 0 }?.let { b -> stringResource(R.string.bag_count, b.items.count { it.qty > 0 }, b.total) },
                )
            }
            current?.event?.let { event -> item { EventBand(event.mult, event.endsAtMillis) } }
            when {
                current == null && loading -> item {
                    Box(Modifier.fillMaxWidth().padding(top = 80.dp), contentAlignment = Alignment.Center) { StepUpSpinner() }
                }
                current == null && failed -> item {
                    // 불러오기 실패 — 빈 · 오류 상태 한 모양(캐릭터 · 제목 · 글자 버튼)
                    Column(
                        Modifier.fillMaxWidth().padding(horizontal = StepUpUi.Gutter, vertical = 24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                    ) {
                        PlainEmptyState(R.drawable.mascot_oops, stringResource(R.string.bag_load_failed))
                        RunTextAction(
                            stringResource(R.string.bag_retry), { scope.launch { load() } },
                            Modifier.testTag("bag-retry"), chevron = true,
                        )
                    }
                }
                current != null -> {
                    if (current.total == 0) {
                        item { EmptyCard(Modifier.padding(start = StepUpUi.Gutter, end = StepUpUi.Gutter, top = 12.dp)) }
                    }
                    item {
                        SectionHead(stringResource(R.string.bag_section_run), stringResource(R.string.bag_section_run_hint))
                        Row(
                            Modifier.fillMaxWidth().padding(horizontal = StepUpUi.Gutter),
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                        ) {
                            ItemKind.entries.filter { it.runPick }.forEach { kind ->
                                ItemTile(
                                    item = current.items.firstOrNull { it.kind == kind } ?: BagItem(kind, null, 0),
                                    selected = selected == kind,
                                    tall = false,
                                    modifier = Modifier.weight(1f),
                                    onClick = { selected = kind; request = null; message = null },
                                )
                            }
                        }
                    }
                    item {
                        SectionHead(stringResource(R.string.bag_section_now), null)
                        Row(
                            Modifier.fillMaxWidth().padding(horizontal = StepUpUi.Gutter),
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                        ) {
                            ItemKind.entries.filter { !it.runPick }.forEach { kind ->
                                ItemTile(
                                    item = current.items.firstOrNull { it.kind == kind } ?: BagItem(kind, null, 0),
                                    selected = selected == kind,
                                    tall = true,
                                    modifier = Modifier.weight(1f),
                                    onClick = { selected = kind; request = null; message = null },
                                )
                            }
                        }
                    }
                }
            }
        }

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
                    .clip(RoundedCornerShape(26.dp)).background(runTone().panel)
                    .border(1.dp, runTone().panelEdge, RoundedCornerShape(26.dp))
                    .padding(start = 18.dp, end = 10.dp, top = 12.dp, bottom = 12.dp)
                    .testTag("bag-use-bar"),
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f).padding(end = 10.dp)) {
                        Text(itemName(pick), style = StepUpUi.type.rowTitle, color = runTone().text, maxLines = 1, overflow = TextOverflow.Ellipsis)
                        Text(detail, style = StepUpUi.type.rowSubtitle, color = runTone().label, modifier = Modifier.padding(top = 2.dp))
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

@Composable
private fun SectionHead(title: String, hint: String?) {
    val t = runTone()
    Row(
        Modifier.fillMaxWidth().padding(start = StepUpUi.TitleGutter, end = StepUpUi.TitleGutter, top = 22.dp, bottom = 10.dp),
        verticalAlignment = Alignment.Bottom,
    ) {
        Text(
            title, style = runTextStyle(17.sp, t.text, FontWeight.Bold, 1.3f),
            modifier = Modifier.weight(1f).semantics { heading() }, maxLines = 1, overflow = TextOverflow.Ellipsis,
        )
        if (hint != null) {
            Text(hint, style = runTextStyle(13.5.sp, t.label, FontWeight.Medium, 1.3f), maxLines = 1, softWrap = false, modifier = Modifier.padding(start = 8.dp))
        }
    }
}

private val CardShape = RoundedCornerShape(22.dp)

/** 아이템이 하나도 없을 때 — 흰 카드 한 줄 + 캐릭터(재디자인 3장: 아이템 없음 안내 줄 · mascot_bored) */
@Composable
private fun EmptyCard(modifier: Modifier = Modifier) {
    val t = runTone()
    Row(
        modifier.fillMaxWidth().clip(CardShape).background(t.panel).border(1.dp, t.panelEdge, CardShape)
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Image(
            painterResource(R.drawable.mascot_bored), contentDescription = null,
            modifier = Modifier.height(64.dp), contentScale = ContentScale.Fit,
        )
        Column(Modifier.weight(1f).padding(start = 12.dp)) {
            Text(stringResource(R.string.rd_c_bag_empty_title), style = runTextStyle(16.5.sp, t.text, FontWeight.Bold, 1.3f).copy(lineBreak = KoreanLineBreak))
            Text(
                stringResource(R.string.rd_c_bag_empty_body),
                style = runTextStyle(14.sp, t.label, FontWeight.Medium, 1.4f).copy(lineBreak = KoreanLineBreak),
                modifier = Modifier.padding(top = 2.dp),
            )
        }
    }
}

/** 보상 이벤트 줄 — 운영자가 연 이벤트가 켜져 있을 때만(0106). 누르는 곳은 없다. 흰 카드 · 배수 숫자만 파랑 */
@Composable
private fun EventBand(mult: Double, endsAt: Long) {
    val t = runTone()
    val until = remember(endsAt) {
        DateTimeFormatter.ofLocalizedDateTime(FormatStyle.SHORT).format(Instant.ofEpochMilli(endsAt).atZone(ZoneId.systemDefault()))
    }
    Row(
        Modifier.padding(start = StepUpUi.Gutter, end = StepUpUi.Gutter, top = 14.dp).fillMaxWidth().heightIn(min = 56.dp)
            .clip(CardShape).background(t.panel).border(1.dp, t.panelEdge, CardShape)
            .padding(horizontal = 18.dp, vertical = 12.dp)
            .testTag("bag-event"),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text("×" + formatValue(mult), style = runNumberStyle(30.sp, t.cobaltText), maxLines = 1, softWrap = false)
        Column(Modifier.weight(1f).padding(start = 14.dp)) {
            Text(stringResource(R.string.bag_event_title, formatValue(mult)), style = runTextStyle(15.5.sp, t.text, FontWeight.SemiBold, 1.3f))
            Text(stringResource(R.string.bag_event_until, until), style = runTextStyle(13.sp, t.label, FontWeight.Medium, 1.3f))
        }
    }
}

/**
 * 아이템 칸(2026-11-24 재디자인) — 흰 카드 안에 회색 칸(그림) · 이름 · 효과. 개수 배지는 없다 —
 * 가진 개수는 1개 이상일 때만 효과 아래 파란 숫자로(×N), 위 제목 줄에 종류 · 개수 합계. 고르면 테두리만 파랗게 굵어진다.
 */
@Composable
internal fun ItemTile(item: BagItem, selected: Boolean, tall: Boolean, modifier: Modifier = Modifier, onClick: () -> Unit) {
    val t = runTone()
    Column(
        modifier
            .heightIn(min = if (tall) 166.dp else 150.dp)
            .clip(CardShape)
            .background(t.panel)
            .border(if (selected) 2.dp else 1.dp, if (selected) t.cobalt else t.panelEdge, CardShape)
            .feedbackClickable(role = Role.Button, onClick = onClick)
            .semantics { this.selected = selected }
            .testTag("bag-item-${item.kind.id}")
            .padding(start = 10.dp, end = 10.dp, top = 10.dp, bottom = 14.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Box(
            Modifier.fillMaxWidth().aspectRatio(if (tall) 1.45f else 1.05f).clip(RoundedCornerShape(16.dp)).background(t.inset),
            contentAlignment = Alignment.Center,
        ) {
            Image(
                painterResource(itemArt(item.kind)), contentDescription = null,
                modifier = Modifier.fillMaxSize().padding(if (tall) 10.dp else 6.dp), contentScale = ContentScale.Fit,
            )
        }
        Text(
            itemName(item.kind), textAlign = TextAlign.Center, maxLines = 2, overflow = TextOverflow.Ellipsis,
            style = runTextStyle(if (tall) 16.sp else 15.sp, t.text, FontWeight.Bold, 1.25f).copy(lineBreak = KoreanLineBreak),
            modifier = Modifier.padding(top = 12.dp),
        )
        Text(
            effectText(item.kind, item.value), textAlign = TextAlign.Center, maxLines = 3,
            style = runTextStyle(13.5.sp, t.label, FontWeight.Medium, 1.35f).copy(lineBreak = KoreanLineBreak),
            modifier = Modifier.padding(top = 3.dp),
        )
        if (item.qty > 0) {
            Text(
                stringResource(R.string.item_qty, item.qty), maxLines = 1, softWrap = false,
                style = runTextStyle(14.sp, t.cobaltText, FontWeight.Bold, 1.2f).copy(fontFeatureSettings = "tnum"),
                modifier = Modifier.padding(top = 4.dp),
            )
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
