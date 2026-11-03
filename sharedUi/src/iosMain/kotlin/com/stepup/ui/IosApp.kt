package com.stepup.ui

import com.stepup.android.data.remote.KeychainSessionStore
import com.stepup.android.data.remote.SessionHolder
import com.stepup.android.data.remote.StepUpServer
import com.stepup.android.data.remote.SupabaseAuth
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import platform.Foundation.NSBundle

/**
 * iOS 앱이 한 번 만들어 같이 쓰는 것(안드로이드 ServiceLocator 자리) — 서버 주소 · 공개 키는 Info.plist(Config.xcconfig)에서.
 * 공개 키(sb_publishable)만 앱에 넣는다. 서버 비밀 키는 앱에 넣지 않는다(CLAUDE.md "지킬 것").
 */
object IosApp {
    private fun plist(key: String): String =
        (NSBundle.mainBundle.objectForInfoDictionaryKey(key) as? String)?.trim().orEmpty()

    val supabaseUrl: String by lazy { plist("StepUpSupabaseUrl") }
    val supabaseKey: String by lazy { plist("StepUpSupabaseKey") }
    val attesterUrl: String by lazy { plist("StepUpAttesterUrl") }

    /** 지도 타일 — 안드로이드 BuildConfig.MAPTILER_KEY 와 같은 공개 키(앱에 넣으라고 발급되는 키). 비면 OpenStreetMap */
    fun configureMap() {
        com.stepup.android.ui.components.MapTiles.mapTilerKey = plist("StepUpMapTilerKey")
        val version = plist("CFBundleShortVersionString")
        com.stepup.android.ui.components.MapTiles.userAgent = "StepUp/$version (iOS; +https://stepupcrew.com)"
    }

    val sessionStore by lazy { KeychainSessionStore() }
    val auth by lazy { SupabaseAuth(supabaseUrl, supabaseKey) }
    val sessions by lazy { SessionHolder(auth, sessionStore) }
    val server by lazy { StepUpServer(supabaseUrl, supabaseKey, sessions) }
    /** 설정(안드로이드와 같은 UserPrefs — 문서 폴더의 strideup_prefs) */
    /** 폰 DB(안드로이드와 같은 AppDatabase — 문서 폴더의 strideup.db) */
    val db by lazy { com.stepup.android.data.local.buildIosDatabase() }
    /** 러닝 올리기(안드로이드와 같은 ClaimRepository · ServerSessionRecorder) — 앱을 켜면 IosUploads 에 이어 둔다 */
    val claims by lazy {
        com.stepup.android.data.repo.ClaimRepository(
            sessionDao = db.walkSessionDao(),
            recorder = com.stepup.android.data.repo.ServerSessionRecorder(server),
            uploadOwner = { sessions.recordingOwner() },
        )
    }

    fun connectUploads() {
        IosUploads.connect(flushReports = { true }, uploadPending = { claims.uploadPending() })
    }

    val prefs by lazy { com.stepup.android.data.prefs.createIosUserPrefs() }

    /** 서버 경제(신발 · 잔고 · 에너지) — 안드로이드 ServiceLocator 와 같은 공용 저장소. 폰의 목록은 서버 원장의 사본이다 */
    val economyApi by lazy { com.stepup.android.data.remote.EconomyApi(server) }
    val economySync by lazy {
        com.stepup.android.data.repo.EconomySync(
            api = economyApi, market = com.stepup.android.data.remote.MarketApi(server), db = db, prefs = prefs, sessions = sessions,
        )
    }
    val rewards by lazy {
        com.stepup.android.data.repo.RewardRepository(
            rewardDao = db.rewardDao(), sneakerDao = db.sneakerDao(), boostDao = db.boostDao(), notificationDao = db.notificationDao(),
            prefs = prefs, serverEconomy = true,
            claimGoalOnServer = {
                if (economyApi.goalClaim() is com.stepup.android.data.remote.ServerResult.Ok) economySync.refresh()
            },
        )
    }
    val sneakers by lazy {
        com.stepup.android.data.repo.SneakerRepository(db, rewards, economy = economyApi, sync = economySync, prefs = prefs)
    }
    val boosts by lazy { com.stepup.android.data.repo.BoostRepository(db, rewards, prefs, economy = economyApi, sync = economySync) }

    val steps by lazy {
        com.stepup.android.data.repo.StepRepository(
            stepDao = db.stepDao(), walkSessionDao = db.walkSessionDao(), prefs = prefs, tracker = IosStepSource,
            rewardRepository = rewards,
            pushGoal = { goal -> economyApi.setDailyGoal(goal) },
            // 안드로이드 ServiceLocator.recordingOwnerFlow 와 같다
            owner = prefs.loginMethod.map { sessions.recordingOwner() }.distinctUntilChanged(),
            runActive = { IosRunSession.active },
        )
    }
    val avatars by lazy { com.stepup.android.data.repo.AvatarRepository(prefs, sneakers) }
    val notifications by lazy { com.stepup.android.data.repo.NotificationRepository(db.notificationDao()) }

    // ── 함께 뛰기 · 크루 · 커뮤니티 · 기록(안드로이드 ServiceLocator 와 같은 저장소 · 같은 인자) ──
    private val crewApi by lazy { com.stepup.android.data.remote.CrewApi(server) }
    val crewImages by lazy { IosCrewImages(crewApi::image) }
    val crews by lazy {
        com.stepup.android.data.repo.CrewRepository(
            api = crewApi, crewDao = db.crewDao(), crewInfoDao = db.crewInfoDao(), walkSessionDao = db.walkSessionDao(),
            rewardRepository = rewards, partyApi = com.stepup.android.data.remote.PartyApi(server),
            shareByDefault = { prefs.partyShareLocation.first() },
            exitedParties = { prefs.exitedParties.first() },
            rememberExit = { prefs.addExitedParty(it) },
            groupRun = { com.stepup.android.data.repo.GroupRunState(IosRunSession.active, IosRunSession.state.value?.paused == true, inGroup = false) },
        )
    }
    private fun ownerFlow() = prefs.loginMethod.map { sessions.recordingOwner() }.distinctUntilChanged()
    val crewCards by lazy {
        com.stepup.android.data.repo.CrewCardRepository(
            api = crewApi, crews = crews, images = crewImages, prefs = prefs,
            owner = { sessions.recordingOwner() }, ownerFlow = ownerFlow(),
            draftDir = IosDirs.documents + "/crew_drafts",
        )
    }
    val crewChat by lazy {
        com.stepup.android.data.repo.CrewChatRepository(
            api = com.stepup.android.data.remote.CrewChatApi(server), crewApi = crewApi, prefs = prefs,
            owner = { sessions.recordingOwner() }, photoDir = IosDirs.caches + "/chat_photos", codec = IosChatImageCodec,
        )
    }
    val crewHome by lazy {
        com.stepup.android.data.repo.CrewHomeRepository(
            api = com.stepup.android.data.remote.CrewHomeApi(server), owner = { sessions.recordingOwner() },
            onEnded = { crewId -> crewChat.forget(crewId) },
        )
    }
    val community by lazy {
        com.stepup.android.data.repo.CommunityRepository(
            api = com.stepup.android.data.remote.CommunityApi(server), postDao = db.postDao(), commentDao = db.commentDao(),
            notificationDao = db.notificationDao(), prefs = prefs, owner = { sessions.recordingOwner() }, ownerFlow = ownerFlow(),
            localRuns = { owner, since ->
                val dao = db.walkSessionDao()
                com.stepup.android.domain.StoryLocalRuns(count = dao.countFor(owner), pendingRecent = dao.pendingSince(owner, since) > 0)
            },
        )
    }
    val coaching by lazy {
        com.stepup.android.data.repo.CoachingRepository(
            dao = db.walkSessionDao(), api = com.stepup.android.data.remote.CoachingApi(server),
            cache = com.stepup.android.data.repo.CoachingCacheStore(IosDirs.documents + "/coaching"),
            owner = ownerFlow(), online = { com.stepup.android.core.hasNetwork() },
        )
    }
    val runRecords by lazy {
        com.stepup.android.data.repo.RunRecordsRepository(db.walkSessionDao(), ownerFlow(), onDeleted = { record -> coaching.forgetRun(record) })
    }
    val placeSearch by lazy {
        com.stepup.android.data.repo.PlaceSearch(
            api = com.stepup.android.data.remote.PlaceSearchApi(key = com.stepup.android.ui.components.MapTiles.mapTilerKey, fetch = ::iosDownloadText),
            platform = null,
            language = { com.stepup.android.core.AppLocale.tag.ifEmpty { androidx.compose.ui.text.intl.Locale.current.language }.takeIf { it in com.stepup.android.core.AppLocale.SUPPORTED } ?: "ko" },
        )
    }

    /** 공용 화면이 쓰는 저장소를 채운다(안드로이드 ServiceLocator 와 같은 이름 — AppServices) */
    fun connectServices(onReturnToSignIn: suspend () -> Unit, onSignedOut: () -> Unit) {
        val s = com.stepup.android.core.AppServices
        s.userPrefs = prefs
        s.rewardRepository = rewards
        s.sneakerRepository = sneakers
        s.boostRepository = boosts
        s.economySync = economySync
        s.stepRepository = steps
        s.avatarRepository = avatars
        s.notificationRepository = notifications
        s.database = db
        s.crewRepository = crews
        s.crewCards = crewCards
        s.crewChat = crewChat
        s.crewImage = { id, ver -> crewImages.image(id, ver) }
        s.crewHome = crewHome
        s.crewRun = com.stepup.android.data.repo.CrewRunRepository(api = com.stepup.android.data.remote.CrewRunApi(server), owner = { sessions.recordingOwner() })
        s.communityRepository = community
        s.coachingRepository = coaching
        s.runRecordsRepository = runRecords
        s.placeSearch = placeSearch
        s.rankingRepository = com.stepup.android.data.repo.RankingRepository(server)
        s.leaderboards = com.stepup.android.data.repo.ServerLeaderboardSource(com.stepup.android.data.remote.LeaderboardApi(server))
        s.courseRepository = com.stepup.android.data.repo.CourseRepository(
            dao = db.courseDao(), prefs = prefs, rewardRepository = rewards,
            api = com.stepup.android.data.remote.CourseApi(server), serverEconomy = true,
        )
        s.eventRepository = com.stepup.android.data.repo.EventRepository(dao = db.claimedEventDao(), api = com.stepup.android.data.remote.EventApi(server), stepDao = db.stepDao())
        s.marketRepository = com.stepup.android.data.repo.MarketRepository(
            api = com.stepup.android.data.remote.MarketApi(server), server = server, sneakerDao = db.sneakerDao(),
            rewardDao = db.rewardDao(), prefs = prefs, economySync = economySync,
        )
        s.runningFeedRepository = com.stepup.android.data.repo.RunningFeedRepository(com.stepup.android.data.remote.RunningFeedApi(server))
        s.announcementRepository = com.stepup.android.data.repo.AnnouncementRepository(
            source = com.stepup.android.data.remote.AnnouncementApi(server),
            cache = com.stepup.android.data.repo.FileAnnouncementCache(IosDirs.documents + "/announcements.json"),
        )
        s.recordingOwnerFlow = ::ownerFlow
        s.runActive = { IosRunSession.activeFlow }
        s.forgeRepository = com.stepup.android.data.repo.ForgeRepository(
            server = economyApi,
            pendingStore = com.stepup.android.data.repo.PrefsForgePendingStore(prefs),
            resync = { economySync.refresh() },
        )
        s.sessionHolder = sessions
        s.server = server
        s.economyApi = economyApi
        s.deleteAccount = { com.stepup.android.data.remote.IosAuth.deleteAccount(server, sessions, sessionStore, attesterUrl) }
        s.onForgetAccount = {
            // 달리던 러닝은 먼저 끝낸다(기록은 남긴다) · 이 폰의 로그인(키체인)도 지운다
            if (IosRunSession.active) IosRunSession.stop()
            com.stepup.android.data.remote.IosAuth.signOut(sessionStore)
        }
        s.serverEconomyOn = supabaseUrl.isNotEmpty()
        s.onReturnToSignIn = onReturnToSignIn
        s.onSignedOut = onSignedOut
        com.stepup.android.core.AppBuild.VERSION_NAME = plist("CFBundleShortVersionString")
    }
}

/** 장소 찾기 받기(안드로이드 placeSearchDownload 와 같은 6초 · User-Agent) */
private suspend fun iosDownloadText(url: String): String? = runCatching {
    val bytes = com.stepup.android.ui.components.fetchTileBytes(url, com.stepup.android.ui.components.MapTiles.userAgent).bytes ?: return null
    bytes.decodeToString()
}.getOrNull()
