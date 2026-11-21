package com.stepup.android.ui.screens.items

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.stepup.android.core.ServiceLocator
import com.stepup.android.data.repo.ForgeLoad
import com.stepup.android.data.repo.ForgeOutcome
import com.stepup.android.data.repo.ForgeRepository
import com.stepup.android.domain.ForgeBar
import com.stepup.android.domain.ForgeKind
import com.stepup.android.domain.ForgeMaterial
import com.stepup.android.domain.ForgeMaterialBlock
import com.stepup.android.domain.ForgePending
import com.stepup.android.domain.ForgePrecious
import com.stepup.android.domain.ForgeQuote
import com.stepup.android.domain.ForgeStats
import com.stepup.android.domain.ForgeTargetBlock
import com.stepup.android.domain.MaterialSelection
import com.stepup.android.domain.Sneaker
import com.stepup.android.domain.XpCatalog
import com.stepup.android.domain.XpMaterial
import com.stepup.android.domain.XpQuote
import com.stepup.android.domain.XpResult
import com.stepup.android.domain.XpSelection
import com.stepup.android.domain.bulkPick
import com.stepup.android.domain.forPicker
import com.stepup.android.domain.preciousPicked
import com.stepup.android.ui.experience.ExperienceEvents
import com.stepup.android.ui.screens.gacha.origin as drawOrigin
import com.stepup.android.ui.experience.FeedbackCue
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/**
 * 신발 강화 화면의 동작. 화면은 [ShoeUpgradeState] 만 그린다.
 *
 * 경험치 강화(서버 0118, docs/forge-xp-daily-spec-2026-11-19.md 2장 · 5.1)가 먼저다 — 대상의 통(지금 → 넣은 뒤) · 재료 목록(값 낮은 것부터) ·
 * "남는 신발 넣기" · 주 버튼 "경험치 넣기 / 레벨업" 하나. 아까운 재료가 있으면 넣기 전에 시트로 한 번 더 묻는다. 실패가 없다.
 * 서버에 0118 이 없으면 예전 확률 강화(파란 톤 v4 강화 전달본 UP01 ~ UP15 · UP18, 재료 3켤레 · forge-v2)가 그대로 돈다 —
 * 1.20.x 에서 보낸 예전 요청의 결과(성공 · 실패)도 예전 화면으로 보인다.
 *
 * 대상 · 재료 · 값 · %는 서버가 정한다. 달리는 동안([runActive])은 강화하지 않는다. 아직 올리지 않은 러닝에 신은 신발 · 열어 보지 않은
 * 뽑기 신발([appBlocks])은 서버가 모르므로 앱이 재료에서 막는다. 실행은 [ForgeRepository] 하나만 부르고, 보낸 동안은 다시 보내지 않는다.
 */
class ShoeUpgradeViewModel(
    private val requestedId: Long,
    private val forge: ForgeRepository,
    private val localShoes: Flow<List<Sneaker>>,
    /** 뽑기 결과의 "재료로 쓰면" 줄에서 왔다 — 그 신발을 골라 둔다(쓸 수 있을 때만). 0 이면 없음 */
    private val preselect: Long = 0L,
    private val nicknames: Flow<Map<Long, String>> = flowOf(emptyMap()),
    private val runActive: Flow<Boolean> = flowOf(false),
    private val appBlocks: Flow<Map<Long, ForgeMaterialBlock>> = flowOf(emptyMap()),
    /** 재료를 바꾼 뒤 견적을 묻기까지 — 연달아 누르는 동안 묻지 않게 */
    private val quoteDelayMs: Long = 250L,
) : ViewModel() {

    private val _state = MutableStateFlow(ShoeUpgradeState())
    val state: StateFlow<ShoeUpgradeState> = _state.asStateFlow()

    /** 지금 보내는 중(견적 · 실행 · 결과 조회) — 두 번 누르기 막기 */
    private var busy = false

    /** 서버가 준 그대로의 재료 목록 — 앱의 까닭은 [publishXp] 가 붙인다 */
    private var xpRaw: XpCatalog? = null
    private var blocksNow: Map<Long, ForgeMaterialBlock> = emptyMap()
    private var runningNow = false
    private var preselectDone = preselect <= 0L
    private var quoteJob: Job? = null

    init {
        viewModelScope.launch { nicknames.collect { names -> _state.update { it.copy(nicknames = names) } } }
        viewModelScope.launch {
            appBlocks.distinctUntilChanged().collect { blocks ->
                blocksNow = blocks
                if (publishXp()) requote()
            }
        }
        viewModelScope.launch { runActive.distinctUntilChanged().collect(::onRunActive) }
        viewModelScope.launch { open() }
    }

    private suspend fun open() {
        // 결과를 아직 모르는 요청이 있으면 새로 강화하지 않고 그 요청의 결과부터 본다(어느 신발이든)
        val pending = runCatching { forge.pending() }.getOrNull()
        val targetId = pending?.targetId ?: requestedId
        _state.update { it.copy(targetId = targetId, shoe = localShoe(targetId)) }
        reload(targetId)
        if (pending != null) {
            _state.update {
                it.copy(
                    phase = UpgradePhase.Unknown(pending),
                    running = materialsOf(pending.materialIds),
                    xpRunning = xpMaterialsOf(pending.materialIds),
                )
            }
            checkResult()
        }
    }

    private suspend fun localShoe(id: Long): Sneaker? =
        runCatching { localShoes.first().firstOrNull { it.id == id } }.getOrNull()

    private fun materialsOf(ids: List<Long>): List<ForgeMaterial> {
        val known = _state.value.known
        return ids.mapNotNull { known[it] }
    }

    private fun xpMaterialsOf(ids: List<Long>): List<XpMaterial> {
        val catalog = _state.value.xp ?: return emptyList()
        return ids.mapNotNull { catalog.item(it) }
    }

    /** 결과 · 진행 · 미확인 장면은 다시 읽어도 그대로 둔다 */
    private fun UpgradePhase.keeps(): Boolean =
        this is UpgradePhase.Unknown || this is UpgradePhase.Running || this is UpgradePhase.Succeeded ||
            this is UpgradePhase.Failed || this is UpgradePhase.XpDone

    /** 대상과 후보를 다시 읽는다. 읽지 못해도 고른 재료는 지우지 않는다 */
    private suspend fun reload(targetId: Long = _state.value.targetId): Boolean {
        return when (val load = forge.load(targetId)) {
            is ForgeLoad.XpReady -> {
                val local = runCatching { localShoes.first() }.getOrNull().orEmpty().associateBy { it.id }
                // 그림 · 이름 · 번호는 폰의 신발 사본(같은 서버 번호)으로 — 서버 목록에는 번호 · 속성이 없다
                val raw = load.catalog.copy(items = load.catalog.items.map { m -> local[m.id]?.let { m.copy(shoe = it) } ?: m })
                xpRaw = raw
                val catalog = raw.withAppBlocks(blocksNow)
                val shoe = local[targetId]
                // 목록 · 장면을 한 번에 바꾼다(목록 없이 고르기 장면이 한 번 그려지지 않게). 쓸 수 없게 된 재료는 선택에서 뺀다
                _state.update { s ->
                    val gone = s.xpSelection.ids.filter { id -> catalog.item(id)?.usable != true }
                    val selection = s.xpSelection.drop(gone).withMax(catalog.maxMaterials)
                    s.copy(
                        shoe = shoe ?: s.shoe,
                        info = null,
                        xpBlock = null,
                        xp = catalog,
                        xpSelection = selection,
                        xpQuote = s.xpQuote?.takeIf { it.covers(selection.ids) },
                        phase = when {
                            s.phase.keeps() -> s.phase
                            runningNow -> UpgradePhase.RunActive
                            else -> UpgradePhase.Editing
                        },
                    )
                }
                if (!preselectDone) {
                    preselectDone = true
                    val pick = _state.value.xp?.item(preselect)
                    if (pick != null && pick.usable && preselect !in _state.value.xpSelection.ids) {
                        _state.update { it.copy(xpSelection = it.xpSelection.toggle(preselect)) }
                    }
                }
                requote()
                true
            }
            is ForgeLoad.XpBlocked -> {
                xpRaw = null
                _state.update { s ->
                    s.copy(
                        shoe = localShoe(targetId) ?: s.shoe,
                        xp = null,
                        xpBlock = load.block,
                        xpSelection = s.xpSelection.clear(),
                        xpQuote = null,
                        phase = when {
                            s.phase.keeps() -> s.phase
                            runningNow -> UpgradePhase.RunActive
                            else -> UpgradePhase.Blocked(load.block)
                        },
                    )
                }
                true
            }
            is ForgeLoad.Ready -> {
                val blocks = blocksNow
                val materials = load.materials.map { m ->
                    val app = blocks[m.id]
                    if (m.block == null && app != null) m.copy(block = app) else m
                }
                val confirmedGone = _state.value.selection.applied.filter { id ->
                    materials.none { it.id == id && it.usable }
                }
                _state.update { s ->
                    s.copy(
                        info = load.target,
                        xp = null,
                        candidates = MaterialsLoad.Ready(materials.forPicker()),
                        known = s.known + materials.associateBy { it.id },
                        selection = s.selection.drop(confirmedGone),
                        shoe = localShoe(targetId) ?: s.shoe,
                        phase = when {
                            s.phase.keeps() -> s.phase
                            runningNow -> UpgradePhase.RunActive
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

    /** 서버 목록에 앱의 까닭을 붙여 보인다. 쓸 수 없게 된 재료는 선택에서 뺀다(서버 · 앱이 확인한 것만). 선택이 바뀌었으면 true */
    private fun publishXp(): Boolean {
        val raw = xpRaw ?: return false
        val catalog = raw.withAppBlocks(blocksNow)
        val before = _state.value.xpSelection.ids
        _state.update { s ->
            val gone = s.xpSelection.ids.filter { id -> catalog.item(id)?.usable != true }
            val selection = s.xpSelection.drop(gone).withMax(catalog.maxMaterials)
            s.copy(
                xp = catalog,
                xpSelection = selection,
                xpQuote = s.xpQuote?.takeIf { it.covers(selection.ids) },
            )
        }
        return _state.value.xpSelection.ids != before
    }

    private fun onRunActive(active: Boolean) {
        runningNow = active
        val phase = _state.value.phase
        if (active) {
            if (!phase.keeps()) {
                quoteJob?.cancel()
                _state.update { it.copy(phase = UpgradePhase.RunActive, sheet = UpgradeSheet.None, quoting = false) }
            }
        } else if (phase is UpgradePhase.RunActive) {
            _state.update { it.copy(phase = UpgradePhase.Loading) }
            viewModelScope.launch { reload() }
        }
    }

    fun retryLoad() {
        _state.update { it.copy(phase = UpgradePhase.Loading) }
        viewModelScope.launch { reload() }
    }

    // ── 경험치 강화 — 재료 고르기 · 견적 ─────────────────────────

    /** 재료 한 줄을 누른다 — 쓸 수 있는 줄만 고른다(고른 줄은 늘 뺄 수 있다). 상한이면 그대로 */
    fun xpToggle(id: Long) {
        val s = _state.value
        if (s.phase !is UpgradePhase.Editing || busy) return
        val catalog = s.xp ?: return
        val selected = id in s.xpSelection.ids
        if (!selected && catalog.item(id)?.usable != true) return
        if (!selected && s.xpSelection.full) return
        ExperienceEvents.emit(FeedbackCue.Select)
        _state.update { it.copy(xpSelection = it.xpSelection.toggle(id), notice = null) }
        requote()
    }

    /** "남는 신발 넣기" — 아까운 · 막힌 신발은 고르지 않고 값이 낮은 것부터 통이 찰 때까지(지금 고른 것은 그대로) */
    fun xpBulk() {
        val s = _state.value
        if (s.phase !is UpgradePhase.Editing || busy) return
        val catalog = s.xp ?: return
        val picked = catalog.bulkPick(s.xpSelection.ids, s.nicknames)
        if (picked == s.xpSelection.ids) return
        ExperienceEvents.emit(FeedbackCue.Select)
        _state.update { it.copy(xpSelection = it.xpSelection.copy(ids = picked).withMax(catalog.maxMaterials), notice = null) }
        requote()
    }

    /** 고른 재료의 견적을 다시 묻는다 — 바로 뒤에 또 바꾸면 앞 질문은 버린다 */
    private fun requote(immediate: Boolean = false) {
        quoteJob?.cancel()
        val s = _state.value
        val ids = s.xpSelection.ids
        if (s.xp == null || ids.isEmpty()) {
            _state.update { it.copy(xpQuote = null, xpQuoteFailed = false, quoting = false) }
            return
        }
        if (s.xpQuote?.covers(ids) == true && !s.xpQuoteFailed) return
        _state.update { it.copy(quoting = true, xpQuoteFailed = false) }
        quoteJob = viewModelScope.launch {
            if (!immediate) delay(quoteDelayMs)
            val outcome = try {
                forge.xpQuote(_state.value.targetId, ids)
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (_: Exception) {
                ForgeOutcome.NotSent
            }
            // 그사이 재료를 바꿨으면 이 답은 버린다(새 질문이 간다)
            if (_state.value.xpSelection.ids != ids) return@launch
            handleQuote(outcome)
        }
    }

    private suspend fun handleQuote(outcome: ForgeOutcome) {
        when (outcome) {
            is ForgeOutcome.XpQuoted -> _state.update { it.copy(xpQuote = outcome.quote, quoting = false, xpQuoteFailed = false) }
            is ForgeOutcome.MaterialsUnavailable -> dropUnavailable(outcome.ids, outcome.blocks, XpChange.UNAVAILABLE)
            is ForgeOutcome.NoValue -> dropUnavailable(outcome.ids, outcome.ids.associateWith { ForgeMaterialBlock.NO_VALUE }, XpChange.NO_VALUE)
            is ForgeOutcome.TargetBlocked -> _state.update {
                it.copy(phase = UpgradePhase.Blocked(outcome.block), quoting = false, xpSelection = it.xpSelection.clear(), xpQuote = null)
            }
            ForgeOutcome.SignInRequired -> _state.update { it.copy(phase = UpgradePhase.SignIn, quoting = false) }
            ForgeOutcome.MaterialsInvalid -> _state.update {
                it.copy(quoting = false, xpQuote = null, xpSelection = it.xpSelection.clear(), sheet = UpgradeSheet.Rejected(REJECT_INVALID))
            }
            is ForgeOutcome.Rejected -> _state.update {
                it.copy(quoting = false, xpQuoteFailed = true, sheet = UpgradeSheet.Rejected(outcome.reason))
            }
            // 끊김 · 그 밖 — 견적이 없다(주 버튼이 "다시 확인")
            else -> _state.update { it.copy(quoting = false, xpQuoteFailed = true) }
        }
    }

    /** 서버가 쓸 수 없다고 확인한 재료만 뺀다 — 시트로 무엇이 왜 빠졌는지 알리고 목록을 다시 읽는다 */
    private suspend fun dropUnavailable(ids: List<Long>, blocks: Map<Long, ForgeMaterialBlock>, kind: XpChange) {
        val catalog = _state.value.xp
        val removed = ids.mapNotNull { id ->
            catalog?.item(id)?.let { m -> m.copy(block = blocks[id] ?: m.block ?: ForgeMaterialBlock.UNKNOWN) }
        }
        _state.update {
            it.copy(
                phase = if (it.phase is UpgradePhase.Running) UpgradePhase.Editing else it.phase,
                quoting = false,
                xpQuote = null,
                xpSelection = it.xpSelection.drop(ids),
                sheet = UpgradeSheet.XpChanged(removed, kind),
            )
        }
        reload()
    }

    /**
     * 주 버튼 "경험치 넣기 / 레벨업". 견적이 없거나 실패했으면 다시 묻고, 아까운 재료가 있으면 까닭을 적은 시트로 한 번 더 묻는다.
     * 그 밖에는 바로 넣는다(주 버튼은 하나 — 확인은 시트).
     */
    fun xpPrimary() {
        val s = _state.value
        if (busy || s.phase !is UpgradePhase.Editing || s.xpSelection.isEmpty) return
        val catalog = s.xp ?: return
        val quote = s.xpQuote?.takeIf { it.covers(s.xpSelection.ids) && !s.xpQuoteFailed }
        if (quote == null) {
            if (!s.quoting) requote(immediate = true)
            return
        }
        val precious = catalog.preciousPicked(s.xpSelection.ids, s.nicknames)
        if (precious.isNotEmpty()) {
            _state.update { it.copy(sheet = UpgradeSheet.Precious(quote, precious)) }
        } else {
            xpStart(quote)
        }
    }

    /** 아까운 재료 시트의 "그래도 넣기" */
    fun xpConfirm() {
        val sheet = _state.value.sheet as? UpgradeSheet.Precious ?: return
        _state.update { it.copy(sheet = UpgradeSheet.None) }
        xpStart(sheet.quote)
    }

    private fun xpStart(quote: XpQuote) {
        val s = _state.value
        if (busy || runningNow) return
        busy = true
        quoteJob?.cancel()
        val snapshot = xpMaterialsOf(quote.materialIds)
        _state.update { it.copy(sheet = UpgradeSheet.None, phase = UpgradePhase.Running, xpRunning = snapshot, xpQuote = quote, notice = null) }
        viewModelScope.launch {
            try {
                handle(forge.xpAdd(s.targetId, quote), fromConfirm = true)
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

    fun dismissNotice() {
        _state.update { it.copy(notice = null) }
    }

    // ── 예전 확률 강화 — 재료 선택(02 · 08 · 09 · 10 · 11) ─────────

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

    // ── 예전 확률 강화 — 실행(03 → 04 → 05 → 06 · 07 · 12 · 14 · 15) ──

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
        if (busy || runningNow) return
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

    /** 15 결과 다시 확인 — 같은 요청 키의 결과만 묻는다(경험치 강화는 서버에 기록이 없으면 같은 키로 한 번 더 보낸다) */
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
            is ForgeOutcome.XpDone -> {
                ExperienceEvents.emit(FeedbackCue.Success)
                val s = _state.value
                val target = s.xp?.target?.takeIf { it.id == outcome.result.targetId }
                val stats = targetStats(s.shoe, outcome.result.levelBefore, target?.maxLevel)
                val bar = target?.bar
                _state.update {
                    it.copy(
                        phase = UpgradePhase.XpDone(outcome.result, bar, stats),
                        xpRunning = xpMaterialsOf(outcome.result.materialIds).ifEmpty { it.xpRunning },
                        xpSelection = it.xpSelection.clear(),
                        xpQuote = null,
                        notice = null,
                    )
                }
                reload()
                _state.update { it.copy(shoe = localShoe(it.targetId) ?: it.shoe) }
            }
            is ForgeOutcome.XpQuoteChanged -> {
                // 넣지 않았다 — 새 견적을 보이고 사용자가 다시 누른다
                _state.update {
                    it.copy(
                        phase = UpgradePhase.Editing,
                        xpQuote = outcome.quote?.takeIf { q -> q.covers(it.xpSelection.ids) },
                        notice = UpgradeNotice.QuoteChanged,
                        sheet = UpgradeSheet.None,
                    )
                }
                reload()
            }
            is ForgeOutcome.XpQuoted -> handleQuote(outcome)
            is ForgeOutcome.NoValue ->
                dropUnavailable(outcome.ids, outcome.ids.associateWith { ForgeMaterialBlock.NO_VALUE }, XpChange.NO_VALUE)
            ForgeOutcome.MaterialsInvalid -> _state.update {
                it.copy(phase = UpgradePhase.Editing, xpQuote = null, xpSelection = it.xpSelection.clear(),
                    sheet = UpgradeSheet.Rejected(REJECT_INVALID))
            }
            ForgeOutcome.KeyReused -> {
                _state.update { it.copy(phase = UpgradePhase.Editing, xpQuote = null, sheet = UpgradeSheet.Rejected(REJECT_KEY_REUSED)) }
                reload()
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
                if (_state.value.xp != null) {
                    dropUnavailable(outcome.ids, outcome.blocks, XpChange.UNAVAILABLE)
                    return
                }
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
                it.copy(phase = UpgradePhase.Blocked(outcome.block), sheet = UpgradeSheet.None, selection = MaterialSelection(),
                    xpSelection = it.xpSelection.clear(), xpQuote = null)
            }
            ForgeOutcome.SignInRequired -> _state.update {
                it.copy(phase = if (fromConfirm) UpgradePhase.SignIn else it.phase.takeUnless { p -> p is UpgradePhase.Running } ?: UpgradePhase.Editing,
                    sheet = UpgradeSheet.None)
            }
            // 서버가 거절했다 — 연결 문제가 아니다(코드마다 글). 재료는 그대로다
            is ForgeOutcome.Rejected -> _state.update {
                it.copy(phase = UpgradePhase.Editing, sheet = UpgradeSheet.Rejected(outcome.reason))
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

    /** 06 다음 레벨 강화 · 07 재료 다시 선택 · 경험치 "더 넣기" — 빈 선택으로 새로 시작(태운 재료를 다시 채우지 않는다) */
    fun again() {
        quoteJob?.cancel()
        _state.update {
            it.copy(
                selection = MaterialSelection(), quote = null, running = emptyList(), sheet = UpgradeSheet.None, phase = UpgradePhase.Loading,
                xpSelection = it.xpSelection.clear(), xpQuote = null, xpRunning = emptyList(), notice = null, quoting = false,
            )
        }
        viewModelScope.launch { reload() }
    }

    companion object {
        /** 화면이 코드마다 글을 고르는 앱 쪽 거절 코드 */
        const val REJECT_INVALID = "MATERIALS_INVALID"
        const val REJECT_KEY_REUSED = "KEY_REUSED"

        fun factory(id: Long, material: Long = 0L) = viewModelFactory {
            initializer {
                ShoeUpgradeViewModel(
                    requestedId = id,
                    forge = ServiceLocator.forgeRepository,
                    localShoes = ServiceLocator.sneakerRepository.inventory,
                    preselect = material,
                    nicknames = ServiceLocator.userPrefs.shoeNicknames,
                    runActive = com.stepup.android.service.WalkSessionService.state.map { it.isActive },
                    appBlocks = appMaterialBlocks(),
                )
            }
        }

        /**
         * 서버가 모르는 재료 막힘(spec 2.5) — 아직 올리지 않은 러닝에 신은 신발(출발 전 점검의 신발 닳음과 같은 출처 [PendingShoeWear]) ·
         * 열어 보지 않은 뽑기 신발(뽑기 화면의 결과 확인 표시 — 같은 계정 · 하루 안).
         */
        private fun appMaterialBlocks(): Flow<Map<Long, ForgeMaterialBlock>> {
            val runs = combine(
                ServiceLocator.stepRepository.recentSessions(com.stepup.android.ui.screens.walk.PENDING_WEAR_SCAN),
                ServiceLocator.economySync.syncedFrom,
            ) { rows, syncedFrom ->
                com.stepup.android.ui.screens.walk.pendingShoeWearByShoe(rows, syncedFrom, System.currentTimeMillis()).byShoe.keys
            }
            val drawMemory = runCatching { com.stepup.android.ui.screens.gacha.PrefsDrawMemory(ServiceLocator.appContext) }.getOrNull()
            return combine(runs, ServiceLocator.sneakerRepository.inventory, ServiceLocator.userPrefs.economyOwnerFlow) { pendingRun, shoes, owner ->
                val unrevealed = drawMemory?.pending()?.let { unrevealedDrawShoes(it, shoes, owner, System.currentTimeMillis()) }.orEmpty()
                buildMap {
                    unrevealed.forEach { put(it, ForgeMaterialBlock.UNREVEALED) }
                    pendingRun.forEach { put(it, ForgeMaterialBlock.PENDING_RUN) }
                }
            }
        }
    }
}

/**
 * 아직 열어 보지 않은 뽑기 신발 — 뽑기 화면의 결과 확인 표시([com.stepup.android.ui.screens.gacha.PendingDraw])가 이 계정 · 하루 안이면:
 * 서버가 번호를 이미 돌려줬으면 그 신발, 아니면 보내기 직전 가장 큰 번호보다 큰 그 종류(출처)의 신발.
 */
internal fun unrevealedDrawShoes(
    pending: com.stepup.android.ui.screens.gacha.PendingDraw,
    shoes: List<Sneaker>,
    owner: String?,
    now: Long,
): Set<Long> {
    if (pending.account != null && owner != null && pending.account != owner) return emptySet()
    if (now - pending.startedAt > UNREVEALED_WINDOW_MS) return emptySet()
    pending.shoeId?.let { id -> return if (shoes.any { it.id == id }) setOf(id) else emptySet() }
    val origin = pending.kind.drawOrigin()
    return shoes.filter { it.id > pending.newestBefore && it.server?.origin == origin }.map { it.id }.toSet()
}

/** 뽑기 화면이 결과 확인 표시를 믿는 시간과 같다(하루) */
private const val UNREVEALED_WINDOW_MS = 24L * 60 * 60 * 1000

/** 대상의 지금 능력치 — 폰의 신발 사본(서버 값). 레벨 · 상한은 서버 목록의 값 */
internal fun targetStats(shoe: Sneaker?, level: Int?, maxLevel: Int?): ForgeStats? {
    shoe ?: return null
    val server = shoe.server
    return ForgeStats(
        level = level ?: shoe.level,
        maxLevel = maxLevel ?: server?.maxLevel?.takeIf { it > 0 } ?: shoe.rarity.maxLevel,
        efficiencyBps = server?.efficiencyBps ?: 0,
        comfortBps = server?.comfortBps ?: 0,
        pointsTotal = server?.points?.total ?: 0,
        pointsFree = server?.points?.free ?: 0,
    )
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

    /** UP13 최대 레벨 · UP18 대상 사용 불가(예전 신발 포함) · 크루 러닝 중(서버 RUNNING) */
    data class Blocked(val block: ForgeTargetBlock) : UpgradePhase

    /** 이 폰에서 달리는 중 — 러닝을 마친 뒤에 강화한다 */
    data object RunActive : UpgradePhase

    /** 01 · 03 · 08 — 재료를 고르는 중(경험치 강화도 같다) */
    data object Editing : UpgradePhase

    /** 05 */
    data object Running : UpgradePhase

    /** 06 — [before] 는 실행한 견적의 값(없으면 null) */
    data class Succeeded(val result: com.stepup.android.domain.ForgeResult, val before: ForgeStats?) : UpgradePhase

    /** 07 */
    data class Failed(val result: com.stepup.android.domain.ForgeResult) : UpgradePhase

    /** 경험치를 넣었다(실패 없음) — [before] 는 넣기 전 통, [beforeStats] 는 넣기 전 능력치(모르면 null) */
    data class XpDone(val result: XpResult, val before: ForgeBar?, val beforeStats: ForgeStats?) : UpgradePhase

    /** 15 */
    data class Unknown(val pending: ForgePending) : UpgradePhase
}

/** 경험치 강화에서 재료가 빠진 까닭 */
enum class XpChange { UNAVAILABLE, NO_VALUE }

/** 본문 위 알림 한 줄 */
enum class UpgradeNotice {
    /** 넣기 직전 값이 바뀌어 넣지 않았다 — 새 값을 보고 다시 누른다 */
    QuoteChanged,
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

    /** 서버가 거절했다 — [code] 마다 글을 고른다(연결 끊김이 아니다) */
    data class Rejected(val code: String) : UpgradeSheet

    /** 경험치 강화 — 아까운 재료를 넣기 전 한 번 더(까닭 · 하루 에너지 · 상한 앞뒤) */
    data class Precious(val quote: XpQuote, val picks: List<Pair<XpMaterial, List<ForgePrecious>>>) : UpgradeSheet

    /** 경험치 강화 — 서버가 쓸 수 없다고 확인해 뺀 재료(까닭과 함께) */
    data class XpChanged(val removed: List<XpMaterial>, val kind: XpChange) : UpgradeSheet
}

data class ShoeUpgradeState(
    val targetId: Long = 0L,
    /** 폰 목록의 대상 — 그림 · 이름. 서버 값과 다르면 서버 값([info] · [xp])을 믿는다 */
    val shoe: Sneaker? = null,
    /** 예전 확률 강화의 대상(서버에 0118 이 없을 때) */
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
    // ── 경험치 강화(0118) ──
    /** 서버 재료 목록(앱의 까닭을 붙였다). 있으면 경험치 강화 화면이다 */
    val xp: XpCatalog? = null,
    /** 경험치 강화 — 대상을 강화할 수 없는 까닭(서버) */
    val xpBlock: ForgeTargetBlock? = null,
    val xpSelection: XpSelection = XpSelection(),
    /** 지금 고른 재료의 견적(최소) — 고른 재료와 맞을 때만 쓴다 */
    val xpQuote: XpQuote? = null,
    /** 견적을 받지 못했다(끊김 · 거절) — 주 버튼이 "다시 확인" */
    val xpQuoteFailed: Boolean = false,
    /** 넣는 중 · 결과 미확인 · 결과 화면의 재료(보낸 그대로) */
    val xpRunning: List<XpMaterial> = emptyList(),
    val nicknames: Map<Long, String> = emptyMap(),
    val notice: UpgradeNotice? = null,
) {
    /** 메인 슬롯 — 적용된 소유 id 순서 그대로 */
    val applied: List<ForgeMaterial> get() = selection.applied.mapNotNull { known[it] }

    /** 경험치 강화 화면인가 — 서버 목록을 받았거나 서버가 대상을 막았다 */
    val xpMode: Boolean get() = xp != null || xpBlock != null

    /** 경험치 강화 — 고른 재료와 맞는 견적(없으면 null) */
    val liveXpQuote: XpQuote? get() = xpQuote?.takeIf { it.covers(xpSelection.ids) && !xpQuoteFailed }
}
