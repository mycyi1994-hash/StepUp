package com.stepup.android.ui.screens.onboarding

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import com.stepup.android.R
import com.stepup.android.domain.FirstGuideAction
import com.stepup.android.ui.Screen
import com.stepup.android.ui.components.OnboardingFeatureRow
import com.stepup.android.ui.components.OnboardingPrimaryButton
import com.stepup.android.ui.components.OnboardingSheet
import com.stepup.android.ui.components.OnboardingTextButton
import com.stepup.android.ui.components.S2Stage
import com.stepup.android.ui.components.SecondaryHeader
import com.stepup.android.ui.components.settingsPalette

/*
 * 시작·로그인·첫 사용 v1(2026-09-28 전달본, docs/redesign/onboarding-v1) — 첫 사용 안내(시안 02)와 사용 안내 다시 보기(시안 20).
 * 예전 세 단계 스포트라이트 투어(탭을 차례로 옮겨 다니며 테두리를 깜빡이던 것)를 한 장으로 줄였다. 마켓 · 도감 안내는 넣지 않는다.
 * 세 줄의 그림은 하단 탭의 그림 그대로다 — 안내를 닫은 뒤 같은 그림을 아래 탭에서 찾게.
 */

/** 러닝 · 신발 · 내 정보 세 줄 — 이름은 하단 탭 이름 그대로. 설명일 뿐 버튼이 아니다 */
@Composable
private fun GuideRows(runBody: Int, shoesBody: Int, meBody: Int, dividers: Boolean) {
    val p = settingsPalette()
    val rows = listOf(Screen.Run to runBody, Screen.Customize to shoesBody, Screen.Profile to meBody)
    rows.forEachIndexed { index, (tab, body) ->
        OnboardingFeatureRow(tab.icon, stringResource(tab.labelRes), stringResource(body), Modifier.testTag("guide-row-${tab.route}"))
        if (dividers && index < rows.lastIndex) {
            Box(Modifier.fillMaxWidth().padding(vertical = 6.dp).height(1.dp).background(p.divider))
        }
    }
}

/**
 * 첫 사용 안내(시안 02) — 새로 가입해 처음 러닝 홈에 온 사람에게 한 번. 한 장에서 끝난다.
 *
 * "러닝 시작"은 러닝 방법 고르기(12)로 갈 뿐 운동 기록을 시작하지 않는다. "먼저 둘러보기" · X · 뒤로 · 바깥 · 아래로 끌기는 모두
 * 홈에 머문다. 어느 쪽이든 고르면 부른 쪽이 "봤음"을 적는다 — 앱을 끄거나 강제로 멈추면 적지 않는다.
 */
@Composable
fun FirstGuideSheet(onAction: (FirstGuideAction) -> Unit) {
    val p = settingsPalette()
    OnboardingSheet(
        title = stringResource(R.string.onb_guide_title),
        onDismiss = { onAction(FirstGuideAction.Dismiss) },
        modifier = Modifier.testTag("first-guide-sheet"),
        actions = {
            OnboardingPrimaryButton(
                stringResource(R.string.home_start_run), { onAction(FirstGuideAction.StartRun) },
                Modifier.fillMaxWidth().testTag("first-guide-start"),
            )
            OnboardingTextButton(
                stringResource(R.string.onb_guide_browse), { onAction(FirstGuideAction.Browse) },
                Modifier.fillMaxWidth().testTag("first-guide-browse"),
            )
        },
    ) {
        Text(stringResource(R.string.onb_guide_subtitle), color = p.secondary, fontSize = 14.sp, lineHeight = 1.5.em)
        Spacer(Modifier.height(24.dp))
        GuideRows(R.string.onb_guide_run_body, R.string.onb_guide_shoes_body, R.string.onb_guide_me_body, dividers = false)
        Spacer(Modifier.height(12.dp))
    }
}

/**
 * 사용 안내 다시 보기(시안 20) — 내 정보 › 설정 › 도움말 · 문의 › 앱 사용 안내. 뒤로 가면 도움말로 돌아간다.
 * 가이드를 "안 봄"으로 되돌리지 않는다(첫 안내가 다시 뜨지 않는다). "러닝 홈으로"는 러닝 탭 첫 화면으로 간다.
 */
@Composable
fun GuideReplayScreen(onBack: () -> Unit, onRunHome: () -> Unit) {
    val p = settingsPalette()
    Box(Modifier.fillMaxSize().testTag("guide-replay")) {
        S2Stage(Modifier.fillMaxSize())
        Column(Modifier.fillMaxSize().safeDrawingPadding()) {
            SecondaryHeader(
                onBack = onBack, balance = null, onOpenWallet = null, title = stringResource(R.string.set_guide),
                modifier = Modifier.padding(horizontal = 12.dp),
            )
            Column(
                Modifier.weight(1f).fillMaxWidth().verticalScroll(rememberScrollState()).padding(horizontal = 24.dp),
            ) {
                Spacer(Modifier.height(28.dp))
                Text(
                    stringResource(R.string.onb_replay_headline), color = p.text, fontSize = 28.sp,
                    fontWeight = FontWeight.SemiBold, lineHeight = 1.3.em, letterSpacing = (-0.025).em,
                    modifier = Modifier.semantics { heading() },
                )
                Spacer(Modifier.height(12.dp))
                Text(stringResource(R.string.onb_replay_subtitle), color = p.secondary, fontSize = 14.sp, lineHeight = 1.5.em)
                Spacer(Modifier.height(36.dp))
                GuideRows(R.string.onb_replay_run_body, R.string.onb_replay_shoes_body, R.string.onb_replay_me_body, dividers = true)
                Spacer(Modifier.height(40.dp))
                Text(stringResource(R.string.onb_replay_permission_note), color = p.secondary, fontSize = 14.sp, lineHeight = 1.5.em)
                Spacer(Modifier.height(24.dp))
            }
            OnboardingPrimaryButton(
                stringResource(R.string.onb_replay_home), onRunHome,
                Modifier.fillMaxWidth().padding(horizontal = 24.dp).padding(bottom = 16.dp).testTag("guide-replay-home"),
            )
        }
    }
}
