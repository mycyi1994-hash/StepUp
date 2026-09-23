package com.stepup.android.data.repo

import com.stepup.android.data.local.CourseDao
import com.stepup.android.data.local.CourseEntity
import com.stepup.android.data.local.NotificationType
import com.stepup.android.data.local.RewardType
import com.stepup.android.data.prefs.UserPrefs
import com.stepup.android.data.remote.CourseApi
import com.stepup.android.data.remote.CourseRow
import com.stepup.android.data.remote.ServerResult
import com.stepup.android.domain.CourseRewards
import com.stepup.android.domain.DemoCourses
import com.stepup.android.domain.GeoPoint
import com.stepup.android.domain.RunCourse
import com.stepup.android.domain.simplify
import com.stepup.android.domain.trackDistanceKm
import com.stepup.android.core.Analytics
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

/**
 * 러닝 코스 — 선택 · 내 코스 등록 · 게시판 공유 · 완주 보상.
 *
 * 코스는 폰(Room)에 있다 — 내가 뛰어 만든 코스, StepUp 이 까는 공원 코스,
 * 게시판에서 골라 받아 둔 코스. 게시판은 서버에 있다. 내가 코스를 "공유"하면
 * 서버에 올라가고 남의 게시판에도 뜬다.
 *
 * 게시판의 서버 코스는 [REMOTE_BASE] 를 더한 번호로 다닌다. 폰의 코스 번호와
 * 겹치지 않게 하려는 것이다. 달리기로 고르면 폰에 한 벌 받아 두고 그 번호로
 * 고른다 — 러닝 화면과 완주 보상은 폰의 코스만 본다.
 */
class CourseRepository(
    private val dao: CourseDao,
    private val prefs: UserPrefs,
    private val rewardRepository: RewardRepository,
    private val api: CourseApi,
) {

    val courses: Flow<List<RunCourse>> = dao.observeAll().map { list -> list.map { it.toDomain() } }

    private val _remote = MutableStateFlow<List<CourseRow>>(emptyList())
    private val _boardSync = MutableStateFlow<BoardSyncState>(BoardSyncState.Idle)
    val boardSync: StateFlow<BoardSyncState> = _boardSync
    private val refreshLock = Mutex()

    /**
     * 코스 게시판 — 서버의 공유 코스와, 아직 서버에 없는 폰의 공유 코스(기본
     * 공원 코스, 올리지 못한 내 코스). 같은 길이 둘 다에 있으면 서버 쪽을 쓴다.
     */
    val board: Flow<List<RunCourse>> = combine(courses, _remote) { local, remote ->
        val onServer = remote.mapTo(HashSet()) { it.track }
        remote.map { it.toDomain() } + local.filter { it.shared && it.encode() !in onServer }
    }

    /** 게시판을 서버에서 새로 받는다. */
    suspend fun refreshBoard(): BoardSyncState = refreshLock.withLock {
        if (_remote.value.isEmpty()) _boardSync.value = BoardSyncState.Loading
        val state = when (val result = api.board()) {
            is ServerResult.Ok -> {
                _remote.value = result.value
                BoardSyncState.Ready
            }
            is ServerResult.SignInRequired -> BoardSyncState.SignInRequired
            is ServerResult.Rejected -> BoardSyncState.Failed(result.reason)
            is ServerResult.Retry -> BoardSyncState.Failed(result.reason)
        }
        _boardSync.value = state
        state
    }

    /** 지금 달리기로 고른 코스 */
    val selectedCourse: Flow<RunCourse?> = combine(courses, prefs.selectedCourseId) { list, id ->
        list.firstOrNull { it.id == id }
    }

    /**
     * 코스 녹화 중인가 — "코스 만들기"를 누르고 아직 저장하지 않은 상태.
     *
     * 코스는 "실제로 뛴 길"이라야 지도 위에서 말이 된다. 손으로 선을 그으면
     * 건물이나 강을 가로지르는 코스가 나오고, 그걸 받은 사람은 뛸 수 없다.
     * 그래서 코스 만들기는 러닝 화면으로 보내 한 번 뛰게 한다.
     */
    val recording: Flow<Boolean> = prefs.courseRecording

    suspend fun select(id: Long) = prefs.setSelectedCourse(id)

    /**
     * 게시판 번호를 폰의 코스 번호로. 서버 코스면 폰에 한 벌 받아 둔다 —
     * 같은 길이 이미 있으면 그걸 쓴다.
     *
     * @return 폰의 코스 번호. 없어진 코스면 null
     */
    suspend fun localIdFor(id: Long): Long? {
        if (id < REMOTE_BASE) return id
        val row = _remote.value.firstOrNull { it.id == id - REMOTE_BASE } ?: return null
        dao.byTrack(row.track)?.let { return it.id }
        val points = RunCourse.decode(row.track)
        if (points.size < 2) return null
        return dao.insert(
            CourseEntity(
                name = row.name,
                area = row.area,
                distanceKm = row.distanceKm,
                elevationM = row.elevationM,
                track = row.track,
                author = row.author,
                mine = false,
                // 받아 둔 코스는 게시판에 다시 올라가지 않는다 — 원래 코스가 거기 있다
                shared = false,
                likes = row.likes,
                liked = row.liked,
                runCount = row.runCount,
                createdAt = row.createdAt.isoToMillis(),
            )
        )
    }

    /**
     * 녹화 시작 — 다음 러닝이 코스가 된다.
     *
     * 고른 코스가 있으면 푼다. 남의 코스를 따라 뛰면서 동시에 새 코스를
     * 만들면, 끝났을 때 완주 보상이 어느 코스 것인지가 흐려진다.
     */
    suspend fun beginRecording() {
        prefs.setSelectedCourse(-1L)
        prefs.setCourseRecording(true)
    }

    suspend fun cancelRecording() = prefs.setCourseRecording(false)

    /** 녹화한 트랙을 코스로 저장하고 녹화를 끝낸다. @return 새 코스 id (트랙이 짧으면 null) */
    suspend fun saveRecorded(
        name: String,
        area: String,
        track: List<GeoPoint>,
        shared: Boolean,
    ): Long? {
        val id = create(name, area, track, shared)
        prefs.setCourseRecording(false)
        return id
    }

    suspend fun clearSelection() = prefs.setSelectedCourse(-1L)

    suspend fun selectedCourseNow(): RunCourse? {
        val id = prefs.selectedCourseNow()
        if (id < 0) return null
        return dao.byId(id)?.toDomain()
    }

    /**
     * 방금 달린 GPS 트랙을 코스로 등록한다. [shared] 면 서버에도 올린다 —
     * 올리지 못하면 폰에만 남고 공유는 꺼진다.
     *
     * @return 새 코스 id (트랙이 짧으면 null)
     */
    suspend fun create(
        name: String,
        area: String,
        track: List<GeoPoint>,
        shared: Boolean,
    ): Long? {
        val slim = track.simplify()
        val km = slim.trackDistanceKm()
        if (slim.size < 2 || km < 0.2) return null
        val id = dao.insert(
            CourseEntity(
                name = name.trim().ifEmpty { "Course" },
                area = area.trim(),
                distanceKm = km,
                // 고도 센서가 없으므로 데모 추정치 — 1km당 완만한 8m
                elevationM = (km * 8).toInt(),
                track = slim.joinToString(";") { "${it.lat},${it.lng}" },
                author = "",
                mine = true,
                shared = false,
                likes = 0,
                liked = false,
                runCount = 0,
                createdAt = System.currentTimeMillis(),
            )
        )
        if (shared) setShared(id, true)
        return id
    }

    /**
     * 내 코스를 게시판에 올리거나 내린다. 서버가 받아 준 뒤에야 폰의 표시를
     * 바꾼다 — 먼저 바꾸면 "공유됨"인데 아무도 못 보는 코스가 생긴다.
     */
    suspend fun setShared(id: Long, shared: Boolean): BoardResult {
        val entity = dao.byId(id) ?: return BoardResult.Failed("")
        if (!entity.mine) return BoardResult.Failed("")
        val result = if (shared) {
            api.share(entity.name, entity.area, entity.distanceKm, entity.elevationM, entity.track)
        } else {
            api.unshare(entity.track)
        }
        if (result !is ServerResult.Ok) return result.asBoardFailure()
        dao.update(entity.copy(shared = shared))
        if (shared) Analytics.courseShared()
        refreshBoard()
        return BoardResult.Ok()
    }

    /**
     * 하트. 게시판의 서버 코스는 서버에 누르고, 폰에만 있는 코스(기본 공원
     * 코스, 받아 둔 코스)는 나만 보는 표시다.
     */
    suspend fun toggleLike(id: Long): BoardResult {
        if (id >= REMOTE_BASE) {
            val serverId = id - REMOTE_BASE
            return when (val result = api.toggleLike(serverId)) {
                is ServerResult.Ok -> {
                    _remote.value = _remote.value.map { row ->
                        if (row.id != serverId || row.liked == result.value) row
                        else row.copy(
                            liked = result.value,
                            likes = (row.likes + if (result.value) 1 else -1).coerceAtLeast(0),
                        )
                    }
                    BoardResult.Ok()
                }
                else -> result.asBoardFailure()
            }
        }
        val entity = dao.byId(id) ?: return BoardResult.Failed("")
        dao.update(
            entity.copy(
                liked = !entity.liked,
                likes = (entity.likes + if (entity.liked) -1 else 1).coerceAtLeast(0),
            )
        )
        return BoardResult.Ok()
    }

    /** 코스 지우기. 게시판에 올린 코스면 서버에서도 내린다. */
    suspend fun delete(id: Long): BoardResult {
        val entity = dao.byId(id) ?: return BoardResult.Ok()
        if (entity.mine && entity.shared) {
            val result = api.unshare(entity.track)
            if (result !is ServerResult.Ok) return result.asBoardFailure()
        }
        dao.deleteLocal(id)
        if (prefs.selectedCourseNow() == id) prefs.setSelectedCourse(-1L)
        if (entity.mine && entity.shared) refreshBoard()
        return BoardResult.Ok()
    }

    /**
     * 코스 완주 정산 — 거리 1km당 [CourseRewards.SUP_PER_KM] SUP 정량 지급.
     * 세션 거리가 코스 거리의 98% 이상이면 완주로 인정한다(GPS 오차 허용).
     */
    suspend fun grantCompletionIfFinished(sessionKm: Double): RunCourse? {
        val course = selectedCourseNow() ?: return null
        if (sessionKm < course.distanceKm * 0.98) return null
        rewardRepository.credit(
            RewardType.EARN_EVENT,
            course.reward,
            "코스 완주: ${course.name}",
        )
        rewardRepository.notify(
            type = NotificationType.COURSE_COMPLETE,
            argText = course.name,
            argAmount = course.reward,
        )
        dao.byId(course.id)?.let { dao.update(it.copy(runCount = it.runCount + 1)) }
        return course
    }

    /**
     * 데모 코스를 심는다. 이미 최신 판이 들어 있으면 아무것도 하지 않는다.
     *
     * 판 번호가 다르면 **데모 코스만** 갈아 끼운다. 내가 만든 코스는 그대로
     * 둔다 — 데모를 고치자고 사용자가 직접 뛰어서 만든 코스를 지울 수는 없다.
     */
    suspend fun ensureSeeded() {
        val installed = prefs.courseSeedVersion()
        if (installed == DemoCourses.VERSION && dao.count() > 0) return

        if (installed != DemoCourses.VERSION) {
            dao.deleteSeeded()
            // 지운 코스를 고른 채로 두면 러닝 화면이 없는 코스를 가리킨다
            val selected = prefs.selectedCourseNow()
            if (selected >= 0 && dao.byId(selected) == null) prefs.setSelectedCourse(-1L)
        }
        dao.insertAll(seedCourses())
        prefs.setCourseSeedVersion(DemoCourses.VERSION)
    }

    /**
     * 기본 코스 — 큰 공원 안을 도는 고리. 만든 사람은 StepUp 이고, 하트와
     * 완주 수는 0 에서 시작한다. 남이 누른 것처럼 지어내지 않는다.
     *
     *
     * 좌표를 손으로 찍지 않고 [DemoCourses] 가 공원 상자 안에서 만들어 준다.
     * 손으로 찍으면 점 사이가 멀어 선이 각지고, 조금만 빗나가도 강이나 건물
     * 위를 지난다 — 예전 데모 코스가 한강을 가로지르던 이유다.
     */
    private fun seedCourses(): List<CourseEntity> {
        val now = System.currentTimeMillis()
        return DemoCourses.parks.map { park ->
            val points = park.track()
            val km = points.trackDistanceKm()
            CourseEntity(
                name = park.name,
                area = park.area,
                distanceKm = km,
                elevationM = (km * 8).toInt(),
                track = points.joinToString(";") { "${it.lat},${it.lng}" },
                author = DemoCourses.AUTHOR,
                mine = false,
                shared = true,
                likes = 0,
                liked = false,
                runCount = 0,
                createdAt = now,
            )
        }
    }

    companion object {
        /** 게시판의 서버 코스 번호에 더하는 값. 폰의 코스 번호는 여기까지 가지 않는다. */
        const val REMOTE_BASE = 1_000_000_000_000L
    }
}

/** 서버 줄 → 도메인 모델. 번호는 [CourseRepository.REMOTE_BASE] 를 더해 폰의 코스와 가른다. */
fun CourseRow.toDomain(): RunCourse = RunCourse(
    id = CourseRepository.REMOTE_BASE + id,
    name = name,
    area = area,
    distanceKm = distanceKm,
    elevationM = elevationM,
    points = RunCourse.decode(track),
    author = author,
    mine = mine,
    shared = true,
    likes = likes,
    liked = liked,
    runCount = runCount,
    createdAt = createdAt.isoToMillis(),
)

/** Room 엔티티 → 도메인 모델 */
fun CourseEntity.toDomain(): RunCourse = RunCourse(
    id = id,
    name = name,
    area = area,
    distanceKm = distanceKm,
    elevationM = elevationM,
    points = RunCourse.decode(track),
    author = author,
    mine = mine,
    shared = shared,
    likes = likes,
    liked = liked,
    runCount = runCount,
    createdAt = createdAt,
)
