package com.stepup.android.ui.screens.coaching

import android.net.Uri
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.composable
import androidx.navigation.navArgument

/**
 * AI 코칭(2026-10-16 인계 16장)의 길. 러닝 탭 아래 상세 화면이다(하단 탭은 그대로, 머리는 화면이 공용 RunTopBar 로 그린다).
 * 고른 기록은 이 휴대폰의 기록 번호로 싣는다 — 서버에는 보내지 않는다(서버는 시작 시각으로 가리킨다).
 * 코칭으로 들어오는 자리(러닝 홈 · 시작 메뉴 · 기록 상세 중 어디)는 아직 정하지 않았다 — 정할 때 그 화면에서 [home] 을 연다.
 */
object CoachingRoutes {
    /** 01 · 03 · 04 · 07–12 · 16 — 탭(today · analysis · change)과 고른 기록(없으면 -1) */
    const val HOME = "coaching?tab={tab}&run={run}"

    /** 02 훈련 상세 */
    const val WORKOUT = "coaching/workout/{template}"

    /** 05 첫 사용 동의 */
    const val CONSENT = "coaching/consent"

    /** 06 코칭 목표 */
    const val GOALS = "coaching/goals"

    /** 13 다음 주 계획 */
    const val PLAN = "coaching/plan"

    /** 14 AI 코칭 설정 · 15 해제 확인 */
    const val SETTINGS = "coaching/settings"

    /** 코칭 화면 모두 — 공통 머리 · 탭 규칙(AppChromePolicy)과 바닥색에 쓴다 */
    val ALL: List<String> = listOf(HOME, WORKOUT, CONSENT, GOALS, PLAN, SETTINGS)

    fun home(tab: CoachTab = CoachTab.TODAY, run: Long? = null) = "coaching?tab=${tab.key}&run=${run ?: -1L}"
    fun workout(templateId: String) = "coaching/workout/${Uri.encode(templateId)}"
}

/**
 * 코칭 화면들을 길에 올린다. [onGoRun] 은 기존 러닝 시작 흐름(시작 메뉴 · 달리던 러닝), [onStartFreeRun] 은 훈련 상세의
 * "이 훈련으로 시작"(기존 자유 러닝 시작), [onOpenRecord] 는 기존 지난 러닝 상세(기본 기록). 코칭은 러닝을 스스로 시작하지 않는다.
 */
fun NavGraphBuilder.coachingGraph(
    navController: NavHostController,
    onGoRun: () -> Unit,
    onStartFreeRun: () -> Unit,
    onOpenRecord: (Long) -> Unit,
) {
    val back: () -> Unit = { navController.popBackStack() }
    val toGoals: () -> Unit = { navController.navigate(CoachingRoutes.GOALS) { launchSingleTop = true } }
    val toConsent: () -> Unit = { navController.navigate(CoachingRoutes.CONSENT) { launchSingleTop = true } }

    composable(
        CoachingRoutes.HOME,
        arguments = listOf(
            navArgument("tab") { type = NavType.StringType; defaultValue = CoachTab.TODAY.key },
            navArgument("run") { type = NavType.LongType; defaultValue = -1L },
        ),
    ) { entry ->
        val tab = CoachTab.of(entry.arguments?.getString("tab"))
        val run = entry.arguments?.getLong("run")?.takeIf { it > 0 }
        CoachingHomeScreen(
            viewModel(factory = CoachingHubViewModel.factory(run)),
            startTab = tab,
            actions = CoachingActions(
                onBack = back,
                onSettings = { navController.navigate(CoachingRoutes.SETTINGS) { launchSingleTop = true } },
                onConsent = toConsent,
                onGoals = toGoals,
                onWorkout = { id -> navController.navigate(CoachingRoutes.workout(id)) },
                onPlan = { navController.navigate(CoachingRoutes.PLAN) { launchSingleTop = true } },
                onGoRun = onGoRun,
                onOpenRecord = onOpenRecord,
            ),
        )
    }
    composable(CoachingRoutes.WORKOUT, arguments = listOf(navArgument("template") { type = NavType.StringType })) { entry ->
        CoachingWorkoutScreen(
            templateId = entry.arguments?.getString("template").orEmpty(),
            viewModel = viewModel(factory = CoachingSetupViewModel.Factory),
            onBack = back,
            onStart = onStartFreeRun,
        )
    }
    composable(CoachingRoutes.CONSENT) {
        CoachingConsentScreen(
            viewModel(factory = CoachingSetupViewModel.Factory),
            onBack = back,
            // 동의했으면 목표가 없을 때만 목표(06)로 — 동의 화면은 걷는다(뒤로 가면 들어온 곳)
            onGranted = { hasGoal ->
                if (hasGoal) {
                    navController.popBackStack()
                } else {
                    navController.navigate(CoachingRoutes.GOALS) { popUpTo(CoachingRoutes.CONSENT) { inclusive = true } }
                }
            },
        )
    }
    composable(CoachingRoutes.GOALS) {
        // 저장만 한다 — 러닝을 시작하지 않고 들어온 곳(코칭 오늘 · 설정)으로 돌아간다
        CoachingGoalsScreen(viewModel(factory = CoachingSetupViewModel.Factory), onBack = back, onSaved = back)
    }
    composable(CoachingRoutes.PLAN) {
        CoachingPlanScreen(viewModel(factory = CoachingSetupViewModel.Factory), onBack = back, onGoals = toGoals)
    }
    composable(CoachingRoutes.SETTINGS) {
        CoachingSettingsScreen(viewModel(factory = CoachingSetupViewModel.Factory), onBack = back, onConsent = toConsent, onGoals = toGoals)
    }
}
