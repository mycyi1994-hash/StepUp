package com.stepup.android.data.repo

import com.stepup.android.domain.Post
import com.stepup.android.domain.PostCategory
import org.junit.Assert.assertEquals
import org.junit.Test

/** 전체 목록(최근 200)과 따로 받은 크루 · 내 글 · 한 편을 합치는 규칙(점검 2026-11-17 #19) */
class PostMergeTest {
    private fun post(id: Long, at: Long, crew: String = "", mine: Boolean = false) = Post(
        id = id, category = PostCategory.FREE, crewId = crew, author = "", title = "t", body = "",
        createdAt = at, likes = 0, liked = false, commentCount = 0, mine = mine, place = "",
        distanceKm = 0.0, meetAt = 0L, capacity = 0, joinedCount = 0, joined = false,
    )

    @Test
    fun `전체 목록을 다시 받아도 따로 받은 예전 크루 글은 남는다`() {
        val oldCrew = post(1, at = 10, crew = "c")
        val current = listOf(post(5, at = 50), oldCrew)
        val fresh = listOf(post(6, at = 60), post(5, at = 50))
        val merged = PostMerge.merge(current, fresh, limit = 2) { true }
        assertEquals(listOf(6L, 5L, 1L), merged.map { it.id })
    }

    @Test
    fun `다 읽은 범위에 없는 글은 지워진 것으로 본다`() {
        val current = listOf(post(5, at = 50), post(1, at = 10, crew = "c"))
        val merged = PostMerge.merge(current, listOf(post(5, at = 50)), limit = 200) { true }
        assertEquals(listOf(5L), merged.map { it.id })
    }

    @Test
    fun `크루 글을 받으면 그 크루의 덮인 범위만 바꾸고 다른 글은 둔다`() {
        val current = listOf(post(9, at = 90), post(4, at = 40, crew = "c"), post(3, at = 30, crew = "d"))
        val fresh = listOf(post(2, at = 20, crew = "c"), post(1, at = 10, crew = "c"))
        val merged = PostMerge.merge(current, fresh, limit = 200) { it.crewId == "c" }
        // 4 는 크루 c 를 다 읽었는데 없다 — 지워졌다
        assertEquals(listOf(9L, 3L, 2L, 1L), merged.map { it.id })
    }

    @Test
    fun `한 편을 더하면 같은 글은 새 값으로 바뀐다`() {
        val current = listOf(post(9, at = 90), post(1, at = 10))
        val merged = PostMerge.merge(current, listOf(post(1, at = 10).copy(likes = 3)), Int.MAX_VALUE) { false }
        assertEquals(listOf(9L, 1L), merged.map { it.id })
        assertEquals(3, merged.last().likes)
    }
}
