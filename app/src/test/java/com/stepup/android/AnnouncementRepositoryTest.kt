package com.stepup.android

import com.stepup.android.data.remote.AnnouncementRow
import com.stepup.android.data.remote.AnnouncementSource
import com.stepup.android.data.remote.ServerResult
import com.stepup.android.data.repo.AnnouncementCache
import com.stepup.android.data.repo.AnnouncementRepository
import com.stepup.android.data.repo.NoticeAction
import com.stepup.android.data.repo.NoticeBlock
import com.stepup.android.data.repo.NoticeDetail
import com.stepup.android.data.repo.noticeBlocks
import com.stepup.android.data.repo.pickText
import com.stepup.android.data.repo.toDomain
import com.stepup.android.ui.screens.notifications.DayGroup
import com.stepup.android.ui.screens.notifications.dayGroup
import java.time.ZoneId
import java.time.ZonedDateTime
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * 앱 공지(알림·공지 v1 15~20 · 23) — 서버 답과 폰의 사본.
 * 받지 못한 것 · 빈 목록 · 내린 글을 서로 섞지 않는다. 예시 공지는 앱이 만들지 않는다.
 */
class AnnouncementRepositoryTest {

    @Test fun `처음 받으면 목록을 보이고 사본을 남긴다`() = runBlocking {
        val cache = MemoryCache()
        val repo = AnnouncementRepository(FakeSource(list = ServerResult.Ok(listOf(row(1), row(2)))), cache)
        repo.refresh()
        assertEquals(listOf(1L, 2L), repo.board.value.items?.map { it.id })
        assertFalse(repo.board.value.refreshFailed)
        assertEquals(listOf(1L, 2L), cache.rows?.map { it.id })
    }

    @Test fun `받지 못했고 사본도 없으면 불러오지 못했다 — 빈 목록이 아니다`() = runBlocking {
        val repo = AnnouncementRepository(FakeSource(list = ServerResult.Retry("offline")), MemoryCache())
        repo.refresh()
        assertNull(repo.board.value.items)
        assertTrue(repo.board.value.refreshFailed)
    }

    @Test fun `서버가 빈 목록을 주면 공지가 없다 — 실패가 아니다`() = runBlocking {
        val repo = AnnouncementRepository(FakeSource(list = ServerResult.Ok(emptyList())), MemoryCache())
        repo.refresh()
        assertEquals(emptyList<Any>(), repo.board.value.items)
        assertFalse(repo.board.value.refreshFailed)
    }

    @Test fun `새로 받지 못하면 사본을 그대로 보이고 알린다`() = runBlocking {
        val cache = MemoryCache(listOf(row(7)))
        val repo = AnnouncementRepository(FakeSource(list = ServerResult.Retry("offline")), cache)
        repo.refresh()
        assertEquals(listOf(7L), repo.board.value.items?.map { it.id })
        assertTrue(repo.board.value.refreshFailed)
        assertEquals(listOf(7L), cache.rows?.map { it.id })
    }

    @Test fun `배포 전 서버(표 없음)도 공지 없음으로 보이지 않는다`() = runBlocking {
        val repo = AnnouncementRepository(FakeSource(list = ServerResult.Rejected("relation does not exist")), MemoryCache())
        repo.refresh()
        assertNull(repo.board.value.items)
        assertTrue(repo.board.value.refreshFailed)
    }

    @Test fun `내린 공지는 사본보다 서버 답이 먼저다`() = runBlocking {
        val cache = MemoryCache(listOf(row(1), row(2)))
        val source = FakeSource(list = ServerResult.Ok(listOf(row(2))))
        val repo = AnnouncementRepository(source, cache)
        repo.refresh()
        assertEquals(listOf(2L), repo.board.value.items?.map { it.id })
        assertEquals(listOf(2L), cache.rows?.map { it.id })
    }

    @Test fun `상세 — 서버가 주지 않으면 볼 수 없는 공지이고 목록 · 사본에서도 뺀다`() = runBlocking {
        val cache = MemoryCache()
        val source = FakeSource(list = ServerResult.Ok(listOf(row(1), row(2))), one = ServerResult.Ok(null))
        val repo = AnnouncementRepository(source, cache)
        repo.refresh()
        assertEquals(NoticeDetail.Unavailable, repo.detail(1))
        assertEquals(listOf(2L), repo.board.value.items?.map { it.id })
        assertEquals(listOf(2L), cache.rows?.map { it.id })
    }

    @Test fun `상세 — 닿지 못하면 받아 둔 내용을 보이고, 받아 둔 것도 없으면 못 불러왔다`() = runBlocking {
        val source = FakeSource(list = ServerResult.Ok(listOf(row(1))), one = ServerResult.Retry("offline"))
        val repo = AnnouncementRepository(source, MemoryCache())
        repo.refresh()
        val saved = repo.detail(1)
        assertTrue(saved is NoticeDetail.Ready && saved.saved && saved.notice.id == 1L)
        assertEquals(listOf(1L), repo.board.value.items?.map { it.id })
        assertEquals(NoticeDetail.Failed, repo.detail(99))
    }

    @Test fun `상세 — 서버가 준 새 내용을 보이고 사본도 바꾼다`() = runBlocking {
        val cache = MemoryCache()
        val updated = row(1, title = "고친 제목")
        val source = FakeSource(list = ServerResult.Ok(listOf(row(1))), one = ServerResult.Ok(updated))
        val repo = AnnouncementRepository(source, cache)
        repo.refresh()
        val fresh = repo.detail(1)
        assertTrue(fresh is NoticeDetail.Ready && !fresh.saved && fresh.notice.title("ko") == "고친 제목")
        assertEquals("고친 제목", repo.board.value.items?.single()?.title("ko"))
        assertEquals("고친 제목", (cache.rows?.single()?.title?.get("ko") as JsonPrimitive).content)
    }

    @Test fun `언어 — 지금 언어, 없으면 한국어, 영어, 남은 것 순`() {
        val texts = mapOf("ko" to "안내", "en" to "Notice", "zh-TW" to "公告")
        assertEquals("Notice", pickText(texts, "en"))
        assertEquals("公告", pickText(texts, "zh"))
        assertEquals("안내", pickText(texts, "ja"))
        assertEquals("Notice", pickText(mapOf("en" to "Notice", "fr" to "Avis"), "ja"))
        assertEquals("Avis", pickText(mapOf("fr" to "Avis"), "ja"))
        assertEquals("", pickText(emptyMap(), "ko"))
    }

    @Test fun `서버 한 줄 — 제목이 없거나 날짜가 깨지면 보이지 않고, 모르는 버튼은 달지 않는다`() {
        assertNull(row(1, title = null).toDomain())
        assertNull(row(1).copy(publishedAt = "어제").toDomain())
        assertNull(row(1).copy(action = "OPEN_URL").toDomain()?.action)
        assertEquals(NoticeAction.DRAW, row(1).copy(action = "DRAW").toDomain()?.action)
        assertEquals(1_790_470_800_000L, row(1).copy(publishedAt = "2026-09-27T01:00:00+00:00").toDomain()?.publishedAt)
    }

    @Test fun `본문 — 빈 줄은 문단, 소제목 · 구분선`() {
        val blocks = noticeBlocks("## 무료 뽑기\n처음 가입하면 10회\n매일 무료 뽑기 3회\n\n---\n지갑 연결은 준비 중이에요.\n")
        assertEquals(
            listOf(
                NoticeBlock.Heading("무료 뽑기"),
                NoticeBlock.Paragraph(listOf("처음 가입하면 10회", "매일 무료 뽑기 3회")),
                NoticeBlock.Rule,
                NoticeBlock.Paragraph(listOf("지갑 연결은 준비 중이에요.")),
            ),
            blocks,
        )
        assertEquals(emptyList<NoticeBlock>(), noticeBlocks("  \n\n"))
    }

    @Test fun `알림 날짜 묶음은 사용자 시간대의 날짜로 가른다`() {
        val seoul = ZoneId.of("Asia/Seoul")
        val now = ZonedDateTime.of(2026, 9, 28, 0, 30, 0, 0, seoul).toInstant().toEpochMilli()
        fun at(day: Int, hour: Int) = ZonedDateTime.of(2026, 9, day, hour, 0, 0, 0, seoul).toInstant().toEpochMilli()
        assertEquals(DayGroup.Today, dayGroup(at(28, 0), now, seoul))
        assertEquals(DayGroup.Yesterday, dayGroup(at(27, 23), now, seoul))
        assertEquals(DayGroup.Earlier, dayGroup(at(26, 23), now, seoul))
        // 같은 순간도 UTC 로는 아직 27일이다 — 시간대를 바꾸면 묶음이 달라진다
        assertEquals(DayGroup.Today, dayGroup(at(27, 23), now, ZoneId.of("UTC")))
        // 시계가 조금 앞선 알림도 오늘
        assertEquals(DayGroup.Today, dayGroup(now + 60_000, now, seoul))
    }

    private fun row(id: Long, title: String? = "공지 $id") = AnnouncementRow(
        id = id,
        title = JsonObject(if (title == null) emptyMap() else mapOf("ko" to JsonPrimitive(title))),
        body = JsonObject(mapOf("ko" to JsonPrimitive("본문 $id"))),
        publishedAt = "2026-09-2${id % 9}T02:30:00+00:00",
    )

    private class FakeSource(
        private val list: ServerResult<List<AnnouncementRow>>,
        private val one: ServerResult<AnnouncementRow?> = ServerResult.Retry("unused"),
    ) : AnnouncementSource {
        override suspend fun list() = list
        override suspend fun one(id: Long) = one
    }

    private class MemoryCache(var rows: List<AnnouncementRow>? = null) : AnnouncementCache {
        override suspend fun read() = rows
        override suspend fun write(rows: List<AnnouncementRow>) {
            this.rows = rows
        }
    }
}
