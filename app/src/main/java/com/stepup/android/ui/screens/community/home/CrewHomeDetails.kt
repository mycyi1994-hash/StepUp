package com.stepup.android.ui.screens.community.home

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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.LifecycleResumeEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.stepup.android.R
import com.stepup.android.domain.CrewCard
import com.stepup.android.domain.CrewHomeRules
import com.stepup.android.domain.CrewMember
import com.stepup.android.domain.CrewPersonRole
import com.stepup.android.domain.CrewRun
import com.stepup.android.ui.screens.community.chat.ChatFace
import com.stepup.android.ui.screens.community.chat.ChatRetryState
import com.stepup.android.ui.screens.community.crew.CrewAvatar
import com.stepup.android.ui.screens.community.crew.CrewBottomBar
import com.stepup.android.ui.screens.community.crew.CrewButton
import com.stepup.android.ui.screens.community.crew.CrewGutter
import com.stepup.android.ui.screens.community.crew.CrewImage
import com.stepup.android.ui.screens.community.crew.CrewLevelChip
import com.stepup.android.ui.screens.community.crew.CrewLoad
import com.stepup.android.ui.screens.community.crew.CrewPage
import com.stepup.android.ui.screens.community.crew.CrewScreenViewModel
import com.stepup.android.ui.screens.community.crew.CrewSkeletonBox
import com.stepup.android.ui.screens.community.crew.CrewTopBar
import com.stepup.android.ui.screens.community.crew.crewInk
import com.stepup.android.ui.screens.community.crew.leaderFace
import com.stepup.android.ui.screens.community.crew.rememberCrewWords

// ─────────────────────────────────────────────────────────────
// 01 크루 소개
// ─────────────────────────────────────────────────────────────

/** 01 크루 소개 — 대표 이미지(누르면 02) · 이름 · Lv · 소개 · 활동 지역 · 정기 러닝 · 거리와 속도 · 크루장. 없는 값의 줄은 두지 않는다 */
@Composable
fun CrewIntroScreen(
    viewModel: CrewScreenViewModel,
    onBack: () -> Unit,
    onImage: () -> Unit,
    onLeader: (String) -> Unit,
    onMembers: () -> Unit,
) {
    val ink = crewInk()
    val words = rememberCrewWords()
    val card by viewModel.card.collectAsStateWithLifecycle()
    CrewPage(Modifier.testTag("home-intro")) {
        CrewTopBar(stringResource(R.string.crewhome_intro_bar), onBack)
        val crew = card
        if (crew == null) {
            Column(Modifier.weight(1f).padding(horizontal = CrewGutter)) {
                Spacer(Modifier.height(20.dp))
                CrewSkeletonBox(Modifier.fillMaxWidth().height(268.dp), 24.dp)
            }
            return@CrewPage
        }
        Column(Modifier.weight(1f).fillMaxWidth().verticalScroll(rememberScrollState()).padding(horizontal = CrewGutter)) {
            Spacer(Modifier.height(18.dp))
            Box(Modifier.fillMaxWidth().homeClickable(onClick = onImage).testTag("home-intro-image")) {
                CrewImage(crew, 268.dp, 24.dp, Modifier.fillMaxWidth().height(268.dp))
            }
            Spacer(Modifier.height(28.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    crew.name, color = ink.text, fontSize = 32.sp, lineHeight = 38.sp, fontWeight = FontWeight.Bold,
                    modifier = Modifier.weight(1f), maxLines = 2, overflow = TextOverflow.Ellipsis,
                )
                CrewLevelChip(crew.level)
            }
            if (crew.tagline.isNotBlank()) {
                Spacer(Modifier.height(14.dp))
                Text(crew.tagline, color = ink.secondary, fontSize = 15.sp, lineHeight = 25.sp)
            }
            Spacer(Modifier.height(20.dp))
            crew.area.takeIf { it.isNotBlank() }?.let { HomeValueRow(stringResource(R.string.crewhome_intro_area), it, Modifier.testTag("home-intro-area")) }
            if (!crew.schedule.none) HomeValueRow(stringResource(R.string.crewhome_intro_schedule), words.scheduleLong(crew.schedule))
            words.onceLine(crew)?.let { HomeValueRow(stringResource(R.string.crewhome_intro_pace), it) }
            Row(
                Modifier.fillMaxWidth().heightIn(min = 72.dp).homeClickable { onLeader(crew.leaderId) }.padding(vertical = 14.dp)
                    .testTag("home-intro-leader"),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                CrewAvatar(crew.leaderName, 36.dp, leaderFace(crew), ink.text)
                Column(Modifier.weight(1f).padding(horizontal = 16.dp)) {
                    Text(stringResource(R.string.crew_leader_named, crew.leaderName), color = ink.text, fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
                    if (crew.leaderNote.isNotBlank()) {
                        Spacer(Modifier.height(6.dp))
                        Text(crew.leaderNote, color = ink.secondary, fontSize = 12.5.sp, maxLines = 2, overflow = TextOverflow.Ellipsis)
                    }
                }
                Icon(Icons.Filled.ChevronRight, null, tint = ink.secondary, modifier = Modifier.size(20.dp))
            }
            Spacer(Modifier.height(24.dp))
        }
        CrewBottomBar {
            CrewButton(stringResource(R.string.crewhome_intro_members, crew.memberCount), onMembers, Modifier.testTag("home-intro-members"))
        }
    }
}

// ─────────────────────────────────────────────────────────────
// 03 크루 레벨 — 승급 기준은 정해지지 않았다(계산하지 않는다)
// ─────────────────────────────────────────────────────────────

@Composable
fun CrewLevelScreen(viewModel: CrewScreenViewModel, onBack: () -> Unit, onWeekly: () -> Unit) {
    val ink = crewInk()
    val card by viewModel.card.collectAsStateWithLifecycle()
    val level by viewModel.level.collectAsStateWithLifecycle()
    LaunchedEffect(viewModel) { viewModel.loadLevel() }
    CrewPage(Modifier.testTag("home-level-page")) {
        CrewTopBar(stringResource(R.string.crewhome_level_bar), onBack)
        val crew = card
        Column(Modifier.weight(1f).fillMaxWidth().verticalScroll(rememberScrollState()).padding(horizontal = CrewGutter)) {
            Spacer(Modifier.height(20.dp))
            if (crew != null) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    CrewImage(crew, 76.dp, 16.dp)
                    Column(Modifier.padding(start = 20.dp)) {
                        Text(crew.name, color = ink.text, fontSize = 24.sp, fontWeight = FontWeight.SemiBold, maxLines = 2, overflow = TextOverflow.Ellipsis)
                        Spacer(Modifier.height(6.dp))
                        Text(stringResource(R.string.crewhome_level_sub), color = ink.secondary, fontSize = 13.sp)
                    }
                }
            }
            Spacer(Modifier.height(34.dp))
            Text(stringResource(R.string.crewhome_level_now), color = ink.secondary, fontSize = 14.sp)
            // 저장된 레벨(없으면 새 크루) — 서버 값이 기준이다
            val info = (level as? CrewLoad.Ready)?.value
            val current = info?.level ?: crew?.level
            Text(
                if (current != null) stringResource(R.string.crew_level_value, current) else stringResource(R.string.crew_level_new),
                color = ink.text, fontSize = 64.sp, lineHeight = 76.sp, fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(top = 8.dp).testTag("home-level-now"),
            )
            Spacer(Modifier.height(18.dp))
            Column(
                Modifier.fillMaxWidth().clip(RoundedCornerShape(20.dp)).background(ink.card).padding(horizontal = 24.dp, vertical = 22.dp)
                    .testTag("home-level-next"),
            ) {
                HomeLabel(stringResource(R.string.crewhome_level_next))
                Spacer(Modifier.height(14.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    if (current != null) {
                        Text(stringResource(R.string.crew_level_value, current + 1), color = ink.text, fontSize = 26.sp, fontWeight = FontWeight.SemiBold)
                    }
                    Spacer(Modifier.weight(1f))
                    Text(stringResource(R.string.crewhome_level_pending), color = ink.secondary, fontSize = 13.sp)
                }
                Spacer(Modifier.height(14.dp))
                Text(stringResource(R.string.crewhome_level_pending_body), color = ink.secondary, fontSize = 13.sp, lineHeight = 20.sp)
            }
            Spacer(Modifier.height(36.dp))
            Text(stringResource(R.string.crewhome_level_week), color = ink.text, fontSize = 19.sp, fontWeight = FontWeight.SemiBold)
            Spacer(Modifier.height(18.dp))
            val weekKm = info?.weekKm ?: crew?.weekKm
            val runners = info?.weekRunners ?: crew?.weekRunners
            Row {
                Column(Modifier.weight(1f)) {
                    Text(
                        if (weekKm != null) stringResource(R.string.crewhome_week_km, km(weekKm)) else "—",
                        color = ink.text, fontSize = 28.sp, fontWeight = FontWeight.SemiBold, modifier = Modifier.testTag("home-level-km"),
                    )
                    Spacer(Modifier.height(6.dp))
                    Text(stringResource(R.string.crewhome_level_week_km), color = ink.secondary, fontSize = 12.5.sp)
                }
                Column(Modifier.weight(1f)) {
                    Text(
                        if (runners != null) stringResource(R.string.crewhome_level_runners_value, runners) else "—",
                        color = ink.text, fontSize = 28.sp, fontWeight = FontWeight.SemiBold,
                    )
                    Spacer(Modifier.height(6.dp))
                    Text(stringResource(R.string.crewhome_level_week_runners), color = ink.secondary, fontSize = 12.5.sp)
                }
            }
            Spacer(Modifier.height(28.dp))
        }
        CrewBottomBar { CrewButton(stringResource(R.string.crewhome_level_weekly), onWeekly, Modifier.testTag("home-level-weekly")) }
    }
}

// ─────────────────────────────────────────────────────────────
// 04 크루원 · 27 이름 검색
// ─────────────────────────────────────────────────────────────

/** 04 크루원 — 서버 목록 전체를 이어 보인다(크루장 먼저). 27 이름 검색은 같은 자리에서 결과를 바꾼다 */
@Composable
fun CrewMembersHomeScreen(viewModel: CrewHomeMembersViewModel, onBack: () -> Unit, onOpen: (member: CrewMember) -> Unit) {
    val ink = crewInk()
    val card by viewModel.card.collectAsStateWithLifecycle()
    val roster by viewModel.roster.collectAsStateWithLifecycle()
    val me by viewModel.me.collectAsStateWithLifecycle()
    val query by viewModel.query.collectAsStateWithLifecycle()
    LifecycleResumeEffect(viewModel) {
        viewModel.load()
        onPauseOrDispose {}
    }
    CrewPage(Modifier.testTag("home-members-page")) {
        CrewTopBar(stringResource(R.string.crewhome_members_bar), onBack)
        when (val state = roster) {
            is HomeLoad.Ready -> {
                val all = state.value
                val searching = query.isNotBlank()
                val shown = remember(all, query) { all.filter { CrewHomeRules.matches(it.name, query) } }
                LazyColumn(
                    Modifier.weight(1f).fillMaxWidth().testTag("home-members-list"),
                    contentPadding = PaddingValues(start = CrewGutter, end = CrewGutter, bottom = 40.dp),
                ) {
                    item(key = "head") {
                        Column {
                            Spacer(Modifier.height(20.dp))
                            Text(
                                stringResource(R.string.crewhome_members_title, all.size), color = ink.text, fontSize = 27.sp, fontWeight = FontWeight.SemiBold,
                                modifier = Modifier.testTag("home-members-title"),
                            )
                            card?.let { crew ->
                                Spacer(Modifier.height(10.dp))
                                Text(
                                    crew.capacity?.let { stringResource(R.string.crewhome_members_sub, crew.name, it) } ?: crew.name,
                                    color = ink.secondary, fontSize = 13.sp,
                                )
                            }
                            Spacer(Modifier.height(26.dp))
                            HomeSearchField(query, viewModel::setQuery, stringResource(R.string.crewhome_members_search), Modifier.testTag("home-members-search"))
                            Spacer(Modifier.height(if (searching) 24.dp else 14.dp))
                            if (searching && shown.isNotEmpty()) {
                                Text(stringResource(R.string.crewhome_members_found, shown.size), color = ink.secondary, fontSize = 13.sp, modifier = Modifier.testTag("home-members-found"))
                                Spacer(Modifier.height(8.dp))
                            }
                        }
                    }
                    if (searching && shown.isEmpty()) {
                        item(key = "none") {
                            Column(Modifier.fillMaxWidth().padding(top = 48.dp).testTag("home-members-none"), horizontalAlignment = Alignment.CenterHorizontally) {
                                Text(stringResource(R.string.crewhome_members_none_title), color = ink.text, fontSize = 18.sp, fontWeight = FontWeight.SemiBold)
                                Spacer(Modifier.height(10.dp))
                                Text(stringResource(R.string.crewhome_members_none_body), color = ink.secondary, fontSize = 13.5.sp)
                            }
                        }
                    }
                    items(shown, key = { it.userId }) { member ->
                        val role = stringResource(if (member.owner) R.string.crewhome_members_owner else R.string.crewhome_members_member)
                        val sub = when {
                            searching -> stringResource(R.string.crewhome_members_found_sub, card?.name.orEmpty(), role).trim()
                            member.userId == me -> stringResource(R.string.crewhome_members_me, role)
                            else -> role
                        }
                        MemberRow(card, member, sub) { onOpen(member) }
                    }
                }
            }
            HomeLoad.Failed, HomeLoad.Missing -> ChatRetryState(
                title = stringResource(R.string.crewhome_members_error),
                body = stringResource(R.string.crewhome_error_body),
                button = stringResource(R.string.crewhome_error_retry),
                onRetry = viewModel::load,
                modifier = Modifier.padding(top = 150.dp),
                tag = "home-members-error",
            )
            HomeLoad.Loading -> Column(Modifier.padding(CrewGutter)) {
                CrewSkeletonBox(Modifier.fillMaxWidth().height(40.dp))
                Spacer(Modifier.height(24.dp))
                repeat(5) {
                    CrewSkeletonBox(Modifier.fillMaxWidth().height(56.dp))
                    Spacer(Modifier.height(14.dp))
                }
            }
        }
    }
}

@Composable
private fun MemberRow(card: CrewCard?, member: CrewMember, sub: String, onClick: () -> Unit) {
    val ink = crewInk()
    Row(
        Modifier.fillMaxWidth().heightIn(min = 76.dp).homeClickable(onClick = onClick).testTag("home-member-${member.userId}"),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (member.owner && card != null) CrewAvatar(member.name, 44.dp, leaderFace(card), ink.text)
        else ChatFace(member.name, owner = false, size = 44.dp)
        Column(Modifier.weight(1f).padding(start = 18.dp)) {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f).padding(vertical = 14.dp)) {
                    Text(member.name, color = ink.text, fontSize = 17.sp, fontWeight = FontWeight.SemiBold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    Spacer(Modifier.height(6.dp))
                    Text(sub, color = if (member.owner) ink.info else ink.secondary, fontSize = 13.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                }
                Icon(Icons.Filled.ChevronRight, null, tint = ink.secondary, modifier = Modifier.size(20.dp))
            }
            HomeDivider()
        }
    }
}

// ─────────────────────────────────────────────────────────────
// 05 크루장 · 06 크루원 프로필 — 공개한 활동만
// ─────────────────────────────────────────────────────────────

/**
 * 05 · 06 — 고른 사람(userId)의 이름 · 이 크루에서의 역할 · 함께한 날 · 가장 최근에 크루로 적은 러닝. 크루장은 크루에 남긴 한마디.
 * 개인 소개 · 주로 뛰는 곳 · 즐겨 뛰는 거리 · 달리는 스타일은 서비스에 개인 프로필 값이 없어 두지 않는다(예시로 채우지 않는다).
 */
@Composable
fun CrewPersonHomeScreen(viewModel: CrewHomePersonViewModel, onBack: () -> Unit, onRun: (CrewRun) -> Unit, onEnded: () -> Unit) {
    val ink = crewInk()
    val card by viewModel.card.collectAsStateWithLifecycle()
    val person by viewModel.person.collectAsStateWithLifecycle()
    val lastRun by viewModel.lastRun.collectAsStateWithLifecycle()
    val ended by viewModel.ended.collectAsStateWithLifecycle()
    LifecycleResumeEffect(viewModel) {
        viewModel.load()
        onPauseOrDispose {}
    }
    LaunchedEffect(ended) { if (ended) onEnded() }
    val current = (person as? HomeLoad.Ready)?.value
    val leader = current?.role == CrewPersonRole.OWNER
    CrewPage(Modifier.testTag(if (leader) "home-person-leader" else "home-person")) {
        CrewTopBar(stringResource(if (leader) R.string.crewhome_person_leader_bar else R.string.crewhome_person_member_bar), onBack)
        when {
            current != null -> Column(Modifier.weight(1f).fillMaxWidth().verticalScroll(rememberScrollState()).padding(horizontal = CrewGutter)) {
                Spacer(Modifier.height(34.dp))
                Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                    val crew = card
                    if (leader && crew != null) CrewAvatar(current.name, 96.dp, leaderFace(crew), ink.text)
                    else ChatFace(current.name, owner = leader, size = 96.dp)
                }
                Spacer(Modifier.height(22.dp))
                Text(
                    current.name, color = ink.text, fontSize = 30.sp, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth().testTag("home-person-name"),
                )
                Spacer(Modifier.height(10.dp))
                val member = current.role == CrewPersonRole.OWNER || current.role == CrewPersonRole.MEMBER
                Text(
                    if (member) stringResource(if (leader) R.string.crewhome_person_leader_of else R.string.crewhome_person_member_of, card?.name.orEmpty())
                    else stringResource(R.string.crewhome_person_gone),
                    color = ink.info, fontSize = 14.sp, fontWeight = FontWeight.SemiBold, textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth(),
                )
                current.joinedAt?.takeIf { member && it > 0 }?.let { joined ->
                    Spacer(Modifier.height(8.dp))
                    Text(
                        stringResource(R.string.crewhome_person_since, homeDate(localDay(joined))), color = ink.secondary, fontSize = 12.5.sp,
                        textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth().testTag("home-person-since"),
                    )
                }
                val note = card?.leaderNote?.takeIf { leader && it.isNotBlank() }
                if (note != null) {
                    Spacer(Modifier.height(40.dp))
                    Text(note, color = ink.text, fontSize = 20.sp, lineHeight = 31.sp, fontWeight = FontWeight.SemiBold, modifier = Modifier.testTag("home-person-note"))
                }
                if (member) {
                    Spacer(Modifier.height(40.dp))
                    HomeDivider()
                    Spacer(Modifier.height(28.dp))
                    Text(stringResource(R.string.crewhome_person_runs), color = ink.text, fontSize = 17.sp, fontWeight = FontWeight.SemiBold)
                    Spacer(Modifier.height(18.dp))
                    when (val runState = lastRun) {
                        is HomeLoad.Ready -> {
                            val run = runState.value
                            if (run == null) {
                                Text(
                                    stringResource(R.string.crewhome_person_runs_none), color = ink.secondary, fontSize = 14.sp,
                                    modifier = Modifier.testTag("home-person-runs-none"),
                                )
                            } else {
                                Row(
                                    Modifier.fillMaxWidth().heightIn(min = 64.dp).homeClickable { onRun(run) }.testTag("home-person-run"),
                                    verticalAlignment = Alignment.CenterVertically,
                                ) {
                                    Column(Modifier.weight(1f)) {
                                        Text(
                                            stringResource(R.string.crewhome_person_run, recordDayLabel(run.endedAt), km(run.km)),
                                            color = ink.text, fontSize = 21.sp, fontWeight = FontWeight.SemiBold,
                                        )
                                        Spacer(Modifier.height(8.dp))
                                        Text(
                                            stringResource(R.string.crewhome_person_run_sub, homeClock(run.endedAt), durationLong(run.durationS)),
                                            color = ink.secondary, fontSize = 12.5.sp,
                                        )
                                    }
                                    Icon(Icons.Filled.ChevronRight, null, tint = ink.secondary, modifier = Modifier.size(20.dp))
                                }
                            }
                        }
                        HomeLoad.Loading -> CrewSkeletonBox(Modifier.fillMaxWidth().height(56.dp))
                        else -> Text(stringResource(R.string.crewhome_person_error), color = ink.secondary, fontSize = 14.sp)
                    }
                }
                Spacer(Modifier.height(32.dp))
            }
            person is HomeLoad.Loading -> Column(Modifier.weight(1f).padding(CrewGutter), horizontalAlignment = Alignment.CenterHorizontally) {
                Spacer(Modifier.height(20.dp))
                CrewSkeletonBox(Modifier.size(96.dp), 48.dp)
                Spacer(Modifier.height(20.dp))
                CrewSkeletonBox(Modifier.width(140.dp).height(30.dp))
            }
            else -> ChatRetryState(
                title = stringResource(R.string.crewhome_person_error),
                body = stringResource(R.string.crewhome_error_body),
                button = stringResource(R.string.crewhome_error_retry),
                onRetry = viewModel::load,
                modifier = Modifier.padding(top = 150.dp),
                tag = "home-person-error",
            )
        }
    }
}
