package com.stepup.android.core

import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue

import com.stepup.android.domain.GeoPoint
import com.stepup.android.domain.urlEncodeUtf8
import platform.Foundation.NSURL
import platform.Foundation.NSUserDefaults
import platform.UIKit.UIApplication
import platform.UIKit.UIApplicationOpenSettingsURLString
import platform.UIKit.popoverPresentationController
import platform.Foundation.dateWithTimeIntervalSince1970

internal actual fun applyAppLanguage(tag: String): Boolean {
    val defaults = NSUserDefaults.standardUserDefaults
    if (tag.isEmpty()) defaults.removeObjectForKey("AppleLanguages") else defaults.setObject(listOf(tag), forKey = "AppleLanguages")
    return true
}

actual object ExternalIntents {
    private fun open(url: String): Boolean {
        val u = NSURL.URLWithString(url) ?: return false
        val app = UIApplication.sharedApplication
        if (!app.canOpenURL(u)) return false
        app.openURL(u, options = emptyMap<Any?, Any>(), completionHandler = null)
        return true
    }

    actual fun openAppSettings(): Boolean = open(UIApplicationOpenSettingsURLString)

    /** iOS 는 위치 서비스 설정으로 바로 가는 공개 주소가 없다 — 이 앱 설정(위치 권한 줄이 있다) */
    actual fun openLocationSettings(): Boolean = openAppSettings()

    actual fun openPlaceInMaps(place: String) {
        open("https://maps.apple.com/?q=" + urlEncodeUtf8(place).replace("+", "%20"))
    }

    actual fun openRouteInMaps(points: List<GeoPoint>) {
        val start = points.firstOrNull() ?: return
        val end = points.last()
        open("https://maps.apple.com/?saddr=${start.lat},${start.lng}&daddr=${end.lat},${end.lng}&dirflg=w")
    }

    actual fun openCoordinateInMaps(point: GeoPoint) {
        open("https://maps.apple.com/?ll=${point.lat},${point.lng}&q=${point.lat},${point.lng}")
    }

    actual fun openUrl(url: String) {
        open(url)
    }

    actual fun tryOpenUrl(url: String): Boolean = open(url)

    private fun top(): platform.UIKit.UIViewController? {
        var top = UIApplication.sharedApplication.keyWindow?.rootViewController
        while (top?.presentedViewController != null) top = top.presentedViewController
        return top
    }

    actual fun copyText(text: String) {
        platform.UIKit.UIPasteboard.generalPasteboard.string = text
    }

    /** 공유 시트(글 · 그림) — 맨 위 화면에 띄운다 */
    fun share(items: List<Any>): Boolean {
        val vc = top() ?: return false
        val sheet = platform.UIKit.UIActivityViewController(activityItems = items, applicationActivities = null)
        sheet.popoverPresentationController?.sourceView = vc.view
        vc.presentViewController(sheet, animated = true, completion = null)
        return true
    }

    actual fun shareText(text: String, title: String?): Boolean {
        val vc = top() ?: return false
        val sheet = platform.UIKit.UIActivityViewController(activityItems = listOf(text), applicationActivities = null)
        // 아이패드는 떠 있을 자리가 있어야 한다
        sheet.popoverPresentationController?.sourceView = vc.view
        vc.presentViewController(sheet, animated = true, completion = null)
        return true
    }

    private val eventStore by lazy { platform.EventKit.EKEventStore() }
    private val calendarDelegate by lazy {
        object : platform.darwin.NSObject(), platform.EventKitUI.EKEventEditViewDelegateProtocol {
            override fun eventEditViewController(controller: platform.EventKitUI.EKEventEditViewController, didCompleteWithAction: platform.EventKitUI.EKEventEditViewAction) {
                controller.dismissViewControllerAnimated(true, completion = null)
            }
        }
    }

    actual fun addToCalendar(title: String, place: String, beginMs: Long, endMs: Long): Boolean {
        val vc = top() ?: return false
        val event = platform.EventKit.EKEvent.eventWithEventStore(eventStore).apply {
            this.title = title
            location = place
            startDate = platform.Foundation.NSDate.dateWithTimeIntervalSince1970(beginMs / 1000.0)
            endDate = platform.Foundation.NSDate.dateWithTimeIntervalSince1970(endMs / 1000.0)
        }
        val editor = platform.EventKitUI.EKEventEditViewController().apply {
            this.eventStore = this@ExternalIntents.eventStore
            this.event = event
            editViewDelegate = calendarDelegate
        }
        vc.presentViewController(editor, animated = true, completion = null)
        return true
    }

    actual fun composeEmail(address: String, subject: String): Boolean =
        open("mailto:$address?subject=" + urlEncodeUtf8(subject).replace("+", "%20"))
}

@androidx.compose.runtime.Composable
actual fun rememberNotificationsAllowed(): Boolean {
    var allowed by androidx.compose.runtime.remember { androidx.compose.runtime.mutableStateOf(true) }
    val lifecycle = androidx.lifecycle.compose.LocalLifecycleOwner.current
    androidx.compose.runtime.DisposableEffect(lifecycle) {
        fun check() {
            platform.UserNotifications.UNUserNotificationCenter.currentNotificationCenter().getNotificationSettingsWithCompletionHandler { s ->
                val status = s?.authorizationStatus
                // 아직 묻지 않았으면 막힌 것으로 보지 않는다(처음 알림을 켤 때 시스템이 묻는다)
                allowed = status != platform.UserNotifications.UNAuthorizationStatusDenied
            }
        }
        check()
        val observer = androidx.lifecycle.LifecycleEventObserver { _, e -> if (e == androidx.lifecycle.Lifecycle.Event.ON_RESUME) check() }
        lifecycle.lifecycle.addObserver(observer)
        onDispose { lifecycle.lifecycle.removeObserver(observer) }
    }
    return allowed
}


/** 알림 허락 — 시스템이 비동기로 답해서 마지막 값을 들고 있다(처음은 묻기 전 = 허락 안 함으로 보지 않는다) */
private var notificationsAllowedCache = true

private fun refreshNotificationCache() {
    platform.UserNotifications.UNUserNotificationCenter.currentNotificationCenter().getNotificationSettingsWithCompletionHandler { s ->
        notificationsAllowedCache = s?.authorizationStatus == platform.UserNotifications.UNAuthorizationStatusAuthorized ||
            s?.authorizationStatus == platform.UserNotifications.UNAuthorizationStatusProvisional
    }
}

actual fun readPermissionSnapshot(): PermissionState {
    refreshNotificationCache()
    val location = platform.CoreLocation.CLLocationManager()
    val status = location.authorizationStatus
    val located = status == platform.CoreLocation.kCLAuthorizationStatusAuthorizedWhenInUse ||
        status == platform.CoreLocation.kCLAuthorizationStatusAuthorizedAlways
    val precise = located && location.accuracyAuthorization == platform.CoreLocation.CLAccuracyAuthorization.CLAccuracyAuthorizationFullAccuracy
    return PermissionState(
        activity = platform.CoreMotion.CMPedometer.authorizationStatus() == platform.CoreMotion.CMAuthorizationStatusAuthorized,
        preciseLocation = precise,
        approximateLocation = located,
        notifications = notificationsAllowedCache,
    )
}

/** iOS 는 연결 상태를 바로 물을 수 있는 동기 API 가 없다 — 보내 보고 답으로 안다(답을 못 받으면 Unknown 으로 안내) */
actual fun hasNetwork(): Boolean = true

actual object LocalFlags {
    actual fun getInt(file: String, key: String, default: Int): Int {
        val d = NSUserDefaults.standardUserDefaults
        val k = "$file.$key"
        return if (d.objectForKey(k) == null) default else d.integerForKey(k).toInt()
    }

    actual fun putInt(file: String, key: String, value: Int) {
        NSUserDefaults.standardUserDefaults.setInteger(value.toLong(), forKey = "$file.$key")
    }
}

actual fun locationServicesOn(): Boolean = platform.CoreLocation.CLLocationManager.locationServicesEnabled()
