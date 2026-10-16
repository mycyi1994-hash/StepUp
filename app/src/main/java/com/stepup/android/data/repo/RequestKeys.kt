package com.stepup.android.data.repo

import java.util.UUID
import java.util.concurrent.ConcurrentHashMap

/**
 * 값을 치르는 요청(강화 · 수리 · 부스트 구매)의 요청 번호 — 서버가 같은 번호를 한 번만 치른다(0057 request_receipts).
 *
 * 응답을 잃은(연결이 끊긴) 요청을 다시 보내면 같은 번호를 쓴다. 답을 받으면 버린다([done]) — 다음은 새 요청이다.
 * 잃은 번호도 마지막으로 쓴 지 [TTL_MS] 가 지나면 버린다: 그 요청이 실제로는 서버에 닿았다면, 한참 뒤의 새 구매 ·
 * 수리가 옛 영수증을 받아 "됐어요"로 보이고 아무 일도 일어나지 않았다.
 */
internal class RequestKeys(private val now: () -> Long = System::currentTimeMillis) {
    private data class Entry(val key: String, val usedAt: Long)

    private val entries = ConcurrentHashMap<String, Entry>()

    /** [action] 의 요청 번호 — 답을 못 받은 번호가 아직 새것이면 그 번호, 아니면 새 번호 */
    fun keyFor(action: String): String {
        val t = now()
        return entries.compute(action) { _, old ->
            if (old != null && t - old.usedAt <= TTL_MS) old.copy(usedAt = t) else Entry(UUID.randomUUID().toString(), t)
        }!!.key
    }

    /** 이 번호의 답을 받았다 — 그사이 새 번호로 바뀌었으면 그대로 둔다 */
    fun done(action: String, key: String) {
        entries.computeIfPresent(action) { _, entry -> if (entry.key == key) null else entry }
    }

    companion object {
        const val TTL_MS = 10 * 60_000L
    }
}
