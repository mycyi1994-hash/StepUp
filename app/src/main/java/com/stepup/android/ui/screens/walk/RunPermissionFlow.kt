package com.stepup.android.ui.screens.walk

import android.Manifest
import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.os.Build
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.requiredSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.automirrored.filled.DirectionsRun
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material3.Text
import androidx.compose.ui.Alignment
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import com.stepup.android.ui.components.LiveRouteMap
import com.stepup.android.ui.components.RunButton
import com.stepup.android.ui.components.RunButtonKind
import com.stepup.android.ui.components.RunDialog
import com.stepup.android.ui.components.RunMapFrame
import com.stepup.android.ui.components.RunMapPlaceholder
import com.stepup.android.ui.components.RunPage
import com.stepup.android.ui.components.RunSheet
import com.stepup.android.ui.components.RunStateArt
import com.stepup.android.ui.components.runTextStyle
import com.stepup.android.ui.components.runTone
import com.stepup.android.ui.theme.StepUpSans
import androidx.compose.material.icons.Icons
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.core.app.ActivityCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import com.stepup.android.R
import com.stepup.android.core.ExternalIntents
import com.stepup.android.core.ServiceLocator
import com.stepup.android.domain.ActivityRequestHistory
import com.stepup.android.domain.RunPermissionRules
import com.stepup.android.domain.RunPermissionSheet
import com.stepup.android.domain.RunPermissionState
import com.stepup.android.domain.RunPermissionStep
import com.stepup.android.ui.StepPermissions
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

/**
 * 러닝 권한 안내(시작·로그인·첫 사용 v1 시안 13~19) — 한 번 고른 러닝 하나에 붙는다.
 *
 * 부른 쪽이 러닝을 고른 동안에만 이 composable 을 둔다. [onReady] · [onCancel] · [onHome] 중 하나가 불리면
 * 부른 쪽이 내리고, 그걸로 그 러닝 의도도 끝난다(다른 화면으로 가도 마찬가지 — 돌아와서 저절로 시작하지 않는다).
 *
 * - 활동 → 위치 → 알림 차례. 이미 허용했거나 이미 고른 선택 권한(위치 · 알림)은 건너뛴다.
 * - 시스템 권한 창은 안내의 주 버튼을 눌렀을 때만 띄운다. 창이 떠 있는 동안 우리 시트는 내린다.
 * - 허용 여부를 앱 표시로 대신하지 않는다 — 매번, 그리고 설정 · 다른 앱에서 돌아올 때마다 OS 에서 다시 읽는다.
 * - 위치 · 알림을 거절해도 러닝은 막지 않는다. 닫기(X · 뒤로 · 바깥 · 아래로 끌기)는 아무것도 시작하지 않는다.
 * - 러닝 서비스는 여기서 켜지 않는다. [onReady] 가 한 번만 불리고, 부른 쪽이 3-2-1 로 간다.
 */
@Composable
internal fun RunPermissionFlow(onReady: () -> Unit, onCancel: () -> Unit, onHome: () -> Unit) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val prefs = ServiceLocator.userPrefs
    val ready by rememberUpdatedState(onReady)
    val cancel by rememberUpdatedState(onCancel)
    val home by rememberUpdatedState(onHome)
    // 화면 회전 · 앱 복귀에도 보던 안내와 기다리던 결과를 잇는다
    var sheet by rememberSaveable { mutableStateOf<RunPermissionSheet?>(null) }
    var requesting by rememberSaveable { mutableStateOf(false) }
    var inSettings by rememberSaveable { mutableStateOf(false) }
    // 준비 완료는 한 번만 — 콜백이 겹쳐도 러닝을 두 번 준비하지 않는다
    var finished by remember { mutableStateOf(false) }
    val settingsFailed = stringResource(R.string.onb_settings_open_failed)

    suspend fun current(): RunPermissionState = RunPermissionState(
        api = Build.VERSION.SDK_INT,
        activityGranted = StepPermissions.hasActivityRecognition(context),
        preciseLocation = StepPermissions.hasPreciseLocation(context),
        approximateLocation = StepPermissions.hasLocation(context),
        notificationsGranted = StepPermissions.hasNotificationPermission(context),
        activityHistory = prefs.runActivityHistory.first(),
        activityRationale = context.activityRationale(),
        locationChosen = prefs.runLocationChosen.first(),
        notificationChosen = prefs.runNotificationChosen.first(),
    )

    fun apply(step: RunPermissionStep) {
        when (step) {
            is RunPermissionStep.Show -> sheet = step.sheet
            RunPermissionStep.Ready -> if (!finished) {
                finished = true
                sheet = null
                ready()
            }
        }
    }

    fun advance() {
        scope.launch { apply(RunPermissionRules.next(current())) }
    }

    /** 고른 것을 적고(물어봤다 · 골랐다) 다음 차례로 */
    fun choose(mark: suspend () -> Unit) {
        scope.launch {
            mark()
            apply(RunPermissionRules.next(current()))
        }
    }

    val launchActivityRequest = rememberActivityPermissionRequest { outcome ->
        requesting = false
        if (outcome == null) advance() else sheet = outcome
    }
    val locationLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) {
        requesting = false
        scope.launch {
            prefs.setRunLocationChosen()
            val outcome = RunPermissionRules.afterLocationRequest(
                StepPermissions.hasPreciseLocation(context), StepPermissions.hasLocation(context),
            )
            if (outcome == null) apply(RunPermissionRules.next(current())) else sheet = outcome
        }
    }
    val notificationLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) {
        // 허용이든 거절이든 러닝은 이어 간다 — 거절은 오류가 아니다
        requesting = false
        scope.launch {
            prefs.setRunNotificationChosen()
            apply(RunPermissionRules.next(current()))
        }
    }

    // 시스템 권한 창을 띄울 곳이 없으면(드문 기기) 그 자리에 머물지 않고 알맞은 안내로 잇는다
    fun requestActivity() {
        if (requesting) return
        requesting = true
        sheet = null
        launchActivityRequest()
    }

    fun requestLocation() {
        if (requesting) return
        requesting = true
        sheet = null
        // 정확한 위치만 따로 물으면 Android 12+ 가 무시할 수 있다 — 둘을 함께 물어 OS 가 정확 · 대략을 고르게 한다
        try {
            locationLauncher.launch(arrayOf(Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION))
        } catch (_: android.content.ActivityNotFoundException) {
            requesting = false
            sheet = RunPermissionSheet.WithoutLocation
        }
    }

    fun requestNotifications() {
        if (requesting) return
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) {
            advance()
            return
        }
        requesting = true
        sheet = null
        try {
            notificationLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
        } catch (_: android.content.ActivityNotFoundException) {
            requesting = false
            choose { prefs.setRunNotificationChosen() }
        }
    }

    fun openSettings() {
        // 여는 것 자체가 실패하면 그 자리에 남는다 — 휴대폰 설정에서 직접 바꾸게 알린다
        if (ExternalIntents.openAppSettings(context)) inSettings = true
        else Toast.makeText(context, settingsFailed, Toast.LENGTH_LONG).show()
    }

    // 설정 · 다른 앱에서 돌아오면 OS 를 다시 읽는다. 시스템 권한 창의 결과는 위의 콜백이 받는다
    LifecycleEventEffect(Lifecycle.Event.ON_RESUME) {
        if (requesting) return@LifecycleEventEffect
        val showing = sheet
        if (inSettings || showing != null) {
            inSettings = false
            scope.launch { apply(RunPermissionRules.afterReturn(showing, current())) }
        }
    }
    LaunchedEffect(Unit) {
        if (sheet == null && !requesting && !inSettings) apply(RunPermissionRules.next(current()))
    }

    val showing = sheet
    if (showing != null && !requesting) {
        RunPermissionSheetView(
            sheet = showing,
            onPrimary = {
                when (showing) {
                    RunPermissionSheet.ActivityRationale, RunPermissionSheet.ActivityDenied -> requestActivity()
                    RunPermissionSheet.ActivitySettings -> openSettings()
                    RunPermissionSheet.LocationRationale -> requestLocation()
                    // 이대로 계속 — 위치 고르기는 끝났다
                    RunPermissionSheet.ApproximateLocation -> choose { prefs.setRunLocationChosen() }
                    // 설정에서 위치 켜기(L02 의 주 버튼) — 돌아오면 다시 읽는다
                    RunPermissionSheet.WithoutLocation -> openSettings()
                    RunPermissionSheet.NotificationRationale -> requestNotifications()
                }
            },
            onSecondary = {
                when (showing) {
                    // 나중에 — 아직 시작하지 않은 러닝을 거둔다(메뉴로)
                    RunPermissionSheet.ActivityRationale -> cancel()
                    RunPermissionSheet.ActivityDenied, RunPermissionSheet.ActivitySettings -> home()
                    // 경로 없이 계속 — 이미 뜻을 밝혔으니 18 을 다시 보이지 않는다
                    RunPermissionSheet.LocationRationale -> choose { prefs.setRunLocationChosen() }
                    RunPermissionSheet.ApproximateLocation -> openSettings()
                    // 위치 없이 시간만 기록 — 위치 고르기는 끝났다
                    RunPermissionSheet.WithoutLocation -> choose { prefs.setRunLocationChosen() }
                    // 나중에 — 권한을 묻지 않고 러닝으로. 이 선택을 매번 다시 묻지 않는다(설정에서 바꾼다)
                    RunPermissionSheet.NotificationRationale -> choose { prefs.setRunNotificationChosen() }
                }
            },
            // X · 뒤로 · 바깥 · 아래로 끌기 — 준비를 거두고 메뉴로. 닫았는데 러닝이 시작되지 않게 한다
            onDismiss = { cancel() },
        )
    }
}

/**
 * 걸음 권한 시스템 창 하나를 띄우는 함수를 돌려준다. 띄우기 전후의 설명 권유와 남은 이력으로 결과를 가린다 —
 * null 이면 허용, 아니면 다시 물을 수 있음(14) · 설정에서만(15). 이력도 여기서 남긴다.
 * 러닝 권한 안내와 홈의 수동 권한 줄이 함께 쓴다 — 어디서 물었든 같은 이력이 남아 다음 판단이 맞는다.
 */
@Composable
internal fun rememberActivityPermissionRequest(onResult: (RunPermissionSheet?) -> Unit): () -> Unit {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val prefs = ServiceLocator.userPrefs
    val result by rememberUpdatedState(onResult)
    var rationaleBefore by rememberSaveable { mutableStateOf(false) }
    var historyBefore by rememberSaveable { mutableStateOf(ActivityRequestHistory.NEVER) }
    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        val outcome = RunPermissionRules.afterActivityRequest(
            granted || StepPermissions.hasActivityRecognition(context), rationaleBefore, context.activityRationale(), historyBefore,
        )
        scope.launch {
            prefs.setRunActivityHistory(RunPermissionRules.historyAfter(outcome))
            result(outcome)
        }
    }
    return launch@{
        // Android 10 미만은 걸음 권한을 실행 중에 묻지 않는다(설치 때 받음) — 허용으로 본다
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) {
            result(null)
            return@launch
        }
        scope.launch {
            historyBefore = prefs.runActivityHistory.first()
            rationaleBefore = context.activityRationale()
            try {
                launcher.launch(Manifest.permission.ACTIVITY_RECOGNITION)
            } catch (_: android.content.ActivityNotFoundException) {
                result(RunPermissionSheet.ActivitySettings)
            }
        }
    }
}

/** 안내가 놓이는 모양 — 메뉴 위 가운데 창(P01) · 아래 시트(L01) · 화면 전체(P02 · P03 · P04 · P05 · L02) */
private enum class PermLook { Card, Sheet, Page }

/** 한 장면의 글 · 버튼 이름 · 모양 */
private class SheetSpec(
    val tag: String,
    val look: PermLook,
    val title: Int,
    val body: Int,
    val primary: Int,
    val secondary: Int,
    /** 가운데 창의 작은 덧말(P01) · 시트 맨 위 이름(L01) */
    val note: Int? = null,
    /** 주 버튼을 크게(기울인 굵은 글자) — 이 화면에서 가장 큰 행동(L02 · L01) */
    val hero: Boolean = false,
)

private fun specOf(sheet: RunPermissionSheet): SheetSpec = when (sheet) {
    RunPermissionSheet.ActivityRationale -> SheetSpec(
        "activity", PermLook.Card, R.string.run_pm_activity_title, R.string.run_pm_activity_head,
        R.string.run_pm_allow, R.string.run_perm_later, note = R.string.run_pm_activity_note,
    )
    RunPermissionSheet.ActivityDenied -> SheetSpec(
        "activity-denied", PermLook.Page, R.string.run_pm_activity_off, R.string.run_pm_activity_off_body,
        R.string.run_pm_allow_again, R.string.run_go_home,
    )
    RunPermissionSheet.ActivitySettings -> SheetSpec(
        "activity-settings", PermLook.Page, R.string.run_pm_activity_off, R.string.run_pm_activity_settings_body,
        R.string.run_pm_open_settings, R.string.run_go_home,
    )
    RunPermissionSheet.LocationRationale -> SheetSpec(
        "location", PermLook.Sheet, R.string.run_pm_location_head, R.string.run_pm_location_body,
        R.string.run_pm_location_allow, R.string.run_pm_location_later, note = R.string.run_pm_location_sheet, hero = true,
    )
    RunPermissionSheet.ApproximateLocation -> SheetSpec(
        "location-approximate", PermLook.Page, R.string.run_pm_approx_title, R.string.run_pm_approx_body,
        R.string.run_pm_continue, R.string.run_pm_location_settings,
    )
    // 앱 위치 권한 없음(L02) — 주 버튼은 설정에서 위치 켜기, 위치 없이 시간만 기록은 보조
    RunPermissionSheet.WithoutLocation -> SheetSpec(
        "location-off", PermLook.Page, R.string.run_pm_no_location_title, R.string.run_pm_no_location_body,
        R.string.run_pm_turn_on_location, R.string.run_pm_time_only, hero = true,
    )
    RunPermissionSheet.NotificationRationale -> SheetSpec(
        "notification", PermLook.Page, R.string.run_pm_notify_title, R.string.run_pm_notify_body,
        R.string.run_pm_notify_allow, R.string.run_perm_later,
    )
}

/**
 * 권한 안내 하나(시안 P01–P05 · L01 · L02) — 메뉴 위 가운데 창 · 아래 시트 · 화면 전체 중 하나로, 남색 러닝 화면과 같은 틀.
 * 그림 · 큰 한 줄 · 설명 · 주 버튼 · 보조 버튼. OS 권한 창은 주 버튼을 눌러야만 뜬다(그림으로 흉내 내지 않는다).
 * 닫기(창 바깥 · 뒤로 · 시트의 X · 화면의 뒤로)는 [onDismiss] 하나 — 아무것도 시작하지 않는다.
 * 스크린리더는 제목 → 설명 → 주 행동 → 보조 행동 차례로 읽는다. 시안 검사도 이 부품을 그대로 그린다.
 */
@Composable
internal fun RunPermissionSheetView(
    sheet: RunPermissionSheet,
    onPrimary: () -> Unit,
    onSecondary: () -> Unit,
    onDismiss: () -> Unit,
) {
    val spec = specOf(sheet)
    val tag = "perm-sheet-${spec.tag}"
    val buttons: @Composable ColumnScope.() -> Unit = {
        RunButton(
            stringResource(spec.primary), onPrimary, Modifier.testTag("perm-primary"),
            hero = spec.hero && spec.look == PermLook.Page, italic = spec.hero,
        )
        Spacer(Modifier.height(10.dp))
        RunButton(stringResource(spec.secondary), onSecondary, Modifier.testTag("perm-secondary"), kind = RunButtonKind.Secondary)
    }
    when (spec.look) {
        PermLook.Card -> RunDialog(onDismiss = onDismiss, modifier = Modifier.testTag(tag)) {
            RunStateArt(Icons.AutoMirrored.Filled.DirectionsRun, size = 96.dp)
            Spacer(Modifier.height(14.dp))
            PermissionTitle(stringResource(spec.title), size = 28.sp)
            Spacer(Modifier.height(6.dp))
            Text(stringResource(spec.body), style = runTextStyle(19.sp, runTone().text, FontWeight.SemiBold), textAlign = TextAlign.Center)
            spec.note?.let {
                Spacer(Modifier.height(4.dp))
                Text(stringResource(it), style = runTextStyle(15.sp, runTone().label, FontWeight.Medium), textAlign = TextAlign.Center)
            }
            Spacer(Modifier.height(20.dp))
            buttons()
        }
        PermLook.Sheet -> RunSheet(onDismiss = onDismiss, modifier = Modifier.testTag(tag), closeTag = "onboarding-sheet-close") {
            spec.note?.let {
                Text(stringResource(it), style = runTextStyle(17.sp, runTone().text, FontWeight.Bold), textAlign = TextAlign.Center)
                Spacer(Modifier.height(14.dp))
            }
            RunStateArt(Icons.Filled.LocationOn, size = 104.dp)
            Spacer(Modifier.height(14.dp))
            PermissionTitle(stringResource(spec.title), size = 27.sp)
            Spacer(Modifier.height(6.dp))
            Text(stringResource(spec.body), style = runTextStyle(15.sp, runTone().label, FontWeight.Medium), textAlign = TextAlign.Center)
            Spacer(Modifier.height(20.dp))
            buttons()
        }
        PermLook.Page -> androidx.compose.ui.window.Dialog(
            onDismissRequest = onDismiss,
            properties = androidx.compose.ui.window.DialogProperties(
                usePlatformDefaultWidth = false, decorFitsSystemWindows = false, dismissOnClickOutside = false,
            ),
        ) {
            RunPermissionPage(sheet, spec, onDismiss, buttons, Modifier.testTag(tag))
        }
    }
}

/** 화면 전체 안내(P02 · P03 · P04 · P05 · L02) — 남색 바닥 · 뒤로(닫기) · 그림 · 큰 글 · 아래 버튼 */
@Composable
private fun RunPermissionPage(
    sheet: RunPermissionSheet,
    spec: SheetSpec,
    onBack: () -> Unit,
    buttons: @Composable ColumnScope.() -> Unit,
    modifier: Modifier = Modifier,
) {
    val t = runTone()
    RunPage(
        onBack = onBack, modifier = modifier, backTag = "onboarding-sheet-close",
        bottom = { buttons() },
    ) {
        if (sheet == RunPermissionSheet.ApproximateLocation) {
            // P04 — 제목은 왼쪽, 지금 대략적인 자리를 실제 지도에(모르면 지도를 그리지 않는다)
            PermissionTitle(stringResource(spec.title), size = 38.sp, align = TextAlign.Start, italic = true)
            Spacer(Modifier.height(10.dp))
            Text(stringResource(spec.body), style = runTextStyle(17.sp, t.label, FontWeight.Medium, 1.5f))
            Spacer(Modifier.height(18.dp))
            val here = com.stepup.android.ui.components.rememberCurrentLocation(enabled = true)
            RunMapFrame(Modifier.height(300.dp).testTag("perm-approx-map")) {
                if (here != null) {
                    LiveRouteMap(listOf(here), Modifier.fillMaxSize(), follow = true, live = true, routeColor = t.cyan)
                } else {
                    RunMapPlaceholder(Icons.Filled.LocationOn, stringResource(R.string.run_pm_approx_map), null)
                }
            }
            return@RunPage
        }
        if (sheet == RunPermissionSheet.NotificationRationale) {
            // P05 — 제목 · 설명이 위, 알림 그림이 아래
            Spacer(Modifier.height(36.dp))
            PermissionTitle(stringResource(spec.title), size = 50.sp, italic = true)
            Spacer(Modifier.height(10.dp))
            Text(
                stringResource(spec.body), style = runTextStyle(18.sp, t.label, FontWeight.Medium, 1.5f), textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth(),
            )
            Spacer(Modifier.height(36.dp))
            Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) { NotificationArt() }
            return@RunPage
        }
        Spacer(Modifier.height(48.dp))
        Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
            when (sheet) {
                RunPermissionSheet.ActivityDenied -> SlashedArt(Icons.AutoMirrored.Filled.DirectionsRun)
                RunPermissionSheet.ActivitySettings ->
                    RunStateArt(Icons.AutoMirrored.Filled.DirectionsRun, size = 200.dp, badge = Icons.Filled.Close)
                else -> SlashedArt(Icons.Filled.LocationOn, cyan = true)
            }
        }
        Spacer(Modifier.height(34.dp))
        PermissionTitle(stringResource(spec.title), size = 37.sp)
        Spacer(Modifier.height(12.dp))
        Text(
            stringResource(spec.body), style = runTextStyle(18.sp, t.label, FontWeight.Medium, 1.5f), textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth(),
        )
    }
}

@Composable
private fun PermissionTitle(
    text: String,
    size: androidx.compose.ui.unit.TextUnit,
    align: TextAlign = TextAlign.Center,
    italic: Boolean = false,
) {
    val t = runTone()
    Text(
        text,
        style = TextStyle(
            fontFamily = StepUpSans, fontWeight = FontWeight.ExtraBold, fontSize = size, lineHeight = 1.22.em,
            letterSpacing = (-0.03).em, color = t.text,
            fontStyle = if (italic) androidx.compose.ui.text.font.FontStyle.Italic else androidx.compose.ui.text.font.FontStyle.Normal,
        ),
        textAlign = align, modifier = Modifier.fillMaxWidth().semantics { heading() },
    )
}

/** 빗금 그은 그림 — 꺼짐(P02 신체 활동 · L02 위치 권한 · E07 위치 기능). 빗금은 원 안에서 */
@Composable
internal fun SlashedArt(icon: ImageVector, cyan: Boolean = false, size: androidx.compose.ui.unit.Dp = 200.dp) {
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

/** 알림 안내(P05) — 휴대폰 윤곽 가운데에 걸친 알림 한 장(무엇이 보이는지 이름만, 숫자는 그리지 않는다) */
@Composable
private fun NotificationArt() {
    val t = runTone()
    Box(Modifier.size(width = 320.dp, height = 250.dp), contentAlignment = Alignment.Center) {
        Canvas(Modifier.size(width = 200.dp, height = 250.dp)) {
            drawCircle(
                Brush.radialGradient(listOf(t.cobalt.copy(alpha = 0.32f), Color.Transparent), radius = this.size.width * 0.85f),
                radius = this.size.width * 0.85f,
            )
            val radius = androidx.compose.ui.geometry.CornerRadius(30.dp.toPx())
            drawRoundRect(t.inset.copy(alpha = 0.7f), size = this.size, cornerRadius = radius)
            drawRoundRect(t.cobalt.copy(alpha = 0.9f), size = this.size, cornerRadius = radius, style = Stroke(3.dp.toPx()))
            // 위 노치 · 아래 흐린 줄 둘(빈 화면)
            drawRoundRect(
                t.cobalt.copy(alpha = 0.9f), topLeft = androidx.compose.ui.geometry.Offset(this.size.width * 0.32f, 10.dp.toPx()),
                size = androidx.compose.ui.geometry.Size(this.size.width * 0.36f, 12.dp.toPx()),
                cornerRadius = androidx.compose.ui.geometry.CornerRadius(6.dp.toPx()),
            )
            listOf(0.76f, 0.88f).forEach { y ->
                drawRoundRect(
                    t.track, topLeft = androidx.compose.ui.geometry.Offset(this.size.width * 0.14f, this.size.height * y),
                    size = androidx.compose.ui.geometry.Size(this.size.width * 0.72f, 16.dp.toPx()),
                    cornerRadius = androidx.compose.ui.geometry.CornerRadius(8.dp.toPx()),
                )
            }
        }
        Row(
            Modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp))
                .background(t.panel).border(1.5.dp, t.cobalt, RoundedCornerShape(16.dp))
                .padding(horizontal = 14.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            // 알림 큰 아이콘 자리 — 런처와 같은 앱 아이콘(바탕색 + 앞그림, 적응형 아이콘의 보이는 가운데 72/108)
            Box(
                Modifier.size(40.dp).clip(RoundedCornerShape(10.dp))
                    .background(androidx.compose.ui.res.colorResource(R.color.ic_launcher_background)),
                contentAlignment = Alignment.Center,
            ) {
                androidx.compose.foundation.Image(
                    androidx.compose.ui.res.painterResource(R.drawable.ic_launcher_foreground), contentDescription = null,
                    modifier = Modifier.requiredSize(60.dp),
                )
            }
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(stringResource(R.string.run_pm_notify_card), style = runTextStyle(16.sp, t.text, FontWeight.Bold))
                Text(stringResource(R.string.run_pm_notify_card_body), style = runTextStyle(14.sp, t.label, FontWeight.Medium))
            }
            Text(stringResource(R.string.run_pm_notify_now), style = runTextStyle(13.sp, t.label, FontWeight.Medium),
                modifier = Modifier.align(Alignment.Top))
        }
    }
}

/** OS 가 지금 걸음 권한 설명을 권하는가 — 화면(Activity)이 없으면 false */
private fun Context.activityRationale(): Boolean {
    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) return false
    val activity = findActivity() ?: return false
    return ActivityCompat.shouldShowRequestPermissionRationale(activity, Manifest.permission.ACTIVITY_RECOGNITION)
}

private fun Context.findActivity(): Activity? {
    var current: Context? = this
    while (current is ContextWrapper) {
        if (current is Activity) return current
        current = current.baseContext
    }
    return null
}
