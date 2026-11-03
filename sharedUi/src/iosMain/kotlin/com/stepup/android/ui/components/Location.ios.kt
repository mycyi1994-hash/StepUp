@file:OptIn(kotlinx.cinterop.ExperimentalForeignApi::class)

package com.stepup.android.ui.components

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import com.stepup.android.domain.GeoPoint
import kotlinx.cinterop.useContents
import platform.CoreLocation.CLLocation
import platform.CoreLocation.CLLocationManager
import platform.CoreLocation.CLLocationManagerDelegateProtocol
import platform.CoreLocation.kCLAuthorizationStatusAuthorizedAlways
import platform.CoreLocation.kCLAuthorizationStatusAuthorizedWhenInUse
import platform.CoreLocation.kCLLocationAccuracyBest
import platform.darwin.NSObject

@Composable
actual fun SystemBackHandler(enabled: Boolean, onBack: () -> Unit) = Unit

private fun CLLocation.toGeo(): GeoPoint = coordinate.useContents { GeoPoint(latitude, longitude) }

@Composable
actual fun rememberCurrentLocation(enabled: Boolean): GeoPoint? {
    var here by remember { mutableStateOf<GeoPoint?>(null) }
    DisposableEffect(enabled) {
        if (!enabled) {
            here = null
            return@DisposableEffect onDispose { }
        }
        val manager = CLLocationManager()
        val allowed = manager.authorizationStatus.let {
            it == kCLAuthorizationStatusAuthorizedWhenInUse || it == kCLAuthorizationStatusAuthorizedAlways
        }
        if (!allowed) return@DisposableEffect onDispose { }
        manager.location?.let { here = it.toGeo() }
        val delegate = object : NSObject(), CLLocationManagerDelegateProtocol {
            override fun locationManager(manager: CLLocationManager, didUpdateLocations: List<*>) {
                didUpdateLocations.filterIsInstance<CLLocation>().lastOrNull()?.let { here = it.toGeo() }
            }
        }
        manager.delegate = delegate
        manager.desiredAccuracy = kCLLocationAccuracyBest
        manager.startUpdatingLocation()
        onDispose {
            manager.stopUpdatingLocation()
            manager.delegate = null
        }
    }
    return here
}
