package com.stepup.android.ui.screens.community.crew

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.stepup.android.R
import com.stepup.android.core.ServiceLocator
import com.stepup.android.data.repo.CrewOutcome
import com.stepup.android.domain.CrewCard
import com.stepup.android.domain.CrewLevelInfo
import com.stepup.android.domain.CrewRules

/**
 * 11 레벨 안내 · 68 레벨 조회 실패 — 레벨은 목록과 따로 읽는다. 레벨만 못 읽어도 다른 크루 정보는 그대로 보인다.
 * 저장된 레벨이 없으면 "새 크루"(레벨 계산식은 정해지지 않았다 — 여기서 만들지 않는다).
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
        is CrewLoad.Failed -> CrewSheet(stringResource(R.string.crew_level_error_title), onDismiss, Modifier.testTag("crew-level-error")) {
            Spacer(Modifier.height(18.dp))
            Text(stringResource(R.string.crew_level_error_body), color = ink.secondary, fontSize = 15.sp, lineHeight = 25.sp)
            Spacer(Modifier.height(140.dp))
            CrewButton(stringResource(R.string.crew_level_retry), { attempt++ }, Modifier.testTag("crew-level-retry"))
        }
        else -> CrewSheet(stringResource(R.string.crew_level_title), onDismiss, Modifier.testTag("crew-level-sheet")) {
            val info = (current as? CrewLoad.Ready)?.value
            Spacer(Modifier.height(18.dp))
            if (info == null) {
                Box(Modifier.fillMaxWidth().heightIn(min = 180.dp), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(Modifier.size(28.dp), color = ink.info, strokeWidth = 2.5.dp)
                }
            } else {
                Text(
                    if (info.level != null) stringResource(R.string.crew_level_value, info.level) else stringResource(R.string.crew_level_new),
                    color = ink.text, fontSize = 40.sp, fontWeight = FontWeight.Bold, modifier = Modifier.testTag("crew-level-value"),
                )
                Spacer(Modifier.height(8.dp))
                Text(card.name, color = ink.text, fontSize = 20.sp, fontWeight = FontWeight.SemiBold)
                Spacer(Modifier.height(30.dp))
                Text(
                    if (info.level != null) stringResource(R.string.crew_level_about) else stringResource(R.string.crew_level_about_new),
                    color = ink.secondary, fontSize = 15.sp, lineHeight = 25.sp,
                )
                Text(
                    if (info.weekRunners > 0) {
                        stringResource(R.string.crew_level_week, info.weekRunners, CrewRules.km(info.weekKm))
                    } else {
                        stringResource(R.string.crew_level_week_none)
                    },
                    color = ink.secondary, fontSize = 15.sp, lineHeight = 25.sp,
                )
            }
            Spacer(Modifier.height(90.dp))
            CrewButton(stringResource(R.string.crew_ok), onDismiss, Modifier.testTag("crew-level-ok"))
        }
    }
}

/** 더보기 줄 하나 */
internal data class CrewMenuItem(val label: String, val tag: String, val warn: Boolean = false, val onClick: () -> Unit)

/** 73 방문자용 · 60 멤버용 더보기 */
@Composable
internal fun CrewMenuSheet(title: String, items: List<CrewMenuItem>, onDismiss: () -> Unit, tag: String) {
    val ink = crewInk()
    CrewSheet(title, onDismiss, Modifier.testTag(tag)) {
        Spacer(Modifier.height(16.dp))
        items.forEach { item ->
            CrewRow(item.label, item.onClick, Modifier.testTag(item.tag), titleColor = if (item.warn) ink.warn else null)
        }
        Spacer(Modifier.height(48.dp))
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
 * 64 크루 신고 — 사유를 골라야 보낼 수 있다. 실패하면 고른 사유와 쓴 내용을 그대로 두고 다시 보낸다.
 * 서버가 받은 뒤에만 [onSent](65).
 */
@Composable
internal fun CrewReportSheet(
    busy: Boolean,
    failed: Boolean,
    onSend: (reason: String, note: String) -> Unit,
    onDismiss: () -> Unit,
) {
    val ink = crewInk()
    var picked by rememberSaveable { mutableStateOf("") }
    var other by rememberSaveable { mutableStateOf("") }
    CrewSheet(stringResource(R.string.crew_report_title), onDismiss, Modifier.testTag("crew-report-sheet"), dismissible = !busy) {
        Spacer(Modifier.height(10.dp))
        CrewReportReason.entries.forEach { reason ->
            CrewRow(
                stringResource(
                    when (reason) {
                        CrewReportReason.SPAM -> R.string.crew_report_spam
                        CrewReportReason.CONTENT -> R.string.crew_report_content
                        CrewReportReason.ABUSE -> R.string.crew_report_abuse
                        CrewReportReason.OTHER -> R.string.crew_report_other
                    },
                ),
                { picked = reason.name },
                Modifier.testTag("crew-report-${reason.name}"),
                value = if (picked == reason.name) stringResource(R.string.crew_selected) else null,
            )
        }
        if (picked == CrewReportReason.OTHER.name) {
            Spacer(Modifier.height(14.dp))
            CrewTextField(
                other, { other = it }, Modifier.testTag("crew-report-note"),
                placeholder = stringResource(R.string.crew_report_other_hint), singleLine = false, minHeight = 88.dp, maxChars = 500,
            )
        }
        if (failed) {
            Spacer(Modifier.height(12.dp))
            Text(stringResource(R.string.crew_report_failed), color = ink.warn, fontSize = 13.sp, modifier = Modifier.testTag("crew-report-failed"))
        }
        Spacer(Modifier.height(40.dp))
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

/** 24 신청 중 모집 상태 변경 — 쓴 한마디는 남기고 지금 상태를 다시 읽는다 */
@Composable
internal fun CrewUnavailableSheet(onShow: () -> Unit, onDismiss: () -> Unit) {
    val ink = crewInk()
    CrewSheet(stringResource(R.string.crew_unavailable_title), onDismiss, Modifier.testTag("crew-unavailable")) {
        Spacer(Modifier.height(18.dp))
        Text(stringResource(R.string.crew_unavailable_body), color = ink.secondary, fontSize = 15.sp, lineHeight = 25.sp)
        Spacer(Modifier.height(120.dp))
        CrewButton(stringResource(R.string.crew_unavailable_show), onShow, Modifier.testTag("crew-unavailable-show"))
    }
}

/** 관리 작업 실패(80) — 서버가 성공하기 전 상태를 확정하지 않는다 */
@Composable
internal fun CrewActionErrorSheet(onRecheck: () -> Unit, onDismiss: () -> Unit, body: String? = null) {
    val ink = crewInk()
    CrewSheet(stringResource(R.string.crew_action_error_title), onDismiss, Modifier.testTag("crew-action-error")) {
        Spacer(Modifier.height(18.dp))
        Text(body ?: stringResource(R.string.crew_action_error_body), color = ink.secondary, fontSize = 15.sp, lineHeight = 25.sp)
        Spacer(Modifier.height(120.dp))
        CrewButton(stringResource(R.string.crew_action_error_recheck), onRecheck, Modifier.testTag("crew-action-error-recheck"))
    }
}

/** 한 번 쓰는 설명 문단(시트 본문) */
@Composable
internal fun CrewSheetBody(text: String, modifier: Modifier = Modifier) {
    val ink = crewInk()
    Column(modifier.padding(top = 18.dp)) { Text(text, color = ink.secondary, fontSize = 15.sp, lineHeight = 25.sp) }
}
