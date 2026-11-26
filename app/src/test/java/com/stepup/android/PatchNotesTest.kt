package com.stepup.android

import com.stepup.android.domain.PatchNotes
import com.stepup.android.domain.PatchNotesRules
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

/** 업데이트 소식 창 · 내역(2026-11-26 v12 S-337 · S-339) */
class PatchNotesTest {
    @Test fun `지금 판의 소식이 목록 맨 위에 있다 — 판을 올리면 소식도 더한다`() {
        val current = BuildConfig.VERSION_NAME
        assertNotNull("PatchNotes 에 $current 줄이 없다", PatchNotes.of(current))
        assertEquals(current, PatchNotes.releases.first().version)
    }

    @Test fun `판은 겹치지 않고 최신이 위`() {
        val versions = PatchNotes.releases.map { it.version }
        assertEquals(versions.size, versions.toSet().size)
        val parsed = versions.map { v -> v.split('.').map { it.toInt() } }
        val sorted = parsed.sortedWith(compareByDescending<List<Int>> { it[0] }.thenByDescending { it[1] }.thenByDescending { it[2] })
        assertEquals(sorted, parsed)
    }

    @Test fun `소식 창은 셋까지 · 스토어 판에는 테스트 전용 줄이 없다`() {
        PatchNotes.releases.forEach { release ->
            assertTrue(PatchNotes.headline(release, testBuild = true).size <= 3)
            assertTrue(PatchNotes.headline(release, testBuild = false).none { it.testOnly })
            assertTrue(PatchNotes.visible(release, testBuild = false).none { it.testOnly })
        }
    }

    @Test fun `띄우는 조건 — 판마다 한 번 · 새로 설치하면 안 띄움 · 소식이 없으면 안 띄움`() {
        assertTrue(PatchNotesRules.shouldShow(seen = null, current = "1.22.0", freshInstall = false, hasNotes = true))
        assertTrue(PatchNotesRules.shouldShow(seen = "1.21.0", current = "1.22.0", freshInstall = false, hasNotes = true))
        assertFalse(PatchNotesRules.shouldShow(seen = "1.22.0", current = "1.22.0", freshInstall = false, hasNotes = true))
        assertFalse(PatchNotesRules.shouldShow(seen = null, current = "1.22.0", freshInstall = true, hasNotes = true))
        assertFalse(PatchNotesRules.shouldShow(seen = "1.21.0", current = "1.22.0", freshInstall = false, hasNotes = false))
    }
}
