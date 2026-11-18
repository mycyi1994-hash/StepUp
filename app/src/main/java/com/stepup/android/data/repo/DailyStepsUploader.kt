package com.stepup.android.data.repo

import com.stepup.android.data.remote.DaySteps
import com.stepup.android.data.remote.ServerResult
import java.time.LocalDate
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

/**
 * 하루 걸음을 서버에 올린다(`steps_sync`, 서버 0014) — 2026-11-18 사용자 "하루 걸음 체인 기록 안 나가는 것도 고쳐".
 *
 * 서버의 하루 걸음 기록(체인 DAILY_STEPS — 목표 달성 여부만, 걸음 수는 체인에 올리지 않는다, 0078)은 `daily_steps` 표에 그날
 * 줄이 처음 생길 때 줄을 세우고, 그날이 끝난 뒤(한국 자정 + 10분)에 보낸다. 앱이 이 값을 한 번도 올리지 않아 기록이 0 건이었다
 * (`EventApi.syncSteps` 는 있었지만 부르는 곳이 없었다).
 *
 * 어제 · 오늘(이 폰의 날짜) 두 날만 보낸다 — 서버도 그 두 날만 체인에 줄 세운다. 같은 값을 다시 보내지 않고(바뀐 날만),
 * 못 보냈으면 다음에 다시 한다. 서버는 받은 걸음 가운데 큰 값을 남긴다.
 */
class DailyStepsUploader(
    private val readDay: suspend (epochDay: Long) -> Pair<Int, Int>?,
    private val send: suspend (List<DaySteps>) -> ServerResult<Unit>,
    private val today: () -> Long = { LocalDate.now().toEpochDay() },
    /** 지금 로그인한 계정 — 계정이 바뀌면 처음부터 다시 보낸다(보낸 기억은 계정마다) */
    private val account: suspend () -> String? = { null },
) {
    private val lock = Mutex()

    /** 마지막으로 서버가 받은 값 — 날 → (걸음, 목표). 같은 값이면 보내지 않는다 */
    private val sent = mutableMapOf<Long, Pair<Int, Int>>()
    private var sentFor: String? = null

    /** 어제 · 오늘을 올린다. 보낼 것이 없거나 서버가 받았으면 true */
    suspend fun upload(): Boolean = lock.withLock {
        val who = account()
        if (who != sentFor) {
            sent.clear()
            sentFor = who
        }
        val day = today()
        val days = listOf(day - 1, day).mapNotNull { d ->
            readDay(d)?.takeIf { (steps, _) -> steps > 0 }?.let { d to it }
        }.filter { (d, value) -> sent[d] != value }
        if (days.isEmpty()) return@withLock true
        val ok = send(days.map { (d, v) -> DaySteps(d, v.first, v.second) }) is ServerResult.Ok
        if (ok) {
            days.forEach { (d, v) -> sent[d] = v }
            // 지난 날은 다시 보내지 않는다 — 기억도 두 날만
            sent.keys.removeAll { it < day - 1 }
        }
        ok
    }

    /** 로그아웃 · 계정 바뀜 — 다음 계정에는 다시 보낸다 */
    suspend fun forget() = lock.withLock { sent.clear() }
}
