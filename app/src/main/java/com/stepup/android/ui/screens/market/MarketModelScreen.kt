package com.stepup.android.ui.screens.market

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.Spacer
import androidx.compose.ui.Alignment
import com.stepup.android.ui.components.label
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.material.icons.filled.HourglassEmpty
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.stepup.android.R
import com.stepup.android.data.local.SneakerEntity
import com.stepup.android.data.remote.AskRow
import com.stepup.android.data.remote.BidRow
import com.stepup.android.data.remote.ModelKey
import com.stepup.android.data.remote.TradeRow
import com.stepup.android.ui.components.DetailPage
import com.stepup.android.ui.components.DialogPanel
import com.stepup.android.ui.components.FormField
import com.stepup.android.ui.components.PreferenceChoice
import com.stepup.android.ui.components.HairlineDivider
import com.stepup.android.ui.components.GhostButton
import com.stepup.android.ui.components.GlowCard
import com.stepup.android.ui.components.SectionHeader
import com.stepup.android.ui.components.SneakerFrame
import com.stepup.android.ui.components.VoltButton
import com.stepup.android.ui.components.quietClickable
import com.stepup.android.ui.theme.Silver
import com.stepup.android.ui.theme.Slate
import com.stepup.android.ui.theme.Snow
import com.stepup.android.ui.theme.VoltText

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

    // 팔 수 있는 신발 = 이 모델이면서 신고 있지 않고, 서버가 넘길 수 있다고 한 것.
    // 신고 있는 것을 팔면 다음 러닝의 부스트가 말없이 사라진다. 첫 신발 · 예전 신발 ·
    // 잠금 거리(50km) 전 · 이미 건 것 · 체인에 있는 것은 서버가 거절하므로 고르지 못하게 한다
    // (서버의 can_withdraw 가 같은 조건이다).
    val sellable = state.mySneakers.filter { !it.equipped && it.canWithdraw }

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
        // S2 머리 — 계열 · 등급 → 모델 이름 → 기울인 파란 면 위 신발
        item {
            val preview = previewSneaker(faction, rarity, variant)
            Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
                com.stepup.android.ui.components.S2Kicker(
                    preview.faction.label() + " · " + preview.rarity.label(),
                )
                Spacer(Modifier.height(10.dp))
                com.stepup.android.ui.components.S2Headline(modelName(faction, rarity, variant))
                Spacer(Modifier.height(16.dp))
                com.stepup.android.ui.components.S2ShoeStage(preview, Modifier.fillMaxWidth(0.86f))
            }
        }

        message?.let { note ->
            item {
                Box(Modifier.quietClickable { viewModel.consumeMessage() }) {
                    MarketMessageBar(note)
                }
            }
        }

        if (state.loading) {
            item {
                com.stepup.android.ui.components.StatePanel(
                    stringResource(R.string.feed_loading), androidx.compose.material.icons.Icons.Filled.HourglassEmpty,
                    loading = true,
                )
            }
            return@DetailPage
        }
        if (state.problem != null) {
            item { MarketProblemNote(state.problem!!, onRetry = viewModel::load) }
            return@DetailPage
        }

        // ── 시세 ──
        item {
            GlowCard(contentPadding = MarketCardPadding, spacing = 12.dp) {
                // S2 — 판매가 | 구매가 | 최근 거래가 한 줄. 값이 없으면 "—"
                com.stepup.android.ui.components.S2Stats(listOf(
                    stringResource(R.string.market_ask) to (state.quote?.ask?.let { formatSup(it) } ?: "—"),
                    stringResource(R.string.market_bid) to (state.quote?.bid?.let { formatSup(it) } ?: "—"),
                    stringResource(R.string.market_last) to (state.quote?.lastPrice?.let { formatSup(it) } ?: "—"),
                ), valueSize = 20.sp)
                Text(
                    text = stringResource(
                        R.string.market_quote_note,
                        state.quote?.supply ?: 0,
                        state.quote?.trades24h ?: 0,
                    ),
                    fontSize = 14.sp,
                    color = Slate,
                    lineHeight = 20.sp,
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
                        fontSize = 14.sp,
                        color = Slate,
                        lineHeight = 20.sp,
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

    if (bidding && !state.loading && state.problem == null) {
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

    if (asking && !state.loading && state.problem == null) {
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
    GlowCard(contentPadding = PaddingValues(18.dp), spacing = 12.dp) {
        Text(stringResource(R.string.price_sup, formatSup(row.price)),
            style = MaterialTheme.typography.titleLarge, color = Snow)
        Text(stringResource(R.string.market_level, row.level) + " · #${row.mintNumber} · " +
            stringResource(R.string.market_durability, row.durability), fontSize = 14.sp, color = Silver)
        VoltButton(stringResource(R.string.market_buy_now), onBuy, Modifier.fillMaxWidth())
    }
}

@Composable
private fun BidRowCard(row: BidRow, canSell: Boolean, onSell: () -> Unit) {
    GlowCard(contentPadding = PaddingValues(18.dp), spacing = 12.dp) {
        Text(stringResource(R.string.price_sup, formatSup(row.price)),
            style = MaterialTheme.typography.titleLarge, color = Snow)
        Text(stringResource(R.string.market_min_level, row.minLevel), fontSize = 14.sp, color = Silver)
        GhostButton(stringResource(R.string.market_sell_now), onSell, Modifier.fillMaxWidth(), enabled = canSell)
    }
}

@Composable
private fun TradeHistoryRow(row: TradeRow) {
    Column(Modifier.fillMaxWidth().padding(horizontal = 4.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Text(stringResource(R.string.market_level, row.level), fontSize = 14.sp,
                color = Silver, modifier = Modifier.weight(1f))
            Text(row.tradedAt.take(10), fontSize = 14.sp, color = Silver)
        }
        Text(stringResource(R.string.price_sup, formatSup(row.price)),
            style = MaterialTheme.typography.titleMedium, color = VoltText)
        HairlineDivider()
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
    DialogPanel(
        title = stringResource(R.string.market_place_bid), onDismiss = onDismiss,
        actions = {
            VoltButton(stringResource(R.string.market_place_bid), { onConfirm(level, amount) },
                Modifier.fillMaxWidth(), enabled = canSend)
        },
    ) {
        Text(stringResource(R.string.market_bid_help), style = MaterialTheme.typography.bodyLarge, color = Silver)
        NumberField(stringResource(R.string.market_price_label), price) { price = it.filter(Char::isDigit).take(9) }
        NumberField(stringResource(R.string.market_min_level_label), minLevel) { minLevel = it.filter(Char::isDigit).take(2) }
        Text(stringResource(R.string.market_tradable) + " " + stringResource(R.string.price_sup, formatSup(tradable)),
            fontSize = 14.sp, color = if (amount > tradable) VoltText else Silver)
    }
}

@Composable
private fun AskDialog(
    sneakers: List<SneakerEntity>,
    suggested: Double?,
    onDismiss: () -> Unit,
    onConfirm: (SneakerEntity, Double) -> Unit,
    preselect: Long = 0,
) {
    var price by remember { mutableStateOf(suggested?.toLong()?.toString() ?: "") }
    var picked by remember { mutableStateOf(sneakers.firstOrNull { it.id == preselect } ?: sneakers.firstOrNull()) }
    val amount = price.toDoubleOrNull() ?: 0.0
    val canSend = amount >= 1 && picked != null
    DialogPanel(
        title = stringResource(R.string.market_place_ask), onDismiss = onDismiss,
        actions = {
            VoltButton(stringResource(R.string.market_place_ask), { picked?.let { onConfirm(it, amount) } },
                Modifier.fillMaxWidth(), enabled = canSend)
        },
    ) {
        Text(stringResource(R.string.market_ask_help), style = MaterialTheme.typography.bodyLarge, color = Silver)
        NumberField(stringResource(R.string.market_price_label), price) { price = it.filter(Char::isDigit).take(9) }
        sneakers.forEach { sneaker ->
            PreferenceChoice(
                title = stringResource(R.string.market_level, sneaker.level) + " · #${sneaker.mintNumber}",
                description = stringResource(R.string.market_durability, sneaker.durability),
                selected = picked?.id == sneaker.id, onClick = { picked = sneaker },
            )
        }
    }
}

@Composable
private fun NumberField(label: String, value: String, onValueChange: (String) -> Unit) {
    FormField(label = label, value = value, onValueChange = onValueChange,
        placeholder = "0", keyboardType = KeyboardType.Number)
}
