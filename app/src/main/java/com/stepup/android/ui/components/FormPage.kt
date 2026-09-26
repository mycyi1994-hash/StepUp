package com.stepup.android.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
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
    /** S2 원형 버튼 안의 아이콘. 비우면 확인 표시 */
    actionIcon: androidx.compose.ui.graphics.vector.ImageVector = Icons.Filled.Check,
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
        // S2 — 작성의 주 행동도 가운데 흰 원 + 이름. 키보드 위에 붙어 있다.
        S2RoundAction(
            icon = actionIcon, label = actionLabel, enabled = actionEnabled, onClick = onAction,
            modifier = Modifier.align(androidx.compose.ui.Alignment.CenterHorizontally)
                .padding(vertical = 8.dp).testTag(actionTag),
        )
    }
}
