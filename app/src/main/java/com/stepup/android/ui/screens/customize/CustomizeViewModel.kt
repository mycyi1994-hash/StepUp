package com.stepup.android.ui.screens.customize

import androidx.annotation.StringRes
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.stepup.android.R
import com.stepup.android.core.ServiceLocator
import com.stepup.android.data.repo.AvatarRepository
import com.stepup.android.data.repo.RewardRepository
import com.stepup.android.data.repo.SneakerRepository
import com.stepup.android.domain.AvatarGender
import com.stepup.android.domain.AvatarLook
import com.stepup.android.domain.Outfit
import com.stepup.android.domain.Sneaker
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/**
 * 꾸미기 · 러너 마켓이 함께 쓰는 상태.
 *
 * 입히는 일은 모두 이미 있는 길을 탄다. 신발은 [SneakerRepository.equip]
 * (보관함의 장착과 같은 함수), 의상은 [AvatarRepository.equipOutfit]. 여기서
 * 새로 사고파는 일은 없다.
 */
class CustomizeViewModel(
    private val avatars: AvatarRepository,
    private val sneakers: SneakerRepository,
    rewards: RewardRepository,
) : ViewModel() {

    val look: StateFlow<AvatarLook> = avatars.look
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), AvatarLook())

    val balance: StateFlow<Double> = rewards.balance
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), 0.0)

    /** 가진 신발 — 착용 중인 것이 맨 앞, 그다음 등급 높은 순 */
    val shoes: StateFlow<List<Sneaker>?> = sneakers.inventory
        .map<List<Sneaker>, List<Sneaker>?> { list ->
            list.sortedWith(
                compareByDescending<Sneaker> { it.equipped }
                    .thenByDescending { it.rarity.ordinal }
                    .thenByDescending { it.level },
            )
        }
        // 아직 못 읽었으면 null — "신발이 없어요"를 잠깐이라도 보이지 않는다
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    val demoMode: StateFlow<Boolean> = avatars.demoMode
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), false)

    /** 한 번 보여 주고 사라지는 말 */
    val message = MutableStateFlow<Int?>(null)

    fun ownedOutfits(): List<Outfit> = avatars.owned()

    fun isOwned(outfit: Outfit): Boolean = avatars.isOwned(outfit)

    fun setGender(gender: AvatarGender) {
        viewModelScope.launch { avatars.setGender(gender) }
    }

    fun equipOutfit(outfit: Outfit) {
        viewModelScope.launch {
            message.value = if (avatars.equipOutfit(outfit)) {
                if (avatars.isOwned(outfit)) R.string.customize_equipped else R.string.customize_trial_on
            } else {
                R.string.customize_not_owned
            }
        }
    }

    fun equipShoe(id: Long) {
        viewModelScope.launch {
            sneakers.equip(id)
            message.value = R.string.customize_equipped
        }
    }

    fun say(@StringRes id: Int) {
        message.value = id
    }

    fun consumeMessage() {
        message.value = null
    }

    companion object {
        val Factory = viewModelFactory {
            initializer {
                CustomizeViewModel(
                    ServiceLocator.avatarRepository,
                    ServiceLocator.sneakerRepository,
                    ServiceLocator.rewardRepository,
                )
            }
        }
    }
}
