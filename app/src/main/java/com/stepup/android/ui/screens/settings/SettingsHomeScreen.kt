package com.stepup.android.ui.screens.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.stepup.android.R
import com.stepup.android.core.AppLocale
import com.stepup.android.core.AppTheme
import com.stepup.android.core.ServiceLocator
import com.stepup.android.data.prefs.UserPrefs
import com.stepup.android.domain.RewardEconomy
import com.stepup.android.ui.components.DetailPage
import com.stepup.android.ui.components.SettingsGroupLabel
import com.stepup.android.ui.components.SettingsNavRow
import com.stepup.android.ui.components.SettingsNote
import com.stepup.android.ui.components.SettingsPrimaryButton
import com.stepup.android.ui.components.SettingsSecondaryButton
import com.stepup.android.ui.components.SettingsSheet
import com.stepup.android.ui.components.SettingsSwitchRow
import com.stepup.android.ui.components.settingsPalette
import com.stepup.android.ui.experience.feedbackClickable
import com.stepup.android.ui.theme.StepUpNumbers
import com.stepup.android.ui.theme.ThemeMode
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

/**
 * 설정 첫 화면(설정 v1 01) — 러닝과 알림 · 앱 사용 · 계정과 도움말, 세 그룹의 짧은 목록.
 *
 * 줄마다 이름과 현재 값만 두고, 자세한 설명은 들어간 화면에서 한다. 프로필 수정과 지갑은 내 정보 첫 화면(러닝 패스)에
 * 그대로 있어 여기 다시 두지 않는다. 예전 설정 목록에 있던 알림함 · 업적 · 친구 초대 · 신체 정보 · 모드와 앱 정보(버전 ·
 * 데모 모드)는 지우지 않고 세 그룹 아래 "더 보기" · "앱 정보"로 옮겼다(덜 쓰는 기능은 안쪽으로).
 */
@Composable
fun SettingsHomeScreen(
    onBack: () -> Unit = {},
    onOpenNotificationSettings: () -> Unit = {},
    onOpenPrivacy: () -> Unit = {},
    onOpenLanguage: () -> Unit = {},
    onOpenTheme: () -> Unit = {},
    onOpenExperience: () -> Unit = {},
    onOpenConnected: () -> Unit = {},
    onOpenSupport: () -> Unit = {},
    onOpenInbox: () -> Unit = {},
    onOpenAchievements: () -> Unit = {},
    onOpenInvite: () -> Unit = {},
    onOpenBody: () -> Unit = {},
    onOpenMode: () -> Unit = {},
) {
    val goal by ServiceLocator.userPrefs.dailyGoal.collectAsStateWithLifecycle(initialValue = null)
    val demo by ServiceLocator.avatarRepository.demoMode.collectAsStateWithLifecycle(initialValue = false)
    val scope = rememberCoroutineScope()
    var goalOpen by rememberSaveable { mutableStateOf(false) }

    DetailPage(title = stringResource(R.string.profile_tab_settings), onBack = onBack) {
        item {
            Column(Modifier.fillMaxWidth()) {
                SettingsGroupLabel(stringResource(R.string.set_group_running))
                SettingsNavRow(
                    stringResource(R.string.set_goal), onClick = { goalOpen = true },
                    value = goal?.let { stringResource(R.string.set_goal_value, "%,d".format(it)) } ?: "—",
                    modifier = Modifier.testTag("settings-goal"),
                )
                SettingsNavRow(stringResource(R.string.set_notifications), onClick = onOpenNotificationSettings,
                    modifier = Modifier.testTag("settings-notifications"))
                SettingsNavRow(stringResource(R.string.set_privacy), onClick = onOpenPrivacy,
                    modifier = Modifier.testTag("settings-privacy"))
            }
        }
        item {
            Column(Modifier.fillMaxWidth()) {
                SettingsGroupLabel(stringResource(R.string.set_group_app))
                SettingsNavRow(stringResource(R.string.set_language), onClick = onOpenLanguage,
                    value = languageValue(), modifier = Modifier.testTag("settings-language"))
                SettingsNavRow(stringResource(R.string.set_theme), onClick = onOpenTheme,
                    value = themeValue(AppTheme.mode), modifier = Modifier.testTag("settings-theme"))
                SettingsNavRow(stringResource(R.string.set_experience), onClick = onOpenExperience,
                    modifier = Modifier.testTag("settings-experience"))
            }
        }
        item {
            Column(Modifier.fillMaxWidth()) {
                SettingsGroupLabel(stringResource(R.string.set_group_account))
                SettingsNavRow(stringResource(R.string.set_connected), onClick = onOpenConnected,
                    modifier = Modifier.testTag("settings-connected"))
                SettingsNavRow(stringResource(R.string.set_support), onClick = onOpenSupport,
                    modifier = Modifier.testTag("settings-support"))
            }
        }
        // 예전 설정 목록에만 있던 길 — 지우지 않는다
        item {
            Column(Modifier.fillMaxWidth()) {
                SettingsGroupLabel(stringResource(R.string.set_group_more))
                SettingsNavRow(stringResource(R.string.settings_inbox), onClick = onOpenInbox,
                    modifier = Modifier.testTag("settings-inbox"))
                SettingsNavRow(stringResource(R.string.profile_achievements), onClick = onOpenAchievements)
                SettingsNavRow(stringResource(R.string.invite_title), onClick = onOpenInvite)
                SettingsNavRow(stringResource(R.string.settings_body), onClick = onOpenBody)
                SettingsNavRow(stringResource(R.string.settings_mode), onClick = onOpenMode)
            }
        }
        item {
            Column(Modifier.fillMaxWidth()) {
                SettingsGroupLabel(stringResource(R.string.set_group_about))
                // 데모 모드 — 서버 없이 화면을 둘러보는 모드. 운영 데이터와 섞이지 않는다
                SettingsSwitchRow(
                    stringResource(R.string.settings_demo), stringResource(R.string.settings_demo_note), demo,
                    onCheckedChange = { on -> scope.launch { ServiceLocator.avatarRepository.setDemoMode(on) } },
                )
                AboutLine(stringResource(R.string.about_version), "StepUp " + com.stepup.android.core.TestUpdates.buildLabel)
                if (com.stepup.android.core.TestUpdates.enabled) TestUpdateLine()
                AboutLine(stringResource(R.string.about_network), stringResource(R.string.about_network_value))
            }
        }
    }

    if (goalOpen) {
        DailyGoalSheet(onDismiss = { goalOpen = false })
    }
}

@Composable
private fun languageValue(): String = when (AppLocale.tag) {
    "en" -> "English"
    "ko" -> "한국어"
    "zh" -> "中文"
    "ja" -> "日本語"
    else -> stringResource(R.string.set_follow_device)
}

@Composable
internal fun themeValue(mode: ThemeMode): String = stringResource(
    when (mode) {
        ThemeMode.SYSTEM -> R.string.set_follow_device
        ThemeMode.DARK -> R.string.set_theme_dark
        ThemeMode.LIGHT -> R.string.set_theme_light
    },
)

@Composable
private fun AboutLine(label: String, value: String) {
    val p = settingsPalette()
    Row(
        Modifier.fillMaxWidth().heightIn(min = 44.dp).semantics(mergeDescendants = true) {},
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Text(label, color = p.secondary, fontSize = 14.sp)
        Text(value, color = p.text, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
    }
}

/** 테스트 APK 에서만 — 새 빌드를 지금 확인한다. 있으면 앱 위에 업데이트 창이 뜬다 */
@Composable
private fun TestUpdateLine() {
    val p = settingsPalette()
    val state by com.stepup.android.core.TestUpdates.state.collectAsState()
    val status = when (state) {
        com.stepup.android.core.TestUpdates.State.Checking -> stringResource(R.string.test_update_checking)
        com.stepup.android.core.TestUpdates.State.UpToDate -> stringResource(R.string.test_update_latest)
        com.stepup.android.core.TestUpdates.State.CheckFailed -> stringResource(R.string.test_update_check_failed)
        is com.stepup.android.core.TestUpdates.State.Available,
        is com.stepup.android.core.TestUpdates.State.Dismissed,
        is com.stepup.android.core.TestUpdates.State.Failed -> stringResource(R.string.test_update_found)
        is com.stepup.android.core.TestUpdates.State.Downloading -> stringResource(R.string.test_update_downloading_short)
        com.stepup.android.core.TestUpdates.State.Idle -> stringResource(R.string.test_update_check)
    }
    Row(
        Modifier.fillMaxWidth().heightIn(min = 48.dp).testTag("test-update-check")
            .feedbackClickable(onClick = { com.stepup.android.core.TestUpdates.startCheck(force = true) }),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Text(stringResource(R.string.test_update_row), color = p.secondary, fontSize = 14.sp)
        Text(status, color = p.accent, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
    }
}

/**
 * 하루 걸음 목표(설정 v1 24) — 큰 숫자와 −500 · +500. 범위는 기존 그대로 3,000~20,000걸음, 500걸음씩.
 *
 * 누를 때마다 바로 저장한다(기존 저장 길 StepRepository.setDailyGoal — 폰에 적고 서버 목표도 맞춘다).
 * 빨리 여러 번 눌러도 마지막 값이 남는다(앞선 저장은 새 값이 오면 접는다). 저장하지 못하면 실제로 저장된 목표로
 * 되돌리고 알린다. 목표를 바꾸는 것만으로 SUP 를 주지 않는다 — 예상 거리와 기본 보너스는 기존 계산식의 안내다.
 */
@Composable
fun DailyGoalSheet(
    onDismiss: () -> Unit,
    save: suspend (Int) -> Unit = { ServiceLocator.stepRepository.setDailyGoal(it) },
) {
    val stored by ServiceLocator.userPrefs.dailyGoal.collectAsStateWithLifecycle(initialValue = null)
    var shown by rememberSaveable { mutableStateOf<Int?>(null) }
    var pending by remember { mutableStateOf<Int?>(null) }
    var failed by remember { mutableStateOf(false) }
    val current = shown ?: stored
    LaunchedEffect(Unit) {
        snapshotFlow { pending }.filterNotNull().collectLatest { value ->
            try {
                save(value)
                failed = false
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (_: Exception) {
                failed = true
                // 실제로 저장된 값으로 되돌린다
                shown = ServiceLocator.userPrefs.dailyGoal.first()
            }
        }
    }
    fun change(delta: Int) {
        val base = current ?: return
        val next = (base + delta).coerceIn(UserPrefs.MIN_GOAL, UserPrefs.MAX_GOAL)
        if (next == base) return
        shown = next
        pending = next
    }

    val p = settingsPalette()
    SettingsSheet(
        title = stringResource(R.string.set_goal),
        onDismiss = onDismiss,
        modifier = Modifier.testTag("goal-sheet"),
        actions = { SettingsPrimaryButton(stringResource(R.string.set_close), onDismiss, Modifier.fillMaxWidth().testTag("goal-close")) },
    ) {
        Row(verticalAlignment = Alignment.Bottom, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            Text(current?.let { "%,d".format(it) } ?: "—", color = p.text, fontSize = 58.sp, fontFamily = StepUpNumbers,
                fontWeight = FontWeight.SemiBold, modifier = Modifier.testTag("goal-value"))
            Text(stringResource(R.string.set_goal_steps), color = p.secondary, fontSize = 18.sp,
                modifier = Modifier.padding(bottom = 10.dp))
        }
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            val decrease = stringResource(R.string.set_goal_decrease)
            val increase = stringResource(R.string.set_goal_increase)
            SettingsSecondaryButton(
                stringResource(R.string.set_goal_minus), onClick = { change(-500) },
                enabled = current != null && current > UserPrefs.MIN_GOAL,
                modifier = Modifier.weight(1f).semantics { contentDescription = decrease }.testTag("goal-minus"),
            )
            SettingsSecondaryButton(
                stringResource(R.string.set_goal_plus), onClick = { change(500) },
                enabled = current != null && current < UserPrefs.MAX_GOAL,
                modifier = Modifier.weight(1f).semantics { contentDescription = increase }.testTag("goal-plus"),
            )
        }
        Text(stringResource(R.string.set_goal_range, "%,d".format(UserPrefs.MIN_GOAL), "%,d".format(UserPrefs.MAX_GOAL)),
            color = p.secondary, fontSize = 13.sp)
        val steps = current ?: UserPrefs.DEFAULT_GOAL
        GoalFact(stringResource(R.string.set_goal_distance),
            stringResource(R.string.set_goal_distance_value, "%.1f".format(RewardEconomy.distanceMeters(steps) / 1000)))
        GoalFact(stringResource(R.string.set_goal_bonus),
            stringResource(R.string.set_goal_bonus_value, "%,.1f".format(RewardEconomy.goalBaseBonus(steps))))
        SettingsNote(
            stringResource(if (failed) R.string.set_goal_save_failed else R.string.set_goal_note),
            Modifier.testTag("goal-note"),
        )
    }
}

@Composable
private fun GoalFact(label: String, value: String) {
    val p = settingsPalette()
    Row(Modifier.fillMaxWidth().semantics(mergeDescendants = true) {}, verticalAlignment = Alignment.CenterVertically) {
        Text(label, color = p.secondary, fontSize = 15.sp, modifier = Modifier.weight(1f))
        Text(value, color = p.text, fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
    }
}
