package com.stepup.android.ui.screens.gacha

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.createSavedStateHandle
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.stepup.android.core.ServiceLocator
import com.stepup.android.data.remote.ServerResult
import com.stepup.android.data.repo.EconomyOutcome
import com.stepup.android.data.repo.EconomySyncState
import com.stepup.android.data.repo.toDomain
import com.stepup.android.data.repo.toEconomyOutcome
import com.stepup.android.domain.DrawKind
import com.stepup.android.domain.DrawStatus
import com.stepup.android.domain.Sneaker
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/** 뽑기 화면에 보일 현황 — 로그인 전 · 불러오는 중(22) · 값 · 못 읽음(23). 못 읽은 것을 0회로 보이지 않는다 */
sealed interface DrawScreenState {
    data object SignedOut : DrawScreenState
    data object Loading : DrawScreenState
    data class Ready(val status: DrawStatus) : DrawScreenState
    data object Failed : DrawScreenState
}

/** 지금 보이는 장면(번호는 v2 전달본) — 메인(01 · 02 …) · 결과 확인 중(10) · 상자 열기(11) · 결과(12 · 13 · 14) · 결과 확인 지연(20) */
sealed interface DrawFlow {
    data object Home : DrawFlow
    data class Requesting(val kind: DrawKind) : DrawFlow
    data class Opening(val result: DrawnShoe) : DrawFlow
    data class Result(val result: DrawnShoe) : DrawFlow

    /** 20 — [busy] 면 서버를 다시 읽는 중, [tried] 면 한 번 읽었는데 아직 모른다 */
    data class Checking(val kind: DrawKind, val busy: Boolean, val tried: Boolean = false) : DrawFlow
}

/** 결과를 아직 보지 않은 요청(26) — 메인의 버튼이(두 탭 모두) "결과 확인"이 되고 새 뽑기는 막힌다. [ready] 면 결과를 이미 받았다 */
data class DrawPending(val kind: DrawKind, val ready: Boolean)

/** 한 번 보여 줄 안내 — 시트(19 · 08 · 23) 또는 짧은 알림(18 · 다시 연결) */
sealed interface DrawNotice {
    /** 19 — 뽑기가 시작되지 않았고 기회를 쓰지 않았다(서버가 거절했거나, 보내지 못했거나, 확인해 보니 처리되지 않았다) */
    data class NotStarted(val kind: DrawKind, val stop: DrawStop) : DrawNotice

    /** 08 — 지갑을 연결하고 돌아와 첫 연결 선물 [gift] 회가 서버에 생긴 것을 확인했다 */
    data class Linked(val gift: Int) : DrawNotice

    /** 다시 연결 — 선물은 다시 주지 않는다. 짧은 알림만 */
    data object Relinked : DrawNotice

    /** 18 — 러닝이 반영돼 상급 기회가 [added] 회 늘었다(서버 값) */
    data class RunReward(val added: Int) : DrawNotice

    /** 23 — 횟수를 불러오지 못했다(소진과 다르다) */
    data object LoadFailed : DrawNotice
}

/** 서버에 보낸 뽑기 한 번의 답 */
sealed interface DrawReply {
    /** 뽑혔다 — 서버가 준 새 신발 번호. [shoe] 는 목록을 다시 받아 찾은 그 신발(아직 못 받았으면 null) */
    data class Drawn(val shoeId: Long, val shoe: Sneaker?) : DrawReply

    /** 서버가 거절했다 — 함수가 예외로 끝나 되돌려졌으니 기회를 쓰지 않았다 */
    data class Refused(val outcome: EconomyOutcome) : DrawReply

    /** 답을 받지 못했다 — 끊김 · 시간 초과 · 서버 오류. 처리됐는지 모른다 */
    data object Unknown : DrawReply
}

/** 뽑기가 기대는 것 — 서버 현황 · 한 번 뽑기 · 결과 확인 · 기억. 기기 검사는 흉내 낸 것을 넣는다 */
interface DrawSource {
    /** 서버가 설정돼 있고 로그인했는가 */
    fun ready(): Boolean
    suspend fun status(): ServerResult<DrawStatus>

    /** 보내기 직전의 기준 — 서버에 있는 내 신발 중 가장 큰 번호(없으면 0) */
    suspend fun newestShoeId(): ServerResult<Long>
    suspend fun draw(kind: DrawKind): DrawReply

    /** 결과를 모르는 요청 — 서버 목록을 다시 받아 기준 뒤에 생긴 그 종류의 신발을 찾는다(없으면 Ok(null)) */
    suspend fun findDrawn(pending: PendingDraw): ServerResult<Sneaker?>

    /** 지금 계정 — 남겨 둔 요청 · 본 수가 다른 계정 것이 아닌지 가른다 */
    suspend fun account(): String?
    val memory: DrawMemory
}

/**
 * 신발 뽑기 — 규칙은 v2 전달본(2026-09-28, 번호도 그 전달본), 화면은 26장 디자인(docs/redesign/shoe-draw-v3). 수 · 연결 상태 · 결과 신발은 모두 서버가 정한다.
 *
 * 한 번 누르면 결과를 확인할 때까지 새 뽑기를 받지 않는다(10). 보내기 직전에 서버의 신발 번호를 기준으로 적어 두고(보내지 못하면
 * 기회를 쓰지 않은 것이 확실하다 — 19), 서버가 거절하면 19, 답을 받지 못하면 서버 목록 · 현황을 다시 읽어 확인한다(20).
 * 확인하는 동안과 뒤로 간 뒤(26)에도 새 뽑기는 막고, 앱을 다시 열면 적어 둔 요청부터 확인한다. 결과는 서버가 준 신발만 보인다.
 */
class DrawViewModel(
    private val source: DrawSource,
    private val saved: SavedStateHandle,
    private val clock: () -> Long = System::currentTimeMillis,
    private val settleMillis: Long = SETTLE_MILLIS,
    private val orphanMillis: Long = ORPHAN_MILLIS,
) : ViewModel() {

    private val _state = MutableStateFlow<DrawScreenState>(DrawScreenState.Loading)
    val state: StateFlow<DrawScreenState> = _state.asStateFlow()

    private val _flow = MutableStateFlow<DrawFlow>(DrawFlow.Home)
    val flow: StateFlow<DrawFlow> = _flow.asStateFlow()

    private val _pending = MutableStateFlow<DrawPending?>(null)
    val pending: StateFlow<DrawPending?> = _pending.asStateFlow()

    val notice = MutableStateFlow<DrawNotice?>(null)

    /** 뒤로 간 사이에 받은 결과 — "결과 확인"을 누르면 보인다 */
    private var readyResult: DrawnShoe? = null
    private var sending: Job? = null
    private var checking: Job? = null

    /** 이 화면이 보낸 요청의 답을 받았는가 — 못 받은 채 남은 요청(앞 화면 · 앞 실행)은 답이 아직 오갈 수 있어 더 기다린다 */
    private var replied = false

    private var accountRead = false
    private var accountValue: String? = null

    init {
        restorePending()
        refresh()
    }

    /** 화면에 돌아올 때마다 — 지갑 페이지에서 연결하고 오거나 러닝을 마치고 오면 수가 바뀌어 있다 */
    fun refresh() {
        viewModelScope.launch { load() }
    }

    /** 칸의 버튼 · 내역 시트의 버튼 · 결과의 "한 번 더 뽑기" · 연결 완료의 "상급으로 1회 뽑기" — 그 종류로 새 요청 하나 */
    fun draw(kind: DrawKind) {
        if (sending != null || checking != null || _pending.value != null) return
        val status = (_state.value as? DrawScreenState.Ready)?.status ?: return
        if (!status.canDraw(kind)) return
        readyResult = null
        replied = false
        _pending.value = DrawPending(kind, ready = false)
        _flow.value = DrawFlow.Requesting(kind)
        sending = viewModelScope.launch {
            try {
                send(kind, status.left(kind))
            } finally {
                sending = null
            }
        }
    }

    /** 26 의 "결과 확인" · 20 의 "결과 다시 확인" — 새로 뽑지 않고 남은 요청의 결과만 */
    fun checkPending() {
        val pending = _pending.value ?: return
        if (pending.ready) {
            val result = readyResult ?: return
            readyResult = null
            _pending.value = null
            source.memory.forget()
            _flow.value = DrawFlow.Result(result)
            return
        }
        if (sending != null) {
            // 답을 기다리는 중 — 오면 바로 결과로 간다
            _flow.value = DrawFlow.Checking(pending.kind, busy = true)
            return
        }
        _flow.value = DrawFlow.Checking(pending.kind, busy = true, tried = (_flow.value as? DrawFlow.Checking)?.tried ?: false)
        startCheck()
    }

    /** 10 · 20 에서 뒤로 — 요청은 그대로 두고 메인으로(26) */
    fun leaveFlow() {
        if (_flow.value is DrawFlow.Requesting || _flow.value is DrawFlow.Checking) _flow.value = DrawFlow.Home
    }

    /** 상자 열기가 끝났거나 건너뛰었다 — 이미 확정된 결과로만 */
    fun finishOpening() {
        val opening = _flow.value as? DrawFlow.Opening ?: return
        _flow.value = DrawFlow.Result(opening.result)
    }

    /** 결과에서 뒤로 · "뽑기 화면으로" · "내 신발 보기" — 메인은 최신 수로 */
    fun closeResult() {
        when (val current = _flow.value) {
            is DrawFlow.Opening -> _flow.value = DrawFlow.Result(current.result)
            is DrawFlow.Result -> {
                _flow.value = DrawFlow.Home
                refresh()
            }
            else -> Unit
        }
    }

    /** 지갑 페이지를 열기 직전 — 돌아와서 연결이 확인되면(서버 값이 바뀌면) 08 을 보인다 */
    fun watchWalletLink() {
        val status = (_state.value as? DrawScreenState.Ready)?.status ?: return
        if (status.walletLinked) return
        saved[KEY_WATCH_GIFT] = status.giftLeft
    }

    fun consumeNotice() {
        notice.value = null
    }

    // ── 보내기 · 확인 ────────────────────────────────────────────────

    private suspend fun send(kind: DrawKind, leftBefore: Int) {
        val account = account()
        // 1) 기준 — 보내기 직전 서버의 내 신발 번호. 읽지 못하면 보내지 않는다(기회를 쓰지 않은 것이 확실하다)
        val base = when (val newest = attempt { source.newestShoeId() }) {
            is ServerResult.Ok -> newest.value
            is ServerResult.SignInRequired -> return stopped(kind, DrawStop.SignIn)
            else -> return stopped(kind, DrawStop.Network)
        }
        val marker = PendingDraw(kind, base, leftBefore, clock(), account)
        // 2) 보내기 전에 적는다 — 뒤로 가거나 앱이 죽어도 다시 열면 이 요청부터 확인한다
        source.memory.keep(marker)
        val reply = try {
            source.draw(kind)
        } catch (cancelled: CancellationException) {
            throw cancelled
        } catch (_: Exception) {
            DrawReply.Unknown
        }
        replied = true
        when (reply) {
            is DrawReply.Drawn -> if (reply.shoe != null) {
                delivered(DrawnShoe(kind, reply.shoe))
            } else {
                // 뽑기는 됐다(번호를 받았다) — 목록만 못 받았다. 번호로 다시 찾는다
                source.memory.keep(marker.copy(shoeId = reply.shoeId))
                unresolved(kind)
            }
            is DrawReply.Refused -> {
                source.memory.forget()
                stopped(kind, reply.outcome.toDrawStop())
            }
            DrawReply.Unknown -> unresolved(kind)
        }
    }

    /**
     * 결과를 받았다 — 보고 있으면 상자 열기(11, 20 에서는 바로 결과), 뒤로 갔으면 "결과 확인"으로 남긴다.
     * 보기 전까지는 적어 둔 요청을 그 신발 번호와 함께 남긴다 — 화면을 나갔다 와도 "결과 확인"으로 이어진다.
     */
    private suspend fun delivered(result: DrawnShoe, fresh: DrawStatus? = null) {
        when (_flow.value) {
            is DrawFlow.Requesting -> {
                _pending.value = null
                source.memory.forget()
                _flow.value = DrawFlow.Opening(result)
            }
            is DrawFlow.Checking -> {
                _pending.value = null
                source.memory.forget()
                _flow.value = DrawFlow.Result(result)
            }
            else -> {
                source.memory.pending()?.let { source.memory.keep(it.copy(shoeId = result.shoe.id)) }
                readyResult = result
                _pending.value = DrawPending(result.kind, ready = true)
            }
        }
        // 뽑은 뒤의 수 — 결과의 "남은 N회"와 메인. 읽지 못하면 결과에 수를 두지 않는다
        val status = fresh ?: load()
        if (status != null) withLeft(result.shoe.id, status.left(result.kind))
    }

    private fun withLeft(shoeId: Long, left: Int) {
        fun DrawnShoe.fill() = if (shoe.id == shoeId) copy(left = left) else this
        _flow.value = when (val current = _flow.value) {
            is DrawFlow.Opening -> current.copy(result = current.result.fill())
            is DrawFlow.Result -> current.copy(result = current.result.fill())
            else -> current
        }
        readyResult = readyResult?.fill()
    }

    /**
     * 시작하지 못했다(19) — 기회를 쓰지 않은 것이 확실할 때만 온다. 최신 수는 뒤에서 따로 읽는다 — 이 요청은 바로 끝나
     * 안내의 "다시 뽑기"가 버려지지 않는다(새 요청은 서버가 수를 다시 확인한다).
     */
    private fun stopped(kind: DrawKind, stop: DrawStop, fresh: DrawStatus? = null) {
        _pending.value = null
        if (_flow.value is DrawFlow.Requesting || _flow.value is DrawFlow.Checking) _flow.value = DrawFlow.Home
        notice.value = DrawNotice.NotStarted(kind, stop)
        if (fresh == null) refresh()
    }

    /** 답을 받지 못했다(20) — 잠시 뒤 서버 목록 · 현황을 다시 읽어 확인한다. 그동안 새 뽑기는 막는다 */
    private fun unresolved(kind: DrawKind) {
        _pending.value = DrawPending(kind, ready = false)
        if (_flow.value is DrawFlow.Requesting) _flow.value = DrawFlow.Checking(kind, busy = true)
        startCheck()
    }

    private fun startCheck() {
        if (checking != null) return
        val marker = source.memory.pending()
        if (marker == null) {
            // 확인할 요청이 없다 — 막아 둘 까닭이 없다
            _pending.value = null
            if (_flow.value is DrawFlow.Checking) _flow.value = DrawFlow.Home
            return
        }
        checking = viewModelScope.launch {
            try {
                check(marker)
            } finally {
                checking = null
            }
        }
    }

    private suspend fun check(marker: PendingDraw) {
        val kind = marker.kind
        val settle = settleMillis - (clock() - marker.startedAt)
        if (settle > 0) delay(settle)
        while (true) {
            val found = attempt { source.findDrawn(marker) }
            // 목록을 다시 받았을 때만 현황도 읽는다 — 둘 다 서버 값이어야 가를 수 있다
            val status = if (found is ServerResult.Ok) load() else null
            val verdict = if (found is ServerResult.Ok) judgePending(marker, found.value, status) else DrawCheck.Unknown
            val age = clock() - marker.startedAt
            if (verdict == DrawCheck.NotStarted && !replied && age < orphanMillis) {
                // 앞 화면 · 앞 실행이 보낸 요청은 답이 아직 오가는 중일 수 있다 — 넉넉히 기다렸다가 한 번 더 본다
                delay(orphanMillis - age)
                continue
            }
            when (verdict) {
                is DrawCheck.Found -> delivered(DrawnShoe(kind, verdict.shoe), fresh = status)
                DrawCheck.NotStarted -> {
                    source.memory.forget()
                    stopped(kind, DrawStop.Network, fresh = status)
                }
                DrawCheck.Unknown -> {
                    _pending.value = DrawPending(kind, ready = false)
                    (_flow.value as? DrawFlow.Checking)?.let { _flow.value = it.copy(busy = false, tried = true) }
                }
            }
            return
        }
    }

    /** 앞 실행이 남긴 요청 — 같은 계정 · 하루 안의 것만 이어서 확인한다 */
    private fun restorePending() {
        val marker = source.memory.pending() ?: return
        if (!source.ready()) return
        viewModelScope.launch {
            val stale = clock() - marker.startedAt > MAX_PENDING_AGE_MILLIS
            if (stale || marker.account != account()) {
                source.memory.forget()
                return@launch
            }
            _pending.value = DrawPending(marker.kind, ready = false)
            startCheck()
        }
    }

    // ── 현황 ───────────────────────────────────────────────────────

    /** 서버 현황을 읽는다 — 읽었으면 그 값, 못 읽었으면 null(보던 수는 그대로 두고, 처음이면 23) */
    private suspend fun load(): DrawStatus? {
        if (!source.ready()) {
            _state.value = DrawScreenState.SignedOut
            return null
        }
        val before = _state.value
        if (before !is DrawScreenState.Ready) _state.value = DrawScreenState.Loading
        return when (val read = attempt { source.status() }) {
            is ServerResult.Ok -> {
                _state.value = DrawScreenState.Ready(read.value)
                noticeChanges(read.value)
                read.value
            }
            is ServerResult.SignInRequired -> {
                _state.value = DrawScreenState.SignedOut
                null
            }
            else -> {
                // 다시 읽기가 잠깐 실패하면 보던 수를 그대로 둔다. 처음부터 못 읽었으면 23(0 으로 보이지 않는다)
                if (before is DrawScreenState.Ready) {
                    _state.value = before
                } else {
                    _state.value = DrawScreenState.Failed
                    if (notice.value == null) notice.value = DrawNotice.LoadFailed
                }
                null
            }
        }
    }

    /** 서버 값이 바뀐 것을 확인했을 때만 알린다 — 지갑 연결(08), 러닝 반영(18) */
    private suspend fun noticeChanges(status: DrawStatus) {
        if (notice.value == DrawNotice.LoadFailed) notice.value = null
        val watch = saved.get<Int>(KEY_WATCH_GIFT)
        if (watch != null) {
            val gift = linkedGift(LinkWatch(linked = false, giftLeft = watch), status)
            if (gift != null) {
                saved.remove<Int>(KEY_WATCH_GIFT)
                notice.value = if (gift > 0) DrawNotice.Linked(gift) else DrawNotice.Relinked
            }
        }
        val account = account()
        val added = runChancesAdded(source.memory.seenRunChances(account), status)
        if (added > 0 && notice.value == null) notice.value = DrawNotice.RunReward(added)
        source.memory.seeRunChances(account, status.runLeft)
    }

    private suspend fun account(): String? {
        if (!accountRead) {
            accountValue = try { source.account() } catch (c: CancellationException) { throw c } catch (_: Exception) { null }
            accountRead = true
        }
        return accountValue
    }

    private suspend fun <T> attempt(call: suspend () -> ServerResult<T>): ServerResult<T>? = try {
        call()
    } catch (cancelled: CancellationException) {
        throw cancelled
    } catch (_: Exception) {
        null
    }

    companion object {
        private const val KEY_WATCH_GIFT = "draw_link_watch_gift"

        /** 답을 받지 못한 뒤 서버를 다시 읽기까지 — 처리 중이던 요청이 끝날 틈 */
        const val SETTLE_MILLIS = 3_000L

        /** 이 화면이 답을 받지 못한 요청은 연결 · 읽기 시간 초과(20초씩)를 넘길 때까지 "시작 안 됨"이라 하지 않는다 */
        const val ORPHAN_MILLIS = 45_000L

        /** 하루가 지난 요청은 버린다(백업에서 되살아난 기록 등) */
        const val MAX_PENDING_AGE_MILLIS = 24 * 60 * 60 * 1000L

        val Factory = viewModelFactory {
            initializer {
                DrawViewModel(ServiceLocator.drawSource(), createSavedStateHandle())
            }
        }
    }
}

/** 앱의 뽑기 — 서버 경제(EconomyApi)와 신발 사본(EconomySync) */
internal class ServerDrawSource : DrawSource {
    override val memory: DrawMemory = PrefsDrawMemory(ServiceLocator.appContext)

    override fun ready(): Boolean =
        ServiceLocator.economyApi.isConfigured &&
            ServiceLocator.economySync.state.value != EconomySyncState.SIGNED_OUT

    override suspend fun status(): ServerResult<DrawStatus> = ServiceLocator.economyApi.drawStatus()

    override suspend fun newestShoeId(): ServerResult<Long> = when (val read = ServiceLocator.economyApi.sneakers()) {
        is ServerResult.Ok -> ServerResult.Ok(read.value.maxOfOrNull { it.id } ?: 0L)
        is ServerResult.Rejected -> read
        is ServerResult.Retry -> read
        is ServerResult.SignInRequired -> read
    }

    override suspend fun draw(kind: DrawKind): DrawReply {
        val api = ServiceLocator.economyApi
        val result = if (kind == DrawKind.FREE) api.drawFree() else api.drawPremium()
        return when (result) {
            is ServerResult.Ok -> {
                // 서버가 정한 신발 — 목록을 다시 받아 그 번호를 찾는다. 받지 못해도 뽑기는 끝났다(번호로 나중에 찾는다)
                ServiceLocator.economySync.refresh()
                DrawReply.Drawn(result.value, ServiceLocator.database.sneakerDao().byId(result.value)?.toDomain())
            }
            is ServerResult.Rejected -> DrawReply.Refused(result.toEconomyOutcome())
            is ServerResult.SignInRequired -> DrawReply.Refused(EconomyOutcome.SignInRequired)
            is ServerResult.Retry -> DrawReply.Unknown
        }
    }

    override suspend fun findDrawn(pending: PendingDraw): ServerResult<Sneaker?> {
        when (val synced = ServiceLocator.economySync.refresh()) {
            is ServerResult.Ok -> Unit
            is ServerResult.Rejected -> return synced
            is ServerResult.Retry -> return synced
            is ServerResult.SignInRequired -> return synced
        }
        val shoes = ServiceLocator.database.sneakerDao().allNow()
        val hit = if (pending.shoeId != null) {
            shoes.firstOrNull { it.serverId == pending.shoeId }
        } else {
            shoes.filter { it.serverId > pending.newestBefore && it.origin == pending.kind.origin() }.maxByOrNull { it.serverId }
        }
        return ServerResult.Ok(hit?.toDomain())
    }

    override suspend fun account(): String? = ServiceLocator.userPrefs.economyOwner()
}
