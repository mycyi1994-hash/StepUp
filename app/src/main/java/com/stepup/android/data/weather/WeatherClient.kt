package com.stepup.android.data.weather

import com.stepup.android.data.remote.HttpPoster
import com.stepup.android.domain.WeatherScene
import com.stepup.android.domain.WeatherScenes
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

/**
 * Open-Meteo(키 없는 공개 날씨 API)에서 지금 날씨를 받아 풍경으로 바꾼다.
 * 보내는 것은 소수 한 자리로 줄인 위도 · 경도뿐이다. 실패하면 null — 홈은 고른 풍경 그대로.
 */
class WeatherClient(private val http: HttpPoster, private val baseUrl: String = BASE_URL) {

    suspend fun current(lat: Double, lng: Double): WeatherScene? {
        val url = "$baseUrl?latitude=${WeatherScenes.coarse(lat)}&longitude=${WeatherScenes.coarse(lng)}" +
            "&current=weather_code,is_day&daily=sunset&timezone=auto&forecast_days=1&timeformat=unixtime"
        val response = http.get(url, emptyMap())
        if (response.status !in 200..299) return null
        return parse(response.body)
    }

    @Serializable
    private data class Forecast(val current: Current? = null, val daily: Daily? = null)

    @Serializable
    private data class Current(
        val time: Long = 0,
        @SerialName("weather_code") val weatherCode: Int? = null,
        @SerialName("is_day") val isDay: Int? = null,
    )

    @Serializable
    private data class Daily(val sunset: List<Long> = emptyList())

    companion object {
        const val BASE_URL = "https://api.open-meteo.com/v1/forecast"
        private val json = Json { ignoreUnknownKeys = true }

        fun parse(body: String): WeatherScene? = runCatching {
            val forecast = json.decodeFromString(Forecast.serializer(), body)
            val current = forecast.current ?: return null
            val code = current.weatherCode ?: return null
            val isDay = current.isDay ?: return null
            WeatherScenes.sceneFor(code, isDay == 1, current.time, forecast.daily?.sunset?.firstOrNull())
        }.getOrNull()
    }
}
