package com.stepup.android.ui.screens.items

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.stepup.android.core.ServiceLocator
import com.stepup.android.data.repo.ForgeLoad
import com.stepup.android.data.repo.ForgeOutcome
import com.stepup.android.data.repo.ForgeRepository
import com.stepup.android.domain.ForgeMaterial
import com.stepup.android.domain.ForgePending
import com.stepup.android.domain.ForgeQuote
import com.stepup.android.domain.ForgeTargetBlock
import com.stepup.android.domain.MaterialSelection
import com.stepup.android.domain.Sneaker
import com.stepup.android.ui.experience.ExperienceEvents
import com.stepup.android.ui.experience.FeedbackCue
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/**
 * 신발 강화 화면(지시서 v6 01 ~ 15 · 17 · 18)의 동작. 화면은 [ShoeUpgradeState] 만 그린다.
 *
 * 대상 · 재료 후보 · 확률은 서버(forge_materials · forge_quote)가 정한다. 재료 선택은 [MaterialSelection] 하나로 — 메인 슬롯 ·
 * 선택창의 체크 · 숫자 · 버튼이 같은 소유 id 집합을 본다. 실행은 [ForgeRepository.start] 하나만 부르고, 보낸 동안은 다시 보내지 않는다.
 */
class ShoeUpgradeViewModel(
    private val requestedId: Long,
    private val forge: ForgeRepository,
    private val localShoes: Flow<List<Sneaker>>,
) : ViewModel() {

    private val _state = MutableStateFlow(ShoeUpgradeState())
    val state: StateFlow<ShoeUpgradeState> = _state.asStateFlow()

    /** 지금 보내는 중(견적 · 실행 · 결과 조회) — 두 번 누르기 막기 */
    private var busy = false

    init {
        viewModelScope.launch { open() }
    }

    private suspend fun open() {
        // 결과를 아직 모르는 요청이 있으면 새로 강화하지 않고 그 요청의 결과부터 본다(어느 신발이든)
        val pending = runCatching { forge.pending() }.getOrNull()
        val targetId = pending?.targetId ?: requestedId
        _state.update { it.copy(targetId = targetId, shoe = localShoe(targetId)) }
        reload(targetId)
        if (pending != null) {
            _state.update { it.copy(phase = UpgradePhase.Unknown(pending), running = materialsOf(pending.materialIds)) }
            checkResult()
        }
    }

    private suspend fun localShoe(id: Long): Sneaker? =
        runCatching { localShoes.first().firstOrNull { it.id == id } }.getOrNull()

    private fun materialsOf(ids: List<Long>): List<ForgeMaterial> {
        val known = _state.value.known
        return ids.mapNotNull { known[it] }
    }

    /** 대상과 후보를 다시 읽는다. 읽지 못해도 적용된 선택은 지우지 않는다 */
    private suspend fun reload(targetId: Long = _state.value.targetId): Boolean {
        return when (val load = forge.load(targetId)) {
            is ForgeLoad.Ready -> {
                val confirmedGone = _state.value.selection.applied.filter { id ->
                    load.materials.none { it.id == id && it.usable }
                }
                _state.update { s ->
                    s.copy(
                        info = load.target,
                        candidates = MaterialsLoad.Ready(load.materials),
                        known = s.known + load.materials.associateBy { it.id },
                        selection = s.selection.drop(confirmedGone),
                        shoe = localShoe(targetId) ?: s.shoe,
                        phase = when {
                            s.phase is UpgradePhase.Unknown || s.phase is UpgradePhase.Running ||
                                s.phase is UpgradePhase.Succeeded || s.phase is UpgradePhase.Failed -> s.phase
                            load.target.block != null -> UpgradePhase.Blocked(load.target.block)
                            else -> UpgradePhase.Editing
                        },
                    )
                }
                true
            }
            ForgeLoad.SignInRequired -> {
                _state.update {
                    it.copy(phase = if (it.phase is UpgradePhase.Loading) UpgradePhase.SignIn else it.phase,
                        candidates = MaterialsLoad.Failed)
                }
                false
            }
            ForgeLoad.Failed -> {
                _state.update {
                    it.copy(phase = if (it.phase is UpgradePhase.Loading) UpgradePhase.LoadFailed else it.phase,
                        candidates = MaterialsLoad.Failed)
                }
                false
            }
        }
    }

    fun retryLoad() {
        _state.update { it.copy(phase = UpgradePhase.Loading) }
        viewModelScope.launch { reload() }
    }

    // ── 재료 선택(02 · 08 · 09 · 10 · 11) ─────────────────────────

    /** 빈 슬롯의 + — 적용된 선택을 지닌 채 선택창을 열고 후보를 새로 읽는다 */
    fun openPicker() {
        if (_state.value.phase !is UpgradePhase.Editing) return
        _state.update { it.copy(selection = it.selection.openPicker(), sheet = UpgradeSheet.Picker, candidates = MaterialsLoad.Loading) }
        viewModelScope.launch { reload() }
    }

    /** 11 다시 불러오기 */
    fun reloadPicker() {
        _state.update { it.copy(candidates = MaterialsLoad.Loading) }
        viewModelScope.launch { reload() }
    }

    fun toggle(id: Long) {
        val s = _state.value
        val usable = (s.candidates as? MaterialsLoad.Ready)?.list?.any { it.id == id && it.usable } == true
        if (!usable && id !in s.selection.draft.orEmpty()) return
        ExperienceEvents.emit(FeedbackCue.Select)
        _state.update { it.copy(selection = it.selection.toggle(id)) }
    }

    fun applyPicker() {
        _state.update { it.copy(selection = it.selection.apply(), sheet = UpgradeSheet.None, quote = null) }
    }

    /** X · 뒤로 · 아래로 닫기 · 바깥 — 선택창을 열기 전의 선택으로 */
    fun closeSheet() {
        _state.update { it.copy(selection = it.selection.cancel(), sheet = UpgradeSheet.None) }
    }

    fun removeMaterial(id: Long) {
        _state.update { it.copy(selection = it.selection.remove(id), quote = null) }
    }

    // ── 실행(03 → 04 → 05 → 06 · 07 · 12 · 14 · 15) ─────────────

    /** 03 강화하기 — 서버 견적을 받아 04 최종 확인 */
    fun requestConfirm() {
        val s = _state.value
        if (busy || s.phase !is UpgradePhase.Editing || s.selection.applied.size != com.stepup.android.domain.ShoeForge.MATERIALS) return
        busy = true
        _state.update { it.copy(quoting = true) }
        viewModelScope.launch {
            try {
                handle(forge.quote(s.targetId, s.selection.applied), fromConfirm = false)
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (_: Exception) {
                _state.update { it.copy(sheet = UpgradeSheet.Offline) }
            } finally {
                busy = false
                _state.update { it.copy(quoting = false) }
            }
        }
    }

    /** 04 강화 시작 */
    fun start() {
        val s = _state.value
        val quote = (s.sheet as? UpgradeSheet.Confirm)?.quote ?: return
        if (busy) return
        busy = true
        val snapshot = materialsOf(quote.materialIds)
        _state.update { it.copy(sheet = UpgradeSheet.None, phase = UpgradePhase.Running, running = snapshot, quote = quote) }
        viewModelScope.launch {
            try {
                handle(forge.start(s.targetId, quote), fromConfirm = true)
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (_: Exception) {
                val pending = runCatching { forge.pending() }.getOrNull()
                _state.update {
                    it.copy(phase = if (pending != null) UpgradePhase.Unknown(pending) else UpgradePhase.Editing,
                        sheet = if (pending == null) UpgradeSheet.Offline else UpgradeSheet.None)
                }
            } finally {
                busy = false
            }
        }
    }

    /** 15 결과 다시 확인 — 같은 요청 키의 결과만 묻는다 */
    fun checkResult() {
        val pending = (_state.value.phase as? UpgradePhase.Unknown)?.pending ?: return
        if (busy) return
        busy = true
        _state.update { it.copy(checking = true) }
        viewModelScope.launch {
            try {
                handle(forge.check(pending), fromConfirm = true)
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (_: Exception) {
                _state.update { it.copy(phase = UpgradePhase.Unknown(pending)) }
            } finally {
                busy = false
                _state.update { it.copy(checking = false) }
            }
        }
    }

    private suspend fun handle(outcome: ForgeOutcome, fromConfirm: Boolean) {
        when (outcome) {
            is ForgeOutcome.Quoted -> _state.update { it.copy(quote = outcome.quote, sheet = UpgradeSheet.Confirm(outcome.quote)) }
            is ForgeOutcome.QuoteChanged -> {
                // 최신 준비 상태를 보이고 다시 최종 확인을 받는다
                _state.update { it.copy(phase = UpgradePhase.Editing, quote = outcome.quote, sheet = UpgradeSheet.Confirm(outcome.quote)) }
                reload()
            }
            is ForgeOutcome.Done -> {
                ExperienceEvents.emit(if (outcome.result.success) FeedbackCue.Success else FeedbackCue.Error)
                val before = _state.value.quote?.before ?: _state.value.info?.stats
                _state.update {
                    it.copy(
                        phase = if (outcome.result.success) UpgradePhase.Succeeded(outcome.result, before)
                        else UpgradePhase.Failed(outcome.result),
                        running = materialsOf(outcome.result.materialIds).ifEmpty { it.running },
                        selection = MaterialSelection(),
                        quote = null,
                    )
                }
                // 대상 최신 값 · 남은 후보(태운 재료는 빠진다)
                reload()
                _state.update { it.copy(shoe = localShoe(it.targetId) ?: it.shoe) }
            }
            ForgeOutcome.NotAccepted, ForgeOutcome.NotSent -> {
                // 요청을 보내지 않았거나 서버가 받지 않았다고 확정 — 재료는 그대로, 자동으로 다시 보내지 않는다(14)
                _state.update { it.copy(phase = UpgradePhase.Editing, sheet = UpgradeSheet.Offline) }
            }
            ForgeOutcome.Unknown -> {
                val pending = forge.pending()
                _state.update {
                    it.copy(phase = if (pending != null) UpgradePhase.Unknown(pending) else UpgradePhase.Editing,
                        sheet = if (pending == null) UpgradeSheet.Offline else UpgradeSheet.None)
                }
            }
            is ForgeOutcome.MaterialsUnavailable -> {
                val gone = outcome.ids.mapNotNull { _state.value.known[it] }
                _state.update {
                    it.copy(
                        phase = UpgradePhase.Editing,
                        selection = it.selection.drop(outcome.ids),
                        sheet = UpgradeSheet.Changed(gone),
                        quote = null,
                    )
                }
                reload()
            }
            is ForgeOutcome.TargetBlocked -> _state.update {
                it.copy(phase = UpgradePhase.Blocked(outcome.block), sheet = UpgradeSheet.None, selection = MaterialSelection())
            }
            ForgeOutcome.SignInRequired -> _state.update {
                it.copy(phase = if (fromConfirm) UpgradePhase.SignIn else it.phase.takeUnless { p -> p is UpgradePhase.Running } ?: UpgradePhase.Editing,
                    sheet = UpgradeSheet.None)
            }
            is ForgeOutcome.Rejected -> _state.update {
                it.copy(phase = UpgradePhase.Editing, sheet = UpgradeSheet.Offline)
            }
        }
    }

    /** 14 연결 다시 확인 — 서버에 닿으면 시트를 닫고 준비 상태로. 강화는 다시 보내지 않는다(사용자가 다시 누른다) */
    fun recheckConnection() {
        if (busy) return
        busy = true
        _state.update { it.copy(checking = true) }
        viewModelScope.launch {
            try {
                if (reload()) _state.update { it.copy(sheet = UpgradeSheet.None) }
            } finally {
                busy = false
                _state.update { it.copy(checking = false) }
            }
        }
    }

    /** 06 다음 레벨 강화 · 07 재료 다시 선택 — 빈 슬롯으로 새로 시작(태운 재료를 다시 채우지 않는다) */
    fun again() {
        _state.update { it.copy(selection = MaterialSelection(), quote = null, running = emptyList(), sheet = UpgradeSheet.None, phase = UpgradePhase.Loading) }
        viewModelScope.launch { reload() }
    }

    companion object {
        fun factory(id: Long) = viewModelFactory {
            initializer {
                ShoeUpgradeViewModel(id, ServiceLocator.forgeRepository, ServiceLocator.sneakerRepository.inventory)
            }
        }
    }
}

/** 재료 후보 읽기 — 읽는 중(10) · 실패(11) · 목록(02 · 09) */
sealed interface MaterialsLoad {
    data object Loading : MaterialsLoad
    data object Failed : MaterialsLoad
    data class Ready(val list: List<ForgeMaterial>) : MaterialsLoad
}

/** 강화 화면 본문의 상태 */
sealed interface UpgradePhase {
    data object Loading : UpgradePhase
    data object LoadFailed : UpgradePhase
    data object SignIn : UpgradePhase

    /** 13 최대 레벨 · 17 최하위 등급 · 18 대상 사용 불가(예전 신발 포함) */
    data class Blocked(val block: ForgeTargetBlock) : UpgradePhase

    /** 01 · 03 · 08 — 재료를 고르는 중 */
    data object Editing : UpgradePhase

    /** 05 */
    data object Running : UpgradePhase

    /** 06 — [before] 는 실행한 견적의 값(없으면 null) */
    data class Succeeded(val result: com.stepup.android.domain.ForgeResult, val before: com.stepup.android.domain.ForgeStats?) : UpgradePhase

    /** 07 */
    data class Failed(val result: com.stepup.android.domain.ForgeResult) : UpgradePhase

    /** 15 */
    data class Unknown(val pending: ForgePending) : UpgradePhase
}

/** 본문 위 시트 — 한 번에 하나(재료 시트 위에 다른 재료 시트를 겹치지 않는다) */
sealed interface UpgradeSheet {
    data object None : UpgradeSheet

    /** 02 · 09 · 10 · 11 */
    data object Picker : UpgradeSheet

    /** 04 */
    data class Confirm(val quote: ForgeQuote) : UpgradeSheet

    /** 12 — 쓸 수 없게 된 재료(이미 선택에서 뺐다) */
    data class Changed(val removed: List<ForgeMaterial>) : UpgradeSheet

    /** 14 — 요청을 보내지 않았다 */
    data object Offline : UpgradeSheet
}

data class ShoeUpgradeState(
    val targetId: Long = 0L,
    /** 폰 목록의 대상 — 그림 · 이름. 서버 값과 다르면 서버 값([info])을 믿는다 */
    val shoe: Sneaker? = null,
    val info: com.stepup.android.domain.ForgeTarget? = null,
    val candidates: MaterialsLoad = MaterialsLoad.Loading,
    /** 지금까지 본 후보 — 적용된 선택이 후보를 다시 읽지 못해도 슬롯에 남게 */
    val known: Map<Long, ForgeMaterial> = emptyMap(),
    val selection: MaterialSelection = MaterialSelection(),
    val phase: UpgradePhase = UpgradePhase.Loading,
    val sheet: UpgradeSheet = UpgradeSheet.None,
    /** 05 · 06 · 07 · 15 가 보여 주는 재료(보낸 그대로) */
    val running: List<ForgeMaterial> = emptyList(),
    val quote: ForgeQuote? = null,
    val quoting: Boolean = false,
    val checking: Boolean = false,
) {
    /** 메인 슬롯 — 적용된 소유 id 순서 그대로 */
    val applied: List<ForgeMaterial> get() = selection.applied.mapNotNull { known[it] }
}
