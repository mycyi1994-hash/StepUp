package com.stepup.android.ui.components

import io.ktor.client.HttpClient
import io.ktor.client.engine.darwin.Darwin
import io.ktor.client.plugins.HttpTimeout
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.client.statement.readRawBytes

private val tileClient by lazy {
    HttpClient(Darwin) {
        expectSuccess = false
        install(HttpTimeout) { connectTimeoutMillis = 6_000; socketTimeoutMillis = 6_000 }
    }
}

actual suspend fun fetchTileBytes(url: String, userAgent: String): FetchedTile = try {
    val response = tileClient.get(url) { header("User-Agent", userAgent) }
    when (response.status.value) {
        200 -> FetchedTile(response.readRawBytes(), permanent = false)
        403, 404 -> FetchedTile(null, permanent = true)
        else -> FetchedTile(null, permanent = false)
    }
} catch (e: kotlinx.coroutines.CancellationException) {
    throw e
} catch (e: Exception) {
    FetchedTile(null, permanent = false)
}
