package com.stepup.android

import com.stepup.android.domain.RunCourse
import org.junit.Assert.assertEquals
import org.junit.Test

/** 화면의 코스 보상은 서버 규칙(0024 course_run_submit)과 같아야 한다 */
class CourseRewardDisplayTest {
    private fun course(id: Long, km: Double, mine: Boolean = false) = RunCourse(
        id = id, name = "c", area = "", distanceKm = km, elevationM = 0, points = emptyList(),
        author = "a", mine = mine, shared = true, likes = 0, liked = false, runCount = 0, createdAt = 0,
    )

    @Test fun onlyOtherPeoplesBoardCoursesPay() {
        val board = RunCourse.SERVER_ID_BASE + 7
        assertEquals(3.0, course(board, 3.7).serverReward, 0.0)   // 서버처럼 km 를 버린다
        assertEquals(42.0, course(board, 60.0).serverReward, 0.0) // 최대 42
        assertEquals(0.0, course(board, 5.0, mine = true).serverReward, 0.0) // 내 코스
        assertEquals(0.0, course(12, 5.0).serverReward, 0.0) // 폰의 체험 코스
    }
}
