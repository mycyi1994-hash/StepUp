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
import com.stepup.android.ui.screens.community.chat.BlueSurface
import com.stepup.android.ui.screens.community.chat.blueText
import com.stepup.android.ui.screens.community.chat.BlueBottomBar
import com.stepup.android.ui.screens.community.chat.BlueButton
import com.stepup.android.ui.screens.community.chat.BlueGutter
import com.stepup.android.ui.screens.community.chat.BlueLevelChip
import com.stepup.android.ui.screens.community.chat.BluePage
import com.stepup.android.ui.screens.community.chat.BlueSkeleton
import com.stepup.android.ui.screens.community.chat.BlueTopBar
import com.stepup.android.ui.screens.community.chat.blueInk
import com.stepup.android.domain.CrewCard
import com.stepup.android.domain.CrewHomeRules
import com.stepup.android.domain.CrewMember
import com.stepup.android.domain.CrewPersonRole
import com.stepup.android.domain.CrewRun
import com.stepup.android.ui.screens.community.chat.ChatFace
import com.stepup.android.ui.screens.community.chat.ChatRetryState
import com.stepup.android.ui.screens.community.crew.CrewAvatar
import com.stepup.android.ui.screens.community.crew.CrewImage
import com.stepup.android.ui.screens.community.crew.CrewLoad
import com.stepup.android.ui.screens.community.crew.CrewScreenViewModel
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
    val ink = blueInk()
    val words = rememberCrewWords()
    val card by viewModel.card.collectAsStateWithLifecycle()
    BluePage(Modifier.testTag("home-intro")) {
        BlueTopBar(stringResource(R.string.crewhome_intro_bar), onBack)
        val crew = card
        if (crew == null) {
            Column(Modifier.weight(1f).padding(horizontal = BlueGutter)) {
                Spacer(Modifier.height(20.dp))
                BlueSkeleton(Modifier.fillMaxWidth().height(268.dp), 24.dp)
            }
            return@BluePage
        }
        Column(Modifier.weight(1f).fillMaxWidth().verticalScroll(rememberScrollState()).padding(horizontal = BlueGutter)) {
            Spacer(Modifier.height(8.dp))
            Box(Modifier.fillMaxWidth().homeClickable(onClick = onImage).testTag("home-intro-image")) {
                CrewImage(crew, 268.dp, 20.dp, Modifier.fillMaxWidth().height(250.dp))
            }
            Spacer(Modifier.height(24.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    crew.name, style = blueText(34.sp, ink.text, FontWeight.ExtraBold, 1.2f),
                    modifier = Modifier.weight(1f), maxLines = 2, overflow = TextOverflow.Ellipsis,
                )
                BlueLevelChip(crew.level, large = true)
            }
            if (crew.tagline.isNotBlank()) {
                Spacer(Modifier.height(10.dp))
                Text(crew.tagline, style = blueText(18.sp, ink.secondary, FontWeight.Medium, 1.5f))
            }
            Spacer(Modifier.height(16.dp))
            HomeDivider()
            crew.area.takeIf { it.isNotBlank() }?.let { HomeValueRow(stringResource(R.string.crewhome_intro_area), it, Modifier.testTag("home-intro-area")) }
            if (!crew.schedule.none) HomeValueRow(stringResource(R.string.crewhome_intro_schedule), words.scheduleLong(crew.schedule))
            words.onceLine(crew)?.let { HomeValueRow(stringResource(R.string.crewhome_intro_pace), it) }
            Row(
                Modifier.fillMaxWidth().heightIn(min = 72.dp).homeClickable { onLeader(crew.leaderId) }.padding(vertical = 14.dp)
                    .testTag("home-intro-leader"),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                ChatFace(crew.leaderName, owner = true, size = 50.dp)
                Column(Modifier.weight(1f).padding(horizontal = 16.dp)) {
                    Text(stringResource(R.string.crew_leader_named, crew.leaderName), style = blueText(17.sp, ink.text, FontWeight.Bold))
                    if (crew.leaderNote.isNotBlank()) {
                        Spacer(Modifier.height(4.dp))
                        Text(crew.leaderNote, style = blueText(15.sp, ink.secondary, FontWeight.Medium), maxLines = 2, overflow = TextOverflow.Ellipsis)
                    }
                }
                Icon(Icons.Filled.ChevronRight, null, tint = ink.secondary, modifier = Modifier.size(24.dp))
            }
            Spacer(Modifier.height(24.dp))
        }
        BlueBottomBar {
            BlueButton(stringResource(R.string.crewhome_intro_members, crew.memberCount), onMembers, Modifier.testTag("home-intro-members"))
        }
    }
}

// ─────────────────────────────────────────────────────────────
// 03 크루 레벨 — 승급 기준은 정해지지 않았다(계산하지 않는다)
// ─────────────────────────────────────────────────────────────

@Composable
fun CrewLevelScreen(viewModel: CrewScreenViewModel, onBack: () -> Unit, onWeekly: () -> Unit) {
    val ink = blueInk()
    val card by viewModel.card.collectAsStateWithLifecycle()
    val level by viewModel.level.collectAsStateWithLifecycle()
    LaunchedEffect(viewModel) { viewModel.loadLevel() }
    BluePage(Modifier.testTag("home-level-page")) {
        BlueTopBar(stringResource(R.string.crewhome_level_bar), onBack)
        val crew = card
        Column(Modifier.weight(1f).fillMaxWidth().verticalScroll(rememberScrollState()).padding(horizontal = BlueGutter)) {
            Spacer(Modifier.height(16.dp))
            if (crew != null) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    CrewImage(crew, 76.dp, 16.dp)
                    Column(Modifier.padding(start = 20.dp)) {
                        Text(crew.name, style = blueText(26.sp, ink.text, FontWeight.ExtraBold, 1.25f), maxLines = 2, overflow = TextOverflow.Ellipsis)
                        Spacer(Modifier.height(4.dp))
                        Text(stringResource(R.string.crewhome_level_sub), style = blueText(15.sp, ink.secondary, FontWeight.Medium))
                    }
                }
            }
            Spacer(Modifier.height(30.dp))
            Text(stringResource(R.string.crewhome_level_now), style = blueText(15.sp, ink.secondary, FontWeight.Medium))
            // 저장된 레벨(없으면 새 크루) — 서버 값이 기준이다
            val info = (level as? CrewLoad.Ready)?.value
            val current = info?.level ?: crew?.level
            Text(
                if (current != null) stringResource(R.string.crew_level_value, current) else stringResource(R.string.crew_level_new),
                style = blueText(72.sp, ink.text, FontWeight.ExtraBold, 1.1f),
                modifier = Modifier.padding(top = 4.dp).testTag("home-level-now"),
            )
            Spacer(Modifier.height(18.dp))
            BlueSurface(Modifier.fillMaxWidth().testTag("home-level-next"), selected = true, radius = 20.dp) {
                Column(Modifier.padding(horizontal = 22.dp, vertical = 20.dp)) {
                    Text(stringResource(R.string.crewhome_level_next), style = blueText(15.sp, ink.secondary, FontWeight.Bold))
                    Spacer(Modifier.height(8.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        if (current != null) {
                            Text(stringResource(R.string.crew_level_value, current + 1), style = blueText(36.sp, ink.text, FontWeight.ExtraBold, 1.15f))
                        }
                        Spacer(Modifier.weight(1f))
                        // 승급 조건은 정해지지 않았다 — XP · 필요 거리 · 보상을 만들지 않는다
                        Text(stringResource(R.string.crewhome_level_pending), style = blueText(15.sp, ink.secondary, FontWeight.Medium))
                    }
                    Spacer(Modifier.height(10.dp))
                    Text(stringResource(R.string.crewhome_level_pending_body), style = blueText(15.sp, ink.secondary, FontWeight.Medium, 1.45f))
                }
            }
            Spacer(Modifier.height(32.dp))
            Text(stringResource(R.string.crewhome_level_week), style = blueText(20.sp, ink.text, FontWeight.Bold))
            Spacer(Modifier.height(14.dp))
            val weekKm = info?.weekKm ?: crew?.weekKm
            val runners = info?.weekRunners ?: crew?.weekRunners
            Row {
                Column(Modifier.weight(1f)) {
                    Text(
                        if (weekKm != null) stringResource(R.string.crewhome_week_km, km(weekKm)) else "—",
                        style = blueText(36.sp, ink.text, FontWeight.ExtraBold, 1.15f), modifier = Modifier.testTag("home-level-km"),
                    )
                    Spacer(Modifier.height(4.dp))
                    Text(stringResource(R.string.crewhome_level_week_km), style = blueText(15.sp, ink.secondary, FontWeight.Medium))
                }
                Column(Modifier.weight(1f)) {
                    Text(
                        if (runners != null) stringResource(R.string.crewhome_level_runners_value, runners) else "—",
                        style = blueText(36.sp, ink.text, FontWeight.ExtraBold, 1.15f),
                    )
                    Spacer(Modifier.height(4.dp))
                    Text(stringResource(R.string.crewhome_level_week_runners), style = blueText(15.sp, ink.secondary, FontWeight.Medium))
                }
            }
            Spacer(Modifier.height(28.dp))
        }
        BlueBottomBar { BlueButton(stringResource(R.string.crewhome_level_weekly), onWeekly, Modifier.testTag("home-level-weekly")) }
    }
}

// ─────────────────────────────────────────────────────────────
// 04 크루원 · 27 이름 검색
// ─────────────────────────────────────────────────────────────

/** 04 크루원 — 서버 목록 전체를 이어 보인다(크루장 먼저). 27 이름 검색은 같은 자리에서 결과를 바꾼다 */
@Composable
fun CrewMembersHomeScreen(viewModel: CrewHomeMembersViewModel, onBack: () -> Unit, onOpen: (member: CrewMember) -> Unit) {
    val ink = blueInk()
    val card by viewModel.card.collectAsStateWithLifecycle()
    val roster by viewModel.roster.collectAsStateWithLifecycle()
    val me by viewModel.me.collectAsStateWithLifecycle()
    val query by viewModel.query.collectAsStateWithLifecycle()
    LifecycleResumeEffect(viewModel) {
        viewModel.load()
        onPauseOrDispose {}
    }
    BluePage(Modifier.testTag("home-members-page")) {
        BlueTopBar(stringResource(R.string.crewhome_members_bar), onBack)
        when (val state = roster) {
            is HomeLoad.Ready -> {
                val all = state.value
                val searching = query.isNotBlank()
                val shown = remember(all, query) { all.filter { CrewHomeRules.matches(it.name, query) } }
                LazyColumn(
                    Modifier.weight(1f).fillMaxWidth().testTag("home-members-list"),
                    contentPadding = PaddingValues(start = BlueGutter, end = BlueGutter, bottom = 40.dp),
                ) {
                    item(key = "head") {
                        Column {
                            Spacer(Modifier.height(16.dp))
                            Text(
                                stringResource(R.string.crewhome_members_title, all.size), style = blueText(32.sp, ink.text, FontWeight.ExtraBold, 1.25f),
                                modifier = Modifier.testTag("home-members-title"),
                            )
                            card?.let { crew ->
                                Spacer(Modifier.height(6.dp))
                                Text(
                                    crew.capacity?.let { stringResource(R.string.crewhome_members_sub, crew.name, it) } ?: crew.name,
                                    style = blueText(16.sp, ink.secondary, FontWeight.Medium),
                                )
                            }
                            Spacer(Modifier.height(20.dp))
                            HomeSearchField(query, viewModel::setQuery, stringResource(R.string.crewhome_members_search), fieldTag = "home-members-search")
                            Spacer(Modifier.height(if (searching) 24.dp else 14.dp))
                            if (searching && shown.isNotEmpty()) {
                                Text(stringResource(R.string.crewhome_members_found, shown.size), style = blueText(15.sp, ink.secondary, FontWeight.Medium), modifier = Modifier.testTag("home-members-found"))
                                Spacer(Modifier.height(8.dp))
                            }
                        }
                    }
                    if (searching && shown.isEmpty()) {
                        item(key = "none") {
                            Column(Modifier.fillMaxWidth().padding(top = 48.dp).testTag("home-members-none"), horizontalAlignment = Alignment.CenterHorizontally) {
                                Text(stringResource(R.string.crewhome_members_none_title), style = blueText(20.sp, ink.text, FontWeight.Bold))
                                Spacer(Modifier.height(8.dp))
                                Text(stringResource(R.string.crewhome_members_none_body), style = blueText(15.sp, ink.secondary, FontWeight.Medium))
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
            HomeLoad.Loading -> Column(Modifier.padding(BlueGutter)) {
                BlueSkeleton(Modifier.fillMaxWidth().height(40.dp))
                Spacer(Modifier.height(24.dp))
                repeat(5) {
                    BlueSkeleton(Modifier.fillMaxWidth().height(56.dp))
                    Spacer(Modifier.height(14.dp))
                }
            }
        }
    }
}

@Composable
private fun MemberRow(card: CrewCard?, member: CrewMember, sub: String, onClick: () -> Unit) {
    val ink = blueInk()
    Row(
        Modifier.fillMaxWidth().heightIn(min = 76.dp).homeClickable(onClick = onClick).testTag("home-member-${member.userId}"),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        ChatFace(member.name, owner = member.owner, size = 48.dp)
        Column(Modifier.weight(1f).padding(start = 18.dp)) {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f).padding(vertical = 14.dp)) {
                    Text(member.name, style = blueText(18.sp, ink.text, FontWeight.Bold, 1.3f), maxLines = 1, overflow = TextOverflow.Ellipsis)
                    Spacer(Modifier.height(4.dp))
                    Text(sub, style = blueText(14.5.sp, if (member.owner) ink.info else ink.secondary, FontWeight.Medium), maxLines = 1, overflow = TextOverflow.Ellipsis)
                }
                Icon(Icons.Filled.ChevronRight, null, tint = ink.secondary, modifier = Modifier.size(24.dp))
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
    val ink = blueInk()
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
    BluePage(Modifier.testTag(if (leader) "home-person-leader" else "home-person")) {
        BlueTopBar(stringResource(if (leader) R.string.crewhome_person_leader_bar else R.string.crewhome_person_member_bar), onBack)
        when {
            current != null -> Column(Modifier.weight(1f).fillMaxWidth().verticalScroll(rememberScrollState()).padding(horizontal = BlueGutter)) {
                Spacer(Modifier.height(24.dp))
                Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                    ChatFace(current.name, owner = leader, size = 96.dp)
                }
                Spacer(Modifier.height(18.dp))
                Text(
                    current.name, style = blueText(32.sp, ink.text, FontWeight.ExtraBold, 1.25f), textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth().testTag("home-person-name"),
                )
                Spacer(Modifier.height(6.dp))
                val member = current.role == CrewPersonRole.OWNER || current.role == CrewPersonRole.MEMBER
                Text(
                    if (member) stringResource(if (leader) R.string.crewhome_person_leader_of else R.string.crewhome_person_member_of, card?.name.orEmpty())
                    else stringResource(R.string.crewhome_person_gone),
                    style = blueText(16.sp, ink.info, FontWeight.Bold), textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth(),
                )
                current.joinedAt?.takeIf { member && it > 0 }?.let { joined ->
                    Spacer(Modifier.height(6.dp))
                    Text(
                        stringResource(R.string.crewhome_person_since, homeDate(localDay(joined))), style = blueText(15.sp, ink.secondary, FontWeight.Medium),
                        textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth().testTag("home-person-since"),
                    )
                }
                val note = card?.leaderNote?.takeIf { leader && it.isNotBlank() }
                if (note != null) {
                    Spacer(Modifier.height(32.dp))
                    Text(note, style = blueText(22.sp, ink.text, FontWeight.ExtraBold, 1.45f), modifier = Modifier.testTag("home-person-note"))
                }
                if (member) {
                    Spacer(Modifier.height(28.dp))
                    HomeDivider()
                    Spacer(Modifier.height(24.dp))
                    Text(stringResource(R.string.crewhome_person_runs), style = blueText(18.sp, ink.text, FontWeight.Bold))
                    Spacer(Modifier.height(14.dp))
                    when (val runState = lastRun) {
                        is HomeLoad.Ready -> {
                            val run = runState.value
                            if (run == null) {
                                Text(
                                    stringResource(R.string.crewhome_person_runs_none), style = blueText(15.5.sp, ink.secondary, FontWeight.Medium),
                                    modifier = Modifier.testTag("home-person-runs-none"),
                                )
                            } else {
                                // 시각은 끝난 시각, 실제 runId 의 19 로
                                BlueSurface(Modifier.fillMaxWidth().testTag("home-person-run"), onClick = { onRun(run) }, selected = true) {
                                    Row(
                                        Modifier.heightIn(min = 72.dp).padding(start = 20.dp, end = 14.dp, top = 12.dp, bottom = 12.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                    ) {
                                        Column(Modifier.weight(1f)) {
                                            Text(
                                                stringResource(R.string.crewhome_person_run, recordDayLabel(run.endedAt), km(run.km)),
                                                style = blueText(22.sp, ink.text, FontWeight.ExtraBold, 1.25f),
                                            )
                                            Spacer(Modifier.height(4.dp))
                                            Text(
                                                stringResource(R.string.crewhome_person_run_sub, homeClock(run.endedAt), durationLong(run.durationS)),
                                                style = blueText(15.sp, ink.secondary, FontWeight.Medium),
                                            )
                                        }
                                        Icon(Icons.Filled.ChevronRight, null, tint = ink.secondary, modifier = Modifier.size(24.dp))
                                    }
                                }
                            }
                        }
                        HomeLoad.Loading -> BlueSkeleton(Modifier.fillMaxWidth().height(56.dp))
                        else -> Text(stringResource(R.string.crewhome_person_error), style = blueText(15.sp, ink.secondary, FontWeight.Medium))
                    }
                }
                Spacer(Modifier.height(32.dp))
            }
            person is HomeLoad.Loading -> Column(Modifier.weight(1f).padding(BlueGutter), horizontalAlignment = Alignment.CenterHorizontally) {
                Spacer(Modifier.height(20.dp))
                BlueSkeleton(Modifier.size(96.dp), 48.dp)
                Spacer(Modifier.height(20.dp))
                BlueSkeleton(Modifier.width(140.dp).height(30.dp))
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
