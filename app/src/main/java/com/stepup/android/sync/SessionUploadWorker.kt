package com.stepup.android.sync

import android.content.Context
import android.util.Log
import androidx.work.Constraints
import androidx.work.CoroutineWorker
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import com.stepup.android.core.ServiceLocator

/**
 * 쌓인 러닝 세션을 증명 서버로 밀어 올리는 일꾼.
 *
 * 러닝이 끝나는 곳은 대체로 밖이고, 신호가 좋은 곳이라는 보장이 없다.
 * 세션 종료 시점에 직접 보내지 않고 이 일꾼에게 맡기는 이유다 — WorkManager
 * 는 네트워크가 돌아올 때까지 기다렸다가, 앱이 꺼져 있어도, 기기를 다시
 * 켜도 하던 일을 이어서 한다.
 */
class SessionUploadWorker(
    context: Context,
    params: WorkerParameters,
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        ServiceLocator.init(applicationContext)

        // 한 번에 10개씩 올린다. 더 남았거나 도는 사이 새로 끝난 러닝이 있으면
        // 이어서 올린다 — 여기서 그만두면 남은 것은 다음 러닝을 끝낼 때까지 잠든다.
        while (!isStopped) {
            val run = runCatching { ServiceLocator.claimRepository.uploadPending() }
                .getOrElse { error ->
                    // 여기서 예외가 새면 일꾼이 실패로 끝나고 세션은 대기열에 남는다.
                    // 데이터가 사라지지는 않지만, 왜 안 올라가는지 모르게 된다.
                    Log.w(TAG, "세션 업로드 중 예기치 못한 오류", error)
                    return Result.retry()
                }

            run.blockedBy?.let {
                // 서버 주소가 없거나 로그인이 필요해서 아예 시도하지 못한 경우.
                // 재시도해도 같으므로 물러난다. 앱을 다시 켜거나 로그인하면 다시 예약된다.
                Log.i(TAG, "업로드를 건너뜁니다: $it")
                return Result.success()
            }

            Log.i(TAG, "업로드 결과 — 서명 ${run.signed} · 거절 ${run.rejected} · 실패 ${run.failed}")

            // 실패가 있으면 WorkManager 의 백오프에 맡긴다. 직접 루프를 돌며
            // 재시도하면 배터리를 태우고, 지하철에서 나올 때까지 아무 소용이 없다.
            if (run.shouldRetry) return Result.retry()
            // 이번에 처리한 것이 없으면 대기열이 비었다.
            if (run.signed + run.rejected == 0) return Result.success()
        }
        return Result.retry()
    }

    companion object {
        private const val TAG = "SessionUpload"

        /** 같은 일을 여러 번 예약하지 않도록 하나의 이름으로 묶는다 */
        private const val WORK_NAME = "session-upload"

        /**
         * 대기열을 처리하도록 예약한다.
         *
         * 이미 예약된 것이 있으면 그대로 둔다([ExistingWorkPolicy.KEEP]).
         * 러닝을 연달아 끝내도 일꾼은 하나면 충분하고, 새로 예약하면 이미
         * 쌓인 백오프 시간이 초기화되어 끊긴 네트워크를 계속 두드리게 된다.
         */
        fun schedule(context: Context) {
            val request = OneTimeWorkRequestBuilder<SessionUploadWorker>()
                .setConstraints(
                    Constraints.Builder()
                        .setRequiredNetworkType(NetworkType.CONNECTED)
                        .build(),
                )
                .build()

            WorkManager.getInstance(context)
                .enqueueUniqueWork(WORK_NAME, ExistingWorkPolicy.KEEP, request)
        }
    }
}
