package com.stepup.android.ui.screens.community

import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.togetherWith
import com.stepup.android.ui.experience.*
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.DirectionsWalk
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Groups
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.platform.testTag
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.stepup.android.R
import com.stepup.android.data.repo.CrewRepository
import com.stepup.android.data.repo.PartyMember
import com.stepup.android.data.repo.PartyPhase
import com.stepup.android.data.repo.PartyProblem
import com.stepup.android.domain.RewardEconomy
import com.stepup.android.service.WalkSessionService
import com.stepup.android.ui.StepPermissions
import com.stepup.android.ui.components.BarMeter
import com.stepup.android.ui.components.DetailPage
import com.stepup.android.ui.components.GhostButton
import com.stepup.android.ui.components.GlowCard
import com.stepup.android.ui.components.HexBadge
import com.stepup.android.ui.components.HexEmblem
import com.stepup.android.ui.theme.Night
import com.stepup.android.ui.theme.Silver
import com.stepup.android.ui.theme.Slate
import com.stepup.android.ui.theme.Snow
import com.stepup.android.ui.theme.Volt

/**
 * 파티런 로비 — 로비를 연 사람이 파티장이다.
 *
 * 크루 로비와 번개러닝 로비가 같은 화면을 쓴다. 준비 → 시작 → 같이 측정까지
 * 흐름이 똑같고, 다른 것은 누가 모였는가뿐이다.
 *
 * 방은 서버에 있다. 같은 크루(번개 글)의 로비를 연 사람들이 한 방에 모이고,
 * 처음 연 사람이 방장이다. 방장은 사람을 내보낼 수 있고, **준비를 마친
 * 사람들끼리** 출발한다. 전원을 기다리지 않는다 — 한 사람 때문에 나머지가
 * 길에 서 있게 되면, 그 사람들은 다음부터 파티런을 안 쓴다.
 * 러닝 중 방장에게서 일정 거리 이상 떨어진 사람은 자동으로 빠진다.
 *
 * @param crewId 크루 로비면 크루 id, 번개러닝 로비면 빈 문자열
 * @param flashPostId 번개러닝 로비면 그 글의 id
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
    androidx.activity.compose.BackHandler {
        viewModel.leaveLobby()
        onBack()
    }
    val context = LocalContext.current
    val party by viewModel.party.collectAsStateWithLifecycle()
    val resultPoints by viewModel.resultServerPoints.collectAsStateWithLifecycle()

    // 번개러닝이면 글에서 제목을 가져온다.
    val posts by community.allPosts.collectAsStateWithLifecycle()
    val flashPost = flashPostId?.let { id -> posts.firstOrNull { it.id == id } }

    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) {
        if (party.phase == PartyPhase.RUNNING && StepPermissions.hasActivityRecognition(context)) {
            WalkSessionService.start(context, party.partySize)
            onRunStarted()
        }
    }

    LaunchedEffect(crewId, flashPostId) {
        when {
            flashPostId != null -> viewModel.openFlashLobby(flashPostId, flashPost?.title.orEmpty())
            crewId.isNotEmpty() -> viewModel.openLobby(crewId)
        }
    }

    // 전원 준비 → 카운트다운이 끝나면 실제 세션을 시작한다.
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

    // 시스템 뒤로 가기도 화살표와 같이 방에서 나간다. 그냥 화면만 닫으면 앱 전체
    // 수명의 폴링이 계속 돌아, 서버에는 떠난 사람이 방에 남는다(방장이면 아무도 출발 못 한다).
    BackHandler(enabled = party.phase == PartyPhase.LOBBY || party.phase == PartyPhase.COUNTDOWN) {
        viewModel.leaveLobby()
        onBack()
    }

    val boostPercent = RewardEconomy.partyBonusPercent(party.partySize)
    val showReadyAction = party.phase == PartyPhase.LOBBY && party.problem == null && (party.canStart || !party.myReady)

    Box(Modifier.fillMaxSize()) {
        DetailPage(
            title = stringResource(R.string.crew_lobby),
            onBack = { viewModel.leaveLobby(); onBack() },
            primaryActionIcon = if (party.canStart) Icons.Filled.PlayArrow else Icons.Filled.Check,
            primaryActionLabel = if (showReadyAction) when {
                party.canStart && party.allReady -> stringResource(R.string.party_start)
                party.canStart -> stringResource(R.string.party_start_ready, party.readyCount)
                else -> stringResource(R.string.crew_ready)
            } else null,
            onPrimaryAction = {
                if (party.canStart) viewModel.startParty() else viewModel.setReady(true)
            },
        ) {
            // S2 — 준비 상태 한 줄 → 모임 이름 → 큰 준비 인원 → 파티 보너스
            item {
                Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
                    com.stepup.android.ui.components.S2Kicker(
                        when {
                            party.allReady -> stringResource(R.string.crew_all_ready)
                            party.canStart -> stringResource(R.string.crew_can_start)
                            party.myReady && !party.isHost -> stringResource(R.string.party_wait_host)
                            else -> stringResource(R.string.crew_waiting)
                        },
                    )
                    Spacer(Modifier.height(10.dp))
                    com.stepup.android.ui.components.S2Headline(party.crewName.ifBlank { flashPost?.title.orEmpty() })
                    Spacer(Modifier.height(18.dp))
                    com.stepup.android.ui.components.S2Number("${party.readyCount}/${party.partySize}", 68.sp)
                    Text(
                        stringResource(R.string.crew_ready_count, party.readyCount, party.partySize),
                        color = Silver, fontSize = 13.sp,
                    )
                    Spacer(Modifier.height(10.dp))
                    com.stepup.android.ui.components.S2Subtitle(
                        stringResource(R.string.crew_boost, boostPercent) + " · " + stringResource(R.string.crew_boost_hint),
                        color = com.stepup.android.ui.theme.VoltText,
                    )
                }
            }

            // 방에 못 들어갔거나 멈췄으면 왜인지부터
            party.problem?.let { problem ->
                item {
                    PartyProblemCard(
                        problem = problem,
                        onRetry = viewModel::retry,
                        onLeave = {
                            viewModel.leaveLobby()
                            onBack()
                        },
                    )
                }
            }

            // 멤버 목록 (방장은 로비에서 내보낼 수 있다)
            items(party.members.size, key = { party.members[it].id }) { index ->
                val m = party.members[index]
                MemberRow(
                    member = m,
                    canKick = party.phase == PartyPhase.LOBBY && party.isHost && !m.isMe,
                    onKick = { viewModel.kick(m.id) },
                )
            }

            // S2 같이 뛰는 중 — 내 위치 · 거리를 보일지(기본 끔). 로비와 달리는 중에 바꿀 수 있다.
            if (party.partyId != null && (party.phase == PartyPhase.LOBBY || party.phase == PartyPhase.RUNNING)) {
                item {
                    androidx.compose.foundation.layout.Box(Modifier.testTag("party-share-location")) {
                        com.stepup.android.ui.components.PreferenceToggle(
                            title = stringResource(R.string.party_share_title),
                            description = stringResource(R.string.party_share_desc),
                            icon = Icons.Filled.LocationOn,
                            checked = party.myShare,
                            onCheckedChange = viewModel::setShare,
                        )
                    }
                }
            }

            // 누가 이 방에 들어올 수 있는지 (로비 단계에서만)
            if (party.phase == PartyPhase.LOBBY) {
                item {
                    Text(
                        text = stringResource(R.string.party_join_hint),
                        style = MaterialTheme.typography.bodyMedium,
                        color = Slate,
                    )
                }
            }

            // 컨트롤
            item {
                Spacer(Modifier.height(4.dp))
                when (party.phase) {
                    PartyPhase.RUNNING -> {
                        GlowCard(accent = true, contentPadding = PaddingValues(18.dp), spacing = 10.dp) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(10.dp),
                            ) {
                                Icon(
                                    Icons.AutoMirrored.Filled.DirectionsWalk,
                                    contentDescription = null,
                                    tint = Volt,
                                    modifier = Modifier.size(20.dp),
                                )
                                Text(
                                    text = stringResource(
                                        R.string.crew_running_together,
                                        party.partySize,
                                    ),
                                    style = MaterialTheme.typography.titleSmall,
                                    color = Snow,
                                )
                            }
                        }
                    }
                    PartyPhase.FINISHED -> {
                        GlowCard(accent = true, contentPadding = PaddingValues(20.dp), spacing = 12.dp) {
                            Column(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.spacedBy(9.dp),
                            ) {
                                Text(
                                    text = stringResource(R.string.crew_result_title),
                                    style = MaterialTheme.typography.titleMedium,
                                    color = Snow,
                                )
                                // 금액은 서버가 확인한 뒤에만 — 그 전에는 "서버 확인 중"
                                val confirmed = resultPoints
                                if (confirmed != null) {
                                    Text(
                                        text = "+%.2f SUP".format(confirmed),
                                        fontFamily = com.stepup.android.ui.theme.StepUpNumbers,
                                        fontSize = 34.sp,
                                        fontWeight = FontWeight.ExtraBold,
                                        letterSpacing = (-1).sp,
                                        color = Volt,
                                        modifier = Modifier.testTag("party-result-points"),
                                    )
                                } else {
                                    Text(
                                        text = stringResource(R.string.finish_pending_short),
                                        style = MaterialTheme.typography.titleMedium,
                                        color = Silver,
                                        modifier = Modifier.testTag("party-result-pending"),
                                    )
                                }
                                // 걸음 · 파티 부스트 줄은 뺐다 — 둘 다 폰의 예상치라, 서버가 인원 · 에너지를 다시 따진
                                // 금액 옆에 두면 서로 맞지 않을 수 있다. 확정 금액만 보인다.
                            }
                            GhostButton(
                                text = stringResource(R.string.common_ok),
                                onClick = {
                                    viewModel.dismissResult()
                                    onBack()
                                },
                                modifier = Modifier.fillMaxWidth(),
                            )
                        }
                    }
                    else -> {
                        Column(verticalArrangement = Arrangement.spacedBy(9.dp)) {
                            // 전원을 기다리지 않는다. 파티장이 준비했으면
                            // 준비된 사람들끼리 출발할 수 있다.
                            if (party.canStart) {
                                if (!party.allReady) {
                                    Text(
                                        text = stringResource(
                                            R.string.party_start_leaves_behind,
                                            party.partySize - party.readyCount,
                                        ),
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = Slate,
                                    )
                                }
                            }
                            if (party.myReady) {
                                GhostButton(
                                    text = stringResource(R.string.crew_ready_cancel),
                                    onClick = { viewModel.setReady(false) },
                                    accent = Silver,
                                    modifier = Modifier.fillMaxWidth(),
                                )
                            }

                            Text(
                                text = stringResource(
                                    R.string.party_gps_rule,
                                    CrewRepository.MAX_PARTY_DISTANCE_M,
                                ),
                                style = MaterialTheme.typography.bodyMedium,
                                color = Slate,
                            )
                        }
                    }
                }
            }
        }

        // 카운트다운 오버레이
        if (party.phase == PartyPhase.COUNTDOWN) {
            CountdownOverlay(party.countdown)
        }
    }

}

@Composable
private fun PartyProblemCard(problem: PartyProblem, onRetry: () -> Unit, onLeave: () -> Unit) {
    val message = stringResource(
        when (problem) {
            PartyProblem.SIGN_IN -> R.string.party_problem_sign_in
            PartyProblem.REMOVED -> R.string.party_problem_removed
            PartyProblem.NETWORK -> R.string.party_problem_network
            PartyProblem.CLOSED -> R.string.party_problem_closed
            PartyProblem.FAILED -> R.string.party_problem_failed
        },
    )
    GlowCard(contentPadding = PaddingValues(18.dp), spacing = 12.dp) {
        Text(message, style = MaterialTheme.typography.bodyMedium, color = Snow)
        Row(horizontalArrangement = Arrangement.spacedBy(9.dp)) {
            // 서버에 닿지 못한 것은 저절로 다시 묻는다. 나머지는 사람이 정한다.
            if (problem == PartyProblem.CLOSED || problem == PartyProblem.REMOVED) {
                GhostButton(
                    text = stringResource(R.string.crew_retry),
                    onClick = onRetry,
                    modifier = Modifier.weight(1f),
                )
            }
            // 누른 것이 막힌 것뿐이면 방에는 그대로 있다
            if (problem != PartyProblem.FAILED) {
                GhostButton(
                    text = stringResource(R.string.party_leave),
                    onClick = onLeave,
                    accent = Silver,
                    modifier = Modifier.weight(1f),
                )
            }
        }
    }
}

@Composable
private fun MemberRow(
    member: PartyMember,
    canKick: Boolean,
    onKick: () -> Unit,
) {
    val name = if (member.isMe) stringResource(R.string.crew_you) else member.name
    GlowCard(contentPadding = PaddingValues(18.dp), accent = member.isMe) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            com.stepup.android.ui.components.IconSquare(Icons.Filled.Groups, size = 44.dp, tint = if (member.ready) Volt else Silver)
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(name, style = MaterialTheme.typography.titleMedium, color = Snow)
                if (member.isHost) {
                    Text(stringResource(R.string.party_host_badge), style = MaterialTheme.typography.labelLarge, color = Volt)
                }
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    if (member.ready) Icon(Icons.Filled.Check, null, tint = Volt, modifier = Modifier.size(18.dp))
                    Text(stringResource(if (member.ready) R.string.crew_state_ready else R.string.crew_state_waiting),
                        style = MaterialTheme.typography.bodyMedium, color = if (member.ready) Volt else Silver)
                }
                if (!member.isHost && member.distanceM >= 0) Text("${member.distanceM}m", color = Silver, style = MaterialTheme.typography.bodyMedium)
            }
            if (canKick) {
                androidx.compose.material3.IconButton(onClick = onKick) {
                    Icon(Icons.Filled.Close, stringResource(R.string.party_kick), tint = Silver, modifier = Modifier.size(22.dp))
                }
            }
        }
    }
}

@Composable
private fun CountdownOverlay(count: Int) {
    val motion = LocalMotion.current
    val feedback = LocalFeedback.current
    LaunchedEffect(count) { if (count > 0) feedback?.play(FeedbackCue.Countdown) }
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Night.copy(alpha = 0.92f)),
        contentAlignment = Alignment.Center,
    ) {
        // S2 카운트다운 — 파란 한 줄 위에 가는 큰 숫자 하나
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            com.stepup.android.ui.components.S2Kicker(stringResource(R.string.crew_all_ready))
            Spacer(Modifier.height(12.dp))
            AnimatedContent(targetState = count, transitionSpec = {
                (fadeIn(tween(motion.duration(130))) + scaleIn(tween(motion.duration(320)), initialScale = .86f)) togetherWith
                    fadeOut(tween(motion.duration(100)))
            }, label = "partyCountdown") { digit ->
                Text(
                    text = if (digit > 0) "$digit" else stringResource(R.string.crew_go),
                    fontFamily = com.stepup.android.ui.theme.StepUpSans,
                    fontSize = if (digit > 0) 160.sp else 56.sp,
                    fontWeight = FontWeight.Normal,
                    letterSpacing = (-4).sp,
                    color = Snow,
                )
            }
        }
    }
}
