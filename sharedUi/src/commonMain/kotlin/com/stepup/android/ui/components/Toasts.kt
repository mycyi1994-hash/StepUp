package com.stepup.android.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.getString

/**
 * 짧은 알림(안드로이드 Toast 자리, 2026-11-03 iOS C3) — 공용 화면은 [showToast] 만 부르고, 앱 뿌리의 [ToastHost] 가 화면 아래에 띄운다.
 * 안드로이드 Toast.LENGTH_SHORT(2초) · LENGTH_LONG(3.5초)과 같은 시간.
 */
object Toasts {
    internal val messages = MutableSharedFlow<Pair<String, Boolean>>(extraBufferCapacity = 8, onBufferOverflow = BufferOverflow.DROP_OLDEST)
    internal val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
}

fun showToast(text: String, long: Boolean = false) {
    Toasts.messages.tryEmit(text to long)
}

fun showToast(res: StringResource, long: Boolean = false) {
    Toasts.scope.launch { showToast(getString(res), long) }
}

@Composable
fun ToastHost() {
    var shown by remember { mutableStateOf<String?>(null) }
    LaunchedEffect(Unit) {
        Toasts.messages.collect { (text, long) ->
            shown = text
            delay(if (long) 3_500 else 2_000)
            shown = null
            delay(200)
        }
    }
    Box(Modifier.fillMaxSize().safeDrawingPadding().padding(bottom = 72.dp, start = 24.dp, end = 24.dp), contentAlignment = Alignment.BottomCenter) {
        AnimatedVisibility(shown != null, enter = fadeIn(), exit = fadeOut()) {
            Text(
                shown.orEmpty(), color = Color.White, fontSize = 14.sp, textAlign = TextAlign.Center,
                modifier = Modifier.background(Color(0xE6202634), RoundedCornerShape(22.dp)).padding(horizontal = 18.dp, vertical = 11.dp).testTag("toast"),
            )
        }
    }
}
