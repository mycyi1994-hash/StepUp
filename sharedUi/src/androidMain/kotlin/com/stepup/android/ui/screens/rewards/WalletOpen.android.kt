package com.stepup.android.ui.screens.rewards

import android.content.ActivityNotFoundException
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import com.stepup.android.core.AndroidHost
import com.stepup.android.domain.WalletApp

/**
 * 지갑 앱을 연다(예전 WalletConnectSheet.openWallet 그대로) — 앱 안 브라우저로 바로 여는 링크가 있으면 그것,
 * 없으면(Rabby) 연결 페이지 주소를 복사하고 앱을 연다. 앱이 없으면 플레이 스토어로.
 */
actual fun openWallet(app: WalletApp, page: String): WalletPickPhase {
    val context = AndroidHost.app
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
