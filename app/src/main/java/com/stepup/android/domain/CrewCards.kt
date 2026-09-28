package com.stepup.android.domain

import java.math.BigDecimal
import java.math.RoundingMode
import kotlin.math.abs
import kotlin.math.roundToInt

/*
 * 크루 명함형(2026-09-28 확정 2번) — 화면이 서버 값에서 읽어 내는 규칙.
 *
 * 스크린샷의 이름 · 숫자는 여기 없다. 레벨 계산식 · 등급 이름 · 보상도 없다(정해지지 않았다) — 서버의 level 칸이
 * 비어 있으면 "새 크루"로 보인다. 크루 거리는 기존 규칙(크루 러닝으로 적힌 러닝)을 서버가 센 값을 쓴다.
 */

/** 한 번에 달리는 거리 */
enum class CrewDistance {
    D1_3, D3_5, D5P;

    companion object {
        fun of(code: String?): CrewDistance? = entries.firstOrNull { it.name == code }
    }
}

/** 우리 크루의 분위기 — [pace] 인 셋은 달리는 방식(상세의 "한 번에" 줄), 나머지는 누구를 반기는지 */
enum class CrewMood(val pace: Boolean) {
    WALK_FIRST(true), EASY(true), RECORD(true), BEGINNER(false), EXPERIENCED(false);

    companion object {
        fun of(code: String): CrewMood? = entries.firstOrNull { it.name == code }
    }
}

/** 가입 신청의 문구 버튼 — 고른 것은 한마디와 따로 남는다 */
enum class CrewPhrase {
    BEGINNER, AFTERWORK, STEADY, EASY;

    companion object {
        fun of(code: String): CrewPhrase? = entries.firstOrNull { it.name == code }
    }
}

/**
 * 정기 모임 — 요일(월=0 … 일=6 을 비트로)과 시작 시각(0시부터 분). 요일이 없으면 "정해진 일정 없음"이고,
 * 요일만 있고 시간이 없을 수도 있다(서버도 같은 규칙: 시간만 있는 일정은 없다).
 */
data class CrewSchedule(val days: Int = 0, val minutes: Int? = null) {
    val none: Boolean get() = days == 0

    fun has(day: Int): Boolean = days and (1 shl day) != 0

    fun toggle(day: Int): CrewSchedule = copy(days = days xor (1 shl day))

    val dayIndexes: List<Int> get() = (0..6).filter(::has)

    /** 서버에 보낼 모양 — 요일이 없으면 시간도 보내지 않는다 */
    val normalized: CrewSchedule get() = if (days == 0) NONE else this

    companion object {
        val NONE = CrewSchedule()
    }
}

enum class CrewApplicationStatus {
    PENDING, APPROVED, DECLINED, CANCELED;

    companion object {
        fun of(code: String?): CrewApplicationStatus? = entries.firstOrNull { it.name == code }
    }
}

/** 보는 사람과 크루의 관계 — 서버의 가입 상태(owned · joined · requested)로만 판단한다 */
enum class CrewRole { VISITOR, PENDING, MEMBER, OWNER }

/** 크루 명함 한 장 — 목록과 상세가 같은 값을 쓴다 */
data class CrewCard(
    val id: String,
    val name: String,
    /** 한 줄 소개 */
    val tagline: String = "",
    /** 크루장 한마디(선택) */
    val leaderNote: String = "",
    val leaderId: String = "",
    val leaderName: String = "",
    /** 이름 이미지 바탕(0~3). 비어 있으면 크루 id 로 고른다 */
    val imageBg: Int? = null,
    val imageVer: Int = 0,
    val hasImage: Boolean = false,
    val area: String = "",
    val lat: Double? = null,
    val lng: Double? = null,
    val schedule: CrewSchedule = CrewSchedule.NONE,
    val distance: CrewDistance? = null,
    val moods: List<CrewMood> = emptyList(),
    val memberCount: Int = 1,
    /** 모집 정원. 비어 있으면 예전 크루 — 정원 없음 */
    val capacity: Int? = null,
    val recruiting: Boolean = true,
    val recruitChangedAt: Long = 0L,
    val createdAt: Long = 0L,
    /** 이번 주 목표(km). 비어 있으면 목표 없음 */
    val goalKm: Int? = null,
    /** 저장된 레벨. 비어 있으면 새 크루 */
    val level: Int? = null,
    /** 이번 주(한국 월요일부터) 지금 멤버가 크루 러닝으로 달린 거리 */
    val weekKm: Double = 0.0,
    val weekRunners: Int = 0,
    val joined: Boolean = false,
    val requested: Boolean = false,
    val owned: Boolean = false,
    /** 기다리는 신청 수 — 크루장에게만 */
    val pendingCount: Int = 0,
    /** 예전 "바로 가입" 크루 — 신청서 없이 바로 멤버가 된다 */
    val openJoin: Boolean = false,
    /** 내가 이 크루에 낸 가장 최근 신청서 */
    val myApplicationId: Long? = null,
    val myApplicationStatus: CrewApplicationStatus? = null,
    val myApplicationSeen: Boolean = false,
) {
    val role: CrewRole
        get() = when {
            owned -> CrewRole.OWNER
            joined -> CrewRole.MEMBER
            requested -> CrewRole.PENDING
            else -> CrewRole.VISITOR
        }

    val full: Boolean get() = capacity != null && memberCount >= capacity

    val point: GeoPoint? get() = if (lat != null && lng != null) GeoPoint(lat, lng) else null

    /** 이름 이미지의 바탕 */
    val bg: Int get() = imageBg ?: CrewRules.bgFor(id)

    /** 아직 보지 않은 가입 결과(승인 · 미승인) */
    val unseenResult: CrewApplicationStatus?
        get() = myApplicationStatus?.takeIf {
            !myApplicationSeen && (it == CrewApplicationStatus.APPROVED || it == CrewApplicationStatus.DECLINED)
        }
}

/** 상세 아래 큰 버튼 한 개 — 역할과 모집 상태를 따로 본다 */
enum class CrewFooter {
    /** 방문자 · 모집 중 · 자리 있음 */
    APPLY,

    /** 방문자 · 예전 바로 가입 크루 */
    JOIN_NOW,

    /** 방문자 · 모집 쉬는 중 — 누를 수 없다 */
    PAUSED,

    /** 방문자 · 정원 마감 — 누를 수 없다 */
    FULL,
    PENDING,
    MEMBER,
    OWNER,

    /** 크루장 · 모집을 멈춘 뒤 — 관리 권한은 그대로 */
    OWNER_PAUSED,
}

/** 이번 주 목표 — 목표가 없으면 만들지 않는다(나누지 않는다) */
data class CrewGoalProgress(val goalKm: Int, val doneKm: Double) {
    /** 달성률 — 반올림(126/160 → 79). 100 을 넘으면 넘는 대로 */
    val percent: Int get() = BigDecimal(doneKm * 100.0 / goalKm).setScale(0, RoundingMode.HALF_UP).toInt()

    /** 진행 바는 100% 까지만 채운다 */
    val fraction: Float get() = (doneKm / goalKm).coerceIn(0.0, 1.0).toFloat()

    val remainingKm: Double get() = (goalKm - doneKm).coerceAtLeast(0.0)

    val reached: Boolean get() = doneKm >= goalKm
}

/** 목록 정렬 */
enum class CrewSort { NEAR, RECENT, ACTIVE }

/** 목록을 보는 범위 — [center] 가 없으면 모든 크루 */
data class CrewListQuery(
    val center: GeoPoint? = null,
    val radiusKm: Int = 3,
    val sort: CrewSort = CrewSort.NEAR,
)

object CrewRules {
    /**
     * 이름 이미지의 글자 — 이름 그대로. 띄어쓰기가 있는 긴 이름은 가운데에 가까운 빈칸에서 두 줄로 나눈다
     * (글자 중간에서 끊겨 "공덕 한바 / 퀴"가 되지 않게). 빈 이름은 가운뎃점.
     */
    fun nameImageText(name: String): String {
        val label = name.trim().replace(Regex("\\s+"), " ")
        if (label.isEmpty()) return "·"
        if (label.length <= 4) return label
        val middle = label.length / 2.0
        val cut = label.indices.filter { label[it] == ' ' }.minByOrNull { abs(it - middle) } ?: return label
        return label.substring(0, cut) + "\n" + label.substring(cut + 1)
    }

    const val NAME_MAX = 40
    const val TAGLINE_MAX = 120
    const val NOTE_MAX = 300
    const val AREA_MAX = 60
    const val MESSAGE_MAX = 300
    const val CAPACITY_MAX = 9999
    const val GOAL_MAX = 99999

    /** 이름 이미지 바탕 네 가지 */
    const val BG_COUNT = 4

    /** 지역 범위 버튼 */
    val RADII = listOf(1, 3, 5)

    /** 만들기 3단계 · 모집 설정의 목표 버튼 */
    val CREATE_GOALS = listOf(50, 100)

    /** 주간 목표 수정의 목표 버튼 */
    val EDIT_GOALS = listOf(100, 160, 200)

    fun bgFor(key: String): Int = Math.floorMod(key.hashCode(), BG_COUNT)

    fun footer(card: CrewCard): CrewFooter = when (card.role) {
        CrewRole.OWNER -> if (card.recruiting) CrewFooter.OWNER else CrewFooter.OWNER_PAUSED
        CrewRole.MEMBER -> CrewFooter.MEMBER
        CrewRole.PENDING -> CrewFooter.PENDING
        CrewRole.VISITOR -> when {
            !card.recruiting -> CrewFooter.PAUSED
            card.full -> CrewFooter.FULL
            card.openJoin -> CrewFooter.JOIN_NOW
            else -> CrewFooter.APPLY
        }
    }

    fun progress(card: CrewCard): CrewGoalProgress? = card.goalKm?.let { CrewGoalProgress(it, card.weekKm) }

    /**
     * 거리 글자 — 100km 이상은 정수, 그 아래는 소수 한 자리(0 이면 떼고). 126.0 → "126", 17.6 → "17.6", 34.0 → "34".
     */
    fun km(value: Double): String {
        val v = value.coerceAtLeast(0.0)
        if (v >= 100) return v.roundToInt().toString()
        val tenth = BigDecimal(v).setScale(1, RoundingMode.HALF_UP)
        return if (tenth.stripTrailingZeros().scale() <= 0) tenth.toInt().toString() else tenth.toPlainString()
    }

    /** 보는 사람에게서 크루까지(km) — 둘 중 하나라도 위치를 모르면 null */
    fun distanceKm(card: CrewCard, center: GeoPoint?): Double? {
        val point = card.point ?: return null
        return center?.let { haversineMeters(point, it) / 1000.0 }
    }

    /**
     * 목록에 보일 크루. 범위가 있으면 그 안의 크루만 — 위치가 없는 크루는 거리를 잴 수 없어 뺀다. 내가 멤버 · 크루장 ·
     * 신청 중인 크루는 범위 밖이어도 남긴다(찾으러 목록을 옮기지 않게).
     */
    fun visible(cards: List<CrewCard>, query: CrewListQuery): List<CrewCard> {
        val center = query.center
        val kept = cards.filter { card ->
            if (card.role != CrewRole.VISITOR || center == null) return@filter true
            val km = distanceKm(card, center) ?: return@filter false
            km <= query.radiusKm + 1e-9
        }
        return sort(kept, query)
    }

    fun sort(cards: List<CrewCard>, query: CrewListQuery): List<CrewCard> {
        val recent = compareByDescending<CrewCard> { it.recruitChangedAt }.thenBy { it.name }.thenBy { it.id }
        val order: Comparator<CrewCard> = when {
            query.sort == CrewSort.ACTIVE ->
                compareByDescending<CrewCard> { it.weekKm }.thenByDescending { it.memberCount }.then(recent)
            query.sort == CrewSort.NEAR && query.center != null ->
                compareBy<CrewCard> { distanceKm(it, query.center) ?: Double.MAX_VALUE }.then(recent)
            else -> recent
        }
        return cards.sortedWith(order)
    }

    /** 이름만 있으면 다음 단계로 — 이름 중복 · 다른 칸은 기존 정책에 없어 막지 않는다 */
    fun nameOk(name: String): Boolean = name.trim().length in 1..NAME_MAX

    /** 정원 입력 — 숫자만, 1명 이상. 넘으면 null */
    fun capacity(text: String): Int? = text.trim().toIntOrNull()?.takeIf { it in 1..CAPACITY_MAX }

    /** 정원이 지금 인원보다 작지 않은가 */
    fun capacityProblem(text: String, members: Int): CrewCapacityProblem? {
        val value = capacity(text) ?: return if (text.isBlank()) CrewCapacityProblem.EMPTY else CrewCapacityProblem.INVALID
        return if (value < members) CrewCapacityProblem.BELOW_MEMBERS else null
    }

    fun goal(text: String): Int? = text.trim().toIntOrNull()?.takeIf { it in 1..GOAL_MAX }

    /** 끝 줄바꿈 · 공백만 떼고 줄바꿈은 그대로 — 서버도 같은 모양으로 남긴다 */
    fun message(text: String): String = text.trimEnd()

    /** 한 사람 이름의 첫 글자 — 동그라미 안의 글자 */
    fun initial(name: String): String = name.trim().let { if (it.isEmpty()) "?" else String(Character.toChars(it.codePointAt(0))) }

    /** 소수점 아래를 가진 거리가 사실상 같은지 — 테스트 · 비교용 */
    fun sameKm(a: Double, b: Double): Boolean = abs(a - b) < 1e-6
}

enum class CrewCapacityProblem { EMPTY, INVALID, BELOW_MEMBERS }

/** 목표 고르기 — 없음 · 버튼 · 직접 입력 */
sealed interface CrewGoalChoice {
    data object None : CrewGoalChoice
    data class Km(val value: Int, val custom: Boolean = false) : CrewGoalChoice

    /** 고른 목표(km). 없으면 null */
    val km: Int? get() = (this as? Km)?.value
}

/** 대표 이미지 고르기 — 수정할 때는 처음에 서버의 것을 그대로 둔다 */
sealed interface CrewImageChoice {
    /** 서버의 대표 이미지(사진 또는 이름 이미지)를 그대로 */
    data object Server : CrewImageChoice

    /** 이름이 들어간 기본 이미지 — 바탕만 고른다(사진이 있었다면 지운다) */
    data class Named(val bg: Int) : CrewImageChoice

    /** 정사각형으로 자른 사진 — 앱 안 파일 */
    data class Photo(val path: String) : CrewImageChoice
}

/** 만들기 · 수정 초안이 어느 화면의 것인지 */
enum class CrewDraftMode { CREATE, EDIT_PROFILE, EDIT_RUNNING, EDIT_RECRUIT }

/** 활동 지역 — 검색 · 지도에서 고른 곳(이름과 좌표) */
data class CrewArea(val name: String, val address: String = "", val lat: Double? = null, val lng: Double? = null) {
    val point: GeoPoint? get() = if (lat != null && lng != null) GeoPoint(lat, lng) else null
}

/**
 * 크루 만들기 · 수정 초안. 단계를 오가도(이전 · 이미지 · 지역 · 일정 고르기) 같은 초안을 고친다. 만들기 초안은
 * "저장 후 나가기"로 이 폰에 남는다. 수정 초안은 서버 값을 저장이 끝나기 전까지 바꾸지 않는다.
 */
data class CrewDraft(
    val mode: CrewDraftMode = CrewDraftMode.CREATE,
    val crewId: String = "",
    val name: String = "",
    val tagline: String = "",
    val leaderNote: String = "",
    val image: CrewImageChoice = CrewImageChoice.Named(0),
    val area: CrewArea? = null,
    val schedule: CrewSchedule = CrewSchedule.NONE,
    val distance: CrewDistance? = null,
    val moods: List<CrewMood> = emptyList(),
    val capacity: String = "",
    val goal: CrewGoalChoice = CrewGoalChoice.None,
    val recruiting: Boolean = true,
    /** 만들기 단계(0 소개 · 1 모임 · 2 모집 · 3 미리보기) */
    val step: Int = 0,
    /** 같은 만들기가 두 번 가지 않게 — 저장을 다시 눌러도 같은 값 */
    val clientKey: String = "",
    val savedAt: Long = 0L,
) {
    /** 이 폰에 남길 때의 열쇠 */
    val key: String get() = if (mode == CrewDraftMode.CREATE) KEY_CREATE else "${mode.name}:$crewId"

    val nameOk: Boolean get() = CrewRules.nameOk(name)

    fun toggleMood(mood: CrewMood): CrewDraft = copy(moods = if (mood in moods) moods - mood else moods + mood)

    companion object {
        const val KEY_CREATE = "CREATE"

        /** 서버의 크루로 수정 초안을 시작한다 */
        fun edit(mode: CrewDraftMode, card: CrewCard): CrewDraft = CrewDraft(
            mode = mode,
            crewId = card.id,
            name = card.name,
            tagline = card.tagline,
            leaderNote = card.leaderNote,
            image = CrewImageChoice.Server,
            area = card.area.takeIf { it.isNotBlank() }?.let { CrewArea(it, lat = card.lat, lng = card.lng) },
            schedule = card.schedule,
            distance = card.distance,
            moods = card.moods,
            capacity = card.capacity?.toString().orEmpty(),
            goal = card.goalKm?.let { CrewGoalChoice.Km(it, custom = it !in CrewRules.CREATE_GOALS) } ?: CrewGoalChoice.None,
            recruiting = card.recruiting,
        )
    }
}

/** 한 크루의 멤버 한 줄 */
data class CrewMember(
    val userId: String,
    val name: String,
    val owner: Boolean,
    val joinedAt: Long,
    /** 이번 주 크루 러닝 거리 */
    val weekKm: Double,
    val weekRuns: Int,
)

/** 가입 신청서 한 장 */
data class CrewApplication(
    val id: Long,
    val crewId: String,
    val userId: String,
    val name: String,
    val status: CrewApplicationStatus,
    val phrases: List<CrewPhrase>,
    val message: String,
    val createdAt: Long,
    val decidedAt: Long? = null,
    val seen: Boolean = false,
)

/** 이 크루에서 본 한 사람 — 공개 정보만 */
enum class CrewPersonRole { OWNER, MEMBER, APPLICANT, NONE }

data class CrewPerson(
    val userId: String,
    val name: String,
    val role: CrewPersonRole,
    val joinedAt: Long? = null,
    val weekKm: Double = 0.0,
    /** 크루장에게만 — 신청자의 기다리는 신청서 */
    val application: CrewApplication? = null,
)

/** 레벨 안내 — 레벨과 이번 주 활동 */
data class CrewLevelInfo(val level: Int?, val weekKm: Double, val weekRunners: Int, val memberCount: Int)

/** 승인 · 미승인 뒤의 크루 */
data class CrewDecision(val approved: Boolean, val memberCount: Int, val capacity: Int?, val pendingCount: Int)

/** 서버가 알려 준 실패 — 화면이 고칠 칸이나 다음 화면을 고른다 */
enum class CrewProblem {
    NAME, TAGLINE, LEADER_NOTE, AREA, SCHEDULE, CAPACITY, GOAL, IMAGE, MESSAGE,
    CREW_LIMIT, CREW_CLOSED, CREW_FULL, CREW_MISSING, APPLICATION_DECIDED, APPLICATION_MISSING,
    CAPACITY_BELOW_MEMBERS, TARGET_NOT_MEMBER, TARGET_IS_OWNER, OWNER_CANNOT_LEAVE, NOT_OWNER,

    /** 서버에 닿지 못했다 · 나중에 다시 */
    NETWORK,

    /** 로그인이 필요하다 */
    SIGN_IN,
    OTHER;

    companion object {
        /** 서버 함수가 raise 한 첫 마디(예: "crew_full", "invalid:name") */
        fun of(reason: String): CrewProblem {
            val code = reason.trim().substringBefore(' ')
            return when {
                code == "invalid:name" -> NAME
                code == "invalid:tagline" -> TAGLINE
                code == "invalid:leader_note" -> LEADER_NOTE
                code == "invalid:area" -> AREA
                code == "invalid:schedule" || code == "invalid:distance" || code == "invalid:moods" -> SCHEDULE
                code == "invalid:capacity" -> CAPACITY
                code == "invalid:goal" -> GOAL
                code == "invalid:image" || code == "invalid:image_bg" -> IMAGE
                code == "invalid:message" || code == "invalid:phrases" -> MESSAGE
                code == "crew_limit" -> CREW_LIMIT
                code == "crew_closed" -> CREW_CLOSED
                code == "crew_full" -> CREW_FULL
                code == "crew_missing" -> CREW_MISSING
                code == "application_decided" -> APPLICATION_DECIDED
                code == "application_missing" -> APPLICATION_MISSING
                code == "capacity_below_members" -> CAPACITY_BELOW_MEMBERS
                code == "target_not_member" -> TARGET_NOT_MEMBER
                code == "target_is_owner" -> TARGET_IS_OWNER
                code == "owner_cannot_leave" -> OWNER_CANNOT_LEAVE
                reason.contains("크루장만") -> NOT_OWNER
                else -> OTHER
            }
        }
    }
}
