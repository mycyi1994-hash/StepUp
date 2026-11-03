package com.stepup.ui

import com.stepup.android.domain.DrawKind
import com.stepup.android.ui.screens.gacha.DrawMemory
import com.stepup.android.ui.screens.gacha.PendingDraw
import platform.Foundation.NSUserDefaults

/**
 * 뽑기 화면의 기억(안드로이드 PrefsDrawMemory 와 같은 열쇠 · 같은 뜻) — iOS UserDefaults 의 따로 둔 묶음(`stepup_draw`).
 * 결과를 아직 모르는 요청은 보내기 전에 적고(바로 디스크에), 계정마다 본 "러닝으로 받은 상급 기회" 수를 둔다. 수를 만들지 않는다.
 */
class IosDrawMemory : DrawMemory {
    private val d = NSUserDefaults(suiteName = "stepup_draw")

    override fun pending(): PendingDraw? {
        val kind = d.stringForKey(KIND)?.let { k -> DrawKind.entries.firstOrNull { it.name == k } } ?: return null
        return PendingDraw(
            kind = kind,
            newestBefore = d.stringForKey(NEWEST)?.toLongOrNull() ?: 0L,
            leftBefore = d.integerForKey(LEFT).toInt(),
            startedAt = d.stringForKey(STARTED)?.toLongOrNull() ?: 0L,
            account = d.stringForKey(ACCOUNT),
            shoeId = d.stringForKey(SHOE)?.toLongOrNull(),
        )
    }

    override fun keep(pending: PendingDraw) {
        d.setObject(pending.kind.name, KIND)
        d.setObject(pending.newestBefore.toString(), NEWEST)
        d.setInteger(pending.leftBefore.toLong(), LEFT)
        d.setObject(pending.startedAt.toString(), STARTED)
        if (pending.account != null) d.setObject(pending.account, ACCOUNT) else d.removeObjectForKey(ACCOUNT)
        if (pending.shoeId != null) d.setObject(pending.shoeId.toString(), SHOE) else d.removeObjectForKey(SHOE)
        // 보내기 전에 적는다 — 바로 뒤에 앱이 죽어도 남도록(안드로이드 commit 과 같다)
        d.synchronize()
    }

    override fun forget() {
        listOf(KIND, NEWEST, LEFT, STARTED, ACCOUNT, SHOE).forEach { d.removeObjectForKey(it) }
    }

    override fun seenRunChances(account: String?): Int? {
        val key = SEEN_RUN + account.orEmpty()
        return if (d.objectForKey(key) != null) d.integerForKey(key).toInt() else null
    }

    override fun seeRunChances(account: String?, count: Int) {
        d.setInteger(count.toLong(), SEEN_RUN + account.orEmpty())
    }

    private companion object {
        const val KIND = "pending_kind"
        const val NEWEST = "pending_newest_before"
        const val LEFT = "pending_left_before"
        const val STARTED = "pending_started_at"
        const val ACCOUNT = "pending_account"
        const val SHOE = "pending_shoe_id"
        const val SEEN_RUN = "seen_run_chances:"
    }
}
