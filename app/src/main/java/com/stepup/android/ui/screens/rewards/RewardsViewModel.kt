package com.stepup.android.ui.screens.rewards

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.stepup.android.BuildConfig
import com.stepup.android.core.ServiceLocator
import com.stepup.android.core.WalletPage
import com.stepup.android.data.remote.TokenResult
import com.stepup.android.data.local.RewardEntity
import com.stepup.android.data.repo.RewardRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.map

class RewardsViewModel(rewardRepository: RewardRepository) : ViewModel() {

    val totals = rewardRepository.totals
        .map<com.stepup.android.data.local.RewardTotals, com.stepup.android.data.local.RewardTotals?> { it }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    val ledger: StateFlow<List<RewardEntity>?> = rewardRepository.ledger()
        .map<List<RewardEntity>, List<RewardEntity>?> { it }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    init {
        ServiceLocator.refreshEconomyInBackground()
    }

    /** 멤버십 카드의 등급 표기에 사용한다. */
    val sneakerLevel: StateFlow<Int> = rewardRepository.sneakerLevel
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), 1)

    /** 웹 지갑 페이지를 열 수 있는가 — 주소가 설정돼 있을 때만 버튼을 보인다 */
    val walletPageAvailable: Boolean = BuildConfig.WALLET_URL.isNotBlank()

    /** 지금 로그인한 계정으로 웹 지갑 페이지를 여는 주소 */
    suspend fun walletPageLink(): WalletPageLink =
        when (val token = ServiceLocator.sessionHolder.accessToken()) {
            is TokenResult.Ok -> WalletPage.url(BuildConfig.WALLET_URL, token.accessToken)
                ?.let { WalletPageLink.Open(it) } ?: WalletPageLink.Offline
            is TokenResult.SignInRequired -> WalletPageLink.SignIn
            is TokenResult.Unavailable -> WalletPageLink.Offline
        }

    companion object {
        val Factory = viewModelFactory {
            initializer {
                RewardsViewModel(ServiceLocator.rewardRepository)
            }
        }
    }
}

sealed interface WalletPageLink {
    data class Open(val url: String) : WalletPageLink
    data object SignIn : WalletPageLink
    data object Offline : WalletPageLink
}
