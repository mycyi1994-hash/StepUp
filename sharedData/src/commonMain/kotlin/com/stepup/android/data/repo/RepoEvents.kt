package com.stepup.android.data.repo

/**
 * 저장소가 남기는 사용 기록(안드로이드 core/Analytics 의 저장소 쪽 자리, 2026-11-03 iOS C5) — 앱이 [sink] 를 채운다.
 * 안드로이드는 Firebase Analytics 로, iOS 는 붙이기 전까지 아무것도 남기지 않는다.
 */
interface RepoEventSink {
    fun crewJoined(requested: Boolean) {}
    fun crewCreated() {}
    fun postWritten(category: String) {}
    fun flashJoined() {}
    fun partyStarted(size: Int) {}
}

object RepoEvents {
    var sink: RepoEventSink = object : RepoEventSink {}
}

/** 이 폰이 지금 뛰는 러닝(안드로이드 WalkSessionService · iOS 러닝 세션) — 모임 방에 위치를 보낼지 정한다 */
data class GroupRunState(val active: Boolean, val paused: Boolean, val inGroup: Boolean) {
    companion object {
        val NONE = GroupRunState(active = false, paused = false, inGroup = false)
    }
}
