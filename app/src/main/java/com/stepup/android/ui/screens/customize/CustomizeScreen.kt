package com.stepup.android.ui.screens.customize

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
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
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.automirrored.filled.DirectionsRun
import androidx.compose.material.icons.filled.Checkroom
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.MoreHoriz
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
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
import com.stepup.android.ui.components.MainHeader
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
import com.stepup.android.ui.components.SupPill
import com.stepup.android.ui.components.TwoWaySwitch
import com.stepup.android.ui.components.Wordmark
import com.stepup.android.ui.components.variantLabel
import com.stepup.android.ui.experience.feedbackClickable
import com.stepup.android.ui.guide.GuideTour
import com.stepup.android.ui.guide.guideTarget
import com.stepup.android.ui.theme.CarbonHigh
import com.stepup.android.ui.theme.Edge
import com.stepup.android.ui.theme.OnVolt
import com.stepup.android.ui.theme.Silver
import com.stepup.android.ui.theme.Slate
import com.stepup.android.ui.theme.Snow
import com.stepup.android.ui.theme.Volt

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
    onOpenWallet: () -> Unit = {},
    onOpenMarket: () -> Unit = {},
    onOpenVault: () -> Unit = {},
    onOpenSneaker: (Long) -> Unit = {},
    viewModel: CustomizeViewModel = viewModel(factory = CustomizeViewModel.Factory),
) {
    val context = LocalContext.current
    val look by viewModel.look.collectAsStateWithLifecycle()
    val balance by viewModel.balance.collectAsStateWithLifecycle()
    val shoes by viewModel.shoes.collectAsStateWithLifecycle()
    val demo by viewModel.demoMode.collectAsStateWithLifecycle()
    val message by viewModel.message.collectAsStateWithLifecycle()

    // 0 = 의상, 1 = 신발
    var tab by rememberSaveable { mutableIntStateOf(0) }
    // 미리 입혀 본 것. 비어 있으면 지금 입은 것을 보여 준다.
    var pickedOutfitId by rememberSaveable { mutableStateOf<String?>(null) }
    var pickedShoeId by rememberSaveable { mutableStateOf<Long?>(null) }

    LaunchedEffect(message) {
        val m = message ?: return@LaunchedEffect
        Toast.makeText(context, context.getString(m), Toast.LENGTH_SHORT).show()
        viewModel.consumeMessage()
    }

    // 고를 수 있는 의상 — 가진 것, 그리고 데모 모드에서만 NFT 의상 체험
    val outfits = remember(demo) {
        viewModel.ownedOutfits() + if (demo) Outfits.ALL.filter { !viewModel.isOwned(it) } else emptyList()
    }
    val pickedOutfit = outfits.firstOrNull { it.id == pickedOutfitId } ?: look.outfit
    val pickedShoe = shoes?.firstOrNull { it.id == pickedShoeId } ?: look.shoe
    val preview = look.copy(
        outfit = pickedOutfit,
        shoe = pickedShoe,
        trial = !viewModel.isOwned(pickedOutfit),
    )
    var showOptions by rememberSaveable { mutableStateOf(false) }
    val render = AvatarArtCatalog.resolve(preview, AvatarPose.IDLE)
    val wearing = if (tab == 0) look.outfit.id == pickedOutfit.id else pickedShoe?.equipped == true
    val canEquip = if (tab == 0) true else pickedShoe != null

    androidx.compose.foundation.layout.BoxWithConstraints(Modifier.fillMaxSize()) {
        val previewHeight = maxHeight * 0.48f
        Column(
            Modifier.fillMaxSize()
                .padding(horizontal = com.stepup.android.ui.theme.StepUpDesign.Gutter)
                .padding(bottom = 12.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Box(Modifier.fillMaxWidth().height(previewHeight)) {
                AvatarImage(
                    art = render.art,
                    contentDescription = stringResource(R.string.cd_customize_preview),
                    modifier = Modifier.fillMaxSize().padding(top = 12.dp, bottom = 6.dp)
                        .guideTarget(GuideTour.Targets.CUSTOMIZE_PREVIEW),
                )
                com.stepup.android.ui.components.DarkIconButton(
                    Icons.Filled.MoreHoriz, stringResource(R.string.common_more),
                    onClick = { showOptions = true },
                    modifier = Modifier.align(Alignment.TopEnd).testTag("wardrobe-options"),
                )
                if (preview.trial) {
                    SmallBadge(
                        stringResource(R.string.customize_trial_badge), tone = BadgeTone.Glow,
                        modifier = Modifier.align(Alignment.TopStart).padding(top = 12.dp),
                    )
                }
            }
            com.stepup.android.ui.components.AvatarLookNote(preview, render)
            TwoWaySwitch(
                labels = listOf(stringResource(R.string.customize_tab_outfit), stringResource(R.string.customize_tab_shoes)),
                icons = listOf(StepUpIcons.Shirt, Icons.AutoMirrored.Filled.DirectionsRun),
                selected = tab, onSelect = { tab = it },
            )
            Column(
                Modifier.fillMaxWidth().weight(1f).verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
        if (tab == 0) {
            ItemGrid(outfits) { outfit ->
                OutfitCard(
                    outfit = outfit,
                    look = look,
                    owned = viewModel.isOwned(outfit),
                    worn = look.outfit.id == outfit.id,
                    picked = pickedOutfit.id == outfit.id,
                    onClick = { pickedOutfitId = outfit.id },
                )
            }
        } else {
            val list = shoes
            when {
                list == null -> Box(Modifier.fillMaxWidth().height(80.dp), contentAlignment = Alignment.Center) {
                    androidx.compose.material3.CircularProgressIndicator(color = Volt)
                }
                list.isEmpty() -> GlowCard(contentPadding = RenewalCardPadding, spacing = 6.dp) {
                    Text(
                        text = stringResource(R.string.customize_no_shoes),
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        color = Snow,
                    )
                    Text(
                        text = stringResource(R.string.customize_no_shoes_hint),
                        fontSize = 13.sp,
                        color = Silver,
                        lineHeight = 18.sp,
                    )
                }
                else -> ItemGrid(list) { shoe ->
                    ShoeCard(
                        shoe = shoe,
                        picked = pickedShoe?.id == shoe.id,
                        onClick = { pickedShoeId = shoe.id },
                    )
                }
            }
        }


            }
            if (!wearing && canEquip) {
                PrimaryCta(
                    text = stringResource(R.string.customize_equip),
                    icon = Icons.Filled.Checkroom, showArrow = false,
                    onClick = {
                        if (tab == 0) viewModel.equipOutfit(pickedOutfit)
                        else pickedShoe?.let { viewModel.equipShoe(it.id) }
                    },
                    modifier = Modifier.testTag("wardrobe-equip"),
                )
            }
        }
    }
    if (showOptions) {
        androidx.compose.material3.ModalBottomSheet(
            onDismissRequest = { showOptions = false },
            containerColor = com.stepup.android.ui.theme.Night,
        ) {
            Column(
                Modifier.fillMaxWidth().verticalScroll(rememberScrollState())
                    .padding(horizontal = com.stepup.android.ui.theme.StepUpDesign.Gutter)
                    .padding(bottom = 24.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp),
            ) {
                Text(stringResource(R.string.customize_title), color = Snow,
                    style = androidx.compose.material3.MaterialTheme.typography.titleLarge)
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    AvatarGender.entries.forEach { gender ->
                        GenderCard(
                            gender, look.gender == gender, onClick = { viewModel.setGender(gender) },
                            modifier = Modifier.weight(1f).height(144.dp),
                        )
                    }
                }
                GhostButton(
                    text = stringResource(R.string.customize_open_market),
                    onClick = { showOptions = false; onOpenMarket() },
                )
                GhostButton(
                    text = stringResource(R.string.customize_open_vault),
                    onClick = { showOptions = false; onOpenVault() },
                )
                pickedShoe?.let { shoe ->
                    GhostButton(
                        text = stringResource(R.string.customize_shoe_detail),
                        onClick = { showOptions = false; onOpenSneaker(shoe.id) },
                    )
                }
            }
        }
    }
}

@Composable
private fun <T> ItemGrid(items: List<T>, cell: @Composable (T) -> Unit) {
    val cols = if (LocalDensity.current.fontScale > 1.2f) 2 else 3
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        items.chunked(cols).forEach { row ->
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
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
                .weight(1f)
                .fillMaxWidth(),
        )
        Text(
            text = label,
            fontSize = 13.sp,
            fontWeight = FontWeight.Bold,
            color = if (selected) Snow else Silver,
            maxLines = 1,
        )
    }
}

/** 격자 한 칸의 틀 — 그림 · 이름 · 표시 · 선택 테두리 · 착용 체크 */
@Composable
private fun ItemCell(
    picked: Boolean,
    worn: Boolean,
    name: String,
    onClick: () -> Unit,
    badges: @Composable () -> Unit,
    art: @Composable () -> Unit,
) {
    val shape = RoundedCornerShape(16.dp)
    Box {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(shape)
                .background(if (picked) Volt.copy(alpha = 0.12f) else CarbonHigh, shape)
                .border(if (picked) 2.dp else 1.dp, if (picked) Volt else Edge, shape)
                .feedbackClickable(role = Role.RadioButton, onClick = onClick)
                .semantics { selected = picked }
                .padding(8.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(78.dp),
                contentAlignment = Alignment.Center,
            ) { art() }
            Text(
                text = name,
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                color = Snow,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                lineHeight = 15.sp,
            )
            badges()
        }
        if (worn) {
            Box(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(6.dp)
                    .size(22.dp)
                    .background(Volt, CircleShape),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    Icons.Filled.Check,
                    contentDescription = stringResource(R.string.customize_wearing),
                    tint = OnVolt,
                    modifier = Modifier.size(14.dp),
                )
            }
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
        badges = {
            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                if (outfit.nft) SmallBadge("NFT", tone = BadgeTone.Nft)
                if (!owned) SmallBadge(stringResource(R.string.customize_trial_badge), tone = BadgeTone.Glow)
                if (outfit.starter) SmallBadge(stringResource(R.string.customize_basic_badge), tone = BadgeTone.Muted)
            }
        },
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
        badges = {
            // 레벨 · 능력치는 신발 상세에서. 꾸미기 칸에는 NFT 표시만 둔다.
            SmallBadge("NFT", tone = BadgeTone.Nft)
        },
        art = { SneakerFrame(sneaker = shoe, modifier = Modifier.fillMaxSize()) },
    )
}
