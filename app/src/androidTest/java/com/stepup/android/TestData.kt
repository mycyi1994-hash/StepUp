package com.stepup.android

import com.stepup.android.core.ServiceLocator
import com.stepup.android.data.repo.Crew
import com.stepup.android.data.repo.CrewJoinPolicy
import com.stepup.android.data.repo.PartyMember
import com.stepup.android.data.repo.PartyPhase
import com.stepup.android.data.repo.PartyState
import com.stepup.android.domain.Post
import com.stepup.android.domain.PostCategory

/**
 * 화면 검사가 쓰는 커뮤니티 자료.
 *
 * 크루·글·파티 방은 서버에만 있고, 화면 검사는 서버 없이 돈다. 실제 서버가
 * 주는 것과 같은 모양의 자료를 메모리에 채워 넣는다. 화면 검사 둘
 * (ExperienceUiTest · DesignReferenceTest)이 같은 자료를 본다.
 */
object TestData {
    /** Legacy notification fixture only; production startup must never fabricate rewards. */
    suspend fun seedWelcomeNotification() {
        val dao = ServiceLocator.database.notificationDao()
        if (dao.count() > 0) return
        dao.insert(com.stepup.android.data.local.NotificationEntity(
            timestamp = System.currentTimeMillis() - 40_000,
            type = "EVENT_REWARD", argText = "Welcome Runner", argAmount = 30.0,
            argExtra = "welcome", read = false, actioned = false,
        ))
    }

    fun seedCommunity() {
        // 크루는 서버에만 있다. 화면 검사는 서버 없이 도는 것이라, 실제 크루와 같은
        // 모양의 크루 하나를 채워 넣는다. 크루장이고 승인제라 관리 카드까지 그려진다.
        ServiceLocator.crewRepository.showForTest(
            listOf(
                Crew(
                    id = "00000000-0000-0000-0000-00000000c0de",
                    monogram = "HR",
                    name = "Hangang Runners",
                    tagline = "Saturday 7AM, 5K by the river",
                    area = "Mapo",
                    memberCount = 24,
                    roster = listOf("Ara Kim", "Bo Lee", "Cha Park", "Dan Choi"),
                    owned = true,
                    joinPolicy = CrewJoinPolicy.APPROVAL,
                    joined = true,
                    pendingCount = 2,
                ),
            ),
        )
        // 글도 서버에만 있다. 번개 하나, 자유 글 하나, 크루 글 하나로 게시판 화면을 채운다.
        val now = System.currentTimeMillis()
        ServiceLocator.communityRepository.showForTest(
            listOf(
                Post(
                    id = 101, category = PostCategory.FLASH, crewId = "", author = "Sora K.",
                    authorId = "u-sora", title = "Tonight 7PM · 5K by the river",
                    body = "Easy pace, everyone welcome.", createdAt = now - 3_600_000,
                    likes = 12, liked = false, commentCount = 4, mine = false,
                    place = "Yeouido Park Gate 3", distanceKm = 1.2, meetAt = now + 5_400_000,
                    capacity = 8, joinedCount = 5, joined = false,
                ),
                Post(
                    id = 102, category = PostCategory.TIP, crewId = "", author = "Ara Kim",
                    authorId = "u-ara", title = "Wide-toe running shoes that worked for me",
                    body = "Three picks after two months of testing.", createdAt = now - 7_200_000,
                    likes = 31, liked = true, commentCount = 9, mine = true,
                    place = "", distanceKm = 0.0, meetAt = 0L, capacity = 0, joinedCount = 0, joined = false,
                ),
                Post(
                    id = 103, category = PostCategory.FREE, crewId = "00000000-0000-0000-0000-00000000c0de",
                    author = "Bo Lee", authorId = "u-bo", title = "Saturday route is set",
                    body = "Meet at the bridge, 6:50.", createdAt = now - 10_800_000,
                    likes = 6, liked = false, commentCount = 2, mine = false,
                    place = "", distanceKm = 0.0, meetAt = 0L, capacity = 0, joinedCount = 0, joined = false,
                ),
            ),
        )
        // 파티 로비도 서버의 방이다. 방장인 나와 크루원 둘이 모인 로비를 채운다.
        ServiceLocator.crewRepository.showPartyForTest(
            PartyState(
                phase = PartyPhase.LOBBY,
                partyId = 1L,
                crewId = "00000000-0000-0000-0000-00000000c0de",
                crewName = "Hangang Runners",
                members = listOf(
                    PartyMember(id = "u-me", name = "", ready = true, isMe = true, isHost = true),
                    PartyMember(id = "u-ara", name = "Ara Kim", ready = true, isMe = false),
                    PartyMember(id = "u-bo", name = "Bo Lee", ready = false, isMe = false),
                ),
            ),
        )
    }
}
