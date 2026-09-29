package com.stepup.android.ui

import kotlinx.coroutines.flow.map

import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.statusBarsPadding
import com.stepup.android.ui.components.MainHeader
import com.stepup.android.ui.theme.StepUpDesign
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.animation.*
import androidx.compose.animation.core.tween
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.selected
import com.stepup.android.ui.experience.*
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import com.stepup.android.ui.screens.customize.RunnerMarketScreen
import com.stepup.android.ui.screens.customize.CustomizeScreen
import com.stepup.android.ui.components.StepUpIcons
import androidx.compose.foundation.layout.heightIn
import androidx.compose.material.icons.automirrored.filled.DirectionsRun
import androidx.compose.material.icons.filled.Groups
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.Icon
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import kotlinx.coroutines.launch
import com.stepup.android.R
import com.stepup.android.BuildConfig
import com.stepup.android.core.InviteLinks
import com.stepup.android.core.ServiceLocator
import com.stepup.android.ui.components.HairlineDivider
import com.stepup.android.ui.components.NightCanvas
import com.stepup.android.ui.guide.GuideOverlay
import com.stepup.android.ui.guide.GuideTour
import com.stepup.android.ui.guide.guideTarget
import com.stepup.android.ui.screens.community.CommunityScreen
import com.stepup.android.ui.screens.community.CrewBoardScreen
import com.stepup.android.ui.screens.community.crew.crewGraph
import com.stepup.android.ui.screens.community.chat.chatGraph
import com.stepup.android.ui.screens.community.home.crewHomeGraph
import com.stepup.android.ui.screens.community.chat.openChatRoom
import com.stepup.android.ui.screens.community.FlashRunDetailScreen
import com.stepup.android.ui.screens.community.FocusedCommentSheetHost
import com.stepup.android.ui.screens.community.PartyLobbyScreen
import com.stepup.android.ui.screens.community.PostComposeScreen
import com.stepup.android.ui.screens.community.RankingScreen
import com.stepup.android.ui.screens.events.EventsScreen
import com.stepup.android.ui.screens.events.NewsScreen
import com.stepup.android.ui.screens.home.HomeScreen
import com.stepup.android.ui.screens.gacha.MysteryBoxScreen
import com.stepup.android.ui.screens.login.LoginScreen
import com.stepup.android.ui.screens.market.MarketModelScreen
import com.stepup.android.ui.screens.items.ItemsScreen
import com.stepup.android.ui.screens.items.SneakerDetailScreen
import com.stepup.android.ui.screens.items.SneakerDexScreen
import com.stepup.android.ui.screens.notifications.NotificationsScreen
import com.stepup.android.ui.screens.profile.AchievementsScreen
import com.stepup.android.ui.screens.profile.AnalyticsScreen
import com.stepup.android.ui.screens.profile.HistoryMapScreen
import com.stepup.android.ui.screens.map.MapScreen
import com.stepup.android.ui.screens.profile.ProfileScreen
import com.stepup.android.ui.screens.rewards.WalletScreen
import com.stepup.android.ui.screens.settings.ConnectedAccountsScreen
import com.stepup.android.ui.screens.settings.LanguageScreen
import com.stepup.android.ui.screens.settings.ThemeScreen
import com.stepup.android.ui.screens.settings.NotificationSettingsScreen
import com.stepup.android.ui.screens.settings.PrivacyScreen
import com.stepup.android.ui.screens.settings.SupportScreen
import com.stepup.android.ui.screens.splash.SplashScreen
import com.stepup.android.ui.screens.walk.CourseHubScreen
import com.stepup.android.ui.screens.walk.RunScreen
import com.stepup.android.ui.theme.Carbon
import com.stepup.android.ui.theme.Night
import com.stepup.android.ui.theme.Slate
import com.stepup.android.ui.theme.Volt
import com.stepup.android.ui.theme.VoltText

sealed class Screen(val route: String, val labelRes: Int, val icon: ImageVector) {
    /**
     * 러닝 — 첫 화면. 오늘 번 포인트 · 내 캐릭터 · 러닝 시작.
     *
     * 길(route)은 "home" 그대로 둔다. 알림·가이드 투어·다른 화면에서 넘어오는
     * 이동이 모두 이 이름을 쓴다.
     */
    data object Run : Screen("home", R.string.tab_run, Icons.AutoMirrored.Filled.DirectionsRun)

    /** 신발 — 내 신발 · 신발 보관함(신발 화면 확정안 2026-09-28). 러너 마켓 · 도감은 보관함 아래 작은 줄로 들어간다. */
    data object Customize : Screen("customize", R.string.tab_customize, StepUpIcons.Shoe)

    /**
     * 뽑기 — 하단 가운데 탭. 기존 신발 뽑기(v2 두 칸 — 무료 · 상급)를 그대로 연다.
     * 길은 예전 하위 화면의 "mystery-box" 그대로라 알림 · 지갑 · 공지에서 오는 이동이 같은 자리로 온다.
     */
    data object Draw : Screen(Routes.MYSTERY_BOX, R.string.tab_draw, StepUpIcons.DrawBox)

    data object Community : Screen("community", R.string.tab_community, Icons.Filled.Groups)

    /** 내 정보 — 프로필 · 기록 · 포인트 · 설정 */
    data object Profile : Screen("profile", R.string.tab_me, Icons.Filled.Person)
}

/**
 * 하단 목적지 탭은 다섯이다 — 러닝 / 신발 / 뽑기 / 커뮤니티 / 내 정보(신발 화면 확정안 2026-09-28).
 * 뽑기는 가운데 독립 탭이고, 신발 탭 위의 글자 탭은 내 신발 · 신발 보관함이다.
 *
 * 예전의 뉴스 · 마켓 · 이벤트 탭은 없어진 것이 아니라 자리를 옮겼다.
 *
 *   * 뉴스(대회·건강 소식) → 러닝 안의 "소식"
 *   * 이벤트(챌린지·미션) → 러닝 안의 "챌린지"
 *   * 마켓(스토어·NFT 마켓·보관함) → 꾸미기 안의 "러너 마켓" · "신발 보관함"
 *
 * 길 이름("news" · "events" · "items")은 그대로라, 알림이나 다른 화면에서
 * 그 자리로 가는 이동은 전처럼 동작한다.
 */
private val bottomTabs get() = AppChromePolicy.tabs

/**
 * 하위 화면이 어느 탭 밑에 있는가.
 *
 * 소식을 보고 있으면 러닝 탭에 불이 들어와 있어야 "지금 러닝 안에 있다"가
 * 읽힌다. 불이 꺼지면 사용자는 길을 잃은 것처럼 느낀다.
 */
internal fun parentTabOf(route: String?): Screen? = AppChromePolicy.destination(route)?.parent


object Routes {
    /** 러닝 안의 소식 — 대회 · 러닝·건강 */
    const val NEWS = "news"

    /** 러닝 안의 챌린지 */
    const val EVENTS = "events"

    /** 꾸미기 안의 신발 보관함 · 거래소 · 스토어 */
    const val ITEMS = "items"
    const val MYSTERY_BOX = "mystery-box"

    /** 꾸미기 안의 러너 마켓 — 의상 / 신발 */
    const val RUNNER_MARKET = "runner-market"

    const val RUN = "run"

    /** 러닝 화면 — start=true 면 들어오자마자 달리기를 시작한다 */
    const val RUN_ROUTE = "run?start={start}"
    const val RUN_NOW = "run?start=true"

    /** 러닝 시작 메뉴(시안 U01) · 러닝 챌린지(U02) · 지난 도전(C03) */
    const val RUN_MENU = "run-start"
    const val RUN_GOALS = "run-goals"
    const val RUN_GOAL_HISTORY = "run-goals/history"

    /** 다이어트 모드 — 입력(U05) · 러닝 방법(U06) · 몸 정보·경험 수정(D06) */
    const val RUN_DIET = "run-diet"
    const val RUN_DIET_PLAN = "run-diet/plan"
    const val RUN_DIET_EDIT = "run-diet/edit"
    const val WALLET = "wallet"
    /** 내 정보 › 지갑 › 체인 기록 */
    const val CHAIN_ACTIVITY = "wallet/chain"
    const val NOTIFICATIONS = "notifications"

    /** 앱 공지 상세(알림·공지 v1) — 알림 화면의 "공지"에서. 바깥 소식(NEWS)과 다른 것이다 */
    const val NOTICE = "notice/{id}"
    const val ACHIEVEMENTS = "achievements"
    /** 예전 기록 · 분석(걸음 통계 · 기록 지도) — 러닝 통계의 "걸음 통계와 기록 지도"에서 */
    const val ANALYTICS = "analytics"

    /** 프로필 수정(2026-09-28 전달본) — 내 정보의 "프로필 수정". 사진 · 닉네임, 하단 탭 없이 */
    const val PROFILE_EDIT = "profile/edit"

    /** 편집 화면에서 닉네임을 저장하고 돌아왔다는 표시 — 내 정보 화면의 저장 상태에 한 번 적는다 */
    const val NICKNAME_SAVED = "profile_nickname_saved"

    /** 내 러닝 기록(2026-09-28 전달본) — 내 정보의 "내 러닝 기록 보기". 목록 · 통계 · 지난 러닝 상세 · 경로 확대 */
    const val RECORDS = "records"
    const val RECORD_STATS = "records/stats?month={month}"
    const val RUN_RECORD = "records/run/{id}"
    const val RUN_RECORD_MAP = "records/run/{id}/map"
    /** 설정 첫 목록(설정 v1, 2026-09-28) — 내 정보 첫 화면의 "설정"에서 */
    const val SETTINGS = "settings"
    const val SETTINGS_NOTIFICATIONS = "settings/notifications"
    const val SETTINGS_PRIVACY = "settings/privacy"
    const val SETTINGS_SUPPORT = "settings/support"

    /** 사용 안내 다시 보기(시작·로그인·첫 사용 v1 시안 20) — 도움말 · 문의의 "앱 사용 안내"에서 */
    const val SETTINGS_GUIDE = "settings/guide"
    const val SETTINGS_CONNECTED = "settings/connected"
    const val SETTINGS_LANGUAGE = "settings/language"
    const val SETTINGS_EXPERIENCE = "settings/experience"
    const val SETTINGS_THEME = "settings/theme"
    const val SETTINGS_BODY = "settings/body"
    const val SETTINGS_MODE = "settings/mode"
    const val INVITE = "invite"
    const val CHALLENGE_HISTORY = "challenge-history"
    const val SNEAKER = "sneaker/{id}"
    const val LOBBY = "lobby/{crewId}"
    const val RANKING = "ranking"
    const val CREW_CREATE = "crew/create"
    const val CREW_BOARD = "crew/board/{crewId}"
    const val POST_COMPOSE = "post/compose/{crewId}"
    const val FLASH_DETAIL = "flash/{postId}"
    const val FLASH_LOBBY = "flash/lobby/{postId}"
    const val COURSES = "courses"

    /** 지도 — 내 주변 번개·코스, 땅따먹기 */
    const val MAP = "map"

    /** 기록 지도 — 달린 길을 모두 겹친 히트맵 */
    const val HISTORY_MAP = "analytics/map"
    const val SNEAKER_DEX = "sneaker/dex"

    /**
     * 거래소의 모델 장부 — 속성 × 등급 × 변형 하나.
     *
     * sell 은 보관함에서 "판매"로 들어왔을 때 그 신발의 로컬 번호다. 값을
     * 들고 오면 판매 등록 창이 그 켤레를 고른 채로 열린다. 등록은 값을 적고
     * 확정해야 끝난다 — 번호를 들고 왔다고 저절로 올라가지 않는다.
     */
    const val MARKET_MODEL = "market/{faction}/{rarity}/{variant}?sell={sell}"

    fun sneaker(id: Long) = "sneaker/$id"

    fun marketModel(faction: String, rarity: String, variant: Int, sell: Long = 0) =
        "market/$faction/$rarity/$variant?sell=$sell"
    fun lobby(crewId: String) = "lobby/$crewId"
    fun notice(id: Long) = "notice/$id"
    fun recordStats(month: java.time.YearMonth) = "records/stats?month=$month"
    fun runRecord(id: Long) = "records/run/$id"
    fun runRecordMap(id: Long) = "records/run/$id/map"
    fun crewBoard(crewId: String) = "crew/board/$crewId"

    /** crewId가 비어 있으면 전체 게시판에 쓰는 글 */
    fun postCompose(crewId: String) = "post/compose/${crewId.ifBlank { NO_CREW }}"

    fun flashDetail(postId: Long) = "flash/$postId"

    // ── 동네 이야기(목록형 커뮤니티, 2026-09-27) ──
    /** 글 상세 — 댓글 · 반응 · 내 글 관리 · 신고 */
    const val STORY_DETAIL = "story/{postId}"

    /** 글쓰기 · 고치기 — edit 가 0 이면 새 글, resume 이면 쓰다 만 글을 이어 쓴다 */
    const val STORY_COMPOSE = "story/compose?edit={edit}&resume={resume}"

    /** 내 주변 지도 — 장소와 장소마다 글 수 */
    const val STORY_MAP = "story/map"

    /** 내 주변 — 내 위치 사용 · 지역 직접 선택 */
    const val STORY_LOCATION = "story/location"

    /** 지역 직접 선택 */
    const val STORY_REGION = "story/region"

    fun storyDetail(postId: Long) = "story/$postId"
    fun storyCompose(edit: Long = 0L, resume: Boolean = false) = "story/compose?edit=$edit&resume=$resume"

    fun flashLobby(postId: Long) = "flash/lobby/$postId"

    const val NO_CREW = "_"
}

@Composable
fun StepUpRoot() {
    var ready by rememberSaveable { mutableStateOf(false) }
    // 로그인/가이드는 DataStore 값이 로드될 때까지 null — 스플래시가 그 시간을 가려준다.
    val loginMethod by ServiceLocator.userPrefs.loginMethod
        .collectAsState(initial = null)
    val guideSeen by ServiceLocator.userPrefs.guideSeen
        .collectAsState(initial = null)
    val setupSeen by ServiceLocator.userPrefs.s2SetupSeen
        .collectAsState(initial = null)

    // 로그인 표시와 실제 세션이 어긋나면 로그인 화면을 다시 띄운다.
    //
    // 표시만 보고 통과시키면, 세션을 잃은 사람이 로그인돼 있다고 믿으면서
    // 아무것도 서버에 안 올라가는 상태로 계속 뛰게 된다. 조용히 기록을
    // 잃는 것보다 한 번 더 로그인하는 편이 낫다.
    var sessionChecked by rememberSaveable { mutableStateOf(false) }
    LaunchedEffect(loginMethod) {
        if (loginMethod?.isNotEmpty() == true && !ServiceLocator.sessionHolder.isSignedIn()) {
            // Google 로 로그인했던 사람이다 — 로그인 화면이 "다시 로그인해 주세요"(시작·로그인·첫 사용 v1 시안 09)로 알린다
            if (loginMethod == "google") ServiceLocator.userPrefs.setSignInAgain(true)
            ServiceLocator.userPrefs.setLoginMethod("")
        }
        sessionChecked = true
    }

    Box(Modifier.fillMaxSize()) {
        NightCanvas(Modifier.fillMaxSize())

        val stage = when {
            !ready || loginMethod == null || guideSeen == null || setupSeen == null || !sessionChecked -> 0
            loginMethod!!.isEmpty() -> 1
            // S2 첫 설정은 새로 온 사람(가이드를 아직 안 본 사람)에게만 — 기존 사용자를 다시 붙잡지 않는다
            guideSeen == false && setupSeen == false -> 3
            else -> 2
        }
        // 앱 진입은 불투명도 180ms(시작·로그인·첫 사용 v1) — 동작 줄이기에서는 바로 바뀐다
        Crossfade(stage, animationSpec = tween(LocalMotion.current.duration(180)), label = "entryStage") { visible ->
        when (visible) {
            0 ->
                SplashScreen(onReady = { ready = true })

            1 -> LoginScreen(onDone = {})

            3 -> com.stepup.android.ui.screens.setup.S2SetupFlow(onDone = {})

            else -> MainScaffold(startTour = guideSeen == false)
        }
        }
        // 테스트 APK 새 버전 알림 — 스플래시가 끝난 뒤에만(debug 빌드 · 테스트 중이 아닐 때)
        if (stage != 0) com.stepup.android.ui.components.TestUpdatePrompt()
    }
}

@Composable
internal fun MainScaffold(
    startTour: Boolean = false,
    initialTab: Screen = Screen.Run,
    initialRoute: String = initialTab.route,
) {
    RunFeedback()
    val motion = LocalMotion.current
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    // 첫 사용 안내(시작·로그인·첫 사용 v1 시안 02)는 한 장짜리 시트다 — 아래 FirstGuideSheet. 예전 스포트라이트 투어는 시작하지 않는다.
    // 고른 뒤에는 이 화면에서 다시 열지 않는다("봤음"을 적기 전에 다시 그려져도)
    var firstGuideClosed by rememberSaveable { mutableStateOf(false) }
    // Browsing the app never prompts for run permissions. Start passive tracking only when already allowed.
    LaunchedEffect(Unit) {
        if (StepPermissions.hasActivityRecognition(context)) {
            ServiceLocator.stepRepository.startTracking()
        }
    }

    val navController = rememberNavController()

    // 앱이 죽어 멈춘 러닝이 있으면 묻는다. 저장하던 중에 죽었으면 묻지 않고 다시 저장한다.
    // 걸음 권한이 없으면 러닝 서비스를 띄울 수 없으므로(안드로이드 14 건강 서비스) 권한이 있을 때만.
    LaunchedEffect(Unit) { com.stepup.android.service.WalkSessionService.checkRecovery() }
    val pendingRun by com.stepup.android.service.WalkSessionService.recovery.collectAsState()
    // 지금 로그인한 계정 — 다른 계정(또는 로그인 전)에서 멈춘 러닝은 이어 달리지 않고 저장만 한다
    var currentOwner by remember { mutableStateOf<String?>(null) }
    LaunchedEffect(pendingRun) {
        currentOwner = runCatching { ServiceLocator.sessionHolder.recordingOwner() }.getOrNull()
    }
    pendingRun?.takeIf { currentOwner != null }?.let { checkpoint ->
        if (StepPermissions.hasActivityRecognition(context)) {
            if (checkpoint.phase == com.stepup.android.service.RunCheckpointPhase.SETTLING) {
                LaunchedEffect(checkpoint.state.startedAt) {
                    com.stepup.android.service.WalkSessionService.recover(context, finish = true)
                }
            } else {
                RunRecoveryDialog(
                    checkpoint = checkpoint,
                    sameAccount = checkpoint.state.recordingOwner == currentOwner,
                    onResume = {
                        com.stepup.android.service.WalkSessionService.recover(context, finish = false)
                        navController.navigate(Routes.RUN)
                    },
                    onFinish = {
                        com.stepup.android.service.WalkSessionService.recover(context, finish = true)
                        navController.navigate(Routes.RUN)
                    },
                )
            }
        }
    }

    // 로그인이 필요해 멈췄던 초대 — 로그인하고 새로 열린 앱 화면이면 알림함에서 그 초대를 다시 보인다(자동 수락 없음)
    val scaffoldOpenedAt = remember { android.os.SystemClock.elapsedRealtime() }
    val reopenInvite by ServiceLocator.notificationRepository.inviteAfterSignIn.collectAsState()
    LaunchedEffect(reopenInvite) {
        val request = reopenInvite ?: return@LaunchedEffect
        if (request.requestedAt < scaffoldOpenedAt) navController.navigate(Routes.NOTIFICATIONS) { launchSingleTop = true }
    }

    // 크루 채팅 알림(crew-chat/<id>)으로 들어왔으면 그 방을 연다 — 지금 멤버가 아니면 방이 32 로 넘긴다
    val pendingChat by InviteLinks.pendingChat.collectAsState()
    LaunchedEffect(pendingChat) {
        val crewId = pendingChat ?: return@LaunchedEffect
        navController.openChatRoom(crewId)
        InviteLinks.consumeChat()
    }

    // 초대 링크(stepupcrew.com/c/...)나 크루 알림으로 들어왔으면 그 크루 화면을 연다
    val pendingCrew by InviteLinks.pendingCrew.collectAsState()
    LaunchedEffect(pendingCrew) {
        val crewId = pendingCrew ?: return@LaunchedEffect
        // 크루 명함형(2026-09-28) — 크루의 첫 화면은 상세(게시판 · 같이 달리기는 상세 안에서)
        navController.navigate(com.stepup.android.ui.screens.community.crew.CrewRoutes.detail(crewId))
        InviteLinks.consume()
    }

    val backStack by navController.currentBackStackEntryAsState()
    val currentRoute = backStack?.destination?.route
    val chrome = AppChromePolicy.destination(currentRoute)
    // 뽑기 요청 · 상자 열기 · 결과 · 확인은 화면을 다 쓴다 — 그동안만 공통 머리 · 하단 탭을 걷는다
    val immersive = AppChromePolicy.immersiveAt(currentRoute)
    val showBar = chrome?.showBottomBar == true && !immersive
    val balanceFlow = remember { ServiceLocator.rewardRepository.balance.map<Double, Double?> { it } }
    val balance by balanceFlow.collectAsState(initial = null)
    var wardrobeSetting by rememberSaveable {
        mutableStateOf(com.stepup.android.ui.components.RunnerSetting.Wardrobe)
    }
    // 홈 바탕 사진(HomePhotos.all 의 순번)
    var homePhoto by rememberSaveable {
        mutableIntStateOf(com.stepup.android.ui.components.HomePhotos.initial())
    }
    var profileSetting by rememberSaveable {
        mutableStateOf(com.stepup.android.ui.components.ProfileBackgrounds.settings.random())
    }
    // The scene belongs to the running journey, not to its timer or live data updates.
    val runSetting = rememberSaveable {
        com.stepup.android.ui.components.RunBackgrounds.settings.random()
    }
    // S2 날씨 풍경 — 설정에서 켠 사람만. 홈에 올 때 날씨를 (30분에 한 번까지) 묻고 맞는 풍경을 고른다.
    val weatherOn by ServiceLocator.userPrefs.weatherBackground.collectAsState(initial = false)
    val weather by com.stepup.android.data.weather.WeatherBackground.scene.collectAsState()
    val weatherPick = weather.takeIf { weatherOn }
    LaunchedEffect(weatherOn, currentRoute) {
        if (!weatherOn) com.stepup.android.data.weather.WeatherBackground.clear()
        else if (currentRoute == Screen.Run.route) com.stepup.android.data.weather.WeatherBackground.refresh(context)
    }
    LaunchedEffect(weatherPick) {
        weatherPick?.let { homePhoto = com.stepup.android.ui.components.HomePhotos.forWeather(it, homePhoto) }
    }
    var previousRoute by remember { mutableStateOf<String?>(null) }
    LaunchedEffect(currentRoute) {
        if (currentRoute != null) {
            if (currentRoute == Screen.Run.route && previousRoute in listOf(
                    Screen.Customize.route, Screen.Draw.route, Screen.Community.route, Screen.Profile.route,
                )) {
                homePhoto = com.stepup.android.ui.components.HomePhotos.shuffle(homePhoto, weatherPick)
            }
            previousRoute = currentRoute
        }
    }
    val wardrobeScene = wardrobeSetting.takeIf {
        it in com.stepup.android.ui.components.WardrobeBackgrounds.settings
    } ?: com.stepup.android.ui.components.RunnerSetting.Wardrobe
    val feedback = LocalFeedback.current
    LaunchedEffect(currentRoute, homePhoto, profileSetting, wardrobeScene, runSetting, feedback) {
        if (currentRoute == Screen.Run.route) {
            feedback?.setAmbientScene(when (com.stepup.android.ui.components.HomePhotos.all[homePhoto].mood) {
                com.stepup.android.domain.WeatherScene.DAY, com.stepup.android.domain.WeatherScene.DUSK -> AmbientScene.Dawn
                com.stepup.android.domain.WeatherScene.NIGHT, com.stepup.android.domain.WeatherScene.RAIN -> AmbientScene.Night
            })
            return@LaunchedEffect
        }
        val setting = when (currentRoute) {
            Screen.Customize.route -> null
            Screen.Community.route -> com.stepup.android.ui.components.RunnerSetting.RunSunset
            Screen.Profile.route -> profileSetting
            Routes.RUN_ROUTE -> runSetting
            else -> null
        }
        feedback?.setAmbientScene(when (setting) {
            com.stepup.android.ui.components.RunnerSetting.Wardrobe -> AmbientScene.Wardrobe
            com.stepup.android.ui.components.RunnerSetting.Night,
            com.stepup.android.ui.components.RunnerSetting.HomeBlueNight,
            com.stepup.android.ui.components.RunnerSetting.RunNight -> AmbientScene.Night
            com.stepup.android.ui.components.RunnerSetting.Sunset,
            com.stepup.android.ui.components.RunnerSetting.HomeDawn,
            com.stepup.android.ui.components.RunnerSetting.RunSunset -> AmbientScene.Dawn
            null -> null
        })
    }
    DisposableEffect(feedback) { onDispose { feedback?.setAmbientScene(null) } }

    Box(Modifier.fillMaxSize()) {
    if (currentRoute == Screen.Run.route) {
        Crossfade(com.stepup.android.ui.components.HomePhotos.all[homePhoto], animationSpec = tween(motion.duration(420)), label = "homeBackground") { photo ->
            // 홈 풍경은 화면 전체 바탕 — 가운데 아치를 없앴다(2026-09-26 사용 피드백 · 사용자 결정)
            com.stepup.android.ui.components.S2Scenery(
                photo, Modifier.fillMaxSize().testTag("home-scene-${photo.key}"),
            )
        }
    } else if (currentRoute == Screen.Customize.route) {
        com.stepup.android.ui.components.RunnerScene(
            Modifier.fillMaxSize().testTag("wardrobe-scene-${wardrobeScene.name}"),
            wardrobeScene, wardrobe = true,
        )
    } else if (currentRoute == Screen.Community.route) {
        com.stepup.android.ui.components.RunnerScene(
            Modifier.fillMaxSize(), com.stepup.android.ui.components.RunnerSetting.RunSunset,
        )
    } else if (currentRoute == Screen.Profile.route) {
        com.stepup.android.ui.components.RunnerScene(
            Modifier.fillMaxSize().testTag("profile-scene-${profileSetting.name}"),
            profileSetting, home = true,
        )
    } else if (currentRoute in listOf(
            Routes.ITEMS, Routes.RUNNER_MARKET, Routes.SNEAKER_DEX,
            Routes.SNEAKER, Routes.MARKET_MODEL,
        )) {
        com.stepup.android.ui.components.CommerceBackdrop(Modifier.fillMaxSize())
    } else if (currentRoute == Routes.POST_COMPOSE) {
        com.stepup.android.ui.components.CommerceBackdrop(Modifier.fillMaxSize())
    } else if (currentRoute == Routes.COURSES) {
        com.stepup.android.ui.components.RunnerScene(
            Modifier.fillMaxSize(), com.stepup.android.ui.components.RunnerSetting.RunNight,
        )
    } else if (chrome?.header == AppChromePolicy.Header.Focus) {
        com.stepup.android.ui.components.RunnerScene(
            Modifier.fillMaxSize(), runSetting,
        )
    } else {
        com.stepup.android.ui.components.CommerceBackdrop(Modifier.fillMaxSize())
    }
    Scaffold(
        containerColor = Color.Transparent,
        topBar = {
            if (chrome?.header == AppChromePolicy.Header.Main && !immersive) {
                MainHeader(
                    balance = balance,
                    onOpenWallet = { navController.navigate(Routes.WALLET) },
                    modifier = Modifier.statusBarsPadding().padding(horizontal = StepUpDesign.Gutter),
                    balanceModifier = Modifier.guideTarget(GuideTour.Targets.HOME_TOKEN),
                )
            }
        },
        bottomBar = {
            AnimatedVisibility(
                visible = showBar,
                enter = slideInVertically { it } + fadeIn(),
                exit = slideOutVertically { it } + fadeOut(),
            ) {
                VoltNavBar(navController, currentRoute)
            }
        },
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = initialRoute,
            modifier = Modifier.padding(innerPadding).consumeWindowInsets(innerPadding),
            enterTransition = { fadeIn(tween(motion.duration(180))) + slideInHorizontally(tween(motion.duration(220))) { if (motion.reduced) 0 else it / 18 } },
            exitTransition = { fadeOut(tween(motion.duration(140))) },
            popEnterTransition = { fadeIn(tween(motion.duration(180))) },
            popExitTransition = { fadeOut(tween(motion.duration(140))) + slideOutHorizontally(tween(motion.duration(220))) { if (motion.reduced) 0 else it / 18 } },
        ) {
            composable(Screen.Run.route) {
                HomeScreen(
                    onStartRun = {
                        // 달리던 러닝이 있으면 그 러닝으로. 아니면 시작 메뉴(시안 U01)에서 고른다
                        if (com.stepup.android.service.WalkSessionService.state.value.isActive) {
                            navController.navigate(Routes.RUN_NOW)
                        } else {
                            navController.navigate(Routes.RUN_MENU)
                        }
                    },
                    onOpenWallet = { navController.navigate(Routes.WALLET) },
                    onOpenNews = { navController.navigate(Routes.NEWS) },
                    onOpenCustomize = { navController.switchTab(Screen.Customize) },
                    weatherScene = weatherPick.takeIf { com.stepup.android.ui.components.HomePhotos.all[homePhoto].suits(it) },
                    onPreviousBackground = {
                        homePhoto = com.stepup.android.ui.components.HomePhotos.previous(homePhoto)
                    },
                    onNextBackground = {
                        homePhoto = com.stepup.android.ui.components.HomePhotos.next(homePhoto)
                    },
                )
            }
            composable(Routes.MYSTERY_BOX) {
                // 신발 뽑기 v2(두 칸, 2026-09-28 전달본) — 모두 무료. 수 · 연결 상태 · 결과는 서버(draw_status · draw_free ·
                // premium_draw)가 정한다. 하단 가운데 뽑기 탭의 첫 화면이라 공통 머리(로고 · 잔액) · 하단 탭 아래에 두 칸을 둔다.
                val drawVm: com.stepup.android.ui.screens.gacha.DrawViewModel =
                    androidx.lifecycle.viewmodel.compose.viewModel(factory = com.stepup.android.ui.screens.gacha.DrawViewModel.Factory)
                val drawState by drawVm.state.collectAsStateWithLifecycle()
                val drawFlow by drawVm.flow.collectAsStateWithLifecycle()
                val drawPending by drawVm.pending.collectAsStateWithLifecycle()
                val drawNotice by drawVm.notice.collectAsStateWithLifecycle()
                // 요청 · 상자 열기 · 결과 · 확인은 v2 시안처럼 화면을 다 쓴다 — 두 칸으로 돌아오거나 탭을 떠나면 공통 머리 · 하단 탭이 돌아온다
                val drawImmersive = drawFlow != com.stepup.android.ui.screens.gacha.DrawFlow.Home
                DisposableEffect(drawImmersive) {
                    AppChromePolicy.immersive = drawImmersive
                    onDispose { AppChromePolicy.immersive = false }
                }
                // 지갑 페이지에서 연결하고 오거나 러닝을 마치고 오면 수가 바뀌어 있다 — 돌아올 때마다 다시 읽는다
                androidx.lifecycle.compose.LifecycleResumeEffect(Unit) {
                    drawVm.refresh()
                    onPauseOrDispose { }
                }
                // 21 — 신발 탭(내 신발)으로. 받은 신발이 있으면 그 신발을 고른 채로(착용은 바꾸지 않는다)
                val openShoes: (Long?) -> Unit = { shoeId ->
                    navController.switchTab(Screen.Customize)
                    if (shoeId != null) {
                        runCatching { navController.getBackStackEntry(Screen.Customize.route) }.getOrNull()
                            ?.savedStateHandle?.set(RECEIVED_SHOE_KEY, shoeId)
                    }
                }
                val drawScope = rememberCoroutineScope()
                com.stepup.android.ui.screens.gacha.MysteryBoxScreen(
                    state = drawState,
                    flow = drawFlow,
                    pending = drawPending,
                    notice = drawNotice,
                    actions = remember(drawVm) {
                        com.stepup.android.ui.screens.gacha.DrawActions(
                            // 불러오기 실패 시트의 "내 신발로 돌아가기" — 신발 탭으로(뽑기는 이제 하단 탭이라 뒤로 쌓이지 않는다)
                            onBack = { navController.switchTab(Screen.Customize) },
                            onDraw = drawVm::draw,
                            // 상급 뽑기의 지갑 연결은 웹 지갑 페이지에서 한다(서명 · 2단계 인증). 주소가 없으면 지갑 화면으로.
                            // 돌아와서 서버의 연결 여부가 바뀐 것을 읽었을 때만 연결 완료(08)를 보인다
                            onConnectWallet = {
                                drawVm.watchWalletLink()
                                drawScope.openWalletPage(context) { navController.navigate(Routes.WALLET) }
                            },
                            onRetry = drawVm::refresh,
                            onCheckPending = drawVm::checkPending,
                            onLeaveFlow = drawVm::leaveFlow,
                            onFinishOpening = drawVm::finishOpening,
                            onCloseResult = drawVm::closeResult,
                            onOpenShoes = { shoeId ->
                                drawVm.closeResult()
                                openShoes(shoeId)
                            },
                            // 17 "러닝 시작" — 기존 자유 러닝 시작(러닝 중이면 그 러닝으로)
                            onStartRun = {
                                if (!com.stepup.android.service.WalkSessionService.state.value.isActive) {
                                    com.stepup.android.ui.screens.events.ChallengeRunFocus.clear()
                                    com.stepup.android.domain.RunPlans.set(com.stepup.android.domain.RunPlan.Free)
                                }
                                navController.navigate(Routes.RUN_NOW)
                            },
                            onNoticeDone = drawVm::consumeNotice,
                        )
                    },
                )
            }
            composable(Screen.Customize.route) { entry ->
                // 뽑기 결과의 "내 신발 보기"(21) — 받은 신발을 고른 채로 연다. 한 번 고르면 비운다
                val receivedShoe by entry.savedStateHandle.getStateFlow<Long?>(RECEIVED_SHOE_KEY, null)
                    .collectAsStateWithLifecycle()
                CustomizeScreen(
                    focusShoeId = receivedShoe,
                    onFocusShoeShown = { entry.savedStateHandle[RECEIVED_SHOE_KEY] = null },
                    onBack = { navController.switchTab(Screen.Run) },
                    onChangeBackground = {
                        wardrobeSetting = com.stepup.android.ui.components.WardrobeBackgrounds.next(wardrobeScene)
                    },
                    onOpenWallet = { navController.navigate(Routes.WALLET) },
                    onOpenMarket = { navController.navigate(Routes.RUNNER_MARKET) },
                    onOpenVault = { navController.navigate(Routes.ITEMS) },
                    onOpenDex = { navController.navigate(Routes.SNEAKER_DEX) },
                    onOpenMarketModel = { faction, rarity, variant -> navController.navigate(Routes.marketModel(faction, rarity, variant)) },
                    onOpenSneaker = { id -> navController.navigate(Routes.sneaker(id)) },
                    onOpenDraw = { navController.switchTab(Screen.Draw) },
                )
            }
            composable(Routes.RUNNER_MARKET) {
                RunnerMarketScreen(
                    onBack = { navController.popBackStack() },
                    onOpenWallet = { navController.navigate(Routes.WALLET) },
                    onOpenModel = { faction, rarity, variant ->
                        navController.navigate(Routes.marketModel(faction, rarity, variant))
                    },
                    onOpenVault = { navController.navigate(Routes.ITEMS) },
                )
            }
            composable(Screen.Community.route) {
                CommunityScreen(
                    onOpenLobby = { crewId -> navController.navigate(Routes.lobby(crewId)) },
                    onOpenNotifications = { navController.navigate(Routes.NOTIFICATIONS) },
                    onOpenRanking = { navController.navigate(Routes.RANKING) },
                    onOpenCrew = { crewId -> navController.navigate(Routes.crewBoard(crewId)) },
                    onCreateCrew = { resume ->
                        navController.navigate(
                            com.stepup.android.ui.screens.community.crew.CrewRoutes.draft(com.stepup.android.domain.CrewDraftMode.CREATE, resume = resume),
                        )
                    },
                    onWritePost = { crewId -> navController.navigate(Routes.postCompose(crewId)) },
                    onOpenFlash = { postId -> navController.navigate(Routes.flashDetail(postId)) },
                    onOpenMap = { navController.navigate(Routes.MAP) },
                    onOpenStory = { postId -> navController.navigate(Routes.storyDetail(postId)) },
                    onOpenStoryMap = { place ->
                        com.stepup.android.ui.screens.community.stories.StoryMapSeed.place = place
                        navController.navigate(Routes.STORY_MAP)
                    },
                    onWriteStory = { resume -> navController.navigate(Routes.storyCompose(resume = resume)) },
                    onOpenStoryLocation = { navController.navigate(Routes.STORY_LOCATION) },
                    onOpenStoryRegion = { navController.navigate(Routes.STORY_REGION) },
                    crewActions = com.stepup.android.ui.screens.community.crew.crewListActions(navController),
                    onOpenChat = { crewId -> navController.openChatRoom(crewId) },
                )
            }
            composable(
                route = Routes.STORY_DETAIL,
                arguments = listOf(navArgument("postId") { type = NavType.LongType }),
            ) {
                com.stepup.android.ui.screens.community.stories.StoryDetailScreen(
                    onBack = { navController.popBackStack() },
                    onOpenPlace = { place ->
                        com.stepup.android.ui.screens.community.stories.StoryMapSeed.place = place
                        navController.navigate(Routes.STORY_MAP)
                    },
                    onEdit = { postId -> navController.navigate(Routes.storyCompose(edit = postId)) },
                )
            }
            composable(
                route = Routes.STORY_COMPOSE,
                arguments = listOf(
                    navArgument("edit") { type = NavType.LongType; defaultValue = 0L },
                    navArgument("resume") { type = NavType.BoolType; defaultValue = false },
                ),
            ) {
                com.stepup.android.ui.screens.community.stories.StoryComposeScreen(
                    onBack = { navController.popBackStack() },
                    // 올린 뒤에는 목록으로 — 고친 글이면 그 글의 상세로 돌아간다
                    onDone = { navController.popBackStack() },
                    // 쓰던 글은 저장된 뒤다. 돌아오면(뒤로) 글쓰기가 그대로 있고, 새로 열면 "작성하던 글이 있어요"로 이어 쓴다
                    onStartRun = {
                        if (com.stepup.android.service.WalkSessionService.state.value.isActive) {
                            navController.navigate(Routes.RUN_NOW)
                        } else {
                            navController.navigate(Routes.RUN_MENU)
                        }
                    },
                    onOpenRecords = { navController.navigate(Routes.RECORDS) },
                )
            }
            composable(Routes.STORY_MAP) { entry ->
                // 목록과 같은 뷰모델 — 범위 · 장소 필터를 함께 쓴다(커뮤니티 탭이 뒤에 있을 때)
                val owner = remember(entry) {
                    runCatching { navController.getBackStackEntry(Screen.Community.route) }.getOrNull() ?: entry
                }
                val stories: com.stepup.android.ui.screens.community.stories.StoriesViewModel =
                    androidx.lifecycle.viewmodel.compose.viewModel(
                        viewModelStoreOwner = owner,
                        factory = com.stepup.android.ui.screens.community.stories.StoriesViewModel.Factory,
                    )
                com.stepup.android.ui.screens.community.stories.StoryMapScreen(
                    viewModel = stories,
                    onBack = { navController.popBackStack() },
                    onOpenPost = { postId -> navController.navigate(Routes.storyDetail(postId)) },
                    onPlaceFeed = {
                        if (!navController.popBackStack(Screen.Community.route, inclusive = false)) navController.popBackStack()
                    },
                )
            }
            composable(Routes.STORY_LOCATION) {
                com.stepup.android.ui.screens.community.stories.StoryLocationScreen(
                    onBack = { navController.popBackStack() },
                    onChooseRegion = { navController.navigate(Routes.STORY_REGION) },
                )
            }
            composable(Routes.STORY_REGION) {
                com.stepup.android.ui.screens.community.stories.StoryRegionScreen(
                    onBack = { navController.popBackStack() },
                    // 고르면 목록으로 곧장 — 위치 안내 화면을 거쳐 왔어도 그 화면까지 닫는다
                    onChosen = {
                        if (!navController.popBackStack(Screen.Community.route, inclusive = false)) navController.popBackStack()
                    },
                )
            }
            composable(Routes.MAP) {
                MapScreen(
                    onBack = { navController.popBackStack() },
                    onOpenFlash = { postId -> navController.navigate(Routes.flashDetail(postId)) },
                    onOpenCourses = { navController.navigate(Routes.COURSES) },
                )
            }
            composable(Routes.ITEMS) {
                ItemsScreen(
                    onBack = { navController.popBackStack() },
                    onOpenSneaker = { id -> navController.navigate(Routes.sneaker(id)) },
                    onOpenDex = { navController.navigate(Routes.SNEAKER_DEX) },
                    onOpenDraw = { navController.switchTab(Screen.Draw) },
                    onOpenMarketModel = { faction, rarity, variant ->
                        navController.navigate(Routes.marketModel(faction, rarity, variant))
                    },
                )
            }
            composable(
                route = Routes.MARKET_MODEL,
                arguments = listOf(
                    navArgument("faction") { type = NavType.StringType },
                    navArgument("rarity") { type = NavType.StringType },
                    navArgument("variant") { type = NavType.IntType },
                    navArgument("sell") {
                        type = NavType.LongType
                        defaultValue = 0L
                    },
                ),
            ) { entry ->
                MarketModelScreen(
                    faction = entry.arguments?.getString("faction").orEmpty(),
                    rarity = entry.arguments?.getString("rarity").orEmpty(),
                    variant = entry.arguments?.getInt("variant") ?: 0,
                    sellLocalId = entry.arguments?.getLong("sell") ?: 0L,
                    onBack = { navController.popBackStack() },
                )
            }
            composable(Routes.NEWS) {
                NewsScreen(
                    onBack = { navController.popBackStack() },
                    onOpenWallet = { navController.navigate(Routes.WALLET) },
                )
            }
            composable(Routes.EVENTS) {
                EventsScreen(
                    onBack = { navController.popBackStack() },
                    onOpenWallet = { navController.navigate(Routes.WALLET) },
                    onStartRun = {
                        com.stepup.android.domain.RunPlans.clear()
                        navController.navigate(Routes.RUN_NOW)
                    },
                )
            }
            composable(Screen.Profile.route) { entry ->
                val nicknameSaved by entry.savedStateHandle.getStateFlow(Routes.NICKNAME_SAVED, false).collectAsState()
                ProfileScreen(
                    onChangeBackground = {
                        profileSetting = com.stepup.android.ui.components.ProfileBackgrounds.next(profileSetting)
                    },
                    onOpenCustomize = { navController.switchTab(Screen.Customize) },
                    onOpenChallenges = { navController.navigate(Routes.EVENTS) },
                    onOpenWallet = { navController.navigate(Routes.WALLET) },
                    onOpenAnalytics = { navController.navigate(Routes.RECORDS) },
                    onOpenChallengeHistory = { navController.navigate(Routes.CHALLENGE_HISTORY) },
                    onOpenSettings = { navController.navigate(Routes.SETTINGS) },
                    onOpenProfileEdit = { navController.navigate(Routes.PROFILE_EDIT) },
                    nicknameSaved = nicknameSaved,
                    onNicknameNoticeShown = { entry.savedStateHandle[Routes.NICKNAME_SAVED] = false },
                )
            }
            // 프로필 수정 — 닉네임을 실제로 저장한 뒤에만 내 정보에 "닉네임을 저장했어요"
            composable(Routes.PROFILE_EDIT) {
                com.stepup.android.ui.screens.profile.ProfileEditScreen(
                    onBack = { navController.popBackStack() },
                    onSaved = {
                        navController.previousBackStackEntry?.savedStateHandle?.set(Routes.NICKNAME_SAVED, true)
                        navController.popBackStack()
                    },
                )
            }
            // 설정 첫 목록 — 세 그룹(러닝과 알림 · 앱 사용 · 계정과 도움말)과 예전 목록에만 있던 길(더 보기 · 앱 정보)
            composable(Routes.SETTINGS) {
                com.stepup.android.ui.screens.settings.SettingsHomeScreen(
                    onBack = { navController.popBackStack() },
                    onOpenNotificationSettings = { navController.navigate(Routes.SETTINGS_NOTIFICATIONS) },
                    onOpenPrivacy = { navController.navigate(Routes.SETTINGS_PRIVACY) },
                    onOpenLanguage = { navController.navigate(Routes.SETTINGS_LANGUAGE) },
                    onOpenTheme = { navController.navigate(Routes.SETTINGS_THEME) },
                    onOpenExperience = { navController.navigate(Routes.SETTINGS_EXPERIENCE) },
                    onOpenConnected = { navController.navigate(Routes.SETTINGS_CONNECTED) },
                    onOpenSupport = { navController.navigate(Routes.SETTINGS_SUPPORT) },
                    onOpenInbox = { navController.navigate(Routes.NOTIFICATIONS) },
                    onOpenAchievements = { navController.navigate(Routes.ACHIEVEMENTS) },
                    onOpenInvite = { navController.navigate(Routes.INVITE) },
                    onOpenBody = { navController.navigate(Routes.SETTINGS_BODY) },
                    onOpenMode = { navController.navigate(Routes.SETTINGS_MODE) },
                )
            }

            composable(
                route = Routes.RUN_ROUTE,
                arguments = listOf(
                    navArgument("start") {
                        type = NavType.BoolType
                        defaultValue = false
                    },
                ),
            ) { entry ->
                RunScreen(
                    onBack = { navController.popBackStack() },
                    onOpenCourses = { navController.navigate(Routes.COURSES) },
                    // "처음 화면으로" — 시작 메뉴(시안 U01). 메뉴 없이 들어왔으면 러닝 탭 첫 화면
                    onHome = {
                        if (!navController.popBackStack(Routes.RUN_MENU, inclusive = false) &&
                            !navController.popBackStack(Screen.Run.route, inclusive = false)) navController.popBackStack()
                    },
                    onGoals = {
                        if (!navController.popBackStack(Routes.RUN_GOALS, inclusive = false)) navController.popBackStack()
                    },
                    // 같은 방법 다시 하기 — 같은 계획으로 새 러닝(3-2-1부터)
                    onRepeat = { plan ->
                        com.stepup.android.domain.RunPlans.set(plan)
                        navController.navigate(Routes.RUN_NOW) { popUpTo(Routes.RUN_ROUTE) { inclusive = true } }
                    },
                    autoStart = entry.arguments?.getBoolean("start") ?: false,
                    // 권한 안내의 "홈으로 돌아가기"(시안 14 · 15) — 러닝 탭 첫 화면
                    onLeaveToHome = {
                        if (!navController.popBackStack(Screen.Run.route, inclusive = false)) navController.switchTab(Screen.Run)
                    },
                )
            }
            composable(Routes.RUN_MENU) {
                val savedExperience by com.stepup.android.core.ServiceLocator.userPrefs.runExperience.collectAsState(initial = null)
                // 새로 시작하는 러닝은 예전 챌린지 · 계획과 묶지 않는다
                val startFresh = { plan: com.stepup.android.domain.RunPlan ->
                    com.stepup.android.ui.screens.events.ChallengeRunFocus.clear()
                    com.stepup.android.domain.RunPlans.set(plan)
                    // 권한 안내 콜백이 겹쳐도 러닝 화면을 두 번 쌓지 않는다
                    navController.navigate(Routes.RUN_NOW) { launchSingleTop = true }
                }
                com.stepup.android.ui.screens.walk.RunStartMenuScreen(
                    onBack = { navController.popBackStack() },
                    // 자유 러닝 — 이 메뉴 위에서 필요한 권한 안내(시안 13~19)를 마친 뒤에만 불린다
                    onFreeRun = { startFresh(com.stepup.android.domain.RunPlan.Free) },
                    onGoals = { navController.navigate(Routes.RUN_GOALS) },
                    onDiet = {
                        // 러닝 경험을 이미 골랐으면 러닝 방법으로 바로, 아니면 입력부터
                        navController.navigate(if (savedExperience != null) Routes.RUN_DIET_PLAN else Routes.RUN_DIET)
                    },
                    // 권한 안내의 "홈으로 돌아가기"(시안 14 · 15)
                    onHome = {
                        if (!navController.popBackStack(Screen.Run.route, inclusive = false)) navController.switchTab(Screen.Run)
                    },
                )
            }
            composable(Routes.RUN_GOALS) {
                com.stepup.android.ui.screens.walk.RunGoalsScreen(
                    onBack = { navController.popBackStack() },
                    onStart = { goal ->
                        com.stepup.android.ui.screens.events.ChallengeRunFocus.clear()
                        com.stepup.android.domain.RunPlans.set(com.stepup.android.domain.RunPlan.Goal(goal))
                        navController.navigate(Routes.RUN_NOW)
                    },
                    onHistory = { navController.navigate(Routes.RUN_GOAL_HISTORY) },
                )
            }
            composable(Routes.RUN_DIET) {
                com.stepup.android.ui.screens.walk.DietInputScreen(
                    onBack = { navController.popBackStack() },
                    onNext = {
                        navController.navigate(Routes.RUN_DIET_PLAN) { popUpTo(Routes.RUN_DIET) { inclusive = true } }
                    },
                )
            }
            composable(Routes.RUN_DIET_EDIT) {
                com.stepup.android.ui.screens.walk.DietInputScreen(
                    onBack = { navController.popBackStack() },
                    onNext = { navController.popBackStack() },
                    editing = true,
                )
            }
            composable(Routes.RUN_DIET_PLAN) {
                com.stepup.android.ui.screens.walk.DietPlanScreen(
                    onBack = { navController.popBackStack() },
                    onEdit = { navController.navigate(Routes.RUN_DIET_EDIT) },
                    onStart = { experience ->
                        com.stepup.android.ui.screens.events.ChallengeRunFocus.clear()
                        com.stepup.android.domain.RunPlans.set(com.stepup.android.domain.RunPlan.Diet(experience))
                        navController.navigate(Routes.RUN_NOW)
                    },
                )
            }
            composable(Routes.RUN_GOAL_HISTORY) {
                com.stepup.android.ui.screens.walk.RunGoalHistoryScreen(onBack = { navController.popBackStack() })
            }
            composable(Routes.COURSES) {
                CourseHubScreen(onBack = { navController.popBackStack() })
            }
            composable(Routes.WALLET) {
                val walletScope = rememberCoroutineScope()
                WalletScreen(
                    onBack = { navController.popBackStack() },
                    onOpenChain = { navController.navigate(Routes.CHAIN_ACTIVITY) { launchSingleTop = true } },
                    // 연결 상태 → 기존 두 칸 뽑기 탭(자동으로 뽑지 않는다)
                    onOpenDraw = { navController.switchTab(Screen.Draw) },
                    // 연결하기 · 지갑 페이지 — 기존 웹 지갑 페이지. 주소를 만들지 못하면(연결) 여기서 알린다
                    onOpenWalletPage = {
                        walletScope.openWalletPage(context) {
                            android.widget.Toast.makeText(context, context.getString(R.string.wallet_web_offline), android.widget.Toast.LENGTH_SHORT).show()
                        }
                    },
                )
            }
            composable(Routes.CHAIN_ACTIVITY) {
                com.stepup.android.ui.screens.rewards.ChainActivityScreen(onBack = { navController.popBackStack() })
            }
            composable(Routes.NOTIFICATIONS) {
                NotificationsScreen(
                    onOpenChallenges = { navController.navigate(Routes.EVENTS) },
                    onBack = { navController.popBackStack() },
                    onOpenSettings = { navController.navigate(Routes.SETTINGS_NOTIFICATIONS) },
                    onOpenLobby = { crewId -> navController.navigate(Routes.lobby(crewId)) },
                    onOpenCrew = { crewId -> navController.navigate(com.stepup.android.ui.screens.community.crew.CrewRoutes.detail(crewId)) },
                    // 댓글은 게시판 위에 창으로 뜬다. 어느 댓글인지는 저장소에
                    // 남겨 두고 커뮤니티 탭으로 보내면, 게시판이 그 창을 연다.
                    onOpenComment = { target ->
                        ServiceLocator.communityRepository.focusComment(target)
                        navController.switchTab(Screen.Community)
                    },
                    onOpenWallet = { navController.navigate(Routes.WALLET) },
                    // 받은 신발은 보유 목록(신발 탭)에서 — 알림의 모델 이름으로 한 켤레를 골라 열지 않는다
                    onOpenShoes = { navController.switchTab(Screen.Customize) },
                    onOpenCommunity = { navController.switchTab(Screen.Community) },
                    onOpenNotice = { id -> navController.navigate(Routes.notice(id)) },
                    onSignIn = {
                        scope.launch {
                            try {
                                com.stepup.android.ui.components.returnToSignIn(context)
                            } catch (cancelled: kotlinx.coroutines.CancellationException) {
                                throw cancelled
                            } catch (_: Exception) {
                                android.widget.Toast.makeText(context, R.string.feed_save_failed, android.widget.Toast.LENGTH_SHORT).show()
                            }
                        }
                    },
                )
            }
            composable(
                route = Routes.NOTICE,
                arguments = listOf(navArgument("id") { type = NavType.LongType }),
            ) { entry ->
                com.stepup.android.ui.screens.notifications.NoticeDetailScreen(
                    id = entry.arguments?.getLong("id") ?: 0L,
                    onBack = { navController.popBackStack() },
                    onAction = { action ->
                        when (action) {
                            // 신발 뽑기(무료 · 상급 두 칸)로 옮겨 갈 뿐 기회를 쓰지 않는다
                            com.stepup.android.data.repo.NoticeAction.DRAW -> navController.switchTab(Screen.Draw)
                            com.stepup.android.data.repo.NoticeAction.RUN_HISTORY -> navController.navigate(Routes.RECORDS)
                            com.stepup.android.data.repo.NoticeAction.NOTIFICATION_SETTINGS ->
                                navController.navigate(Routes.SETTINGS_NOTIFICATIONS)
                            com.stepup.android.data.repo.NoticeAction.PRIVACY_SETTINGS -> navController.navigate(Routes.SETTINGS_PRIVACY)
                        }
                    },
                )
            }
            composable(Routes.ACHIEVEMENTS) {
                AchievementsScreen(onBack = { navController.popBackStack() })
            }
            // 내 러닝 기록 — 목록(기간 · 스크롤은 상세 · 통계에 다녀와도 그대로) → 통계 · 지난 러닝 상세 → 경로 확대
            composable(Routes.RECORDS) {
                com.stepup.android.ui.screens.records.RecordsScreen(
                    onBack = { navController.popBackStack() },
                    onOpenStats = { month -> navController.navigate(Routes.recordStats(month)) },
                    onOpenRun = { id -> navController.navigate(Routes.runRecord(id)) },
                    // 기록이 하나도 없을 때(07) — 달리던 러닝이 있으면 그 러닝으로, 아니면 기존 자유 러닝 시작
                    onStartRun = {
                        if (com.stepup.android.service.WalkSessionService.state.value.isActive) {
                            navController.navigate(Routes.RUN_NOW)
                        } else {
                            com.stepup.android.ui.screens.events.ChallengeRunFocus.clear()
                            com.stepup.android.domain.RunPlans.set(com.stepup.android.domain.RunPlan.Free)
                            navController.navigate(Routes.RUN_NOW)
                        }
                    },
                )
            }
            composable(
                route = Routes.RECORD_STATS,
                arguments = listOf(navArgument("month") { type = NavType.StringType; nullable = true; defaultValue = null }),
            ) { entry ->
                val month = entry.arguments?.getString("month")
                    ?.let { runCatching { java.time.YearMonth.parse(it) }.getOrNull() } ?: java.time.YearMonth.now()
                com.stepup.android.ui.screens.records.RecordStatsScreen(
                    start = month,
                    onBack = { navController.popBackStack() },
                    onOpenStepStats = { navController.navigate(Routes.ANALYTICS) },
                )
            }
            composable(
                route = Routes.RUN_RECORD,
                arguments = listOf(navArgument("id") { type = NavType.LongType }),
            ) { entry ->
                val id = entry.arguments?.getLong("id") ?: 0L
                com.stepup.android.ui.screens.records.RunRecordScreen(
                    id = id,
                    onBack = { navController.popBackStack() },
                    onOpenMap = { navController.navigate(Routes.runRecordMap(id)) },
                    // 실제로 지운 뒤에만 — 목록은 저장소를 보고 있어 합계 · 줄이 바로 바뀐다
                    onDeleted = { navController.popBackStack() },
                )
            }
            composable(
                route = Routes.RUN_RECORD_MAP,
                arguments = listOf(navArgument("id") { type = NavType.LongType }),
            ) { entry ->
                com.stepup.android.ui.screens.records.RunRouteMapScreen(
                    id = entry.arguments?.getLong("id") ?: 0L,
                    onBack = { navController.popBackStack() },
                )
            }
            composable(Routes.ANALYTICS) {
                AnalyticsScreen(
                    onBack = { navController.popBackStack() },
                    onOpenHistoryMap = { navController.navigate(Routes.HISTORY_MAP) },
                )
            }
            composable(Routes.HISTORY_MAP) {
                HistoryMapScreen(onBack = { navController.popBackStack() })
            }
            composable(Routes.SETTINGS_NOTIFICATIONS) {
                NotificationSettingsScreen(onBack = { navController.popBackStack() })
            }
            composable(Routes.SETTINGS_PRIVACY) {
                PrivacyScreen(onBack = { navController.popBackStack() })
            }
            composable(Routes.SETTINGS_SUPPORT) {
                SupportScreen(
                    onBack = { navController.popBackStack() },
                    // 앱 사용 안내 — 한 장짜리 다시 보기(시작·로그인·첫 사용 v1 시안 20). 뒤로 가면 여기로 돌아온다
                    onOpenGuide = { navController.navigate(Routes.SETTINGS_GUIDE) { launchSingleTop = true } },
                )
            }
            composable(Routes.SETTINGS_GUIDE) {
                // 가이드를 "안 봄"으로 되돌리지 않는다 — 첫 안내가 다시 뜨지 않는다
                com.stepup.android.ui.screens.onboarding.GuideReplayScreen(
                    onBack = { navController.popBackStack() },
                    onRunHome = { navController.switchTab(Screen.Run) },
                )
            }
            composable(Routes.SETTINGS_CONNECTED) {
                val walletScope = rememberCoroutineScope()
                ConnectedAccountsScreen(
                    onBack = { navController.popBackStack() },
                    onConnectWallet = { walletScope.openWalletPage(context) { navController.navigate(Routes.WALLET) } },
                )
            }
            composable(Routes.SETTINGS_EXPERIENCE) {
                com.stepup.android.ui.screens.settings.ExperienceSettingsScreen { navController.popBackStack() }
            }
            composable(Routes.SETTINGS_BODY) {
                com.stepup.android.ui.screens.setup.BodySettingsScreen(onBack = { navController.popBackStack() })
            }
            composable(Routes.CHALLENGE_HISTORY) {
                com.stepup.android.ui.screens.events.ChallengeHistoryScreen(
                    onBack = { navController.popBackStack() },
                    onOpenChallenges = { navController.navigate(Routes.EVENTS) },
                )
            }
            composable(Routes.INVITE) {
                com.stepup.android.ui.screens.invite.InviteScreen(onBack = { navController.popBackStack() })
            }
            composable(Routes.SETTINGS_MODE) {
                com.stepup.android.ui.screens.setup.ModeSettingsScreen(onBack = { navController.popBackStack() })
            }
            composable(Routes.SETTINGS_THEME) {
                ThemeScreen(onBack = { navController.popBackStack() })
            }
            composable(Routes.SETTINGS_LANGUAGE) {
                LanguageScreen(onBack = { navController.popBackStack() })
            }
            composable(
                route = Routes.SNEAKER,
                arguments = listOf(navArgument("id") { type = NavType.LongType }),
            ) { entry ->
                SneakerDetailScreen(
                    sneakerId = entry.arguments?.getLong("id") ?: 0L,
                    onBack = { navController.popBackStack() },
                    onSell = { faction, rarity, variant, localId ->
                        navController.navigate(
                            Routes.marketModel(faction, rarity, variant, localId),
                        )
                    },
                    // 보유 신발 상세 v1 — 조회 실패(14) · 없는 신발(15)에서 "보유 신발로 돌아가기": 신발 탭의 최신 목록으로
                    onOpenOwned = {
                        if (!navController.popBackStack(Screen.Customize.route, inclusive = false)) {
                            navController.popBackStack()
                            navController.switchTab(Screen.Customize)
                        }
                    },
                )
            }
            composable(
                route = Routes.LOBBY,
                arguments = listOf(navArgument("crewId") { type = NavType.StringType }),
            ) { entry ->
                PartyLobbyScreen(
                    crewId = entry.arguments?.getString("crewId").orEmpty(),
                    onBack = { navController.popBackStack() },
                    onRunStarted = {
                        // 모임 러닝은 챌린지 상세에서 시작한 러닝이 아니다
                        com.stepup.android.ui.screens.events.ChallengeRunFocus.clear()
                        com.stepup.android.domain.RunPlans.clear()
                        navController.navigate(Routes.RUN)
                    },
                )
            }
            composable(Routes.SNEAKER_DEX) {
                SneakerDexScreen(
                    onBack = { navController.popBackStack() },
                    onOpenSneaker = { id -> navController.navigate(Routes.sneaker(id)) },
                )
            }
            composable(Routes.RANKING) {
                RankingScreen(onBack = { navController.popBackStack() })
            }
            // 예전 "모임 만들기" 길 — 크루 명함형의 만들기(1 / 3 · 크루 소개)를 연다
            composable(Routes.CREW_CREATE) { entry ->
                com.stepup.android.ui.screens.community.crew.CrewDraftRoute(navController, Routes.CREW_CREATE, entry)
            }
            // 크루 명함형(확정 2번) — 상세 · 가입 신청 · 멤버 · 주간 목표 · 만들기 · 관리
            crewGraph(
                navController,
                communityRoute = Screen.Community.route,
                boardRoute = { crewId -> Routes.crewBoard(crewId) },
                lobbyRoute = { crewId -> Routes.lobby(crewId) },
            )
            // 내 크루 홈(확정 4번, 2026-09-29) — 가입한 크루의 홈 · 소개 · 레벨 · 크루원 · 모임 · 참석 · 주간 기록
            crewHomeGraph(
                navController,
                communityRoute = Screen.Community.route,
                boardRoute = { crewId -> Routes.crewBoard(crewId) },
                lobbyRoute = { crewId -> Routes.lobby(crewId) },
            )
            // 크루 채팅(2026-09-29) — 크루마다 크루원 전용 방 하나. 크루원 목록의 "나"는 내 정보 탭
            chatGraph(
                navController,
                communityRoute = Screen.Community.route,
                onMyProfile = { navController.switchTab(Screen.Profile) },
            )
            composable(
                route = Routes.CREW_BOARD,
                arguments = listOf(navArgument("crewId") { type = NavType.StringType }),
            ) { entry ->
                CrewBoardScreen(
                    crewId = entry.arguments?.getString("crewId").orEmpty(),
                    onBack = { navController.popBackStack() },
                    onOpenLobby = { crewId -> navController.navigate(Routes.lobby(crewId)) },
                    onWritePost = { crewId -> navController.navigate(Routes.postCompose(crewId)) },
                    onOpenFlash = { postId -> navController.navigate(Routes.flashDetail(postId)) },
                )
            }
            composable(
                route = Routes.FLASH_DETAIL,
                arguments = listOf(navArgument("postId") { type = NavType.LongType }),
            ) { entry ->
                val postId = entry.arguments?.getLong("postId") ?: 0L
                FlashRunDetailScreen(
                    postId = postId,
                    onBack = { navController.popBackStack() },
                    onOpenLobby = { navController.navigate(Routes.flashLobby(postId)) },
                )
            }
            composable(
                route = Routes.FLASH_LOBBY,
                arguments = listOf(navArgument("postId") { type = NavType.LongType }),
            ) { entry ->
                PartyLobbyScreen(
                    flashPostId = entry.arguments?.getLong("postId") ?: 0L,
                    onBack = { navController.popBackStack() },
                    onRunStarted = {
                        // 모임 러닝은 챌린지 상세에서 시작한 러닝이 아니다
                        com.stepup.android.ui.screens.events.ChallengeRunFocus.clear()
                        com.stepup.android.domain.RunPlans.clear()
                        navController.navigate(Routes.RUN)
                    },
                )
            }
            composable(
                route = Routes.POST_COMPOSE,
                arguments = listOf(navArgument("crewId") { type = NavType.StringType }),
            ) { entry ->
                val raw = entry.arguments?.getString("crewId").orEmpty()
                val crewId = if (raw == Routes.NO_CREW) "" else raw
                PostComposeScreen(
                    crewId = crewId,
                    crewName = ServiceLocator.crewRepository.crewOf(crewId)?.name.orEmpty(),
                    onBack = { navController.popBackStack() },
                )
            }
        }
    }

    // 알림에서 눌러 들어온 댓글 창. 어느 탭에 있든 여기서 연다.
    FocusedCommentSheetHost()

    // 첫 사용 안내(시안 02) — 러닝 홈에서만, 먼저 처리할 목적지(초대 링크 · 로그인 뒤 다시 열 초대)나 멈춘 러닝이 없을 때.
    // 목적지가 있으면 그 화면을 먼저 보이고 안내는 다음에 러닝 홈에 올 때로 미룬다. 사람이 고른 뒤에만 "봤음"을 적는다.
    val destinationPending = pendingCrew != null || pendingChat != null || reopenInvite?.let { it.requestedAt < scaffoldOpenedAt } == true
    if (com.stepup.android.domain.FirstGuideRules.shouldShow(
            pending = startTour && !firstGuideClosed,
            onHome = currentRoute == Screen.Run.route,
            destinationPending = destinationPending,
            recoveryPending = pendingRun != null && currentOwner != null,
        )
    ) {
        com.stepup.android.ui.screens.onboarding.FirstGuideSheet(onAction = { action ->
            if (!firstGuideClosed) {
                firstGuideClosed = true
                if (com.stepup.android.domain.FirstGuideRules.savesSeen(action)) {
                    scope.launch { ServiceLocator.userPrefs.setGuideSeen() }
                }
                // 러닝 시작 — 러닝 방법 고르기(12)로. 운동 기록은 아직 시작하지 않는다
                if (com.stepup.android.domain.FirstGuideRules.opensRunMenu(action)) {
                    navController.navigate(Routes.RUN_MENU) { launchSingleTop = true }
                }
            }
        })
    }
    }
}

/** 탭 전환 — 백스택을 쌓지 않고 각 탭의 상태를 보존한다. */
private fun NavHostController.switchTab(screen: Screen) {
    navigate(screen.route) {
        popUpTo(graph.findStartDestination().id) { saveState = true }
        launchSingleTop = true
        restoreState = true
    }
}

/** 하단 탭 줄의 testTag — 화면 검사가 화면 안의 같은 이름 탭과 구분하는 데 쓴다. */
const val BOTTOM_NAV_TAG = "bottom-nav"

/** 뽑기 결과에서 신발 탭으로 넘기는 받은 신발 번호(신발 탭 항목의 SavedStateHandle) */
private const val RECEIVED_SHOE_KEY = "draw_received_shoe"

@Composable
private fun VoltNavBar(navController: NavHostController, currentRoute: String?) {
    androidx.compose.foundation.layout.BoxWithConstraints(Modifier.fillMaxWidth()) {
    val density = androidx.compose.ui.platform.LocalDensity.current
    val measurer = androidx.compose.ui.text.rememberTextMeasurer()
    val labelStyle = com.stepup.android.ui.theme.StepUpTypography.bodySmall.copy(
        fontSize = StepUpDesign.NavigationLabel, fontWeight = FontWeight.SemiBold,
        letterSpacing = 0.sp, textAlign = TextAlign.Center,
    )
    val labels = bottomTabs.map { stringResource(it.labelRes) }
    // Allocate spare space to long localized labels without defeating system font scaling.
    val preferredWidths = labels.map { label ->
        val measured = measurer.measure(text = label, style = labelStyle, softWrap = false)
        (with(density) { measured.size.width.toDp().value } + 10f)
            .coerceAtLeast(StepUpDesign.TouchTarget.value)
    }
    val equalWidth = maxWidth.value / labels.size
    val minimumWidth = StepUpDesign.TouchTarget.value
    val widths = if (preferredWidths.all { it <= equalWidth }) labels.map { equalWidth } else {
        val spare = maxWidth.value - preferredWidths.sum()
        if (spare >= 0f) preferredWidths.map { it + spare / labels.size } else {
            val needs = preferredWidths.map { (it - minimumWidth).coerceAtLeast(0f) }
            val available = (maxWidth.value - minimumWidth * labels.size).coerceAtLeast(0f)
            val totalNeed = needs.sum().coerceAtLeast(1f)
            needs.map { minimumWidth + available * it / totalNeed }
        }
    }
    val labelHeightPx = bottomTabs.mapIndexed { index, screen ->
        val labelWidth = with(density) { (widths[index].dp - 8.dp).roundToPx().coerceAtLeast(1) }
        measurer.measure(
            text = stringResource(screen.labelRes), style = labelStyle,
            constraints = androidx.compose.ui.unit.Constraints(maxWidth = labelWidth),
        ).size.height
    }.maxOrNull() ?: 0
    val labelHeight = with(density) { labelHeightPx.toDp() }
    Column(
        Modifier
            .fillMaxWidth()
            // S2 — 탭 줄은 바닥에 녹아든다. 카드 면을 두르지 않는다.
            .background(Brush.verticalGradient(listOf(Night.copy(alpha = 0.92f), Night))),
    ) {
        if (!com.stepup.android.ui.theme.StepUpColors.dark) HairlineDivider()
        Row(
            modifier = Modifier
                .fillMaxWidth()
                // 프로필 화면 안에도 "프로필" 탭이 있다. 검사가 하단 탭만 집도록 이름을 단다.
                .testTag(BOTTOM_NAV_TAG)
                .navigationBarsPadding()
                // 탭 줄은 64dp — 그 아래로 시스템 안전 영역만큼 더 내려간다
                .heightIn(min = StepUpDesign.NavigationHeight)
                .padding(vertical = 4.dp),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            val parent = parentTabOf(currentRoute)
            bottomTabs.forEachIndexed { index, screen ->
                NavTab(
                    screen = screen,
                    slotWeight = widths[index],
                    labelHeight = labelHeight,
                    labelStyle = labelStyle,
                    selected = parent == screen,
                    onClick = {
                        if (parent == screen) {
                            // 같은 탭의 하위 화면에 있으면 그 탭의 첫 화면으로 돌아간다.
                            // 첫 화면이 백스택에 없으면(다른 길로 들어왔으면) 탭 전환으로 간다.
                            if (currentRoute != screen.route &&
                                !navController.popBackStack(screen.route, inclusive = false)
                            ) {
                                navController.switchTab(screen)
                            }
                        } else {
                            navController.switchTab(screen)
                        }
                    },
                )
            }
        }
    }
    }
}

@Composable
private fun RowScope.NavTab(
    screen: Screen, labelHeight: androidx.compose.ui.unit.Dp,
    slotWeight: Float,
    labelStyle: androidx.compose.ui.text.TextStyle,
    selected: Boolean, onClick: () -> Unit,
) {
    val tint by animateColorAsState(
        // S2 — 선택한 탭은 밝은 글자, 아래 짧은 파란 선이 자리를 알린다
        targetValue = if (selected) com.stepup.android.ui.theme.Snow else Slate,
        label = "navTabTint",
    )
    val dotAlpha by animateFloatAsState(
        targetValue = if (selected) 1f else 0f,
        label = "navTabDot",
    )
    Column(
        modifier = Modifier.weight(slotWeight)
            // 기능을 설명하기 전에 "그게 이 버튼 안에 있다"부터 보여준다.
            .guideTarget(GuideTour.Targets.tab(screen.route))
            .semantics { this.selected = selected }
            .feedbackClickable(cue = FeedbackCue.Select, role = Role.Tab) { onClick() }
            .heightIn(min = StepUpDesign.NavigationItemHeight)
            .padding(horizontal = 4.dp, vertical = 2.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        Icon(
            imageVector = screen.icon,
            contentDescription = null,
            tint = tint,
            modifier = Modifier.size(StepUpDesign.NavigationIcon).testTag("nav-icon-${screen.route}"),
        )
        Text(
            text = stringResource(screen.labelRes),
            modifier = Modifier.fillMaxWidth().heightIn(min = labelHeight).testTag("nav-label-${screen.route}"),
            color = tint,
            style = labelStyle,
            softWrap = true,
        )
        Box(
            modifier = Modifier
                .size(width = 17.dp, height = 3.dp)
                .alpha(dotAlpha)
                .background(Volt, androidx.compose.foundation.shape.RoundedCornerShape(2.dp)),
        )
    }
}

/** 앱이 죽어 멈춘 러닝 — 이어 달리거나 여기서 끝내고 저장한다. 버리는 선택은 두지 않는다(뛴 기록을 잃지 않게). */
@Composable
private fun RunRecoveryDialog(
    checkpoint: com.stepup.android.service.RunCheckpoint,
    /** 다른 계정의 러닝이면 자세한 기록을 보이지 않고, 이어 달리기 없이 원래 계정 이름으로 저장만 한다 */
    sameAccount: Boolean,
    onResume: () -> Unit,
    onFinish: () -> Unit,
) {
    val state = checkpoint.state
    val started = java.time.format.DateTimeFormatter.ofPattern("HH:mm")
        .format(java.time.Instant.ofEpochMilli(state.startedAt).atZone(java.time.ZoneId.systemDefault()))
    val minutes = state.elapsedSec / 60
    com.stepup.android.ui.components.DialogPanel(
        title = androidx.compose.ui.res.stringResource(R.string.run_recover_title),
        onDismiss = {},
        actions = {
            if (sameAccount) {
                com.stepup.android.ui.components.VoltButton(
                    androidx.compose.ui.res.stringResource(R.string.run_recover_resume), onResume,
                    Modifier.fillMaxWidth().testTag("run-recover-resume"),
                )
                com.stepup.android.ui.components.GhostButton(
                    androidx.compose.ui.res.stringResource(R.string.run_recover_finish), onFinish,
                    Modifier.fillMaxWidth().testTag("run-recover-finish"),
                )
            } else {
                com.stepup.android.ui.components.VoltButton(
                    androidx.compose.ui.res.stringResource(R.string.run_recover_finish), onFinish,
                    Modifier.fillMaxWidth().testTag("run-recover-finish"),
                )
            }
        },
    ) {
        androidx.compose.material3.Text(
            if (sameAccount) {
                androidx.compose.ui.res.stringResource(R.string.run_recover_body, started, "%,d".format(state.steps), minutes)
            } else {
                androidx.compose.ui.res.stringResource(R.string.run_recover_other_account)
            },
            style = androidx.compose.material3.MaterialTheme.typography.bodyLarge,
            color = com.stepup.android.ui.theme.Silver,
        )
    }
}

/**
 * 웹 지갑 페이지(인증 · 서명 · 연결) — 상급 뽑기의 지갑 연결과 연결된 계정의 WEB3 지갑이 함께 쓴다.
 * 로그인이 필요하면 알리고, 주소를 만들지 못하면(연결) 지갑 화면으로 간다.
 */
private fun kotlinx.coroutines.CoroutineScope.openWalletPage(context: android.content.Context, onOffline: () -> Unit) {
    launch {
        when (val link = com.stepup.android.ui.screens.rewards.openWalletPageLink()) {
            is com.stepup.android.ui.screens.rewards.WalletPageLink.Open ->
                com.stepup.android.core.ExternalIntents.openUrl(context, link.url)
            com.stepup.android.ui.screens.rewards.WalletPageLink.SignIn -> android.widget.Toast.makeText(
                context, context.getString(R.string.wallet_web_sign_in), android.widget.Toast.LENGTH_SHORT).show()
            com.stepup.android.ui.screens.rewards.WalletPageLink.Offline -> onOffline()
        }
    }
}
