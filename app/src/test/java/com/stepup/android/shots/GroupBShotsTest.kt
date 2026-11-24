package com.stepup.android.shots

import android.graphics.Bitmap
import androidx.activity.ComponentActivity
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.requiredSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import com.stepup.android.domain.Faction
import com.stepup.android.domain.PointsPreview
import com.stepup.android.domain.PointsSplit
import com.stepup.android.domain.Rarity
import com.stepup.android.domain.ServerStats
import com.stepup.android.domain.ShoeRules
import com.stepup.android.domain.StatRule
import com.stepup.android.domain.Sneaker
import com.stepup.android.domain.StatPoints
import com.stepup.android.ui.components.RunSheetPanel
import com.stepup.android.ui.components.S2ShoesSections
import com.stepup.android.ui.components.ShoeSection
import com.stepup.android.ui.components.StepUpUi
import com.stepup.android.ui.components.runTone
import com.stepup.android.ui.screens.customize.MyShoesContent
import com.stepup.android.ui.screens.items.OwnedLoad
import com.stepup.android.ui.screens.items.PointsActions
import com.stepup.android.ui.screens.items.PointsDraft
import com.stepup.android.ui.screens.items.PointsPhase
import com.stepup.android.ui.screens.items.ShoeDetailContent
import com.stepup.android.ui.screens.items.ShoeDetailState
import com.stepup.android.ui.screens.items.ShoePointsSheetBody
import com.stepup.android.ui.screens.items.pointsBasisOf
import com.stepup.android.ui.theme.StepUpTheme
import com.stepup.android.ui.theme.ThemeMode
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import java.io.File

/**
 * 재디자인 B(2026-11-24) 시안 맞대기 — 14 · 14b 내 신발 탭, 05 · 05b 신발 상세, 06 스탯 포인트 ① ~ ④.
 * 신발 · 숫자는 시안의 예시(스파크 러너 · 일반 Lv.6 · No. 1129 · 효율 140 · 착화감 140 · 31분)를 이 캡처에서만 쓴다.
 */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [35], qualifiers = "ko-w393dp-h852dp-xxhdpi")
class GroupBShotsTest {
    @get:Rule val compose = createAndroidComposeRule<ComponentActivity>()

    @Before fun only() = Shots.require()

    private var dark by mutableStateOf(false)
    private var size by mutableStateOf(ShotSizes[0])

    /** 한 번 그린 뒤 테마 · 폭만 바꿔 네 장을 찍는다 */
    private fun shots(name: String, content: @Composable () -> Unit) {
        compose.setContent {
            CompositionLocalProvider(LocalDensity provides Density(3f, size.font)) {
                StepUpTheme(if (dark) ThemeMode.DARK else ThemeMode.LIGHT) {
                    val t = runTone()
                    key(dark, size) {
                        Box(Modifier.requiredSize(size.widthDp.dp, size.heightDp.dp).background(t.screen).testTag("shot-root")) {
                            content()
                        }
                    }
                }
            }
        }
        for (d in listOf(false, true)) for (s in ShotSizes) {
            dark = d
            size = s
            compose.waitForIdle()
            compose.mainClock.advanceTimeBy(1500)
            compose.waitForIdle()
            val view = compose.activity.window.decorView
            val img = Bitmap.createBitmap(view.width.coerceAtLeast(1), view.height.coerceAtLeast(1), Bitmap.Config.ARGB_8888)
            view.draw(android.graphics.Canvas(img))
            val b = compose.onNodeWithTag("shot-root").fetchSemanticsNode().boundsInWindow
            val x = b.left.toInt().coerceIn(0, img.width - 1)
            val y = b.top.toInt().coerceIn(0, img.height - 1)
            val crop = Bitmap.createBitmap(img, x, y, b.width.toInt().coerceAtMost(img.width - x), b.height.toInt().coerceAtMost(img.height - y))
            val f = File(Shots.dir, "${name}_${if (d) "dark" else "light"}_${s.tag}.png")
            f.outputStream().use { crop.compress(Bitmap.CompressFormat.PNG, 100, it) }
        }
    }

    @Test fun s14_myShoes() = shots("14_myshoes") { tab(FULL) }
    @Test fun s14b_myShoesWorn() = shots("14b_myshoes_worn") { tab(WORN) }
    @Test fun s05_detail() = shots("05_detail") { ShoeDetailContent(ShoeDetailState.Ready(FULL, FULL)) }
    @Test fun s05b_detailWorn() = shots("05b_detail_worn") { ShoeDetailContent(ShoeDetailState.Ready(WORN, WORN)) }

    private val basis get() = requireNotNull(pointsBasisOf(FULL, RULES))
    @Test fun s06_1_recommend() = shots("06_1_recommend") { sheet(PointsPhase.Editing(basis, basis.saved)) }
    @Test fun s06_2_manual() = shots("06_2_manual") {
        val b = basis
        sheet(PointsPhase.Editing(b, b.saved.copy(efficiency = 1)), manual = true)
    }
    @Test fun s06_3_confirm() = shots("06_3_confirm") { sheet(PointsPhase.Confirm(basis, PointsDraft(2, 0, 0))) }
    @Test fun s06_4_done() = shots("06_4_done") { sheet(PointsPhase.Done(basis, PointsDraft(2, 0, 0))) }

    @Composable
    private fun tab(shoe: Sneaker) {
        Column(Modifier.fillMaxSize()) {
            S2ShoesSections(ShoeSection.MINE, onSelect = {})
            MyShoesContent(
                load = OwnedLoad.Ready(listOf(shoe)), selectedId = shoe.id, onSelect = {}, onOpenSneaker = {}, onOpenDraw = {}, onReload = {},
                modifier = Modifier.weight(1f),
            )
        }
    }

    /** 상세 위 시트 — 창(ModalBottomSheet) 없이 같은 면(RunSheetPanel)에 같은 몸통을 그린다 */
    @Composable
    private fun sheet(phase: PointsPhase, manual: Boolean = false) {
        Box(Modifier.fillMaxSize()) {
            ShoeDetailContent(ShoeDetailState.Ready(FULL, FULL))
            Box(Modifier.fillMaxSize().background(StepUpUi.colors.sheetScrim))
            RunSheetPanel(onClose = {}, modifier = Modifier.align(Alignment.BottomCenter)) {
                ShoePointsSheetBody(FULL, phase, PointsActions(), onSignIn = null, preview = PREVIEW, startManual = manual)
            }
        }
    }

    private companion object {
        fun server(now: Int) = ServerStats(
            origin = "DRAW", efficiencyBps = 4000, comfortBps = 2857, durabilityPts = now * 100.0 / 155, maxLevel = 20,
            status = "OWNED", chainState = "APP", canWithdraw = true, upgradeCost = 0.0, repairCostPerPoint = 0.0, genesisNo = 0,
            durabilityNow = now, durabilityMax = 155, durabilityBase = 155, efficiencyBaseBps = 4000, comfortBaseBps = 2857,
            points = StatPoints(total = 2, free = 2), repairCostPerUnit = 0.01,
        )

        fun shoe(now: Int) = Sneaker(
            id = 1129, faction = Faction.FIRE, rarity = Rarity.COMMON, variant = 0, level = 6, mintNumber = 1129,
            luck = 1.0, comfort = 1.0, durability = now * 100 / 155, equipped = true, acquiredAt = 0L, server = server(now),
        )

        /** 서버 0119 규칙(능력치 묶음을 서버에서 받은 것으로 — 5레벨마다 다시 나누기 · 효율 포인트로 닳는 속도) */
        val RULES = ShoeRules.FALLBACK.copy(stats = StatRule.FALLBACK.copy(fromServer = true))

        val FULL = shoe(155)
        val WORN = shoe(65)

        val PREVIEW = PointsPreview(
            source = PointsPreview.Source.MINE, runs = 7, days = 5, avgMin = 30, total = 2, free = 2, respec = false,
            now = PointsSplit(0, 0, 0, dayNet = 10.0), wornRuns = 0, energyOutDays = 0,
            perPoint = PointsPreview.PerPoint(0.57, 0.31, 0.12), comfortCapped = false,
            recommend = PointsSplit(2, 0, 0, dayNet = 11.14), recommendAll = null,
        )
    }
}
