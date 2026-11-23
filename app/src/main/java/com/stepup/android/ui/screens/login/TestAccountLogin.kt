package com.stepup.android.ui.screens.login

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import com.stepup.android.R
import com.stepup.android.ui.components.LockCover
import kotlinx.coroutines.launch

/**
 * 시험 계정으로 들어가기(2026-11-24, 사용자 "구글 로그인 없이 바로 접속되는 계정") — 테스트 · 개발 빌드(`BuildConfig.TEST_TOOLS`)의 로그인 화면에만.
 * 이메일 · 비밀번호는 들어가는 사람이 친다(앱에 넣지 않는다 — 테스트 APK 는 누구나 받는다). 계정 만들기 · 채우기는 서버 0125 머리말.
 * [signIn] 은 들어갔으면 true.
 */
@Composable
internal fun TestAccountLogin(enabled: Boolean, signIn: suspend (email: String, password: String) -> Boolean) {
    var open by rememberSaveable { mutableStateOf(false) }
    TextButton(
        onClick = { open = true },
        enabled = enabled,
        modifier = Modifier.fillMaxWidth().testTag("login-test-account"),
    ) { Text(stringResource(R.string.test_login_link)) }
    if (open) TestAccountDialog(onClose = { open = false }, signIn = signIn)
}

@Composable
private fun TestAccountDialog(onClose: () -> Unit, signIn: suspend (String, String) -> Boolean) {
    if (LockCover.hidesWindows()) return
    var email by rememberSaveable { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var busy by remember { mutableStateOf(false) }
    var failed by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()
    AlertDialog(
        onDismissRequest = { if (!busy) onClose() },
        title = { Text(stringResource(R.string.test_login_title)) },
        text = {
            Column(horizontalAlignment = Alignment.Start) {
                OutlinedTextField(
                    value = email, onValueChange = { email = it; failed = false }, singleLine = true, enabled = !busy,
                    label = { Text(stringResource(R.string.test_login_email)) },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                    modifier = Modifier.fillMaxWidth().testTag("login-test-email"),
                )
                Spacer(Modifier.height(8.dp))
                OutlinedTextField(
                    value = password, onValueChange = { password = it; failed = false }, singleLine = true, enabled = !busy,
                    label = { Text(stringResource(R.string.test_login_password)) },
                    visualTransformation = PasswordVisualTransformation(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                    modifier = Modifier.fillMaxWidth().testTag("login-test-password"),
                )
                if (failed) {
                    Spacer(Modifier.height(8.dp))
                    Text(stringResource(R.string.test_login_failed))
                }
            }
        },
        confirmButton = {
            TextButton(
                enabled = !busy && email.isNotBlank() && password.isNotEmpty(),
                onClick = {
                    busy = true
                    scope.launch {
                        val ok = runCatching { signIn(email, password) }.getOrDefault(false)
                        busy = false
                        if (ok) onClose() else failed = true
                    }
                },
            ) { Text(stringResource(R.string.test_login_go)) }
        },
        dismissButton = { TextButton(enabled = !busy, onClick = onClose) { Text(stringResource(R.string.test_login_cancel)) } },
    )
}
