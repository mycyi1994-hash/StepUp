package com.stepup.android.ui.screens.items

import com.stepup.android.core.ServiceLocator
import com.stepup.android.data.repo.EconomyOutcome
import com.stepup.android.data.repo.RepairReply
import com.stepup.android.data.repo.refreshShoeRules
import com.stepup.android.data.repo.toEconomyOutcome
import com.stepup.android.domain.Sneaker
import com.stepup.android.sync.SessionUploadWorker
import com.stepup.android.ui.screens.walk.PENDING_WEAR_SCAN
import com.stepup.android.ui.screens.walk.pendingShoeWearOf
import com.stepup.android.ui.experience.ExperienceEvents
import com.stepup.android.ui.experience.FeedbackCue
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.drop
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/**
 * 수리가 쓰는 저장소 길 — 서버 요청은 경제 4판 수리(SneakerRepository.repairChecked → sneaker_repair_v2, 예전 서버면 sneaker_repair).
 * [refresh] 는 읽기만 한다(서버 값을 다시 받아 폰 사본을 맞춘다 — 차감 · 지급을 하지 않는다).
 * 강화는 여기를 지나지 않는다 — 재료 신발 3개 · 확률 강화(ShoeUpgradeViewModel · ForgeRepository, 0054).
 */
interface ShoeCareBackend {
    val serverEconomy: Boolean
    suspend fun refresh(): EconomyOutcome
    suspend fun shoe(id: Long): Sneaker?
    suspend fun balance(): Double?

    /**
     * 수리 한 번 — [points] 는 고칠 내구도(가진 만큼 수리 · 예전 서버는 null = 가득), [expected] 는 화면이 보인 수리비.
     * 서버 값이 다르면 고치지 않고 [RepairReply.quoteChanged] 로 돌려준다
     */
    suspend fun repair(id: Long, points: Int?, expected: Double?): RepairReply

    /**
     * 이 계정에 서버가 아직 신발 닳음을 반영하지 않은 러닝이 있는가 — 출발 전 점검과 같은 판단([pendingShoeWearOf]).
     * 있으면 서버가 그 러닝을 받은 뒤 수리 금액이 바뀐다(금액은 서버가 정한다 — 폰이 미리 빼서 견적을 꾸미지 않는다).
     */
    suspend fun unsentRuns(): Boolean = false

    /** 올리지 못한 러닝을 올리도록 올리기 일꾼을 깨운다 */
    fun sendRuns() {}

    /** 동기화가 끝날 때마다 바뀌는 값 — 러닝이 올라가 서버가 닳게 한 뒤의 동기화를 보고 견적을 다시 본다 */
    val synced: Flow<Long> get() = emptyFlow()
}

private object AppShoeCareBackend : ShoeCareBackend {
    override val serverEconomy: Boolean get() = ServiceLocator.serverEconomyOn
    override suspend fun refresh(): EconomyOutcome =
        if (!serverEconomy) EconomyOutcome.SignInRequired else ServiceLocator.economySync.refresh().toEconomyOutcome()
    override suspend fun shoe(id: Long): Sneaker? = ServiceLocator.sneakerRepository.inventory.first().firstOrNull { it.id == id }
    override suspend fun balance(): Double? = ServiceLocator.rewardRepository.balance.first()
    override suspend fun repair(id: Long, points: Int?, expected: Double?): RepairReply =
        // 수리 뒤 규칙을 다시 읽는다 — 무료 수리를 썼으면 다음 무료 수리 시각이 바뀐다
        ServiceLocator.sneakerRepository.repairChecked(id, points, expected).also { refreshShoeRules() }
    override suspend fun unsentRuns(): Boolean {
        // 러닝 줄을 읽기만 한다(고치지 않는다) — 러닝 화면의 남은 닳음 계산 그대로
        val rows = ServiceLocator.stepRepository.recentSessions(PENDING_WEAR_SCAN).first()
        return pendingShoeWearOf(rows, ServiceLocator.economySync.syncedFrom.value, System.currentTimeMillis()) > 0
    }
    override fun sendRuns() = SessionUploadWorker.schedule(ServiceLocator.appContext)
    override val synced: Flow<Long> get() = ServiceLocator.economySync.syncedFrom
}

/**
 * 신발 한 켤레(소유 id)마다 수리의 진행을 들고 있는 곳 — 상세에서 나가도 보낸 요청의 결말을 잃지 않게
 * 화면(ViewModel)보다 오래 산다. 앱을 다시 켜면 비어 있다: 지금 서버(sneaker_repair)에는 요청 id 로 결과를 조회하는 계약이 없어
 * 재시작 뒤 같은 요청을 복구할 수 없다(tracker 에 적었다). 강화는 요청 키를 폰에 저장해 재시작 뒤에도 복구한다(ForgeRepository).
 *
 * 지키는 것 — 같은 신발에 결과가 안 정해진 요청이 있으면 새로 보내지 않는다. 결과를 모르면 "결과 확인"은 읽기만 한다.
 * 성공은 서버의 성공 답과 동기화된 값으로만.
 */
class ShoeCareStore(
    private val backend: ShoeCareBackend,
    private val scope: CoroutineScope,
) {
    private val _repair = MutableStateFlow<Map<Long, RepairPhase>>(emptyMap())
    val repair: StateFlow<Map<Long, RepairPhase>> = _repair.asStateFlow()

    private fun setRepair(id: Long, phase: RepairPhase?) = _repair.update { if (phase == null) it - id else it + (id to phase) }

    /**
     * 아직 올라가지 않은 러닝이 있는가(수리 시트를 열 때 본다) — 시트가 "올라간 뒤 수리 금액이 바뀔 수 있어요" 한 줄을 보인다.
     * 수리는 그대로 할 수 있다. 금액은 늘 서버 사본으로만 매긴다.
     */
    private val _unsentRuns = MutableStateFlow(false)
    val unsentRuns: StateFlow<Boolean> = _unsentRuns.asStateFlow()
    private var unsentWatch: Job? = null

    /** 대상 신발에 결과가 안 정해진 수리가 있는가 — 착용 · 강화 · 수리 · 판매를 막는다(강화 미확인은 상세가 따로 본다) */
    fun busy(id: Long): Boolean = _repair.value[id]?.pending == true

    // ── 수리 ──────────────────────────────────────────────────────

    /** 관리 › 수리하기 — RP02 에서 최신 내구도 · 비용 · 잔고를 읽는다(읽기만). 결과가 안 정해진 수리가 있으면 그 상태를 그대로 연다 */
    fun openRepair(id: Long) {
        val current = _repair.value[id]
        if (current?.pending == true || current is RepairPhase.Loading) return
        setRepair(id, RepairPhase.Loading)
        scope.launch {
            val quoted = quoteRepair(id)
            setRepair(id, quoted)
            // 금액을 보이는 장면에서만 — 로그인 · 읽기 실패 · 막힌 신발이면 볼 금액이 없다
            if (quoted is RepairPhase.Confirm || quoted is RepairPhase.Insufficient || quoted is RepairPhase.NotNeeded) {
                watchUnsentRuns(id)
            }
        }
    }

    /**
     * 견적을 낸 뒤(그 동기화가 끝난 뒤) 아직 올라가지 않은 러닝이 있으면 알리고 올리기 일꾼을 깨운다. 러닝이 올라가 서버가 닳게 하면
     * 뒤이은 동기화(ClaimRepository.onSigned)가 폰 사본을 맞춘다 — 그때마다 견적을 다시 본다. 다 올라갔거나 시트를 닫으면 그만 본다.
     */
    private suspend fun watchUnsentRuns(id: Long) {
        unsentWatch?.cancel()
        val unsent = safeCheck { backend.unsentRuns() }
        _unsentRuns.value = unsent
        if (!unsent) return
        backend.sendRuns()
        unsentWatch = scope.launch {
            backend.synced.distinctUntilChanged().drop(1).collect {
                if (_repair.value[id] == null) {
                    _unsentRuns.value = false
                    unsentWatch?.cancel()
                    return@collect
                }
                requote(id)
                val still = safeCheck { backend.unsentRuns() }
                _unsentRuns.value = still
                if (!still) unsentWatch?.cancel()
            }
        }
    }

    /**
     * 동기화된 사본으로 견적만 다시 본다(서버에 보내지 않는다). 사용자가 금액을 보고 있는 장면(확인 · 잔액 부족 · 가득)만 바꾼다 —
     * 보내는 중 · 결과 미확인은 그대로. 금액이 바뀌었으면 "금액이 바뀌었어요"(RP10)로 다시 확인받는다.
     */
    private suspend fun requote(id: Long) {
        val shown = _repair.value[id]
        if (shown !is RepairPhase.Confirm && shown !is RepairPhase.Insufficient && shown !is RepairPhase.NotNeeded) return
        val now = repairGate(backend.shoe(id), backend.balance(), backend.serverEconomy)
        // 그사이 사용자가 수리를 보냈으면 건드리지 않는다
        if (_repair.value[id] !== shown) return
        val before = when (shown) {
            is RepairPhase.Confirm -> shown.quote
            is RepairPhase.Insufficient -> shown.quote
            else -> null
        }
        val quoted = when (now) {
            is RepairPhase.Confirm -> now.quote
            is RepairPhase.Insufficient -> now.quote
            else -> null
        }
        val next = if (before != null && quoted != null && !quoted.sameAs(before)) now.markChanged() else now
        setRepair(id, next)
    }

    private suspend fun quoteRepair(id: Long): RepairPhase {
        if (!backend.serverEconomy) return RepairPhase.SignIn
        return when (safe { backend.refresh() }) {
            EconomyOutcome.Ok -> repairGate(backend.shoe(id), backend.balance(), backend.serverEconomy)
            EconomyOutcome.SignInRequired -> RepairPhase.SignIn
            else -> RepairPhase.LoadFailed
        }
    }

    /**
     * RP01 · RP10 의 "N SUP로 수리하기"(무료면 "무료로 수리하기") · RP06 의 "가진 만큼 수리하기" — 이때만 서버에 한 번 보낸다.
     * 보내기 전 폰의 최신 사본으로 견적을 다시 보고, 레벨 · 내구도 · 단가가 바뀌었으면 보내지 않고 새 견적(RP10)을 다시 확인받는다.
     * 보인 금액을 함께 보내(sneaker_repair_v2 의 p_expected) 서버 값이 다르면 서버가 고치지 않고 새 값을 돌려준다 — 그때도 다시 확인받는다.
     */
    fun confirmRepair(id: Long) {
        val shown = _repair.value[id] ?: return
        val quote = shown.sendable() ?: return
        val shownBalance = (shown as? RepairPhase.Confirm)?.balance ?: (shown as? RepairPhase.Insufficient)?.balance ?: return
        // 누르는 즉시 보내는 중으로 — 두 번 눌러도 두 번 보내지 않는다
        setRepair(id, RepairPhase.Sending(quote, shownBalance))
        scope.launch {
            val now = repairGate(backend.shoe(id), backend.balance(), backend.serverEconomy)
            val nowQuote = now.sendable()
            if (nowQuote == null) {
                setRepair(id, now)
                return@launch
            }
            if (!now.sameQuoteAs(shown)) {
                setRepair(id, now.markChanged())
                return@launch
            }
            val balance = (now as? RepairPhase.Confirm)?.balance ?: (now as? RepairPhase.Insufficient)?.balance ?: shownBalance
            setRepair(id, RepairPhase.Sending(nowQuote, balance))
            val reply = safeRepair { backend.repair(id, nowQuote.sendPoints, nowQuote.cost.toDouble()) }
            val next = when (val outcome = reply.outcome) {
                EconomyOutcome.Ok -> settledRepair(id, nowQuote, balance)
                EconomyOutcome.Offline -> RepairPhase.Unknown(nowQuote, balance, accepted = false)
                EconomyOutcome.NothingToRepair ->
                    RepairPhase.NotNeeded(backend.shoe(id)?.server?.durabilityPts ?: 100.0)
                EconomyOutcome.NotEnoughBalance -> {
                    val latest = backend.balance() ?: balance
                    val gate = repairGate(backend.shoe(id), latest, backend.serverEconomy)
                    gate as? RepairPhase.Insufficient ?: RepairPhase.Insufficient(nowQuote, latest)
                }
                EconomyOutcome.SignInRequired -> RepairPhase.SignIn
                // 서버 수리비가 보인 값과 다르다(무료를 그새 다른 폰에서 썼다 · 효율이 바뀌었다) — 치르지 않았다. 다시 받은 값으로 다시 확인받는다
                is EconomyOutcome.Rejected -> if (reply.quoteChanged != null || outcome.reason == com.stepup.android.data.repo.QUOTE_CHANGED) {
                    repairGate(backend.shoe(id), backend.balance(), backend.serverEconomy).markChanged()
                } else {
                    RepairPhase.Blocked(careBlockOf(outcome.reason))
                }
                else -> RepairPhase.Blocked(CareBlock.OTHER)
            }
            ExperienceEvents.emit(if (next is RepairPhase.Done) FeedbackCue.Success else FeedbackCue.Error)
            setRepair(id, next)
        }
    }

    /**
     * 서버가 처리했다고 답한 뒤 — 동기화된 사본의 내구도가 고친 만큼(가득 · 가진 만큼)일 때만 완료(RP04). 아직이면 확인 중으로 남긴다
     * (뒤이은 동기화가 됐는지 여기서는 모른다 — 결과 확인이 한 번 맞추면 끝낸다)
     */
    private suspend fun settledRepair(id: Long, quote: RepairQuote, before: Double): RepairPhase {
        val shoe = backend.shoe(id)
        val server = shoe?.server
        val durability = server?.durabilityPts
        val balance = backend.balance()
        val reached = when {
            server == null -> false
            quote.absolute && server.hasDurabilityMax -> server.durabilityNow >= quote.targetNow
            else -> server.durabilityPts >= 100.0
        }
        return if (reached && durability != null && balance != null) {
            RepairPhase.Done(quote, balance, durability)
        } else {
            RepairPhase.Unknown(quote, before, accepted = true)
        }
    }

    /** RP08 · SD19 의 "결과 확인" — 서버 값을 다시 읽기만 한다. 수리를 다시 보내지 않는다 */
    fun recheckRepair(id: Long) {
        val unknown = _repair.value[id] as? RepairPhase.Unknown ?: return
        if (unknown.checking) return
        setRepair(id, unknown.copy(checking = true))
        scope.launch {
            val synced = safe { backend.refresh() } == EconomyOutcome.Ok
            val durability = if (synced) backend.shoe(id)?.server?.durabilityPts else null
            val next = when {
                !synced -> unknown.copy(checking = false)
                // 서버가 처리했다고 답한 수리 — 처음으로 맞춘 값이면 내구도와 상관없이 끝낸다. 그사이 달려 100 아래로 닳았을 수
                // 있고, 100 만 기다리면 결과 확인이 끝나지 않아 신발이 잠긴 채 남았다
                unknown.accepted && durability != null -> {
                    val balance = backend.balance()
                    if (balance != null) RepairPhase.Done(unknown.quote, balance, durability) else unknown.copy(checking = false)
                }
                // 응답을 못 받은 요청 — 지금 서버에는 요청별 결과 조회가 없어 이 수리가 반영됐는지 단정하지 않는다. 확인한 값만 보인다
                else -> unknown.copy(checking = false, observed = durability)
            }
            setRepair(id, next)
        }
    }

    /** RP02 · RP07 의 "다시 불러오기" — 조회만 */
    fun reloadRepair(id: Long) {
        val current = _repair.value[id]
        if (current?.pending == true) return
        setRepair(id, null)
        openRepair(id)
    }

    /** 시트 닫기 — 결과가 안 정해진 수리는 남긴다(상세가 SD19 로 이어서 보인다). 닫기가 서버 요청을 취소하지 않는다 */
    fun closeRepair(id: Long) {
        if (_repair.value[id]?.pending == true) return
        setRepair(id, null)
        if (_repair.value.isEmpty()) {
            unsentWatch?.cancel()
            _unsentRuns.value = false
        }
    }

    /** 읽기 실패는 "없음"으로 — 안내 한 줄을 못 보일 뿐 수리는 그대로 */
    private suspend fun safeCheck(block: suspend () -> Boolean): Boolean = try {
        block()
    } catch (cancelled: CancellationException) {
        throw cancelled
    } catch (_: Exception) {
        false
    }

    private suspend fun safeRepair(block: suspend () -> RepairReply): RepairReply = try {
        block()
    } catch (cancelled: CancellationException) {
        throw cancelled
    } catch (_: Exception) {
        RepairReply(EconomyOutcome.Offline)
    }

    private suspend fun safe(block: suspend () -> EconomyOutcome): EconomyOutcome = try {
        block()
    } catch (cancelled: CancellationException) {
        throw cancelled
    } catch (_: Exception) {
        EconomyOutcome.Offline
    }

    companion object {
        /** 앱 하나에 하나 — 화면을 나가도 보낸 요청의 결말을 받는다 */
        val shared: ShoeCareStore by lazy {
            ShoeCareStore(AppShoeCareBackend, CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate))
        }
    }
}
