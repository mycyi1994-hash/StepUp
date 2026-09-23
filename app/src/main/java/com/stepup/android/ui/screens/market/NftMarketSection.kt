package com.stepup.android.ui.screens.market

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.stepup.android.R
import com.stepup.android.data.remote.ModelKey
import com.stepup.android.data.remote.MyBidRow
import com.stepup.android.data.remote.MySneakerRow
import com.stepup.android.data.remote.MyTradeRow
import com.stepup.android.ui.components.Eyebrow
import com.stepup.android.ui.components.GhostButton
import com.stepup.android.ui.components.GlowCard
import com.stepup.android.ui.components.SectionHeader
import com.stepup.android.ui.components.SneakerFrame
import com.stepup.android.ui.components.quietClickable
import com.stepup.android.ui.theme.CarbonHigh
import com.stepup.android.ui.theme.Silver
import com.stepup.android.ui.theme.Slate
import com.stepup.android.ui.theme.Snow
import com.stepup.android.ui.theme.Volt

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
) {
    item {
        GlowCard(contentPadding = MarketCardPadding, spacing = 12.dp) {
            MarketCardHead(
                title = stringResource(R.string.market_nft_head),
                sub = stringResource(R.string.market_nft_body),
            )
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(14.dp))
                    .background(CarbonHigh)
                    .padding(horizontal = 14.dp, vertical = 11.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    Text(
                        text = stringResource(R.string.market_tradable),
                        style = MaterialTheme.typography.bodySmall,
                        color = Silver,
                    )
                    // 폰에 보이는 잔고와 다를 수 있다. 감추지 않고 왜 다른지
                    // 적어 둔다 — 살 때 "모자랍니다"만 뜨면 화면이 거짓말한
                    // 것처럼 보인다.
                    Text(
                        text = stringResource(R.string.market_tradable_note),
                        fontSize = 10.sp,
                        color = Slate,
                    )
                }
                Text(
                    text = stringResource(R.string.price_sup, formatSup(board.tradable)),
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = Volt,
                )
            }
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
        item { MarketProblemNote(board.problem) }
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
            fontSize = 10.sp,
            color = Slate,
            lineHeight = 15.sp,
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

    item { SectionHeader(title = stringResource(R.string.market_my_listings)) }
    if (listed.isEmpty()) {
        item {
            GlowCard(contentPadding = MarketCardPadding) {
                EmptyNote(stringResource(R.string.market_my_listings_empty))
            }
        }
    } else {
        items(listed.size) { index -> MyListingRow(listed[index], onCancelListing) }
    }

    item { SectionHeader(title = stringResource(R.string.market_my_bids)) }
    if (bids.isEmpty()) {
        item {
            GlowCard(contentPadding = MarketCardPadding) {
                EmptyNote(stringResource(R.string.market_my_bids_empty))
            }
        }
    } else {
        items(bids.size) { index -> MyBidRowCard(bids[index], onCancelBid) }
    }

    item { SectionHeader(title = stringResource(R.string.market_my_trades)) }
    if (trades.isEmpty()) {
        item {
            GlowCard(contentPadding = MarketCardPadding) {
                EmptyNote(stringResource(R.string.market_my_trades_empty))
            }
        }
    } else {
        items(trades.size) { index -> MyTradeRowCard(trades[index]) }
    }
}

@Composable
private fun MyListingRow(row: MySneakerRow, onCancel: (Long) -> Unit) {
    GlowCard(contentPadding = MarketCardPadding, spacing = 11.dp) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(11.dp),
        ) {
            SneakerFrame(
                sneaker = previewSneaker(row.faction, row.rarity, row.variant, row.level),
                modifier = Modifier.size(44.dp),
                corner = 13.dp,
            )
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(
                    text = modelName(row.faction, row.rarity, row.variant),
                    style = MaterialTheme.typography.titleSmall,
                    color = Snow,
                    maxLines = 1,
                )
                Row(horizontalArrangement = Arrangement.spacedBy(5.dp)) {
                    Tag(stringResource(R.string.market_level, row.level))
                    Tag("#${row.mintNumber}")
                }
            }
            Text(
                text = stringResource(R.string.price_sup, formatSup(row.listingPrice ?: 0.0)),
                style = MaterialTheme.typography.bodySmall,
                fontWeight = FontWeight.Bold,
                color = Volt,
            )
        }
        val listingId = row.listingId
        if (listingId != null) {
            GhostButton(
                text = stringResource(R.string.market_cancel_listing),
                onClick = { onCancel(listingId) },
                modifier = Modifier.fillMaxWidth(),
            )
        }
    }
}

@Composable
private fun MyBidRowCard(row: MyBidRow, onCancel: (Long) -> Unit) {
    GlowCard(contentPadding = MarketCardPadding, spacing = 11.dp) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(11.dp),
        ) {
            SneakerFrame(
                sneaker = previewSneaker(row.faction, row.rarity, row.variant),
                modifier = Modifier.size(44.dp),
                corner = 13.dp,
            )
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(
                    text = modelName(row.faction, row.rarity, row.variant),
                    style = MaterialTheme.typography.titleSmall,
                    color = Snow,
                    maxLines = 1,
                )
                Tag(stringResource(R.string.market_min_level, row.minLevel))
            }
            Text(
                text = stringResource(R.string.price_sup, formatSup(row.price)),
                style = MaterialTheme.typography.bodySmall,
                fontWeight = FontWeight.Bold,
                color = Volt,
            )
        }
        // 입찰을 걸어 둔 동안 그 SUP 는 잠겨 있다. 거두면 곧바로 풀린다.
        Text(
            text = stringResource(R.string.market_bid_locked),
            fontSize = 10.sp,
            color = Slate,
            lineHeight = 15.sp,
        )
        GhostButton(
            text = stringResource(R.string.market_cancel_bid),
            onClick = { onCancel(row.id) },
            modifier = Modifier.fillMaxWidth(),
        )
    }
}

@Composable
private fun MyTradeRowCard(row: MyTradeRow) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(CarbonHigh)
            .padding(horizontal = 13.dp, vertical = 11.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
            Eyebrow(
                text = stringResource(
                    if (row.sold) R.string.market_trade_sold else R.string.market_trade_bought
                ),
            )
            Text(
                text = modelName(row.faction, row.rarity, row.variant),
                style = MaterialTheme.typography.bodySmall,
                color = Snow,
                maxLines = 1,
            )
        }
        Column(horizontalAlignment = Alignment.End, verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(
                text = stringResource(R.string.price_sup, formatSup(row.price)),
                style = MaterialTheme.typography.bodySmall,
                fontWeight = FontWeight.Bold,
                color = if (row.sold) Volt else Snow,
            )
            if (row.sold && row.fee > 0) {
                Text(
                    text = stringResource(R.string.market_fee_paid, formatSup(row.fee)),
                    fontSize = 9.sp,
                    color = Slate,
                )
            }
        }
    }
}
