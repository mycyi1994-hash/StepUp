package com.stepup.android

import android.graphics.Bitmap
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.stepup.android.domain.GeoPoint
import com.stepup.android.ui.components.RunShareCard
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import java.io.File

/**
 * 공유 카드(E05)는 공유 미리보기에서 그려진다. 기기에서 실제로 그려 크기와 경로 포함 여부를 확인하고, 검토용 PNG 로 남긴다(form-checks/).
 * 경로 · 숫자는 이 검사용 값이다 — 앱에 보이지 않는다.
 *
 * 경로를 빼면(기본) 가로로 긴 그림이고 달린 길(시안 선) · 출발 · 도착 점이 한 점도 없어야 한다.
 *
 * 자유 러닝 그림(Style.Free)은 경로를 넣든 빼든 늘 세로(4:5)이고, 경로는 형광 초록으로 그린다.
 */
@RunWith(AndroidJUnit4::class)
class ShareCardRenderTest {
    private val context get() = InstrumentationRegistry.getInstrumentation().targetContext
    private val labels get() = RunShareCard.Labels(
        distance = context.getString(R.string.share_card_distance),
        time = context.getString(R.string.share_card_time),
        pace = context.getString(R.string.share_card_pace),
        footer = context.getString(R.string.share_card_footer),
    )
    private val track = (0..40).map { i -> GeoPoint(37.528 + i * 0.0004, 126.932 + i * 0.0007 + (i % 5) * 0.0001) }

    /** 경로 시안(#48D9FA)에 가까운 점의 수 — 로고의 파랑(#0754FF)은 세지 않는다 */
    private fun routePixels(card: Bitmap): Int {
        var n = 0
        for (y in 0 until card.height step 2) for (x in 0 until card.width step 2) {
            val c = card.getPixel(x, y)
            val r = (c shr 16) and 0xFF
            val g = (c shr 8) and 0xFF
            val b = c and 0xFF
            if (r < 130 && g > 170 && b > 200) n++
        }
        return n
    }

    private fun save(card: Bitmap, name: String) {
        val directory = File(context.getExternalFilesDir(null), "form-checks").apply { mkdirs() }
        File(directory, name).outputStream().use { card.compress(Bitmap.CompressFormat.PNG, 100, it) }
    }

    @Test fun rendersCardWithRoute() {
        val card = RunShareCard.render(context, km = 5.02, elapsed = "31:08", pace = "6'12\"", track = track, labels = labels,
            date = "2026.10.03 (토)")
        assertEquals(RunShareCard.WIDTH, card.width)
        assertEquals(RunShareCard.HEIGHT, card.height)
        assertTrue("경로가 그려져야 한다", routePixels(card) > 200)
        save(card, "share-card.png")
    }

    @Test fun routeOffLeavesNoRouteOrPlaces() {
        val card = RunShareCard.render(context, km = 5.02, elapsed = "31:08", pace = "6'12\"", track = emptyList(), labels = labels,
            date = "2026.10.03 (토)")
        assertEquals(RunShareCard.WIDTH, card.width)
        assertEquals(RunShareCard.HEIGHT_NO_ROUTE, card.height)
        assertEquals("경로를 빼면 달린 길이 한 점도 없어야 한다", 0, routePixels(card))
        save(card, "share-card-no-route.png")
    }

    /** 자유 러닝 경로 초록(#19F52E)에 가까운 점의 수 — 남색 바탕 · 흰 글 · 단위 파랑은 세지 않는다 */
    private fun greenPixels(card: Bitmap): Int {
        var n = 0
        for (y in 0 until card.height step 2) for (x in 0 until card.width step 2) {
            val c = card.getPixel(x, y)
            val r = (c shr 16) and 0xFF
            val g = (c shr 8) and 0xFF
            val b = c and 0xFF
            if (g > 200 && r < 120 && b < 120) n++
        }
        return n
    }

    /**
     * 자유 러닝 공유 그림 — 앱과 같이 아래 안내 글(footer)을 비워 넘긴다. 그림은 이 글 없이도 그려져야 한다.
     * 글자를 읽어 확인하지는 않는다(OCR 없음) — 빈 글로 그려지는지와 크기 · 경로 색만 본다.
     */
    private val freeLabels get() = RunShareCard.Labels(
        distance = context.getString(R.string.share_card_distance),
        time = context.getString(R.string.share_card_time),
        pace = context.getString(R.string.run_label_pace),
        footer = "",
    )

    @Test fun freeStyleRendersGreenRouteWithoutFooter() {
        val card = RunShareCard.render(context, km = 3.24, elapsed = "21:40", pace = "6′41″", track = track, labels = freeLabels,
            date = "2026.10.14", style = RunShareCard.Style.Free, distance = "3.24")
        assertEquals(RunShareCard.WIDTH, card.width)
        assertEquals(RunShareCard.HEIGHT, card.height)
        assertTrue("자유 러닝 경로는 형광 초록으로 그려야 한다", greenPixels(card) > 200)
        save(card, "share-card-free.png")
    }

    @Test fun freeStyleWithoutRouteKeepsPortraitAndNoGreen() {
        // 경로를 빼도 자유 러닝 그림은 세로(4:5) 그대로 — 위치가 없던 러닝처럼 거리도 "—"
        val card = RunShareCard.render(context, km = 0.0, elapsed = "21:40", pace = "—", track = emptyList(), labels = freeLabels,
            date = "2026.10.14", style = RunShareCard.Style.Free, distance = "—")
        assertEquals(RunShareCard.WIDTH, card.width)
        assertEquals(RunShareCard.HEIGHT, card.height)
        assertEquals("경로를 빼면 초록 경로가 한 점도 없어야 한다", 0, greenPixels(card))
        save(card, "share-card-free-no-route.png")
    }
}
