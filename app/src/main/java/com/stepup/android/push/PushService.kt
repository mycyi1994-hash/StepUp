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
        // 로그아웃한 폰에는 띄우지 않는다 — 떼기 전에 보낸 앞 계정의 소식이 늦게 와도(일꾼 스레드라 잠깐 기다려도 된다)
        ServiceLocator.init(applicationContext)
        val signedIn = kotlinx.coroutines.runBlocking {
            kotlinx.coroutines.withTimeoutOrNull(2_000) {
                runCatching { ServiceLocator.sessionHolder.isSignedIn() }.getOrDefault(true)
            }
        }
        if (signedIn == false) return
        val title = message.notification?.title ?: message.data["title"] ?: return
        val body = message.notification?.body ?: message.data["body"].orEmpty()
        val link = message.data["link"].orEmpty()
        // 지금 화면에 보이는 크루 채팅 방의 새 메시지는 띄우지 않는다(방이 이미 따라오고 있다)
        val visible = com.stepup.android.ui.screens.community.chat.ChatVisibility.crewId
        if (visible != null && link == "crew-chat/$visible") return
        // 이 폰이 지금 들어가 있는(달리고 있는) 방의 출발 소식은 앱이 앞에 있으면 띄우지 않는다 — 화면이 이미 출발을 보인다(서버 0117)
        if (shownByTheRoom(link, message.data["kind"])) return
        show(this, title, body, link)
    }

    /**
     * 함께 뛰기 방의 출발 푸시(PARTY_STARTED · PARTY_MISSED, 서버 0117 — 링크 `room/<크루>/<방>` · `flash/<글>`)를 앱 화면이 대신 알리는가.
     * 앱이 앞에 있고 이 폰이 그 방에 들어가 있을 때만: 방을 바로 다시 물어(출발 연출 · 러닝 켜기 · "함께 출발하지 못했어요"는 화면이 한다)
     * 알림은 띄우지 않는다. 어떤 소식인지는 푸시의 kind 로 가른다 — 번개 글 링크는 그 글의 댓글 알림도 같이 쓴다(2026-11-18 검토:
     * 링크만 보면 달리는 동안 그 글의 댓글 알림까지 숨겼다). kind 가 없으면(예전 push-send) 크루 방 링크만 가린다.
     */
    private fun shownByTheRoom(link: String, kind: String?): Boolean {
        if (link.isBlank() || !com.stepup.android.service.CrewStartAlert.appInForeground) return false
        val startPush = kind == KIND_STARTED || kind == KIND_MISSED
        if (kind != null && !startPush) return false
        val repo = runCatching { ServiceLocator.crewRepository }.getOrNull() ?: return false
        val party = repo.party.value
        // 이미 "함께 출발하지 못했어요"를 화면이 보이고 있다(방 묻기가 푸시보다 먼저 닿았다)
        if (kind == KIND_MISSED && leftBehindFor(link, party)) return true
        return when (com.stepup.android.data.repo.partyPush(link, party)) {
            com.stepup.android.data.repo.PartyPush.OTHER -> false
            com.stepup.android.data.repo.PartyPush.MINE ->
                if (!startPush && link.startsWith("flash/")) {
                    false
                } else {
                    repo.wakeParty()
                    true
                }
            // 이 폰이 그 번개 글의 대기실에 있다 — 출발 소식이면 방을 바로 다시 물어 화면이 알린다
            com.stepup.android.data.repo.PartyPush.CHECK ->
                if (startPush) {
                    repo.wakeParty()
                    true
                } else {
                    false
                }
        }
    }

    /** 이 폰이 그 방(크루) · 그 번개 글에서 "함께 출발하지 못했어요"를 보이고 있다 */
    private fun leftBehindFor(link: String, party: com.stepup.android.data.repo.PartyState): Boolean {
        if (party.problem != com.stepup.android.data.repo.PartyProblem.LEFT_BEHIND) return false
        val parts = link.trim('/').split('/')
        return when {
            parts.size == 3 && parts[0] == "room" -> party.crewId == parts[1]
            parts.size == 2 && parts[0] == "flash" -> party.flashPostId != null && party.flashPostId == parts[1].toLongOrNull()
            else -> false
        }
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
                // 크루 채팅은 방마다 알림 하나(새 메시지가 오면 바꿔 단다) — 쌓이지 않게
                val id = if (link.startsWith("crew-chat/")) link.hashCode() else (title + body).hashCode()
                NotificationManagerCompat.from(context).notify(id, notification)
            }
        }

        /** 알림을 눌러 들어왔을 때 열 화면. 앱 안 링크 형식(예: crew/<id>) */
        const val EXTRA_LINK = "push_link"

        /** 함께 뛰기 출발 푸시의 kind(서버 0117 · push-send) */
        private const val KIND_STARTED = "PARTY_STARTED"
        private const val KIND_MISSED = "PARTY_MISSED"
    }
}
