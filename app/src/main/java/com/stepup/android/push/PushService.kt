package com.stepup.android.push

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import com.stepup.android.MainActivity
import com.stepup.android.R
import com.stepup.android.core.ServiceLocator

/**
 * 서버가 보낸 푸시를 받는다 — 번개·파티런·크루·댓글 소식.
 *
 * 앱이 꺼져 있을 때 온 알림(notification 칸이 있는 메시지)은 안드로이드가
 * 알아서 띄운다. 여기로 오는 것은 앱이 켜져 있을 때 온 알림과, 토큰이 바뀐
 * 소식이다.
 */
class PushService : FirebaseMessagingService() {

    override fun onNewToken(token: String) {
        ServiceLocator.init(applicationContext)
        ServiceLocator.pushRegistrar.syncInBackground(token)
    }

    override fun onMessageReceived(message: RemoteMessage) {
        val title = message.notification?.title ?: message.data["title"] ?: return
        val body = message.notification?.body ?: message.data["body"].orEmpty()
        show(this, title, body, message.data["link"].orEmpty())
    }

    companion object {
        /** 커뮤니티 소식 알림 채널. 러닝 중 알림(걸음 수)과 따로 두어 각각 끌 수 있게 한다. */
        const val CHANNEL_ID = "community"

        fun createChannel(service: android.content.Context) {
            val manager = service.getSystemService(NotificationManager::class.java) ?: return
            manager.createNotificationChannel(
                NotificationChannel(
                    CHANNEL_ID,
                    service.getString(R.string.notification_channel_community),
                    NotificationManager.IMPORTANCE_DEFAULT,
                ),
            )
        }

        private fun show(context: android.content.Context, title: String, body: String, link: String) {
            createChannel(context)
            val intent = Intent(context, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
                if (link.isNotBlank()) putExtra(EXTRA_LINK, link)
            }
            val pending = PendingIntent.getActivity(
                context,
                link.hashCode(),
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
            )
            val notification = NotificationCompat.Builder(context, CHANNEL_ID)
                .setSmallIcon(R.drawable.ic_stat_walk)
                .setContentTitle(title)
                .setContentText(body)
                .setStyle(NotificationCompat.BigTextStyle().bigText(body))
                .setAutoCancel(true)
                .setContentIntent(pending)
                .build()
            // 알림 권한을 거절했으면 조용히 넘어간다. 앱 안 알림 목록에는 그대로 남는다.
            val allowed = Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU ||
                ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) ==
                PackageManager.PERMISSION_GRANTED
            if (allowed) {
                NotificationManagerCompat.from(context).notify((title + body).hashCode(), notification)
            }
        }

        /** 알림을 눌러 들어왔을 때 열 화면. 앱 안 링크 형식(예: crew/<id>) */
        const val EXTRA_LINK = "push_link"
    }
}
