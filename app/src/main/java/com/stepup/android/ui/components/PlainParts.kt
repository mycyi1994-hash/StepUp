package com.stepup.android.ui.components

import androidx.annotation.DrawableRes
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.graphics.painter.BitmapPainter
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.imageResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.foundation.Canvas
import com.stepup.android.ui.experience.feedbackClickable
import java.util.concurrent.ConcurrentHashMap

/*
 * 2026-11-24 재디자인 공통 부품(StepUp 새 빌드 재디자인 — 개발 적용 안내 2-4장).
 *
 * - [PlainListCard] + [PlainListRow]: 흰 카드 하나에 줄을 구분선으로 나눈다. 이름 · 값 · (필요하면) 가는 막대.
 * - [PlainEmptyState]: 빈 · 오류 상태는 모두 이것 — 캐릭터 그림 → 제목 → 설명 → 글자 버튼 하나.
 * - [ProductImage]: 신발 그림은 상자에 넣지 않고 바탕 위 가운데, 높이만 고정. 그림 파일의 투명 여백은 잘라 그린다.
 * - [PlainNumber]: 큰 숫자 뒤 단위는 6dp 띄운다.
 *
 * 넣지 않는 것(안내 2-6장): 제목 위 알약 배지 · 숫자 세 칸 나란히 · 번호 목록 · 아이콘 넣은 색 원/네모 · 이니셜 원 ·
 * 그라데이션 면 · 빛 번짐 · 링 그래프 · ⓘ 안내 줄 · 사진 위 알약 칩 · 이모지. 면은 흰 카드 / 회색 안쪽 칸(inset) 두 가지,
 * 색(eff · com · dur · gold)은 숫자 · 막대 · 글자 버튼에만.
 */

private val CardShape = RoundedCornerShape(22.dp)

/** 흰 카드 하나(어두운 테마는 panel) — 안에 [PlainListRow] 를 차례로 넣고 줄 사이는 [PlainDivider] */
@Composable
fun PlainListCard(modifier: Modifier = Modifier, content: @Composable ColumnScope.() -> Unit) {
    val t = runTone()
    Column(
        modifier.fillMaxWidth().clip(CardShape).background(t.panel)
            // 시안 카드는 밝게 · 어둡게 모두 1dp 옅은 테두리
            .border(1.dp, t.panelEdge, CardShape).padding(horizontal = 18.dp),
        content = content,
    )
}

/** 카드 안 줄 사이 1dp 구분선 */
@Composable
fun PlainDivider(modifier: Modifier = Modifier) {
    val t = runTone()
    Box(modifier.fillMaxWidth().height(1.dp).background(t.divider))
}

/**
 * 카드 한 줄 — 왼쪽 이름(15.5sp SemiBold) · 오른쪽 값(16sp Bold, 숫자 폭 고정 · 줄바꿈 없음).
 * [progress] 가 있으면 아래 가는 막대(3dp, 위 9dp). [sub] 는 이름 아래 보조 한 줄. [onClick] 이 있으면 줄 전체를 누르고 › 를 단다.
 */
@Composable
fun PlainListRow(
    title: String,
    modifier: Modifier = Modifier,
    value: String? = null,
    valueColor: Color? = null,
    sub: String? = null,
    progress: Float? = null,
    barColor: Color? = null,
    onClick: (() -> Unit)? = null,
    chevron: Boolean = onClick != null,
    minHeight: Dp = 56.dp,
    leading: (@Composable () -> Unit)? = null,
    trailing: (@Composable RowScope.() -> Unit)? = null,
) {
    val t = runTone()
    val base = if (onClick != null) modifier.feedbackClickable(onClick = onClick) else modifier
    Column(base.fillMaxWidth().heightIn(min = minHeight).padding(vertical = 14.dp), verticalArrangement = Arrangement.Center) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            if (leading != null) {
                leading()
                Spacer(Modifier.width(12.dp))
            }
            Column(Modifier.weight(1f)) {
                Text(title, style = runTextStyle(15.5.sp, t.text, FontWeight.SemiBold, 1.3f), maxLines = 2, overflow = TextOverflow.Ellipsis)
                if (sub != null) {
                    Spacer(Modifier.height(2.dp))
                    Text(sub, style = runTextStyle(13.sp, t.label, FontWeight.Medium, 1.35f))
                }
            }
            if (value != null) {
                Spacer(Modifier.width(12.dp))
                Text(
                    value,
                    style = runTextStyle(16.sp, valueColor ?: t.text, FontWeight.Bold, 1.2f).copy(fontFeatureSettings = "tnum"),
                    maxLines = 1, softWrap = false,
                )
            }
            if (trailing != null) {
                Spacer(Modifier.width(10.dp))
                trailing()
            }
            if (chevron) {
                Spacer(Modifier.width(4.dp))
                Icon(Icons.Filled.ChevronRight, null, tint = t.muted, modifier = Modifier.size(20.dp))
            }
        }
        if (progress != null) {
            Spacer(Modifier.height(9.dp))
            PlainBar(progress, barColor ?: t.cobalt, Modifier.fillMaxWidth(), height = 3.dp)
        }
    }
}

/** 가는 진행 막대 — 바탕 track, 채움 한 색(그라데이션 없음) */
@Composable
fun PlainBar(progress: Float, color: Color, modifier: Modifier = Modifier, height: Dp = 6.dp) {
    val t = runTone()
    val p = progress.coerceIn(0f, 1f)
    Canvas(modifier.height(height)) {
        val h = size.height
        val y = h / 2f
        drawLine(t.track, Offset(h / 2f, y), Offset(size.width - h / 2f, y), strokeWidth = h, cap = StrokeCap.Round)
        if (p > 0f) {
            val end = (h / 2f + (size.width - h) * p).coerceAtLeast(h / 2f)
            drawLine(color, Offset(h / 2f, y), Offset(end, y), strokeWidth = h, cap = StrokeCap.Round)
        }
    }
}

/**
 * 빈 · 오류 상태 — 가운데 정렬. 캐릭터 그림(높이 [imageHeight], 비율 그대로) → 제목 18sp Bold → 설명 14sp → 글자 버튼 하나.
 */
@Composable
fun PlainEmptyState(
    @DrawableRes image: Int?,
    title: String,
    modifier: Modifier = Modifier,
    body: String? = null,
    action: String? = null,
    onAction: (() -> Unit)? = null,
    imageHeight: Dp = 116.dp,
    /** 글자 버튼 꼬리표(검사가 집는다) · 끝의 › (시안의 빈 · 오류 상태 글자 버튼에는 없다) */
    actionTag: String? = null,
    actionChevron: Boolean = false,
) {
    val t = runTone()
    Column(modifier.fillMaxWidth().padding(vertical = 24.dp), horizontalAlignment = Alignment.CenterHorizontally) {
        if (image != null) {
            Image(
                painterResource(image), contentDescription = null,
                modifier = Modifier.height(imageHeight), contentScale = ContentScale.Fit,
            )
            Spacer(Modifier.height(16.dp))
        }
        Text(title, style = runTextStyle(18.sp, t.text, FontWeight.Bold, 1.3f), textAlign = TextAlign.Center)
        if (body != null) {
            Spacer(Modifier.height(6.dp))
            Text(body, style = runTextStyle(14.sp, t.label, FontWeight.Medium, 1.45f), textAlign = TextAlign.Center)
        }
        if (action != null && onAction != null) {
            Spacer(Modifier.height(14.dp))
            RunTextAction(
                action, onAction, chevron = actionChevron,
                modifier = if (actionTag != null) Modifier.testTag(actionTag) else Modifier,
            )
        }
    }
}

/**
 * 신발(물건) 그림 — 상자 없이 바탕 위 가운데, 높이만 고정하고 너비는 비율대로. 그림 파일 위아래의 투명 여백을 잘라 그려
 * 칸 밖으로 삐져나오거나 작아 보이지 않게 한다(자른 범위는 그림마다 한 번 재어 둔다).
 */
@Composable
fun ProductImage(@DrawableRes res: Int, height: Dp, modifier: Modifier = Modifier, contentDescription: String? = null) {
    val bitmap = ImageBitmap.imageResource(res)
    val painter = remember(res, bitmap) {
        val r = OpaqueBounds.of(res, bitmap)
        BitmapPainter(bitmap, IntOffset(r.left, r.top), IntSize(r.width, r.height))
    }
    Image(painter, contentDescription, modifier.height(height), contentScale = ContentScale.Fit)
}

private object OpaqueBounds {
    data class Box(val left: Int, val top: Int, val width: Int, val height: Int)
    private val cache = ConcurrentHashMap<Int, Box>()

    fun of(res: Int, image: ImageBitmap): Box = cache.getOrPut(res) { measure(image) }

    private fun measure(image: ImageBitmap): Box {
        val b = image.asAndroidBitmap()
        val w = b.width
        val h = b.height
        if (!b.hasAlpha() || w == 0 || h == 0) return Box(0, 0, w, h)
        val row = IntArray(w)
        var top = -1
        var bottom = -1
        var left = w
        var right = -1
        for (y in 0 until h) {
            b.getPixels(row, 0, w, 0, y, w, 1)
            var any = false
            for (x in 0 until w) {
                if ((row[x] ushr 24) > 12) {
                    any = true
                    if (x < left) left = x
                    if (x > right) right = x
                }
            }
            if (any) {
                if (top < 0) top = y
                bottom = y
            }
        }
        if (top < 0) return Box(0, 0, w, h)
        return Box(left, top, right - left + 1, bottom - top + 1)
    }
}

/** 큰 숫자 + 단위 — 단위는 6dp 띄워 숫자에 붙어 보이지 않게(「16 회」) */
@Composable
fun PlainNumber(
    number: String,
    unit: String?,
    modifier: Modifier = Modifier,
    size: TextUnit = 54.sp,
    color: Color? = null,
    unitColor: Color? = null,
    unitSize: TextUnit = 20.sp,
) {
    val t = runTone()
    Row(modifier, verticalAlignment = Alignment.Bottom) {
        Text(number, style = runNumberStyle(size, color ?: t.text), maxLines = 1, softWrap = false)
        if (!unit.isNullOrEmpty()) {
            Spacer(Modifier.width(6.dp))
            Text(
                unit, style = runTextStyle(unitSize, unitColor ?: t.label, FontWeight.SemiBold, 1.2f),
                maxLines = 1, softWrap = false, modifier = Modifier.padding(bottom = (size.value * 0.12f).dp),
            )
        }
    }
}
