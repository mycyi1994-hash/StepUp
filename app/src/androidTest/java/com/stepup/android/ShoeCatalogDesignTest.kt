package com.stepup.android

import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.test.platform.app.InstrumentationRegistry
import com.stepup.android.core.ServiceLocator
import com.stepup.android.domain.Rarity
import com.stepup.android.domain.ShoeCatalog
import com.stepup.android.ui.MainScaffold
import com.stepup.android.ui.Screen
import com.stepup.android.ui.components.S2Stage
import com.stepup.android.ui.experience.ExperienceProvider
import com.stepup.android.ui.screens.gacha.DrawResultDialog
import com.stepup.android.ui.screens.items.SneakerDexScreen
import com.stepup.android.ui.theme.StepUpTheme
import com.stepup.android.ui.theme.ThemeMode
import java.io.File
import java.util.Locale
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

/**
 * 새 신발 도감 70종(2026-09-28, stepup-grade-70) — 뽑기에서 나오는 새 그림이 실제 화면에 어떻게 서는지 찍는다.
 *
 * 등급은 그림 폴더 그대로(grade-1 레어 · grade-2 에픽 · grade-3 레전더리). 신발 탭 무대(등급 프레임 v8) · 보유 칸 ·
 * 도감(70칸, 가진 것만 제 색) · 뽑기 결과. 이 테스트가 넣은 신발은 끝나면 지우고 원래 신던 신발을 다시 신긴다.
 */
class ShoeCatalogDesignTest {
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

    private val directory get() = File(compose.activity.getExternalFilesDir(null), "shoe-catalog").apply { mkdirs() }
    private var originalEquipped: Long? = null

    /** 신발 탭(앱 셸 안) — 새 도감 세 등급을 차례로 골라 무대에 세우고, 보유 칸 · 도감으로 */
    @Test fun newShoesInTheShoesTab() {
        val shoes = seed()
        try {
            edgeToEdge()
            compose.setContent { StepUpTheme(ThemeMode.DARK) { ExperienceProvider { MainScaffold(initialTab = Screen.Customize) } } }
            awaitTag("shoe-list")
            PICKS.forEachIndexed { index, model ->
                val id = shoes.getValue(model)
                list().performScrollToNode(hasTestTag("shoe-choice-$id"))
                compose.onNodeWithTag("shoe-choice-$id").performClick().assertIsSelected()
                list().performScrollToIndex(0)
                val rarity = ShoeCatalog.of(model)!!.rarity
                awaitSingle("grade-stage-${rarity.name.lowercase(Locale.ROOT)}")
                shot("0${index + 1}-stage-$model")
            }
            list().performScrollToNode(hasTestTag("shoe-choice-${shoes.getValue(PICKS.last())}"))
            shot("04-owned-row")
        } finally {
            restore(shoes)
        }
    }

    /** 도감 — 70칸: 가진 모델은 제 색, 없는 것은 회색. 등급 거르개 */
    @Test fun newCatalogDex() {
        val shoes = seed()
        try {
            edgeToEdge()
            compose.setContent { StepUpTheme(ThemeMode.DARK) { ExperienceProvider { Box(Modifier.fillMaxSize()) { S2Stage(Modifier.fillMaxSize()); SneakerDexScreen() } } } }
            compose.waitUntil(10_000) { compose.onAllNodesWithText("${PICKS.size} / ${ShoeCatalog.models.size}").fetchSemanticsNodes().isNotEmpty() }
            shot("05-dex-top")
            val names = PICKS.map { compose.activity.getString(com.stepup.android.ui.components.shoeModelNameRes(it)!!) }
            val scroller = { compose.onNode(hasScrollToIndexAction(), useUnmergedTree = true) }
            scroller().performScrollToNode(hasText(names[1]))
            shot("06-dex-epic")
            scroller().performScrollToNode(hasText(names[2]))
            shot("07-dex-legendary")
            assertEquals(70, ShoeCatalog.models.size)
        } finally {
            restore(shoes)
        }
    }

    /** 뽑기 결과 — 새 도감 레전더리(레드라인 시리즈) 한 켤레 */
    @Test fun drawResultWithANewShoe() {
        val shoes = seed()
        try {
            val shoe = runBlocking { ServiceLocator.sneakerRepository.inventory.first().first { it.id == shoes.getValue(PICKS.last()) } }
            edgeToEdge()
            compose.setContent {
                StepUpTheme(ThemeMode.DARK) {
                    ExperienceProvider { Box(Modifier.fillMaxSize()) { S2Stage(Modifier.fillMaxSize()); DrawResultDialog(shoe, onOpenShoes = {}, onClose = {}) } }
                }
            }
            awaitSingle("grade-stage-legendary")
            shot("08-draw-result-${PICKS.last()}", settle = 1_500)
        } finally {
            restore(shoes)
        }
    }

    // ── 도우미 ─────────────────────────────────────────────────────

    private fun list() = compose.onNodeWithTag("shoe-list")

    /** 새 도감 모델 세 켤레를 넣는다 — 레어 · 에픽 · 레전더리(레드라인) */
    private fun seed(): Map<Int, Long> = runBlocking {
        ServiceLocator.userPrefs.setReducedMotion(true)
        ServiceLocator.userPrefs.setSounds(false)
        ServiceLocator.userPrefs.setHaptics(false)
        ServiceLocator.userPrefs.setGuideSeen()
        ServiceLocator.sneakerRepository.ensureStarter()
        val dao = ServiceLocator.database.sneakerDao()
        val base = requireNotNull(dao.equippedNow() ?: dao.allNow().firstOrNull())
        if (!base.equipped) ServiceLocator.sneakerRepository.equip(base.id)
        originalEquipped = base.id
        val now = System.currentTimeMillis()
        PICKS.associateWith { model ->
            val m = ShoeCatalog.of(model)!!
            dao.insert(
                base.copy(
                    id = 0, rarity = m.rarity.id, variant = 0, level = if (m.rarity == Rarity.LEGENDARY) 3 else 1,
                    mintNumber = dao.maxMintNumber() + 1, equipped = false, acquiredAt = now, serverId = 0, modelId = model,
                ),
            )
        }
    }

    private fun restore(shoes: Map<Int, Long>) = runBlocking {
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
        /** 레어 스플릿 힐 로드 러너 · 에픽 니트 랩 러너 · 레전더리 레드라인 100m 스프린트 스파이크 */
        val PICKS = listOf(1107, 1201, 1311)
    }
}
