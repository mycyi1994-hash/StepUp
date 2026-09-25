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
import com.stepup.android.data.repo.MarketRepository
import com.stepup.android.data.repo.ModelBook
import com.stepup.android.data.repo.MyMarket
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

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

    data class Res(@StringRes val id: Int) : MarketMessage
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

    init {
        refresh()
    }

    fun refresh() {
        viewModelScope.launch {
            board.value = board.value.copy(loading = true, problem = null)

            // 사는 사람과 파는 사람의 장부를 맞추는 것이 먼저다. 팔린 신발이
            // 아직 보관함에 남아 있는 채로 시세판을 열면, 없는 신발을 팔려고
            // 하게 된다.
            repo.sync()

            when (val quotes = repo.quotes()) {
                is ServerResult.Ok -> {
                    // 내 거래와 잔고는 없어도 시세판은 보여 준다.
                    // 셋을 묶어 하나가 실패했다고 전부 못 보게 할 이유가 없다.
                    val mineResult = repo.mine()
                    val mine = if (mineResult is ServerResult.Ok) mineResult.value else null
                    val balanceResult = repo.tradableBalance()
                    val tradable =
                        if (balanceResult is ServerResult.Ok) balanceResult.value else 0.0
                    board.value = MarketBoard(
                        loading = false,
                        quotes = quotes.value,
                        mine = mine,
                        tradable = tradable,
                    )
                }
                else -> board.value = board.value.copy(
                    loading = false,
                    problem = quotes.problem(),
                )
            }
        }
    }

    fun cancelListing(listingId: Long) = act { repo.cancelListing(listingId) }

    fun cancelBid(bidId: Long) = act { repo.cancelBid(bidId) }

    private fun act(block: suspend () -> MarketOutcome) {
        viewModelScope.launch {
            message.value = block().say()
            refresh()
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

    fun open(key: ModelKey) {
        if (model == key && state.value.book != null) return
        model = key
        load()
    }

    fun load() {
        val key = model ?: return
        viewModelScope.launch {
            state.value = state.value.copy(loading = true, problem = null)
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
                    val tradable =
                        if (balanceResult is ServerResult.Ok) balanceResult.value else 0.0
                    state.value = MarketModelState(
                        loading = false,
                        book = book.value,
                        quote = quote,
                        tradable = tradable,
                        // 구독하는 쪽이 없는 stateIn(WhileSubscribed) 값은 늘 빈 목록이라
                        // 팔 신발을 못 고른다. 지금 목록을 직접 읽는다.
                        mySneakers = repo.inventory().first().filter {
                            it.factionId == key.faction &&
                                it.rarity == key.rarity &&
                                it.variant == key.variant
                        },
                    )
                }
                else -> state.value = state.value.copy(
                    loading = false,
                    problem = book.problem(),
                )
            }
        }
    }

    fun buy(listingId: Long) = act { repo.buy(listingId) }

    fun sell(localId: Long, bidId: Long) = act { repo.sellInto(localId, bidId) }

    fun listForSale(localId: Long, price: Double) = act { repo.listForSale(localId, price) }

    fun bid(minLevel: Int, price: Double) = act {
        val key = model ?: return@act MarketOutcome.Offline("")
        repo.placeBid(key, minLevel, price)
    }

    private fun act(block: suspend () -> MarketOutcome) {
        viewModelScope.launch {
            message.value = block().say()
            load()
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
 * 서버가 적어 보낸 이유를 그대로 쓴다. "거래에 실패했습니다"로 덮으면
 * 무엇이 모자랐는지가 사라지고, 사용자는 같은 일을 또 시도하게 된다.
 */
private fun MarketOutcome.say(): MarketMessage? = when (this) {
    is MarketOutcome.Ok -> if (traded) MarketMessage.Res(R.string.market_msg_traded) else null
    MarketOutcome.Equipped -> MarketMessage.Res(R.string.market_msg_equipped)
    is MarketOutcome.Failed -> MarketMessage.Text(reason)
    is MarketOutcome.Offline -> MarketMessage.Text(reason)
    MarketOutcome.SignInRequired -> MarketMessage.Res(R.string.market_msg_sign_in)
}
