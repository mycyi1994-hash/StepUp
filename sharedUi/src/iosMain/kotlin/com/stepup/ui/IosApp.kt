package com.stepup.ui

import com.stepup.android.data.remote.KeychainSessionStore
import com.stepup.android.data.remote.SessionHolder
import com.stepup.android.data.remote.StepUpServer
import com.stepup.android.data.remote.SupabaseAuth
import kotlinx.coroutines.flow.distinctUntilChanged
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
