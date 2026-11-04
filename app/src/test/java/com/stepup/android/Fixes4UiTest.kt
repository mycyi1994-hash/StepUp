package com.stepup.android

import com.stepup.android.data.remote.RunningFeedApi
import com.stepup.android.data.repo.BoardRefusal
import com.stepup.android.data.repo.boardRefusalCapacity
import com.stepup.android.ui.components.showsKoreanOnlyText
import com.stepup.android.ui.screens.feed.RunningFeedViewModel
import com.stepup.android.ui.screens.feed.regionRes
import com.stepup.android.ui.screens.feed.weekdayInitials
import com.stepup.android.ui.screens.walk.RoomCreateRefusal
import com.stepup.android.ui.screens.walk.RunMenuLocks
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.Locale

/** 2026-11-04 화면 글자 · 언어 고침 4차 — 서버 한국어 문장을 번역된 갈래로, 한국어만 있는 글 숨기기, 스토어 판 잠긴 자리 */
class Fixes4UiTest {

    @Test
    fun `게시판 서버 거절 문장을 갈래로 옮긴다`() {
        assertEquals(BoardRefusal.POST_TOO_OFTEN, BoardRefusal.of("글을 너무 자주 쓰고 있습니다. 잠시 뒤에 다시 써 주세요"))
        assertEquals(BoardRefusal.COMMENT_TOO_OFTEN, BoardRefusal.of("댓글을 너무 자주 쓰고 있습니다. 잠시 뒤에 다시 써 주세요"))
        assertEquals(BoardRefusal.NOT_MY_POST_DELETE, BoardRefusal.of("내가 쓴 글만 지울 수 있습니다"))
        assertEquals(BoardRefusal.NOT_MY_POST_EDIT, BoardRefusal.of("내가 쓴 글만 고칠 수 있습니다"))
        assertEquals(BoardRefusal.NOT_MY_COMMENT, BoardRefusal.of("내가 쓴 댓글만 지울 수 있습니다"))
        assertEquals(BoardRefusal.FLASH_FULL, BoardRefusal.of("정원이 찼습니다 (8명)"))
        assertEquals(BoardRefusal.FLASH_PAST, BoardRefusal.of("이미 지난 모임입니다"))
        assertEquals(BoardRefusal.FLASH_NEEDS_TIME, BoardRefusal.of("번개러닝은 앞으로의 모임 시각이 있어야 합니다"))
        assertEquals(BoardRefusal.NOT_CREW_MEMBER, BoardRefusal.of("이 크루의 멤버가 아닙니다"))
        assertEquals(BoardRefusal.POST_GONE, BoardRefusal.of("글을 찾을 수 없습니다"))
        assertEquals(BoardRefusal.POST_GONE, BoardRefusal.of("번개러닝 글을 찾을 수 없습니다"))
        assertEquals(BoardRefusal.POST_GONE, BoardRefusal.of("답글을 달 댓글을 찾을 수 없습니다"))
        assertEquals(BoardRefusal.BLOCK_SELF, BoardRefusal.of("나 자신은 차단할 수 없습니다"))
        assertEquals(BoardRefusal.INVALID_INPUT, BoardRefusal.of("모임 장소 좌표가 올바르지 않습니다"))
        assertEquals(BoardRefusal.INVALID_INPUT, BoardRefusal.of("게시판 종류가 올바르지 않습니다"))
        assertEquals(BoardRefusal.TOO_LONG, BoardRefusal.of("첫 줄이 너무 깁니다"))
        assertEquals(BoardRefusal.EMPTY, BoardRefusal.of("내용을 적어 주세요"))
        assertEquals(BoardRefusal.SIGN_IN, BoardRefusal.of("로그인이 필요합니다"))
    }

    @Test
    fun `모르는 거절 문장은 일반 안내로 — 서버 문장을 그대로 띄우지 않는다`() {
        assertEquals(BoardRefusal.OTHER, BoardRefusal.of("요청이 거절되었습니다 (400)"))
        assertEquals(BoardRefusal.OTHER, BoardRefusal.of("new row violates check constraint"))
        assertEquals(BoardRefusal.OTHER, BoardRefusal.of(""))
    }

    @Test
    fun `정원 문장에서 정원 수를 꺼낸다`() {
        assertEquals(8, boardRefusalCapacity("정원이 찼습니다 (8명)"))
        assertNull(boardRefusalCapacity("정원이 찼습니다"))
    }

    @Test
    fun `대기실 만들기 거절은 코드와 문장 모두 갈래로`() {
        assertEquals(RoomCreateRefusal.DONE_TODAY, RoomCreateRefusal.of("crew_run_done_today"))
        assertEquals(RoomCreateRefusal.OPTIONS, RoomCreateRefusal.of("room_options_invalid"))
        assertEquals(RoomCreateRefusal.NAME_INVALID, RoomCreateRefusal.of("room_name_invalid"))
        assertEquals(RoomCreateRefusal.COURSE_INVALID, RoomCreateRefusal.of("course_invalid"))
        assertEquals(RoomCreateRefusal.ROOM_CLOSED, RoomCreateRefusal.of("room_closed"))
        assertEquals(RoomCreateRefusal.NOT_MEMBER, RoomCreateRefusal.of("크루원만 대기실을 열 수 있습니다"))
        assertEquals(RoomCreateRefusal.SIGN_IN, RoomCreateRefusal.of("로그인이 필요합니다"))
        assertEquals(RoomCreateRefusal.OTHER, RoomCreateRefusal.of("알 수 없는 문장"))
    }

    @Test
    fun `한국어로만 적힌 설명은 한국어 화면에서만 보인다`() {
        assertTrue(showsKoreanOnlyText("ko"))
        assertTrue(showsKoreanOnlyText("ko-KR"))
        assertFalse(showsKoreanOnlyText("en"))
        assertFalse(showsKoreanOnlyText("ja"))
        assertFalse(showsKoreanOnlyText("zh"))
    }

    @Test
    fun `요일은 월요일부터 화면 언어로`() {
        assertEquals(listOf("월", "화", "수", "목", "금", "토", "일"), weekdayInitials(Locale.KOREAN))
        val en = weekdayInitials(Locale.ENGLISH)
        assertEquals(7, en.size)
        assertEquals("M", en.first())
        assertEquals("S", en.last())
    }

    @Test
    fun `시 · 도 열쇠마다 화면 이름이 있다`() {
        RunningFeedViewModel.REGIONS.forEach { assertNotNull(it, regionRes(it)) }
        assertNotNull(regionRes(RunningFeedApi.ALL))
        assertNull(regionRes("모르는 곳"))
    }

    @Test
    fun `스토어 판은 잠긴 자리를 숨기고 테스트 판은 자물쇠로 보인다`() {
        val store = RunMenuLocks.showsLocked(store = true)
        val test = RunMenuLocks.showsLocked(store = false)
        assertFalse(RunMenuLocks.shows(locked = true, showLocked = store))
        assertTrue(RunMenuLocks.shows(locked = false, showLocked = store))
        assertTrue(RunMenuLocks.shows(locked = true, showLocked = test))
        assertTrue(RunMenuLocks.shows(locked = false, showLocked = test))
    }
}
