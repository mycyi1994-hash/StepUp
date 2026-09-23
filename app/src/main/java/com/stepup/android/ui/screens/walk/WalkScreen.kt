package com.stepup.android.ui.screens.walk

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import com.stepup.android.ui.experience.feedbackClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.DirectionsRun
import androidx.compose.material.icons.automirrored.filled.DirectionsWalk
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Flag
import androidx.compose.material.icons.filled.GpsFixed
import androidx.compose.material.icons.filled.LocalFireDepartment
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.Terrain
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material.icons.filled.Map
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material.icons.filled.Route
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.stepup.android.BuildConfig
import com.stepup.android.R
import com.stepup.android.service.WalkSessionState
import androidx.compose.foundation.layout.heightIn
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Pause
import com.stepup.android.data.local.UploadState
import com.stepup.android.ui.components.VerticalHairline
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Stop
import com.stepup.android.domain.AvatarPose
import com.stepup.android.ui.components.CharacterStage
import com.stepup.android.ui.components.StatCell
import com.stepup.android.ui.components.SupPill
import com.stepup.android.ui.components.PrimaryCta
import com.stepup.android.domain.CourseRewards
import com.stepup.android.domain.GeoPoint
import com.stepup.android.domain.RunVerdict
import com.stepup.android.core.ExternalIntents
import com.stepup.android.domain.RewardEconomy
import com.stepup.android.domain.RunCourse
import com.stepup.android.service.RunLap
import com.stepup.android.service.WalkSessionService
import com.stepup.android.domain.trackDistanceKm
import com.stepup.android.ui.StepPermissions
import com.stepup.android.ui.components.BarMeter
import com.stepup.android.ui.components.LiveRouteMap
import com.stepup.android.ui.components.rememberCurrentLocation
import com.stepup.android.ui.components.DarkIconButton
import com.stepup.android.ui.components.EnergyMeter
import com.stepup.android.ui.components.GhostButton
import com.stepup.android.ui.components.GlowCard
import com.stepup.android.ui.components.HairlineDivider
import com.stepup.android.ui.components.HexEmblem
import com.stepup.android.ui.components.NeonRing
import com.stepup.android.ui.components.StartRunButton
import com.stepup.android.ui.components.VoltButton
import com.stepup.android.ui.components.Wordmark
import com.stepup.android.ui.components.breathing
import com.stepup.android.ui.components.quietClickable
import com.stepup.android.ui.screens.community.LabeledField
import com.stepup.android.ui.theme.Alert
import com.stepup.android.ui.theme.Carbon
import com.stepup.android.ui.theme.CarbonHigh
import com.stepup.android.ui.theme.Edge
import com.stepup.android.ui.theme.Night
import com.stepup.android.ui.theme.OnVolt
import com.stepup.android.ui.theme.Silver
import com.stepup.android.ui.theme.Slate
import com.stepup.android.ui.theme.Snow
import com.stepup.android.ui.theme.Volt

import com.stepup.android.ui.components.reveal
import com.stepup.android.ui.components.celebrate
import kotlin.math.max
import kotlin.math.sin

/** 랩 스냅샷(누적)을 구간값으로 변환한 것 */
private data class LapSegment(
    val index: Int,
    val km: Double,
    val sec: Long,
    val paceSec: Long,
)

@Composable
@OptIn(ExperimentalLayoutApi::class)
fun RunScreen(
    onBack: () -> Unit = {},
    onOpenCourses: () -> Unit = {},
    viewModel: WalkViewModel = viewModel(factory = WalkViewModel.Factory),
) {
    val session by viewModel.session.collectAsStateWithLifecycle()
    val energy by viewModel.energy.collectAsStateWithLifecycle()
    val sneakerLevel by viewModel.sneakerLevel.collectAsStateWithLifecycle()
    val equipped by viewModel.equipped.collectAsStateWithLifecycle()
    val xpBoosted by viewModel.xpBoosted.collectAsStateWithLifecycle()
    val balance by viewModel.balance.collectAsStateWithLifecycle()
    val laps by viewModel.laps.collectAsStateWithLifecycle()
    val course by viewModel.selectedCourse.collectAsStateWithLifecycle()
    val lastUpload by viewModel.lastUpload.collectAsStateWithLifecycle()
    val look by viewModel.look.collectAsStateWithLifecycle()
    val todaySteps by viewModel.todaySteps.collectAsStateWithLifecycle()
    val dailyGoal by viewModel.dailyGoal.collectAsStateWithLifecycle()
    val context = LocalContext.current
    // 종료는 한 번 더 묻는다 — 뛰다가 손이 스쳐 러닝이 끝나면 되돌릴 수 없다
    var confirmStop by rememberSaveable { mutableStateOf(false) }
    val largeText = androidx.compose.ui.platform.LocalDensity.current.fontScale > 1.15f

    // 목표 거리(km) — 프로세스에 살아서 화면을 나갔다 와도, 회전해도 유지된다
    val goalKm by viewModel.goalKm.collectAsStateWithLifecycle()
    var showGoalDialog by remember { mutableStateOf(false) }

    // 코스 녹화 — "코스 만들기"에서 넘어온 러닝인지, 그리고 방금 끝난 트랙.
    // 러닝이 끝나고(isActive=false) 트랙이 남아 있으면 저장 창을 띄운다.
    val recordingCourse by viewModel.courseRecording.collectAsStateWithLifecycle()
    val recordedTrack by viewModel.lastTrack.collectAsStateWithLifecycle()
    val readyToSaveCourse = recordingCourse && !session.isActive && recordedTrack.size >= 2

    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) {
        if (StepPermissions.hasActivityRecognition(context)) {
            WalkSessionService.start(context)
        }
    }

    // 착용 스니커즈 · 파티 인원 · XP 부스터를 모두 반영한 예상 적립
    val earningMultiplier = equipped?.earningMultiplier
        ?: RewardEconomy.sneakerMultiplier(sneakerLevel)
    val energyEfficiency = equipped?.energyEfficiency ?: 1.0
    val estimate = RewardEconomy.sessionReward(
        walkedSteps = session.steps,
        energyRemaining = energy,
        earningMultiplier = earningMultiplier,
        energyEfficiency = energyEfficiency,
        partyMultiplier = RewardEconomy.partyMultiplier(session.partySize),
        boostMultiplier = if (xpBoosted) RewardEconomy.XP_BOOST_MULTIPLIER else 1.0,
    )
    val earnableSteps = RewardEconomy.earnableSteps(energy, energyEfficiency)
    val maxEnergy = RewardEconomy.maxEnergy(equipped?.level ?: sneakerLevel)
    val distanceKm = RewardEconomy.distanceMeters(session.steps) / 1000
    val calories = RewardEconomy.calories(session.steps)
    val running = session.isActive && !session.isPaused


    val avgPaceSec: Long? = if (distanceKm >= 0.01 && session.elapsedSec > 0) {
        (session.elapsedSec / distanceKm).toLong()
    } else {
        null
    }
    // 현재 구간(마지막 랩 이후) 페이스 — 구간 데이터가 모자라면 평균으로 대체
    val lastLap = laps.lastOrNull()
    val segKm = distanceKm - (lastLap?.km ?: 0.0)
    val segSec = session.elapsedSec - (lastLap?.splitSec ?: 0L)
    val segPaceSec: Long = if (segKm >= 0.01 && segSec > 0) (segSec / segKm).toLong() else 0L
    val curPaceSec: Long? = if (segPaceSec > 0) segPaceSec else avgPaceSec

    val cadenceVal = if (session.elapsedSec > 0) (session.steps * 60L / session.elapsedSec).toInt() else 0
    val speedVal = if (session.elapsedSec > 0) distanceKm / (session.elapsedSec / 3600.0) else 0.0
    // No heart-rate source is connected; never infer a health reading from steps.
    val hr: Int? = null

    val segments = lapSegments(laps)


    // 러닝 중과 러닝 전에 놓이는 자리만 다른 카드 둘 — 목표 링과 예상 적립
    val goalRingCard: @Composable () -> Unit = {
        GlowCard(contentPadding = PaddingValues(vertical = 20.dp, horizontal = 12.dp)) {
            if (largeText) {
                Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                    RunTimeRing(distanceKm, goalKm, session.elapsedSec, running, { showGoalDialog = true }, Modifier.size(210.dp))
                }
            }
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(2.dp),
                ) {
                    Text(
                        text = stringResource(R.string.stat_distance),
                        fontSize = 10.sp,
                        color = Slate,
                    )
                    Text(
                        text = "%.2f".format(distanceKm),
                        fontFamily = com.stepup.android.ui.theme.StepUpNumbers,
                        fontSize = 21.sp,
                        fontWeight = FontWeight.ExtraBold,
                        letterSpacing = (-0.5).sp,
                        color = Snow,
                        maxLines = 1,
                    )
                    Text(
                        text = "km",
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        color = Volt,
                    )
                }
                if (!largeText) RunTimeRing(distanceKm, goalKm, session.elapsedSec, running, { showGoalDialog = true })
                Column(
                    modifier = Modifier.weight(1f),
                    horizontalAlignment = Alignment.End,
                    verticalArrangement = Arrangement.spacedBy(2.dp),
                ) {
                    Text(
                        text = stringResource(R.string.run_remaining),
                        fontSize = 10.sp,
                        color = Slate,
                    )
                    Text(
                        text = "%.2f".format(max(goalKm - distanceKm, 0.0)),
                        fontFamily = com.stepup.android.ui.theme.StepUpNumbers,
                        fontSize = 21.sp,
                        fontWeight = FontWeight.ExtraBold,
                        letterSpacing = (-0.5).sp,
                        color = Snow,
                        maxLines = 1,
                    )
                    Text(
                        text = "km",
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        color = Volt,
                    )
                    Spacer(Modifier.height(6.dp))
                    Text(
                        text = stringResource(R.string.run_eta),
                        fontSize = 10.sp,
                        color = Slate,
                    )
                    Text(
                        text = if (distanceKm >= 0.05 && session.elapsedSec > 0) {
                            formatDuration((session.elapsedSec / distanceKm * goalKm).toLong())
                        } else {
                            "—"
                        },
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        color = Volt,
                    )
                }
            }
        }
    }
    val estimateCard: @Composable () -> Unit = {
        GlowCard(spacing = 12.dp) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        Icons.Filled.Bolt,
                        contentDescription = null,
                        tint = Volt,
                        modifier = Modifier.size(15.dp),
                    )
                    Spacer(Modifier.width(6.dp))
                    Text(
                        text = stringResource(R.string.run_estimated_points),
                        style = MaterialTheme.typography.titleSmall,
                        color = Snow,
                    )
                }
                Text(
                    text = "+%.2f SUP".format(estimate.points),
                   fontFamily = com.stepup.android.ui.theme.StepUpNumbers,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = Volt,
                )
            }
            // 확정 잔액이 아니다 — 러닝을 마치고 판정을 거친 걸음만 적립된다
            Text(
                text = stringResource(R.string.run_estimated_note),
                style = MaterialTheme.typography.bodySmall,
                color = Slate,
            )
            EnergyMeter(current = energy, max = maxEnergy)
            Text(
                text = stringResource(R.string.run_earnable, "%,d".format(earnableSteps)),
                style = MaterialTheme.typography.bodySmall,
                color = Slate,
            )
            if (xpBoosted) {
                Text(
                    text = stringResource(R.string.run_boost_active),
                    style = MaterialTheme.typography.bodySmall,
                    fontWeight = FontWeight.Bold,
                    color = Volt,
                )
            }
            if (session.partySize > 1) {
                Text(
                    text = stringResource(R.string.crew_boost, RewardEconomy.partyBonusPercent(session.partySize)),
                    style = MaterialTheme.typography.bodySmall,
                    fontWeight = FontWeight.Bold,
                    color = Volt,
                )
            }
        }
    }
    val finishing = !session.isActive && session.lastRewardPoints != null

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(start = 18.dp, end = 18.dp, top = 10.dp, bottom = 22.dp),
        verticalArrangement = Arrangement.spacedBy(15.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                DarkIconButton(
                    icon = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = stringResource(R.string.cd_back),
                    onClick = onBack,
                )
                Box(modifier = Modifier.weight(1f), contentAlignment = Alignment.Center) {
                    if (session.isActive) {
                        StatusChip(isActive = session.isActive, isPaused = session.isPaused)
                    } else {
                        Wordmark(fontSize = 20.sp)
                    }
                }
                SupPill(balance = balance, onClick = null)
            }
        }

        if (finishing) {
            session.lastRewardPoints?.let { points ->
                item {
                    FinishCard(
                        session = session,
                        points = points,
                        upload = lastUpload,
                        look = look,
                        balance = balance,
                        todaySteps = todaySteps,
                        goal = dailyGoal,
                        onDone = viewModel::clearReward,
                    )
                }
            }
        } else {
            // 녹화 중이라는 것을 러닝 내내 보이게 둔다. 안 보이면 끝나고 뜨는
            // 저장 창이 난데없이 느껴지고, 취소할 자리도 없다.
            if (recordingCourse && !readyToSaveCourse) {
                item { CourseRecordingStrip(running = session.isActive, onCancel = viewModel::cancelRecording) }
            }

            if (session.isActive) {
                item {
                    RunHero(
                        paused = session.isPaused,
                        gpsFix = session.gpsFix,
                        elapsedSec = session.elapsedSec,
                        distanceKm = distanceKm,
                        avgPaceSec = avgPaceSec,
                    )
                }
            }

            item {
                CourseChallengeCard(
                    course = course,
                    sessionKm = distanceKm,
                    liveTrack = session.geoTrack,
                    gpsFix = session.gpsFix,
                    goalKm = goalKm,
                    onOpenCourses = onOpenCourses,
                    running = session.isActive,
                )
            }

            if (!session.isActive) {
                item { goalRingCard() }
                item {
                    StartRunButton(
                        title = stringResource(R.string.start_run),
                        subtitle = stringResource(R.string.start_run_sub),
                        onClick = {
                            val missing = StepPermissions.missing(context)
                            if (missing.isEmpty()) {
                                WalkSessionService.start(context)
                            } else {
                                permissionLauncher.launch(missing)
                            }
                        },
                    )
                }
            }

            if (session.isActive) {
                item { estimateCard() }
                // 러닝 중 실시간 경고 — 왜 거리가 안 늘어나는지 바로 알 수 있게 한다
                if (session.isActive && session.flaggedSegments > 0) {
                    item {
                        val voided = session.liveVerdict == RunVerdict.VOID
                        GlowCard(contentPadding = PaddingValues(14.dp), spacing = 5.dp) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(9.dp),
                            ) {
                                Icon(
                                    Icons.Filled.Warning,
                                    contentDescription = null,
                                    tint = if (voided) Alert else Color(0xFFD99A00),
                                    modifier = Modifier.size(17.dp),
                                )
                                Text(
                                    text = stringResource(
                                        if (voided) R.string.run_void_title else R.string.run_flagged_title,
                                    ),
                                    style = MaterialTheme.typography.titleSmall,
                                    color = Snow,
                                )
                            }
                            Text(
                                text = if (voided) {
                                    stringResource(R.string.run_void_body)
                                } else {
                                    stringResource(R.string.run_flagged_body, session.flaggedSegments)
                                },
                                fontSize = 11.sp,
                                color = Silver,
                                lineHeight = 17.sp,
                            )
                        }
                    }
                }

                item {
                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        // 일시정지 / 재개 · 종료 — 같은 크기로 나란히. 종료는 붉은 테로 갈라
                        // 손이 헷갈리지 않게 하고, 누르면 한 번 더 묻는다. 글자가 크면 위아래로.
                        val pauseButton: @Composable (Modifier) -> Unit = { m ->
                            RunControlButton(
                                text = if (session.isPaused) {
                                    stringResource(R.string.cd_resume)
                                } else {
                                    stringResource(R.string.cd_pause)
                                },
                                icon = if (session.isPaused) Icons.Filled.PlayArrow else Icons.Filled.Pause,
                                accent = if (session.isPaused) Volt else Snow,
                                onClick = {
                                    if (session.isPaused) WalkSessionService.resume(context)
                                    else WalkSessionService.pause(context)
                                },
                                modifier = m,
                            )
                        }
                        val stopButton: @Composable (Modifier) -> Unit = { m ->
                            RunControlButton(
                                text = stringResource(R.string.run_finish),
                                icon = Icons.Filled.Stop,
                                accent = Alert,
                                onClick = { confirmStop = true },
                                modifier = m,
                            )
                        }
                        if (largeText) {
                            pauseButton(Modifier.fillMaxWidth())
                            stopButton(Modifier.fillMaxWidth())
                        } else {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(10.dp),
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                pauseButton(Modifier.weight(1f))
                                stopButton(Modifier.weight(1f))
                            }
                        }
                        LapButton(
                            text = stringResource(R.string.run_lap),
                            onClick = { viewModel.recordLap() },
                            modifier = Modifier.fillMaxWidth(),
                            // 직전 랩에서 최소 50m는 나아가야 새 랩을 찍을 수 있다
                            enabled = running && distanceKm > (laps.lastOrNull()?.km ?: 0.0) + 0.05,
                        )
                    }
                }

                item { goalRingCard() }

                item {
                    val hrZone = hr?.let {
                        when {
                            it < 120 -> stringResource(R.string.hr_zone_recovery) to Silver
                            it < 150 -> stringResource(R.string.hr_zone_aerobic) to Volt
                            else -> stringResource(R.string.hr_zone_anaerobic) to Alert
                        }
                    }
                    val lastSplit = segments.lastOrNull()?.paceSec?.takeIf { it > 0 }
                    GlowCard(spacing = 14.dp) {
                        FlowRow(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                            verticalArrangement = Arrangement.spacedBy(16.dp),
                        ) {
                            MetricCell(
                                icon = Icons.Filled.Timer,
                                label = stringResource(R.string.stat_pace),
                                value = curPaceSec?.let { formatPace(it) } ?: "—",
                                fraction = paceFraction(curPaceSec),
                            )
                            MetricCell(
                                icon = Icons.Filled.Flag,
                                label = stringResource(R.string.run_lap_split),
                                value = lastSplit?.let { formatPace(it) } ?: "—",
                                fraction = paceFraction(lastSplit),
                            )
                            MetricCell(
                                icon = Icons.Filled.Favorite,
                                label = stringResource(R.string.stat_hr),
                                value = hr?.toString() ?: "—",
                                unit = "bpm",
                                fraction = (hr ?: 0) / 190f,
                                chip = stringResource(R.string.measurement_unavailable),
                                chipColor = hrZone?.second ?: Volt,
                            )

                            MetricCell(
                                icon = Icons.AutoMirrored.Filled.DirectionsRun,
                                label = stringResource(R.string.stat_cadence),
                                value = if (session.elapsedSec > 0) "%d".format(cadenceVal) else "—",
                                unit = "spm",
                                fraction = cadenceVal / 200f,
                            )
                            MetricCell(
                                icon = Icons.Filled.LocalFireDepartment,
                                label = stringResource(R.string.stat_calories),
                                value = "%,.0f".format(calories),
                                unit = "kcal",
                                fraction = (calories / 600.0).toFloat(),
                            )
                            MetricCell(
                                icon = Icons.AutoMirrored.Filled.DirectionsWalk,
                                label = stringResource(R.string.stat_steps),
                                value = "%,d".format(session.steps),
                                fraction = session.steps / 10_000f,
                            )

                            MetricCell(
                                icon = Icons.Filled.Speed,
                                label = stringResource(R.string.stat_speed),
                                value = if (session.elapsedSec > 0) "%.1f".format(speedVal) else "—",
                                unit = "km/h",
                                fraction = (speedVal / 15.0).toFloat(),
                            )
                            MetricCell(
                                icon = Icons.Filled.Terrain,
                                label = stringResource(R.string.run_elevation),
                                value = "—",
                                unit = "m",
                                fraction = 0f,
                                chip = stringResource(R.string.measurement_unavailable),
                            )
                            MetricCell(
                                icon = Icons.Filled.Schedule,
                                label = stringResource(R.string.run_total_time),
                                value = formatDuration(session.elapsedSec),
                                fraction = session.elapsedSec / 3_600f,
                            )
                        }
                    }
                }
            }

            if (laps.isNotEmpty() || session.isActive) {
                item {
                    GlowCard {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                        ) {
                            Column(
                                modifier = Modifier.weight(1f),
                                verticalArrangement = Arrangement.spacedBy(7.dp),
                            ) {
                                Text(
                                    text = stringResource(R.string.run_lap),
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = Snow,
                                )
                                // 최근 5개 랩만 — 마지막 완료 랩을 볼트로 강조
                                segments.takeLast(5).forEach { seg ->
                                    LapRow(
                                        index = seg.index,
                                        km = seg.km,
                                        paceSec = seg.paceSec,
                                        bpm = 0,
                                        highlight = seg.index == segments.size,
                                    )
                                }
                                // 진행 중인 부분 랩
                                if (session.isActive && segKm > 0.01) {
                                    LapRow(
                                        index = laps.size + 1,
                                        km = segKm,
                                        paceSec = segPaceSec,
                                        bpm = hr ?: 0,
                                        highlight = false,
                                        dimmed = true,
                                    )
                                }
                            }
                            Column(
                                horizontalAlignment = Alignment.End,
                                verticalArrangement = Arrangement.spacedBy(6.dp),
                            ) {
                                Box(
                                    modifier = Modifier
                                        .background(CarbonHigh, RoundedCornerShape(50))
                                        .border(1.dp, Edge, RoundedCornerShape(50))
                                        .padding(horizontal = 9.dp, vertical = 3.dp),
                                ) {
                                    Text(
                                        text = stringResource(R.string.run_pace_chart),
                                        fontSize = 9.sp,
                                        fontWeight = FontWeight.Bold,
                                        letterSpacing = 0.5.sp,
                                        color = Silver,
                                    )
                                }
                                PaceChart(
                                    paces = segments.map { it.paceSec },
                                    modifier = Modifier
                                        .width(132.dp)
                                        .height(104.dp),
                                )
                            }
                        }
                    }
                }
            }

            if (!session.isActive) {
                item { estimateCard() }
            }

            if (BuildConfig.DEBUG) {
                item {
                    TextButton(onClick = { viewModel.simulateSteps(100) }) {
                        Text(
                            text = stringResource(R.string.run_simulate),
                            style = MaterialTheme.typography.bodySmall,
                            color = Slate,
                        )
                    }
                }
            }
        }
    }

    if (confirmStop) {
        AlertDialog(
            onDismissRequest = { confirmStop = false },
            containerColor = com.stepup.android.ui.theme.Carbon,
            titleContentColor = Snow,
            textContentColor = Silver,
            title = { Text(stringResource(R.string.run_stop_confirm_title), fontWeight = FontWeight.Black) },
            text = { Text(stringResource(R.string.run_stop_confirm_body)) },
            confirmButton = {
                TextButton(onClick = {
                    confirmStop = false
                    WalkSessionService.stop(context)
                }) {
                    Text(stringResource(R.string.run_stop_confirm_yes), color = Volt, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { confirmStop = false }) {
                    Text(stringResource(R.string.run_stop_confirm_no), color = Silver)
                }
            },
        )
    }

    if (readyToSaveCourse) {
        SaveCourseDialog(
            track = recordedTrack,
            onSave = viewModel::saveRecordedCourse,
            onDismiss = viewModel::cancelRecording,
        )
    }

    if (showGoalDialog) {
        AlertDialog(
            onDismissRequest = { showGoalDialog = false },
            containerColor = Carbon,
            titleContentColor = Snow,
            textContentColor = Silver,
            confirmButton = {
                TextButton(onClick = { showGoalDialog = false }) {
                    Text(
                        text = stringResource(R.string.common_ok),
                        color = Volt,
                        fontWeight = FontWeight.Bold,
                    )
                }
            },
            title = {
                Text(
                    text = stringResource(R.string.run_goal_dialog_title),
                    fontWeight = FontWeight.Black,
                )
            },
            text = {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceEvenly,
                ) {
                    StepperButton(text = "−") {
                        viewModel.setGoalKm(if (goalKm > 42.0) 42.0 else goalKm - 0.5)
                    }
                    Text(
                        text = "%.1f km".format(goalKm),
                        fontFamily = com.stepup.android.ui.theme.StepUpNumbers,
                        fontSize = 24.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = Snow,
                    )
                    StepperButton(text = "+") {
                        viewModel.setGoalKm(if (goalKm >= 42.0) 42.2 else goalKm + 0.5)
                    }
                }
            },
        )
    }
}

/** 준비 / 러닝 중 / 일시정지 — 러닝 중엔 볼트 점이 호흡한다. */
@Composable
private fun StatusChip(isActive: Boolean, isPaused: Boolean) {
    val running = isActive && !isPaused
    val pulse = breathing()
    val label = when {
        !isActive -> stringResource(R.string.run_ready)
        isPaused -> stringResource(R.string.run_paused)
        else -> stringResource(R.string.run_live)
    }
    val accent = if (running) Volt else Silver
    Row(
        modifier = Modifier
            .background(
                if (running) Volt.copy(alpha = 0.10f) else CarbonHigh,
                RoundedCornerShape(50),
            )
            .border(
                1.dp,
                if (running) Volt.copy(alpha = 0.35f) else Color.Transparent,
                RoundedCornerShape(50),
            )
            .padding(horizontal = 12.dp, vertical = 7.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier
                .size(6.dp)
                .alpha(if (running) 0.45f + pulse * 0.55f else 0.7f)
                .background(accent, CircleShape),
        )
        Spacer(Modifier.width(7.dp))
        Text(
            text = label,
            color = accent,
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
            letterSpacing = 1.sp,
        )
    }
}

/** 지표 그리드 셀 — 아이콘+라벨 / 큰 값+단위 / (선택) 존 칩 / 얇은 게이지 */
@Composable
@OptIn(ExperimentalLayoutApi::class)
private fun MetricCell(
    icon: ImageVector,
    label: String,
    value: String,
    fraction: Float,
    unit: String? = null,
    chip: String? = null,
    chipColor: Color = Volt,
) {
    Column(
        modifier = Modifier.fillMaxWidth(if (LocalDensity.current.fontScale > 1.2f) 1f else 0.48f),
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = Slate,
                modifier = Modifier.size(16.dp),
            )
            Text(
                text = label,
                fontSize = 12.sp,
                color = Slate,
            )
        }
        FlowRow(
            verticalArrangement = Arrangement.spacedBy(2.dp),
            horizontalArrangement = Arrangement.spacedBy(3.dp),
        ) {
            Text(
                text = value,
                fontFamily = com.stepup.android.ui.theme.StepUpNumbers,
                fontSize = 17.sp,
                fontWeight = FontWeight.ExtraBold,
                letterSpacing = (-0.3).sp,
                color = Snow,
            )
            if (unit != null) {
                Text(
                    text = unit,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = Volt,
                    modifier = Modifier.padding(bottom = 2.dp),
                )
            }
        }
        if (chip != null) {
            Box(
                modifier = Modifier
                    .border(1.dp, chipColor.copy(alpha = 0.45f), RoundedCornerShape(50))
                    .padding(horizontal = 7.dp, vertical = 2.dp),
            ) {
                Text(
                    text = chip,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = chipColor,
                )
            }
        }
        BarMeter(fraction = fraction, height = 3.dp)
    }
}

/** Lap measurements; heart rate remains unavailable until actually measured. */
@Composable
private fun LapRow(
    index: Int,
    km: Double,
    paceSec: Long,
    bpm: Int,
    highlight: Boolean,
    dimmed: Boolean = false,
) {
    val main = when {
        highlight -> Volt
        dimmed -> Slate
        else -> Silver
    }
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = "%d".format(index),
            fontFamily = com.stepup.android.ui.theme.StepUpNumbers,
            fontSize = 10.sp,
            fontWeight = FontWeight.Bold,
            color = if (highlight) Volt else Slate,
            modifier = Modifier.width(18.dp),
        )
        Text(
            text = "%.2f".format(km),
            fontFamily = com.stepup.android.ui.theme.StepUpNumbers,
            fontSize = 11.sp,
            fontWeight = FontWeight.SemiBold,
            color = main,
            maxLines = 1,
            modifier = Modifier.weight(1f),
        )
        Text(
            text = if (paceSec > 0) formatPace(paceSec) else "—",
            fontSize = 11.sp,
            fontWeight = FontWeight.SemiBold,
            color = main,
            maxLines = 1,
            modifier = Modifier.weight(1.2f),
        )
        Text(
            text = if (bpm > 0) "%d bpm".format(bpm) else "—",
            fontSize = 10.sp,
            color = if (highlight) Volt else Slate,
            maxLines = 1,
        )
    }
}

/** 랩 스플릿 페이스 폴리라인 — 빠를수록 위쪽. 데이터 2개 미만이면 기준선만 그린다. */
@Composable
private fun PaceChart(paces: List<Long>, modifier: Modifier = Modifier) {
    Canvas(modifier) {
        val valid = paces.filter { it > 0 }
        if (valid.size < 2) {
            drawLine(
                color = Snow.copy(alpha = 0.12f),
                start = Offset(0f, size.height * 0.55f),
                end = Offset(size.width, size.height * 0.55f),
                strokeWidth = 2f,
            )
            return@Canvas
        }
        val minP = valid.min().toFloat()
        val maxP = valid.max().toFloat()
        val span = (maxP - minP).coerceAtLeast(1f)
        val inset = 5f
        fun pointAt(i: Int): Offset {
            val x = inset + (size.width - inset * 2) * i / (valid.size - 1)
            // 초/km가 작을수록(빠를수록) 위로
            val y = size.height * (0.12f + 0.72f * ((valid[i] - minP) / span))
            return Offset(x, y)
        }
        val line = Path()
        for (i in valid.indices) {
            val p = pointAt(i)
            if (i == 0) line.moveTo(p.x, p.y) else line.lineTo(p.x, p.y)
        }
        val fill = Path().apply {
            addPath(line)
            lineTo(pointAt(valid.size - 1).x, size.height)
            lineTo(inset, size.height)
            close()
        }
        drawPath(fill, Brush.verticalGradient(listOf(Volt.copy(alpha = 0.22f), Color.Transparent)))
        drawPath(line, Volt, style = Stroke(width = 2.5f, cap = StrokeCap.Round, join = StrokeJoin.Round))
        drawCircle(Volt, radius = 3.5f, center = pointAt(valid.size - 1))
    }
}


/** GhostButton 톤의 랩 기록 버튼 — 깃발 아이콘 포함 */
@Composable
private fun LapButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
) {
    val shape = RoundedCornerShape(50)
    Row(
        modifier = modifier
            .height(44.dp)
            .clip(shape)
            .background(Volt.copy(alpha = if (enabled) 0.08f else 0.03f), shape)
            .border(1.dp, Volt.copy(alpha = if (enabled) 0.45f else 0.15f), shape)
            .feedbackClickable(enabled = enabled, onClick = onClick),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center,
    ) {
        Icon(
            imageVector = Icons.Filled.Flag,
            contentDescription = null,
            tint = if (enabled) Volt else Slate,
            modifier = Modifier.size(14.dp),
        )
        Spacer(Modifier.width(5.dp))
        Text(
            text = text,
            color = if (enabled) Volt else Slate,
            fontSize = 13.sp,
            fontWeight = FontWeight.SemiBold,
            maxLines = 1,
        )
    }
}

/** 목표 거리 스테퍼 (− / +) */
@Composable
private fun StepperButton(text: String, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .size(40.dp)
            .clip(CircleShape)
            .background(CarbonHigh, CircleShape)
            .border(1.dp, Edge, CircleShape)
            .feedbackClickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = text,
            fontSize = 18.sp,
            fontWeight = FontWeight.Bold,
            color = Volt,
        )
    }
}

/** 누적 랩 스냅샷 → 구간 리스트 (직전 랩과의 차) */
private fun lapSegments(laps: List<RunLap>): List<LapSegment> {
    var prevKm = 0.0
    var prevSec = 0L
    return laps.map { lap ->
        val dKm = lap.km - prevKm
        val dSec = lap.splitSec - prevSec
        prevKm = lap.km
        prevSec = lap.splitSec
        LapSegment(
            index = lap.index,
            km = dKm,
            sec = dSec,
            paceSec = if (dKm >= 0.001 && dSec > 0) (dSec / dKm).toLong() else 0L,
        )
    }
}


private fun formatPace(secPerKm: Long): String =
    "%d'%02d\"".format(secPerKm / 60, secPerKm % 60)

/** 페이스 게이지 정규화 — 15'00"/km ≈ 0, 5'00"/km ≈ 1 */
private fun paceFraction(secPerKm: Long?): Float =
    if (secPerKm == null || secPerKm <= 0) 0f else ((900f - secPerKm) / 600f).coerceIn(0f, 1f)

private fun formatDuration(totalSec: Long): String {
    val hours = totalSec / 3600
    val minutes = (totalSec % 3600) / 60
    val seconds = totalSec % 60
    return if (hours > 0) {
        "%d:%02d:%02d".format(hours, minutes, seconds)
    } else {
        "%02d:%02d".format(minutes, seconds)
    }
}


/**
 * 오늘의 챌린지 카드 — 선택한 코스의 실제 GPS 트랙을 네온 지도로 보여준다.
 *
 * 러닝 중에는 내가 실제로 그리고 있는 GPS 경로를 함께 얹고,
 * 완주 진행도(코스 거리 대비 세션 거리)와 거리 정량 보상을 표시한다.
 * 코스를 고르지 않았으면 아트 지도와 코스 선택 버튼을 보여준다.
 */
@Composable
private fun CourseChallengeCard(
    course: RunCourse?,
    sessionKm: Double,
    liveTrack: List<GeoPoint>,
    gpsFix: Boolean,
    goalKm: Double,
    onOpenCourses: () -> Unit,
    /** 달리는 중이면 지도와 완주 진행만 — 코스 고르기는 러닝 전에 한다 */
    running: Boolean = false,
) {
    GlowCard(contentPadding = PaddingValues(0.dp), spacing = 0.dp) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(200.dp),
        ) {
            // 지도에 그릴 좌표 — 달리는 중이면 내 실시간 경로가 우선이다.
            // 코스를 골랐어도 "내가 지금 어디를 뛰고 있는지"가 더 급한 정보다.
            val routePoints = when {
                liveTrack.size >= 2 -> liveTrack
                course != null && course.hasTrack -> course.points
                else -> emptyList()
            }

            // 보여 줄 경로가 없으면 **내가 선 자리**를 보여 준다. 예전에는
            // 지어낸 아트 지도가 나왔는데, 저 지그재그는 어디에도 없는 길이라
            // 사용자는 GPS 가 엉뚱한 곳을 잡은 줄 안다. 아직 코스를 안 골랐다는
            // 사실은 아래 "선택한 코스가 없어요"가 이미 말하고 있다.
            val here = rememberCurrentLocation(enabled = routePoints.isEmpty())
            val mapPoints = when {
                routePoints.isNotEmpty() -> routePoints
                here != null -> listOf(here)
                else -> emptyList()
            }
            when {
                mapPoints.isNotEmpty() -> {
                    val progress = if (course != null && course.distanceKm > 0) {
                        (sessionKm / course.distanceKm).toFloat().coerceIn(0f, 1f)
                    } else {
                        0f
                    }
                    // 시드는 세션 내내 고정이어야 한다. 좌표 수로 만들면 8점마다
                    // 폴백 도로망이 다시 추첨돼 배경이 눈앞에서 뒤바뀐다.
                    //
                    // 내 자리를 보여 주는 동안에는 좌표가 몇 초마다 갱신되므로
                    // 도 단위로 뭉뚱그린 값에 묶는다. 한 도시 안에서는 같은 키다.
                    val first = mapPoints.firstOrNull()
                    val fallbackSeed = remember(
                        course?.id,
                        first?.lat?.toInt(),
                        first?.lng?.toInt(),
                    ) {
                        course?.id?.toInt() ?: first?.let {
                            (it.lat * 1e4).toInt() xor (it.lng * 1e4).toInt()
                        } ?: 0
                    }
                    LiveRouteMap(
                        interactive = true,
                        points = mapPoints,
                        seed = fallbackSeed,
                        progress = progress.takeIf { course != null && sessionKm > 0.005 },
                        modifier = Modifier.fillMaxSize(),
                    )
                }

                // 권한이 없거나 위치가 꺼져 있어 내 자리조차 모를 때. 예전에는 아트
                // 지도(지어낸 경로)가 나왔는데, 그 선은 실제로 달린 길처럼 읽힌다.
                // 지도 대신 위치를 기다린다고만 적는다.
                else -> MapWaiting(Modifier.fillMaxSize())
            }

            // 앱 안 지도는 어디를 뛰었는지까지, 확대·길안내는 구글 지도로
            if (mapPoints.size >= 2) {
                val context = LocalContext.current
                Row(
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .padding(10.dp)
                        .background(Night.copy(alpha = 0.82f), RoundedCornerShape(50))
                        .border(1.dp, Edge, RoundedCornerShape(50))
                        .quietClickable { ExternalIntents.openRouteInMaps(context, mapPoints) }
                        .padding(horizontal = 10.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(5.dp),
                ) {
                    Icon(
                        Icons.Filled.Map,
                        contentDescription = null,
                        tint = Volt,
                        modifier = Modifier.size(13.dp),
                    )
                    Text(
                        text = stringResource(R.string.map_open_google),
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        color = Snow,
                    )
                }
            }

            // 오늘의 챌린지 + GPS 상태
            Row(
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .padding(12.dp),
                horizontalArrangement = Arrangement.spacedBy(7.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Box(
                    modifier = Modifier
                        .background(Volt.copy(alpha = 0.14f), RoundedCornerShape(50))
                        .border(1.dp, Volt.copy(alpha = 0.5f), RoundedCornerShape(50))
                        .padding(horizontal = 9.dp, vertical = 4.dp),
                ) {
                    Text(
                        text = stringResource(R.string.course_today),
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Black,
                        letterSpacing = 0.6.sp,
                        color = Volt,
                    )
                }
                Row(
                    modifier = Modifier
                        .background(Night.copy(alpha = 0.60f), RoundedCornerShape(50))
                        .border(1.dp, Edge, RoundedCornerShape(50))
                        .padding(horizontal = 9.dp, vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                ) {
                    Icon(
                        imageVector = Icons.Filled.GpsFixed,
                        contentDescription = null,
                        tint = if (gpsFix) Volt else Slate,
                        modifier = Modifier.size(11.dp),
                    )
                    Text(
                        text = "GPS",
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.sp,
                        color = if (gpsFix) Snow else Slate,
                    )
                }
            }

            Box(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(12.dp)
                    .size(32.dp)
                    .background(Night.copy(alpha = 0.60f), CircleShape)
                    .border(1.dp, Edge, CircleShape),
                contentAlignment = Alignment.Center,
            ) {
                Text(text = "N", fontSize = 12.sp, fontWeight = FontWeight.Black, color = Volt)
            }

        }
            // 코스 이름 · 거리 · 코스 변경 — 러닝 전에만
            if (!running) Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(14.dp),
                verticalArrangement = Arrangement.spacedBy(3.dp),
            ) {
                Text(
                    text = course?.name ?: stringResource(R.string.course_none_title),
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = Snow,
                )
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    Text(
                        text = "%.2f km".format(course?.distanceKm ?: goalKm),
                        fontFamily = com.stepup.android.ui.theme.StepUpNumbers,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = Volt,
                    )
                    Row(
                        modifier = Modifier.quietClickable(onOpenCourses),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(
                            text = stringResource(
                                if (course != null) R.string.course_change else R.string.course_pick,
                            ),
                            fontSize = 11.sp,
                            color = Silver,
                        )
                        Icon(
                            imageVector = Icons.Filled.ChevronRight,
                            contentDescription = null,
                            tint = Silver,
                            modifier = Modifier.size(14.dp),
                        )
                    }
                }
            }


        // 완주 보상 줄 — 코스가 있을 때만
        if (course != null) {
            val progress = if (course.distanceKm > 0) {
                (sessionKm / course.distanceKm).coerceIn(0.0, 1.0).toFloat()
            } else {
                0f
            }
            val remaining = (course.distanceKm - sessionKm).coerceAtLeast(0.0)
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 15.dp, vertical = 13.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    Text(
                        text = stringResource(R.string.course_to_finish),
                        fontSize = 11.sp,
                        color = Silver,
                    )
                    Text(
                        text = "%.2f km".format(remaining),
                        fontFamily = com.stepup.android.ui.theme.StepUpNumbers,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Black,
                        color = Snow,
                    )
                    Spacer(Modifier.weight(1f))
                    HexEmblem(size = 15.dp, glow = false)
                    Text(
                        text = stringResource(
                            R.string.course_reward_value,
                            "%.1f".format(course.reward),
                        ),
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Black,
                        color = Volt,
                    )
                }
                BarMeter(fraction = progress, height = 7.dp)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    listOf("25%", "50%", "75%").forEachIndexed { index, label ->
                        Text(
                            text = label,
                            fontSize = 8.5.sp,
                            color = if (progress >= (index + 1) * 0.25f) Volt else Slate,
                        )
                    }
                    Text(
                        text = stringResource(
                            R.string.course_per_km,
                            "%.0f".format(CourseRewards.SUP_PER_KM),
                            "%.0f".format(CourseRewards.MAX_REWARD),
                        ),
                        fontSize = 8.5.sp,
                        color = Slate,
                    )
                }
            }
        } else if (!running) {
            // 코스 미선택 — 고르러 가기
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 15.dp, vertical = 13.dp),
                verticalArrangement = Arrangement.spacedBy(9.dp),
            ) {
                Text(
                    text = stringResource(R.string.course_none_body),
                    style = MaterialTheme.typography.bodySmall,
                    color = Silver,
                )
                VoltButton(
                    text = stringResource(R.string.course_pick),
                    onClick = onOpenCourses,
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        }
    }
}

/**
 * 위치를 모를 때의 지도 자리 — 흐린 격자와 안내 한 줄.
 *
 * 경로처럼 보이는 선은 긋지 않는다. GPS 가 잡히면 [LiveRouteMap] 이 이 자리를
 * 실제 위치와 경로로 바꾼다.
 */
@Composable
private fun MapWaiting(modifier: Modifier = Modifier) {
    val grid = Edge
    Box(modifier = modifier.background(Night), contentAlignment = Alignment.Center) {
        androidx.compose.foundation.Canvas(Modifier.fillMaxSize()) {
            val step = 28.dp.toPx()
            var x = 0f
            while (x < size.width) {
                drawLine(grid.copy(alpha = 0.45f), androidx.compose.ui.geometry.Offset(x, 0f), androidx.compose.ui.geometry.Offset(x, size.height), strokeWidth = 1f)
                x += step
            }
            var y = 0f
            while (y < size.height) {
                drawLine(grid.copy(alpha = 0.45f), androidx.compose.ui.geometry.Offset(0f, y), androidx.compose.ui.geometry.Offset(size.width, y), strokeWidth = 1f)
                y += step
            }
        }
        Column(
            modifier = Modifier.padding(horizontal = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            Icon(Icons.Filled.GpsFixed, contentDescription = null, tint = Slate, modifier = Modifier.size(22.dp))
            Text(
                text = stringResource(R.string.map_waiting_title),
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                color = Snow,
                textAlign = TextAlign.Center,
            )
            Text(
                text = stringResource(R.string.map_waiting_body),
                fontSize = 12.sp,
                color = Silver,
                textAlign = TextAlign.Center,
                lineHeight = 16.sp,
            )
        }
    }
}

/**
 * 코스 녹화 중이라는 띠.
 *
 * 러닝 화면은 녹화 중이나 아니나 똑같이 생겼다. 표시가 없으면 끝나고 뜨는
 * 저장 창이 난데없이 느껴지고, 그만두고 싶어도 그만둘 자리가 없다.
 */
@Composable
private fun CourseRecordingStrip(running: Boolean, onCancel: () -> Unit) {
    GlowCard(
        accent = true,
        contentPadding = PaddingValues(horizontal = 14.dp, vertical = 12.dp),
        spacing = 6.dp,
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Icon(
                imageVector = Icons.Filled.Route,
                contentDescription = null,
                tint = Volt,
                modifier = Modifier.size(18.dp),
            )
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(
                    text = stringResource(R.string.course_rec_title),
                    style = MaterialTheme.typography.titleSmall,
                    color = Snow,
                )
                Text(
                    text = stringResource(
                        if (running) R.string.course_rec_running else R.string.course_rec_ready,
                    ),
                    fontSize = 11.sp,
                    color = Silver,
                    lineHeight = 16.sp,
                )
            }
            Text(
                text = stringResource(R.string.common_cancel),
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                color = Slate,
                modifier = Modifier
                    .quietClickable(onCancel)
                    .padding(horizontal = 6.dp, vertical = 4.dp),
            )
        }
    }
}

/**
 * 러닝이 끝나고 뜨는 코스 저장 창.
 *
 * 여기서 저장해야 방금 뛴 길이 코스가 된다. 닫으면 그 트랙은 버려진다 —
 * 그래서 닫기 버튼에도 "저장 안 함"이라고 적는다. "취소"라고만 적으면
 * 나중에 저장할 수 있다고 읽힌다.
 */
@Composable
private fun SaveCourseDialog(
    track: List<GeoPoint>,
    onSave: (name: String, area: String, shared: Boolean) -> Unit,
    onDismiss: () -> Unit,
) {
    var name by rememberSaveable { mutableStateOf("") }
    var area by rememberSaveable { mutableStateOf("") }
    var share by rememberSaveable { mutableStateOf(true) }
    val km = remember(track) { track.trackDistanceKm() }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = Carbon,
        titleContentColor = Snow,
        textContentColor = Silver,
        title = {
            Text(text = stringResource(R.string.course_save_title), fontWeight = FontWeight.Black)
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(11.dp)) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(140.dp)
                        .clip(RoundedCornerShape(16.dp))
                        .background(Night)
                        .border(1.dp, Edge, RoundedCornerShape(16.dp)),
                ) {
                    LiveRouteMap(
                        points = track,
                        seed = track.size,
                        modifier = Modifier.fillMaxSize(),
                    )
                }
                Text(
                    text = stringResource(R.string.course_save_body, "%.2f".format(km)),
                    style = MaterialTheme.typography.titleSmall,
                    color = Volt,
                )
                LabeledField(
                    label = stringResource(R.string.course_name_hint),
                    value = name,
                    onValueChange = { name = it },
                )
                LabeledField(
                    label = stringResource(R.string.course_area_hint),
                    value = area,
                    onValueChange = { area = it },
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        text = stringResource(R.string.course_share_toggle),
                        style = MaterialTheme.typography.bodyMedium,
                        color = Snow,
                    )
                    Switch(
                        checked = share,
                        onCheckedChange = { share = it },
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = OnVolt,
                            checkedTrackColor = Volt,
                            uncheckedThumbColor = Silver,
                            uncheckedTrackColor = CarbonHigh,
                        ),
                    )
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = { onSave(name, area, share) },
                enabled = name.isNotBlank(),
            ) {
                Text(
                    text = stringResource(R.string.course_register),
                    color = if (name.isNotBlank()) Volt else Slate,
                    fontWeight = FontWeight.Bold,
                )
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(text = stringResource(R.string.course_save_skip), color = Silver)
            }
        },
    )
}

@Composable
private fun RunTimeRing(
    distanceKm: Double, goalKm: Double, elapsedSec: Long, running: Boolean,
    onEditGoal: () -> Unit, modifier: Modifier = Modifier.size(150.dp),
) {
    NeonRing(
        progress = if (goalKm > 0) (distanceKm / goalKm).toFloat() else 0f,
        modifier = modifier,
        ringWidth = 9.dp,
        glowAlpha = if (running) 0.20f else 0.12f,
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(2.dp),
        ) {
            Icon(
                imageVector = Icons.AutoMirrored.Filled.DirectionsRun,
                contentDescription = null,
                tint = Volt,
                modifier = Modifier.size(14.dp),
            )
            Text(
                text = stringResource(R.string.run_total_time),
                fontSize = 9.sp,
                color = Slate,
            )
            Text(
                text = formatDuration(elapsedSec),
                fontFamily = com.stepup.android.ui.theme.StepUpNumbers,
                fontSize = 23.sp,
                fontWeight = FontWeight.ExtraBold,
                letterSpacing = (-0.5).sp,
                color = Snow,
            )
            Row(
                modifier = Modifier.quietClickable { onEditGoal() },
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(3.dp),
            ) {
                Text(
                    text = stringResource(R.string.run_goal_label, "%.2f".format(goalKm)),
                    fontSize = 10.sp,
                    color = Silver,
                )
                Icon(
                    imageVector = Icons.Filled.Edit,
                    contentDescription = null,
                    tint = Volt,
                    modifier = Modifier.size(14.dp),
                )
            }
        }
    }
}

// ── 러닝 완료 ───────────────────────────────────────────────────────

/**
 * 러닝 완료 카드.
 *
 * ── 머리말은 서버가 정한다 ──
 *
 * 적립은 러닝이 끝날 때 앱 안의 원장에 먼저 적힌다. 서버가 그 기록을
 * 확인(서명)해야 온체인으로 청구할 수 있다. 그래서 서명을 받기 전에는
 * "적립 완료"라고 적지 않고 "서버 확인 중"이라고 적는다. 거절되면 그렇게
 * 적는다.
 *
 * ── 중복 적립 ──
 *
 * 이 카드는 이미 끝난 정산의 결과를 **보여 주기만** 한다. "완료"를 눌러도,
 * 화면을 다시 열어도 정산을 다시 하지 않는다 — 정산은 서비스가 러닝을
 * 끝낼 때 한 번만 한다.
 */
@Composable
private fun FinishCard(
    session: WalkSessionState,
    points: Double,
    upload: String?,
    look: com.stepup.android.domain.AvatarLook,
    balance: Double,
    @Suppress("UNUSED_PARAMETER") todaySteps: Int,
    @Suppress("UNUSED_PARAMETER") goal: Int,
    onDone: () -> Unit,
) {
    val context = LocalContext.current
    val voided = session.lastVerdict == RunVerdict.VOID
    val km = if (session.lastGpsKm > 0.0) session.lastGpsKm else RewardEconomy.distanceMeters(session.lastSessionSteps) / 1000
    val paceSec: Long? = if (km >= 0.05 && session.lastElapsedSec > 0) (session.lastElapsedSec / km).toLong() else null
    // 서버가 확인한 뒤에만 "적립 완료". 그 전에는 확인 중이라고 적는다.
    val headline = when {
        voided -> R.string.run_void_title
        upload == UploadState.SIGNED.name -> R.string.finish_confirmed
        upload == UploadState.REJECTED.name -> R.string.finish_rejected
        else -> R.string.finish_pending_short
    }
    val shareText = stringResource(
        R.string.finish_share_text,
        "%.1f".format(km),
        formatDuration(session.lastElapsedSec),
        "%,.0f".format(points),
    )

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .reveal(session.lastStartedAt)
            .celebrate(if (!voided) session.lastStartedAt else null),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Column(
            modifier = Modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(2.dp),
        ) {
            Text(
                text = stringResource(R.string.finish_title),
                fontSize = 26.sp,
                fontWeight = FontWeight.Black,
                letterSpacing = (-0.6).sp,
                color = Snow,
            )
            Text(
                text = stringResource(headline),
                fontSize = 15.sp,
                fontWeight = FontWeight.Bold,
                color = if (voided || upload == UploadState.REJECTED.name) Alert else Silver,
                textAlign = TextAlign.Center,
            )
            Row(verticalAlignment = Alignment.Bottom) {
                Text(
                    text = "+%,.0f".format(points),
                    style = androidx.compose.ui.text.TextStyle(
                        brush = com.stepup.android.ui.theme.VoltInk,
                        shadow = androidx.compose.ui.graphics.Shadow(
                            color = Volt.copy(alpha = 0.55f),
                            blurRadius = 28f,
                        ),
                    ),
                    fontFamily = com.stepup.android.ui.theme.StepUpNumbers,
                    fontSize = 52.sp,
                    fontWeight = FontWeight.ExtraBold,
                    letterSpacing = (-1.5).sp,
                    maxLines = 1,
                )
                Text(
                    text = " SUP",
                    fontFamily = com.stepup.android.ui.theme.StepUpNumbers,
                    fontSize = 26.sp,
                    fontWeight = FontWeight.Bold,
                    color = com.stepup.android.ui.theme.VoltText,
                    modifier = Modifier.padding(bottom = 7.dp),
                )
            }
            if (!voided && upload != UploadState.SIGNED.name && upload != UploadState.REJECTED.name) {
                Text(
                    text = stringResource(R.string.finish_pending_note),
                    fontSize = 12.sp,
                    color = Silver,
                    textAlign = TextAlign.Center,
                    lineHeight = 17.sp,
                )
            }
            if (voided) {
                Text(
                    text = stringResource(R.string.run_void_body),
                    style = MaterialTheme.typography.bodySmall,
                    color = Silver,
                    textAlign = TextAlign.Center,
                )
            }
        }

        // 내 캐릭터 — 축하 자세 그림이 아직 없어 같은 성별의 그림을 쓴다
        CharacterStage(
            look = look,
            pose = if (voided) AvatarPose.IDLE else AvatarPose.CHEER,
            modifier = Modifier
                .fillMaxWidth()
                .height(if (androidx.compose.ui.platform.LocalDensity.current.fontScale > 1.3f) 200.dp else 240.dp),
            characterFraction = 0.9f,
        )

        // 거리 · 시간 · 페이스
        GlowCard(contentPadding = PaddingValues(horizontal = 10.dp, vertical = 12.dp)) {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                FinishStat(stringResource(R.string.stat_distance), "%.2f".format(km), "km", Modifier.weight(1f))
                VerticalHairline(height = 36.dp)
                FinishStat(stringResource(R.string.home_run_time), formatDuration(session.lastElapsedSec), "", Modifier.weight(1f))
                VerticalHairline(height = 36.dp)
                FinishStat(stringResource(R.string.run_avg_pace), paceSec?.let { formatPace(it) } ?: "—", "", Modifier.weight(1f))
            }
        }

        // 갱신된 보유 포인트
        GlowCard(contentPadding = PaddingValues(horizontal = 16.dp, vertical = 14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                HexEmblem(size = 22.dp, glow = false)
                Text(
                    text = stringResource(R.string.finish_balance),
                    modifier = Modifier.weight(1f),
                    fontSize = 14.sp,
                    color = Silver,
                )
                Text(
                    text = "%,.0f SUP".format(balance),
                    fontFamily = com.stepup.android.ui.theme.StepUpNumbers,
                    fontSize = 17.sp,
                    fontWeight = FontWeight.Bold,
                    color = Snow,
                )
            }
        }

        PrimaryCta(
            text = stringResource(R.string.finish_done),
            icon = Icons.Filled.Check,
            showArrow = false,
            onClick = onDone,
        )
        GhostButton(
            text = stringResource(R.string.finish_share),
            onClick = {
                val send = android.content.Intent(android.content.Intent.ACTION_SEND).apply {
                    type = "text/plain"
                    putExtra(android.content.Intent.EXTRA_TEXT, shareText)
                }
                context.startActivity(android.content.Intent.createChooser(send, null))
            },
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 54.dp),
            accent = Silver,
        )
    }
}

/**
 * 러닝 중 머리 — 상태 · GPS · 큰 시계 · 거리와 평균 페이스.
 *
 * 뛰면서 흘끗 보는 화면이라 시계를 가장 크게 둔다. 시계는 글자 크기 설정과
 * 무관하게 같은 크기다 — 이미 충분히 크고, 더 키우면 한 줄에 들어가지 않는다.
 */
@Composable
private fun RunHero(
    paused: Boolean,
    gpsFix: Boolean,
    elapsedSec: Long,
    distanceKm: Double,
    avgPaceSec: Long?,
) {
    val density = androidx.compose.ui.platform.LocalDensity.current
    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        Text(
            text = stringResource(if (paused) R.string.run_paused else R.string.run_active),
            fontSize = 24.sp,
            fontWeight = FontWeight.Black,
            color = Snow,
        )
        GpsChip(gpsFix)
        Text(
            text = formatDuration(elapsedSec),
            fontFamily = com.stepup.android.ui.theme.StepUpNumbers,
            fontSize = with(density) { 68.dp.toSp() },
            fontWeight = FontWeight.ExtraBold,
            letterSpacing = (-1).sp,
            color = if (paused) Silver else Snow,
            maxLines = 1,
            softWrap = false,
        )
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            StatCell(
                icon = Icons.Filled.LocationOn,
                value = "%.2f".format(distanceKm),
                unit = "km",
                label = stringResource(R.string.stat_distance),
                modifier = Modifier
                    .weight(1f)
                    .padding(start = 12.dp),
            )
            VerticalHairline(height = 44.dp)
            StatCell(
                icon = Icons.Filled.Timer,
                value = avgPaceSec?.let { formatPace(it) } ?: "—",
                unit = "",
                label = stringResource(R.string.run_avg_pace),
                modifier = Modifier
                    .weight(1f)
                    .padding(start = 16.dp),
            )
        }
    }
}

/** GPS 상태 알약 — 잡혔으면 시안, 찾는 중이면 흐리게 */
@Composable
private fun GpsChip(fix: Boolean) {
    val color = if (fix) com.stepup.android.ui.theme.Cyan else Slate
    Row(
        modifier = Modifier
            .clip(RoundedCornerShape(50))
            .background(color.copy(alpha = 0.10f))
            .border(1.dp, color.copy(alpha = 0.7f), RoundedCornerShape(50))
            .padding(horizontal = 12.dp, vertical = 5.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        Icon(Icons.Filled.GpsFixed, contentDescription = null, tint = color, modifier = Modifier.size(14.dp))
        Text(
            text = stringResource(if (fix) R.string.run_gps_ok else R.string.run_gps_search),
            fontSize = 13.sp,
            fontWeight = FontWeight.Bold,
            color = if (fix) Snow else Silver,
        )
    }
}

/** 러닝 조작 버튼 — 어두운 바탕에 색 테. 일시정지와 종료가 같은 크기로 선다. */
@Composable
private fun RunControlButton(
    text: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    accent: Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val shape = RoundedCornerShape(18.dp)
    Row(
        modifier = modifier
            .heightIn(min = 58.dp)
            .clip(shape)
            .background(accent.copy(alpha = 0.08f), shape)
            .border(1.5.dp, accent.copy(alpha = 0.75f), shape)
            .quietClickable(onClick)
            .padding(horizontal = 12.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center,
    ) {
        Icon(icon, contentDescription = null, tint = accent, modifier = Modifier.size(22.dp))
        Spacer(Modifier.width(8.dp))
        Text(
            text = text,
            fontSize = 17.sp,
            fontWeight = FontWeight.Bold,
            color = if (accent == Snow) Snow else accent,
            maxLines = 1,
        )
    }
}

@Composable
private fun FinishStat(label: String, value: String, unit: String, modifier: Modifier = Modifier) {
    Column(modifier = modifier, horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(2.dp)) {
        Text(text = label, fontSize = 12.sp, color = Silver, maxLines = 1)
        Row(verticalAlignment = Alignment.Bottom) {
            Text(
                text = value,
                fontFamily = com.stepup.android.ui.theme.StepUpNumbers,
                fontSize = 19.sp,
                fontWeight = FontWeight.Bold,
                color = Snow,
                maxLines = 1,
            )
            if (unit.isNotEmpty()) {
                Text(text = " $unit", fontSize = 12.sp, color = Silver, modifier = Modifier.padding(bottom = 2.dp))
            }
        }
    }
}
