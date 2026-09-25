package com.stepup.android.data.repo

import com.stepup.android.data.local.AppDatabase
import androidx.room.withTransaction
import com.stepup.android.data.local.BoostEntity
import com.stepup.android.data.local.NotificationType
import com.stepup.android.data.local.RewardType
import com.stepup.android.data.prefs.UserPrefs
import com.stepup.android.domain.BoostType
import java.time.LocalDate
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.transformLatest

data class ActiveBoost(val type: BoostType, val expiresAt: Long)

/** 부스트 구매 및 효과 적용 */
class BoostRepository(
    private val database: AppDatabase,
    private val rewardRepository: RewardRepository,
    private val prefs: UserPrefs,
    /** 서버 경제. 있으면 구매는 서버(boost_buy)가 하고 폰은 결과를 다시 받아 온다 */
    private val economy: com.stepup.android.data.remote.EconomyApi? = null,
    private val sync: EconomySync? = null,
) {
    private val boostDao = database.boostDao()

    /**
     * 지속형 활성 부스트. 만료 시각이 지나면 자동으로 목록에서 빠진다.
     * DB 변경이 없어도 다음 만료 시각에 다시 발행한다.
     */
    @OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
    val active: Flow<List<ActiveBoost>> =
        boostDao.observeActive(System.currentTimeMillis()).transformLatest { list ->
            while (true) {
                val now = System.currentTimeMillis()
                val remaining = list.filter { it.expiresAt > now }
                emit(remaining.map { ActiveBoost(BoostType.of(it.type), it.expiresAt) })
                val nextExpiry = remaining.minOfOrNull { it.expiresAt } ?: break
                delay((nextExpiry - System.currentTimeMillis()).coerceAtLeast(1L))
            }
        }

    suspend fun isActive(type: BoostType): Boolean =
        boostDao.activeOf(type.id, System.currentTimeMillis()) != null

    /**
     * 부스트 구매. 잔액이 부족하거나 이미 같은 지속형 부스트가 활성이면 실패한다.
     *
     * @return 실패 사유. null이면 성공.
     */
    suspend fun purchase(type: BoostType): PurchaseError? {
        val api = economy
        if (api != null) {
            // 지속형은 서버가 끝나는 시각 뒤로 이어 붙여 준다. 그래도 켜져 있는데 또 사는 것은 막는다
            if (!type.isInstant && boostDao.activeOf(type.id, System.currentTimeMillis()) != null) {
                return PurchaseError.ALREADY_ACTIVE
            }
            return when (val outcome = api.boostBuy(type.id).toEconomyOutcome()) {
                EconomyOutcome.Ok -> {
                    sync?.refresh()
                    rewardRepository.notify(NotificationType.BOOST_ACTIVATED, type.id, type.cost)
                    null
                }
                EconomyOutcome.NotEnoughBalance -> PurchaseError.NOT_ENOUGH_BALANCE
                EconomyOutcome.EnergyFull -> PurchaseError.ENERGY_CAPACITY
                EconomyOutcome.SignInRequired -> PurchaseError.SIGN_IN_REQUIRED
                EconomyOutcome.Offline -> {
                    // 응답만 잃었을 수 있다 — 잔고를 다시 받아 실제로 샀는지 보이게 한다
                    sync?.refresh()
                    PurchaseError.OFFLINE
                }
                else -> PurchaseError.FAILED
            }
        }
        return if (type.isInstant) purchaseEnergy()
        else database.withTransaction { purchaseInternal(type) }
    }

    private suspend fun purchaseEnergy(): PurchaseError? {
        val (receipts, error) = database.withTransaction {
            val pending = database.energyPurchaseDao().pending()
            if (pending.isNotEmpty()) return@withTransaction pending to null // Retry delivery, never charge again.
            rewardRepository.syncEnergyCap()
            if (!prefs.hasEnergyCapacity(LocalDate.now().toEpochDay(), 2.0)) {
                return@withTransaction emptyList<com.stepup.android.data.local.EnergyPurchase>() to PurchaseError.ENERGY_CAPACITY
            }
            val type = BoostType.ENERGY_CELL
            if (!rewardRepository.spend(RewardType.SPEND_BOOST, type.cost, "부스트 구매: ${type.id}")) {
                return@withTransaction emptyList<com.stepup.android.data.local.EnergyPurchase>() to PurchaseError.NOT_ENOUGH_BALANCE
            }
            val now = System.currentTimeMillis()
            val receipt = com.stepup.android.data.local.EnergyPurchase(java.util.UUID.randomUUID().toString(), now, 2.0)
            database.energyPurchaseDao().insert(receipt)
            boostDao.insert(BoostEntity(type = type.id, activatedAt = now, expiresAt = now))
            listOf(receipt) to null
        }
        if (error != null) return error
        return if (deliverEnergy(receipts)) null else PurchaseError.ENERGY_CAPACITY
    }

    suspend fun recoverEnergyPurchases() = deliverEnergy(database.energyPurchaseDao().pending())

    private suspend fun deliverEnergy(receipts: List<com.stepup.android.data.local.EnergyPurchase>): Boolean {
        for (receipt in receipts) {
            // A refill/level change between debit and delivery must not discard paid energy.
            if (!prefs.restorePurchasedEnergy(receipt.id, LocalDate.now().toEpochDay(), receipt.amount)) return false
            database.withTransaction {
                if (database.energyPurchaseDao().pending().any { it.id == receipt.id }) {
                    rewardRepository.notify(NotificationType.BOOST_ACTIVATED, BoostType.ENERGY_CELL.id, BoostType.ENERGY_CELL.cost)
                    database.energyPurchaseDao().markDelivered(receipt.id)
                }
            }
        }
        return true
    }

    private suspend fun purchaseInternal(type: BoostType): PurchaseError? {
        require(!type.isInstant)
        val now = System.currentTimeMillis()
        if (!type.isInstant && boostDao.activeOf(type.id, now) != null) {
            return PurchaseError.ALREADY_ACTIVE
        }
        if (!rewardRepository.spend(RewardType.SPEND_BOOST, type.cost, "부스트 구매: ${type.id}")) {
            return PurchaseError.NOT_ENOUGH_BALANCE
        }

        boostDao.insert(
            BoostEntity(
                type = type.id,
                activatedAt = now,
                expiresAt = now + type.durationMillis,
            )
        )
        rewardRepository.notify(NotificationType.BOOST_ACTIVATED, type.id, type.cost)
        return null
    }

    /**
     * 끝난 부스트를 지운다. 이틀은 남겨 둔다 — 스트릭 보호막은 끝난 뒤에도
     * "놓친 어제를 덮었는가"를 다음 날 목표 달성 때 확인해야 한다.
     */
    suspend fun purgeExpired() {
        boostDao.purgeExpired(System.currentTimeMillis() - KEEP_EXPIRED_MILLIS)
    }

    private companion object {
        const val KEEP_EXPIRED_MILLIS = 2 * 24 * 60 * 60 * 1000L
    }
}

enum class PurchaseError { NOT_ENOUGH_BALANCE, ALREADY_ACTIVE, ENERGY_CAPACITY, SIGN_IN_REQUIRED, OFFLINE, FAILED }
