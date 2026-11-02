package com.stepup.android.domain

/**
 * 러닝 중 오래 서 있음(2026-10-17 사용자 결정 "너무 오래 멈췄을 때(30초 이상) 경고도 떠야" — 러너가 30초 넘게 서 있음).
 *
 * 걸음도 늘지 않고 경로 점도 생기지 않은 지 [STILL_MS] 가 지나면 "30초 넘게 멈춰 있어요"를 알린다. 쉬는 중이면 일시정지하라고
 * 권할 뿐 저절로 멈추지 않는다 — 기록 · 보상 · 판정 규칙은 그대로다. 다시 걷거나 달리면(걸음이 늘거나 경로 점이 생기면) 바로 사라진다.
 * 걸음과 위치 둘 다 보는 것은 하나만 보면 틀리기 때문이다 — 러닝머신 · 실내는 위치가 그대로여도 걸음이 늘고, 신호가 약하면
 * 위치가 안 와도 걸음은 는다.
 */
object RunStill {
    /** 이만큼(ms) 걸음 · 경로가 그대로면 멈춰 있다고 본다 */
    const val STILL_MS = 30_000L

    /**
     * @param recording 기록 중인가(시작했고 일시정지 · 저장 중이 아님) — 일시정지 중에는 알리지 않는다
     * @param sinceMoveMs 마지막으로 움직인 때(걸음이 늘거나 경로 점이 생긴 때, 시작 · 다시 시작한 때)부터(ms)
     */
    fun isStill(recording: Boolean, sinceMoveMs: Long): Boolean = recording && sinceMoveMs >= STILL_MS
}
