package com.stepup.android.data.remote

import java.io.IOException
import java.net.HttpURLConnection
import java.net.URL
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * [HttpURLConnection] 으로 보내는 기본 구현.
 *
 * HTTP 라이브러리를 새로 들이지 않는다. 쓰는 엔드포인트가 몇 개뿐이고,
 * 지도 타일이 이미 같은 방식으로 동작한다.
 */
class UrlConnectionPoster(
    private val timeoutMillis: Int = 20_000,
) : DefaultHttpPoster {

    override suspend fun post(url: String, body: String, headers: Map<String, String>) =
        send(url, "POST", body, headers)

    override suspend fun get(url: String, headers: Map<String, String>) =
        send(url, "GET", null, headers)

    private suspend fun send(
        url: String,
        method: String,
        body: String?,
        headers: Map<String, String>,
    ): HttpResponse = withContext(Dispatchers.IO) {
        val connection = try {
            URL(url).openConnection() as HttpURLConnection
        } catch (e: Exception) {
            return@withContext HttpResponse(0, "연결할 수 없습니다: ${e.message}")
        }

        try {
            connection.requestMethod = method
            connection.connectTimeout = timeoutMillis
            connection.readTimeout = timeoutMillis
            headers.forEach { (k, v) -> connection.setRequestProperty(k, v) }

            if (body != null) {
                connection.doOutput = true
                connection.outputStream.use { it.write(body.toByteArray()) }
            }

            val status = connection.responseCode
            // 4xx·5xx 본문은 errorStream 으로 온다. 여기서 흘리면 서버가 적어 보낸
            // 실패 이유가 사라져 "왜 안 되는지 모르는" 상태가 된다.
            val text = (if (status in 200..299) connection.inputStream else connection.errorStream)
                ?.bufferedReader()?.use { it.readText() }.orEmpty()

            HttpResponse(status, text)
        } catch (e: IOException) {
            HttpResponse(0, "통신 실패: ${e.message}")
        } catch (e: Exception) {
            HttpResponse(0, "요청 실패: ${e.message}")
        } finally {
            connection.disconnect()
        }
    }
}

/** 안드로이드 · JVM 은 예전 그대로 [UrlConnectionPoster] */
actual fun defaultHttpPoster(timeoutMillis: Int): HttpPoster = UrlConnectionPoster(timeoutMillis)
