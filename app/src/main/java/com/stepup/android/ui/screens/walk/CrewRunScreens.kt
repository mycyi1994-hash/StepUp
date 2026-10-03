package com.stepup.android.ui.screens.walk

import android.graphics.Bitmap
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.Chat
import androidx.compose.material.icons.filled.Groups
import androidx.compose.material.icons.outlined.CloudOff
import androidx.compose.material.icons.outlined.LocationOn
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.stepup.android.R
import com.stepup.android.core.ServiceLocator
import com.stepup.android.data.repo.ChatOutcome
import com.stepup.android.data.repo.ChatPhoto
import com.stepup.android.data.repo.Crew
import com.stepup.android.data.repo.CrewSyncState
import com.stepup.android.domain.ChatMessage
import com.stepup.android.ui.components.RunButton
import com.stepup.android.ui.components.RunButtonKind
import com.stepup.android.ui.components.RunCard
import com.stepup.android.ui.components.RunChoiceRow
import com.stepup.android.ui.components.RunEmptyState
import com.stepup.android.ui.components.RunNotice
import com.stepup.android.ui.components.RunNoticeKind
import com.stepup.android.ui.components.RunNumber
import com.stepup.android.ui.components.RunPage
import com.stepup.android.ui.components.RunSheet
import com.stepup.android.ui.components.RunSheetText
import com.stepup.android.ui.components.RunSpinner
import com.stepup.android.ui.components.RunStat
import com.stepup.android.ui.components.RunStatRow
import com.stepup.android.ui.components.RunStateArt
import com.stepup.android.ui.components.RunSwitch
import com.stepup.android.ui.components.RunTextAction
import com.stepup.android.ui.components.runTextStyle
import com.stepup.android.ui.components.runTone
import com.stepup.android.ui.theme.StepUpSans
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch

/*
 * 크루 달리기(러닝 전체 리메이크 2026-10-02 · 시안 CR02–CR08 · CR13–CR17 · CR19–CR21) — 시작 메뉴(U01)의 크루 달리기 입구,
 * 크루 선택, 준비 전 확인, 출발 확인, 크루에게 알리기, 크루에 기록 공유.
 *
 * 대기실(party_open)은 들어가는 순간 참가 · 생성을 일으킨다. 입구에서는 부르지 않고 "대기실 들어가기"를 눌러야만 연다.
 * 참가 없이 대기실을 들여다보는 서버 함수가 없어(남은 연동) 준비 중인 대기실의 러닝 정보는 입구에 보이지 않는다.
 */

/** 크루 달리기에서 마지막으로 고른 크루(CR19) — 앱이 켜져 있는 동안 */
object CrewRunChoice {
    val current = MutableStateFlow<String?>(null)
}

/** 크루 달리기 입구의 상태 */
sealed interface CrewEntryUi {
    data object Loading : CrewEntryUi
    data object SignIn : CrewEntryUi
    data class Failed(val reason: String) : CrewEntryUi
    /** 가입한 크루가 없다(CR14) */
    data object NoCrew : CrewEntryUi
    /** 고른 크루 — 가입한 크루가 여럿이면 바꿀 수 있다(CR19) */
    data class Ready(val crew: Crew, val crews: List<Crew>) : CrewEntryUi
}

/**
 * 크루 달리기 입구(U01 → 크루) — 가입한 크루가 없으면 CR14, 있으면 고른 크루(여럿이면 최근 고른 것)와 "대기실 들어가기".
 * 크루 목록은 서버에서 다시 받는다. 자동으로 가입 · 크루 생성 · 대기실 참가를 하지 않는다.
 */
@Composable
fun CrewRunEntryScreen(
    onBack: () -> Unit,
    onEnterLobby: (String) -> Unit,
    onFindCrew: () -> Unit,
    onCreateCrew: () -> Unit,
    onFreeRun: () -> Unit,
    onSignIn: () -> Unit,
) {
    val repo = ServiceLocator.crewRepository
    val crews by repo.crews.collectAsStateWithLifecycle()
    val sync by repo.sync.collectAsStateWithLifecycle()
    val chosen by CrewRunChoice.current.collectAsStateWithLifecycle()
    val scope = rememberCoroutineScope()
    LaunchedEffect(Unit) { repo.refresh() }
    val joined = crews.filter { it.joined }
    val state = sync
    val ui = when {
        joined.isNotEmpty() -> CrewEntryUi.Ready(joined.firstOrNull { it.id == chosen } ?: joined.first(), joined)
        state == CrewSyncState.SignInRequired -> CrewEntryUi.SignIn
        state is CrewSyncState.Failed -> CrewEntryUi.Failed(state.reason)
        state == CrewSyncState.Ready -> CrewEntryUi.NoCrew
        else -> CrewEntryUi.Loading
    }
    var picking by rememberSaveable { mutableStateOf(false) }
    CrewRunEntryContent(
        ui = ui, onBack = onBack,
        onEnter = { id ->
            CrewRunChoice.current.value = id
            onEnterLobby(id)
        },
        onChange = { picking = true }, onFindCrew = onFindCrew, onCreateCrew = onCreateCrew, onFreeRun = onFreeRun,
        onRetry = { scope.launch { repo.refresh() } }, onSignIn = onSignIn,
    )
    if (picking && ui is CrewEntryUi.Ready) {
        CrewPickSheet(
            ui.crews, ui.crew.id,
            onApply = {
                CrewRunChoice.current.value = it
                picking = false
            },
            onDismiss = { picking = false },
        )
    }
}

@Composable
fun CrewRunEntryContent(
    ui: CrewEntryUi,
    onBack: () -> Unit,
    onEnter: (String) -> Unit,
    onChange: () -> Unit,
    onFindCrew: () -> Unit,
    onCreateCrew: () -> Unit,
    onFreeRun: () -> Unit,
    onRetry: () -> Unit,
    onSignIn: () -> Unit,
) {
    val t = runTone()
    when (ui) {
        CrewEntryUi.Loading -> RunPage(onBack = onBack, modifier = Modifier.testTag("crew-entry-loading"),
            title = stringResource(R.string.run_crew_title)) {
            Box(Modifier.fillMaxWidth().padding(top = 80.dp), contentAlignment = Alignment.Center) { RunSpinner(Modifier.size(40.dp)) }
        }
        CrewEntryUi.SignIn -> RunPage(
            onBack = onBack, modifier = Modifier.testTag("crew-entry-sign-in"), title = stringResource(R.string.run_crew_title),
            bottom = { RunButton(stringResource(R.string.run_cr_sign_in), onSignIn, Modifier.testTag("crew-entry-sign-in-go")) },
        ) {
            Spacer(Modifier.height(40.dp))
            RunEmptyState(Icons.Filled.Groups, stringResource(R.string.run_cr_sign_in_title))
        }
        is CrewEntryUi.Failed -> RunPage(
            onBack = onBack, modifier = Modifier.testTag("crew-entry-failed"), title = stringResource(R.string.run_crew_title),
            bottom = { RunButton(stringResource(R.string.run_rec_reload), onRetry, Modifier.testTag("crew-entry-retry"), italic = true) },
        ) {
            Spacer(Modifier.height(40.dp))
            RunEmptyState(Icons.Outlined.CloudOff, stringResource(R.string.run_cr_load_failed), alert = true)
        }
        // CR14 — 가입한 크루가 없다. 찾기 · 만들기는 기존 크루 기능으로, 자유 러닝은 바로
        CrewEntryUi.NoCrew -> RunPage(
            onBack = onBack, modifier = Modifier.testTag("crew-entry-none"), title = stringResource(R.string.run_crew_title),
            bottom = {
                RunButton(stringResource(R.string.run_cr_find), onFindCrew, Modifier.testTag("crew-entry-find"), italic = true)
                RunButton(stringResource(R.string.run_cr_create), onCreateCrew, Modifier.testTag("crew-entry-create"),
                    kind = RunButtonKind.Secondary)
                RunTextAction(stringResource(R.string.run_cr_free), onFreeRun, Modifier.fillMaxWidth().testTag("crew-entry-free"),
                    chevron = true)
            },
        ) {
            Spacer(Modifier.height(24.dp))
            Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) { RunStateArt(Icons.Filled.Groups, size = 200.dp) }
            Spacer(Modifier.height(26.dp))
            Text(
                stringResource(R.string.run_cr_none_title),
                style = TextStyle(
                    fontFamily = StepUpSans, fontWeight = FontWeight.ExtraBold, fontSize = 34.sp, lineHeight = 1.22.em,
                    letterSpacing = (-0.03).em, color = t.text,
                ),
                textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth().semantics { heading() },
            )
            Spacer(Modifier.height(10.dp))
            Text(stringResource(R.string.run_cr_none_body), style = runTextStyle(17.sp, t.label, FontWeight.Medium),
                textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth())
        }
        // 고른 크루(CR03 의 자리) — 대기실은 눌러야만 열린다
        is CrewEntryUi.Ready -> RunPage(
            onBack = onBack, modifier = Modifier.testTag("crew-entry"), crumb = stringResource(R.string.run_crew_title),
            bottom = {
                RunButton(stringResource(R.string.run_cr_enter), { onEnter(ui.crew.id) }, Modifier.testTag("crew-entry-enter"), italic = true)
                if (ui.crews.size > 1) {
                    RunButton(stringResource(R.string.run_cr_change), onChange, Modifier.testTag("crew-entry-change"),
                        kind = RunButtonKind.Secondary)
                }
            },
        ) {
            CrewTitleRow(ui.crew.name, ui.crew.monogram, ui.crew.tagline.takeIf { it.isNotBlank() })
            Spacer(Modifier.height(16.dp))
            RunCard(padding = PaddingValues(horizontal = 18.dp, vertical = 22.dp), tag = "crew-entry-card") {
                Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
                    RunStateArt(Icons.Filled.Groups, size = 96.dp)
                    Spacer(Modifier.height(14.dp))
                    Text(stringResource(R.string.run_cr_enter_title), style = runTextStyle(21.sp, t.text, FontWeight.ExtraBold),
                        textAlign = TextAlign.Center)
                    Spacer(Modifier.height(6.dp))
                    Text(stringResource(R.string.run_cr_enter_body), style = runTextStyle(15.sp, t.label, FontWeight.Medium, 1.5f),
                        textAlign = TextAlign.Center)
                    Spacer(Modifier.height(8.dp))
                    Text(stringResource(R.string.run_cr_enter_note), style = runTextStyle(13.sp, t.muted, FontWeight.Medium),
                        textAlign = TextAlign.Center)
                }
            }
            Spacer(Modifier.height(12.dp))
            RunStatRow(
                listOfNotNull(
                    RunStat(stringResource(R.string.run_crew_label), stringResource(R.string.run_crew_people, ui.crew.memberCount)),
                    ui.crew.area.takeIf { it.isNotBlank() }?.let { RunStat(stringResource(R.string.run_cr_area), it) },
                ),
                framed = true, valueSize = 24.sp,
            )
        }
    }
}

/** 크루 이름 줄 — 크루 표시(이니셜) · 큰 이름 · (한 줄 소개) */
@Composable
internal fun CrewTitleRow(name: String, monogram: String, tagline: String? = null) {
    val t = runTone()
    Row(verticalAlignment = Alignment.CenterVertically) {
        CrewMark(monogram.ifBlank { initialsOf(name) }, 56.dp)
        Spacer(Modifier.width(14.dp))
        Column(Modifier.weight(1f)) {
            Text(
                name,
                style = TextStyle(
                    fontFamily = StepUpSans, fontWeight = FontWeight.ExtraBold, fontSize = 30.sp, letterSpacing = (-0.03).em,
                    color = t.text,
                ),
                maxLines = 2, overflow = TextOverflow.Ellipsis,
                modifier = Modifier.semantics { heading() }.testTag("crew-title"),
            )
            if (tagline != null) {
                Text(tagline, style = runTextStyle(15.sp, t.label, FontWeight.Medium), maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
        }
    }
}

/** 크루 표시 — 파란 둥근 네모 안에 크루 이니셜(크루가 정한 글자) */
@Composable
internal fun CrewMark(text: String, size: Dp) {
    val shape = RoundedCornerShape(size * 0.28f)
    Box(
        Modifier.size(size).clip(shape)
            .background(Brush.linearGradient(listOf(Color(0xFF1D6BFF), Color(0xFF0A3FC0)))),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text.take(2).uppercase(),
            style = TextStyle(fontFamily = StepUpSans, fontWeight = FontWeight.ExtraBold, fontSize = (size.value * 0.36f).sp, color = Color.White),
            maxLines = 1,
        )
    }
}

/** 내 크루 선택(CR19) — 가입한 크루 중 하나. 고르기만 하고 대기실은 열지 않는다 */
@Composable
internal fun CrewPickSheet(crews: List<Crew>, selected: String, onApply: (String) -> Unit, onDismiss: () -> Unit) {
    val t = runTone()
    var pending by rememberSaveable { mutableStateOf(selected) }
    RunSheet(onDismiss = onDismiss, modifier = Modifier.testTag("crew-pick-sheet")) {
        Text(
            stringResource(R.string.run_cr_pick_title),
            style = TextStyle(fontFamily = StepUpSans, fontWeight = FontWeight.ExtraBold, fontSize = 25.sp, color = t.text),
            modifier = Modifier.fillMaxWidth().semantics { heading() },
        )
        Text(stringResource(R.string.run_cr_pick_body), style = runTextStyle(15.sp, t.label, FontWeight.Medium),
            modifier = Modifier.fillMaxWidth().padding(top = 4.dp))
        Spacer(Modifier.height(14.dp))
        Column(
            Modifier.fillMaxWidth().heightIn(max = 340.dp).verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            crews.forEach { crew ->
                RunChoiceRow(
                    crew.name, Modifier.testTag("crew-pick-${crew.id}"),
                    description = stringResource(R.string.run_cr_members, crew.memberCount),
                    selected = pending == crew.id, onSelect = { pending = crew.id },
                    leading = { CrewMark(crew.monogram.ifBlank { initialsOf(crew.name) }, 44.dp) },
                )
            }
        }
        Spacer(Modifier.height(16.dp))
        RunButton(stringResource(R.string.run_cr_pick_apply), { onApply(pending) }, Modifier.testTag("crew-pick-apply"))
    }
}

/**
 * 준비 전 확인(CR17) — 위치 사용(OS 권한)과 크루원에게 위치 공유(기본 끔)는 따로. 허용은 OS 창을 띄울 뿐이고,
 * 돌아오면 다시 읽어 걸음 권한이 있으면 준비한다(위치 없이도 준비할 수 있다). 나중에는 준비 전으로 남는다.
 */
@Composable
internal fun CrewReadyCheckSheet(
    locationAllowed: Boolean,
    share: Boolean,
    onShare: (Boolean) -> Unit,
    onAllow: () -> Unit,
    onLater: () -> Unit,
) {
    RunSheet(onDismiss = onLater, modifier = Modifier.testTag("crew-ready-check"), showClose = false) {
        CrewReadyCheckBody(locationAllowed, share, onShare, onAllow, onLater)
    }
}

@Composable
internal fun ColumnScope.CrewReadyCheckBody(
    locationAllowed: Boolean,
    share: Boolean,
    onShare: (Boolean) -> Unit,
    onAllow: () -> Unit,
    onLater: () -> Unit,
) {
    val t = runTone()
    RunSheetText(stringResource(R.string.run_cr_check_title), body = stringResource(R.string.run_cr_check_body))
    Spacer(Modifier.height(16.dp))
    RunCard(padding = PaddingValues(horizontal = 14.dp, vertical = 12.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Outlined.LocationOn, contentDescription = null, tint = t.label, modifier = Modifier.size(26.dp))
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(stringResource(R.string.run_cr_check_location), style = runTextStyle(16.sp, t.text, FontWeight.Bold))
                Text(stringResource(R.string.run_cr_check_location_body), style = runTextStyle(12.sp, t.label))
            }
            Text(
                stringResource(if (locationAllowed) R.string.run_cr_check_allowed else R.string.run_cr_check_needed),
                style = runTextStyle(14.sp, if (locationAllowed) t.cyan else t.dangerText, FontWeight.Bold),
                modifier = Modifier.testTag("crew-ready-check-location"),
            )
        }
    }
    Spacer(Modifier.height(8.dp))
    RunCard(padding = PaddingValues(horizontal = 14.dp, vertical = 10.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Filled.Groups, contentDescription = null, tint = t.label, modifier = Modifier.size(26.dp))
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(stringResource(R.string.run_cr_check_share), style = runTextStyle(16.sp, t.text, FontWeight.Bold))
                Text(stringResource(R.string.run_cr_check_share_body), style = runTextStyle(12.sp, t.label))
            }
            RunSwitch(share, onShare, Modifier.testTag("crew-ready-check-share"))
        }
    }
    Text(stringResource(R.string.run_cr_check_note), style = runTextStyle(12.sp, t.muted), modifier = Modifier.fillMaxWidth().padding(top = 8.dp))
    Spacer(Modifier.height(16.dp))
    RunButton(stringResource(R.string.run_cr_check_allow), onAllow, Modifier.testTag("crew-ready-check-allow"))
    Spacer(Modifier.height(10.dp))
    RunButton(stringResource(R.string.run_perm_later), onLater, Modifier.testTag("crew-ready-check-later"), kind = RunButtonKind.Secondary)
}

/** 준비한 사람끼리 출발(CR07) — 준비 완료 · 준비 전 인원은 지금 방 상태 그대로(출발은 서버가 명단을 정한다) */
@Composable
internal fun CrewStartConfirmSheet(ready: Int, notReady: Int, onGo: () -> Unit, onWait: () -> Unit) {
    RunSheet(onDismiss = onWait, modifier = Modifier.testTag("crew-start-confirm"), showClose = false) {
        CrewStartConfirmBody(ready, notReady, onGo, onWait)
    }
}

@Composable
internal fun ColumnScope.CrewStartConfirmBody(ready: Int, notReady: Int, onGo: () -> Unit, onWait: () -> Unit) {
    RunSheetText(
        stringResource(R.string.run_cr_go_confirm_title, ready),
        body = stringResource(R.string.run_cr_go_confirm_body, ready, notReady),
    )
    Spacer(Modifier.height(14.dp))
    RunStatRow(
        listOf(
            RunStat(stringResource(R.string.run_cr_ready), "$ready", stringResource(R.string.run_cr_people_unit), accent = true, tag = "crew-start-ready"),
            RunStat(stringResource(R.string.run_cr_not_ready), "$notReady", stringResource(R.string.run_cr_people_unit), tag = "crew-start-not-ready"),
        ),
        framed = true, valueSize = 32.sp,
    )
    Spacer(Modifier.height(16.dp))
    RunButton(stringResource(R.string.run_cr_start_n, ready), onGo, Modifier.testTag("crew-start-go"))
    Spacer(Modifier.height(10.dp))
    RunButton(stringResource(R.string.run_cr_wait_more), onWait, Modifier.testTag("crew-start-wait"), kind = RunButtonKind.Secondary)
}

/** 크루 채팅으로 보내기의 상태 — 보낸 뒤에는 다시 보내지 않는다(같은 요청 키로만 다시 시도) */
enum class CrewSend { Idle, Sending, Sent, Failed }

/**
 * 크루에게 알리기(CR20) — 보낼 곳(크루 채팅)과 내용을 먼저 보이고 "알림 보내기"를 눌러야만 보낸다.
 * 실패하면 같은 메시지(같은 요청 키)로 다시 보낸다 — 서버에는 하나만 남는다.
 */
@Composable
internal fun CrewNotifySheet(crewId: String, crewName: String, waiting: Int, onDismiss: () -> Unit) {
    val chat = ServiceLocator.crewChat
    val scope = rememberCoroutineScope()
    val body = stringResource(R.string.run_cr_notify_message, crewName, waiting)
    // 보냈음은 화면을 돌려도 남긴다(다시 보내지 않게). 보내는 중에 화면이 다시 만들어졌으면 결과를 모르니 실패로 두고 다시 보내게 한다
    var state by rememberSaveable { mutableStateOf(CrewSend.Idle) }
    LaunchedEffect(Unit) { if (state == CrewSend.Sending) state = CrewSend.Failed }
    var pending by remember { mutableStateOf<ChatMessage?>(null) }
    val text = pending?.body ?: body
    RunSheet(onDismiss = onDismiss, modifier = Modifier.testTag("crew-notify-sheet"), dismissible = state != CrewSend.Sending) {
        CrewNotifyBody(
            crewName, text, state,
            onSend = {
                if (state == CrewSend.Sending || state == CrewSend.Sent) return@CrewNotifyBody
                scope.launch {
                    state = CrewSend.Sending
                    val message = pending ?: chat.outgoing(crewId, text, null, null).also { pending = it }
                    state = if (chat.send(crewId, message, null) is ChatOutcome.Ok) CrewSend.Sent else CrewSend.Failed
                }
            },
            onCancel = onDismiss,
        )
    }
}

@Composable
internal fun ColumnScope.CrewNotifyBody(crewName: String, text: String, state: CrewSend, onSend: () -> Unit, onCancel: () -> Unit) {
    val t = runTone()
    Text(stringResource(R.string.run_cr_notify_title), style = runTextStyle(20.sp, t.text, FontWeight.ExtraBold), textAlign = TextAlign.Center)
    Spacer(Modifier.height(14.dp))
    RunCard(padding = PaddingValues(14.dp), tag = "crew-notify-preview") {
        Row(verticalAlignment = Alignment.CenterVertically) {
            CrewMark(initialsOf(crewName), 30.dp)
            Spacer(Modifier.width(10.dp))
            Text(crewName, style = runTextStyle(16.sp, t.text, FontWeight.Bold), maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
        Spacer(Modifier.height(10.dp))
        Box(
            Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp)).background(t.inset).padding(14.dp),
        ) {
            Text(text, style = runTextStyle(15.sp, t.text, FontWeight.Medium, 1.5f), modifier = Modifier.testTag("crew-notify-text"))
        }
        Spacer(Modifier.height(10.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.AutoMirrored.Outlined.Chat, contentDescription = null, tint = t.label, modifier = Modifier.size(22.dp))
            Spacer(Modifier.width(10.dp))
            Column {
                Text(stringResource(R.string.run_cr_notify_where), style = runTextStyle(12.sp, t.label))
                Text(stringResource(R.string.run_cr_notify_chat), style = runTextStyle(15.sp, t.text, FontWeight.Bold))
            }
        }
    }
    when (state) {
        CrewSend.Sent -> RunNotice(stringResource(R.string.run_cr_notify_sent), Modifier.padding(top = 10.dp), tag = "crew-notify-sent")
        CrewSend.Failed -> RunNotice(stringResource(R.string.run_cr_send_failed), Modifier.padding(top = 10.dp), kind = RunNoticeKind.Error,
            tag = "crew-notify-failed")
        else -> Unit
    }
    Spacer(Modifier.height(16.dp))
    if (state == CrewSend.Sent) {
        RunButton(stringResource(R.string.common_close), onCancel, Modifier.testTag("crew-notify-close"))
    } else {
        RunButton(
            stringResource(R.string.run_cr_notify_send), onSend, Modifier.testTag("crew-notify-send"),
            busy = state == CrewSend.Sending,
        )
        Spacer(Modifier.height(10.dp))
        RunButton(stringResource(R.string.common_cancel), onCancel, Modifier.testTag("crew-notify-cancel"),
            kind = RunButtonKind.Secondary, enabled = state != CrewSend.Sending)
    }
}

/**
 * 크루에 기록 공유(CR21) — 내 기록(거리 · 시간 · 페이스) · 보낼 곳(크루 채팅) · 경로 포함(처음엔 끔). 미리보기는 보낼 그대로.
 * "공유하기"를 눌러야만 보낸다. 경로를 켜면 공유 카드 그림(지도 · 경로)을 함께 보낸다. 실패하면 같은 내용으로 다시 보낸다.
 */
@Composable
internal fun CrewShareScreen(
    crewId: String,
    crewName: String,
    km: String,
    time: String,
    pace: String,
    card: suspend (Boolean) -> Bitmap?,
    hasRoute: Boolean,
    onClose: () -> Unit,
) {
    val chat = ServiceLocator.crewChat
    val scope = rememberCoroutineScope()
    var includeRoute by rememberSaveable { mutableStateOf(false) }
    var preview by remember { mutableStateOf<Bitmap?>(null) }
    var state by rememberSaveable { mutableStateOf(CrewSend.Idle) }
    // 보낸 것(요청 키 · 그림)은 다시 보낼 때 그대로 — 경로를 바꾸면 새 메시지로
    var pending by remember { mutableStateOf<Pair<ChatMessage, ChatPhoto?>?>(null) }
    val text = stringResource(R.string.run_cr_share_message, km, time, pace)
    var restored by remember { mutableStateOf(true) }
    LaunchedEffect(includeRoute) {
        // 보내는 중에 화면이 다시 만들어졌으면 결과를 모른다 — 실패로 두고 다시 보내게 한다
        if (restored && state == CrewSend.Sending) state = CrewSend.Failed
        // 보낼 것은 그림을 다시 그리기 전에 비운다 — 그리는 사이에 누른 공유하기(pending)를 나중에 지우지 않게
        if (!restored && state != CrewSend.Sent) {
            pending = null
            if (state == CrewSend.Failed) state = CrewSend.Idle
        }
        restored = false
        preview = null
        if (includeRoute) preview = card(true)
    }
    CrewShareContent(
        crewName = crewName, km = km, time = time, pace = pace, message = text, includeRoute = includeRoute, hasRoute = hasRoute,
        preview = preview, state = state,
        onRoute = { if (state != CrewSend.Sending && state != CrewSend.Sent) includeRoute = it },
        onShare = {
            if (state == CrewSend.Sending || state == CrewSend.Sent) return@CrewShareContent
            scope.launch {
                state = CrewSend.Sending
                val ready = pending ?: run {
                    val photo = if (includeRoute) (preview ?: card(true))?.let { chat.photoOf(it) } else null
                    chat.outgoing(crewId, text, null, photo) to photo
                }.also { pending = it }
                state = if (chat.send(crewId, ready.first, ready.second) is ChatOutcome.Ok) CrewSend.Sent else CrewSend.Failed
            }
        },
        onClose = onClose,
    )
}

@Composable
internal fun CrewShareContent(
    crewName: String,
    km: String,
    time: String,
    pace: String,
    message: String,
    includeRoute: Boolean,
    hasRoute: Boolean,
    preview: Bitmap?,
    state: CrewSend,
    onRoute: (Boolean) -> Unit,
    onShare: () -> Unit,
    onClose: () -> Unit,
) {
    val t = runTone()
    RunPage(
        onBack = onClose, modifier = Modifier.testTag("crew-share"),
        title = stringResource(R.string.run_cr_share_title), subtitle = stringResource(R.string.run_cr_share_sub),
        bottom = {
            if (state == CrewSend.Sent) {
                RunButton(stringResource(R.string.common_close), onClose, Modifier.testTag("crew-share-close"))
            } else {
                RunButton(stringResource(R.string.run_cr_share_do), onShare, Modifier.testTag("crew-share-send"), italic = true,
                    busy = state == CrewSend.Sending)
                RunButton(stringResource(R.string.common_cancel), onClose, Modifier.testTag("crew-share-cancel"),
                    kind = RunButtonKind.Secondary, enabled = state != CrewSend.Sending)
            }
        },
    ) {
        RunCard(padding = PaddingValues(horizontal = 18.dp, vertical = 14.dp), tag = "crew-share-record") {
            Text(stringResource(R.string.run_cr_share_my), style = runTextStyle(15.sp, t.label, FontWeight.SemiBold))
            RunNumber(km, unit = "km", size = 76.sp, unitSize = 28.sp, align = Alignment.CenterHorizontally,
                modifier = Modifier.fillMaxWidth(), valueTag = "crew-share-km")
            Box(Modifier.padding(vertical = 8.dp).fillMaxWidth().height(1.dp).background(t.divider))
            RunStatRow(
                listOf(
                    RunStat(stringResource(R.string.run_label_time), time),
                    RunStat(stringResource(R.string.run_label_pace), pace, "/km"),
                ),
                valueSize = 30.sp,
            )
        }
        Spacer(Modifier.height(12.dp))
        RunCard(padding = PaddingValues(horizontal = 16.dp, vertical = 12.dp), tag = "crew-share-target") {
            Text(stringResource(R.string.run_cr_share_target), style = runTextStyle(14.sp, t.label, FontWeight.SemiBold))
            Spacer(Modifier.height(8.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(Modifier.size(40.dp).clip(CircleShape).background(t.inset), contentAlignment = Alignment.Center) {
                    Icon(Icons.Filled.Groups, contentDescription = null, tint = t.label, modifier = Modifier.size(22.dp))
                }
                Spacer(Modifier.width(12.dp))
                Column {
                    Text(crewName, style = runTextStyle(16.sp, t.text, FontWeight.Bold), maxLines = 1, overflow = TextOverflow.Ellipsis)
                    Text(stringResource(R.string.run_cr_share_target_body), style = runTextStyle(12.sp, t.label))
                }
            }
        }
        Spacer(Modifier.height(12.dp))
        RunCard(padding = PaddingValues(horizontal = 16.dp, vertical = 12.dp), tag = "crew-share-route") {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text(stringResource(R.string.run_cr_share_route), style = runTextStyle(16.sp, t.text, FontWeight.Bold))
                    Text(stringResource(R.string.run_cr_share_route_body), style = runTextStyle(12.sp, t.label))
                }
                RunSwitch(includeRoute, onRoute, Modifier.testTag("crew-share-route-toggle"), enabled = hasRoute && state != CrewSend.Sent)
            }
        }
        // 미리보기 — 보낼 글과(경로를 켜면) 그림
        Spacer(Modifier.height(12.dp))
        RunCard(padding = PaddingValues(14.dp), tag = "crew-share-preview") {
            Text(message, style = runTextStyle(15.sp, t.text, FontWeight.Medium, 1.5f), modifier = Modifier.testTag("crew-share-text"))
            val image = preview
            if (includeRoute && image != null) {
                Spacer(Modifier.height(10.dp))
                Image(
                    image.asImageBitmap(), contentDescription = null, contentScale = ContentScale.Fit,
                    modifier = Modifier.fillMaxWidth().height(220.dp).clip(RoundedCornerShape(12.dp)).testTag("crew-share-image"),
                )
            }
        }
        when (state) {
            CrewSend.Sent -> RunNotice(stringResource(R.string.run_cr_shared), Modifier.padding(top = 12.dp), tag = "crew-share-sent")
            CrewSend.Failed -> RunNotice(stringResource(R.string.run_cr_send_failed), Modifier.padding(top = 12.dp),
                kind = RunNoticeKind.Error, tag = "crew-share-failed")
            else -> Unit
        }
    }
}
