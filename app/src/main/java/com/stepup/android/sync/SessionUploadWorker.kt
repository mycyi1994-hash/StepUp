package com.stepup.android.sync

import android.content.Context
import android.util.Log
import androidx.work.BackoffPolicy
import androidx.work.Constraints
import androidx.work.CoroutineWorker
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import com.stepup.android.core.ServiceLocator
import java.util.concurrent.TimeUnit

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

        // 과속으로 멈춘 러닝의 패널티 신고를 러닝보다 먼저 보낸다 — 서버가 금지 시간을 알아야 그 사이 러닝을 무효로 둔다(0065).
        // 못 보냈으면 러닝도 올리지 않고 다시 시도한다(순서가 뒤바뀌지 않게)
        val reported = runCatching { ServiceLocator.overspeedReporter.flush() }
            .getOrElse { error ->
                Log.w(TAG, "과속 정지 신고 중 예기치 못한 오류", error)
                false
            }
        if (!reported) return Result.retry()

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

        /** 실패한 뒤 다시 하기까지 — n 번째 실패 뒤 n × 이만큼(초) */
        private const val BACKOFF_SECONDS = 30L

        /**
         * 대기열을 처리하도록 예약한다.
         *
         * 돌고 있거나 기다리는(연결 · 백오프) 일꾼이 있으면 그 뒤에 하나를 잇는다([ExistingWorkPolicy.APPEND_OR_REPLACE]).
         * 앞 일꾼의 백오프는 그대로다 — 새로 예약해 초기화하면 끊긴 네트워크를 계속 두드린다. 이은 일꾼은 앞 일꾼이 끝난 뒤
         * 대기열을 한 번 보고, 비었으면 바로 끝난다.
         *
         * 예전엔 있던 일꾼을 그대로 두고 새 예약을 버렸다(KEEP). 앞 일꾼이 대기열을 마지막으로 본 직후 끝난 러닝은 그 예약이
         * 버려져 다음 러닝 · 앱을 다시 켤 때까지 "정산 대기"로 남았다(2026-10-17 점검). 끝내기 직전에 한 번 더 보는 것으로는
         * 그 틈이 옮겨 갈 뿐 닫히지 않는다.
         */
        fun schedule(context: Context) {
            val request = OneTimeWorkRequestBuilder<SessionUploadWorker>()
                .setConstraints(
                    Constraints.Builder()
                        .setRequiredNetworkType(NetworkType.CONNECTED)
                        .build(),
                )
                // 실패할 때마다 30초씩 더 기다린다(기본은 두 배씩 — 몇 번 끊긴 뒤엔 와이파이에서 끝낸 러닝도 몇 시간씩
                // "정산 대기"였다, 2026-10-17 점검). 끊긴 동안은 연결 조건이 두드리지 않게 막는다
                .setBackoffCriteria(BackoffPolicy.LINEAR, BACKOFF_SECONDS, TimeUnit.SECONDS)
                .build()

            WorkManager.getInstance(context)
                .enqueueUniqueWork(WORK_NAME, ExistingWorkPolicy.APPEND_OR_REPLACE, request)
        }
    }
}
