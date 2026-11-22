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
import androidx.annotation.StringRes
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.stepup.android.MainActivity
import com.stepup.android.R

/**
 * 크루 러닝 중 꼭 알아야 하는 일의 알림(2026-11-18) — 함께 달리던 사람이 모두 마쳐 내 크루 러닝도 곧 끝난다 · 끝났다.
 *
 * 예전에는 마지막 한 사람이 되면 러닝이 말없이 멈추고 러닝 알림만 사라졌다 — 휴대폰을 주머니에 넣고 달리는 사람은
 * 기록이 끊긴 줄 모르고 계속 달렸다. 러닝 알림(소리 없는 채널)과 따로, 소리 · 진동이 나는 높은 중요도의 알림(헤드업)으로 알린다.
 * 진동은 알림 채널이 맡는다(앱이 직접 울리지 않는다 — 진동 권한이 없다). 알림 권한이 없으면 띄우지 못한다(화면의 띠 · 시트는 그대로 보인다).
 */
object CrewRunAlerts {

    const val CHANNEL_ID = "crew_run_alert"
    private const val NOTIFICATION_ID = 0x5C2F

    /** 마지막 한 사람 — "함께 달리던 모두가 마쳤어요 · N초 뒤 내 크루 러닝도 끝나요". 알림의 시계가 남은 시간을 거꾸로 센다 */
    fun alone(context: Context, leftSec: Int) {
        val left = leftSec.coerceAtLeast(1)
        val builder = base(context, R.string.crew_alone_notif_title, context.getString(R.string.crew_alone_notif_body, left))
            ?: return
        builder
            .setUsesChronometer(true)
            .setChronometerCountDown(true)
            .setShowWhen(true)
            .setWhen(System.currentTimeMillis() + left * 1000L)
            // 끝났다는 알림이 이 자리를 바꾼다 — 그 알림이 오지 못해도(저장 실패) 끝난 뒤까지 "곧 끝나요"가 남지 않게
            .setTimeoutAfter(left * 1000L + LINGER_MS)
        post(context, builder.build())
    }

    /** 그 러닝이 끝났고 기록이 저장됐다 — 위 알림을 바꾼다(다시 소리 · 진동) */
    fun aloneSaved(context: Context) {
        val builder = base(context, R.string.crew_alone_ended_title, context.getString(R.string.crew_alone_ended_body)) ?: return
        post(context, builder.build())
    }

    /** 알린 것을 거둔다 — 사용자가 먼저 러닝을 마쳤다 */
    fun clear(context: Context) {
        runCatching { NotificationManagerCompat.from(context).cancel(NOTIFICATION_ID) }
    }

    private fun base(context: Context, @StringRes title: Int, body: String): NotificationCompat.Builder? {
        if (!allowed(context)) return null
        createChannel(context)
        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        // 우리 알림이라는 표(LinkToken — InviteLinks.trustsExtras)
        com.stepup.android.core.LinkToken.stamp(context, intent)
        val pending = PendingIntent.getActivity(
            context, NOTIFICATION_ID, intent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
        return NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_stat_walk)
            .setContentTitle(context.getString(title))
            .setContentText(body)
            .setStyle(NotificationCompat.BigTextStyle().bigText(body))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setCategory(NotificationCompat.CATEGORY_STATUS)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .setAutoCancel(true)
            .setContentIntent(pending)
    }

    private fun allowed(context: Context): Boolean = Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU ||
        ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED

    private fun post(context: Context, notification: Notification) {
        val allowed = Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU ||
            ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED
        if (!allowed) return
        try {
            NotificationManagerCompat.from(context).notify(NOTIFICATION_ID, notification)
        } catch (e: SecurityException) {
            // 그사이 알림 권한을 거뒀다
        }
    }

    private fun createChannel(context: Context) {
        val manager = context.getSystemService(NotificationManager::class.java) ?: return
        manager.createNotificationChannel(
            NotificationChannel(
                CHANNEL_ID,
                context.getString(R.string.notification_channel_crew_run_alert),
                NotificationManager.IMPORTANCE_HIGH,
            ).apply {
                // 소리는 채널 기본값(알림 소리) 그대로, 진동도 켠다 — 화면을 보지 않고 달리는 사람이 알 수 있게
                enableVibration(true)
                setShowBadge(false)
                lockscreenVisibility = Notification.VISIBILITY_PUBLIC
            },
        )
    }

    /** 끝날 시각 뒤에도 "곧 끝나요" 알림을 잠깐 남겨 두는 시간 — 그사이 "끝났어요"가 바꾼다 */
    private const val LINGER_MS = 15_000L
}
