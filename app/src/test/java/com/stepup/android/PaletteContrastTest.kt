package com.stepup.android

import androidx.compose.ui.graphics.Color
import com.stepup.android.ui.theme.DarkPalette
import com.stepup.android.ui.theme.LightPalette
import com.stepup.android.ui.theme.StepUpPalette
import kotlin.math.max
import kotlin.math.min
import kotlin.math.pow
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * 두 팔레트가 읽히는지 자로 잰다.
 *
 * ── 왜 이 검사가 있나 ──
 *
 * 가이드 투어의 딤막이 한동안 `Snow`(글자색)로 칠해져 있었다. 그 색은
 * 테마를 따라 뒤집히기 때문에, 어두운 테마에서는 화면이 **흰 막**으로
 * 덮였고 그 위의 밝은 버튼들이 통째로 사라졌다. 눈으로 밝은 테마만 보고
 * 넘어가면 다시 이렇게 된다. 그래서 색을 고를 때마다 이 검사가 두 테마를
 * 같이 재도록 두었다.
 *
 * 기준은 WCAG 대비비다. 작은 글자는 4.5:1, 큰 글자와 테두리·아이콘 같은
 * 면은 3:1 을 아래로 두지 않는다.
 */
class PaletteContrastTest {

    // ── 자 ──────────────────────────────────────────────────────

    private fun channel(c: Float): Double {
        val v = c.toDouble()
        return if (v <= 0.03928) v / 12.92 else ((v + 0.055) / 1.055).pow(2.4)
    }

    private fun luminance(color: Color): Double =
        0.2126 * channel(color.red) + 0.7152 * channel(color.green) + 0.0722 * channel(color.blue)

    private fun contrast(fg: Color, bg: Color): Double {
        val a = luminance(fg)
        val b = luminance(bg)
        return (max(a, b) + 0.05) / (min(a, b) + 0.05)
    }

    /** [fg] 를 [alpha] 만큼 [bg] 위에 칠했을 때의 색 */
    private fun over(fg: Color, bg: Color, alpha: Float): Color = Color(
        red = fg.red * alpha + bg.red * (1 - alpha),
        green = fg.green * alpha + bg.green * (1 - alpha),
        blue = fg.blue * alpha + bg.blue * (1 - alpha),
    )

    private fun StepUpPalette.assertAtLeast(
        floor: Double,
        what: String,
        fg: Color,
        bg: Color,
    ) {
        val r = contrast(fg, bg)
        assertTrue(
            "${if (dark) "다크" else "라이트"} · $what 대비 ${"%.2f".format(r)} " +
                "— ${"%.1f".format(floor)} 아래다",
            r >= floor,
        )
    }

    /**
     * 막을 가장 밝은 뒤(앱 바닥) 위에 칠했을 때. 실제 화면에는 이보다
     * 어두운 것들이 깔리므로, 이 값이 가장 나쁜 경우다.
     */
    private fun StepUpPalette.scrimOverApp(): Color = over(scrim, night, scrimAlpha)

    // ── 막과 그 위 ──────────────────────────────────────────────

    @Test
    fun `막은 두 테마 모두 어둡다`() {
        // 막이 밝으면 그 위의 흰 글자가 사라진다. 이것이 예전 버그였다.
        for (p in listOf(LightPalette, DarkPalette)) {
            val scrim = p.scrimOverApp()
            assertTrue(
                "${if (p.dark) "다크" else "라이트"} 막이 밝다 — 휘도 ${luminance(scrim)}",
                luminance(scrim) < 0.20,
            )
        }
    }

    @Test
    fun `막 위의 흰 글자가 읽힌다`() {
        // 가이드의 건너뛰기·이전 버튼이 이 자리에 선다.
        for (p in listOf(LightPalette, DarkPalette)) {
            p.assertAtLeast(4.5, "막 위 흰 글자", p.onVolt, p.scrimOverApp())
        }
    }

    @Test
    fun `막 위에서 패널 둘레가 보인다`() {
        // 바닥이든 테두리든, 둘 중 하나는 막과 갈라져야 패널이 패널로 보인다.
        for (p in listOf(LightPalette, DarkPalette)) {
            val scrim = p.scrimOverApp()
            val border = over(p.voltSoft, scrim, 0.55f)
            val best = max(contrast(p.overlay, scrim), contrast(border, scrim))
            assertTrue(
                "${if (p.dark) "다크" else "라이트"} 패널이 막에 묻힌다 — ${"%.2f".format(best)}",
                best >= 3.0,
            )
        }
    }

    @Test
    fun `스포트라이트 테두리가 막 위에서 보인다`() {
        for (p in listOf(LightPalette, DarkPalette)) {
            p.assertAtLeast(3.0, "스포트라이트 테두리", p.voltSoft, p.scrimOverApp())
        }
    }

    @Test
    fun `패널 안의 글자가 읽힌다`() {
        for (p in listOf(LightPalette, DarkPalette)) {
            p.assertAtLeast(4.5, "패널 제목", p.snow, p.overlay)
            p.assertAtLeast(4.5, "패널 본문", p.silver, p.overlay)
            p.assertAtLeast(4.5, "패널 위 강조 글자", p.voltText, p.overlay)
        }
    }

    // ── 화면 전반 ───────────────────────────────────────────────

    @Test
    fun `카드 위의 글자가 읽힌다`() {
        for (p in listOf(LightPalette, DarkPalette)) {
            for (surface in listOf(p.night, p.carbon, p.carbonHigh)) {
                p.assertAtLeast(4.5, "제목", p.snow, surface)
                p.assertAtLeast(4.5, "본문", p.silver, surface)
                // 힌트·비활성은 작은 글자 기준을 면제받는 자리지만,
                // 그래도 면 기준 3:1 아래로는 두지 않는다.
                p.assertAtLeast(3.0, "힌트", p.slate, surface)
            }
        }
    }

    @Test
    fun `파란 버튼 위의 글자가 읽힌다`() {
        for (p in listOf(LightPalette, DarkPalette)) {
            p.assertAtLeast(4.0, "버튼 글자", p.onVolt, p.volt)
        }
    }

    @Test
    fun `강조 글자색은 표면 위에서 쓰는 것이다`() {
        // volt 는 버튼 **바닥**, voltText 는 표면 **위 글자**. 둘이 같은
        // 색이면 어두운 테마에서 한쪽이 반드시 묻힌다.
        for (p in listOf(LightPalette, DarkPalette)) {
            p.assertAtLeast(4.5, "강조 글자", p.voltText, p.carbon)
        }
    }
}
