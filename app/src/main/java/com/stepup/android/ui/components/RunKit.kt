package com.stepup.android.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Backspace
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.stepup.android.R
import com.stepup.android.ui.experience.FeedbackCue
import com.stepup.android.ui.experience.feedbackClickable
import com.stepup.android.ui.theme.Alert
import com.stepup.android.ui.theme.Carbon
import com.stepup.android.ui.theme.CarbonHigh
import com.stepup.android.ui.theme.Edge
import com.stepup.android.ui.theme.Night
import com.stepup.android.ui.theme.Silver
import com.stepup.android.ui.theme.Snow
import com.stepup.android.ui.theme.StepUpSans
import com.stepup.android.ui.theme.Volt
import com.stepup.android.ui.theme.VoltText

/*
 * 러닝 흐름 부품 — 2026-09-27 디자이너 전달본(design/handoff-2026-09-27)의 컴포넌트.
 * 버튼 56dp · 모서리 16dp, 카드 모서리 16dp, 확인 창 모서리 24dp, 좌우 여백 24dp.
 * 색은 앱 테마 값에 맞춘다(어두운 테마에서 시안 색과 같고, 밝은 테마에서는 뒤집힌다).
 */

object RunKit {
    val Gutter = 24.dp
    val ButtonHeight = 56.dp
    val CardRadius = 16.dp
    val DialogRadius = 24.dp
}

private fun kitText(size: TextUnit, weight: FontWeight = FontWeight.Normal, tracking: Double = -0.2) = TextStyle(
    fontFamily = StepUpSans, fontWeight = weight, fontSize = size, letterSpacing = tracking.sp,
    fontFeatureSettings = "tnum", lineHeight = 1.4.em,
)

enum class KitTone { Primary, Secondary, Ghost, Danger }

/** 시안 버튼 — 흰 주 버튼 · 테두리 보조 · 글자만 · 위험 */
@Composable
fun KitButton(
    label: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    tone: KitTone = KitTone.Primary,
    enabled: Boolean = true,
) {
    val shape = RoundedCornerShape(RunKit.CardRadius)
    val (face, ink) = when (tone) {
        KitTone.Primary -> Snow to Night
        KitTone.Secondary -> Carbon to Snow
        KitTone.Ghost -> Color.Transparent to Snow
        KitTone.Danger -> Alert.copy(alpha = 0.16f) to Alert
    }
    Box(
        modifier.fillMaxWidth().heightIn(min = RunKit.ButtonHeight).clip(shape)
            .background(if (enabled) face else face.copy(alpha = face.alpha * 0.4f), shape)
            .then(if (tone == KitTone.Secondary) Modifier.border(1.dp, Edge, shape) else Modifier)
            .feedbackClickable(enabled = enabled, onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 12.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(label, style = kitText(18.sp, FontWeight.Medium), color = if (enabled) ink else ink.copy(alpha = 0.45f),
            textAlign = TextAlign.Center)
    }
}

/**
 * 러닝 흐름 화면 틀 — 위에 뒤로 · 작은 이름, 큰 제목 · 한 줄 설명, 가운데 내용, 아래 버튼.
 * 바탕은 오른쪽 위에서 번지는 남색(S2Stage).
 */
@Composable
fun KitScreen(
    title: String,
    onBack: (() -> Unit)?,
    modifier: Modifier = Modifier,
    headline: String? = null,
    subtitle: String? = null,
    centered: Boolean = false,
    scroll: Boolean = true,
    bottom: @Composable ColumnScope.() -> Unit = {},
    content: @Composable ColumnScope.() -> Unit = {},
) {
    Box(modifier.fillMaxSize().background(Night)) {
        S2Stage(Modifier.fillMaxSize())
        Column(Modifier.fillMaxSize().safeDrawingPadding()) {
            KitHeader(title, onBack, Modifier.padding(start = 8.dp, end = RunKit.Gutter))
            Column(
                Modifier.weight(1f).fillMaxWidth()
                    .then(if (scroll) Modifier.verticalScroll(rememberScrollState()) else Modifier)
                    .padding(horizontal = RunKit.Gutter),
                horizontalAlignment = if (centered) Alignment.CenterHorizontally else Alignment.Start,
            ) {
                if (headline != null) {
                    Spacer(Modifier.height(if (centered) 40.dp else 12.dp))
                    Text(
                        headline, style = kitText(28.sp, FontWeight.SemiBold, -0.4), color = Snow,
                        textAlign = if (centered) TextAlign.Center else TextAlign.Start,
                        modifier = Modifier.semantics { heading() },
                    )
                }
                if (subtitle != null) {
                    Spacer(Modifier.height(8.dp))
                    Text(subtitle, style = kitText(16.sp), color = Silver,
                        textAlign = if (centered) TextAlign.Center else TextAlign.Start)
                }
                Spacer(Modifier.height(24.dp))
                content()
                Spacer(Modifier.height(16.dp))
            }
            Column(
                Modifier.fillMaxWidth().padding(horizontal = RunKit.Gutter).padding(bottom = 16.dp).imePadding(),
                verticalArrangement = Arrangement.spacedBy(8.dp),
                content = bottom,
            )
        }
    }
}

/** 위 한 줄 — 뒤로 · 작은 화면 이름 · (오른쪽 행동) */
@Composable
fun KitHeader(
    title: String,
    onBack: (() -> Unit)?,
    modifier: Modifier = Modifier,
    action: (@Composable () -> Unit)? = null,
) {
    Row(
        modifier.fillMaxWidth().height(56.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (onBack != null) {
            Box(
                Modifier.size(48.dp).clip(CircleShape)
                    .feedbackClickable(cue = FeedbackCue.Back, onClick = onBack).testTag("kit-back"),
                contentAlignment = Alignment.Center,
            ) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, stringResource(R.string.cd_back), tint = Snow,
                    modifier = Modifier.size(22.dp))
            }
        } else {
            Spacer(Modifier.size(16.dp))
        }
        Text(title, style = kitText(16.sp), color = Snow, maxLines = 1, modifier = Modifier.weight(1f))
        action?.invoke()
    }
}

/** 시작 메뉴 카드 — 제목 · 설명 · 오른쪽 아이콘. 강조(흰 면) 한 장과 어두운 면. */
@Composable
fun KitActionCard(
    title: String,
    description: String,
    icon: ImageVector,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    primary: Boolean = false,
    enabled: Boolean = true,
    badge: String? = null,
) {
    val shape = RoundedCornerShape(RunKit.CardRadius)
    val face = if (primary) Snow else Carbon
    val ink = if (primary) Night else Snow
    val muted = if (primary) Night.copy(alpha = 0.72f) else Silver
    Row(
        modifier.fillMaxWidth().heightIn(min = 88.dp).clip(shape).background(face, shape)
            .border(1.dp, Edge, shape)
            .feedbackClickable(enabled = enabled, onClick = onClick)
            .padding(horizontal = 20.dp, vertical = 16.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(title, style = kitText(20.sp, FontWeight.Medium), color = if (enabled) ink else ink.copy(alpha = 0.5f))
                if (badge != null) {
                    Text(
                        badge, style = kitText(12.sp), color = Silver,
                        modifier = Modifier.clip(RoundedCornerShape(8.dp)).background(CarbonHigh)
                            .padding(horizontal = 8.dp, vertical = 2.dp),
                    )
                }
            }
            Text(description, style = kitText(14.sp), color = if (enabled) muted else muted.copy(alpha = 0.5f))
        }
        Icon(icon, contentDescription = null, tint = if (primary) Volt else VoltText.copy(alpha = if (enabled) 1f else 0.4f),
            modifier = Modifier.size(24.dp))
    }
}

/** 목표 카드 — 제목 · 설명 · 카드 안의 흰 시작 버튼 */
@Composable
fun KitGoalCard(
    title: String,
    description: String,
    actionLabel: String,
    onAction: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val shape = RoundedCornerShape(RunKit.CardRadius)
    Column(
        modifier.fillMaxWidth().clip(shape).background(Carbon, shape).border(1.dp, Edge, shape).padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        Text(title, style = kitText(20.sp, FontWeight.Medium), color = Snow)
        Text(description, style = kitText(14.sp), color = Silver)
        Spacer(Modifier.height(8.dp))
        KitButton(actionLabel, onAction)
    }
}

/** 선택 카드 — 하나만 고른다. 고르면 파란 테두리 + 체크 */
@Composable
fun KitChoice(
    title: String,
    description: String,
    selected: Boolean,
    onSelect: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val shape = RoundedCornerShape(RunKit.CardRadius)
    Row(
        modifier.fillMaxWidth().heightIn(min = 72.dp).clip(shape).background(Carbon, shape)
            .border(1.dp, if (selected) Volt else Edge, shape)
            .feedbackClickable(role = Role.RadioButton, onClick = onSelect)
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(title, style = kitText(18.sp, FontWeight.Medium), color = Snow)
            Text(description, style = kitText(12.sp), color = Silver)
        }
        if (selected) Icon(Icons.Filled.Check, contentDescription = null, tint = VoltText, modifier = Modifier.size(22.dp))
    }
}

/** 안내 상자 — 파란 제목 한 줄 + 회색 설명 */
@Composable
fun KitNotice(title: String, body: String, modifier: Modifier = Modifier) {
    val shape = RoundedCornerShape(RunKit.CardRadius)
    Column(
        modifier.fillMaxWidth().clip(shape).background(Carbon, shape).padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        Text(title, style = kitText(16.sp), color = VoltText)
        Text(body, style = kitText(14.sp), color = Silver)
    }
}

/** 측정값 — 작은 이름 + 큰 숫자 */
@Composable
fun KitMetric(label: String, value: String, modifier: Modifier = Modifier, valueSize: TextUnit = 28.sp) {
    Column(modifier, verticalArrangement = Arrangement.spacedBy(2.dp)) {
        Text(label, style = kitText(14.sp), color = Silver)
        Text(value, style = kitText(valueSize, FontWeight.Medium, -0.6), color = Snow, maxLines = 1)
    }
}

/** 두 측정값을 반씩 */
@Composable
fun KitMetricRow(left: Pair<String, String>, right: Pair<String, String>, modifier: Modifier = Modifier) {
    Row(modifier.fillMaxWidth()) {
        KitMetric(left.first, left.second, Modifier.weight(1f))
        KitMetric(right.first, right.second, Modifier.weight(1f))
    }
}

/** 가운데 큰 값 — 작은 파란 이름 · 큰 숫자 · 아래 한 줄 */
@Composable
fun KitHero(label: String, value: String, modifier: Modifier = Modifier, caption: String? = null, dim: Boolean = false) {
    Column(modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
        Text(label, style = kitText(14.sp), color = VoltText)
        Spacer(Modifier.height(4.dp))
        S2Number(value, fontSize = 64.sp, textAlign = TextAlign.Center, color = if (dim) Silver else Snow,
            modifier = Modifier.testTag("run-hero-value"))
        if (caption != null) {
            Spacer(Modifier.height(4.dp))
            Text(caption, style = kitText(14.sp), color = Silver, textAlign = TextAlign.Center)
        }
    }
}

/** 진행 막대 — 6dp, 파란 채움 */
@Composable
fun KitProgress(fraction: Float, modifier: Modifier = Modifier) {
    val shape = RoundedCornerShape(4.dp)
    Box(modifier.fillMaxWidth().height(6.dp).clip(shape).background(Edge, shape)) {
        Box(Modifier.fillMaxWidth(fraction.coerceIn(0f, 1f)).height(6.dp).clip(shape).background(Volt, shape))
    }
}

/** 기다리는 중 — 작은 파란 글 + 세 점 */
@Composable
fun KitWaiting(label: String, modifier: Modifier = Modifier) {
    var dot by remember { mutableIntStateOf(0) }
    LaunchedEffect(Unit) {
        while (true) { kotlinx.coroutines.delay(420); dot = (dot + 1) % 3 }
    }
    Column(modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
        Text(label, style = kitText(14.sp), color = VoltText)
        Spacer(Modifier.height(20.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            repeat(3) { i ->
                Box(Modifier.size(8.dp).clip(CircleShape).background(if (i == dot) Snow else Snow.copy(alpha = 0.45f)))
            }
        }
    }
}

/** 완료 표시 — 작은 체크 */
@Composable
fun KitDoneMark(modifier: Modifier = Modifier) {
    Icon(Icons.Filled.Check, contentDescription = null, tint = Silver, modifier = modifier.size(22.dp))
}

/**
 * 확인 창 — 제목 · 설명 · 세로 버튼들. 바깥을 누르거나 뒤로 가면 [onDismiss](취소)만 한다.
 * 파괴적인 행동은 버튼으로만.
 */
@Composable
fun KitDialog(
    title: String,
    body: String,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
    actions: @Composable ColumnScope.() -> Unit,
) {
    Dialog(onDismissRequest = onDismiss, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        Box(Modifier.fillMaxWidth().padding(RunKit.Gutter), contentAlignment = Alignment.Center) {
            Surface(
                modifier.widthIn(max = 480.dp).fillMaxWidth(),
                shape = RoundedCornerShape(RunKit.DialogRadius), color = CarbonHigh, border = BorderStroke(1.dp, Edge),
            ) {
                Column(Modifier.padding(24.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(title, style = kitText(20.sp, FontWeight.Medium), color = Snow,
                        modifier = Modifier.semantics { heading() })
                    Text(body, style = kitText(16.sp), color = Silver)
                    Spacer(Modifier.height(16.dp))
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp), content = actions)
                }
            }
        }
    }
}

/** 숫자 입력칸 — 이름 · 값 · 단위, 고른 칸은 파란 테두리, 오류는 빨간 테두리 + 아래 한 줄 */
@Composable
fun KitNumberField(
    label: String,
    value: String,
    unit: String,
    focused: Boolean,
    onFocus: () -> Unit,
    modifier: Modifier = Modifier,
    error: String? = null,
) {
    val shape = RoundedCornerShape(RunKit.CardRadius)
    val line = when {
        error != null -> Alert
        focused -> Volt
        else -> Edge
    }
    Column(
        modifier.clip(shape).background(Carbon, shape).border(1.dp, line, shape)
            .feedbackClickable(onClick = onFocus).padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Text(label, style = kitText(16.sp), color = Snow)
        Row(verticalAlignment = Alignment.Bottom) {
            Text(value.ifEmpty { "—" }, style = kitText(28.sp, FontWeight.Medium, -0.6), color = Snow,
                modifier = Modifier.weight(1f), maxLines = 1)
            Text(unit, style = kitText(14.sp), color = Silver)
        }
        if (error != null) Text(error, style = kitText(12.sp), color = Alert)
    }
}

/** 숫자 키패드 — 1~9, 소수점, 0, 지우기 */
@Composable
fun KitKeypad(onKey: (Char) -> Unit, onDelete: () -> Unit, modifier: Modifier = Modifier, deleteLabel: String) {
    val rows = listOf("123", "456", "789", ".0<")
    Column(modifier.fillMaxWidth().background(Carbon).padding(8.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
        rows.forEach { row ->
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                row.forEach { key ->
                    Box(
                        Modifier.weight(1f).height(48.dp).clip(RoundedCornerShape(8.dp)).background(CarbonHigh)
                            .feedbackClickable { if (key == '<') onDelete() else onKey(key) }
                            .testTag("keypad-$key"),
                        contentAlignment = Alignment.Center,
                    ) {
                        if (key == '<') {
                            Icon(Icons.AutoMirrored.Filled.Backspace, deleteLabel,
                                tint = Snow, modifier = Modifier.size(22.dp))
                        } else {
                            Text(key.toString(), style = kitText(22.sp, FontWeight.Medium), color = Snow)
                        }
                    }
                }
            }
        }
    }
}

/** 칸 사이 기본 간격 */
val KitGap: Dp = 12.dp
