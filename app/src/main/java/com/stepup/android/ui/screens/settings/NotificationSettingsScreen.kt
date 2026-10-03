package com.stepup.android.ui.screens.settings

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.core.app.NotificationManagerCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.stepup.android.R
import com.stepup.android.core.ServiceLocator
import com.stepup.android.data.prefs.NotifyPrefs
import com.stepup.android.push.NotificationSyncState
import com.stepup.android.ui.components.BlueAlertIcon
import com.stepup.android.ui.components.BlueGroup
import com.stepup.android.ui.components.BlueLoadFailed
import com.stepup.android.ui.components.BlueNote
import com.stepup.android.ui.components.BlueNotice
import com.stepup.android.ui.components.BlueNoticeAction
import com.stepup.android.ui.components.BluePage
import com.stepup.android.ui.components.BlueSkeleton
import com.stepup.android.ui.components.BlueSwitchRow
import com.stepup.android.ui.components.BlueToast
import com.stepup.android.ui.components.RunSpinner
import com.stepup.android.ui.components.runTextStyle
import com.stepup.android.ui.components.runTone
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Text
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch

/** 설정 값을 읽는 중 · 읽음 · 못 읽음. 읽기 전에는 임시 기본 토글을 실제 값처럼 보이지 않는다 */
sealed interface SettingsLoad<out T> {
    data object Loading : SettingsLoad<Nothing>
    data class Ready<T>(val value: T) : SettingsLoad<T>
    data object Failed : SettingsLoad<Nothing>
}

/** 알림 설정 화면이 보일 것(설정 v1 02~05 · 25 · 26) — 기기 테스트는 이것을 바로 넣어 장면을 찍는다 */
data class NotificationSettingsUi(
    val load: SettingsLoad<NotifyPrefs>,
    /** 휴대폰(OS)에서 이 앱의 알림을 막았다 — 앱의 스위치와 따로 본다 */
    val osBlocked: Boolean = false,
    val sync: NotificationSyncState = NotificationSyncState.Idle,
    val saving: Boolean = false,
    /** 휴대폰에 저장하지 못했다 — 스위치는 저장된 값 그대로 */
    val saveFailed: Boolean = false,
    val toast: String? = null,
)

/**
 * 알림 설정 — 기존 네 가지(push · goalReminder · partyInvite · eventNews). 바꾸면 바로 저장한다.
 *
 * 1) 휴대폰(DataStore)에 저장되면 "이 휴대폰에 저장했어요" 2) 기존 PushRegistrar 로 서버에 반영한다 — 반영을 확인하지
 * 못하면 "휴대폰에는 저장했어요" 안내를 두고 다시 반영할 수 있다(서버 성공으로 보이지 않는다) 3) 휴대폰에 저장하지 못하면
 * 스위치는 저장된 값 그대로 두고 알린다. 저장 중에는 다시 누를 수 없다. 휴대폰(OS)에서 알림을 막은 것은 앱 스위치와 따로
 * 알리고, 앱 스위치를 대신 끄지 않는다 — 휴대폰 설정에서 돌아오면 다시 읽는다.
 */
@Composable
fun NotificationSettingsScreen(onBack: () -> Unit = {}) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var attempt by remember { mutableIntStateOf(0) }
    val load by remember(attempt) {
        ServiceLocator.userPrefs.notifyPrefs
            .map<NotifyPrefs, SettingsLoad<NotifyPrefs>> { SettingsLoad.Ready(it) }
            .catch { emit(SettingsLoad.Failed) }
    }.collectAsState(initial = SettingsLoad.Loading)
    val sync by ServiceLocator.pushRegistrar.preferenceSync.collectAsState()
    val osBlocked = !rememberNotificationsAllowed()
    var saving by remember { mutableStateOf(false) }
    var saveFailed by remember { mutableStateOf(false) }
    var toast by remember { mutableStateOf<String?>(null) }
    val savedMessage = stringResource(R.string.set_saved_on_phone)
    LaunchedEffect(toast) {
        if (toast != null) {
            delay(2_500)
            toast = null
        }
    }

    fun save(next: NotifyPrefs) {
        if (saving || load !is SettingsLoad.Ready) return
        saving = true
        scope.launch {
            try {
                ServiceLocator.userPrefs.setNotifyPrefs(next)
                saveFailed = false
                toast = savedMessage
                ServiceLocator.pushRegistrar.syncPrefsInBackground()
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (_: Exception) {
                saveFailed = true
            } finally {
                saving = false
            }
        }
    }

    NotificationSettingsContent(
        ui = NotificationSettingsUi(load, osBlocked, sync, saving, saveFailed, toast),
        onBack = onBack,
        onChange = ::save,
        onOpenPhoneSettings = { com.stepup.android.core.ExternalIntents.openAppSettings(context) },
        onRetrySync = { ServiceLocator.pushRegistrar.syncPrefsInBackground() },
        onReload = { attempt++ },
    )
}

@Composable
fun NotificationSettingsContent(
    ui: NotificationSettingsUi,
    onBack: () -> Unit = {},
    onChange: (NotifyPrefs) -> Unit = {},
    onOpenPhoneSettings: () -> Unit = {},
    onRetrySync: () -> Unit = {},
    onReload: () -> Unit = {},
) {
    Box(Modifier.fillMaxSize()) {
        BluePage(title = stringResource(R.string.set_notifications), onBack = onBack) {
            when (val load = ui.load) {
                // SET25 — 모르는 토글 값을 OFF 나 예시로 보이지 않는다
                SettingsLoad.Loading -> {
                    item { BlueNote(stringResource(R.string.set_loading), intro = true) }
                    item { BlueSkeleton(4, Modifier.testTag("settings-loading")) }
                }
                // SET26 — 빈 설정으로 덮어쓰지 않는다
                SettingsLoad.Failed -> item {
                    BlueLoadFailed(
                        stringResource(R.string.set_load_failed), stringResource(R.string.set_load_failed_body),
                        stringResource(R.string.set_reload), onReload, Modifier.testTag("settings-load-failed"),
                    )
                }
                is SettingsLoad.Ready -> {
                    val prefs = load.value
                    val anyOn = prefs.push || prefs.goalReminder || prefs.partyInvite || prefs.eventNews
                    when {
                        // SET03 — 휴대폰(OS) 차단이 서버 반영 안내보다 먼저다(앱 스위치를 대신 끄지 않는다)
                        ui.osBlocked && anyOn -> item {
                            BlueNotice(
                                stringResource(R.string.set_notif_blocked), stringResource(R.string.set_notif_blocked_body),
                                Modifier.testTag("notif-blocked"),
                                actionLabel = stringResource(R.string.set_open_phone_settings), onAction = onOpenPhoneSettings,
                                actionStyle = BlueNoticeAction.Primary,
                            )
                        }
                        // SET04 · SET37 — 이 휴대폰 저장과 서버 반영을 나눈다. 보내는 중엔 다시 반영만 막는다
                        ui.sync == NotificationSyncState.Pending || ui.sync == NotificationSyncState.Sending -> item {
                            val sending = ui.sync == NotificationSyncState.Sending
                            BlueNotice(
                                stringResource(R.string.set_sync_pending), stringResource(R.string.set_sync_pending_body),
                                Modifier.testTag("notif-sync-pending"),
                                actionLabel = stringResource(if (sending) R.string.set_sync_sending else R.string.set_sync_retry),
                                onAction = onRetrySync, actionEnabled = !sending && !ui.saving, actionTag = "notif-sync-retry",
                            )
                        }
                        else -> item { BlueNote(stringResource(R.string.set_notif_intro), intro = true) }
                    }
                    item {
                        // 네 가지는 따로 고른다(푸시가 나머지의 마스터 스위치가 아니다). 이 휴대폰에 저장하는 동안만 잠시 막는다
                        BlueGroup {
                            BlueSwitchRow(
                                stringResource(R.string.set_notif_push), stringResource(R.string.set_notif_push_desc),
                                prefs.push, { onChange(prefs.copy(push = it)) }, Modifier.testTag("notif-push"), !ui.saving,
                            )
                            BlueSwitchRow(
                                stringResource(R.string.set_notif_goal), stringResource(R.string.set_notif_goal_desc),
                                prefs.goalReminder, { onChange(prefs.copy(goalReminder = it)) }, Modifier.testTag("notif-goal"), !ui.saving,
                            )
                            BlueSwitchRow(
                                stringResource(R.string.set_notif_invite), stringResource(R.string.set_notif_invite_desc),
                                prefs.partyInvite, { onChange(prefs.copy(partyInvite = it)) }, Modifier.testTag("notif-invite"), !ui.saving,
                            )
                            BlueSwitchRow(
                                stringResource(R.string.set_notif_event), stringResource(R.string.set_notif_event_desc),
                                prefs.eventNews, { onChange(prefs.copy(eventNews = it)) }, Modifier.testTag("notif-event"), !ui.saving,
                            )
                        }
                    }
                    when {
                        // SET36 — 기존 saving 상태에 진행 한 줄(새 서버 상태가 아니다)
                        ui.saving -> item { SavingLine(stringResource(R.string.set_blue_saving_on_phone)) }
                        // SET05 — 이전 저장값 그대로, 성공 알림 없음
                        ui.saveFailed -> item {
                            BlueNotice(
                                stringResource(R.string.set_save_failed), stringResource(R.string.set_save_failed_body),
                                Modifier.testTag("notif-save-failed"), icon = BlueAlertIcon,
                            )
                        }
                        else -> item { BlueNote(stringResource(R.string.set_saves_right_away)) }
                    }
                }
            }
        }
        BlueToast(ui.toast, Modifier.align(Alignment.BottomCenter).padding(horizontal = 24.dp, vertical = 16.dp))
    }
}

/** 처리 중 한 줄 — 도는 표시 하나와 글(가짜 백분율 없음) */
@Composable
internal fun SavingLine(text: String, modifier: Modifier = Modifier) {
    val t = runTone()
    Row(
        modifier.fillMaxWidth().padding(horizontal = 4.dp).semantics(mergeDescendants = true) { liveRegion = LiveRegionMode.Polite }
            .testTag("settings-saving"),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        RunSpinner(Modifier.size(24.dp))
        Spacer(Modifier.width(12.dp))
        Text(text, style = runTextStyle(15.sp, t.label, FontWeight.Medium))
    }
}

/** 휴대폰(OS)이 이 앱의 알림을 허락했는가 — 휴대폰 설정에서 돌아올 때마다 다시 본다 */
@Composable
internal fun rememberNotificationsAllowed(): Boolean {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    var allowed by remember(context) { mutableStateOf(NotificationManagerCompat.from(context).areNotificationsEnabled()) }
    DisposableEffect(lifecycleOwner, context) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) allowed = NotificationManagerCompat.from(context).areNotificationsEnabled()
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }
    return allowed
}
