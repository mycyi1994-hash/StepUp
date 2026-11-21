package com.stepup.android.core
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.drop

import android.content.Context
import androidx.room.Room
import com.stepup.android.BuildConfig
import com.stepup.android.data.local.AppDatabase
import com.stepup.android.data.prefs.UserPrefs
import com.stepup.android.data.remote.CommunityApi
import com.stepup.android.data.remote.CourseApi
import com.stepup.android.data.remote.CrewApi
import com.stepup.android.data.remote.EventApi
import com.stepup.android.data.remote.GoogleSignIn
import com.stepup.android.data.remote.MarketApi
import com.stepup.android.data.remote.ServerResult
import kotlinx.coroutines.launch
import com.stepup.android.data.remote.PartyApi
import com.stepup.android.data.remote.PushApi
import com.stepup.android.data.remote.RunningFeedApi
import com.stepup.android.data.remote.SessionHolder
import com.stepup.android.data.remote.StepUpServer
import com.stepup.android.data.remote.SupabaseAuth
import com.stepup.android.data.remote.TerritoryApi
import com.stepup.android.data.repo.AvatarRepository
import com.stepup.android.data.repo.BoostRepository
import com.stepup.android.data.repo.ClaimRepository
import com.stepup.android.data.repo.PrefsAuthSessionStore
import com.stepup.android.data.repo.RankingRepository
import com.stepup.android.data.repo.ServerSessionRecorder
import com.stepup.android.data.repo.CommunityRepository
import com.stepup.android.data.repo.CourseRepository
import com.stepup.android.data.repo.CrewRepository
import com.stepup.android.data.repo.EventRepository
import com.stepup.android.data.repo.MarketRepository
import com.stepup.android.data.repo.NewsRepository
import com.stepup.android.data.repo.RunningFeedRepository
import com.stepup.android.data.repo.NotificationRepository
import com.stepup.android.data.repo.RewardRepository
import com.stepup.android.data.repo.SneakerRepository
import com.stepup.android.data.repo.StepRepository
import com.stepup.android.sensor.StepTracker
import com.stepup.android.push.PushRegistrar
import com.stepup.android.service.WalkSessionService

/** 간단한 수동 DI 컨테이너. Application.onCreate에서 [init]을 호출한다. */
object ServiceLocator {
    /** 목표 보너스를 미룰지 볼 때 읽는 이 계정의 올리기 대기 러닝 수 */
    private const val GOAL_PENDING_SCAN = 200

    lateinit var appContext: Context
        private set
    lateinit var database: AppDatabase
        private set
    lateinit var userPrefs: UserPrefs
        private set
    lateinit var stepTracker: StepTracker
        private set
    lateinit var rewardRepository: RewardRepository
        private set
    lateinit var runSettlementRepository: com.stepup.android.data.repo.RunSettlementRepository
        private set
    lateinit var stepRepository: StepRepository

    /** 하루 걸음 올리기(체인 하루 걸음 기록, 2026-11-18) */
    lateinit var dailyStepsUploader: com.stepup.android.data.repo.DailyStepsUploader

    /** 경제 4판(서버 0116) 규칙 · 상자 현황 — 신발 · 수리 · 강화 · 출발 전 점검 · 뽑기가 함께 본다 */
    lateinit var shoeRules: com.stepup.android.data.repo.ShoeRulesRepository
        private set

    /** 러너 레벨(서버 0121) — 내 정보 카드 · 레벨이 올랐다는 알림 · 신발 화면의 하루 에너지 글 · 크루 명단 배지 */
    lateinit var runnerLevel: com.stepup.android.data.repo.RunnerLevelRepository
        private set
    lateinit var sneakerRepository: SneakerRepository
        private set

    /** 신발 강화(0054) — 재료 신발 3개 · 확률. 서버 경제가 없으면 강화하지 않는다 */
    lateinit var forgeRepository: com.stepup.android.data.repo.ForgeRepository
        private set
    lateinit var avatarRepository: AvatarRepository
        private set
    lateinit var boostRepository: BoostRepository
        private set
    lateinit var crewRepository: CrewRepository
        private set

    /** 크루 명함형 — 목록 · 상세 · 가입 신청 · 크루장 관리 · 만들기(2026-09-28) */
    lateinit var crewCards: com.stepup.android.data.repo.CrewCardRepository
        private set

    /** 기기 검사용 — 크루 화면을 흉내 낸 서버로 돌린다(CrewCardsTest). 앱 코드는 부르지 않는다. */
    @androidx.annotation.VisibleForTesting
    fun useCrewCardsForTest(repository: com.stepup.android.data.repo.CrewCardRepository) {
        crewCards = repository
    }

    /** 크루 채팅 — 크루마다 크루원 전용 방 하나(2026-09-29) */
    lateinit var crewChat: com.stepup.android.data.repo.CrewChatRepository
        private set

    /** 기기 검사용 — 채팅 화면을 흉내 낸 서버로 돌린다(CrewChatDesignTest). 앱 코드는 부르지 않는다. */
    @androidx.annotation.VisibleForTesting
    fun useCrewChatForTest(repository: com.stepup.android.data.repo.CrewChatRepository) {
        crewChat = repository
    }

    /** 내 크루 홈(확정 4번) — 홈 · 모임 · 참석 · 주간 기록 · 러닝 기록(2026-09-29) */
    lateinit var crewHome: com.stepup.android.data.repo.CrewHomeRepository
        private set

    /** 기기 검사용 — 크루 홈 화면을 흉내 낸 서버로 돌린다(CrewHomeDesignTest). 앱 코드는 부르지 않는다. */
    @androidx.annotation.VisibleForTesting
    fun useCrewHomeForTest(repository: com.stepup.android.data.repo.CrewHomeRepository) {
        crewHome = repository
    }

    /** 크루 러닝 3판(서버 0070) — 오늘 크루 러닝 · 방의 크루 체력 · 결과 */
    lateinit var crewRun: com.stepup.android.data.repo.CrewRunRepository
        private set

    /** 기기 검사용 — 크루 러닝을 흉내 낸 서버로 돌린다. 앱 코드는 부르지 않는다. */
    @androidx.annotation.VisibleForTesting
    fun useCrewRunForTest(repository: com.stepup.android.data.repo.CrewRunRepository) {
        crewRun = repository
    }
    /** 동네 이야기의 장소 찾기(MapTiler · 기기 지오코더) */
    lateinit var placeSearch: com.stepup.android.data.repo.PlaceSearch
        private set

    lateinit var communityRepository: CommunityRepository
        private set

    /**
     * 기기 검사용 — 동네 이야기 화면을 흉내 낸 서버 · 고정 장소 검색으로 돌린다(CommunityStoriesTest).
     * 앱 코드는 부르지 않는다.
     */
    @androidx.annotation.VisibleForTesting
    fun useCommunityForTest(repository: CommunityRepository, places: com.stepup.android.data.repo.PlaceSearch) {
        communityRepository = repository
        placeSearch = places
    }

    private var drawSourceForTest: com.stepup.android.ui.screens.gacha.DrawSource? = null

    /** 신발 뽑기가 기대는 것 — 서버 현황(draw_status)과 한 번 뽑기 */
    fun drawSource(): com.stepup.android.ui.screens.gacha.DrawSource =
        drawSourceForTest ?: com.stepup.android.ui.screens.gacha.ServerDrawSource()

    /** 기기 테스트가 뽑기 서버를 흉내 낸 것으로 바꾼다. null 이면 되돌린다. 앱 코드는 부르지 않는다. */
    @androidx.annotation.VisibleForTesting
    fun useDrawForTest(source: com.stepup.android.ui.screens.gacha.DrawSource?) {
        drawSourceForTest = source
    }
    lateinit var courseRepository: CourseRepository
        private set
    lateinit var eventRepository: EventRepository
        private set
    lateinit var notificationRepository: NotificationRepository
        private set

    /** NFT 마켓 — 소유권과 값이 서버에서 정해지는 유일한 곳 */
    lateinit var marketRepository: MarketRepository
        private set

    /** 러닝 소식 — 하루 한 번 받아 두고 그 사본을 보여 준다 */
    lateinit var newsRepository: NewsRepository
        private set

    /**
     * 바깥 대회와 언론사 기사.
     *
     * 기존 eventRepository(챌린지·미션·SUP 보상)와 **다른 것**이다. 이쪽은
     * 보상을 주지 않는다 — 바깥으로 보내 줄 뿐이다.
     */
    lateinit var runningFeedRepository: RunningFeedRepository
        private set

    /**
     * 앱 공지(알림 › 공지) — 운영자가 서버 표에 올린 StepUp 안내. 위의 바깥 소식과 **다른 것**이다.
     * 받은 목록은 사본으로 두고, 새로 받지 못하면 사본을 보인다.
     */
    lateinit var announcementRepository: com.stepup.android.data.repo.AnnouncementRepository
        private set

    /** 내 러닝 기록 — 이 휴대폰에 저장된 지금 계정의 러닝(목록 · 기간 합계 · 통계 · 하나 · 지우기) */
    val runRecordsRepository: com.stepup.android.data.repo.RunRecordsRepository by lazy {
        com.stepup.android.data.repo.RunRecordsRepository(
            database.walkSessionDao(), recordingOwnerFlow(),
            // 지운 기록의 AI 코칭 사본도 지운다(서버의 분석은 그대로 — 서버에 러닝 삭제 계약이 없다)
            onDeleted = { record -> coachingRepository.forgetRun(record) },
        )
    }

    /**
     * AI 코칭(2026-10-16, docs/redesign/ai-coaching-2026-10) — 지금 계정의 기록과 서버 코칭(0060). 사본은 백업하지 않는 폴더에.
     * 서버가 켜지기 전(coaching_provider none)에는 AI 칸 없이 기록 숫자 · 기본 훈련만 보인다.
     */
    val coachingRepository: com.stepup.android.data.repo.CoachingRepository by lazy {
        com.stepup.android.data.repo.CoachingRepository(
            dao = database.walkSessionDao(),
            api = com.stepup.android.data.remote.CoachingApi(server),
            cache = com.stepup.android.data.repo.CoachingCacheStore(java.io.File(appContext.noBackupFilesDir, "coaching")),
            owner = recordingOwnerFlow(),
            online = { hasNetwork(appContext) },
        )
    }

    private val coachingScope = kotlinx.coroutines.CoroutineScope(
        kotlinx.coroutines.SupervisorJob() + kotlinx.coroutines.Dispatchers.IO,
    )

    /** 서버가 러닝을 확인한 뒤 — 자동 분석을 켠 사람만(서버가 동의 · 토글을 다시 본다). 올리기 일꾼을 기다리게 하지 않는다 */
    private fun coachAfterUpload() {
        coachingScope.launch {
            runCatching {
                val language = AppLocale.tag.ifEmpty { java.util.Locale.getDefault().language }
                coachingRepository.autoAnalyzeRecent(
                    sessionHolder.recordingOwner(), com.stepup.android.data.repo.coachLocale(language),
                )
            }
        }
    }

    private fun hasNetwork(context: Context): Boolean {
        val manager = context.getSystemService(android.net.ConnectivityManager::class.java) ?: return true
        val capabilities = manager.getNetworkCapabilities(manager.activeNetwork) ?: return false
        return capabilities.hasCapability(android.net.NetworkCapabilities.NET_CAPABILITY_INTERNET)
    }

    /**
     * 프로필 수정(2026-09-28) — 사진 · 기본 이미지는 고른 즉시, 닉네임은 "저장"으로. 저장은 앱 범위에서 끝까지 한다
     * (편집 화면을 나가도 반쯤 저장되지 않게). 단계 표시는 메인 스레드에서 바뀐다.
     */
    val profileEditor: com.stepup.android.data.repo.ProfileEditor by lazy {
        val photos = com.stepup.android.data.repo.AvatarPhotoStore(
            appContext.filesDir, com.stepup.android.data.prefs.UserPrefs.AVATAR_FILE, commit = { userPrefs.useCustomAvatar() },
        )
        com.stepup.android.data.repo.ProfileEditor(
            // 폰에 적고 서버 이름(크루장 · 명단)도 바꾼다 — 서버에 못 닿으면 다음 동기화가 한다
            saveNickname = {
                userPrefs.setNickname(it)
                runCatching { economySync.pushNickname() }
            },
            saveAvatarId = { userPrefs.setAvatarId(it) },
            savePhotoFile = { open -> photos.save(open) },
            scope = kotlinx.coroutines.CoroutineScope(kotlinx.coroutines.SupervisorJob() + kotlinx.coroutines.Dispatchers.Main.immediate),
        )
    }

    lateinit var claimRepository: ClaimRepository
    /** 과속으로 멈춘 러닝의 패널티 신고 — 올리기 일꾼이 러닝보다 먼저 보낸다 */
    lateinit var overspeedReporter: com.stepup.android.data.repo.OverspeedReporter

    /** 아이템(서버 0106) — 가방 · 바로 쓰기 · 출발 때 고른 아이템 */
    lateinit var itemsRepository: com.stepup.android.data.repo.ItemsRepository

    /** 순위표 — 유일하게 남의 기록이 필요한 화면이라 서버가 계산해 준다 */
    lateinit var rankingRepository: RankingRepository
        private set

    /** 리더보드 세 화면(2026-10-17) — 크루 · 개인은 서버 주간 순위(0061), 주간 포인트는 서버 순위가 없어 '준비 중' */
    lateinit var leaderboards: com.stepup.android.data.repo.LeaderboardSource
        private set

    /** 기기 검사용 — 리더보드를 개발용 고정 값으로 돌린다(LeaderboardDesignTest). 앱 코드는 부르지 않는다. */
    @androidx.annotation.VisibleForTesting
    fun useLeaderboardsForTest(source: com.stepup.android.data.repo.LeaderboardSource) {
        leaderboards = source
    }

    /** 지금 누구로 로그인해 있는지. 화면이 로그인 상태를 물을 때 쓴다. */
    lateinit var sessionHolder: SessionHolder

    /** 달리는 중인 러닝의 저장본 — 앱이 죽어도 러닝을 되살린다. 백업하지 않는 폴더에 둔다. */
    lateinit var runCheckpoints: com.stepup.android.service.RunCheckpointStore
        private set

    /** 서버 경제(A안) — 잔고 · 신발 · 에너지의 정본은 서버다 */
    lateinit var economyApi: com.stepup.android.data.remote.EconomyApi
    lateinit var inviteApi: com.stepup.android.data.remote.InviteApi
    lateinit var accessApi: com.stepup.android.data.remote.AccessApi
    lateinit var economySync: com.stepup.android.data.repo.EconomySync

    /** 서버가 잔고를 정하는 빌드인가 — 화면이 폰이 계산한 옛 금액을 가릴지 정할 때 쓴다 */
    val serverEconomyOn: Boolean
        get() = ::economyApi.isInitialized && economyApi.isConfigured

    private val economyScope = kotlinx.coroutines.CoroutineScope(
        kotlinx.coroutines.SupervisorJob() + kotlinx.coroutines.Dispatchers.IO,
    )

    /** 서버 경제를 뒤에서 한 번 맞춘다 — 앱 시작 · 로그인 · 러닝 업로드 뒤 */
    /**
     * 지금 로그인한 계정의 기록 주인(account:… / guest / legacy). 로그인 방법이 바뀔 때(다시 로그인 · 계정
     * 삭제 · 새 로그인) 다시 읽는다 — 화면의 러닝 기록 · 경로 · 누적이 그 계정 것만 보이게.
     */
    fun recordingOwnerFlow(): kotlinx.coroutines.flow.Flow<String> =
        userPrefs.loginMethod
            .map { sessionHolder.recordingOwner() }
            .distinctUntilChanged()

    fun refreshEconomyInBackground() {
        economyScope.launch { runCatching { economySync.refresh() } }
    }

    /** 구글 계정을 받아 오는 쪽. 로그인 화면이 쓴다. */
    lateinit var googleSignIn: GoogleSignIn
        private set

    /** 이 폰의 푸시 주소를 서버에 적는 쪽 */
    lateinit var pushRegistrar: PushRegistrar
        private set

    /** 서버 — 계정 삭제처럼 저장소를 거치지 않는 호출에 쓴다 */
    lateinit var server: StepUpServer
        private set

    /** 땅따먹기 — 지도에 보이는 칸과 크루 순위 */
    lateinit var territoryApi: TerritoryApi
        private set

    fun init(context: Context) {
        if (this::database.isInitialized) return
        val app = context.applicationContext
        appContext = app
        runCheckpoints = com.stepup.android.service.RunCheckpointStore(java.io.File(app.noBackupFilesDir, "run-checkpoint.bin"))
        // 파일 이름은 옛 이름 그대로 둔다. 패키지는 바꿔도 이 이름을 바꾸면
        // 안드로이드가 다른 파일을 찾게 되어, 이미 설치된 기기의 기록이 통째로
        // 사라진다. 눈에 거슬려도 건드리지 않는다.
        database = Room.databaseBuilder(app, AppDatabase::class.java, "strideup.db")
            // 버전 7부터는 실제 마이그레이션을 쓴다. 러닝 기록이 SUP 청구의
            // 근거가 되는 순간부터, 스키마를 고쳤다고 사용자 기록을 지우는 것은
            // 개발 편의가 아니라 데이터 손실이다.
            .addMigrations(*AppDatabase.MIGRATIONS)
            // Unknown schemas must remain intact. Never erase records to make an
            // unsupported upgrade or downgrade appear successful.
            .build()
        userPrefs = UserPrefs(app)
        googleSignIn = GoogleSignIn(BuildConfig.GOOGLE_WEB_CLIENT_ID)
        sessionHolder = SessionHolder(
            auth = SupabaseAuth(BuildConfig.SUPABASE_URL, BuildConfig.SUPABASE_KEY),
            store = PrefsAuthSessionStore(userPrefs),
        )
        server = StepUpServer(BuildConfig.SUPABASE_URL, BuildConfig.SUPABASE_KEY, sessionHolder)
        // 접속 코드 문에 막힌 요청 — 권한을 다시 묻는다(코드를 넣었으면 문이 열리고 로그인 뒤 일을 다시 한다)
        server.onAccessRequired = { AccessGate.onServerBlocked(app) }
        Analytics.init(app)
        pushRegistrar = PushRegistrar(
            PushApi(server),
            locale = { userPrefs.languageNow() },
            preferences = { userPrefs.notifyPrefs.first() },
        )
        territoryApi = TerritoryApi(server)
        economyApi = com.stepup.android.data.remote.EconomyApi(server)
        val dailyRewardsApi = com.stepup.android.data.remote.DailyRewardsApi(server)
        shoeRules = com.stepup.android.data.repo.ShoeRulesRepository(
            fetchRules = { economyApi.shoeRules() },
            fetchBoxes = { economyApi.boxStatus() },
            // 오늘의 보상(0118) — 같은 box_status 글로 상자 현황 · 홈 막대를 읽고 폰에도 남긴다(계정마다)
            fetchBoxesRaw = { dailyRewardsApi.boxStatusRaw() },
            store = userPrefs.dailyRewardsStore,
            owner = { sessionHolder.recordingOwner() },
            // 오늘 목표를 채웠는데 보너스가 없으면(러닝을 기록할 때 서버가 주는 단계가 실패했다) 그날을 한 번 청구한다 — 받으면 잔고를 다시 맞춘다
            claimGoalDay = { day -> economyApi.goalClaimDay(day) is ServerResult.Ok },
            onGoalPaid = { refreshEconomyInBackground() },
        )
        // 하루 러닝 목표의 범위 · 기본값 — 서버 규칙(shoe_rules_v4 "goal"). 예전 판에서 올라온 설치는 목표 규칙을 한 번 옮기고
        // 남긴 오늘의 보상을 되살린다(앱을 켜자마자 막대를 그리게 — 한국 날짜가 지났으면 "—")
        userPrefs.goalRule = { shoeRules.now.goal }
        economyScope.launch {
            runCatching { userPrefs.migrateGoalRule() }
            runCatching { shoeRules.restore() }
        }
        // 러너 레벨(0121) — 서버에 함수가 없으면 칸을 숨긴다. 알린 레벨은 계정마다 폰에 남긴다
        val runnerLevelApi = com.stepup.android.data.remote.RunnerLevelApi(server)
        runnerLevel = com.stepup.android.data.repo.RunnerLevelRepository(
            fetchMe = { runnerLevelApi.me() },
            fetchBadges = { ids -> runnerLevelApi.badges(ids) },
            store = userPrefs.runnerSeenStore,
            owner = { sessionHolder.recordingOwner() },
        )
        inviteApi = com.stepup.android.data.remote.InviteApi(server)
        accessApi = com.stepup.android.data.remote.AccessApi(server)
        val stepsApi = EventApi(server)
        dailyStepsUploader = com.stepup.android.data.repo.DailyStepsUploader(
            readDay = { day -> database.stepDao().byDay(day)?.let { it.steps to it.goal } },
            send = { days -> stepsApi.syncSteps(days) },
            account = { runCatching { sessionHolder.recordingOwner() }.getOrNull() },
        )
        economySync = com.stepup.android.data.repo.EconomySync(
            api = economyApi,
            market = MarketApi(server),
            db = database,
            prefs = userPrefs,
            sessions = sessionHolder,
            afterCheckIn = {
                if (economyApi.isConfigured) {
                    dailyStepsUploader.upload()
                    shoeRules.refresh()
                    shoeRules.refreshBoxes()
                    // 러너 레벨(0121) — 러닝을 올린 뒤의 동기화에서도 다시 읽어 레벨이 올랐으면 한 번 알린다
                    runCatching { runnerLevel.refresh() }
                    // 이 앱의 판(스토어 번호) — 운영자가 아이템을 줄 때 가방이 있는 판인지 본다(0116). 테스트 판은 데이터베이스 판 × 1000 을 뺀다
                    economyApi.reportAppVersion(BuildConfig.VERSION_CODE % 1000, BuildConfig.VERSION_NAME)
                }
            },
            // 신발마다 강화 경험치 통(0118 my_shoe_xp) — 신발 카드 · 상세의 "다음 레벨까지 N%"
            shoeXp = com.stepup.android.data.remote.ShoeXpApi(server),
            // 서버에 적힌 목표(0118 box_status) — 이 폰에서 바꾼 목표가 닿지 않았을 때가 아니면 폰이 서버 목표를 따른다
            serverGoal = { shoeRules.refreshGoal() },
        )
        // 서버 주소가 있는 빌드는 언제나 서버 경제다. 없는 빌드(로컬 개발)만 예전처럼 폰 안에서 계산한다.
        val serverEconomy = economyApi.isConfigured
        claimRepository = ClaimRepository(
            sessionDao = database.walkSessionDao(),
            uploadOwner = { sessionHolder.recordingOwner() },
            // 서버가 러닝을 확인했으면 목표 보너스를 청구해 보고 잔고 · 에너지를 다시 받는다
            onSigned = { signedStartedAt ->
                // AI 코칭 자동 분석(켠 사람만) — 따로 돌아 올리기 · 잔고 맞추기를 늦추거나 막지 않는다
                coachAfterUpload()
                if (serverEconomy) {
                    // 오늘의 보상(0118)이면 목표 보너스는 서버가 러닝을 기록할 때 준다. 그 단계가 실패했을 때만 남지 않게 확인받은 러닝이
                    // 시작한 날을 한 번 더 청구한다 — 이미 받은 날(23505) · 못 채운 날(23514)은 서버가 거절할 뿐이다
                    val serverPays = shoeRules.daily.value?.rule == com.stepup.android.domain.DailyRewards.RULE_REWARD_STEPS
                    if (serverPays) {
                        for (day in com.stepup.android.domain.DailyGoalSync.signedRunDays(signedStartedAt)) {
                            economyApi.goalClaimDay(day)
                        }
                    } else {
                        // 러닝은 시작한 날(한국 시각)의 목표에 든다 — 자정을 넘긴 러닝 · 자정 뒤에 올린 러닝도 그날 보너스를 받게
                        // 서버가 받는 최근 7일부터 오늘까지, 오래된 날부터(뒷날의 연속 일수가 앞날을 센다) 청구한다(0068).
                        // 아직 올리지 못한 이 계정의 러닝이 있으면 그날 뒤는 미룬다 — 짧은 연속 일수로 굳지 않게(EconomyApi.goalDays)
                        val pending = runCatching {
                            database.walkSessionDao().pendingUploads(GOAL_PENDING_SCAN, sessionHolder.recordingOwner()).map { it.startedAt }
                        }.getOrDefault(emptyList())
                        for (day in com.stepup.android.data.remote.EconomyApi.goalDays(signedStartedAt, pending)) {
                            economyApi.goalClaimDay(day)
                        }
                    }
                    economySync.refresh()
                    // 새로 받은 보상(러닝 상자 · 목표 보너스 …)을 한 번 알린다 — 앱이 앞에 있으면 홈의 한 줄, 뒤에 있으면 알림
                    runCatching {
                        shoeRules.refreshForNews()?.let { news ->
                            if (com.stepup.android.service.CrewStartAlert.appInForeground) shoeRules.showNews(news)
                            else com.stepup.android.push.GoalReminderWorker.showRewards(app, news)
                        }
                    }
                }
            },
            // 다시 로그인해야 올릴 수 있다 — 러닝 결과가 "다시 로그인"을 보이게
            onSignInRequired = { economySync.markSignedOut() },
            recorder = ServerSessionRecorder(
                server = server,
                courseApi = CourseApi(server),
                readCourseRun = { startedAt -> userPrefs.pendingCourseRun(startedAt) },
                acknowledgeCourseRun = { startedAt -> userPrefs.takePendingCourseRun(startedAt); Unit },
            ),
        )
        overspeedReporter = com.stepup.android.data.repo.OverspeedReporter(
            read = { userPrefs.pendingOverspeedStops() },
            acknowledge = { startedAt -> userPrefs.takePendingOverspeedStop(startedAt) },
            currentOwner = { sessionHolder.recordingOwner() },
            report = { stop, userId -> server.reportOverspeedStop(stop.startedAt, stop.stoppedAt, userId, stop.sneakerId) },
            configured = { server.isConfigured },
            onReported = { if (serverEconomy) economySync.refresh() },
        )
        val itemsApi = com.stepup.android.data.remote.ItemsApi(server)
        itemsRepository = com.stepup.android.data.repo.ItemsRepository(
            bagCall = { itemsApi.bag() },
            useCall = { kind, request, sneaker -> itemsApi.use(kind, request, sneaker) },
            bindCall = { startedAt, kinds, userId -> itemsApi.bindToRun(startedAt, kinds, userId) },
            addPending = { row -> userPrefs.addPendingRunItems(row) },
            readPending = { userPrefs.pendingRunItems() },
            takePending = { startedAt -> userPrefs.takePendingRunItems(startedAt) },
            configured = { server.isConfigured },
            onChanged = { if (serverEconomy) economySync.refresh() },
        )
        rankingRepository = RankingRepository(server)
        leaderboards = com.stepup.android.data.repo.ServerLeaderboardSource(com.stepup.android.data.remote.LeaderboardApi(server))
        stepTracker = StepTracker(app, userPrefs) { day ->
            database.stepDao().byDay(day)?.steps ?: 0
        }
        rewardRepository = RewardRepository(
            rewardDao = database.rewardDao(),
            sneakerDao = database.sneakerDao(),
            boostDao = database.boostDao(),
            notificationDao = database.notificationDao(),
            prefs = userPrefs,
            recoverRunEnergy = { runSettlementRepository.recoverEnergy() },
            serverEconomy = serverEconomy,
            claimGoalOnServer = {
                if (economyApi.goalClaim() is ServerResult.Ok) economySync.refresh()
            },
        )
        runSettlementRepository = com.stepup.android.data.repo.RunSettlementRepository(database, userPrefs, serverEconomy)
        // 신발을 갈아 신거나 강화하면 에너지 상한(화면·소모·리필)도 따라가게 한다
        rewardRepository.keepEnergyCapInSync(
            kotlinx.coroutines.CoroutineScope(kotlinx.coroutines.SupervisorJob() + kotlinx.coroutines.Dispatchers.IO),
        )
        stepRepository = StepRepository(
            stepDao = database.stepDao(),
            walkSessionDao = database.walkSessionDao(),
            prefs = userPrefs,
            tracker = stepTracker,
            rewardRepository = rewardRepository,
            // 서버가 적은 목표를 돌려준다(0118 — 4,000 ~ 20,000 · 500 단위로 맞춘다). 닿지 못했으면 null(폰 값 그대로)
            pushGoal = { goal -> if (serverEconomy) (economyApi.setDailyGoal(goal) as? ServerResult.Ok)?.value else null },
            owner = recordingOwnerFlow(),
            uploadSteps = { if (serverEconomy && sessionHolder.isSignedIn()) dailyStepsUploader.upload() },
            // 서버 경제에서는 목표 보너스 · 연속을 폰 만보기 걸음으로 정하지 않는다(서버가 러닝 걸음으로)
            localGoal = !serverEconomy,
            serverDaily = if (serverEconomy) shoeRules.daily else kotlinx.coroutines.flow.flowOf(null),
        )
        sneakerRepository = SneakerRepository(
            database, rewardRepository,
            economy = economyApi.takeIf { serverEconomy },
            sync = economySync.takeIf { serverEconomy },
            prefs = userPrefs,
            // 포인트 미리보기(0119) — 서버에 없으면 포인트 시트가 그 칸만 숨긴다
            pointsPreviewApi = com.stepup.android.data.remote.ShoePointsPreviewApi(server).takeIf { serverEconomy },
        )
        forgeRepository = com.stepup.android.data.repo.ForgeRepository(
            server = economyApi.takeIf { serverEconomy },
            pendingStore = com.stepup.android.data.repo.PrefsForgePendingStore(userPrefs),
            resync = { economySync.refresh() },
            // 경험치 강화(0118) — 서버에 없으면 ForgeRepository 가 예전 강화로 돈다
            xp = com.stepup.android.data.remote.ForgeXpApi(server).takeIf { serverEconomy },
            // 넣은 뒤의 통을 폰 사본에 바로(my_shoe_xp 를 못 읽어도 경험치 줄이 맞게)
            onXpDone = { r -> database.sneakerDao().setForgeXp(r.targetId, r.after.xp, r.after.need) },
        )
        avatarRepository = AvatarRepository(userPrefs, sneakerRepository)
        boostRepository = BoostRepository(
            database, rewardRepository, userPrefs,
            economy = economyApi.takeIf { serverEconomy },
            sync = economySync.takeIf { serverEconomy },
        )
        val crewApi = CrewApi(server)
        crewRepository = CrewRepository(
            api = crewApi,
            crewDao = database.crewDao(),
            crewInfoDao = database.crewInfoDao(),
            walkSessionDao = database.walkSessionDao(),
            rewardRepository = rewardRepository,
            partyApi = PartyApi(server),
            // 파티런 중 방에 보내는 위치 — 러닝 서비스가 받은 마지막 GPS 점. 30초 넘게 새 점이 없으면(GPS 를 잃었다)
            // 모른다고 보낸다 — 굴다리에서 멈춘 옛 점이 다른 사람에게 지금 위치처럼 보이지 않게
            currentLocation = {
                // 지금 GPS 위치 — 마지막 GPS 점이 30초 안일 때만. 경로 점(8m 움직여야 생긴다)으로 재면 신호등에서 30초 넘게
                // 서 있는 사람이 위치를 잃은 것으로 보내져 다른 사람 지도에서 사라지고 곁 판정에서도 빠졌다
                // 흐린 위치(오차 50m 초과 — 기지국 등)는 보내지 않는다 — 수백 m 틀린 자리가 다른 사람 지도 · 곁 판정에 들어갔다(2026-10-17 점검)
                WalkSessionService.state.value.let { s ->
                    s.here?.takeIf { s.hereAccurate && s.hereAt > 0 && System.currentTimeMillis() - s.hereAt <= 30_000L }
                }
            },
            currentKm = {
                // 화면 · 결과 · 기록과 같은 거리(RunDistance — 경로 100m 부터 GPS, 그 전엔 걸음)
                WalkSessionService.state.value.let { s -> com.stepup.android.domain.RunDistance.liveKm(s.gpsKm, s.steps) }
            },
            shareByDefault = { userPrefs.partyShareLocation.first() },
            exitedParties = { userPrefs.exitedParties.first() },
            rememberExit = { userPrefs.addExitedParty(it) },
        )
        // 계정이 바뀌면(다시 로그인 · 계정 삭제 · 새 로그인) 앞 계정의 대기실 · 가입 표시를 지운다 — 옛 방을 새 계정으로
        // 계속 묻거나, 앞 계정이 가입한 크루를 내 크루처럼 보이지 않게
        economyScope.launch {
            recordingOwnerFlow().drop(1).collect {
                crewRepository.resetForAccount()
                // 앞 계정의 러너 레벨 · 알림도(새 계정은 동기화 때 다시 읽는다)
                runnerLevel.forgetAccount()
            }
        }
        crewCards = com.stepup.android.data.repo.CrewCardRepository(
            api = crewApi,
            crews = crewRepository,
            images = com.stepup.android.data.repo.CrewImageStore(
                fetch = crewApi::image,
                dir = java.io.File(app.cacheDir, "crew_images"),
            ),
            prefs = userPrefs,
            owner = { sessionHolder.recordingOwner() },
            ownerFlow = recordingOwnerFlow(),
            draftDir = java.io.File(app.filesDir, "crew_drafts"),
        )
        crewChat = com.stepup.android.data.repo.CrewChatRepository(
            api = com.stepup.android.data.remote.CrewChatApi(server),
            crewApi = crewApi,
            prefs = userPrefs,
            owner = { sessionHolder.recordingOwner() },
            photoDir = java.io.File(app.cacheDir, "chat_photos"),
        )
        crewRun = com.stepup.android.data.repo.CrewRunRepository(
            api = com.stepup.android.data.remote.CrewRunApi(server),
            owner = { sessionHolder.recordingOwner() },
        )
        crewHome = com.stepup.android.data.repo.CrewHomeRepository(
            api = com.stepup.android.data.remote.CrewHomeApi(server),
            owner = { sessionHolder.recordingOwner() },
            // 홈이 접근 종료를 알면 같은 크루의 채팅 캐시도 지운다(대화 · 보내던 메시지 · 사진)
            onEnded = { crewId -> crewChat.forget(crewId) },
        )
        // 크루 러닝 상태 알림(0071) — 달리는 동안 5초마다 공용 체력을 정산한다. 마지막 한 사람이 되면 크게 알리고(소리 · 진동 · 알림)
        // 서버가 적립하는 데까지(1분) 기록한 뒤 러닝을 멈춘다. 끝난 러닝의 "끝냄"은 닿을 때까지 — 인터넷이 다시 되면 바로 다시 보낸다
        crewRun.startBeats(
            scope = kotlinx.coroutines.CoroutineScope(kotlinx.coroutines.SupervisorJob() + kotlinx.coroutines.Dispatchers.IO),
            context = app,
            party = crewRepository.party,
            run = com.stepup.android.service.WalkSessionService.state,
            onCrewPool = { startedAt, live, empty ->
                com.stepup.android.service.WalkSessionService.setCrewPool(startedAt, live.pool?.remaining, live.pool?.total, empty)
            },
            stopRun = { com.stepup.android.service.WalkSessionService.stop(app) },
            onAlone = { leftSec -> com.stepup.android.service.CrewRunAlerts.alone(app, leftSec) },
            onAloneSaved = { com.stepup.android.service.CrewRunAlerts.aloneSaved(app) },
            onAloneCleared = { com.stepup.android.service.CrewRunAlerts.clear(app) },
            networkBack = NetworkWatch.back,
        )
        communityRepository = CommunityRepository(
            api = CommunityApi(server),
            postDao = database.postDao(),
            commentDao = database.commentDao(),
            notificationDao = database.notificationDao(),
            prefs = userPrefs,
            owner = { sessionHolder.recordingOwner() },
            ownerFlow = recordingOwnerFlow(),
            localRuns = { owner, since ->
                val dao = database.walkSessionDao()
                com.stepup.android.domain.StoryLocalRuns(
                    count = dao.countFor(owner),
                    pendingRecent = dao.pendingSince(owner, since) > 0,
                )
            },
        )
        placeSearch = com.stepup.android.data.repo.PlaceSearch(
            api = com.stepup.android.data.remote.PlaceSearchApi(),
            platform = com.stepup.android.data.repo.AndroidGeocoder(app),
        )
        courseRepository = CourseRepository(
            dao = database.courseDao(),
            prefs = userPrefs,
            rewardRepository = rewardRepository,
            api = CourseApi(server),
            serverEconomy = serverEconomy,
        )
        eventRepository = EventRepository(
            dao = database.claimedEventDao(),
            api = EventApi(server),
            stepDao = database.stepDao(),
        )
        notificationRepository = NotificationRepository(database.notificationDao())
        marketRepository = MarketRepository(
            api = MarketApi(server),
            server = server,
            sneakerDao = database.sneakerDao(),
            rewardDao = database.rewardDao(),
            prefs = userPrefs,
            economySync = economySync.takeIf { serverEconomy },
        )
        runningFeedRepository = RunningFeedRepository(RunningFeedApi(server))
        announcementRepository = com.stepup.android.data.repo.AnnouncementRepository(
            source = com.stepup.android.data.remote.AnnouncementApi(server),
            cache = com.stepup.android.data.repo.FileAnnouncementCache(java.io.File(app.filesDir, "announcements.json")),
        )
        newsRepository = NewsRepository(
            server = server,
            dao = database.newsDao(),
            prefs = userPrefs,
        )
    }
}
