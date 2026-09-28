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
import com.stepup.android.data.repo.EconomyOutcome
import com.stepup.android.domain.BoostType
import com.stepup.android.domain.Faction
import com.stepup.android.domain.Sneaker
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeoutOrNull
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
    data object SignInRequired : ItemsMessage
    data object Offline : ItemsMessage
    data object UpgradeLegacy : ItemsMessage
    data object UpgradeListed : ItemsMessage
}

/** 서버 경제의 결말 → 화면 문구. 성공은 부르는 쪽이 따로 정한다. */
private fun com.stepup.android.data.repo.EconomyOutcome.toMessage(): ItemsMessage = when (this) {
    com.stepup.android.data.repo.EconomyOutcome.NotEnoughBalance -> ItemsMessage.NotEnoughBalance
    com.stepup.android.data.repo.EconomyOutcome.MaxLevel -> ItemsMessage.MaxLevel
    com.stepup.android.data.repo.EconomyOutcome.EnergyFull -> ItemsMessage.EnergyCapacity
    com.stepup.android.data.repo.EconomyOutcome.NothingToRepair -> ItemsMessage.NothingToRepair
    com.stepup.android.data.repo.EconomyOutcome.SignInRequired -> ItemsMessage.SignInRequired
    com.stepup.android.data.repo.EconomyOutcome.Offline -> ItemsMessage.Offline
    // 뽑기 결말은 뽑기 화면(DrawViewModel)이 따로 옮긴다 — 여기서는 나오지 않는다
    com.stepup.android.data.repo.EconomyOutcome.NoFreeDraws,
    com.stepup.android.data.repo.EconomyOutcome.NoPremiumDraws,
    com.stepup.android.data.repo.EconomyOutcome.WalletRequired,
    com.stepup.android.data.repo.EconomyOutcome.MintLimitReached,
    com.stepup.android.data.repo.EconomyOutcome.ChainPaused,
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

    /** 다시 불러오기(시안 14) — 목록 구독을 새로 시작한다 */
    private val ownedReloads = MutableStateFlow(0)

    /**
     * 보유 신발 읽기 — 읽는 중 · 읽지 못함 · 목록(보유 신발 상세 v1). 저장소 읽기가 실패하면 빈 목록이 아니라 [OwnedLoad.Failed].
     * 신발 탭과 상세가 이것 하나로 13 조회 중 · 14 조회 실패 · 18 빈 목록을 가른다.
     */
    @OptIn(ExperimentalCoroutinesApi::class)
    val owned: StateFlow<OwnedLoad> = ownedReloads
        .flatMapLatest {
            sneakerRepository.inventory
                .map<List<Sneaker>, OwnedLoad> { OwnedLoad.Ready(it) }
                .onStart { emit(OwnedLoad.Loading) }
                .catch { error ->
                    if (error is CancellationException) throw error
                    emit(OwnedLoad.Failed)
                }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), OwnedLoad.Loading)

    /** 14 "다시 불러오기" — 폰의 목록을 다시 읽고, 서버 경제면 서버 값도 한 번 맞춘다(적립 · 지급은 하지 않는다) */
    fun reloadOwned() {
        ownedReloads.value += 1
        ServiceLocator.refreshEconomyInBackground()
    }

    val equipping = MutableStateFlow(false)

    /** 착용 변경의 결말 — 상세가 한 번만 보여 주고 [consumeEquipResult] 로 지운다(다시 들어와도 되풀이하지 않는다) */
    val equipResult = MutableStateFlow<EquipResult?>(null)

    fun consumeEquipResult() {
        equipResult.value = null
    }

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

    val message = MutableStateFlow<ItemsMessage?>(null)

    /**
     * 이 켤레를 신는다 — 기존 길 그대로([SneakerRepository.equipOnServer]: 서버 경제면 서버 sneaker_equip, 아니면
     * [com.stepup.android.data.local.SneakerDao.equipExclusively] 한 번의 UPDATE). 누르는 동안 [equipping] 이라 두 번 보내지 않는다.
     *
     * 성공 신호만 믿지 않는다 — 화면이 보는 목록([owned])에서 실제 착용이 이 켤레로 바뀐 뒤에야 [EquipResult.Worn].
     * 보내기 전에 최신 목록에서 아직 내 신발인지 다시 본다(다시 신기 포함) — 없는 켤레로 기존 착용을 건드리지 않는다.
     */
    fun equip(id: Long) {
        if (equipping.value) return
        equipping.value = true
        equipResult.value = null
        viewModelScope.launch {
            val result = try {
                wear(id)
            } catch (cancelled: CancellationException) {
                equipping.value = false
                throw cancelled
            } catch (_: Exception) {
                EquipResult.NotWorn(id, EquipFailure.UNCONFIRMED, keptId = null, confirmed = false)
            }
            // 결과를 모르면 서버 값을 한 번 더 맞춰 둔다 — 화면은 저장된 값이 바뀌는 대로 따라간다
            if (result is EquipResult.NotWorn && !result.confirmed) ServiceLocator.refreshEconomyInBackground()
            ExperienceEvents.emit(if (result is EquipResult.Worn) FeedbackCue.Equip else FeedbackCue.Error)
            equipResult.value = result
            equipping.value = false
        }
    }

    private suspend fun wear(id: Long): EquipResult {
        val latest = sneakerRepository.inventory.first()
        if (latest.none { it.id == id }) {
            return EquipResult.NotWorn(id, EquipFailure.MISSING, latest.firstOrNull { it.equipped }?.id, confirmed = true)
        }
        val outcome = sneakerRepository.equipOnServer(id)
        if (outcome == EconomyOutcome.Ok || outcome == EconomyOutcome.Offline) {
            // 저장이 끝났으면 목록이 곧 따라온다. 오래 기다리지 않는다 — 안 오면 아래에서 "확인하지 못했어요"
            withTimeoutOrNull(STORE_WAIT_MS) {
                owned.first { load -> (load as? OwnedLoad.Ready)?.shoes?.any { it.id == id && it.equipped } == true }
            }
        }
        return classifyEquip(id, outcome, sneakerRepository.equipped.first()?.id)
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

    companion object {
        /** 착용 저장 뒤 화면 목록이 따라오기를 기다리는 한도 */
        private const val STORE_WAIT_MS = 3_000L

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
