package com.stepup.android

import com.stepup.android.domain.WeatherScene
import com.stepup.android.ui.components.HomePhotos
import kotlin.random.Random
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
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
            // 날씨로 고를 수 있는 사진이 둘 이상이어야 돌아올 때 다른 한 장을 고른다
            assertTrue("$scene", HomePhotos.all.count { it.suits(scene) } >= 2)
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
                assertTrue("$scene", HomePhotos.all[picked].suits(scene))
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

    @Test
    fun rainNeverPicksOrLabelsTheSnowPhoto() {
        val snow = HomePhotos.all.indexOfFirst { it.key == "seoul-snow" }
        assertFalse(HomePhotos.all[snow].suits(WeatherScene.RAIN))
        // 눈 사진을 보고 있을 때 비가 오면 다른 사진으로 바꾼다
        assertNotEquals(snow, HomePhotos.forWeather(WeatherScene.RAIN, snow))
        val random = Random(11)
        HomePhotos.all.indices.forEach { index ->
            repeat(20) { assertNotEquals(snow, HomePhotos.forWeather(WeatherScene.RAIN, index, random)) }
        }
        // 화살표로는 그대로 볼 수 있다
        assertEquals(snow, HomePhotos.next(snow - 1))
    }
}
