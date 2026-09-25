package com.stepup.android.ui.screens.items

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Air
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.LocalFireDepartment
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.WaterDrop
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.stepup.android.R
import com.stepup.android.domain.BoostType
import com.stepup.android.domain.Faction
import com.stepup.android.domain.RewardEconomy
import com.stepup.android.ui.components.BarMeter
import com.stepup.android.ui.components.DetailPage
import com.stepup.android.ui.components.EquippedSneakerCard
import com.stepup.android.ui.components.FactionChip
import com.stepup.android.ui.components.FilterSummaryRow
import com.stepup.android.ui.components.FilterToolbar
import com.stepup.android.ui.components.GhostButton
import com.stepup.android.ui.components.GlowCard
import com.stepup.android.ui.components.HexEmblem
import com.stepup.android.ui.components.IconSquare
import com.stepup.android.ui.components.RarityChip
import com.stepup.android.ui.components.SectionHeader
import com.stepup.android.ui.components.SneakerCollectionCard
import com.stepup.android.ui.components.SneakerFrame
import com.stepup.android.ui.components.SupPill
import com.stepup.android.ui.components.VoltButton
import com.stepup.android.ui.components.fullLabel
import com.stepup.android.ui.components.label
import com.stepup.android.ui.components.quietClickable
import com.stepup.android.ui.components.tint
import com.stepup.android.ui.guide.GuideTour
import com.stepup.android.ui.guide.guideTarget
import com.stepup.android.ui.screens.community.SegmentedTabs
import com.stepup.android.ui.theme.CarbonHigh
import com.stepup.android.ui.theme.Silver
import com.stepup.android.ui.screens.market.MarketMessageBar
import com.stepup.android.ui.screens.market.MarketViewModel
import com.stepup.android.ui.screens.market.nftMarketSection
import com.stepup.android.ui.theme.Snow
import com.stepup.android.ui.theme.Volt

import com.stepup.android.ui.components.reveal
import com.stepup.android.ui.components.celebrate

@Composable
fun ItemsScreen(
    onBack: (() -> Unit)? = null,
    showHeader: Boolean = true,
    initialTab: Int = 2,
    marketOnly: Boolean = false,
    onOpenSneaker: (Long) -> Unit = {},
    onOpenDex: () -> Unit = {},
    onOpenMarketModel: (faction: String, rarity: String, variant: Int) -> Unit = { _, _, _ -> },
    viewModel: ItemsViewModel = viewModel(factory = ItemsViewModel.Factory),
    marketViewModel: MarketViewModel = viewModel(factory = MarketViewModel.Factory),
) {
    val context = LocalContext.current
    val inventory by viewModel.inventory.collectAsStateWithLifecycle()
    val groups by viewModel.groups.collectAsStateWithLifecycle()
    // 0 = 스토어, 1 = NFT 마켓, 2 = 아이템(보관함).
    // 보관함이 이 탭의 본디 자리이고 가이드 투어도 거기를 가리키므로 기본은 2다.
    var tab by rememberSaveable { mutableIntStateOf(initialTab.coerceIn(0, 2)) }
    var rarityFilter by rememberSaveable { mutableStateOf<String?>(null) }
    var factionFilter by rememberSaveable { mutableStateOf<String?>(null) }
    // 추가 조건. 속성 카드와는 함께(AND) 걸린다.
    var equipFilter by rememberSaveable { mutableStateOf(EquipFilter.ALL) }
    var itemSort by rememberSaveable { mutableStateOf(ItemSort.RARITY) }
    var itemFilterSheet by rememberSaveable { mutableStateOf(false) }
    var itemSortSheet by rememberSaveable { mutableStateOf(false) }
    var copiesFor by rememberSaveable { mutableStateOf<String?>(null) }
    // NFT 마켓 안의 자리 — 0 = 시세, 1 = 내 거래
    var marketSection by rememberSaveable { mutableIntStateOf(0) }
    val board by marketViewModel.board.collectAsStateWithLifecycle()
    val marketMessage by marketViewModel.message.collectAsStateWithLifecycle()
    val equipped by viewModel.equipped.collectAsStateWithLifecycle()
    val balance by viewModel.balance.collectAsStateWithLifecycle()
    val boosts by viewModel.activeBoosts.collectAsStateWithLifecycle()
    val progress by viewModel.collectionProgress.collectAsStateWithLifecycle()
    val factions by viewModel.factionProgress.collectAsStateWithLifecycle()
    val message by viewModel.message.collectAsStateWithLifecycle()
    val minted by viewModel.mintResult.collectAsStateWithLifecycle()

    val msgNoBalance = stringResource(R.string.toast_no_balance)
    val msgBoostActive = stringResource(R.string.toast_boost_active)
    val msgBoostBought = stringResource(R.string.toast_boost_bought)
    val msgMaxLevel = stringResource(R.string.toast_max_level)
    val msgUpgraded = stringResource(R.string.toast_upgraded)
    val equippedFmt = stringResource(R.string.toast_equipped, "%s")

    LaunchedEffect(message) {
        val m = message ?: return@LaunchedEffect
        val text = when (m) {
            ItemsMessage.SaveFailed -> context.getString(R.string.feed_save_failed)
            ItemsMessage.NotEnoughBalance -> msgNoBalance
            ItemsMessage.BoostAlreadyActive -> msgBoostActive
            ItemsMessage.EnergyCapacity -> context.getString(R.string.energy_purchase_capacity)
            ItemsMessage.BoostBought -> msgBoostBought
            ItemsMessage.MaxLevel -> msgMaxLevel
            is ItemsMessage.Upgraded -> msgUpgraded
            is ItemsMessage.Equipped -> equippedFmt.format(m.sneaker.fullLabel(context))
        }
        Toast.makeText(context, text, Toast.LENGTH_SHORT).show()
        viewModel.consumeMessage()
    }

    val filteredGroups = groups.filter { group ->
        (rarityFilter == null || group.representative.rarity.id == rarityFilter) &&
            (factionFilter == null || group.representative.faction.id == factionFilter) &&
            (equipFilter == EquipFilter.ALL ||
                group.copies.any { it.equipped } == (equipFilter == EquipFilter.ON))
    }.let { list ->
        when (itemSort) {
            ItemSort.RECENT -> list.sortedWith(
                compareByDescending<SneakerGroup> { it.representative.equipped }
                    .thenByDescending { it.representative.acquiredAt },
            )
            ItemSort.LEVEL -> list.sortedWith(
                compareByDescending<SneakerGroup> { it.representative.equipped }
                    .thenByDescending { it.representative.level }
                    .thenByDescending { it.representative.acquiredAt },
            )
            else -> list
        }
    }
    val gridColumns = when {
        LocalDensity.current.fontScale > 1.3f || LocalConfiguration.current.screenWidthDp < 360 -> 1
        LocalConfiguration.current.screenWidthDp >= 600 -> 3
        else -> 2
    }

    DetailPage(title = stringResource(R.string.items_vault_title), onBack = onBack ?: {}, showHeader = showHeader) {
        if (!marketOnly) item {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                GhostButton(
                    text = stringResource(R.string.dex_title),
                    onClick = onOpenDex,
                    modifier = Modifier.weight(1f),
                )
                if (showHeader) SupPill(balance = balance, onClick = null)
            }
        }

        // 탭 안의 탭 — 스토어와 아이템(보관함).
        //
        // 도감 입구와 토큰은 탭 위에 그대로 둔다. 둘 다 어느 쪽에서 눌러도
        // 뜻이 같아서, 탭마다 옮기면 찾는 자리가 두 곳이 된다.
        if (!marketOnly) item {
            SegmentedTabs(
                labels = listOf(
                    stringResource(R.string.market_tab_store),
                    stringResource(R.string.market_tab_nft),
                    stringResource(R.string.market_tab_items),
                ),
                selected = tab,
                onSelect = { tab = it },
            )
        }

        if (tab == 0) {
            storeSection()
            return@DetailPage
        }
        if (tab == 1) {
            marketMessage?.let { note ->
                item {
                    Box(Modifier.quietClickable { marketViewModel.consumeMessage() }) {
                        MarketMessageBar(note)
                    }
                }
            }
            nftMarketSection(
                board = board,
                section = marketSection,
                onSection = { marketSection = it },
                onOpenModel = { onOpenMarketModel(it.faction, it.rarity, it.variant) },
                onCancelListing = marketViewModel::cancelListing,
                onCancelBid = marketViewModel::cancelBid,
                onRetry = marketViewModel::refresh,
            )
            return@DetailPage
        }

        // ── 착용 중인 스니커즈 ──────────────────────────────
        equipped?.let { sneaker ->
            item {
                EquippedSneakerCard(
                    sneaker = sneaker,
                    modifier = Modifier.guideTarget(GuideTour.Targets.ITEMS_EQUIPPED),
                    onClick = { onOpenSneaker(sneaker.id) },
                )
            }
        }

        // Detailed conditions and faction collection counts live in the filter sheet.
        item {
            FilterToolbar(
                filterLabel = stringResource(R.string.filter_button_detail),
                filterCount = itemFilterCount(rarityFilter, equipFilter) + if (factionFilter != null) 1 else 0,
                sortLabel = stringResource(itemSortRes(itemSort)),
                onOpenFilters = { itemFilterSheet = true },
                onOpenSort = { itemSortSheet = true },
            )
        }

        item {
            FilterSummaryRow(
                parts = listOfNotNull(Faction.entries.firstOrNull { it.id == factionFilter }?.label()) +
                    itemFilterParts(rarityFilter, equipFilter),
                onReset = {
                    factionFilter = null
                    rarityFilter = null
                    equipFilter = EquipFilter.ALL
                },
            )
        }

        // ── 컬렉션 헤더 — "N / 52 조합" ───────────────────────
        item {
            Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(stringResource(R.string.items_vault), style = MaterialTheme.typography.titleMedium, color = Snow)
                Text(stringResource(R.string.items_combo, progress.first, progress.second), style = MaterialTheme.typography.bodyMedium, color = Silver)
            }
        }

        if (filteredGroups.isEmpty()) {
            item {
                GlowCard(contentPadding = PaddingValues(24.dp)) {
                    Text(stringResource(R.string.common_none),
                        style = MaterialTheme.typography.bodyMedium, color = Silver)
                }
            }
        } else {
            items(filteredGroups.chunked(gridColumns), key = { it.first().representative.slotKey }) { row ->
                Row(
                    modifier = Modifier.fillMaxWidth().then(
                        if (row.first().representative.slotKey == filteredGroups.first().representative.slotKey)
                            Modifier.guideTarget(GuideTour.Targets.ITEMS_COLLECTION) else Modifier,
                    ),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    row.forEach { group ->
                        SneakerCollectionCard(
                            sneaker = group.representative,
                            count = group.count,
                            modifier = Modifier.weight(1f),
                            onClick = {
                                if (group.count == 1) onOpenSneaker(group.representative.id)
                                else copiesFor = group.representative.slotKey
                            },
                        )
                    }
                    repeat(gridColumns - row.size) { Spacer(Modifier.weight(1f)) }
                }
            }
        }

        // ── 민팅 ────────────────────────────────────────────
        item {
            GlowCard(
                modifier = Modifier.guideTarget(GuideTour.Targets.ITEMS_MINT),
                contentPadding = PaddingValues(18.dp),
                spacing = 12.dp,
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(13.dp),
                ) {
                    HexEmblem(size = 44.dp)
                    Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                        Text(
                            text = stringResource(R.string.items_mint_title),
                            style = MaterialTheme.typography.titleMedium,
                            color = Snow,
                        )
                        Text(
                            text = stringResource(R.string.items_mint_desc),
                            style = MaterialTheme.typography.bodyMedium,
                            color = Silver,
                        )
                    }
                }
                VoltButton(
                    text = stringResource(
                        R.string.items_mint_button,
                        "%,.0f".format(RewardEconomy.MINT_COST),
                    ),
                    onClick = { viewModel.mint() },
                    enabled = balance?.let { it >= RewardEconomy.MINT_COST } == true,
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        }

        // ── 컬렉션 — 한 줄, 옆으로 밀어서 넘긴다 ───────────────
        // ── 활성 부스트 ─────────────────────────────────────
        if (boosts.isNotEmpty()) {
            item { SectionHeader(title = stringResource(R.string.items_active_boosts)) }
            items(boosts.size) { index ->
                val boost = boosts[index]
                GlowCard(
                    accent = true,
                    contentPadding = PaddingValues(horizontal = 15.dp, vertical = 13.dp),
                    shape = RoundedCornerShape(18.dp),
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        IconSquare(icon = boostIcon(boost.type), size = 36.dp)
                        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            Text(stringResource(boostTitle(boost.type)), style = MaterialTheme.typography.titleMedium, color = Snow)
                            Text(stringResource(R.string.boost_time_left, remainingLabel(boost.expiresAt)), style = MaterialTheme.typography.bodyMedium, color = com.stepup.android.ui.theme.VoltText)
                        }
                    }
                }
            }
        }

        // ── 부스트 상점 — 설명과 구매 동작 분리 ─────────────────────
        item {
            SectionHeader(
                title = stringResource(R.string.items_boosts),
                actionText = stringResource(R.string.common_more),
            )
        }

        items(BoostType.entries.toList()) { type ->
            BoostCard(type = type, affordable = balance?.let { it >= type.cost } == true,
                onBuy = { viewModel.buyBoost(type) }, modifier = Modifier.fillMaxWidth())
        }

    }

    // ── 사본 목록 ───────────────────────────────────────────
    copiesFor?.let { slot ->
        val copies = inventory.filter { it.slotKey == slot }
            .sortedWith(
                compareByDescending<com.stepup.android.domain.Sneaker> { it.equipped }
                    .thenByDescending { it.level }
                    .thenBy { it.mintNumber },
            )
        if (copies.isEmpty()) {
            copiesFor = null
        } else {
            CopiesDialog(
                copies = copies,
                onOpen = { id ->
                    copiesFor = null
                    onOpenSneaker(id)
                },
                onDismiss = { copiesFor = null },
            )
        }
    }

    // ── 민팅 결과 ───────────────────────────────────────────
    minted?.let { sneaker ->
        com.stepup.android.ui.components.DialogPanel(
            title = stringResource(R.string.mint_result_title),
            onDismiss = { viewModel.dismissMintResult() },
            actions = {
                VoltButton(stringResource(R.string.common_ok), { viewModel.dismissMintResult() }, Modifier.fillMaxWidth())
            },
        ) {
            Column(
                modifier = Modifier.fillMaxWidth().reveal(sneaker.id).celebrate(sneaker.id),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    FactionChip(sneaker.faction)
                    RarityChip(sneaker.rarity)
                }
                SneakerFrame(sneaker = sneaker, modifier = Modifier.fillMaxWidth().height(180.dp),
                    corner = 16.dp, animate = true)
                Text(sneaker.fullLabel(), style = MaterialTheme.typography.titleLarge,
                    color = Snow, textAlign = TextAlign.Center)
                Text(stringResource(R.string.sneaker_mint_no, sneaker.mintNumber), fontSize = 14.sp, color = Silver)
            }
        }
    }

    // ── 추가 거르기 패널 ───────────────────────────────────────
    //
    // 목록 밖에서 띄운다. LazyColumn 의 item 안에서 창을 띄우면 그 줄이
    // 화면 밖으로 밀릴 때 창까지 사라진다.
    if (itemFilterSheet) {
        ItemFilterSheet(
            faction = factionFilter,
            factionProgress = factions,
            rarity = rarityFilter,
            equip = equipFilter,
            sort = itemSort,
            onDismiss = { itemFilterSheet = false },
            onApply = { faction, rarity, equip, sort ->
                itemFilterSheet = false
                factionFilter = faction
                rarityFilter = rarity
                equipFilter = equip
                itemSort = sort
            },
        )
    }
    if (itemSortSheet) {
        ItemSortSheet(
            selected = itemSort,
            onPick = { itemSort = it },
            onDismiss = { itemSortSheet = false },
        )
    }
}

/** 속성 아이콘 */
private fun factionIcon(faction: Faction) = when (faction) {
    Faction.FIRE -> Icons.Filled.LocalFireDepartment
    Faction.WATER -> Icons.Filled.WaterDrop
    Faction.LIGHTNING -> Icons.Filled.Bolt
    Faction.WIND -> Icons.Filled.Air
}

/**
 * 속성 하나의 도감 진행도 카드.
 * 탭하면 그 속성만 보는 필터가 되고, 선택 중에는 볼트 테두리가 붙는다.
 */
@Composable
private fun FactionProgressCell(
    faction: Faction,
    owned: Int,
    total: Int,
    selected: Boolean,
    modifier: Modifier = Modifier,
    onClick: () -> Unit = {},
) {
    val c = faction.tint()
    val shape = RoundedCornerShape(20.dp)
    Column(
        modifier = modifier
            .clip(shape)
            .background(c.copy(alpha = if (selected) 0.13f else 0.07f))
            .border(
                width = 1.dp,
                color = if (selected) Volt else c.copy(alpha = 0.25f),
                shape = shape,
            )
            .quietClickable(onClick)
            .padding(vertical = 12.dp, horizontal = 8.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        Box(
            modifier = Modifier
                .size(34.dp)
                .clip(CircleShape)
                .background(c.copy(alpha = 0.16f)),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = factionIcon(faction),
                contentDescription = faction.label(),
                tint = c,
                modifier = Modifier.size(18.dp),
            )
        }
        Text(
            text = faction.label(),
            color = c,
            fontSize = 14.sp,
            fontWeight = FontWeight.Black,
            letterSpacing = 0.6.sp,
        )
        Text(
            text = "$owned / $total",
            color = Snow,
            fontSize = 14.sp,
            fontWeight = FontWeight.Bold,
        )
        BarMeter(
            fraction = if (total == 0) 0f else (owned.toFloat() / total).coerceIn(0f, 1f),
            height = 4.dp,
            color = c,
        )
    }
}

/** 같은 도감 슬롯의 사본 목록 — 민팅 번호·레벨·부스트로 구분한다 */
@Composable
private fun CopiesDialog(
    copies: List<com.stepup.android.domain.Sneaker>,
    onOpen: (Long) -> Unit,
    onDismiss: () -> Unit,
) {
    val first = copies.firstOrNull() ?: return
    com.stepup.android.ui.components.DialogPanel(
        title = first.fullLabel() + " ×${copies.size}", onDismiss = onDismiss,
        actions = { VoltButton(stringResource(R.string.common_close), onDismiss, Modifier.fillMaxWidth()) },
    ) {
        copies.forEach { copy ->
            Row(
                Modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp)).background(CarbonHigh)
                    .quietClickable { onOpen(copy.id) }.padding(14.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                SneakerFrame(sneaker = copy, modifier = Modifier.size(64.dp), corner = 12.dp, fade = false)
                Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(5.dp)) {
                    Text(stringResource(R.string.level_chip, copy.level),
                        style = MaterialTheme.typography.titleMedium, color = Snow)
                    if (copy.equipped) {
                        Text(stringResource(R.string.items_equipped), fontSize = 14.sp,
                            fontWeight = FontWeight.SemiBold, color = com.stepup.android.ui.theme.VoltText)
                    }
                    Text(stringResource(R.string.sneaker_mint_no, copy.mintNumber) +
                        " · +%.1f%%".format(copy.boostPercent), fontSize = 14.sp, color = Silver)
                }
                Icon(Icons.Filled.ChevronRight, null, tint = Silver, modifier = Modifier.size(20.dp))
            }
        }
    }
}

@Composable
private fun BoostCard(
    type: BoostType,
    affordable: Boolean,
    onBuy: () -> Unit,
    modifier: Modifier = Modifier,
) {
    GlowCard(modifier, contentPadding = PaddingValues(20.dp), spacing = 16.dp) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(14.dp)) {
            IconSquare(icon = boostIcon(type), size = 44.dp)
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(stringResource(boostTitle(type)), style = MaterialTheme.typography.titleMedium, color = Snow)
                Text(stringResource(boostDesc(type)), style = MaterialTheme.typography.bodyMedium, color = Silver)
            }
        }
        GhostButton(stringResource(R.string.price_sup, "%,.0f".format(type.cost)), onClick = onBuy, enabled = affordable, modifier = Modifier.fillMaxWidth())
    }
}

private fun boostIcon(type: BoostType): ImageVector = when (type) {
    BoostType.ENERGY_CELL -> Icons.Filled.Bolt
    BoostType.STREAK_SHIELD -> Icons.Filled.Shield
    BoostType.XP_BOOSTER -> Icons.Filled.AutoAwesome
}

private fun boostTitle(type: BoostType): Int = when (type) {
    BoostType.ENERGY_CELL -> R.string.boost_energy_title
    BoostType.STREAK_SHIELD -> R.string.boost_shield_title
    BoostType.XP_BOOSTER -> R.string.boost_xp_title
}

private fun boostDesc(type: BoostType): Int = when (type) {
    BoostType.ENERGY_CELL -> R.string.boost_energy_desc
    BoostType.STREAK_SHIELD -> R.string.boost_shield_desc
    BoostType.XP_BOOSTER -> R.string.boost_xp_desc
}

/** 남은 시간 라벨 (h/m) */
private fun remainingLabel(expiresAt: Long): String {
    val left = ((expiresAt - System.currentTimeMillis()) / 1000).coerceAtLeast(0)
    val h = left / 3600
    val m = (left % 3600) / 60
    return if (h > 0) "${h}h ${m}m" else "${m}m"
}
