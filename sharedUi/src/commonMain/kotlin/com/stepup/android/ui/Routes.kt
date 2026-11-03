package com.stepup.android.ui

/** 화면 길 이름 — 안드로이드 StepUpRoot.kt 의 Routes 와 같은 값(알림 · 다른 화면에서 그 자리로 가는 이동이 같은 이름을 쓴다) */
object Routes {
    /** 러닝 안의 소식 — 대회 · 러닝·건강 */
    const val NEWS = "news"

    /** 러닝 안의 챌린지 */
    const val EVENTS = "events"

    /** 꾸미기 안의 신발 보관함 · 거래소 · 스토어 */
    const val ITEMS = "items"
    const val MYSTERY_BOX = "mystery-box"

    /** 꾸미기 안의 러너 마켓 — 의상 / 신발 */
    const val RUNNER_MARKET = "runner-market"

    const val RUN = "run"

    /** 러닝 화면 — start=true 면 들어오자마자 달리기를 시작한다 */
    const val RUN_ROUTE = "run?start={start}"
    const val RUN_NOW = "run?start=true"

    /** 러닝 시작 메뉴(시안 U01) · 러닝 챌린지(U02) · 지난 도전(C03) */
    const val RUN_MENU = "run-start"
    const val RUN_GOALS = "run-goals"
    const val RUN_GOAL_HISTORY = "run-goals/history"

    /** 추천 코스(시안 U04 · K01~K03) — 내 위치에서 가까운 저장 · 게시판 코스 */
    const val RUN_COURSE = "run-course"
    /** 크루 달리기 입구(시안 U01 → 크루 · CR14 · CR19) */
    const val RUN_CREW = "run-crew"

    /** 다이어트 모드 — 입력(U05) · 러닝 방법(U06) · 몸 정보·경험 수정(D06) */
    const val RUN_DIET = "run-diet"
    const val RUN_DIET_PLAN = "run-diet/plan"
    const val RUN_DIET_EDIT = "run-diet/edit"
    const val WALLET = "wallet"
    /** 내 정보 › 지갑 › 체인 기록 */
    const val CHAIN_ACTIVITY = "wallet/chain"
    const val NOTIFICATIONS = "notifications"

    /** 앱 공지 상세(알림·공지 v1) — 알림 화면의 "공지"에서. 바깥 소식(NEWS)과 다른 것이다 */
    const val NOTICE = "notice/{id}"
    const val ACHIEVEMENTS = "achievements"
    /** 예전 기록 · 분석(걸음 통계 · 기록 지도) — 러닝 통계의 "걸음 통계와 기록 지도"에서 */
    const val ANALYTICS = "analytics"

    /** 프로필 수정(2026-09-28 전달본) — 내 정보의 "프로필 수정". 사진 · 닉네임, 하단 탭 없이 */
    const val PROFILE_EDIT = "profile/edit"

    /** 편집 화면에서 닉네임을 저장하고 돌아왔다는 표시 — 내 정보 화면의 저장 상태에 한 번 적는다 */
    const val NICKNAME_SAVED = "profile_nickname_saved"

    /** 내 러닝 기록(2026-09-28 전달본) — 내 정보의 "내 러닝 기록 보기". 목록 · 통계 · 지난 러닝 상세 · 경로 확대 */
    const val RECORDS = "records"
    const val RECORD_STATS = "records/stats?month={month}"
    const val RUN_RECORD = "records/run/{id}"
    const val RUN_RECORD_MAP = "records/run/{id}/map"
    /** 설정 첫 목록(설정 v1, 2026-09-28) — 내 정보 첫 화면의 "설정"에서 */
    const val SETTINGS = "settings"
    const val SETTINGS_NOTIFICATIONS = "settings/notifications"
    const val SETTINGS_PRIVACY = "settings/privacy"
    const val SETTINGS_SUPPORT = "settings/support"

    /** 사용 안내 다시 보기(시작·로그인·첫 사용 v1 시안 20) — 도움말 · 문의의 "앱 사용 안내"에서 */
    const val SETTINGS_GUIDE = "settings/guide"
    const val SETTINGS_CONNECTED = "settings/connected"
    const val SETTINGS_LANGUAGE = "settings/language"
    const val SETTINGS_EXPERIENCE = "settings/experience"
    const val SETTINGS_THEME = "settings/theme"
    const val SETTINGS_BODY = "settings/body"
    const val SETTINGS_MODE = "settings/mode"
    const val INVITE = "invite"
    const val BETA_CODES = "beta_codes"
    const val CHALLENGE_HISTORY = "challenge-history"
    const val SNEAKER = "sneaker/{id}"

    /** 신발 강화(파란 톤 v4 강화 · 재료 신발 3개, 0054) — 상세의 강화하기 · 강화 결과 확인이 같은 화면으로. 뒤로는 상세로 */
    const val SNEAKER_UPGRADE = "sneaker/{id}/upgrade"
    /** 크루 대기실 — party 는 목록에서 고른(또는 방금 연) 방 번호(0072). 없으면(-1) 예전처럼 크루의 열린 방 하나에 */
    const val LOBBY = "lobby/{crewId}?party={party}"

    /** 크루 러닝 — 같은 크루의 열린 대기실(2026-10-24 장면 02) · 러닝 만들기(03 · 06, 코스 고르기 04 · 05 · 09 포함) */
    const val CREW_ROOMS = "crew-rooms/{crewId}"
    const val CREW_ROOM_NEW = "crew-rooms/{crewId}/new"
    const val RANKING = "ranking"
    const val CREW_CREATE = "crew/create"
    const val CREW_BOARD = "crew/board/{crewId}"
    /** 글쓰기 — flash 면 번개러닝 모집으로 열린다(함께 뛰기 만들기 → 누구나) */
    const val POST_COMPOSE = "post/compose/{crewId}?flash={flash}"
    const val FLASH_DETAIL = "flash/{postId}"
    const val FLASH_LOBBY = "flash/lobby/{postId}"
    const val COURSES = "courses"

    /** 지도 — 내 주변 번개·코스, 땅따먹기 */
    const val MAP = "map"

    /** 기록 지도 — 달린 길을 모두 겹친 히트맵 */
    const val HISTORY_MAP = "analytics/map"
    const val SNEAKER_DEX = "sneaker/dex"

    /**
     * 거래소의 모델 장부 — 속성 × 등급 × 변형 하나.
     *
     * sell 은 보관함에서 "판매"로 들어왔을 때 그 신발의 로컬 번호다. 값을
     * 들고 오면 판매 등록 창이 그 켤레를 고른 채로 열린다. 등록은 값을 적고
     * 확정해야 끝난다 — 번호를 들고 왔다고 저절로 올라가지 않는다.
     */
    const val MARKET_MODEL = "market/{faction}/{rarity}/{variant}?sell={sell}"

    fun sneaker(id: Long) = "sneaker/$id"
    fun sneakerUpgrade(id: Long) = "sneaker/$id/upgrade"

    fun marketModel(faction: String, rarity: String, variant: Int, sell: Long = 0) =
        "market/$faction/$rarity/$variant?sell=$sell"
    fun lobby(crewId: String) = "lobby/$crewId"

    /** 고른 크루 대기실 */
    fun room(crewId: String, partyId: Long) = "lobby/$crewId?party=$partyId"
    fun crewRooms(crewId: String) = "crew-rooms/$crewId"
    fun crewRoomNew(crewId: String) = "crew-rooms/$crewId/new"

    /**
     * 크루 러닝 입구 — 그 크루의 대기실(출발 전 · 카운트다운)에 들어가 있으면 그 방, 아니면 열린 대기실 목록.
     * 들어가는 것은 목록에서 방을 고를 때뿐이다(예전처럼 저절로 아무 방에 넣지 않는다).
     */
    fun crewRun(crewId: String): String {
        // 안드로이드: ServiceLocator.crewRepository.party — 공용에서는 앱이 채운 AppServices.openCrewRoom(크루 대기실 · 카운트다운 중인 방 번호)
        val id = com.stepup.android.core.AppServices.openCrewRoom(crewId)
        return if (id != null) room(crewId, id) else crewRooms(crewId)
    }
    fun notice(id: Long) = "notice/$id"
    fun recordStats(month: kotlinx.datetime.YearMonth) = "records/stats?month=$month"
    fun runRecord(id: Long) = "records/run/$id"
    fun runRecordMap(id: Long) = "records/run/$id/map"
    fun crewBoard(crewId: String) = "crew/board/$crewId"

    /** crewId가 비어 있으면 전체 게시판에 쓰는 글 */
    fun postCompose(crewId: String, flash: Boolean = false) = "post/compose/${crewId.ifBlank { NO_CREW }}?flash=$flash"

    fun flashDetail(postId: Long) = "flash/$postId"

    // ── 동네 이야기(목록형 커뮤니티, 2026-09-27) ──
    /** 글 상세 — 댓글 · 반응 · 내 글 관리 · 신고 */
    const val STORY_DETAIL = "story/{postId}"

    /** 글쓰기 · 고치기 — edit 가 0 이면 새 글, resume 이면 쓰다 만 글을 이어 쓴다 */
    const val STORY_COMPOSE = "story/compose?edit={edit}&resume={resume}"

    /** 내 주변 지도 — 장소와 장소마다 글 수 */
    const val STORY_MAP = "story/map"

    /** 내 주변 — 내 위치 사용 · 지역 직접 선택 */
    const val STORY_LOCATION = "story/location"

    /** 지역 직접 선택 */
    const val STORY_REGION = "story/region"

    fun storyDetail(postId: Long) = "story/$postId"
    fun storyCompose(edit: Long = 0L, resume: Boolean = false) = "story/compose?edit=$edit&resume=$resume"

    fun flashLobby(postId: Long) = "flash/lobby/$postId"

    const val NO_CREW = "_"
}
