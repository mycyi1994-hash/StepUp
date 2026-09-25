package com.stepup.android.ui.screens.community

import androidx.compose.foundation.background
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Map
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.Groups
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.stepup.android.R
import com.stepup.android.data.repo.BoardSyncState
import com.stepup.android.ui.components.TwoWaySwitch
import com.stepup.android.ui.components.PrimaryCta
import com.stepup.android.data.repo.Crew
import com.stepup.android.data.repo.CrewJoinPolicy
import com.stepup.android.domain.GeoPoint
import com.stepup.android.domain.Post
import com.stepup.android.domain.PostCategory
import com.stepup.android.ui.components.AvatarStack
import com.stepup.android.ui.components.DarkIconButton
import com.stepup.android.ui.components.GhostButton
import com.stepup.android.ui.components.GlowCard
import com.stepup.android.ui.components.HexBadge
import com.stepup.android.ui.components.SectionHeader
import com.stepup.android.ui.components.VoltButton
import com.stepup.android.ui.components.quietClickable
import com.stepup.android.ui.components.rememberCurrentLocation
import com.stepup.android.ui.guide.GuideTour
import com.stepup.android.ui.guide.guideTarget
import com.stepup.android.ui.theme.CarbonHigh
import com.stepup.android.ui.theme.Edge
import com.stepup.android.ui.theme.Night
import com.stepup.android.ui.theme.OnVolt
import com.stepup.android.ui.theme.Silver
import com.stepup.android.ui.theme.Slate
import com.stepup.android.ui.theme.Snow
import com.stepup.android.ui.theme.Volt

/**
 * 커뮤니티 — 두 개의 세그먼트로 나뉜다.
 *
 * **게시판**: 앱 사용자 전체가 쓰는 열린 공간. 번개러닝(가까운 순) · 자유 · 꿀팁.
 * **크루**: 내가 속한 소규모 커뮤니티. 모임 만들기와 크루 전용 게시판.
 */
@Composable
fun CommunityScreen(
    onOpenLobby: (String) -> Unit = {},
    onOpenNotifications: () -> Unit = {},
    onOpenRanking: () -> Unit = {},
    onOpenCrew: (String) -> Unit = {},
    onCreateCrew: () -> Unit = {},
    onWritePost: (String) -> Unit = {},
    onOpenFlash: (Long) -> Unit = {},
    onOpenMap: () -> Unit = {},
    viewModel: CommunityViewModel = viewModel(factory = CommunityViewModel.Factory),
) {
    val tab by viewModel.tab.collectAsStateWithLifecycle()
    var stories by rememberSaveable { mutableStateOf(false) }
    var allMeetups by rememberSaveable { mutableStateOf(false) }
    LaunchedEffect(GuideTour.active) {
        if (GuideTour.active) { viewModel.selectTab(CommunityTab.BOARD); stories = false; allMeetups = false }
    }
    BackHandler(enabled = tab == CommunityTab.CREW || allMeetups) {
        viewModel.selectTab(CommunityTab.BOARD)
        allMeetups = false
    }
    val segments = listOf(
        stringResource(R.string.community_together),
        stringResource(R.string.community_stories),
    )

    // 댓글 창은 어느 세그먼트에 있든 같은 뷰모델이 열고 닫는다
    CommentSheetHost(viewModel)

    Column(Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier.padding(start = 20.dp, end = 20.dp, top = 4.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            if (tab == CommunityTab.CREW) {
                com.stepup.android.ui.components.FocusHeader(
                    stringResource(R.string.community_tab_my_crew),
                    onBack = { viewModel.selectTab(CommunityTab.BOARD) },
                )
            } else {
              Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Text(stringResource(R.string.community_hero_title), modifier = Modifier.weight(1f),
                    color = Snow, fontSize = 26.sp, fontWeight = FontWeight.Bold)
                DarkIconButton(Icons.Filled.Map, stringResource(R.string.community_map_title), onClick = onOpenMap)
                DarkIconButton(Icons.Filled.Groups, stringResource(R.string.community_tab_my_crew),
                    onClick = { viewModel.selectTab(CommunityTab.CREW) })
              }
            TwoWaySwitch(
                labels = segments,
                selected = if (stories) 1 else 0,
                onSelect = {
                    stories = it == 1
                    allMeetups = false
                },
                modifier = Modifier.guideTarget(GuideTour.Targets.COMMUNITY_SEGMENTS),
            )
            }
        }

        when (tab) {
            CommunityTab.BOARD -> if (!stories && !allMeetups) TogetherTab(
                viewModel = viewModel, onOpenFlash = onOpenFlash,
                onWritePost = { onWritePost("") }, onAllMeetups = { allMeetups = true },
            ) else BoardTab(
                viewModel = viewModel,
                onlyFlash = !stories,
                onOpenRanking = onOpenRanking,
                onWritePost = { onWritePost("") },
                onOpenFlash = onOpenFlash,
                onOpenMap = onOpenMap,
            )

            CommunityTab.CREW -> CrewTab(
                viewModel = viewModel,
                onOpenLobby = onOpenLobby,
                onOpenCrew = onOpenCrew,
                onCreateCrew = onCreateCrew,
            )
        }
    }
}

// ─────────────────────────────────────────────────────────────
// 게시판 탭
// ─────────────────────────────────────────────────────────────

@Composable
private fun BoardTab(
    viewModel: CommunityViewModel,
    onlyFlash: Boolean,
    onOpenRanking: () -> Unit,
    onWritePost: () -> Unit,
    onOpenFlash: (Long) -> Unit,
    onOpenMap: () -> Unit,
) {
    val posts by viewModel.boardPosts.collectAsStateWithLifecycle()
    val boardSync by viewModel.boardSync.collectAsStateWithLifecycle()

    // 시안대로 한 줄 피드 — 번개러닝도 일반 글도 최신 순으로 섞어 보여 준다.
    // 검색 · 분류 칩 · 랭킹 카드는 두지 않는다(랭킹은 내 정보 › 설정).
    // 번개 카드의 "몇 km"는 내 자리에서 잰다. 모르면 함께 달릴 거리를 적는다.
    val here = rememberCurrentLocation()
    val visible = remember(posts, onlyFlash) {
        posts.filter { it.isFlash == onlyFlash }.sortedByDescending { it.createdAt }
    }

    Box(Modifier.fillMaxSize()) {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(start = 20.dp, end = 20.dp, top = 12.dp, bottom = 96.dp),
            verticalArrangement = Arrangement.spacedBy(13.dp),
        ) {
            // 지도 — 번개·코스를 내 주변에서, 그리고 땅따먹기. 목록 맨 위 한 줄이라
            // 글쓰기(주 행동)와 겹치지 않는다.
            item { MapEntryCard(onClick = onOpenMap) }

            // 글은 서버에만 있다. 로그인·연결 문제로 비었으면 "아직 글이 없어요"와 섞지 않는다.
            if (boardSync != BoardSyncState.Ready) {
                item { BoardSyncCard(boardSync, onRetry = viewModel::refreshBoard) }
            } else if (visible.isEmpty()) {
                item {
                    com.stepup.android.ui.components.StatePanel(
                        message = stringResource(if (onlyFlash) R.string.community_meetups_empty else R.string.community_board_empty),
                        icon = Icons.Filled.Groups,
                    )
                }
            }

            items(visible, key = { it.id }) { post ->
                if (post.isFlash) {
                    FlashRunCard(
                        post = post,
                        onJoin = { viewModel.toggleJoinFlash(post.id) },
                        onLike = { viewModel.toggleLike(post.id) },
                        onComment = { viewModel.openComments(post.id) },
                        onDelete = { viewModel.deletePost(post.id) },
                        onOpen = { onOpenFlash(post.id) },
                        onReport = { viewModel.askReport(post) },
                        here = here,
                    )
                } else {
                    TextPostCard(
                        post = post,
                        onLike = { viewModel.toggleLike(post.id) },
                        onComment = { viewModel.openComments(post.id) },
                        onDelete = { viewModel.deletePost(post.id) },
                        onReport = { viewModel.askReport(post) },
                    )
                }
            }
        }

        // 글쓰기 — 화면의 주 행동 하나. 시안대로 아래에 넓게.
        PrimaryCta(
            text = stringResource(R.string.post_write),
            icon = Icons.Filled.Edit,
            showArrow = false,
            onClick = onWritePost,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(start = 20.dp, end = 20.dp, bottom = 14.dp)
                .guideTarget(GuideTour.Targets.COMMUNITY_WRITE),
        )
    }
}

/**
 * 번개러닝은 가까운 순, 나머지는 최신 순.
 * 필터가 없으면 번개러닝을 위로 올려 "지금 뛸 사람"이 먼저 보이게 한다.
 */
/** 뽑혀 온 목록에 검색어만 건다 — 순서는 건드리지 않는다 */
private fun searchPosts(posts: List<Post>, query: String): List<Post> =
    if (query.isBlank()) {
        posts
    } else {
        posts.filter {
            it.title.contains(query, ignoreCase = true) ||
                it.body.contains(query, ignoreCase = true) ||
                it.author.contains(query, ignoreCase = true)
        }
    }

private fun filterPosts(
    posts: List<Post>,
    filter: PostCategory?,
    query: String,
    here: GeoPoint?,
): List<Post> {
    val matched = posts.filter { post ->
        (filter == null || post.category == filter) &&
            (
                query.isBlank() ||
                    post.title.contains(query, ignoreCase = true) ||
                    post.body.contains(query, ignoreCase = true) ||
                    post.author.contains(query, ignoreCase = true)
                )
    }
    val (flash, rest) = matched.partition { it.isFlash }
    // 마감된 번개는 아래로, 나머지는 가까운 순. 거리를 모르는 글은 가까운 글
    // 뒤에 모임 시각 순으로 선다.
    val sortedFlash = flash.sortedWith(
        compareBy<Post>({ it.isClosed }, { it.awayKmFrom(here) ?: Double.MAX_VALUE }, { it.meetAt }),
    )
    val sortedRest = rest.sortedByDescending { it.createdAt }
    return when (filter) {
        PostCategory.FLASH -> sortedFlash
        // 전체 보기에서는 번개러닝을 창내창이 따로 보여주므로 일반 글만
        null -> sortedRest
        else -> sortedRest
    }
}

/**
 * 번개러닝 모집 창 — 화면에는 2개 높이만 보이고 안에서 스크롤해 나머지를 본다.
 * (그룹모집을 한눈에, 피드는 그 아래로)
 */
@Composable
private fun FlashRunWindow(
    posts: List<Post>,
    onJoin: (Long) -> Unit,
    onLike: (Long) -> Unit,
    onComment: (Long) -> Unit,
    onDelete: (Long) -> Unit,
    onOpen: (Long) -> Unit,
    onReport: (Post) -> Unit,
    here: GeoPoint?,
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(452.dp)
            .clip(RoundedCornerShape(24.dp))
            .background(Volt.copy(alpha = 0.05f))
            .border(1.dp, Volt.copy(alpha = 0.18f), RoundedCornerShape(24.dp)),
    ) {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(10.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            items(posts, key = { it.id }) { post ->
                FlashRunCard(
                    post = post,
                    onJoin = { onJoin(post.id) },
                    onLike = { onLike(post.id) },
                    onComment = { onComment(post.id) },
                    onDelete = { onDelete(post.id) },
                    onOpen = { onOpen(post.id) },
                    onReport = { onReport(post) },
                    here = here,
                )
            }
        }
        // 아래에 더 있음을 알리는 하단 페이드
        if (posts.size > 2) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(26.dp)
                    .align(Alignment.BottomCenter)
                    .background(
                        androidx.compose.ui.graphics.Brush.verticalGradient(
                            listOf(
                                androidx.compose.ui.graphics.Color.Transparent,
                                Night.copy(alpha = 0.85f),
                            ),
                        ),
                    ),
            )
        }
    }
}

@Composable
private fun RankingTeaser(rank: Int?, balance: Double, onClick: () -> Unit) {
    GlowCard(
        modifier = Modifier
            .quietClickable(onClick)
            .guideTarget(GuideTour.Targets.COMMUNITY_RANKING),
        accent = true,
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 14.dp),
        spacing = 0.dp,
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(13.dp),
        ) {
            Box(
                modifier = Modifier
                    .size(42.dp)
                    .clip(RoundedCornerShape(14.dp))
                    .background(Volt.copy(alpha = 0.13f)),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    Icons.Filled.EmojiEvents,
                    contentDescription = null,
                    tint = Volt,
                    modifier = Modifier.size(21.dp),
                )
            }
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                Text(
                    text = stringResource(R.string.community_ranking),
                    style = MaterialTheme.typography.titleSmall,
                    color = Snow,
                )
                Text(
                    text = if (rank == null) {
                        stringResource(R.string.ranking_teaser_unknown, "%,.0f".format(balance))
                    } else {
                        stringResource(R.string.ranking_teaser, rank, "%,.0f".format(balance))
                    },
                    fontSize = 14.sp,
                    color = Silver,
                )
            }
            Icon(
                Icons.Filled.ChevronRight,
                contentDescription = null,
                tint = Slate,
                modifier = Modifier.size(18.dp),
            )
        }
    }
}

@Composable
private fun WriteFab(onClick: () -> Unit, modifier: Modifier = Modifier) {
    Row(
        modifier = modifier
            .clip(RoundedCornerShape(50))
            .background(Volt)
            .quietClickable(onClick)
            .padding(horizontal = 18.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        Icon(
            Icons.Filled.Add,
            contentDescription = null,
            tint = OnVolt,
            modifier = Modifier.size(17.dp),
        )
        Text(
            text = stringResource(R.string.post_write),
            color = OnVolt,
            fontSize = 13.sp,
            fontWeight = FontWeight.Black,
        )
    }
}

// ─────────────────────────────────────────────────────────────
// 크루 탭
// ─────────────────────────────────────────────────────────────

@Composable
private fun CrewTab(
    viewModel: CommunityViewModel,
    onOpenLobby: (String) -> Unit,
    onOpenCrew: (String) -> Unit,
    onCreateCrew: () -> Unit,
) {
    val crews by viewModel.crews.collectAsStateWithLifecycle()
    val sync by viewModel.crewSync.collectAsStateWithLifecycle()
    var query by rememberSaveable { mutableStateOf("") }

    CrewNoticeToast(viewModel)

    val filtered = crews.filter { it.name.contains(query, ignoreCase = true) }
    val myCrews = filtered.filter { it.joined }
    // 신청해 둔 크루를 위로. 기다리는 중인 것을 찾으러 목록을 뒤지지 않게 한다.
    val others = filtered.filterNot { it.joined }
        .sortedWith(compareByDescending<Crew> { it.requested }.thenBy { it.kmAway })

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(start = 18.dp, end = 18.dp, top = 14.dp, bottom = 24.dp),
        verticalArrangement = Arrangement.spacedBy(13.dp),
    ) {
        item { CreateCrewCard(onClick = onCreateCrew) }

        if (crews.isEmpty()) {
            item { CrewSyncCard(sync, onRetry = viewModel::refreshCrews) }
        }

        item { SearchField(query, { query = it }) }

        item {
            SectionHeader(
                title = stringResource(R.string.community_my_crews),
                actionText = "${myCrews.size}",
            )
        }

        if (myCrews.isEmpty()) {
            item {
                com.stepup.android.ui.components.StatePanel(
                    stringResource(R.string.community_no_crew_yet), Icons.Filled.Groups,
                )
            }
        }

        items(myCrews, key = { "my_${it.id}" }) { crew ->
            CrewCard(
                crew = crew,
                onToggleJoin = { viewModel.toggleJoin(crew.id) },
                onOpenLobby = { onOpenLobby(crew.id) },
                onOpenBoard = { onOpenCrew(crew.id) },
            )
        }

        item { SectionHeader(title = stringResource(R.string.community_nearby)) }

        if (others.isEmpty()) {
            item {
                GlowCard(contentPadding = PaddingValues(24.dp)) {
                    Text(
                        text = if (query.isBlank()) {
                            stringResource(R.string.common_none)
                        } else {
                            stringResource(R.string.community_no_results, query)
                        },
                        style = MaterialTheme.typography.bodyMedium,
                        color = Silver,
                    )
                }
            }
        }

        items(others, key = { "near_${it.id}" }) { crew ->
            CrewCard(
                crew = crew,
                onToggleJoin = { viewModel.toggleJoin(crew.id) },
                onOpenLobby = { onOpenLobby(crew.id) },
                onOpenBoard = { onOpenCrew(crew.id) },
            )
        }
    }
}

/** 당근 그룹처럼 "모임 만들기"를 크루 탭 맨 위에 둔다. */
@Composable
private fun CreateCrewCard(onClick: () -> Unit) {
    GlowCard(
        modifier = Modifier.quietClickable(onClick),
        contentPadding = PaddingValues(16.dp),
        spacing = 0.dp,
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(13.dp),
        ) {
            Box(
                modifier = Modifier
                    .size(42.dp)
                    .clip(RoundedCornerShape(14.dp))
                    .background(Volt.copy(alpha = 0.13f)),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    Icons.Filled.Groups,
                    contentDescription = null,
                    tint = Volt,
                    modifier = Modifier.size(21.dp),
                )
            }
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                Text(
                    text = stringResource(R.string.crew_create_title),
                    style = MaterialTheme.typography.titleSmall,
                    color = Snow,
                )
                Text(
                    text = stringResource(R.string.crew_create_sub),
                    fontSize = 14.sp,
                    color = Silver,
                )
            }
            Icon(
                Icons.Filled.ChevronRight,
                contentDescription = null,
                tint = Slate,
                modifier = Modifier.size(18.dp),
            )
        }
    }
}

@Composable
private fun CrewCard(
    crew: Crew,
    onToggleJoin: () -> Unit,
    onOpenLobby: () -> Unit,
    onOpenBoard: () -> Unit,
) {
    val joined = crew.joined
    GlowCard(contentPadding = PaddingValues(16.dp), spacing = 12.dp, accent = joined) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .quietClickable(onOpenBoard),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(13.dp),
        ) {
            HexBadge(text = crew.monogram, size = 46.dp)
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    Text(
                        crew.name,
                        modifier = Modifier.weight(1f, fill = false),
                        style = MaterialTheme.typography.titleSmall,
                        color = Snow,
                        maxLines = 1,
                    )
                    if (crew.owned) CrewTag(stringResource(R.string.crew_owner_badge))
                    if (crew.joinPolicy == CrewJoinPolicy.APPROVAL) {
                        CrewTag(stringResource(R.string.crew_policy_approval))
                    }
                }
                if (crew.tagline.isNotBlank()) {
                    Text(crew.tagline, fontSize = 14.sp, color = Silver, maxLines = 1)
                }
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                ) {
                    Icon(
                        Icons.Filled.LocationOn,
                        contentDescription = null,
                        tint = Slate,
                        modifier = Modifier.size(11.dp),
                    )
                    Text(
                        text = if (crew.kmAway > 0) {
                            stringResource(R.string.community_km_away, "%.1f".format(crew.kmAway))
                        } else {
                            crew.area.ifBlank { stringResource(R.string.crew_area_here) }
                        },
                        fontSize = 14.sp,
                        color = Slate,
                    )
                    Text("·", fontSize = 14.sp, color = Slate)
                    Text(
                        text = stringResource(R.string.community_members, crew.memberCount),
                        fontSize = 14.sp,
                        color = Silver,
                    )
                }
            }
            AvatarStack(visible = 3, extra = crew.memberCount / 10, dot = 22.dp)
        }
        // 크루장에게는 기다리는 신청이 있다는 것을 카드에서 바로 알린다. 크루
        // 안으로 들어가야만 보이면 신청한 사람이 며칠씩 기다리게 된다.
        if (crew.owned && crew.pendingCount > 0) {
            Text(
                text = stringResource(R.string.crew_pending_badge, crew.pendingCount),
                modifier = Modifier.quietClickable(onOpenBoard),
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                color = Volt,
            )
        }
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(9.dp),
        ) {
            if (joined) {
                VoltButton(
                    text = stringResource(R.string.crew_open_lobby),
                    onClick = onOpenLobby,
                    modifier = Modifier.weight(1f),
                )
                GhostButton(
                    text = stringResource(R.string.crew_board),
                    onClick = onOpenBoard,
                )
                // 크루장은 나갈 수 없다 — 나가면 주인 없는 크루가 남는다.
                if (!crew.owned) {
                    GhostButton(
                        text = stringResource(R.string.community_leave_crew),
                        onClick = onToggleJoin,
                        accent = Silver,
                    )
                }
            } else {
                CrewJoinAction(crew, onToggleJoin, Modifier.fillMaxWidth())
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────

@Composable
private fun SearchField(
    query: String,
    onQueryChange: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(CarbonHigh)
            .border(1.dp, Edge, RoundedCornerShape(16.dp))
            .padding(horizontal = 13.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Icon(
            Icons.Filled.Search,
            contentDescription = null,
            tint = Slate,
            modifier = Modifier.size(17.dp),
        )
        Box(Modifier.weight(1f)) {
            if (query.isEmpty()) {
                Text(
                    text = stringResource(R.string.community_search_hint),
                    style = MaterialTheme.typography.bodyMedium,
                    color = Slate,
                )
            }
            BasicTextField(
                value = query,
                onValueChange = onQueryChange,
                singleLine = true,
                textStyle = TextStyle(fontFamily = com.stepup.android.ui.theme.StepUpSans, color = Snow, fontSize = 13.sp),
                cursorBrush = SolidColor(Volt),
                modifier = Modifier.fillMaxWidth(),
            )
        }
    }
}

/** 게시판 맨 위 한 줄 — 지도로 들어가는 입구 */
@Composable
private fun MapEntryCard(onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(CarbonHigh)
            .quietClickable(onClick)
            .padding(horizontal = 16.dp, vertical = 13.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Icon(Icons.Filled.Map, contentDescription = null, tint = Volt, modifier = Modifier.size(22.dp))
        Column(Modifier.weight(1f)) {
            Text(
                text = stringResource(R.string.community_map_title),
                color = Snow,
                fontWeight = FontWeight.Bold,
                fontSize = 14.sp,
            )
            Text(
                text = stringResource(R.string.community_map_sub),
                color = Silver,
                fontSize = 14.sp,
            )
        }
        Icon(Icons.Filled.ChevronRight, contentDescription = null, tint = Silver)
    }
}
