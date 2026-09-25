package com.stepup.android.ui.screens.customize

import android.widget.Toast
import androidx.compose.material3.MaterialTheme
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.automirrored.filled.DirectionsRun
import androidx.compose.material.icons.filled.Checkroom
import androidx.compose.ui.platform.testTag
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.stepup.android.R
import com.stepup.android.domain.AvatarArtCatalog
import com.stepup.android.domain.AvatarGender
import com.stepup.android.domain.AvatarLook
import com.stepup.android.domain.AvatarPose
import com.stepup.android.domain.Outfit
import com.stepup.android.domain.Outfits
import com.stepup.android.domain.Sneaker
import com.stepup.android.ui.components.AvatarImage
import com.stepup.android.ui.components.CharacterStage
import com.stepup.android.ui.components.StepUpIcons
import com.stepup.android.ui.components.OutfitArt
import com.stepup.android.ui.components.outfitNameRes
import com.stepup.android.ui.components.BadgeTone
import com.stepup.android.ui.components.GhostButton
import com.stepup.android.ui.components.GlowCard
import com.stepup.android.ui.components.PrimaryCta
import com.stepup.android.ui.components.RenewalCardPadding
import com.stepup.android.ui.components.SmallBadge
import com.stepup.android.ui.components.SneakerFrame
import com.stepup.android.ui.components.TwoWaySwitch
import com.stepup.android.ui.components.variantLabel
import com.stepup.android.ui.experience.feedbackClickable
import com.stepup.android.ui.guide.GuideTour
import com.stepup.android.ui.guide.guideTarget
import com.stepup.android.ui.theme.CarbonHigh
import com.stepup.android.ui.theme.Edge
import com.stepup.android.ui.theme.OnVolt
import com.stepup.android.ui.theme.Silver
import com.stepup.android.ui.theme.Snow
import com.stepup.android.ui.theme.Volt
import com.stepup.android.ui.theme.Night
import com.stepup.android.ui.theme.StepUpDesign
import com.stepup.android.ui.components.SceneToolbar

/**
 * 꾸미기 — 내 러너에 의상과 신발을 입히는 곳.
 *
 * 성별은 누르면 바로 바뀐다. 의상과 신발은 먼저 **미리 입혀 보고**, 마음에
 * 들면 "장착하기"로 확정한다. 확정한 모습이 러닝 홈 · 러닝 완료 · 내 정보에
 * 그대로 보인다.
 *
 * 캐릭터는 NFT 가 아니다. 남녀 두 명은 모두 처음부터 갖고 있고 얼굴과
 * 체형은 바꾸지 않는다. NFT 인 것은 입히는 것(의상 · 신발)뿐이다.
 */
@Composable
@OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)
fun CustomizeScreen(
    onBack: () -> Unit = {},
    onOpenDex: () -> Unit = {},
    onOpenMarketModel: (String, String, Int) -> Unit = { _, _, _ -> },
    onChangeBackground: () -> Unit = {},
    onOpenWallet: () -> Unit = {},
    onOpenMarket: () -> Unit = {},
    onOpenVault: () -> Unit = {},
    onOpenSneaker: (Long) -> Unit = {},
    viewModel: CustomizeViewModel = viewModel(factory = CustomizeViewModel.Factory),
) {
    com.stepup.android.ui.screens.items.ItemsScreen(
        onBack = null, onOpenSneaker = onOpenSneaker, onOpenDex = onOpenDex,
        onOpenMarketModel = onOpenMarketModel, showHeader = false,
    )
}

@Composable
private fun <T> ItemGrid(items: List<T>, cell: @Composable (T) -> Unit) {
    val cols = if (LocalDensity.current.fontScale > 1.2f) 2 else 3
    Column(verticalArrangement = Arrangement.spacedBy(StepUpDesign.WardrobeGridGap)) {
        items.chunked(cols).forEach { row ->
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(StepUpDesign.WardrobeGridGap)) {
                row.forEach { Box(Modifier.weight(1f)) { cell(it) } }
                repeat(cols - row.size) { Spacer(Modifier.weight(1f)) }
            }
        }
    }
}

@Composable
private fun GenderCard(
    gender: AvatarGender,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val shape = RoundedCornerShape(16.dp)
    val label = stringResource(if (gender == AvatarGender.MALE) R.string.gender_male else R.string.gender_female)
    // 성별마다 제 그림을 쓴다 — 여자 카드에 남자 그림을 넣지 않는다
    val art = AvatarArtCatalog.resolve(AvatarLook(gender = gender), AvatarPose.IDLE).art
    Column(
        modifier = modifier
            .clip(shape)
            .background(if (selected) Volt.copy(alpha = 0.16f) else CarbonHigh, shape)
            .border(if (selected) 2.dp else 1.dp, if (selected) Volt else Edge, shape)
            .feedbackClickable(role = Role.RadioButton, onClick = onClick)
            .semantics {
                this.selected = selected
                contentDescription = label
            }
            .padding(start = 4.dp, end = 4.dp, top = 6.dp, bottom = 6.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(2.dp),
    ) {
        AvatarImage(
            art = art,
            modifier = Modifier
                .height(128.dp)
                .fillMaxWidth(),
        )
        Text(
            text = label,
            fontSize = 14.sp,
            fontWeight = FontWeight.Bold,
            color = if (selected) Snow else Silver,

        )
    }
}

/** Image-first inventory. Names remain available to accessibility services. */
@Composable
private fun ItemCell(
    picked: Boolean,
    worn: Boolean,
    name: String,
    onClick: () -> Unit,
    trial: Boolean = false,
    art: @Composable () -> Unit,
) {
    val shape = RoundedCornerShape(StepUpDesign.WardrobeCellRadius)
    val wearingLabel = stringResource(R.string.customize_wearing)
    val trialLabel = stringResource(R.string.customize_trial_badge)
    Column(
        modifier = Modifier.fillMaxWidth().clip(shape)
            .background(if (picked) CarbonHigh else Night, shape)
            .border(if (picked) 2.dp else 1.dp, if (picked) Volt else Edge.copy(alpha = 0.7f), shape)
            .feedbackClickable(role = Role.RadioButton, onClick = onClick)
            .semantics(mergeDescendants = true) {
                selected = picked
                contentDescription = listOfNotNull(name, wearingLabel.takeIf { worn }, trialLabel.takeIf { trial }).joinToString(", ")
            },
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Box(Modifier.fillMaxWidth().aspectRatio(StepUpDesign.WardrobeCellAspect), contentAlignment = Alignment.Center) {
            Box(Modifier.fillMaxSize().padding(8.dp), contentAlignment = Alignment.Center) { art() }
            if (worn) Box(
                Modifier.align(Alignment.TopEnd).padding(6.dp).size(24.dp).background(Volt, CircleShape),
                contentAlignment = Alignment.Center,
            ) { Icon(Icons.Filled.Check, null, tint = OnVolt, modifier = Modifier.size(16.dp)) }
        }
        if (worn || trial) {
            Text(
                if (trial) trialLabel else wearingLabel,
                style = MaterialTheme.typography.bodyMedium, color = Snow,
                textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                modifier = Modifier.fillMaxWidth().background(CarbonHigh).padding(horizontal = 6.dp, vertical = 8.dp),
            )
        }
    }
}

@Composable
private fun OutfitCard(
    outfit: Outfit,
    look: AvatarLook,
    owned: Boolean,
    worn: Boolean,
    picked: Boolean,
    onClick: () -> Unit,
) {
    ItemCell(
        picked = picked,
        worn = worn,
        name = stringResource(outfitNameRes(outfit)),
        onClick = onClick,
        trial = !owned,
        art = { OutfitArt(outfit, look.gender, Modifier.fillMaxSize()) },
    )
}

@Composable
private fun ShoeCard(
    shoe: Sneaker,
    picked: Boolean,
    onClick: () -> Unit,
) {
    ItemCell(
        picked = picked,
        worn = shoe.equipped,
        name = shoe.variantLabel(),
        onClick = onClick,
        art = { SneakerFrame(sneaker = shoe, modifier = Modifier.fillMaxSize()) },
    )
}
