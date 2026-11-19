package com.stepup.android.data.repo

import android.content.Context
import com.stepup.android.core.ServiceLocator

/**
 * 나온 뒤 다시 들어간 함께 뛰기 방 — 이 폰에 적어 둔다(최근 [KEEP] 개).
 *
 * 대기실은 "나온 방"(UserPrefs.exitedParties — 나가기가 서버에 닿지 않았어도 그 방에 말없이 다시 붙지 않게)을 기억한다.
 * 그런데 나온 방에 사람이 다시 들어간 뒤에도 그 목록에 남아, 앱이 그 방을 다시 열 때(되살린 화면 등) 먼저 나가기를 보내
 * 준비 · 진행자를 잃거나 혼자 있던 방이 닫혔다(2026-11-18 점검). 다시 들어간 방을 여기 적어 "나온 방"에서 빼고 본다 —
 * 그 방에서 다시 나오면(러닝을 마치면) 지운다.
 */
internal object PartyReturns {
    private const val PREFS = "party_returns"
    private const val KEY = "ids"
    private const val KEEP = 20

    private fun prefs() = ServiceLocator.appContext.getSharedPreferences(PREFS, Context.MODE_PRIVATE)

    /** 다시 들어가 있는 방들 — 읽지 못하면 없다(예전처럼 "나온 방"을 그대로 본다) */
    fun all(): Set<Long> = runCatching { parse(prefs().getString(KEY, "").orEmpty()).toSet() }.getOrDefault(emptySet())

    fun add(id: Long) {
        runCatching { save(added(parse(prefs().getString(KEY, "").orEmpty()), id)) }
    }

    fun remove(id: Long) {
        runCatching {
            val now = parse(prefs().getString(KEY, "").orEmpty())
            if (id in now) save(now - id)
        }
    }

    private fun save(ids: List<Long>) {
        prefs().edit().putString(KEY, ids.joinToString(",")).apply()
    }

    internal fun parse(raw: String): List<Long> = raw.split(',').mapNotNull { it.trim().toLongOrNull() }

    /** [id] 를 맨 뒤에 둔다(이미 있으면 옮긴다) — 가장 오래된 것부터 버린다 */
    internal fun added(ids: List<Long>, id: Long): List<Long> = (ids.filter { it != id } + id).takeLast(KEEP)
}
