package com.stepup.android

import com.stepup.android.domain.GeoPoint
import com.stepup.android.domain.Post
import com.stepup.android.domain.PostCategory
import com.stepup.android.domain.StoryDraft
import com.stepup.android.domain.StoryOrigin
import com.stepup.android.domain.StoryPlace
import com.stepup.android.domain.StoryRange
import com.stepup.android.domain.StoryReportReason
import com.stepup.android.domain.StoryText
import com.stepup.android.domain.formatStoryDistance
import com.stepup.android.domain.storyPlace
import com.stepup.android.ui.screens.community.stories.buildStoryList
import com.stepup.android.ui.screens.community.stories.rangeToReveal
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * 동네 이야기(목록형 커뮤니티) — 본문 한 칸 ↔ 서버 제목 · 본문, 거리 문구, 신고 사유, 목록 규칙.
 *
 * 여기서 어긋나면 글을 고칠 때 줄바꿈이나 첫 줄 뒷부분이 사라지고, 목록과 지도 핀의 글 수가 달라진다.
 */
class StoriesTest {

    // ── 본문 한 칸 → 제목(첫 줄, 120자) + 본문 → 다시 한 칸 ─────────────────

    private fun roundTrip(text: String): String {
        val (title, body) = StoryText.split(StoryText.normalize(text))
        return StoryText.join(title, body)
    }

    @Test fun oneLinePostIsATitleWithAnEmptyBody() {
        assertEquals("오늘 한강 좋았어요" to "", StoryText.split("오늘 한강 좋았어요"))
        assertEquals("오늘 한강 좋았어요", roundTrip("  오늘 한강 좋았어요  \n"))
    }

    @Test fun firstLineBecomesTheTitleAndTheRestKeepsItsLineBreaks() {
        val text = "처음으로 5km 완주했어요!\n느리게 뛰어도\n\n끝까지 가니까 좋네요."
        val (title, body) = StoryText.split(text)
        assertEquals("처음으로 5km 완주했어요!", title)
        assertEquals("\n느리게 뛰어도\n\n끝까지 가니까 좋네요.", body)
        assertEquals(text, StoryText.join(title, body))
        assertEquals("느리게 뛰어도\n\n끝까지 가니까 좋네요.", StoryText.bodyForDisplay(body))
    }

    @Test fun aFirstLineLongerThanTheServerTitleLimitIsCutWithoutLosingAnything() {
        val first = "가".repeat(150)
        val text = "$first\n둘째 줄"
        val (title, body) = StoryText.split(text)
        assertEquals(StoryText.TITLE_MAX_CHARS, StoryText.length(title))
        assertEquals("가".repeat(30) + "\n둘째 줄", body)
        assertEquals(text, StoryText.join(title, body))
    }

    @Test fun aFirstLineOfExactlyTheLimitStillSplitsAtTheLineBreak() {
        val text = "나".repeat(120) + "\n다음"
        val (title, body) = StoryText.split(text)
        assertEquals("나".repeat(120), title)
        assertEquals("\n다음", body)
        assertEquals(text, StoryText.join(title, body))
    }

    @Test fun theTitleCutNeverSplitsAnEmoji() {
        // 🏃 는 UTF-16 두 칸이다 — 120번째 글자에서 반으로 가르면 깨진 글자가 서버로 간다
        val text = "🏃".repeat(130)
        val (title, body) = StoryText.split(text)
        assertEquals(120, StoryText.length(title))
        assertEquals("🏃".repeat(120), title)
        assertEquals("🏃".repeat(10), body)
        assertEquals(text, StoryText.join(title, body))
    }

    @Test fun postsFromTheOlderTitleAndBodyFormReadAsOneTextWithALineBetween() {
        assertEquals("제목\n본문", StoryText.join("제목", "본문"))
    }

    @Test fun blankOrTooLongTextCannotBePosted() {
        assertFalse(StoryText.canPost(""))
        assertFalse(StoryText.canPost(" \n\t "))
        assertTrue(StoryText.canPost("a".repeat(1_000)))
        assertFalse(StoryText.canPost("a".repeat(1_001)))
        // 글자 수는 이모지 하나를 한 글자로 센다
        assertEquals(2, StoryText.length("🏃a"))
        assertTrue(StoryText.canPost("🏃".repeat(1_000)))
    }

    // ── 거리 · 신고 · 범위 ─────────────────────────────────────────

    @Test fun distanceIsRoundedAndNeverInvented() {
        assertEquals("10m", formatStoryDistance(3.0))
        assertEquals("130m", formatStoryDistance(134.0))
        assertEquals("990m", formatStoryDistance(994.0))
        assertEquals("1.0km", formatStoryDistance(996.0))
        assertEquals("1.8km", formatStoryDistance(1_830.0))
        assertEquals("", formatStoryDistance(Double.NaN))
        assertEquals("", formatStoryDistance(-5.0))
    }

    @Test fun reportReasonsMapExplicitlyOntoTheServerList() {
        val server = setOf("SPAM", "ABUSE", "SEXUAL", "DANGER", "FRAUD", "OTHER")
        StoryReportReason.entries.forEach { assertTrue(it.name, it.serverReason in server) }
        assertEquals("SPAM" to "", StoryReportReason.SPAM.serverReason to StoryReportReason.SPAM.note)
        assertEquals("ABUSE" to "", StoryReportReason.ABUSE.serverReason to StoryReportReason.ABUSE.note)
        // "개인정보 노출"은 서버 목록에 없어 OTHER 로 보내고 원래 사유를 note 에 적는다
        assertEquals("OTHER" to "PRIVACY", StoryReportReason.PRIVACY.serverReason to StoryReportReason.PRIVACY.note)
        assertEquals("OTHER" to "", StoryReportReason.OTHER.serverReason to StoryReportReason.OTHER.note)
    }

    @Test fun rangeDefaultsToOneKilometre() {
        assertEquals(StoryRange.KM1, StoryRange.DEFAULT)
        assertEquals(StoryRange.KM1, StoryRange.of(null))
        assertEquals(StoryRange.KM1, StoryRange.of("nonsense"))
        assertEquals(StoryRange.KM3, StoryRange.of("KM3"))
    }

    @Test fun aNewPostWidensTheRangeOnlyAsFarAsNeeded() {
        assertEquals(StoryRange.KM1, rangeToReveal(400.0, StoryRange.KM1))
        assertEquals(StoryRange.KM1, rangeToReveal(700.0, StoryRange.M500))
        assertEquals(StoryRange.KM3, rangeToReveal(1_500.0, StoryRange.KM1))
        // 넓게 보고 있으면 좁히지 않는다
        assertEquals(StoryRange.KM3, rangeToReveal(300.0, StoryRange.KM3))
        assertNull(rangeToReveal(5_000.0, StoryRange.KM1))
    }

    @Test fun draftPreviewIsItsFirstWrittenLine() {
        assertEquals("비 오는 날", StoryDraft("\n\n  비 오는 날  \n쿠션", null, 0, 0).preview)
        assertTrue(StoryDraft("x", null, 42, 0).isEdit)
    }

    // ── 목록 · 핀 · 장소 없는 이전 글 ─────────────────────────────────

    private val origin = StoryOrigin(GeoPoint(37.5267, 126.9238), "여의도동", manual = true)
    private val park = StoryPlace("여의도공원", "서울특별시 영등포구 여의공원로", 37.5261, 126.9225) // 약 130m
    private val ferry = StoryPlace("여의나루", "서울특별시 영등포구 여의동로", 37.5269, 126.9325) // 약 770m
    private val creek = StoryPlace("샛강생태공원", "", 37.5176, 126.9207) // 약 1.05km

    private fun post(
        id: Long, place: StoryPlace?, minutesAgo: Long, category: PostCategory = PostCategory.FREE, crew: String = "",
    ) = Post(
        id = id, category = category, crewId = crew, author = "러너", title = "글 $id", body = "",
        createdAt = 1_000_000L - minutesAgo * 60_000, likes = 0, liked = false, commentCount = 0, mine = false,
        place = place?.name.orEmpty(), distanceKm = 0.0, meetAt = 0L, capacity = 0, joinedCount = 0, joined = false,
        lat = place?.lat, lng = place?.lng, placeAddress = place?.address.orEmpty(),
    )

    private val posts = listOf(
        post(1, park, 10), post(2, park, 30), post(3, ferry, 20), post(4, creek, 5),
        post(5, null, 60), // 장소 없는 이전 글
        post(6, park, 1, category = PostCategory.FLASH), // 번개 — 함께 뛰기의 것
        post(7, park, 2, crew = "crew-1"), // 크루 게시판 글
    )

    @Test fun listShowsOnlyPublicStoriesInRangeNewestFirst() {
        val state = buildStoryList(posts, emptySet(), origin, StoryRange.KM1, null)
        assertEquals(listOf(1L, 3L, 2L), state.nearby.map { it.post.id })
        assertEquals(listOf(5L), state.legacy.map { it.post.id })
        assertEquals(130.0, state.nearby.first().meters!!, 10.0)
    }

    @Test fun pinsCountTheSamePostsAsTheList() {
        val state = buildStoryList(posts, setOf(2L), origin, StoryRange.KM1, null)
        assertEquals(listOf(1L, 3L), state.nearby.map { it.post.id })
        assertEquals(listOf("여의도공원" to 1, "여의나루" to 1), state.pins.map { it.place.name to it.count })
        assertEquals(state.nearby.size, state.pins.sumOf { it.count })
    }

    @Test fun widerRangeBringsInFartherPlaces() {
        val state = buildStoryList(posts, emptySet(), origin, StoryRange.KM3, null)
        assertEquals(listOf(4L, 1L, 3L, 2L), state.nearby.map { it.post.id })
        assertEquals("여의도공원", state.pins.first().place.name)
    }

    @Test fun aChosenPlaceShowsItsPostsEvenOutsideTheRange() {
        val state = buildStoryList(posts, emptySet(), origin, StoryRange.M500, creek)
        assertEquals(listOf(4L), state.nearby.map { it.post.id })
        assertTrue(state.legacy.isEmpty())
        assertEquals(1, state.pins.first { it.place.key == creek.key }.count)
        // 목록이 어느 장소로 걸렀는지 안다 — 필터가 바뀌면 화면이 처음부터 보인다
        assertEquals(creek.key, state.filterKey)
        assertNull(buildStoryList(posts, emptySet(), origin, StoryRange.M500, null).filterKey)
    }

    @Test fun withoutAnOriginNothingIsPlacedByDistance() {
        val state = buildStoryList(posts, emptySet(), null, StoryRange.KM3, null)
        assertTrue(state.nearby.isEmpty())
        assertTrue(state.pins.isEmpty())
    }

    @Test fun storyPlaceNeedsANameAndCoordinates() {
        assertEquals(park, post(1, park, 0).storyPlace)
        assertNull(post(5, null, 0).storyPlace)
        assertNull(post(1, park, 0).copy(place = " ").storyPlace)
        assertNull(post(1, park, 0).copy(lat = null).storyPlace)
    }
}
