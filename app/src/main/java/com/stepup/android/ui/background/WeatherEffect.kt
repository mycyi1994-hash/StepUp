package com.stepup.android.ui.background

import android.provider.Settings
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.stepup.android.data.weather.WeatherBackground
import com.stepup.android.ui.experience.LocalMotion
import kotlin.math.floor
import kotlin.math.roundToInt
import kotlin.math.sin

/*
 * 날씨 효과 레이어 — 사진 위에 얹는 비 · 눈 · 미세먼지(맑음 = 효과 없음). UI 최종 통합안(2026-10-25 compose/WeatherEffect.kt).
 * 사진은 장소 · 시간대별 1장(RunBackground), 날씨는 사진을 바꾸지 않고 이 레이어만 위에 얹는다. 글자 · 버튼은 이 레이어 위에 그린다.
 * 모양 · 양 · 속도는 시안 영상(02_영상 「장소·시간·날씨별 배경」)과 같다.
 *
 * 날씨 값은 앱이 이미 받는 것만 쓴다 — "날씨에 맞춰 홈 풍경 바꾸기"(설정 › 경험)가 켜져 있을 때 받는 Open-Meteo 날씨 코드(WMO).
 * 미세먼지 자료는 앱이 받지 않아 지금은 켜지지 않는다([WeatherSource.from] 은 기상청 · 에어코리아 값이 생기면 쓴다).
 */

enum class Weather { CLEAR, RAIN, SNOW, DUST }

object WeatherSource {
    /**
     * 기상청 초단기실황 강수형태(PTY) + 에어코리아 미세먼지 → 효과
     * PTY: 0 없음 · 1 비 · 2 비/눈 · 3 눈 · 4 소나기 · 5 빗방울 · 6 빗방울눈날림 · 7 눈날림
     * 미세먼지 나쁨: PM10 81 이상 또는 PM2.5 36 이상(비 · 눈이 오면 비 · 눈이 먼저)
     */
    fun from(pty: Int, pm10: Int?, pm25: Int?): Weather = when (pty) {
        1, 2, 4, 5, 6 -> Weather.RAIN
        3, 7 -> Weather.SNOW
        else -> if ((pm10 ?: 0) >= 81 || (pm25 ?: 0) >= 36) Weather.DUST else Weather.CLEAR
    }

    /** WMO 날씨 코드(Open-Meteo weather_code) → 효과. 이슬비 · 비 · 소나기 · 뇌우 = 비, 눈 · 눈 소나기 = 눈 */
    fun fromWmo(code: Int?): Weather = when (code) {
        null -> Weather.CLEAR
        in 51..67, in 80..82, in 95..99 -> Weather.RAIN
        in 71..77, 85, 86 -> Weather.SNOW
        else -> Weather.CLEAR
    }
}

/** 지금 날씨 효과 — 앱이 받아 둔 날씨 코드에서. 날씨를 받지 않았으면 맑음(효과 없음) */
@Composable
fun currentWeather(): Weather {
    val code by WeatherBackground.weatherCode.collectAsStateWithLifecycle()
    return WeatherSource.fromWmo(code)
}

/** 사진 위에 얹는 날씨 효과. 동작 줄이기 · 시스템 '애니메이션 끄기'면 멈춘 한 장면으로 그린다 */
@Composable
fun WeatherLayer(weather: Weather, modifier: Modifier = Modifier) {
    if (weather == Weather.CLEAR) return
    val ctx = LocalContext.current
    val reduced = LocalMotion.current.reduced
    val still = remember(reduced) {
        reduced || Settings.Global.getFloat(ctx.contentResolver, Settings.Global.ANIMATOR_DURATION_SCALE, 1f) == 0f
    }
    val t by produceState(1.7f, weather, still) {
        if (still) return@produceState
        val start = withFrameNanosCompat()
        while (true) value = (withFrameNanosCompat() - start) / 1_000_000_000f
    }
    Canvas(modifier.fillMaxSize()) {
        when (weather) {
            Weather.RAIN -> drawRain(t)
            Weather.SNOW -> drawSnow(t)
            Weather.DUST -> drawDust(t)
            Weather.CLEAR -> Unit
        }
    }
}

private suspend fun withFrameNanosCompat(): Long = androidx.compose.runtime.withFrameNanos { it }

// 같은 i 면 늘 같은 값(시안과 같은 식)
private fun rnd(i: Int, k: Int): Float {
    val x = sin(i * 127.1 + k * 311.7) * 43758.5453
    return (x - floor(x)).toFloat()
}

private fun DrawScope.unit() = size.width / 361f // 시안 카드 폭 361 기준 배율
private fun DrawScope.count(base: Int) = (base * (size.height / unit()) / 340f).roundToInt()
private fun rgba(r: Int, g: Int, b: Int, a: Float) = Color(r, g, b, (a.coerceIn(0f, 1f) * 255).roundToInt())

/** 비: 살짝 어둡고 차가운 막 + 3겹 깊이의 사선 빗줄기(기울기 12°) */
private fun DrawScope.drawRain(t: Float) {
    val s = unit(); val w = size.width; val h = size.height
    drawRect(rgba(8, 14, 28, .16f))
    drawRect(Brush.verticalGradient(0f to rgba(170, 180, 200, .12f), 1f to Color.Transparent, startY = 0f, endY = h * .5f))
    for (i in 0 until count(230)) {
        val z = rnd(i, 1); val len = (10 + z * 28) * s; val span = h + len + 40 * s
        val sp = (1.3f + z * 1.1f) * h
        val y = ((rnd(i, 3) * span + t * sp) % span) - len
        val x = rnd(i, 2) * (w + 60 * s) - 30 * s + (h - y) * .21f - h * .105f
        drawLine(rgba(214, 226, 248, .2f + z * .42f), Offset(x, y), Offset(x - len * .21f, y + len), strokeWidth = (.55f + z * .95f) * s, cap = StrokeCap.Round)
    }
}

/** 눈: 아주 옅은 흰 막 + 3겹 눈송이(가까울수록 크고 빠르고 좌우로 흔들림) */
private fun DrawScope.drawSnow(t: Float) {
    val s = unit(); val w = size.width; val h = size.height
    drawRect(rgba(225, 232, 244, .07f))
    for (i in 0 until count(140)) {
        val z = rnd(i, 1); val r = (.7f + z * z * 3f) * s; val span = h + 4 * r
        val sp = (14 + z * 46) * s
        val y = ((rnd(i, 3) * span + t * sp) % span) - 2 * r
        val x = rnd(i, 2) * w + sin(t * (.5f + z * .7f) + i) * (5 + z * 10) * s
        val al = .45f + z * .5f
        if (r > 2.2f * s) {
            drawCircle(Brush.radialGradient(listOf(rgba(255, 255, 255, al), Color.Transparent), center = Offset(x, y), radius = r * 1.6f), r * 1.6f, Offset(x, y))
        } else {
            drawCircle(rgba(255, 255, 255, al), r, Offset(x, y))
        }
    }
}

/** 미세먼지: 누런 회색 연무(먼 곳 = 위쪽이 더 진함) + 천천히 흘러가는 뿌연 덩어리 + 아주 작은 먼지 알갱이 */
private fun DrawScope.drawDust(t: Float) {
    val s = unit(); val w = size.width; val h = size.height
    drawRect(Brush.verticalGradient(0f to rgba(196, 182, 152, .46f), .55f to rgba(184, 172, 146, .30f), 1f to rgba(170, 160, 138, .20f)))
    for (i in 0 until 7) {
        val rr = (110 + rnd(i, 4) * 120) * s; val span = w + 2 * rr
        val x = ((rnd(i, 6) * span + t * (6 + rnd(i, 5) * 10) * s) % span) - rr; val y = rnd(i, 7) * h * .8f
        drawCircle(Brush.radialGradient(listOf(rgba(206, 192, 160, .10f + rnd(i, 8) * .08f), Color.Transparent), center = Offset(x, y), radius = rr), rr, Offset(x, y))
    }
    for (i in 0 until count(90)) {
        val z = rnd(i, 1); val r = (.5f + z * .8f) * s; val span = w + 10 * s
        val x = ((rnd(i, 2) * span + t * (5 + z * 12) * s) % span) - 5 * s
        val y = rnd(i, 3) * h + sin(t * .8f + i * 2) * 4 * s
        drawCircle(rgba(232, 222, 196, .22f + z * .3f), r, Offset(x, y))
    }
}
