package com.stepup.android.data.repo

import androidx.annotation.VisibleForTesting
import com.stepup.android.data.local.CommentDao
import com.stepup.android.data.local.NotificationDao
import com.stepup.android.data.local.NotificationType
import com.stepup.android.data.local.PostDao
import com.stepup.android.data.prefs.UserPrefs
import com.stepup.android.data.remote.CommentRow
import com.stepup.android.data.remote.CommunityApi
import com.stepup.android.data.remote.PostRow
import com.stepup.android.data.remote.ServerResult
import com.stepup.android.domain.Comment
import com.stepup.android.domain.CommentThread
import com.stepup.android.domain.FlashMember
import com.stepup.android.domain.Post
import com.stepup.android.domain.PostCategory
import com.stepup.android.domain.toThreads
import com.stepup.android.core.Analytics
import java.time.DayOfWeek
import java.time.Instant
import java.time.OffsetDateTime
import java.time.ZoneId
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

/**
 * 알림이 가리키는 댓글 한 개.
 *
 * 알림에는 문자열 한 칸(argExtra)만 있어서 "글 번호:댓글 번호"로 눌러 담는다.
 * 댓글 번호가 없던 시절의 알림(글 번호만 있는 것)도 그대로 읽힌다 — 이미
 * 알림함에 쌓여 있는 것들을 깨뜨리지 않기 위해서다.
 */
data class CommentTarget(val postId: Long, val commentId: Long) {
    fun encode(): String = "$postId:$commentId"

    companion object {
        fun decode(raw: String): CommentTarget? {
            val parts = raw.split(":")
            val post = parts.getOrNull(0)?.toLongOrNull() ?: return null
            return CommentTarget(post, parts.getOrNull(1)?.toLongOrNull() ?: 0L)
        }
    }
}

/** 게시판을 서버에서 받아 온 상태 */
sealed interface BoardSyncState {
    data object Idle : BoardSyncState
    data object Loading : BoardSyncState
    data object Ready : BoardSyncState

    /** 로그인하지 않았다. 게시판은 계정이 있어야 보인다. */
    data object SignInRequired : BoardSyncState

    /** 서버에 닿지 못했거나 거절당했다. */
    data class Failed(val reason: String) : BoardSyncState
}

/** 게시판에 무언가를 했을 때의 결말 */
sealed interface BoardResult {
    /** @param id 새로 생긴 글·댓글 번호. 해당 없으면 0. */
    data class Ok(val id: Long = 0L) : BoardResult

    /** @param signIn 로그인해야 할 수 있는 일이었다 */
    data class Failed(val reason: String, val signIn: Boolean = false) : BoardResult
}

/** 동네 이야기 목록에 잠깐 띄울 확인 메시지 */
sealed interface StoryNotice {
    val postId: Long

    data class Posted(override val postId: Long) : StoryNotice
    data class Edited(override val postId: Long) : StoryNotice
    data class Deleted(override val postId: Long) : StoryNotice

    /** 숨긴 글 — 되돌리기를 준다 */
    data class Hidden(override val postId: Long) : StoryNotice

    /** 쓰던 글을 임시저장했다 — [postId] 는 고치던 글 번호(새 글이면 0) */
    data class DraftSaved(override val postId: Long) : StoryNotice
}

/** 신고 사유 — 서버 `content_reports.reason` 과 같은 이름 */
enum class ReportReason { SPAM, ABUSE, SEXUAL, DANGER, FRAUD, OTHER }

/**
 * 커뮤니티 게시판.
 *
 * 전체 게시판(crewId = "")과 크루 게시판(crewId = 크루 ID)이 서버의 같은 표를
 * 쓴다. 예전에는 폰 안에 데모 글을 심고 데모 답글을 흉내 냈는데, 그러면 내가
 * 쓴 글을 아무도 볼 수 없었다. 이제 글·댓글·좋아요·번개 참가는 서버에서 한다.
 *
 * 목록은 메모리에 들고 있다. 쓰기는 서버 함수를 부르고, 결과를 목록에 바로
 * 반영하거나 목록을 다시 받는다.
 */
class CommunityRepository(
    private val api: CommunityApi,
    private val postDao: PostDao,
    private val commentDao: CommentDao,
    private val notificationDao: NotificationDao,
    private val prefs: UserPrefs,
    /** 지금 계정의 기록 주인(account:… / guest / legacy) — 쓰다 만 글 · 숨긴 글을 계정마다 나눈다 */
    private val owner: suspend () -> String = { "" },
    private val ownerFlow: Flow<String> = kotlinx.coroutines.flow.flowOf(""),
) {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    /**
     * 알림에서 눌러 들어온 댓글.
     *
     * 화면(뷰모델)마다 인스턴스가 달라서 알림함에서 게시판으로 값을 건네줄 방법이
     * 없다. 저장소는 앱에 하나뿐이므로 여기에 둔다. 게시판이 이 값을 보고
     * 댓글 창을 열어 그 댓글까지 스크롤한 뒤 비운다.
     */
    private val _commentFocus = MutableStateFlow<CommentTarget?>(null)
    val commentFocus: StateFlow<CommentTarget?> = _commentFocus

    fun focusComment(target: CommentTarget) {
        _commentFocus.value = target
    }

    fun clearCommentFocus() {
        _commentFocus.value = null
    }

    /** 알림이 가리키는 글을 지금 볼 수 있는가 — 서버가 없다고 답한 것과 닿지 못한 것을 구분한다 */
    suspend fun postVisible(postId: Long): ServerResult<Boolean> = api.postVisible(postId)

    private val _posts = MutableStateFlow<List<Post>>(emptyList())

    /** 내가 볼 수 있는 글 전부 — 전체 게시판과 내가 들어간 크루의 게시판 */
    val posts: StateFlow<List<Post>> = _posts

    private val _sync = MutableStateFlow<BoardSyncState>(BoardSyncState.Idle)
    val sync: StateFlow<BoardSyncState> = _sync

    private val refreshLock = Mutex()

    /** 전체 게시판 글만 */
    val boardPosts: Flow<List<Post>> = posts.map { list -> list.filter { it.crewId.isEmpty() } }

    fun crewPosts(crewId: String): Flow<List<Post>> =
        posts.map { list -> list.filter { it.crewId == crewId } }

    /** 내가 쓴 글 수 — 업적에 쓴다 */
    val myPostCount: Flow<Int> = posts.map { list -> list.count { it.mine } }

    /**
     * 예전 버전이 폰 안에 만들어 둔 데모 글·댓글을 지운다. 한 번만 일을 한다.
     *
     * 게시판이 서버로 옮겨 가면서 폰의 글은 더 쓰지 않는다. 남겨 두면 글 번호가
     * 서버 글과 겹쳐, 핫글과 "답글이 달렸어요" 알림이 엉뚱한 글을 가리킨다.
     */
    suspend fun clearLegacy() {
        if (postDao.count() == 0 && commentDao.count() == 0) return
        postDao.clear()
        commentDao.clear()
        notificationDao.deleteByType(NotificationType.COMMENT_REPLY)
        prefs.clearHotPosts()
    }

    /** 서버에서 글 목록을 다시 받는다. 동시에 여러 번 불려도 차례로 한 번씩 한다. */
    suspend fun refresh(): BoardSyncState = refreshLock.withLock {
        if (_posts.value.isEmpty()) _sync.value = BoardSyncState.Loading
        val next = when (val result = api.posts()) {
            is ServerResult.Ok -> {
                _posts.value = result.value.map { it.toDomain() }
                BoardSyncState.Ready
            }
            is ServerResult.SignInRequired -> BoardSyncState.SignInRequired
            is ServerResult.Rejected -> BoardSyncState.Failed(result.reason)
            is ServerResult.Retry -> BoardSyncState.Failed(result.reason)
        }
        _sync.value = next
        next
    }

    /** 좋아요를 누르거나 거둔다. 서버가 돌려준 상태를 그 글에 바로 반영한다. */
    suspend fun toggleLike(id: Long): BoardResult =
        when (val result = api.toggleLike(id)) {
            is ServerResult.Ok -> {
                val liked = result.value
                updatePost(id) {
                    if (it.liked == liked) it
                    else it.copy(liked = liked, likes = (it.likes + if (liked) 1 else -1).coerceAtLeast(0))
                }
                BoardResult.Ok()
            }
            else -> result.asBoardFailure()
        }

    /** 번개러닝 참가/취소. 정원은 서버가 지킨다 — 마지막 한 자리를 둘이 노려도 한 명만 들어간다. */
    suspend fun toggleJoinFlash(id: Long): BoardResult {
        val post = _posts.value.firstOrNull { it.id == id } ?: return BoardResult.Failed("")
        if (!post.isFlash) return BoardResult.Failed("")
        val joining = !post.joined
        val result = if (joining) api.joinFlash(id) else api.leaveFlash(id)
        return when (result) {
            is ServerResult.Ok -> {
                updatePost(id) { it.copy(joined = joining, joinedCount = result.value) }
                if (joining) Analytics.flashJoined()
                BoardResult.Ok()
            }
            else -> {
                // 정원이 찼거나 모임이 지났을 수 있다. 목록을 다시 받아 지금 상태를 보여 준다.
                refresh()
                result.asBoardFailure()
            }
        }
    }

    /**
     * 글쓰기.
     *
     * @param meetInMinutes 번개러닝이 몇 분 뒤에 모이는지
     */
    suspend fun write(
        category: PostCategory,
        title: String,
        body: String,
        crewId: String = "",
        place: String = "",
        distanceKm: Double = 0.0,
        meetInMinutes: Int = 0,
        capacity: Int = 0,
        lat: Double? = null,
        lng: Double? = null,
    ): BoardResult {
        val flash = category == PostCategory.FLASH
        val meetAt = if (flash) {
            Instant.ofEpochMilli(System.currentTimeMillis() + meetInMinutes.coerceAtLeast(1) * 60_000L)
                .toString()
        } else {
            ""
        }
        val result = api.createPost(
            category = category.id,
            crewId = crewId,
            title = title.trim(),
            body = body.trim(),
            place = place.trim(),
            distanceKm = distanceKm,
            meetAtIso = meetAt,
            capacity = if (flash) capacity.coerceAtLeast(2) else 0,
            lat = if (flash) lat else null,
            lng = if (flash) lng else null,
        )
        return when (result) {
            is ServerResult.Ok -> {
                Analytics.postWritten(category.id)
                refresh()
                BoardResult.Ok(result.value)
            }
            else -> result.asBoardFailure()
        }
    }

    suspend fun delete(id: Long): BoardResult =
        when (val result = api.deletePost(id)) {
            is ServerResult.Ok -> {
                _posts.value = _posts.value.filterNot { it.id == id }
                _comments.value = _comments.value - id
                BoardResult.Ok()
            }
            else -> result.asBoardFailure()
        }

    /** 신고. 5건이 모이면 서버가 모두의 목록에서 숨긴다. 신고한 사람에게는 바로 숨긴다. */
    suspend fun reportPost(id: Long, reason: ReportReason): BoardResult =
        when (val result = api.report("POST", id.toString(), reason.name)) {
            is ServerResult.Ok -> {
                _posts.value = _posts.value.filterNot { it.id == id }
                BoardResult.Ok()
            }
            else -> result.asBoardFailure()
        }

    suspend fun reportComment(postId: Long, commentId: Long, reason: ReportReason): BoardResult =
        when (val result = api.report("COMMENT", commentId.toString(), reason.name)) {
            is ServerResult.Ok -> {
                val left = _comments.value[postId].orEmpty().filterNot { it.id == commentId }
                _comments.value = _comments.value + (postId to left)
                BoardResult.Ok()
            }
            else -> result.asBoardFailure()
        }

    /** 차단. 그 사람의 글·댓글이 내 화면에서 사라진다. 상대에게는 알리지 않는다. */
    suspend fun block(userId: String): BoardResult {
        if (userId.isBlank()) return BoardResult.Failed("")
        return when (val result = api.block(userId)) {
            is ServerResult.Ok -> {
                _posts.value = _posts.value.filterNot { it.authorId == userId }
                _comments.value = _comments.value.mapValues { (_, list) ->
                    list.filterNot { it.authorId == userId }
                }
                BoardResult.Ok()
            }
            else -> result.asBoardFailure()
        }
    }

    private fun updatePost(id: Long, change: (Post) -> Post) {
        _posts.value = _posts.value.map { if (it.id == id) change(it) else it }
    }

    /** 화면 검사용 — 서버 없이 글 목록을 채운다. */
    @VisibleForTesting
    fun showForTest(list: List<Post>) {
        _posts.value = list
        _sync.value = BoardSyncState.Ready
    }

    /** 화면 검사용 — 빈 게시판의 서버/로그인 상태를 실제 화면에 표시한다. */
    @VisibleForTesting
    fun showBoardStateForTest(state: BoardSyncState) {
        _posts.value = emptyList()
        _sync.value = state
    }

    // ── 핫글 ─────────────────────────────────────────────────
    //
    // 매주 한 번, 그 주에 가장 많이 읽히고 이야기된 글 30개를 골라 둔다.
    //
    // 한 번 오른 글은 다시 오르지 않는다. 안 그러면 인기 글 몇 개가 자리를
    // 차지하고 앉아, 이번 주에 잘 쓴 글이 영영 못 올라온다. 핫글은 명예의
    // 전당이 아니라 "이번 주에 볼 만한 것"이어야 한다.

    /** 이번 주 핫글. 뽑힌 순서(점수 높은 순) 그대로 나온다. */
    val hotPosts: Flow<List<Post>> =
        combine(posts, prefs.hotPostIds) { all, ids ->
            val byId = all.associateBy { it.id }
            // 지워진 글은 조용히 빠진다. 목록에 남아 있어도 보여 줄 것이 없다.
            ids.mapNotNull { byId[it] }
        }

    /**
     * 갱신할 때가 됐으면 이번 주 핫글을 다시 뽑는다.
     *
     * 화면을 열 때마다 불러도 된다 — 갱신 시각이 지나지 않았으면 아무 일도
     * 하지 않는다.
     */
    suspend fun refreshHotIfDue(now: Long = System.currentTimeMillis()) {
        val due = lastHotRotation(now)
        if (prefs.hotRotatedAt() >= due) return

        val featured = prefs.hotFeaturedIds.first()
        // 아직 목록을 못 받았으면 뽑지 않는다. 빈 목록으로 뽑으면 이번 주
        // 핫글이 통째로 비고, 다음 갱신 시각까지 그대로 남는다.
        if (_posts.value.isEmpty() && refresh() != BoardSyncState.Ready) return
        val picked = _posts.value
            .filter { it.crewId.isEmpty() }
            .filter { it.id !in featured && hotScore(it) > 0 }
            // 점수가 같으면 최근 글을 앞에 둔다. 오래된 글이 계속 앞자리를
            // 차지하면 새 글은 같은 점수로는 절대 못 올라온다.
            .sortedWith(compareByDescending<Post> { hotScore(it) }.thenByDescending { it.createdAt })
            .take(HOT_LIMIT)
            .map { it.id }

        prefs.setHotPosts(picked, due)
    }

    /** 좋아요 5점, 댓글 10점. 댓글이 더 무거운 것은 쓰는 데 더 드는 값이기 때문이다. */
    fun hotScore(post: Post): Int =
        post.likes * HOT_LIKE_POINTS + post.commentCount * HOT_COMMENT_POINTS

    // ── 댓글 ─────────────────────────────────────────────────

    /** 글마다 받아 온 댓글 */
    private val _comments = MutableStateFlow<Map<Long, List<Comment>>>(emptyMap())

    /** 한 글의 댓글을 부모–답글 묶음으로. 보기 시작하면 서버에서 새로 받는다. */
    fun commentThreads(postId: Long): Flow<List<CommentThread>> =
        _comments
            .map { it[postId].orEmpty().toThreads() }
            .onStart { scope.launch { loadComments(postId) } }

    /** 번개 참가자 명단. 서버에 닿지 못하면 null — 빈 명단과 섞지 않는다. */
    suspend fun roster(postId: Long): List<FlashMember>? =
        (api.roster(postId) as? ServerResult.Ok)?.value?.map {
            FlashMember(userId = it.userId, name = it.name, isHost = it.isHost, isMe = it.isMe)
        }

    /** @return 서버에서 받아 왔으면 true */
    suspend fun loadComments(postId: Long): Boolean {
        val result = api.comments(postId)
        if (result is ServerResult.Ok) {
            val list = result.value.map { it.toDomain() }
            _comments.value = _comments.value + (postId to list)
            // 글 카드의 댓글 수가 방금 받은 목록과 어긋나지 않게 맞춘다.
            updatePost(postId) { it.copy(commentCount = list.size) }
            return true
        }
        return false
    }

    /**
     * 댓글/답글 작성.
     *
     * @param parentId 0이면 새 댓글, 그 외에는 그 댓글에 대한 답글
     */
    suspend fun addComment(postId: Long, body: String, parentId: Long = 0L): BoardResult {
        val text = body.trim()
        if (text.isEmpty()) return BoardResult.Failed("")
        return when (val result = api.createComment(postId, parentId, text)) {
            is ServerResult.Ok -> {
                loadComments(postId)
                BoardResult.Ok(result.value)
            }
            else -> result.asBoardFailure()
        }
    }

    suspend fun deleteComment(postId: Long, id: Long): BoardResult =
        when (val result = api.deleteComment(id)) {
            is ServerResult.Ok -> {
                loadComments(postId)
                BoardResult.Ok()
            }
            else -> result.asBoardFailure()
        }

    // ── 동네 이야기(목록형 커뮤니티) ──────────────────────────────
    //
    // 장소가 있는 일반 글. 서버는 0041 의 story_create · story_update 로 쓴다.
    // 숨기기와 쓰다 만 글은 이 폰에만, 계정마다 따로 둔다 — 숨기기는 차단(user_block)과 다르다.

    /** 지금 계정이 숨긴 글 번호 */
    @OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
    val hiddenStories: Flow<Set<Long>> = ownerFlow.flatMapLatest { prefs.hiddenStories(it) }

    /** 지금 계정이 쓰다 만 글 */
    @OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
    val storyDraft: Flow<com.stepup.android.domain.StoryDraft?> = ownerFlow.flatMapLatest { prefs.storyDraft(it) }

    suspend fun saveStoryDraft(draft: com.stepup.android.domain.StoryDraft?) = prefs.setStoryDraft(owner(), draft)

    suspend fun setStoryHidden(id: Long, hidden: Boolean) = prefs.setStoryHidden(owner(), id, hidden)

    /** 목록에 잠깐 띄울 확인 메시지 — 게시 · 수정 · 삭제 · 숨김(되돌리기) */
    private val _storyNotice = MutableStateFlow<StoryNotice?>(null)
    val storyNotice: StateFlow<StoryNotice?> = _storyNotice

    fun postStoryNotice(notice: StoryNotice) {
        _storyNotice.value = notice
    }

    fun consumeStoryNotice(notice: StoryNotice) {
        if (_storyNotice.value == notice) _storyNotice.value = null
    }

    /** 동네 이야기 삭제 — 서버가 지운 뒤에만 목록에서 뺀다(댓글도 서버에서 함께 지워진다) */
    suspend fun deleteStory(id: Long): BoardResult {
        val result = delete(id)
        if (result is BoardResult.Ok) _storyNotice.value = StoryNotice.Deleted(id)
        return result
    }

    /** 이 글 숨기기 — 이 폰 · 이 계정의 목록에서만. 글쓴이 차단과 다르다 */
    suspend fun hideStory(id: Long) {
        setStoryHidden(id, true)
        _storyNotice.value = StoryNotice.Hidden(id)
    }

    /**
     * 동네 이야기 쓰기. 서버가 받아 준 뒤에만 목록에 넣는다. 목록을 다시 받지 못해도 방금 쓴 글은
     * 서버가 준 번호로 목록 맨 위에 한 번만 보인다(다시 받으면 서버 줄로 바뀐다).
     */
    suspend fun writeStory(text: String, place: com.stepup.android.domain.StoryPlace): BoardResult {
        val clean = com.stepup.android.domain.StoryText.normalize(text)
        if (!com.stepup.android.domain.StoryText.canPost(clean)) return BoardResult.Failed("")
        val (title, body) = com.stepup.android.domain.StoryText.split(clean)
        return when (val result = api.createStory(title, body, place.name, place.address, place.lat, place.lng)) {
            is ServerResult.Ok -> {
                Analytics.postWritten("STORY")
                val id = result.value
                if (refresh() != BoardSyncState.Ready && _posts.value.none { it.id == id }) {
                    _posts.value = listOf(
                        Post(
                            id = id, category = PostCategory.FREE, crewId = "", author = "", title = title.trim(),
                            body = body, createdAt = System.currentTimeMillis(), likes = 0, liked = false,
                            commentCount = 0, mine = true, place = place.name, distanceKm = 0.0, meetAt = 0L,
                            capacity = 0, joinedCount = 0, joined = false, lat = place.lat, lng = place.lng,
                            placeAddress = place.address,
                        ),
                    ) + _posts.value
                }
                _storyNotice.value = StoryNotice.Posted(id)
                BoardResult.Ok(id)
            }
            else -> result.asBoardFailure()
        }
    }

    /** 내 동네 이야기 고치기 — 같은 글 번호 · 댓글 · 좋아요 그대로 */
    suspend fun editStory(id: Long, text: String, place: com.stepup.android.domain.StoryPlace): BoardResult {
        val clean = com.stepup.android.domain.StoryText.normalize(text)
        if (!com.stepup.android.domain.StoryText.canPost(clean)) return BoardResult.Failed("")
        val (title, body) = com.stepup.android.domain.StoryText.split(clean)
        return when (val result = api.updateStory(id, title, body, place.name, place.address, place.lat, place.lng)) {
            is ServerResult.Ok -> {
                updatePost(id) {
                    it.copy(
                        title = title.trim(), body = body, place = place.name, placeAddress = place.address,
                        lat = place.lat, lng = place.lng,
                    )
                }
                _storyNotice.value = StoryNotice.Edited(id)
                BoardResult.Ok(id)
            }
            else -> result.asBoardFailure()
        }
    }

    private val likeInFlight = mutableSetOf<Long>()

    /**
     * 좋아요 — 누르는 순간 표시와 개수를 함께 바꾸고, 서버가 거절하면 되돌린다.
     * 같은 글에 요청이 가는 동안의 두 번째 누름은 무시한다(개수가 어긋나지 않게).
     */
    suspend fun toggleStoryLike(id: Long): BoardResult {
        val before = _posts.value.firstOrNull { it.id == id } ?: return BoardResult.Failed("")
        synchronized(likeInFlight) { if (!likeInFlight.add(id)) return BoardResult.Ok() }
        try {
            val wanted = !before.liked
            updatePost(id) { it.copy(liked = wanted, likes = (it.likes + if (wanted) 1 else -1).coerceAtLeast(0)) }
            return when (val result = api.toggleLike(id)) {
                is ServerResult.Ok -> {
                    val liked = result.value
                    // 서버가 다른 상태를 돌려주면(다른 기기에서 눌렀다) 그 상태에 맞춘다
                    updatePost(id) { now ->
                        if (now.liked == liked) now
                        else now.copy(liked = liked, likes = (now.likes + if (liked) 1 else -1).coerceAtLeast(0))
                    }
                    BoardResult.Ok()
                }
                else -> {
                    updatePost(id) { it.copy(liked = before.liked, likes = before.likes) }
                    result.asBoardFailure()
                }
            }
        } finally {
            synchronized(likeInFlight) { likeInFlight.remove(id) }
        }
    }

    /**
     * 동네 이야기 댓글 — 성공하면 목록을 다시 받는다. 다시 받지 못하면 서버가 준 번호로 내 댓글을
     * 한 번만 더한다(두 번 세지 않게).
     */
    suspend fun addStoryComment(postId: Long, body: String): BoardResult {
        val text = body.trim()
        if (text.isEmpty()) return BoardResult.Failed("")
        return when (val result = api.createComment(postId, 0L, text)) {
            is ServerResult.Ok -> {
                val id = result.value
                if (!loadComments(postId)) {
                    val now = _comments.value[postId].orEmpty()
                    if (now.none { it.id == id }) {
                        val mine = Comment(
                            id = id, postId = postId, parentId = 0L, author = "", body = text,
                            createdAt = System.currentTimeMillis(), mine = true,
                        )
                        _comments.value = _comments.value + (postId to now + mine)
                        updatePost(postId) { it.copy(commentCount = it.commentCount + 1) }
                    }
                }
                BoardResult.Ok(id)
            }
            else -> result.asBoardFailure()
        }
    }

    /**
     * 신고 — 시안의 사유를 서버 사유로 명시적으로 바꿔 보낸다. 시안대로 접수 뒤에도 글은 그대로 보인다
     * (확인 후 글로 돌아간다). 안 보고 싶으면 "이 글 숨기기"가 따로 있다. 5건이 쌓이면 서버가 목록에서 뺀다.
     */
    suspend fun reportStory(id: Long, reason: com.stepup.android.domain.StoryReportReason): BoardResult =
        when (val result = api.report("POST", id.toString(), reason.serverReason, reason.note)) {
            is ServerResult.Ok -> BoardResult.Ok()
            else -> result.asBoardFailure()
        }

    /** 화면 검사용 — 서버 없이 댓글을 채운다. */
    @VisibleForTesting
    fun showCommentsForTest(postId: Long, list: List<Comment>) {
        _comments.value = _comments.value + (postId to list)
    }

    companion object HotRules {
        /** 핫글에 올릴 글 수 */
        const val HOT_LIMIT = 30

        const val HOT_LIKE_POINTS = 5
        const val HOT_COMMENT_POINTS = 10

        /** 갱신 시각 — 한국 시간 월요일 09:00 */
        private val ROTATION_ZONE: ZoneId = ZoneId.of("Asia/Seoul")
        private val ROTATION_DAY: DayOfWeek = DayOfWeek.MONDAY
        private const val ROTATION_HOUR = 9

        /**
         * [now] 기준으로 가장 최근에 지나온 갱신 시각.
         *
         * 이 값이 곧 "이번 주 핫글"의 이름표다. 저장된 값이 이것보다 오래됐으면
         * 새로 뽑을 때가 된 것이다. 앱이 그 시각에 꺼져 있었어도, 다음에 켤 때
         * 같은 값이 나오므로 한 주를 통째로 건너뛰지 않는다.
         */
        fun lastHotRotation(now: Long): Long {
            val zoned = Instant.ofEpochMilli(now).atZone(ROTATION_ZONE)
            var boundary = zoned.with(ROTATION_DAY)
                .withHour(ROTATION_HOUR)
                .withMinute(0)
                .withSecond(0)
                .withNano(0)
            if (boundary.isAfter(zoned)) boundary = boundary.minusWeeks(1)
            return boundary.toInstant().toEpochMilli()
        }
    }
}

/** ISO-8601 시각(서버 timestamptz) → epoch 밀리초. 못 읽으면 0. */
internal fun String?.isoToMillis(): Long =
    this?.let { runCatching { OffsetDateTime.parse(it).toInstant().toEpochMilli() }.getOrNull() } ?: 0L

/** 서버 줄 → 도메인 모델 */
fun PostRow.toDomain(): Post = Post(
    id = id,
    category = PostCategory.of(category),
    crewId = crewId.orEmpty(),
    author = author,
    authorId = authorId,
    title = title,
    body = body,
    createdAt = createdAt.isoToMillis(),
    likes = likes,
    liked = liked,
    commentCount = commentCount,
    mine = mine,
    place = place,
    distanceKm = distanceKm,
    meetAt = meetAt.isoToMillis(),
    capacity = capacity,
    joinedCount = joinedCount,
    joined = joined,
    lat = lat,
    lng = lng,
    placeAddress = placeAddress,
)

/** 서버 줄 → 도메인 모델 */
fun CommentRow.toDomain(): Comment = Comment(
    id = id,
    postId = postId,
    parentId = parentId,
    author = author,
    authorId = authorId,
    body = body,
    createdAt = createdAt.isoToMillis(),
    mine = mine,
)

/** 서버가 받아 주지 않은 결말을 화면이 쓰는 실패로 */
internal fun ServerResult<*>.asBoardFailure(): BoardResult.Failed = when (this) {
    is ServerResult.SignInRequired -> BoardResult.Failed(reason, signIn = true)
    is ServerResult.Rejected -> BoardResult.Failed(reason)
    is ServerResult.Retry -> BoardResult.Failed(reason)
    is ServerResult.Ok -> BoardResult.Failed("")
}
