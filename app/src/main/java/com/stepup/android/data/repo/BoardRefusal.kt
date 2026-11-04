package com.stepup.android.data.repo

/**
 * 게시판(함께 뛰기 탭)에서 서버가 거절한 까닭 — 화면이 번역된 문구를 고르는 데 쓴다.
 *
 * 서버 함수(post_create · story_create · story_update · post_set_like · join_flash · post_delete · comment_create ·
 * comment_delete · user_block · content_report 와 그 안의 story_check)는 한국어 문장으로 거절한다. 그대로 띄우면 영어 ·
 * 일본어 · 중국어 화면에 한국어가 뜨므로 아는 문장은 여기서 갈래로 옮기고, 모르는 문장은 [OTHER](일반 안내)로 둔다.
 * 문장이 바뀌면 [OTHER] 로 떨어질 뿐 틀린 말을 하지는 않는다([MarketRefusal] 과 같은 방식).
 */
enum class BoardRefusal {
    /** 글을 너무 자주 쓴다(도배 막이) */
    POST_TOO_OFTEN,
    /** 댓글을 너무 자주 쓴다 */
    COMMENT_TOO_OFTEN,
    /** 내가 쓴 글만 지운다 */
    NOT_MY_POST_DELETE,
    /** 내가 쓴 글만 고친다 */
    NOT_MY_POST_EDIT,
    /** 내가 쓴 댓글만 지운다 */
    NOT_MY_COMMENT,
    /** 번개 정원이 찼다 — 정원은 [boardRefusalCapacity] */
    FLASH_FULL,
    /** 이미 지난 모임 */
    FLASH_PAST,
    /** 번개러닝은 앞으로의 모임 시각이 있어야 한다 */
    FLASH_NEEDS_TIME,
    /** 크루 멤버가 아니다 */
    NOT_CREW_MEMBER,
    /** 글 · 번개 · 답글을 달 댓글을 찾을 수 없다(지워졌다) */
    POST_GONE,
    /** 나 자신은 차단할 수 없다 */
    BLOCK_SELF,
    /** 장소 좌표 · 장소 · 내용 · 길이 · 게시판 종류 · 모집 조건 등 입력이 맞지 않는다 */
    INVALID_INPUT,
    /** 내용이 너무 길다 */
    TOO_LONG,
    /** 내용을 적지 않았다 */
    EMPTY,
    SIGN_IN,
    OTHER;

    companion object {
        // 서버 문장의 고정된 앞부분 → 갈래. 앞의 것부터 본다(긴 문장 · 겹치는 낱말을 먼저)
        private val PHRASES = listOf(
            "댓글을 너무 자주" to COMMENT_TOO_OFTEN,
            "글을 너무 자주" to POST_TOO_OFTEN,
            "내가 쓴 댓글만" to NOT_MY_COMMENT,
            "내가 쓴 글만 지울" to NOT_MY_POST_DELETE,
            "내가 쓴 글만 고칠" to NOT_MY_POST_EDIT,
            "정원이 찼습니다" to FLASH_FULL,
            "정원이 가득" to FLASH_FULL,
            "이미 지난 모임" to FLASH_PAST,
            "앞으로의 모임 시각" to FLASH_NEEDS_TIME,
            "멤버가 아닙니다" to NOT_CREW_MEMBER,
            "답글을 달 댓글을 찾을 수 없" to POST_GONE,
            "글을 찾을 수 없" to POST_GONE,
            "좋아요 상태가 없" to INVALID_INPUT,
            "나 자신은 차단할 수 없" to BLOCK_SELF,
            "글이 너무 깁니다" to TOO_LONG,
            "첫 줄이 너무 깁니다" to TOO_LONG,
            "장소 이름이 너무 깁니다" to TOO_LONG,
            "내용을 적어 주세요" to EMPTY,
            "좌표가 올바르지 않" to INVALID_INPUT,
            "장소를 골라 주세요" to INVALID_INPUT,
            "게시판 종류가 올바르지 않" to INVALID_INPUT,
            "room_options_invalid" to INVALID_INPUT,
            "로그인이 필요" to SIGN_IN,
        )

        /** 서버(또는 앱)가 적어 보낸 거절 문장을 갈래로 */
        fun of(reason: String): BoardRefusal =
            PHRASES.firstOrNull { (phrase, _) -> phrase in reason }?.second ?: OTHER
    }
}

/** "정원이 찼습니다 (8명)" 의 정원(8). 없으면 null */
fun boardRefusalCapacity(reason: String): Int? =
    Regex("""\((\d+)\s*명\)""").find(reason)?.groupValues?.get(1)?.toIntOrNull()
