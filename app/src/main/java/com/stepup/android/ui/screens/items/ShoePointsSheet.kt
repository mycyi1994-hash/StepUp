package com.stepup.android.ui.screens.items

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
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
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import com.stepup.android.ui.components.StepUpUi
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
import com.stepup.android.domain.PointsPreview
import com.stepup.android.domain.Sneaker
import com.stepup.android.ui.components.RunButton
import com.stepup.android.ui.components.RunButtonKind
import com.stepup.android.ui.components.RunDivider
import com.stepup.android.ui.components.RunSheet
import com.stepup.android.ui.components.RunTextAction
import com.stepup.android.ui.components.runTextStyle
import com.stepup.android.ui.components.runTone
import com.stepup.android.ui.experience.ExperienceEvents
import com.stepup.android.ui.experience.FeedbackCue
import com.stepup.android.ui.experience.feedbackClickable
import com.stepup.android.ui.theme.StepUpDesign
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.first

/*
 * 스탯 포인트 나누기 시트(서버 0059) — 상세 위의 하단 시트 하나. 남은 포인트 · 세 줄(효율 · 착화감 · 내구도, ± 로 나눔) ·
 * 주 버튼 하나("포인트 나누기"). 되돌릴 수 없어 한 번 더 확인하고, 서버가 받았다고 답한 뒤에만 "나눴어요"라고 쓴다.
 * 계산은 ShoePointsModel 의 순수 함수 — 서버가 같은 규칙으로 다시 확인한다.
 *
 * 서버 0119(docs/economy-roots-contract-2026-11-21.md) — 능력치 줄마다 뜻 한 줄(효율은 신발 · 에너지도 빨리 쓴다)과, 미리보기가 있으면
 * "1포인트 = 하루 약 +N SUP"(내 지난 14일 러닝 기준 — 어림값), 가득에서 달리는 분(효율로 줄고 내구도로 는다), 글자 버튼 "추천대로 나누기".
 * 미리보기를 못 받았으면(0119 전 서버 · 연결 끊김) 그 칸만 없고 나누기는 예전 그대로다. 주 버튼은 여전히 하나다.
 */

/** 시트의 손잡이 — 열기 · ± · 추천대로 채우기 · 나누기(확인으로) · 확인 취소 · 보내기 · 닫기 */
data class PointsActions(
    val onOpen: () -> Unit = {},
    val onAdd: (PointStat) -> Unit = {},
    val onRemove: (PointStat) -> Unit = {},
    val onReview: () -> Unit = {},
    val onBack: () -> Unit = {},
    val onSend: () -> Unit = {},
    val onClose: () -> Unit = {},
    /** "추천대로 나누기" — 초안을 서버 추천(이미 나눈 값 + 더할 양)으로 채운다. 보내지 않는다 */
    val onRecommend: () -> Unit = {},
    /** "처음부터 추천대로" — 다시 나눌 수 있는 신발의 초안을 서버 추천(절대값)으로 바꾼다. 보내지 않는다 */
    val onRecommendAll: () -> Unit = {},
)

/** 시트 고유 이름 — 테스트 · 캡처가 지금 어느 장면인지 본다 */
fun PointsPhase.sceneTag(): String = "points-" + when (this) {
    is PointsPhase.Editing -> when (error) {
        null -> "editing"
        PointsError.OFFLINE -> "offline"
        PointsError.REJECTED -> "rejected"
        PointsError.REJECTED_NOT_RELOADED -> "rejected-stale"
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
    val (outcome, reloaded) = try {
        ServiceLocator.sneakerRepository.setPointsOnServerChecked(sending.basis.shoeId, draft.efficiency, draft.comfort, draft.durability)
    } catch (cancelled: CancellationException) {
        throw cancelled
    } catch (_: Exception) {
        EconomyOutcome.Offline to false
    }
    val latest = try {
        ServiceLocator.sneakerRepository.inventory.first().firstOrNull { it.id == sending.basis.shoeId }
            ?.let { pointsBasisOf(it, sending.basis.rules) }
    } catch (cancelled: CancellationException) {
        throw cancelled
    } catch (_: Exception) {
        null
    }
    ExperienceEvents.emit(if (outcome == EconomyOutcome.Ok) FeedbackCue.Success else FeedbackCue.Error)
    return pointsAfterSend(sending, outcome, latest, reloaded)
}

@Composable
fun ShoePointsSheet(
    shoe: Sneaker,
    phase: PointsPhase,
    actions: PointsActions,
    onSignIn: (() -> Unit)?,
    /** 처음 포인트가 생긴 사람([firstPointsOf]) — ⓘ 를 펼치면 "나눈 포인트는 되돌릴 수 없어요"도 함께 */
    firstPoints: Boolean = false,
    /** 서버가 내 지난 14일 러닝으로 셈한 미리보기(0119) — 없으면 그 칸을 보이지 않는다 */
    preview: PointsPreview? = null,
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
            PointsBody(shoe, phase, actions, onSignIn, firstPoints, preview)
        }
    }
}

@Composable
private fun ColumnScope.PointsBody(
    shoe: Sneaker,
    phase: PointsPhase,
    actions: PointsActions,
    onSignIn: (() -> Unit)?,
    firstPoints: Boolean,
    preview: PointsPreview?,
) {
    val t = runTone()
    val basis = phase.basis
    val draft = phase.draft
    // 능력치의 뜻(서버 0119) — 효율은 신발 · 에너지도 빨리 쓰고, 내구도는 가득 수리비를 올리지 않는다. 예전 서버면 예전 글
    // 효율 포인트가 닳는 속도를 올리는 규칙이면 가득에서 달리는 분을 따로 보인다(효율로 줄고 내구도로 는다)
    val lifeMoves = basis.rules.stats.effWearExp > 0.0
    // ⓘ — 펼친 줄 하나(창 없이 그 줄 아래). 다른 줄을 펼치면 앞의 것은 접힌다
    var infoOpen by rememberSaveable { mutableStateOf<PointStat?>(null) }
    val info = { stat: PointStat -> PointInfo(open = infoOpen == stat, firstPoints = firstPoints, onToggle = {
        infoOpen = if (infoOpen == stat) null else stat
    }) }
    when (phase) {
        is PointsPhase.Editing -> {
            // 미리보기는 서버가 본 값이 이 시트의 값과 같을 때만 쓴다(낡은 사본이면 숨긴다)
            val live = preview?.takeIf { basis.accepts(it) }
            CareSheetTitle(stringResource(R.string.points_title), shoe.nameAndNumber())
            FreeLine(basis.free(draft), basis)
            // 미리보기 기준 줄("아직 기록이 없어 30분 달리기 기준 …")은 뺐다(2026-11-24 사용자 "글이 너무 많다")
            PointStat.entries.forEach { stat ->
                PointRow(
                    basis, draft, stat, editable = true, info = info(stat),
                    onAdd = { actions.onAdd(stat) }, onRemove = { actions.onRemove(stat) },
                    help = null, // 뜻 한 줄은 ⓘ 에만(2026-11-24)
                    worth = live?.let { pointWorthOf(it, stat) },
                )
            }
            Text(pointsNote(basis), style = runTextStyle(14.sp, t.label, FontWeight.Medium), modifier = Modifier.testTag("points-note"))
            RecommendActions(basis, draft, live, actions)
            val error = phase.error
            if (error != null) {
                Text(
                    stringResource(
                        when (error) {
                            PointsError.OFFLINE -> R.string.points_error_offline
                            PointsError.REJECTED -> R.string.points_error_rejected
                            PointsError.REJECTED_NOT_RELOADED -> R.string.fix_points_error_not_reloaded
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
            PointStat.entries.forEach { stat -> PointRow(basis, draft, stat, editable = false, info = info(stat)) }
            if (lifeMoves) LifeLine(basis, draft, showChange = true)
            Text(
                when {
                    basis.lowers(draft) -> stringResource(R.string.points_confirm_respec)
                    // 서버 0119 — 되돌릴 수 없지만 5레벨마다 한 번 다시 나눌 수 있다고 함께 말한다
                    respecEveryOf(basis.rules) > 0 -> finalNote(basis.rules)
                    else -> stringResource(R.string.points_confirm_body)
                },
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
            FreeLine(basis.free(draft), basis)
            PointStat.entries.forEach { stat -> PointRow(basis, draft, stat, editable = false, info = info(stat), showChange = false) }
            if (lifeMoves) LifeLine(basis, draft, showChange = false)
            RunDivider()
            RunButton(stringResource(R.string.care_done), actions.onClose, modifier = Modifier.testTag("points-done"))
        }
    }
}

/** "남은 포인트 4" — 레벨마다 등급의 포인트(2 ~ 8개), 5레벨마다 더 */
@Composable
private fun FreeLine(free: Int, basis: PointsBasis) {
    val t = runTone()
    Row(
        Modifier.fillMaxWidth().semantics(mergeDescendants = true) {}.testTag("points-free"),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(stringResource(R.string.points_free), style = runTextStyle(16.sp, t.label, FontWeight.SemiBold))
        Spacer(Modifier.width(10.dp))
        Text("$free", style = runTextStyle(22.sp, t.cyan, FontWeight.ExtraBold), modifier = Modifier.testTag("points-free-value"))
    }
}

/**
 * "가득에서 31분 → 29분" — 이 배분으로 신발이 가득에서 달리는 분(서버 0119: 최대 내구도 ÷ (5 × 효율 포인트 배수)). 효율에 넣으면 줄고
 * 내구도에 넣으면 는다. [showChange] 가 아니면(완료) 나눈 뒤 값만
 */
@Composable
private fun LifeLine(basis: PointsBasis, draft: PointsDraft, showChange: Boolean) {
    val t = runTone()
    val now = formatMinutes(basis.lifeMinutes(basis.saved))
    val next = formatMinutes(basis.lifeMinutes(draft))
    val moved = showChange && now != next
    Text(
        if (moved) stringResource(R.string.ers_points_life_change, now, next) else stringResource(R.string.ers_points_life, next),
        style = runTextStyle(14.sp, if (moved) t.text else t.label, if (moved) FontWeight.SemiBold else FontWeight.Medium)
            .copy(fontFeatureSettings = "tnum"),
        modifier = Modifier.semantics { liveRegion = LiveRegionMode.Polite }.testTag("points-life"),
    )
}

/**
 * 되돌릴 수 없다는 한 줄 — 서버 0119 부터는 5레벨마다 한 번 다시 나눌 수 있다고 함께 말한다(예전 서버 · 규칙을 못 읽었으면 예전 글)
 */
@Composable
private fun finalNote(rules: com.stepup.android.domain.ShoeRules): String {
    val every = respecEveryOf(rules)
    return if (every > 0) stringResource(R.string.ers_points_note_final, every) else stringResource(R.string.points_note_final)
}

/** 나누는 장면의 안내 — 다시 나누기가 남았으면 "다시 나누기 1번 · 5레벨마다 한 번", 아니면 되돌릴 수 없다는 한 줄 */
@Composable
private fun pointsNote(basis: PointsBasis): String {
    val every = respecEveryOf(basis.rules)
    return when {
        !basis.respec -> finalNote(basis.rules)
        every > 0 -> stringResource(R.string.ers_points_note_respec, every)
        else -> stringResource(R.string.points_note_respec)
    }
}

/**
 * 글자 버튼 — "추천대로 나누기"(남은 포인트를 서버 추천대로) · 다시 나눌 수 있으면 "처음부터 추천대로"(모든 포인트를 처음부터).
 * 초안만 채운다(보내는 것은 주 버튼 "포인트 나누기" 하나). 추천이 없으면 아무것도 그리지 않는다. 이미 추천과 같으면 흐리게
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun RecommendActions(basis: PointsBasis, draft: PointsDraft, preview: PointsPreview?, actions: PointsActions) {
    val add = basis.recommended(preview)
    // 처음부터 나눈 추천이 남은 포인트만 나눈 추천과 같으면 버튼 하나면 된다
    val all = basis.recommendedAll(preview)?.takeIf { it != add }
    if (add == null && all == null) return
    FlowRow(
        Modifier.fillMaxWidth().testTag("points-recommend-actions"),
        horizontalArrangement = Arrangement.spacedBy(4.dp, Alignment.End),
    ) {
        if (add != null) {
            RunTextAction(stringResource(R.string.ers_points_recommend), actions.onRecommend, enabled = add != draft,
                modifier = Modifier.testTag("points-recommend"))
        }
        if (all != null) {
            RunTextAction(stringResource(R.string.ers_points_recommend_all), actions.onRecommendAll, enabled = all != draft,
                modifier = Modifier.testTag("points-recommend-all"))
        }
    }
}

/** 한 줄의 ⓘ — 펼쳤는가 · 처음 포인트가 생긴 사람인가 · 누르면 */
private class PointInfo(val open: Boolean, val firstPoints: Boolean, val onToggle: () -> Unit)

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
    info: PointInfo? = null,
    onAdd: () -> Unit = {},
    onRemove: () -> Unit = {},
    showChange: Boolean = true,
    /** 뜻 한 줄(서버 0119 — 나누는 장면에서만) */
    help: String? = null,
    /** 미리보기가 말하는 포인트 1 의 값 — 있으면 "1개 = 뽑은 값의 +N%" 자리에 보인다(그 값은 ⓘ 에 남는다) */
    worth: PointWorth? = null,
) {
    val label = stringResource(
        when (stat) {
            PointStat.EFFICIENCY -> R.string.my_shoes_stat_efficiency
            PointStat.COMFORT -> R.string.sdv_stat_comfort
            PointStat.DURABILITY -> R.string.sdv_stat_durability
        },
    )
    // 포인트 1 = 뽑은 값의 +N%(등급마다) — 착화감은 에너지 70% 절약까지(서버 상한)
    val rate = basis.rateText()
    val hint = when (stat) {
        PointStat.COMFORT -> stringResource(R.string.v4s_points_hint_comfort, rate, basis.comfortCapBps / 100)
        else -> stringResource(R.string.v4s_points_hint, rate)
    }
    // 효율 · 착화감은 지수(100 = 기준), 내구도는 가득 찼을 때 달리는 분(서버 0119 — 효율 포인트만큼 빨리 닳아 그만큼 짧아진다)
    val (now, next) = when (stat) {
        PointStat.EFFICIENCY -> formatIndex(basis.efficiencyIndex(basis.saved)) to formatIndex(basis.efficiencyIndex(draft))
        PointStat.COMFORT -> formatIndex(basis.comfortIndex(basis.saved)) to formatIndex(basis.comfortIndex(draft))
        PointStat.DURABILITY -> formatMinutes(basis.lifeMinutes(basis.saved)) to formatMinutes(basis.lifeMinutes(draft))
    }
    val unit = if (stat == PointStat.DURABILITY) stringResource(R.string.v4s_unit_min_suffix) else ""
    // 내구도(분)는 효율 포인트로도 바뀐다 — 보이는 값이 달라지면 화살표로
    val changed = showChange && (draft[stat] != basis.saved[stat] || (stat == PointStat.DURABILITY && now != next))
    val worthText = when (worth) {
        is PointWorth.Gain -> stringResource(R.string.ers_preview_worth, worth.sup)
        PointWorth.Flat -> stringResource(R.string.ers_preview_worth_none)
        PointWorth.ComfortCapped -> stringResource(R.string.ers_preview_comfort_capped, basis.comfortCapBps / 100)
        null -> null
    }
    // v12 시제품(2026-11-26) — 흰 줄 · 바뀐 값은 파란 굵은 글자 · 아래 얇은 구분선(남색 칸 없음)
    val tone = runTone()
    val accent = StepUpUi.colors.accentText
    val key = stat.name.lowercase()
    val countCd = stringResource(R.string.points_count_cd, label, draft[stat])
    Column(
        Modifier.fillMaxWidth().heightIn(min = 64.dp)
            .testTag("points-row-$key").padding(horizontal = 2.dp, vertical = if (info != null) 4.dp else 10.dp),
        verticalArrangement = Arrangement.spacedBy(if (info != null) 0.dp else 6.dp),
    ) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            if (info != null) {
                InfoLabel(label, info, "points-info-$key")
            } else {
                Text(label, style = runTextStyle(16.sp, tone.text, FontWeight.SemiBold), maxLines = 1)
            }
            Spacer(Modifier.width(8.dp))
            Text(
                buildAnnotatedString {
                    // 완료 장면은 이미 바뀐 값(초안)을, 그 밖에는 지금 값을 — 바뀌면 "지금 → 나눈 뒤"
                    append(if (showChange) now else next)
                    if (changed) {
                        append(" → ")
                        withStyle(SpanStyle(color = accent, fontWeight = FontWeight.ExtraBold)) { append(next) }
                    }
                    append(unit)
                },
                style = runTextStyle(15.sp, tone.text, FontWeight.SemiBold).copy(fontFeatureSettings = "tnum"),
                modifier = Modifier.weight(1f).testTag("points-value-$key"),
                textAlign = androidx.compose.ui.text.style.TextAlign.End,
            )
        }
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                if (help != null) {
                    Text(help, style = runTextStyle(13.sp, tone.text, FontWeight.Medium, 1.3f),
                        modifier = Modifier.testTag("points-help-$key"))
                }
                if (worthText != null) {
                    // 어림값 — 늘 "약". 늘어나는 값이 있을 때만 능력치 색으로 앞세운다
                    Text(worthText, style = runTextStyle(13.sp, if (worth is PointWorth.Gain) accent else tone.label,
                        FontWeight.SemiBold, 1.3f).copy(fontFeatureSettings = "tnum"), modifier = Modifier.testTag("points-worth-$key"))
                } else {
                    Text(hint, style = runTextStyle(13.sp, tone.label, FontWeight.Medium))
                }
            }
            if (editable) {
                StepButton(Icons.Filled.Remove, stringResource(R.string.points_remove_cd, label), basis.canRemove(draft, stat), onRemove,
                    "points-remove-$key")
            }
            Text(
                "${draft[stat]}",
                style = runTextStyle(18.sp, tone.text, FontWeight.ExtraBold).copy(fontFeatureSettings = "tnum"),
                textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                modifier = Modifier.widthIn(min = 40.dp).semantics { contentDescription = countCd }.testTag("points-count-$key"),
            )
            if (editable) {
                StepButton(Icons.Filled.Add, stringResource(R.string.points_add_cd, label), basis.canAdd(draft, stat), onAdd,
                    "points-add-$key")
            }
        }
        if (info?.open == true) PointInfoLines(basis, stat, info.firstPoints, key)
        Spacer(Modifier.height(6.dp))
        Box(Modifier.fillMaxWidth().height(1.dp).background(tone.divider))
    }
}

/**
 * 이름 + ⓘ — 함께 누르는 자리(높이 48). 누르면 그 줄 아래 설명이 펼쳐지고, 다시 누르면 접힌다(창을 띄우지 않는다)
 */
@Composable
private fun InfoLabel(label: String, info: PointInfo, tag: String) {
    val cd = stringResource(if (info.open) R.string.shoe_info_hide_cd else R.string.shoe_info_show_cd, label)
    Row(
        Modifier.heightIn(min = StepUpDesign.TouchTarget).clip(RoundedCornerShape(8.dp))
            .feedbackClickable(role = Role.Button, onClick = info.onToggle)
            .semantics(mergeDescendants = true) { contentDescription = cd }
            .testTag(tag),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(label, style = runTextStyle(16.sp, CartoonColors.Text, FontWeight.SemiBold), maxLines = 1)
        Spacer(Modifier.width(5.dp))
        Icon(
            Icons.Outlined.Info, contentDescription = null,
            tint = if (info.open) CartoonColors.Text else CartoonColors.Number, modifier = Modifier.size(18.dp),
        )
    }
}

/**
 * ⓘ 를 펼친 두 줄 — 한 줄 설명 + 이 신발(등급)에서 포인트 1 의 값(서버 규칙). 처음 포인트가 생긴 사람에게는 되돌릴 수 없다는 한 줄을 더
 */
@Composable
private fun PointInfoLines(basis: PointsBasis, stat: PointStat, firstPoints: Boolean, key: String) {
    // 서버 0119 의 뜻이면 새 글(효율은 신발 · 에너지도 빨리 쓴다 · 내구도는 가득 수리비 그대로), 예전 서버면 예전 글
    val meaning = stringResource(statMeaningRes(stat, newStatMeaning(basis.rules)))
    val value = when (stat) {
        PointStat.EFFICIENCY -> stringResource(R.string.shoe_info_point_efficiency, basis.rateText())
        PointStat.COMFORT -> stringResource(R.string.shoe_info_point_comfort, basis.rateText(), basis.comfortCapBps / 100)
        PointStat.DURABILITY -> stringResource(R.string.shoe_info_point_durability, basis.minutesPerPointText())
    }
    Column(
        Modifier.fillMaxWidth().padding(top = 2.dp, bottom = 6.dp)
            .semantics(mergeDescendants = true) { liveRegion = LiveRegionMode.Polite }
            .testTag("points-info-lines-$key"),
        verticalArrangement = Arrangement.spacedBy(3.dp),
    ) {
        Text(meaning, style = runTextStyle(14.sp, CartoonColors.Text, FontWeight.Medium, 1.35f), modifier = Modifier.testTag("points-info-meaning-$key"))
        Text(value, style = runTextStyle(14.sp, CartoonColors.Number, FontWeight.SemiBold).copy(fontFeatureSettings = "tnum"),
            modifier = Modifier.testTag("points-info-value-$key"))
        if (firstPoints) {
            Text(finalNote(basis.rules), style = runTextStyle(13.sp, CartoonColors.Number, FontWeight.Medium, 1.35f),
                modifier = Modifier.testTag("points-info-final-$key"))
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
    Box(
        Modifier.size(48.dp).clip(CircleShape)
            .feedbackClickable(enabled = enabled, role = Role.Button, onClick = onClick)
            .semantics { contentDescription = description }
            .testTag(tag),
        contentAlignment = Alignment.Center,
    ) {
        // v12 시제품 — 옅은 회색 원 위 짙은 ± (누를 수 없으면 흐리게)
        Box(
            Modifier.size(34.dp).clip(CircleShape).background(StepUpUi.colors.fill, CircleShape).alpha(if (enabled) 1f else 0.35f),
            contentAlignment = Alignment.Center,
        ) {
            Icon(icon, contentDescription = null, tint = StepUpUi.colors.label, modifier = Modifier.size(20.dp))
        }
    }
}
