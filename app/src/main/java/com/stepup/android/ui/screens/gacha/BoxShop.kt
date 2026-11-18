package com.stepup.android.ui.screens.gacha

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.stepup.android.core.ServiceLocator
import com.stepup.android.data.remote.ServerResult
import com.stepup.android.data.repo.RequestKeys
import com.stepup.android.domain.BoxStatus
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/*
 * 상자 받는 곳(경제 4판, 서버 0116 `box_status` · `box_buy`) — 뽑기 화면 안의 시트.
 *
 * 일반 상자 = 무료 뽑기 기회(출석 · 오늘 적립 걸음 2,000 · 4,000 · 40 SUP 로 사기 하루 3), 상급 상자 = 상급 뽑기 기회(한 주 적립 걸음
 * 25,000 · 7일 연속 목표 · 신발이 처음 Lv5 · 10 · 15 · 20 · 600 SUP 로 사기 한 주 2). 모든 수 · 값 · 잔고는 서버가 준 것이다 —
 * 산 뒤에도 서버가 다시 준 현황으로만 바뀐다(폰이 잔고를 미리 빼 보이지 않는다).
 */

/** 살 수 있는 상자 — 서버 `box_buy(p_kind)` 의 값 */
enum class BoxKind(val id: String) { NORMAL("NORMAL"), PREMIUM("PREMIUM") }

/** 지금 살 수 없는 까닭 — 서버 현황으로 가를 수 있는 것만 */
enum class BoxBuyBlock { NOT_SELLING, LIMIT, BALANCE }

/** 사기 칸 하나 — 값 · 이번 기간(일반은 오늘, 상급은 이번 주)에 산 수 / 한도 · 못 사는 까닭(살 수 있으면 null) */
data class BoxBuyView(val kind: BoxKind, val price: Double, val bought: Int, val limit: Int, val block: BoxBuyBlock?) {
    val canBuy: Boolean get() = block == null
}

/** 서버 현황의 사기 칸 — 값이 없거나 한도가 0 이면 팔지 않는다, 한도를 다 샀으면 한도, 잔고가 모자라면 잔고 */
fun BoxStatus.buyView(kind: BoxKind): BoxBuyView {
    val price = if (kind == BoxKind.NORMAL) normalPrice else premiumPrice
    val limit = if (kind == BoxKind.NORMAL) normalLimit else premiumLimit
    val bought = if (kind == BoxKind.NORMAL) normalBought else premiumBought
    val block = when {
        price <= 0.0 || limit <= 0 -> BoxBuyBlock.NOT_SELLING
        bought >= limit -> BoxBuyBlock.LIMIT
        balance < price -> BoxBuyBlock.BALANCE
        else -> null
    }
    return BoxBuyView(kind, price, bought.coerceAtLeast(0), limit.coerceAtLeast(0), block)
}

/** 러닝 상자 한 줄 — 오늘 적립 걸음 문턱 · 받았는지 · 문턱까지 센 걸음(문턱에서 멈춘다) · 막대의 몫 */
data class RunBoxView(val steps: Int, val got: Boolean, val progress: Int, val fraction: Float)

/** 러닝 상자 줄들 — 문턱 오름차순. 받은 상자는 막대가 가득이다(오늘 걸음이 서버 줄보다 늦게 와도 받은 것은 받은 것이다) */
fun BoxStatus.runBoxViews(): List<RunBoxView> = runBoxes.filter { it.first > 0 }.sortedBy { it.first }.map { (steps, got) ->
    val progress = if (got) steps else stepsToday.coerceIn(0, steps)
    RunBoxView(steps, got, progress, progress.toFloat() / steps)
}

/** 이번 주 상급 상자까지의 몫(0 ~ 1) — 받았으면 가득. 목표가 없으면(서버 설정 0) 0 */
val BoxStatus.weekFraction: Float
    get() = when {
        weekGot -> 1f
        weekTarget <= 0 -> 0f
        else -> (stepsWeek.toFloat() / weekTarget).coerceIn(0f, 1f)
    }

/** 상자를 받는 신발 레벨(서버 0116 — 처음 Lv5 · 10 · 15 · 20 이 될 때) */
val BOX_LEVELS: List<Int> = listOf(5, 10, 15, 20)

/** 사기가 거절된 까닭 — 서버 글(0116 `box_buy` · 원장 `ledger_apply`)로 가른다. 모르는 글은 [OTHER] */
enum class BoxRefusal { NOT_ENOUGH, LIMIT, NOT_SELLING, SIGN_IN, OFFLINE, OTHER }

fun boxRefusal(reason: String): BoxRefusal = when {
    "SUP가 부족" in reason -> BoxRefusal.NOT_ENOUGH
    "까지 살 수 있어요" in reason -> BoxRefusal.LIMIT
    "상자를 팔지 않아요" in reason -> BoxRefusal.NOT_SELLING
    "로그인이 필요" in reason -> BoxRefusal.SIGN_IN
    else -> BoxRefusal.OTHER
}

/** 시트의 한 단계 — 목록(산 결과 · 거절 안내를 한 줄 얹을 수 있다) · 확인 · 보내는 중 */
sealed interface BoxStep {
    /** 목록 — [note] 는 방금 산 결과(성공) 또는 거절 까닭 */
    data class Home(val note: BoxNote? = null) : BoxStep

    /** "N SUP 로 살까요?" — [retry] 면 앞 요청의 답을 받지 못했다(다시 누르면 같은 요청 번호로 보낸다 — 서버가 한 번만 치른다) */
    data class Confirm(val kind: BoxKind, val retry: Boolean = false) : BoxStep

    data class Buying(val kind: BoxKind) : BoxStep
}

sealed interface BoxNote {
    data class Bought(val kind: BoxKind) : BoxNote
    data class Refused(val kind: BoxKind, val why: BoxRefusal) : BoxNote
}

/** 상자 받는 곳이 기대는 것 — 서버 현황 · 사기 · 산 뒤 다시 받기. 기기 검사는 흉내 낸 것을 넣는다 */
interface BoxShopSource {
    val boxes: StateFlow<BoxStatus?>
    suspend fun refresh(): BoxStatus?
    suspend fun buy(kind: BoxKind, request: String): ServerResult<Double>

    /** 산 뒤 — 잔고 · 뽑기 수 사본을 다시 받는다(머리의 잔고가 서버 값으로 바뀐다) */
    suspend fun afterBought()
}

/**
 * 상자 받는 곳의 상태 — 현황은 [BoxShopSource.boxes](ShoeRulesRepository) 하나를 함께 본다.
 * 사기는 확인을 한 번 거친 뒤에만 보내고, 응답을 잃은 사기를 다시 누르면 같은 요청 번호로 보낸다([RequestKeys], 서버 0116
 * `request_receipts`). 답을 받으면(성공 · 거절) 번호를 버린다 — 다음 사기는 새 요청이다.
 */
class BoxShopViewModel(private val source: BoxShopSource) : ViewModel() {
    val boxes: StateFlow<BoxStatus?> = source.boxes

    private val _step = MutableStateFlow<BoxStep>(BoxStep.Home())
    val step: StateFlow<BoxStep> = _step.asStateFlow()

    /** 현황을 읽는 중 · 못 읽음 — 처음 열었는데 값이 없을 때만 쓴다(있던 값은 그대로 보인다) */
    private val _loading = MutableStateFlow(false)
    val loading: StateFlow<Boolean> = _loading.asStateFlow()
    private val _failed = MutableStateFlow(false)
    val failed: StateFlow<Boolean> = _failed.asStateFlow()

    private val keys = RequestKeys()

    /** 시트를 열 때 — 서버 현황을 다시 읽는다 */
    fun open() {
        if (_step.value !is BoxStep.Buying) _step.value = BoxStep.Home()
        reload()
    }

    fun reload() {
        viewModelScope.launch {
            _loading.value = true
            val read = try { source.refresh() } catch (c: CancellationException) { throw c } catch (_: Exception) { null }
            _loading.value = false
            _failed.value = read == null && source.boxes.value == null
        }
    }

    /** 사기 칸의 "사기" — 서버 현황으로 살 수 있을 때만 확인으로 */
    fun ask(kind: BoxKind) {
        if (_step.value is BoxStep.Buying) return
        val status = source.boxes.value ?: return
        if (!status.buyView(kind).canBuy) return
        // 새로 누른 사기는 새 요청이다 — 답을 잃은 앞 요청은 그 확인 창의 "다시 시도"로만 같은 번호를 쓴다. 그 창을 닫고
        // (다시 읽은 현황을 보고) 또 사면 새 번호다 — 옛 번호를 쓰면 서버가 앞 영수증을 돌려줘 아무것도 사지 않는다
        keys.forget("box:${kind.id}")
        _step.value = BoxStep.Confirm(kind)
    }

    /** 확인의 "취소" · 시트 닫기 — 보내는 중이면 그대로 둔다(답이 오면 목록으로) */
    fun cancel() {
        if (_step.value !is BoxStep.Buying) _step.value = BoxStep.Home()
    }

    /**
     * 확인의 "N SUP 로 사기" — 한 번에 하나만 보낸다. 서버가 샀다고 답하면 [onBought](뽑기 현황 다시 읽기 — 뽑기 칸의 남은 수는
     * 뽑기 현황이 보인다)
     */
    fun confirm(onBought: () -> Unit = {}) {
        val confirm = _step.value as? BoxStep.Confirm ?: return
        val kind = confirm.kind
        _step.value = BoxStep.Buying(kind)
        viewModelScope.launch {
            val action = "box:${kind.id}"
            val key = keys.keyFor(action)
            val result = try {
                source.buy(kind, key)
            } catch (c: CancellationException) {
                throw c
            } catch (_: Exception) {
                ServerResult.Retry("network")
            }
            if (result !is ServerResult.Retry) keys.done(action, key)
            when (result) {
                is ServerResult.Ok -> {
                    // 산 것은 서버가 다시 준 현황 · 잔고로만 보인다
                    runCatching { source.refresh() }
                    runCatching { source.afterBought() }
                    _step.value = BoxStep.Home(BoxNote.Bought(kind))
                    onBought()
                }
                is ServerResult.Rejected -> {
                    runCatching { source.refresh() }
                    _step.value = BoxStep.Home(BoxNote.Refused(kind, boxRefusal(result.reason)))
                }
                is ServerResult.SignInRequired -> _step.value = BoxStep.Home(BoxNote.Refused(kind, BoxRefusal.SIGN_IN))
                is ServerResult.Retry -> {
                    // 응답만 잃었을 수 있다 — 현황을 다시 읽어 보이고, 다시 누르면 같은 번호로 보낸다(두 번 치르지 않는다)
                    runCatching { source.refresh() }
                    runCatching { source.afterBought() }
                    _step.value = BoxStep.Confirm(kind, retry = true)
                }
            }
        }
    }

    companion object {
        val Factory = viewModelFactory {
            initializer { BoxShopViewModel(ServerBoxShop()) }
        }
    }
}

/** 앱의 상자 받는 곳 — 서버 경제(EconomyApi)와 규칙 · 상자 현황 저장소(ShoeRulesRepository) */
internal class ServerBoxShop : BoxShopSource {
    override val boxes: StateFlow<BoxStatus?> = ServiceLocator.shoeRules.boxes
    override suspend fun refresh(): BoxStatus? = ServiceLocator.shoeRules.refreshBoxes()
    override suspend fun buy(kind: BoxKind, request: String): ServerResult<Double> = ServiceLocator.economyApi.boxBuy(kind.id, request)
    override suspend fun afterBought() {
        ServiceLocator.economySync.refresh()
    }
}
