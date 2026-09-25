package com.stepup.android.ui.components

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
    Row(
        Modifier.fillMaxWidth().clip(shape).background(CarbonHigh)
            .border(1.dp, Edge, shape)
            .toggleable(checked, enabled = enabled, role = Role.Switch, onValueChange = {
                feedback?.play(FeedbackCue.Toggle)
                onCheckedChange(it)
            })
            .padding(StepUpDesign.PanelPadding),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        IconSquare(icon, tint = if (checked && enabled) Volt else Silver, size = 40.dp)
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(title, style = MaterialTheme.typography.titleMedium, color = if (enabled) Snow else Silver)
            Text(description, style = MaterialTheme.typography.bodyMedium, color = Silver)
        }
        Switch(
            checked = checked, onCheckedChange = null, enabled = enabled,
            colors = SwitchDefaults.colors(
                checkedThumbColor = OnVolt, checkedTrackColor = Volt,
                uncheckedThumbColor = Silver, uncheckedTrackColor = Carbon,
                uncheckedBorderColor = Edge,
            ),
        )
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
    Row(
        Modifier.fillMaxWidth().clip(shape)
            .background(if (selected) Volt.copy(alpha = .10f) else CarbonHigh)
            .border(1.dp, if (selected) Volt else Edge, shape)
            .selectable(selected, role = Role.RadioButton, onClick = onClick)
            .padding(StepUpDesign.PanelPadding),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        if (icon != null) IconSquare(icon, tint = if (selected) Volt else Silver, size = 40.dp)
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(title, style = MaterialTheme.typography.titleMedium, color = Snow)
            if (!description.isNullOrBlank()) Text(description, style = MaterialTheme.typography.bodyMedium, color = Silver)
        }
        RadioButton(selected, onClick = null, colors = RadioButtonDefaults.colors(selectedColor = Volt, unselectedColor = Silver))
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
) {
    OutlinedTextField(
        value = value, onValueChange = onValueChange,
        modifier = modifier.fillMaxWidth().heightIn(min = minHeight)
            .semantics { contentDescription = label },
        label = { Text(label) }, placeholder = { Text(placeholder) },
        singleLine = singleLine, minLines = minLines, maxLines = maxLines,
        keyboardOptions = KeyboardOptions(keyboardType = keyboardType),
        textStyle = MaterialTheme.typography.bodyLarge,
        shape = RoundedCornerShape(StepUpDesign.FieldRadius),
        colors = OutlinedTextFieldDefaults.colors(
            focusedTextColor = Snow, unfocusedTextColor = Snow,
            focusedContainerColor = CarbonHigh, unfocusedContainerColor = CarbonHigh,
            focusedBorderColor = Volt, unfocusedBorderColor = Edge,
            focusedLabelColor = VoltText, unfocusedLabelColor = Silver,
            focusedPlaceholderColor = Silver, unfocusedPlaceholderColor = Silver,
            cursorColor = Volt,
        ),
    )
}
