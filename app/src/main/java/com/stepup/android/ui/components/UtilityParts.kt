package com.stepup.android.ui.components

import androidx.compose.foundation.layout.Box
import androidx.compose.ui.unit.sp
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.focus.focusRequester
import androidx.compose.runtime.setValue
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.foundation.clickable
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.stepup.android.ui.theme.*
import com.stepup.android.ui.experience.FeedbackCue
import com.stepup.android.ui.experience.LocalFeedback

/** Settings rows share spacing, selection semantics and controls across every preference page. */
@Composable
fun PreferenceToggle(
    title: String,
    description: String,
    icon: ImageVector,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    enabled: Boolean = true,
) {
    val feedback = LocalFeedback.current
    val shape = RoundedCornerShape(StepUpDesign.PanelRadius)
    Column(
        Modifier.fillMaxWidth().clip(shape).background(CarbonHigh)
            .border(1.dp, Edge, shape)
            .toggleable(checked, enabled = enabled, role = Role.Switch, onValueChange = {
                feedback?.play(FeedbackCue.Toggle)
                onCheckedChange(it)
            })
            .padding(StepUpDesign.PanelPadding),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            IconSquare(icon, tint = if (checked && enabled) Volt else Silver, size = 40.dp)
            Text(title, modifier = Modifier.weight(1f), style = MaterialTheme.typography.titleMedium,
                color = if (enabled) Snow else Silver)
            Switch(
                checked = checked, onCheckedChange = null, enabled = enabled,
                colors = SwitchDefaults.colors(
                    checkedThumbColor = OnVolt, checkedTrackColor = Volt,
                    uncheckedThumbColor = Silver, uncheckedTrackColor = Carbon,
                    uncheckedBorderColor = Edge,
                ),
            )
        }
        Text(description, style = MaterialTheme.typography.bodyMedium, color = Silver)
    }
}

@Composable
fun PreferenceChoice(
    title: String,
    description: String?,
    selected: Boolean,
    onClick: () -> Unit,
    icon: ImageVector? = null,
) {
    val shape = RoundedCornerShape(StepUpDesign.PanelRadius)
    Column(
        Modifier.fillMaxWidth().clip(shape)
            .background(if (selected) Volt.copy(alpha = .10f) else CarbonHigh)
            .border(1.dp, if (selected) Volt else Edge, shape)
            .selectable(selected, role = Role.RadioButton, onClick = onClick)
            .padding(StepUpDesign.PanelPadding),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            if (icon != null) IconSquare(icon, tint = if (selected) Volt else Silver, size = 40.dp)
            Text(title, modifier = Modifier.weight(1f), style = MaterialTheme.typography.titleMedium, color = Snow)
            RadioButton(selected, onClick = null, colors = RadioButtonDefaults.colors(selectedColor = Volt, unselectedColor = Silver))
        }
        if (!description.isNullOrBlank()) Text(description, style = MaterialTheme.typography.bodyMedium, color = Silver)
    }
}

/** Quiet explanatory text; does not compete with the screen's actual controls. */
@Composable
fun InformationNote(text: String, icon: ImageVector, modifier: Modifier = Modifier) {
    Row(modifier.fillMaxWidth().padding(vertical = 8.dp), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        Icon(icon, contentDescription = null, tint = Silver, modifier = Modifier.size(22.dp))
        Text(text, style = MaterialTheme.typography.bodyMedium, color = Silver, modifier = Modifier.weight(1f))
    }
}

/** Records use a consistent left-aligned number rhythm instead of shrinking long values. */
@Composable
fun RowScope.RecordMetric(icon: ImageVector, label: String, value: String, tint: androidx.compose.ui.graphics.Color = Volt) {
    Column(Modifier.weight(1f).padding(horizontal = 8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Icon(icon, contentDescription = null, tint = tint, modifier = Modifier.size(22.dp))
        Text(label, style = MaterialTheme.typography.bodyMedium, color = Silver)
        Text(value, style = MaterialTheme.typography.headlineSmall, fontFamily = StepUpNumbers, color = Snow)
    }
}

/** Loading, empty, sign-in and retry states use real state from the caller. */
@Composable
fun StatePanel(
    message: String,
    icon: ImageVector,
    modifier: Modifier = Modifier,
    loading: Boolean = false,
    action: (@Composable () -> Unit)? = null,
) {
    GlowCard(modifier, contentPadding = PaddingValues(24.dp), spacing = 16.dp) {
        Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(16.dp)) {
            if (loading) {
                CircularProgressIndicator(Modifier.size(36.dp), color = Volt, strokeWidth = 3.dp)
            } else {
                IconSquare(icon, size = 56.dp)
            }
            Text(message, style = MaterialTheme.typography.bodyLarge, color = Silver, textAlign = TextAlign.Center)
            action?.invoke()
        }
    }
}

/** Native fields keep a real label, focus border and a growing multiline body. */
@Composable
fun FormField(
    label: String,
    value: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    placeholder: String = "",
    singleLine: Boolean = true,
    minLines: Int = if (singleLine) 1 else 4,
    maxLines: Int = Int.MAX_VALUE,
    minHeight: Dp = 56.dp,
    keyboardType: KeyboardType = KeyboardType.Text,
    /** 예전 모양(이름을 칸 위에)의 자리 — UI 최종 통합안은 이름을 늘 칸 안 위에 둔다 */
    @Suppress("UNUSED_PARAMETER") labelAbove: Boolean = false,
) {
    // UI 최종 통합안 입력칸 — 카드 면 · 모서리 14 · 칸 안 위에 작은 이름, 쓰는 중이면 블루 2dp 테두리(FieldFrame)
    val c = StepUpUi.colors
    var focused by remember { mutableStateOf(false) }
    val focus = remember { androidx.compose.ui.focus.FocusRequester() }
    FieldFrame(
        label, focused,
        modifier.clickable(interactionSource = remember { androidx.compose.foundation.interaction.MutableInteractionSource() }, indication = null) {
            runCatching { focus.requestFocus() }
        },
    ) {
        androidx.compose.foundation.text.BasicTextField(
            value = value, onValueChange = onValueChange,
            modifier = Modifier.fillMaxWidth().heightIn(min = (minHeight - 34.dp).coerceAtLeast(23.dp))
                .focusRequester(focus).onFocusChanged { focused = it.isFocused }
                .semantics { contentDescription = label },
            singleLine = singleLine, minLines = minLines, maxLines = maxLines,
            keyboardOptions = KeyboardOptions(keyboardType = keyboardType),
            textStyle = StepUpUi.type.body.copy(color = c.label, fontSize = 16.5.sp),
            cursorBrush = androidx.compose.ui.graphics.SolidColor(c.primary),
            decorationBox = { inner ->
                Box {
                    if (value.isEmpty() && placeholder.isNotEmpty()) {
                        Text(placeholder, style = StepUpUi.type.body.copy(fontSize = 16.5.sp), color = c.tertiaryLabel)
                    }
                    inner()
                }
            },
        )
    }
}
