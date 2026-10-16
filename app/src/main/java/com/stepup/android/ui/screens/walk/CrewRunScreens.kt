package com.stepup.android.ui.screens.walk

import android.graphics.Bitmap
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.Chat
import androidx.compose.material.icons.automirrored.outlined.Send
import androidx.compose.material.icons.filled.Groups
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.CloudOff
import androidx.compose.material.icons.outlined.PeopleAlt
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
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
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
import com.stepup.android.ui.components.RunSheet
import com.stepup.android.ui.components.RunSheetText
import com.stepup.android.ui.components.RunSpinner
import com.stepup.android.ui.components.RunStat
import com.stepup.android.ui.components.RunStatRow
import com.stepup.android.ui.components.RunSwitch
import com.stepup.android.ui.components.runTextStyle
import com.stepup.android.ui.components.runTone
import com.stepup.android.ui.experience.FeedbackCue
import com.stepup.android.ui.experience.feedbackClickable
import com.stepup.android.ui.theme.StepUpSans
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch

/*
 * 크루 달리기(크루 달리기 v2 · 2026-10-16 전달본 CR03 · CR07 · CR14 · CR17 · CR19 · CR20 · CR21) — 시작 메뉴(U01)의 크루 달리기 입구,
 * 크루 선택, 준비 전 확인, 출발 확인, 크루에게 알리기, 크루에 기록 공유.
 *
 * 대기실(party_open)은 들어가는 순간 참가 · 생성을 일으킨다. 입구에서는 부르지 않고 "대기실 들어가기"를 눌러야만 연다.
 * 참가 없이 대기실을 들여다보는 서버 함수가 없어(CR02 · CR16 — 남은 연동) 준비 중인 대기실 · 이미 출발한 러닝은 입구에 보이지 않는다.
 * 알리기 · 공유는 보낸 뒤에만 "크루 채팅 보기"로 그 크루의 채팅방에 간다(보내기 전에 옮기지 않는다).
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
    val tag = when (ui) {
        CrewEntryUi.Loading -> "crew-entry-loading"
        CrewEntryUi.SignIn -> "crew-entry-sign-in"
        is CrewEntryUi.Failed -> "crew-entry-failed"
        CrewEntryUi.NoCrew -> "crew-entry-none"
        is CrewEntryUi.Ready -> "crew-entry"
    }
    CrewPage(
        title = stringResource(R.string.run_crew_title), subtitle = stringResource(R.string.run_crew_entry_sub), onBack = onBack,
        badge = false, headerIcon = true, modifier = Modifier.testTag(tag),
        bottom = {
            when (ui) {
                CrewEntryUi.Loading -> Unit
                CrewEntryUi.SignIn ->
                    FreeRunButton(stringResource(R.string.run_cr_sign_in), onSignIn, Modifier.fillMaxWidth().testTag("crew-entry-sign-in-go"))
                is CrewEntryUi.Failed ->
                    FreeRunButton(stringResource(R.string.run_rec_reload), onRetry, Modifier.fillMaxWidth().testTag("crew-entry-retry"))
                // CR14 — 찾기 · 만들기는 기존 크루 기능으로, 자유 러닝은 바로
                CrewEntryUi.NoCrew -> {
                    FreeRunButton(stringResource(R.string.run_cr_find), onFindCrew, Modifier.fillMaxWidth().testTag("crew-entry-find"))
                    FreeRunButton(stringResource(R.string.run_cr_create), onCreateCrew, Modifier.fillMaxWidth().testTag("crew-entry-create"),
                        kind = FreeButtonKind.Secondary)
                    CrewTextAction(stringResource(R.string.run_cr_free), onFreeRun, Modifier.fillMaxWidth().testTag("crew-entry-free"),
                        center = true)
                }
                // 대기실은 눌러야만 열린다
                is CrewEntryUi.Ready -> {
                    FreeRunButton(stringResource(R.string.run_cr_enter), { onEnter(ui.crew.id) },
                        Modifier.fillMaxWidth().testTag("crew-entry-enter"))
                    if (ui.crews.size > 1) {
                        FreeRunButton(stringResource(R.string.run_crew_pick_other), onChange, Modifier.fillMaxWidth().testTag("crew-entry-change"),
                            kind = FreeButtonKind.Secondary)
                    }
                }
            }
        },
    ) {
        val c = crewColors()
        when (ui) {
            CrewEntryUi.Loading -> Box(Modifier.fillMaxWidth().padding(top = 96.dp), contentAlignment = Alignment.Center) {
                RunSpinner(Modifier.size(40.dp))
            }
            CrewEntryUi.SignIn -> CrewEmpty(Icons.Outlined.PeopleAlt, stringResource(R.string.run_cr_sign_in_title), null)
            is CrewEntryUi.Failed -> CrewEmpty(Icons.Outlined.CloudOff, stringResource(R.string.run_cr_load_failed), null)
            CrewEntryUi.NoCrew -> CrewEmpty(
                Icons.Outlined.PeopleAlt, stringResource(R.string.run_crew_none_title), stringResource(R.string.run_crew_none_body),
            )
            is CrewEntryUi.Ready -> {
                Spacer(Modifier.height(18.dp))
                CrewTitleRow(ui.crew.name, ui.crew.monogram, ui.crew.tagline.takeIf { it.isNotBlank() })
                Spacer(Modifier.height(30.dp))
                Text(
                    stringResource(R.string.run_crew_entry_title),
                    style = TextStyle(fontFamily = StepUpSans, fontWeight = FontWeight.ExtraBold, fontSize = 27.sp, lineHeight = 1.35.em,
                        letterSpacing = (-0.05).em, color = c.text),
                    modifier = Modifier.semantics { heading() },
                )
                Spacer(Modifier.height(8.dp))
                Text(stringResource(R.string.run_cr_enter_body), style = runTextStyle(15.sp, c.label, FontWeight.Medium, 1.6f),
                    modifier = Modifier.testTag("crew-entry-card"))
                Spacer(Modifier.height(6.dp))
                Text(stringResource(R.string.run_cr_enter_note), style = runTextStyle(13.sp, c.muted, FontWeight.Medium, 1.5f))
                Spacer(Modifier.height(18.dp))
                CrewLine()
                Spacer(Modifier.height(14.dp))
                Row(Modifier.fillMaxWidth()) {
                    Column(Modifier.weight(1f)) {
                        Text(stringResource(R.string.run_crew_members_label), style = runTextStyle(13.sp, c.label, FontWeight.Medium))
                        Text(stringResource(R.string.run_crew_people, ui.crew.memberCount),
                            style = runTextStyle(22.sp, c.text, FontWeight.ExtraBold, 1.25f))
                    }
                    ui.crew.area.takeIf { it.isNotBlank() }?.let { area ->
                        Column(horizontalAlignment = Alignment.End) {
                            Text(stringResource(R.string.run_crew_area_label), style = runTextStyle(13.sp, c.label, FontWeight.Medium))
                            Text(area, style = runTextStyle(22.sp, c.text, FontWeight.ExtraBold, 1.25f), maxLines = 1,
                                overflow = TextOverflow.Ellipsis)
                        }
                    }
                }
            }
        }
    }
}

/**
 * 크루 화면 틀 — 남색 바탕 · 머리(크루 이름 · 배지) · 내용(넘김) · 아래 버튼. 버튼은 시스템 아래 영역 위에 선다.
 * [headerIcon] 이면 배지 대신 동그란 사람들 아이콘(제목이 이미 "크루 달리기"인 입구).
 */
@Composable
internal fun CrewPage(
    title: String,
    modifier: Modifier = Modifier,
    subtitle: String? = null,
    onBack: (() -> Unit)? = null,
    badge: Boolean = true,
    headerIcon: Boolean = false,
    bottom: (@Composable ColumnScope.() -> Unit)? = null,
    content: @Composable ColumnScope.() -> Unit,
) {
    val c = crewColors()
    Box(modifier.fillMaxSize().background(c.screen)) {
        Column(Modifier.fillMaxSize().safeDrawingPadding()) {
            CrewHeader(
                title, subtitle = subtitle, onBack = onBack, badge = badge,
                trailing = if (headerIcon) {
                    {
                        Box(Modifier.size(44.dp).clip(CircleShape).background(c.badge), contentAlignment = Alignment.Center) {
                            Icon(Icons.Outlined.PeopleAlt, contentDescription = null, tint = c.badgeInk, modifier = Modifier.size(22.dp))
                        }
                    }
                } else null,
            )
            Column(
                Modifier.weight(1f).fillMaxWidth().verticalScroll(rememberScrollState()).padding(horizontal = 20.dp),
                content = content,
            )
            if (bottom != null) {
                Column(
                    Modifier.fillMaxWidth().padding(horizontal = 20.dp).padding(top = 8.dp, bottom = 12.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                    content = bottom,
                )
            }
        }
    }
}

/** 빈 상태 — 둥근 네모 아이콘 · 큰 제목 · 설명(가운데) */
@Composable
private fun CrewEmpty(icon: androidx.compose.ui.graphics.vector.ImageVector, title: String, body: String?) {
    val c = crewColors()
    Column(Modifier.fillMaxWidth().padding(top = 36.dp), horizontalAlignment = Alignment.CenterHorizontally) {
        Box(Modifier.size(92.dp).clip(RoundedCornerShape(28.dp)).background(c.inset), contentAlignment = Alignment.Center) {
            Icon(icon, contentDescription = null, tint = c.link, modifier = Modifier.size(46.dp))
        }
        Spacer(Modifier.height(24.dp))
        Text(
            title,
            style = TextStyle(fontFamily = StepUpSans, fontWeight = FontWeight.ExtraBold, fontSize = 26.sp, lineHeight = 1.35.em,
                letterSpacing = (-0.04).em, color = c.text),
            textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth().semantics { heading() },
        )
        if (body != null) {
            Spacer(Modifier.height(10.dp))
            Text(body, style = runTextStyle(15.sp, c.label, FontWeight.Medium, 1.6f), textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth())
        }
    }
}

/** 크루 이름 줄 — 크루 표시(이니셜) · 큰 이름 · (한 줄 소개) */
@Composable
internal fun CrewTitleRow(name: String, monogram: String, tagline: String? = null) {
    val c = crewColors()
    Row(verticalAlignment = Alignment.CenterVertically) {
        CrewMark(monogram.ifBlank { initialsOf(name) }, 64.dp)
        Spacer(Modifier.width(14.dp))
        Column(Modifier.weight(1f)) {
            Text(
                name,
                style = TextStyle(fontFamily = StepUpSans, fontWeight = FontWeight.ExtraBold, fontSize = 25.sp, letterSpacing = (-0.03).em,
                    color = c.text),
                maxLines = 2, overflow = TextOverflow.Ellipsis,
                modifier = Modifier.semantics { heading() }.testTag("crew-title"),
            )
            if (tagline != null) {
                Text(tagline, style = runTextStyle(14.sp, c.label, FontWeight.Medium), maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
        }
    }
}

/** 내 크루 선택(CR19) — 가입한 크루 중 하나. 고르기만 하고 대기실은 열지 않는다 */
@Composable
internal fun CrewPickSheet(crews: List<Crew>, selected: String, onApply: (String) -> Unit, onDismiss: () -> Unit) {
    val c = crewColors()
    var pending by rememberSaveable { mutableStateOf(selected) }
    RunSheet(onDismiss = onDismiss, modifier = Modifier.testTag("crew-pick-sheet")) {
        CrewSheetTitle(stringResource(R.string.run_crew_pick_title))
        Spacer(Modifier.height(14.dp))
        Column(
            Modifier.fillMaxWidth().heightIn(max = 340.dp).verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            crews.forEach { crew ->
                val on = pending == crew.id
                val shape = RoundedCornerShape(14.dp)
                Row(
                    Modifier.fillMaxWidth().heightIn(min = 72.dp).clip(shape).background(if (on) c.selectedFace else c.card, shape)
                        .border(1.dp, if (on) c.selectedEdge else c.line, shape)
                        .feedbackClickable(role = Role.RadioButton, cue = FeedbackCue.Select, onClick = { pending = crew.id })
                        .semantics { this.selected = on }.padding(horizontal = 14.dp, vertical = 12.dp).testTag("crew-pick-${crew.id}"),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    CrewMark(crew.monogram.ifBlank { initialsOf(crew.name) }, 44.dp)
                    Spacer(Modifier.width(12.dp))
                    Column(Modifier.weight(1f)) {
                        Text(crew.name, style = runTextStyle(16.sp, c.text, FontWeight.Bold), maxLines = 1, overflow = TextOverflow.Ellipsis)
                        Text(
                            crew.area.takeIf { it.isNotBlank() }?.let { stringResource(R.string.run_crew_pick_line, it, crew.memberCount) }
                                ?: stringResource(R.string.run_cr_members, crew.memberCount),
                            style = runTextStyle(13.sp, c.label, FontWeight.Medium), maxLines = 1, overflow = TextOverflow.Ellipsis,
                        )
                    }
                    Spacer(Modifier.width(10.dp))
                    Box(
                        Modifier.size(20.dp).clip(CircleShape)
                            .then(if (on) Modifier.border(5.dp, c.selectedEdge, CircleShape).background(androidx.compose.ui.graphics.Color.White)
                            else Modifier.border(2.dp, c.muted, CircleShape)),
                    )
                }
            }
        }
        Spacer(Modifier.height(16.dp))
        FreeRunButton(stringResource(R.string.run_crew_pick_apply), { onApply(pending) }, Modifier.fillMaxWidth().testTag("crew-pick-apply"))
    }
}

/**
 * 준비 전 확인(CR17) — 위치 사용(OS 권한)과 크루원에게 위치 공유(처음엔 끔 · 고른 대로 기억)는 따로. 허용은 OS 창을 띄울 뿐이고,
 * 돌아오면 다시 읽어 걸음 권한이 있으면 준비한다(위치 없이도 준비할 수 있다). 나중에는 준비 전으로 남는다.
 * [v2] 가 false 면 예전 모습(번개러닝 대기실 — 번개 디자인은 그대로 둔다).
 */
@Composable
internal fun CrewReadyCheckSheet(
    locationAllowed: Boolean,
    share: Boolean,
    onShare: (Boolean) -> Unit,
    onAllow: () -> Unit,
    onLater: () -> Unit,
    v2: Boolean = true,
) {
    RunSheet(onDismiss = onLater, modifier = Modifier.testTag("crew-ready-check"), showClose = false) {
        if (v2) CrewReadyCheckBody(locationAllowed, share, onShare, onAllow, onLater)
        else FlashReadyCheckBody(locationAllowed, share, onShare, onAllow, onLater)
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
    val c = crewColors()
    CrewSheetTitle(stringResource(R.string.run_crew_check_title), body = stringResource(R.string.run_crew_check_body))
    Spacer(Modifier.height(16.dp))
    CrewCard(padding = 16.dp) {
        Row(Modifier.padding(vertical = 10.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Outlined.LocationOn, contentDescription = null, tint = c.text, modifier = Modifier.size(22.dp))
            Spacer(Modifier.width(8.dp))
            Text(stringResource(R.string.run_cr_check_location), style = runTextStyle(16.sp, c.text, FontWeight.SemiBold),
                modifier = Modifier.weight(1f))
            Text(
                stringResource(if (locationAllowed) R.string.run_cr_check_allowed else R.string.run_cr_check_needed),
                style = runTextStyle(14.sp, if (locationAllowed) c.ready else freeRunColors().dangerText, FontWeight.Bold),
                modifier = Modifier.testTag("crew-ready-check-location"),
            )
        }
        Text(stringResource(R.string.run_crew_check_location_body), style = runTextStyle(13.sp, c.label, FontWeight.Normal, 1.45f),
            modifier = Modifier.padding(bottom = 12.dp))
    }
    Spacer(Modifier.height(8.dp))
    CrewToggleRow(
        stringResource(R.string.run_crew_check_share), stringResource(R.string.run_crew_check_share_body), share, onShare,
        "crew-ready-check-share",
    )
    Text(stringResource(R.string.run_crew_check_note), style = runTextStyle(13.sp, c.muted, FontWeight.Normal, 1.45f),
        modifier = Modifier.fillMaxWidth().padding(top = 4.dp))
    Spacer(Modifier.height(16.dp))
    FreeRunButton(stringResource(R.string.run_cr_check_allow), onAllow, Modifier.fillMaxWidth().testTag("crew-ready-check-allow"))
    Spacer(Modifier.height(10.dp))
    FreeRunButton(stringResource(R.string.run_perm_later), onLater, Modifier.fillMaxWidth().testTag("crew-ready-check-later"),
        kind = FreeButtonKind.Secondary)
}

/** 번개러닝 대기실의 준비 전 확인 — 예전 모습 그대로(번개 디자인은 바꾸지 않는다) */
@Composable
private fun ColumnScope.FlashReadyCheckBody(
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

/**
 * 준비한 사람끼리 출발(CR07) — 준비 완료 · 준비 전 인원은 지금 방 상태 그대로(출발은 서버가 명단을 정한다).
 * [v2] 가 false 면 예전 모습(번개러닝 대기실).
 */
@Composable
internal fun CrewStartConfirmSheet(ready: Int, notReady: Int, onGo: () -> Unit, onWait: () -> Unit, v2: Boolean = true) {
    RunSheet(onDismiss = onWait, modifier = Modifier.testTag("crew-start-confirm"), showClose = false) {
        if (v2) CrewStartConfirmBody(ready, notReady, onGo, onWait) else FlashStartConfirmBody(ready, notReady, onGo, onWait)
    }
}

@Composable
internal fun ColumnScope.CrewStartConfirmBody(ready: Int, notReady: Int, onGo: () -> Unit, onWait: () -> Unit) {
    val c = crewColors()
    CrewSheetTitle(stringResource(R.string.run_crew_go_title, ready), body = stringResource(R.string.run_crew_go_body, notReady))
    Row(Modifier.fillMaxWidth().padding(vertical = 18.dp)) {
        for ((value, label, tag, accent) in listOf(
            Quad("$ready", stringResource(R.string.run_cr_ready), "crew-start-ready", true),
            Quad("$notReady", stringResource(R.string.run_cr_not_ready), "crew-start-not-ready", false),
        )) {
            Column(Modifier.weight(1f), horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    value,
                    style = TextStyle(fontFamily = StepUpSans, fontWeight = FontWeight.ExtraBold, fontSize = 40.sp,
                        color = if (accent) c.goal else c.text, letterSpacing = (-0.05).em),
                    modifier = Modifier.testTag(tag),
                )
                Text(label, style = runTextStyle(14.sp, c.label, FontWeight.Medium))
            }
        }
    }
    FreeRunButton(stringResource(R.string.run_crew_go_n, ready), onGo, Modifier.fillMaxWidth().testTag("crew-start-go"))
    Spacer(Modifier.height(10.dp))
    FreeRunButton(stringResource(R.string.run_crew_wait), onWait, Modifier.fillMaxWidth().testTag("crew-start-wait"),
        kind = FreeButtonKind.Secondary)
}

private data class Quad(val value: String, val label: String, val tag: String, val accent: Boolean)

/** 번개러닝 대기실의 출발 확인 — 예전 모습 그대로 */
@Composable
private fun ColumnScope.FlashStartConfirmBody(ready: Int, notReady: Int, onGo: () -> Unit, onWait: () -> Unit) {
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
 * 보낸 뒤에만 [onOpenChat](그 크루의 채팅방)을 보인다 — null 이면 채팅으로 가지 않는다(곧 출발할 수 있는 준비 완료 참가자).
 */
@Composable
internal fun CrewNotifySheet(
    crewId: String,
    crewName: String,
    waiting: Int,
    onDismiss: () -> Unit,
    onOpenChat: (() -> Unit)? = null,
) {
    val chat = ServiceLocator.crewChat
    val scope = rememberCoroutineScope()
    val body = stringResource(R.string.run_cr_notify_message, crewName, waiting)
    // 보냈음은 화면을 돌려도 남긴다(다시 보내지 않게). 보내는 중에 화면이 다시 만들어졌으면 결과를 모르니 실패로 두고 다시 보내게 한다
    var state by rememberSaveable { mutableStateOf(CrewSend.Idle) }
    LaunchedEffect(Unit) { if (state == CrewSend.Sending) state = CrewSend.Failed }
    var pending by rememberSaveable(stateSaver = com.stepup.android.ui.screens.community.chat.ChatOutgoingSaver) { mutableStateOf<ChatMessage?>(null) }
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
            onOpenChat = onOpenChat?.let { open -> { onDismiss(); open() } },
        )
    }
}

@Composable
internal fun ColumnScope.CrewNotifyBody(
    crewName: String,
    text: String,
    state: CrewSend,
    onSend: () -> Unit,
    onCancel: () -> Unit,
    onOpenChat: (() -> Unit)? = null,
) {
    val c = crewColors()
    CrewSheetTitle(stringResource(R.string.run_cr_notify_title), body = stringResource(R.string.run_crew_notify_body, crewName))
    Spacer(Modifier.height(14.dp))
    CrewCard(Modifier.testTag("crew-notify-preview"), padding = 16.dp) {
        Spacer(Modifier.height(8.dp))
        Text(crewName, style = runTextStyle(14.sp, c.label, FontWeight.Medium), maxLines = 1, overflow = TextOverflow.Ellipsis)
        Spacer(Modifier.height(6.dp))
        Text(text, style = runTextStyle(15.sp, c.text, FontWeight.Medium, 1.55f), modifier = Modifier.testTag("crew-notify-text"))
        Spacer(Modifier.height(10.dp))
    }
    when (state) {
        CrewSend.Sent -> CrewNote(stringResource(R.string.run_cr_notify_sent), Modifier.padding(top = 10.dp),
            icon = Icons.Outlined.CheckCircle, tag = "crew-notify-sent")
        CrewSend.Failed -> CrewError(stringResource(R.string.run_cr_send_failed), Modifier.padding(top = 10.dp), tag = "crew-notify-failed")
        else -> Unit
    }
    Spacer(Modifier.height(16.dp))
    if (state == CrewSend.Sent) {
        if (onOpenChat != null) {
            FreeRunButton(stringResource(R.string.run_crew_open_chat), onOpenChat, Modifier.fillMaxWidth().testTag("crew-notify-chat"),
                icon = Icons.AutoMirrored.Outlined.Chat)
            Spacer(Modifier.height(10.dp))
        }
        FreeRunButton(stringResource(R.string.common_close), onCancel, Modifier.fillMaxWidth().testTag("crew-notify-close"),
            kind = if (onOpenChat != null) FreeButtonKind.Secondary else FreeButtonKind.Primary)
    } else {
        FreeRunButton(stringResource(R.string.run_cr_notify_send), onSend, Modifier.fillMaxWidth().testTag("crew-notify-send"),
            busy = state == CrewSend.Sending)
        Spacer(Modifier.height(10.dp))
        FreeRunButton(stringResource(R.string.common_cancel), onCancel, Modifier.fillMaxWidth().testTag("crew-notify-cancel"),
            kind = FreeButtonKind.Secondary, enabled = state != CrewSend.Sending)
    }
}

/**
 * 크루에 기록 공유(CR21) — 내 기록(거리 · 시간 · 페이스) · 보낼 곳(그 러닝의 크루 채팅) · 경로 포함(처음엔 끔). 미리보기는 보낼 그대로.
 * "공유하기"를 눌러야만 보낸다. 경로를 켜면 공유 카드 그림(지도 · 경로)을 함께 보내고, 끄면 그림 없이 글만 보낸다.
 * 실패하면 같은 내용으로 다시 보낸다. 보낸 뒤에만 "크루 채팅 보기"([onOpenChat]).
 */
@Composable
internal fun CrewShareScreen(
    crewId: String,
    crewName: String,
    monogram: String,
    km: String,
    time: String,
    pace: String,
    card: suspend (Boolean) -> Bitmap?,
    hasRoute: Boolean,
    onClose: () -> Unit,
    onOpenChat: (() -> Unit)? = null,
) {
    val chat = ServiceLocator.crewChat
    val scope = rememberCoroutineScope()
    var includeRoute by rememberSaveable { mutableStateOf(false) }
    var preview by remember { mutableStateOf<Bitmap?>(null) }
    var state by rememberSaveable { mutableStateOf(CrewSend.Idle) }
    // 보낸 것(요청 키 · 그림)은 다시 보낼 때 그대로 — 경로를 바꾸면 새 메시지로
    var pending by rememberSaveable(stateSaver = com.stepup.android.ui.screens.community.chat.ChatOutgoingPhotoSaver) { mutableStateOf<Pair<ChatMessage, ChatPhoto?>?>(null) }
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
        crewName = crewName, monogram = monogram, km = km, time = time, pace = pace, message = text, includeRoute = includeRoute,
        hasRoute = hasRoute, preview = preview, state = state,
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
        onOpenChat = onOpenChat,
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
    monogram: String = "",
    onOpenChat: (() -> Unit)? = null,
) {
    val c = crewColors()
    CrewPage(
        title = stringResource(R.string.run_crew_share_title), subtitle = crewName, onBack = onClose, modifier = Modifier.testTag("crew-share"),
        bottom = {
            if (state == CrewSend.Sent) {
                if (onOpenChat != null) {
                    FreeRunButton(stringResource(R.string.run_crew_open_chat), onOpenChat, Modifier.fillMaxWidth().testTag("crew-share-chat"),
                        icon = Icons.AutoMirrored.Outlined.Chat)
                }
                FreeRunButton(stringResource(R.string.common_close), onClose, Modifier.fillMaxWidth().testTag("crew-share-close"),
                    kind = if (onOpenChat != null) FreeButtonKind.Secondary else FreeButtonKind.Primary)
            } else {
                FreeRunButton(stringResource(R.string.run_cr_share_do), onShare, Modifier.fillMaxWidth().testTag("crew-share-send"),
                    icon = Icons.AutoMirrored.Outlined.Send, busy = state == CrewSend.Sending)
                FreeRunButton(stringResource(R.string.common_cancel), onClose, Modifier.fillMaxWidth().testTag("crew-share-cancel"),
                    kind = FreeButtonKind.Secondary, enabled = state != CrewSend.Sending)
            }
        },
    ) {
        Spacer(Modifier.height(10.dp))
        Text(
            stringResource(R.string.run_crew_share_headline),
            style = TextStyle(fontFamily = StepUpSans, fontWeight = FontWeight.ExtraBold, fontSize = 27.sp, lineHeight = 1.35.em,
                letterSpacing = (-0.05).em, color = c.text),
            modifier = Modifier.semantics { heading() },
        )
        Spacer(Modifier.height(16.dp))
        CrewCard(Modifier.testTag("crew-share-record"), padding = 18.dp) {
            Spacer(Modifier.height(10.dp))
            Text(stringResource(R.string.run_crew_share_my), style = runTextStyle(14.sp, c.label, FontWeight.Medium))
            FitNumber(km, "km", valueSize = 52.sp, unitSize = 22.sp, valueColor = c.text, unitColor = c.label,
                modifier = Modifier.fillMaxWidth(), valueTag = "crew-share-km")
            Spacer(Modifier.height(8.dp))
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Text(stringResource(R.string.stat_time), style = runTextStyle(14.sp, c.label, FontWeight.Medium))
                Spacer(Modifier.width(8.dp))
                Text(time, style = runTextStyle(16.sp, c.text, FontWeight.Bold), modifier = Modifier.weight(1f))
                Text(stringResource(R.string.run_label_pace), style = runTextStyle(14.sp, c.label, FontWeight.Medium))
                Spacer(Modifier.width(8.dp))
                Text("$pace /km", style = runTextStyle(16.sp, c.text, FontWeight.Bold))
            }
            Spacer(Modifier.height(12.dp))
        }
        Spacer(Modifier.height(16.dp))
        Row(Modifier.fillMaxWidth().testTag("crew-share-target"), verticalAlignment = Alignment.CenterVertically) {
            CrewMark(monogram.ifBlank { initialsOf(crewName) }, 44.dp)
            Spacer(Modifier.width(12.dp))
            Column {
                Text(crewName, style = runTextStyle(16.sp, c.text, FontWeight.Bold), maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text(stringResource(R.string.run_crew_share_target), style = runTextStyle(13.sp, c.label))
            }
        }
        Spacer(Modifier.height(10.dp))
        CrewToggleRow(
            stringResource(R.string.run_crew_share_route), stringResource(R.string.run_crew_share_route_body), includeRoute, onRoute,
            "crew-share-route-toggle", Modifier.testTag("crew-share-route"), enabled = hasRoute && state != CrewSend.Sent,
        )
        Spacer(Modifier.height(8.dp))
        // 미리보기 — 보낼 글과(경로를 켜면) 그림
        CrewCard(Modifier.testTag("crew-share-preview"), padding = 18.dp) {
            Spacer(Modifier.height(10.dp))
            Text(stringResource(R.string.run_crew_share_preview), style = runTextStyle(13.sp, c.label, FontWeight.Medium))
            Spacer(Modifier.height(6.dp))
            Text(message, style = runTextStyle(15.sp, c.text, FontWeight.Medium, 1.55f), modifier = Modifier.testTag("crew-share-text"))
            val image = preview
            if (includeRoute && image != null) {
                Spacer(Modifier.height(10.dp))
                Image(
                    image.asImageBitmap(), contentDescription = null, contentScale = ContentScale.Fit,
                    modifier = Modifier.fillMaxWidth().height(220.dp).clip(RoundedCornerShape(12.dp)).testTag("crew-share-image"),
                )
            }
            Spacer(Modifier.height(12.dp))
        }
        when (state) {
            CrewSend.Sent -> CrewNote(stringResource(R.string.run_cr_shared), Modifier.padding(top = 12.dp), icon = Icons.Outlined.CheckCircle,
                tag = "crew-share-sent")
            CrewSend.Failed -> CrewError(stringResource(R.string.run_cr_send_failed), Modifier.padding(top = 12.dp), tag = "crew-share-failed")
            else -> Unit
        }
        Spacer(Modifier.height(12.dp))
    }
}
