package com.stepup.android.ui.screens.settings

import android.widget.Toast
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.RemoveCircleOutline
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.stepup.android.R
import com.stepup.android.core.ServiceLocator
import com.stepup.android.ui.components.DarkIconButton
import com.stepup.android.ui.components.Eyebrow
import com.stepup.android.ui.components.GhostButton
import com.stepup.android.ui.components.GlowCard
import com.stepup.android.ui.components.HairlineDivider
import com.stepup.android.ui.components.IconSquare
import com.stepup.android.ui.theme.Alert
import com.stepup.android.ui.theme.Silver
import com.stepup.android.ui.theme.Snow
import com.stepup.android.ui.theme.Volt
import kotlinx.coroutines.launch

/**
 * 개인정보 · 보안 — 저장 데이터 안내, 사용 중인 권한, 로컬 데이터 초기화.
 *
 * 초기화 버튼은 알림함만 비운다
 * (걸음 · SUP · 스니커는 유지).
 */
@Composable
fun PrivacyScreen(onBack: () -> Unit = {}) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val resetDoneMessage = stringResource(R.string.privacy_reset_done)
    val lifecycleOwner = LocalLifecycleOwner.current
    var permissions by remember(context) { mutableStateOf(readPermissions(context)) }
    DisposableEffect(lifecycleOwner, context) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) permissions = readPermissions(context)
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    com.stepup.android.ui.components.DetailPage(
        title = stringResource(R.string.settings_privacy), onBack = onBack,
    ) {
        item {
            GlowCard(spacing = 12.dp) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(13.dp),
                ) {
                    IconSquare(icon = Icons.Filled.Shield, size = 38.dp)
                    Text(
                        text = stringResource(R.string.privacy_data_title),
                        style = MaterialTheme.typography.titleSmall,
                        color = Snow,
                    )
                }
                Text(
                    text = stringResource(R.string.privacy_data_body),
                    style = MaterialTheme.typography.bodySmall,
                    color = Silver,
                )
            }
        }

        item {
            GlowCard(spacing = 12.dp) {
                Text(
                    text = stringResource(R.string.privacy_perm_title),
                    style = MaterialTheme.typography.titleSmall,
                    color = Snow,
                )
                PermissionRow(
                    text = stringResource(R.string.privacy_perm_activity),
                    status = stringResource(if (permissions.activity) R.string.privacy_permission_allowed else R.string.privacy_permission_not_allowed),
                    allowed = permissions.activity,
                )
                HairlineDivider()
                PermissionRow(
                    text = stringResource(R.string.privacy_perm_location),
                    status = stringResource(when {
                        permissions.preciseLocation -> R.string.privacy_location_precise
                        permissions.approximateLocation -> R.string.privacy_location_approximate
                        else -> R.string.privacy_permission_not_allowed
                    }),
                    allowed = permissions.preciseLocation || permissions.approximateLocation,
                )
                HairlineDivider()
                PermissionRow(
                    text = stringResource(R.string.privacy_perm_notification),
                    status = stringResource(if (permissions.notifications) R.string.privacy_permission_allowed else R.string.privacy_permission_not_allowed),
                    allowed = permissions.notifications,
                )
                GhostButton(
                    text = stringResource(R.string.cd_open_settings),
                    onClick = { com.stepup.android.core.ExternalIntents.openAppSettings(context) },
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        }

        item {
            GlowCard(spacing = 12.dp) {
                Text(
                    text = stringResource(R.string.privacy_reset_title),
                    style = MaterialTheme.typography.titleSmall,
                    color = Snow,
                )
                Text(
                    text = stringResource(R.string.privacy_reset_body),
                    style = MaterialTheme.typography.bodySmall,
                    color = Silver,
                )
                GhostButton(
                    text = stringResource(R.string.privacy_reset_button),
                    onClick = {
                        scope.launch {
                            ServiceLocator.notificationRepository.clearAll()
                            Toast.makeText(context, resetDoneMessage, Toast.LENGTH_SHORT).show()
                        }
                    },
                    accent = Alert,
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        }
    }
}

/** 볼트 체크 + 권한 설명 한 줄. */
@Composable
private fun PermissionRow(text: String, status: String, allowed: Boolean) {
    Row(
        modifier = Modifier.fillMaxWidth().semantics(mergeDescendants = true) {},
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Icon(
            imageVector = if (allowed) Icons.Filled.CheckCircle else Icons.Filled.RemoveCircleOutline,
            contentDescription = null,
            tint = if (allowed) com.stepup.android.ui.theme.VoltText else Silver,
            modifier = Modifier.size(16.dp),
        )
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(text = text, style = MaterialTheme.typography.bodyMedium, color = Snow)
            Text(text = status, style = MaterialTheme.typography.bodySmall, color = Silver)
        }
    }
}

private data class PermissionSnapshot(
    val activity: Boolean, val preciseLocation: Boolean,
    val approximateLocation: Boolean, val notifications: Boolean,
)

private fun readPermissions(context: android.content.Context): PermissionSnapshot {
    fun granted(permission: String) = androidx.core.content.ContextCompat.checkSelfPermission(context, permission) ==
        android.content.pm.PackageManager.PERMISSION_GRANTED
    return PermissionSnapshot(
        activity = com.stepup.android.ui.StepPermissions.hasActivityRecognition(context),
        preciseLocation = granted(android.Manifest.permission.ACCESS_FINE_LOCATION),
        approximateLocation = granted(android.Manifest.permission.ACCESS_COARSE_LOCATION),
        notifications = androidx.core.app.NotificationManagerCompat.from(context).areNotificationsEnabled(),
    )
}
