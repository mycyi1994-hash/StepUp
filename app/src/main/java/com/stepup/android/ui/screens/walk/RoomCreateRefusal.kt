package com.stepup.android.ui.screens.walk

/**
 * 크루 대기실 만들기(서버 0072 party_create · 0074 party_create_v2)가 거절한 까닭 — 화면이 번역된 문구를 고른다.
 *
 * 서버는 한국어 문장(크루원만 대기실을 열 수 있습니다 · 로그인이 필요합니다)이나 영어 코드(room_name_invalid ·
 * course_invalid · room_options_invalid · crew_run_done_today)로 거절한다. 그대로 띄우면 영어 · 일본어 · 중국어 화면에
 * 한국어가 뜨므로 아는 까닭은 갈래로 옮기고, 모르는 까닭은 [OTHER](까닭 없이 "열지 못했어요")로 둔다.
 */
internal enum class RoomCreateRefusal {
    DONE_TODAY,
    OPTIONS,
    NOT_MEMBER,
    NAME_INVALID,
    COURSE_INVALID,
    ROOM_CLOSED,
    SIGN_IN,
    OTHER;

    companion object {
        private val PHRASES = listOf(
            "crew_run_done_today" to DONE_TODAY,
            "room_options_invalid" to OPTIONS,
            "room_name_invalid" to NAME_INVALID,
            "course_invalid" to COURSE_INVALID,
            "room_closed" to ROOM_CLOSED,
            "크루원만" to NOT_MEMBER,
            "멤버가 아닙니다" to NOT_MEMBER,
            "로그인이 필요" to SIGN_IN,
        )

        fun of(reason: String): RoomCreateRefusal =
            PHRASES.firstOrNull { (phrase, _) -> phrase in reason }?.second ?: OTHER
    }
}
