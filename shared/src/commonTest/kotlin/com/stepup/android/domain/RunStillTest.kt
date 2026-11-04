package com.stepup.android.domain

import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/** 러닝 중 오래 서 있음(2026-10-17 "너무 오래 멈췄을 때(30초 이상) 경고도 떠야") — 걸음 · 경로 점이 30초 넘게 그대로면 알린다 */
class RunStillTest {

    @Test fun `30초가 되면 멈춰 있다고 본다`() {
        assertFalse(RunStill.isStill(recording = true, sinceMoveMs = 0, sinceFixMs = 1_000))
        assertFalse(RunStill.isStill(recording = true, sinceMoveMs = 29_999, sinceFixMs = 1_000))
        assertTrue(RunStill.isStill(recording = true, sinceMoveMs = 30_000, sinceFixMs = 1_000))
        assertTrue(RunStill.isStill(recording = true, sinceMoveMs = 5 * 60_000, sinceFixMs = 1_000))
    }

    @Test fun `일시정지 · 저장 중에는 서 있어도 알리지 않는다`() {
        assertFalse(RunStill.isStill(recording = false, sinceMoveMs = 30_000, sinceFixMs = 1_000))
        assertFalse(RunStill.isStill(recording = false, sinceMoveMs = 5 * 60_000, sinceFixMs = 1_000))
    }

    @Test fun `위치를 못 잡는 동안 — 지하철 · 지하 — 알리지 않는다`() {
        // 2026-11-04 사용자 "지하철 타고 가는데 30초 정지가 왜 나오냐" — 걸음이 없어도 어디인지 모르면 서 있다고 하지 않는다
        assertFalse(RunStill.isStill(recording = true, sinceMoveMs = 5 * 60_000, sinceFixMs = 30_000))
        assertFalse(RunStill.isStill(recording = true, sinceMoveMs = 5 * 60_000, sinceFixMs = 10 * 60_000))
        assertFalse(RunStill.isStill(recording = true, sinceMoveMs = 5 * 60_000, sinceFixMs = null))
        // 위치가 잡히는 채로 서 있으면 그대로 알린다
        assertTrue(RunStill.isStill(recording = true, sinceMoveMs = 5 * 60_000, sinceFixMs = 29_999))
    }
}
