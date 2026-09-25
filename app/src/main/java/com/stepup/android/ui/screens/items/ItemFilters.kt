package com.stepup.android.ui.screens.items

import androidx.annotation.StringRes
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.res.stringResource
import com.stepup.android.R
import com.stepup.android.domain.Rarity
import com.stepup.android.domain.Faction
import com.stepup.android.domain.VARIANTS_PER_FACTION
import com.stepup.android.ui.components.ChoiceGrid
import com.stepup.android.ui.components.FilterBottomSheet
import com.stepup.android.ui.components.FilterSection
import com.stepup.android.ui.components.SortBottomSheet
import com.stepup.android.ui.components.label

/** All inventory conditions are edited together; dismissing does not apply drafts. */

/** 장착 상태 — 서버 값이 아니라 화면 안에서만 쓰는 구분이다. */
object EquipFilter {
    const val ALL = "ALL"
    const val ON = "ON"
    const val OFF = "OFF"

    val values = listOf(ALL, ON, OFF)
}

/**
 * 보관함 정렬.
 *
 * 기본은 [RARITY] — 지금까지 보관함이 매기던 차례(착용 중 → 등급 → 획득
 * 순)와 같다. 새 정렬을 기본으로 삼으면, 늘 같은 자리에 있던 신발이 앱을
 * 올린 날 갑자기 다른 곳에 가 있게 된다.
 */
object ItemSort {
    const val RARITY = "RARITY"
    const val RECENT = "RECENT"
    const val LEVEL = "LEVEL"

    val values = listOf(RARITY, RECENT, LEVEL)
}

@StringRes
fun itemSortRes(key: String): Int = when (key) {
    ItemSort.RECENT -> R.string.items_sort_recent
    ItemSort.LEVEL -> R.string.items_sort_level
    else -> R.string.items_sort_rarity
}

@StringRes
fun equipFilterRes(key: String): Int = when (key) {
    EquipFilter.ON -> R.string.filter_equip_on
    EquipFilter.OFF -> R.string.filter_equip_off
    else -> R.string.filter_equip_all
}

/** 고른 추가 조건 — 속성은 여기 적지 않는다. 위 카드가 이미 켜져 있다. */
@Composable
fun itemFilterParts(rarity: String?, equip: String): List<String> = buildList {
    rarity?.let { id ->
        Rarity.entries.firstOrNull { it.id == id }?.let { add(it.label()) }
    }
    if (equip != EquipFilter.ALL) add(stringResource(equipFilterRes(equip)))
}

fun itemFilterCount(rarity: String?, equip: String): Int =
    (if (rarity != null) 1 else 0) + (if (equip != EquipFilter.ALL) 1 else 0)

/** 등급 격자에서 "전체"를 나타내는 값 */
private val RARITY_CHOICES: List<String?> = listOf(null) + Rarity.entries.map { it.id }

@Composable
fun ItemFilterSheet(
    faction: String?,
    factionProgress: Map<Faction, Int>,
    rarity: String?,
    equip: String,
    sort: String,
    onDismiss: () -> Unit,
    onApply: (faction: String?, rarity: String?, equip: String, sort: String) -> Unit,
) {
    var draftFaction by remember(faction) { mutableStateOf(faction) }
    var draftRarity by remember(rarity) { mutableStateOf(rarity) }
    var draftEquip by remember(equip) { mutableStateOf(equip) }
    var draftSort by remember(sort) { mutableStateOf(sort) }

    FilterBottomSheet(
        title = stringResource(R.string.filter_items_title),
        onDismiss = onDismiss,
        onReset = {
            draftFaction = null
            draftRarity = null
            draftEquip = EquipFilter.ALL
            draftSort = ItemSort.RARITY
        },
        onApply = { onApply(draftFaction, draftRarity, draftEquip, draftSort) },
    ) {
        FilterSection(stringResource(R.string.filter_group_faction)) {
            ChoiceGrid(
                values = listOf<String?>(null) + Faction.entries.map { it.id },
                isSelected = { it == draftFaction },
                label = { id ->
                    if (id == null) stringResource(R.string.post_cat_all)
                    else {
                        val entry = Faction.entries.first { it.id == id }
                        entry.label() + " · ${factionProgress[entry] ?: 0} / $VARIANTS_PER_FACTION"
                    }
                },
                onSelect = { draftFaction = it },
                columns = 2,
            )
        }
        // 레어와 희귀는 서로 다른 등급이다. 같은 칸에 묶지 않는다.
        FilterSection(stringResource(R.string.filter_group_rarity)) {
            ChoiceGrid(
                values = RARITY_CHOICES,
                isSelected = { it == draftRarity },
                label = { id ->
                    if (id == null) {
                        stringResource(R.string.post_cat_all)
                    } else {
                        Rarity.entries.first { it.id == id }.label()
                    }
                },
                onSelect = { draftRarity = it },
            )
        }
        FilterSection(stringResource(R.string.filter_group_equip)) {
            ChoiceGrid(
                values = EquipFilter.values,
                isSelected = { it == draftEquip },
                label = { stringResource(equipFilterRes(it)) },
                onSelect = { draftEquip = it },
            )
        }
        FilterSection(stringResource(R.string.filter_group_sort)) {
            ChoiceGrid(
                values = ItemSort.values,
                isSelected = { it == draftSort },
                label = { stringResource(itemSortRes(it)) },
                onSelect = { draftSort = it },
                columns = 2,
            )
        }
    }
}

@Composable
fun ItemSortSheet(selected: String, onPick: (String) -> Unit, onDismiss: () -> Unit) {
    SortBottomSheet(
        title = stringResource(R.string.filter_sort_title),
        options = ItemSort.values,
        selected = selected,
        label = { stringResource(itemSortRes(it)) },
        onPick = onPick,
        onDismiss = onDismiss,
    )
}
