package com.stepup.android.ui.screens.records

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.LocationOff
import androidx.compose.material.icons.outlined.Schedule
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.stepup.android.R
import com.stepup.android.core.ServiceLocator
import com.stepup.android.data.local.WalkSessionEntity
import com.stepup.android.domain.GeoPoint
import com.stepup.android.domain.localDate
import com.stepup.android.ui.components.LiveRouteMap
import com.stepup.android.ui.components.RunButton
import com.stepup.android.ui.components.RunButtonKind
import com.stepup.android.ui.components.RunHeaderAction
import com.stepup.android.ui.components.RunHero
import com.stepup.android.ui.components.RunMapFrame
import com.stepup.android.ui.components.RunMapPlaceholder
import com.stepup.android.ui.components.RunNotice
import com.stepup.android.ui.components.RunNoticeKind
import com.stepup.android.ui.components.RunSheet
import com.stepup.android.ui.components.RunSheetText
import com.stepup.android.ui.components.RunSpec
import com.stepup.android.ui.components.RunSpinner
import com.stepup.android.ui.components.RunStat
import com.stepup.android.ui.components.RunStatRow
import com.stepup.android.ui.components.runTextStyle
import com.stepup.android.ui.components.runTone
import com.stepup.android.ui.experience.feedbackClickable
import com.stepup.android.ui.screens.walk.challengeTitle
import java.time.Instant
import java.time.ZoneId

/**
 * 지난 러닝 상세(시안 H11 · H13 · H14 · H16) — 목록에서 고른 그 러닝 하나(세션 id). 러닝 종류 · 날짜 · 시작 시각 ·
 * 큰 거리 · 시간 · 페이스 · 속도 · 그 러닝의 저장 경로(실제 지도). 거리가 없는 러닝(시간만)은 시간을 크게 두고
 * 거리 · 페이스는 "—"(H13), 경로가 없으면 지도 대신 안내.
 *
 * 머리의 "삭제" → 확인 시트(H14) → 이 휴대폰의 기록에서 지운 뒤에만 목록으로. 못 지우면 기록을 두고 위에 알림,
 * 아래 버튼은 다시 삭제 · 기록 목록으로(H16). 서버 확인을 기다리는 러닝은 지우지 않는다.
 */
@Composable
fun RunRecordScreen(
    id: Long,
    onBack: () -> Unit = {},
    onOpenMap: () -> Unit = {},
    onDeleted: () -> Unit = {},
    viewModel: RunRecordViewModel = viewModel(key = "run-$id", factory = RunRecordViewModel.factory(id)),
) {
    val record by viewModel.record.collectAsStateWithLifecycle()
    val route by viewModel.route.collectAsStateWithLifecycle()
    val delete by viewModel.delete.collectAsStateWithLifecycle()
    // 러닝 종류 — 세션에 남은 것만(지난 도전 · 크루). 코스 · 다이어트 러닝은 세션에 적히지 않아 쓰지 않는다
    val attempts by ServiceLocator.userPrefs.goalAttempts.collectAsStateWithLifecycle(initialValue = emptyList())
    val session = (record as? RecordLookup.Found)?.session
    val goal = session?.let { s -> attempts.firstOrNull { it.startedAt == s.startedAt }?.goal }
    val kind = when {
        goal != null -> challengeTitle(goal)
        session != null && session.crewId.isNotEmpty() -> stringResource(R.string.run_rec_kind_crew)
        else -> null
    }
    RunRecordContent(
        record = record, route = route, delete = delete, onBack = onBack, onOpenMap = onOpenMap,
        onAskDelete = viewModel::askDelete, onCloseDelete = viewModel::closeDelete,
        onConfirmDelete = { viewModel.confirmDelete(onDeleted) }, kind = kind,
    )
}

@Composable
fun RunRecordContent(
    record: RecordLookup,
    route: List<GeoPoint>?,
    delete: DeleteState,
    onBack: () -> Unit = {},
    onOpenMap: () -> Unit = {},
    onAskDelete: () -> Unit = {},
    onCloseDelete: () -> Unit = {},
    onConfirmDelete: () -> Unit = {},
    zone: ZoneId = ZoneId.systemDefault(),
    /** 러닝 종류(챌린지 이름 · 크루 러닝) — 모르면 null. 시간만 남은 러닝은 "시간 기록" */
    kind: String? = null,
) {
    val t = runTone()
    val session = (record as? RecordLookup.Found)?.session
    val failed = delete == DeleteState.Failed
    RecordsFrame(
        onBack,
        trailing = if (session == null || failed) null else {
            {
                RunHeaderAction(
                    stringResource(R.string.rec_delete), onAskDelete, Modifier.testTag("run-delete"),
                    danger = true, enabled = delete != DeleteState.Deleting,
                )
            }
        },
    ) {
        when (record) {
            RecordLookup.Loading -> Box(Modifier.weight(1f).fillMaxWidth().testTag("run-loading"), contentAlignment = Alignment.Center) {
                RunSpinner(Modifier.size(36.dp))
            }
            RecordLookup.Missing -> RecordsState(
                stringResource(R.string.rec_missing_title), stringResource(R.string.rec_missing_body),
                Modifier.weight(1f).testTag("run-missing"), actionLabel = stringResource(R.string.rec_back_to_list),
                onAction = onBack, actionTag = "run-missing-back", art = RecordArt.Missing,
            )
            RecordLookup.Failed -> RecordsState(
                stringResource(R.string.run_rec_failed_title), stringResource(R.string.run_rec_failed_body),
                Modifier.weight(1f).testTag("run-failed"), actionLabel = stringResource(R.string.rec_back_to_list),
                onAction = onBack, actionTag = "run-failed-back", refresh = true,
            )
            is RecordLookup.Found -> {
                val s = record.session
                Column(
                    Modifier.weight(1f).fillMaxWidth().verticalScroll(rememberScrollState())
                        .padding(horizontal = RunSpec.Gutter).padding(top = 2.dp, bottom = 12.dp),
                ) {
                    RecordTitle(stringResource(R.string.run_rec_detail_title))
                    val subtitle = kind ?: if (s.distanceMeters <= 0) stringResource(R.string.run_rec_kind_time) else null
                    if (subtitle != null) {
                        Text17(subtitle, Modifier.padding(top = 4.dp))
                    }
                    val date = rememberFormatter(R.string.run_rec_date_pattern).format(s.startedAt.localDate(zone))
                    val time = rememberFormatter(R.string.rec_time_pattern).format(Instant.ofEpochMilli(s.startedAt).atZone(zone))
                    androidx.compose.material3.Text(
                        stringResource(R.string.run_rec_date_time, date, time),
                        style = runTextStyle(14.sp, t.label, FontWeight.Medium),
                        modifier = Modifier.padding(top = 2.dp).testTag("run-date"),
                    )
                    when (delete) {
                        DeleteState.Failed -> RunNotice(
                            stringResource(R.string.run_rec_delete_failed_title), Modifier.padding(top = 12.dp),
                            body = stringResource(R.string.run_rec_delete_failed_body), kind = RunNoticeKind.Error, tag = "run-delete-notice",
                        )
                        DeleteState.Uploading -> RunNotice(
                            stringResource(R.string.rec_delete_uploading), Modifier.padding(top = 12.dp),
                            kind = RunNoticeKind.Warn, tag = "run-delete-notice",
                        )
                        else -> Unit
                    }
                    RunFacts(s)
                    Spacer(Modifier.height(14.dp))
                    RouteCard(s, route, onOpenMap)
                    if (s.distanceMeters > 0 && route != null && route.size < 2) {
                        androidx.compose.material3.Text(
                            stringResource(R.string.rec_steps_distance_note), style = runTextStyle(14.sp, t.label, FontWeight.Medium),
                            modifier = Modifier.padding(top = 12.dp).testTag("run-note"),
                        )
                    }
                }
                Column(
                    Modifier.fillMaxWidth().padding(horizontal = RunSpec.Gutter).padding(top = 6.dp, bottom = 14.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    if (failed) {
                        RunButton(stringResource(R.string.rec_delete), onAskDelete, Modifier.testTag("run-delete-retry"))
                        RunButton(stringResource(R.string.rec_back_to_list), onBack, Modifier.testTag("run-back-to-list"),
                            kind = RunButtonKind.Secondary)
                    } else {
                        RunButton(stringResource(R.string.rec_back_to_list), onBack, Modifier.testTag("run-back-to-list"))
                    }
                }
            }
        }
    }
    if (session != null && (delete == DeleteState.Confirm || delete == DeleteState.Deleting)) {
        DeleteSheet(session, deleting = delete == DeleteState.Deleting, onCancel = onCloseDelete, onDelete = onConfirmDelete, zone = zone)
    }
}

@Composable
private fun Text17(text: String, modifier: Modifier = Modifier) {
    val t = runTone()
    androidx.compose.material3.Text(text, style = runTextStyle(17.sp, t.label, FontWeight.Bold), modifier = modifier)
}

/** 큰 수 하나(거리, 시간만이면 시간) · 세 칸(달린 시간 · 평균 페이스 · 평균 속도 / 달린 시간 · 거리 · 평균 페이스) */
@Composable
private fun RunFacts(session: WalkSessionEntity) {
    val km = "km"
    val perKm = stringResource(R.string.run_rec_unit_per_km)
    if (session.distanceMeters > 0) {
        RunHero(null, km2(session.distanceMeters), Modifier.padding(top = 18.dp, bottom = 10.dp), unit = km, size = 100.sp,
            valueTag = "run-distance")
        RunStatRow(
            listOf(
                RunStat(stringResource(R.string.run_label_time), clockText(session.durationSec), tag = "run-time"),
                RunStat(stringResource(R.string.run_label_pace), paceValue(session.durationSec, session.distanceMeters) ?: "—",
                    unit = perKm, tag = "run-pace"),
                RunStat(stringResource(R.string.run_label_speed), speedText(session.durationSec, session.distanceMeters) ?: "—",
                    unit = stringResource(R.string.run_rec_unit_kmh), tag = "run-speed"),
            ),
            framed = true, valueSize = 29.sp,
        )
    } else {
        // 시간만 남은 러닝(H13) — 거리 · 페이스는 재지 않았다("—")
        RunHero(null, clockText(session.durationSec), Modifier.padding(top = 18.dp, bottom = 10.dp), size = 100.sp, valueTag = "run-time")
        RunStatRow(
            listOf(
                RunStat(stringResource(R.string.run_label_time), clockText(session.durationSec), tag = "run-elapsed"),
                RunStat(stringResource(R.string.run_label_distance), "—", unit = km, tag = "run-distance"),
                RunStat(stringResource(R.string.run_label_pace), "—", unit = perKm, tag = "run-pace"),
            ),
            framed = true, valueSize = 29.sp,
        )
    }
}

/** 경로 — 그 러닝의 저장 좌표만(실제 지도). 누르면(또는 확대 버튼) 크게. 좌표가 없으면 지도 대신 안내(H13) */
@Composable
private fun RouteCard(session: WalkSessionEntity, route: List<GeoPoint>?, onOpenMap: () -> Unit) {
    val t = runTone()
    when {
        route == null -> RunMapFrame(Modifier.height(236.dp).testTag("run-route-loading")) {
            RunSpinner(Modifier.align(Alignment.Center).size(30.dp))
        }
        route.size < 2 -> RunMapFrame(Modifier.height(236.dp).testTag("run-no-route")) {
            if (session.distanceMeters <= 0) {
                RunMapPlaceholder(Icons.Outlined.Schedule, stringResource(R.string.run_rec_no_location_title),
                    stringResource(R.string.run_rec_no_location_body))
            } else {
                RunMapPlaceholder(Icons.Outlined.LocationOff, stringResource(R.string.rec_no_route_title),
                    stringResource(R.string.rec_no_route_body))
            }
        }
        else -> {
            val label = stringResource(R.string.rec_open_map_cd)
            RunMapFrame(
                Modifier.height(236.dp)
                    .feedbackClickable(role = null, onClick = onOpenMap)
                    .semantics {
                        role = Role.Button
                        contentDescription = label
                    }
                    .testTag("run-route"),
                onExpand = onOpenMap, expandLabel = label,
            ) {
                val shown = remember(route) { thin(route, ZOOM_ROUTE_POINTS) }
                LiveRouteMap(shown, Modifier.fillMaxSize(), seed = session.id.toInt(), routeColor = t.cyan)
            }
        }
    }
}

/** 삭제 확인(H14) — 무엇을 지우는지(날짜 · 거리 · 시간), 되돌릴 수 없음, 지우는 범위. 지우는 동안은 닫히지 않는다 */
@Composable
private fun DeleteSheet(session: WalkSessionEntity, deleting: Boolean, onCancel: () -> Unit, onDelete: () -> Unit, zone: ZoneId) {
    val t = runTone()
    val day = rememberFormatter(R.string.date_month_day).format(session.startedAt.localDate(zone))
    val subject = listOfNotNull(
        day,
        if (session.distanceMeters > 0) "${km2(session.distanceMeters)}km" else null,
        durationText(session.durationSec),
    ).joinToString(" · ")
    RunSheet(onDismiss = onCancel, modifier = Modifier.testTag("run-delete-sheet"), showClose = false, dismissible = !deleting) {
        RunSheetText(stringResource(R.string.run_rec_delete_title))
        Spacer(Modifier.height(8.dp))
        androidx.compose.material3.Text(
            subject, style = runTextStyle(15.sp, t.label, FontWeight.SemiBold), textAlign = TextAlign.Center,
            modifier = Modifier.testTag("run-delete-subject"),
        )
        Spacer(Modifier.height(12.dp))
        androidx.compose.material3.Text(
            stringResource(R.string.rec_delete_body), style = runTextStyle(15.sp, t.text, FontWeight.Medium), textAlign = TextAlign.Center,
        )
        androidx.compose.material3.Text(
            stringResource(R.string.rec_delete_scope), style = runTextStyle(13.sp, t.muted, FontWeight.Medium, 1.45f),
            textAlign = TextAlign.Center, modifier = Modifier.padding(top = 4.dp, start = 8.dp, end = 8.dp),
        )
        Spacer(Modifier.height(20.dp))
        RunButton(
            stringResource(if (deleting) R.string.rec_deleting else R.string.run_rec_delete_do), onDelete,
            Modifier.testTag("run-delete-confirm"), kind = RunButtonKind.Danger, busy = deleting,
        )
        Spacer(Modifier.height(10.dp))
        RunButton(
            stringResource(R.string.common_cancel), onCancel, Modifier.testTag("run-delete-cancel"),
            kind = RunButtonKind.Secondary, enabled = !deleting,
        )
    }
}
