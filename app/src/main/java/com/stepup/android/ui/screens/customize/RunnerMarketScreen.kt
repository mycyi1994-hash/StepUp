package com.stepup.android.ui.screens.customize

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.stepup.android.R
import com.stepup.android.domain.AvatarArtCatalog
import com.stepup.android.domain.AvatarPose
import com.stepup.android.domain.Faction
import com.stepup.android.domain.Outfit
import com.stepup.android.domain.Outfits
import com.stepup.android.domain.Rarity
import com.stepup.android.ui.components.BadgeTone
import com.stepup.android.ui.components.AvatarImage
import com.stepup.android.ui.components.PageHero
import com.stepup.android.ui.components.SecondaryHeader
import com.stepup.android.ui.components.OutfitArt
import com.stepup.android.ui.components.GhostButton
import com.stepup.android.ui.components.outfitNameRes
import com.stepup.android.ui.components.GlowCard
import com.stepup.android.ui.components.HexEmblem
import com.stepup.android.ui.components.RenewalCardPadding
import com.stepup.android.ui.components.SmallBadge
import com.stepup.android.ui.components.SneakerFrame
import com.stepup.android.ui.components.SubHeader
import com.stepup.android.ui.components.TwoWaySwitch
import com.stepup.android.ui.experience.feedbackClickable
import com.stepup.android.ui.screens.market.MarketProblemNote
import com.stepup.android.ui.screens.market.MarketViewModel
import com.stepup.android.ui.screens.market.modelName
import com.stepup.android.ui.screens.market.previewSneaker
import com.stepup.android.ui.theme.CarbonHigh
import com.stepup.android.ui.theme.Edge
import com.stepup.android.ui.theme.Silver
import com.stepup.android.ui.theme.Slate
import com.stepup.android.ui.theme.Snow
import com.stepup.android.ui.theme.StepUpNumbers
import com.stepup.android.ui.theme.VoltText

/**
 * 러너 마켓 — 꾸미기 안에서 의상과 신발을 찾는 곳.
 *
 * 카드 두 줄, 필터 둘(의상 / 신발)이 전부다. 호가창 · 차트 · 수익률 같은
 * 거래소의 것은 이 화면에 두지 않는다. 신발 카드를 누르면 그 모델의 상세로
 * 가고, 사는 것은 거기서 **가격과 조건을 확인한 뒤** 한다(기존 구매 흐름).
 *
 * ── 의상 ──
 *
 * 의상은 아직 살 수 없다. 사고파는 흐름이 없기 때문이다. 그래서 가격을
 * 적지 않고 "출시 예정"으로 둔다. 데모 모드에서는 입혀 볼 수만 있다.
 *
 * ── 신발 ──
 *
 * 지금 거래소에 올라와 있는 모델만 보여 준다. 값은 그 모델의 가장 싼
 * 매물이다. 올라온 것이 없으면 없다고 적는다 — 52종을 전부 깔고 "매물
 * 없음"을 52번 적지 않는다.
 */
@Composable
fun RunnerMarketScreen(
    onBack: () -> Unit = {},
    onOpenWallet: () -> Unit = {},
    onOpenModel: (faction: String, rarity: String, variant: Int) -> Unit = { _, _, _ -> },
    onOpenVault: () -> Unit = {},
    viewModel: CustomizeViewModel = viewModel(factory = CustomizeViewModel.Factory),
    marketViewModel: MarketViewModel = viewModel(factory = MarketViewModel.Factory),
) {
    val context = LocalContext.current
    val look by viewModel.look.collectAsStateWithLifecycle()
    val balance by viewModel.balance.collectAsStateWithLifecycle()
    val demo by viewModel.demoMode.collectAsStateWithLifecycle()
    val message by viewModel.message.collectAsStateWithLifecycle()
    val board by marketViewModel.board.collectAsStateWithLifecycle()

    var filter by rememberSaveable { mutableIntStateOf(0) }

    LaunchedEffect(message) {
        val m = message ?: return@LaunchedEffect
        Toast.makeText(context, context.getString(m), Toast.LENGTH_SHORT).show()
        viewModel.consumeMessage()
    }

    val cols = if (LocalDensity.current.fontScale > 1.3f) 1 else 2

    LazyVerticalGrid(
        columns = GridCells.Fixed(cols),
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(start = 18.dp, end = 18.dp, top = 8.dp, bottom = 18.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        item(span = { GridItemSpan(cols) }) {
            SecondaryHeader(onBack = onBack, balance = balance, onOpenWallet = onOpenWallet)
        }
        // 큰 제목 · 한 줄 소개 · 내 러너(내 성별의 그림 그대로 — 아이템을 입은 척하지 않는다)
        item(span = { GridItemSpan(cols) }) {
            PageHero(
                title = stringResource(R.string.market_runner_title),
                subtitle = stringResource(R.string.market_runner_sub),
            ) {
                AvatarImage(
                    art = AvatarArtCatalog.resolve(look, AvatarPose.RUN).art,
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .fillMaxSize(),
                )
            }
        }
        item(span = { GridItemSpan(cols) }) {
            TwoWaySwitch(
                labels = listOf(
                    stringResource(R.string.feed_filter_all),
                    stringResource(R.string.customize_tab_outfit),
                    stringResource(R.string.customize_tab_shoes),
                ),
                selected = filter,
                onSelect = { filter = it },
            )
        }
        if (demo) {
            item(span = { GridItemSpan(cols) }) { DemoNote() }
        }

        // 0 = 전체, 1 = 의상, 2 = 신발
        if (filter != 2) {
            // ── 의상 — NFT 의상만. 기본 의상은 이미 갖고 있다 ──
            val nft = Outfits.ALL.filter { it.nft }
            items(nft, key = { it.id }) { outfit ->
                OutfitProduct(
                    outfit = outfit,
                    demo = demo,
                    look = look,
                    onTry = {
                        viewModel.equipOutfit(outfit)
                    },
                    onInfo = { viewModel.say(R.string.market_outfit_soon_toast) },
                )
            }
        }
        if (filter != 1) {
            // ── 신발 ──
            when {
                demo -> items(DEMO_SHOES, key = { "demo-${it.faction}-${it.rarity}-${it.variant}" }) { d ->
                    ShoeProduct(
                        faction = d.faction.id,
                        rarity = d.rarity.id,
                        variant = d.variant,
                        price = d.price,
                        demo = true,
                        onClick = { viewModel.say(R.string.market_demo_toast) },
                    )
                }
                board.loading && board.quotes.isEmpty() -> item(span = { GridItemSpan(cols) }) {
                    StateCard(stringResource(R.string.feed_loading), null)
                }
                board.problem != null -> item(span = { GridItemSpan(cols) }) {
                    MarketProblemNote(board.problem!!)
                }
                else -> {
                    val listed = board.quotes.filter { it.ask != null }.sortedBy { it.ask }
                    if (listed.isEmpty()) {
                        item(span = { GridItemSpan(cols) }) {
                            StateCard(
                                stringResource(R.string.market_runner_no_shoes),
                                stringResource(R.string.market_runner_no_shoes_hint),
                            ) {
                                GhostButton(
                                    text = stringResource(R.string.customize_open_vault),
                                    onClick = onOpenVault,
                                    modifier = Modifier.fillMaxWidth(),
                                )
                            }
                        }
                    } else {
                        items(listed, key = { "${it.faction}-${it.rarity}-${it.variant}" }) { q ->
                            ShoeProduct(
                                faction = q.faction,
                                rarity = q.rarity,
                                variant = q.variant,
                                price = q.ask,
                                demo = false,
                                onClick = { onOpenModel(q.faction, q.rarity, q.variant) },
                            )
                        }
                    }
                }
            }
        }
    }
}

/** 데모 신발 — 테스트용. 살 수 없고, 누르면 그렇다고 알린다. */
private data class DemoShoe(val faction: Faction, val rarity: Rarity, val variant: Int, val price: Double)

private val DEMO_SHOES = listOf(
    DemoShoe(Faction.WATER, Rarity.RARE, 0, 600.0),
    DemoShoe(Faction.LIGHTNING, Rarity.EPIC, 1, 800.0),
    DemoShoe(Faction.WIND, Rarity.COMMON, 2, 250.0),
    DemoShoe(Faction.FIRE, Rarity.LEGENDARY, 0, 2400.0),
)

@Composable
private fun DemoNote() {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(VoltText.copy(alpha = 0.10f))
            .padding(horizontal = 14.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        SmallBadge(stringResource(R.string.demo_badge), tone = BadgeTone.Glow)
        Text(
            text = stringResource(R.string.market_demo_note),
            fontSize = 13.sp,
            color = Silver,
            lineHeight = 18.sp,
        )
    }
}

@Composable
private fun StateCard(title: String, hint: String?, action: (@Composable () -> Unit)? = null) {
    GlowCard(contentPadding = RenewalCardPadding, spacing = 8.dp) {
        Text(text = title, fontSize = 15.sp, fontWeight = FontWeight.Bold, color = Snow)
        if (hint != null) Text(text = hint, fontSize = 13.sp, color = Silver, lineHeight = 18.sp)
        action?.invoke()
    }
}

/** 상품 카드 틀 — 그림 · 이름 · 값 · 표시 */
@Composable
private fun ProductCard(
    onClick: () -> Unit,
    badge: @Composable () -> Unit,
    art: @Composable () -> Unit,
    name: String,
    price: @Composable () -> Unit,
) {
    val shape = RoundedCornerShape(18.dp)
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(shape)
            .background(CarbonHigh, shape)
            .border(1.dp, Edge, shape)
            .feedbackClickable(onClick = onClick)
            .padding(12.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Box(Modifier.fillMaxWidth()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(110.dp),
                contentAlignment = Alignment.Center,
            ) { art() }
            Box(Modifier.align(Alignment.TopEnd)) { badge() }
        }
        Text(
            text = name,
            fontSize = 15.sp,
            fontWeight = FontWeight.Bold,
            color = Snow,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.heightIn(min = 20.dp),
        )
        price()
    }
}

@Composable
private fun ShoeProduct(
    faction: String,
    rarity: String,
    variant: Int,
    price: Double?,
    demo: Boolean,
    onClick: () -> Unit,
) {
    ProductCard(
        onClick = onClick,
        badge = {
            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                if (demo) SmallBadge(stringResource(R.string.demo_badge), tone = BadgeTone.Glow)
                SmallBadge("NFT", tone = BadgeTone.Nft)
            }
        },
        art = {
            SneakerFrame(
                sneaker = previewSneaker(faction, rarity, variant),
                modifier = Modifier.fillMaxSize(),
            )
        },
        name = modelName(faction, rarity, variant),
        price = {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                HexEmblem(size = 18.dp, glow = false)
                Text(
                    text = price?.let { "%,.0f".format(it) } ?: "—",
                    fontFamily = StepUpNumbers,
                    fontSize = 17.sp,
                    fontWeight = FontWeight.Bold,
                    color = Snow,
                )
                Text(text = "SUP", fontSize = 12.sp, color = Silver)
            }
        },
    )
}

@Composable
private fun OutfitProduct(
    outfit: Outfit,
    demo: Boolean,
    look: com.stepup.android.domain.AvatarLook,
    onTry: () -> Unit,
    onInfo: () -> Unit,
) {
    ProductCard(
        onClick = if (demo) onTry else onInfo,
        badge = { SmallBadge("NFT", tone = BadgeTone.Nft) },
        art = { OutfitArt(outfit, look.gender, Modifier.fillMaxSize().padding(4.dp)) },
        name = stringResource(outfitNameRes(outfit)),
        price = {
            // 팔지 않는 물건에 값을 적지 않는다
            Text(
                text = stringResource(if (demo) R.string.market_outfit_try else R.string.market_outfit_soon),
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                color = if (demo) VoltText else Slate,
            )
        },
    )
}
