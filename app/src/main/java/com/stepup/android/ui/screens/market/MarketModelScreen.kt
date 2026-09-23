package com.stepup.android.ui.screens.market

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.stepup.android.R
import com.stepup.android.data.local.SneakerEntity
import com.stepup.android.data.remote.AskRow
import com.stepup.android.data.remote.BidRow
import com.stepup.android.data.remote.ModelKey
import com.stepup.android.data.remote.TradeRow
import com.stepup.android.ui.components.DetailPage
import com.stepup.android.ui.components.Eyebrow
import com.stepup.android.ui.components.GhostButton
import com.stepup.android.ui.components.GlowCard
import com.stepup.android.ui.components.SectionHeader
import com.stepup.android.ui.components.SneakerFrame
import com.stepup.android.ui.components.VoltButton
import com.stepup.android.ui.components.quietClickable
import com.stepup.android.ui.theme.Carbon
import com.stepup.android.ui.theme.CarbonHigh
import com.stepup.android.ui.theme.Edge
import com.stepup.android.ui.theme.Silver
import com.stepup.android.ui.theme.Slate
import com.stepup.android.ui.theme.Snow
import com.stepup.android.ui.theme.Volt

/**
 * 모델 하나의 장부.
 *
 * 크림의 상품 페이지와 오픈시의 컬렉션 페이지가 하는 일을 한 화면에 담았다.
 *
 *   * 위쪽 — 시세 세 값. 즉시 구매가 · 즉시 판매가 · 최근 체결가.
 *   * 가운데 — 실제 매물과 구매 입찰. 줄마다 레벨과 민팅 번호가 다르다.
 *   * 아래 — 체결 내역. 시세가 어떻게 움직였는지는 이것뿐이다.
 *
 * 값을 거는 길은 둘이다. **기다리는 쪽**(판매 등록 · 구매 입찰)과 **바로
 * 하는 쪽**(구매 · 판매). 둘을 한 화면에 두는 것이 중요하다 — 기다릴지
 * 지금 할지는 시세를 보고 정하는 일이기 때문이다.
 */
@Composable
fun MarketModelScreen(
    faction: String,
    rarity: String,
    variant: Int,
    onBack: () -> Unit,
    /**
     * 보관함에서 "판매"로 들어왔을 때 그 신발의 로컬 번호. 0이면 그냥
     * 들어온 것이다. 값이 있으면 판매 등록 창을 그 켤레로 열어 준다 —
     * 창이 열릴 뿐이고, 값을 적고 확정해야 매물이 올라간다.
     */
    sellLocalId: Long = 0,
    viewModel: MarketModelViewModel = viewModel(factory = MarketModelViewModel.Factory),
) {
    val model = remember(faction, rarity, variant) { ModelKey(faction, rarity, variant) }
    val state by viewModel.state.collectAsStateWithLifecycle()
    val message by viewModel.message.collectAsStateWithLifecycle()

    LaunchedEffect(model) { viewModel.open(model) }

    var asking by remember { mutableStateOf(false) }
    var bidding by remember { mutableStateOf(false) }

    // 팔 수 있는 신발 = 이 모델이면서 신고 있지 않은 것.
    // 신고 있는 것을 팔면 다음 러닝의 부스트가 말없이 사라진다.
    val sellable = state.mySneakers.filter { !it.equipped }

    // 보관함에서 판매로 들어왔으면 등록 창을 한 번 열어 준다. 장부를
    // 받아 온 뒤에 여는 것은, 그 전에는 고를 신발 목록이 비어 있어 창이
    // 빈 채로 뜨기 때문이다. 한 번 열고 나면 다시 열지 않는다 — 사용자가
    // 닫은 창을 앱이 도로 여는 것은 닫기를 무시하는 것이다.
    var sellOpened by rememberSaveable(sellLocalId) { mutableStateOf(false) }
    LaunchedEffect(sellLocalId, sellable) {
        if (sellLocalId > 0 && !sellOpened && sellable.any { it.id == sellLocalId }) {
            sellOpened = true
            asking = true
        }
    }

    DetailPage(title = modelName(faction, rarity, variant), onBack = onBack) {
        item {
            SneakerFrame(
                sneaker = previewSneaker(faction, rarity, variant),
                modifier = Modifier.fillMaxWidth().height(160.dp),
                corner = 24.dp,
            )
        }

        message?.let { note ->
            item {
                Box(Modifier.quietClickable { viewModel.consumeMessage() }) {
                    MarketMessageBar(note)
                }
            }
        }

        if (state.problem != null) {
            item { MarketProblemNote(state.problem!!) }
            return@DetailPage
        }

        // ── 시세 ──
        item {
            GlowCard(contentPadding = MarketCardPadding, spacing = 12.dp) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    PriceCell(
                        label = stringResource(R.string.market_ask),
                        value = state.quote?.ask,
                        modifier = Modifier.weight(1f),
                        accent = true,
                    )
                    PriceCell(
                        label = stringResource(R.string.market_bid),
                        value = state.quote?.bid,
                        modifier = Modifier.weight(1f),
                    )
                    PriceCell(
                        label = stringResource(R.string.market_last),
                        value = state.quote?.lastPrice,
                        modifier = Modifier.weight(1f),
                    )
                }
                Text(
                    text = stringResource(
                        R.string.market_quote_note,
                        state.quote?.supply ?: 0,
                        state.quote?.trades24h ?: 0,
                    ),
                    fontSize = 10.sp,
                    color = Slate,
                    lineHeight = 15.sp,
                )
                Row(horizontalArrangement = Arrangement.spacedBy(9.dp)) {
                    VoltButton(
                        text = stringResource(R.string.market_place_bid),
                        onClick = { bidding = true },
                        modifier = Modifier.weight(1f),
                    )
                    GhostButton(
                        text = stringResource(R.string.market_place_ask),
                        onClick = { asking = true },
                        enabled = sellable.isNotEmpty(),
                        modifier = Modifier.weight(1f),
                    )
                }
                if (sellable.isEmpty()) {
                    Text(
                        text = stringResource(R.string.market_nothing_to_sell),
                        fontSize = 10.sp,
                        color = Slate,
                        lineHeight = 15.sp,
                    )
                }
            }
        }

        // ── 매물 ──
        item { SectionHeader(title = stringResource(R.string.market_asks_title)) }
        val asks = state.book?.asks.orEmpty()
        if (asks.isEmpty()) {
            item {
                GlowCard(contentPadding = MarketCardPadding) {
                    EmptyNote(stringResource(R.string.market_asks_empty))
                }
            }
        } else {
            items(asks.size) { index ->
                AskRowCard(asks[index], onBuy = { viewModel.buy(asks[index].listingId) })
            }
        }

        // ── 구매 입찰 ──
        item { SectionHeader(title = stringResource(R.string.market_bids_title)) }
        val bids = state.book?.bids.orEmpty()
        if (bids.isEmpty()) {
            item {
                GlowCard(contentPadding = MarketCardPadding) {
                    EmptyNote(stringResource(R.string.market_bids_empty))
                }
            }
        } else {
            items(bids.size) { index ->
                val bid = bids[index]
                // 레벨 조건을 채우는 내 신발이 있어야 팔 수 있다.
                // 조건이 모자라면 버튼을 꺼 둔다 — 눌렀을 때 거절되는 것보다 낫다.
                val match = sellable.firstOrNull { it.level >= bid.minLevel }
                BidRowCard(
                    row = bid,
                    canSell = match != null,
                    onSell = { match?.let { viewModel.sell(it.id, bid.bidId) } },
                )
            }
        }

        // ── 체결 내역 ──
        item { SectionHeader(title = stringResource(R.string.market_history_title)) }
        val history = state.book?.history.orEmpty()
        if (history.isEmpty()) {
            item {
                GlowCard(contentPadding = MarketCardPadding) {
                    EmptyNote(stringResource(R.string.market_history_empty))
                }
            }
        } else {
            items(history.size) { index -> TradeHistoryRow(history[index]) }
        }
    }

    if (bidding) {
        BidDialog(
            tradable = state.tradable,
            suggested = state.quote?.bid ?: state.quote?.lastPrice,
            onDismiss = { bidding = false },
            onConfirm = { minLevel, price ->
                bidding = false
                viewModel.bid(minLevel, price)
            },
        )
    }

    if (asking) {
        AskDialog(
            sneakers = sellable,
            preselect = sellLocalId,
            suggested = state.quote?.ask ?: state.quote?.lastPrice,
            onDismiss = { asking = false },
            onConfirm = { sneaker, price ->
                asking = false
                viewModel.listForSale(sneaker.id, price)
            },
        )
    }
}

// ── 줄 ──────────────────────────────────────────────────────────────

@Composable
private fun AskRowCard(row: AskRow, onBuy: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(18.dp))
            .background(CarbonHigh)
            .padding(horizontal = 13.dp, vertical = 11.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(11.dp),
    ) {
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(5.dp)) {
            Text(
                text = stringResource(R.string.price_sup, formatSup(row.price)),
                style = MaterialTheme.typography.titleSmall,
                color = Snow,
            )
            Row(horizontalArrangement = Arrangement.spacedBy(5.dp)) {
                Tag(stringResource(R.string.market_level, row.level), accent = true)
                Tag("#${row.mintNumber}")
                Tag(stringResource(R.string.market_durability, row.durability))
            }
        }
        VoltButton(
            text = stringResource(R.string.market_buy_now),
            onClick = onBuy,
            modifier = Modifier.padding(start = 4.dp),
        )
    }
}

@Composable
private fun BidRowCard(row: BidRow, canSell: Boolean, onSell: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(18.dp))
            .background(CarbonHigh)
            .padding(horizontal = 13.dp, vertical = 11.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(11.dp),
    ) {
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(5.dp)) {
            Text(
                text = stringResource(R.string.price_sup, formatSup(row.price)),
                style = MaterialTheme.typography.titleSmall,
                color = Snow,
            )
            Tag(stringResource(R.string.market_min_level, row.minLevel))
        }
        GhostButton(
            text = stringResource(R.string.market_sell_now),
            onClick = onSell,
            enabled = canSell,
        )
    }
}

@Composable
private fun TradeHistoryRow(row: TradeRow) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(CarbonHigh.copy(alpha = 0.6f))
            .padding(horizontal = 13.dp, vertical = 9.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Text(
            text = stringResource(R.string.market_level, row.level),
            fontSize = 11.sp,
            color = Silver,
        )
        Text(
            text = row.tradedAt.take(10),
            fontSize = 10.sp,
            color = Slate,
        )
        Text(
            text = stringResource(R.string.price_sup, formatSup(row.price)),
            style = MaterialTheme.typography.bodySmall,
            fontWeight = FontWeight.Bold,
            color = Volt,
        )
    }
}

// ── 값 거는 창 ──────────────────────────────────────────────────────

@Composable
private fun BidDialog(
    tradable: Double,
    suggested: Double?,
    onDismiss: () -> Unit,
    onConfirm: (minLevel: Int, price: Double) -> Unit,
) {
    var price by remember { mutableStateOf(suggested?.toLong()?.toString() ?: "") }
    var minLevel by remember { mutableStateOf("1") }
    val amount = price.toDoubleOrNull() ?: 0.0
    val level = minLevel.toIntOrNull() ?: 1
    val canSend = amount >= 1 && amount <= tradable && level in 1..30

    MarketDialog(onDismiss = onDismiss) {
        DialogTitle(stringResource(R.string.market_place_bid))
        Text(
            text = stringResource(R.string.market_bid_help),
            style = MaterialTheme.typography.bodySmall,
            color = Silver,
            lineHeight = 18.sp,
        )
        NumberField(
            label = stringResource(R.string.market_price_label),
            value = price,
            onValueChange = { price = it.filter(Char::isDigit).take(9) },
        )
        NumberField(
            label = stringResource(R.string.market_min_level_label),
            value = minLevel,
            onValueChange = { minLevel = it.filter(Char::isDigit).take(2) },
        )
        Text(
            text = stringResource(R.string.market_tradable) + " " +
                stringResource(R.string.price_sup, formatSup(tradable)),
            fontSize = 11.sp,
            color = if (amount > tradable) Volt else Slate,
        )
        VoltButton(
            text = stringResource(R.string.market_place_bid),
            onClick = { onConfirm(level, amount) },
            enabled = canSend,
            modifier = Modifier.fillMaxWidth(),
        )
    }
}

@Composable
private fun AskDialog(
    sneakers: List<SneakerEntity>,
    suggested: Double?,
    onDismiss: () -> Unit,
    onConfirm: (SneakerEntity, Double) -> Unit,
    /** 보관함에서 고르고 온 켤레. 목록에 없으면 첫 켤레를 고른다. */
    preselect: Long = 0,
) {
    var price by remember { mutableStateOf(suggested?.toLong()?.toString() ?: "") }
    var picked by remember {
        mutableStateOf(
            sneakers.firstOrNull { it.id == preselect } ?: sneakers.firstOrNull(),
        )
    }
    val amount = price.toDoubleOrNull() ?: 0.0
    val canSend = amount >= 1 && picked != null

    MarketDialog(onDismiss = onDismiss) {
        DialogTitle(stringResource(R.string.market_place_ask))
        Text(
            text = stringResource(R.string.market_ask_help),
            style = MaterialTheme.typography.bodySmall,
            color = Silver,
            lineHeight = 18.sp,
        )
        // 어느 켤레를 파는지 골라야 한다. 같은 모델이라도 레벨이 다르면
        // 값이 다르고, 파는 것은 한 켤레다.
        sneakers.forEach { sneaker ->
            val selected = picked?.id == sneaker.id
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(14.dp))
                    .background(if (selected) Volt.copy(alpha = 0.12f) else CarbonHigh)
                    .quietClickable { picked = sneaker }
                    .padding(horizontal = 12.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(9.dp),
            ) {
                Text(
                    text = stringResource(R.string.market_level, sneaker.level),
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = if (selected) Volt else Snow,
                )
                Text(
                    text = stringResource(R.string.market_durability, sneaker.durability),
                    fontSize = 11.sp,
                    color = Silver,
                    modifier = Modifier.weight(1f),
                )
                Text(text = "#${sneaker.mintNumber}", fontSize = 10.sp, color = Slate)
            }
        }
        NumberField(
            label = stringResource(R.string.market_price_label),
            value = price,
            onValueChange = { price = it.filter(Char::isDigit).take(9) },
        )
        VoltButton(
            text = stringResource(R.string.market_place_ask),
            onClick = { picked?.let { onConfirm(it, amount) } },
            enabled = canSend,
            modifier = Modifier.fillMaxWidth(),
        )
    }
}

@Composable
private fun MarketDialog(onDismiss: () -> Unit, content: @Composable () -> Unit) {
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false),
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .quietClickable(onDismiss)
                .padding(22.dp),
            contentAlignment = Alignment.Center,
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    // 창 본체를 눌렀을 때 배경으로 새어 나가 닫히지 않게 흡수한다
                    .quietClickable { }
                    .clip(RoundedCornerShape(24.dp))
                    .background(Carbon)
                    .border(1.dp, Edge, RoundedCornerShape(24.dp))
                    .imePadding()
                    .padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                content()
            }
        }
    }
}

/**
 * 숫자만 받는 칸.
 *
 * Material 의 TextField 를 쓰지 않는 것은 이 앱의 다른 입력과 같은 모양을
 * 지키기 위해서다 — 한 화면 안에서 입력칸이 두 가지 모양이면 다른 앱처럼 보인다.
 */
@Composable
private fun NumberField(label: String, value: String, onValueChange: (String) -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(5.dp)) {
        Text(text = label, fontSize = 10.sp, color = Slate)
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(14.dp))
                .background(CarbonHigh)
                .padding(horizontal = 14.dp, vertical = 12.dp),
        ) {
            BasicTextField(
                value = value,
                onValueChange = onValueChange,
                textStyle = TextStyle(color = Snow, fontSize = 15.sp, fontWeight = FontWeight.Bold),
                cursorBrush = SolidColor(Volt),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
            )
            if (value.isEmpty()) {
                Text(text = "0", fontSize = 15.sp, color = Slate)
            }
        }
    }
}
