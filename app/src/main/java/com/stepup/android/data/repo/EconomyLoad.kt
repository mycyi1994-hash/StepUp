package com.stepup.android.data.repo

/**
 * 이 폰의 경제 사본(신발 · 원장)을 "확인된 값"으로 보여도 되는가 — 신발 탭 · 지갑이 빈 목록 · 0 SUP 를 확정처럼 보이지 않게.
 *
 * - [READY]: 서버 경제가 아닌 빌드이거나, 이 계정으로 동기화가 한 번 이상 성공했다.
 * - [LOADING]: 아직 첫 동기화를 기다리는 중(맞추는 중이거나 아직 묻지 않았다).
 * - [UNREACHABLE]: 첫 동기화가 끊김 · 로그인 필요로 끝났다 — 다시 시도를 보인다.
 */
enum class EconomyLoad { LOADING, READY, UNREACHABLE }

fun economyLoadOf(serverEconomy: Boolean, syncedFrom: Long, state: EconomySyncState, running: Boolean): EconomyLoad = when {
    !serverEconomy -> EconomyLoad.READY
    syncedFrom > 0 -> EconomyLoad.READY
    // 맞추는 중이면 지난 결과(로그아웃 뒤 SIGNED_OUT 등)보다 기다림이 먼저다
    running -> EconomyLoad.LOADING
    state == EconomySyncState.OFFLINE || state == EconomySyncState.SIGNED_OUT -> EconomyLoad.UNREACHABLE
    else -> EconomyLoad.LOADING
}

/**
 * 비어 보이는 사본을 그대로 보여도 되는가. 비어 있지 않은 사본은 서버에서 받은 것이라 늘 보인다(끊긴 동안에도) —
 * 비어 있을 때만 첫 동기화를 기다린다.
 */
fun emptyCopyLoad(empty: Boolean, load: EconomyLoad): EconomyLoad = if (empty) load else EconomyLoad.READY
