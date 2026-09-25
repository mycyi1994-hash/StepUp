package com.stepup.android.ui.screens.items

import com.stepup.android.ui.experience.ExperienceEvents
import com.stepup.android.ui.experience.FeedbackCue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.stepup.android.core.ServiceLocator
import com.stepup.android.data.repo.ActiveBoost
import com.stepup.android.data.repo.BoostRepository
import com.stepup.android.data.repo.PurchaseError
import com.stepup.android.data.repo.RewardRepository
import com.stepup.android.data.repo.SneakerRepository
import com.stepup.android.domain.BoostType
import com.stepup.android.domain.Faction
import com.stepup.android.domain.Sneaker
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.CancellationException

/** 화면에 한 번만 보여줄 메시지 */
sealed interface ItemsMessage {
    data object SaveFailed : ItemsMessage
    data object NotEnoughBalance : ItemsMessage
    data object BoostAlreadyActive : ItemsMessage
    data object EnergyCapacity : ItemsMessage
    data object BoostBought : ItemsMessage
    data object MaxLevel : ItemsMessage
    data class Upgraded(val sneaker: Sneaker) : ItemsMessage
    data class Equipped(val sneaker: Sneaker) : ItemsMessage
}

/** 같은 (속성 × 등급 × 변형) 사본 묶음 — 그리드에 ×N 으로 표시한다 */
data class SneakerGroup(
    val representative: Sneaker,
    val copies: List<Sneaker>,
) {
    val count: Int get() = copies.size
}

class ItemsViewModel(
    private val sneakerRepository: SneakerRepository,
    private val boostRepository: BoostRepository,
    rewardRepository: RewardRepository,
) : ViewModel() {

    val inventory: StateFlow<List<Sneaker>> = sneakerRepository.inventory
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    /** Null is loading; an empty list is an actual empty shoe vault. */
    val selectionInventory: StateFlow<List<Sneaker>?> = sneakerRepository.inventory
        .map<List<Sneaker>, List<Sneaker>?> { it }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    val equipping = MutableStateFlow(false)

    /**
     * 도감 슬롯별 그룹. 대표는 착용 중인 사본, 없으면 최고 레벨 사본.
     * 착용 중인 그룹이 맨 위, 그다음 등급 → 획득 순.
     */
    val groups: StateFlow<List<SneakerGroup>> = sneakerRepository.inventory
        .map { list ->
            list.groupBy { it.slotKey }
                .map { (_, copies) ->
                    val rep = copies.firstOrNull { it.equipped }
                        ?: copies.maxByOrNull { it.level * 1000L + it.mintNumber }!!
                    SneakerGroup(
                        representative = rep,
                        copies = copies.sortedWith(
                            compareByDescending<Sneaker> { it.equipped }
                                .thenByDescending { it.level }
                                .thenBy { it.mintNumber },
                        ),
                    )
                }
                .sortedWith(
                    compareByDescending<SneakerGroup> { it.representative.equipped }
                        .thenByDescending { it.representative.rarity.ordinal }
                        .thenByDescending { it.representative.acquiredAt },
                )
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val equipped: StateFlow<Sneaker?> = sneakerRepository.equipped
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    val balance: StateFlow<Double?> = rewardRepository.balance
        .map<Double, Double?> { it }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    val activeBoosts: StateFlow<List<ActiveBoost>> = boostRepository.active
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val collectionProgress: StateFlow<Pair<Int, Int>> = sneakerRepository.collectionProgress
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), 0 to 0)

    val factionProgress: StateFlow<Map<Faction, Int>> = sneakerRepository.factionProgress
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyMap())

    /** 민팅 성공 시 결과 다이얼로그용 */
    val mintResult = MutableStateFlow<Sneaker?>(null)

    val message = MutableStateFlow<ItemsMessage?>(null)

    fun equip(id: Long) {
        if (equipping.value) return
        equipping.value = true
        viewModelScope.launch {
            try {
                if (!sneakerRepository.equip(id)) {
                    message.value = ItemsMessage.SaveFailed
                    return@launch
                }
                val target = (selectionInventory.value ?: inventory.value).firstOrNull { it.id == id }
                if (target != null) {
                    message.value = ItemsMessage.Equipped(target)
                    ExperienceEvents.emit(FeedbackCue.Equip)
                }
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (_: Exception) {
                message.value = ItemsMessage.SaveFailed
            } finally {
                equipping.value = false
            }
        }
    }

    fun upgrade(id: Long) {
        savePurchase {
            val target = inventory.value.firstOrNull { it.id == id }
            if (target != null && !target.canUpgrade) {
                ExperienceEvents.emit(FeedbackCue.Error)
                message.value = ItemsMessage.MaxLevel
                return@savePurchase
            }
            val result = sneakerRepository.upgrade(id)
            ExperienceEvents.emit(if (result != null) FeedbackCue.Success else FeedbackCue.Error)
            message.value = if (result != null) {
                ItemsMessage.Upgraded(result)
            } else {
                ItemsMessage.NotEnoughBalance
            }
        }
    }

    fun mint() {
        savePurchase {
            val minted = sneakerRepository.mint()
            if (minted == null) {
                ExperienceEvents.emit(FeedbackCue.Error)
                message.value = ItemsMessage.NotEnoughBalance
            } else {
                mintResult.value = minted
                ExperienceEvents.emit(FeedbackCue.Success)
            }
        }
    }

    private fun savePurchase(action: suspend () -> Unit) {
        viewModelScope.launch {
            try {
                action()
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (_: Exception) {
                message.value = ItemsMessage.SaveFailed
            }
        }
    }

    fun buyBoost(type: BoostType) {
        savePurchase {
            message.value = when (boostRepository.purchase(type)) {
                null -> ItemsMessage.BoostBought
                PurchaseError.NOT_ENOUGH_BALANCE -> ItemsMessage.NotEnoughBalance
                PurchaseError.ALREADY_ACTIVE -> ItemsMessage.BoostAlreadyActive
                PurchaseError.ENERGY_CAPACITY -> ItemsMessage.EnergyCapacity
            }
            ExperienceEvents.emit(if (message.value == ItemsMessage.BoostBought) FeedbackCue.Success else FeedbackCue.Error)
        }
    }

    fun consumeMessage() {
        message.value = null
    }

    fun dismissMintResult() {
        mintResult.value = null
    }

    companion object {
        val Factory = viewModelFactory {
            initializer {
                ItemsViewModel(
                    ServiceLocator.sneakerRepository,
                    ServiceLocator.boostRepository,
                    ServiceLocator.rewardRepository,
                )
            }
        }
    }
}
