package com.stepup.android.ui.screens.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.background
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.stepup.android.R
import com.stepup.android.core.ServiceLocator
import com.stepup.android.ui.components.DetailPage
import com.stepup.android.ui.components.SettingsDangerButton
import com.stepup.android.ui.components.SettingsGroupLabel
import com.stepup.android.ui.components.SettingsNavRow
import com.stepup.android.ui.components.SettingsPrimaryButton
import com.stepup.android.ui.components.SettingsSecondaryButton
import com.stepup.android.ui.components.SettingsSheet
import com.stepup.android.ui.components.SettingsStatusRow
import com.stepup.android.ui.components.SettingsToast
import com.stepup.android.ui.components.settingsPalette
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/** 휴대폰이 이 앱에 준 권한 — 앱 안의 스위치가 아니라 읽기만 한다 */
data class PermissionSnapshot(
    val activity: Boolean,
    val preciseLocation: Boolean,
    val approximateLocation: Boolean,
    val notifications: Boolean,
)

/** 알림 기록 지우기 — 확인 창 · 지우는 중 · 못 지움 */
enum class ClearStep { Closed, Confirm, Clearing, Failed }

/**
 * 개인정보 · 앱 권한(설정 v1 06~10 · 27).
 *
 * 권한 줄은 휴대폰의 실제 상태(신체 활동 · 위치 정확/대략/안 됨 · 알림)를 읽어 글자로 보인다 — 앱 안에서 바꿀 수 있는
 * 것처럼 보이지 않고, 바꾸는 길은 "휴대폰 설정 열기" 하나다. 돌아오면 다시 읽는다.
 *
 * 알림 기록 지우기는 기존 그대로 이 휴대폰의 일반 알림만 지운다(notificationRepository.clearAll —
 * 답하지 않은 초대 · 미확인 보상 알림은 남긴다). 러닝 · SUP · 신발 · 서버 계정 · 권한은 건드리지 않는다.
 * 확인 창에서 그 범위를 먼저 알리고, 지우는 동안 다시 누를 수 없으며, 실제로 지운 뒤에만 "지웠어요"를 띄운다.
 */
@Composable
fun PrivacyScreen(onBack: () -> Unit = {}) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val lifecycleOwner = LocalLifecycleOwner.current
    var permissions by remember(context) { mutableStateOf(readPermissions(context)) }
    DisposableEffect(lifecycleOwner, context) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) permissions = readPermissions(context)
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }
    var clear by remember { mutableStateOf(ClearStep.Closed) }
    var storedOpen by remember { mutableStateOf(false) }
    var toast by remember { mutableStateOf<String?>(null) }
    val cleared = stringResource(R.string.set_cleared)
    LaunchedEffect(toast) {
        if (toast != null) {
            delay(2_500)
            toast = null
        }
    }

    fun runClear() {
        if (clear == ClearStep.Clearing) return
        clear = ClearStep.Clearing
        scope.launch {
            clear = try {
                ServiceLocator.notificationRepository.clearAll()
                toast = cleared
                ClearStep.Closed
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (_: Exception) {
                ClearStep.Failed
            }
        }
    }

    PrivacyContent(
        permissions = permissions, clear = clear, storedOpen = storedOpen, toast = toast,
        onBack = onBack,
        onOpenStored = { storedOpen = true }, onCloseStored = { storedOpen = false },
        onOpenPhoneSettings = { com.stepup.android.core.ExternalIntents.openAppSettings(context) },
        onAskClear = { clear = ClearStep.Confirm }, onClear = ::runClear,
        onCloseClear = { if (clear != ClearStep.Clearing) clear = ClearStep.Closed },
    )
}

@Composable
fun PrivacyContent(
    permissions: PermissionSnapshot,
    clear: ClearStep = ClearStep.Closed,
    storedOpen: Boolean = false,
    toast: String? = null,
    onBack: () -> Unit = {},
    onOpenStored: () -> Unit = {},
    onCloseStored: () -> Unit = {},
    onOpenPhoneSettings: () -> Unit = {},
    onAskClear: () -> Unit = {},
    onClear: () -> Unit = {},
    onCloseClear: () -> Unit = {},
) {
    Box(Modifier.fillMaxSize()) {
        DetailPage(title = stringResource(R.string.set_privacy), onBack = onBack) {
            item {
                SettingsNavRow(
                    stringResource(R.string.set_stored_info), onClick = onOpenStored,
                    description = stringResource(R.string.set_stored_info_desc), modifier = Modifier.testTag("privacy-stored"),
                )
            }
            item {
                Column(Modifier.fillMaxWidth()) {
                    SettingsGroupLabel(stringResource(R.string.set_group_permissions))
                    SettingsStatusRow(
                        stringResource(R.string.set_perm_activity), stringResource(R.string.set_perm_activity_desc),
                        stringResource(if (permissions.activity) R.string.set_perm_allowed else R.string.set_perm_denied),
                        Modifier.testTag("perm-activity"), strong = permissions.activity,
                    )
                    val location = when {
                        permissions.preciseLocation -> R.string.set_perm_precise
                        permissions.approximateLocation -> R.string.set_perm_approximate
                        else -> R.string.set_perm_denied
                    }
                    SettingsStatusRow(
                        stringResource(R.string.set_perm_location), stringResource(R.string.set_perm_location_desc),
                        stringResource(location), Modifier.testTag("perm-location"),
                        strong = permissions.preciseLocation || permissions.approximateLocation,
                    )
                    SettingsStatusRow(
                        stringResource(R.string.set_perm_notifications), stringResource(R.string.set_perm_notifications_desc),
                        stringResource(if (permissions.notifications) R.string.set_perm_allowed else R.string.set_perm_denied),
                        Modifier.testTag("perm-notifications"), strong = permissions.notifications,
                    )
                }
            }
            item {
                SettingsPrimaryButton(
                    stringResource(R.string.set_open_phone_settings), onOpenPhoneSettings,
                    Modifier.fillMaxWidth().padding(top = 10.dp).testTag("privacy-open-settings"),
                )
            }
            item {
                SettingsNavRow(
                    stringResource(R.string.set_clear_history), onClick = onAskClear,
                    description = stringResource(R.string.set_clear_history_desc),
                    modifier = Modifier.padding(top = 18.dp).testTag("privacy-clear"),
                )
            }
        }
        SettingsToast(toast, Modifier.align(Alignment.BottomCenter).padding(horizontal = 24.dp, vertical = 16.dp))
    }

    if (storedOpen) StoredInfoSheet(onCloseStored)
    if (clear != ClearStep.Closed) ClearHistorySheet(clear, onClear, onCloseClear)
}

/** 저장하는 정보(08) — 보관 범위의 짧은 안내. 새 정책을 정하지 않고, 기존 안내(보관 이유 · 광고 식별자)를 아래에 둔다 */
@Composable
private fun StoredInfoSheet(onDismiss: () -> Unit) {
    val p = settingsPalette()
    SettingsSheet(
        title = stringResource(R.string.set_stored_info), onDismiss = onDismiss, modifier = Modifier.testTag("stored-sheet"),
        actions = { SettingsPrimaryButton(stringResource(R.string.set_ok), onDismiss, Modifier.fillMaxWidth()) },
    ) {
        Text(stringResource(R.string.set_stored_heading), color = p.text, fontSize = 19.sp, fontWeight = FontWeight.SemiBold)
        Text(stringResource(R.string.set_stored_body), color = p.secondary, fontSize = 15.sp, lineHeight = 1.55.em)
        Text(stringResource(R.string.privacy_data_body), color = p.secondary, fontSize = 13.sp, lineHeight = 1.5.em)
        Box(Modifier.fillMaxWidth().height(1.dp).background(p.divider))
        Text(stringResource(R.string.set_stored_delete_note), color = p.secondary, fontSize = 14.sp)
        Text(stringResource(R.string.set_stored_clear_note), color = p.secondary, fontSize = 14.sp)
    }
}

/** 알림 기록 지우기 확인(09) · 지우는 중 · 못 지움(27). 지우는 동안에는 닫을 수 없다 */
@Composable
private fun ClearHistorySheet(step: ClearStep, onClear: () -> Unit, onDismiss: () -> Unit) {
    val p = settingsPalette()
    val failed = step == ClearStep.Failed
    val clearing = step == ClearStep.Clearing
    SettingsSheet(
        title = stringResource(if (failed) R.string.set_clear_failed else R.string.set_clear_title),
        onDismiss = onDismiss, dismissible = !clearing, showClose = false,
        modifier = Modifier.testTag(if (failed) "clear-failed-sheet" else "clear-sheet"),
        actions = {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                SettingsSecondaryButton(stringResource(R.string.set_cancel), onDismiss, Modifier.weight(1f), enabled = !clearing)
                SettingsDangerButton(
                    stringResource(
                        when {
                            clearing -> R.string.set_clearing
                            failed -> R.string.set_clear_again
                            else -> R.string.set_clear_action
                        },
                    ),
                    onClear, Modifier.weight(1f).testTag("clear-confirm"), loading = clearing,
                )
            }
        },
    ) {
        Text(stringResource(R.string.set_clear_heading), color = p.text, fontSize = 17.sp, fontWeight = FontWeight.SemiBold)
        Text(stringResource(if (failed) R.string.set_clear_failed_body else R.string.set_clear_cannot_undo),
            color = p.secondary, fontSize = 15.sp)
        Text(stringResource(R.string.set_clear_keeps_data), color = p.secondary, fontSize = 15.sp)
        Text(stringResource(R.string.set_clear_keeps_pending), color = p.secondary, fontSize = 15.sp,
            modifier = Modifier.padding(top = 4.dp))
    }
}

internal fun readPermissions(context: android.content.Context): PermissionSnapshot {
    fun granted(permission: String) = androidx.core.content.ContextCompat.checkSelfPermission(context, permission) ==
        android.content.pm.PackageManager.PERMISSION_GRANTED
    return PermissionSnapshot(
        activity = com.stepup.android.ui.StepPermissions.hasActivityRecognition(context),
        preciseLocation = granted(android.Manifest.permission.ACCESS_FINE_LOCATION),
        approximateLocation = granted(android.Manifest.permission.ACCESS_COARSE_LOCATION),
        notifications = androidx.core.app.NotificationManagerCompat.from(context).areNotificationsEnabled(),
    )
}
