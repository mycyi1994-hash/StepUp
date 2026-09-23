package com.stepup.android.data.repo

import com.stepup.android.data.local.NewsDao
import com.stepup.android.data.local.NewsItemEntity
import com.stepup.android.data.prefs.UserPrefs
import com.stepup.android.data.remote.NewsItemRow
import com.stepup.android.data.remote.ServerResult
import com.stepup.android.data.remote.StepUpServer
import java.time.OffsetDateTime
import kotlinx.coroutines.flow.Flow

/**
 * 러닝 소식.
 *
 * ── 어디서 오는가 ──
 *
 * 매일 아침 9시(KST)에 GitHub Actions 가 한 번 모아 Supabase 의 news_items
 * 표에 넣는다(.github/workflows/news-refresh.yml). 앱은 그 표만 읽는다.
 *
 * 앱이 바깥 사이트를 직접 읽지 않는 이유는 세 가지다.
 *   1. 사이트가 모양을 바꾸면 모든 폰이 한꺼번에 깨지고, 고치려면 새 APK 를 내야 한다.
 *   2. 사용자 수만큼 남의 서버를 두드린다.
 *   3. 폰이 켜져 있어야 갱신되므로 "매일 9시"가 지켜지지 않는다.
 *
 * ── 왜 받아 두는가 ──
 *
 * 지하철에서 앱을 열면 서버에 닿지 못한다. 그때 탭이 비면 어제 본 소식조차
 * 못 보게 되므로, 받은 것을 폰에 두고 닿을 때 갈아 끼운다.
 *
 * ── 얼마나 자주 묻는가 ──
 *
 * 서버가 하루 한 번 바뀌므로 앱도 하루 한 번만 물으면 된다. 탭을 열 때마다
 * 물으면 바뀌지도 않은 것을 하루 수십 번 받게 된다.
 */
class NewsRepository(
    private val server: StepUpServer,
    private val dao: NewsDao,
    private val prefs: UserPrefs,
) {

    fun observe(kind: String): Flow<List<NewsItemEntity>> = dao.observe(kind, LIMIT)

    /**
     * 필요하면 새로 받아 온다.
     *
     * @param force 사용자가 직접 새로 고침을 눌렀을 때. 그때는 하루 한 번
     *   규칙을 건너뛴다 — 기다리라는 말보다 한 번 더 받는 편이 낫다.
     * @return 받아 왔으면 true. 실패해도 예외를 던지지 않는다 — 소식 하나
     *   못 받았다고 탭이 멈출 이유가 없다.
     */
    suspend fun refresh(kind: String = RUN_EVENT, force: Boolean = false): Boolean {
        val last = prefs.newsFetchedAtNow()
        val now = System.currentTimeMillis()
        if (!force && now - last < REFRESH_EVERY_MS) return false

        val result = server.newsItems(kind, LIMIT)
        if (result !is ServerResult.Ok) return false

        // 빈 목록으로 덮지 않는다. 서버가 아직 안 채워졌을 때 어제 받은
        // 소식까지 지우면, 고치기 전보다 나빠진다.
        if (result.value.isEmpty()) {
            prefs.setNewsFetchedAt(now)
            return false
        }

        dao.replace(kind, result.value.map { it.toEntity(kind) })
        prefs.setNewsFetchedAt(now)
        return true
    }

    companion object {
        const val RUN_EVENT = "RUN_EVENT"

        private const val LIMIT = 30

        /** 서버가 하루 한 번 바뀌므로 앱도 그 간격으로 묻는다. */
        private const val REFRESH_EVERY_MS = 12 * 60 * 60 * 1000L
    }
}

private fun NewsItemRow.toEntity(kind: String) = NewsItemEntity(
    url = url,
    title = title,
    source = source,
    summary = summary,
    kind = kind,
    publishedAt = runCatching { OffsetDateTime.parse(publishedAt).toInstant().toEpochMilli() }
        .getOrElse { System.currentTimeMillis() },
)
