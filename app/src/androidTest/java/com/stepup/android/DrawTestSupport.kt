package com.stepup.android

import com.stepup.android.core.ServiceLocator
import com.stepup.android.data.remote.ServerResult
import com.stepup.android.domain.DrawKind
import com.stepup.android.domain.DrawStatus
import com.stepup.android.domain.Faction
import com.stepup.android.domain.Rarity
import com.stepup.android.domain.Sneaker
import com.stepup.android.data.repo.toDomain
import com.stepup.android.ui.screens.gacha.DrawReply
import com.stepup.android.ui.screens.gacha.DrawSource
import com.stepup.android.ui.screens.gacha.InMemoryDrawMemory
import com.stepup.android.ui.screens.gacha.PendingDraw
import java.util.concurrent.CopyOnWriteArrayList
import java.util.concurrent.atomic.AtomicInteger
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.runBlocking

/** 뽑기 기기 검사의 예시 현황 — 시안의 13회 · 10회 · 0.6 / 1km 는 검사 화면 안에서만 쓴다 */
object DrawSamples {
    val FRESH = DrawStatus(
        dailyLeft = 3, dailyTotal = 3, signupLeft = 10, signupGranted = 10, walletLinked = false, giftOnLink = 10,
        giftLeft = 0, runLeft = 0, genesisLeft = 0, runProgressMeters = 0.0, runStepMeters = 1000,
        runToday = 0, runDailyCap = 10, chainPaused = false,
    )
    val LINKED = FRESH.copy(walletLinked = true, giftOnLink = 0, giftLeft = 10, genesisLeft = 1, runProgressMeters = 600.0)

    /** 두 기회를 모두 쓴 연결 회원(15) */
    val EMPTY = LINKED.copy(dailyLeft = 0, signupLeft = 0, giftLeft = 0, runLeft = 0)

    /** 예시 신발(검사 화면에만) */
    fun shoe(id: Long = 9_001, rarity: Rarity = Rarity.RARE, faction: Faction = Faction.WATER, variant: Int = 1) = Sneaker(
        id = id, faction = faction, rarity = rarity, variant = variant, level = 1, mintNumber = 128, luck = 1.0,
        comfort = 1.0, durability = 100, equipped = false, acquiredAt = 0,
    )

    /**
     * 이 기기의 신발 사본에 착용하지 않은 신발 한 켤레를 더한다 — 앱 셸 안의 뽑기 결과로 쓴다(검사가 끝나면 [removeShoe]).
     * 착용 중인 신발은 그대로 둔다.
     */
    fun addShoe(): Sneaker = runBlocking {
        ServiceLocator.sneakerRepository.ensureStarter()
        val dao = ServiceLocator.database.sneakerDao()
        val base = requireNotNull(dao.equippedNow() ?: dao.allNow().firstOrNull())
        if (!base.equipped) ServiceLocator.sneakerRepository.equip(base.id)
        val id = dao.insert(
            base.copy(
                id = 0, factionId = Faction.WATER.id, rarity = Rarity.RARE.id, variant = 1, level = 1,
                mintNumber = dao.maxMintNumber() + 1, equipped = false, acquiredAt = System.currentTimeMillis(), serverId = 0,
            ),
        )
        requireNotNull(dao.byId(id)).toDomain()
    }

    fun removeShoe(shoe: Sneaker) = runBlocking {
        val dao = ServiceLocator.database.sneakerDao()
        dao.byId(shoe.id)?.let { dao.delete(it) }
    }

    fun equippedId(): Long? = runBlocking { ServiceLocator.database.sneakerDao().equippedNow()?.id }
}

/**
 * 뽑기 서버 흉내 — 수는 서버처럼 뽑을 때마다 줄고, [gate] 가 있으면 뽑기 답을 붙잡는다.
 * [nextReply] 로 한 번의 답을 바꾼다(답을 잃음 · 거절). 결과 신발은 [shoe] 하나다(서버가 정한 신발 흉내).
 * 현황 읽기는 [statusGate] 로 붙잡고(불러오는 중), [statusFails] 면 실패, [statusOverride] 가 있으면 그 값을 그대로 준다.
 * [signedIn] 이 false 면 로그인 전이다.
 */
class FakeDrawSource(private val shoe: Sneaker) : DrawSource {
    val draws = AtomicInteger(0)
    val statusReads = AtomicInteger(0)
    val checks = AtomicInteger(0)
    val kinds = CopyOnWriteArrayList<DrawKind>()
    @Volatile var gate: CompletableDeferred<Unit>? = null
    @Volatile var linked = false
    @Volatile var free = 13
    @Volatile var premium = 10
    @Volatile var nextReply: DrawReply? = null
    @Volatile var signedIn = true
    @Volatile var statusGate: CompletableDeferred<Unit>? = null
    @Volatile var statusFails = false
    @Volatile var statusOverride: DrawStatus? = null
    @Volatile private var drawnOnServer = false
    override val memory = InMemoryDrawMemory()

    override fun ready() = signedIn

    override suspend fun status(): ServerResult<DrawStatus> {
        statusReads.incrementAndGet()
        statusGate?.await()
        if (statusFails) return ServerResult.Retry("test")
        statusOverride?.let { return ServerResult.Ok(it) }
        val daily = (free - 10).coerceAtLeast(0)
        return ServerResult.Ok(
            (if (linked) DrawSamples.LINKED else DrawSamples.FRESH).copy(
                dailyLeft = daily, signupLeft = free - daily, giftLeft = if (linked) premium else 0,
            ),
        )
    }

    override suspend fun newestShoeId(): ServerResult<Long> = ServerResult.Ok(shoe.id - 1)

    override suspend fun draw(kind: DrawKind): DrawReply {
        draws.incrementAndGet()
        kinds += kind
        gate?.await()
        gate = null
        val reply = nextReply
        nextReply = null
        if (reply is DrawReply.Refused) return reply
        // 서버에서는 처리됐다 — 답만 잃었을 수 있다
        if (kind == DrawKind.FREE) free-- else premium--
        drawnOnServer = true
        return reply ?: DrawReply.Drawn(shoe.id, shoe)
    }

    override suspend fun findDrawn(pending: PendingDraw): ServerResult<Sneaker?> {
        checks.incrementAndGet()
        return ServerResult.Ok(if (drawnOnServer) shoe else null)
    }

    override suspend fun account(): String = "draw-test"
}
