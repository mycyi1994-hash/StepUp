package com.stepup.android.data.remote

import io.ktor.client.HttpClient
import io.ktor.client.engine.darwin.Darwin
import io.ktor.client.plugins.HttpTimeout
import io.ktor.client.request.get
import io.ktor.client.request.headers
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.client.statement.HttpResponse as KtorResponse
import io.ktor.client.statement.bodyAsText
import kotlinx.coroutines.CancellationException

/**
 * iOS 의 기본 HTTP — Ktor(Darwin 엔진 = NSURLSession). 안드로이드 [HttpPoster] 기본 구현(UrlConnectionPoster)과 같은 약속:
 * 응답을 못 받으면(연결 실패 · 시간 초과 · 끊김) status 0, 서버 오류(4xx · 5xx)는 본문까지 그대로 돌려준다.
 */
class KtorPoster(private val timeoutMillis: Int = 20_000) : DefaultHttpPoster {
    private val client = HttpClient(Darwin) {
        expectSuccess = false
        install(HttpTimeout) {
            connectTimeoutMillis = timeoutMillis.toLong()
            socketTimeoutMillis = timeoutMillis.toLong()
        }
    }

    override suspend fun post(url: String, body: String, headers: Map<String, String>): HttpResponse =
        send { client.post(url) { headers { headers.forEach { (k, v) -> append(k, v) } }; setBody(body) } }

    override suspend fun get(url: String, headers: Map<String, String>): HttpResponse =
        send { client.get(url) { headers { headers.forEach { (k, v) -> append(k, v) } } } }

    private suspend fun send(call: suspend () -> KtorResponse): HttpResponse = try {
        val response = call()
        HttpResponse(response.status.value, response.bodyAsText())
    } catch (e: CancellationException) {
        throw e
    } catch (e: Exception) {
        HttpResponse(0, "통신 실패: ${e.message}")
    }
}

actual fun defaultHttpPoster(timeoutMillis: Int): HttpPoster = KtorPoster(timeoutMillis)
