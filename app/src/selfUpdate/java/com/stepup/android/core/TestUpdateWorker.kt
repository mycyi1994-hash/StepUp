package com.stepup.android.core

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import androidx.work.Constraints
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.NetworkType
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import com.stepup.android.MainActivity
import com.stepup.android.R
import java.util.concurrent.TimeUnit

/**
 * 테스트 APK 를 앱이 꺼져 있어도 새 버전으로 — 1시간마다 묻고, 앱을 쓰지 않고 러닝 중이 아니면 받아서 깐다([TestUpdates.backgroundUpdate]).
 * test-apk 빌드(debug · qa)에서만 걸린다([TestUpdates.attach]).
 */
class TestUpdateWorker(
    context: Context,
    params: WorkerParameters,
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        runCatching { TestUpdates.backgroundUpdate(applicationContext) }
        return Result.success()
    }

    companion object {
        private const val WORK_NAME = "test-apk-update"
        const val CHANNEL_ID = "app_update"
        private const val NOTIFY_ID = 7_301
        private const val PREFS = "test_updates"
        private const val KEY_NOTIFIED = "notified_commit"

        fun schedule(context: Context) {
            val request = PeriodicWorkRequestBuilder<TestUpdateWorker>(1, TimeUnit.HOURS)
                .setConstraints(Constraints.Builder().setRequiredNetworkType(NetworkType.CONNECTED).build())
                .build()
            WorkManager.getInstance(context)
                .enqueueUniquePeriodicWork(WORK_NAME, ExistingPeriodicWorkPolicy.KEEP, request)
        }

        fun createChannel(context: Context) {
            val manager = context.getSystemService(NotificationManager::class.java) ?: return
            manager.createNotificationChannel(
                NotificationChannel(
                    CHANNEL_ID,
                    context.getString(R.string.test_update_channel),
                    NotificationManager.IMPORTANCE_DEFAULT,
                ).apply { setSound(null, null) },
            )
        }

        /** "새 버전이 있어요 — 눌러서 업데이트" — 확인이 필요한 폰에서. 같은 빌드는 한 번만 */
        fun notifyAvailable(context: Context, build: TestBuild) {
            val prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            if (build.commit.isNotBlank() && prefs.getString(KEY_NOTIFIED, null) == build.commit) return
            val shown = show(
                context,
                context.getString(R.string.test_update_notify_available_title),
                context.getString(R.string.test_update_notify_available_body, build.version + build.commit.takeIf { it.isNotBlank() }?.let { " · $it" }.orEmpty()),
            )
            if (shown) prefs.edit().putString(KEY_NOTIFIED, build.commit).apply()
        }

        /** "새 버전으로 업데이트했어요 · 눌러서 열기" — 이 앱이 깐 업데이트가 끝난 뒤 */
        fun notifyUpdated(context: Context, label: String) {
            show(
                context,
                context.getString(R.string.test_update_notify_done_title),
                context.getString(R.string.test_update_notify_done_body, label),
            )
        }

        private fun show(context: Context, title: String, body: String): Boolean {
            val allowed = Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU ||
                ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED
            if (!allowed) return false
            createChannel(context)
            val open = PendingIntent.getActivity(
                context,
                NOTIFY_ID,
                Intent(context, MainActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP),
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
            )
            val notification = NotificationCompat.Builder(context, CHANNEL_ID)
                .setSmallIcon(R.drawable.ic_stat_walk)
                .setContentTitle(title)
                .setContentText(body)
                .setAutoCancel(true)
                .setContentIntent(open)
                .build()
            return runCatching { NotificationManagerCompat.from(context).notify(NOTIFY_ID, notification) }.isSuccess
        }
    }
}
