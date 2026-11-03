package com.stepup.android.sync

import com.stepup.android.data.repo.UploadRun
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals

/** 올리기 한 차례의 규칙 — 안드로이드 일꾼 · iOS BGTask 가 같은 것을 돈다 */
class UploadPassTest {
    @Test fun reportsFirstOtherwiseRetryWithoutUploading() = runTest {
        var uploads = 0
        val out = UploadPass.run(flushReports = { false }, uploadPending = { uploads++; UploadRun() })
        assertEquals(UploadPass.Outcome.RETRY, out)
        assertEquals(0, uploads)
        assertEquals(UploadPass.Outcome.RETRY, UploadPass.run(flushReports = { error("x") }, uploadPending = { UploadRun() }))
    }

    @Test fun keepsGoingUntilQueueIsEmpty() = runTest {
        val batches = ArrayDeque(listOf(UploadRun(signed = 10), UploadRun(signed = 3, rejected = 1), UploadRun()))
        var calls = 0
        val out = UploadPass.run(flushReports = { true }, uploadPending = { calls++; batches.removeFirst() })
        assertEquals(UploadPass.Outcome.DONE, out)
        assertEquals(3, calls)
    }

    @Test fun blockedIsDoneFailureIsRetry() = runTest {
        assertEquals(UploadPass.Outcome.DONE, UploadPass.run({ true }, { UploadRun(blockedBy = "로그인이 필요합니다") }))
        assertEquals(UploadPass.Outcome.RETRY, UploadPass.run({ true }, { UploadRun(signed = 2, failed = 1, shouldRetry = true) }))
        assertEquals(UploadPass.Outcome.RETRY, UploadPass.run({ true }, { error("db") }))
        assertEquals(UploadPass.Outcome.RETRY, UploadPass.run({ true }, { UploadRun(signed = 1) }, isStopped = { true }))
    }
}
