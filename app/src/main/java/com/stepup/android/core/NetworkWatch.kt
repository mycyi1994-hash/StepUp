package com.stepup.android.core

import android.content.Context
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.os.Handler
import android.os.Looper

/**
 * 인터넷이 다시 되면 밀린 일을 한 번 다시 한다(2026-10-17 인터넷 끊김 점검) — 잔고 · 신발 맞추기, 푸시 주소 적기, 러닝 올리기,
 * 지도 타일, 크루 달리기 연결. 예전엔 앱을 다시 켜거나 그 화면을 다시 열 때까지 아무도 다시 하지 않아, 연결이 돌아와도
 * 잔고 · 확인 상태가 낡은 채였고 지도는 끊긴 동안 못 받은 칸이 빈 채였다.
 *
 * 실제로 인터넷이 되는 망(VALIDATED)으로 돌아왔을 때만 부른다 — 처음 붙일 때의 지금 상태로는 부르지 않는다(켤 때 이미 한다).
 * 끊긴 채로 켰으면 처음 연결도 "다시 됐다"로 친다. 끊겼다 붙었다를 빠르게 되풀이해도 한 번만 하게 [SETTLE_MS] 동안 모은다.
 */
object NetworkWatch {
    private const val SETTLE_MS = 3_000L

    @Volatile private var started = false

    /** 지금 망이 인터넷이 되는가. 처음 알기 전에는 null — 그때는 "다시 됐다"로 치지 않는다 */
    @Volatile private var online: Boolean? = null

    fun start(context: Context, onBack: () -> Unit) {
        if (started) return
        val manager = context.applicationContext.getSystemService(ConnectivityManager::class.java) ?: return
        // 켤 때 망이 아예 없으면 끊긴 것으로 둔다 — 그 뒤의 첫 연결이 "다시 됐다"가 되게(망이 없으면 처음 알림도 오지 않는다)
        online = runCatching {
            manager.activeNetwork?.let { manager.getNetworkCapabilities(it)?.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED) } ?: false
        }.getOrNull()
        val main = Handler(Looper.getMainLooper())
        val run = Runnable { runCatching(onBack) }
        val callback = object : ConnectivityManager.NetworkCallback() {
            override fun onCapabilitiesChanged(network: Network, capabilities: NetworkCapabilities) {
                val now = capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED)
                val before = online
                online = now
                if (before == false && now) {
                    main.removeCallbacks(run)
                    main.postDelayed(run, SETTLE_MS)
                }
            }

            override fun onLost(network: Network) {
                online = false
                main.removeCallbacks(run)
            }
        }
        started = runCatching { manager.registerDefaultNetworkCallback(callback) }.isSuccess
    }
}
