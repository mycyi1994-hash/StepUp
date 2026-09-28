package com.stepup.android

import android.graphics.Bitmap
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.requiredSize
import androidx.compose.foundation.layout.requiredWidth
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.semantics.getOrNull
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.test.platform.app.InstrumentationRegistry
import com.stepup.android.core.ServiceLocator
import com.stepup.android.data.local.SneakerEntity
import com.stepup.android.domain.Faction
import com.stepup.android.domain.Rarity
import com.stepup.android.domain.ServerStats
import com.stepup.android.domain.ShoeCatalog
import com.stepup.android.domain.ShoeTier
import com.stepup.android.domain.Sneaker
import com.stepup.android.domain.tier
import com.stepup.android.ui.MainScaffold
import com.stepup.android.ui.BOTTOM_NAV_TAG
import com.stepup.android.ui.Screen
import com.stepup.android.ui.components.CommerceBackdrop
import com.stepup.android.ui.components.S2ShoesSections
import com.stepup.android.ui.components.ShoeGradeBadge
import com.stepup.android.ui.components.ShoeNameWithBadge
import com.stepup.android.ui.components.ShoeSection
import com.stepup.android.ui.components.shoeModelNameRes
import com.stepup.android.ui.experience.ExperienceProvider
import com.stepup.android.ui.screens.customize.MyShoesContent
import com.stepup.android.ui.screens.customize.ShoeVaultContent
import com.stepup.android.ui.screens.items.ItemSort
import com.stepup.android.ui.screens.items.OwnedLoad
import com.stepup.android.ui.theme.StepUpTheme
import com.stepup.android.ui.theme.ThemeMode
import java.io.File
import kotlin.math.abs
import kotlin.math.roundToInt
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

/**
 * 신발 화면 확정안(2026-09-28, docs/redesign/shoes-ui-2026-09-28) — 내 신발 · 신발 보관함 · 이름 끝의 둥근 등급 배지 여섯.
 *
 * [inTheAppAtDeviceSizes] 는 앱 셸(공통 머리 · 하단 탭 다섯) 그대로 360×800 · 390×844 · 412×915(dp) 화면을 만든다 — 에뮬레이터 창
 * 높이를 그 dp 로 채우도록 밀도를 낮춰, 실제 기기와 같은 dp 크기에서 그린다. 이 기기의 신발 창고에 여섯 갈래를 한 켤레씩 넣고
 * 내 신발이 세로 스크롤 거리 0 으로 무대 · 능력치 막대 셋 · 보유 목록을 하단 탭 위에 다 보이는지 재고 찍는다. 보관함 2열 · 보유 수 ·
 * 거르기 수 · 정렬 · 착용 체크 한 켤레, 관리(⋯)가 착용을 바꾸지 않는 것도 본다. 넣은 신발의 능력치는 서버 범위 안의 예시 값(이 테스트
 * 기기에만 있다)이고, 끝나면 지우고 원래 착용을 돌려놓는다.
 * [scenes] 는 기기에서 만들 수 없는 상태(읽는 중 · 실패 · 빈 목록)와 긴 이름 · 배지 여섯 · 큰 글씨 · 밝은 테마를 바로 넣어 그린다.
 */
class MyShoesDesignTest {
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

    private val directory get() = File(compose.activity.getExternalFilesDir(null), "my-shoes").apply { mkdirs() }
    private var originalEquipped: Long? = null

    private data class Viewport(val width: Int, val height: Int, val font: Float = 1f, val mode: ThemeMode = ThemeMode.DARK) {
        val label get() = "${width}x$height" + (if (font != 1f) "-font${(font * 100).roundToInt()}" else "") +
            (if (mode == ThemeMode.LIGHT) "-light" else "")
    }

    private var viewport by mutableStateOf(Viewport(360, 800))

    @Test fun inTheAppAtDeviceSizes() {
        val seeded = seed()
        val dao = ServiceLocator.database.sneakerDao()
        try {
            edgeToEdge()
            compose.setContent { DeviceFrame { MainScaffold(initialTab = Screen.Customize) } }
            val owned = runBlocking { dao.allNow().size }
            for (next in listOf(Viewport(360, 800), Viewport(390, 844), Viewport(412, 915))) {
                show(next)
                // 01 — 내 신발: 신고 있는 레어(1107)가 이름 끝의 레어 배지와 함께, 세로로 끌지 않고 다 보인다
                assertName(modelName(1107))
                assertBadge(ShoeTier.RARE)
                assertFitsWithoutScrolling()
                shot("01-my-shoes-${next.label}")
                // 02 — 신발 보관함: 보유 수 · 2열 · 착용 체크는 한 켤레
                openVault(owned)
                shot("02-vault-${next.label}")
                compose.onNodeWithTag("shoes-section-mine").performClick()
                awaitTag("my-shoes")
            }

            // 03 — 여섯 갈래를 차례로(390×844): 무대 프레임 · 배지가 같은 갈래, 여전히 스크롤 없음
            show(Viewport(390, 844))
            for ((tier, id) in seeded.byTier) {
                pick(id)
                awaitHero(tier)
                assertBadge(tier)
                assertFitsWithoutScrolling()
                shot("03-tier-${tier.key}")
            }
            assertEquals("Picking pairs must not change the stored equipment", seeded.worn, runBlocking { dao.equippedNow()?.id })

            // 04 — 막대 기준(모든 신발에 같은 기준) — 능력치 덩어리를 누르면
            compose.onNodeWithTag("shoe-stats").performClick()
            awaitTag("shoe-basis-sheet")
            compose.onNodeWithTag("shoe-basis-note", useUnmergedTree = true).assertExists()
            shot("04-stat-basis", whole = true)
            back()
            awaitGone("shoe-basis-sheet")

            // 05 — 관리(⋯): 그 켤레의 기존 상세(신기 · 강화 · 수리 · 판매). 여는 것만으로 착용은 그대로
            pick(seeded.byTier.getValue(ShoeTier.EPIC))
            compose.onNodeWithTag("shoe-manage").performClick()
            awaitTag("detail-primary-action")
            shot("05-manage-detail")
            assertEquals(seeded.worn, runBlocking { dao.equippedNow()?.id })
            back()
            awaitTag("my-shoes")

            // 06 — 보관함 거르기(실제 수) · 정렬
            openVault(owned)
            compose.onNodeWithTag("vault-filter-epic").performScrollTo().performClick().assertIsSelected()
            compose.waitForIdle()
            val epicCards = cards()
            val epicOwned = runBlocking { ownedTiers() }.count { it == ShoeTier.EPIC }
            assertEquals("epic filter shows every epic pair and nothing else", epicOwned, epicCards.size)
            shot("06-vault-filter-epic")
            compose.onNodeWithTag("vault-filter-all").performScrollTo().performClick().assertIsSelected()
            compose.onNodeWithTag("vault-sort").performClick()
            compose.waitForIdle()
            shot("07-vault-sort-sheet", whole = true)
            compose.onNodeWithText(string(R.string.items_sort_rarity)).performClick()
            compose.waitForIdle()
            // 정렬을 바꾸면 격자는 맨 위(새 순서의 첫 켤레)부터 — 왼쪽 위 칸이 가장 높은 등급
            val first = cards().minWith(compareBy({ it.boundsInRoot.top }, { it.boundsInRoot.left }))
            val firstId = first.config[SemanticsProperties.TestTag].removePrefix("vault-card-").toLong()
            assertEquals("highest rarity first", Rarity.LEGENDARY.id, runBlocking { dao.byId(firstId)?.rarity })
            shot("08-vault-sorted-rarity")

            // 09 · 10 — 큰 글씨(1.3) 360×800: 무대부터 줄이고, 모자라면 그때만 넘긴다 — 글자는 잘리지 않는다
            show(Viewport(360, 800, font = 1.3f))
            assertName(modelName(1107))
            shot("09-my-shoes-large-font-top")
            compose.onNodeWithTag("shoe-owned-row").performScrollTo()
            shot("10-my-shoes-large-font-bottom")
            openVault(owned)
            shot("11-vault-large-font")

            // 12 · 13 — 밝은 테마 390×844
            show(Viewport(390, 844, mode = ThemeMode.LIGHT))
            assertFitsWithoutScrolling()
            shot("12-my-shoes-light")
            openVault(owned)
            shot("13-vault-light")
        } finally {
            restore(seeded)
        }
    }

    /** 기기에서 만들 수 없는 상태 · 긴 이름 · 배지 여섯 — 상태를 화면에 바로 넣는다(값은 이 테스트 화면에만 있는 예시) */
    @Test fun scenes() {
        prepare()
        edgeToEdge()
        var scene by mutableStateOf<@Composable () -> Unit>({})
        var light by mutableStateOf(false)
        var large by mutableStateOf(false)
        compose.setContent {
            val density = LocalDensity.current
            CompositionLocalProvider(LocalDensity provides Density(density.density, if (large) 1.3f else 1f)) {
                StepUpTheme(if (light) ThemeMode.LIGHT else ThemeMode.DARK) {
                    ExperienceProvider {
                        Box(Modifier.fillMaxSize()) {
                            CommerceBackdrop(Modifier.fillMaxSize())
                            Box(Modifier.fillMaxSize().statusBarsPadding()) { scene() }
                        }
                    }
                }
            }
        }
        fun show(name: String, readyTag: String, content: @Composable () -> Unit) {
            compose.runOnIdle { scene = { key(name) { content() } } }
            awaitTag(readyTag)
            shot(name, whole = true)
        }
        fun mine(load: OwnedLoad, selected: Long? = null, basis: Boolean = false): @Composable () -> Unit = {
            Column(Modifier.fillMaxSize()) {
                S2ShoesSections(ShoeSection.MINE, onSelect = {})
                MyShoesContent(load, selected, {}, {}, {}, {}, Modifier.weight(1f), initialBasis = basis)
            }
        }
        fun vault(load: OwnedLoad, filter: String? = null, sort: String = ItemSort.RECENT): @Composable () -> Unit = {
            Column(Modifier.fillMaxSize()) {
                S2ShoesSections(ShoeSection.VAULT, onSelect = {})
                ShoeVaultContent(
                    load = load, selectedId = null, sort = sort, filter = filter, onSort = {}, onFilter = {}, onOpen = {},
                    onOpenDraw = {}, onOpenDex = {}, onOpenMarket = {}, onOpenItems = {}, onReload = {}, modifier = Modifier.weight(1f),
                )
            }
        }

        // s01 — 배지 여섯: 이름 끝 같은 줄, 긴 이름은 두 줄 끝에(320dp 폭)
        show("s01-badges", "tier-badge-finish") {
            Column(Modifier.fillMaxSize().padding(20.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
                ShoeTier.entries.forEach { ShoeGradeBadge(it) }
                Box(Modifier.requiredWidth(320.dp)) {
                    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        SAMPLES.filter { it.modelId != null }.forEach { shoe ->
                            ShoeNameWithBadge(
                                name = compose.activity.getString(shoeModelNameRes(requireNotNull(shoe.modelId))!!), tier = shoe.tier,
                                style = TextStyle(color = Color.White, fontSize = 21.sp, fontWeight = FontWeight.SemiBold),
                                modifier = Modifier.fillMaxWidth().testTag("sample-name-${shoe.id}"),
                            )
                        }
                    }
                }
            }
        }
        // 긴 이름(피니시 1329)도 두 줄 안에서 배지가 마지막 줄 끝에 남는다
        val longName = compose.onNodeWithTag("sample-name-${LONG.id}", useUnmergedTree = true).fetchSemanticsNode()
        val longBadge = compose.onNode(hasTestTag("tier-badge-finish") and hasAnyAncestor(hasTestTag("sample-name-${LONG.id}")),
            useUnmergedTree = true).fetchSemanticsNode()
        assertTrue("badge stays inside the two-line name", longBadge.boundsInRoot.bottom <= longName.boundsInRoot.bottom + 1)

        show("s02-my-shoes-long-name", "shoe-hero", mine(OwnedLoad.Ready(SAMPLES), LONG.id))
        show("s03-my-shoes-empty", "shoe-tab-empty", mine(OwnedLoad.Ready(emptyList())))
        compose.onNodeWithTag("shoe-state-primary").assertTextEquals(string(R.string.sdv_empty_action))
        show("s04-my-shoes-loading", "shoe-tab-loading", mine(OwnedLoad.Loading))
        compose.onAllNodesWithTag("shoe-tab-empty").assertCountEquals(0)
        show("s05-my-shoes-failed", "shoe-tab-failed", mine(OwnedLoad.Failed))
        show("s06-stat-basis", "shoe-basis-sheet", mine(OwnedLoad.Ready(SAMPLES), SAMPLES.first().id, basis = true))
        show("s07-vault", "vault-grid", vault(OwnedLoad.Ready(SAMPLES)))
        assertEquals("${SAMPLES.size}", text("vault-count"))
        show("s08-vault-filter-finish", "vault-grid", vault(OwnedLoad.Ready(SAMPLES), filter = ShoeTier.FINISH.key))
        assertEquals(1, cards().size)
        show("s09-vault-empty", "vault-empty", vault(OwnedLoad.Ready(emptyList())))
        show("s10-vault-loading", "vault-loading", vault(OwnedLoad.Loading))
        show("s11-vault-failed", "vault-failed", vault(OwnedLoad.Failed))

        compose.runOnIdle { large = true }
        show("s12-large-font-my-shoes", "shoe-hero", mine(OwnedLoad.Ready(SAMPLES), LONG.id))
        show("s13-large-font-vault", "vault-grid", vault(OwnedLoad.Ready(SAMPLES)))
        compose.runOnIdle { large = false; light = true }
        show("s14-light-badges", "tier-badge-finish") {
            Column(Modifier.fillMaxSize().padding(20.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
                ShoeTier.entries.forEach { ShoeGradeBadge(it) }
            }
        }
        show("s15-light-my-shoes", "shoe-hero", mine(OwnedLoad.Ready(SAMPLES), SAMPLES.first().id))
    }

    // ── 앱 셸을 기기 크기로 ────────────────────────────────────────

    /** 에뮬레이터 창 안에 [viewport] dp 화면을 만든다 — 창 높이를 그 dp 로 채우는 밀도(폭이 모자라면 폭에 맞춘다) */
    @Composable private fun DeviceFrame(content: @Composable () -> Unit) {
        BoxWithConstraints(Modifier.fillMaxSize().background(Color.Black), contentAlignment = Alignment.TopCenter) {
            val v = viewport
            val px = minOf(constraints.maxHeight / v.height.toFloat(), constraints.maxWidth / v.width.toFloat())
            CompositionLocalProvider(LocalDensity provides Density(px, v.font)) {
                StepUpTheme(v.mode) {
                    ExperienceProvider {
                        Box(Modifier.requiredSize(v.width.dp, v.height.dp).testTag(VIEWPORT)) {
                            key(v) { content() }
                        }
                    }
                }
            }
        }
    }

    private fun show(next: Viewport) {
        compose.runOnIdle { viewport = next }
        compose.waitForIdle()
        awaitTag("my-shoes")
        awaitTag("shoe-hero")
    }

    /** 세로 스크롤 거리 0 — 무대 · 능력치 · 보유 목록이 창 안, 하단 탭 위에 */
    private fun assertFitsWithoutScrolling() {
        compose.waitForIdle()
        val scroll = compose.onNodeWithTag("my-shoes").fetchSemanticsNode().config[SemanticsProperties.VerticalScrollAxisRange]
        assertEquals("my shoes must not scroll at ${viewport.label}", 0f, scroll.maxValue(), 1f)
        val nav = bounds(BOTTOM_NAV_TAG)
        val frame = bounds(VIEWPORT)
        for (tag in listOf("shoe-hero-name", "shoe-hero", "shoe-stats", "shoe-owned-row")) {
            val b = bounds(tag)
            assertTrue("$tag inside the screen at ${viewport.label}: $b in $frame", b.top >= frame.top - 1 && b.bottom <= frame.bottom + 1)
            assertTrue("$tag above the bottom tabs at ${viewport.label}: $b / nav ${nav.top}", b.bottom <= nav.top + 1)
        }
        val stage = bounds("shoe-hero")
        assertTrue("the framed shoe keeps 440:418 at ${viewport.label}: $stage",
            abs(stage.width / stage.height - 440f / 418f) < 0.03f)
    }

    /** 무대 위 이름 — 이름 칸(제목 한 덩어리)은 글 · 배지를 감싼다. 그 안의 글에 [name] 이 있다(끝에 배지 대체 글) */
    private fun assertName(name: String) {
        compose.waitUntil(10_000) {
            compose.onAllNodes(hasText(name, substring = true) and hasAnyAncestor(hasTestTag("shoe-hero-name")), useUnmergedTree = true)
                .fetchSemanticsNodes().isNotEmpty()
        }
    }

    /** 배지는 이름 글 안(같은 덩어리) — 따로 한 줄을 차지하지 않는다 */
    private fun assertBadge(tier: ShoeTier) {
        compose.waitUntil(10_000) {
            compose.onAllNodes(hasTestTag("tier-badge-${tier.key}") and hasAnyAncestor(hasTestTag("shoe-hero-name")), useUnmergedTree = true)
                .fetchSemanticsNodes().size == 1
        }
        val badge = compose.onNode(hasTestTag("tier-badge-${tier.key}") and hasAnyAncestor(hasTestTag("shoe-hero-name")),
            useUnmergedTree = true).fetchSemanticsNode().boundsInRoot
        val name = bounds("shoe-hero-name")
        assertTrue("badge ${tier.key} sits inside the name block: $badge in $name",
            badge.top >= name.top - 1 && badge.bottom <= name.bottom + 1 && badge.right <= name.right + 1)
    }

    private fun awaitHero(tier: ShoeTier) {
        compose.waitUntil(10_000) {
            compose.onAllNodes(hasTestTag("grade-stage-${tier.key}") and hasAnyAncestor(hasTestTag("shoe-hero")), useUnmergedTree = true)
                .fetchSemanticsNodes().size == 1
        }
    }

    private fun openVault(owned: Int) {
        compose.onNodeWithTag("shoes-section-vault").performClick()
        awaitTag("vault-grid")
        compose.waitForIdle()
        assertEquals("vault count is the real number of pairs", "$owned", text("vault-count"))
        val shown = cards().map { it.boundsInRoot }.sortedWith(compareBy({ it.top }, { it.left }))
        if (shown.size >= 2) {
            assertTrue("two columns at ${viewport.label}: ${shown[0]} · ${shown[1]}",
                abs(shown[0].top - shown[1].top) < 2f && shown[1].left > shown[0].right)
        }
        assertTrue("at most one worn check", compose.onAllNodes(hasTestTag("shoe-worn-badge") and hasAnyAncestor(hasTestTag("vault-grid")),
            useUnmergedTree = true).fetchSemanticsNodes().size <= 1)
    }

    /** 격자에 놓인 칸 — 미리 만들어 두고 아직 놓지 않은 칸(넘길 때의 준비분)은 빼고 */
    private fun cards() = compose.onAllNodes(
        SemanticsMatcher("vault card") { it.config.getOrNull(SemanticsProperties.TestTag)?.startsWith("vault-card-") == true },
        useUnmergedTree = true,
    ).fetchSemanticsNodes().filter { it.layoutInfo.isPlaced }

    private fun pick(id: Long) {
        compose.onNodeWithTag("shoe-owned-row").performScrollToNode(hasTestTag("shoe-choice-$id"))
        compose.onNodeWithTag("shoe-choice-$id").performClick().assertIsSelected()
        compose.waitForIdle()
    }

    private fun text(tag: String): String =
        compose.onNodeWithTag(tag, useUnmergedTree = true).fetchSemanticsNode().config[SemanticsProperties.Text].joinToString { it.text }

    private fun bounds(tag: String): Rect = compose.onNodeWithTag(tag, useUnmergedTree = true).fetchSemanticsNode().boundsInRoot

    private fun modelName(model: Int): String = compose.activity.getString(requireNotNull(shoeModelNameRes(model)))

    private fun string(id: Int): String = compose.activity.getString(id)

    // ── 신발 창고 ────────────────────────────────────────────────

    private data class Seeded(val byTier: Map<ShoeTier, Long>, val worn: Long) {
        val all get() = byTier.values.toList()
    }

    private suspend fun ownedTiers(): List<ShoeTier> =
        ServiceLocator.database.sneakerDao().allNow().map { entity ->
            val model = entity.modelId.takeIf { it > 0 }?.let(ShoeCatalog::of)
            when (model?.series) {
                "REDLINE" -> ShoeTier.REDLINE
                "FINISH" -> ShoeTier.FINISH
                else -> when (Rarity.of(entity.rarity)) {
                    Rarity.COMMON -> ShoeTier.COMMON
                    Rarity.RARE -> ShoeTier.RARE
                    Rarity.EPIC -> ShoeTier.EPIC
                    Rarity.LEGENDARY -> ShoeTier.LEGENDARY
                }
            }
        }

    private fun prepare() = runBlocking {
        ServiceLocator.userPrefs.setReducedMotion(true)
        ServiceLocator.userPrefs.setSounds(false)
        ServiceLocator.userPrefs.setHaptics(false)
        ServiceLocator.userPrefs.setGuideSeen()
    }

    /**
     * 여섯 갈래 한 켤레씩 — 일반은 예전 52종, 나머지는 새 도감(레어 1107 · 에픽 1201 · 레전더리 1301 · 레드라인 1312 · 피니시 1329 — 가장 긴 이름).
     * 서버 신발 모양의 능력치는 서버 범위(0022 economy) 안의 예시 값이다. 레어를 신긴다(서버를 거치지 않는 기존 착용 길 — equipExclusively).
     */
    private fun seed(): Seeded = runBlocking {
        prepare()
        ServiceLocator.sneakerRepository.ensureStarter()
        val dao = ServiceLocator.database.sneakerDao()
        val base = requireNotNull(dao.equippedNow() ?: dao.allNow().firstOrNull())
        originalEquipped = base.id
        val now = System.currentTimeMillis()
        suspend fun add(model: Int?, rarity: Rarity, level: Int, efficiency: Int, comfort: Int, durability: Double, minutesAgo: Int) = dao.insert(
            SneakerEntity(
                factionId = Faction.WIND.id, rarity = rarity.id, variant = 0, level = level, mintNumber = dao.maxMintNumber() + 1,
                luck = 1.0, comfort = 1.0, durability = durability.toInt(), equipped = false, acquiredAt = now - minutesAgo * 60_000L,
                origin = if (model == null) "" else "FREE_DRAW", efficiencyBps = efficiency, comfortBps = comfort, durabilityPts = durability,
                maxLevel = rarity.maxLevel, status = "OWNED", modelId = model ?: 0,
            ),
        )
        val byTier = linkedMapOf(
            ShoeTier.COMMON to add(null, Rarity.COMMON, 1, 0, 0, 100.0, 50),
            ShoeTier.RARE to add(1107, Rarity.RARE, 1, 480, 420, 92.6, 40),
            ShoeTier.EPIC to add(1201, Rarity.EPIC, 3, 860, 740, 88.0, 30),
            ShoeTier.LEGENDARY to add(1301, Rarity.LEGENDARY, 5, 1180, 1010, 76.4, 20),
            ShoeTier.REDLINE to add(1312, Rarity.LEGENDARY, 1, 1240, 950, 100.0, 10),
            ShoeTier.FINISH to add(1329, Rarity.LEGENDARY, 2, 1060, 1150, 64.0, 1),
        )
        val worn = byTier.getValue(ShoeTier.RARE)
        dao.equipExclusively(worn)
        Seeded(byTier, worn)
    }

    private fun restore(seeded: Seeded) = runBlocking {
        val dao = ServiceLocator.database.sneakerDao()
        originalEquipped?.let { dao.equipExclusively(it) }
        seeded.all.filter { it != originalEquipped }.forEach { id -> dao.byId(id)?.let { dao.delete(it) } }
    }

    private fun edgeToEdge() {
        compose.activityRule.scenario.onActivity {
            it.enableEdgeToEdge(
                statusBarStyle = SystemBarStyle.dark(android.graphics.Color.TRANSPARENT),
                navigationBarStyle = SystemBarStyle.dark(android.graphics.Color.TRANSPARENT),
            )
        }
    }

    private fun back() {
        InstrumentationRegistry.getInstrumentation().sendKeyDownUpSync(android.view.KeyEvent.KEYCODE_BACK)
        compose.waitForIdle()
    }

    private fun awaitTag(tag: String) {
        compose.waitUntil(10_000) { compose.onAllNodesWithTag(tag, useUnmergedTree = true).fetchSemanticsNodes().isNotEmpty() }
    }

    private fun awaitGone(tag: String) {
        compose.waitUntil(10_000) { compose.onAllNodesWithTag(tag, useUnmergedTree = true).fetchSemanticsNodes().isEmpty() }
    }

    /** 기기 화면 — 기기 크기 화면만 잘라 남긴다([whole] 이면 시트 · 다른 창까지 화면 전체) */
    private fun shot(name: String, whole: Boolean = false, settle: Long = 700) {
        compose.waitForIdle()
        InstrumentationRegistry.getInstrumentation().waitForIdleSync()
        Thread.sleep(settle)
        awaitFrameOnScreen(compose.activity)
        val file = File(directory, "$name.png")
        val frame = if (whole) null else runCatching { bounds(VIEWPORT) }.getOrNull()
        if (frame == null) {
            captureDisplay(file)
            return
        }
        val screen = InstrumentationRegistry.getInstrumentation().uiAutomation.takeScreenshot() ?: error("no screenshot: $name")
        try {
            val left = frame.left.roundToInt().coerceIn(0, screen.width - 1)
            val top = frame.top.roundToInt().coerceIn(0, screen.height - 1)
            val width = frame.width.roundToInt().coerceAtMost(screen.width - left)
            val height = frame.height.roundToInt().coerceAtMost(screen.height - top)
            val cropped = Bitmap.createBitmap(screen, left, top, width, height)
            file.outputStream().use { check(cropped.compress(Bitmap.CompressFormat.PNG, 100, it)) }
            cropped.recycle()
        } finally {
            screen.recycle()
        }
    }

    private companion object {
        const val VIEWPORT = "device-viewport"

        fun server(efficiencyBps: Int, comfortBps: Int, durability: Double, maxLevel: Int) = ServerStats(
            origin = "FREE_DRAW", efficiencyBps = efficiencyBps, comfortBps = comfortBps, durabilityPts = durability, maxLevel = maxLevel,
            status = "OWNED", chainState = "APP", canWithdraw = true, upgradeCost = 120.0, repairCostPerPoint = 0.0, genesisNo = 0,
        )

        fun sample(id: Long, model: Int?, rarity: Rarity, mint: Int, level: Int, e: Int, c: Int, d: Double, worn: Boolean = false) = Sneaker(
            id = id, faction = Faction.WIND, rarity = rarity, variant = 0, level = level, mintNumber = mint,
            luck = 1.0, comfort = 1.0, durability = d.toInt(), equipped = worn, acquiredAt = 1_790_000_000_000L - id * 60_000L,
            server = if (model == null) null else server(e, c, d, rarity.maxLevel), modelId = model,
        )

        /** 여섯 갈래 예시 — 이 테스트 화면에만 있는 번호 · 능력치 */
        val SAMPLES = listOf(
            sample(9101, 1107, Rarity.RARE, 1, 1, 480, 420, 92.6, worn = true),
            sample(9102, 1201, Rarity.EPIC, 2, 3, 860, 740, 88.0),
            sample(9103, 1301, Rarity.LEGENDARY, 3, 5, 1180, 1010, 76.4),
            sample(9104, 1312, Rarity.LEGENDARY, 4, 1, 1240, 950, 100.0),
            sample(9105, 1329, Rarity.LEGENDARY, 5, 2, 1060, 1150, 64.0),
            sample(9106, null, Rarity.COMMON, 6, 1, 0, 0, 100.0),
        )
        val LONG = SAMPLES.first { it.modelId == 1329 }
    }
}
