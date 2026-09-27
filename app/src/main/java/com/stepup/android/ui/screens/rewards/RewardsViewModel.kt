package com.stepup.android.ui.screens.rewards

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.stepup.android.BuildConfig
import com.stepup.android.core.ServiceLocator
import com.stepup.android.core.WalletPage
import com.stepup.android.data.local.RewardEntity
import com.stepup.android.data.local.RewardTotals
import com.stepup.android.data.remote.ServerResult
import com.stepup.android.data.remote.TokenResult
import com.stepup.android.data.repo.RewardRepository
import com.stepup.android.domain.DrawStatus
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/** 이용 내역 거르개 — 원장 전체에서(처음 읽은 한 쪽이 아니라) */
enum class LedgerFilter(val kind: Int) { ALL(0), EARNED(1), SPENT(2) }

/** 잔액 · 누계 — 확인하지 못한 숫자를 0 으로 보이지 않는다 */
sealed interface TotalsLoad {
    data object Loading : TotalsLoad
    data class Ready(val totals: RewardTotals) : TotalsLoad
    data object Failed : TotalsLoad
}

/** 이용 내역 — 한 쪽씩 늘린다 */
sealed interface HistoryLoad {
    data object Loading : HistoryLoad

    /**
     * @param more 더 오래된 줄이 있을 수 있다(한 쪽을 꽉 채웠다)
     * @param pageFailed 다음 쪽을 읽지 못했다 — 읽은 줄은 그대로(16)
     * @param stale 다시 읽기가 실패해 앞서 확인한 내역을 보이는 중
     */
    data class Ready(
        val filter: LedgerFilter,
        val rows: List<RewardEntity>,
        val more: Boolean,
        val pageFailed: Boolean = false,
        val stale: Boolean = false,
    ) : HistoryLoad

    data object Failed : HistoryLoad
}

/** WEB3 지갑 줄 — 실제로 제공하는 기능과 서버가 아는 연결 상태만 */
enum class Web3State {
    /** 연결 상태를 읽는 중 */
    Checking,

    /** 이 빌드에 연결 기능(웹 지갑 페이지)이 없다 — 12 */
    Unavailable,

    /** 로그인 전 — 연결은 로그인한 계정에만 */
    SignedOut,
    NotLinked,
    Linked,

    /** 읽지 못했다 — 연결 안 됨으로 단정하지 않는다 */
    Unknown,
}

/**
 * 지갑(지갑 v1, 2026-09-28) — 잔액 · 누계는 원장 전체(기존 정의 그대로), 이용 내역은 원장 전체에서 거른 한 쪽씩,
 * WEB3 줄은 서버의 실제 연결(draw_status)만. 읽기는 적립 · 차감과 무관하다 — 다시 불러와도 원장에 쓰지 않는다.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class RewardsViewModel(private val rewardRepository: RewardRepository) : ViewModel() {
    private val attempt = MutableStateFlow(0)

    val totals: StateFlow<TotalsLoad> = attempt.flatMapLatest {
        rewardRepository.totals.map<RewardTotals, TotalsLoad> { TotalsLoad.Ready(it) }.catch { emit(TotalsLoad.Failed) }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), TotalsLoad.Loading)

    private val _filter = MutableStateFlow(LedgerFilter.ALL)
    val filter: StateFlow<LedgerFilter> = _filter.asStateFlow()
    private val limit = MutableStateFlow(PAGE)
    private val historyAttempt = MutableStateFlow(0)
    private var lastReady: HistoryLoad.Ready? = null

    val history: StateFlow<HistoryLoad> = combine(_filter, historyAttempt, attempt) { f, _, _ -> f }.flatMapLatest { f ->
        limit.flatMapLatest { n ->
            rewardRepository.ledgerPage(f.kind, n).map<List<RewardEntity>, HistoryLoad> { rows ->
                HistoryLoad.Ready(f, rows, more = rows.size >= n)
            }.catch {
                val before = lastReady?.takeIf { it.filter == f }
                emit(
                    when {
                        before == null -> HistoryLoad.Failed
                        // 다음 쪽을 읽다 실패 — 읽은 줄과 자리는 그대로
                        n > before.rows.size -> before.copy(pageFailed = true)
                        else -> before.copy(stale = true)
                    },
                )
            }
        }.onStart { emit(lastReady?.takeIf { it.filter == f } ?: HistoryLoad.Loading) }
    }
        .map { if (it is HistoryLoad.Ready && !it.pageFailed && !it.stale) it.also { ready -> lastReady = ready } else it }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), HistoryLoad.Loading)

    /** 연결 기능이 이 빌드에 있는가 — 웹 지갑 페이지 주소가 있을 때만 */
    val walletPageAvailable: Boolean = BuildConfig.WALLET_URL.isNotBlank()

    private val _web3 = MutableStateFlow(if (walletPageAvailable) Web3State.Checking else Web3State.Unavailable)
    val web3: StateFlow<Web3State> = _web3.asStateFlow()

    /** 연결 혜택 안내의 숫자(첫 연결 선물 · 러닝 몇 km 마다) — 서버가 준 값만 */
    private val _drawStatus = MutableStateFlow<DrawStatus?>(null)
    val drawStatus: StateFlow<DrawStatus?> = _drawStatus.asStateFlow()

    init {
        ServiceLocator.refreshEconomyInBackground()
    }

    fun setFilter(next: LedgerFilter) {
        if (next == _filter.value) return
        limit.value = PAGE
        _filter.value = next
    }

    fun loadMore() {
        val ready = history.value as? HistoryLoad.Ready ?: return
        if (ready.more && !ready.pageFailed && limit.value <= ready.rows.size) limit.value = ready.rows.size + PAGE
    }

    /** 내역만 다시(11 · 16) — 쪽 수는 그대로 */
    fun reloadHistory() {
        historyAttempt.value++
    }

    /** 전체를 다시(10) */
    fun reload() {
        attempt.value++
        refreshWeb3()
    }

    /** 연결 상태를 읽는다 — 화면에 돌아올 때마다(웹 지갑 페이지에서 연결하고 돌아오면 바로 바뀌게) */
    fun refreshWeb3() {
        if (!walletPageAvailable) {
            _web3.value = Web3State.Unavailable
            return
        }
        viewModelScope.launch {
            if (_web3.value != Web3State.Linked && _web3.value != Web3State.NotLinked) _web3.value = Web3State.Checking
            _web3.value = try {
                if (!ServiceLocator.sessionHolder.isSignedIn()) {
                    Web3State.SignedOut
                } else {
                    when (val status = ServiceLocator.economyApi.drawStatus()) {
                        is ServerResult.Ok -> {
                            _drawStatus.value = status.value
                            if (status.value.walletLinked) Web3State.Linked else Web3State.NotLinked
                        }
                        else -> Web3State.Unknown
                    }
                }
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (_: Exception) {
                Web3State.Unknown
            }
        }
    }

    companion object {
        const val PAGE = 30
        val Factory = viewModelFactory {
            initializer {
                RewardsViewModel(ServiceLocator.rewardRepository)
            }
        }
    }
}

/**
 * 지금 로그인한 계정으로 웹 지갑 페이지를 여는 주소 — 지갑 화면과 뽑기(상급 · 지갑 연결)가 함께 쓴다.
 * 페이지에서 인증 · 서명 · 확정 대기까지 하는 동안 만료되지 않게, 15분 넘게 남은 토큰을 넘긴다.
 * 새로 받기가 잠깐 실패하면(연결) 지금 토큰이 아직 쓸 만할 때 그것으로 연다.
 */
suspend fun openWalletPageLink(): WalletPageLink =
    when (val token = ServiceLocator.sessionHolder.accessToken(minValiditySeconds = 15 * 60)
        .let { if (it is TokenResult.Unavailable) ServiceLocator.sessionHolder.accessToken() else it }) {
        is TokenResult.Ok -> WalletPage.url(BuildConfig.WALLET_URL, token.accessToken)
            ?.let { WalletPageLink.Open(it) } ?: WalletPageLink.Offline
        is TokenResult.SignInRequired -> WalletPageLink.SignIn
        is TokenResult.Unavailable -> WalletPageLink.Offline
    }

sealed interface WalletPageLink {
    data class Open(val url: String) : WalletPageLink
    data object SignIn : WalletPageLink
    data object Offline : WalletPageLink
}
