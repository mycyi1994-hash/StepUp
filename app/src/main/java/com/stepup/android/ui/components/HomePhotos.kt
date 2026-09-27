package com.stepup.android.ui.components

import androidx.annotation.DrawableRes
import com.stepup.android.R
import com.stepup.android.domain.WeatherScene
import kotlin.random.Random

/**
 * 홈 바탕 사진 한 장 — [mood] 는 날씨 풍경을 켰을 때 고르는 기준(배경음도 따른다).
 * [weatherMatch] 가 false 면 날씨로는 고르지 않고 화살표로만 본다 — 그 날씨라고 적으면 틀린 사진이다.
 */
data class HomePhoto(
    val key: String,
    @DrawableRes val res: Int,
    val mood: WeatherScene,
    val weatherMatch: Boolean = true,
) {
    /** 날씨 풍경에서 [scene] 날씨의 사진으로 고르고 "지금 날씨에 맞춘 풍경"이라 적어도 되는가 */
    fun suits(scene: WeatherScene?): Boolean = weatherMatch && scene == mood
}

/**
 * 홈 바탕 사진 30장 — 한국 풍경(2026-09-27 사용자 결정). 모두 CC0(출처 표시 없이 쓸 수 있다).
 * 원본 주소와 작가는 design/home-photos/README.md. 화살표는 이 순서로 넘긴다(낮 → 노을 → 밤 → 흐림·눈).
 */
object HomePhotos {
    val all: List<HomePhoto> = listOf(
        HomePhoto("hangang-bamseom", R.drawable.home_bg_01, WeatherScene.DAY),
        HomePhoto("hangang-skyline", R.drawable.home_bg_02, WeatherScene.DAY),
        HomePhoto("bukhansan-winter", R.drawable.home_bg_03, WeatherScene.DAY),
        HomePhoto("hwaseong", R.drawable.home_bg_04, WeatherScene.DAY),
        HomePhoto("bukhansan-spring", R.drawable.home_bg_05, WeatherScene.DAY),
        HomePhoto("gyeongbokgung", R.drawable.home_bg_06, WeatherScene.DAY),
        HomePhoto("bukhansan-summer", R.drawable.home_bg_07, WeatherScene.DAY),
        HomePhoto("gamcheon", R.drawable.home_bg_08, WeatherScene.DAY),
        HomePhoto("seongsan", R.drawable.home_bg_09, WeatherScene.DAY),
        HomePhoto("sanbangsan", R.drawable.home_bg_10, WeatherScene.DAY),
        HomePhoto("seoul-forest-blossom", R.drawable.home_bg_11, WeatherScene.DAY),
        HomePhoto("gapyeong-bridge", R.drawable.home_bg_12, WeatherScene.DAY),
        HomePhoto("seoul-sunset", R.drawable.home_bg_13, WeatherScene.DUSK),
        HomePhoto("seocho-hangang-dusk", R.drawable.home_bg_14, WeatherScene.DUSK),
        HomePhoto("jamsil-bridge-dusk", R.drawable.home_bg_15, WeatherScene.DUSK),
        HomePhoto("songpa-sunset", R.drawable.home_bg_16, WeatherScene.DUSK),
        HomePhoto("lotte-tower-purple", R.drawable.home_bg_17, WeatherScene.DUSK),
        HomePhoto("gapyeong-sunset", R.drawable.home_bg_18, WeatherScene.DUSK),
        HomePhoto("olympic-daero-lights", R.drawable.home_bg_19, WeatherScene.NIGHT),
        HomePhoto("seoul-night-tower", R.drawable.home_bg_20, WeatherScene.NIGHT),
        HomePhoto("banpo-fountain", R.drawable.home_bg_21, WeatherScene.NIGHT),
        HomePhoto("busan-harbor-night", R.drawable.home_bg_22, WeatherScene.NIGHT),
        HomePhoto("busan-skyline-night", R.drawable.home_bg_23, WeatherScene.NIGHT),
        HomePhoto("cheonggyecheon-falls", R.drawable.home_bg_24, WeatherScene.NIGHT),
        HomePhoto("seoul-night-park", R.drawable.home_bg_25, WeatherScene.NIGHT),
        HomePhoto("seoul-gate-night", R.drawable.home_bg_26, WeatherScene.NIGHT),
        HomePhoto("hangang-clouds", R.drawable.home_bg_27, WeatherScene.RAIN),
        // 눈 풍경 — 비 오는 날 "비"로 고르지 않는다. 날씨 분류(WeatherScenes)에 눈이 없어 화살표로만 본다.
        HomePhoto("seoul-snow", R.drawable.home_bg_28, WeatherScene.RAIN, weatherMatch = false),
        HomePhoto("jamsil-bridge-haze", R.drawable.home_bg_29, WeatherScene.RAIN),
        HomePhoto("jeju-lighthouse-grey", R.drawable.home_bg_30, WeatherScene.RAIN),
    )

    fun initial(random: Random = Random.Default): Int = random.nextInt(all.size)

    fun next(index: Int): Int = (index.coerceIn(all.indices) + 1) % all.size

    fun previous(index: Int): Int = (index.coerceIn(all.indices) + all.size - 1) % all.size

    /** 다른 탭에서 돌아올 때 — 지금과 다른 한 장. [mood] 가 있으면 그 날씨의 사진 안에서 고른다. */
    fun shuffle(index: Int, mood: WeatherScene? = null, random: Random = Random.Default): Int {
        val pool = all.indices.filter { it != index && (mood == null || all[it].suits(mood)) }
        return if (pool.isEmpty()) index else pool[random.nextInt(pool.size)]
    }

    /** 날씨가 정해지면 — 이미 그 날씨의 사진이면 그대로, 아니면 그 날씨의 사진 하나로 */
    fun forWeather(scene: WeatherScene, index: Int, random: Random = Random.Default): Int =
        if (all.getOrNull(index)?.suits(scene) == true) index else shuffle(index, scene, random)
}
