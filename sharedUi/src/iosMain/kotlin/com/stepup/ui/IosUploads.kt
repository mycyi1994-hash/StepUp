package com.stepup.ui

import com.stepup.android.data.repo.UploadRun
import com.stepup.android.sync.UploadPass
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import platform.Foundation.NSLog

/**
 * iOS 의 러닝 올리기 — 안드로이드 WorkManager 일꾼(SessionUploadWorker)과 같은 규칙(UploadPass)을
 * 백그라운드 작업(BGTaskScheduler, iosApp 의 UploadTask.swift)과 앱이 앞에 올 때 돈다.
 *
 * 올릴 곳(과속 신고 · 러닝 대기열)은 앱이 화면 · 저장소를 iOS 에 붙일 때 [connect] 로 넣는다 — 그 전에는 할 일이 없다(끝으로 답한다).
 */
object IosUploads {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private var flushReports: (suspend () -> Boolean)? = null
    private var uploadPending: (suspend () -> UploadRun)? = null
    private var running: Job? = null

    fun connect(flushReports: suspend () -> Boolean, uploadPending: suspend () -> UploadRun) {
        this.flushReports = flushReports
        this.uploadPending = uploadPending
    }

    /** 한 차례 돈다. 끝나면 onDone(true = 다 올렸거나 할 일 없음, false = 다시 해야 함) — 한 번에 하나만 */
    fun run(onDone: (Boolean) -> Unit) {
        val flush = flushReports
        val upload = uploadPending
        if (flush == null || upload == null) {
            onDone(true)
            return
        }
        if (running?.isActive == true) {
            onDone(false)
            return
        }
        running = scope.launch {
            val outcome = UploadPass.run(flush, upload, log = { NSLog("StepUp upload: %@", it) })
            onDone(outcome == UploadPass.Outcome.DONE)
        }
    }

    /** 백그라운드 시간이 끝났다(iOS 가 멈추라고 할 때) — 하던 차례를 거둔다. 대기열은 그대로 남는다 */
    fun cancel() {
        running?.cancel()
    }
}
