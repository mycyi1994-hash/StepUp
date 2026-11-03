package com.stepup.android.ui.screens.access

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
import com.stepup.android.data.remote.AccessStatusRow
import com.stepup.android.data.remote.ServerResult
import com.stepup.android.ui.components.ButtonState
import com.stepup.android.ui.components.FormField
import com.stepup.android.ui.components.PrimaryButton
import com.stepup.android.ui.components.StepUpUi
import com.stepup.android.ui.theme.StepUpDesign
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
    redeem: suspend (String) -> ServerResult<com.stepup.android.data.remote.AccessRedeemRow> = { com.stepup.android.core.AppServices.accessApi.redeem(it) },
    /** iOS — 로그인 화면 뒤판의 Apple 버튼(누르면 모달을 다시 연다) */
    showApple: Boolean = false,
) {
    // 모달이 열렸는가 — 화면 상태일 뿐, 접속 권한(서버 · AccessGate)과 따로 둔다
    var open by rememberSaveable { mutableStateOf(true) }
    Box(Modifier.fillMaxSize().testTag("access-gate")) {
        LoginContent(phase = LoginPhase.Idle, notice = null, onSignIn = { open = true }, onApple = if (showApple) ({ open = true }) else null)
        if (open) {
            AccessCodeModal(
                status = status, signedIn = signedIn, redeem = redeem, onGranted = onGranted, onSignOut = onSignOut,
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
) {
    val c = StepUpUi.colors
    val type = StepUpUi.type
    // 모달이 닫히면 이 범위가 끝나 확인 중인 요청의 늦은 답도 버려진다
    val scope = rememberCoroutineScope()
    var code by rememberSaveable { mutableStateOf("") }
    var busy by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<org.jetbrains.compose.resources.StringResource?>(null) }
    val canEnter = signedIn && !status.revoked
    Dialog(
        onDismissRequest = { onClose(busy) },
        properties = DialogProperties(
            dismissOnBackPress = true, dismissOnClickOutside = false,
            usePlatformDefaultWidth = false,
        ),
    ) {
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
                        stringResource(if (status.mode == "OPEN_BETA") Res.string.access_title_open_beta else Res.string.access_title_closed_beta),
                        style = type.section, color = c.label, modifier = Modifier.weight(1f).semantics { heading() },
                    )
                    IconButton(onClick = { onClose(busy) }, modifier = Modifier.size(48.dp).testTag("access-close")) {
                        Icon(Icons.Filled.Close, stringResource(Res.string.common_close), tint = c.secondaryLabel)
                    }
                }
                Spacer(Modifier.height(8.dp))
                Text(
                    stringResource(
                        when {
                            !signedIn -> Res.string.access_body_sign_in
                            status.revoked -> Res.string.access_body_revoked
                            status.mode == "OPEN_BETA" -> Res.string.access_body_open_beta
                            else -> Res.string.access_body_closed_beta
                        },
                    ),
                    style = type.body, color = c.secondaryLabel,
                )
                if (canEnter) {
                    Spacer(Modifier.height(20.dp))
                    FormField(
                        label = stringResource(Res.string.access_code_label), value = code,
                        // 확인 중에는 바꾸지 않는다 · 고치면 앞 오류를 지운다
                        onValueChange = { v -> if (!busy) { code = v.take(40); error = null } },
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
                        text = stringResource(Res.string.access_enter),
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
                                    else -> error = Res.string.access_error_network
                                }
                            }
                        },
                        modifier = Modifier.fillMaxWidth().testTag("access-enter"),
                    )
                } else {
                    Spacer(Modifier.height(20.dp))
                }
                Spacer(Modifier.height(4.dp))
                Box(
                    Modifier.fillMaxWidth().heightIn(min = 48.dp).clip(RoundedCornerShape(12.dp))
                        .pressable(enabled = !busy, onClick = onSignOut).testTag("access-sign-out"),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        stringResource(if (signedIn) Res.string.access_other_account else Res.string.access_sign_in),
                        style = type.smallButton, color = c.secondaryLabel,
                    )
                }
            }
        }
    }
}

fun errorText(error: String?): org.jetbrains.compose.resources.StringResource = when (error) {
    "not_found" -> Res.string.access_error_not_found
    "revoked" -> Res.string.access_error_revoked
    "expired" -> Res.string.access_error_expired
    "used_up" -> Res.string.access_error_used_up
    "too_many" -> Res.string.access_error_too_many
    "blocked" -> Res.string.access_body_revoked
    else -> Res.string.access_error_network
}

