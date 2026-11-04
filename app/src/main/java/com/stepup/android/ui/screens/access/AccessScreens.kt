package com.stepup.android.ui.screens.access

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
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
import com.stepup.android.ui.components.SecondaryHeader
import com.stepup.android.ui.components.StepUpUi
import com.stepup.android.ui.theme.StepUpDesign
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import kotlinx.coroutines.launch
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material.icons.filled.Close
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.compose.ui.window.DialogWindowProvider
import com.stepup.android.ui.components.pressable
import com.stepup.android.ui.screens.login.LoginContent
import com.stepup.android.ui.screens.login.LoginPhase

/**
 * 접속 코드 입력(서버 0079) — 비공개 베타 · 공개 베타에서 권한이 없는 사람에게만 보인다.
 *
 * 2026-10-29 핸드오프(StepUP_AccessCode_Modal_HANDOFF): 전체 화면이 아니라 로그인 화면([LoginContent]) 위 가운데 모달.
 * · X · 뒤로 가기는 모달만 닫는다 — 로그인 화면만 남고, 그 화면의 구글 버튼을 누르면 모달이 다시 뜬다(이미 로그인했으면 구글을 다시 열지 않는다).
 * · 모달 밖을 눌러서는 닫히지 않는다. 닫는다고 앱에 들어가지 않는다 — 들어가는 것은 서버가 코드를 받고 [AccessGate] 가 권한을 볼 때뿐.
 * · 확인 중에 닫으면 그 요청의 답은 버리고(화면을 바꾸지 않는다) 서버에 다시 묻는다(이미 받았을 수 있다).
 * 로그인 안 한 사람(예전 둘러보기)은 구글로 로그인부터.
 */
@Composable
fun AccessCodeScreen(
    status: AccessStatusRow,
    signedIn: Boolean,
    onGranted: () -> Unit,
    onSignOut: () -> Unit,
    redeem: suspend (String) -> ServerResult<com.stepup.android.data.remote.AccessRedeemRow> = { ServiceLocator.accessApi.redeem(it) },
    checkFailed: Boolean = false,
) {
    // 모달이 열렸는가 — 화면 상태일 뿐, 접속 권한(서버 · AccessGate)과 따로 둔다
    var open by rememberSaveable { mutableStateOf(true) }
    Box(Modifier.fillMaxSize().testTag("access-gate")) {
        LoginContent(phase = LoginPhase.Idle, notice = null, onSignIn = { open = true })
        if (open) {
            AccessCodeModal(
                status = status, signedIn = signedIn, redeem = redeem, onGranted = onGranted, onSignOut = onSignOut,
                checkFailed = checkFailed,
                onClose = { wasChecking ->
                    open = false
                    // 확인 중에 닫았다 — 서버가 이미 받았을 수 있으니 권한을 다시 묻는다(답에 따라 AccessGate 가 정한다)
                    if (wasChecking) onGranted()
                },
            )
        }
    }
}

@Composable
private fun AccessCodeModal(
    status: AccessStatusRow,
    signedIn: Boolean,
    redeem: suspend (String) -> ServerResult<com.stepup.android.data.remote.AccessRedeemRow>,
    onGranted: () -> Unit,
    onSignOut: () -> Unit,
    onClose: (wasChecking: Boolean) -> Unit,
    checkFailed: Boolean = false,
) {
    val c = StepUpUi.colors
    val type = StepUpUi.type
    // 모달이 닫히면 이 범위가 끝나 확인 중인 요청의 늦은 답도 버려진다
    val scope = rememberCoroutineScope()
    var code by rememberSaveable { mutableStateOf("") }
    var busy by remember { mutableStateOf(false) }
    var error by rememberSaveable { mutableStateOf<Int?>(null) }
    val canEnter = signedIn && !status.revoked
    Dialog(
        onDismissRequest = { onClose(busy) },
        properties = DialogProperties(
            dismissOnBackPress = true, dismissOnClickOutside = false,
            usePlatformDefaultWidth = false, decorFitsSystemWindows = false,
        ),
    ) {
        // 뒤 로그인 화면이 보이게 검정 32% 딤만(창의 기본 딤 대신)
        val window = (LocalView.current.parent as? DialogWindowProvider)?.window
        SideEffect { window?.setDimAmount(0.32f) }
        Box(
            Modifier.fillMaxSize().safeDrawingPadding().imePadding().padding(horizontal = 24.dp, vertical = 16.dp),
            contentAlignment = Alignment.Center,
        ) {
            Column(
                Modifier.widthIn(max = 440.dp).fillMaxWidth()
                    .clip(RoundedCornerShape(24.dp)).background(c.elevated)
                    .verticalScroll(rememberScrollState())
                    .padding(20.dp)
                    .testTag("access-modal"),
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        stringResource(if (status.mode == "OPEN_BETA") R.string.access_title_open_beta else R.string.access_title_closed_beta),
                        style = type.section, color = c.label, modifier = Modifier.weight(1f).semantics { heading() },
                    )
                    IconButton(onClick = { onClose(busy) }, modifier = Modifier.size(48.dp).testTag("access-close")) {
                        Icon(Icons.Filled.Close, stringResource(R.string.common_close), tint = c.secondaryLabel)
                    }
                }
                Spacer(Modifier.height(8.dp))
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
                if (canEnter) {
                    Spacer(Modifier.height(20.dp))
                    FormField(
                        label = stringResource(R.string.access_code_label), value = code,
                        // 확인 중에는 바꾸지 않는다 · 고치면 앞 오류를 지운다
                        onValueChange = { v -> if (!busy) { code = pastedAccessCode(v).take(40); error = null } },
                        placeholder = "XXXX-XXXX", keyboardType = KeyboardType.Ascii,
                        modifier = Modifier.fillMaxWidth().testTag("access-code-field"),
                    )
                    error?.let {
                        Spacer(Modifier.height(8.dp))
                        Text(
                            stringResource(it), style = type.caption, color = c.live,
                            modifier = Modifier.semantics { liveRegion = LiveRegionMode.Polite }.testTag("access-error"),
                        )
                    }
                    Spacer(Modifier.height(20.dp))
                    PrimaryButton(
                        text = stringResource(R.string.access_enter),
                        state = when {
                            busy -> ButtonState.Loading
                            code.count { it.isLetterOrDigit() } < 6 -> ButtonState.Disabled
                            else -> ButtonState.Idle
                        },
                        arrow = false,
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
                } else {
                    Spacer(Modifier.height(20.dp))
                }
                Spacer(Modifier.height(4.dp))
                // 권한을 묻지 못했다 — 이미 들어온 사람이 연결 탓에 막히지 않게 조용히 다시 묻는다(주 버튼이 아닌 글자)
                if (signedIn && checkFailed) {
                    Box(
                        Modifier.fillMaxWidth().heightIn(min = 48.dp).clip(RoundedCornerShape(12.dp))
                            .pressable(enabled = !busy, onClick = onGranted).testTag("access-check-again"),
                        contentAlignment = Alignment.Center,
                    ) {
                        Text(stringResource(R.string.access_check_again), style = type.smallButton, color = c.secondaryLabel)
                    }
                }
                Box(
                    Modifier.fillMaxWidth().heightIn(min = 48.dp).clip(RoundedCornerShape(12.dp))
                        .pressable(enabled = !busy, onClick = onSignOut).testTag("access-sign-out"),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        stringResource(if (signedIn) R.string.access_other_account else R.string.access_sign_in),
                        style = type.smallButton, color = c.secondaryLabel,
                    )
                }
            }
        }
    }
}

private val CODE_IN_TEXT = Regex("(?<![A-Za-z0-9])([A-Za-z0-9]{4})-([A-Za-z0-9]{4})(?![A-Za-z0-9])")
private val BARE_CODE_IN_TEXT = Regex("(?<![A-Za-z0-9])[A-Z0-9]{8}(?![A-Za-z0-9])")

/**
 * 공유받은 글 통째로 붙여 넣었을 때 코드만 꺼낸다 — 글 안에 XXXX-XXXX(또는 대문자 8자)가 있으면 그것.
 * 코드 글자(영문 · 숫자 · '-')만 쳤으면 그대로 둔다.
 */
internal fun pastedAccessCode(raw: String): String {
    val trimmed = raw.trim()
    if (trimmed.all { it.isLetterOrDigit() && it.code < 128 || it == '-' }) return raw
    CODE_IN_TEXT.find(raw)?.let { return it.value }
    return BARE_CODE_IN_TEXT.find(raw)?.value ?: raw
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
    // 이번에 서버에서 내 상태를 받았는가 — 폰에 적어 둔 상태에는 "만들 수 있는가"가 없다(끊긴 채로 "만들 수 없어요"라고 하지 않게)
    var statusKnown by remember { mutableStateOf(false) }
    var codes by remember { mutableStateOf<List<AccessCodeRow>?>(null) }
    var failed by remember { mutableStateOf(false) }
    var loading by remember { mutableStateOf(true) }
    var busy by remember { mutableStateOf(false) }
    var message by remember { mutableStateOf<Int?>(null) }
    suspend fun reload() {
        loading = true
        try {
            status = AccessGate.refresh(context)
            statusKnown = !AccessGate.failed.value
            when (val r = ServiceLocator.accessApi.myCodes()) {
                is ServerResult.Ok -> { codes = r.value; failed = false }
                else -> failed = codes == null
            }
        } finally {
            loading = false
        }
    }
    LaunchedEffect(Unit) { reload() }
    val left = status?.let { (it.dailyLimit - it.issuedToday).coerceAtLeast(0) } ?: 0
    val view = betaCodesView(loading, statusKnown, status?.canIssue == true)
    val retry: () -> Unit = {
        if (!loading && !busy) {
            message = null
            scope.launch { reload() }
        }
    }
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
                when (view) {
                    BetaCodesView.LOADING -> Unit
                    else -> Text(
                        when (view) {
                            BetaCodesView.ALLOWED -> stringResource(R.string.beta_codes_left, left, status?.dailyLimit ?: 0)
                            BetaCodesView.OFFLINE -> stringResource(R.string.fx4_beta_codes_offline)
                            else -> stringResource(R.string.beta_codes_not_allowed)
                        },
                        style = type.body, color = c.secondaryLabel, modifier = Modifier.padding(bottom = 14.dp).testTag("beta-codes-left"),
                    )
                }
                message?.let { Text(stringResource(it), style = type.caption, color = c.live, modifier = Modifier.padding(bottom = 10.dp)) }
            }
            when {
                // 처음 불러오는 중 — 빈 목록 · 실패로 보이지 않게
                codes == null && loading -> item(key = "loading") {
                    Box(Modifier.fillMaxWidth().padding(vertical = 32.dp).testTag("beta-codes-loading"), contentAlignment = Alignment.Center) {
                        androidx.compose.material3.CircularProgressIndicator(Modifier.size(28.dp), color = c.secondaryLabel, strokeWidth = 2.5.dp)
                    }
                }
                failed -> item(key = "failed") {
                    Column(Modifier.testTag("beta-codes-failed")) {
                        Text(stringResource(R.string.beta_codes_failed), style = type.body, color = c.secondaryLabel)
                        // 아래 주 버튼이 이미 "다시 시도"면 같은 버튼을 두 번 두지 않는다
                        if (view != BetaCodesView.OFFLINE) {
                            androidx.compose.material3.TextButton(onClick = retry, modifier = Modifier.testTag("beta-codes-retry")) {
                                Text(stringResource(R.string.fx4_retry), style = type.body)
                            }
                        }
                    }
                }
                codes?.isEmpty() == true -> item(key = "empty") {
                    Text(stringResource(R.string.beta_codes_empty), style = type.body, color = c.secondaryLabel)
                }
                else -> items(codes.orEmpty(), key = { it.code }) { row -> BetaCodeRow(row, context) }
            }
        }
        if (view == BetaCodesView.OFFLINE) {
            // 내 상태를 받지 못했다 — "만들 수 없어요"로 막지 않고 다시 묻는다
            PrimaryButton(
                text = stringResource(R.string.fx4_retry),
                state = if (loading) ButtonState.Loading else ButtonState.Idle,
                onClick = retry,
                modifier = Modifier.fillMaxWidth().padding(horizontal = StepUpDesign.Gutter, vertical = 16.dp).testTag("beta-codes-make"),
            )
            return@Column
        }
        PrimaryButton(
            text = stringResource(R.string.beta_codes_make),
            state = when {
                busy || view == BetaCodesView.LOADING -> ButtonState.Loading
                view != BetaCodesView.ALLOWED || left <= 0 -> ButtonState.Disabled
                else -> ButtonState.Idle
            },
            onClick = {
                if (busy || loading) return@PrimaryButton
                busy = true
                message = null
                scope.launch {
                    try {
                        when (val r = ServiceLocator.accessApi.issue()) {
                            is ServerResult.Ok -> if (r.value.code == null) {
                                message = if (r.value.error == "daily_limit") R.string.beta_codes_limit else R.string.beta_codes_not_allowed
                            }
                            else -> message = R.string.access_error_network
                        }
                        reload()
                    } finally {
                        busy = false
                    }
                }
            },
            modifier = Modifier.fillMaxWidth().padding(horizontal = StepUpDesign.Gutter, vertical = 16.dp).testTag("beta-codes-make"),
        )
    }
}

/** 베타 초대 코드 화면의 윗글 · 주 버튼 — 불러오는 중 · 만들 수 있음 · 만들 수 없음(서버 답) · 끊김(답을 못 받음) */
internal enum class BetaCodesView { LOADING, ALLOWED, NOT_ALLOWED, OFFLINE }

internal fun betaCodesView(loading: Boolean, statusKnown: Boolean, canIssue: Boolean): BetaCodesView = when {
    statusKnown -> if (canIssue) BetaCodesView.ALLOWED else BetaCodesView.NOT_ALLOWED
    loading -> BetaCodesView.LOADING
    else -> BetaCodesView.OFFLINE
}

/**
 * 서버 시각(PostgREST "2026-11-03T07:30:06.12+00:00")을 날짜 글로. Instant.parse 는 안드로이드 13 이하에서 "+00:00" 을
 * 못 읽어 날짜가 비었다 — 시차가 붙은 글은 OffsetDateTime 으로 읽는다. 못 읽으면 null.
 */
internal fun betaCodeDay(iso: String?, zone: ZoneId, format: DateTimeFormatter): String? = iso?.let { text ->
    runCatching { java.time.OffsetDateTime.parse(text).atZoneSameInstant(zone).format(format) }
        .recoverCatching { Instant.parse(text).atZone(zone).format(format) }
        .getOrNull()
}

@Composable
private fun BetaCodeRow(row: AccessCodeRow, context: Context) {
    val c = StepUpUi.colors
    val type = StepUpUi.type
    val open = row.status == "OPEN"
    val day = DateTimeFormatter.ofPattern(stringResource(R.string.beta_codes_date))
    fun fmt(iso: String?) = betaCodeDay(iso, ZoneId.systemDefault(), day)
    val statusText = when (row.status) {
        "USED" -> stringResource(R.string.beta_code_used, fmt(row.usedAt) ?: "")
        "EXPIRED" -> stringResource(R.string.beta_code_expired)
        "REVOKED" -> stringResource(R.string.beta_code_revoked)
        else -> stringResource(R.string.beta_code_open, fmt(row.expiresAt) ?: "")
    }
    // 테스트 판(debug · qa)은 테스트 APK 받기 주소, 스토어 판은 홈페이지 — 스토어 앱이 밖의 APK 를 나누지 않게
    val appLink = if (com.stepup.android.BuildConfig.BUILD_TYPE == "debug" || com.stepup.android.BuildConfig.BUILD_TYPE == "qa") {
        "https://${com.stepup.android.core.InviteLinks.HOST}/downloads/StepUp-MVP.apk"
    } else {
        "https://${com.stepup.android.core.InviteLinks.HOST}"
    }
    val share = stringResource(R.string.beta_code_share_text, row.code, appLink)
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
                // 복사됐다고 알린다(기기마다 시스템 알림이 없기도 하다)
                android.widget.Toast.makeText(context, R.string.beta_code_copied, android.widget.Toast.LENGTH_SHORT).show()
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
