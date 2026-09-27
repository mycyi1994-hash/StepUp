package com.stepup.android

import com.stepup.android.domain.Rarity
import com.stepup.android.ui.components.GradeArtRatio
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * 신발 등급 프레임 v8 — 앱에는 현재 등급 넷(일반 · 레어 · 에픽 · 레전더리)의 레이어만 들어간다.
 * 레드라인 · 피니시(05 · 06)는 확장 콘셉트라 도메인 등급에도, 앱 그림에도 없다(docs/redesign/shoe-grade-frames).
 */
class ShoeGradeArtTest {
    private val gradeArt = R.drawable::class.java.fields.map { it.name }.filter { it.startsWith("shoe_grade_") }.toSet()

    @Test fun theDomainKeepsFourGrades() {
        assertEquals(listOf("COMMON", "RARE", "EPIC", "LEGENDARY"), Rarity.entries.map { it.id })
    }

    @Test fun everyGradeHasItsLayersAndNothingElse() {
        val expected = Rarity.entries.flatMap { rarity ->
            val key = rarity.name.lowercase()
            listOfNotNull(
                "shoe_grade_${key}_back",
                // 일반(01)은 시안의 앞 효과가 비어 있다
                if (rarity == Rarity.COMMON) null else "shoe_grade_${key}_front",
                "shoe_grade_${key}_frame",
                "shoe_grade_${key}_frame_small",
            )
        }.toSet()
        assertEquals(expected, gradeArt)
        assertTrue("no expansion-concept art in the app: $gradeArt",
            gradeArt.none { "redline" in it || "finish" in it || "_05" in it || "_06" in it })
    }

    @Test fun layersShareTheHandoffCoordinateSystem() {
        assertEquals(440f / 418f, GradeArtRatio, 0.0001f)
    }
}
