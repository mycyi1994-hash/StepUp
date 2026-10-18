package com.stepup.android.ui.screens.rewards

import android.content.ActivityNotFoundException
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.stepup.android.BuildConfig
import com.stepup.android.R
import com.stepup.android.core.ServiceLocator
import com.stepup.android.data.remote.ServerResult
import com.stepup.android.domain.WalletApp
import com.stepup.android.domain.WalletLinkRequest
import com.stepup.android.ui.components.RunButton
import com.stepup.android.ui.components.RunButtonKind
import com.stepup.android.ui.components.RunCard
import com.stepup.android.ui.components.RunSheet
import com.stepup.android.ui.components.RunSheetText
import com.stepup.android.ui.components.runTextStyle
import com.stepup.android.ui.components.runTone
import kotlinx.coroutines.launch

/** 지갑 고르기 창의 단계 */
enum class WalletPickPhase { Pick, Busy, Opened, Copied, Checking, NotYet, Linked, SignIn, Offline, NeedsMfa, Failed }

/**
 * WEB3 지갑 연결(2026-10-18) — GIWA Wallet(맨 위, 준비 중) · MetaMask · OKX Wallet · Rabby Wallet.
 *
 * 지갑을 고르면 서버에서 10분짜리 일회용 확인 번호를 받아 연결 페이지를 그 지갑 앱 안의 브라우저로 연다(서명 한 번).
 * 앱으로 돌아오면 서버에 연결 여부를 다시 물어 붙은 것을 본 뒤에만 "연결했어요"라고 한다.
 * 이미 붙은 지갑을 바꾸는 것(2단계 인증)은 웹 지갑 페이지에서 한다.
 */
@Composable
fun WalletConnectSheet(onDismiss: () -> Unit, onLinked: () -> Unit = {}) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var phase by rememberSaveable { mutableStateOf(WalletPickPhase.Pick) }
    var picked by rememberSaveable { mutableStateOf<WalletApp?>(null) }

    fun check() {
        phase = WalletPickPhase.Checking
        scope.launch {
            phase = when (val status = ServiceLocator.economyApi.drawStatus()) {
                is ServerResult.Ok -> if (status.value.walletLinked) WalletPickPhase.Linked.also { onLinked() } else WalletPickPhase.NotYet
                is ServerResult.SignInRequired -> WalletPickPhase.SignIn
                else -> WalletPickPhase.Offline
            }
        }
    }

    fun start(app: WalletApp) {
        picked = app
        phase = WalletPickPhase.Busy
        scope.launch {
            phase = when (val reply = ServiceLocator.economyApi.walletLinkChallenge()) {
                is ServerResult.Ok -> {
                    val page = WalletLinkRequest.parse(reply.value)?.pageUrl(BuildConfig.WALLET_URL)
                    if (page == null) WalletPickPhase.Failed else openWallet(context, app, page)
                }
                is ServerResult.SignInRequired -> WalletPickPhase.SignIn
                is ServerResult.Rejected -> if ("2단계" in reply.reason) WalletPickPhase.NeedsMfa else WalletPickPhase.Failed
                is ServerResult.Retry -> WalletPickPhase.Offline
            }
        }
    }

    // 지갑 앱에 갔다가(멈춤) 돌아오면(다시 시작) 서버에 묻는다 — 지갑 앱을 열자마자는 묻지 않는다
    var leftApp by rememberSaveable { mutableStateOf(false) }
    val lifecycle = androidx.lifecycle.compose.LocalLifecycleOwner.current.lifecycle
    androidx.compose.runtime.DisposableEffect(lifecycle) {
        val observer = androidx.lifecycle.LifecycleEventObserver { _, event ->
            when (event) {
                androidx.lifecycle.Lifecycle.Event.ON_PAUSE ->
                    if (phase == WalletPickPhase.Opened || phase == WalletPickPhase.Copied) leftApp = true
                androidx.lifecycle.Lifecycle.Event.ON_RESUME -> if (leftApp) {
                    leftApp = false
                    check()
                }
                else -> Unit
            }
        }
        lifecycle.addObserver(observer)
        onDispose { lifecycle.removeObserver(observer) }
    }

    RunSheet(onDismiss = onDismiss, modifier = Modifier.testTag("wallet-connect-sheet")) {
        when (phase) {
            WalletPickPhase.Pick, WalletPickPhase.Busy -> {
                RunSheetText(stringResource(R.string.wc_title), body = stringResource(R.string.wc_body))
                Spacer(Modifier.height(16.dp))
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    WalletApp.entries.forEach { app ->
                        WalletRow(app, busy = phase == WalletPickPhase.Busy && picked == app,
                            enabled = app.ready && phase == WalletPickPhase.Pick, onClick = { start(app) })
                    }
                }
                Spacer(Modifier.height(12.dp))
                Text(stringResource(R.string.wc_note), runTone().label)
            }
            WalletPickPhase.Opened, WalletPickPhase.Copied, WalletPickPhase.Checking, WalletPickPhase.NotYet -> {
                val name = picked?.let { walletName(it) }.orEmpty()
                RunSheetText(
                    stringResource(R.string.wc_waiting_title, name),
                    body = stringResource(
                        when (phase) {
                            WalletPickPhase.Copied -> R.string.wc_copied_body
                            WalletPickPhase.NotYet -> R.string.wc_not_yet_body
                            else -> R.string.wc_waiting_body
                        },
                        name,
                    ),
                )
                Spacer(Modifier.height(16.dp))
                RunButton(stringResource(R.string.wc_check), { check() }, Modifier.fillMaxWidth().testTag("wallet-connect-check"),
                    busy = phase == WalletPickPhase.Checking, enabled = phase != WalletPickPhase.Checking)
                Spacer(Modifier.height(10.dp))
                RunButton(stringResource(R.string.wc_other_wallet), { phase = WalletPickPhase.Pick }, Modifier.fillMaxWidth(),
                    kind = RunButtonKind.Secondary)
            }
            WalletPickPhase.Linked -> {
                RunSheetText(stringResource(R.string.wc_linked_title), body = stringResource(R.string.wc_linked_body))
                Spacer(Modifier.height(16.dp))
                RunButton(stringResource(R.string.wc_done), onDismiss, Modifier.fillMaxWidth().testTag("wallet-connect-done"))
            }
            else -> {
                RunSheetText(
                    stringResource(R.string.wc_error_title),
                    body = stringResource(
                        when (phase) {
                            WalletPickPhase.SignIn -> R.string.wc_error_sign_in
                            WalletPickPhase.Offline -> R.string.wc_error_offline
                            WalletPickPhase.NeedsMfa -> R.string.wc_error_mfa
                            else -> R.string.wc_error_failed
                        },
                    ),
                )
                Spacer(Modifier.height(16.dp))
                RunButton(stringResource(R.string.wc_back), { phase = WalletPickPhase.Pick }, Modifier.fillMaxWidth())
            }
        }
    }
}

@Composable
private fun Text(text: String, color: Color) {
    androidx.compose.material3.Text(text, style = runTextStyle(13.sp, color), modifier = Modifier.fillMaxWidth())
}

@Composable
private fun WalletRow(app: WalletApp, busy: Boolean, enabled: Boolean, onClick: () -> Unit) {
    val t = runTone()
    RunCard(
        onClick = if (enabled) onClick else null,
        padding = androidx.compose.foundation.layout.PaddingValues(horizontal = 14.dp, vertical = 12.dp),
        tag = "wallet-option-${app.key}",
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            // 지갑 표시 — 머리글자 타일(지갑 회사 로고 그림은 앱에 넣지 않았다)
            Box(
                Modifier.size(40.dp).clip(RoundedCornerShape(12.dp)).background(walletTint(app)),
                contentAlignment = Alignment.Center,
            ) {
                androidx.compose.material3.Text(walletInitial(app), style = runTextStyle(18.sp, Color.White, FontWeight.ExtraBold))
            }
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                androidx.compose.material3.Text(walletName(app),
                    style = runTextStyle(16.sp, if (app.ready) t.text else t.label, FontWeight.Bold))
                androidx.compose.material3.Text(
                    stringResource(if (app == WalletApp.RABBY) R.string.wc_sub_paste else R.string.wc_sub_open),
                    style = runTextStyle(12.sp, t.label),
                )
            }
            when {
                !app.ready -> androidx.compose.material3.Text(
                    stringResource(R.string.wc_soon),
                    style = runTextStyle(12.sp, t.label, FontWeight.Bold),
                    modifier = Modifier.clip(RoundedCornerShape(50)).background(t.track).padding(horizontal = 10.dp, vertical = 4.dp)
                        .testTag("wallet-option-${app.key}-soon"),
                )
                busy -> androidx.compose.material3.CircularProgressIndicator(Modifier.size(20.dp), strokeWidth = 2.dp, color = t.cyan)
            }
        }
    }
}

@Composable
private fun walletName(app: WalletApp): String = stringResource(
    when (app) {
        WalletApp.GIWA -> R.string.wc_giwa
        WalletApp.METAMASK -> R.string.wc_metamask
        WalletApp.OKX -> R.string.wc_okx
        WalletApp.RABBY -> R.string.wc_rabby
    },
)

private fun walletInitial(app: WalletApp): String = when (app) {
    WalletApp.GIWA -> "G"
    WalletApp.METAMASK -> "M"
    WalletApp.OKX -> "O"
    WalletApp.RABBY -> "R"
}

private fun walletTint(app: WalletApp): Color = when (app) {
    WalletApp.GIWA -> Color(0xFF4A5A73)
    WalletApp.METAMASK -> Color(0xFFE2761B)
    WalletApp.OKX -> Color(0xFF111111)
    WalletApp.RABBY -> Color(0xFF7084FF)
}

/**
 * 지갑 앱을 연다 — 앱 안 브라우저로 바로 여는 링크가 있으면 그것, 없으면(Rabby) 연결 페이지 주소를 복사하고 앱을 연다.
 * 앱이 없으면 플레이 스토어로.
 */
internal fun openWallet(context: Context, app: WalletApp, page: String): WalletPickPhase {
    fun view(url: String): Boolean = try {
        context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
        true
    } catch (_: ActivityNotFoundException) {
        false
    }
    fun store(): WalletPickPhase {
        app.storeUrl()?.let { view(it) }
        return WalletPickPhase.Pick
    }
    val direct = app.openUrl(page)
    if (direct != null) return if (view(direct)) WalletPickPhase.Opened else store()
    val pkg = app.packageName ?: return WalletPickPhase.Failed
    val launch = context.packageManager.getLaunchIntentForPackage(pkg) ?: return store()
    (context.getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager)
        ?.setPrimaryClip(ClipData.newPlainText("STEPUP", page))
    return try {
        context.startActivity(launch.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
        WalletPickPhase.Copied
    } catch (_: ActivityNotFoundException) {
        store()
    }
}
