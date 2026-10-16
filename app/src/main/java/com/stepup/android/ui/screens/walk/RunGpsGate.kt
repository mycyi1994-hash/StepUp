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
import androidx.compose.material.icons.filled.GpsOff
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
import androidx.core.location.GnssStatusCompat
import androidx.core.location.LocationManagerCompat
import com.stepup.android.R
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
 * 시작 전 GPS 대기(2026-10-16 사용자 결정 — "GPS 안 되면 못 하는 거야").
 *
 * 권한(정확한 위치)과 위치 기능을 확인한 뒤, 3-2-1 전에 실제 GPS 위치를 하나 받을 때까지 기다린다. 시간만 기록하는 길은 없다.
 * 기다리는 동안 휴대폰이 알려 주는 위성 수 · 정확도 · 기다린 시간을 그대로 보이고, 오래 걸리면 까닭과 할 일을 알린다
 * (실내 · 유심 없는 휴대폰의 첫 위치 · 가짜 위치 앱 · Android 8 이하 절전 위치 방식). 받아들이는 기준은 [GpsFixRule].
 */

/**
 * [enabled] 동안 GPS 를 켜고 기다린다 — 끄거나 화면을 떠나면 바로 끊는다. 정확한 위치 권한이 있을 때만 켠다(부르는 쪽이 확인한다).
 * 휴대폰이 이미 알던 새 GPS 위치([GpsFixRule.FRESH_MS] 안)가 있으면 기다리지 않는다.
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
        val providers = runCatching { lm?.allProviders }.getOrNull().orEmpty()
        if (lm == null || LocationManager.GPS_PROVIDER !in providers) {
            wait = GpsWait(hasGps = false)
            return@DisposableEffect onDispose { }
        }
        wait = GpsWait(locationOn = lm.locationOn(), gpsOn = lm.gpsOn())

        fun take(location: Location) {
            val mock = isMockLocation(location)
            val ageMs = (SystemClock.elapsedRealtimeNanos() - location.elapsedRealtimeNanos) / 1_000_000
            val accuracy = if (location.hasAccuracy()) location.accuracy else null
            val ok = GpsFixRule.accepts(location.provider == LocationManager.GPS_PROVIDER, mock, accuracy, ageMs)
            wait = wait.copy(
                fixed = wait.fixed || ok,
                // 가짜 위치의 정확도는 보이지 않는다 — 받아들이지 않는 값이다
                accuracyM = if (mock) wait.accuracyM else accuracy ?: wait.accuracyM,
                mock = mock,
            )
        }

        // API 29 이하에서는 넷 모두 추상 메서드다 — 하나라도 빠지면 위치를 켜고 끌 때 앱이 죽는다
        val listener = object : LocationListener {
            override fun onLocationChanged(location: Location) = take(location)
            override fun onProviderEnabled(provider: String) {
                wait = wait.copy(gpsOn = lm.gpsOn(), locationOn = lm.locationOn())
            }
            override fun onProviderDisabled(provider: String) {
                wait = wait.copy(gpsOn = lm.gpsOn(), locationOn = lm.locationOn())
            }

            @Deprecated("Deprecated in Java")
            override fun onStatusChanged(provider: String?, status: Int, extras: Bundle?) = Unit
        }
        // 위성 수 — 신호를 받는 위성(세기 > 0)과 위치 계산에 쓰는 위성. GPS 를 켠 동안에만 온다
        val satellites = object : GnssStatusCompat.Callback() {
            override fun onSatelliteStatusChanged(status: GnssStatusCompat) {
                var seen = 0
                var used = 0
                for (i in 0 until status.satelliteCount) {
                    if (status.getCn0DbHz(i) > 0f) seen++
                    if (status.usedInFix(i)) used++
                }
                wait = wait.copy(satellitesSeen = seen, satellitesUsed = used)
            }
        }

        try {
            lm.getLastKnownLocation(LocationManager.GPS_PROVIDER)?.let(::take)
        } catch (_: SecurityException) {
        } catch (_: IllegalArgumentException) {
        }
        try {
            // 1초 · 0m — 첫 위치를 받는 대로 알고, 정확도가 좋아지는 것을 바로 보인다
            lm.requestLocationUpdates(LocationManager.GPS_PROVIDER, 1_000L, 0f, listener, Looper.getMainLooper())
        } catch (_: SecurityException) {
            // 그 사이 권한을 거뒀다 — 부르는 쪽이 돌아올 때 다시 확인한다
        } catch (_: IllegalArgumentException) {
            wait = wait.copy(hasGps = false)
        }
        val satellitesOn = try {
            LocationManagerCompat.registerGnssStatusCallback(lm, ContextCompat.getMainExecutor(context), satellites)
        } catch (_: SecurityException) {
            false
        }

        onDispose {
            runCatching { lm.removeUpdates(listener) }
            if (satellitesOn) runCatching { LocationManagerCompat.unregisterGnssStatusCallback(lm, satellites) }
        }
    }

    // 기다린 시간 · 위치 기능(설정에서 끄고 켠 것)을 1초마다 다시 읽는다
    LaunchedEffect(enabled) {
        if (!enabled) return@LaunchedEffect
        val lm = manager ?: return@LaunchedEffect
        val started = SystemClock.elapsedRealtime()
        while (true) {
            delay(1_000)
            wait = wait.copy(
                elapsedSec = ((SystemClock.elapsedRealtime() - started) / 1_000).toInt(),
                locationOn = lm.locationOn(),
                gpsOn = lm.gpsOn(),
            )
        }
    }
    return wait
}

private fun LocationManager.locationOn(): Boolean =
    runCatching { LocationManagerCompat.isLocationEnabled(this) }.getOrDefault(true)

private fun LocationManager.gpsOn(): Boolean =
    runCatching { isProviderEnabled(LocationManager.GPS_PROVIDER) }.getOrDefault(true)

/**
 * GPS 대기 화면(혼자 러닝 · 자유 러닝 · 챌린지 · 다이어트) — 권한 · 위치 기능 다음, 3-2-1 전.
 * 그림 · 큰 한 줄 · 설명 · 위성 · 정확도 · 기다린 시간 · (오래 걸리면) 까닭과 할 일. 잡히면 부른 쪽이 바로 3-2-1 로 간다.
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
 * 크루 · 번개 대기실의 GPS 확인(준비 전) — 잡히면 부른 쪽이 준비 완료로 바꾸고 닫는다. 닫기 · 취소는 준비 전으로 남는다.
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
private fun gpsHeadline(wait: GpsWait): String = stringResource(
    when {
        wait.fixed -> R.string.run_gps_wait_found
        !wait.hasGps -> R.string.run_gps_hint_no_gps_title
        else -> R.string.run_gps_wait_title
    },
)

/** 찾는 중이면 GPS 표시 둘레로 퍼지는 고리(동작 줄이기에서는 멈춘 고리), 잡히면 채운 표시, GPS 가 없으면 꺼진 표시 */
@Composable
private fun GpsSearchArt(wait: GpsWait, size: Dp) {
    val t = runTone()
    val searching = !wait.fixed && wait.hasGps
    val phase = if (searching) ambientPhase(1_800) else null
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
        RunStateArt(
            when {
                wait.fixed -> Icons.Filled.GpsFixed
                !wait.hasGps -> Icons.Filled.GpsOff
                else -> Icons.Filled.GpsNotFixed
            },
            size = size, alert = !wait.hasGps,
        )
    }
}

/** 위성 · 정확도 · 기다린 시간 한 판과, 까닭이 있으면 그 아래 안내 띠 */
@Composable
private fun ColumnScope.GpsWaitDetails(wait: GpsWait, onOpenLocationSettings: () -> Unit) {
    if (wait.hasGps) {
        RunCard(padding = androidx.compose.foundation.layout.PaddingValues(horizontal = 16.dp, vertical = 12.dp), tag = "run-gps-status") {
            val satellites = if (wait.satellitesSeen != null && wait.satellitesUsed != null) {
                stringResource(R.string.run_gps_wait_satellites_value, wait.satellitesUsed, wait.satellitesSeen)
            } else {
                stringResource(R.string.run_gps_wait_checking)
            }
            GpsStatusRow(stringResource(R.string.run_gps_wait_satellites), satellites, "run-gps-satellites")
            GpsStatusRow(
                stringResource(R.string.run_gps_wait_accuracy),
                wait.accuracyM?.let { stringResource(R.string.run_gps_wait_accuracy_value, Math.round(it)) } ?: "—",
                "run-gps-accuracy",
                note = stringResource(R.string.run_gps_wait_accuracy_need, GpsFixRule.MAX_ACCURACY_M.toInt()),
            )
            GpsStatusRow(
                stringResource(R.string.run_gps_wait_time), "%d:%02d".format(wait.elapsedSec / 60, wait.elapsedSec % 60), "run-gps-elapsed",
            )
        }
    }
    val hint = gpsHintOf(wait)
    if (hint != GpsHint.NONE) {
        Spacer(Modifier.height(12.dp))
        val (title, body) = when (hint) {
            GpsHint.NO_GPS -> R.string.run_gps_hint_no_gps_title to R.string.run_gps_hint_no_gps_body
            GpsHint.LOCATION_OFF -> R.string.run_loc_off_title to R.string.run_loc_off_body
            GpsHint.GPS_OFF -> R.string.run_gps_hint_gps_off_title to R.string.run_gps_hint_gps_off_body
            GpsHint.MOCK -> R.string.run_gps_hint_mock_title to R.string.run_gps_hint_mock_body
            GpsHint.NO_SATELLITES -> R.string.run_gps_hint_no_satellites_title to R.string.run_gps_hint_no_satellites_body
            GpsHint.WEAK -> R.string.run_gps_hint_weak_title to R.string.run_gps_hint_weak_body
            GpsHint.SLOW, GpsHint.NONE -> R.string.run_gps_hint_slow_title to R.string.run_gps_hint_slow_body
        }
        val settings = hint == GpsHint.LOCATION_OFF || hint == GpsHint.GPS_OFF
        RunNotice(
            stringResource(title),
            body = if (hint == GpsHint.WEAK) stringResource(body, GpsFixRule.MAX_ACCURACY_M.toInt()) else stringResource(body),
            kind = if (hint == GpsHint.NO_GPS || hint == GpsHint.MOCK) RunNoticeKind.Error else RunNoticeKind.Warn,
            action = if (settings) stringResource(R.string.run_loc_off_open) else null,
            onAction = if (settings) onOpenLocationSettings else null,
            tag = "run-gps-hint-${hint.name.lowercase()}",
        )
    }
}

@Composable
private fun GpsStatusRow(label: String, value: String, tag: String, note: String? = null) {
    val t = runTone()
    Row(Modifier.fillMaxWidth().padding(vertical = 6.dp), verticalAlignment = Alignment.CenterVertically) {
        androidx.compose.foundation.layout.Column(Modifier.weight(1f)) {
            Text(label, style = runTextStyle(15.sp, t.label, FontWeight.Medium))
            if (note != null) Text(note, style = runTextStyle(12.sp, t.label, FontWeight.Normal))
        }
        Text(value, style = runTextStyle(17.sp, t.text, FontWeight.Bold), textAlign = TextAlign.End, modifier = Modifier.testTag(tag))
    }
}
