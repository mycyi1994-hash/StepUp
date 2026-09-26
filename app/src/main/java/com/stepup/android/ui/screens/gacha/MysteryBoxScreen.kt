package com.stepup.android.ui.screens.gacha

import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.Row
import androidx.compose.material3.TextButton
import androidx.compose.ui.platform.testTag
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.filled.CardGiftcard
import com.stepup.android.ui.components.S2Arch
import com.stepup.android.ui.components.S2Headline
import com.stepup.android.ui.components.S2Kicker
import com.stepup.android.ui.components.S2RoundAction
import com.stepup.android.ui.components.S2Subtitle
import com.stepup.android.ui.components.s2ArchHeight
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.stepup.android.R
import com.stepup.android.ui.components.ambientPhase
import com.stepup.android.ui.experience.LocalMotion
import com.stepup.android.ui.experience.LocalFeedback
import com.stepup.android.ui.experience.FeedbackCue
import com.stepup.android.ui.theme.Cyan
import com.stepup.android.ui.theme.Silver
import com.stepup.android.ui.theme.StepUpDesign
import com.stepup.android.ui.theme.Volt

/** Art, state and tap targets stay separate so a deployed draw contract can enable the actions. */
@Composable
fun MysteryBoxScreen(
    shoeDrawReady: Boolean = false,
    outfitDrawReady: Boolean = false,
    onDrawShoe: () -> Unit = {},
    onDrawOutfit: () -> Unit = {},
    onOpenDex: () -> Unit = {},
    onOpenWallet: () -> Unit = {},
    /** 뽑기 버튼 글자 — 무료 남은 수나 가격. 비우면 "신발 뽑기" */
    drawLabel: String? = null,
    /** 신발 탭 안의 "내 신발"로 돌아간다 */
    onOpenShoes: () -> Unit = {},
) {
    val feedback = LocalFeedback.current
    LaunchedEffect(feedback) { feedback?.play(FeedbackCue.DrawEnter) }
    val pulse = if (LocalMotion.current.decorative) ambientPhase(3400, reverse = true) else null
    // S2 뽑기 — 가운데 아치 틀 안의 상자, 아래 흰 원형 뽑기. 상자 그림은 기존 자산이다(S2 상자는 미확보).
    Column(
        modifier = Modifier.fillMaxSize()
            .padding(horizontal = StepUpDesign.Gutter)
            .padding(bottom = 12.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        com.stepup.android.ui.components.S2ShoesSections(drawSelected = true, onShoes = onOpenShoes, onDraw = {})
        Column(Modifier.weight(1f).fillMaxWidth().verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally) {
            Box(Modifier.height(12.dp))
            S2Kicker(stringResource(R.string.mystery_draw_shoe))
            Box(Modifier.height(12.dp))
            S2Headline(stringResource(R.string.mystery_subtitle))
            Box(Modifier.height(12.dp))
            S2Subtitle(stringResource(R.string.mystery_guide_body), Modifier.padding(horizontal = 12.dp))
            Box(Modifier.height(20.dp))
            val screenHeight = androidx.compose.ui.platform.LocalConfiguration.current.screenHeightDp
            val archHeight = s2ArchHeight(screenHeight, androidx.compose.ui.platform.LocalDensity.current.fontScale > 1.2f)
            Box(Modifier.fillMaxWidth().height(archHeight), contentAlignment = Alignment.Center) {
                S2Arch(Modifier.height(archHeight).width(archHeight * (216f / 262f)))
                Box(
                    modifier = Modifier.size(archHeight * 0.8f).graphicsLayer {
                        val p = pulse?.value ?: 0.5f
                        scaleX = 0.96f + p * 0.08f
                        scaleY = scaleX
                        alpha = 0.18f + p * 0.18f
                    }.background(Brush.radialGradient(listOf(Volt, Cyan.copy(alpha = 0.45f), androidx.compose.ui.graphics.Color.Transparent)), CircleShape),
                )
                Image(
                    painter = painterResource(R.drawable.mystery_box_closed),
                    contentDescription = stringResource(R.string.mystery_box_description),
                    contentScale = ContentScale.Fit,
                    modifier = Modifier.size(archHeight),
                )
            }
            if (!shoeDrawReady) {
                Box(Modifier.height(14.dp))
                S2Subtitle(stringResource(R.string.mystery_pending_chain), Modifier.padding(horizontal = 12.dp))
            }
            Box(Modifier.height(8.dp))
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
                TextButton(onClick = onOpenDex, modifier = Modifier.testTag("draw-dex")) {
                    Text(stringResource(R.string.dex_title), color = Silver)
                }
                TextButton(onClick = onOpenWallet, modifier = Modifier.testTag("draw-wallet")) {
                    Text(stringResource(R.string.settings_wallet), color = Silver)
                }
            }
        }
        S2RoundAction(
            icon = androidx.compose.material.icons.Icons.Filled.CardGiftcard,
            label = drawLabel ?: stringResource(R.string.mystery_draw_shoe),
            onClick = onDrawShoe,
            enabled = shoeDrawReady,
            modifier = Modifier.padding(top = 8.dp).testTag("draw-shoe"),
        )
    }
}
