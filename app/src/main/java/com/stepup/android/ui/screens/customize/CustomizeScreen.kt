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
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.layout
import com.stepup.android.ui.screens.items.plainCardEdge
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
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
import com.stepup.android.ui.components.StepUpUi
import com.stepup.android.ui.components.label
import com.stepup.android.ui.components.preloadShoeArt
import com.stepup.android.ui.components.runTextStyle
import com.stepup.android.ui.components.runTone
import com.stepup.android.ui.experience.feedbackClickable
import com.stepup.android.ui.guide.GuideTour
import com.stepup.android.ui.guide.guideTarget
import com.stepup.android.ui.screens.items.ItemSort
import com.stepup.android.ui.screens.items.ItemsViewModel
import com.stepup.android.ui.screens.items.OwnedLoad
import com.stepup.android.ui.screens.items.RepairPhase
import com.stepup.android.ui.screens.items.ShoeCareStore
import com.stepup.android.ui.screens.items.ShoeRepairSheet
import com.stepup.android.ui.screens.items.ShoeStatePanel
import com.stepup.android.ui.screens.items.holds
import com.stepup.android.ui.screens.items.itemSortRes
import com.stepup.android.ui.screens.items.repairable
import com.stepup.android.ui.screens.items.shoeName
import com.stepup.android.ui.theme.StepUpColors
import com.stepup.android.ui.theme.StepUpDesign
import kotlinx.coroutines.launch

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
    onOpenMarketModel: (com.stepup.android.data.remote.ModelKey) -> Unit = {},
    onChangeBackground: () -> Unit = {},
    onOpenWallet: () -> Unit = {},
    onOpenMarket: () -> Unit = {},
    onOpenVault: () -> Unit = {},
    onOpenSneaker: (Long) -> Unit = {},
    onOpenDraw: () -> Unit = {},
    /** 뽑기 결과의 "내 신발 보기" — 받은 신발을 고른 채로 내 신발을 연다(미리 보기일 뿐, 착용은 바꾸지 않는다) */
    focusShoeId: Long? = null,
    onFocusShoeShown: () -> Unit = {},
    /** 뽑기 화면의 글자 탭에서 온 칸(내 신발 · 보관함) — 한 번 고르면 비운다 */
    openSection: ShoeSection? = null,
    onOpenSectionShown: () -> Unit = {},
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
    LaunchedEffect(openSection) {
        if (openSection != null && openSection != ShoeSection.DRAW) {
            section = openSection
            onOpenSectionShown()
        }
    }
    Column(Modifier.fillMaxSize()) {
        // 뽑기는 제 길이라 고르면 그 화면으로 간다(이 화면의 칸은 그대로)
        S2ShoesSections(section, onSelect = { if (it == ShoeSection.DRAW) onOpenDraw() else section = it })
        when (section) {
            ShoeSection.MINE -> MyShoesContent(
                load = owned, selectedId = selectedId, onSelect = { selectedId = it },
                onOpenSneaker = onOpenSneaker, onOpenDraw = onOpenDraw, onReload = viewModel::reloadOwned,
                modifier = Modifier.weight(1f), onOpenVault = { section = ShoeSection.VAULT },
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
            ShoeSection.DRAW -> Unit
        }
    }
}

/**
 * 신발 탭 글자 색 — 러닝 리메이크(2026-10-02)의 신발 색감(별첨 shoes-blue.png): 러닝 화면과 같은 남색 팔레트의 글자 · 강조.
 * 색만 바꾼다 — 배치 · 정렬 · 거르기 · 보관함 · 착용 구분은 그대로.
 */
private class ShoeInk(val text: Color, val secondary: Color, val accent: Color, val divider: Color, val skeleton: Color)

@Composable
private fun shoeInk(): ShoeInk {
    val t = runTone()
    return ShoeInk(text = t.text, secondary = t.label, accent = t.cyan, divider = t.divider, skeleton = t.track)
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
    /** 신발 보관함 탭으로 — "보유 신발 N켤레 ›" 줄(2026-11-24 재디자인) */
    onOpenVault: () -> Unit = {},
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
        load is OwnedLoad.Ready -> MyShoesReady(load.shoes, selectedId, onSelect, onOpenSneaker, onOpenVault, modifier, initialBasis)
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
    onOpenVault: () -> Unit,
    modifier: Modifier,
    initialBasis: Boolean,
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    // 수리 시트를 연 켤레(소유 id) — 신발 상세를 거치지 않고 이 화면에서 바로(출발 전 점검 · 러닝 결과의 수리와 같은 길)
    var repairId by rememberSaveable { mutableStateOf<Long?>(null) }
    // 결과를 아직 모르는 강화 요청 — 이 신발이 대상 · 재료면 수리를 막는다(신발 상세의 내구도 칸 수리하기와 같은 조건)
    val forgePending by remember {
        runCatching { com.stepup.android.core.ServiceLocator.forgeRepository.pendingFlow }
            .getOrElse { kotlinx.coroutines.flow.flowOf(null) }
    }.collectAsStateWithLifecycle(initialValue = null)
    val row = remember(shoes) { ownedRow(shoes) }
    val selected = resolvePair(row, selectedId) ?: return
    // 보이는 켤레 — 새로 고른 켤레의 그림을 먼저 읽은 뒤 이름 · 능력치와 함께 바꾼다(글만 앞서 바뀌지 않게)
    var shownId by rememberSaveable { mutableStateOf<Long?>(null) }
    LaunchedEffect(selected.id) {
        if (shownId != null && shownId != selected.id) preloadShoeArt(context, selected)
        shownId = selected.id
    }
    val shown = shoes.firstOrNull { it.id == shownId } ?: selected
    var basisOpen by rememberSaveable { mutableStateOf(initialBasis) }
    var renaming by rememberSaveable { mutableStateOf(false) }
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
    val rules = com.stepup.android.ui.screens.items.rememberShoeRules()
    // 내구도가 줄었으면 주 버튼이 수리하기(그 화면에서 수리 시트), 상세 보기는 능력치 줄의 글자 버튼. 결과 미확인 강화가 붙잡은 신발은 수리하지 않는다
    val repairFirst = repairable(shown) && !forgePending.holds(shown.id)
    val openRepair = {
        repairId = shown.id
        ShoeCareStore.shared.openRepair(shown.id)
    }
    val manage = stringResource(R.string.my_shoes_manage)
    Column(modifier.fillMaxWidth()) {
        BoxWithConstraints(Modifier.fillMaxWidth().weight(1f)) {
            val tight = maxHeight < 600.dp
            val artHeight = if (tight) 100.dp else 120.dp
            Column(
                Modifier.fillMaxSize().verticalScroll(rememberScrollState()).testTag("my-shoes").padding(horizontal = StepUpUi.Gutter),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Spacer(Modifier.height(if (tight) 6.dp else 14.dp))
                // 신발 그림 — 상자 없이 바탕 위 가운데, 높이만 고정. 누르면 이 켤레의 관리(신기 · 강화 · 수리 · 판매 — 신발 상세)
                val art = com.stepup.android.ui.components.sneakerArtRes(shown)
                Box(
                    Modifier.fillMaxWidth().height(artHeight).clip(RoundedCornerShape(16.dp))
                        .feedbackClickable(role = Role.Button) { onOpenSneaker(selected.id) }
                        .semantics { onClick(label = manage) { onOpenSneaker(selected.id); true } }
                        .guideTarget(GuideTour.Targets.CUSTOMIZE_PREVIEW).testTag("shoe-hero"),
                    contentAlignment = Alignment.Center,
                ) {
                    if (art != null) {
                        com.stepup.android.ui.components.ProductImage(art, height = artHeight, contentDescription = shown.shoeName())
                    } else {
                        Icon(StepUpIcons.Shoe, contentDescription = null, tint = runTone().muted, modifier = Modifier.size(56.dp))
                    }
                }
                Spacer(Modifier.height(if (tight) 6.dp else 10.dp))
                NameLine(shown, onRename = { renaming = true })
                Spacer(Modifier.height(if (tight) 12.dp else 16.dp))
                com.stepup.android.ui.screens.items.ShoeDurabilityCard(
                    com.stepup.android.ui.screens.items.durabilityView(shown, rules),
                    numberTag = "shoe-bar-durability-value", barTag = "shoe-bar-durability",
                )
                Spacer(Modifier.height(10.dp))
                StatsLine(shown, onExplain = { basisOpen = true }, onDetail = if (repairFirst) ({ onOpenSneaker(selected.id) }) else null)
                Spacer(Modifier.height(10.dp))
                OwnedCard(row, compact = tight, selectedId = selected.id, state = rowState, onPick = onSelect, onOpenVault = onOpenVault)
                Spacer(Modifier.height(12.dp))
            }
        }
        // 주 버튼 하나 — 가득이면 상세 보기, 줄었으면 수리하기(2026-11-07 "신발 수리하는거 버튼 앞으로")
        Box(Modifier.fillMaxWidth().padding(horizontal = StepUpUi.Gutter).padding(top = 8.dp, bottom = 12.dp)) {
            if (repairFirst) {
                com.stepup.android.ui.components.RunButton(
                    stringResource(R.string.care_repair), openRepair, Modifier.fillMaxWidth().testTag("shoe-repair-quick"),
                )
            } else {
                com.stepup.android.ui.components.RunButton(
                    stringResource(R.string.shoes_open_detail), { onOpenSneaker(selected.id) }, Modifier.fillMaxWidth().testTag("shoe-manage"),
                )
            }
        }
    }
    if (basisOpen) StatBasisSheet(onClose = { basisOpen = false })
    if (renaming) com.stepup.android.ui.screens.items.ShoeRenameSheet(shown, onDone = { renaming = false })
    // 수리 시트 — 신발 상세 · 출발 전 점검과 같은 수리 길([ShoeCareStore]): 최신 내구도 · 비용 · 잔고를 읽고, 확인을 눌렀을 때만 서버에 한 번 보낸다
    repairId?.let { id ->
        val store = ShoeCareStore.shared
        val repairs by store.repair.collectAsStateWithLifecycle()
        val repairShoe = shoes.firstOrNull { it.id == id }
        // 화면이 다시 만들어졌는데(프로세스 재시작) 열려 있던 수리 시트의 상태가 없으면 — 조회부터 다시(읽기만)
        LaunchedEffect(id) {
            if (store.repair.value[id] == null) store.openRepair(id)
        }
        val closeRepair: () -> Unit = {
            repairId = null
            store.closeRepair(id)
        }
        if (repairShoe != null) {
            ShoeRepairSheet(
                shoe = repairShoe,
                phase = repairs[id] ?: RepairPhase.Loading,
                onConfirm = { store.confirmRepair(id) },
                onRecheck = { store.recheckRepair(id) },
                onReload = { store.reloadRepair(id) },
                // 로그인 화면으로 — 돌아온 뒤 저절로 수리하지 않는다. 서버 설정이 없는 빌드는 로그인할 수 없다(버튼 없음)
                onSignIn = if (!com.stepup.android.core.ServiceLocator.serverEconomyOn) null else ({
                    scope.launch { runCatching { com.stepup.android.ui.components.returnToSignIn(context) } }
                }),
                onClose = closeRepair,
            )
        } else {
            // 신발이 목록에서 사라졌다(판매 · 강화 재료) — 닫는다
            LaunchedEffect(Unit) { closeRepair() }
        }
    }
}

/**
 * 가운데 이름(30 · 굵게, 길면 두 줄까지 글자를 줄여 다 보인다) · 오른쪽 끝 작은 연필(이 폰에서만 이름 바꾸기, 2026-10-17) →
 * "일반 · Lv.6 · 착용 중". 가득일 때 달리는 분은 내구도 칸에, 별 · 유형과 신발 번호는 신발 상세에 있다.
 */
@Composable
private fun NameLine(shoe: Sneaker, onRename: () -> Unit) {
    val t = runTone()
    Box(Modifier.fillMaxWidth()) {
        androidx.compose.foundation.text.BasicText(
            shoe.shoeName(),
            style = TextStyle(
                fontFamily = com.stepup.android.ui.theme.StepUpSans, color = t.text, fontSize = 30.sp, fontWeight = FontWeight.Bold,
                lineHeight = 1.2.em, letterSpacing = (-0.025).em, textAlign = TextAlign.Center,
            ),
            maxLines = 2,
            autoSize = androidx.compose.foundation.text.TextAutoSize.StepBased(minFontSize = 16.sp, maxFontSize = 30.sp, stepSize = 1.sp),
            modifier = Modifier.fillMaxWidth().padding(horizontal = 44.dp).semantics { heading() }.testTag("shoe-hero-name"),
        )
        val renameCd = stringResource(R.string.home_shoe_rename)
        // 연필은 이름 줄의 높이를 늘리지 않는다(시안 14 — 이름과 아래 줄 사이가 벌어지지 않게) — 누르는 자리 44 는 그대로
        Box(
            Modifier.align(Alignment.CenterEnd)
                .layout { measurable, constraints ->
                    val placeable = measurable.measure(constraints.copy(minHeight = 0))
                    layout(placeable.width, 0) { placeable.place(0, -placeable.height / 2) }
                }
                .size(44.dp).clip(CircleShape)
                .feedbackClickable(role = Role.Button, onClick = onRename)
                .semantics { contentDescription = renameCd }.testTag("shoe-rename"),
            contentAlignment = Alignment.Center,
        ) {
            Icon(Icons.Outlined.Edit, contentDescription = null, tint = t.muted, modifier = Modifier.size(18.dp))
        }
    }
    Spacer(Modifier.height(2.dp))
    val meta = listOfNotNull(shoe.tier.label(), "Lv.${shoe.level}", stringResource(R.string.my_shoes_wearing).takeIf { shoe.equipped })
        .joinToString(" · ")
    Text(
        meta, style = runTextStyle(15.sp, t.label, FontWeight.Medium, 1.3f), textAlign = TextAlign.Center, maxLines = 1,
        overflow = TextOverflow.Ellipsis, modifier = Modifier.fillMaxWidth().testTag("shoe-card-tag"),
    )
}

/**
 * "효율 140 · 착화감 140" 한 줄(흰 카드) — 줄을 누르면 숫자 기준 설명. [onDetail] 이 있으면(주 버튼이 수리하기일 때) 오른쪽에 "상세 보기" 글자 버튼.
 * 효율 · 착화감은 지수(100 = 기준, 2배면 2배 — 경제 4판).
 */
@Composable
private fun StatsLine(shoe: Sneaker, onExplain: () -> Unit, onDetail: (() -> Unit)?) {
    val t = runTone()
    val explain = stringResource(R.string.my_shoes_basis_open)
    val effLabel = stringResource(R.string.my_shoes_stat_efficiency)
    val comLabel = stringResource(R.string.sdv_stat_comfort)
    val eff = com.stepup.android.ui.screens.items.formatIndex(com.stepup.android.ui.screens.items.efficiencyIndex(shoe))
    val comfort = com.stepup.android.ui.screens.items.formatIndex(com.stepup.android.ui.screens.items.comfortIndex(shoe))
    Row(
        Modifier.fillMaxWidth().heightIn(min = 56.dp).clip(RoundedCornerShape(22.dp)).background(t.panel)
            .then(Modifier.plainCardEdge())
            .feedbackClickable(role = Role.Button, onClick = onExplain)
            .semantics { onClick(label = explain) { onExplain(); true } }
            .testTag("shoe-stats")
            .padding(start = 18.dp, end = if (onDetail != null) 4.dp else 18.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            buildAnnotatedString {
                append("$effLabel ")
                withStyle(SpanStyle(fontWeight = FontWeight.ExtraBold)) { append(eff) }
                append(" · $comLabel ")
                withStyle(SpanStyle(fontWeight = FontWeight.ExtraBold)) { append(comfort) }
            },
            style = runTextStyle(17.5.sp, t.text, FontWeight.SemiBold, 1.3f).copy(fontFeatureSettings = "tnum"),
            maxLines = 2, modifier = Modifier.weight(1f).padding(vertical = 12.dp).testTag("shoe-stats-line"),
        )
        if (onDetail != null) {
            com.stepup.android.ui.components.RunTextAction(
                stringResource(R.string.shoes_open_detail), onDetail, modifier = Modifier.testTag("shoe-manage"),
            )
        }
    }
}

/**
 * "보유 신발 N켤레 ›"(흰 카드 한 줄 — 누르면 신발 보관함). 두 켤레 이상이면 그 아래 가로 목록 — 켤레마다 한 칸(작은 프레임).
 * 고른 칸은 바깥 테두리, 신고 있는 켤레는 작은 체크 — 서로 다른 표시. 칸을 누르면 보는 신발만 바뀐다(착용은 그대로).
 */
@Composable
private fun OwnedCard(
    row: List<Sneaker>,
    compact: Boolean,
    selectedId: Long,
    state: LazyListState,
    onPick: (Long) -> Unit,
    onOpenVault: () -> Unit,
) {
    val t = runTone()
    Column(
        Modifier.fillMaxWidth().clip(RoundedCornerShape(22.dp)).background(t.panel)
            .then(Modifier.plainCardEdge()),
    ) {
        Row(
            Modifier.fillMaxWidth().heightIn(min = 56.dp)
                .feedbackClickable(role = Role.Button, onClick = onOpenVault)
                .semantics(mergeDescendants = true) {}
                .testTag("shoe-owned-head")
                .padding(start = 18.dp, end = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                buildAnnotatedString {
                    append(stringResource(R.string.my_shoes_owned) + " ")
                    withStyle(SpanStyle(fontWeight = FontWeight.ExtraBold)) {
                        append(pluralStringResource(R.plurals.sdv_pairs, row.size, row.size))
                    }
                },
                style = runTextStyle(17.sp, t.text, FontWeight.SemiBold, 1.3f).copy(fontFeatureSettings = "tnum"),
                maxLines = 1, overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f).semantics { heading() }.testTag("shoe-owned-count"),
            )
            Icon(Icons.Filled.ChevronRight, contentDescription = null, tint = t.muted, modifier = Modifier.size(22.dp))
        }
        if (row.size > 1) {
            LazyRow(
                state = state,
                modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp).testTag("shoe-owned-row"),
                contentPadding = PaddingValues(horizontal = 14.dp - ThumbRing),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                items(row, key = { it.id }) { shoe ->
                    OwnedThumb(shoe, height = if (compact) CompactThumbHeight else ThumbHeight, selected = shoe.id == selectedId,
                        onClick = { onPick(shoe.id) })
                }
            }
        }
    }
}

/** "Lv. 1 · #0001 · 착용 중" — 착용 중은 실제로 신고 있는 켤레에만, 강조색으로 */
@Composable
private fun ShoeMeta(shoe: Sneaker, fontSize: Float, modifier: Modifier = Modifier) {
    val p = shoeInk()
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

/** 칸 테두리 자리 — 고른 칸의 바깥 선이 이 안에 그려져 이웃 칸과 겹치지 않는다 */
private val ThumbRing = 4.dp
private val ThumbHeight = 60.dp
private val CompactThumbHeight = 52.dp

@Composable
private fun OwnedThumb(shoe: Sneaker, height: Dp, selected: Boolean, onClick: () -> Unit) {
    val p = shoeInk()
    val name = shoe.shoeName()
    val levelNo = stringResource(R.string.sdv_level_no_cd, shoe.level, shoe.mintNumber)
    val state = listOfNotNull(
        stringResource(R.string.sdv_state_viewing).takeIf { selected },
        stringResource(R.string.my_shoes_wearing).takeIf { shoe.equipped },
    ).joinToString(", ")
    val tierName = shoe.tier.label()
    // 고른 칸 — 파란 테두리(신발 색감 시안의 고른 카드)
    val ring = if (StepUpColors.dark) Color(0xFF3B7BFF) else runTone().cobalt
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
        modifier.size(size).clip(CircleShape).background(Color(0xFF1F66FF))
            .border(1.5.dp, if (dark) runTone().screen else Color.White, CircleShape)
            .testTag("shoe-worn-badge"),
        contentAlignment = Alignment.Center,
    ) {
        Icon(Icons.Filled.Check, contentDescription = null, tint = Color.White, modifier = Modifier.size(size * 0.66f))
    }
}

/** 읽는 중 — 이름 자리 · 빈 무대 · "신발을 불러오고 있어요." (임시 값 · 예시 신발을 보이지 않는다) */
@Composable
private fun MyShoesLoading(modifier: Modifier) {
    val p = shoeInk()
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

/**
 * 숫자 기준(경제 4판, 0116) — 효율 · 착화감은 100 이 기준이고 2배면 2배, 막대 끝은 그 등급에서 가장 잘 뽑힌 새 신발, 내구도는 달리는 분,
 * 별 · 유형은 뽑힌 값. 효율은 금액이 아니라는 것(실제 SUP 는 러닝 뒤 확정)
 */
@Composable
private fun StatBasisSheet(onClose: () -> Unit) {
    val p = shoeInk()
    val rules = com.stepup.android.ui.screens.items.rememberShoeRules()
    val newMeaning = com.stepup.android.ui.screens.items.newStatMeaning(rules)
    SettingsSheet(
        title = stringResource(R.string.my_shoes_basis_open), onDismiss = onClose,
        modifier = Modifier.testTag("shoe-basis-sheet"),
    ) {
        listOf(
            stringResource(R.string.v4s_basis_intro),
            // 서버 0119 의 능력치 뜻(효율 포인트는 신발 · 에너지도 빨리 쓴다 · 내구도 포인트는 가득 수리비를 올리지 않는다) — 예전 서버면 예전 글
            stringResource(if (newMeaning) R.string.ers_basis_efficiency else R.string.v4s_basis_efficiency),
            stringResource(R.string.v4s_basis_comfort, com.stepup.android.ui.screens.items.comfortCapPercent(rules)),
            stringResource(if (newMeaning) R.string.ers_basis_durability else R.string.v4s_basis_durability),
            stringResource(R.string.v4s_basis_quality),
        ).forEach { line ->
            Text(line, color = p.text, fontSize = 15.sp, lineHeight = 1.45.em)
        }
        Text(stringResource(R.string.v4s_basis_note), color = p.secondary, fontSize = 14.sp, lineHeight = 1.45.em,
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
    val p = shoeInk()
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
    val p = shoeInk()
    val dark = StepUpColors.dark
    val shape = RoundedCornerShape(24.dp)
    BoxWithConstraints(Modifier.fillMaxWidth().padding(horizontal = StepUpDesign.Gutter).padding(top = 4.dp)) {
        val cell = (maxWidth - 8.dp) / counts.size
        Row(
            Modifier.fillMaxWidth().clip(shape)
                .background(runTone().inset, shape)
                .border(1.dp, runTone().panelEdge, shape)
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
                        .then(if (chosen) Modifier.background(
                            androidx.compose.ui.graphics.Brush.horizontalGradient(listOf(Color(0xFF0754FF), Color(0xFF2A73FF))), shape,
                        ).border(1.dp, Color(0xFF4D8BFF), shape) else Modifier)
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
                        color = if (chosen) Color.White else p.secondary,
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
    val p = shoeInk()
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
                .background(androidx.compose.ui.graphics.Brush.verticalGradient(listOf(runTone().panelTop, runTone().panel)), shape)
                .border(
                    if (selected) 2.dp else 1.dp,
                    when {
                        selected -> if (dark) Color(0xFF3B7BFF) else runTone().cobalt
                        else -> runTone().panelEdge
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
    val p = shoeInk()
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
    val p = shoeInk()
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
