package com.stepup.android.core

import android.content.Context
import com.stepup.android.data.remote.AccessStatusRow
import com.stepup.android.data.remote.ServerResult
import kotlin.coroutines.cancellation.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * 접속 코드 문(서버 0079) — 앱이 지금 막혀 있는가를 한곳에서 안다.
 *
 * 서버가 정본이다. 앱은 막힌 사람에게 코드 입력 화면을 보일 뿐이고, 권한 없는 요청은 서버가 403 으로 막는다.
 * 마지막으로 받은 상태를 폰에 적어 둔다 — 연결 없이 켜도 이미 들어온 사람을 다시 막지 않게.
 * 한 번도 받은 적이 없고 연결도 안 되면 막지 않는다(열린 문에서 연결이 끊긴 사람을 막지 않게 — 막혀 있어도 서버가 막는다).
 */
object AccessGate {
    private const val FILE = "stepup_access"

    private val _state = MutableStateFlow<AccessStatusRow?>(null)

    /** 마지막으로 안 상태 — null 이면 아직 모른다 */
    val state: StateFlow<AccessStatusRow?> = _state.asStateFlow()

    fun load(context: Context) {
        val p = context.getSharedPreferences(FILE, Context.MODE_PRIVATE)
        val mode = p.getString("mode", null) ?: return
        _state.value = AccessStatusRow(mode = mode, granted = p.getBoolean("granted", true))
    }

    /** 다시 묻는다 — 로그인했으면 내 상태, 아니면 문만. 못 받으면 아는 값을 그대로 둔다 */
    suspend fun refresh(context: Context): AccessStatusRow? {
        val api = ServiceLocator.accessApi
        if (!api.isConfigured) return _state.value
        val fresh = try {
            if (ServiceLocator.sessionHolder.isSignedIn()) {
                (api.status() as? ServerResult.Ok)?.value
            } else {
                (api.mode() as? ServerResult.Ok)?.value?.let { AccessStatusRow(mode = it, granted = it == "OPEN") }
            }
        } catch (cancelled: CancellationException) {
            throw cancelled
        } catch (_: Exception) {
            null
        }
        if (fresh != null) {
            _state.value = fresh
            context.getSharedPreferences(FILE, Context.MODE_PRIVATE).edit()
                .putString("mode", fresh.mode).putBoolean("granted", fresh.granted).apply()
        }
        return _state.value
    }

    /** 로그아웃 · 다른 계정 — 앞 계정의 권한을 들고 있지 않게 */
    fun forget(context: Context) {
        _state.value = _state.value?.let { AccessStatusRow(mode = it.mode, granted = it.mode == "OPEN") }
        context.getSharedPreferences(FILE, Context.MODE_PRIVATE).edit().remove("granted").apply()
    }
}

/** 막혀 있는가 — 문이 닫혔고 권한이 없다 */
val AccessStatusRow.blocked: Boolean get() = mode != "OPEN" && !granted
