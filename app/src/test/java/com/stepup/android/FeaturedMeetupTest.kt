package com.stepup.android

import com.stepup.android.domain.Post
import com.stepup.android.domain.PostCategory
import com.stepup.android.domain.featuredMeetup
import org.junit.Assert.*
import org.junit.Test

class FeaturedMeetupTest {
    private fun post(id: Long, time: Long) = Post(id = id, category = PostCategory.FLASH,
        crewId = "", author = "Runner", title = "Run", body = "", createdAt = 1,
        likes = 0, liked = false, commentCount = 0, mine = false, place = "Park",
        distanceKm = 3.0, meetAt = time, capacity = 5, joinedCount = 1, joined = false)

    @Test fun excludesClosedFullPrivateAndStoryPosts() {
        val candidates = listOf(post(1, 50), post(2, 110).copy(joinedCount = 5),
            post(3, 120).copy(crewId = "private"), post(4, 130).copy(category = PostCategory.FREE),
            post(5, 160), post(6, 140))
        assertEquals(6L, featuredMeetup(candidates, 100)?.id)
        assertNull(featuredMeetup(candidates, 200))
    }

    @Test fun noFallbackForUnscheduledOrEmptyMeetups() {
        assertNull(featuredMeetup(emptyList(), 100))
        assertNull(featuredMeetup(listOf(post(1, 0)), 100))
        assertNull(featuredMeetup(listOf(post(1, 100)), 100))
    }
}
