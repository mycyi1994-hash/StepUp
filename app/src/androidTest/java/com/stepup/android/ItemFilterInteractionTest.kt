package com.stepup.android

import androidx.activity.ComponentActivity
import androidx.compose.runtime.*
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import com.stepup.android.domain.Faction
import com.stepup.android.ui.components.GhostButton
import com.stepup.android.ui.screens.items.EquipFilter
import com.stepup.android.ui.screens.items.ItemFilterSheet
import com.stepup.android.ui.screens.items.ItemSort
import com.stepup.android.ui.theme.StepUpTheme
import com.stepup.android.ui.theme.ThemeMode
import java.io.File
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Rule
import org.junit.Test

class ItemFilterInteractionTest {
    @get:Rule val compose = createAndroidComposeRule<ComponentActivity>()

    @Test fun dismissDiscardsDraftAndApplyAndResetCommitAllConditionsTogether() {
        var open by mutableStateOf(true)
        var faction by mutableStateOf<String?>(null)
        var rarity by mutableStateOf<String?>(null)
        var equip by mutableStateOf(EquipFilter.OFF)
        var sort by mutableStateOf(ItemSort.LEVEL)
        var applies = 0
        compose.setContent {
            StepUpTheme(ThemeMode.DARK) {
                GhostButton("Open filters", onClick = { open = true })
                if (open) ItemFilterSheet(
                    faction = faction, factionProgress = mapOf(Faction.FIRE to 2),
                    rarity = rarity, equip = equip, sort = sort,
                    onDismiss = { open = false },
                    onApply = { nextFaction, nextRarity, nextEquip, nextSort ->
                        faction = nextFaction
                        rarity = nextRarity
                        equip = nextEquip
                        sort = nextSort
                        applies++
                        open = false
                    },
                )
            }
        }
        fun label(id: Int) = compose.activity.getString(id)
        fun fire() = compose.onNodeWithText(label(R.string.faction_fire), substring = true)
        fun reopen() { compose.onNodeWithText("Open filters").performClick() }
        fire().performScrollTo().performClick().assertIsSelected()
        compose.onNodeWithContentDescription(label(R.string.common_close)).performClick()
        compose.runOnIdle {
            assertNull(faction)
            assertEquals(0, applies)
        }
        reopen()
        fire().performScrollTo().assertIsNotSelected().performClick()
        compose.onNodeWithText(label(R.string.filter_apply)).assertIsDisplayed().performClick()
        compose.runOnIdle {
            assertEquals(Faction.FIRE.id, faction)
            assertEquals(EquipFilter.OFF, equip)
            assertEquals(ItemSort.LEVEL, sort)
            assertEquals(1, applies)
        }
        reopen()
        fire().performScrollTo().assertIsSelected()
        compose.onNodeWithText(label(R.string.filter_reset)).performClick()
        fire().assertIsNotSelected()
        // Reset is also a draft until Show results is pressed.
        compose.runOnIdle { assertEquals(Faction.FIRE.id, faction) }
        val directory = File(compose.activity.getExternalFilesDir(null), "form-checks").apply { mkdirs() }
        captureDisplay(File(directory, "inventory-filter-reset.png"))
        compose.onNodeWithText(label(R.string.filter_apply)).assertIsDisplayed().performClick()
        compose.runOnIdle {
            assertNull(faction)
            assertNull(rarity)
            assertEquals(EquipFilter.ALL, equip)
            assertEquals(ItemSort.RARITY, sort)
            assertEquals(2, applies)
        }
    }
}
