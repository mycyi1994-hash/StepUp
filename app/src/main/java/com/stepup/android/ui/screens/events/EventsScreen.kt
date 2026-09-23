package com.stepup.android.ui.screens.events

import android.content.Intent
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.DirectionsRun
import androidx.compose.material.icons.automirrored.filled.DirectionsWalk
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.GroupAdd
import androidx.compose.material.icons.filled.Whatshot
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.stepup.android.R
import com.stepup.android.data.repo.EventDef
import com.stepup.android.data.repo.Events
import com.stepup.android.domain.RewardEconomy
import com.stepup.android.ui.components.BadgeTone
import com.stepup.android.ui.components.BarMeter
import com.stepup.android.ui.components.GhostButton
import com.stepup.android.ui.components.GlowCard
import com.stepup.android.ui.components.HexEmblem
import com.stepup.android.ui.components.PrimaryCta
import com.stepup.android.ui.components.SectionHeader
import com.stepup.android.ui.components.SmallBadge
import com.stepup.android.ui.components.AvatarImage
import com.stepup.android.domain.AvatarArt
import com.stepup.android.ui.components.SubHeader
import com.stepup.android.ui.components.VoltButton
import com.stepup.android.ui.components.celebrate
import com.stepup.android.ui.theme.Carbon
import com.stepup.android.ui.theme.CarbonHigh
import com.stepup.android.ui.theme.Edge
import com.stepup.android.ui.theme.Silver
import com.stepup.android.ui.theme.Slate
import com.stepup.android.ui.theme.Snow
import com.stepup.android.ui.theme.StepUpNumbers
import com.stepup.android.ui.theme.Volt
import com.stepup.android.ui.theme.VoltText

/**
 * 챌린지 — 러닝 안에서 여는 "추가로 받는" 자리.
 *
 * ── 진행률은 기록에서만 ──
 *
 * 예전 이벤트 화면에는 고정된 숫자가 있었다(친구 초대 2/3, 나이트 러너
 * 12.4/20km). 아무도 초대하지 않았고 밤에 뛴 적이 없어도 그렇게 보였다.
 * 이제 모든 진행률은 실제 기록에서 계산하고, 계산할 기록이 없는 것은
 * "준비 중"으로 둔다.
 *
 * ── 받기 버튼 ──
 *
 * 목표를 채운 것에만 켜진다. 채우지 못한 것에 버튼을 켜 두면 누른 사람이
 * 거절당하고, 그것은 버튼이 한 거짓말이다. 누르기만 하면 15,000 SUP 가
 * 들어오던 캠페인 카드도 같은 이유로 뺐다 — 무엇을 해야 받는지가 정해지지
 * 않은 보상이었다.
 */
@Composable
fun EventsScreen(
    onBack: () -> Unit = {},
    onOpenWallet: () -> Unit = {},
    onStartRun: () -> Unit = {},
    viewModel: EventsViewModel = viewModel(factory = EventsViewModel.Factory),
) {
    val context = LocalContext.current
    val balance by viewModel.balance.collectAsStateWithLifecycle()
    val weekSteps by viewModel.weekSteps.collectAsStateWithLifecycle()
    val claimed by viewModel.claimedIds.collectAsStateWithLifecycle()
    val claimResult by viewModel.claimResult.collectAsStateWithLifecycle()
    val daily by viewModel.daily.collectAsStateWithLifecycle()
    val nightKm by viewModel.nightKm.collectAsStateWithLifecycle()

    var celebration by rememberSaveable { mutableStateOf(0) }
    val claimedFmt = stringResource(R.string.toast_claimed, "%s")
    val notFinished = stringResource(R.string.toast_not_finished)
    LaunchedEffect(claimResult) {
        when (val r = claimResult) {
            is ClaimResult.Success -> {
                celebration++
                Toast.makeText(context, claimedFmt.format("%,.0f".format(r.amount)), Toast.LENGTH_SHORT).show()
            }
            ClaimResult.NotFinished ->
                Toast.makeText(context, notFinished, Toast.LENGTH_SHORT).show()
            null -> {}
        }
        if (claimResult != null) viewModel.consumeClaimResult()
    }

    // 초대 — 보낼 말을 먼저 정하고, 그다음에 어디로 보낼지 고른다.
    val inviteSubject = stringResource(R.string.invite_subject)
    val inviteText = stringResource(R.string.invite_text)
    var showInvite by rememberSaveable { mutableStateOf(false) }
    if (showInvite) {
        InviteDialog(
            subject = inviteSubject,
            initialText = inviteText,
            onSend = { text ->
                showInvite = false
                val send = Intent(Intent.ACTION_SEND).apply {
                    type = "text/plain"
                    putExtra(Intent.EXTRA_SUBJECT, inviteSubject)
                    putExtra(Intent.EXTRA_TEXT, text)
                }
                context.startActivity(Intent.createChooser(send, inviteSubject))
            },
            onDismiss = { showInvite = false },
        )
    }

    val weekFraction = (weekSteps.toFloat() / Events.STEP_SURGE.target.toFloat()).coerceIn(0f, 1f)
    val nightFraction = (nightKm / Events.NIGHT_QUEST.target).toFloat().coerceIn(0f, 1f)

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(start = 18.dp, end = 18.dp, top = 8.dp, bottom = 18.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        item {
            SubHeader(
                title = stringResource(R.string.challenge_title),
                onBack = onBack,
                balance = balance,
                onOpenWallet = onOpenWallet,
            )
        }
        item {
            Row(
                modifier = Modifier.celebrate(celebration.takeIf { it > 0 }),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(3.dp),
                ) {
                    Text(
                        text = stringResource(R.string.challenge_today_title),
                        fontSize = 26.sp,
                        fontWeight = FontWeight.Black,
                        letterSpacing = (-0.6).sp,
                        color = Snow,
                    )
                    Text(text = stringResource(R.string.challenge_today_sub), fontSize = 14.sp, color = Silver)
                }
                // 남녀 기본 캐릭터 그림 그대로
                if (androidx.compose.ui.platform.LocalDensity.current.fontScale <= 1.3f) {
                    Box(Modifier.size(width = 112.dp, height = 92.dp)) {
                        AvatarImage(
                            art = AvatarArt.MALE_RUN,
                            modifier = Modifier
                                .align(Alignment.BottomStart)
                                .size(width = 60.dp, height = 88.dp),
                        )
                        AvatarImage(
                            art = AvatarArt.FEMALE_IDLE,
                            modifier = Modifier
                                .align(Alignment.BottomEnd)
                                .size(width = 56.dp, height = 88.dp),
                        )
                    }
                }
            }
        }

        // ── 일일 — 걸음 목표. 달성하면 보너스가 저절로 들어온다 ──
        item {
            val d = daily
            ChallengeCard(
                tag = stringResource(R.string.challenge_tag_daily),
                tagTone = BadgeTone.Accent,
                icon = Icons.AutoMirrored.Filled.DirectionsWalk,
                title = stringResource(R.string.challenge_daily_title),
                desc = stringResource(R.string.challenge_daily_desc),
                reward = if (d != null && d.paidToday > 0) d.paidToday else RewardEconomy.goalBaseBonus(d?.goal ?: 0),
                rewardNote = if (d != null && d.paidToday > 0) null else stringResource(R.string.challenge_daily_streak_note),
                fraction = d?.fraction ?: 0f,
                progressText = if (d == null) "—" else "%,d / %,d".format(d.steps, d.goal),
                state = when {
                    d == null -> ChallengeState.Loading
                    d.paidToday > 0 -> ChallengeState.Paid
                    d.done -> ChallengeState.Settling
                    else -> ChallengeState.InProgress
                },
                onClaim = null,
            )
        }

        // ── 주간 — 7일 걸음 합 ──
        item {
            val done = weekFraction >= 1f
            ChallengeCard(
                tag = stringResource(R.string.challenge_tag_weekly),
                tagTone = BadgeTone.Nft,
                icon = Icons.Filled.Whatshot,
                title = stringResource(R.string.event_step_surge),
                desc = stringResource(R.string.event_step_surge_desc, "%,d".format(Events.STEP_SURGE.target.toLong())),
                reward = Events.STEP_SURGE.reward,
                fraction = weekFraction,
                progressText = "%,d / %,d".format(weekSteps, Events.STEP_SURGE.target.toLong()),
                state = claimState(Events.STEP_SURGE, claimed, done),
                onClaim = { viewModel.claim(Events.STEP_SURGE, weekFraction) },
            )
        }

        // ── 나이트 러너 — 저녁 8시 이후 러닝 거리 합 ──
        item {
            val done = nightFraction >= 1f
            ChallengeCard(
                tag = stringResource(R.string.tag_limited),
                tagTone = BadgeTone.Glow,
                icon = Icons.Filled.DarkMode,
                title = stringResource(R.string.event_night_quest),
                desc = stringResource(R.string.event_night_quest_desc),
                reward = Events.NIGHT_QUEST.reward,
                fraction = nightFraction,
                progressText = "%.1f / %.0f km".format(nightKm, Events.NIGHT_QUEST.target),
                state = claimState(Events.NIGHT_QUEST, claimed, done),
                onClaim = { viewModel.claim(Events.NIGHT_QUEST, nightFraction) },
            )
        }

        // ── 친구 초대 — 초대한 사람을 셀 기록이 아직 없다 ──
        item {
            ChallengeCard(
                tag = stringResource(R.string.tag_mission),
                tagTone = BadgeTone.Muted,
                icon = Icons.Filled.GroupAdd,
                title = stringResource(R.string.event_refer),
                desc = stringResource(R.string.challenge_refer_desc),
                reward = Events.REFER.reward,
                fraction = null,
                progressText = stringResource(R.string.challenge_soon),
                state = ChallengeState.Soon,
                onClaim = null,
                extra = {
                    GhostButton(
                        text = stringResource(R.string.events_invite),
                        onClick = { showInvite = true },
                        modifier = Modifier.fillMaxWidth(),
                    )
                },
            )
        }

        item {
            PrimaryCta(
                text = stringResource(R.string.home_start_run),
                icon = Icons.AutoMirrored.Filled.DirectionsRun,
                onClick = onStartRun,
            )
        }

        // ── 공지 — StepUp 이 쓰는 안내. 예전 뉴스 탭의 특가 공지 ──
        item { SectionHeader(title = stringResource(R.string.challenge_notices)) }
        items(DEAL_FEED.size) { index -> FeedCard(DEAL_FEED[index]) }
        item { FeedFootnote(R.string.feed_note_deals) }
    }
}

private enum class ChallengeState { Loading, InProgress, Ready, Settling, Paid, Claimed, Soon }

/** 받기형 도전의 상태 — 받았으면 받음, 채웠으면 받기, 아니면 진행 중 */
private fun claimState(def: EventDef, claimed: Set<String>, done: Boolean): ChallengeState = when {
    claimed.contains(def.id) -> ChallengeState.Claimed
    done -> ChallengeState.Ready
    else -> ChallengeState.InProgress
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun ChallengeCard(
    tag: String,
    tagTone: BadgeTone,
    icon: ImageVector,
    title: String,
    desc: String,
    reward: Double,
    fraction: Float?,
    progressText: String,
    state: ChallengeState,
    onClaim: (() -> Unit)?,
    rewardNote: String? = null,
    extra: (@Composable () -> Unit)? = null,
) {
    GlowCard(
        accent = state == ChallengeState.Ready,
        contentPadding = PaddingValues(16.dp),
        spacing = 12.dp,
    ) {
        // 표시 · 상태 · 보상을 맨 위 한 줄에 둔다. 보상을 제목 옆에 두면 큰 글자에서
        // 설명이 한 단어씩 세로로 쪼개진다.
        //
        // 보상 숫자를 먼저 재고 표시 둘은 남은 폭에 눕힌다 — 모자라면 표시가 다음
        // 줄로 내려간다. 반대로 하면 큰 글자에서 "+20"이 "+2 / 0"으로 끊긴다.
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            FlowRow(
                modifier = Modifier.weight(1f),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                SmallBadge(tag, tone = tagTone)
                StatusChip(state)
            }
            Row(
                modifier = Modifier
                    .clip(RoundedCornerShape(12.dp))
                    .border(1.dp, Volt.copy(alpha = 0.45f), RoundedCornerShape(12.dp))
                    .padding(horizontal = 8.dp, vertical = 5.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                HexEmblem(size = 16.dp, glow = false)
                Text(
                    text = "+%,.0f".format(reward),
                    fontFamily = StepUpNumbers,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold,
                    color = Snow,
                    maxLines = 1,
                    softWrap = false,
                )
                Text(
                    text = "SUP",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = Silver,
                    maxLines = 1,
                    softWrap = false,
                )
            }
        }
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(CircleShape)
                    .background(CarbonHigh, CircleShape)
                    .border(1.dp, Edge, CircleShape),
                contentAlignment = Alignment.Center,
            ) {
                Icon(icon, contentDescription = null, tint = VoltText, modifier = Modifier.size(24.dp))
            }
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(text = title, fontSize = 17.sp, fontWeight = FontWeight.Black, color = Snow)
                Text(text = desc, fontSize = 13.sp, color = Silver, lineHeight = 18.sp)
            }
        }
        if (rewardNote != null) {
            Text(text = rewardNote, fontSize = 11.sp, color = Slate)
        }
        if (fraction != null) {
            // 큰 진행값 한 줄, 그 아래 막대와 백분율
            Text(
                text = progressText,
                fontFamily = StepUpNumbers,
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                color = Snow,
            )
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                BarMeter(fraction = fraction, height = 8.dp, modifier = Modifier.weight(1f))
                Text(
                    text = "%d%%".format((fraction.coerceIn(0f, 1f) * 100).toInt()),
                    fontFamily = StepUpNumbers,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    color = com.stepup.android.ui.theme.VoltText,
                )
            }
        } else {
            Text(text = progressText, fontSize = 13.sp, color = Slate)
        }
        // 받기 — 채운 것에만. 채우지 못했으면 버튼 자체를 보이지 않는다.
        if (onClaim != null && state == ChallengeState.Ready) {
            VoltButton(
                text = stringResource(R.string.events_claim),
                onClick = onClaim,
                modifier = Modifier.fillMaxWidth(),
            )
        }
        extra?.invoke()
    }
}

@Composable
private fun StatusChip(state: ChallengeState) {
    val (text, tone) = when (state) {
        ChallengeState.Loading -> return
        ChallengeState.InProgress -> stringResource(R.string.challenge_state_progress) to BadgeTone.Muted
        ChallengeState.Ready -> stringResource(R.string.challenge_state_ready) to BadgeTone.Accent
        ChallengeState.Settling -> stringResource(R.string.challenge_state_settling) to BadgeTone.Accent
        ChallengeState.Paid -> stringResource(R.string.challenge_state_paid) to BadgeTone.Glow
        ChallengeState.Claimed -> stringResource(R.string.events_claimed) to BadgeTone.Glow
        ChallengeState.Soon -> stringResource(R.string.challenge_soon) to BadgeTone.Muted
    }
    SmallBadge(text, tone = tone)
}

/**
 * 친구 초대 — 보낼 문구를 먼저 보여 준다.
 *
 * 기본 문구를 넣어 두되 고칠 수 있게 한다. 빈 칸으로 시작하면 대부분 아무
 * 말도 못 쓰고 닫고, 고칠 수 없으면 앱이 쓴 말이 내 이름으로 나간다.
 *
 * "보내기"를 누르면 그때 공유 시트가 뜬다 — 어디로 보낼지(메시지·카톡·메일)는
 * 안드로이드가 고르게 두는 편이 낫다. 우리가 목록을 만들면 그 사람이 쓰는
 * 앱이 빠져 있을 수 있다.
 */
@Composable
private fun InviteDialog(
    subject: String,
    initialText: String,
    onSend: (String) -> Unit,
    onDismiss: () -> Unit,
) {
    var text by rememberSaveable(initialText) { mutableStateOf(initialText) }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = Carbon,
        titleContentColor = Snow,
        textContentColor = Silver,
        confirmButton = {
            TextButton(
                onClick = { onSend(text.trim()) },
                enabled = text.isNotBlank(),
            ) {
                Text(
                    text = stringResource(R.string.invite_send),
                    color = if (text.isNotBlank()) Volt else Slate,
                    fontWeight = FontWeight.Bold,
                )
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(text = stringResource(R.string.common_cancel), color = Slate)
            }
        },
        title = { Text(text = subject, fontWeight = FontWeight.Black) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    text = stringResource(R.string.invite_edit_hint),
                    style = MaterialTheme.typography.bodySmall,
                    color = Slate,
                )
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(14.dp))
                        .background(CarbonHigh)
                        .border(1.dp, Edge, RoundedCornerShape(14.dp))
                        .padding(horizontal = 13.dp, vertical = 12.dp),
                ) {
                    BasicTextField(
                        value = text,
                        onValueChange = { if (it.length <= INVITE_MAX) text = it },
                        textStyle = TextStyle(color = Snow, fontSize = 14.sp, lineHeight = 20.sp),
                        cursorBrush = SolidColor(Volt),
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(min = 76.dp),
                    )
                }
                Text(
                    text = "${text.length} / $INVITE_MAX",
                    style = MaterialTheme.typography.bodySmall,
                    color = Slate,
                    modifier = Modifier.align(Alignment.End),
                )
            }
        },
    )
}

/** 초대 문구 길이 상한. 문자 메시지 한 통에 들어가는 정도. */
private const val INVITE_MAX = 300
