package com.stepup.android.ui.screens.community.crew

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.stepup.android.R
import com.stepup.android.core.ServiceLocator
import com.stepup.android.data.repo.CrewOutcome
import com.stepup.android.domain.CrewCard
import com.stepup.android.domain.CrewLevelInfo
import com.stepup.android.domain.CrewRules
import com.stepup.android.ui.components.RunSpinner
import com.stepup.android.ui.experience.FeedbackCue
import com.stepup.android.ui.experience.feedbackClickable

/**
 * 11 레벨 안내 · 68 레벨 조회 실패 — 레벨은 목록과 따로 읽는다. 레벨만 못 읽어도 다른 크루 정보는 그대로 보인다.
 * 저장된 레벨이 없으면 "새 크루"(레벨 계산식은 정해지지 않았다 — 여기서 만들지 않는다). 주간 참여 · 거리는 레벨 산식이 아닌 별개 현황.
 */
@Composable
internal fun CrewLevelSheet(card: CrewCard, onDismiss: () -> Unit) {
    val ink = crewInk()
    var attempt by rememberSaveable { mutableIntStateOf(0) }
    var state by remember(card.id) { mutableStateOf<CrewLoad<CrewLevelInfo>>(CrewLoad.Loading) }
    LaunchedEffect(card.id, attempt) {
        state = CrewLoad.Loading
        state = when (val result = ServiceLocator.crewCards.level(card.id)) {
            is CrewOutcome.Ok -> CrewLoad.Ready(result.value)
            is CrewOutcome.Failed -> CrewLoad.Failed(result.problem)
        }
    }
    when (val current = state) {
        is CrewLoad.Failed -> CrewSheet(null, onDismiss, Modifier.testTag("crew-level-error")) {
            // 68 — 가운데 제목 · 설명 · 다시 확인(레벨 0 · 새 크루로 바꿔 보이지 않는다)
            Box(Modifier.fillMaxWidth()) {
                CrewSheetClose(onDismiss, Modifier.align(Alignment.TopEnd))
                Column(Modifier.fillMaxWidth().padding(top = 44.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        stringResource(R.string.crew_level_error_title), style = crewTitleStyle(ink.text, 25.sp),
                        textAlign = TextAlign.Center,
                    )
                    Spacer(Modifier.height(12.dp))
                    Text(stringResource(R.string.crew_level_error_body), color = ink.text, fontSize = 16.sp, lineHeight = 25.sp, textAlign = TextAlign.Center)
                }
            }
            Spacer(Modifier.height(26.dp))
            CrewButton(stringResource(R.string.crew_level_retry), { attempt++ }, Modifier.testTag("crew-level-retry"))
        }
        else -> CrewSheet(stringResource(R.string.crew_level_title), onDismiss, Modifier.testTag("crew-level-sheet")) {
            val info = (current as? CrewLoad.Ready)?.value
            Spacer(Modifier.height(10.dp))
            if (info == null) {
                Box(Modifier.fillMaxWidth().heightIn(min = 180.dp), contentAlignment = Alignment.Center) {
                    RunSpinner(Modifier.size(32.dp))
                }
            } else {
                Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
                    Box(
                        Modifier.clip(RoundedCornerShape(12.dp)).background(ink.level).padding(horizontal = 26.dp, vertical = 8.dp),
                        contentAlignment = Alignment.Center,
                    ) {
                        Text(
                            if (info.level != null) stringResource(R.string.crew_level_value, info.level) else stringResource(R.string.crew_level_new),
                            color = ink.text, fontSize = 30.sp, fontWeight = FontWeight.ExtraBold, modifier = Modifier.testTag("crew-level-value"),
                        )
                    }
                    Spacer(Modifier.height(14.dp))
                    Text(card.name, style = crewTitleStyle(ink.text, 25.sp), textAlign = TextAlign.Center)
                    Spacer(Modifier.height(8.dp))
                    Text(
                        if (info.level != null) stringResource(R.string.crew_level_about) else stringResource(R.string.crew_level_about_new),
                        color = ink.text, fontSize = 16.sp, lineHeight = 24.sp, textAlign = TextAlign.Center,
                    )
                }
                Spacer(Modifier.height(18.dp))
                Box(Modifier.fillMaxWidth().height(1.dp).background(ink.divider))
                Text(
                    if (info.weekRunners > 0) {
                        stringResource(R.string.crew_level_week, info.weekRunners, CrewRules.km(info.weekKm))
                    } else {
                        stringResource(R.string.crew_level_week_none)
                    },
                    color = ink.text, fontSize = 16.sp, lineHeight = 25.sp, modifier = Modifier.padding(vertical = 16.dp),
                )
                Box(Modifier.fillMaxWidth().height(1.dp).background(ink.divider))
            }
            Spacer(Modifier.height(22.dp))
            CrewButton(stringResource(R.string.crew_ok), onDismiss, Modifier.testTag("crew-level-ok"))
        }
    }
}

/** 제목 없는 시트의 오른쪽 위 닫기 */
@Composable
internal fun CrewSheetClose(onClose: () -> Unit, modifier: Modifier = Modifier, enabled: Boolean = true) {
    val ink = crewInk()
    Box(
        modifier.size(48.dp).clip(CircleShape)
            .feedbackClickable(enabled = enabled, role = Role.Button, cue = FeedbackCue.Back, onClick = onClose)
            .testTag("crew-sheet-close"),
        contentAlignment = Alignment.Center,
    ) {
        Icon(Icons.Filled.Close, stringResource(R.string.common_close), tint = ink.text, modifier = Modifier.size(26.dp))
    }
}

/** 더보기 줄 하나 — [icon] 이 있으면 앞에 선 그림 */
internal data class CrewMenuItem(
    val label: String,
    val tag: String,
    val warn: Boolean = false,
    val icon: ImageVector? = null,
    val onClick: () -> Unit,
)

/** 73 방문자용 · 60 멤버용 더보기 · 75 멤버 관리 — 테두리 있는 칸들(나가기 · 내보내기는 코랄 글자) */
@Composable
internal fun CrewMenuSheet(title: String, items: List<CrewMenuItem>, onDismiss: () -> Unit, tag: String) {
    CrewSheet(title, onDismiss, Modifier.testTag(tag)) {
        Spacer(Modifier.height(16.dp))
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            items.forEach { item ->
                CrewMenuRow(item.label, item.onClick, Modifier.testTag(item.tag), icon = item.icon, warn = item.warn)
            }
        }
        Spacer(Modifier.height(12.dp))
    }
}

/** 신고 사유 — 기존 신고함의 분류로 보낸다(부적절한 모집 내용은 기타 + 메모) */
internal enum class CrewReportReason(val code: String, val note: String) {
    SPAM("SPAM", ""),
    CONTENT("OTHER", "RECRUIT_CONTENT"),
    ABUSE("ABUSE", ""),
    OTHER("OTHER", ""),
}

/**
 * 64 크루 신고 — 사유 하나를 골라야 보낼 수 있다(기타만 메모 500자). 실패하면 고른 사유와 쓴 내용을 그대로 두고 다시 보낸다.
 * 서버가 받은 뒤에만 [onSent](65).
 */
@Composable
internal fun CrewReportSheet(
    busy: Boolean,
    failed: Boolean,
    onSend: (reason: String, note: String) -> Unit,
    onDismiss: () -> Unit,
) {
    var picked by rememberSaveable { mutableStateOf("") }
    var other by rememberSaveable { mutableStateOf("") }
    CrewSheet(stringResource(R.string.crew_report_title), onDismiss, Modifier.testTag("crew-report-sheet"), dismissible = !busy) {
        Spacer(Modifier.height(14.dp))
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            CrewReportReason.entries.forEach { reason ->
                CrewRadioRow(
                    stringResource(
                        when (reason) {
                            CrewReportReason.SPAM -> R.string.crew_report_spam
                            CrewReportReason.CONTENT -> R.string.crew_report_content
                            CrewReportReason.ABUSE -> R.string.crew_report_abuse
                            CrewReportReason.OTHER -> R.string.crew_report_other
                        },
                    ),
                    selected = picked == reason.name,
                    enabled = !busy,
                    onClick = { picked = reason.name },
                    modifier = Modifier.testTag("crew-report-${reason.name}"),
                )
            }
        }
        if (picked == CrewReportReason.OTHER.name) {
            Spacer(Modifier.height(12.dp))
            CrewTextField(
                other, { other = it }, Modifier.testTag("crew-report-note"),
                placeholder = stringResource(R.string.crew_report_other_hint), singleLine = false, minHeight = 96.dp, maxChars = 500,
            )
            Spacer(Modifier.height(6.dp))
            CrewHelp(stringResource(R.string.crew_blue_count, other.length, 500), modifier = Modifier.align(Alignment.End))
        }
        if (failed) {
            Spacer(Modifier.height(12.dp))
            CrewErrorLine(stringResource(R.string.crew_report_failed), Modifier.testTag("crew-report-failed"), boxed = true)
        }
        Spacer(Modifier.height(20.dp))
        CrewButton(
            stringResource(R.string.crew_report_send),
            {
                val reason = CrewReportReason.entries.firstOrNull { it.name == picked } ?: return@CrewButton
                val note = if (reason == CrewReportReason.OTHER) other.trim() else reason.note
                onSend(reason.code, note)
            },
            Modifier.testTag("crew-report-send"),
            enabled = picked.isNotEmpty(),
            busy = busy,
        )
    }
}

/** 하나만 고르는 줄 — 왼쪽 동그라미(고르면 청록 체크) · 이름 · 오른쪽 "선택됨" */
@Composable
internal fun CrewRadioRow(text: String, selected: Boolean, onClick: () -> Unit, modifier: Modifier = Modifier, enabled: Boolean = true) {
    val ink = crewInk()
    Row(
        modifier.fillMaxWidth().heightIn(min = 56.dp).crewPanel(ink, 12.dp, selected = selected)
            .feedbackClickable(enabled = enabled, role = Role.RadioButton, cue = FeedbackCue.Select, onClick = onClick)
            .semantics { this.selected = selected }
            .padding(horizontal = 16.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            Modifier.size(26.dp).clip(CircleShape)
                .then(if (selected) Modifier.background(ink.info) else Modifier.border(1.5.dp, ink.secondary, CircleShape)),
            contentAlignment = Alignment.Center,
        ) {
            if (selected) Icon(Icons.Filled.Check, null, tint = if (com.stepup.android.ui.theme.StepUpColors.dark) Color(0xFF071B3D) else Color.White, modifier = Modifier.size(18.dp))
        }
        Spacer(Modifier.width(14.dp))
        Text(text, color = ink.text, fontSize = 17.sp, fontWeight = FontWeight.SemiBold, modifier = Modifier.weight(1f))
        if (selected) Text(stringResource(R.string.crew_selected), color = ink.info, fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
    }
}

/** 24 신청 중 모집 상태 변경 — 신청은 끝나지 않았고 쓴 한마디는 남긴다. 지금 상태를 다시 읽는다 */
@Composable
internal fun CrewUnavailableSheet(onShow: () -> Unit, onDismiss: () -> Unit) {
    val ink = crewInk()
    CrewSheet(stringResource(R.string.crew_unavailable_title), onDismiss, Modifier.testTag("crew-unavailable")) {
        Spacer(Modifier.height(12.dp))
        Text(stringResource(R.string.crew_unavailable_body), color = ink.text, fontSize = 16.sp, lineHeight = 25.sp)
        Spacer(Modifier.height(26.dp))
        CrewButton(stringResource(R.string.crew_unavailable_show), onShow, Modifier.testTag("crew-unavailable-show"))
    }
}

/** 관리 작업 실패(80) — 서버가 성공하기 전 상태를 확정하지 않는다. "다시 확인"은 크루 · 신청을 다시 읽는다 */
@Composable
internal fun CrewActionErrorSheet(onRecheck: () -> Unit, onDismiss: () -> Unit, body: String? = null) {
    val ink = crewInk()
    CrewSheet(stringResource(R.string.crew_action_error_title), onDismiss, Modifier.testTag("crew-action-error")) {
        Spacer(Modifier.height(12.dp))
        Text(body ?: stringResource(R.string.crew_action_error_body), color = ink.text, fontSize = 16.sp, lineHeight = 25.sp)
        Spacer(Modifier.height(26.dp))
        CrewButton(stringResource(R.string.crew_action_error_recheck), onRecheck, Modifier.testTag("crew-action-error-recheck"))
    }
}

/** 한 번 쓰는 설명 문단(시트 본문) */
@Composable
internal fun CrewSheetBody(text: String, modifier: Modifier = Modifier) {
    val ink = crewInk()
    Column(modifier.padding(top = 12.dp)) { Text(text, color = ink.text, fontSize = 16.sp, lineHeight = 25.sp) }
}
