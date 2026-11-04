package com.stepup.android.ui.screens.market

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
import androidx.compose.material.icons.filled.SwapHoriz
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.stepup.android.R
import com.stepup.android.domain.Faction
import com.stepup.android.domain.MarketShoeLook
import com.stepup.android.domain.Rarity
import com.stepup.android.domain.Sneaker
import com.stepup.android.domain.tier
import com.stepup.android.ui.components.SneakerFrame
import com.stepup.android.ui.components.SneakerGradeStage
import com.stepup.android.ui.components.label
import com.stepup.android.ui.components.variantNameRes
import com.stepup.android.ui.theme.Alert
import com.stepup.android.ui.theme.CarbonHigh
import com.stepup.android.ui.theme.Silver
import com.stepup.android.ui.theme.Slate
import com.stepup.android.ui.theme.Snow
import com.stepup.android.ui.theme.Volt

/**
 * 마켓 화면이 함께 쓰는 조각들.
 *
 * 시세판과 모델 장부가 같은 줄 모양을 쓴다 — 같은 값을 두 곳에서 다르게
 * 그리면 같은 것인지 알아보지 못한다.
 */

/** 값 하나. 시세는 소수점이 필요 없다 — SUP 는 1 단위로 오간다. */
fun formatSup(value: Double): String = "%,d".format(value.toLong())

/**
 * 시세판에 쓰려고 만든 겉모습용 스니커즈.
 *
 * 남의 신발이라 폰에는 없다. 신발 탭과 같은 규칙으로 속성 · 등급 · 변형 · 레벨에서 만든다([MarketShoeLook.preview]) —
 * 속성 "NONE"(0066 뒤 신발)도 신발 탭처럼 그린다.
 */
fun previewSneaker(faction: String, rarity: String, variant: Int, level: Int = 1): Sneaker =
    MarketShoeLook.preview(faction, rarity, variant, level)

/**
 * 줄의 이름. 매물마다 모델이 다를 수 있는 줄(0066 뒤 신발의 레어 이상 — [MarketShoeLook.pinsDesign])은 예전 불 모델 이름을
 * 붙이지 않고 "레어 신발"처럼 등급만 적는다. 나머지는 신발 탭과 같은 도감 이름이다.
 */
@Composable
fun modelName(faction: String, rarity: String, variant: Int): String {
    if (!MarketShoeLook.pinsDesign(faction, rarity)) {
        return stringResource(R.string.market_model_any, Rarity.of(rarity).tier.label())
    }
    return stringResource(variantNameRes(Faction.of(faction), Rarity.of(rarity), variant))
}

/**
 * 줄의 신발 그림. 모델이 정해진 줄은 신발 탭과 같은 그림, 매물마다 모델이 다를 수 있는 줄은 그 등급 틀만(다른 신발 그림을 대신 넣지 않는다).
 */
@Composable
fun MarketShoeThumb(
    faction: String,
    rarity: String,
    variant: Int,
    modifier: Modifier = Modifier,
    level: Int = 1,
) {
    val preview = previewSneaker(faction, rarity, variant, level)
    if (MarketShoeLook.pinsDesign(faction, rarity)) {
        SneakerFrame(preview, modifier = modifier, corner = 14.dp)
    } else {
        Box(modifier, contentAlignment = Alignment.Center) {
            SneakerGradeStage(preview, Modifier.fillMaxWidth(), showShoe = false)
        }
    }
}

/** 값 한 칸 — 이름과 숫자. 없으면 "—" 로 둔다. 0 으로 적으면 거짓말이다. */
@Composable
fun PriceCell(
    label: String,
    value: Double?,
    modifier: Modifier = Modifier,
    accent: Boolean = false,
) {
    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Text(text = label, fontSize = 14.sp, color = Silver)
        Text(
            text = value?.let { formatSup(it) } ?: "—",
            fontSize = 18.sp,
            fontWeight = FontWeight.Bold,
            color = if (value == null) Slate else if (accent) Volt else Snow,
        )
    }
}

/** 거래소 아이콘 한 칸 */
@Composable
fun MarketIcon(modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .size(38.dp)
            .clip(RoundedCornerShape(13.dp))
            .background(CarbonHigh),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            imageVector = Icons.Filled.SwapHoriz,
            contentDescription = null,
            tint = Volt,
            modifier = Modifier.size(19.dp),
        )
    }
}

/** 작은 표시 — 레벨, 민팅 번호처럼 줄에 붙는 것들 */
@Composable
fun Tag(text: String, accent: Boolean = false) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(50))
            .background(if (accent) Volt.copy(alpha = 0.14f) else CarbonHigh)
            .padding(horizontal = 8.dp, vertical = 3.dp),
    ) {
        Text(
            text = text,
            fontSize = 14.sp,
            fontWeight = FontWeight.Bold,
            color = if (accent) Volt else Silver,
        )
    }
}

/**
 * 못 가져왔을 때.
 *
 * 무엇을 해야 하는지까지 적는다 — "오류"만 적으면 사용자가 할 수 있는 일은
 * 앱을 껐다 켜는 것뿐이다.
 */
@Composable
fun MarketProblemNote(problem: MarketProblem, modifier: Modifier = Modifier, onRetry: (() -> Unit)? = null) {
    val text = stringResource(
        when (problem) {
            MarketProblem.SIGN_IN -> R.string.market_problem_sign_in
            MarketProblem.REJECTED -> R.string.market_problem_rejected
            MarketProblem.OFFLINE -> R.string.market_problem_offline
        }
    )
    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(Alert.copy(alpha = 0.10f))
            .padding(horizontal = 14.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.bodyMedium,
            color = Silver,
            lineHeight = 18.sp,
        )
        if (problem == MarketProblem.SIGN_IN) {
            com.stepup.android.ui.components.SignInAgainButton()
        } else if (onRetry != null) {
            com.stepup.android.ui.components.GhostButton(
                text = stringResource(R.string.crew_retry),
                onClick = onRetry,
                modifier = Modifier.fillMaxWidth(),
            )
        }
    }
}

/** 시세판 한 줄 — 모델 하나 */
@Composable
fun QuoteRowCard(
    faction: String,
    rarity: String,
    variant: Int,
    supply: Int,
    ask: Double?,
    bid: Double?,
    lastPrice: Double?,
    modifier: Modifier = Modifier,
) {
    Column(modifier.fillMaxWidth().clip(RoundedCornerShape(20.dp)).background(CarbonHigh).padding(20.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(16.dp)) {
            val preview = previewSneaker(faction, rarity, variant)
            MarketShoeThumb(faction, rarity, variant, modifier = Modifier.size(80.dp))
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                // 이름 끝에 공통 등급 배지(2026-09-29) — 등급을 따로 한 줄로 쓰지 않는다
                com.stepup.android.ui.components.ShoeNameWithBadge(
                    name = modelName(faction, rarity, variant), tier = preview.tier,
                    style = MaterialTheme.typography.titleMedium.copy(color = Snow), maxLines = 2,
                    modifier = Modifier.fillMaxWidth(),
                )
                Text(stringResource(R.string.market_supply, supply), style = MaterialTheme.typography.bodyMedium, color = Silver)
            }
        }
        PriceCell(stringResource(R.string.market_ask_short), ask, accent = true)
        PriceCell(stringResource(R.string.market_bid_short), bid)
        PriceCell(stringResource(R.string.market_last_short), lastPrice)
    }
}

/** 카드 안에 넣는 빈 줄 안내 */
@Composable
fun EmptyNote(text: String, modifier: Modifier = Modifier) {
    Text(
        text = text,
        modifier = modifier.padding(vertical = 10.dp),
        fontSize = 15.sp,
        color = Silver,
        lineHeight = 22.sp,
    )
}

/** 화면 위에 잠깐 뜨는 말 */
@Composable
fun MarketMessageBar(message: MarketMessage, modifier: Modifier = Modifier) {
    val text = when (message) {
        is MarketMessage.Text -> message.value
        is MarketMessage.Res -> message.arg?.let { stringResource(message.id, it) } ?: stringResource(message.id)
    }
    Box(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(Volt.copy(alpha = 0.12f))
            .padding(horizontal = 14.dp, vertical = 10.dp),
    ) {
        Text(text = text, fontSize = 12.sp, color = Snow, lineHeight = 18.sp)
    }
}

/** 값을 적는 칸이 딸린 다이얼로그가 쓰는 제목줄 */
@Composable
fun DialogTitle(text: String) {
    Text(
        text = text,
        fontSize = 17.sp,
        fontWeight = FontWeight.Black,
        letterSpacing = (-0.4).sp,
        color = Snow,
    )
}

/** 카드 머리 — 아이콘 · 제목 · 부제 */
@Composable
fun MarketCardHead(title: String, sub: String) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(11.dp),
        modifier = Modifier.fillMaxWidth(),
    ) {
        MarketIcon()
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
            Text(text = title, style = MaterialTheme.typography.titleSmall, color = Snow)
            Text(text = sub, fontSize = 11.sp, color = Silver, lineHeight = 16.sp)
        }
    }
}

/** 카드 padding 기본값 — 마켓 안에서는 같은 여백을 쓴다 */
val MarketCardPadding = PaddingValues(16.dp)
