package com.stepup.android.domain.coaching

/*
 * AI 코칭(2026-10-16 전달본, docs/redesign/ai-coaching-2026-10) — 저장된 러닝 하나의 1km 구간.
 *
 * 구간 시간은 러닝 중 찍힌 자동 랩의 누적 "활성" 시간 차로만 센다 — 일시정지 · 앱이 죽어 있던 동안은 랩 시간에도
 * 기록 시간(durationSec)에도 들어가지 않는다. GPS 경로의 시각은 벽시계라 일시정지가 섞이고 일시정지 구간은 남지 않으므로
 * 경로로 구간을 다시 만들지 않는다. 거리는 기록의 거리와 같은 걸음 거리(걸음 × 0.762m)라 구간의 합이 기록과 맞는다.
 */

/** 저장된 랩 한 줄 — 서비스의 RunLap 과 같은 값(순번 · 랩을 찍을 때의 누적 km · 누적 활성 초) */
data class SavedLap(val index: Int, val km: Double, val splitSec: Long)

/**
 * walk_sessions.laps 글자 — `순번,누적km,누적초;…`(예: `1,1.0,432;2,2.0,878`). 경로(RunTrack)처럼 사람이 열어 읽을 수 있게 둔다.
 * 숫자는 Kotlin 의 기본 글자(로캘과 무관 — 소수점은 늘 '.')다.
 */
object LapCodec {
    private const val LAP_SEPARATOR = ';'
    private const val FIELD_SEPARATOR = ','

    fun encode(laps: List<SavedLap>): String =
        laps.joinToString(LAP_SEPARATOR.toString()) { "${it.index}$FIELD_SEPARATOR${it.km}$FIELD_SEPARATOR${it.splitSec}" }

    /**
     * 저장된 글자를 랩으로 되돌린다. 빈 글자는 빈 목록(랩을 남기기 전의 기록 · 1km 를 못 넘은 러닝).
     * 읽을 수 없는 조각이 하나라도 있으면 null — 일부만 읽은 랩으로 구간을 비교하지 않는다.
     */
    fun decode(raw: String): List<SavedLap>? {
        if (raw.isBlank()) return emptyList()
        return raw.split(LAP_SEPARATOR).map { chunk ->
            val parts = chunk.split(FIELD_SEPARATOR)
            if (parts.size != 3) return null
            val index = parts[0].toIntOrNull() ?: return null
            val km = parts[1].toDoubleOrNull()?.takeIf { it.isFinite() } ?: return null
            val sec = parts[2].toLongOrNull() ?: return null
            SavedLap(index, km, sec)
        }
    }
}

/** 완료한 1km 구간 하나 — [km] 번째 1km 를 [seconds] 초에 달렸다. 1km 이므로 초가 곧 페이스(초/km)다 */
data class KmSplit(val km: Int, val seconds: Long)

/** 마지막 1km 를 다 채우지 못한 남은 거리(예: 0.24km). 1km 구간과 견주지 않는다 */
data class PartialSplit(val meters: Double, val seconds: Long) {
    val paceSecPerKm: Double get() = seconds / (meters / 1000.0)
}

/** 구간을 보일 수 없는 까닭 — 화면은 까닭마다 다른 말을 한다(없는 것과 틀린 것을 같은 글자로 만들지 않는다) */
enum class SplitGap {
    /** 거리 · 시간이 없다(무효 러닝 · 걸음 없음) */
    NO_DISTANCE,

    /** 1km 를 다 채우지 못했다 — 견줄 1km 구간이 없다 */
    TOO_SHORT,

    /** 랩을 남기기 전(2026-10-16 이전)에 저장된 기록 — 구간 정보가 없다 */
    NOT_SAVED,

    /** 1km 경계가 아닌 랩(수동 랩)이 섞였다 — 자동 랩의 경계가 빠졌을 수 있다 */
    MANUAL_LAPS,

    /** 랩 수 · 시간이 기록의 거리 · 시간과 맞지 않는다(깨진 글자 포함) */
    MISMATCH,

    /** 사람 범위를 벗어난 구간이 있다 */
    OUT_OF_RANGE,
}

sealed interface RunSplits {
    data class Ready(val full: List<KmSplit>, val partial: PartialSplit?) : RunSplits
    data class Unavailable(val gap: SplitGap) : RunSplits
}

/**
 * 기록 하나의 1km 구간.
 *
 * 랩은 걸음 거리가 1km 를 넘을 때마다 `km = 순번` 으로 찍히고, 기록의 거리도 같은 걸음 거리다 — 그래서 랩 수는 기록 거리의
 * 정수 km 와 같아야 한다(서비스와 같은 식 `(m / 1000).toInt()`). 마지막 부분 구간은 `durationSec − 마지막 랩` 으로 따로 둔다.
 * 평균 페이스는 여기서 만들지 않는다 — 전체 시간 ÷ 전체 거리([com.stepup.android.domain.averagePace])다.
 */
fun runSplits(distanceMeters: Double, durationSec: Long, lapsRaw: String): RunSplits {
    if (!distanceMeters.isFinite() || distanceMeters <= 0.0 || durationSec <= 0L) return RunSplits.Unavailable(SplitGap.NO_DISTANCE)
    val wholeKm = (distanceMeters / 1000.0).toInt()
    if (wholeKm < 1) return RunSplits.Unavailable(SplitGap.TOO_SHORT)
    val laps = LapCodec.decode(lapsRaw) ?: return RunSplits.Unavailable(SplitGap.MISMATCH)
    if (laps.isEmpty()) return RunSplits.Unavailable(SplitGap.NOT_SAVED)
    if (laps.withIndex().any { (i, lap) -> lap.index != i + 1 || lap.km != (i + 1).toDouble() }) {
        return RunSplits.Unavailable(SplitGap.MANUAL_LAPS)
    }
    if (laps.size != wholeKm) return RunSplits.Unavailable(SplitGap.MISMATCH)
    var previous = 0L
    val full = laps.map { lap ->
        if (lap.splitSec <= previous || lap.splitSec > durationSec) return RunSplits.Unavailable(SplitGap.MISMATCH)
        val seconds = lap.splitSec - previous
        previous = lap.splitSec
        if (seconds !in CoachingQuality.MIN_SPLIT_SEC..CoachingQuality.MAX_SPLIT_SEC) {
            return RunSplits.Unavailable(SplitGap.OUT_OF_RANGE)
        }
        KmSplit(lap.index, seconds)
    }
    val restMeters = distanceMeters - wholeKm * 1000.0
    val restSeconds = durationSec - previous
    val partial = if (restMeters >= CoachingQuality.MIN_PARTIAL_METERS && restSeconds > 0) PartialSplit(restMeters, restSeconds) else null
    return RunSplits.Ready(full, partial)
}

/**
 * 분석 규칙의 값 — 한곳에 둔다. 서버(0059 `coaching.*`)도 같은 값을 쓴다(scripts/check-coaching-twin.py 가 맞대어 본다).
 * 실제 데이터로 정하기 전의 초안이다(지시서 14절 "데이터 품질 · 최소 표본 기준") — 사람이 낼 수 없는 값만 거른다.
 */
object CoachingQuality {
    const val RULES_VERSION = "quality-v1"

    /** 1km 를 2분 30초보다 빨리 — 걸음 수 오류로 본다 */
    const val MIN_SPLIT_SEC = 150L

    /** 1km 에 1시간 넘게 — 러닝 구간으로 보지 않는다 */
    const val MAX_SPLIT_SEC = 3_600L

    /** 이보다 짧은 마지막 남은 거리는 부분 구간으로 보이지 않는다(10m) */
    const val MIN_PARTIAL_METERS = 10.0

    /** 첫 · 마지막 1km 차이가 이 안이면 "비슷했어요" — 빨라졌다 · 느려졌다고 하지 않는다 */
    const val EVEN_SPLIT_SEC = 10L
}
