package com.stepup.android.service

import android.Manifest
import android.app.Notification
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
import com.stepup.android.LockRunActivity
import com.stepup.android.R
import com.stepup.android.domain.wornPercent

/**
 * 달리는 중 신발 내구도 알림(2026-11-26 테스터 "20% / 10% 에서 알림음이 나오면 러닝 중 인지하기가 더 편할 것").
 *
 * 러닝 알림(소리 없는 채널 `run_live`)과 따로, 소리 · 진동이 나는 알림(채널 `run_shoe_alert`)으로 20% · 10% · 다 씀을 한 번씩 알린다 —
 * 화면을 끄고 주머니에 넣고 달려도 들린다. 같은 자리에 바꿔 띄워 알림 줄에 쌓이지 않는다. 러닝이 끝나면 거둔다.
 * 진동은 채널이 맡는다(진동 권한 없음). 알림 권한이 없으면 띄우지 못한다 — 그때는 달리는 중 화면이 소리를 낸다(다 씀만).
 */
object ShoeDurabilityAlerts {

    const val CHANNEL_ID = "run_shoe_alert"
    private const val NOTIFICATION_ID = 0x5D11

    /** 남은 [percent]% — [minutesLeft] 는 가득 적립하며 더 달릴 수 있는 분(모르면 null) */
    fun low(context: Context, percent: Int, minutesLeft: Long?) {
        val title = context.getString(R.string.shoe_alert_low_title, percent)
        val body = if (minutesLeft != null && minutesLeft > 0) {
            context.getString(R.string.shoe_alert_low_body_min, minutesLeft.toInt())
        } else {
            context.getString(R.string.shoe_alert_low_body)
        }
        post(context, title, body)
    }

    /** 다 썼다 — 그 뒤 걸음은 일부만 적립(서버 0119), 예전 규칙이면 적립 없음 */
    fun wornOut(context: Context) {
        val stats = com.stepup.android.data.repo.shoeRulesNow().stats
        val body = if (stats.earnsWhenWorn) context.getString(R.string.roots_notification_shoe_worn, stats.wornPercent)
        else context.getString(R.string.notification_run_shoe_worn)
        post(context, context.getString(R.string.shoe_alert_worn_title), body)
    }

    /** 러닝이 끝났다 — 남은 알림을 거둔다 */
    fun clear(context: Context) {
        runCatching { NotificationManagerCompat.from(context).cancel(NOTIFICATION_ID) }
    }

    /** 알림을 띄울 수 있다(권한) */
    fun canAlert(context: Context): Boolean = Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU ||
        ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED

    private fun post(context: Context, title: String, body: String) {
        if (!canAlert(context)) return
        createChannel(context)
        // 누르면 러닝 현황(잠겨 있으면 잠금 화면 가운데, 아니면 앱) — 러닝 알림과 같은 곳
        val open = PendingIntent.getActivity(
            context, NOTIFICATION_ID, Intent(context, LockRunActivity::class.java),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_stat_walk)
            .setContentTitle(title)
            .setContentText(body)
            .setStyle(NotificationCompat.BigTextStyle().bigText(body))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setCategory(NotificationCompat.CATEGORY_STATUS)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .setAutoCancel(true)
            .setContentIntent(open)
            // 오래 남지 않게 — 러닝 알림이 계속 % 를 보인다
            .setTimeoutAfter(TIMEOUT_MS)
            .build()
        try {
            NotificationManagerCompat.from(context).notify(NOTIFICATION_ID, notification)
        } catch (_: SecurityException) {
            // 그사이 알림 권한을 거뒀다
        }
    }

    private fun createChannel(context: Context) {
        val manager = context.getSystemService(NotificationManager::class.java) ?: return
        manager.createNotificationChannel(
            NotificationChannel(
                CHANNEL_ID, context.getString(R.string.notification_channel_shoe_alert), NotificationManager.IMPORTANCE_HIGH,
            ).apply {
                enableVibration(true)
                setShowBadge(false)
                lockscreenVisibility = Notification.VISIBILITY_PUBLIC
            },
        )
    }

    private const val TIMEOUT_MS = 60_000L
}
