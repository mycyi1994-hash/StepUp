package com.stepup.android.ui.screens.community

import com.stepup.android.ui.experience.ExperienceEvents
import com.stepup.android.ui.experience.FeedbackCue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.stepup.android.data.repo.SneakerRepository
import com.stepup.android.core.ServiceLocator
import com.stepup.android.data.repo.BoardResult
import com.stepup.android.data.repo.BoardSyncState
import com.stepup.android.data.repo.CommentTarget
import com.stepup.android.data.repo.CommunityRepository
import com.stepup.android.data.repo.ReportReason
import com.stepup.android.data.remote.ServerResult
import com.stepup.android.data.repo.Crew
import com.stepup.android.data.repo.CrewActionResult
import com.stepup.android.data.repo.CrewJoinPolicy
import com.stepup.android.data.repo.CrewJoinRequest
import com.stepup.android.data.repo.CrewRepository
import com.stepup.android.data.repo.CrewSyncState
import com.stepup.android.data.repo.FactionRankingState
import com.stepup.android.data.repo.RankingRepository
import com.stepup.android.data.repo.RankingState
import com.stepup.android.data.repo.RewardRepository
import com.stepup.android.domain.Comment
import com.stepup.android.domain.CommentThread
import com.stepup.android.domain.CrewRank
import com.stepup.android.domain.FlashMember
import com.stepup.android.domain.Post
import com.stepup.android.domain.PostCategory
import com.stepup.android.domain.RankBoard
import com.stepup.android.domain.RankPeriod
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/** 크루에 무언가를 한 뒤 화면에 잠깐 띄울 말 */
enum class CrewNotice { JOINED, REQUESTED, CANCELLED, LEFT, SAVED, APPROVED, REJECTED, FAILED, SIGN_IN }

/** 게시판에서 무언가를 한 뒤 화면에 잠깐 띄울 말 */
enum class BoardNotice { REPORTED, BLOCKED, FAILED, SIGN_IN }

/** 신고 창이 가리키는 것 — 글 또는 댓글 */
sealed interface ReportTarget {
    val postId: Long
    val authorId: String
    val authorName: String

    data class OfPost(
        override val postId: Long,
        override val authorId: String,
        override val authorName: String,
    ) : ReportTarget

    data class OfComment(
        override val postId: Long,
        val commentId: Long,
        override val authorId: String,
        override val authorName: String,
    ) : ReportTarget
}

/** 크루장이 보는 가입 신청 목록의 상태 */
sealed interface CrewRequestsState {
    data object Loading : CrewRequestsState
    data class Ready(val requests: List<CrewJoinRequest>) : CrewRequestsState
    data object Failed : CrewRequestsState
}

/** 커뮤니티 최상단 세그먼트 */
enum class CommunityTab { BOARD, CREW }

/**
 * 게시판 필터.
 *
 * 카테고리(번개·자유·꿀팁)와 핫글을 한 줄에 같이 둔다. 핫글은 카테고리가
 * 아니라 **뽑힌 목록**이라 PostCategory 에 넣을 수 없다 — 글은 자유이면서
 * 동시에 핫글일 수 있다.
 */
enum class BoardFilter(val category: PostCategory?) {
    ALL(null),
    FLASH(PostCategory.FLASH),
    HOT(null),
    FREE(PostCategory.FREE),
    TIP(PostCategory.TIP),
}

class CommunityViewModel(
    private val crewRepository: CrewRepository,
    private val communityRepository: CommunityRepository,
    private val rankingRepository: RankingRepository,
    rewardRepository: RewardRepository,
    private val sneakerRepository: SneakerRepository,
) : ViewModel() {

    init {
        // 갱신할 때가 지났으면 이번 주 핫글을 다시 뽑는다. 때가 아니면
        // 아무 일도 하지 않으므로 화면이 열릴 때마다 불러도 된다.
        viewModelScope.launch {
            // 글을 먼저 받고 핫글을 뽑는다. 순서가 바뀌면 빈 목록으로 뽑힌다.
            communityRepository.refresh()
            communityRepository.refreshHotIfDue()
        }
        // 크루는 서버에만 있다. 화면을 열 때마다 새로 받아야 남이 만든 크루와
        // 크루장이 승인해 준 가입이 보인다.
        refreshCrews()
    }

    val crews: StateFlow<List<Crew>> = crewRepository.crews

    /** 크루 목록을 서버에서 받아 온 상태 — 비어 있을 때 이유를 보여 주는 데 쓴다 */
    val crewSync: StateFlow<CrewSyncState> = crewRepository.sync

    private val _crewNotice = MutableStateFlow<CrewNotice?>(null)

    /** 크루 가입·탈퇴 등의 결과. 화면이 보여 준 뒤 [consumeCrewNotice] 로 비운다. */
    val crewNotice: StateFlow<CrewNotice?> = _crewNotice

    private val _crewRequests = MutableStateFlow<Map<String, CrewRequestsState>>(emptyMap())

    /** 크루장이 보는 가입 신청 — 크루별 */
    val crewRequests: StateFlow<Map<String, CrewRequestsState>> = _crewRequests

    val joinedCrewIds: StateFlow<Set<String>> = crewRepository.joinedCrewIds
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptySet())

    /** 게시판을 서버에서 받아 온 상태 — 비어 있을 때 이유를 보여 주는 데 쓴다 */
    val boardSync: StateFlow<BoardSyncState> = communityRepository.sync

    private val _boardNotice = MutableStateFlow<BoardNotice?>(null)
    val boardNotice: StateFlow<BoardNotice?> = _boardNotice

    fun consumeBoardNotice() {
        _boardNotice.value = null
    }

    private val _reportTarget = MutableStateFlow<ReportTarget?>(null)

    /** 열려 있는 신고 창. null 이면 닫혀 있다. */
    val reportTarget: StateFlow<ReportTarget?> = _reportTarget

    private val _posting = MutableStateFlow(false)

    /** 글을 올리는 중 — 버튼을 두 번 눌러 같은 글이 두 개 올라가지 않게 한다 */
    val posting: StateFlow<Boolean> = _posting

    fun refreshBoard() {
        viewModelScope.launch { communityRepository.refresh() }
    }

    val boardPosts: StateFlow<List<Post>> = communityRepository.boardPosts
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val allPosts: StateFlow<List<Post>> = communityRepository.posts
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val balance: StateFlow<Double> = rewardRepository.balance
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), 0.0)

    // ── 순위표 ──────────────────────────────────────────────────────
    //
    // 서버에서 가져온다. 예전에는 상대 15명이 코드에 박혀 있었고, 그러면
    // "당신은 3등입니다"가 거짓말이 된다.
    //
    // 부문별로 받아 두고 다시 쓴다. 탭을 오갈 때마다 다시 물으면 같은 답을
    // 받으려고 네트워크를 쓰는 셈이다.

    /** 순위표 하나를 가리키는 열쇠 — 부문 하나에 기간 넷이라 둘이 함께 와야 한다 */
    private data class BoardKey(val board: RankBoard, val period: RankPeriod)

    private val boards = MutableStateFlow<Map<BoardKey, RankingState>>(emptyMap())

    private val selectedBoard = MutableStateFlow(RankBoard.TOP_SPEED)

    private val _period = MutableStateFlow(RankPeriod.ALL)

    /** 지금 보고 있는 기간 */
    val period: StateFlow<RankPeriod> = _period

    /** 지금 보고 있는 부문·기간의 순위 */
    val ranking: StateFlow<RankingState> =
        combine(selectedBoard, _period, boards) { board, period, cache ->
            cache[BoardKey(board, period)] ?: RankingState.Loading
        }
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), RankingState.Loading)

    private val factionBoards = MutableStateFlow<Map<RankPeriod, FactionRankingState>>(emptyMap())

    val factionRanking: StateFlow<FactionRankingState> =
        combine(_period, factionBoards) { period, cache ->
            cache[period] ?: FactionRankingState.Loading
        }
            .stateIn(
                viewModelScope,
                SharingStarted.WhileSubscribed(5_000),
                FactionRankingState.Loading,
            )

    // ── 크루 순위 ──────────────────────────────────────────────────
    //
    // 이것만 기기 안에서 계산한다. 크루와 크루 러닝이 아직 이 기기에만
    // 있기 때문이다. 서버에 없는 것을 서버에 묻는 척할 수는 없다.

    private val crewBoards = MutableStateFlow<Map<RankPeriod, List<CrewRank>>>(emptyMap())

    /** 지금 기간의 크루 순위. 아직 세는 중이면 null 이다. */
    val crewRanking: StateFlow<List<CrewRank>?> =
        combine(_period, crewBoards) { period, cache -> cache[period] }
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    /**
     * 커뮤니티 첫 화면의 "내 순위" 미리보기. 아직 모르면 null 이다.
     *
     * 적립 부문 · 전체기간을 쓴다 — 세 부문 중 누구에게나 값이 있는 축이고,
     * 미리보기 한 줄에 "최근 30일 기준"까지 붙일 자리는 없다.
     */
    val mySupRank: StateFlow<Int?> = boards
        .map {
            (it[BoardKey(RankBoard.TOTAL_SUP, RankPeriod.ALL)] as? RankingState.Ready)?.me?.rank
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    /**
     * 기간 탭. 보고 있던 부문은 그대로 두고 기간만 바꾼다.
     *
     * 여기서 불러오지는 않는다. 지금 어느 부문을 보고 있는지는 화면만 알고,
     * 화면이 기간이 바뀐 것을 보고 그 부문 하나를 불러온다. 여기서 셋을 다
     * 불러 두면 탭 한 번에 요청이 셋 나가고, 그중 둘은 아무도 안 본다.
     */
    fun selectPeriod(next: RankPeriod) {
        _period.value = next
    }

    /**
     * @param meLabel 내 줄에 붙일 이름. 화면의 문자열 자원에서 온다.
     * @param force 이미 받아 둔 것도 다시 받는다 (당겨서 새로고침)
     */
    fun loadRanking(board: RankBoard, meLabel: String, force: Boolean = false) {
        selectedBoard.value = board
        val key = BoardKey(board, _period.value)
        if (!force && boards.value[key] is RankingState.Ready) return
        viewModelScope.launch {
            boards.value = boards.value + (key to RankingState.Loading)
            boards.value = boards.value +
                (key to rankingRepository.personal(key.board, key.period, meLabel))
        }
    }

    fun loadFactionRanking(force: Boolean = false) {
        val period = _period.value
        if (!force && factionBoards.value[period] is FactionRankingState.Ready) return
        viewModelScope.launch {
            factionBoards.value = factionBoards.value + (period to FactionRankingState.Loading)
            val myFaction = sneakerRepository.equipped.first()?.faction
            val next = rankingRepository.factions(myFaction, period)
            factionBoards.value = factionBoards.value + (period to next)
        }
    }

    fun loadCrewRanking(force: Boolean = false) {
        val period = _period.value
        if (!force && crewBoards.value.containsKey(period)) return
        viewModelScope.launch {
            crewBoards.value = crewBoards.value +
                (period to crewRepository.ranking(period, period.sinceMillis()))
        }
    }

    /** 선택된 세그먼트 — 탭을 오갔다 와도 유지된다 */
    val tab = MutableStateFlow(CommunityTab.BOARD)

    /** 게시판 필터 */
    val boardFilter = MutableStateFlow(BoardFilter.ALL)

    /** 이번 주 핫글 — 점수 높은 순으로 최대 30개 */
    val hotPosts: StateFlow<List<Post>> = communityRepository.hotPosts
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    /** 댓글 창을 열어 둔 글의 id. null이면 닫혀 있다 */
    val openCommentsFor = MutableStateFlow<Long?>(null)

    /**
     * 알림에서 눌러 들어온 댓글. 댓글 창이 이 댓글까지 스크롤하고 표시해 준다.
     * 0이면 특정 댓글을 가리키지 않는다.
     */
    val focusCommentId = MutableStateFlow(0L)

    /** 알림함이 남긴 "이 댓글로" 신호 */
    val commentFocus: StateFlow<CommentTarget?> = communityRepository.commentFocus

    fun selectFilter(filter: BoardFilter) {
        boardFilter.value = filter
    }

    fun openComments(postId: Long) {
        focusCommentId.value = 0L
        openCommentsFor.value = postId
    }

    /** 알림에서 들어온 경로 — 글의 댓글 창을 열고 그 댓글을 가리킨다 */
    fun openCommentsFocused(target: CommentTarget) {
        openCommentsFor.value = target.postId
        focusCommentId.value = target.commentId
        communityRepository.clearCommentFocus()
    }

    fun closeComments() {
        openCommentsFor.value = null
        focusCommentId.value = 0L
    }

    fun commentThreads(postId: Long): Flow<List<CommentThread>> =
        communityRepository.commentThreads(postId)

    /** 번개 참가자 명단. 받는 중이거나 받지 못했으면 null 이 흐른다. */
    fun flashRoster(postId: Long): Flow<List<FlashMember>?> = flow {
        emit(null)
        emit(communityRepository.roster(postId))
    }

    fun sendComment(postId: Long, body: String, parentId: Long) {
        viewModelScope.launch {
            val result = communityRepository.addComment(postId, body, parentId)
            if (result is BoardResult.Ok) ExperienceEvents.emit(FeedbackCue.Success) else noticeFailure(result)
        }
    }

    fun deleteComment(id: Long) {
        val postId = openCommentsFor.value ?: return
        viewModelScope.launch { noticeFailure(communityRepository.deleteComment(postId, id)) }
    }

    // ── 신고와 차단 ─────────────────────────────────────────────────

    fun askReport(post: Post) {
        _reportTarget.value = ReportTarget.OfPost(post.id, post.authorId, post.author)
    }

    fun askReport(comment: Comment) {
        _reportTarget.value =
            ReportTarget.OfComment(comment.postId, comment.id, comment.authorId, comment.author)
    }

    fun dismissReport() {
        _reportTarget.value = null
    }

    fun submitReport(reason: ReportReason) {
        val target = _reportTarget.value ?: return
        _reportTarget.value = null
        viewModelScope.launch {
            val result = when (target) {
                is ReportTarget.OfPost -> communityRepository.reportPost(target.postId, reason)
                is ReportTarget.OfComment ->
                    communityRepository.reportComment(target.postId, target.commentId, reason)
            }
            if (result is BoardResult.Ok) _boardNotice.value = BoardNotice.REPORTED else noticeFailure(result)
        }
    }

    /** 신고 창에서 — 쓴 사람을 차단한다. 그 사람의 글·댓글이 내 화면에서 사라진다. */
    fun blockReported() {
        val target = _reportTarget.value ?: return
        _reportTarget.value = null
        viewModelScope.launch {
            val result = communityRepository.block(target.authorId)
            if (result is BoardResult.Ok) _boardNotice.value = BoardNotice.BLOCKED else noticeFailure(result)
        }
    }

    /** 실패했으면 이유를 띄운다. 성공이면 아무것도 하지 않는다. */
    private fun noticeFailure(result: BoardResult) {
        if (result is BoardResult.Failed) {
            _boardNotice.value = if (result.signIn) BoardNotice.SIGN_IN else BoardNotice.FAILED
        }
    }

    fun crewOf(id: String): Crew? = crewRepository.crewOf(id)

    fun crewPosts(crewId: String): Flow<List<Post>> = communityRepository.crewPosts(crewId)

    fun selectTab(next: CommunityTab) {
        tab.value = next
    }

    fun refreshCrews() {
        viewModelScope.launch { crewRepository.refresh() }
    }

    fun consumeCrewNotice() {
        _crewNotice.value = null
    }

    /**
     * 크루 버튼 하나 — 멤버면 탈퇴, 신청 중이면 신청 취소, 아니면 가입(또는 신청).
     *
     * 크루장은 여기로 오지 않는다. 크루장이 나가면 주인 없는 크루가 남으므로
     * 화면이 탈퇴 버튼을 보여 주지 않고, 서버도 막는다.
     */
    fun toggleJoin(crewId: String) {
        val crew = crewRepository.crewOf(crewId)
        viewModelScope.launch {
            val result = when {
                crew?.joined == true -> crewRepository.leave(crewId)
                crew?.requested == true -> crewRepository.leave(crewId)
                else -> crewRepository.join(crewId)
            }
            // 크루에 들고 나면 볼 수 있는 크루 게시판 글도 바뀐다.
            communityRepository.refresh()
            val success = when {
                crew?.joined == true -> CrewNotice.LEFT
                crew?.requested == true -> CrewNotice.CANCELLED
                result == CrewActionResult.Requested -> CrewNotice.REQUESTED
                else -> CrewNotice.JOINED
            }
            report(result, success)
        }
    }

    /** 크루장 — 가입 방식 바꾸기 */
    fun setJoinPolicy(crewId: String, policy: CrewJoinPolicy) {
        viewModelScope.launch {
            report(crewRepository.setJoinPolicy(crewId, policy), CrewNotice.SAVED)
            // 자유 가입으로 열면 기다리던 신청이 모두 받아들여진다. 목록도 따라가야 한다.
            loadCrewRequests(crewId)
        }
    }

    /** 크루장 — 가입 신청 목록을 다시 받는다 */
    fun loadCrewRequests(crewId: String) {
        viewModelScope.launch {
            if (_crewRequests.value[crewId] !is CrewRequestsState.Ready) {
                _crewRequests.value = _crewRequests.value + (crewId to CrewRequestsState.Loading)
            }
            val next = when (val result = crewRepository.requests(crewId)) {
                is ServerResult.Ok -> CrewRequestsState.Ready(result.value)
                else -> CrewRequestsState.Failed
            }
            _crewRequests.value = _crewRequests.value + (crewId to next)
        }
    }

    /** 크루장 — 가입 신청 승인·거절 */
    fun decideCrewRequest(crewId: String, userId: String, approve: Boolean) {
        viewModelScope.launch {
            val result = crewRepository.decide(crewId, userId, approve)
            report(result, if (approve) CrewNotice.APPROVED else CrewNotice.REJECTED)
            loadCrewRequests(crewId)
        }
    }

    private fun report(result: CrewActionResult, success: CrewNotice) {
        _crewNotice.value = when (result) {
            is CrewActionResult.Failed -> if (result.signIn) CrewNotice.SIGN_IN else CrewNotice.FAILED
            else -> {
                ExperienceEvents.emit(FeedbackCue.Success)
                success
            }
        }
    }

    fun toggleLike(postId: Long) {
        viewModelScope.launch { noticeFailure(communityRepository.toggleLike(postId)) }
    }

    fun toggleJoinFlash(postId: Long) {
        viewModelScope.launch { noticeFailure(communityRepository.toggleJoinFlash(postId)) }
    }

    fun deletePost(postId: Long) {
        viewModelScope.launch { noticeFailure(communityRepository.delete(postId)) }
    }

    /**
     * 글쓰기. 서버가 받아 주면 [onDone] 을 부른다.
     *
     * 화면을 먼저 닫으면 이 뷰모델이 함께 사라지면서 요청도 끊긴다. 그래서
     * 올라간 것을 확인한 뒤에 닫는다.
     */
    fun writePost(
        category: PostCategory,
        title: String,
        body: String,
        crewId: String,
        place: String,
        distanceKm: Double,
        meetInMinutes: Int,
        capacity: Int,
        lat: Double?,
        lng: Double?,
        onDone: () -> Unit,
    ) {
        if (_posting.value) return
        _posting.value = true
        viewModelScope.launch {
            try {
            val result = communityRepository.write(
                category = category,
                title = title,
                body = body,
                crewId = crewId,
                place = place,
                distanceKm = distanceKm,
                meetInMinutes = meetInMinutes,
                capacity = capacity,
                lat = lat,
                lng = lng,
            )
            if (result is BoardResult.Ok) {
                ExperienceEvents.emit(FeedbackCue.Success)
                onDone()
            } else {
                noticeFailure(result)
            }
            } catch (cancelled: kotlinx.coroutines.CancellationException) {
                throw cancelled
            } catch (_: Exception) {
                _boardNotice.value = BoardNotice.FAILED
            } finally {
                _posting.value = false
            }
        }
    }

    private val _creatingCrew = kotlinx.coroutines.flow.MutableStateFlow(false)
    val creatingCrew: kotlinx.coroutines.flow.StateFlow<Boolean> = _creatingCrew

    fun createCrew(
        name: String,
        tagline: String,
        area: String,
        policy: CrewJoinPolicy,
        onCreated: (String) -> Unit,
    ) {
        if (name.isBlank() || _creatingCrew.value) return
        _creatingCrew.value = true
        viewModelScope.launch {
            try {
            when (val result = crewRepository.create(name, tagline, area, policy)) {
                is CrewActionResult.Created -> {
                    ExperienceEvents.emit(FeedbackCue.Success)
                    onCreated(result.crewId)
                }
                is CrewActionResult.Failed ->
                    _crewNotice.value = if (result.signIn) CrewNotice.SIGN_IN else CrewNotice.FAILED
                else -> Unit
            }
            } catch (cancelled: kotlinx.coroutines.CancellationException) {
                throw cancelled
            } catch (_: Exception) {
                _crewNotice.value = CrewNotice.FAILED
            } finally {
                _creatingCrew.value = false
            }
        }
    }

    companion object {
        val Factory = viewModelFactory {
            initializer {
                CommunityViewModel(
                    ServiceLocator.crewRepository,
                    ServiceLocator.communityRepository,
                    ServiceLocator.rankingRepository,
                    ServiceLocator.rewardRepository,
                    ServiceLocator.sneakerRepository,
                )
            }
        }
    }
}
