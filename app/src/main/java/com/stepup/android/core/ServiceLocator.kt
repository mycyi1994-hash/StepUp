package com.stepup.android.core
import kotlinx.coroutines.flow.first

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
        private set
    lateinit var sneakerRepository: SneakerRepository
        private set
    lateinit var avatarRepository: AvatarRepository
        private set
    lateinit var boostRepository: BoostRepository
        private set
    lateinit var crewRepository: CrewRepository
        private set
    lateinit var communityRepository: CommunityRepository
        private set
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

    lateinit var claimRepository: ClaimRepository

    /** 순위표 — 유일하게 남의 기록이 필요한 화면이라 서버가 계산해 준다 */
    lateinit var rankingRepository: RankingRepository
        private set

    /** 지금 누구로 로그인해 있는지. 화면이 로그인 상태를 물을 때 쓴다. */
    lateinit var sessionHolder: SessionHolder

    /** 달리는 중인 러닝의 저장본 — 앱이 죽어도 러닝을 되살린다. 백업하지 않는 폴더에 둔다. */
    lateinit var runCheckpoints: com.stepup.android.service.RunCheckpointStore
        private set

    /** 서버 경제(A안) — 잔고 · 신발 · 에너지의 정본은 서버다 */
    lateinit var economyApi: com.stepup.android.data.remote.EconomyApi
    lateinit var economySync: com.stepup.android.data.repo.EconomySync

    /** 서버가 잔고를 정하는 빌드인가 — 화면이 폰이 계산한 옛 금액을 가릴지 정할 때 쓴다 */
    val serverEconomyOn: Boolean
        get() = ::economyApi.isInitialized && economyApi.isConfigured

    private val economyScope = kotlinx.coroutines.CoroutineScope(
        kotlinx.coroutines.SupervisorJob() + kotlinx.coroutines.Dispatchers.IO,
    )

    /** 서버 경제를 뒤에서 한 번 맞춘다 — 앱 시작 · 로그인 · 러닝 업로드 뒤 */
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
        Analytics.init(app)
        pushRegistrar = PushRegistrar(
            PushApi(server),
            locale = { userPrefs.languageNow() },
            preferences = { userPrefs.notifyPrefs.first() },
        )
        territoryApi = TerritoryApi(server)
        economyApi = com.stepup.android.data.remote.EconomyApi(server)
        economySync = com.stepup.android.data.repo.EconomySync(
            api = economyApi,
            market = MarketApi(server),
            db = database,
            prefs = userPrefs,
            sessions = sessionHolder,
        )
        // 서버 주소가 있는 빌드는 언제나 서버 경제다. 없는 빌드(로컬 개발)만 예전처럼 폰 안에서 계산한다.
        val serverEconomy = economyApi.isConfigured
        claimRepository = ClaimRepository(
            sessionDao = database.walkSessionDao(),
            uploadOwner = { sessionHolder.recordingOwner() },
            // 서버가 러닝을 확인했으면 목표 보너스를 청구해 보고 잔고 · 에너지를 다시 받는다
            onSigned = {
                if (serverEconomy) {
                    economyApi.goalClaim()
                    economySync.refresh()
                }
            },
            recorder = ServerSessionRecorder(
                server = server,
                courseApi = CourseApi(server),
                readCourseRun = { startedAt -> userPrefs.pendingCourseRun(startedAt) },
                acknowledgeCourseRun = { startedAt -> userPrefs.takePendingCourseRun(startedAt); Unit },
            ),
        )
        rankingRepository = RankingRepository(server)
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
            pushGoal = { goal -> if (serverEconomy) economyApi.setDailyGoal(goal) },
        )
        sneakerRepository = SneakerRepository(
            database, rewardRepository,
            economy = economyApi.takeIf { serverEconomy },
            sync = economySync.takeIf { serverEconomy },
            prefs = userPrefs,
        )
        avatarRepository = AvatarRepository(userPrefs, sneakerRepository)
        boostRepository = BoostRepository(
            database, rewardRepository, userPrefs,
            economy = economyApi.takeIf { serverEconomy },
            sync = economySync.takeIf { serverEconomy },
        )
        crewRepository = CrewRepository(
            api = CrewApi(server),
            crewDao = database.crewDao(),
            crewInfoDao = database.crewInfoDao(),
            walkSessionDao = database.walkSessionDao(),
            rewardRepository = rewardRepository,
            partyApi = PartyApi(server),
            // 파티런 중 방에 보내는 위치 — 러닝 서비스가 받은 마지막 GPS 점
            currentLocation = { WalkSessionService.state.value.track.lastOrNull()?.toGeoPoint() },
        )
        communityRepository = CommunityRepository(
            api = CommunityApi(server),
            postDao = database.postDao(),
            commentDao = database.commentDao(),
            notificationDao = database.notificationDao(),
            prefs = userPrefs,
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
        newsRepository = NewsRepository(
            server = server,
            dao = database.newsDao(),
            prefs = userPrefs,
        )
    }
}
