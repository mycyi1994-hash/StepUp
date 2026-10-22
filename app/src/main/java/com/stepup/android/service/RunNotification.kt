package com.stepup.android.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.SystemClock
import android.widget.RemoteViews
import androidx.core.app.NotificationCompat
import com.stepup.android.MainActivity
import com.stepup.android.R
import com.stepup.android.domain.RunNotificationText

/**
 * 러닝 중 알림(2026-10-18 사용자 "백그라운드에서 재생할 때 잠금화면에 우리 거 나오게").
 *
 * - 안드로이드 16(API 36) 이상: 표준 알림 + **실시간 알림(Live Updates)** 요청 — 상태 표시줄 칩과 잠금화면 맨 위에 크게 고정된다.
 *   실시간 알림은 직접 그린 모양(RemoteViews)과 색 입힌 알림을 받지 않아 표준 모양에 시간(흐르는 시계) · 거리 · 페이스를 쓴다.
 * - 그 아래: 남색 바탕에 우리 모양(로고 · 큰 시간 · 거리 | 페이스) — 접힌 · 펼친 두 모양.
 * - 둘 다 잠금화면에서 내용을 보이고(PUBLIC), 일시정지 / 다시 달리기 버튼이 있다. 마치기는 앱에서만(잠금화면에서 실수로 끝내지 않게).
 * - 시간은 시계 칸(Chronometer)이 스스로 흐르므로 1초마다 다시 쓰지 않는다. 거리 · 페이스는 서비스가 몇 초마다 고쳐 쓴다.
 */
internal object RunNotification {
    const val CHANNEL_ID = "run_live"

    /** 안드로이드 16 실시간 알림 요청 표시(Notification.EXTRA_REQUEST_PROMOTED_ONGOING) — SDK 36 상수를 글자로 쓴다 */
    private const val EXTRA_REQUEST_PROMOTED_ONGOING = "android.requestPromotedOngoing"

    /** 상태 표시줄 칩의 짧은 글(Notification.EXTRA_SHORT_CRITICAL_TEXT) */
    private const val EXTRA_SHORT_CRITICAL_TEXT = "android.shortCriticalText"

    /** 남색 바탕 — 러닝 화면과 같은 계열 */
    private const val NAVY = 0xFF061A38.toInt()

    private const val LIVE_UPDATES_SDK = 36

    fun createChannel(context: Context) {
        val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        // 잠금화면에 펼쳐 보이려면 '조용한 알림'(LOW)보다 높아야 한다 — 소리 · 진동은 없다
        val channel = NotificationChannel(
            CHANNEL_ID, context.getString(R.string.notification_channel_walk), NotificationManager.IMPORTANCE_DEFAULT,
        ).apply {
            setSound(null, null)
            enableVibration(false)
            setShowBadge(false)
            lockscreenVisibility = Notification.VISIBILITY_PUBLIC
        }
        manager.createNotificationChannel(channel)
        // 예전 채널(조용한 알림) — 새 채널로 옮겼다
        manager.deleteNotificationChannel("walk_session")
    }

    fun build(context: Context, state: WalkSessionState): Notification {
        val open = PendingIntent.getActivity(
            context, 0,
            Intent(context, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
            },
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
        val paused = state.isPaused
        val toggle = PendingIntent.getService(
            context, if (paused) 2 else 1,
            Intent(context, WalkSessionService::class.java).setAction(
                if (paused) WalkSessionService.ACTION_RESUME else WalkSessionService.ACTION_PAUSE,
            ),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
        val km = state.gpsKm
        val distance = context.getString(R.string.run_notif_distance, RunNotificationText.distance(km))
        val pace = RunNotificationText.pace(state.elapsedSec, km)
        val crewLeft = state.crewPoolRemaining
        val crewTotal = state.crewPoolTotal
        val status = when {
            paused -> context.getString(R.string.run_notif_paused)
            state.still -> context.getString(R.string.notification_run_still)
            // 크루 러닝(0071) — 오늘 공용 체력 남은 양 / 총량. 0 이면 적립 없이 기록만 이어진다
            crewLeft != null && crewTotal != null ->
                if (state.crewPoolEmpty || crewLeft <= 0.0) context.getString(R.string.run_notify_crew_pool_empty)
                else context.getString(
                    R.string.run_notify_crew_pool,
                    com.stepup.android.domain.CrewRunMath.amount(crewLeft), com.stepup.android.domain.CrewRunMath.amount(crewTotal),
                )
            // 신발 내구도를 다 썼다 — 잠금화면에서도 보이게(2026-10-19 "내구도 다 소모하면 따로 안내")
            state.shoeWornOut -> context.getString(R.string.notification_run_shoe_worn)
            else -> context.getString(R.string.notification_steps, state.steps)
        }
        val builder = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_stat_walk)
            .setContentIntent(open)
            .setOngoing(true)
            .setOnlyAlertOnce(true)
            .setSilent(true)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .setCategory(NotificationCompat.CATEGORY_PROGRESS)
            .addAction(
                if (paused) R.drawable.ic_notif_play else R.drawable.ic_notif_pause,
                context.getString(if (paused) R.string.run_notif_resume else R.string.run_notif_pause),
                toggle,
            )
        // 시간 — 일시정지면 멈춘 값, 아니면 스스로 흐르는 시계
        val startedAtWall = System.currentTimeMillis() - state.elapsedSec * 1000
        if (Build.VERSION.SDK_INT >= LIVE_UPDATES_SDK) {
            builder
                .setContentTitle(context.getString(if (paused) R.string.run_notif_title_paused else R.string.run_notif_title))
                .setContentText(
                    listOfNotNull(distance, pace?.let { context.getString(R.string.run_notif_pace, it) }).joinToString(" · "),
                )
                .setSubText(status)
                .setStyle(NotificationCompat.BigTextStyle().bigText(
                    listOfNotNull(distance, pace?.let { context.getString(R.string.run_notif_pace, it) }, status).joinToString(" · "),
                ))
                .setShowWhen(true)
                .setWhen(startedAtWall)
                .setUsesChronometer(!paused)
            builder.extras.putBoolean(EXTRA_REQUEST_PROMOTED_ONGOING, true)
            builder.extras.putString(EXTRA_SHORT_CRITICAL_TEXT, distance)
        } else {
            val base = RunNotificationText.chronometerBase(SystemClock.elapsedRealtime(), state.elapsedSec)
            val small = RemoteViews(context.packageName, R.layout.notification_run).apply {
                setChronometer(R.id.run_notif_time, base, null, !paused)
                setTextViewText(R.id.run_notif_distance_small, distance)
                setTextViewText(R.id.run_notif_status, status)
            }
            val big = RemoteViews(context.packageName, R.layout.notification_run_big).apply {
                setChronometer(R.id.run_notif_time, base, null, !paused)
                setTextViewText(R.id.run_notif_status, status)
                setTextViewText(R.id.run_notif_distance_label, context.getString(R.string.run_notif_distance_label))
                setTextViewText(R.id.run_notif_distance, distance)
                setTextViewText(R.id.run_notif_pace_label, context.getString(R.string.run_notif_pace_label))
                setTextViewText(R.id.run_notif_pace, pace ?: "—")
            }
            builder
                // 글로 읽는 곳(접근성 · 웨어러블)을 위한 같은 내용
                .setContentTitle(context.getString(if (paused) R.string.run_notif_title_paused else R.string.run_notif_title))
                .setContentText(listOfNotNull(distance, pace?.let { context.getString(R.string.run_notif_pace, it) }).joinToString(" · "))
                .setStyle(NotificationCompat.DecoratedCustomViewStyle())
                .setCustomContentView(small)
                .setCustomBigContentView(big)
                .setCustomHeadsUpContentView(small)
                // 전경 서비스 알림은 바탕에 색을 입힐 수 있다 — 남색
                .setColor(NAVY)
                .setColorized(true)
        }
        return builder.build()
    }
}
