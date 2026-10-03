package com.stepup.android.ui.screens.onboarding

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
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import com.stepup.android.R
import com.stepup.android.ui.components.RunSpinner
import com.stepup.android.ui.components.runTextStyle
import com.stepup.android.ui.components.runTone
import com.stepup.android.ui.experience.FeedbackCue
import com.stepup.android.ui.experience.feedbackClickable
import com.stepup.android.ui.theme.StepUpSans

/*
 * 파란 톤 통합 전달본 v4(2026-10-03) — 시작·로그인·첫 설정(ONB) · 알림·공지(NOT)가 함께 쓰는 보조 화면 부품.
 *
 * 색은 러닝 리메이크의 runTone()(남색 #031427 · 면 #0B2B50 · 전기 파랑 #0754FF · 청록 #48D9FA · 흰 글자 #F5F8FF)을 그대로 쓴다.
 * 러닝 화면의 주 버튼은 모서리를 깎은 게임식 버튼이지만, 시안의 안내 · 첫 설정 · 알림 화면 주 버튼은 둥근 흰 면에 얇은 파란
 * 아랫면이다 — 같은 색 · 같은 아랫면으로 모양만 둥글게 둔다. 높이는 최소만 정한다(큰 글씨 · 긴 번역이면 늘어난다).
 */

private val PlainRadius = 14.dp
private val PlainBase = 4.dp

/** 둥근 흰 주 버튼 — 흰 면 · 남색 굵은 글자 · 얇은 파란 아랫면. [busy] 면 글자 앞에 도는 표시(누를 수 없다) */
@Composable
internal fun BluePlainButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    busy: Boolean = false,
) {
    val t = runTone()
    val active = enabled && !busy
    val face = if (active) t.primaryFace else listOf(t.disabledFace, t.disabledFace)
    val ink = if (active) t.primaryInk else t.disabledInk
    val base = if (active) t.primaryBase else t.disabledBase
    Box(
        modifier.heightIn(min = 56.dp + PlainBase)
            .drawBehind {
                val r = CornerRadius(PlainRadius.toPx())
                val b = PlainBase.toPx()
                drawRoundRect(base, topLeft = Offset(0f, b), size = Size(size.width, size.height - b), cornerRadius = r)
                drawRoundRect(Brush.verticalGradient(face, endY = size.height - b), size = Size(size.width, size.height - b), cornerRadius = r)
            }
            .clip(RoundedCornerShape(PlainRadius))
            .feedbackClickable(enabled = active, onClick = onClick)
            .semantics { if (busy) contentDescription = text }
            .padding(bottom = PlainBase)
            .padding(horizontal = 18.dp, vertical = 12.dp),
        contentAlignment = Alignment.Center,
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.Center) {
            if (busy) {
                RunSpinner(Modifier.size(20.dp), color = ink, track = ink.copy(alpha = 0.25f))
                Spacer(Modifier.width(10.dp))
            }
            Text(text, style = runTextStyle(18.sp, ink, FontWeight.Bold, 1.25f), textAlign = TextAlign.Center)
        }
    }
}

/** 둥근 보조 버튼 — 남색 면 · 파란 테두리(거절 · 취소 · 나중에) */
@Composable
internal fun BlueOutlineButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
) {
    val t = runTone()
    val shape = RoundedCornerShape(PlainRadius)
    Box(
        modifier.heightIn(min = 56.dp).clip(shape).background(t.secondaryFace, shape)
            .border(1.5.dp, if (enabled) t.secondaryEdge else t.divider, shape)
            .feedbackClickable(enabled = enabled, onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 12.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(text, style = runTextStyle(17.sp, if (enabled) t.secondaryInk else t.muted, FontWeight.SemiBold, 1.25f),
            textAlign = TextAlign.Center)
    }
}

/** 글자만 있는 보조 행동 — "건너뛰기" · "먼저 둘러보기". 누르는 곳은 48dp 이상 */
@Composable
internal fun BlueTextButton(text: String, onClick: () -> Unit, modifier: Modifier = Modifier, enabled: Boolean = true) {
    val t = runTone()
    Box(
        modifier.heightIn(min = 48.dp).clip(RoundedCornerShape(12.dp))
            .feedbackClickable(enabled = enabled, onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 10.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(text, style = runTextStyle(16.sp, if (enabled) t.label else t.muted, FontWeight.SemiBold), textAlign = TextAlign.Center)
    }
}

/**
 * 보조 화면 머리 — 왼쪽 뒤로(가는 화살표) · 가운데 제목 · 오른쪽 작은 행동 하나(설정 · 단계 표시).
 * 로고 · SUP 잔액은 두지 않는다(보조 화면). 제목은 양쪽 자리를 비워 늘 가운데에 선다.
 */
@Composable
internal fun BlueTitleBar(
    title: String?,
    onBack: (() -> Unit)?,
    modifier: Modifier = Modifier,
    backTag: String = "blue-back",
    trailing: (@Composable () -> Unit)? = null,
) {
    val t = runTone()
    Box(modifier.fillMaxWidth().heightIn(min = 56.dp).padding(horizontal = 4.dp), contentAlignment = Alignment.Center) {
        if (title != null) {
            Text(
                title, style = runTextStyle(19.sp, t.text, FontWeight.Bold), maxLines = 1, overflow = TextOverflow.Ellipsis,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth().padding(horizontal = 64.dp).semantics { heading() },
            )
        }
        if (onBack != null) {
            com.stepup.android.ui.components.RunBackButton(onBack, Modifier.align(Alignment.CenterStart), tag = backTag, tint = t.text)
        }
        if (trailing != null) {
            Box(Modifier.align(Alignment.CenterEnd).heightIn(min = 48.dp).widthIn(min = 48.dp), contentAlignment = Alignment.CenterEnd) {
                trailing()
            }
        }
    }
}

/** 머리 오른쪽 글자 행동 — "설정" */
@Composable
internal fun BlueHeaderText(text: String, onClick: () -> Unit, modifier: Modifier = Modifier) {
    val t = runTone()
    Box(
        modifier.heightIn(min = 48.dp).widthIn(min = 48.dp).clip(RoundedCornerShape(12.dp))
            .feedbackClickable(onClick = onClick).padding(horizontal = 12.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(text, style = runTextStyle(17.sp, t.label, FontWeight.SemiBold))
    }
}

/** 큰 제목 — 굵은 두 줄 제목 · 한 줄 설명(왼쪽 정렬) */
@Composable
internal fun BlueHeadline(title: String, modifier: Modifier = Modifier, subtitle: String? = null, size: Int = 32) {
    val t = runTone()
    Column(modifier.fillMaxWidth()) {
        Text(
            title,
            style = TextStyle(
                fontFamily = StepUpSans, fontWeight = FontWeight.ExtraBold, fontSize = size.sp, lineHeight = 1.28.em,
                letterSpacing = (-0.03).em, color = t.text,
            ),
            modifier = Modifier.semantics { heading() },
        )
        if (subtitle != null) {
            Spacer(Modifier.height(10.dp))
            Text(subtitle, style = runTextStyle(16.sp, t.label, FontWeight.Medium, 1.45f))
        }
    }
}

/** 가는 가로선 — 파란 둘레 색 */
@Composable
internal fun BlueRule(modifier: Modifier = Modifier, strong: Boolean = false) {
    val t = runTone()
    Box(modifier.fillMaxWidth().height(1.dp).background(if (strong) t.panelEdge else t.divider))
}
