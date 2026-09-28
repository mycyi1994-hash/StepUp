package com.stepup.android.ui.screens.customize

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Refresh
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.stepup.android.R
import com.stepup.android.domain.Sneaker
import com.stepup.android.ui.components.S2ShoesSections
import com.stepup.android.ui.components.SettingsSheet
import com.stepup.android.ui.components.ShoeArtThumbnail
import com.stepup.android.ui.components.ShoeStage
import com.stepup.android.ui.components.ShoeStageMessageAlignment
import com.stepup.android.ui.components.ShoeStageRatio
import com.stepup.android.ui.components.StepUpIcons
import com.stepup.android.ui.components.preloadShoeArt
import com.stepup.android.ui.components.settingsPalette
import com.stepup.android.ui.experience.feedbackClickable
import com.stepup.android.ui.guide.GuideTour
import com.stepup.android.ui.guide.guideTarget
import com.stepup.android.ui.screens.items.ItemsViewModel
import com.stepup.android.ui.screens.items.OwnedGroup
import com.stepup.android.ui.screens.items.OwnedLoad
import com.stepup.android.ui.screens.items.ShoeStatePanel
import com.stepup.android.ui.screens.items.energySavingPercent
import com.stepup.android.ui.screens.items.formatPercent
import com.stepup.android.ui.screens.items.ownedGroups
import com.stepup.android.ui.screens.items.pickInGroup
import com.stepup.android.ui.screens.items.resolveSelection
import com.stepup.android.ui.screens.items.shoeName
import com.stepup.android.ui.theme.StepUpColors
import com.stepup.android.ui.theme.StepUpDesign
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

/**
 * 신발 탭(보유 신발 상세 v1, 2026-09-28 전달본 — docs/redesign/shoe-detail-v1, 시안 01 · 07 · 12 · 13 · 18).
 *
 * 선택한(또는 신고 있는) 신발 한 켤레를 낮은 받침 위에 크게 → "신발 자세히 보기" → 보유 신발(같은 모델은 한 칸, 가로로 넘긴다).
 * 칸을 누르면 보기만 바뀐다 — 착용은 상세의 "이 신발 신기"로만 바뀐다. 파란 밑줄은 보는 신발, 작은 체크는 실제로 신고 있는 신발.
 * 같은 모델이 여러 켤레면 칸 아래 "N켤레 보기"가 신발 번호로 구분하는 시트(07)를 연다. 고르기는 늘 소유 id 로 한다 —
 * 목록 순서가 바뀌어도 예전 자리의 다른 신발을 고르지 않는다. 가로 목록 자리와 고른 신발은 돌아왔을 때 그대로다.
 *
 * 예전 원형 "신기" 버튼은 상세의 주 버튼으로 옮겼고, 마켓 · 도감 · 보관함은 목록 아래 작은 줄로 옮겼다(지우지 않음).
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
    /** 뽑기 결과의 "내 신발 보기" — 받은 신발을 고른 채로 연다(미리 보기일 뿐, 착용은 바꾸지 않는다) */
    focusShoeId: Long? = null,
    onFocusShoeShown: () -> Unit = {},
    viewModel: ItemsViewModel = viewModel(factory = ItemsViewModel.Factory),
) {
    val owned by viewModel.owned.collectAsStateWithLifecycle()
    // 보는 신발 — 소유 id. 없거나 사라졌으면 신고 있는 켤레(resolveSelection)
    var selectedId by rememberSaveable { mutableStateOf<Long?>(null) }
    LaunchedEffect(focusShoeId) {
        if (focusShoeId != null) {
            selectedId = focusShoeId
            onFocusShoeShown()
        }
    }
    Column(Modifier.fillMaxSize().padding(bottom = 4.dp)) {
        S2ShoesSections(drawSelected = false, onShoes = {}, onDraw = onOpenDraw, modifier = Modifier.padding(horizontal = StepUpDesign.Gutter))
        ShoeTabContent(
            load = owned,
            selectedId = selectedId,
            onSelect = { selectedId = it },
            onOpenSneaker = onOpenSneaker,
            onOpenDraw = onOpenDraw,
            onOpenVault = onOpenVault,
            onOpenDex = onOpenDex,
            onOpenMarket = onOpenMarket,
            onReload = viewModel::reloadOwned,
            modifier = Modifier.weight(1f),
        )
    }
}

/**
 * 신발 탭 본문 — 상태를 받아 그리기만 한다(테스트가 시안 장면을 바로 넣어 찍는다). [initialCopiesFor] 는 07 시트를 연 채로 시작할 모델 키.
 */
@Composable
fun ShoeTabContent(
    load: OwnedLoad,
    selectedId: Long?,
    onSelect: (Long) -> Unit,
    onOpenSneaker: (Long) -> Unit,
    onOpenDraw: () -> Unit,
    onOpenVault: () -> Unit,
    onOpenDex: () -> Unit,
    onOpenMarket: () -> Unit,
    onReload: () -> Unit,
    modifier: Modifier = Modifier,
    zone: ZoneId = ZoneId.systemDefault(),
    initialCopiesFor: String? = null,
) {
    BoxWithConstraints(modifier.fillMaxWidth()) {
        val window = maxHeight
        val width = maxWidth
        when {
            load == OwnedLoad.Loading -> LazyColumn(Modifier.fillMaxSize().testTag("shoe-list")) {
                item(key = "loading") { HeroLoading(stageWidth(window, width)) }
            }
            load == OwnedLoad.Failed -> LazyColumn(Modifier.fillMaxSize().testTag("shoe-list")) {
                item(key = "failed") {
                    ShoeStatePanel(
                        icon = Icons.Filled.Refresh,
                        title = stringResource(R.string.sdv_load_failed_title),
                        body = stringResource(R.string.sdv_load_failed_body),
                        primary = stringResource(R.string.sdv_reload) to onReload,
                        modifier = Modifier.padding(horizontal = StepUpDesign.Gutter).testTag("shoe-tab-failed"),
                    )
                }
            }
            load is OwnedLoad.Ready && load.shoes.isEmpty() -> LazyColumn(Modifier.fillMaxSize().testTag("shoe-list")) {
                // 18 — 읽기가 끝났고 정말 비었을 때만. 누르면 기존 무료 뽑기(뽑기 화면이 수 · 조건을 정한다)
                item(key = "empty") {
                    ShoeStatePanel(
                        icon = StepUpIcons.Shoe,
                        title = stringResource(R.string.sdv_empty_title),
                        body = stringResource(R.string.sdv_empty_body),
                        primary = stringResource(R.string.sdv_empty_action) to onOpenDraw,
                        modifier = Modifier.padding(horizontal = StepUpDesign.Gutter).testTag("shoe-tab-empty"),
                    )
                }
            }
            load is OwnedLoad.Ready -> OwnedShoes(
                shoes = load.shoes, selectedId = selectedId, onSelect = onSelect, stageWidth = stageWidth(window, width),
                windowWidth = width, onOpenSneaker = onOpenSneaker, onOpenVault = onOpenVault, onOpenDex = onOpenDex,
                onOpenMarket = onOpenMarket, zone = zone, initialCopiesFor = initialCopiesFor,
            )
        }
    }
}

/**
 * 큰 무대의 폭 — 이름 · 무대 · 자세히 보기 · 보유 목록 첫 줄이 한 화면에 들도록 목록 창 높이에서 글자 자리를 뺀 만큼.
 * 창이 아주 짧으면(가로 화면 · 큰 글씨) 220dp 아래로 줄이지 않고 스크롤에 맡긴다.
 */
@Composable
private fun stageWidth(window: Dp, width: Dp): Dp {
    val scale = LocalDensity.current.fontScale
    val texts = 104.dp * scale
    val below = 56.dp + 196.dp * minOf(scale, 1.3f)
    val fromHeight = (window - texts - below) * ShoeStageRatio
    return fromHeight.coerceIn(minOf(220.dp, width), width)
}

@Composable
private fun OwnedShoes(
    shoes: List<Sneaker>,
    selectedId: Long?,
    onSelect: (Long) -> Unit,
    stageWidth: Dp,
    windowWidth: Dp,
    onOpenSneaker: (Long) -> Unit,
    onOpenVault: () -> Unit,
    onOpenDex: () -> Unit,
    onOpenMarket: () -> Unit,
    zone: ZoneId,
    initialCopiesFor: String?,
) {
    val context = LocalContext.current
    val groups = remember(shoes) { ownedGroups(shoes) }
    val selected = resolveSelection(groups, selectedId) ?: return
    // 무대에 보이는 켤레 — 새로 고른 켤레의 그림을 먼저 읽은 뒤 이름과 함께 바꾼다(이름만 앞서 바뀌지 않게)
    var shownId by rememberSaveable { mutableStateOf<Long?>(null) }
    LaunchedEffect(selected.id) {
        if (shownId != null && shownId != selected.id) preloadShoeArt(context, selected)
        shownId = selected.id
    }
    val shown = shoes.firstOrNull { it.id == shownId } ?: selected
    var copiesFor by rememberSaveable { mutableStateOf(initialCopiesFor) }
    val rowState = rememberLazyListState()
    // 처음 한 번만 — 고른 칸이 보이게 가로 목록을 둔다. 돌아왔을 때는 사용자가 둔 자리를 그대로 쓴다
    var rowPlaced by rememberSaveable { mutableStateOf(false) }
    LaunchedEffect(groups.isNotEmpty()) {
        if (!rowPlaced && groups.isNotEmpty()) {
            val index = groups.indexOfFirst { group -> group.copies.any { it.id == selected.id } }
            if (index > 1) rowState.scrollToItem(index - 1)
            rowPlaced = true
        }
    }
    LazyColumn(Modifier.fillMaxSize().testTag("shoe-list"), contentPadding = PaddingValues(bottom = 16.dp)) {
        item(key = "hero") { ShoeHero(shown, stageWidth, onOpenDetail = { onOpenSneaker(shown.id) }) }
        item(key = "owned-head") { OwnedHeader(shoes.size) }
        item(key = "owned-row") {
            OwnedRow(
                groups = groups, selectedId = selected.id, state = rowState, windowWidth = windowWidth,
                onPick = { group -> onSelect(pickInGroup(group, selected.id)) },
                onOpenCopies = { group -> copiesFor = group.key },
            )
        }
        item(key = "links") { ShoeLinks(onOpenVault, onOpenDex, onOpenMarket) }
    }
    val copiesGroup = groups.firstOrNull { it.key == copiesFor }
    if (copiesGroup != null) {
        CopiesSheet(
            group = copiesGroup, viewingId = selected.id, zone = zone,
            onOpen = { id ->
                copiesFor = null
                onSelect(id)
                onOpenSneaker(id)
            },
            onClose = { copiesFor = null },
        )
    }
}

/** 01 · 12 — 선택한 신발 · 지금 신고 있는 신발 → 이름 → Lv · 번호 → 받침 위 신발 → 신발 자세히 보기 */
@Composable
private fun ShoeHero(shoe: Sneaker, stageWidth: Dp, onOpenDetail: () -> Unit) {
    val p = settingsPalette()
    Column(
        Modifier.fillMaxWidth().testTag("shoe-preview").guideTarget(GuideTour.Targets.CUSTOMIZE_PREVIEW),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        val levelNoCd = stringResource(R.string.sdv_level_no_cd, shoe.level, shoe.mintNumber)
        Column(
            Modifier.fillMaxWidth().padding(horizontal = StepUpDesign.Gutter).padding(top = 14.dp)
                .semantics(mergeDescendants = true) {},
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(
                stringResource(if (shoe.equipped) R.string.sdv_kicker_wearing else R.string.sdv_kicker_selected),
                color = p.secondary, fontSize = 12.5.sp, textAlign = TextAlign.Center, modifier = Modifier.testTag("shoe-kicker"),
            )
            Spacer(Modifier.height(8.dp))
            Text(
                shoe.shoeName(), color = p.text, fontSize = 28.sp, fontWeight = FontWeight.SemiBold, lineHeight = 1.24.em,
                letterSpacing = (-0.028).em, textAlign = TextAlign.Center, maxLines = 2, overflow = TextOverflow.Ellipsis,
                modifier = Modifier.fillMaxWidth().semantics { heading() }.testTag("shoe-hero-name"),
            )
            Spacer(Modifier.height(6.dp))
            Text(
                stringResource(R.string.sdv_level_no, shoe.level, shoe.mintNumber), color = p.secondary, fontSize = 12.sp,
                modifier = Modifier.semantics { contentDescription = levelNoCd },
            )
        }
        // 조명이 위 글자 뒤로 번지게 무대를 먼저 그린다
        Box(Modifier.width(stageWidth).zIndex(-1f).testTag("shoe-hero")) {
            ShoeStage(shoe, Modifier.fillMaxWidth())
        }
        Row(
            Modifier.heightIn(min = StepUpDesign.TouchTarget).clip(RoundedCornerShape(12.dp))
                .feedbackClickable(role = Role.Button, onClick = onOpenDetail)
                .padding(horizontal = 14.dp).testTag("shoe-detail"),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(stringResource(R.string.sdv_open_detail), color = p.accent, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
            Spacer(Modifier.width(6.dp))
            Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, contentDescription = null, tint = p.accent, modifier = Modifier.size(18.dp))
        }
    }
}

/** 13 — 신발 탭을 처음 읽는 동안: 이름 자리 · 빈 받침 · "신발을 불러오고 있어요." */
@Composable
private fun HeroLoading(stageWidth: Dp) {
    val p = settingsPalette()
    Column(Modifier.fillMaxWidth().padding(top = 18.dp).testTag("shoe-tab-loading"), horizontalAlignment = Alignment.CenterHorizontally) {
        Box(Modifier.size(width = 96.dp, height = 11.dp).clip(RoundedCornerShape(4.dp)).background(p.skeleton))
        Spacer(Modifier.height(14.dp))
        Box(Modifier.size(width = 148.dp, height = 23.dp).clip(RoundedCornerShape(6.dp)).background(p.skeleton))
        Spacer(Modifier.height(12.dp))
        Box(Modifier.size(width = 104.dp, height = 11.dp).clip(RoundedCornerShape(4.dp)).background(p.skeleton))
        Box(Modifier.width(stageWidth).zIndex(-1f)) {
            ShoeStage(
                sneaker = null,
                modifier = Modifier.fillMaxWidth(),
                overlay = {
                    Text(stringResource(R.string.sdv_loading), color = p.secondary, fontSize = 15.sp, textAlign = TextAlign.Center,
                        modifier = Modifier.align(ShoeStageMessageAlignment))
                },
            )
        }
    }
}

/** "보유 신발  4켤레" — 켤레 수(모델 종류 수가 아니다) */
@Composable
private fun OwnedHeader(pairs: Int) {
    val p = settingsPalette()
    Row(
        Modifier.fillMaxWidth().padding(horizontal = StepUpDesign.Gutter).padding(top = 10.dp, bottom = 12.dp)
            .semantics(mergeDescendants = true) {}.testTag("shoe-owned-head"),
        verticalAlignment = Alignment.Bottom,
    ) {
        Text(stringResource(R.string.sdv_owned_title), color = p.text, fontSize = 16.sp, fontWeight = FontWeight.SemiBold,
            modifier = Modifier.semantics { heading() })
        Spacer(Modifier.width(10.dp))
        Text(pluralStringResource(R.plurals.sdv_pairs, pairs, pairs), color = p.secondary, fontSize = 13.sp,
            modifier = Modifier.testTag("shoe-owned-count"))
    }
}

/** 보유 목록 — 같은 모델은 한 칸, 가로로 넘긴다. 칸 폭은 한 화면에 세 칸(시안 106dp) 안팎 */
@Composable
private fun OwnedRow(
    groups: List<OwnedGroup>,
    selectedId: Long,
    state: LazyListState,
    windowWidth: Dp,
    onPick: (OwnedGroup) -> Unit,
    onOpenCopies: (OwnedGroup) -> Unit,
) {
    val tileWidth = ((windowWidth - StepUpDesign.Gutter * 2 - 24.dp) / 3).coerceIn(88.dp, 132.dp)
    LazyRow(
        state = state,
        modifier = Modifier.fillMaxWidth().testTag("shoe-owned-row"),
        contentPadding = PaddingValues(horizontal = StepUpDesign.Gutter),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        items(groups, key = { it.key }) { group ->
            OwnedTile(
                group = group,
                viewing = group.copies.any { it.id == selectedId },
                width = tileWidth,
                onPick = { onPick(group) },
                onOpenCopies = { onOpenCopies(group) },
            )
        }
    }
}

/**
 * 한 칸 — 신발 그림 · 이름. 보는 칸은 밝은 면 + 파란 밑줄, 신고 있는 켤레가 든 칸은 오른쪽 위 작은 체크.
 * 색만으로 가르지 않는다 — 읽을 때 "보는 중" · "신고 있음"을 함께 말한다.
 */
@Composable
private fun OwnedTile(group: OwnedGroup, viewing: Boolean, width: Dp, onPick: () -> Unit, onOpenCopies: () -> Unit) {
    val p = settingsPalette()
    val dark = StepUpColors.dark
    val shoe = group.representative
    val worn = group.worn != null
    val name = shoe.shoeName()
    val stateText = listOfNotNull(
        stringResource(R.string.sdv_state_viewing).takeIf { viewing },
        stringResource(R.string.sdv_state_wearing).takeIf { worn },
    ).joinToString(", ")
    val shape = RoundedCornerShape(14.dp)
    Column(Modifier.width(width), horizontalAlignment = Alignment.CenterHorizontally) {
        Column(
            Modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp))
                .feedbackClickable(role = Role.RadioButton, onClick = onPick)
                .semantics {
                    selected = viewing
                    if (stateText.isNotEmpty()) stateDescription = stateText
                }
                .testTag("shoe-choice-${shoe.id}"),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Box(
                Modifier.fillMaxWidth().aspectRatio(106f / 87f).clip(shape)
                    .background(tileFill(dark, viewing)).testTag("shoe-tile-${shoe.id}"),
            ) {
                ShoeArtThumbnail(shoe, Modifier.fillMaxSize().padding(start = 9.dp, end = 9.dp, top = 12.dp, bottom = 12.dp),
                    failedTint = p.secondary)
                if (worn) {
                    Box(
                        Modifier.align(Alignment.TopEnd).offset(x = (-6).dp, y = 6.dp).size(15.dp).clip(CircleShape)
                            .background(p.accent).testTag("shoe-worn-badge"),
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(Icons.Filled.Check, contentDescription = null, tint = if (dark) Color(0xFF081223) else Color.White,
                            modifier = Modifier.size(11.dp))
                    }
                }
                if (viewing) {
                    Box(
                        Modifier.align(Alignment.BottomCenter).padding(bottom = 2.dp).size(width = 30.dp, height = 2.5.dp)
                            .clip(RoundedCornerShape(2.dp)).background(if (dark) Color(0xFF7EA7FF) else p.accent)
                            .testTag("shoe-viewing-mark"),
                    )
                }
            }
            Text(
                name, color = if (viewing) p.text else p.secondary, fontSize = 12.5.sp, lineHeight = 1.35.em,
                textAlign = TextAlign.Center, maxLines = 2, overflow = TextOverflow.Ellipsis,
                modifier = Modifier.padding(top = 8.dp, bottom = 2.dp),
            )
        }
        if (group.count > 1) {
            val copiesCd = pluralStringResource(R.plurals.sdv_see_copies_cd, group.count, name, group.count)
            Box(
                Modifier.fillMaxWidth().heightIn(min = StepUpDesign.TouchTarget)
                    .feedbackClickable(role = Role.Button, onClick = onOpenCopies)
                    .semantics { contentDescription = copiesCd }
                    .testTag("shoe-copies-${group.key}"),
                contentAlignment = Alignment.TopCenter,
            ) {
                Text(pluralStringResource(R.plurals.sdv_see_copies, group.count, group.count), color = p.accent, fontSize = 11.sp,
                    fontWeight = FontWeight.SemiBold, textAlign = TextAlign.Center, modifier = Modifier.padding(top = 2.dp))
            }
        }
    }
}

/** 칸 면 — 어두운 테마는 시안 값, 밝은 테마는 설정 v1 의 옅은 면 */
private fun tileFill(dark: Boolean, viewing: Boolean): Color = when {
    dark && viewing -> Color(0xFF112038)
    dark -> Color(0xFF09121F)
    viewing -> Color(0xFFDCE5F2)
    else -> Color(0xFFEEF2F8)
}

/** 목록 아래 작은 줄 — 신발 보관함 · 도감 · 마켓(원형 버튼 줄에서 옮김, 지우지 않음) */
@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun ShoeLinks(onOpenVault: () -> Unit, onOpenDex: () -> Unit, onOpenMarket: () -> Unit) {
    val p = settingsPalette()
    FlowRow(
        Modifier.fillMaxWidth().padding(horizontal = StepUpDesign.Gutter).padding(top = 6.dp),
        horizontalArrangement = Arrangement.spacedBy(4.dp, Alignment.CenterHorizontally),
    ) {
        listOf(
            Triple(R.string.customize_open_vault, onOpenVault, "shoe-open-vault"),
            Triple(R.string.shoes_s2_dex, onOpenDex, "shoe-open-dex"),
            Triple(R.string.shoes_s2_market, onOpenMarket, "shoe-open-market"),
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

/**
 * 07 같은 모델 여러 켤레 — 신발 번호로 구분. "보는 중"은 고른 켤레, "신고 있음"은 실제 착용(따로 적는다).
 * 줄을 누르면 그 켤레를 고르고 그 id 의 상세로. 고르기만으로는 착용이 바뀌지 않는다.
 */
@Composable
private fun CopiesSheet(group: OwnedGroup, viewingId: Long, zone: ZoneId, onOpen: (Long) -> Unit, onClose: () -> Unit) {
    val p = settingsPalette()
    val name = group.representative.shoeName()
    val pattern = stringResource(R.string.sdv_date_pattern)
    val formatter = remember(pattern) { DateTimeFormatter.ofPattern(pattern) }
    SettingsSheet(
        title = pluralStringResource(R.plurals.sdv_copies_title, group.count, name, group.count),
        onDismiss = onClose,
        modifier = Modifier.testTag("shoe-copies-sheet"),
    ) {
        Text(stringResource(R.string.sdv_copies_intro), color = p.secondary, fontSize = 15.sp, lineHeight = 1.45.em)
        Column {
            group.copies.forEach { copy ->
                CopyRow(
                    copy = copy,
                    viewing = copy.id == viewingId,
                    received = formatter.format(Instant.ofEpochMilli(copy.acquiredAt).atZone(zone)),
                    onClick = { onOpen(copy.id) },
                )
            }
        }
        Text(stringResource(R.string.sdv_copies_note), color = p.secondary, fontSize = 14.sp, lineHeight = 1.45.em,
            modifier = Modifier.padding(top = 8.dp))
    }
}

@Composable
private fun CopyRow(copy: Sneaker, viewing: Boolean, received: String, onClick: () -> Unit) {
    val p = settingsPalette()
    Column(
        Modifier.fillMaxWidth().feedbackClickable(role = Role.Button, onClick = onClick)
            .semantics { selected = viewing }.testTag("shoe-copy-${copy.id}"),
    ) {
        Row(Modifier.fillMaxWidth().heightIn(min = 96.dp).padding(vertical = 14.dp), verticalAlignment = Alignment.CenterVertically) {
            ShoeArtThumbnail(copy, Modifier.size(width = 84.dp, height = 60.dp), failedTint = p.secondary)
            Spacer(Modifier.width(16.dp))
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text("#%04d · ".format(copy.mintNumber) + stringResource(R.string.level_chip, copy.level), color = p.text,
                    fontSize = 18.sp, fontWeight = FontWeight.SemiBold)
                Text(stringResource(R.string.sdv_copy_energy, formatPercent(energySavingPercent(copy))), color = p.secondary, fontSize = 14.sp)
                Text(stringResource(R.string.sdv_copy_received, received), color = p.secondary, fontSize = 14.sp)
                if (copy.equipped) {
                    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.testTag("shoe-copy-worn-${copy.id}")) {
                        Icon(Icons.Filled.Check, contentDescription = null, tint = p.accent, modifier = Modifier.size(14.dp))
                        Spacer(Modifier.width(4.dp))
                        Text(stringResource(R.string.sdv_state_wearing), color = p.accent, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                    }
                }
            }
            Spacer(Modifier.width(8.dp))
            if (viewing) {
                Text(stringResource(R.string.sdv_state_viewing), color = p.accent, fontSize = 13.sp, fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.testTag("shoe-copy-viewing"))
            } else {
                Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, contentDescription = null, tint = p.secondary,
                    modifier = Modifier.size(22.dp))
            }
        }
        Box(Modifier.fillMaxWidth().height(1.dp).background(p.divider))
    }
}
