package com.stepup.android.ui.screens.walk

import android.Manifest
import android.os.Build
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.clickable
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.DirectionsRun
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import com.stepup.android.R
import com.stepup.android.ui.StepPermissions
import com.stepup.android.ui.components.S2ActionRow
import com.stepup.android.ui.components.S2Headline
import com.stepup.android.ui.components.S2Kicker
import com.stepup.android.ui.components.S2Number
import com.stepup.android.ui.components.S2RoundAction
import com.stepup.android.ui.components.S2SideInfo
import com.stepup.android.ui.components.S2Stage
import com.stepup.android.ui.components.S2Subtitle
import com.stepup.android.ui.experience.FeedbackCue
import com.stepup.android.ui.experience.LocalFeedback
import com.stepup.android.ui.theme.Silver
import com.stepup.android.ui.theme.Snow
import com.stepup.android.ui.theme.StepUpColors
import com.stepup.android.ui.theme.StepUpDesign
import com.stepup.android.ui.theme.VoltText
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first

/** 권한 안내에 한 줄로 보이는 권한 하나. 필요 정도는 실제 동작 그대로다. */
internal data class RunPermissionItem(
    val icon: ImageVector,
    val title: Int,
    val body: Int,
    val need: Int,
    val granted: Boolean,
)

/**
 * 러닝에 쓰는 권한 목록 — 기기 버전에서 실제로 묻는 것만.
 *
 * 신체 활동은 걸음을 세는 데 꼭 필요하다(API 29+). 위치는 경로 · 거리용이고 없으면
 * 걸음으로만 기록한다. 알림은 러닝 중 상태 표시(API 33+)라 골라서 켠다.
 */
@Composable
internal fun rememberRunPermissionItems(refreshKey: Any?): List<RunPermissionItem> {
    val context = LocalContext.current
    return remember(refreshKey) {
        buildList {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                add(RunPermissionItem(Icons.Filled.DirectionsRun, R.string.run_perm_activity,
                    R.string.run_perm_activity_body, R.string.run_perm_required,
                    StepPermissions.hasActivityRecognition(context)))
            }
            add(RunPermissionItem(Icons.Filled.LocationOn, R.string.run_perm_location,
                R.string.run_perm_location_body, R.string.run_perm_recommended,
                StepPermissions.hasLocation(context)))
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                add(RunPermissionItem(Icons.Filled.Notifications, R.string.run_perm_notification,
                    R.string.run_perm_notification_body, R.string.run_perm_optional,
                    ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) ==
                        android.content.pm.PackageManager.PERMISSION_GRANTED))
            }
        }
    }
}

/**
 * S2 권한 안내(시안 23) — 시스템 창을 띄우기 전에 무엇을 왜 묻는지 먼저 보인다.
 * 거절해도 "나중에"로 빠져나오고, 러닝 화면의 설정 열기 안내가 이어진다.
 */
@Composable
internal fun RunPermissionPrimer(
    items: List<RunPermissionItem>,
    onAllow: () -> Unit,
    onLater: () -> Unit,
) {
    BackHandler(onBack = onLater)
    Box(Modifier.fillMaxSize().testTag("run-permission-primer")) {
        S2Stage(Modifier.fillMaxSize())
        Column(
            Modifier.fillMaxSize().statusBarsPadding().navigationBarsPadding()
                .padding(horizontal = StepUpDesign.Gutter).padding(bottom = 12.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Column(
                Modifier.weight(1f).fillMaxWidth().verticalScroll(rememberScrollState()),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Spacer(Modifier.height(40.dp))
                S2Kicker(stringResource(R.string.run_perm_kicker))
                Spacer(Modifier.height(12.dp))
                S2Headline(stringResource(R.string.run_perm_title))
                Spacer(Modifier.height(10.dp))
                S2Subtitle(stringResource(R.string.run_perm_subtitle))
                Spacer(Modifier.height(28.dp))
                Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    items.forEach { PermissionRow(it) }
                }
            }
            S2ActionRow(
                start = {
                    S2SideInfo(
                        stringResource(R.string.run_perm_granted_label),
                        value = "${items.count { it.granted }}/${items.size}",
                    )
                },
                end = {
                    S2SideInfo(stringResource(R.string.run_perm_later), end = true, onClick = onLater,
                        modifier = Modifier.testTag("run-permission-later"))
                },
            ) {
                S2RoundAction(
                    icon = Icons.Filled.Check,
                    label = stringResource(R.string.run_perm_allow_all),
                    onClick = onAllow,
                    modifier = Modifier.testTag("run-permission-allow"),
                )
            }
        }
    }
}

@Composable
private fun PermissionRow(item: RunPermissionItem) {
    val shape = RoundedCornerShape(StepUpDesign.PanelRadius)
    Row(
        Modifier.fillMaxWidth()
            .background(StepUpColors.carbon, shape)
            .border(1.dp, if (item.granted) StepUpColors.edge else VoltText.copy(alpha = 0.45f), shape)
            .padding(16.dp),
        horizontalArrangement = Arrangement.spacedBy(14.dp),
        verticalAlignment = Alignment.Top,
    ) {
        Icon(item.icon, contentDescription = null, tint = Snow, modifier = Modifier.size(22.dp))
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(stringResource(item.title), color = Snow, style = MaterialTheme.typography.titleSmall)
            Text(stringResource(item.body), color = Silver, style = MaterialTheme.typography.bodySmall)
        }
        Text(
            stringResource(if (item.granted) R.string.run_perm_done else item.need),
            color = if (item.granted) Silver else VoltText,
            style = MaterialTheme.typography.labelMedium,
        )
    }
}

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
            remaining -= 1
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
