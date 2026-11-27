package com.stepup.android.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.layout.layout
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.text
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.stepup.android.R
import com.stepup.android.domain.Sneaker
import com.stepup.android.domain.ShoeTier
import com.stepup.android.domain.tier
import com.stepup.android.ui.experience.feedbackClickable
import com.stepup.android.ui.screens.items.shoeName

/*
 * v12 디자인 시제품(2026-11-26)의 신발 카드 — 남색 카드 · 등급 칩(왼쪽 위) · No.(오른쪽 위) · 가운데 신발 · 아래 이름.
 * 신발 탭(카드 넘기기 — [showStats] 로 레벨 원 · 효율 · 착화감)과 신발 상세의 남색 무대가 같은 카드를 쓴다.
 * 테두리는 등급 색을 은빛에 섞는다(일반은 은빛 그대로). 값은 모두 신발이 가진 값 그대로 — 새로 셈하지 않는다.
 */

/** 카드 비율(너비 : 높이) — 시제품 v12_shoes · v12_shoe */
const val V12ShoeCardRatio = 0.72f

private val CardTop = Color(0xFF13285A)
private val CardBottom = Color(0xFF0A1633)
private val CardGlow = Color(0x553A6BD8)
private val Silver = Color(0xFFD3DBE8)

/** 등급 테두리 — 은빛에 등급 색을 조금 섞는다 */
fun v12TierRim(tier: ShoeTier): Color =
    if (tier == ShoeTier.COMMON) Silver else lerp(Silver, tierColors(tier).fill, 0.55f)

@Composable
fun V12ShoeCard(
    shoe: Sneaker,
    modifier: Modifier = Modifier,
    /** 신발 탭 — 레벨 원 · 효율 · 착화감 줄 */
    showStats: Boolean = false,
    efficiency: String? = null,
    comfort: String? = null,
    onClick: (() -> Unit)? = null,
    clickLabel: String? = null,
    /**
     * 화면의 주인공 카드 — 위 줄 · 번호 · 이름에 예전 신발 카드와 같은 테스트 이름(shoe-card-tag · shoe-card-no · shoe-hero-name)을 붙인다.
     * 카드 넘기기의 양옆 카드는 false(같은 이름이 둘 생기지 않게)
     */
    hero: Boolean = true,
    /** 작은 카드(목록 · 시트의 76 ~ 110dp) — 등급 칩만 작게, 번호 · 이름은 빼고 그림을 가운데에 */
    mini: Boolean = false,
) {
    val shape = RoundedCornerShape(if (mini) 12.dp else 22.dp)
    val rim = v12TierRim(shoe.tier)
    Box(
        modifier
            .aspectRatio(V12ShoeCardRatio)
            // 바깥 옅은 테 — 시제품의 카드 둘레 빛(밝은 바탕에서도 카드가 떠 보이게)
            .then(if (mini) Modifier else Modifier.border(6.dp, rim.copy(alpha = .28f), RoundedCornerShape(26.dp)).padding(5.dp))
            .clip(shape)
            .drawBehind {
                drawRect(Brush.verticalGradient(listOf(CardTop, CardBottom)))
                drawCircle(
                    Brush.radialGradient(listOf(CardGlow, Color.Transparent), center = Offset(size.width / 2, size.height * .42f),
                        radius = size.width * .7f),
                    radius = size.width * .7f, center = Offset(size.width / 2, size.height * .42f),
                )
            }
            .border(if (mini) 2.dp else 3.dp, rim, shape)
            .then(if (onClick != null) Modifier.feedbackClickable(role = Role.Button, onClick = onClick) else Modifier)
            .then(if (clickLabel != null) Modifier.semantics { contentDescription = clickLabel } else Modifier),
    ) {
        if (mini) {
            Text(
                shoe.tier.label(), color = Color(0xFF1B2433), fontSize = 9.sp, fontWeight = FontWeight.Bold, maxLines = 1,
                modifier = Modifier.padding(6.dp).clip(RoundedCornerShape(4.dp)).background(Color.White.copy(alpha = .82f))
                    .padding(horizontal = 4.dp, vertical = 1.dp),
            )
            ShoeArtThumbnail(
                shoe, Modifier.align(Alignment.Center).fillMaxWidth(.92f).aspectRatio(1.45f),
                failedTint = Color.White.copy(alpha = .4f), sample = ShoeArtFull,
            )
            LevelCoin(shoe.level, 18.dp, Modifier.align(Alignment.BottomEnd).padding(6.dp))
            return@Box
        }
        Row(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 14.dp), verticalAlignment = Alignment.CenterVertically) {
            // 화면 읽기 · 검사는 "착용 중 · 레어"로 읽는다(눈에는 등급 칩만 — 착용 중은 카드 아래 줄)
            val tierText = shoe.tier.label()
            val meta = if (shoe.equipped) androidx.compose.ui.res.stringResource(R.string.my_shoes_wearing) + " · " + tierText else tierText
            Text(
                tierText, color = Color(0xFF1B2433), fontSize = 13.sp, fontWeight = FontWeight.Bold,
                modifier = Modifier.clip(RoundedCornerShape(6.dp)).background(Color.White.copy(alpha = .82f))
                    .padding(horizontal = 7.dp, vertical = 2.dp)
                    // 이름표가 먼저 — clearAndSetSemantics 는 그 뒤(안쪽) 의미를 지운다
                    .testTag(if (hero) "shoe-card-tag" else "v12-card-tier")
                    .then(if (hero) Modifier.clearAndSetSemantics { text = androidx.compose.ui.text.AnnotatedString(meta) } else Modifier),
            )
            Spacer(Modifier.weight(1f))
            Text(
                com.stepup.android.ui.screens.items.formatShoeNumber(shoe.mintNumber), color = Color.White.copy(alpha = .78f), fontSize = 14.sp,
                fontWeight = FontWeight.SemiBold, maxLines = 1, modifier = if (hero) Modifier.testTag("shoe-card-no") else Modifier,
            )
        }
        ShoeArtThumbnail(
            shoe,
            Modifier.align(Alignment.Center).padding(bottom = if (showStats) 56.dp else 30.dp).fillMaxWidth(.92f).aspectRatio(1.45f)
                .testTag("v12-card-art"),
            failedTint = Color.White.copy(alpha = .4f), sample = ShoeArtFull,
        )
        Column(Modifier.align(Alignment.BottomStart).fillMaxWidth().padding(horizontal = 18.dp, vertical = 16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                // 긴 이름(별명 20자)도 늘 다 보인다 — 세 줄까지, 넘치면 글자를 줄인다(2026-10-17 "이름은 늘 다 보이게")
                val name = shoe.shoeName()
                var nameSize by remember(name) { mutableFloatStateOf(24f) }
                Text(
                    name, color = Color.White, fontSize = nameSize.sp, lineHeight = (nameSize * 1.18f).sp, fontWeight = FontWeight.ExtraBold,
                    maxLines = 3, overflow = TextOverflow.Clip,
                    onTextLayout = { if (it.hasVisualOverflow && nameSize > 12f) nameSize -= 1f },
                    modifier = Modifier.weight(1f).testTag(if (hero) "shoe-hero-name" else "v12-card-name"),
                )
                if (showStats) {
                    Spacer(Modifier.width(8.dp))
                    LevelCoin(shoe.level, 38.dp)
                }
            }
            if (showStats && efficiency != null && comfort != null) {
                Spacer(Modifier.height(10.dp))
                Box(Modifier.fillMaxWidth().height(1.dp).background(Color.White.copy(alpha = .14f)))
                Spacer(Modifier.height(10.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(18.dp), verticalAlignment = Alignment.CenterVertically) {
                    CardStat(androidx.compose.ui.res.stringResource(R.string.my_shoes_stat_efficiency), efficiency)
                    CardStat(androidx.compose.ui.res.stringResource(R.string.sdv_stat_comfort), comfort)
                }
            }
        }
    }
}

@Composable
private fun CardStat(label: String, value: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Text(label, color = Color.White.copy(alpha = .72f), fontSize = 15.sp, fontWeight = FontWeight.Medium)
        Spacer(Modifier.width(6.dp))
        Text(value, color = Color.White, fontSize = 19.sp, fontWeight = FontWeight.ExtraBold)
    }
}

/** 은빛 동전 위 레벨 숫자 */
@Composable
fun LevelCoin(level: Int, size: Dp, modifier: Modifier = Modifier) {
    Box(
        modifier.size(size).clip(CircleShape)
            .background(Brush.linearGradient(listOf(Color(0xFFF2F5FA), Color(0xFFA9B4C6), Color(0xFFE4E9F1))))
            .border(1.dp, Color.White.copy(alpha = .7f), CircleShape),
        contentAlignment = Alignment.Center,
    ) {
        Text("$level", color = Color(0xFF1B2433), fontSize = (size.value * .42f).sp, fontWeight = FontWeight.ExtraBold)
    }
}

/** 부모의 좌우 여백을 넘어 화면 끝까지 그린다(신발 상세의 남색 무대) */
fun Modifier.bleedX(gutter: Dp): Modifier = layout { measurable, constraints ->
    val extra = gutter.roundToPx() * 2
    val width = constraints.maxWidth + extra
    val placeable = measurable.measure(constraints.copy(minWidth = width, maxWidth = width))
    layout(constraints.maxWidth, placeable.height) { placeable.place(-extra / 2, 0) }
}
