package com.stepup.android.sync

import com.stepup.android.data.repo.UploadRun
import kotlinx.coroutines.CancellationException

/**
 * 쌓인 러닝을 서버로 올리는 한 차례(2026-11-03 — 안드로이드 SessionUploadWorker 의 규칙을 그대로 옮겼다. iOS 는 BGTask · 앱이 앞에 올 때).
 *
 * 1. 과속 정지 신고를 러닝보다 먼저 — 못 보내면 러닝도 올리지 않고 다시 시도(서버가 금지 시간을 알아야 그 사이 러닝을 무효로 둔다, 0065).
 * 2. 한 번에 10개씩 올리고, 더 남았거나 도는 사이 새로 끝난 러닝이 있으면 이어서.
 * 3. 시도조차 못 했으면(서버 주소 없음 · 로그인 필요) 끝 — 다시 해도 같다. 실패가 있으면 다시 시도(백오프는 기기 일정표에 맡긴다).
 */
object UploadPass {
    enum class Outcome { DONE, RETRY }

    suspend fun run(
        flushReports: suspend () -> Boolean,
        uploadPending: suspend () -> UploadRun,
        isStopped: () -> Boolean = { false },
        log: (String) -> Unit = {},
    ): Outcome {
        val reported = try {
            flushReports()
        } catch (e: CancellationException) {
            throw e
        } catch (e: Throwable) {
            log("과속 정지 신고 중 예기치 못한 오류: ${e.message}")
            false
        }
        if (!reported) return Outcome.RETRY

        while (!isStopped()) {
            val run = try {
                uploadPending()
            } catch (e: CancellationException) {
                throw e
            } catch (e: Throwable) {
                // 여기서 새면 세션은 대기열에 남는다 — 데이터는 그대로, 다음 차례에 다시
                log("세션 업로드 중 예기치 못한 오류: ${e.message}")
                return Outcome.RETRY
            }
            run.blockedBy?.let {
                log("업로드를 건너뜁니다: $it")
                return Outcome.DONE
            }
            log("업로드 결과 — 서명 ${run.signed} · 거절 ${run.rejected} · 실패 ${run.failed}")
            if (run.shouldRetry) return Outcome.RETRY
            if (run.signed + run.rejected == 0) return Outcome.DONE
        }
        return Outcome.RETRY
    }
}
