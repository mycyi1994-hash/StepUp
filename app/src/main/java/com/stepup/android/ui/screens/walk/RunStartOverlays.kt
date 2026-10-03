package com.stepup.android.ui.screens.walk

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
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
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.ui.unit.sp
import com.stepup.android.R
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.material3.Text
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import com.stepup.android.domain.GeoPoint
import com.stepup.android.ui.components.LiveRouteMap
import com.stepup.android.ui.components.RunBackdrop
import com.stepup.android.ui.components.RunButton
import com.stepup.android.ui.components.RunButtonKind
import com.stepup.android.ui.components.RunHeadline
import com.stepup.android.ui.components.RunNumber
import com.stepup.android.ui.components.RunSpec
import com.stepup.android.ui.components.RunTopBar
import com.stepup.android.ui.components.rememberCurrentLocation
import com.stepup.android.ui.components.runTextStyle
import com.stepup.android.ui.components.runTone
import com.stepup.android.ui.experience.FeedbackCue
import com.stepup.android.ui.experience.LocalFeedback
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first

// 러닝 시작 전 권한 안내는 RunPermissionFlow.kt(시작·로그인·첫 사용 v1 시안 13~19)로 옮겼다 — 예전 한 장짜리 안내(S2 시안 23)를 대신한다.

/**
 * 혼자 러닝 3-2-1(시안 R01). 끝나면 [onGo] 가 한 번만 불린다.
 * 화면을 누르면 바로 시작하고, 시작 취소나 뒤로 가기는 아무것도 시작하지 않는다(기록이 생기지 않는다).
 */
@Composable
internal fun RunCountdown(
    courseName: String?,
    locationAllowed: Boolean,
    onGo: () -> Unit,
    onCancel: () -> Unit,
    /** 휴대폰 위치 기능이 켜져 있는가(권한과 별개) */
    locationServicesOn: Boolean = true,
    /** 이번 러닝 이름 — "자유 러닝" · "10분 챌린지"(시안 R01 제목) */
    title: String = "",
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
            // 그 1초 사이에 앱을 벗어났으면 세지 않는다 — 마지막 1초에 벗어났다가 (몇 시간 뒤에) 돌아오는
            // 순간 카운트 없이 러닝이 시작되지 않게, 돌아오면 그 숫자를 다시 센다
            if (lifecycle.currentState.isAtLeast(androidx.lifecycle.Lifecycle.State.RESUMED)) remaining -= 1
        }
        awaitShown()
        go()
    }
    val tapToStart = stringResource(R.string.run_countdown_tap)
    val here = rememberCurrentLocation(enabled = locationAllowed && locationServicesOn)
    RunCountdownStage(
        title = title,
        subtitle = courseName?.takeIf { it.isNotBlank() },
        kicker = stringResource(R.string.run_countdown_soon),
        digit = remaining.coerceAtLeast(1),
        caption = stringResource(
            when {
                !locationAllowed -> R.string.run_countdown_gps_off
                !locationServicesOn -> R.string.run_countdown_location_services_off
                else -> R.string.run_countdown_gps_on
            },
        ),
        here = here,
        onCancel = { if (!fired) onCancel() },
        modifier = Modifier.semantics { contentDescription = tapToStart }
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null, role = Role.Button,
            ) { go() },
    )
}

/**
 * 3-2-1 의 모습(R01 · CR08) — 머리 · 이번 러닝 이름 · "곧 시작해요" · 아주 큰 숫자 · 위치 안내 한 줄 · 시작 취소.
 * 지금 자리를 알면 뒤에 실제 지도를 어둡게 깐다(모르면 지도를 꾸미지 않는다).
 */
@Composable
internal fun RunCountdownStage(
    title: String,
    subtitle: String?,
    kicker: String,
    digit: Int,
    caption: String?,
    here: GeoPoint?,
    onCancel: (() -> Unit)?,
    modifier: Modifier = Modifier,
    /** "함께 출발 27명"처럼 숫자 위 한 줄을 바꾼다 */
    headline: (@Composable () -> Unit)? = null,
) {
    val t = runTone()
    Box(modifier.fillMaxSize().testTag("run-countdown")) {
        RunBackdrop(Modifier.fillMaxSize())
        if (here != null) {
            LiveRouteMap(
                points = listOf(here), modifier = Modifier.fillMaxSize().testTag("run-countdown-map"),
                follow = true, live = true, routeColor = t.cyan, tileFilter = t.mapFilter, tileShade = t.mapShade,
            )
            Box(
                Modifier.fillMaxSize().background(
                    Brush.verticalGradient(
                        0f to t.screen.copy(alpha = 0.92f), 0.22f to t.screen.copy(alpha = 0.55f),
                        0.7f to t.screen.copy(alpha = 0.55f), 1f to t.screen.copy(alpha = 0.95f),
                    ),
                ),
            )
        }
        Column(Modifier.fillMaxSize().safeDrawingPadding()) {
            RunTopBar(onBack = onCancel)
            Column(
                Modifier.weight(1f).fillMaxWidth().padding(horizontal = RunSpec.Gutter),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                RunHeadline(title, subtitle = subtitle, titleTag = "run-countdown-title")
                Spacer(Modifier.weight(1f))
                if (headline != null) {
                    headline()
                } else {
                    Text(
                        kicker, style = runTextStyle(30.sp, t.text, FontWeight.ExtraBold, 1.2f), textAlign = TextAlign.Center,
                        modifier = Modifier.testTag("run-countdown-kicker"),
                    )
                }
                Spacer(Modifier.height(4.dp))
                RunNumber(
                    digit.toString(), size = 200.sp, align = Alignment.CenterHorizontally,
                    modifier = Modifier.fillMaxWidth(), valueTag = "run-countdown-digit",
                )
                if (caption != null) {
                    Text(caption, style = runTextStyle(15.sp, t.label, FontWeight.Medium), textAlign = TextAlign.Center,
                        modifier = Modifier.testTag("run-countdown-caption"))
                }
                Spacer(Modifier.weight(1.3f))
            }
            if (onCancel != null) {
                Box(Modifier.fillMaxWidth().padding(horizontal = RunSpec.Gutter).padding(top = 8.dp, bottom = 14.dp)) {
                    RunButton(
                        stringResource(R.string.run_countdown_cancel), onCancel, kind = RunButtonKind.Secondary,
                        modifier = Modifier.testTag("run-countdown-cancel"),
                    )
                }
            }
        }
    }
}

/**
 * 휴대폰 위치 기능 꺼짐(시안 E07) — 위치 권한은 있는데 기기의 위치 기능이 꺼져 있다(앱 권한 거절 L02 와 다른 원인).
 * 위치 설정 열기(돌아오면 다시 읽는다) · 시간만 기록하기(R01 → R02_TIME). 위치가 없으니 지도는 그리지 않는다.
 */
@Composable
internal fun RunLocationOffContent(
    title: String,
    onBack: () -> Unit,
    onOpenSettings: () -> Unit,
    onTimeOnly: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val t = runTone()
    com.stepup.android.ui.components.RunPage(
        onBack = onBack, modifier = modifier.testTag("run-location-off-gate"), title = title.ifBlank { null },
        bottom = {
            RunButton(stringResource(R.string.run_loc_off_open), onOpenSettings, Modifier.testTag("run-location-off-open"), hero = true)
            RunButton(stringResource(R.string.run_loc_off_time_only), onTimeOnly, Modifier.testTag("run-location-off-time"),
                kind = RunButtonKind.Secondary)
        },
    ) {
        Spacer(Modifier.height(24.dp))
        Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
            SlashedArt(androidx.compose.material.icons.Icons.Filled.LocationOn, size = 180.dp)
        }
        Spacer(Modifier.height(30.dp))
        Text(
            stringResource(R.string.run_loc_off_title),
            style = androidx.compose.ui.text.TextStyle(
                fontFamily = com.stepup.android.ui.theme.StepUpSans, fontWeight = FontWeight.ExtraBold, fontSize = 28.sp,
                letterSpacing = (-0.03).em, color = t.text,
            ),
            textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth(),
        )
        Spacer(Modifier.height(8.dp))
        Text(
            stringResource(R.string.run_loc_off_body), style = runTextStyle(17.sp, t.label, FontWeight.Medium),
            textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth(),
        )
    }
}

/** 휴대폰 위치 기능이 켜져 있는가 — 알 수 없으면 켜진 것으로 본다(막지 않는다) */
internal fun locationServicesOn(context: android.content.Context): Boolean {
    val lm = androidx.core.content.ContextCompat.getSystemService(context, android.location.LocationManager::class.java)
    return lm == null || runCatching { androidx.core.location.LocationManagerCompat.isLocationEnabled(lm) }.getOrDefault(true)
}
