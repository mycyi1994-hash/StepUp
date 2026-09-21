package com.stepup.android.ui.screens.items

import androidx.annotation.StringRes
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.DirectionsRun
import androidx.compose.material.icons.filled.LocalDrink
import androidx.compose.material.icons.filled.Storefront
import androidx.compose.material.icons.filled.SwapHoriz
import androidx.compose.material.icons.filled.Watch
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.stepup.android.R
import com.stepup.android.ui.components.Eyebrow
import com.stepup.android.ui.components.GhostButton
import com.stepup.android.ui.components.GlowCard
import com.stepup.android.ui.components.SectionHeader
import com.stepup.android.ui.theme.CarbonHigh
import com.stepup.android.ui.theme.Edge
import com.stepup.android.ui.theme.Silver
import com.stepup.android.ui.theme.Slate
import com.stepup.android.ui.theme.Snow
import com.stepup.android.ui.theme.Volt

/**
 * 마켓의 "스토어" 쪽 — 유저 간 NFT 거래소와 스텝업 스토어.
 *
 * ── 왜 살 수 없는가 ──
 *
 * 둘 다 서버가 있어야 성립한다. 거래소는 남의 매물을 봐야 하고, 스토어는
 * 주문과 배송을 받아 줄 곳이 있어야 한다. 지금은 둘 다 없다.
 *
 * 그래서 살 수 있는 척하지 않는다. 무엇이 열릴 것인지 보여 주고, 버튼은
 * 끈 채로 "준비 중"이라고 적는다 — 지갑의 GIWA 출금 줄과 같은 태도다.
 * 눌리는 버튼을 두고 눌렀을 때 아무 일도 없게 하는 것이 제일 나쁘다.
 *
 * 아래 물건 목록은 **무엇을 팔 것인지 보여 주는 견본**이다. 값은 SUP 로
 * 적어 두었지만 아직 결제되지 않는다.
 */
private data class StoreGood(
    val icon: ImageVector,
    @StringRes val name: Int,
    @StringRes val note: Int,
    val priceSup: Int,
)

private val GOODS = listOf(
    StoreGood(Icons.AutoMirrored.Filled.DirectionsRun, R.string.store_good_shoes, R.string.store_good_shoes_note, 12_000),
    StoreGood(Icons.Filled.LocalDrink, R.string.store_good_supplement, R.string.store_good_supplement_note, 3_200),
    StoreGood(Icons.Filled.Watch, R.string.store_good_band, R.string.store_good_band_note, 5_800),
)

/**
 * 스토어 목록을 [LazyListScope] 에 붙인다.
 *
 * 화면을 따로 만들지 않고 마켓의 같은 목록에 얹는 이유는, 상단 탭을 오갈 때
 * 스크롤 위치와 머리글이 하나로 묶여 있어야 탭이 한 화면 안의 전환으로
 * 읽히기 때문이다.
 *
 * @param mySneakerCount 내가 가진 스니커즈 수 — 거래소에 내놓을 수 있는 것
 */
fun LazyListScope.storeSection(mySneakerCount: Int) {
    // ── 유저 간 NFT 거래소 ──
    item { SectionHeader(title = stringResource(R.string.store_trade_title)) }
    item {
        GlowCard(contentPadding = PaddingValues(16.dp), spacing = 12.dp) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(11.dp),
            ) {
                StoreIcon(Icons.Filled.SwapHoriz)
                Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                    Eyebrow(text = stringResource(R.string.store_eyebrow_p2p))
                    Text(
                        text = stringResource(R.string.store_trade_head),
                        style = MaterialTheme.typography.titleSmall,
                        color = Snow,
                    )
                }
                SoonBadge()
            }
            Text(
                text = stringResource(R.string.store_trade_body),
                style = MaterialTheme.typography.bodySmall,
                color = Silver,
                lineHeight = 19.sp,
            )
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(14.dp))
                    .background(CarbonHigh)
                    .padding(horizontal = 14.dp, vertical = 11.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Text(
                    text = stringResource(R.string.store_trade_mine),
                    style = MaterialTheme.typography.bodySmall,
                    color = Silver,
                )
                Text(
                    text = stringResource(R.string.store_trade_mine_count, mySneakerCount),
                    style = MaterialTheme.typography.bodySmall,
                    fontWeight = FontWeight.Bold,
                    color = Volt,
                )
            }
            // 끈 버튼이다. 눌리는 버튼을 두고 아무 일도 없게 하는 것보다,
            // 못 누르는 이유를 적어 두는 편이 낫다.
            GhostButton(
                text = stringResource(R.string.store_trade_list),
                onClick = {},
                enabled = false,
                modifier = Modifier.fillMaxWidth(),
            )
        }
    }

    // ── 스텝업 스토어 ──
    item { SectionHeader(title = stringResource(R.string.store_goods_title)) }
    item {
        GlowCard(contentPadding = PaddingValues(16.dp), spacing = 10.dp) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(11.dp),
            ) {
                StoreIcon(Icons.Filled.Storefront)
                Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                    Eyebrow(text = stringResource(R.string.store_eyebrow_goods))
                    Text(
                        text = stringResource(R.string.store_goods_head),
                        style = MaterialTheme.typography.titleSmall,
                        color = Snow,
                    )
                }
                SoonBadge()
            }
            Text(
                text = stringResource(R.string.store_goods_body),
                style = MaterialTheme.typography.bodySmall,
                color = Silver,
                lineHeight = 19.sp,
            )
        }
    }
    items(GOODS.size) { index -> GoodRow(GOODS[index]) }
    item {
        Text(
            text = stringResource(R.string.store_note),
            modifier = Modifier.padding(horizontal = 4.dp),
            fontSize = 10.sp,
            color = Slate,
            lineHeight = 15.sp,
        )
    }
}

@Composable
private fun StoreIcon(icon: ImageVector) {
    Box(
        modifier = Modifier
            .size(38.dp)
            .clip(RoundedCornerShape(13.dp))
            .background(CarbonHigh),
        contentAlignment = Alignment.Center,
    ) {
        Icon(imageVector = icon, contentDescription = null, tint = Volt, modifier = Modifier.size(19.dp))
    }
}

@Composable
private fun SoonBadge() {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(50))
            .background(Volt.copy(alpha = 0.14f))
            .padding(horizontal = 9.dp, vertical = 4.dp),
    ) {
        Text(
            text = stringResource(R.string.store_soon),
            fontSize = 9.sp,
            fontWeight = FontWeight.Black,
            color = Volt,
        )
    }
}

/** 팔 물건 한 줄 — 값은 적혀 있지만 아직 결제되지 않는다 */
@Composable
private fun GoodRow(good: StoreGood) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(18.dp))
            .background(CarbonHigh)
            .padding(horizontal = 14.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Box(
            modifier = Modifier
                .size(42.dp)
                .clip(RoundedCornerShape(14.dp))
                .background(Volt.copy(alpha = 0.10f)),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = good.icon,
                contentDescription = null,
                tint = Volt,
                modifier = Modifier.size(20.dp),
            )
        }
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(
                text = stringResource(good.name),
                style = MaterialTheme.typography.titleSmall,
                color = Snow,
            )
            Text(text = stringResource(good.note), fontSize = 11.sp, color = Silver)
        }
        Column(horizontalAlignment = Alignment.End, verticalArrangement = Arrangement.spacedBy(3.dp)) {
            Text(
                text = stringResource(R.string.price_sup, "%,d".format(good.priceSup)),
                style = MaterialTheme.typography.bodySmall,
                fontWeight = FontWeight.Bold,
                color = Volt,
            )
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(50))
                    .background(Edge.copy(alpha = 0.6f))
                    .padding(horizontal = 9.dp, vertical = 3.dp),
            ) {
                Text(
                    text = stringResource(R.string.store_soon),
                    fontSize = 9.sp,
                    fontWeight = FontWeight.Bold,
                    color = Slate,
                )
            }
        }
    }
}
