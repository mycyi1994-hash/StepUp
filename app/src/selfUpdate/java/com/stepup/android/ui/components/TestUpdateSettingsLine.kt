package com.stepup.android.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.stepup.android.R
import com.stepup.android.ui.experience.feedbackClickable

// 설정 › 앱 정보의 "새 버전 확인" 줄 — 테스트 APK(debug · qa)에만 들어간다(src/selfUpdate). 스토어 판은 src/noSelfUpdate 의 빈 줄.

/** 테스트 APK 에서만 — 새 빌드를 지금 확인한다. 있으면 앱 위에 업데이트 창이 뜬다 */
@Composable
fun TestUpdateSettingsLine() {
    val t = runTone()
    val state by com.stepup.android.core.TestUpdates.state.collectAsState()
    val status = when (state) {
        com.stepup.android.core.TestUpdates.State.Checking -> stringResource(R.string.test_update_checking)
        com.stepup.android.core.TestUpdates.State.UpToDate -> stringResource(R.string.test_update_latest)
        com.stepup.android.core.TestUpdates.State.CheckFailed -> stringResource(R.string.test_update_check_failed)
        is com.stepup.android.core.TestUpdates.State.Available,
        is com.stepup.android.core.TestUpdates.State.Dismissed,
        is com.stepup.android.core.TestUpdates.State.Failed -> stringResource(R.string.test_update_found)
        is com.stepup.android.core.TestUpdates.State.Downloading -> stringResource(R.string.test_update_downloading_short)
        is com.stepup.android.core.TestUpdates.State.Installing -> stringResource(R.string.test_update_installing_short)
        is com.stepup.android.core.TestUpdates.State.NeedsPermission -> stringResource(R.string.test_update_permission_short)
        com.stepup.android.core.TestUpdates.State.Idle -> stringResource(R.string.test_update_check)
    }
    Row(
        Modifier.fillMaxWidth().background(blueListColors().face).heightIn(min = 56.dp).testTag("test-update-check")
            .feedbackClickable(onClick = { com.stepup.android.core.TestUpdates.startCheck(force = true) })
            .padding(horizontal = 20.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Text(stringResource(R.string.test_update_row), style = runTextStyle(16.sp, t.label, FontWeight.Medium), modifier = Modifier.weight(1f, fill = false))
        Spacer(Modifier.width(12.dp))
        Text(status, style = runTextStyle(15.sp, t.cyan, FontWeight.SemiBold))
    }
}
