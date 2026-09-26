package com.stepup.android.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
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
    /** S2 원형 버튼 안의 아이콘. 비우면 앞으로 가는 화살표 */
    primaryActionIcon: androidx.compose.ui.graphics.vector.ImageVector? = null,
    showHeader: Boolean = true,
    content: LazyListScope.() -> Unit,
) {
    Column(Modifier.fillMaxSize().padding(horizontal = StepUpDesign.Gutter)) {
        if (showHeader) SecondaryHeader(onBack = onBack, balance = null, onOpenWallet = null, title = title)
        LazyColumn(
            modifier = Modifier.weight(1f),
            contentPadding = PaddingValues(top = 12.dp, bottom = 22.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
            content = content,
        )
        if (primaryActionLabel != null && onPrimaryAction != null) {
            // S2 — 화면의 주 행동 하나는 가운데 흰 원 + 이름
            S2RoundAction(
                icon = primaryActionIcon ?: Icons.AutoMirrored.Filled.ArrowForward,
                label = primaryActionLabel,
                onClick = onPrimaryAction,
                enabled = primaryActionEnabled,
                modifier = Modifier.align(androidx.compose.ui.Alignment.CenterHorizontally)
                    .padding(vertical = 8.dp).testTag("detail-primary-action"),
            )
        }
    }
}
