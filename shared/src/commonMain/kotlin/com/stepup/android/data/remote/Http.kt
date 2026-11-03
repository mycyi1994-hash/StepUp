package com.stepup.android.data.remote

/**
 * HTTP 응답.
 *
 * [status] 가 0이면 응답을 받지 못한 것이다 — 연결 실패, 시간 초과, 끊김.
 * 서버가 준 오류(4xx·5xx)와 구분해야 한다. 앞의 것은 나중에 다시 하면 되고,
 * 뒤의 것은 다시 해도 같은 답이 온다.
 */
data class HttpResponse(val status: Int, val body: String)

/**
 * 요청을 보내는 쪽.
 *
 * 인터페이스로 둔 것은 네트워크 없이 서버 응답을 흉내 내기 위해서다.
 * 토큰 갱신이나 재시도 규칙은 실기기에서 재현하기 어려운데, 틀리면 사용자가
 * 로그아웃되거나 기록을 잃는다.
 */
interface HttpPoster {
    suspend fun post(url: String, body: String, headers: Map<String, String>): HttpResponse

    suspend fun get(url: String, headers: Map<String, String>): HttpResponse
}

/** 기기의 기본 HTTP 라는 표시 — 시간 제한만 바꿔 새로 만들 때([defaultHttpPoster]) 본다(검사의 흉내 서버는 그대로 둔다) */
interface DefaultHttpPoster : HttpPoster

/** 기기마다 기본 HTTP — 안드로이드는 HttpURLConnection(예전 그대로), iOS 는 Ktor(NSURLSession) */
expect fun defaultHttpPoster(timeoutMillis: Int = 20_000): HttpPoster
