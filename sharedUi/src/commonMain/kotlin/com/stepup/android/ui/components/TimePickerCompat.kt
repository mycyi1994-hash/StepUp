package com.stepup.android.ui.components

import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TimePicker
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import com.stepup.ui.res.Res
import com.stepup.ui.res.d26_time_cancel
import com.stepup.ui.res.d26_time_ok
import org.jetbrains.compose.resources.stringResource

/**
 * 시간 고르기(안드로이드 TimePickerDialog 자리) — 돌려받은 함수에 처음 시 · 분을 주면 창이 열리고, 고르면 [onPicked].
 * 24시간 · 오전/오후는 기기 설정을 따른다(is24HourClock).
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun rememberTimePicker(onPicked: (hour: Int, minute: Int) -> Unit): (hour: Int, minute: Int) -> Unit {
    var open by remember { mutableStateOf<Pair<Int, Int>?>(null) }
    val picked by rememberUpdatedState(onPicked)
    open?.let { (h, m) ->
        val state = rememberTimePickerState(initialHour = h, initialMinute = m, is24Hour = is24HourClock())
        AlertDialog(
            onDismissRequest = { open = null },
            confirmButton = { TextButton({ picked(state.hour, state.minute); open = null }) { Text(stringResource(Res.string.d26_time_ok)) } },
            dismissButton = { TextButton({ open = null }) { Text(stringResource(Res.string.d26_time_cancel)) } },
            text = { TimePicker(state) },
        )
    }
    return remember { { h, m -> open = h to m } }
}

/** 기기가 24시간 표시인가(안드로이드 DateFormat.is24HourFormat) */
expect fun is24HourClock(): Boolean
