package com.stepup.android.ui.screens.notifications

import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.Schedule
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.stepup.android.R
import com.stepup.android.data.local.NotificationEntity
import com.stepup.android.data.local.NotificationType
import com.stepup.android.ui.components.StepUpIcons
import com.stepup.android.ui.components.runTextStyle
import com.stepup.android.ui.components.runTone
import com.stepup.android.ui.experience.LocalMotion
import com.stepup.android.ui.screens.onboarding.BlueOutlineButton
import com.stepup.android.ui.screens.onboarding.BluePlainButton
import java.time.Instant
import java.time.ZoneId

/** 내용 시트에서 할 수 있는 일 — 화면(내비게이션 · 뷰모델)이 채운다 */
class NotificationSheetActions(
    val dismiss: () -> Unit = {},
    val openWallet: () -> Unit = {},
    val openShoes: () -> Unit = {},
    val openChallenges: () -> Unit = {},
    /** 크루 상세 — 보기만 한다(수락 아님) */
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
 * 알림 하나의 내용(파란 톤 v4 NOT03~09 · 14 · 21~26) — 저장된 본문 · 시간 · 금액만 보인다(지금 잔액으로 덮어쓰지 않는다).
 * 초대의 거절 확인 · 로그인 안내는 시트를 겹치지 않고 같은 시트의 내용만 바꾼다. 시트는 하단 탭까지 덮는다.
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

/**
 * NOT03 · NOT25 — 적립 · 기록 알림. 이 알림에 기록된 내용과 지갑 진입(보상 수령이 아니다). 거래 하나를 골라 열지 않는다.
 * 서버 경제에서 폰이 계산하던 때의 기록은 [showsAmount] 가 거짓 — 금액 없이 "기록 알림"으로(목록과 같은 조건).
 */
@Composable
private fun RewardSheet(entity: NotificationEntity, actions: NotificationSheetActions, zone: ZoneId) {
    val amount = showsAmount(entity)
    NotifSheet(
        kicker = stringResource(if (amount) R.string.sheet_reward_title else R.string.sheet_record_title),
        onDismiss = actions.dismiss, modifier = Modifier.testTag("sheet-reward"),
        actions = {
            BluePlainButton(stringResource(R.string.sheet_open_wallet), actions.openWallet,
                Modifier.fillMaxWidth().testTag("sheet-open-wallet"))
        },
    ) {
        SheetHeadline(messageFor(entity))
        DateLine(entity.timestamp, zone)
        SheetRule()
        Column {
            SheetNote(stringResource(if (amount) R.string.sheet_reward_note else R.string.sheet_record_note))
            SheetNote(stringResource(R.string.sheet_wallet_note))
        }
    }
}

/** NOT04 — 신발 알림. 모델 이름만 알 수 있어 한 켤레를 골라 열지 않고 보유 목록으로 간다. 지급 · 착용하지 않는다 */
@Composable
private fun ShoeSheet(entity: NotificationEntity, actions: NotificationSheetActions, zone: ZoneId) {
    val t = runTone()
    NotifSheet(
        kicker = null, onDismiss = actions.dismiss, modifier = Modifier.testTag("sheet-shoe"),
        actions = {
            BluePlainButton(stringResource(R.string.sheet_open_shoes), actions.openShoes,
                Modifier.fillMaxWidth().testTag("sheet-open-shoes"))
        },
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(StepUpIcons.Shoe, contentDescription = null, tint = t.label, modifier = Modifier.size(40.dp))
            Spacer(Modifier.width(18.dp))
            Text(stringResource(R.string.sheet_shoe_title), style = runTextStyle(17.sp, t.label, FontWeight.SemiBold))
        }
        SheetHeadline(messageFor(entity))
        DateLine(entity.timestamp, zone)
        SheetRule()
        SheetNote(stringResource(R.string.sheet_shoe_note))
    }
}

/** NOT09 — 처리하지 않은 이전 보상 알림. 알림으로 지급하지 않고 챌린지에서 실제 자격과 지급 상태를 본다 */
@Composable
private fun UnverifiedRewardSheet(actions: NotificationSheetActions) {
    NotifSheet(
        kicker = stringResource(R.string.reward_prev_title), onDismiss = actions.dismiss,
        modifier = Modifier.testTag("sheet-unverified-reward"),
        actions = {
            BluePlainButton(stringResource(R.string.reward_open_challenges), actions.openChallenges,
                Modifier.fillMaxWidth().testTag("sheet-open-challenges"))
        },
    ) {
        InfoMark()
        SheetHeadline(stringResource(R.string.reward_check_title))
        Column {
            SheetNote(stringResource(R.string.reward_check_body))
            SheetNote(stringResource(R.string.reward_check_where))
        }
    }
}

/** NOT24 — 그 밖의 알림. 저장된 내용 · 시간과 "확인" 하나. 없는 갈 곳을 꾸미거나 다시 지급하지 않는다 */
@Composable
private fun GeneralSheet(entity: NotificationEntity, actions: NotificationSheetActions, zone: ZoneId) {
    NotifSheet(
        kicker = stringResource(R.string.notif_title), onDismiss = actions.dismiss, modifier = Modifier.testTag("sheet-general"),
        actions = {
            BluePlainButton(stringResource(R.string.set_ok), actions.dismiss, Modifier.fillMaxWidth().testTag("sheet-ok"))
        },
    ) {
        InfoMark()
        SheetHeadline(messageFor(entity))
        DateLine(entity.timestamp, zone)
    }
}

// ── 초대 ─────────────────────────────────────────────────────────

/** 같은 초대 시트 안에서 바뀌는 장면 — 시트를 새로 띄우지 않고 내용만 바꾼다 */
private enum class InviteScene { Invite, Decline, SignIn }

private fun sceneOf(step: InviteStep, crew: Boolean): InviteScene = when (step) {
    InviteStep.ConfirmDecline, InviteStep.Declining, InviteStep.DeclineFailed -> InviteScene.Decline
    InviteStep.SignIn -> if (crew) InviteScene.SignIn else InviteScene.Invite
    else -> InviteScene.Invite
}

/**
 * 초대 시트 하나 — 초대(NOT05 · 08) · 거절 확인(NOT21) · 로그인 필요(NOT22)는 같은 시트의 내용만 바뀐다.
 * 거절 확인은 X 없이 취소 · 초대 거절, 지우는 동안에는 닫히지 않는다(기존 dismissible 제한).
 */
@Composable
private fun InviteSheetFrame(
    entity: NotificationEntity,
    step: InviteStep,
    crew: Boolean,
    actions: NotificationSheetActions,
    inviteTag: String,
    inviteKicker: String,
    inviteActions: @Composable () -> Unit,
    inviteContent: @Composable () -> Unit,
) {
    val scene = sceneOf(step, crew)
    val declining = step == InviteStep.Declining
    NotifSheet(
        kicker = if (scene == InviteScene.Invite) inviteKicker else null,
        onDismiss = when (scene) {
            InviteScene.Decline -> actions.cancelDecline
            else -> actions.dismiss
        },
        dismissible = !declining,
        showClose = scene != InviteScene.Decline,
        modifier = Modifier.testTag(
            when (scene) {
                InviteScene.Invite -> inviteTag
                InviteScene.Decline -> "sheet-decline"
                InviteScene.SignIn -> "sheet-login"
            },
        ),
        actions = {
            when (scene) {
                InviteScene.Invite -> inviteActions()
                InviteScene.Decline -> ButtonPair(
                    stringResource(R.string.set_cancel), actions.cancelDecline,
                    stringResource(if (step == InviteStep.DeclineFailed) R.string.decline_again else R.string.decline_confirm),
                    actions.decline, busy = declining, secondaryTag = "decline-cancel", primaryTag = "decline-confirm",
                )
                InviteScene.SignIn -> ButtonPair(
                    stringResource(R.string.invite_later), actions.later,
                    stringResource(R.string.invite_login), actions.signIn, secondaryTag = "login-later", primaryTag = "login-go",
                )
            }
        },
    ) {
        Crossfade(scene, animationSpec = tween(LocalMotion.current.duration(200)), label = "inviteScene") { shown ->
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                when (shown) {
                    InviteScene.Invite -> inviteContent()
                    // NOT21 — 지워지는 것은 이 초대 알림 하나뿐(크루 탈퇴 · 대기실 변화 없음)
                    InviteScene.Decline -> {
                        SheetHeadline(stringResource(R.string.decline_title))
                        SheetLead(stringResource(if (crew) R.string.decline_crew_body else R.string.decline_party_body, entity.argText))
                        Spacer(Modifier.height(6.dp))
                        SheetNote(stringResource(if (crew) R.string.decline_crew_note else R.string.decline_party_note))
                        if (step == InviteStep.DeclineFailed) {
                            ProblemBox(stringResource(R.string.decline_failed_title), stringResource(R.string.decline_failed_body),
                                "decline-failed")
                        }
                    }
                    // NOT22 — 초대는 처리하지 않은 채 로그인으로. 돌아오면 같은 초대를 다시 열 뿐 자동 수락하지 않는다
                    InviteScene.SignIn -> {
                        SheetHeadline(stringResource(R.string.invite_login_title))
                        SheetLead(stringResource(R.string.invite_login_body))
                        Spacer(Modifier.height(6.dp))
                        SheetNote(stringResource(R.string.invite_login_note))
                    }
                }
            }
        }
    }
}

/**
 * NOT05 · 06 · 07 · 26 — 크루 초대. 크루 이름과 대상은 알림에 저장된 실제 값이다.
 * 열기 · 크루 보기 · 닫기만으로 수락하지 않는다. 가입 완료(Joined, 26)와 가입 신청(Requested, 06)을 나누고,
 * 결과를 모르면(Unknown) 수락을 다시 보내지 않고 가입 상태부터 확인한다.
 */
@Composable
private fun CrewInviteSheet(entity: NotificationEntity, step: InviteStep, actions: NotificationSheetActions, zone: ZoneId) {
    val crewId = entity.argExtra.takeIf { it.isNotBlank() }
    val busy = step == InviteStep.Busy
    val settled = step == InviteStep.Joined || step == InviteStep.Requested || step == InviteStep.Answered
    InviteSheetFrame(
        entity, step, crew = true, actions,
        inviteTag = "sheet-crew-invite", inviteKicker = stringResource(R.string.invite_crew_title),
        inviteActions = {
            when (step) {
                InviteStep.Joined, InviteStep.Requested, InviteStep.Answered ->
                    if (crewId != null) {
                        BluePlainButton(stringResource(R.string.invite_view_crew), { actions.viewCrew(crewId) },
                            Modifier.fillMaxWidth().testTag("invite-view-crew-primary"))
                    } else {
                        BluePlainButton(stringResource(R.string.set_ok), actions.dismiss, Modifier.fillMaxWidth())
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
        SheetHeadline(entity.argText, big = true)
        if (!settled) SheetLead(stringResource(R.string.invite_crew_body))
        DateLine(entity.timestamp, zone)
        when (step) {
            InviteStep.Joined -> {
                Spacer(Modifier.height(10.dp))
                StatusBox(Icons.Outlined.CheckCircle, stringResource(R.string.crew_notice_joined),
                    stringResource(R.string.invite_joined_body), "invite-joined")
            }
            InviteStep.Requested -> {
                Spacer(Modifier.height(10.dp))
                StatusBox(Icons.Outlined.Schedule, stringResource(R.string.invite_requested_title),
                    stringResource(R.string.invite_requested_body), "invite-requested")
            }
            InviteStep.Answered -> {
                Spacer(Modifier.height(10.dp))
                StatusBox(Icons.Outlined.Info, stringResource(R.string.invite_answered_title),
                    stringResource(R.string.invite_answered_body), "invite-answered")
            }
            InviteStep.Failed -> ProblemBox(stringResource(R.string.invite_failed_title),
                stringResource(R.string.invite_failed_body), "invite-failed")
            InviteStep.Unknown -> ProblemBox(stringResource(R.string.invite_unknown_title),
                stringResource(R.string.invite_unknown_body), "invite-unknown")
            InviteStep.StillUnknown -> ProblemBox(stringResource(R.string.invite_unknown_title),
                stringResource(R.string.invite_still_unknown), "invite-unknown")
            else -> Unit
        }
        if (!settled) {
            SheetRule()
            SheetNote(stringResource(R.string.invite_crew_preview))
            if (crewId != null) {
                LinkRow(stringResource(R.string.invite_view_crew), "invite-view-crew") { actions.viewCrew(crewId) }
            }
        }
    }
}

/**
 * NOT08 — 함께 뛰기 초대. "대기실 보기"는 보기만, "수락하고 보기"는 이 폰의 알림에 수락 표시만 하고 대기실로 간다 —
 * 실제 참가 · 출발은 대기실이 서버와 확인한다(참가 완료로 꾸미지 않는다). 대기실 정보가 없으면 수락 · 이동을 만들지 않는다.
 */
@Composable
private fun PartyInviteSheet(entity: NotificationEntity, step: InviteStep, actions: NotificationSheetActions, zone: ZoneId) {
    val lobby = entity.argExtra.takeIf { it.isNotBlank() }
    val busy = step == InviteStep.Busy
    InviteSheetFrame(
        entity, step, crew = false, actions,
        inviteTag = "sheet-party-invite", inviteKicker = stringResource(R.string.invite_party_title),
        inviteActions = {
            when {
                step == InviteStep.Marked ->
                    if (lobby != null) {
                        BluePlainButton(stringResource(R.string.invite_view_lobby), { actions.viewLobby(lobby) },
                            Modifier.fillMaxWidth().testTag("invite-view-lobby-primary"))
                    } else {
                        BluePlainButton(stringResource(R.string.set_ok), actions.dismiss, Modifier.fillMaxWidth())
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
        SheetHeadline(entity.argText, big = true)
        SheetLead(stringResource(R.string.invite_party_body))
        DateLine(entity.timestamp, zone)
        when (step) {
            InviteStep.Marked -> {
                Spacer(Modifier.height(10.dp))
                StatusBox(Icons.Outlined.CheckCircle, stringResource(R.string.invite_party_marked_title),
                    stringResource(R.string.invite_party_marked_body), "invite-marked")
            }
            InviteStep.MarkFailed -> ProblemBox(stringResource(R.string.invite_party_failed), null, "invite-mark-failed")
            else -> Unit
        }
        if (step != InviteStep.Marked) {
            SheetRule()
            SheetNote(stringResource(if (lobby != null) R.string.invite_party_preview else R.string.invite_party_no_lobby))
            if (lobby != null) {
                LinkRow(stringResource(R.string.invite_view_lobby), "invite-view-lobby") { actions.viewLobby(lobby) }
            }
        }
    }
}

/** NOT14 — 답글 알림이 가리키던 글을 서버가 주지 않는다(지워졌거나 볼 수 없음). 알림 · 적립 기록은 지우지 않는다 */
@Composable
fun MissingTargetSheet(onOpenCommunity: () -> Unit, onBack: () -> Unit) {
    NotifSheet(
        kicker = null, onDismiss = onBack, modifier = Modifier.testTag("sheet-missing"),
        actions = {
            BluePlainButton(stringResource(R.string.missing_open_community), onOpenCommunity,
                Modifier.fillMaxWidth().testTag("missing-community"))
            BlueOutlineButton(stringResource(R.string.missing_back), onBack, Modifier.fillMaxWidth().testTag("missing-back"))
        },
    ) {
        InfoMark()
        SheetHeadline(stringResource(R.string.missing_title))
        Column {
            SheetNote(stringResource(R.string.missing_heading))
            SheetNote(stringResource(R.string.missing_body))
        }
    }
}

// ── 시트 조각 ─────────────────────────────────────────────────────

/** 저장된 알림 시각 — 지금 시각이 아니다 */
@Composable
private fun DateLine(timestamp: Long, zone: ZoneId) {
    Text(
        rememberPattern(R.string.date_time_notice).format(Instant.ofEpochMilli(timestamp).atZone(zone)),
        style = runTextStyle(17.sp, runTone().label, FontWeight.Medium),
    )
}

/** 정보 그림 — 지급 미확인 · 일반 알림 · 열 수 없는 글 */
@Composable
private fun InfoMark() {
    Box(Modifier.padding(top = 4.dp)) {
        Icon(Icons.Outlined.Info, contentDescription = null, tint = runTone().label, modifier = Modifier.size(44.dp))
    }
}
