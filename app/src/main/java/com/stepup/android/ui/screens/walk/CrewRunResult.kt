package com.stepup.android.ui.screens.walk

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.Send
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.MoreHoriz
import androidx.compose.material.icons.outlined.Map
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import com.stepup.android.R
import com.stepup.android.ui.components.LocalMapTone
import com.stepup.android.ui.components.MapTone
import com.stepup.android.ui.components.RunMapPlaceholder
import com.stepup.android.ui.components.RunSheet
import com.stepup.android.ui.components.runTextStyle
import com.stepup.android.ui.components.runTone
import com.stepup.android.ui.experience.feedbackClickable
import com.stepup.android.ui.theme.StepUpSans

/*
 * 크루 달리기 v2 — 크루 러닝 결과(CR13). 머리(러닝 결과 · 크루 이름 · 배지) · 저장 표시 · 함께 출발한 인원 · 큰 거리 ·
 * 시간 | 평균 페이스 · 내 경로 지도 · 내 기록 저장 · 보상(예상 · 확인 중 · 반영 완료 · 지급 제외 · 미지급) · 크루에 기록 공유 | 확인.
 * 덜 쓰는 "내 러닝 기록 보기" · "삭제"는 ⋯ 안으로 옮겼다(지우지 않는다). 보상은 서버가 확인하기 전에는 "예상 · 확인 중"이다.
 */

@Immutable
internal data class CrewResultUi(
    val crewName: String,
    /** 함께 출발한 인원(나 포함) — 출발할 때 서버가 정한 명단. 1이면 혼자 출발한 크루 달리기 */
    val started: Int,
    val distance: String,
    val time: String,
    val pace: String,
    /** 마친 뒤의 사실 한 줄(진행자 · 참가자) — 모르면 null */
    val note: String?,
    /** 걸음으로 셈한 거리면 그 근거 */
    val basis: String?,
)

@Composable
internal fun CrewRunResult(
    ui: CrewResultUi,
    reward: ResultReward,
    map: (@Composable BoxScope.() -> Unit)?,
    onReason: () -> Unit,
    onHistory: () -> Unit,
    onShare: (() -> Unit)?,
    shareLabel: String,
    onDone: () -> Unit,
    onRecords: () -> Unit,
    onDelete: () -> Unit,
    modifier: Modifier = Modifier,
    deleteFailed: String? = null,
) {
    val c = crewColors()
    var more by rememberSaveable { mutableStateOf(false) }
    Box(modifier.fillMaxSize().background(c.screen).testTag("run-result")) {
        Column(Modifier.fillMaxSize().safeDrawingPadding()) {
            CrewHeader(
                title = stringResource(R.string.run_result_title), subtitle = ui.crewName, onBack = onDone,
                trailing = {
                    val label = stringResource(R.string.run_free_more)
                    Box(
                        Modifier.size(44.dp).clip(CircleShape).border(1.dp, c.line, CircleShape)
                            .feedbackClickable(onClick = { more = true }).semantics { contentDescription = label }
                            .testTag("run-result-more"),
                        contentAlignment = Alignment.Center,
                    ) { Icon(Icons.Filled.MoreHoriz, contentDescription = null, tint = c.text, modifier = Modifier.size(22.dp)) }
                },
            )
            Column(
                Modifier.weight(1f).fillMaxWidth().verticalScroll(rememberScrollState()).padding(horizontal = 20.dp)
                    .testTag("run-result-scroll"),
            ) {
                if (deleteFailed != null) {
                    FailureBanner(stringResource(R.string.run_delete_failed), deleteFailed, "run-delete-failed")
                    Spacer(Modifier.height(12.dp))
                }
                Box(Modifier.size(44.dp).clip(CircleShape).background(c.success), contentAlignment = Alignment.Center) {
                    Icon(Icons.Filled.Check, contentDescription = null, tint = c.successInk, modifier = Modifier.size(24.dp))
                }
                Spacer(Modifier.height(12.dp))
                Text(
                    stringResource(if (ui.started > 1) R.string.run_crew_result_title else R.string.run_crew_result_solo),
                    style = TextStyle(
                        fontFamily = StepUpSans, fontWeight = FontWeight.ExtraBold, fontSize = 27.sp, lineHeight = 1.3.em,
                        letterSpacing = (-0.04).em, color = c.text,
                    ),
                    modifier = Modifier.semantics { heading() }.testTag("run-result-title"),
                )
                Spacer(Modifier.height(6.dp))
                Text(
                    stringResource(R.string.run_crew_result_line, ui.crewName, ui.started),
                    style = runTextStyle(14.sp, c.label, FontWeight.Medium), maxLines = 2, overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.testTag("run-result-crew"),
                )
                Spacer(Modifier.height(10.dp))
                FitNumber(
                    ui.distance, "km", valueSize = 64.sp, unitSize = 24.sp, valueColor = c.text, unitColor = c.label,
                    modifier = Modifier.fillMaxWidth(), valueTag = "run-result-distance",
                )
                Spacer(Modifier.height(10.dp))
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    CrewStat(stringResource(R.string.run_label_time), ui.time, null, "run-result-time", 24.sp)
                    CrewStat(stringResource(R.string.run_label_pace), ui.pace, "/km", "run-result-pace", 24.sp)
                }
                Spacer(Modifier.height(16.dp))
                CrewResultMap(map)
                if (ui.basis != null) {
                    Text(ui.basis, style = runTextStyle(13.sp, c.label), modifier = Modifier.padding(top = 8.dp).testTag("run-result-basis"))
                }
                Spacer(Modifier.height(14.dp))
                CrewLine()
                Spacer(Modifier.height(6.dp))
                CrewSummaryRow(stringResource(R.string.run_crew_my_record), stringResource(R.string.run_free_record_saved), "run-result-saved")
                CrewRewardRows(reward, onReason, onHistory)
                if (ui.note != null) {
                    Spacer(Modifier.height(10.dp))
                    Text(ui.note, style = runTextStyle(14.sp, c.label, FontWeight.Medium, 1.5f), modifier = Modifier.testTag("run-result-note"))
                }
                Spacer(Modifier.height(12.dp))
            }
            Column(
                Modifier.fillMaxWidth().padding(horizontal = 20.dp).padding(top = 8.dp, bottom = 12.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                if (onShare != null) {
                    FreeRunButton(shareLabel, onShare, Modifier.fillMaxWidth().testTag("run-result-share"),
                        icon = Icons.AutoMirrored.Outlined.Send)
                }
                FreeRunButton(
                    stringResource(R.string.common_confirm), onDone, Modifier.fillMaxWidth().testTag("run-result-done"),
                    kind = if (onShare != null) FreeButtonKind.Secondary else FreeButtonKind.Primary,
                )
            }
        }
    }
    if (more) {
        RunSheet(onDismiss = { more = false }, modifier = Modifier.testTag("run-result-more-sheet")) {
            FreeRunButton(stringResource(R.string.run_open_records), {
                more = false
                onRecords()
            }, Modifier.fillMaxWidth().testTag("run-result-records"), kind = FreeButtonKind.Secondary)
            Spacer(Modifier.height(10.dp))
            FreeRunButton(stringResource(R.string.run_delete), {
                more = false
                onDelete()
            }, Modifier.fillMaxWidth().testTag("run-result-delete"), kind = FreeButtonKind.Danger)
        }
    }
}

/** 결과의 작은 지도 — 내 경로만(다른 사람의 위치는 남기지 않는다). 경로가 없으면 꾸미지 않고 그렇다고 적는다 */
@Composable
private fun CrewResultMap(map: (@Composable BoxScope.() -> Unit)?) {
    val c = crewColors()
    val t = runTone()
    val shape = RoundedCornerShape(14.dp)
    Box(
        Modifier.fillMaxWidth().height(180.dp).clip(shape).background(t.inset).border(1.dp, c.line, shape)
            .testTag("run-result-map-frame"),
    ) {
        if (map != null) {
            CompositionLocalProvider(LocalMapTone provides MapTone(t.mapFilter, t.mapShade, t.mapRoads)) {
                Box(Modifier.fillMaxSize(), content = map)
            }
        } else {
            RunMapPlaceholder(Icons.Outlined.Map, stringResource(R.string.run_result_route_none), null, Modifier.testTag("run-result-no-route"))
        }
    }
}

@Composable
private fun CrewSummaryRow(label: String, value: String, tag: String) {
    val c = crewColors()
    Row(Modifier.fillMaxWidth().heightIn(min = 40.dp), verticalAlignment = Alignment.CenterVertically) {
        Text(label, style = runTextStyle(15.sp, c.label, FontWeight.Medium), modifier = Modifier.weight(1f))
        Text(value, style = runTextStyle(15.sp, c.text, FontWeight.Bold), modifier = Modifier.testTag(tag))
    }
}

/**
 * 이번 러닝 보상 — 서버가 확인하기 전에는 이 폰이 셈한 "예상 · 확인 중"뿐이다. 확인되면 "반영 완료", 무효는 "지급 제외"와 까닭,
 * 거절 · 적립 없음은 "미지급"과 내역. 결과 화면 보상 칸([ResultRewardCard])과 같은 상태 · 같은 이름표를 쓴다.
 */
@Composable
private fun CrewRewardRows(reward: ResultReward, onReason: () -> Unit, onHistory: () -> Unit) {
    val c = crewColors()
    Column(Modifier.fillMaxWidth().testTag("run-result-reward-card")) {
        Row(Modifier.fillMaxWidth().heightIn(min = 44.dp), verticalAlignment = Alignment.CenterVertically) {
            Text(
                stringResource(R.string.run_reward_this), style = runTextStyle(15.sp, c.label, FontWeight.Medium),
                modifier = Modifier.weight(1f).testTag("run-result-reward-label"),
            )
            when (reward.settle) {
                Settle.PENDING -> {
                    CrewChip(stringResource(R.string.run_crew_reward_estimate), "run-result-settle-pending")
                    Spacer(Modifier.width(8.dp))
                    Text("${reward.amount} SUP", style = runTextStyle(16.sp, c.text, FontWeight.Bold),
                        modifier = Modifier.testTag("run-result-reward"))
                }
                Settle.DONE -> {
                    Text("${reward.amount} SUP", style = runTextStyle(16.sp, c.text, FontWeight.Bold),
                        modifier = Modifier.testTag("run-result-reward"))
                    Spacer(Modifier.width(8.dp))
                    CrewChip(stringResource(R.string.run_reward_done), "run-result-settle-done")
                }
                Settle.VOID -> CrewChip(stringResource(R.string.run_reward_void), "run-result-settle-void")
                Settle.NOT_PAID -> CrewChip(stringResource(R.string.run_reward_not_paid), "run-result-settle-rejected")
            }
        }
        // 까닭 줄(다시 로그인 · 미지급 까닭)만 "run-result-reward-note" — 결과 화면 보상 칸(ResultRewardCard)과 같은 이름표다.
        // 늘 붙는 안내 한 줄(확인 중 · 반영 완료 · 지급 제외)은 까닭이 아니라 이름표가 다르다. 보이는 글과 차례는 그대로다
        val reasons = when (reward.settle) {
            Settle.PENDING -> reward.notes
            Settle.NOT_PAID -> listOf(reward.notes.firstOrNull() ?: stringResource(R.string.run_reward_not_paid_note))
            Settle.DONE, Settle.VOID -> emptyList()
        }
        val hint = when (reward.settle) {
            Settle.PENDING -> stringResource(R.string.run_reward_note).takeIf { reward.notes.isEmpty() }
            Settle.DONE -> stringResource(R.string.run_reward_done_note)
            Settle.VOID -> stringResource(R.string.run_reward_void_note)
            Settle.NOT_PAID -> null
        }
        hint?.let {
            Text(it, style = runTextStyle(13.sp, c.muted, FontWeight.Normal, 1.45f), modifier = Modifier.testTag("run-result-reward-hint"))
        }
        reasons.forEach { line ->
            Text(line, style = runTextStyle(13.sp, c.muted, FontWeight.Normal, 1.45f), modifier = Modifier.testTag("run-result-reward-note"))
        }
        when (reward.settle) {
            Settle.VOID -> CrewTextAction(stringResource(R.string.run_details_more), onReason, Modifier.testTag("run-result-void-reason"),
                chevron = true)
            Settle.NOT_PAID -> CrewTextAction(stringResource(R.string.run_reward_history), onHistory, Modifier.testTag("run-result-history"),
                chevron = true)
            Settle.PENDING -> if (reward.signIn) {
                Spacer(Modifier.height(8.dp))
                com.stepup.android.ui.components.SignInAgainButton()
            }
            Settle.DONE -> Unit
        }
    }
}

@Composable
private fun CrewChip(text: String, tag: String) {
    val c = crewColors()
    Text(
        text, style = runTextStyle(12.sp, c.noticeInk, FontWeight.SemiBold, 1.2f), maxLines = 1,
        modifier = Modifier.clip(RoundedCornerShape(12.dp)).background(c.notice).padding(horizontal = 9.dp, vertical = 4.dp).testTag(tag),
    )
}
