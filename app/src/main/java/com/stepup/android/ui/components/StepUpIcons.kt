package com.stepup.android.ui.components

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathFillType
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.vector.addPathNodes
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.unit.dp

/**
 * 머티리얼 아이콘 모음에 없는 것.
 *
 * 꾸미기 탭은 티셔츠 모양이어야 "옷을 입히는 곳"으로 읽힌다. 옷걸이는
 * 옷장이나 세탁으로 읽히고, 붓은 사진 편집으로 읽힌다.
 * 뽑기 탭은 뚜껑 덮인 상자(신발 화면 확정안 2026-09-28의 하단 탭) — 선물 리본은 구매 · 선물로 읽혀 넣지 않는다.
 */
object StepUpIcons {
    val Shoe: ImageVector by lazy {
        ImageVector.Builder(name = "StepUpShoe", defaultWidth = 24.dp, defaultHeight = 24.dp,
            viewportWidth = 24f, viewportHeight = 24f).apply {
            path(fill = SolidColor(Color.Black)) {
                moveTo(2f, 14f); lineTo(4f, 6f); lineTo(7f, 7f)
                curveTo(7f, 10f, 9f, 11f, 11f, 9f)
                lineTo(13f, 11f); lineTo(11f, 12f); lineTo(12f, 13f)
                lineTo(14f, 12f); lineTo(16f, 14f); lineTo(20f, 15f)
                curveTo(21f, 15f, 22f, 16f, 22f, 18f)
                lineTo(22f, 20f); lineTo(2f, 20f); close()
            }
        }.build()
    }

    val Shirt: ImageVector by lazy {
        ImageVector.Builder(
            name = "StepUpShirt",
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 24f,
            viewportHeight = 24f,
        ).apply {
            path(fill = SolidColor(Color.Black)) {
                // 어깨 → 소매 → 몸통 → 반대쪽 소매, 목둘레는 오목하게
                moveTo(8.2f, 3.0f)
                curveTo(9.0f, 4.4f, 10.4f, 5.2f, 12.0f, 5.2f)
                curveTo(13.6f, 5.2f, 15.0f, 4.4f, 15.8f, 3.0f)
                lineTo(20.6f, 5.2f)
                curveTo(21.3f, 5.5f, 21.6f, 6.3f, 21.3f, 7.0f)
                lineTo(20.0f, 9.9f)
                curveTo(19.8f, 10.4f, 19.2f, 10.6f, 18.7f, 10.4f)
                lineTo(17.2f, 9.8f)
                lineTo(17.2f, 19.8f)
                curveTo(17.2f, 20.5f, 16.7f, 21.0f, 16.0f, 21.0f)
                lineTo(8.0f, 21.0f)
                curveTo(7.3f, 21.0f, 6.8f, 20.5f, 6.8f, 19.8f)
                lineTo(6.8f, 9.8f)
                lineTo(5.3f, 10.4f)
                curveTo(4.8f, 10.6f, 4.2f, 10.4f, 4.0f, 9.9f)
                lineTo(2.7f, 7.0f)
                curveTo(2.4f, 6.3f, 2.7f, 5.5f, 3.4f, 5.2f)
                close()
            }
        }.build()
    }

    /** 뽑기 — 뚜껑 · 몸통 · 가운데 걸쇠 자리(비움) */
    val DrawBox: ImageVector by lazy {
        ImageVector.Builder(name = "StepUpDrawBox", defaultWidth = 24.dp, defaultHeight = 24.dp,
            viewportWidth = 24f, viewportHeight = 24f).apply {
            path(fill = SolidColor(Color.Black)) {
                moveTo(4.2f, 4.2f); lineTo(19.8f, 4.2f)
                curveTo(20.9f, 4.2f, 21.6f, 4.9f, 21.6f, 6.0f)
                lineTo(21.6f, 7.6f)
                curveTo(21.6f, 8.7f, 20.9f, 9.4f, 19.8f, 9.4f)
                lineTo(4.2f, 9.4f)
                curveTo(3.1f, 9.4f, 2.4f, 8.7f, 2.4f, 7.6f)
                lineTo(2.4f, 6.0f)
                curveTo(2.4f, 4.9f, 3.1f, 4.2f, 4.2f, 4.2f)
                close()
            }
            path(fill = SolidColor(Color.Black), pathFillType = PathFillType.EvenOdd) {
                moveTo(3.6f, 10.8f); lineTo(20.4f, 10.8f); lineTo(20.4f, 18.6f)
                curveTo(20.4f, 19.9f, 19.5f, 20.8f, 18.2f, 20.8f)
                lineTo(5.8f, 20.8f)
                curveTo(4.5f, 20.8f, 3.6f, 19.9f, 3.6f, 18.6f)
                close()
                moveTo(9.4f, 12.4f); lineTo(14.6f, 12.4f); lineTo(14.6f, 14.2f)
                curveTo(14.6f, 14.8f, 14.2f, 15.2f, 13.6f, 15.2f)
                lineTo(10.4f, 15.2f)
                curveTo(9.8f, 15.2f, 9.4f, 14.8f, 9.4f, 14.2f)
                close()
            }
        }.build()
    }

    // ── 하단 탭 선 아이콘(UI 최종 통합안 2026-10-25) — 가는 선(1.7) · 둥근 끝. 선 모양은 Tabler Icons(MIT)를 따랐다 ──

    private fun strokeIcon(name: String, vararg paths: String): ImageVector =
        ImageVector.Builder(name = name, defaultWidth = 24.dp, defaultHeight = 24.dp, viewportWidth = 24f, viewportHeight = 24f).apply {
            paths.forEach { d ->
                addPath(
                    pathData = addPathNodes(d), fill = null, stroke = SolidColor(Color.Black), strokeLineWidth = 1.7f,
                    strokeLineCap = StrokeCap.Round, strokeLineJoin = StrokeJoin.Round,
                )
            }
        }.build()

    /** 러닝 — 달리는 사람 */
    val TabRun: ImageVector by lazy {
        strokeIcon("TabRun", "M12 4a1 1 0 1 0 2 0a1 1 0 1 0 -2 0", "M4 17l5 1l.75 -1.5", "M15 21v-4l-4 -3l1 -6", "M7 12v-3l5 -1l3 3l3 1")
    }

    /** 신발 — 운동화 옆모습 */
    val TabShoe: ImageVector by lazy {
        strokeIcon(
            "TabShoe",
            "M4 6h5.426a1 1 0 0 1 .863 .496l1.064 1.823a3 3 0 0 0 1.896 1.407l4.677 1.114a4 4 0 0 1 3.074 3.89v2.27a1 1 0 0 1 -1 1h-16a1 1 0 0 1 -1 -1v-10a1 1 0 0 1 1 -1z",
            "M14 13l1 -2", "M8 18v-1a4 4 0 0 0 -4 -4h-1", "M10 12l1.5 -3",
        )
    }

    /** 뽑기 — 뚜껑 덮인 상자(리본 없이) */
    val TabDraw: ImageVector by lazy {
        strokeIcon(
            "TabDraw",
            "M4.5 4.5h15a1.5 1.5 0 0 1 1.5 1.5v1.5a1.5 1.5 0 0 1 -1.5 1.5h-15a1.5 1.5 0 0 1 -1.5 -1.5v-1.5a1.5 1.5 0 0 1 1.5 -1.5z",
            "M4.5 10.5v8a2 2 0 0 0 2 2h11a2 2 0 0 0 2 -2v-8", "M10 13.5h4",
        )
    }

    /** 커뮤니티 — 세 사람 */
    val TabCommunity: ImageVector by lazy {
        strokeIcon(
            "TabCommunity",
            "M10 13a2 2 0 1 0 4 0a2 2 0 0 0 -4 0", "M8 21v-1a2 2 0 0 1 2 -2h4a2 2 0 0 1 2 2v1",
            "M15 5a2 2 0 1 0 4 0a2 2 0 0 0 -4 0", "M17 10h2a2 2 0 0 1 2 2v1",
            "M5 5a2 2 0 1 0 4 0a2 2 0 0 0 -4 0", "M3 13v-1a2 2 0 0 1 2 -2h2",
        )
    }

    /** 내 정보 — 원 안의 사람 */
    val TabProfile: ImageVector by lazy {
        strokeIcon(
            "TabProfile",
            "M3 12a9 9 0 1 0 18 0a9 9 0 1 0 -18 0", "M9 10a3 3 0 1 0 6 0a3 3 0 1 0 -6 0",
            "M6.168 18.849a4 4 0 0 1 3.832 -2.849h4a4 4 0 0 1 3.834 2.855",
        )
    }
}
