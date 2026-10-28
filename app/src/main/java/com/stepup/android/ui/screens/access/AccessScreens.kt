package com.stepup.android.ui.screens.access

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.stepup.android.R
import com.stepup.android.core.AccessGate
import com.stepup.android.core.ServiceLocator
import com.stepup.android.data.remote.AccessCodeRow
import com.stepup.android.data.remote.AccessStatusRow
import com.stepup.android.data.remote.ServerResult
import com.stepup.android.ui.components.ButtonState
import com.stepup.android.ui.components.FormField
import com.stepup.android.ui.components.PrimaryButton
import com.stepup.android.ui.components.SecondaryButton
import com.stepup.android.ui.components.SecondaryHeader
import com.stepup.android.ui.components.StepUpUi
import com.stepup.android.ui.theme.StepUpDesign
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import kotlinx.coroutines.launch

/**
 * 접속 코드 입력(서버 0079) — 비공개 베타 · 공개 베타에서 권한이 없는 사람에게만 보인다.
 * 로그인 안 한 사람(예전 둘러보기)은 구글로 로그인부터. 코드가 맞으면 서버가 권한을 주고 앱으로 들어간다.
 */
@Composable
fun AccessCodeScreen(
    status: AccessStatusRow,
    signedIn: Boolean,
    onGranted: () -> Unit,
    onSignOut: () -> Unit,
    redeem: suspend (String) -> ServerResult<com.stepup.android.data.remote.AccessRedeemRow> = { ServiceLocator.accessApi.redeem(it) },
) {
    val c = StepUpUi.colors
    val type = StepUpUi.type
    val scope = rememberCoroutineScope()
    var code by rememberSaveable { mutableStateOf("") }
    var busy by remember { mutableStateOf(false) }
    var error by rememberSaveable { mutableStateOf<Int?>(null) }
    Box(Modifier.fillMaxSize().background(c.background).safeDrawingPadding().imePadding().testTag("access-gate")) {
        Column(
            Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = StepUpDesign.Gutter),
        ) {
            Spacer(Modifier.height(56.dp))
            Text(
                stringResource(if (status.mode == "OPEN_BETA") R.string.access_title_open_beta else R.string.access_title_closed_beta),
                style = type.largeTitle, color = c.label, modifier = Modifier.semantics { heading() },
            )
            Spacer(Modifier.height(10.dp))
            Text(
                stringResource(
                    when {
                        !signedIn -> R.string.access_body_sign_in
                        status.revoked -> R.string.access_body_revoked
                        status.mode == "OPEN_BETA" -> R.string.access_body_open_beta
                        else -> R.string.access_body_closed_beta
                    },
                ),
                style = type.body, color = c.secondaryLabel,
            )
            if (signedIn && !status.revoked) {
                Spacer(Modifier.height(28.dp))
                FormField(
                    label = stringResource(R.string.access_code_label), value = code,
                    onValueChange = { v -> code = v.take(40); error = null },
                    placeholder = "XXXX-XXXX", modifier = Modifier.fillMaxWidth().testTag("access-code-field"),
                )
                error?.let {
                    Spacer(Modifier.height(8.dp))
                    Text(stringResource(it), style = type.caption, color = c.live, modifier = Modifier.testTag("access-error"))
                }
            }
            Spacer(Modifier.height(24.dp))
        }
        Column(
            Modifier.align(Alignment.BottomCenter).fillMaxWidth().padding(horizontal = StepUpDesign.Gutter, vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            if (signedIn && !status.revoked) {
                PrimaryButton(
                    text = stringResource(R.string.access_enter),
                    state = when {
                        busy -> ButtonState.Loading
                        code.count { it.isLetterOrDigit() } < 6 -> ButtonState.Disabled
                        else -> ButtonState.Idle
                    },
                    onClick = {
                        if (busy) return@PrimaryButton
                        busy = true
                        scope.launch {
                            val r = redeem(code)
                            busy = false
                            when {
                                r is ServerResult.Ok && r.value.ok -> onGranted()
                                r is ServerResult.Ok -> error = errorText(r.value.error)
                                else -> error = R.string.access_error_network
                            }
                        }
                    },
                    modifier = Modifier.fillMaxWidth().testTag("access-enter"),
                )
            }
            SecondaryButton(
                text = stringResource(if (signedIn) R.string.access_other_account else R.string.access_sign_in),
                onClick = onSignOut, modifier = Modifier.fillMaxWidth().testTag("access-sign-out"),
            )
        }
    }
}

internal fun errorText(error: String?): Int = when (error) {
    "not_found" -> R.string.access_error_not_found
    "revoked" -> R.string.access_error_revoked
    "expired" -> R.string.access_error_expired
    "used_up" -> R.string.access_error_used_up
    "too_many" -> R.string.access_error_too_many
    "blocked" -> R.string.access_body_revoked
    else -> R.string.access_error_network
}

/**
 * 베타 초대 코드 — 코드로 들어온 사람(과 운영자가 고른 사람)이 하루 N개 만들어 나눈다.
 * 내가 만든 코드가 쓰였는지만 보인다(누가 썼는지는 서버도 앱에 주지 않는다).
 */
@Composable
fun BetaCodesScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    val c = StepUpUi.colors
    val type = StepUpUi.type
    val scope = rememberCoroutineScope()
    var status by remember { mutableStateOf(AccessGate.state.value) }
    var codes by remember { mutableStateOf<List<AccessCodeRow>?>(null) }
    var failed by remember { mutableStateOf(false) }
    var busy by remember { mutableStateOf(false) }
    var message by remember { mutableStateOf<Int?>(null) }
    suspend fun reload() {
        status = AccessGate.refresh(context)
        when (val r = ServiceLocator.accessApi.myCodes()) {
            is ServerResult.Ok -> { codes = r.value; failed = false }
            else -> failed = codes == null
        }
    }
    LaunchedEffect(Unit) { reload() }
    val left = status?.let { (it.dailyLimit - it.issuedToday).coerceAtLeast(0) } ?: 0
    Column(Modifier.fillMaxSize().background(c.background).testTag("beta-codes")) {
        SecondaryHeader(
            onBack = onBack, balance = null, onOpenWallet = null, title = stringResource(R.string.beta_codes_title),
            modifier = Modifier.padding(horizontal = StepUpDesign.Gutter),
        )
        LazyColumn(
            Modifier.weight(1f).fillMaxWidth(),
            contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = StepUpDesign.Gutter, vertical = 8.dp),
        ) {
            item(key = "intro") {
                Text(
                    if (status?.canIssue == true) stringResource(R.string.beta_codes_left, left, status?.dailyLimit ?: 0)
                    else stringResource(R.string.beta_codes_not_allowed),
                    style = type.body, color = c.secondaryLabel, modifier = Modifier.padding(bottom = 14.dp).testTag("beta-codes-left"),
                )
                message?.let { Text(stringResource(it), style = type.caption, color = c.live, modifier = Modifier.padding(bottom = 10.dp)) }
            }
            when {
                failed -> item(key = "failed") {
                    Text(stringResource(R.string.beta_codes_failed), style = type.body, color = c.secondaryLabel)
                }
                codes?.isEmpty() == true -> item(key = "empty") {
                    Text(stringResource(R.string.beta_codes_empty), style = type.body, color = c.secondaryLabel)
                }
                else -> items(codes.orEmpty(), key = { it.code }) { row -> BetaCodeRow(row, context) }
            }
        }
        PrimaryButton(
            text = stringResource(R.string.beta_codes_make),
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
                    when (val r = ServiceLocator.accessApi.issue()) {
                        is ServerResult.Ok -> if (r.value.code == null) {
                            message = if (r.value.error == "daily_limit") R.string.beta_codes_limit else R.string.beta_codes_not_allowed
                        }
                        else -> message = R.string.access_error_network
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
private fun BetaCodeRow(row: AccessCodeRow, context: Context) {
    val c = StepUpUi.colors
    val type = StepUpUi.type
    val open = row.status == "OPEN"
    val day = DateTimeFormatter.ofPattern(stringResource(R.string.beta_codes_date))
    fun fmt(iso: String?) = iso?.let { runCatching { Instant.parse(it).atZone(ZoneId.systemDefault()).format(day) }.getOrNull() }
    val statusText = when (row.status) {
        "USED" -> stringResource(R.string.beta_code_used, fmt(row.usedAt) ?: "")
        "EXPIRED" -> stringResource(R.string.beta_code_expired)
        "REVOKED" -> stringResource(R.string.beta_code_revoked)
        else -> stringResource(R.string.beta_code_open, fmt(row.expiresAt) ?: "")
    }
    val share = stringResource(R.string.beta_code_share_text, row.code)
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
                val clip = context.getSystemService(ClipboardManager::class.java)
                clip?.setPrimaryClip(ClipData.newPlainText("StepUp", row.code))
            }, modifier = Modifier.size(48.dp)) {
                Icon(Icons.Filled.ContentCopy, stringResource(R.string.beta_code_copy), tint = c.secondaryLabel)
            }
            IconButton(onClick = {
                runCatching {
                    context.startActivity(
                        Intent.createChooser(Intent(Intent.ACTION_SEND).setType("text/plain").putExtra(Intent.EXTRA_TEXT, share), null)
                            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK),
                    )
                }
            }, modifier = Modifier.size(48.dp)) {
                Icon(Icons.Filled.Share, stringResource(R.string.beta_code_share), tint = c.secondaryLabel)
            }
        }
    }
}
