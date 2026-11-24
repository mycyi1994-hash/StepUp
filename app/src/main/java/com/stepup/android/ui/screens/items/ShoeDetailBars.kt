package com.stepup.android.ui.screens.items

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.graphics.drawscope.clipRect
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.stepup.android.R
import com.stepup.android.domain.ShoeRules
import com.stepup.android.domain.Sneaker
import com.stepup.android.domain.wornPercent
import com.stepup.android.ui.components.PlainBar
import com.stepup.android.ui.components.PlainDivider
import com.stepup.android.ui.components.PlainListCard
import com.stepup.android.ui.components.runTextStyle
import com.stepup.android.ui.components.runTone
import com.stepup.android.ui.experience.feedbackClickable
import com.stepup.android.ui.theme.StepUpNumbers

/*
 * 신발 상세의 능력치 네 칸 — 카툰 입체형(2026-09-30 사용자 확정안 1번, docs/redesign/shoe-detail-cartoon-2026-09-30).
 * 스틸 블루 칸 하나에 "항목명 · 입체 막대 · 값" 한 줄. 칸 배경은 평평하고 입체감은 가운데 막대에만 둔다.
 * 막대는 그림을 늘이지 않고 경로로 그린다(길이가 바뀌어도 테두리가 찌그러지지 않게). 움직임 · 반짝임은 없다.
 */

/** 막대 채움 한 벌 — 정면 · 밝은 윗면 · 어두운 아랫면. 신발 등급과 상관없이 항목마다 같은 색이다 */
@Immutable
class CartoonFill(val face: Color, val top: Color, val bottom: Color)

object CartoonColors {
    /** 네 칸의 파란 바탕(파란 톤 v4 — 기본 파란 표면 #0B2B50 보다 한 단계 밝게, 남색 바닥과 갈리게) */
    val Cell = Color(0xFF0E2E57)
    val Text = Color(0xFFF5F8FF)
    val Number = Color(0xFFAAC3EA)
    val Ink = Color(0xFF020C1A)
    val Frame = Color(0xFF1B4FB0)
    val FrameTop = Color(0xFF4F8DF2)
    val FrameBottom = Color(0xFF0E2C6A)
    val Track = Color(0xFF061830)

    val Level = CartoonFill(Color(0xFFFFBD35), Color(0xFFFFE586), Color(0xFFC77A0E))
    val Efficiency = CartoonFill(Color(0xFF2E99FF), Color(0xFF80CBFF), Color(0xFF1254B0))
    val Comfort = CartoonFill(Color(0xFFA779F3), Color(0xFFD0B5FF), Color(0xFF6630AF))
    val Durability = CartoonFill(Color(0xFF27DCC5), Color(0xFF93FFEC), Color(0xFF0B927C))

    /** 강화 화면의 효율 — 강화 전달본의 청록 #48D9FA */
    val UpgradeEfficiency = CartoonFill(Color(0xFF48D9FA), Color(0xFFB0F1FF), Color(0xFF1287B0))
}

/*
 * 2026-11-24 재디자인(docs/redesign/plain-2026-11-24 — 05 신발 상세 · 14 내 신발 탭).
 * 능력치는 흰 카드(panel) 한 장에 줄로 — 이름 · 값 · 가는 막대(효율 eff · 착화감 com · 내구도 dur). 줄을 누르면 그 줄 아래에
 * 한 줄 설명이 펼쳐진다(ⓘ 대신). 내구도가 줄었으면 내구도는 따로 큰 칸([ShoeDurabilityCard]) — 남은 분 · 막대 · 두 줄 설명.
 * 위의 카툰 입체 막대([CartoonStatBar] · [CartoonColors])는 강화 화면이 그대로 쓴다.
 */

/** 내구도 칸의 숫자 · 막대 · 설명 두 줄 — 신발 탭과 상세가 같은 값을 쓴다 */
internal class DurabilityView(
    /** "13 / 31분"(가득이면 상세 줄은 "31분") · 최대를 모르는 예전 신발은 "92 / 100" */
    val text: String,
    /** [text] 안에서 크게 · 색으로 쓸 앞 숫자("13") */
    val lead: String,
    val fraction: Float,
    val color: Color,
    val full: Boolean,
    val lines: List<String>,
)

/**
 * 남은 분(서버 규칙의 수명 — [wholeMinutes]) · 가득에서의 분 · 다 닳은 뒤 적립 비율([com.stepup.android.domain.StatRule.wornRate]).
 * 색은 가득 dur · 줄었음 gold · 다 닳음 dangerText. 숫자를 지어내지 않는다 — 최대 내구도를 모르는 신발은 설명 줄이 없다.
 */
@Composable
internal fun durabilityView(shoe: Sneaker, rules: ShoeRules): DurabilityView {
    val t = runTone()
    val full = !repairable(shoe)
    val worn = durabilityWornOut(shoe)
    val color = when {
        full -> t.dur
        worn -> t.dangerText
        else -> t.gold
    }
    val fraction = durabilityFraction(shoe)
    if (shoe.server?.hasDurabilityMax != true) {
        val left = formatCount(durabilityLeft(shoe))
        return DurabilityView(formatDurability(shoe), left, fraction, color, full, emptyList())
    }
    val left = wholeMinutes(shoe, durabilityLeft(shoe), rules).toString()
    val max = wholeMinutes(shoe, durabilityMax(shoe), rules).toString()
    val stats = rules.stats
    val lines = when {
        full -> listOf(stringResource(R.string.rd_b_dur_full_1), stringResource(R.string.rd_b_dur_full_2, max))
        worn -> listOf(
            stringResource(R.string.rd_b_dur_worn_1),
            if (stats.earnsWhenWorn) stringResource(R.string.rd_b_dur_worn_2, stats.wornPercent)
            else stringResource(R.string.rd_b_dur_worn_2_none),
        )
        else -> listOf(
            stringResource(R.string.rd_b_dur_left_1, left),
            if (stats.earnsWhenWorn) stringResource(R.string.rd_b_dur_left_2, stats.wornPercent)
            else stringResource(R.string.rd_b_dur_left_2_none),
        )
    }
    return DurabilityView(minutesText("$left / $max"), left, fraction, color, full, lines)
}

/** "13 / 31분" — 앞 숫자만 크게 · 색으로, 나머지는 보조 글자. 한 줄(줄바꿈 없음) */
@Composable
internal fun DurabilityNumber(view: DurabilityView, modifier: Modifier = Modifier, size: TextUnit = 30.sp) {
    val t = runTone()
    val start = view.text.indexOf(view.lead).coerceAtLeast(0)
    val end = (start + view.lead.length).coerceAtMost(view.text.length)
    val text = buildAnnotatedString {
        append(view.text)
        if (view.lead.isNotEmpty() && view.text.startsWith(view.lead, start)) {
            addStyle(
                SpanStyle(fontFamily = StepUpNumbers, fontSize = size, fontWeight = FontWeight.ExtraBold, color = view.color),
                start, end,
            )
        }
    }
    Text(
        text, style = runTextStyle(16.sp, t.label, FontWeight.SemiBold, 1.15f).copy(fontFeatureSettings = "tnum"),
        maxLines = 1, softWrap = false, modifier = modifier,
    )
}

/**
 * 내구도 큰 칸 — "내구도" · 오른쪽 "13 / 31분" → 막대([barHeight]) → 설명 두 줄. [onRepair] 가 있으면 설명 오른쪽에 "수리하기"
 * 알약(밝게 검정 · 어둡게 흰색, 높이 40). [numberTag] · [barTag] 는 기기 검사가 값 · 막대를 찾는 이름이다.
 */
@Composable
internal fun ShoeDurabilityCard(
    view: DurabilityView,
    modifier: Modifier = Modifier,
    barHeight: Dp = 8.dp,
    numberTag: String,
    barTag: String,
    onRepair: (() -> Unit)? = null,
    repairEnabled: Boolean = true,
) {
    val t = runTone()
    val title = stringResource(R.string.sdv_stat_durability)
    Column(
        modifier.fillMaxWidth().clip(RoundedCornerShape(22.dp)).background(t.panel).padding(horizontal = 18.dp, vertical = 18.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                title, style = runTextStyle(17.sp, t.text, FontWeight.Bold), maxLines = 1, overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f),
            )
            Spacer(Modifier.width(12.dp))
            DurabilityNumber(view, Modifier.testTag(numberTag))
        }
        Spacer(Modifier.height(14.dp))
        PlainBar(view.fraction, view.color, Modifier.fillMaxWidth().testTag(barTag), height = barHeight)
        if (view.lines.isNotEmpty() || onRepair != null) {
            Spacer(Modifier.height(14.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    view.lines.forEach { line ->
                        Text(line, style = runTextStyle(14.sp, t.label, FontWeight.Medium, 1.4f))
                    }
                }
                if (onRepair != null) {
                    Spacer(Modifier.width(12.dp))
                    RepairPill(onRepair, repairEnabled)
                }
            }
        }
    }
}

/** 내구도 칸 안의 "수리하기" — 검정(어둡게 흰색) 알약 40dp. 누를 수 없으면 흐리게 */
@Composable
private fun RepairPill(onClick: () -> Unit, enabled: Boolean) {
    val t = runTone()
    val shape = RoundedCornerShape(20.dp)
    Box(
        Modifier.alpha(if (enabled) 1f else 0.4f)
            .heightIn(min = 40.dp).clip(shape).background(t.text, shape)
            .feedbackClickable(enabled = enabled, role = Role.Button, onClick = onClick)
            .testTag("shoe-durability-repair")
            .padding(horizontal = 18.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            stringResource(R.string.care_repair), style = runTextStyle(15.sp, t.panel, FontWeight.Bold, 1.2f),
            maxLines = 1, softWrap = false,
        )
    }
}

/**
 * 상세의 능력치 카드 — 효율 · 착화감(+ [withDurability] 면 내구도 "31분") 줄. 줄 = 이름 · 값 · 가는 막대(3dp, 능력치 색).
 * 줄을 누르면 한 줄 설명(서버 0119 의 뜻이면 새 글)이 펼쳐지고, 다시 누르면 접힌다. 다른 줄을 누르면 앞의 것은 접힌다.
 */
@Composable
fun ShoeStatListCard(shoe: Sneaker, withDurability: Boolean, modifier: Modifier = Modifier) {
    val t = runTone()
    val rules = rememberShoeRules()
    val rows = remember(shoe, rules) { detailStatRows(shoe, rules).filter { it.stat != DetailStat.LEVEL } }
    val durability = durabilityView(shoe, rules)
    var infoOpen by rememberSaveable(shoe.id) { mutableStateOf<DetailStat?>(null) }
    val newMeaning = newStatMeaning(rules)
    val shown = rows.filter { withDurability || it.stat != DetailStat.DURABILITY }
    PlainListCard(modifier) {
        shown.forEachIndexed { index, row ->
            if (index > 0) PlainDivider()
            val stat = row.stat
            val pointStat = when (stat) {
                DetailStat.EFFICIENCY -> PointStat.EFFICIENCY
                DetailStat.COMFORT -> PointStat.COMFORT
                else -> PointStat.DURABILITY
            }
            val value = if (stat == DetailStat.DURABILITY) {
                // 가득이면 "31분"(남은 · 최대가 같다), 최대를 모르는 예전 신발은 "100 / 100"
                if (durability.full && shoe.server?.hasDurabilityMax == true) minutesText(row.end) else durability.text
            } else row.value
            val color = when (stat) {
                DetailStat.EFFICIENCY -> t.eff
                DetailStat.COMFORT -> t.com
                else -> t.dur
            }
            StatLine(
                title = statTitle(stat), value = value, fraction = row.fraction, color = color,
                meaning = stringResource(statMeaningRes(pointStat, newMeaning)),
                open = infoOpen == stat,
                onToggle = { infoOpen = if (infoOpen == stat) null else stat },
                tag = "shoe-cell-" + stat.name.lowercase(),
            )
        }
    }
}

@Composable
private fun statTitle(stat: DetailStat): String = stringResource(
    when (stat) {
        DetailStat.LEVEL -> R.string.sdv_info_level
        DetailStat.EFFICIENCY -> R.string.my_shoes_stat_efficiency
        DetailStat.COMFORT -> R.string.sdv_stat_comfort
        DetailStat.DURABILITY -> R.string.sdv_stat_durability
    },
)

/** 능력치 한 줄 — 이름(15.5 SemiBold) · 값(16 Bold, 숫자 폭 고정) → 막대 3dp. 누르면 설명이 그 아래에 */
@Composable
private fun StatLine(
    title: String,
    value: String,
    fraction: Float,
    color: Color,
    meaning: String,
    open: Boolean,
    onToggle: () -> Unit,
    tag: String,
) {
    val t = runTone()
    val action = stringResource(if (open) R.string.shoe_info_hide_cd else R.string.shoe_info_show_cd, title)
    Column(
        Modifier.fillMaxWidth()
            .feedbackClickable(role = Role.Button, onClick = onToggle)
            .semantics(mergeDescendants = true) { contentDescription = listOfNotNull("$title $value", meaning.takeIf { open }, action).joinToString(". ") }
            .testTag(tag)
            .padding(vertical = 15.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(title, style = runTextStyle(15.5.sp, t.text, FontWeight.SemiBold, 1.3f), maxLines = 1,
                overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f))
            Spacer(Modifier.width(12.dp))
            Text(value, style = runTextStyle(16.sp, t.text, FontWeight.Bold, 1.2f).copy(fontFeatureSettings = "tnum"),
                maxLines = 1, softWrap = false, modifier = Modifier.testTag("$tag-value"))
        }
        Spacer(Modifier.height(9.dp))
        PlainBar(fraction, color, Modifier.fillMaxWidth().testTag("$tag-bar"), height = 3.dp)
        if (open) {
            Spacer(Modifier.height(8.dp))
            Text(meaning, style = runTextStyle(13.5.sp, t.label, FontWeight.Medium, 1.4f),
                modifier = Modifier.semantics { liveRegion = LiveRegionMode.Polite }.testTag("$tag-info"))
        }
    }
}


/**
 * 카툰 입체 막대 하나 — 아래 순서로 겹친다.
 * 1 아래로 내려온 어두운 받침 → 2 짙은 잉크 외곽선 → 3 파란 회색 프레임(밝은 윗면 · 어두운 아랫면) →
 * 4 안으로 들어간 짙은 남색 트랙 → 5 진행률만큼의 채움 → 6 채움의 밝은 윗면 · 정면 · 어두운 아랫면 → 7 왼쪽 위 하이라이트 한 줄.
 * [fraction] 은 프레임을 뺀 안쪽 트랙 폭에 적용한다. 0 이면 채움 · 하이라이트를 그리지 않는다. 읽기 도구는 칸이 읽는다.
 */
@Composable
fun CartoonStatBar(fraction: Float, fill: CartoonFill, modifier: Modifier = Modifier, preview: Float? = null) {
    Canvas(modifier) {
        val now = fraction.coerceIn(0f, 1f).takeUnless { it.isNaN() } ?: 0f
        drawCartoonBar(now, fill, preview?.coerceIn(0f, 1f)?.takeUnless { it.isNaN() || it <= now })
    }
}

/** 모서리를 짧게 잘라낸 사각형 — [cut] 은 모서리 사선의 깊이 */
private fun chamfered(rect: Rect, cut: Float): Path {
    val c = cut.coerceAtMost(minOf(rect.width, rect.height) / 2f).coerceAtLeast(0f)
    return Path().apply {
        moveTo(rect.left + c, rect.top)
        lineTo(rect.right - c, rect.top)
        lineTo(rect.right, rect.top + c)
        lineTo(rect.right, rect.bottom - c)
        lineTo(rect.right - c, rect.bottom)
        lineTo(rect.left + c, rect.bottom)
        lineTo(rect.left, rect.bottom - c)
        lineTo(rect.left, rect.top + c)
        close()
    }
}

private fun Rect.inset(by: Float) = Rect(left + by, top + by, right - by, bottom - by)

private fun DrawScope.drawCartoonBar(fraction: Float, fill: CartoonFill, preview: Float? = null) {
    val drop = 2.5.dp.toPx()
    val ink = 1.75.dp.toPx()
    val body = Rect(0f, 0f, size.width, size.height - drop)
    if (body.width <= ink * 8 || body.height <= ink * 6) return
    val cut = 6.dp.toPx()

    // 1 받침 — 같은 모양을 아래로 내려 어둡게
    drawPath(chamfered(body.translate(Offset(0f, drop)), cut), CartoonColors.Ink.copy(alpha = 0.85f))
    // 2 잉크 외곽선
    drawPath(chamfered(body, cut), CartoonColors.Ink)
    // 3 프레임 — 밝은 윗면, 그 위에 조금 내린 어두운 아랫면, 가운데 프레임 면
    val frame = body.inset(ink)
    val framePath = chamfered(frame, cut - ink * 0.6f)
    val rim = 1.5.dp.toPx()
    drawPath(framePath, CartoonColors.FrameTop)
    clipPath(framePath) {
        drawPath(chamfered(frame.translate(Offset(0f, rim)), cut - ink * 0.6f), CartoonColors.FrameBottom)
    }
    val face = Rect(frame.left + rim, frame.top + rim, frame.right - rim, frame.bottom - rim)
    drawPath(chamfered(face, cut - ink - rim * 0.6f), CartoonColors.Frame)
    // 4 안쪽 트랙 — 잉크 테두리 안의 짙은 남색
    val trackOuter = face.inset(1.5.dp.toPx())
    val trackCut = (cut - ink - rim).coerceAtLeast(2.dp.toPx())
    drawPath(chamfered(trackOuter, trackCut), CartoonColors.Ink)
    val track = trackOuter.inset(1.25.dp.toPx())
    drawPath(chamfered(track, trackCut - 0.5.dp.toPx()), CartoonColors.Track)
    // 5 ~ 7 채움 — 트랙 안쪽 전체 폭이 100%
    val lane = track.inset(1.5.dp.toPx())
    // 강화 미리보기 — 지금 채움 뒤에 다음 값까지의 구간을 옅게(실제 값으로 계산한 길이 그대로, 과장하지 않는다)
    if (preview != null) {
        val previewWidth = lane.width * preview
        if (previewWidth >= 0.5f) {
            val ahead = Rect(lane.left, lane.top, lane.left + previewWidth, lane.bottom)
            val aheadCut = minOf(3.dp.toPx(), previewWidth / 2f, ahead.height / 2f)
            drawPath(chamfered(ahead, aheadCut), fill.top.copy(alpha = 0.55f))
        }
    }
    val width = lane.width * fraction
    if (fraction <= 0f || width < 0.5f) return
    val bar = Rect(lane.left, lane.top, lane.left + width, lane.bottom)
    // 사선 깊이는 채움 길이의 절반 이하 — 아주 짧아도 모양이 뒤집히지 않게
    val barCut = minOf(3.dp.toPx(), width / 2f, bar.height / 2f)
    val barPath = chamfered(bar, barCut)
    clipPath(barPath) {
        drawRect(fill.face, topLeft = bar.topLeft, size = bar.size)
        val topBand = bar.height * 0.30f
        clipRect(bar.left, bar.top, bar.right, bar.top + topBand) { drawRect(fill.top, bar.topLeft, bar.size) }
        val bottomBand = bar.height * 0.28f
        clipRect(bar.left, bar.bottom - bottomBand, bar.right, bar.bottom) { drawRect(fill.bottom, bar.topLeft, bar.size) }
    }
    drawPath(barPath, CartoonColors.Ink, style = Stroke(width = 1.dp.toPx()))
    // 7 하이라이트 — 채움 왼쪽 위의 짧고 둥근 한 줄
    val line = 2.dp.toPx()
    val startX = bar.left + barCut + 3.dp.toPx()
    val endX = minOf(bar.left + width * 0.42f, startX + 22.dp.toPx(), bar.right - barCut - 2.dp.toPx())
    if (endX - startX >= line) {
        val y = bar.top + bar.height * 0.17f + line / 2f
        drawLine(Color.White.copy(alpha = 0.9f), Offset(startX, y), Offset(endX, y), strokeWidth = line, cap = StrokeCap.Round)
    }
}

/**
 * SD08 조회 중 — 능력치 카드의 이름만 두고 막대는 빈 바탕, 값은 "—". 읽지 못한 값을 0 으로 보이지 않는다.
 */
@Composable
fun ShoeStatCellsPlaceholder(modifier: Modifier = Modifier) {
    val t = runTone()
    PlainListCard(modifier) {
        listOf(DetailStat.EFFICIENCY, DetailStat.COMFORT, DetailStat.DURABILITY).forEachIndexed { index, stat ->
            if (index > 0) PlainDivider()
            Column(Modifier.fillMaxWidth().padding(vertical = 15.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(statTitle(stat), style = runTextStyle(15.5.sp, t.label, FontWeight.SemiBold, 1.3f), maxLines = 1,
                        modifier = Modifier.weight(1f))
                    Text("—", style = runTextStyle(16.sp, t.muted, FontWeight.Bold, 1.2f))
                }
                Spacer(Modifier.height(9.dp))
                PlainBar(0f, t.track, Modifier.fillMaxWidth(), height = 3.dp)
            }
        }
    }
}
