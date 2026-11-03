package com.stepup.android.ui.screens.walk

import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import com.stepup.ui.res.*


fun formatPace(secPerKm: Long): String =
    "${secPerKm / 60}'${com.stepup.android.domain.zeroPad((secPerKm % 60).toLong(), 2)}\""


fun formatDuration(totalSec: Long): String {
    val hours = totalSec / 3600
    val minutes = (totalSec % 3600) / 60
    val seconds = totalSec % 60
    return if (hours > 0) {
        "${hours}:${com.stepup.android.domain.zeroPad((minutes).toLong(), 2)}:${com.stepup.android.domain.zeroPad((seconds).toLong(), 2)}"
    } else {
        "${com.stepup.android.domain.zeroPad((minutes).toLong(), 2)}:${com.stepup.android.domain.zeroPad((seconds).toLong(), 2)}"
    }
}
