package com.stepup.android.core

import com.stepup.android.data.remote.AccessStatusRow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * 접속 문의 마지막 상태(안드로이드 core/AccessGate.kt 와 같은 값) — 앱 뿌리가 서버에 물어 [set] 한다. 화면은 [state] 만 본다.
 */
object AccessGate {
    private val _state = MutableStateFlow<AccessStatusRow?>(null)
    val state: StateFlow<AccessStatusRow?> = _state.asStateFlow()

    fun set(row: AccessStatusRow?) {
        if (row != null) _state.value = row
    }

    /** 받은 문 상태를 기기에 남긴다(안드로이드 설정 파일 stepup_access) — 앱이 정한다 */
    var persist: (AccessStatusRow) -> Unit = {}

    /** 다시 묻는다 — 로그인했으면 내 상태, 아니면 문만. 못 받으면 아는 값을 그대로 둔다(안드로이드 AccessGate.refresh 와 같다) */
    suspend fun refresh(): AccessStatusRow? {
        val api = AppServices.accessApi
        if (!api.isConfigured) return _state.value
        val fresh = try {
            if (AppServices.sessionHolder.isSignedIn()) {
                (api.status() as? com.stepup.android.data.remote.ServerResult.Ok)?.value
            } else {
                (api.mode() as? com.stepup.android.data.remote.ServerResult.Ok)?.value?.let { AccessStatusRow(mode = it, granted = it == "OPEN") }
            }
        } catch (cancelled: kotlinx.coroutines.CancellationException) {
            throw cancelled
        } catch (_: Exception) {
            null
        }
        if (fresh != null) {
            _state.value = fresh
            persist(fresh)
        }
        return _state.value
    }

    /** 로그아웃 · 다른 계정 — 앞 계정의 권한을 들고 있지 않게 */
    fun forget() {
        _state.value = _state.value?.let { AccessStatusRow(mode = it.mode, granted = it.mode == "OPEN") }
    }
}

val AccessStatusRow.blocked: Boolean get() = mode != "OPEN" && !granted
