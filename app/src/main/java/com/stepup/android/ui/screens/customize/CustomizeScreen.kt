package com.stepup.android.ui.screens.customize

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.SwapHoriz
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.stepup.android.R
import com.stepup.android.domain.Sneaker
import com.stepup.android.ui.components.*
import com.stepup.android.ui.experience.feedbackClickable
import com.stepup.android.ui.guide.GuideTour
import com.stepup.android.ui.guide.guideTarget
import com.stepup.android.ui.screens.items.ItemsMessage
import com.stepup.android.ui.screens.items.ItemsViewModel
import com.stepup.android.ui.theme.*

/** Shoe selection uses owned copies only. Preview never changes the equipped pair. */
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
    viewModel: ItemsViewModel = viewModel(factory = ItemsViewModel.Factory),
) {
    val loadedInventory by viewModel.selectionInventory.collectAsStateWithLifecycle()
    val inventory = loadedInventory.orEmpty()
    val ready = loadedInventory != null
    val message by viewModel.message.collectAsStateWithLifecycle()
    var selectedId by rememberSaveable { mutableStateOf<Long?>(null) }
    val saving by viewModel.equipping.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val equipped = inventory.firstOrNull { it.equipped }
    val selected = inventory.firstOrNull { it.id == selectedId } ?: equipped ?: inventory.firstOrNull()
    val columns = if (LocalDensity.current.fontScale > 1.3f) 1 else 2
    LaunchedEffect(message) {
        val text = when (val result = message) {
            is ItemsMessage.Equipped -> context.getString(R.string.toast_equipped, result.sneaker.fullLabel(context))
            ItemsMessage.SaveFailed -> context.getString(R.string.feed_save_failed)
            ItemsMessage.SignInRequired -> context.getString(R.string.toast_sign_in_required)
            ItemsMessage.Offline -> context.getString(R.string.toast_offline)
            else -> null
        }
        if (message != null) {
            text?.let { Toast.makeText(context, it, Toast.LENGTH_SHORT).show() }
            viewModel.consumeMessage()
        }
    }
    // S2 신발 — 신발 한 켤레가 화면 가운데. 고르는 것은 미리 보기이고, 원형 버튼을 눌러야 신는다.
    Column(Modifier.fillMaxSize().padding(horizontal = StepUpDesign.Gutter).padding(bottom = 12.dp)) {
        LazyColumn(Modifier.weight(1f).fillMaxWidth(), contentPadding = PaddingValues(bottom = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)) {
            if (!ready) item {
                StatePanel(stringResource(R.string.feed_loading), StepUpIcons.Shoe, loading = true)
            } else if (selected == null) item {
                StatePanel(stringResource(R.string.customize_no_shoes), StepUpIcons.Shoe,
                    action = { GhostButton(stringResource(R.string.customize_open_vault), onClick = onOpenVault) })
            } else {
                item {
                    Column(
                        Modifier.fillMaxWidth().testTag("shoe-preview").guideTarget(GuideTour.Targets.CUSTOMIZE_PREVIEW),
                        horizontalAlignment = Alignment.CenterHorizontally,
                    ) {
                        Spacer(Modifier.height(12.dp))
                        S2Kicker(stringResource(if (selected.equipped) R.string.items_equipped else R.string.shoes_preview) +
                            " · " + selected.rarity.label())
                        Spacer(Modifier.height(10.dp))
                        S2Headline(selected.variantLabel())
                        Spacer(Modifier.height(10.dp))
                        S2Subtitle(stringResource(R.string.sneaker_mint_no, selected.mintNumber) + " · " +
                            stringResource(R.string.level_chip, selected.level))
                        Spacer(Modifier.height(18.dp))
                        ShoeStage(selected, Modifier.fillMaxWidth(if (columns == 1) 1f else 0.86f))
                        TextButton(onClick = { onOpenSneaker(selected.id) },
                            modifier = Modifier.heightIn(min = StepUpDesign.TouchTarget).testTag("shoe-detail")) {
                            Text(stringResource(R.string.shoes_details), color = Silver)
                        }
                    }
                }
                item {
                    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                        Text(stringResource(R.string.shoes_owned, inventory.size), color = Snow,
                            style = MaterialTheme.typography.titleMedium, modifier = Modifier.weight(1f))
                        TextButton(onClick = onOpenVault) { Text(stringResource(R.string.me_see_all)) }
                    }
                }
                items(inventory.chunked(columns), key = { row -> row.first().id }) { row ->
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        row.forEach { shoe ->
                            ShoeChoice(shoe, selected.id == shoe.id, { selectedId = shoe.id }, Modifier.weight(1f))
                        }
                        repeat(columns - row.size) { Spacer(Modifier.weight(1f)) }
                    }
                }
            }
        }
        S2ActionRow(
            start = { S2SideInfo(stringResource(R.string.shoes_s2_market), onClick = onOpenMarket) },
            end = { S2SideInfo(stringResource(R.string.shoes_s2_dex), end = true, onClick = onOpenDex) },
        ) {
            if (selected != null) {
                S2RoundAction(
                    icon = if (selected.equipped) androidx.compose.material.icons.Icons.Filled.Check
                        else androidx.compose.material.icons.Icons.Filled.SwapHoriz,
                    label = stringResource(if (selected.equipped) R.string.items_equipped else R.string.shoes_s2_equip),
                    enabled = ready && !selected.equipped && !saving,
                    onClick = { viewModel.equip(selected.id) },
                    modifier = Modifier.testTag("shoe-equip"),
                )
            } else {
                Spacer(Modifier.width(88.dp))
            }
        }
    }
}

/** S2 신발 무대 — 기울인 파란 면 위에 신발 그림. 그림은 기존 신발 자산 그대로다. */
@Composable
private fun ShoeStage(shoe: Sneaker, modifier: Modifier = Modifier) {
    Box(modifier.aspectRatio(312f / 214f), contentAlignment = Alignment.Center) {
        Box(
            Modifier.fillMaxSize().padding(horizontal = 6.dp, vertical = 8.dp)
                .graphicsLayer { rotationZ = -8f }
                .background(
                    if (StepUpColors.dark) androidx.compose.ui.graphics.Color(0xFF294B9C) else CarbonHigh,
                    RoundedCornerShape(4.dp),
                ),
        )
        SneakerFrame(shoe, Modifier.fillMaxWidth(0.84f).fillMaxHeight(0.86f).graphicsLayer { rotationZ = -7f })
    }
}

@Composable
private fun ShoeChoice(shoe: Sneaker, picked: Boolean, onPick: () -> Unit, modifier: Modifier) {
    val shape = RoundedCornerShape(StepUpDesign.PanelRadius)
    Column(modifier.clip(shape).background(CarbonHigh)
        .border(if (picked) 2.dp else 1.dp, if (picked) Volt else Edge, shape)
        .feedbackClickable(role = Role.RadioButton, onClick = onPick).semantics { selected = picked }
        .testTag("shoe-choice-${shoe.id}").padding(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
        SneakerFrame(shoe, Modifier.fillMaxWidth().aspectRatio(1.4f))
        Text(shoe.variantLabel(), color = Snow, style = MaterialTheme.typography.titleSmall)
        Text(stringResource(R.string.sneaker_mint_no, shoe.mintNumber), color = Silver,
            style = MaterialTheme.typography.bodySmall)
        Text(if (shoe.equipped) stringResource(R.string.items_equipped) else stringResource(R.string.level_chip, shoe.level), color = if (shoe.equipped) VoltText else Silver, style = MaterialTheme.typography.bodySmall)
    }
}
