package com.stepup.android.domain

import java.math.BigDecimal
import java.math.RoundingMode

/*
 * 크루 러닝 대기실 — 이름 · 함께 달릴 코스 · 열린 대기실 목록(서버 0072, 2026-10-24 크루 러닝 핸드오프 장면 02 ~ 09 · 22 · 25).
 *
 * - 크루원 누구나 이름과 코스를 붙여 대기실을 연다(party_create). 연 것 · 들어간 것만으로 하루 한 번은 쓰이지 않는다.
 * - 같은 크루의 열린 대기실을 골라 들어간다(party_rooms → party_join). 예전처럼 저절로 아무 방에 넣지 않는다.
 * - 방의 코스는 방에 둔 사본이다(이름 · 거리 · 경로 · 판). 진행자만 바꾸고, 바꿀 때마다 판이 오른다 — 진행자를 뺀 모두의
 *   준비가 풀리고(D14) 출발은 진행자가 본 판으로만 된다(party_start_rev).
 *
 * 규칙 값은 서버 함수(economy.party_room_name · economy.party_course_check)와 같다 — CrewRoomTest.
 *
 * 2026-10-24 사용자 "코스를 내가 선택할 수가 없네 … 정해준 것만 해야 되던데 그건 아닌 것 같다"(서버 0073):
 * - 코스는 골라도 되고 안 골라도 된다 — 코스 없는 방은 각자 원하는 길로 함께 달린다(자유 코스, [RoomCourse] 가 null).
 * - 지도를 눌러 점을 찍어 코스를 직접 그린다([RoomCourse.drawn]). 진행자는 방에서 코스를 빼도 된다.
 */

/** 방의 코스 사본 — 방에 둔 이름 · 거리 · 경로. [rev] 는 방의 코스 판(코스 없는 방 · 아직 방에 넣기 전이면 0) */
data class RoomCourse(
    val rev: Int,
    val name: String,
    val km: Double,
    /** 경로 — 판이 바뀔 때만 받는다(2초마다 묻는 방 상태에는 없다). 아직 받지 못했으면 비어 있다 */
    val points: List<GeoPoint> = emptyList(),
) {
    val hasTrack: Boolean get() = points.size >= 2

    /** 서버로 보내는 경로 — "위도,경도;…", 소수 6자리 · 지수 표기 없이(서버 정규식이 받는 꼴) */
    fun encodeTrack(): String = points.joinToString(";") { "${plain(it.lat)},${plain(it.lng)}" }

    companion object {
        /** 내 코스 · 공유 코스로 방에 둘 사본 — 이름은 60자 안, 경로는 서버 한도에 맞게 점을 고르게 줄인다 */
        fun of(course: RunCourse): RoomCourse = RoomCourse(
            rev = 0,
            name = CrewRoomRules.courseName(course.name),
            km = course.distanceKm.coerceIn(0.0, CrewRoomRules.COURSE_KM_MAX),
            points = CrewRoomRules.fitTrack(course.points),
        )

        /** 지도에 찍은 점으로 그린 코스 — 거리는 점 사이 거리의 합([CrewRoomRules.pathKm]). 쓸 수 있는지는 [CrewRoomRules.drawnUsable] */
        fun drawn(name: String, points: List<GeoPoint>): RoomCourse = RoomCourse(
            rev = 0,
            name = CrewRoomRules.courseName(name),
            km = CrewRoomRules.pathKm(points).coerceIn(0.0, CrewRoomRules.COURSE_KM_MAX),
            points = CrewRoomRules.fitTrack(points),
        )

        private fun plain(v: Double): String =
            BigDecimal(v).setScale(6, RoundingMode.HALF_UP).stripTrailingZeros().toPlainString()
    }
}

/** 열린 대기실 하나 — `party_rooms` */
data class CrewRoom(
    val id: Long,
    val name: String,
    /** LOBBY · COUNTDOWN · RUNNING */
    val status: String,
    val hostId: String,
    val hostName: String,
    val createdAtMs: Long,
    val members: Int,
    val ready: Int,
    val capacity: Int,
    val course: RoomCourse?,
    /** 내가 들어가 있는 방 */
    val mine: Boolean,
    /** 진행자가 나를 내보낸 방 — 다시 들어가지 못한다 */
    val kicked: Boolean,
    /** 달리는 방식(0074) */
    val mode: RoomMode = RoomMode.TOGETHER,
    /** 만나는 곳(같이 뛰기) */
    val meetPlace: String? = null,
    /** 출발 예정 시각(이 폰 시각, 0 이면 없음) — 안내일 뿐이다 */
    val planAtMs: Long = 0L,
    /** 들어온 사람 이름 앞 넷 */
    val memberNames: List<String> = emptyList(),
) {
    val open: Boolean get() = status == "LOBBY"
    val full: Boolean get() = members >= capacity
}

/**
 * 달리는 방식(0074, 2026-10-26 시안) — 같이 뛰기는 만나는 곳에 모여, 원격 뛰기는 같은 시간에 각자 자리에서.
 * 원격 방은 함께 달리기 인원 보너스(500m)를 받지 않는다(서버가 정한다).
 */
enum class RoomMode(val wire: String) {
    TOGETHER("TOGETHER"), REMOTE("REMOTE");

    companion object {
        fun of(wire: String?): RoomMode = entries.firstOrNull { it.wire == wire } ?: TOGETHER
    }
}

/** 크루의 열린 대기실 목록 — 오늘(한국 날짜) 크루 러닝을 이미 했는지 함께 */
data class CrewRooms(
    val rooms: List<CrewRoom>,
    val capacity: Int,
    val doneToday: Boolean,
)

/** 대기실에 들어가는 길 — 저절로(예전 · 번개러닝) · 고른 방 · 새 방 */
sealed interface RoomTarget {
    /** 크루(번개 글)의 열린 방 하나에 저절로 — 예전 party_open. 번개러닝 대기실 · 옛 길 */
    data object Auto : RoomTarget

    /** 목록에서 고른 방 · 방금 연 방 */
    data class Join(val partyId: Long) : RoomTarget
}

object CrewRoomRules {
    const val NAME_MAX = 30

    /** 만나는 곳 글자 한도 · 정원 최소(0074 — 서버와 같다) */
    const val MEET_MAX = 60
    const val CAPACITY_MIN = 2
    const val COURSE_NAME_MAX = 60
    const val COURSE_KM_MAX = 100.0

    /** 경로 글자 수 한도(서버 6만 자) — 점 하나는 많아야 "-90.123456,-180.123456;" 23자 */
    const val TRACK_MAX_CHARS = 60_000
    const val TRACK_MAX_POINTS = 2_400

    /** 방 이름 — 공백 묶음은 하나로, 앞뒤 공백은 뺀다(서버 economy.party_room_name 과 같다) */
    fun normalizeName(raw: String): String = raw.replace(Regex("\\s+"), " ").trim()

    /** 방 이름 길이 — 서버 char_length 처럼 글자(코드 포인트)로 센다 */
    fun nameLength(raw: String): Int = normalizeName(raw).let { it.codePointCount(0, it.length) }

    fun nameValid(raw: String): Boolean = nameLength(raw) in 1..NAME_MAX

    /** 코스 이름 — 앞뒤 공백을 빼고 60자 안(넘으면 자른다 — 코스 이름은 사용자가 방에서 고친 값이 아니다) */
    fun courseName(raw: String): String {
        val trimmed = raw.trim().ifEmpty { "코스" }
        if (trimmed.codePointCount(0, trimmed.length) <= COURSE_NAME_MAX) return trimmed
        return trimmed.substring(0, trimmed.offsetByCodePoints(0, COURSE_NAME_MAX)).trim()
    }

    /** 방에 둘 수 있는 코스 — 경로 점 둘 이상 · 거리 100km 안 */
    fun usable(course: RunCourse): Boolean = course.points.size >= 2 && course.distanceKm in 0.0..COURSE_KM_MAX

    /** 지도에 찍을 수 있는 점 — 손으로 찍는 코스라 넉넉하다(서버 경로 한도 안) */
    const val DRAW_MAX_POINTS = 300

    /** 꺾은선 거리(km) — 점 사이 하버사인 거리의 합 */
    fun pathKm(points: List<GeoPoint>): Double = points.zipWithNext { a, b -> haversineMeters(a, b) }.sum() / 1000.0

    /** 그린 코스를 방에 둘 수 있는가 — 점 둘 이상 · 0 보다 길고 100km 안(서버 economy.party_course_check 와 같은 한도) */
    fun drawnUsable(points: List<GeoPoint>): Boolean =
        points.size in 2..DRAW_MAX_POINTS && pathKm(points).let { it > 0.0 && it <= COURSE_KM_MAX }

    /** 경로를 서버 한도 안으로 — 처음 · 끝은 두고 사이를 고르게 고른다 */
    fun fitTrack(points: List<GeoPoint>): List<GeoPoint> {
        if (points.size <= TRACK_MAX_POINTS) return points
        val last = points.lastIndex
        return List(TRACK_MAX_POINTS) { i -> points[(i.toLong() * last / (TRACK_MAX_POINTS - 1)).toInt()] }
    }

    /** 목록 순서 — 내 방 먼저, 그다음 막 열린 방부터. 출발한 방은 내 방일 때만(목록에서 들어갈 수 없다) */
    fun ordered(rooms: List<CrewRoom>): List<CrewRoom> = rooms
        .filter { it.open && !it.kicked }
        .sortedWith(compareByDescending<CrewRoom> { it.mine }.thenByDescending { it.createdAtMs }.thenByDescending { it.id })
}
