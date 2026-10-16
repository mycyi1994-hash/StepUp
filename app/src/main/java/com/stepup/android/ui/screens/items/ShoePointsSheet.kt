package com.stepup.android.ui.screens.items

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.stepup.android.R
import com.stepup.android.core.ServiceLocator
import com.stepup.android.data.repo.EconomyOutcome
import com.stepup.android.domain.Sneaker
import com.stepup.android.ui.components.RunButton
import com.stepup.android.ui.components.RunButtonKind
import com.stepup.android.ui.components.RunDivider
import com.stepup.android.ui.components.RunSheet
import com.stepup.android.ui.components.runTextStyle
import com.stepup.android.ui.components.runTone
import com.stepup.android.ui.experience.ExperienceEvents
import com.stepup.android.ui.experience.FeedbackCue
import com.stepup.android.ui.experience.feedbackClickable
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.first

/*
 * 스탯 포인트 나누기 시트(서버 0059) — 상세 위의 하단 시트 하나. 남은 포인트 · 세 줄(효율 · 착화감 · 내구도, ± 로 나눔) ·
 * 주 버튼 하나("포인트 나누기"). 되돌릴 수 없어 한 번 더 확인하고, 서버가 받았다고 답한 뒤에만 "나눴어요"라고 쓴다.
 * 계산은 ShoePointsModel 의 순수 함수 — 서버가 같은 규칙으로 다시 확인한다.
 */

/** 시트의 손잡이 — 열기 · ± · 나누기(확인으로) · 확인 취소 · 보내기 · 닫기 */
data class PointsActions(
    val onOpen: () -> Unit = {},
    val onAdd: (PointStat) -> Unit = {},
    val onRemove: (PointStat) -> Unit = {},
    val onReview: () -> Unit = {},
    val onBack: () -> Unit = {},
    val onSend: () -> Unit = {},
    val onClose: () -> Unit = {},
)

/** 시트 고유 이름 — 테스트 · 캡처가 지금 어느 장면인지 본다 */
fun PointsPhase.sceneTag(): String = "points-" + when (this) {
    is PointsPhase.Editing -> when (error) {
        null -> "editing"
        PointsError.OFFLINE -> "offline"
        PointsError.REJECTED -> "rejected"
        PointsError.SIGN_IN -> "sign-in"
    }
    is PointsPhase.Confirm -> "confirm"
    is PointsPhase.Sending -> "sending"
    is PointsPhase.Done -> "done"
}

/**
 * 서버에 보낸다(앱 저장소 — 같은 초안은 같은 요청 번호). 답을 받은 뒤 최신 신발로 다음 장면을 정한다.
 * 연결이 끊기면 결과를 단정하지 않는다(같은 초안으로 다시 보낼 수 있게 남긴다).
 */
internal suspend fun sendPoints(sending: PointsPhase.Sending): PointsPhase {
    val draft = sending.draft
    val outcome = try {
        ServiceLocator.sneakerRepository.setPointsOnServer(sending.basis.shoeId, draft.efficiency, draft.comfort, draft.durability)
    } catch (cancelled: CancellationException) {
        throw cancelled
    } catch (_: Exception) {
        EconomyOutcome.Offline
    }
    val latest = try {
        ServiceLocator.sneakerRepository.inventory.first().firstOrNull { it.id == sending.basis.shoeId }?.let(::pointsBasisOf)
    } catch (cancelled: CancellationException) {
        throw cancelled
    } catch (_: Exception) {
        null
    }
    ExperienceEvents.emit(if (outcome == EconomyOutcome.Ok) FeedbackCue.Success else FeedbackCue.Error)
    return pointsAfterSend(sending, outcome, latest)
}

@Composable
fun ShoePointsSheet(
    shoe: Sneaker,
    phase: PointsPhase,
    actions: PointsActions,
    onSignIn: (() -> Unit)?,
) {
    val sending = phase is PointsPhase.Sending
    RunSheet(
        onDismiss = { if (!sending) actions.onClose() },
        modifier = Modifier.testTag("points-sheet"),
        dismissible = !sending,
        closeTag = "points-close",
    ) {
        Column(
            Modifier.fillMaxWidth().verticalScroll(rememberScrollState()).testTag(phase.sceneTag()),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            PointsBody(shoe, phase, actions, onSignIn)
        }
    }
}

@Composable
private fun ColumnScope.PointsBody(shoe: Sneaker, phase: PointsPhase, actions: PointsActions, onSignIn: (() -> Unit)?) {
    val t = runTone()
    val basis = phase.basis
    val draft = phase.draft
    when (phase) {
        is PointsPhase.Editing -> {
            CareSheetTitle(stringResource(R.string.points_title), shoe.nameAndNumber())
            FreeLine(basis.free(draft))
            PointStat.entries.forEach { stat ->
                PointRow(basis, draft, stat, editable = true, onAdd = { actions.onAdd(stat) }, onRemove = { actions.onRemove(stat) })
            }
            Text(
                stringResource(if (basis.respec) R.string.points_note_respec else R.string.points_note_final),
                style = runTextStyle(14.sp, t.label, FontWeight.Medium), modifier = Modifier.testTag("points-note"),
            )
            val error = phase.error
            if (error != null) {
                Text(
                    stringResource(
                        when (error) {
                            PointsError.OFFLINE -> R.string.points_error_offline
                            PointsError.REJECTED -> R.string.points_error_rejected
                            PointsError.SIGN_IN -> R.string.points_error_sign_in
                        },
                    ),
                    style = runTextStyle(14.sp, if (error == PointsError.OFFLINE) t.cyan else t.dangerText, FontWeight.SemiBold),
                    modifier = Modifier.testTag("points-error").semantics { liveRegion = LiveRegionMode.Polite },
                )
            }
            RunDivider()
            when {
                error == PointsError.SIGN_IN && onSignIn != null ->
                    RunButton(stringResource(R.string.care_sign_in), onSignIn, modifier = Modifier.testTag("points-sign-in"))
                error == PointsError.OFFLINE && phase.send() != null ->
                    RunButton(stringResource(R.string.points_retry), actions.onSend, modifier = Modifier.testTag("points-retry"))
                else -> RunButton(
                    stringResource(R.string.points_action), actions.onReview,
                    enabled = basis.changed(draft) && basis.valid(draft),
                    modifier = Modifier.testTag("points-save"),
                )
            }
        }
        is PointsPhase.Confirm, is PointsPhase.Sending -> {
            CareSheetTitle(stringResource(R.string.points_confirm_title), shoe.nameAndNumber())
            PointStat.entries.forEach { stat -> PointRow(basis, draft, stat, editable = false) }
            Text(
                stringResource(if (basis.lowers(draft)) R.string.points_confirm_respec else R.string.points_confirm_body),
                style = runTextStyle(15.sp, t.text, FontWeight.SemiBold), modifier = Modifier.testTag("points-confirm-note"),
            )
            RunDivider()
            if (phase is PointsPhase.Sending) {
                RunButton(stringResource(R.string.points_sending), {}, busy = true, modifier = Modifier.testTag("points-send"))
            } else {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    RunButton(stringResource(R.string.common_cancel), actions.onBack, kind = RunButtonKind.Secondary,
                        modifier = Modifier.weight(1f).testTag("points-back"))
                    RunButton(stringResource(R.string.points_send), actions.onSend, modifier = Modifier.weight(1f).testTag("points-send"))
                }
            }
        }
        is PointsPhase.Done -> {
            CareSheetTitle(stringResource(R.string.points_done_title), shoe.nameAndNumber())
            FreeLine(basis.free(draft))
            PointStat.entries.forEach { stat -> PointRow(basis, draft, stat, editable = false, showChange = false) }
            RunDivider()
            RunButton(stringResource(R.string.care_done), actions.onClose, modifier = Modifier.testTag("points-done"))
        }
    }
}

/** "남은 포인트 4" — 레벨마다 4개 */
@Composable
private fun FreeLine(free: Int) {
    val t = runTone()
    Row(
        Modifier.fillMaxWidth().semantics(mergeDescendants = true) {}.testTag("points-free"),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(stringResource(R.string.points_free), style = runTextStyle(16.sp, t.label, FontWeight.SemiBold))
        Spacer(Modifier.width(10.dp))
        Text("$free", style = runTextStyle(22.sp, t.cyan, FontWeight.ExtraBold), modifier = Modifier.testTag("points-free-value"))
        Spacer(Modifier.weight(1f))
        Text(stringResource(R.string.points_per_level), style = runTextStyle(13.sp, t.label, FontWeight.Medium))
    }
}

private fun signedBonus(bps: Int): String = formatBonus(bps / 100.0)
private fun percentOf(bps: Int): String = formatPercent(bps / 100.0)

/**
 * 한 줄 — 이름 · 지금 값(→ 나눈 뒤 값) · 1개의 값 · ± 와 나눈 수. [editable] 이 아니면 ± 없이 나눈 수만.
 * [showChange] 면 서버 값과 다를 때 화살표로 보인다(완료 장면은 끈다 — 이미 바뀐 값이다).
 */
@Composable
private fun PointRow(
    basis: PointsBasis,
    draft: PointsDraft,
    stat: PointStat,
    editable: Boolean,
    onAdd: () -> Unit = {},
    onRemove: () -> Unit = {},
    showChange: Boolean = true,
) {
    val label = stringResource(
        when (stat) {
            PointStat.EFFICIENCY -> R.string.my_shoes_stat_efficiency
            PointStat.COMFORT -> R.string.sdv_stat_comfort
            PointStat.DURABILITY -> R.string.sdv_stat_durability
        },
    )
    val hint = stringResource(
        when (stat) {
            PointStat.EFFICIENCY -> R.string.points_hint_efficiency
            PointStat.COMFORT -> R.string.points_hint_comfort
            PointStat.DURABILITY -> R.string.points_hint_durability
        },
    )
    val (now, next) = when (stat) {
        PointStat.EFFICIENCY -> signedBonus(basis.efficiencyBps(basis.saved)) to signedBonus(basis.efficiencyBps(draft))
        PointStat.COMFORT -> percentOf(basis.comfortBps(basis.saved)) to percentOf(basis.comfortBps(draft))
        PointStat.DURABILITY -> formatCount(basis.durabilityMax(basis.saved)) to formatCount(basis.durabilityMax(draft))
    }
    val changed = showChange && draft[stat] != basis.saved[stat]
    val accent = when (stat) {
        PointStat.EFFICIENCY -> CartoonColors.Efficiency.top
        PointStat.COMFORT -> CartoonColors.Comfort.top
        PointStat.DURABILITY -> CartoonColors.Durability.top
    }
    val key = stat.name.lowercase()
    val countCd = stringResource(R.string.points_count_cd, label, draft[stat])
    Column(
        Modifier.fillMaxWidth().heightIn(min = 64.dp).background(CartoonColors.Cell, RoundedCornerShape(14.dp))
            .testTag("points-row-$key").padding(horizontal = 14.dp, vertical = 10.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Text(label, style = runTextStyle(16.sp, CartoonColors.Text, FontWeight.SemiBold), maxLines = 1)
            Spacer(Modifier.width(8.dp))
            Text(
                buildAnnotatedString {
                    // 완료 장면은 이미 바뀐 값(초안)을, 그 밖에는 지금 값을 — 바뀌면 "지금 → 나눈 뒤"
                    append(if (showChange) now else next)
                    if (changed) {
                        append(" → ")
                        withStyle(SpanStyle(color = accent, fontWeight = FontWeight.ExtraBold)) { append(next) }
                    }
                },
                style = runTextStyle(15.sp, CartoonColors.Text, FontWeight.SemiBold).copy(fontFeatureSettings = "tnum"),
                modifier = Modifier.weight(1f).testTag("points-value-$key"),
                textAlign = androidx.compose.ui.text.style.TextAlign.End,
            )
        }
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Text(hint, style = runTextStyle(13.sp, CartoonColors.Number, FontWeight.Medium), modifier = Modifier.weight(1f))
            if (editable) {
                StepButton(Icons.Filled.Remove, stringResource(R.string.points_remove_cd, label), basis.canRemove(draft, stat), onRemove,
                    "points-remove-$key")
            }
            Text(
                "${draft[stat]}",
                style = runTextStyle(18.sp, CartoonColors.Text, FontWeight.ExtraBold).copy(fontFeatureSettings = "tnum"),
                textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                modifier = Modifier.widthIn(min = 40.dp).semantics { contentDescription = countCd }.testTag("points-count-$key"),
            )
            if (editable) {
                StepButton(Icons.Filled.Add, stringResource(R.string.points_add_cd, label), basis.canAdd(draft, stat), onAdd,
                    "points-add-$key")
            }
        }
    }
}

/** ± 동그라미 — 누를 수 있는 넓이 48dp, 누를 수 없으면 흐리게 */
@Composable
private fun StepButton(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    description: String,
    enabled: Boolean,
    onClick: () -> Unit,
    tag: String,
) {
    val t = runTone()
    Box(
        Modifier.size(48.dp).clip(CircleShape)
            .feedbackClickable(enabled = enabled, role = Role.Button, onClick = onClick)
            .semantics { contentDescription = description }
            .testTag(tag),
        contentAlignment = Alignment.Center,
    ) {
        Box(
            Modifier.size(36.dp).clip(CircleShape).background(t.cobalt, CircleShape)
                .border(1.dp, CartoonColors.FrameTop, CircleShape).alpha(if (enabled) 1f else 0.35f),
            contentAlignment = Alignment.Center,
        ) {
            Icon(icon, contentDescription = null, tint = androidx.compose.ui.graphics.Color.White, modifier = Modifier.size(20.dp))
        }
    }
}
