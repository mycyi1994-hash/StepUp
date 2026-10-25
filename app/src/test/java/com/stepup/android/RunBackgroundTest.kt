package com.stepup.android

import com.stepup.android.ui.background.RunBackground
import com.stepup.android.ui.background.Spot
import com.stepup.android.ui.background.SpotKind
import com.stepup.android.ui.background.Spots
import com.stepup.android.ui.background.TimeOfDay
import com.stepup.android.ui.background.Weather
import com.stepup.android.ui.background.WeatherSource
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

/** 장소별 배경 사진 고르기(UI 최종 통합안 2026-10-25 BACKGROUND_ASSETS.md) — 고르는 순서 · 시간대 · 이름 맞추기 · 날씨 효과 */
class RunBackgroundTest {
    private val mangwon = Spot("mangwon", "망원 한강공원", SpotKind.HANGANG, 37.5552, 126.8953, 900)
    private val yeouido = Spot("yeouido", "여의도 한강공원", SpotKind.HANGANG, 37.5284, 126.9327, 1200)
    private val forest = Spot("seoulforest", "서울숲", SpotKind.PARK, 37.5444, 127.0374, 700)

    @Test fun timeBands() {
        assertEquals(TimeOfDay.DAWN, RunBackground.timeOf(5))
        assertEquals(TimeOfDay.DAY, RunBackground.timeOf(8))
        assertEquals(TimeOfDay.DUSK, RunBackground.timeOf(17))
        assertEquals(TimeOfDay.NIGHT, RunBackground.timeOf(20))
        assertEquals(TimeOfDay.NIGHT, RunBackground.timeOf(4))
    }

    @Test fun resolveOrder() {
        val have = setOf("bg_mangwon_dusk", "bg_yeouido_day", "bg_yeouido", "bg_kind_park_night", "bg_kind_hangang_dusk")
        // ① 장소 + 시간대
        assertEquals("bg_yeouido_day", RunBackground.resolve(have, yeouido, TimeOfDay.DAY).assetKey)
        // ② 장소 기본 1장 + 색보정
        val graded = RunBackground.resolve(have, yeouido, TimeOfDay.NIGHT)
        assertEquals("bg_yeouido", graded.assetKey)
        assertNotNull(graded.grade)
        // ③ 같은 종류 대표
        assertEquals("bg_kind_park_night", RunBackground.resolve(have, forest, TimeOfDay.NIGHT).assetKey)
        // ④ 기본 = 망원 저녁
        assertEquals(RunBackground.DEFAULT, RunBackground.resolve(have, forest, TimeOfDay.DAWN).assetKey)
        assertEquals(RunBackground.DEFAULT, RunBackground.resolve(have, null, TimeOfDay.DAY).assetKey)
        assertNull(RunBackground.resolve(have, null, TimeOfDay.DAY).grade)
    }

    @Test fun assetPaths() {
        assertEquals("backgrounds/mangwon/bg_mangwon_dusk.webp", RunBackground.assetPath("bg_mangwon_dusk"))
        assertEquals("backgrounds/_kind/bg_kind_park_night.webp", RunBackground.assetPath("bg_kind_park_night"))
        assertEquals("backgrounds/songdo_centralpark/bg_songdo_centralpark_day.webp", RunBackground.assetPath("bg_songdo_centralpark_day"))
    }

    @Test fun matchByName() {
        val spots = listOf(mangwon, yeouido, forest)
        assertEquals("mangwon", Spots.matchIn(spots, "망원 한강공원 2번 출입구")?.id)
        // "한강공원"만으로는 어느 장소도 아니다 — 여의도가 망원으로 잡히지 않는다
        assertEquals("yeouido", Spots.matchIn(spots, "여의도 한강공원")?.id)
        assertNull(Spots.matchIn(spots, "한강공원"))
        assertEquals("seoulforest", Spots.matchIn(spots, "서울숲 한 바퀴")?.id)
        assertNull(Spots.matchIn(spots, "우리 동네"))
        assertNull(Spots.matchIn(spots, null))
    }

    @Test fun nearestWithinRadius() {
        val spots = listOf(mangwon, yeouido)
        assertEquals("mangwon", RunBackground.nearest(spots, 37.5555, 126.8960)?.id)
        assertNull(RunBackground.nearest(spots, 37.40, 126.70))
    }

    @Test fun kindForIsStable() {
        assertEquals(RunBackground.kindFor("crew-a"), RunBackground.kindFor("crew-a"))
    }

    @Test fun weatherFromCodes() {
        assertEquals(Weather.RAIN, WeatherSource.fromWmo(61))
        assertEquals(Weather.RAIN, WeatherSource.fromWmo(95))
        assertEquals(Weather.SNOW, WeatherSource.fromWmo(73))
        assertEquals(Weather.CLEAR, WeatherSource.fromWmo(2))
        assertEquals(Weather.CLEAR, WeatherSource.fromWmo(null))
        assertEquals(Weather.DUST, WeatherSource.from(0, 90, null))
        assertEquals(Weather.RAIN, WeatherSource.from(1, 200, 100))
        assertEquals(Weather.SNOW, WeatherSource.from(3, null, null))
    }

    /** 앱에 넣은 사진이 spots.json 의 모든 장소 · 시간대 넉 장과 종류 대표 넉 장을 빠짐없이 갖췄다 */
    @Test fun bundledAssetsCoverEverySpot() {
        val root = listOf(File("src/main/assets/backgrounds"), File("app/src/main/assets/backgrounds")).first { it.isDirectory }
        val ids = Regex("\"id\":\\s*\"([a-z_]+)\"").findAll(File(root, "spots.json").readText()).map { it.groupValues[1] }.toList()
        assertEquals(40, ids.size)
        val times = listOf("dawn", "day", "dusk", "night")
        for (id in ids) for (t in times) assertTrue("$id $t", File(root, "$id/bg_${id}_$t.webp").isFile)
        for (k in SpotKind.entries.map { it.name.lowercase() }) for (t in times) assertTrue("kind $k $t", File(root, "_kind/bg_kind_${k}_$t.webp").isFile)
    }

    @Test fun frequentSpotCountsRunStarts() {
        val spots = listOf(mangwon, yeouido, forest)
        // 최근 것부터 — 여의도 둘 · 망원 하나 → 여의도
        val starts = listOf(37.5285 to 126.9330, 37.5553 to 126.8950, 37.5283 to 126.9325)
        assertEquals("yeouido", RunBackground.frequent(spots, starts)?.id)
        // 같은 수면 최근 장소
        assertEquals("mangwon", RunBackground.frequent(spots, listOf(37.5553 to 126.8950, 37.5285 to 126.9330))?.id)
        // 어느 장소 반경에도 들지 않으면 세지 않는다
        assertNull(RunBackground.frequent(spots, listOf(35.1 to 129.0)))
        assertNull(RunBackground.frequent(spots, emptyList()))
    }

    @Test fun firstTrackPointOnly() {
        val p = com.stepup.android.domain.RunTrack.first("37.5285,126.933,1000;37.6,127.0,2000")
        assertEquals(37.5285, p!!.lat, 1e-9)
        assertEquals(1000L, p.at)
        assertNull(com.stepup.android.domain.RunTrack.first(""))
        assertNull(com.stepup.android.domain.RunTrack.first("broken"))
    }
}
