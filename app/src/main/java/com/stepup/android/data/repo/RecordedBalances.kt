package com.stepup.android.data.repo

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

/**
 * 서버가 러닝을 받으며 돌려준 "이 러닝까지 반영한 잔고" — 러닝 시작 시각별, 앱이 살아 있는 동안만.
 *
 * 러닝이 확인된 직후 원장 동기화가 실패하면 폰의 원장은 적립 전 잔고에 머문다. 그때 결과 화면이
 * "+N" 옆에 예전 잔고를 보이지 않도록, 그 뒤로 동기화가 성공하기 전까지는 이 값을 쓴다.
 */
object RecordedBalances {
    /** [owner] — 이 러닝을 올린 계정(서버 사용자 id). 다른 계정의 결과 화면에 쓰지 않는다 */
    data class Entry(val balance: Double, val recordedAt: Long, val owner: String)

    private val _byStart = MutableStateFlow<Map<Long, Entry>>(emptyMap())
    val byStart: StateFlow<Map<Long, Entry>> = _byStart.asStateFlow()

    fun put(startedAt: Long, owner: String, balance: Double, now: Long = System.currentTimeMillis()) {
        if (!balance.isFinite() || owner.isBlank()) return
        // 오래된 것은 버린다 — 결과 화면은 방금 끝난 러닝만 본다
        _byStart.update { (it + (startedAt to Entry(balance, now, owner))).entries.sortedByDescending { e -> e.value.recordedAt }.take(8).associate { e -> e.key to e.value } }
    }

    /**
     * 결과 화면에 보일 잔고. 이 러닝을 서버가 확인한 뒤로 원장 동기화가 성공하지 않았으면 서버가 돌려준 값,
     * 아니면 폰의 원장 값.
     */
    fun resultBalance(ledger: Double?, entry: Entry?, syncedFrom: Long, currentOwner: String?): Double? =
        if (entry != null && entry.owner == currentOwner && entry.recordedAt > syncedFrom) entry.balance else ledger

    /** 계정이 바뀌거나 지워졌다 — 앞 계정의 잔고를 남기지 않는다 */
    fun clear() {
        _byStart.value = emptyMap()
    }
}
