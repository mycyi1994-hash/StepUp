package com.stepup.android.ui.screens.notifications

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import com.stepup.android.ui.components.RunSheet
import com.stepup.android.ui.components.runTextStyle
import com.stepup.android.ui.components.runTone
import com.stepup.android.ui.experience.LocalMotion
import com.stepup.android.ui.experience.feedbackClickable
import com.stepup.android.ui.screens.onboarding.BlueOutlineButton
import com.stepup.android.ui.screens.onboarding.BluePlainButton
import com.stepup.android.ui.screens.onboarding.BlueRule
import com.stepup.android.ui.theme.StepUpSans

/*
 * 파란 톤 통합 전달본 v4(2026-10-03) 알림·공지(NOT01~26) 부품 — 내용 시트 · 결과 칸 · 이동 줄 · 버튼 한 쌍 · 짧은 알림 · 빈/실패 화면.
 * 색은 runTone() 그대로(남색 · 전기 파랑 · 청록 · 흰 글자). 한 화면에 흰 주 버튼은 하나, 보조는 남색 테두리 버튼이나 글자.
 */

/**
 * 알림 내용 시트 — 하단 탭까지 덮고 뒤 목록을 어둡게 한다. 위에 작은 이름(적립 알림 · 크루 초대), 그 아래 내용, 맨 아래 버튼.
 * X · 뒤로 · 바깥 · 아래로 끌기는 [onDismiss](목록으로). [dismissible] 이 아니면(거절 저장 중) 닫히지 않는다.
 * 큰 글씨에서는 내용만 넘기고 버튼은 시트 아래에 붙어 있다.
 */
@Composable
internal fun NotifSheet(
    kicker: String?,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
    dismissible: Boolean = true,
    showClose: Boolean = true,
    actions: @Composable ColumnScope.() -> Unit,
    content: @Composable ColumnScope.() -> Unit,
) {
    val t = runTone()
    RunSheet(
        onDismiss = onDismiss, modifier = modifier, showClose = showClose, dismissible = dismissible,
        closeTag = "settings-sheet-close",
    ) {
        Column(
            Modifier.fillMaxWidth().weight(1f, fill = false).verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            if (kicker != null) {
                Text(kicker, style = runTextStyle(17.sp, t.label, FontWeight.SemiBold), modifier = Modifier.padding(end = 48.dp))
            } else {
                Spacer(Modifier.height(18.dp))
            }
            content()
            Spacer(Modifier.height(6.dp))
        }
        Column(Modifier.fillMaxWidth().padding(top = 6.dp), verticalArrangement = Arrangement.spacedBy(10.dp), content = actions)
    }
}

/** 시트의 큰 글 — 알림 본문 · 크루 이름 · 확인 질문 */
@Composable
internal fun SheetHeadline(text: String, modifier: Modifier = Modifier, big: Boolean = false) {
    val t = runTone()
    Text(
        text,
        style = TextStyle(
            fontFamily = StepUpSans, fontWeight = FontWeight.ExtraBold, fontSize = if (big) 32.sp else 29.sp,
            lineHeight = 1.3.em, letterSpacing = (-0.02).em, color = t.text,
        ),
        modifier = modifier.padding(top = 2.dp).semantics { heading() },
    )
}

/** 시트의 굵은 한 줄 — "크루에서 초대가 왔어요." */
@Composable
internal fun SheetLead(text: String, modifier: Modifier = Modifier) {
    Text(text, style = runTextStyle(20.sp, runTone().text, FontWeight.Bold, 1.4f), modifier = modifier)
}

/** 시트의 보조 설명 — 옅은 파랑 */
@Composable
internal fun SheetNote(text: String, modifier: Modifier = Modifier) {
    Text(text, style = runTextStyle(17.sp, runTone().label, FontWeight.Medium, 1.5f), modifier = modifier)
}

/** 시트 안 가로선 */
@Composable
internal fun SheetRule() {
    BlueRule(Modifier.padding(vertical = 6.dp), strong = true)
}

/** 서버가 확인한 결과 한 칸 — 가입 완료(26) · 가입 신청(06) · 응답한 초대 · 수락 표시. 파란 테두리 칸, 청록 선 그림 */
@Composable
internal fun StatusBox(icon: ImageVector, title: String, body: String, tag: String) {
    val t = runTone()
    val shape = RoundedCornerShape(18.dp)
    Row(
        Modifier.fillMaxWidth().clip(shape).background(t.panel.copy(alpha = 0.6f), shape)
            .border(1.5.dp, t.secondaryEdge, shape)
            .semantics(mergeDescendants = true) { liveRegion = LiveRegionMode.Polite }.testTag(tag)
            .padding(horizontal = 20.dp, vertical = 24.dp),
        horizontalArrangement = Arrangement.spacedBy(18.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(icon, contentDescription = null, tint = t.cyan, modifier = Modifier.size(44.dp))
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text(title, style = runTextStyle(22.sp, t.text, FontWeight.ExtraBold, 1.3f))
            Text(body, style = runTextStyle(16.sp, t.label, FontWeight.Medium, 1.45f))
        }
    }
}

/** 처리되지 않았거나 모를 때 — 테두리 칸 안의 정보 그림 · 굵은 한 줄 · 설명. 초대는 그대로 남아 있다 */
@Composable
internal fun ProblemBox(title: String, body: String?, tag: String) {
    val t = runTone()
    val shape = RoundedCornerShape(16.dp)
    Row(
        Modifier.fillMaxWidth().clip(shape).border(1.5.dp, t.secondaryEdge, shape)
            .semantics(mergeDescendants = true) { liveRegion = LiveRegionMode.Polite }.testTag(tag)
            .padding(horizontal = 18.dp, vertical = 16.dp),
        horizontalArrangement = Arrangement.spacedBy(16.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(Icons.Outlined.Info, contentDescription = null, tint = t.label, modifier = Modifier.size(30.dp))
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(title, style = runTextStyle(17.sp, t.text, FontWeight.Bold, 1.35f))
            if (body != null) Text(body, style = runTextStyle(15.sp, t.label, FontWeight.Medium, 1.45f))
        }
    }
}

/** "크루 보기 >" · "대기실 보기 >" — 테두리 칸 전체가 누르는 곳. 보기만 한다(수락이 아니다) */
@Composable
internal fun LinkRow(label: String, tag: String, onClick: () -> Unit) {
    val t = runTone()
    val shape = RoundedCornerShape(16.dp)
    Row(
        Modifier.fillMaxWidth().heightIn(min = 56.dp).clip(shape).border(1.5.dp, t.secondaryEdge, shape)
            .feedbackClickable(onClick = onClick).testTag(tag)
            .padding(start = 20.dp, end = 14.dp, top = 8.dp, bottom = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(label, style = runTextStyle(19.sp, t.text, FontWeight.Bold), modifier = Modifier.weight(1f))
        Icon(Icons.Filled.ChevronRight, contentDescription = null, tint = t.text, modifier = Modifier.size(28.dp))
    }
}

/** 보조(왼쪽 · 테두리) · 주(오른쪽 · 흰 면) 버튼 한 쌍. 처리 중이면 둘 다 막고 주 버튼이 돈다 */
@Composable
internal fun ButtonPair(
    secondary: String,
    onSecondary: () -> Unit,
    primary: String,
    onPrimary: () -> Unit,
    busy: Boolean = false,
    secondaryTag: String = "sheet-secondary",
    primaryTag: String = "sheet-primary",
) {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(14.dp), verticalAlignment = Alignment.Top) {
        BlueOutlineButton(secondary, onSecondary, Modifier.weight(0.75f).testTag(secondaryTag), enabled = !busy)
        BluePlainButton(primary, onPrimary, Modifier.weight(1f).testTag(primaryTag), busy = busy)
    }
}

/**
 * 화면 아래 짧은 알림 — 남색 칸 · 결과 그림(성공은 체크, 실패는 정보) · 한 줄. 읽음 저장 결과 같은 것.
 * 꼬리표는 예전 설정 알림과 같은 "settings-toast"(검사가 같은 이름으로 찾는다).
 */
@Composable
internal fun BlueToast(message: String?, modifier: Modifier = Modifier, success: Boolean = true) {
    val t = runTone()
    val motion = LocalMotion.current
    AnimatedVisibility(
        visible = message != null, modifier = modifier,
        enter = fadeIn(tween(motion.duration(160))), exit = fadeOut(tween(motion.duration(160))),
    ) {
        val shape = RoundedCornerShape(16.dp)
        Row(
            Modifier.widthIn(max = 520.dp).fillMaxWidth().clip(shape).background(t.sheetTop, shape)
                .border(1.dp, t.panelEdge, shape)
                .semantics { liveRegion = LiveRegionMode.Polite }
                .padding(horizontal = 18.dp, vertical = 16.dp)
                .testTag("settings-toast"),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            Icon(if (success) Icons.Outlined.CheckCircle else Icons.Outlined.Info, contentDescription = null,
                tint = t.label, modifier = Modifier.size(28.dp))
            Text(message.orEmpty(), style = runTextStyle(16.sp, t.text, FontWeight.Medium, 1.45f))
        }
    }
}

/** 비었을 때 · 못 읽었을 때 — 가운데 선 그림 · 굵은 제목 · 설명 · (버튼 하나). 큰 글씨면 넘긴다 */
@Composable
internal fun PaneState(
    icon: ImageVector,
    title: String,
    body: String,
    modifier: Modifier = Modifier,
    actionLabel: String? = null,
    onAction: () -> Unit = {},
    scroll: Boolean = true,
) {
    val t = runTone()
    Column(
        modifier.fillMaxWidth().then(if (scroll) Modifier.fillMaxSize().verticalScroll(rememberScrollState()) else Modifier)
            .padding(top = 96.dp, bottom = 24.dp, start = 8.dp, end = 8.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Box(Modifier.size(84.dp), contentAlignment = Alignment.Center) {
            Icon(icon, contentDescription = null, tint = t.label, modifier = Modifier.size(64.dp))
        }
        Text(
            title, style = runTextStyle(25.sp, t.text, FontWeight.ExtraBold, 1.3f), textAlign = TextAlign.Center,
            modifier = Modifier.semantics { heading() },
        )
        Text(body, style = runTextStyle(17.sp, t.label, FontWeight.Medium, 1.5f), textAlign = TextAlign.Center)
        if (actionLabel != null) {
            Spacer(Modifier.height(10.dp))
            BluePlainButton(actionLabel, onAction, Modifier.widthIn(min = 240.dp).testTag("pane-action"))
        }
    }
}

/** 불러오는 자리 칸 — 남색 면에 둥근 막대 */
@Composable
internal fun SkeletonBar(widthFraction: Float, height: androidx.compose.ui.unit.Dp, modifier: Modifier = Modifier) {
    val t = runTone()
    Box(modifier.fillMaxWidth(widthFraction).height(height).clip(RoundedCornerShape(height / 2.6f)).background(t.panelEdge.copy(alpha = 0.5f)))
}
