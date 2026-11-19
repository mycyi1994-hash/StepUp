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
 * 서버가 보내는 푸시가 아니다. 알림 설정의 "목표 알림"과 "푸시 알림"이 둘 다 켜져 있을 때만 울린다.
 *
 * 2026-11-19(오늘의 보상, 서버 0118) — 목표는 러닝 걸음 목표다. 서버가 오늘의 보상을 주면 "목표까지 N보"는 서버의 마지막 상태(오늘 보상
 * 걸음 · 오늘 목표)로 센다 — 폰 만보기 걸음으로 세면 막대는 0 인데 알림은 조용했다. 오늘 상태를 모르면(받지 못했다) 울리지 않는다.
 * 이미 목표 보너스를 받았으면 울리지 않는다. 서버 경제가 없는 빌드만 예전처럼 하루 전체 걸음으로 센다.
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

        val server = if (ServiceLocator.serverEconomyOn) serverStepsLeft() else ServerLeft.OldRule
        val left = when (server) {
            is ServerLeft.Left -> server.steps
            ServerLeft.Unknown -> return Result.success()
            ServerLeft.OldRule -> {
                val goal = prefs.dailyGoal.first()
                val steps = runCatching {
                    ServiceLocator.database.stepDao().byDay(LocalDate.now().toEpochDay())?.steps ?: 0
                }.getOrDefault(0)
                goal - steps
            }
        }
        if (left <= 0) return Result.success()

        show(applicationContext, left)
        return Result.success()
    }

    private sealed interface ServerLeft {
        /** 목표까지 남은 러닝 걸음(서버 상태) */
        data class Left(val steps: Int) : ServerLeft

        /** 오늘 상태를 모른다 · 이미 받았다 — 울리지 않는다 */
        data object Unknown : ServerLeft

        /** 서버에 오늘의 보상(0118)이 없다 — 예전처럼 하루 전체 걸음으로 */
        data object OldRule : ServerLeft
    }

    /** 목표까지 남은 러닝 걸음 — 서버 상태(오늘의 보상)를 한 번 다시 묻고(못 닿으면 폰에 남은 마지막 상태) 오늘 것일 때만 */
    private suspend fun serverStepsLeft(): ServerLeft {
        val repo = ServiceLocator.shoeRules
        runCatching { repo.restore() }
        val read = if (runCatching { ServiceLocator.sessionHolder.isSignedIn() }.getOrDefault(false)) {
            runCatching { kotlinx.coroutines.withTimeoutOrNull(15_000) { repo.refreshBoxes() } }.getOrNull()
        } else {
            null
        }
        val state = repo.daily.value ?: return if (read != null) ServerLeft.OldRule else ServerLeft.Unknown
        val today = com.stepup.android.domain.DailyRewards.gameDay(System.currentTimeMillis())
        if (!state.isFor(today) || state.goalPaid != null || state.goal <= 0) return ServerLeft.Unknown
        return ServerLeft.Left(state.goal - state.rewardStepsToday)
    }

    companion object {
        private const val WORK_NAME = "goal-reminder"
        const val CHANNEL_ID = "reminder"

        /** 러닝이 올라간 뒤 새로 받은 보상(앱이 뒤에 있을 때) — 소리 있는 보통 알림 */
        const val REWARDS_CHANNEL_ID = "daily_rewards"
        private const val REWARDS_NOTIFICATION_ID = 0x5D1E

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

        /**
         * 새로 받은 보상을 알린다(앱이 뒤에 있을 때) — "러닝 상자를 받았어요 · 목표 보너스 +24 SUP · 3일째 연속". 서버가 받았다고 한 것만
         * (ShoeRulesRepository.refreshForNews). "모든 푸시 알림"을 껐으면 띄우지 않는다.
         */
        suspend fun showRewards(context: Context, news: com.stepup.android.domain.RewardNews) {
            if (runCatching { !ServiceLocator.userPrefs.notifyPrefs.first().push }.getOrDefault(false)) return
            val text = rewardNewsText(context, news).ifBlank { return }
            val manager = context.getSystemService(NotificationManager::class.java) ?: return
            manager.createNotificationChannel(
                NotificationChannel(
                    REWARDS_CHANNEL_ID,
                    context.getString(R.string.dr_channel_name),
                    NotificationManager.IMPORTANCE_DEFAULT,
                ),
            )
            val intent = Intent(context, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
            }
            val pending = PendingIntent.getActivity(
                context, REWARDS_CHANNEL_ID.hashCode(), intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
            )
            val notification = NotificationCompat.Builder(context, REWARDS_CHANNEL_ID)
                .setSmallIcon(R.drawable.ic_stat_walk)
                .setContentTitle(context.getString(R.string.dr_news_title))
                .setContentText(text)
                .setStyle(NotificationCompat.BigTextStyle().bigText(text))
                .setAutoCancel(true)
                .setContentIntent(pending)
                .build()
            val allowed = Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU ||
                ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) ==
                PackageManager.PERMISSION_GRANTED
            if (allowed) runCatching { NotificationManagerCompat.from(context).notify(REWARDS_NOTIFICATION_ID, notification) }
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

/**
 * 새로 받은 보상 한 줄 — 홈의 한 줄과 알림이 같은 글을 쓴다. 지난날 받은 것은 날을 붙인다("어제 러닝 상자를 받았어요").
 * 금액 · 연속 일수는 서버 값 그대로(RewardNews).
 */
fun rewardNewsText(context: Context, news: com.stepup.android.domain.RewardNews): String =
    news.items.joinToString(" · ") { item ->
        val body = when (item.kind) {
            com.stepup.android.domain.RecentRewardKind.RUN_BOX ->
                if (item.count > 1) context.getString(R.string.dr_news_run_boxes, item.count) else context.getString(R.string.dr_news_run_box)
            com.stepup.android.domain.RecentRewardKind.WEEK_BOX -> context.getString(R.string.dr_news_week_box)
            com.stepup.android.domain.RecentRewardKind.STREAK_BOX -> context.getString(R.string.dr_news_streak_box)
            com.stepup.android.domain.RecentRewardKind.LEVEL_BOX -> context.getString(R.string.dr_news_level_box)
            com.stepup.android.domain.RecentRewardKind.GOAL -> {
                val amount = item.amount
                val ago = com.stepup.android.domain.DailyRewardsNews.daysAgo(item, news.today)
                when {
                    amount == null -> context.getString(R.string.dr_news_goal_plain)
                    ago == 0L && (news.streak ?: 0) >= 2 -> context.getString(
                        R.string.dr_news_goal_streak, com.stepup.android.domain.DailyRewardsBar.supText(amount), news.streak ?: 0,
                    )
                    else -> context.getString(R.string.dr_news_goal, com.stepup.android.domain.DailyRewardsBar.supText(amount))
                }
            }
        }
        when (val ago = com.stepup.android.domain.DailyRewardsNews.daysAgo(item, news.today)) {
            0L -> body
            1L -> context.getString(R.string.dr_news_yesterday, body)
            else -> context.getString(
                R.string.dr_news_on_day,
                (item.day ?: news.today.minusDays(ago)).format(java.time.format.DateTimeFormatter.ofPattern("M/d")), body,
            )
        }
    }
