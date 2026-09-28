package com.stepup.android

import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.requiredWidth
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.semantics.getOrNull
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import androidx.test.platform.app.InstrumentationRegistry
import com.stepup.android.core.ServiceLocator
import com.stepup.android.domain.Faction
import com.stepup.android.domain.Rarity
import com.stepup.android.ui.MainScaffold
import com.stepup.android.ui.Screen
import com.stepup.android.ui.components.GradeArtRatio
import com.stepup.android.ui.components.S2Stage
import com.stepup.android.ui.components.ShoeStageRatio
import com.stepup.android.ui.experience.ExperienceProvider
import com.stepup.android.ui.screens.gacha.DrawResultDialog
import com.stepup.android.ui.theme.StepUpTheme
import com.stepup.android.ui.theme.ThemeMode
import java.io.File
import java.util.Locale
import kotlin.math.abs
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

/**
 * 신발 등급 프레임 v8(docs/redesign/shoe-grade-frames) — 앱의 네 등급(일반 · 레어 · 에픽 · 레전더리)을 실제 화면에서 찍는다.
 *
 * 신발 화면 확정안(2026-09-28)부터 내 신발의 큰 신발은 등급 프레임 무대다 — 신발 탭에서 네 등급을 차례로 골라 무대가 창 안에
 * 온전히 · 440:418 그대로 · 세로 스크롤 없이 보유 목록과 함께 서는지, 보유 칸 · 신발 보관함 2열 격자의 무대가 비율 그대로인지,
 * 뽑기 결과와 받침 무대의 상세(보유 신발 상세 v1), 밝은 테마 · 좁은 폭 · 큰 글씨를 본다.
 * 신발은 기존 52종 그림이고, 이 테스트가 넣은 신발은 끝나면 지우고 원래 신던 신발을 다시 신긴다.
 */
class ShoeGradeFrameTest {
    @get:Rule(order = 0) val appLanguage = object : org.junit.rules.ExternalResource() {
        private var previous = ""
        override fun before() {
            val instrumentation = InstrumentationRegistry.getInstrumentation()
            val context = instrumentation.targetContext
            com.stepup.android.core.AppLocale.syncFromSystem(context)
            previous = com.stepup.android.core.AppLocale.tag
            instrumentation.runOnMainSync { com.stepup.android.core.AppLocale.change(context, "ko") }
        }
        override fun after() {
            val instrumentation = InstrumentationRegistry.getInstrumentation()
            instrumentation.runOnMainSync { com.stepup.android.core.AppLocale.change(instrumentation.targetContext, previous) }
        }
    }
    @get:Rule(order = 1) val compose = createAndroidComposeRule<ComponentActivity>()

    private val directory get() = File(compose.activity.getExternalFilesDir(null), "shoe-grade").apply { mkdirs() }
    private var originalEquipped: Long? = null

    /** 앱 셸 안의 신발 탭 → 내 신발(등급 프레임 무대 · 보유 칸) → 신발 보관함(2열 격자) → 상세 */
    @Test fun gradeFramesInTheShoesTab() {
        val shoes = seedGrades()
        try {
            // 일반을 신은 상태에서 시작 — 첫 무대가 일반이다
            runBlocking { ServiceLocator.sneakerRepository.equip(shoes.getValue(Rarity.COMMON)) }
            edgeToEdge()
            compose.setContent {
                StepUpTheme(ThemeMode.DARK) { ExperienceProvider { MainScaffold(initialTab = Screen.Customize) } }
            }
            awaitTag("my-shoes")
            // 01–04 — 일반(신고 있는 신발) → 레어 → 에픽 → 레전더리. 고르는 것은 미리 보기라 신발은 바뀌지 않는다
            GRADES.forEachIndexed { index, rarity ->
                if (index > 0) pick(shoes.getValue(rarity))
                stageFullyVisible(rarity.key)
                shot("0${index + 1}-${rarity.key}")
            }
            // "신발 자세히 보기" 글 링크는 없다(신발 화면 확정안 2026-09-28)
            compose.onAllNodesWithTag("shoe-detail").assertCountEquals(0)
            // 05 — 보유 칸(가로 목록): 작은 프레임 비율 그대로
            GRADES.forEach { rarity ->
                showTile(shoes.getValue(rarity))
                thumbKeepsRatio(rarity)
            }
            shot("05-owned-row")

            // 07 — 신발 보관함(위 글자 탭): 2열 격자의 등급 무대(효과 포함). 두 칸이 한 줄에 선다
            compose.onNodeWithTag("shoes-section-vault").performClick()
            awaitTag("vault-grid")
            GRADES.forEach { rarity ->
                val id = shoes.getValue(rarity)
                grid().performScrollToNode(hasTestTag("vault-card-$id"))
                compose.waitForIdle()
                keepsRatio(cardStage(id, rarity.key), "vault ${rarity.key}")
            }
            grid().performScrollToIndex(0)
            compose.waitForIdle()
            val cards = compose.onAllNodes(hasTestTagPrefix("vault-card-"), useUnmergedTree = true).fetchSemanticsNodes()
                .map { it.boundsInRoot }.sortedWith(compareBy({ it.top }, { it.left }))
            assertTrue("vault shows at least two cards", cards.size >= 2)
            assertTrue("two columns: ${cards[0]} · ${cards[1]}", abs(cards[0].top - cards[1].top) < 2f && cards[1].left > cards[0].right)
            shot("07-vault-grid")

            // 09 — 레전더리 칸 → 상세(보유 신발 상세 v1 — 받침 무대, 비율 그대로, 실제 신발 그림)
            val legendary = shoes.getValue(Rarity.LEGENDARY)
            grid().performScrollToNode(hasTestTag("vault-card-$legendary"))
            compose.onNodeWithTag("vault-card-$legendary").performClick()
            compose.waitUntil(10_000) { compose.onAllNodesWithTag("vault-grid").fetchSemanticsNodes().isEmpty() }
            awaitSingle("shoe-stage")
            awaitSingle("shoe-art")
            keepsRatio(stageBounds("shoe-stage"), "detail stage", ShoeStageRatio)
            shot("09-detail-legendary")
        } finally {
            restore(shoes)
        }
    }

    /** 밝은 테마 · 좁은 폭(320dp) · 큰 글씨 — 무대가 첫 화면 안에 온전히, 찌그러지지 않게 */
    @Test fun gradeFramesAcrossThemesAndSizes() {
        val shoes = seedGrades()
        var light by mutableStateOf(true)
        var narrow by mutableStateOf(false)
        var large by mutableStateOf(false)
        try {
            runBlocking { ServiceLocator.sneakerRepository.equip(shoes.getValue(Rarity.LEGENDARY)) }
            edgeToEdge()
            compose.setContent {
                val density = LocalDensity.current
                CompositionLocalProvider(LocalDensity provides Density(density.density, if (large) 1.5f else 1f)) {
                    StepUpTheme(if (light) ThemeMode.LIGHT else ThemeMode.DARK) {
                        ExperienceProvider {
                            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.TopCenter) {
                                Box(if (narrow) Modifier.requiredWidth(320.dp).fillMaxHeight() else Modifier.fillMaxSize()) {
                                    MainScaffold(initialTab = Screen.Customize)
                                }
                            }
                        }
                    }
                }
            }
            awaitTag("my-shoes")
            stageFullyVisible("legendary")
            shot("10-light-legendary")
            pick(shoes.getValue(Rarity.EPIC))
            stageFullyVisible("epic")
            showTile(shoes.getValue(Rarity.RARE))
            thumbKeepsRatio(Rarity.RARE)
            shot("11-light-owned")

            compose.runOnIdle { light = false; narrow = true }
            stageFullyVisible("epic")
            shot("12-narrow-320")

            // 큰 글씨 — 글자가 커진 만큼 무대부터 줄이고, 그래도 모자라면 그때만 넘겨 본다
            compose.runOnIdle { narrow = false; large = true }
            stageFullyVisible("epic", firstView = false)
            shot("13-large-font")
            showTile(shoes.getValue(Rarity.LEGENDARY))
            thumbKeepsRatio(Rarity.LEGENDARY)
            shot("14-large-font-owned")
        } finally {
            restore(shoes)
        }
    }

    /** 뽑기 결과 — 뽑은 신발의 등급 무대(나타나기 · 반짝임은 그대로) */
    @Test fun drawResultShowsTheGrade() {
        val shoes = seedGrades()
        try {
            val legendary = runBlocking {
                ServiceLocator.sneakerRepository.inventory.first().first { it.id == shoes.getValue(Rarity.LEGENDARY) }
            }
            edgeToEdge()
            compose.setContent {
                StepUpTheme(ThemeMode.DARK) {
                    ExperienceProvider {
                        Box(Modifier.fillMaxSize()) {
                            S2Stage(Modifier.fillMaxSize())
                            DrawResultDialog(legendary, onOpenShoes = {}, onClose = {})
                        }
                    }
                }
            }
            awaitSingle("grade-stage-legendary")
            keepsRatio(stageBounds("grade-stage-legendary"), "draw result stage")
            compose.onNodeWithTag("draw-result-shoes").assertIsDisplayed()
            shot("15-draw-result-legendary", settle = 1_500)
        } finally {
            restore(shoes)
        }
    }

    // ── 도우미 ─────────────────────────────────────────────────────

    private val Rarity.key get() = name.lowercase(Locale.ROOT)

    private fun grid() = compose.onNodeWithTag("vault-grid")

    private fun hasTestTagPrefix(prefix: String) = SemanticsMatcher("testTag starts with $prefix") { node ->
        node.config.getOrNull(SemanticsProperties.TestTag)?.startsWith(prefix) == true
    }

    /** 격자 칸 안의 등급 무대 */
    private fun cardStage(id: Long, tier: String): Rect =
        compose.onNode(hasTestTag("grade-stage-$tier") and hasAnyAncestor(hasTestTag("vault-card-$id")), useUnmergedTree = true)
            .fetchSemanticsNode().boundsInRoot

    /** 보유 칸(가로 목록)을 보이게 — 내 신발은 세로로 끌지 않는다. 가로 목록만 그 칸까지 */
    private fun showTile(id: Long) {
        compose.onNodeWithTag("shoe-owned-row").performScrollTo()
        compose.onNodeWithTag("shoe-owned-row").performScrollToNode(hasTestTag("shoe-choice-$id"))
    }

    /** 보유 칸을 눌러 미리 보기로 고른다 */
    private fun pick(id: Long) {
        showTile(id)
        compose.onNodeWithTag("shoe-choice-$id").performClick().assertIsSelected()
    }

    /**
     * 큰 무대(등급 프레임) — 내 신발의 무대가 창 안에 온전히(잘리지 않게), 440:418 비율 그대로, 보이는 갈래 [tier] 의 프레임이다.
     * [firstView] 면 넘기지 않은 첫 화면에서 본다 — 세로 스크롤 거리가 0 이고 보유 목록 줄이 하단 탭 위에 있다(보통 글씨).
     * 아니면 무대까지 넘긴 뒤에 본다(큰 글씨).
     */
    private fun stageFullyVisible(tier: String, firstView: Boolean = true) {
        awaitSingle("shoe-hero")
        compose.waitUntil(10_000) {
            compose.onAllNodes(hasTestTag("grade-stage-$tier") and hasAnyAncestor(hasTestTag("shoe-hero")), useUnmergedTree = true)
                .fetchSemanticsNodes().size == 1
        }
        if (!firstView) compose.onNodeWithTag("shoe-hero").performScrollTo()
        compose.waitForIdle()
        val viewport = compose.onNodeWithTag("my-shoes").fetchSemanticsNode().boundsInRoot
        val stage = stageBounds("shoe-hero")
        keepsRatio(stage, "hero stage")
        assertTrue("hero stage fully inside the window: $stage in $viewport",
            stage.top >= viewport.top - 1 && stage.bottom <= viewport.bottom + 1 &&
                stage.left >= viewport.left - 1 && stage.right <= viewport.right + 1)
        val minHeight = with(compose.density) { 112.dp.toPx() }
        assertTrue("hero stage keeps the shoe visible: ${stage.height}", stage.height >= minHeight)
        if (firstView) {
            val range = compose.onNodeWithTag("my-shoes").fetchSemanticsNode().config[SemanticsProperties.VerticalScrollAxisRange]
            assertEquals("my shoes fits without vertical scrolling", 0f, range.maxValue(), 1f)
            val row = compose.onNodeWithTag("shoe-owned-row").fetchSemanticsNode().boundsInRoot
            assertTrue("owned row above the bottom of the window: $row in $viewport", row.bottom <= viewport.bottom + 1)
        }
    }

    /** 목록 칸 — 눌리는 카드가 안쪽을 합치므로 합치기 전 나무에서 찾는다. 하나도 없으면 실패다 */
    private fun thumbKeepsRatio(rarity: Rarity) {
        compose.waitForIdle()
        val thumbs = compose.onAllNodesWithTag("grade-thumb-${rarity.key}", useUnmergedTree = true).fetchSemanticsNodes()
        assertTrue("${rarity.key} thumb is on screen", thumbs.isNotEmpty())
        thumbs.forEach { keepsRatio(it.boundsInRoot, "${rarity.key} thumb") }
    }

    /** 무대 — 보관함 착용 카드처럼 눌리는 카드 안에 있으면 카드가 합치므로 합치기 전 나무에서 잰다 */
    private fun stageBounds(tag: String): Rect =
        compose.onNodeWithTag(tag, useUnmergedTree = true).fetchSemanticsNode().boundsInRoot

    private fun keepsRatio(bounds: Rect, what: String, expected: Float = GradeArtRatio) {
        val ratio = bounds.width / bounds.height
        assertTrue("$what keeps $expected — ${bounds.width} × ${bounds.height}", abs(ratio - expected) / expected < 0.02f)
    }

    /** 앱 신발 창고에 네 등급 한 켤레씩 — 일반은 신고 있는 신발(없으면 새로), 나머지는 새로 넣는다 */
    private fun seedGrades(): Map<Rarity, Long> = runBlocking {
        ServiceLocator.userPrefs.setReducedMotion(true)
        ServiceLocator.userPrefs.setSounds(false)
        ServiceLocator.userPrefs.setHaptics(false)
        ServiceLocator.userPrefs.setGuideSeen()
        ServiceLocator.sneakerRepository.ensureStarter()
        val dao = ServiceLocator.database.sneakerDao()
        val base = requireNotNull(dao.equippedNow() ?: dao.allNow().firstOrNull())
        if (!base.equipped) ServiceLocator.sneakerRepository.equip(base.id)
        originalEquipped = base.id
        val ids = linkedMapOf<Rarity, Long>()
        if (base.rarity == Rarity.COMMON.id) ids[Rarity.COMMON] = base.id
        val now = System.currentTimeMillis()
        for ((rarity, faction, variant) in PICKS) {
            if (ids.containsKey(rarity)) continue
            ids[rarity] = dao.insert(
                base.copy(
                    id = 0, factionId = faction.id, rarity = rarity.id, variant = variant, level = 1,
                    mintNumber = dao.maxMintNumber() + 1, equipped = false, acquiredAt = now, serverId = 0,
                ),
            )
        }
        ids
    }

    private fun restore(shoes: Map<Rarity, Long>) = runBlocking {
        val dao = ServiceLocator.database.sneakerDao()
        originalEquipped?.let { ServiceLocator.sneakerRepository.equip(it) }
        shoes.values.filter { it != originalEquipped }.forEach { id -> dao.byId(id)?.let { dao.delete(it) } }
    }

    private fun edgeToEdge() {
        compose.activityRule.scenario.onActivity {
            it.enableEdgeToEdge(
                statusBarStyle = SystemBarStyle.dark(android.graphics.Color.TRANSPARENT),
                navigationBarStyle = SystemBarStyle.dark(android.graphics.Color.TRANSPARENT),
            )
        }
    }

    private fun awaitTag(tag: String) {
        compose.waitUntil(10_000) { compose.onAllNodesWithTag(tag, useUnmergedTree = true).fetchSemanticsNodes().isNotEmpty() }
    }

    /** 화면이 바뀌는 동안에는 앞뒤 화면이 함께 있다 — 하나만 남을 때까지(눌리는 카드 안의 무대도 보이게 합치기 전 나무에서) */
    private fun awaitSingle(tag: String) {
        compose.waitUntil(10_000) { compose.onAllNodesWithTag(tag, useUnmergedTree = true).fetchSemanticsNodes().size == 1 }
    }

    private fun shot(name: String, settle: Long = 700) {
        compose.waitForIdle()
        InstrumentationRegistry.getInstrumentation().waitForIdleSync()
        Thread.sleep(settle)
        captureDisplay(File(directory, "$name.png"))
    }

    private companion object {
        val GRADES = listOf(Rarity.COMMON, Rarity.RARE, Rarity.EPIC, Rarity.LEGENDARY)

        /** 등급마다 한 켤레 — 일반 클라우드 러너(스타터) · 레어 불 · 에픽 번개 · 레전더리 물 */
        val PICKS = listOf(
            Triple(Rarity.COMMON, Faction.WIND, 0),
            Triple(Rarity.RARE, Faction.FIRE, 0),
            Triple(Rarity.EPIC, Faction.LIGHTNING, 0),
            Triple(Rarity.LEGENDARY, Faction.WATER, 0),
        )
    }
}
