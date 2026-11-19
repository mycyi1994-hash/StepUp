package com.stepup.android.data.repo

import android.content.Context
import com.stepup.android.core.ServiceLocator

/**
 * 이 폰이 달린(달리고 있는) 크루 러닝의 방 — 읽기만 한다.
 *
 * 러닝 상태 알림([CrewRunRepository.startBeats])이 달리는 크루 러닝의 방 번호를 적어 둔다(앱을 다시 켜도 같은 러닝으로 이어 가게).
 * 대기실 쪽([CrewRepository])은 "출발했는데 달리지 않은 방"의 오늘 크루 러닝을 돌려받기 전에 이것을 본다 — 이 폰이 달린 러닝
 * (아직 올리기 전일 수 있다)의 기회를 돌려받지 않게. 서버도 달린 기록이 있으면 돌려주지 않는다(crew_run_release) — 이것은 한 겹 더다.
 */
internal object CrewRunLedger {
    private const val PREFS = "crew_run_beats"
    private const val KEY_PARTY = "party_id"
    private const val KEY_END_PARTY = "end_party_id"

    /** 이 폰이 [partyId] 방의 크루 러닝을 달렸는가 — 읽지 못하면 달린 것으로 본다(돌려받지 않는 쪽이 안전하다) */
    fun ran(partyId: Long): Boolean = runCatching {
        val prefs = ServiceLocator.appContext.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        prefs.getLong(KEY_PARTY, 0L) == partyId || prefs.getLong(KEY_END_PARTY, 0L) == partyId
    }.getOrDefault(true)
}
