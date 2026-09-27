package com.stepup.android

import com.stepup.android.data.repo.RecordedBalances
import org.junit.Assert.assertEquals
import org.junit.Test

class RecordedBalancesTest {
    @Test fun serverBalanceWinsUntilALaterSyncSucceeds() {
        val recorded = RecordedBalances.Entry(balance = 112.5, recordedAt = 2_000)
        // 러닝을 확인한 뒤 원장 동기화가 아직 없다(마지막 성공은 그 전에 시작) — 서버가 돌려준 잔고
        assertEquals(112.5, RecordedBalances.resultBalance(100.0, recorded, syncedFrom = 1_000)!!, 0.0)
        // 확인 뒤에 시작한 동기화가 성공했다 — 원장(그 뒤 쓴 것까지 반영)
        assertEquals(90.0, RecordedBalances.resultBalance(90.0, recorded, syncedFrom = 3_000)!!, 0.0)
        // 이번 러닝의 서버 응답이 없으면 원장 그대로, 원장도 못 읽었으면 모른다
        assertEquals(100.0, RecordedBalances.resultBalance(100.0, null, syncedFrom = 0)!!, 0.0)
        assertEquals(null, RecordedBalances.resultBalance(null, null, syncedFrom = 0))
    }
}
