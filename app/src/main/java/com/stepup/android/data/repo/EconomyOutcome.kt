package com.stepup.android.data.repo

import com.stepup.android.data.remote.ServerResult

/** 서버 경제 요청(뽑기 · 강화 · 수리 · 착용 · 부스터)의 결말 — 화면이 문구를 고르는 데 쓴다 */
sealed interface EconomyOutcome {
    data object Ok : EconomyOutcome
    data object NotEnoughBalance : EconomyOutcome
    data object MaxLevel : EconomyOutcome
    data object EnergyFull : EconomyOutcome
    data object NoFreeDraws : EconomyOutcome
    data object NothingToRepair : EconomyOutcome
    data object SignInRequired : EconomyOutcome
    data object Offline : EconomyOutcome
    data class Rejected(val reason: String) : EconomyOutcome
}

/**
 * 서버 함수의 거절 사유(0022 · 0023 의 raise 문구)를 결말로 옮긴다.
 * 문구가 바뀌면 [EconomyOutcome.Rejected] 로 떨어질 뿐 틀린 말을 하지는 않는다.
 */
fun ServerResult<*>.toEconomyOutcome(): EconomyOutcome = when (this) {
    is ServerResult.Ok -> EconomyOutcome.Ok
    is ServerResult.SignInRequired -> EconomyOutcome.SignInRequired
    is ServerResult.Retry -> EconomyOutcome.Offline
    is ServerResult.Rejected -> when {
        "SUP가 부족" in reason -> EconomyOutcome.NotEnoughBalance
        "최대 레벨" in reason -> EconomyOutcome.MaxLevel
        "에너지가 이미" in reason -> EconomyOutcome.EnergyFull
        "무료 뽑기가 남아 있지" in reason -> EconomyOutcome.NoFreeDraws
        "고칠 곳이 없" in reason -> EconomyOutcome.NothingToRepair
        else -> EconomyOutcome.Rejected(reason)
    }
}
