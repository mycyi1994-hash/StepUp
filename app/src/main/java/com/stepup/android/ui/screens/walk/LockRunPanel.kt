package com.stepup.android.ui.screens.walk

import android.app.Activity
import android.app.KeyguardManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.ContextWrapper
import android.content.Intent
import android.content.IntentFilter
import android.os.Build
import android.view.WindowManager
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.repeatOnLifecycle
import com.stepup.android.R
import com.stepup.android.domain.RunNotificationText
import com.stepup.android.service.RunNotification
import com.stepup.android.service.WalkSessionService
import com.stepup.android.service.WalkSessionState
import com.stepup.android.ui.components.PrimaryButton
import com.stepup.android.ui.components.RunStartWordmark
import com.stepup.android.ui.components.StepUpUi
import com.stepup.android.ui.components.pressable
import kotlinx.coroutines.delay

/**
 * 잠금 화면(홀드 화면) 가운데의 러닝 현황(2026-10-26 사용자 "홀드화면(잠금화면) 중앙에 현재 현황이 딱 뜨는 그런 거").
 *
 * 달리는 동안에는 어느 화면에 있든 앱이 잠금 화면 위에 뜰 수 있게 두고(`setShowWhenLocked`), 기기가 잠겨 있으면 앱 화면을 덮어
 * 이 현황만 보인다 — 잠금을 풀지 않고 지갑 · 설정 같은 앱 화면을 만질 수 없다. 화면이 꺼질 때 미리 덮어 두어 켜는 순간 앱 화면이 비치지 않는다.
 * 잠금을 풀면(지문 · "잠금 해제하고 앱 열기") 덮개가 걷히고 보던 화면 그대로다. 뒤로 가기는 잠금 화면으로 돌아간다.
 */
@Composable
fun LockRunHost(content: @Composable () -> Unit) {
    val context = LocalContext.current
    val activity = remember(context) { context.hostActivity() }
    val session by WalkSessionService.state.collectAsStateWithLifecycle()
    val running = session.isActive
    DisposableEffect(activity, running) {
        activity?.showOverLockScreen(running)
        onDispose { activity?.showOverLockScreen(false) }
    }
    val locked = rememberDeviceLocked(watch = running)
    val cover = running && locked
    Box(Modifier.fillMaxSize()) {
        // 덮여 있는 동안 앱 화면은 화면 읽기에서도 빠진다
        Box(if (cover) Modifier.fillMaxSize().clearAndSetSemantics { } else Modifier.fillMaxSize()) { content() }
        if (cover) {
            BackHandler { activity?.moveTaskToBack(true) }
            LockRunPanel(
                state = session,
                onToggle = { if (session.isPaused) WalkSessionService.resume(context) else WalkSessionService.pause(context) },
                onOpen = { activity?.dismissLock {} },
            )
        }
    }
}

/**
 * 기기가 잠겨 있는가 — [watch] 동안만 본다. 화면이 꺼지면 바로 잠긴 것으로 두고(켜는 순간 앱 화면이 비치지 않게),
 * 화면이 켜져 앱이 앞에 있는 동안에는 0.5초마다 잠금 상태를 다시 읽는다(지문으로 풀었거나 잠금이 늦게 걸리는 기기).
 */
@Composable
internal fun rememberDeviceLocked(watch: Boolean): Boolean {
    val context = LocalContext.current
    val keyguard = remember(context) { context.getSystemService(KeyguardManager::class.java) }
    var locked by remember { mutableStateOf(watch && keyguard?.isKeyguardLocked == true) }
    DisposableEffect(context, watch) {
        if (!watch) {
            locked = false
            return@DisposableEffect onDispose { }
        }
        locked = keyguard?.isKeyguardLocked == true
        val receiver = object : BroadcastReceiver() {
            override fun onReceive(c: Context, intent: Intent) {
                locked = intent.action == Intent.ACTION_SCREEN_OFF || keyguard?.isKeyguardLocked == true
            }
        }
        val filter = IntentFilter().apply {
            addAction(Intent.ACTION_SCREEN_OFF)
            addAction(Intent.ACTION_SCREEN_ON)
            addAction(Intent.ACTION_USER_PRESENT)
        }
        ContextCompat.registerReceiver(context, receiver, filter, ContextCompat.RECEIVER_NOT_EXPORTED)
        onDispose { runCatching { context.unregisterReceiver(receiver) } }
    }
    val lifecycle = LocalLifecycleOwner.current.lifecycle
    LaunchedEffect(lifecycle, watch) {
        if (!watch) return@LaunchedEffect
        // 화면이 꺼진 동안(멈춤)에는 읽지 않는다 — 잠금이 몇 초 늦게 걸리는 기기에서 덮개를 미리 걷지 않게
        lifecycle.repeatOnLifecycle(Lifecycle.State.RESUMED) {
            while (true) {
                locked = keyguard?.isKeyguardLocked == true
                delay(LOCK_POLL_MS)
            }
        }
    }
    return watch && locked
}

private const val LOCK_POLL_MS = 500L

/** 잠금 화면 가운데의 러닝 현황 — 검정 바탕, 큰 시간, 거리 · 페이스, 주 버튼 하나(일시정지 / 다시 달리기). 마치기는 앱에서만 */
@Composable
internal fun LockRunPanel(state: WalkSessionState, onToggle: () -> Unit, onOpen: () -> Unit) {
    val context = LocalContext.current
    val type = StepUpUi.type
    val paused = state.isPaused
    val km = state.gpsKm
    val pace = RunNotificationText.pace(state.elapsedSec, km)
    Box(
        Modifier
            .fillMaxSize()
            .background(Color.Black)
            // 아래 앱 화면으로 누름이 새지 않게 모두 받는다
            .pointerInput(Unit) { awaitPointerEventScope { while (true) awaitPointerEvent() } }
            .safeDrawingPadding()
            .padding(horizontal = 24.dp),
    ) {
        RunStartWordmark(width = 116.dp, modifier = Modifier.align(Alignment.TopCenter).padding(top = 20.dp))
        Column(
            Modifier.align(Alignment.Center).fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(
                stringResource(if (paused) R.string.run_notif_title_paused else R.string.run_notif_title),
                style = type.rowTitle, color = if (paused) LockMuted else LockAccent,
                modifier = Modifier.semantics { heading() },
            )
            Spacer(Modifier.height(6.dp))
            Text(
                RunNotificationText.clock(state.elapsedSec),
                style = type.numberLarge, color = Color.White, maxLines = 1, textAlign = TextAlign.Center,
            )
            Spacer(Modifier.height(20.dp))
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
                LockStat(stringResource(R.string.run_notif_distance_label), RunNotificationText.distance(km), "km")
                LockStat(stringResource(R.string.run_notif_pace_label), pace ?: "—", null)
            }
            Spacer(Modifier.height(20.dp))
            Text(
                RunNotification.status(context, state),
                style = type.caption, color = LockMuted, textAlign = TextAlign.Center, maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
        }
        Column(
            Modifier.align(Alignment.BottomCenter).fillMaxWidth().padding(bottom = 16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            PrimaryButton(
                text = stringResource(if (paused) R.string.run_notif_resume else R.string.run_notif_pause),
                onClick = onToggle, arrow = false, modifier = Modifier.fillMaxWidth(),
            )
            Spacer(Modifier.height(8.dp))
            Box(
                Modifier.heightIn(min = 48.dp).pressable(onClick = onOpen).padding(horizontal = 16.dp),
                contentAlignment = Alignment.Center,
            ) {
                Text(stringResource(R.string.lock_run_open), style = type.smallButton, color = LockMuted)
            }
        }
    }
}

@Composable
private fun LockStat(label: String, value: String, unit: String?) {
    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.width(140.dp)) {
        Text(label, style = StepUpUi.type.caption, color = LockMuted)
        Row(verticalAlignment = Alignment.Bottom) {
            Text(value, style = StepUpUi.type.numberStat.copy(fontSize = 40.sp), color = Color.White, maxLines = 1)
            if (unit != null) {
                Spacer(Modifier.width(4.dp))
                Text(unit, style = StepUpUi.type.unit, color = LockMuted, modifier = Modifier.padding(bottom = 6.dp))
            }
        }
    }
}

private val LockAccent = Color(0xFF7DA2FF)
private val LockMuted = Color(0xFFA7AFBC)

private fun Context.hostActivity(): Activity? {
    var current: Context? = this
    while (current is ContextWrapper) {
        if (current is Activity) return current
        current = current.baseContext
    }
    return null
}

/** 잠금 화면 위에 이 화면을 보일지 — 켜면 전원 버튼으로 화면을 켰을 때 잠금을 풀지 않아도 앱(덮개)이 보인다 */
internal fun Activity.showOverLockScreen(show: Boolean) {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O_MR1) {
        setShowWhenLocked(show)
    } else {
        @Suppress("DEPRECATION")
        if (show) window.addFlags(WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED)
        else window.clearFlags(WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED)
    }
}

/** 잠금 해제 화면(지문 · 비밀번호)을 띄운다 — 풀리면 [onUnlocked] */
internal fun Activity.dismissLock(onUnlocked: () -> Unit) {
    val keyguard = getSystemService(KeyguardManager::class.java) ?: return onUnlocked()
    if (!keyguard.isKeyguardLocked) return onUnlocked()
    keyguard.requestDismissKeyguard(this, object : KeyguardManager.KeyguardDismissCallback() {
        override fun onDismissSucceeded() = onUnlocked()
    })
}
