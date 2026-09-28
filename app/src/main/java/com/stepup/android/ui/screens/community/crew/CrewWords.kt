package com.stepup.android.ui.screens.community.crew

import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import com.stepup.android.R
import com.stepup.android.domain.CrewCard
import com.stepup.android.domain.CrewDistance
import com.stepup.android.domain.CrewMood
import com.stepup.android.domain.CrewPhrase
import com.stepup.android.domain.CrewRules
import com.stepup.android.domain.CrewSchedule
import java.time.Instant
import java.time.LocalTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

/** 크루 화면의 글자 — 요일 · 시간 · 거리 · 분위기 · 날짜를 앱 언어로 */
class CrewWords(private val context: Context, private val locale: Locale) {
    private fun s(id: Int, vararg args: Any): String = context.getString(id, *args)

    private val days = listOf(
        R.string.crew_day_mon, R.string.crew_day_tue, R.string.crew_day_wed, R.string.crew_day_thu,
        R.string.crew_day_fri, R.string.crew_day_sat, R.string.crew_day_sun,
    )

    fun day(index: Int): String = s(days[index])

    /** "화·목", 모든 요일이면 "매일". 요일이 없으면 빈 글자 */
    fun dayList(schedule: CrewSchedule): String = when {
        schedule.none -> ""
        schedule.days == 0x7F -> s(R.string.crew_every_day)
        else -> schedule.dayIndexes.joinToString("·") { day(it) }
    }

    /** 카드 · 상세 — "화·목 19:30" (정해진 일정이 없으면 "정해진 일정 없어요") */
    fun scheduleShort(schedule: CrewSchedule): String {
        if (schedule.none) return s(R.string.crew_schedule_none)
        val time = schedule.minutes?.let { LocalTime.of(it / 60, it % 60).format(DateTimeFormatter.ofPattern("HH:mm", locale)) }
        return listOfNotNull(dayList(schedule), time).joinToString(" ")
    }

    /** 입력칸 — "화·목 오후 7시 30분" */
    fun scheduleLong(schedule: CrewSchedule): String {
        if (schedule.none) return s(R.string.crew_schedule_none)
        return listOfNotNull(dayList(schedule), schedule.minutes?.let(::timeLong)).joinToString(" ")
    }

    fun timeLong(minutes: Int): String {
        val time = LocalTime.of(minutes / 60, minutes % 60)
        val pattern = if (time.minute == 0) s(R.string.crew_time_hour_pattern) else s(R.string.crew_time_pattern)
        return time.format(DateTimeFormatter.ofPattern(pattern, locale))
    }

    fun distance(value: CrewDistance): String = when (value) {
        CrewDistance.D1_3 -> s(R.string.crew_distance_1_3)
        CrewDistance.D3_5 -> s(R.string.crew_distance_3_5)
        CrewDistance.D5P -> s(R.string.crew_distance_5p)
    }

    /** 분위기 버튼의 짧은 이름 */
    fun mood(value: CrewMood): String = when (value) {
        CrewMood.WALK_FIRST -> s(R.string.crew_mood_walk_first)
        CrewMood.EASY -> s(R.string.crew_mood_easy)
        CrewMood.RECORD -> s(R.string.crew_mood_record)
        CrewMood.BEGINNER -> s(R.string.crew_mood_beginner)
        CrewMood.EXPERIENCED -> s(R.string.crew_mood_experienced)
    }

    /** 분위기의 긴 이름 — 카드의 성격 줄 · 미리보기("천천히 달리기 · 처음도 환영") */
    fun moodLong(value: CrewMood): String = when (value) {
        CrewMood.WALK_FIRST -> s(R.string.crew_mood_walk_first_long)
        CrewMood.EASY -> s(R.string.crew_mood_easy_long)
        CrewMood.RECORD -> s(R.string.crew_mood_record_long)
        CrewMood.BEGINNER -> s(R.string.crew_mood_beginner)
        CrewMood.EXPERIENCED -> s(R.string.crew_mood_experienced)
    }

    /** 달리는 방식 문장 — 상세 "한 번에" 줄("천천히 달려요") */
    private fun pace(value: CrewMood): String? = when (value) {
        CrewMood.WALK_FIRST -> s(R.string.crew_pace_walk_first)
        CrewMood.EASY -> s(R.string.crew_pace_easy)
        CrewMood.RECORD -> s(R.string.crew_pace_record)
        else -> null
    }

    fun phrase(value: CrewPhrase): String = when (value) {
        CrewPhrase.BEGINNER -> s(R.string.crew_phrase_beginner)
        CrewPhrase.AFTERWORK -> s(R.string.crew_phrase_afterwork)
        CrewPhrase.STEADY -> s(R.string.crew_phrase_steady)
        CrewPhrase.EASY -> s(R.string.crew_phrase_easy)
    }

    /** 카드 성격 줄(푸른 글자) — 고른 분위기 */
    fun styleLine(card: CrewCard): String = card.moods.joinToString(" · ") { moodLong(it) }

    /** 카드 정보 줄 — "공덕 · 화·목 19:30 · 3–5km" (없는 것은 뺀다) */
    fun infoLine(card: CrewCard): String = listOfNotNull(
        card.area.takeIf { it.isNotBlank() },
        card.schedule.takeIf { !it.none }?.let(::scheduleShort),
        card.distance?.let(::distance),
    ).joinToString(" · ")

    /** 상세 이름 아래 — "공덕 · 천천히 · 처음도 환영" */
    fun subLine(card: CrewCard): String =
        (listOfNotNull(card.area.takeIf { it.isNotBlank() }) + card.moods.map(::mood)).joinToString(" · ")

    /** 상세 "한 번에" — "3–5km · 천천히 달려요" (둘 다 없으면 null) */
    fun onceLine(card: CrewCard): String? =
        (listOfNotNull(card.distance?.let(::distance)) + card.moods.mapNotNull(::pace)).joinToString(" · ").ifEmpty { null }

    /** 러닝 스타일 칩(크루장 프로필) — 거리와 분위기 */
    fun styleChips(card: CrewCard): List<String> = listOfNotNull(card.distance?.let(::distance)) + card.moods.map(::moodLong)

    /** "9월 28일" */
    fun date(millis: Long): String =
        Instant.ofEpochMilli(millis).atZone(ZoneId.systemDefault()).toLocalDate()
            .format(DateTimeFormatter.ofPattern(s(R.string.crew_date_pattern), locale))

    fun km(value: Double): String = CrewRules.km(value)
}

@Composable
fun rememberCrewWords(): CrewWords {
    val context = LocalContext.current
    val configuration = LocalConfiguration.current
    val locale = configuration.locales[0] ?: Locale.getDefault()
    return remember(context, locale) { CrewWords(context, locale) }
}
