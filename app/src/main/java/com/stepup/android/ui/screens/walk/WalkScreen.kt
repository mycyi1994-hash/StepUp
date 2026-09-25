package com.stepup.android.ui.screens.walk

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import com.stepup.android.ui.components.AdaptiveNumber
import com.stepup.android.ui.components.DialogPanel
import com.stepup.android.ui.components.FormField
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import com.stepup.android.ui.experience.feedbackClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
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
import androidx.compose.material.icons.automirrored.filled.DirectionsRun
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Flag
import androidx.compose.material.icons.filled.GpsFixed
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material.icons.filled.Route
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
import androidx.compose.runtime.LaunchedEffect
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.stepup.android.BuildConfig
import com.stepup.android.R
import com.stepup.android.service.WalkSessionState
import com.stepup.android.service.RunSaveStatus
import androidx.compose.foundation.layout.heightIn
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Pause
import com.stepup.android.data.local.UploadState
import com.stepup.android.ui.components.RunShareCard
import androidx.compose.material.icons.filled.MoreHoriz
import androidx.compose.ui.platform.testTag
import com.stepup.android.domain.AvatarPose
import com.stepup.android.ui.components.CharacterStage
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
import com.stepup.android.ui.components.GhostButton
import com.stepup.android.ui.components.GlowCard
import com.stepup.android.ui.components.HairlineDivider
import com.stepup.android.ui.components.HexEmblem
import com.stepup.android.ui.components.NeonRing
import com.stepup.android.ui.components.VoltButton
import com.stepup.android.ui.components.breathing
import com.stepup.android.ui.components.quietClickable
import com.stepup.android.ui.theme.Alert
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

/** 랩 스냅샷(누적)을 구간값으로 변환한 것 */
private data class LapSegment(
    val index: Int,
    val km: Double,
    val sec: Long,
    val paceSec: Long,
)

@Composable
@OptIn(ExperimentalLayoutApi::class, androidx.compose.material3.ExperimentalMaterial3Api::class)
fun RunScreen(
    onBack: () -> Unit = {},
    onOpenCourses: () -> Unit = {},
    /** 러닝 홈의 "러닝 시작"에서 왔으면 곧바로 달리기를 시작한다 */
    autoStart: Boolean = false,
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
    val lastServerPoints by viewModel.lastServerPoints.collectAsStateWithLifecycle()
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

    var permissionDenied by rememberSaveable { mutableStateOf(false) }
    var locationAllowed by remember { mutableStateOf(StepPermissions.hasLocation(context)) }
    androidx.lifecycle.compose.LifecycleResumeEffect(Unit) {
        locationAllowed = StepPermissions.hasLocation(context)
        if (StepPermissions.hasActivityRecognition(context)) permissionDenied = false
        onPauseOrDispose { }
    }
    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) {
        permissionDenied = !StepPermissions.hasActivityRecognition(context)
        locationAllowed = StepPermissions.hasLocation(context)
        if (!permissionDenied) {
            WalkSessionService.start(context)
        }
    }

    // 러닝 홈에서 "러닝 시작"을 눌렀으면 이 화면에서 한 번 더 누르게 하지 않는다.
    // 한 번만 — 화면을 돌리거나 돌아와도 다시 시작하지 않는다.
    var autoStartDone by rememberSaveable { mutableStateOf(false) }
    LaunchedEffect(autoStart) {
        if (autoStart && !autoStartDone) {
            autoStartDone = true
            if (!WalkSessionService.state.value.isActive) {
                viewModel.clearReward()
                val missing = StepPermissions.missing(context)
                if (missing.isEmpty()) WalkSessionService.start(context) else permissionLauncher.launch(missing)
            }
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


    val goalCard: @Composable () -> Unit = {
        GlowCard(contentPadding = PaddingValues(20.dp), spacing = 16.dp) {
            Text(stringResource(R.string.run_goal_dialog_title), style = MaterialTheme.typography.titleMedium, color = Snow)
            FinishStat(stringResource(R.string.stat_distance), "%.2f".format(distanceKm), "km")
            BarMeter(fraction = if (goalKm > 0) (distanceKm / goalKm).toFloat().coerceIn(0f, 1f) else 0f, height = 7.dp)
            FinishStat(stringResource(R.string.run_remaining), "%.2f".format(max(goalKm - distanceKm, 0.0)), "km")
            Text(
                text = stringResource(R.string.run_eta) + " · " + if (distanceKm >= 0.05 && session.elapsedSec > 0) {
                    formatDuration((session.elapsedSec / distanceKm * goalKm).toLong())
                } else "—",
                style = MaterialTheme.typography.bodyMedium, color = Silver,
            )
            GhostButton(
                text = stringResource(R.string.run_goal_dialog_title) + " · %.1f km".format(goalKm),
                onClick = { showGoalDialog = true }, modifier = Modifier.fillMaxWidth(),
            )
        }
    }
    var showEstimateNote by rememberSaveable { mutableStateOf(false) }
    val estimateCard: @Composable () -> Unit = {
        GlowCard(contentPadding = PaddingValues(20.dp), spacing = 12.dp) {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                HexEmblem(size = 28.dp, glow = false)
                Text(stringResource(R.string.run_estimated_points), style = MaterialTheme.typography.titleMedium, color = Snow, modifier = Modifier.weight(1f))
                DarkIconButton(Icons.Filled.Info, stringResource(R.string.run_estimated_note), onClick = { showEstimateNote = !showEstimateNote })
            }
            AdaptiveNumber("+%,.2f".format(estimate.points), 32.sp, color = com.stepup.android.ui.theme.VoltText)
            Text("SUP", style = MaterialTheme.typography.bodyMedium, color = Silver)
            if (showEstimateNote) {
                Text(stringResource(R.string.run_estimated_note), style = MaterialTheme.typography.bodyMedium, color = Silver)
            }
            if (earnableSteps <= 0) {
                Text(stringResource(R.string.home_energy_empty), style = MaterialTheme.typography.bodyMedium, color = Alert)
            }
            if (xpBoosted) {
                Text(stringResource(R.string.run_boost_active), style = MaterialTheme.typography.bodyMedium, color = com.stepup.android.ui.theme.VoltText)
            }
            if (session.partySize > 1) {
                Text(stringResource(R.string.crew_boost, RewardEconomy.partyBonusPercent(session.partySize)), style = MaterialTheme.typography.bodyMedium, color = com.stepup.android.ui.theme.VoltText)
            }
        }
    }
    val finishing = !session.isActive && session.lastRewardPoints != null

    var showDetails by rememberSaveable { mutableStateOf(false) }
    Box(Modifier.fillMaxSize()) {
        Column(
            Modifier.fillMaxSize().padding(horizontal = com.stepup.android.ui.theme.StepUpDesign.Gutter),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            com.stepup.android.ui.components.FocusHeader(
                title = stringResource(when {
                    finishing -> R.string.finish_title
                    session.saveStatus == RunSaveStatus.SAVING -> R.string.run_saving
                    session.saveStatus == RunSaveStatus.FAILED -> R.string.run_save_retry
                    session.isPaused -> R.string.run_paused
                    session.isActive -> R.string.run_active
                    else -> R.string.run_ready
                }),
                onBack = onBack,
                action = {
                    DarkIconButton(
                        Icons.Filled.MoreHoriz, stringResource(R.string.common_more),
                        onClick = { showDetails = true },
                    )
                },
            )
            if (finishing) {
                LazyColumn(
                    Modifier.weight(1f).fillMaxWidth(),
                    contentPadding = PaddingValues(bottom = 24.dp),
                ) {
                    item {
                        FinishCard(
                            // 금액은 서버가 확인한 값만 — 확인 전(또는 금액을 아직 못 읽었으면) "—"
                            session = session, points = lastServerPoints,
                            upload = lastUpload, look = look, balance = balance,
                        )
                    }
                }
                PrimaryCta(
                    text = stringResource(R.string.finish_done),
                    icon = Icons.Filled.Check,
                    showArrow = false,
                    modifier = Modifier.testTag("run-result-done").padding(vertical = 12.dp),
                    onClick = { viewModel.clearReward(); onBack() },
                )
            } else {
                Column(
                    Modifier.weight(1f).fillMaxWidth().verticalScroll(rememberScrollState()),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    if (permissionDenied && !session.isActive) {
                        Text(stringResource(R.string.perm_body), color = Silver,
                            style = MaterialTheme.typography.bodyMedium, textAlign = TextAlign.Center)
                        TextButton(onClick = {
                            ExternalIntents.openAppSettings(context)
                        }) { Text(stringResource(R.string.cd_open_settings)) }
                    }
                    if (recordingCourse && !readyToSaveCourse) {
                        CourseRecordingStrip(running = session.isActive, onCancel = viewModel::cancelRecording)
                    }
                    RunHero(
                        paused = session.isPaused, gpsFix = session.gpsFix, locationAllowed = locationAllowed,
                        elapsedSec = session.elapsedSec, distanceKm = distanceKm, avgPaceSec = avgPaceSec,
                    )
                    Spacer(Modifier.height(20.dp))
                    val mapHeight = if (androidx.compose.ui.platform.LocalConfiguration.current.screenHeightDp < 800) 256.dp else 280.dp
                    val mapModifier = Modifier.fillMaxWidth().height(mapHeight)
                        .clip(RoundedCornerShape(20.dp)).testTag("run-live-map")
                    if (session.geoTrack.isNotEmpty()) {
                        LiveRouteMap(points = session.geoTrack, modifier = mapModifier, progress = 1f)
                    } else {
                        MapWaiting(mapModifier)
                    }
                    if (session.flaggedSegments > 0) {
                        TextButton(onClick = { showDetails = true }) {
                            Icon(Icons.Filled.Warning, null, tint = Alert, modifier = Modifier.size(18.dp))
                            Spacer(Modifier.width(8.dp))
                            Text(stringResource(
                                if (session.liveVerdict == RunVerdict.VOID) R.string.run_void_title else R.string.run_flagged_title,
                            ), color = Alert)
                        }
                    } else {
                        Spacer(Modifier.height(16.dp))
                    }
                }
                PrimaryCta(
                    text = stringResource(when {
                        session.saveStatus == RunSaveStatus.SAVING -> R.string.run_saving
                        session.saveStatus == RunSaveStatus.FAILED -> R.string.run_save_retry
                        !session.isActive -> R.string.home_start_run
                        session.isPaused -> R.string.cd_resume
                        else -> R.string.cd_pause
                    }),
                    icon = if (session.saveStatus != RunSaveStatus.IDLE) Icons.Filled.Check
                        else if (running) Icons.Filled.Pause else Icons.Filled.PlayArrow,
                    enabled = session.saveStatus != RunSaveStatus.SAVING,
                    onClick = {
                        when {
                            session.saveStatus == RunSaveStatus.FAILED -> WalkSessionService.stop(context)
                            running -> WalkSessionService.pause(context)
                            session.isActive -> WalkSessionService.resume(context)
                            else -> {
                                val missing = StepPermissions.missing(context)
                                if (missing.isEmpty()) WalkSessionService.start(context)
                                else permissionLauncher.launch(missing)
                            }
                        }
                    },
                    modifier = Modifier.testTag("run-primary-action"),
                )
                if (session.saveStatus == RunSaveStatus.FAILED) {
                    Text(stringResource(R.string.run_save_failed), color = Alert,
                        style = MaterialTheme.typography.bodyMedium, textAlign = TextAlign.Center,
                        modifier = Modifier.padding(vertical = 12.dp).testTag("run-save-error"))
                } else if (session.isActive && session.saveStatus == RunSaveStatus.IDLE) {
                    TextButton(
                        onClick = { confirmStop = true },
                        modifier = Modifier.heightIn(min = com.stepup.android.ui.theme.StepUpDesign.TouchTarget)
                            .testTag("run-finish"),
                    ) { Text(stringResource(R.string.run_finish), color = Silver) }
                } else {
                    Spacer(Modifier.height(16.dp))
                }
            }
        }

    }

    if (showDetails) {
        androidx.compose.material3.ModalBottomSheet(
            onDismissRequest = { showDetails = false }, containerColor = Night,
        ) {
            LazyColumn(
                Modifier.fillMaxWidth(),
                contentPadding = PaddingValues(horizontal = 20.dp, vertical = 16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                item { goalCard() }
                item { estimateCard() }
                item {
                    CourseChallengeCard(
                        course = course, sessionKm = distanceKm, liveTrack = session.geoTrack,
                        onOpenCourses = { showDetails = false; onOpenCourses() }, running = session.isActive,
                    )
                }
                if (session.flaggedSegments > 0) {
                    item {
                        Text(
                            if (session.liveVerdict == RunVerdict.VOID) stringResource(R.string.run_void_body)
                            else stringResource(R.string.run_flagged_body, session.flaggedSegments),
                            color = Silver,
                        )
                    }
                }
                if (BuildConfig.DEBUG) {
                    item {
                        TextButton(onClick = { viewModel.simulateSteps(100) }) {
                            Text(stringResource(R.string.run_simulate), color = Slate)
                        }
                    }
                }
            }
        }
    }

    if (confirmStop) {
        DialogPanel(
            title = stringResource(R.string.run_stop_confirm_title),
            onDismiss = { confirmStop = false },
            actions = {
                VoltButton(stringResource(R.string.run_stop_confirm_yes), onClick = {
                    confirmStop = false
                    WalkSessionService.stop(context)
                }, modifier = Modifier.fillMaxWidth())
                GhostButton(stringResource(R.string.run_stop_confirm_no), onClick = { confirmStop = false }, modifier = Modifier.fillMaxWidth())
            },
        ) {
            Text(stringResource(R.string.run_stop_confirm_body), style = MaterialTheme.typography.bodyLarge, color = Silver)
        }
    }

    if (readyToSaveCourse) {
        SaveCourseDialog(
            track = recordedTrack,
            onSave = viewModel::saveRecordedCourse,
            onDismiss = viewModel::cancelRecording,
        )
    }

    if (showGoalDialog) {
        DialogPanel(
            title = stringResource(R.string.run_goal_dialog_title),
            onDismiss = { showGoalDialog = false },
            actions = {
                VoltButton(stringResource(R.string.common_ok), onClick = { showGoalDialog = false }, modifier = Modifier.fillMaxWidth())
            },
        ) {
            AdaptiveNumber("%.1f".format(goalKm), 44.sp, textAlign = TextAlign.Center)
            Text("km", style = MaterialTheme.typography.bodyMedium, color = Silver, modifier = Modifier.fillMaxWidth(), textAlign = TextAlign.Center)
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
                StepperButton(text = "−") { viewModel.setGoalKm(if (goalKm > 42.0) 42.0 else goalKm - 0.5) }
                StepperButton(text = "+") { viewModel.setGoalKm(if (goalKm >= 42.0) 42.2 else goalKm + 0.5) }
            }
        }
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
            .size(56.dp)
            .clip(CircleShape)
            .background(CarbonHigh, CircleShape)
            .border(1.dp, Edge, CircleShape)
            .feedbackClickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = text,
            fontSize = 24.sp,
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
 * 코스를 고르지 않았으면 실제 현재 위치 또는 위치 대기 상태를 보여준다.
 */
@Composable
private fun CourseChallengeCard(
    course: RunCourse?,
    sessionKm: Double,
    liveTrack: List<GeoPoint>,
    onOpenCourses: () -> Unit,
    /** 달리는 중이면 지도와 완주 진행만 — 코스 고르기는 러닝 전에 한다 */
    running: Boolean = false,
) {
    val routePoints = when {
        liveTrack.size >= 2 -> liveTrack
        course != null && course.hasTrack -> course.points
        else -> emptyList()
    }
    val here = rememberCurrentLocation(enabled = routePoints.isEmpty())
    val mapPoints = when {
        routePoints.isNotEmpty() -> routePoints
        here != null -> listOf(here)
        else -> emptyList()
    }
    val first = mapPoints.firstOrNull()
    val seed = remember(course?.id, first?.lat?.toInt(), first?.lng?.toInt()) {
        course?.id?.toInt() ?: first?.let { (it.lat * 1e4).toInt() xor (it.lng * 1e4).toInt() } ?: 0
    }
    val progress = if (course != null && course.distanceKm > 0) (sessionKm / course.distanceKm).toFloat().coerceIn(0f, 1f) else 0f
    val context = LocalContext.current
    GlowCard(contentPadding = PaddingValues(20.dp), spacing = 16.dp) {
        Text(course?.name ?: stringResource(R.string.course_none_title), style = MaterialTheme.typography.titleMedium, color = Snow)
        Box(Modifier.fillMaxWidth().height(220.dp).clip(RoundedCornerShape(16.dp))) {
            if (mapPoints.isNotEmpty()) {
                LiveRouteMap(
                    interactive = true, points = mapPoints, seed = seed,
                    progress = progress.takeIf { course != null && sessionKm > 0.005 },
                    modifier = Modifier.fillMaxSize(),
                )
            } else MapWaiting(Modifier.fillMaxSize())
        }
        if (course != null) {
            FinishStat(stringResource(R.string.course_to_finish), "%.2f".format((course.distanceKm - sessionKm).coerceAtLeast(0.0)), "km")
            BarMeter(fraction = progress, height = 7.dp)
            // 서버가 주는 코스만 금액을 보인다(최대치) — 체험 · 내 코스는 "보상 없음"
            if (course.serverReward > 0) {
                Text(stringResource(R.string.course_reward_upto, "%.0f".format(course.serverReward)), style = MaterialTheme.typography.titleMedium, color = com.stepup.android.ui.theme.VoltText)
            } else {
                Text(stringResource(R.string.course_reward_none), style = MaterialTheme.typography.bodyMedium, color = Silver)
            }
            if (course.serverReward > 0) Text(stringResource(R.string.course_per_km, "%.0f".format(CourseRewards.SUP_PER_KM), "%.0f".format(CourseRewards.MAX_REWARD)), style = MaterialTheme.typography.bodyMedium, color = Silver)
        } else if (!running) {
            Text(stringResource(R.string.course_none_body), style = MaterialTheme.typography.bodyMedium, color = Silver)
        }
        if (!running) {
            VoltButton(stringResource(if (course != null) R.string.course_change else R.string.course_pick), onClick = onOpenCourses, modifier = Modifier.fillMaxWidth())
            if (mapPoints.size >= 2) {
                GhostButton(stringResource(R.string.map_open_google), onClick = { ExternalIntents.openRouteInMaps(context, mapPoints) }, modifier = Modifier.fillMaxWidth())
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
                fontSize = 14.sp,
                color = Silver,
                textAlign = TextAlign.Center,
                lineHeight = 20.sp,
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
                    fontSize = 14.sp,
                    color = Silver,
                    lineHeight = 20.sp,
                )
            }
        }
        GhostButton(stringResource(R.string.common_cancel), onClick = onCancel, modifier = Modifier.fillMaxWidth())
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

    DialogPanel(
        title = stringResource(R.string.course_save_title),
        onDismiss = onDismiss,
        actions = {
            VoltButton(stringResource(R.string.course_register), onClick = { onSave(name, area, share) }, enabled = name.isNotBlank(), modifier = Modifier.fillMaxWidth())
            GhostButton(stringResource(R.string.course_save_skip), onClick = onDismiss, modifier = Modifier.fillMaxWidth())
        },
    ) {
        LiveRouteMap(points = track, seed = track.size, modifier = Modifier.fillMaxWidth().height(160.dp).clip(RoundedCornerShape(16.dp)))
        Text(stringResource(R.string.course_save_body, "%.2f".format(km)), style = MaterialTheme.typography.titleMedium, color = Snow)
        FormField(label = stringResource(R.string.course_name_hint), value = name, onValueChange = { name = it })
        FormField(label = stringResource(R.string.course_area_hint), value = area, onValueChange = { area = it })
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Text(stringResource(R.string.course_share_toggle), style = MaterialTheme.typography.bodyLarge, color = Snow, modifier = Modifier.weight(1f))
            Switch(checked = share, onCheckedChange = { share = it }, colors = SwitchDefaults.colors(checkedThumbColor = OnVolt, checkedTrackColor = Volt, uncheckedThumbColor = Silver, uncheckedTrackColor = CarbonHigh))
        }
    }
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
    /** 서버가 확인한 적립액. null 이면 아직 확인되지 않았다 */
    points: Double?,
    upload: String?,
    look: com.stepup.android.domain.AvatarLook?,
    balance: Double?,
) {
    val context = LocalContext.current
    val voided = session.lastVerdict == RunVerdict.VOID
    // 서버가 확인했고 그 금액까지 읽었을 때만 확정으로 보인다 — 따로 도는 두 흐름이 잠깐 어긋나도 "+0" 을 보이지 않게
    // 금액이 0 이면(서버가 무효 · 상한 처리) 확인은 됐어도 "적립 완료"가 아니다 — 축하도 하지 않는다
    val confirmed = !voided && upload == UploadState.SIGNED.name && points != null && points > 0.0
    // 걸음이 0 인 러닝은 서버에 올리지 않는다(올릴 것이 없다) — "서버 확인 중"으로 영영 두지 않고 적립 없음으로
    val nothingToUpload = !voided && session.lastSessionSteps <= 0
    val noReward = nothingToUpload ||
        (!voided && upload == UploadState.SIGNED.name && points != null && points <= 0.0)
    val rejected = voided || upload == UploadState.REJECTED.name || noReward
    val km = if (session.lastGpsKm > 0.0) session.lastGpsKm else RewardEconomy.distanceMeters(session.lastSessionSteps) / 1000
    val paceSec: Long? = if (km >= 0.05 && session.lastElapsedSec > 0) (session.lastElapsedSec / km).toLong() else null
    // 서버가 확인한 뒤에만 "적립 완료". 그 전에는 확인 중이라고 적는다.
    val headline = when {
        voided -> R.string.run_void_title
        confirmed -> R.string.finish_confirmed
        nothingToUpload -> R.string.finish_no_steps
        noReward -> R.string.finish_no_reward
        upload == UploadState.REJECTED.name -> R.string.finish_rejected
        else -> R.string.finish_pending_short
    }
    val shareLabels = RunShareCard.Labels(
        distance = stringResource(R.string.share_card_distance),
        time = stringResource(R.string.share_card_time),
        pace = stringResource(R.string.share_card_pace),
        footer = stringResource(R.string.share_card_footer),
    )
    val shareText = if (confirmed && points != null) {
        stringResource(R.string.finish_share_text, "%.1f".format(km),
            formatDuration(session.lastElapsedSec), "%,.0f".format(points))
    } else {
        stringResource(R.string.finish_share_activity, "%.1f".format(km), formatDuration(session.lastElapsedSec))
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .reveal(session.lastStartedAt)
            .celebrate(if (confirmed) session.lastStartedAt else null),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Icon(Icons.Filled.Check, contentDescription = null, tint = com.stepup.android.ui.theme.VoltText,
            modifier = Modifier.size(48.dp).padding(8.dp))
        Text(stringResource(R.string.finish_title), color = Snow, style = MaterialTheme.typography.headlineSmall)
        // Keep the three activity results together in the first viewport. Stack
        // only when a narrow screen or enlarged type needs the full line width.
        BoxWithConstraints(Modifier.fillMaxWidth()) {
            val stacked = maxWidth < 320.dp || LocalDensity.current.fontScale > 1.25f
            GlowCard(contentPadding = PaddingValues(16.dp), spacing = 12.dp) {
                if (stacked) {
                    FinishStat(stringResource(R.string.stat_distance), "%.2f".format(km), "km")
                    HairlineDivider()
                    FinishStat(stringResource(R.string.home_run_time), formatDuration(session.lastElapsedSec), "")
                    HairlineDivider()
                    FinishStat(stringResource(R.string.run_avg_pace), paceSec?.let { formatPace(it) } ?: "—", "")
                } else {
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        FinishStat(stringResource(R.string.stat_distance), "%.2f".format(km), "km", Modifier.weight(1f))
                        FinishStat(stringResource(R.string.home_run_time), formatDuration(session.lastElapsedSec), "", Modifier.weight(1f))
                        FinishStat(stringResource(R.string.run_avg_pace), paceSec?.let { formatPace(it) } ?: "—", "", Modifier.weight(1f))
                    }
                }
            }
        }
        if (session.geoTrack.isNotEmpty()) {
            LiveRouteMap(points = session.geoTrack, modifier = Modifier.fillMaxWidth().height(220.dp)
                .clip(RoundedCornerShape(20.dp)).testTag("run-result-map"))
        } else {
            Text(stringResource(R.string.run_route_unavailable), color = Silver,
                style = MaterialTheme.typography.bodyMedium, modifier = Modifier.padding(vertical = 12.dp))
        }
        GlowCard(contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp)) {
          Column(
              modifier = Modifier.fillMaxWidth(),
              horizontalAlignment = Alignment.CenterHorizontally,
              verticalArrangement = Arrangement.spacedBy(6.dp),
          ) {
            Text(
                text = stringResource(headline),
                fontSize = 15.sp,
                fontWeight = FontWeight.Bold,
                color = if (voided || upload == UploadState.REJECTED.name) Alert else Silver,
                textAlign = TextAlign.Center,
            )
            AdaptiveNumber(
                text = when {
                    confirmed && points != null -> "+%,.0f".format(points)
                    rejected -> "0"
                    else -> "—"
                },
                fontSize = 32.sp, modifier = Modifier.testTag("run-result-reward"),
                color = com.stepup.android.ui.theme.VoltText, textAlign = TextAlign.Center,
            )
            Text("SUP", style = MaterialTheme.typography.bodyMedium, color = Silver)
            if (!voided && !confirmed && upload != UploadState.REJECTED.name) {
                Text(
                    text = stringResource(R.string.finish_pending_note),
                    fontSize = 14.sp,
                    color = Silver,
                    textAlign = TextAlign.Center,
                    lineHeight = 20.sp,
                )
            }
            if (voided) {
                Text(
                    text = stringResource(R.string.run_void_body),
                    style = MaterialTheme.typography.bodyMedium,
                    color = Silver,
                    textAlign = TextAlign.Center,
                )
            }
        }

        }
        GlowCard(contentPadding = PaddingValues(20.dp), spacing = 12.dp) {
            Text(stringResource(R.string.finish_balance), style = MaterialTheme.typography.bodyMedium, color = Silver)
            AdaptiveNumber(balance?.let { com.stepup.android.ui.components.formatSupDown(it) } ?: "—", 28.sp)
            Text("SUP", style = MaterialTheme.typography.bodyMedium, color = Silver)
        }

        GhostButton(
            text = stringResource(R.string.finish_share),
            onClick = {
                // 달린 길과 기록을 그림 한 장으로. 그림을 못 만들면 글만 보낸다.
                val card = runCatching {
                    RunShareCard.render(
                        context = context,
                        km = km,
                        elapsed = formatDuration(session.lastElapsedSec),
                        pace = paceSec?.let { "%d'%02d\"".format(it / 60, it % 60) } ?: "—",
                        track = session.geoTrack,
                        labels = shareLabels,
                    )
                }.getOrNull()
                if (card != null) {
                    RunShareCard.share(context, card, shareText, null)
                } else {
                    val send = android.content.Intent(android.content.Intent.ACTION_SEND).apply {
                        type = "text/plain"
                        putExtra(android.content.Intent.EXTRA_TEXT, shareText)
                    }
                    context.startActivity(android.content.Intent.createChooser(send, null))
                }
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
 * 시계는 시스템 글자 크기를 따르되, 전체 시간이 한 줄에 들어오도록 폭에 맞춘다.
 */
@Composable
private fun RunHero(
    paused: Boolean,
    gpsFix: Boolean,
    locationAllowed: Boolean,
    elapsedSec: Long,
    distanceKm: Double,
    avgPaceSec: Long?,
) {
    BoxWithConstraints(Modifier.fillMaxWidth()) {
        val stacked = maxWidth < 300.dp || LocalDensity.current.fontScale > 1.25f
        Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(12.dp)) {
            GpsChip(gpsFix, locationAllowed)
            AdaptiveNumber(formatDuration(elapsedSec), if (elapsedSec >= 3600) 44.sp else 64.sp, color = if (paused) Silver else Snow, textAlign = TextAlign.Center)
            if (stacked) {
                FinishStat(stringResource(R.string.stat_distance), "%.2f".format(distanceKm), "km")
                FinishStat(stringResource(R.string.run_avg_pace), avgPaceSec?.let { formatPace(it) } ?: "—", "")
            } else {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(20.dp)) {
                    FinishStat(stringResource(R.string.stat_distance), "%.2f".format(distanceKm), "km", Modifier.weight(1f))
                    FinishStat(stringResource(R.string.run_avg_pace), avgPaceSec?.let { formatPace(it) } ?: "—", "", Modifier.weight(1f))
                }
            }
        }
    }
}

/** GPS 상태 알약 — 잡혔으면 시안, 찾는 중이면 흐리게 */
@Composable
private fun GpsChip(fix: Boolean, allowed: Boolean = true) {
    val color = if (fix) com.stepup.android.ui.theme.Cyan else Slate
    Row(
        modifier = Modifier
            .clip(RoundedCornerShape(50))
            .background(color.copy(alpha = 0.10f))
            .border(1.dp, color.copy(alpha = 0.7f), RoundedCornerShape(50))
            .padding(horizontal = 12.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        Icon(Icons.Filled.GpsFixed, contentDescription = null, tint = color, modifier = Modifier.size(18.dp))
        Text(
            text = stringResource(when {
                !allowed -> R.string.run_location_disabled
                fix -> R.string.run_gps_ok
                else -> R.string.run_gps_search
            }),
            fontSize = 14.sp,
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
    Column(modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Text(label, style = MaterialTheme.typography.bodyMedium, color = Silver)
        AdaptiveNumber(value, 26.sp)
        if (unit.isNotEmpty()) Text(unit, style = MaterialTheme.typography.bodyMedium, color = Silver)
    }
}
