package com.stepup.android.ui.screens.settings

import com.stepup.android.data.prefs.NotifyPrefs
import com.stepup.android.core.ServiceLocator
import kotlinx.coroutines.launch
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.collectAsState
import android.widget.Toast
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.DirectionsWalk
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.stepup.android.R
import com.stepup.android.ui.components.DarkIconButton
import com.stepup.android.ui.components.Eyebrow
import com.stepup.android.ui.components.GlowCard
import com.stepup.android.ui.components.IconSquare
import com.stepup.android.ui.theme.CarbonHigh
import com.stepup.android.ui.theme.Edge
import com.stepup.android.ui.theme.OnVolt
import com.stepup.android.ui.theme.Silver
import com.stepup.android.ui.theme.Slate
import com.stepup.android.ui.theme.Snow
import com.stepup.android.ui.theme.Volt

/**
 * 알림 설정 — 네 개의 토글.
 *
 * 폰(DataStore)에 저장하고 서버(notify_prefs)에도 올린다. 알림은 서버가
 * 보내므로 서버가 알아야 끈 알림이 오지 않는다.
 */
@Composable
fun NotificationSettingsScreen(onBack: () -> Unit = {}) {
    val context = LocalContext.current
    val savedMessage = stringResource(R.string.pref_saved)
    val notifySaved = { Toast.makeText(context, savedMessage, Toast.LENGTH_SHORT).show() }

    val storedPrefs by ServiceLocator.userPrefs.notifyPrefs.collectAsState(initial = null)
    val prefs = storedPrefs ?: NotifyPrefs()
    var saving by remember { mutableStateOf(false) }
    val saveFailed = stringResource(R.string.feed_save_failed)
    val scope = rememberCoroutineScope()
    fun save(next: NotifyPrefs) {
        if (saving || storedPrefs == null) return
        saving = true
        scope.launch {
            try {
                ServiceLocator.userPrefs.setNotifyPrefs(next)
                ServiceLocator.pushRegistrar.syncPrefsInBackground(next)
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

/** 아이콘 · 제목 · 설명 + 볼트 스위치 한 줄. */
@Composable
private fun ToggleRow(
    icon: ImageVector,
    title: String,
    description: String,
    checked: Boolean,
    enabled: Boolean,
    onCheckedChange: (Boolean) -> Unit,
) {
    GlowCard(
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 15.dp),
        spacing = 0.dp,
        shape = RoundedCornerShape(22.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(13.dp),
        ) {
            IconSquare(icon = icon, size = 38.dp, tint = if (checked) Volt else Slate)
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(3.dp),
            ) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.bodyLarge,
                    color = Snow,
                )
                Text(
                    text = description,
                    style = MaterialTheme.typography.bodySmall,
                    color = Silver,
                )
            }
            Switch(
                enabled = enabled,
                checked = checked,
                onCheckedChange = onCheckedChange,
                colors = SwitchDefaults.colors(
                    checkedThumbColor = OnVolt,
                    checkedTrackColor = Volt,
                    uncheckedThumbColor = Slate,
                    uncheckedTrackColor = CarbonHigh,
                    checkedBorderColor = Volt,
                    uncheckedBorderColor = Edge,
                ),
            )
        }
    }
}
