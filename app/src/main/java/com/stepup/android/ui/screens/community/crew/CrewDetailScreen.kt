package com.stepup.android.ui.screens.community.crew

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.stepup.android.R
import com.stepup.android.data.repo.CrewApplied
import com.stepup.android.domain.CrewApplicationStatus
import com.stepup.android.domain.CrewCard
import com.stepup.android.domain.CrewFooter
import com.stepup.android.domain.CrewProblem
import com.stepup.android.domain.CrewRole
import com.stepup.android.domain.CrewRules
import com.stepup.android.ui.experience.feedbackClickable

/** 상세에서 나가는 곳 */
class CrewDetailActions(
    val onBack: () -> Unit,
    val onOpenImage: () -> Unit,
    val onOpenLeader: (leaderId: String) -> Unit,
    val onOpenMembers: () -> Unit,
    val onOpenGoal: () -> Unit,
    /** 14 가입 신청 작성 */
    val onJoin: () -> Unit,
    /** 15 신청 내역 */
    val onOpenApplication: (Long) -> Unit,
    /** 18 · 19 결과 */
    val onOpenResult: (Long) -> Unit,
    /** 바로 가입 크루에 들어갔다 — 18 모양의 결과 */
    val onJoinedNow: () -> Unit,
    val onManage: () -> Unit,
    val onSettings: () -> Unit,
    val onReported: () -> Unit,
    /** 기존 크루 게시판 · 같이 달리기 대기실(멤버) */
    val onOpenBoard: () -> Unit,
    val onOpenLobby: () -> Unit,
    /** 66 → 목록 */
    val onList: () -> Unit,
    /** 크루 채팅(2026-09-29) — 멤버 · 크루장만(87 새 멤버 상세에서도) */
    val onOpenChat: () -> Unit = {},
)

/**
 * 크루 상세 — 방문자(08 · 70) · 가입 대기(20) · 멤버(21 · 87 · 86) · 크루장(76 · 89) · 모집 쉼(22) · 정원 마감(23).
 * 역할과 모집 상태를 따로 보고, 아래 큰 버튼 하나만 둔다. 처음 읽는 동안은 67, 해산 · 접근 불가는 66.
 */
@Composable
fun CrewDetailScreen(actions: CrewDetailActions, viewModel: CrewScreenViewModel) {
    val card by viewModel.card.collectAsStateWithLifecycle()
    val load by viewModel.load.collectAsStateWithLifecycle()
    val missing by viewModel.missing.collectAsStateWithLifecycle()
    val op by viewModel.op.collectAsStateWithLifecycle()
    var sheet by rememberSaveable { mutableStateOf("") }

    LaunchedEffect(op) {
        when {
            op.op == CrewOp.LEAVE && op.done -> { sheet = ""; viewModel.consumeOp() }
            op.op == CrewOp.REPORT && op.done -> { sheet = ""; viewModel.consumeOp(); actions.onReported() }
            op.op == CrewOp.APPLY && op.done -> {
                viewModel.consumeOp()
                when (val applied = op.applied()) {
                    CrewApplied.Member -> actions.onJoinedNow()
                    is CrewApplied.Pending -> actions.onOpenApplication(applied.applicationId)
                    is CrewApplied.Declined -> actions.onOpenResult(applied.applicationId)
                    CrewApplied.Canceled, null -> Unit
                }
            }
        }
    }

    val current = card
    when {
        missing -> CrewMissingPage(actions.onBack, actions.onList)
        current == null && load is CrewLoad.Failed -> CrewDetailFailed(actions.onBack) { viewModel.reload() }
        current == null -> CrewDetailLoading(actions.onBack)
        else -> CrewDetailContent(
            card = current,
            actions = actions,
            busy = op.running,
            joinFailed = op.op == CrewOp.APPLY && op.problem != null,
            onMore = { sheet = if (current.role == CrewRole.OWNER) { actions.onSettings(); "" } else SHEET_MENU },
            onLevel = { sheet = SHEET_LEVEL },
            onJoinNow = { viewModel.apply() },
        )
    }

    when (sheet) {
        SHEET_LEVEL -> current?.let { CrewLevelSheet(it) { sheet = "" } }
        SHEET_MENU -> current?.let { crew ->
            if (crew.role == CrewRole.MEMBER) {
                CrewMenuSheet(
                    stringResource(R.string.crew_menu_member_title),
                    listOf(
                        CrewMenuItem(stringResource(R.string.crew_menu_members), "crew-menu-members") { sheet = ""; actions.onOpenMembers() },
                        CrewMenuItem(stringResource(R.string.crew_menu_report_member), "crew-menu-report") { sheet = SHEET_REPORT },
                        CrewMenuItem(stringResource(R.string.crew_menu_leave), "crew-menu-leave", warn = true) { sheet = SHEET_LEAVE },
                    ),
                    onDismiss = { sheet = "" },
                    tag = "crew-member-menu",
                )
            } else {
                CrewMenuSheet(
                    stringResource(R.string.crew_menu_visitor_title),
                    listOf(
                        CrewMenuItem(stringResource(R.string.crew_menu_members_short), "crew-menu-members") { sheet = ""; actions.onOpenMembers() },
                        CrewMenuItem(stringResource(R.string.crew_menu_report), "crew-menu-report") { sheet = SHEET_REPORT },
                    ),
                    onDismiss = { sheet = "" },
                    tag = "crew-visitor-menu",
                )
            }
        }
        SHEET_REPORT -> CrewReportSheet(
            busy = op.op == CrewOp.REPORT && op.running,
            failed = op.op == CrewOp.REPORT && op.problem != null,
            onSend = { reason, note -> viewModel.report(reason, note) },
            onDismiss = { if (!op.running) { sheet = ""; if (op.op == CrewOp.REPORT) viewModel.consumeOp() } },
        )
        SHEET_LEAVE -> current?.let { crew ->
            CrewConfirmSheet(
                title = stringResource(R.string.crew_leave_title, crew.name),
                body = stringResource(R.string.crew_leave_body),
                confirm = stringResource(R.string.crew_menu_leave),
                danger = true,
                busy = op.op == CrewOp.LEAVE && op.running,
                error = if (op.op == CrewOp.LEAVE && op.problem != null) stringResource(R.string.crew_action_failed) else null,
                tag = "crew-leave",
                onConfirm = { viewModel.leave() },
                onDismiss = { if (!op.running) { sheet = ""; if (op.op == CrewOp.LEAVE) viewModel.consumeOp() } },
            )
        }
    }
}

private const val SHEET_LEVEL = "level"
private const val SHEET_MENU = "menu"
private const val SHEET_REPORT = "report"
private const val SHEET_LEAVE = "leave"

@Composable
private fun CrewDetailContent(
    card: CrewCard,
    actions: CrewDetailActions,
    busy: Boolean,
    joinFailed: Boolean,
    onMore: () -> Unit,
    onLevel: () -> Unit,
    onJoinNow: () -> Unit,
) {
    val ink = crewInk()
    val words = rememberCrewWords()
    CrewPage(Modifier.testTag("crew-detail")) {
        CrewTopBar(card.name, actions.onBack, onMore = onMore)
        Column(Modifier.weight(1f).fillMaxWidth().verticalScroll(rememberScrollState()).padding(horizontal = CrewGutter)) {
            Spacer(Modifier.height(18.dp))
            card.unseenResult?.let { result ->
                Row(
                    Modifier.fillMaxWidth().heightIn(min = 48.dp).clip(RoundedCornerShape(14.dp)).background(ink.card)
                        .feedbackClickable(role = Role.Button) { card.myApplicationId?.let(actions.onOpenResult) }
                        .padding(horizontal = 16.dp, vertical = 10.dp).testTag("crew-detail-result"),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        stringResource(if (result == CrewApplicationStatus.APPROVED) R.string.crew_result_banner_approved else R.string.crew_result_banner_declined, card.name),
                        color = ink.text, fontSize = 13.5.sp, modifier = Modifier.weight(1f),
                    )
                    Icon(Icons.Filled.ChevronRight, null, tint = ink.info, modifier = Modifier.size(18.dp))
                }
                Spacer(Modifier.height(16.dp))
            }
            // 대표 이미지 · 이름 · 레벨 · 인원 · 지역과 분위기
            Row(verticalAlignment = Alignment.Top) {
                Box(
                    Modifier.size(84.dp).clip(RoundedCornerShape(17.dp))
                        .feedbackClickable(role = Role.Image, onClick = actions.onOpenImage).testTag("crew-detail-image"),
                ) { CrewImage(card, 84.dp, 17.dp) }
                Column(Modifier.weight(1f).padding(start = 17.dp)) {
                    Text(
                        card.name, color = ink.text, fontSize = 28.sp, lineHeight = 34.sp, fontWeight = FontWeight.SemiBold,
                        maxLines = 2, overflow = TextOverflow.Ellipsis, modifier = Modifier.testTag("crew-detail-name"),
                    )
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            Modifier.heightIn(min = 44.dp).clip(RoundedCornerShape(8.dp)).feedbackClickable(role = Role.Button, onClick = onLevel)
                                .testTag("crew-detail-level"),
                            contentAlignment = Alignment.CenterStart,
                        ) { CrewLevelChip(card.level) }
                        Spacer(Modifier.width(36.dp))
                        Box(
                            Modifier.heightIn(min = 44.dp).clip(RoundedCornerShape(8.dp))
                                .feedbackClickable(role = Role.Button, onClick = actions.onOpenMembers).testTag("crew-detail-members"),
                            contentAlignment = Alignment.CenterStart,
                        ) { CrewMembersLabel(card.memberCount, card.capacity, ink.secondary) }
                    }
                    val sub = words.subLine(card)
                    if (sub.isNotEmpty()) Text(sub, color = ink.secondary, fontSize = 11.5.sp, maxLines = 2, overflow = TextOverflow.Ellipsis)
                }
            }
            if (card.tagline.isNotBlank()) {
                Spacer(Modifier.height(26.dp))
                Text(card.tagline, color = ink.text, fontSize = 24.sp, lineHeight = 31.sp, fontWeight = FontWeight.SemiBold, modifier = Modifier.testTag("crew-detail-tagline"))
            }
            if (card.leaderNote.isNotBlank()) {
                Spacer(Modifier.height(12.dp))
                Text(card.leaderNote, color = ink.secondary, fontSize = 14.sp, lineHeight = 25.sp)
            }
            // 정기 모임 · 한 번에
            Spacer(Modifier.height(24.dp))
            Column(
                Modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp)).background(ink.card).padding(horizontal = 18.dp, vertical = 16.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp),
            ) {
                CrewInfoLine(stringResource(R.string.crew_detail_meet), words.scheduleShort(card.schedule), 15)
                words.onceLine(card)?.let { CrewInfoLine(stringResource(R.string.crew_detail_once), it, 13) }
            }
            // 크루장
            Spacer(Modifier.height(12.dp))
            Row(
                Modifier.fillMaxWidth().heightIn(min = 50.dp).clip(RoundedCornerShape(12.dp))
                    .feedbackClickable(role = Role.Button) { actions.onOpenLeader(card.leaderId) }.testTag("crew-detail-leader"),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                CrewAvatar(card.leaderName, 34.dp, leaderFace(card), ink.text)
                Spacer(Modifier.width(12.dp))
                Text(
                    stringResource(R.string.crew_leader_named, card.leaderName), color = ink.text, fontSize = 12.5.sp,
                    fontWeight = FontWeight.SemiBold, modifier = Modifier.weight(1f), maxLines = 1, overflow = TextOverflow.Ellipsis,
                )
                Text(stringResource(R.string.crew_detail_leader_more), color = ink.info, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                Icon(Icons.Filled.ChevronRight, null, tint = ink.info, modifier = Modifier.size(18.dp))
            }
            CrewRow(
                stringResource(R.string.crew_detail_members), actions.onOpenMembers, Modifier.testTag("crew-detail-members-row"),
                value = stringResource(R.string.crew_members_only, card.memberCount),
            )
            CrewRules.progress(card)?.let { progress ->
                Spacer(Modifier.height(19.dp))
                Column(
                    Modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp)).background(ink.card)
                        .feedbackClickable(role = Role.Button, onClick = actions.onOpenGoal)
                        .padding(horizontal = 18.dp, vertical = 18.dp).testTag("crew-detail-goal"),
                ) {
                    Text(stringResource(R.string.crew_goal_together, progress.goalKm), color = ink.text, fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
                    Spacer(Modifier.height(18.dp))
                    CrewWeeklyLine(progress)
                }
            }
            // 멤버에게는 기존 크루 게시판 · 같이 달리기(크루 러닝으로 달린 거리가 이번 주 목표에 들어간다)
            if (card.role == CrewRole.MEMBER || card.role == CrewRole.OWNER) {
                Spacer(Modifier.height(8.dp))
                CrewRow(stringResource(R.string.chat_row_crew_chat), actions.onOpenChat, Modifier.testTag("crew-detail-chat"))
                CrewRow(stringResource(R.string.crew_detail_lobby), actions.onOpenLobby, Modifier.testTag("crew-detail-lobby"), value = stringResource(R.string.crew_detail_lobby_hint))
                CrewRow(stringResource(R.string.crew_detail_board), actions.onOpenBoard, Modifier.testTag("crew-detail-board"))
            }
            Spacer(Modifier.height(28.dp))
        }
        CrewDetailFooter(card, actions, busy, joinFailed, onJoinNow)
    }
}

@Composable
private fun CrewInfoLine(label: String, value: String, size: Int) {
    val ink = crewInk()
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Text(label, color = ink.secondary, fontSize = 12.sp, modifier = Modifier.padding(end = 12.dp))
        Text(
            value, color = ink.text, fontSize = size.sp, fontWeight = FontWeight.SemiBold, textAlign = TextAlign.End,
            modifier = Modifier.weight(1f), maxLines = 2, overflow = TextOverflow.Ellipsis,
        )
    }
}

/** 아래 한 줄 설명과 큰 버튼 하나 — 역할 · 모집 상태로 고른다 */
@Composable
private fun CrewDetailFooter(card: CrewCard, actions: CrewDetailActions, busy: Boolean, joinFailed: Boolean, onJoinNow: () -> Unit) {
    val footer = CrewRules.footer(card)
    val caption = stringResource(
        when (footer) {
            CrewFooter.APPLY -> R.string.crew_footer_apply
            CrewFooter.JOIN_NOW -> R.string.crew_footer_join_now
            CrewFooter.PAUSED -> R.string.crew_footer_paused
            CrewFooter.FULL -> R.string.crew_footer_full
            CrewFooter.PENDING -> R.string.crew_footer_pending
            CrewFooter.MEMBER -> R.string.crew_footer_member
            CrewFooter.OWNER -> R.string.crew_footer_owner
            CrewFooter.OWNER_PAUSED -> R.string.crew_footer_owner_paused
        },
    )
    CrewBottomBar(caption = if (joinFailed && footer == CrewFooter.JOIN_NOW) stringResource(R.string.crew_join_now_failed) else caption) {
        when (footer) {
            CrewFooter.APPLY -> CrewButton(stringResource(R.string.crew_apply_button), actions.onJoin, Modifier.testTag("crew-detail-primary"))
            CrewFooter.JOIN_NOW -> CrewButton(stringResource(R.string.crew_join_now_button), onJoinNow, Modifier.testTag("crew-detail-primary"), busy = busy)
            CrewFooter.PAUSED -> CrewButton(stringResource(R.string.crew_paused_button), {}, Modifier.testTag("crew-detail-primary"), CrewButtonKind.DISABLED)
            CrewFooter.FULL -> CrewButton(stringResource(R.string.crew_full_button), {}, Modifier.testTag("crew-detail-primary"), CrewButtonKind.DISABLED)
            CrewFooter.PENDING -> CrewButton(
                stringResource(R.string.crew_pending_button),
                { card.myApplicationId?.let(actions.onOpenApplication) },
                Modifier.testTag("crew-detail-primary"),
            )
            CrewFooter.MEMBER -> CrewButton(stringResource(R.string.crew_members_button), actions.onOpenMembers, Modifier.testTag("crew-detail-primary"))
            CrewFooter.OWNER, CrewFooter.OWNER_PAUSED ->
                CrewButton(stringResource(R.string.crew_manage_button), actions.onManage, Modifier.testTag("crew-detail-primary"))
        }
    }
}

/** 67 상세 로딩 — 신청 동작은 정보를 확인한 뒤 켠다 */
@Composable
private fun CrewDetailLoading(onBack: () -> Unit) {
    CrewPage(Modifier.testTag("crew-detail-loading")) {
        CrewTopBar(stringResource(R.string.crew_title), onBack)
        Column(Modifier.weight(1f).padding(horizontal = CrewGutter)) {
            Spacer(Modifier.height(18.dp))
            Row {
                CrewSkeletonBox(Modifier.size(84.dp), 17.dp)
                Column(Modifier.padding(start = 17.dp, top = 16.dp)) {
                    CrewSkeletonBox(Modifier.width(160.dp).height(24.dp), 8.dp)
                    Spacer(Modifier.height(20.dp))
                    CrewSkeletonBox(Modifier.width(206.dp).height(12.dp), 6.dp)
                }
            }
            Spacer(Modifier.height(46.dp))
            CrewSkeletonBox(Modifier.fillMaxWidth(0.84f).height(24.dp), 8.dp)
            Spacer(Modifier.height(24.dp))
            CrewSkeletonBox(Modifier.fillMaxWidth(0.95f).height(12.dp), 6.dp)
            Spacer(Modifier.height(38.dp))
            CrewSkeletonBox(Modifier.fillMaxWidth().height(100.dp), 16.dp)
            Spacer(Modifier.height(20.dp))
            CrewSkeletonBox(Modifier.fillMaxWidth().height(52.dp), 16.dp)
        }
        CrewBottomBar { CrewButton(stringResource(R.string.crew_checking), {}, kind = CrewButtonKind.DISABLED) }
    }
}

/** 처음 읽기 실패(캐시 없음) — 다시 읽기 */
@Composable
private fun CrewDetailFailed(onBack: () -> Unit, onRetry: () -> Unit) {
    val ink = crewInk()
    CrewPage(Modifier.testTag("crew-detail-error")) {
        CrewTopBar(stringResource(R.string.crew_title), onBack)
        Spacer(Modifier.height(140.dp))
        CrewEmptyState(
            icon = { Icon(Icons.Filled.Refresh, null, tint = ink.info, modifier = Modifier.size(44.dp)) },
            title = stringResource(R.string.crew_detail_error_title),
            body = stringResource(R.string.crew_list_error_body),
        ) { CrewButton(stringResource(R.string.crew_list_reload), onRetry, Modifier.testTag("crew-detail-retry")) }
    }
}

/** 66 크루 접근 불가 — 해산 · 비공개 · 권한 변경 */
@Composable
internal fun CrewMissingPage(onBack: () -> Unit, onList: () -> Unit) {
    val ink = crewInk()
    CrewPage(Modifier.testTag("crew-missing")) {
        CrewTopBar(stringResource(R.string.crew_title), onBack)
        Column(Modifier.weight(1f).fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
            Spacer(Modifier.height(170.dp))
            Icon(Icons.Outlined.Info, null, tint = ink.info, modifier = Modifier.size(44.dp))
            Spacer(Modifier.height(30.dp))
            Text(stringResource(R.string.crew_missing_title), color = ink.text, fontSize = 24.sp, fontWeight = FontWeight.SemiBold, textAlign = TextAlign.Center)
            Spacer(Modifier.height(16.dp))
            Text(stringResource(R.string.crew_missing_body), color = ink.secondary, fontSize = 13.5.sp, textAlign = TextAlign.Center, modifier = Modifier.padding(horizontal = CrewGutter))
        }
        CrewBottomBar { CrewButton(stringResource(R.string.crew_to_list), onList, Modifier.testTag("crew-missing-list")) }
    }
}

/** 실패의 까닭을 한 줄로 — 칸 오류가 아니면 일반 문구 */
@Composable
internal fun crewProblemText(problem: CrewProblem): String = stringResource(
    when (problem) {
        CrewProblem.NETWORK -> R.string.crew_problem_network
        CrewProblem.SIGN_IN -> R.string.crew_problem_signin
        CrewProblem.CREW_LIMIT -> R.string.crew_problem_limit
        CrewProblem.CREW_MISSING -> R.string.crew_missing_body
        CrewProblem.NOT_OWNER -> R.string.crew_problem_not_owner
        else -> R.string.crew_action_failed
    },
)
