package com.stepup.android.ui.screens.items

import com.stepup.android.core.ServiceLocator
import com.stepup.android.data.repo.EconomyOutcome
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
 * 수리가 쓰는 기존 저장소 길 — 서버 요청은 지금 계약 그대로(SneakerRepository.repairOnServer).
 * [refresh] 는 읽기만 한다(서버 값을 다시 받아 폰 사본을 맞춘다 — 차감 · 지급을 하지 않는다).
 * 강화는 여기를 지나지 않는다 — 재료 신발 3개 · 확률 강화(ShoeUpgradeViewModel · ForgeRepository, 0054).
 */
interface ShoeCareBackend {
    val serverEconomy: Boolean
    suspend fun refresh(): EconomyOutcome
    suspend fun shoe(id: Long): Sneaker?
    suspend fun balance(): Double?
    suspend fun repair(id: Long): EconomyOutcome

    /**
     * 이 계정에 서버가 아직 신발 닳음을 반영하지 않은 러닝이 있는가 — 출발 전 점검과 같은 판단([pendingShoeWearOf]).
     * 있으면 서버가 그 러닝을 받은 뒤 수리 금액이 바뀐다(금액은 서버가 정한다 — 폰이 미리 빼서 견적을 꾸미지 않는다).
     */
    suspend fun unsentRuns(): Boolean = false

    /**
     * 이 신발(소유 id)을 닳게 할, 서버가 아직 반영하지 않은 러닝이 있는가 — 출발 때 신발을 적은 러닝은 그 신발일 때만,
     * 적지 않은 러닝은 신은 신발일 때만, 크루 러닝은 세지 않는다([pendingShoeWearOf]). 다른 신발을 고칠 때 "올라간 뒤 바뀔 수 있어요"를 보이지 않게.
     */
    suspend fun unsentRuns(id: Long): Boolean = unsentRuns()

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
    override suspend fun repair(id: Long): EconomyOutcome = ServiceLocator.sneakerRepository.repairOnServer(id)
    override suspend fun unsentRuns(id: Long): Boolean {
        // 러닝 줄을 읽기만 한다(고치지 않는다) — 러닝 화면의 남은 닳음 계산 그대로(이 신발의 러닝만)
        val shoe = ServiceLocator.database.sneakerDao().byId(id) ?: return false
        val rows = ServiceLocator.stepRepository.recentSessions(PENDING_WEAR_SCAN).first()
        return pendingShoeWearOf(
            rows,
            ServiceLocator.economySync.syncedFrom.value,
            System.currentTimeMillis(),
            shoeServerId = shoe.serverId,
            shoeEquipped = shoe.equipped,
        ) > 0
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
        val unsent = safeCheck { backend.unsentRuns(id) }
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
                val still = safeCheck { backend.unsentRuns(id) }
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
        val next = if (now is RepairPhase.Confirm && before != null && !now.quote.sameAs(before)) now.copy(changed = true) else now
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
     * RP01 · RP10 의 "N SUP로 수리하기" — 이때만 서버에 한 번 보낸다. 보내기 전 폰의 최신 사본으로 견적을 다시 보고,
     * 레벨 · 내구도 · 단가가 바뀌었으면 보내지 않고 새 견적(RP10)을 다시 확인받는다.
     * 한계: 지금 서버(sneaker_repair p_id)는 확인한 금액을 받지 않아, 확인과 실행 사이의 서버 쪽 변경까지 막지는 못한다.
     */
    fun confirmRepair(id: Long) {
        val shown = _repair.value[id] as? RepairPhase.Confirm ?: return
        // 누르는 즉시 보내는 중으로 — 두 번 눌러도 두 번 보내지 않는다
        setRepair(id, RepairPhase.Sending(shown.quote, shown.balance))
        scope.launch {
            val now = repairGate(backend.shoe(id), backend.balance(), backend.serverEconomy)
            if (now !is RepairPhase.Confirm) {
                setRepair(id, now)
                return@launch
            }
            if (!now.quote.sameAs(shown.quote)) {
                setRepair(id, now.copy(changed = true))
                return@launch
            }
            setRepair(id, RepairPhase.Sending(now.quote, now.balance))
            val outcome = safe { backend.repair(id) }
            val next = when (outcome) {
                EconomyOutcome.Ok -> settledRepair(id, now.quote, now.balance)
                EconomyOutcome.Offline -> RepairPhase.Unknown(now.quote, now.balance, accepted = false)
                EconomyOutcome.NothingToRepair ->
                    RepairPhase.NotNeeded(backend.shoe(id)?.server?.durabilityPts ?: 100.0)
                EconomyOutcome.NotEnoughBalance -> RepairPhase.Insufficient(now.quote, backend.balance() ?: now.balance)
                EconomyOutcome.SignInRequired -> RepairPhase.SignIn
                is EconomyOutcome.Rejected -> RepairPhase.Blocked(careBlockOf(outcome.reason))
                else -> RepairPhase.Blocked(CareBlock.OTHER)
            }
            ExperienceEvents.emit(if (next is RepairPhase.Done) FeedbackCue.Success else FeedbackCue.Error)
            setRepair(id, next)
        }
    }

    /**
     * 서버가 처리했다고 답한 뒤 — 동기화된 사본의 내구도가 가득일 때만 완료(RP04). 아직이면 확인 중으로 남긴다
     * (뒤이은 동기화가 됐는지 여기서는 모른다 — 결과 확인이 한 번 맞추면 끝낸다)
     */
    private suspend fun settledRepair(id: Long, quote: RepairQuote, before: Double): RepairPhase {
        val shoe = backend.shoe(id)
        val durability = shoe?.server?.durabilityPts
        val balance = backend.balance()
        return if (durability != null && durability >= 100.0 && balance != null) {
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
