package com.stepup.android.data.remote

/**
 * 푸시 알림 받을 폰을 서버에 적는다 (`supabase/migrations/0013_push.sql`).
 *
 * 알림을 보내는 일은 서버가 한다. 앱은 "이 폰으로 보내 달라"만 적는다.
 */
class PushApi(private val server: StepUpServer) {

    /** 이 폰의 FCM 토큰을 지금 로그인한 사람에게 붙인다. */
    suspend fun register(token: String, locale: String): ServerResult<Unit> =
        server.authed { accessToken ->
            server.http.post(
                "${server.restUrl}/rpc/push_register",
                jsonBody {
                    put("p_token", token)
                    put("p_locale", locale)
                },
                server.headers(accessToken),
            )
        }.mapBody { }

    /** 받을 푸시 종류를 적는다. 보내는 쪽이 보내기 전에 본다. */
    suspend fun setPrefs(push: Boolean, goalReminder: Boolean, partyInvite: Boolean, eventNews: Boolean): ServerResult<Unit> =
        server.authed { accessToken ->
            server.http.post(
                "${server.restUrl}/rpc/notify_prefs_set",
                jsonBody {
                    put("p_push", push)
                    put("p_goal_reminder", goalReminder)
                    put("p_party_invite", partyInvite)
                    put("p_event_news", eventNews)
                },
                server.headers(accessToken),
            )
        }.mapBody { }
}
