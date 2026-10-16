package com.stepup.android

import com.stepup.android.ui.screens.walk.FreeRunStartMotion
import com.stepup.android.ui.screens.walk.FreeRunStartMotion.Layer
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * 자유러닝 시작 시퀀스의 시간표(2026-10-17 전달본 motion-spec.json) — 3·2·1·0 → StepUP! → 지도 펼침.
 * 화면은 이 값을 그리기만 한다. 끊김(둘 다 안 보이는 프레임) · 멈춤(숫자마다 다시 도는 원호)이 없는지 수로 본다.
 */
class FreeRunStartMotionTest {

    private val m = FreeRunStartMotion

    @Test fun `numbers run 3 2 1 0 a second apart and 0 holds 180ms before StepUP`() {
        assertEquals(3, m.digitAt(0))
        assertEquals(3, m.digitAt(999))
        assertEquals(2, m.digitAt(1_000))
        assertEquals(1, m.digitAt(2_000))
        assertEquals(0, m.digitAt(3_000))
        assertEquals(0, m.digitAt(3_179))
        assertEquals(3_180L, m.LAUNCH_AT)
        assertEquals(3_000L, m.digitOnset(3_100))
        // 정상 타임라인 — 펼침(기록 시작) 3,760 · 러닝 화면 전체 4,410
        assertEquals(3_760L, m.REVEAL_AT)
        assertEquals(4_410L, m.REVEAL_AT + m.REVEAL_MS)
    }

    @Test fun `the arc turns once a second without restarting at each number`() {
        assertEquals(0f, m.arcDegrees(0), 0.001f)
        assertEquals(360f, m.arcDegrees(1_000), 0.001f)
        assertEquals(720f, m.arcDegrees(2_000), 0.001f)
        // 숫자가 바뀌는 순간에도 이어진다 — 앞뒤 1ms 의 차이가 1초에 한 바퀴의 1ms 몫뿐
        assertEquals(0.36f, m.arcDegrees(1_000) - m.arcDegrees(999), 0.001f)
        // 0 과 로고 교차 동안에도 돈다(3,180~3,480)
        assertTrue(m.arcDegrees(3_400) > m.arcDegrees(3_200))
        assertEquals(1_260f, m.arcDegrees(3_500), 0.001f)
    }

    @Test fun `numbers enter in 160ms and stay still with reduced motion`() {
        val start = m.numberEnter(0, reduced = false)
        assertEquals(0.6f, start.alpha, 0.001f)
        assertEquals(6f, start.dy, 0.001f)
        assertEquals(0.96f, start.scale, 0.001f)
        assertEquals(FreeRunStartMotion.Look.Shown, m.numberEnter(160, reduced = false))
        assertEquals(FreeRunStartMotion.Look.Shown, m.numberEnter(0, reduced = true))
    }

    @Test fun `count exit and StepUP enter share one start so no frame shows neither`() {
        for (t in 0L..m.LAUNCH_MS) {
            val count = m.countExit(t, reduced = false).alpha
            val logo = m.wordmarkEnter(t, reduced = false).alpha
            assertTrue("t=$t count=$count logo=$logo — 둘 다 보이지 않는 프레임", count + logo >= 0.99f)
        }
        for (t in 0L..m.REDUCED_FADE_MS) {
            val count = m.countExit(t, reduced = true).alpha
            val logo = m.wordmarkEnter(t, reduced = true).alpha
            assertEquals("모션 감소의 교차 페이드는 같은 시작점에서 200ms", 1f, count + logo, 0.001f)
        }
        // 들어오기 전에는 숫자만, 끝나면 로고만
        assertEquals(1f, m.countExit(-1, reduced = false).alpha, 0f)
        assertEquals(0f, m.wordmarkEnter(-1, reduced = false).alpha, 0f)
        assertEquals(0f, m.countExit(300, reduced = false).alpha, 0.001f)
        val exit = m.countExit(300, reduced = false)
        assertEquals(-10f, exit.dy, 0.001f)
        assertEquals(1.04f, exit.scale, 0.001f)
    }

    @Test fun `StepUP overshoots to the right then settles`() {
        val first = m.wordmarkEnter(0, reduced = false)
        assertEquals(-18f, first.dx, 0.001f)
        assertEquals(0.96f, first.scale, 0.001f)
        // 곡선을 지난 진행 72% 지점 — 오른쪽 3 · 1.025
        val peak = (0..300).map { m.wordmarkEnter(it.toLong(), reduced = false) }.maxBy { it.dx }
        assertEquals(3f, peak.dx, 0.3f)
        assertEquals(1.025f, peak.scale, 0.002f)
        assertEquals(FreeRunStartMotion.Look.Shown, m.wordmarkEnter(300, reduced = false))
        // 모션 감소 — 움직이지 않는다
        val reduced = m.wordmarkEnter(100, reduced = true)
        assertEquals(0f, reduced.dx, 0f)
        assertEquals(1f, reduced.scale, 0f)
    }

    @Test fun `StepUP stays 120ms into the reveal then fades over 300ms`() {
        assertEquals(1f, m.wordmarkExit(0, reduced = false).alpha, 0f)
        assertEquals(1f, m.wordmarkExit(120, reduced = false).alpha, 0f)
        assertTrue(m.wordmarkExit(200, reduced = false).alpha < 1f)
        val gone = m.wordmarkExit(420, reduced = false)
        assertEquals(0f, gone.alpha, 0.001f)
        assertEquals(1.045f, gone.scale, 0.001f)
        // 모션 감소 — 바로 200ms 페이드
        assertEquals(0.5f, m.wordmarkExit(100, reduced = true).alpha, 0.001f)
    }

    @Test fun `reveal layers grow from one start, lead first, screen last, all full by 650ms`() {
        for (t in 0L..m.REVEAL_MS) {
            val lead = m.revealFraction(Layer.LEAD, t)
            val accent = m.revealFraction(Layer.ACCENT, t)
            val screen = m.revealFraction(Layer.SCREEN, t)
            assertTrue("t=$t", lead >= accent && accent >= screen)
        }
        assertEquals(0f, m.revealFraction(Layer.SCREEN, 70), 0f)
        assertEquals(1f, m.revealFraction(Layer.LEAD, 500), 0.001f)
        assertEquals(1f, m.revealFraction(Layer.ACCENT, 575), 0.001f)
        assertEquals(1f, m.revealFraction(Layer.SCREEN, 650), 0.001f)
        assertEquals(650L, m.revealDuration(reduced = false))
        assertEquals(200L, m.revealDuration(reduced = true))
    }

    @Test fun `the mask covers every corner of the screen`() {
        // 360 × 748 시안 — ceil(hypot(180, 374)) + 12 = 416 + 12
        assertEquals(428f, m.maxRadius(360f, 748f, 12f), 0.001f)
        val r = m.maxRadius(1080f, 2400f, 36f)
        assertTrue(r * r >= 540f * 540f + 1200f * 1200f)
    }
}
