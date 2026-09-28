package com.stepup.android.ui.screens.customize

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyGridState
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.MoreHoriz
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.onClick
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.stepup.android.R
import com.stepup.android.domain.ShoeTier
import com.stepup.android.domain.Sneaker
import com.stepup.android.domain.tier
import com.stepup.android.ui.components.GradeArtRatio
import com.stepup.android.ui.components.S2ShoesSections
import com.stepup.android.ui.components.SettingsSheet
import com.stepup.android.ui.components.ShoeNameWithBadge
import com.stepup.android.ui.components.ShoeSection
import com.stepup.android.ui.components.SneakerGradeStage
import com.stepup.android.ui.components.SneakerGradeThumb
import com.stepup.android.ui.components.SortBottomSheet
import com.stepup.android.ui.components.StepUpIcons
import com.stepup.android.ui.components.label
import com.stepup.android.ui.components.preloadShoeArt
import com.stepup.android.ui.components.settingsPalette
import com.stepup.android.ui.experience.feedbackClickable
import com.stepup.android.ui.guide.GuideTour
import com.stepup.android.ui.guide.guideTarget
import com.stepup.android.ui.screens.items.ItemSort
import com.stepup.android.ui.screens.items.ItemsViewModel
import com.stepup.android.ui.screens.items.OwnedLoad
import com.stepup.android.ui.screens.items.ShoeStatePanel
import com.stepup.android.ui.screens.items.formatPercent
import com.stepup.android.ui.screens.items.itemSortRes
import com.stepup.android.ui.screens.items.shoeName
import com.stepup.android.ui.theme.StepUpColors
import com.stepup.android.ui.theme.StepUpDesign
import kotlin.math.roundToInt

/**
 * 신발 탭(신발 화면 확정안 2026-09-28 — docs/redesign/shoes-ui-2026-09-28, 시안 01 내 신발 · 02 신발 보관함).
 *
 * 위 글자 탭 둘(내 신발 · 신발 보관함). 공통 머리(로고 · 실제 잔액)와 하단 탭 다섯(러닝 / 신발 / 뽑기 / 커뮤니티 / 내 정보)은
 * 앱 셸이 그린다. 두 탭은 보는 켤레(소유 id)를 함께 쓴다 — 보관함에서 고른 켤레가 내 신발에 그대로 보인다.
 *
 * 내 신발 — 이름 + 끝의 작은 둥근 등급 배지 → Lv · 번호 · 착용 상태 → 등급 프레임 속 실제 신발 → 능력치 막대 셋 → 보유 신발 가로 목록.
 * 기본 휴대폰 크기에서 세로로 끌지 않아도 다 보이게 큰 무대가 남은 높이만큼만 커진다(작은 화면 · 큰 글씨는 무대부터 줄이고,
 * 그래도 모자랄 때만 스크롤). 칸을 누르면 보는 신발만 바뀐다 — 착용은 그대로. 무대 · ⋯ 를 누르면 그 켤레의 기존 관리
 * (이 신발 신기 · 강화 · 수리 · 판매 — 신발 상세)로 간다. "신발 자세히 보기" 글 링크는 없앴다.
 *
 * 신발 보관함 — 보유 켤레 수 · 정렬 · 등급 거르기(실제 수) → 2열 격자. 칸을 누르면 그 켤레를 고르고 관리로 간다.
 * 착용 체크는 실제로 신고 있는 한 켤레에만, 고른 켤레는 테두리로 따로 보인다. 도감 · 마켓 · 아이템은 격자 아래 작은 줄(지우지 않음).
 */
@Composable
fun CustomizeScreen(
    onBack: () -> Unit = {},
    onOpenDex: () -> Unit = {},
    onOpenMarketModel: (String, String, Int) -> Unit = { _, _, _ -> },
    onChangeBackground: () -> Unit = {},
    onOpenWallet: () -> Unit = {},
    onOpenMarket: () -> Unit = {},
    onOpenVault: () -> Unit = {},
    onOpenSneaker: (Long) -> Unit = {},
    onOpenDraw: () -> Unit = {},
    /** 뽑기 결과의 "내 신발 보기" — 받은 신발을 고른 채로 내 신발을 연다(미리 보기일 뿐, 착용은 바꾸지 않는다) */
    focusShoeId: Long? = null,
    onFocusShoeShown: () -> Unit = {},
    viewModel: ItemsViewModel = viewModel(factory = ItemsViewModel.Factory),
) {
    val owned by viewModel.owned.collectAsStateWithLifecycle()
    var section by rememberSaveable { mutableStateOf(ShoeSection.MINE) }
    // 보는 켤레 — 소유 id. 없거나 사라졌으면 신고 있는 켤레(resolvePair)
    var selectedId by rememberSaveable { mutableStateOf<Long?>(null) }
    var sort by rememberSaveable { mutableStateOf(ItemSort.RECENT) }
    var filter by rememberSaveable { mutableStateOf<String?>(null) }
    val grid = rememberLazyGridState()
    LaunchedEffect(focusShoeId) {
        if (focusShoeId != null) {
            selectedId = focusShoeId
            section = ShoeSection.MINE
            onFocusShoeShown()
        }
    }
    Column(Modifier.fillMaxSize()) {
        S2ShoesSections(section, onSelect = { section = it })
        when (section) {
            ShoeSection.MINE -> MyShoesContent(
                load = owned, selectedId = selectedId, onSelect = { selectedId = it },
                onOpenSneaker = onOpenSneaker, onOpenDraw = onOpenDraw, onReload = viewModel::reloadOwned,
                modifier = Modifier.weight(1f),
            )
            ShoeSection.VAULT -> ShoeVaultContent(
                load = owned, selectedId = selectedId, sort = sort, filter = filter,
                onSort = { sort = it }, onFilter = { filter = it },
                onOpen = { id ->
                    selectedId = id
                    onOpenSneaker(id)
                },
                onOpenDraw = onOpenDraw, onOpenDex = onOpenDex, onOpenMarket = onOpenMarket, onOpenItems = onOpenVault,
                onReload = viewModel::reloadOwned, gridState = grid, modifier = Modifier.weight(1f),
            )
        }
    }
}

// ── 내 신발 ───────────────────────────────────────────────────────

/**
 * 내 신발 본문 — 상태를 받아 그리기만 한다(기기 검사가 장면을 바로 넣어 찍는다).
 * [initialBasis] 는 막대 기준 시트를 연 채로 시작한다(검사용).
 */
@Composable
fun MyShoesContent(
    load: OwnedLoad,
    selectedId: Long?,
    onSelect: (Long) -> Unit,
    onOpenSneaker: (Long) -> Unit,
    onOpenDraw: () -> Unit,
    onReload: () -> Unit,
    modifier: Modifier = Modifier,
    initialBasis: Boolean = false,
) {
    when {
        load == OwnedLoad.Loading -> MyShoesLoading(modifier)
        load == OwnedLoad.Failed -> StateBox(modifier) {
            ShoeStatePanel(
                icon = Icons.Filled.Refresh,
                title = stringResource(R.string.sdv_load_failed_title),
                body = stringResource(R.string.sdv_load_failed_body),
                primary = stringResource(R.string.sdv_reload) to onReload,
                modifier = Modifier.padding(horizontal = StepUpDesign.Gutter).testTag("shoe-tab-failed"),
                top = 56.dp,
            )
        }
        // 읽기가 끝났고 정말 비었을 때만. 누르면 하단 뽑기 탭(뽑기 화면이 수 · 조건을 정한다)
        load is OwnedLoad.Ready && load.shoes.isEmpty() -> StateBox(modifier) {
            ShoeStatePanel(
                icon = StepUpIcons.Shoe,
                title = stringResource(R.string.sdv_empty_title),
                body = stringResource(R.string.sdv_empty_body),
                primary = stringResource(R.string.sdv_empty_action) to onOpenDraw,
                modifier = Modifier.padding(horizontal = StepUpDesign.Gutter).testTag("shoe-tab-empty"),
                top = 56.dp,
            )
        }
        load is OwnedLoad.Ready -> MyShoesReady(load.shoes, selectedId, onSelect, onOpenSneaker, modifier, initialBasis)
    }
}

/** 빈 목록 · 실패 — 큰 글씨에서도 잘리지 않게 스크롤 안에 */
@Composable
private fun StateBox(modifier: Modifier, content: @Composable () -> Unit) {
    Box(modifier.fillMaxWidth().verticalScroll(rememberScrollState())) { content() }
}

@Composable
private fun MyShoesReady(
    shoes: List<Sneaker>,
    selectedId: Long?,
    onSelect: (Long) -> Unit,
    onOpenSneaker: (Long) -> Unit,
    modifier: Modifier,
    initialBasis: Boolean,
) {
    val context = LocalContext.current
    val row = remember(shoes) { ownedRow(shoes) }
    val selected = resolvePair(row, selectedId) ?: return
    // 무대에 보이는 켤레 — 새로 고른 켤레의 그림을 먼저 읽은 뒤 이름 · 능력치와 함께 바꾼다(글만 앞서 바뀌지 않게)
    var shownId by rememberSaveable { mutableStateOf<Long?>(null) }
    LaunchedEffect(selected.id) {
        if (shownId != null && shownId != selected.id) preloadShoeArt(context, selected)
        shownId = selected.id
    }
    val shown = shoes.firstOrNull { it.id == shownId } ?: selected
    var basisOpen by rememberSaveable { mutableStateOf(initialBasis) }
    val rowState = rememberLazyListState()
    // 처음 한 번만 — 고른 칸이 보이게 가로 목록을 둔다. 돌아왔을 때는 사용자가 둔 자리를 그대로 쓴다
    var rowPlaced by rememberSaveable { mutableStateOf(false) }
    LaunchedEffect(row.isNotEmpty()) {
        if (!rowPlaced && row.isNotEmpty()) {
            val index = row.indexOfFirst { it.id == selected.id }
            if (index > 1) rowState.scrollToItem(index - 1)
            rowPlaced = true
        }
    }
    BoxWithConstraints(modifier.fillMaxWidth()) {
        val viewport = constraints.maxHeight
        // 짧은 창(작은 폰 · 3버튼 내비 · 큰 글씨)은 무대가 줄기 전에 여백 · 보유 칸부터 줄인다
        val compact = maxHeight < CompactBelow
        MyShoesLayout(
            viewport = viewport,
            compact = compact,
            modifier = Modifier.fillMaxWidth().verticalScroll(rememberScrollState()).testTag("my-shoes"),
            // 관리(⋯) · 무대는 고른 켤레의 관리로 — 방금 고른 칸의 그림을 읽는 동안(무대가 아직 앞 켤레)에 눌러도 고른 켤레다
            header = { NameBlock(shown, compact, onManage = { onOpenSneaker(selected.id) }) },
            stage = { StageBlock(shown, onOpen = { onOpenSneaker(selected.id) }) },
            stats = { StatsBlock(shown, compact, onExplain = { basisOpen = true }) },
            owned = { OwnedBlock(row, compact, selectedId = selected.id, state = rowState, onPick = onSelect) },
        )
    }
    if (basisOpen) StatBasisSheet(onClose = { basisOpen = false })
}

/** 무대가 이보다 작아지면 줄이지 않고 스크롤에 맡긴다 */
private val MinStageHeight = 116.dp

/** 창이 이보다 짧으면 여백 · 보유 칸 · 이름 글자를 줄인다(무대를 지키려고) */
private val CompactBelow = 560.dp

/** 위 · 이름↔무대 · 무대↔능력치 · 능력치↔보유 · 아래 */
private val LayoutGaps = listOf(8.dp, 8.dp, 12.dp, 14.dp, 12.dp)
private val CompactGaps = listOf(4.dp, 6.dp, 8.dp, 10.dp, 8.dp)

/**
 * 이름 · 무대 · 능력치 · 보유 목록을 위에서부터 쌓는다. 글자 칸(이름 · 능력치 · 보유)을 먼저 재고, 무대는 남은 높이만큼
 * (440:418 비율 그대로, 폭을 넘지 않게) — 창 높이 [viewport] 안에 다 들면 스크롤 거리가 0 이다.
 */
@Composable
private fun MyShoesLayout(
    viewport: Int,
    compact: Boolean,
    modifier: Modifier,
    header: @Composable () -> Unit,
    stage: @Composable () -> Unit,
    stats: @Composable () -> Unit,
    owned: @Composable () -> Unit,
) {
    val density = LocalDensity.current
    val gaps = remember(density, compact) { with(density) { (if (compact) CompactGaps else LayoutGaps).map { it.roundToPx() } } }
    val minStage = with(density) { MinStageHeight.roundToPx() }
    val gutter = with(density) { StepUpDesign.Gutter.roundToPx() }
    Layout(contents = listOf(header, stage, stats, owned), modifier = modifier) { (h, st, sa, o), constraints ->
        val width = constraints.maxWidth
        val loose = Constraints(maxWidth = width)
        val headerP = h.first().measure(loose)
        val statsP = sa.first().measure(loose)
        val ownedP = o.first().measure(loose)
        val fixed = headerP.height + statsP.height + ownedP.height + gaps.sum()
        val widest = (width - gutter * 2).coerceAtLeast(1)
        val tallest = (widest / GradeArtRatio).toInt()
        val stageH = (viewport - fixed).coerceAtMost(tallest).coerceAtLeast(minOf(minStage, tallest))
        val stageW = (stageH * GradeArtRatio).roundToInt().coerceAtMost(widest)
        val stageP = st.first().measure(Constraints.fixed(stageW, stageH))
        val content = fixed + stageH
        val height = if (viewport == Constraints.Infinity) content else maxOf(content, viewport)
        // 무대가 가장 커지고도 남는 높이(큰 화면)는 무대 위아래로 나눠 가운데에 둔다
        val spare = height - content
        layout(width, height) {
            var y = gaps[0]
            headerP.place(0, y)
            y += headerP.height + gaps[1] + spare / 2
            stageP.place((width - stageW) / 2, y)
            y += stageH + gaps[2] + (spare - spare / 2)
            statsP.place(0, y)
            y += statsP.height + gaps[3]
            ownedP.place(0, y)
        }
    }
}

/** 이름 + 끝의 둥근 등급 배지(최대 두 줄) · 오른쪽 작은 관리(⋯) → 아래 Lv · 번호 · 착용 상태 */
@Composable
private fun NameBlock(shoe: Sneaker, compact: Boolean, onManage: () -> Unit) {
    val p = settingsPalette()
    Row(Modifier.fillMaxWidth().padding(start = StepUpDesign.Gutter, end = 6.dp)) {
        Column(Modifier.weight(1f).padding(top = 2.dp)) {
            ShoeNameWithBadge(
                name = shoe.shoeName(), tier = shoe.tier,
                style = TextStyle(color = p.text, fontSize = if (compact) 19.sp else 21.sp, fontWeight = FontWeight.SemiBold,
                    lineHeight = 1.28.em, letterSpacing = (-0.02).em),
                // 이름 글과 배지의 대체 글(" 레어")을 한 덩어리로 — 읽기 도구가 이름을 제목으로 읽는다(감싸는 칸에만 제목을 달면
                // 그 칸은 글이 없어 읽기 도구가 머물지 않는다)
                modifier = Modifier.fillMaxWidth().semantics(mergeDescendants = true) { heading() }.testTag("shoe-hero-name"),
            )
            Spacer(Modifier.height(3.dp))
            ShoeMeta(shoe, fontSize = 13.5f, modifier = Modifier.testTag("shoe-hero-meta"))
        }
        // 작은 관리 메뉴 — 이 켤레의 기존 관리(신기 · 강화 · 수리 · 판매)로. 큰 상세 버튼 · 원형 착용 영역을 두지 않는다
        val manage = stringResource(R.string.my_shoes_manage)
        Box(
            Modifier.size(StepUpDesign.TouchTarget).clip(CircleShape)
                .feedbackClickable(role = Role.Button, onClick = onManage)
                .semantics { contentDescription = manage }
                .testTag("shoe-manage"),
            contentAlignment = Alignment.Center,
        ) {
            Icon(Icons.Filled.MoreHoriz, contentDescription = null, tint = p.secondary, modifier = Modifier.size(22.dp))
        }
    }
}

/** "Lv. 1 · #0001 · 착용 중" — 착용 중은 실제로 신고 있는 켤레에만, 강조색으로 */
@Composable
private fun ShoeMeta(shoe: Sneaker, fontSize: Float, modifier: Modifier = Modifier) {
    val p = settingsPalette()
    val levelNo = stringResource(R.string.sdv_level_no, shoe.level, shoe.mintNumber)
    val wearing = stringResource(R.string.my_shoes_wearing)
    val cd = stringResource(R.string.sdv_level_no_cd, shoe.level, shoe.mintNumber) +
        if (shoe.equipped) ", $wearing" else ""
    Text(
        buildAnnotatedString {
            append(levelNo)
            if (shoe.equipped) {
                append(" · ")
                withStyle(SpanStyle(color = p.accent, fontWeight = FontWeight.SemiBold)) { append(wearing) }
            }
        },
        color = p.secondary, fontSize = fontSize.sp, lineHeight = 1.3.em, maxLines = 2,
        modifier = modifier.semantics { contentDescription = cd },
    )
}

/** 등급 프레임 속 실제 신발(무대 면 → 뒤 효과 → 신발 → 앞 효과 → 프레임). 누르면 이 켤레의 관리로 */
@Composable
private fun StageBlock(shoe: Sneaker, onOpen: () -> Unit) {
    val manage = stringResource(R.string.my_shoes_manage)
    Box(
        Modifier.fillMaxSize()
            .guideTarget(GuideTour.Targets.CUSTOMIZE_PREVIEW)
            .feedbackClickable(role = Role.Button, onClick = onOpen)
            .semantics { onClick(label = manage) { onOpen(); true } }
            .testTag("shoe-hero"),
    ) {
        SneakerGradeStage(shoe, Modifier.fillMaxSize())
    }
}

/**
 * 능력치 — 제목 · 효율 · 착화감 · 내구도. 세 막대는 같은 폭 · 두께 · 정렬.
 * 덩어리 전체가 막대 기준 설명을 연다(제목 옆 ⓘ 가 그 표시) — 작은 단추를 따로 두지 않는다.
 */
@Composable
private fun StatsBlock(shoe: Sneaker, compact: Boolean, onExplain: () -> Unit) {
    val p = settingsPalette()
    val bars = remember(shoe) { statBars(shoe) }
    val explain = stringResource(R.string.my_shoes_basis_open)
    Column(
        Modifier.fillMaxWidth().padding(horizontal = StepUpDesign.Gutter - 6.dp).clip(RoundedCornerShape(12.dp))
            .feedbackClickable(role = Role.Button, onClick = onExplain)
            .semantics { onClick(label = explain) { onExplain(); true } }
            .padding(horizontal = 6.dp, vertical = 2.dp)
            .testTag("shoe-stats"),
    ) {
        Row(Modifier.padding(bottom = if (compact) 6.dp else 8.dp).testTag("shoe-stats-basis"), verticalAlignment = Alignment.CenterVertically) {
            Text(stringResource(R.string.my_shoes_stats), color = p.text, fontSize = 16.sp, fontWeight = FontWeight.SemiBold,
                modifier = Modifier.semantics { heading() })
            Spacer(Modifier.width(6.dp))
            Icon(Icons.Outlined.Info, contentDescription = null, tint = p.secondary, modifier = Modifier.size(16.dp))
        }
        Column(verticalArrangement = Arrangement.spacedBy(if (compact) 6.dp else 9.dp)) {
            bars.forEach { bar -> StatBarRow(bar) }
        }
    }
}

@Composable
private fun StatBarRow(bar: StatBar) {
    val p = settingsPalette()
    val (title, hint, full) = when (bar.stat) {
        BarStat.EFFICIENCY -> Triple(
            stringResource(R.string.my_shoes_stat_efficiency), stringResource(R.string.sdv_stat_bonus),
            formatPercent(StatScale.EFFICIENCY_MAX_PERCENT),
        )
        BarStat.COMFORT -> Triple(
            stringResource(R.string.sdv_stat_comfort), stringResource(R.string.sdv_stat_energy),
            formatPercent(StatScale.ENERGY_MAX_PERCENT),
        )
        BarStat.DURABILITY -> Triple(stringResource(R.string.sdv_stat_durability), null, StatScale.DURABILITY_MAX.toString())
    }
    val cd = stringResource(R.string.my_shoes_stat_cd, title, hint ?: title, bar.value, full)
    val tag = when (bar.stat) {
        BarStat.EFFICIENCY -> "bonus"
        BarStat.COMFORT -> "energy"
        BarStat.DURABILITY -> "durability"
    }
    Column(Modifier.fillMaxWidth().semantics(mergeDescendants = true) { contentDescription = cd }.testTag("shoe-bar-$tag")) {
        Row(Modifier.fillMaxWidth()) {
            // 제목 · 짧은 설명은 남는 폭 안에서(길면 설명을 줄인다), 값은 늘 오른쪽 끝
            Row(Modifier.weight(1f).alignByBaseline()) {
                Text(title, color = p.text, fontSize = 15.sp, fontWeight = FontWeight.SemiBold, maxLines = 1,
                    modifier = Modifier.alignByBaseline())
                if (hint != null) {
                    Spacer(Modifier.width(8.dp))
                    Text(hint, color = p.secondary, fontSize = 12.5.sp, maxLines = 1, overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f, fill = false).alignByBaseline())
                }
            }
            Spacer(Modifier.width(8.dp))
            Text(bar.value, color = p.text, fontSize = 15.sp, fontWeight = FontWeight.SemiBold, maxLines = 1,
                modifier = Modifier.alignByBaseline().testTag("shoe-bar-$tag-value"))
        }
        Spacer(Modifier.height(5.dp))
        StatBarTrack(bar.fraction, Modifier.fillMaxWidth().height(7.dp))
    }
}

/** 막대 — 같은 트랙 · 같은 파랑. 값이 조금이라도 있으면 둥근 끝이 보일 만큼은 채운다 */
@Composable
private fun StatBarTrack(fraction: Float, modifier: Modifier) {
    val dark = StepUpColors.dark
    val track = if (dark) Color(0xFF16233A) else Color(0xFFDCE4F0)
    val fill = if (dark) listOf(Color(0xFF2C77F0), Color(0xFF5AA6FF)) else listOf(Color(0xFF2E5FB8), Color(0xFF4C86E0))
    Box(
        modifier.drawBehind {
            val radius = CornerRadius(size.height / 2)
            drawRoundRect(track, cornerRadius = radius)
            if (fraction > 0f) {
                val w = (size.width * fraction).coerceAtLeast(size.height)
                drawRoundRect(Brush.horizontalGradient(fill, endX = w), size = Size(w, size.height), cornerRadius = radius)
            }
        },
    )
}

/** 보유 신발 N → 켤레마다 한 칸(작은 프레임). 고른 칸은 바깥 테두리, 신고 있는 켤레는 작은 체크 — 서로 다른 표시 */
@Composable
private fun OwnedBlock(row: List<Sneaker>, compact: Boolean, selectedId: Long, state: LazyListState, onPick: (Long) -> Unit) {
    val p = settingsPalette()
    Column(Modifier.fillMaxWidth()) {
        Row(
            Modifier.padding(horizontal = StepUpDesign.Gutter).semantics(mergeDescendants = true) {}.testTag("shoe-owned-head"),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(stringResource(R.string.my_shoes_owned), color = p.text, fontSize = 16.sp, fontWeight = FontWeight.SemiBold,
                modifier = Modifier.semantics { heading() })
            Spacer(Modifier.width(8.dp))
            Text(row.size.toString(), color = p.secondary, fontSize = 16.sp, fontWeight = FontWeight.SemiBold,
                modifier = Modifier.testTag("shoe-owned-count"))
        }
        Spacer(Modifier.height(if (compact) 5.dp else 8.dp))
        LazyRow(
            state = state,
            modifier = Modifier.fillMaxWidth().testTag("shoe-owned-row"),
            contentPadding = PaddingValues(horizontal = StepUpDesign.Gutter - ThumbRing),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            items(row, key = { it.id }) { shoe ->
                OwnedThumb(shoe, height = if (compact) CompactThumbHeight else ThumbHeight, selected = shoe.id == selectedId,
                    onClick = { onPick(shoe.id) })
            }
        }
    }
}

/** 칸 테두리 자리 — 고른 칸의 바깥 선이 이 안에 그려져 이웃 칸과 겹치지 않는다 */
private val ThumbRing = 4.dp
private val ThumbHeight = 72.dp
private val CompactThumbHeight = 62.dp

@Composable
private fun OwnedThumb(shoe: Sneaker, height: Dp, selected: Boolean, onClick: () -> Unit) {
    val p = settingsPalette()
    val name = shoe.shoeName()
    val levelNo = stringResource(R.string.sdv_level_no_cd, shoe.level, shoe.mintNumber)
    val state = listOfNotNull(
        stringResource(R.string.sdv_state_viewing).takeIf { selected },
        stringResource(R.string.my_shoes_wearing).takeIf { shoe.equipped },
    ).joinToString(", ")
    val tierName = shoe.tier.label()
    val ring = if (StepUpColors.dark) Color(0xFFE9F0FF) else p.text
    Box(
        Modifier
            .size(width = height * GradeArtRatio + ThumbRing * 2, height = height + ThumbRing * 2)
            .clip(RoundedCornerShape(14.dp))
            .then(if (selected) Modifier.border(2.dp, ring, RoundedCornerShape(14.dp)) else Modifier)
            .feedbackClickable(role = Role.RadioButton, onClick = onClick)
            .semantics {
                this.selected = selected
                contentDescription = "$name, $tierName, $levelNo"
                if (state.isNotEmpty()) stateDescription = state
            }
            .testTag("shoe-choice-${shoe.id}"),
        contentAlignment = Alignment.Center,
    ) {
        SneakerGradeThumb(shoe, Modifier.height(height))
        if (shoe.equipped) WornCheck(Modifier.align(Alignment.TopEnd).offset(x = (-2).dp, y = 2.dp), size = 17.dp)
    }
}

/** 착용 체크 — 실제로 신고 있는 한 켤레에만(고른 표시와 다르다) */
@Composable
private fun WornCheck(modifier: Modifier, size: Dp) {
    val dark = StepUpColors.dark
    Box(
        modifier.size(size).clip(CircleShape).background(Color(0xFF2F86FF))
            .border(1.5.dp, if (dark) Color(0xFF0A1424) else Color.White, CircleShape)
            .testTag("shoe-worn-badge"),
        contentAlignment = Alignment.Center,
    ) {
        Icon(Icons.Filled.Check, contentDescription = null, tint = Color.White, modifier = Modifier.size(size * 0.66f))
    }
}

/** 읽는 중 — 이름 자리 · 빈 무대 · "신발을 불러오고 있어요." (임시 값 · 예시 신발을 보이지 않는다) */
@Composable
private fun MyShoesLoading(modifier: Modifier) {
    val p = settingsPalette()
    Column(modifier.fillMaxWidth().padding(horizontal = StepUpDesign.Gutter).padding(top = 12.dp).testTag("shoe-tab-loading")) {
        Box(Modifier.size(width = 168.dp, height = 22.dp).clip(RoundedCornerShape(6.dp)).background(p.skeleton))
        Spacer(Modifier.height(10.dp))
        Box(Modifier.size(width = 112.dp, height = 12.dp).clip(RoundedCornerShape(4.dp)).background(p.skeleton))
        Spacer(Modifier.height(14.dp))
        Box(
            Modifier.fillMaxWidth(0.72f).aspectRatio(GradeArtRatio).align(Alignment.CenterHorizontally)
                .clip(RoundedCornerShape(22.dp)).background(p.skeleton.copy(alpha = 0.6f)),
            contentAlignment = Alignment.Center,
        ) {
            Text(stringResource(R.string.sdv_loading), color = p.secondary, fontSize = 15.sp, textAlign = TextAlign.Center)
        }
    }
}

/** 막대 기준 — 모든 신발에 같은 기준(서버의 최대 · 상한)이라는 것과 효율은 금액이 아니라는 것 */
@Composable
private fun StatBasisSheet(onClose: () -> Unit) {
    val p = settingsPalette()
    SettingsSheet(
        title = stringResource(R.string.my_shoes_basis_open), onDismiss = onClose,
        modifier = Modifier.testTag("shoe-basis-sheet"),
    ) {
        listOf(
            stringResource(R.string.my_shoes_basis_intro),
            stringResource(R.string.my_shoes_basis_efficiency, formatPercent(StatScale.EFFICIENCY_MAX_PERCENT), StatScale.EFFICIENCY_MAX_LEVEL),
            stringResource(R.string.my_shoes_basis_comfort, formatPercent(StatScale.ENERGY_MAX_PERCENT)),
            stringResource(R.string.my_shoes_basis_durability, StatScale.DURABILITY_MAX),
        ).forEach { line ->
            Text(line, color = p.text, fontSize = 15.sp, lineHeight = 1.45.em)
        }
        Text(stringResource(R.string.my_shoes_basis_note), color = p.secondary, fontSize = 14.sp, lineHeight = 1.45.em,
            modifier = Modifier.padding(top = 4.dp).testTag("shoe-basis-note"))
    }
}

// ── 신발 보관함 ───────────────────────────────────────────────────

/**
 * 신발 보관함 본문 — 보유 수 · 정렬 · 거르기(고정) 아래 2열 격자(세로로 넘긴다).
 * [filter] 는 보이는 갈래의 key(null = 전체). 그 갈래 신발이 없어졌으면 전체로 보인다.
 */
@Composable
fun ShoeVaultContent(
    load: OwnedLoad,
    selectedId: Long?,
    sort: String,
    filter: String?,
    onSort: (String) -> Unit,
    onFilter: (String?) -> Unit,
    onOpen: (Long) -> Unit,
    onOpenDraw: () -> Unit,
    onOpenDex: () -> Unit,
    onOpenMarket: () -> Unit,
    onOpenItems: () -> Unit,
    onReload: () -> Unit,
    modifier: Modifier = Modifier,
    gridState: LazyGridState = rememberLazyGridState(),
    initialSortSheet: Boolean = false,
) {
    when {
        load == OwnedLoad.Loading -> VaultLoading(modifier)
        load == OwnedLoad.Failed -> StateBox(modifier) {
            ShoeStatePanel(
                icon = Icons.Filled.Refresh,
                title = stringResource(R.string.sdv_load_failed_title),
                body = stringResource(R.string.sdv_load_failed_body),
                primary = stringResource(R.string.sdv_reload) to onReload,
                modifier = Modifier.padding(horizontal = StepUpDesign.Gutter).testTag("vault-failed"),
                top = 56.dp,
            )
        }
        load is OwnedLoad.Ready && load.shoes.isEmpty() -> StateBox(modifier) {
            ShoeStatePanel(
                icon = StepUpIcons.Shoe,
                title = stringResource(R.string.sdv_empty_title),
                body = stringResource(R.string.sdv_empty_body),
                primary = stringResource(R.string.sdv_empty_action) to onOpenDraw,
                modifier = Modifier.padding(horizontal = StepUpDesign.Gutter).testTag("vault-empty"),
                top = 56.dp,
            )
        }
        load is OwnedLoad.Ready -> VaultReady(
            load.shoes, selectedId, sort, filter, onSort, onFilter, onOpen, onOpenDex, onOpenMarket, onOpenItems,
            modifier, gridState, initialSortSheet,
        )
    }
}

@Composable
private fun VaultReady(
    shoes: List<Sneaker>,
    selectedId: Long?,
    sort: String,
    filterKey: String?,
    onSort: (String) -> Unit,
    onFilter: (String?) -> Unit,
    onOpen: (Long) -> Unit,
    onOpenDex: () -> Unit,
    onOpenMarket: () -> Unit,
    onOpenItems: () -> Unit,
    modifier: Modifier,
    gridState: LazyGridState,
    initialSortSheet: Boolean,
) {
    val filter = remember(shoes, filterKey) { activeFilter(shoes, filterKey) }
    val counts = remember(shoes) { tierCounts(shoes) }
    val shown = remember(shoes, filter, sort) { vaultShown(shoes, filter, sort) }
    val chosen = remember(shoes, selectedId) { resolvePair(shoes, selectedId)?.id }
    var sortSheet by rememberSaveable { mutableStateOf(initialSortSheet) }
    // 정렬 · 거르기가 바뀌면 새 순서의 첫 켤레부터 보인다. 격자는 맨 위에 있던 켤레를 키로 따라가므로(그 켤레가 뒤로 가면 격자
    // 가운데가 보인다) 새 순서를 그리는 이 조합에서 맨 위를 요청한다 — 누르는 순간에 요청하면 새 순서를 그리기 전의 배치가 그 요청을
    // 먼저 써 버려 한 줄 내려간 채 남았다(2026-09-28 기기 캡처). 처음 그릴 때(상세에서 돌아올 때)는 보던 자리 그대로다
    val order = sort to filter
    val drawnOrder = remember { DrawnOrder(order) }
    if (drawnOrder.value != order) {
        drawnOrder.value = order
        gridState.requestScrollToItem(0)
    }
    Column(modifier.fillMaxWidth()) {
        VaultHeader(shoes.size, sort, onSortClick = { sortSheet = true })
        VaultFilters(counts, filter, onFilter = { onFilter(it?.key) })
        LazyVerticalGrid(
            columns = GridCells.Fixed(2),
            state = gridState,
            modifier = Modifier.fillMaxWidth().weight(1f).testTag("vault-grid"),
            contentPadding = PaddingValues(start = StepUpDesign.Gutter, end = StepUpDesign.Gutter, top = 14.dp, bottom = 18.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            // 칸 사이 18dp = 칸 안 아래 여백 10dp(VaultCard) + 8dp
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            items(shown, key = { it.id }) { shoe ->
                VaultCard(shoe, selected = shoe.id == chosen, onClick = { onOpen(shoe.id) })
            }
            item(key = "links", span = { GridItemSpan(maxLineSpan) }) { VaultLinks(onOpenDex, onOpenMarket, onOpenItems) }
        }
    }
    if (sortSheet) {
        SortBottomSheet(
            title = stringResource(R.string.vault_sort_title),
            options = VaultSorts,
            selected = sort,
            label = { stringResource(itemSortRes(it)) },
            onPick = onSort,
            onDismiss = { sortSheet = false },
        )
    }
}

/** 격자가 마지막으로 그린 정렬 · 거르기 — 바뀌었는지 가르는 데만 쓴다(그리기를 다시 부르는 상태가 아니다) */
private class DrawnOrder(var value: Pair<String, ShoeTier?>)

/** "보유 신발 3" · 오른쪽 정렬("최근 획득순 ⌄") */
@Composable
private fun VaultHeader(count: Int, sort: String, onSortClick: () -> Unit) {
    val p = settingsPalette()
    val sortLabel = stringResource(itemSortRes(sort))
    val sortCd = stringResource(R.string.vault_sort_cd, sortLabel)
    Row(
        Modifier.fillMaxWidth().padding(start = StepUpDesign.Gutter, end = 8.dp, top = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Row(Modifier.weight(1f).semantics(mergeDescendants = true) {}.testTag("vault-head"), verticalAlignment = Alignment.CenterVertically) {
            Text(stringResource(R.string.my_shoes_owned), color = p.text, fontSize = 17.sp, fontWeight = FontWeight.SemiBold,
                modifier = Modifier.semantics { heading() })
            Spacer(Modifier.width(8.dp))
            Text(count.toString(), color = p.text, fontSize = 17.sp, fontWeight = FontWeight.SemiBold,
                modifier = Modifier.testTag("vault-count"))
        }
        Row(
            Modifier.heightIn(min = StepUpDesign.TouchTarget).clip(RoundedCornerShape(12.dp))
                .feedbackClickable(role = Role.Button, onClick = onSortClick)
                .semantics { contentDescription = sortCd }
                .padding(horizontal = 12.dp).testTag("vault-sort"),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(sortLabel, color = p.accent, fontSize = 14.sp, fontWeight = FontWeight.SemiBold, maxLines = 1)
            Spacer(Modifier.width(4.dp))
            Icon(Icons.Filled.KeyboardArrowDown, contentDescription = null, tint = p.accent, modifier = Modifier.size(20.dp))
        }
    }
}

/**
 * 거르기 — 전체 + 가진 갈래(실제 수). 한 줄 알약 안의 칸들, 좁으면 가로로 넘긴다(여러 줄로 쌓지 않는다).
 * 고른 칸은 파란 테두리 알약. 누를 수 있는 곳이라 고름 상태를 읽어 준다.
 */
@Composable
private fun VaultFilters(counts: List<TierCount>, selected: ShoeTier?, onFilter: (ShoeTier?) -> Unit) {
    val p = settingsPalette()
    val dark = StepUpColors.dark
    val shape = RoundedCornerShape(24.dp)
    BoxWithConstraints(Modifier.fillMaxWidth().padding(horizontal = StepUpDesign.Gutter).padding(top = 4.dp)) {
        val cell = (maxWidth - 8.dp) / counts.size
        Row(
            Modifier.fillMaxWidth().clip(shape)
                .background(if (dark) Color(0xFF0B1628) else Color(0xFFEEF2F8), shape)
                .border(1.dp, if (dark) Color(0xFF1E2C44) else Color(0xFFD8E0ED), shape)
                .horizontalScroll(rememberScrollState())
                .padding(4.dp)
                .testTag("vault-filters"),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            counts.forEachIndexed { index, entry ->
                val chosen = entry.tier == selected
                val label = entry.tier?.label() ?: stringResource(R.string.vault_filter_all)
                val pairs = pluralStringResource(R.plurals.sdv_pairs, entry.count, entry.count)
                val cd = stringResource(R.string.vault_filter_cd, label, pairs)
                if (index > 0) {
                    val quiet = !chosen && counts[index - 1].tier != selected
                    Box(Modifier.size(width = 1.dp, height = 16.dp).background(if (quiet) p.divider else Color.Transparent))
                }
                Box(
                    Modifier.widthIn(min = cell - 1.dp).heightIn(min = 40.dp).clip(shape)
                        .then(if (chosen) Modifier.background(if (dark) Color(0xFF10284A) else Color.White, shape)
                            .border(1.5.dp, Color(0xFF2F86FF), shape) else Modifier)
                        .feedbackClickable(role = Role.Tab, onClick = { onFilter(entry.tier) })
                        .semantics {
                            this.selected = chosen
                            contentDescription = cd
                        }
                        .padding(horizontal = 14.dp)
                        .testTag("vault-filter-${entry.tier?.key ?: "all"}"),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        "$label ${entry.count}", maxLines = 1, fontSize = 14.sp,
                        fontWeight = if (chosen) FontWeight.SemiBold else FontWeight.Medium,
                        color = if (chosen) p.text else p.secondary,
                    )
                }
            }
        }
    }
}

/**
 * 격자 한 칸 — 등급 프레임 속 실제 신발 → 이름 끝 둥근 배지 → Lv · 번호 · 착용 상태.
 * 고른 켤레는 칸 테두리, 신고 있는 켤레는 오른쪽 위 체크(한 켤레에만).
 */
@Composable
private fun VaultCard(shoe: Sneaker, selected: Boolean, onClick: () -> Unit) {
    val p = settingsPalette()
    val dark = StepUpColors.dark
    val shape = RoundedCornerShape(18.dp)
    val state = listOfNotNull(
        stringResource(R.string.sdv_state_viewing).takeIf { selected },
        stringResource(R.string.my_shoes_wearing).takeIf { shoe.equipped },
    ).joinToString(", ")
    val manage = stringResource(R.string.my_shoes_manage)
    Column(
        Modifier.fillMaxWidth().clip(RoundedCornerShape(18.dp))
            .feedbackClickable(role = Role.Button, onClick = onClick)
            .semantics(mergeDescendants = true) {
                this.selected = selected
                if (state.isNotEmpty()) stateDescription = state
                onClick(label = manage) { onClick(); true }
            }
            .testTag("vault-card-${shoe.id}"),
    ) {
        Box(
            Modifier.fillMaxWidth().aspectRatio(GradeArtRatio).clip(shape)
                .background(if (dark) Color(0xFF0A1424) else Color(0xFFE9EEF6), shape)
                .border(
                    if (selected) 2.dp else 1.dp,
                    when {
                        selected -> if (dark) Color(0xFFE9F0FF) else p.text
                        dark -> Color(0xFF17243A)
                        else -> Color(0xFFD5DEEB)
                    },
                    shape,
                ),
            contentAlignment = Alignment.Center,
        ) {
            SneakerGradeStage(shoe, Modifier.fillMaxSize().padding(5.dp))
            if (shoe.equipped) WornCheck(Modifier.align(Alignment.TopEnd).padding(top = 11.dp, end = 11.dp), size = 24.dp)
        }
        Spacer(Modifier.height(9.dp))
        ShoeNameWithBadge(
            name = shoe.shoeName(), tier = shoe.tier,
            style = TextStyle(color = p.text, fontSize = 15.sp, fontWeight = FontWeight.SemiBold, lineHeight = 1.3.em,
                letterSpacing = (-0.01).em),
            modifier = Modifier.fillMaxWidth().padding(horizontal = 2.dp).testTag("vault-name-${shoe.id}"),
        )
        Spacer(Modifier.height(2.dp))
        ShoeMeta(shoe, fontSize = 12.5f, modifier = Modifier.padding(horizontal = 2.dp))
        // 눌림 자리(둥근 18dp)의 아래 모서리 곡선이 마지막 줄 첫 글자("Lv."의 L)를 자르지 않게 — 2026-09-28 기기 캡처
        Spacer(Modifier.height(10.dp))
    }
}

/** 격자 아래 작은 줄 — 도감 · 마켓 · 아이템(스토어 · NFT). 보유 격자에 섞지 않고 따로 들어간다(지우지 않음) */
@Composable
private fun VaultLinks(onOpenDex: () -> Unit, onOpenMarket: () -> Unit, onOpenItems: () -> Unit) {
    val p = settingsPalette()
    Row(Modifier.fillMaxWidth().padding(top = 2.dp), horizontalArrangement = Arrangement.spacedBy(4.dp, Alignment.CenterHorizontally)) {
        listOf(
            Triple(R.string.shoes_s2_dex, onOpenDex, "shoe-open-dex"),
            Triple(R.string.shoes_s2_market, onOpenMarket, "shoe-open-market"),
            Triple(R.string.market_tab_items, onOpenItems, "shoe-open-vault"),
        ).forEach { (label, onClick, tag) ->
            Box(
                Modifier.heightIn(min = StepUpDesign.TouchTarget).clip(RoundedCornerShape(12.dp))
                    .feedbackClickable(role = Role.Button, onClick = onClick).padding(horizontal = 12.dp).testTag(tag),
                contentAlignment = Alignment.Center,
            ) {
                Text(stringResource(label), color = p.secondary, fontSize = 13.5.sp, fontWeight = FontWeight.SemiBold)
            }
        }
    }
}

/** 읽는 중 — 머리 자리 · 빈 칸 넷(예시 신발을 넣지 않는다) */
@Composable
private fun VaultLoading(modifier: Modifier) {
    val p = settingsPalette()
    Column(modifier.fillMaxWidth().padding(horizontal = StepUpDesign.Gutter).padding(top = 14.dp).testTag("vault-loading")) {
        Box(Modifier.size(width = 120.dp, height = 20.dp).clip(RoundedCornerShape(6.dp)).background(p.skeleton))
        Spacer(Modifier.height(14.dp))
        Box(Modifier.fillMaxWidth().height(48.dp).clip(RoundedCornerShape(24.dp)).background(p.skeleton.copy(alpha = 0.6f)))
        Spacer(Modifier.height(16.dp))
        repeat(2) {
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                repeat(2) {
                    Box(Modifier.weight(1f).aspectRatio(GradeArtRatio).clip(RoundedCornerShape(18.dp)).background(p.skeleton.copy(alpha = 0.6f)))
                }
            }
            Spacer(Modifier.height(18.dp))
        }
        Text(stringResource(R.string.sdv_loading), color = p.secondary, fontSize = 15.sp, textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth())
    }
}
