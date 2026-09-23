package com.stepup.android

import com.stepup.android.domain.AvatarArt
import com.stepup.android.domain.AvatarArtCatalog
import com.stepup.android.domain.AvatarGender
import com.stepup.android.domain.AvatarLook
import com.stepup.android.domain.AvatarPose
import com.stepup.android.domain.Faction
import com.stepup.android.domain.Outfits
import com.stepup.android.domain.Sneaker
import com.stepup.android.domain.SneakerDesigns
import com.stepup.android.domain.SneakerMint
import com.stepup.android.domain.designCode
import com.stepup.android.domain.designIdFor
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

/**
 * 캐릭터 그림과 장비의 약속.
 *
 *   * 성별은 절대 바뀌지 않는다.
 *   * 입은 것이 그림에 보이면 보인다고, 아니면 아니라고 한다.
 *   * 신발 52 · 의상 5 — 장비 카탈로그(design/equipment, design/equipment/lumi)와 같은 id.
 */
class AvatarArtTest {

    /** 도감 번호로 신발 한 켤레를 만든다 — "WND-010" → 바람 · 일반 · 0 */
    private fun shoe(code: String): Sneaker {
        val faction = when (code.take(3)) {
            "FIR" -> Faction.FIRE
            "WAT" -> Faction.WATER
            "LIT" -> Faction.LIGHTNING
            else -> Faction.WIND
        }
        val (rarity, variant) = SneakerDesigns.slotOf(code.takeLast(3).toInt())
        return SneakerMint.starter().copy(faction = faction, rarity = rarity, variant = variant)
    }

    private val catalog: String by lazy {
        // 단위 테스트는 app/ 에서 돈다
        listOf(File("../design/equipment/equipment-catalog.json"), File("design/equipment/equipment-catalog.json"))
            .first { it.exists() }.readText()
    }

    private fun catalogIds(prefix: String): List<String> =
        Regex("\"id\"\\s*:\\s*\"($prefix[A-Z]*-\\d{3})\"").findAll(catalog).map { it.groupValues[1] }.toList()

    // ── 카탈로그 ─────────────────────────────────────────────────

    @Test
    fun `신발은 52종이고 속성마다 13종 — 카탈로그와 같다`() {
        val codes = AvatarArtCatalog.SHOE_CODES
        assertEquals(52, codes.size)
        assertEquals(52, codes.toSet().size)
        listOf("FIR", "WAT", "LIT", "WND").forEach { p -> assertEquals(p, 13, codes.count { it.startsWith(p) }) }
        val fromCatalog = listOf("FIR", "WAT", "LIT", "WND").flatMap { catalogIds(it) }.toSet()
        assertEquals(fromCatalog, codes.toSet())
    }

    @Test
    fun `의상은 기본 하나와 새 의상 5종 — 카탈로그와 같다`() {
        val nft = Outfits.ALL.filter { it.nft }.map { it.id }
        assertEquals(listOf("CLO-001", "CLO-002", "CLO-003", "CLO-004", "CLO-005"), nft)
        assertEquals(catalogIds("CLO").toSet(), nft.toSet())
        assertEquals(1, Outfits.ALL.count { it.starter })
        assertFalse("볼트 저지는 반팔이다", Outfits.VOLT_JERSEY.longSleeve)
    }

    @Test
    fun `시작 신발 클라우드 러너는 WND-010`() {
        assertEquals("WND-010", SneakerMint.starter().designCode())
    }

    @Test
    fun `모든 그림은 실제 파일이 있다`() {
        val dir = listOf(File("src/main/res/drawable-nodpi"), File("app/src/main/res/drawable-nodpi")).first { it.exists() }
        AvatarArtCatalog.ALL.forEach { art ->
            assertTrue("avatar_${art.key}.webp 가 없다", File(dir, "avatar_${art.key}.webp").exists())
        }
        Outfits.ALL.filter { it.nft }.forEach { o ->
            AvatarGender.entries.forEach { g ->
                val name = "outfit_" + o.designIdFor(g).lowercase().replace('-', '_') + ".webp"
                assertTrue("$name 가 없다", File(dir, name).exists())
            }
        }
    }

    private val lumiCatalog: String by lazy {
        listOf(File("../design/equipment/lumi/equipment-catalog.json"), File("design/equipment/lumi/equipment-catalog.json"))
            .first { it.exists() }.readText()
    }

    @Test
    fun `LUMI 카탈로그 — 신발 52종은 RUNO 와 같은 번호, 의상은 LUM-CLO 5종`() {
        val shoes = Regex("\"id\"\\s*:\\s*\"((FIR|WAT|LIT|WND)-\\d{3})\"").findAll(lumiCatalog).map { it.groupValues[1] }.toSet()
        assertEquals(AvatarArtCatalog.SHOE_CODES.toSet(), shoes)
        val outfits = Regex("\"id\"\\s*:\\s*\"(LUM-CLO-\\d{3})\"").findAll(lumiCatalog).map { it.groupValues[1] }.toList()
        assertEquals(Outfits.ALL.filter { it.nft }.map { it.designIdFor(AvatarGender.FEMALE) }, outfits)
        assertEquals("OUTFIT-BASE", Outfits.STARTER_HOODIE.designIdFor(AvatarGender.FEMALE))
    }

    @Test
    fun `LUMI 그림은 신발 52 · 의상 5 · 기본 1`() {
        val lumi = AvatarArtCatalog.ALL.filter { it.gender == AvatarGender.FEMALE }
        assertEquals(52, lumi.count { it.shoeCode != null })
        assertEquals(5, lumi.count { it.shoeCode == null && it.outfitId != Outfits.BASE_ID })
        assertEquals(1, lumi.count { it.shoeCode == null && it.outfitId == Outfits.BASE_ID })
        // 신발 시트는 속성마다 추천 의상 하나를 입고 있다
        lumi.filter { it.shoeCode != null }.forEach { a ->
            assertEquals(AvatarArtCatalog.LUMI_SHEET_OUTFIT.getValue(a.shoeCode!!.take(3)).id, a.outfitId)
        }
    }

    @Test
    fun `RUNO 그림은 신발 52 · 의상 5 · 기본 2 — 모두 57 + 2`() {
        val runo = AvatarArtCatalog.ALL.filter { it.gender == AvatarGender.MALE }
        assertEquals(52, runo.count { it.shoeCode != null && it.outfitId == Outfits.BASE_ID })
        assertEquals(5, runo.count { it.shoeCode == null && it.outfitId != Outfits.BASE_ID })
        assertEquals(2, runo.count { it.shoeCode == null && it.outfitId == Outfits.BASE_ID })
    }

    // ── 고르기 ──────────────────────────────────────────────────

    @Test
    fun `어떤 착장 어떤 자세에도 성별이 바뀌지 않는다`() {
        val shoes = listOf<Sneaker?>(null) + AvatarArtCatalog.SHOE_CODES.map { shoe(it) }
        AvatarGender.entries.forEach { g ->
            Outfits.ALL.forEach { o ->
                shoes.forEach { s ->
                    AvatarPose.entries.forEach { p ->
                        val r = AvatarArtCatalog.resolve(AvatarLook(gender = g, outfit = o, shoe = s), p)
                        assertEquals(g, r.art.gender)
                    }
                }
            }
        }
    }

    @Test
    fun `RUNO 6 x 53 조합 — 그림이 있는 조합만 그대로 보인다고 한다`() {
        val shoes = listOf<Sneaker?>(null) + AvatarArtCatalog.SHOE_CODES.map { shoe(it) }
        Outfits.ALL.forEach { o ->
            shoes.forEach { s ->
                val r = AvatarArtCatalog.resolve(AvatarLook(gender = AvatarGender.MALE, outfit = o, shoe = s), AvatarPose.IDLE)
                val drawn = o.starter || s == null
                assertEquals("${o.id} + ${s?.designCode()}", drawn, r.lookShown)
                // 그림 속 착장이 말하는 대로다
                assertEquals(r.art.outfitId == o.id, r.outfitShown)
                assertEquals(r.art.shoeCode == s?.designCode(), r.shoeShown)
                if (!r.shoeShown) assertEquals(null, r.art.shoeCode)
                if (!drawn) {
                    // 새 의상 + NFT 신발 — 의상이 보이는 그림을 고르고, 신발은 아니라고 한다
                    assertTrue(r.outfitShown)
                    assertFalse(r.shoeShown)
                }
            }
        }
    }

    @Test
    fun `신발만 바꾸면 의상은 그대로, 의상만 바꾸면 신발은 그대로`() {
        val base = AvatarLook(gender = AvatarGender.MALE, outfit = Outfits.STARTER_HOODIE, shoe = shoe("WND-010"))
        val shoeChanged = AvatarArtCatalog.resolve(base.copy(shoe = shoe("FIR-001")), AvatarPose.IDLE)
        assertEquals(Outfits.BASE_ID, shoeChanged.art.outfitId)
        assertEquals("FIR-001", shoeChanged.art.shoeCode)
        val outfitChanged = AvatarArtCatalog.resolve(base.copy(outfit = Outfits.EMBER_SHELL, shoe = null), AvatarPose.IDLE)
        assertEquals("CLO-002", outfitChanged.art.outfitId)
        assertEquals(null, outfitChanged.art.shoeCode)
    }

    @Test
    fun `대표 혼합 조합도 막히지 않는다 — 같은 색 계열만 되는 것이 아니다`() {
        listOf(Outfits.EMBER_SHELL to "WND-008", Outfits.VOLT_JERSEY to "WAT-004", Outfits.AERO_WINDBREAKER to "FIR-001").forEach { (o, c) ->
            val r = AvatarArtCatalog.resolve(AvatarLook(gender = AvatarGender.MALE, outfit = o, shoe = shoe(c)), AvatarPose.RUN)
            assertEquals(o.id, r.art.outfitId)
            assertFalse(r.lookShown) // 신발 그림은 아직 없다고 말해야 한다
        }
    }

    @Test
    fun `홈과 내 정보는 같은 착장을 같은 그림으로 — 착장이 자세보다 먼저다`() {
        val look = AvatarLook(gender = AvatarGender.MALE, shoe = shoe("WND-010"))
        val home = AvatarArtCatalog.resolve(look, AvatarPose.RUN)
        val profile = AvatarArtCatalog.resolve(look, AvatarPose.IDLE)
        assertEquals(profile.art, home.art)
        assertTrue(home.lookShown)
    }

    @Test
    fun `기본 착장이면 달리는 그림을 쓴다`() {
        val r = AvatarArtCatalog.resolve(AvatarLook(gender = AvatarGender.MALE, shoe = null), AvatarPose.RUN)
        assertEquals(AvatarArt.MALE_RUN, r.art)
        assertTrue(r.exactPose && r.lookShown)
    }

    @Test
    fun `LUMI 6 x 53 조합 — 그림이 있는 조합만 그대로 보인다고 한다`() {
        val shoes = listOf<Sneaker?>(null) + AvatarArtCatalog.SHOE_CODES.map { shoe(it) }
        var shown = 0
        Outfits.ALL.forEach { o ->
            shoes.forEach { s ->
                val r = AvatarArtCatalog.resolve(AvatarLook(gender = AvatarGender.FEMALE, outfit = o, shoe = s), AvatarPose.IDLE)
                val code = s?.designCode()
                val drawn = s == null || (o.nft && AvatarArtCatalog.LUMI_SHEET_OUTFIT.getValue(code!!.take(3)) == o)
                assertEquals("${o.id} + $code", drawn, r.lookShown)
                assertEquals(r.art.outfitId == o.id, r.outfitShown)
                assertEquals(r.art.shoeCode == code, r.shoeShown)
                // 의상(모자 색까지)이 먼저다 — 입은 옷이 다른 그림은 고르지 않는다
                assertTrue("${o.id} + $code", r.outfitShown)
                // 신발이 안 보인다고 할 때 그림 속 신발은 기본 운동화다(다른 NFT 신발이 아니다)
                if (!r.shoeShown) assertEquals("${o.id} + $code", null, r.art.shoeCode)
                if (r.lookShown) shown++
            }
        }
        // 기본 착장 1 + 새 의상 5 + 추천 의상 × 신발 52
        assertEquals(1 + 5 + 52, shown)
    }

    @Test
    fun `LUMI — 신발을 바꿔도 의상 · 모자는 그대로, 의상을 바꾸면 모자가 함께 바뀐다`() {
        val base = AvatarLook(gender = AvatarGender.FEMALE, outfit = Outfits.EMBER_SHELL, shoe = shoe("FIR-001"))
        val r1 = AvatarArtCatalog.resolve(base, AvatarPose.IDLE)
        assertEquals("lumi_idle_fir_001", r1.art.key)
        assertTrue(r1.lookShown)
        val r2 = AvatarArtCatalog.resolve(base.copy(shoe = shoe("FIR-008")), AvatarPose.IDLE)
        assertEquals(Outfits.EMBER_SHELL.id, r2.art.outfitId)
        assertEquals("FIR-008", r2.art.shoeCode)
        val r3 = AvatarArtCatalog.resolve(base.copy(outfit = Outfits.CORE_ZIP, shoe = null), AvatarPose.IDLE)
        assertEquals("lumi_idle_lum_clo_001", r3.art.key)
    }

    @Test
    fun `LUMI 대표 혼합 조합 — 의상 그림에 신발은 아니라고 적는다`() {
        listOf(Outfits.EMBER_SHELL to "WND-008", Outfits.VOLT_JERSEY to "WAT-004", Outfits.AERO_WINDBREAKER to "FIR-001").forEach { (o, c) ->
            val look = AvatarLook(gender = AvatarGender.FEMALE, outfit = o, shoe = shoe(c))
            val home = AvatarArtCatalog.resolve(look, AvatarPose.RUN)
            val profile = AvatarArtCatalog.resolve(look, AvatarPose.IDLE)
            assertEquals(profile.art, home.art)
            assertEquals(o.id, home.art.outfitId)
            assertFalse(home.shoeShown)
        }
    }

    @Test
    fun `LUMI 기본 의상 + 시작 신발 — 입지 않은 옷의 그림을 쓰지 않는다`() {
        val r = AvatarArtCatalog.resolve(AvatarLook(gender = AvatarGender.FEMALE, shoe = shoe("WND-010")), AvatarPose.IDLE)
        assertEquals(AvatarArt.FEMALE_IDLE, r.art)
        assertFalse(r.shoeShown)
    }

    @Test
    fun `추가로 필요한 자세 목록`() {
        assertEquals(
            setOf(
                AvatarGender.MALE to AvatarPose.CHEER,
                AvatarGender.FEMALE to AvatarPose.RUN,
                AvatarGender.FEMALE to AvatarPose.CHEER,
            ),
            AvatarArtCatalog.missingPoses().toSet(),
        )
    }
}
