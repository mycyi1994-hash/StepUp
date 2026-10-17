package com.stepup.android.data.repo

import com.stepup.android.data.prefs.OverspeedStop
import com.stepup.android.data.remote.ServerResult
import com.stepup.android.domain.RecordingOwner

/**
 * 과속으로 멈춘 러닝을 서버에 알린다(2026-10-17 사용자 결정 — 패널티: 신발 남은 내구도 절반 · 10분 러닝 금지, 서버 0065).
 *
 * 러닝이 끝나는 곳에 신호가 있다는 보장이 없어 폰에 적어 두고(UserPrefs) 올리기 일꾼이 러닝보다 **먼저** 보낸다 —
 * 서버가 금지 시간을 알아야 그 사이에 시작한 러닝을 무효로 둘 수 있고, 멈춘 러닝에 까닭을 남길 수 있다.
 * 무효로 끝나 올라가지 않는 러닝(처음부터 차에 탄 경우)도 신고는 간다.
 */
class OverspeedReporter(
    private val read: suspend () -> List<OverspeedStop>,
    private val acknowledge: suspend (startedAt: Long) -> Unit,
    private val currentOwner: suspend () -> String,
    private val report: suspend (startedAt: Long, stoppedAt: Long, userId: String) -> ServerResult<Unit>,
    private val configured: () -> Boolean = { true },
    /** 서버가 하나라도 받았다 — 신발 내구도가 바뀌었으니 다시 받아 온다 */
    private val onReported: suspend () -> Unit = {},
) {
    /**
     * 쌓인 신고를 보낸다. 끊겨 다시 해 볼 것이 남았으면 false — 일꾼이 러닝을 올리지 않고 다시 시도한다.
     * 다른 계정이 기록한 러닝의 신고는 그 계정으로 로그인할 때까지 남겨 두고 막지 않는다(그 계정의 러닝도 지금 올라가지 않는다).
     */
    suspend fun flush(): Boolean {
        // 서버 주소가 없는 빌드(로컬 개발) — 보낼 곳이 없다. 러닝 올리기와 같이 물러난다
        if (!configured()) return true
        val owner = currentOwner()
        var clear = true
        var reported = false
        for (stop in read()) {
            if (stop.owner != owner) continue
            val userId = RecordingOwner.userId(stop.owner)
            if (userId == null) {
                // 로그인 전에 기록한 러닝 — 서버의 어느 계정에도 깎을 신발이 없다
                acknowledge(stop.startedAt)
                continue
            }
            when (val result = report(stop.startedAt, stop.stoppedAt, userId)) {
                is ServerResult.Ok -> {
                    acknowledge(stop.startedAt)
                    reported = true
                }
                // 서버가 받을 수 없다고 답했다(7일 지남 · 시각이 맞지 않음) — 다시 보내도 같다. 다만 서버에 아직 이 함수가 없으면
                // (앱이 서버 0065 보다 먼저 깔렸다) 지우지 않고 다음에 보낸다 — 러닝 올리기는 막지 않는다
                is ServerResult.Rejected -> if (FUNCTION !in result.reason) acknowledge(stop.startedAt)
                is ServerResult.Retry -> clear = false
                // 다시 로그인해야 한다 — 남겨 둔다. 러닝 올리기도 같은 까닭으로 멈추고 화면이 "다시 로그인"을 보인다
                is ServerResult.SignInRequired -> Unit
            }
        }
        if (reported) runCatching { onReported() }
        return clear
    }

    private companion object {
        const val FUNCTION = "report_overspeed_stop"
    }
}
