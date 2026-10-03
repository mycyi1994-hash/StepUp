package com.stepup.android.ui.components

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBackIos
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.stepup.android.R
import com.stepup.android.ui.experience.FeedbackCue
import com.stepup.android.ui.experience.feedbackClickable

/**
 * 파란 톤 화면들의 공통 뒤로 버튼(48dp 원 · 얇은 꺾쇠) — 러닝 머리(RunTopBar)와 같은 모양.
 * 화면마다 뒤로 화살표를 따로 그리지 않고 이것을 쓴다(tools/check_design_contract.py).
 */
@Composable
fun RunBackButton(
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    tag: String? = null,
    tint: Color = runTone().text,
) {
    Box(
        modifier.size(48.dp).clip(CircleShape)
            .feedbackClickable(cue = FeedbackCue.Back, onClick = onBack)
            .then(if (tag != null) Modifier.testTag(tag) else Modifier),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            Icons.AutoMirrored.Filled.ArrowBackIos, stringResource(R.string.cd_back), tint = tint,
            modifier = Modifier.size(20.dp).offset(x = 3.dp),
        )
    }
}
