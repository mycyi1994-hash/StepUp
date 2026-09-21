package com.stepup.android.ui.screens.events

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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.DirectionsRun
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Groups
import androidx.compose.material.icons.filled.LocalOffer
import androidx.compose.material.icons.filled.SelfImprovement
import androidx.compose.material.icons.filled.Storefront
import androidx.compose.material.icons.filled.WbTwilight
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
import com.stepup.android.ui.components.GlowCard
import com.stepup.android.ui.theme.CarbonHigh
import com.stepup.android.ui.theme.Silver
import com.stepup.android.ui.theme.Slate
import com.stepup.android.ui.theme.Snow
import com.stepup.android.ui.theme.Volt

/**
 * 이벤트 탭의 소식 — 특가 공지와 건강 뉴스.
 *
 * ── 어디까지가 사실인가 ──
 *
 * 여기 글은 전부 **StepUp 이 쓴 글**이다. 바깥 기사나 남의 연구를 옮겨 온
 * 것처럼 보이지 않게, 출처·기자·날짜를 지어내지 않는다. 한 줄짜리 눈썹
 * 글귀가 "StepUp 가이드"인 것이 그 표시다.
 *
 * 특가도 마찬가지다. 아직 마켓이 열리지 않아 실제로 살 수 있는 물건이 없고,
 * 살 수 없는 할인을 띄우는 것은 거짓말이다. 그래서 지금은 "무엇이 언제
 * 열리는지"를 알리는 공지만 둔다 — 지갑의 GIWA 줄과 같은 태도다.
 */
data class FeedItem(
    val icon: ImageVector,
    @StringRes val eyebrow: Int,
    @StringRes val title: Int,
    @StringRes val body: Int,
    /** 아직 열리지 않은 것 — 뱃지를 붙여 기다리는 중임을 밝힌다 */
    val soon: Boolean = false,
)

/**
 * 러닝 이벤트 소식 — "무슨 일이 열린다"를 알리는 쪽.
 *
 * 받을 보상과 진행률이 있는 **이벤트 탭**과는 다르다. 저쪽은 누르면 받는
 * 자리이고, 여기는 읽는 자리다. 그래서 여기에는 버튼을 두지 않는다.
 */
val RUN_EVENT_FEED = listOf(
    FeedItem(
        icon = Icons.Filled.DirectionsRun,
        eyebrow = R.string.feed_eyebrow_notice,
        title = R.string.feed_run_weekly_title,
        body = R.string.feed_run_weekly_body,
    ),
    FeedItem(
        icon = Icons.Filled.Groups,
        eyebrow = R.string.feed_eyebrow_notice,
        title = R.string.feed_run_crew_title,
        body = R.string.feed_run_crew_body,
    ),
    FeedItem(
        icon = Icons.Filled.Bolt,
        eyebrow = R.string.feed_eyebrow_notice,
        title = R.string.feed_run_flash_title,
        body = R.string.feed_run_flash_body,
    ),
)

/** 특가 공지 — 마켓이 열리면 이 자리에 실제 할인이 올라온다 */
val DEAL_FEED = listOf(
    FeedItem(
        icon = Icons.Filled.Storefront,
        eyebrow = R.string.feed_eyebrow_notice,
        title = R.string.feed_deal_store_title,
        body = R.string.feed_deal_store_body,
        soon = true,
    ),
    FeedItem(
        icon = Icons.Filled.LocalOffer,
        eyebrow = R.string.feed_eyebrow_notice,
        title = R.string.feed_deal_trade_title,
        body = R.string.feed_deal_trade_body,
        soon = true,
    ),
    FeedItem(
        icon = Icons.Filled.DirectionsRun,
        eyebrow = R.string.feed_eyebrow_notice,
        title = R.string.feed_deal_course_title,
        body = R.string.feed_deal_course_body,
    ),
)

/** 건강 뉴스 — StepUp 이 쓴 러닝·회복 이야기 */
val HEALTH_FEED = listOf(
    FeedItem(
        icon = Icons.Filled.SelfImprovement,
        eyebrow = R.string.feed_eyebrow_guide,
        title = R.string.feed_health_warmup_title,
        body = R.string.feed_health_warmup_body,
    ),
    FeedItem(
        icon = Icons.Filled.Favorite,
        eyebrow = R.string.feed_eyebrow_guide,
        title = R.string.feed_health_zone_title,
        body = R.string.feed_health_zone_body,
    ),
    FeedItem(
        icon = Icons.Filled.WbTwilight,
        eyebrow = R.string.feed_eyebrow_guide,
        title = R.string.feed_health_recovery_title,
        body = R.string.feed_health_recovery_body,
    ),
    FeedItem(
        icon = Icons.Filled.DirectionsRun,
        eyebrow = R.string.feed_eyebrow_guide,
        title = R.string.feed_health_cadence_title,
        body = R.string.feed_health_cadence_body,
    ),
)

/** 소식 한 장 */
@Composable
fun FeedCard(item: FeedItem) {
    GlowCard(contentPadding = PaddingValues(16.dp), spacing = 9.dp) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(11.dp),
        ) {
            Box(
                modifier = Modifier
                    .size(38.dp)
                    .clip(RoundedCornerShape(13.dp))
                    .background(CarbonHigh),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = item.icon,
                    contentDescription = null,
                    tint = Volt,
                    modifier = Modifier.size(19.dp),
                )
            }
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                Eyebrow(text = stringResource(item.eyebrow))
                Text(
                    text = stringResource(item.title),
                    style = MaterialTheme.typography.titleSmall,
                    color = Snow,
                )
            }
            if (item.soon) {
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(50))
                        .background(Volt.copy(alpha = 0.14f))
                        .padding(horizontal = 9.dp, vertical = 4.dp),
                ) {
                    Text(
                        text = stringResource(R.string.feed_soon),
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Black,
                        color = Volt,
                    )
                }
            }
        }
        Text(
            text = stringResource(item.body),
            style = MaterialTheme.typography.bodySmall,
            color = Silver,
            lineHeight = 19.sp,
        )
    }
}

/** 목록 끝에 한 줄 — 이 글들이 어디서 왔는지 밝힌다 */
@Composable
fun FeedFootnote(@StringRes text: Int) {
    Text(
        text = stringResource(text),
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 4.dp),
        fontSize = 10.sp,
        color = Slate,
        lineHeight = 15.sp,
    )
}
