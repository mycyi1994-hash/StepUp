package com.stepup.android.ui.screens.rewards

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.DirectionsWalk
import androidx.compose.material.icons.filled.AccountBalanceWallet
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.LockOpen
import androidx.compose.material.icons.filled.Receipt
import androidx.compose.material.icons.filled.SwapHoriz
import androidx.compose.material.icons.filled.Groups
import androidx.compose.material.icons.filled.Redeem
import androidx.compose.material.icons.filled.Whatshot
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Upgrade
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.platform.LocalContext
import com.stepup.android.core.ExternalIntents
import com.stepup.android.ui.components.VoltButton
import kotlinx.coroutines.launch
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.foundation.Canvas
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.stepup.android.R
import com.stepup.android.data.local.RewardEntity
import com.stepup.android.data.local.RewardType
import com.stepup.android.ui.components.GlowCard
import com.stepup.android.ui.components.HexEmblem
import com.stepup.android.ui.components.formatSupDown
import com.stepup.android.ui.components.IconSquare
import com.stepup.android.ui.components.VerticalHairline
import com.stepup.android.ui.theme.Alert
import com.stepup.android.ui.theme.OnVolt
import com.stepup.android.ui.theme.Silver
import com.stepup.android.ui.theme.Slate
import com.stepup.android.ui.theme.Snow
import com.stepup.android.ui.theme.Volt
import com.stepup.android.ui.theme.VoltPlate
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

private val ledgerTimeFormatter: DateTimeFormatter =
    DateTimeFormatter.ofPattern("M.d HH:mm").withZone(ZoneId.systemDefault())

@Composable
fun WalletScreen(
    onBack: () -> Unit = {},
    viewModel: RewardsViewModel = viewModel(factory = RewardsViewModel.Factory),
) {
    val totals by viewModel.totals.collectAsStateWithLifecycle()
    val ledger by viewModel.ledger.collectAsStateWithLifecycle()

    val entries = ledger.orEmpty()

    com.stepup.android.ui.components.DetailPage(
        title = stringResource(R.string.settings_wallet), onBack = onBack,
    ) {
        // S2 — 파란 한 줄 → 가는 큰 잔액(내림) → 안내 → 번 SUP | 쓴 SUP
        item {
            Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
                com.stepup.android.ui.components.S2Kicker(stringResource(R.string.wallet_balance))
                com.stepup.android.ui.components.S2Number(
                    totals?.balance?.let { formatSupDown(it, 2) } ?: "—", 64.sp,
                    Modifier.padding(top = 10.dp, bottom = 2.dp),
                )
                Text("SUP", color = Silver, fontSize = 14.sp)
                Spacer(Modifier.size(10.dp))
                com.stepup.android.ui.components.S2Subtitle(stringResource(R.string.wallet_tagline))
                Spacer(Modifier.size(18.dp))
                com.stepup.android.ui.components.S2Stats(listOf(
                    stringResource(R.string.wallet_earned) to (totals?.earned?.let { "+%,.2f".format(it) } ?: "—"),
                    stringResource(R.string.wallet_spent) to (totals?.spent?.let { if (it == 0.0) "0.00" else "-%,.2f".format(it) } ?: "—"),
                ), valueSize = 22.sp)
            }
        }

        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 4.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = stringResource(R.string.wallet_history),
                    style = MaterialTheme.typography.titleMedium,
                    color = Snow,
                )
                Text(
                    text = ledger?.let { stringResource(R.string.wallet_records, it.size) } ?: "—",
                    style = MaterialTheme.typography.bodyMedium,
                    color = Slate,
                )
            }
        }

        if (ledger == null) {
            item { com.stepup.android.ui.components.StatePanel(stringResource(R.string.feed_loading), Icons.Filled.Receipt, loading = true) }
        } else if (entries.isEmpty()) {
            item {
                GlowCard(contentPadding = PaddingValues(26.dp), spacing = 12.dp) {
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(5.dp),
                    ) {
                        IconSquare(icon = Icons.Filled.Receipt, size = 56.dp)
                        Spacer(Modifier.size(8.dp))
                        Text(
                            text = stringResource(R.string.wallet_empty_title),
                            style = MaterialTheme.typography.titleSmall,
                            color = Snow,
                        )
                        Text(
                            text = stringResource(R.string.wallet_empty_body),
                            style = MaterialTheme.typography.bodyMedium,
                            color = Silver,
                        )
                    }
                }
            }
        } else {
            items(entries, key = { it.id }) { entry ->
                LedgerRow(entry)
            }
        }
        item {
            val context = LocalContext.current
            val scope = rememberCoroutineScope()
            var notice by remember { mutableStateOf<Int?>(null) }
            GiwaCard(
                canOpen = viewModel.walletPageAvailable,
                notice = notice,
                onOpen = {
                    notice = null
                    scope.launch {
                        when (val link = viewModel.walletPageLink()) {
                            is WalletPageLink.Open -> ExternalIntents.openUrl(context, link.url)
                            WalletPageLink.SignIn -> notice = R.string.wallet_web_sign_in
                            WalletPageLink.Offline -> notice = R.string.wallet_web_offline
                        }
                    }
                },
            )
        }
    }
}

@Composable
private fun GiwaCard(canOpen: Boolean, notice: Int?, onOpen: () -> Unit) {
    GlowCard(spacing = 10.dp) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            IconSquare(icon = Icons.Filled.AccountBalanceWallet, size = 42.dp)
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(stringResource(R.string.wallet_giwa), color = Snow, style = MaterialTheme.typography.titleMedium)
                Text(stringResource(R.string.wallet_giwa_status), color = Silver, fontSize = 14.sp)
            }
        }
        Text(stringResource(R.string.wallet_giwa_body), color = Silver, fontSize = 14.sp)
        if (canOpen) {
            VoltButton(
                text = stringResource(R.string.wallet_web_open),
                onClick = onOpen,
                modifier = Modifier.fillMaxWidth(),
            )
        }
        if (notice != null) {
            Text(stringResource(notice), color = Silver, fontSize = 14.sp)
        }
    }
}

@Composable
private fun LedgerRow(entry: RewardEntity) {
    val (icon, labelRes) = when (entry.type) {
        RewardType.EARN_WALK -> Icons.AutoMirrored.Filled.DirectionsWalk to R.string.ledger_earn_walk
        RewardType.BONUS_GOAL -> Icons.Filled.EmojiEvents to R.string.ledger_bonus_goal
        RewardType.SPEND_UPGRADE -> Icons.Filled.Upgrade to R.string.ledger_spend_upgrade
        RewardType.SPEND_MINT -> Icons.Filled.AutoAwesome to R.string.ledger_spend_mint
        RewardType.SPEND_BOOST -> Icons.Filled.Whatshot to R.string.ledger_spend_boost
        RewardType.EARN_EVENT -> Icons.Filled.Redeem to R.string.ledger_earn_event
        RewardType.EARN_PARTY -> Icons.Filled.Groups to R.string.ledger_earn_party
        RewardType.TRADE_BUY -> Icons.Filled.SwapHoriz to R.string.ledger_trade_buy
        RewardType.TRADE_SELL -> Icons.Filled.SwapHoriz to R.string.ledger_trade_sell
        RewardType.TRADE_FEE -> Icons.Filled.Receipt to R.string.ledger_trade_fee
        RewardType.ESCROW_LOCK -> Icons.Filled.Lock to R.string.ledger_escrow_lock
        RewardType.ESCROW_UNLOCK -> Icons.Filled.LockOpen to R.string.ledger_escrow_unlock
        // 서버 경제(0022 · 0025)의 줄
        "SPEND_DRAW" -> Icons.Filled.AutoAwesome to R.string.ledger_spend_draw
        "SPEND_REPAIR" -> Icons.Filled.Upgrade to R.string.ledger_spend_repair
        "EARN_COURSE" -> Icons.Filled.Redeem to R.string.ledger_earn_course
        "EARN_INVITE" -> Icons.Filled.Groups to R.string.ledger_earn_invite
        "CHAIN_WITHDRAW", "CHAIN_REFUND", "CHAIN_DEPOSIT" -> Icons.Filled.SwapHoriz to R.string.ledger_chain
        com.stepup.android.data.repo.EconomySync.CARRIED_OVER -> Icons.Filled.Receipt to R.string.ledger_carried_over
        else -> Icons.Filled.EmojiEvents to R.string.ledger_other
    }
    GlowCard(
        contentPadding = PaddingValues(horizontal = 15.dp, vertical = 13.dp),
        shape = RoundedCornerShape(18.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(13.dp),
        ) {
            IconSquare(icon = icon, size = 38.dp, tint = if (entry.amount >= 0) Volt else Alert)
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(2.dp),
            ) {
                Text(
                    text = stringResource(labelRes),
                    style = MaterialTheme.typography.bodyMedium,
                    color = Snow,
                )
                Text(
                    text = ledgerTimeFormatter.format(Instant.ofEpochMilli(entry.timestamp)),
                    style = MaterialTheme.typography.bodyMedium,
                    color = Slate,
                )
            }
            Text(
                text = (if (entry.amount >= 0) "+" else "") + "%,.2f".format(entry.amount),
                fontSize = 15.sp,
                fontWeight = FontWeight.ExtraBold,
                color = if (entry.amount >= 0) Volt else Alert,
            )
        }
    }
}
