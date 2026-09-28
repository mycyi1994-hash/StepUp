package com.stepup.android.ui.screens.community.stories

import android.content.Context
import androidx.compose.foundation.background
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.automirrored.outlined.DirectionsRun
import androidx.compose.material.icons.outlined.Place
import androidx.compose.material3.CircularProgressIndicator
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
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
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
import com.stepup.android.ui.experience.feedbackClickable
import com.stepup.android.ui.theme.StepUpColors
import java.time.Instant
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale

/*
 * 러닝 이야기 글쓰기(2026-09-28 쉬운 글쓰기 상황별 시안, 390 × 844)의 조각 — 기록 칸 · 장소 · 한 줄 이야기 ·
 * 본문 · 기록 고르기 시트 · 위치 없이 장소 고르기 시트. 시안의 수치를 그대로 옮겼다: 좌우 24, 기록 칸 142 · 모서리 18,
 * 장소 줄 52 · 15, 버튼 40 · 12, 본문 141 · 16, 올리기 54 · 17. 누르는 곳은 48dp 이상이다 — 40dp 버튼은 위아래 4dp 를
 * 터치 영역으로 더 잡고, 시안의 간격에서 그만큼 뺐다.
 */

/** 시안의 색 — 어두운 테마는 시안 값, 밝은 테마는 설정 v1 의 밝은 값에 맞춘다 */
@Immutable
internal class ComposeInk(
    val text: Color,
    val secondary: Color,
    val accent: Color,
    val surface: Color,
    val sheet: Color,
    /** 기록 칸 안의 작은 버튼 */
    val inner: Color,
    val selected: Color,
    val selectedText: Color,
    /** 올리기를 누를 수 없을 때 */
    val disabled: Color,
    /** 기간이 지났다 · 올리지 못했다 */
    val warn: Color,
    val primaryFace: Brush,
    val primaryText: Color,
    val skeleton: Color,
)

private val DarkInk = ComposeInk(
    text = Color(0xFFF2F4FC), secondary = Color(0xFF98A8C0), accent = Color(0xFFA3BFFE),
    surface = Color(0xFF0E192A), sheet = Color(0xFF111D2E), inner = Color(0xFF1A2C45),
    selected = Color(0xFF243E63), selectedText = Color(0xFFF2F4FC), disabled = Color(0xFF243249),
    warn = Color(0xFFDDBA92),
    primaryFace = Brush.verticalGradient(listOf(Color(0xFFF8F9FF), Color(0xFFEDF0F9))), primaryText = Color(0xFF081223),
    skeleton = Color(0xFF1A2940),
)

private val LightInk = ComposeInk(
    text = Color(0xFF10203B), secondary = Color(0xFF536580), accent = Color(0xFF335EAB),
    surface = Color(0xFFE7EDF6), sheet = Color(0xFFFFFFFF), inner = Color(0xFFD6E1F1),
    selected = Color(0xFF335EAB), selectedText = Color(0xFFFFFFFF), disabled = Color(0xFFD5DCE8),
    warn = Color(0xFF94561C),
    primaryFace = Brush.verticalGradient(listOf(Color(0xFF1B2D4E), Color(0xFF10203B))), primaryText = Color(0xFFFFFFFF),
    skeleton = Color(0xFFDCE4F0),
)

@Composable
internal fun composeInk(): ComposeInk = if (StepUpColors.dark) DarkInk else LightInk

internal val ComposeGutter = 24.dp

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
 * 40dp 버튼 — 보이는 면은 시안대로 40dp, 누르는 곳은 위아래 4dp 를 더해 48dp.
 * [selected] 면 시안의 고른 색(짙은 파랑 면 · 흰 글자).
 */
@Composable
internal fun ComposeChip(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    face: Color,
    ink: Color,
    enabled: Boolean = true,
) {
    Box(
        modifier.heightIn(min = 48.dp).feedbackClickable(enabled = enabled, role = Role.Button, onClick = onClick)
            .padding(vertical = 4.dp),
        contentAlignment = Alignment.Center,
    ) {
        Box(
            Modifier.fillMaxWidth().heightIn(min = 40.dp).clip(RoundedCornerShape(12.dp)).background(face)
                .padding(horizontal = 10.dp, vertical = 8.dp),
            contentAlignment = Alignment.Center,
        ) {
            Text(text, color = ink, fontSize = 13.sp, fontWeight = FontWeight.SemiBold, textAlign = TextAlign.Center, lineHeight = 17.sp)
        }
    }
}

/** 올리기 — 54dp · 모서리 17. 누를 수 없으면 짙은 면에 흐린 글자, 올리는 동안은 도는 표시 */
@Composable
internal fun ComposePrimaryButton(text: String, onClick: () -> Unit, enabled: Boolean, busy: Boolean, modifier: Modifier = Modifier) {
    val ink = composeInk()
    val active = enabled && !busy
    val shape = RoundedCornerShape(17.dp)
    Row(
        modifier.fillMaxWidth().heightIn(min = 54.dp).clip(shape)
            .then(if (active) Modifier.background(ink.primaryFace, shape) else Modifier.background(ink.disabled, shape))
            .feedbackClickable(enabled = active, role = Role.Button, onClick = onClick)
            .padding(horizontal = 18.dp, vertical = 12.dp),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        val color = if (active) ink.primaryText else ink.secondary
        if (busy) {
            CircularProgressIndicator(Modifier.size(16.dp), color = color, strokeWidth = 2.dp)
            Spacer(Modifier.width(8.dp))
        }
        Text(text, color = color, fontSize = 16.sp, fontWeight = FontWeight.SemiBold, textAlign = TextAlign.Center)
    }
}

/** 시트의 큰 버튼 — 51dp · 모서리 17. 주 행동은 흰 면, 나머지는 짙은 면 */
@Composable
internal fun ComposeSheetButton(text: String, onClick: () -> Unit, primary: Boolean, modifier: Modifier = Modifier) {
    val ink = composeInk()
    val shape = RoundedCornerShape(17.dp)
    Box(
        modifier.fillMaxWidth().heightIn(min = 51.dp).clip(shape)
            .then(if (primary) Modifier.background(ink.primaryFace, shape) else Modifier.background(ink.surface, shape))
            .feedbackClickable(role = Role.Button, onClick = onClick)
            .padding(horizontal = 18.dp, vertical = 12.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(text, color = if (primary) ink.primaryText else ink.text, fontSize = 16.sp, fontWeight = FontWeight.SemiBold,
            textAlign = TextAlign.Center)
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

/**
 * 글쓰기 위 기록 칸 — 상황마다 모습만 바뀌고 자리(142dp)는 같다. 코스 없음과 불러오기 실패를 다르게 보이고,
 * 경로 없는 기록에는 코스 그림을 만들지 않는다. [modifier]에는 testTag 를 붙이지 않는다 — 같은 자리에 붙는
 * 상황 태그(story-record-never 등)를 바깥 태그가 덮는다.
 */
@Composable
fun StoryRecordCardView(card: StoryRecordCard, words: StoryWords, actions: StoryRecordActions, modifier: Modifier = Modifier) {
    val ink = composeInk()
    val base = modifier.fillMaxWidth().heightIn(min = 142.dp).clip(RoundedCornerShape(18.dp)).background(ink.surface)
    when (card) {
        is StoryRecordCard.Attached -> {
            val run = card.attachment.run
            if (run.hasRoute) {
                RouteCard(run, words.cardDate(run, card.daysAgo), words.detail(run), actions, base.testTag("story-record-attached"))
            } else {
                TextCard(
                    title = stringResource(R.string.story_card_summary_title),
                    sub = words.summary(run, card.daysAgo), subColor = ink.accent, subStrong = true,
                    modifier = base.testTag("story-record-summary"),
                ) {
                    CardButton(stringResource(R.string.story_card_pick_other), actions.onPick, Modifier.weight(1f).testTag("story-record-pick"))
                    CardButton(stringResource(R.string.story_card_remove), actions.onRemove, Modifier.weight(1f).testTag("story-record-remove"))
                }
            }
        }
        is StoryRecordCard.Expired -> TextCard(
            title = stringResource(R.string.story_card_expired_title),
            sub = stringResource(R.string.story_card_expired_body, words.date(card.run.day), words.ago(card.daysAgo)),
            subColor = ink.warn, modifier = base.testTag("story-record-expired"),
        ) {
            CardButton(stringResource(R.string.story_card_pick_another), actions.onPick, Modifier.weight(1f).testTag("story-record-pick"))
            CardButton(stringResource(R.string.story_card_remove_course), actions.onRemove, Modifier.weight(1f).testTag("story-record-remove"))
        }
        is StoryRecordCard.Invalid -> TextCard(
            title = stringResource(R.string.story_card_invalid_title),
            sub = stringResource(R.string.story_card_invalid_body), subColor = ink.warn,
            modifier = base.testTag("story-record-invalid"),
        ) {
            CardButton(stringResource(R.string.story_card_pick_another), actions.onPick, Modifier.weight(1f).testTag("story-record-pick"))
            CardButton(stringResource(R.string.story_card_remove_course), actions.onRemove, Modifier.weight(1f).testTag("story-record-remove"))
        }
        StoryRecordCard.Loading -> TextCard(
            title = stringResource(R.string.story_card_loading_title),
            sub = stringResource(R.string.story_card_loading_body), modifier = base.testTag("story-record-loading"),
        ) {
            Box(Modifier.heightIn(min = 48.dp), contentAlignment = Alignment.CenterStart) {
                CircularProgressIndicator(Modifier.size(18.dp), color = ink.accent, strokeWidth = 2.dp)
            }
        }
        StoryRecordCard.FetchError -> TextCard(
            title = stringResource(R.string.story_card_error_title),
            sub = stringResource(R.string.story_card_error_body), modifier = base.testTag("story-record-error"),
        ) {
            CardButton(stringResource(R.string.story_card_reload), actions.onRetry, Modifier.widthIn(min = 154.dp).testTag("story-record-retry"))
        }
        StoryRecordCard.ActiveRun -> TextCard(
            title = stringResource(R.string.story_card_active_title),
            sub = stringResource(R.string.story_card_active_body), modifier = base.testTag("story-record-active"),
        ) {
            CardButton(stringResource(R.string.story_card_back_to_run), actions.onStartRun, Modifier.widthIn(min = 154.dp).testTag("story-record-start-run"))
        }
        StoryRecordCard.Pending -> TextCard(
            title = stringResource(R.string.story_card_pending_title),
            sub = stringResource(R.string.story_card_pending_body), modifier = base.testTag("story-record-pending"),
        ) {
            CardButton(stringResource(R.string.story_card_reload), actions.onRetry, Modifier.widthIn(min = 154.dp).testTag("story-record-retry"))
        }
        is StoryRecordCard.Available -> TextCard(
            title = stringResource(R.string.story_card_available_title),
            sub = stringResource(R.string.story_card_available_body), modifier = base.testTag("story-record-available"),
        ) {
            CardButton(stringResource(R.string.story_card_pick), actions.onPick, Modifier.widthIn(min = 154.dp).testTag("story-record-pick"))
        }
        is StoryRecordCard.Old -> TextCard(
            title = stringResource(R.string.story_card_old_title),
            sub = stringResource(R.string.story_card_old_body, words.date(card.lastDay), words.ago(card.daysAgo)),
            modifier = base.testTag("story-record-old"),
        ) {
            CardButton(stringResource(R.string.story_card_history), actions.onHistory, Modifier.weight(1f).testTag("story-record-history"))
            CardButton(stringResource(R.string.story_card_start_run), actions.onStartRun, Modifier.weight(1f).testTag("story-record-start-run"))
        }
        StoryRecordCard.Never -> TextCard(
            title = stringResource(R.string.story_card_never_title),
            sub = stringResource(R.string.story_card_never_body), modifier = base.testTag("story-record-never"),
        ) {
            CardButton(stringResource(R.string.story_card_first_run), actions.onStartRun, Modifier.widthIn(min = 154.dp).testTag("story-record-start-run"))
        }
        StoryRecordCard.Neutral -> TextCard(
            title = stringResource(R.string.story_card_old_title),
            sub = stringResource(R.string.story_card_neutral_body), modifier = base.testTag("story-record-neutral"),
        ) {
            CardButton(stringResource(R.string.story_card_history), actions.onHistory, Modifier.weight(1f).testTag("story-record-history"))
            CardButton(stringResource(R.string.story_card_start_run), actions.onStartRun, Modifier.weight(1f).testTag("story-record-start-run"))
        }
    }
}

/** 글 카드 — 제목 19 · 설명 12.5, 아래 버튼 줄. 버튼 줄 아래 여백은 시안의 6dp(누르는 곳 4dp 포함) */
@Composable
private fun TextCard(
    title: String,
    sub: String,
    modifier: Modifier,
    subColor: Color = composeInk().secondary,
    subStrong: Boolean = false,
    buttons: @Composable RowScope.() -> Unit,
) {
    val ink = composeInk()
    Column(modifier.padding(start = 18.dp, end = 18.dp, top = 18.dp, bottom = 2.dp)) {
        Text(title, color = ink.text, fontSize = 19.sp, fontWeight = FontWeight.SemiBold, lineHeight = 25.sp,
            modifier = Modifier.semantics { heading() })
        Spacer(Modifier.height(6.dp))
        Text(sub, color = subColor, fontSize = if (subStrong) 15.sp else 12.5.sp,
            fontWeight = if (subStrong) FontWeight.SemiBold else FontWeight.Normal, lineHeight = if (subStrong) 20.sp else 18.sp)
        Spacer(Modifier.height(20.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.CenterVertically, content = buttons)
    }
}

@Composable
private fun CardButton(text: String, onClick: () -> Unit, modifier: Modifier = Modifier) {
    val ink = composeInk()
    ComposeChip(text, onClick, modifier, face = ink.inner, ink = ink.accent)
}

/** 코스가 붙은 카드 — 왼쪽 102 × 116 코스 그림, 오른쪽 날짜 · 거리 · 시간 · 버튼 둘 */
@Composable
private fun RouteCard(run: StoryRun, dateLine: String, detail: String, actions: StoryRecordActions, modifier: Modifier) {
    val ink = composeInk()
    Row(modifier.padding(start = 12.dp, end = 14.dp, top = 12.dp, bottom = 2.dp)) {
        StoryRouteThumb(run.route, Modifier.padding(bottom = 10.dp).size(width = 102.dp, height = 116.dp), seed = run.id.toInt(), radius = 14.dp)
        Spacer(Modifier.width(15.dp))
        Column(Modifier.weight(1f)) {
            Spacer(Modifier.height(2.dp))
            Text(dateLine, color = ink.accent, fontSize = 12.sp, fontWeight = FontWeight.SemiBold, maxLines = 1,
                overflow = TextOverflow.Ellipsis, modifier = Modifier.testTag("story-record-date"))
            Row(verticalAlignment = Alignment.Bottom) {
                Text(StoryComposeRules.km(run.distanceMeters), color = ink.text, fontSize = 26.sp, fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.alignByBaseline().testTag("story-record-km"))
                Spacer(Modifier.width(8.dp))
                Text("km", color = ink.secondary, fontSize = 14.sp, modifier = Modifier.alignByBaseline())
            }
            Text(detail, color = ink.secondary, fontSize = 11.5.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Spacer(Modifier.height(4.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                CardButton(stringResource(R.string.story_card_change), actions.onPick, Modifier.weight(1f).testTag("story-record-pick"))
                CardButton(stringResource(R.string.story_card_remove), actions.onRemove, Modifier.weight(1f).testTag("story-record-remove"))
            }
        }
    }
}

/** 코스 그림 — 저장된 좌표를 실제 지도 위에. 좌표가 둘 미만이면 부르지 않는다(가짜 코스를 그리지 않는다) */
@Composable
internal fun StoryRouteThumb(route: List<GeoPoint>, modifier: Modifier, seed: Int, radius: Dp) {
    val ink = composeInk()
    Box(modifier.clip(RoundedCornerShape(radius)).background(ink.skeleton)) {
        if (route.size >= 2) LiveRouteMap(route, Modifier.matchParentSize(), seed = seed)
    }
}

// ── 장소 · 한 줄 이야기 · 본문 ───────────────────────────────────────

@Composable
fun StoryPlaceBlock(
    place: StoryPlace?,
    sources: List<StoryPlaceSource>,
    selected: StoryPlaceSource?,
    onOpen: () -> Unit,
    onSource: (StoryPlaceSource) -> Unit,
) {
    val ink = composeInk()
    Text(stringResource(R.string.story_compose_place_label), color = ink.secondary, fontSize = 14.sp,
        modifier = Modifier.semantics { heading() })
    Spacer(Modifier.height(9.dp))
    Row(
        Modifier.fillMaxWidth().heightIn(min = 52.dp).clip(RoundedCornerShape(15.dp)).background(ink.surface)
            .feedbackClickable(role = Role.Button, onClick = onOpen).padding(start = 15.dp, end = 14.dp, top = 8.dp, bottom = 8.dp)
            .testTag("story-compose-pick"),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(Icons.Outlined.Place, contentDescription = null, tint = ink.accent, modifier = Modifier.size(21.dp))
        Spacer(Modifier.width(13.dp))
        Text(
            place?.name ?: stringResource(R.string.story_compose_place_empty),
            color = ink.text, fontSize = 16.sp, fontWeight = FontWeight.SemiBold, maxLines = 1, overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f).testTag("story-compose-place"),
        )
        Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, contentDescription = null, tint = ink.secondary, modifier = Modifier.size(20.dp))
    }
    Spacer(Modifier.height(7.dp))
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        sources.forEach { source ->
            val on = source == selected
            ComposeChip(
                text = stringResource(
                    when (source) {
                        StoryPlaceSource.COURSE -> R.string.story_place_src_course
                        StoryPlaceSource.NEARBY -> R.string.story_place_src_nearby
                        StoryPlaceSource.RECENT -> R.string.story_place_src_recent
                        StoryPlaceSource.SEARCH -> R.string.story_place_src_search
                    },
                ),
                onClick = { onSource(source) },
                modifier = Modifier.weight(1f).testTag("story-src-${source.name}"),
                face = if (on) ink.selected else ink.surface,
                ink = if (on) ink.selectedText else ink.accent,
            )
        }
    }
}

/** 한 줄 이야기 — 2 × 2 문구 버튼. 불러오는 중이면 빈 자리만(처음인지 오래 쉬었는지 아직 모른다) */
@Composable
fun StoryPhraseBlock(set: StoryPhraseSet?, selected: StoryPhrase?, words: StoryWords, onPhrase: (StoryPhrase) -> Unit) {
    val ink = composeInk()
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Text(stringResource(R.string.story_phrase_title), color = ink.text, fontSize = 15.sp, fontWeight = FontWeight.SemiBold,
            modifier = Modifier.weight(1f).semantics { heading() })
        Text(stringResource(R.string.story_phrase_hint), color = ink.secondary, fontSize = 12.sp)
    }
    Spacer(Modifier.height(10.dp))
    val phrases = set?.let { StoryPhrase.of(it) }
    Column(Modifier.testTag(if (set == null) "story-phrases-loading" else "story-phrases-${set.name}")) {
        (0 until 2).forEach { row ->
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                (0 until 2).forEach { col ->
                    val phrase = phrases?.getOrNull(row * 2 + col)
                    if (phrase == null) {
                        Box(
                            Modifier.weight(1f).padding(vertical = 4.dp).height(40.dp).clip(RoundedCornerShape(12.dp))
                                .background(ink.surface.copy(alpha = 0.6f)),
                        )
                    } else {
                        val on = phrase == selected
                        ComposeChip(
                            text = words.phraseLabel(phrase),
                            onClick = { onPhrase(phrase) },
                            modifier = Modifier.weight(1f).testTag("story-phrase-${phrase.name}"),
                            face = if (on) ink.selected else ink.surface,
                            ink = if (on) ink.selectedText else ink.accent,
                        )
                    }
                }
            }
            if (row == 0) Spacer(Modifier.height(2.dp))
        }
    }
}

/** 본문 — 141dp · 모서리 16. 1,000자에 가까워지면 남은 글자를 보인다 */
@Composable
fun StoryBodyBox(text: String, onChange: (String) -> Unit, enabled: Boolean, large: Boolean) {
    val ink = composeInk()
    Box(
        Modifier.fillMaxWidth().heightIn(min = if (large) 120.dp else 141.dp).clip(RoundedCornerShape(16.dp)).background(ink.surface)
            .padding(start = 18.dp, end = 18.dp, top = 16.dp, bottom = 14.dp),
    ) {
        if (text.isEmpty()) Text(stringResource(R.string.story_write_hint), color = ink.secondary, fontSize = 16.5.sp, lineHeight = 28.sp)
        BasicTextField(
            value = text,
            onValueChange = onChange,
            enabled = enabled,
            textStyle = TextStyle(color = ink.text, fontSize = 16.5.sp, lineHeight = 28.sp),
            cursorBrush = SolidColor(ink.accent),
            modifier = Modifier.fillMaxWidth().testTag("story-compose-text"),
        )
        val count = StoryText.length(text)
        if (count >= StoryText.MAX_CHARS - 100) {
            Text(
                "%,d / %,d".format(count, StoryText.MAX_CHARS),
                color = if (count >= StoryText.MAX_CHARS) ink.warn else ink.secondary, fontSize = 11.sp,
                modifier = Modifier.align(Alignment.BottomEnd).padding(top = 8.dp).testTag("story-compose-count"),
            )
        }
    }
}

// ── 기록 고르기 시트 ──────────────────────────────────────────────

/**
 * 붙일 기록 고르기 — 오늘 · 어제 · 2일 전 · 3일 전으로 걸러 볼 수 있다. 고른 날만 비었으면 그 날의 기록 없음과
 * 전체 기간의 기록 없음을 나눠 말하고 "전체 보기"로 되돌린다.
 */
@Composable
fun StoryRunsSheet(
    records: StoryRecords,
    attachedId: Long?,
    words: StoryWords,
    onPick: (StoryRun) -> Unit,
    onRetry: () -> Unit,
    onHistory: () -> Unit,
    onDismiss: () -> Unit,
) {
    val ink = composeInk()
    var day by rememberSaveable { mutableIntStateOf(-1) }
    StorySheet(stringResource(R.string.story_runs_title), onDismiss, container = ink.sheet, modifier = Modifier.testTag("story-runs-sheet")) {
        Text(stringResource(R.string.story_runs_body), color = ink.secondary, fontSize = 13.sp, lineHeight = 19.sp)
        Spacer(Modifier.height(12.dp))
        Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            (-1..StoryRunRules.WINDOW_DAYS).forEach { option ->
                val on = option == day
                ComposeChip(
                    text = if (option < 0) stringResource(R.string.story_runs_all) else words.ago(option.toLong()),
                    onClick = { day = option },
                    modifier = Modifier.widthIn(min = 64.dp).testTag("story-runs-day-$option"),
                    face = if (on) ink.selected else ink.surface,
                    ink = if (on) ink.selectedText else ink.accent,
                )
            }
        }
        Spacer(Modifier.height(6.dp))
        when (records) {
            StoryRecords.Loading -> Row(Modifier.fillMaxWidth().padding(vertical = 22.dp), verticalAlignment = Alignment.CenterVertically) {
                CircularProgressIndicator(Modifier.size(18.dp), color = ink.accent, strokeWidth = 2.dp)
                Spacer(Modifier.width(10.dp))
                Text(stringResource(R.string.story_card_loading_title), color = ink.secondary, fontSize = 13.sp)
            }
            StoryRecords.Failed -> Column(Modifier.fillMaxWidth().padding(vertical = 14.dp)) {
                Text(stringResource(R.string.story_card_error_title), color = ink.text, fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
                Spacer(Modifier.height(4.dp))
                Text(stringResource(R.string.story_card_error_body), color = ink.secondary, fontSize = 13.sp)
                Spacer(Modifier.height(10.dp))
                ComposeSheetButton(stringResource(R.string.story_card_reload), onRetry, primary = false, modifier = Modifier.testTag("story-runs-retry"))
            }
            is StoryRecords.Ready -> {
                val today = records.options.today
                val all = records.options.runs
                val shown = if (day < 0) all else all.filter { StoryRunRules.daysAgo(it.day, today) == day.toLong() }
                if (shown.isEmpty()) {
                    Column(Modifier.fillMaxWidth().padding(vertical = 14.dp).testTag(if (all.isEmpty()) "story-runs-empty" else "story-runs-empty-day")) {
                        Text(
                            stringResource(if (all.isEmpty()) R.string.story_runs_empty else R.string.story_runs_empty_day),
                            color = ink.text, fontSize = 15.sp, fontWeight = FontWeight.SemiBold,
                        )
                        Spacer(Modifier.height(10.dp))
                        if (all.isEmpty()) {
                            ComposeSheetButton(stringResource(R.string.story_card_history), onHistory, primary = false)
                        } else {
                            ComposeSheetButton(stringResource(R.string.story_runs_show_all), { day = -1 }, primary = false,
                                modifier = Modifier.testTag("story-runs-show-all"))
                        }
                    }
                } else {
                    Column(Modifier.fillMaxWidth().heightIn(max = 380.dp).verticalScroll(rememberScrollState())) {
                        shown.forEach { run ->
                            StoryRunRow(run, words, StoryRunRules.daysAgo(run.day, today), attached = run.id == attachedId) { onPick(run) }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun StoryRunRow(run: StoryRun, words: StoryWords, days: Long, attached: Boolean, onClick: () -> Unit) {
    val ink = composeInk()
    Row(
        Modifier.fillMaxWidth().heightIn(min = 72.dp).clip(RoundedCornerShape(14.dp))
            .feedbackClickable(role = Role.Button, onClick = onClick).padding(vertical = 8.dp).testTag("story-run-${run.id}"),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (run.hasRoute) {
            StoryRouteThumb(run.route, Modifier.size(56.dp), seed = run.id.toInt(), radius = 12.dp)
        } else {
            Box(Modifier.size(56.dp).clip(RoundedCornerShape(12.dp)).background(ink.surface), contentAlignment = Alignment.Center) {
                Icon(Icons.AutoMirrored.Outlined.DirectionsRun, contentDescription = stringResource(R.string.story_runs_no_route), tint = ink.secondary,
                    modifier = Modifier.size(22.dp))
            }
        }
        Spacer(Modifier.width(14.dp))
        Column(Modifier.weight(1f)) {
            Text(words.cardDate(run, days), color = ink.accent, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
            Text("${StoryComposeRules.km(run.distanceMeters)}km", color = ink.text, fontSize = 17.sp, fontWeight = FontWeight.SemiBold)
            Text(
                listOfNotNull(words.detail(run), if (run.hasRoute) null else stringResource(R.string.story_runs_no_route)).joinToString(" · "),
                color = ink.secondary, fontSize = 12.sp, maxLines = 1, overflow = TextOverflow.Ellipsis,
            )
        }
        if (attached) Icon(Icons.Filled.Check, contentDescription = null, tint = ink.accent, modifier = Modifier.size(20.dp))
    }
}

// ── 위치 없이 장소 고르기 ────────────────────────────────────────────

/** 현재 위치를 쓸 수 없을 때(권한 거부 · 위치 꺼짐) — 이름으로 검색 · 지도에서 고르기 · 위치 설정 열기 */
@Composable
fun StoryNoLocationSheet(onSearch: () -> Unit, onMap: () -> Unit, onSettings: () -> Unit, onDismiss: () -> Unit) {
    val ink = composeInk()
    StorySheet(stringResource(R.string.story_noloc_title), onDismiss, container = ink.sheet, modifier = Modifier.testTag("story-noloc-sheet")) {
        Spacer(Modifier.height(10.dp))
        Text(stringResource(R.string.story_noloc_head), color = ink.text, fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
        Spacer(Modifier.height(10.dp))
        Text(stringResource(R.string.story_noloc_body), color = ink.secondary, fontSize = 13.sp)
        Spacer(Modifier.height(22.dp))
        ComposeSheetButton(stringResource(R.string.story_noloc_search), onSearch, primary = true, modifier = Modifier.testTag("story-noloc-search"))
        Spacer(Modifier.height(12.dp))
        ComposeSheetButton(stringResource(R.string.story_noloc_map), onMap, primary = false, modifier = Modifier.testTag("story-noloc-map"))
        Spacer(Modifier.height(12.dp))
        ComposeSheetButton(stringResource(R.string.story_noloc_settings), onSettings, primary = false, modifier = Modifier.testTag("story-noloc-settings"))
        Spacer(Modifier.height(8.dp))
    }
}
