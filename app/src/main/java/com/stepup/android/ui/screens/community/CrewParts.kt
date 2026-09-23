package com.stepup.android.ui.screens.community

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.stepup.android.R
import com.stepup.android.data.repo.Crew
import com.stepup.android.data.repo.CrewJoinPolicy
import com.stepup.android.data.repo.CrewJoinRequest
import com.stepup.android.data.repo.CrewSyncState
import com.stepup.android.ui.components.GhostButton
import com.stepup.android.ui.components.GlowCard
import com.stepup.android.ui.components.VoltButton
import com.stepup.android.ui.theme.Silver
import com.stepup.android.ui.theme.Slate
import com.stepup.android.ui.theme.Snow
import com.stepup.android.ui.theme.Volt

/** 크루 가입·탈퇴 등의 결과를 짧게 띄우고 비운다. */
@Composable
fun CrewNoticeToast(viewModel: CommunityViewModel) {
    val notice by viewModel.crewNotice.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val text = notice?.let { stringResource(it.messageRes()) }
    LaunchedEffect(notice) {
        if (text != null) {
            Toast.makeText(context, text, Toast.LENGTH_SHORT).show()
            viewModel.consumeCrewNotice()
        }
    }
}

private fun CrewNotice.messageRes(): Int = when (this) {
    CrewNotice.JOINED -> R.string.crew_notice_joined
    CrewNotice.REQUESTED -> R.string.crew_notice_requested
    CrewNotice.CANCELLED -> R.string.crew_notice_cancelled
    CrewNotice.LEFT -> R.string.crew_notice_left
    CrewNotice.SAVED -> R.string.crew_notice_saved
    CrewNotice.APPROVED -> R.string.crew_notice_approved
    CrewNotice.REJECTED -> R.string.crew_notice_rejected
    CrewNotice.FAILED -> R.string.crew_notice_failed
    CrewNotice.SIGN_IN -> R.string.crew_sign_in_needed
}

/**
 * 크루 목록이 비어 있을 때, 왜 비었는지.
 *
 * 크루는 이제 서버에만 있어서, 로그인하지 않았거나 서버에 닿지 못하면 목록이
 * 빈다. "크루가 없다"와 "못 불러왔다"는 다른 말이다 — 앞의 것은 만들면 되고,
 * 뒤의 것은 다시 시도하면 된다.
 *
 * @return 보여 줄 것이 없으면(목록을 제대로 받았으면) 아무것도 그리지 않는다.
 */
@Composable
fun CrewSyncCard(state: CrewSyncState, onRetry: () -> Unit) {
    val message = when (state) {
        CrewSyncState.Idle, CrewSyncState.Loading -> stringResource(R.string.crew_loading)
        CrewSyncState.SignInRequired -> stringResource(R.string.crew_sign_in_needed)
        is CrewSyncState.Failed -> stringResource(R.string.crew_load_failed)
        CrewSyncState.Ready -> return
    }
    GlowCard(contentPadding = PaddingValues(20.dp), spacing = 12.dp) {
        Text(message, style = MaterialTheme.typography.bodyMedium, color = Silver)
        if (state is CrewSyncState.Failed) {
            GhostButton(
                text = stringResource(R.string.crew_retry),
                onClick = onRetry,
                modifier = Modifier.fillMaxWidth(),
            )
        }
    }
}

/** 이름 옆에 붙는 작은 표. 승인제 크루와 크루장 표시에 쓴다. */
@Composable
fun CrewTag(text: String) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(50))
            .background(Volt.copy(alpha = 0.16f))
            .padding(horizontal = 6.dp, vertical = 2.dp),
    ) {
        Text(text = text, color = Volt, fontSize = 8.5.sp, fontWeight = FontWeight.Black)
    }
}

/**
 * 아직 멤버가 아닌 사람의 가입 버튼.
 *
 * 승인제 크루는 "가입 신청", 신청해 둔 크루는 "승인 대기 중"과 취소 버튼을 보인다.
 */
@Composable
fun CrewJoinAction(crew: Crew, onToggleJoin: () -> Unit, modifier: Modifier = Modifier) {
    if (crew.requested) {
        Column(modifier, verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(
                text = stringResource(R.string.crew_waiting_approval),
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                color = Silver,
            )
            GhostButton(
                text = stringResource(R.string.crew_cancel_request),
                onClick = onToggleJoin,
                modifier = Modifier.fillMaxWidth(),
                accent = Silver,
            )
        }
    } else {
        GhostButton(
            text = stringResource(
                if (crew.joinPolicy == CrewJoinPolicy.APPROVAL) R.string.crew_apply
                else R.string.community_join_crew,
            ),
            onClick = onToggleJoin,
            modifier = modifier,
        )
    }
}

/** 가입 방식 고르기 — 크루 만들기와 크루 관리에서 같이 쓴다. */
@Composable
fun CrewPolicyPicker(selected: CrewJoinPolicy, onSelect: (CrewJoinPolicy) -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(
            text = stringResource(R.string.crew_policy_title),
            fontSize = 14.sp,
            fontWeight = FontWeight.Black,
            color = Slate,
        )
        com.stepup.android.ui.components.TwoWaySwitch(
            labels = CrewJoinPolicy.entries.map { stringResource(it.labelRes()) },
            selected = CrewJoinPolicy.entries.indexOf(selected),
            onSelect = { onSelect(CrewJoinPolicy.entries[it]) },
        )
        Text(
            text = stringResource(
                if (selected == CrewJoinPolicy.APPROVAL) R.string.crew_policy_approval_desc
                else R.string.crew_policy_open_desc,
            ),
            fontSize = 14.sp,
            color = Silver,
        )
    }
}

fun CrewJoinPolicy.labelRes(): Int = when (this) {
    CrewJoinPolicy.OPEN -> R.string.crew_policy_open
    CrewJoinPolicy.APPROVAL -> R.string.crew_policy_approval
}

/**
 * 크루장만 보는 관리 카드 — 가입 방식과 기다리는 가입 신청.
 */
@Composable
fun CrewManageCard(
    crew: Crew,
    requests: CrewRequestsState?,
    onPolicy: (CrewJoinPolicy) -> Unit,
    onDecide: (CrewJoinRequest, Boolean) -> Unit,
    onRetry: () -> Unit,
) {
    GlowCard(contentPadding = PaddingValues(18.dp), spacing = 14.dp) {
        Text(
            text = stringResource(R.string.crew_manage_title),
            style = MaterialTheme.typography.titleSmall,
            color = Snow,
        )
        CrewPolicyPicker(selected = crew.joinPolicy, onSelect = onPolicy)

        // 자유 가입 크루에는 기다리는 신청이 생기지 않는다.
        if (crew.joinPolicy == CrewJoinPolicy.APPROVAL) {
            Text(
                text = stringResource(R.string.crew_requests_title),
                fontSize = 11.sp,
                fontWeight = FontWeight.Black,
                color = Slate,
            )
            when (requests) {
                null, CrewRequestsState.Loading -> Text(
                    text = stringResource(R.string.crew_loading),
                    fontSize = 12.sp,
                    color = Silver,
                )
                CrewRequestsState.Failed -> GhostButton(
                    text = stringResource(R.string.crew_retry),
                    onClick = onRetry,
                    modifier = Modifier.fillMaxWidth(),
                )
                is CrewRequestsState.Ready ->
                    if (requests.requests.isEmpty()) {
                        Text(
                            text = stringResource(R.string.crew_requests_empty),
                            fontSize = 12.sp,
                            color = Silver,
                        )
                    } else {
                        requests.requests.forEach { request ->
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                            ) {
                                Text(
                                    text = request.name,
                                    modifier = Modifier.weight(1f),
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = Snow,
                                    maxLines = 1,
                                )
                                GhostButton(
                                    text = stringResource(R.string.crew_reject),
                                    onClick = { onDecide(request, false) },
                                    accent = Silver,
                                )
                                VoltButton(
                                    text = stringResource(R.string.crew_approve),
                                    onClick = { onDecide(request, true) },
                                )
                            }
                        }
                    }
            }
        }
    }
}
