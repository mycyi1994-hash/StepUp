package com.stepup.android.ui.components

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.net.HttpURLConnection
import java.net.URL

/** 지도 타일 받기(예전 MapTiles.download 그대로 — 6초 · User-Agent · 404/403 은 다시 받지 않는다) */
actual suspend fun fetchTileBytes(url: String, userAgent: String): FetchedTile = withContext(Dispatchers.IO) {
    try {
        val connection = URL(url).openConnection() as HttpURLConnection
        try {
            connection.setRequestProperty("User-Agent", userAgent)
            connection.connectTimeout = 6_000
            connection.readTimeout = 6_000
            val code = connection.responseCode
            when {
                code == HttpURLConnection.HTTP_OK -> FetchedTile(connection.inputStream.use { it.readBytes() }, permanent = false)
                code == HttpURLConnection.HTTP_NOT_FOUND || code == HttpURLConnection.HTTP_FORBIDDEN -> FetchedTile(null, permanent = true)
                else -> FetchedTile(null, permanent = false)
            }
        } finally {
            connection.disconnect()
        }
    } catch (_: java.io.IOException) {
        FetchedTile(null, permanent = false)
    }
}
