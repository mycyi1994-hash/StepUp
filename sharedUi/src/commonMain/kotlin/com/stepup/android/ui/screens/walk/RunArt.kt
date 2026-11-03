package com.stepup.android.ui.screens.walk

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.ui.Alignment
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.unit.dp
import com.stepup.android.ui.components.RunStateArt
import com.stepup.android.ui.components.runTone
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import com.stepup.ui.res.*

/** 빗금 그은 그림 — 꺼짐(P02 신체 활동 · L02 위치 권한 · E07 위치 기능). 빗금은 원 안에서 */
@Composable
fun SlashedArt(icon: ImageVector, cyan: Boolean = false, size: androidx.compose.ui.unit.Dp = 200.dp) {
    val t = runTone()
    Box(contentAlignment = Alignment.Center) {
        RunStateArt(icon, size = size)
        Canvas(Modifier.size(size)) {
            val r = this.size.minDimension / 2.25f
            val d = r * 0.6f
            drawLine(
                if (cyan) t.cyan else t.cobalt, androidx.compose.ui.geometry.Offset(center.x - d, center.y - d),
                androidx.compose.ui.geometry.Offset(center.x + d, center.y + d), strokeWidth = 7.dp.toPx(), cap = StrokeCap.Round,
            )
        }
    }
}
