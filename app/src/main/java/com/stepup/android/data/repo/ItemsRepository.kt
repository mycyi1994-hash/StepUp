package com.stepup.android.data.repo

import com.stepup.android.data.prefs.RunItemBinding
import com.stepup.android.data.remote.ServerResult
import com.stepup.android.domain.Bag
import com.stepup.android.domain.ItemKind
import com.stepup.android.domain.RecordingOwner
import com.stepup.android.domain.RunItemPicks
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

/**
 * 아이템(서버 0106) — 가방 · 바로 쓰기 · 출발 때 고른 아이템.
 *
 * 출발 전 점검에서 고른 아이템([picks])은 러닝이 실제로 켜질 때 그 러닝의 시작 시각과 함께 폰에 적고([startRun]),
 * 서버에 묶는다([flush] — `run_items_use`). 신호가 없으면 올리기 일꾼이 러닝보다 **먼저** 다시 보낸다
 * (서버는 이미 기록한 러닝에는 아이템을 붙이지 않는다 — 과속 신고와 같은 순서).
 * 개수는 서버가 정본이다 — 폰은 쓴 뒤 서버 값을 다시 읽어 보인다.
 */
class ItemsRepository(
    private val bagCall: suspend () -> ServerResult<Bag>,
    private val useCall: suspend (ItemKind, String, Long?) -> ServerResult<Int>,
    private val bindCall: suspend (startedAt: Long, kinds: List<String>, userId: String) -> ServerResult<Unit>,
    private val addPending: suspend (RunItemBinding) -> Unit,
    private val readPending: suspend () -> List<RunItemBinding>,
    private val takePending: suspend (Long) -> Unit,
    private val configured: () -> Boolean = { true },
    /** 바로 쓰는 아이템을 썼다 — 신발 내구도 · 에너지가 바뀌었으니 잔고 · 신발을 다시 받는다 */
    private val onChanged: suspend () -> Unit = {},
) {
    private val _bag = MutableStateFlow<Bag?>(null)

    /** 내 가방 — 아직 못 읽었으면 null */
    val bag: StateFlow<Bag?> = _bag

    private val _picks = MutableStateFlow<Set<ItemKind>>(emptySet())

    /** 다음 러닝에 쓸 아이템(출발 전 점검에서 고른 것) */
    val picks: StateFlow<Set<ItemKind>> = _picks

    private val flushLock = Mutex()

    /** 마지막으로 고른 때 — 고르고 출발하지 않은 채 오래 지나면(다른 러닝 · 크루 출발) 쓰지 않는다 */
    @Volatile
    private var pickedAt = 0L

    suspend fun refresh(): ServerResult<Bag> {
        if (!configured()) return ServerResult.Retry("서버 주소가 설정되지 않았습니다")
        val result = bagCall()
        if (result is ServerResult.Ok) {
            _bag.value = result.value
            _picks.update { RunItemPicks.keepOwned(it, result.value) }
        }
        return result
    }

    fun toggle(kind: ItemKind, now: Long = System.currentTimeMillis()) {
        _picks.update { RunItemPicks.keepOwned(RunItemPicks.toggle(it, kind), _bag.value) }
        pickedAt = now
    }

    fun clearPicks() {
        _picks.value = emptySet()
    }

    /** 바로 쓰는 아이템. [request] 는 화면이 누른 한 번마다 하나 — 다시 보내도 서버가 한 번만 쓴다 */
    suspend fun use(kind: ItemKind, request: String, sneakerServerId: Long?): ServerResult<Int> {
        val result = useCall(kind, request, sneakerServerId)
        if (result is ServerResult.Ok) {
            _bag.update { bag -> bag?.copy(items = bag.items.map { if (it.kind == kind) it.copy(qty = result.value) else it }) }
            runCatching { onChanged() }
            refresh()
        }
        return result
    }

    /**
     * 러닝이 켜졌다(새 러닝 — 되살린 러닝이 아니다) — 고른 아이템을 이 러닝에 적고 바로 묶어 본다. 고른 것은 비운다.
     * 고른 것이 없으면 아무것도 하지 않는다.
     */
    suspend fun startRun(startedAt: Long, owner: String) {
        val kinds = _picks.value
        if (kinds.isEmpty()) return
        _picks.value = emptySet()
        // 고르고 출발하지 않은 채 오래 지났다(권한 안내에서 멈춤 등) — 그 뒤의 다른 러닝에 쓰지 않는다
        if (startedAt - pickedAt > PICK_FRESH_MS) return
        addPending(RunItemBinding(startedAt, owner, kinds.map { it.id }.sorted()))
        // 화면에서 바로 빠진 것처럼 보이게(서버가 받으면 다시 읽어 맞춘다)
        _bag.update { bag -> bag?.copy(items = bag.items.map { if (it.kind in kinds) it.copy(qty = (it.qty - 1).coerceAtLeast(0)) else it }) }
        runCatching { flush(owner) }
    }

    /**
     * 적어 둔 러닝 아이템을 서버에 묶는다. 끊겨 다시 해 볼 것이 남았으면 false — 일꾼이 러닝을 올리지 않고 다시 시도한다.
     * [currentOwner] 가 아닌 계정의 줄은 남겨 둔다(그 계정의 러닝도 지금 올라가지 않는다).
     */
    suspend fun flush(currentOwner: String): Boolean = flushLock.withLock {
        if (!configured()) return@withLock true
        var clear = true
        var bound = false
        for (row in readPending()) {
            if (row.owner != currentOwner) continue
            val userId = RecordingOwner.userId(row.owner)
            if (userId == null) {
                // 로그인 전에 기록한 러닝 — 서버의 어느 가방에서도 뺄 수 없다
                takePending(row.startedAt)
                continue
            }
            when (val result = bindCall(row.startedAt, row.kinds, userId)) {
                is ServerResult.Ok -> {
                    takePending(row.startedAt)
                    bound = true
                }
                // 서버가 받을 수 없다고 답했다(아이템이 없음 · 이미 기록한 러닝 · 오래된 러닝) — 다시 보내도 같다.
                // 서버에 아직 이 함수가 없으면(앱이 서버 0106 보다 먼저 깔렸다) 지우지 않고 다음에 — 러닝 올리기는 막지 않는다
                is ServerResult.Rejected -> {
                    if (FUNCTION !in result.reason && "PGRST202" !in result.reason) takePending(row.startedAt)
                }
                is ServerResult.Retry -> clear = false
                is ServerResult.SignInRequired -> Unit
            }
        }
        if (bound) runCatching { refresh() }
        clear
    }

    private companion object {
        const val FUNCTION = "run_items_use"

        /** 고른 뒤 이 안에 시작한 러닝에만 쓴다 — 점검 → 권한 · 위치 안내 → 3-2-1 을 넉넉히 지나는 시간 */
        const val PICK_FRESH_MS = 10 * 60 * 1000L
    }
}
