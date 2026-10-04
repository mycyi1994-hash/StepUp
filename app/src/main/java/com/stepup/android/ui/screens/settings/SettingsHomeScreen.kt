package com.stepup.android.ui.screens.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.width
import androidx.compose.ui.draw.clip
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.text.style.TextAlign
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
import com.stepup.android.ui.components.BlueGroup
import com.stepup.android.ui.components.BlueGroupLabel
import com.stepup.android.ui.components.BlueNavRow
import com.stepup.android.ui.components.BluePage
import com.stepup.android.ui.components.BlueSheet
import com.stepup.android.ui.components.BlueSheetDivider
import com.stepup.android.ui.components.BlueSwitchRow
import com.stepup.android.ui.components.RunButton
import com.stepup.android.ui.components.RunNumber
import com.stepup.android.ui.components.blueListColors
import com.stepup.android.ui.components.runTextStyle
import com.stepup.android.ui.components.runTone
import com.stepup.android.ui.experience.feedbackClickable
import com.stepup.android.ui.theme.ThemeMode
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

/**
 * 설정 첫 화면(설정 v1 01 → 파란 톤 v4 SET01 · SET29) — 러닝과 알림 · 앱 사용 · 계정과 도움말, 세 묶음의 짧은 목록.
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

    BluePage(title = stringResource(R.string.profile_tab_settings), onBack = onBack) {
        item {
            BlueGroupLabel(stringResource(R.string.set_group_running))
            BlueGroup {
                BlueNavRow(
                    stringResource(R.string.set_goal), onClick = { goalOpen = true },
                    // 읽기 전에는 "—"(예시 숫자를 실제 목표처럼 보이지 않는다)
                    value = goal?.let { stringResource(R.string.set_goal_value, "%,d".format(it)) } ?: "—",
                    modifier = Modifier.testTag("settings-goal"),
                )
                BlueNavRow(stringResource(R.string.set_notifications), onClick = onOpenNotificationSettings,
                    modifier = Modifier.testTag("settings-notifications"))
                BlueNavRow(stringResource(R.string.set_privacy), onClick = onOpenPrivacy,
                    modifier = Modifier.testTag("settings-privacy"))
            }
        }
        item {
            BlueGroupLabel(stringResource(R.string.set_group_app))
            BlueGroup {
                BlueNavRow(stringResource(R.string.set_language), onClick = onOpenLanguage,
                    value = languageValue(), modifier = Modifier.testTag("settings-language"))
                BlueNavRow(stringResource(R.string.set_theme), onClick = onOpenTheme,
                    value = themeValue(AppTheme.mode), modifier = Modifier.testTag("settings-theme"))
                BlueNavRow(stringResource(R.string.set_experience), onClick = onOpenExperience,
                    modifier = Modifier.testTag("settings-experience"))
            }
        }
        item {
            BlueGroupLabel(stringResource(R.string.set_group_account))
            BlueGroup {
                BlueNavRow(stringResource(R.string.set_connected), onClick = onOpenConnected,
                    modifier = Modifier.testTag("settings-connected"))
                BlueNavRow(stringResource(R.string.set_support), onClick = onOpenSupport,
                    modifier = Modifier.testTag("settings-support"))
            }
        }
        // SET29 — 예전 설정 목록에만 있던 길(덜 쓰는 기능은 안쪽으로, 지우지 않는다)
        item {
            BlueGroupLabel(stringResource(R.string.set_group_more))
            BlueGroup {
                BlueNavRow(stringResource(R.string.settings_inbox), onClick = onOpenInbox,
                    modifier = Modifier.testTag("settings-inbox"))
                BlueNavRow(stringResource(R.string.profile_achievements), onClick = onOpenAchievements,
                    modifier = Modifier.testTag("settings-achievements"))
                BlueNavRow(stringResource(R.string.invite_title), onClick = onOpenInvite, modifier = Modifier.testTag("settings-invite"))
                BlueNavRow(stringResource(R.string.settings_body), onClick = onOpenBody, modifier = Modifier.testTag("settings-body"))
                BlueNavRow(stringResource(R.string.settings_mode), onClick = onOpenMode, modifier = Modifier.testTag("settings-mode"))
            }
        }
        item {
            BlueGroupLabel(stringResource(R.string.set_group_about))
            BlueGroup {
                // 데모 모드 — 서버 없이 화면을 둘러보는 모드. 운영 데이터와 섞이지 않는다
                BlueSwitchRow(
                    stringResource(R.string.settings_demo), stringResource(R.string.settings_demo_note), demo,
                    onCheckedChange = { on -> scope.launch { ServiceLocator.avatarRepository.setDemoMode(on) } },
                    modifier = Modifier.testTag("settings-demo"),
                )
                // 실제 빌드 이름(시안의 1.0.0 은 예시)
                AboutLine(stringResource(R.string.about_version), "StepUp " + com.stepup.android.core.TestUpdates.buildLabel)
                // 테스트 APK 에서만 — 운영 빌드에는 나타나지 않는다
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
    val t = runTone()
    Row(
        Modifier.fillMaxWidth().background(blueListColors().face).heightIn(min = 60.dp)
            .padding(horizontal = 20.dp, vertical = 12.dp).semantics(mergeDescendants = true) {},
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Text(label, style = runTextStyle(18.sp, t.text, FontWeight.SemiBold), modifier = Modifier.weight(1f, fill = false))
        Spacer(Modifier.width(12.dp))
        Text(value, style = runTextStyle(15.sp, t.label, FontWeight.Medium), textAlign = TextAlign.End)
    }
}

/** 테스트 APK 에서만 — 새 빌드를 지금 확인한다. 있으면 앱 위에 업데이트 창이 뜬다 */
@Composable
private fun TestUpdateLine() {
    val t = runTone()
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
        Modifier.fillMaxWidth().background(blueListColors().face).heightIn(min = 56.dp).testTag("test-update-check")
            .feedbackClickable(onClick = { com.stepup.android.core.TestUpdates.startCheck(force = true) })
            .padding(horizontal = 20.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Text(stringResource(R.string.test_update_row), style = runTextStyle(16.sp, t.label, FontWeight.Medium), modifier = Modifier.weight(1f, fill = false))
        Spacer(Modifier.width(12.dp))
        Text(status, style = runTextStyle(15.sp, t.cyan, FontWeight.SemiBold))
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

    val t = runTone()
    BlueSheet(
        title = stringResource(R.string.set_goal),
        onDismiss = onDismiss,
        modifier = Modifier.testTag("goal-sheet"),
        centered = true, compactTitle = true,
        actions = { RunButton(stringResource(R.string.set_close), onDismiss, Modifier.testTag("goal-close")) },
    ) {
        Row(verticalAlignment = Alignment.Bottom, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            // 읽기 전엔 "—" — 기본값을 저장된 목표처럼 보이지 않는다
            RunNumber(
                current?.let { "%,d".format(it) } ?: "—", size = 60.sp, color = t.cyan,
                valueTag = "goal-value",
            )
            Text(stringResource(R.string.set_goal_steps), style = runTextStyle(18.sp, t.label, FontWeight.Medium),
                modifier = Modifier.padding(bottom = 10.dp))
        }
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            val decrease = stringResource(R.string.set_goal_decrease)
            val increase = stringResource(R.string.set_goal_increase)
            GoalStep(
                stringResource(R.string.set_goal_minus), onClick = { change(-500) },
                enabled = current != null && current > UserPrefs.MIN_GOAL,
                modifier = Modifier.weight(1f).semantics { contentDescription = decrease }.testTag("goal-minus"),
            )
            GoalStep(
                stringResource(R.string.set_goal_plus), onClick = { change(500) },
                enabled = current != null && current < UserPrefs.MAX_GOAL,
                modifier = Modifier.weight(1f).semantics { contentDescription = increase }.testTag("goal-plus"),
            )
        }
        Text(stringResource(R.string.set_goal_range, "%,d".format(UserPrefs.MIN_GOAL), "%,d".format(UserPrefs.MAX_GOAL)),
            style = runTextStyle(14.sp, t.label, FontWeight.Medium), textAlign = TextAlign.Center)
        // 계산 안내 — 기존 계산식의 예상이며 지급 확정이 아니다. 읽기 전엔 "—"
        BlueSheetDivider()
        GoalFact(stringResource(R.string.set_goal_distance),
            current?.let { stringResource(R.string.set_goal_distance_value, "%.1f".format(RewardEconomy.distanceMeters(it) / 1000)) } ?: "—")
        BlueSheetDivider()
        GoalFact(stringResource(R.string.set_goal_bonus),
            current?.let { stringResource(R.string.set_goal_bonus_value, "%,.1f".format(RewardEconomy.goalBaseBonus(it))) } ?: "—")
        BlueSheetDivider()
        Text(
            stringResource(if (failed) R.string.set_goal_save_failed else R.string.set_goal_note),
            style = runTextStyle(15.sp, if (failed) t.dangerText else t.label, if (failed) FontWeight.SemiBold else FontWeight.Medium),
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth().semantics { liveRegion = LiveRegionMode.Polite }.testTag("goal-note"),
        )
    }
}

/** −500 · +500 — 파란 테두리 칸. 끝값이면 누를 수 없다 */
@Composable
private fun GoalStep(label: String, onClick: () -> Unit, enabled: Boolean, modifier: Modifier = Modifier) {
    val t = runTone()
    val c = blueListColors()
    val shape = androidx.compose.foundation.shape.RoundedCornerShape(14.dp)
    Box(
        modifier.heightIn(min = 60.dp).clip(shape).background(c.face, shape)
            .border(1.5.dp, if (enabled) t.secondaryEdge else t.divider, shape)
            .feedbackClickable(enabled = enabled, role = Role.Button, onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Text(label, style = runTextStyle(26.sp, if (enabled) t.text else t.muted, FontWeight.Bold))
    }
}

@Composable
private fun GoalFact(label: String, value: String) {
    val t = runTone()
    Row(Modifier.fillMaxWidth().heightIn(min = 40.dp).semantics(mergeDescendants = true) {}, verticalAlignment = Alignment.CenterVertically) {
        Text(label, style = runTextStyle(16.sp, t.text, FontWeight.SemiBold), modifier = Modifier.weight(1f))
        Text(value, style = runTextStyle(16.sp, t.text, FontWeight.Medium))
    }
}
