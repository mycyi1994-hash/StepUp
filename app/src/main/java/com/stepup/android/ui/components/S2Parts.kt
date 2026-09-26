package com.stepup.android.ui.components

import androidx.annotation.DrawableRes
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import com.stepup.android.R
import com.stepup.android.ui.experience.feedbackClickable
import com.stepup.android.ui.theme.Night
import com.stepup.android.ui.theme.Silver
import com.stepup.android.ui.theme.Snow
import com.stepup.android.ui.theme.StepUpColors
import com.stepup.android.ui.theme.StepUpSans
import com.stepup.android.ui.theme.Volt
import com.stepup.android.ui.theme.VoltText

// ─────────────────────────────────────────────────────────────
// S2 무대 — docs/redesign/s2. 가운데 모인 글, 아치 풍경, 흰 원형 주 행동.
// 글자 · 숫자 · 버튼은 모두 여기서 그린다. 그림에 글자를 얹어 붙이지 않는다.
// ─────────────────────────────────────────────────────────────

/** S2 바닥 — 오른쪽 위에서 번지는 남색. 밝은 테마는 기존 밝은 바닥을 쓴다. */
@Composable
fun S2Stage(modifier: Modifier = Modifier) {
    val dark = StepUpColors.dark
    Box(modifier.background(Night)) {
        if (dark) {
            Canvas(Modifier.fillMaxSize()) {
                val center = Offset(size.width, 0f)
                val radius = size.width * 1.15f
                drawRect(
                    Brush.radialGradient(
                        0f to Color(0xFF09203F),
                        0.53f to Color(0xFF060C18),
                        1f to Color.Transparent,
                        center = center,
                        radius = radius,
                    ),
                )
            }
        } else {
            Box(Modifier.fillMaxSize().background(StepUpColors.backdrop))
        }
    }
}

/**
 * 홈 전체 바탕 풍경(2026-09-26 사용자 결정 — 가운데 아치 대신 화면 뒤에 사진을 깐다).
 * 위 · 아래는 바닥색으로 덮어 글자와 버튼 · 하단 탭이 읽히게 한다. 밝은 테마는 밝은 막.
 */
@Composable
fun S2Scenery(setting: RunnerSetting, modifier: Modifier = Modifier) {
    Box(modifier.background(Night)) {
        Image(
            painter = painterResource(s2SceneryRes(setting)),
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = Modifier.fillMaxSize(),
        )
        Box(
            Modifier.fillMaxSize().background(
                Brush.verticalGradient(
                    0f to Night.copy(alpha = 0.82f),
                    0.3f to Night.copy(alpha = 0.45f),
                    0.55f to Night.copy(alpha = 0.40f),
                    0.78f to Night.copy(alpha = 0.78f),
                    1f to Night.copy(alpha = 0.97f),
                ),
            ),
        )
    }
}

/** 제목 위의 짧은 파란 한 줄 — "일일 목표 8,000걸음" */
@Composable
fun S2Kicker(text: String, modifier: Modifier = Modifier, color: Color = VoltText) {
    Text(
        text, modifier = modifier.fillMaxWidth(), color = color, textAlign = TextAlign.Center,
        style = TextStyle(fontFamily = StepUpSans, fontWeight = FontWeight.SemiBold, fontSize = 14.sp, lineHeight = 1.4.em),
    )
}

/** 가운데 큰 제목 — 문장으로 오늘을 말한다 */
@Composable
fun S2Headline(text: String, modifier: Modifier = Modifier) {
    Text(
        text, modifier = modifier.fillMaxWidth(), color = Snow, textAlign = TextAlign.Center,
        style = TextStyle(
            fontFamily = StepUpSans, fontWeight = FontWeight.SemiBold, fontSize = 29.sp,
            lineHeight = 1.24.em, letterSpacing = (-0.025).em,
        ),
    )
}

/** 제목 아래 보조 한 줄 */
@Composable
fun S2Subtitle(text: String, modifier: Modifier = Modifier, color: Color = Silver) {
    Text(
        text, modifier = modifier.fillMaxWidth(), color = color, textAlign = TextAlign.Center,
        style = TextStyle(fontFamily = StepUpSans, fontWeight = FontWeight.Normal, fontSize = 13.sp, lineHeight = 1.5.em),
    )
}

/**
 * S2 의 가벼운 큰 숫자 — 타이머 · 잔액. 가는 글꼴로 크게, 폭이 모자라면 줄인다.
 * 큰 글씨 설정은 따르되 한 줄을 넘기지 않는다.
 */
@Composable
fun S2Number(
    text: String,
    fontSize: TextUnit,
    modifier: Modifier = Modifier,
    color: Color = Snow,
    textAlign: TextAlign = TextAlign.Center,
) {
    val measurer = rememberTextMeasurer()
    val style = TextStyle(
        fontFamily = StepUpSans, fontWeight = FontWeight.Normal, fontSize = fontSize,
        letterSpacing = (-0.035).em, fontFeatureSettings = "tnum", lineHeight = 1.1.em,
    )
    BoxWithConstraints(modifier.fillMaxWidth()) {
        val available = constraints.maxWidth
        val fitted = remember(text, style, available) {
            val preferred = fontSize.value.coerceAtLeast(1f)
            fun width(size: Float) = measurer.measure(text, style.copy(fontSize = size.sp), softWrap = false).size.width
            if (width(preferred) <= available) preferred.sp else {
                var lower = 1f
                var upper = preferred
                repeat(8) {
                    val mid = (lower + upper) / 2f
                    if (width(mid) <= available) lower = mid else upper = mid
                }
                lower.sp
            }
        }
        Text(text, Modifier.fillMaxWidth(), style = style.copy(fontSize = fitted), color = color,
            textAlign = textAlign, maxLines = 1, softWrap = false)
    }
}

/** S2 풍경 그림. 실시간 날씨가 아니라 사용자가 고르는 장식이다. */
@DrawableRes
fun s2SceneryRes(setting: RunnerSetting): Int = when (setting) {
    RunnerSetting.HomeBlueNight, RunnerSetting.Night -> R.drawable.s2_bg_city
    RunnerSetting.HomeDawn, RunnerSetting.Sunset, RunnerSetting.RunSunset -> R.drawable.s2_bg_dusk
    RunnerSetting.HomeHarbor -> R.drawable.s2_bg_harbor
    RunnerSetting.HomeDay -> R.drawable.s2_bg_day
    RunnerSetting.HomeNight, RunnerSetting.RunNight -> R.drawable.s2_bg_night
    RunnerSetting.HomeRain -> R.drawable.s2_bg_rain
    RunnerSetting.Wardrobe -> R.drawable.s2_bg_harbor
}

/** 아치 창 — 위가 반원인 틀. 풍경을 담거나([image]) 파란 선만 두른 빈 틀로 쓴다. */
@Composable
fun S2Arch(
    modifier: Modifier = Modifier,
    @DrawableRes image: Int? = null,
    content: @Composable () -> Unit = {},
) {
    val shape = RoundedCornerShape(topStartPercent = 50, topEndPercent = 50)
    val line = Volt.copy(alpha = if (StepUpColors.dark) 0.55f else 0.35f)
    Box(
        modifier
            .clip(shape)
            .then(
                if (image == null) {
                    Modifier
                        .background(Brush.linearGradient(listOf(StepUpColors.carbon, Night)))
                        .border(1.dp, line, shape)
                } else Modifier.background(StepUpColors.carbon),
            ),
        contentAlignment = Alignment.Center,
    ) {
        if (image != null) {
            Image(
                painterResource(image), contentDescription = null,
                contentScale = ContentScale.Crop, modifier = Modifier.fillMaxSize(),
            )
            // 아래로 바닥색에 녹아든다
            Box(Modifier.fillMaxSize().background(Brush.verticalGradient(
                0f to Color.Transparent, 0.69f to Color.Transparent, 1f to Night,
            )))
        }
        content()
    }
}

/** S2 원형 행동 — 흰 원 + 아래 이름. 원과 이름이 한 버튼이다(터치 영역 48dp 이상). */
@Composable
fun S2RoundAction(
    icon: ImageVector,
    label: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    primary: Boolean = true,
    enabled: Boolean = true,
    circle: Dp = if (primary) 60.dp else 48.dp,
    /** 키보드가 떠 있을 때처럼 세로 자리가 모자라면 원 옆에 이름을 둔 낮은 한 줄로 */
    compact: Boolean = false,
) {
    val dark = StepUpColors.dark
    val face = when {
        !enabled -> StepUpColors.carbonHigh
        primary -> if (dark) Color(0xFFF3F5FF) else StepUpColors.snow
        else -> if (dark) Color(0xFF202735) else StepUpColors.carbonHigh
    }
    val ink = when {
        !enabled -> StepUpColors.slate
        primary -> if (dark) Color(0xFF070B12) else Color.White
        else -> if (dark) Color(0xFFD3D8E4) else StepUpColors.snow
    }
    val labelText: @Composable () -> Unit = {
        Text(
            label, color = if (enabled) Snow else StepUpColors.slate, textAlign = TextAlign.Center,
            style = TextStyle(fontFamily = StepUpSans, fontWeight = FontWeight.Medium, fontSize = 13.sp, lineHeight = 1.35.em),
        )
    }
    val base = modifier
        .widthIn(min = 88.dp)
        .clip(RoundedCornerShape(16.dp))
        .feedbackClickable(enabled = enabled, role = Role.Button, onClick = onClick)
    if (compact) {
        Row(
            base.heightIn(min = 48.dp).padding(horizontal = 6.dp, vertical = 2.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Box(Modifier.size(40.dp).background(face, CircleShape), contentAlignment = Alignment.Center) {
                Icon(icon, contentDescription = null, tint = ink, modifier = Modifier.size(20.dp))
            }
            labelText()
        }
        return
    }
    Column(
        base.padding(horizontal = 6.dp, vertical = 4.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Box(Modifier.size(circle).background(face, CircleShape), contentAlignment = Alignment.Center) {
            Icon(icon, contentDescription = null, tint = ink, modifier = Modifier.size(if (primary) 26.dp else 22.dp))
        }
        labelText()
    }
}

/** 원형 행동 양옆의 작은 정보 · 링크 — "보유 / 83 SUP", "코스 보기" */
@Composable
fun S2SideInfo(
    label: String,
    modifier: Modifier = Modifier,
    value: String? = null,
    end: Boolean = false,
    onClick: (() -> Unit)? = null,
) {
    Column(
        modifier
            .heightIn(min = 48.dp)
            .clip(RoundedCornerShape(10.dp))
            .then(if (onClick != null) Modifier.feedbackClickable(role = Role.Button, onClick = onClick) else Modifier)
            .padding(vertical = 6.dp, horizontal = 4.dp),
        horizontalAlignment = if (end) Alignment.End else Alignment.Start,
        verticalArrangement = Arrangement.Center,
    ) {
        Text(
            label, color = Silver, textAlign = if (end) TextAlign.End else TextAlign.Start,
            style = TextStyle(fontFamily = StepUpSans, fontSize = 13.sp, lineHeight = 1.4.em),
        )
        if (value != null) {
            Text(
                value, color = Snow, textAlign = if (end) TextAlign.End else TextAlign.Start,
                style = TextStyle(fontFamily = StepUpSans, fontWeight = FontWeight.SemiBold, fontSize = 16.sp,
                    lineHeight = 1.35.em, fontFeatureSettings = "tnum"),
            )
        }
    }
}

/** S2 주 행동 줄 — 왼쪽 정보, 가운데 원형 행동, 오른쪽 링크 */
@Composable
fun S2ActionRow(
    modifier: Modifier = Modifier,
    start: @Composable () -> Unit = {},
    end: @Composable () -> Unit = {},
    center: @Composable () -> Unit,
) {
    Row(modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Box(Modifier.weight(1f), contentAlignment = Alignment.CenterStart) { start() }
        center()
        Box(Modifier.weight(1f), contentAlignment = Alignment.CenterEnd) { end() }
    }
}

/** 칸 사이 가는 선을 둔 기록 숫자 줄 — "거리 2.14 km | 페이스 5'58"" */
@Composable
fun S2Stats(items: List<Pair<String, String>>, modifier: Modifier = Modifier, valueSize: TextUnit = 25.sp) {
    Row(modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        items.forEachIndexed { index, (label, value) ->
            Column(Modifier.weight(1f).padding(horizontal = 6.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                Text(label, color = Silver, textAlign = TextAlign.Center,
                    style = TextStyle(fontFamily = StepUpSans, fontSize = 12.sp, lineHeight = 1.4.em))
                S2Number(value, valueSize, Modifier.padding(top = 4.dp))
            }
            if (index < items.lastIndex) {
                Box(Modifier.size(width = 1.dp, height = 40.dp).background(StepUpColors.edge))
            }
        }
    }
}

/** 탭 선택 표시 — 짧은 파란 선 */
@Composable
fun S2TabMark(visible: Boolean, modifier: Modifier = Modifier) {
    Box(
        modifier.size(width = 17.dp, height = 3.dp)
            .background(if (visible) Volt else Color.Transparent, RoundedCornerShape(2.dp)),
    )
}

/** 풍경 한 칸 높이 — 작은 화면 · 큰 글씨에서 풍경부터 줄인다 */
fun s2ArchHeight(screenHeightDp: Int, largeText: Boolean): Dp = when {
    largeText -> 180.dp
    screenHeightDp < 700 -> 170.dp
    screenHeightDp < 800 -> 214.dp
    else -> 262.dp
}

/** S2 글자 탭 — "근처 번개 · 내 크루 · 지난 모임". 고른 탭은 밝은 글자와 짧은 파란 선. */
@Composable
fun S2TextTab(label: String, selected: Boolean, onClick: () -> Unit, modifier: Modifier = Modifier) {
    Column(
        modifier
            .heightIn(min = 48.dp)
            .clip(RoundedCornerShape(8.dp))
            .feedbackClickable(role = Role.Tab, onClick = onClick)
            .semantics { this.selected = selected }
            .padding(horizontal = 4.dp, vertical = 6.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterVertically),
    ) {
        Text(
            label, color = if (selected) Snow else Silver,
            style = TextStyle(fontFamily = StepUpSans, fontSize = 15.sp, lineHeight = 1.3.em,
                fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Medium),
        )
        S2TabMark(selected)
    }
}

/** S2 신발 무대 — 기울인 파란 면 위에 신발 그림. 그림은 기존 신발 자산 그대로다. */
@Composable
fun S2ShoeStage(shoe: com.stepup.android.domain.Sneaker, modifier: Modifier = Modifier, animate: Boolean = false) {
    Box(modifier.aspectRatio(312f / 214f), contentAlignment = Alignment.Center) {
        Box(
            Modifier.fillMaxSize().padding(horizontal = 6.dp, vertical = 8.dp)
                .graphicsLayer { rotationZ = -8f }
                .background(
                    if (StepUpColors.dark) Color(0xFF294B9C) else StepUpColors.carbonHigh,
                    RoundedCornerShape(4.dp),
                ),
        )
        SneakerFrame(
            shoe, Modifier.fillMaxWidth(0.84f).fillMaxHeight(0.86f).graphicsLayer { rotationZ = -7f },
            animate = animate,
        )
    }
}


/**
 * 신발 탭 안의 글자 탭 — 내 신발 · 뽑기.
 *
 * 하단 탭은 넷(러닝 · 신발 · 같이 뛰기 · 내 정보)이고 뽑기는 신발 안쪽에 있다.
 * 두 화면 맨 위에 같은 줄을 두어 어느 쪽에서든 한 번에 오간다.
 */
@Composable
fun S2ShoesSections(drawSelected: Boolean, onShoes: () -> Unit, onDraw: () -> Unit, modifier: Modifier = Modifier) {
    Row(modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(16.dp, Alignment.CenterHorizontally)) {
        S2TextTab(
            androidx.compose.ui.res.stringResource(R.string.shoes_section_mine), !drawSelected,
            onClick = { if (drawSelected) onShoes() },
            modifier = Modifier.testTag("shoes-section-mine"),
        )
        S2TextTab(
            androidx.compose.ui.res.stringResource(R.string.tab_draw), drawSelected,
            onClick = { if (!drawSelected) onDraw() },
            modifier = Modifier.testTag("shoes-section-draw"),
        )
    }
}
