package com.stepup.android.ui.screens.walk

import androidx.annotation.StringRes
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.displayCutoutPadding
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.DirectionsRun
import androidx.compose.material.icons.filled.MoreHoriz
import androidx.compose.material.icons.filled.OpenInFull
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.Map
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.stepup.android.R
import com.stepup.android.domain.TrackPoint
import com.stepup.android.ui.components.LocalMapTone
import com.stepup.android.ui.components.MapTone
import com.stepup.android.ui.components.RunMapPlaceholder
import com.stepup.android.ui.components.RunSheet
import com.stepup.android.ui.components.RunSheetText
import com.stepup.android.ui.components.RunSpinner
import com.stepup.android.ui.components.runTextStyle
import com.stepup.android.ui.components.runTone
import com.stepup.android.ui.experience.feedbackClickable
import com.stepup.android.ui.theme.StepUpColors

/*
 * 자유 러닝 — 2026-10-14 v2 전달본(사용자가 고른 4번 모습, 보상 UI 없음).
 *
 * 화면 위 대부분은 실제 지도와 형광 초록 경로, 아래는 작은 기록 판(시간 | 페이스, 거리 · 속도 · 내구도)과 일시정지 | 종료.
 * 자유 러닝의 화면 · 시트 · 결과 · 공유에는 SUP · 예상/확정 보상 · 적립 · 잔액 · 보상 확인 중을 두지 않는다(화면 낭독 글 포함).
 * 보상 셈 · 저장 · 정산은 그대로 돈다 — 이 파일은 그 값을 읽지 않는다. 결과는 기록 저장(finishing)만 보고 보상 상태로 나누지 않는다.
 *
 * 믿을 수 없는 수는 "—"로 적는다(위치 찾는 중 · 신호 약함 · 위치 기능 꺼짐 · 속도 측정 오류 · 측정 무효).
 * 문제는 지도 위 노란 띠 하나로 바로 보인다(아이콘 + 글, 색만으로 알리지 않는다).
 */

// ── 판단(화면 밖에서 검사한다 — FreeRunUiTest) ──────────────────────

/** 지도 위 띠 — 위에서부터 앞선다. 문제 띠가 일시정지 띠보다 앞선다 */
internal enum class FreeBand { None, LocationOff, MeasureInvalid, SpeedError, Still, Weak, Approx, Paused }

/**
 * 지금 보일 띠 하나. 마칠지 묻는 동안([asking] — 시트가 덮는다)과 러닝 전에는 띠가 없다.
 * 위치 권한이 없어 시간만 기록하는 러닝은 문제가 아니라 고른 방식이다 — 칩("위치 없이 기록 중")만 둔다.
 * 위치를 찾는 동안에도 띠가 없다 — 지도 앱처럼 잡히는 대로 내 점이 뜬다(2026-10-17 "그 글도 없애고").
 * 30초 넘게 서 있으면([still], RunStill) "멈춰 있어요" 띠와 일시정지 알약 — 신호 약함 · 대략 위치보다 앞선다(지금 할 수 있는 일이라서).
 */
internal fun freeBand(
    active: Boolean,
    paused: Boolean,
    asking: Boolean,
    locationAllowed: Boolean,
    locationOn: Boolean,
    gpsLost: Boolean,
    precise: Boolean,
    void: Boolean,
    speedFlagged: Boolean,
    still: Boolean = false,
): FreeBand = when {
    !active || asking -> FreeBand.None
    // 달리다 휴대폰 위치를 꺼도(GPS 가 잡혀 있었어도) 알린다 — 그동안 속도 · 페이스는 "—"
    locationAllowed && !locationOn -> FreeBand.LocationOff
    void -> FreeBand.MeasureInvalid
    speedFlagged -> FreeBand.SpeedError
    still && !paused -> FreeBand.Still
    locationAllowed && gpsLost -> FreeBand.Weak
    locationAllowed && !precise -> FreeBand.Approx
    paused -> FreeBand.Paused
    else -> FreeBand.None
}

/**
 * 지금 속도(km/h) — 경로의 점으로만 잰다([currentSpeedKmh]). 걸음으로 셈한 평균은 쓰지 않는다.
 * 측정 무효 · 위치 권한 없음 · 위치 기능 꺼짐 · 신호 약함이면 모른다(null). 위치를 재는 중에 멈춰 있으면 0.
 */
internal fun freeSpeedKmh(
    paused: Boolean,
    locationAllowed: Boolean,
    locationOn: Boolean,
    gpsLost: Boolean,
    void: Boolean,
    track: List<TrackPoint>,
    now: Long,
    flaggedAt: Long,
): Double? = when {
    void -> null
    !locationAllowed || !locationOn -> null
    paused -> 0.0
    gpsLost -> null
    else -> currentSpeedKmh(track, now, flaggedAt)
}

/** 이보다 느리면(서 있거나 거의 멈춤) 페이스를 적지 않는다 — 60분/km 를 넘는 페이스는 뜻이 없다 */
internal const val FREE_PACE_MIN_KMH = 1.0

/** 지금 페이스(초/km) — 지금 속도에서. 속도를 모르거나 [FREE_PACE_MIN_KMH] 아래면 null */
internal fun freePaceSec(speedKmh: Double?): Long? =
    if (speedKmh == null || speedKmh.isNaN() || speedKmh < FREE_PACE_MIN_KMH) null else (3600.0 / speedKmh).toLong()

/** 이번 러닝 전체의 평균 페이스(초/km) — GPS 로 50m 를 넘게 달렸을 때만 */
internal fun freeAveragePaceSec(km: Double?, elapsedSec: Long): Long? =
    if (km != null && km >= 0.05 && elapsedSec > 0) (elapsedSec / km).toLong() else null

/** 자유 러닝 거리 — GPS 로 잰 거리만. 걸음으로 셈한 거리는 쓰지 않는다 */
internal sealed interface FreeDistance {
    /** 위치 없이 기록 — "—" */
    data object Unknown : FreeDistance

    data class Km(val km: Double) : FreeDistance
}

/** 경로가 생기기 전에도 0.00 부터 적는다 — "경로 대기 중"을 두지 않는다(2026-10-17 지도 앱처럼) */
internal fun freeDistance(timeOnly: Boolean, gpsKm: Double): FreeDistance =
    if (timeOnly) FreeDistance.Unknown else FreeDistance.Km(gpsKm.coerceAtLeast(0.0))

/** 저장한 자유 러닝의 거리 — GPS 거리가 없으면 모른다(null, "—") */
internal fun freeResultKm(lastGpsKm: Double): Double? = lastGpsKm.takeIf { it > 0.0 }

/** 공유할 때 함께 보내는 글 — 거리를 알면 거리 · 시간, 모르면 시간만. 보상(SUP)이 든 글(finish_share_text)은 쓰지 않는다 */
@StringRes
internal fun freeShareTextRes(lastGpsKm: Double): Int =
    if (freeResultKm(lastGpsKm) != null) R.string.finish_share_activity else R.string.run_free_share_time

/** 7′30″ — 시안의 프라임 기호(다른 모드의 7'30" 는 그대로) */
internal fun formatPacePrime(secPerKm: Long): String = "%d′%02d″".format(secPerKm / 60, secPerKm % 60)

// ── 색 ─────────────────────────────────────────────────────────────

/** 자유 러닝 색 — 시안은 남색 한 벌이고, 밝은 테마는 러닝 화면의 밝은 값([LightRunTone])에서 끌어냈다 */
@Immutable
internal class FreeRunColors(
    val screen: Color,
    val panel: Color,
    val label: Color,
    val text: Color,
    val muted: Color,
    val primary: Color,
    val primaryInk: Color,
    val slate: Color,
    val slateEdge: Color,
    val slateInk: Color,
    val off: Color,
    val offInk: Color,
    val stopMark: Color,
    val band: Color,
    val bandInk: Color,
    val calmBand: Color,
    val calmEdge: Color,
    val calmInk: Color,
    val danger: Color,
    val dangerInk: Color,
    val dangerText: Color,
    val chip: Color,
    val chipIcon: Color,
    val controlEdge: Color,
    val ring: Color,
)

private val FreeDark = FreeRunColors(
    screen = Color(0xFF05213B), panel = Color(0xFF05213B),
    label = Color(0xFFAAC3EA), text = Color(0xFFF5F8FF), muted = Color(0xFF7F99C2),
    primary = Color(0xFF0C59FF), primaryInk = Color.White,
    slate = Color(0xFF1B3D66), slateEdge = Color(0xFF2B5583), slateInk = Color(0xFFF5F8FF),
    off = Color(0xFF1B3D66), offInk = Color(0xFFA9B8CF),
    stopMark = Color(0xFFF26D6D),
    band = Color(0xFFFDB829), bandInk = Color(0xFF14171D),
    calmBand = Color(0xE6123D65), calmEdge = Color(0xFF2B5583), calmInk = Color(0xFFF5F8FF),
    danger = Color(0xFFD93A46), dangerInk = Color.White, dangerText = Color(0xFFFF6B6B),
    chip = Color(0xE605213B), chipIcon = Color(0xFF19F52E), controlEdge = Color(0xFF2B5583),
    ring = Color(0x5548D9FA),
)

private val FreeLight = FreeRunColors(
    screen = Color(0xFFF5F8FF), panel = Color.White,
    label = Color(0xFF4A5F84), text = Color(0xFF0B1E3F), muted = Color(0xFF6A7C9C),
    primary = Color(0xFF0754FF), primaryInk = Color.White,
    slate = Color(0xFFE6EEFC), slateEdge = Color(0xFFC3D3EE), slateInk = Color(0xFF0B1E3F),
    off = Color(0xFFC9D3E3), offInk = Color(0xFF6E7D96),
    stopMark = Color(0xFFE05252),
    band = Color(0xFFFDB829), bandInk = Color(0xFF14171D),
    calmBand = Color(0xF2FFFFFF), calmEdge = Color(0xFFC9D8F2), calmInk = Color(0xFF0B1E3F),
    danger = Color(0xFFD9364A), dangerInk = Color.White, dangerText = Color(0xFFC62337),
    chip = Color(0xF2FFFFFF), chipIcon = Color(0xFF0E9F1F), controlEdge = Color(0xFFC9D8F2),
    ring = Color(0x330754FF),
)

@Composable
internal fun freeRunColors(): FreeRunColors = if (StepUpColors.dark) FreeDark else FreeLight

/** 자유 러닝 경로 — 형광 초록(시안 4번) */
internal val FreeRunRoute = Color(0xFF19F52E)

/** 경로 둘레 — 밝은 길 위에서도 초록 선이 또렷하게 남색 테 */
internal val FreeRunRouteOutline = Color(0xE605213B)

// ── 수 ─────────────────────────────────────────────────────────────

/** 기록 판의 수 — 모두 이번 러닝의 실제 값. 모르면 null("—") */
@Immutable
internal data class FreeRunNumbers(
    val time: String,
    val paceSec: Long?,
    val distance: FreeDistance,
    val speedKmh: Double?,
    val durability: Int?,
    /** 멈춘 뒤(저장 중 · 저장 실패 · 결과)의 페이스 · 속도는 이번 러닝 전체 평균이다 — "평균 페이스" */
    val average: Boolean = false,
)

/** 칸마다 붙이는 이름표(검사용) — 러닝 중과 결과가 다르다 */
private class FreeTags(val time: String, val pace: String, val distance: String, val speed: String, val durability: String)

private val LiveTags = FreeTags("run-hero-value", "run-pace-value", "run-distance-value", "run-speed-value", "run-durability-value")
private val ResultTags = FreeTags("run-result-time", "run-result-pace", "run-result-distance", "run-result-speed", "run-result-durability")
/** 3-2-1 의 판(00:00 | —) — 러닝 중 화면의 이름표와 겹치지 않게 */

/** 시트 · 실패 화면의 한 줄 — "24:18 · 3.24 km". 거리를 모르면 시간만 */
internal fun freeTimeDistance(time: String, distance: FreeDistance): String = when (distance) {
    is FreeDistance.Km -> "$time · ${"%.2f".format(distance.km)} km"
    else -> time
}

// ── 러닝 중 · 저장 중 · 저장 실패 ───────────────────────────────────

internal enum class FreeLiveMode {
    /** 시작 전(3-2-1 뒤에 깔린 화면) */
    Idle,

    /** 달리는 중 · 일시정지(01 · 03 · 13) */
    Live,

    /** 기록 저장 중(06) */
    Saving,

    /** 기록 저장 실패(07) — 기록은 서비스에 그대로 있고 같은 러닝으로 다시 저장한다 */
    Failed,

    /** 저장 없이 종료가 실패(14 오른쪽) — 러닝은 멈춘 채 그대로 */
    DiscardFailed,
}

/**
 * 자유 러닝의 러닝 중 화면 — 지도 가득 + 아래 작은 기록 판. 상태 없이 그린다.
 * [onPrimary] 는 판의 파란 버튼(러닝 시작 · 일시정지 · 다시 시작 · 다시 저장 · 다시 시도), [onSecondary] 는 옆 버튼
 * (취소 · 종료 · 화면에 머물기 · 돌아가기). 저장 중에는 버튼 하나(누를 수 없음)만 둔다.
 */
@Composable
internal fun FreeRunLive(
    mode: FreeLiveMode,
    numbers: FreeRunNumbers,
    map: @Composable BoxScope.() -> Unit,
    onPrimary: () -> Unit,
    onSecondary: () -> Unit,
    modifier: Modifier = Modifier,
    band: FreeBand = FreeBand.None,
    paused: Boolean = false,
    /** 위치 권한 없이 시간만 기록 — 칩 글이 "위치 없이 기록 중" */
    noLocation: Boolean = false,
    onDetails: (() -> Unit)? = null,
    onExpandMap: (() -> Unit)? = null,
    /** 측정 무효 · 속도 오류 띠를 누르면(러닝 정보) */
    onBandTap: (() -> Unit)? = null,
    /** 위치 기능 꺼짐 · 대략적 위치 띠의 "설정" */
    onBandAction: (() -> Unit)? = null,
    /** 저장 실패의 "화면에 머물기" — 한 번 누르면 숨는다(띠와 다시 저장은 남는다) */
    showSecondary: Boolean = true,
) {
    val top: (@Composable () -> Unit)? = when (mode) {
        FreeLiveMode.Idle, FreeLiveMode.Saving -> null
        FreeLiveMode.Failed -> {
            { FreeRunBand(stringResource(R.string.run_free_save_failed), "run-save-error") }
        }
        FreeLiveMode.DiscardFailed -> {
            { FreeRunBand(stringResource(R.string.run_free_delete_failed), "run-discard-failed") }
        }
        FreeLiveMode.Live -> liveTop(band, paused, noLocation, onBandTap, onBandAction)
    }
    FreeRunFrame(
        rootTag = if (mode == FreeLiveMode.Saving) "run-saving" else "run-live",
        modifier = modifier,
        map = map,
        mapTag = "run-live-map",
        top = top,
        onMore = onDetails.takeIf { mode == FreeLiveMode.Idle || mode == FreeLiveMode.Live },
        moreLabel = stringResource(R.string.run_details),
        moreTag = "run-details-open",
        onExpand = onExpandMap.takeIf { mode == FreeLiveMode.Live },
    ) { compact ->
        when (mode) {
            FreeLiveMode.Idle -> {
                FreeTopRow(numbers, LiveTags, compact)
                Spacer(Modifier.height(if (compact) 10.dp else 14.dp))
                FreeButtonRow {
                    FreeRunButton(stringResource(R.string.home_start_run), onPrimary, Modifier.weight(1.15f).testTag("run-primary-action"),
                        icon = Icons.Filled.PlayArrow)
                    FreeRunButton(stringResource(R.string.run_cancel), onSecondary, Modifier.weight(1f).testTag("run-finish"),
                        kind = FreeButtonKind.Secondary)
                }
            }
            FreeLiveMode.Live -> {
                FreeTopRow(numbers, LiveTags, compact)
                FreeHairline(compact)
                FreeDetailRow(numbers, LiveTags)
                if (paused) FreeCaption(stringResource(R.string.runflow_pause_body), tag = "run-paused-note")
                Spacer(Modifier.height(if (compact) 8.dp else 12.dp))
                FreeButtonRow {
                    FreeRunButton(
                        stringResource(if (paused) R.string.run_free_resume else R.string.run_pause), onPrimary,
                        Modifier.weight(1.15f).testTag("run-primary-action"),
                        icon = if (paused) Icons.Filled.PlayArrow else Icons.Filled.Pause,
                    )
                    FreeRunButton(stringResource(R.string.run_free_stop), onSecondary, Modifier.weight(1f).testTag("run-finish"),
                        kind = FreeButtonKind.Secondary, stopMark = true)
                }
            }
            FreeLiveMode.Saving -> {
                FreeTopRow(numbers, LiveTags, compact)
                FreeHairline(compact)
                FreeDetailRow(numbers, LiveTags)
                FreeCaption(stringResource(R.string.run_saving_wait))
                Spacer(Modifier.height(if (compact) 8.dp else 10.dp))
                FreeRunButton(stringResource(R.string.run_saving_title), {}, Modifier.fillMaxWidth().testTag("run-primary-action"),
                    kind = FreeButtonKind.Secondary, enabled = false, busy = true)
            }
            FreeLiveMode.Failed -> {
                FreeTopRow(numbers, LiveTags, compact)
                FreeHairline(compact)
                FreeDetailRow(numbers, LiveTags)
                FreeCaption(stringResource(R.string.run_free_save_kept), icon = Icons.Outlined.Info, tag = "run-save-kept")
                Spacer(Modifier.height(if (compact) 8.dp else 10.dp))
                FreeButtonRow {
                    FreeRunButton(stringResource(R.string.runflow_save_again), onPrimary, Modifier.weight(1f).testTag("run-primary-action"))
                    if (showSecondary) {
                        FreeRunButton(stringResource(R.string.run_free_save_stay), onSecondary, Modifier.weight(1f).testTag("run-save-stay"),
                            kind = FreeButtonKind.Secondary)
                    }
                }
            }
            FreeLiveMode.DiscardFailed -> {
                FreeCaption(stringResource(R.string.run_free_try_again), strong = true)
                FreeHairline(compact)
                FreeCompactRow(numbers)
                Spacer(Modifier.height(if (compact) 10.dp else 14.dp))
                FreeButtonRow {
                    FreeRunButton(stringResource(R.string.run_free_retry), onPrimary, Modifier.weight(1f).testTag("run-discard-retry"))
                    FreeRunButton(stringResource(R.string.runflow_discard_back), onSecondary, Modifier.weight(1f).testTag("run-discard-failed-back"),
                        kind = FreeButtonKind.Secondary)
                }
            }
        }
    }
}

/** 달리는 중 지도 위 — 문제 띠 하나, 일시정지 띠, 없으면 "러닝 중" 칩. 마칠지 묻는 동안(멈춤 + 띠 없음)은 비운다 */
internal fun liveTop(
    band: FreeBand,
    paused: Boolean,
    noLocation: Boolean,
    onBandTap: (() -> Unit)?,
    onBandAction: (() -> Unit)?,
): (@Composable () -> Unit)? = when (band) {
    FreeBand.None -> if (paused) null else {
        { FreeRunChip(stringResource(if (noLocation) R.string.run_status_no_gps else R.string.run_free_chip_running)) }
    }
    FreeBand.Paused -> {
        { FreeRunBand(stringResource(R.string.run_paused_title), "run-paused-band", calm = true, icon = Icons.Filled.Pause) }
    }
    FreeBand.LocationOff -> {
        { FreeRunBand(stringResource(R.string.run_free_location_off), "run-location-off",
            action = stringResource(R.string.run_free_settings), onAction = onBandAction) }
    }
    FreeBand.MeasureInvalid -> {
        { FreeRunBand(stringResource(R.string.run_free_measure_invalid), "run-void-banner", onClick = onBandTap) }
    }
    FreeBand.SpeedError -> {
        { FreeRunBand(stringResource(R.string.run_speed_error), "run-speed-banner", onClick = onBandTap) }
    }
    FreeBand.Still -> {
        { FreeRunBand(stringResource(R.string.run_still_title), "run-still", icon = Icons.Filled.Pause,
            action = stringResource(R.string.run_pause), onAction = onBandAction) }
    }
    FreeBand.Weak -> {
        { FreeRunBand(stringResource(R.string.run_free_gps_weak), "run-gps-weak") }
    }
    FreeBand.Approx -> {
        { FreeRunBand(stringResource(R.string.run_gps_approx), "run-location-approx",
            action = stringResource(R.string.run_free_settings), onAction = onBandAction) }
    }
}

// ── 결과 ───────────────────────────────────────────────────────────

/**
 * 러닝 완료(08) — 지도 가득 + 러닝 완료 · 기록 저장 완료 + 기록 판 + 완료 | 공유. 보상 상태와 상관없이 같은 모습이다.
 * 덜 쓰는 "내 러닝 기록 보기" · "삭제"는 ⋯ 안으로 옮겼다(지우지 않는다).
 * [map] 이 null 이면 저장된 경로가 없다(14 왼쪽) — 지도를 꾸미지 않는다.
 * [deleteFailed] — 기록 삭제가 실패(14 오른쪽). 기록은 그대로 있고 다시 시도하거나 돌아간다.
 */
@Composable
internal fun FreeRunResult(
    numbers: FreeRunNumbers,
    map: (@Composable BoxScope.() -> Unit)?,
    onDone: () -> Unit,
    onShare: () -> Unit,
    onRecords: () -> Unit,
    onDelete: () -> Unit,
    modifier: Modifier = Modifier,
    deleteFailed: Boolean = false,
    deleting: Boolean = false,
    onRetryDelete: () -> Unit = {},
    onDeleteBack: () -> Unit = {},
) {
    val c = freeRunColors()
    var more by rememberSaveable { mutableStateOf(false) }
    FreeRunFrame(
        rootTag = "run-result",
        modifier = modifier,
        map = {
            if (map != null) {
                map()
            } else {
                Box(Modifier.fillMaxSize().background(c.screen)) {
                    RunMapPlaceholder(
                        Icons.Outlined.Map, stringResource(R.string.rec_no_route_title), stringResource(R.string.run_free_no_route_body),
                        Modifier.testTag("run-result-no-route"),
                    )
                }
            }
        },
        mapTag = null,
        top = if (deleteFailed) {
            { FreeRunBand(stringResource(R.string.run_free_delete_failed), "run-delete-failed") }
        } else {
            {
                Column(Modifier.padding(start = 4.dp, top = 2.dp)) {
                    Text(
                        stringResource(R.string.finish_title), style = runTextStyle(28.sp, c.text, FontWeight.ExtraBold, 1.2f),
                        modifier = Modifier.semantics { heading() }.testTag("run-result-title"),
                    )
                    Text(stringResource(R.string.run_free_record_saved), style = runTextStyle(15.sp, c.label, FontWeight.SemiBold),
                        modifier = Modifier.testTag("run-result-saved"))
                }
            }
        },
        scrimTop = !deleteFailed,
        onMore = if (deleteFailed) null else ({ more = true }),
        moreLabel = stringResource(R.string.run_free_more),
        moreTag = "run-result-more",
        onExpand = null,
    ) { compact ->
        if (deleteFailed) {
            FreeCaption(stringResource(R.string.run_free_try_again), strong = true)
            FreeHairline(compact)
            FreeCompactRow(numbers)
            Spacer(Modifier.height(if (compact) 10.dp else 14.dp))
            FreeButtonRow {
                FreeRunButton(stringResource(R.string.run_free_retry), onRetryDelete, Modifier.weight(1f).testTag("run-delete-retry"),
                    busy = deleting)
                FreeRunButton(stringResource(R.string.runflow_discard_back), onDeleteBack, Modifier.weight(1f).testTag("run-delete-back"),
                    kind = FreeButtonKind.Secondary, enabled = !deleting)
            }
        } else {
            FreeTopRow(numbers, ResultTags, compact)
            FreeHairline(compact)
            FreeDetailRow(numbers, ResultTags)
            Spacer(Modifier.height(if (compact) 10.dp else 14.dp))
            FreeButtonRow {
                FreeRunButton(stringResource(R.string.finish_done), onDone, Modifier.weight(1.15f).testTag("run-result-done"))
                FreeRunButton(stringResource(R.string.run_free_share), onShare, Modifier.weight(1f).testTag("run-result-share"),
                    kind = FreeButtonKind.Secondary)
            }
        }
    }
    if (more && !deleteFailed) {
        RunSheet(onDismiss = { more = false }, modifier = Modifier.testTag("run-result-more-sheet")) {
            FreeRunButton(stringResource(R.string.run_open_records), {
                more = false
                onRecords()
            }, Modifier.fillMaxWidth().testTag("run-result-records"), kind = FreeButtonKind.Secondary)
            Spacer(Modifier.height(10.dp))
            FreeRunButton(stringResource(R.string.run_delete), {
                more = false
                onDelete()
            }, Modifier.fillMaxWidth().testTag("run-result-delete"), kind = FreeButtonKind.Danger)
        }
    }
}

// ── 시트 ───────────────────────────────────────────────────────────

/** 04 — 러닝을 마칠까요? 저장하고 종료(주) · 계속 달리기 · 저장하지 않고 종료(빨간 글, 지울 수 있을 때만) */
@Composable
internal fun ColumnScope.FreeFinishSheetContent(
    line: String,
    onSave: () -> Unit,
    onContinue: () -> Unit,
    onDiscard: (() -> Unit)?,
) {
    val c = freeRunColors()
    RunSheetText(stringResource(R.string.run_finish_title))
    Spacer(Modifier.height(6.dp))
    Text(line, style = runTextStyle(16.sp, c.label, FontWeight.SemiBold), textAlign = TextAlign.Center,
        modifier = Modifier.testTag("run-end-line"))
    Spacer(Modifier.height(18.dp))
    FreeRunButton(stringResource(R.string.run_free_save_finish), onSave, Modifier.fillMaxWidth().testTag("run-end-save"))
    Spacer(Modifier.height(10.dp))
    FreeRunButton(stringResource(R.string.run_continue), onContinue, Modifier.fillMaxWidth().testTag("run-end-continue"),
        kind = FreeButtonKind.Secondary)
    if (onDiscard != null) {
        Spacer(Modifier.height(12.dp))
        Box(Modifier.fillMaxWidth().height(1.dp).background(runPalette().divider))
        Box(
            Modifier.fillMaxWidth().heightIn(min = 48.dp).clip(RoundedCornerShape(12.dp)).feedbackClickable(onClick = onDiscard)
                .testTag("run-end-discard"),
            contentAlignment = Alignment.Center,
        ) {
            Text(stringResource(R.string.run_free_discard), style = runTextStyle(16.sp, c.dangerText, FontWeight.Bold))
        }
    }
}

/** 05 — 저장하지 않고 종료할까요? 돌아가기 | 저장 없이 종료(빨강). 돌아가면 04 로, 러닝은 멈춘 채 */
@Composable
internal fun ColumnScope.FreeDiscardSheetContent(line: String, onDiscard: () -> Unit, onCancel: () -> Unit) {
    val c = freeRunColors()
    RunSheetText(stringResource(R.string.run_free_discard_title), note = stringResource(R.string.run_free_discard_body))
    Spacer(Modifier.height(8.dp))
    Text(line, style = runTextStyle(16.sp, c.label, FontWeight.SemiBold), textAlign = TextAlign.Center)
    Spacer(Modifier.height(18.dp))
    FreeButtonRow {
        FreeRunButton(stringResource(R.string.runflow_discard_back), onCancel, Modifier.weight(1f).testTag("run-discard-back"),
            kind = FreeButtonKind.Secondary)
        FreeRunButton(stringResource(R.string.run_free_discard_confirm), onDiscard, Modifier.weight(1f).testTag("run-discard-confirm"),
            kind = FreeButtonKind.Danger)
    }
}

// ── 틀 · 부품 ──────────────────────────────────────────────────────

/**
 * 지도 가득(위) + 기록 판(아래). 판은 내용만큼, 지도는 남는 높이를 모두 쓴다 — 버튼은 늘 화면 안에 있다.
 * 낮은 화면(큰 글씨 포함)은 판의 수와 사이를 줄인다([compact]).
 */
@Composable
private fun FreeRunFrame(
    rootTag: String,
    modifier: Modifier,
    map: @Composable BoxScope.() -> Unit,
    mapTag: String?,
    top: (@Composable () -> Unit)?,
    onMore: (() -> Unit)?,
    moreLabel: String,
    moreTag: String,
    onExpand: (() -> Unit)?,
    scrimTop: Boolean = false,
    panel: @Composable ColumnScope.(compact: Boolean) -> Unit,
) {
    val c = freeRunColors()
    val t = runTone()
    BoxWithConstraints(modifier.fillMaxSize().background(c.screen).testTag(rootTag)) {
        val compact = maxHeight < 640.dp
        Column(Modifier.fillMaxSize()) {
            Box(Modifier.weight(1f).fillMaxWidth().clipToBounds().background(t.inset)) {
                // 실제 지도 타일을 러닝 화면의 남색 지도 색으로(지도 틀 RunMapFrame 과 같은 색)
                CompositionLocalProvider(LocalMapTone provides MapTone(t.mapFilter, t.mapShade, t.mapRoads)) {
                    Box(Modifier.fillMaxSize().then(if (mapTag != null) Modifier.testTag(mapTag) else Modifier), content = map)
                }
                if (scrimTop) {
                    // 밝아진 지도 위에서도 "러닝 완료 · 기록 저장 완료"가 읽히게 — 글 자리까지 짙게, 아래로 녹인다
                    Box(
                        Modifier.fillMaxWidth().statusBarsPadding().height(130.dp)
                            .background(Brush.verticalGradient(
                                0f to c.screen.copy(alpha = 0.94f), 0.6f to c.screen.copy(alpha = 0.78f), 1f to c.screen.copy(alpha = 0f),
                            )),
                    )
                    Box(Modifier.fillMaxWidth().background(c.screen.copy(alpha = 0.94f)).statusBarsPadding())
                }
                Row(
                    Modifier.fillMaxWidth().statusBarsPadding().displayCutoutPadding().padding(horizontal = 12.dp).padding(top = 8.dp),
                    verticalAlignment = Alignment.Top,
                ) {
                    Box(Modifier.weight(1f)) { top?.invoke() }
                    if (onMore != null) {
                        Spacer(Modifier.width(8.dp))
                        FreeMapButton(Icons.Filled.MoreHoriz, moreLabel, moreTag, onMore)
                    }
                }
                if (onExpand != null) {
                    FreeMapButton(
                        Icons.Filled.OpenInFull, stringResource(R.string.run_map_expand), "run-map-expand", onExpand,
                        Modifier.align(Alignment.BottomEnd).padding(12.dp),
                    )
                }
            }
            Box(Modifier.fillMaxWidth().height(1.dp).background(runPalette().divider))
            Column(
                Modifier.fillMaxWidth().background(c.panel).navigationBarsPadding()
                    .padding(horizontal = 16.dp).padding(top = if (compact) 10.dp else 14.dp, bottom = if (compact) 10.dp else 14.dp),
            ) { panel(compact) }
        }
    }
}

/** 1줄 — 시간 | 페이스(큰 수 두 칸) */
@Composable
private fun FreeTopRow(numbers: FreeRunNumbers, tags: FreeTags, compact: Boolean) {
    val timeLabel = stringResource(R.string.stat_time)
    val paceLabel = stringResource(if (numbers.average) R.string.run_label_pace else R.string.run_pace)
    val pace = numbers.paceSec?.let { formatPacePrime(it) }
    val paceSpoken = pace?.let { "$paceLabel $it /km" } ?: "$paceLabel · ${stringResource(R.string.run_free_pace_unknown)}"
    val size = if (compact) 28.sp else 34.sp
    DividedRow(columns = 2, gap = 28.dp) {
        FreeBigCell(timeLabel, numbers.time, null, size, tags.time, "$timeLabel ${numbers.time}")
        FreeBigCell(paceLabel, pace ?: "—", "/km", size, tags.pace, paceSpoken)
    }
}

@Composable
private fun RowScope.FreeBigCell(label: String, value: String, unit: String?, size: androidx.compose.ui.unit.TextUnit, tag: String, spoken: String) {
    val c = freeRunColors()
    Column(Modifier.weight(1f).semantics(mergeDescendants = true) { contentDescription = spoken }) {
        Text(label, style = runTextStyle(14.sp, c.label, FontWeight.Medium), maxLines = 1, overflow = TextOverflow.Ellipsis)
        FitNumber(
            value, unit, valueSize = size, unitSize = size * 0.5f, valueColor = c.text, unitColor = c.label,
            modifier = Modifier.fillMaxWidth(), valueTag = tag,
        )
    }
}

/** 2줄 — 거리 · 속도 · 내구도(작은 글 세 칸). 신발 그림은 두지 않는다 */
@Composable
private fun FreeDetailRow(numbers: FreeRunNumbers, tags: FreeTags) {
    val distanceLabel = stringResource(R.string.run_label_distance)
    val speedLabel = stringResource(if (numbers.average) R.string.run_label_speed else R.string.run_current_speed)
    val durabilityLabel = stringResource(R.string.sdv_stat_durability)
    val unit = stringResource(R.string.run_speed_unit)
    DividedRow(columns = 3, gap = 12.dp) {
        when (val d = numbers.distance) {
            is FreeDistance.Km -> {
                val km = "%.2f".format(d.km)
                FreeSmallCell(km, "km", tags.distance, "$distanceLabel $km km", Arrangement.Start)
            }
            FreeDistance.Unknown -> FreeSmallCell("—", "km", tags.distance, "$distanceLabel —", Arrangement.Start)
        }
        val speed = numbers.speedKmh?.let { "%.1f".format(it) }
        FreeSmallCell(
            speed ?: "—", unit, tags.speed,
            if (speed != null) "$speedLabel $speed $unit"
            else if (numbers.average) "$speedLabel —" else stringResource(R.string.run_speed_unknown),
            Arrangement.Center,
        )
        val points = numbers.durability?.toString() ?: "—"
        FreeSmallCell(points, "%", tags.durability, "$durabilityLabel $points%", Arrangement.End, lead = durabilityLabel)
    }
}

@Composable
private fun RowScope.FreeSmallCell(
    value: String,
    unit: String?,
    tag: String,
    spoken: String,
    arrangement: Arrangement.Horizontal,
    lead: String? = null,
) {
    val c = freeRunColors()
    Row(
        Modifier.weight(1f).heightIn(min = 28.dp).semantics(mergeDescendants = true) { contentDescription = spoken },
        horizontalArrangement = arrangement,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (lead != null) {
            // 좁으면 이름(내구도)이 먼저 줄어든다 — 수는 제 크기로 남는다
            Text(lead, style = runTextStyle(13.sp, c.label, FontWeight.Medium), maxLines = 1, overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f, fill = false))
            Spacer(Modifier.width(5.dp))
            FitNumber(
                value, unit, valueSize = 17.sp, unitSize = 13.sp, valueColor = c.text, unitColor = c.label, valueTag = tag,
            )
        } else {
            FitNumber(
                value, unit, valueSize = 17.sp, unitSize = 13.sp, valueColor = c.text, unitColor = c.label,
                modifier = Modifier.weight(1f, fill = false), valueTag = tag,
            )
        }
    }
}

/** 실패 화면의 줄 — 시간 · 페이스 · 거리 세 칸(14 오른쪽) */
@Composable
private fun FreeCompactRow(numbers: FreeRunNumbers) {
    val c = freeRunColors()
    val pace = numbers.paceSec?.let { formatPacePrime(it) } ?: "—"
    val km = (numbers.distance as? FreeDistance.Km)?.let { "%.2f".format(it.km) } ?: "—"
    val names = listOf(stringResource(R.string.stat_time), stringResource(R.string.run_label_pace), stringResource(R.string.run_label_distance))
    DividedRow(columns = 3, gap = 12.dp, modifier = Modifier.testTag("run-free-compact")) {
        for ((i, cell) in listOf(numbers.time to null, pace to "/km", km to "km").withIndex()) {
            val (value, unit) = cell
            Box(
                Modifier.weight(1f).semantics(mergeDescendants = true) { contentDescription = "${names[i]} $value ${unit ?: ""}".trim() },
                contentAlignment = Alignment.Center,
            ) {
                FitNumber(value, unit, valueSize = 22.sp, unitSize = 14.sp, valueColor = c.text, unitColor = c.label)
            }
        }
    }
}

@Composable
private fun FreeHairline(compact: Boolean) {
    Spacer(Modifier.height(if (compact) 6.dp else 10.dp))
    Box(Modifier.fillMaxWidth().height(1.dp).background(runPalette().divider))
    Spacer(Modifier.height(if (compact) 6.dp else 10.dp))
}

/** 판 안 한 줄 안내 — 가운데, 작은 글(멈춤 · 저장 중 · 저장 실패) */
@Composable
private fun FreeCaption(text: String, icon: ImageVector? = null, tag: String? = null, strong: Boolean = false) {
    val c = freeRunColors()
    Row(
        Modifier.fillMaxWidth().padding(top = 8.dp).then(if (tag != null) Modifier.testTag(tag) else Modifier),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (icon != null) {
            Icon(icon, contentDescription = null, tint = c.label, modifier = Modifier.size(16.dp))
            Spacer(Modifier.width(6.dp))
        }
        Text(
            text, style = runTextStyle(if (strong) 17.sp else 13.sp, if (strong) c.text else c.label,
                if (strong) FontWeight.Bold else FontWeight.Medium),
            textAlign = TextAlign.Center,
        )
    }
}

@Composable
private fun FreeButtonRow(content: @Composable RowScope.() -> Unit) {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp), verticalAlignment = Alignment.CenterVertically,
        content = content)
}

internal enum class FreeButtonKind { Primary, Secondary, Danger }

/** 판의 버튼 — 평평한 코발트(주) · 짙은 남색(보조, 빨간 네모를 앞에 둘 수 있다) · 빨강(위험). 56dp 높이 */
@Composable
internal fun FreeRunButton(
    label: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    kind: FreeButtonKind = FreeButtonKind.Primary,
    icon: ImageVector? = null,
    stopMark: Boolean = false,
    enabled: Boolean = true,
    busy: Boolean = false,
) {
    val c = freeRunColors()
    val shape = RoundedCornerShape(14.dp)
    val live = enabled && !busy
    val (face, ink) = when {
        !enabled && !busy -> c.off to c.offInk
        kind == FreeButtonKind.Primary -> c.primary to c.primaryInk
        kind == FreeButtonKind.Danger -> c.danger to c.dangerInk
        else -> c.slate to c.slateInk
    }
    Row(
        modifier.heightIn(min = 56.dp).clip(shape).background(face, shape)
            .then(if (kind == FreeButtonKind.Secondary) Modifier.border(1.dp, c.slateEdge, shape) else Modifier)
            .feedbackClickable(enabled = live, onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        when {
            busy -> {
                RunSpinner(Modifier.size(22.dp), color = ink)
                Spacer(Modifier.width(10.dp))
            }
            stopMark -> {
                Box(Modifier.size(16.dp).clip(RoundedCornerShape(3.dp)).background(c.stopMark))
                Spacer(Modifier.width(10.dp))
            }
            icon != null -> {
                Icon(icon, contentDescription = null, tint = ink, modifier = Modifier.size(24.dp))
                Spacer(Modifier.width(8.dp))
            }
        }
        Text(label, style = runTextStyle(17.sp, ink, FontWeight.Bold, 1.2f), maxLines = 2, overflow = TextOverflow.Ellipsis,
            textAlign = TextAlign.Center)
    }
}

/**
 * 지도 위 띠 — 노랑(문제) · 남색(일시정지). 경고 아이콘과 글을 함께 둔다(색만으로 알리지 않는다). 바뀌면 화면 낭독이 읽는다.
 * [onClick] 이 있으면 띠를 눌러 까닭(러닝 정보)을 연다. [action] 은 오른쪽 작은 버튼(설정).
 */
@Composable
internal fun FreeRunBand(
    text: String,
    tag: String,
    calm: Boolean = false,
    icon: ImageVector = Icons.Filled.Warning,
    onClick: (() -> Unit)? = null,
    action: String? = null,
    onAction: (() -> Unit)? = null,
) {
    val c = freeRunColors()
    val shape = RoundedCornerShape(24.dp)
    val face = if (calm) c.calmBand else c.band
    val ink = if (calm) c.calmInk else c.bandInk
    val hasAction = action != null && onAction != null
    Row(
        Modifier.fillMaxWidth().heightIn(min = 48.dp).clip(shape).background(face, shape)
            .then(if (calm) Modifier.border(1.dp, c.calmEdge, shape) else Modifier)
            .then(if (onClick != null) Modifier.feedbackClickable(onClick = onClick) else Modifier)
            .semantics(mergeDescendants = true) { liveRegion = LiveRegionMode.Polite }
            .padding(start = 16.dp, end = if (hasAction) 2.dp else 16.dp, top = if (hasAction) 0.dp else 4.dp, bottom = if (hasAction) 0.dp else 4.dp)
            .testTag(tag),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(icon, contentDescription = null, tint = ink, modifier = Modifier.size(20.dp))
        Spacer(Modifier.width(8.dp))
        Text(
            text, style = runTextStyle(15.sp, ink, FontWeight.Bold, 1.25f), maxLines = 2, overflow = TextOverflow.Ellipsis,
            textAlign = if (hasAction) TextAlign.Start else TextAlign.Center,
            modifier = Modifier.weight(1f, fill = hasAction),
        )
        if (hasAction) {
            Spacer(Modifier.width(8.dp))
            // 누르는 자리는 48dp, 보이는 알약은 36dp
            Box(
                Modifier.heightIn(min = 48.dp).clip(RoundedCornerShape(24.dp)).feedbackClickable(onClick = onAction!!)
                    .testTag("$tag-action"),
                contentAlignment = Alignment.Center,
            ) {
                Box(
                    Modifier.heightIn(min = 36.dp).clip(RoundedCornerShape(18.dp)).background(c.bandInk).padding(horizontal = 16.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(action!!, style = runTextStyle(14.sp, c.band, FontWeight.Bold), maxLines = 1)
                }
            }
        }
    }
}

/** "러닝 중" — 지도 왼쪽 위 작은 칩(달리는 사람 + 글) */
@Composable
private fun FreeRunChip(text: String) {
    val c = freeRunColors()
    val shape = RoundedCornerShape(20.dp)
    Row(
        Modifier.heightIn(min = 36.dp).clip(shape).background(c.chip, shape).border(1.dp, c.controlEdge, shape)
            .padding(horizontal = 12.dp).testTag("run-status"),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(Icons.AutoMirrored.Filled.DirectionsRun, contentDescription = null, tint = c.chipIcon, modifier = Modifier.size(18.dp))
        Spacer(Modifier.width(6.dp))
        Text(text, style = runTextStyle(14.sp, c.text, FontWeight.SemiBold), maxLines = 1)
    }
}

/** 지도 위 동그란 48dp 버튼 — ⋯(러닝 정보 · 더 보기) · 지도 크게 보기 */
@Composable
internal fun FreeMapButton(icon: ImageVector, label: String, tag: String, onClick: () -> Unit, modifier: Modifier = Modifier, size: Dp = 48.dp) {
    val c = freeRunColors()
    Box(
        modifier.size(size).clip(CircleShape).background(c.chip, CircleShape).border(1.dp, c.controlEdge, CircleShape)
            .feedbackClickable(onClick = onClick).semantics { contentDescription = label }.testTag(tag),
        contentAlignment = Alignment.Center,
    ) {
        Icon(icon, contentDescription = null, tint = c.text, modifier = Modifier.size(22.dp))
    }
}
