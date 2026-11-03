package com.stepup.android.core

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

/** 크루 초대 링크 · 알림 링크(안드로이드 core/InviteLinks.kt 와 같은 규칙) */
object InviteLinks {

    const val HOST = "stepupcrew.com"

    private val UUID = Regex("^[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{12}$")

    private val _pendingCrew = MutableStateFlow<String?>(null)

    /** 열어야 할 크루. 화면이 열고 나면 [consume] 한다. */
    val pendingCrew: StateFlow<String?> = _pendingCrew

    private val _pendingChat = MutableStateFlow<String?>(null)

    /** 열어야 할 크루 채팅 방(크루 id). 화면이 열고 나면 [consumeChat] 한다. */
    val pendingChat: StateFlow<String?> = _pendingChat

    /** 이 크루로 초대하는 링크 */
    fun crewLink(crewId: String): String = "https://$HOST/c/$crewId"

    /**
     * 링크에서 크루 id 를 꺼낸다. 우리 주소의 `/c/<uuid>` 가 아니면 null.
     *
     * 아무 주소나 받아 주면 남의 사이트 링크로 앱 화면을 조작할 수 있다.
     */
    fun crewIdOf(url: String?): String? {
        // java.net.URI 와 같은 판정 — https · 우리 주소 · 경로만(물음표 · # 뒤는 뺀다)
        val text = url ?: return null
        if (!text.startsWith("https://")) return null
        val rest = text.removePrefix("https://")
        val host = rest.substringBefore('/').substringBefore('?').substringBefore('#').substringBefore(':').lowercase()
        if (host != HOST && host != "www.$HOST") return null
        val path = rest.drop(rest.substringBefore('/').length).substringBefore('?').substringBefore('#')
        val parts = path.trim('/').split('/')
        if (parts.size != 2 || parts[0] != "c") return null
        return parts[1].takeIf { UUID.matches(it) }?.lowercase()
    }

    /** 푸시 알림이 싣고 온 앱 안 링크(`crew/<id>`)에서 크루 id 를 꺼낸다. */
    fun crewIdOfPush(link: String?): String? {
        val parts = link.orEmpty().trim('/').split('/')
        if (parts.size != 2 || parts[0] != "crew") return null
        return parts[1].takeIf { UUID.matches(it) }?.lowercase()
    }

    /** 크루 채팅 알림이 싣고 온 앱 안 링크(`crew-chat/<id>`)에서 크루 id 를 꺼낸다. */
    fun chatIdOfPush(link: String?): String? {
        val parts = link.orEmpty().trim('/').split('/')
        if (parts.size != 2 || parts[0] != "crew-chat") return null
        return parts[1].takeIf { UUID.matches(it) }?.lowercase()
    }

    /** 앱을 연 주소 · 알림 링크를 본다(안드로이드 handle(Intent) 의 공용 쪽) — 초대 링크나 크루 알림이면 그 크루를, 크루 채팅 알림이면 그 방을 연다 */
    fun handle(url: String?, pushLink: String?) {
        val chatId = chatIdOfPush(pushLink)
        if (chatId != null) {
            _pendingChat.value = chatId
            return
        }
        val crewId = crewIdOf(url) ?: crewIdOfPush(pushLink) ?: return
        _pendingCrew.value = crewId
    }

    fun consume() {
        _pendingCrew.value = null
    }

    fun consumeChat() {
        _pendingChat.value = null
    }
}
