package com.stepup.android.ui.screens.customize

import androidx.compose.ui.draw.alpha

import androidx.compose.ui.semantics.text

import androidx.compose.ui.semantics.clearAndSetSemantics

import com.stepup.android.ui.components.RimKind

import com.stepup.android.ui.components.stepUpRim

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
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.MoreHoriz
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.outlined.Build
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.Info
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
import com.stepup.android.ui.components.BottomActionBar
import com.stepup.android.ui.components.CircleIconButton
import com.stepup.android.ui.components.PrimaryButton
import com.stepup.android.ui.components.SecondaryButton
import com.stepup.android.ui.components.ShoeCard
import com.stepup.android.ui.components.StatItem
import com.stepup.android.ui.components.StatRow
import com.stepup.android.ui.components.StepUpUi
import com.stepup.android.ui.components.label
import com.stepup.android.ui.components.preloadShoeArt
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
import com.stepup.android.ui.screens.items.formatDurability
import com.stepup.android.ui.screens.items.holds
import com.stepup.android.ui.screens.items.itemSortRes
import com.stepup.android.ui.screens.items.repairUrgent
import com.stepup.android.ui.screens.items.repairable
import com.stepup.android.ui.screens.items.shoeName
import com.stepup.android.ui.theme.StepUpColors
import com.stepup.android.ui.theme.StepUpDesign
import kotlin.math.roundToInt
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
            // 보관함은 내 신발로 합쳤다(13차)
            section = ShoeSection.MINE
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
    /**
     * 신발 보관함 탭으로 — 2026-11-07 부터 아래 보조 버튼은 수리하기다(사용자 "신발 수리하는거 버튼 앞으로").
     * 보관함은 위 글자 탭(내 신발 · 신발 보관함)으로 그대로 간다. 부르는 쪽이 넘기던 값은 받아 두기만 한다
     */
    @Suppress("UNUSED_PARAMETER") onOpenVault: () -> Unit = {},
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
    val shown = selected
    var basisOpen by rememberSaveable { mutableStateOf(initialBasis) }
    var renaming by rememberSaveable { mutableStateOf(false) }
    var sort by rememberSaveable { mutableStateOf(ItemSort.RECENT) }
    var filterKey by rememberSaveable { mutableStateOf<String?>(null) }
    var sortOpen by rememberSaveable { mutableStateOf(false) }
    val rules = com.stepup.android.ui.screens.items.rememberShoeRules()
    val c = StepUpUi.colors
    val worn = shoes.firstOrNull { it.equipped }
    val filter = activeFilter(shoes, filterKey)
    val grid = remember(shoes, filter, sort) { vaultShown(shoes, filter, sort) }
    // 13차(2026-11-26 S-345 · S-346) — 넘기기 없이 한 화면: 위 "✓ 신고 있는 신발 … 신발 상세 ›" + 카드 한 줄, 아래 "보유 신발 … 정렬 ⌄" + 등급 칩 +
    // 보유 신발 전부 4칸 격자. 칸을 누르면 그 신발 상세(안 신은 신발이면 주 버튼 "이 신발 신기")
    Column(modifier.fillMaxWidth().verticalScroll(rememberScrollState()).testTag("my-shoes")) {
        Spacer(Modifier.height(14.dp))
        if (worn != null) {
            Row(Modifier.fillMaxWidth().padding(horizontal = StepUpUi.Gutter), verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Filled.Check, null, tint = c.label, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(4.dp))
                Text(stringResource(R.string.v13_shoes_wearing_now), color = c.label, fontSize = 17.sp, fontWeight = FontWeight.Bold,
                    modifier = Modifier.weight(1f).testTag("shoe-tab-wearing"))
                Text(
                    stringResource(R.string.v12_shoe_detail) + " ›", color = c.accentText, fontSize = 15.sp, fontWeight = FontWeight.Bold,
                    modifier = Modifier.clip(RoundedCornerShape(8.dp)).feedbackClickable(role = Role.Button) { onOpenSneaker(worn.id) }
                        .padding(6.dp).testTag("shoe-manage"),
                )
            }
            Spacer(Modifier.height(10.dp))
            WornShoeCard(worn, rules, canRepair = repairable(worn) && !forgePending.holds(worn.id),
                onOpen = { onOpenSneaker(worn.id) },
                onRepair = {
                    repairId = worn.id
                    ShoeCareStore.shared.openRepair(worn.id)
                })
            Spacer(Modifier.height(26.dp))
        }
        Row(Modifier.fillMaxWidth().padding(start = StepUpUi.Gutter, end = 8.dp), verticalAlignment = Alignment.CenterVertically) {
            Text(stringResource(R.string.my_shoes_owned), color = c.label, fontSize = 17.sp, fontWeight = FontWeight.Bold,
                modifier = Modifier.weight(1f).semantics { heading() }.testTag("shoe-owned-head"))
            Row(
                Modifier.heightIn(min = 40.dp).clip(RoundedCornerShape(10.dp)).feedbackClickable(role = Role.Button) { sortOpen = true }
                    .padding(horizontal = 10.dp).testTag("vault-sort"),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(stringResource(itemSortRes(sort)), color = c.secondaryLabel, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
                Icon(Icons.Filled.KeyboardArrowDown, null, tint = c.secondaryLabel, modifier = Modifier.size(20.dp))
            }
        }
        // 등급 칩 — 전체 + 가진 갈래(실제 수)
        Row(
            Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()).padding(horizontal = StepUpUi.Gutter, vertical = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            tierCounts(shoes).forEach { entry ->
                val on = entry.tier == filter
                val label = (entry.tier?.label() ?: stringResource(R.string.vault_filter_all)) + " " + entry.count
                Text(
                    label, color = if (on) Color.White else c.secondaryLabel, fontSize = 15.sp,
                    fontWeight = if (on) FontWeight.Bold else FontWeight.SemiBold,
                    modifier = Modifier.clip(RoundedCornerShape(50)).background(if (on) c.primary else c.fill)
                        .feedbackClickable(role = Role.Tab) { filterKey = entry.tier?.key }
                        .semantics { this.selected = on }
                        .padding(horizontal = 16.dp, vertical = 9.dp).testTag("vault-filter-${entry.tier?.key ?: "all"}"),
                )
            }
        }
        // 4칸 격자 — 등급은 테두리 색, 왼쪽 위 Lv, 아래 이름 · 내구도 막대 + 남은 분. 30% 아래 주황, 0분은 흐리게 + "닳음". 신은 신발은 블루 테두리 + 체크
        Column(Modifier.fillMaxWidth().padding(horizontal = StepUpUi.Gutter).testTag("shoe-owned-row"),
            verticalArrangement = Arrangement.spacedBy(14.dp)) {
            grid.chunked(4).forEach { line ->
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    line.forEach { shoe ->
                        GridShoe(shoe, rules, selected = shoe.id == selectedId, modifier = Modifier.weight(1f)) {
                            onSelect(shoe.id)
                            onOpenSneaker(shoe.id)
                        }
                    }
                    repeat(4 - line.size) { Spacer(Modifier.weight(1f)) }
                }
            }
        }
        Spacer(Modifier.height(24.dp))
    }
    if (sortOpen) {
        com.stepup.android.ui.components.RunSheet(onDismiss = { sortOpen = false }, modifier = Modifier.testTag("vault-sort-sheet")) {
            Text(stringResource(R.string.v13_sort_title), color = c.label, fontSize = 22.sp, fontWeight = FontWeight.ExtraBold)
            Spacer(Modifier.height(10.dp))
            VaultSorts.forEach { key ->
                val on = key == sort
                Row(
                    Modifier.fillMaxWidth().heightIn(min = 52.dp).clip(RoundedCornerShape(10.dp))
                        .feedbackClickable(role = Role.RadioButton) { sort = key; sortOpen = false }
                        .semantics { this.selected = on }.testTag("vault-sort-$key"),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(stringResource(itemSortRes(key)), color = if (on) c.accentText else c.label, fontSize = 16.sp,
                        fontWeight = if (on) FontWeight.Bold else FontWeight.Medium, modifier = Modifier.weight(1f))
                    if (on) Icon(Icons.Filled.Check, null, tint = c.accentText, modifier = Modifier.size(20.dp))
                }
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
 * 신발 카드 · 그 아래 — 아래(이름 · 숫자 줄 · 보유 줄)를 먼저 재고, 카드는 [available] 에서 남는 높이만큼만 비율(361 : 236) 그대로 줄여
 * 가운데 둔다(최소 112, 원래 크기까지). 짧은 화면(411 × 731 등)에서도 넘기지 않고 한 화면에 — 그래도 모자라면(큰 글씨) 그때만 넘긴다.
 */
@Composable
private fun CardThenRest(available: Dp, card: @Composable () -> Unit, rest: @Composable androidx.compose.foundation.layout.ColumnScope.() -> Unit) {
    Layout(contents = listOf(card, { Column(Modifier.fillMaxWidth(), content = rest) })) { (cardM, restM), constraints ->
        val restP = restM.first().measure(constraints.copy(minHeight = 0, maxHeight = Constraints.Infinity))
        val pad = StepUpUi.Gutter.roundToPx()
        val ratio = 361f / 236f
        val natural = ((constraints.maxWidth - 2 * pad) / ratio).roundToInt()
        val h = (available.roundToPx() - restP.height).coerceIn(minOf(112.dp.roundToPx(), natural), natural)
        val w = ((h * ratio).roundToInt() + 2 * pad).coerceAtMost(constraints.maxWidth)
        val cardP = cardM.first().measure(Constraints.fixed(w, h))
        layout(constraints.maxWidth, cardP.height + restP.height) {
            cardP.place((constraints.maxWidth - cardP.width) / 2, 0)
            restP.place(0, cardP.height)
        }
    }
}

/** 카드 왼쪽 위 — "착용 중 · 레어"(신고 있는 켤레) · "레어"(아닌 켤레) */
@Composable
private fun shoeCardTag(shoe: Sneaker): String {
    val tier = shoe.tier.label()
    return if (shoe.equipped) stringResource(R.string.my_shoes_wearing) + " · " + tier else tier
}

/**
 * 이름(시안 30 · 아주 굵게, 길면 두 줄까지 글자를 줄여 다 보인다) · 끝의 연필(이 폰에서만 이름 바꾸기, 2026-10-17) →
 * 아래 남은 / 최대 내구도. 번호 · 레벨 · 등급은 카드에 있어 다시 쓰지 않는다.
 */
@Composable
private fun NameBlock(shoe: Sneaker, onRename: () -> Unit) {
    val c = StepUpUi.colors
    Row(Modifier.fillMaxWidth().padding(start = 20.dp, end = 8.dp), verticalAlignment = Alignment.CenterVertically) {
        Column(Modifier.weight(1f)) {
            androidx.compose.foundation.text.BasicText(
                shoe.shoeName(),
                style = StepUpUi.type.largeTitle.copy(color = c.label, fontSize = 30.sp, lineHeight = 1.2.em, letterSpacing = (-0.045).em),
                maxLines = 2,
                autoSize = androidx.compose.foundation.text.TextAutoSize.StepBased(minFontSize = 16.sp, maxFontSize = 30.sp, stepSize = 1.sp),
                modifier = Modifier.fillMaxWidth().semantics { heading() }.testTag("shoe-hero-name"),
            )
            // 가득 찼을 때 달리는 시간(별 · 유형은 뺐다 — 2026-11-24 사용자). 가득 값을 모르는 신발은 남은 내구도만
            val rules = com.stepup.android.ui.screens.items.rememberShoeRules()
            val time = if (shoe.server?.hasDurabilityMax == true) {
                stringResource(R.string.v4s_full_minutes,
                    com.stepup.android.ui.screens.items.wholeMinutes(shoe, com.stepup.android.ui.screens.items.durabilityMax(shoe), rules))
            } else {
                stringResource(R.string.shoes_durability_left, formatDurability(shoe))
            }
            Text(
                time,
                style = StepUpUi.type.rowSubtitle.copy(fontSize = 14.sp), color = c.secondaryLabel, maxLines = 1,
                modifier = Modifier.padding(top = 4.dp).testTag("shoe-hero-meta"),
            )
        }
        CircleIconButton(
            Icons.Outlined.Edit, stringResource(R.string.home_shoe_rename), onClick = onRename,
            modifier = Modifier.testTag("shoe-rename"),
        )
    }
}

/**
 * 숫자 셋 — 내구도(%) · 효율 · 착화감(시안 shoes 숫자 줄, 상자 없음). 시안의 민팅 횟수 · 착용 거리는 앱에 없는 값이라 쓰지 않는다.
 * 줄 전체가 숫자 기준 설명을 연다.
 */
@Composable
private fun StatsBlock(shoe: Sneaker, onExplain: () -> Unit) {
    val explain = stringResource(R.string.my_shoes_basis_open)
    // 경제 4판(0116) — 내구도는 남은 분, 효율 · 착화감은 지수(100 = 기준, 2배면 2배). 최대 내구도를 모르는 예전 신발은 남은 비율
    val minutes = shoe.server?.hasDurabilityMax == true
    // 분 = 내구도 ÷ (5 × 효율 포인트 배수) — 효율에 포인트를 넣은 신발은 그만큼 빨리 닳는다(서버 0119)
    val rules = com.stepup.android.ui.screens.items.rememberShoeRules()
    StatRow(
        listOf(
            if (minutes) {
                StatItem(stringResource(R.string.sdv_stat_durability),
                    "${com.stepup.android.ui.screens.items.wholeMinutes(shoe, com.stepup.android.ui.screens.items.durabilityLeft(shoe), rules)}",
                    stringResource(R.string.v4s_unit_min), tag = "shoe-bar-durability-value")
            } else {
                StatItem(stringResource(R.string.sdv_stat_durability), "${com.stepup.android.ui.screens.items.durabilityPercent(shoe)}", "%",
                    tag = "shoe-bar-durability-value")
            },
            StatItem(stringResource(R.string.my_shoes_stat_efficiency),
                com.stepup.android.ui.screens.items.formatIndex(com.stepup.android.ui.screens.items.efficiencyIndex(shoe)),
                tag = "shoe-bar-bonus-value"),
            StatItem(stringResource(R.string.sdv_stat_comfort),
                com.stepup.android.ui.screens.items.formatIndex(com.stepup.android.ui.screens.items.comfortIndex(shoe)),
                tag = "shoe-bar-energy-value"),
        ),
        numberStyle = StepUpUi.type.numberStat.copy(fontSize = 36.sp),
        modifier = Modifier.padding(horizontal = 14.dp).clip(RoundedCornerShape(12.dp))
            .feedbackClickable(role = Role.Button, onClick = onExplain)
            .semantics { onClick(label = explain) { onExplain(); true } }
            .padding(horizontal = 6.dp, vertical = 4.dp)
            .testTag("shoe-stats"),
    )
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

/** 보유 신발 N → 켤레마다 한 칸(작은 프레임). 고른 칸은 바깥 테두리, 신고 있는 켤레는 작은 체크 — 서로 다른 표시 */
@Composable
private fun OwnedBlock(row: List<Sneaker>, compact: Boolean, selectedId: Long, state: LazyListState, onPick: (Long) -> Unit) {
    val p = shoeInk()
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
            // 도감 보기 · 마켓 · 아이템 줄은 뺐다(2026-11-24 사용자 "다 없애고")
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

/** 카드 넘기기 화살표 — 흰 원(그림자) 위 꺾쇠. 끝이면 숨긴다 */
@Composable
private fun PagerArrow(icon: androidx.compose.ui.graphics.vector.ImageVector, label: String, modifier: Modifier, enabled: Boolean, onClick: () -> Unit) {
    if (!enabled) return
    Box(
        modifier.size(44.dp).shadow(6.dp, CircleShape).clip(CircleShape).background(androidx.compose.ui.graphics.Color.White)
            .feedbackClickable(role = Role.Button, onClick = onClick).semantics { contentDescription = label },
        contentAlignment = Alignment.Center,
    ) {
        Icon(icon, null, tint = androidx.compose.ui.graphics.Color(0xFF1B2433), modifier = Modifier.size(26.dp))
    }
}

/** 신고 있는 신발 카드 한 줄(13차 S-345) — 작은 카드 96 · 이름 · 등급 칩 · Lv · 효율 · 착화감 · 내구도 막대 + 수리 */
@Composable
private fun WornShoeCard(
    shoe: Sneaker, rules: com.stepup.android.domain.ShoeRules, canRepair: Boolean, onOpen: () -> Unit, onRepair: () -> Unit,
) {
    val c = StepUpUi.colors
    val shape = RoundedCornerShape(18.dp)
    val urgent = canRepair && repairUrgent(shoe, rules)
    Row(
        Modifier.padding(horizontal = StepUpUi.Gutter).fillMaxWidth().clip(shape).background(c.card).stepUpRim(shape, RimKind.Surface)
            .feedbackClickable(role = Role.Button, onClick = onOpen).padding(14.dp).testTag("shoe-hero"),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        com.stepup.android.ui.components.V12ShoeCard(shoe, Modifier.width(96.dp), hero = false, mini = true)
        Spacer(Modifier.width(14.dp))
        Column(Modifier.weight(1f)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(shoe.shoeName(), color = c.label, fontSize = 20.sp, fontWeight = FontWeight.ExtraBold, maxLines = 2,
                    overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f, fill = false).testTag("shoe-hero-name"))
                Spacer(Modifier.width(6.dp))
                val meta = shoeMeta(shoe)
                Text(
                    shoe.tier.label(), color = c.label, fontSize = 12.sp, fontWeight = FontWeight.Bold,
                    modifier = Modifier.clip(RoundedCornerShape(6.dp)).background(c.fill).padding(horizontal = 6.dp, vertical = 2.dp)
                        .testTag("shoe-card-tag")
                        .clearAndSetSemantics { text = androidx.compose.ui.text.AnnotatedString(meta) },
                )
            }
            Text(
                stringResource(R.string.v13_shoe_line, shoe.level,
                    com.stepup.android.ui.screens.items.formatIndex(com.stepup.android.ui.screens.items.efficiencyIndex(shoe)),
                    com.stepup.android.ui.screens.items.formatIndex(com.stepup.android.ui.screens.items.comfortIndex(shoe))),
                color = c.secondaryLabel, fontSize = 14.sp, modifier = Modifier.padding(top = 2.dp),
            )
            Spacer(Modifier.height(10.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(stringResource(R.string.sdv_stat_durability), color = c.label, fontSize = 15.sp, fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.weight(1f))
                Text(com.stepup.android.ui.screens.items.durabilityText(shoe, rules), color = if (urgent) c.live else c.label,
                    fontSize = 15.sp, fontWeight = FontWeight.Bold, modifier = Modifier.testTag("shoe-bar-durability-value"))
                if (repairable(shoe)) {
                    Spacer(Modifier.width(6.dp))
                    com.stepup.android.ui.screens.items.V12RepairChip(urgent, canRepair, onRepair)
                }
            }
            Spacer(Modifier.height(8.dp))
            DurabilityBar(com.stepup.android.ui.screens.items.durabilityPercent(shoe))
        }
    }
}

/** "착용 중 · 레어" — 화면 읽기 · 검사가 읽는 카드 이름표(예전 신발 카드와 같은 글) */
@Composable
private fun shoeMeta(shoe: Sneaker): String =
    if (shoe.equipped) stringResource(R.string.my_shoes_wearing) + " · " + shoe.tier.label() else shoe.tier.label()

/** 격자 한 칸(13차 S-345) — 카드(등급 테두리) · Lv · 이름 · 내구도 막대 + 남은 분 */
@Composable
private fun GridShoe(shoe: Sneaker, rules: com.stepup.android.domain.ShoeRules, selected: Boolean, modifier: Modifier, onClick: () -> Unit) {
    val c = StepUpUi.colors
    val pct = com.stepup.android.ui.screens.items.durabilityPercent(shoe)
    val worn = com.stepup.android.ui.screens.items.durabilityWornOut(shoe)
    val minutes = com.stepup.android.ui.screens.items.durabilityEarnMinutes(shoe, rules)
    val name = shoe.shoeName()
    Column(
        modifier.clip(RoundedCornerShape(12.dp)).feedbackClickable(role = Role.Button, onClick = onClick)
            .semantics(mergeDescendants = true) { this.selected = selected; contentDescription = name }
            .testTag("shoe-choice-${shoe.id}"),
    ) {
        Box {
            Box(
                Modifier.fillMaxWidth().then(if (shoe.equipped) Modifier.border(2.dp, c.primary, RoundedCornerShape(12.dp)).padding(2.dp) else Modifier)
                    .alpha(if (worn) .5f else 1f),
            ) {
                com.stepup.android.ui.components.V12ShoeCard(shoe, Modifier.fillMaxWidth(), hero = false, mini = true, miniLabels = false)
            }
            Text("Lv.${shoe.level}", color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(start = 8.dp, top = 7.dp))
            if (shoe.equipped) {
                Box(
                    Modifier.align(Alignment.TopEnd).offset(x = 4.dp, y = (-4).dp).size(20.dp).clip(CircleShape).background(c.primary)
                        .testTag("shoe-worn-badge"),
                    contentAlignment = Alignment.Center,
                ) { Icon(Icons.Filled.Check, null, tint = Color.White, modifier = Modifier.size(13.dp)) }
            }
        }
        Spacer(Modifier.height(6.dp))
        Text(name, color = c.label, fontSize = 13.sp, fontWeight = FontWeight.SemiBold, maxLines = 1, overflow = TextOverflow.Ellipsis)
        Spacer(Modifier.height(4.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.weight(1f)) { DurabilityBar(pct, height = 4.dp) }
            Spacer(Modifier.width(4.dp))
            Text(
                if (worn) stringResource(R.string.v13_shoe_worn) else minutes?.let { stringResource(R.string.v4s_minutes, it.toString()) } ?: "$pct%",
                color = when {
                    worn -> c.live
                    pct < 30 -> Color(0xFFFF7A1A)
                    else -> c.secondaryLabel
                },
                fontSize = 11.sp, fontWeight = FontWeight.SemiBold, maxLines = 1,
            )
        }
    }
}

@Composable
private fun DurabilityBar(percent: Int, height: androidx.compose.ui.unit.Dp = 6.dp) {
    val c = StepUpUi.colors
    val f = (percent / 100f).coerceIn(0f, 1f)
    Box(Modifier.fillMaxWidth().height(height).clip(CircleShape).background(c.fill)) {
        if (f > 0f) Box(Modifier.fillMaxWidth(f).height(height).clip(CircleShape)
            .background(if (percent < 30) Color(0xFFFF7A1A) else c.secondaryLabel.copy(alpha = .85f)))
    }
}
