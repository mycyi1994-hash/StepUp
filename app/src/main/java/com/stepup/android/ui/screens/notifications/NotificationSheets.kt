package com.stepup.android.ui.screens.notifications

import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.Schedule
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import com.stepup.android.R
import com.stepup.android.data.local.NotificationEntity
import com.stepup.android.data.local.NotificationType
import com.stepup.android.ui.components.SettingsPrimaryButton
import com.stepup.android.ui.components.SettingsSecondaryButton
import com.stepup.android.ui.components.SettingsSheet
import com.stepup.android.ui.components.StepUpIcons
import com.stepup.android.ui.components.settingsPalette
import com.stepup.android.ui.experience.LocalMotion
import com.stepup.android.ui.experience.feedbackClickable
import java.time.Instant
import java.time.ZoneId

/** 내용 시트에서 할 수 있는 일 — 화면(내비게이션 · 뷰모델)이 채운다 */
class NotificationSheetActions(
    val dismiss: () -> Unit = {},
    val openWallet: () -> Unit = {},
    val openShoes: () -> Unit = {},
    val openChallenges: () -> Unit = {},
    /** 크루 게시판 — 보기만 한다(수락 아님) */
    val viewCrew: (String) -> Unit = {},
    val acceptCrew: () -> Unit = {},
    val checkCrew: () -> Unit = {},
    /** 대기실 — 보기만 한다(수락 표시 아님) */
    val viewLobby: (String) -> Unit = {},
    val acceptParty: () -> Unit = {},
    val askDecline: () -> Unit = {},
    val cancelDecline: () -> Unit = {},
    val decline: () -> Unit = {},
    val signIn: () -> Unit = {},
    val later: () -> Unit = {},
)

/** 알림 종류마다의 내용 시트 */
enum class SheetKind { Reward, Shoe, CrewInvite, PartyInvite, UnverifiedReward, General }

fun sheetKindOf(entity: NotificationEntity): SheetKind = when (entity.type) {
    NotificationType.REWARD_EARNED, NotificationType.GOAL_REACHED, NotificationType.COURSE_COMPLETE,
    NotificationType.PARTY_FINISHED, NotificationType.EVENT_CLAIMED -> SheetKind.Reward
    NotificationType.SNEAKER_MINTED, NotificationType.SNEAKER_UPGRADED -> SheetKind.Shoe
    NotificationType.CREW_INVITE -> SheetKind.CrewInvite
    NotificationType.PARTY_INVITE -> SheetKind.PartyInvite
    // 처리하지 않은 이전 보상 알림 — 금액만으로 지급 가능 · 완료를 단정하지 않는다
    NotificationType.EVENT_REWARD -> if (entity.actioned) SheetKind.General else SheetKind.UnverifiedReward
    else -> SheetKind.General
}

/**
 * 알림 하나의 내용 — 저장된 본문 · 시간 · 금액만 보인다(지금 잔액으로 덮어쓰지 않는다).
 * 초대의 거절 확인 · 로그인 안내는 시트를 겹치지 않고 같은 시트의 내용만 바꾼다.
 */
@Composable
fun NotificationSheet(
    entity: NotificationEntity,
    step: InviteStep?,
    actions: NotificationSheetActions,
    zone: ZoneId = ZoneId.systemDefault(),
) {
    when (sheetKindOf(entity)) {
        SheetKind.Reward -> RewardSheet(entity, actions, zone)
        SheetKind.Shoe -> ShoeSheet(entity, actions, zone)
        SheetKind.CrewInvite -> CrewInviteSheet(entity, step ?: if (entity.actioned) InviteStep.Answered else InviteStep.Idle, actions, zone)
        SheetKind.PartyInvite -> PartyInviteSheet(entity, step ?: if (entity.actioned) InviteStep.Marked else InviteStep.Idle, actions, zone)
        SheetKind.UnverifiedReward -> UnverifiedRewardSheet(actions)
        SheetKind.General -> GeneralSheet(entity, actions, zone)
    }
}

// ── 정보 알림 ─────────────────────────────────────────────────────

/** 03 — 적립 알림. 이 알림에 기록된 내용과 지갑 진입. 거래 하나를 골라 열지 않는다(전체 지갑) */
@Composable
private fun RewardSheet(entity: NotificationEntity, actions: NotificationSheetActions, zone: ZoneId) {
    val amount = showsAmount(entity)
    SettingsSheet(
        title = stringResource(if (amount) R.string.sheet_reward_title else R.string.sheet_record_title),
        onDismiss = actions.dismiss, modifier = Modifier.testTag("sheet-reward"),
        actions = {
            SettingsPrimaryButton(stringResource(R.string.sheet_open_wallet), actions.openWallet,
                Modifier.fillMaxWidth().testTag("sheet-open-wallet"))
        },
    ) {
        Headline(messageFor(entity))
        DateLine(entity.timestamp, zone)
        SheetDivider()
        Note(stringResource(if (amount) R.string.sheet_reward_note else R.string.sheet_record_note))
        Note(stringResource(R.string.sheet_wallet_note))
    }
}

/** 04 — 신발 알림. 모델 이름만 알 수 있어 한 켤레를 골라 열지 않고 보유 목록으로 간다. 지급 · 착용하지 않는다 */
@Composable
private fun ShoeSheet(entity: NotificationEntity, actions: NotificationSheetActions, zone: ZoneId) {
    val p = settingsPalette()
    SettingsSheet(
        title = stringResource(R.string.sheet_shoe_title), onDismiss = actions.dismiss, modifier = Modifier.testTag("sheet-shoe"),
        actions = {
            SettingsPrimaryButton(stringResource(R.string.sheet_open_shoes), actions.openShoes,
                Modifier.fillMaxWidth().testTag("sheet-open-shoes"))
        },
    ) {
        Icon(StepUpIcons.Shoe, contentDescription = null, tint = p.accent, modifier = Modifier.padding(top = 4.dp).size(32.dp))
        Headline(messageFor(entity))
        DateLine(entity.timestamp, zone)
        Note(stringResource(R.string.sheet_shoe_note), Modifier.padding(top = 10.dp))
    }
}

/** 09 — 처리하지 않은 이전 보상 알림. 알림으로 지급하지 않고 챌린지에서 실제 자격과 지급 상태를 본다 */
@Composable
private fun UnverifiedRewardSheet(actions: NotificationSheetActions) {
    val p = settingsPalette()
    SettingsSheet(
        title = stringResource(R.string.reward_prev_title), onDismiss = actions.dismiss,
        modifier = Modifier.testTag("sheet-unverified-reward"),
        actions = {
            SettingsPrimaryButton(stringResource(R.string.reward_open_challenges), actions.openChallenges,
                Modifier.fillMaxWidth().testTag("sheet-open-challenges"))
        },
    ) {
        Icon(Icons.Outlined.Info, contentDescription = null, tint = p.accent, modifier = Modifier.padding(top = 4.dp).size(32.dp))
        Headline(stringResource(R.string.reward_check_title))
        Note(stringResource(R.string.reward_check_body), Modifier.padding(top = 8.dp))
        Note(stringResource(R.string.reward_check_where))
    }
}

/** 그 밖의 알림 — 저장된 내용만, "확인" 하나. 없는 갈 곳을 꾸미지 않는다 */
@Composable
private fun GeneralSheet(entity: NotificationEntity, actions: NotificationSheetActions, zone: ZoneId) {
    SettingsSheet(
        title = stringResource(R.string.notif_title), onDismiss = actions.dismiss, modifier = Modifier.testTag("sheet-general"),
        actions = {
            SettingsPrimaryButton(stringResource(R.string.set_ok), actions.dismiss, Modifier.fillMaxWidth().testTag("sheet-ok"))
        },
    ) {
        Headline(messageFor(entity))
        DateLine(entity.timestamp, zone)
    }
}

// ── 크루 초대 ─────────────────────────────────────────────────────

/**
 * 05 · 06 · 07 · 21 · 22 — 크루 초대. 크루 이름과 대상은 알림에 저장된 실제 값이다.
 * 열기 · 크루 보기 · 닫기만으로 수락하지 않는다. 가입 완료와 가입 신청을 나누고, 결과를 모르면 가입 상태부터 확인한다.
 */
@Composable
private fun CrewInviteSheet(entity: NotificationEntity, step: InviteStep, actions: NotificationSheetActions, zone: ZoneId) {
    val crewId = entity.argExtra.takeIf { it.isNotBlank() }
    when (step) {
        InviteStep.ConfirmDecline, InviteStep.Declining, InviteStep.DeclineFailed ->
            DeclineSheet(entity.argText, party = false, step, actions)
        InviteStep.SignIn -> LoginSheet(actions)
        else -> SettingsSheet(
            title = stringResource(R.string.invite_crew_title), onDismiss = actions.dismiss,
            modifier = Modifier.testTag("sheet-crew-invite"),
            actions = {
                val busy = step == InviteStep.Busy
                when (step) {
                    InviteStep.Joined, InviteStep.Requested, InviteStep.Answered ->
                        if (crewId != null) {
                            SettingsPrimaryButton(stringResource(R.string.invite_view_crew), { actions.viewCrew(crewId) },
                                Modifier.fillMaxWidth().testTag("invite-view-crew-primary"))
                        } else {
                            SettingsPrimaryButton(stringResource(R.string.set_ok), actions.dismiss, Modifier.fillMaxWidth())
                        }
                    InviteStep.Unknown, InviteStep.StillUnknown -> ButtonPair(
                        stringResource(R.string.common_close), actions.dismiss,
                        stringResource(R.string.invite_check_status), actions.checkCrew, primaryTag = "invite-check-status",
                    )
                    else -> ButtonPair(
                        stringResource(R.string.notif_decline), actions.askDecline,
                        stringResource(
                            when {
                                busy -> R.string.invite_checking
                                step == InviteStep.Failed -> R.string.invite_retry_accept
                                else -> R.string.invite_accept
                            },
                        ),
                        actions.acceptCrew, busy = busy, secondaryTag = "invite-decline", primaryTag = "invite-accept",
                    )
                }
            },
        ) {
            Crossfade(step, animationSpec = tween(LocalMotion.current.duration(200)), label = "crewInvite") { shown ->
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Headline(entity.argText, big = true)
                    Note(stringResource(R.string.invite_crew_body))
                    DateLine(entity.timestamp, zone)
                    Spacer(Modifier.height(6.dp))
                    when (shown) {
                        InviteStep.Joined -> StatusBox(Icons.Filled.CheckCircle, stringResource(R.string.crew_notice_joined),
                            stringResource(R.string.invite_joined_body), "invite-joined")
                        InviteStep.Requested -> StatusBox(Icons.Outlined.Schedule, stringResource(R.string.invite_requested_title),
                            stringResource(R.string.invite_requested_body), "invite-requested")
                        InviteStep.Answered -> StatusBox(Icons.Outlined.Info, stringResource(R.string.invite_answered_title),
                            stringResource(R.string.invite_answered_body), "invite-answered")
                        InviteStep.Failed -> Problem(stringResource(R.string.invite_failed_title),
                            stringResource(R.string.invite_failed_body), "invite-failed")
                        InviteStep.Unknown -> Problem(stringResource(R.string.invite_unknown_title),
                            stringResource(R.string.invite_unknown_body), "invite-unknown")
                        InviteStep.StillUnknown -> Problem(stringResource(R.string.invite_unknown_title),
                            stringResource(R.string.invite_still_unknown), "invite-unknown")
                        else -> Note(stringResource(R.string.invite_crew_preview))
                    }
                    val showLink = shown != InviteStep.Joined && shown != InviteStep.Requested && shown != InviteStep.Answered
                    if (crewId != null && showLink) {
                        LinkRow(stringResource(R.string.invite_view_crew), "invite-view-crew") { actions.viewCrew(crewId) }
                    }
                }
            }
        }
    }
}

// ── 함께 뛰기 초대 ────────────────────────────────────────────────

/**
 * 08 — 함께 뛰기 초대. "대기실 보기"는 보기만, "수락하고 보기"는 이 폰의 알림에 수락 표시만 하고 대기실로 간다 —
 * 실제 참가 · 출발은 대기실이 서버와 확인한다(참가 완료로 꾸미지 않는다).
 */
@Composable
private fun PartyInviteSheet(entity: NotificationEntity, step: InviteStep, actions: NotificationSheetActions, zone: ZoneId) {
    val lobby = entity.argExtra.takeIf { it.isNotBlank() }
    when (step) {
        InviteStep.ConfirmDecline, InviteStep.Declining, InviteStep.DeclineFailed ->
            DeclineSheet(entity.argText, party = true, step, actions)
        else -> SettingsSheet(
            title = stringResource(R.string.invite_party_title), onDismiss = actions.dismiss,
            modifier = Modifier.testTag("sheet-party-invite"),
            actions = {
                val busy = step == InviteStep.Busy
                when {
                    step == InviteStep.Marked ->
                        if (lobby != null) {
                            SettingsPrimaryButton(stringResource(R.string.invite_view_lobby), { actions.viewLobby(lobby) },
                                Modifier.fillMaxWidth().testTag("invite-view-lobby-primary"))
                        } else {
                            SettingsPrimaryButton(stringResource(R.string.set_ok), actions.dismiss, Modifier.fillMaxWidth())
                        }
                    lobby == null -> ButtonPair(
                        stringResource(R.string.notif_decline), actions.askDecline,
                        stringResource(R.string.common_close), actions.dismiss, secondaryTag = "invite-decline",
                    )
                    else -> ButtonPair(
                        stringResource(R.string.notif_decline), actions.askDecline,
                        stringResource(if (busy) R.string.invite_checking else R.string.invite_accept_and_view),
                        actions.acceptParty, busy = busy, secondaryTag = "invite-decline", primaryTag = "invite-accept-party",
                    )
                }
            },
        ) {
            Headline(entity.argText, big = true)
            Note(stringResource(R.string.invite_party_body))
            DateLine(entity.timestamp, zone)
            Spacer(Modifier.height(6.dp))
            when (step) {
                InviteStep.Marked -> StatusBox(Icons.Filled.CheckCircle, stringResource(R.string.invite_party_marked_title),
                    stringResource(R.string.invite_party_marked_body), "invite-marked")
                InviteStep.MarkFailed -> Problem(stringResource(R.string.invite_party_failed), null, "invite-mark-failed")
                else -> Note(stringResource(if (lobby != null) R.string.invite_party_preview else R.string.invite_party_no_lobby))
            }
            if (lobby != null && step != InviteStep.Marked) {
                LinkRow(stringResource(R.string.invite_view_lobby), "invite-view-lobby") { actions.viewLobby(lobby) }
            }
        }
    }
}

/** 21 — 거절 확인. 지워지는 것은 이 초대 알림 하나뿐이라는 것만 알린다. 지우는 동안에는 닫히지 않는다 */
@Composable
private fun DeclineSheet(name: String, party: Boolean, step: InviteStep, actions: NotificationSheetActions) {
    val p = settingsPalette()
    val declining = step == InviteStep.Declining
    SettingsSheet(
        title = stringResource(R.string.decline_title), onDismiss = actions.cancelDecline, dismissible = !declining,
        showClose = false, modifier = Modifier.testTag("sheet-decline"),
        actions = {
            ButtonPair(
                stringResource(R.string.set_cancel), actions.cancelDecline,
                stringResource(if (step == InviteStep.DeclineFailed) R.string.decline_again else R.string.decline_confirm),
                actions.decline, busy = declining, secondaryTag = "decline-cancel", primaryTag = "decline-confirm",
            )
        },
    ) {
        Text(
            stringResource(if (party) R.string.decline_party_body else R.string.decline_crew_body, name),
            color = p.text, fontSize = 17.sp, fontWeight = FontWeight.SemiBold, lineHeight = 1.4.em,
        )
        Note(stringResource(if (party) R.string.decline_party_note else R.string.decline_crew_note))
        if (step == InviteStep.DeclineFailed) {
            Problem(stringResource(R.string.decline_failed_title), stringResource(R.string.decline_failed_body), "decline-failed")
        }
    }
}

/** 22 — 로그인 필요. 취소해도 초대는 남고, 로그인하고 돌아오면 이 초대를 다시 보인다(자동 수락 없음) */
@Composable
private fun LoginSheet(actions: NotificationSheetActions) {
    val p = settingsPalette()
    SettingsSheet(
        title = stringResource(R.string.invite_login_title), onDismiss = actions.dismiss, modifier = Modifier.testTag("sheet-login"),
        actions = {
            ButtonPair(
                stringResource(R.string.invite_later), actions.later,
                stringResource(R.string.invite_login), actions.signIn, secondaryTag = "login-later", primaryTag = "login-go",
            )
        },
    ) {
        Text(stringResource(R.string.invite_login_body), color = p.text, fontSize = 17.sp, fontWeight = FontWeight.SemiBold,
            lineHeight = 1.4.em)
        Note(stringResource(R.string.invite_login_note))
    }
}

/** 14 — 답글 알림이 가리키던 글을 서버가 주지 않는다(지워졌거나 볼 수 없음). 알림 · 적립 기록은 지우지 않는다 */
@Composable
fun MissingTargetSheet(onOpenCommunity: () -> Unit, onBack: () -> Unit) {
    val p = settingsPalette()
    SettingsSheet(
        title = stringResource(R.string.missing_title), onDismiss = onBack, modifier = Modifier.testTag("sheet-missing"),
        actions = {
            SettingsPrimaryButton(stringResource(R.string.missing_open_community), onOpenCommunity,
                Modifier.fillMaxWidth().testTag("missing-community"))
            SettingsSecondaryButton(stringResource(R.string.missing_back), onBack, Modifier.fillMaxWidth().testTag("missing-back"))
        },
    ) {
        Text(stringResource(R.string.missing_heading), color = p.text, fontSize = 17.sp, fontWeight = FontWeight.SemiBold,
            lineHeight = 1.4.em)
        Note(stringResource(R.string.missing_body))
    }
}

// ── 시트 조각 ─────────────────────────────────────────────────────

@Composable
private fun Headline(text: String, big: Boolean = false) {
    val p = settingsPalette()
    Text(text, color = p.text, fontSize = if (big) 27.sp else 24.sp, fontWeight = FontWeight.SemiBold, lineHeight = 1.32.em,
        modifier = Modifier.padding(top = 6.dp))
}

@Composable
private fun DateLine(timestamp: Long, zone: ZoneId) {
    val p = settingsPalette()
    Text(rememberPattern(R.string.date_time_notice).format(Instant.ofEpochMilli(timestamp).atZone(zone)),
        color = p.secondary, fontSize = 15.sp)
}

@Composable
private fun Note(text: String, modifier: Modifier = Modifier) {
    val p = settingsPalette()
    Text(text, color = p.secondary, fontSize = 15.sp, lineHeight = 1.55.em, modifier = modifier)
}

@Composable
private fun SheetDivider() {
    val p = settingsPalette()
    Box(Modifier.padding(vertical = 8.dp).fillMaxWidth().height(1.dp).background(p.divider))
}

/** 서버가 확인한 결과 한 칸 — 가입 완료 · 가입 신청 · 수락 표시 */
@Composable
private fun StatusBox(icon: ImageVector, title: String, body: String, tag: String) {
    val p = settingsPalette()
    Row(
        Modifier.fillMaxWidth().clip(RoundedCornerShape(20.dp)).background(raisedSurface()).padding(18.dp)
            .semantics(mergeDescendants = true) { liveRegion = LiveRegionMode.Polite }.testTag(tag),
        horizontalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        Icon(icon, contentDescription = null, tint = p.accent, modifier = Modifier.padding(top = 2.dp).size(22.dp))
        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text(title, color = p.text, fontSize = 17.sp, fontWeight = FontWeight.SemiBold, lineHeight = 1.35.em)
            Text(body, color = p.secondary, fontSize = 14.sp, lineHeight = 1.5.em)
        }
    }
}

/** 처리되지 않았거나 모를 때 — 굵은 한 줄과 설명. 초대는 그대로 남아 있다 */
@Composable
private fun Problem(title: String, body: String?, tag: String) {
    val p = settingsPalette()
    Column(
        Modifier.fillMaxWidth().semantics(mergeDescendants = true) { liveRegion = LiveRegionMode.Polite }.testTag(tag),
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        Text(title, color = p.text, fontSize = 17.sp, fontWeight = FontWeight.SemiBold, lineHeight = 1.35.em)
        if (body != null) Text(body, color = p.secondary, fontSize = 14.sp, lineHeight = 1.5.em)
    }
}

/** "크루 보기 >" 같은 글자 줄 — 줄 전체가 누르는 곳 */
@Composable
private fun LinkRow(label: String, tag: String, onClick: () -> Unit) {
    val p = settingsPalette()
    Row(
        Modifier.fillMaxWidth().heightIn(min = 48.dp).feedbackClickable(role = Role.Button, onClick = onClick).testTag(tag),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(label, color = p.accent, fontSize = 16.sp, fontWeight = FontWeight.SemiBold, modifier = Modifier.weight(1f))
        Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, contentDescription = null, tint = p.accent, modifier = Modifier.size(22.dp))
    }
}

/** 보조(왼쪽) · 주(오른쪽) 버튼 한 쌍. 처리 중이면 둘 다 막고 주 버튼이 돈다 */
@Composable
private fun ColumnScope.ButtonPair(
    secondary: String,
    onSecondary: () -> Unit,
    primary: String,
    onPrimary: () -> Unit,
    busy: Boolean = false,
    secondaryTag: String = "sheet-secondary",
    primaryTag: String = "sheet-primary",
) {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        SettingsSecondaryButton(secondary, onSecondary, Modifier.weight(0.9f).testTag(secondaryTag), enabled = !busy)
        SettingsPrimaryButton(primary, onPrimary, Modifier.weight(1f).testTag(primaryTag), loading = busy)
    }
}
