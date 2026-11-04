package com.stepup.android.core

import android.content.Intent
import com.stepup.android.push.PushService
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

/**
 * 크루 초대 링크 — `https://stepupcrew.com/c/<크루 id>`.
 *
 * 카톡 등에 보낸 링크를 누르면, 앱이 있으면 앱이 바로 열리고(App Links,
 * `web/.well-known/assetlinks.json`), 없으면 같은 주소의 웹페이지가 설치를
 * 안내한다. 앱은 링크를 받으면 그 크루 화면을 연다 — 가입은 거기서 한다.
 *
 * 로그인 전에 링크로 들어왔으면 열 크루를 기억해 두었다가, 로그인한 뒤에 연다.
 */
object InviteLinks {

    const val HOST = "stepupcrew.com"

    private val UUID = Regex("^[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{12}$")

    private val _pendingCrew = MutableStateFlow<String?>(null)

    /** 열어야 할 크루. 화면이 열고 나면 [consume] 한다. */
    val pendingCrew: StateFlow<String?> = _pendingCrew

    /**
     * [pendingCrew] 를 연 뒤 그 크루의 가입 신청 목록까지 열까 — 크루장이 받은 가입 신청 알림(`crew/<id>/requests`, 서버 0086).
     * [pendingCrew] 보다 먼저 정해 두고 [consume] 이 끈다.
     */
    @Volatile
    var pendingCrewRequests: Boolean = false
        private set

    private val _pendingChat = MutableStateFlow<String?>(null)

    /** 열어야 할 크루 채팅 방(크루 id). 화면이 열고 나면 [consumeChat] 한다. */
    val pendingChat: StateFlow<String?> = _pendingChat

    /**
     * 푸시 알림이 가리키는 글 · 번개 · 대기실(서버 0087 · 0088) — 크루 화면이 아니라 그 자리로 간다. 로그인 전에 눌렀으면
     * [pendingCrew] 처럼 들고 있다가 로그인한 뒤 앱 화면이 연다. 화면이 열고 나면 [consumePush] 한다.
     */
    sealed interface PushTarget {
        /** 댓글 · 답글 알림 — 그 글(`post/<id>`) */
        data class Post(val postId: Long) : PushTarget

        /** 번개러닝 알림 — 그 번개 글(`flash/<id>`) */
        data class Flash(val postId: Long) : PushTarget

        /** 크루 대기실이 열렸다는 알림 — 그 방(`room/<크루 id>/<방 번호>`) */
        data class Room(val crewId: String, val partyId: Long) : PushTarget
    }

    private val _pendingPush = MutableStateFlow<PushTarget?>(null)

    /** 열어야 할 글 · 번개 · 대기실. 화면이 열고 나면 [consumePush] 한다. */
    val pendingPush: StateFlow<PushTarget?> = _pendingPush

    /** 이 크루로 초대하는 링크 */
    fun crewLink(crewId: String): String = "https://$HOST/c/$crewId"

    /** 이 번개러닝으로 부르는 링크 — 앱이 있으면 그 번개 글이 열린다(App Links `/f/`), 없으면 같은 초대 페이지가 설치를 안내한다 */
    fun flashLink(postId: Long): String = "https://$HOST/f/$postId"

    /** 링크에서 번개 글 번호를 꺼낸다. 우리 주소의 `/f/<번호>` 가 아니면 null — [crewIdOf] 와 같은 까닭 */
    fun flashIdOf(url: String?): Long? {
        val uri = runCatching { java.net.URI(url ?: return null) }.getOrNull() ?: return null
        if (uri.scheme != "https") return null
        if (uri.host != HOST && uri.host != "www.$HOST") return null
        val parts = uri.path.orEmpty().trim('/').split('/')
        if (parts.size != 2 || parts[0] != "f") return null
        return idOf(parts[1])
    }

    /**
     * 링크에서 크루 id 를 꺼낸다. 우리 주소의 `/c/<uuid>` 가 아니면 null.
     *
     * 아무 주소나 받아 주면 남의 사이트 링크로 앱 화면을 조작할 수 있다.
     */
    fun crewIdOf(url: String?): String? {
        val uri = runCatching { java.net.URI(url ?: return null) }.getOrNull() ?: return null
        if (uri.scheme != "https") return null
        if (uri.host != HOST && uri.host != "www.$HOST") return null
        val parts = uri.path.orEmpty().trim('/').split('/')
        if (parts.size != 2 || parts[0] != "c") return null
        return parts[1].takeIf { UUID.matches(it) }?.lowercase()
    }

    /** 푸시 알림이 싣고 온 앱 안 링크(`crew/<id>`)에서 크루 id 를 꺼낸다. */
    fun crewIdOfPush(link: String?): String? {
        val parts = link.orEmpty().trim('/').split('/')
        if (parts.size != 2 || parts[0] != "crew") return null
        return parts[1].takeIf { UUID.matches(it) }?.lowercase()
    }

    /** 가입 신청 알림이 싣고 온 앱 안 링크(`crew/<id>/requests`)에서 크루 id 를 꺼낸다 — 크루장의 신청 목록으로 간다 */
    fun crewRequestsIdOfPush(link: String?): String? {
        val parts = link.orEmpty().trim('/').split('/')
        if (parts.size != 3 || parts[0] != "crew" || parts[2] != "requests") return null
        return parts[1].takeIf { UUID.matches(it) }?.lowercase()
    }

    /** 크루 채팅 알림이 싣고 온 앱 안 링크(`crew-chat/<id>`)에서 크루 id 를 꺼낸다. */
    fun chatIdOfPush(link: String?): String? {
        val parts = link.orEmpty().trim('/').split('/')
        if (parts.size != 2 || parts[0] != "crew-chat") return null
        return parts[1].takeIf { UUID.matches(it) }?.lowercase()
    }

    /** 글 · 번개 번호 — 1 이상 숫자만(앞자리 0 · 부호 · 너무 큰 수는 받지 않는다) */
    private fun idOf(raw: String): Long? =
        raw.takeIf { it.isNotEmpty() && it.length <= 18 && it[0] != '0' && it.all { c -> c in '0'..'9' } }?.toLongOrNull()

    /**
     * 푸시 알림이 싣고 온 앱 안 링크에서 글(`post/<id>`) · 번개(`flash/<id>`) · 대기실(`room/<크루 id>/<방 번호>`)을 꺼낸다.
     * 다른 모양이면 null — 크루(`crew/<id>`) · 신청 목록 · 채팅 링크는 아래 함수들이 그대로 읽는다.
     */
    fun pushTargetOf(link: String?): PushTarget? {
        val parts = link.orEmpty().trim('/').split('/')
        return when {
            parts.size == 2 && parts[0] == "post" -> idOf(parts[1])?.let { PushTarget.Post(it) }
            parts.size == 2 && parts[0] == "flash" -> idOf(parts[1])?.let { PushTarget.Flash(it) }
            parts.size == 3 && parts[0] == "room" && UUID.matches(parts[1]) ->
                idOf(parts[2])?.let { PushTarget.Room(parts[1].lowercase(), it) }
            else -> null
        }
    }

    /** 앱을 연 인텐트를 본다. 초대 링크(크루 · 번개)나 크루 알림이면 그 크루를, 크루 채팅 알림이면 그 방을, 글 · 번개 · 대기실 알림이면 그 자리를 연다. */
    fun handle(intent: Intent?) {
        intent ?: return
        val target = pushTargetOf(intent.getStringExtra(PushService.EXTRA_LINK)) ?: pushTargetOf(intent.getStringExtra("link"))
            // 번개러닝 공유 링크(https://stepupcrew.com/f/<번호>) — 번개 알림과 같은 자리로
            ?: flashIdOf(intent.dataString)?.let { PushTarget.Flash(it) }
        if (target != null) {
            _pendingPush.value = target
            return
        }
        val chatId = chatIdOfPush(intent.getStringExtra(PushService.EXTRA_LINK)) ?: chatIdOfPush(intent.getStringExtra("link"))
        if (chatId != null) {
            _pendingChat.value = chatId
            return
        }
        val requestsId = crewRequestsIdOfPush(intent.getStringExtra(PushService.EXTRA_LINK)) ?: crewRequestsIdOfPush(intent.getStringExtra("link"))
        if (requestsId != null) {
            pendingCrewRequests = true
            _pendingCrew.value = requestsId
            return
        }
        val crewId = crewIdOf(intent.dataString)
            // 앱이 켜져 있을 때 받은 알림은 PushService 가 이 이름으로 싣는다
            ?: crewIdOfPush(intent.getStringExtra(PushService.EXTRA_LINK))
            // 앱이 꺼져 있을 때는 안드로이드가 알림을 띄우고, 누르면 data 칸이 그대로 온다
            ?: crewIdOfPush(intent.getStringExtra("link"))
            ?: return
        pendingCrewRequests = false
        _pendingCrew.value = crewId
    }

    fun consume() {
        pendingCrewRequests = false
        _pendingCrew.value = null
    }

    fun consumeChat() {
        _pendingChat.value = null
    }

    fun consumePush() {
        _pendingPush.value = null
    }
}
