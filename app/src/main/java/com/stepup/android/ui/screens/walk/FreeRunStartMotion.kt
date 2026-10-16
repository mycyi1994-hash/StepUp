package com.stepup.android.ui.screens.walk

import androidx.compose.animation.core.CubicBezierEasing
import kotlin.math.ceil
import kotlin.math.hypot

/*
 * 자유러닝 시작 시퀀스의 시간표(2026-10-17 전달본 — docs/redesign/free-run-start-2026-10-17, motion-spec.json).
 *
 * 3 → 2 → 1 → 0 → StepUP! → 가운데에서 지도가 펼쳐짐. t=0 은 신발 점검 · 권한 준비가 끝나고 숫자 3 이 보인 순간이다.
 * 화면은 이 값을 그리기만 한다 — 시각(ms)을 넣으면 숫자 · 원호 · 로고 · 펼침의 모습이 나온다(FreeRunStartMotionTest).
 */
internal object FreeRunStartMotion {
    /** 숫자 3 · 2 · 1 · 0 이 나오는 시각 */
    const val STEP_MS = 1_000L
    const val ZERO_AT = 3_000L

    /** 0 을 보인 뒤 180ms — 숫자 · 원호 퇴장과 StepUP! 진입을 같은 시작점에서 */
    const val LAUNCH_AT = 3_180L
    const val LAUNCH_MS = 300L

    /** 로고가 자리 잡고 280ms 읽을 틈 — 그다음 지도 펼침(러닝 기록 시작) */
    const val LAUNCH_TOTAL_MS = 580L
    const val REVEAL_AT = LAUNCH_AT + LAUNCH_TOTAL_MS

    /** 숫자마다 다시 돌지 않는 한 번의 회전 — 3.5초에 1260도(1초에 한 바퀴) */
    const val ARC_MS = 3_500L
    const val ARC_DEGREES = 1_260f

    const val NUMBER_ENTER_MS = 160L

    /** 지도 펼침 — 선행 색면 · 강조 색면 · 러닝 화면(지연 · 길이), 로고는 120ms 남았다가 300ms 동안 빠진다 */
    const val LEAD_DELAY = 0L
    const val LEAD_MS = 500L
    const val ACCENT_DELAY = 35L
    const val ACCENT_MS = 540L
    const val SCREEN_DELAY = 70L
    const val SCREEN_MS = 580L
    const val WORDMARK_EXIT_DELAY = 120L
    const val WORDMARK_EXIT_MS = 300L
    const val REVEAL_MS = 650L

    /** 모션 감소 — 회전 · 이동 · 확대 없이 같은 시작점에서 200ms 페이드 */
    const val REDUCED_FADE_MS = 200L

    /** 기록 시작을 요청하고 이만큼 지나도 러닝이 시작되지 않으면 연출을 거둔다(실패한 러닝을 시작한 것처럼 보이지 않게) */
    const val START_TIMEOUT_MS = 5_000L

    /** 한 프레임에 나아가는 시간의 상한 — 멈췄다 돌아온 첫 프레임에 시퀀스를 건너뛰지 않게 */
    const val MAX_FRAME_MS = 100L

    /** 숫자 · 로고가 들어오고 나가는 곡선 cubic-bezier(.23,1,.32,1) */
    val ExitEnter = CubicBezierEasing(0.23f, 1f, 0.32f, 1f)

    /** 지도 펼침 곡선 cubic-bezier(.77,0,.175,1) */
    val Reveal = CubicBezierEasing(0.77f, 0f, 0.175f, 1f)

    /** 화면 하나의 모습 — 투명도 · 이동(dp) · 배율 */
    data class Look(val alpha: Float = 1f, val dx: Float = 0f, val dy: Float = 0f, val scale: Float = 1f) {
        companion object {
            val Shown = Look()
            val Hidden = Look(alpha = 0f)
        }
    }

    /** t(ms)에 보일 숫자 — 3 · 2 · 1 · 0 */
    fun digitAt(t: Long): Int = when {
        t < STEP_MS -> 3
        t < 2 * STEP_MS -> 2
        t < ZERO_AT -> 1
        else -> 0
    }

    /** 지금 숫자가 나온 시각 */
    fun digitOnset(t: Long): Long = when {
        t < STEP_MS -> 0L
        t < 2 * STEP_MS -> STEP_MS
        t < ZERO_AT -> 2 * STEP_MS
        else -> ZERO_AT
    }

    /** 원호 회전(도) — 숫자가 바뀌어도 이어 돈다 */
    fun arcDegrees(t: Long): Float = ARC_DEGREES * fraction(t, ARC_MS)

    /** 숫자 진입 160ms — 투명도 .6→1 · 아래 6→0 · 배율 .96→1. 모션 감소면 그대로 */
    fun numberEnter(sinceOnset: Long, reduced: Boolean): Look {
        if (reduced) return Look.Shown
        val p = ExitEnter.transform(fraction(sinceOnset, NUMBER_ENTER_MS))
        return Look(alpha = lerp(0.6f, 1f, p), dy = lerp(6f, 0f, p), scale = lerp(0.96f, 1f, p))
    }

    /** 숫자 · 원호 묶음의 퇴장 — [sinceLaunch] 는 LAUNCH_AT 부터(음수면 아직 그대로) */
    fun countExit(sinceLaunch: Long, reduced: Boolean): Look {
        if (sinceLaunch < 0) return Look.Shown
        if (reduced) return Look(alpha = 1f - fraction(sinceLaunch, REDUCED_FADE_MS))
        val p = ExitEnter.transform(fraction(sinceLaunch, LAUNCH_MS))
        return Look(alpha = 1f - p, dy = lerp(0f, -10f, p), scale = lerp(1f, 1.04f, p))
    }

    /**
     * StepUP! 진입 — 숫자 퇴장과 같은 시작점. 0: 투명 · 왼쪽 18 · .96 → 72%: 보임 · 오른쪽 3 · 1.025 → 끝: 제자리.
     * 곡선은 전체 진행에 한 번 걸고(키프레임 사이는 곧은 보간), 모션 감소면 200ms 투명도만.
     */
    fun wordmarkEnter(sinceLaunch: Long, reduced: Boolean): Look {
        if (sinceLaunch < 0) return Look.Hidden
        if (reduced) return Look(alpha = fraction(sinceLaunch, REDUCED_FADE_MS))
        if (sinceLaunch >= LAUNCH_MS) return Look.Shown
        val p = ExitEnter.transform(fraction(sinceLaunch, LAUNCH_MS))
        return if (p <= 0.72f) {
            val k = p / 0.72f
            Look(alpha = lerp(0f, 1f, k), dx = lerp(-18f, 3f, k), scale = lerp(0.96f, 1.025f, k))
        } else {
            val k = (p - 0.72f) / 0.28f
            Look(alpha = 1f, dx = lerp(3f, 0f, k), scale = lerp(1.025f, 1f, k))
        }
    }

    /** 지도 펼침 동안 로고 — 120ms 남았다가 300ms 동안 빠진다(투명 · 1.045 배). 모션 감소면 200ms 페이드 */
    fun wordmarkExit(sinceReveal: Long, reduced: Boolean): Look {
        if (sinceReveal < 0) return Look.Shown
        if (reduced) return Look(alpha = 1f - fraction(sinceReveal, REDUCED_FADE_MS))
        val p = ExitEnter.transform(fraction(sinceReveal - WORDMARK_EXIT_DELAY, WORDMARK_EXIT_MS))
        return Look(alpha = 1f - p, scale = lerp(1f, 1.045f, p))
    }

    /** 펼침 층 셋 — 아래에서 위로 선행 파랑 · 강조 파랑 · 러닝 화면 */
    enum class Layer(val delayMs: Long, val durationMs: Long) {
        LEAD(LEAD_DELAY, LEAD_MS),
        ACCENT(ACCENT_DELAY, ACCENT_MS),
        SCREEN(SCREEN_DELAY, SCREEN_MS),
    }

    /** 펼침 층의 반경 비율(0..1) — 같은 시작점에서 지연 · 길이만 다르다 */
    fun revealFraction(layer: Layer, sinceReveal: Long): Float =
        Reveal.transform(fraction(sinceReveal - layer.delayMs, layer.durationMs))

    /** 펼침이 끝나는 길이 — 모션 감소면 200ms 교차 페이드 */
    fun revealDuration(reduced: Boolean): Long = if (reduced) REDUCED_FADE_MS else REVEAL_MS

    /** 마스크 최대 반경 — 가운데에서 화면 네 모서리(둥근 모서리 포함)까지 다 덮는다: ceil(hypot(w/2, h/2)) + 12 */
    fun maxRadius(width: Float, height: Float, extra: Float): Float =
        ceil(hypot(width / 2.0, height / 2.0)).toFloat() + extra

    private fun fraction(elapsed: Long, duration: Long): Float =
        if (duration <= 0L) 1f else (elapsed.toFloat() / duration).coerceIn(0f, 1f)

    /** 끝점은 정확히 — 진행이 다 차면 목표값 그대로(부동소수 오차로 1.0000001 배가 되지 않게) */
    private fun lerp(from: Float, to: Float, p: Float): Float = when {
        p <= 0f -> from
        p >= 1f -> to
        else -> from + (to - from) * p
    }
}
