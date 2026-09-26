package com.stepup.android

import android.graphics.Bitmap
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.stepup.android.domain.GeoPoint
import com.stepup.android.ui.components.RunShareCard
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Test
import org.junit.runner.RunWith
import java.io.File

/**
 * 공유 카드는 공유 버튼을 눌러야 만들어져 화면 캡처에 나오지 않는다.
 * 기기에서 실제로 그려 크기를 확인하고, 검토용 PNG 로 남긴다(form-checks/).
 * 경로 · 숫자는 이 검사용 값이다 — 앱에 보이지 않는다.
 */
@RunWith(AndroidJUnit4::class)
class ShareCardRenderTest {
    @Test fun rendersS2CardWithRouteAndStats() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val track = (0..40).map { i -> GeoPoint(37.528 + i * 0.0004, 126.932 + i * 0.0007 + (i % 5) * 0.0001) }
        val card = RunShareCard.render(
            context = context, km = 5.02, elapsed = "31:08", pace = "6'12\"", track = track,
            labels = RunShareCard.Labels(
                distance = context.getString(R.string.share_card_distance),
                time = context.getString(R.string.share_card_time),
                pace = context.getString(R.string.share_card_pace),
                footer = context.getString(R.string.share_card_footer),
            ),
        )
        assertEquals(RunShareCard.WIDTH, card.width)
        assertEquals(RunShareCard.HEIGHT, card.height)
        // 바탕은 S2 푸른 검정, 가운데 위쪽에는 풍경이나 경로가 그려져 바탕과 다르다
        assertEquals(0xFF05080E.toInt(), card.getPixel(RunShareCard.WIDTH / 2, RunShareCard.HEIGHT - 4))
        assertNotEquals(0xFF05080E.toInt(), card.getPixel(RunShareCard.WIDTH / 2, 420))
        val directory = File(context.getExternalFilesDir(null), "form-checks").apply { mkdirs() }
        File(directory, "share-card.png").outputStream().use { card.compress(Bitmap.CompressFormat.PNG, 100, it) }
    }
}
