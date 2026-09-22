package com.stepup.android.ui.components

import android.provider.Settings
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.animateIntAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext

/**
 * 값이 툭 바뀌지 않고 굴러 올라가게 한다. 숫자가 "쌓이는" 감각이 곧 리워드 앱의 만족감.
 */
@Composable
fun animatedInt(target: Int, durationMillis: Int = 750): Int {
    val value by animateIntAsState(
        targetValue = target,
        animationSpec = tween(durationMillis, easing = FastOutSlowInEasing),
        label = "animatedInt",
    )
    return value
}

/**
 * 기기에서 애니메이션을 꺼 두었는가.
 *
 * 설정 > 접근성에서 애니메이션을 끄면 ANIMATOR_DURATION_SCALE 이 0이 된다.
 * 움직임에 어지러움을 느끼는 사람이 직접 끈 것이므로, 앱이 제 판단으로
 * 다시 움직이게 해서는 안 된다. 값이 바로 도착해야 하는 자리에서는
 * 이 함수가 true 를 돌려주고, 부르는 쪽은 애니메이션 없이 그린다.
 */
@Composable
fun reduceMotion(): Boolean {
    val context = LocalContext.current
    return remember(context) {
        runCatching {
            Settings.Global.getFloat(
                context.contentResolver,
                Settings.Global.ANIMATOR_DURATION_SCALE,
                1f,
            )
        }.getOrDefault(1f) == 0f
    }
}

@Composable
fun animatedFloat(target: Float, durationMillis: Int = 750): Float {
    val value by animateFloatAsState(
        targetValue = target,
        animationSpec = tween(durationMillis, easing = FastOutSlowInEasing),
        label = "animatedFloat",
    )
    return value
}

/**
 * 금속 표면을 훑고 지나가는 광택 띠.
 *
 * 모디파이어 체인에서 배경 **뒤**에 붙여야 배경 위로 빛이 흐른다.
 * 클리핑이 필요한 경우 앞쪽에 `Modifier.clip(shape)`을 먼저 둘 것.
 */
@Composable
fun Modifier.sheen(
    color: Color = Color.White,
    alpha: Float = 0.16f,
    bandFraction: Float = 0.28f,
    durationMillis: Int = 4200,
    delayMillis: Int = 1400,
): Modifier {
    val transition = rememberInfiniteTransition(label = "sheen")
    val progress by transition.animateFloat(
        initialValue = -0.5f,
        targetValue = 1.5f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis, delayMillis, LinearEasing),
            repeatMode = RepeatMode.Restart,
        ),
        label = "sheenProgress",
    )
    return drawWithContent {
        drawContent()
        val band = size.width * bandFraction
        val x = size.width * progress
        drawRect(
            brush = Brush.linearGradient(
                colors = listOf(Color.Transparent, color.copy(alpha = alpha), Color.Transparent),
                start = Offset(x - band, 0f),
                end = Offset(x + band, size.height),
            ),
        )
    }
}

/** 살아 있는 상태(워킹 중)를 알리는 아주 느린 호흡. 0.0~1.0 */
@Composable
fun breathing(durationMillis: Int = 2600): Float {
    val transition = rememberInfiniteTransition(label = "breathing")
    val value by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse,
        ),
        label = "breathingValue",
    )
    return value
}
