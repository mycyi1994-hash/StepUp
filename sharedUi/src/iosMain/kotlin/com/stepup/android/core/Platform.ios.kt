package com.stepup.android.core

import com.stepup.android.domain.GeoPoint
import com.stepup.android.domain.urlEncodeUtf8
import platform.Foundation.NSURL
import platform.Foundation.NSUserDefaults
import platform.UIKit.UIApplication
import platform.UIKit.UIApplicationOpenSettingsURLString

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
}
