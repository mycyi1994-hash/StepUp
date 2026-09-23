package com.stepup.android.ui.screens.items

import android.widget.Toast
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.TextButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.stepup.android.R
import com.stepup.android.ui.components.DetailPage
import com.stepup.android.ui.components.FactionChip
import com.stepup.android.ui.components.GhostButton
import com.stepup.android.ui.components.GlowCard
import com.stepup.android.ui.components.RarityChip
import com.stepup.android.ui.components.SneakerFrame
import com.stepup.android.ui.components.fullLabel
import com.stepup.android.ui.components.variantLabel
import com.stepup.android.ui.components.StatBar
import com.stepup.android.ui.components.tint
import com.stepup.android.ui.theme.Silver
import com.stepup.android.ui.theme.Slate
import com.stepup.android.ui.theme.Snow
import com.stepup.android.ui.theme.Volt

@Composable
fun SneakerDetailScreen(
    sneakerId: Long,
    onBack: () -> Unit = {},
    /** 이 신발을 들고 NFT 마켓플레이스의 판매 등록으로 — 등록은 거기서 확정한다 */
    onSell: (faction: String, rarity: String, variant: Int, localId: Long) -> Unit =
        { _, _, _, _ -> },
    viewModel: ItemsViewModel = viewModel(factory = ItemsViewModel.Factory),
) {
    val context = LocalContext.current
    val inventory by viewModel.inventory.collectAsStateWithLifecycle()
    val balance by viewModel.balance.collectAsStateWithLifecycle()
    val message by viewModel.message.collectAsStateWithLifecycle()
    val sneaker = inventory.firstOrNull { it.id == sneakerId }

    // Opening the confirmation never spends SUP; the confirmed repository write is atomic.
    var enhanceOpen by rememberSaveable(sneakerId) { mutableStateOf(false) }

    var statsOpen by rememberSaveable(sneakerId) { mutableStateOf(false) }

    val msgNoBalance = stringResource(R.string.toast_no_balance)
    val msgMaxLevel = stringResource(R.string.toast_max_level)
    val msgUpgraded = stringResource(R.string.toast_upgraded)
    val equippedFmt = stringResource(R.string.toast_equipped, "%s")

    LaunchedEffect(message) {
        val m = message ?: return@LaunchedEffect
        val text = when (m) {
            ItemsMessage.SaveFailed -> context.getString(R.string.feed_save_failed)
            ItemsMessage.NotEnoughBalance -> msgNoBalance
            ItemsMessage.MaxLevel -> msgMaxLevel
            is ItemsMessage.Upgraded -> msgUpgraded
            is ItemsMessage.Equipped -> equippedFmt.format(m.sneaker.fullLabel(context))
            else -> null
        }
        if (text != null) Toast.makeText(context, text, Toast.LENGTH_SHORT).show()
        viewModel.consumeMessage()
    }

    DetailPage(
        title = sneaker?.fullLabel() ?: stringResource(R.string.profile_my_sneakers),
        onBack = onBack,
        primaryActionLabel = sneaker?.let {
            stringResource(when {
                !it.equipped -> R.string.sneaker_action_equip
                it.canUpgrade -> R.string.sneaker_action_enhance
                else -> R.string.sneaker_action_equipped
            })
        },
        primaryActionEnabled = sneaker != null && (!sneaker.equipped || sneaker.canUpgrade),
        onPrimaryAction = {
            sneaker?.let {
                if (it.equipped) enhanceOpen = true else viewModel.equip(it.id)
            }
        },
    ) {
        if (sneaker == null) {
            item {
                GlowCard(contentPadding = PaddingValues(24.dp)) {
                    Text(
                        text = stringResource(R.string.common_none),
                        style = MaterialTheme.typography.bodyMedium,
                        color = Silver,
                    )
                }
            }
            return@DetailPage
        }

        // 히어로
        item {
            GlowCard(accent = true, contentPadding = PaddingValues(20.dp), spacing = 13.dp) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        FactionChip(sneaker.faction)
                        RarityChip(sneaker.rarity)
                    }
                    Text(
                        text = stringResource(R.string.sneaker_mint_no, sneaker.mintNumber),
                        fontSize = 14.sp,
                        color = Slate,
                    )
                }
                SneakerFrame(
                    sneaker = sneaker,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(180.dp),
                    corner = 18.dp,
                    animate = true,
                )
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(3.dp),
                ) {
                    Text(
                        text = sneaker.variantLabel(),
                        style = MaterialTheme.typography.titleMedium,
                        color = Snow,
                    )
                    Text(
                        text = stringResource(R.string.level_chip, sneaker.level) +
                            " · " + stringResource(R.string.items_owned),
                        fontSize = 14.sp,
                        color = Silver,
                    )
                }
            }
        }

        item {
            GhostButton(
                text = stringResource(R.string.sneaker_stats),
                onClick = { statsOpen = !statsOpen },
                modifier = Modifier.fillMaxWidth(),
            )
        }
        // 스탯
        if (statsOpen) {
            item {
                GlowCard(contentPadding = PaddingValues(18.dp), spacing = 14.dp) {
                    Text(
                        text = stringResource(R.string.sneaker_stats),
                        style = MaterialTheme.typography.titleMedium,
                        color = Snow,
                    )
                    StatBar(
                        label = stringResource(R.string.sneaker_earning),
                        value = sneaker.boostPercent,
                        max = 20.0,
                        accent = sneaker.faction.tint(),
                        display = "+%.1f%%".format(sneaker.boostPercent),
                    )
                    StatBar(
                        label = stringResource(R.string.stat_luck),
                        value = sneaker.luck,
                        max = 2.5,
                        accent = sneaker.rarity.tint(),
                    )
                    StatBar(
                        label = stringResource(R.string.stat_comfort),
                        value = sneaker.comfort,
                        max = 2.5,
                        accent = sneaker.rarity.tint(),
                    )
                    StatBar(
                        label = stringResource(R.string.sneaker_energy_saving),
                        value = (1.0 - sneaker.energyEfficiency) * 100,
                        max = 15.0,
                        accent = Volt,
                        display = "%.0f%%".format((1.0 - sneaker.energyEfficiency) * 100),
                    )
                    StatBar(
                        label = stringResource(R.string.sneaker_durability),
                        value = sneaker.durability.toDouble(),
                        max = 100.0,
                        accent = Volt,
                        display = "${sneaker.durability}/100",
                    )
                }
            }
        }
        item {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                if (!sneaker.canUpgrade) {
                    Text(stringResource(R.string.sneaker_enhance_max),
                        style = MaterialTheme.typography.bodyMedium, color = Silver)
                }
                if (!sneaker.equipped && sneaker.canUpgrade) {
                    GhostButton(
                        text = stringResource(R.string.sneaker_action_enhance),
                        onClick = { enhanceOpen = true },
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
                GhostButton(
                    text = stringResource(R.string.sneaker_action_sell),
                    onClick = {
                        onSell(sneaker.faction.id, sneaker.rarity.id, sneaker.variant, sneaker.id)
                    },
                    enabled = !sneaker.equipped,
                    modifier = Modifier.fillMaxWidth(),
                )
                if (sneaker.equipped) {
                    Text(stringResource(R.string.sneaker_sell_equipped),
                        style = MaterialTheme.typography.bodyMedium, color = Silver)
                }
            }
        }

        // 설명
        item {
            GlowCard(contentPadding = PaddingValues(18.dp), spacing = 8.dp) {
                Text(
                    text = stringResource(R.string.sneaker_about),
                    style = MaterialTheme.typography.titleSmall,
                    color = Snow,
                )
                Text(
                    text = stringResource(R.string.sneaker_about_body),
                    style = MaterialTheme.typography.bodySmall,
                    color = Silver,
                )
            }
        }
    }
    if (enhanceOpen && sneaker != null && sneaker.canUpgrade) {
        val cost = sneaker.upgradeCost
        AlertDialog(
            onDismissRequest = { enhanceOpen = false },
            title = { Text(stringResource(R.string.sneaker_action_enhance)) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text(stringResource(R.string.level_chip, sneaker.level) + " → " +
                        stringResource(R.string.level_chip, sneaker.level + 1))
                    Text(stringResource(R.string.items_upgrade_cost, "%,.0f".format(cost)))
                    if (balance < cost) Text(msgNoBalance)
                }
            },
            confirmButton = {
                TextButton(
                    enabled = balance >= cost,
                    onClick = {
                        enhanceOpen = false
                        viewModel.upgrade(sneaker.id)
                    },
                ) { Text(stringResource(R.string.sneaker_action_enhance)) }
            },
            dismissButton = {
                TextButton(onClick = { enhanceOpen = false }) {
                    Text(stringResource(R.string.common_cancel))
                }
            },
        )
    }

}
