package com.stepup.android.ui.screens.walk

import com.stepup.android.ui.components.pressable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.onClick
import androidx.compose.ui.semantics.onLongClick
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.ui.draw.drawBehind
import kotlinx.coroutines.launch
import com.stepup.android.ui.components.ButtonState
import com.stepup.android.ui.components.DangerButton
import com.stepup.android.ui.components.SecondaryBusyButton
import com.stepup.android.ui.components.PrimaryButton
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
import androidx.compose.material.icons.filled.Error
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.outlined.IosShare
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material.icons.outlined.LockOpen
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.MoreHoriz
import androidx.compose.material.icons.rounded.Pause
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.em
import com.stepup.android.ui.components.BottomActionBar
import com.stepup.android.ui.components.CircleIconButton
import com.stepup.android.ui.components.HoldBubble
import com.stepup.android.ui.components.HoldToEndButton
import com.stepup.android.ui.components.RoundControl
import com.stepup.android.ui.components.StatItem
import com.stepup.android.ui.components.StatRow
import com.stepup.android.ui.components.StepUpUi
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
internal enum class FreeBand { None, Overspeed, LocationOff, MeasureInvalid, SpeedError, Still, Weak, Approx, Worn, Paused }

/**
 * 지금 보일 띠 하나. 마칠지 묻는 동안([asking] — 시트가 덮는다)과 러닝 전에는 띠가 없다.
 * 위치 권한이 없어 시간만 기록하는 러닝은 문제가 아니라 고른 방식이다 — 칩("위치 없이 기록 중")만 둔다.
 * 위치를 찾는 동안에도 띠가 없다 — 지도 앱처럼 잡히는 대로 내 점이 뜬다(2026-10-17 "그 글도 없애고").
 * 30초 넘게 서 있으면([still], RunStill) "멈춰 있어요" 띠와 일시정지 알약 — 신호 약함 · 대략 위치보다 앞선다(지금 할 수 있는 일이라서).
 * 과속이 이어지는 동안([overspeed], RunOverspeed)은 "N초 뒤 러닝이 멈춰요" 띠가 가장 앞선다 — 30초를 채우면 러닝이 멈추고 패널티가 있다.
 * 신발 내구도를 다 썼으면([shoeWorn]) "이제 걸음은 보상이 없어요" 띠 — 남은 러닝 내내 이어지므로 다른 문제 띠보다 뒤에 둔다
 * (2026-10-19 "내구도 다 소모하면 따로 안내").
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
    overspeed: Boolean = false,
    shoeWorn: Boolean = false,
): FreeBand = when {
    !active || asking -> FreeBand.None
    overspeed && !paused -> FreeBand.Overspeed
    // 달리다 휴대폰 위치를 꺼도(GPS 가 잡혀 있었어도) 알린다 — 그동안 속도 · 페이스는 "—"
    locationAllowed && !locationOn -> FreeBand.LocationOff
    void -> FreeBand.MeasureInvalid
    speedFlagged -> FreeBand.SpeedError
    still && !paused -> FreeBand.Still
    locationAllowed && gpsLost -> FreeBand.Weak
    locationAllowed && !precise -> FreeBand.Approx
    shoeWorn && !paused -> FreeBand.Worn
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
    /** 마지막으로 걸음이 는 때 — 경로 점이 드문 동안에도 걷고 있으면 속도를 0 으로 떨어뜨리지 않는다 */
    movingAt: Long = 0L,
): Double? = when {
    void -> null
    !locationAllowed || !locationOn -> null
    paused -> 0.0
    gpsLost -> null
    else -> currentSpeedKmh(track, now, flaggedAt, movingAt)
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

// UI 최종 통합안(2026-10-25) — 러닝 판도 다크블루 · 화이트블루 바닥 한 장, 주 = 블루, 보조 = 회색 채움
private val FreeDark = FreeRunColors(
    screen = Color(0xFF070B14), panel = Color(0xFF070B14),
    label = Color(0xFF969CAA), text = Color.White, muted = Color(0xFF6E7586),
    primary = Color(0xFF2F6BFF), primaryInk = Color.White,
    slate = Color(0xFF202736), slateEdge = Color.Transparent, slateInk = Color.White,
    off = Color(0xFF141A27), offInk = Color(0xFF5D6371),
    stopMark = Color(0xFFFF453A),
    band = Color(0xFFFDB829), bandInk = Color(0xFF14171D),
    calmBand = Color(0xF20E1424), calmEdge = Color.Transparent, calmInk = Color.White,
    danger = Color(0xFFFF453A), dangerInk = Color.White, dangerText = Color(0xFFFF453A),
    chip = Color(0xE60E1424), chipIcon = Color(0xFF32D74B), controlEdge = Color.Transparent,
    ring = Color(0x552F6BFF),
)

private val FreeLight = FreeRunColors(
    screen = Color(0xFFF2F2F7), panel = Color(0xFFF2F2F7),
    label = Color(0xFF848489), text = Color.Black, muted = Color(0xFF9C9CA2),
    primary = Color(0xFF1F5EFF), primaryInk = Color.White,
    slate = Color(0xFFE3E3E8), slateEdge = Color.Transparent, slateInk = Color.Black,
    off = Color(0xFFE8E8ED), offInk = Color(0xFFA9A9AE),
    stopMark = Color(0xFFFF3B30),
    band = Color(0xFFFDB829), bandInk = Color(0xFF14171D),
    calmBand = Color(0xF2FFFFFF), calmEdge = Color.Transparent, calmInk = Color.Black,
    danger = Color(0xFFFF3B30), dangerInk = Color.White, dangerText = Color(0xFFFF3B30),
    chip = Color(0xF2FFFFFF), chipIcon = Color(0xFF28B44A), controlEdge = Color.Transparent,
    ring = Color(0x331F5EFF),
)

@Composable
internal fun freeRunColors(): FreeRunColors = if (StepUpColors.dark) FreeDark else FreeLight

/** 자유 러닝 경로 — 달린 길은 블루 실선(UI 최종 통합안 2026-10-25, 예전 형광 초록) */
internal val FreeRunRoute = Color(0xFF2F6BFF)

/** 경로 둘레 — 밝은 지도 · 어두운 지도 모두에서 선이 또렷하게 옅은 흰 테 */
internal val FreeRunRouteOutline = Color(0x99FFFFFF)

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
 * 자유 러닝의 러닝 중 화면 — UI 최종 통합안(2026-10-25 running · paused · end). 상태 없이 그린다.
 * 위 ← · 제목 · ⋯ → GPS 한 줄(칩 아님 — 문제가 있으면 그 줄이 문제를 말한다) → 큰 거리(0.00 km) → 둥근 지도 카드 →
 * 시간 · 페이스 · 속도 → 둥근 컨트롤(달리는 중: 화면 잠금 · 일시정지 · 지도, 멈춤: 길게 눌러 종료 · 재개).
 * 종료는 1초 길게 누른다([onHoldEnd]) — "러닝을 마칠까요?" 확인창을 두지 않는다. 짧게 누르면 "길게 눌러 종료해요".
 * [onPrimary] 는 가운데 파란 원(러닝 시작 · 일시정지 · 재개 · 다시 저장 · 다시 시도), [onSecondary] 는 옆
 * (취소 · 화면에 머물기 · 돌아가기). 저장 중에는 버튼 하나(누를 수 없음)만 둔다.
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
    /** 위치 권한 없이 시간만 기록 — GPS 줄이 "위치 꺼짐" */
    noLocation: Boolean = false,
    onDetails: (() -> Unit)? = null,
    onExpandMap: (() -> Unit)? = null,
    /** 측정 무효 · 속도 오류 줄을 누르면(러닝 정보) */
    onBandTap: (() -> Unit)? = null,
    /** 위치 기능 꺼짐 · 대략적 위치 줄의 "설정" */
    onBandAction: (() -> Unit)? = null,
    /** 저장 실패의 "화면에 머물기" — 한 번 누르면 숨는다(줄과 다시 저장은 남는다) */
    showSecondary: Boolean = true,
    /** 과속 줄 — 러닝이 멈추기까지 남은 초 */
    overspeedLeft: Int = 0,
    /** 위 왼쪽 ← — null 이면 두지 않는다 */
    onBack: (() -> Unit)? = null,
    /** 멈춘 뒤 1초 길게 눌러 종료 — 저장하고 마친다 */
    onHoldEnd: () -> Unit = {},
    /** 위치가 잡혀 있다(GPS 연결됨) · 지금 자리 근처 장소 이름 */
    gpsOk: Boolean = false,
    place: String? = null,
) {
    val live = mode == FreeLiveMode.Live
    var locked by rememberSaveable { mutableStateOf(false) }
    // 멈추거나 끝나면 잠금을 푼다 — 멈춘 화면은 잠그지 않는다
    LaunchedEffect(live, paused) { if (!live || paused) locked = false }
    val title = when (mode) {
        FreeLiveMode.Idle -> stringResource(R.string.runflow_free)
        FreeLiveMode.Saving -> stringResource(R.string.run_saving_title)
        FreeLiveMode.Live -> stringResource(if (paused) R.string.run_paused_title else R.string.run_free_chip_running)
        FreeLiveMode.Failed, FreeLiveMode.DiscardFailed -> stringResource(R.string.run_paused_title)
    }
    val titleTag = when {
        live && band == FreeBand.Paused -> "run-paused-band"
        live && !paused -> "run-status"
        else -> null
    }
    var tip by remember { mutableStateOf<String?>(null) }
    // 길게 누르는 중인가 — 말풍선 글을 고른다
    var heldTip by remember { mutableStateOf(false) }
    LaunchedEffect(tip) { if (tip != null) { kotlinx.coroutines.delay(2_200); tip = null } }
    Box(modifier.fillMaxSize()) {
        if (live) {
            // 2026-10-26 시안 active — 지도가 화면 대부분, 아래 2 × 2 숫자 · 작은 알약 버튼
            FreeRunMapLive(
                title = title, titleTag = titleTag, place = place, numbers = numbers, map = map,
                top = liveTop(band, paused, noLocation, onBandTap, onBandAction, overspeedLeft),
                gpsOk = gpsOk && (band == FreeBand.None || band == FreeBand.Paused),
                noLocation = noLocation && (band == FreeBand.None || band == FreeBand.Paused),
                onBack = onBack, onDetails = onDetails, onExpandMap = onExpandMap,
                paused = paused, onPrimary = onPrimary, onLock = { locked = true },
                onHoldEnd = onHoldEnd, onHint = { tip = it }, onHolding = { holding -> tip = null; heldTip = holding },
            )
        } else FreeRunScaffold(
            rootTag = if (mode == FreeLiveMode.Saving) "run-saving" else "run-live",
            title = title, titleTag = titleTag,
            onBack = onBack.takeIf { mode == FreeLiveMode.Idle || live },
            onMore = onDetails.takeIf { mode == FreeLiveMode.Idle || live },
            moreLabel = stringResource(R.string.run_details), moreTag = "run-details-open",
            status = {
                when (mode) {
                    FreeLiveMode.Failed -> FreeStatusLine(stringResource(R.string.run_free_save_failed), "run-save-error", FreeLineKind.Problem)
                    FreeLiveMode.DiscardFailed -> FreeStatusLine(stringResource(R.string.run_free_delete_failed), "run-discard-failed", FreeLineKind.Problem)
                    FreeLiveMode.Live -> LiveStatus(band, noLocation, gpsOk, place, onBandTap, onBandAction, overspeedLeft)
                    else -> if (gpsOk) GpsLine(place) else Unit
                }
            },
            hero = { DistanceHero(numbers.distance, LiveTags.distance, centered = true) },
            map = map, mapTag = "run-live-map",
            stats = { FreeStats(numbers, LiveTags) },
            notes = {
                when (mode) {
                    // 멈춤 안내 글은 두지 않는다(시안 paused — 제목 "일시정지"가 말한다)
                    FreeLiveMode.Saving -> FreeCaption(stringResource(R.string.run_saving_wait))
                    FreeLiveMode.Failed -> FreeCaption(stringResource(R.string.run_free_save_kept), icon = Icons.Outlined.Info, tag = "run-save-kept")
                    FreeLiveMode.DiscardFailed -> FreeCaption(stringResource(R.string.run_free_try_again), strong = true)
                    else -> Unit
                }
            },
            controls = {
                when (mode) {
                    FreeLiveMode.Idle -> ControlRow {
                        RoundControl(Icons.Rounded.Close, stringResource(R.string.run_cancel), onSecondary, primary = false, tag = "run-finish")
                        RoundControl(Icons.Rounded.PlayArrow, stringResource(R.string.home_start_run), onPrimary, size = 96.dp, tag = "run-primary-action")
                    }
                    FreeLiveMode.Live -> if (paused) {
                        ControlRow {
                            HoldEndControl(onHoldEnd, onHint = { tip = it }, onHolding = { holding -> tip = null; heldTip = holding })
                            RoundControl(Icons.Rounded.PlayArrow, stringResource(R.string.run_resume_short), onPrimary, size = 96.dp, tag = "run-primary-action")
                        }
                    } else {
                        ControlRow {
                            RoundControl(Icons.Outlined.Lock, stringResource(R.string.run_lock), { locked = true }, primary = false, tag = "run-lock")
                            RoundControl(Icons.Rounded.Pause, stringResource(R.string.run_pause), onPrimary, size = 96.dp, tag = "run-primary-action")
                            // 크게 볼 경로가 없으면(위치 없이 시간만 · 아직 점이 없음) 지도 버튼을 두지 않는다
                            if (onExpandMap != null) {
                                RoundControl(Icons.Outlined.Map, stringResource(R.string.run_map_short), onExpandMap, primary = false,
                                    tag = "run-map-expand")
                            }
                        }
                    }
                    FreeLiveMode.Saving -> BottomActionBar {
                        FreeRunButton(stringResource(R.string.run_saving_title), {}, Modifier.weight(1f).testTag("run-primary-action"),
                            kind = FreeButtonKind.Secondary, enabled = false, busy = true)
                    }
                    FreeLiveMode.Failed -> BottomActionBar {
                        if (showSecondary) {
                            FreeRunButton(stringResource(R.string.run_free_save_stay), onSecondary, Modifier.weight(.42f).testTag("run-save-stay"),
                                kind = FreeButtonKind.Secondary)
                        }
                        FreeRunButton(stringResource(R.string.runflow_save_again), onPrimary, Modifier.weight(.58f).testTag("run-primary-action"))
                    }
                    FreeLiveMode.DiscardFailed -> BottomActionBar {
                        FreeRunButton(stringResource(R.string.runflow_discard_back), onSecondary, Modifier.weight(.42f).testTag("run-discard-failed-back"),
                            kind = FreeButtonKind.Secondary)
                        FreeRunButton(stringResource(R.string.run_free_retry), onPrimary, Modifier.weight(.58f).testTag("run-discard-retry"))
                    }
                }
            },
        )
        // 짧게 누름 · 누르는 동안 — 종료 버튼 위 말풍선
        val bubble = if (heldTip) stringResource(R.string.run_hold_tip) else tip
        if (bubble != null && live && paused) {
            HoldBubble(bubble, Modifier.align(Alignment.BottomEnd).navigationBarsPadding().padding(end = 30.dp, bottom = 92.dp).testTag("run-hold-tip"))
        }
        if (locked) LockOverlay(onUnlock = { locked = false })
    }
}

/**
 * 달리는 중 · 일시정지(2026-10-26 시안 active) — 지도가 위를 채우고 왼쪽 위 ← · 제목 알약, 오른쪽 위 GPS 알약(문제가 있으면 그 줄),
 * 아래 판에 이동 거리 · 시간 · 평균 페이스 · 현재 속도(2 × 2), 그 아래 알약 둘.
 * 달리는 중: 일시정지 | 화면 잠금. 멈춤: 다시 달리기 | 길게 눌러 종료(1초) — 종료는 예전처럼 멈춘 뒤에만 있다.
 * 러닝 정보(⋯) · 크게 보기는 지도 위 작은 원 버튼이다.
 */
@Composable
private fun FreeRunMapLive(
    title: String,
    titleTag: String?,
    place: String?,
    numbers: FreeRunNumbers,
    map: @Composable BoxScope.() -> Unit,
    top: (@Composable () -> Unit)?,
    gpsOk: Boolean,
    noLocation: Boolean,
    onBack: (() -> Unit)?,
    onDetails: (() -> Unit)?,
    onExpandMap: (() -> Unit)?,
    paused: Boolean,
    onPrimary: () -> Unit,
    onLock: () -> Unit,
    onHoldEnd: () -> Unit,
    onHint: (String) -> Unit,
    onHolding: (Boolean) -> Unit,
) {
    val c = StepUpUi.colors
    val t = runTone()
    Column(Modifier.fillMaxSize().background(c.background).testTag("run-live")) {
        Box(Modifier.fillMaxWidth().weight(1f).background(c.mapBackground)) {
            CompositionLocalProvider(LocalMapTone provides MapTone(t.mapFilter, t.mapShade, t.mapRoads)) {
                Box(Modifier.fillMaxSize().testTag("run-live-map"), content = map)
            }
            Column(Modifier.fillMaxWidth().statusBarsPadding().padding(horizontal = 14.dp, vertical = 8.dp)) {
                Row(verticalAlignment = Alignment.Top) {
                    if (onBack != null) {
                        com.stepup.android.ui.components.BackButton(onBack, modifier = Modifier.testTag("run-back"))
                        Spacer(Modifier.width(8.dp))
                    }
                    Column(
                        Modifier.clip(RoundedCornerShape(16.dp)).background(c.card.copy(alpha = .94f)).padding(horizontal = 14.dp, vertical = 8.dp),
                    ) {
                        Text(
                            title, style = StepUpUi.type.rowTitle.copy(fontWeight = FontWeight.ExtraBold), color = c.label, maxLines = 1,
                            modifier = Modifier.semantics { heading() }.then(if (titleTag != null) Modifier.testTag(titleTag) else Modifier),
                        )
                        if (place != null) {
                            Text(place, style = StepUpUi.type.caption, color = c.secondaryLabel, maxLines = 1, overflow = TextOverflow.Ellipsis)
                        }
                    }
                    Spacer(Modifier.weight(1f))
                    when {
                        gpsOk -> MapChip(stringResource(R.string.run_gps_connected), c.success, "run-gps-line")
                        noLocation -> MapChip(stringResource(R.string.run_status_no_gps), c.tertiaryLabel, "run-gps-line")
                    }
                }
                if (top != null) {
                    Spacer(Modifier.height(8.dp))
                    top()
                }
            }
            Column(
                Modifier.align(Alignment.BottomEnd).padding(14.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                if (onDetails != null) {
                    CircleIconButton(Icons.Rounded.MoreHoriz, stringResource(R.string.run_details), onDetails,
                        modifier = Modifier.testTag("run-details-open"))
                }
                if (onExpandMap != null) {
                    CircleIconButton(Icons.Outlined.Map, stringResource(R.string.run_map_short), onExpandMap,
                        modifier = Modifier.testTag("run-map-expand"))
                }
            }
        }
        Column(Modifier.fillMaxWidth().navigationBarsPadding().padding(horizontal = 24.dp).padding(top = 16.dp, bottom = 12.dp)) {
            val km = (numbers.distance as? FreeDistance.Km)?.let { "%.2f".format(it.km) } ?: "—"
            val pace = numbers.paceSec?.let { formatPacePrime(it) } ?: "—"
            val speed = numbers.speedKmh?.let { "%.1f".format(it) } ?: "—"
            Row(Modifier.fillMaxWidth()) {
                LiveCell(stringResource(R.string.run_label_distance), km, "km", LiveTags.distance, Modifier.weight(1f))
                LiveCell(stringResource(R.string.stat_time), numbers.time, null, LiveTags.time, Modifier.weight(1f))
            }
            Spacer(Modifier.height(10.dp))
            Row(Modifier.fillMaxWidth()) {
                LiveCell(stringResource(if (numbers.average) R.string.run_label_pace else R.string.run_pace), pace, "/km", LiveTags.pace, Modifier.weight(1f))
                LiveCell(stringResource(if (numbers.average) R.string.run_label_speed else R.string.run_current_speed), speed, stringResource(R.string.run_speed_unit), LiveTags.speed, Modifier.weight(1f))
            }
            Spacer(Modifier.height(16.dp))
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp, Alignment.CenterHorizontally)) {
                LivePill(
                    if (paused) Icons.Rounded.PlayArrow else Icons.Rounded.Pause,
                    stringResource(if (paused) R.string.run_resume_short else R.string.run_pause),
                    primary = true, onClick = onPrimary, modifier = Modifier.weight(1f).testTag("run-primary-action"),
                )
                if (paused) {
                    HoldEndPill(onHoldEnd, onHint, onHolding, Modifier.weight(1f))
                } else {
                    LivePill(Icons.Outlined.Lock, stringResource(R.string.run_lock), primary = false, onClick = onLock,
                        modifier = Modifier.weight(1f).testTag("run-lock"))
                }
            }
        }
    }
}

/** 지도 위 작은 알약 — 점 + 글 */
@Composable
private fun MapChip(text: String, dot: Color, tag: String) {
    val c = StepUpUi.colors
    Row(
        Modifier.clip(RoundedCornerShape(50)).background(c.card.copy(alpha = .94f)).padding(horizontal = 12.dp, vertical = 8.dp)
            .semantics(mergeDescendants = true) { liveRegion = LiveRegionMode.Polite }.testTag(tag),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(Modifier.size(7.dp).clip(CircleShape).background(dot))
        Spacer(Modifier.width(6.dp))
        Text(text, style = StepUpUi.type.caption.copy(fontWeight = FontWeight.SemiBold), color = c.label, maxLines = 1)
    }
}

/** 숫자 칸 — 이름 위, 큰 숫자 + 단위 */
@Composable
private fun LiveCell(label: String, value: String, unit: String?, tag: String, modifier: Modifier) {
    val c = StepUpUi.colors
    Column(modifier.semantics(mergeDescendants = true) { contentDescription = "$label $value ${unit.orEmpty()}".trim() }) {
        Text(label, style = StepUpUi.type.rowSubtitle, color = c.secondaryLabel, maxLines = 1)
        Row(verticalAlignment = Alignment.Bottom) {
            Text(
                value, style = StepUpUi.type.numberStat.copy(fontSize = 38.sp, lineHeight = 40.sp), color = c.label, maxLines = 1, softWrap = false,
                modifier = Modifier.alignByBaseline().testTag(tag),
            )
            if (unit != null) {
                Spacer(Modifier.width(4.dp))
                Text(unit, style = StepUpUi.type.unit, color = c.secondaryLabel, modifier = Modifier.alignByBaseline())
            }
        }
    }
}

/** 작은 알약 버튼(시안 일시정지 · 길게 눌러 종료) — 주 행동은 블루, 다른 하나는 카드 면 */
@Composable
private fun LivePill(icon: ImageVector, label: String, primary: Boolean, onClick: () -> Unit, modifier: Modifier) {
    val c = StepUpUi.colors
    Row(
        modifier.heightIn(min = 52.dp).pressable(onClick = onClick).clip(RoundedCornerShape(50))
            .background(if (primary) c.primary else c.card).then(if (primary) Modifier else Modifier.border(1.dp, c.separator, RoundedCornerShape(50)))
            .padding(horizontal = 16.dp),
        horizontalArrangement = Arrangement.Center, verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(icon, contentDescription = null, tint = if (primary) c.onPrimary else c.label, modifier = Modifier.size(20.dp))
        Spacer(Modifier.width(8.dp))
        Text(label, style = StepUpUi.type.button.copy(fontSize = 16.sp), color = if (primary) c.onPrimary else c.label, maxLines = 1)
    }
}

/** 길게 눌러 종료 알약 — 1초 누르면 마친다. 누르는 동안 알약이 왼쪽부터 찬다. 짧게 누르면 말풍선 안내 */
@Composable
private fun HoldEndPill(onEnd: () -> Unit, onHint: (String) -> Unit, onHolding: (Boolean) -> Unit, modifier: Modifier) {
    val c = StepUpUi.colors
    val hint = stringResource(R.string.run_hold_end_hint)
    val label = stringResource(R.string.run_hold_end)
    val scope = rememberCoroutineScope()
    val progress = remember { androidx.compose.animation.core.Animatable(0f) }
    var done by remember { mutableStateOf(false) }
    val end by androidx.compose.runtime.rememberUpdatedState(onEnd)
    val shape = RoundedCornerShape(50)
    Box(
        modifier.heightIn(min = 52.dp).clip(shape).background(c.card).border(1.dp, c.separator, shape)
            .semantics {
                role = androidx.compose.ui.semantics.Role.Button
                contentDescription = label
                onClick(label = hint) { onHint(hint); true }
                onLongClick(label = label) { if (!done) { done = true; end() }; true }
            }
            .pointerInput(Unit) {
                detectTapGestures(onPress = {
                    onHolding(true)
                    val job = scope.launch {
                        progress.animateTo(1f, androidx.compose.animation.core.tween(1000, easing = androidx.compose.animation.core.LinearEasing))
                        if (!done) { done = true; end() }
                    }
                    tryAwaitRelease()
                    onHolding(false)
                    if (progress.value < 1f) {
                        job.cancel()
                        scope.launch { progress.animateTo(0f, androidx.compose.animation.core.tween(200)) }
                        onHint(hint)
                    }
                })
            }
            .testTag("run-finish"),
        contentAlignment = Alignment.Center,
    ) {
        Box(Modifier.matchParentSize().drawBehind {
            drawRect(c.live.copy(alpha = .22f), size = size.copy(width = size.width * progress.value))
        })
        Row(Modifier.padding(horizontal = 16.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.size(16.dp).border(2.dp, c.label, RoundedCornerShape(3.dp)))
            Spacer(Modifier.width(8.dp))
            Text(label, style = StepUpUi.type.button.copy(fontSize = 16.sp), color = c.label, maxLines = 1)
        }
    }
}

/** 둥근 컨트롤 줄 — 가운데 모아 30dp 간격, 아래 16 */
@Composable
private fun ControlRow(content: @Composable RowScope.() -> Unit) {
    Row(
        Modifier.fillMaxWidth().padding(top = 8.dp, bottom = 16.dp),
        horizontalArrangement = Arrangement.spacedBy(30.dp, Alignment.CenterHorizontally),
        verticalAlignment = Alignment.Top,
        content = content,
    )
}

/** 길게 눌러 종료(시안 paused · end) — 76 링 + 흰 원, 아래 "길게 눌러 종료" / 누르는 동안 "계속 누르고 있어요" */
@Composable
private fun HoldEndControl(onEnd: () -> Unit, onHint: (String) -> Unit, onHolding: (Boolean) -> Unit) {
    val c = StepUpUi.colors
    val hint = stringResource(R.string.run_hold_end_hint)
    var holding by remember { mutableStateOf(false) }
    Column(Modifier.padding(top = 10.dp), horizontalAlignment = Alignment.CenterHorizontally) {
        HoldToEndButton(
            onEnd = onEnd, onHint = { onHint(hint) }, size = 76.dp,
            contentDescription = stringResource(R.string.run_hold_end), hintLabel = hint,
            onHolding = { holding = it; onHolding(it) },
            modifier = Modifier.testTag("run-finish"),
        )
        Spacer(Modifier.height(10.dp))
        Text(
            stringResource(if (holding) R.string.run_holding else R.string.run_hold_end),
            style = StepUpUi.type.caption.copy(fontSize = 12.5.sp), color = c.secondaryLabel, maxLines = 1,
        )
    }
}

/** 화면 잠금 — 달리는 동안 주머니 속 잘못 누름을 막는다. 숫자는 비쳐 보이고, 1초 길게 눌러 푼다. 뒤로 가기도 막는다 */
@Composable
private fun LockOverlay(onUnlock: () -> Unit) {
    androidx.activity.compose.BackHandler { }
    Box(
        Modifier.fillMaxSize().background(Color.Black.copy(alpha = .55f))
            .pointerInput(Unit) { awaitPointerEventScope { while (true) awaitPointerEvent().changes.forEach { it.consume() } } }
            .testTag("run-locked"),
        contentAlignment = Alignment.BottomCenter,
    ) {
        Column(Modifier.navigationBarsPadding().padding(bottom = 40.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Text(stringResource(R.string.run_lock_title), style = StepUpUi.type.navTitle, color = Color.White)
            Spacer(Modifier.height(16.dp))
            HoldToEndButton(
                onEnd = onUnlock, onHint = {}, size = 76.dp, icon = Icons.Outlined.LockOpen,
                contentDescription = stringResource(R.string.run_unlock_hold), modifier = Modifier.testTag("run-unlock"),
            )
            Spacer(Modifier.height(10.dp))
            Text(stringResource(R.string.run_unlock_hold), style = StepUpUi.type.caption, color = Color.White.copy(alpha = .8f))
        }
    }
}

/** 달리는 중 GPS 줄 — 문제가 있으면 그 문제 한 줄(누르면 까닭 · 오른쪽 "설정"), 없으면 "GPS 연결됨 · 장소" */
@Composable
private fun LiveStatus(
    band: FreeBand,
    noLocation: Boolean,
    gpsOk: Boolean,
    place: String?,
    onBandTap: (() -> Unit)?,
    onBandAction: (() -> Unit)?,
    overspeedLeft: Int,
) {
    when (band) {
        FreeBand.None, FreeBand.Paused -> when {
            noLocation -> FreeStatusLine(stringResource(R.string.run_status_no_gps), "run-gps-line", FreeLineKind.Off)
            gpsOk -> GpsLine(place)
            // 위치를 찾는 동안은 아무것도 적지 않는다(2026-10-17 "지도 앱처럼")
            else -> Unit
        }
        FreeBand.Overspeed -> FreeStatusLine(stringResource(R.string.run_overspeed_band, overspeedLeft), "run-overspeed", FreeLineKind.Problem, onClick = onBandTap)
        FreeBand.LocationOff -> FreeStatusLine(stringResource(R.string.run_free_location_off), "run-location-off", FreeLineKind.Problem,
            action = stringResource(R.string.run_free_settings), onAction = onBandAction)
        FreeBand.MeasureInvalid -> FreeStatusLine(stringResource(R.string.run_free_measure_invalid), "run-void-banner", FreeLineKind.Problem, onClick = onBandTap)
        FreeBand.SpeedError -> FreeStatusLine(stringResource(R.string.run_speed_error), "run-speed-banner", FreeLineKind.Problem, onClick = onBandTap)
        FreeBand.Still -> FreeStatusLine(stringResource(R.string.run_still_title), "run-still", FreeLineKind.Warn,
            action = stringResource(R.string.run_pause), onAction = onBandAction)
        FreeBand.Weak -> FreeStatusLine(stringResource(R.string.run_free_gps_weak), "run-gps-weak", FreeLineKind.Warn)
        FreeBand.Approx -> FreeStatusLine(stringResource(R.string.run_gps_approx), "run-location-approx", FreeLineKind.Warn,
            action = stringResource(R.string.run_free_settings), onAction = onBandAction)
        FreeBand.Worn -> FreeStatusLine(stringResource(R.string.run_shoe_worn_band), "run-shoe-worn", FreeLineKind.Warn, onClick = onBandTap)
    }
}

/** 지도 위에 얹는 문제 한 줄(크루 달리기) — 같은 글 · 점을 카드 면 위에. 문제가 없으면 null */
internal fun liveTop(
    band: FreeBand,
    paused: Boolean,
    noLocation: Boolean,
    onBandTap: (() -> Unit)?,
    onBandAction: (() -> Unit)?,
    overspeedLeft: Int = 0,
): (@Composable () -> Unit)? = if (band == FreeBand.None || band == FreeBand.Paused) null else {
    {
        Box(Modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp)).background(StepUpUi.colors.card.copy(alpha = .94f)).padding(vertical = 4.dp)) {
            LiveStatus(band, noLocation, gpsOk = false, place = null, onBandTap = onBandTap, onBandAction = onBandAction, overspeedLeft = overspeedLeft)
        }
    }
}

@Composable
private fun GpsLine(place: String?) {
    val text = stringResource(R.string.run_gps_connected) + (place?.let { " · $it" } ?: "")
    FreeStatusLine(text, "run-gps-line", FreeLineKind.Ok)
}

internal enum class FreeLineKind { Ok, Off, Warn, Problem }

/**
 * GPS 한 줄(시안 running — 칩이 아니라 점 + 글자). 문제는 빨강 · 주의는 노랑 점, 글은 늘 같은 색 + 점(색만으로 알리지 않게 글이 말한다).
 * [onClick] 이 있으면 줄을 눌러 까닭(러닝 정보)을 연다. [action] 은 줄 끝 파란 글자 버튼(설정 · 일시정지). 바뀌면 화면 낭독이 읽는다.
 */
@Composable
internal fun FreeStatusLine(
    text: String,
    tag: String,
    kind: FreeLineKind,
    onClick: (() -> Unit)? = null,
    action: String? = null,
    onAction: (() -> Unit)? = null,
) {
    val c = StepUpUi.colors
    val dot = when (kind) {
        FreeLineKind.Ok -> c.success
        FreeLineKind.Off -> c.tertiaryLabel
        FreeLineKind.Warn -> Color(0xFFFFB020)
        FreeLineKind.Problem -> c.live
    }
    Row(
        Modifier.fillMaxWidth().heightIn(min = 30.dp).padding(horizontal = 20.dp),
        horizontalArrangement = Arrangement.Center, verticalAlignment = Alignment.CenterVertically,
    ) {
        Row(
            Modifier.weight(1f, fill = false).clip(RoundedCornerShape(10.dp))
                .then(if (onClick != null) Modifier.feedbackClickable(onClick = onClick) else Modifier)
                .semantics(mergeDescendants = true) { liveRegion = LiveRegionMode.Polite }
                .padding(horizontal = 4.dp, vertical = 4.dp)
                .testTag(tag),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(Modifier.size(7.dp).clip(CircleShape).background(dot))
            Spacer(Modifier.width(6.dp))
            Text(
                text, style = StepUpUi.type.caption.copy(fontWeight = FontWeight.SemiBold),
                color = if (kind == FreeLineKind.Problem) c.label else c.secondaryLabel, maxLines = 2, overflow = TextOverflow.Ellipsis,
            )
        }
        if (action != null && onAction != null) {
            Box(
                Modifier.heightIn(min = 48.dp).clip(RoundedCornerShape(12.dp)).feedbackClickable(onClick = onAction)
                    .padding(horizontal = 10.dp).testTag("$tag-action"),
                contentAlignment = Alignment.Center,
            ) {
                Text(action, style = StepUpUi.type.smallButton, color = c.accentText, maxLines = 1)
            }
        }
    }
}

/** 큰 거리(시안 132 · km 26). 모르면 "—" */
@Composable
private fun DistanceHero(distance: FreeDistance, tag: String, centered: Boolean, compact: Boolean = false) {
    val c = StepUpUi.colors
    val value = (distance as? FreeDistance.Km)?.let { "%.2f".format(it.km) } ?: "—"
    val label = stringResource(R.string.run_label_distance)
    Row(
        Modifier.fillMaxWidth().padding(horizontal = 20.dp).semantics(mergeDescendants = true) { contentDescription = "$label $value km" },
        horizontalArrangement = if (centered) Arrangement.Center else Arrangement.Start,
        verticalAlignment = Alignment.Bottom,
    ) {
        Text(
            value, style = StepUpUi.type.numberHero.copy(fontSize = if (compact) 104.sp else 132.sp, letterSpacing = .005.em),
            color = c.label, maxLines = 1, softWrap = false,
            modifier = Modifier.alignByBaseline().testTag(tag),
        )
        Spacer(Modifier.width(6.dp))
        Text("km", style = StepUpUi.type.navTitle.copy(fontSize = 26.sp, fontWeight = FontWeight.SemiBold), color = c.secondaryLabel,
            modifier = Modifier.alignByBaseline())
    }
}

/** 시간 · 페이스 · 속도(시안 숫자 줄, 상자 없음). 멈춘 뒤에는 평균 */
@Composable
private fun FreeStats(numbers: FreeRunNumbers, tags: FreeTags) {
    val pace = numbers.paceSec?.let { formatPacePrime(it) } ?: "—"
    val speed = numbers.speedKmh?.let { "%.1f".format(it) } ?: "—"
    StatRow(
        listOf(
            StatItem(stringResource(R.string.stat_time), numbers.time, tag = tags.time),
            StatItem(stringResource(if (numbers.average) R.string.run_label_pace else R.string.run_pace), pace, tag = tags.pace),
            StatItem(stringResource(if (numbers.average) R.string.run_label_speed else R.string.run_current_speed), speed,
                stringResource(R.string.run_speed_unit), tag = tags.speed),
        ),
        numberStyle = StepUpUi.type.numberStat,
        modifier = Modifier.padding(horizontal = 20.dp),
    )
}

// ── 결과 ───────────────────────────────────────────────────────────

/**
 * 러닝 완료(시안 result) — 위 ← · 러닝 완료 · ⋯ → "기록 저장 완료 · 시각 · 장소" → 큰 거리 → 시간 · 평균 페이스 · 평균 속도 →
 * 둥근 지도(달린 길 블루 실선) → 알림 줄(패널티 · 내구도) → 기록 공유 | 완료. 보상 상태와 상관없이 같은 모습이다.
 * 덜 쓰는 "내 러닝 기록 보기" · "삭제"는 ⋯ 안에(지우지 않는다). [map] 이 null 이면 저장된 경로가 없다 — 지도를 꾸미지 않는다.
 * [deleteFailed] — 기록 삭제가 실패. 기록은 그대로 있고 다시 시도하거나 돌아간다.
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
    /** 과속이 30초 이어져 앱이 멈춘 러닝 — 패널티 안내(내구도 절반 · 10분 러닝 금지) */
    penalty: String? = null,
    /** 이 러닝에서 신발 내구도를 다 썼다 — 그 뒤 걸음은 보상에 들지 않았다는 안내 */
    shoeNote: String? = null,
    /** 시작 시각(ms) · 시작 자리 근처 장소 — 위 한 줄 */
    startedAt: Long = 0L,
    place: String? = null,
) {
    val c = StepUpUi.colors
    var more by rememberSaveable { mutableStateOf(false) }
    FreeRunScaffold(
        rootTag = "run-result",
        title = stringResource(R.string.finish_title), titleTag = "run-result-title",
        onBack = onDone.takeIf { !deleteFailed }, onMore = if (deleteFailed) null else ({ more = true }),
        moreLabel = stringResource(R.string.run_free_more), moreTag = "run-result-more",
        status = {
            Row(Modifier.fillMaxWidth().padding(horizontal = 20.dp).heightIn(min = 26.dp), verticalAlignment = Alignment.CenterVertically) {
                Text(stringResource(R.string.run_free_record_saved), style = StepUpUi.type.rowSubtitle.copy(fontSize = 14.sp),
                    color = c.secondaryLabel, maxLines = 1, modifier = Modifier.testTag("run-result-saved"))
                val rest = listOfNotNull(
                    startedAt.takeIf { it > 0 }?.let {
                        java.time.Instant.ofEpochMilli(it).atZone(java.time.ZoneId.systemDefault()).toLocalTime()
                            .format(java.time.format.DateTimeFormatter.ofLocalizedTime(java.time.format.FormatStyle.SHORT))
                    },
                    place,
                )
                if (rest.isNotEmpty()) {
                    Text(rest.joinToString(" · ", prefix = " · "), style = StepUpUi.type.rowSubtitle.copy(fontSize = 14.sp),
                        color = c.secondaryLabel, maxLines = 1, overflow = TextOverflow.Ellipsis)
                }
            }
        },
        hero = { DistanceHero(numbers.distance, ResultTags.distance, centered = false) },
        statsFirst = true,
        map = {
            if (map != null) {
                map()
            } else {
                Box(Modifier.fillMaxSize().background(c.mapBackground)) {
                    RunMapPlaceholder(
                        Icons.Outlined.Map, stringResource(R.string.rec_no_route_title), stringResource(R.string.run_free_no_route_body),
                        Modifier.testTag("run-result-no-route"),
                    )
                }
            }
        },
        mapTag = null,
        stats = { FreeStats(numbers, ResultTags) },
        notes = {
            if (deleteFailed) ResultNote(stringResource(R.string.run_free_delete_failed), stringResource(R.string.run_free_try_again), "run-delete-failed")
            if (penalty != null) ResultNote(penalty, null, "run-result-overspeed")
            if (shoeNote != null) ResultNote(shoeNote, null, "run-result-shoe-worn")
        },
        controls = {
            BottomActionBar {
                if (deleteFailed) {
                    FreeRunButton(stringResource(R.string.runflow_discard_back), onDeleteBack, Modifier.weight(.42f).testTag("run-delete-back"),
                        kind = FreeButtonKind.Secondary, enabled = !deleting)
                    FreeRunButton(stringResource(R.string.run_free_retry), onRetryDelete, Modifier.weight(.58f).testTag("run-delete-retry"),
                        busy = deleting)
                } else {
                    FreeRunButton(stringResource(R.string.run_share_record), onShare, Modifier.weight(.38f).testTag("run-result-share"),
                        kind = FreeButtonKind.Secondary, icon = Icons.Outlined.IosShare)
                    FreeRunButton(stringResource(R.string.finish_done), onDone, Modifier.weight(.62f).testTag("run-result-done"))
                }
            }
        },
    )
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

/** 결과 알림 줄(시안 result 빨간 줄) — 옅은 빨강 면 · 경고 아이콘 · 굵은 글 + 작은 글 */
@Composable
private fun ResultNote(title: String, body: String?, tag: String) {
    val c = StepUpUi.colors
    Row(
        Modifier.fillMaxWidth().padding(horizontal = 16.dp).padding(top = 12.dp).clip(RoundedCornerShape(16.dp)).background(c.errorFill)
            .padding(horizontal = 16.dp, vertical = 13.dp).testTag(tag),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(Icons.Filled.Error, contentDescription = null, tint = c.live, modifier = Modifier.size(20.dp))
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Text(title, style = StepUpUi.type.rowTitle.copy(fontSize = 14.5.sp), color = c.live)
            if (body != null) Text(body, style = StepUpUi.type.caption.copy(fontSize = 12.5.sp), color = c.live.copy(alpha = .85f))
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
 * 러닝 화면 틀(시안 running · result) — 위 ← · 제목 · ⋯(44) → 상태 한 줄 → 큰 거리 → 둥근 지도 카드(남는 높이) → 숫자 줄 → 알림 →
 * 컨트롤. 결과는 숫자 줄이 지도 위([statsFirst]). 낮은 화면(큰 글씨 포함)은 지도가 먼저 줄고, 그래도 모자라면 넘긴다.
 */
@Composable
private fun FreeRunScaffold(
    rootTag: String,
    title: String,
    titleTag: String?,
    onBack: (() -> Unit)?,
    onMore: (() -> Unit)?,
    moreLabel: String,
    moreTag: String,
    status: @Composable () -> Unit,
    hero: @Composable () -> Unit,
    map: @Composable BoxScope.() -> Unit,
    mapTag: String?,
    stats: @Composable () -> Unit,
    notes: @Composable ColumnScope.() -> Unit,
    controls: @Composable () -> Unit,
    statsFirst: Boolean = false,
) {
    val c = StepUpUi.colors
    val t = runTone()
    BoxWithConstraints(Modifier.fillMaxSize().background(c.background).testTag(rootTag)) {
        val tall = maxHeight >= 640.dp
        Column(Modifier.fillMaxSize().statusBarsPadding().navigationBarsPadding()) {
            Box(Modifier.fillMaxWidth().height(44.dp).padding(horizontal = 12.dp)) {
                if (onBack != null) {
                    com.stepup.android.ui.components.BackButton(onBack, modifier = Modifier.align(Alignment.CenterStart).testTag("run-back"))
                }
                Text(
                    title, style = StepUpUi.type.navTitle, color = c.label, maxLines = 1, overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.align(Alignment.Center).padding(horizontal = 56.dp).semantics { heading() }
                        .then(if (titleTag != null) Modifier.testTag(titleTag) else Modifier),
                )
                if (onMore != null) {
                    CircleIconButton(Icons.Rounded.MoreHoriz, moreLabel, onMore, modifier = Modifier.align(Alignment.CenterEnd).testTag(moreTag))
                }
            }
            Column(
                Modifier.fillMaxWidth().weight(1f).then(if (tall) Modifier else Modifier.verticalScroll(rememberScrollState())),
            ) {
                status()
                if (statsFirst) Spacer(Modifier.height(2.dp)) else Spacer(Modifier.height(4.dp))
                hero()
                if (statsFirst) {
                    Spacer(Modifier.height(14.dp))
                    stats()
                }
                // 지도 카드 361 × 180(결과 190) · 모서리 22 — 시안 높이 그대로, 남는 높이는 컨트롤 위 빈칸이 된다
                Box(
                    Modifier.padding(horizontal = StepUpUi.Gutter).padding(top = 20.dp).fillMaxWidth()
                        .height(if (statsFirst) 190.dp else 180.dp)
                        .clip(RoundedCornerShape(22.dp)).background(c.mapBackground),
                ) {
                    // 실제 지도 타일을 러닝 화면의 지도 색으로(지도 틀 RunMapFrame 과 같은 색)
                    CompositionLocalProvider(LocalMapTone provides MapTone(t.mapFilter, t.mapShade, t.mapRoads)) {
                        Box(Modifier.fillMaxSize().then(if (mapTag != null) Modifier.testTag(mapTag) else Modifier), content = map)
                    }
                }
                if (!statsFirst) {
                    Spacer(Modifier.height(20.dp))
                    stats()
                }
                notes()
                if (tall) Spacer(Modifier.weight(1f)) else Spacer(Modifier.height(8.dp))
            }
            controls()
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

/** 판의 버튼 — 통합안 버튼: 주 = 블루 알약 + 원 화살표, 보조 = 회색 알약(앞에 빨간 네모를 둘 수 있다), 위험 = 회색 알약 + 빨간 글자 */
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
    when (kind) {
        FreeButtonKind.Primary -> PrimaryButton(
            label, onClick, modifier,
            state = when {
                busy -> ButtonState.Loading
                !enabled -> ButtonState.Disabled
                else -> ButtonState.Idle
            },
            icon = icon,
        )
        FreeButtonKind.Secondary -> if (stopMark && !busy) {
            Row(
                modifier.heightIn(min = 54.dp).clip(RoundedCornerShape(50))
                    .background(com.stepup.android.ui.components.StepUpUi.colors.fill)
                    .feedbackClickable(enabled = enabled, onClick = onClick)
                    .padding(horizontal = 20.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center,
            ) {
                Box(Modifier.size(14.dp).clip(RoundedCornerShape(3.dp)).background(com.stepup.android.ui.components.StepUpUi.colors.live))
                Spacer(Modifier.width(10.dp))
                Text(
                    label, style = com.stepup.android.ui.components.StepUpUi.type.button,
                    color = if (enabled) com.stepup.android.ui.components.StepUpUi.colors.label else com.stepup.android.ui.components.StepUpUi.colors.onDisabled,
                    maxLines = 1,
                )
            }
        } else SecondaryBusyButton(label, onClick, modifier, enabled = enabled, busy = busy, icon = icon)
        FreeButtonKind.Danger -> DangerButton(label, onClick, modifier, enabled = enabled, busy = busy, icon = icon)
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
