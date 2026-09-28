package com.stepup.android

import com.stepup.android.domain.Rarity
import com.stepup.android.domain.ShoeTier
import com.stepup.android.ui.components.GradeArtRatio
import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * 신발 등급 프레임 v8 — 앱에는 보이는 갈래 여섯(일반 · 레어 · 에픽 · 레전더리 · 레드라인 · 피니시)의 레이어가 들어간다
 * (신발 화면 확정안 2026-09-28). 레드라인 · 피니시는 새 도감 레전더리의 시리즈 그림일 뿐 서버 · 보상 · 확률의 등급은 넷 그대로다.
 * 좁은 자리(격자 · 보유 칸)는 1배(_small) 레이어를 쓴다.
 */
class ShoeGradeArtTest {
    private val gradeArt = R.drawable::class.java.fields.map { it.name }.filter { it.startsWith("shoe_grade_") }.toSet()

    @Test fun theDomainKeepsFourGrades() {
        assertEquals(listOf("COMMON", "RARE", "EPIC", "LEGENDARY"), Rarity.entries.map { it.id })
        // 보이는 갈래 여섯은 모두 서버 등급 넷 중 하나다
        assertEquals(
            listOf(Rarity.COMMON, Rarity.RARE, Rarity.EPIC, Rarity.LEGENDARY, Rarity.LEGENDARY, Rarity.LEGENDARY),
            ShoeTier.entries.map { it.rarity },
        )
    }

    @Test fun everyTierHasItsLayersAndNothingElse() {
        val expected = ShoeTier.entries.flatMap { tier ->
            val key = tier.key
            // 일반(01)은 시안의 앞 효과가 비어 있다
            val front = if (tier == ShoeTier.COMMON) emptyList() else listOf("shoe_grade_${key}_front", "shoe_grade_${key}_front_small")
            listOf("shoe_grade_${key}_back", "shoe_grade_${key}_back_small", "shoe_grade_${key}_frame", "shoe_grade_${key}_frame_small") + front
        }.toSet()
        assertEquals(expected, gradeArt)
    }

    @Test fun layersShareTheHandoffCoordinateSystem() {
        assertEquals(440f / 418f, GradeArtRatio, 0.0001f)
    }
}
