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
import androidx.compose.material.icons.automirrored.filled.DirectionsWalk
import androidx.compose.material.icons.filled.Groups
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.Icon
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
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
import com.stepup.android.core.InviteLinks
import com.stepup.android.core.ServiceLocator
import com.stepup.android.ui.components.HairlineDivider
import com.stepup.android.ui.components.NightCanvas
import com.stepup.android.ui.components.quietClickable
import com.stepup.android.ui.guide.GuideOverlay
import com.stepup.android.ui.guide.GuideTour
import com.stepup.android.ui.guide.guideTarget
import com.stepup.android.ui.screens.community.CommunityScreen
import com.stepup.android.ui.screens.community.CrewBoardScreen
import com.stepup.android.ui.screens.community.FlashRunDetailScreen
import com.stepup.android.ui.screens.community.FocusedCommentSheetHost
import com.stepup.android.ui.screens.community.CrewCreateScreen
import com.stepup.android.ui.screens.community.PartyLobbyScreen
import com.stepup.android.ui.screens.community.PostComposeScreen
import com.stepup.android.ui.screens.community.RankingScreen
import com.stepup.android.ui.screens.events.EventsScreen
import com.stepup.android.ui.screens.events.NewsScreen
import com.stepup.android.ui.screens.home.HomeScreen
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

    /** 꾸미기 — 캐릭터에 의상과 신발을 입히는 곳. 러너 마켓은 이 안에 있다. */
    data object Customize : Screen("customize", R.string.tab_customize, StepUpIcons.Shirt)

    data object Community : Screen("community", R.string.tab_community, Icons.Filled.Groups)

    /** 내 정보 — 프로필 · 기록 · 포인트 · 설정 */
    data object Profile : Screen("profile", R.string.tab_me, Icons.Filled.Person)
}

/**
 * 하단 탭은 넷이다 — 러닝 / 꾸미기 / 커뮤니티 / 내 정보.
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

    /** 꾸미기 안의 러너 마켓 — 의상 / 신발 */
    const val RUNNER_MARKET = "runner-market"

    const val RUN = "run"

    /** 러닝 화면 — start=true 면 들어오자마자 달리기를 시작한다 */
    const val RUN_ROUTE = "run?start={start}"
    const val RUN_NOW = "run?start=true"
    const val WALLET = "wallet"
    const val NOTIFICATIONS = "notifications"
    const val ACHIEVEMENTS = "achievements"
    const val ANALYTICS = "analytics"
    const val SETTINGS_NOTIFICATIONS = "settings/notifications"
    const val SETTINGS_PRIVACY = "settings/privacy"
    const val SETTINGS_SUPPORT = "settings/support"
    const val SETTINGS_CONNECTED = "settings/connected"
    const val SETTINGS_LANGUAGE = "settings/language"
    const val SETTINGS_EXPERIENCE = "settings/experience"
    const val SETTINGS_THEME = "settings/theme"
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
    fun crewBoard(crewId: String) = "crew/board/$crewId"

    /** crewId가 비어 있으면 전체 게시판에 쓰는 글 */
    fun postCompose(crewId: String) = "post/compose/${crewId.ifBlank { NO_CREW }}"

    fun flashDetail(postId: Long) = "flash/$postId"

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

    // 로그인 표시와 실제 세션이 어긋나면 로그인 화면을 다시 띄운다.
    //
    // 표시만 보고 통과시키면, 세션을 잃은 사람이 로그인돼 있다고 믿으면서
    // 아무것도 서버에 안 올라가는 상태로 계속 뛰게 된다. 조용히 기록을
    // 잃는 것보다 한 번 더 로그인하는 편이 낫다.
    var sessionChecked by rememberSaveable { mutableStateOf(false) }
    LaunchedEffect(loginMethod) {
        if (loginMethod?.isNotEmpty() == true && !ServiceLocator.sessionHolder.isSignedIn()) {
            ServiceLocator.userPrefs.setLoginMethod("")
        }
        sessionChecked = true
    }

    Box(Modifier.fillMaxSize()) {
        NightCanvas(Modifier.fillMaxSize())

        val stage = when {
            !ready || loginMethod == null || guideSeen == null || !sessionChecked -> 0
            loginMethod!!.isEmpty() -> 1
            else -> 2
        }
        Crossfade(stage, animationSpec = tween(LocalMotion.current.duration(220)), label = "entryStage") { visible ->
        when (visible) {
            0 ->
                SplashScreen(onReady = { ready = true })

            1 -> LoginScreen(onDone = {})

            else -> MainScaffold(startTour = guideSeen == false)
        }
        }
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

    // 첫 실행이면 화면이 자리를 잡은 뒤 스포트라이트 투어를 시작한다
    LaunchedEffect(startTour) {
        if (startTour) {
            kotlinx.coroutines.delay(450)
            GuideTour.start()
        }
    }
    // Browsing the app never prompts for run permissions. Start passive tracking only when already allowed.
    LaunchedEffect(Unit) {
        if (StepPermissions.hasActivityRecognition(context)) {
            ServiceLocator.stepRepository.startTracking()
        }
    }

    val navController = rememberNavController()

    // 초대 링크(stepupcrew.com/c/...)나 크루 알림으로 들어왔으면 그 크루 화면을 연다
    val pendingCrew by InviteLinks.pendingCrew.collectAsState()
    LaunchedEffect(pendingCrew) {
        val crewId = pendingCrew ?: return@LaunchedEffect
        navController.navigate(Routes.crewBoard(crewId))
        InviteLinks.consume()
    }

    val backStack by navController.currentBackStackEntryAsState()
    val currentRoute = backStack?.destination?.route
    val chrome = AppChromePolicy.destination(currentRoute)
    val showBar = chrome?.showBottomBar == true
    val balanceFlow = remember { ServiceLocator.rewardRepository.balance.map<Double, Double?> { it } }
    val balance by balanceFlow.collectAsState(initial = null)

    Box(Modifier.fillMaxSize()) {
    if (currentRoute == Screen.Run.route || currentRoute == Routes.EVENTS) {
        com.stepup.android.ui.components.RunnerScene(Modifier.fillMaxSize())
    } else if (currentRoute == Screen.Customize.route) {
        com.stepup.android.ui.components.RunnerScene(
            Modifier.fillMaxSize(), com.stepup.android.ui.components.RunnerSetting.Wardrobe,
        )
    } else if (chrome?.header == AppChromePolicy.Header.Focus) {
        com.stepup.android.ui.components.RunnerScene(
            Modifier.fillMaxSize(), com.stepup.android.ui.components.RunnerSetting.Sunset,
        )
    }
    Scaffold(
        containerColor = Color.Transparent,
        topBar = {
            if (chrome?.header == AppChromePolicy.Header.Main) {
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
                    onStartRun = { navController.navigate(Routes.RUN_NOW) },
                    onOpenWallet = { navController.navigate(Routes.WALLET) },
                    onOpenChallenges = { navController.navigate(Routes.EVENTS) },
                    onOpenNews = { navController.navigate(Routes.NEWS) },
                    onOpenCustomize = { navController.switchTab(Screen.Customize) },
                )
            }
            composable(Screen.Customize.route) {
                CustomizeScreen(
                    onOpenWallet = { navController.navigate(Routes.WALLET) },
                    onOpenMarket = { navController.navigate(Routes.RUNNER_MARKET) },
                    onOpenVault = { navController.navigate(Routes.ITEMS) },
                    onOpenSneaker = { id -> navController.navigate(Routes.sneaker(id)) },
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
                    onCreateCrew = { navController.navigate(Routes.CREW_CREATE) },
                    onWritePost = { crewId -> navController.navigate(Routes.postCompose(crewId)) },
                    onOpenFlash = { postId -> navController.navigate(Routes.flashDetail(postId)) },
                    onOpenMap = { navController.navigate(Routes.MAP) },
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
                    onStartRun = { navController.navigate(Routes.RUN_NOW) },
                )
            }
            composable(Screen.Profile.route) {
                ProfileScreen(
                    onOpenCustomize = { navController.switchTab(Screen.Customize) },
                    onOpenChallenges = { navController.navigate(Routes.EVENTS) },
                    onOpenGuide = {
                        navController.switchTab(Screen.Run)
                        GuideTour.start()
                    },
                    onOpenWallet = { navController.navigate(Routes.WALLET) },
                    onOpenAchievements = { navController.navigate(Routes.ACHIEVEMENTS) },
                    onOpenAnalytics = { navController.navigate(Routes.ANALYTICS) },
                    onOpenNotificationSettings = { navController.navigate(Routes.SETTINGS_NOTIFICATIONS) },
                    onOpenPrivacy = { navController.navigate(Routes.SETTINGS_PRIVACY) },
                    onOpenSupport = { navController.navigate(Routes.SETTINGS_SUPPORT) },
                    onOpenConnected = { navController.navigate(Routes.SETTINGS_CONNECTED) },
                    onOpenLanguage = { navController.navigate(Routes.SETTINGS_LANGUAGE) },
                    onOpenExperience = { navController.navigate(Routes.SETTINGS_EXPERIENCE) },
                    onOpenTheme = { navController.navigate(Routes.SETTINGS_THEME) },
                    // 내 아이템 — 신발 보관함(강화 · 판매 · 조합 · 도감)
                    onOpenItems = { navController.navigate(Routes.ITEMS) },
                    onOpenNotifications = { navController.navigate(Routes.NOTIFICATIONS) },
                    onOpenRanking = { navController.navigate(Routes.RANKING) },
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
                    autoStart = entry.arguments?.getBoolean("start") ?: false,
                )
            }
            composable(Routes.COURSES) {
                CourseHubScreen(onBack = { navController.popBackStack() })
            }
            composable(Routes.WALLET) { WalletScreen(onBack = { navController.popBackStack() }) }
            composable(Routes.NOTIFICATIONS) {
                NotificationsScreen(
                    onOpenChallenges = { navController.navigate(Routes.EVENTS) },
                    onBack = { navController.popBackStack() },
                    onOpenLobby = { crewId -> navController.navigate(Routes.lobby(crewId)) },
                    onOpenCrew = { crewId -> navController.navigate(Routes.crewBoard(crewId)) },
                    // 댓글은 게시판 위에 창으로 뜬다. 어느 댓글인지는 저장소에
                    // 남겨 두고 커뮤니티 탭으로 보내면, 게시판이 그 창을 연다.
                    onOpenComment = { target ->
                        ServiceLocator.communityRepository.focusComment(target)
                        navController.switchTab(Screen.Community)
                    },
                )
            }
            composable(Routes.ACHIEVEMENTS) {
                AchievementsScreen(onBack = { navController.popBackStack() })
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
                SupportScreen(onBack = { navController.popBackStack() })
            }
            composable(Routes.SETTINGS_CONNECTED) {
                ConnectedAccountsScreen(onBack = { navController.popBackStack() })
            }
            composable(Routes.SETTINGS_EXPERIENCE) {
                com.stepup.android.ui.screens.settings.ExperienceSettingsScreen { navController.popBackStack() }
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
                )
            }
            composable(
                route = Routes.LOBBY,
                arguments = listOf(navArgument("crewId") { type = NavType.StringType }),
            ) { entry ->
                PartyLobbyScreen(
                    crewId = entry.arguments?.getString("crewId").orEmpty(),
                    onBack = { navController.popBackStack() },
                    onRunStarted = { navController.navigate(Routes.RUN) },
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
            composable(Routes.CREW_CREATE) {
                CrewCreateScreen(
                    onBack = { navController.popBackStack() },
                    onCreated = { crewId ->
                        navController.popBackStack()
                        navController.navigate(Routes.crewBoard(crewId))
                    },
                )
            }
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
                    onRunStarted = { navController.navigate(Routes.RUN) },
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

    // 스포트라이트 가이드 오버레이 — 하단 바까지 덮는다
    if (GuideTour.active) {
        GuideOverlay(
            onSwitchTab = { route ->
                bottomTabs.firstOrNull { it.route == route }?.let { navController.switchTab(it) }
            },
            onFinished = {
                scope.launch { ServiceLocator.userPrefs.setGuideSeen() }
                navController.switchTab(Screen.Run)
            },
        )
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

@Composable
private fun VoltNavBar(navController: NavHostController, currentRoute: String?) {
    androidx.compose.foundation.layout.BoxWithConstraints(Modifier.fillMaxWidth()) {
    val density = androidx.compose.ui.platform.LocalDensity.current
    val measurer = androidx.compose.ui.text.rememberTextMeasurer()
    val labelStyle = com.stepup.android.ui.theme.StepUpTypography.bodySmall.copy(
        fontSize = StepUpDesign.NavigationLabel, fontWeight = FontWeight.SemiBold,
        letterSpacing = 0.sp, textAlign = TextAlign.Center,
    )
    val labelWidth = with(density) { (maxWidth / bottomTabs.size - 8.dp).roundToPx().coerceAtLeast(1) }
    val labelHeightPx = bottomTabs.map { screen ->
        measurer.measure(
            text = stringResource(screen.labelRes), style = labelStyle,
            constraints = androidx.compose.ui.unit.Constraints(maxWidth = labelWidth),
        ).size.height
    }.maxOrNull() ?: 0
    val labelHeight = with(density) { labelHeightPx.toDp() }
    Column(
        Modifier
            .fillMaxWidth()
            .background(Brush.verticalGradient(listOf(Carbon.copy(alpha = 0.97f), Night))),
    ) {
        HairlineDivider()
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
            bottomTabs.forEach { screen ->
                NavTab(
                    screen = screen,
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
    labelStyle: androidx.compose.ui.text.TextStyle,
    selected: Boolean, onClick: () -> Unit,
) {
    val tint by animateColorAsState(
        // 선택한 탭은 밝은 파랑 — 버튼 바탕색(Volt)은 검은 바닥 위 작은 글자에 어둡다
        targetValue = if (selected) VoltText else Slate,
        label = "navTabTint",
    )
    val dotAlpha by animateFloatAsState(
        targetValue = if (selected) 1f else 0f,
        label = "navTabDot",
    )
    Column(
        modifier = Modifier.weight(1f)
            // 기능을 설명하기 전에 "그게 이 버튼 안에 있다"부터 보여준다.
            .guideTarget(GuideTour.Targets.tab(screen.route))
            .semantics { this.selected = selected }
            .feedbackClickable(cue = FeedbackCue.Select, role = Role.Tab) { onClick() }
            // 탭이 넷이라 한 칸이 넉넉하다. 누르는 자리는 최소 48dp 로 잡는다.
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
                .size(StepUpDesign.NavigationIndicator)
                .alpha(dotAlpha)
                .background(Volt, CircleShape),
        )
    }
}
