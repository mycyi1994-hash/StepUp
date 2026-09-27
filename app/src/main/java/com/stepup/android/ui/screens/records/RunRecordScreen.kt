package com.stepup.android.ui.screens.records

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.Fullscreen
import androidx.compose.material.icons.outlined.LocationOn
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.TextUnit
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
import com.stepup.android.ui.components.SecondaryHeader
import com.stepup.android.ui.components.SettingsPrimaryButton
import com.stepup.android.ui.components.SettingsSecondaryButton
import com.stepup.android.ui.components.SettingsSheet
import com.stepup.android.ui.components.SettingsToast
import com.stepup.android.ui.components.settingsPalette
import com.stepup.android.ui.experience.feedbackClickable
import com.stepup.android.ui.theme.StepUpDesign
import java.time.Instant
import java.time.ZoneId
import java.util.Locale

/**
 * 지난 러닝 상세(11 · 13 · 14 · 16) — 목록에서 고른 그 러닝 하나(세션 id). 날짜 · 시작 시각 · 거리 · 시간 · 페이스 ·
 * 그 러닝의 저장 경로. 거리가 없는 러닝(시간만)은 시간을 크게 두고 거리 · 페이스는 "—"(13). 경로가 없으면 지도 대신 안내.
 *
 * 휴지통 → 확인 시트(14) → 이 휴대폰의 기록에서 지운 뒤에만 목록으로. 못 지우면 기록을 두고 아래에 한 줄(16).
 * 서버 확인을 기다리는 러닝은 지우지 않는다.
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
    RunRecordContent(
        record = record, route = route, delete = delete, onBack = onBack, onOpenMap = onOpenMap,
        onAskDelete = viewModel::askDelete, onCloseDelete = viewModel::closeDelete,
        onConfirmDelete = { viewModel.confirmDelete(onDeleted) },
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
) {
    val p = settingsPalette()
    val session = (record as? RecordLookup.Found)?.session
    Column(Modifier.fillMaxSize().padding(horizontal = StepUpDesign.Gutter)) {
        SecondaryHeader(
            onBack = onBack, balance = null, onOpenWallet = null, title = stringResource(R.string.rec_detail_title),
            trailing = if (session == null) null else {
                {
                    IconButton(onClick = onAskDelete, enabled = delete != DeleteState.Deleting,
                        modifier = Modifier.size(48.dp).testTag("run-delete")) {
                        Icon(Icons.Outlined.Delete, stringResource(R.string.rec_delete_cd), tint = p.secondary)
                    }
                }
            },
        )
        when (record) {
            RecordLookup.Loading -> Box(Modifier.weight(1f).fillMaxWidth().testTag("run-loading"), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(Modifier.size(26.dp), color = p.accent, strokeWidth = 2.dp)
            }
            RecordLookup.Missing -> RecordsState(
                stringResource(R.string.rec_missing_title), stringResource(R.string.rec_missing_body),
                Modifier.weight(1f).testTag("run-missing"), actionLabel = stringResource(R.string.rec_back_to_list),
                onAction = onBack, actionTag = "run-missing-back",
            )
            RecordLookup.Failed -> RecordsState(
                stringResource(R.string.rec_failed_title), stringResource(R.string.rec_failed_body),
                Modifier.weight(1f).testTag("run-failed"), actionLabel = stringResource(R.string.rec_back_to_list),
                onAction = onBack, actionTag = "run-failed-back", refresh = true,
            )
            is RecordLookup.Found -> {
                Column(Modifier.weight(1f).fillMaxWidth().verticalScroll(rememberScrollState()).padding(bottom = 16.dp)) {
                    RunFacts(record.session, zone)
                    Text(stringResource(R.string.rec_route), color = p.text, fontSize = 17.sp, fontWeight = FontWeight.SemiBold,
                        modifier = Modifier.padding(top = 34.dp, bottom = 14.dp).semantics { heading() })
                    RouteCard(record.session, route, onOpenMap)
                    val note = when {
                        record.session.distanceMeters <= 0 -> stringResource(R.string.rec_no_distance_note)
                        route.isNullOrEmpty() -> stringResource(R.string.rec_steps_distance_note)
                        else -> stringResource(R.string.rec_map_hint)
                    }
                    if (route != null) {
                        Text(note, color = p.secondary, fontSize = 14.sp, lineHeight = 1.45.em,
                            modifier = Modifier.padding(top = 14.dp).testTag("run-note"))
                    }
                }
                SettingsToast(
                    when (delete) {
                        DeleteState.Failed -> stringResource(R.string.rec_delete_failed_toast)
                        DeleteState.Uploading -> stringResource(R.string.rec_delete_uploading)
                        else -> null
                    },
                    Modifier.padding(bottom = 8.dp).testTag("run-delete-notice"), success = false,
                )
                SettingsPrimaryButton(stringResource(R.string.rec_back_to_list), onBack,
                    Modifier.fillMaxWidth().padding(bottom = 16.dp).testTag("run-back-to-list"))
            }
        }
    }
    if (session != null && (delete == DeleteState.Confirm || delete == DeleteState.Deleting)) {
        DeleteSheet(session, deleting = delete == DeleteState.Deleting, onCancel = onCloseDelete, onDelete = onConfirmDelete, zone = zone)
    }
}

/** 날짜 · 시작 시각 · 큰 숫자 하나 · 두 칸(시간 · 페이스 또는 거리 · 페이스) */
@Composable
private fun RunFacts(session: WalkSessionEntity, zone: ZoneId) {
    val p = settingsPalette()
    val date = rememberFormatter(R.string.rec_detail_date_pattern).format(session.startedAt.localDate(zone))
    val time = rememberFormatter(R.string.rec_time_pattern).format(Instant.ofEpochMilli(session.startedAt).atZone(zone))
    val hasDistance = session.distanceMeters > 0
    val pace = runPace(session.durationSec, session.distanceMeters) ?: "—"
    Text(date, color = p.text, fontSize = 19.sp, fontWeight = FontWeight.SemiBold, modifier = Modifier.padding(top = 10.dp).testTag("run-date"))
    Text(stringResource(R.string.rec_started_at, time), color = p.secondary, fontSize = 14.sp, modifier = Modifier.padding(top = 6.dp))
    if (hasDistance) {
        BigNumber(stringResource(R.string.rec_distance_label), km(session.distanceMeters), "km", "run-distance")
        Row(Modifier.fillMaxWidth().padding(top = 16.dp)) {
            Fact(stringResource(R.string.rec_time_label), durationText(session.durationSec), Modifier.weight(1f), "run-time")
            Fact(stringResource(R.string.rec_pace_label), pace, Modifier.weight(1f), "run-pace")
        }
    } else {
        // 시간만 남은 러닝(13) — 22:24 분:초
        val (clock, unit) = clockText(session.durationSec)
        BigNumber(stringResource(R.string.rec_time_label), clock, stringResource(unit), "run-time")
        Row(Modifier.fillMaxWidth().padding(top = 16.dp)) {
            Fact(stringResource(R.string.rec_distance_label_short), "—", Modifier.weight(1f), "run-distance")
            Fact(stringResource(R.string.rec_pace_label), "—", Modifier.weight(1f), "run-pace")
        }
    }
}

/** 라벨 위 큰 숫자 — 글자가 크거나 숫자가 길면 숫자를 먼저 줄인다 */
@Composable
private fun BigNumber(label: String, number: String, unit: String, tag: String) {
    val p = settingsPalette()
    val size: TextUnit = when {
        number.length > 7 -> 48.sp
        number.length > 5 -> 58.sp
        else -> 70.sp
    }
    Text(label, color = p.secondary, fontSize = 15.sp, modifier = Modifier.padding(top = 30.dp))
    Row(verticalAlignment = Alignment.Bottom, modifier = Modifier.semantics(mergeDescendants = true) {}.testTag(tag)) {
        Text(number, color = p.text, fontSize = size, fontWeight = FontWeight.SemiBold, maxLines = 1,
            modifier = Modifier.alignByBaseline())
        Spacer(Modifier.width(14.dp))
        Text(unit, color = p.secondary, fontSize = if (unit.length > 2) 16.sp else 26.sp, modifier = Modifier.alignByBaseline())
    }
}

@Composable
private fun Fact(label: String, value: String, modifier: Modifier, tag: String) {
    val p = settingsPalette()
    Column(modifier.semantics(mergeDescendants = true) {}.testTag(tag)) {
        Text(label, color = p.secondary, fontSize = 14.sp)
        Text(value, color = p.text, fontSize = 22.sp, fontWeight = FontWeight.SemiBold, modifier = Modifier.padding(top = 6.dp))
    }
}

/** 22:24(분:초) · 1:05:12(시:분:초) */
private fun clockText(seconds: Long): Pair<String, Int> {
    val s = seconds.coerceAtLeast(0)
    return if (s >= 3600) {
        String.format(Locale.ROOT, "%d:%02d:%02d", s / 3600, (s % 3600) / 60, s % 60) to R.string.rec_hours_minutes_seconds_unit
    } else {
        String.format(Locale.ROOT, "%d:%02d", s / 60, s % 60) to R.string.rec_minutes_seconds_unit
    }
}

/** 경로 카드 — 그 러닝의 저장 좌표만. 누르면(또는 "확대") 크게. 좌표가 없으면 지도 대신 안내(13) */
@Composable
private fun RouteCard(session: WalkSessionEntity, route: List<GeoPoint>?, onOpenMap: () -> Unit) {
    val p = settingsPalette()
    val shape = RoundedCornerShape(22.dp)
    when {
        route == null -> Box(Modifier.fillMaxWidth().height(250.dp).clip(shape).background(p.skeleton.copy(alpha = 0.6f)).testTag("run-route-loading"))
        route.size < 2 -> Column(
            Modifier.fillMaxWidth().heightIn(min = 250.dp).clip(shape).background(p.surface).padding(24.dp).testTag("run-no-route"),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            Icon(Icons.Outlined.LocationOn, contentDescription = null, tint = p.accent, modifier = Modifier.size(30.dp))
            Text(stringResource(R.string.rec_no_route_title), color = p.text, fontSize = 18.sp, fontWeight = FontWeight.SemiBold,
                textAlign = TextAlign.Center, modifier = Modifier.padding(top = 18.dp))
            Text(stringResource(R.string.rec_no_route_body), color = p.secondary, fontSize = 14.sp, textAlign = TextAlign.Center,
                modifier = Modifier.padding(top = 8.dp))
        }
        else -> {
            val label = stringResource(R.string.rec_open_map_cd)
            Box(
                Modifier.fillMaxWidth().height(250.dp).clip(shape).background(p.surface)
                    .feedbackClickable(role = null, onClick = onOpenMap)
                    .semantics(mergeDescendants = true) {
                        role = Role.Button
                        contentDescription = label
                    }
                    .testTag("run-route"),
            ) {
                LiveRouteMap(route, Modifier.fillMaxSize(), seed = session.id.toInt())
                Row(
                    Modifier.align(Alignment.TopEnd).padding(12.dp).clip(RoundedCornerShape(14.dp))
                        .background(Color(0xCC0B1424)).padding(horizontal = 12.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Icon(Icons.Outlined.Fullscreen, contentDescription = null, tint = p.text, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(8.dp))
                    Text(stringResource(R.string.rec_expand), color = p.text, fontSize = 13.sp, fontWeight = FontWeight.Medium)
                }
            }
        }
    }
}

/** 삭제 확인(14) — 무엇을 지우는지(날짜 · 거리 또는 시간), 되돌릴 수 없음, 지우는 범위. 지우는 동안은 닫히지 않는다 */
@Composable
private fun DeleteSheet(session: WalkSessionEntity, deleting: Boolean, onCancel: () -> Unit, onDelete: () -> Unit, zone: ZoneId) {
    val p = settingsPalette()
    val day = rememberFormatter(R.string.date_month_day).format(session.startedAt.localDate(zone))
    val amount = if (session.distanceMeters > 0) "${km(session.distanceMeters)}km" else durationText(session.durationSec)
    SettingsSheet(
        title = stringResource(R.string.rec_delete_title), onDismiss = onCancel, modifier = Modifier.testTag("run-delete-sheet"),
        dismissible = !deleting, showClose = false,
        actions = {
            DeleteActions(deleting, onCancel, onDelete)
        },
    ) {
        Text(stringResource(R.string.rec_delete_subject, day, amount), color = p.text, fontSize = 16.sp,
            modifier = Modifier.testTag("run-delete-subject"))
        Text(stringResource(R.string.rec_delete_body), color = p.secondary, fontSize = 14.sp)
        Text(stringResource(R.string.rec_delete_scope), color = p.secondary, fontSize = 13.sp, lineHeight = 1.45.em)
    }
}

@Composable
private fun ColumnScope.DeleteActions(deleting: Boolean, onCancel: () -> Unit, onDelete: () -> Unit) {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        SettingsSecondaryButton(stringResource(R.string.common_cancel), onCancel, Modifier.weight(1f).testTag("run-delete-cancel"),
            enabled = !deleting)
        SettingsPrimaryButton(
            stringResource(if (deleting) R.string.rec_deleting else R.string.rec_delete), onDelete,
            Modifier.weight(1f).testTag("run-delete-confirm"), loading = deleting,
        )
    }
}
