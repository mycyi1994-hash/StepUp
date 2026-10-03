package com.stepup.android.ui.screens.onboarding

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.stepup.android.R
import com.stepup.android.domain.FirstGuideAction
import com.stepup.android.ui.Screen
import com.stepup.android.ui.components.RunBackdrop
import com.stepup.android.ui.components.RunButton
import com.stepup.android.ui.components.RunSheet
import com.stepup.android.ui.components.runTextStyle
import com.stepup.android.ui.components.runTone

/*
 * 파란 톤 통합 전달본 v4(2026-10-03) — 첫 사용 안내(ONB02)와 사용 안내 다시 보기(ONB20).
 * 한 장으로 끝나는 안내다. 마켓 · 도감 안내는 넣지 않는다.
 * 세 줄의 그림은 하단 탭의 그림 그대로다 — 안내를 닫은 뒤 같은 그림을 아래 탭에서 찾게. 세 줄은 설명일 뿐 버튼이 아니다.
 */

/** 러닝 · 신발 · 내 정보 세 줄 — 이름은 하단 탭 이름 그대로. 설명일 뿐 버튼이 아니다(화살표 · 누르는 곳 없음) */
@Composable
private fun GuideRows(runBody: Int, shoesBody: Int, meBody: Int, replay: Boolean) {
    val t = runTone()
    val rows = listOf(Screen.Run to runBody, Screen.Customize to shoesBody, Screen.Profile to meBody)
    if (replay) BlueRule(strong = true)
    rows.forEach { (tab, body) ->
        Row(
            Modifier.fillMaxWidth().semantics(mergeDescendants = true) {}.testTag("guide-row-${tab.route}")
                .padding(vertical = if (replay) 22.dp else 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(if (replay) 24.dp else 18.dp),
        ) {
            if (replay) {
                // 다시 보기 — 시안 20 은 바탕 없는 선 그림
                Box(Modifier.size(52.dp), contentAlignment = Alignment.Center) {
                    Icon(tab.icon, contentDescription = null, tint = t.label, modifier = Modifier.size(38.dp))
                }
            } else {
                // 첫 안내 — 시안 02 는 남색 원 안의 청록 그림
                Box(
                    Modifier.size(60.dp).clip(CircleShape).background(t.inset),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(tab.icon, contentDescription = null, tint = t.cyan, modifier = Modifier.size(30.dp))
                }
            }
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(stringResource(tab.labelRes), style = runTextStyle(19.sp, t.text, FontWeight.Bold, 1.3f))
                Text(stringResource(body), style = runTextStyle(15.sp, t.label, FontWeight.Medium, 1.45f))
            }
        }
        if (replay) BlueRule(strong = true)
    }
}

/**
 * 첫 사용 안내(ONB02) — 새로 가입해 처음 러닝 홈에 온 사람에게 한 번. 한 장에서 끝난다. 시트는 하단 탭까지 덮는다.
 *
 * "러닝 시작"은 러닝 방법 고르기(U01)로 갈 뿐 운동 기록을 시작하지 않는다. "먼저 둘러보기" · X · 뒤로 · 바깥 · 아래로 끌기는 모두
 * 홈에 머문다. 어느 쪽이든 고르면 부른 쪽이 "봤음"을 적는다 — 앱을 끄거나 강제로 멈추면 적지 않는다.
 * 큰 글씨에서는 본문만 넘기고 두 행동은 시트 아래에 붙어 있다.
 */
@Composable
fun FirstGuideSheet(onAction: (FirstGuideAction) -> Unit) {
    val t = runTone()
    RunSheet(
        onDismiss = { onAction(FirstGuideAction.Dismiss) },
        modifier = Modifier.testTag("first-guide-sheet"),
        closeTag = "onboarding-sheet-close",
    ) {
        Column(Modifier.fillMaxWidth().weight(1f, fill = false).verticalScroll(rememberScrollState())) {
            Spacer(Modifier.height(6.dp))
            BlueHeadline(
                stringResource(R.string.onb_guide_title),
                subtitle = null, size = 34,
                modifier = Modifier.padding(end = 40.dp),
            )
            Spacer(Modifier.height(8.dp))
            Text(stringResource(R.string.onb_guide_subtitle), style = runTextStyle(17.sp, t.cobaltText, FontWeight.Medium, 1.45f))
            Spacer(Modifier.height(18.dp))
            GuideRows(R.string.onb_guide_run_body, R.string.onb_guide_shoes_body, R.string.onb_guide_me_body, replay = false)
            Spacer(Modifier.height(18.dp))
        }
        RunButton(
            stringResource(R.string.home_start_run), { onAction(FirstGuideAction.StartRun) },
            Modifier.testTag("first-guide-start"), italic = true,
        )
        Spacer(Modifier.height(4.dp))
        BlueTextButton(
            stringResource(R.string.onb_guide_browse), { onAction(FirstGuideAction.Browse) },
            Modifier.fillMaxWidth().testTag("first-guide-browse"),
        )
    }
}

/**
 * 사용 안내 다시 보기(ONB20) — 내 정보 › 설정 › 도움말 · 문의 › 앱 사용 안내. 머리는 뒤로 + 제목만, 하단 탭 없음.
 * 뒤로 가면 도움말로 돌아간다. 가이드를 "안 봄"으로 되돌리지 않는다(첫 안내가 다시 뜨지 않는다). "러닝 홈으로"는 러닝 탭 첫 화면.
 */
@Composable
fun GuideReplayScreen(onBack: () -> Unit, onRunHome: () -> Unit) {
    val t = runTone()
    Box(Modifier.fillMaxSize().testTag("guide-replay")) {
        RunBackdrop(Modifier.fillMaxSize())
        Column(Modifier.fillMaxSize().safeDrawingPadding()) {
            BlueTitleBar(stringResource(R.string.set_guide), onBack = onBack)
            Column(
                Modifier.weight(1f).fillMaxWidth().verticalScroll(rememberScrollState()).padding(horizontal = 24.dp),
            ) {
                Spacer(Modifier.height(24.dp))
                BlueHeadline(stringResource(R.string.onb_replay_headline), size = 38)
                Spacer(Modifier.height(14.dp))
                Text(stringResource(R.string.onb_replay_subtitle), style = runTextStyle(17.sp, t.cobaltText, FontWeight.Medium, 1.45f))
                Spacer(Modifier.height(32.dp))
                GuideRows(R.string.onb_replay_run_body, R.string.onb_replay_shoes_body, R.string.onb_replay_me_body, replay = true)
                Spacer(Modifier.height(36.dp))
                Text(stringResource(R.string.onb_replay_permission_note), style = runTextStyle(15.sp, t.label, FontWeight.Medium, 1.5f))
                Spacer(Modifier.height(24.dp))
            }
            BluePlainButton(
                stringResource(R.string.onb_replay_home), onRunHome,
                Modifier.fillMaxWidth().padding(horizontal = 20.dp).padding(top = 8.dp, bottom = 16.dp).testTag("guide-replay-home"),
            )
        }
    }
}
