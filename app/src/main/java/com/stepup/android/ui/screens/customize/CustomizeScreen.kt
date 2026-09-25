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
            else -> null
        }
        if (message != null) {
            text?.let { Toast.makeText(context, it, Toast.LENGTH_SHORT).show() }
            viewModel.consumeMessage()
        }
    }
    Column(Modifier.fillMaxSize().padding(horizontal = StepUpDesign.Gutter)) {
        LazyColumn(Modifier.weight(1f).fillMaxWidth(), contentPadding = PaddingValues(bottom = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)) {
            item {
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    Text(stringResource(R.string.shoes_title), style = MaterialTheme.typography.headlineMedium,
                        color = Snow, modifier = Modifier.weight(1f))
                    TextButton(onClick = onOpenDex) { Text(stringResource(R.string.dex_title)) }
                }
            }
            if (!ready) item {
                StatePanel(stringResource(R.string.feed_loading), StepUpIcons.Shoe, loading = true)
            } else if (selected == null) item {
                StatePanel(stringResource(R.string.customize_no_shoes), StepUpIcons.Shoe,
                    action = { GhostButton(stringResource(R.string.customize_open_vault), onClick = onOpenVault) })
            } else {
                item {
                    GlowCard(Modifier.testTag("shoe-preview").guideTarget(GuideTour.Targets.CUSTOMIZE_PREVIEW), spacing = 8.dp) {
                        Text(stringResource(if (selected.equipped) R.string.items_equipped else R.string.shoes_preview),
                            style = MaterialTheme.typography.labelLarge, color = VoltText)
                        SneakerFrame(selected, Modifier.fillMaxWidth().height(190.dp))
                        Text(selected.variantLabel(), style = MaterialTheme.typography.headlineSmall, color = Snow)
                        Text(selected.rarity.label() + " · " + stringResource(R.string.level_chip, selected.level),
                            style = MaterialTheme.typography.bodyMedium, color = Silver)
                        TextButton(onClick = { onOpenSneaker(selected.id) }, modifier = Modifier.testTag("shoe-detail")) {
                            Text(stringResource(R.string.shoes_details))
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
        if (selected != null) {
            PrimaryCta(text = stringResource(if (selected.equipped) R.string.items_equipped else R.string.shoes_select),
                enabled = ready && !selected.equipped && !saving,
                onClick = { viewModel.equip(selected.id) },
                modifier = Modifier.testTag("shoe-equip"))
        }
        TextButton(onClick = onOpenMarket, modifier = Modifier.fillMaxWidth().heightIn(min = StepUpDesign.TouchTarget)) {
            Text(stringResource(R.string.shoes_market))
        }
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
