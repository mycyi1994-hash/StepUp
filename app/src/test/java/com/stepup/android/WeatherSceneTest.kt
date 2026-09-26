package com.stepup.android

import com.stepup.android.data.weather.WeatherClient
import com.stepup.android.domain.WeatherScene
import com.stepup.android.domain.WeatherScenes
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class WeatherSceneTest {
    private val sunset = 1_790_000_000L

    @Test fun rainWinsOverTimeOfDay() {
        assertEquals(WeatherScene.RAIN, WeatherScenes.sceneFor(61, isDay = true, nowEpochSec = sunset - 10_000, sunsetEpochSec = sunset))
        assertEquals(WeatherScene.RAIN, WeatherScenes.sceneFor(95, isDay = false, nowEpochSec = sunset + 10_000, sunsetEpochSec = sunset))
        assertEquals(WeatherScene.RAIN, WeatherScenes.sceneFor(81, isDay = true, nowEpochSec = 0, sunsetEpochSec = null))
    }

    @Test fun dayDuskNight() {
        assertEquals(WeatherScene.DAY, WeatherScenes.sceneFor(0, true, sunset - 2 * 3600, sunset))
        assertEquals(WeatherScene.DUSK, WeatherScenes.sceneFor(2, true, sunset - 1800, sunset))
        assertEquals(WeatherScene.NIGHT, WeatherScenes.sceneFor(3, false, sunset + 1800, sunset))
        // Snow has no scenery of its own.
        assertEquals(WeatherScene.DAY, WeatherScenes.sceneFor(73, true, sunset - 5 * 3600, sunset))
    }

    @Test fun coordinatesAreCoarsened() {
        assertEquals(37.5, WeatherScenes.coarse(37.5321), 0.0)
        assertEquals(127.0, WeatherScenes.coarse(126.9812), 0.0)
    }

    @Test fun parsesOpenMeteoUnixResponse() {
        val body = """{"latitude":37.5,"current":{"time":${sunset - 1200},"interval":900,"weather_code":1,"is_day":1},
            |"daily":{"time":[1789948800],"sunset":[$sunset]}}""".trimMargin()
        assertEquals(WeatherScene.DUSK, WeatherClient.parse(body))
        assertNull(WeatherClient.parse("""{"current":{"time":1}}"""))
        assertNull(WeatherClient.parse("not json"))
    }
}
