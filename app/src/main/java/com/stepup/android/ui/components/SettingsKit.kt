package com.stepup.android.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.stepup.android.R
import com.stepup.android.ui.experience.FeedbackCue
import com.stepup.android.ui.experience.LocalFeedback
import com.stepup.android.ui.experience.LocalMotion
import com.stepup.android.ui.experience.feedbackClickable
import com.stepup.android.ui.theme.StepUpColors

/*
 * 설정 v1(2026-09-28 전달본, docs/redesign/settings-v1) 한 벌 — 짧은 목록 줄 · 스위치 줄 · 선택 줄 · 권한 줄 ·
 * 안내 · 버튼 · 아래에서 올라오는 시트 · 짧은 알림. 설정의 모든 화면이 이것만 쓴다.
 *
 * 카드와 아이콘을 늘리지 않고 이름 · 현재 값 · 누를 곳만 둔다. 어두운 테마는 시안 값, 밝은 테마는 시안의 밝은 값.
 * 줄과 시트는 글씨가 커지거나 번역이 길어지면 높이가 늘어난다(최소 높이만 정한다). 누르는 곳은 모두 48dp 이상.
 */

@Immutable
class SettingsPalette(
    val text: Color,
    val secondary: Color,
    val accent: Color,
    val divider: Color,
    val surface: Color,
    val sheet: Color,
    val toast: Color,
    val toastText: Color,
    val switchOn: Color,
    val switchOff: Color,
    val cancel: Color,
    val primaryFace: Brush,
    val primaryText: Color,
    val dangerFace: Color,
    val dangerText: Color,
    /** 줄 글자로 쓰는 위험 색 — "계정 삭제" */
    val danger: Color,
    val skeleton: Color,
)

private val DarkSettings = SettingsPalette(
    text = Color(0xFFF2F4FC), secondary = Color(0xFF98A8C0), accent = Color(0xFFA3BFFE),
    divider = Color(0xFF1D2B3F), surface = Color(0xFF0E192A), sheet = Color(0xFF111D2E),
    toast = Color(0xFF273B55), toastText = Color(0xFFF2F4FC),
    switchOn = Color(0xFFA3BFFE), switchOff = Color(0xFF34445C), cancel = Color(0xFF0B1524),
    primaryFace = Brush.verticalGradient(listOf(Color(0xFFF7F8FF), Color(0xFFE8EDFA))), primaryText = Color(0xFF10203B),
    dangerFace = Color(0xFFF2B3B8), dangerText = Color(0xFF36151D), danger = Color(0xFFF2A3AA),
    skeleton = Color(0xFF1A2940),
)

private val LightSettings = SettingsPalette(
    text = Color(0xFF10203B), secondary = Color(0xFF536580), accent = Color(0xFF335EAB),
    divider = Color(0xFFD8E0ED), surface = Color(0xFFE7EDF6), sheet = Color(0xFFFFFFFF),
    toast = Color(0xFF273B55), toastText = Color(0xFFF2F4FC),
    switchOn = Color(0xFF335EAB), switchOff = Color(0xFFC3CDDD), cancel = Color(0xFFE7EDF6),
    primaryFace = Brush.verticalGradient(listOf(Color(0xFF1B2D4E), Color(0xFF10203B))), primaryText = Color(0xFFFFFFFF),
    dangerFace = Color(0xFFF2B3B8), dangerText = Color(0xFF36151D), danger = Color(0xFFC0364A),
    skeleton = Color(0xFFDCE4F0),
)

@Composable
fun settingsPalette(): SettingsPalette = if (StepUpColors.dark) DarkSettings else LightSettings

/** 그룹 이름 — "러닝과 알림". 제목으로 읽힌다 */
@Composable
fun SettingsGroupLabel(text: String, modifier: Modifier = Modifier) {
    val p = settingsPalette()
    Text(
        text, color = p.accent, fontSize = 13.sp, fontWeight = FontWeight.SemiBold,
        modifier = modifier.fillMaxWidth().padding(top = 18.dp, bottom = 2.dp).semantics { heading() },
    )
}

/** 화면 위 한 줄 설명 · 아래 짧은 안내("바꾸면 바로 저장돼요.") */
@Composable
fun SettingsNote(text: String, modifier: Modifier = Modifier, top: Boolean = false) {
    val p = settingsPalette()
    Text(
        text, color = p.secondary, fontSize = 14.sp, lineHeight = 1.45.em,
        modifier = modifier.fillMaxWidth().padding(top = if (top) 4.dp else 18.dp, bottom = if (top) 6.dp else 0.dp),
    )
}

@Composable
private fun SettingsDivider() {
    Box(Modifier.fillMaxWidth().height(1.dp).background(settingsPalette().divider))
}

@Composable
private fun RowTexts(title: String, description: String?, titleColor: Color, modifier: Modifier = Modifier) {
    val p = settingsPalette()
    Column(modifier, verticalArrangement = Arrangement.spacedBy(5.dp)) {
        Text(title, color = titleColor, fontSize = 17.sp, fontWeight = FontWeight.SemiBold, lineHeight = 1.3.em)
        if (description != null) Text(description, color = p.secondary, fontSize = 13.sp, lineHeight = 1.4.em)
    }
}

/** 들어가는 줄 — 이름 · (현재 값) · 오른쪽 화살표. 설명이 있으면 두 줄 */
@Composable
fun SettingsNavRow(
    title: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    value: String? = null,
    description: String? = null,
    danger: Boolean = false,
    enabled: Boolean = true,
) {
    val p = settingsPalette()
    // 누르는 곳 · 이름표(테스트 태그)는 줄 전체(구분선 포함) 한 곳에 — 합친 의미 노드가 태그를 갖는다
    Column(modifier.fillMaxWidth().feedbackClickable(enabled = enabled, role = Role.Button, onClick = onClick)) {
        Row(
            Modifier.fillMaxWidth().heightIn(min = if (description != null) 76.dp else 58.dp).padding(vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            RowTexts(title, description, if (danger) p.danger else p.text, Modifier.weight(1f))
            if (value != null) {
                Text(value, color = p.secondary, fontSize = 15.sp, textAlign = TextAlign.End,
                    modifier = Modifier.widthIn(max = 160.dp))
            }
            Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, contentDescription = null,
                tint = p.secondary, modifier = Modifier.size(22.dp))
        }
        SettingsDivider()
    }
}

/** 스위치 줄 — 줄 전체가 한 스위치다(이름 · 현재 상태를 한 번에 읽고, 따로 초점이 두 번 잡히지 않는다) */
@Composable
fun SettingsSwitchRow(
    title: String,
    description: String?,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
) {
    val p = settingsPalette()
    Column(
        modifier.fillMaxWidth()
            .toggleable(value = checked, enabled = enabled, role = Role.Switch, onValueChange = onCheckedChange),
    ) {
        Row(
            Modifier.fillMaxWidth().heightIn(min = if (description != null) 76.dp else 58.dp).padding(vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            RowTexts(title, description, p.text, Modifier.weight(1f))
            SettingsSwitchFace(checked, enabled)
        }
        SettingsDivider()
    }
}

/** 스위치 모양만 — 켜짐은 밝은 파랑에 ✓, 꺼짐은 어두운 면에 − (색만으로 상태를 말하지 않는다) */
@Composable
private fun SettingsSwitchFace(checked: Boolean, enabled: Boolean) {
    val p = settingsPalette()
    val track = if (checked) p.switchOn else p.switchOff
    Box(
        Modifier.size(width = 52.dp, height = 30.dp).clip(RoundedCornerShape(50))
            .background(if (enabled) track else track.copy(alpha = 0.5f)).padding(3.dp),
    ) {
        Icon(
            if (checked) Icons.Filled.Check else Icons.Filled.Remove, contentDescription = null,
            tint = if (checked) Color(0xFF10203B).copy(alpha = 0.7f) else Color.White.copy(alpha = 0.55f),
            modifier = Modifier.size(14.dp).align(if (checked) Alignment.CenterStart else Alignment.CenterEnd)
                .padding(horizontal = 1.dp),
        )
        Box(
            Modifier.size(24.dp).align(if (checked) Alignment.CenterEnd else Alignment.CenterStart)
                .background(Color.White, CircleShape),
        )
    }
}

/** 하나만 고르는 줄 — 테마 · 언어. 고른 줄은 채운 동그라미에 ✓ */
@Composable
fun SettingsChoiceRow(
    title: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    description: String? = null,
) {
    val p = settingsPalette()
    Column(modifier.fillMaxWidth().selectable(selected = selected, role = Role.RadioButton, onClick = onClick)) {
        Row(
            Modifier.fillMaxWidth().heightIn(min = if (description != null) 76.dp else 58.dp).padding(vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            RowTexts(title, description, p.text, Modifier.weight(1f))
            Box(
                Modifier.size(26.dp).clip(CircleShape)
                    .then(if (selected) Modifier.background(p.accent) else Modifier.border(1.5.dp, p.secondary, CircleShape)),
                contentAlignment = Alignment.Center,
            ) {
                if (selected) Icon(Icons.Filled.Check, contentDescription = null,
                    tint = if (StepUpColors.dark) Color(0xFF10203B) else Color.White, modifier = Modifier.size(16.dp))
            }
        }
        SettingsDivider()
    }
}

/** 권한 · 상태 줄 — 앱 안의 스위치가 아니다. 상태는 글자로(허용됨 · 정확한 위치 · 허용 안 됨) */
@Composable
fun SettingsStatusRow(
    title: String,
    description: String?,
    status: String,
    modifier: Modifier = Modifier,
    strong: Boolean = true,
) {
    val p = settingsPalette()
    Column(modifier.fillMaxWidth().semantics(mergeDescendants = true) {}) {
        Row(
            Modifier.fillMaxWidth().heightIn(min = if (description != null) 76.dp else 58.dp).padding(vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            RowTexts(title, description, p.text, Modifier.weight(1f))
            Text(status, color = if (strong) p.accent else p.secondary, fontSize = 15.sp,
                fontWeight = FontWeight.SemiBold, textAlign = TextAlign.End, modifier = Modifier.widthIn(max = 150.dp))
        }
        SettingsDivider()
    }
}

/** 안내 한 칸 — 휴대폰 알림 꺼짐 · 서버 반영 대기 · 저장 실패. 할 일이 있으면 아래 글자 버튼 하나 */
@Composable
fun SettingsNotice(
    title: String,
    body: String,
    modifier: Modifier = Modifier,
    actionLabel: String? = null,
    onAction: () -> Unit = {},
    actionEnabled: Boolean = true,
    icon: ImageVector = Icons.Outlined.Info,
) {
    val p = settingsPalette()
    Column(
        modifier.fillMaxWidth().clip(RoundedCornerShape(20.dp)).background(p.surface)
            .padding(start = 18.dp, end = 18.dp, top = 16.dp, bottom = if (actionLabel != null) 4.dp else 16.dp)
            .semantics { liveRegion = LiveRegionMode.Polite },
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            Icon(icon, contentDescription = null, tint = p.accent, modifier = Modifier.size(20.dp))
            Text(title, color = p.text, fontSize = 16.sp, fontWeight = FontWeight.SemiBold, lineHeight = 1.35.em)
        }
        Text(body, color = p.secondary, fontSize = 14.sp, lineHeight = 1.5.em)
        if (actionLabel != null) {
            Box(
                Modifier.heightIn(min = 48.dp).feedbackClickable(enabled = actionEnabled, role = Role.Button, onClick = onAction),
                contentAlignment = Alignment.CenterStart,
            ) {
                Text(actionLabel, color = if (actionEnabled) p.accent else p.secondary, fontSize = 15.sp,
                    fontWeight = FontWeight.SemiBold)
            }
        }
    }
}

/** 설정의 주 버튼 — 어두운 테마는 흰 면에 남색 글자, 밝은 테마는 남색 면에 흰 글자. 일하는 중이면 돌고 누를 수 없다 */
@Composable
fun SettingsPrimaryButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    loading: Boolean = false,
) {
    val p = settingsPalette()
    SettingsButtonFace(text, onClick, modifier, enabled && !loading, loading, p.primaryFace, p.primaryText)
}

/** 보조 버튼 — "취소" · "주소 복사" */
@Composable
fun SettingsSecondaryButton(text: String, onClick: () -> Unit, modifier: Modifier = Modifier, enabled: Boolean = true) {
    val p = settingsPalette()
    SettingsButtonFace(text, onClick, modifier, enabled, false, Brush.verticalGradient(listOf(p.cancel, p.cancel)), p.text)
}

/** 되돌릴 수 없는 일 — "기록 지우기" · "계정 삭제" */
@Composable
fun SettingsDangerButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    loading: Boolean = false,
) {
    val p = settingsPalette()
    SettingsButtonFace(text, onClick, modifier, enabled && !loading, loading,
        Brush.verticalGradient(listOf(p.dangerFace, p.dangerFace)), p.dangerText)
}

@Composable
private fun SettingsButtonFace(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier,
    enabled: Boolean,
    loading: Boolean,
    face: Brush,
    ink: Color,
) {
    val shape = RoundedCornerShape(17.dp)
    Row(
        modifier.heightIn(min = 52.dp).clip(shape).background(face, shape)
            .then(if (enabled || loading) Modifier else Modifier.background(Color.Black.copy(alpha = 0.28f), shape))
            .feedbackClickable(enabled = enabled, role = Role.Button, onClick = onClick)
            .padding(horizontal = 18.dp, vertical = 12.dp),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (loading) {
            CircularProgressIndicator(Modifier.size(18.dp), color = ink, strokeWidth = 2.dp)
            Spacer(Modifier.width(10.dp))
        }
        Text(text, color = ink, fontSize = 16.sp, fontWeight = FontWeight.SemiBold, textAlign = TextAlign.Center)
    }
}

/** 불러오는 동안 — 임시 값 대신 자리만 */
@Composable
fun SettingsSkeleton(rows: Int = 4, modifier: Modifier = Modifier, switches: Boolean = true) {
    val p = settingsPalette()
    Column(modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(30.dp)) {
        repeat(rows) {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Box(Modifier.width(120.dp).height(14.dp).clip(RoundedCornerShape(7.dp)).background(p.skeleton))
                    Box(Modifier.width(190.dp).height(8.dp).clip(RoundedCornerShape(4.dp)).background(p.skeleton))
                }
                if (switches) Box(Modifier.size(width = 46.dp, height = 26.dp).clip(RoundedCornerShape(50)).background(p.skeleton))
            }
        }
    }
}

/** 읽지 못했을 때 — 이전 설정은 그대로, 다시 불러오기 */
@Composable
fun SettingsLoadFailed(title: String, body: String, retryLabel: String, onRetry: () -> Unit, modifier: Modifier = Modifier) {
    val p = settingsPalette()
    Column(
        modifier.fillMaxWidth().padding(top = 96.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        Box(Modifier.size(64.dp).clip(RoundedCornerShape(20.dp)).background(p.surface), contentAlignment = Alignment.Center) {
            Icon(Icons.Filled.Refresh, contentDescription = null, tint = p.accent,
                modifier = Modifier.size(28.dp))
        }
        Text(title, color = p.text, fontSize = 19.sp, fontWeight = FontWeight.SemiBold, textAlign = TextAlign.Center)
        Text(body, color = p.secondary, fontSize = 14.sp, textAlign = TextAlign.Center)
        Spacer(Modifier.height(10.dp))
        SettingsPrimaryButton(retryLabel, onRetry, Modifier.fillMaxWidth())
    }
}

/**
 * 아래에서 올라오는 시트 — 목표 · 문의 · 기록 지우기 확인 · 계정 삭제 · 저장하는 정보.
 * 열리면 뒤 화면을 누를 수 없고(창이 따로 뜬다) 뒤로 가기로 닫힌다. [dismissible] 이 아니면(삭제 요청 중) 닫히지 않는다.
 */
@Composable
fun SettingsSheet(
    title: String,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
    dismissible: Boolean = true,
    showClose: Boolean = true,
    actions: (@Composable ColumnScope.() -> Unit)? = null,
    content: @Composable ColumnScope.() -> Unit,
) {
    val p = settingsPalette()
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
            Column(
                modifier.widthIn(max = 600.dp).fillMaxWidth().heightIn(max = maxHeight - 24.dp)
                    .clip(RoundedCornerShape(topStart = 27.dp, topEnd = 27.dp)).background(p.sheet)
                    .pointerInput(Unit) { detectTapGestures { } }
                    .navigationBarsPadding().testTag("settings-sheet"),
            ) {
                Box(Modifier.fillMaxWidth().padding(top = 10.dp), contentAlignment = Alignment.Center) {
                    Box(Modifier.size(width = 36.dp, height = 4.dp).clip(RoundedCornerShape(2.dp)).background(p.secondary.copy(alpha = 0.5f)))
                }
                Row(
                    Modifier.fillMaxWidth().padding(start = 24.dp, end = 10.dp, top = 12.dp, bottom = 4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(title, color = p.text, fontSize = 21.sp, fontWeight = FontWeight.SemiBold, lineHeight = 1.3.em,
                        modifier = Modifier.weight(1f).semantics { heading() })
                    if (showClose) {
                        IconButton(onClick = { dismiss() }, enabled = dismissible, modifier = Modifier.size(48.dp)) {
                            Icon(Icons.Filled.Close, stringResource(R.string.common_close), tint = p.secondary)
                        }
                    } else {
                        Spacer(Modifier.height(48.dp))
                    }
                }
                Column(
                    Modifier.weight(1f, fill = false).verticalScroll(rememberScrollState())
                        .padding(horizontal = 24.dp, vertical = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    content = content,
                )
                if (actions != null) {
                    Column(
                        Modifier.fillMaxWidth().padding(start = 24.dp, end = 24.dp, top = 12.dp, bottom = 20.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp),
                        content = actions,
                    )
                } else {
                    Spacer(Modifier.height(20.dp))
                }
            }
        }
    }
}

/**
 * 짧은 알림 — "✓ 알림 기록을 지웠어요." 화면 아래에 잠깐. 실제로 끝난 일에만 ✓ 를 붙인다.
 * [success] 가 아니면(저장 실패 · 연결 확인 안내) ✓ 없이 글만.
 */
@Composable
fun SettingsToast(message: String?, modifier: Modifier = Modifier, success: Boolean = true) {
    val p = settingsPalette()
    val motion = LocalMotion.current
    AnimatedVisibility(
        visible = message != null, modifier = modifier,
        enter = fadeIn(tween(motion.duration(160))), exit = fadeOut(tween(motion.duration(160))),
    ) {
        Row(
            Modifier.widthIn(max = 520.dp).fillMaxWidth().clip(RoundedCornerShape(18.dp)).background(p.toast)
                .padding(horizontal = 18.dp, vertical = 14.dp).semantics { liveRegion = LiveRegionMode.Polite }
                .testTag("settings-toast"),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            if (success) Icon(Icons.Filled.Check, contentDescription = null, tint = p.toastText, modifier = Modifier.size(20.dp))
            Text(message.orEmpty(), color = p.toastText, fontSize = 15.sp, fontWeight = FontWeight.Medium)
        }
    }
}
