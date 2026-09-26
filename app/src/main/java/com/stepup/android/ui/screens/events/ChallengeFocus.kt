package com.stepup.android.ui.screens.events

import java.time.Instant
import java.time.ZoneId
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

enum class ChallengeKind { DAILY, WEEKLY, NIGHT }

/**
 * 챌린지 상세에서 "이 챌린지 달리기"로 시작한 러닝이 어느 챌린지를 향하는지(사용 피드백 9).
 *
 * [base] 는 시작할 때 서버가 확인해 둔 진행값이다. 러닝 중에는 이번 러닝의 걸음 · 거리를 더해
 * 예상 진행을 보인다 — 확정은 러닝이 서버에 저장 · 확인된 뒤 챌린지 화면의 값이다.
 */
data class ChallengeFocus(
    val kind: ChallengeKind,
    val base: Double,
    val target: Double,
) {
    /** 이번 러닝을 더한 예상 진행값. 나이트 러너는 저녁 8시(한국 시각) 이후에 시작한 러닝만 센다. */
    fun expected(sessionSteps: Int, sessionKm: Double, sessionStartedAt: Long): Double = when (kind) {
        ChallengeKind.DAILY, ChallengeKind.WEEKLY -> base + sessionSteps
        ChallengeKind.NIGHT -> base + if (startsAtNight(sessionStartedAt)) sessionKm else 0.0
    }

    fun fraction(value: Double): Float = if (target > 0) (value / target).toFloat().coerceIn(0f, 1f) else 0f

    companion object {
        private val SEOUL: ZoneId = ZoneId.of("Asia/Seoul")

        fun startsAtNight(startedAt: Long): Boolean =
            startedAt > 0 && Instant.ofEpochMilli(startedAt).atZone(SEOUL).hour >= 20
    }
}

/**
 * 지금 러닝이 향하는 챌린지 — 앱이 살아 있는 동안만. 홈에서 그냥 시작한 러닝이면 비운다.
 *
 * 처음 만난 러닝 하나에 묶는다([bind]). 그 러닝이 끝나고 다른 길(모임 · 알림)로 새 러닝을 시작하면
 * 예전 챌린지의 옛 기준값으로 막대를 그리지 않도록 비운다.
 */
object ChallengeRunFocus {
    private val _current = MutableStateFlow<ChallengeFocus?>(null)
    val current: StateFlow<ChallengeFocus?> = _current.asStateFlow()

    /** 묶인 러닝의 시작 시각. 아직 러닝을 만나지 않았으면 null */
    private var boundTo: Long? = null

    fun set(focus: ChallengeFocus) {
        boundTo = null
        _current.value = focus
    }

    fun clear() {
        boundTo = null
        _current.value = null
    }

    /** [startedAt] 러닝에 보여도 되는가 — 아직 묶이지 않았거나 그 러닝에 묶였을 때만 */
    fun matches(startedAt: Long): Boolean = _current.value != null && (boundTo == null || boundTo == startedAt)

    /** 달리는 중인 러닝을 만나면 부른다 — 처음이면 묶고, 다른 러닝이면 비운다 */
    fun bind(startedAt: Long) {
        if (_current.value == null || startedAt <= 0) return
        val bound = boundTo
        if (bound == null) boundTo = startedAt else if (bound != startedAt) clear()
    }
}
