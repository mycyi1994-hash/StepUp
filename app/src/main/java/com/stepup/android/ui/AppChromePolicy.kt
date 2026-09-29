package com.stepup.android.ui

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue

/** The only source of tab ownership and shared-chrome visibility. */
object AppChromePolicy {
    enum class Header { Main, Detail, Focus, Form }

    data class Destination(val route: String, val parent: Screen, val header: Header) {
        val showBottomBar: Boolean get() = header == Header.Main || header == Header.Detail
    }

    /** 하단 탭 다섯 — 러닝 / 신발 / 뽑기 / 커뮤니티 / 내 정보(신발 화면 확정안 2026-09-28). 뽑기는 가운데 독립 탭 */
    val tabs: List<Screen> = listOf(Screen.Run, Screen.Customize, Screen.Draw, Screen.Community, Screen.Profile)

    /**
     * 화면이 잠깐 공통 머리 · 하단 탭을 걷는 중 — 뽑기 탭의 요청 · 상자 열기 · 결과 · 확인(신발 뽑기 v2 시안은 화면을 다 쓴다).
     * 뽑기 화면이 그 흐름에 들어갈 때 켜고, 두 칸으로 돌아오거나 화면을 떠나면 끈다. 뽑기 길([Routes.MYSTERY_BOX])에서만 읽는다.
     */
    var immersive by mutableStateOf(false)

    /** 지금 길에서 공통 머리 · 하단 탭을 걷는가 */
    fun immersiveAt(route: String?): Boolean = immersive && destination(route)?.parent == Screen.Draw

    val destinations: List<Destination> = listOf(
        Destination(Screen.Run.route, Screen.Run, Header.Main),
        Destination(Screen.Customize.route, Screen.Customize, Header.Main),
        Destination(Screen.Community.route, Screen.Community, Header.Main),
        Destination(Screen.Profile.route, Screen.Profile, Header.Main),
        // 신발 뽑기 v2 — 하단 가운데 뽑기 탭의 첫 화면(두 칸). 요청 · 상자 열기 · 결과 · 확인 동안은 [immersive] 로 화면을 다 쓴다
        Destination(Routes.MYSTERY_BOX, Screen.Draw, Header.Main),
        Destination(Routes.RUN_ROUTE, Screen.Run, Header.Focus),
        Destination(Routes.RUN_MENU, Screen.Run, Header.Focus),
        Destination(Routes.RUN_GOALS, Screen.Run, Header.Focus),
        Destination(Routes.RUN_GOAL_HISTORY, Screen.Run, Header.Focus),
        Destination(Routes.RUN_DIET, Screen.Run, Header.Focus),
        Destination(Routes.RUN_DIET_PLAN, Screen.Run, Header.Focus),
        Destination(Routes.RUN_DIET_EDIT, Screen.Run, Header.Focus),
        Destination(Routes.NEWS, Screen.Run, Header.Detail),
        Destination(Routes.EVENTS, Screen.Run, Header.Detail),
        Destination(Routes.COURSES, Screen.Run, Header.Detail),
        Destination(Routes.RUNNER_MARKET, Screen.Customize, Header.Detail),
        Destination(Routes.ITEMS, Screen.Customize, Header.Detail),
        Destination(Routes.SNEAKER_DEX, Screen.Customize, Header.Detail),
        Destination(Routes.SNEAKER, Screen.Customize, Header.Detail),
        Destination(Routes.MARKET_MODEL, Screen.Customize, Header.Detail),
        Destination(Routes.MAP, Screen.Community, Header.Detail),
        Destination(Routes.RANKING, Screen.Community, Header.Detail),
        Destination(Routes.CREW_BOARD, Screen.Community, Header.Detail),
        Destination(Routes.LOBBY, Screen.Community, Header.Detail),
        Destination(Routes.FLASH_DETAIL, Screen.Community, Header.Detail),
        Destination(Routes.FLASH_LOBBY, Screen.Community, Header.Detail),
        Destination(Routes.CREW_CREATE, Screen.Community, Header.Form),
        Destination(Routes.POST_COMPOSE, Screen.Community, Header.Form),
        // 동네 이야기 — 각 화면이 자기 머리(뒤로 · 제목)를 그린다. 하단 탭 없이 댓글 입력 · 올리기가 아래에 선다
        Destination(Routes.STORY_DETAIL, Screen.Community, Header.Form),
        Destination(Routes.STORY_COMPOSE, Screen.Community, Header.Form),
        Destination(Routes.STORY_MAP, Screen.Community, Header.Form),
        Destination(Routes.STORY_LOCATION, Screen.Community, Header.Form),
        Destination(Routes.STORY_REGION, Screen.Community, Header.Form),
    ) + com.stepup.android.ui.screens.community.crew.CrewRoutes.ALL.map {
        // 크루 명함형(확정 2번) — 상세 · 신청 · 멤버 · 목표 · 만들기 · 관리는 자기 머리(뒤로 · 제목)와 아래 큰 버튼 하나
        Destination(it, Screen.Community, Header.Form)
    } + com.stepup.android.ui.screens.community.chat.ChatRoutes.ALL.map {
        // 크루 채팅 — 대화는 본문과 입력창을 넓게 쓰도록 하단 탭을 숨기고, 뒤로 가면 대화 목록(또는 들어온 곳)
        Destination(it, Screen.Community, Header.Form)
    } + listOf(
        Destination(Routes.WALLET, Screen.Profile, Header.Detail),
        Destination(Routes.CHAIN_ACTIVITY, Screen.Profile, Header.Detail),
        Destination(Routes.NOTIFICATIONS, Screen.Profile, Header.Detail),
        Destination(Routes.NOTICE, Screen.Profile, Header.Detail),
        Destination(Routes.ACHIEVEMENTS, Screen.Profile, Header.Detail),
        Destination(Routes.ANALYTICS, Screen.Profile, Header.Detail),
        // 프로필 수정 — 하단 탭 없이 자기 머리(뒤로 가면 내 정보)
        Destination(Routes.PROFILE_EDIT, Screen.Profile, Header.Form),
        // 내 러닝 기록 — 목록 · 통계는 내 정보 탭과 함께, 상세 · 경로 확대는 뒤로 가기 중심(하단 탭 없이 자기 머리)
        Destination(Routes.RECORDS, Screen.Profile, Header.Detail),
        Destination(Routes.RECORD_STATS, Screen.Profile, Header.Detail),
        Destination(Routes.RUN_RECORD, Screen.Profile, Header.Form),
        Destination(Routes.RUN_RECORD_MAP, Screen.Profile, Header.Form),
        Destination(Routes.HISTORY_MAP, Screen.Profile, Header.Detail),
        Destination(Routes.SETTINGS, Screen.Profile, Header.Detail),
        Destination(Routes.SETTINGS_NOTIFICATIONS, Screen.Profile, Header.Detail),
        Destination(Routes.SETTINGS_PRIVACY, Screen.Profile, Header.Detail),
        Destination(Routes.SETTINGS_SUPPORT, Screen.Profile, Header.Detail),
        // 사용 안내 다시 보기(시작·로그인·첫 사용 v1 시안 20) — 설정 밑, 하단 탭 없이 자기 머리와 아래 "러닝 홈으로"
        Destination(Routes.SETTINGS_GUIDE, Screen.Profile, Header.Form),
        Destination(Routes.SETTINGS_CONNECTED, Screen.Profile, Header.Detail),
        Destination(Routes.SETTINGS_LANGUAGE, Screen.Profile, Header.Detail),
        Destination(Routes.SETTINGS_EXPERIENCE, Screen.Profile, Header.Detail),
        Destination(Routes.SETTINGS_THEME, Screen.Profile, Header.Detail),
        Destination(Routes.SETTINGS_BODY, Screen.Profile, Header.Detail),
        Destination(Routes.SETTINGS_MODE, Screen.Profile, Header.Detail),
        Destination(Routes.INVITE, Screen.Profile, Header.Detail),
        Destination(Routes.CHALLENGE_HISTORY, Screen.Profile, Header.Detail),
    )

    /** Accept both NavHost route templates and concrete/deep-link route values. */
    fun destination(route: String?): Destination? {
        if (route == null) return null
        val path = route.substringBefore('?')
        destinations.firstOrNull { it.route.substringBefore('?') == path }?.let { return it }
        val segments = path.split('/')
        return destinations.firstOrNull { entry ->
            val pattern = entry.route.substringBefore('?').split('/')
            pattern.size == segments.size && pattern.zip(segments).all { (part, value) ->
                part == value || (part.startsWith('{') && part.endsWith('}') && value.isNotBlank())
            }
        }
    }
}
