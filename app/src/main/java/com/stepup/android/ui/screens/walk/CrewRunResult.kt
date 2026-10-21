package com.stepup.android.ui.screens.walk

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.BoxWithConstraints
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
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.MoreHoriz
import androidx.compose.material.icons.outlined.CardGiftcard
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
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.coerceIn
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.min
import androidx.compose.ui.unit.sp
import com.stepup.android.R
import com.stepup.android.ui.components.LocalMapTone
import com.stepup.android.ui.components.RunMapPlaceholder
import com.stepup.android.ui.components.RunSheet
import com.stepup.android.ui.components.runTextStyle
import com.stepup.android.ui.components.runTone
import com.stepup.android.ui.experience.feedbackClickable

/*
 * 크루 달리기 v2 — 크루 러닝 결과(CR13, 2026-10-16 크루 전달본). 완료 설명 대신 내 기록과 경로가 주인공이다.
 *
 * 머리(러닝 결과 · "크루 달리기" 배지 · ⋯) · 크루 표시 + 이름 · 함께 출발한 인원 · 큰 거리와 한 번의 "저장됨" · 시간 | 평균 페이스 ·
 * 넓은 내 경로 지도 · 보상 한 줄(누르면 설명) · 크루에 기록 공유(주 행동) | 완료(조용한 글자 행동).
 * 큰 완료 체크 · 큰 완료 제목 · "내 기록 저장 완료" 줄은 두지 않는다(같은 말을 두 번 하지 않는다).
 * 덜 쓰는 "내 러닝 기록 보기" · "삭제"는 ⋯ 안이다. 보상은 서버가 확인하기 전에는 "예상 · 확인 중"이다 — 금액 · 정산 규칙은 그대로다.
 */

@Immutable
internal data class CrewResultUi(
    val crewName: String,
    /** 크루가 정한 머리글자(크루 목록) — 모르면 이름 머리글자 */
    val monogram: String?,
    /** 함께 출발한 인원(나 포함) — 출발할 때 서버가 정한 명단. 1이면 혼자 출발한 크루 달리기 */
    val started: Int,
    val distance: String,
    val time: String,
    val pace: String,
    /** 마친 뒤의 사실 한 줄(진행자 — 함께 달리기가 끝났다 · 참가자) — 모르면 null */
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
    /** 크루 체력(0069) — 서버가 기록한 뒤 다시 읽은 오늘 내 몫 한 줄. 보상 설명 시트에만 둔다 */
    staminaNote: String? = null,
) {
    val c = crewColors()
    var more by rememberSaveable { mutableStateOf(false) }
    var rewardInfo by rememberSaveable { mutableStateOf(false) }
    Box(modifier.fillMaxSize().background(c.screen).testTag("run-result")) {
        Column(Modifier.fillMaxSize().safeDrawingPadding()) {
            CrewHeader(
                title = stringResource(R.string.run_result_title), onBack = onDone,
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
            BoxWithConstraints(Modifier.weight(1f).fillMaxWidth()) {
                // 지도 높이 — 폭의 72%(390dp 화면에서 약 250dp), 보이는 높이의 42%를 넘지 않게(작은 화면 · 큰 글씨에서 기록을 밀어내지 않는다)
                val mapHeight = min((maxWidth - 40.dp) * 0.72f, maxHeight * 0.42f).coerceIn(160.dp, 300.dp)
                Column(
                    Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = 20.dp)
                        .testTag("run-result-scroll"),
                ) {
                    if (deleteFailed != null) {
                        FailureBanner(stringResource(R.string.run_delete_failed), deleteFailed, "run-delete-failed")
                        Spacer(Modifier.height(12.dp))
                    }
                    Spacer(Modifier.height(4.dp))
                    CrewResultCrewRow(ui)
                    Spacer(Modifier.height(16.dp))
                    // 거리 + 작고 단 한 번의 저장 표시(이 화면은 기록이 이 휴대폰에 저장된 뒤에만 보인다)
                    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                        FitNumber(
                            ui.distance, "km", valueSize = 60.sp, unitSize = 24.sp, valueColor = c.text, unitColor = c.label,
                            modifier = Modifier.weight(1f), valueTag = "run-result-distance",
                        )
                        Spacer(Modifier.width(8.dp))
                        CrewSavedMark()
                    }
                    Spacer(Modifier.height(12.dp))
                    DividedRow(columns = 2, gap = 20.dp) {
                        CrewStat(stringResource(R.string.run_label_time), ui.time, null, "run-result-time", 26.sp)
                        CrewStat(stringResource(R.string.run_label_pace), ui.pace, "/km", "run-result-pace", 26.sp)
                    }
                    Spacer(Modifier.height(16.dp))
                    CrewResultMap(map, mapHeight)
                    if (ui.basis != null) {
                        Text(ui.basis, style = runTextStyle(13.sp, c.label), modifier = Modifier.padding(top = 8.dp).testTag("run-result-basis"))
                    }
                    Spacer(Modifier.height(14.dp))
                    CrewRewardCard(reward, onOpen = { rewardInfo = true }, onReason = onReason, onHistory = onHistory)
                    if (ui.note != null) {
                        Spacer(Modifier.height(10.dp))
                        Text(ui.note, style = runTextStyle(13.sp, c.label, FontWeight.Medium, 1.45f), modifier = Modifier.testTag("run-result-note"))
                    }
                    Spacer(Modifier.height(12.dp))
                }
            }
            // 크루에 공유가 주 행동, 완료는 조용한 글자 행동(같은 무게의 큰 버튼 둘을 두지 않는다)
            Column(Modifier.fillMaxWidth().padding(horizontal = 20.dp).padding(top = 8.dp, bottom = 8.dp)) {
                if (onShare != null) {
                    FreeRunButton(shareLabel, onShare, Modifier.fillMaxWidth().testTag("run-result-share"),
                        icon = Icons.AutoMirrored.Outlined.Send)
                    CrewTextAction(
                        stringResource(R.string.finish_done), onDone, Modifier.fillMaxWidth().padding(top = 4.dp).testTag("run-result-done"),
                        center = true,
                    )
                } else {
                    FreeRunButton(stringResource(R.string.finish_done), onDone, Modifier.fillMaxWidth().testTag("run-result-done"))
                }
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
    if (rewardInfo) CrewRewardSheet(reward, staminaNote, onClose = { rewardInfo = false })
}

/** 크루 표시 · 크루 이름 · 함께 출발한 인원(출발 명단 — 지금 남은 사람 수가 아니다). 혼자 출발했으면 "혼자 출발" */
@Composable
private fun CrewResultCrewRow(ui: CrewResultUi) {
    val c = crewColors()
    Row(Modifier.fillMaxWidth().semantics(mergeDescendants = true) {}, verticalAlignment = Alignment.CenterVertically) {
        CrewMark(ui.monogram?.takeIf { it.isNotBlank() } ?: initialsOf(ui.crewName), 50.dp)
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Text(
                ui.crewName, style = runTextStyle(17.sp, c.text, FontWeight.Bold, 1.3f), maxLines = 1, overflow = TextOverflow.Ellipsis,
                modifier = Modifier.testTag("run-result-crew-name"),
            )
            Text(
                if (ui.started > 1) stringResource(R.string.run_crew_started_n, ui.started) else stringResource(R.string.run_crew_started_solo),
                style = runTextStyle(14.sp, c.label, FontWeight.Medium, 1.35f), maxLines = 1, overflow = TextOverflow.Ellipsis,
                modifier = Modifier.testTag("run-result-crew"),
            )
        }
    }
}

/** "✓ 저장됨" — 기록이 이 휴대폰에 저장됐다는 것 하나. 보상 확인과는 다르다(보상 줄이 따로 말한다) */
@Composable
private fun CrewSavedMark() {
    val c = crewColors()
    Row(Modifier.semantics(mergeDescendants = true) {}, verticalAlignment = Alignment.CenterVertically) {
        Icon(Icons.Filled.CheckCircle, contentDescription = null, tint = c.ready, modifier = Modifier.size(20.dp))
        Spacer(Modifier.width(5.dp))
        Text(stringResource(R.string.run_crew_saved), style = runTextStyle(15.sp, c.ready, FontWeight.Bold),
            modifier = Modifier.testTag("run-result-saved"))
    }
}

/** 결과 지도 — 내 경로만(다른 사람의 위치는 남기지 않는다), 밝은 실제 지도. 경로가 없으면 꾸미지 않고 그렇다고 적는다 */
@Composable
private fun CrewResultMap(map: (@Composable BoxScope.() -> Unit)?, height: androidx.compose.ui.unit.Dp) {
    val c = crewColors()
    val t = runTone()
    val shape = RoundedCornerShape(16.dp)
    Box(
        Modifier.fillMaxWidth().height(height).clip(shape).background(t.inset).border(1.dp, c.line, shape)
            .testTag("run-result-map-frame"),
    ) {
        if (map != null) {
            CompositionLocalProvider(LocalMapTone provides CrewBrightMap) {
                Box(Modifier.fillMaxSize(), content = map)
            }
        } else {
            RunMapPlaceholder(Icons.Outlined.Map, stringResource(R.string.run_result_route_none), null, Modifier.testTag("run-result-no-route"))
        }
    }
}

/**
 * 이번 러닝 보상 한 줄 — 서버가 확인하기 전에는 이 폰이 셈한 "예상 · 확인 중"뿐이다. 확인되면 "반영 완료", 무효는 "지급 제외",
 * 거절 · 적립 없음은 "미지급". 늘 붙던 설명(확인 후 반영 · 반영됨 · 기준 미달)은 누르면 여는 시트로 옮겼다.
 * 문제를 푸는 행동(다시 로그인 · 지급 제외 까닭 · 보상 내역)과 그 까닭 줄은 카드 안에 그대로 둔다.
 * 이름표는 결과 화면 보상 칸([ResultRewardCard])과 같다(run-result-settle-* · run-result-reward · run-result-reward-note).
 */
@Composable
private fun CrewRewardCard(reward: ResultReward, onOpen: () -> Unit, onReason: () -> Unit, onHistory: () -> Unit) {
    val c = crewColors()
    val shape = RoundedCornerShape(16.dp)
    val label = rewardLabel(reward)
    Column(
        Modifier.fillMaxWidth().clip(shape).background(c.card, shape).border(1.dp, c.line, shape).testTag("run-result-reward-card"),
    ) {
        Row(
            Modifier.fillMaxWidth().heightIn(min = 64.dp).feedbackClickable(onClick = onOpen).padding(horizontal = 14.dp, vertical = 10.dp)
                .testTag("run-result-reward-open"),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(Modifier.size(42.dp).clip(RoundedCornerShape(12.dp)).background(c.badge), contentAlignment = Alignment.Center) {
                Icon(Icons.Outlined.CardGiftcard, contentDescription = null, tint = c.badgeInk, modifier = Modifier.size(22.dp))
            }
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(label, style = runTextStyle(13.sp, c.label, FontWeight.Medium), maxLines = 1, overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.testTag("run-result-reward-label"))
                // 금액은 예상 · 반영 완료일 때만 — 지급 제외 · 미지급에 "0 SUP"를 크게 적지 않는다
                if (reward.settle == Settle.PENDING || reward.settle == Settle.DONE) {
                    Text("${reward.amount} SUP", style = runTextStyle(20.sp, c.text, FontWeight.ExtraBold, 1.2f),
                        modifier = Modifier.testTag("run-result-reward"))
                }
            }
            Spacer(Modifier.width(8.dp))
            CrewSettleChip(reward.settle)
            Icon(Icons.Filled.ChevronRight, contentDescription = null, tint = c.label, modifier = Modifier.size(20.dp))
        }
        // 까닭 줄(다시 로그인 · 미지급 까닭)만 "run-result-reward-note" — 결과 화면 보상 칸(ResultRewardCard)과 같은 이름표다
        val reasons = when (reward.settle) {
            Settle.PENDING -> reward.notes
            Settle.NOT_PAID -> listOf(reward.notes.firstOrNull() ?: stringResource(R.string.run_reward_not_paid_note))
            Settle.DONE, Settle.VOID -> emptyList()
        }
        val fix = reward.settle == Settle.VOID || reward.settle == Settle.NOT_PAID || (reward.settle == Settle.PENDING && reward.signIn)
        if (reasons.isNotEmpty() || fix) {
            Column(Modifier.fillMaxWidth().padding(start = 14.dp, end = 14.dp, bottom = 10.dp)) {
                CrewLine(Modifier.padding(bottom = 8.dp))
                reasons.forEach { line ->
                    Text(line, style = runTextStyle(13.sp, c.label, FontWeight.Normal, 1.45f), modifier = Modifier.testTag("run-result-reward-note"))
                }
                when (reward.settle) {
                    Settle.VOID -> CrewTextAction(stringResource(R.string.run_details_more), onReason,
                        Modifier.testTag("run-result-void-reason"), chevron = true)
                    Settle.NOT_PAID -> CrewTextAction(stringResource(R.string.run_reward_history), onHistory,
                        Modifier.testTag("run-result-history"), chevron = true)
                    Settle.PENDING -> if (reward.signIn) {
                        Spacer(Modifier.height(8.dp))
                        com.stepup.android.ui.components.SignInAgainButton()
                    }
                    Settle.DONE -> Unit
                }
            }
        }
    }
}

/** 보상 줄의 이름 — 확인 전에는 "예상 보상", 그 밖에는 "이번 러닝 SUP" */
@Composable
private fun rewardLabel(reward: ResultReward): String =
    if (reward.settle == Settle.PENDING) reward.label else stringResource(R.string.run_reward_this)

/** 정산 상태 알약 — 예상 · 확인 중 / 반영 완료 / 지급 제외 / 미지급(이름표는 예전 결과와 같다) */
@Composable
private fun CrewSettleChip(settle: Settle) {
    val (text, tag) = when (settle) {
        Settle.PENDING -> stringResource(R.string.run_crew_reward_estimate) to "run-result-settle-pending"
        Settle.DONE -> stringResource(R.string.run_reward_done) to "run-result-settle-done"
        Settle.VOID -> stringResource(R.string.run_reward_void) to "run-result-settle-void"
        Settle.NOT_PAID -> stringResource(R.string.run_reward_not_paid) to "run-result-settle-rejected"
    }
    CrewChip(text, tag, done = settle == Settle.DONE)
}

/** 보상 설명 — 누를 때만. 지금 상태 · 금액 · 늘 붙던 안내 한 줄(확인 후 반영 · 반영됨 · 기준 미달 · 내역 확인) */
@Composable
private fun CrewRewardSheet(reward: ResultReward, staminaNote: String?, onClose: () -> Unit) {
    val c = crewColors()
    val hint = when (reward.settle) {
        Settle.PENDING -> stringResource(R.string.run_reward_note)
        Settle.DONE -> stringResource(R.string.run_reward_done_note)
        Settle.VOID -> stringResource(R.string.run_reward_void_note)
        Settle.NOT_PAID -> stringResource(R.string.run_reward_not_paid_note)
    }
    RunSheet(onDismiss = onClose, modifier = Modifier.testTag("run-result-reward-sheet")) {
        CrewSheetTitle(rewardLabel(reward))
        Spacer(Modifier.height(12.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            if (reward.settle == Settle.PENDING || reward.settle == Settle.DONE) {
                Text("${reward.amount} SUP", style = runTextStyle(24.sp, c.text, FontWeight.ExtraBold, 1.2f))
                Spacer(Modifier.width(10.dp))
            }
            CrewSettleChip(reward.settle)
        }
        Spacer(Modifier.height(10.dp))
        Text(hint, style = runTextStyle(15.sp, c.label, FontWeight.Medium, 1.5f), modifier = Modifier.testTag("run-result-reward-hint"))
        if (staminaNote != null) {
            Spacer(Modifier.height(8.dp))
            Text(staminaNote, style = runTextStyle(15.sp, c.text, FontWeight.SemiBold, 1.5f), modifier = Modifier.testTag("run-result-stamina"))
        }
        Spacer(Modifier.height(16.dp))
        FreeRunButton(stringResource(R.string.common_confirm), onClose, Modifier.fillMaxWidth().testTag("run-result-reward-close"))
    }
}

@Composable
private fun CrewChip(text: String, tag: String, done: Boolean = false) {
    val c = crewColors()
    Text(
        text, style = runTextStyle(12.sp, if (done) c.successInk else c.noticeInk, FontWeight.SemiBold, 1.2f), maxLines = 1,
        modifier = Modifier.clip(RoundedCornerShape(12.dp)).background(if (done) c.success else c.notice)
            .padding(horizontal = 9.dp, vertical = 4.dp).testTag(tag),
    )
}
