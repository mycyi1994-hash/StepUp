package com.stepup.android.ui.screens.items

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material3.Icon
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import com.stepup.android.R
import com.stepup.android.domain.Sneaker
import com.stepup.android.domain.tier
import com.stepup.android.ui.components.RunSpec
import com.stepup.android.ui.components.RunSpinner
import com.stepup.android.ui.components.ShoeNameWithBadge
import com.stepup.android.ui.components.SneakerGradeStage
import com.stepup.android.ui.components.runTextStyle
import com.stepup.android.ui.components.runTone
import com.stepup.android.ui.experience.LocalFeedback
import com.stepup.android.ui.experience.feedbackClickable
import com.stepup.android.ui.theme.StepUpSans

/*
 * 신발 상세 · 수리 · 강화(파란 톤 v4) 공통 부품. 색은 러닝 리메이크의 runTone() 을 그대로 쓴다(밝은 테마 포함).
 * 주 버튼은 RunButton(흰 면 · 파란 아랫면), 여기에는 그 옆의 "파란 면" 버튼 · 관리 줄 · 정보 줄 · 상태 칸 · 강화 줄만 둔다.
 */

private fun chamfer(width: Float, height: Float, cut: Float, top: Float = 0f): Path {
    val c = cut.coerceAtMost(minOf(width, height) / 2f)
    val b = top + height
    return Path().apply {
        moveTo(c, top); lineTo(width - c, top); lineTo(width, top + c); lineTo(width, b - c)
        lineTo(width - c, b); lineTo(c, b); lineTo(0f, b - c); lineTo(0f, top + c); close()
    }
}

/**
 * 파란 면 버튼 — 상세 아래의 "강화하기"(주 버튼 옆의 작은 버튼). RunButton 과 같은 높이 · 깎은 모서리 · 아랫면.
 * 밝은 테마에서는 흰 면 · 파란 테두리(주 버튼이 이미 파랗다).
 */
@Composable
fun CareBlueButton(
    label: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
) {
    val t = runTone()
    val feedback = LocalFeedback.current
    val interactions = remember { MutableInteractionSource() }
    val pressed by interactions.collectIsPressedAsState()
    val faceHeight = RunSpec.PrimaryHeight
    val base = RunSpec.PrimaryBase
    val face = when {
        !enabled -> t.disabledFace
        t.dark -> t.cobalt
        else -> t.secondaryFace
    }
    val baseColor = when {
        !enabled -> t.disabledBase
        t.dark -> Color(0xFF0334B8)
        else -> t.secondaryEdge
    }
    val ink = when {
        !enabled -> t.disabledInk
        t.dark -> Color.White
        else -> t.cobaltText
    }
    val sink = if (pressed && enabled) base * 0.6f else 0.dp
    Box(
        modifier.heightIn(min = faceHeight + base)
            .clickable(interactionSource = interactions, indication = null, enabled = enabled, role = Role.Button) {
                feedback?.play(com.stepup.android.ui.experience.FeedbackCue.Tap)
                onClick()
            },
    ) {
        Canvas(Modifier.matchParentSize()) {
            val faceH = size.height - base.toPx()
            val cut = (faceHeight * 0.3f).toPx()
            drawPath(chamfer(size.width, faceH, cut, top = base.toPx()), baseColor)
            val path = chamfer(size.width, faceH, cut, top = sink.toPx())
            drawPath(path, face)
            if (!t.dark && enabled) {
                drawPath(path, t.secondaryEdge, style = androidx.compose.ui.graphics.drawscope.Stroke(1.5.dp.toPx()))
            }
        }
        Box(
            Modifier.fillMaxWidth().height(faceHeight).offset(y = sink).padding(horizontal = 12.dp),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                label, maxLines = 2, overflow = TextOverflow.Ellipsis, textAlign = TextAlign.Center,
                style = TextStyle(fontFamily = StepUpSans, fontWeight = FontWeight.ExtraBold, fontSize = 17.sp,
                    letterSpacing = (-0.02).em, color = ink, lineHeight = 1.15.em),
            )
        }
    }
}

/** 시트 제목 — 왼쪽 큰 제목 · 아래 작은 줄(신발 이름 · No. 번호). 오른쪽 위 닫기는 RunSheet 가 둔다 */
@Composable
fun CareSheetTitle(title: String, subtitle: String?, modifier: Modifier = Modifier) {
    val t = runTone()
    Column(modifier.fillMaxWidth().padding(end = 40.dp)) {
        Text(
            title,
            style = TextStyle(fontFamily = StepUpSans, fontWeight = FontWeight.ExtraBold, fontSize = 25.sp,
                lineHeight = 1.25.em, letterSpacing = (-0.02).em, color = t.text),
            modifier = Modifier.semantics { heading() },
        )
        if (subtitle != null) {
            Spacer(Modifier.height(4.dp))
            Text(subtitle, style = runTextStyle(15.sp, t.label, FontWeight.Medium))
        }
    }
}

/** 시트 · 상세의 "이름 · No. 0007" */
@Composable
fun Sneaker.nameAndNumber(): String = shoeName() + " · " + formatShoeNumber(mintNumber)

/** 관리 시트의 한 줄 — 둥근 파란 칸, 제목 · 설명 · 오른쪽 화살표. 누를 수 없으면 흐리게, 까닭은 설명으로 */
@Composable
fun CareMenuRow(title: String, description: String?, enabled: Boolean, onClick: () -> Unit, tag: String) {
    val t = runTone()
    val shape = RoundedCornerShape(14.dp)
    Row(
        Modifier.fillMaxWidth().heightIn(min = 60.dp).clip(shape)
            .background(if (t.dark) Color(0xFF0E3268) else t.panel, shape)
            .border(1.dp, if (enabled) t.panelEdge else t.divider, shape)
            .feedbackClickable(enabled = enabled, onClick = onClick)
            .testTag(tag)
            .padding(horizontal = 16.dp, vertical = 10.dp)
            .alpha(if (enabled) 1f else 0.55f),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
            Text(title, style = runTextStyle(17.sp, t.text, FontWeight.Bold, 1.3f))
            if (description != null) Text(description, style = runTextStyle(13.sp, t.label, FontWeight.Medium, 1.35f))
        }
        if (enabled) {
            Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, contentDescription = null, tint = t.text,
                modifier = Modifier.size(24.dp))
        }
    }
}

/** 정보 한 줄 — 이름 왼쪽, 값 오른쪽(필요하면 줄을 바꾼다). [valueContent] 가 있으면 글 대신(등급 배지) */
@Composable
fun CareInfoRow(
    label: String,
    value: String,
    tag: String,
    valueContent: (@Composable () -> Unit)? = null,
) {
    val t = runTone()
    val shape = RoundedCornerShape(10.dp)
    Row(
        Modifier.fillMaxWidth().heightIn(min = 40.dp).clip(shape).background(t.panel, shape)
            .semantics(mergeDescendants = true) { if (valueContent != null) contentDescription = "$label $value" }
            .testTag(tag)
            .padding(horizontal = 14.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(label, style = runTextStyle(14.sp, t.label, FontWeight.Medium), modifier = Modifier.padding(end = 12.dp))
        Box(Modifier.weight(1f), contentAlignment = Alignment.CenterEnd) {
            if (valueContent != null) valueContent()
            else Text(value, style = runTextStyle(14.sp, t.text, FontWeight.SemiBold), textAlign = TextAlign.End)
        }
    }
}

/** 금액 두 줄(보유 · 수리 후 · 이전 잔액 …) — 이름 열과 값 열 */
@Composable
fun CareAmountLine(label: String, value: String, tag: String, accent: Color? = null) {
    val t = runTone()
    Row(Modifier.fillMaxWidth().testTag(tag), verticalAlignment = Alignment.CenterVertically) {
        Text(label, style = runTextStyle(15.sp, t.label, FontWeight.Medium), modifier = Modifier.widthIn(min = 64.dp))
        Spacer(Modifier.width(10.dp))
        Text(value, style = runTextStyle(16.sp, accent ?: t.text, FontWeight.Bold), modifier = Modifier.testTag("$tag-value"))
    }
}

/**
 * 상세의 조회 실패(SD09) · 없는 신발(SD10) — 빛나는 아이콘 · 제목 · 한 줄 · 흰 주 버튼(+ 글자 보조 행동).
 * 신발 탭의 상태 칸(ShoeStatePanel)은 신발 탭 전달본이 그대로 쓴다 — 이것은 상세 전용이다.
 */
@Composable
fun CareStatePanel(
    icon: ImageVector,
    title: String,
    body: String,
    primary: Pair<String, () -> Unit>,
    modifier: Modifier = Modifier,
    secondary: Pair<String, () -> Unit>? = null,
) {
    val t = runTone()
    Column(modifier.fillMaxWidth().padding(top = 96.dp, bottom = 16.dp), horizontalAlignment = Alignment.CenterHorizontally) {
        Box(Modifier.size(120.dp), contentAlignment = Alignment.Center) {
            Canvas(Modifier.matchParentSize()) {
                drawCircle(Brush.radialGradient(listOf(t.cobalt.copy(alpha = if (t.dark) 0.45f else 0.18f), Color.Transparent)))
            }
            Icon(icon, contentDescription = null, tint = t.cyan, modifier = Modifier.size(64.dp))
        }
        Text(
            title, textAlign = TextAlign.Center,
            style = TextStyle(fontFamily = StepUpSans, fontWeight = FontWeight.ExtraBold, fontSize = 24.sp,
                lineHeight = 1.3.em, letterSpacing = (-0.02).em, color = t.text),
            modifier = Modifier.padding(top = 16.dp).semantics { heading() },
        )
        Text(body, style = runTextStyle(15.sp, t.label, FontWeight.Medium, 1.45f), textAlign = TextAlign.Center,
            modifier = Modifier.padding(top = 10.dp))
        Spacer(Modifier.height(40.dp))
        com.stepup.android.ui.components.RunButton(primary.first, primary.second, Modifier.testTag("shoe-state-primary"))
        if (secondary != null) {
            Spacer(Modifier.height(8.dp))
            com.stepup.android.ui.components.RunTextAction(secondary.first, secondary.second,
                Modifier.testTag("shoe-state-secondary"))
        }
    }
}

/** 시트 안의 작은 도는 표시 + 글("비용 확인 중…") */
@Composable
fun CareBusyLine(text: String, modifier: Modifier = Modifier) {
    val t = runTone()
    Row(modifier, verticalAlignment = Alignment.CenterVertically) {
        RunSpinner(Modifier.size(20.dp))
        Spacer(Modifier.width(10.dp))
        Text(text, style = runTextStyle(15.sp, t.label, FontWeight.Medium))
    }
}

/**
 * 강화 · 기록의 머리 — 왼쪽 등급 무대(실제 신발 · 등급 프레임), 오른쪽 이름 · 등급 배지 · No. 번호 · Lv.
 * 이름이 길면 두 줄까지, 배지와 번호는 함께 남는다.
 */
@Composable
fun CareShoeHeader(
    shoe: Sneaker,
    modifier: Modifier = Modifier,
    stageWidth: Dp = 150.dp,
    showLevel: Boolean = true,
    levelText: String? = null,
) {
    val t = runTone()
    Row(modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        SneakerGradeStage(shoe, Modifier.width(stageWidth))
        Spacer(Modifier.width(14.dp))
        Column(Modifier.weight(1f)) {
            ShoeNameWithBadge(
                name = shoe.shoeName(), tier = shoe.tier,
                style = TextStyle(color = t.text, fontSize = 21.sp, fontWeight = FontWeight.Bold, lineHeight = 1.25.em),
                maxLines = 3,
                suffix = formatShoeNumber(shoe.mintNumber),
                suffixStyle = TextStyle(color = t.label, fontSize = 14.sp, fontWeight = FontWeight.Medium, fontFeatureSettings = "tnum"),
                modifier = Modifier.fillMaxWidth().semantics { heading() }.testTag("care-shoe-name"),
            )
            if (showLevel) {
                Spacer(Modifier.height(6.dp))
                Text(
                    levelText ?: stringResource(R.string.level_chip, shoe.level),
                    style = TextStyle(fontFamily = StepUpSans, fontWeight = FontWeight.Bold, fontSize = 22.sp,
                        color = t.cyan, fontFeatureSettings = "tnum"),
                    modifier = Modifier.testTag("care-shoe-level"),
                )
            }
        }
    }
}

// ── 강화의 세 줄(레벨 · 효율 · 착화감) ──────────────────────────────

private fun upgradeFill(stat: DetailStat): CartoonFill = when (stat) {
    DetailStat.LEVEL -> CartoonColors.Level
    DetailStat.EFFICIENCY -> CartoonColors.UpgradeEfficiency
    DetailStat.COMFORT -> CartoonColors.Comfort
    DetailStat.DURABILITY -> CartoonColors.Durability
}

@Composable
private fun upgradeLabel(stat: DetailStat): String = stringResource(
    when (stat) {
        DetailStat.LEVEL -> R.string.sdv_info_level
        DetailStat.EFFICIENCY -> R.string.my_shoes_stat_efficiency
        DetailStat.COMFORT -> R.string.sdv_stat_comfort
        DetailStat.DURABILITY -> R.string.sdv_stat_durability
    },
)

/**
 * 강화 화면의 독립된 세 줄 — 각 줄이 둥근 파란 칸 하나, "항목명 · 입체 막대 · 지금 → 다음". 세 줄을 다시 감싸는 카드는 없다.
 * [rows] 의 next 가 없으면(최대 · 결과 미확인) 지금 값만, 막대에 증가 구간을 그리지 않는다. 레벨 분모는 실제 상한([maxLevel]).
 */
@Composable
fun UpgradeStatRows(rows: List<UpgradeRow>, maxLevel: Int, modifier: Modifier = Modifier, previewBars: Boolean = true) {
    val labels = rows.map { upgradeLabel(it.stat) }
    val measurer = rememberTextMeasurer()
    val density = LocalDensity.current
    val base = LocalTextStyle.current
    val labelStyle = remember(base) { base.merge(TextStyle(fontSize = 16.sp, fontWeight = FontWeight.SemiBold)) }
    val valueStyle = remember(base) { base.merge(TextStyle(fontSize = 15.sp, fontWeight = FontWeight.SemiBold, fontFeatureSettings = "tnum")) }
    fun valueText(row: UpgradeRow): String =
        if (row.stat == DetailStat.LEVEL) {
            (if (row.next != null) "${row.now} → ${row.next}" else row.now) + " / $maxLevel"
        } else if (row.next != null) "${row.now} → ${row.next}" else row.now
    val texts = rows.map(::valueText)
    val (labelWidth, valueWidth) = remember(labels, texts, labelStyle, valueStyle, density) {
        with(density) {
            (labels.maxOf { measurer.measure(it, labelStyle, softWrap = false).size.width } + 1).toDp() to
                (texts.maxOf { measurer.measure(it, valueStyle, softWrap = false).size.width } + 1).toDp()
        }
    }
    BoxWithConstraints(modifier.fillMaxWidth()) {
        val stacked = maxWidth - 24.dp - 16.dp - labelWidth - valueWidth < 56.dp
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            rows.forEachIndexed { index, row ->
                val accent = when (row.stat) {
                    DetailStat.LEVEL -> CartoonColors.Level.top
                    DetailStat.EFFICIENCY -> CartoonColors.UpgradeEfficiency.face
                    else -> CartoonColors.Comfort.top
                }
                val value = buildAnnotatedString {
                    append(row.now)
                    if (row.next != null) {
                        append(" → ")
                        withStyle(SpanStyle(color = accent, fontWeight = FontWeight.ExtraBold)) { append(row.next) }
                    }
                    if (row.stat == DetailStat.LEVEL) {
                        withStyle(SpanStyle(color = CartoonColors.Number)) { append(" / $maxLevel") }
                    }
                }
                val cell = Modifier.fillMaxWidth().heightIn(min = 52.dp)
                    .background(CartoonColors.Cell, RoundedCornerShape(14.dp))
                    .semantics(mergeDescendants = true) { contentDescription = labels[index] + " " + texts[index] }
                    .testTag("upgrade-row-" + row.stat.name.lowercase())
                    .padding(horizontal = 12.dp, vertical = 6.dp)
                val bar = @Composable { m: Modifier ->
                    CartoonStatBar(row.nowFraction, upgradeFill(row.stat), m.height(28.dp),
                        preview = if (previewBars) row.nextFraction else null)
                }
                if (stacked) {
                    Column(cell, verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(labels[index], style = labelStyle, color = CartoonColors.Text, modifier = Modifier.weight(1f))
                            Text(value, style = valueStyle, color = CartoonColors.Text, modifier = Modifier.testTag("upgrade-value-" + row.stat.name.lowercase()))
                        }
                        bar(Modifier.fillMaxWidth())
                    }
                } else {
                    Row(cell, verticalAlignment = Alignment.CenterVertically) {
                        Text(labels[index], style = labelStyle, color = CartoonColors.Text, maxLines = 1, softWrap = false,
                            modifier = Modifier.width(labelWidth))
                        Spacer(Modifier.width(8.dp))
                        bar(Modifier.weight(1f))
                        Spacer(Modifier.width(8.dp))
                        Text(value, style = valueStyle, color = CartoonColors.Text, maxLines = 1, softWrap = false,
                            textAlign = TextAlign.End,
                            modifier = Modifier.width(valueWidth).testTag("upgrade-value-" + row.stat.name.lowercase()))
                    }
                }
            }
        }
    }
}

/** 시트 안 내구도 한 줄(수리) — "내구도 · 막대(지금 → 가득 미리보기) · 68 → 100" */
@Composable
fun RepairDurabilityRow(now: Double, to: Double?, modifier: Modifier = Modifier) {
    val shown = kotlin.math.floor(now).toInt().coerceAtLeast(0)
    val text = if (to != null) "$shown → ${kotlin.math.floor(to).toInt()}" else "$shown / 100"
    val label = stringResource(R.string.sdv_stat_durability)
    Row(
        modifier.fillMaxWidth().heightIn(min = 52.dp).background(CartoonColors.Cell, RoundedCornerShape(14.dp))
            .semantics(mergeDescendants = true) { contentDescription = "$label $text" }
            .testTag("repair-durability")
            .padding(horizontal = 12.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(label, style = runTextStyle(16.sp, CartoonColors.Text, FontWeight.SemiBold), maxLines = 1, softWrap = false)
        Spacer(Modifier.width(10.dp))
        CartoonStatBar(
            (now / 100.0).toFloat(), CartoonColors.Durability, Modifier.weight(1f).height(28.dp),
            preview = to?.let { (it / 100.0).toFloat() },
        )
        Spacer(Modifier.width(10.dp))
        Text(text, style = runTextStyle(15.sp, CartoonColors.Text, FontWeight.SemiBold).copy(fontFeatureSettings = "tnum"),
            maxLines = 1, softWrap = false, modifier = Modifier.testTag("repair-durability-value"))
    }
}

/** 상세 · 강화의 둥근 알림 칸(작은 원 아이콘 · 글) — 착용 완료 · 첫 착용 안내 등 */
@Composable
fun CareStrip(text: String, modifier: Modifier = Modifier, icon: ImageVector? = null) {
    val t = runTone()
    val shape = RoundedCornerShape(14.dp)
    Row(
        modifier.fillMaxWidth().clip(shape).background(t.panel, shape).border(1.dp, t.panelEdge, shape)
            .padding(horizontal = 14.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (icon != null) {
            Box(Modifier.size(24.dp).clip(CircleShape).background(t.cobalt), contentAlignment = Alignment.Center) {
                Icon(icon, contentDescription = null, tint = Color.White, modifier = Modifier.size(16.dp))
            }
            Spacer(Modifier.width(10.dp))
        }
        Text(text, style = runTextStyle(14.sp, t.text, FontWeight.SemiBold))
    }
}
