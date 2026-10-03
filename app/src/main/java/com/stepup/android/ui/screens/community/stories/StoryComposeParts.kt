package com.stepup.android.ui.screens.community.stories

import android.content.Context
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.automirrored.outlined.DirectionsRun
import androidx.compose.material.icons.automirrored.outlined.Article
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.outlined.CalendarMonth
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.GpsFixed
import androidx.compose.material.icons.outlined.HourglassTop
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.Place
import androidx.compose.material.icons.outlined.PlayArrow
import androidx.compose.material.icons.outlined.Route
import androidx.compose.material.icons.outlined.Schedule
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material.icons.outlined.Timer
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.stepup.android.R
import com.stepup.android.domain.GeoPoint
import com.stepup.android.domain.StoryComposeRules
import com.stepup.android.domain.StoryPhrase
import com.stepup.android.domain.StoryPhraseSet
import com.stepup.android.domain.StoryPlace
import com.stepup.android.domain.StoryPlaceSource
import com.stepup.android.domain.StoryRecordCard
import com.stepup.android.domain.StoryRecords
import com.stepup.android.domain.StoryRun
import com.stepup.android.domain.StoryRunRules
import com.stepup.android.domain.StoryText
import com.stepup.android.ui.components.LiveRouteMap
import com.stepup.android.ui.components.RunSpinner
import com.stepup.android.ui.components.runNumberStyle
import com.stepup.android.ui.components.runTextStyle
import com.stepup.android.ui.components.runTone
import com.stepup.android.ui.experience.FeedbackCue
import com.stepup.android.ui.experience.feedbackClickable
import java.time.Instant
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale

/*
 * 러닝 이야기 글쓰기(코스 글쓰기 WRITE01~20 · 커뮤니티 기본 CM12~21, 2026-10-03 파란 톤)의 조각 — 기록 칸 · 장소 ·
 * 한 줄 이야기 · 본문 · 기록 고르기 시트 · 위치 없이 장소 고르기 시트. 작성 순서는 기록 칸 → 공개 장소 → 장소 버튼(최대 셋)
 * → 문구 버튼 넷 → 본문 → 안내 → 올리기. 러닝 리메이크의 남색 부품([runTone])을 쓴다: 패널은 남색 면 · 파란 테두리,
 * 고른 버튼은 전기 파랑 면, 주 버튼은 흰 면 · 파란 아랫면. 누르는 곳은 48dp 이상이다.
 */

/** 글쓰기의 색 — [runTone] 에서 끌어낸다(밝은 테마도 같은 자리에 밝은 값) */
@Immutable
internal class ComposeInk(
    val text: Color,
    val secondary: Color,
    val accent: Color,
    val surface: Color,
    val edge: Color,
    val selected: Color,
    val selectedEdge: Color,
    val selectedText: Color,
    /** 기간이 지났다 · 올리지 못했다 */
    val warn: Color,
    val skeleton: Color,
)

@Composable
internal fun composeInk(): ComposeInk {
    val t = runTone()
    return remember(t) {
        ComposeInk(
            text = t.text, secondary = t.label, accent = if (t.dark) t.cyan else t.cobaltText,
            surface = t.panel, edge = t.panelEdge,
            selected = t.cobalt, selectedEdge = if (t.dark) t.cyan.copy(alpha = 0.85f) else t.cobalt, selectedText = Color.White,
            warn = t.dangerText, skeleton = if (t.dark) Color(0xFF1C3A66) else t.track,
        )
    }
}

internal val ComposeGutter = 20.dp

// ── 글자 ─────────────────────────────────────────────────────────

/** 러닝 이야기의 날짜 · 시간 · 문장 — 앱 언어의 문자열로 만든다 */
class StoryWords(private val context: Context, private val locale: Locale) {
    private fun s(id: Int, vararg args: Any): String = context.getString(id, *args)

    /** "9월 25일" — 저장되는 문장에도 이 고정 날짜를 쓴다(나중에 열어도 사실이 달라지지 않게) */
    fun date(day: LocalDate): String = day.format(DateTimeFormatter.ofPattern(s(R.string.story_run_date_pattern), locale))

    private fun time(millis: Long): String =
        Instant.ofEpochMilli(millis).atZone(StoryRunRules.ZONE).format(DateTimeFormatter.ofPattern(s(R.string.story_run_time_pattern), locale))

    /** "오늘" · "어제" · "3일 전" */
    fun ago(days: Long): String = when (days) {
        0L -> s(R.string.story_run_today)
        1L -> s(R.string.story_run_yesterday)
        else -> s(R.string.story_run_days_ago, days.coerceAtLeast(0).toInt())
    }

    /** 코스 카드 위 날짜 — "오늘 오전 7:12" · "어제 오후 6:30" · "9월 25일 · 3일 전" */
    fun cardDate(run: StoryRun, days: Long): String = when (days) {
        0L -> s(R.string.story_run_today_at, time(run.endedAt))
        1L -> s(R.string.story_run_yesterday_at, time(run.endedAt))
        else -> s(R.string.story_run_date_ago, date(run.day), ago(days))
    }

    /** "오늘" · "어제" · "9월 25일" */
    private fun shortDay(run: StoryRun, days: Long): String = if (days in 0..1) ago(days) else date(run.day)

    fun duration(sec: Int): String = when {
        sec >= 3_600 -> s(R.string.story_run_duration_hm, sec / 3_600, (sec % 3_600) / 60)
        sec >= 60 -> s(R.string.story_run_duration_ms, sec / 60, sec % 60)
        else -> s(R.string.story_run_duration_s, sec.coerceAtLeast(0))
    }

    /** "16분 48초 · 8′00″/km" */
    fun detail(run: StoryRun): String = listOfNotNull(duration(run.durationSec), StoryComposeRules.pace(run.paceSecPerKm)).joinToString(" · ")

    /** "오늘 · 3.24km · 24분 18초" — 코스 없이 붙인 기록 */
    fun summary(run: StoryRun, days: Long): String =
        listOf(shortDay(run, days), "${StoryComposeRules.km(run.distanceMeters)}km", duration(run.durationSec)).joinToString(" · ")

    fun phraseLabel(phrase: StoryPhrase): String = s(
        when (phrase) {
            StoryPhrase.START -> R.string.story_phrase_start
            StoryPhrase.WHERE -> R.string.story_phrase_where
            StoryPhrase.SHOES -> R.string.story_phrase_shoes
            StoryPhrase.QUESTION -> R.string.story_phrase_question
            StoryPhrase.AGAIN -> R.string.story_phrase_again
            StoryPhrase.LAST_RUN -> R.string.story_phrase_last_run
            StoryPhrase.RECOMMEND -> R.string.story_phrase_recommend
            StoryPhrase.FINISHED -> R.string.story_phrase_finished
            StoryPhrase.EASY -> R.string.story_phrase_easy
            StoryPhrase.COURSE -> R.string.story_phrase_course
            StoryPhrase.CURIOUS -> R.string.story_phrase_curious
        },
    )

    /**
     * 문구 버튼이 넣을 문장. [phrase] 가 null 이면 러닝을 붙였을 때의 기본 문장("9월 25일 마포대교에서 2.10km
     * 달렸어요.") — 붙인 기록이 없으면 빈 문자열. 러닝 문장은 붙인 기록의 고정 날짜 · 장소 · 거리로만 만든다.
     */
    fun starter(phrase: StoryPhrase?, run: StoryRun?, place: StoryPlace?): String {
        val where = place?.name?.let(StoryComposeRules::shortPlace)?.takeIf { it.isNotEmpty() }
        fun record(withPlace: Int, noPlace: Int, none: Int): String = when {
            run == null -> if (none == 0) "" else s(none)
            where != null -> s(withPlace, date(run.day), where, StoryComposeRules.km(run.distanceMeters))
            else -> s(noPlace, date(run.day), StoryComposeRules.km(run.distanceMeters))
        }
        return when (phrase) {
            null -> record(R.string.story_auto_run_place, R.string.story_auto_run, 0)
            StoryPhrase.START -> s(R.string.story_phrase_start_text)
            StoryPhrase.WHERE -> s(R.string.story_phrase_where_text)
            StoryPhrase.SHOES -> s(R.string.story_phrase_shoes_text)
            StoryPhrase.QUESTION -> s(R.string.story_phrase_question_text)
            StoryPhrase.AGAIN -> s(R.string.story_phrase_again_text)
            StoryPhrase.LAST_RUN -> s(R.string.story_phrase_last_run_text)
            StoryPhrase.RECOMMEND -> s(R.string.story_phrase_recommend_text)
            StoryPhrase.FINISHED -> record(R.string.story_phrase_finished_run_place, R.string.story_phrase_finished_run, R.string.story_phrase_finished_text)
            StoryPhrase.EASY -> record(R.string.story_phrase_easy_run_place, R.string.story_phrase_easy_run, R.string.story_phrase_easy_text)
            StoryPhrase.COURSE -> when {
                run == null -> s(R.string.story_phrase_course_text)
                where != null -> s(R.string.story_phrase_course_run_place, where, StoryComposeRules.km(run.distanceMeters))
                else -> s(R.string.story_phrase_course_run, StoryComposeRules.km(run.distanceMeters))
            }
            StoryPhrase.CURIOUS -> s(R.string.story_phrase_curious_text)
        }
    }
}

@Composable
fun rememberStoryWords(): StoryWords {
    val context = LocalContext.current
    val configuration = LocalConfiguration.current
    return remember(context, configuration) { StoryWords(context, configuration.locales[0] ?: Locale.getDefault()) }
}


// ── 버튼 ─────────────────────────────────────────────────────────

/**
 * 글쓰기의 칸 버튼 — 남색 면 · 파란 테두리, [selected] 면 전기 파랑 면 · 시안 테두리 · 흰 글자. 보이는 면은 48dp 이상이다.
 * [icon] 이 있으면 글자 앞(내 주변 · 최근 장소 · 직접 검색).
 */
@Composable
internal fun ComposeChip(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    selected: Boolean = false,
    enabled: Boolean = true,
    icon: ImageVector? = null,
    minHeight: Dp = 52.dp,
) {
    val ink = composeInk()
    val shape = RoundedCornerShape(14.dp)
    Row(
        modifier.heightIn(min = minHeight).clip(shape)
            .background(if (selected) Brush.horizontalGradient(listOf(ink.selected, Color(0xFF1F66FF))) else Brush.verticalGradient(listOf(ink.surface, ink.surface)), shape)
            .border(if (selected) 1.5.dp else 1.dp, if (selected) ink.selectedEdge else ink.edge, shape)
            .feedbackClickable(enabled = enabled, role = Role.Button, cue = FeedbackCue.Select, onClick = onClick)
            .semantics { this.selected = selected }
            .padding(horizontal = 10.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center,
    ) {
        val color = if (selected) ink.selectedText else ink.text
        if (icon != null) {
            Icon(icon, contentDescription = null, tint = if (selected) ink.selectedText else ink.accent, modifier = Modifier.size(22.dp))
            Spacer(Modifier.width(8.dp))
        }
        Text(text, style = runTextStyle(16.sp, color, FontWeight.Bold, 1.25f), textAlign = TextAlign.Center,
            maxLines = 2, overflow = TextOverflow.Ellipsis)
    }
}

/** 기록 칸 안의 작은 테두리 버튼 — "기록 변경" · "첨부 삭제" · "첫 러닝 시작" */
@Composable
private fun CardButton(text: String, onClick: () -> Unit, modifier: Modifier = Modifier, icon: ImageVector? = null) {
    val t = runTone()
    val ink = composeInk()
    val shape = RoundedCornerShape(12.dp)
    Row(
        modifier.heightIn(min = 48.dp).clip(shape).background(t.secondaryFace, shape)
            .border(1.5.dp, if (t.dark) t.cobaltText else t.secondaryEdge, shape)
            .feedbackClickable(role = Role.Button, onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center,
    ) {
        if (icon != null) {
            Icon(icon, contentDescription = null, tint = ink.text, modifier = Modifier.size(18.dp))
            Spacer(Modifier.width(6.dp))
        }
        Text(text, style = runTextStyle(15.sp, ink.text, FontWeight.SemiBold, 1.2f), maxLines = 1, overflow = TextOverflow.Ellipsis)
    }
}

/** 시안 테두리 알약 버튼 — 기록 칸 오른쪽의 하나뿐인 행동(첫 러닝 시작 · 다시 불러오기) */
@Composable
private fun CardPillButton(text: String, onClick: () -> Unit, modifier: Modifier = Modifier) {
    val t = runTone()
    val ink = composeInk()
    val shape = RoundedCornerShape(50)
    Box(
        modifier.heightIn(min = 48.dp).clip(shape).background(t.secondaryFace, shape)
            .border(1.5.dp, ink.accent, shape)
            .feedbackClickable(role = Role.Button, onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 8.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(text, style = runTextStyle(15.sp, ink.accent, FontWeight.Bold, 1.2f), maxLines = 1)
    }
}

// ── 기록 칸 ───────────────────────────────────────────────────────

/** 기록 칸의 행동 — 카드마다 쓰는 것만 보인다 */
class StoryRecordActions(
    /** 기록 변경 · 다른 기록 고르기 · 다른 기록 선택 · 기록 고르기 */
    val onPick: () -> Unit,
    /** 첨부 삭제 · 코스만 빼기 */
    val onRemove: () -> Unit,
    /** 다시 불러오기 */
    val onRetry: () -> Unit,
    /** 첫 러닝 시작 · 러닝 시작 · 러닝 화면으로 */
    val onStartRun: () -> Unit,
    /** 지난 기록 보기 */
    val onHistory: () -> Unit,
)

/** 기록 칸의 면 — 남색 면 · 파란 테두리 · 모서리 18 */
@Composable
private fun recordCardModifier(modifier: Modifier): Modifier {
    val t = runTone()
    val shape = RoundedCornerShape(18.dp)
    return modifier.fillMaxWidth().clip(shape)
        .background(Brush.verticalGradient(listOf(t.panelTop, t.panel)), shape)
        .border(1.5.dp, t.cobalt.copy(alpha = if (t.dark) 0.85f else 0.6f), shape)
}

/**
 * 글쓰기 위 기록 칸 — 상황마다 모습만 바뀐다. 코스 없음과 불러오기 실패를 다르게 보이고, 경로 없는 기록에는 코스 그림을
 * 만들지 않는다. [modifier]에는 testTag 를 붙이지 않는다 — 같은 자리에 붙는 상황 태그(story-record-never 등)를 바깥 태그가 덮는다.
 */
@Composable
fun StoryRecordCardView(card: StoryRecordCard, words: StoryWords, actions: StoryRecordActions, modifier: Modifier = Modifier) {
    val base = recordCardModifier(modifier)
    when (card) {
        is StoryRecordCard.Attached -> {
            val run = card.attachment.run
            if (run.hasRoute) {
                RouteCard(run, words.cardDate(run, card.daysAgo), words.detail(run), base.testTag("story-record-attached")) {
                    CardButton(stringResource(R.string.story_card_change), actions.onPick, Modifier.weight(1f).testTag("story-record-pick"), Icons.Outlined.Edit)
                    CardButton(stringResource(R.string.story_card_remove), actions.onRemove, Modifier.weight(1f).testTag("story-record-remove"), Icons.Outlined.Delete)
                }
            } else {
                // 경로 없는 러닝(WRITE05) — 확인된 거리 · 시간만. 지도 · 코스 그림을 만들지 않는다
                TextCard(
                    icon = Icons.AutoMirrored.Outlined.DirectionsRun,
                    title = stringResource(R.string.story_blue_summary_title),
                    sub = words.summary(run, card.daysAgo), subStrong = true,
                    footer = stringResource(R.string.story_blue_summary_note),
                    modifier = base.testTag("story-record-summary"),
                ) {
                    CardButton(stringResource(R.string.story_card_change), actions.onPick, Modifier.weight(1f).testTag("story-record-pick"), Icons.Outlined.Edit)
                    CardButton(stringResource(R.string.story_card_remove), actions.onRemove, Modifier.weight(1f).testTag("story-record-remove"), Icons.Outlined.Delete)
                }
            }
        }
        is StoryRecordCard.Expired -> ProblemCard(
            run = card.run,
            dateLine = words.cardDate(card.run, card.daysAgo),
            detail = words.detail(card.run),
            title = stringResource(R.string.story_card_expired_title),
            body = stringResource(R.string.story_card_expired_body, words.date(card.run.day), words.ago(card.daysAgo)),
            actions = actions,
            modifier = base.testTag("story-record-expired"),
        )
        is StoryRecordCard.Invalid -> ProblemCard(
            run = card.run,
            dateLine = null,
            detail = words.detail(card.run),
            title = stringResource(R.string.story_card_invalid_title),
            body = stringResource(R.string.story_card_invalid_body),
            actions = actions,
            modifier = base.testTag("story-record-invalid"),
        )
        StoryRecordCard.Loading -> TextCard(
            icon = null, busy = true,
            title = stringResource(R.string.story_card_loading_title),
            sub = stringResource(R.string.story_card_loading_body), modifier = base.testTag("story-record-loading"),
        )
        StoryRecordCard.FetchError -> TextCard(
            icon = Icons.Filled.Refresh,
            title = stringResource(R.string.story_card_error_title),
            sub = stringResource(R.string.story_blue_error_body), modifier = base.testTag("story-record-error"),
            side = { CardPillButton(stringResource(R.string.story_card_reload), actions.onRetry, Modifier.testTag("story-record-retry")) },
        )
        StoryRecordCard.ActiveRun -> TextCard(
            icon = Icons.AutoMirrored.Outlined.DirectionsRun,
            title = stringResource(R.string.story_card_active_title),
            sub = stringResource(R.string.story_card_active_body), modifier = base.testTag("story-record-active"),
            side = { CardPillButton(stringResource(R.string.story_card_back_to_run), actions.onStartRun, Modifier.testTag("story-record-start-run")) },
        )
        StoryRecordCard.Pending -> TextCard(
            icon = Icons.Outlined.HourglassTop,
            title = stringResource(R.string.story_card_pending_title),
            sub = stringResource(R.string.story_card_pending_body), modifier = base.testTag("story-record-pending"),
            side = { CardPillButton(stringResource(R.string.story_blue_check_again), actions.onRetry, Modifier.testTag("story-record-retry")) },
        )
        is StoryRecordCard.Available -> Box(base.testTag("story-record-available")) {
            // 달린 코스 붙이기(CM12) — 선택사항. 칸 전체가 하나의 버튼이고 누르면 기록 고르기 시트
            Row(
                Modifier.fillMaxWidth().heightIn(min = 96.dp).feedbackClickable(role = Role.Button, onClick = actions.onPick)
                    .padding(horizontal = 18.dp, vertical = 16.dp).testTag("story-record-pick"),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                val ink = composeInk()
                Icon(Icons.Outlined.Route, contentDescription = null, tint = ink.accent, modifier = Modifier.size(38.dp))
                Spacer(Modifier.width(16.dp))
                Column(Modifier.weight(1f)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(stringResource(R.string.story_blue_attach_title), style = runTextStyle(19.sp, ink.text, FontWeight.ExtraBold),
                            modifier = Modifier.weight(1f, fill = false).semantics { heading() })
                        Spacer(Modifier.width(8.dp))
                        OptionalPill()
                    }
                    Spacer(Modifier.height(2.dp))
                    Text(stringResource(R.string.story_blue_attach_body), style = runTextStyle(15.sp, ink.secondary, FontWeight.Medium))
                }
                Icon(Icons.Filled.Add, contentDescription = null, tint = ink.accent, modifier = Modifier.size(30.dp))
            }
        }
        is StoryRecordCard.Old -> TextCard(
            icon = Icons.Outlined.CalendarMonth,
            title = stringResource(R.string.story_card_old_title),
            sub = stringResource(R.string.story_card_old_body, words.date(card.lastDay), words.ago(card.daysAgo)),
            footer = stringResource(R.string.story_blue_window_info),
            modifier = base.testTag("story-record-old"),
        ) {
            CardButton(stringResource(R.string.story_card_history), actions.onHistory, Modifier.weight(1f).testTag("story-record-history"),
                Icons.AutoMirrored.Outlined.Article)
            CardButton(stringResource(R.string.story_card_start_run), actions.onStartRun, Modifier.weight(1f).testTag("story-record-start-run"),
                Icons.Outlined.PlayArrow)
        }
        StoryRecordCard.Never -> TextCard(
            icon = Icons.AutoMirrored.Outlined.Article,
            title = stringResource(R.string.story_card_never_title),
            sub = stringResource(R.string.story_blue_never_body), modifier = base.testTag("story-record-never"),
            side = { CardPillButton(stringResource(R.string.story_card_first_run), actions.onStartRun, Modifier.testTag("story-record-start-run")) },
        )
        // 처음인지 오래 쉬었는지 단정할 근거가 없다(WRITE11) — 날짜를 지어내지 않는 중립 문구
        StoryRecordCard.Neutral -> TextCard(
            icon = Icons.Outlined.CalendarMonth,
            title = stringResource(R.string.story_card_old_title),
            sub = stringResource(R.string.story_card_neutral_body), modifier = base.testTag("story-record-neutral"),
        ) {
            CardButton(stringResource(R.string.story_card_history), actions.onHistory, Modifier.weight(1f).testTag("story-record-history"),
                Icons.AutoMirrored.Outlined.Article)
            CardButton(stringResource(R.string.story_card_start_run), actions.onStartRun, Modifier.weight(1f).testTag("story-record-start-run"),
                Icons.Outlined.PlayArrow)
        }
    }
}

/** "선택" 알약 — 코스 첨부는 선택사항이다 */
@Composable
private fun OptionalPill() {
    val ink = composeInk()
    val shape = RoundedCornerShape(50)
    Box(Modifier.clip(shape).border(1.dp, ink.accent.copy(alpha = 0.8f), shape).padding(horizontal = 10.dp, vertical = 2.dp)) {
        Text(stringResource(R.string.story_blue_optional), style = runTextStyle(13.sp, ink.accent, FontWeight.SemiBold, 1.3f))
    }
}

/**
 * 글 카드 — 왼쪽 아이콘(또는 도는 표시), 제목 · 설명, 오른쪽 하나뿐인 버튼([side]) 또는 아래 버튼 줄([buttons]),
 * 맨 아래 한 줄 안내([footer]).
 */
@Composable
private fun TextCard(
    icon: ImageVector?,
    title: String,
    sub: String,
    modifier: Modifier,
    busy: Boolean = false,
    subStrong: Boolean = false,
    footer: String? = null,
    side: (@Composable () -> Unit)? = null,
    buttons: (@Composable RowScope.() -> Unit)? = null,
) {
    val ink = composeInk()
    Column(modifier.padding(start = 18.dp, end = 16.dp, top = 16.dp, bottom = 16.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.size(40.dp), contentAlignment = Alignment.Center) {
                if (busy) RunSpinner(Modifier.size(32.dp))
                else if (icon != null) Icon(icon, contentDescription = null, tint = ink.accent, modifier = Modifier.size(34.dp))
            }
            Spacer(Modifier.width(14.dp))
            Column(Modifier.weight(1f)) {
                Text(title, style = runTextStyle(18.sp, ink.text, FontWeight.ExtraBold, 1.3f), modifier = Modifier.semantics { heading() })
                Spacer(Modifier.height(2.dp))
                Text(sub, style = runTextStyle(if (subStrong) 16.sp else 15.sp, if (subStrong) ink.accent else ink.secondary,
                    if (subStrong) FontWeight.Bold else FontWeight.Medium, 1.4f))
            }
            if (side != null) {
                Spacer(Modifier.width(10.dp))
                side()
            }
        }
        if (buttons != null) {
            Spacer(Modifier.height(14.dp))
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp), verticalAlignment = Alignment.CenterVertically,
                content = buttons)
        }
        if (footer != null) {
            Spacer(Modifier.height(12.dp))
            Box(Modifier.fillMaxWidth().height(1.dp).background(ink.edge))
            Spacer(Modifier.height(10.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Outlined.Info, contentDescription = null, tint = ink.secondary, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(8.dp))
                Text(footer, style = runTextStyle(14.sp, ink.secondary, FontWeight.Medium))
            }
        }
    }
}

/** 코스가 붙은 카드 — 왼쪽 실제 코스 지도, 오른쪽 날짜 · 거리 · 시간 · 페이스 · 버튼 둘(WRITE03) */
@Composable
private fun RouteCard(
    run: StoryRun,
    dateLine: String?,
    detail: String,
    modifier: Modifier,
    notice: (@Composable () -> Unit)? = null,
    buttons: @Composable RowScope.() -> Unit,
) {
    val ink = composeInk()
    Row(modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
        if (run.hasRoute) {
            StoryRouteThumb(run.route, Modifier.size(width = 112.dp, height = if (notice != null) 160.dp else 124.dp), seed = run.id.toInt(), radius = 12.dp)
        } else {
            Box(Modifier.size(width = 72.dp, height = 72.dp).clip(RoundedCornerShape(12.dp)).background(ink.skeleton), contentAlignment = Alignment.Center) {
                Icon(Icons.AutoMirrored.Outlined.DirectionsRun, contentDescription = null, tint = ink.secondary, modifier = Modifier.size(28.dp))
            }
        }
        Spacer(Modifier.width(14.dp))
        Column(Modifier.weight(1f)) {
            if (dateLine != null) {
                Text(dateLine, style = runTextStyle(14.sp, ink.secondary, FontWeight.Medium), maxLines = 1,
                    overflow = TextOverflow.Ellipsis, modifier = Modifier.testTag("story-record-date"))
            }
            Row(verticalAlignment = Alignment.Bottom) {
                Text(StoryComposeRules.km(run.distanceMeters), style = runNumberStyle(32.sp, ink.text),
                    modifier = Modifier.alignByBaseline().testTag("story-record-km"))
                Spacer(Modifier.width(6.dp))
                Text("km", style = runTextStyle(17.sp, ink.text, FontWeight.Bold), modifier = Modifier.alignByBaseline())
            }
            Text(detail, style = runTextStyle(14.sp, ink.secondary, FontWeight.Medium), maxLines = 1, overflow = TextOverflow.Ellipsis)
            Spacer(Modifier.height(8.dp))
            notice?.let {
                it()
                Spacer(Modifier.height(8.dp))
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), content = buttons)
        }
    }
}

/**
 * 붙인 기록을 지금은 쓸 수 없다(WRITE04 기간 지남 · WRITE19 사용할 수 없음) — 기록은 그대로 보이고 안내 칸과
 * "다른 기록 고르기" · "코스만 빼기". 사유를 모르면 부정행위 · 타인 기록이라고 단정하지 않는다.
 */
@Composable
private fun ProblemCard(
    run: StoryRun,
    dateLine: String?,
    detail: String,
    title: String,
    body: String,
    actions: StoryRecordActions,
    modifier: Modifier,
) {
    val ink = composeInk()
    RouteCard(
        run, dateLine, detail, modifier,
        notice = {
            val shape = RoundedCornerShape(12.dp)
            Row(
                Modifier.fillMaxWidth().clip(shape).border(1.dp, ink.edge, shape).padding(10.dp),
                verticalAlignment = Alignment.Top,
            ) {
                Icon(Icons.Outlined.Info, contentDescription = null, tint = ink.secondary, modifier = Modifier.size(20.dp))
                Spacer(Modifier.width(8.dp))
                Column {
                    Text(title, style = runTextStyle(15.sp, ink.text, FontWeight.Bold))
                    Text(body, style = runTextStyle(13.sp, ink.secondary, FontWeight.Medium))
                }
            }
        },
    ) {
        CardButton(stringResource(R.string.story_card_pick_another), actions.onPick, Modifier.weight(1f).testTag("story-record-pick"),
            Icons.AutoMirrored.Outlined.Article)
        CardButton(stringResource(R.string.story_card_remove_course), actions.onRemove, Modifier.weight(1f).testTag("story-record-remove"),
            Icons.Outlined.Delete)
    }
}

/** 코스 그림 — 저장된 좌표를 실제 남색 지도 위에. 좌표가 둘 미만이면 부르지 않는다(가짜 코스를 그리지 않는다) */
@Composable
internal fun StoryRouteThumb(route: List<GeoPoint>, modifier: Modifier, seed: Int, radius: Dp) {
    val ink = composeInk()
    Box(modifier.clip(RoundedCornerShape(radius)).background(ink.skeleton).border(1.dp, ink.edge, RoundedCornerShape(radius))) {
        if (route.size >= 2) StoryMapTone { LiveRouteMap(route, Modifier.matchParentSize(), seed = seed, routeColor = runTone().cyan) }
    }
}

// ── 장소 · 한 줄 이야기 · 본문 ───────────────────────────────────────

/** 장소 버튼 하나의 이름과 아이콘 */
@Composable
internal fun placeSourceLabel(source: StoryPlaceSource): Pair<String, ImageVector> = when (source) {
    StoryPlaceSource.COURSE -> stringResource(R.string.story_place_src_course) to Icons.Outlined.Route
    StoryPlaceSource.NEARBY -> stringResource(R.string.story_place_src_nearby) to Icons.Outlined.GpsFixed
    StoryPlaceSource.RECENT -> stringResource(R.string.story_place_src_recent) to Icons.Outlined.Schedule
    StoryPlaceSource.SEARCH -> stringResource(R.string.story_place_src_search) to Icons.Outlined.Search
}

@Composable
fun StoryPlaceBlock(
    place: StoryPlace?,
    sources: List<StoryPlaceSource>,
    selected: StoryPlaceSource?,
    onOpen: () -> Unit,
    onSource: (StoryPlaceSource) -> Unit,
) {
    val ink = composeInk()
    Text(stringResource(R.string.story_compose_place_label), style = runTextStyle(15.sp, ink.secondary, FontWeight.Medium),
        modifier = Modifier.semantics { heading() })
    Spacer(Modifier.height(8.dp))
    val shape = RoundedCornerShape(16.dp)
    Row(
        Modifier.fillMaxWidth().heightIn(min = 64.dp).clip(shape).background(ink.surface, shape).border(1.dp, ink.edge, shape)
            .feedbackClickable(role = Role.Button, onClick = onOpen).padding(start = 16.dp, end = 12.dp, top = 10.dp, bottom = 10.dp)
            .testTag("story-compose-pick"),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(Icons.Outlined.Place, contentDescription = null, tint = ink.accent, modifier = Modifier.size(28.dp))
        Spacer(Modifier.width(14.dp))
        Column(Modifier.weight(1f)) {
            Text(
                place?.name ?: stringResource(R.string.story_compose_place_empty),
                style = runTextStyle(18.sp, ink.text, FontWeight.ExtraBold), maxLines = 1, overflow = TextOverflow.Ellipsis,
                modifier = Modifier.testTag("story-compose-place"),
            )
            Text(
                stringResource(if (place != null) R.string.story_blue_place_public else R.string.story_blue_place_public_empty),
                style = runTextStyle(14.sp, ink.secondary, FontWeight.Medium), maxLines = 1, overflow = TextOverflow.Ellipsis,
            )
        }
        Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, contentDescription = null, tint = ink.secondary, modifier = Modifier.size(26.dp))
    }
    Spacer(Modifier.height(10.dp))
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        sources.forEach { source ->
            val (label, icon) = placeSourceLabel(source)
            ComposeChip(
                text = label,
                onClick = { onSource(source) },
                modifier = Modifier.weight(1f).testTag("story-src-${source.name}"),
                selected = source == selected,
                icon = icon,
            )
        }
    }
}

/** 한 줄 이야기 — 2 × 2 문구 버튼. 불러오는 중이면 빈 자리만(처음인지 오래 쉬었는지 아직 모른다) */
@Composable
fun StoryPhraseBlock(set: StoryPhraseSet?, selected: StoryPhrase?, words: StoryWords, onPhrase: (StoryPhrase) -> Unit) {
    val ink = composeInk()
    Text(stringResource(R.string.story_phrase_title), style = runTextStyle(18.sp, ink.text, FontWeight.ExtraBold),
        modifier = Modifier.semantics { heading() })
    Spacer(Modifier.height(10.dp))
    val phrases = set?.let { StoryPhrase.of(it) }
    Column(
        Modifier.testTag(if (set == null) "story-phrases-loading" else "story-phrases-${set.name}"),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        (0 until 2).forEach { row ->
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                (0 until 2).forEach { col ->
                    val phrase = phrases?.getOrNull(row * 2 + col)
                    if (phrase == null) {
                        Box(
                            Modifier.weight(1f).height(52.dp).clip(RoundedCornerShape(14.dp))
                                .background(ink.surface.copy(alpha = 0.55f)).border(1.dp, ink.edge.copy(alpha = 0.5f), RoundedCornerShape(14.dp)),
                        )
                    } else {
                        ComposeChip(
                            text = words.phraseLabel(phrase),
                            onClick = { onPhrase(phrase) },
                            modifier = Modifier.weight(1f).testTag("story-phrase-${phrase.name}"),
                            selected = phrase == selected,
                        )
                    }
                }
            }
        }
    }
}

/** 본문 — 남색 칸 · 파란 테두리. 1,000자에 가까워지면 남은 글자를 보인다. 진입하자마자 키보드를 올리지 않는다 */
@Composable
fun StoryBodyBox(text: String, onChange: (String) -> Unit, enabled: Boolean, large: Boolean) {
    val ink = composeInk()
    val shape = RoundedCornerShape(16.dp)
    Box(
        Modifier.fillMaxWidth().heightIn(min = if (large) 120.dp else 150.dp).clip(shape)
            .background(Brush.verticalGradient(listOf(ink.surface, ink.surface.copy(alpha = 0.92f))), shape)
            .border(1.dp, ink.edge, shape)
            .padding(start = 18.dp, end = 18.dp, top = 16.dp, bottom = 14.dp),
    ) {
        if (text.isEmpty()) Text(stringResource(R.string.story_blue_write_hint), style = runTextStyle(17.sp, ink.secondary.copy(alpha = 0.8f), FontWeight.Medium, 1.6f))
        BasicTextField(
            value = text,
            onValueChange = onChange,
            enabled = enabled,
            textStyle = runTextStyle(17.sp, ink.text, FontWeight.Medium, 1.6f),
            cursorBrush = SolidColor(ink.accent),
            modifier = Modifier.fillMaxWidth().testTag("story-compose-text"),
        )
        val count = StoryText.length(text)
        if (count >= StoryText.MAX_CHARS - 100) {
            Text(
                "%,d / %,d".format(count, StoryText.MAX_CHARS),
                style = runTextStyle(12.sp, if (count >= StoryText.MAX_CHARS) ink.warn else ink.secondary, FontWeight.Medium),
                modifier = Modifier.align(Alignment.BottomEnd).padding(top = 8.dp).testTag("story-compose-count"),
            )
        }
    }
}

// ── 기록 고르기 시트 ──────────────────────────────────────────────

/**
 * 붙일 기록 고르기(WRITE09 · WRITE11 · WRITE16) — 전체 · 오늘 · 어제 · 2일 전 · 3일 전으로 거른다. 행이나 "붙이기"를 한 번
 * 누르면 바로 붙고 작성으로 돌아간다(따로 확인하지 않는다). 고른 날만 비었으면 그 날의 기록 없음과 전체 기간의 기록 없음을
 * 나눠 말하고 "전체 보기"로 되돌린다. 아래 "기록 없이 이야기 쓰기"는 시트만 닫는다(붙여 둔 기록은 그대로).
 */
@Composable
fun StoryRunsSheet(
    records: StoryRecords,
    attachedId: Long?,
    words: StoryWords,
    onPick: (StoryRun) -> Unit,
    onRetry: () -> Unit,
    @Suppress("UNUSED_PARAMETER") onHistory: () -> Unit,
    onDismiss: () -> Unit,
) {
    val ink = composeInk()
    var day by rememberSaveable { mutableIntStateOf(-1) }
    StorySheet(stringResource(R.string.story_blue_runs_title), onDismiss, modifier = Modifier.testTag("story-runs-sheet")) {
        Text(stringResource(R.string.story_blue_runs_body), style = runTextStyle(15.sp, ink.secondary, FontWeight.Medium))
        Spacer(Modifier.height(14.dp))
        Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            (-1..StoryRunRules.WINDOW_DAYS).forEach { option ->
                ComposeChip(
                    text = if (option < 0) stringResource(R.string.story_runs_all) else words.ago(option.toLong()),
                    onClick = { day = option },
                    modifier = Modifier.widthIn(min = 66.dp).testTag("story-runs-day-$option"),
                    selected = option == day,
                    minHeight = 48.dp,
                )
            }
        }
        Spacer(Modifier.height(6.dp))
        when (records) {
            StoryRecords.Loading -> Row(Modifier.fillMaxWidth().padding(vertical = 22.dp), verticalAlignment = Alignment.CenterVertically) {
                RunSpinner(Modifier.size(24.dp))
                Spacer(Modifier.width(12.dp))
                Text(stringResource(R.string.story_card_loading_title), style = runTextStyle(15.sp, ink.secondary, FontWeight.Medium))
            }
            StoryRecords.Failed -> Column(Modifier.fillMaxWidth().padding(vertical = 14.dp)) {
                Text(stringResource(R.string.story_card_error_title), style = runTextStyle(17.sp, ink.text, FontWeight.Bold))
                Spacer(Modifier.height(4.dp))
                Text(stringResource(R.string.story_card_error_body), style = runTextStyle(15.sp, ink.secondary, FontWeight.Medium))
                Spacer(Modifier.height(12.dp))
                StoryButton(stringResource(R.string.story_card_reload), onRetry, Modifier.testTag("story-runs-retry"), style = StoryButtonStyle.SECONDARY)
            }
            is StoryRecords.Ready -> {
                val today = records.options.today
                val all = records.options.runs
                val shown = if (day < 0) all else all.filter { StoryRunRules.daysAgo(it.day, today) == day.toLong() }
                if (shown.isEmpty()) {
                    Column(
                        Modifier.fillMaxWidth().padding(vertical = 18.dp)
                            .testTag(if (all.isEmpty()) "story-runs-empty" else "story-runs-empty-day"),
                        horizontalAlignment = Alignment.CenterHorizontally,
                    ) {
                        Icon(Icons.Outlined.CalendarMonth, contentDescription = null, tint = ink.accent, modifier = Modifier.size(40.dp))
                        Spacer(Modifier.height(10.dp))
                        Text(
                            stringResource(if (all.isEmpty()) R.string.story_runs_empty else R.string.story_runs_empty_day),
                            style = runTextStyle(17.sp, ink.text, FontWeight.Bold), textAlign = TextAlign.Center,
                        )
                        if (all.isNotEmpty()) {
                            Spacer(Modifier.height(10.dp))
                            StoryTextButton(stringResource(R.string.story_runs_show_all), { day = -1 },
                                Modifier.testTag("story-runs-show-all"), color = ink.accent)
                        }
                    }
                } else {
                    Column(Modifier.fillMaxWidth().heightIn(max = 380.dp).verticalScroll(rememberScrollState())) {
                        shown.forEachIndexed { index, run ->
                            StoryRunRow(run, words, StoryRunRules.daysAgo(run.day, today), attached = run.id == attachedId) { onPick(run) }
                            if (index != shown.lastIndex) StoryDivider()
                        }
                    }
                    Spacer(Modifier.height(6.dp))
                    Text(
                        stringResource(R.string.story_blue_runs_footer, words.date(today.minusDays(StoryRunRules.WINDOW_DAYS.toLong()))),
                        style = runTextStyle(13.sp, ink.secondary, FontWeight.Medium), textAlign = TextAlign.Center,
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
            }
        }
        Spacer(Modifier.height(12.dp))
        StoryButton(stringResource(R.string.story_blue_runs_skip), onDismiss, Modifier.testTag("story-runs-skip"), style = StoryButtonStyle.SECONDARY)
    }
}

@Composable
private fun StoryRunRow(run: StoryRun, words: StoryWords, days: Long, attached: Boolean, onClick: () -> Unit) {
    val ink = composeInk()
    Row(
        Modifier.fillMaxWidth().heightIn(min = 88.dp).clip(RoundedCornerShape(14.dp))
            .feedbackClickable(role = Role.Button, onClick = onClick).padding(vertical = 10.dp).testTag("story-run-${run.id}"),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (run.hasRoute) {
            StoryRouteThumb(run.route, Modifier.size(width = 84.dp, height = 72.dp), seed = run.id.toInt(), radius = 10.dp)
        } else {
            Box(Modifier.size(width = 84.dp, height = 72.dp).clip(RoundedCornerShape(10.dp)).background(ink.skeleton), contentAlignment = Alignment.Center) {
                Icon(Icons.AutoMirrored.Outlined.DirectionsRun, contentDescription = stringResource(R.string.story_runs_no_route), tint = ink.secondary,
                    modifier = Modifier.size(24.dp))
            }
        }
        Spacer(Modifier.width(14.dp))
        Column(Modifier.weight(1f)) {
            Text(words.cardDate(run, days), style = runTextStyle(15.sp, ink.text, FontWeight.SemiBold), maxLines = 1, overflow = TextOverflow.Ellipsis)
            Spacer(Modifier.height(4.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Outlined.Place, contentDescription = null, tint = ink.accent, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(4.dp))
                Text("${StoryComposeRules.km(run.distanceMeters)} km", style = runTextStyle(15.sp, ink.text, FontWeight.SemiBold))
                Spacer(Modifier.width(14.dp))
                Icon(Icons.Outlined.Timer, contentDescription = null, tint = ink.accent, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(4.dp))
                Text(words.duration(run.durationSec), style = runTextStyle(15.sp, ink.text, FontWeight.SemiBold), maxLines = 1)
            }
            if (!run.hasRoute) {
                Text(stringResource(R.string.story_runs_no_route), style = runTextStyle(13.sp, ink.secondary, FontWeight.Medium))
            }
        }
        Spacer(Modifier.width(8.dp))
        if (attached) {
            Box(Modifier.size(32.dp).clip(CircleShape).background(ink.accent), contentAlignment = Alignment.Center) {
                Icon(Icons.Filled.Check, contentDescription = null, tint = runTone().screen, modifier = Modifier.size(20.dp))
            }
        } else {
            val shape = RoundedCornerShape(12.dp)
            Box(
                Modifier.heightIn(min = 44.dp).clip(shape).border(1.5.dp, ink.accent, shape).padding(horizontal = 14.dp),
                contentAlignment = Alignment.Center,
            ) {
                Text(stringResource(R.string.story_blue_attach), style = runTextStyle(15.sp, ink.accent, FontWeight.Bold))
            }
        }
    }
}

// ── 위치 없이 장소 고르기 ────────────────────────────────────────────

/** 현재 위치를 쓸 수 없을 때(WRITE07, 권한 거부 · 위치 꺼짐) — 이름으로 검색 · 지도에서 고르기 · 위치 설정 열기 */
@Composable
fun StoryNoLocationSheet(onSearch: () -> Unit, onMap: () -> Unit, onSettings: () -> Unit, onDismiss: () -> Unit) {
    val ink = composeInk()
    StorySheet(stringResource(R.string.story_blue_noloc_title), onDismiss, centered = true, modifier = Modifier.testTag("story-noloc-sheet")) {
        Spacer(Modifier.height(4.dp))
        Text(stringResource(R.string.story_blue_noloc_body), style = runTextStyle(16.sp, ink.text, FontWeight.Medium, 1.55f),
            textAlign = TextAlign.Center)
        Spacer(Modifier.height(22.dp))
        StoryButton(stringResource(R.string.story_noloc_search), onSearch, Modifier.testTag("story-noloc-search"))
        Spacer(Modifier.height(10.dp))
        StoryButton(stringResource(R.string.story_noloc_map), onMap, Modifier.testTag("story-noloc-map"), style = StoryButtonStyle.SECONDARY)
        Spacer(Modifier.height(4.dp))
        StoryTextButton(stringResource(R.string.story_noloc_settings), onSettings, Modifier.testTag("story-noloc-settings"), color = ink.accent)
    }
}
