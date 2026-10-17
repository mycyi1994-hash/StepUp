package com.stepup.android

import com.stepup.android.domain.RunStill
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/** 러닝 중 오래 서 있음(2026-10-17 "너무 오래 멈췄을 때(30초 이상) 경고도 떠야") — 걸음 · 경로 점이 30초 넘게 그대로면 알린다 */
class RunStillTest {

    @Test fun `30초가 되면 멈춰 있다고 본다`() {
        assertFalse(RunStill.isStill(recording = true, sinceMoveMs = 0))
        assertFalse(RunStill.isStill(recording = true, sinceMoveMs = 29_999))
        assertTrue(RunStill.isStill(recording = true, sinceMoveMs = 30_000))
        assertTrue(RunStill.isStill(recording = true, sinceMoveMs = 5 * 60_000))
    }

    @Test fun `일시정지 · 저장 중에는 서 있어도 알리지 않는다`() {
        assertFalse(RunStill.isStill(recording = false, sinceMoveMs = 30_000))
        assertFalse(RunStill.isStill(recording = false, sinceMoveMs = 5 * 60_000))
    }
}
