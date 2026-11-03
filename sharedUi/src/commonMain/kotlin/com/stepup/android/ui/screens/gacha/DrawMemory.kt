package com.stepup.android.ui.screens.gacha

import com.stepup.android.domain.DrawKind

/**
 * 뽑기 화면이 앱을 다시 열어도 기억하는 것 두 가지.
 *
 * - 결과를 아직 모르는 요청([PendingDraw]) — 보내기 전에 적고, 결과를 확인하면 지운다. 뒤로 가거나 앱이 죽어도
 *   다시 열면 "결과 확인"(26)으로 이어서 확인한다. 새 뽑기는 그동안 막는다.
 * - 계정마다 마지막으로 본 "러닝으로 받은 상급 기회" 수 — 러닝이 반영돼 늘었을 때만 짧게 알린다(18).
 *
 * 둘 다 서버 값을 **비교하는 기준**일 뿐 수를 만들지 않는다. 기회 · 결과는 언제나 서버에서 다시 읽는다.
 */
interface DrawMemory {
    fun pending(): PendingDraw?
    fun keep(pending: PendingDraw)
    fun forget()
    fun seenRunChances(account: String?): Int?
    fun seeRunChances(account: String?, count: Int)
}

/** 기기 검사 · 흉내 낸 서버용 — 프로세스 안에서만 */
class InMemoryDrawMemory : DrawMemory {
    @kotlin.concurrent.Volatile private var pending: PendingDraw? = null
    private val seen = com.stepup.android.domain.ConcurrentMap<String, Int>()

    override fun pending(): PendingDraw? = pending
    override fun keep(pending: PendingDraw) { this.pending = pending }
    override fun forget() { pending = null }
    override fun seenRunChances(account: String?): Int? = seen[account.orEmpty()]
    override fun seeRunChances(account: String?, count: Int) { seen[account.orEmpty()] = count }
}
