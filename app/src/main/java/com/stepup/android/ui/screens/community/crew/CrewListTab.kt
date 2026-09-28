package com.stepup.android.ui.screens.community.crew

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
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.outlined.LocationOn
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
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.stepup.android.R
import com.stepup.android.core.ServiceLocator
import com.stepup.android.data.repo.CrewSyncState
import com.stepup.android.domain.CrewApplicationStatus
import com.stepup.android.domain.CrewCard
import com.stepup.android.domain.CrewDraft
import com.stepup.android.domain.CrewRules
import com.stepup.android.domain.CrewSort
import com.stepup.android.ui.components.rememberCurrentLocation
import com.stepup.android.ui.experience.feedbackClickable
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/** 목록에서 나가는 곳 — 명함 안의 누르는 곳은 각자의 화면으로 간다(카드 전체의 상세 이동과 겹치지 않는다) */
class CrewListActions(
    val onOpenCrew: (String) -> Unit,
    val onOpenImage: (String) -> Unit,
    val onOpenMembers: (String) -> Unit,
    val onOpenLeader: (crewId: String, leaderId: String) -> Unit,
    val onOpenGoal: (String) -> Unit,
    val onOpenResult: (crewId: String, applicationId: Long) -> Unit,
    /** 25 모집할 크루 선택(운영 중인 크루가 있을 때) */
    val onRecruitEntry: () -> Unit,
    /** 만들기 1단계 — resume 이면 남긴 초안을 이어 쓴다 */
    val onCreate: (resume: Boolean) -> Unit,
    val onOpenRegion: () -> Unit,
)

/**
 * 크루 모집 — 확정 2번 크루 명함 목록(01). 지역 · 범위(02) · 정렬(04)은 적용해야 바뀌고, 불러오는 중(06) · 빈 목록(05) ·
 * 실패(07)를 나눈다. 다시 불러와도 고른 지역과 정렬은 그대로다. 목록 위치는 상세에 다녀와도 남는다.
 */
@Composable
fun CrewListTab(
    actions: CrewListActions,
    /** 번개 모임 · 내 크루(예전 "함께 뛰기") — 크루 모집 목록 끝 안쪽으로 옮겼다 */
    onOpenMeetups: () -> Unit,
    viewModel: CrewListViewModel = viewModel(factory = CrewListViewModel.Factory),
) {
    val ink = crewInk()
    val words = rememberCrewWords()
    val visible by viewModel.visible.collectAsStateWithLifecycle()
    val all by viewModel.cards.collectAsStateWithLifecycle()
    val sync by viewModel.sync.collectAsStateWithLifecycle()
    val region by viewModel.region.collectAsStateWithLifecycle()
    val hereName by viewModel.hereName.collectAsStateWithLifecycle()
    val radius by viewModel.radius.collectAsStateWithLifecycle()
    val sort by viewModel.sort.collectAsStateWithLifecycle()
    val query by viewModel.query.collectAsStateWithLifecycle()
    val results by viewModel.results.collectAsStateWithLifecycle()
    val draft by viewModel.createDraft.collectAsStateWithLifecycle()
    val here = rememberCurrentLocation()
    LaunchedEffect(here) { viewModel.setHere(here) }
    // 다른 화면 · 다른 앱에서 돌아오면 다시 읽는다 — 그사이 난 가입 결과(01 알림 줄)와 인원이 보이게
    androidx.lifecycle.compose.LifecycleResumeEffect(Unit) {
        viewModel.refreshOnResume()
        onPauseOrDispose {}
    }

    var sheet by rememberSaveable { mutableStateOf("") }
    val listState = rememberLazyListState()

    fun startCreate() {
        if (draft != null) sheet = SHEET_DRAFT_RESUME else actions.onCreate(false)
    }

    val loading = all.isEmpty() && (sync == CrewSyncState.Loading || sync == CrewSyncState.Idle)
    val failed = all.isEmpty() && sync is CrewSyncState.Failed
    val signIn = sync == CrewSyncState.SignInRequired

    Box(Modifier.fillMaxSize().testTag("crew-list")) {
        LazyColumn(
            Modifier.fillMaxSize().testTag("crew-list-scroll"),
            state = listState,
            contentPadding = PaddingValues(start = CrewGutter, end = CrewGutter, top = 4.dp, bottom = 104.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            item(key = "filters") {
                CrewFilterRow(
                    place = region?.name ?: hereName,
                    ranged = query.center != null,
                    sort = sort,
                    onRegion = { sheet = SHEET_REGION },
                    onSort = { sheet = SHEET_SORT },
                )
            }
            items(results, key = { "result-${it.id}" }) { card ->
                CrewResultBanner(card) { card.myApplicationId?.let { actions.onOpenResult(card.id, it) } }
            }
            when {
                signIn -> item(key = "signin") {
                    CrewEmptyState(
                        icon = { Icon(Icons.Filled.Search, null, tint = ink.info, modifier = Modifier.size(44.dp)) },
                        title = stringResource(R.string.crew_list_signin_title),
                        body = stringResource(R.string.crew_list_signin_body),
                        modifier = Modifier.padding(top = 72.dp).testTag("crew-list-signin"),
                    ) { com.stepup.android.ui.components.SignInAgainButton() }
                }
                loading -> items(2, key = { "skeleton-$it" }) { CrewCardSkeleton() }
                failed -> item(key = "failed") {
                    CrewEmptyState(
                        icon = { Icon(Icons.Filled.Refresh, null, tint = ink.info, modifier = Modifier.size(44.dp)) },
                        title = stringResource(R.string.crew_list_error_title),
                        body = stringResource(R.string.crew_list_error_body),
                        modifier = Modifier.padding(top = 72.dp).testTag("crew-list-error"),
                    ) {
                        CrewButton(stringResource(R.string.crew_list_reload), viewModel::refresh, Modifier.testTag("crew-list-retry"))
                        CrewButton(stringResource(R.string.crew_create_button), { startCreate() }, kind = CrewButtonKind.SECONDARY)
                    }
                }
                visible.isEmpty() -> item(key = "empty") {
                    CrewEmptyState(
                        icon = { Icon(Icons.Filled.Search, null, tint = ink.info, modifier = Modifier.size(44.dp)) },
                        title = stringResource(R.string.crew_list_empty_title),
                        body = stringResource(R.string.crew_list_empty_body),
                        modifier = Modifier.padding(top = 72.dp).testTag("crew-list-empty"),
                    ) {
                        CrewButton(stringResource(R.string.crew_list_widen), { sheet = SHEET_REGION }, Modifier.testTag("crew-list-widen"))
                        CrewButton(stringResource(R.string.crew_create_button), { startCreate() }, kind = CrewButtonKind.SECONDARY)
                    }
                }
                else -> items(visible, key = { it.id }) { card ->
                    CrewProfileCard(
                        card = card,
                        words = words,
                        onOpen = { actions.onOpenCrew(card.id) },
                        onImage = { actions.onOpenImage(card.id) },
                        onLevel = { sheet = SHEET_LEVEL + card.id },
                        onMembers = { actions.onOpenMembers(card.id) },
                        onLeader = { actions.onOpenLeader(card.id, card.leaderId) },
                        onGoal = { actions.onOpenGoal(card.id) },
                    )
                }
            }
            item(key = "meetups") {
                Row(
                    Modifier.fillMaxWidth().heightIn(min = 48.dp).feedbackClickable(role = Role.Button, onClick = onOpenMeetups)
                        .testTag("crew-list-meetups"),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(stringResource(R.string.crew_list_meetups), color = ink.secondary, fontSize = 13.sp, modifier = Modifier.weight(1f))
                    Icon(Icons.Filled.ChevronRight, null, tint = ink.secondary, modifier = Modifier.size(18.dp))
                }
            }
        }

        // + 크루 모집하기 — 운영 중인 크루가 있으면 고르는 단계(25), 없으면 바로 만들기(26 · 초안이 있으면 39).
        // 빈 목록(05) · 불러오기 실패(07)는 가운데에 "크루 만들기"가 있어 띄우지 않는다(한 화면에 만들기 하나)
        val bare = !signIn && !loading && (failed || visible.isEmpty())
        if (!bare) Row(
            Modifier.align(Alignment.BottomEnd).padding(end = CrewGutter, bottom = 16.dp).heightIn(min = 44.dp)
                .clip(RoundedCornerShape(22.dp)).background(ink.primaryFace)
                .feedbackClickable(role = Role.Button) {
                    if (all.any { it.owned }) actions.onRecruitEntry() else startCreate()
                }
                .padding(horizontal = 26.dp, vertical = 11.dp)
                .testTag("crew-recruit"),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(Icons.Filled.Add, null, tint = ink.primaryText, modifier = Modifier.size(20.dp))
            Spacer(Modifier.width(18.dp))
            Text(stringResource(R.string.crew_recruit_button), color = ink.primaryText, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
        }
    }

    when {
        sheet == SHEET_REGION -> CrewRangeSheet(
            place = region?.name ?: hereName,
            radius = radius,
            onPlace = { sheet = ""; actions.onOpenRegion() },
            onApply = { km -> viewModel.apply(km); sheet = "" },
            onDismiss = { sheet = "" },
        )
        sheet == SHEET_SORT -> CrewSortSheet(sort, onApply = { viewModel.applySort(it); sheet = "" }, onDismiss = { sheet = "" })
        sheet.startsWith(SHEET_LEVEL) -> {
            val card = all.firstOrNull { it.id == sheet.removePrefix(SHEET_LEVEL) }
            if (card != null) CrewLevelSheet(card, onDismiss = { sheet = "" }) else sheet = ""
        }
        sheet == SHEET_DRAFT_RESUME || sheet == SHEET_DRAFT_DISCARD ->
            CrewStartSheets(draft, discard = sheet == SHEET_DRAFT_DISCARD, onStep = { sheet = it }, onCreate = actions.onCreate)
    }
}

/**
 * 39 초안 이어 쓰기 · 78 초안 삭제 확인 — "크루 모집하기"와 25의 "새 크루 만들기"가 같이 쓴다.
 * [onStep] 은 다음 시트("" 이면 닫기 · [SHEET_DRAFT_DISCARD]), [onCreate] 는 만들기(이어 쓰면 true).
 */
@Composable
internal fun CrewStartSheets(draft: CrewDraft?, discard: Boolean, onStep: (String) -> Unit, onCreate: (Boolean) -> Unit) {
    val scope = androidx.compose.runtime.rememberCoroutineScope()
    val saved = draft ?: return onStep("")
    if (!discard) {
        CrewResumeDraftSheet(
            draft = saved,
            onResume = { onStep(""); onCreate(true) },
            onNew = { onStep(SHEET_DRAFT_DISCARD) },
            onDismiss = { onStep("") },
        )
    } else {
        CrewConfirmSheet(
            title = stringResource(R.string.crew_discard_title),
            body = stringResource(R.string.crew_discard_body),
            confirm = stringResource(R.string.crew_discard_confirm),
            danger = true,
            tag = "crew-discard",
            onConfirm = {
                scope.launch {
                    ServiceLocator.crewCards.deleteDraft(saved)
                    withContext(Dispatchers.Main.immediate) {
                        onStep("")
                        onCreate(false)
                    }
                }
            },
            onDismiss = { onStep("") },
        )
    }
}

private const val SHEET_REGION = "region"
private const val SHEET_SORT = "sort"
private const val SHEET_LEVEL = "level:"
internal const val SHEET_DRAFT_RESUME = "resume"
internal const val SHEET_DRAFT_DISCARD = "discard"

/** 지역 · 정렬 줄 — "📍 도화동 주변 ⌄"  "가까운 순 ⌄" */
@Composable
private fun CrewFilterRow(place: String?, ranged: Boolean, sort: CrewSort, onRegion: () -> Unit, onSort: () -> Unit) {
    val ink = crewInk()
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Row(
            Modifier.weight(1f, fill = false).heightIn(min = 44.dp).clip(RoundedCornerShape(10.dp))
                .feedbackClickable(role = Role.Button, onClick = onRegion).padding(end = 8.dp).testTag("crew-region"),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(Icons.Outlined.LocationOn, null, tint = ink.info, modifier = Modifier.size(19.dp))
            Spacer(Modifier.width(6.dp))
            Text(
                when {
                    place != null && ranged -> stringResource(R.string.crew_region_near, place)
                    ranged -> stringResource(R.string.crew_region_here)
                    else -> stringResource(R.string.crew_region_choose)
                },
                color = ink.text, fontSize = 15.sp, fontWeight = FontWeight.SemiBold, maxLines = 1, overflow = TextOverflow.Ellipsis,
            )
            Icon(Icons.Filled.KeyboardArrowDown, null, tint = ink.secondary, modifier = Modifier.padding(start = 6.dp).size(18.dp))
        }
        Spacer(Modifier.weight(0.01f))
        Row(
            Modifier.heightIn(min = 44.dp).clip(RoundedCornerShape(10.dp)).feedbackClickable(role = Role.Button, onClick = onSort)
                .padding(start = 8.dp).testTag("crew-sort"),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(sortLabel(sort), color = ink.secondary, fontSize = 12.5.sp, maxLines = 1)
            Icon(Icons.Filled.KeyboardArrowDown, null, tint = ink.secondary, modifier = Modifier.padding(start = 4.dp).size(16.dp))
        }
    }
}

@Composable
internal fun sortLabel(sort: CrewSort): String = when (sort) {
    CrewSort.NEAR -> stringResource(R.string.crew_sort_near)
    CrewSort.RECENT -> stringResource(R.string.crew_sort_recent)
    CrewSort.ACTIVE -> stringResource(R.string.crew_sort_active)
}

/**
 * 크루 명함(확정 2번) — 대표 이미지와 이름 → 레벨 · 인원/정원 → 한 줄 소개 → 성격 · 지역 · 일정 · 거리 → 크루장 → 주간 목표.
 * 카드와 "크루 보기"는 상세로, 이미지 · 레벨 · 인원 · 크루장 · 목표는 각자의 화면으로 간다.
 */
@Composable
internal fun CrewProfileCard(
    card: CrewCard,
    words: CrewWords,
    onOpen: () -> Unit,
    onImage: () -> Unit,
    onLevel: () -> Unit,
    onMembers: () -> Unit,
    onLeader: () -> Unit,
    onGoal: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val ink = crewInk()
    Column(
        modifier.fillMaxWidth().clip(RoundedCornerShape(18.dp)).background(ink.card)
            .feedbackClickable(role = Role.Button, onClick = onOpen)
            .padding(start = 18.dp, end = 18.dp, top = 16.dp, bottom = 10.dp)
            .testTag("crew-card-${card.id}"),
    ) {
        Row(verticalAlignment = Alignment.Top) {
            Box(Modifier.size(68.dp).clip(RoundedCornerShape(14.dp)).feedbackClickable(role = Role.Image, onClick = onImage).testTag("crew-card-image")) {
                CrewImage(card, 68.dp, 14.dp)
            }
            Column(Modifier.weight(1f).padding(start = 14.dp)) {
                Text(
                    card.name, color = ink.text, fontSize = 23.sp, lineHeight = 29.sp, fontWeight = FontWeight.SemiBold,
                    maxLines = 2, overflow = TextOverflow.Ellipsis, modifier = Modifier.testTag("crew-card-name"),
                )
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        Modifier.heightIn(min = 44.dp).clip(RoundedCornerShape(8.dp)).feedbackClickable(role = Role.Button, onClick = onLevel)
                            .testTag("crew-card-level"),
                        contentAlignment = Alignment.CenterStart,
                    ) { CrewLevelChip(card.level) }
                    Spacer(Modifier.weight(1f))
                    Box(
                        Modifier.heightIn(min = 44.dp).widthIn(min = 96.dp).clip(RoundedCornerShape(8.dp))
                            .feedbackClickable(role = Role.Button, onClick = onMembers).testTag("crew-card-members"),
                        contentAlignment = Alignment.CenterEnd,
                    ) { CrewMembersLabel(card.memberCount, card.capacity, ink.text.copy(alpha = 0.78f)) }
                }
            }
        }
        if (card.tagline.isNotBlank()) {
            Spacer(Modifier.height(10.dp))
            Text(card.tagline, color = ink.text, fontSize = 18.sp, lineHeight = 25.sp, fontWeight = FontWeight.SemiBold, maxLines = 2, overflow = TextOverflow.Ellipsis)
        }
        val style = words.styleLine(card)
        if (style.isNotEmpty()) {
            Spacer(Modifier.height(4.dp))
            Text(style, color = ink.info, fontSize = 12.5.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
        val info = words.infoLine(card)
        if (info.isNotEmpty()) {
            Spacer(Modifier.height(3.dp))
            Text(info, color = ink.secondary, fontSize = 12.5.sp, maxLines = 2, overflow = TextOverflow.Ellipsis)
        }
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Row(
                Modifier.weight(1f).heightIn(min = 44.dp).clip(RoundedCornerShape(10.dp))
                    .feedbackClickable(role = Role.Button, onClick = onLeader).testTag("crew-card-leader"),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                CrewAvatar(card.leaderName, 24.dp, leaderFace(card), ink.text)
                Spacer(Modifier.width(7.dp))
                Text(
                    stringResource(R.string.crew_leader_named, card.leaderName), color = ink.text, fontSize = 12.5.sp,
                    fontWeight = FontWeight.SemiBold, maxLines = 1, overflow = TextOverflow.Ellipsis,
                )
            }
            CrewSmallButton(stringResource(R.string.crew_view), onOpen, Modifier.padding(start = 8.dp).testTag("crew-card-open"))
        }
        CrewRules.progress(card)?.let { progress ->
            Box(
                Modifier.fillMaxWidth().heightIn(min = 32.dp).clip(RoundedCornerShape(8.dp))
                    .feedbackClickable(role = Role.Button, onClick = onGoal).testTag("crew-card-goal"),
                contentAlignment = Alignment.CenterStart,
            ) { CrewWeeklyLine(progress) }
        }
    }
}

/** 불러오는 중 — 명함 자리만 */
@Composable
private fun CrewCardSkeleton() {
    val ink = crewInk()
    Column(
        Modifier.fillMaxWidth().height(229.dp).clip(RoundedCornerShape(18.dp)).background(ink.card).padding(18.dp)
            .testTag("crew-list-loading"),
    ) {
        Row {
            CrewSkeletonBox(Modifier.size(68.dp), 14.dp)
            Spacer(Modifier.width(14.dp))
            CrewSkeletonBox(Modifier.padding(top = 10.dp).width(130.dp).height(16.dp), 6.dp)
        }
        Spacer(Modifier.height(24.dp))
        CrewSkeletonBox(Modifier.fillMaxWidth(0.62f).height(16.dp), 6.dp)
        Spacer(Modifier.height(30.dp))
        CrewSkeletonBox(Modifier.fillMaxWidth(0.8f).height(10.dp), 5.dp)
    }
}

/** 앱 안에서 다시 읽은 가입 결과 — 누르면 결과 화면(18 · 19) */
@Composable
private fun CrewResultBanner(card: CrewCard, onClick: () -> Unit) {
    val ink = crewInk()
    val approved = card.unseenResult == CrewApplicationStatus.APPROVED
    Row(
        Modifier.fillMaxWidth().heightIn(min = 52.dp).clip(RoundedCornerShape(14.dp)).background(ink.card)
            .feedbackClickable(role = Role.Button, onClick = onClick).padding(horizontal = 16.dp, vertical = 12.dp)
            .testTag("crew-result-banner-${card.id}"),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            stringResource(if (approved) R.string.crew_result_banner_approved else R.string.crew_result_banner_declined, card.name),
            color = ink.text, fontSize = 13.5.sp, modifier = Modifier.weight(1f), maxLines = 2, overflow = TextOverflow.Ellipsis,
        )
        Icon(Icons.Filled.ChevronRight, null, tint = ink.info, modifier = Modifier.size(18.dp))
    }
}

/** 02 지역과 거리 범위 — 고른 범위는 "이 범위로 보기"를 눌러야 바뀐다 */
@Composable
private fun CrewRangeSheet(place: String?, radius: Int, onPlace: () -> Unit, onApply: (Int) -> Unit, onDismiss: () -> Unit) {
    val ink = crewInk()
    var picked by rememberSaveable { mutableStateOf(radius) }
    CrewSheet(stringResource(R.string.crew_range_title), onDismiss, Modifier.testTag("crew-range-sheet")) {
        Spacer(Modifier.height(14.dp))
        CrewRow(
            stringResource(R.string.crew_range_area), onPlace, Modifier.testTag("crew-range-area"),
            value = place ?: stringResource(R.string.crew_range_area_none),
        )
        Spacer(Modifier.height(22.dp))
        Text(stringResource(R.string.crew_range_from_here), color = ink.secondary, fontSize = 14.sp)
        Spacer(Modifier.height(8.dp))
        CrewChoiceRow {
            CrewRules.RADII.forEach { km ->
                CrewChoice(stringResource(R.string.crew_range_km, km), picked == km, { picked = km }, Modifier.weight(1f).testTag("crew-range-$km"))
            }
        }
        Spacer(Modifier.height(96.dp))
        CrewButton(stringResource(R.string.crew_range_apply), { onApply(picked) }, Modifier.testTag("crew-range-apply"))
    }
}

/** 04 정렬 — 고른 것은 적용 전까지 임시 값 */
@Composable
private fun CrewSortSheet(sort: CrewSort, onApply: (CrewSort) -> Unit, onDismiss: () -> Unit) {
    var picked by rememberSaveable { mutableStateOf(sort) }
    CrewSheet(stringResource(R.string.crew_sort_title), onDismiss, Modifier.testTag("crew-sort-sheet")) {
        Spacer(Modifier.height(14.dp))
        CrewSort.entries.forEach { option ->
            CrewRow(
                sortLabel(option), { picked = option }, Modifier.testTag("crew-sort-${option.name}"),
                value = if (option == picked) stringResource(R.string.crew_selected) else null,
            )
        }
        Spacer(Modifier.height(40.dp))
        CrewButton(stringResource(R.string.crew_apply_choice), { onApply(picked) }, Modifier.testTag("crew-sort-apply"))
    }
}

/** 39 초안 이어 쓰기 */
@Composable
private fun CrewResumeDraftSheet(draft: CrewDraft, onResume: () -> Unit, onNew: () -> Unit, onDismiss: () -> Unit) {
    val ink = crewInk()
    CrewSheet(stringResource(R.string.crew_resume_title), onDismiss, Modifier.testTag("crew-resume-sheet")) {
        Spacer(Modifier.height(18.dp))
        Row(
            Modifier.fillMaxWidth().clip(RoundedCornerShape(18.dp)).background(ink.card).padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            CrewDraftImage(draft, 60.dp, 14.dp)
            Column(Modifier.weight(1f).padding(start = 15.dp)) {
                Text(draft.name.ifBlank { stringResource(R.string.crew_untitled) }, color = ink.text, fontSize = 20.sp, fontWeight = FontWeight.SemiBold, maxLines = 2, overflow = TextOverflow.Ellipsis)
                Spacer(Modifier.height(4.dp))
                Text(
                    listOfNotNull(draft.area?.name, stringResource(R.string.crew_leader_named, stringResource(R.string.crew_leader_me))).joinToString(" · "),
                    color = ink.secondary, fontSize = 12.5.sp, maxLines = 1,
                )
            }
        }
        Spacer(Modifier.height(44.dp))
        CrewButton(stringResource(R.string.crew_resume_continue), onResume, Modifier.testTag("crew-resume-continue"))
        Spacer(Modifier.height(16.dp))
        CrewButton(stringResource(R.string.crew_resume_new), onNew, Modifier.testTag("crew-resume-new"), CrewButtonKind.SECONDARY)
    }
}
