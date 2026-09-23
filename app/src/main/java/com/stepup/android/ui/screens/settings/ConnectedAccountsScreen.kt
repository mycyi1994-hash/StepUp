package com.stepup.android.ui.screens.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
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
import androidx.compose.material.icons.filled.AccountBalanceWallet
import androidx.compose.material.icons.filled.Link
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.stepup.android.R
import com.stepup.android.core.ServiceLocator
import com.stepup.android.data.remote.ServerResult
import com.stepup.android.ui.components.quietClickable
import com.stepup.android.ui.theme.Alert
import com.stepup.android.ui.theme.Carbon
import kotlinx.coroutines.launch
import com.stepup.android.ui.components.DarkIconButton
import com.stepup.android.ui.components.Eyebrow
import com.stepup.android.ui.components.GlowCard
import com.stepup.android.ui.components.IconSquare
import com.stepup.android.ui.theme.CarbonHigh
import com.stepup.android.ui.theme.Silver
import com.stepup.android.ui.theme.Slate
import com.stepup.android.ui.theme.Snow
import com.stepup.android.ui.theme.Volt

/**
 * 연결된 계정 — GIWA 지갑 · Health Connect · 소셜 계정 연동 상태.
 *
 * 아직 연결 가능한 항목이 없으므로 행은 클릭되지 않는다.
 * 상태는 칩으로만 표시한다 (Coming soon / Not connected).
 */
@Composable
fun ConnectedAccountsScreen(onBack: () -> Unit = {}) {
    val scope = rememberCoroutineScope()
    var signedIn by remember { mutableStateOf<Boolean?>(null) }
    LaunchedEffect(Unit) { signedIn = ServiceLocator.sessionHolder.isSignedIn() }
    var confirming by remember { mutableStateOf(false) }
    var deleting by remember { mutableStateOf(false) }
    var failed by remember { mutableStateOf(false) }

    if (confirming) {
        AlertDialog(
            onDismissRequest = { if (!deleting) confirming = false },
            containerColor = Carbon,
            titleContentColor = Snow,
            textContentColor = Silver,
            title = { Text(stringResource(R.string.account_delete_title), fontWeight = FontWeight.Black) },
            text = {
                Text(
                    text = stringResource(if (failed) R.string.account_delete_failed else R.string.account_delete_body),
                    style = MaterialTheme.typography.bodyMedium,
                    lineHeight = 20.sp,
                )
            },
            confirmButton = {
                TextButton(
                    enabled = !deleting,
                    onClick = {
                        deleting = true
                        scope.launch {
                            try {
                            val result = ServiceLocator.server.deleteAccount()
                            if (result is ServerResult.Ok) {
                                // 서버 계정이 사라졌다. 이 폰의 로그인도 지우면 첫 화면(로그인)으로 돌아간다.
                                ServiceLocator.sessionHolder.signOut()
                                ServiceLocator.userPrefs.setLoginMethod("")
                                confirming = false
                            } else {
                                failed = true
                            }
                            } catch (cancelled: kotlinx.coroutines.CancellationException) {
                                throw cancelled
                            } catch (_: Exception) {
                                failed = true
                            } finally {
                                deleting = false
                            }
                        }
                    },
                ) {
                    Text(stringResource(R.string.account_delete_confirm), color = Alert, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(enabled = !deleting, onClick = { confirming = false; failed = false }) {
                    Text(stringResource(R.string.common_cancel), color = Silver)
                }
            },
        )
    }

    Column(Modifier.fillMaxSize().padding(horizontal = com.stepup.android.ui.theme.StepUpDesign.Gutter)) {
    com.stepup.android.ui.components.SecondaryHeader(
        onBack = onBack, balance = null, onOpenWallet = null,
        title = stringResource(R.string.settings_connected),
    )
    LazyColumn(
        modifier = Modifier.weight(1f),
        contentPadding = PaddingValues(top = 12.dp, bottom = 22.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        item {
            GlowCard {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(13.dp),
                ) {
                    IconSquare(icon = Icons.Filled.Link, size = 38.dp)
                    Text(
                        text = stringResource(R.string.connected_body),
                        modifier = Modifier.weight(1f),
                        style = MaterialTheme.typography.bodySmall,
                        color = Silver,
                    )
                }
            }
        }

        item {
            AccountRow(
                icon = Icons.Filled.AccountBalanceWallet,
                name = stringResource(R.string.connected_giwa),
                statusText = stringResource(R.string.connected_status_soon),
                statusColor = Volt,
                statusBackground = Volt.copy(alpha = 0.12f),
            )
        }

        item {
            AccountRow(
                icon = Icons.AutoMirrored.Filled.DirectionsWalk,
                name = stringResource(R.string.connected_health),
                statusText = stringResource(R.string.connected_status_soon),
                statusColor = Volt,
                statusBackground = Volt.copy(alpha = 0.12f),
            )
        }

        item {
            AccountRow(
                icon = Icons.Filled.Link,
                name = stringResource(R.string.connected_social),
                statusText = if (signedIn == null) "—" else stringResource(
                    if (signedIn == true) R.string.connected_status_on else R.string.connected_status_off),
                statusColor = Slate,
                statusBackground = CarbonHigh,
                iconTint = Slate,
            )
        }

        // 계정 삭제 — 로그인한 사람에게만. 서버의 기록을 지우고 되돌릴 수 없다.
        if (signedIn == true) {
            item {
                Text(
                    text = stringResource(R.string.account_delete_title),
                    color = Alert,
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 18.dp)
                        .quietClickable { confirming = true }
                        .padding(vertical = 12.dp),
                )
            }
        }
    }
    }
}

/** 계정 한 줄 — 아이콘 배지 + 이름 + 상태 칩. 아직 연결할 수 없어 클릭되지 않는다. */
@Composable
private fun AccountRow(
    icon: ImageVector,
    name: String,
    statusText: String,
    statusColor: Color,
    statusBackground: Color,
    iconTint: Color = Volt,
) {
    GlowCard(
        contentPadding = PaddingValues(horizontal = 15.dp, vertical = 14.dp),
        shape = RoundedCornerShape(20.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(13.dp),
        ) {
            IconSquare(icon = icon, size = 38.dp, tint = iconTint)
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(
                text = name,
                style = MaterialTheme.typography.titleSmall,
                color = Snow,
            )
            StatusChip(
                text = statusText,
                color = statusColor,
                background = statusBackground,
            )
            }
        }
    }
}

/** 상태 pill 칩 — 배경 알약 위 소형 볼드 라벨. */
@Composable
private fun StatusChip(
    text: String,
    color: Color,
    background: Color,
) {
    Box(
        modifier = Modifier
            .background(background, RoundedCornerShape(50))
            .padding(horizontal = 11.dp, vertical = 6.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = text,
            color = color,
            fontSize = 14.sp,
            fontWeight = FontWeight.Bold,
            letterSpacing = 0.3.sp,
        )
    }
}
