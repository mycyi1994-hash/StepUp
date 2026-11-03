package com.stepup.android.core

import android.Manifest
import android.app.LocaleManager
import android.content.Context
import android.content.pm.PackageManager
import android.content.res.Configuration
import android.os.Build
import android.os.LocaleList
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import java.util.Locale

/**
 * 언어 적용(안드로이드 — 예전 app/core/AppLocale.change 그대로). 안드로이드 13 이상은 OS 가 앱 언어를 바꾸고 화면을 다시 만든다(false),
 * 그 아래는 프로세스 언어를 바꾸고 부르는 쪽이 화면을 다시 만든다(true).
 */
actual fun applyAppLanguage(tag: String): Boolean {
    val context = AndroidHost.app
    return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        context.getSystemService(LocaleManager::class.java)?.applicationLocales =
            if (tag.isEmpty()) LocaleList.getEmptyLocaleList() else LocaleList.forLanguageTags(tag)
        false
    } else {
        applyLanguageToProcess(context, tag)
        true
    }
}

/**
 * 프로세스 기본 언어(Locale · LocaleList 기본값)를 앱 언어로 — 공용 화면의 글자(Compose 리소스)는 이 값(Compose Locale.current)으로
 * 언어를 고른다. 안드로이드 13+ 의 앱 언어(LocaleManager)는 화면 리소스만 바꾸고 이 값을 바로 바꾸지 않아, 화면이 기기 언어로 남았다.
 * 빈 태그면 기기 언어.
 */
fun setProcessLocale(tag: String) {
    val locale = if (tag.isEmpty()) systemLocale() else Locale.forLanguageTag(tag)
    Locale.setDefault(locale)
    LocaleList.setDefault(LocaleList(locale))
}

/** 안드로이드 13 미만 — 프로세스 기본 언어와 앱 리소스 언어를 바꾼다(앱이 켜질 때 · 바꿀 때) */
@Suppress("DEPRECATION")
fun applyLanguageToProcess(context: Context, tag: String) {
    val locale = if (tag.isEmpty()) systemLocale() else Locale.forLanguageTag(tag)
    setProcessLocale(tag)
    val resources = context.applicationContext.resources
    val config = Configuration(resources.configuration)
    config.setLocale(locale)
    config.setLocales(LocaleList(locale))
    resources.updateConfiguration(config, resources.displayMetrics)
}

/** 기기 로케일 — Locale.getDefault 는 앱이 덮어썼을 수 있어 시스템 리소스에서 읽는다 */
fun systemLocale(): Locale {
    val locales = android.content.res.Resources.getSystem().configuration.locales
    return if (locales.size() > 0) locales.get(0) else Locale.ENGLISH
}

private fun granted(permission: String): Boolean =
    ContextCompat.checkSelfPermission(AndroidHost.app, permission) == PackageManager.PERMISSION_GRANTED

actual fun readPermissionSnapshot(): PermissionState {
    val fine = granted(Manifest.permission.ACCESS_FINE_LOCATION)
    return PermissionState(
        // 걸음 권한(29 부터) · 알림 권한(33 부터) — 그 아래 판은 실행 중에 묻지 않아 늘 허용
        activity = Build.VERSION.SDK_INT < Build.VERSION_CODES.Q || granted(Manifest.permission.ACTIVITY_RECOGNITION),
        preciseLocation = fine,
        approximateLocation = fine || granted(Manifest.permission.ACCESS_COARSE_LOCATION),
        notifications = Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU || granted(Manifest.permission.POST_NOTIFICATIONS),
    )
}

actual fun hasNetwork(): Boolean {
    val manager = AndroidHost.app.getSystemService(android.net.ConnectivityManager::class.java) ?: return true
    val capabilities = manager.getNetworkCapabilities(manager.activeNetwork) ?: return false
    return capabilities.hasCapability(android.net.NetworkCapabilities.NET_CAPABILITY_INTERNET)
}

actual fun locationServicesOn(): Boolean {
    val manager = AndroidHost.app.getSystemService(android.location.LocationManager::class.java) ?: return true
    return androidx.core.location.LocationManagerCompat.isLocationEnabled(manager)
}

actual object LocalFlags {
    actual fun getInt(file: String, key: String, default: Int): Int =
        AndroidHost.app.getSharedPreferences(file, Context.MODE_PRIVATE).getInt(key, default)

    actual fun putInt(file: String, key: String, value: Int) {
        AndroidHost.app.getSharedPreferences(file, Context.MODE_PRIVATE).edit().putInt(key, value).apply()
    }
}

/** 휴대폰(OS)이 이 앱의 알림을 허락했는가 — 휴대폰 설정에서 돌아올 때마다 다시 본다 */
@Composable
actual fun rememberNotificationsAllowed(): Boolean {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    var allowed by remember(context) { mutableStateOf(NotificationManagerCompat.from(context).areNotificationsEnabled()) }
    DisposableEffect(lifecycleOwner, context) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) allowed = NotificationManagerCompat.from(context).areNotificationsEnabled()
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }
    return allowed
}
