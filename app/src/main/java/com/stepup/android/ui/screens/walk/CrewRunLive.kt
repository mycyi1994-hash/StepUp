package com.stepup.android.ui.screens.walk

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.OpenInFull
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material.icons.outlined.PeopleAlt
import androidx.compose.material.icons.outlined.LinkOff
import androidx.compose.material.icons.outlined.WifiOff
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Immutable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.progressBarRangeInfo
import androidx.compose.ui.semantics.ProgressBarRangeInfo
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.stepup.android.R
import com.stepup.android.data.repo.PartyMember
import com.stepup.android.data.repo.PartyProblem
import com.stepup.android.data.repo.PartyState
import com.stepup.android.ui.components.LocalMapTone
import com.stepup.android.ui.components.runTextStyle
import com.stepup.android.ui.components.runTone
import com.stepup.android.ui.experience.feedbackClickable

/*
 * 크루 달리기 v2 — 달리는 중(CR09) · 함께 달리는 사람(CR10) · 내 러닝 일시정지(CR11) · 내 러닝 종료(CR12) · 연결 끊김(CR18).
 *
 * 위에서부터 크루 이름 머리("참여 n명")와 "크루 달리기" 배지, (띠 한 줄), 밝은 실제 지도 카드(내 경로 · 위치를 공유한 사람),
 * 같이 달리는 사람 줄, 거리 | 시간 | 평균 페이스 카드, 내 목표 진행률 카드(러닝 정보 ›), 일시정지 | 종료(2026-10-16 크루 전달본).
 * 예상 보상 · 내구도는 정상 화면에 두지 않는다 — 러닝 정보(E03)가 지금 값을 보인다.
 *
 * 다른 사람이 달리는지 멈췄는지는 서버가 알려 주지 않는다 — "참여 n명"만 적고 "모두 달리는 중"이라 하지 않는다.
 * 내 일시정지는 내 기록만 멈춘다. 연결이 끊기거나 방이 닫히면 옛 명단 · 위치를 지금 것처럼 보이지 않는다(내 기록은 이어진다).
 * 공동 목표 · 팀 점수는 없다 — 목표는 내 목표뿐이다.
 */

// ── 판단(화면 밖에서 검사한다 — CrewRunTest) ───────────────────────

/** 크루 달리기 중 방과의 연결 — 명단 · 다른 사람 위치를 보일지 정한다 */
internal enum class CrewLink {
    /** 같이 뛰는 방과 이어져 있다 */
    Linked,

    /** 서버에 잠시 닿지 못한다(CR18) — 다시 물으면 풀린다 */
    Offline,

    /** 진행자가 마쳐 방이 닫혔다 — 더 묻지 않는다. 내 러닝은 내가 멈출 때까지 */
    Closed,

    /** 이 러닝의 방과 끊겼다 — 앱이 다시 켜졌거나(되살린 러닝) 방에서 빠졌다 · 로그인이 풀렸다 */
    Detached,
}

/** [crewId] 는 이 러닝을 연 크루(출발할 때 적은 값) — 지금 방이 다른 크루 · 번개면 이 러닝의 방이 아니다 */
internal fun crewLink(party: PartyState, crewId: String): CrewLink = when {
    !party.isActive || party.flashPostId != null || (crewId.isNotBlank() && party.crewId != crewId) -> CrewLink.Detached
    party.roomClosed -> CrewLink.Closed
    party.problem == PartyProblem.REMOVED || party.problem == PartyProblem.SIGN_IN -> CrewLink.Detached
    party.networkProblem -> CrewLink.Offline
    else -> CrewLink.Linked
}

/** 지도에 올릴 다른 사람 — 이어져 있는 동안, 위치 공유를 켰고 좌표가 있는 사람만. 공유하지 않는 사람은 줄에만 있다 */
internal fun crewPins(party: PartyState, link: CrewLink): List<PartyMember> =
    if (link != CrewLink.Linked) emptyList() else party.members.filter { !it.isMe && it.sharing && it.point != null }

/** 마칠 때 무슨 일이 생기나 — 진행자가 마치면 방이 닫힌다(서버 party_finish), 참가자는 방에서 나온다(party_leave) */
internal enum class CrewEnd { Host, Member, Alone }

internal fun crewEnd(party: PartyState, link: CrewLink): CrewEnd = when {
    link != CrewLink.Linked && link != CrewLink.Offline -> CrewEnd.Alone
    party.members.count { !it.isMe } == 0 -> CrewEnd.Alone
    party.isHost -> CrewEnd.Host
    else -> CrewEnd.Member
}

// ── 달리는 중 ──────────────────────────────────────────────────────

/** 크루 달리기 화면의 때 — 달리는 중(일시정지 포함) · 기록 저장 중 · 저장 실패(기록은 서비스에 그대로, 같은 러닝으로 다시 저장) */
internal enum class CrewLiveMode { Live, Saving, Failed }

@Immutable
internal data class CrewLiveUi(
    val crewName: String,
    /** 이어져 있을 때의 명단(나 포함) — 아니면 비어 있다 */
    val members: List<PartyMember>,
    val link: CrewLink,
    val paused: Boolean,
    /** "1.56" · 위치 없이 기록하면 "—" */
    val distance: String,
    val time: String,
    val pace: String,
    val goalKm: Double,
    /** 내 목표 진행(0..1) — 거리를 모르면 null */
    val goalFraction: Float?,
)

/**
 * 크루 달리기 중 화면 — 상태 없이 그린다. [problemBand] 는 지도 위 노란 띠(위치 · 측정 문제 — 자유 러닝과 같은 띠).
 * [onPrimary] 는 일시정지 · 다시 달리기, [onFinish] 는 종료(멈추고 묻는다), [onDetails] 는 러닝 정보(E03 — 예상 보상 · 내구도).
 */
@Composable
internal fun CrewRunLive(
    ui: CrewLiveUi,
    map: @Composable BoxScope.() -> Unit,
    onPrimary: () -> Unit,
    onFinish: () -> Unit,
    onTogether: () -> Unit,
    onDetails: () -> Unit,
    onExpandMap: (() -> Unit)?,
    onReconnect: () -> Unit,
    modifier: Modifier = Modifier,
    mode: CrewLiveMode = CrewLiveMode.Live,
    problemBand: (@Composable () -> Unit)? = null,
) {
    val c = crewColors()
    val t = runTone()
    BoxWithConstraints(modifier.fillMaxSize().background(c.screen).testTag("run-live")) {
        val compact = maxHeight < 640.dp
        val gap = if (compact) 8.dp else 12.dp
        Column(Modifier.fillMaxSize()) {
            CrewHeader(
                title = ui.crewName,
                subtitle = when (ui.link) {
                    CrewLink.Linked -> stringResource(R.string.run_cr_joined, ui.members.size.coerceAtLeast(1))
                    CrewLink.Offline -> stringResource(R.string.run_crew_checking)
                    CrewLink.Closed -> stringResource(R.string.run_crew_room_closed_sub)
                    CrewLink.Detached -> stringResource(R.string.run_crew_detached_sub)
                },
                dot = when (ui.link) {
                    CrewLink.Linked -> c.live
                    CrewLink.Offline -> c.muted
                    else -> null
                },
                modifier = Modifier.statusBarsPadding(),
            )
            // 띠 한 줄 — 지도 카드와 같은 여백 · 둥근 모서리
            val band = Modifier.padding(horizontal = 16.dp).padding(bottom = 8.dp).clip(RoundedCornerShape(12.dp))
            when {
                mode == CrewLiveMode.Failed ->
                    CrewBand(stringResource(R.string.run_free_save_failed), Icons.Filled.Warning, "run-save-error", band)
                mode == CrewLiveMode.Saving -> Unit
                ui.link == CrewLink.Offline ->
                    CrewBand(stringResource(R.string.run_crew_offline_band), Icons.Outlined.WifiOff, "run-crew-offline", band)
                ui.link == CrewLink.Closed ->
                    CrewBand(stringResource(R.string.run_crew_closed_band), Icons.Outlined.PeopleAlt, "run-crew-closed", band)
                ui.link == CrewLink.Detached ->
                    CrewBand(stringResource(R.string.run_crew_detached_band), Icons.Outlined.LinkOff, "run-crew-detached", band)
                ui.paused -> CrewBand(stringResource(R.string.run_crew_paused_band), Icons.Filled.Pause, "run-crew-others", band)
            }
            // 지도 카드 — 남는 높이를 모두 쓴다(버튼 · 기록은 늘 화면 안). 밝은 실제 지도 그대로(어두운 덧칠 없음)
            val shape = RoundedCornerShape(16.dp)
            Box(
                Modifier.weight(1f).fillMaxWidth().padding(horizontal = 16.dp).clip(shape).background(t.inset)
                    .border(1.dp, c.line, shape).clipToBounds(),
            ) {
                CompositionLocalProvider(LocalMapTone provides CrewBrightMap) {
                    Box(Modifier.fillMaxSize().testTag("run-live-map"), content = map)
                }
                if (problemBand != null) {
                    Box(Modifier.fillMaxWidth().padding(horizontal = 10.dp).padding(top = 10.dp)) { problemBand() }
                }
                if (onExpandMap != null) {
                    FreeMapButton(
                        Icons.Filled.OpenInFull, stringResource(R.string.run_map_expand), "run-map-expand", onExpandMap,
                        Modifier.align(Alignment.BottomEnd).padding(10.dp),
                    )
                }
            }
            Column(
                Modifier.fillMaxWidth().background(c.screen).navigationBarsPadding()
                    .padding(horizontal = 16.dp).padding(top = gap, bottom = gap),
            ) {
                when (ui.link) {
                    CrewLink.Linked -> CrewRoster(ui.members, onTogether, compact = compact)
                    CrewLink.Offline -> CrewNote(stringResource(R.string.run_crew_offline_note), tag = "run-crew-offline-card")
                    CrewLink.Closed -> CrewNote(stringResource(R.string.run_crew_closed_note), tag = "run-crew-closed-note")
                    CrewLink.Detached -> CrewNote(stringResource(R.string.run_crew_detached_note), icon = Icons.Outlined.LinkOff,
                        tag = "run-crew-detached-note")
                }
                Spacer(Modifier.height(gap))
                CrewCard(padding = 12.dp) {
                    Spacer(Modifier.height(if (compact) 2.dp else 6.dp))
                    CrewStatsRow(ui.distance, ui.time, ui.pace, if (compact) 24.sp else 28.sp, center = true)
                    Spacer(Modifier.height(if (compact) 2.dp else 6.dp))
                }
                Spacer(Modifier.height(if (compact) 6.dp else 8.dp))
                // 러닝 정보(E03)는 달리는 동안에만 — 저장 중 · 저장 실패에는 열 것이 없다
                CrewGoal(ui.goalKm, ui.goalFraction, onDetails.takeIf { mode == CrewLiveMode.Live })
                Spacer(Modifier.height(gap))
                when (mode) {
                    // 기록 저장 중 — 버튼 하나(누를 수 없음). 보상을 기다리는 것이 아니다
                    CrewLiveMode.Saving -> FreeRunButton(
                        stringResource(R.string.run_saving_title), {}, Modifier.fillMaxWidth().testTag("run-primary-action"),
                        kind = FreeButtonKind.Secondary, enabled = false, busy = true,
                    )
                    // 저장 실패 — 기록은 이 휴대폰에 그대로 있다. 같은 러닝으로 다시 저장한다
                    CrewLiveMode.Failed -> {
                        Text(stringResource(R.string.run_free_save_kept), style = runTextStyle(13.sp, c.label, FontWeight.Medium),
                            modifier = Modifier.padding(bottom = 8.dp).testTag("run-save-kept"))
                        FreeRunButton(stringResource(R.string.runflow_save_again), onPrimary, Modifier.fillMaxWidth().testTag("run-primary-action"))
                    }
                    // 일시정지 · 다시 달리기가 주 행동, 종료는 보조
                    CrewLiveMode.Live -> Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        FreeRunButton(
                            stringResource(if (ui.paused) R.string.run_free_resume else R.string.run_pause), onPrimary,
                            Modifier.weight(1.15f).testTag("run-primary-action"),
                            icon = if (ui.paused) Icons.Filled.PlayArrow else Icons.Filled.Pause,
                        )
                        FreeRunButton(
                            stringResource(R.string.run_free_stop), onFinish, Modifier.weight(1f).testTag("run-finish"),
                            kind = FreeButtonKind.Secondary, stopMark = true,
                        )
                    }
                }
                if (mode == CrewLiveMode.Live && ui.link == CrewLink.Offline) {
                    CrewTextAction(
                        stringResource(R.string.run_crew_reconnect_again), onReconnect,
                        Modifier.fillMaxWidth().testTag("run-crew-reconnect"), center = true,
                    )
                }
            }
        }
    }
}

/** 거리 | 시간 | 페이스 — 이름 위, 큰 수(세로선으로 나눈다) */
@Composable
internal fun CrewStatsRow(
    distance: String,
    time: String,
    pace: String,
    size: androidx.compose.ui.unit.TextUnit = 28.sp,
    /** 칸마다 붙이는 이름표(검사용) — 마칠까요 시트는 러닝 중 화면과 겹치지 않게 다른 이름 */
    tags: List<String> = listOf("run-distance-value", "run-hero-value", "run-pace-value"),
    center: Boolean = false,
) {
    DividedRow(columns = 3, gap = 14.dp) {
        CrewStat(stringResource(R.string.run_label_distance), distance, "km", tags[0], size, center)
        CrewStat(stringResource(R.string.stat_time), time, null, tags[1], size, center)
        CrewStat(stringResource(R.string.run_label_pace), pace, "/km", tags[2], size, center)
    }
}

/**
 * 내 목표 {n}km · 진행률 카드 — 거리를 모르면 "확인할 수 없어요"(막대는 비운다). 오른쪽은 러닝 정보(E03) 입구 —
 * 예상 보상 · 내구도 · 측정 까닭은 거기서 본다. [onDetails] 가 없으면(저장 중 · 실패) 입구를 두지 않는다.
 */
@Composable
private fun CrewGoal(goalKm: Double, fraction: Float?, onDetails: (() -> Unit)?) {
    val c = crewColors()
    val goal = if (goalKm % 1.0 == 0.0) "%.0f".format(goalKm) else "%.1f".format(goalKm)
    val line = if (fraction != null) {
        stringResource(R.string.run_crew_goal, goal, (fraction * 100).toInt())
    } else {
        stringResource(R.string.run_crew_goal_unknown, goal)
    }
    CrewCard(padding = 14.dp) {
        Row(Modifier.fillMaxWidth().heightIn(min = 52.dp), verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f).padding(vertical = 6.dp).semantics(mergeDescendants = true) {
                contentDescription = line
                if (fraction != null) progressBarRangeInfo = ProgressBarRangeInfo(fraction, 0f..1f)
            }.testTag("run-goal-bar")) {
                Text(line, style = runTextStyle(14.sp, c.text, FontWeight.SemiBold), maxLines = 2, overflow = TextOverflow.Ellipsis)
                Spacer(Modifier.height(8.dp))
                Box(Modifier.fillMaxWidth().height(8.dp).clip(RoundedCornerShape(4.dp)).background(c.goalTrack)) {
                    if (fraction != null && fraction > 0f) {
                        Box(Modifier.fillMaxWidth(fraction.coerceIn(0f, 1f)).height(8.dp).clip(RoundedCornerShape(4.dp)).background(c.badge))
                    }
                }
            }
            if (onDetails != null) {
                Spacer(Modifier.width(10.dp))
                CrewTextAction(stringResource(R.string.run_details), onDetails, Modifier.testTag("run-details-open"), chevron = true)
            }
        }
    }
}

// ── 함께 달리는 사람(CR10) ─────────────────────────────────────────

/**
 * 함께 달리는 사람 — 진행자 · 나 · 참여 중. 거리는 공유를 켠 사람만(각 휴대폰이 잰 화면용 값 — 보상과 무관), 나머지는 "비공개".
 * 다른 사람이 달리는지 멈췄는지는 모른다 — 내 줄만 달리는 중 · 일시정지를 적는다.
 * [myKm] 은 내 거리(위치 없이 기록하면 null — "—").
 */
@Composable
internal fun ColumnScope.CrewTogetherSheetContent(
    members: List<PartyMember>,
    myKm: Double?,
    myPaused: Boolean,
    myShare: Boolean,
    onShare: (Boolean) -> Unit,
    onClose: () -> Unit,
) {
    val c = crewColors()
    CrewSheetTitle(stringResource(R.string.run_together_sheet_title, members.size), modifier = Modifier.testTag("run-together-title"))
    Spacer(Modifier.height(14.dp))
    CrewCard(Modifier.heightIn(max = 380.dp).verticalScroll(rememberScrollState()).testTag("run-together-list"), padding = 14.dp) {
        crewOrder(members).forEachIndexed { i, m ->
            if (i > 0) CrewLine()
            val meta = when {
                m.isHost && m.isMe -> stringResource(R.string.run_crew_host_me)
                m.isHost -> stringResource(R.string.run_cr_host)
                m.isMe && myPaused -> stringResource(R.string.run_together_paused)
                m.isMe -> stringResource(R.string.run_crew_my_run)
                else -> stringResource(R.string.run_together_in)
            }
            CrewPersonRow(m, meta, avatar = 40.dp) {
                val km = if (m.isMe) myKm else m.km.takeIf { m.sharing }
                Text(
                    when {
                        km != null -> "%.2f km".format(km)
                        m.isMe -> "—"
                        else -> stringResource(R.string.run_together_hidden)
                    },
                    style = runTextStyle(15.sp, if (km != null) c.text else c.muted, FontWeight.SemiBold),
                    modifier = Modifier.testTag("run-together-km-${m.id}"),
                )
            }
        }
    }
    Spacer(Modifier.height(6.dp))
    CrewToggleRow(
        stringResource(R.string.run_crew_share_location), stringResource(if (myShare) R.string.run_my_share_on else R.string.run_my_share_off),
        myShare, onShare, "run-together-share-toggle", Modifier.testTag("run-together-share"),
    )
    Text(stringResource(R.string.run_together_note), style = runTextStyle(12.sp, c.muted, FontWeight.Normal, 1.45f),
        modifier = Modifier.fillMaxWidth())
    Spacer(Modifier.height(14.dp))
    FreeRunButton(stringResource(R.string.common_confirm), onClose, Modifier.fillMaxWidth().testTag("run-together-close"))
}

// ── 내 러닝 종료(CR12) ─────────────────────────────────────────────

/**
 * 내 러닝을 마칠까요? — 실제로 일어나는 일만 적는다. 참가자: 저장하고 함께 달리기에서 나온다(다른 참가자는 그대로).
 * 진행자: 저장하면 이 함께 달리기가 끝난다(방이 닫힌다 — 다른 참가자의 기록은 각자 휴대폰에서 이어지지만 서로 보이지 않는다).
 * 혼자: 저장하고 마친다. 선택하는 동안 내 기록은 멈춰 있다.
 */
@Composable
internal fun ColumnScope.CrewFinishSheetContent(
    end: CrewEnd,
    distance: String,
    time: String,
    pace: String,
    onSave: () -> Unit,
    onContinue: () -> Unit,
    onDiscard: (() -> Unit)?,
) {
    val c = crewColors()
    CrewSheetTitle(
        stringResource(R.string.run_crew_finish_title),
        body = stringResource(
            when (end) {
                CrewEnd.Host -> R.string.run_crew_end_host
                CrewEnd.Member -> R.string.run_crew_end_member
                CrewEnd.Alone -> R.string.run_crew_end_alone
            },
        ),
        modifier = Modifier.testTag("run-crew-end-${end.name.lowercase()}"),
    )
    Spacer(Modifier.height(14.dp))
    CrewStatsRow(distance, time, pace, 26.sp, tags = listOf("run-end-distance", "run-end-time", "run-end-pace"))
    if (end != CrewEnd.Alone) {
        Spacer(Modifier.height(14.dp))
        CrewNote(
            stringResource(if (end == CrewEnd.Host) R.string.run_crew_end_host_note else R.string.run_crew_end_member_note),
            tag = "run-crew-end-note",
        )
    }
    Spacer(Modifier.height(10.dp))
    Text(stringResource(R.string.run_crew_end_paused), style = runTextStyle(13.sp, c.muted, FontWeight.Medium),
        modifier = Modifier.fillMaxWidth())
    Spacer(Modifier.height(14.dp))
    FreeRunButton(stringResource(R.string.run_free_save_finish), onSave, Modifier.fillMaxWidth().testTag("run-end-save"))
    Spacer(Modifier.height(10.dp))
    FreeRunButton(stringResource(R.string.run_continue), onContinue, Modifier.fillMaxWidth().testTag("run-end-continue"),
        kind = FreeButtonKind.Secondary)
    if (onDiscard != null) {
        Spacer(Modifier.height(4.dp))
        Box(
            Modifier.fillMaxWidth().heightIn(min = 48.dp).clip(RoundedCornerShape(12.dp)).feedbackClickable(onClick = onDiscard)
                .testTag("run-end-discard"),
            contentAlignment = Alignment.Center,
        ) {
            Text(stringResource(R.string.run_free_discard), style = runTextStyle(16.sp, freeRunColors().dangerText, FontWeight.Bold),
                textAlign = TextAlign.Center)
        }
    }
}
