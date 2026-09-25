package com.stepup.android.ui.screens.market

import com.stepup.android.ui.components.AdaptiveNumber
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.heightIn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Gavel
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.ReceiptLong
import androidx.compose.material3.Icon
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.stepup.android.R
import com.stepup.android.data.remote.ModelKey
import com.stepup.android.data.remote.MyBidRow
import com.stepup.android.data.remote.MySneakerRow
import com.stepup.android.data.remote.MyTradeRow
import com.stepup.android.ui.components.GhostButton
import com.stepup.android.ui.components.GlowCard
import com.stepup.android.ui.components.SectionHeader
import com.stepup.android.ui.components.SneakerFrame
import com.stepup.android.ui.components.quietClickable
import com.stepup.android.ui.theme.Silver
import com.stepup.android.ui.theme.Slate
import com.stepup.android.ui.theme.Snow
import com.stepup.android.ui.theme.VoltText

/**
 * 마켓 탭 안의 NFT 마켓.
 *
 * ── 이 장이 하는 일 ──
 *
 * 러너끼리 스니커즈를 사고판다. 값은 두 갈래로 매겨진다 — 파는 사람이 건
 * 값(매물)과 사는 사람이 건 값(구매 입찰)이고, 둘이 만나면 체결된다.
 *
 * 크림처럼 **모델 단위로 시세를 묶고**, 오픈시처럼 **켤레마다 다른 값
 * (레벨·민팅 번호)을 줄에 적는다.** 같은 모델이라도 레벨이 다르면 값이
 * 다르기 때문이다.
 *
 * 여기는 시세판이고, 실제 매물과 입찰은 모델을 누르면 나오는 장부에 있다.
 */
fun LazyListScope.nftMarketSection(
    board: MarketBoard,
    section: Int,
    onSection: (Int) -> Unit,
    onOpenModel: (ModelKey) -> Unit,
    onCancelListing: (Long) -> Unit,
    onCancelBid: (Long) -> Unit,
    onRetry: () -> Unit,
) {
    item {
        GlowCard(contentPadding = MarketCardPadding, spacing = 12.dp) {
            MarketCardHead(
                title = stringResource(R.string.market_nft_head),
                sub = stringResource(R.string.market_nft_body),
            )
            Text(stringResource(R.string.market_tradable), style = MaterialTheme.typography.bodyMedium, color = Silver)
            AdaptiveNumber(if (board.loading || board.problem != null) "—" else formatSup(board.tradable), 30.sp, color = VoltText)
            Text("SUP", style = MaterialTheme.typography.bodyMedium, color = Silver)
            Text(stringResource(R.string.market_tradable_note), style = MaterialTheme.typography.bodyMedium, color = Silver)
        }
    }

    item {
        com.stepup.android.ui.screens.community.SegmentedTabs(
            labels = listOf(
                stringResource(R.string.market_section_quotes),
                stringResource(R.string.market_section_mine),
            ),
            selected = section,
            onSelect = onSection,
        )
    }

    // Keep destinations stable while loading/authentication/network errors are shown.
    if (board.problem != null) {
        item { MarketProblemNote(board.problem, onRetry = onRetry) }
        return
    }

    if (board.loading) {
        item { Text(stringResource(R.string.feed_loading), color = Silver) }
        return
    }
    if (section == 1) {
        myMarketSection(board.mine, onCancelListing, onCancelBid)
        return
    }

    item { SectionHeader(title = stringResource(R.string.market_quotes_title)) }

    if (board.quotes.isEmpty()) {
        item {
            GlowCard(contentPadding = MarketCardPadding) {
                EmptyNote(stringResource(R.string.market_quotes_empty))
            }
        }
        return
    }

    items(board.quotes.size) { index ->
        val quote = board.quotes[index]
        QuoteRowCard(
            faction = quote.faction,
            rarity = quote.rarity,
            variant = quote.variant,
            supply = quote.supply,
            ask = quote.ask,
            bid = quote.bid,
            lastPrice = quote.lastPrice,
            modifier = Modifier.quietClickable {
                onOpenModel(ModelKey(quote.faction, quote.rarity, quote.variant))
            },
        )
    }

    item {
        Text(
            text = stringResource(R.string.market_fee_note),
            modifier = Modifier.padding(horizontal = 4.dp),
            fontSize = 14.sp,
            color = Slate,
            lineHeight = 20.sp,
        )
    }
}

/** 내 거래 — 팔려고 내놓은 것 · 걸어 둔 입찰 · 사고판 기록 */
private fun LazyListScope.myMarketSection(
    mine: com.stepup.android.data.repo.MyMarket?,
    onCancelListing: (Long) -> Unit,
    onCancelBid: (Long) -> Unit,
) {
    val listed = mine?.sneakers.orEmpty().filter { it.status == "LISTED" }
    val bids = mine?.bids.orEmpty()
    val trades = mine?.trades.orEmpty()

    if (listed.isEmpty()) {
        item { MarketEmptyState(R.string.market_my_listings, R.string.market_my_listings_empty,
            Icons.Filled.ReceiptLong) }
    } else {
        item { SectionHeader(title = stringResource(R.string.market_my_listings)) }
        items(listed.size) { index -> MyListingRow(listed[index], onCancelListing) }
    }

    if (bids.isEmpty()) {
        item { MarketEmptyState(R.string.market_my_bids, R.string.market_my_bids_empty,
            Icons.Filled.Gavel) }
    } else {
        item { SectionHeader(title = stringResource(R.string.market_my_bids)) }
        items(bids.size) { index -> MyBidRowCard(bids[index], onCancelBid) }
    }

    if (trades.isEmpty()) {
        item { MarketEmptyState(R.string.market_my_trades, R.string.market_my_trades_empty,
            Icons.Filled.History) }
    } else {
        item { SectionHeader(title = stringResource(R.string.market_my_trades)) }
        items(trades.size) { index -> MyTradeRowCard(trades[index]) }
    }
}

@Composable
private fun MarketEmptyState(title: Int, message: Int, icon: ImageVector) {
    GlowCard(contentPadding = PaddingValues(20.dp), spacing = 12.dp) {
        Text(stringResource(title), style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold, color = Snow)
        Box(Modifier.fillMaxWidth().heightIn(min = 84.dp), contentAlignment = Alignment.Center) {
            Icon(icon, contentDescription = null, tint = VoltText,
                modifier = Modifier.size(52.dp))
        }
        Text(stringResource(message), style = MaterialTheme.typography.bodyMedium,
            color = Silver, modifier = Modifier.align(Alignment.CenterHorizontally))
    }
}

@Composable
private fun HoldingSummary(faction: String, rarity: String, variant: Int, meta: String, level: Int = 1) {
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(16.dp)) {
        SneakerFrame(previewSneaker(faction, rarity, variant, level), modifier = Modifier.size(72.dp), corner = 14.dp)
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text(modelName(faction, rarity, variant), style = MaterialTheme.typography.titleMedium, color = Snow)
            Text(meta, style = MaterialTheme.typography.bodyMedium, color = Silver)
        }
    }
}

@Composable
private fun HoldingAmount(value: Double?, color: androidx.compose.ui.graphics.Color = VoltText) {
    AdaptiveNumber(value?.let { formatSup(it) } ?: "—", 26.sp, color = color)
    Text("SUP", style = MaterialTheme.typography.bodyMedium, color = Silver)
}

@Composable
private fun MyListingRow(row: MySneakerRow, onCancel: (Long) -> Unit) {
    GlowCard(contentPadding = PaddingValues(20.dp), spacing = 12.dp) {
        HoldingSummary(row.faction, row.rarity, row.variant, stringResource(R.string.market_level, row.level) + " · #${row.mintNumber}", row.level)
        HoldingAmount(row.listingPrice)
        row.listingId?.let { id ->
            GhostButton(stringResource(R.string.market_cancel_listing), onClick = { onCancel(id) }, modifier = Modifier.fillMaxWidth())
        }
    }
}

@Composable
private fun MyBidRowCard(row: MyBidRow, onCancel: (Long) -> Unit) {
    GlowCard(contentPadding = PaddingValues(20.dp), spacing = 12.dp) {
        HoldingSummary(row.faction, row.rarity, row.variant, stringResource(R.string.market_min_level, row.minLevel))
        HoldingAmount(row.price)
        Text(stringResource(R.string.market_bid_locked), style = MaterialTheme.typography.bodyMedium, color = Silver)
        GhostButton(stringResource(R.string.market_cancel_bid), onClick = { onCancel(row.id) }, modifier = Modifier.fillMaxWidth())
    }
}

@Composable
private fun MyTradeRowCard(row: MyTradeRow) {
    GlowCard(contentPadding = PaddingValues(20.dp), spacing = 12.dp) {
        Text(stringResource(if (row.sold) R.string.market_trade_sold else R.string.market_trade_bought), style = MaterialTheme.typography.bodyMedium, color = Silver)
        Text(modelName(row.faction, row.rarity, row.variant), style = MaterialTheme.typography.titleMedium, color = Snow)
        HoldingAmount(row.price, color = if (row.sold) VoltText else Snow)
        if (row.sold && row.fee > 0) Text(stringResource(R.string.market_fee_paid, formatSup(row.fee)), style = MaterialTheme.typography.bodyMedium, color = Silver)
    }
}
