package com.stepup.android.ui.screens.rewards

import com.stepup.android.core.ExternalIntents
import com.stepup.android.domain.WalletApp

/**
 * iOS — 지갑 앱 안 브라우저로 바로 여는 링크(유니버설 링크)만 연다. 링크가 없는 지갑(Rabby)은 iOS 에서 다른 앱을 이름으로 열 수 없어
 * 고르지 못한다. iOS 의 지갑 주소 연결 여부는 사용자 결정 전이다(docs/APP-STORE.md).
 */
internal actual fun openWallet(app: WalletApp, page: String): WalletPickPhase {
    val direct = app.openUrl(page) ?: return WalletPickPhase.Failed
    return if (ExternalIntents.tryOpenUrl(direct)) WalletPickPhase.Opened else WalletPickPhase.Failed
}
