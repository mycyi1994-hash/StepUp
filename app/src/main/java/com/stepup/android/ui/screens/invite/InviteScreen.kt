package com.stepup.android.ui.screens.invite

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Intent
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import androidx.lifecycle.viewmodel.compose.viewModel
import com.stepup.android.R
import com.stepup.android.core.ServiceLocator
import com.stepup.android.data.remote.InviteApi
import com.stepup.android.data.remote.InviteStatusRow
import com.stepup.android.data.remote.InviteeRow
import com.stepup.android.data.remote.ServerResult
import com.stepup.android.ui.components.DialogPanel
import com.stepup.android.ui.components.FocusHeader
import com.stepup.android.ui.components.FormField
import com.stepup.android.ui.components.GhostButton
import com.stepup.android.ui.components.S2ActionRow
import com.stepup.android.ui.components.S2Headline
import com.stepup.android.ui.components.S2Kicker
import com.stepup.android.ui.components.S2Number
import com.stepup.android.ui.components.S2RoundAction
import com.stepup.android.ui.components.S2SideInfo
import com.stepup.android.ui.components.S2Subtitle
import com.stepup.android.ui.components.StatePanel
import com.stepup.android.ui.components.VoltButton
import com.stepup.android.ui.theme.Silver
import com.stepup.android.ui.theme.Slate
import com.stepup.android.ui.theme.Snow
import com.stepup.android.ui.theme.StepUpColors
import com.stepup.android.ui.theme.StepUpDesign
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

sealed interface InviteUi {
    data object Loading : InviteUi
    /** 로그인하지 않았거나 서버가 설정되지 않았다 */
    data object SignedOut : InviteUi
    data object Failed : InviteUi
    data class Ready(val status: InviteStatusRow, val invitees: List<InviteeRow>) : InviteUi
}

sealed interface RedeemResult {
    data class Done(val inviter: String) : RedeemResult
    data class Rejected(val reason: String) : RedeemResult
    data object Failed : RedeemResult
}

class InviteViewModel(private val api: InviteApi?) : ViewModel() {
    private val _ui = MutableStateFlow<InviteUi>(InviteUi.Loading)
    val ui: StateFlow<InviteUi> = _ui.asStateFlow()
    private val _redeem = MutableStateFlow<RedeemResult?>(null)
    val redeem: StateFlow<RedeemResult?> = _redeem.asStateFlow()
    private val _busy = MutableStateFlow(false)
    val busy: StateFlow<Boolean> = _busy.asStateFlow()

    init { load() }

    fun load() {
        val api = api?.takeIf { it.isConfigured } ?: run { _ui.value = InviteUi.SignedOut; return }
        viewModelScope.launch {
            _ui.value = InviteUi.Loading
            _ui.value = when (val status = api.status()) {
                is ServerResult.Ok -> {
                    val list = (api.invitees() as? ServerResult.Ok)?.value.orEmpty()
                    InviteUi.Ready(status.value, list)
                }
                is ServerResult.SignInRequired -> InviteUi.SignedOut
                else -> InviteUi.Failed
            }
        }
    }

    fun redeem(code: String) {
        val api = api ?: return
        if (_busy.value) return
        viewModelScope.launch {
            _busy.value = true
            _redeem.value = when (val result = api.redeem(code.trim())) {
                is ServerResult.Ok -> RedeemResult.Done(result.value)
                is ServerResult.Rejected -> RedeemResult.Rejected(result.reason)
                else -> RedeemResult.Failed
            }
            _busy.value = false
            if (_redeem.value is RedeemResult.Done) load()
        }
    }

    fun consumeRedeem() { _redeem.value = null }

    companion object {
        val Factory = viewModelFactory {
            initializer {
                InviteViewModel(runCatching { ServiceLocator.inviteApi }.getOrNull())
            }
        }
    }
}

/** 적립액을 SUP 표기로 — 0 이하이면 보상이 정해지지 않은 것이라 null */
internal fun rewardLabel(reward: Double): String? = when {
    reward <= 0 || !reward.isFinite() -> null
    reward % 1.0 == 0.0 -> com.stepup.android.ui.components.formatSupDown(reward, 0)
    else -> com.stepup.android.ui.components.formatSupDown(reward, 2)
}

/** S2 친구 초대(시안 28). 금액 · 확정은 서버 값만 쓴다. */
@Composable
fun InviteScreen(onBack: () -> Unit, viewModel: InviteViewModel = viewModel(factory = InviteViewModel.Factory)) {
    val ui by viewModel.ui.collectAsStateWithLifecycle()
    val redeem by viewModel.redeem.collectAsStateWithLifecycle()
    val busy by viewModel.busy.collectAsStateWithLifecycle()
    val context = LocalContext.current
    var showRedeem by rememberSaveable { mutableStateOf(false) }
    LaunchedEffect(redeem) {
        when (val r = redeem ?: return@LaunchedEffect) {
            is RedeemResult.Done -> {
                showRedeem = false
                Toast.makeText(context, context.getString(R.string.invite_redeem_done, r.inviter), Toast.LENGTH_SHORT).show()
            }
            is RedeemResult.Rejected -> Toast.makeText(context, r.reason, Toast.LENGTH_SHORT).show()
            RedeemResult.Failed -> Toast.makeText(context, context.getString(R.string.toast_offline), Toast.LENGTH_SHORT).show()
        }
        viewModel.consumeRedeem()
    }
    Column(Modifier.fillMaxSize().padding(horizontal = StepUpDesign.Gutter).padding(bottom = 12.dp).testTag("invite-screen")) {
        FocusHeader(stringResource(R.string.invite_title), onBack = onBack)
        when (val state = ui) {
            InviteUi.Loading -> Box(Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
                StatePanel(stringResource(R.string.feed_loading), Icons.Filled.Share, loading = true)
            }
            InviteUi.SignedOut -> Box(Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
                StatePanel(stringResource(R.string.invite_sign_in), Icons.Filled.Share)
            }
            InviteUi.Failed -> Box(Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
                StatePanel(stringResource(R.string.invite_failed), Icons.Filled.Share,
                    action = { GhostButton(stringResource(R.string.feed_retry), onClick = viewModel::load) })
            }
            is InviteUi.Ready -> InviteContent(
                state.status, state.invitees,
                modifier = Modifier.weight(1f),
                onCopy = {
                    val clipboard = context.getSystemService(ClipboardManager::class.java)
                    clipboard?.setPrimaryClip(ClipData.newPlainText("StepUp", state.status.code))
                    Toast.makeText(context, context.getString(R.string.invite_copied), Toast.LENGTH_SHORT).show()
                },
                onShare = {
                    val text = context.getString(R.string.invite_share_text, state.status.code)
                    val send = Intent(Intent.ACTION_SEND).apply {
                        type = "text/plain"
                        putExtra(Intent.EXTRA_SUBJECT, context.getString(R.string.invite_subject))
                        putExtra(Intent.EXTRA_TEXT, text)
                    }
                    context.startActivity(Intent.createChooser(send, context.getString(R.string.invite_subject)))
                },
                onRedeem = { showRedeem = true },
            )
        }
    }
    if (showRedeem) {
        var code by rememberSaveable { mutableStateOf("") }
        DialogPanel(
            title = stringResource(R.string.invite_redeem_title),
            onDismiss = { showRedeem = false },
            actions = {
                VoltButton(stringResource(R.string.invite_redeem_submit), { viewModel.redeem(code) },
                    Modifier.fillMaxWidth().testTag("invite-redeem-submit"), enabled = code.isNotBlank() && !busy)
                GhostButton(stringResource(R.string.common_cancel), { showRedeem = false }, Modifier.fillMaxWidth())
            },
        ) {
            Text(stringResource(R.string.invite_redeem_body), color = Silver, style = MaterialTheme.typography.bodyMedium)
            FormField(
                label = stringResource(R.string.invite_redeem_field), value = code,
                onValueChange = { if (it.length <= 16) code = it.uppercase() },
                placeholder = "STEP-XXXXXX", modifier = Modifier.testTag("invite-redeem-field"),
            )
        }
    }
}

@Composable
internal fun InviteContent(
    status: InviteStatusRow,
    invitees: List<InviteeRow>,
    modifier: Modifier = Modifier,
    onCopy: () -> Unit = {},
    onShare: () -> Unit = {},
    onRedeem: () -> Unit = {},
) {
    val reward = rewardLabel(status.rewardSup)
    Column(modifier.fillMaxWidth()) {
        Column(
            Modifier.weight(1f).fillMaxWidth().verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Spacer(Modifier.height(8.dp))
            S2Kicker(stringResource(R.string.invite_kicker))
            Spacer(Modifier.height(12.dp))
            S2Headline(
                if (reward != null) stringResource(R.string.invite_headline_reward, reward)
                else stringResource(R.string.invite_headline),
            )
            Spacer(Modifier.height(10.dp))
            S2Subtitle(stringResource(if (reward != null) R.string.invite_subtitle_reward else R.string.invite_subtitle))
            Spacer(Modifier.height(28.dp))
            Text(stringResource(R.string.invite_code_caption), color = Silver, style = MaterialTheme.typography.bodySmall)
            Spacer(Modifier.height(8.dp))
            S2Number(status.code, 40.sp, modifier = Modifier.testTag("invite-code"))
            TextButton(onClick = onCopy, modifier = Modifier.testTag("invite-copy")) {
                Icon(Icons.Filled.ContentCopy, contentDescription = null, tint = Silver, modifier = Modifier.size(16.dp))
                Spacer(Modifier.size(6.dp))
                Text(stringResource(R.string.invite_copy), color = Silver)
            }
            Spacer(Modifier.height(20.dp))
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Text(stringResource(R.string.invite_friends, status.invited), color = Snow,
                    style = MaterialTheme.typography.titleSmall, modifier = Modifier.weight(1f))
                Text(stringResource(R.string.invite_confirmed_count, status.rewarded), color = Silver,
                    style = MaterialTheme.typography.bodySmall)
            }
            Spacer(Modifier.height(8.dp))
            if (invitees.isEmpty()) {
                Text(stringResource(R.string.invite_empty), color = Slate, style = MaterialTheme.typography.bodySmall,
                    textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth().padding(vertical = 16.dp))
            }
            invitees.forEach { InviteeLine(it) }
        }
        S2ActionRow(
            start = {
                if (reward != null) S2SideInfo(stringResource(R.string.invite_per_friend), value = "$reward SUP")
            },
            end = {
                if (status.canRedeem) {
                    S2SideInfo(stringResource(R.string.invite_redeem_title), end = true, onClick = onRedeem,
                        modifier = Modifier.testTag("invite-redeem"))
                }
            },
        ) {
            S2RoundAction(Icons.Filled.Share, stringResource(R.string.invite_send_action), onShare,
                modifier = Modifier.testTag("invite-share"))
        }
    }
}

@Composable
private fun InviteeLine(row: InviteeRow) {
    val date = runCatching {
        java.time.OffsetDateTime.parse(row.joinedAt).atZoneSameInstant(java.time.ZoneId.systemDefault())
            .toLocalDate().let { "${it.monthValue}/${it.dayOfMonth}" }
    }.getOrDefault("")
    val confirmed = row.rewardedAt != null
    Row(
        Modifier.fillMaxWidth().heightIn(min = 52.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Box(Modifier.size(32.dp).background(StepUpColors.carbonHigh, CircleShape), contentAlignment = Alignment.Center) {
            Text(row.displayName.take(1), color = Snow, fontSize = 13.sp)
        }
        Column(Modifier.weight(1f)) {
            Text(row.displayName, color = Snow, style = MaterialTheme.typography.bodyMedium, maxLines = 1)
            Text(date, color = Slate, style = MaterialTheme.typography.bodySmall)
        }
        Text(
            stringResource(if (confirmed) R.string.invite_status_confirmed else R.string.invite_status_waiting),
            color = if (confirmed) Snow else Silver,
            style = MaterialTheme.typography.labelMedium,
            modifier = Modifier
                .background(if (confirmed) StepUpColors.carbonHigh else StepUpColors.carbon, RoundedCornerShape(50))
                .padding(horizontal = 10.dp, vertical = 4.dp),
        )
    }
}
