package com.stepup.android.ui.components

import androidx.compose.ui.graphics.ColorFilter

/* 지도 타일 색(안드로이드 RouteMapLive.kt 와 같은 값) — 지도 그리기 자체는 러닝 화면을 옮길 때(C2) 함께 */

/** 지도 타일 색 — 러닝 화면의 지도 틀([RunMapFrame])이 남색 시안 색으로 바꿔 넣는다 */
@androidx.compose.runtime.Immutable
data class MapTone(
    val filter: ColorFilter? = null,
    val shade: Float = 0.30f,
    val roads: ColorFilter? = null,
)

val LocalMapTone = androidx.compose.runtime.staticCompositionLocalOf { MapTone() }

/** 밝은 거리 지도 — 실제 타일 색을 거의 그대로, 땅의 누런 기만 덜어 낸다 */
val BrightMapFilter = ColorFilter.colorMatrix(
    androidx.compose.ui.graphics.ColorMatrix(
        floatArrayOf(
            1.0311f, -0.0567f, -0.0060f, 0f, 7.65f,
            -0.0165f, 1.0128f, -0.0060f, 0f, 7.65f,
            -0.0165f, -0.0567f, 1.1197f, 0f, 7.65f,
            0f, 0f, 0f, 1f, 0f,
        ),
    ),
)

/** 러닝 · 크루 달리기 지도의 색 — [BrightMapFilter] 만 */
val BrightMapTone = MapTone(filter = BrightMapFilter, shade = 0f, roads = null)
