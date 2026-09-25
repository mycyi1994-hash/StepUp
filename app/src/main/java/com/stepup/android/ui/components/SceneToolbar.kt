package com.stepup.android.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.outlined.Image
import androidx.compose.material.icons.filled.MoreHoriz
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.stepup.android.R
import com.stepup.android.ui.experience.FeedbackCue

/** Scene controls reuse the same native 48 dp targets as every shared header. */
@Composable
fun SceneToolbar(
    onBack: () -> Unit,
    onChangeBackground: () -> Unit,
    onMore: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
        DarkIconButton(Icons.AutoMirrored.Filled.ArrowBack, stringResource(R.string.cd_back), onBack,
            cue = FeedbackCue.Back)
        Spacer(Modifier.weight(1f))
        DarkIconButton(Icons.Outlined.Image, stringResource(R.string.wardrobe_change_background),
            onChangeBackground, Modifier.testTag("wardrobe-background"), cue = FeedbackCue.BackgroundSwitch)
        DarkIconButton(Icons.Filled.MoreHoriz, stringResource(R.string.common_more),
            onMore, Modifier.testTag("wardrobe-options"))
    }
}
