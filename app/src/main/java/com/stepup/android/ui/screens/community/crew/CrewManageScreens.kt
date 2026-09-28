package com.stepup.android.ui.screens.community.crew

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
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
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.stepup.android.R
import com.stepup.android.domain.CrewApplication
import com.stepup.android.domain.CrewApplicationStatus
import com.stepup.android.domain.CrewCard
import com.stepup.android.domain.CrewProblem
import com.stepup.android.domain.CrewRole

/** 관리 홈에서 나가는 곳 */
class CrewManageActions(
    val onBack: () -> Unit,
    val onRequests: () -> Unit,
    val onMembers: () -> Unit,
    val onEditProfile: () -> Unit,
    val onEditRunning: () -> Unit,
    val onEditRecruit: () -> Unit,
    val onGoal: () -> Unit,
    val onSettings: () -> Unit,
    val onList: () -> Unit,
)

/**
 * 40 크루 관리 홈 · 77 새 크루 관리(만든 직후 — 1명 · 신청 0건). 크루장이 아니게 되면(넘김 · 해산) 관리 화면을 닫는다.
 */
@Composable
fun CrewManageScreen(viewModel: CrewScreenViewModel, created: Boolean, actions: CrewManageActions) {
    val ink = crewInk()
    val card by viewModel.card.collectAsStateWithLifecycle()
    val missing by viewModel.missing.collectAsStateWithLifecycle()
    LaunchedEffect(Unit) { viewModel.reload() }
    val crew = card
    if (missing) return CrewMissingPage(actions.onBack, actions.onList)
    CrewPage(Modifier.testTag(if (created) "crew-manage-created" else "crew-manage")) {
        CrewTopBar(stringResource(R.string.crew_manage_title), actions.onBack)
        if (crew == null) return@CrewPage
        if (crew.role != CrewRole.OWNER) {
            // 크루장을 넘긴 뒤 — 관리 권한이 남지 않는다
            CrewEmptyState(icon = {}, title = stringResource(R.string.crew_manage_lost), body = "", modifier = Modifier.padding(top = 120.dp)) {
                CrewButton(stringResource(R.string.crew_view), actions.onBack)
            }
            return@CrewPage
        }
        Column(Modifier.weight(1f).fillMaxWidth().verticalScroll(rememberScrollState()).padding(horizontal = CrewGutter)) {
            Spacer(Modifier.height(18.dp))
            CrewIdentityStrip(crew, sub = if (created) crewLevelMembers(crew) else null)
            Spacer(Modifier.height(24.dp))
            CrewRow(
                stringResource(R.string.crew_manage_requests), actions.onRequests, Modifier.testTag("crew-manage-requests"),
                value = stringResource(R.string.crew_count_cases, crew.pendingCount),
            )
            if (!created) {
                CrewRow(
                    stringResource(R.string.crew_manage_members), actions.onMembers, Modifier.testTag("crew-manage-members"),
                    value = stringResource(R.string.crew_members_only, crew.memberCount),
                )
            }
            CrewRow(stringResource(R.string.crew_manage_profile), actions.onEditProfile, Modifier.testTag("crew-manage-profile"))
            CrewRow(stringResource(R.string.crew_manage_running), actions.onEditRunning, Modifier.testTag("crew-manage-running"))
            CrewRow(
                stringResource(R.string.crew_manage_recruit), actions.onEditRecruit, Modifier.testTag("crew-manage-recruit"),
                value = stringResource(if (crew.recruiting) R.string.crew_recruit_open else R.string.crew_recruit_paused),
            )
            CrewRow(
                stringResource(R.string.crew_manage_goal), actions.onGoal, Modifier.testTag("crew-manage-goal"),
                value = crew.goalKm?.let { stringResource(R.string.crew_km_value, it.toString()) } ?: stringResource(R.string.crew_goal_set),
            )
            if (!created) CrewRow(stringResource(R.string.crew_manage_settings), actions.onSettings, Modifier.testTag("crew-manage-settings"))
            Spacer(Modifier.height(24.dp))
        }
        if (created) CrewBottomBar { CrewButton(stringResource(R.string.crew_to_list_short), actions.onList, Modifier.testTag("crew-manage-list")) }
    }
}

/** "새 크루 · 1 / 20명" — 레벨이 없으면 새 크루 */
@Composable
internal fun crewLevelMembers(crew: CrewCard): String {
    val level = crew.level?.let { stringResource(R.string.crew_level_value, it) } ?: stringResource(R.string.crew_level_new)
    val members = if (crew.capacity != null) stringResource(R.string.crew_members_of, crew.memberCount, crew.capacity)
    else stringResource(R.string.crew_members_only, crew.memberCount)
    return "$level · $members"
}

/**
 * 48 가입 신청 목록 · 54 빈 목록 · 79 남은 신청 — 처리한 신청은 목록에서 빠진다. 신청자를 누르면 그 사람의 신청서(49).
 */
@Composable
fun CrewRequestsScreen(viewModel: CrewScreenViewModel, onBack: () -> Unit, onOpen: (Long) -> Unit, onManage: () -> Unit) {
    val ink = crewInk()
    val card by viewModel.card.collectAsStateWithLifecycle()
    val pending by viewModel.pending.collectAsStateWithLifecycle()
    LaunchedEffect(Unit) { viewModel.loadPending() }
    val list = (pending as? CrewLoad.Ready)?.value
    CrewPage(Modifier.testTag("crew-requests")) {
        CrewTopBar(stringResource(R.string.crew_requests_title), onBack)
        when {
            pending is CrewLoad.Failed -> CrewEmptyState(icon = {}, title = stringResource(R.string.crew_requests_failed), body = "", modifier = Modifier.padding(top = 120.dp)) {
                CrewButton(stringResource(R.string.crew_list_reload), { viewModel.loadPending() }, Modifier.testTag("crew-requests-retry"))
            }
            list == null -> Column(Modifier.padding(CrewGutter)) { CrewSkeletonBox(Modifier.fillMaxWidth().height(180.dp), 16.dp) }
            list.isEmpty() -> Column(Modifier.weight(1f).fillMaxWidth().padding(horizontal = CrewGutter).testTag("crew-requests-empty")) {
                Spacer(Modifier.height(22.dp))
                Text(stringResource(R.string.crew_requests_empty_title), color = ink.text, fontSize = 26.sp, fontWeight = FontWeight.SemiBold)
                Spacer(Modifier.height(12.dp))
                Text(stringResource(R.string.crew_requests_empty_body), color = ink.secondary, fontSize = 13.5.sp)
                Spacer(Modifier.height(150.dp))
                Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                    Icon(Icons.Filled.Check, null, tint = ink.info, modifier = Modifier.size(34.dp))
                }
                Spacer(Modifier.height(64.dp))
                CrewButton(stringResource(R.string.crew_to_manage), onManage, Modifier.testTag("crew-requests-manage"))
            }
            else -> LazyColumn(Modifier.weight(1f).fillMaxWidth(), contentPadding = PaddingValues(start = CrewGutter, end = CrewGutter, bottom = 32.dp)) {
                item {
                    Spacer(Modifier.height(22.dp))
                    Text(
                        stringResource(R.string.crew_requests_heading, list.size), color = ink.text, fontSize = 26.sp,
                        fontWeight = FontWeight.SemiBold, modifier = Modifier.testTag("crew-requests-heading"),
                    )
                    Spacer(Modifier.height(12.dp))
                    val crew = card
                    Text(
                        if (crew?.capacity != null) stringResource(R.string.crew_requests_sub, crew.name, crew.memberCount, crew.capacity)
                        else stringResource(R.string.crew_requests_sub_waiting),
                        color = ink.secondary, fontSize = 13.5.sp,
                    )
                    Spacer(Modifier.height(40.dp))
                }
                items(list, key = { it.id }) { application ->
                    CrewRequestItem(application) { onOpen(application.id) }
                }
            }
        }
    }
}

@Composable
private fun CrewRequestItem(application: CrewApplication, onClick: () -> Unit) {
    val ink = crewInk()
    val words = rememberCrewWords()
    Column(Modifier.fillMaxWidth().testTag("crew-request-${application.id}")) {
        CrewPersonRow(application.name, stringResource(R.string.crew_request_label), onClick, subColor = ink.info)
        val line = application.message.lines().firstOrNull()?.takeIf { it.isNotBlank() }
            ?: application.phrases.joinToString(" · ") { words.phrase(it) }.takeIf { it.isNotEmpty() }
        if (line != null) {
            Spacer(Modifier.height(12.dp))
            Text(line, color = ink.secondary, fontSize = 13.5.sp, maxLines = 2, overflow = TextOverflow.Ellipsis)
        }
        Spacer(Modifier.height(30.dp))
    }
}

/**
 * 49 가입 신청 검토 — 50 승인 확인 · 52 미승인 확인 · 81 승인 직전 정원 초과 · 80 관리 작업 실패.
 * 승인 전 정원을 서버가 다시 재고, 같은 신청을 두 번 처리하지 않는다. 성공한 뒤에만 결과(51 · 53)로 간다.
 */
@Composable
fun CrewReviewScreen(
    viewModel: CrewScreenViewModel,
    applicationId: Long,
    onBack: () -> Unit,
    onApplicantProfile: (String) -> Unit,
    onDecided: (approved: Boolean, name: String, members: Int, capacity: Int?, pending: Int) -> Unit,
    onRecruitSettings: () -> Unit,
) {
    val ink = crewInk()
    val words = rememberCrewWords()
    val card by viewModel.card.collectAsStateWithLifecycle()
    val application by viewModel.application.collectAsStateWithLifecycle()
    val op by viewModel.op.collectAsStateWithLifecycle()
    var sheet by rememberSaveable { mutableStateOf("") }
    LaunchedEffect(applicationId) { viewModel.loadApplication(applicationId) }
    val loaded = (application as? CrewLoad.Ready)?.value
    LaunchedEffect(op) {
        val decision = op.decision()
        if ((op.op == CrewOp.APPROVE || op.op == CrewOp.DECLINE) && op.done && decision != null) {
            viewModel.consumeOp()
            sheet = ""
            onDecided(decision.approved, loaded?.name.orEmpty(), decision.memberCount, decision.capacity, decision.pendingCount)
        } else if ((op.op == CrewOp.APPROVE || op.op == CrewOp.DECLINE) && op.problem != null) {
            sheet = if (op.problem == CrewProblem.CREW_FULL) SHEET_FULL else SHEET_ERROR
        }
    }
    val crew = card
    val decided = loaded != null && loaded.status != CrewApplicationStatus.PENDING
    CrewPage(Modifier.testTag("crew-review")) {
        CrewTopBar(stringResource(R.string.crew_review_title), onBack)
        when (val state = application) {
            CrewLoad.Loading -> Column(Modifier.padding(CrewGutter)) { CrewSkeletonBox(Modifier.fillMaxWidth().height(240.dp), 18.dp) }
            is CrewLoad.Failed -> CrewEmptyState(icon = {}, title = stringResource(R.string.crew_requests_failed), body = "", modifier = Modifier.padding(top = 120.dp)) {
                CrewButton(stringResource(R.string.crew_list_reload), { viewModel.loadApplication(applicationId) })
            }
            is CrewLoad.Ready -> {
                val app = state.value
                Column(Modifier.weight(1f).fillMaxWidth().verticalScroll(rememberScrollState()).padding(horizontal = CrewGutter)) {
                    Column(Modifier.fillMaxWidth().padding(top = 40.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                        CrewAvatar(app.name, 76.dp, ink.avatar, ink.avatarText)
                        Spacer(Modifier.height(18.dp))
                        Text(app.name, color = ink.text, fontSize = 26.sp, fontWeight = FontWeight.SemiBold, modifier = Modifier.testTag("crew-review-name"))
                        Spacer(Modifier.height(10.dp))
                        Text(stringResource(R.string.crew_applied_on, words.date(app.createdAt)), color = ink.secondary, fontSize = 13.5.sp)
                    }
                    Spacer(Modifier.height(56.dp))
                    CrewSentMessage(app, words, label = stringResource(R.string.crew_review_message_label))
                    Spacer(Modifier.height(20.dp))
                    CrewRow(stringResource(R.string.crew_review_profile), { onApplicantProfile(app.userId) }, Modifier.testTag("crew-review-profile"))
                    Spacer(Modifier.height(24.dp))
                }
                CrewBottomBar {
                    // 승인하면 몇 명이 되는지 — 작은 화면에서도 보이게 버튼 바로 위에
                    Text(
                        when {
                            decided -> stringResource(R.string.crew_review_decided)
                            crew?.full == true -> stringResource(R.string.crew_review_full_note)
                            crew != null -> stringResource(R.string.crew_review_note, crew.memberCount, crew.memberCount + 1)
                            else -> ""
                        },
                        color = if (decided || crew?.full == true) ink.warn else ink.secondary, fontSize = 13.sp,
                        modifier = Modifier.fillMaxWidth().padding(bottom = 14.dp).testTag("crew-review-note"),
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(18.dp)) {
                        CrewButton(
                            stringResource(R.string.crew_review_decline), { sheet = SHEET_DECLINE }, Modifier.weight(1f).testTag("crew-review-decline"),
                            CrewButtonKind.SECONDARY, enabled = !decided,
                        )
                        CrewButton(
                            stringResource(R.string.crew_review_approve), { sheet = SHEET_APPROVE }, Modifier.weight(1f).testTag("crew-review-approve"),
                            enabled = !decided,
                        )
                    }
                }
            }
        }
    }
    val name = loaded?.name.orEmpty()
    when (sheet) {
        SHEET_APPROVE -> CrewConfirmSheet(
            title = stringResource(R.string.crew_approve_title, name),
            body = stringResource(R.string.crew_approve_body, crew?.name.orEmpty(), crew?.memberCount ?: 0, (crew?.memberCount ?: 0) + 1),
            confirm = stringResource(R.string.crew_review_approve),
            busy = op.op == CrewOp.APPROVE && op.running,
            tag = "crew-approve",
            onConfirm = { viewModel.decide(applicationId, true) },
            onDismiss = { if (!op.running) sheet = "" },
        )
        SHEET_DECLINE -> CrewConfirmSheet(
            title = stringResource(R.string.crew_decline_title),
            body = stringResource(R.string.crew_decline_body, name),
            confirm = stringResource(R.string.crew_review_decline),
            danger = true,
            busy = op.op == CrewOp.DECLINE && op.running,
            tag = "crew-decline",
            onConfirm = { viewModel.decide(applicationId, false) },
            onDismiss = { if (!op.running) sheet = "" },
        )
        SHEET_FULL -> CrewSheet(stringResource(R.string.crew_full_title), { viewModel.consumeOp(); sheet = "" }, Modifier.testTag("crew-approve-full")) {
            CrewSheetBody(stringResource(R.string.crew_full_body))
            Spacer(Modifier.height(90.dp))
            CrewButton(stringResource(R.string.crew_full_capacity), { viewModel.consumeOp(); sheet = ""; onRecruitSettings() }, Modifier.testTag("crew-approve-full-capacity"))
            Spacer(Modifier.height(16.dp))
            CrewButton(stringResource(R.string.crew_full_back), { viewModel.consumeOp(); sheet = ""; viewModel.reload() }, kind = CrewButtonKind.SECONDARY)
        }
        SHEET_ERROR -> CrewActionErrorSheet(
            onRecheck = {
                viewModel.consumeOp()
                sheet = ""
                viewModel.reload()
                viewModel.loadApplication(applicationId)
            },
            onDismiss = { viewModel.consumeOp(); sheet = "" },
            body = if (op.problem == CrewProblem.NETWORK || op.problem == CrewProblem.SIGN_IN) crewProblemText(op.problem!!) else null,
        )
    }
}

private const val SHEET_APPROVE = "approve"
private const val SHEET_DECLINE = "decline"
private const val SHEET_FULL = "full"
private const val SHEET_ERROR = "error"

/** 51 가입 승인 완료 · 53 미승인 처리 완료 — 서버가 돌려준 인원 · 남은 신청 수로 */
@Composable
fun CrewDecidedScreen(
    approved: Boolean,
    name: String,
    crewName: String,
    members: Int,
    capacity: Int?,
    pending: Int,
    onBack: () -> Unit,
    onRemaining: () -> Unit,
) {
    if (approved) {
        CrewResultPage(
            title = stringResource(R.string.crew_approved_done_title, name),
            body = (if (capacity != null) stringResource(R.string.crew_approved_done_members, crewName, members, capacity)
            else stringResource(R.string.crew_approved_done_members_only, crewName, members)) + "\n" +
                stringResource(R.string.crew_remaining_requests, pending),
            button = stringResource(R.string.crew_remaining_button),
            onButton = onRemaining,
            onBack = onBack,
            modifier = Modifier.testTag("crew-decided-approved"),
        )
    } else {
        CrewResultPage(
            title = stringResource(R.string.crew_declined_done_title),
            body = stringResource(R.string.crew_declined_done_body, members),
            button = stringResource(R.string.crew_remaining_button),
            onButton = onRemaining,
            onBack = onBack,
            modifier = Modifier.testTag("crew-decided-declined"),
        )
    }
}

/** 운영 설정에서 나가는 곳 */
class CrewSettingsActions(
    val onBack: () -> Unit,
    val onTransfer: () -> Unit,
    /** 모집을 멈췄다(89) · 다시 시작했다(76) — 상세로 */
    val onRecruitChanged: () -> Unit,
    /** 해산했다 — 목록으로 */
    val onDissolved: () -> Unit,
)

/**
 * 74 운영 설정(모집 중) · 90 모집 중지 상태의 운영 설정 — 멈춤과 재개 중 지금 상태에 맞는 하나만 보인다.
 * 크루장 탈퇴는 다른 멤버가 있으면 먼저 크루장을 정하고(62), 혼자면 해산(63)으로 잇는다.
 * 예전의 가입 방식(바로 가입 · 크루장 확인)은 지우지 않고 여기 안쪽에 둔다.
 */
@Composable
fun CrewSettingsScreen(viewModel: CrewScreenViewModel, actions: CrewSettingsActions) {
    val ink = crewInk()
    val card by viewModel.card.collectAsStateWithLifecycle()
    val op by viewModel.op.collectAsStateWithLifecycle()
    var sheet by rememberSaveable { mutableStateOf("") }
    LaunchedEffect(op) {
        if (!op.done) return@LaunchedEffect
        when (op.op) {
            CrewOp.PAUSE, CrewOp.RESUME -> { viewModel.consumeOp(); sheet = ""; actions.onRecruitChanged() }
            CrewOp.DISSOLVE -> { viewModel.consumeOp(); sheet = ""; actions.onDissolved() }
            CrewOp.POLICY -> { viewModel.consumeOp(); sheet = "" }
            else -> Unit
        }
    }
    val crew = card
    CrewPage(Modifier.testTag("crew-settings")) {
        CrewTopBar(stringResource(R.string.crew_settings_title), actions.onBack)
        if (crew == null || crew.role != CrewRole.OWNER) return@CrewPage
        Column(Modifier.weight(1f).fillMaxWidth().verticalScroll(rememberScrollState()).padding(horizontal = CrewGutter)) {
            Spacer(Modifier.height(18.dp))
            CrewIdentityStrip(crew)
            Spacer(Modifier.height(30.dp))
            Text(
                stringResource(if (crew.recruiting) R.string.crew_settings_open else R.string.crew_settings_paused),
                color = ink.info, fontSize = 13.5.sp, modifier = Modifier.testTag("crew-settings-state"),
            )
            Spacer(Modifier.height(10.dp))
            if (crew.recruiting) {
                CrewRow(stringResource(R.string.crew_settings_pause), { sheet = SHEET_PAUSE }, Modifier.testTag("crew-settings-pause"))
            } else {
                CrewRow(stringResource(R.string.crew_settings_resume), { sheet = SHEET_RESUME }, Modifier.testTag("crew-settings-resume"))
            }
            CrewRow(stringResource(R.string.crew_transfer_row), actions.onTransfer, Modifier.testTag("crew-settings-transfer"))
            CrewRow(
                stringResource(R.string.crew_menu_leave), { sheet = if (crew.memberCount > 1) SHEET_EXIT else SHEET_DISSOLVE },
                Modifier.testTag("crew-settings-leave"),
            )
            CrewRow(stringResource(R.string.crew_settings_dissolve), { sheet = SHEET_DISSOLVE }, Modifier.testTag("crew-settings-dissolve"), titleColor = ink.warn)
            CrewRow(
                stringResource(R.string.crew_settings_policy), { sheet = SHEET_POLICY }, Modifier.testTag("crew-settings-policy"),
                value = stringResource(if (crew.openJoin) R.string.crew_policy_open else R.string.crew_policy_approval),
            )
            Spacer(Modifier.height(100.dp))
            Text(stringResource(R.string.crew_settings_note), color = ink.secondary, fontSize = 13.5.sp)
            Spacer(Modifier.height(24.dp))
        }
    }
    val failed = op.problem != null
    when (sheet) {
        SHEET_PAUSE -> CrewConfirmSheet(
            title = stringResource(R.string.crew_pause_title),
            body = stringResource(R.string.crew_pause_body),
            confirm = stringResource(R.string.crew_pause_confirm),
            busy = op.op == CrewOp.PAUSE && op.running,
            error = if (op.op == CrewOp.PAUSE && failed) crewProblemText(op.problem!!) else null,
            tag = "crew-pause",
            onConfirm = { viewModel.setRecruiting(false) },
            onDismiss = { if (!op.running) { sheet = ""; viewModel.consumeOp() } },
        )
        SHEET_RESUME -> crew?.let {
            CrewConfirmSheet(
                title = stringResource(R.string.crew_resume_recruit_title),
                body = when {
                    it.capacity == null -> stringResource(R.string.crew_resume_recruit_body_open)
                    it.full -> stringResource(R.string.crew_resume_recruit_body_full, it.memberCount, it.capacity)
                    else -> stringResource(R.string.crew_resume_recruit_body, it.memberCount, it.capacity, it.capacity - it.memberCount)
                },
                confirm = stringResource(R.string.crew_resume_recruit_confirm),
                busy = op.op == CrewOp.RESUME && op.running,
                error = if (op.op == CrewOp.RESUME && failed) crewProblemText(op.problem!!) else null,
                tag = "crew-resume-recruit",
                onConfirm = { viewModel.setRecruiting(true) },
                onDismiss = { if (!op.running) { sheet = ""; viewModel.consumeOp() } },
            )
        }
        SHEET_EXIT -> CrewSheet(stringResource(R.string.crew_owner_exit_title), { sheet = "" }, Modifier.testTag("crew-owner-exit")) {
            CrewSheetBody(stringResource(R.string.crew_owner_exit_body))
            Spacer(Modifier.height(110.dp))
            CrewButton(stringResource(R.string.crew_owner_exit_pick), { sheet = ""; actions.onTransfer() }, Modifier.testTag("crew-owner-exit-pick"))
        }
        SHEET_DISSOLVE -> crew?.let {
            CrewConfirmSheet(
                title = stringResource(R.string.crew_dissolve_title, withParticle(it.name, "을", "를")),
                body = stringResource(R.string.crew_dissolve_body),
                confirm = stringResource(R.string.crew_settings_dissolve),
                danger = true,
                busy = op.op == CrewOp.DISSOLVE && op.running,
                error = if (op.op == CrewOp.DISSOLVE && failed) crewProblemText(op.problem!!) else null,
                tag = "crew-dissolve",
                onConfirm = { viewModel.dissolve() },
                onDismiss = { if (!op.running) { sheet = ""; viewModel.consumeOp() } },
            )
        }
        SHEET_POLICY -> crew?.let { CrewPolicySheet(it.openJoin, busy = op.op == CrewOp.POLICY && op.running, onApply = viewModel::setOpenJoin) { sheet = ""; viewModel.consumeOp() } }
    }
}

private const val SHEET_PAUSE = "pause"
private const val SHEET_RESUME = "resume"
private const val SHEET_EXIT = "exit"
private const val SHEET_DISSOLVE = "dissolve"
private const val SHEET_POLICY = "policy"

/** 가입 방식(예전 기능) — 크루장 확인 · 바로 가입 */
@Composable
private fun CrewPolicySheet(open: Boolean, busy: Boolean, onApply: (Boolean) -> Unit, onDismiss: () -> Unit) {
    var picked by rememberSaveable { mutableStateOf(open) }
    CrewSheet(stringResource(R.string.crew_settings_policy), onDismiss, Modifier.testTag("crew-policy-sheet"), dismissible = !busy) {
        Spacer(Modifier.height(14.dp))
        CrewRow(
            stringResource(R.string.crew_policy_approval_long), { picked = false }, Modifier.testTag("crew-policy-approval"),
            value = if (!picked) stringResource(R.string.crew_selected) else null,
        )
        CrewRow(
            stringResource(R.string.crew_policy_open_long), { picked = true }, Modifier.testTag("crew-policy-open"),
            value = if (picked) stringResource(R.string.crew_selected) else null,
        )
        Spacer(Modifier.height(40.dp))
        CrewButton(stringResource(R.string.crew_apply_choice), { onApply(picked) }, Modifier.testTag("crew-policy-apply"), busy = busy, enabled = picked != open)
    }
}

/** 크루장이 크루를 떠나거나 넘긴 뒤 비어 있는 자리(안내 한 줄) */
@Composable
internal fun CrewPlainNote(text: String) {
    val ink = crewInk()
    Text(text, color = ink.secondary, fontSize = 13.sp, textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth().padding(24.dp))
}

@Suppress("unused")
private fun Modifier.crewDim(): Modifier = this.background(androidx.compose.ui.graphics.Color.Transparent)
