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
                    ServiceLocator.userPrefs.setLoginMethod("")
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
