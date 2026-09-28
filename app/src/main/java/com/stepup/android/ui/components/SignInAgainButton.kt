package com.stepup.android.ui.components

import android.widget.Toast
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import com.stepup.android.R
import com.stepup.android.core.ServiceLocator
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.launch

/** Opens the root login flow without discarding local records, equipment or rewards. */
@Composable
fun SignInAgainButton(modifier: Modifier = Modifier.fillMaxWidth()) {
    val scope = rememberCoroutineScope()
    val context = LocalContext.current
    var busy by remember { mutableStateOf(false) }
    GhostButton(
        text = stringResource(R.string.session_sign_in_again),
        enabled = !busy,
        modifier = modifier,
        onClick = {
            busy = true
            scope.launch {
                try {
                    returnToSignIn(context)
                } catch (cancelled: CancellationException) {
                    throw cancelled
                } catch (_: Exception) {
                    Toast.makeText(context, R.string.feed_save_failed, Toast.LENGTH_SHORT).show()
                } finally {
                    busy = false
                }
            }
        },
    )
}

/**
 * 로그인 화면으로 돌아간다 — 이 폰의 기록 · 장비 · 보상 · 알림은 지우지 않는다.
 * 달리는 중이면 먼저 끝내 저장한다 — 로그인 화면으로 바뀌면 러닝을 멈출 곳이 없어 GPS 가 계속 돈다.
 */
suspend fun returnToSignIn(context: android.content.Context) {
    if (com.stepup.android.service.WalkSessionService.state.value.isActive) {
        com.stepup.android.service.WalkSessionService.stop(context)
    }
    // 다시 로그인하러 간다는 이유를 남긴다 — 로그인 화면이 "다시 로그인해 주세요"(시작·로그인·첫 사용 v1 시안 09)로 알리고,
    // 로그인에 성공하면 지운다
    ServiceLocator.userPrefs.setSignInAgain(true)
    ServiceLocator.userPrefs.setLoginMethod("")
}
