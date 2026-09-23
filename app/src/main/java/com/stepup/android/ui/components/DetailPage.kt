package com.stepup.android.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.stepup.android.ui.theme.StepUpDesign

/** Fixed detail chrome and spacing; screens supply only their title, navigation and content. */
@Composable
fun DetailPage(
    title: String,
    onBack: () -> Unit,
    primaryActionLabel: String? = null,
    onPrimaryAction: (() -> Unit)? = null,
    primaryActionEnabled: Boolean = true,
    content: LazyListScope.() -> Unit,
) {
    Column(Modifier.fillMaxSize().padding(horizontal = StepUpDesign.Gutter)) {
        SecondaryHeader(onBack = onBack, balance = null, onOpenWallet = null, title = title)
        LazyColumn(
            modifier = Modifier.weight(1f),
            contentPadding = PaddingValues(top = 12.dp, bottom = 22.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
            content = content,
        )
        if (primaryActionLabel != null && onPrimaryAction != null) {
            PrimaryCta(
                text = primaryActionLabel,
                onClick = onPrimaryAction,
                enabled = primaryActionEnabled,
                modifier = Modifier.padding(vertical = 12.dp),
            )
        }
    }
}
