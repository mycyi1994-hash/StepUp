package com.stepup.android.ui.screens.items

import android.widget.Toast
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.stepup.android.R
import com.stepup.android.ui.components.runTextStyle
import com.stepup.android.ui.components.runTone
import com.stepup.android.ui.experience.feedbackClickable

/**
 * 아이템칸(2026-11-07 "아이템칸이 생긴다는 가정하에 디자인").
 *
 * 서버에 아직 신발에 끼우는 칸이 없다 — 칸 수 · 해금 조건 · 장착 계약이 정해지기 전까지 잠긴 자리만 둔다.
 * 아이템 · 개수 · 숫자를 지어내지 않는다(데모 모드도 같다). 계정 단위 시간제 부스터(에너지 셀 등 — 보관함 › 아이템)와는 다른 것이다.
 */
object ItemSlots {
    /** 아이템칸이 열렸는가 — 서버 계약이 생기면 켜고, 그때 칸 수 · 끼운 아이템은 서버 값으로 그린다 */
    const val OPEN = false

    /** 잠긴 동안 보이는 자리 수 — 디자인 자리일 뿐 규칙이 아니다(강화 재료 칸 셋과 같은 리듬) */
    const val PLACEHOLDER = 3
}

private val SlotRadius = 14.dp

/**
 * 신발 상세 본문의 아이템칸 — 제목 "아이템칸" + 오른쪽 "곧 열려요"(보조 색 글자, 배지 아님) → 같은 폭의 정사각 칸 셋 → 한 줄 설명.
 * 칸은 강화 재료 칸과 같은 면([slotSurface])에 1dp 점선 테두리(어두운 테마 흰색 약 20%, 밝은 테마 옅은 남색) · 자물쇠 · 흐리게(.5).
 * 누르면 아직 쓸 수 없다는 짧은 안내만(시트 · 화면 이동 없음).
 */
@Composable
fun ShoeItemSlots(modifier: Modifier = Modifier) {
    val t = runTone()
    val context = LocalContext.current
    val toast = stringResource(R.string.item_slots_toast)
    val locked = !ItemSlots.OPEN
    val dash = if (t.dark) Color.White.copy(alpha = 0.2f) else Color(0xFF0B2B50).copy(alpha = 0.22f)
    Column(modifier.fillMaxWidth().testTag("shoe-item-slots")) {
        Row(Modifier.fillMaxWidth().padding(start = 4.dp, end = 4.dp, bottom = 8.dp), verticalAlignment = Alignment.CenterVertically) {
            Text(
                stringResource(R.string.item_slots_title), style = runTextStyle(18.sp, t.text, FontWeight.Bold),
                modifier = Modifier.weight(1f).semantics { heading() },
            )
            if (locked) {
                Text(stringResource(R.string.item_slots_soon), style = runTextStyle(14.sp, t.label, FontWeight.Medium), maxLines = 1)
            }
        }
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            repeat(ItemSlots.PLACEHOLDER) { index ->
                val description = stringResource(R.string.item_slot_locked_cd, index + 1)
                Box(
                    Modifier.weight(1f).aspectRatio(1f)
                        .alpha(if (locked) 0.5f else 1f)
                        .then(slotSurface(edge = false))
                        .dashedEdge(dash)
                        .feedbackClickable(enabled = locked, role = null) {
                            Toast.makeText(context, toast, Toast.LENGTH_SHORT).show()
                        }
                        .semantics { contentDescription = description }
                        .testTag("shoe-item-slot-$index"),
                    contentAlignment = Alignment.Center,
                ) {
                    if (locked) Icon(Icons.Filled.Lock, contentDescription = null, tint = t.muted, modifier = Modifier.size(22.dp))
                }
            }
        }
        Text(
            stringResource(R.string.item_slots_body), style = runTextStyle(13.sp, t.label, FontWeight.Medium, 1.4f),
            modifier = Modifier.padding(start = 4.dp, end = 4.dp, top = 8.dp),
        )
    }
}

/** 칸 둘레의 1dp 점선 — 선 굵기의 절반만큼 안으로 들여 모서리가 잘리지 않게 */
private fun Modifier.dashedEdge(color: Color): Modifier = drawWithContent {
    drawContent()
    val stroke = 1.dp.toPx()
    val radius = SlotRadius.toPx() - stroke / 2f
    drawRoundRect(
        color = color,
        topLeft = Offset(stroke / 2f, stroke / 2f),
        size = Size(size.width - stroke, size.height - stroke),
        cornerRadius = CornerRadius(radius, radius),
        style = Stroke(width = stroke, pathEffect = PathEffect.dashPathEffect(floatArrayOf(6.dp.toPx(), 4.dp.toPx()))),
    )
}
