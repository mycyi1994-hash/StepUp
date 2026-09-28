package com.stepup.android.domain

/*
 * 시작·로그인·첫 사용 v1(2026-09-28 전달본, docs/redesign/onboarding-v1) — 화면 없이 판단만 하는 규칙.
 *
 * 권한을 허용했는지는 여기에 적어 두지 않는다. 늘 OS 에서 다시 읽은 값을 받는다 — 앱이 남기는 것은
 * "물어봤다 · 골랐다"뿐이다(걸음 권한 창을 띄운 이력, 위치 안내에서 고른 적, 알림 안내에서 고른 적).
 */

/** 러닝을 고른 뒤 차례로 보이는 권한 안내(시안 13~19). 시스템 권한 창은 OS 가 그린다 */
enum class RunPermissionSheet {
    /** 13 걸음 기록을 허용해 주세요 */
    ActivityRationale,

    /** 14 거절했지만 시스템 창을 다시 띄울 수 있다 */
    ActivityDenied,

    /** 15 시스템 창을 다시 띄울 수 없다 — 휴대폰 설정에서 */
    ActivitySettings,

    /** 16 달린 길도 기록할까요?(선택) */
    LocationRationale,

    /** 17 대략적인 위치만 허용했다 — 그대로 계속할 수 있다 */
    ApproximateLocation,

    /** 18 위치 없이 — 경로 없이 계속 */
    WithoutLocation,

    /** 19 러닝 소식(알림, 선택) */
    NotificationRationale,
}

/**
 * 걸음 권한 시스템 창을 띄운 이력.
 *
 * shouldShowRequestPermissionRationale 가 false 인 것만으로는 "다시 묻지 않음"인지 "한 번도 묻지 않음"인지
 * 알 수 없다. 실제로 창을 띄운 결과를 함께 적어 두고 판단한다 — 처음 묻기도 전에 설정으로 보내지 않는다.
 */
enum class ActivityRequestHistory { NEVER, ASKED, BLOCKED }

/** 지금 OS 가 알려 준 권한 + 이 앱이 남긴 선택 기록 */
data class RunPermissionState(
    val api: Int,
    val activityGranted: Boolean,
    val preciseLocation: Boolean,
    val approximateLocation: Boolean,
    val notificationsGranted: Boolean,
    val activityHistory: ActivityRequestHistory = ActivityRequestHistory.NEVER,
    /** OS 가 지금 설명을 권하는가(shouldShowRequestPermissionRationale) — 이 값 하나로 거절을 단정하지 않는다 */
    val activityRationale: Boolean = false,
    /** 위치 안내(16)에서 이미 골랐다 — 허용 · 거절 · 경로 없이 계속 */
    val locationChosen: Boolean = false,
    /** 알림 안내(19)에서 이미 골랐다 — 허용 · 거절 · 나중에 */
    val notificationChosen: Boolean = false,
) {
    val hasLocation: Boolean get() = preciseLocation || approximateLocation
}

/** 다음에 할 일 — 안내 하나를 보이거나, 러닝 준비(3-2-1)로 한 번 넘어간다 */
sealed interface RunPermissionStep {
    data class Show(val sheet: RunPermissionSheet) : RunPermissionStep

    /** 걸음 권한이 있고 선택 단계도 끝났다. 러닝 서비스는 여기서 켜지 않는다 — 3-2-1 이 끝나야 켠다 */
    data object Ready : RunPermissionStep
}

object RunPermissionRules {
    /** 신체 활동 권한을 실행 중에 묻는 첫 판(Android 10) */
    const val ACTIVITY_API = 29

    /** 알림 권한을 실행 중에 묻는 첫 판(Android 13) */
    const val NOTIFICATION_API = 33

    /**
     * 활동 → 위치 → 알림 차례. 이미 허용했거나 이미 고른 선택 권한(위치 · 알림)은 건너뛴다 —
     * 허용한 사람에게 매번 안내 세 장을 보이지 않는다. 위치 · 알림을 거절해도 러닝은 막지 않는다.
     */
    fun next(s: RunPermissionState): RunPermissionStep = when {
        s.api >= ACTIVITY_API && !s.activityGranted -> RunPermissionStep.Show(
            // 창을 띄웠더니 막혀 있었고, OS 도 설명을 권하지 않는다 — 시스템 창이 다시 뜨지 않는다
            if (s.activityHistory == ActivityRequestHistory.BLOCKED && !s.activityRationale) RunPermissionSheet.ActivitySettings
            else RunPermissionSheet.ActivityRationale,
        )
        !s.hasLocation && !s.locationChosen -> RunPermissionStep.Show(RunPermissionSheet.LocationRationale)
        s.api >= NOTIFICATION_API && !s.notificationsGranted && !s.notificationChosen ->
            RunPermissionStep.Show(RunPermissionSheet.NotificationRationale)
        else -> RunPermissionStep.Ready
    }

    /**
     * 걸음 권한 시스템 창의 결과. null 이면 허용 — [next] 로 잇는다.
     *
     * - 거절했고 OS 가 설명을 권한다 → 다시 물을 수 있다(14).
     * - 창을 띄우기 전에는 설명을 권했는데 지금은 아니다 → 방금 "다시 묻지 않음"이 됐다(15).
     * - 전에도 띄운 적이 있는데 둘 다 아니다 → 이미 막혀 있어 창이 뜨지 않았다(15).
     * - 처음 띄운 창을 고르지 않고 닫았다(Android 11+) → 설명 권유 없이 거절로 오지만 다시 물을 수 있다(14).
     */
    fun afterActivityRequest(
        granted: Boolean,
        rationaleBefore: Boolean,
        rationaleAfter: Boolean,
        history: ActivityRequestHistory,
    ): RunPermissionSheet? = when {
        granted -> null
        rationaleAfter -> RunPermissionSheet.ActivityDenied
        rationaleBefore || history != ActivityRequestHistory.NEVER -> RunPermissionSheet.ActivitySettings
        else -> RunPermissionSheet.ActivityDenied
    }

    /** 걸음 권한 창을 띄운 뒤 남길 이력 */
    fun historyAfter(outcome: RunPermissionSheet?): ActivityRequestHistory =
        if (outcome == RunPermissionSheet.ActivitySettings) ActivityRequestHistory.BLOCKED else ActivityRequestHistory.ASKED

    /** 위치 시스템 창의 결과. null 이면 정확한 위치 — [next] 로 잇는다(실제 GPS 신호는 러닝 화면이 따로 기다린다) */
    fun afterLocationRequest(precise: Boolean, approximate: Boolean): RunPermissionSheet? = when {
        precise -> null
        approximate -> RunPermissionSheet.ApproximateLocation
        else -> RunPermissionSheet.WithoutLocation
    }

    /**
     * 설정 · 다른 앱에서 돌아왔을 때 — OS 를 다시 읽는다. 보던 안내의 권한이 풀렸으면 다음으로 잇고,
     * 그대로면(거절 · 바꾸지 않음) 그 안내를 그대로 둔다. 성공으로 짐작하지 않는다.
     */
    fun afterReturn(showing: RunPermissionSheet?, s: RunPermissionState): RunPermissionStep = when (showing) {
        null -> next(s)
        RunPermissionSheet.ActivityRationale, RunPermissionSheet.ActivityDenied, RunPermissionSheet.ActivitySettings ->
            if (s.api < ACTIVITY_API || s.activityGranted) next(s) else RunPermissionStep.Show(showing)
        RunPermissionSheet.LocationRationale -> if (s.hasLocation) next(s) else RunPermissionStep.Show(showing)
        RunPermissionSheet.ApproximateLocation, RunPermissionSheet.WithoutLocation -> when {
            s.preciseLocation -> next(s)
            s.approximateLocation -> RunPermissionStep.Show(RunPermissionSheet.ApproximateLocation)
            else -> RunPermissionStep.Show(RunPermissionSheet.WithoutLocation)
        }
        RunPermissionSheet.NotificationRationale ->
            if (s.api < NOTIFICATION_API || s.notificationsGranted) next(s) else RunPermissionStep.Show(showing)
    }
}

/** 첫 사용 안내(시안 02)에서 고른 것 */
enum class FirstGuideAction {
    /** 러닝 시작 — 러닝 방법 고르기(12)로. 운동 기록은 아직 시작하지 않는다 */
    StartRun,

    /** 먼저 둘러보기 */
    Browse,

    /** X · 뒤로 · 바깥 · 아래로 끌어 닫기 — 먼저 둘러보기와 같다 */
    Dismiss,
}

object FirstGuideRules {
    /**
     * 첫 안내를 지금 보일까 — 아직 안 봤고, 러닝 홈에 있고, 먼저 처리할 외부 목적지(초대 링크 · 로그인 뒤 다시 열 초대)나
     * 멈춘 러닝이 없을 때만. 목적지가 있으면 그 화면을 먼저 보이고 안내는 다음에 홈에 올 때로 미룬다.
     */
    fun shouldShow(pending: Boolean, onHome: Boolean, destinationPending: Boolean, recoveryPending: Boolean): Boolean =
        pending && onHome && !destinationPending && !recoveryPending

    /**
     * 사람이 고른 행동이면 모두 "봤음"으로 적는다 — 닫아도 다시 열리지 않게. 앱 종료 · 강제 중단은 고른 행동이 아니므로
     * 적지 않는다(다음 실행에서 다시 보인다).
     */
    @Suppress("UNUSED_PARAMETER")
    fun savesSeen(action: FirstGuideAction): Boolean = true

    /** 러닝 방법 고르기로 가는 것은 "러닝 시작"뿐이다 */
    fun opensRunMenu(action: FirstGuideAction): Boolean = action == FirstGuideAction.StartRun
}

/**
 * 첫 러닝 홈(시안 11) — 걸음 권한 전이고, 이 계정의 러닝 기록 · 누적 걸음 · 오늘 활동이 모두 없을 때만.
 * 읽는 중(null)이면 판단하지 않는다 — 기록이 있는 사람에게 "첫 러닝"이라고 쓰지 않는다.
 */
object FirstHomeRules {
    fun applies(noActivityToday: Boolean, activityGranted: Boolean, runCount: Int?, lifetimeSteps: Long): Boolean =
        noActivityToday && !activityGranted && runCount == 0 && lifetimeSteps == 0L
}
