package com.stepup.android.ui.components

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import platform.CoreMotion.CMAuthorizationStatusAuthorized
import platform.CoreMotion.CMPedometer
import platform.Foundation.NSDate
import platform.Foundation.dateWithTimeIntervalSinceNow
import platform.UserNotifications.UNAuthorizationOptionAlert
import platform.UserNotifications.UNAuthorizationOptionBadge
import platform.UserNotifications.UNAuthorizationOptionSound
import platform.UserNotifications.UNUserNotificationCenter
import platform.darwin.dispatch_async
import platform.darwin.dispatch_get_main_queue

/** iOS 는 걸음(동작 및 피트니스) · 알림 권한을 실행 중에 묻는다 — 안드로이드 판으로 치면 둘 다 묻는 판 */
actual val permissionApiLevel: Int = 36

/** 걸음 — 처음 걸음 기록을 물으면 iOS 가 "동작 및 피트니스" 창을 띄운다. 답한 뒤 권한을 다시 읽는다 */
@Composable
actual fun rememberActivityPermissionLauncher(onResult: (Boolean) -> Unit): () -> Boolean {
    val callback = rememberUpdatedState(onResult)
    val pedometer = remember { CMPedometer() }
    return remember {
        {
            if (!CMPedometer.isStepCountingAvailable()) {
                callback.value(false)
            } else {
                pedometer.queryPedometerDataFromDate(NSDate.dateWithTimeIntervalSinceNow(-60.0), NSDate()) { _, _ ->
                    dispatch_async(dispatch_get_main_queue()) {
                        callback.value(CMPedometer.authorizationStatus() == CMAuthorizationStatusAuthorized)
                    }
                }
            }
            true
        }
    }
}

@Composable
actual fun rememberNotificationPermissionLauncher(onResult: (Boolean) -> Unit): () -> Boolean {
    val callback = rememberUpdatedState(onResult)
    return remember {
        {
            UNUserNotificationCenter.currentNotificationCenter().requestAuthorizationWithOptions(
                UNAuthorizationOptionAlert or UNAuthorizationOptionSound or UNAuthorizationOptionBadge,
            ) { granted, _ ->
                dispatch_async(dispatch_get_main_queue()) { callback.value(granted) }
            }
            true
        }
    }
}

/** iOS 에는 "설명을 권함"이 없다 — 한 번 거절하면 설정에서만 바꾼다 */
@Composable
actual fun rememberActivityRationale(): () -> Boolean = remember { { false } }
