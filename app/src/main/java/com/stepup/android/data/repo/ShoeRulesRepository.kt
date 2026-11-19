package com.stepup.android.data.repo

import com.stepup.android.data.remote.ServerResult
import com.stepup.android.domain.BoxStatus
import com.stepup.android.domain.DailyRewards
import com.stepup.android.domain.DailyRewardsNews
import com.stepup.android.domain.RewardNews
import com.stepup.android.domain.ShoeRules
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

/**
 * 경제 4판(서버 0116)의 규칙 · 상자 현황을 앱 한 곳에 둔다 — 신발 화면 · 포인트 · 수리 · 강화 · 출발 전 점검 · 뽑기가 같은 값을 본다.
 *
 * 규칙은 서버 `shoe_rules_v4`(등급 범위 · 포인트 · 수리 몫 · 무료 수리 · 운). 아직 못 읽었으면 [ShoeRules.FALLBACK](서버 0116 과 같은 값).
 * 동기화 때(EconomySync 뒤) · 수리 · 뽑기 뒤에 [refresh] 한다. 서버에 함수가 없으면(예전 서버) 그대로 둔다.
 *
 * 오늘의 보상(서버 0118, 2026-11-19) — 같은 `box_status` 글로 [daily](홈 막대 · 저녁 알림 · 업적 · 이벤트)도 읽는다. 받은 글은 폰에도
 * 남긴다([DailyRewardsStore]) — 앱을 다시 켜도 바로 그리고, 한국 날짜가 바뀌었으면 화면이 "—" 를 보인다(어제 눈금이 남지 않게).
 * 상자 현황([boxes])은 오늘 받은 것만 디스크에서 되살린다.
 */
class ShoeRulesRepository(
    private val fetchRules: suspend () -> ServerResult<ShoeRules>,
    private val fetchBoxes: suspend () -> ServerResult<BoxStatus>,
    /** `box_status` 응답 글 그대로(0118 의 칸까지 읽는다). 없으면 [fetchBoxes] 만 — 오늘의 보상은 없다 */
    private val fetchBoxesRaw: (suspend () -> ServerResult<String>)? = null,
    /** 받은 글을 남기는 곳 — 없으면 메모리만 */
    private val store: DailyRewardsStore? = null,
    /** 지금 계정(기록 주인) — 남긴 글 · 본 것은 계정마다 */
    private val owner: suspend () -> String = { com.stepup.android.domain.RecordingOwner.LEGACY },
    private val clock: () -> Long = System::currentTimeMillis,
) {
    private val _rules = MutableStateFlow(ShoeRules.FALLBACK)
    val rules: StateFlow<ShoeRules> = _rules.asStateFlow()

    private val _boxes = MutableStateFlow<BoxStatus?>(null)
    /** 상자 현황 — 로그인 전 · 아직 못 읽었으면 null */
    val boxes: StateFlow<BoxStatus?> = _boxes.asStateFlow()

    private val _daily = MutableStateFlow<DailyRewards?>(null)

    /**
     * 오늘의 보상(0118) — 서버에 0118 이 없거나 아직 못 읽었으면 null(화면은 예전 모습). [DailyRewards.day] 가 오늘이 아닐 수 있다 —
     * 화면은 [DailyRewards.isFor] 로 가른다.
     */
    val daily: StateFlow<DailyRewards?> = _daily.asStateFlow()

    private val _news = MutableStateFlow<RewardNews?>(null)

    /** 앱 안 한 줄로 알릴 새로 받은 보상 — 홈이 보이고 [consumeNews] 한다 */
    val news: StateFlow<RewardNews?> = _news.asStateFlow()

    /** 남긴 글을 되살린 계정 — 다른 계정으로 바뀌면 다시 읽는다 */
    private var restoredFor: String? = null
    private val restoreLock = Mutex()

    /** 지금 값 — 화면 밖(계산)에서 읽을 때 */
    val now: ShoeRules get() = _rules.value

    suspend fun refresh(): Boolean {
        val r = fetchRules()
        if (r is ServerResult.Ok) _rules.value = r.value
        return r is ServerResult.Ok
    }

    /**
     * 폰에 남긴 `box_status` 글을 되살린다(앱을 켤 때 · 저녁 알림) — 이미 받은 값이 있으면 그대로 둔다. 계정마다 한 번.
     * 상자 현황은 한국 날짜가 오늘인 것만(어제 받은 상자 수 · 산 수를 오늘 것처럼 보이지 않는다).
     */
    suspend fun restore() {
        val store = store ?: return
        restoreLock.withLock {
            val who = runCatching { owner() }.getOrNull() ?: return
            if (restoredFor == who) return
            restoredFor = who
            val saved = runCatching { store.read(who) }.getOrNull() ?: return
            val today = DailyRewards.gameDay(clock())
            if (_daily.value == null) _daily.value = DailyRewards.parse(saved.raw, saved.fetchedAt)
            if (_boxes.value == null && DailyRewards.gameDay(saved.fetchedAt) == today) _boxes.value = BoxStatus.parse(saved.raw)
        }
    }

    suspend fun refreshBoxes(): BoxStatus? {
        val raw = fetchBoxesRaw
        if (raw == null) {
            val r = fetchBoxes()
            if (r is ServerResult.Ok) _boxes.value = r.value
            return (r as? ServerResult.Ok)?.value
        }
        val r = raw()
        if (r !is ServerResult.Ok) return null
        val fetchedAt = clock()
        val box = BoxStatus.parse(r.value) ?: return null
        _boxes.value = box
        // 0118 이 없는 서버면 null — 화면은 예전 모습으로 돌아간다
        _daily.value = DailyRewards.parse(r.value, fetchedAt)
        store?.let { s -> runCatching { s.write(owner(), r.value, fetchedAt) } }
        return box
    }

    /**
     * 러닝이 올라간 뒤 — 상태를 다시 읽고 이 계정이 아직 보지 않은 받은 것을 한 번만 돌려준다(본 것으로 적는다).
     * 이 폰에서 처음 보는 계정이면 지금까지 받은 것을 본 것으로 적고 알리지 않는다(새 판을 처음 켠 날 사흘 치를 한꺼번에 알리지 않게).
     */
    suspend fun refreshForNews(): RewardNews? {
        refreshBoxes() ?: return null
        val state = _daily.value ?: return null
        val store = store ?: return null
        val who = runCatching { owner() }.getOrNull() ?: return null
        val seen = runCatching { store.seenAt(who) }.getOrNull()
        val latest = DailyRewardsNews.latestAt(state.recent)
        val today = DailyRewards.gameDay(clock())
        val fresh = DailyRewardsNews.fresh(state.recent, seen)
        if (latest != null && (seen == null || latest > seen)) runCatching { store.setSeenAt(who, latest) }
        if (seen == null && latest == null) runCatching { store.setSeenAt(who, 0L) }
        return DailyRewardsNews.of(fresh, state, today)
    }

    /** 앱 안 한 줄로 알린다 — 홈이 보이고 [consumeNews] */
    fun showNews(news: RewardNews) {
        _news.value = news
    }

    fun consumeNews() {
        _news.value = null
    }

    /** 로그아웃 · 계정 바뀜 — 계정에 묶인 값(무료 수리 · 상자 · 오늘의 보상)을 비운다. 폰에 남긴 글은 계정마다라 다른 계정이 읽지 않는다 */
    fun forgetAccount() {
        _rules.value = _rules.value.copy(freeRepairReady = null, freeRepairNextMs = null)
        _boxes.value = null
        _daily.value = null
        _news.value = null
        restoredFor = null
    }
}

/** 폰에 남긴 `box_status` 글 — 받은 시각 */
data class SavedBoxStatus(val raw: String, val fetchedAt: Long)

/** 오늘의 보상을 폰에 남기는 곳(UserPrefs) — 계정마다 */
interface DailyRewardsStore {
    suspend fun read(owner: String): SavedBoxStatus?
    suspend fun write(owner: String, raw: String, fetchedAt: Long)

    /** 이 계정이 마지막으로 본 받은 시각(벽시계 ms) — 처음이면 null */
    suspend fun seenAt(owner: String): Long?
    suspend fun setSeenAt(owner: String, at: Long)
}
