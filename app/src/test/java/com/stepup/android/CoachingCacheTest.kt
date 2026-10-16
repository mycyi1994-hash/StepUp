package com.stepup.android

import com.stepup.android.data.repo.CoachingCacheStore
import com.stepup.android.domain.coaching.CoachCandidate
import com.stepup.android.domain.coaching.CoachFact
import com.stepup.android.domain.coaching.CoachIds
import com.stepup.android.domain.coaching.CoachKind
import com.stepup.android.domain.coaching.CoachResult
import java.nio.file.Files
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/** 코칭 결과 사본 — 계정마다 따로, 기록을 지우면 그 기록의 사본도, 깨진 파일은 결과로 보이지 않는다 */
class CoachingCacheTest {
    private val result = CoachResult(
        id = 3, kind = CoachKind.RUN, generatedAt = 1_760_602_029_159, locale = "ko",
        facts = listOf(CoachFact(CoachIds.FACT_SPLIT_LAST_VS_FIRST, mapOf("last_km" to 3L, "delta_sec" to 40L))),
        factId = CoachIds.FACT_SPLIT_LAST_VS_FIRST, explanation = "후반으로 갈수록 페이스가 느려졌어요.",
        candidates = listOf(CoachCandidate(CoachIds.CAND_PACE_START_EASIER, mapOf("pace_sec" to 450L))),
        candidateId = CoachIds.CAND_PACE_START_EASIER, actionReason = null, limitation = null, provider = "mock",
    )

    private fun store() = CoachingCacheStore(Files.createTempDirectory("coach-cache").toFile())

    @Test fun `저장한 결과를 같은 계정 · 같은 열쇠로만 다시 읽는다`() = runBlocking {
        val cache = store()
        val key = CoachingCacheStore.runKey(1_760_400_000_000, "ko")
        cache.put("account:A", key, result, savedAt = 99)
        val back = cache.get("account:A", key)!!
        assertEquals(result, back.result)
        assertEquals(99L, back.savedAt)
        assertNull("다른 계정은 읽지 못한다", cache.get("account:B", key))
        assertNull("다른 언어는 다른 사본", cache.get("account:A", CoachingCacheStore.runKey(1_760_400_000_000, "en")))
    }

    @Test fun `로컬 기록을 지우면 그 기록의 모든 언어 사본을 지운다 — 다른 기록 · 주간은 남는다`() = runBlocking {
        val cache = store()
        cache.put("account:A", CoachingCacheStore.runKey(1, "ko"), result, 1)
        cache.put("account:A", CoachingCacheStore.runKey(1, "en"), result, 2)
        cache.put("account:A", CoachingCacheStore.runKey(10, "ko"), result, 3)
        cache.put("account:A", CoachingCacheStore.weekKey(java.time.LocalDate.of(2026, 10, 5), "Asia/Seoul", "ko"), result, 4)
        cache.removeRun("account:A", 1)
        assertNull(cache.get("account:A", CoachingCacheStore.runKey(1, "ko")))
        assertNull(cache.get("account:A", CoachingCacheStore.runKey(1, "en")))
        assertTrue(cache.get("account:A", CoachingCacheStore.runKey(10, "ko")) != null)
        assertTrue(cache.get("account:A", CoachingCacheStore.weekKey(java.time.LocalDate.of(2026, 10, 5), "Asia/Seoul", "ko")) != null)
    }

    @Test fun `해제하며 삭제를 고르면 그 계정의 사본만 지운다`() = runBlocking {
        val cache = store()
        cache.put("account:A", "rec:2026-10-12:ko", result, 1)
        cache.put("account:B", "rec:2026-10-12:ko", result, 1)
        cache.clear("account:A")
        assertNull(cache.get("account:A", "rec:2026-10-12:ko"))
        assertTrue(cache.get("account:B", "rec:2026-10-12:ko") != null)
    }

    @Test fun `계정마다 최근 것만 둔다`() = runBlocking {
        val cache = store()
        repeat(CoachingCacheStore.MAX_ENTRIES + 5) { cache.put("account:A", "rec:$it:ko", result, savedAt = it.toLong()) }
        assertNull(cache.get("account:A", "rec:0:ko"))
        assertTrue(cache.get("account:A", "rec:${CoachingCacheStore.MAX_ENTRIES + 4}:ko") != null)
    }

    @Test fun `깨진 사본 파일은 결과로 보이지 않는다`() = runBlocking {
        val dir = Files.createTempDirectory("coach-cache").toFile()
        val cache = CoachingCacheStore(dir)
        cache.put("account:A", "rec:x:ko", result, 1)
        dir.listFiles()!!.single().writeText("{ broken")
        assertNull(cache.get("account:A", "rec:x:ko"))
    }
}
