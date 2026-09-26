package com.stepup.android.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.stepup.android.R
import com.stepup.android.core.TestUpdates
import com.stepup.android.ui.theme.Carbon
import com.stepup.android.ui.theme.Silver
import com.stepup.android.ui.theme.Snow
import com.stepup.android.ui.theme.StepUpDesign
import com.stepup.android.ui.theme.VoltText

/**
 * 테스트 APK 새 버전 알림 창. 앱을 켤 때 한 번 묻고, 더 새 빌드가 있을 때만 뜬다.
 * "업데이트"를 누르면 받아서 안드로이드 설치 화면을 연다 — 데이터는 그대로 남는다.
 */
@Composable
fun TestUpdatePrompt() {
    if (!TestUpdates.enabled) return
    val context = LocalContext.current
    val state by TestUpdates.state.collectAsState()
    LaunchedEffect(Unit) { TestUpdates.startCheck() }

    val build = when (val s = state) {
        is TestUpdates.State.Available -> s.build
        is TestUpdates.State.Downloading -> s.build
        is TestUpdates.State.Failed -> s.build
        else -> return
    }
    val downloading = state as? TestUpdates.State.Downloading
    val failed = state is TestUpdates.State.Failed
    AlertDialog(
        modifier = Modifier.testTag("test-update-dialog"),
        shape = RoundedCornerShape(StepUpDesign.DialogRadius),
        onDismissRequest = { if (downloading == null) TestUpdates.dismiss() },
        containerColor = Carbon,
        titleContentColor = Snow,
        textContentColor = Silver,
        title = { Text(stringResource(R.string.test_update_title), fontWeight = FontWeight.Black) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(
                    stringResource(
                        when {
                            failed -> R.string.test_update_failed
                            downloading != null -> R.string.test_update_downloading
                            else -> R.string.test_update_body
                        },
                    ),
                    style = MaterialTheme.typography.bodyMedium,
                    lineHeight = 20.sp,
                )
                Text(
                    stringResource(R.string.test_update_versions, TestUpdates.buildLabel,
                        build.version + build.commit.takeIf { it.isNotBlank() }?.let { " · $it" }.orEmpty()),
                    style = MaterialTheme.typography.bodySmall,
                    color = Silver,
                )
                if (downloading != null) {
                    val progress = downloading.progress
                    if (progress == null) {
                        LinearProgressIndicator(Modifier.fillMaxWidth(), color = VoltText)
                    } else {
                        LinearProgressIndicator(progress = { progress }, modifier = Modifier.fillMaxWidth(), color = VoltText)
                    }
                }
            }
        },
        confirmButton = {
            TextButton(
                enabled = downloading == null,
                onClick = { TestUpdates.startDownload(context) },
                modifier = Modifier.testTag("test-update-install"),
            ) {
                Text(
                    stringResource(if (failed) R.string.test_update_retry else R.string.test_update_install),
                    color = VoltText, fontWeight = FontWeight.Bold,
                )
            }
        },
        dismissButton = {
            if (downloading == null) {
                TextButton(onClick = TestUpdates::dismiss, modifier = Modifier.testTag("test-update-later")) {
                    Text(stringResource(R.string.test_update_later), color = Silver)
                }
            }
        },
    )
}
