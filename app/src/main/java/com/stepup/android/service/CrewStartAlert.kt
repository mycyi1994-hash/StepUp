package com.stepup.android.service

import android.Manifest
import android.app.Activity
import android.app.Application
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.stepup.android.MainActivity
import com.stepup.android.R
import com.stepup.android.data.repo.CrewRepository
import com.stepup.android.data.repo.PartyPhase
import com.stepup.android.data.repo.PartyState
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.launch

/**
 * 앱이 뒤에 있을 때 크루 러닝이 출발했다는 알림(2026-11-04).
 *
 * 안드로이드는 뒤에 있는 앱이 위치 러닝 서비스를 켜지 못하게 한다 — 준비를 마친 사람이 다른 앱에 가 있으면
 * 출발 때 이 폰의 러닝이 켜지지 않았다. 대기실 묻기(CrewRepository.party)는 앱 범위에서 계속 돌므로, 출발(카운트다운 ·
 * 달리는 중)을 보면 알림을 띄운다. 누르면 앱이 앞으로 오고 PartyRunStartHost 가 출발 2분 안이면 러닝을 켠다.
 * 러닝이 켜졌거나 · 앱이 앞으로 왔거나 · 2분이 지났거나 · 방을 떠나면 알림을 거둔다.
 * 전체 화면 알림 권한은 없으므로 높은 중요도의 일반 알림(헤드업)이다.
 */
object CrewStartAlert {

    const val CHANNEL_ID = "crew_start"
    private const val NOTIFICATION_ID = 0x5C2E

    // 처음 쓸 때 만든다 — 판단만 보는 단위 검사가 안드로이드 메인 스레드 없이 이 객체를 읽을 수 있게
    private val scope by lazy { CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate) }
    private val foreground = MutableStateFlow(false)
    private var started = 0

    /** 준비를 누른 방 — 달리는 중의 명단에는 준비 표시가 남지 않을 수 있어 대기실 · 카운트다운에서 본 것을 기억한다 */
    private var readyParty: Long? = null

    /** 지금 알림을 띄운 방 */
    private var shownParty: Long? = null

    /** 앱 켤 때 한 번(StepUpApp) — 앞에 보이는 화면이 있는지 세고, 방 상태 · 러닝 상태를 본다. 따로 묻지 않는다(가벼운 일만) */
    fun attach(app: Application, crew: CrewRepository) {
        app.registerActivityLifecycleCallbacks(object : Application.ActivityLifecycleCallbacks {
            override fun onActivityStarted(activity: Activity) {
                started++
                foreground.value = true
            }

            override fun onActivityStopped(activity: Activity) {
                started = (started - 1).coerceAtLeast(0)
                foreground.value = started > 0
            }

            override fun onActivityCreated(activity: Activity, savedInstanceState: Bundle?) = Unit
            override fun onActivityResumed(activity: Activity) = Unit
            override fun onActivityPaused(activity: Activity) = Unit
            override fun onActivitySaveInstanceState(activity: Activity, outState: Bundle) = Unit
            override fun onActivityDestroyed(activity: Activity) = Unit
        })
        scope.launch {
            combine(crew.party, foreground, WalkSessionService.state) { party, fg, run ->
                Triple(party, fg, run.isActive && run.group != RunGroup.NONE)
            }.distinctUntilChanged().collect { (party, fg, running) -> update(app, party, fg, running) }
        }
    }

    private fun update(context: Context, party: PartyState, foreground: Boolean, running: Boolean) {
        val id = party.partyId
        if (id != null && party.myReady && (party.phase == PartyPhase.LOBBY || party.phase == PartyPhase.COUNTDOWN)) {
            readyParty = id
        } else if (id != null && party.phase == PartyPhase.LOBBY && !party.myReady && readyParty == id) {
            readyParty = null
        }
        val show = shouldAlert(
            phase = party.phase,
            partyId = id,
            readyParty = readyParty,
            startsAtLocal = party.startsAtLocal,
            nowMs = System.currentTimeMillis(),
            foreground = foreground,
            running = running,
        )
        when {
            show && shownParty != id -> post(context, id!!, party.startsAtLocal)
            !show && shownParty != null -> cancel(context)
        }
    }

    /**
     * 알림을 띄울까 — 준비한 방이 출발했고(카운트다운 · 출발 2분 안) 앱이 뒤에 있고 이 폰의 러닝이 아직 꺼져 있을 때.
     * 출발 시각을 아직 모르면(0) 카운트다운으로만 본다.
     */
    internal fun shouldAlert(
        phase: PartyPhase,
        partyId: Long?,
        readyParty: Long?,
        startsAtLocal: Long,
        nowMs: Long,
        foreground: Boolean,
        running: Boolean,
    ): Boolean {
        if (foreground || running || partyId == null || partyId != readyParty) return false
        return when (phase) {
            PartyPhase.COUNTDOWN -> true
            PartyPhase.RUNNING -> startsAtLocal > 0 && nowMs - startsAtLocal <= CrewRepository.LATE_START_MS
            else -> false
        }
    }

    private fun post(context: Context, partyId: Long, startsAtLocal: Long) {
        val allowed = Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU ||
            ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED
        if (!allowed) return
        createChannel(context)
        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val pending = PendingIntent.getActivity(
            context, NOTIFICATION_ID, intent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
        // 출발 2분이 지나면 눌러도 러닝이 켜지지 않는다 — 그때 저절로 사라지게
        val until = (if (startsAtLocal > 0) startsAtLocal else System.currentTimeMillis()) + CrewRepository.LATE_START_MS
        val left = (until - System.currentTimeMillis()).coerceAtLeast(1_000L)
        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_stat_walk)
            .setContentTitle(context.getString(R.string.crew_start_alert_title))
            .setContentText(context.getString(R.string.crew_start_alert_body))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setCategory(NotificationCompat.CATEGORY_EVENT)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .setAutoCancel(true)
            .setTimeoutAfter(left)
            .setContentIntent(pending)
            .build()
        try {
            NotificationManagerCompat.from(context).notify(NOTIFICATION_ID, notification)
        } catch (e: SecurityException) {
            return // 그사이 알림 권한을 거뒀다
        }
        shownParty = partyId
        // 창이 닫히는 때 다시 본다 — 그사이 방 상태가 바뀌지 않아도 알림을 거둔다(위 setTimeoutAfter 와 같이)
        scope.launch {
            delay(left)
            if (shownParty == partyId) cancel(context)
        }
    }

    private fun cancel(context: Context) {
        shownParty = null
        NotificationManagerCompat.from(context).cancel(NOTIFICATION_ID)
    }

    private fun createChannel(context: Context) {
        val manager = context.getSystemService(NotificationManager::class.java) ?: return
        manager.createNotificationChannel(
            NotificationChannel(
                CHANNEL_ID,
                context.getString(R.string.notification_channel_crew_start),
                NotificationManager.IMPORTANCE_HIGH,
            ).apply { lockscreenVisibility = Notification.VISIBILITY_PUBLIC },
        )
    }
}
