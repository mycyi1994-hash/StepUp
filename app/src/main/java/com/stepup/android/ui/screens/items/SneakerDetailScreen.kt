package com.stepup.android.ui.screens.items

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Sell
import androidx.compose.material3.Icon
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.disabled
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.stepup.android.R
import com.stepup.android.ui.components.BarMeter
import com.stepup.android.ui.components.DarkIconButton
import com.stepup.android.ui.components.FactionChip
import com.stepup.android.ui.components.GhostButton
import com.stepup.android.ui.components.GlowCard
import com.stepup.android.ui.components.RarityChip
import com.stepup.android.ui.components.SneakerFrame
import com.stepup.android.ui.components.fullLabel
import com.stepup.android.ui.components.variantLabel
import com.stepup.android.ui.components.StatBar
import com.stepup.android.ui.components.label
import com.stepup.android.ui.components.quietClickable
import com.stepup.android.ui.components.tint
import com.stepup.android.ui.theme.CarbonHigh
import com.stepup.android.ui.theme.Edge
import com.stepup.android.ui.theme.OnVolt
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

    // 강화 자리는 "강화"를 눌러야 펼친다. 값을 미리 다 펼쳐 두면 관리
    // 버튼 넷이 화면 밖으로 밀린다. 펼치기 전에도 다음 레벨 비용은
    // 관리 카드에 한 줄로 남겨 둔다 — 눌러 보기 전에 알 수 있어야 한다.
    var enhanceOpen by rememberSaveable(sneakerId) { mutableStateOf(false) }

    val msgNoBalance = stringResource(R.string.toast_no_balance)
    val msgMaxLevel = stringResource(R.string.toast_max_level)
    val msgUpgraded = stringResource(R.string.toast_upgraded)
    val equippedFmt = stringResource(R.string.toast_equipped, "%s")

    LaunchedEffect(message) {
        val m = message ?: return@LaunchedEffect
        val text = when (m) {
            ItemsMessage.NotEnoughBalance -> msgNoBalance
            ItemsMessage.MaxLevel -> msgMaxLevel
            is ItemsMessage.Upgraded -> msgUpgraded
            is ItemsMessage.Equipped -> equippedFmt.format(m.sneaker.fullLabel(context))
            else -> null
        }
        if (text != null) Toast.makeText(context, text, Toast.LENGTH_SHORT).show()
        viewModel.consumeMessage()
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(start = 18.dp, end = 18.dp, top = 10.dp, bottom = 24.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                DarkIconButton(
                    icon = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = stringResource(R.string.cd_back),
                    onClick = onBack,
                )
                Text(
                    text = if (sneaker != null) sneaker.fullLabel() else "",
                    modifier = Modifier.weight(1f),
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Black,
                    letterSpacing = (-0.5).sp,
                    color = Snow,
                )
            }
        }

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
            return@LazyColumn
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
                        fontSize = 11.sp,
                        color = Slate,
                    )
                }
                SneakerFrame(
                    sneaker = sneaker,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(220.dp),
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
                        fontSize = 12.sp,
                        color = Silver,
                    )
                }
            }
        }

        // 스탯
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

        // ── 아이템 관리 — 장착 / 강화 / 조합 / 판매 ─────────────
        //
        // 넷을 한 자리에 모은다. 누르는 것만으로 자산이 움직이지 않는다 —
        // 강화는 비용을 보여 준 뒤 한 번 더 눌러야 하고, 판매는 마켓의
        // 등록 화면에서 값을 적고 확정해야 한다. 조합은 규칙이 정해지지
        // 않아 눌리지 않는다.
        item {
            GlowCard(contentPadding = PaddingValues(18.dp), spacing = 12.dp) {
                Text(
                    text = stringResource(R.string.sneaker_manage),
                    style = MaterialTheme.typography.titleMedium,
                    color = Snow,
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    ActionTile(
                        icon = Icons.Filled.CheckCircle,
                        label = stringResource(
                            if (sneaker.equipped) R.string.sneaker_action_equipped
                            else R.string.sneaker_action_equip
                        ),
                        primary = true,
                        // 이미 신고 있으면 다시 누를 일이 없다. 꺼 두는 것이
                        // 같은 요청을 두 번 보내는 것을 막는다.
                        enabled = !sneaker.equipped,
                        onClick = { viewModel.equip(sneaker.id) },
                        modifier = Modifier.weight(1f),
                    )
                    ActionTile(
                        icon = Icons.Filled.ArrowUpward,
                        label = stringResource(R.string.sneaker_action_enhance),
                        enabled = sneaker.canUpgrade,
                        onClick = { enhanceOpen = !enhanceOpen },
                        modifier = Modifier.weight(1f),
                    )
                }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    ActionTile(
                        icon = Icons.Filled.AutoAwesome,
                        label = stringResource(R.string.sneaker_action_fuse),
                        // 조합 규칙(자격·재료 수·비용·성공률·결과)이 아직
                        // 정해지지 않았다. 정해지기 전에 눌리게 두면 무엇이
                        // 사라지는지 모르는 채로 재료를 태우게 된다.
                        enabled = false,
                        onClick = {},
                        modifier = Modifier.weight(1f),
                    )
                    ActionTile(
                        icon = Icons.Filled.Sell,
                        label = stringResource(R.string.sneaker_action_sell),
                        subtitle = stringResource(R.string.sneaker_action_sell_sub),
                        // 신고 있는 신발은 팔 수 없다 — 다음 러닝의 부스트가
                        // 말없이 사라지기 때문이다. 이건 마켓이 이미 지키는
                        // 규칙이고, 여기서 따로 벗기지 않는다.
                        enabled = !sneaker.equipped,
                        onClick = {
                            onSell(
                                sneaker.faction.id,
                                sneaker.rarity.id,
                                sneaker.variant,
                                sneaker.id,
                            )
                        },
                        modifier = Modifier.weight(1f),
                    )
                }

                // 눌러 보기 전에 알아야 할 것 한 줄.
                if (sneaker.canUpgrade) {
                    val cost = sneaker.upgradeCost
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                    ) {
                        Text(
                            text = stringResource(R.string.items_next_level),
                            style = MaterialTheme.typography.bodySmall,
                            color = Silver,
                        )
                        Text(
                            text = "%,.0f / %,.0f SUP".format(balance.coerceAtMost(cost), cost),
                            style = MaterialTheme.typography.bodySmall,
                            fontWeight = FontWeight.Bold,
                            color = if (balance >= cost) Volt else Slate,
                        )
                    }
                } else {
                    Text(
                        text = stringResource(R.string.sneaker_enhance_max),
                        style = MaterialTheme.typography.bodySmall,
                        color = Volt,
                    )
                }
                Text(
                    text = stringResource(R.string.sneaker_fuse_pending),
                    fontSize = 10.sp,
                    color = Slate,
                    lineHeight = 16.sp,
                )
                if (sneaker.equipped) {
                    Text(
                        text = stringResource(R.string.sneaker_sell_equipped),
                        fontSize = 10.sp,
                        color = Slate,
                        lineHeight = 16.sp,
                    )
                }
            }
        }

        // ── 강화 ────────────────────────────────────────────────
        //
        // "강화"를 눌러야 펼쳐진다. 현재 레벨과 다음 레벨, 비용, 지금 가진
        // SUP 를 실제 값으로 보여 주고, 여기서 한 번 더 눌러야 차감된다.
        if (enhanceOpen && sneaker.canUpgrade) {
            item {
                val cost = sneaker.upgradeCost
                GlowCard(contentPadding = PaddingValues(18.dp), spacing = 12.dp) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                    ) {
                        Text(
                            text = stringResource(R.string.sneaker_action_enhance),
                            style = MaterialTheme.typography.titleSmall,
                            color = Snow,
                        )
                        Text(
                            text = stringResource(R.string.level_chip, sneaker.level) +
                                " → " + stringResource(R.string.level_chip, sneaker.level + 1),
                            style = MaterialTheme.typography.bodySmall,
                            fontWeight = FontWeight.Bold,
                            color = Volt,
                        )
                    }
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                    ) {
                        Text(
                            text = stringResource(R.string.items_next_level),
                            style = MaterialTheme.typography.bodySmall,
                            color = Silver,
                        )
                        Text(
                            text = "%,.0f / %,.0f SUP".format(balance.coerceAtMost(cost), cost),
                            style = MaterialTheme.typography.bodySmall,
                            fontWeight = FontWeight.Bold,
                            color = if (balance >= cost) Volt else Slate,
                        )
                    }
                    BarMeter(fraction = (balance / cost).coerceIn(0.0, 1.0).toFloat(), height = 7.dp)
                    GhostButton(
                        text = stringResource(R.string.items_upgrade_cost, "%,.0f".format(cost)),
                        onClick = { viewModel.upgrade(sneaker.id) },
                        enabled = balance >= cost,
                        modifier = Modifier.fillMaxWidth(),
                    )
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
}

/**
 * 관리 버튼 한 칸.
 *
 * 넷이 같은 크기로 서야 어느 것이 더 중요한지가 색으로만 읽힌다. 글자가
 * 길어지는 다른 언어에서도 두 줄까지 접혀 잘리지 않는다.
 */
@Composable
private fun ActionTile(
    icon: ImageVector,
    label: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    subtitle: String? = null,
    primary: Boolean = false,
    enabled: Boolean = true,
) {
    val shape = RoundedCornerShape(16.dp)
    val fill = when {
        !enabled -> CarbonHigh
        primary -> Volt
        else -> CarbonHigh
    }
    val ink = when {
        !enabled -> Slate
        primary -> OnVolt
        else -> Snow
    }
    Column(
        modifier = modifier
            .heightIn(min = 76.dp)
            .clip(shape)
            .background(fill, shape)
            .border(
                width = 1.dp,
                color = if (primary && enabled) Color.Transparent else Edge,
                shape = shape,
            )
            .then(if (enabled) Modifier.quietClickable(onClick) else Modifier)
            .semantics { if (!enabled) disabled() }
            .padding(horizontal = 10.dp, vertical = 12.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(5.dp, Alignment.CenterVertically),
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = ink,
            modifier = Modifier.size(19.dp),
        )
        Text(
            text = label,
            fontSize = 13.sp,
            fontWeight = FontWeight.Bold,
            color = ink,
            textAlign = TextAlign.Center,
            lineHeight = 17.sp,
            maxLines = 2,
        )
        if (subtitle != null) {
            Text(
                text = subtitle,
                fontSize = 9.sp,
                color = if (enabled && !primary) Slate else ink.copy(alpha = 0.75f),
                textAlign = TextAlign.Center,
                lineHeight = 13.sp,
                maxLines = 2,
            )
        }
    }
}
