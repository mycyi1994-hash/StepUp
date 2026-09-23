package com.stepup.android.core

import android.content.Context
import android.os.Bundle
import com.google.firebase.analytics.FirebaseAnalytics

/**
 * 성장 지표를 재는 이벤트 — Firebase Analytics 로 보낸다.
 *
 * 성장 기획서의 퍼널(가입 → 첫 러닝 → 크루 가입 → 4주 잔존)과 주간 지표(WRU,
 * 활동 크루, 초대)를 재는 데 필요한 일만 적는다. 무엇을 눌렀는지 하나하나
 * 따라다니지 않는다. 이름·글 내용·위치 같은 개인 정보는 보내지 않는다.
 *
 * [init] 전에는 아무것도 하지 않는다. 단위 테스트는 앱 없이 저장소를 돌리는데,
 * 그때 Firebase 를 부르면 죽는다.
 */
object Analytics {

    @Volatile
    private var firebase: FirebaseAnalytics? = null

    fun init(context: Context) {
        firebase = runCatching { FirebaseAnalytics.getInstance(context.applicationContext) }.getOrNull()
    }

    /** 구글 로그인 완료 — 퍼널의 첫 칸 */
    fun login() = log(FirebaseAnalytics.Event.LOGIN) { putString(FirebaseAnalytics.Param.METHOD, "google") }

    /**
     * 러닝 한 번을 마쳤다 — WRU(주간 러닝 사용자)와 "첫 러닝 전환"의 근거.
     *
     * @param partySize 같이 뛴 인원. 혼자면 1
     */
    fun runFinished(distanceKm: Double, partySize: Int) = log("run_finish") {
        putDouble("distance_km", (distanceKm * 10).toLong() / 10.0)
        putLong("party_size", partySize.toLong())
    }

    fun crewJoined(requested: Boolean) =
        log(FirebaseAnalytics.Event.JOIN_GROUP) { putString("result", if (requested) "requested" else "joined") }

    fun crewCreated() = log("crew_create")

    fun postWritten(category: String) = log("post_create") { putString("category", category) }

    fun flashJoined() = log("flash_join")

    fun partyStarted(size: Int) = log("party_start") { putLong("party_size", size.toLong()) }

    fun courseShared() = log(FirebaseAnalytics.Event.SHARE) { putString(FirebaseAnalytics.Param.CONTENT_TYPE, "course") }

    private fun log(event: String, params: Bundle.() -> Unit = {}) {
        val target = firebase ?: return
        runCatching { target.logEvent(event, Bundle().apply(params)) }
    }
}
