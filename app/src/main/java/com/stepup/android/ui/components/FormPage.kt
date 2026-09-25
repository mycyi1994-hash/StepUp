package com.stepup.android.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import com.stepup.android.ui.theme.StepUpDesign

/** Shared writing surface: the editor scrolls while its action stays above the keyboard. */
@Composable
fun FormPage(
    title: String,
    onBack: () -> Unit,
    actionLabel: String,
    actionEnabled: Boolean,
    actionTag: String,
    onAction: () -> Unit,
    content: LazyListScope.() -> Unit,
) {
    Column(Modifier.fillMaxSize().imePadding().padding(horizontal = StepUpDesign.Gutter)) {
        FocusHeader(title, onBack)
        LazyColumn(
            modifier = Modifier.weight(1f).testTag("form-content"),
            contentPadding = PaddingValues(vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
            content = content,
        )
        PrimaryCta(
            text = actionLabel, enabled = actionEnabled, onClick = onAction,
            modifier = Modifier.fillMaxWidth().padding(vertical = 12.dp).testTag(actionTag),
        )
    }
}
