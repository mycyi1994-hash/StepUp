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

    /** 이 크루로 초대하는 링크 */
    fun crewLink(crewId: String): String = "https://$HOST/c/$crewId"

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

    /** 앱을 연 인텐트를 본다. 초대 링크나 크루 알림이면 그 크루를 연다. */
    fun handle(intent: Intent?) {
        intent ?: return
        val crewId = crewIdOf(intent.dataString)
            ?: crewIdOfPush(intent.getStringExtra(PushService.EXTRA_LINK))
            ?: return
        _pendingCrew.value = crewId
    }

    fun consume() {
        _pendingCrew.value = null
    }
}
