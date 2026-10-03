package com.stepup.android.ui.screens.community.crew

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.Icon
import androidx.compose.ui.semantics.Role
import com.stepup.android.ui.experience.feedbackClickable
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.stepup.android.R
import com.stepup.android.domain.CrewCard
import com.stepup.android.domain.CrewMember
import com.stepup.android.domain.CrewPerson
import com.stepup.android.domain.CrewPersonRole
import com.stepup.android.domain.CrewRole
import com.stepup.android.domain.CrewRules

/** 09 대표 이미지 크게 보기 — 목록 · 상세와 같은 이미지 */
@Composable
fun CrewImageScreen(viewModel: CrewScreenViewModel, onBack: () -> Unit) {
    val ink = crewInk()
    val card by viewModel.card.collectAsStateWithLifecycle()
    CrewPage(Modifier.testTag("crew-image-view")) {
        CrewTopBar(stringResource(R.string.crew_image_title), onBack)
        Column(
            Modifier.weight(1f).fillMaxWidth().verticalScroll(rememberScrollState()).padding(horizontal = CrewGutter),
            verticalArrangement = Arrangement.Center, horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Spacer(Modifier.height(48.dp))
            card?.let { crew ->
                androidx.compose.foundation.layout.BoxWithConstraints(Modifier.fillMaxWidth()) {
                    CrewImage(crew, maxWidth, 26.dp, textSize = (maxWidth.value * 0.2f).sp)
                }
                Spacer(Modifier.height(36.dp))
                Text(crew.name, style = crewTitleStyle(ink.text, 30.sp), textAlign = TextAlign.Center)
                if (crew.tagline.isNotBlank()) {
                    Spacer(Modifier.height(10.dp))
                    Text(crew.tagline, color = ink.text.copy(alpha = 0.86f), fontSize = 17.sp, textAlign = TextAlign.Center)
                }
            }
        }
        CrewBottomBar { CrewButton(stringResource(R.string.common_close), onBack, Modifier.testTag("crew-image-close")) }
    }
}

/**
 * 10 크루장 프로필 — 이 크루의 크루장 이름과 역할, 크루장이 남긴 한마디, 크루의 러닝 스타일(거리 · 분위기).
 * 선택한 크루의 크루장을 보인다(다른 크루의 예시로 바뀌지 않는다).
 */
@Composable
fun CrewLeaderScreen(viewModel: CrewScreenViewModel, onBack: () -> Unit, onOpenCrew: () -> Unit) {
    val ink = crewInk()
    val words = rememberCrewWords()
    val card by viewModel.card.collectAsStateWithLifecycle()
    CrewPage(Modifier.testTag("crew-leader")) {
        CrewTopBar(stringResource(R.string.crew_leader_title), onBack)
        val crew = card ?: return@CrewPage
        Column(Modifier.weight(1f).fillMaxWidth().verticalScroll(rememberScrollState()).padding(horizontal = CrewGutter)) {
            // 선택한 크루의 크루장 — 연락처 · 메시지 기능은 새로 만들지 않는다
            Spacer(Modifier.height(18.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                CrewAvatar(crew.leaderName, 88.dp, leaderFace(crew), ink.text)
                Column(Modifier.weight(1f).padding(start = 20.dp)) {
                    Text(crew.leaderName, style = crewTitleStyle(ink.text, 30.sp), maxLines = 2, modifier = Modifier.testTag("crew-person-name"))
                    Text(stringResource(R.string.crew_leader_of, crew.name), color = ink.text.copy(alpha = 0.86f), fontSize = 16.sp, modifier = Modifier.testTag("crew-person-sub"))
                }
            }
            val note = crew.leaderNote.trim()
            if (note.isNotEmpty()) {
                val lines = note.lines()
                Spacer(Modifier.height(30.dp))
                Text(lines.first(), style = crewTitleStyle(ink.text, 26.sp), modifier = Modifier.testTag("crew-leader-note"))
                if (lines.size > 1) {
                    Spacer(Modifier.height(10.dp))
                    Text(lines.drop(1).joinToString("\n"), color = ink.text, fontSize = 17.sp, lineHeight = 26.sp)
                }
            }
            Spacer(Modifier.height(32.dp))
            Text(stringResource(R.string.crew_leader_runs), color = ink.text, fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
            Spacer(Modifier.height(10.dp))
            Row(
                Modifier.fillMaxWidth().crewPanel(ink, 16.dp, selected = false).feedbackClickable(role = Role.Button, onClick = onOpenCrew)
                    .padding(14.dp).testTag("crew-leader-crew"),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                CrewImage(crew, 72.dp, 14.dp)
                Text(
                    crew.name, color = ink.text, fontSize = 20.sp, fontWeight = FontWeight.Bold, maxLines = 2,
                    modifier = Modifier.weight(1f).padding(start = 16.dp),
                )
                Icon(Icons.Filled.ChevronRight, null, tint = ink.link, modifier = Modifier.size(24.dp))
            }
            val chips = words.styleChips(crew)
            if (chips.isNotEmpty()) {
                Spacer(Modifier.height(26.dp))
                Text(stringResource(R.string.crew_style_title), color = ink.text, fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
                Spacer(Modifier.height(10.dp))
                CrewChipFlow(chips)
            }
            Spacer(Modifier.height(32.dp))
        }
    }
}

/** 동그라미 · 이름 · 한 줄 */
@Composable
private fun CrewPersonHead(name: String, sub: String?, subColor: androidx.compose.ui.graphics.Color? = null) {
    val ink = crewInk()
    Column(Modifier.fillMaxWidth().padding(top = 32.dp), horizontalAlignment = Alignment.CenterHorizontally) {
        CrewAvatar(name, 92.dp, ink.avatar, ink.avatarText)
        Spacer(Modifier.height(16.dp))
        Text(name, style = crewTitleStyle(ink.text, 30.sp), textAlign = TextAlign.Center, modifier = Modifier.testTag("crew-person-name"))
        if (sub != null) {
            Spacer(Modifier.height(6.dp))
            Text(sub, color = subColor ?: ink.info, fontSize = 17.sp, fontWeight = FontWeight.SemiBold, textAlign = TextAlign.Center, modifier = Modifier.testTag("crew-person-sub"))
        }
    }
}

/** 러닝 스타일 칩 — 누르지 않는 표시(옅은 파랑 면) */
@Composable
private fun CrewStyleChips(chips: List<String>) {
    CrewChipFlow(chips)
}

/**
 * 한 사람 — 멤버 공개 프로필(56) · 크루장이 보는 멤버(84, 더보기 → 75) · 신청자(85, 승인 전 멤버로 보이지 않는다).
 * 공개 가능한 정보만: 이름 · 이 크루에서의 역할 · 함께한 날 · 이번 주 크루 러닝 거리. 크루장 본인은 내보내기 대상이 아니다.
 */
@Composable
fun CrewPersonScreen(
    viewModel: CrewScreenViewModel,
    userId: String,
    onBack: () -> Unit,
    onOpenCrew: () -> Unit,
    onTransfer: (userId: String, name: String) -> Unit,
    /** 57 내보내기가 끝났다 — 멤버 관리(55)로 */
    onRemoved: () -> Unit,
) {
    val ink = crewInk()
    val words = rememberCrewWords()
    val card by viewModel.card.collectAsStateWithLifecycle()
    val person by viewModel.person.collectAsStateWithLifecycle()
    var menu by rememberSaveable { mutableStateOf(false) }
    var removing by rememberSaveable { mutableStateOf(false) }
    LaunchedEffect(userId) { viewModel.loadPerson(userId) }
    val loaded = (person as? CrewLoad.Ready)?.value
    val viewerOwner = card?.role == CrewRole.OWNER
    val manageable = viewerOwner && loaded?.role == CrewPersonRole.MEMBER
    CrewPage(Modifier.testTag("crew-person")) {
        CrewTopBar(
            stringResource(if (loaded?.role == CrewPersonRole.APPLICANT) R.string.crew_applicant_title else R.string.crew_member_title),
            onBack,
            onMore = if (manageable) ({ menu = true }) else null,
        )
        when (val state = person) {
            CrewLoad.Loading -> Column(Modifier.padding(CrewGutter)) { CrewSkeletonBox(Modifier.fillMaxWidth().height(160.dp), 18.dp) }
            is CrewLoad.Failed -> CrewEmptyState(icon = { CrewStateIcon(Icons.Filled.Refresh) }, title = stringResource(R.string.crew_person_failed), body = "", modifier = Modifier.padding(top = 120.dp)) {
                CrewButton(stringResource(R.string.crew_list_reload), { viewModel.loadPerson(userId) })
            }
            is CrewLoad.Ready -> CrewPersonBody(state.value, card, words, onOpenCrew, Modifier.weight(1f))
        }
    }
    if (menu && loaded != null) {
        CrewMenuSheet(
            stringResource(R.string.crew_member_manage_title, loaded.name),
            listOf(
                CrewMenuItem(stringResource(R.string.crew_member_make_leader), "crew-member-make-leader") { menu = false; onTransfer(loaded.userId, loaded.name) },
                CrewMenuItem(stringResource(R.string.crew_member_remove), "crew-member-remove", warn = true) { menu = false; removing = true },
            ),
            onDismiss = { menu = false },
            tag = "crew-member-actions",
        )
    }
    if (removing && loaded != null) {
        CrewRemoveSheet(viewModel, loaded.userId, loaded.name, onDone = { removing = false; onRemoved() }, onDismiss = { removing = false })
    }
}

@Composable
private fun CrewPersonBody(person: CrewPerson, card: CrewCard?, words: CrewWords, onOpenCrew: () -> Unit, modifier: Modifier) {
    val ink = crewInk()
    val crewName = card?.name.orEmpty()
    Column(modifier.fillMaxWidth().verticalScroll(rememberScrollState()).padding(horizontal = CrewGutter)) {
        val sub = when (person.role) {
            CrewPersonRole.OWNER -> stringResource(R.string.crew_leader_of, crewName)
            CrewPersonRole.MEMBER -> stringResource(R.string.crew_member_of, crewName)
            CrewPersonRole.APPLICANT -> person.application?.let { stringResource(R.string.crew_applied_on, words.date(it.createdAt)) }
            CrewPersonRole.NONE -> stringResource(R.string.crew_person_not_member)
        }
        CrewPersonHead(person.name, sub, if (person.role == CrewPersonRole.APPLICANT || person.role == CrewPersonRole.NONE) ink.text.copy(alpha = 0.86f) else null)
        Spacer(Modifier.height(30.dp))
        when (person.role) {
            CrewPersonRole.APPLICANT -> {
                // 85 아직 멤버가 아니다 — 신청 날짜 · 고른 문구만(가입일 · 주간 거리 · 관리 메뉴 없음)
                val phrases = person.application?.phrases.orEmpty()
                if (phrases.isNotEmpty()) {
                    Text(stringResource(R.string.crew_applicant_phrases), color = ink.text, fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
                    Spacer(Modifier.height(10.dp))
                    Column(
                        Modifier.fillMaxWidth().crewPanel(ink, 16.dp).padding(horizontal = 20.dp, vertical = 20.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp),
                    ) {
                        phrases.forEach { Text(words.phrase(it), color = ink.info, fontSize = 19.sp, fontWeight = FontWeight.ExtraBold) }
                    }
                    Spacer(Modifier.height(14.dp))
                }
                val noteLines = stringResource(R.string.crew_applicant_note, crewName).lines()
                Column(
                    Modifier.fillMaxWidth().crewPanel(ink, 16.dp).padding(horizontal = 20.dp, vertical = 20.dp).testTag("crew-applicant-note"),
                    verticalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    Text(noteLines.first(), color = ink.text, fontSize = 18.sp, fontWeight = FontWeight.ExtraBold)
                    if (noteLines.size > 1) Text(noteLines.drop(1).joinToString("\n"), color = ink.text.copy(alpha = 0.86f), fontSize = 16.sp, lineHeight = 24.sp)
                }
            }
            CrewPersonRole.OWNER, CrewPersonRole.MEMBER -> {
                Column(
                    Modifier.fillMaxWidth().crewPanel(ink, 16.dp).padding(horizontal = 20.dp, vertical = 20.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    person.joinedAt?.takeIf { it > 0 }?.let {
                        Text(stringResource(R.string.crew_member_since, words.date(it)), color = ink.text, fontSize = 18.sp, fontWeight = FontWeight.ExtraBold)
                    }
                    Text(
                        if (person.weekKm > 0) stringResource(R.string.crew_member_week_km, CrewRules.km(person.weekKm))
                        else stringResource(R.string.crew_member_week_none),
                        color = if (person.weekKm > 0) ink.text else ink.secondary, fontSize = 16.sp,
                    )
                }
                Spacer(Modifier.height(14.dp))
                CrewMenuRow(stringResource(R.string.crew_member_crew), onOpenCrew, Modifier.testTag("crew-person-crew"), value = crewName)
            }
            CrewPersonRole.NONE -> Unit
        }
        Spacer(Modifier.height(28.dp))
        Text(stringResource(R.string.crew_person_public_only), color = ink.text.copy(alpha = 0.8f), fontSize = 15.sp)
        Spacer(Modifier.height(24.dp))
    }
}

/** 명단 종류 — 12 전체 멤버 · 55 멤버 관리(크루장) · 82 이번 주 목표에 참여한 멤버 */
enum class CrewRosterMode { MEMBERS, MANAGE, WEEK }

/**
 * 12 · 55 · 82 — 같은 명단 틀. 82 는 이번 주 크루 러닝이 있는 멤버만(전체 명단과 섞지 않는다).
 * 크루장 줄은 크루장 프로필로, 멤버 줄은 공개 프로필(크루장이 보면 관리 가능한 프로필)로 간다.
 */
@Composable
fun CrewRosterScreen(
    viewModel: CrewScreenViewModel,
    mode: CrewRosterMode,
    onBack: () -> Unit,
    onOpenLeader: () -> Unit,
    onOpenMember: (String) -> Unit,
    onTransfer: () -> Unit,
) {
    val ink = crewInk()
    val card by viewModel.card.collectAsStateWithLifecycle()
    val roster by viewModel.roster.collectAsStateWithLifecycle()
    LaunchedEffect(Unit) { viewModel.loadRoster() }
    val crew = card
    val members = (roster as? CrewLoad.Ready)?.value.orEmpty()
    val shown = if (mode == CrewRosterMode.WEEK) members.filter { it.weekKm > 0 }.sortedByDescending { it.weekKm } else members
    CrewPage(Modifier.testTag(if (mode == CrewRosterMode.WEEK) "crew-participants" else if (mode == CrewRosterMode.MANAGE) "crew-members-manage" else "crew-members")) {
        CrewTopBar(
            stringResource(
                when (mode) {
                    CrewRosterMode.MEMBERS -> R.string.crew_members_title
                    CrewRosterMode.MANAGE -> R.string.crew_members_manage_title
                    CrewRosterMode.WEEK -> R.string.crew_participants_title
                },
            ),
            onBack,
        )
        LazyColumn(Modifier.weight(1f).fillMaxWidth(), contentPadding = PaddingValues(start = CrewGutter, end = CrewGutter, bottom = 32.dp)) {
            item {
                Spacer(Modifier.height(14.dp))
                val count = if (mode == CrewRosterMode.WEEK) shown.size else crew?.memberCount ?: members.size
                if (mode == CrewRosterMode.WEEK) {
                    Text(stringResource(R.string.crew_goal_participants), color = ink.info, fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
                    Spacer(Modifier.height(4.dp))
                }
                Text(
                    stringResource(if (mode == CrewRosterMode.WEEK) R.string.crew_participants_heading else R.string.crew_members_heading, count),
                    style = crewTitleStyle(ink.text, 30.sp), modifier = Modifier.testTag("crew-roster-heading"),
                )
                Spacer(Modifier.height(6.dp))
                val sub = when {
                    crew == null -> ""
                    mode == CrewRosterMode.WEEK -> stringResource(R.string.crew_participants_sub, crew.name, CrewRules.km(shown.sumOf { it.weekKm }))
                    crew.capacity != null -> stringResource(R.string.crew_members_sub_capacity, crew.name, crew.capacity)
                    else -> crew.name
                }
                Text(sub, color = ink.text.copy(alpha = 0.86f), fontSize = 17.sp)
                Spacer(Modifier.height(if (mode == CrewRosterMode.MANAGE) 14.dp else 26.dp))
                if (mode == CrewRosterMode.MANAGE) {
                    CrewRow(stringResource(R.string.crew_transfer_row), onTransfer, Modifier.testTag("crew-transfer-row"))
                    Spacer(Modifier.height(10.dp))
                } else if (mode == CrewRosterMode.MEMBERS) {
                    Text(stringResource(R.string.crew_members_label), color = ink.text, fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
                    Spacer(Modifier.height(4.dp))
                }
            }
            when (val state = roster) {
                CrewLoad.Loading -> item { CrewSkeletonBox(Modifier.fillMaxWidth().height(160.dp), 16.dp) }
                is CrewLoad.Failed -> item {
                    Column(Modifier.padding(top = 20.dp)) {
                        CrewErrorLine(stringResource(R.string.crew_roster_failed))
                        Spacer(Modifier.height(16.dp))
                        CrewButton(stringResource(R.string.crew_list_reload), { viewModel.loadRoster() }, kind = CrewButtonKind.SECONDARY)
                    }
                }
                is CrewLoad.Ready -> {
                    if (shown.isEmpty()) item {
                        Text(
                            stringResource(if (mode == CrewRosterMode.WEEK) R.string.crew_participants_none else R.string.crew_roster_empty),
                            color = ink.secondary, fontSize = 16.sp, modifier = Modifier.padding(top = 12.dp).testTag("crew-roster-empty"),
                        )
                    }
                    items(shown, key = { it.userId }) { member ->
                        CrewPersonRow(
                            name = member.name,
                            sub = rosterSub(member, mode),
                            subColor = if (mode == CrewRosterMode.WEEK) ink.text else null,
                            onClick = { if (member.owner) onOpenLeader() else onOpenMember(member.userId) },
                            modifier = Modifier.testTag("crew-roster-${member.userId}"),
                            badge = if (member.owner) stringResource(R.string.crew_role_leader) else null,
                        )
                    }
                    if (mode == CrewRosterMode.WEEK && crew?.goalKm != null) item {
                        Spacer(Modifier.height(24.dp))
                        Text(stringResource(R.string.crew_goal_gathered), color = ink.text.copy(alpha = 0.8f), fontSize = 15.sp, textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth())
                    }
                }
            }
        }
    }
}

@Composable
private fun rosterSub(member: CrewMember, mode: CrewRosterMode): String = when {
    mode == CrewRosterMode.WEEK && member.owner -> stringResource(R.string.crew_km_value, CrewRules.km(member.weekKm))
    mode == CrewRosterMode.WEEK -> stringResource(R.string.crew_km_value, CrewRules.km(member.weekKm))
    member.owner -> stringResource(R.string.crew_role_leader)
    else -> stringResource(R.string.crew_role_member)
}

/**
 * 58 새 크루장 선택 · 59 크루장 변경 확인 — 지금 멤버만 후보다(신청 대기자는 아니다). 바꾸면 나는 일반 멤버로 남고,
 * 성공한 뒤 상세(86)에서 새 크루장과 내 권한이 바로 바뀐다.
 */
@Composable
fun CrewTransferScreen(
    viewModel: CrewScreenViewModel,
    presetUserId: String?,
    presetName: String?,
    onBack: () -> Unit,
    onDone: () -> Unit,
) {
    val ink = crewInk()
    val roster by viewModel.roster.collectAsStateWithLifecycle()
    val op by viewModel.op.collectAsStateWithLifecycle()
    var target by rememberSaveable { mutableStateOf(presetUserId?.let { "$it|${presetName.orEmpty()}" } ?: "") }
    LaunchedEffect(Unit) { viewModel.loadRoster() }
    LaunchedEffect(op) {
        if (op.op == CrewOp.TRANSFER && op.done) {
            viewModel.consumeOp()
            target = ""
            onDone()
        }
    }
    val candidates = (roster as? CrewLoad.Ready)?.value.orEmpty().filterNot { it.owner }
    CrewPage(Modifier.testTag("crew-transfer")) {
        CrewTopBar(stringResource(R.string.crew_transfer_title), onBack)
        LazyColumn(Modifier.weight(1f).fillMaxWidth(), contentPadding = PaddingValues(start = CrewGutter, end = CrewGutter, bottom = 32.dp)) {
            item {
                Spacer(Modifier.height(18.dp))
                CrewHeading(stringResource(R.string.crew_transfer_heading), sub = stringResource(R.string.crew_transfer_sub))
                Spacer(Modifier.height(18.dp))
            }
            when (val state = roster) {
                CrewLoad.Loading -> item { CrewSkeletonBox(Modifier.fillMaxWidth().height(160.dp), 16.dp) }
                is CrewLoad.Failed -> item {
                    CrewButton(stringResource(R.string.crew_list_reload), { viewModel.loadRoster() }, kind = CrewButtonKind.SECONDARY)
                }
                is CrewLoad.Ready -> {
                    if (candidates.isEmpty()) item {
                        Text(stringResource(R.string.crew_transfer_none), color = ink.secondary, fontSize = 16.sp, lineHeight = 24.sp, modifier = Modifier.testTag("crew-transfer-none"))
                    }
                    items(candidates, key = { it.userId }) { member ->
                        CrewPersonRow(member.name, stringResource(R.string.crew_role_member), { target = "${member.userId}|${member.name}" },
                            Modifier.testTag("crew-transfer-${member.userId}"))
                    }
                }
            }
        }
    }
    if (target.isNotEmpty()) {
        val (id, name) = target.split('|', limit = 2).let { it[0] to it.getOrElse(1) { "" } }
        CrewConfirmSheet(
            title = stringResource(R.string.crew_transfer_confirm_title, name),
            body = stringResource(R.string.crew_transfer_confirm_body, name),
            confirm = stringResource(R.string.crew_transfer_confirm),
            busy = op.op == CrewOp.TRANSFER && op.running,
            error = if (op.op == CrewOp.TRANSFER && op.problem != null) crewProblemText(op.problem!!) else null,
            tag = "crew-transfer-confirm",
            onConfirm = { viewModel.transfer(id) },
            onDismiss = { if (!op.running) { target = ""; if (op.op == CrewOp.TRANSFER) viewModel.consumeOp() } },
        )
    }
}

/** 57 멤버 내보내기 확인 — 성공한 뒤 명단과 인원을 다시 읽는다 */
@Composable
fun CrewRemoveSheet(viewModel: CrewScreenViewModel, userId: String, name: String, onDone: () -> Unit, onDismiss: () -> Unit) {
    val op by viewModel.op.collectAsStateWithLifecycle()
    LaunchedEffect(op) {
        if (op.op == CrewOp.REMOVE && op.done) {
            viewModel.consumeOp()
            viewModel.loadRoster()
            onDone()
        }
    }
    CrewConfirmSheet(
        title = stringResource(R.string.crew_remove_title, name),
        body = stringResource(R.string.crew_remove_body, name),
        confirm = stringResource(R.string.crew_remove_confirm),
        danger = true,
        busy = op.op == CrewOp.REMOVE && op.running,
        error = if (op.op == CrewOp.REMOVE && op.problem != null) crewProblemText(op.problem!!) else null,
        tag = "crew-remove",
        onConfirm = { viewModel.removeMember(userId) },
        onDismiss = { if (!op.running) { onDismiss(); if (op.op == CrewOp.REMOVE) viewModel.consumeOp() } },
    )
}

/** 정사각형 틀(사진 자르기 · 크게 보기) */
@Composable
internal fun CrewSquare(modifier: Modifier = Modifier, content: @Composable () -> Unit) {
    Box(modifier.fillMaxWidth().aspectRatio(1f)) { content() }
}
