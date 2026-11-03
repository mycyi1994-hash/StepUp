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

    /** 로그아웃 · 다른 계정 — 앞 계정의 권한을 들고 있지 않게 */
    fun forget() {
        _state.value = _state.value?.let { AccessStatusRow(mode = it.mode, granted = it.mode == "OPEN") }
    }
}

val AccessStatusRow.blocked: Boolean get() = mode != "OPEN" && !granted
