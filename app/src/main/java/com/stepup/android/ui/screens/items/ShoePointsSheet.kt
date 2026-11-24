package com.stepup.android.ui.screens.items

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ChevronLeft
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material.icons.outlined.Timer
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import com.stepup.android.R
import com.stepup.android.core.ServiceLocator
import com.stepup.android.data.repo.EconomyOutcome
import com.stepup.android.domain.PointsPreview
import com.stepup.android.domain.Sneaker
import com.stepup.android.ui.components.PlainDivider
import com.stepup.android.ui.components.ProductImage
import com.stepup.android.ui.components.RunButton
import com.stepup.android.ui.components.RunButtonKind
import com.stepup.android.ui.components.RunSheet
import com.stepup.android.ui.components.RunTextAction
import com.stepup.android.ui.components.StepUpUi
import com.stepup.android.ui.components.runTextStyle
import com.stepup.android.ui.components.runTone
import com.stepup.android.ui.components.sneakerArtRes
import com.stepup.android.ui.experience.ExperienceEvents
import com.stepup.android.ui.experience.FeedbackCue
import com.stepup.android.ui.experience.feedbackClickable
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.first

/*
 * 스탯 포인트 나누기 시트(서버 0059) — 상세 위의 하단 시트 하나. 되돌릴 수 없어 한 번 더 확인하고, 서버가 받았다고 답한 뒤에만
 * "나눴어요"라고 쓴다. 계산은 ShoePointsModel 의 순수 함수 — 서버가 같은 규칙으로 다시 확인한다.
 * 미리보기(서버 0119 — 내 지난 14일 러닝)를 받으면 추천 카드와 "1포인트 = 하루 약 +N SUP"(어림값)를 보이고, 못 받았으면(0119 전 서버 ·
 * 연결 끊김) 그 칸만 없이 바로 직접 나누기다. 장면 배치는 아래 [PointsBody] 의 설명(2026-11-24 재디자인).
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
    /** 처음 포인트가 생긴 사람([firstPointsOf]) — 직접 나누기에서 "나눈 포인트는 되돌릴 수 없어요"를 늘 함께 보인다 */
    firstPoints: Boolean = false,
    /** 서버가 내 지난 14일 러닝으로 셈한 미리보기(0119) — 없으면 추천 카드 · "하루 약 +N SUP" 줄을 보이지 않는다 */
    preview: PointsPreview? = null,
) {
    val sending = phase is PointsPhase.Sending
    RunSheet(
        onDismiss = { if (!sending) actions.onClose() },
        modifier = Modifier.testTag("points-sheet"),
        dismissible = !sending,
        closeTag = "points-close",
    ) {
        ShoePointsSheetBody(shoe, phase, actions, onSignIn, firstPoints, preview)
    }
}

/** 시트 안 — 창 없이 그리는 캡처(시안 맞대기)도 이것을 그린다 */
@Composable
internal fun ShoePointsSheetBody(
    shoe: Sneaker,
    phase: PointsPhase,
    actions: PointsActions,
    onSignIn: (() -> Unit)?,
    firstPoints: Boolean = false,
    preview: PointsPreview? = null,
    /** 캡처용 — 직접 나누기 장면에서 시작 */
    startManual: Boolean = false,
) {
    Column(Modifier.fillMaxWidth().verticalScroll(rememberScrollState()).testTag(phase.sceneTag())) {
        PointsBody(shoe, phase, actions, onSignIn, firstPoints, preview, startManual)
    }
}

/*
 * 2026-11-24 재디자인(docs/redesign/plain-2026-11-24 06 스탯 포인트) — 네 장면.
 * ① 추천: 미리보기가 추천을 줄 때만 처음에 — 파란 카드 하나("내 러닝으로 보면 남은 2개는 · 효율에 2개 · 하루 약 +N SUP 더") ·
 *    주 버튼 "추천대로 나누기"(= 초안을 추천으로 채우고 확인으로) · 글자 버튼 "직접 나누기 ›".
 * ② 직접 나누기: 효율 · 착화감 · 내구도 전환 칸 → 가운데 큰 값 · 양옆 −/+ 62dp → "1포인트 = 하루 약 +N SUP" → "추천 보기".
 * ③ 확인 · ④ 완료: 구분선 목록, 바뀐 줄만 그 능력치 색. 서버 호출(sneaker_points_preview · sneaker_points_set)과 상태는 그대로다.
 */
@Composable
private fun ColumnScope.PointsBody(
    shoe: Sneaker,
    phase: PointsPhase,
    actions: PointsActions,
    onSignIn: (() -> Unit)?,
    firstPoints: Boolean,
    preview: PointsPreview?,
    startManual: Boolean = false,
) {
    val basis = phase.basis
    // 직접 나누기를 골랐는가 — 확인으로 갔다 "취소"로 돌아와도 그대로(장면 밖에 둔다)
    var manual by rememberSaveable(basis.shoeId) { mutableStateOf(startManual) }
    when (phase) {
        is PointsPhase.Editing -> {
            // 미리보기는 서버가 본 값이 이 시트의 값과 같을 때만 쓴다(낡은 사본이면 숨긴다)
            val live = preview?.takeIf { basis.accepts(it) }
            val add = basis.recommended(live)
            // 처음부터 나눈 추천이 남은 포인트만 나눈 추천과 같으면 하나면 된다
            val all = basis.recommendedAll(live)?.takeIf { it != add }
            val rec = add ?: all
            val canRecommend = rec != null && phase.error == null && live != null
            if (!manual && canRecommend) {
                RecommendView(shoe, basis, rec, fromScratch = add == null, live = live, actions = actions, onManual = { manual = true })
            } else {
                ManualView(shoe, phase, live, add, all, actions, onSignIn, firstPoints,
                    onShowRecommend = if (canRecommend) ({ manual = false }) else null)
            }
        }
        is PointsPhase.Confirm, is PointsPhase.Sending -> ConfirmView(shoe, phase, actions)
        is PointsPhase.Done -> DoneView(shoe, phase, actions)
    }
}

// ── ① 추천 ─────────────────────────────────────────────────────

@Composable
private fun ColumnScope.RecommendView(
    shoe: Sneaker,
    basis: PointsBasis,
    rec: PointsDraft,
    fromScratch: Boolean,
    live: PointsPreview,
    actions: PointsActions,
    onManual: () -> Unit,
) {
    val t = runTone()
    CareSheetTitle(stringResource(R.string.points_title), shoe.nameAndNumber())
    Spacer(Modifier.height(18.dp))
    // 더할 양(남은 포인트 추천) 또는 처음부터 나눈 값 — 0 인 능력치는 쓰지 않는다
    val parts = PointStat.entries.mapNotNull { stat ->
        val n = if (fromScratch) rec[stat] else rec[stat] - basis.saved[stat]
        if (n > 0) stat to n else null
    }
    val headline = if (parts.size == 1) {
        stringResource(R.string.rd_b_rec_split, statLabel(parts[0].first), parts[0].second)
    } else {
        parts.map { (stat, n) -> stringResource(R.string.rd_b_rec_part, statLabel(stat), n) }.joinToString(" · ")
    }
    val lead = if (fromScratch) stringResource(R.string.rd_b_rec_lead_all)
    else stringResource(R.string.rd_b_rec_lead, basis.free(basis.saved))
    // 하루에 더 남는 SUP — 서버가 추천 배분과 지금 배분의 하루 값을 둘 다 보냈을 때만(어림값, 늘 "약")
    val target = if (fromScratch) live.recommendAll else live.recommend
    val gain = target?.dayNet?.let { next -> live.now?.dayNet?.let { now -> next - now } }?.takeIf { it.isFinite() && it >= 0.005 }
    val art = sneakerArtRes(shoe)
    Row(
        Modifier.fillMaxWidth().clip(RoundedCornerShape(28.dp)).background(t.cobalt)
            .semantics(mergeDescendants = true) {}
            .testTag("points-recommend-card")
            .padding(start = 20.dp, end = 12.dp, top = 22.dp, bottom = 22.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(Modifier.weight(1f)) {
            Text(lead, style = runTextStyle(15.sp, Color.White.copy(alpha = 0.86f), FontWeight.SemiBold, 1.3f)
                .copy(lineBreak = com.stepup.android.ui.components.KoreanLineBreak))
            Spacer(Modifier.height(2.dp))
            Text(headline, style = runTextStyle(32.sp, Color.White, FontWeight.ExtraBold, 1.2f), maxLines = 2,
                overflow = TextOverflow.Ellipsis, modifier = Modifier.testTag("points-recommend-headline"))
            if (gain != null) {
                Spacer(Modifier.height(8.dp))
                Text(stringResource(R.string.rd_b_rec_gain, formatApproxSup(gain)),
                    style = runTextStyle(17.sp, Color.White.copy(alpha = 0.92f), FontWeight.SemiBold, 1.3f)
                        .copy(fontFeatureSettings = "tnum", lineBreak = com.stepup.android.ui.components.KoreanLineBreak),
                    maxLines = 2, overflow = TextOverflow.Ellipsis)
            }
        }
        if (art != null) {
            Spacer(Modifier.width(8.dp))
            // 큰 글씨면 그림을 줄여 글 칸을 넓힌다(카드 안에 다 들어가게)
            val big = androidx.compose.ui.platform.LocalDensity.current.fontScale > 1.1f
            ProductImage(art, height = if (big) 52.dp else 66.dp, modifier = Modifier.width(if (big) 92.dp else 118.dp))
        }
    }
    Spacer(Modifier.height(8.dp))
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Text(sceneNote(basis), style = runTextStyle(14.5.sp, t.label, FontWeight.Medium, 1.35f).copy(lineBreak = com.stepup.android.ui.components.KoreanLineBreak),
            modifier = Modifier.weight(1f).testTag("points-note"))
        RunTextAction(stringResource(R.string.rd_b_manual), onManual, chevron = true, modifier = Modifier.testTag("points-manual"))
    }
    Spacer(Modifier.height(8.dp))
    RunButton(
        stringResource(R.string.ers_points_recommend),
        onClick = {
            // 초안을 추천으로 채우고 바로 확인으로 — 보내는 것은 확인의 "나누기"
            if (fromScratch) actions.onRecommendAll() else actions.onRecommend()
            actions.onReview()
        },
        modifier = Modifier.testTag("points-recommend"),
    )
}

// ── ② 직접 나누기 ──────────────────────────────────────────────

@Composable
private fun ColumnScope.ManualView(
    shoe: Sneaker,
    phase: PointsPhase.Editing,
    live: PointsPreview?,
    add: PointsDraft?,
    all: PointsDraft?,
    actions: PointsActions,
    onSignIn: (() -> Unit)?,
    firstPoints: Boolean,
    onShowRecommend: (() -> Unit)?,
) {
    val t = runTone()
    val basis = phase.basis
    val draft = phase.draft
    var selected by rememberSaveable(basis.shoeId) { mutableStateOf(PointStat.EFFICIENCY) }
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        if (onShowRecommend != null) {
            SheetBack(onShowRecommend)
            Spacer(Modifier.width(10.dp))
        }
        CareSheetTitle(
            stringResource(if (onShowRecommend != null) R.string.rd_b_manual else R.string.points_title),
            shoe.nameAndNumber(), Modifier.weight(1f),
        )
    }
    Spacer(Modifier.height(14.dp))
    FreeLine(basis.free(draft), basis)
    Spacer(Modifier.height(12.dp))
    StatSwitcher(basis, draft, selected, onSelect = { selected = it })
    Spacer(Modifier.height(12.dp))
    StatEditor(basis, draft, selected, live, actions)
    // 2026-11-24 재디자인(06 ②) — 고른 능력치의 뜻 · 가득에서 달리는 분은 확인(③)의 줄로, 등급의 레벨당 포인트는 완료(④)로 옮겼다
    Spacer(Modifier.height(10.dp))
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Text(
            if (firstPoints && !basis.respec) finalNote(basis.rules) else sceneNote(basis),
            style = runTextStyle(14.5.sp, t.label, FontWeight.Medium, 1.35f).copy(lineBreak = com.stepup.android.ui.components.KoreanLineBreak),
            modifier = Modifier.weight(1f).testTag("points-note"),
        )
        if (onShowRecommend != null) {
            RunTextAction(stringResource(R.string.rd_b_show_rec), onShowRecommend, modifier = Modifier.testTag("points-show-recommend"))
            Spacer(Modifier.width(4.dp))
        }
    }
    if (all != null) {
        // 다시 나눌 수 있는 신발 — 모든 포인트를 처음부터 서버 추천대로(초안만 채운다)
        RunTextAction(
            stringResource(R.string.ers_points_recommend_all), actions.onRecommendAll, enabled = all != draft,
            modifier = Modifier.align(Alignment.End).testTag("points-recommend-all"),
        )
    }
    // 추천 카드로 돌아갈 수 없을 때(바로 앞의 보내기가 실패) 남은 포인트 추천을 초안에 채우는 글자 버튼은 그대로 둔다
    if (add != null && onShowRecommend == null) {
        RunTextAction(
            stringResource(R.string.ers_points_recommend), actions.onRecommend, enabled = add != draft,
            modifier = Modifier.align(Alignment.End).testTag("points-recommend"),
        )
    }
    val error = phase.error
    if (error != null) {
        Spacer(Modifier.height(6.dp))
        Text(
            stringResource(
                when (error) {
                    PointsError.OFFLINE -> R.string.points_error_offline
                    PointsError.REJECTED -> R.string.points_error_rejected
                    PointsError.REJECTED_NOT_RELOADED -> R.string.fix_points_error_not_reloaded
                    PointsError.SIGN_IN -> R.string.points_error_sign_in
                },
            ),
            style = runTextStyle(14.sp, if (error == PointsError.OFFLINE) t.label else t.dangerText, FontWeight.SemiBold, 1.4f),
            modifier = Modifier.testTag("points-error").semantics { liveRegion = LiveRegionMode.Polite },
        )
    }
    Spacer(Modifier.height(8.dp))
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

/** 직접 나누기의 뒤로(추천으로) — 회색 원 안의 화살표, 누르는 자리 48 */
@Composable
private fun SheetBack(onClick: () -> Unit) {
    val t = runTone()
    val label = stringResource(R.string.cd_back)
    Box(
        Modifier.size(48.dp).clip(CircleShape).feedbackClickable(role = Role.Button, onClick = onClick)
            .semantics { contentDescription = label }.testTag("points-manual-back"),
        contentAlignment = Alignment.Center,
    ) {
        Box(Modifier.size(38.dp).clip(CircleShape).background(StepUpUi.colors.fill), contentAlignment = Alignment.Center) {
            Icon(Icons.Filled.ChevronLeft, contentDescription = null, tint = t.text, modifier = Modifier.size(20.dp))
        }
    }
}

/** "남은 포인트 ●○ 1개" — 점은 이번에 나눌 포인트(10개까지), 오른쪽 끝에 등급의 레벨당 포인트 */
@Composable
private fun FreeLine(free: Int, basis: PointsBasis) {
    val t = runTone()
    val start = basis.free(basis.saved)
    val unit = stringResource(R.string.rd_b_points_unit)
    Row(
        Modifier.fillMaxWidth().semantics(mergeDescendants = true) {}.testTag("points-free"),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(stringResource(R.string.points_free), style = runTextStyle(16.sp, t.label, FontWeight.SemiBold), maxLines = 1)
        Spacer(Modifier.width(10.dp))
        if (!basis.respec && start in 1..10) {
            repeat(start) { i ->
                val on = i < free
                Box(
                    Modifier.padding(end = 6.dp).size(14.dp).clip(CircleShape)
                        .background(if (on) t.cobalt else Color.Transparent, CircleShape)
                        .then(if (on) Modifier else Modifier.border(1.5.dp, t.track, CircleShape)),
                )
            }
            Spacer(Modifier.width(5.dp))
        }
        Text("$free", style = runTextStyle(18.sp, t.cobaltText, FontWeight.ExtraBold, 1.2f).copy(fontFeatureSettings = "tnum"),
            maxLines = 1, softWrap = false, modifier = Modifier.testTag("points-free-value"))
        if (unit.isNotBlank()) {
            Text(unit, style = runTextStyle(18.sp, t.cobaltText, FontWeight.ExtraBold, 1.2f), maxLines = 1, softWrap = false)
        }
    }
}

/** 효율 · 착화감 · 내구도 전환 칸 — 회색 칸 안에 셋, 고른 칸은 흰 면. 칸마다 그 능력치에 든 포인트 수(바뀌면 파랑) */
@Composable
private fun StatSwitcher(basis: PointsBasis, draft: PointsDraft, selected: PointStat, onSelect: (PointStat) -> Unit) {
    val t = runTone()
    Row(
        Modifier.fillMaxWidth().clip(RoundedCornerShape(20.dp)).background(t.inset).padding(4.dp),
        horizontalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        PointStat.entries.forEach { stat ->
            val on = stat == selected
            val key = stat.name.lowercase()
            val label = statLabel(stat)
            val countCd = stringResource(R.string.points_count_cd, label, draft[stat])
            val moved = draft[stat] != basis.saved[stat]
            Row(
                // 고른 칸은 이름 옆에 알약이 붙어 조금 넓게(시안 06 ②)
                Modifier.weight(if (on) 1.4f else 1f).heightIn(min = 52.dp).clip(RoundedCornerShape(16.dp))
                    .background(if (on) t.panel else Color.Transparent)
                    .feedbackClickable(role = Role.Tab, onClick = { onSelect(stat) })
                    .semantics { this.selected = on }
                    .testTag("points-tab-$key")
                    .padding(horizontal = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center,
            ) {
                Icon(statIcon(stat, filled = on), contentDescription = null, tint = if (on) statColor(stat) else t.label,
                    modifier = Modifier.size(19.dp))
                Spacer(Modifier.width(6.dp))
                // 좁은 폭 · 큰 글씨에서는 이름을 줄여 다 보인다(… 로 자르지 않는다)
                androidx.compose.foundation.text.BasicText(
                    label, style = runTextStyle(16.sp, if (on) t.text else t.label, if (on) FontWeight.Bold else FontWeight.SemiBold, 1.2f),
                    maxLines = 1,
                    autoSize = androidx.compose.foundation.text.TextAutoSize.StepBased(minFontSize = 10.sp, maxFontSize = 16.sp, stepSize = 0.5.sp),
                    modifier = Modifier.weight(1f, fill = false),
                )
                // 이 능력치에 든 포인트 — 이번에 바꿨으면 파란 알약(처음 넣는 것이면 "+1"), 이미 든 것만 있으면 흐린 숫자, 0 이면 없음
                val count = draft[stat]
                if (moved || count > 0) {
                    Spacer(Modifier.width(6.dp))
                    val countStyle = runTextStyle(13.sp, if (moved) Color.White else t.muted, FontWeight.ExtraBold, 1.2f)
                        .copy(fontFeatureSettings = "tnum")
                    Row(
                        Modifier.then(
                            if (moved) Modifier.clip(RoundedCornerShape(50)).background(t.cobalt).padding(horizontal = 7.dp, vertical = 2.dp)
                            else Modifier,
                        ),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        if (moved && basis.saved[stat] == 0) Text("+", style = countStyle, maxLines = 1)
                        Text(
                            "$count", style = countStyle, maxLines = 1, softWrap = false,
                            modifier = Modifier.semantics { contentDescription = countCd }.testTag("points-count-$key"),
                        )
                    }
                }
            }
        }
    }
}

/** 고른 능력치 하나 — 이름 · 가운데 큰 값(나눈 뒤) · 양옆 −/+ 62dp · "지금 140" · "1포인트 = 하루 약 +N SUP" */
@Composable
private fun StatEditor(basis: PointsBasis, draft: PointsDraft, stat: PointStat, live: PointsPreview?, actions: PointsActions) {
    val t = runTone()
    val key = stat.name.lowercase()
    val label = statLabel(stat)
    val color = statColor(stat)
    val (now, next) = statValues(basis, draft, stat)
    val unit = if (stat == PointStat.DURABILITY) stringResource(R.string.v4s_unit_min_suffix) else null
    val worth = live?.let { pointWorthOf(it, stat) }
    val worthText = when (worth) {
        is PointWorth.Gain -> stringResource(R.string.ers_preview_worth, worth.sup)
        PointWorth.Flat -> stringResource(R.string.ers_preview_worth_none)
        PointWorth.ComfortCapped -> stringResource(R.string.ers_preview_comfort_capped, basis.comfortCapBps / 100)
        null -> null
    }
    // 포인트 1 = 뽑은 값의 +N%(등급마다) — 미리보기가 없을 때 그 자리에
    val rate = basis.rateText()
    val hint = when (stat) {
        PointStat.COMFORT -> stringResource(R.string.v4s_points_hint_comfort, rate, basis.comfortCapBps / 100)
        PointStat.DURABILITY -> stringResource(R.string.shoe_info_point_durability, basis.minutesPerPointText())
        else -> stringResource(R.string.v4s_points_hint, rate)
    }
    Column(
        Modifier.fillMaxWidth().clip(RoundedCornerShape(26.dp)).background(t.inset)
            .testTag("points-row-$key").padding(start = 16.dp, end = 16.dp, top = 22.dp, bottom = 20.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(label, style = runTextStyle(15.sp, color, FontWeight.Bold, 1.2f), maxLines = 1)
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            StepButton(Icons.Filled.Remove, stringResource(R.string.points_remove_cd, label), basis.canRemove(draft, stat),
                { actions.onRemove(stat) }, "points-remove-$key", filled = false)
            Box(Modifier.weight(1f), contentAlignment = Alignment.Center) {
                // 긴 값("1,063" · "29.5분")은 조금 작게 — 360dp · 큰 글씨에서도 −/+ 사이에 한 줄로
                // 큰 값 — 앱 글꼴 ExtraBold(시안 06 ②), 긴 값("1,063" · "29.5분")은 조금 작게 — 360dp · 큰 글씨에서도 −/+ 사이에 한 줄로
                Text(
                    buildAnnotatedString {
                        append(next)
                        if (unit != null) withStyle(SpanStyle(fontSize = 26.sp)) { append(unit) }
                    },
                    style = runTextStyle(if (next.length > 4 || unit != null) 50.sp else 60.sp, color, FontWeight.ExtraBold, 1.1f)
                        .copy(fontFeatureSettings = "tnum", letterSpacing = (-0.03).em),
                    maxLines = 1, softWrap = false,
                    modifier = Modifier.semantics(mergeDescendants = true) { liveRegion = LiveRegionMode.Polite }
                        .testTag("points-value-$key"),
                )
            }
            StepButton(Icons.Filled.Add, stringResource(R.string.points_add_cd, label), basis.canAdd(draft, stat),
                { actions.onAdd(stat) }, "points-add-$key", filled = true)
        }
        Text(stringResource(R.string.rd_b_now, now + (unit ?: "")), style = runTextStyle(16.sp, t.label, FontWeight.Medium, 1.3f)
            .copy(fontFeatureSettings = "tnum"), maxLines = 1)
        Spacer(Modifier.height(16.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            if (worth is PointWorth.Gain) {
                Icon(Icons.Filled.Bolt, contentDescription = null, tint = t.cobaltText, modifier = Modifier.size(20.dp))
                Spacer(Modifier.width(6.dp))
            }
            Text(
                worthText ?: hint,
                style = runTextStyle(16.5.sp, if (worth is PointWorth.Gain) t.text else t.label, FontWeight.SemiBold, 1.35f)
                    .copy(fontFeatureSettings = "tnum"),
                textAlign = TextAlign.Center,
                modifier = Modifier.testTag(if (worthText != null) "points-worth-$key" else "points-hint-$key"),
            )
        }
        // 위 "하루 약 +N SUP" 가 무엇으로 셈한 어림값인지(서버 0119 미리보기의 기준)
        if (live != null && worthText != null) {
            Spacer(Modifier.height(4.dp))
            PreviewBasisLine(live)
        }
    }
}

/** −/+ 둥근 버튼 62dp — − 는 흰 면 · 테두리, + 는 파란 면. 누를 수 없으면 흐리게 */
@Composable
private fun StepButton(icon: ImageVector, description: String, enabled: Boolean, onClick: () -> Unit, tag: String, filled: Boolean) {
    val t = runTone()
    Box(
        Modifier.size(62.dp).alpha(if (enabled) 1f else 0.35f).clip(CircleShape)
            .background(if (filled) t.cobalt else t.panel, CircleShape)
            .then(if (filled) Modifier else Modifier.border(1.dp, t.divider, CircleShape))
            .feedbackClickable(enabled = enabled, role = Role.Button, onClick = onClick)
            .semantics { contentDescription = description }
            .testTag(tag),
        contentAlignment = Alignment.Center,
    ) {
        Icon(icon, contentDescription = null, tint = if (filled) Color.White else t.cobaltText, modifier = Modifier.size(26.dp))
    }
}

// ── ③ 확인 · ④ 완료 ────────────────────────────────────────────

@Composable
private fun ColumnScope.ConfirmView(shoe: Sneaker, phase: PointsPhase, actions: PointsActions) {
    val t = runTone()
    val basis = phase.basis
    val draft = phase.draft
    CareSheetTitle(stringResource(R.string.points_confirm_title), shoe.nameAndNumber())
    Spacer(Modifier.height(16.dp))
    PlainDivider()
    PointStat.entries.forEachIndexed { index, stat ->
        if (index > 0) PlainDivider()
        ChangeRow(basis, draft, stat, withIcon = true)
    }
    // 바뀌는 능력치의 뜻(② 에서 옮김) — 효율이면 "더 빨리 벌어요 · 신발과 에너지도 빨리 써요"
    val changed = PointStat.entries.filter { draft[it] != basis.saved[it] }
    if (changed.isNotEmpty()) {
        Spacer(Modifier.height(14.dp))
        val helps = changed.map { stat ->
            if (newStatMeaning(basis.rules)) pointHelp(stat) else stringResource(statMeaningRes(stat, false))
        }
        Text(helps.joinToString("\n"), style = runTextStyle(14.5.sp, t.label, FontWeight.Medium, 1.45f),
            modifier = Modifier.fillMaxWidth().testTag("points-help"))
    }
    Spacer(Modifier.height(14.dp))
    val every = respecEveryOf(basis.rules)
    Text(
        when {
            basis.lowers(draft) -> stringResource(R.string.points_confirm_respec)
            every > 0 -> stringResource(R.string.rd_b_final_lead)
            else -> stringResource(R.string.points_confirm_body)
        },
        style = runTextStyle(15.5.sp, t.text, FontWeight.Bold, 1.45f), modifier = Modifier.testTag("points-confirm-note"),
    )
    // 서버 0119 — 되돌릴 수 없지만 5레벨마다 한 번 다시 나눌 수 있다(흐린 둘째 줄)
    if (!basis.lowers(draft) && every > 0) {
        Text(stringResource(R.string.rd_b_respec_every, every), style = runTextStyle(15.5.sp, t.label, FontWeight.Medium, 1.45f))
    }
    Spacer(Modifier.height(20.dp))
    if (phase is PointsPhase.Sending) {
        RunButton(stringResource(R.string.points_sending), {}, busy = true, modifier = Modifier.testTag("points-send"))
    } else {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            RunButton(stringResource(R.string.common_cancel), actions.onBack, kind = RunButtonKind.Secondary,
                modifier = Modifier.weight(0.38f).testTag("points-back"))
            RunButton(stringResource(R.string.points_send), actions.onSend, modifier = Modifier.weight(0.62f).testTag("points-send"))
        }
    }
}

@Composable
private fun ColumnScope.DoneView(shoe: Sneaker, phase: PointsPhase.Done, actions: PointsActions) {
    val t = runTone()
    val basis = phase.basis
    val draft = phase.draft
    Image(
        painterResource(R.drawable.scene_on_shoe), contentDescription = null, contentScale = ContentScale.Fit,
        modifier = Modifier.align(Alignment.CenterHorizontally).height(120.dp),
    )
    Spacer(Modifier.height(14.dp))
    Text(
        stringResource(R.string.points_done_title), style = runTextStyle(25.sp, t.text, FontWeight.ExtraBold, 1.25f),
        textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth().semantics { heading() },
    )
    Spacer(Modifier.height(4.dp))
    Text(shoe.nameAndNumber(), style = runTextStyle(15.sp, t.label, FontWeight.Medium), textAlign = TextAlign.Center,
        modifier = Modifier.fillMaxWidth())
    Spacer(Modifier.height(18.dp))
    PointStat.entries.forEachIndexed { index, stat ->
        if (index > 0) PlainDivider()
        ChangeRow(basis, draft, stat, withIcon = false)
    }
    val free = basis.free(draft)
    if (free > 0) {
        Spacer(Modifier.height(8.dp))
        Text(stringResource(R.string.points_manage_free, free), style = runTextStyle(14.sp, t.label, FontWeight.Medium),
            modifier = Modifier.testTag("points-free"))
        // 등급의 레벨당 포인트(② 의 남은 포인트 줄에서 옮김) — 남은 포인트가 있을 때 다음에 받을 몫과 함께
        val grade = basis.rules.grade(basis.rarity)
        Text(
            stringResource(R.string.v4s_points_per_level, grade.pointsPerLevel, grade.pointsMilestone),
            style = runTextStyle(13.sp, t.muted, FontWeight.Medium), modifier = Modifier.testTag("points-per-level"),
        )
    }
    Spacer(Modifier.height(22.dp))
    RunButton(stringResource(R.string.common_ok), actions.onClose, modifier = Modifier.testTag("points-done"))
}

/**
 * 확인 · 완료의 한 줄 — (아이콘) 이름 (+2) · 오른쪽 "140 → 152". 바뀐 줄만 그 능력치 색, 안 바뀐 줄은 지금 값 하나.
 * 내구도(분)는 효율 포인트로도 바뀐다 — 보이는 값이 달라지면 화살표로.
 */
@Composable
private fun ChangeRow(basis: PointsBasis, draft: PointsDraft, stat: PointStat, withIcon: Boolean) {
    val t = runTone()
    val key = stat.name.lowercase()
    val color = statColor(stat)
    val (now, next) = statValues(basis, draft, stat)
    val unit = if (stat == PointStat.DURABILITY) stringResource(R.string.v4s_unit_min_suffix) else ""
    val delta = draft[stat] - basis.saved[stat]
    val changed = delta != 0 || now != next
    Row(
        Modifier.fillMaxWidth().heightIn(min = if (withIcon) 58.dp else 50.dp)
            .semantics(mergeDescendants = true) {}.testTag("points-row-$key"),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (withIcon) {
            Icon(statIcon(stat, filled = true), contentDescription = null, tint = color, modifier = Modifier.size(24.dp))
            Spacer(Modifier.width(16.dp))
        }
        Row(Modifier.weight(1f), verticalAlignment = Alignment.CenterVertically) {
            Text(statLabel(stat), style = runTextStyle(17.sp, t.text, if (withIcon) FontWeight.Bold else FontWeight.SemiBold, 1.25f),
                maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f, fill = false))
            // 몇 개를 넣는지는 확인(③)에만 — 완료(④)는 바뀐 값만(시안 06 ④)
            if (delta != 0 && withIcon) {
                Spacer(Modifier.width(14.dp))
                Text(if (delta > 0) "+$delta" else "−${-delta}", style = runTextStyle(16.5.sp, color, FontWeight.ExtraBold, 1.2f), maxLines = 1)
            }
        }
        Spacer(Modifier.width(12.dp))
        Text(
            buildAnnotatedString {
                if (changed) {
                    withStyle(SpanStyle(color = if (withIcon) t.label else color)) { append("$now$unit → ") }
                    withStyle(SpanStyle(color = color, fontWeight = FontWeight.ExtraBold)) { append("$next$unit") }
                } else {
                    append("$next$unit")
                }
            },
            style = runTextStyle(20.sp, if (withIcon) t.label else t.text, FontWeight.Bold, 1.2f).copy(fontFeatureSettings = "tnum"),
            maxLines = 1, softWrap = false, modifier = Modifier.testTag("points-value-$key"),
        )
    }
}

// ── 공용 ───────────────────────────────────────────────────────

/** 지금 값 → 나눈 뒤 값 — 효율 · 착화감은 지수(100 = 기준), 내구도는 가득에서 달리는 분(효율 포인트만큼 짧아진다, 서버 0119) */
private fun statValues(basis: PointsBasis, draft: PointsDraft, stat: PointStat): Pair<String, String> = when (stat) {
    PointStat.EFFICIENCY -> formatIndex(basis.efficiencyIndex(basis.saved)) to formatIndex(basis.efficiencyIndex(draft))
    PointStat.COMFORT -> formatIndex(basis.comfortIndex(basis.saved)) to formatIndex(basis.comfortIndex(draft))
    PointStat.DURABILITY -> formatMinutes(basis.lifeMinutes(basis.saved)) to formatMinutes(basis.lifeMinutes(draft))
}

@Composable
private fun statLabel(stat: PointStat): String = stringResource(
    when (stat) {
        PointStat.EFFICIENCY -> R.string.my_shoes_stat_efficiency
        PointStat.COMFORT -> R.string.sdv_stat_comfort
        PointStat.DURABILITY -> R.string.sdv_stat_durability
    },
)

@Composable
private fun statColor(stat: PointStat): Color {
    val t = runTone()
    return when (stat) {
        PointStat.EFFICIENCY -> t.eff
        PointStat.COMFORT -> t.com
        PointStat.DURABILITY -> t.dur
    }
}

private fun statIcon(stat: PointStat, filled: Boolean): ImageVector = when (stat) {
    PointStat.EFFICIENCY -> Icons.Filled.Bolt
    PointStat.COMFORT -> if (filled) Icons.Filled.Favorite else Icons.Outlined.FavoriteBorder
    PointStat.DURABILITY -> if (filled) Icons.Filled.Timer else Icons.Outlined.Timer
}

/**
 * 미리보기의 기준 한 줄 — "내 지난 14일 러닝 7번 기준 · 어림값이에요" / 기록이 없으면 "30분 달리기 기준". 위 SUP 는 받은 것이
 * 아니라 서버가 지난 러닝으로 셈한 어림값이다. 기준을 모르면 줄이 없다
 */
@Composable
private fun PreviewBasisLine(preview: PointsPreview) {
    val text = when (preview.source) {
        PointsPreview.Source.MINE -> stringResource(R.string.ers_preview_basis_mine, preview.runs)
        PointsPreview.Source.DEFAULT -> stringResource(R.string.ers_preview_basis_default, preview.avgMin ?: DEFAULT_PREVIEW_MINUTES)
        null -> return
    }
    Text(text, style = runTextStyle(12.5.sp, runTone().muted, FontWeight.Medium, 1.35f), textAlign = TextAlign.Center,
        modifier = Modifier.fillMaxWidth().testTag("points-preview-basis"))
}

/** 기록이 없을 때 서버가 가정하는 러닝(분) — 서버가 값을 보내지 않았을 때만 쓴다(계약서 3장 "30분 달리기 하루 한 번") */
private const val DEFAULT_PREVIEW_MINUTES = 30

/** 고른 능력치의 뜻 한 줄(서버 0119) — 효율: 더 빨리 벌고 신발 · 에너지도 빨리 쓴다 · 착화감: 하루에 더 많이 · 내구도: 가득 수리비는 그대로 */
@Composable
private fun pointHelp(stat: PointStat): String = stringResource(
    when (stat) {
        PointStat.EFFICIENCY -> R.string.ers_points_help_efficiency
        PointStat.COMFORT -> R.string.ers_points_help_comfort
        PointStat.DURABILITY -> R.string.ers_points_help_durability
    },
)

/**
 * 되돌릴 수 없다는 한 줄 — 서버 0119 부터는 5레벨마다 한 번 다시 나눌 수 있다고 함께 말한다(예전 서버 · 규칙을 못 읽었으면 예전 글)
 */
@Composable
private fun finalNote(rules: com.stepup.android.domain.ShoeRules): String {
    val every = respecEveryOf(rules)
    return if (every > 0) stringResource(R.string.ers_points_note_final, every) else stringResource(R.string.points_note_final)
}

/**
 * 추천(①) · 직접 나누기(②) 아래 한 줄 — 서버 0119 처럼 5레벨마다 다시 나눌 수 있으면 그 한 줄만("되돌릴 수 없어요"는 확인 ③ 에서 굵게),
 * 아니면 [pointsNote] 그대로
 */
@Composable
private fun sceneNote(basis: PointsBasis): String {
    val every = respecEveryOf(basis.rules)
    return if (!basis.respec && every > 0) stringResource(R.string.rd_b_respec_every, every) else pointsNote(basis)
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
