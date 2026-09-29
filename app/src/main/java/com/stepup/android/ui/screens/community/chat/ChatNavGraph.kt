package com.stepup.android.ui.screens.community.chat

import android.net.Uri
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import com.stepup.android.domain.CrewDraftMode
import com.stepup.android.ui.screens.community.crew.CrewRoutes
import com.stepup.android.ui.screens.community.home.CrewHomeRoutes
import com.stepup.android.ui.screens.community.home.crewManageMembersRoute
import com.stepup.android.ui.screens.community.home.openCrewHome
import java.util.concurrent.ConcurrentHashMap

/**
 * 크루 채팅(2026-09-29) 화면의 길 — 방 id 는 크루 id 다. 방에서 연 화면은 모두 같은 crewId 를 이어 쓰고, 고른 메시지 ·
 * 크루원 · 공지는 그 id 로 연다(시안의 예시 데이터로 돌아가지 않는다).
 */
object ChatRoutes {
    /** 02 · 03 · 13 · 16 · 18–21 · 27 · 30 · 35 · 36 · 40 대화 */
    const val ROOM = "chat/room/{crewId}"

    /** 04 · 05 채팅방 정보(24 · 37 알림, 크루 나가기) */
    const val INFO = "chat/room/{crewId}/info"

    /** 06 크루원 */
    const val MEMBERS = "chat/room/{crewId}/members"

    /** 07 · 33 크루원 프로필(34 내보내기) */
    const val MEMBER = "chat/room/{crewId}/member/{userId}"

    /** 09 공지 모아보기 */
    const val NOTICES = "chat/room/{crewId}/notices"

    /** 08 공지 */
    const val NOTICE = "chat/room/{crewId}/notice/{noticeId}"

    /** 10 등록 · 11 수정(12 · 43 · 44) — fromMessage 면 고른 메시지를 미리 채운다(38) */
    const val NOTICE_EDIT = "chat/room/{crewId}/notice-edit?noticeId={noticeId}&fromMessage={fromMessage}"

    /** 22 · 23 대화 검색 */
    const val SEARCH = "chat/room/{crewId}/search"

    /** 26 · 42 보낼 사진 확인 */
    const val PHOTO = "chat/room/{crewId}/photo"

    /** 41 받은 사진 */
    const val VIEWER = "chat/room/{crewId}/viewer/{messageId}"

    /** 29 신고 접수 */
    const val REPORTED = "chat/room/{crewId}/reported"

    /** 32 채팅 참여 종료 */
    const val ENDED = "chat/ended"

    /** 하단 탭 없이 자기 머리를 그리는 채팅 화면(공통 머리 · 탭 규칙에 쓴다) */
    val ALL: List<String> = listOf(ROOM, INFO, MEMBERS, MEMBER, NOTICES, NOTICE, NOTICE_EDIT, SEARCH, PHOTO, VIEWER, REPORTED, ENDED)

    fun room(crewId: String) = "chat/room/$crewId"
    fun info(crewId: String) = "chat/room/$crewId/info"
    fun members(crewId: String) = "chat/room/$crewId/members"
    fun member(crewId: String, userId: String) = "chat/room/$crewId/member/${Uri.encode(userId)}"
    fun notices(crewId: String) = "chat/room/$crewId/notices"
    fun notice(crewId: String, noticeId: Long) = "chat/room/$crewId/notice/$noticeId"
    fun noticeEdit(crewId: String, noticeId: Long? = null, fromMessage: Boolean = false) =
        "chat/room/$crewId/notice-edit?noticeId=${noticeId ?: 0L}&fromMessage=$fromMessage"
    fun search(crewId: String) = "chat/room/$crewId/search"
    fun photo(crewId: String) = "chat/room/$crewId/photo"
    fun viewer(crewId: String, messageId: Long) = "chat/room/$crewId/viewer/$messageId"
    fun reported(crewId: String) = "chat/room/$crewId/reported"
}

/** 크루 채팅 목록으로 돌아가라는 요청 — 커뮤니티 화면이 크루 채팅 글자 탭을 연다 */
object ChatListFocus {
    var requested by mutableStateOf(false)
}

/**
 * 방에 돌아가서 할 일 — 검색 결과의 메시지로 옮겨 강조(22), 크루장 권한이 사라져 크루원으로 돌아옴(한 번 알림).
 * 방 화면이 다시 보일 때 꺼내 쓴다.
 */
object ChatRoomNotes {
    private val anchors = ConcurrentHashMap<String, Pair<Long, Long>>()
    private val ownerLost = ConcurrentHashMap.newKeySet<String>()

    fun anchor(crewId: String, messageId: Long, seq: Long) {
        anchors[crewId] = messageId to seq
    }

    fun takeAnchor(crewId: String): Pair<Long, Long>? = anchors.remove(crewId)

    fun ownerLost(crewId: String) {
        ownerLost += crewId
    }

    fun takeOwnerLost(crewId: String): Boolean = ownerLost.remove(crewId)
}

/** 지금 화면에 보이는 방 — 그 방의 새 메시지는 앱 안에서 알림을 띄우지 않는다(PushService) */
object ChatVisibility {
    @Volatile var crewId: String? = null
}

/** 크루 채팅 목록(커뮤니티 · 크루 채팅)으로 — 쌓인 채팅 화면을 걷어 낸다 */
fun NavHostController.toChatList(communityRoute: String) {
    ChatListFocus.requested = true
    if (!popBackStack(communityRoute, inclusive = false)) {
        navigate(communityRoute) {
            popUpTo(graph.findStartDestination().id)
            launchSingleTop = true
        }
    }
}

/** 32 채팅 참여 종료 — 이 방의 화면을 모두 걷고 목록 위에 연다(뒤로 가면 목록) */
fun NavHostController.toChatEnded(communityRoute: String) {
    toChatList(communityRoute)
    navigate(ChatRoutes.ENDED) { launchSingleTop = true }
}

/** 대화로 돌아가기 — 뒤에 같은 방이 있으면 거기로(스크롤 위치 그대로), 없으면 방을 연다 */
fun NavHostController.backToRoom(crewId: String) {
    if (!popBackStack(ChatRoutes.room(crewId), inclusive = false)) {
        navigate(ChatRoutes.room(crewId)) { launchSingleTop = true }
    }
}

/** 크루 채팅 방을 연다(목록 · 크루 화면 · 알림) */
fun NavHostController.openChatRoom(crewId: String) {
    navigate(ChatRoutes.room(crewId)) { launchSingleTop = true }
}

private val crewIdArg = navArgument("crewId") { type = NavType.StringType }

/**
 * 채팅 화면들을 길에 올린다. [communityRoute] 는 채팅 목록이 있는 커뮤니티 탭, [onMyProfile] 은 크루원 목록에서 "나"를
 * 눌렀을 때(내 정보 탭).
 */
fun NavGraphBuilder.chatGraph(
    navController: NavHostController,
    communityRoute: String,
    onMyProfile: () -> Unit,
) {
    val back: () -> Unit = { navController.popBackStack() }
    val toList: () -> Unit = { navController.toChatList(communityRoute) }
    val ended: () -> Unit = { navController.toChatEnded(communityRoute) }

    composable(ChatRoutes.ROOM, arguments = listOf(crewIdArg)) { entry ->
        val id = entry.arguments?.getString("crewId").orEmpty()
        ChatRoomScreen(
            viewModel(factory = ChatRoomViewModel.Factory),
            ChatRoomActions(
                // 실제로 들어온 길로 돌아간다 — 목록에서 열었으면 목록, 크루 화면에서 열었으면 그 화면
                onBack = { if (navController.previousBackStackEntry == null) toList() else back() },
                onInfo = { navController.navigate(ChatRoutes.info(id)) },
                onNotice = { noticeId -> navController.navigate(ChatRoutes.notice(id, noticeId)) },
                onManageNotice = { noticeId -> navController.navigate(ChatRoutes.noticeEdit(id, noticeId)) },
                onProfile = { userId -> navController.navigate(ChatRoutes.member(id, userId)) },
                onMyProfile = onMyProfile,
                onPhoto = { navController.navigate(ChatRoutes.photo(id)) },
                onViewer = { messageId -> navController.navigate(ChatRoutes.viewer(id, messageId)) },
                onReported = { navController.navigate(ChatRoutes.reported(id)) },
                onNoticeFromMessage = { navController.navigate(ChatRoutes.noticeEdit(id, fromMessage = true)) },
                onRecruit = { navController.navigate(CrewRoutes.draft(CrewDraftMode.EDIT_RECRUIT, id)) },
                onEnded = ended,
            ),
        )
    }
    composable(ChatRoutes.INFO, arguments = listOf(crewIdArg)) { entry ->
        val id = entry.arguments?.getString("crewId").orEmpty()
        ChatInfoScreen(
            viewModel(factory = ChatInfoViewModel.Factory),
            ChatInfoActions(
                onBack = back,
                onNotices = { navController.navigate(ChatRoutes.notices(id)) },
                // 내 크루 홈(4번) — 크루원 보기는 홈의 크루원(04), 크루장은 기존 멤버 관리(55), 크루 상세 보기는 홈(00)
                onMembers = { navController.navigate(CrewHomeRoutes.members(id)) },
                onSearch = { navController.navigate(ChatRoutes.search(id)) },
                onCrew = { navController.openCrewHome(id) },
                onWriteNotice = { navController.navigate(ChatRoutes.noticeEdit(id)) },
                onEnded = ended,
                onManageMembers = { navController.navigate(crewManageMembersRoute(id)) },
            ),
        )
    }
    composable(ChatRoutes.MEMBERS, arguments = listOf(crewIdArg)) { entry ->
        val id = entry.arguments?.getString("crewId").orEmpty()
        ChatMembersScreen(
            viewModel(factory = ChatPeopleViewModel.Factory),
            onBack = back,
            onOpen = { userId -> navController.navigate(ChatRoutes.member(id, userId)) },
            onMe = onMyProfile,
            onEnded = ended,
        )
    }
    composable(ChatRoutes.MEMBER, arguments = listOf(crewIdArg, navArgument("userId") { type = NavType.StringType })) { entry ->
        val id = entry.arguments?.getString("crewId").orEmpty()
        ChatMemberScreen(
            viewModel(factory = ChatPeopleViewModel.Factory),
            onBack = back,
            onPublicProfile = { userId -> navController.navigate(CrewRoutes.person(id, userId)) },
            // 35 — 내보낸 뒤에는 갱신된 인원과 알림 줄이 있는 대화로
            onRemoved = { navController.backToRoom(id) },
            onEnded = ended,
        )
    }
    composable(ChatRoutes.NOTICES, arguments = listOf(crewIdArg)) { entry ->
        val id = entry.arguments?.getString("crewId").orEmpty()
        ChatNoticesScreen(
            viewModel(factory = ChatNoticesViewModel.Factory),
            onBack = back,
            onOpen = { noticeId -> navController.navigate(ChatRoutes.notice(id, noticeId)) },
            onEnded = ended,
        )
    }
    composable(ChatRoutes.NOTICE, arguments = listOf(crewIdArg, navArgument("noticeId") { type = NavType.LongType })) { entry ->
        val id = entry.arguments?.getString("crewId").orEmpty()
        ChatNoticeScreen(
            viewModel(factory = ChatNoticesViewModel.Factory),
            onBack = back,
            onRoom = { navController.backToRoom(id) },
            onEdit = { noticeId -> navController.navigate(ChatRoutes.noticeEdit(id, noticeId)) },
            onEnded = ended,
            // 공지에 이은 모임(4번 20) — 모임 상세에서 뒤로 가면 이 공지
            onMeeting = { meetingId -> navController.navigate(CrewHomeRoutes.meeting(id, meetingId)) },
        )
    }
    composable(
        ChatRoutes.NOTICE_EDIT,
        arguments = listOf(
            crewIdArg,
            navArgument("noticeId") { type = NavType.LongType; defaultValue = 0L },
            navArgument("fromMessage") { type = NavType.BoolType; defaultValue = false },
        ),
    ) { entry ->
        val id = entry.arguments?.getString("crewId").orEmpty()
        ChatNoticeEditScreen(
            viewModel(factory = ChatNoticeEditViewModel.Factory),
            onBack = back,
            // 13 — 올린(고친 · 지운) 뒤에는 대화로. 새 공지는 서버가 남긴 알림 줄이 한 번 보인다
            onDone = { navController.backToRoom(id) },
            onOwnerLost = {
                ChatRoomNotes.ownerLost(id)
                navController.backToRoom(id)
            },
            onEnded = ended,
        )
    }
    composable(ChatRoutes.SEARCH, arguments = listOf(crewIdArg)) { entry ->
        val id = entry.arguments?.getString("crewId").orEmpty()
        ChatSearchScreen(
            viewModel(factory = ChatSearchViewModel.Factory),
            onBack = back,
            onOpen = { messageId, seq ->
                ChatRoomNotes.anchor(id, messageId, seq)
                navController.backToRoom(id)
            },
            onEnded = ended,
        )
    }
    composable(ChatRoutes.PHOTO, arguments = listOf(crewIdArg)) { entry ->
        val id = entry.arguments?.getString("crewId").orEmpty()
        ChatPhotoScreen(
            viewModel(factory = ChatPhotoViewModel.Factory),
            onBack = back,
            onSent = { navController.backToRoom(id) },
            onEnded = ended,
        )
    }
    composable(ChatRoutes.VIEWER, arguments = listOf(crewIdArg, navArgument("messageId") { type = NavType.LongType })) { entry ->
        val id = entry.arguments?.getString("crewId").orEmpty()
        ChatViewerScreen(
            viewModel(factory = ChatViewerViewModel.Factory),
            onBack = back,
            onRoom = { navController.backToRoom(id) },
            onEnded = ended,
        )
    }
    composable(ChatRoutes.REPORTED, arguments = listOf(crewIdArg)) { entry ->
        val id = entry.arguments?.getString("crewId").orEmpty()
        ChatReportedScreen(onBack = back, onRoom = { navController.backToRoom(id) })
    }
    composable(ChatRoutes.ENDED) {
        ChatEndedScreen(onList = toList)
    }
}
