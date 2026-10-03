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
import androidx.compose.ui.graphics.asImageBitmap
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
import com.stepup.android.ui.components.RunNotice
import com.stepup.android.ui.components.RunNoticeKind
import com.stepup.android.ui.components.RunStat
import com.stepup.android.ui.components.runTone
import androidx.compose.material.icons.filled.Groups

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
    /** 결과의 "내 러닝 기록 보기" · 기록 삭제 뒤 — 러닝 기록 */
    onOpenRecords: () -> Unit = {},
    /** 결과 보상 칸의 "내역 보기"(미지급 E09) — 지갑 */
    onOpenWallet: () -> Unit = {},
    viewModel: WalkViewModel = viewModel(factory = WalkViewModel.Factory),
) {
    // 러닝 서비스는 끝난 러닝을 백그라운드 스레드에서 내놓는다. 화면은 그 값을 메인 스레드에서 받는다 — 기기 테스트의
    // 즉시 실행 환경에서 결과 목록(LazyColumn)을 백그라운드 스레드에서 처음 그리다 멈췄다(Looper 없음, QA 147)
    val session by viewModel.session.collectAsStateWithLifecycle(context = Dispatchers.Main.immediate)
    // 경로 좌표는 읽을 때마다 새 목록을 만든다(track.toGeoPoints) — 갱신(1초에 여러 번)마다 여러 번 만들지 않게 경로가 바뀔 때만
    val geoTrack = remember(session.track) { session.geoTrack }
    val energy by viewModel.energy.collectAsStateWithLifecycle()
    val sneakerLevel by viewModel.sneakerLevel.collectAsStateWithLifecycle()
    val equipped by viewModel.equipped.collectAsStateWithLifecycle()
    val xpBoosted by viewModel.xpBoosted.collectAsStateWithLifecycle()
    val balance by viewModel.balance.collectAsStateWithLifecycle()
    val laps by viewModel.laps.collectAsStateWithLifecycle()
    // 값으로 받는다 — 아래에서 고른 코스가 있는지 본 뒤 그 코스의 거리 · 이름을 그대로 쓴다
    val course = viewModel.selectedCourse.collectAsStateWithLifecycle().value
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

    // 코스 녹화 — "코스 만들기"에서 넘어온 러닝인지, 그리고 방금 끝난 트랙.
    // 러닝이 끝나고(isActive=false) 트랙이 남아 있으면 저장 창을 띄운다.
    val recordingCourse by viewModel.courseRecording.collectAsStateWithLifecycle()
    val recordedTrack by viewModel.lastTrack.collectAsStateWithLifecycle()
    val readyToSaveCourse = recordingCourse && !session.isActive && recordedTrack.size >= 2

    var countingDown by rememberSaveable { mutableStateOf(false) }
    // 한 번 고른 러닝 — 권한 안내(시작·로그인·첫 사용 v1 시안 13~19)를 활동 → 위치 → 알림 차례로 지나 3-2-1(R01)로 간다.
    // 안내를 닫으면 아무것도 시작하지 않는다. 필요한 권한이 이미 있으면 안내 없이 바로 3-2-1 이다.
    var startPending by rememberSaveable { mutableStateOf(false) }
    // E07 — 위치 권한은 있는데 휴대폰 위치 기능이 꺼져 있어 시작 전에 묻는 중
    var locationOffGate by rememberSaveable { mutableStateOf(false) }
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

    // 확인 창들 — 종료(R04) · 저장 없이 끝내기(R07) · 목표 달성(C01). 일시정지(R03)는 창 없이 화면이 바뀐다
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
            showGoalReached = false
            confirmStop = true
        }
    }
    // 러닝이 끝나면(알림에서 마쳤거나 다른 화면에서 저장) 묻던 창을 접는다 — 다음 러닝에 남지 않게
    LaunchedEffect(session.isActive) {
        if (!session.isActive) {
            confirmStop = false
            discardDialog = false
            showGoalReached = false
        }
    }
    // 달리는 중 뒤로 가기는 화면을 닫지 않는다 — 달리고 있으면 일시정지(R03), 멈춰 있으면 마칠지 묻는다(R04)
    androidx.activity.compose.BackHandler(enabled = canAskEnd && !confirmStop && !discardDialog) {
        if (!session.isPaused) WalkSessionService.pause(context) else askEnd()
    }

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
    // 러닝 전체 리메이크(2026-10-02) — 전체 지도(K07) · 목표 수정(E02) · 위치 신호(L04) · 코스 이탈(K05) · 자유 러닝 전환(K08)
    var showFullMap by rememberSaveable { mutableStateOf(false) }
    var goalEdit by rememberSaveable { mutableStateOf<Double?>(null) }
    var gpsLostSheet by rememberSaveable { mutableStateOf(false) }
    var gpsLostSeen by rememberSaveable { mutableStateOf(false) }
    var courseOffSheet by rememberSaveable { mutableStateOf(false) }
    var courseFreeSheet by rememberSaveable { mutableStateOf(false) }
    var togetherSheet by rememberSaveable { mutableStateOf(false) }
    // 결과 — 공유 미리보기(E05) · 삭제 확인(R06) · 삭제 실패(H16) · 무효 까닭
    var showShare by rememberSaveable { mutableStateOf(false) }
    // 크루에 기록 공유(CR21) — 크루 러닝 결과의 공유
    var crewShare by rememberSaveable { mutableStateOf(false) }
    var deleteSheet by rememberSaveable { mutableStateOf(false) }
    var deleting by remember { mutableStateOf(false) }
    var deleteFailed by rememberSaveable { mutableStateOf<String?>(null) }
    var voidReason by rememberSaveable { mutableStateOf(false) }
    // 전체 지도 · 공유 미리보기는 뒤로 가기로 닫는다(러닝 · 결과로 돌아간다). 러닝이 끝나거나 결과를 떠나면 접는다
    androidx.activity.compose.BackHandler(enabled = showFullMap && session.isActive) { showFullMap = false }
    androidx.activity.compose.BackHandler(enabled = showShare && finishing) { showShare = false }
    androidx.activity.compose.BackHandler(enabled = crewShare && finishing) { crewShare = false }
    LaunchedEffect(session.isActive) { if (!session.isActive) showFullMap = false }
    LaunchedEffect(finishing) {
        if (!finishing) {
            showShare = false
            crewShare = false
            deleteFailed = null
        }
    }
    val tone = runTone()
    val party by com.stepup.android.core.ServiceLocator.crewRepository.party.collectAsStateWithLifecycle()
    val crewRun = session.isActive && party.isActive || (!session.isActive && session.lastPartySize > 1)
    // 끝난 크루 러닝의 크루(번개러닝은 크루가 없다) — 결과에서 크루에 기록 공유(CR21)
    val crewShareId = party.crewId?.takeIf { crewRun && !session.isActive && it.isNotBlank() }
    val coursePoints = course?.takeIf { it.hasTrack && plan is RunPlan.Free && !recordingCourse }?.points.orEmpty()
    val courseRun = coursePoints.isNotEmpty() && !crewRun

    // 위치 신호가 끊기면(L04) 한 번 알린다 — 다시 잡히면 다음 끊김에 또 알린다
    LaunchedEffect(session.gpsLost, running) {
        if (session.gpsLost && running && !gpsLostSeen) {
            gpsLostSeen = true
            gpsLostSheet = true
        }
        if (!session.gpsLost) {
            gpsLostSeen = false
            gpsLostSheet = false
        }
    }
    // 코스에서 벗어났는가(K05) — 지금 자리가 코스 선에서 60m 넘게 15초 넘게 떨어지면 한 번 알린다(기록은 그대로 이어진다)
    val offCourse = courseRun && running && session.gpsFix && session.here != null &&
        com.stepup.android.domain.distanceToPathMeters(session.here!!, coursePoints) > OFF_COURSE_METERS
    var offSince by remember { mutableLongStateOf(0L) }
    var offShown by rememberSaveable { mutableStateOf(false) }
    LaunchedEffect(offCourse) {
        if (offCourse) {
            if (offSince == 0L) offSince = System.currentTimeMillis()
            kotlinx.coroutines.delay(OFF_COURSE_MS)
            if (!offShown) {
                offShown = true
                courseOffSheet = true
            }
        } else {
            offSince = 0L
            offShown = false
        }
    }

    // 달리는 중 뒤로 가기(머리 · 시스템) — 달리고 있으면 일시정지(R03), 멈춰 있으면 마칠지 묻는다(R04)
    val pauseOrAsk = {
        if (canAskEnd) {
            if (!session.isPaused) WalkSessionService.pause(context) else askEnd()
        }
    }
    val timeOnly = !locationAllowed || (!session.locationOn && !session.gpsFix && geoTrack.isEmpty())
    val liveKm = goalKmNow
    // 위치 없이 기록하는 동안(R02_TIME)은 거리 · 페이스를 재지 않은 것으로 둔다 — 걸음으로 셈한 값은 저장한 결과에만 그 근거와 함께 적는다
    val liveKmText = if (timeOnly) "—" else "%.2f".format(liveKm)
    val livePace = if (!timeOnly && liveKm >= 0.05 && session.elapsedSec > 0) formatPace((session.elapsedSec / liveKm).toLong()) else "—"
    val distanceCaption = if (timeOnly) stringResource(R.string.run_needs_location) else null
    val gpsBadge = when {
        !locationAllowed -> GpsBadge.None
        !session.locationOn && !session.gpsFix -> GpsBadge.Off
        session.gpsLost -> GpsBadge.Weak
        session.gpsFix && !session.precise -> GpsBadge.Approx
        session.gpsFix -> GpsBadge.Connected
        // GPS 전에 지도에 먼저 보인 자리는 대략적인 위치라고 알린다
        session.isActive && session.here != null -> GpsBadge.Rough
        else -> GpsBadge.Searching
    }
    val distanceStat = RunStat(stringResource(R.string.run_label_distance), liveKmText, "km", caption = distanceCaption, tag = "run-distance-value")
    val paceStat = RunStat(stringResource(R.string.run_label_pace), livePace, "/km", caption = distanceCaption)

    // 지도 틀 안 — 실제 지도(달린 길 · 지금 자리 · 고른 코스 · 같이 뛰는 사람). 위치 없이 기록하면 안내만
    val together = session.isActive && party.phase == com.stepup.android.data.repo.PartyPhase.RUNNING && party.members.size > 1
    val others = if (together && !party.networkProblem) {
        party.members.filter { !it.isMe }.mapNotNull { m -> m.point?.let { it to m.name } }
    } else emptyList()
    val breaks = remember(session.track) { com.stepup.android.domain.segmentBreaks(session.track) }
    val liveMap: @Composable androidx.compose.foundation.layout.BoxScope.() -> Unit = {
        val here = session.here
        when {
            timeOnly && geoTrack.isEmpty() -> NoLocationMap()
            geoTrack.isNotEmpty() -> LiveRouteMap(
                points = geoTrack, modifier = Modifier.fillMaxSize(), others = others, follow = true,
                breaks = breaks, course = coursePoints, routeColor = tone.cyan, live = true,
            )
            here != null -> LiveRouteMap(
                points = listOf(here), modifier = Modifier.fillMaxSize().testTag("run-rough-location"), others = others,
                follow = true, course = coursePoints, routeColor = tone.cyan, live = true,
            )
            coursePoints.isNotEmpty() -> LiveRouteMap(points = emptyList(), course = coursePoints, modifier = Modifier.fillMaxSize())
            else -> MapWaiting(Modifier.fillMaxSize())
        }
    }

    Box(Modifier.fillMaxSize()) {
        val dietFinished = finishing && diet != null && diet.finished(session.lastElapsedSec)
        val dietPartial = finishing && diet != null && !dietFinished
        val done = {
            viewModel.clearReward()
            onHome()
        }
        when {
            finishing && showShare -> RunShareScreen(session, lastServerPoints, lastUpload, onClose = { showShare = false })
            finishing && crewShare && crewShareId != null -> CrewShareFor(session, crewShareId, party.crewName, onClose = { crewShare = false })
            finishing -> {
                val km = finishKm(session)
                val paceSec = finishPace(session)
                val reward = finishReward(session, lastServerPoints, lastUpload)
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
                val goalMissed = goal != null && !goal.reached(session.lastElapsedSec, km)
                val lastTrackBreaks = remember(session.track) { com.stepup.android.domain.segmentBreaks(session.track) }
                val timeText = formatDuration(session.lastElapsedSec)
                val speed = averageSpeedKmh(km, session.lastElapsedSec)
                val baseStats = listOf(
                    RunStat(stringResource(R.string.run_label_time), timeText, tag = "run-result-time"),
                    RunStat(stringResource(R.string.run_label_pace), paceSec?.let { formatPace(it) } ?: "—", "/km", tag = "run-result-pace"),
                    RunStat(stringResource(R.string.run_label_speed), speed?.let { "%.1f".format(it) } ?: "—", "km/h", tag = "run-result-speed"),
                )
                val courseDone = courseRun || (course != null && plan is RunPlan.Free && session.lastPartySize <= 1 &&
                    course.hasTrack && km >= course.distanceKm * 0.98)
                val minutesTotal = (diet?.totalSec ?: 0L) / 60
                val ui = when {
                    diet != null && dietFinished -> RunResultUi(
                        title = stringResource(R.string.run_diet_saved_title),
                        subtitle = stringResource(R.string.run_diet_saved_sub, minutesTotal.toInt()),
                        pill = stringResource(R.string.run_diet_saved_pill, minutesTotal.toInt()),
                        date = null, heroValue = "%.2f".format(km), heroUnit = "km", heroLine = null,
                        stats = baseStats.take(2), message = null,
                    )
                    dietPartial -> RunResultUi(
                        title = stringResource(R.string.run_diet_saved_title),
                        pill = stringResource(R.string.run_diet_pill),
                        date = null,
                        heroAbove = stringResource(R.string.run_diet_partial_line, formatDurationWords(session.lastElapsedSec), minutesTotal.toInt()),
                        heroValue = formatDurationWords(session.lastElapsedSec), heroUnit = null,
                        heroLine = stringResource(R.string.run_diet_partial_good),
                        stats = listOf(
                            RunStat(stringResource(R.string.run_label_run_distance), "%.2f".format(km), "km", tag = "run-result-distance-stat"),
                            baseStats[0], baseStats[1],
                        ),
                        message = null,
                    )
                    reward.settle == Settle.NOT_PAID -> RunResultUi(
                        title = stringResource(R.string.run_result_saved_title),
                        subtitle = stringResource(R.string.run_result_saved_sub),
                        pill = null, date = date, heroValue = "%.2f".format(km), heroUnit = "km",
                        heroLine = planTitle(plan), stats = baseStats, message = null,
                    )
                    else -> RunResultUi(
                        title = if (crewRun) party.crewName ?: stringResource(R.string.run_crew_title) else stringResource(R.string.run_result_title),
                        subtitle = null,
                        pill = stringResource(if (crewRun) R.string.run_crew_line else R.string.run_saved_pill),
                        date = date,
                        heroAbove = when {
                            goal != null && goalMissed -> stringResource(R.string.run_result_challenge_tried, challengeTitle(goal), formatDurationWords(session.lastElapsedSec))
                            goal != null -> stringResource(R.string.run_result_challenge_line, challengeTitle(goal),
                                if (goal.isDistance) "%.2f km".format(km) else formatDurationWords(session.lastElapsedSec))
                            courseDone && course != null -> course.name
                            else -> null
                        },
                        heroValue = "%.2f".format(km), heroUnit = "km",
                        heroLine = when {
                            crewRun -> stringResource(R.string.run_crew_title)
                            goal != null || courseDone -> null
                            else -> planTitle(plan)
                        },
                        stats = baseStats,
                        message = when {
                            goal != null && goalMissed -> stringResource(R.string.run_result_partial)
                            courseDone && course != null && km >= course.distanceKm * 0.98 -> stringResource(R.string.run_result_course_done)
                            else -> stringResource(R.string.run_result_good)
                        },
                    )
                }.copy(note = if (session.lastGpsKm <= 0.0 && km > 0.0) stringResource(R.string.run_result_steps_basis) else null)
                RunResultContent(
                    ui = ui,
                    onBack = done,
                    onDelete = { deleteFailed = null; deleteSheet = true },
                    // 크루 러닝이면 크루 채팅으로(CR21), 아니면 공유 그림(E05)
                    onShare = { if (crewShareId != null) crewShare = true else showShare = true },
                    shareLabel = stringResource(if (crewShareId != null) R.string.run_crew_share else R.string.run_share),
                    onRecords = {
                        viewModel.clearReward()
                        onOpenRecords()
                    },
                    onHome = done,
                    map = {
                        if (geoTrack.isNotEmpty()) {
                            LiveRouteMap(points = geoTrack, modifier = Modifier.fillMaxSize().testTag("run-result-map"),
                                breaks = lastTrackBreaks, routeColor = tone.cyan)
                        } else {
                            com.stepup.android.ui.components.RunMapPlaceholder(
                                Icons.Filled.GpsFixed, stringResource(R.string.run_result_route_none), null,
                                Modifier.testTag("run-result-no-route"),
                            )
                        }
                    },
                    cards = {
                        deleteFailed?.let { why ->
                            FailureBanner(stringResource(R.string.run_delete_failed), why, "run-delete-failed")
                        }
                        if (crewRun) CrewResultCard(session.lastPartySize.takeIf { it > 1 })
                        equipped?.let { ResultShoeCard(it) }
                        ResultRewardCard(reward, onReason = { voidReason = true }, onHistory = onOpenWallet)
                    },
                    extra = {
                        if (diet != null) {
                            Spacer(Modifier.height(4.dp))
                            com.stepup.android.ui.components.RunTextAction(
                                stringResource(R.string.run_diet_repeat), {
                                    viewModel.clearReward()
                                    onRepeat(plan)
                                }, modifier = Modifier.fillMaxWidth().testTag("run-result-repeat"),
                            )
                        }
                    },
                    modifier = Modifier.reveal(session.lastStartedAt)
                        .celebrate(if (finishConfirmed(session, lastServerPoints, lastUpload)) session.lastStartedAt else null),
                )
            }
            session.isActive && session.saveStatus == RunSaveStatus.SAVING -> RunSavingContent(
                stats = listOf(
                    RunStat(stringResource(R.string.run_label_distance), liveKmText, "km"),
                    RunStat(stringResource(R.string.run_label_time), formatDuration(session.elapsedSec)),
                    RunStat(stringResource(R.string.run_label_pace), livePace, "/km"),
                ),
                map = liveMap,
            )
            dietDone && diet != null -> DietCompleteContent(
                totalMinutes = (diet.totalSec / 60).toInt(), runMinutes = (diet.totalRunSec / 60).toInt(),
                walkMinutes = (diet.totalWalkSec / 60).toInt(),
                onSave = { WalkSessionService.stop(context) }, saving = session.saveStatus == RunSaveStatus.SAVING,
            )
            showFullMap && session.isActive -> RunFullMapContent(
                title = stringResource(if (courseRun) R.string.run_course_map_title else R.string.run_full_map_title),
                courseName = if (courseRun) course?.name else null,
                stats = buildList {
                    add(RunStat(stringResource(R.string.run_label_run_distance), liveKmText, "km"))
                    if (courseRun && course != null) add(RunStat(stringResource(R.string.run_total_distance), "%.2f".format(course.distanceKm), "km"))
                    else add(RunStat(stringResource(R.string.run_label_time), formatDuration(session.elapsedSec)))
                },
                status = stringResource(if (session.isPaused) R.string.run_paused_title else R.string.run_status_recording),
                onClose = { showFullMap = false },
                map = {
                    val here = session.here
                    LiveRouteMap(
                        points = geoTrack.ifEmpty { listOfNotNull(here) }, modifier = Modifier.fillMaxSize(),
                        interactive = true, others = others, breaks = breaks, course = coursePoints, routeColor = tone.cyan, live = true,
                        controlLabels = com.stepup.android.ui.components.MapControlLabels(
                            zoomIn = stringResource(R.string.rec_zoom_in), zoomOut = stringResource(R.string.rec_zoom_out),
                            recenter = stringResource(R.string.rec_map_recenter),
                        ),
                    )
                },
            )
            session.isActive && session.saveStatus == RunSaveStatus.FAILED -> RunLiveContent(
                // S01 — 저장 전 요약. 기록은 서비스에 그대로 있고, 같은 러닝으로 다시 저장한다
                ui = LiveRunUi(
                    title = stringResource(R.string.run_result_title), paused = true,
                    status = stringResource(R.string.run_unsaved_pill), hero = LiveHero.Clock(stringResource(R.string.run_label_time), formatDuration(session.elapsedSec)),
                    stats = listOf(distanceStat, paceStat), primaryLabel = stringResource(R.string.run_save_again), primaryIcon = null,
                    finishLabel = "", pillCentered = false,
                ),
                onBack = {}, onDetails = { showDetails = true },
                onPrimary = { WalkSessionService.stop(context) }, onFinish = {}, onExpandMap = null, map = liveMap,
                showFinish = false,
                // 창을 닫은 뒤에도 저장되지 않았다는 것을 화면에 남긴다 — 기록은 서비스에 그대로 있고 같은 러닝으로 다시 저장한다
                notices = {
                    RunNotice(stringResource(R.string.run_save_failed_title), body = stringResource(R.string.run_save_failed_body),
                        kind = RunNoticeKind.Error, tag = "run-save-error")
                },
            )
            else -> {
                val ui = liveRunUi(
                    plan = plan, diet = diet, dietPos = dietPos, goal = goal, goalReached = goalReached,
                    elapsedSec = session.elapsedSec, km = liveKm, paused = session.isPaused,
                    timeOnly = timeOnly, gps = gpsBadge, distance = distanceStat, pace = paceStat,
                    courseName = if (courseRun) course?.name else null, courseKm = if (courseRun) course?.distanceKm else null,
                    crewName = if (crewRun) party.crewName ?: "" else null, active = session.isActive,
                    asking = confirmStop || discardDialog,
                )
                RunLiveContent(
                    ui = ui,
                    onBack = { if (session.isActive) pauseOrAsk() else onBack() },
                    onDetails = { showDetails = true },
                    onPrimary = {
                        when {
                            !session.isActive -> requestStart()
                            running -> WalkSessionService.pause(context)
                            else -> WalkSessionService.resume(context)
                        }
                    },
                    onFinish = { if (session.isActive) askEnd() else onBack() },
                    onExpandMap = if (timeOnly && geoTrack.isEmpty()) null else ({ showFullMap = true }),
                    map = liveMap,
                    notices = {
                        val voidNow = session.isActive && session.liveVerdict == RunVerdict.VOID
                        val flaggedNow = session.isActive && session.flaggedSegments > 0
                        if (voidNow) {
                            RunNotice(stringResource(R.string.run_live_void_title), body = stringResource(R.string.run_void_banner),
                                kind = RunNoticeKind.Error, action = stringResource(R.string.run_details_more), onAction = { showDetails = true },
                                tag = "run-void-banner")
                        } else if (flaggedNow) {
                            RunNotice(stringResource(R.string.run_flag_title), body = stringResource(R.string.run_flag_body),
                                kind = RunNoticeKind.Warn, action = stringResource(R.string.run_details_more), onAction = { showDetails = true },
                                tag = "run-speed-banner")
                        }
                        if (recordingCourse && !readyToSaveCourse) {
                            RunNotice(stringResource(R.string.course_rec_title),
                                body = stringResource(if (session.isActive) R.string.course_rec_running else R.string.course_rec_ready),
                                action = stringResource(R.string.common_cancel), onAction = viewModel::cancelRecording, tag = "run-course-recording")
                        }
                        if (session.isActive && locationAllowed && (!session.gpsFix || !session.precise)) {
                            when {
                                !session.locationOn && !session.gpsFix -> RunNotice(
                                    stringResource(R.string.run_location_off_hint), kind = RunNoticeKind.Warn,
                                    action = stringResource(R.string.run_location_off_action),
                                    onAction = { ExternalIntents.openLocationSettings(context) }, tag = "run-location-off",
                                )
                                !session.precise -> RunNotice(
                                    stringResource(R.string.run_location_approx_hint), kind = RunNoticeKind.Warn,
                                    action = stringResource(R.string.cd_open_settings),
                                    onAction = { ExternalIntents.openAppSettings(context) }, tag = "run-location-approx",
                                )
                            }
                        }
                        // CR09 · CR11 · CR18 — 같이 뛰는 중. 연결이 끊기면 옛 인원 · 위치를 보이지 않는다(내 기록은 이어진다)
                        if (crewRun && session.isActive) {
                            if (party.networkProblem) {
                                RunNotice(stringResource(R.string.run_crew_reconnect_title), body = stringResource(R.string.run_crew_reconnect_body),
                                    kind = RunNoticeKind.Warn, action = stringResource(R.string.run_crew_reconnect),
                                    onAction = { com.stepup.android.core.ServiceLocator.crewRepository.wakeParty() }, tag = "run-crew-offline")
                                CrewOfflineCard()
                            } else if (session.isPaused) {
                                RunNotice(stringResource(R.string.run_crew_others_running), icon = Icons.Filled.Groups, tag = "run-crew-others")
                            } else if (together) {
                                TogetherRow(party.members.size) { togetherSheet = true }
                            }
                        }
                        val challenge by com.stepup.android.ui.screens.events.ChallengeRunFocus.current.collectAsStateWithLifecycle()
                        LaunchedEffect(session.isActive, session.startedAt) {
                            if (session.isActive) com.stepup.android.ui.screens.events.ChallengeRunFocus.bind(session.startedAt)
                        }
                        val focus = challenge?.takeIf { com.stepup.android.ui.screens.events.ChallengeRunFocus.matches(session.startedAt) }
                        if (focus != null && session.isActive) {
                            com.stepup.android.ui.components.RunCard(padding = PaddingValues(horizontal = 14.dp, vertical = 10.dp)) {
                                ChallengeRunStrip(focus, focus.expected(session.steps, distanceKm, session.startedAt))
                            }
                        }
                    },
                )
            }
        }
        if (startPending && !session.isActive) {
            RunPermissionFlow(
                onReady = {
                    startPending = false
                    locationAllowed = StepPermissions.hasLocation(context)
                    // 권한은 있는데 휴대폰 위치 기능이 꺼져 있으면(E07) 시작 전에 묻는다 — 앱 권한 거절(L02)과 다른 원인
                    if (locationAllowed && !locationServicesOn(context)) locationOffGate = true else countingDown = true
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
        if (locationOffGate && !session.isActive) {
            // 위치 설정에서 돌아와 켜져 있으면 바로 3-2-1. 시간만 기록하기는 위치 없이 3-2-1(R02_TIME)
            androidx.lifecycle.compose.LifecycleEventEffect(androidx.lifecycle.Lifecycle.Event.ON_RESUME) {
                if (locationServicesOn(context)) {
                    locationOffGate = false
                    countingDown = true
                }
            }
            val leave = {
                locationOffGate = false
                if (autoStart) onBack()
            }
            androidx.activity.compose.BackHandler { leave() }
            RunLocationOffContent(
                title = planTitle(plan),
                onBack = leave,
                onOpenSettings = { ExternalIntents.openLocationSettings(context) },
                onTimeOnly = {
                    locationOffGate = false
                    countingDown = true
                },
            )
        }
        if (countingDown && !session.isActive) {
            // 권한이 있어도 휴대폰 위치가 꺼져 있으면 위치가 오지 않는다 — 시간만 기록으로 고른 경우 3-2-1 에도 알린다
            val servicesOn = remember { locationServicesOn(context) }
            RunCountdown(
                courseName = course?.name,
                locationAllowed = locationAllowed,
                locationServicesOn = servicesOn,
                title = planTitle(plan),
                onGo = {
                    countingDown = false
                    if (!WalkSessionService.state.value.isActive) WalkSessionService.start(context)
                },
                // 시작 취소 — 이 러닝을 연 화면(시작 메뉴 · 챌린지)으로 돌아간다(시안 R01)
                onCancel = { countingDown = false; onBack() },
            )
        }
    }

    // E03 — 러닝 상세 정보: 시간 · 거리 · 목표와 달성률(수정) · 예상 SUP(확인 중) · 선택한 코스 · 현재 속도 · 내구도
    if (showDetails) {
        val now = remember(session.elapsedSec, session.track.size, session.lastFlaggedAt) { System.currentTimeMillis() }
        val speedKmh = if (running && !session.gpsLost) currentSpeedKmh(session.track, now, session.lastFlaggedAt) else null
        com.stepup.android.ui.components.RunSheet(onDismiss = { showDetails = false }) {
            RunDetailsContent(
                ui = RunDetailsUi(
                    time = formatDuration(session.elapsedSec), km = liveKmText, goalKm = goalKm,
                    goalFraction = if (goalKm > 0) (liveKm / goalKm).toFloat().coerceIn(0f, 1f) else 0f,
                    estimate = "+" + rewardAmount(estimate.points), courseName = course?.name,
                    speed = speedKmh?.let { "%.1f".format(it) } ?: "—",
                    durability = equipped?.let { com.stepup.android.ui.screens.items.durabilityPoints(it).toString() } ?: "—",
                    reasons = buildList {
                        if (session.flaggedSegments > 0) {
                            add(if (session.liveVerdict == RunVerdict.VOID) context.getString(R.string.run_void_body)
                            else context.getString(R.string.run_flagged_body, session.flaggedSegments))
                        }
                        if (earnableSteps <= 0) add(context.getString(R.string.home_energy_empty))
                        if (xpBoosted) add(context.getString(R.string.run_boost_active))
                        if (session.partySize > 1) add(context.getString(R.string.crew_boost, RewardEconomy.partyBonusPercent(session.partySize)))
                    },
                ),
                // 목표 수정은 멈춘 뒤에(E02) — 적용 · 취소 뒤에도 멈춘 채로 둔다
                onEditGoal = {
                    showDetails = false
                    if (session.isActive && !session.isPaused && session.saveStatus == RunSaveStatus.IDLE) WalkSessionService.pause(context)
                    goalEdit = goalKm
                },
                onCourse = {
                    showDetails = false
                    if (course != null && session.isActive) showFullMap = true else onOpenCourses()
                },
                onClose = { showDetails = false },
                extra = {
                    if (BuildConfig.DEBUG) {
                        TextButton(onClick = { viewModel.simulateSteps(100) }) {
                            Text(stringResource(R.string.run_simulate), color = tone.muted)
                        }
                    }
                },
            )
        }
    }

    // E02 — 러닝 목표 수정. 바꾼 값은 적용할 때만 남고, 적용 · 취소 · 닫기 모두 멈춘 채로 돌아간다
    goalEdit?.let { pendingGoal ->
        com.stepup.android.ui.components.RunSheet(onDismiss = { goalEdit = null }) {
            GoalEditContent(
                km = pendingGoal,
                onMinus = { goalEdit = (pendingGoal - 0.5).coerceAtLeast(1.0) },
                onPlus = { goalEdit = (pendingGoal + 0.5).coerceAtMost(42.0) },
                onApply = {
                    viewModel.setGoalKm(pendingGoal)
                    goalEdit = null
                },
                onCancel = { goalEdit = null },
            )
        }
    }

    // L04 — 위치 신호가 약해요. 시간은 계속 기록하고, 끊긴 구간의 거리는 더하지 않는다
    if (gpsLostSheet && session.isActive) {
        com.stepup.android.ui.components.RunSheet(onDismiss = { gpsLostSheet = false }, modifier = Modifier.testTag("run-gps-lost")) {
            GpsLostContent(
                onRetry = {
                    gpsLostSheet = false
                    if (!session.locationOn) ExternalIntents.openLocationSettings(context)
                },
                onTimeOnly = { gpsLostSheet = false },
            )
        }
    }

    // K05 — 코스에서 벗어났어요. K08 — 자유 러닝으로 바꿀까요?(코스 안내만 끈다)
    if (courseOffSheet && session.isActive) {
        com.stepup.android.ui.components.RunSheet(onDismiss = { courseOffSheet = false }) {
            CourseOffContent(
                onMap = { courseOffSheet = false; showFullMap = true },
                onFree = { courseOffSheet = false; courseFreeSheet = true },
            )
        }
    }
    if (courseFreeSheet && session.isActive) {
        com.stepup.android.ui.components.RunSheet(onDismiss = { courseFreeSheet = false }) {
            CourseFreeContent(
                onSwitch = {
                    courseFreeSheet = false
                    viewModel.clearCourse()
                },
                onCancel = { courseFreeSheet = false },
            )
        }
    }

    // CR10 — 함께 달리는 사람(거리를 보이기로 한 사람만 수, 나머지는 비공개)
    if (togetherSheet && together) {
        com.stepup.android.ui.components.RunSheet(onDismiss = { togetherSheet = false }) {
            TogetherSheetContent(
                members = party.members, myKm = liveKm, myPaused = session.isPaused, myShare = party.myShare,
                onShare = { com.stepup.android.core.ServiceLocator.crewRepository.setMyShare(it) },
                onClose = { togetherSheet = false },
            )
        }
    }

    // S01 — 저장이 실패하면 한 번 알린다. 기록은 화면에 남고, 같은 러닝으로 다시 저장하거나 기록을 확인한다
    var saveFailedDialog by rememberSaveable { mutableStateOf(false) }
    LaunchedEffect(session.saveStatus) {
        if (session.saveStatus == RunSaveStatus.FAILED) saveFailedDialog = true
    }
    if (saveFailedDialog && session.saveStatus == RunSaveStatus.FAILED) {
        com.stepup.android.ui.components.RunSheet(onDismiss = { saveFailedDialog = false }, modifier = Modifier.testTag("run-save-failed-dialog")) {
            SaveFailedContent(
                onRetry = {
                    saveFailedDialog = false
                    WalkSessionService.stop(context)
                },
                onReview = { saveFailedDialog = false },
            )
        }
    }

    // R04 — 러닝을 마칠까요? 저장하고 마치기 · 계속 달리기 · 기록 없이 끝내기. 닫으면 멈춘 채로 둔다
    if (confirmStop && canAskEnd) {
        val partyOpen = party.isActive
        com.stepup.android.ui.components.RunSheet(onDismiss = { confirmStop = false }, modifier = Modifier.testTag("run-end-dialog")) {
            FinishSheetContent(
                time = minSecText(session.elapsedSec), km = liveKmText,
                onSave = {
                    confirmStop = false
                    WalkSessionService.stop(context)
                },
                onContinue = {
                    confirmStop = false
                    WalkSessionService.resume(context)
                },
                // 모임 러닝(방에 들어가 있으면 혼자여도)은 저장하고 마친다 — 서비스도 같은 까닭으로 거절한다
                onDiscard = if (WalkSessionService.canDiscard(session) && !partyOpen) {
                    {
                        confirmStop = false
                        discardDialog = true
                    }
                } else null,
                title = stringResource(if (crewRun) R.string.run_crew_finish_title else R.string.run_finish_title),
                note = if (crewRun) stringResource(R.string.run_crew_finish_note) else stringResource(R.string.run_finish_note),
                saveLabel = stringResource(if (crewRun) R.string.run_crew_finish_save else R.string.run_save_finish),
            )
        }
    }

    // R07 — 저장 없이 끝낼까요? 취소 · 바깥 · 뒤로는 마칠까요(R04)로 돌아간다(러닝은 멈춘 채)
    if (discardDialog && canAskEnd && !discarding) {
        com.stepup.android.ui.components.RunSheet(
            onDismiss = { discardDialog = false; confirmStop = true }, modifier = Modifier.testTag("run-discard-dialog"),
        ) {
            DiscardSheetContent(
                time = minSecText(session.elapsedSec), km = liveKmText,
                onDiscard = {
                    discardDialog = false
                    // "코스 만들기"로 시작한 러닝이면 녹화도 그만둔다 — 다음 러닝이 녹화로 이어지지 않게
                    if (recordingCourse) viewModel.cancelRecording()
                    discarding = true
                    WalkSessionService.discard(context)
                },
                onCancel = { discardDialog = false; confirmStop = true },
            )
        }
    }

    // C01 — 목표를 달성했어요. 계속 달리면 기록은 이어진다(달성 안내는 한 번만)
    if (showGoalReached && session.isActive && session.saveStatus == RunSaveStatus.IDLE && goal != null) {
        com.stepup.android.ui.components.RunDialog(onDismiss = { showGoalReached = false }, modifier = Modifier.testTag("run-goal-reached")) {
            GoalReachedContent(
                goalShort = goalShort(goal),
                onSave = {
                    showGoalReached = false
                    WalkSessionService.stop(context)
                },
                onMore = { showGoalReached = false },
            )
        }
    }

    // R06 — 저장한 이 기록을 지울까요? 실패하면 결과에 남기고 까닭을 적는다(H16)
    if (deleteSheet && finishing) {
        val scope = androidx.compose.runtime.rememberCoroutineScope()
        val subject = stringResource(
            R.string.run_delete_subject,
            java.time.Instant.ofEpochMilli(session.lastStartedAt).atZone(java.time.ZoneId.systemDefault()).toLocalDate()
                .format(java.time.format.DateTimeFormatter.ofPattern(stringResource(R.string.date_month_day),
                    androidx.compose.ui.platform.LocalConfiguration.current.locales[0])),
            "%.2f".format(finishKm(session)), minSecText(session.lastElapsedSec),
        )
        val uploadingText = stringResource(R.string.run_delete_uploading)
        val keptText = stringResource(R.string.run_delete_kept)
        com.stepup.android.ui.components.RunSheet(
            onDismiss = { if (!deleting) deleteSheet = false }, dismissible = !deleting, modifier = Modifier.testTag("run-delete-sheet"),
        ) {
            DeleteSheetContent(
                subject = subject, busy = deleting,
                onDelete = {
                    deleting = true
                    scope.launch {
                        val records = com.stepup.android.core.ServiceLocator.runRecordsRepository
                        val outcome = runCatching {
                            records.idForStart(session.lastStartedAt)?.let { records.delete(it) }
                                ?: com.stepup.android.data.repo.RecordDeletion.Missing
                        }.getOrNull()
                        // 지우기는 다른 스레드에서 끝날 수 있다 — 화면 상태 · 기록 화면 이동은 메인에서
                        kotlinx.coroutines.withContext(Dispatchers.Main.immediate) {
                            deleting = false
                            deleteSheet = false
                            when (outcome) {
                                com.stepup.android.data.repo.RecordDeletion.Deleted,
                                com.stepup.android.data.repo.RecordDeletion.Missing -> {
                                    viewModel.clearReward()
                                    onOpenRecords()
                                }
                                com.stepup.android.data.repo.RecordDeletion.Uploading -> deleteFailed = uploadingText
                                null -> deleteFailed = keptText
                            }
                        }
                    }
                },
                onCancel = { deleteSheet = false },
            )
        }
    }

    // 지급 제외 까닭(E08 자세히 보기) — 저장된 판정의 설명
    if (voidReason && finishing) {
        com.stepup.android.ui.components.RunSheet(onDismiss = { voidReason = false }) {
            com.stepup.android.ui.components.RunSheetText(stringResource(R.string.run_reward_void), body = stringResource(R.string.run_void_body))
            Spacer(Modifier.height(20.dp))
            com.stepup.android.ui.components.RunButton(stringResource(R.string.run_close), { voidReason = false })
        }
    }

    if (readyToSaveCourse) {
        SaveCourseDialog(
            track = recordedTrack,
            elapsedSec = session.lastElapsedSec,
            onSave = viewModel::saveRecordedCourse,
            onDismiss = viewModel::cancelRecording,
        )
    }
}

/** 코스에서 벗어났다고 볼 거리(m)와 시간(ms) — 화면 안내용(서버 판정과 별개) */
private const val OFF_COURSE_METERS = 60.0
private const val OFF_COURSE_MS = 15_000L

/** "12분 30초" — 마칠까요 · 기록 없이 끝낼까요 · 삭제 확인의 한 줄 */
@Composable
private fun minSecText(totalSec: Long): String =
    stringResource(R.string.run_min_sec, (totalSec / 60).toInt(), (totalSec % 60).toInt())

/** 챌린지 짧은 이름 — "10분" · "1km" · "3km" */
@Composable
private fun goalShort(goal: com.stepup.android.domain.RunGoal): String = stringResource(when (goal) {
    com.stepup.android.domain.RunGoal.TEN_MIN -> R.string.run_short_10min
    com.stepup.android.domain.RunGoal.ONE_KM -> R.string.run_short_1km
    com.stepup.android.domain.RunGoal.THREE_KM -> R.string.run_short_3km
})

/** 챌린지 화면 이름 — "10분 챌린지" */
@Composable
internal fun challengeTitle(goal: com.stepup.android.domain.RunGoal): String = stringResource(when (goal) {
    com.stepup.android.domain.RunGoal.TEN_MIN -> R.string.run_challenge_title_10
    com.stepup.android.domain.RunGoal.ONE_KM -> R.string.run_challenge_title_1k
    com.stepup.android.domain.RunGoal.THREE_KM -> R.string.run_challenge_title_3k
})

/**
 * 러닝 중 화면의 내용 — 계획(자유 · 챌린지 · 코스 · 다이어트 · 크루)과 지금 값으로 제목 · 상태 · 큰 수 · 막대를 정한다.
 */
@Composable
private fun liveRunUi(
    plan: RunPlan,
    diet: DietRoutine?,
    dietPos: DietRoutine.Position?,
    goal: com.stepup.android.domain.RunGoal?,
    goalReached: Boolean,
    elapsedSec: Long,
    km: Double,
    paused: Boolean,
    timeOnly: Boolean,
    gps: GpsBadge,
    distance: RunStat,
    pace: RunStat,
    courseName: String?,
    courseKm: Double?,
    crewName: String?,
    active: Boolean,
    /** 마칠지 묻는 중(R04 · R07) — 뒤 화면은 이번 러닝 이름 · 왼쪽 "일시정지" 알약 그대로 */
    asking: Boolean = false,
): LiveRunUi {
    val pauseLabel = stringResource(R.string.run_pause)
    val resumeLabel = stringResource(R.string.run_resume)
    val primaryLabel = when {
        !active -> stringResource(R.string.home_start_run)
        paused -> resumeLabel
        else -> pauseLabel
    }
    val primaryIcon = if (active && !paused) Icons.Filled.Pause else Icons.Filled.PlayArrow
    val finish = stringResource(if (crewName != null) R.string.run_finish_mine else R.string.run_finish_run)
    val status = when {
        paused && asking -> stringResource(R.string.run_paused_title)
        paused -> stringResource(R.string.run_status_paused)
        timeOnly -> stringResource(R.string.run_status_no_gps)
        goal != null -> stringResource(R.string.run_status_challenge)
        courseName != null -> stringResource(R.string.run_status_course)
        else -> stringResource(R.string.run_status_recording)
    }
    val time = formatDuration(elapsedSec)
    val timeLabel = stringResource(R.string.run_label_time)
    return when {
        diet != null && dietPos != null -> {
            val seg = dietPos.segment
            val upcoming = diet.next(dietPos.index)
            val kindWord = stringResource(if (seg.kind == DietSegmentKind.RUN) R.string.run_diet_kind_run else R.string.run_diet_kind_walk)
            LiveRunUi(
                title = stringResource(R.string.runflow_diet),
                subtitle = if (paused) stringResource(R.string.run_paused_title) else when (seg.kind) {
                    DietSegmentKind.WARMUP -> stringResource(R.string.run_diet_warmup_part)
                    DietSegmentKind.COOLDOWN -> stringResource(R.string.run_diet_last_part)
                    DietSegmentKind.RUN -> stringResource(R.string.run_diet_round_run, seg.round, diet.rounds)
                    DietSegmentKind.WALK -> stringResource(R.string.run_diet_round_walk, seg.round, diet.rounds)
                },
                paused = paused, status = status, gps = gps,
                hero = LiveHero.Clock(
                    label = if (paused) stringResource(R.string.run_diet_paused_left, kindWord) else stringResource(when (seg.kind) {
                        DietSegmentKind.WARMUP -> R.string.run_diet_seg_warmup
                        DietSegmentKind.RUN -> R.string.run_diet_seg_run
                        DietSegmentKind.WALK -> R.string.run_diet_seg_walk
                        DietSegmentKind.COOLDOWN -> R.string.run_diet_seg_cooldown
                    }),
                    value = formatDuration(dietPos.remainingSec),
                    caption = when {
                        // D12 — 어느 구간에서 멈췄는지와, 다시 시작하면 그 구간부터 이어진다는 것
                        paused -> listOf(
                            when (seg.kind) {
                                DietSegmentKind.WARMUP -> stringResource(R.string.run_diet_warmup_part)
                                DietSegmentKind.COOLDOWN -> stringResource(R.string.run_diet_last_part)
                                DietSegmentKind.RUN -> stringResource(R.string.run_diet_paused_run_part, seg.round, diet.rounds)
                                DietSegmentKind.WALK -> stringResource(R.string.run_diet_paused_walk_part, seg.round, diet.rounds)
                            },
                            stringResource(R.string.run_diet_paused_note),
                        ).joinToString("\n")
                        seg.kind == DietSegmentKind.RUN -> stringResource(R.string.run_diet_run_hint)
                        else -> stringResource(R.string.run_diet_left)
                    },
                    accentLabel = true,
                ),
                stats = listOf(
                    RunStat(stringResource(R.string.run_diet_total), time, "/ " + formatDuration(diet.totalSec)),
                    RunStat(stringResource(R.string.run_diet_moved), distance.value, "km", caption = distance.caption),
                ),
                meter = (elapsedSec.toFloat() / diet.totalSec).coerceIn(0f, 1f),
                next = if (paused) null else upcoming?.let {
                    stringResource(R.string.run_diet_next_value, stringResource(R.string.run_diet_min, (it.seconds / 60).toInt()),
                        stringResource(if (it.kind == DietSegmentKind.RUN) R.string.run_diet_kind_run else R.string.run_diet_kind_walk))
                } ?: stringResource(R.string.run_diet_next_finish),
                primaryLabel = primaryLabel, primaryIcon = primaryIcon, finishLabel = finish, dietLayout = true,
                // 구간 이름이 상태 알약 자리를 맡는다(시안 D07~D12) — 위치 문제는 아래 안내 띠로 알린다
                showStatus = false,
            )
        }
        goal != null && goal.isDistance -> {
            val target = goal.km ?: 1.0
            val left = (target - km).coerceAtLeast(0.0)
            LiveRunUi(
                title = challengeTitle(goal), subtitle = if (paused) stringResource(R.string.run_paused_title) else null,
                paused = paused, status = status, gps = gps,
                hero = LiveHero.Distance(stringResource(R.string.run_label_run_distance), "%.2f".format(km),
                    "km " + stringResource(R.string.run_goal_of, "%.0fkm".format(target))),
                stats = listOf(RunStat(stringResource(R.string.run_label_elapsed), time), pace),
                meter = goal.fraction(elapsedSec, km), meterInside = "${(goal.fraction(elapsedSec, km) * 100).toInt()}%",
                meterCaption = if (goalReached) stringResource(R.string.runflow_goal_reached_title)
                    else if (left < 1.0) stringResource(R.string.run_goal_left_m, (left * 1000).toInt())
                    else stringResource(R.string.run_goal_left_km, "%.2f".format(left)),
                primaryLabel = primaryLabel, primaryIcon = primaryIcon, finishLabel = finish,
            )
        }
        goal != null -> {
            val left = ((goal.seconds ?: 0L) - elapsedSec).coerceAtLeast(0L)
            LiveRunUi(
                title = challengeTitle(goal), subtitle = if (paused) stringResource(R.string.run_paused_title) else null,
                paused = paused, status = status, gps = gps,
                hero = LiveHero.Clock(stringResource(R.string.run_label_elapsed), time),
                stats = listOf(distance, pace),
                meter = goal.fraction(elapsedSec, km), meterInside = "${(goal.fraction(elapsedSec, km) * 100).toInt()}%",
                meterCaption = if (goalReached) stringResource(R.string.runflow_goal_reached_title)
                    else stringResource(R.string.run_goal_left_time, formatDurationWords(left)),
                primaryLabel = primaryLabel, primaryIcon = primaryIcon, finishLabel = finish,
            )
        }
        crewName != null -> LiveRunUi(
            title = stringResource(if (paused) R.string.run_my_pause_title else R.string.run_crew_title),
            subtitle = crewName.ifBlank { null },
            paused = paused, status = status, gps = gps,
            hero = LiveHero.Clock(timeLabel, time),
            stats = listOf(distance, pace),
            primaryLabel = primaryLabel, primaryIcon = primaryIcon, finishLabel = finish,
        )
        courseName != null -> LiveRunUi(
            title = stringResource(R.string.run_course_title), subtitle = courseName,
            paused = paused, status = status, gps = gps,
            hero = LiveHero.Clock(timeLabel, time, caption = if (paused) stringResource(R.string.run_paused_caption) else null),
            stats = listOf(distance, pace),
            meter = courseKm?.takeIf { it > 0 }?.let { (km / it).toFloat().coerceIn(0f, 1f) },
            meterCaption = courseKm?.let { "%.2f / %.2f km".format(km, it) },
            primaryLabel = primaryLabel, primaryIcon = primaryIcon, finishLabel = finish,
        )
        else -> LiveRunUi(
            title = if (paused && !asking) stringResource(R.string.run_paused_title) else planTitle(plan),
            paused = paused, status = status, gps = gps,
            hero = LiveHero.Clock(timeLabel, time, caption = if (paused && !asking) stringResource(R.string.run_paused_caption) else null),
            stats = listOf(distance, pace),
            primaryLabel = primaryLabel, primaryIcon = primaryIcon, finishLabel = finish,
        )
    }.let { if (asking) it.copy(pillCentered = false) else it }
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
 * 위치를 모를 때의 지도 자리 — 안내 한 줄만. 경로처럼 보이는 선 · 가짜 지도는 그리지 않는다.
 * GPS 가 잡히면 [LiveRouteMap] 이 이 자리를 실제 위치와 경로로 바꾼다.
 */
@Composable
private fun MapWaiting(modifier: Modifier = Modifier) {
    Box(modifier.testTag("run-map-waiting")) {
        com.stepup.android.ui.components.RunMapPlaceholder(
            Icons.Filled.GpsFixed, stringResource(R.string.map_waiting_title), stringResource(R.string.map_waiting_body),
        )
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
 * 러닝이 끝나고 뜨는 코스 저장(시안 K15) — 지도 · 거리 | 달린 시간 · 코스 이름 · 지역 · 코스 공유하기.
 *
 * 여기서 저장해야 방금 뛴 길이 코스가 된다. 닫으면 녹화를 그만둔다 — 그래서 바깥을 눌러 닫히지 않고, 아래 버튼에도
 * "저장 안 함"이라고 적는다. "취소"라고만 적으면 나중에 저장할 수 있다고 읽힌다.
 */
@Composable
private fun SaveCourseDialog(
    track: List<GeoPoint>,
    elapsedSec: Long,
    onSave: (name: String, area: String, shared: Boolean) -> Unit,
    onDismiss: () -> Unit,
) {
    val t = runTone()
    var name by rememberSaveable { mutableStateOf("") }
    var area by rememberSaveable { mutableStateOf("") }
    var share by rememberSaveable { mutableStateOf(true) }
    val km = remember(track) { track.trackDistanceKm() }
    com.stepup.android.ui.components.RunSheet(onDismiss = onDismiss, dismissible = false, modifier = Modifier.testTag("course-save-sheet")) {
        Column(Modifier.fillMaxWidth().heightIn(max = 620.dp).verticalScroll(rememberScrollState())) {
            Text(stringResource(R.string.run_course_save_title),
                style = com.stepup.android.ui.components.runTextStyle(24.sp, t.text, FontWeight.ExtraBold))
            Spacer(Modifier.height(12.dp))
            com.stepup.android.ui.components.RunMapFrame(Modifier.fillMaxWidth().height(170.dp)) {
                LiveRouteMap(points = track, modifier = Modifier.fillMaxSize(), routeColor = t.cyan)
            }
            Spacer(Modifier.height(12.dp))
            com.stepup.android.ui.components.RunStatRow(
                listOf(
                    RunStat(stringResource(R.string.run_label_distance), "%.2f".format(km), "km", tag = "course-save-km"),
                    RunStat(stringResource(R.string.run_label_time), formatDuration(elapsedSec)),
                ),
                valueSize = 32.sp,
            )
            Spacer(Modifier.height(12.dp))
            com.stepup.android.ui.components.RunTextField(
                name, { name = it }, label = stringResource(R.string.run_course_field_name),
                clearLabel = stringResource(R.string.run_course_clear_text), fieldTag = "course-save-name",
            )
            Spacer(Modifier.height(10.dp))
            com.stepup.android.ui.components.RunTextField(
                area, { area = it }, label = stringResource(R.string.run_course_field_area),
                clearLabel = stringResource(R.string.run_course_clear_text), fieldTag = "course-save-area",
            )
            Spacer(Modifier.height(12.dp))
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text(stringResource(R.string.run_course_save_share),
                        style = com.stepup.android.ui.components.runTextStyle(15.sp, t.text, FontWeight.Bold))
                    Text(stringResource(R.string.run_course_save_share_desc),
                        style = com.stepup.android.ui.components.runTextStyle(12.sp, t.label))
                }
                Switch(
                    checked = share, onCheckedChange = { share = it },
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = Color.White, checkedTrackColor = t.cobalt,
                        uncheckedThumbColor = Color.White, uncheckedTrackColor = t.track, uncheckedBorderColor = t.panelEdge,
                    ),
                    modifier = Modifier.testTag("course-save-share"),
                )
            }
        }
        Spacer(Modifier.height(16.dp))
        com.stepup.android.ui.components.RunButton(
            stringResource(R.string.run_course_save_do), { onSave(name, area, share) }, enabled = name.isNotBlank(),
            modifier = Modifier.testTag("course-save-do"),
        )
        Spacer(Modifier.height(10.dp))
        com.stepup.android.ui.components.RunButton(
            stringResource(R.string.course_save_skip), onDismiss, kind = com.stepup.android.ui.components.RunButtonKind.Secondary,
            modifier = Modifier.testTag("course-save-skip"),
        )
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
 * 결과 이미지 저장(E05 "이미지 저장") — 공유 미리보기에 보이는 바로 그 그림을 사진(Pictures/StepUp)에 넣는다.
 * Android 9 이하는 저장 권한을 먼저 묻는다(10 부터는 권한 없이 사진에 넣을 수 있다).
 */
@Composable
private fun rememberCardSave(card: android.graphics.Bitmap?, name: String): () -> Unit {
    val context = LocalContext.current
    val scope = androidx.compose.runtime.rememberCoroutineScope()
    val savedText = stringResource(R.string.result_saved)
    val failedText = stringResource(R.string.result_save_failed)
    val save: () -> Unit = {
        val bitmap = card
        scope.launch {
            val saved = bitmap != null && kotlinx.coroutines.withContext(Dispatchers.IO) {
                runCatching { RunImageStore.save(context, bitmap, name) }.getOrDefault(false)
            }
            kotlinx.coroutines.withContext(Dispatchers.Main.immediate) {
                android.widget.Toast.makeText(context, if (saved) savedText else failedText, android.widget.Toast.LENGTH_SHORT).show()
            }
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

/**
 * E05 — 러닝 기록 공유 미리보기. 내보낼 그림을 그대로 보이고, "경로 포함"을 끄면(기본 끔) 그림에서 달린 길 · 출발 · 도착이
 * 빠진다 — 미리보기 · 공유 · 이미지 저장이 같은 그림이다. 사용자가 "공유하기"를 눌러야 시스템 공유 창이 열린다.
 * SUP 는 그림에 넣지 않는다(서버 확인 전 금액이 밖으로 나가지 않게). 글에는 서버가 확인한 금액만 적는다.
 */
/** 크루에 기록 공유(CR21) — 이 러닝의 거리 · 시간 · 페이스와(켜면) 공유 카드 그림 */
@Composable
private fun CrewShareFor(session: WalkSessionState, crewId: String, crewName: String, onClose: () -> Unit) {
    val context = LocalContext.current
    val km = finishKm(session)
    val elapsed = formatDuration(session.lastElapsedSec)
    val pace = finishPace(session)?.let { formatPace(it) } ?: "—"
    val labels = RunShareCard.Labels(
        distance = stringResource(R.string.share_card_distance),
        time = stringResource(R.string.share_card_time),
        pace = stringResource(R.string.share_card_pace),
        footer = stringResource(R.string.share_card_footer),
    )
    CrewShareScreen(
        crewId = crewId, crewName = crewName.ifBlank { stringResource(R.string.run_crew_title) },
        km = "%.2f".format(km), time = elapsed, pace = pace,
        card = { route ->
            kotlinx.coroutines.withContext(Dispatchers.Default) {
                runCatching {
                    RunShareCard.render(context, km, elapsed, pace, if (route) session.geoTrack else emptyList(), labels)
                }.getOrNull()
            }
        },
        hasRoute = session.geoTrack.size >= 2,
        onClose = onClose,
    )
}

@Composable
private fun RunShareScreen(session: WalkSessionState, points: Double?, upload: String?, onClose: () -> Unit) {
    val context = LocalContext.current
    var includeRoute by rememberSaveable { mutableStateOf(false) }
    val km = finishKm(session)
    val elapsed = formatDuration(session.lastElapsedSec)
    val pace = finishPace(session)?.let { formatPace(it) } ?: "—"
    val labels = RunShareCard.Labels(
        distance = stringResource(R.string.share_card_distance),
        time = stringResource(R.string.share_card_time),
        pace = stringResource(R.string.share_card_pace),
        footer = stringResource(R.string.share_card_footer),
    )
    val shareText = if (finishConfirmed(session, points, upload) && points != null) {
        stringResource(R.string.finish_share_text, "%.1f".format(km), elapsed, com.stepup.android.ui.components.formatSupDown(points))
    } else {
        stringResource(R.string.finish_share_activity, "%.1f".format(km), elapsed)
    }
    val datePattern = stringResource(R.string.result_date_pattern)
    val locale = androidx.compose.ui.platform.LocalConfiguration.current.locales[0]
    val date = remember(session.lastStartedAt, datePattern, locale) {
        if (session.lastStartedAt > 0L) {
            runCatching { java.text.SimpleDateFormat(datePattern, locale).format(java.util.Date(session.lastStartedAt)) }.getOrNull()
        } else null
    }
    // 경로를 끄면 빈 경로로 그린다 — 지도 · 출발 · 도착이 그림에 남지 않는다
    var card by remember { mutableStateOf<android.graphics.Bitmap?>(null) }
    LaunchedEffect(includeRoute, session.lastStartedAt) {
        card = null
        val track = if (includeRoute) session.geoTrack else emptyList()
        card = kotlinx.coroutines.withContext(Dispatchers.Default) {
            runCatching { RunShareCard.render(context, km, elapsed, pace, track, labels, date) }.getOrNull()
        }
    }
    val preview = remember(card) { card?.asImageBitmap() }
    val save = rememberCardSave(card, "StepUp-run-${session.lastStartedAt}")
    RunSharePreviewContent(
        title = stringResource(R.string.run_share_title),
        subtitle = stringResource(R.string.run_share_sub),
        preview = preview,
        includeRoute = includeRoute,
        onRouteChange = { includeRoute = it },
        onShare = { card?.let { RunShareCard.share(context, it, shareText, null) } },
        onCancel = onClose,
        shareLabel = stringResource(R.string.run_share),
        onSaveImage = save,
    )
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
