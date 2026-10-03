package com.stepup.android.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBackIos
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.PriorityHigh
import androidx.compose.material.icons.outlined.ErrorOutline
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.stepup.android.R
import com.stepup.android.ui.Routes
import com.stepup.android.ui.experience.FeedbackCue
import com.stepup.android.ui.experience.LocalFeedback
import com.stepup.android.ui.experience.LocalMotion
import com.stepup.android.ui.experience.feedbackClickable

/*
 * 내 정보 · 프로필 수정 · 설정(파란 톤 통합 전달본 v4, docs/redesign/blue-v4-2026-10 의 PF · PRO · SET 묶음) 부품.
 *
 * 러닝 리메이크의 남색 · 전기 파랑 한 벌(RunStyle 의 runTone · RunButton)을 그대로 쓰고, 설정에만 있는 모양 —
 * 가운데 제목 머리, 파란 테두리로 묶은 줄 목록, 스위치 · 고르기 · 상태 줄, 안내 칸, 아래에서 올라오는 시트 — 을 더한다.
 * 줄은 최소 높이만 정해 큰 글씨 · 긴 번역에서 늘어나고, 누르는 곳은 모두 48dp 이상이다. 밝은 테마는 runTone 의 밝은 값.
 */

/** 이 묶음의 화면들 — 앱 셸이 바닥을 러닝 화면과 같은 남색으로 깐다 */
object BlueProfileArea {
    val backdropRoutes: Set<String> = setOf(
        Routes.PROFILE_EDIT,
        Routes.SETTINGS,
        Routes.SETTINGS_NOTIFICATIONS,
        Routes.SETTINGS_PRIVACY,
        Routes.SETTINGS_SUPPORT,
        Routes.SETTINGS_CONNECTED,
        Routes.SETTINGS_LANGUAGE,
        Routes.SETTINGS_EXPERIENCE,
        Routes.SETTINGS_THEME,
    )
}

/** 줄 목록의 색 — 어두운 테마는 시안(#0B2B50 면 · #0754FF 계열 테두리), 밝은 테마는 흰 면 · 옅은 파랑 */
@Immutable
class BlueListColors(
    val face: Color,
    val selectedFace: Color,
    val selectedEdge: Color,
    val edge: Color,
    val divider: Color,
    val switchOn: Color,
    val switchOff: Color,
    val skeleton: Color,
    val checkInk: Color,
)

@Composable
fun blueListColors(): BlueListColors {
    val t = runTone()
    return if (t.dark) {
        BlueListColors(
            face = Color(0xFF0A2A50), selectedFace = Color(0xFF0B3C7E), selectedEdge = Color(0xFF2F7BFF),
            edge = Color(0xFF1D5BD6), divider = Color(0xFF173F79),
            switchOn = t.cyan, switchOff = Color(0xFF1E3D6E), skeleton = Color(0xFF214D8A), checkInk = Color(0xFF051A36),
        )
    } else {
        BlueListColors(
            face = Color.White, selectedFace = Color(0xFFEAF2FF), selectedEdge = Color(0xFF3D7CF2),
            edge = Color(0xFF8DB0EE), divider = Color(0xFFD6E2F6),
            switchOn = t.cobalt, switchOff = Color(0xFFC6D3EA), skeleton = Color(0xFFDCE6F6), checkInk = Color.White,
        )
    }
}

/** 테두리를 내용 위에 그린다 — 줄이 면을 채워도 둘레가 가려지지 않는다 */
fun Modifier.blueEdge(color: Color, radius: Dp, width: Dp = 1.dp): Modifier = drawWithContent {
    drawContent()
    val w = width.toPx()
    drawRoundRect(
        color, topLeft = Offset(w / 2f, w / 2f), size = Size(size.width - w, size.height - w),
        cornerRadius = CornerRadius(radius.toPx() - w / 2f), style = Stroke(w),
    )
}

// ── 머리 · 화면 틀 ─────────────────────────────────────────────────

/** 설정 머리 — 왼쪽 뒤로, 가운데 제목, (아래 가운데 한 줄 설명). 로고 · SUP 는 두지 않는다 */
@Composable
fun BluePageHeader(
    title: String,
    onBack: (() -> Unit)?,
    modifier: Modifier = Modifier,
    subtitle: String? = null,
    backEnabled: Boolean = true,
) {
    val t = runTone()
    Column(modifier.fillMaxWidth()) {
        Box(Modifier.fillMaxWidth().heightIn(min = 60.dp), contentAlignment = Alignment.Center) {
            Text(
                title, style = runTextStyle(21.sp, t.text, FontWeight.Bold, 1.25f), textAlign = TextAlign.Center,
                maxLines = 2, overflow = TextOverflow.Ellipsis,
                modifier = Modifier.padding(horizontal = 52.dp).semantics { heading() }.testTag("blue-page-title"),
            )
            if (onBack != null) {
                Box(
                    Modifier.align(Alignment.CenterStart).offset(x = (-8).dp).size(48.dp).clip(CircleShape)
                        .feedbackClickable(enabled = backEnabled, cue = FeedbackCue.Back, onClick = onBack)
                        .testTag("blue-back"),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        Icons.AutoMirrored.Filled.ArrowBackIos, stringResource(R.string.cd_back),
                        tint = if (backEnabled) t.text else t.muted, modifier = Modifier.size(22.dp).offset(x = 4.dp),
                    )
                }
            }
        }
        if (subtitle != null) {
            Text(
                subtitle, style = runTextStyle(15.sp, t.label, FontWeight.Medium), textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp).padding(bottom = 4.dp),
            )
        }
    }
}

/** 설정 상세 틀 — 머리 아래 목록만 넘어간다(하단 탭은 앱 셸의 것) */
@Composable
fun BluePage(
    title: String,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    subtitle: String? = null,
    content: LazyListScope.() -> Unit,
) {
    Column(modifier.fillMaxSize().padding(horizontal = RunSpec.Gutter)) {
        BluePageHeader(title, onBack, subtitle = subtitle)
        LazyColumn(
            Modifier.weight(1f).fillMaxWidth(),
            contentPadding = PaddingValues(top = 8.dp, bottom = 28.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
            content = content,
        )
    }
}

// ── 묶음 · 줄 ──────────────────────────────────────────────────────

/** 묶음 이름 — "러닝과 알림". 제목으로 읽힌다 */
@Composable
fun BlueGroupLabel(text: String, modifier: Modifier = Modifier) {
    val t = runTone()
    Text(
        text, style = runTextStyle(15.sp, t.label, FontWeight.SemiBold),
        modifier = modifier.fillMaxWidth().padding(start = 4.dp, top = 8.dp, bottom = 10.dp).semantics { heading() },
    )
}

/** 줄 묶음 — 둥근 파란 테두리 안에 줄들, 줄 사이에 가는 선 */
@Composable
fun BlueGroup(modifier: Modifier = Modifier, content: @Composable ColumnScope.() -> Unit) {
    val c = blueListColors()
    val shape = RoundedCornerShape(16.dp)
    Column(
        modifier.fillMaxWidth().clip(shape).background(c.divider, shape).blueEdge(c.edge, 16.dp),
        verticalArrangement = Arrangement.spacedBy(1.dp),
        content = content,
    )
}

@Composable
private fun RowTexts(title: String, description: String?, titleColor: Color, modifier: Modifier = Modifier, extra: String? = null) {
    val t = runTone()
    Column(modifier, verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text(title, style = runTextStyle(18.sp, titleColor, FontWeight.SemiBold, 1.3f))
        if (description != null) Text(description, style = runTextStyle(14.sp, t.label, FontWeight.Medium, 1.4f))
        if (extra != null) Text(extra, style = runTextStyle(13.sp, t.label.copy(alpha = 0.85f), FontWeight.Medium, 1.4f))
    }
}

/** 들어가는 줄 — 이름 · (현재 값) · ">" . [plain] 이면 면 없이(묶음 밖 "계정 삭제") */
@Composable
fun BlueNavRow(
    title: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    value: String? = null,
    description: String? = null,
    danger: Boolean = false,
    enabled: Boolean = true,
    valueAccent: Boolean = false,
    plain: Boolean = false,
) {
    val t = runTone()
    val c = blueListColors()
    Row(
        modifier.fillMaxWidth().then(if (plain) Modifier else Modifier.background(c.face))
            .feedbackClickable(enabled = enabled, role = Role.Button, onClick = onClick)
            .heightIn(min = if (description != null) 76.dp else 60.dp)
            .padding(start = if (plain) 4.dp else 20.dp, end = 14.dp, top = 12.dp, bottom = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        RowTexts(title, description, if (danger) t.dangerText else if (enabled) t.text else t.muted, Modifier.weight(1f))
        if (value != null) {
            Text(
                value, style = runTextStyle(15.sp, if (valueAccent) t.cyan else t.label, if (valueAccent) FontWeight.Bold else FontWeight.Medium),
                textAlign = TextAlign.End, modifier = Modifier.widthIn(max = 170.dp),
            )
        }
        Icon(Icons.Filled.ChevronRight, contentDescription = null, tint = if (danger) t.dangerText else t.label, modifier = Modifier.size(26.dp))
    }
}

/** 스위치 줄 — 줄 전체가 한 스위치(이름 · 상태를 한 번에 읽는다) */
@Composable
fun BlueSwitchRow(
    title: String,
    description: String?,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    extra: String? = null,
) {
    val t = runTone()
    val c = blueListColors()
    Row(
        modifier.fillMaxWidth().background(c.face)
            .toggleable(value = checked, enabled = enabled, role = Role.Switch, onValueChange = onCheckedChange)
            .heightIn(min = if (description != null) 80.dp else 60.dp)
            .padding(start = 20.dp, end = 16.dp, top = 14.dp, bottom = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        RowTexts(title, description, t.text, Modifier.weight(1f), extra)
        BlueSwitchFace(checked, enabled)
    }
}

/** 스위치 모양 — 켜짐은 시안 길 · 오른쪽 흰 점, 꺼짐은 남색 길 · 왼쪽 흰 점(점 자리로도 상태를 말한다) */
@Composable
fun BlueSwitchFace(checked: Boolean, enabled: Boolean = true) {
    val c = blueListColors()
    val track = if (checked) c.switchOn else c.switchOff
    Box(
        Modifier.size(width = 54.dp, height = 32.dp).clip(RoundedCornerShape(50))
            .background(if (enabled) track else track.copy(alpha = 0.45f)).padding(3.dp),
    ) {
        Box(
            Modifier.size(26.dp).align(if (checked) Alignment.CenterEnd else Alignment.CenterStart)
                .background(if (enabled) Color.White else Color.White.copy(alpha = 0.6f), CircleShape),
        )
    }
}

/** 하나만 고르는 줄 — 테마 · 언어. 고른 줄은 밝은 파란 면 · 채운 동그라미에 체크 */
@Composable
fun BlueChoiceRow(
    title: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    description: String? = null,
) {
    val t = runTone()
    val c = blueListColors()
    Row(
        modifier.fillMaxWidth()
            .background(if (selected) Brush.horizontalGradient(listOf(c.selectedFace, c.selectedFace.copy(alpha = 0.85f))) else Brush.linearGradient(listOf(c.face, c.face)))
            .then(if (selected) Modifier.border(1.5.dp, c.selectedEdge) else Modifier)
            .selectable(selected = selected, role = Role.RadioButton, onClick = onClick)
            .heightIn(min = if (description != null) 80.dp else 62.dp)
            .padding(start = 20.dp, end = 16.dp, top = 12.dp, bottom = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        RowTexts(title, description, t.text, Modifier.weight(1f))
        Box(
            Modifier.size(28.dp).clip(CircleShape)
                .then(if (selected) Modifier.background(c.switchOn) else Modifier.border(2.dp, t.label.copy(alpha = 0.85f), CircleShape)),
            contentAlignment = Alignment.Center,
        ) {
            if (selected) Icon(Icons.Filled.Check, contentDescription = null, tint = c.checkInk, modifier = Modifier.size(18.dp))
        }
    }
}

/** 상태 줄 — 앱 안의 스위치가 아니다. 상태는 글자로(허용됨 · 정확한 위치 · 허용 안 됨) */
@Composable
fun BlueStatusRow(
    title: String,
    description: String?,
    status: String,
    modifier: Modifier = Modifier,
    strong: Boolean = true,
) {
    val t = runTone()
    val c = blueListColors()
    Row(
        modifier.fillMaxWidth().background(c.face).semantics(mergeDescendants = true) {}
            .heightIn(min = if (description != null) 80.dp else 60.dp)
            .padding(start = 20.dp, end = 20.dp, top = 12.dp, bottom = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        RowTexts(title, description, t.text, Modifier.weight(1f))
        Text(
            status, style = runTextStyle(16.sp, if (strong) t.cyan else t.label, FontWeight.SemiBold),
            textAlign = TextAlign.End, modifier = Modifier.widthIn(max = 160.dp),
        )
    }
}

// ── 안내 ───────────────────────────────────────────────────────────

/** 한 줄 설명 — 화면 위(굵게) · 아래(보조) */
@Composable
fun BlueNote(text: String, modifier: Modifier = Modifier, intro: Boolean = false) {
    val t = runTone()
    Text(
        text,
        style = if (intro) runTextStyle(17.sp, t.label, FontWeight.SemiBold) else runTextStyle(15.sp, t.label, FontWeight.Medium, 1.5f),
        modifier = modifier.fillMaxWidth().padding(horizontal = 4.dp),
    )
}

enum class BlueNoticeAction { Primary, Chip, Text }

/**
 * 안내 칸 — 휴대폰 알림 꺼짐(아래 흰 버튼) · 서버 반영 대기(오른쪽 작은 버튼) · 저장 실패(왼쪽 느낌표).
 * 화면 낭독이 바로 읽는다.
 */
@Composable
fun BlueNotice(
    title: String,
    body: String?,
    modifier: Modifier = Modifier,
    actionLabel: String? = null,
    onAction: () -> Unit = {},
    actionEnabled: Boolean = true,
    actionStyle: BlueNoticeAction = BlueNoticeAction.Chip,
    icon: ImageVector? = null,
    actionTag: String? = null,
) {
    val t = runTone()
    val c = blueListColors()
    val shape = RoundedCornerShape(16.dp)
    Column(
        modifier.fillMaxWidth().clip(shape).background(c.face, shape).border(1.dp, c.edge, shape)
            .padding(horizontal = 20.dp, vertical = 18.dp)
            .semantics(mergeDescendants = actionLabel == null) { liveRegion = LiveRegionMode.Polite },
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            if (icon != null) {
                Icon(icon, contentDescription = null, tint = t.cyan, modifier = Modifier.size(30.dp))
                Spacer(Modifier.width(16.dp))
            }
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(title, style = runTextStyle(if (icon != null) 16.sp else 19.sp, t.text, FontWeight.Bold, 1.3f))
                if (body != null) Text(body, style = runTextStyle(14.sp, t.label, FontWeight.Medium, 1.5f))
            }
            if (actionLabel != null && actionStyle != BlueNoticeAction.Primary) {
                Spacer(Modifier.width(12.dp))
                if (actionStyle == BlueNoticeAction.Chip) {
                    val chip = RoundedCornerShape(12.dp)
                    Box(
                        Modifier.heightIn(min = 48.dp).clip(chip).border(1.5.dp, if (actionEnabled) t.secondaryEdge else t.divider, chip)
                            .feedbackClickable(enabled = actionEnabled, role = Role.Button, onClick = onAction)
                            .padding(horizontal = 14.dp)
                            .then(if (actionTag != null) Modifier.testTag(actionTag) else Modifier),
                        contentAlignment = Alignment.Center,
                    ) {
                        Text(actionLabel, style = runTextStyle(15.sp, if (actionEnabled) t.text else t.muted, FontWeight.SemiBold), maxLines = 1)
                    }
                } else {
                    Box(
                        Modifier.heightIn(min = 48.dp).feedbackClickable(enabled = actionEnabled, role = Role.Button, onClick = onAction)
                            .padding(horizontal = 8.dp)
                            .then(if (actionTag != null) Modifier.testTag(actionTag) else Modifier),
                        contentAlignment = Alignment.Center,
                    ) {
                        Text(actionLabel, style = runTextStyle(16.sp, if (actionEnabled) t.cyan else t.muted, FontWeight.Bold), maxLines = 1)
                    }
                }
            }
        }
        if (actionLabel != null && actionStyle == BlueNoticeAction.Primary) {
            RunButton(
                actionLabel, onAction, enabled = actionEnabled,
                modifier = if (actionTag != null) Modifier.testTag(actionTag) else Modifier,
            )
        }
    }
}

/** 칸 없이 쓰는 실패 안내 — 빨간 제목 · 보조 설명(테마 · 소리 저장 실패) */
@Composable
fun BlueInlineError(title: String, body: String?, modifier: Modifier = Modifier) {
    val t = runTone()
    Column(
        modifier.fillMaxWidth().padding(horizontal = 4.dp).semantics(mergeDescendants = true) { liveRegion = LiveRegionMode.Polite },
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        Text(title, style = runTextStyle(16.sp, t.dangerText, FontWeight.Bold))
        if (body != null) Text(body, style = runTextStyle(15.sp, t.label, FontWeight.Medium))
    }
}

/** 불러오는 동안 — 임시 값 대신 자리만(묶음 모양 그대로) */
@Composable
fun BlueSkeleton(rows: Int = 4, modifier: Modifier = Modifier, switches: Boolean = true) {
    val c = blueListColors()
    BlueGroup(modifier) {
        repeat(rows) {
            Row(
                Modifier.fillMaxWidth().background(c.face).heightIn(min = 66.dp).padding(horizontal = 20.dp, vertical = 16.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Box(Modifier.weight(1f)) {
                    Box(Modifier.fillMaxWidth(0.62f).height(20.dp).clip(RoundedCornerShape(10.dp)).background(c.skeleton))
                }
                if (switches) Box(Modifier.size(width = 54.dp, height = 28.dp).clip(RoundedCornerShape(50)).background(c.skeleton))
            }
        }
    }
}

/** 읽지 못했을 때 — 이전 설정은 그대로, 다시 불러오기 */
@Composable
fun BlueLoadFailed(title: String, body: String, retryLabel: String, onRetry: () -> Unit, modifier: Modifier = Modifier) {
    val t = runTone()
    Column(
        modifier.fillMaxWidth().padding(top = 96.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Box(Modifier.size(64.dp).border(2.dp, t.cobaltText, CircleShape), contentAlignment = Alignment.Center) {
            Icon(Icons.Filled.PriorityHigh, contentDescription = null, tint = t.cobaltText, modifier = Modifier.size(34.dp))
        }
        Spacer(Modifier.height(4.dp))
        Text(title, style = runTextStyle(21.sp, t.text, FontWeight.Bold), textAlign = TextAlign.Center)
        Text(body, style = runTextStyle(15.sp, t.label, FontWeight.Medium), textAlign = TextAlign.Center)
        Spacer(Modifier.height(10.dp))
        Box(Modifier.widthIn(max = 280.dp)) { RunButton(retryLabel, onRetry) }
    }
}

// ── 시트 · 알림 ─────────────────────────────────────────────────────

/**
 * 아래에서 올라오는 시트 — 하단 탭까지 덮는다(창이 따로 뜬다). 뒤 화면은 누를 수 없고 뒤로 가기로 닫힌다.
 * [dismissible] 이 아니면(지우는 중 · 삭제 요청 중) 닫히지 않는다. [centered] 면 제목 · 본문을 가운데로.
 */
@Composable
fun BlueSheet(
    title: String,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
    dismissible: Boolean = true,
    showClose: Boolean = true,
    centered: Boolean = false,
    compactTitle: Boolean = false,
    actions: (@Composable ColumnScope.() -> Unit)? = null,
    content: @Composable ColumnScope.() -> Unit,
) {
    val t = runTone()
    val feedback = LocalFeedback.current
    LaunchedEffect(feedback) { feedback?.play(FeedbackCue.SheetOpen) }
    fun dismiss() {
        if (!dismissible) return
        feedback?.play(FeedbackCue.SheetClose)
        onDismiss()
    }
    Dialog(
        onDismissRequest = { dismiss() },
        properties = DialogProperties(
            usePlatformDefaultWidth = false, decorFitsSystemWindows = false,
            dismissOnBackPress = dismissible, dismissOnClickOutside = dismissible,
        ),
    ) {
        BoxWithConstraints(Modifier.fillMaxSize().statusBarsPadding(), contentAlignment = Alignment.BottomCenter) {
            Box(Modifier.matchParentSize().pointerInput(dismissible) { detectTapGestures { dismiss() } })
            val shape = RoundedCornerShape(topStart = RunSpec.SheetRadius, topEnd = RunSpec.SheetRadius)
            Column(
                modifier.widthIn(max = 600.dp).fillMaxWidth().heightIn(max = maxHeight - 24.dp)
                    .clip(shape).background(Brush.verticalGradient(listOf(t.sheetTop, t.sheet)), shape)
                    .border(1.5.dp, t.secondaryEdge.copy(alpha = if (t.dark) 0.9f else 0.6f), shape)
                    .pointerInput(Unit) { detectTapGestures { } }
                    .navigationBarsPadding().testTag("settings-sheet"),
            ) {
                Box(Modifier.fillMaxWidth()) {
                    Box(
                        Modifier.align(Alignment.TopCenter).padding(top = 12.dp).size(width = 44.dp, height = 5.dp)
                            .clip(RoundedCornerShape(3.dp)).background(if (t.dark) Color(0xFF3B7BE8) else t.handle),
                    )
                    if (showClose) {
                        val close = stringResource(R.string.common_close)
                        Box(
                            Modifier.align(Alignment.TopEnd).padding(top = 18.dp, end = 10.dp).size(48.dp).clip(CircleShape)
                                .feedbackClickable(enabled = dismissible, cue = FeedbackCue.Back) { dismiss() }
                                .semantics { contentDescription = close }.testTag("blue-sheet-close"),
                            contentAlignment = Alignment.Center,
                        ) {
                            Icon(Icons.Filled.Close, contentDescription = null, tint = if (dismissible) t.text else t.muted, modifier = Modifier.size(28.dp))
                        }
                    }
                    Text(
                        title,
                        style = runTextStyle(if (compactTitle) 20.sp else if (centered) 24.sp else 26.sp, t.text, FontWeight.ExtraBold, 1.3f),
                        textAlign = if (centered) TextAlign.Center else TextAlign.Start,
                        modifier = Modifier.fillMaxWidth()
                            .padding(start = if (centered) 56.dp else 24.dp, end = if (showClose || centered) 56.dp else 24.dp, top = if (compactTitle) 32.dp else 40.dp)
                            .semantics { heading() },
                    )
                }
                Column(
                    Modifier.weight(1f, fill = false).verticalScroll(rememberScrollState())
                        .padding(horizontal = 24.dp, vertical = 12.dp),
                    horizontalAlignment = if (centered) Alignment.CenterHorizontally else Alignment.Start,
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                    content = content,
                )
                if (actions != null) {
                    Column(
                        Modifier.fillMaxWidth().padding(start = 20.dp, end = 20.dp, top = 8.dp, bottom = 20.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        content = actions,
                    )
                } else {
                    Spacer(Modifier.height(20.dp))
                }
            }
        }
    }
}

/** 시트 안의 가는 가로선 */
@Composable
fun BlueSheetDivider(modifier: Modifier = Modifier) {
    Box(modifier.fillMaxWidth().padding(vertical = 4.dp).height(1.dp).background(runTone().secondaryEdge.copy(alpha = 0.45f)))
}

/**
 * 짧은 알림 — "✓ 알림 기록을 지웠어요." 화면 아래에 잠깐. 실제로 끝난 일에만 체크를 붙인다.
 */
@Composable
fun BlueToast(message: String?, modifier: Modifier = Modifier, success: Boolean = true, tag: String = "settings-toast") {
    val t = runTone()
    val c = blueListColors()
    val motion = LocalMotion.current
    AnimatedVisibility(
        visible = message != null, modifier = modifier,
        enter = fadeIn(tween(motion.duration(160))), exit = fadeOut(tween(motion.duration(160))),
    ) {
        val shape = RoundedCornerShape(14.dp)
        Row(
            Modifier.widthIn(max = 520.dp).clip(shape).background(if (t.dark) Color(0xFF0B2A57) else Color.White, shape)
                .border(1.dp, c.edge, shape)
                .padding(horizontal = 18.dp, vertical = 14.dp).semantics { liveRegion = LiveRegionMode.Polite }
                .testTag(tag),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            if (success) Icon(Icons.Filled.CheckCircle, contentDescription = null, tint = t.cyan, modifier = Modifier.size(24.dp))
            Text(message.orEmpty(), style = runTextStyle(15.sp, t.text, FontWeight.SemiBold))
        }
    }
}

/**
 * 넓은 주 버튼 — 누를 수 있으면 흰 주 버튼(RunButton), 누를 것이 없으면 남색 면 · 흐린 글자(시안 PRO01 의 비활성 "저장").
 * 일하는 중이면 도는 표시와 함께 누를 수 없다.
 */
@Composable
fun BlueWideButton(
    label: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    busy: Boolean = false,
    kind: RunButtonKind = RunButtonKind.Primary,
) {
    if (enabled || busy) {
        RunButton(label, onClick, modifier, kind = kind, enabled = enabled, busy = busy)
        return
    }
    val t = runTone()
    val c = blueListColors()
    val shape = RoundedCornerShape(18.dp)
    Box(
        modifier.fillMaxWidth().heightIn(min = 58.dp).clip(shape).background(c.face, shape).border(1.dp, c.edge.copy(alpha = 0.6f), shape)
            .feedbackClickable(enabled = false, role = Role.Button, onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Text(label, style = runTextStyle(18.sp, t.muted, FontWeight.Bold), textAlign = TextAlign.Center)
    }
}

/** 원 안의 느낌표 · i — 안내 칸 왼쪽 */
val BlueInfoIcon: ImageVector get() = Icons.Outlined.Info
val BlueAlertIcon: ImageVector get() = Icons.Outlined.ErrorOutline
