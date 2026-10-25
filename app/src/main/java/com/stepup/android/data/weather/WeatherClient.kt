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

    suspend fun current(lat: Double, lng: Double): WeatherScene? = report(lat, lng)?.scene

    /** 풍경과 WMO 날씨 코드를 한 번에 — 코드는 사진 위 날씨 효과(비 · 눈)를 고르는 데 쓴다. 요청은 [current] 와 같은 하나다 */
    suspend fun report(lat: Double, lng: Double): Report? {
        val url = "$baseUrl?latitude=${WeatherScenes.coarse(lat)}&longitude=${WeatherScenes.coarse(lng)}" +
            "&current=weather_code,is_day&daily=sunset&timezone=auto&forecast_days=1&timeformat=unixtime"
        val response = http.get(url, emptyMap())
        if (response.status !in 200..299) return null
        val scene = parse(response.body) ?: return null
        return Report(scene, parseCode(response.body))
    }

    /** 지금 날씨 — 풍경과 WMO 코드(모르면 null) */
    data class Report(val scene: WeatherScene, val weatherCode: Int?)

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

        /** WMO 날씨 코드만 — 모르면 null */
        fun parseCode(body: String): Int? = runCatching {
            json.decodeFromString(Forecast.serializer(), body).current?.weatherCode
        }.getOrNull()

        fun parse(body: String): WeatherScene? = runCatching {
            val forecast = json.decodeFromString(Forecast.serializer(), body)
            val current = forecast.current ?: return null
            val code = current.weatherCode ?: return null
            val isDay = current.isDay ?: return null
            WeatherScenes.sceneFor(code, isDay == 1, current.time, forecast.daily?.sunset?.firstOrNull())
        }.getOrNull()
    }
}
