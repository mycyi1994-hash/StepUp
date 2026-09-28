package com.stepup.android

import com.stepup.android.data.remote.GoogleIdResult
import com.stepup.android.data.remote.TokenResult
import com.stepup.android.domain.ActivityRequestHistory
import com.stepup.android.domain.FirstGuideAction
import com.stepup.android.domain.FirstGuideRules
import com.stepup.android.domain.FirstHomeRules
import com.stepup.android.domain.RunPermissionRules
import com.stepup.android.domain.RunPermissionSheet
import com.stepup.android.domain.RunPermissionState
import com.stepup.android.domain.RunPermissionStep
import com.stepup.android.ui.screens.login.LoginNotice
import com.stepup.android.ui.screens.login.LoginRules
import com.stepup.android.ui.screens.login.LoginStep
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/** 시작·로그인·첫 사용 v1 — 화면 없이 판단만 하는 규칙(권한 차례 · 건너뛰기 · 첫 안내 · 로그인 결과) */
class OnboardingRulesTest {
    private fun show(sheet: RunPermissionSheet) = RunPermissionStep.Show(sheet)

    /** Android 14, 아무 권한도 없고 아무것도 고른 적 없는 새 설치 */
    private val fresh = RunPermissionState(
        api = 34, activityGranted = false, preciseLocation = false, approximateLocation = false, notificationsGranted = false,
    )

    // ── 권한 차례 · 건너뛰기 ─────────────────────────────────────

    @Test fun `new install walks activity then location then notifications once each`() {
        assertEquals(show(RunPermissionSheet.ActivityRationale), RunPermissionRules.next(fresh))
        val activity = fresh.copy(activityGranted = true, activityHistory = ActivityRequestHistory.ASKED)
        assertEquals(show(RunPermissionSheet.LocationRationale), RunPermissionRules.next(activity))
        val located = activity.copy(preciseLocation = true, approximateLocation = true, locationChosen = true)
        assertEquals(show(RunPermissionSheet.NotificationRationale), RunPermissionRules.next(located))
        assertEquals(RunPermissionStep.Ready, RunPermissionRules.next(located.copy(notificationChosen = true)))
    }

    @Test fun `people who already allowed everything go straight to the countdown`() {
        val allowed = fresh.copy(activityGranted = true, preciseLocation = true, approximateLocation = true, notificationsGranted = true)
        assertEquals(RunPermissionStep.Ready, RunPermissionRules.next(allowed))
    }

    @Test fun `older android skips the steps it cannot ask`() {
        // Android 9 — 신체 활동 · 알림은 실행 중에 묻지 않는다. 위치만 한 번
        val pie = fresh.copy(api = 28)
        assertEquals(show(RunPermissionSheet.LocationRationale), RunPermissionRules.next(pie))
        assertEquals(RunPermissionStep.Ready, RunPermissionRules.next(pie.copy(locationChosen = true)))
        // Android 12 — 알림 권한이 없다
        val s = fresh.copy(api = 31, activityGranted = true, approximateLocation = true)
        assertEquals(RunPermissionStep.Ready, RunPermissionRules.next(s))
    }

    @Test fun `optional permissions refused or deferred never block the run and are not asked again`() {
        val activity = fresh.copy(activityGranted = true)
        // 위치를 거절했거나 "경로 없이 계속"을 골랐다 — 다시 묻지 않고 알림으로
        assertEquals(show(RunPermissionSheet.NotificationRationale), RunPermissionRules.next(activity.copy(locationChosen = true)))
        // 알림을 거절했거나 "나중에" — 러닝으로
        assertEquals(RunPermissionStep.Ready, RunPermissionRules.next(activity.copy(locationChosen = true, notificationChosen = true)))
    }

    @Test fun `approximate location is respected and not asked again`() {
        val approximate = fresh.copy(activityGranted = true, approximateLocation = true, notificationsGranted = true)
        assertEquals(RunPermissionStep.Ready, RunPermissionRules.next(approximate))
    }

    @Test fun `activity permission is asked every time until allowed`() {
        val refusedBefore = fresh.copy(locationChosen = true, notificationChosen = true, activityHistory = ActivityRequestHistory.ASKED,
            activityRationale = true)
        assertEquals(show(RunPermissionSheet.ActivityRationale), RunPermissionRules.next(refusedBefore))
    }

    @Test fun `never sends people to settings before the first request`() {
        // 처음에는 OS 도 설명을 권하지 않는다(false) — 그것만으로 "다시 묻지 않음"이라 하지 않는다
        assertEquals(show(RunPermissionSheet.ActivityRationale), RunPermissionRules.next(fresh.copy(activityRationale = false)))
        // 실제로 막혀 있음을 확인한 뒤에만 설정으로
        assertEquals(
            show(RunPermissionSheet.ActivitySettings),
            RunPermissionRules.next(fresh.copy(activityHistory = ActivityRequestHistory.BLOCKED, activityRationale = false)),
        )
        // 설정에서 "매번 묻기"로 되돌려 OS 가 다시 설명을 권하면 시스템 창부터
        assertEquals(
            show(RunPermissionSheet.ActivityRationale),
            RunPermissionRules.next(fresh.copy(activityHistory = ActivityRequestHistory.BLOCKED, activityRationale = true)),
        )
    }

    @Test fun `activity request results are classified from real request history`() {
        val never = ActivityRequestHistory.NEVER
        val asked = ActivityRequestHistory.ASKED
        assertNull(RunPermissionRules.afterActivityRequest(granted = true, rationaleBefore = false, rationaleAfter = false, history = never))
        // 거절했고 OS 가 설명을 권한다 — 다시 물을 수 있다(14)
        assertEquals(RunPermissionSheet.ActivityDenied,
            RunPermissionRules.afterActivityRequest(false, rationaleBefore = false, rationaleAfter = true, history = never))
        // 두 번째 거절 · 다시 묻지 않음 — 설정으로(15)
        assertEquals(RunPermissionSheet.ActivitySettings,
            RunPermissionRules.afterActivityRequest(false, rationaleBefore = true, rationaleAfter = false, history = asked))
        // 이미 막혀 창이 뜨지 않았다(15)
        assertEquals(RunPermissionSheet.ActivitySettings,
            RunPermissionRules.afterActivityRequest(false, rationaleBefore = false, rationaleAfter = false, history = asked))
        // 처음 창을 고르지 않고 닫았다 — 다시 물을 수 있다(14)
        assertEquals(RunPermissionSheet.ActivityDenied,
            RunPermissionRules.afterActivityRequest(false, rationaleBefore = false, rationaleAfter = false, history = never))
        assertEquals(ActivityRequestHistory.BLOCKED, RunPermissionRules.historyAfter(RunPermissionSheet.ActivitySettings))
        assertEquals(ActivityRequestHistory.ASKED, RunPermissionRules.historyAfter(RunPermissionSheet.ActivityDenied))
        assertEquals(ActivityRequestHistory.ASKED, RunPermissionRules.historyAfter(null))
    }

    @Test fun `location request results split precise approximate and denied`() {
        assertNull(RunPermissionRules.afterLocationRequest(precise = true, approximate = true))
        assertEquals(RunPermissionSheet.ApproximateLocation, RunPermissionRules.afterLocationRequest(precise = false, approximate = true))
        assertEquals(RunPermissionSheet.WithoutLocation, RunPermissionRules.afterLocationRequest(precise = false, approximate = false))
    }

    @Test fun `returning from settings rereads the os and never assumes success`() {
        val blocked = fresh.copy(activityHistory = ActivityRequestHistory.BLOCKED, locationChosen = true, notificationChosen = true)
        // 바꾸지 않고 돌아왔다 — 같은 안내
        assertEquals(show(RunPermissionSheet.ActivitySettings), RunPermissionRules.afterReturn(RunPermissionSheet.ActivitySettings, blocked))
        // 허용하고 돌아왔다 — 러닝 준비를 잇는다
        assertEquals(RunPermissionStep.Ready, RunPermissionRules.afterReturn(RunPermissionSheet.ActivitySettings, blocked.copy(activityGranted = true)))
        val located = fresh.copy(activityGranted = true, locationChosen = true, notificationChosen = true)
        // 대략적인 위치에서 설정에 다녀왔다 — 정확한 위치면 잇고, 그대로면 17, 위치를 껐으면 18
        assertEquals(RunPermissionStep.Ready,
            RunPermissionRules.afterReturn(RunPermissionSheet.ApproximateLocation, located.copy(preciseLocation = true, approximateLocation = true)))
        assertEquals(show(RunPermissionSheet.ApproximateLocation),
            RunPermissionRules.afterReturn(RunPermissionSheet.ApproximateLocation, located.copy(approximateLocation = true)))
        assertEquals(show(RunPermissionSheet.WithoutLocation), RunPermissionRules.afterReturn(RunPermissionSheet.ApproximateLocation, located))
        assertEquals(show(RunPermissionSheet.ApproximateLocation),
            RunPermissionRules.afterReturn(RunPermissionSheet.WithoutLocation, located.copy(approximateLocation = true)))
        // 알림 안내 중 다른 앱에서 알림을 허용하고 왔다 — 잇는다
        assertEquals(RunPermissionStep.Ready,
            RunPermissionRules.afterReturn(RunPermissionSheet.NotificationRationale, located.copy(notificationChosen = false, notificationsGranted = true)))
        assertEquals(show(RunPermissionSheet.NotificationRationale),
            RunPermissionRules.afterReturn(RunPermissionSheet.NotificationRationale, located.copy(notificationChosen = false)))
    }

    // ── 첫 안내 · 첫 러닝 홈 ─────────────────────────────────────

    @Test fun `first guide waits for home and for pending destinations`() {
        assertTrue(FirstGuideRules.shouldShow(pending = true, onHome = true, destinationPending = false, recoveryPending = false))
        assertFalse(FirstGuideRules.shouldShow(pending = false, onHome = true, destinationPending = false, recoveryPending = false))
        // 초대 링크 · 로그인 뒤 다시 열 초대가 먼저 — 안내는 다음 홈 진입으로
        assertFalse(FirstGuideRules.shouldShow(pending = true, onHome = true, destinationPending = true, recoveryPending = false))
        assertFalse(FirstGuideRules.shouldShow(pending = true, onHome = false, destinationPending = false, recoveryPending = false))
        assertFalse(FirstGuideRules.shouldShow(pending = true, onHome = true, destinationPending = false, recoveryPending = true))
    }

    @Test fun `every guide choice is saved and only start run opens the run menu`() {
        FirstGuideAction.entries.forEach { assertTrue(it.name, FirstGuideRules.savesSeen(it)) }
        assertTrue(FirstGuideRules.opensRunMenu(FirstGuideAction.StartRun))
        assertFalse(FirstGuideRules.opensRunMenu(FirstGuideAction.Browse))
        assertFalse(FirstGuideRules.opensRunMenu(FirstGuideAction.Dismiss))
    }

    @Test fun `first home copy is only for a new user without permission or records`() {
        assertTrue(FirstHomeRules.applies(noActivityToday = true, activityGranted = false, runCount = 0, lifetimeSteps = 0))
        // 기록을 아직 읽지 못했으면 판단하지 않는다
        assertFalse(FirstHomeRules.applies(noActivityToday = true, activityGranted = false, runCount = null, lifetimeSteps = 0))
        // 이전 러닝 · 걸음이 있는 사람에게 "첫 러닝"이라고 쓰지 않는다
        assertFalse(FirstHomeRules.applies(noActivityToday = true, activityGranted = false, runCount = 3, lifetimeSteps = 0))
        assertFalse(FirstHomeRules.applies(noActivityToday = true, activityGranted = false, runCount = 0, lifetimeSteps = 1200))
        assertFalse(FirstHomeRules.applies(noActivityToday = false, activityGranted = false, runCount = 0, lifetimeSteps = 0))
        assertFalse(FirstHomeRules.applies(noActivityToday = true, activityGranted = true, runCount = 0, lifetimeSteps = 0))
    }

    // ── 로그인 결과 → 장면 ───────────────────────────────────────

    @Test fun `login results map to scenes and cancel stays quiet`() {
        assertEquals(LoginStep.Verify("token", "nonce"), LoginRules.afterPick(GoogleIdResult.Ok("token", "nonce"), base = null))
        // 취소 — 오류 없이 01, 다시 로그인하러 왔으면 09 그대로
        assertEquals(LoginStep.Stay(null), LoginRules.afterPick(GoogleIdResult.Cancelled, base = null))
        assertEquals(LoginStep.Stay(LoginNotice.SignInAgain), LoginRules.afterPick(GoogleIdResult.Cancelled, base = LoginNotice.SignInAgain))
        assertEquals(LoginStep.Stay(LoginNotice.NoAccount), LoginRules.afterPick(GoogleIdResult.NoAccount("none"), base = null))
        assertEquals(LoginStep.Stay(LoginNotice.Failed), LoginRules.afterPick(GoogleIdResult.Failed("x"), base = null))
        // 서버가 세션을 준 뒤에만 로그인 완료
        assertEquals(LoginStep.SignedIn, LoginRules.afterServer(TokenResult.Ok("access", "user")))
        // 통신 오류는 연결 안내 — 계정 만료로 단정하지 않는다
        assertEquals(LoginStep.Stay(LoginNotice.Offline), LoginRules.afterServer(TokenResult.Unavailable("timeout")))
        assertEquals(LoginStep.Stay(LoginNotice.Failed), LoginRules.afterServer(TokenResult.SignInRequired("rejected")))
        assertEquals(LoginNotice.SignInAgain, LoginRules.base(signInAgain = true))
        assertNull(LoginRules.base(signInAgain = false))
    }
}
