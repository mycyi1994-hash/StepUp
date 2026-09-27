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
import com.stepup.android.domain.DrawKind
import com.stepup.android.domain.DrawStatus
import com.stepup.android.domain.Sneaker
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/** 뽑기 화면에 보일 현황 — 로그인 전 · 불러오는 중 · 값 · 못 읽음. 못 읽은 것을 0회로 보이지 않는다 */
sealed interface DrawScreenState {
    data object SignedOut : DrawScreenState
    data object Loading : DrawScreenState
    data class Ready(val status: DrawStatus) : DrawScreenState
    data object Failed : DrawScreenState
}

/** 뽑기 뒤 한 번 보일 안내 */
enum class DrawMessage { NoFreeDraws, NoPremiumDraws, WalletRequired, MintLimit, ChainPaused, SignInRequired, Offline, DrawnRefreshing, Failed }

/** 뽑기가 기대는 것 — 서버 현황과 한 번 뽑기. 기기 테스트는 흉내 낸 것을 넣는다 */
interface DrawSource {
    /** 서버가 설정돼 있고 로그인했는가 */
    fun ready(): Boolean
    suspend fun status(): ServerResult<DrawStatus>
    suspend fun draw(kind: DrawKind): Pair<EconomyOutcome, Sneaker?>
}

/**
 * 신발 뽑기(2026-09-27 무료 정책). 무료 · 상급 두 칸, 수는 모두 서버(draw_status)가 준다.
 *
 * 한 번 누르면 결과가 올 때까지 다시 누를 수 없다. 결과는 서버가 정한 신발이고, 받은 뒤에 수를
 * 다시 읽어 온 다음에야 버튼이 풀린다 — 옛 수로 한 번 더 누르지 않게.
 */
class DrawViewModel(
    private val source: DrawSource,
    private val saved: SavedStateHandle,
) : ViewModel() {

    val tab: StateFlow<DrawKind> = saved.getStateFlow(KEY_TAB, DrawKind.FREE)

    private val _state = MutableStateFlow<DrawScreenState>(DrawScreenState.Loading)
    val state: StateFlow<DrawScreenState> = _state.asStateFlow()

    private val _drawing = MutableStateFlow(false)
    val drawing: StateFlow<Boolean> = _drawing.asStateFlow()

    /** 방금 뽑은 신발 — 결과 창을 닫으면 비운다 */
    val result = MutableStateFlow<Sneaker?>(null)

    val message = MutableStateFlow<DrawMessage?>(null)

    init {
        refresh()
    }

    fun selectTab(kind: DrawKind) {
        saved[KEY_TAB] = kind
    }

    /** 화면에 돌아올 때마다 — 지갑 페이지에서 연결하고 오면 상급 수가 바뀌어 있다 */
    fun refresh() {
        if (_drawing.value) return
        viewModelScope.launch { load() }
    }

    private suspend fun load() {
        if (!source.ready()) {
            _state.value = DrawScreenState.SignedOut
            return
        }
        val before = _state.value
        if (before !is DrawScreenState.Ready) _state.value = DrawScreenState.Loading
        _state.value = when (val read = try { source.status() } catch (c: CancellationException) { throw c } catch (_: Exception) { null }) {
            is ServerResult.Ok -> DrawScreenState.Ready(read.value)
            is ServerResult.SignInRequired -> DrawScreenState.SignedOut
            // 다시 읽기가 잠깐 실패하면 보던 수를 그대로 둔다
            else -> if (before is DrawScreenState.Ready) before else DrawScreenState.Failed
        }
    }

    /** 고른 칸으로 한 번 */
    fun draw() {
        if (_drawing.value) return
        val kind = tab.value
        val status = (_state.value as? DrawScreenState.Ready)?.status ?: return
        if (!status.canDraw(kind)) return
        _drawing.value = true
        viewModelScope.launch {
            try {
                val (outcome, shoe) = source.draw(kind)
                when {
                    shoe != null -> result.value = shoe
                    // 뽑기는 됐다 — 실패라고 하면 다시 눌러 한 번 더 뽑는다
                    outcome == EconomyOutcome.Ok -> message.value = DrawMessage.DrawnRefreshing
                    else -> message.value = outcome.toDrawMessage()
                }
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (_: Exception) {
                message.value = DrawMessage.Failed
            } finally {
                load()
                _drawing.value = false
            }
        }
    }

    fun consumeMessage() {
        message.value = null
    }

    fun dismissResult() {
        result.value = null
    }

    companion object {
        private const val KEY_TAB = "draw_tab"

        val Factory = viewModelFactory {
            initializer {
                DrawViewModel(ServiceLocator.drawSource(), createSavedStateHandle())
            }
        }
    }
}

private fun EconomyOutcome.toDrawMessage(): DrawMessage = when (this) {
    EconomyOutcome.NoFreeDraws -> DrawMessage.NoFreeDraws
    EconomyOutcome.NoPremiumDraws -> DrawMessage.NoPremiumDraws
    EconomyOutcome.WalletRequired -> DrawMessage.WalletRequired
    EconomyOutcome.MintLimitReached -> DrawMessage.MintLimit
    EconomyOutcome.ChainPaused -> DrawMessage.ChainPaused
    EconomyOutcome.SignInRequired -> DrawMessage.SignInRequired
    EconomyOutcome.Offline -> DrawMessage.Offline
    else -> DrawMessage.Failed
}

/** 앱의 뽑기 — 서버 경제(EconomyApi)와 신발 저장소 */
internal class ServerDrawSource : DrawSource {
    override fun ready(): Boolean =
        ServiceLocator.economyApi.isConfigured &&
            ServiceLocator.economySync.state.value != EconomySyncState.SIGNED_OUT

    override suspend fun status(): ServerResult<DrawStatus> = ServiceLocator.economyApi.drawStatus()

    override suspend fun draw(kind: DrawKind): Pair<EconomyOutcome, Sneaker?> =
        ServiceLocator.sneakerRepository.drawOnServer(kind)
}
