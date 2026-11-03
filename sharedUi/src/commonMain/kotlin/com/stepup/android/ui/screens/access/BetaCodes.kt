package com.stepup.android.ui.screens.access

import kotlinx.datetime.toLocalDateTime
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import org.jetbrains.compose.resources.stringResource
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.stepup.ui.res.*
import com.stepup.android.core.AccessGate
import com.stepup.android.core.AppServices
import com.stepup.android.data.remote.AccessCodeRow
import com.stepup.android.data.remote.AccessStatusRow
import com.stepup.android.data.remote.ServerResult
import com.stepup.android.ui.components.ButtonState
import com.stepup.android.ui.components.FormField
import com.stepup.android.ui.components.PrimaryButton
import com.stepup.android.ui.components.SecondaryHeader
import com.stepup.android.ui.components.StepUpUi
import com.stepup.android.ui.theme.StepUpDesign
import kotlin.time.Instant
import kotlinx.coroutines.launch
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material.icons.filled.Close
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.stepup.android.ui.components.pressable
import com.stepup.android.ui.screens.login.LoginContent
import com.stepup.android.ui.screens.login.LoginPhase

/**
 * 베타 초대 코드 — 코드로 들어온 사람(과 운영자가 고른 사람)이 하루 N개 만들어 나눈다.
 * 내가 만든 코드가 쓰였는지만 보인다(누가 썼는지는 서버도 앱에 주지 않는다).
 */
@Composable
fun BetaCodesScreen(onBack: () -> Unit) {
    val c = StepUpUi.colors
    val type = StepUpUi.type
    val scope = rememberCoroutineScope()
    var status by remember { mutableStateOf(AccessGate.state.value) }
    var codes by remember { mutableStateOf<List<AccessCodeRow>?>(null) }
    var failed by remember { mutableStateOf(false) }
    var busy by remember { mutableStateOf(false) }
    var message by remember { mutableStateOf<org.jetbrains.compose.resources.StringResource?>(null) }
    suspend fun reload() {
        status = AccessGate.refresh()
        when (val r = AppServices.accessApi.myCodes()) {
            is ServerResult.Ok -> { codes = r.value; failed = false }
            else -> failed = codes == null
        }
    }
    LaunchedEffect(Unit) { reload() }
    val left = status?.let { (it.dailyLimit - it.issuedToday).coerceAtLeast(0) } ?: 0
    Column(Modifier.fillMaxSize().background(c.background).testTag("beta-codes")) {
        SecondaryHeader(
            onBack = onBack, balance = null, onOpenWallet = null, title = stringResource(Res.string.beta_codes_title),
            modifier = Modifier.padding(horizontal = StepUpDesign.Gutter),
        )
        LazyColumn(
            Modifier.weight(1f).fillMaxWidth(),
            contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = StepUpDesign.Gutter, vertical = 8.dp),
        ) {
            item(key = "intro") {
                Text(
                    if (status?.canIssue == true) stringResource(Res.string.beta_codes_left, left, status?.dailyLimit ?: 0)
                    else stringResource(Res.string.beta_codes_not_allowed),
                    style = type.body, color = c.secondaryLabel, modifier = Modifier.padding(bottom = 14.dp).testTag("beta-codes-left"),
                )
                message?.let { Text(stringResource(it), style = type.caption, color = c.live, modifier = Modifier.padding(bottom = 10.dp)) }
            }
            when {
                failed -> item(key = "failed") {
                    Text(stringResource(Res.string.beta_codes_failed), style = type.body, color = c.secondaryLabel)
                }
                codes?.isEmpty() == true -> item(key = "empty") {
                    Text(stringResource(Res.string.beta_codes_empty), style = type.body, color = c.secondaryLabel)
                }
                else -> items(codes.orEmpty(), key = { it.code }) { row -> BetaCodeRow(row) }
            }
        }
        PrimaryButton(
            text = stringResource(Res.string.beta_codes_make),
            state = when {
                busy -> ButtonState.Loading
                status?.canIssue != true || left <= 0 -> ButtonState.Disabled
                else -> ButtonState.Idle
            },
            onClick = {
                if (busy) return@PrimaryButton
                busy = true
                message = null
                scope.launch {
                    when (val r = AppServices.accessApi.issue()) {
                        is ServerResult.Ok -> if (r.value.code == null) {
                            message = if (r.value.error == "daily_limit") Res.string.beta_codes_limit else Res.string.beta_codes_not_allowed
                        }
                        else -> message = Res.string.access_error_network
                    }
                    reload()
                    busy = false
                }
            },
            modifier = Modifier.fillMaxWidth().padding(horizontal = StepUpDesign.Gutter, vertical = 16.dp).testTag("beta-codes-make"),
        )
    }
}

@Composable
private fun BetaCodeRow(row: AccessCodeRow) {
    val c = StepUpUi.colors
    val type = StepUpUi.type
    val open = row.status == "OPEN"
    val pattern = stringResource(Res.string.beta_codes_date)
    val language = androidx.compose.ui.text.intl.Locale.current.language
    fun fmt(iso: String?) = iso?.let {
        runCatching {
            com.stepup.android.domain.DatePattern.format(
                kotlin.time.Instant.parse(it).toEpochMilliseconds(), kotlinx.datetime.TimeZone.currentSystemDefault(), pattern, language,
            )
        }.getOrNull()
    }
    val statusText = when (row.status) {
        "USED" -> stringResource(Res.string.beta_code_used, fmt(row.usedAt) ?: "")
        "EXPIRED" -> stringResource(Res.string.beta_code_expired)
        "REVOKED" -> stringResource(Res.string.beta_code_revoked)
        else -> stringResource(Res.string.beta_code_open, fmt(row.expiresAt) ?: "")
    }
    val share = stringResource(Res.string.beta_code_share_text, row.code)
    Row(
        Modifier.fillMaxWidth().padding(vertical = 5.dp).clip(RoundedCornerShape(16.dp)).background(c.card)
            .heightIn(min = 64.dp).padding(start = 16.dp, end = 4.dp, top = 10.dp, bottom = 10.dp).testTag("beta-code-${row.code}"),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(Modifier.weight(1f)) {
            Text(row.code, style = type.rowTitle, color = if (open) c.label else c.tertiaryLabel)
            Text(statusText, style = type.caption, color = c.secondaryLabel)
        }
        if (open) {
            IconButton(onClick = {
                com.stepup.android.core.ExternalIntents.copyText(row.code)
            }, modifier = Modifier.size(48.dp)) {
                Icon(Icons.Filled.ContentCopy, stringResource(Res.string.beta_code_copy), tint = c.secondaryLabel)
            }
            IconButton(onClick = {
                com.stepup.android.core.ExternalIntents.shareText(share)
            }, modifier = Modifier.size(48.dp)) {
                Icon(Icons.Filled.Share, stringResource(Res.string.beta_code_share), tint = c.secondaryLabel)
            }
        }
    }
}
