package com.stepup.android.ui.screens.rewards

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.stepup.android.core.ServiceLocator
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

    /** 멤버십 카드의 등급 표기에 사용한다. */
    val sneakerLevel: StateFlow<Int> = rewardRepository.sneakerLevel
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), 1)

    companion object {
        val Factory = viewModelFactory {
            initializer {
                RewardsViewModel(ServiceLocator.rewardRepository)
            }
        }
    }
}
