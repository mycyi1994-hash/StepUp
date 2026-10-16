package com.stepup.android.ui.screens.walk

import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import android.os.Bundle
import android.os.Looper
import android.os.SystemClock
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.GpsFixed
import androidx.compose.material.icons.filled.GpsNotFixed
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.core.location.LocationManagerCompat
import com.google.android.gms.location.LocationCallback
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationResult
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import com.stepup.android.R
import com.stepup.android.core.LocationEngine
import com.stepup.android.domain.GpsFixRule
import com.stepup.android.domain.GpsHint
import com.stepup.android.domain.GpsWait
import com.stepup.android.domain.gpsHintOf
import com.stepup.android.service.isMockLocation
import com.stepup.android.ui.components.RunButton
import com.stepup.android.ui.components.RunButtonKind
import com.stepup.android.ui.components.RunCard
import com.stepup.android.ui.components.RunNotice
import com.stepup.android.ui.components.RunNoticeKind
import com.stepup.android.ui.components.RunPage
import com.stepup.android.ui.components.RunSheet
import com.stepup.android.ui.components.RunSheetText
import com.stepup.android.ui.components.RunStateArt
import com.stepup.android.ui.components.ambientPhase
import com.stepup.android.ui.components.runTextStyle
import com.stepup.android.ui.components.runTone
import com.stepup.android.ui.theme.StepUpSans
import kotlinx.coroutines.delay

/*
 * 시작 전 위치 대기(2026-10-16 사용자 결정 — "GPS 안 되면 못 하는 거야" → "그래 다 되도록 만들어").
 *
 * 권한(정확한 위치)과 위치 기능을 확인한 뒤, 3-2-1 전에 지금 자리를 하나 받을 때까지 기다린다. 시간만 기록하는 길은 없다.
 * 위치는 구글 지도와 같은 엔진(구글 위치 서비스 — GPS · 와이파이 · 기지국)으로 받으니, 실내에서도 보통 1~2초면 잡힌다.
 * 구글 Play 서비스가 없는 휴대폰은 휴대폰 위치(GPS · 와이파이 · 기지국)로 받는다. 받아들이는 기준은 [GpsFixRule].
 */

/**
 * [enabled] 동안 위치를 받으며 기다린다 — 끄거나 화면을 떠나면 바로 끊는다. 정확한 위치 권한이 있을 때만 켠다(부르는 쪽이 확인한다).
 * 휴대폰이 이미 알던 새 위치([GpsFixRule.FRESH_MS] 안)가 있으면 기다리지 않는다.
 */
@Composable
internal fun rememberGpsWait(enabled: Boolean): GpsWait {
    val context = LocalContext.current
    val manager = remember { ContextCompat.getSystemService(context, LocationManager::class.java) }
    var wait by remember { mutableStateOf(GpsWait()) }

    DisposableEffect(enabled) {
        if (!enabled) {
            wait = GpsWait()
            return@DisposableEffect onDispose { }
        }
        val lm = manager
        wait = GpsWait(locationOn = lm?.locationOn() ?: true)
        // 끊은 뒤에 늦게 온 위치(구글 위치 서비스의 마지막 위치 등)는 버린다
        var listening = true

        fun take(location: Location) {
            if (!listening) return
            val mock = isMockLocation(location)
            val ageMs = (SystemClock.elapsedRealtimeNanos() - location.elapsedRealtimeNanos) / 1_000_000
            val accuracy = if (location.hasAccuracy()) location.accuracy else null
            wait = wait.copy(
                fixed = wait.fixed || GpsFixRule.accepts(mock, ageMs),
                // 가짜 위치의 정확도는 보이지 않는다 — 받아들이지 않는 값이다
                accuracyM = if (mock) wait.accuracyM else accuracy ?: wait.accuracyM,
                mock = mock,
            )
        }

        // 1) 구글 위치 서비스 — 구글 지도와 같은 엔진
        val fused = if (LocationEngine.fusedAvailable(context)) LocationServices.getFusedLocationProviderClient(context) else null
        val fusedCallback = object : LocationCallback() {
            override fun onLocationResult(result: LocationResult) {
                result.locations.forEach(::take)
            }
        }
        val fusedOn = fused != null && try {
            fused.lastLocation.addOnSuccessListener { last -> last?.let(::take) }
            fused.requestLocationUpdates(
                LocationRequest.Builder(Priority.PRIORITY_HIGH_ACCURACY, 1_000L).build(), fusedCallback, Looper.getMainLooper(),
            )
            true
        } catch (_: SecurityException) {
            false
        }

        // 2) Play 서비스가 없으면 휴대폰 위치 — GPS 와 와이파이 · 기지국 둘 다(먼저 오는 쪽)
        // API 29 이하에서는 넷 모두 추상 메서드다 — 하나라도 빠지면 위치를 켜고 끌 때 앱이 죽는다
        val listener = object : LocationListener {
            override fun onLocationChanged(location: Location) = take(location)
            override fun onProviderEnabled(provider: String) {
                if (listening) wait = wait.copy(locationOn = lm?.locationOn() ?: true)
            }
            override fun onProviderDisabled(provider: String) {
                if (listening) wait = wait.copy(locationOn = lm?.locationOn() ?: true)
            }

            @Deprecated("Deprecated in Java")
            override fun onStatusChanged(provider: String?, status: Int, extras: Bundle?) = Unit
        }
        var managerOn = false
        if (!fusedOn && lm != null) {
            val providers = runCatching { lm.allProviders }.getOrDefault(emptyList())
            for (provider in listOf(LocationManager.GPS_PROVIDER, LocationManager.NETWORK_PROVIDER)) {
                if (provider !in providers) continue
                try {
                    lm.getLastKnownLocation(provider)?.let(::take)
                    lm.requestLocationUpdates(provider, 1_000L, 0f, listener, Looper.getMainLooper())
                    managerOn = true
                } catch (_: SecurityException) {
                    // 그 사이 권한을 거뒀다 — 부르는 쪽이 돌아올 때 다시 확인한다
                } catch (_: IllegalArgumentException) {
                }
            }
        }

        onDispose {
            listening = false
            if (fusedOn) runCatching { fused?.removeLocationUpdates(fusedCallback) }
            if (managerOn) runCatching { lm?.removeUpdates(listener) }
        }
    }

    // 기다린 시간 · 위치 기능(설정에서 끄고 켠 것)을 1초마다 다시 읽는다
    LaunchedEffect(enabled) {
        if (!enabled) return@LaunchedEffect
        val started = SystemClock.elapsedRealtime()
        while (true) {
            delay(1_000)
            wait = wait.copy(
                elapsedSec = ((SystemClock.elapsedRealtime() - started) / 1_000).toInt(),
                locationOn = manager?.locationOn() ?: true,
            )
        }
    }
    return wait
}

private fun LocationManager.locationOn(): Boolean =
    runCatching { LocationManagerCompat.isLocationEnabled(this) }.getOrDefault(true)

/**
 * 위치 대기 화면(혼자 러닝 · 자유 러닝 · 챌린지 · 다이어트) — 권한 · 위치 기능 다음, 3-2-1 전.
 * 그림 · 큰 한 줄 · 설명 · 정확도 · 기다린 시간 · (오래 걸리면) 할 일. 잡히면 부른 쪽이 바로 3-2-1 로 간다.
 * 주 버튼은 없다(기다리는 것이 할 일) — 아래는 취소 하나. 위치 기능이 꺼진 경우만 안내 띠에 "위치 설정 열기".
 */
@Composable
internal fun RunGpsGateContent(
    title: String,
    wait: GpsWait,
    onBack: () -> Unit,
    onOpenLocationSettings: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val t = runTone()
    RunPage(
        onBack = onBack, modifier = modifier.testTag("run-gps-gate"), title = title.ifBlank { null },
        bottom = {
            RunButton(
                stringResource(R.string.common_cancel), onBack, Modifier.testTag("run-gps-gate-cancel"),
                kind = RunButtonKind.Secondary,
            )
        },
    ) {
        Spacer(Modifier.height(16.dp))
        Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) { GpsSearchArt(wait, 168.dp) }
        Spacer(Modifier.height(22.dp))
        Text(
            gpsHeadline(wait),
            style = TextStyle(
                fontFamily = StepUpSans, fontWeight = FontWeight.ExtraBold, fontSize = 28.sp, lineHeight = 1.25.em,
                letterSpacing = (-0.03).em, color = t.text,
            ),
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth().semantics { heading() }.testTag("run-gps-gate-title"),
        )
        Spacer(Modifier.height(8.dp))
        Text(
            stringResource(R.string.run_gps_wait_body), style = runTextStyle(16.sp, t.label, FontWeight.Medium, 1.5f),
            textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth(),
        )
        Spacer(Modifier.height(20.dp))
        GpsWaitDetails(wait, onOpenLocationSettings)
    }
}

/**
 * 크루 · 번개 대기실의 위치 확인(준비 전) — 잡히면 부른 쪽이 준비 완료로 바꾸고 닫는다. 닫기 · 취소는 준비 전으로 남는다.
 */
@Composable
internal fun RunGpsWaitSheet(wait: GpsWait, onDismiss: () -> Unit, onOpenLocationSettings: () -> Unit) {
    RunSheet(onDismiss = onDismiss, modifier = Modifier.testTag("run-gps-wait-sheet")) {
        GpsWaitSheetBody(wait, onDismiss, onOpenLocationSettings)
    }
}

/** [RunGpsWaitSheet] 의 안 — 시안 검사가 창 없이 그린다 */
@Composable
internal fun ColumnScope.GpsWaitSheetBody(wait: GpsWait, onCancel: () -> Unit, onOpenLocationSettings: () -> Unit) {
    GpsSearchArt(wait, 104.dp)
    Spacer(Modifier.height(12.dp))
    RunSheetText(gpsHeadline(wait), body = stringResource(R.string.run_gps_lobby_body))
    Spacer(Modifier.height(16.dp))
    GpsWaitDetails(wait, onOpenLocationSettings)
    Spacer(Modifier.height(16.dp))
    RunButton(
        stringResource(R.string.common_cancel), onCancel, Modifier.testTag("run-gps-wait-cancel"), kind = RunButtonKind.Secondary,
    )
}

@Composable
private fun gpsHeadline(wait: GpsWait): String =
    stringResource(if (wait.fixed) R.string.run_gps_wait_found else R.string.run_gps_wait_title)

/** 찾는 중이면 위치 표시 둘레로 퍼지는 고리(동작 줄이기에서는 멈춘 고리), 잡히면 채운 표시 */
@Composable
private fun GpsSearchArt(wait: GpsWait, size: Dp) {
    val t = runTone()
    val phase = if (!wait.fixed) ambientPhase(1_800) else null
    Box(
        Modifier.size(size).drawBehind {
            val p = phase?.value ?: return@drawBehind
            val base = this.size.minDimension / 2.25f
            drawCircle(
                t.cyan.copy(alpha = 0.55f * (1f - p)), radius = base * (1f + 0.32f * p), style = Stroke(width = 2.dp.toPx()),
            )
        },
        contentAlignment = Alignment.Center,
    ) {
        RunStateArt(if (wait.fixed) Icons.Filled.GpsFixed else Icons.Filled.GpsNotFixed, size = size)
    }
}

/** 정확도 · 기다린 시간 한 판과, 할 일이 있으면 그 아래 안내 띠 */
@Composable
private fun ColumnScope.GpsWaitDetails(wait: GpsWait, onOpenLocationSettings: () -> Unit) {
    RunCard(padding = androidx.compose.foundation.layout.PaddingValues(horizontal = 16.dp, vertical = 12.dp), tag = "run-gps-status") {
        GpsStatusRow(
            stringResource(R.string.run_gps_wait_accuracy),
            wait.accuracyM?.let { stringResource(R.string.run_gps_wait_accuracy_value, Math.round(it)) } ?: "—",
            "run-gps-accuracy",
        )
        GpsStatusRow(
            stringResource(R.string.run_gps_wait_time), "%d:%02d".format(wait.elapsedSec / 60, wait.elapsedSec % 60), "run-gps-elapsed",
        )
    }
    val hint = gpsHintOf(wait)
    if (hint != GpsHint.NONE) {
        Spacer(Modifier.height(12.dp))
        val (title, body) = when (hint) {
            GpsHint.LOCATION_OFF -> R.string.run_loc_off_title to R.string.run_loc_off_body
            GpsHint.MOCK -> R.string.run_gps_hint_mock_title to R.string.run_gps_hint_mock_body
            GpsHint.NO_FIX, GpsHint.NONE -> R.string.run_gps_hint_no_fix_title to R.string.run_gps_hint_no_fix_body
        }
        val settings = hint == GpsHint.LOCATION_OFF
        RunNotice(
            stringResource(title),
            body = stringResource(body),
            kind = if (hint == GpsHint.MOCK) RunNoticeKind.Error else RunNoticeKind.Warn,
            action = if (settings) stringResource(R.string.run_loc_off_open) else null,
            onAction = if (settings) onOpenLocationSettings else null,
            tag = "run-gps-hint-${hint.name.lowercase()}",
        )
    }
}

@Composable
private fun GpsStatusRow(label: String, value: String, tag: String) {
    val t = runTone()
    Row(Modifier.fillMaxWidth().padding(vertical = 6.dp), verticalAlignment = Alignment.CenterVertically) {
        Text(label, style = runTextStyle(15.sp, t.label, FontWeight.Medium), modifier = Modifier.weight(1f))
        Text(value, style = runTextStyle(17.sp, t.text, FontWeight.Bold), textAlign = TextAlign.End, modifier = Modifier.testTag(tag))
    }
}
