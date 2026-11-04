package com.stepup.android.data.repo

import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

/**
 * 한 번에 하나씩 도는 일을 함께 기다리게 한다 — 동기화를 여러 곳에서 잇달아 불러도 줄이 길어지지 않게(EconomySync).
 *
 * 부른 사람은 **부른 뒤에 시작하는** 실행의 결과를 받는다. 앞 실행이 도는 동안 부른 사람들은 다음 실행 하나를 함께 기다린다 —
 * 이미 시작한 실행은 부르기 전 값(방금 올린 러닝 등)을 못 봤을 수 있어 그것을 나눠 받지 않는다.
 * 그 다음 실행을 줄 세운 사람이 취소되면 함께 기다리던 사람은 다시 줄 선다.
 */
class CoalescedRun<T>(private val lock: Mutex = Mutex()) {
    private val guard = Any()
    private var queued: CompletableDeferred<T>? = null
    private var waiting = 0

    private val _running = MutableStateFlow(false)

    /** 실행 중이거나 실행을 기다리는 사람이 있다 */
    val running: StateFlow<Boolean> = _running

    suspend fun run(block: suspend () -> T): T {
        var owner = false
        val shared = synchronized(guard) {
            queued ?: CompletableDeferred<T>().also {
                queued = it
                owner = true
            }
        }
        if (!owner) {
            return try {
                shared.await()
            } catch (failed: Throwable) {
                currentCoroutineContext().ensureActive()
                if (failed !is OwnerGone) throw failed
                run(block)
            }
        }
        mark(+1)
        try {
            val result = lock.withLock {
                // 시작했다 — 이제부터 부르는 사람은 다음 실행으로
                synchronized(guard) { if (queued === shared) queued = null }
                block()
            }
            shared.complete(result)
            return result
        } catch (error: Throwable) {
            synchronized(guard) { if (queued === shared) queued = null }
            shared.completeExceptionally(if (error is CancellationException) OwnerGone() else error)
            throw error
        } finally {
            mark(-1)
        }
    }

    private fun mark(delta: Int) = synchronized(guard) {
        waiting += delta
        _running.value = waiting > 0
    }

    /** 다음 실행을 줄 세운 쪽이 취소됐다 — 함께 기다리던 사람은 다시 줄 선다 */
    private class OwnerGone : Exception()
}
