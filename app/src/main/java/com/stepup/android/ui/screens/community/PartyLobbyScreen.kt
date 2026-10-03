package com.stepup.android.ui.screens.community

import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.DirectionsRun
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Groups
import androidx.compose.material.icons.outlined.Circle
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.stepup.android.R
import com.stepup.android.data.repo.PartyMember
import com.stepup.android.data.repo.PartyPhase
import com.stepup.android.data.repo.PartyProblem
import com.stepup.android.data.repo.PartyState
import com.stepup.android.domain.GeoPoint
import com.stepup.android.service.WalkSessionService
import com.stepup.android.ui.StepPermissions
import com.stepup.android.ui.components.RunButton
import com.stepup.android.ui.components.RunButtonKind
import com.stepup.android.ui.components.RunCard
import com.stepup.android.ui.components.RunInitials
import com.stepup.android.ui.components.RunNotice
import com.stepup.android.ui.components.RunNoticeKind
import com.stepup.android.ui.components.RunPage
import com.stepup.android.ui.components.RunSwitch
import com.stepup.android.ui.components.RunTextAction
import com.stepup.android.ui.components.rememberCurrentLocation
import com.stepup.android.ui.components.runTextStyle
import com.stepup.android.ui.components.runTone
import com.stepup.android.ui.experience.feedbackClickable
import com.stepup.android.ui.screens.walk.CrewNotifySheet
import com.stepup.android.ui.screens.walk.CrewReadyCheckSheet
import com.stepup.android.ui.screens.walk.CrewStartConfirmSheet
import com.stepup.android.ui.screens.walk.CrewTitleRow
import com.stepup.android.ui.screens.walk.RunCountdownStage
import com.stepup.android.ui.screens.walk.initialsOf

/**
 * 크루 러닝 대기실(러닝 전체 리메이크 2026-10-02 · 시안 CR04 · CR05 · CR06 · CR07 · CR08 · CR15 · CR17 · CR20).
 *
 * 크루 대기실과 번개러닝 대기실이 같은 화면을 쓴다. 준비 → 출발 → 같이 측정까지 흐름이 똑같고, 다른 것은 누가 모였는가뿐이다.
 *
 * 방은 서버에 있다. 같은 크루(번개 글)의 대기실을 연 사람들이 한 방에 모이고, 처음 연 사람이 진행자다(역할은 서버 값).
 * 진행자는 사람을 내보낼 수 있고, **준비를 마친 사람들끼리** 출발한다 — 준비 전 인원이 있으면 한 번 묻는다(CR07).
 * 출발 시각은 서버가 정하고 모두 같은 시각에 3-2-1(CR08). 이미 출발한 방에 늦게 온 사람은 서버가 새 방에 넣는다.
 * 준비하기 전에 권한이 없으면 위치 사용 · 위치 공유(기본 끔)를 따로 묻는다(CR17).
 *
 * @param crewId 크루 대기실이면 크루 id, 번개러닝 대기실이면 빈 문자열
 * @param flashPostId 번개러닝 대기실이면 그 글의 id
 */
@Composable
fun PartyLobbyScreen(
    crewId: String = "",
    flashPostId: Long? = null,
    onBack: () -> Unit,
    onRunStarted: () -> Unit,
    viewModel: PartyLobbyViewModel = viewModel(factory = PartyLobbyViewModel.Factory),
    community: CommunityViewModel = viewModel(factory = CommunityViewModel.Factory),
) {
    val context = LocalContext.current
    val party by viewModel.party.collectAsStateWithLifecycle()
    val resultPoints by viewModel.resultServerPoints.collectAsStateWithLifecycle()

    // 번개러닝이면 글에서 제목을 가져온다.
    val posts by community.allPosts.collectAsStateWithLifecycle()
    val flashPost = flashPostId?.let { id -> posts.firstOrNull { it.id == id } }

    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions(),
    ) {
        if (party.phase == PartyPhase.RUNNING && StepPermissions.hasActivityRecognition(context)) {
            WalkSessionService.start(context, party.partySize)
            onRunStarted()
        }
    }
    // CR17 — 준비 전 확인에서 "위치 사용 허용": OS 창의 결과를 다시 읽어 걸음 권한이 있으면 준비한다
    var readyCheck by rememberSaveable { mutableStateOf(false) }
    val readyLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) {
        readyCheck = false
        if (StepPermissions.hasActivityRecognition(context)) viewModel.setReady(true)
    }
    var confirmStart by rememberSaveable { mutableStateOf(false) }
    var notify by rememberSaveable { mutableStateOf(false) }

    LaunchedEffect(crewId, flashPostId) {
        when {
            flashPostId != null -> viewModel.openFlashLobby(flashPostId, flashPost?.title.orEmpty())
            crewId.isNotEmpty() -> viewModel.openLobby(crewId)
        }
    }

    // 서버가 정한 출발 시각이 되면 실제 세션을 시작한다.
    LaunchedEffect(party.phase) {
        if (party.phase == PartyPhase.RUNNING) {
            val missing = StepPermissions.missing(context)
            if (missing.isEmpty()) {
                WalkSessionService.start(context, party.partySize)
                onRunStarted()
            } else {
                permissionLauncher.launch(missing)
            }
        }
    }

    val leave = {
        viewModel.leaveLobby()
        onBack()
    }
    // 시스템 뒤로 가기도 화살표와 같이 방에서 나간다. 그냥 화면만 닫으면 앱 전체 수명의 폴링이 계속 돌아,
    // 서버에는 떠난 사람이 방에 남는다(진행자면 아무도 출발 못 한다).
    BackHandler { leave() }

    val title = party.crewName.ifBlank { flashPost?.title.orEmpty() }
    val canNotify = flashPostId == null && !party.crewId.isNullOrBlank()
    val here = rememberCurrentLocation(enabled = party.phase == PartyPhase.COUNTDOWN && StepPermissions.hasLocation(context))
    val monogram = party.crewId?.let { com.stepup.android.core.ServiceLocator.crewRepository.crewOf(it)?.monogram }.orEmpty()
    PartyLobbyContent(
        party = party, title = title, resultPoints = resultPoints, canNotify = canNotify, here = here, monogram = monogram,
        onBack = leave,
        onReady = {
            // 걸음 · 위치 권한이 없으면 먼저 묻는다(CR17) — 둘 다 있으면 바로 준비
            if (StepPermissions.hasActivityRecognition(context) && StepPermissions.hasLocation(context)) viewModel.setReady(true)
            else readyCheck = true
        },
        onCancelReady = { viewModel.setReady(false) },
        onStart = { if (party.allReady) viewModel.startParty() else confirmStart = true },
        onShare = viewModel::setShare,
        onKick = viewModel::kick,
        onNotify = { notify = true },
        onRetry = viewModel::retry,
        onLeave = leave,
        onDismissResult = {
            viewModel.dismissResult()
            onBack()
        },
    )
    if (readyCheck && party.phase == PartyPhase.LOBBY) {
        CrewReadyCheckSheet(
            locationAllowed = StepPermissions.hasLocation(context), share = party.myShare, onShare = viewModel::setShare,
            onAllow = {
                val missing = StepPermissions.missing(context)
                if (missing.isEmpty()) {
                    readyCheck = false
                    viewModel.setReady(true)
                } else {
                    readyLauncher.launch(missing)
                }
            },
            onLater = { readyCheck = false },
        )
    }
    if (confirmStart && party.phase == PartyPhase.LOBBY && party.canStart) {
        // 열려 있는 동안 방 상태가 바뀌면 숫자도 바뀐다. 그새 모두 준비했으면 묻지 않고 출발 버튼 그대로
        CrewStartConfirmSheet(
            ready = party.readyCount, notReady = party.partySize - party.readyCount,
            onGo = {
                confirmStart = false
                viewModel.startParty()
            },
            onWait = { confirmStart = false },
        )
    }
    val notifyCrew = party.crewId
    if (notify && canNotify && notifyCrew != null) {
        CrewNotifySheet(crewId = notifyCrew, crewName = title, waiting = party.partySize, onDismiss = { notify = false })
    }
}

/**
 * 대기실의 모습 — 상태 없이 그린다(시안 검사도 이것을 그대로 그린다).
 * 머리(뒤로 · "크루 러닝 대기실" · 로고) · 크루 이름 · 참여 | 준비 완료 · 크루원 줄 · 내 위치 공유 · 아래 버튼.
 */
@Composable
fun PartyLobbyContent(
    party: PartyState,
    title: String,
    resultPoints: Double?,
    canNotify: Boolean,
    here: GeoPoint?,
    onBack: () -> Unit,
    onReady: () -> Unit,
    onCancelReady: () -> Unit,
    onStart: () -> Unit,
    onShare: (Boolean) -> Unit,
    onKick: (String) -> Unit,
    onNotify: () -> Unit,
    onRetry: () -> Unit,
    onLeave: () -> Unit,
    onDismissResult: () -> Unit,
    /** 크루가 정한 이니셜(번개러닝이면 비어 있다) */
    monogram: String = "",
) {
    val t = runTone()
    // CR08 — 서버가 정한 시각에 모두 같이. 한 사람이 끊을 수 없다(나가기는 뒤로)
    if (party.phase == PartyPhase.COUNTDOWN) {
        RunCountdownStage(
            title = stringResource(R.string.run_crew_title), subtitle = title.ifBlank { null },
            kicker = stringResource(R.string.run_cr_go_soon), digit = party.countdown.coerceAtLeast(1),
            caption = stringResource(R.string.run_cr_go_soon), here = here, onCancel = null,
            modifier = Modifier.testTag("party-countdown"),
            headline = {
                // "함께 출발 27명" — 숫자만 시안 색으로 크게
                val line = stringResource(R.string.run_cr_go_together, party.readyCount)
                val count = party.readyCount.toString()
                val at = line.indexOf(count)
                Text(
                    androidx.compose.ui.text.buildAnnotatedString {
                        append(line)
                        if (at >= 0) {
                            addStyle(
                                androidx.compose.ui.text.SpanStyle(
                                    color = t.cyan, fontSize = 34.sp, fontStyle = androidx.compose.ui.text.font.FontStyle.Italic,
                                    fontWeight = FontWeight.ExtraBold,
                                ),
                                at, at + count.length,
                            )
                        }
                    },
                    style = runTextStyle(24.sp, t.text, FontWeight.ExtraBold), modifier = Modifier.testTag("party-countdown-together"),
                )
            },
        )
        return
    }
    val alone = party.partySize <= 1
    val lobby = party.phase == PartyPhase.LOBBY || party.phase == PartyPhase.IDLE
    RunPage(
        onBack = onBack, modifier = Modifier.testTag("party-lobby"), crumb = stringResource(R.string.run_cr_room_title),
        bottom = {
            when {
                party.phase == PartyPhase.FINISHED ->
                    RunButton(stringResource(R.string.common_ok), onDismissResult, Modifier.testTag("party-result-ok"))
                // 누른 것이 막힌 것뿐(FAILED)이면 방에 그대로 있으니 버튼도 그대로 — 다시 누르면 문제가 풀린다
                !lobby || (party.problem != null && party.problem != PartyProblem.FAILED) || party.partyId == null -> Unit
                !party.myReady -> {
                    RunButton(stringResource(R.string.run_cr_get_ready), onReady, Modifier.testTag("party-ready"))
                    if (alone && canNotify) {
                        RunButton(stringResource(R.string.run_cr_notify), onNotify, Modifier.testTag("party-notify"),
                            kind = RunButtonKind.Secondary)
                    }
                }
                party.isHost -> {
                    RunButton(
                        if (alone) stringResource(R.string.run_cr_start_alone) else stringResource(R.string.run_cr_start_n, party.readyCount),
                        onStart, Modifier.testTag("party-start"),
                    )
                    if (alone && canNotify) {
                        RunButton(stringResource(R.string.run_cr_notify), onNotify, Modifier.testTag("party-notify"),
                            kind = RunButtonKind.Secondary)
                    } else {
                        RunButton(stringResource(R.string.run_cr_cancel_ready), onCancelReady, Modifier.testTag("party-cancel-ready"),
                            kind = RunButtonKind.Secondary)
                    }
                }
                // 크루원 준비 완료(CR05) — 출발은 진행자가 · 서버가
                else -> Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    RunButton(stringResource(R.string.run_cr_waiting_start), {}, Modifier.weight(1.5f).testTag("party-waiting"),
                        enabled = false)
                    RunButton(stringResource(R.string.run_cr_cancel_ready), onCancelReady, Modifier.weight(1f).testTag("party-cancel-ready"),
                        kind = RunButtonKind.Secondary)
                }
            }
        },
    ) {
        CrewTitleRow(title, monogram.ifBlank { initialsOf(title) })
        Spacer(Modifier.height(14.dp))
        // 방에 못 들어갔거나 멈췄으면 왜인지부터
        party.problem?.let { problem ->
            ProblemNotice(problem, onRetry, onLeave)
            Spacer(Modifier.height(12.dp))
        }
        when (party.phase) {
            PartyPhase.FINISHED -> ResultCard(resultPoints)
            PartyPhase.RUNNING -> RunNotice(
                stringResource(R.string.crew_running_together, party.partySize), icon = Icons.AutoMirrored.Filled.DirectionsRun,
                tag = "party-running",
            )
            else -> if (party.partyId != null) {
                Counts(party)
                Spacer(Modifier.height(8.dp))
                RunCard(padding = PaddingValues(horizontal = 14.dp, vertical = 4.dp), tag = "party-members") {
                    party.members.forEachIndexed { index, member ->
                        if (index > 0) Box(Modifier.fillMaxWidth().height(1.dp).background(t.divider))
                        MemberRow(member, canKick = lobby && party.isHost && !member.isMe, onKick = { onKick(member.id) })
                    }
                    if (alone) {
                        Box(Modifier.fillMaxWidth().height(1.dp).background(t.divider))
                        Row(Modifier.fillMaxWidth().padding(vertical = 12.dp), verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Filled.Groups, contentDescription = null, tint = t.muted, modifier = Modifier.size(26.dp))
                            Spacer(Modifier.width(12.dp))
                            Text(stringResource(R.string.run_cr_alone_body), style = runTextStyle(13.sp, t.label, FontWeight.Medium),
                                modifier = Modifier.testTag("party-alone"))
                        }
                    }
                }
                Spacer(Modifier.height(10.dp))
                ShareRow(party.myShare, onShare)
                if (canNotify && !alone && lobby) {
                    RunTextAction(stringResource(R.string.run_cr_notify), onNotify, Modifier.fillMaxWidth().testTag("party-notify-link"),
                        chevron = true)
                }
                Text(
                    stringResource(R.string.party_gps_rule, com.stepup.android.data.repo.CrewRepository.MAX_PARTY_DISTANCE_M),
                    style = runTextStyle(12.sp, t.muted), modifier = Modifier.padding(top = 6.dp),
                )
            }
        }
    }
}

/** 참여 N명 | ✓ 준비 완료 M명 */
@Composable
private fun Counts(party: PartyState) {
    val t = runTone()
    Row(Modifier.fillMaxWidth().padding(vertical = 6.dp).testTag("party-counts"), verticalAlignment = Alignment.CenterVertically) {
        Icon(Icons.Filled.Groups, contentDescription = null, tint = t.label, modifier = Modifier.size(22.dp))
        Spacer(Modifier.width(8.dp))
        Text(stringResource(R.string.run_cr_joined, party.partySize), style = runTextStyle(16.sp, t.text, FontWeight.Bold),
            modifier = Modifier.weight(1f))
        Icon(Icons.Filled.CheckCircle, contentDescription = null, tint = t.cobaltText, modifier = Modifier.size(20.dp))
        Spacer(Modifier.width(6.dp))
        Text(stringResource(R.string.run_cr_ready_count, party.readyCount), style = runTextStyle(16.sp, t.text, FontWeight.Bold))
    }
}

/** 크루원 한 줄 — 이니셜 · 이름(· 나) · 진행자 · 준비 상태. 진행자는 대기실에서 내보낼 수 있다 */
@Composable
private fun MemberRow(member: PartyMember, canKick: Boolean, onKick: () -> Unit) {
    val t = runTone()
    val name = when {
        member.isMe && member.name.isNotBlank() -> stringResource(R.string.run_cr_me, member.name)
        member.isMe -> stringResource(R.string.run_cr_me_only)
        else -> member.name
    }
    Row(
        Modifier.fillMaxWidth().heightIn(min = 56.dp).semantics(mergeDescendants = true) {}.testTag("party-member-${member.id}"),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        RunInitials(initialsOf(member.name.ifBlank { name }), size = 38.dp, filled = member.isMe)
        Spacer(Modifier.width(12.dp))
        Text(name, style = runTextStyle(16.sp, t.text, FontWeight.Bold), maxLines = 1, overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f, fill = false))
        if (member.isHost) {
            Spacer(Modifier.width(8.dp))
            Text(
                stringResource(R.string.run_cr_host), style = runTextStyle(12.sp, t.cobaltText, FontWeight.Bold),
                modifier = Modifier.clip(RoundedCornerShape(50)).background(t.chipFace).padding(horizontal = 8.dp, vertical = 2.dp),
            )
        }
        Spacer(Modifier.weight(1f))
        Icon(
            if (member.ready) Icons.Filled.CheckCircle else Icons.Outlined.Circle, contentDescription = null,
            tint = if (member.ready) t.cobaltText else t.label, modifier = Modifier.size(20.dp),
        )
        Spacer(Modifier.width(6.dp))
        Text(
            stringResource(if (member.ready) R.string.run_cr_ready else R.string.run_cr_not_ready),
            style = runTextStyle(14.sp, if (member.ready) t.cobaltText else t.label, FontWeight.SemiBold),
        )
        if (canKick) {
            val label = stringResource(R.string.run_cr_kick)
            Box(
                Modifier.size(48.dp).clip(CircleShape).feedbackClickable(onClick = onKick)
                    .semantics { contentDescription = label }.testTag("party-kick-${member.id}"),
                contentAlignment = Alignment.Center,
            ) {
                Icon(Icons.Filled.Close, contentDescription = null, tint = t.muted, modifier = Modifier.size(18.dp))
            }
        }
    }
}

/** 내 위치 공유 — 기본 끔. 대기실 · 달리는 중에 바꿀 수 있다(다음 방에서도 이 값으로 시작) */
@Composable
private fun ShareRow(share: Boolean, onShare: (Boolean) -> Unit) {
    val t = runTone()
    RunCard(padding = PaddingValues(horizontal = 14.dp, vertical = 10.dp), tag = "party-share-location") {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text(stringResource(if (share) R.string.run_cr_share_on else R.string.run_cr_share_off),
                    style = runTextStyle(15.sp, t.text, FontWeight.Bold))
                Text(stringResource(if (share) R.string.run_my_share_on else R.string.run_my_share_off), style = runTextStyle(12.sp, t.label))
            }
            RunSwitch(share, onShare, Modifier.testTag("party-share-toggle"))
        }
    }
}

/** 멈춘 까닭 — 서버에 닿지 못한 것은 저절로 다시 묻는다. 나머지는 사람이 정한다 */
@Composable
private fun ProblemNotice(problem: PartyProblem, onRetry: () -> Unit, onLeave: () -> Unit) {
    val message = stringResource(
        when (problem) {
            PartyProblem.SIGN_IN -> R.string.party_problem_sign_in
            PartyProblem.REMOVED -> R.string.party_problem_removed
            PartyProblem.NETWORK -> R.string.party_problem_network
            PartyProblem.CLOSED -> R.string.party_problem_closed
            PartyProblem.FAILED -> R.string.party_problem_failed
        },
    )
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        RunNotice(
            message, kind = if (problem == PartyProblem.NETWORK) RunNoticeKind.Warn else RunNoticeKind.Error, tag = "party-problem",
            action = if (problem == PartyProblem.CLOSED || problem == PartyProblem.REMOVED) stringResource(R.string.crew_retry) else null,
            onAction = if (problem == PartyProblem.CLOSED || problem == PartyProblem.REMOVED) onRetry else null,
        )
        // 누른 것이 막힌 것뿐이면 방에는 그대로 있다
        if (problem != PartyProblem.FAILED) {
            RunButton(stringResource(R.string.run_cr_leave), onLeave, Modifier.testTag("party-leave"), kind = RunButtonKind.Secondary)
        }
    }
}

/** 끝난 크루 러닝 — 금액은 서버가 확인한 뒤에만(그 전에는 "서버 확인 중") */
@Composable
private fun ResultCard(resultPoints: Double?) {
    val t = runTone()
    RunCard(padding = PaddingValues(18.dp), tag = "party-result") {
        Text(stringResource(R.string.crew_result_title), style = runTextStyle(18.sp, t.text, FontWeight.ExtraBold))
        Spacer(Modifier.height(8.dp))
        if (resultPoints != null) {
            Text(stringResource(R.string.run_cr_result_points), style = runTextStyle(13.sp, t.label))
            com.stepup.android.ui.components.RunNumber(
                "+%.2f".format(resultPoints), unit = "SUP", size = 34.sp, color = t.cyan,
                modifier = Modifier.testTag("party-result-points"),
            )
        } else {
            Text(stringResource(R.string.finish_pending_short), style = runTextStyle(16.sp, t.label, FontWeight.SemiBold),
                modifier = Modifier.testTag("party-result-pending"))
        }
    }
}
