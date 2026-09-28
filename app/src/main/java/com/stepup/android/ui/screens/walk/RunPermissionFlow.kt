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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.DirectionsRun
import androidx.compose.material.icons.outlined.LocationOn
import androidx.compose.material.icons.outlined.Notifications
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
import com.stepup.android.ui.components.OnboardingPrimaryButton
import com.stepup.android.ui.components.OnboardingSheet
import com.stepup.android.ui.components.OnboardingSheetBody
import com.stepup.android.ui.components.OnboardingTextButton
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
                    // 이대로 계속 · 경로 없이 계속 — 위치 고르기는 끝났다
                    RunPermissionSheet.ApproximateLocation, RunPermissionSheet.WithoutLocation -> choose { prefs.setRunLocationChosen() }
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
                    RunPermissionSheet.ApproximateLocation, RunPermissionSheet.WithoutLocation -> openSettings()
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

/** 한 장면의 글 · 그림 · 버튼 이름 */
private class SheetSpec(
    val tag: String,
    val title: Int,
    val icon: ImageVector,
    val headline: Int,
    val body: Int,
    val primary: Int,
    val secondary: Int,
)

private fun specOf(sheet: RunPermissionSheet): SheetSpec = when (sheet) {
    RunPermissionSheet.ActivityRationale -> SheetSpec(
        "activity", R.string.onb_perm_activity_title, Icons.AutoMirrored.Outlined.DirectionsRun,
        R.string.onb_perm_activity_headline, R.string.onb_perm_activity_body, R.string.perm_allow, R.string.run_perm_later,
    )
    RunPermissionSheet.ActivityDenied -> SheetSpec(
        "activity-denied", R.string.onb_perm_activity_off_title, Icons.AutoMirrored.Outlined.DirectionsRun,
        R.string.onb_perm_activity_off_headline, R.string.onb_perm_activity_denied_body, R.string.onb_perm_allow_again,
        R.string.onb_perm_home,
    )
    RunPermissionSheet.ActivitySettings -> SheetSpec(
        "activity-settings", R.string.onb_perm_activity_off_title, Icons.AutoMirrored.Outlined.DirectionsRun,
        R.string.onb_perm_activity_off_headline, R.string.onb_perm_activity_settings_body, R.string.cd_open_settings,
        R.string.onb_perm_home,
    )
    RunPermissionSheet.LocationRationale -> SheetSpec(
        "location", R.string.onb_perm_location_title, Icons.Outlined.LocationOn,
        R.string.onb_perm_location_headline, R.string.onb_perm_location_body, R.string.onb_perm_location_allow,
        R.string.onb_perm_without_route,
    )
    RunPermissionSheet.ApproximateLocation -> SheetSpec(
        "location-approximate", R.string.onb_perm_approx_title, Icons.Outlined.LocationOn,
        R.string.onb_perm_approx_headline, R.string.onb_perm_approx_body, R.string.onb_perm_continue_as_is,
        R.string.onb_perm_location_settings,
    )
    RunPermissionSheet.WithoutLocation -> SheetSpec(
        "location-off", R.string.onb_perm_no_location_title, Icons.Outlined.LocationOn,
        R.string.onb_perm_no_location_headline, R.string.onb_perm_no_location_body, R.string.onb_perm_without_route,
        R.string.onb_perm_location_settings,
    )
    RunPermissionSheet.NotificationRationale -> SheetSpec(
        "notification", R.string.onb_perm_notification_title, Icons.Outlined.Notifications,
        R.string.onb_perm_notification_headline, R.string.onb_perm_notification_body, R.string.onb_perm_notification_allow,
        R.string.run_perm_later,
    )
}

/**
 * 권한 안내 시트 하나 — 제목 · 닫기, 아이콘 칸, 큰 한 줄, 설명, 주 버튼, 글자 보조 버튼.
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
    OnboardingSheet(
        title = stringResource(spec.title),
        onDismiss = onDismiss,
        modifier = Modifier.testTag("perm-sheet-${spec.tag}"),
        actions = {
            OnboardingPrimaryButton(stringResource(spec.primary), onPrimary, Modifier.fillMaxWidth().testTag("perm-primary"))
            OnboardingTextButton(stringResource(spec.secondary), onSecondary, Modifier.fillMaxWidth().testTag("perm-secondary"))
        },
    ) {
        OnboardingSheetBody(spec.icon, stringResource(spec.headline), stringResource(spec.body))
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
