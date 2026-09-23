package com.stepup.android.data.repo

import com.stepup.android.data.local.CourseDao
import com.stepup.android.data.local.CourseEntity
import com.stepup.android.data.local.NotificationType
import com.stepup.android.data.local.RewardType
import com.stepup.android.data.prefs.UserPrefs
import com.stepup.android.domain.CourseRewards
import com.stepup.android.domain.DemoCourses
import com.stepup.android.domain.GeoPoint
import com.stepup.android.domain.RunCourse
import com.stepup.android.domain.simplify
import com.stepup.android.domain.trackDistanceKm
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

/**
 * 러닝 코스 — 선택 · 내 코스 등록 · 게시판 공유 · 완주 보상.
 *
 * 백엔드가 없으므로 "게시판"은 shared 플래그가 켜진 로컬 코스들이고,
 * 첫 실행 때 실제 서울 러닝 명소를 본뜬 데모 코스를 심는다.
 */
class CourseRepository(
    private val dao: CourseDao,
    private val prefs: UserPrefs,
    private val rewardRepository: RewardRepository,
) {

    val courses: Flow<List<RunCourse>> = dao.observeAll().map { list -> list.map { it.toDomain() } }

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

    /** 방금 달린 GPS 트랙을 코스로 등록한다. @return 새 코스 id (트랙이 짧으면 null) */
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
                shared = shared,
                likes = 0,
                liked = false,
                runCount = 0,
                createdAt = System.currentTimeMillis(),
            )
        )
        return id
    }

    suspend fun setShared(id: Long, shared: Boolean) {
        val entity = dao.byId(id) ?: return
        if (!entity.mine) return
        dao.update(entity.copy(shared = shared))
    }

    suspend fun toggleLike(id: Long) {
        val entity = dao.byId(id) ?: return
        dao.update(
            entity.copy(
                liked = !entity.liked,
                likes = (entity.likes + if (entity.liked) -1 else 1).coerceAtLeast(0),
            )
        )
    }

    suspend fun delete(id: Long) {
        dao.deleteMine(id)
        if (prefs.selectedCourseNow() == id) prefs.setSelectedCourse(-1L)
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
     * 데모 시드 — 큰 공원 안을 도는 고리.
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
                author = park.author,
                mine = false,
                shared = true,
                likes = park.likes,
                liked = false,
                runCount = park.runs,
                createdAt = now - park.hoursAgo * 3_600_000L,
            )
        }
    }
}

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
