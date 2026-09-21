package com.stepup.android.ui.theme

import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color

/** 화면 전체의 빛 방향을 한곳에서 통일한다. */

/** 진행 링 — 한 바퀴 돌며 블루의 밝기가 굽이친다 */
val VoltSweep: Brush = Brush.sweepGradient(
    0.00f to VoltDeep,
    0.30f to Volt,
    0.62f to VoltSoft,
    1.00f to VoltDeep,
)

/** 큰 CTA 면 — START RUN 등 */
val VoltPlate: Brush = Brush.linearGradient(
    listOf(VoltDeep, Volt, VoltSoft),
)

/** 강조 텍스트 채움 */
val VoltInk: Brush = Brush.verticalGradient(
    listOf(VoltSoft, Volt),
)

/** 세로 강조 — 차트 바 */
val VoltVertical: Brush = Brush.verticalGradient(
    listOf(VoltSoft, Volt),
)

/** 카드 표면 — 위가 미세하게 밝다 */
val CardFill: Brush = Brush.verticalGradient(
    listOf(Color(0xFFFAFCFF), Color(0xFFF1F6FF)),
)

/** 앱 배경 — 위쪽이 아주 옅게 푸르다 */
val NightBackdrop: Brush = Brush.verticalGradient(
    listOf(Color(0xFFFBFCFF), Color(0xFFFFFFFF)),
)

/** 양끝이 사라지는 헤어라인 */
val HairlineFade: Brush = Brush.horizontalGradient(
    listOf(Color.Transparent, Color(0xFF101D40).copy(alpha = 0.08f), Color.Transparent),
)
