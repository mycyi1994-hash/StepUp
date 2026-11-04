package com.stepup.android.ui.components

import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/** 여러 색 세로 그라디언트를 두 색 띠로 — 띠가 빈틈 · 겹침 없이 높이를 덮고, 구간마다 그 두 색을 잇는다 */
class VerticalStopsTest {
    private val a = Color.Black.copy(alpha = .28f)
    private val b = Color.Transparent
    private val c = Color.Black.copy(alpha = .05f)
    private val d = Color.Black.copy(alpha = .78f)

    @Test fun 사진_카드_네_색은_세_띠로_정수_픽셀에서_이어진다() {
        val bands = verticalBands(Size(1080f, 867f), listOf(0f to a, .3f to b, .5f to c, 1f to d))
        assertEquals(3, bands.size)
        assertEquals(listOf(0f, 260f, 434f), bands.map { it.top })
        assertEquals(listOf(260f, 434f, 867f), bands.map { it.bottom })
        bands.forEach { assertNotNull(it.brush); assertEquals(it.top, it.top.toInt().toFloat()) }
        assertEquals(listOf(a, b, c), bands.map { it.color })
    }

    @Test fun 띠는_빈틈_없이_높이를_덮는다() {
        for (h in listOf(1f, 7f, 330f, 1001f, 2400f)) {
            val bands = verticalBands(Size(10f, h), listOf(.1f to a, .55f to b, .9f to c))
            assertEquals(0f, bands.first().top)
            assertEquals(h, bands.last().bottom)
            bands.zipWithNext().forEach { (x, y) -> assertEquals(x.bottom, y.top) }
            assertTrue(bands.all { it.bottom > it.top })
        }
    }

    @Test fun 첫_점_위와_끝_점_아래는_그_색으로_채운다() {
        val bands = verticalBands(Size(10f, 100f), listOf(.2f to a, .8f to c))
        assertEquals(3, bands.size)
        assertNull(bands.first().brush); assertEquals(a, bands.first().color)
        assertNull(bands.last().brush); assertEquals(c, bands.last().color)
    }

    @Test fun 높이_없음은_그리지_않는다() {
        assertTrue(verticalBands(Size(10f, 0f), listOf(0f to a, 1f to d)).isEmpty())
    }
}
