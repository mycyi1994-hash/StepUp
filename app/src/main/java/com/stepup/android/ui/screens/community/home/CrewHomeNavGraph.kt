package com.stepup.android.ui.screens.community.home

import android.net.Uri
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import com.stepup.android.ui.screens.community.chat.ChatRoutes
import com.stepup.android.ui.screens.community.chat.backToRoom
import com.stepup.android.ui.screens.community.crew.CrewMissingPage
import com.stepup.android.ui.screens.community.crew.CrewRosterMode
import com.stepup.android.ui.screens.community.crew.CrewRoutes
import com.stepup.android.ui.screens.community.crew.CrewScreenViewModel
import com.stepup.android.ui.screens.community.crew.toCrewList
import java.time.LocalDate

/**
 * 내 크루 홈(확정 4번, 2026-09-29) 화면의 길 — 같은 crewId 를 이어 쓰고, 고른 모임(meetingId) · 크루원(userId) ·
 * 주(week) · 날(day) · 기록(runId)을 길에 싣는다. 시안의 퇴근런 · 지연 같은 예시 값으로 돌아가지 않는다.
 * 대표 이미지(02)는 크루 명함형의 것(CrewRoutes.IMAGE), 공지(20 · 21)와 채팅(07 · 08 · 30 · 31)은 크루 채팅의 것을 잇는다.
 */
object CrewHomeRoutes {
    /** 00 · 22 · 23 · 26 · 28 · 29 */
    const val HOME = "crew/home/{crewId}"

    /** 01 크루 소개 */
    const val INTRO = "crew/home/{crewId}/intro"

    /** 03 크루 레벨 */
    const val LEVEL = "crew/home/{crewId}/level"

    /** 04 크루원 · 27 이름 검색 */
    const val MEMBERS = "crew/home/{crewId}/members"

    /** 05 크루장 · 06 크루원 프로필 */
    const val PERSON = "crew/home/{crewId}/person/{userId}"

    /** 09 · 10 · 11 · 12 · 25 모임 */
    const val MEETING = "crew/home/{crewId}/meeting/{meetingId}"

    /** 13 참석자 */
    const val ATTENDEES = "crew/home/{crewId}/meeting/{meetingId}/attendees"

    /** 14 모이는 장소 */
    const val PLACE = "crew/home/{crewId}/meeting/{meetingId}/place"

    /** 15 · 16 · 17 · 24 주간 기록 */
    const val WEEK = "crew/home/{crewId}/week?week={week}"

    /** 18 참여 기록 */
    const val RUNS = "crew/home/{crewId}/runs?week={week}&day={day}&userId={userId}&name={name}"

    /** 19 러닝 기록 */
    const val RUN = "crew/home/{crewId}/run/{runId}"

    /** 접근이 사라졌다(탈퇴 · 내보내기 · 해산) — 크루를 볼 수 없어요(기존 66) */
    const val ENDED = "crew/home-ended"

    /** 하단 탭 없이 자기 머리를 그리는 크루 홈 화면(공통 머리 · 탭 규칙에 쓴다) */
    val ALL: List<String> = listOf(HOME, INTRO, LEVEL, MEMBERS, PERSON, MEETING, ATTENDEES, PLACE, WEEK, RUNS, RUN, ENDED)

    fun home(crewId: String) = "crew/home/$crewId"
    fun intro(crewId: String) = "crew/home/$crewId/intro"
    fun level(crewId: String) = "crew/home/$crewId/level"
    fun members(crewId: String) = "crew/home/$crewId/members"
    fun person(crewId: String, userId: String) = "crew/home/$crewId/person/${Uri.encode(userId)}"
    fun meeting(crewId: String, meetingId: Long) = "crew/home/$crewId/meeting/$meetingId"
    fun attendees(crewId: String, meetingId: Long) = "crew/home/$crewId/meeting/$meetingId/attendees"
    fun place(crewId: String, meetingId: Long) = "crew/home/$crewId/meeting/$meetingId/place"
    fun week(crewId: String, week: LocalDate? = null) = "crew/home/$crewId/week?week=${week?.toString().orEmpty()}"
    fun runs(crewId: String, week: LocalDate?, day: LocalDate? = null, userId: String? = null, name: String? = null) =
        "crew/home/$crewId/runs?week=${week?.toString().orEmpty()}&day=${day?.toString().orEmpty()}" +
            "&userId=${Uri.encode(userId.orEmpty())}&name=${Uri.encode(name.orEmpty())}"
    fun run(crewId: String, runId: Long) = "crew/home/$crewId/run/$runId"
}

/** 크루 홈을 연다 — 뒤에 같은 크루의 홈이 있으면 거기로 돌아간다(채팅 → 정보 → 크루 상세 보기가 홈을 겹겹이 쌓지 않게) */
fun NavHostController.openCrewHome(crewId: String) {
    if (!popBackStack(CrewHomeRoutes.home(crewId), inclusive = false)) navigate(CrewHomeRoutes.home(crewId))
}

/** 접근 종료 — 이 크루의 화면을 걷고 크루 목록 위에 연다(뒤로 가면 목록) */
fun NavHostController.toCrewHomeEnded(communityRoute: String) {
    toCrewList(communityRoute)
    navigate(CrewHomeRoutes.ENDED) { launchSingleTop = true }
}

private val crewIdArg = navArgument("crewId") { type = NavType.StringType }
private val meetingIdArg = navArgument("meetingId") { type = NavType.LongType }

/**
 * 크루 홈 화면들을 길에 올린다. [communityRoute] 는 크루 목록이 있는 커뮤니티 탭, [boardRoute] · [lobbyRoute] 는 기존 크루
 * 게시판 · 같이 달리기(더보기에 둔다).
 */
fun NavGraphBuilder.crewHomeGraph(
    navController: NavHostController,
    communityRoute: String,
    boardRoute: (String) -> String,
    lobbyRoute: (String) -> String,
) {
    val back: () -> Unit = { navController.popBackStack() }
    val toList: () -> Unit = { navController.toCrewList(communityRoute) }
    val ended: () -> Unit = { navController.toCrewHomeEnded(communityRoute) }

    composable(CrewHomeRoutes.HOME, arguments = listOf(crewIdArg)) { entry ->
        val id = entry.arguments?.getString("crewId").orEmpty()
        CrewHomeScreen(
            viewModel(factory = CrewHomeViewModel.Factory),
            CrewHomeActions(
                onBack = back,
                onIntro = { navController.navigate(CrewHomeRoutes.intro(id)) },
                onLevel = { navController.navigate(CrewHomeRoutes.level(id)) },
                onMembers = { navController.navigate(CrewHomeRoutes.members(id)) },
                onPerson = { userId -> if (userId.isNotBlank()) navController.navigate(CrewHomeRoutes.person(id, userId)) },
                // 채팅에서 홈으로 왔으면 그 방으로 돌아간다(스크롤 위치 그대로), 아니면 방을 연다
                onChat = { navController.backToRoom(id) },
                onMeeting = { meetingId -> navController.navigate(CrewHomeRoutes.meeting(id, meetingId)) },
                onWeek = { navController.navigate(CrewHomeRoutes.week(id)) },
                onNotice = { noticeId -> navController.navigate(ChatRoutes.notice(id, noticeId)) },
                onNotices = { navController.navigate(ChatRoutes.notices(id)) },
                onBoard = { navController.navigate(boardRoute(id)) },
                onLobby = { navController.navigate(lobbyRoute(id)) },
                onManage = { navController.navigate(CrewRoutes.manage(id)) },
                onList = toList,
                onLeft = toList,
            ),
        )
    }
    composable(CrewHomeRoutes.INTRO, arguments = listOf(crewIdArg)) { entry ->
        val id = entry.arguments?.getString("crewId").orEmpty()
        CrewIntroScreen(
            viewModel(factory = CrewScreenViewModel.Factory),
            onBack = back,
            onImage = { navController.navigate(CrewRoutes.image(id)) },
            onLeader = { leaderId -> if (leaderId.isNotBlank()) navController.navigate(CrewHomeRoutes.person(id, leaderId)) },
            onMembers = { navController.navigate(CrewHomeRoutes.members(id)) },
        )
    }
    composable(CrewHomeRoutes.LEVEL, arguments = listOf(crewIdArg)) { entry ->
        val id = entry.arguments?.getString("crewId").orEmpty()
        CrewLevelScreen(viewModel(factory = CrewScreenViewModel.Factory), onBack = back, onWeekly = { navController.navigate(CrewHomeRoutes.week(id)) })
    }
    composable(CrewHomeRoutes.MEMBERS, arguments = listOf(crewIdArg)) { entry ->
        val id = entry.arguments?.getString("crewId").orEmpty()
        CrewMembersHomeScreen(
            viewModel(factory = CrewHomeMembersViewModel.Factory),
            onBack = back,
            onOpen = { member -> navController.navigate(CrewHomeRoutes.person(id, member.userId)) },
        )
    }
    composable(CrewHomeRoutes.PERSON, arguments = listOf(crewIdArg, navArgument("userId") { type = NavType.StringType })) { entry ->
        val id = entry.arguments?.getString("crewId").orEmpty()
        CrewPersonHomeScreen(
            viewModel(factory = CrewHomePersonViewModel.Factory),
            onBack = back,
            onRun = { run -> navController.navigate(CrewHomeRoutes.run(id, run.id)) },
            onEnded = ended,
        )
    }
    composable(CrewHomeRoutes.MEETING, arguments = listOf(crewIdArg, meetingIdArg)) { entry ->
        val id = entry.arguments?.getString("crewId").orEmpty()
        val meetingId = entry.arguments?.getLong("meetingId") ?: 0L
        CrewMeetingScreen(
            viewModel(factory = CrewMeetingViewModel.Factory),
            crewName = crewNameOf(id),
            onBack = back,
            onPlace = { navController.navigate(CrewHomeRoutes.place(id, meetingId)) },
            onAttendees = { navController.navigate(CrewHomeRoutes.attendees(id, meetingId)) },
            onChat = { navController.backToRoom(id) },
            onEnded = ended,
        )
    }
    composable(CrewHomeRoutes.ATTENDEES, arguments = listOf(crewIdArg, meetingIdArg)) { entry ->
        val id = entry.arguments?.getString("crewId").orEmpty()
        CrewAttendeesScreen(
            viewModel(factory = CrewAttendeesViewModel.Factory),
            onBack = back,
            onOpen = { person -> navController.navigate(CrewHomeRoutes.person(id, person.userId)) },
            onEnded = ended,
        )
    }
    composable(CrewHomeRoutes.PLACE, arguments = listOf(crewIdArg, meetingIdArg)) {
        CrewPlaceScreen(viewModel(factory = CrewMeetingViewModel.Factory), onBack = back, onEnded = ended)
    }
    composable(
        CrewHomeRoutes.WEEK,
        arguments = listOf(crewIdArg, navArgument("week") { type = NavType.StringType; defaultValue = "" }),
    ) { entry ->
        val id = entry.arguments?.getString("crewId").orEmpty()
        CrewWeekScreen(
            viewModel(factory = CrewWeekViewModel.Factory),
            CrewWeekActions(
                onBack = back,
                onRuns = { week, day, member -> navController.navigate(CrewHomeRoutes.runs(id, week, day, member?.userId, member?.name)) },
                onHome = { navController.openCrewHome(id) },
                onEnded = ended,
            ),
        )
    }
    composable(
        CrewHomeRoutes.RUNS,
        arguments = listOf(
            crewIdArg,
            navArgument("week") { type = NavType.StringType; defaultValue = "" },
            navArgument("day") { type = NavType.StringType; defaultValue = "" },
            navArgument("userId") { type = NavType.StringType; defaultValue = "" },
            navArgument("name") { type = NavType.StringType; defaultValue = "" },
        ),
    ) { entry ->
        val id = entry.arguments?.getString("crewId").orEmpty()
        CrewRunsScreen(
            viewModel(factory = CrewRunsViewModel.Factory),
            onBack = back,
            onRun = { run -> navController.navigate(CrewHomeRoutes.run(id, run.id)) },
            onEnded = ended,
        )
    }
    composable(CrewHomeRoutes.RUN, arguments = listOf(crewIdArg, navArgument("runId") { type = NavType.LongType })) { entry ->
        val id = entry.arguments?.getString("crewId").orEmpty()
        CrewRunScreen(
            viewModel(factory = CrewRunViewModel.Factory),
            onBack = back,
            // 참여 기록에서 왔으면 그 목록으로(고른 주 · 날 · 사람 그대로), 프로필에서 왔으면 이번 주 참여 기록을 연다
            onRuns = {
                val previous = navController.previousBackStackEntry?.destination?.route
                if (previous == CrewHomeRoutes.RUNS) navController.popBackStack()
                else navController.navigate(CrewHomeRoutes.runs(id, null)) { popUpTo(CrewHomeRoutes.RUN) { inclusive = true } }
            },
            onEnded = ended,
        )
    }
    composable(CrewHomeRoutes.ENDED) {
        CrewMissingPage(onBack = back, onList = toList)
    }
}

/** 크루장 멤버 관리(기존 55) — 채팅방 정보(31)의 크루원 보기 · 멤버 관리 */
fun crewManageMembersRoute(crewId: String): String = CrewRoutes.members(crewId, CrewRosterMode.MANAGE)
