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
    data object Repaired : ItemsMessage
    data object NothingToRepair : ItemsMessage
    data object NoFreeDraws : ItemsMessage
    data object SignInRequired : ItemsMessage
    data object Offline : ItemsMessage
    /** 뽑기는 됐는데 새 신발을 아직 못 받아 왔다 — 실패가 아니다, 다시 누르면 또 뽑힌다 */
    data object DrawnRefreshing : ItemsMessage
    data object UpgradeLegacy : ItemsMessage
    data object UpgradeListed : ItemsMessage
}

/** 서버 경제의 결말 → 화면 문구. 성공은 부르는 쪽이 따로 정한다. */
private fun com.stepup.android.data.repo.EconomyOutcome.toMessage(): ItemsMessage = when (this) {
    com.stepup.android.data.repo.EconomyOutcome.NotEnoughBalance -> ItemsMessage.NotEnoughBalance
    com.stepup.android.data.repo.EconomyOutcome.MaxLevel -> ItemsMessage.MaxLevel
    com.stepup.android.data.repo.EconomyOutcome.EnergyFull -> ItemsMessage.EnergyCapacity
    com.stepup.android.data.repo.EconomyOutcome.NoFreeDraws -> ItemsMessage.NoFreeDraws
    com.stepup.android.data.repo.EconomyOutcome.NothingToRepair -> ItemsMessage.NothingToRepair
    com.stepup.android.data.repo.EconomyOutcome.SignInRequired -> ItemsMessage.SignInRequired
    com.stepup.android.data.repo.EconomyOutcome.Offline -> ItemsMessage.Offline
    com.stepup.android.data.repo.EconomyOutcome.Ok,
    is com.stepup.android.data.repo.EconomyOutcome.Rejected -> ItemsMessage.SaveFailed
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

    init {
        // 화면을 열 때 서버 값으로 한 번 맞춘다 — 다른 폰이나 지갑 페이지에서 바뀐 것까지
        ServiceLocator.refreshEconomyInBackground()
    }

    /** 민팅 성공 시 결과 다이얼로그용 */
    val mintResult = MutableStateFlow<Sneaker?>(null)

    val message = MutableStateFlow<ItemsMessage?>(null)

    fun equip(id: Long) {
        if (equipping.value) return
        equipping.value = true
        viewModelScope.launch {
            try {
                val outcome = sneakerRepository.equipOnServer(id)
                if (outcome != com.stepup.android.data.repo.EconomyOutcome.Ok) {
                    message.value = outcome.toMessage()
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
            val block = target?.upgradeBlock
            if (block != null) {
                ExperienceEvents.emit(FeedbackCue.Error)
                message.value = when (block) {
                    com.stepup.android.domain.UpgradeBlock.MAX_LEVEL -> ItemsMessage.MaxLevel
                    com.stepup.android.domain.UpgradeBlock.LEGACY -> ItemsMessage.UpgradeLegacy
                    com.stepup.android.domain.UpgradeBlock.LISTED -> ItemsMessage.UpgradeListed
                }
                return@savePurchase
            }
            val (outcome, result) = sneakerRepository.upgradeOnServer(id)
            ExperienceEvents.emit(if (result != null) FeedbackCue.Success else FeedbackCue.Error)
            message.value = if (result != null) ItemsMessage.Upgraded(result) else outcome.toMessage()
        }
    }

    /** 수리 — 내구도를 가득 채운다 */
    fun repair(id: Long) {
        savePurchase {
            val outcome = sneakerRepository.repairOnServer(id)
            val ok = outcome == com.stepup.android.data.repo.EconomyOutcome.Ok
            ExperienceEvents.emit(if (ok) FeedbackCue.Success else FeedbackCue.Error)
            message.value = if (ok) ItemsMessage.Repaired else outcome.toMessage()
        }
    }

    /** 뽑기 — 무료가 남았으면 무료로, 아니면 SUP 로. 결과는 서버가 굴린 신발이다 */
    fun mint() {
        savePurchase {
            val (outcome, minted) = sneakerRepository.drawOnServer()
            if (minted == null && outcome == com.stepup.android.data.repo.EconomyOutcome.Ok) {
                ExperienceEvents.emit(FeedbackCue.Success)
                message.value = ItemsMessage.DrawnRefreshing
            } else if (minted == null) {
                ExperienceEvents.emit(FeedbackCue.Error)
                message.value = outcome.toMessage()
            } else {
                mintResult.value = minted
                ExperienceEvents.emit(FeedbackCue.Success)
            }
        }
    }

    /** 남은 무료 뽑기 */
    val freeDrawsLeft: StateFlow<Int> = sneakerRepository.freeDrawsLeft
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), 0)

    private fun savePurchase(onDone: () -> Unit = {}, action: suspend () -> Unit) {
        viewModelScope.launch {
            try {
                action()
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (_: Exception) {
                message.value = ItemsMessage.SaveFailed
            } finally {
                onDone()
            }
        }
    }

    /** 구매 요청이 서버에 가 있는 동안 — 두 번 눌러 두 번 사지 않게 */
    private var buyingBoost = false

    fun buyBoost(type: BoostType) {
        if (buyingBoost) return
        buyingBoost = true
        savePurchase(onDone = { buyingBoost = false }) {
            message.value = when (boostRepository.purchase(type)) {
                null -> ItemsMessage.BoostBought
                PurchaseError.NOT_ENOUGH_BALANCE -> ItemsMessage.NotEnoughBalance
                PurchaseError.ALREADY_ACTIVE -> ItemsMessage.BoostAlreadyActive
                PurchaseError.ENERGY_CAPACITY -> ItemsMessage.EnergyCapacity
                PurchaseError.SIGN_IN_REQUIRED -> ItemsMessage.SignInRequired
                PurchaseError.OFFLINE -> ItemsMessage.Offline
                PurchaseError.FAILED -> ItemsMessage.SaveFailed
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
