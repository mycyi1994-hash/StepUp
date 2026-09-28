package com.stepup.android.ui.screens.walk

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.stepup.android.R
import com.stepup.android.ui.components.S2ActionRow
import com.stepup.android.ui.components.S2Kicker
import com.stepup.android.ui.components.S2Number
import com.stepup.android.ui.components.S2SideInfo
import com.stepup.android.ui.components.S2Stage
import com.stepup.android.ui.components.S2Subtitle
import com.stepup.android.ui.experience.FeedbackCue
import com.stepup.android.ui.experience.LocalFeedback
import com.stepup.android.ui.theme.StepUpDesign
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first

// 러닝 시작 전 권한 안내는 RunPermissionFlow.kt(시작·로그인·첫 사용 v1 시안 13~19)로 옮겼다 — 예전 한 장짜리 안내(S2 시안 23)를 대신한다.

/**
 * S2 혼자 러닝 3-2-1(시안 24). 끝나면 [onGo] 가 한 번만 불린다.
 * 화면을 누르면 바로 시작하고, 취소나 뒤로 가기는 아무것도 시작하지 않는다.
 */
@Composable
internal fun RunCountdown(
    courseName: String?,
    locationAllowed: Boolean,
    onGo: () -> Unit,
    onCancel: () -> Unit,
    /** 휴대폰 위치 기능이 켜져 있는가(권한과 별개) */
    locationServicesOn: Boolean = true,
) {
    val feedback = LocalFeedback.current
    var remaining by rememberSaveable { mutableIntStateOf(3) }
    var fired by remember { mutableStateOf(false) }
    val go = {
        if (!fired) {
            fired = true
            onGo()
        }
    }
    BackHandler { if (!fired) onCancel() }
    // 앱이 화면에 없으면 세지 않는다 — 백그라운드에서 러닝 서비스를 띄우면 안드로이드 12+ 가
    // 막아 앱이 죽는다(ForegroundServiceStartNotAllowedException). 돌아오면 남은 숫자부터 잇는다.
    val lifecycle = androidx.lifecycle.compose.LocalLifecycleOwner.current.lifecycle
    LaunchedEffect(Unit) {
        suspend fun awaitShown() {
            lifecycle.currentStateFlow.first { it.isAtLeast(androidx.lifecycle.Lifecycle.State.RESUMED) }
        }
        while (remaining > 0) {
            awaitShown()
            feedback?.play(FeedbackCue.Countdown)
            delay(1_000)
            // 그 1초 사이에 앱을 벗어났으면 세지 않는다 — 마지막 1초에 벗어났다가 (몇 시간 뒤에) 돌아오는
            // 순간 카운트 없이 러닝이 시작되지 않게, 돌아오면 그 숫자를 다시 센다
            if (lifecycle.currentState.isAtLeast(androidx.lifecycle.Lifecycle.State.RESUMED)) remaining -= 1
        }
        awaitShown()
        go()
    }
    val tapToStart = stringResource(R.string.run_countdown_tap)
    Box(
        Modifier.fillMaxSize().testTag("run-countdown")
            .semantics { contentDescription = tapToStart }
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null, role = Role.Button,
            ) { go() },
    ) {
        S2Stage(Modifier.fillMaxSize())
        Column(
            Modifier.fillMaxSize().statusBarsPadding().navigationBarsPadding()
                .padding(horizontal = StepUpDesign.Gutter).padding(bottom = 12.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Spacer(Modifier.height(56.dp))
            S2Kicker(
                if (courseName.isNullOrBlank()) stringResource(R.string.run_countdown_kicker)
                else stringResource(R.string.run_countdown_kicker_course, courseName),
            )
            Box(Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
                S2Number(remaining.coerceAtLeast(1).toString(), 180.sp,
                    modifier = Modifier.testTag("run-countdown-digit"))
            }
            S2Subtitle(stringResource(
                when {
                    !locationAllowed -> R.string.run_countdown_gps_off
                    !locationServicesOn -> R.string.run_countdown_location_services_off
                    else -> R.string.run_countdown_gps_on
                },
            ))
            Spacer(Modifier.height(16.dp))
            S2ActionRow(
                start = { S2SideInfo(tapToStart) },
                end = {
                    S2SideInfo(stringResource(R.string.common_cancel), end = true,
                        onClick = { if (!fired) onCancel() },
                        modifier = Modifier.testTag("run-countdown-cancel"))
                },
            ) { Spacer(Modifier.size(88.dp, 1.dp)) }
        }
    }
}
