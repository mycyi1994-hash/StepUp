package com.stepup.android.ui.screens.settings

import com.stepup.android.data.prefs.NotifyPrefs
import com.stepup.android.core.ServiceLocator
import kotlinx.coroutines.launch
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.collectAsState
import android.widget.Toast
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.DirectionsWalk
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.core.app.NotificationManagerCompat
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.stepup.android.R
import com.stepup.android.ui.components.GlowCard
import com.stepup.android.ui.theme.Silver

/**
 * 알림 설정 — 네 개의 토글.
 *
 * 폰(DataStore)에 저장하고 서버(notify_prefs)에도 올린다. 알림은 서버가
 * 보내므로 서버가 알아야 끈 알림이 오지 않는다.
 */
@Composable
fun NotificationSettingsScreen(onBack: () -> Unit = {}) {
    val context = LocalContext.current
    val savedMessage = stringResource(R.string.pref_saved_on_device)
    val notifySaved = { Toast.makeText(context, savedMessage, Toast.LENGTH_SHORT).show() }

    val storedPrefs by ServiceLocator.userPrefs.notifyPrefs.collectAsState(initial = null)
    val syncState by ServiceLocator.pushRegistrar.preferenceSync.collectAsState()
    val prefs = storedPrefs ?: NotifyPrefs()
    var saving by remember { mutableStateOf(false) }
    val saveFailed = stringResource(R.string.feed_save_failed)
    val scope = rememberCoroutineScope()
    val lifecycleOwner = LocalLifecycleOwner.current
    var notificationsAllowed by remember(context) {
        mutableStateOf(NotificationManagerCompat.from(context).areNotificationsEnabled())
    }
    DisposableEffect(lifecycleOwner, context) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                notificationsAllowed = NotificationManagerCompat.from(context).areNotificationsEnabled()
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }
    fun save(next: NotifyPrefs) {
        if (saving || storedPrefs == null) return
        saving = true
        scope.launch {
            try {
                ServiceLocator.userPrefs.setNotifyPrefs(next)
                ServiceLocator.pushRegistrar.syncPrefsInBackground()
                notifySaved()
            } catch (cancelled: kotlinx.coroutines.CancellationException) {
                throw cancelled
            } catch (_: Exception) {
                Toast.makeText(context, saveFailed, Toast.LENGTH_SHORT).show()
            } finally {
                saving = false
            }
        }
    }

    com.stepup.android.ui.components.DetailPage(
        title = stringResource(R.string.settings_notifications), onBack = onBack,
    ) {
        if (storedPrefs == null) {
            item { Text(stringResource(R.string.feed_loading), color = Silver) }
            return@DetailPage
        }
        if (syncState == com.stepup.android.push.NotificationSyncState.Pending) {
            item {
                GlowCard(contentPadding = PaddingValues(20.dp), spacing = 12.dp) {
                    Text(stringResource(R.string.pref_sync_pending),
                        style = MaterialTheme.typography.bodyMedium, color = Silver)
                    com.stepup.android.ui.components.GhostButton(
                        text = stringResource(R.string.pref_sync_retry),
                        onClick = { ServiceLocator.pushRegistrar.syncPrefsInBackground() },
                        enabled = !saving,
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
            }
        } else if (syncState == com.stepup.android.push.NotificationSyncState.Sending) {
            item {
                Text(stringResource(R.string.pref_sync_sending),
                    style = MaterialTheme.typography.bodyMedium, color = Silver)
            }
        }
        if (prefs.push && !notificationsAllowed) {
            item {
                GlowCard(contentPadding = PaddingValues(20.dp), spacing = 12.dp) {
                    Text(stringResource(R.string.pref_notifications_blocked),
                        style = MaterialTheme.typography.bodyMedium, color = Silver)
                    com.stepup.android.ui.components.GhostButton(
                        text = stringResource(R.string.cd_open_settings),
                        onClick = { com.stepup.android.core.ExternalIntents.openAppSettings(context) },
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
            }
        }
        item {
            ToggleRow(
                enabled = !saving && storedPrefs != null,
                icon = Icons.Filled.Notifications,
                title = stringResource(R.string.pref_push),
                description = stringResource(R.string.pref_push_desc),
                checked = prefs.push,
                onCheckedChange = { save(prefs.copy(push = it)) },
            )
        }

        item {
            ToggleRow(
                enabled = !saving && storedPrefs != null,
                icon = Icons.Filled.Schedule,
                title = stringResource(R.string.pref_goal_reminder),
                description = stringResource(R.string.pref_goal_reminder_desc),
                checked = prefs.goalReminder,
                onCheckedChange = { save(prefs.copy(goalReminder = it)) },
            )
        }

        item {
            ToggleRow(
                enabled = !saving && storedPrefs != null,
                icon = Icons.AutoMirrored.Filled.DirectionsWalk,
                title = stringResource(R.string.pref_party_invite),
                description = stringResource(R.string.pref_party_invite_desc),
                checked = prefs.partyInvite,
                onCheckedChange = { save(prefs.copy(partyInvite = it)) },
            )
        }

        item {
            ToggleRow(
                enabled = !saving && storedPrefs != null,
                icon = Icons.Filled.EmojiEvents,
                title = stringResource(R.string.pref_event_news),
                description = stringResource(R.string.pref_event_news_desc),
                checked = prefs.eventNews,
                onCheckedChange = { save(prefs.copy(eventNews = it)) },
            )
        }
    }
}

/** One shared switch row for every settings page. */
@Composable
private fun ToggleRow(
    icon: ImageVector,
    title: String,
    description: String,
    checked: Boolean,
    enabled: Boolean,
    onCheckedChange: (Boolean) -> Unit,
) {
    com.stepup.android.ui.components.PreferenceToggle(
        title = title, description = description, icon = icon,
        checked = checked, enabled = enabled, onCheckedChange = onCheckedChange,
    )
}
