package com.stepup.android.ui.screens.community

import android.widget.Toast
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.stepup.android.R
import com.stepup.android.data.repo.BoardSyncState
import com.stepup.android.data.repo.ReportReason
import com.stepup.android.ui.components.GhostButton
import com.stepup.android.ui.components.GlowCard
import com.stepup.android.ui.components.quietClickable
import com.stepup.android.ui.theme.Alert
import com.stepup.android.ui.theme.Carbon
import com.stepup.android.ui.theme.Silver
import com.stepup.android.ui.theme.Snow
import com.stepup.android.ui.theme.Volt

/** 게시판에서 한 일의 결과를 짧게 띄우고 비운다. */
@Composable
fun BoardNoticeToast(viewModel: CommunityViewModel) {
    val notice by viewModel.boardNotice.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val text = notice?.let { stringResource(it.messageRes()) }
    LaunchedEffect(notice) {
        if (text != null) {
            Toast.makeText(context, text, Toast.LENGTH_SHORT).show()
            viewModel.consumeBoardNotice()
        }
    }
}

private fun BoardNotice.messageRes(): Int = when (this) {
    BoardNotice.REPORTED -> R.string.board_notice_reported
    BoardNotice.BLOCKED -> R.string.board_notice_blocked
    BoardNotice.FAILED -> R.string.crew_notice_failed
    BoardNotice.SIGN_IN -> R.string.board_sign_in_needed
}

/**
 * 게시판이 비어 있을 때, 왜 비었는지.
 *
 * 글은 이제 서버에만 있어서 로그인하지 않았거나 서버에 닿지 못하면 목록이
 * 빈다. 그걸 "아직 글이 없어요"와 섞으면 사용자는 게시판이 망한 줄 안다.
 */
@Composable
fun BoardSyncCard(state: BoardSyncState, onRetry: () -> Unit) {
    val message = when (state) {
        BoardSyncState.Idle, BoardSyncState.Loading -> stringResource(R.string.board_loading)
        BoardSyncState.SignInRequired -> stringResource(R.string.board_sign_in_needed)
        is BoardSyncState.Failed -> stringResource(R.string.board_load_failed)
        BoardSyncState.Ready -> return
    }
    GlowCard(contentPadding = PaddingValues(20.dp), spacing = 12.dp) {
        Text(message, style = MaterialTheme.typography.bodyMedium, color = Silver)
        if (state == BoardSyncState.SignInRequired) {
            com.stepup.android.ui.components.SignInAgainButton()
        }
        if (state is BoardSyncState.Failed) {
            GhostButton(
                text = stringResource(R.string.crew_retry),
                onClick = onRetry,
                modifier = Modifier.fillMaxWidth(),
            )
        }
    }
}

/**
 * 신고 창 — 사유를 고르면 신고가 들어가고, 아래에서 쓴 사람을 차단할 수도 있다.
 *
 * 한 사람이 신고를 넣어도 글은 바로 모두에게서 사라지지 않는다. 5건이 모여야
 * 서버가 내린다. 신고한 사람의 화면에서는 바로 빠진다.
 */
@Composable
fun ReportDialogHost(viewModel: CommunityViewModel) {
    val target by viewModel.reportTarget.collectAsStateWithLifecycle()
    val current = target ?: return

    AlertDialog(
        onDismissRequest = viewModel::dismissReport,
        containerColor = Carbon,
        titleContentColor = Snow,
        textContentColor = Silver,
        title = {
            Text(text = stringResource(R.string.report_title), fontWeight = FontWeight.Black)
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(
                    text = stringResource(R.string.report_hint),
                    fontSize = 12.sp,
                    color = Silver,
                    modifier = Modifier.padding(bottom = 8.dp),
                )
                ReportReason.entries.forEach { reason ->
                    Text(
                        text = stringResource(reason.labelRes()),
                        fontSize = 14.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = Snow,
                        modifier = Modifier
                            .fillMaxWidth()
                            .quietClickable { viewModel.submitReport(reason) }
                            .padding(vertical = 10.dp),
                    )
                }
                if (current.authorId.isNotBlank()) {
                    Text(
                        text = stringResource(R.string.report_block, current.authorName),
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = Alert,
                        modifier = Modifier
                            .fillMaxWidth()
                            .quietClickable(viewModel::blockReported)
                            .padding(vertical = 12.dp),
                    )
                }
            }
        },
        confirmButton = {
            TextButton(onClick = viewModel::dismissReport) {
                Text(
                    text = stringResource(R.string.common_cancel),
                    color = Volt,
                    fontWeight = FontWeight.Bold,
                )
            }
        },
    )
}

private fun ReportReason.labelRes(): Int = when (this) {
    ReportReason.SPAM -> R.string.report_reason_spam
    ReportReason.ABUSE -> R.string.report_reason_abuse
    ReportReason.SEXUAL -> R.string.report_reason_sexual
    ReportReason.DANGER -> R.string.report_reason_danger
    ReportReason.FRAUD -> R.string.report_reason_fraud
    ReportReason.OTHER -> R.string.report_reason_other
}
