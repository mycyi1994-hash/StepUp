package com.stepup.android.data.repo


/**
 * 값을 치르는 요청(강화 · 수리 · 부스트 구매)의 요청 번호 — 서버가 같은 번호를 한 번만 치른다(0057 request_receipts).
 *
 * 응답을 잃은(연결이 끊긴) 요청을 다시 보내면 같은 번호를 쓴다. 답을 받으면 버린다([done]) — 다음은 새 요청이다.
 * 잃은 번호도 마지막으로 쓴 지 [TTL_MS] 가 지나면 버린다: 그 요청이 실제로는 서버에 닿았다면, 한참 뒤의 새 구매 ·
 * 수리가 옛 영수증을 받아 "됐어요"로 보이고 아무 일도 일어나지 않았다.
 */
@OptIn(kotlin.concurrent.atomics.ExperimentalAtomicApi::class, kotlin.uuid.ExperimentalUuidApi::class)
class RequestKeys(private val now: () -> Long = { com.stepup.android.domain.epochMillis() }) {
    private data class Entry(val key: String, val usedAt: Long)

    // 여러 일꾼이 함께 써도 되게 — 원자 참조의 바꿔 쓰기(예전 ConcurrentHashMap.compute 와 같은 한 번에 하나)
    private val entries = kotlin.concurrent.atomics.AtomicReference(emptyMap<String, Entry>())

    private inline fun update(change: (Map<String, Entry>) -> Map<String, Entry>): Map<String, Entry> {
        while (true) {
            val old = entries.load()
            val new = change(old)
            if (entries.compareAndSet(old, new)) return new
        }
    }

    /** [action] 의 요청 번호 — 답을 못 받은 번호가 아직 새것이면 그 번호, 아니면 새 번호 */
    fun keyFor(action: String): String {
        val t = now()
        val fresh = kotlin.uuid.Uuid.random().toString()
        return update { map ->
            val old = map[action]
            map + (action to if (old != null && t - old.usedAt <= TTL_MS) old.copy(usedAt = t) else Entry(fresh, t))
        }.getValue(action).key
    }

    /** 이 번호의 답을 받았다 — 그사이 새 번호로 바뀌었으면 그대로 둔다 */
    fun done(action: String, key: String) {
        update { map -> if (map[action]?.key == key) map - action else map }
    }

    /**
     * [prefix] 로 시작하는 요청의 번호를 버린다 — 사용자가 그 일을 스스로 거뒀다(입찰 · 매물 거두기). 응답을 잃은 입찰을 거둔 뒤
     * 같은 값으로 다시 걸면 새 요청이어야 한다 — 옛 번호를 쓰면 서버가 거둔 입찰의 영수증을 돌려줘 아무것도 걸리지 않았다.
     */
    fun forget(prefix: String) {
        update { map -> map.filterKeys { !it.startsWith(prefix) } }
    }

    companion object {
        const val TTL_MS = 10 * 60_000L
    }
}
