package com.stepup.android.ui.screens.community.crew

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
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
        Column(Modifier.weight(1f).fillMaxWidth().padding(horizontal = CrewGutter), verticalArrangement = Arrangement.Center, horizontalAlignment = Alignment.CenterHorizontally) {
            card?.let { crew ->
                androidx.compose.foundation.layout.BoxWithConstraints(Modifier.fillMaxWidth()) {
                    CrewImage(crew, maxWidth, 26.dp, textSize = (maxWidth.value * 0.2f).sp)
                }
                Spacer(Modifier.height(40.dp))
                Text(crew.name, color = ink.text, fontSize = 22.sp, fontWeight = FontWeight.SemiBold, textAlign = TextAlign.Center)
                if (crew.tagline.isNotBlank()) {
                    Spacer(Modifier.height(12.dp))
                    Text(crew.tagline, color = ink.secondary, fontSize = 14.sp, textAlign = TextAlign.Center)
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
            CrewPersonHead(crew.leaderName, stringResource(R.string.crew_leader_of, crew.name))
            val note = crew.leaderNote.trim()
            if (note.isNotEmpty()) {
                val lines = note.lines()
                Spacer(Modifier.height(48.dp))
                Text(lines.first(), color = ink.text, fontSize = 24.sp, lineHeight = 31.sp, fontWeight = FontWeight.SemiBold, modifier = Modifier.testTag("crew-leader-note"))
                if (lines.size > 1) {
                    Spacer(Modifier.height(14.dp))
                    Text(lines.drop(1).joinToString("\n"), color = ink.secondary, fontSize = 14.sp, lineHeight = 25.sp)
                }
            }
            Spacer(Modifier.height(44.dp))
            CrewRow(stringResource(R.string.crew_leader_runs), onOpenCrew, Modifier.testTag("crew-leader-crew"), value = crew.name)
            val chips = words.styleChips(crew)
            if (chips.isNotEmpty()) {
                Spacer(Modifier.height(24.dp))
                Text(stringResource(R.string.crew_style_title), color = ink.secondary, fontSize = 14.sp)
                Spacer(Modifier.height(12.dp))
                CrewStyleChips(chips)
            }
            Spacer(Modifier.height(32.dp))
        }
    }
}

/** 동그라미 · 이름 · 한 줄 */
@Composable
private fun CrewPersonHead(name: String, sub: String?, subColor: androidx.compose.ui.graphics.Color? = null) {
    val ink = crewInk()
    Column(Modifier.fillMaxWidth().padding(top = 40.dp), horizontalAlignment = Alignment.CenterHorizontally) {
        CrewAvatar(name, 76.dp, ink.avatar, ink.avatarText)
        Spacer(Modifier.height(18.dp))
        Text(name, color = ink.text, fontSize = 26.sp, fontWeight = FontWeight.SemiBold, textAlign = TextAlign.Center, modifier = Modifier.testTag("crew-person-name"))
        if (sub != null) {
            Spacer(Modifier.height(10.dp))
            Text(sub, color = subColor ?: ink.info, fontSize = 13.5.sp, textAlign = TextAlign.Center, modifier = Modifier.testTag("crew-person-sub"))
        }
    }
}

/** 러닝 스타일 칩 — 누르지 않는 표시(옅은 파랑 면) */
@Composable
private fun CrewStyleChips(chips: List<String>) {
    chips.chunked(2).forEach { row ->
        CrewChoiceRow {
            row.forEach { CrewChoice(it, true, {}, Modifier.weight(1f), enabled = false) }
            if (row.size == 1) Spacer(Modifier.weight(1f))
        }
    }
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
            is CrewLoad.Failed -> CrewEmptyState(icon = {}, title = stringResource(R.string.crew_person_failed), body = "", modifier = Modifier.padding(top = 120.dp)) {
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
        CrewPersonHead(person.name, sub, if (person.role == CrewPersonRole.NONE) ink.secondary else null)
        Spacer(Modifier.height(40.dp))
        when (person.role) {
            CrewPersonRole.APPLICANT -> {
                val phrases = person.application?.phrases.orEmpty()
                if (phrases.isNotEmpty()) {
                    Text(stringResource(R.string.crew_applicant_phrases), color = ink.text, fontSize = 22.sp, fontWeight = FontWeight.SemiBold)
                    Spacer(Modifier.height(16.dp))
                    CrewStyleChips(phrases.map { words.phrase(it) })
                    Spacer(Modifier.height(30.dp))
                }
                Text(stringResource(R.string.crew_applicant_note, crewName), color = ink.secondary, fontSize = 14.sp, lineHeight = 25.sp, modifier = Modifier.testTag("crew-applicant-note"))
            }
            CrewPersonRole.OWNER, CrewPersonRole.MEMBER -> {
                Column(
                    Modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp)).background(ink.card).padding(horizontal = 18.dp, vertical = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    person.joinedAt?.takeIf { it > 0 }?.let {
                        Text(stringResource(R.string.crew_member_since, words.date(it)), color = ink.text, fontSize = 14.sp)
                    }
                    Text(
                        if (person.weekKm > 0) stringResource(R.string.crew_member_week_km, CrewRules.km(person.weekKm))
                        else stringResource(R.string.crew_member_week_none),
                        color = ink.secondary, fontSize = 13.sp,
                    )
                }
                Spacer(Modifier.height(20.dp))
                CrewRow(stringResource(R.string.crew_member_crew), onOpenCrew, Modifier.testTag("crew-person-crew"), value = crewName)
            }
            CrewPersonRole.NONE -> Unit
        }
        Spacer(Modifier.height(80.dp))
        Text(stringResource(R.string.crew_person_public_only), color = ink.secondary, fontSize = 12.5.sp)
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
                Spacer(Modifier.height(22.dp))
                val count = if (mode == CrewRosterMode.WEEK) shown.size else crew?.memberCount ?: members.size
                Text(
                    stringResource(if (mode == CrewRosterMode.WEEK) R.string.crew_participants_heading else R.string.crew_members_heading, count),
                    color = ink.text, fontSize = 26.sp, fontWeight = FontWeight.SemiBold, modifier = Modifier.testTag("crew-roster-heading"),
                )
                Spacer(Modifier.height(12.dp))
                val sub = when {
                    crew == null -> ""
                    mode == CrewRosterMode.WEEK -> stringResource(R.string.crew_participants_sub, crew.name, CrewRules.km(shown.sumOf { it.weekKm }))
                    crew.capacity != null -> stringResource(R.string.crew_members_sub_capacity, crew.name, crew.capacity)
                    else -> crew.name
                }
                Text(sub, color = ink.secondary, fontSize = 13.5.sp)
                Spacer(Modifier.height(if (mode == CrewRosterMode.MANAGE) 26.dp else 44.dp))
                if (mode == CrewRosterMode.MANAGE) {
                    CrewRow(stringResource(R.string.crew_transfer_row), onTransfer, Modifier.testTag("crew-transfer-row"))
                    Spacer(Modifier.height(10.dp))
                } else if (mode == CrewRosterMode.MEMBERS) {
                    Text(stringResource(R.string.crew_members_label), color = ink.secondary, fontSize = 13.sp)
                    Spacer(Modifier.height(8.dp))
                }
            }
            when (val state = roster) {
                CrewLoad.Loading -> item { CrewSkeletonBox(Modifier.fillMaxWidth().height(160.dp), 16.dp) }
                is CrewLoad.Failed -> item {
                    Column(Modifier.padding(top = 20.dp)) {
                        Text(stringResource(R.string.crew_roster_failed), color = ink.warn, fontSize = 13.5.sp)
                        Spacer(Modifier.height(16.dp))
                        CrewButton(stringResource(R.string.crew_list_reload), { viewModel.loadRoster() }, kind = CrewButtonKind.SECONDARY)
                    }
                }
                is CrewLoad.Ready -> {
                    if (shown.isEmpty()) item {
                        Text(
                            stringResource(if (mode == CrewRosterMode.WEEK) R.string.crew_participants_none else R.string.crew_roster_empty),
                            color = ink.secondary, fontSize = 14.sp, modifier = Modifier.padding(top = 12.dp).testTag("crew-roster-empty"),
                        )
                    }
                    items(shown, key = { it.userId }) { member ->
                        CrewPersonRow(
                            name = member.name,
                            sub = rosterSub(member, mode),
                            subColor = if (member.owner || mode == CrewRosterMode.WEEK) ink.info else null,
                            onClick = { if (member.owner) onOpenLeader() else onOpenMember(member.userId) },
                            modifier = Modifier.testTag("crew-roster-${member.userId}"),
                        )
                    }
                    if (mode == CrewRosterMode.WEEK && crew?.goalKm != null) item {
                        Spacer(Modifier.height(28.dp))
                        Text(stringResource(R.string.crew_goal_gathered), color = ink.secondary, fontSize = 13.sp)
                    }
                }
            }
        }
    }
}

@Composable
private fun rosterSub(member: CrewMember, mode: CrewRosterMode): String = when {
    mode == CrewRosterMode.WEEK && member.owner -> stringResource(R.string.crew_roster_leader_km, CrewRules.km(member.weekKm))
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
                Spacer(Modifier.height(22.dp))
                Text(stringResource(R.string.crew_transfer_heading), color = ink.text, fontSize = 26.sp, fontWeight = FontWeight.SemiBold)
                Spacer(Modifier.height(12.dp))
                Text(stringResource(R.string.crew_transfer_sub), color = ink.secondary, fontSize = 13.5.sp)
                Spacer(Modifier.height(44.dp))
            }
            when (val state = roster) {
                CrewLoad.Loading -> item { CrewSkeletonBox(Modifier.fillMaxWidth().height(160.dp), 16.dp) }
                is CrewLoad.Failed -> item {
                    CrewButton(stringResource(R.string.crew_list_reload), { viewModel.loadRoster() }, kind = CrewButtonKind.SECONDARY)
                }
                is CrewLoad.Ready -> {
                    if (candidates.isEmpty()) item {
                        Text(stringResource(R.string.crew_transfer_none), color = ink.secondary, fontSize = 14.sp, lineHeight = 22.sp, modifier = Modifier.testTag("crew-transfer-none"))
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
