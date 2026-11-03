package com.stepup.android.ui.components

import com.stepup.android.ui.components.showToast
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import org.jetbrains.compose.resources.stringResource
import com.stepup.ui.res.*
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.launch

/** Opens the root login flow without discarding local records, equipment or rewards. */
@Composable
fun SignInAgainButton(modifier: Modifier = Modifier.fillMaxWidth()) {
    val scope = rememberCoroutineScope()
    var busy by remember { mutableStateOf(false) }
    GhostButton(
        text = stringResource(Res.string.session_sign_in_again),
        enabled = !busy,
        modifier = modifier,
        onClick = {
            busy = true
            scope.launch {
                try {
                    returnToSignIn()
                } catch (cancelled: CancellationException) {
                    throw cancelled
                } catch (_: Exception) {
                    showToast(Res.string.feed_save_failed)
                } finally {
                    busy = false
                }
            }
        },
    )
}

