package com.stepup.android.domain

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * 러닝 챌린지(시안 U02) — 준비된 목표. 서버 보상 챌린지(주간 걸음 · 나이트)와 다르다:
 * 이 목표는 러닝 한 번 안에서의 도전이고, 보상은 여느 러닝과 똑같이 서버 정산을 따른다.
 */
enum class RunGoal(val key: String, val seconds: Long? = null, val km: Double? = null) {
    TEN_MIN("10min", seconds = 600),
    ONE_KM("1km", km = 1.0),
    THREE_KM("3km", km = 3.0);

    val isDistance: Boolean get() = km != null

    /** 0~1 — 달린 시간 · 거리가 목표에 얼마나 다가갔나 */
    fun fraction(elapsedSec: Long, distanceKm: Double): Float = when {
        seconds != null -> (elapsedSec.toFloat() / seconds).coerceIn(0f, 1f)
        km != null -> (distanceKm / km).toFloat().coerceIn(0f, 1f)
        else -> 0f
    }

    fun reached(elapsedSec: Long, distanceKm: Double): Boolean = when {
        seconds != null -> elapsedSec >= seconds
        km != null -> distanceKm >= km - 1e-9
        else -> false
    }

    companion object {
        fun of(key: String?): RunGoal? = entries.firstOrNull { it.key == key }
    }
}

/** 러닝 경험(시안 U05) — 한 개만 고른다. 다이어트 루틴을 고르는 기준 */
enum class RunExperience(val key: String) {
    FIRST("first"), SOMETIMES("sometimes"), STEADY("steady");

    companion object {
        fun of(key: String?): RunExperience? = entries.firstOrNull { it.key == key }
    }
}

/** 이번 러닝이 무엇으로 시작됐나 — 자유 러닝 · 러닝 챌린지 · 다이어트 모드 */
sealed interface RunPlan {
    data object Free : RunPlan
    data class Goal(val goal: RunGoal) : RunPlan
    data class Diet(val experience: RunExperience) : RunPlan
}

/**
 * 시작 메뉴에서 고른 계획을 러닝 화면에 넘긴다. 처음 만난 러닝 하나에 묶는다([bind]) —
 * 그 러닝이 끝나고 다른 길(모임 · 알림 · 이어 달리기)로 새 러닝이 시작되면 자유 러닝으로 본다.
 * 프로세스가 죽었다 되살린 러닝은 자유 러닝으로 이어진다(기록 · 정산은 그대로다).
 */
object RunPlans {
    private val _current = MutableStateFlow<RunPlan>(RunPlan.Free)
    val current: StateFlow<RunPlan> = _current.asStateFlow()
    private var boundTo: Long? = null

    fun set(plan: RunPlan) {
        boundTo = null
        _current.value = plan
    }

    fun clear() = set(RunPlan.Free)

    /** [startedAt] 러닝의 계획 — 묶이지 않았거나 그 러닝에 묶였을 때만 */
    fun planFor(startedAt: Long): RunPlan {
        val bound = boundTo
        return if (bound == null || bound == startedAt) _current.value else RunPlan.Free
    }

    /** 달리는 중인 러닝을 만나면 부른다 — 처음이면 묶고, 다른 러닝이면 자유 러닝으로 되돌린다 */
    fun bind(startedAt: Long) {
        if (startedAt <= 0 || _current.value == RunPlan.Free) return
        val bound = boundTo
        if (bound == null) boundTo = startedAt else if (bound != startedAt) clear()
    }
}

/** 지난 도전 한 줄(시안 C03) — 저장한 러닝에서만 남긴다 */
data class GoalAttempt(
    val startedAt: Long,
    val goal: RunGoal,
    val achieved: Boolean,
    val elapsedSec: Long,
    val distanceKm: Double,
) {
    fun encode(): String = listOf(startedAt, goal.key, if (achieved) 1 else 0, elapsedSec, "%.3f".format(java.util.Locale.ROOT, distanceKm))
        .joinToString("\t")

    companion object {
        const val KEEP = 50

        fun decode(line: String): GoalAttempt? = runCatching {
            val p = line.split("\t")
            GoalAttempt(p[0].toLong(), RunGoal.of(p[1])!!, p[2] == "1", p[3].toLong(), p[4].toDouble())
        }.getOrNull()

        fun decodeAll(raw: String?): List<GoalAttempt> =
            raw.orEmpty().lineSequence().filter { it.isNotBlank() }.mapNotNull(::decode).toList()

        /** 같은 러닝은 한 줄 — 새 값이 이긴다. 최근 [KEEP] 개만, 최근 것이 앞 */
        fun merge(existing: List<GoalAttempt>, add: GoalAttempt): List<GoalAttempt> =
            (existing.filter { it.startedAt != add.startedAt } + add)
                .sortedByDescending { it.startedAt }.take(KEEP)
    }
}
