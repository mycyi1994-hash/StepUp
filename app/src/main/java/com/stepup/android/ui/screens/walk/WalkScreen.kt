package com.stepup.android.ui.screens.walk

import com.stepup.android.domain.DietRoutine
import com.stepup.android.domain.DietSegment
import com.stepup.android.domain.DietSegmentKind
import com.stepup.android.domain.GoalAttempt
import com.stepup.android.domain.RunPlan
import com.stepup.android.domain.RunPlans
import com.stepup.android.ui.components.KitButton
import com.stepup.android.ui.components.KitDialog
import com.stepup.android.ui.components.KitNotice
import com.stepup.android.ui.components.KitTone
import com.stepup.android.ui.components.RunKit
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableLongStateOf
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
import androidx.compose.ui.draw.clipToBounds
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
import androidx.compose.ui.layout.boundsInRoot
import androidx.compose.ui.layout.layout
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInRoot
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material.icons.filled.Share
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
import com.stepup.android.ui.components.followAnchor
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
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import androidx.compose.material.icons.outlined.FileDownload
import androidx.compose.material.icons.outlined.IosShare
import com.stepup.android.ui.components.KitCenterHeader

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
    /** 결과의 "처음 화면으로" — 러닝 탭 첫 화면(홈) */
    onHome: () -> Unit = onBack,
    /** 챌린지 결과의 "챌린지로 돌아가기" — 러닝 챌린지 목록 */
    onGoals: () -> Unit = onBack,
    /** 다이어트 결과의 "같은 방법 다시 하기" */
    onRepeat: (RunPlan) -> Unit = {},
    /** 러닝 홈의 "러닝 시작"에서 왔으면 곧바로 달리기를 시작한다 */
    autoStart: Boolean = false,
    /** 권한 안내의 "홈으로 돌아가기"(시작·로그인·첫 사용 v1 시안 14 · 15) — 러닝 탭 첫 화면 */
    onLeaveToHome: () -> Unit = onHome,
    viewModel: WalkViewModel = viewModel(factory = WalkViewModel.Factory),
) {
    // 러닝 서비스는 끝난 러닝을 백그라운드 스레드에서 내놓는다. 화면은 그 값을 메인 스레드에서 받는다 — 기기 테스트의
    // 즉시 실행 환경에서 결과 목록(LazyColumn)을 백그라운드 스레드에서 처음 그리다 멈췄다(Looper 없음, QA 147)
    val session by viewModel.session.collectAsStateWithLifecycle(context = Dispatchers.Main.immediate)
    val energy by viewModel.energy.collectAsStateWithLifecycle()
    val sneakerLevel by viewModel.sneakerLevel.collectAsStateWithLifecycle()
    val equipped by viewModel.equipped.collectAsStateWithLifecycle()
    val xpBoosted by viewModel.xpBoosted.collectAsStateWithLifecycle()
    val balance by viewModel.balance.collectAsStateWithLifecycle()
    val laps by viewModel.laps.collectAsStateWithLifecycle()
    val course by viewModel.selectedCourse.collectAsStateWithLifecycle()
    val lastUpload by viewModel.lastUpload.collectAsStateWithLifecycle()
    val lastServerPoints by viewModel.lastServerPoints.collectAsStateWithLifecycle()
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

    var countingDown by rememberSaveable { mutableStateOf(false) }
    // 한 번 고른 러닝 — 권한 안내(시작·로그인·첫 사용 v1 시안 13~19)를 활동 → 위치 → 알림 차례로 지나 3-2-1(R01)로 간다.
    // 안내를 닫으면 아무것도 시작하지 않는다. 필요한 권한이 이미 있으면 안내 없이 바로 3-2-1 이다.
    var startPending by rememberSaveable { mutableStateOf(false) }
    var locationAllowed by remember { mutableStateOf(StepPermissions.hasLocation(context)) }
    androidx.lifecycle.compose.LifecycleResumeEffect(Unit) {
        locationAllowed = StepPermissions.hasLocation(context)
        onPauseOrDispose { }
    }
    val requestStart = { if (!startPending && !countingDown) startPending = true }

    // 러닝 홈에서 "러닝 시작"을 눌렀으면 이 화면에서 한 번 더 누르게 하지 않는다.
    // 한 번만 — 화면을 돌리거나 돌아와도 다시 시작하지 않는다.
    var autoStartDone by rememberSaveable { mutableStateOf(false) }
    LaunchedEffect(autoStart) {
        if (autoStart && !autoStartDone) {
            autoStartDone = true
            if (!WalkSessionService.state.value.isActive) {
                viewModel.clearReward()
                requestStart()
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

    // 시작 메뉴 · 러닝 챌린지에서 고른 계획(시안 U01 · U02) — 처음 만난 러닝 하나에 묶는다
    val planNow by RunPlans.current.collectAsStateWithLifecycle()
    LaunchedEffect(session.isActive, session.startedAt) {
        if (session.isActive) RunPlans.bind(session.startedAt)
    }
    val plan: RunPlan = when {
        session.isActive -> remember(session.startedAt, planNow) { RunPlans.planFor(session.startedAt) }
        finishing -> remember(session.lastStartedAt, planNow) { RunPlans.planFor(session.lastStartedAt) }
        else -> planNow
    }
    val goal = (plan as? RunPlan.Goal)?.goal
    // 목표 거리는 결과(finishKm)와 같은 기준 — GPS 가 있으면 GPS 거리, 없으면 걸음 거리
    val goalKmNow = if (session.gpsKm > 0.0) session.gpsKm else distanceKm
    val goalReached = goal != null && goal.reached(session.elapsedSec, goalKmNow)
    // 다이어트 모드(시안 D07~D13) — 구간은 운동 시간에서 계산한다
    val diet = (plan as? RunPlan.Diet)?.let { DietRoutine.forExperience(it.experience) }
    val dietPos = diet?.at(session.elapsedSec)
    val dietDone = diet != null && session.isActive && diet.finished(session.elapsedSec)
    val cues = com.stepup.android.ui.experience.LocalFeedback.current
    LaunchedEffect(dietPos?.index) {
        // 구간이 바뀌면 소리로도 알린다 — 화면을 보지 않고 달려도 알 수 있게
        if (dietPos != null && dietPos.index > 0 && running) cues?.play(com.stepup.android.ui.experience.FeedbackCue.Lap)
    }
    var dietDoneFor by rememberSaveable { mutableLongStateOf(0L) }
    LaunchedEffect(dietDone, session.startedAt) {
        // 루틴을 다 마치면 한 번 멈추고 완료 화면(D10)을 보인다 — 저장은 사용자가 누른다
        if (dietDone && dietDoneFor != session.startedAt) {
            dietDoneFor = session.startedAt
            if (!session.isPaused) WalkSessionService.pause(context)
            cues?.play(com.stepup.android.ui.experience.FeedbackCue.Finish)
        }
    }

    // 확인 창들 — 일시정지(R03) · 종료(R04) · 저장 없이 끝내기(R07) · 목표 달성(C01)
    var pauseDialog by rememberSaveable { mutableStateOf(false) }
    var discardDialog by rememberSaveable { mutableStateOf(false) }
    // 저장 없이 끝내기를 서비스가 마칠 때까지 기다린다 — 못 지웠으면 러닝이 그대로 이어진다
    var discarding by rememberSaveable { mutableStateOf(false) }
    LaunchedEffect(discarding, session.isActive) {
        if (discarding && !session.isActive) {
            discarding = false
            RunPlans.clear()
            onBack()
        }
    }
    val discardFailures by WalkSessionService.discardFailures.collectAsStateWithLifecycle()
    val discardFailedText = stringResource(R.string.runflow_discard_failed)
    LaunchedEffect(discardFailures) {
        if (discarding) {
            discarding = false
            android.widget.Toast.makeText(context, discardFailedText, android.widget.Toast.LENGTH_LONG).show()
        }
    }
    var goalSheetFor by rememberSaveable { mutableLongStateOf(0L) }
    var showGoalReached by rememberSaveable { mutableStateOf(false) }
    LaunchedEffect(goalReached, session.startedAt, session.isPaused) {
        if (goalReached && running && goalSheetFor != session.startedAt) {
            goalSheetFor = session.startedAt
            showGoalReached = true
        }
    }
    val canAskEnd = session.isActive && session.saveStatus == RunSaveStatus.IDLE
    // 종료를 누르면 먼저 멈추고 묻는다 — "계속 달리기"를 눌렀을 때만 다시 달린다
    val askEnd = {
        if (canAskEnd) {
            if (!session.isPaused) WalkSessionService.pause(context)
            pauseDialog = false
            showGoalReached = false
            confirmStop = true
        }
    }
    // 달리는 중 뒤로 가기는 화면을 닫지 않고 종료를 묻는다(시안 동작 기준)
    androidx.activity.compose.BackHandler(enabled = canAskEnd && !confirmStop && !discardDialog) { askEnd() }

    // 저장한 챌린지 러닝은 지난 도전에 남긴다(시안 C03) — 같은 러닝은 한 줄
    LaunchedEffect(finishing, session.lastStartedAt, goal) {
        if (finishing && goal != null && session.lastStartedAt > 0) {
            val km = finishKm(session)
            com.stepup.android.core.ServiceLocator.userPrefs.addGoalAttempt(
                GoalAttempt(session.lastStartedAt, goal, goal.reached(session.lastElapsedSec, km), session.lastElapsedSec, km),
            )
        }
    }

    var showDetails by rememberSaveable { mutableStateOf(false) }
    val palette = runPalette()
    Box(Modifier.fillMaxSize().background(palette.screen)) {
        Column(Modifier.fillMaxSize()) {
            val goalMissed = finishing && goal != null && !goal.reached(session.lastElapsedSec, finishKm(session))
            val dietFinished = finishing && diet != null && diet.finished(session.lastElapsedSec)
            val dietPartial = finishing && diet != null && !dietFinished
            val done = {
                viewModel.clearReward()
                if (goalMissed) onGoals() else onHome()
            }
            // 전달본 01 · 03 — 가운데 이름 한 줄. 달리는 중에는 뒤로 대신 시스템 뒤로 가기가 종료를 묻는다
            KitCenterHeader(
                title = when {
                    dietFinished -> stringResource(R.string.diet_saved_title)
                    finishing -> stringResource(if (goalMissed || dietPartial) R.string.runflow_result_partial_title else R.string.finish_title)
                    else -> planTitle(plan)
                },
                onBack = if (!finishing && !session.isActive) onBack else null,
                action = if (finishing) {
                    { RunCloseButton(stringResource(R.string.common_close), done, Modifier.testTag("run-result-close")) }
                } else {
                    {
                        DarkIconButton(
                            Icons.Filled.MoreHoriz, stringResource(R.string.run_details),
                            onClick = { showDetails = true },
                        )
                    }
                },
            )
            if (finishing) {
                // 전달본 03 — 기록 카드 한 장(지도가 남는 높이를 채운다) + 이미지 저장 | 공유하기 + 완료
                val km = finishKm(session)
                val paceSec = finishPace(session)
                val reward = finishReward(session, lastServerPoints, lastUpload)
                val confirmed = finishConfirmed(session, lastServerPoints, lastUpload)
                val datePattern = stringResource(R.string.result_date_pattern)
                // 요일은 앱 언어로 — 앱 안에서 고른 언어가 기기 기본 언어와 다를 수 있다
                val locale = androidx.compose.ui.platform.LocalConfiguration.current.locales[0]
                val date = remember(session.lastStartedAt, datePattern, locale) {
                    if (session.lastStartedAt > 0L) {
                        runCatching {
                            java.text.SimpleDateFormat(datePattern, locale).format(java.util.Date(session.lastStartedAt))
                        }.getOrNull()
                    } else null
                }
                val note = when {
                    diet != null && dietFinished -> stringResource(R.string.diet_run_plus_walk) + " · " +
                        stringResource(R.string.diet_run_plus_walk_value, minutesText(diet.totalRunSec), minutesText(diet.totalWalkSec))
                    dietPartial -> stringResource(R.string.diet_partial_note)
                    goal == null -> null
                    goalMissed -> stringResource(R.string.runflow_result_goal_partial, goalName(goal), formatDuration(session.lastElapsedSec))
                    else -> stringResource(R.string.runflow_result_goal_done, goalName(goal), "%.2f".format(km))
                }
                BoxWithConstraints(Modifier.weight(1f).fillMaxWidth()) {
                    val density = LocalDensity.current
                    val chrome = with(density) { (ResultCardMargin * 2 + ResultCardPadding * 2 + 2.dp).roundToPx() }
                    val viewport = constraints.maxHeight - chrome
                    Column(
                        Modifier.fillMaxSize().verticalScroll(rememberScrollState()).testTag("run-result-scroll")
                            .padding(horizontal = ResultCardMargin, vertical = ResultCardMargin),
                    ) {
                        RunResultCard(
                            viewport = viewport,
                            date = date,
                            km = km,
                            note = note,
                            track = session.geoTrack,
                            time = formatDuration(session.lastElapsedSec),
                            paceSec = paceSec,
                            speedKmh = averageSpeedKmh(km, session.lastElapsedSec),
                            shoe = equipped,
                            reward = reward,
                            modifier = Modifier.reveal(session.lastStartedAt)
                                .celebrate(if (confirmed) session.lastStartedAt else null),
                            brand = { com.stepup.android.ui.components.ResultCardBrand() },
                        )
                    }
                }
                val share = rememberFinishShare(session, lastServerPoints, lastUpload)
                val saveImage = rememberFinishSave(session)
                Column(
                    Modifier.fillMaxWidth().padding(horizontal = ResultCardMargin).padding(bottom = 12.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        RunOutlineButton(stringResource(R.string.result_save_image), Icons.Outlined.FileDownload, saveImage,
                            Modifier.weight(1f).testTag("run-result-save"))
                        RunOutlineButton(stringResource(R.string.result_share), Icons.Outlined.IosShare, share,
                            Modifier.weight(1f).testTag("run-result-share"))
                    }
                    if (diet != null) {
                        KitButton(stringResource(R.string.diet_repeat), {
                            viewModel.clearReward()
                            onRepeat(plan)
                        }, tone = KitTone.Secondary, modifier = Modifier.testTag("run-result-repeat"))
                    }
                    RunPrimaryButton(
                        stringResource(if (goalMissed) R.string.runflow_history_back else R.string.finish_done), done,
                        Modifier.fillMaxWidth().testTag("run-result-done"), height = 56.dp,
                    )
                }
            } else {
                // 전달본 01 — 지도를 화면 가득, 위에 러닝 시간 | 달린 거리, 아래에 현재 속도 | 내구도와 일시정지 · 종료
                val gpsLost = running && session.gpsLost
                val now = remember(session.elapsedSec, session.track.size, session.lastFlaggedAt) { System.currentTimeMillis() }
                val speedKmh = if (running && !gpsLost) currentSpeedKmh(session.track, now, session.lastFlaggedAt) else null
                val speedError = running && session.lastFlaggedAt > (session.track.lastOrNull()?.at ?: 0L)
                val voidNow = session.isActive && session.liveVerdict == RunVerdict.VOID
                val flaggedNow = session.isActive && session.flaggedSegments > 0
                // 지금 자리를 둘 높이 — 지도 중 위 판과 아래 판 사이(보이는 쪽)의 가운데
                var mapTop by remember { mutableFloatStateOf(0f) }
                var mapHeight by remember { mutableFloatStateOf(0f) }
                var seenTop by remember { mutableFloatStateOf(0f) }
                var seenBottom by remember { mutableFloatStateOf(0f) }
                val followAt = if (mapHeight > 0f && seenBottom > seenTop) {
                    followAnchor((seenTop - mapTop) / mapHeight, (seenBottom - mapTop) / mapHeight, 0f, 1f)
                } else 0.5f
                // S2 같이 뛰는 중(시안 14) — 파티런이면 위치를 보이기로 한 사람을 지도에, 거리 순위를 위 판 아래에
                val party by com.stepup.android.core.ServiceLocator.crewRepository.party.collectAsStateWithLifecycle()
                val together = session.isActive && party.phase == com.stepup.android.data.repo.PartyPhase.RUNNING &&
                    party.members.size > 1
                val others = if (together) {
                    party.members.filter { !it.isMe }.mapNotNull { m -> m.point?.let { it to m.name } }
                } else emptyList()
                // 챌린지 상세에서 "이 챌린지 달리기"로 시작했으면 그 챌린지의 예상 진행(사용 피드백 9)
                val challenge by com.stepup.android.ui.screens.events.ChallengeRunFocus.current.collectAsStateWithLifecycle()
                LaunchedEffect(session.isActive, session.startedAt) {
                    if (session.isActive) com.stepup.android.ui.screens.events.ChallengeRunFocus.bind(session.startedAt)
                }
                val focus = challenge?.takeIf {
                    com.stepup.android.ui.screens.events.ChallengeRunFocus.matches(session.startedAt)
                }
                Box(Modifier.weight(1f).fillMaxWidth()) {
                    Box(
                        Modifier.fillMaxSize().clipToBounds()
                            .onGloballyPositioned { c ->
                                mapTop = c.positionInRoot().y
                                mapHeight = c.size.height.toFloat()
                            }
                            .testTag("run-live-map"),
                    ) {
                        val here = session.here
                        if (session.geoTrack.isNotEmpty()) {
                            LiveRouteMap(points = session.geoTrack, modifier = Modifier.fillMaxSize(), progress = 1f, others = others,
                                follow = true, followAt = followAt)
                        } else if (session.isActive && here != null) {
                            // GPS 가 잡히기 전 — 기지국 · 마지막으로 알던 위치로 "여기쯤"을 먼저 보인다(경로는 아직 없다)
                            LiveRouteMap(points = listOf(here), modifier = Modifier.fillMaxSize().testTag("run-rough-location"), others = others,
                                follow = true, followAt = followAt)
                        } else {
                            MapWaiting(Modifier.fillMaxSize())
                        }
                    }
                    // 머리 → 지도, 지도 → 버튼이 바닥색으로 부드럽게 이어진다
                    Box(Modifier.fillMaxWidth().height(20.dp).background(Brush.verticalGradient(listOf(palette.screen, Color.Transparent))))
                    Box(
                        Modifier.align(Alignment.BottomCenter).fillMaxWidth().height(110.dp)
                            .background(Brush.verticalGradient(listOf(Color.Transparent, palette.screen.copy(alpha = 0.92f)))),
                    )
                    Column(Modifier.fillMaxSize()) {
                        BoxWithConstraints(Modifier.weight(1f).fillMaxWidth()) {
                            RunOverlayLayout(
                                viewport = constraints.maxHeight,
                                minGap = 96.dp,
                                modifier = Modifier.fillMaxWidth().verticalScroll(rememberScrollState()).testTag("run-overlay-scroll"),
                                top = {
                                    Column(
                                        Modifier.fillMaxWidth().padding(top = 4.dp)
                                            .onGloballyPositioned { seenTop = it.boundsInRoot().bottom },
                                        verticalArrangement = Arrangement.spacedBy(8.dp),
                                    ) {
                                        if (voidNow || flaggedNow) {
                                            RunAlertBanner(
                                                stringResource(if (voidNow) R.string.run_void_banner else R.string.run_speed_error),
                                                void = voidNow, onClick = { showDetails = true },
                                                modifier = Modifier.padding(horizontal = 8.dp),
                                            )
                                        }
                                        Column(
                                            Modifier.fillMaxWidth().padding(horizontal = 16.dp),
                                            verticalArrangement = Arrangement.spacedBy(8.dp),
                                        ) {
                                            if (recordingCourse && !readyToSaveCourse) {
                                                CourseRecordingStrip(running = session.isActive, onCancel = viewModel::cancelRecording)
                                            }
                                            RunPlanPanel(
                                                diet = diet, dietPos = dietPos, dietDone = dietDone, goal = goal,
                                                goalReached = goalReached, elapsedSec = session.elapsedSec, km = goalKmNow,
                                                paused = session.isPaused,
                                            )
                                            // GPS 가 잡혀 있으면 말하지 않는다 — 찾는 중 · 대략적인 위치 · 권한 꺼짐만
                                            val gpsOk = session.gpsFix && !gpsLost && locationAllowed
                                            if (!gpsOk && !gpsLost) {
                                                RunOverlayCard(Modifier.testTag("run-gps-status")) {
                                                    GpsStatusLine(
                                                        gpsFix = session.gpsFix, locationAllowed = locationAllowed,
                                                        roughFix = session.isActive && session.here != null,
                                                    )
                                                }
                                            }
                                            // L04 — 위치 신호가 끊긴 것 같다. 시간 · 걸음은 계속 기록한다(안내만, 창을 띄우지 않는다)
                                            if (gpsLost) {
                                                KitNotice(
                                                    stringResource(R.string.runflow_gps_lost_title), stringResource(R.string.runflow_gps_lost_body),
                                                    modifier = Modifier.testTag("run-gps-lost"),
                                                )
                                            }
                                            // 위치가 안 잡히는 흔한 두 까닭 — 휴대폰 위치가 꺼졌거나, "대략적인 위치"만 허용했다.
                                            // 대략적인 위치만 허용하면 기지국 점이 들어와 "잡힘"으로 보여도 경로 · 거리가 뭉개진다 — 잡혔어도 보인다
                                            if (session.isActive && locationAllowed && (!session.gpsFix || !session.precise)) {
                                                when {
                                                    !session.locationOn && !session.gpsFix -> RunOverlayCard {
                                                        LocationHint(
                                                            stringResource(R.string.run_location_off_hint),
                                                            stringResource(R.string.run_location_off_action),
                                                            tag = "run-location-off",
                                                        ) { ExternalIntents.openLocationSettings(context) }
                                                    }
                                                    !session.precise -> RunOverlayCard {
                                                        LocationHint(
                                                            stringResource(R.string.run_location_approx_hint),
                                                            stringResource(R.string.cd_open_settings),
                                                            tag = "run-location-approx",
                                                        ) { ExternalIntents.openAppSettings(context) }
                                                    }
                                                }
                                            }
                                            if (together) {
                                                RunOverlayCard { TogetherRanking(party.members, myKm = distanceKm) }
                                            }
                                            if (focus != null && session.isActive) {
                                                RunOverlayCard {
                                                    ChallengeRunStrip(focus, focus.expected(session.steps, distanceKm, session.startedAt))
                                                }
                                            }
                                        }
                                    }
                                },
                                bottom = {
                                    Column(
                                        Modifier.fillMaxWidth().padding(horizontal = 16.dp)
                                            .onGloballyPositioned { seenBottom = it.boundsInRoot().top },
                                    ) {
                                        RunBottomPanel(
                                            speedKmh = speedKmh, speedError = speedError, shoe = equipped,
                                            dim = session.isPaused,
                                        )
                                    }
                                },
                            )
                        }
                        Column(
                            Modifier.fillMaxWidth().padding(horizontal = 16.dp).padding(top = 10.dp, bottom = 14.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp),
                        ) {
                            when {
                                session.saveStatus == RunSaveStatus.SAVING -> RunPrimaryButton(
                                    stringResource(R.string.run_saving), {}, enabled = false,
                                    modifier = Modifier.fillMaxWidth().testTag("run-primary-action"),
                                )
                                session.saveStatus == RunSaveStatus.FAILED -> {
                                    RunPrimaryButton(stringResource(R.string.run_save_retry), { WalkSessionService.stop(context) },
                                        modifier = Modifier.fillMaxWidth().testTag("run-primary-action"))
                                    Text(stringResource(R.string.run_save_failed), color = Alert,
                                        style = MaterialTheme.typography.bodyMedium, textAlign = TextAlign.Center,
                                        modifier = Modifier.fillMaxWidth().testTag("run-save-error"))
                                }
                                dietDone -> RunPrimaryButton(stringResource(R.string.diet_save_finish), {
                                    WalkSessionService.stop(context)
                                }, modifier = Modifier.fillMaxWidth().testTag("run-primary-action"))
                                session.isActive -> Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                                    RunPrimaryButton(
                                        stringResource(if (running) R.string.cd_pause else R.string.runflow_resume),
                                        onClick = {
                                            if (running) {
                                                WalkSessionService.pause(context)
                                                pauseDialog = true
                                            } else {
                                                WalkSessionService.resume(context)
                                            }
                                        },
                                        icon = if (running) Icons.Filled.Pause else Icons.Filled.PlayArrow,
                                        modifier = Modifier.weight(1.5f).testTag("run-primary-action"),
                                    )
                                    RunStopButton(stringResource(R.string.cd_stop), askEnd, Modifier.weight(1f).testTag("run-finish"))
                                }
                                else -> RunPrimaryButton(stringResource(R.string.home_start_run), requestStart,
                                    icon = Icons.Filled.PlayArrow, modifier = Modifier.fillMaxWidth().testTag("run-primary-action"))
                            }
                        }
                    }
                }
            }
        }
        if (startPending && !session.isActive) {
            RunPermissionFlow(
                onReady = {
                    startPending = false
                    locationAllowed = StepPermissions.hasLocation(context)
                    countingDown = true
                },
                // 닫기 — 아무것도 시작하지 않는다. 이 러닝을 연 화면(시작 메뉴 · 챌린지 · 다이어트)에서 왔으면 그리로
                onCancel = {
                    startPending = false
                    if (autoStart) onBack()
                },
                onHome = {
                    startPending = false
                    onLeaveToHome()
                },
            )
        }
        if (countingDown && !session.isActive) {
            // 권한이 있어도 휴대폰 위치가 꺼져 있으면 위치가 오지 않는다 — 시작 전에 미리 알린다
            val locationServicesOn = remember {
                val lm = androidx.core.content.ContextCompat.getSystemService(context, android.location.LocationManager::class.java)
                lm == null || runCatching { androidx.core.location.LocationManagerCompat.isLocationEnabled(lm) }.getOrDefault(true)
            }
            RunCountdown(
                courseName = course?.name,
                locationAllowed = locationAllowed,
                locationServicesOn = locationServicesOn,
                onGo = {
                    countingDown = false
                    if (!WalkSessionService.state.value.isActive) WalkSessionService.start(context)
                },
                // 시작 취소 — 이 러닝을 연 화면(시작 메뉴 · 챌린지)으로 돌아간다(시안 R01)
                onCancel = { countingDown = false; onBack() },
            )
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

    // S01 — 저장이 실패하면 한 번 알린다. 기록은 화면에 남고, 다시 저장하거나 머무를 수 있다
    var saveFailedDialog by rememberSaveable { mutableStateOf(false) }
    LaunchedEffect(session.saveStatus) {
        if (session.saveStatus == RunSaveStatus.FAILED) saveFailedDialog = true
    }
    if (saveFailedDialog && session.saveStatus == RunSaveStatus.FAILED) {
        KitDialog(
            title = stringResource(R.string.runflow_save_failed_title),
            body = stringResource(R.string.runflow_save_failed_body),
            onDismiss = { saveFailedDialog = false },
            modifier = Modifier.testTag("run-save-failed-dialog"),
        ) {
            KitButton(stringResource(R.string.runflow_save_again), {
                saveFailedDialog = false
                WalkSessionService.stop(context)
            }, modifier = Modifier.testTag("run-save-again"))
            KitButton(stringResource(R.string.runflow_save_stay), { saveFailedDialog = false }, tone = KitTone.Secondary,
                modifier = Modifier.testTag("run-save-stay"))
        }
    }

    // R03 — 잠깐 쉬어가요. 바깥을 누르면 창만 닫고 멈춘 채로 둔다
    if (pauseDialog && session.isActive && session.saveStatus == RunSaveStatus.IDLE) {
        KitDialog(
            title = stringResource(R.string.runflow_pause_title),
            body = stringResource(if (diet != null) R.string.diet_pause_body else R.string.runflow_pause_body),
            onDismiss = { pauseDialog = false },
            modifier = Modifier.testTag("run-pause-dialog"),
        ) {
            KitButton(stringResource(if (diet != null) R.string.diet_resume else R.string.runflow_resume), {
                pauseDialog = false
                WalkSessionService.resume(context)
            }, modifier = Modifier.testTag("run-pause-resume"))
            KitButton(stringResource(R.string.runflow_end), askEnd, tone = KitTone.Secondary)
        }
    }

    // R04 — 러닝을 마칠까요? 저장하고 마치기 · 계속 달리기 · 기록 없이 끝내기
    if (confirmStop && canAskEnd) {
        KitDialog(
            title = stringResource(R.string.run_stop_confirm_title),
            body = stringResource(R.string.run_stop_confirm_body),
            onDismiss = { confirmStop = false },
            modifier = Modifier.testTag("run-end-dialog"),
        ) {
            KitButton(stringResource(R.string.run_stop_confirm_yes), {
                confirmStop = false
                WalkSessionService.stop(context)
            })
            KitButton(stringResource(R.string.run_stop_confirm_no), {
                confirmStop = false
                WalkSessionService.resume(context)
            }, tone = KitTone.Secondary, modifier = Modifier.testTag("run-end-continue"))
            // 모임 러닝(방에 들어가 있으면 혼자여도)은 저장하고 마친다 — 서비스도 같은 까닭으로 거절한다
            val partyOpen by com.stepup.android.core.ServiceLocator.crewRepository.party.collectAsStateWithLifecycle()
            if (WalkSessionService.canDiscard(session) && !partyOpen.isActive) {
                KitButton(stringResource(R.string.runflow_end_discard), {
                    confirmStop = false
                    discardDialog = true
                }, tone = KitTone.Secondary, modifier = Modifier.testTag("run-end-discard"))
            }
        }
    }

    // R07 — 저장 없이 끝낼까요? 돌아가면 R04 로
    if (discardDialog && canAskEnd && !discarding) {
        KitDialog(
            title = stringResource(R.string.runflow_discard_title),
            body = stringResource(R.string.runflow_discard_body),
            onDismiss = { discardDialog = false; confirmStop = true },
            modifier = Modifier.testTag("run-discard-dialog"),
        ) {
            KitButton(stringResource(R.string.runflow_discard_confirm), {
                discardDialog = false
                // "코스 만들기"로 시작한 러닝이면 녹화도 그만둔다 — 다음 러닝이 녹화로 이어지지 않게
                if (recordingCourse) viewModel.cancelRecording()
                discarding = true
                WalkSessionService.discard(context)
            }, tone = KitTone.Danger, modifier = Modifier.testTag("run-discard-confirm"))
            KitButton(stringResource(R.string.runflow_discard_back), {
                discardDialog = false
                confirmStop = true
            }, tone = KitTone.Secondary, modifier = Modifier.testTag("run-discard-back"))
        }
    }

    // C01 — 목표를 달성했어요. 계속 달리면 기록은 이어진다
    if (showGoalReached && session.isActive && session.saveStatus == RunSaveStatus.IDLE) {
        KitDialog(
            title = stringResource(R.string.runflow_goal_reached_title),
            body = stringResource(R.string.runflow_goal_reached_body),
            onDismiss = { showGoalReached = false },
            modifier = Modifier.testTag("run-goal-reached"),
        ) {
            KitButton(stringResource(R.string.run_stop_confirm_yes), {
                showGoalReached = false
                WalkSessionService.stop(context)
            })
            KitButton(stringResource(R.string.run_stop_confirm_no), { showGoalReached = false }, tone = KitTone.Secondary)
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


internal fun formatPace(secPerKm: Long): String =
    "%d'%02d\"".format(secPerKm / 60, secPerKm % 60)

/** 페이스 게이지 정규화 — 15'00"/km ≈ 0, 5'00"/km ≈ 1 */
private fun paceFraction(secPerKm: Long?): Float =
    if (secPerKm == null || secPerKm <= 0) 0f else ((900f - secPerKm) / 600f).coerceIn(0f, 1f)

internal fun formatDuration(totalSec: Long): String {
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
/** 서버가 확인했고 그 금액까지 읽었을 때만 확정 — 금액이 0 이면(무효 · 상한) 확인은 됐어도 적립이 아니다 */
private fun finishConfirmed(session: WalkSessionState, points: Double?, upload: String?): Boolean =
    session.lastVerdict != RunVerdict.VOID && upload == UploadState.SIGNED.name && points != null && points > 0.0

private fun finishKm(session: WalkSessionState): Double =
    if (session.lastGpsKm > 0.0) session.lastGpsKm else RewardEconomy.distanceMeters(session.lastSessionSteps) / 1000

private fun finishPace(session: WalkSessionState): Long? {
    val km = finishKm(session)
    return if (km >= 0.05 && session.lastElapsedSec > 0) (session.lastElapsedSec / km).toLong() else null
}

/**
 * 러닝 완료의 보상 줄(전달본 03) — 서버가 확인하기 전에는 이 폰이 셈한 값을 "예상 보상 · 정산 대기"로만 보인다.
 * 서버가 확인하고 금액까지 읽었을 때만 "보상 · 적립 완료". 무효 · 거절 · 적립 없음은 0 과 그 까닭.
 */
@Composable
private fun finishReward(session: WalkSessionState, points: Double?, upload: String?): ResultReward {
    val sync by com.stepup.android.core.ServiceLocator.economySync.state.collectAsStateWithLifecycle()
    val voided = session.lastVerdict == RunVerdict.VOID
    // 걸음이 0 인 러닝은 서버에 올리지 않는다(올릴 것이 없다) — "정산 대기"로 영영 두지 않고 적립 없음으로
    val nothingToUpload = !voided && session.lastSessionSteps <= 0
    val noReward = !voided && upload == UploadState.SIGNED.name && points != null && points <= 0.0
    val reward = stringResource(R.string.result_reward)
    return when {
        voided -> ResultReward(reward, "0", Settle.VOID, listOf(stringResource(R.string.run_void_body)))
        // 따로 도는 두 흐름이 잠깐 어긋나도 "+0" 을 보이지 않게 — 확정은 서버 금액까지 읽었을 때만
        finishConfirmed(session, points, upload) && points != null -> ResultReward(reward, "+" + rewardAmount(points), Settle.DONE)
        nothingToUpload -> ResultReward(reward, "0", Settle.NOT_PAID, listOf(stringResource(R.string.finish_no_steps)))
        noReward -> ResultReward(reward, "0", Settle.NOT_PAID, listOf(stringResource(R.string.finish_no_reward)))
        upload == UploadState.REJECTED.name -> ResultReward(reward, "0", Settle.NOT_PAID, listOf(stringResource(R.string.finish_rejected)))
        else -> {
            val estimate = session.lastRewardPoints ?: 0.0
            // 로그인이 풀렸으면 올리기가 조용히 멈춘다 — 서버는 7일 지난 러닝을 받지 않으므로 여기서 알린다
            val signedOut = sync == com.stepup.android.data.repo.EconomySyncState.SIGNED_OUT
            ResultReward(
                label = stringResource(R.string.result_estimated_reward),
                amount = if (estimate > 0.0) "+" + rewardAmount(estimate) else "0",
                settle = Settle.PENDING,
                notes = if (signedOut) listOf(stringResource(R.string.finish_sign_in_needed)) else emptyList(),
                signIn = signedOut,
            )
        }
    }
}

/**
 * 결과 이미지 저장(전달본 03 "이미지 저장") — 공유하기와 같은 그림을 사진(Pictures/StepUp)에 넣는다.
 * Android 9 이하는 저장 권한을 먼저 묻는다(10 부터는 권한 없이 사진에 넣을 수 있다).
 */
@Composable
private fun rememberFinishSave(session: WalkSessionState): () -> Unit {
    val context = LocalContext.current
    val scope = androidx.compose.runtime.rememberCoroutineScope()
    val km = finishKm(session)
    val paceSec = finishPace(session)
    val labels = RunShareCard.Labels(
        distance = stringResource(R.string.share_card_distance),
        time = stringResource(R.string.share_card_time),
        pace = stringResource(R.string.share_card_pace),
        footer = stringResource(R.string.share_card_footer),
    )
    val savedText = stringResource(R.string.result_saved)
    val failedText = stringResource(R.string.result_save_failed)
    val save: () -> Unit = {
        scope.launch {
            val saved = kotlinx.coroutines.withContext(Dispatchers.IO) {
                runCatching {
                    val card = RunShareCard.render(
                        context = context,
                        km = km,
                        elapsed = formatDuration(session.lastElapsedSec),
                        pace = paceSec?.let { "%d'%02d\"".format(it / 60, it % 60) } ?: "—",
                        track = session.geoTrack,
                        labels = labels,
                    )
                    RunImageStore.save(context, card, "StepUp-run-${session.lastStartedAt}")
                }.getOrDefault(false)
            }
            android.widget.Toast.makeText(context, if (saved) savedText else failedText, android.widget.Toast.LENGTH_SHORT).show()
        }
    }
    val permission = androidx.activity.compose.rememberLauncherForActivityResult(
        androidx.activity.result.contract.ActivityResultContracts.RequestPermission(),
    ) { granted ->
        if (granted) save() else android.widget.Toast.makeText(context, failedText, android.widget.Toast.LENGTH_SHORT).show()
    }
    return {
        if (RunImageStore.needsPermission(context)) {
            permission.launch(android.Manifest.permission.WRITE_EXTERNAL_STORAGE)
        } else {
            save()
        }
    }
}

/** 러닝 중 위 판 — 계획마다 두 칸의 이름 · 값과 아래 한두 줄(자유 러닝: 러닝 시간 | 달린 거리만) */
@Composable
private fun RunPlanPanel(
    diet: DietRoutine?,
    dietPos: DietRoutine.Position?,
    dietDone: Boolean,
    goal: com.stepup.android.domain.RunGoal?,
    goalReached: Boolean,
    elapsedSec: Long,
    km: Double,
    paused: Boolean,
) {
    val timeLabel = stringResource(R.string.runflow_time_label)
    val distance = stringResource(R.string.runflow_distance_label) to "%.2f".format(km)
    when {
        // D10 — 루틴 완료
        diet != null && (dietDone || dietPos == null) -> RunTopPanel(
            left = stringResource(R.string.diet_total_time) to formatDuration(diet.totalSec),
            right = distance, rightUnit = "km", dim = false,
            captions = listOf(
                stringResource(R.string.diet_done_headline, (diet.totalSec / 60).toInt()),
                stringResource(R.string.diet_total_run) + " " + minutesText(diet.totalRunSec) + " · " +
                    stringResource(R.string.diet_total_walk) + " " + minutesText(diet.totalWalkSec),
            ),
        )
        // D07 · D08 · D09 · D11 — 남은 구간 시간, 이번 구간 안내와 다음 구간(버튼이 아니다)
        diet != null && dietPos != null -> {
            val upcoming = diet.next(dietPos.index)
            RunTopPanel(
                left = dietSubtitle(dietPos.segment, diet) to formatDuration(dietPos.remainingSec),
                right = stringResource(R.string.diet_total_exercise) to formatDuration(elapsedSec), rightUnit = null,
                dim = paused, progress = dietPos.fraction,
                captions = listOf(
                    dietHeadline(dietPos.segment) + " · " + if (upcoming == null) stringResource(R.string.diet_next_last)
                    else stringResource(R.string.diet_next, dietKindName(upcoming.kind), minutesText(upcoming.seconds)),
                    stringResource(R.string.diet_next_auto),
                ),
            )
        }
        goal != null -> RunTopPanel(
            left = timeLabel to formatDuration(elapsedSec), right = distance, rightUnit = "km", dim = paused,
            progress = goal.fraction(elapsedSec, km),
            captions = listOf(when {
                goalReached -> stringResource(R.string.runflow_goal_reached_title)
                goal.km != null -> stringResource(R.string.runflow_goal_dist_left, "%.2f".format(max(goal.km - km, 0.0)))
                goal.seconds != null -> stringResource(R.string.runflow_goal_time_left, formatDurationWords(max(goal.seconds - elapsedSec, 0L)))
                else -> goalName(goal)
            }),
        )
        else -> RunTopPanel(left = timeLabel to formatDuration(elapsedSec), right = distance, rightUnit = "km", dim = paused)
    }
}

/** 결과 공유 — 달린 길과 기록을 그림 한 장으로. 그림을 못 만들면 글만 보낸다. */
@Composable
private fun rememberFinishShare(session: WalkSessionState, points: Double?, upload: String?): () -> Unit {
    val context = LocalContext.current
    val km = finishKm(session)
    val paceSec = finishPace(session)
    val shareLabels = RunShareCard.Labels(
        distance = stringResource(R.string.share_card_distance),
        time = stringResource(R.string.share_card_time),
        pace = stringResource(R.string.share_card_pace),
        footer = stringResource(R.string.share_card_footer),
    )
    val shareText = if (finishConfirmed(session, points, upload) && points != null) {
        stringResource(R.string.finish_share_text, "%.1f".format(km),
            formatDuration(session.lastElapsedSec), com.stepup.android.ui.components.formatSupDown(points))
    } else {
        stringResource(R.string.finish_share_activity, "%.1f".format(km), formatDuration(session.lastElapsedSec))
    }
    return {
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
    }
}

/**
 * 러닝 중 머리 — 상태 · GPS · 큰 시계 · 거리와 평균 페이스.
 *
 * 시계는 시스템 글자 크기를 따르되, 전체 시간이 한 줄에 들어오도록 폭에 맞춘다.
 */
@Composable
private fun GpsStatusLine(
    gpsFix: Boolean,
    locationAllowed: Boolean,
    /** GPS 전에 대략적인 위치는 알고 있다 */
    roughFix: Boolean = false,
    /** 잡혔던 위치가 한동안 안 온다(L04) */
    lost: Boolean = false,
) {
    val gps = stringResource(when {
        !locationAllowed -> R.string.run_location_disabled
        lost -> R.string.runflow_gps_lost_title
        gpsFix -> R.string.run_gps_ok
        roughFix -> R.string.run_gps_search_rough
        else -> R.string.run_gps_search
    })
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        Box(Modifier.size(7.dp).background(if (gpsFix && locationAllowed) com.stepup.android.ui.theme.Cyan else Slate, CircleShape))
        Text(gps,
            color = if (gpsFix && locationAllowed) com.stepup.android.ui.theme.VoltText else Silver,
            fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
    }
}

/** 이번 러닝의 이름 — "자유 러닝" · "10분 러닝" */
@Composable
private fun planTitle(plan: RunPlan): String = when (plan) {
    is RunPlan.Goal -> goalName(plan.goal)
    is RunPlan.Diet -> stringResource(R.string.runflow_diet)
    RunPlan.Free -> stringResource(R.string.runflow_free)
}

@Composable
private fun planHeadline(plan: RunPlan): String = stringResource(when (plan) {
    is RunPlan.Goal -> if (plan.goal.isDistance) R.string.runflow_goal_dist_headline else R.string.runflow_goal_time_headline
    else -> R.string.runflow_free_headline
})

@Composable
private fun planSubtitle(plan: RunPlan): String = when (plan) {
    is RunPlan.Goal -> if (plan.goal.isDistance) stringResource(R.string.runflow_goal_dist_sub)
        else stringResource(R.string.runflow_goal_time_sub, goalName(plan.goal))
    else -> stringResource(R.string.runflow_free_sub)
}

@Composable
private fun dietKindName(kind: DietSegmentKind): String = stringResource(when (kind) {
    DietSegmentKind.WARMUP -> R.string.diet_seg_warmup
    DietSegmentKind.RUN -> R.string.diet_seg_run
    DietSegmentKind.WALK -> R.string.diet_seg_walk
    DietSegmentKind.COOLDOWN -> R.string.diet_seg_cooldown
})

@Composable
private fun dietHeadline(segment: DietSegment): String = stringResource(when (segment.kind) {
    DietSegmentKind.WARMUP -> R.string.diet_head_warmup
    DietSegmentKind.RUN -> R.string.diet_head_run
    DietSegmentKind.WALK -> R.string.diet_head_walk
    DietSegmentKind.COOLDOWN -> R.string.diet_head_cooldown
})

/** "준비 걷기 · 3분" · "달리기 · 1회 / 3회" */
@Composable
private fun dietSubtitle(segment: DietSegment, routine: DietRoutine): String = when (segment.kind) {
    DietSegmentKind.RUN, DietSegmentKind.WALK ->
        stringResource(R.string.diet_round_of, dietKindName(segment.kind), segment.round, routine.rounds)
    else -> dietKindName(segment.kind) + " · " + minutesText(segment.seconds)
}

/** "3분 36초" 모양 — 남은 시간 한 줄 */
@Composable
private fun formatDurationWords(totalSec: Long): String {
    val m = totalSec / 60
    val sec = totalSec % 60
    return if (m > 0) stringResource(R.string.runflow_min_sec, m.toInt(), sec.toInt())
    else stringResource(R.string.runflow_sec, sec.toInt())
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

/**
 * 같이 뛰는 중 실시간 순위 — 거리를 보이기로 한 사람만 숫자가 있고, 나머지는 "비공개"로 아래에 둔다.
 * 거리는 각 폰이 잰 화면용 값이다(적립과 무관).
 */
@Composable
internal fun TogetherRanking(members: List<com.stepup.android.data.repo.PartyMember>, myKm: Double) {
    val rows = members.map { m -> Triple(m, if (m.isMe) myKm else m.km, m.isMe) }
        .sortedWith(compareByDescending<Triple<com.stepup.android.data.repo.PartyMember, Double?, Boolean>> { it.second != null }
            .thenByDescending { it.second ?: 0.0 })
    Column(Modifier.fillMaxWidth().testTag("run-together-ranking"), verticalArrangement = Arrangement.spacedBy(2.dp)) {
        Text(stringResource(R.string.run_together_title, members.size), color = Silver,
            style = MaterialTheme.typography.bodySmall, modifier = Modifier.padding(bottom = 4.dp))
        rows.forEachIndexed { index, (member, km, me) ->
            Row(Modifier.fillMaxWidth().heightIn(min = 36.dp), verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(if (km != null) "${index + 1}" else "–", color = Slate, fontSize = 13.sp,
                    modifier = Modifier.width(18.dp))
                Text(
                    if (me) stringResource(R.string.run_together_me) else member.name,
                    color = if (me) Snow else Silver, fontSize = 14.sp, maxLines = 1, modifier = Modifier.weight(1f),
                )
                Text(km?.let { "%.2f km".format(it) } ?: stringResource(R.string.run_together_hidden),
                    color = if (km != null) Snow else Slate, fontSize = 13.sp)
            }
        }
    }
}

/** 위치가 안 잡히는 까닭 한 줄 + 고치러 가는 버튼 */
@Composable
private fun LocationHint(text: String, action: String, tag: String, onAction: () -> Unit) {
    Column(Modifier.fillMaxWidth().padding(top = 8.dp).testTag(tag), horizontalAlignment = Alignment.CenterHorizontally) {
        Text(text, color = Silver, style = MaterialTheme.typography.bodyMedium, textAlign = TextAlign.Center)
        TextButton(onClick = onAction) { Text(action) }
    }
}

/** 러닝 중 챌린지 예상 진행 — 확정은 저장 · 서버 확인 뒤 챌린지 화면에서 */
@Composable
private fun ChallengeRunStrip(focus: com.stepup.android.ui.screens.events.ChallengeFocus, value: Double) {
    val km = focus.kind == com.stepup.android.ui.screens.events.ChallengeKind.NIGHT
    fun fmt(v: Double) = if (km) "%.1f".format(v) else "%,d".format(v.toLong())
    val title = stringResource(when (focus.kind) {
        com.stepup.android.ui.screens.events.ChallengeKind.DAILY -> R.string.challenge_daily_title
        com.stepup.android.ui.screens.events.ChallengeKind.WEEKLY -> R.string.event_step_surge
        com.stepup.android.ui.screens.events.ChallengeKind.NIGHT -> R.string.event_night_quest
    })
    val unit = stringResource(if (km) R.string.challenge_unit_km else R.string.challenge_unit_steps)
    val fraction = focus.fraction(value)
    Column(Modifier.fillMaxWidth().testTag("run-challenge-progress"), verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Text(title, color = com.stepup.android.ui.theme.VoltText, fontSize = 13.sp, fontWeight = FontWeight.SemiBold,
                modifier = Modifier.weight(1f), maxLines = 1)
            Text("${fmt(value)} / ${fmt(focus.target)} $unit · ${(fraction * 100).toInt()}%", color = Snow, fontSize = 13.sp)
        }
        BarMeter(fraction = fraction, height = 5.dp)
        Text(stringResource(R.string.challenge_run_expected), color = Slate, fontSize = 11.sp)
    }
}

/** 러닝 지도의 위 · 아래가 바닥색에 녹아드는 띠(높이 비율) — 그 사이가 또렷한 곳 */
private const val MAP_FADE_TOP = 0.14f
private const val MAP_FADE_BOTTOM = 0.72f
