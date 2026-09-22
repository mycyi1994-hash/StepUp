package com.stepup.android

import com.stepup.android.data.remote.RunningFeedApi
import com.stepup.android.data.repo.EventFilter
import com.stepup.android.data.repo.NewsFilter
import com.stepup.android.ui.screens.feed.eventFilterCount
import com.stepup.android.ui.screens.feed.newsFilterCount
import com.stepup.android.ui.screens.items.EquipFilter
import com.stepup.android.ui.screens.items.itemFilterCount
import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * "필터 N"이 세는 것.
 *
 * 화면이 내놓는 이 숫자는 "기본과 다른 조건이 몇 묶음인가"다. 정렬은 제
 * 버튼이 따로 있으므로 여기 들어가지 않고, 검색어도 검색칸이 스스로
 * 보여 주므로 세지 않는다. 이 규칙이 흔들리면 "필터 3"이라고 적힌 화면을
 * 열었을 때 켜진 조건이 둘이거나 넷이 된다.
 */
class FilterCountTest {

    @Test
    fun `아무것도 안 고르면 0이다`() {
        assertEquals(0, eventFilterCount(EventFilter()))
        assertEquals(0, newsFilterCount(NewsFilter()))
        assertEquals(0, itemFilterCount(null, EquipFilter.ALL))
    }

    @Test
    fun `고른 묶음 수만큼 센다`() {
        val filter = EventFilter(eventType = "ROAD", distance = "HALF", region = "서울")
        assertEquals(3, eventFilterCount(filter))
        assertEquals(4, eventFilterCount(filter.copy(status = "OPEN")))
    }

    @Test
    fun `정렬은 세지 않는다`() {
        assertEquals(0, eventFilterCount(EventFilter(sort = "CLOSING")))
        assertEquals(0, newsFilterCount(NewsFilter(sort = "RELEVANCE")))
    }

    @Test
    fun `검색어는 세지 않는다`() {
        assertEquals(0, eventFilterCount(EventFilter(query = "마라톤")))
        assertEquals(0, newsFilterCount(NewsFilter(query = "회복")))
    }

    @Test
    fun `전체를 고른 것은 기본값이라 세지 않는다`() {
        val filter = EventFilter(
            eventType = RunningFeedApi.ALL,
            distance = RunningFeedApi.ALL,
            region = RunningFeedApi.ALL,
            status = RunningFeedApi.ALL,
        )
        assertEquals(0, eventFilterCount(filter))
    }

    @Test
    fun `뉴스는 주제와 출처 둘을 센다`() {
        assertEquals(1, newsFilterCount(NewsFilter(category = "TRAINING")))
        assertEquals(
            2,
            newsFilterCount(NewsFilter(category = "TRAINING", publisher = "sbs.co.kr")),
        )
    }

    @Test
    fun `아이템은 등급과 장착 상태 둘을 센다`() {
        assertEquals(1, itemFilterCount("RARE", EquipFilter.ALL))
        assertEquals(1, itemFilterCount(null, EquipFilter.ON))
        assertEquals(2, itemFilterCount("EPIC", EquipFilter.OFF))
    }
}
