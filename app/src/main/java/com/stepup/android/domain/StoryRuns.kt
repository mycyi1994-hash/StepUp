package com.stepup.android.domain

import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.temporal.ChronoUnit

/*
 * 러닝 이야기(2026-09-28 쉬운 글쓰기 상황별 시안, docs/redesign/story-compose-states) — 동네 이야기에 최근 러닝을 붙인다.
 *
 * 코스 첨부의 가능 여부와 일반 글쓰기의 가능 여부를 나눈다. 최근 코스가 없거나 기록 조회가 실패해도 장소와
 * 본문이 있으면 일반 글을 쓸 수 있다. 붙일 수 있는 것은 서버(0046 story_runs)가 확인한 내 러닝 중
 * **끝난 날(한국 날짜)이 오늘~3일 전**인 것 — 9월 28일에는 9월 25일 기록까지, 9월 29일에는 같은 기록이
 * 4일 전이 되어 새로 붙일 수 없다. 이미 올라간 글의 첨부는 기간이 지나도 그대로다(3일은 새로 붙일 때의 규칙).
 */

/** 붙이는(붙인) 러닝 — 거리 · 시간 · 코스 그림은 서버 기록의 값이다(앱이 지어내지 않는다) */
data class StoryRun(
    /** 서버 러닝 번호(walk_sessions.id). 글에 이미 붙어 있던 것을 읽은 것이면 0 — 번호 없이 그대로 둔다 */
    val id: Long,
    val startedAt: Long,
    val endedAt: Long,
    val distanceMeters: Int,
    val durationSec: Int,
    /** 끝난 날(한국 날짜) — 기간은 이 날로 잰다(올린 · 동기화한 시각이 아니다) */
    val day: LocalDate,
    /** 코스 그림. 두 점 미만이면 경로 없는 러닝 — 거리 · 시간만 공유한다 */
    val route: List<GeoPoint> = emptyList(),
) {
    val hasRoute: Boolean get() = route.size >= 2

    /** 1km 당 초. 거리나 시간을 모르면 null */
    val paceSecPerKm: Double? get() = if (distanceMeters > 0 && durationSec > 0) durationSec * 1000.0 / distanceMeters else null
}

/** 글쓰기 기록 칸의 서버 조회 결과(story_runs) */
data class StoryRunOptions(
    /** 서버의 오늘(한국 날짜) — 기간은 이 날로 판단한다(폰 시계가 틀려도 서버와 같은 답) */
    val today: LocalDate,
    /** 무효가 아닌 완료 러닝 수 — 전체 기간 */
    val total: Int,
    /** 무효 판정된 러닝 수 */
    val voided: Int,
    /** 무효가 아닌 마지막 완료 시각. 없으면 null */
    val lastEndedAt: Long?,
    /** 지금 붙일 수 있는 러닝 — 최근 것부터 */
    val runs: List<StoryRun>,
)

/** 이 폰에 있는 이 계정의 러닝 — 서버에 아직 올라가지 않은 러닝이 있으면 "처음"이라 단정하지 않는다 */
data class StoryLocalRuns(
    /** 이 계정(+ 계정을 나누기 전의 옛 기록) 러닝 수 */
    val count: Int = 0,
    /** 서버 확인을 기다리는 최근 3일 안의 러닝이 있다 */
    val pendingRecent: Boolean = false,
)

object StoryRunRules {
    /** 앱 기준 시간대 — 서버의 하루(economy.game_day)와 같다 */
    val ZONE: ZoneId = ZoneId.of("Asia/Seoul")

    /** 오늘 · 어제 · 2일 전 · 3일 전 */
    const val WINDOW_DAYS = 3

    fun dayOf(millis: Long): LocalDate = Instant.ofEpochMilli(millis).atZone(ZONE).toLocalDate()

    fun today(nowMillis: Long = System.currentTimeMillis()): LocalDate = dayOf(nowMillis)

    /** 며칠 전 — 오늘 0 · 어제 1. 미래면 음수 */
    fun daysAgo(day: LocalDate, today: LocalDate): Long = ChronoUnit.DAYS.between(day, today)

    /** 새로 붙일 수 있는 날인가 — 오늘~3일 전(미래는 아니다) */
    fun attachable(day: LocalDate, today: LocalDate): Boolean = daysAgo(day, today) in 0..WINDOW_DAYS.toLong()

    /** 이 날 끝난 러닝을 붙일 수 있는 마지막 날 */
    fun lastAttachDay(day: LocalDate): LocalDate = day.plusDays(WINDOW_DAYS.toLong())

    /** 기간 첫날(3일 전)의 시작 시각 */
    fun windowStartMillis(today: LocalDate): Long =
        today.minusDays(WINDOW_DAYS.toLong()).atStartOfDay(ZONE).toInstant().toEpochMilli()

    fun encodeRoute(points: List<GeoPoint>): String = points.joinToString(";") { "${it.lat},${it.lng}" }

    /** 서버의 코스 그림("위도,경도;…"). 읽을 수 없는 조각은 건너뛴다 */
    fun decodeRoute(raw: String?): List<GeoPoint> {
        if (raw.isNullOrBlank()) return emptyList()
        return raw.split(';').mapNotNull { chunk ->
            val parts = chunk.split(',')
            if (parts.size < 2) return@mapNotNull null
            val lat = parts[0].trim().toDoubleOrNull() ?: return@mapNotNull null
            val lng = parts[1].trim().toDoubleOrNull() ?: return@mapNotNull null
            if (lat !in -90.0..90.0 || lng !in -180.0..180.0) null else GeoPoint(lat, lng)
        }
    }
}

/** 기록 조회 상태 */
sealed interface StoryRecords {
    /** 처음 불러오는 중 — 아직 결과가 없다는 이유로 처음 사용자 · 오래 쉰 사용자로 나누지 않는다 */
    data object Loading : StoryRecords

    /** 조회 실패(연결 · 서버) — 기록 없음으로 취급하지 않는다 */
    data object Failed : StoryRecords

    data class Ready(val options: StoryRunOptions, val local: StoryLocalRuns = StoryLocalRuns()) : StoryRecords
}

/** 글에 붙인 러닝 — [kept] 는 고치는 글에 이미 붙어 있던 것(기간과 상관없이 그대로 둔다) */
data class StoryAttachment(val run: StoryRun, val kept: Boolean = false)

/** 게시 때 서버가 첨부를 거절한 이유 — 본문 · 장소는 그대로 두고 코스만 정리하게 한다 */
enum class StoryRunProblem { EXPIRED, INVALID }

/** 글쓰기 위 기록 칸이 보일 모습(시안의 8개 상황 + 지시서의 추가 상태) */
sealed interface StoryRecordCard {
    /** 붙어 있다 — 코스 그림(경로가 있을 때) 또는 거리 · 시간만 */
    data class Attached(val attachment: StoryAttachment, val daysAgo: Long) : StoryRecordCard

    /** 쓰다 둔 글의 첨부가 기간을 넘겼다 — 다른 기록 고르기 / 코스만 빼기 */
    data class Expired(val run: StoryRun, val daysAgo: Long) : StoryRecordCard

    /** 붙일 수 없는 기록(무효 · 다른 계정 · 서버에 없음) */
    data class Invalid(val run: StoryRun) : StoryRecordCard

    data object Loading : StoryRecordCard
    data object FetchError : StoryRecordCard

    /** 러닝이 아직 끝나지 않았다 — 진행 중인 코스는 붙일 수 없다 */
    data object ActiveRun : StoryRecordCard

    /** 방금 달린 러닝이 아직 서버 확인 전이다 */
    data object Pending : StoryRecordCard

    /** 붙일 수 있는 기록이 있지만 붙이지 않았다(뺐다) */
    data class Available(val runs: List<StoryRun>) : StoryRecordCard

    /** 오래된 기록만 있다 */
    data class Old(val lastDay: LocalDate, val daysAgo: Long) : StoryRecordCard

    /** 앱에 완료 기록이 한 번도 없다(서버 조회 성공 · 전체 0 · 이 폰에도 없음) */
    data object Never : StoryRecordCard

    /** 최근 첨부할 기록이 없다 — 처음인지 오래 쉬었는지 단정할 근거가 없을 때 */
    data object Neutral : StoryRecordCard
}

/** 한 줄 이야기 버튼 묶음 — 처음 사용자는 시작 · 질문, 쉬었다 돌아온 사용자는 재시작 · 지난 러닝, 최근 기록은 코스 */
enum class StoryPhraseSet { BEGINNER, RETURNING, RECENT }

enum class StoryPhrase(val sets: Set<StoryPhraseSet>) {
    START(setOf(StoryPhraseSet.BEGINNER)),
    WHERE(setOf(StoryPhraseSet.BEGINNER)),
    SHOES(setOf(StoryPhraseSet.BEGINNER)),
    QUESTION(setOf(StoryPhraseSet.BEGINNER, StoryPhraseSet.RETURNING)),
    AGAIN(setOf(StoryPhraseSet.RETURNING)),
    LAST_RUN(setOf(StoryPhraseSet.RETURNING)),
    RECOMMEND(setOf(StoryPhraseSet.RETURNING)),
    FINISHED(setOf(StoryPhraseSet.RECENT)),
    EASY(setOf(StoryPhraseSet.RECENT)),
    COURSE(setOf(StoryPhraseSet.RECENT)),
    CURIOUS(setOf(StoryPhraseSet.RECENT)),
    ;

    /** 러닝 기록의 날짜 · 거리로 문장을 만든다 */
    val usesRun: Boolean get() = this == FINISHED || this == EASY || this == COURSE

    companion object {
        fun of(name: String?): StoryPhrase? = entries.firstOrNull { it.name == name }

        /** 시안의 2 × 2 순서 */
        fun of(set: StoryPhraseSet): List<StoryPhrase> = when (set) {
            StoryPhraseSet.BEGINNER -> listOf(START, WHERE, SHOES, QUESTION)
            StoryPhraseSet.RETURNING -> listOf(AGAIN, LAST_RUN, RECOMMEND, QUESTION)
            StoryPhraseSet.RECENT -> listOf(FINISHED, EASY, COURSE, CURIOUS)
        }
    }
}

/** 장소 버튼 — 코스 주변은 경로가 있을 때만, 최근 장소는 저장된 공개 장소가 있을 때만 */
enum class StoryPlaceSource { COURSE, NEARBY, RECENT, SEARCH }

/** 문구 버튼을 누른 결과 — 새 본문과, 앱이 넣은 글(사용자가 손대기 전까지만 자동으로 바꾼다) */
data class StoryStarterResult(val text: String, val autoText: String)

object StoryComposeRules {
    /** 시안의 장소 버튼은 셋까지 — 코스 주변 · 내 주변 · 최근 장소 · 직접 검색 순으로 */
    const val MAX_PLACE_SOURCES = 3

    fun card(
        attachment: StoryAttachment?,
        problem: StoryRunProblem?,
        records: StoryRecords,
        activeRun: Boolean,
        deviceToday: LocalDate,
    ): StoryRecordCard {
        val ready = records as? StoryRecords.Ready
        val today = ready?.options?.today ?: deviceToday
        if (attachment != null) {
            val run = attachment.run
            val ago = StoryRunRules.daysAgo(run.day, today)
            if (attachment.kept) return StoryRecordCard.Attached(attachment, ago)
            if (problem == StoryRunProblem.EXPIRED || !StoryRunRules.attachable(run.day, today)) {
                return StoryRecordCard.Expired(run, ago)
            }
            if (problem == StoryRunProblem.INVALID) return StoryRecordCard.Invalid(run)
            if (ready != null) {
                // 조회한 목록에 없다 — 무효가 됐거나 다른 계정의 기록이다(기간은 위에서 봤다)
                val fresh = ready.options.runs.firstOrNull { it.id == run.id } ?: return StoryRecordCard.Invalid(run)
                return StoryRecordCard.Attached(attachment.copy(run = fresh), ago)
            }
            // 조회 중 · 실패 — 붙여 둔 것을 지우지 않는다(서버가 게시 때 다시 본다)
            return StoryRecordCard.Attached(attachment, ago)
        }
        if (activeRun && ready?.options?.runs.isNullOrEmpty()) return StoryRecordCard.ActiveRun
        return when (records) {
            StoryRecords.Loading -> StoryRecordCard.Loading
            StoryRecords.Failed -> StoryRecordCard.FetchError
            is StoryRecords.Ready -> {
                val options = records.options
                val last = options.lastEndedAt?.let(StoryRunRules::dayOf)
                when {
                    options.runs.isNotEmpty() -> StoryRecordCard.Available(options.runs)
                    records.local.pendingRecent -> StoryRecordCard.Pending
                    last != null && options.total > 0 && StoryRunRules.daysAgo(last, options.today) > StoryRunRules.WINDOW_DAYS ->
                        StoryRecordCard.Old(last, StoryRunRules.daysAgo(last, options.today))
                    options.total == 0 && options.voided == 0 && records.local.count == 0 -> StoryRecordCard.Never
                    else -> StoryRecordCard.Neutral
                }
            }
        }
    }

    /** 문구 버튼 묶음. 불러오는 중이면 null — 처음인지 오래 쉬었는지 모르니 빈 자리만 보인다 */
    fun phraseSet(card: StoryRecordCard): StoryPhraseSet? = when (card) {
        is StoryRecordCard.Attached, is StoryRecordCard.Expired, is StoryRecordCard.Invalid,
        is StoryRecordCard.Available, StoryRecordCard.Pending, StoryRecordCard.ActiveRun -> StoryPhraseSet.RECENT
        is StoryRecordCard.Old, StoryRecordCard.Neutral -> StoryPhraseSet.RETURNING
        StoryRecordCard.Never, StoryRecordCard.FetchError -> StoryPhraseSet.BEGINNER
        StoryRecordCard.Loading -> null
    }

    /** 게시에 쓸 러닝 — 붙어 있고 지금 유효한 것만 */
    fun postableRun(card: StoryRecordCard): StoryAttachment? = (card as? StoryRecordCard.Attached)?.attachment

    /** 장소 버튼 — 코스 주변(유효한 경로가 붙었을 때) · 내 주변 · 최근 장소(있을 때) · 직접 검색, 셋까지 */
    fun placeSources(card: StoryRecordCard, hasRecent: Boolean): List<StoryPlaceSource> = buildList {
        if (postableRun(card)?.run?.hasRoute == true) add(StoryPlaceSource.COURSE)
        add(StoryPlaceSource.NEARBY)
        if (hasRecent) add(StoryPlaceSource.RECENT)
        add(StoryPlaceSource.SEARCH)
    }.take(MAX_PLACE_SOURCES)

    /**
     * 올릴 수 있는가 — 공개 장소가 있고, 공백이 아닌 본문 또는 유효한 기록이 있어야 한다. 기간이 지난 첨부 ·
     * 붙일 수 없는 첨부는 바꾸거나 빼야 올릴 수 있다(코스가 아예 없는 일반 글은 막지 않는다).
     */
    fun canPost(card: StoryRecordCard, hasPlace: Boolean, text: String, fallback: String): Boolean {
        if (!hasPlace) return false
        if (card is StoryRecordCard.Expired || card is StoryRecordCard.Invalid) return false
        if (StoryText.canPost(text)) return true
        return postableRun(card) != null && text.isBlank() && StoryText.canPost(fallback)
    }

    /**
     * 문구 버튼 — 편집 가능한 시작점이다. 비었거나 앱이 넣은 글 그대로면 바꾸고, 사용자가 쓴 글이 있으면
     * 덮어쓰지 않고 한 줄 아래 붙인다. 이미 들어 있는 문구는 다시 넣지 않는다(null — 본문 그대로).
     */
    fun insertStarter(body: String, autoText: String, starter: String): StoryStarterResult? {
        val clean = starter.trim()
        if (clean.isEmpty()) return null
        if (body.isBlank() || (autoText.isNotEmpty() && body == autoText)) return StoryStarterResult(clean, clean)
        val head = clean.lineSequence().first().trim()
        if (body.contains(clean) || (head.isNotEmpty() && body.contains(head))) return null
        return StoryStarterResult(body.trimEnd() + "\n" + clean, "")
    }

    /** 장소 이름을 문장에 넣을 때 — "마포대교 남단" → "마포대교", "여의도 한강공원" → "여의도" */
    fun shortPlace(name: String): String = name.trim().split(Regex("\\s+")).firstOrNull().orEmpty().ifEmpty { name.trim() }

    /** "2.10" — 문장과 카드의 km(소수 둘째 자리) */
    fun km(distanceMeters: Int): String = String.format(java.util.Locale.ROOT, "%.2f", distanceMeters / 1000.0)

    /** "8′00″/km" */
    fun pace(secondsPerKm: Double?): String? {
        val sec = secondsPerKm?.takeIf { it.isFinite() && it > 0 }?.let { Math.round(it) } ?: return null
        return "%d′%02d″/km".format(java.util.Locale.ROOT, sec / 60, sec % 60)
    }
}
