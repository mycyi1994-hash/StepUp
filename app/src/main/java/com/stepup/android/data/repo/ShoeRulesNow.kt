package com.stepup.android.data.repo

import com.stepup.android.core.ServiceLocator
import com.stepup.android.domain.ShoeRules

/**
 * 지금의 신발 규칙(경제 4판, 서버 0116) — 화면 밖(예상 보상 · 견적)에서 읽을 때. 앱이 아직 준비되지 않았으면(단위 시험 · 미리보기)
 * 서버 0116 과 같은 [ShoeRules.FALLBACK].
 */
fun shoeRulesNow(): ShoeRules = runCatching { ServiceLocator.shoeRules.now }.getOrDefault(ShoeRules.FALLBACK)

/** 수리 · 뽑기 뒤 규칙(무료 수리 · 다음 무료 수리 시각)을 다시 읽는다 — 앱이 준비되지 않았거나 읽지 못하면 그대로 둔다 */
suspend fun refreshShoeRules() {
    val repo = runCatching { ServiceLocator.shoeRules }.getOrNull() ?: return
    try {
        repo.refresh()
    } catch (cancelled: kotlinx.coroutines.CancellationException) {
        throw cancelled
    } catch (_: Exception) {
        // 읽지 못하면 예전 규칙 그대로 — 다음 동기화가 맞춘다
    }
}
