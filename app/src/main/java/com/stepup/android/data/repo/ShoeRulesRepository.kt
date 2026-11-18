package com.stepup.android.data.repo

import com.stepup.android.data.remote.ServerResult
import com.stepup.android.domain.BoxStatus
import com.stepup.android.domain.ShoeRules
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * 경제 4판(서버 0116)의 규칙 · 상자 현황을 앱 한 곳에 둔다 — 신발 화면 · 포인트 · 수리 · 강화 · 출발 전 점검 · 뽑기가 같은 값을 본다.
 *
 * 규칙은 서버 `shoe_rules_v4`(등급 범위 · 포인트 · 수리 몫 · 무료 수리 · 운). 아직 못 읽었으면 [ShoeRules.FALLBACK](서버 0116 과 같은 값).
 * 동기화 때(EconomySync 뒤) · 수리 · 뽑기 뒤에 [refresh] 한다. 서버에 함수가 없으면(예전 서버) 그대로 둔다.
 */
class ShoeRulesRepository(
    private val fetchRules: suspend () -> ServerResult<ShoeRules>,
    private val fetchBoxes: suspend () -> ServerResult<BoxStatus>,
) {
    private val _rules = MutableStateFlow(ShoeRules.FALLBACK)
    val rules: StateFlow<ShoeRules> = _rules.asStateFlow()

    private val _boxes = MutableStateFlow<BoxStatus?>(null)
    /** 상자 현황 — 로그인 전 · 아직 못 읽었으면 null */
    val boxes: StateFlow<BoxStatus?> = _boxes.asStateFlow()

    /** 지금 값 — 화면 밖(계산)에서 읽을 때 */
    val now: ShoeRules get() = _rules.value

    suspend fun refresh(): Boolean {
        val r = fetchRules()
        if (r is ServerResult.Ok) _rules.value = r.value
        return r is ServerResult.Ok
    }

    suspend fun refreshBoxes(): BoxStatus? {
        val r = fetchBoxes()
        if (r is ServerResult.Ok) _boxes.value = r.value
        return (r as? ServerResult.Ok)?.value
    }

    /** 로그아웃 · 계정 바뀜 — 계정에 묶인 값(무료 수리 · 상자)을 비운다 */
    fun forgetAccount() {
        _rules.value = _rules.value.copy(freeRepairReady = null, freeRepairNextMs = null)
        _boxes.value = null
    }
}
