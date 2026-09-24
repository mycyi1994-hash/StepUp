package com.stepup.android.ui.screens.events

import android.content.Intent
import android.widget.Toast
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.ui.platform.testTag
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.DirectionsRun
import androidx.compose.material.icons.automirrored.filled.DirectionsWalk
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.Whatshot
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
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
import com.stepup.android.ui.components.GlowCard
import com.stepup.android.ui.components.HexEmblem
import com.stepup.android.ui.components.PrimaryCta
import com.stepup.android.ui.components.SecondaryHeader
import com.stepup.android.ui.components.VoltButton
import com.stepup.android.ui.components.celebrate
import com.stepup.android.ui.theme.Silver
import com.stepup.android.ui.theme.Slate
import com.stepup.android.ui.theme.Snow
import com.stepup.android.ui.theme.StepUpNumbers
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
    val look by viewModel.look.collectAsStateWithLifecycle()
    val claimingId by viewModel.claimingId.collectAsStateWithLifecycle()
    var selected by rememberSaveable { mutableIntStateOf(0) }

    var celebration by rememberSaveable { mutableStateOf(0) }
    val claimedFmt = stringResource(R.string.toast_claimed, "%s")
    val notFinished = stringResource(R.string.toast_not_finished)
    val alreadyClaimed = stringResource(R.string.toast_claim_already)
    val claimFailed = stringResource(R.string.toast_claim_failed)
    val signInNeeded = stringResource(R.string.toast_claim_sign_in)
    LaunchedEffect(claimResult) {
        when (val r = claimResult) {
            is ClaimResult.Success -> {
                celebration++
                Toast.makeText(context, claimedFmt.format("%,.0f".format(r.amount)), Toast.LENGTH_SHORT).show()
            }
            ClaimResult.NotFinished ->
                Toast.makeText(context, notFinished, Toast.LENGTH_SHORT).show()
            ClaimResult.AlreadyClaimed ->
                Toast.makeText(context, alreadyClaimed, Toast.LENGTH_SHORT).show()
            ClaimResult.SignInRequired ->
                Toast.makeText(context, signInNeeded, Toast.LENGTH_SHORT).show()
            ClaimResult.Failed ->
                Toast.makeText(context, claimFailed, Toast.LENGTH_SHORT).show()
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

    val weekFraction = weekSteps?.let { (it.toFloat() / Events.STEP_SURGE.target.toFloat()).coerceIn(0f, 1f) }
    val nightFraction = nightKm?.let { (it / Events.NIGHT_QUEST.target).toFloat().coerceIn(0f, 1f) }
    val selectedEvent = when (selected) { 1 -> Events.STEP_SURGE; 2 -> Events.NIGHT_QUEST; else -> null }
    val selectedFraction = when (selected) { 1 -> weekFraction; 2 -> nightFraction; else -> null }
    val canClaim = selectedEvent != null && selectedFraction != null && selectedFraction >= 1f &&
        claimed != null && selectedEvent.id !in claimed!!

    Box(Modifier.fillMaxSize()) {
    com.stepup.android.ui.components.RunnerScene(
        Modifier.fillMaxSize(),
        setting = when (selected) {
            1 -> com.stepup.android.ui.components.RunnerSetting.RunSunset
            2 -> com.stepup.android.ui.components.RunnerSetting.RunNight
            else -> com.stepup.android.ui.components.RunnerSetting.HomeBlueNight
        },
    )
    Column(Modifier.fillMaxSize().padding(horizontal = com.stepup.android.ui.theme.StepUpDesign.Gutter)) {
        SecondaryHeader(onBack = onBack, balance = balance, onOpenWallet = onOpenWallet,
            title = stringResource(R.string.challenge_title))
        LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp), contentPadding = PaddingValues(vertical = 8.dp)) {
            itemsIndexed(listOf(R.string.challenge_tag_daily, R.string.challenge_tag_weekly, R.string.event_night_quest)) { index, label ->
                com.stepup.android.ui.components.PillChip(
                    text = stringResource(label), selected = selected == index,
                    onClick = { selected = index }, modifier = Modifier.testTag("challenge-choice-$index"),
                )
            }
        }
    LazyColumn(
        modifier = Modifier.weight(1f).fillMaxWidth(),
        contentPadding = PaddingValues(vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        item {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = stringResource(when (selected) {
                        1 -> R.string.event_step_surge
                        2 -> R.string.event_night_quest
                        else -> R.string.challenge_daily_title
                    }),
                    fontSize = 29.sp, fontWeight = FontWeight.Black, color = Snow,
                )
                Box(Modifier.fillMaxWidth().height(230.dp).celebrate(celebration.takeIf { it > 0 }),
                    contentAlignment = Alignment.Center) {
                    look?.let {
                        com.stepup.android.ui.components.CharacterStage(
                            look = it, pose = com.stepup.android.domain.AvatarPose.IDLE,
                            modifier = Modifier.fillMaxSize(), skyline = false, animate = false, characterFraction = 0.95f,
                        )
                    } ?: androidx.compose.material3.CircularProgressIndicator()
                }
            }
        }

        // ── 일일 — 걸음 목표. 달성하면 보너스가 저절로 들어온다 ──
        if (selected == 0) item {
            val d = daily
            ChallengeCard(
                tag = stringResource(R.string.challenge_tag_daily),
                tagTone = BadgeTone.Accent,
                icon = Icons.AutoMirrored.Filled.DirectionsWalk,
                title = stringResource(R.string.challenge_daily_title),
                desc = stringResource(R.string.challenge_daily_desc),
                reward = if (d != null && d.paidToday > 0) d.paidToday else RewardEconomy.goalBaseBonus(d?.goal ?: 0),
                rewardNote = if (d != null && d.paidToday > 0) null else stringResource(R.string.challenge_daily_streak_note),
                fraction = d?.fraction,
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
        if (selected == 1) item {
            val done = weekFraction != null && weekFraction >= 1f
            ChallengeCard(
                tag = stringResource(R.string.challenge_tag_weekly),
                tagTone = BadgeTone.Nft,
                icon = Icons.Filled.Whatshot,
                title = stringResource(R.string.event_step_surge),
                desc = stringResource(R.string.event_step_surge_desc, "%,d".format(Events.STEP_SURGE.target.toLong())),
                reward = Events.STEP_SURGE.reward,
                fraction = weekFraction,
                progressText = weekSteps?.let { "%,d / %,d".format(it, Events.STEP_SURGE.target.toLong()) } ?: "—",
                state = if (claimingId == Events.STEP_SURGE.id) ChallengeState.Settling else claimState(Events.STEP_SURGE, claimed, done),
                onClaim = null,
            )
        }

        // ── 나이트 러너 — 저녁 8시 이후 러닝 거리 합 ──
        if (selected == 2) item {
            val done = nightFraction != null && nightFraction >= 1f
            ChallengeCard(
                tag = stringResource(R.string.tag_limited),
                tagTone = BadgeTone.Glow,
                icon = Icons.Filled.DarkMode,
                title = stringResource(R.string.event_night_quest),
                desc = stringResource(R.string.event_night_quest_desc),
                reward = Events.NIGHT_QUEST.reward,
                fraction = nightFraction,
                progressText = nightKm?.let { "%.1f / %.0f km".format(it, Events.NIGHT_QUEST.target) } ?: "—",
                state = if (claimingId == Events.NIGHT_QUEST.id) ChallengeState.Settling else claimState(Events.NIGHT_QUEST, claimed, done),
                onClaim = null,
            )
        }

    }
            PrimaryCta(
                text = stringResource(when {
                    claimingId != null -> R.string.challenge_state_settling
                    canClaim -> R.string.events_claim
                    else -> R.string.home_start_run
                }),
                icon = if (canClaim) null else Icons.AutoMirrored.Filled.DirectionsRun,
                enabled = claimingId == null,
                onClick = {
                    if (canClaim) viewModel.claim(selectedEvent!!, selectedFraction!!) else onStartRun()
                },
                modifier = Modifier.padding(vertical = 12.dp).testTag("challenge-primary-action"),
            )
    }
    }
}

private enum class ChallengeState { Loading, InProgress, Ready, Settling, Paid, Claimed, Soon }

/** 받기형 도전의 상태 — 받았으면 받음, 채웠으면 받기, 아니면 진행 중 */
private fun claimState(def: EventDef, claimed: Set<String>?, done: Boolean): ChallengeState = when {
    claimed == null -> ChallengeState.Loading
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
        contentPadding = PaddingValues(20.dp), spacing = 16.dp,
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            com.stepup.android.ui.components.IconSquare(icon, size = 48.dp)
            Text(title, fontSize = 22.sp, fontWeight = FontWeight.Bold, color = Snow, modifier = Modifier.weight(1f))
        }
        Text(desc, fontSize = 15.sp, lineHeight = 23.sp, color = Silver)
        when (state) {
            ChallengeState.Paid, ChallengeState.Claimed, ChallengeState.Settling -> StatusChip(state)
            ChallengeState.Loading -> Text("—", color = Silver)
            else -> RewardPill(reward)
        }
        if (rewardNote != null) {
            Text(text = rewardNote, fontSize = 14.sp, color = Silver)
        }
        if (fraction != null) {
            // 큰 진행값 한 줄, 그 아래 막대와 백분율
            Text(
                text = progressText,
                fontFamily = StepUpNumbers,
                fontSize = 20.sp,
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
                    color = VoltText,
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

/** 보상 알약 — "보상 ⬡ 30 SUP" */
@Composable
private fun RewardPill(reward: Double) {
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Text(stringResource(R.string.challenge_reward_label), fontSize = 14.sp, color = Silver)
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            HexEmblem(size = 22.dp, glow = false)
            Text("%,.0f".format(reward), fontFamily = StepUpNumbers, fontSize = 28.sp,
                fontWeight = FontWeight.Bold, color = Snow)
            Text("SUP", fontSize = 14.sp, color = Silver)
        }
    }
}

@Composable
private fun StatusChip(state: ChallengeState) {
    val text = when (state) {
        ChallengeState.Loading -> return
        ChallengeState.InProgress -> stringResource(R.string.challenge_state_progress)
        ChallengeState.Ready -> stringResource(R.string.challenge_state_ready)
        ChallengeState.Settling -> stringResource(R.string.challenge_state_settling)
        ChallengeState.Paid -> stringResource(R.string.challenge_state_paid)
        ChallengeState.Claimed -> stringResource(R.string.events_claimed)
        ChallengeState.Soon -> stringResource(R.string.challenge_soon)
    }
    Text(text, fontSize = 14.sp, fontWeight = FontWeight.SemiBold, color = VoltText)
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

    com.stepup.android.ui.components.DialogPanel(
        title = subject, onDismiss = onDismiss,
        actions = {
            VoltButton(stringResource(R.string.invite_send), { onSend(text.trim()) },
                Modifier.fillMaxWidth(), enabled = text.isNotBlank())
        },
    ) {
        com.stepup.android.ui.components.FormField(
            label = stringResource(R.string.invite_edit_hint), value = text,
            onValueChange = { if (it.length <= INVITE_MAX) text = it },
            singleLine = false, minLines = 4,
        )
        Text("${text.length} / $INVITE_MAX", fontSize = 14.sp, color = Silver,
            modifier = Modifier.align(Alignment.End))
    }
}

/** 초대 문구 길이 상한. 문자 메시지 한 통에 들어가는 정도. */
private const val INVITE_MAX = 300
