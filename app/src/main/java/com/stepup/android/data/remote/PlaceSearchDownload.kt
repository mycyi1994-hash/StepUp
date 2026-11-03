package com.stepup.android.data.remote

import com.stepup.android.BuildConfig
import java.net.HttpURLConnection
import java.net.URL
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/** 안드로이드의 장소 찾기 — 공용 PlaceSearchApi 에 앱 키와 HttpURLConnection 받기를 넣는다(예전 기본값 그대로) */
fun androidPlaceSearchApi(): PlaceSearchApi = PlaceSearchApi(key = BuildConfig.MAPTILER_KEY.trim(), fetch = ::placeSearchDownload)

private val userAgent = "StepUp/${BuildConfig.VERSION_NAME} (Android; +https://stepupcrew.com)"

private suspend fun placeSearchDownload(url: String): String? = withContext(Dispatchers.IO) {
    runCatching {
        val connection = URL(url).openConnection() as HttpURLConnection
        try {
            connection.setRequestProperty("User-Agent", userAgent)
            connection.connectTimeout = 6_000
            connection.readTimeout = 6_000
            if (connection.responseCode != HttpURLConnection.HTTP_OK) null
            else connection.inputStream.use { it.readBytes().toString(Charsets.UTF_8) }
        } finally {
            connection.disconnect()
        }
    }.getOrNull()
}
