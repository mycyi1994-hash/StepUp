package com.stepup.android.domain

import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import kotlin.math.roundToInt

/*
 * 내 크루 홈(확정 4번, 2026-09-29) — 가입한 크루의 홈과 거기서 여는 상세(모임 · 참석 · 주간 기록 · 러닝 기록)가
 * 서버 값에서 읽어 내는 규칙. 시안의 퇴근런 · 준호 · 126km 같은 예시 값은 여기 없다.
 *
 * 레벨 승급 기준은 정해지지 않아 계산하지 않는다(다음 레벨 자리는 "승급 기준 준비 중"). 모임 정원은 모임 글의 정원이고
 * 크루 정원과 다르다. 주간 기록은 서버가 한국 시간 월요일부터 센 값이다.
 */

/** 참석 응답 — 사람마다 모임 하나에 하나 */
enum class MeetingResponse {
    YES, NO;

    companion object {
        fun of(code: String?): MeetingResponse? = entries.firstOrNull { it.name == code }
    }
}

/** 모임 카드의 얼굴 — 진행자 먼저 */
data class MeetingFace(val userId: String, val name: String)

/**
 * 크루 모임 하나(크루 게시판의 번개러닝 글). 인원 · 내 응답은 서버가 센 값이다 — 누른 순간 앱이 더하지 않는다.
 * [open] 이 거짓이면 이미 시작해 응답을 받지 않는다(서버 상태).
 */
data class CrewMeeting(
    val id: Long,
    val crewId: String,
    val title: String,
    /** 진행자의 메모 */
    val body: String,
    val place: String,
    val lat: Double?,
    val lng: Double?,
    val distanceKm: Double,
    val meetAt: Long?,
    /** 모임 글의 정원. 0 이면 제한 없음(크루 정원과 다르다) */
    val capacity: Int,
    val hostId: String,
    val hostName: String,
    val hostOwner: Boolean,
    val attendees: Int,
    val faces: List<MeetingFace>,
    val myResponse: MeetingResponse?,
    val open: Boolean,
) {
    val point: GeoPoint? get() = if (lat != null && lng != null) GeoPoint(lat, lng) else null

    /** 정원이 찼다 — 이미 참석한 사람은 그대로 불참으로 바꿀 수 있다 */
    val full: Boolean get() = capacity > 0 && attendees >= capacity
}

/** 13 참석자 한 줄 */
data class MeetingAttendee(val userId: String, val name: String, val host: Boolean, val owner: Boolean)

/**
 * 00 홈의 나머지(크루 명함은 따로) — 내 역할 · 채팅 미확인 · 다음 러닝 · 이번 주 함께 · 대표 공지.
 * 모임이 없으면 [meeting] 이 null, 공지가 없으면 [notice] 가 null — 둘은 서로와 주간 기록에 영향을 주지 않는다.
 */
data class CrewHomeSummary(
    val crewId: String,
    val owner: Boolean,
    val ownerId: String,
    val memberCount: Int,
    val unread: Int,
    val meeting: CrewMeeting?,
    val weekKm: Double,
    val weekRunners: Int,
    /** 지금의 주간 목표. 없으면 나누지 않는다 */
    val goalKm: Int?,
    val notice: ChatNotice?,
) {
    val progress: CrewGoalProgress? get() = goalKm?.takeIf { it > 0 }?.let { CrewGoalProgress(it, weekKm) }
}

/** 15 함께 달린 크루원 한 줄 — 그 주의 거리와 마지막 러닝 */
data class CrewWeekMember(
    val userId: String,
    val name: String,
    val km: Double,
    val runs: Int,
    val lastKm: Double?,
    val lastAt: Long?,
)

/** 15 · 17 · 24 한 주의 크루 기록 */
data class CrewWeek(
    val start: LocalDate,
    val thisWeek: Boolean,
    /** 지금의 목표(지난주를 봐도 지금 값) */
    val goalKm: Int?,
    val km: Double,
    val runners: Int,
    /** 월 → 일. null 은 아직 오지 않은 날(0km 와 다르다) */
    val days: List<Double?>,
    val members: List<CrewWeekMember>,
    /** 고를 수 있는 주 — 이번 주부터 거슬러 */
    val weeks: List<LocalDate>,
) {
    val end: LocalDate get() = start.plusDays(6)

    val progress: CrewGoalProgress? get() = goalKm?.takeIf { it > 0 }?.let { CrewGoalProgress(it, km) }

    /** 집계는 됐고 기록이 없다(24) — 읽기 실패와 다르다 */
    val empty: Boolean get() = runners == 0 && km <= 0.0
}

/** 크루로 적은 러닝 한 건 — 크루원에게 공개된 값만 */
data class CrewRun(
    val id: Long,
    val userId: String,
    val name: String,
    val distanceM: Double,
    val durationS: Int,
    val startedAt: Long,
    val endedAt: Long,
    /** 서버가 준 끝난 시각 그대로 — 이어 읽기의 기준(밀리초로 줄이면 같은 때 끝난 기록을 건너뛸 수 있다) */
    val endedCursor: String = "",
) {
    val km: Double get() = distanceM / 1000.0

    /** 1km 에 걸린 초 — 너무 짧은 기록은 만들지 않는다 */
    val paceSecPerKm: Int? get() = CrewHomeRules.pace(distanceM, durationS)
}

/** 19 러닝 기록 — 코스는 서버가 처음과 끝 300m 를 떼고 준다(비었으면 지도를 그리지 않는다) */
data class CrewRunDetail(val run: CrewRun, val crewId: String, val route: List<GeoPoint>)

/** 18 참여 기록의 범위 — 고른 주(없으면 이번 주) · 한 날 · 한 사람 */
data class CrewRunsScope(val week: LocalDate? = null, val day: LocalDate? = null, val userId: String? = null)

object CrewHomeRules {
    /** 서버의 주 기준 — 한국 시간 */
    val KST: ZoneId = ZoneId.of("Asia/Seoul")

    /** 모임을 오늘 · 내일 · 그 밖으로 말한다 */
    enum class When { TODAY, TOMORROW, LATER }

    fun whenOf(meetAt: Long, now: Long, zone: ZoneId): When {
        val day = Instant.ofEpochMilli(meetAt).atZone(zone).toLocalDate()
        val today = Instant.ofEpochMilli(now).atZone(zone).toLocalDate()
        return when (day) {
            today -> When.TODAY
            today.plusDays(1) -> When.TOMORROW
            else -> When.LATER
        }
    }

    /** 요일 막대 높이(0..1) — 값이 있는 날의 가장 큰 값에 맞춘다. 오지 않은 날은 null */
    fun bars(days: List<Double?>): List<Float?> {
        val max = days.filterNotNull().maxOrNull() ?: 0.0
        return days.map { day -> day?.let { if (max <= 0.0) 0f else (it / max).toFloat().coerceIn(0f, 1f) } }
    }

    /** 1km 에 걸린 초 — 50m 가 안 되거나 시간이 없으면 null */
    fun pace(distanceM: Double, durationS: Int): Int? =
        if (distanceM < 50.0 || durationS <= 0) null else (durationS / (distanceM / 1000.0)).roundToInt()

    /** 7'00" */
    fun paceText(secPerKm: Int): String = "%d'%02d\"".format(secPerKm / 60, secPerKm % 60)

    /** 36:24 — 한 시간을 넘으면 1:02:03 */
    fun clock(durationS: Int): String {
        val s = durationS.coerceAtLeast(0)
        val h = s / 3600
        val m = s % 3600 / 60
        val sec = s % 60
        return if (h > 0) "%d:%02d:%02d".format(h, m, sec) else "%d:%02d".format(m, sec)
    }

    /** 서버 코스 글자("위도,경도;…") → 점. 읽을 수 없는 점은 뺀다 */
    fun route(text: String): List<GeoPoint> = text.split(';').mapNotNull { chunk ->
        val parts = chunk.split(',')
        val lat = parts.getOrNull(0)?.trim()?.toDoubleOrNull()
        val lng = parts.getOrNull(1)?.trim()?.toDoubleOrNull()
        if (lat == null || lng == null || lat !in -90.0..90.0 || lng !in -180.0..180.0) null else GeoPoint(lat, lng)
    }

    /** 이름으로 찾기 — 앞뒤 공백을 떼고 대소문자 없이, 이름 안 어디든 */
    fun matches(name: String, query: String): Boolean {
        val q = query.trim()
        return q.isEmpty() || name.contains(q, ignoreCase = true)
    }

    /** 고른 날이 이 주 안인가 */
    fun inWeek(day: LocalDate, start: LocalDate): Boolean = !day.isBefore(start) && !day.isAfter(start.plusDays(6))
}
