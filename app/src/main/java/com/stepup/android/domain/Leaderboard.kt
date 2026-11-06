package com.stepup.android.domain

/*
 * 리더보드 세 화면(2026-10-17 전달본 — 크루 순위 · 개인 러닝 순위 · 주간 포인트)이 그리는 모양.
 *
 * 서버가 매긴 순위 · 앞뒤 · 기간을 그대로 담는다 — 받은 목록의 몇 번째인지로 순위를 셈하지 않는다. 화면이 더 계산하는 것은
 * 전주 대비 변화와 바로 앞과의 차이뿐이고, 그 규칙(없는 값은 지어내지 않는다)은 [LeaderboardMath] 한 곳에 있다.
 */

/** 집계 기간 — 시작(포함) · 끝(제외) 날짜와 그 시간대(서버가 정한 주) */
data class LeaderPeriod(
    val startDate: java.time.LocalDate,
    val endDateExclusive: java.time.LocalDate,
    val timezone: String,
) {
    /** 화면의 "10.12–10.18" — 끝은 마지막 날(제외 날짜의 전날) */
    val lastDate: java.time.LocalDate get() = endDateExclusive.minusDays(1)
}

/** 순위 한 줄 — 사람 또는 크루 */
data class LeaderEntry(
    /** 서버의 바뀌지 않는 식별자(사용자 · 크루 id) */
    val id: String,
    val name: String,
    /** 그림이 없을 때 쓰는 머리글자(크루 모노그램 · 이름 첫 글자) */
    val mark: String,
    /** 서버 순위. 순위에 들지 않았으면 null */
    val rank: Int?,
    /** 지난 기간의 순위. 없으면 null(0 이 아니다) */
    val previousRank: Int? = null,
    /** 정렬에 쓰인 값 그대로(km · 포인트) — 화면은 반올림해 보이기만 한다 */
    val metric: Double,
    val isMe: Boolean = false,
    /** 크루 — 이번 주 달린 크루원 수 */
    val participants: Int? = null,
    /** 사람 — 이번 주 러닝 수 */
    val runs: Int? = null,
    /** 크루 — 대표 사진 판(사진이 없으면 null) · 이름 그림 바탕 */
    val photoVersion: Int? = null,
    val tint: Int = 0,
    /** 줄 아래 작은 글(소속 크루 등) — 없으면 null */
    val sub: String? = null,
)

/** 이번 주 내 러닝 하나(무효 제외) — 기록 상세 시트 */
data class LeaderRun(val startedAt: java.time.Instant, val km: Double)

/** 개인 러닝 순위(서버 weekly_runner_board) — 한 번에 같은 집계로 온 값 */
data class RunnerBoard(
    val period: LeaderPeriod,
    /** 순위에 든 전체 인원 */
    val total: Int,
    /** 내 줄 — 이번 주에 달리지 않았으면 null(순위 없음) */
    val me: LeaderEntry?,
    /** 바로 앞 러너 — 내가 1위이거나 순위가 없으면 null */
    val ahead: LeaderEntry?,
    /** 내 앞 둘 · 나 · 뒤 둘 */
    val nearby: List<LeaderEntry>,
    /** 상위 러너 */
    val top: List<LeaderEntry>,
    /** 내 이름 · 이번 주 내 거리 · 러닝 수 · 러닝 목록(순위가 없어도 온다) */
    val myName: String?,
    val myKm: Double,
    val myRuns: Int,
    val myRunList: List<LeaderRun>,
)

/** 크루 순위(서버 weekly_crew_board) */
data class CrewBoard(
    val period: LeaderPeriod,
    val total: Int,
    val top: List<LeaderEntry>,
    /** 우리 크루(대표 크루) — 이번 주 기록이 없으면 rank null. 크루가 없으면 null */
    val mine: LeaderEntry?,
    /** 우리 크루 앞뒤 둘 */
    val nearby: List<LeaderEntry>,
)

/** 주간 포인트 순위의 범위 */
enum class PointScope { ALL, CREW }

/** 주간 포인트 순위 — 서버에 아직 없다(P 가 무엇인지 정해지지 않았다). 개발용 고정 값만 이 모양으로 온다 */
data class PointsBoard(
    val period: LeaderPeriod,
    val scope: PointScope,
    /** 우리 크루 범위일 때 크루 이름 */
    val crewName: String?,
    val top: List<LeaderEntry>,
    val me: LeaderEntry?,
    val ahead: LeaderEntry?,
    val nearby: List<LeaderEntry>,
    /** 내 이름 · 이번 주 내 적립 · 지난주 적립(없으면 null — 0 으로 치지 않는다) */
    val myName: String?,
    val myPoints: Double,
    val lastWeekPoints: Double?,
)

/** 화면이 보일 수 있는 상태 — 순위 없음 · 못 가져옴 · 준비 중을 서로 다르게 보인다 */
sealed interface LeaderLoad<out T> {
    data object Loading : LeaderLoad<Nothing>
    data class Ready<T>(val value: T) : LeaderLoad<T>

    /** 서버가 이 순위를 아직 주지 않는다(연결할 API 없음) — 지어낸 값을 보이지 않는다 */
    data object Unavailable : LeaderLoad<Nothing>

    /** 로그인이 필요하다 */
    data object SignedOut : LeaderLoad<Nothing>

    /** 통신 실패 — 다시 시도할 수 있다 */
    data class Failed(val offline: Boolean) : LeaderLoad<Nothing>
}

/** 화면이 더 계산하는 값 — 서버 값이 없으면 null(꾸며 내지 않는다) */
object LeaderboardMath {
    /** 지난 기간 대비 순위 변화 — 양수 상승 · 음수 하락 · 0 그대로. 둘 중 하나라도 없으면 null(신규 · 미집계) */
    fun rankRise(current: Int?, previous: Int?): Int? =
        if (current == null || previous == null || current <= 0 || previous <= 0) null else previous - current

    /**
     * 바로 앞 순위와의 차이 — 같은 집계에서 앞사람 값 − 내 값. 앞사람이 없으면(1위) null — "0위"를 만들지 않는다.
     * 0 이면 동점이다(차이를 꾸며 내지 않는다).
     */
    fun gapToNext(own: Double?, next: Double?): Double? =
        if (own == null || next == null) null else (next - own).coerceAtLeast(0.0)

    /** 지난주 대비 적립 변화 — 지난주 값이 없으면 null(0 으로 쳐서 부풀리지 않는다) */
    fun delta(current: Double?, previous: Double?): Double? =
        if (current == null || previous == null) null else current - previous

    /** 표시 — km 소수 한 자리(정렬은 원래 값으로 이미 끝났다) */
    fun km(value: Double): String = "%.1f".format(java.util.Locale.ROOT, value)

    /**
     * 차이 표시 — km 소수 한 자리. 0 보다 크면 올려서 보인다(0.04km 를 "0.0" 으로 보여 동점처럼 읽히지 않게).
     * 0 은 동점이라 따로 말한다.
     */
    fun gapKm(gap: Double): String =
        if (gap <= 0.0) km(0.0) else km(kotlin.math.ceil(gap * 10.0 - 1e-9) / 10.0)

    /** 표시 — 포인트 정수, 천 단위 쉼표 */
    fun points(value: Double): String = "%,d".format(java.util.Locale.ROOT, kotlin.math.floor(value + 1e-9).toLong())

    /** 사람 머리글자 — 한글 · 한자 · 가나 이름은 첫 글자, 로마자는 서버 머리글자(두 글자까지) */
    fun personMark(name: String, serverMark: String?): String {
        val first = name.trim().firstOrNull() ?: return serverMark?.take(2).orEmpty()
        val wide = Character.UnicodeScript.of(first.code).let {
            it == Character.UnicodeScript.HANGUL || it == Character.UnicodeScript.HAN ||
                it == Character.UnicodeScript.HIRAGANA || it == Character.UnicodeScript.KATAKANA
        }
        return if (wide) first.toString() else (serverMark?.takeIf { it.isNotBlank() } ?: first.uppercase()).take(2)
    }
}
