package com.stepup.android.push

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
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import com.stepup.android.MainActivity
import com.stepup.android.R
import com.stepup.android.core.ServiceLocator
import java.time.Duration
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.ZoneId
import java.util.concurrent.TimeUnit
import kotlinx.coroutines.flow.first

/**
 * 목표 알림 — 저녁에 오늘 걸음이 목표에 못 미치면 한 번 알려 준다.
 *
 * 서버가 보내는 푸시가 아니다. 걸음은 폰이 제일 잘 알고, 알림 하나를 위해
 * 걸음을 서버에 계속 올릴 이유가 없다. 알림 설정의 "목표 알림"과 "푸시 알림"이
 * 둘 다 켜져 있을 때만 울린다.
 */
class GoalReminderWorker(
    context: Context,
    params: WorkerParameters,
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        try {
            return remind()
        } finally {
            anchorNextRun()
        }
    }

    /**
     * 다음 차례를 내일 저녁 7시로 못 박는다.
     *
     * 하루 주기 일은 "지난번이 끝난 때 + 하루"에 돈다. 기기가 잠들어 한 번 22시를 넘겨
     * 깨면 그다음부터 매일 그 시각 이후에 돌아, 시간 검사에 걸려 영영 울리지 않는다.
     */
    private fun anchorNextRun() {
        runCatching {
            val request = PeriodicWorkRequestBuilder<GoalReminderWorker>(1, TimeUnit.DAYS)
                .setId(id)
                .setNextScheduleTimeOverride(nextRemindAtMillis(LocalDateTime.now()))
                .build()
            WorkManager.getInstance(applicationContext).updateWork(request)
        }
    }

    private suspend fun remind(): Result {
        ServiceLocator.init(applicationContext)
        val prefs = ServiceLocator.userPrefs
        val notify = prefs.notifyPrefs.first()
        if (!notify.push || !notify.goalReminder) return Result.success()

        // 하루 한 번 도는 일이지만 기기가 잠들어 있으면 늦게 깬다. 밤늦게 울리지 않게.
        val hour = LocalTime.now().hour
        if (hour !in REMIND_FROM_HOUR until REMIND_UNTIL_HOUR) return Result.success()

        val goal = prefs.dailyGoal.first()
        val steps = runCatching {
            ServiceLocator.database.stepDao().byDay(LocalDate.now().toEpochDay())?.steps ?: 0
        }.getOrDefault(0)
        val left = goal - steps
        if (left <= 0) return Result.success()

        show(applicationContext, left)
        return Result.success()
    }

    companion object {
        private const val WORK_NAME = "goal-reminder"
        const val CHANNEL_ID = "reminder"

        /** 이 시각(시)에 알린다 */
        private const val REMIND_AT_HOUR = 19
        private const val REMIND_FROM_HOUR = 18
        private const val REMIND_UNTIL_HOUR = 22

        /** 다음 알림 시각(저녁 7시). 이미 저녁 시간대면 내일이다. */
        private fun nextRemindAtMillis(now: LocalDateTime): Long {
            var next = now.toLocalDate().atTime(REMIND_AT_HOUR, 0)
            if (now.hour >= REMIND_FROM_HOUR) next = next.plusDays(1)
            return next.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()
        }

        /** 매일 저녁 7시쯤 한 번. 이미 예약돼 있으면 그대로 둔다. */
        fun schedule(context: Context) {
            val now = LocalDateTime.now()
            var next = now.toLocalDate().atTime(REMIND_AT_HOUR, 0)
            if (!next.isAfter(now)) next = next.plusDays(1)
            val delay = Duration.between(now, next).toMillis()
            val request = PeriodicWorkRequestBuilder<GoalReminderWorker>(1, TimeUnit.DAYS)
                .setInitialDelay(delay, TimeUnit.MILLISECONDS)
                .build()
            WorkManager.getInstance(context)
                .enqueueUniquePeriodicWork(WORK_NAME, ExistingPeriodicWorkPolicy.KEEP, request)
        }

        fun createChannel(context: Context) {
            val manager = context.getSystemService(NotificationManager::class.java) ?: return
            manager.createNotificationChannel(
                NotificationChannel(
                    CHANNEL_ID,
                    context.getString(R.string.notification_channel_reminder),
                    NotificationManager.IMPORTANCE_DEFAULT,
                ),
            )
        }

        private fun show(context: Context, stepsLeft: Int) {
            createChannel(context)
            val intent = Intent(context, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
            }
            val pending = PendingIntent.getActivity(
                context,
                WORK_NAME.hashCode(),
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
            )
            val title = context.getString(R.string.reminder_goal_title, stepsLeft)
            val body = context.getString(R.string.reminder_goal_body)
            val notification = NotificationCompat.Builder(context, CHANNEL_ID)
                .setSmallIcon(R.drawable.ic_stat_walk)
                .setContentTitle(title)
                .setContentText(body)
                .setAutoCancel(true)
                .setContentIntent(pending)
                .build()
            val allowed = Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU ||
                ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) ==
                PackageManager.PERMISSION_GRANTED
            if (allowed) {
                NotificationManagerCompat.from(context).notify(WORK_NAME.hashCode(), notification)
            }
        }
    }
}
