package com.stepup.android.ui.screens.market

import androidx.annotation.StringRes
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.stepup.android.R
import com.stepup.android.core.ServiceLocator
import com.stepup.android.data.local.SneakerEntity
import com.stepup.android.data.remote.ModelKey
import com.stepup.android.data.remote.QuoteRow
import com.stepup.android.data.remote.ServerResult
import com.stepup.android.data.repo.MarketOutcome
import com.stepup.android.data.repo.MarketRefusal
import com.stepup.android.data.repo.marketRefusalKm
import com.stepup.android.data.repo.MarketRepository
import com.stepup.android.data.repo.ModelBook
import com.stepup.android.data.repo.MyMarket
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.Job
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive

/**
 * 서버에 못 닿았을 때 화면이 할 말.
 *
 * "오류"라고만 적으면 사용자는 앱을 껐다 켜는 것 말고 할 수 있는 일이
 * 없다. 로그인이 필요한 것인지, 잠깐 안 되는 것인지, 서버가 거절한 것인지는
 * 다음에 할 일이 서로 다르다.
 */
enum class MarketProblem { OFFLINE, SIGN_IN, REJECTED }

/**
 * 한 번 보여 주고 사라지는 말.
 *
 * 서버가 적어 보낸 이유는 그대로([Text]), 앱이 정한 말은 번역이 되도록
 * 문자열 번호로([Res]) 나눈다. 서버 메시지를 앱에서 다시 지어내면 진짜
 * 이유가 가려지고, 앱의 말을 한국어로 박아 두면 다른 언어에서 한국어가 뜬다.
 */
sealed interface MarketMessage {
    data class Text(val value: String) : MarketMessage

    /** @property arg 문구에 넣을 값(남은 km 등). 없으면 null */
    data class Res(@StringRes val id: Int, val arg: String? = null) : MarketMessage
}

/** 시세판 화면의 상태 */
data class MarketBoard(
    val loading: Boolean = true,
    val problem: MarketProblem? = null,
    val quotes: List<QuoteRow> = emptyList(),
    val mine: MyMarket? = null,
    /** 거래에 쓸 수 있는 SUP — 서버 원장의 합 */
    val tradable: Double = 0.0,
)

/** 마켓 안 NFT 탭 */
class MarketViewModel(private val repo: MarketRepository) : ViewModel() {

    val board = MutableStateFlow(MarketBoard())

    val message = MutableStateFlow<MarketMessage?>(null)

    private var refreshJob: Job? = null

    init {
        refresh()
    }

    fun refresh() {
        refreshJob?.cancel()
        refreshJob = viewModelScope.launch {
            board.value = board.value.copy(loading = true, problem = null)

            try {
                // 사는 사람과 파는 사람의 장부를 맞추는 것이 먼저다. 팔린 신발이
                // 아직 보관함에 남아 있는 채로 시세판을 열면, 없는 신발을 팔려고
                // 하게 된다.
                repo.sync()

                when (val quotes = repo.quotes()) {
                    is ServerResult.Ok -> {
                        val mineResult = repo.mine()
                        if (mineResult !is ServerResult.Ok) {
                            currentCoroutineContext().ensureActive()
                            board.value = board.value.copy(loading = false, problem = mineResult.problem())
                            return@launch
                        }
                        val mine = mineResult.value
                        val balanceResult = repo.tradableBalance()
                        if (balanceResult !is ServerResult.Ok) {
                            currentCoroutineContext().ensureActive()
                            board.value = board.value.copy(loading = false, problem = balanceResult.problem())
                            return@launch
                        }
                        val tradable = balanceResult.value
                        currentCoroutineContext().ensureActive()
                        board.value = MarketBoard(
                            loading = false,
                            quotes = quotes.value,
                            mine = mine,
                            tradable = tradable,
                        )
                    }
                    else -> {
                        currentCoroutineContext().ensureActive()
                        board.value = board.value.copy(loading = false, problem = quotes.problem())
                    }
                }
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (_: Exception) {
                currentCoroutineContext().ensureActive()
                board.value = board.value.copy(loading = false, problem = MarketProblem.OFFLINE)
            }
        }
    }

    fun cancelListing(listingId: Long) = act { repo.cancelListing(listingId) }

    fun cancelBid(bidId: Long) = act { repo.cancelBid(bidId) }

    /** 요청이 서버에 가 있는 동안 — 두 번 눌러 두 번째 거절이 첫 결과를 덮지 않게 */
    private var acting = false

    private fun act(block: suspend () -> MarketOutcome) {
        if (acting) return
        acting = true
        viewModelScope.launch {
            try {
                message.value = block().say()
                refresh()
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (_: Exception) {
                // 앱 쪽 문제(읽기 · 저장)로 끝을 보지 못했다 — 앱이 죽지 않게 알리고 서버 값으로 다시 불러온다
                message.value = MarketMessage.Res(R.string.market_msg_unfinished)
                refresh()
            } finally {
                acting = false
            }
        }
    }

    fun consumeMessage() {
        message.value = null
    }

    companion object {
        val Factory = viewModelFactory {
            initializer { MarketViewModel(ServiceLocator.marketRepository) }
        }
    }
}

/** 모델 하나의 장부 화면 상태 */
data class MarketModelState(
    val loading: Boolean = true,
    val problem: MarketProblem? = null,
    val book: ModelBook? = null,
    val quote: QuoteRow? = null,
    val tradable: Double = 0.0,
    /** 이 모델로 내가 갖고 있는 신발 — 팔 것을 고를 때 쓴다 */
    val mySneakers: List<SneakerEntity> = emptyList(),
)

class MarketModelViewModel(private val repo: MarketRepository) : ViewModel() {

    val state = MutableStateFlow(MarketModelState())
    val message = MutableStateFlow<MarketMessage?>(null)

    private var model: ModelKey? = null
    private var loadJob: Job? = null

    fun open(key: ModelKey) {
        if (model == key && state.value.book != null && state.value.problem == null) return
        if (model != key) state.value = MarketModelState()
        model = key
        load()
    }

    fun load() {
        val key = model ?: return
        loadJob?.cancel()
        loadJob = viewModelScope.launch {
            state.value = state.value.copy(loading = true, problem = null)
            try {
                when (val book = repo.book(key)) {
                    is ServerResult.Ok -> {
                        val quotesResult = repo.quotes()
                        val quote = if (quotesResult is ServerResult.Ok) {
                            quotesResult.value.firstOrNull {
                                it.faction == key.faction &&
                                    it.rarity == key.rarity &&
                                    it.variant == key.variant
                            }
                        } else {
                            null
                        }
                        val balanceResult = repo.tradableBalance()
                        if (balanceResult !is ServerResult.Ok) {
                            currentCoroutineContext().ensureActive()
                            state.value = state.value.copy(loading = false, problem = balanceResult.problem())
                            return@launch
                        }
                        val tradable = balanceResult.value
                        currentCoroutineContext().ensureActive()
                        state.value = MarketModelState(
                            loading = false,
                            book = book.value,
                            quote = quote,
                            tradable = tradable,
                            mySneakers = repo.inventory().first().filter {
                                it.factionId == key.faction &&
                                    it.rarity == key.rarity &&
                                    it.variant == key.variant
                            },
                        )
                    }
                    else -> {
                        currentCoroutineContext().ensureActive()
                        state.value = state.value.copy(loading = false, problem = book.problem())
                    }
                }
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (_: Exception) {
                currentCoroutineContext().ensureActive()
                state.value = state.value.copy(loading = false, problem = MarketProblem.OFFLINE)
            }
        }
    }

    fun buy(listingId: Long) = act { repo.buy(listingId) }

    fun sell(localId: Long, bidId: Long) = act { repo.sellInto(localId, bidId) }

    // 바로 체결되지 않고 걸어 두었을 때도 알린다 — 말이 없으면 눌린 건지 알 수 없다
    fun listForSale(localId: Long, price: Double) = act(R.string.market_msg_listed) { repo.listForSale(localId, price) }

    fun bid(minLevel: Int, price: Double) {
        val key = model ?: return
        act(R.string.market_msg_bid_placed) { repo.placeBid(key, minLevel, price) }
    }

    /** 요청이 서버에 가 있는 동안 — 두 번 눌러 두 번 사고, 두 번째 거절이 첫 성공을 덮지 않게 */
    private var acting = false

    /** @param placed 바로 체결되지 않고 걸어 두었을 때(Ok · traded = false) 할 말. null 이면 말없이 다시 불러온다 */
    private fun act(@StringRes placed: Int? = null, block: suspend () -> MarketOutcome) {
        if (acting) return
        acting = true
        viewModelScope.launch {
            try {
                message.value = block().say(placed)
                load()
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (_: Exception) {
                // 앱 쪽 문제(읽기 · 저장)로 끝을 보지 못했다 — 앱이 죽지 않게 알리고 서버 값으로 다시 불러온다
                message.value = MarketMessage.Res(R.string.market_msg_unfinished)
                load()
            } finally {
                acting = false
            }
        }
    }

    fun consumeMessage() {
        message.value = null
    }

    companion object {
        val Factory = viewModelFactory {
            initializer { MarketModelViewModel(ServiceLocator.marketRepository) }
        }
    }
}

// ── 옮기기 ──────────────────────────────────────────────────────────

private fun ServerResult<*>.problem(): MarketProblem = when (this) {
    is ServerResult.SignInRequired -> MarketProblem.SIGN_IN
    is ServerResult.Rejected -> MarketProblem.REJECTED
    else -> MarketProblem.OFFLINE
}

/**
 * 결말을 사용자에게 할 말로.
 *
 * 서버가 적어 보낸 이유를 갈래로 옮겨([MarketRefusal]) 번역된 문구로 보인다. "거래에 실패했습니다"로 덮으면
 * 무엇이 모자랐는지가 사라지고, 한국어 문장을 그대로 띄우면 다른 언어 화면에 한국어가 뜬다.
 *
 * @param placed 걸어 두기만 했을 때(내놓기 · 구매 입찰) 할 말. 거두기는 목록이 바뀌는 것으로 충분해 null
 */
internal fun MarketOutcome.say(@StringRes placed: Int? = null): MarketMessage? = when (this) {
    is MarketOutcome.Ok -> if (traded) MarketMessage.Res(R.string.market_msg_traded) else placed?.let { MarketMessage.Res(it) }
    MarketOutcome.Equipped -> MarketMessage.Res(R.string.market_msg_equipped)
    is MarketOutcome.Failed -> refusalMessage(reason)
    // 통신 글("통신 실패: Read timed out")은 서버의 이유가 아니다 — 응답만 잃었을 수 있어 실패라고 하지 않고,
    // 같은 버튼을 다시 눌러도 두 번 되지 않는다고 알린다(요청 번호, 0062)
    is MarketOutcome.Offline -> MarketMessage.Res(R.string.market_msg_offline)
    MarketOutcome.SignInRequired -> MarketMessage.Res(R.string.market_msg_sign_in)
}

/** 거절 문장 → 번역된 문구. 남은 km 를 못 읽으면 일반 안내로 */
internal fun refusalMessage(reason: String): MarketMessage.Res {
    val refusal = MarketRefusal.of(reason)
    if (refusal == MarketRefusal.RUN_MORE) {
        val km = marketRefusalKm(reason) ?: return MarketMessage.Res(R.string.market_err_other)
        return MarketMessage.Res(R.string.market_err_run_more, km)
    }
    return MarketMessage.Res(refusalRes(refusal))
}

/** 거절 갈래 → 문구 */
@StringRes
internal fun refusalRes(refusal: MarketRefusal): Int = when (refusal) {
    MarketRefusal.PRICE_INVALID -> R.string.market_err_price_invalid
    MarketRefusal.PRICE_TOO_LOW -> R.string.market_err_price_low
    MarketRefusal.NOT_MINE, MarketRefusal.NO_SNEAKER -> R.string.market_err_not_mine
    MarketRefusal.ALREADY_LISTED -> R.string.market_err_already_listed
    MarketRefusal.LISTING_CLOSED -> R.string.market_err_listing_closed
    MarketRefusal.OWN_LISTING -> R.string.market_err_own_listing
    MarketRefusal.NOT_MY_LISTING -> R.string.market_err_not_my_listing
    MarketRefusal.NOT_ENOUGH_SUP -> R.string.market_err_not_enough_sup
    MarketRefusal.BID_CLOSED -> R.string.market_err_bid_closed
    MarketRefusal.OWN_BID -> R.string.market_err_own_bid
    MarketRefusal.NOT_MY_BID -> R.string.market_err_not_my_bid
    MarketRefusal.MODEL_MISMATCH -> R.string.market_err_model_mismatch
    MarketRefusal.LEVEL_TOO_LOW -> R.string.market_err_level_low
    MarketRefusal.IMPORT_LIMIT -> R.string.market_err_import_limit
    MarketRefusal.LOCKED_SHOE -> R.string.market_err_locked_shoe
    MarketRefusal.NOT_TRANSFERABLE -> R.string.market_err_not_transferable
    MarketRefusal.RUN_MORE -> R.string.market_err_run_more
    MarketRefusal.SIGN_IN -> R.string.market_msg_sign_in
    MarketRefusal.OTHER -> R.string.market_err_other
}
