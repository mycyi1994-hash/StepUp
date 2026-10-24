package com.stepup.android.ui.screens.walk

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.MoreHoriz
import androidx.compose.material.icons.outlined.CloudOff
import androidx.compose.material.icons.outlined.Flag
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.OpenInFull
import androidx.compose.material.icons.outlined.PersonOff
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import com.stepup.android.R
import com.stepup.android.data.repo.PartyMember
import com.stepup.android.data.repo.PartyPhase
import com.stepup.android.data.repo.PartyProblem
import com.stepup.android.data.repo.PartyState
import com.stepup.android.domain.CrewRunMath
import com.stepup.android.domain.CrewRunRoom
import com.stepup.android.domain.CrewRunRules
import com.stepup.android.domain.CrewRunToday
import com.stepup.android.domain.RoomCourse
import com.stepup.android.domain.Sneaker
import com.stepup.android.ui.components.SneakerVisual
import com.stepup.android.ui.experience.FeedbackCue
import com.stepup.android.ui.experience.LocalMotion
import com.stepup.android.ui.experience.feedbackClickable
import com.stepup.android.ui.screens.community.home.crewRunRuleLines
import com.stepup.android.ui.screens.items.shoeName
import com.stepup.android.ui.theme.StepUpSans

/*
 * 크루 대기실 — 3번 듀얼 레일(2026-10-24 핸드오프 장면 10 ~ 34 · 68 ~ 70, 04_DESIGN_BACKGROUND).
 *
 * 위에서: 앱 머리 → 러닝 이름 · 짧은 부제 → 함께 달릴 크루원(3 × 2, 듀얼 레일) → 왼쪽 크루 내구도 · 크루 목표 / 오른쪽 크루 코스 지도 →
 * 내 신발(듀얼 레일) → 주 버튼 하나(흰 면). 자세한 규칙 · 표 · 산식은 시트로. 배경은 이 화면에만 사진 터널(LOBBY_TUNNEL)이고
 * 시트가 뜨거나 카운트다운이면 멈춘다. 시트는 불투명한 대기실 면(#08243A).
 *
 * 방은 서버에 있다. 진행자는 서버가 정한 역할이고(크루장과 같다고 보지 않는다) 진행자만 출발 · 내보내기 · 코스 바꾸기를 본다.
 * 출발 시각도 서버가 정하고 모두 같은 시각에 3 · 2 · 1 · 0 · StepUP!(CrewLaunchOverlay — 앱 맨 위 겹). 이 화면은 상태 없이 그린다.
 */

/** 그리드에 두는 칸 — 3 × 2. 더 많으면 다섯 사람과 "+N"(전체 명단) */
internal const val CREW_GRID_MAX = 6

/** 크루 대기실 — 상태 없이 그린다. 권한 · 중복 시작 막기는 서버와 저장소가 한다 */
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
    /** 서버에 닿지 못한 동안 "지금 다시 연결" — 걸려 있던 물음을 기다리지 않고 지금 다시 묻는다 */
    onReconnect: () -> Unit = {},
    /** 오늘 크루 러닝(0071) — 읽지 못했으면 null. 오늘 이미 했으면(어느 크루에서든) 준비를 막는다 */
    crewToday: CrewRunToday? = null,
    /** 오늘 공용 체력 · 보너스 미리보기 · 사람마다 고른 신발(0071) — 읽지 못했으면 null */
    room: CrewRunRoom? = null,
    /** 내 신발(고르기 시트) — 서버 신발만 고를 수 있다 */
    shoes: List<Sneaker> = emptyList(),
    onPickShoe: (Long?) -> Unit = {},
    /** 신발 고르기가 서버에 닿지 못했다 */
    shoeFailed: Boolean = false,
    /** 고른 방이 닫혔다 · 내보내졌다 · 찼다 · 출발했다 — 같은 크루의 열린 대기실로 */
    onOtherRooms: () -> Unit = {},
    /** 진행자 — 코스를 바꿨다(서버로 보낸다) */
    onChangeCourse: (RoomCourse) -> Unit = {},
    /** 진행자의 코스 바꾸기가 서버에 닿지 못했다 */
    courseFailed: Boolean = false,
    /** 참가자 — 바뀐 코스를 봤다 */
    onAcknowledgeCourse: () -> Unit = {},
    /** 부모가 띄운 시트(준비 전 확인 · 출발 확인 · 알리기)가 있다 — 배경을 멈추고 어둡게 */
    overlay: Boolean = false,
) {
    val lobby = party.phase == PartyPhase.LOBBY || party.phase == PartyPhase.IDLE
    // 빈 화면 상태(불러오기 실패 · 나옴 · 닫힘 · 찼음 · 출발함) — 방 화면 대신 앱 바탕의 한 화면(69 · 70)
    val terminal = lobbyTerminal(party)
    if (terminal != null) {
        CrewLobbyTerminal(terminal, onBack = onLeave, onRetry = onRetry, onReconnect = onReconnect, onOtherRooms = onOtherRooms)
        return
    }
    if (party.phase == PartyPhase.FINISHED) {
        CrewPage(title = title, onBack = onBack, badge = false, modifier = Modifier.testTag("party-lobby"), bottom = {
            FreeRunButton(stringResource(R.string.common_ok), onDismissResult, Modifier.fillMaxWidth().testTag("party-result-ok"))
        }) {
            Spacer(Modifier.height(12.dp))
            CrewLobbyResult(resultPoints)
        }
        return
    }
    var sheet by rememberSaveable { mutableStateOf(LobbySheet.None) }
    var kickTarget by rememberSaveable { mutableStateOf<String?>(null) }
    var picking by rememberSaveable { mutableStateOf(false) }
    // 진행자 코스 바꾸기의 목록(04) — 대기실 대신 한 화면(뒤로 · 고르기 전 닫기는 원래 코스 그대로)
    if (picking && party.isHost && lobby) {
        BackHandler { picking = false }
        CrewCoursePicker(
            current = party.course,
            onBack = { picking = false },
            onPick = {
                picking = false
                onChangeCourse(it)
            },
        )
        return
    }
    val rules = crewToday?.rules ?: room?.rules ?: CrewRunRules()
    val doneToday = crewToday?.mine?.usedToday == true && lobby
    val loading = party.partyId == null
    val anySheet = sheet != LobbySheet.None || kickTarget != null || overlay || party.courseChanged
    val reduced = LocalMotion.current.reduced
    // 출발 전 대기실에서 뒤로 — 나가기를 한 번 묻는다(21 · 32). 방에 들어가기 전 · 문제가 있으면 바로 나간다
    BackHandler(enabled = lobby && !loading && sheet == LobbySheet.None) { sheet = LobbySheet.Leave }
    Box(Modifier.fillMaxSize().testTag("party-lobby")) {
        // 대기실에만 사진 터널 — 보이고 시트가 없고 출발 전일 때만 흐른다. 불러오는 중은 정지(LOBBY_STATIC)
        LobbyTunnelBackdrop(
            running = lobby && !anySheet, reduced = reduced, dim = anySheet, still = loading,
            modifier = Modifier.testTag(if (loading) "lobby-tunnel-still" else "lobby-tunnel"),
        )
        Column(Modifier.fillMaxSize().safeDrawingPadding()) {
            LobbyHeader(
                onBack = { if (lobby && !loading) sheet = LobbySheet.Leave else onLeave() },
                host = party.isHost && lobby,
                onRules = { sheet = LobbySheet.Rules },
                onMenu = { item ->
                    when (item) {
                        LobbyMenu.Roster -> sheet = LobbySheet.Roster
                        LobbyMenu.Course -> sheet = LobbySheet.Course
                        LobbyMenu.Call -> onNotify()
                        LobbyMenu.Rules -> sheet = LobbySheet.Rules
                        LobbyMenu.Leave -> sheet = LobbySheet.Leave
                    }
                },
                canNotify = canNotify,
                hasCourse = party.course != null,
            )
            Column(
                Modifier.weight(1f).fillMaxWidth().verticalScroll(rememberScrollState())
                    .padding(horizontal = LobbyInk.Gutter).padding(top = 4.dp),
            ) {
                // 러닝 이름 · 부제(크루 이름 · 방을 연 시각)
                Text(
                    party.roomName.ifBlank { title }, style = TextStyle(fontFamily = StepUpSans, fontWeight = FontWeight.ExtraBold,
                        fontSize = 31.sp, lineHeight = 37.sp, letterSpacing = (-1).sp, color = LobbyInk.Text),
                    maxLines = 2, overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.semantics { heading() }.testTag("party-lobby-title"),
                )
                val time = roomTime(party.roomCreatedAt)
                Text(
                    if (time.isNotEmpty() && party.roomName.isNotBlank()) stringResource(R.string.crew5_lobby_sub, title, time) else title,
                    style = navyText(12.sp, LobbyInk.Sub, FontWeight.Medium, 1.65f), maxLines = 1, overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.padding(top = 7.dp).testTag("party-lobby-sub"),
                )
                Spacer(Modifier.height(12.dp))
                LobbyNotices(party, doneToday, crewToday, room, shoeFailed, courseFailed, onReconnect)
                if (loading) {
                    LobbyLoading()
                } else {
                    RosterFrame(
                        party, canNotify = canNotify && lobby,
                        onRoster = { sheet = LobbySheet.Roster },
                        onCall = onNotify,
                    )
                    // 간격은 버튼 아래 글자 한 줄(준비 취소 · 크루원 부르기)이 있어도 390 × 844 에서 내 신발 카드가 다 보이게(시안 10 · 12 · 23)
                    Spacer(Modifier.height(14.dp))
                    Overview(
                        party, crewToday, room,
                        onStamina = { sheet = LobbySheet.Stamina },
                        onGoal = { sheet = LobbySheet.Goal },
                        onCourse = { sheet = LobbySheet.Course },
                    )
                    Spacer(Modifier.height(12.dp))
                    ShoeFrame(room, shoes, onChange = { sheet = LobbySheet.Shoes }, enabled = lobby && room != null)
                    Spacer(Modifier.height(18.dp))
                }
            }
            LobbyFooter(
                party = party, loading = loading, lobby = lobby, doneToday = doneToday, canNotify = canNotify,
                onReady = {
                    // 인증 전이면 한 번 알린다(40 — 출발 · 기여 · 내 보상은 그대로, 인원 보너스에서만 빠진다)
                    if (room != null && !room.myVerified && room.rules.requiresPhone && !UnverifiedNotice.shown) sheet = LobbySheet.Unverified
                    else onReady()
                },
                onCancelReady = onCancelReady,
                // 출발 전 마지막 확인(33) · 준비된 사람만(34) — 서버가 명단 · 코스 판을 다시 본다
                onStart = { sheet = LobbySheet.Start },
                onNotify = onNotify,
            )
        }
    }
    // 시트 — 닫기 · 뒤로 · 바깥 탭은 부모의 선택 · 역할을 그대로 둔다
    when (sheet) {
        LobbySheet.None -> Unit
        LobbySheet.Roster -> RosterSheet(
            party, canKick = lobby && party.isHost, canNotify = canNotify && lobby,
            onKick = { kickTarget = it }, onCall = {
                sheet = LobbySheet.None
                onNotify()
            },
            onCourse = { sheet = LobbySheet.Course },
            onClose = { sheet = LobbySheet.None },
        )
        LobbySheet.Rules -> RulesSheet(rules, onBonus = { sheet = LobbySheet.Bonus }, onClose = { sheet = LobbySheet.None })
        LobbySheet.Bonus -> BonusSheet(rules, onClose = { sheet = LobbySheet.None })
        LobbySheet.Stamina -> StaminaSheet(room, crewToday, rules, onClose = { sheet = LobbySheet.None })
        LobbySheet.Goal -> GoalSheet(crewToday, onLevels = { sheet = LobbySheet.Levels }, onClose = { sheet = LobbySheet.None })
        LobbySheet.Levels -> LevelsSheet(crewToday, rules, onClose = { sheet = LobbySheet.None })
        // 신발 · 코스는 그 값이 있을 때만 열린다(버튼이 막혀 있다) — 그사이 사라졌으면 아래에서 닫는다
        LobbySheet.Shoes -> if (room != null) {
            ShoesSheet(
                shoes = shoes, picked = room.me?.sneakerId, crewLevel = room.crewLevel,
                onConfirm = { id ->
                    sheet = LobbySheet.None
                    if (id != room.me?.sneakerId) onPickShoe(id)
                },
                onClose = { sheet = LobbySheet.None },
            )
        }
        // 진행자는 코스 바꾸기(07) — 지금 코스 · 다른 코스 고르기. 참가자는 읽기 전용 보기(25)
        LobbySheet.Course -> party.course?.let { course ->
            if (party.isHost && lobby) {
                LobbyCourseSheet(
                    course, stringResource(R.string.crew5_course_change_title), stringResource(R.string.crew5_course_change_note),
                    primary = stringResource(R.string.crew5_course_change_pick),
                    onPrimary = {
                        sheet = LobbySheet.None
                        picking = true
                    },
                    onClose = { sheet = LobbySheet.None }, tag = "party-course-change",
                )
            } else {
                LobbyCourseSheet(
                    course, stringResource(R.string.crew5_course_view_title), null,
                    primary = stringResource(R.string.common_confirm), onPrimary = { sheet = LobbySheet.None },
                    onClose = { sheet = LobbySheet.None }, tag = "party-course-view",
                )
            }
        }
        LobbySheet.Start -> if (party.isHost && lobby && party.readyCount >= 2) {
            StartSheet(
                party, room, shoes,
                onGo = {
                    sheet = LobbySheet.None
                    onStart()
                },
                onWait = { sheet = LobbySheet.None },
            )
        }
        LobbySheet.Unverified -> UnverifiedSheet(
            onOk = {
                UnverifiedNotice.shown = true
                sheet = LobbySheet.None
                onReady()
            },
            onClose = { sheet = LobbySheet.None },
        )
        LobbySheet.Leave -> LeaveSheet(
            host = party.isHost,
            onStay = { sheet = LobbySheet.None },
            onLeave = {
                sheet = LobbySheet.None
                onBack()
            },
        )
    }
    // 열린 시트의 값이 사라졌다(방 상태가 바뀌었다) — 빈 시트를 두지 않고 닫는다
    val stale = (sheet == LobbySheet.Shoes && room == null) || (sheet == LobbySheet.Course && party.course == null) ||
        (sheet == LobbySheet.Start && !(party.isHost && lobby && party.readyCount >= 2))
    androidx.compose.runtime.LaunchedEffect(stale) { if (stale) sheet = LobbySheet.None }
    val kickGone = kickTarget?.let { id -> party.members.none { it.id == id } || !party.isHost || !lobby } == true
    androidx.compose.runtime.LaunchedEffect(kickGone) { if (kickGone) kickTarget = null }
    kickTarget?.let { id ->
        val who = party.members.firstOrNull { it.id == id }
        if (who != null && party.isHost && lobby) {
            KickSheet(
                who.name,
                onKick = {
                    kickTarget = null
                    onKick(id)
                },
                onCancel = { kickTarget = null },
            )
        }
    }
    // 참가자 — 진행자가 코스를 바꿨다(08). 확인하면 읽기 전용 코스(25)
    if (party.courseChanged && sheet == LobbySheet.None && !overlay) {
        val course = party.course
        if (course != null) {
            CourseUpdatedSheet(
                course.name,
                onCheck = {
                    onAcknowledgeCourse()
                    sheet = LobbySheet.Course
                },
                onClose = onAcknowledgeCourse,
            )
        }
    }
}

/** 대기실 위 시트 — 하나만 */
internal enum class LobbySheet { None, Roster, Rules, Bonus, Stamina, Goal, Levels, Shoes, Course, Leave, Start, Unverified }

/** 인증 전 안내(40)는 앱을 켠 동안 한 번만 — 출발을 막는 것이 아니다(보너스 인원에서만 빠진다) */
internal object UnverifiedNotice {
    var shown = false
}

/** 진행자 메뉴(⋯) */
internal enum class LobbyMenu { Roster, Course, Call, Rules, Leave }

/** 대기실 대신 보이는 한 화면 — 불러오기 실패(69) · 나옴(70) · 닫힘 · 찼음 · 출발함 · 출발을 놓침 · 로그인 */
internal enum class LobbyTerminal { FAILED, REMOVED, CLOSED, FULL, STARTED, MISSED, SIGN_IN }

/** 이 상태면 방 화면 대신 한 화면 — 방에 들어가기 전의 연결 실패 · 다시 들어갈 수 없는 까닭 */
internal fun lobbyTerminal(party: PartyState): LobbyTerminal? = when (party.problem) {
    PartyProblem.SIGN_IN -> LobbyTerminal.SIGN_IN
    PartyProblem.REMOVED -> LobbyTerminal.REMOVED
    PartyProblem.CLOSED -> LobbyTerminal.CLOSED
    PartyProblem.FULL -> LobbyTerminal.FULL
    PartyProblem.STARTED -> LobbyTerminal.STARTED
    PartyProblem.MISSED_START -> LobbyTerminal.MISSED.takeIf { party.pickedRoom != null && party.partyId == null }
    PartyProblem.NETWORK -> LobbyTerminal.FAILED.takeIf { party.partyId == null }
    PartyProblem.FAILED, null -> null
}

/** 한 화면(앱 바탕 — APP_EXISTING) — 오류를 참가자 0 · 체력 0 으로 덮지 않는다. 주 행동 하나 */
@Composable
private fun CrewLobbyTerminal(
    terminal: LobbyTerminal,
    onBack: () -> Unit,
    onRetry: () -> Unit,
    onReconnect: () -> Unit,
    onOtherRooms: () -> Unit,
) {
    val (titleRes, bodyRes) = when (terminal) {
        LobbyTerminal.FAILED -> R.string.crew5_failed_title to R.string.crew5_failed_body
        LobbyTerminal.REMOVED -> R.string.crew5_removed_title to R.string.crew5_removed_body
        LobbyTerminal.CLOSED -> R.string.crew5_closed_title to R.string.crew5_closed_body
        LobbyTerminal.FULL -> R.string.crew5_full_title to R.string.crew5_full_body
        LobbyTerminal.STARTED -> R.string.crew5_started_title to R.string.crew5_started_body
        LobbyTerminal.MISSED -> R.string.crew5_missed_title to R.string.crew5_missed_body
        LobbyTerminal.SIGN_IN -> R.string.crew5_sign_in_title to R.string.party_problem_sign_in
    }
    val tag = "party-terminal-" + terminal.name.lowercase()
    CrewPage(
        title = stringResource(R.string.crew5_lobby_header), onBack = onBack, badge = false, modifier = Modifier.testTag(tag),
        bottom = {
            when (terminal) {
                // 원래 방 번호로 다시 묻는다(닫힘 · 나옴과 가른다)
                LobbyTerminal.FAILED -> FreeRunButton(stringResource(R.string.crew5_rooms_reload), onReconnect, Modifier.fillMaxWidth().testTag("party-reconnect"))
                LobbyTerminal.SIGN_IN -> FreeRunButton(stringResource(R.string.crew_retry), onRetry, Modifier.fillMaxWidth().testTag("party-retry"))
                else -> FreeRunButton(stringResource(R.string.crew5_other_rooms), onOtherRooms, Modifier.fillMaxWidth().testTag("party-other-rooms"))
            }
        },
    ) {
        CrewCenterState(
            if (terminal == LobbyTerminal.FAILED) Icons.Outlined.CloudOff else Icons.Outlined.PersonOff,
            stringResource(titleRes),
            if (terminal == LobbyTerminal.FULL) stringResource(bodyRes, CrewRunMath.ROOM_MAX) else stringResource(bodyRes),
            modifier = Modifier.padding(top = 40.dp).testTag("party-problem"),
        )
    }
}

/** 대기실 머리 — 뒤로 · "크루 대기실" · 진행자는 ⋯ 메뉴, 참가자는 ⓘ 규칙 */
@Composable
private fun LobbyHeader(
    onBack: () -> Unit,
    host: Boolean,
    onRules: () -> Unit,
    onMenu: (LobbyMenu) -> Unit,
    canNotify: Boolean,
    hasCourse: Boolean,
) {
    Row(Modifier.fillMaxWidth().height(56.dp).padding(horizontal = 4.dp), verticalAlignment = Alignment.CenterVertically) {
        com.stepup.android.ui.components.RunBackButton(onBack, tag = "kit-back", tint = LobbyInk.Text)
        Text(
            stringResource(R.string.crew5_lobby_header), style = navyText(18.sp, LobbyInk.Text, FontWeight.Bold, 1.33f),
            textAlign = TextAlign.Center, modifier = Modifier.weight(1f).semantics { heading() }.testTag("crew-header-title"),
        )
        if (host) {
            var open by rememberSaveable { mutableStateOf(false) }
            Box {
                val label = stringResource(R.string.crew5_lobby_menu)
                Box(
                    Modifier.size(48.dp).clip(CircleShape).feedbackClickable(onClick = { open = true })
                        .semantics { contentDescription = label }.testTag("party-menu"),
                    contentAlignment = Alignment.Center,
                ) { Icon(Icons.Filled.MoreHoriz, contentDescription = null, tint = LobbyInk.Text) }
                DropdownMenu(expanded = open, onDismissRequest = { open = false }, containerColor = LobbyInk.Sheet) {
                    @Composable
                    fun item(res: Int, which: LobbyMenu, tag: String) = DropdownMenuItem(
                        text = { Text(stringResource(res), style = navyText(15.sp, LobbyInk.Text, FontWeight.Medium)) },
                        onClick = {
                            open = false
                            onMenu(which)
                        },
                        modifier = Modifier.heightIn(min = 48.dp).testTag(tag),
                    )
                    item(R.string.crew5_lobby_menu_roster, LobbyMenu.Roster, "party-menu-roster")
                    if (hasCourse) item(R.string.crew5_lobby_menu_course, LobbyMenu.Course, "party-menu-course")
                    if (canNotify) item(R.string.crew5_lobby_call, LobbyMenu.Call, "party-menu-call")
                    item(R.string.crew5_lobby_menu_rules, LobbyMenu.Rules, "party-rule-open")
                    item(R.string.crew5_lobby_menu_leave, LobbyMenu.Leave, "party-menu-leave")
                }
            }
        } else {
            val label = stringResource(R.string.crew5_lobby_rules)
            Box(
                Modifier.size(48.dp).clip(CircleShape).feedbackClickable(onClick = onRules)
                    .semantics { contentDescription = label }.testTag("party-rule-open"),
                contentAlignment = Alignment.Center,
            ) { Icon(Icons.Outlined.Info, contentDescription = null, tint = LobbyInk.Text) }
        }
    }
}

/** 방 위 짧은 안내 — 연결 끊김(지금 다시 연결) · 누른 것 실패 · 오늘 이미 함 · 인증 전 · 신발 · 코스 실패 */
@Composable
private fun LobbyNotices(
    party: PartyState,
    doneToday: Boolean,
    crewToday: CrewRunToday?,
    room: CrewRunRoom?,
    shoeFailed: Boolean,
    courseFailed: Boolean,
    onReconnect: () -> Unit,
) {
    val notes = buildList<@Composable () -> Unit> {
        if (party.problem == PartyProblem.NETWORK) add {
            Column(Modifier.testTag("party-problem")) {
                LobbyNotice(stringResource(R.string.crew5_lobby_offline), warn = true)
                Text(
                    stringResource(R.string.crew5_lobby_reconnect) + "  ›", style = navyText(14.sp, LobbyInk.Ice, FontWeight.Bold),
                    modifier = Modifier.heightIn(min = 48.dp).clip(RoundedCornerShape(8.dp)).feedbackClickable(onClick = onReconnect)
                        .padding(vertical = 13.dp, horizontal = 4.dp).testTag("party-reconnect"),
                )
            }
        }
        if (party.problem == PartyProblem.FAILED) add { LobbyNotice(stringResource(R.string.crew5_lobby_failed), Modifier.testTag("party-problem"), warn = true) }
        if (doneToday) add {
            LobbyNotice(
                if (crewToday?.usedElsewhere == true) stringResource(R.string.crew_lobby_done_elsewhere, crewToday.mine.crewName.orEmpty())
                else stringResource(R.string.crew5_lobby_done_today),
                Modifier.testTag("party-done-today"), warn = true,
            )
        }
        if (shoeFailed) add { LobbyNotice(stringResource(R.string.crew5_shoes_failed), Modifier.testTag("party-shoe-failed"), warn = true) }
        if (courseFailed) add { LobbyNotice(stringResource(R.string.crew5_course_change_failed), Modifier.testTag("party-course-failed"), warn = true) }
        // 인증 전 — 인원 보너스 집계에서만 빠진다(출발 · 기여 · 내 보상은 그대로 — 40). 막지 않는다
        if (room != null && !room.myVerified && room.rules.requiresPhone && party.phase == PartyPhase.LOBBY) add {
            LobbyNotice(stringResource(R.string.crew_run_unverified), Modifier.testTag("party-unverified"))
        }
    }
    notes.forEach {
        it()
        Spacer(Modifier.height(10.dp))
    }
}

/** 불러오는 중(68) — 명단 · 코스 자리만(출발 버튼은 막는다) */
@Composable
private fun LobbyLoading() {
    Column(Modifier.fillMaxWidth().testTag("party-loading")) {
        DualRailFrame(Modifier.fillMaxWidth().height(250.dp)) {
            Column(Modifier.fillMaxWidth()) {
                LobbySkeleton(Modifier.width(120.dp).height(19.dp))
                Spacer(Modifier.height(22.dp))
                repeat(2) {
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
                        repeat(3) { LobbySkeleton(Modifier.size(52.dp), round = true) }
                    }
                    Spacer(Modifier.height(30.dp))
                }
            }
        }
        Spacer(Modifier.height(20.dp))
        Row(Modifier.fillMaxWidth().height(154.dp), horizontalArrangement = Arrangement.spacedBy(20.dp)) {
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(14.dp)) {
                LobbySkeleton(Modifier.fillMaxWidth().height(19.dp))
                LobbySkeleton(Modifier.fillMaxWidth(0.7f).height(30.dp))
                LobbySkeleton(Modifier.fillMaxWidth().height(19.dp))
                LobbySkeleton(Modifier.fillMaxWidth(0.6f).height(30.dp))
            }
            LobbySkeleton(Modifier.weight(1.3f).fillMaxHeight())
        }
        Text(
            stringResource(R.string.crew5_lobby_loading), style = navyText(13.sp, LobbyInk.Sub, FontWeight.Medium),
            modifier = Modifier.padding(top = 14.dp).semantics { liveRegion = LiveRegionMode.Polite },
        )
    }
}

@Composable
private fun LobbySkeleton(modifier: Modifier, round: Boolean = false) {
    Box(modifier.clip(if (round) CircleShape else RoundedCornerShape(5.dp)).background(Color(0xA631546A)))
}

// ── 함께 달릴 크루원 ────────────────────────────────────────────────

/** 함께 달릴 크루원(듀얼 레일) — "N/M 준비" 칩 · 3 × 2(진행자 · 나 · 나머지). 더 많으면 다섯 + "+N", 적으면 빈 자리 */
@Composable
private fun RosterFrame(party: PartyState, canNotify: Boolean, onRoster: () -> Unit, onCall: () -> Unit) {
    val ordered = crewOrder(party.members)
    val many = ordered.size > CREW_GRID_MAX
    val shown = if (many) ordered.take(CREW_GRID_MAX - 1) else ordered
    // 혼자면 빈 자리와 아래 "크루원 부르기"(12)
    val alone = party.partySize <= 1
    DualRailFrame(
        Modifier.fillMaxWidth().testTag(if (alone) "party-alone" else "party-members"),
        padding = PaddingValues(start = 18.dp, end = 18.dp, top = 16.dp, bottom = 16.dp),
    ) {
        Column(Modifier.fillMaxWidth()) {
            Row(
                Modifier.fillMaxWidth().heightIn(min = 25.dp).clip(RoundedCornerShape(8.dp))
                    .feedbackClickable(role = Role.Button, onClick = onRoster)
                    .semantics(mergeDescendants = true) { liveRegion = LiveRegionMode.Polite }.testTag("party-counts"),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(stringResource(R.string.crew5_lobby_members), style = navyText(13.sp, LobbyInk.Text, FontWeight.SemiBold), modifier = Modifier.weight(1f))
                LobbyChip(stringResource(R.string.crew5_lobby_ready_chip, party.readyCount, party.partySize), Modifier.testTag("party-ready-count"))
            }
            Spacer(Modifier.height(12.dp))
            val cells: List<RosterCell> = buildList {
                shown.forEach { add(RosterCell.Person(it)) }
                if (many) add(RosterCell.More(ordered.size - shown.size))
                while (size < CREW_GRID_MAX) add(RosterCell.Empty)
            }
            Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(11.dp)) {
                cells.chunked(3).forEachIndexed { row, three ->
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        three.forEach { cell ->
                            Box(Modifier.weight(1f)) {
                                when (cell) {
                                    is RosterCell.Person -> PersonCell(cell.member, firstRow = row == 0)
                                    is RosterCell.More -> EmptyCell(stringResource(R.string.crew5_lobby_more, cell.rest), stringResource(R.string.crew5_lobby_all), onRoster, "party-more")
                                    RosterCell.Empty -> EmptyCell("+", stringResource(R.string.crew5_lobby_invite), if (canNotify) onCall else null, "party-empty")
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

private sealed interface RosterCell {
    data class Person(val member: PartyMember) : RosterCell
    data class More(val rest: Int) : RosterCell
    data object Empty : RosterCell
}

/** 크루원 한 칸 — 52dp 원(진행자는 파란 면) · 준비면 오른쪽 아래 확인 표시 · 이름(나) · 진행자 */
@Composable
private fun PersonCell(member: PartyMember, firstRow: Boolean) {
    val me = stringResource(R.string.crew5_lobby_me)
    val host = stringResource(R.string.run_cr_host)
    val ready = stringResource(R.string.crew5_roster_ready)
    Column(
        Modifier.fillMaxWidth().semantics(mergeDescendants = true) {
            contentDescription = listOfNotNull(member.name, me.takeIf { member.isMe }, host.takeIf { member.isHost }, ready.takeIf { member.ready })
                .joinToString(", ")
        }.testTag("party-member-${member.id}"),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Box(Modifier.size(52.dp)) {
            Box(
                Modifier.size(52.dp).clip(CircleShape)
                    .background(if (member.isHost) Brush.linearGradient(listOf(LobbyInk.HostTop, LobbyInk.HostBottom)) else Brush.linearGradient(listOf(LobbyInk.Avatar, LobbyInk.Avatar)))
                    .border(1.5.dp, LobbyInk.AvatarRing, CircleShape),
                contentAlignment = Alignment.Center,
            ) {
                Text(initialsOf(member.name).take(1), style = navyText(22.sp, Color(0xFFF3F8FF), FontWeight.Bold, 1.1f))
            }
            if (member.ready) {
                Box(
                    Modifier.align(Alignment.BottomEnd).offset(x = 7.dp, y = 6.dp).size(27.dp).clip(CircleShape).background(LobbyInk.CheckRing)
                        .padding(3.dp).clip(CircleShape).background(LobbyInk.CheckFace).testTag("party-ready-mark"),
                    contentAlignment = Alignment.Center,
                ) { Icon(Icons.Filled.Check, contentDescription = null, tint = LobbyInk.CheckInk, modifier = Modifier.size(13.dp)) }
            }
        }
        Spacer(Modifier.height(8.dp))
        Column(Modifier.heightIn(min = if (firstRow) 36.dp else 24.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                if (member.isMe) me else member.name, style = navyText(14.sp, LobbyInk.Text, FontWeight.SemiBold, 1.43f),
                maxLines = 1, overflow = TextOverflow.Ellipsis, textAlign = TextAlign.Center,
            )
            if (member.isHost) Text(host, style = navyText(11.sp, Color(0xFFCEE5F6), FontWeight.Normal, 1.2f))
        }
    }
}

/** 빈 자리(함께 달려요 — 누르면 크루원 부르기) · 더 보기(+N 전체 보기) */
@Composable
private fun EmptyCell(mark: String, label: String, onClick: (() -> Unit)?, tag: String) {
    Column(
        Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp))
            .then(if (onClick != null) Modifier.feedbackClickable(role = Role.Button, onClick = onClick) else Modifier)
            .semantics(mergeDescendants = true) {}.testTag(tag),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Box(
            Modifier.size(52.dp).clip(CircleShape).background(LobbyInk.AvatarEmpty).border(1.5.dp, LobbyInk.AvatarEmptyRing, CircleShape),
            contentAlignment = Alignment.Center,
        ) { Text(mark, style = navyText(if (mark == "+") 22.sp else 16.sp, LobbyInk.AvatarEmptyInk, FontWeight.Bold, 1.1f)) }
        Spacer(Modifier.height(8.dp))
        Text(label, style = navyText(14.sp, LobbyInk.Text, FontWeight.SemiBold, 1.43f), maxLines = 1, overflow = TextOverflow.Ellipsis,
            modifier = Modifier.heightIn(min = 24.dp))
    }
}

// ── 크루 내구도 · 목표 · 코스 ───────────────────────────────────────

/** 왼쪽 크루 내구도 · 크루 목표(누르면 시트), 오른쪽 크루 코스 지도(누르면 코스 시트). 높이 154dp 를 기준으로 글자가 크면 늘어난다 */
@Composable
private fun Overview(
    party: PartyState,
    crewToday: CrewRunToday?,
    room: CrewRunRoom?,
    onStamina: () -> Unit,
    onGoal: () -> Unit,
    onCourse: () -> Unit,
) {
    // 넘기는 화면 안이라 높이가 열려 있다 — 왼쪽은 글자 크기만큼(최소 154dp), 오른쪽 지도 칸은 154dp 로 정한다
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(20.dp)) {
        Column(Modifier.weight(1f).heightIn(min = 154.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            val pool = room?.pool ?: crewToday?.pool?.takeIf { crewToday.isToday() }
            Metric(
                Icons.Filled.Bolt, stringResource(R.string.crew5_lobby_pool),
                value = pool?.let { poolNumber(it.remaining) } ?: "—",
                small = pool?.let { stringResource(R.string.crew5_lobby_pool_total, poolNumber(it.total)) }.orEmpty(),
                fraction = pool?.let { CrewRunMath.fraction(it.remaining, it.total) }, level = false,
                onClick = onStamina, tag = "party-pool",
            )
            val today = crewToday?.takeIf { it.isToday() }
            val next = today?.crewLevel?.let { it + 1 }?.takeIf { it <= CrewRunMath.LEVEL_MAX && today.nextLevelKm != null }
            Metric(
                Icons.Outlined.Flag, stringResource(R.string.crew5_lobby_goal),
                value = when {
                    today == null -> "—"
                    next == null -> "Lv.${today.crewLevel ?: 1}"
                    else -> "${courseKm(CrewRunMath.kmToNext(today.runKm, today.nextLevelKm) ?: 0.0)} km"
                },
                small = when {
                    today == null -> ""
                    next == null -> stringResource(R.string.crew5_lobby_goal_max)
                    else -> stringResource(R.string.crew5_lobby_goal_next, next)
                },
                fraction = today?.let { CrewRunMath.levelProgress(it.runKm, it.crewLevel, it.nextLevelKm, it.rules.levels) }, level = true,
                onClick = onGoal, tag = "party-goal",
            )
        }
        Column(Modifier.weight(1.3f).height(154.dp)) {
            val expand = stringResource(R.string.crew5_lobby_course_expand)
            Row(
                Modifier.fillMaxWidth().heightIn(min = 24.dp).clip(RoundedCornerShape(8.dp))
                    .then(if (party.course != null) Modifier.feedbackClickable(role = Role.Button, onClick = onCourse) else Modifier)
                    .semantics(mergeDescendants = true) { if (party.course != null) contentDescription = expand }.testTag("party-course"),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(stringResource(R.string.crew5_lobby_course), style = navyText(12.sp, LobbyInk.Label, FontWeight.Medium, 1.5f), modifier = Modifier.weight(1f))
                if (party.course != null) Icon(Icons.Outlined.OpenInFull, contentDescription = null, tint = LobbyInk.Sub, modifier = Modifier.size(15.dp))
            }
            Spacer(Modifier.height(10.dp))
            val course = party.course
            if (course != null) {
                Box(
                    Modifier.fillMaxWidth().weight(1f).clip(RoundedCornerShape(15.dp))
                        .feedbackClickable(role = Role.Button, onClick = onCourse).testTag("party-course-map"),
                ) {
                    CrewCourseMap(course.points, Modifier.fillMaxSize())
                }
            } else {
                Box(
                    Modifier.fillMaxWidth().weight(1f).clip(RoundedCornerShape(15.dp)).background(LobbyInk.Map),
                    contentAlignment = Alignment.Center,
                ) { Text(stringResource(R.string.crew5_lobby_course_none), style = navyText(12.sp, LobbyInk.Sub, FontWeight.Medium)) }
            }
        }
    }
}

/** 수치 하나 — 아이콘 · 이름 · 큰 수(+ 작은 글) · 막대. 누르면 자세히(시트) */
@Composable
private fun Metric(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    value: String,
    small: String,
    fraction: Float?,
    level: Boolean,
    onClick: () -> Unit,
    tag: String,
) {
    Column(
        Modifier.fillMaxWidth().clip(RoundedCornerShape(8.dp)).feedbackClickable(role = Role.Button, onClick = onClick)
            .semantics(mergeDescendants = true) { contentDescription = "$label $value $small" }.testTag(tag),
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(icon, contentDescription = null, tint = LobbyInk.Info, modifier = Modifier.size(13.dp))
            Spacer(Modifier.width(5.dp))
            Text(label, style = navyText(12.sp, LobbyInk.Label, FontWeight.Medium, 1.5f), maxLines = 1)
        }
        Row(verticalAlignment = Alignment.Bottom) {
            Text(
                value, style = navyText(26.sp, LobbyInk.Text, FontWeight.ExtraBold, 1.15f).copy(letterSpacing = (-0.5).sp, fontFeatureSettings = "tnum"),
                maxLines = 1, modifier = Modifier.testTag("$tag-value"),
            )
            Spacer(Modifier.width(4.dp))
            Text(small, style = navyText(11.sp, Color(0xFFAFC7DF), FontWeight.Normal), maxLines = 1, overflow = TextOverflow.Ellipsis,
                modifier = Modifier.padding(bottom = 4.dp))
        }
        if (fraction != null) LobbyTrack(fraction, Modifier.padding(top = 1.dp), level = level)
    }
}

/** 공용 체력 숫자 — 소수 한 자리(0 이면 뗀다) · 천 단위 쉼표. 남은 양을 올려 보이지 않게 버린다 */
internal fun poolNumber(value: Double): String {
    val plain = CrewRunMath.amount(value)
    val whole = plain.substringBefore('.')
    val grouped = whole.reversed().chunked(3).joinToString(",").reversed()
    return if ('.' in plain) "$grouped.${plain.substringAfter('.')}" else grouped
}

// ── 내 신발 ────────────────────────────────────────────────────────

/** 내 신발(듀얼 레일, 크루원 카드와 같은 틀) — 그림 · 이름 · "신발 변경". 고른 신발(없으면 신은 신발) */
@Composable
private fun ShoeFrame(room: CrewRunRoom?, shoes: List<Sneaker>, onChange: () -> Unit, enabled: Boolean) {
    val me = room?.me
    val local = me?.sneakerId?.let { id -> shoes.firstOrNull { it.id == id } } ?: shoes.firstOrNull { it.equipped }
    DualRailFrame(
        Modifier.fillMaxWidth().heightIn(min = 96.dp).testTag("party-shoe"),
        padding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
    ) {
        Row(Modifier.fillMaxWidth().heightIn(min = 72.dp), verticalAlignment = Alignment.CenterVertically) {
            if (local != null) {
                SneakerVisual(local, Modifier.width(88.dp).height(74.dp))
                Spacer(Modifier.width(12.dp))
            }
            Column(Modifier.weight(1f)) {
                Text(stringResource(R.string.crew5_lobby_shoe), style = navyText(12.sp, Color(0xFFD2E9F8), FontWeight.Medium))
                Spacer(Modifier.height(5.dp))
                Text(
                    local?.shoeName() ?: stringResource(R.string.crew5_lobby_shoe_none),
                    style = navyText(15.sp, LobbyInk.Text, FontWeight.Bold, 1.33f), maxLines = 2, overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.testTag("party-shoe-name"),
                )
            }
            Spacer(Modifier.width(8.dp))
            ShoeChangeButton(stringResource(R.string.crew5_lobby_shoe_change), onChange, Modifier.testTag("party-shoe-change"), enabled = enabled)
        }
    }
}

// ── 아래 버튼 ──────────────────────────────────────────────────────

/**
 * 주 버튼 하나(흰 면, 60dp) — 진행자: "N명과 출발"(준비 2명부터) · 혼자면 꺼지고 아래 "크루원 부르기", 아직 준비 전이면 "준비 완료".
 * 참가자: "준비 완료" → 준비하면 꺼진 "진행자가 출발하면 함께 시작해요" + "준비 취소". 불러오는 중 · 오늘 이미 했으면 막힌다.
 */
@Composable
private fun LobbyFooter(
    party: PartyState,
    loading: Boolean,
    lobby: Boolean,
    doneToday: Boolean,
    canNotify: Boolean,
    onReady: () -> Unit,
    onCancelReady: () -> Unit,
    onStart: () -> Unit,
    onNotify: () -> Unit,
) {
    Column(
        Modifier.fillMaxWidth().testTag("party-footer").padding(horizontal = LobbyInk.Gutter).padding(top = 8.dp, bottom = 6.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        when {
            loading -> LobbyPrimaryButton(stringResource(R.string.crew5_lobby_ready), {}, Modifier.testTag("party-ready-loading"), enabled = false)
            !lobby -> LobbyPrimaryButton(stringResource(R.string.crew5_starting), {}, Modifier.testTag("party-starting"), enabled = false)
            !party.myReady -> LobbyPrimaryButton(
                stringResource(R.string.crew5_lobby_ready), onReady, Modifier.testTag("party-ready"), enabled = !doneToday,
            )
            party.isHost -> {
                val needTwo = party.readyCount < 2
                // 크루 러닝은 준비한 크루원 2명부터(서버 0071 crew_run_need_two · 핸드오프 12 단독 출발 막음) — 꺼진 버튼이 까닭을 말한다
                LobbyPrimaryButton(
                    if (needTwo) stringResource(R.string.crew5_lobby_need_two_button) else stringResource(R.string.crew5_lobby_start, party.readyCount),
                    onStart, Modifier.testTag("party-start"), enabled = !needTwo,
                )
                if (needTwo) {
                    if (canNotify) {
                        LobbyTextButton(stringResource(R.string.crew5_lobby_call), onNotify, "party-need-two")
                    } else {
                        Text(stringResource(R.string.crew_lobby_need_two), style = navyText(12.sp, LobbyInk.Sub, FontWeight.Medium),
                            textAlign = TextAlign.Center, modifier = Modifier.padding(top = 8.dp).testTag("party-need-two"))
                    }
                } else {
                    LobbyTextButton(stringResource(R.string.crew5_lobby_cancel_ready), onCancelReady, "party-cancel-ready")
                }
            }
            else -> {
                LobbyPrimaryButton(stringResource(R.string.crew5_lobby_waiting_host), {}, Modifier.testTag("party-waiting"), enabled = false)
                LobbyTextButton(stringResource(R.string.crew5_lobby_cancel_ready), onCancelReady, "party-cancel-ready")
            }
        }
    }
}

/** 버튼 아래 작은 글자 행동(크루원 부르기 · 준비 취소 · 시트의 취소 · 나가기) — 주 버튼 아래 가운데, 누르는 자리 44dp */
@Composable
private fun LobbyTextButton(label: String, onClick: () -> Unit, tag: String) {
    Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
        Box(
            Modifier.heightIn(min = 44.dp).clip(RoundedCornerShape(10.dp))
                .feedbackClickable(role = Role.Button, onClick = onClick).padding(horizontal = 16.dp).testTag(tag),
            contentAlignment = Alignment.Center,
        ) { Text(label, style = navyText(13.sp, Color(0xFFC7E7FA), FontWeight.SemiBold)) }
    }
}

// ── 시트 ───────────────────────────────────────────────────────────

/**
 * 대기실 위 시트 — 불투명한 대기실 면(#08243A, 위 모서리 24dp) · 손잡이 · 제목 · 닫기. 부모(터널)는 어둡게 멈춘다.
 * 바깥 탭 · 뒤로 · 닫기는 부모의 선택 · 역할을 바꾸지 않는다. [dismissible] 이 아니면(서버가 끝낸 것 등) 닫아도 취소되지 않는다.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun LobbySheetFrame(
    title: String,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
    dismissible: Boolean = true,
    content: @Composable ColumnScope.() -> Unit,
) {
    val state = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    ModalBottomSheet(
        onDismissRequest = { if (dismissible) onDismiss() },
        sheetState = state,
        containerColor = Color.Transparent,
        scrimColor = LobbyInk.Scrim,
        dragHandle = null,
        shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp),
    ) {
        val shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp)
        Column(
            modifier.fillMaxWidth().clip(shape).background(LobbyInk.Sheet, shape).border(1.dp, LobbyInk.SheetEdge, shape)
                .navigationBarsPadding().imePadding().padding(horizontal = LobbyInk.Gutter).padding(top = 10.dp, bottom = 22.dp),
        ) {
            Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                Box(Modifier.width(34.dp).height(4.dp).clip(RoundedCornerShape(3.dp)).background(Color(0xFF517C95)))
            }
            Spacer(Modifier.height(12.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    title, style = navyText(23.sp, LobbyInk.Text, FontWeight.ExtraBold, 1.3f).copy(letterSpacing = (-0.5).sp),
                    modifier = Modifier.weight(1f).semantics { heading() }.testTag("lobby-sheet-title"),
                )
                if (dismissible) {
                    val close = stringResource(R.string.common_close)
                    Box(
                        Modifier.size(48.dp).clip(CircleShape).feedbackClickable(cue = FeedbackCue.Back, onClick = onDismiss)
                            .semantics { contentDescription = close }.testTag("lobby-sheet-close"),
                        contentAlignment = Alignment.Center,
                    ) { Icon(Icons.Filled.Close, contentDescription = null, tint = LobbyInk.Text) }
                }
            }
            Spacer(Modifier.height(14.dp))
            Column(Modifier.fillMaxWidth().weight(1f, fill = false).verticalScroll(rememberScrollState()), content = content)
        }
    }
}

/** 시트 안 이름 · 값 한 줄 */
@Composable
private fun SheetRow(label: String, value: String, sub: String? = null, highlight: Boolean = false, tag: String? = null) {
    Column(Modifier.fillMaxWidth().then(if (tag != null) Modifier.testTag(tag) else Modifier)) {
        Row(Modifier.fillMaxWidth().heightIn(min = 47.dp).padding(vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text(label, style = navyText(14.sp, Color(0xFFC6DBEC), FontWeight.Medium))
                if (sub != null) Text(sub, style = navyText(12.sp, Color(0xFFAFC7DF), FontWeight.Normal), modifier = Modifier.padding(top = 3.dp))
            }
            Spacer(Modifier.width(12.dp))
            Text(value, style = navyText(14.sp, if (highlight) LobbyInk.Ice else LobbyInk.Text, FontWeight.Bold), textAlign = TextAlign.End)
        }
        Box(Modifier.fillMaxWidth().height(1.dp).background(LobbyInk.RowLine))
    }
}

/** 전체 명단 — 진행자는 참가자 관리(14 · 내보내기 · 코스 바꾸기 · 부르기), 참가자는 읽기 전용(31). 20명 모두 넘겨 본다 */
@Composable
private fun RosterSheet(
    party: PartyState,
    canKick: Boolean,
    canNotify: Boolean,
    onKick: (String) -> Unit,
    onCall: () -> Unit,
    onCourse: () -> Unit,
    onClose: () -> Unit,
) {
    LobbySheetFrame(
        stringResource(if (canKick) R.string.crew5_roster_host_title else R.string.crew5_roster_member_title), onClose,
        Modifier.testTag("party-roster-sheet"),
    ) {
        Text(stringResource(R.string.crew5_roster_count, party.partySize, party.readyCount), style = navyText(13.sp, LobbyInk.Sub, FontWeight.Medium))
        Spacer(Modifier.height(8.dp))
        Column(Modifier.fillMaxWidth().testTag("party-roster-list")) {
            crewOrder(party.members).forEachIndexed { i, member ->
                if (i > 0) Box(Modifier.fillMaxWidth().height(1.dp).background(LobbyInk.RowLine))
                Row(
                    Modifier.fillMaxWidth().heightIn(min = 57.dp).semantics(mergeDescendants = true) {}.testTag("party-roster-${member.id}"),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Box(
                        Modifier.size(34.dp).clip(CircleShape)
                            .background(if (member.isHost) Brush.linearGradient(listOf(LobbyInk.HostTop, LobbyInk.HostBottom)) else Brush.linearGradient(listOf(LobbyInk.Avatar, LobbyInk.Avatar)))
                            .border(1.5.dp, LobbyInk.AvatarRing, CircleShape),
                        contentAlignment = Alignment.Center,
                    ) { Text(initialsOf(member.name).take(1), style = navyText(15.sp, Color(0xFFF3F8FF), FontWeight.Bold)) }
                    Spacer(Modifier.width(11.dp))
                    Column(Modifier.weight(1f)) {
                        Text(
                            member.name + if (member.isMe) " (${stringResource(R.string.crew5_lobby_me)})" else "",
                            style = navyText(14.sp, LobbyInk.Text, FontWeight.SemiBold), maxLines = 1, overflow = TextOverflow.Ellipsis,
                        )
                        Text(
                            listOfNotNull(
                                stringResource(R.string.run_cr_host).takeIf { member.isHost },
                                stringResource(if (member.ready) R.string.crew5_roster_ready else R.string.crew5_roster_waiting),
                            ).joinToString(" · "),
                            style = navyText(11.sp, LobbyInk.Sub, FontWeight.Normal), modifier = Modifier.padding(top = 3.dp),
                        )
                    }
                    // 진행자만 · 나 말고 · 출발 전에만 — 실제 user_id 로 내보낸다(확인 뒤)
                    if (canKick && !member.isMe) {
                        val label = stringResource(R.string.run_cr_kick)
                        Box(
                            Modifier.size(48.dp).clip(CircleShape).feedbackClickable(onClick = { onKick(member.id) })
                                .semantics { contentDescription = "$label ${member.name}" }.testTag("party-kick-${member.id}"),
                            contentAlignment = Alignment.Center,
                        ) { Icon(Icons.Filled.Close, contentDescription = null, tint = Color(0xFFA5CAE1), modifier = Modifier.size(18.dp)) }
                    }
                }
            }
        }
        if (canKick) {
            Spacer(Modifier.height(12.dp))
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                if (party.course != null) LobbyPill(stringResource(R.string.crew5_lobby_menu_course), onCourse, Modifier.weight(1f), "party-roster-course")
                if (canNotify) LobbyPill(stringResource(R.string.crew5_lobby_call), onCall, Modifier.weight(1f), "party-roster-call")
            }
        }
        Spacer(Modifier.height(16.dp))
        LobbyPrimaryButton(stringResource(R.string.common_confirm), onClose, Modifier.testTag("party-roster-close"))
    }
}

/** 시트 안 작은 알약 버튼(코스 바꾸기 · 크루원 부르기) — 44dp */
@Composable
private fun LobbyPill(label: String, onClick: () -> Unit, modifier: Modifier, tag: String) {
    Box(
        modifier.heightIn(min = 44.dp).clip(RoundedCornerShape(9.dp)).background(LobbyInk.ChipSoftFace)
            .feedbackClickable(role = Role.Button, onClick = onClick).padding(horizontal = 12.dp).testTag(tag),
        contentAlignment = Alignment.Center,
    ) { Text(label, style = navyText(13.sp, Color(0xFFD5F3FF), FontWeight.SemiBold), textAlign = TextAlign.Center) }
}

/** 내보내기 확인(15) — 대기실에서만 빠진다(크루 탈퇴 아님). 서버가 받아야 명단에서 빠진다 */
@Composable
private fun KickSheet(name: String, onKick: () -> Unit, onCancel: () -> Unit) {
    LobbySheetFrame(stringResource(R.string.crew5_kick_title, name), onCancel, Modifier.testTag("party-kick-sheet")) {
        Text(stringResource(R.string.crew5_kick_body), style = navyText(14.sp, LobbyInk.NoteInk, FontWeight.Medium, 1.6f))
        Spacer(Modifier.height(20.dp))
        LobbyPrimaryButton(stringResource(R.string.crew5_kick_go), onKick, Modifier.testTag("party-kick-confirm"))
        LobbyTextButton(stringResource(R.string.common_cancel), onCancel, "party-kick-cancel")
    }
}

/** 나가기 확인(21 진행자 · 32 참가자) — 출발 전 나가기는 하루 한 번을 쓰지 않는다 */
@Composable
private fun LeaveSheet(host: Boolean, onStay: () -> Unit, onLeave: () -> Unit) {
    LobbySheetFrame(stringResource(R.string.crew5_leave_title), onStay, Modifier.testTag(if (host) "party-leave-host" else "party-leave-member")) {
        Text(
            stringResource(if (host) R.string.crew5_leave_host_body else R.string.crew5_leave_member_body),
            style = navyText(14.sp, LobbyInk.NoteInk, FontWeight.Medium, 1.6f),
        )
        Spacer(Modifier.height(20.dp))
        LobbyPrimaryButton(stringResource(R.string.crew5_leave_stay), onStay, Modifier.testTag("party-leave-stay"))
        LobbyTextButton(stringResource(R.string.crew5_leave_go), onLeave, "party-leave")
    }
}

/** 크루 러닝 규칙(30) — 다섯 주제 모두(서버 규칙 값) · 인원 보너스 표로 */
@Composable
private fun RulesSheet(rules: CrewRunRules, onBonus: () -> Unit, onClose: () -> Unit) {
    LobbySheetFrame(stringResource(R.string.crew_run_sheet_title), onClose, Modifier.testTag("party-rule-sheet")) {
        crewRunRuleLines(rules).forEach { (title, body) ->
            Text(title, style = navyText(15.sp, LobbyInk.Text, FontWeight.Bold, 1.4f))
            if (body.isNotEmpty()) Text(body, style = navyText(14.sp, Color(0xFFC4DDEE), FontWeight.Normal, 1.7f), modifier = Modifier.padding(top = 4.dp))
            Spacer(Modifier.height(14.dp))
        }
        LobbyPill(stringResource(R.string.crew5_bonus_title), onBonus, Modifier.fillMaxWidth(), "party-rule-bonus")
        Spacer(Modifier.height(14.dp))
        LobbyPrimaryButton(stringResource(R.string.common_confirm), onClose, Modifier.testTag("party-rule-close"))
    }
}

/** 인원 보너스 표(29) — 2 ~ 20명 모두(구간 하나만). 0 ~ 1명은 없음, 레벨은 따로 더한다 */
@Composable
private fun BonusSheet(rules: CrewRunRules, onClose: () -> Unit) {
    LobbySheetFrame(stringResource(R.string.crew5_bonus_title), onClose, Modifier.testTag("party-bonus-sheet")) {
        Text(stringResource(R.string.crew5_bonus_note, rules.confirmSec / 60, rules.nearM), style = navyText(13.sp, LobbyInk.NoteInk, FontWeight.Medium, 1.6f))
        Spacer(Modifier.height(10.dp))
        Row(Modifier.fillMaxWidth().height(36.dp), verticalAlignment = Alignment.CenterVertically) {
            Text(stringResource(R.string.crew5_bonus_people), style = navyText(12.sp, Color(0xFFB2D1E8), FontWeight.Medium), modifier = Modifier.weight(1f))
            Text(stringResource(R.string.crew5_bonus_value), style = navyText(12.sp, Color(0xFFB2D1E8), FontWeight.Medium))
        }
        rules.headcount.filter { it.n >= 2 }.sortedBy { it.n }.forEach { step ->
            Box(Modifier.fillMaxWidth().height(1.dp).background(LobbyInk.RowLine))
            Row(Modifier.fillMaxWidth().height(38.dp).testTag("party-bonus-${step.n}"), verticalAlignment = Alignment.CenterVertically) {
                Text(stringResource(R.string.crew5_bonus_n, step.n), style = navyText(13.sp, LobbyInk.Text), modifier = Modifier.weight(1f))
                Text("+${CrewRunMath.percent(step.bonus)}%", style = navyText(13.sp, LobbyInk.Text, FontWeight.SemiBold).copy(fontFeatureSettings = "tnum"))
            }
        }
        Spacer(Modifier.height(16.dp))
        LobbyPrimaryButton(stringResource(R.string.common_confirm), onClose, Modifier.testTag("party-bonus-close"))
    }
}

/** 오늘 크루 내구도(26) — 같은 크루의 모든 방이 함께 쓰는 하나. 0 과 읽지 못함을 가른다. 내 몫은 최대 내구도 기준 */
@Composable
private fun StaminaSheet(room: CrewRunRoom?, crewToday: CrewRunToday?, rules: CrewRunRules, onClose: () -> Unit) {
    val pool = room?.pool ?: crewToday?.pool?.takeIf { crewToday.isToday() }
    LobbySheetFrame(stringResource(R.string.crew5_stamina_title), onClose, Modifier.testTag("party-stamina-sheet")) {
        when {
            pool == null -> LobbyNotice(stringResource(R.string.crew5_pool_unknown), warn = true)
            pool.fresh -> LobbyNotice(stringResource(R.string.crew5_stamina_fresh))
            else -> {
                Row(verticalAlignment = Alignment.Bottom) {
                    Text(poolNumber(pool.remaining), style = navyText(46.sp, LobbyInk.Text, FontWeight.ExtraBold, 1.2f).copy(letterSpacing = (-1.5).sp))
                    Text(" / " + poolNumber(pool.total), style = navyText(17.sp, Color(0xFFC1DAED), FontWeight.Medium), modifier = Modifier.padding(bottom = 9.dp))
                }
                LobbyTrack(CrewRunMath.fraction(pool.remaining, pool.total), height = 8.dp)
            }
        }
        Spacer(Modifier.height(8.dp))
        if (pool != null) SheetRow(stringResource(R.string.crew5_stamina_contributors), stringResource(R.string.crew5_stamina_contributors_n, pool.contributors))
        room?.let { SheetRow(stringResource(R.string.crew5_stamina_mine), "+" + poolNumber(it.myContribution), tag = "party-stamina-mine") }
        SheetRow(stringResource(R.string.crew5_stamina_drain), stringResource(R.string.crew5_stamina_drain_value, CrewRunMath.amount(rules.drainPerMin)))
        SheetRow(stringResource(R.string.crew5_stamina_reset), stringResource(R.string.crew5_stamina_reset_value))
        Spacer(Modifier.height(12.dp))
        LobbyNotice(stringResource(R.string.crew5_stamina_note))
        Spacer(Modifier.height(16.dp))
        LobbyPrimaryButton(stringResource(R.string.common_confirm), onClose, Modifier.testTag("party-stamina-close"))
    }
}

/** 크루 목표 · 레벨(27) — 지금 레벨 · 누적 거리 · 다음 목표 · 효과. 방의 코스 거리와 다른 목표다 */
@Composable
private fun GoalSheet(crewToday: CrewRunToday?, onLevels: () -> Unit, onClose: () -> Unit) {
    val today = crewToday?.takeIf { it.isToday() }
    LobbySheetFrame(stringResource(R.string.crew5_goal_sheet_title), onClose, Modifier.testTag("party-goal-sheet")) {
        if (today == null) {
            LobbyNotice(stringResource(R.string.crew5_pool_unknown), warn = true)
        } else {
            val level = today.crewLevel
            Text(
                if (level == null) stringResource(R.string.crew5_goal_sheet_new) else stringResource(R.string.crew5_goal_sheet_level, level),
                style = navyText(28.sp, LobbyInk.Text, FontWeight.ExtraBold, 1.3f),
            )
            Spacer(Modifier.height(8.dp))
            LobbyTrack(CrewRunMath.levelProgress(today.runKm, level, today.nextLevelKm, today.rules.levels), level = true, height = 8.dp)
            Spacer(Modifier.height(8.dp))
            SheetRow(stringResource(R.string.crew5_levels_km), stringResource(R.string.crew5_goal_sheet_total, courseKm(today.runKm)))
            val next = (level ?: 1) + 1
            if (today.nextLevelKm != null && next <= CrewRunMath.LEVEL_MAX) {
                SheetRow(stringResource(R.string.crew5_goal_title),
                    stringResource(R.string.crew5_goal_sheet_next, next, courseKm(CrewRunMath.kmToNext(today.runKm, today.nextLevelKm) ?: 0.0)))
            } else {
                SheetRow(stringResource(R.string.crew5_goal_title), stringResource(R.string.crew5_goal_max), highlight = true)
            }
            SheetRow(
                stringResource(R.string.crew5_levels_level),
                stringResource(R.string.crew5_goal_sheet_effects, CrewRunMath.percent(today.staminaBonus), CrewRunMath.percent(today.rewardBonus)),
            )
        }
        Spacer(Modifier.height(12.dp))
        LobbyNotice(stringResource(R.string.crew5_goal_sheet_note))
        Spacer(Modifier.height(16.dp))
        LobbyPrimaryButton(stringResource(R.string.crew5_goal_sheet_all), onLevels, Modifier.testTag("party-goal-levels"))
    }
}

/** 레벨 1 ~ 20(28) — 누적 거리(그 레벨이 되는 크루 전체 거리) · 내구도 · 보상 효과. 지금 레벨을 밝힌다 */
@Composable
private fun LevelsSheet(crewToday: CrewRunToday?, rules: CrewRunRules, onClose: () -> Unit) {
    val current = crewToday?.takeIf { it.isToday() }?.crewLevel
    LobbySheetFrame(stringResource(R.string.crew5_levels_title), onClose, Modifier.testTag("party-levels-sheet")) {
        Row(Modifier.fillMaxWidth().height(36.dp), verticalAlignment = Alignment.CenterVertically) {
            for ((res, weight) in listOf(R.string.crew5_levels_level to 0.8f, R.string.crew5_levels_km to 1.3f, R.string.crew5_levels_stamina to 1f, R.string.crew5_levels_reward to 1f)) {
                Text(stringResource(res), style = navyText(12.sp, Color(0xFFB2D1E8), FontWeight.Medium), modifier = Modifier.weight(weight),
                    textAlign = if (res == R.string.crew5_levels_level) TextAlign.Start else TextAlign.End)
            }
        }
        rules.levels.sortedBy { it.level }.forEach { step ->
            val on = step.level == current
            val ink = if (on) LobbyInk.Ice else LobbyInk.Text
            Box(Modifier.fillMaxWidth().height(1.dp).background(LobbyInk.RowLine))
            Row(
                Modifier.fillMaxWidth().height(38.dp).semantics(mergeDescendants = true) { selected = on }.testTag("party-level-${step.level}"),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                val style = navyText(13.sp, ink, if (on) FontWeight.Bold else FontWeight.Normal).copy(fontFeatureSettings = "tnum")
                Text("Lv.${step.level}", style = style, modifier = Modifier.weight(0.8f))
                Text("${courseKm(step.km)} km", style = style, modifier = Modifier.weight(1.3f), textAlign = TextAlign.End)
                Text("+${CrewRunMath.percent(step.stamina)}%", style = style, modifier = Modifier.weight(1f), textAlign = TextAlign.End)
                Text("+${CrewRunMath.percent(step.reward)}%", style = style, modifier = Modifier.weight(1f), textAlign = TextAlign.End)
            }
        }
        Spacer(Modifier.height(10.dp))
        Text(stringResource(R.string.crew5_levels_note), style = navyText(12.sp, LobbyInk.Sub, FontWeight.Medium, 1.6f))
        Spacer(Modifier.height(16.dp))
        LobbyPrimaryButton(stringResource(R.string.common_confirm), onClose, Modifier.testTag("party-levels-close"))
    }
}

/**
 * 함께 달릴 내 신발(24) — 앱에 있는 내 신발(서버 · 팔지 않는 것). 고르기는 임시 — "이 신발로 준비"를 눌러야 바뀐다(닫기는 원래 그대로).
 * 기여 미리보기는 최대 내구도 × (1 + 레벨 효과)(남은 개인 내구도가 아니다). 신발 카드는 듀얼 레일(고르지 않은 것은 옅은 테).
 */
@Composable
private fun ShoesSheet(shoes: List<Sneaker>, picked: Long?, crewLevel: Int, onConfirm: (Long) -> Unit, onClose: () -> Unit) {
    val choices = shoes.filter { it.server != null && it.server.status == "OWNED" && it.server.chainState == "APP" }
        .sortedWith(compareByDescending<Sneaker> { it.id == picked }.thenByDescending { it.server?.durabilityMax ?: 0 }.thenByDescending { it.level })
    var draft by rememberSaveable { mutableStateOf(picked ?: choices.firstOrNull { it.equipped }?.id ?: choices.firstOrNull()?.id) }
    LobbySheetFrame(stringResource(R.string.crew5_shoes_title), onClose, Modifier.testTag("party-shoe-sheet")) {
        if (choices.isEmpty()) {
            LobbyNotice(stringResource(R.string.crew5_shoes_empty))
        }
        Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
            choices.forEach { shoe ->
                val server = shoe.server ?: return@forEach
                val on = shoe.id == draft
                DualRailFrame(
                    Modifier.fillMaxWidth().clip(RoundedCornerShape(20.dp))
                        .feedbackClickable(role = Role.RadioButton, cue = FeedbackCue.Select, onClick = { draft = shoe.id })
                        .semantics { selected = on }.testTag("party-shoe-item"),
                    padding = PaddingValues(20.dp), dimmed = !on,
                ) {
                    Column(Modifier.fillMaxWidth()) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            SneakerVisual(shoe, Modifier.width(86.dp).height(70.dp))
                            Spacer(Modifier.width(12.dp))
                            Column(Modifier.weight(1f)) {
                                Text(shoe.shoeName(), style = navyText(16.sp, LobbyInk.Text, FontWeight.Bold), maxLines = 2, overflow = TextOverflow.Ellipsis)
                                Text(stringResource(R.string.crew5_shoes_max, server.durabilityMax), style = navyText(12.sp, Color(0xFFBDD6E9)))
                            }
                            Box(
                                Modifier.size(22.dp).clip(CircleShape).border(2.dp, if (on) LobbyInk.Ice else LobbyInk.AvatarEmptyRing, CircleShape)
                                    .padding(5.dp).clip(CircleShape).background(if (on) LobbyInk.Ice else Color.Transparent),
                            )
                        }
                        Spacer(Modifier.height(12.dp))
                        Row(Modifier.fillMaxWidth()) {
                            ShoeAttr(stringResource(R.string.crew5_shoes_eff), CrewRunMath.percent(server.efficiencyBps / 10_000.0) + "%", Modifier.weight(1f))
                            ShoeAttr(stringResource(R.string.crew5_shoes_comfort), CrewRunMath.percent(server.comfortBps / 10_000.0) + "%", Modifier.weight(1f))
                            ShoeAttr(stringResource(R.string.crew5_shoes_dur), "${server.durabilityMax}", Modifier.weight(1f))
                        }
                    }
                }
            }
        }
        val chosen = choices.firstOrNull { it.id == draft }
        Spacer(Modifier.height(14.dp))
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Text(stringResource(R.string.crew5_shoes_add), style = navyText(12.sp, LobbyInk.Sub, FontWeight.Medium), modifier = Modifier.weight(1f))
            chosen?.server?.let {
                Text("+" + poolNumber(CrewRunMath.contribution(it.durabilityMax, crewLevel)), style = navyText(13.sp, LobbyInk.Ice, FontWeight.Bold),
                    modifier = Modifier.testTag("party-shoe-add"))
            }
        }
        Spacer(Modifier.height(14.dp))
        LobbyPrimaryButton(
            stringResource(R.string.crew5_shoes_confirm), { chosen?.let { onConfirm(it.id) } ?: onClose() },
            Modifier.testTag("party-shoe-close"), enabled = chosen != null,
        )
    }
}

@Composable
private fun ShoeAttr(label: String, value: String, modifier: Modifier) {
    Column(modifier) {
        Text(label, style = navyText(12.sp, Color(0xFFB6D3E7)))
        Text(value, style = navyText(16.sp, LobbyInk.Text, FontWeight.Bold), modifier = Modifier.padding(top = 3.dp))
    }
}

/**
 * 출발 전 마지막 확인(33) · 준비된 인원만 출발(34) — 내 신발 · 출발 인원 · 예상 합산 보너스(인원 + 레벨, 서버 미리보기) · 내가 더할 내구도 ·
 * 하루 한 번. 확정은 출발 2분 뒤 서버가 한다. "함께 출발"을 눌러야 서버에 출발을 보낸다(이 시트는 아무것도 쓰지 않는다).
 */
@Composable
private fun StartSheet(party: PartyState, room: CrewRunRoom?, shoes: List<Sneaker>, onGo: () -> Unit, onWait: () -> Unit) {
    val notReady = party.partySize - party.readyCount
    val partial = notReady > 0
    LobbySheetFrame(
        if (partial) stringResource(R.string.crew5_partial_title, party.readyCount) else stringResource(R.string.crew5_start_title), onWait,
        Modifier.testTag(if (partial) "party-partial-start" else "party-start-confirm"),
    ) {
        if (partial) {
            Text(stringResource(R.string.crew5_partial_body, notReady), style = navyText(14.sp, LobbyInk.NoteInk, FontWeight.Medium, 1.6f))
            Spacer(Modifier.height(8.dp))
        }
        val me = room?.me
        val shoe = me?.sneakerId?.let { id -> shoes.firstOrNull { it.id == id } } ?: shoes.firstOrNull { it.equipped }
        SheetRow(stringResource(R.string.crew5_start_shoe), shoe?.shoeName() ?: stringResource(R.string.crew5_lobby_shoe_none))
        SheetRow(stringResource(R.string.crew5_start_people), stringResource(R.string.crew5_start_people_n, party.readyCount), tag = "crew-start-people")
        if (room != null) {
            SheetRow(
                stringResource(R.string.crew5_start_bonus),
                stringResource(
                    R.string.crew5_start_bonus_value, CrewRunMath.percent(room.bonusPct), room.crewLevel,
                    CrewRunMath.percent(room.rewardBonus), CrewRunMath.percent(room.bonusPct + room.rewardBonus),
                ),
                sub = stringResource(R.string.crew_lobby_preview_note, room.rules.confirmSec / 60, room.rules.nearM),
                highlight = true, tag = "crew-start-bonus",
            )
            SheetRow(stringResource(R.string.crew5_start_add), "+" + poolNumber(room.myContribution))
        }
        Spacer(Modifier.height(12.dp))
        LobbyNotice(stringResource(R.string.crew_start_once))
        Spacer(Modifier.height(16.dp))
        LobbyPrimaryButton(
            if (partial) stringResource(R.string.crew5_partial_go, party.readyCount) else stringResource(R.string.crew5_start_go), onGo,
            Modifier.testTag("crew-start-go"),
        )
        LobbyTextButton(stringResource(if (partial) R.string.crew5_partial_wait else R.string.common_cancel), onWait, "crew-start-wait")
    }
}

/** 보너스 인원 제외 안내(40) — 휴대폰 인증 전이어도 참여 · 기여 · 내 신발 보상은 그대로. 번호 입력 · 문자로 보내지 않는다 */
@Composable
private fun UnverifiedSheet(onOk: () -> Unit, onClose: () -> Unit) {
    LobbySheetFrame(stringResource(R.string.crew5_unverified_title), onClose, Modifier.testTag("crew-unverified")) {
        Text(stringResource(R.string.crew_run_unverified), style = navyText(14.sp, LobbyInk.NoteInk, FontWeight.Medium, 1.6f))
        Spacer(Modifier.height(20.dp))
        LobbyPrimaryButton(stringResource(R.string.crew5_unverified_ok), onOk, Modifier.testTag("crew-unverified-ok"))
    }
}

/** 대기실의 코스 시트 — 진행자의 코스 바꾸기(07) · 참가자의 코스 보기(25). 전체 경로 · 거리 · 출발 점(흰 점) */
@Composable
private fun LobbyCourseSheet(
    course: RoomCourse,
    title: String,
    note: String?,
    primary: String,
    onPrimary: () -> Unit,
    onClose: () -> Unit,
    tag: String,
) {
    LobbySheetFrame(title, onClose, Modifier.testTag(tag)) {
        Text(course.name, style = navyText(18.sp, LobbyInk.Text, FontWeight.ExtraBold), modifier = Modifier.testTag("party-course-name"))
        Text(stringResource(R.string.crew5_course_detail_meta, courseKm(course.km)), style = navyText(13.sp, LobbyInk.Sub, FontWeight.Medium),
            modifier = Modifier.padding(top = 3.dp))
        Spacer(Modifier.height(12.dp))
        CrewCourseMap(course.points, Modifier.fillMaxWidth().height(280.dp), interactive = true)
        if (note != null) {
            Spacer(Modifier.height(12.dp))
            LobbyNotice(note)
        }
        Spacer(Modifier.height(16.dp))
        LobbyPrimaryButton(primary, onPrimary, Modifier.testTag("$tag-go"))
    }
}

/** 참가자 — 코스가 바뀌었다(08). "코스 확인"은 읽기 전용 보기로. 닫아도 다시 묻지 않는다(본 것으로 둔다) */
@Composable
private fun CourseUpdatedSheet(courseName: String, onCheck: () -> Unit, onClose: () -> Unit) {
    LobbySheetFrame(stringResource(R.string.crew5_course_updated_title), onClose, Modifier.testTag("party-course-updated")) {
        Text(stringResource(R.string.crew5_course_updated_body, courseName), style = navyText(14.sp, LobbyInk.NoteInk, FontWeight.Medium, 1.6f))
        Spacer(Modifier.height(20.dp))
        LobbyPrimaryButton(stringResource(R.string.crew5_course_updated_ok), onCheck, Modifier.testTag("party-course-check"))
    }
}

/** 끝난 크루 러닝 — 금액은 서버가 확인한 뒤에만(그 전에는 "서버 확인 중") */
@Composable
private fun CrewLobbyResult(resultPoints: Double?) {
    val c = crewColors()
    CrewCard(Modifier.testTag("party-result"), padding = 18.dp) {
        Spacer(Modifier.height(10.dp))
        Text(stringResource(R.string.crew_result_title), style = com.stepup.android.ui.components.runTextStyle(18.sp, c.text, FontWeight.ExtraBold))
        Spacer(Modifier.height(8.dp))
        if (resultPoints != null) {
            Text(stringResource(R.string.run_cr_result_points), style = com.stepup.android.ui.components.runTextStyle(13.sp, c.label))
            Text("+%.2f SUP".format(resultPoints), style = com.stepup.android.ui.components.runTextStyle(28.sp, c.text, FontWeight.ExtraBold),
                modifier = Modifier.testTag("party-result-points"))
        } else {
            Text(stringResource(R.string.finish_pending_short), style = com.stepup.android.ui.components.runTextStyle(16.sp, c.label, FontWeight.SemiBold),
                modifier = Modifier.testTag("party-result-pending"))
        }
        Spacer(Modifier.height(10.dp))
    }
}
