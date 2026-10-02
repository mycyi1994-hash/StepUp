package com.stepup.android.ui.screens.records

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.stepup.android.R
import com.stepup.android.data.local.WalkSessionEntity
import com.stepup.android.domain.GeoPoint
import com.stepup.android.domain.localDate
import com.stepup.android.ui.components.LiveRouteMap
import com.stepup.android.ui.components.MapControlLabels
import com.stepup.android.ui.components.RunButton
import com.stepup.android.ui.components.RunCard
import com.stepup.android.ui.components.RunMapFrame
import com.stepup.android.ui.components.RunNumber
import com.stepup.android.ui.components.RunSpec
import com.stepup.android.ui.components.RunSpinner
import com.stepup.android.ui.components.runTextStyle
import com.stepup.android.ui.components.runTone
import com.stepup.android.ui.theme.StepUpSans
import java.time.ZoneId

/**
 * 저장된 경로(시안 H12) — 상세에서 고른 그 러닝의 저장 좌표만, 실제 지도(타일 · 출처 표기 그대로) 위에. 처음엔 경로 전체가
 * 보이게 맞추고, 손가락 · +/− 로 확대 · 축소 · 이동한다(지도만 바뀐다). 출발 · 도착 점에 작은 이름표, 요약은 지도 아래.
 * 지난 경로를 보는 것이라 현재 위치 권한을 묻지 않고 기록을 고치지 않는다.
 */
@Composable
fun RunRouteMapScreen(
    id: Long,
    onBack: () -> Unit = {},
    viewModel: RunRecordViewModel = viewModel(key = "run-map-$id", factory = RunRecordViewModel.factory(id)),
) {
    val record by viewModel.record.collectAsStateWithLifecycle()
    val route by viewModel.route.collectAsStateWithLifecycle()
    RunRouteMapContent(record, route, onBack)
}

@Composable
fun RunRouteMapContent(
    record: RecordLookup,
    route: List<GeoPoint>?,
    onBack: () -> Unit = {},
    zone: ZoneId = ZoneId.systemDefault(),
) {
    val t = runTone()
    val session = (record as? RecordLookup.Found)?.session
    RecordsFrame(onBack) {
        RecordTitle(stringResource(R.string.run_rec_map_title), Modifier.padding(horizontal = RunSpec.Gutter).padding(top = 2.dp))
        Box(Modifier.weight(1f).fillMaxWidth()) {
            when {
                record == RecordLookup.Missing -> RecordsState(
                    stringResource(R.string.rec_missing_title), stringResource(R.string.rec_missing_body),
                    Modifier.testTag("route-missing"), art = RecordArt.Missing,
                )
                record == RecordLookup.Failed -> RecordsState(
                    stringResource(R.string.run_rec_failed_title), stringResource(R.string.run_rec_failed_body),
                    Modifier.testTag("route-failed"), refresh = true,
                )
                session == null || route == null -> Box(Modifier.fillMaxSize().testTag("route-loading"), contentAlignment = Alignment.Center) {
                    RunSpinner(Modifier.size(36.dp))
                }
                route.size < 2 -> RecordsState(
                    stringResource(R.string.rec_no_route_title), stringResource(R.string.rec_no_route_body),
                    Modifier.testTag("route-none"), art = RecordArt.Period,
                )
                else -> Column(Modifier.fillMaxSize().padding(horizontal = RunSpec.Gutter).padding(top = 12.dp)) {
                    RunMapFrame(Modifier.weight(1f).testTag("route-map")) {
                        LiveRouteMap(
                            route, Modifier.fillMaxSize(), seed = session.id.toInt(), interactive = true,
                            controlLabels = MapControlLabels(
                                zoomIn = stringResource(R.string.rec_zoom_in),
                                zoomOut = stringResource(R.string.rec_zoom_out),
                                recenter = stringResource(R.string.rec_map_recenter),
                            ),
                            routeColor = t.cyan,
                            endpointLabels = stringResource(R.string.run_rec_map_start) to stringResource(R.string.run_rec_map_end),
                        )
                    }
                    RouteSummary(session, zone)
                }
            }
        }
        RunButton(
            stringResource(R.string.rec_back_to_record), onBack,
            Modifier.padding(horizontal = RunSpec.Gutter).padding(top = 12.dp, bottom = 14.dp).testTag("route-back"),
        )
    }
}

/** 지도 아래 요약 — 날짜 | 거리 | 시간(한 덩어리로 읽힌다) */
@Composable
private fun RouteSummary(session: WalkSessionEntity, zone: ZoneId) {
    val t = runTone()
    val day = rememberFormatter(R.string.date_month_day).format(session.startedAt.localDate(zone))
    RunCard(
        Modifier.padding(top = 10.dp).semantics(mergeDescendants = true) {}.testTag("route-summary"),
        padding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
    ) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            SummaryCell(stringResource(R.string.run_rec_label_date), Modifier.weight(1.1f)) {
                Text(
                    day, maxLines = 1,
                    style = TextStyle(
                        fontFamily = StepUpSans, fontWeight = FontWeight.ExtraBold, fontStyle = FontStyle.Italic,
                        fontSize = 25.sp, letterSpacing = (-0.02).em, color = t.text,
                    ),
                )
            }
            Divider()
            SummaryCell(stringResource(R.string.run_label_distance), Modifier.weight(1f)) {
                if (session.distanceMeters > 0) RunNumber(km2(session.distanceMeters), unit = "km", size = 27.sp, unitSize = 15.sp, italicUnit = false)
                else RunNumber("—", size = 27.sp)
            }
            Divider()
            SummaryCell(stringResource(R.string.run_rec_label_time), Modifier.weight(1f)) {
                RunNumber(clockText(session.durationSec), size = 27.sp)
            }
        }
    }
}

@Composable
private fun RowScope.Divider() {
    val t = runTone()
    Box(Modifier.padding(horizontal = 10.dp).width(1.5.dp).height(44.dp).background(t.divider))
}

@Composable
private fun SummaryCell(label: String, modifier: Modifier, value: @Composable ColumnScope.() -> Unit) {
    val t = runTone()
    Column(modifier) {
        Text(label, style = runTextStyle(13.sp, t.label, FontWeight.SemiBold))
        value()
    }
}
