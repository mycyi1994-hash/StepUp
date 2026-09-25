package com.stepup.android.ui.components

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.unit.dp

/**
 * 머티리얼 아이콘 모음에 없는 것.
 *
 * 꾸미기 탭은 티셔츠 모양이어야 "옷을 입히는 곳"으로 읽힌다. 옷걸이는
 * 옷장이나 세탁으로 읽히고, 붓은 사진 편집으로 읽힌다.
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
}
