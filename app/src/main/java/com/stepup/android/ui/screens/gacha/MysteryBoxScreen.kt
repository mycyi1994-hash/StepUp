package com.stepup.android.ui.screens.gacha

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.stepup.android.R
import com.stepup.android.ui.components.GhostButton
import com.stepup.android.ui.components.VoltButton
import com.stepup.android.ui.components.ambientPhase
import com.stepup.android.ui.experience.LocalMotion
import com.stepup.android.ui.experience.LocalFeedback
import com.stepup.android.ui.experience.FeedbackCue
import com.stepup.android.ui.theme.Cyan
import com.stepup.android.ui.theme.Silver
import com.stepup.android.ui.theme.Snow
import com.stepup.android.ui.theme.StepUpDesign
import com.stepup.android.ui.theme.Volt

/** Art, state and tap targets stay separate so a deployed draw contract can enable the actions. */
@Composable
fun MysteryBoxScreen(
    shoeDrawReady: Boolean = false,
    outfitDrawReady: Boolean = false,
    onDrawShoe: () -> Unit = {},
    onDrawOutfit: () -> Unit = {},
) {
    val feedback = LocalFeedback.current
    LaunchedEffect(feedback) { feedback?.play(FeedbackCue.DrawEnter) }
    val pulse = if (LocalMotion.current.decorative) ambientPhase(3400, reverse = true) else null
    Column(
        modifier = Modifier.fillMaxSize()
            .padding(horizontal = StepUpDesign.Gutter)
            .padding(top = 24.dp, bottom = 22.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            text = stringResource(R.string.mystery_title),
            color = Snow,
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center,
        )
        Text(
            text = stringResource(R.string.mystery_subtitle),
            color = Silver,
            style = MaterialTheme.typography.bodyMedium,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(top = 8.dp),
        )
        Box(
            modifier = Modifier.fillMaxWidth().weight(1f).heightIn(min = 180.dp),
            contentAlignment = Alignment.Center,
        ) {
            Box(
                modifier = Modifier.size(245.dp).graphicsLayer {
                    val p = pulse?.value ?: 0.5f
                    scaleX = 0.96f + p * 0.08f
                    scaleY = scaleX
                    alpha = 0.28f + p * 0.23f
                }.background(Brush.radialGradient(listOf(Volt, Cyan.copy(alpha = 0.55f), androidx.compose.ui.graphics.Color.Transparent)), CircleShape),
            )
            Image(
                painter = painterResource(R.drawable.mystery_box_closed),
                contentDescription = stringResource(R.string.mystery_box_description),
                contentScale = ContentScale.Fit,
                modifier = Modifier.fillMaxWidth().size(320.dp),
            )
        }
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            VoltButton(
                text = stringResource(R.string.mystery_draw_shoe),
                onClick = onDrawShoe,
                enabled = shoeDrawReady,
                modifier = Modifier.fillMaxWidth(),
            )
            GhostButton(
                text = stringResource(R.string.mystery_draw_outfit),
                onClick = onDrawOutfit,
                enabled = outfitDrawReady,
                modifier = Modifier.fillMaxWidth(),
            )
        }
        if (!shoeDrawReady || !outfitDrawReady) {
            Text(
                text = stringResource(R.string.mystery_pending_chain),
                color = Silver,
                style = MaterialTheme.typography.bodySmall,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(top = 16.dp),
            )
        }
    }
}
