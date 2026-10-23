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
 * 테스트 APK 새 버전 창. 더 새 빌드가 있을 때만 뜬다. "업데이트"를 누르면 받아서 바로 깐다 — 안드로이드 12 이상은
 * 확인 화면 없이 앱이 잠깐 닫혔다 새 버전이 되고(알림을 누르면 열린다), 11 이하는 안드로이드 확인 화면에서 한 번 더 누른다.
 * 데이터는 그대로 남는다. 러닝 중에는 뜨지 않는다(깔면 러닝이 끊긴다).
 */
@Composable
fun TestUpdatePrompt() {
    if (!TestUpdates.enabled) return
    val context = LocalContext.current
    val state by TestUpdates.state.collectAsState()
    val run by com.stepup.android.service.WalkSessionService.state.collectAsState()
    // 앱을 켤 때와 앱으로 돌아올 때마다(5분에 한 번까지) 새 빌드를 묻는다 — 켜 둔 앱도 새 APK 를 알게.
    // "출처를 알 수 없는 앱" 설정에서 허용하고 돌아왔으면 이어서 깐다
    androidx.lifecycle.compose.LifecycleResumeEffect(Unit) {
        TestUpdates.onResume(context)
        onPauseOrDispose { }
    }
    if (run.isActive || run.saveStatus == com.stepup.android.service.RunSaveStatus.SAVING) return

    val build = when (val s = state) {
        is TestUpdates.State.Available -> s.build
        is TestUpdates.State.Downloading -> s.build
        is TestUpdates.State.Installing -> s.build
        is TestUpdates.State.NeedsPermission -> s.build
        is TestUpdates.State.Failed -> s.build
        else -> return
    }
    val downloading = state as? TestUpdates.State.Downloading
    val installing = state is TestUpdates.State.Installing
    val needsPermission = state is TestUpdates.State.NeedsPermission
    val failed = state as? TestUpdates.State.Failed
    val working = downloading != null || installing
    AlertDialog(
        modifier = Modifier.testTag("test-update-dialog"),
        shape = RoundedCornerShape(StepUpDesign.DialogRadius),
        onDismissRequest = { if (!working) TestUpdates.dismiss() },
        containerColor = Carbon,
        titleContentColor = Snow,
        textContentColor = Silver,
        title = { Text(stringResource(R.string.test_update_title), fontWeight = FontWeight.Black) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(
                    stringResource(
                        when {
                            failed != null -> failureText(failed.reason)
                            needsPermission -> R.string.test_update_permission
                            installing -> R.string.test_update_installing
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
                if (failed != null && failed.detail.isNotBlank()) {
                    Text(failed.detail, style = MaterialTheme.typography.bodySmall, color = Silver, maxLines = 3)
                }
                if (working) {
                    val progress = downloading?.progress
                    if (progress == null) {
                        LinearProgressIndicator(Modifier.fillMaxWidth(), color = VoltText)
                    } else {
                        LinearProgressIndicator(progress = { progress }, modifier = Modifier.fillMaxWidth(), color = VoltText)
                    }
                }
            }
        },
        confirmButton = {
            if (!working) {
                TextButton(
                    onClick = {
                        if (needsPermission) TestUpdates.openInstallPermission(context) else TestUpdates.startDownload(context)
                    },
                    modifier = Modifier.testTag("test-update-install"),
                ) {
                    Text(
                        stringResource(
                            when {
                                needsPermission -> R.string.test_update_open_settings
                                failed != null -> R.string.test_update_retry
                                else -> R.string.test_update_install
                            },
                        ),
                        color = VoltText, fontWeight = FontWeight.Bold,
                    )
                }
            }
        },
        dismissButton = {
            if (!working) {
                TextButton(onClick = TestUpdates::dismiss, modifier = Modifier.testTag("test-update-later")) {
                    Text(stringResource(R.string.test_update_later), color = Silver)
                }
            }
        },
    )
}

/** 깔지 못한 까닭의 글 */
private fun failureText(reason: TestUpdates.Failure): Int = when (reason) {
    TestUpdates.Failure.DOWNLOAD -> R.string.test_update_failed
    TestUpdates.Failure.SIGNATURE -> R.string.test_update_failed_signature
    TestUpdates.Failure.DOWNGRADE -> R.string.test_update_failed_downgrade
    TestUpdates.Failure.STORAGE -> R.string.test_update_failed_storage
    TestUpdates.Failure.BLOCKED -> R.string.test_update_failed_blocked
    TestUpdates.Failure.UNFINISHED -> R.string.test_update_unfinished
    TestUpdates.Failure.OTHER -> R.string.test_update_failed_other
}
