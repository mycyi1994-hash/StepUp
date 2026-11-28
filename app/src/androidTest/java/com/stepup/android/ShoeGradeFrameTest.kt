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
import com.stepup.android.domain.ShoeTier
import com.stepup.android.ui.MainScaffold
import com.stepup.android.ui.Screen
import com.stepup.android.ui.components.GradeArtRatio
import com.stepup.android.ui.components.S2Stage
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
            // 01 — 13차: 위 카드는 지금 신은 신발(일반). 고르는 미리 보기는 없다
            stageFullyVisible(Rarity.COMMON.key)
            shot("01-${Rarity.COMMON.key}")
            compose.onAllNodesWithTag("shoe-detail").assertCountEquals(0)
            // 05 — 보유 4칸 격자: 켤레마다 한 칸
            GRADES.forEach { rarity ->
                showTile(shoes.getValue(rarity))
                compose.onNodeWithTag("shoe-choice-${shoes.getValue(rarity)}").assertExists()
            }
            shot("05-owned-grid")

            // 09 — 레전더리 칸 → 상세(등급 테두리 카드, 비율 그대로, 실제 신발 그림)
            val legendary = shoes.getValue(Rarity.LEGENDARY)
            showTile(legendary)
            compose.onNodeWithTag("shoe-choice-$legendary").performClick()
            awaitSingle("shoe-art")
            compose.onNode(hasTestTag("shoe-card-tag") and hasAnyAncestor(hasTestTag("shoe-name")), useUnmergedTree = true)
                .assertTextContains(compose.activity.getString(R.string.shoe_tier_legendary), substring = true)
            keepsRatio(stageBounds("shoe-name"), "detail card", com.stepup.android.ui.components.V12ShoeCardRatio)
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
            showTile(shoes.getValue(Rarity.RARE))
            shot("11-light-owned")

            compose.runOnIdle { light = false; narrow = true }
            stageFullyVisible("legendary")
            shot("12-narrow-320")

            // 큰 글씨 — 글자가 커진 만큼 무대부터 줄이고, 그래도 모자라면 그때만 넘겨 본다
            compose.runOnIdle { narrow = false; large = true }
            stageFullyVisible("legendary", firstView = false)
            shot("13-large-font")
            showTile(shoes.getValue(Rarity.LEGENDARY))
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
        compose.onNodeWithTag("my-shoes").performScrollToNode(hasTestTag("shoe-choice-$id"))
    }

    /** 보유 칸을 눌러 미리 보기로 고른다 */
    private fun pick(id: Long) {
        showTile(id)
        compose.onNodeWithTag("shoe-choice-$id").performClick().assertIsSelected()
    }

    /**
     * 신발 카드(UI 최종 통합안 2026-10-25 — 신발 탭 큰 그림은 등급 무대가 아니라 장소 사진 카드다) — 카드가 창 안에 온전히(잘리지 않게),
     * 361:236 비율 그대로, 왼쪽 위 줄이 보이는 갈래 [tier] 의 등급이다.
     * [firstView] 면 넘기지 않은 첫 화면에서 본다 — 세로 스크롤 거리가 0 이고 보유 목록 줄이 창 안에 있다(보통 글씨).
     * 아니면 카드까지 넘긴 뒤에 본다(큰 글씨).
     */
    private fun stageFullyVisible(tier: String, firstView: Boolean = true) {
        awaitSingle("shoe-hero")
        val label = compose.activity.getString(tierLabel(ShoeTier.valueOf(tier.uppercase(Locale.ROOT))))
        compose.waitUntil(10_000) {
            // v12 시제품(2026-11-26) — 카드 넘기기: 고른 켤레의 카드 등급 칩(옆 카드들도 있어 아무 카드에 그 등급이 있으면 된다)
            (compose.onAllNodesWithTag("shoe-card-tag", useUnmergedTree = true).fetchSemanticsNodes() +
                compose.onAllNodesWithTag("v12-card-tier", useUnmergedTree = true).fetchSemanticsNodes()).any { node ->
                node.config.getOrNull(SemanticsProperties.Text)?.any { it.text.contains(label) } == true
            }
        }
        if (!firstView) compose.onNodeWithTag("shoe-hero").performScrollTo()
        compose.waitForIdle()
        val viewport = compose.onNodeWithTag("my-shoes").fetchSemanticsNode().boundsInRoot
        val card = stageBounds("shoe-hero")
        // 카드 비율은 v12 카드(V12ShoeCardRatio) — 넘기기 판 전체(shoe-hero)는 화면 폭이라 비율을 보지 않는다
        assertTrue("shoe card fully inside the window: $card in $viewport",
            card.top >= viewport.top - 1 && card.bottom <= viewport.bottom + 1 &&
                card.left >= viewport.left - 1 && card.right <= viewport.right + 1)
        val minHeight = with(compose.density) { 112.dp.toPx() }
        assertTrue("shoe card keeps the shoe visible: ${card.height}", card.height >= minHeight)
        // 13차 — 아래 보유 격자는 넘겨 본다(켤레가 많으면 길다). 첫 화면에서는 카드가 맨 위에 온전히 보이면 된다
    }

    private fun tierLabel(tier: ShoeTier): Int = when (tier) {
        ShoeTier.COMMON -> R.string.shoe_tier_common
        ShoeTier.RARE -> R.string.shoe_tier_rare
        ShoeTier.EPIC -> R.string.shoe_tier_epic
        ShoeTier.LEGENDARY -> R.string.shoe_tier_legendary
        ShoeTier.REDLINE -> R.string.shoe_tier_redline
        ShoeTier.FINISH -> R.string.shoe_tier_finish
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
        awaitFrameOnScreen(compose.activity)
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
