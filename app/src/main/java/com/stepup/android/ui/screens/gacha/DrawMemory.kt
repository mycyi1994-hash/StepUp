package com.stepup.android.ui.screens.gacha

import android.annotation.SuppressLint
import android.content.Context
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
    @Volatile private var pending: PendingDraw? = null
    private val seen = java.util.concurrent.ConcurrentHashMap<String, Int>()

    override fun pending(): PendingDraw? = pending
    override fun keep(pending: PendingDraw) { this.pending = pending }
    override fun forget() { pending = null }
    override fun seenRunChances(account: String?): Int? = seen[account.orEmpty()]
    override fun seeRunChances(account: String?, count: Int) { seen[account.orEmpty()] = count }
}

/**
 * 앱의 기억 — 작은 설정 파일 하나(`stepup_draw`). 사용자 설정 파일(strideup_prefs)과 따로 둔다.
 * 계정이 다르거나 하루가 지난 요청은 쓰지 않는다([DrawViewModel] 이 거른다).
 */
internal class PrefsDrawMemory(context: Context) : DrawMemory {
    private val prefs = context.applicationContext.getSharedPreferences(FILE, Context.MODE_PRIVATE)

    override fun pending(): PendingDraw? {
        if (!prefs.contains(KIND)) return null
        val kind = runCatching { DrawKind.valueOf(prefs.getString(KIND, null).orEmpty()) }.getOrNull() ?: return null
        return PendingDraw(
            kind = kind,
            newestBefore = prefs.getLong(NEWEST, 0L),
            leftBefore = prefs.getInt(LEFT, 0),
            startedAt = prefs.getLong(STARTED, 0L),
            account = prefs.getString(ACCOUNT, null),
            shoeId = if (prefs.contains(SHOE)) prefs.getLong(SHOE, 0L) else null,
        )
    }

    @SuppressLint("ApplySharedPref")
    override fun keep(pending: PendingDraw) {
        // 보내기 전에 적는다 — 바로 뒤에 앱이 죽어도 남도록 commit(apply 는 나중에 쓴다)
        val editor = prefs.edit()
            .putString(KIND, pending.kind.name)
            .putLong(NEWEST, pending.newestBefore)
            .putInt(LEFT, pending.leftBefore)
            .putLong(STARTED, pending.startedAt)
            .putString(ACCOUNT, pending.account)
        if (pending.shoeId != null) editor.putLong(SHOE, pending.shoeId) else editor.remove(SHOE)
        editor.commit()
    }

    override fun forget() {
        prefs.edit().remove(KIND).remove(NEWEST).remove(LEFT).remove(STARTED).remove(ACCOUNT).remove(SHOE).apply()
    }

    override fun seenRunChances(account: String?): Int? {
        val key = SEEN_RUN + account.orEmpty()
        return if (prefs.contains(key)) prefs.getInt(key, 0) else null
    }

    override fun seeRunChances(account: String?, count: Int) {
        prefs.edit().putInt(SEEN_RUN + account.orEmpty(), count).apply()
    }

    private companion object {
        const val FILE = "stepup_draw"
        const val KIND = "pending_kind"
        const val NEWEST = "pending_newest_before"
        const val LEFT = "pending_left_before"
        const val STARTED = "pending_started_at"
        const val ACCOUNT = "pending_account"
        const val SHOE = "pending_shoe_id"
        const val SEEN_RUN = "seen_run_chances:"
    }
}
