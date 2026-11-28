package com.stepup.android.sensor

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
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import com.stepup.android.MainActivity
import com.stepup.android.R
import com.stepup.android.core.ServiceLocator
import com.stepup.android.push.GoalReminderWorker
import java.util.concurrent.TimeUnit
import kotlinx.coroutines.flow.first

/**
 * 걸음 일꾼(2026-11-26) — 앱을 켜지 않아도 한 시간마다 하루 걸음을 채우고 서버에 올린다.
 *
 * 1. 구글 Play 서비스가 기록해 둔 날마다 걸음([StepHistory])을 폰 기록에 채운다(큰 값만 — 하루 걸음은 줄지 않는다).
 * 2. 로그인했고 서버 경제면 어제 · 오늘을 올리고(steps_sync — DailyStepsUploader), 하루 걸음 상자(서버 0128)를 받아 본다 — 처음 받았으면 알린다.
 *
 * 러닝 보상 · 목표 · 연속은 이 걸음으로 정하지 않는다(서버가 경로로 확인된 러닝 걸음으로 정한다, 0118). 걸음 권한이 없으면 아무것도 하지 않는다.
 */
class DailyStepsWorker(context: Context, params: WorkerParameters) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        runCatching { sync(applicationContext) }
        return Result.success()
    }

    companion object {
        private const val WORK_NAME = "daily_steps_sync"

        /** 한 시간마다 — 앱을 켤 때마다 불러도 하나(KEEP) */
        fun schedule(context: Context) {
            runCatching {
                val request = PeriodicWorkRequestBuilder<DailyStepsWorker>(1, TimeUnit.HOURS)
                    .setConstraints(Constraints.Builder().setRequiresBatteryNotLow(true).build())
                    .build()
                WorkManager.getInstance(context).enqueueUniquePeriodicWork(WORK_NAME, ExistingPeriodicWorkPolicy.KEEP, request)
            }
        }

        /** 걸음 기록을 켜고 지난 날들을 채운 뒤 올린다 — 일꾼과 앱 시작이 같은 일을 한다 */
        suspend fun sync(context: Context) {
            ServiceLocator.init(context)
            if (!StepHistory.available(context)) return
            StepHistory.subscribe(context)
            val days = StepHistory.readDays(context)
            ServiceLocator.stepRepository.mergeHistory(days)
            if (!ServiceLocator.serverEconomyOn || !ServiceLocator.sessionHolder.isSignedIn()) return
            runCatching { ServiceLocator.dailyStepsUploader.upload() }
            claimStepBox(context)
        }

        /** 하루 걸음 상자(0128)를 받아 본다 — 이번에 처음 받았으면 알린다. 서버에 없으면 조용히 */
        suspend fun claimStepBox(context: Context) {
            val box = (ServiceLocator.stepBoxClaim() as? com.stepup.android.data.remote.ServerResult.Ok)?.value ?: return
            if (box.on && box.isNew) {
                runCatching { ServiceLocator.shoeRules.refreshBoxes() }
                notifyBox(context, box.need)
            }
        }

        private suspend fun notifyBox(context: Context, need: Int) {
            if (runCatching { !ServiceLocator.userPrefs.notifyPrefs.first().push }.getOrDefault(false)) return
            val allowed = Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU ||
                ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED
            if (!allowed) return
            val manager = context.getSystemService(NotificationManager::class.java) ?: return
            manager.createNotificationChannel(
                NotificationChannel(
                    GoalReminderWorker.REWARDS_CHANNEL_ID, context.getString(R.string.dr_channel_name), NotificationManager.IMPORTANCE_DEFAULT,
                ),
            )
            val intent = Intent(context, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
            }
            com.stepup.android.core.LinkToken.stamp(context, intent)
            val pending = PendingIntent.getActivity(
                context, NOTIFICATION_ID, intent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
            )
            val body = context.getString(R.string.step_box_notice_body, need)
            val notification = NotificationCompat.Builder(context, GoalReminderWorker.REWARDS_CHANNEL_ID)
                .setSmallIcon(R.drawable.ic_stat_walk)
                .setContentTitle(context.getString(R.string.step_box_notice_title))
                .setContentText(body)
                .setStyle(NotificationCompat.BigTextStyle().bigText(body))
                .setAutoCancel(true)
                .setContentIntent(pending)
                .build()
            runCatching { NotificationManagerCompat.from(context).notify(NOTIFICATION_ID, notification) }
        }

        private const val NOTIFICATION_ID = 0x5B0C
    }
}
