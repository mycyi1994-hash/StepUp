package com.stepup.android.core

import android.net.Uri

/**
 * 바깥으로 나가는 주소를 한 번 거른다.
 *
 * 서버가 걸러서 보내지만(0009_running_feed.sql 의 is_web_url), 앱에서도 다시
 * 본다. 주소는 서버 표에서만 오는 것이 아니다 — 운영자가 손으로 넣기도 하고,
 * 나중에 다른 경로가 생기기도 한다. 한 군데만 막아 두면 그 한 군데를 우회하는
 * 길이 생겼을 때 조용히 뚫린다.
 *
 * 막는 것:
 *   - javascript: · data: · file: — 열리는 순간 위험한 스킴
 *   - intent: · market: 같은 앱 실행 스킴 — 우리가 의도한 이동이 아니다
 *   - host 가 없는 주소
 *
 * 순수 함수라 기기 없이도 검사할 수 있다. Uri 파싱만 안드로이드 것을 쓰므로
 * [looksSafe] 는 문자열만 보고 판단한다.
 */
object SafeUrl {

    private val ALLOWED_SCHEMES = setOf("http", "https")

    /**
     * 열어도 되는 주소인가.
     *
     * Uri 클래스를 쓰지 않고 문자열로 본다 — 기기 없이 검사할 수 있어야
     * 이 규칙이 실제로 지켜지는지 확인할 수 있다.
     */
    fun looksSafe(url: String?): Boolean {
        val raw = url?.trim().orEmpty()
        if (raw.isEmpty()) return false
        // 스킴 앞뒤에 공백·제어문자를 끼워 검사를 피하는 수법을 막는다.
        if (raw.any { it.isWhitespace() || it.isISOControl() }) return false

        val scheme = raw.substringBefore("://", missingDelimiterValue = "").lowercase()
        if (scheme !in ALLOWED_SCHEMES) return false

        val rest = raw.substringAfter("://")
        val host = rest.substringBefore('/').substringBefore('?').substringBefore('#')
            .substringAfterLast('@')     // user:pass@ 는 host 가 아니다
            .substringBefore(':')
        if (host.isEmpty() || '.' !in host) return false
        return true
    }

    /** https 를 앞세운다. 같은 주소가 둘 다 있으면 https 를 쓴다. */
    fun preferHttps(url: String): String =
        if (url.startsWith("http://")) "https://" + url.removePrefix("http://") else url

    /** 화면에 보여 줄 도메인 — "news.sbs.co.kr" */
    fun hostOf(url: String?): String {
        if (!looksSafe(url)) return ""
        val host = runCatching { Uri.parse(url).host }.getOrNull().orEmpty().lowercase()
        return host.removePrefix("www.")
    }
}
