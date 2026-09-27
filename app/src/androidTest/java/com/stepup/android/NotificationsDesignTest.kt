package com.stepup.android

import android.content.res.Configuration
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.requiredWidth
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import androidx.test.platform.app.InstrumentationRegistry
import com.stepup.android.core.ServiceLocator
import com.stepup.android.data.local.NotificationEntity
import com.stepup.android.data.local.NotificationType
import com.stepup.android.data.repo.Announcement
import com.stepup.android.data.repo.NoticeAction
import com.stepup.android.data.repo.NoticeBoard
import com.stepup.android.data.repo.NoticeDetail
import com.stepup.android.ui.MainScaffold
import com.stepup.android.ui.Routes
import com.stepup.android.ui.components.S2Stage
import com.stepup.android.ui.experience.ExperienceProvider
import com.stepup.android.ui.screens.notifications.InboxLoad
import com.stepup.android.ui.screens.notifications.InboxToast
import com.stepup.android.ui.screens.notifications.InviteStep
import com.stepup.android.ui.screens.notifications.MissingTargetSheet
import com.stepup.android.ui.screens.notifications.NoticeDetailContent
import com.stepup.android.ui.screens.notifications.NotifTab
import com.stepup.android.ui.screens.notifications.NotificationSheet
import com.stepup.android.ui.screens.notifications.NotificationSheetActions
import com.stepup.android.ui.screens.notifications.NotificationsContent
import com.stepup.android.ui.screens.notifications.NotificationsUi
import com.stepup.android.ui.theme.StepUpTheme
import com.stepup.android.ui.theme.ThemeMode
import java.io.File
import java.time.ZonedDateTime
import java.util.Locale
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

/**
 * 알림·공지 v1(2026-09-28 전달본, docs/redesign/notifications-v1) — 장면 번호는 시안의 01~23 그대로.
 *
 * [notificationsInTheApp] 은 앱 셸 안에서 이 기기의 알림함으로 간다 — 들어온 것만으로 읽음이 되지 않는지, 연 알림만
 * 읽음이 되는지, "모두 읽음"이 행을 지우거나 초대를 처리하지 않는지를 저장소에서 확인한다. 초대 수락은 서버로 가므로
 * 여기서 누르지 않는다(보기 · 거절 확인 · 취소만). [notificationStates] 는 서버 결과(가입 신청 · 실패 · 모름 · 로그인 필요)와
 * 공지 상태처럼 기기에서 만들 수 없는 장면을 화면에 바로 넣어 찍는다. 장면의 글은 전달본 sample-content.json(미리 보기 전용)이다.
 */
class NotificationsDesignTest {
    @get:Rule(order = 0) val appLanguage = object : org.junit.rules.ExternalResource() {
        private var previous = ""
        override fun before() {
            val instrumentation = InstrumentationRegistry.getInstrumentation()
            val context = instrumentation.targetContext
            com.stepup.android.core.AppLocale.syncFromSystem(context)
            previous = com.stepup.android.core.AppLocale.tag
            instrumentation.runOnMainSync { com.stepup.android.core.AppLocale.change(context, "ko") }
        }
        override fun after() {
            val instrumentation = InstrumentationRegistry.getInstrumentation()
            instrumentation.runOnMainSync { com.stepup.android.core.AppLocale.change(instrumentation.targetContext, previous) }
        }
    }
    @get:Rule(order = 1) val compose = createAndroidComposeRule<ComponentActivity>()

    private val directory get() = File(compose.activity.getExternalFilesDir(null), "notifications-v1").apply { mkdirs() }

    @Test fun notificationsInTheApp() {
        val dao = ServiceLocator.database.notificationDao()
        val now = System.currentTimeMillis()
        val yesterdayEvening = ZonedDateTime.now().minusDays(1).withHour(19).withMinute(0).toInstant().toEpochMilli()
        val ids = runBlocking {
            dao.clear()
            listOf(
                note(NotificationType.COMMENT_REPLY, "민수", now - 12 * 60_000L, extra = "901:1"),
                note(NotificationType.SNEAKER_MINTED, "WATER:RARE:0", now - 60 * 60_000L, amount = 1.0),
                note(NotificationType.REWARD_EARNED, "2400", now - 2 * 60 * 60_000L, amount = 2.4),
                note(NotificationType.CREW_INVITE, "여의도 러너스", yesterdayEvening, extra = CREW_ID, read = true),
            ).forEach { dao.insert(it) }
            dao.observeAll(100).first().associateBy({ it.type }, { it.id })
        }
        try {
            edgeToEdge()
            var restart by mutableIntStateOf(0)
            compose.setContent {
                StepUpTheme(ThemeMode.DARK) {
                    ExperienceProvider { key(restart) { MainScaffold(initialRoute = Routes.NOTIFICATIONS) } }
                }
            }
            awaitTag("inbox-list")
            // 01 — 들어온 것만으로 읽음이 되지 않는다: 새 알림 셋이 그대로, 점과 "읽지 않음"
            awaitTag("notif-unread-${ids.getValue(NotificationType.REWARD_EARNED)}")
            compose.onNodeWithTag("notif-row-${ids.getValue(NotificationType.REWARD_EARNED)}")
                .assert(SemanticsMatcher.expectValue(SemanticsProperties.StateDescription, korean(R.string.inbox_unread)))
            compose.onNodeWithTag("inbox-summary").assertTextEquals(korean(R.string.inbox_new_count).format(3))
            shot("01-inbox")
            assertEquals(3, runBlocking { dao.observeUnreadCount().first() })

            // 03 — 적립 알림을 열면 그 알림만 읽음(행 · 처리 여부 그대로)
            tap("notif-row-${ids.getValue(NotificationType.REWARD_EARNED)}")
            awaitTag("sheet-reward")
            compose.waitUntil(5_000) { runBlocking { dao.byId(ids.getValue(NotificationType.REWARD_EARNED)) }?.read == true }
            shot("03-reward-detail")
            assertEquals(2, runBlocking { dao.observeUnreadCount().first() })
            back()
            awaitGone("sheet-reward")

            // 04 — 신발 알림: 보유 신발로 가는 길만(신발을 주거나 신기지 않는다)
            tap("notif-row-${ids.getValue(NotificationType.SNEAKER_MINTED)}")
            awaitTag("sheet-shoe")
            shot("04-shoe-detail")
            back()
            awaitGone("sheet-shoe")

            // 05 · 21 — 크루 초대: 여는 것만으로 수락하지 않는다. 거절은 확인을 거쳐서만, 취소하면 초대로 돌아온다
            tap("notif-row-${ids.getValue(NotificationType.CREW_INVITE)}")
            awaitTag("sheet-crew-invite")
            compose.onNodeWithTag("invite-view-crew").assertExists()
            shot("05-crew-invite")
            tap("invite-decline")
            awaitTag("sheet-decline")
            shot("21-decline-invite")
            tap("decline-cancel")
            awaitTag("sheet-crew-invite")
            back()
            awaitGone("sheet-crew-invite")
            runBlocking {
                val invite = dao.byId(ids.getValue(NotificationType.CREW_INVITE))
                assertTrue("초대는 그대로 남는다", invite != null && !invite.actioned)
            }

            // 02 — 모두 읽음: 행은 남고 초대는 처리되지 않는다
            tap("inbox-read-all")
            awaitTag("settings-toast")
            compose.waitUntil(5_000) { runBlocking { dao.observeUnreadCount().first() } == 0 }
            shot("02-read-all")
            compose.onNodeWithTag("inbox-summary").assertTextEquals(korean(R.string.inbox_all_read))
            runBlocking {
                val rows = dao.observeAll(100).first()
                assertEquals(4, rows.size)
                assertTrue(rows.all { it.read } && rows.none { it.actioned })
            }

            // 15 — 공지: 이 서버의 실제 상태(글 · 빈 목록 · 못 불러옴 중 하나). 예시 공지를 넣지 않는다
            tap("inbox-tab-notices")
            // 서버에 닿지 못하면 연결 · 응답 제한 시간(각 20초)까지 기다린다
            compose.waitUntil(45_000) {
                listOf("notice-list", "notice-failed").any { compose.onAllNodesWithTag(it).fetchSemanticsNodes().isNotEmpty() }
            }
            shot("15-announcements-live")
            tap("inbox-tab-inbox")
            awaitTag("inbox-list")

            // 설정 → 기존 알림 설정
            tap("inbox-settings")
            compose.waitUntil(10_000) {
                listOf("notif-push", "settings-loading", "settings-load-failed")
                    .any { compose.onAllNodesWithTag(it).fetchSemanticsNodes().isNotEmpty() }
            }
            back()
            awaitTag("inbox-list")

            // 다시 들어와도(앱을 다시 그려도) 읽음 상태는 저장된 그대로
            compose.runOnIdle { restart++ }
            awaitTag("inbox-list")
            compose.onNodeWithTag("inbox-summary").assertTextEquals(korean(R.string.inbox_all_read))
            compose.onAllNodesWithTag("notif-unread-${ids.getValue(NotificationType.COMMENT_REPLY)}", useUnmergedTree = true)
                .assertCountEquals(0)
        } finally {
            runBlocking { dao.clear() }
        }
    }

    /** 기기에서 만들 수 없는 상태 — 서버 결과 · 공지 상태 · 밝은 테마 · 큰 글씨 · 좁은 폭 · 영어 */
    @Test fun notificationStates() {
        var light by mutableStateOf(false)
        var large by mutableStateOf(false)
        var narrow by mutableStateOf(false)
        var english by mutableStateOf(false)
        edgeToEdge()
        compose.setContent {
            val density = LocalDensity.current
            val base = LocalContext.current
            val baseConfig = LocalConfiguration.current
            val config = if (english) Configuration(baseConfig).apply { setLocale(Locale.ENGLISH) } else baseConfig
            val context = if (english) base.createConfigurationContext(config) else base
            CompositionLocalProvider(
                LocalDensity provides Density(density.density, if (large) 1.3f else 1f),
                LocalContext provides context,
                LocalConfiguration provides config,
            ) {
                StepUpTheme(if (light) ThemeMode.LIGHT else ThemeMode.DARK) {
                    ExperienceProvider {
                        Box(Modifier.fillMaxSize()) {
                            S2Stage(Modifier.fillMaxSize())
                            Box(
                                Modifier.then(if (narrow) Modifier.requiredWidth(320.dp).fillMaxHeight() else Modifier.fillMaxSize())
                                    .align(Alignment.TopCenter).statusBarsPadding(),
                            ) { scene() }
                        }
                    }
                }
            }
        }
        val now = System.currentTimeMillis()
        val yesterday = ZonedDateTime.now().minusDays(1).withHour(19).withMinute(0).toInstant().toEpochMilli()
        val reply = note(NotificationType.COMMENT_REPLY, "민수", now - 12 * 60_000L, extra = "901:1", id = 1)
        val shoe = note(NotificationType.SNEAKER_MINTED, "WATER:RARE:0", now - 60 * 60_000L, amount = 1.0, id = 2)
        val reward = note(NotificationType.REWARD_EARNED, "2400", now - 2 * 60 * 60_000L, amount = 2.4, id = 3)
        val invite = note(NotificationType.CREW_INVITE, "여의도 러너스", yesterday, extra = CREW_ID, read = true, id = 4)
        val party = note(NotificationType.PARTY_INVITE, "여의도 러너스", yesterday, extra = CREW_ID, id = 5)
        val oldReward = note(NotificationType.EVENT_REWARD, "Welcome Runner", yesterday, amount = 30.0, extra = "welcome", id = 6)
        val inbox = InboxLoad.Ready(listOf(reply, shoe, reward, invite), newestId = 4)
        val ui = NotificationsUi(inbox = inbox, unread = 3, now = now)
        val allRead = InboxLoad.Ready(inbox.items.map { it.copy(read = true) }, newestId = 4)
        val notices = sampleNotices(now)

        fun list(state: NotificationsUi): @Composable () -> Unit = { NotificationsContent(state) }
        fun sheet(entity: NotificationEntity, step: InviteStep?, actions: NotificationSheetActions = NotificationSheetActions()):
            @Composable () -> Unit = {
                NotificationsContent(ui)
                NotificationSheet(entity, step, actions)
            }

        show("01b-inbox-sample", "inbox-list", list(ui))
        show("02b-read-all-sample", "settings-toast",
            list(ui.copy(inbox = allRead, unread = 0, toast = InboxToast(R.string.inbox_marked_all, success = true, seq = 1))))
        compose.onAllNodesWithTag("inbox-read-all").assertCountEquals(0)
        // 13 — 읽음 저장 실패: 점과 새 알림 수는 저장된 그대로
        show("13-read-error", "settings-toast",
            list(ui.copy(toast = InboxToast(R.string.inbox_mark_all_failed, success = false, seq = 2))))
        compose.onNodeWithTag("inbox-summary").assertTextEquals(korean(R.string.inbox_new_count).format(3))
        show("10-empty-inbox", "inbox-empty", list(ui.copy(inbox = InboxLoad.Ready(emptyList(), 0), unread = 0)))
        show("11-inbox-loading", "inbox-loading", list(ui.copy(inbox = InboxLoad.Loading)))
        compose.onAllNodesWithTag("inbox-empty").assertCountEquals(0)
        show("12-inbox-error", "inbox-failed", list(ui.copy(inbox = InboxLoad.Failed)))

        // 초대 — 서버 결과마다
        var viewed = ""
        var accepted = false
        show("05b-crew-invite-view", "invite-view-crew",
            sheet(invite, InviteStep.Idle, NotificationSheetActions(viewCrew = { viewed = it }, acceptCrew = { accepted = true })))
        tap("invite-view-crew")
        compose.runOnIdle {
            assertEquals("크루 보기는 그 크루로 갈 뿐", CREW_ID, viewed)
            assertFalse("크루 보기로 수락하지 않는다", accepted)
        }
        show("05c-invite-busy", "invite-accept", sheet(invite, InviteStep.Busy))
        compose.onNodeWithTag("invite-accept").assertIsNotEnabled()
        compose.onNodeWithTag("invite-decline").assertIsNotEnabled()
        show("06-membership-requested", "invite-requested", sheet(invite.copy(actioned = true), InviteStep.Requested))
        show("06b-membership-joined", "invite-joined", sheet(invite.copy(actioned = true), InviteStep.Joined))
        show("07-invite-error", "invite-failed", sheet(invite, InviteStep.Failed))
        compose.onNodeWithTag("invite-accept").assertTextContains(korean(R.string.invite_retry_accept))
        // 결과를 모르면 다시 수락하지 않고 가입 상태부터 확인한다
        show("07b-invite-unknown", "invite-unknown", sheet(invite, InviteStep.Unknown))
        compose.onAllNodesWithTag("invite-accept").assertCountEquals(0)
        compose.onNodeWithTag("invite-check-status").assertExists()
        show("08-party-invite", "invite-view-lobby", sheet(party, InviteStep.Idle))
        show("08b-party-marked", "invite-marked", sheet(party.copy(actioned = true), InviteStep.Marked))
        show("09-unverified-reward", "sheet-unverified-reward", sheet(oldReward, null))
        show("14-target-unavailable", "sheet-missing") {
            NotificationsContent(ui)
            MissingTargetSheet(onOpenCommunity = {}, onBack = {})
        }
        show("21b-decline-failed", "decline-failed", sheet(invite, InviteStep.DeclineFailed))
        show("22-login-required", "sheet-login", sheet(invite, InviteStep.SignIn))

        // 공지
        val board = NoticeBoard(items = notices)
        show("15-announcements", "notice-list", list(ui.copy(tab = NotifTab.Notices, notices = board)))
        show("17-empty-announcements", "notice-empty", list(ui.copy(tab = NotifTab.Notices, notices = NoticeBoard(items = emptyList()))))
        show("18-announcement-error", "notice-failed",
            list(ui.copy(tab = NotifTab.Notices, notices = NoticeBoard(refreshFailed = true))))
        show("20-announcement-loading", "notice-loading",
            list(ui.copy(tab = NotifTab.Notices, notices = NoticeBoard(loading = true))))
        compose.onAllNodesWithTag("notice-empty").assertCountEquals(0)
        show("23-cached-announcements", "notice-stale",
            list(ui.copy(tab = NotifTab.Notices, notices = board.copy(refreshFailed = true))))
        compose.onNodeWithTag("notice-row-1").assertExists()
        var action: NoticeAction? = null
        show("16-announcement-detail", "notice-action") {
            NoticeDetailContent(NoticeDetail.Ready(notices.first()), onAction = { action = it })
        }
        tap("notice-action")
        compose.runOnIdle { assertEquals(NoticeAction.DRAW, action) }
        show("16b-announcement-saved", "notice-saved") { NoticeDetailContent(NoticeDetail.Ready(notices[1], saved = true)) }
        compose.onAllNodesWithTag("notice-action").assertCountEquals(1)
        show("19-announcement-unavailable", "notice-unavailable") { NoticeDetailContent(NoticeDetail.Unavailable) }

        // 밝은 테마 · 큰 글씨 · 좁은 폭 · 영어
        compose.runOnIdle { light = true }
        show("30-light-inbox", "inbox-list", list(ui))
        show("31-light-crew-invite", "sheet-crew-invite", sheet(invite, InviteStep.Idle))
        compose.runOnIdle { light = false; large = true }
        show("32-large-font-inbox", "inbox-list", list(ui))
        show("33-large-font-notice", "notice-detail") { NoticeDetailContent(NoticeDetail.Ready(notices.first())) }
        compose.runOnIdle { large = false; narrow = true }
        show("34-narrow-inbox", "inbox-list", list(ui))
        compose.runOnIdle { narrow = false; english = true }
        show("35-english-inbox", "inbox-list", list(ui))
        compose.onNodeWithText("My alerts").assertExists()
        compose.runOnIdle { english = false }
    }

    // ── 도우미 ─────────────────────────────────────────────────────

    private fun note(
        type: String,
        text: String,
        timestamp: Long,
        amount: Double = 0.0,
        extra: String = "",
        read: Boolean = false,
        id: Long = 0,
    ) = NotificationEntity(id = id, timestamp = timestamp, type = type, argText = text, argAmount = amount,
        argExtra = extra, read = read, actioned = false)

    /** 전달본 sample-content.json 의 공지 넷 — 미리 보기 전용(앱 · 서버에 넣지 않는다) */
    private fun sampleNotices(now: Long): List<Announcement> {
        val day = 86_400_000L
        return listOf(
            Announcement(
                1, mapOf("ko" to "무료 뽑기 이용 안내", "en" to "How free draws work"),
                mapOf(
                    "ko" to "## 무료 뽑기\n처음 가입하면 10회\n매일 무료 뽑기 3회\n\n## 상급 뽑기\nWEB3 지갑을 연결하면 추가 10회\n" +
                        "연결한 상태에서 1km 러닝마다 1회\n\n---\n지갑 연결 기능이 준비되면\n상급 뽑기를 이용할 수 있어요.\n\n" +
                        "무료 뽑기는 지갑 연결 없이 이용할 수 있어요.",
                ),
                now, NoticeAction.DRAW,
            ),
            Announcement(2, mapOf("ko" to "러닝 기록을 확인하는 방법"),
                mapOf("ko" to "내 정보에서 내 러닝 기록 보기를 눌러 주세요.\n저장된 러닝 기록을 누르면 해당 러닝의 상세를 확인할 수 있어요."),
                now - day, NoticeAction.RUN_HISTORY),
            Announcement(3, mapOf("ko" to "앱 알림이 오지 않을 때"),
                mapOf("ko" to "앱의 알림 설정과 휴대폰의 알림 허용 상태를 확인해 주세요."), now - 2 * day, NoticeAction.NOTIFICATION_SETTINGS),
            Announcement(4, mapOf("ko" to "위치 권한과 러닝 경로"),
                mapOf("ko" to "러닝 경로를 기록하려면 앱의 위치 권한을 확인해 주세요."), now - 3 * day, NoticeAction.PRIVACY_SETTINGS),
        )
    }

    /** 지금 찍는 장면 — [notificationStates] 의 화면 틀 안에 들어간다 */
    private var scene by mutableStateOf<@Composable () -> Unit>({})

    private fun show(name: String, readyTag: String, content: @Composable () -> Unit) {
        compose.runOnIdle { scene = content }
        awaitTag(readyTag)
        shot(name)
    }

    private fun tap(tag: String) {
        awaitTag(tag)
        val node = compose.onNodeWithTag(tag)
        runCatching { node.performScrollTo() }
        node.performClick()
        compose.waitForIdle()
    }

    private fun back() {
        InstrumentationRegistry.getInstrumentation().sendKeyDownUpSync(android.view.KeyEvent.KEYCODE_BACK)
        compose.waitForIdle()
    }

    private fun korean(id: Int): String {
        val config = Configuration(compose.activity.resources.configuration).apply { setLocale(Locale.KOREAN) }
        return compose.activity.createConfigurationContext(config).getString(id)
    }

    private fun edgeToEdge() {
        runBlocking {
            ServiceLocator.userPrefs.setReducedMotion(true)
            ServiceLocator.userPrefs.setSounds(false)
            ServiceLocator.userPrefs.setHaptics(false)
            ServiceLocator.userPrefs.setGuideSeen()
        }
        compose.activityRule.scenario.onActivity {
            it.enableEdgeToEdge(
                statusBarStyle = SystemBarStyle.dark(android.graphics.Color.TRANSPARENT),
                navigationBarStyle = SystemBarStyle.dark(android.graphics.Color.TRANSPARENT),
            )
        }
    }

    private fun awaitTag(tag: String) {
        compose.waitUntil(10_000) { compose.onAllNodesWithTag(tag, useUnmergedTree = true).fetchSemanticsNodes().isNotEmpty() }
    }

    private fun awaitGone(tag: String) {
        compose.waitUntil(10_000) { compose.onAllNodesWithTag(tag, useUnmergedTree = true).fetchSemanticsNodes().isEmpty() }
    }

    private fun shot(name: String, settle: Long = 700) {
        compose.waitForIdle()
        InstrumentationRegistry.getInstrumentation().waitForIdleSync()
        Thread.sleep(settle)
        captureDisplay(File(directory, "$name.png"))
    }

    private companion object {
        const val CREW_ID = "00000000-0000-0000-0000-0000000000c1"
    }
}
