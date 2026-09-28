package com.stepup.android.ui.screens.community.crew

import android.net.Uri
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import com.stepup.android.core.ServiceLocator
import com.stepup.android.data.prefs.CrewDraftCodec
import com.stepup.android.domain.CrewDraftMode
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * 크루 명함형(확정 2번) 화면의 길. 같은 crewId 를 이어 쓰고, 화면마다 역할 · 신청서 · 신청 id 를 들고 간다.
 * 목록 위치 · 지역 · 정렬은 목록 쪽(저장소 · 화면 상태)에 남아 상세에 다녀와도 그대로다.
 */
object CrewRoutes {
    /** 08 · 20 · 21 · 22 · 23 · 70 · 76 · 86 · 87 · 89 상세 */
    const val DETAIL = "crew/card/{crewId}"
    const val IMAGE = "crew/card/{crewId}/image"
    const val LEADER = "crew/card/{crewId}/leader"
    const val MEMBERS = "crew/card/{crewId}/members?mode={mode}"
    const val PERSON = "crew/card/{crewId}/person/{userId}"
    const val GOAL = "crew/card/{crewId}/goal"
    const val GOAL_EDIT = "crew/card/{crewId}/goal-edit"
    const val JOIN = "crew/card/{crewId}/join"
    const val APPLICATION = "crew/card/{crewId}/application/{applicationId}"
    const val RESULT = "crew/card/{crewId}/result/{applicationId}"
    const val JOINED = "crew/card/{crewId}/joined"
    const val REPORTED = "crew/card/{crewId}/reported"
    const val MANAGE = "crew/card/{crewId}/manage?created={created}"
    const val REQUESTS = "crew/card/{crewId}/requests"
    const val REVIEW = "crew/card/{crewId}/review/{applicationId}"
    const val DECIDED = "crew/card/{crewId}/decided?approved={approved}&name={name}&members={members}&capacity={capacity}&pending={pending}"
    const val SETTINGS = "crew/card/{crewId}/settings"
    const val TRANSFER = "crew/card/{crewId}/transfer?userId={userId}&name={name}"
    const val CREATED = "crew/card/{crewId}/created"

    /** 26 · 31 · 33 · 34(만들기) · 41 · 42 · 43(수정) — resume 이면 남긴 만들기 초안을 이어 쓴다 */
    const val DRAFT = "crew/draft?mode={mode}&crewId={crewId}&resume={resume}"

    /** 25 모집할 크루 선택 */
    const val RECRUIT = "crew/recruit"

    /** 03 · 71 · 72 활동 지역 선택 */
    const val REGION = "crew/region?target={target}"

    /** 하단 탭 없이 자기 머리를 그리는 크루 화면(공통 머리 · 탭 규칙에 쓴다) */
    val ALL: List<String> = listOf(
        DETAIL, IMAGE, LEADER, MEMBERS, PERSON, GOAL, GOAL_EDIT, JOIN, APPLICATION, RESULT, JOINED, REPORTED,
        MANAGE, REQUESTS, REVIEW, DECIDED, SETTINGS, TRANSFER, CREATED, DRAFT, RECRUIT, REGION,
    )

    fun detail(crewId: String) = "crew/card/$crewId"
    fun image(crewId: String) = "crew/card/$crewId/image"
    fun leader(crewId: String) = "crew/card/$crewId/leader"
    fun members(crewId: String, mode: CrewRosterMode = CrewRosterMode.MEMBERS) = "crew/card/$crewId/members?mode=${mode.name}"
    fun person(crewId: String, userId: String) = "crew/card/$crewId/person/${Uri.encode(userId)}"
    fun goal(crewId: String) = "crew/card/$crewId/goal"
    fun goalEdit(crewId: String) = "crew/card/$crewId/goal-edit"
    fun join(crewId: String) = "crew/card/$crewId/join"
    fun application(crewId: String, applicationId: Long) = "crew/card/$crewId/application/$applicationId"
    fun result(crewId: String, applicationId: Long) = "crew/card/$crewId/result/$applicationId"
    fun joined(crewId: String) = "crew/card/$crewId/joined"
    fun reported(crewId: String) = "crew/card/$crewId/reported"
    fun manage(crewId: String, created: Boolean = false) = "crew/card/$crewId/manage?created=$created"
    fun requests(crewId: String) = "crew/card/$crewId/requests"
    fun review(crewId: String, applicationId: Long) = "crew/card/$crewId/review/$applicationId"
    fun decided(crewId: String, approved: Boolean, name: String, members: Int, capacity: Int?, pending: Int) =
        "crew/card/$crewId/decided?approved=$approved&name=${Uri.encode(name)}&members=$members&capacity=${capacity ?: -1}&pending=$pending"
    fun settings(crewId: String) = "crew/card/$crewId/settings"
    fun transfer(crewId: String, userId: String? = null, name: String? = null) =
        "crew/card/$crewId/transfer?userId=${Uri.encode(userId.orEmpty())}&name=${Uri.encode(name.orEmpty())}"
    fun created(crewId: String) = "crew/card/$crewId/created"
    fun draft(mode: CrewDraftMode, crewId: String = "", resume: Boolean = false) =
        "crew/draft?mode=${mode.name}&crewId=$crewId&resume=$resume"
    fun region(target: CrewRegionTarget) = "crew/region?target=${target.name}"
}

/** 크루 모집 목록으로 돌아가라는 요청 — 커뮤니티 화면이 크루 모집 글자 탭을 연다 */
object CrewListFocus {
    var requested by mutableStateOf(false)
}

/** 크루 목록(커뮤니티 · 크루 모집)으로 — 쌓인 크루 화면을 걷어 낸다 */
fun NavHostController.toCrewList(communityRoute: String) {
    CrewListFocus.requested = true
    if (!popBackStack(communityRoute, inclusive = false)) {
        navigate(communityRoute) { launchSingleTop = true }
    }
}

/** 이 크루의 상세로 — 뒤에 같은 크루 상세가 있으면 거기로 돌아가고, 없으면 지금 화면 대신 연다 */
private fun NavHostController.showCrew(crewId: String, replacing: String) {
    if (!popBackStack(CrewRoutes.detail(crewId), inclusive = false)) {
        navigate(CrewRoutes.detail(crewId)) { popUpTo(replacing) { inclusive = true } }
    }
}

/** 커뮤니티 · 크루 모집 목록에서 나가는 곳 */
fun crewListActions(navController: NavHostController): CrewListActions = CrewListActions(
    onOpenCrew = { id -> navController.navigate(CrewRoutes.detail(id)) },
    onOpenImage = { id -> navController.navigate(CrewRoutes.image(id)) },
    onOpenMembers = { id -> navController.navigate(CrewRoutes.members(id)) },
    onOpenLeader = { id, _ -> navController.navigate(CrewRoutes.leader(id)) },
    onOpenGoal = { id -> navController.navigate(CrewRoutes.goal(id)) },
    onOpenResult = { id, applicationId -> navController.navigate(CrewRoutes.result(id, applicationId)) },
    onRecruitEntry = { navController.navigate(CrewRoutes.RECRUIT) },
    onCreate = { resume -> navController.navigate(CrewRoutes.draft(CrewDraftMode.CREATE, resume = resume)) },
    onOpenRegion = { navController.navigate(CrewRoutes.region(CrewRegionTarget.LIST)) },
)

private val crewIdArg = navArgument("crewId") { type = NavType.StringType }

/**
 * 크루 화면들을 길에 올린다. [communityRoute] 는 크루 목록이 있는 커뮤니티 탭,
 * [boardRoute] · [lobbyRoute] 는 기존 크루 게시판 · 같이 달리기 대기실(멤버가 상세에서 연다).
 */
fun NavGraphBuilder.crewGraph(
    navController: NavHostController,
    communityRoute: String,
    boardRoute: (String) -> String,
    lobbyRoute: (String) -> String,
) {
    val back: () -> Unit = { navController.popBackStack() }
    val toList: () -> Unit = { navController.toCrewList(communityRoute) }

    composable(CrewRoutes.DETAIL, arguments = listOf(crewIdArg)) { entry ->
        val id = entry.arguments?.getString("crewId").orEmpty()
        val vm: CrewScreenViewModel = viewModel(factory = CrewScreenViewModel.Factory)
        CrewDetailScreen(
            CrewDetailActions(
                onBack = back,
                onOpenImage = { navController.navigate(CrewRoutes.image(id)) },
                onOpenLeader = { navController.navigate(CrewRoutes.leader(id)) },
                onOpenMembers = { navController.navigate(CrewRoutes.members(id)) },
                onOpenGoal = { navController.navigate(CrewRoutes.goal(id)) },
                onJoin = { navController.navigate(CrewRoutes.join(id)) },
                onOpenApplication = { applicationId -> navController.navigate(CrewRoutes.application(id, applicationId)) },
                onOpenResult = { applicationId -> navController.navigate(CrewRoutes.result(id, applicationId)) },
                onJoinedNow = { navController.navigate(CrewRoutes.joined(id)) },
                onManage = { navController.navigate(CrewRoutes.manage(id)) },
                onSettings = { navController.navigate(CrewRoutes.settings(id)) },
                onReported = { navController.navigate(CrewRoutes.reported(id)) },
                onOpenBoard = { navController.navigate(boardRoute(id)) },
                onOpenLobby = { navController.navigate(lobbyRoute(id)) },
                onList = toList,
            ),
            vm,
        )
    }
    composable(CrewRoutes.IMAGE, arguments = listOf(crewIdArg)) {
        CrewImageScreen(viewModel(factory = CrewScreenViewModel.Factory), onBack = back)
    }
    composable(CrewRoutes.LEADER, arguments = listOf(crewIdArg)) { entry ->
        val id = entry.arguments?.getString("crewId").orEmpty()
        CrewLeaderScreen(
            viewModel(factory = CrewScreenViewModel.Factory), onBack = back,
            onOpenCrew = { navController.showCrew(id, CrewRoutes.LEADER) },
        )
    }
    composable(
        CrewRoutes.MEMBERS,
        arguments = listOf(crewIdArg, navArgument("mode") { type = NavType.StringType; defaultValue = CrewRosterMode.MEMBERS.name }),
    ) { entry ->
        val id = entry.arguments?.getString("crewId").orEmpty()
        val mode = CrewRosterMode.entries.firstOrNull { it.name == entry.arguments?.getString("mode") } ?: CrewRosterMode.MEMBERS
        CrewRosterScreen(
            viewModel(factory = CrewScreenViewModel.Factory), mode, onBack = back,
            onOpenLeader = { navController.navigate(CrewRoutes.leader(id)) },
            onOpenMember = { userId -> navController.navigate(CrewRoutes.person(id, userId)) },
            onTransfer = { navController.navigate(CrewRoutes.transfer(id)) },
        )
    }
    composable(
        CrewRoutes.PERSON,
        arguments = listOf(crewIdArg, navArgument("userId") { type = NavType.StringType }),
    ) { entry ->
        val id = entry.arguments?.getString("crewId").orEmpty()
        CrewPersonScreen(
            viewModel(factory = CrewScreenViewModel.Factory),
            userId = entry.arguments?.getString("userId").orEmpty(),
            onBack = back,
            onOpenCrew = { navController.showCrew(id, CrewRoutes.PERSON) },
            onTransfer = { userId, name -> navController.navigate(CrewRoutes.transfer(id, userId, name)) },
            onRemoved = back,
        )
    }
    composable(CrewRoutes.GOAL, arguments = listOf(crewIdArg)) { entry ->
        val id = entry.arguments?.getString("crewId").orEmpty()
        CrewGoalScreen(
            viewModel(factory = CrewScreenViewModel.Factory), onBack = back,
            onParticipants = { navController.navigate(CrewRoutes.members(id, CrewRosterMode.WEEK)) },
            onSetGoal = { navController.navigate(CrewRoutes.goalEdit(id)) },
        )
    }
    composable(CrewRoutes.GOAL_EDIT, arguments = listOf(crewIdArg)) { entry ->
        val id = entry.arguments?.getString("crewId").orEmpty()
        CrewGoalEditScreen(
            viewModel(factory = CrewScreenViewModel.Factory), onBack = back,
            onApplied = {
                // 83 바뀐 달성률 — 뒤에 이미 목표 화면이 있으면 거기로, 없으면 이 화면 대신
                if (!navController.popBackStack(CrewRoutes.goal(id), inclusive = false)) {
                    navController.navigate(CrewRoutes.goal(id)) { popUpTo(CrewRoutes.GOAL_EDIT) { inclusive = true } }
                }
            },
        )
    }
    composable(CrewRoutes.JOIN, arguments = listOf(crewIdArg)) { entry ->
        val id = entry.arguments?.getString("crewId").orEmpty()
        CrewJoinScreen(
            viewModel(factory = CrewScreenViewModel.Factory), onBack = back,
            onPending = { applicationId ->
                navController.navigate(CrewRoutes.application(id, applicationId)) { popUpTo(CrewRoutes.JOIN) { inclusive = true } }
            },
            onJoinedNow = { navController.navigate(CrewRoutes.joined(id)) { popUpTo(CrewRoutes.JOIN) { inclusive = true } } },
            onShowCurrent = { navController.showCrew(id, CrewRoutes.JOIN) },
        )
    }
    composable(
        CrewRoutes.APPLICATION,
        arguments = listOf(crewIdArg, navArgument("applicationId") { type = NavType.LongType }),
    ) { entry ->
        val id = entry.arguments?.getString("crewId").orEmpty()
        CrewPendingScreen(
            viewModel(factory = CrewScreenViewModel.Factory),
            applicationId = entry.arguments?.getLong("applicationId") ?: 0L,
            onBack = { navController.showCrew(id, CrewRoutes.APPLICATION) },
            onBackToCrew = { navController.showCrew(id, CrewRoutes.APPLICATION) },
            onResult = { applicationId ->
                navController.navigate(CrewRoutes.result(id, applicationId)) { popUpTo(CrewRoutes.APPLICATION) { inclusive = true } }
            },
        )
    }
    composable(
        CrewRoutes.RESULT,
        arguments = listOf(crewIdArg, navArgument("applicationId") { type = NavType.LongType }),
    ) { entry ->
        val id = entry.arguments?.getString("crewId").orEmpty()
        CrewResultScreen(
            viewModel(factory = CrewScreenViewModel.Factory),
            applicationId = entry.arguments?.getLong("applicationId") ?: 0L,
            onBack = back,
            onOpenCrew = { navController.showCrew(id, CrewRoutes.RESULT) },
            onOtherCrews = toList,
        )
    }
    composable(CrewRoutes.JOINED, arguments = listOf(crewIdArg)) { entry ->
        val id = entry.arguments?.getString("crewId").orEmpty()
        val vm: CrewScreenViewModel = viewModel(factory = CrewScreenViewModel.Factory)
        val card by vm.card.collectAsStateWithLifecycle()
        CrewJoinedNowScreen(card, onBack = back, onOpenCrew = { navController.showCrew(id, CrewRoutes.JOINED) })
    }
    composable(CrewRoutes.REPORTED, arguments = listOf(crewIdArg)) {
        CrewReportedScreen(onBack = back, onDone = back)
    }
    composable(
        CrewRoutes.MANAGE,
        arguments = listOf(crewIdArg, navArgument("created") { type = NavType.BoolType; defaultValue = false }),
    ) { entry ->
        val id = entry.arguments?.getString("crewId").orEmpty()
        val created = entry.arguments?.getBoolean("created") == true
        CrewManageScreen(
            viewModel(factory = CrewScreenViewModel.Factory),
            created = created,
            actions = CrewManageActions(
                // 77 만든 직후의 관리 화면은 뒤로 가면 목록
                onBack = if (created) toList else back,
                onRequests = { navController.navigate(CrewRoutes.requests(id)) },
                onMembers = { navController.navigate(CrewRoutes.members(id, CrewRosterMode.MANAGE)) },
                onEditProfile = { navController.navigate(CrewRoutes.draft(CrewDraftMode.EDIT_PROFILE, id)) },
                onEditRunning = { navController.navigate(CrewRoutes.draft(CrewDraftMode.EDIT_RUNNING, id)) },
                onEditRecruit = { navController.navigate(CrewRoutes.draft(CrewDraftMode.EDIT_RECRUIT, id)) },
                onGoal = { navController.navigate(CrewRoutes.goalEdit(id)) },
                onSettings = { navController.navigate(CrewRoutes.settings(id)) },
                onList = toList,
            ),
        )
    }
    composable(CrewRoutes.REQUESTS, arguments = listOf(crewIdArg)) { entry ->
        val id = entry.arguments?.getString("crewId").orEmpty()
        CrewRequestsScreen(
            viewModel(factory = CrewScreenViewModel.Factory), onBack = back,
            onOpen = { applicationId -> navController.navigate(CrewRoutes.review(id, applicationId)) },
            onManage = {
                if (!navController.popBackStack(CrewRoutes.manage(id), inclusive = false)) {
                    navController.navigate(CrewRoutes.manage(id)) { popUpTo(CrewRoutes.REQUESTS) { inclusive = true } }
                }
            },
        )
    }
    composable(
        CrewRoutes.REVIEW,
        arguments = listOf(crewIdArg, navArgument("applicationId") { type = NavType.LongType }),
    ) { entry ->
        val id = entry.arguments?.getString("crewId").orEmpty()
        CrewReviewScreen(
            viewModel(factory = CrewScreenViewModel.Factory),
            applicationId = entry.arguments?.getLong("applicationId") ?: 0L,
            onBack = back,
            onApplicantProfile = { userId -> navController.navigate(CrewRoutes.person(id, userId)) },
            onDecided = { approved, name, members, capacity, pending ->
                navController.navigate(CrewRoutes.decided(id, approved, name, members, capacity, pending)) {
                    popUpTo(CrewRoutes.REVIEW) { inclusive = true }
                }
            },
            onRecruitSettings = { navController.navigate(CrewRoutes.draft(CrewDraftMode.EDIT_RECRUIT, id)) },
        )
    }
    composable(
        CrewRoutes.DECIDED,
        arguments = listOf(
            crewIdArg,
            navArgument("approved") { type = NavType.BoolType; defaultValue = true },
            navArgument("name") { type = NavType.StringType; defaultValue = "" },
            navArgument("members") { type = NavType.IntType; defaultValue = 0 },
            navArgument("capacity") { type = NavType.IntType; defaultValue = -1 },
            navArgument("pending") { type = NavType.IntType; defaultValue = 0 },
        ),
    ) { entry ->
        val id = entry.arguments?.getString("crewId").orEmpty()
        val args = entry.arguments
        CrewDecidedScreen(
            approved = args?.getBoolean("approved") != false,
            name = args?.getString("name").orEmpty(),
            crewName = ServiceLocator.crewCards.cardNow(id)?.name.orEmpty(),
            members = args?.getInt("members") ?: 0,
            capacity = args?.getInt("capacity")?.takeIf { it >= 0 },
            pending = args?.getInt("pending") ?: 0,
            onBack = back,
            // 51 · 53 남은 신청 보기 — 신청 목록(79)은 돌아오면 다시 읽는다
            onRemaining = {
                if (!navController.popBackStack(CrewRoutes.requests(id), inclusive = false)) {
                    navController.navigate(CrewRoutes.requests(id)) { popUpTo(CrewRoutes.DECIDED) { inclusive = true } }
                }
            },
        )
    }
    composable(CrewRoutes.SETTINGS, arguments = listOf(crewIdArg)) { entry ->
        val id = entry.arguments?.getString("crewId").orEmpty()
        CrewSettingsScreen(
            viewModel(factory = CrewScreenViewModel.Factory),
            CrewSettingsActions(
                onBack = back,
                onTransfer = { navController.navigate(CrewRoutes.transfer(id)) },
                onRecruitChanged = { navController.showCrew(id, CrewRoutes.SETTINGS) },
                onDissolved = toList,
            ),
        )
    }
    composable(
        CrewRoutes.TRANSFER,
        arguments = listOf(
            crewIdArg,
            navArgument("userId") { type = NavType.StringType; defaultValue = "" },
            navArgument("name") { type = NavType.StringType; defaultValue = "" },
        ),
    ) { entry ->
        val id = entry.arguments?.getString("crewId").orEmpty()
        CrewTransferScreen(
            viewModel(factory = CrewScreenViewModel.Factory),
            presetUserId = entry.arguments?.getString("userId")?.takeIf { it.isNotBlank() },
            presetName = entry.arguments?.getString("name")?.takeIf { it.isNotBlank() },
            onBack = back,
            // 59 · 86 — 넘긴 뒤에는 멤버로 보는 상세
            onDone = { navController.showCrew(id, CrewRoutes.TRANSFER) },
        )
    }
    composable(CrewRoutes.CREATED, arguments = listOf(crewIdArg)) { entry ->
        val id = entry.arguments?.getString("crewId").orEmpty()
        CrewCreatedScreen(
            viewModel(factory = CrewScreenViewModel.Factory),
            onBack = toList,
            onManage = { navController.navigate(CrewRoutes.manage(id, created = true)) { popUpTo(CrewRoutes.CREATED) { inclusive = true } } },
        )
    }
    composable(
        CrewRoutes.DRAFT,
        arguments = listOf(
            navArgument("mode") { type = NavType.StringType; defaultValue = CrewDraftMode.CREATE.name },
            navArgument("crewId") { type = NavType.StringType; defaultValue = "" },
            navArgument("resume") { type = NavType.BoolType; defaultValue = false },
        ),
    ) {
        CrewDraftRoute(navController, CrewRoutes.DRAFT)
    }
    composable(CrewRoutes.RECRUIT) {
        CrewRecruitEntryScreen(
            onBack = back,
            onRecruitSettings = { id -> navController.navigate(CrewRoutes.draft(CrewDraftMode.EDIT_RECRUIT, id)) },
            onManage = { id -> navController.navigate(CrewRoutes.manage(id)) },
            onCreate = { resume -> navController.navigate(CrewRoutes.draft(CrewDraftMode.CREATE, resume = resume)) },
        )
    }
    composable(
        CrewRoutes.REGION,
        arguments = listOf(navArgument("target") { type = NavType.StringType; defaultValue = CrewRegionTarget.LIST.name }),
    ) { entry ->
        val target = CrewRegionTarget.entries.firstOrNull { it.name == entry.arguments?.getString("target") } ?: CrewRegionTarget.LIST
        val scope = rememberCoroutineScope()
        CrewRegionScreen(
            viewModel(factory = CrewRegionViewModel.Factory),
            target = target,
            onBack = back,
            onPicked = { area ->
                if (target == CrewRegionTarget.DRAFT) {
                    if (area != null) {
                        navController.previousBackStackEntry?.savedStateHandle?.set(CrewDraftViewModel.PICK_REGION, CrewDraftCodec.encodeArea(area))
                    }
                    navController.popBackStack()
                } else {
                    // 목록은 고른 지역(없으면 내 위치)을 기준으로 다시 센다 — 저장한 뒤 화면 스레드에서 돌아간다
                    scope.launch {
                        ServiceLocator.crewCards.setRegion(area)
                        withContext(Dispatchers.Main.immediate) { navController.popBackStack() }
                    }
                }
            },
        )
    }
}

/** 만들기 · 수정 초안 화면 — 예전 "모임 만들기" 길도 같은 화면(만들기)을 연다. [route] 는 이 화면이 올라간 길 */
@androidx.compose.runtime.Composable
fun CrewDraftRoute(navController: NavHostController, route: String) {
    val vm: CrewDraftViewModel = viewModel(factory = CrewDraftViewModel.Factory)
    CrewDraftScreen(
        vm,
        CrewDraftActions(
            onBack = { navController.popBackStack() },
            onPickRegion = { navController.navigate(CrewRoutes.region(CrewRegionTarget.DRAFT)) },
            onGoalEdit = { navController.navigate(CrewRoutes.goalEdit(vm.draft.value.crewId)) },
            onCreated = { crewId -> navController.navigate(CrewRoutes.created(crewId)) { popUpTo(route) { inclusive = true } } },
            onSaved = { navController.popBackStack() },
        ),
    )
}
