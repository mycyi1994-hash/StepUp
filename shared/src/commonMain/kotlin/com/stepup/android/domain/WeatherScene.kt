package com.stepup.android.domain

/** 홈 풍경을 고르는 데 쓰는 지금 날씨 — S2 시안 42~45(낮 · 해질녘 · 밤 · 비) */
enum class WeatherScene { DAY, DUSK, NIGHT, RAIN }

object WeatherScenes {
    /** 해 지기 전 이 시간 안이면 해질녘 */
    const val DUSK_WINDOW_SEC = 60L * 60

    /**
     * WMO 날씨 코드(Open-Meteo `weather_code`)를 풍경으로.
     * 비 · 이슬비 · 소나기 · 뇌우는 비. 눈 풍경은 없어 낮/밤으로 둔다.
     */
    fun sceneFor(weatherCode: Int, isDay: Boolean, nowEpochSec: Long, sunsetEpochSec: Long?): WeatherScene = when {
        isRain(weatherCode) -> WeatherScene.RAIN
        !isDay -> WeatherScene.NIGHT
        sunsetEpochSec != null && nowEpochSec in (sunsetEpochSec - DUSK_WINDOW_SEC) until sunsetEpochSec -> WeatherScene.DUSK
        else -> WeatherScene.DAY
    }

    fun isRain(code: Int): Boolean = code in 51..67 || code in 80..82 || code in 95..99

    /** 약 11km 단위로 줄인 좌표 — 날씨에는 이만큼이면 충분하고, 정확한 위치를 보내지 않는다 */
    fun coarse(value: Double): Double = kotlin.math.round(value * 10) / 10.0
}
