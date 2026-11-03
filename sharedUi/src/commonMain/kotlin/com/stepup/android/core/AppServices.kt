package com.stepup.android.core

import com.stepup.android.data.prefs.UserPrefs
import com.stepup.android.data.repo.BoostRepository
import com.stepup.android.data.repo.EconomySync
import com.stepup.android.data.repo.RewardRepository
import com.stepup.android.data.repo.SneakerRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

/**
 * 공용 화면이 쓰는 저장소(안드로이드 ServiceLocator 의 화면 쪽 자리, 2026-11-03 iOS C3). 앱이 켜질 때 한 번 채운다 —
 * iOS 는 IosApp, 안드로이드가 같은 화면을 쓰게 되면 ServiceLocator 가 같은 저장소를 넣는다. 이름은 안드로이드 ServiceLocator 와 같다.
 */
object AppServices {
    lateinit var userPrefs: UserPrefs
    lateinit var rewardRepository: RewardRepository
    lateinit var sneakerRepository: SneakerRepository
    lateinit var boostRepository: BoostRepository
    lateinit var economySync: EconomySync
    lateinit var stepRepository: com.stepup.android.data.repo.StepRepository
    lateinit var avatarRepository: com.stepup.android.data.repo.AvatarRepository
    lateinit var notificationRepository: com.stepup.android.data.repo.NotificationRepository
    lateinit var database: com.stepup.android.data.local.AppDatabase
    lateinit var crewRepository: com.stepup.android.data.repo.CrewRepository
    lateinit var communityRepository: com.stepup.android.data.repo.CommunityRepository
    lateinit var crewHome: com.stepup.android.data.repo.CrewHomeRepository
    lateinit var crewRun: com.stepup.android.data.repo.CrewRunRepository
    lateinit var profileEditor: com.stepup.android.data.repo.ProfileEditor
    lateinit var runRecordsRepository: com.stepup.android.data.repo.RunRecordsRepository
    lateinit var placeSearch: com.stepup.android.data.repo.PlaceSearch
    lateinit var announcementRepository: com.stepup.android.data.repo.AnnouncementRepository
    lateinit var rankingRepository: com.stepup.android.data.repo.RankingRepository
    lateinit var leaderboards: com.stepup.android.data.repo.LeaderboardSource
    lateinit var courseRepository: com.stepup.android.data.repo.CourseRepository
    lateinit var eventRepository: com.stepup.android.data.repo.EventRepository
    lateinit var marketRepository: com.stepup.android.data.repo.MarketRepository
    lateinit var runningFeedRepository: com.stepup.android.data.repo.RunningFeedRepository
    lateinit var coachingRepository: com.stepup.android.data.repo.CoachingRepository
    /** 크루 명함 · 채팅 — 사진은 화면 그림(ImageBitmap)으로 받는다(안드로이드는 Bitmap 을 감싼다) */
    lateinit var crewCards: com.stepup.android.data.repo.CrewCardRepository<*>
    lateinit var crewChat: com.stepup.android.data.repo.CrewChatRepository<androidx.compose.ui.graphics.ImageBitmap>
    /** 크루 대표 사진(화면 그림) — 없거나 못 받으면 null */
    var crewImage: suspend (crewId: String, version: Int) -> androidx.compose.ui.graphics.ImageBitmap? = { _, _ -> null }

    /** 이 폰이 지금 러닝 중인가(안드로이드 WalkSessionService.state.isActive · iOS 러닝 세션) */
    var runActive: () -> kotlinx.coroutines.flow.StateFlow<Boolean> = { kotlinx.coroutines.flow.MutableStateFlow(false) }

    /** 모임(크루 · 번개) 러닝을 켠다 — 켜졌으면 true(안드로이드 WalkSessionService.start · iOS 러닝 세션) */
    var startGroupRun: (party: com.stepup.android.data.repo.PartyState, title: String) -> Boolean = { _, _ -> false }

    /** 지금 계정의 기록 주인이 바뀌는 흐름(안드로이드 ServiceLocator.recordingOwnerFlow) */
    var recordingOwnerFlow: () -> kotlinx.coroutines.flow.Flow<String> = { kotlinx.coroutines.flow.flowOf("") }

    lateinit var forgeRepository: com.stepup.android.data.repo.ForgeRepository

    /** 이 크루의 대기실(출발 전 · 카운트다운)에 들어가 있으면 그 방 번호 — 크루 러닝을 옮기기 전에는 없다 */
    var openCrewRoom: (crewId: String) -> Long? = { null }

    /** 다시 로그인하러 갈 때 앱이 할 일(달리던 러닝 멈추기 · 로그인 화면으로) */
    var onReturnToSignIn: suspend () -> Unit = {}

    lateinit var sessionHolder: com.stepup.android.data.remote.SessionHolder
    lateinit var server: com.stepup.android.data.remote.StepUpServer
    lateinit var economyApi: com.stepup.android.data.remote.EconomyApi

    /** 계정 지우기 — 서버가 지웠는가. iOS 는 Apple 로그인 토큰 철회를 먼저 한다(App Store 5.1.1(v)) */
    var deleteAccount: suspend () -> com.stepup.android.data.remote.AccountDeletion = { server.deleteAccount() }

    /** 이 폰에서 계정을 잊을 때 앱이 먼저 할 일(달리는 러닝 멈추기 · 앱 파일 지우기 · 로그인 저장 지우기) */
    var onForgetAccount: suspend () -> Unit = {}

    /** 이 폰에서 계정을 다 잊었다 — 앱 뿌리가 로그인 첫 화면으로(안드로이드는 로그인 표시가 지워지면 저절로) */
    var onSignedOut: () -> Unit = {}

    /** 알림 설정을 서버로 보내는 쪽 — iOS 는 푸시를 붙이기 전까지 NoPush */
    var pushRegistrar: PushPreferences = NoPush

    /** 서버 경제인가(서버 주소가 있는 빌드는 언제나) */
    var serverEconomyOn: Boolean = true

    private val economyScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    /** 서버 경제를 뒤에서 한 번 맞춘다(안드로이드 ServiceLocator.refreshEconomyInBackground 와 같다) */
    fun refreshEconomyInBackground() {
        economyScope.launch { runCatching { economySync.refresh() } }
    }
}
