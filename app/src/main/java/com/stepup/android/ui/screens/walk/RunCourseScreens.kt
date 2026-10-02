package com.stepup.android.ui.screens.walk

import android.Manifest
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.LocationOff
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.stepup.android.R
import com.stepup.android.core.ExternalIntents
import com.stepup.android.core.ServiceLocator
import com.stepup.android.domain.CourseRecommendations
import com.stepup.android.domain.DemoCourses
import com.stepup.android.domain.GeoPoint
import com.stepup.android.domain.RunCourse
import com.stepup.android.ui.StepPermissions
import com.stepup.android.ui.components.LiveRouteMap
import com.stepup.android.ui.components.RunButton
import com.stepup.android.ui.components.RunButtonKind
import com.stepup.android.ui.components.RunCard
import com.stepup.android.ui.components.RunMapFrame
import com.stepup.android.ui.components.RunNumber
import com.stepup.android.ui.components.RunPage
import com.stepup.android.ui.components.RunSpinner
import com.stepup.android.ui.components.RunStateArt
import com.stepup.android.ui.components.runTextStyle
import com.stepup.android.ui.components.runTone
import kotlinx.coroutines.delay

/** 추천 코스 화면의 상태 — 찾는 중(K01) · 찾음(U04 · K02) · 없음(K03) · 위치 권한 없음 */
internal sealed interface CourseRecUi {
    data object Finding : CourseRecUi
    data class Found(val pick: CourseRecommendations.Pick, val minutes: Int, val order: Int, val count: Int) : CourseRecUi
    data object None : CourseRecUi
    data object NoLocation : CourseRecUi
}

/** 앱이 심어 둔 체험 코스(폰에만 있는 StepUp 공원 고리) — 추천에 넣지 않는다 */
private fun RunCourse.isSeededDemo(): Boolean = !mine && id < RunCourse.SERVER_ID_BASE && author == DemoCourses.AUTHOR

/** 지금 자리를 기다리는 최대 시간 — 넘으면 "찾지 못했어요"(K03)로 바꾸고 다시 찾게 한다 */
private const val FIND_TIMEOUT_MS = 15_000L

/**
 * 추천 코스(시안 U04 · K01 · K02 · K03) — 저장한 코스와 코스 게시판 중 지금 자리에서 가까운 코스를 하나씩 보인다.
 * 추천 서버가 없어 거리 순이다(남은 연동). 고른 코스로 시작하면 그 코스를 선택해 러닝(3-2-1)으로 간다.
 */
@Composable
fun CourseRecommendScreen(
    onBack: () -> Unit,
    onStart: (RunCourse) -> Unit,
    onFreeRun: () -> Unit,
) {
    val context = LocalContext.current
    val repo = ServiceLocator.courseRepository
    var locationAllowed by remember { mutableStateOf(StepPermissions.hasLocation(context)) }
    androidx.lifecycle.compose.LifecycleResumeEffect(Unit) {
        locationAllowed = StepPermissions.hasLocation(context)
        onPauseOrDispose { }
    }
    var askedOnce by rememberSaveable { mutableStateOf(false) }
    val permission = rememberLauncherForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) {
        locationAllowed = StepPermissions.hasLocation(context)
        askedOnce = true
    }
    var attempt by rememberSaveable { mutableIntStateOf(0) }
    var index by rememberSaveable { mutableIntStateOf(0) }
    // 다시 찾아보기(K03)는 위치를 새로 받는다
    val here = key(attempt) { com.stepup.android.ui.components.rememberCurrentLocation(enabled = locationAllowed) }
    val local by repo.courses.collectAsStateWithLifecycle(emptyList())
    val board by repo.board.collectAsStateWithLifecycle(emptyList())
    var boardDone by remember(attempt) { mutableStateOf(false) }
    LaunchedEffect(attempt) {
        runCatching { repo.refreshBoard() }
        boardDone = true
    }
    var waited by remember(attempt) { mutableStateOf(false) }
    LaunchedEffect(attempt) {
        delay(FIND_TIMEOUT_MS)
        waited = true
    }
    val picks = remember(here, local, board) {
        val at = here ?: return@remember emptyList()
        CourseRecommendations.near(at, board + local.filter { it.mine }, exclude = { it.isSeededDemo() })
    }
    val ui = when {
        !locationAllowed -> CourseRecUi.NoLocation
        picks.isNotEmpty() -> {
            val order = index % picks.size
            val pick = picks[order]
            CourseRecUi.Found(pick, CourseRecommendations.minutes(pick.course.distanceKm), order + 1, picks.size)
        }
        (here == null || !boardDone) && !waited -> CourseRecUi.Finding
        else -> CourseRecUi.None
    }
    CourseRecommendContent(
        ui = ui,
        here = here,
        onBack = onBack,
        onStart = { (ui as? CourseRecUi.Found)?.let { onStart(it.pick.course) } },
        onNext = { index += 1 },
        onRetry = {
            index = 0
            attempt += 1
        },
        onFreeRun = onFreeRun,
        onAllow = {
            // 한 번 거절하면 시스템 창이 다시 뜨지 않을 수 있다 — 그때는 앱 설정으로
            if (askedOnce) ExternalIntents.openAppSettings(context)
            else permission.launch(arrayOf(Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION))
        },
    )
}

/** 추천 코스 — 상태 없이 그린다 */
@Composable
internal fun CourseRecommendContent(
    ui: CourseRecUi,
    here: GeoPoint?,
    onBack: () -> Unit,
    onStart: () -> Unit,
    onNext: () -> Unit,
    onRetry: () -> Unit,
    onFreeRun: () -> Unit,
    onAllow: () -> Unit,
) {
    val t = runTone()
    val found = ui as? CourseRecUi.Found
    RunPage(
        onBack = onBack,
        modifier = Modifier.testTag("run-course-rec"),
        title = stringResource(R.string.run_course_rec_title),
        subtitle = if (ui is CourseRecUi.Finding) null else stringResource(R.string.run_course_rec_sub),
        bottom = {
            when (ui) {
                is CourseRecUi.Found -> {
                    RunButton(stringResource(R.string.run_course_start), onStart, hero = true, modifier = Modifier.testTag("run-course-start"))
                    RunButton(stringResource(R.string.run_course_next), onNext, kind = RunButtonKind.Secondary,
                        enabled = ui.count > 1, modifier = Modifier.testTag("run-course-next"))
                }
                CourseRecUi.Finding -> RunButton(stringResource(R.string.run_cancel), onBack, kind = RunButtonKind.Secondary,
                    modifier = Modifier.testTag("run-course-cancel"))
                CourseRecUi.None -> {
                    RunButton(stringResource(R.string.run_course_retry), onRetry, hero = true, modifier = Modifier.testTag("run-course-retry"))
                    RunButton(stringResource(R.string.run_course_free), onFreeRun, kind = RunButtonKind.Secondary,
                        modifier = Modifier.testTag("run-course-free"))
                }
                CourseRecUi.NoLocation -> {
                    RunButton(stringResource(R.string.run_course_allow), onAllow, modifier = Modifier.testTag("run-course-allow"))
                    RunButton(stringResource(R.string.run_course_free), onFreeRun, kind = RunButtonKind.Secondary,
                        modifier = Modifier.testTag("run-course-free"))
                }
            }
        },
    ) {
        when (ui) {
            CourseRecUi.Finding -> {
                Column(Modifier.fillMaxWidth().padding(top = 8.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                    RunSpinner(Modifier.size(84.dp).testTag("run-course-finding"))
                    Spacer(Modifier.height(18.dp))
                    Text(stringResource(R.string.run_course_finding), style = runTextStyle(24.sp, t.text, FontWeight.ExtraBold),
                        textAlign = TextAlign.Center)
                    Text(stringResource(R.string.run_course_finding_body), style = runTextStyle(15.sp, t.label, FontWeight.Medium),
                        textAlign = TextAlign.Center, modifier = Modifier.padding(top = 6.dp))
                }
                Spacer(Modifier.height(20.dp))
                RunMapFrame(Modifier.fillMaxWidth().height(300.dp)) { HereMap(here) }
            }
            is CourseRecUi.Found -> {
                val course = ui.pick.course
                RunMapFrame(Modifier.fillMaxWidth().height(320.dp).testTag("run-course-map")) {
                    LiveRouteMap(points = course.points, modifier = Modifier.fillMaxSize(), routeColor = t.cyan)
                }
                Spacer(Modifier.height(12.dp))
                RunCard(padding = PaddingValues(horizontal = 18.dp, vertical = 14.dp), tag = "run-course-card") {
                    Text(stringResource(R.string.run_course_name_label), style = runTextStyle(13.sp, t.label, FontWeight.Medium))
                    Text(course.name, style = runTextStyle(22.sp, t.text, FontWeight.ExtraBold), modifier = Modifier.testTag("run-course-name"))
                    Text(
                        listOfNotNull(
                            course.area.takeIf { it.isNotBlank() },
                            stringResource(R.string.run_course_from_here, CourseRecommendations.distanceLabel(ui.pick.toCourseMeters)),
                        ).joinToString(" · "),
                        style = runTextStyle(14.sp, t.label, FontWeight.Medium),
                    )
                    Spacer(Modifier.height(12.dp))
                    Box(Modifier.fillMaxWidth().height(1.dp).padding(horizontal = 0.dp)) {
                        androidx.compose.foundation.Canvas(Modifier.fillMaxSize()) { drawRect(t.divider) }
                    }
                    Spacer(Modifier.height(12.dp))
                    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.Bottom) {
                        Column(Modifier.weight(1f)) {
                            Text(stringResource(R.string.run_label_distance), style = runTextStyle(13.sp, t.label, FontWeight.Medium))
                            RunNumber("%.1f".format(course.distanceKm), unit = "km", size = 40.sp, valueTag = "run-course-km")
                        }
                        Column(Modifier.weight(1f)) {
                            Text(stringResource(R.string.run_course_eta), style = runTextStyle(13.sp, t.label, FontWeight.Medium))
                            Row(verticalAlignment = Alignment.Bottom) {
                                Text(stringResource(R.string.run_course_eta_about), style = runTextStyle(18.sp, t.text, FontWeight.Bold),
                                    modifier = Modifier.padding(bottom = 6.dp))
                                Spacer(Modifier.width(6.dp))
                                RunNumber("${ui.minutes}", unit = stringResource(R.string.run_unit_min), size = 40.sp,
                                    valueTag = "run-course-minutes")
                            }
                            Text(stringResource(R.string.run_course_eta_note), style = runTextStyle(12.sp, t.muted))
                        }
                    }
                }
                Spacer(Modifier.height(8.dp))
                Text(stringResource(R.string.run_course_rec_note), style = runTextStyle(12.sp, t.muted), modifier = Modifier.testTag("run-course-note"))
            }
            CourseRecUi.None, CourseRecUi.NoLocation -> {
                val none = ui == CourseRecUi.None
                RunMapFrame(Modifier.fillMaxWidth().height(420.dp).testTag(if (none) "run-course-none" else "run-course-no-location")) {
                    if (none) HereMap(here)
                    Column(
                        Modifier.fillMaxSize().padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center,
                    ) {
                        RunStateArt(if (none) Icons.Outlined.Search else Icons.Outlined.LocationOff, size = 88.dp)
                        Spacer(Modifier.height(14.dp))
                        Text(
                            stringResource(if (none) R.string.run_course_none_title else R.string.run_course_no_location_title),
                            style = runTextStyle(21.sp, t.text, FontWeight.ExtraBold), textAlign = TextAlign.Center,
                        )
                        Text(
                            stringResource(if (none) R.string.run_course_none_body else R.string.run_course_no_location_body),
                            style = runTextStyle(15.sp, t.label, FontWeight.Medium), textAlign = TextAlign.Center,
                            modifier = Modifier.padding(top = 6.dp),
                        )
                    }
                }
            }
        }
    }
}

/** 지금 자리 지도 — 자리를 모르면 비워 둔다(꾸민 지도를 그리지 않는다) */
@Composable
private fun BoxScope.HereMap(here: GeoPoint?) {
    if (here != null) {
        LiveRouteMap(points = listOf(here), modifier = Modifier.fillMaxSize(), follow = true, live = true, routeColor = runTone().cyan)
        Box(Modifier.fillMaxSize().padding(0.dp)) {
            androidx.compose.foundation.Canvas(Modifier.fillMaxSize()) { drawRect(androidx.compose.ui.graphics.Color.Black.copy(alpha = 0.25f)) }
        }
    }
}
