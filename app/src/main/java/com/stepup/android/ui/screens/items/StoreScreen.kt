package com.stepup.android.ui.screens.items

import androidx.annotation.StringRes
import androidx.compose.foundation.background
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.DirectionsRun
import androidx.compose.material.icons.filled.LocalDrink
import androidx.compose.material.icons.filled.Storefront
import androidx.compose.material.icons.filled.Watch
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.stepup.android.R
import com.stepup.android.ui.components.Eyebrow
import com.stepup.android.ui.components.GlowCard
import com.stepup.android.ui.components.SectionHeader
import com.stepup.android.ui.theme.CarbonHigh
import com.stepup.android.ui.theme.Edge
import com.stepup.android.ui.theme.Silver
import com.stepup.android.ui.theme.Slate
import com.stepup.android.ui.theme.Snow
import com.stepup.android.ui.theme.Volt

/**
 * 스텝업 스토어 — 앱이 파는 물건.
 *
 * ── 왜 살 수 없는가 ──
 *
 * 주문과 배송을 받아 줄 곳이 아직 없다. 그래서 살 수 있는 척하지 않는다 —
 * 무엇이 열릴 것인지 보여 주고, 버튼은 끈 채로 "준비 중"이라고 적는다.
 * 눌리는 버튼을 두고 눌렀을 때 아무 일도 없게 하는 것이 제일 나쁘다.
 * 지갑의 GIWA 출금 줄과 같은 태도다.
 *
 * 러너끼리의 거래는 여기가 아니라 NFT 마켓에 있다. 그쪽은 실제로 오간다 —
 * `ui/screens/market/`.
 *
 * 아래 물건 목록은 **무엇을 팔 것인지 보여 주는 견본**이다. 값은 SUP 로
 * 적어 두었지만 아직 결제되지 않는다.
 */
/** 스토어에 올릴 물건 한 종류 */
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
 * 스텝업 스토어 — 실제 물건을 SUP 로.
 *
 * NFT 거래소와 갈라 둔 것은 사는 것이 다르기 때문이다. 저쪽은 다른 러너의
 * 신발이고 여기는 가게의 물건이다. 파는 사람도, 열리는 시점도 다르다.
 */
fun LazyListScope.storeSection() {
    item { SectionHeader(title = stringResource(R.string.store_goods_title)) }
    item {
        GlowCard(accent = true, contentPadding = PaddingValues(16.dp), spacing = 12.dp) {
            Box(
                modifier = Modifier.fillMaxWidth().height(156.dp)
                    .clip(RoundedCornerShape(18.dp)).background(CarbonHigh),
                contentAlignment = Alignment.Center,
            ) {
                Image(
                    painter = painterResource(R.drawable.sneaker_water_01),
                    contentDescription = null,
                    contentScale = ContentScale.Fit,
                    modifier = Modifier.fillMaxSize().padding(12.dp),
                )
                Box(Modifier.align(Alignment.TopEnd).padding(10.dp)) { SoonBadge() }
            }
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
