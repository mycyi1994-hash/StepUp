package com.stepup.android.domain

/**
 * 다이어트 모드 루틴(디자이너 전달본 U06 · D07~D13) — 러닝 경험별 3단계 고정(2026-09-27 사용자 결정).
 * 개인별 처방이나 추천 알고리즘이 아니다. 키 · 몸무게는 기록용이고 루틴을 바꾸지 않는다.
 *
 * 구간은 러닝의 운동 시간(일시정지 제외)에서 바로 계산한다 — 멈췄다 이어도, 화면을 다시 열어도
 * 같은 구간 · 남은 시간이 나온다. 구간은 시간이 끝나면 저절로 넘어간다.
 */
enum class DietSegmentKind { WARMUP, RUN, WALK, COOLDOWN }

data class DietSegment(val kind: DietSegmentKind, val seconds: Long, val round: Int = 0)

data class DietRoutine(val warmupSec: Long, val runSec: Long, val walkSec: Long, val rounds: Int, val cooldownSec: Long) {
    val segments: List<DietSegment> = buildList {
        add(DietSegment(DietSegmentKind.WARMUP, warmupSec))
        for (round in 1..rounds) {
            add(DietSegment(DietSegmentKind.RUN, runSec, round))
            add(DietSegment(DietSegmentKind.WALK, walkSec, round))
        }
        add(DietSegment(DietSegmentKind.COOLDOWN, cooldownSec))
    }

    val totalSec: Long = segments.sumOf { it.seconds }
    val totalRunSec: Long = runSec * rounds
    val totalWalkSec: Long = totalSec - totalRunSec

    /** 지금 구간 — 루틴을 다 마쳤으면 null */
    fun at(elapsedSec: Long): Position? {
        var start = 0L
        segments.forEachIndexed { index, segment ->
            val end = start + segment.seconds
            if (elapsedSec < end) {
                return Position(index, segment, remainingSec = end - elapsedSec,
                    fraction = ((elapsedSec - start).toFloat() / segment.seconds).coerceIn(0f, 1f))
            }
            start = end
        }
        return null
    }

    fun finished(elapsedSec: Long): Boolean = elapsedSec >= totalSec

    /** 끝까지 마친 구간 수 — 중간 종료 결과(D13)의 "2 / 8" */
    fun completedSegments(elapsedSec: Long): Int {
        var start = 0L
        var done = 0
        for (segment in segments) {
            start += segment.seconds
            if (elapsedSec >= start) done++ else break
        }
        return done
    }

    fun next(index: Int): DietSegment? = segments.getOrNull(index + 1)

    data class Position(val index: Int, val segment: DietSegment, val remainingSec: Long, val fraction: Float)

    companion object {
        fun forExperience(experience: RunExperience): DietRoutine = when (experience) {
            // 준비 걷기 3분 + (러닝 1분 + 걷기 2분) × 3 + 마무리 걷기 3분 = 15분
            RunExperience.FIRST -> DietRoutine(180, 60, 120, 3, 180)
            // 같은 구성 × 4 = 18분
            RunExperience.SOMETIMES -> DietRoutine(180, 60, 120, 4, 180)
            // 준비 3분 + (러닝 2분 + 걷기 1분) × 5 + 마무리 3분 = 21분
            RunExperience.STEADY -> DietRoutine(180, 120, 60, 5, 180)
        }
    }
}
