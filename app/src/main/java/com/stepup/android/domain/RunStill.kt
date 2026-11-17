package com.stepup.android.domain

/**
 * 러닝 중 오래 서 있음(2026-10-17 사용자 결정 "너무 오래 멈췄을 때(30초 이상) 경고도 떠야" — 러너가 30초 넘게 서 있음).
 *
 * 위치가 잡히는데도 걸음도 늘지 않고 경로 점도 생기지 않은 지 [STILL_MS] 가 지나면 "30초 넘게 멈춰 있어요"를 알린다. 쉬는 중이면 일시정지하라고
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
     * @param sinceFixMs 위치를 마지막으로 제대로 잡은 때부터(ms), 한 번도 못 잡았으면 null.
     *   위치를 [STILL_MS] 넘게 못 잡고 있으면 "그 자리에 있다"가 아니라 "어디인지 모른다"다 — 지하철 · 지하에서 걸음 없이 가는 동안
     *   "30초 넘게 멈춰 있어요"가 떴다(2026-11-04 사용자 "지하철 타고 가는데 30초 정지가 왜 나오냐"). 그동안은 알리지 않는다.
     */
    fun isStill(recording: Boolean, sinceMoveMs: Long, sinceFixMs: Long?): Boolean =
        recording && sinceMoveMs >= STILL_MS && sinceFixMs != null && sinceFixMs < STILL_MS
}
