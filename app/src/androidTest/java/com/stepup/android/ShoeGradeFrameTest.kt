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
import com.stepup.android.ui.experience.ExperienceProvider
import com.stepup.android.ui.screens.gacha.DrawResultDialog
import com.stepup.android.ui.theme.StepUpTheme
import com.stepup.android.ui.theme.ThemeMode
import java.io.File
import java.util.Locale
import kotlin.math.abs
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

/**
 * 신발 등급 프레임 v8(docs/redesign/shoe-grade-frames) — 앱의 네 등급(일반 · 레어 · 에픽 · 레전더리)을 실제 화면에서 찍는다.
 *
 * 신발 탭(앱 셸 안)에서 네 등급을 차례로 골라 큰 무대가 첫 화면 안에 온전히 · 그림 비율(440:418) 그대로 보이는지,
 * 보유 칸 · 보관함 착용 카드 · 상세 · 뽑기 결과, 밝은 테마 · 좁은 폭 · 큰 글씨를 본다. 신발은 기존 52종 그림이고,
 * 이 테스트가 넣은 신발은 끝나면 지우고 원래 신던 신발을 다시 신긴다.
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

    /** 앱 셸 안의 신발 탭 → 보관함 → 상세: 네 등급의 무대와 목록 칸 */
    @Test fun gradeFramesInTheShoesTab() {
        val shoes = seedGrades()
        try {
            // 일반을 신은 상태에서 시작 — 첫 무대와 보관함 착용 카드가 일반이다
            runBlocking { ServiceLocator.sneakerRepository.equip(shoes.getValue(Rarity.COMMON)) }
            edgeToEdge()
            compose.setContent {
                StepUpTheme(ThemeMode.DARK) { ExperienceProvider { MainScaffold(initialTab = Screen.Customize) } }
            }
            awaitTag("shoe-list")
            // 01–04 — 일반(신고 있는 신발) → 레어 → 에픽 → 레전더리. 고르는 것은 미리 보기라 신발은 바뀌지 않는다
            GRADES.forEachIndexed { index, rarity ->
                if (index > 0) pick(shoes.getValue(rarity))
                stageFullyVisible(rarity)
                shot("0${index + 1}-${rarity.key}")
            }
            list().performScrollToNode(hasTestTag("shoe-detail"))
            compose.onNodeWithTag("shoe-detail").assertIsDisplayed()
            // 05 · 06 — 보유 신발 칸: 프레임만(효과 없이), 비율 그대로
            list().performScrollToNode(hasTestTag("shoe-choice-${shoes.getValue(Rarity.COMMON)}"))
            list().performScrollToNode(hasTestTag("shoe-choice-${shoes.getValue(Rarity.LEGENDARY)}"))
            thumbKeepsRatio(Rarity.COMMON)
            thumbKeepsRatio(Rarity.LEGENDARY)
            shot("05-owned-first-row")
            list().performScrollToNode(hasTestTag("shoe-choice-${shoes.getValue(Rarity.RARE)}"))
            thumbKeepsRatio(Rarity.EPIC)
            thumbKeepsRatio(Rarity.RARE)
            shot("06-owned-second-row")

            // 07 · 08 — 보관함: 착용 카드(큰 무대)와 컬렉션 칸
            val seeAll = compose.activity.getString(R.string.me_see_all)
            list().performScrollToNode(hasText(seeAll))
            compose.onNodeWithText(seeAll).performClick()
            compose.waitUntil(10_000) { compose.onAllNodesWithTag("shoe-list").fetchSemanticsNodes().isEmpty() }
            awaitSingle("grade-stage-common")
            keepsRatio(compose.onNodeWithTag("grade-stage-common").fetchSemanticsNode().boundsInRoot, "vault stage")
            shot("07-vault-equipped")
            // 보관함에서 세로로 넘기는 목록은 하나(DetailPage)다
            val vault = { compose.onNode(hasScrollToIndexAction()) }
            GRADES.forEach { rarity ->
                vault().performScrollToNode(hasTestTag("grade-thumb-${rarity.key}"))
                thumbKeepsRatio(rarity)
            }
            vault().performScrollToNode(hasTestTag("grade-thumb-epic"))
            shot("08-vault-collection")

            // 09 — 상세: 레전더리 한 켤레
            vault().performScrollToNode(hasTestTag("grade-thumb-legendary"))
            compose.onNodeWithTag("grade-thumb-legendary").performClick()
            compose.waitUntil(10_000) { compose.onAllNodesWithTag("grade-thumb-legendary").fetchSemanticsNodes().isEmpty() }
            awaitSingle("grade-stage-legendary")
            keepsRatio(compose.onNodeWithTag("grade-stage-legendary").fetchSemanticsNode().boundsInRoot, "detail stage")
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
            awaitTag("shoe-list")
            stageFullyVisible(Rarity.LEGENDARY)
            shot("10-light-legendary")
            pick(shoes.getValue(Rarity.EPIC))
            stageFullyVisible(Rarity.EPIC)
            list().performScrollToNode(hasTestTag("shoe-choice-${shoes.getValue(Rarity.RARE)}"))
            thumbKeepsRatio(Rarity.RARE)
            shot("11-light-owned")

            compose.runOnIdle { light = false; narrow = true }
            list().performScrollToIndex(0)
            stageFullyVisible(Rarity.EPIC)
            shot("12-narrow-320")

            // 큰 글씨 — 글자가 커진 만큼 첫 화면이 모자라면 무대를 한 변 190dp 아래로 줄이지 않고 넘겨 보게 한다
            compose.runOnIdle { narrow = false; large = true }
            list().performScrollToIndex(0)
            stageFullyVisible(Rarity.EPIC, firstView = false)
            shot("13-large-font")
            // 큰 글씨에서는 한 줄 칸 — 넓어진 칸은 2배 프레임을 쓴다
            list().performScrollToNode(hasTestTag("shoe-choice-${shoes.getValue(Rarity.LEGENDARY)}"))
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
            keepsRatio(compose.onNodeWithTag("grade-stage-legendary").fetchSemanticsNode().boundsInRoot, "draw result stage")
            compose.onNodeWithTag("draw-result-shoes").assertIsDisplayed()
            shot("15-draw-result-legendary", settle = 1_500)
        } finally {
            restore(shoes)
        }
    }

    // ── 도우미 ─────────────────────────────────────────────────────

    private val Rarity.key get() = name.lowercase(Locale.ROOT)

    private fun list() = compose.onNodeWithTag("shoe-list")

    /** 보유 칸을 눌러 미리 보기로 고르고, 맨 위(무대)로 돌아간다 */
    private fun pick(id: Long) {
        list().performScrollToNode(hasTestTag("shoe-choice-$id"))
        compose.onNodeWithTag("shoe-choice-$id").performClick().assertIsSelected()
        list().performScrollToIndex(0)
    }

    /**
     * 큰 무대 — 그 등급의 무대 하나가 목록 창 안에 온전히(잘리지 않게), 440:418 그대로, 한 변 180dp 이상.
     * [firstView] 면 넘기지 않은 첫 화면에서 본다(보통 글씨). 아니면 무대까지 넘긴 뒤에 본다(큰 글씨).
     */
    private fun stageFullyVisible(rarity: Rarity, firstView: Boolean = true) {
        awaitSingle("grade-stage-${rarity.key}")
        if (!firstView) list().performScrollToNode(hasTestTag("grade-stage-${rarity.key}"))
        compose.waitForIdle()
        val viewport = list().fetchSemanticsNode().boundsInRoot
        val stage = compose.onNodeWithTag("grade-stage-${rarity.key}").fetchSemanticsNode().boundsInRoot
        keepsRatio(stage, "${rarity.key} stage")
        assertTrue("${rarity.key} stage fully inside the list window: $stage in $viewport",
            stage.top >= viewport.top - 1 && stage.bottom <= viewport.bottom + 1 &&
                stage.left >= viewport.left - 1 && stage.right <= viewport.right + 1)
        val minWidth = with(compose.density) { 180.dp.toPx() }
        assertTrue("${rarity.key} stage keeps the shoe large: ${stage.width}", stage.width >= minWidth)
    }

    private fun thumbKeepsRatio(rarity: Rarity) {
        compose.waitForIdle()
        compose.onAllNodesWithTag("grade-thumb-${rarity.key}").fetchSemanticsNodes().forEach {
            keepsRatio(it.boundsInRoot, "${rarity.key} thumb")
        }
    }

    private fun keepsRatio(bounds: Rect, what: String) {
        val ratio = bounds.width / bounds.height
        assertTrue("$what keeps 440:418 — ${bounds.width} × ${bounds.height}", abs(ratio - GradeArtRatio) / GradeArtRatio < 0.02f)
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

    /** 화면이 바뀌는 동안에는 앞뒤 화면이 함께 있다 — 하나만 남을 때까지 */
    private fun awaitSingle(tag: String) {
        compose.waitUntil(10_000) { compose.onAllNodesWithTag(tag).fetchSemanticsNodes().size == 1 }
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
