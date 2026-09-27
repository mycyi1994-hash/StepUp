package com.stepup.android.data.remote

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.JsonObject

/**
 * 앱 공지 한 줄 — 서버 표 `public.announcements`(`supabase/migrations/0043_announcements.sql`).
 *
 * 제목 · 본문은 언어 코드별 글(`{"ko": "...", "en": "..."}`)로 온다. 서버는 공개했고 게시 시각이 지난 글만
 * 준다 — 내리거나 비공개로 바꾼 글은 목록에서 빠지고, 하나만 물으면 빈 목록이 온다.
 */
@Serializable
data class AnnouncementRow(
    val id: Long,
    val title: JsonObject = JsonObject(emptyMap()),
    val body: JsonObject = JsonObject(emptyMap()),
    @SerialName("published_at") val publishedAt: String,
    val action: String? = null,
)

/** 공지를 어디서 받는가 — 앱은 서버, 테스트는 흉내 낸 것 */
interface AnnouncementSource {
    /** 지금 보이는 공지, 최근 게시 먼저 */
    suspend fun list(): ServerResult<List<AnnouncementRow>>

    /** 공지 하나. 서버가 주지 않으면(내렸거나 비공개) Ok(null) */
    suspend fun one(id: Long): ServerResult<AnnouncementRow?>
}

/**
 * 공지는 로그인 없이도 읽는 공개 안내라 익명 키로 읽는다. 쓰기는 앱에 없다 — 운영자가 대시보드에서 올린다.
 * 서버 주소가 없거나 닿지 못하면 Retry, 표가 아직 없는 서버(배포 전)는 Rejected 가 온다 — 어느 쪽도 "공지 없음"이 아니다.
 */
class AnnouncementApi(private val server: StepUpServer) : AnnouncementSource {

    override suspend fun list(): ServerResult<List<AnnouncementRow>> =
        server.anonGet("${server.restUrl}/announcements?select=$COLUMNS&order=published_at.desc,id.desc&limit=$LIMIT")
            .mapBody { serverJson.decodeFromString<List<AnnouncementRow>>(it) }

    override suspend fun one(id: Long): ServerResult<AnnouncementRow?> =
        when (
            val result = server.anonGet("${server.restUrl}/announcements?select=$COLUMNS&id=eq.$id")
                .mapBody { serverJson.decodeFromString<List<AnnouncementRow>>(it) }
        ) {
            is ServerResult.Ok -> ServerResult.Ok(result.value.firstOrNull())
            is ServerResult.Rejected -> result
            is ServerResult.Retry -> result
            is ServerResult.SignInRequired -> result
        }

    private companion object {
        const val COLUMNS = "id,title,body,published_at,action"
        const val LIMIT = 50
    }
}
