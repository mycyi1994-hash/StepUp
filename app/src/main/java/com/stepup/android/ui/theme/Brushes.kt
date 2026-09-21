package com.stepup.android.ui.theme

import androidx.compose.ui.graphics.Brush

/**
 * 화면 전체의 빛 방향을 한곳에서 통일한다.
 *
 * 브러시는 [StepUpPalette] 가 테마마다 한 번씩 만들어 들고 있다. 여기서
 * 매번 만들면 그리기마다 객체가 새로 생긴다.
 */

/** 진행 링 */
val VoltSweep: Brush get() = StepUpColors.voltSweep

/** 큰 CTA 면 — START RUN 등 */
val VoltPlate: Brush get() = StepUpColors.voltPlate

/** 강조 텍스트 채움 */
val VoltInk: Brush get() = StepUpColors.voltInk

/** 세로 강조 — 차트 바 */
val VoltVertical: Brush get() = StepUpColors.voltVertical

/** 카드 표면 */
val CardFill: Brush get() = StepUpColors.cardFill

/** 앱 배경 */
val NightBackdrop: Brush get() = StepUpColors.backdrop

/** 양끝이 사라지는 헤어라인 */
val HairlineFade: Brush get() = StepUpColors.hairlineFade
