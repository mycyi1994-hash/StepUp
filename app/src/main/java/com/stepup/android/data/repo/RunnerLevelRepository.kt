package com.stepup.android.data.repo

import com.stepup.android.data.remote.RunnerBadgesResult
import com.stepup.android.data.remote.RunnerMeResult
import com.stepup.android.domain.RecordingOwner
import com.stepup.android.domain.RunnerBadges
import com.stepup.android.domain.RunnerLevel
import com.stepup.android.domain.RunnerLevelNotice
import com.stepup.android.domain.RunnerLevelNotices
import com.stepup.android.domain.RunnerSeen
import java.util.concurrent.ConcurrentHashMap
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

/** 이 폰이 계정마다 마지막으로 알린 러너 레벨을 남기는 곳 — UserPrefs 가 구현한다 */
interface RunnerSeenStore {
    suspend fun read(owner: String): RunnerSeen?
    suspend fun write(owner: String, seen: RunnerSeen)
}

/**
 * 러너 레벨(서버 0121) — 내 정보 카드 · 레벨이 올랐다는 알림 · 신발 화면의 하루 에너지 글 · 크루 명단 배지가 같은 값을 본다.
 *
 * 내 정보를 열 때와 동기화 때(EconomySync — 앱을 켤 때 · 러닝을 올린 뒤) [refresh] 한다. 계속 묻지 않는다.
 * 서버에 함수가 없거나(0121 전) 서버가 러너 레벨을 껐으면 [level] 은 null — 화면은 러너 레벨 칸을 숨기고 신발 화면은 예전 글 그대로다.
 *
 * 알림은 계정마다 지난번 본 값([RunnerSeenStore])과 견줘 한 번만 — 사람이 닫을 때([consumeNotice]) 본 것으로 적는다(닫기 전에 앱이
 * 꺼져도 다음에 다시 보인다). 이 폰에서 처음 보는 계정이면 "지난 러닝으로 Lv N · 상자 N개"를 한 번.
 */
class RunnerLevelRepository(
    private val fetchMe: suspend () -> RunnerMeResult,
    private val fetchBadges: suspend (List<String>) -> RunnerBadgesResult,
    private val store: RunnerSeenStore? = null,
    /** 지금 계정(기록 주인) — 알린 것은 계정마다 */
    private val owner: suspend () -> String = { RecordingOwner.LEGACY },
    private val clock: () -> Long = System::currentTimeMillis,
) {
    private val _level = MutableStateFlow<RunnerLevel?>(null)

    /** 보이는 러너 레벨 — 서버가 켰을 때만. 아직 못 읽었거나 · 함수가 없거나 · 껐으면 null(칸을 숨긴다) */
    val level: StateFlow<RunnerLevel?> = _level.asStateFlow()

    private val _notice = MutableStateFlow<RunnerLevelNotice?>(null)

    /** 알릴 것(레벨이 올랐다 · 처음 본 러너 레벨) — 앱 셸이 탭 첫 화면에서 한 번 보이고 [consumeNotice] */
    val notice: StateFlow<RunnerLevelNotice?> = _notice.asStateFlow()

    /** 서버에 `runner_me` 가 없다 — 앱이 켜진 동안 다시 묻지 않는다 */
    @Volatile
    private var missing = false

    /** [level] 이 누구의 값인가 — 다른 계정으로 바뀌면 쓰지 않는다 */
    @Volatile
    private var levelOwner: String? = null

    private val lock = Mutex()

    /** 지금 값 — 화면 밖(계산)에서 읽을 때 */
    val now: RunnerLevel? get() = _level.value

    /** `runner_me` 를 다시 읽는다 — 받은 러너 레벨(꺼졌거나 못 받았으면 아는 값 · null) */
    suspend fun refresh(): RunnerLevel? {
        if (missing) return null
        val who = runCatching { owner() }.getOrNull() ?: return null
        // 로그인한 계정만 묻는다(로그인 전이면 서버도 답하지 않는다)
        if (RecordingOwner.userId(who) == null) return null
        return lock.withLock {
            val result = fetchMe()
            // 묻는 사이 계정이 바뀌었다 — 앞 계정의 값을 새 계정에 보이지 않는다
            if (runCatching { owner() }.getOrNull() != who) return@withLock null
            when (result) {
                is RunnerMeResult.Ok -> {
                    val shown = result.level.takeIf { it.enabled }
                    levelOwner = who
                    _level.value = shown
                    if (shown != null) updateNotice(who, shown) else _notice.value = null
                    shown
                }
                RunnerMeResult.Missing -> {
                    missing = true
                    _level.value = null
                    _notice.value = null
                    null
                }
                // 끊겼으면 아는 값 그대로(이 계정의 것일 때만)
                RunnerMeResult.Failed -> _level.value?.takeIf { levelOwner == who }
            }
        }
    }

    /** 지난번 본 값과 견줘 알릴 것을 고른다. 알릴 것이 없으면 지금 값을 조용히 본 것으로 적는다(레벨이 내려갔을 때 등) */
    private suspend fun updateNotice(who: String, now: RunnerLevel) {
        val store = store ?: return
        val seen = try {
            store.read(who)
        } catch (cancelled: kotlinx.coroutines.CancellationException) {
            throw cancelled
        } catch (_: Exception) {
            return
        }
        val notice = RunnerLevelNotices.of(seen, now)
        if (notice != null) {
            _notice.value = notice
            return
        }
        _notice.value = null
        val snapshot = RunnerSeen.of(now)
        if (seen != snapshot) runCatching { store.write(who, snapshot) }
    }

    /** 알림을 닫았다 — 보인 값을 이 계정이 본 것으로 적는다 */
    suspend fun consumeNotice() {
        val shown = _notice.value ?: return
        _notice.value = null
        val who = runCatching { owner() }.getOrNull() ?: return
        if (who != levelOwner) return
        runCatching { store?.write(who, shown.snapshot) }
    }

    /** 로그아웃 · 계정 바뀜 — 앞 계정의 레벨 · 알림을 비운다(폰에 남긴 본 값은 계정마다라 그대로) */
    fun forgetAccount() {
        _level.value = null
        _notice.value = null
        levelOwner = null
    }

    // ── 이름 옆 배지(크루 명단 · 대기실) ──

    private data class CachedBadge(val badge: Int, val at: Long)

    private val badgeCache = ConcurrentHashMap<String, CachedBadge>()

    @Volatile
    private var badgesMissing = false

    private val badgeLock = Mutex()

    /** 이미 받아 둔 배지(오래된 것도) — 목록을 그릴 때 기다리지 않고 바로 */
    fun cachedBadges(ids: Collection<String>): Map<String, Int> =
        ids.mapNotNull { id -> badgeCache[id]?.badge?.takeIf { it > 0 }?.let { id to it } }.toMap()

    /**
     * [ids] 의 배지 — 받은 지 [BADGE_TTL_MS] 안인 사람은 다시 묻지 않고, 나머지는 [RunnerBadges.MAX_PER_CALL] 명씩 한 번에 묻는다.
     * 못 받으면 아는 값만. 서버에 함수가 없으면 다시 묻지 않는다
     */
    suspend fun badges(ids: Collection<String>): Map<String, Int> {
        if (badgesMissing) return emptyMap()
        val wanted = ids.filter(RunnerBadges::isUserId).distinct()
        if (wanted.isEmpty()) return emptyMap()
        badgeLock.withLock {
            val now = clock()
            val stale = wanted.filter { id -> badgeCache[id]?.let { now - it.at >= BADGE_TTL_MS } ?: true }
            for (chunk in stale.chunked(RunnerBadges.MAX_PER_CALL)) {
                when (val r = fetchBadges(chunk)) {
                    is RunnerBadgesResult.Ok -> {
                        val at = clock()
                        // 서버는 배지가 있는 사람만 돌려준다 — 묻고 답이 없는 사람은 0 으로 적어 두고 다시 묻지 않는다
                        chunk.forEach { id -> badgeCache[id] = CachedBadge(r.badges[id] ?: 0, at) }
                    }
                    RunnerBadgesResult.Missing -> {
                        badgesMissing = true
                        return emptyMap()
                    }
                    RunnerBadgesResult.Failed -> break
                }
            }
        }
        return cachedBadges(wanted)
    }

    companion object {
        /** 배지를 다시 묻는 간격 — 목록을 다시 열 때마다 묻지 않게 */
        const val BADGE_TTL_MS = 10 * 60 * 1000L
    }
}
