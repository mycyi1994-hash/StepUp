package com.stepup.android.ui.screens.walk

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.Chat
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.LocationOn
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.coerceIn
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import com.stepup.android.R
import com.stepup.android.data.repo.PartyMember
import com.stepup.android.data.repo.PartyPhase
import com.stepup.android.data.repo.PartyProblem
import com.stepup.android.data.repo.PartyState
import com.stepup.android.ui.components.RunSheet
import com.stepup.android.ui.components.runTextStyle
import com.stepup.android.ui.experience.feedbackClickable
import com.stepup.android.ui.theme.StepUpSans

/*
 * 크루 달리기 v2 — 크루 러닝 대기실(CR04 준비 전 · CR05 준비 완료 · CR06 진행자 · CR15 혼자 대기)과 동시 출발(CR08).
 *
 * 번개러닝 대기실은 예전 모습 그대로다(PartyLobbyContent) — 이 화면은 크루 방에서만 쓴다.
 * 방은 서버에 있다. 진행자는 서버가 정한 역할이고(크루장과 같다고 보지 않는다) 준비를 마친 사람끼리 출발한다 — 준비 전 인원이
 * 있으면 한 번 묻는다(CR07). 출발 시각도 서버가 정하고 모두 같은 시각에 3-2-1 — 이 화면은 숫자만 보인다(멈추거나 앞당기지 않는다).
 */

/** 크루 대기실 — 상태 없이 그린다. 버튼 갈래는 예전 대기실과 같다(권한 · 중복 시작 막기는 서버와 저장소가 한다) */
@Composable
internal fun CrewLobbyContent(
    party: PartyState,
    title: String,
    resultPoints: Double?,
    canNotify: Boolean,
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
) {
    val c = crewColors()
    if (party.phase == PartyPhase.COUNTDOWN) {
        CrewCountdown(party, title, onBack)
        return
    }
    val alone = party.partySize <= 1
    val lobby = party.phase == PartyPhase.LOBBY || party.phase == PartyPhase.IDLE
    // 전체 명단(그리드에 다 들어가지 않을 때 · 진행자의 내보내기) · 함께 달린 인원 기준 — 화면 안 상태(고르기만)
    var roster by rememberSaveable { mutableStateOf(false) }
    var rule by rememberSaveable { mutableStateOf(false) }
    Box(Modifier.fillMaxSize().background(c.screen).testTag("party-lobby")) {
        Column(Modifier.fillMaxSize().safeDrawingPadding()) {
            // 크루 이름 + 정확히 "크루 달리기" 배지(승인 이미지) — 설명 부제는 두지 않는다
            CrewHeader(title, onBack = onBack)
            Column(Modifier.weight(1f).fillMaxWidth().verticalScroll(rememberScrollState()).padding(horizontal = 20.dp)) {
                Spacer(Modifier.height(6.dp))
                // 방에 못 들어갔거나 멈췄으면 왜인지부터
                party.problem?.let { problem ->
                    CrewProblem(problem, onRetry, onLeave)
                    Spacer(Modifier.height(12.dp))
                }
                when (party.phase) {
                    PartyPhase.FINISHED -> CrewLobbyResult(resultPoints)
                    PartyPhase.RUNNING -> CrewNote(stringResource(R.string.run_crew_started), tag = "party-running")
                    else -> if (party.partyId != null) {
                        Text(
                            stringResource(R.string.run_crew_lobby_title),
                            style = TextStyle(fontFamily = StepUpSans, fontWeight = FontWeight.ExtraBold, fontSize = 32.sp, lineHeight = 1.2.em,
                                letterSpacing = (-0.04).em, color = c.text),
                            modifier = Modifier.semantics { heading() }.testTag("party-lobby-title"),
                        )
                        Spacer(Modifier.height(6.dp))
                        CrewReadyLine(party)
                        Spacer(Modifier.height(20.dp))
                        CrewGrid(party.members, onMore = { roster = true })
                        if (alone) {
                            Text(
                                stringResource(R.string.run_crew_alone_hint), style = runTextStyle(14.sp, c.muted, FontWeight.Medium),
                                textAlign = TextAlign.Center,
                                modifier = Modifier.fillMaxWidth().padding(top = 14.dp).testTag("party-alone"),
                            )
                        }
                        // 그리드에 다 들어가지 않거나 진행자가 내보낼 사람이 있으면 전체 명단
                        if (party.partySize > CREW_GRID_MAX || (lobby && party.isHost && party.members.any { !it.isMe })) {
                            CrewTextAction(
                                stringResource(R.string.run_crew_roster_all), { roster = true },
                                Modifier.padding(top = 6.dp).testTag("party-roster-open"), chevron = true,
                            )
                        }
                        Spacer(Modifier.height(14.dp))
                        CrewLine()
                        Spacer(Modifier.height(4.dp))
                        CrewToggleRow(
                            stringResource(R.string.run_my_share), stringResource(R.string.run_crew_share_body), party.myShare, onShare,
                            "party-share-toggle", Modifier.testTag("party-share-location"), icon = Icons.Outlined.LocationOn,
                        )
                        // 혼자면 아래 버튼이 알리기를 맡는다 — 같은 행동을 두 번 두지 않는다
                        if (canNotify && lobby && !alone) {
                            CrewTextAction(
                                stringResource(R.string.run_cr_notify), onNotify, Modifier.testTag("party-notify-link"),
                                icon = Icons.AutoMirrored.Outlined.Chat, chevron = true,
                            )
                        }
                        // 함께 달린 인원 기준(서버 규칙)은 열어 볼 때만 — 정상 화면에 문단으로 펼쳐 두지 않는다
                        CrewTextAction(
                            stringResource(R.string.run_crew_rule_link), { rule = true }, Modifier.testTag("party-rule-open"),
                            icon = Icons.Outlined.Info, chevron = true,
                        )
                    }
                }
                Spacer(Modifier.height(12.dp))
            }
            Column(
                Modifier.fillMaxWidth().padding(horizontal = 20.dp).padding(top = 8.dp, bottom = 12.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                when {
                    party.phase == PartyPhase.FINISHED ->
                        FreeRunButton(stringResource(R.string.common_ok), onDismissResult, Modifier.fillMaxWidth().testTag("party-result-ok"))
                    // 누른 것이 막힌 것뿐(FAILED)이거나 새 대기실 안내면 방에 그대로 있으니 버튼도 그대로 — 다시 누르면 문제가 풀린다
                    !lobby || (party.problem != null && party.problem != PartyProblem.FAILED &&
                        party.problem != PartyProblem.MISSED_START) || party.partyId == null -> Unit
                    !party.myReady -> {
                        FreeRunButton(stringResource(R.string.run_crew_get_ready), onReady, Modifier.fillMaxWidth().testTag("party-ready"))
                        if (alone && canNotify) {
                            FreeRunButton(stringResource(R.string.run_cr_notify), onNotify, Modifier.fillMaxWidth().testTag("party-notify"),
                                kind = FreeButtonKind.Secondary)
                        }
                    }
                    party.isHost -> {
                        FreeRunButton(
                            if (alone) stringResource(R.string.run_cr_start_alone) else stringResource(R.string.run_crew_start_n, party.readyCount),
                            onStart, Modifier.fillMaxWidth().testTag("party-start"),
                        )
                        if (alone && canNotify) {
                            FreeRunButton(stringResource(R.string.run_cr_notify), onNotify, Modifier.fillMaxWidth().testTag("party-notify"),
                                kind = FreeButtonKind.Secondary)
                        } else {
                            FreeRunButton(stringResource(R.string.run_cr_cancel_ready), onCancelReady,
                                Modifier.fillMaxWidth().testTag("party-cancel-ready"), kind = FreeButtonKind.Secondary)
                        }
                    }
                    // 크루원 준비 완료(CR05) — 출발은 진행자가 · 서버가
                    else -> Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        FreeRunButton(stringResource(R.string.run_cr_waiting_start), {}, Modifier.weight(1.4f).testTag("party-waiting"),
                            enabled = false)
                        FreeRunButton(stringResource(R.string.run_cr_cancel_ready), onCancelReady, Modifier.weight(1f).testTag("party-cancel-ready"),
                            kind = FreeButtonKind.Secondary)
                    }
                }
            }
        }
    }
    if (roster) CrewRosterSheet(party, canKick = lobby && party.isHost, onKick = onKick, onClose = { roster = false })
    if (rule) CrewRuleSheet(onClose = { rule = false })
}

/** 그리드에 두는 칸 — 3 × 2. 더 많으면 다섯 사람과 "+N"(전체 명단) */
internal const val CREW_GRID_MAX = 6

/** 준비 점을 두는 최대 인원 — 더 많으면 글만(점이 줄을 넘지 않게) */
private const val CREW_DOTS_MAX = 8

/** "4 / 6명 준비" + 준비 점 — 버튼 · 그리드와 같은 명단에서 센다(readyCount · partySize) */
@Composable
private fun CrewReadyLine(party: PartyState) {
    val c = crewColors()
    Row(
        Modifier.fillMaxWidth().semantics(mergeDescendants = true) { liveRegion = LiveRegionMode.Polite }.testTag("party-counts"),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            stringResource(R.string.run_crew_ready_of, party.readyCount, party.partySize),
            style = runTextStyle(20.sp, c.ready, FontWeight.ExtraBold, 1.25f),
            modifier = Modifier.weight(1f).testTag("party-ready-count"),
        )
        if (party.partySize in 2..CREW_DOTS_MAX) {
            Spacer(Modifier.width(10.dp))
            CrewReadyDots(party.readyCount, party.partySize)
        }
    }
}

/** 참가자 그리드 — 진행자 · 나 · 나머지 순서. 칸 크기는 화면 폭에서(작은 화면에서도 세 칸) */
@Composable
private fun CrewGrid(members: List<PartyMember>, onMore: () -> Unit) {
    val ordered = crewOrder(members)
    val shown = if (ordered.size > CREW_GRID_MAX) ordered.take(CREW_GRID_MAX - 1) else ordered
    val rest = ordered.size - shown.size
    BoxWithConstraints(Modifier.fillMaxWidth().testTag("party-members")) {
        val gap = 10.dp
        val avatar = ((maxWidth - gap * 2) / 3 - 20.dp).coerceIn(56.dp, 92.dp)
        val cells: List<PartyMember?> = shown + if (rest > 0) listOf(null) else emptyList()
        Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(18.dp)) {
            cells.chunked(3).forEach { row ->
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(gap)) {
                    row.forEach { member ->
                        if (member != null) CrewGridCell(member, avatar, Modifier.weight(1f))
                        else CrewMoreCell(rest, avatar, onMore, Modifier.weight(1f))
                    }
                    repeat(3 - row.size) { Spacer(Modifier.weight(1f)) }
                }
            }
        }
    }
}

/** 전체 명단 — 그리드와 같은 순서 · 준비 상태. 진행자는 대기실에서 내보낼 수 있다(예전 명단 줄 그대로) */
@Composable
private fun CrewRosterSheet(party: PartyState, canKick: Boolean, onKick: (String) -> Unit, onClose: () -> Unit) {
    RunSheet(onDismiss = onClose, modifier = Modifier.testTag("party-roster-sheet")) {
        CrewSheetTitle(stringResource(R.string.run_crew_lobby_label))
        Spacer(Modifier.height(12.dp))
        CrewCard(Modifier.heightIn(max = 420.dp).verticalScroll(rememberScrollState()).testTag("party-roster-list"), padding = 14.dp) {
            crewOrder(party.members).forEachIndexed { index, member ->
                if (index > 0) CrewLine()
                CrewLobbyRow(member, canKick = canKick && !member.isMe, onKick = { onKick(member.id) })
            }
        }
        Spacer(Modifier.height(14.dp))
        FreeRunButton(stringResource(R.string.common_confirm), onClose, Modifier.fillMaxWidth().testTag("party-roster-close"))
    }
}

/** 함께 달린 인원 기준(서버 0056 party_near) — 열어 볼 때만 */
@Composable
private fun CrewRuleSheet(onClose: () -> Unit) {
    RunSheet(onDismiss = onClose, modifier = Modifier.testTag("party-rule-sheet")) {
        CrewSheetTitle(
            stringResource(R.string.run_crew_rule_link),
            body = stringResource(R.string.run_crew_gps_rule, com.stepup.android.data.repo.CrewRepository.NEAR_DISTANCE_M),
        )
        Spacer(Modifier.height(16.dp))
        FreeRunButton(stringResource(R.string.common_confirm), onClose, Modifier.fillMaxWidth().testTag("party-rule-close"))
    }
}

/** 크루원 한 줄 — 이니셜 · 이름(· 나) · 진행자 · 크루원 · 준비 상태. 진행자는 대기실에서 내보낼 수 있다 */
@Composable
private fun CrewLobbyRow(member: PartyMember, canKick: Boolean, onKick: () -> Unit) {
    val c = crewColors()
    val meta = when {
        member.isHost -> stringResource(R.string.run_cr_host)
        member.isMe -> stringResource(R.string.run_crew_my_run)
        else -> stringResource(R.string.run_crew_member)
    }
    CrewPersonRow(member, meta, Modifier.semantics(mergeDescendants = true) {}.testTag("party-member-${member.id}")) {
        CrewReadyMark(member.ready)
        if (canKick) {
            val label = stringResource(R.string.run_cr_kick)
            Box(
                Modifier.size(48.dp).clip(CircleShape).feedbackClickable(onClick = onKick)
                    .semantics { contentDescription = label }.testTag("party-kick-${member.id}"),
                contentAlignment = Alignment.Center,
            ) {
                Icon(Icons.Filled.Close, contentDescription = null, tint = c.muted, modifier = Modifier.size(18.dp))
            }
        }
    }
}

/** 멈춘 까닭 — 서버에 닿지 못한 것은 저절로 다시 묻는다. 나머지는 사람이 정한다 */
@Composable
private fun CrewProblem(problem: PartyProblem, onRetry: () -> Unit, onLeave: () -> Unit) {
    val message = stringResource(
        when (problem) {
            PartyProblem.SIGN_IN -> R.string.party_problem_sign_in
            PartyProblem.REMOVED -> R.string.party_problem_removed
            PartyProblem.NETWORK -> R.string.party_problem_network
            PartyProblem.CLOSED -> R.string.party_problem_closed
            PartyProblem.FAILED -> R.string.party_problem_failed
            PartyProblem.MISSED_START -> R.string.party_problem_missed_start
        },
    )
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        // 새 대기실 안내는 알림일 뿐 — 대기실은 그대로 쓴다
        if (problem == PartyProblem.MISSED_START) CrewNote(message, tag = "party-problem") else CrewError(message, tag = "party-problem")
        if (problem == PartyProblem.CLOSED || problem == PartyProblem.REMOVED) {
            FreeRunButton(stringResource(R.string.crew_retry), onRetry, Modifier.fillMaxWidth().testTag("party-retry"))
        }
        // 누른 것이 막힌 것뿐이거나 새 대기실 안내면 방에는 그대로 있다
        if (problem != PartyProblem.FAILED && problem != PartyProblem.MISSED_START) {
            FreeRunButton(stringResource(R.string.run_cr_leave), onLeave, Modifier.fillMaxWidth().testTag("party-leave"),
                kind = FreeButtonKind.Secondary)
        }
    }
}

/** 끝난 크루 러닝 — 금액은 서버가 확인한 뒤에만(그 전에는 "서버 확인 중") */
@Composable
private fun CrewLobbyResult(resultPoints: Double?) {
    val c = crewColors()
    CrewCard(Modifier.testTag("party-result"), padding = 18.dp) {
        Spacer(Modifier.height(10.dp))
        Text(stringResource(R.string.crew_result_title), style = runTextStyle(18.sp, c.text, FontWeight.ExtraBold))
        Spacer(Modifier.height(8.dp))
        if (resultPoints != null) {
            Text(stringResource(R.string.run_cr_result_points), style = runTextStyle(13.sp, c.label))
            Text("+%.2f SUP".format(resultPoints), style = runTextStyle(28.sp, c.text, FontWeight.ExtraBold),
                modifier = Modifier.testTag("party-result-points"))
        } else {
            Text(stringResource(R.string.finish_pending_short), style = runTextStyle(16.sp, c.label, FontWeight.SemiBold),
                modifier = Modifier.testTag("party-result-pending"))
        }
        Spacer(Modifier.height(10.dp))
    }
}

/**
 * 동시 출발(CR08) — 서버가 정한 시각까지 남은 초. 출발 명단(준비를 마친 사람)의 이니셜 줄. 한 사람이 끊을 수 없다(나가기는 뒤로).
 * 시안의 "카운트다운 재생"은 검토 도구라 두지 않는다.
 */
@Composable
private fun CrewCountdown(party: PartyState, title: String, onBack: () -> Unit) {
    val c = crewColors()
    val going = party.members.filter { it.ready }.ifEmpty { party.members }
    Box(Modifier.fillMaxSize().background(c.screen).testTag("party-countdown")) {
        Column(Modifier.fillMaxSize().safeDrawingPadding(), horizontalAlignment = Alignment.CenterHorizontally) {
            CrewHeader(title, subtitle = stringResource(R.string.run_crew_go_ready_sub), onBack = onBack)
            Spacer(Modifier.weight(1f))
            Row(
                Modifier.clip(RoundedCornerShape(20.dp)).background(c.readyFace).padding(horizontal = 12.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Box(Modifier.size(7.dp).clip(CircleShape).background(c.live))
                Spacer(Modifier.width(6.dp))
                Text(stringResource(R.string.run_crew_go_pill), style = runTextStyle(13.sp, c.ready, FontWeight.Bold))
            }
            Spacer(Modifier.height(14.dp))
            Text(
                if (going.size > 1) stringResource(R.string.run_crew_go_together, going.size) else stringResource(R.string.run_crew_go_alone),
                style = TextStyle(fontFamily = StepUpSans, fontWeight = FontWeight.ExtraBold, fontSize = 25.sp, color = c.text,
                    letterSpacing = (-0.03).em),
                textAlign = TextAlign.Center, modifier = Modifier.padding(horizontal = 20.dp).testTag("party-countdown-together"),
            )
            Text(
                "${party.countdown.coerceAtLeast(1)}",
                style = TextStyle(fontFamily = StepUpSans, fontWeight = FontWeight.ExtraBold, fontSize = 150.sp, color = c.text,
                    letterSpacing = (-0.06).em),
                modifier = Modifier.padding(vertical = 8.dp).semantics { liveRegion = LiveRegionMode.Assertive }
                    .testTag("party-countdown-digit"),
            )
            val shown = crewOrder(going).take(6)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                shown.forEach { CrewAvatar(it.name, it.id, it.isMe, size = 40.dp) }
                if (going.size > shown.size) {
                    Text("+${going.size - shown.size}", style = runTextStyle(15.sp, c.label, FontWeight.Bold))
                }
            }
            Spacer(Modifier.weight(1.2f))
        }
    }
}
