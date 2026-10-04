package com.stepup.android.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.core.MutableTransitionState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.draggable
import androidx.compose.foundation.gestures.rememberDraggableState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.stepup.android.R
import com.stepup.android.ui.experience.FeedbackCue
import com.stepup.android.ui.experience.LocalFeedback
import com.stepup.android.ui.experience.LocalMotion
import com.stepup.android.ui.experience.feedbackClickable
import com.stepup.android.ui.theme.StepUpColors

/*
 * 시작·로그인·첫 사용 v1(2026-09-28 전달본, docs/redesign/onboarding-v1) 한 벌 — 흰 주 버튼 · 글자 보조 버튼 ·
 * 아래에서 올라오는 시트 · 안내 행 · 권한 아이콘 칸 · 버튼 위 한 줄 안내 · 잠깐 뜨는 안내.
 *
 * 색은 설정 v1 과 같은 값(settingsPalette — 시안 토큰과 같다)이다. 어두운 테마는 시안 값, 밝은 테마는 뒤집힌 값.
 * 높이는 최소만 정한다 — 글씨가 커지거나 번역이 길어지면 늘어난다. 누르는 곳은 모두 48dp 이상.
 */

/** 권한 아이콘 칸 바탕 — 시안 #1A304B(어두운 테마), 밝은 테마는 옅은 파랑 */
private val TileDark = Color(0xFF1A304B)
private val TileLight = Color(0xFFDDE6F5)

/** 주 버튼 — 54dp · 모서리 17. 어두운 테마는 흰 면에 남색 글자, 밝은 테마는 남색 면에 흰 글자 */
@Composable
fun OnboardingPrimaryButton(text: String, onClick: () -> Unit, modifier: Modifier = Modifier, enabled: Boolean = true) {
    val p = settingsPalette()
    val shape = RoundedCornerShape(17.dp)
    Box(
        modifier.heightIn(min = 54.dp).clip(shape).background(p.primaryFace, shape)
            .then(if (enabled) Modifier else Modifier.background(Color.Black.copy(alpha = 0.28f), shape))
            .feedbackClickable(enabled = enabled, role = Role.Button, onClick = onClick)
            .padding(horizontal = 18.dp, vertical = 12.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(text, color = p.primaryText, fontSize = 16.sp, fontWeight = FontWeight.SemiBold, textAlign = TextAlign.Center)
    }
}

/** 보조 행동 — 글자만("나중에" · "먼저 둘러보기" · "홈으로 돌아가기"). 누르는 곳은 48dp 이상 */
@Composable
fun OnboardingTextButton(text: String, onClick: () -> Unit, modifier: Modifier = Modifier) {
    val p = settingsPalette()
    Box(
        modifier.heightIn(min = 48.dp).clip(RoundedCornerShape(12.dp))
            .feedbackClickable(role = Role.Button, onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 10.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(text, color = p.secondary, fontSize = 15.sp, fontWeight = FontWeight.SemiBold, textAlign = TextAlign.Center)
    }
}

/**
 * 아래에서 올라오는 시트 — 위 모서리 27, 손잡이, 제목(24) · 닫기(X). 첫 사용 안내와 러닝 권한 안내가 쓴다.
 *
 * 창이 따로 떠서 뒤 화면은 읽거나 누를 수 없다. 닫기 · 뒤로 · 바깥 · 손잡이를 아래로 끌기가 모두 [onDismiss] 하나로 온다.
 * 들어올 때 220ms 쯤 올라오고, 동작 줄이기에서는 옮기지 않고 바로 보인다. 본문은 내용만큼 높고 넘치면 스크롤한다.
 */
@Composable
fun OnboardingSheet(
    title: String,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
    actions: @Composable ColumnScope.() -> Unit,
    content: @Composable ColumnScope.() -> Unit,
) {
    val p = settingsPalette()
    val feedback = LocalFeedback.current
    val motion = LocalMotion.current
    LaunchedEffect(feedback) { feedback?.play(FeedbackCue.SheetOpen) }
    val dismiss = {
        feedback?.play(FeedbackCue.SheetClose)
        onDismiss()
    }
    Dialog(
        onDismissRequest = dismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false, decorFitsSystemWindows = false),
    ) {
        val shown = remember { MutableTransitionState(false).apply { targetState = true } }
        var dragged by remember { mutableFloatStateOf(0f) }
        val closeDistance = with(LocalDensity.current) { 96.dp.toPx() }
        BoxWithConstraints(Modifier.fillMaxSize().statusBarsPadding(), contentAlignment = Alignment.BottomCenter) {
            Box(Modifier.matchParentSize().pointerInput(Unit) { detectTapGestures { dismiss() } })
            AnimatedVisibility(
                visibleState = shown,
                enter = fadeIn(tween(motion.duration(220))) +
                    if (motion.reduced) EnterTransition.None else slideInVertically(tween(220)) { it / 4 },
            ) {
                Column(
                    modifier.widthIn(max = 600.dp).fillMaxWidth().heightIn(max = maxHeight - 24.dp)
                        .graphicsLayer { translationY = dragged }
                        .clip(RoundedCornerShape(topStart = 27.dp, topEnd = 27.dp)).background(p.sheet)
                        .pointerInput(Unit) { detectTapGestures { } }
                        .navigationBarsPadding(),
                ) {
                    // 손잡이와 제목 줄 — 아래로 끌면 닫힌다(조금만 끌면 제자리로)
                    Column(
                        Modifier.fillMaxWidth().draggable(
                            state = rememberDraggableState { delta -> dragged = (dragged + delta).coerceAtLeast(0f) },
                            orientation = Orientation.Vertical,
                            onDragStopped = { if (dragged > closeDistance) dismiss() else dragged = 0f },
                        ),
                    ) {
                        Box(Modifier.fillMaxWidth().padding(top = 12.dp), contentAlignment = Alignment.Center) {
                            Box(
                                Modifier.size(width = 46.dp, height = 4.dp).clip(RoundedCornerShape(2.dp))
                                    .background(p.secondary.copy(alpha = 0.5f)),
                            )
                        }
                        Row(
                            Modifier.fillMaxWidth().padding(start = 24.dp, end = 12.dp, top = 18.dp, bottom = 4.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            // 제목 오른쪽에 여유를 둔다 — 큰 글씨에서 닫기와 겹치지 않고 줄을 바꾼다
                            Text(
                                title, color = p.text, fontSize = 24.sp, fontWeight = FontWeight.SemiBold,
                                lineHeight = 1.3.em, letterSpacing = (-0.02).em,
                                modifier = Modifier.weight(1f).padding(end = 8.dp).semantics { heading() },
                            )
                            IconButton(onClick = dismiss, modifier = Modifier.size(48.dp).testTag("onboarding-sheet-close")) {
                                Icon(Icons.Filled.Close, stringResource(R.string.common_close), tint = p.secondary)
                            }
                        }
                    }
                    Column(
                        Modifier.weight(1f, fill = false).verticalScroll(rememberScrollState())
                            .padding(horizontal = 24.dp, vertical = 8.dp),
                        content = content,
                    )
                    Column(
                        Modifier.fillMaxWidth().padding(start = 24.dp, end = 24.dp, top = 16.dp, bottom = 12.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        content = actions,
                    )
                }
            }
        }
    }
}

/** 안내 행 — 아이콘 · 이름 · 한 줄 설명. 설명일 뿐 버튼이 아니다(화살표 없음, 누르는 곳 없음) */
@Composable
fun OnboardingFeatureRow(icon: ImageVector, title: String, body: String, modifier: Modifier = Modifier) {
    val p = settingsPalette()
    Row(
        modifier.fillMaxWidth().semantics(mergeDescendants = true) {}.padding(vertical = 14.dp),
        horizontalArrangement = Arrangement.spacedBy(20.dp),
        verticalAlignment = Alignment.Top,
    ) {
        Icon(icon, contentDescription = null, tint = p.accent, modifier = Modifier.padding(top = 2.dp).size(25.dp))
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text(title, color = p.text, fontSize = 17.sp, fontWeight = FontWeight.SemiBold, lineHeight = 1.3.em)
            Text(body, color = p.secondary, fontSize = 14.sp, lineHeight = 1.45.em)
        }
    }
}

/** 권한 안내의 아이콘 칸 — 52dp, 모서리 16. 뜻을 돕는 그림일 뿐 상태는 글자로 말한다 */
@Composable
fun OnboardingIconTile(icon: ImageVector, modifier: Modifier = Modifier) {
    val p = settingsPalette()
    Box(
        modifier.size(52.dp).clip(RoundedCornerShape(16.dp)).background(if (StepUpColors.dark) TileDark else TileLight),
        contentAlignment = Alignment.Center,
    ) {
        Icon(icon, contentDescription = null, tint = p.accent, modifier = Modifier.size(24.dp))
    }
}

/** 권한 안내 본문 — 아이콘 칸 · 큰 한 줄 · 설명 */
@Composable
fun OnboardingSheetBody(icon: ImageVector, headline: String, body: String, modifier: Modifier = Modifier) {
    val p = settingsPalette()
    Column(modifier.fillMaxWidth().padding(top = 18.dp, bottom = 28.dp)) {
        OnboardingIconTile(icon)
        Spacer(Modifier.height(24.dp))
        Text(headline, color = p.text, fontSize = 21.sp, fontWeight = FontWeight.SemiBold, lineHeight = 1.35.em,
            letterSpacing = (-0.01).em)
        Spacer(Modifier.height(14.dp))
        Text(body, color = p.secondary, fontSize = 14.sp, lineHeight = 1.8.em)
    }
}

/** 버튼 위 한 줄 안내 — 로그인의 연결 실패 · 로그인 실패 · 계정 없음 · 다시 로그인(시안 06~09). 스크린리더가 바로 읽는다 */
@Composable
fun OnboardingNotice(title: String, body: String, modifier: Modifier = Modifier) {
    val p = settingsPalette()
    Column(
        modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp)).background(p.surface)
            .semantics(mergeDescendants = true) { liveRegion = LiveRegionMode.Polite }
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        Text(title, color = p.text, fontSize = 16.sp, fontWeight = FontWeight.SemiBold, lineHeight = 1.35.em)
        Text(body, color = p.secondary, fontSize = 13.sp, lineHeight = 1.45.em)
    }
}

/** 잠깐 뜨는 안내 — 약관 링크를 열지 못했을 때(시안 10). 가운데 두 줄 */
@Composable
fun OnboardingToast(title: String, body: String, modifier: Modifier = Modifier) {
    val p = settingsPalette()
    Column(
        modifier.widthIn(max = 520.dp).fillMaxWidth().clip(RoundedCornerShape(15.dp)).background(p.toast)
            .semantics(mergeDescendants = true) { liveRegion = LiveRegionMode.Polite }
            .padding(horizontal = 18.dp, vertical = 12.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        Text(title, color = p.toastText, fontSize = 14.sp, fontWeight = FontWeight.SemiBold, textAlign = TextAlign.Center)
        Text(body, color = p.toastText, fontSize = 13.sp, textAlign = TextAlign.Center, lineHeight = 1.4.em)
    }
}

/**
 * 아치 풍경 — 위가 반원인 틀에 기존 강변 사진(시안의 river-night 와 같은 파일)을 담는다.
 * 사진 위에 시안의 옅은 남색 막과 아래로 바닥색에 녹아드는 덮개(위 .32 · 가운데 .12 · 아래 바닥색)를 얹는다. 장식이라 읽지 않는다.
 */
@Composable
fun OnboardingArch(@androidx.annotation.DrawableRes image: Int, modifier: Modifier = Modifier) {
    val shape = RoundedCornerShape(topStartPercent = 50, topEndPercent = 50)
    val ground = com.stepup.android.ui.theme.Night
    Box(modifier.clip(shape).background(StepUpColors.carbon)) {
        androidx.compose.foundation.Image(
            painter = cachedPainterResource(image), contentDescription = null,
            contentScale = androidx.compose.ui.layout.ContentScale.Crop, modifier = Modifier.fillMaxSize(),
        )
        Box(Modifier.fillMaxSize().background(Color(0x38071224)))
        Box(
            Modifier.fillMaxSize().background(
                Brush.verticalGradient(0f to ground.copy(alpha = 0.32f), 0.53f to ground.copy(alpha = 0.12f), 1f to ground),
            ),
        )
    }
}
