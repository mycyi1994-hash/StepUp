package com.stepup.android

import com.stepup.android.domain.WeatherScene
import com.stepup.android.ui.components.HomePhotos
import kotlin.random.Random
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class HomePhotosTest {
    @Test
    fun thirtyDistinctPhotosCoverEveryWeather() {
        assertEquals(30, HomePhotos.all.size)
        assertEquals(30, HomePhotos.all.map { it.key }.toSet().size)
        assertEquals(30, HomePhotos.all.map { it.res }.toSet().size)
        WeatherScene.entries.forEach { scene ->
            assertTrue("$scene", HomePhotos.all.count { it.mood == scene } >= 2)
        }
    }

    @Test
    fun arrowsWalkThroughEveryPhotoAndWrap() {
        var index = 0
        val seen = mutableSetOf<Int>()
        repeat(HomePhotos.all.size) { seen += index; index = HomePhotos.next(index) }
        assertEquals(0, index)
        assertEquals(HomePhotos.all.size, seen.size)
        assertEquals(HomePhotos.all.lastIndex, HomePhotos.previous(0))
        assertEquals(0, HomePhotos.previous(1))
    }

    @Test
    fun shuffleNeverRepeatsAndKeepsTheWeather() {
        val random = Random(7)
        repeat(200) {
            val current = random.nextInt(HomePhotos.all.size)
            assertNotEquals(current, HomePhotos.shuffle(current, random = random))
            WeatherScene.entries.forEach { scene ->
                val picked = HomePhotos.shuffle(current, scene, random)
                assertNotEquals(current, picked)
                assertEquals(scene, HomePhotos.all[picked].mood)
            }
        }
    }

    @Test
    fun weatherKeepsAMatchingPhotoAndReplacesOthers() {
        val night = HomePhotos.all.indexOfFirst { it.mood == WeatherScene.NIGHT }
        assertEquals(night, HomePhotos.forWeather(WeatherScene.NIGHT, night))
        val day = HomePhotos.all.indexOfFirst { it.mood == WeatherScene.DAY }
        assertEquals(WeatherScene.RAIN, HomePhotos.all[HomePhotos.forWeather(WeatherScene.RAIN, day)].mood)
    }
}
