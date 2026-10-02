package com.stepup.android.ui.screens.walk

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.DirectionsRun
import androidx.compose.material.icons.automirrored.filled.DirectionsWalk
import androidx.compose.material.icons.filled.Block
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Groups
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.MoreHoriz
import androidx.compose.material.icons.filled.SignalCellularAlt
import androidx.compose.material.icons.outlined.BarChart
import androidx.compose.material.icons.outlined.CardGiftcard
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.IosShare
import androidx.compose.material.icons.outlined.LocationOff
import androidx.compose.material.icons.outlined.Schedule
import androidx.compose.material.icons.outlined.SignalCellularConnectedNoInternet0Bar
import androidx.compose.material3.Icon
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.stepup.android.R
import com.stepup.android.domain.Sneaker
import com.stepup.android.domain.tier
import com.stepup.android.ui.components.RunButton
import com.stepup.android.ui.components.RunButtonKind
import com.stepup.android.ui.components.RunCard
import com.stepup.android.ui.components.RunChip
import com.stepup.android.ui.components.RunDivider
import com.stepup.android.ui.components.RunHeaderAction
import com.stepup.android.ui.components.RunHeadline
import com.stepup.android.ui.components.RunHero
import com.stepup.android.ui.components.RunInitials
import com.stepup.android.ui.components.RunMapFrame
import com.stepup.android.ui.components.RunMapPlaceholder
import com.stepup.android.ui.components.RunMeter
import com.stepup.android.ui.components.RunNotice
import com.stepup.android.ui.components.RunNoticeKind
import com.stepup.android.ui.components.RunNumber
import com.stepup.android.ui.components.RunPair
import com.stepup.android.ui.components.RunPillKind
import com.stepup.android.ui.components.RunSheetText
import com.stepup.android.ui.components.RunSpec
import com.stepup.android.ui.components.RunSpinner
import com.stepup.android.ui.components.RunStat
import com.stepup.android.ui.components.RunStatRow
import com.stepup.android.ui.components.RunStatusPill
import com.stepup.android.ui.components.RunTextAction
import com.stepup.android.ui.components.RunTileButton
import com.stepup.android.ui.components.RunTopBar
import com.stepup.android.ui.components.RunBackdrop
import com.stepup.android.ui.components.ShoeArtThumbnail
import com.stepup.android.ui.components.ShoeGradeBadge
import com.stepup.android.ui.components.runTextStyle
import com.stepup.android.ui.components.runTone
import com.stepup.android.ui.experience.feedbackClickable
import com.stepup.android.ui.screens.items.durabilityPoints
import com.stepup.android.ui.screens.items.shoeName
import com.stepup.android.ui.theme.StepUpSans

/*
 * 러닝 중 · 저장 · 결과 화면(2026-10-02 러닝 109장 전달본) — 상태 없이 그리는 몸통.
 *
 * 러닝 중(R02 · R02_TIME · R03 · U03 · C04 · C05 · K04 · D07~D12 · CR09 · CR11 · CR18): 머리(뒤로 · 로고 · 상세) · 제목 ·
 * 상태 알약과 GPS · 큰 시간(또는 거리 · 구간 시간) · 거리 | 평균 페이스 · 안내 띠 · 지도 틀 · 일시정지(주) | 러닝 마치기(보조).
 * 결과(R05 · E08~E11 · C02 · K06 · D13 · S02 · CR13): 제목 · 저장 완료 · 큰 거리 · 수치 · 지도 · 신발 · 보상 · 공유 | 기록 · 처음 화면으로.
 *
 * 수는 모두 이번 러닝의 실제 값이다. 보상은 서버가 확인하기 전에는 "예상 · 확인 중"으로만 적는다.
 */

// ── 러닝 중 ────────────────────────────────────────────────────────

internal enum class GpsBadge { Connected, Searching, Weak, Off, Approx, None }

/** 큰 수 자리 — 시간(달린 시간 · 경과 시간 · 구간 남은 시간) 또는 거리(1km · 3km 챌린지) */
@Immutable
internal sealed interface LiveHero {
    data class Clock(
        val label: String,
        val value: String,
        val caption: String? = null,
        /** 다이어트 구간 이름처럼 시안 파랑으로 쓰는 위 이름 */
        val accentLabel: Boolean = false,
    ) : LiveHero

    data class Distance(val label: String, val value: String, val unit: String) : LiveHero
}

@Immutable
internal data class LiveRunUi(
    val title: String,
    val subtitle: String? = null,
    val paused: Boolean = false,
    /** 상태 알약 글 — 기록 중 · 위치 없이 기록 중 · 진행 중 · 기록이 멈춰 있어요 */
    val status: String,
    val gps: GpsBadge = GpsBadge.None,
    val hero: LiveHero,
    val stats: List<RunStat> = emptyList(),
    /** 목표 · 구간 진행 막대 */
    val meter: Float? = null,
    val meterInside: String? = null,
    val meterCaption: String? = null,
    /** 다이어트 "다음 | 1분 러닝" */
    val next: String? = null,
    val primaryLabel: String,
    val primaryIcon: ImageVector?,
    val finishLabel: String,
    /** 다이어트 구간 진행 줄(전체 진행 시간 · 이동 거리)을 큰 시간 아래에 둔다 */
    val dietLayout: Boolean = false,
    /** 멈춤 알약을 가운데에(R03). 마칠지 묻는 동안(R04 · R07)은 왼쪽 알약 · GPS 줄 그대로 */
    val pillCentered: Boolean = paused,
    /** 상태 알약 · GPS 줄을 숨긴다(다이어트 구간 화면 — 구간 이름이 그 자리를 맡는다) */
    val showStatus: Boolean = true,
)

/**
 * 러닝 중 화면 — 상태 없이 그린다. 지도 틀 안([map])과 안내 띠([notices])는 부르는 쪽이 채운다.
 * 큰 글씨 · 낮은 화면이면 위쪽을 넘기고 버튼은 늘 아래에 둔다.
 */
@Composable
internal fun RunLiveContent(
    ui: LiveRunUi,
    onBack: () -> Unit,
    onDetails: () -> Unit,
    onPrimary: () -> Unit,
    onFinish: () -> Unit,
    onExpandMap: (() -> Unit)?,
    modifier: Modifier = Modifier,
    notices: @Composable ColumnScope.() -> Unit = {},
    map: (@Composable BoxScope.() -> Unit)?,
    primaryEnabled: Boolean = true,
    /** 아래 보조 버튼(러닝 마치기) — 저장 실패(S01)처럼 주 행동 하나만 둘 때 끈다 */
    showFinish: Boolean = true,
) {
    val t = runTone()
    Box(modifier.fillMaxSize().testTag("run-live")) {
        RunBackdrop(Modifier.fillMaxSize())
        Column(Modifier.fillMaxSize().safeDrawingPadding()) {
            RunTopBar(
                onBack = onBack,
                trailing = {
                    Box(
                        Modifier.size(48.dp).clip(CircleShape).feedbackClickable(onClick = onDetails)
                            .testTag("run-details-open"),
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(Icons.Filled.MoreHoriz, stringResource(R.string.run_details), tint = t.text, modifier = Modifier.size(26.dp))
                    }
                },
            )
            BoxWithConstraints(Modifier.weight(1f).fillMaxWidth()) {
                val compact = maxHeight < 560.dp
                val mapMin = if (compact) 120.dp else 150.dp
                Column(
                    Modifier.fillMaxSize()
                        .then(if (compact) Modifier.verticalScroll(rememberScrollState()) else Modifier)
                        .padding(horizontal = RunSpec.Gutter),
                ) {
                    RunHeadline(ui.title, subtitle = ui.subtitle, titleTag = "run-title")
                    Spacer(Modifier.height(10.dp))
                    if (ui.showStatus) {
                        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                            if (ui.paused && ui.pillCentered) {
                                Spacer(Modifier.weight(1f))
                                RunStatusPill(ui.status, RunPillKind.Paused, modifier = Modifier.testTag("run-status"))
                                Spacer(Modifier.weight(1f))
                            } else {
                                RunStatusPill(ui.status, if (ui.paused) RunPillKind.Pending else RunPillKind.Live,
                                    modifier = Modifier.testTag("run-status"))
                                Spacer(Modifier.weight(1f))
                                GpsIndicator(ui.gps)
                            }
                        }
                    }
                    Spacer(Modifier.height(if (compact) 6.dp else 10.dp))
                    LiveHeroView(ui, compact)
                    if (ui.stats.isNotEmpty() && !ui.dietLayout) {
                        Spacer(Modifier.height(if (compact) 6.dp else 10.dp))
                        RunStatRow(ui.stats, valueSize = if (compact) 34.sp else 42.sp, modifier = Modifier.testTag("run-stats"))
                    }
                    Column(Modifier.fillMaxWidth().padding(top = 10.dp), verticalArrangement = Arrangement.spacedBy(8.dp), content = notices)
                    Spacer(Modifier.height(10.dp))
                    if (map != null) {
                        RunMapFrame(
                            modifier = Modifier.fillMaxWidth()
                                .then(if (compact) Modifier.height(mapMin) else Modifier.weight(1f).heightIn(min = mapMin))
                                .testTag("run-live-map"),
                            onExpand = onExpandMap,
                            content = map,
                        )
                    } else {
                        Spacer(Modifier.weight(1f))
                    }
                }
            }
            Column(
                Modifier.fillMaxWidth().padding(horizontal = RunSpec.Gutter).padding(top = 12.dp, bottom = 12.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                RunButton(
                    ui.primaryLabel, onPrimary, icon = ui.primaryIcon, enabled = primaryEnabled, hero = true,
                    modifier = Modifier.testTag("run-primary-action"),
                )
                if (showFinish) {
                    RunButton(ui.finishLabel, onFinish, kind = RunButtonKind.Secondary, modifier = Modifier.testTag("run-finish"))
                }
            }
        }
    }
}

@Composable
private fun LiveHeroView(ui: LiveRunUi, compact: Boolean) {
    val t = runTone()
    when (val hero = ui.hero) {
        is LiveHero.Clock -> {
            RunHero(
                label = hero.label, value = hero.value, caption = if (ui.dietLayout) null else hero.caption,
                size = if (compact) 72.sp else 96.sp,
                labelColor = if (hero.accentLabel) t.cyan else null,
            )
            if (ui.dietLayout) {
                if (hero.caption != null) {
                    Text(hero.caption, style = runTextStyle(15.sp, t.label, FontWeight.Medium), textAlign = TextAlign.Center,
                        modifier = Modifier.fillMaxWidth())
                }
                if (ui.next != null) {
                    Spacer(Modifier.height(10.dp))
                    NextChip(ui.next)
                }
                Spacer(Modifier.height(12.dp))
                RunStatRow(ui.stats, valueSize = if (compact) 28.sp else 32.sp, modifier = Modifier.testTag("run-stats"))
                if (ui.meter != null) {
                    Spacer(Modifier.height(8.dp))
                    RunMeter(ui.meter, height = 10.dp, fill = listOf(t.cyan, t.cyan), modifier = Modifier.testTag("run-goal-bar"))
                }
            } else if (ui.meter != null) {
                Spacer(Modifier.height(10.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    RunMeter(ui.meter, height = 18.dp, inside = ui.meterInside, modifier = Modifier.weight(1f).testTag("run-goal-bar"))
                    if (ui.meterCaption != null) {
                        Spacer(Modifier.width(12.dp))
                        Text(ui.meterCaption, style = runTextStyle(14.sp, t.text, FontWeight.SemiBold), textAlign = TextAlign.End,
                            modifier = Modifier.widthIn(max = 120.dp).testTag("run-goal-left"))
                    }
                }
            }
        }
        is LiveHero.Distance -> {
            Text(hero.label, style = runTextStyle(17.sp, t.label, FontWeight.SemiBold), textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth())
            RunNumber(
                hero.value, unit = hero.unit, size = if (compact) 72.sp else 90.sp, unitSize = 32.sp,
                color = t.text, unitColor = t.label,
                align = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth(), valueTag = "run-hero-value",
            )
            if (ui.meter != null) {
                Spacer(Modifier.height(8.dp))
                RunMeter(ui.meter, height = 18.dp, inside = ui.meterInside, modifier = Modifier.testTag("run-goal-bar"))
                if (ui.meterCaption != null) {
                    Text(ui.meterCaption, style = runTextStyle(15.sp, t.cyan, FontWeight.Bold), textAlign = TextAlign.End,
                        modifier = Modifier.fillMaxWidth().padding(top = 4.dp).testTag("run-goal-left"))
                }
            }
        }
    }
}

/** 다이어트 "다음 | 1분 러닝" — 예고일 뿐 누르는 곳이 아니다 */
@Composable
private fun NextChip(text: String) {
    val t = runTone()
    val shape = RoundedCornerShape(12.dp)
    Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
        Row(
            Modifier.clip(shape).background(t.inset, shape).border(1.dp, t.panelEdge, shape)
                .padding(horizontal = 22.dp, vertical = 8.dp).testTag("run-diet-next"),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(stringResource(R.string.run_diet_next), style = runTextStyle(15.sp, t.label, FontWeight.SemiBold))
            Box(Modifier.padding(horizontal = 14.dp).width(1.dp).height(16.dp).background(t.divider))
            Text(text, style = runTextStyle(16.sp, t.text, FontWeight.Bold))
        }
    }
}

/** GPS 상태 — 연결됨(시안 · 막대) · 찾는 중 · 약함 · 꺼짐 · 대략적 위치 */
@Composable
private fun GpsIndicator(gps: GpsBadge) {
    if (gps == GpsBadge.None) return
    val t = runTone()
    val (text, color) = when (gps) {
        GpsBadge.Connected -> stringResource(R.string.run_gps_connected) to t.cyan
        GpsBadge.Searching -> stringResource(R.string.run_gps_searching) to t.label
        GpsBadge.Weak -> stringResource(R.string.run_gps_weak) to t.label
        GpsBadge.Off -> stringResource(R.string.run_gps_off) to t.muted
        GpsBadge.Approx -> stringResource(R.string.run_gps_approx) to t.label
        GpsBadge.None -> "" to t.label
    }
    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.testTag("run-gps-status")) {
        Icon(if (gps == GpsBadge.Off) Icons.Outlined.LocationOff else Icons.Filled.LocationOn, null, tint = color,
            modifier = Modifier.size(18.dp))
        Spacer(Modifier.width(4.dp))
        Text(text, style = runTextStyle(14.sp, color, FontWeight.SemiBold))
        if (gps == GpsBadge.Connected || gps == GpsBadge.Weak) {
            Spacer(Modifier.width(4.dp))
            Icon(
                if (gps == GpsBadge.Connected) Icons.Filled.SignalCellularAlt else Icons.Outlined.SignalCellularConnectedNoInternet0Bar,
                null, tint = color, modifier = Modifier.size(18.dp),
            )
        }
    }
}

/** 위치 없이 기록할 때 지도 자리 — 가짜 지도를 그리지 않는다 */
@Composable
internal fun BoxScope.NoLocationMap() {
    RunMapPlaceholder(
        Icons.Outlined.LocationOff, stringResource(R.string.run_no_location_title), stringResource(R.string.run_no_location_body),
        Modifier.testTag("run-no-location"),
    )
}

/** 크루 달리기 "함께 달리는 사람 27명 >" 한 줄 */
@Composable
internal fun TogetherRow(count: Int, onClick: () -> Unit) {
    val t = runTone()
    val shape = RoundedCornerShape(14.dp)
    Row(
        Modifier.fillMaxWidth().heightIn(min = 48.dp).clip(shape).background(t.panel, shape).border(1.dp, t.panelEdge, shape)
            .feedbackClickable(onClick = onClick).padding(horizontal = 16.dp).testTag("run-together-row"),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(Icons.Filled.Groups, null, tint = t.cobaltText, modifier = Modifier.size(24.dp))
        Spacer(Modifier.width(10.dp))
        Text(stringResource(R.string.run_together_row, count), style = runTextStyle(16.sp, t.text, FontWeight.SemiBold),
            modifier = Modifier.weight(1f))
        Icon(Icons.Filled.ChevronRight, null, tint = t.label, modifier = Modifier.size(22.dp))
    }
}

/** CR18 — 크루 연결이 끊긴 동안: 함께 달리는 사람 · 크루 상태를 모른다(옛 값을 보이지 않는다) */
@Composable
internal fun CrewOfflineCard() {
    val t = runTone()
    RunCard(padding = PaddingValues(horizontal = 16.dp, vertical = 10.dp), tag = "run-crew-offline-card") {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text(stringResource(R.string.run_crew_people_label), style = runTextStyle(13.sp, t.label, FontWeight.Medium))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Filled.Groups, null, tint = t.cobaltText, modifier = Modifier.size(22.dp))
                    Spacer(Modifier.width(10.dp))
                    Text("—", style = runTextStyle(18.sp, t.text, FontWeight.Bold))
                }
            }
            Box(Modifier.padding(horizontal = 12.dp).width(1.dp).height(40.dp).background(t.divider))
            Column(Modifier.weight(1f)) {
                Text(stringResource(R.string.run_crew_state_label), style = runTextStyle(13.sp, t.label, FontWeight.Medium))
                Text("—", style = runTextStyle(18.sp, t.text, FontWeight.Bold))
            }
        }
    }
}

/**
 * CR10 — 함께 달리는 사람. 거리는 공유를 켠 사람만(각 폰이 잰 화면용 값 — 보상과 무관), 나머지는 "위치 공유 꺼짐".
 * 다른 사람이 달리는지 멈췄는지는 서버가 알려 주지 않는다 — 내 줄만 달리는 중 · 일시 정지를 적는다.
 */
@Composable
internal fun ColumnScope.TogetherSheetContent(
    members: List<com.stepup.android.data.repo.PartyMember>,
    myKm: Double,
    myPaused: Boolean,
    myShare: Boolean,
    onShare: (Boolean) -> Unit,
    onClose: () -> Unit,
) {
    val t = runTone()
    Text(stringResource(R.string.run_together_sheet_title, members.size), style = runTextStyle(22.sp, t.text, FontWeight.ExtraBold),
        modifier = Modifier.fillMaxWidth().testTag("run-together-title"))
    Spacer(Modifier.height(12.dp))
    val rows = members.sortedWith(compareByDescending<com.stepup.android.data.repo.PartyMember> { it.isMe }.thenByDescending { it.isHost })
    Column(
        Modifier.fillMaxWidth().heightIn(max = 420.dp).verticalScroll(rememberScrollState()).testTag("run-together-list"),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        rows.forEach { m ->
            val km = if (m.isMe) myKm else m.km
            val shape = RoundedCornerShape(14.dp)
            Row(
                Modifier.fillMaxWidth().heightIn(min = 60.dp).clip(shape).background(t.panel, shape).border(1.dp, t.panelEdge, shape)
                    .padding(horizontal = 12.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                RunInitials(initialsOf(m.name), size = 40.dp)
                Spacer(Modifier.width(12.dp))
                Column(Modifier.weight(1f)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            if (m.isMe) stringResource(R.string.run_together_me_name, m.name) else m.name,
                            style = runTextStyle(16.sp, t.text, FontWeight.Bold), maxLines = 1, overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.weight(1f, fill = false),
                        )
                        if (m.isHost) {
                            Spacer(Modifier.width(8.dp))
                            RunChip(stringResource(R.string.run_together_host), accent = t.cobaltText)
                        }
                    }
                    val (status, dot) = when {
                        m.isMe && myPaused -> stringResource(R.string.run_together_paused) to Color(0xFFF5C33B)
                        m.isMe -> stringResource(R.string.run_together_running) to Color(0xFF3DDC84)
                        m.sharing && m.km != null -> stringResource(R.string.run_together_in) to Color(0xFF3DDC84)
                        else -> stringResource(R.string.run_together_no_share) to t.muted
                    }
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(Modifier.size(8.dp).clip(CircleShape).background(dot))
                        Spacer(Modifier.width(6.dp))
                        Text(status, style = runTextStyle(13.sp, t.label, FontWeight.Medium))
                    }
                }
                if (km != null && (m.isMe || m.sharing)) {
                    RunNumber("%.2f".format(km), unit = "km", size = 24.sp, unitSize = 14.sp)
                } else {
                    Text("—", style = runTextStyle(18.sp, t.label, FontWeight.Bold))
                }
            }
        }
    }
    Spacer(Modifier.height(10.dp))
    RunCard(padding = PaddingValues(horizontal = 14.dp, vertical = 10.dp), tag = "run-together-share") {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text(stringResource(R.string.run_my_share), style = runTextStyle(15.sp, t.text, FontWeight.Bold))
                Text(stringResource(if (myShare) R.string.run_my_share_on else R.string.run_my_share_off),
                    style = runTextStyle(12.sp, t.label))
            }
            Switch(
                checked = myShare, onCheckedChange = onShare,
                colors = SwitchDefaults.colors(
                    checkedThumbColor = Color.White, checkedTrackColor = t.cobalt,
                    uncheckedThumbColor = Color.White, uncheckedTrackColor = t.track, uncheckedBorderColor = t.panelEdge,
                ),
                modifier = Modifier.testTag("run-together-share-toggle"),
            )
        }
    }
    Text(stringResource(R.string.run_together_note), style = runTextStyle(12.sp, t.muted), textAlign = TextAlign.Center,
        modifier = Modifier.fillMaxWidth().padding(top = 8.dp))
    Spacer(Modifier.height(12.dp))
    RunButton(stringResource(R.string.run_back_to_run), onClose, modifier = Modifier.testTag("run-together-close"))
}

/** 이름 머리글 — 라틴 이름은 두 낱말의 첫 글자("DY"), 한글 · 한자는 첫 글자 */
internal fun initialsOf(name: String): String {
    val words = name.trim().split(Regex("\\s+")).filter { it.isNotEmpty() }
    if (words.isEmpty()) return "?"
    val first = words[0]
    val latin = first.first().let { it in 'A'..'Z' || it in 'a'..'z' }
    return when {
        latin && words.size >= 2 -> "${first.first()}${words[1].first()}".uppercase()
        latin -> first.take(2).uppercase()
        else -> first.take(1)
    }
}

// ── 시트 몸통 ──────────────────────────────────────────────────────

/** R04 — 러닝을 마칠까요? 저장하고 마치기(주) · 계속 달리기(보조) · 기록 없이 끝내기(빨간 글) */
@Composable
internal fun ColumnScope.FinishSheetContent(
    time: String,
    km: String,
    onSave: () -> Unit,
    onContinue: () -> Unit,
    onDiscard: (() -> Unit)?,
    title: String = stringResource(R.string.run_finish_title),
    note: String = stringResource(R.string.run_finish_note),
    saveLabel: String = stringResource(R.string.run_save_finish),
) {
    RunSheetText(title, body = stringResource(R.string.run_finish_body, time, km), note = note)
    Spacer(Modifier.height(20.dp))
    RunButton(saveLabel, onSave, modifier = Modifier.testTag("run-end-save"))
    Spacer(Modifier.height(10.dp))
    RunButton(stringResource(R.string.run_continue), onContinue, kind = RunButtonKind.Secondary,
        modifier = Modifier.testTag("run-end-continue"))
    if (onDiscard != null) {
        Spacer(Modifier.height(4.dp))
        RunTextAction(stringResource(R.string.run_discard), onDiscard, danger = true, modifier = Modifier.testTag("run-end-discard"))
    }
}

/** R07 — 기록 없이 끝낼까요? 기록 없이 끝내기(빨강) · 취소 */
@Composable
internal fun ColumnScope.DiscardSheetContent(time: String, km: String, onDiscard: () -> Unit, onCancel: () -> Unit) {
    RunSheetText(stringResource(R.string.run_discard_title), body = stringResource(R.string.run_discard_body, time, km))
    Spacer(Modifier.height(20.dp))
    RunButton(stringResource(R.string.run_discard), onDiscard, kind = RunButtonKind.Danger, modifier = Modifier.testTag("run-discard-confirm"))
    Spacer(Modifier.height(10.dp))
    RunButton(stringResource(R.string.run_cancel), onCancel, kind = RunButtonKind.Secondary, modifier = Modifier.testTag("run-discard-back"))
}

/** C01 — 목표 달성. 계속 달리면 기록이 이어진다 */
@Composable
internal fun ColumnScope.GoalReachedContent(goalShort: String, onSave: () -> Unit, onMore: () -> Unit) {
    val t = runTone()
    Box(
        Modifier.size(64.dp).clip(CircleShape).background(t.cobalt).border(3.dp, t.cobalt.copy(alpha = 0.35f), CircleShape),
        contentAlignment = Alignment.Center,
    ) { Icon(Icons.Filled.Check, null, tint = Color.White, modifier = Modifier.size(36.dp)) }
    Spacer(Modifier.height(16.dp))
    RunSheetText(stringResource(R.string.run_goal_done_title, goalShort), note = stringResource(R.string.run_goal_done_body))
    Spacer(Modifier.height(20.dp))
    RunButton(stringResource(R.string.run_save_finish), onSave, modifier = Modifier.testTag("run-goal-reached-save"))
    Spacer(Modifier.height(10.dp))
    RunButton(stringResource(R.string.run_run_more), onMore, kind = RunButtonKind.Secondary, modifier = Modifier.testTag("run-goal-reached-more"))
}

/** E02 — 러닝 목표(거리) 고치기. 0.5km 씩, 적용 · 취소 뒤에도 멈춘 채로 둔다 */
@Composable
internal fun ColumnScope.GoalEditContent(km: Double, onMinus: () -> Unit, onPlus: () -> Unit, onApply: () -> Unit, onCancel: () -> Unit) {
    val t = runTone()
    Text(stringResource(R.string.run_goal_sheet_title), style = runTextStyle(24.sp, t.text, FontWeight.ExtraBold),
        modifier = Modifier.fillMaxWidth())
    Text(stringResource(R.string.run_goal_sheet_body), style = runTextStyle(15.sp, t.label, FontWeight.Medium), textAlign = TextAlign.Center,
        modifier = Modifier.fillMaxWidth().padding(top = 6.dp))
    Spacer(Modifier.height(16.dp))
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        StepButton("−", stringResource(R.string.run_goal_minus), onMinus, Modifier.testTag("run-goal-minus"))
        RunNumber("%.1f".format(km), unit = "km", size = 56.sp, unitSize = 26.sp, align = Alignment.CenterHorizontally,
            modifier = Modifier.weight(1f), valueTag = "run-goal-value")
        StepButton("+", stringResource(R.string.run_goal_plus), onPlus, Modifier.testTag("run-goal-plus"))
    }
    Text(stringResource(R.string.run_goal_step_note), style = runTextStyle(14.sp, t.label), textAlign = TextAlign.Center,
        modifier = Modifier.fillMaxWidth().padding(top = 8.dp))
    Spacer(Modifier.height(20.dp))
    RunButton(stringResource(R.string.run_goal_apply), onApply, modifier = Modifier.testTag("run-goal-apply"))
    Spacer(Modifier.height(10.dp))
    RunButton(stringResource(R.string.run_cancel), onCancel, kind = RunButtonKind.Secondary, modifier = Modifier.testTag("run-goal-cancel"))
}

@Composable
private fun StepButton(label: String, description: String, onClick: () -> Unit, modifier: Modifier) {
    val t = runTone()
    val shape = RoundedCornerShape(14.dp)
    Box(
        modifier.size(64.dp).clip(shape).background(t.secondaryFace, shape).border(1.5.dp, t.secondaryEdge, shape)
            .feedbackClickable(onClick = onClick).semantics { contentDescription = description },
        contentAlignment = Alignment.Center,
    ) {
        Text(label, style = runTextStyle(32.sp, t.text, FontWeight.Bold, 1.0f))
    }
}

/** E03 — 러닝 상세 정보: 시간 | 거리, 목표와 달성률(수정), 예상 SUP(확인 중), 선택한 코스, 현재 속도 | 내구도 */
@Immutable
internal data class RunDetailsUi(
    val time: String,
    val km: String,
    val goalKm: Double,
    val goalFraction: Float,
    val estimate: String,
    val courseName: String?,
    val speed: String,
    val durability: String,
    val reasons: List<String> = emptyList(),
)

@Composable
internal fun ColumnScope.RunDetailsContent(
    ui: RunDetailsUi,
    onEditGoal: () -> Unit,
    onCourse: () -> Unit,
    onClose: () -> Unit,
    extra: @Composable ColumnScope.() -> Unit = {},
) {
    val t = runTone()
    Column(Modifier.fillMaxWidth().heightIn(max = 560.dp).verticalScroll(rememberScrollState()).testTag("run-details")) {
        Text(stringResource(R.string.run_details_title), style = runTextStyle(24.sp, t.text, FontWeight.ExtraBold))
        Spacer(Modifier.height(14.dp))
        RunStatRow(listOf(RunStat(stringResource(R.string.run_label_time), ui.time), RunStat(stringResource(R.string.run_label_distance), ui.km, "km")),
            valueSize = 34.sp)
        Spacer(Modifier.height(14.dp))
        RunCard(padding = PaddingValues(16.dp), tag = "run-details-goal") {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text(stringResource(R.string.run_goal_distance), style = runTextStyle(14.sp, t.label, FontWeight.Medium))
                    RunNumber("%.1f".format(ui.goalKm), unit = "km", size = 34.sp)
                }
                Column(horizontalAlignment = Alignment.End) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(stringResource(R.string.run_goal_rate), style = runTextStyle(13.sp, t.label, FontWeight.Medium))
                        Spacer(Modifier.width(8.dp))
                        Text(
                            stringResource(R.string.run_goal_edit), style = runTextStyle(13.sp, t.text, FontWeight.SemiBold),
                            modifier = Modifier.clip(RoundedCornerShape(50)).border(1.dp, t.secondaryEdge, RoundedCornerShape(50))
                                .feedbackClickable(onClick = onEditGoal).padding(horizontal = 12.dp, vertical = 4.dp)
                                .testTag("run-details-goal-edit"),
                        )
                    }
                    RunNumber("${(ui.goalFraction * 100).toInt()}", unit = "%", size = 34.sp, color = t.cyan, unitColor = t.cyan)
                }
            }
            Spacer(Modifier.height(10.dp))
            RunMeter(ui.goalFraction, height = 10.dp)
        }
        Spacer(Modifier.height(10.dp))
        RunCard(padding = PaddingValues(16.dp), tag = "run-details-reward") {
            Text(stringResource(R.string.run_estimate_title), style = runTextStyle(14.sp, t.label, FontWeight.Medium))
            Row(verticalAlignment = Alignment.CenterVertically) {
                RunNumber(ui.estimate, size = 34.sp, color = t.cyan, valueTag = "run-details-estimate")
                Spacer(Modifier.width(10.dp))
                RunChip(stringResource(R.string.run_checking), icon = Icons.Outlined.Schedule)
            }
            Text(stringResource(R.string.run_reward_note), style = runTextStyle(13.sp, t.label))
        }
        Spacer(Modifier.height(10.dp))
        RunCard(padding = PaddingValues(16.dp), tag = "run-details-course") {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text(stringResource(R.string.run_selected_course), style = runTextStyle(13.sp, t.label, FontWeight.Medium))
                    Text(ui.courseName ?: stringResource(R.string.course_none_title), style = runTextStyle(18.sp, t.text, FontWeight.Bold),
                        maxLines = 2)
                }
                Text(
                    stringResource(if (ui.courseName != null) R.string.run_view_course else R.string.run_pick_course),
                    style = runTextStyle(14.sp, t.text, FontWeight.SemiBold),
                    modifier = Modifier.clip(RoundedCornerShape(12.dp)).border(1.dp, t.secondaryEdge, RoundedCornerShape(12.dp))
                        .feedbackClickable(onClick = onCourse).padding(horizontal = 14.dp, vertical = 8.dp).testTag("run-details-course-open"),
                )
            }
        }
        Spacer(Modifier.height(10.dp))
        RunStatRow(
            listOf(RunStat(stringResource(R.string.run_speed_now), ui.speed, "km/h", tag = "run-speed"),
                RunStat(stringResource(R.string.run_durability), ui.durability, "/ 100", tag = "run-durability")),
            framed = true, valueSize = 26.sp,
        )
        ui.reasons.forEach { reason ->
            Spacer(Modifier.height(10.dp))
            RunNotice(reason, kind = RunNoticeKind.Warn, tag = "run-details-reason")
        }
        extra()
    }
    Spacer(Modifier.height(14.dp))
    RunButton(stringResource(R.string.run_close), onClose, modifier = Modifier.testTag("run-details-close"))
}

/** L04 — 위치 신호가 약해요. 시간은 계속, 끊긴 구간 거리는 더하지 않는다 */
@Composable
internal fun ColumnScope.GpsLostContent(onRetry: () -> Unit, onTimeOnly: () -> Unit) {
    val t = runTone()
    Icon(Icons.Outlined.SignalCellularConnectedNoInternet0Bar, null, tint = t.label, modifier = Modifier.size(34.dp))
    Spacer(Modifier.height(8.dp))
    RunSheetText(stringResource(R.string.run_gps_lost_title), note = stringResource(R.string.run_gps_lost_body))
    Spacer(Modifier.height(20.dp))
    RunButton(stringResource(R.string.run_gps_retry), onRetry, modifier = Modifier.testTag("run-gps-retry"))
    Spacer(Modifier.height(10.dp))
    RunButton(stringResource(R.string.run_gps_time_only), onTimeOnly, kind = RunButtonKind.Secondary, modifier = Modifier.testTag("run-gps-time-only"))
}

/** K05 — 코스에서 벗어났어요. 기록은 이어진다 */
@Composable
internal fun ColumnScope.CourseOffContent(onMap: () -> Unit, onFree: () -> Unit) {
    val t = runTone()
    Box(Modifier.size(52.dp).clip(CircleShape).border(2.dp, t.cyan, CircleShape), contentAlignment = Alignment.Center) {
        Text("!", style = runTextStyle(26.sp, t.cyan, FontWeight.ExtraBold, 1.0f))
    }
    Spacer(Modifier.height(12.dp))
    RunSheetText(stringResource(R.string.run_course_off_title), note = stringResource(R.string.run_course_off_body))
    Spacer(Modifier.height(20.dp))
    RunButton(stringResource(R.string.run_course_full_map), onMap, modifier = Modifier.testTag("run-course-off-map"))
    Spacer(Modifier.height(10.dp))
    RunButton(stringResource(R.string.run_course_to_free), onFree, kind = RunButtonKind.Secondary, modifier = Modifier.testTag("run-course-off-free"))
}

/** K08 — 자유 러닝으로 바꿀까요? 코스 안내만 끈다 */
@Composable
internal fun ColumnScope.CourseFreeContent(onSwitch: () -> Unit, onCancel: () -> Unit) {
    RunSheetText(stringResource(R.string.run_course_free_title), note = stringResource(R.string.run_course_free_body))
    Spacer(Modifier.height(20.dp))
    RunButton(stringResource(R.string.run_course_free_ok), onSwitch, modifier = Modifier.testTag("run-course-free-ok"))
    Spacer(Modifier.height(10.dp))
    RunButton(stringResource(R.string.run_cancel), onCancel, kind = RunButtonKind.Secondary, modifier = Modifier.testTag("run-course-free-cancel"))
}

/** R06 · H14 — 이 기록을 삭제할까요? 기록 삭제(빨강) · 취소 */
@Composable
internal fun ColumnScope.DeleteSheetContent(subject: String, onDelete: () -> Unit, onCancel: () -> Unit, busy: Boolean = false) {
    RunSheetText(stringResource(R.string.run_delete_title), body = subject, note = stringResource(R.string.run_delete_body))
    Spacer(Modifier.height(20.dp))
    RunButton(stringResource(R.string.run_delete_confirm), onDelete, kind = RunButtonKind.Danger, busy = busy,
        modifier = Modifier.testTag("run-delete-confirm"))
    Spacer(Modifier.height(10.dp))
    RunButton(stringResource(R.string.run_cancel), onCancel, kind = RunButtonKind.Secondary, enabled = !busy,
        modifier = Modifier.testTag("run-delete-cancel"))
}

/** S01 — 기록을 저장하지 못했어요. 같은 러닝으로 다시 저장 · 기록 확인하기 */
@Composable
internal fun ColumnScope.SaveFailedContent(onRetry: () -> Unit, onReview: () -> Unit) {
    val t = runTone()
    Box(Modifier.size(52.dp).clip(CircleShape).background(t.dangerFace.copy(alpha = 0.18f)).border(2.dp, t.dangerText, CircleShape),
        contentAlignment = Alignment.Center) {
        Text("!", style = runTextStyle(26.sp, t.dangerText, FontWeight.ExtraBold, 1.0f))
    }
    Spacer(Modifier.height(12.dp))
    RunSheetText(stringResource(R.string.run_save_failed_title), note = stringResource(R.string.run_save_failed_body))
    Spacer(Modifier.height(20.dp))
    RunButton(stringResource(R.string.run_save_again), onRetry, modifier = Modifier.testTag("run-save-again"))
    Spacer(Modifier.height(10.dp))
    RunButton(stringResource(R.string.run_save_review), onReview, kind = RunButtonKind.Secondary, modifier = Modifier.testTag("run-save-stay"))
}

// ── 저장 중(E04) ───────────────────────────────────────────────────

@Composable
internal fun RunSavingContent(
    stats: List<RunStat>,
    modifier: Modifier = Modifier,
    map: @Composable BoxScope.() -> Unit,
) {
    val t = runTone()
    Box(modifier.fillMaxSize().testTag("run-saving")) {
        RunBackdrop(Modifier.fillMaxSize())
        Column(Modifier.fillMaxSize().safeDrawingPadding()) {
            RunTopBar(onBack = null)
            Column(Modifier.weight(1f).fillMaxWidth().padding(horizontal = RunSpec.Gutter)) {
                RunHeadline(stringResource(R.string.run_saving_title))
                Spacer(Modifier.height(18.dp))
                RunSpinner(Modifier.size(64.dp).align(Alignment.CenterHorizontally))
                Spacer(Modifier.height(14.dp))
                Text(stringResource(R.string.run_saving_body), style = runTextStyle(17.sp, t.text, FontWeight.SemiBold),
                    textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth())
                Text(stringResource(R.string.run_saving_wait), style = runTextStyle(14.sp, t.label), textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth())
                Spacer(Modifier.height(16.dp))
                RunStatRow(stats, framed = true, valueSize = 26.sp)
                Spacer(Modifier.height(12.dp))
                RunMapFrame(Modifier.fillMaxWidth().weight(1f).heightIn(min = 120.dp), content = map)
                Spacer(Modifier.height(12.dp))
                RunPair {
                    RunTileButton(stringResource(R.string.run_share), Icons.Outlined.IosShare, {}, Modifier.weight(1f), enabled = false)
                    RunTileButton(stringResource(R.string.run_open_records), Icons.Outlined.BarChart, {}, Modifier.weight(1f), enabled = false)
                }
            }
            Box(Modifier.fillMaxWidth().padding(horizontal = RunSpec.Gutter).padding(top = 12.dp, bottom = 12.dp)) {
                RunButton(stringResource(R.string.run_saving_button), {}, busy = true, modifier = Modifier.testTag("run-primary-action"))
            }
        }
    }
}

// ── 결과 ───────────────────────────────────────────────────────────

/** 결과 보상 칸 — 확인 중(E10) · 반영 완료(E11) · 지급 제외(E08) · 미지급(E09) */
@Composable
internal fun ResultRewardCard(reward: ResultReward, onReason: () -> Unit, onHistory: () -> Unit) {
    val t = runTone()
    RunCard(padding = PaddingValues(horizontal = 16.dp, vertical = 14.dp), tag = "run-result-reward-card") {
        when (reward.settle) {
            Settle.PENDING -> Row(verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1.1f)) {
                    Text(stringResource(R.string.run_reward_this), style = runTextStyle(15.sp, t.text, FontWeight.Bold),
                        modifier = Modifier.testTag("run-result-reward-label"))
                    Spacer(Modifier.height(4.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        RunChip(stringResource(R.string.run_reward_estimate), outlined = true, accent = t.cyan)
                        Spacer(Modifier.width(8.dp))
                        RunNumber(reward.amount, unit = "SUP", size = 30.sp, unitSize = 18.sp, color = t.cyan, unitColor = t.cyan,
                            modifier = Modifier.testTag("run-result-reward"))
                    }
                }
                Box(Modifier.padding(horizontal = 10.dp).width(1.dp).height(48.dp).background(t.divider))
                Column(Modifier.weight(1f), horizontalAlignment = Alignment.CenterHorizontally) {
                    RunChip(stringResource(R.string.run_checking), icon = Icons.Outlined.Schedule,
                        modifier = Modifier.testTag("run-result-settle-pending"))
                    Spacer(Modifier.height(6.dp))
                    Text(stringResource(R.string.run_reward_note), style = runTextStyle(12.sp, t.label), textAlign = TextAlign.Center)
                }
            }
            Settle.DONE -> Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Filled.CheckCircle, null, tint = t.cobalt, modifier = Modifier.size(30.dp))
                Spacer(Modifier.width(12.dp))
                Column(Modifier.weight(1f)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(stringResource(R.string.run_reward_this), style = runTextStyle(15.sp, t.text, FontWeight.Bold),
                            modifier = Modifier.testTag("run-result-reward-label"))
                        Spacer(Modifier.width(8.dp))
                        RunNumber(reward.amount, unit = "SUP", size = 26.sp, unitSize = 16.sp, color = t.cyan, unitColor = t.cyan,
                            modifier = Modifier.testTag("run-result-reward"))
                    }
                    Text(stringResource(R.string.run_reward_done_note), style = runTextStyle(12.sp, t.label))
                }
                RunChip(stringResource(R.string.run_reward_done), outlined = true, accent = t.cyan,
                    modifier = Modifier.testTag("run-result-settle-done"))
            }
            Settle.VOID -> Column {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(stringResource(R.string.run_reward_sup), style = runTextStyle(15.sp, t.text, FontWeight.Bold),
                        modifier = Modifier.testTag("run-result-reward-label"))
                    Spacer(Modifier.width(8.dp))
                    RunChip(stringResource(R.string.run_reward_void), icon = Icons.Filled.Block,
                        modifier = Modifier.testTag("run-result-settle-void"))
                }
                Spacer(Modifier.height(6.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(stringResource(R.string.run_reward_void_note), style = runTextStyle(13.sp, t.label), modifier = Modifier.weight(1f))
                    Row(
                        Modifier.clip(RoundedCornerShape(8.dp)).feedbackClickable(onClick = onReason).padding(4.dp)
                            .testTag("run-result-void-reason"),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(stringResource(R.string.run_details_more), style = runTextStyle(13.sp, t.cobaltText, FontWeight.SemiBold))
                        Icon(Icons.Filled.ChevronRight, null, tint = t.cobaltText, modifier = Modifier.size(18.dp))
                    }
                }
            }
            Settle.NOT_PAID -> Row(verticalAlignment = Alignment.CenterVertically) {
                Box(Modifier.size(48.dp).clip(CircleShape).background(t.inset).border(1.dp, t.panelEdge, CircleShape),
                    contentAlignment = Alignment.Center) {
                    Icon(Icons.Outlined.CardGiftcard, null, tint = t.label, modifier = Modifier.size(26.dp))
                }
                Spacer(Modifier.width(12.dp))
                Column(Modifier.weight(1f)) {
                    Text(stringResource(R.string.run_reward_sup), style = runTextStyle(15.sp, t.text, FontWeight.Bold),
                        modifier = Modifier.testTag("run-result-reward-label"))
                    RunChip(stringResource(R.string.run_reward_not_paid), modifier = Modifier.testTag("run-result-settle-rejected"))
                    Text(reward.notes.firstOrNull() ?: stringResource(R.string.run_reward_not_paid_note), style = runTextStyle(12.sp, t.label),
                        modifier = Modifier.testTag("run-result-reward-note"))
                }
                Text(
                    stringResource(R.string.run_reward_history), style = runTextStyle(13.sp, t.text, FontWeight.SemiBold),
                    modifier = Modifier.clip(RoundedCornerShape(10.dp)).border(1.dp, t.secondaryEdge, RoundedCornerShape(10.dp))
                        .feedbackClickable(onClick = onHistory).padding(horizontal = 12.dp, vertical = 8.dp)
                        .testTag("run-result-history"),
                )
            }
        }
        if (reward.settle == Settle.PENDING) {
            reward.notes.forEach { line ->
                Spacer(Modifier.height(6.dp))
                Text(line, style = runTextStyle(13.sp, t.label), modifier = Modifier.testTag("run-result-reward-note"))
            }
            if (reward.signIn) {
                Spacer(Modifier.height(8.dp))
                com.stepup.android.ui.components.SignInAgainButton()
            }
        }
    }
}

/** 결과의 신발 칸 — 그림 · 이름 · 등급 배지 · Lv · 내구도 */
@Composable
internal fun ResultShoeCard(shoe: Sneaker) {
    val t = runTone()
    val points = durabilityPoints(shoe)
    RunCard(padding = PaddingValues(horizontal = 12.dp, vertical = 10.dp), tag = "run-result-shoe") {
        Row(verticalAlignment = Alignment.CenterVertically) {
            ShoeArtThumbnail(shoe, Modifier.width(96.dp).height(58.dp).testTag("run-result-shoe-image"))
            Spacer(Modifier.width(10.dp))
            Column(Modifier.weight(1.1f)) {
                Text(shoe.shoeName(), style = runTextStyle(16.sp, t.text, FontWeight.ExtraBold), maxLines = 1,
                    overflow = TextOverflow.Ellipsis, modifier = Modifier.testTag("run-result-shoe-name"))
                Spacer(Modifier.height(4.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    ShoeGradeBadge(shoe.tier)
                    Spacer(Modifier.width(8.dp))
                    Text(stringResource(R.string.level_chip, shoe.level), style = runTextStyle(13.sp, t.label, FontWeight.SemiBold))
                }
            }
            Column(Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.Bottom) {
                    Text(stringResource(R.string.run_durability), style = runTextStyle(12.sp, t.label))
                    Spacer(Modifier.width(6.dp))
                    Text("$points", style = runTextStyle(14.sp, t.cyan, FontWeight.ExtraBold), modifier = Modifier.testTag("run-result-durability"))
                    Text(" / 100", style = runTextStyle(12.sp, t.label))
                }
                Spacer(Modifier.height(6.dp))
                RunMeter(points / 100f, height = 8.dp, fill = listOf(Color(0xFF2FB5F0), t.cyan))
            }
        }
    }
}

@Immutable
internal data class RunResultUi(
    val title: String,
    val subtitle: String? = null,
    val pill: String?,
    val date: String?,
    /** 큰 수 — 거리(km) 또는 다이어트 중간 종료의 시간 */
    val heroValue: String,
    val heroUnit: String?,
    val heroLine: String?,
    /** 큰 수 위 한 줄 — 챌린지 결과 · 다이어트 "15분 중 6분 20초" */
    val heroAbove: String? = null,
    val stats: List<RunStat>,
    val message: String?,
    /** 지도 아래 덧붙임(걸음으로 셈한 거리 등) */
    val note: String? = null,
)

/**
 * 결과 화면 — 상태 없이 그린다. 지도 틀 안([map])과 보상 · 신발 · 크루 칸([cards])은 부르는 쪽이 채운다.
 * 정보가 많아 화면을 넘기지만 아래 공유 | 기록 · 처음 화면으로는 늘 아래에 둔다.
 */
@Composable
internal fun RunResultContent(
    ui: RunResultUi,
    onBack: () -> Unit,
    onDelete: (() -> Unit)?,
    onShare: (() -> Unit)?,
    onRecords: () -> Unit,
    onHome: () -> Unit,
    modifier: Modifier = Modifier,
    map: (@Composable BoxScope.() -> Unit)?,
    cards: @Composable ColumnScope.() -> Unit = {},
    shareLabel: String = stringResource(R.string.run_share),
    extra: @Composable ColumnScope.() -> Unit = {},
) {
    val t = runTone()
    Box(modifier.fillMaxSize().testTag("run-result")) {
        RunBackdrop(Modifier.fillMaxSize())
        Column(Modifier.fillMaxSize().safeDrawingPadding()) {
            RunTopBar(
                onBack = onBack,
                trailing = if (onDelete != null) {
                    { RunHeaderAction(stringResource(R.string.run_delete), onDelete, icon = Icons.Outlined.Delete, danger = true,
                        modifier = Modifier.testTag("run-result-delete")) }
                } else null,
            )
            Column(
                Modifier.weight(1f).fillMaxWidth().verticalScroll(rememberScrollState()).padding(horizontal = RunSpec.Gutter)
                    .testTag("run-result-scroll"),
            ) {
                RunHeadline(ui.title, subtitle = ui.subtitle, titleTag = "run-result-title")
                if (ui.pill != null || ui.date != null) {
                    Spacer(Modifier.height(8.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        if (ui.pill != null) RunStatusPill(ui.pill, RunPillKind.Saved, modifier = Modifier.testTag("run-result-saved"))
                        if (ui.date != null) {
                            Spacer(Modifier.width(12.dp))
                            Text(ui.date, style = runTextStyle(14.sp, t.label, FontWeight.Medium), modifier = Modifier.testTag("run-result-date"))
                        }
                    }
                }
                Spacer(Modifier.height(10.dp))
                if (ui.heroAbove != null) {
                    Text(ui.heroAbove, style = TextStyle(fontFamily = StepUpSans, fontWeight = FontWeight.Bold, fontStyle = FontStyle.Italic,
                        fontSize = 17.sp, color = t.text), textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth().testTag("run-result-note"))
                }
                RunNumber(
                    ui.heroValue, unit = ui.heroUnit, size = 92.sp, unitSize = 40.sp, unitColor = t.text,
                    align = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth(), valueTag = "run-result-distance",
                )
                if (ui.heroLine != null) {
                    Text(ui.heroLine, style = TextStyle(fontFamily = StepUpSans, fontWeight = FontWeight.Bold, fontStyle = FontStyle.Italic,
                        fontSize = 18.sp, color = t.text), textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth().testTag("run-result-mode"))
                }
                Spacer(Modifier.height(12.dp))
                RunStatRow(ui.stats, framed = true, valueSize = 26.sp, modifier = Modifier.testTag("run-result-stats"))
                Spacer(Modifier.height(12.dp))
                if (map != null) {
                    RunMapFrame(Modifier.fillMaxWidth().height(184.dp).testTag("run-result-map-frame"), content = map)
                    Spacer(Modifier.height(10.dp))
                }
                if (ui.note != null) {
                    Text(ui.note, style = runTextStyle(13.sp, t.label), modifier = Modifier.padding(bottom = 10.dp).testTag("run-result-basis"))
                }
                Column(verticalArrangement = Arrangement.spacedBy(10.dp), content = cards)
                if (ui.message != null) {
                    Spacer(Modifier.height(12.dp))
                    Text(ui.message, style = runTextStyle(17.sp, t.text, FontWeight.Bold), textAlign = TextAlign.Center,
                        modifier = Modifier.fillMaxWidth().testTag("run-result-message"))
                }
                extra()
                Spacer(Modifier.height(8.dp))
            }
            Column(
                Modifier.fillMaxWidth().padding(horizontal = RunSpec.Gutter).padding(top = 10.dp, bottom = 12.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                RunPair {
                    if (onShare != null) {
                        RunTileButton(shareLabel, Icons.Outlined.IosShare, onShare, Modifier.weight(1f).testTag("run-result-share"))
                    }
                    RunTileButton(stringResource(R.string.run_open_records), Icons.Outlined.BarChart, onRecords,
                        Modifier.weight(1f).testTag("run-result-records"))
                }
                RunButton(stringResource(R.string.run_go_home), onHome, modifier = Modifier.testTag("run-result-done"))
            }
        }
    }
}

/** 결과 아래 크루 칸(CR13) — 함께 출발한 인원과 내 기록 저장 */
@Composable
internal fun CrewResultCard(started: Int?) {
    val t = runTone()
    RunCard(padding = PaddingValues(horizontal = 16.dp, vertical = 12.dp), tag = "run-result-crew") {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Filled.Groups, null, tint = t.cobaltText, modifier = Modifier.size(30.dp))
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(stringResource(R.string.run_crew_together), style = runTextStyle(13.sp, t.label))
                RunNumber(started?.let { stringResource(R.string.run_crew_people, it) } ?: "—", size = 22.sp)
            }
            Box(Modifier.padding(horizontal = 12.dp).width(1.dp).height(40.dp).background(t.divider))
            Text(stringResource(R.string.run_crew_my_saved), style = runTextStyle(15.sp, t.text, FontWeight.SemiBold), modifier = Modifier.weight(1.2f))
        }
    }
}

// ── 다이어트 완료(D10) ─────────────────────────────────────────────

@Composable
internal fun DietCompleteContent(totalMinutes: Int, runMinutes: Int, walkMinutes: Int, onSave: () -> Unit, saving: Boolean) {
    val t = runTone()
    Box(Modifier.fillMaxSize().testTag("run-diet-done")) {
        RunBackdrop(Modifier.fillMaxSize())
        Column(Modifier.fillMaxSize().safeDrawingPadding()) {
            RunTopBar(onBack = null)
            Column(
                Modifier.weight(1f).fillMaxWidth().verticalScroll(rememberScrollState()).padding(horizontal = RunSpec.Gutter),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                RunHeadline(stringResource(R.string.run_diet_done_title))
                Spacer(Modifier.height(18.dp))
                Box(
                    Modifier.size(120.dp).clip(CircleShape).border(6.dp, t.cobalt, CircleShape),
                    contentAlignment = Alignment.Center,
                ) { Icon(Icons.Filled.Check, null, tint = t.text, modifier = Modifier.size(64.dp)) }
                Spacer(Modifier.height(16.dp))
                Text(stringResource(R.string.run_diet_total_label), style = runTextStyle(16.sp, t.label, FontWeight.SemiBold))
                RunNumber(stringResource(R.string.run_diet_min_big, totalMinutes), size = 84.sp, align = Alignment.CenterHorizontally,
                    modifier = Modifier.fillMaxWidth(), valueTag = "run-hero-value")
                Spacer(Modifier.height(8.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    RunMeter(1f, height = 12.dp, modifier = Modifier.weight(1f))
                    Spacer(Modifier.width(8.dp))
                    Text("100%", style = TextStyle(fontFamily = StepUpSans, fontWeight = FontWeight.Bold, fontStyle = FontStyle.Italic,
                        fontSize = 15.sp, color = t.cyan))
                }
                Spacer(Modifier.height(16.dp))
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    DietTotal(Icons.AutoMirrored.Filled.DirectionsRun, stringResource(R.string.run_diet_run_total), runMinutes, Modifier.weight(1f))
                    Box(Modifier.width(1.dp).height(56.dp).background(t.divider))
                    DietTotal(Icons.AutoMirrored.Filled.DirectionsWalk, stringResource(R.string.run_diet_walk_total), walkMinutes, Modifier.weight(1f))
                }
                Spacer(Modifier.height(18.dp))
                Text(stringResource(R.string.run_diet_done_body), style = runTextStyle(18.sp, t.text, FontWeight.Bold), textAlign = TextAlign.Center)
                Text(stringResource(R.string.run_diet_done_hint), style = runTextStyle(14.sp, t.cobaltText, FontWeight.Medium), textAlign = TextAlign.Center)
            }
            Box(Modifier.fillMaxWidth().padding(horizontal = RunSpec.Gutter).padding(top = 12.dp, bottom = 12.dp)) {
                RunButton(stringResource(R.string.run_save_finish), onSave, busy = saving, modifier = Modifier.testTag("run-primary-action"))
            }
        }
    }
}

@Composable
private fun DietTotal(icon: ImageVector, label: String, minutes: Int, modifier: Modifier) {
    val t = runTone()
    Row(modifier, verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.Center) {
        Icon(icon, null, tint = t.cyan, modifier = Modifier.size(34.dp))
        Spacer(Modifier.width(8.dp))
        Column {
            Text(label, style = runTextStyle(13.sp, t.label))
            RunNumber(stringResource(R.string.run_diet_min_big, minutes), size = 30.sp)
        }
    }
}

// ── 전체 지도(K07) ─────────────────────────────────────────────────

@Composable
internal fun RunFullMapContent(
    title: String,
    courseName: String?,
    stats: List<RunStat>,
    status: String,
    onClose: () -> Unit,
    modifier: Modifier = Modifier,
    map: @Composable BoxScope.() -> Unit,
) {
    val t = runTone()
    Box(modifier.fillMaxSize().testTag("run-full-map")) {
        RunBackdrop(Modifier.fillMaxSize())
        Column(Modifier.fillMaxSize().safeDrawingPadding()) {
            RunTopBar(onBack = onClose)
            Column(Modifier.weight(1f).padding(horizontal = RunSpec.Gutter)) {
                RunHeadline(title)
                Spacer(Modifier.height(12.dp))
                RunMapFrame(Modifier.fillMaxWidth().weight(1f), content = map)
                Spacer(Modifier.height(12.dp))
                RunCard(padding = PaddingValues(16.dp)) {
                    if (courseName != null) {
                        Text(courseName, style = runTextStyle(20.sp, t.text, FontWeight.ExtraBold), maxLines = 1, overflow = TextOverflow.Ellipsis)
                        Spacer(Modifier.height(8.dp))
                    }
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        RunStatRow(stats, valueSize = 26.sp, modifier = Modifier.weight(1f))
                        Spacer(Modifier.width(12.dp))
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(stringResource(R.string.run_state_label), style = runTextStyle(13.sp, t.label))
                            Spacer(Modifier.height(4.dp))
                            RunStatusPill(status, RunPillKind.Live)
                        }
                    }
                    Spacer(Modifier.height(12.dp))
                    RunButton(stringResource(R.string.run_back_to_run), onClose, modifier = Modifier.testTag("run-full-map-close"))
                }
            }
            Spacer(Modifier.height(12.dp))
        }
    }
}

// ── 공유 미리보기(E05 · CR21) ──────────────────────────────────────

/**
 * 공유 미리보기 — 실제로 내보낼 그림을 그대로 보인다. 경로 포함을 끄면 그림에서 지도 · 출발 · 도착이 빠진다
 * (토글만 끄고 내보내는 그림에 경로가 남지 않게 — 미리보기와 내보내기가 같은 그림).
 */
@Composable
internal fun RunSharePreviewContent(
    title: String,
    subtitle: String,
    preview: ImageBitmap?,
    includeRoute: Boolean,
    onRouteChange: (Boolean) -> Unit,
    onShare: () -> Unit,
    onCancel: () -> Unit,
    shareLabel: String,
    modifier: Modifier = Modifier,
    target: (@Composable ColumnScope.() -> Unit)? = null,
    onSaveImage: (() -> Unit)? = null,
    sending: Boolean = false,
) {
    val t = runTone()
    Box(modifier.fillMaxSize().testTag("run-share")) {
        RunBackdrop(Modifier.fillMaxSize())
        Column(Modifier.fillMaxSize().safeDrawingPadding()) {
            RunTopBar(onBack = onCancel)
            Column(Modifier.weight(1f).fillMaxWidth().verticalScroll(rememberScrollState()).padding(horizontal = RunSpec.Gutter)) {
                RunHeadline(title, subtitle = subtitle)
                Spacer(Modifier.height(16.dp))
                if (target != null) {
                    target()
                    Spacer(Modifier.height(12.dp))
                }
                val shape = RoundedCornerShape(RunSpec.CardRadius)
                Box(
                    Modifier.fillMaxWidth().aspectRatio(preview?.let { it.width.toFloat() / it.height } ?: 1f)
                        .clip(shape).background(t.inset, shape)
                        .border(1.dp, t.panelEdge, shape).testTag("run-share-preview"),
                    contentAlignment = Alignment.Center,
                ) {
                    if (preview != null) {
                        androidx.compose.foundation.Image(preview, contentDescription = null, contentScale = ContentScale.Fit,
                            modifier = Modifier.fillMaxSize())
                    } else {
                        RunSpinner(Modifier.size(40.dp))
                    }
                }
                Spacer(Modifier.height(12.dp))
                RunCard(padding = PaddingValues(horizontal = 16.dp, vertical = 12.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Column(Modifier.weight(1f)) {
                            Text(stringResource(R.string.run_share_route), style = runTextStyle(17.sp, t.text, FontWeight.Bold))
                            Text(stringResource(R.string.run_share_route_body), style = runTextStyle(13.sp, t.label))
                        }
                        Switch(
                            checked = includeRoute, onCheckedChange = onRouteChange,
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = Color.White, checkedTrackColor = t.cobalt,
                                uncheckedThumbColor = Color.White, uncheckedTrackColor = t.track, uncheckedBorderColor = t.panelEdge,
                            ),
                            modifier = Modifier.testTag("run-share-route"),
                        )
                    }
                }
                if (onSaveImage != null) {
                    Spacer(Modifier.height(4.dp))
                    RunTextAction(stringResource(R.string.run_share_save_image), onSaveImage, modifier = Modifier.align(Alignment.CenterHorizontally)
                        .testTag("run-share-save-image"))
                }
            }
            Column(
                Modifier.fillMaxWidth().padding(horizontal = RunSpec.Gutter).padding(top = 10.dp, bottom = 12.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                RunButton(shareLabel, onShare, busy = sending, enabled = preview != null, modifier = Modifier.testTag("run-share-send"))
                RunButton(stringResource(R.string.run_cancel), onCancel, kind = RunButtonKind.Secondary, modifier = Modifier.testTag("run-share-cancel"))
            }
        }
    }
}

/** 저장 실패 · 삭제 실패 띠(H16) */
@Composable
internal fun FailureBanner(title: String, body: String, tag: String) {
    RunNotice(title, body = body, kind = RunNoticeKind.Error, tag = tag)
}
