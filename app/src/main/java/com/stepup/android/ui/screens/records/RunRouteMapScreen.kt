package com.stepup.android.ui.screens.records

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.stepup.android.R
import com.stepup.android.data.local.WalkSessionEntity
import com.stepup.android.domain.GeoPoint
import com.stepup.android.domain.localDate
import com.stepup.android.ui.components.LiveRouteMap
import com.stepup.android.ui.components.MapControlLabels
import com.stepup.android.ui.components.SecondaryHeader
import com.stepup.android.ui.components.SettingsPrimaryButton
import com.stepup.android.ui.components.settingsPalette
import com.stepup.android.ui.theme.StepUpDesign
import java.time.ZoneId

/**
 * 경로 확대(12) — 상세에서 고른 그 러닝의 저장 좌표만, 기존 지도(타일 · 출처 표기 그대로) 위에. 처음엔 경로 전체가 보이게
 * 맞추고, 손가락 · +/− 로 확대 · 축소 · 이동한다. 요약 카드는 지도 아래에 두어 경로를 가리지 않는다.
 * 지난 경로를 보는 것이라 현재 위치 권한을 묻지 않는다.
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
    val p = settingsPalette()
    val session = (record as? RecordLookup.Found)?.session
    Column(Modifier.fillMaxSize()) {
        SecondaryHeader(
            onBack = onBack, balance = null, onOpenWallet = null, title = stringResource(R.string.rec_map_title),
            modifier = Modifier.padding(horizontal = StepUpDesign.Gutter),
        )
        Box(Modifier.weight(1f).fillMaxWidth()) {
            when {
                record == RecordLookup.Missing -> RecordsState(
                    stringResource(R.string.rec_missing_title), stringResource(R.string.rec_missing_body),
                    Modifier.padding(horizontal = StepUpDesign.Gutter).testTag("route-missing"),
                )
                record == RecordLookup.Failed -> RecordsState(
                    stringResource(R.string.rec_failed_title), stringResource(R.string.rec_failed_body),
                    Modifier.padding(horizontal = StepUpDesign.Gutter).testTag("route-failed"), refresh = true,
                )
                session == null || route == null -> Box(Modifier.fillMaxSize().testTag("route-loading"), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(Modifier.size(26.dp), color = p.accent, strokeWidth = 2.dp)
                }
                route.size < 2 -> RecordsState(
                    stringResource(R.string.rec_no_route_title), stringResource(R.string.rec_no_route_body),
                    Modifier.padding(horizontal = StepUpDesign.Gutter).testTag("route-none"),
                )
                else -> Column(Modifier.fillMaxSize()) {
                    Box(Modifier.weight(1f).fillMaxWidth().clip(RoundedCornerShape(28.dp)).background(p.surface).testTag("route-map")) {
                        LiveRouteMap(
                            route, Modifier.fillMaxSize(), seed = session.id.toInt(), interactive = true,
                            controlLabels = MapControlLabels(
                                zoomIn = stringResource(R.string.rec_zoom_in),
                                zoomOut = stringResource(R.string.rec_zoom_out),
                                recenter = stringResource(R.string.rec_map_recenter),
                            ),
                        )
                    }
                    RouteSummary(session, zone)
                }
            }
        }
        SettingsPrimaryButton(
            stringResource(R.string.rec_back_to_record), onBack,
            Modifier.padding(horizontal = StepUpDesign.Gutter).padding(top = 12.dp, bottom = 16.dp).fillMaxWidth().testTag("route-back"),
        )
    }
}

/** 지도 아래 요약 — "9월 27일 러닝" · 거리 · 시간 */
@Composable
private fun RouteSummary(session: WalkSessionEntity, zone: ZoneId) {
    val p = settingsPalette()
    val day = rememberFormatter(R.string.date_month_day).format(session.startedAt.localDate(zone))
    Column(
        Modifier.padding(horizontal = StepUpDesign.Gutter).padding(top = 12.dp).fillMaxWidth()
            .clip(RoundedCornerShape(22.dp)).background(p.surface).padding(horizontal = 22.dp, vertical = 16.dp)
            .semantics(mergeDescendants = true) {}.testTag("route-summary"),
    ) {
        Text(stringResource(R.string.rec_map_run, day), color = p.secondary, fontSize = 14.sp)
        Row(Modifier.fillMaxWidth().padding(top = 6.dp), verticalAlignment = Alignment.CenterVertically) {
            Text(if (session.distanceMeters > 0) "${km(session.distanceMeters)} km" else "—", color = p.text, fontSize = 30.sp,
                fontWeight = FontWeight.SemiBold)
            Spacer(Modifier.weight(1f))
            Text(durationText(session.durationSec), color = p.text, fontSize = 17.sp)
        }
    }
}
