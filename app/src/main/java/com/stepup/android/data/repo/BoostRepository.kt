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
import kotlinx.coroutines.flow.map

data class ActiveBoost(val type: BoostType, val expiresAt: Long)

/** 부스트 구매 및 효과 적용 */
class BoostRepository(
    private val database: AppDatabase,
    private val rewardRepository: RewardRepository,
    private val prefs: UserPrefs,
) {
    private val boostDao = database.boostDao()

    /**
     * 지속형 활성 부스트. 만료 시각이 지나면 자동으로 목록에서 빠진다.
     * (조회 시점의 now를 쓰므로 화면이 재구독될 때 갱신된다.)
     */
    val active: Flow<List<ActiveBoost>> =
        boostDao.observeActive(System.currentTimeMillis()).map { list ->
            val now = System.currentTimeMillis()
            list.filter { it.expiresAt > now }
                .map { ActiveBoost(BoostType.of(it.type), it.expiresAt) }
        }

    suspend fun isActive(type: BoostType): Boolean =
        boostDao.activeOf(type.id, System.currentTimeMillis()) != null

    /**
     * 부스트 구매. 잔액이 부족하거나 이미 같은 지속형 부스트가 활성이면 실패한다.
     *
     * @return 실패 사유. null이면 성공.
     */
    suspend fun purchase(type: BoostType): PurchaseError? =
        if (type.isInstant) purchaseEnergy()
        else database.withTransaction { purchaseInternal(type) }

    private suspend fun purchaseEnergy(): PurchaseError? {
        val receipts = database.withTransaction {
            val pending = database.energyPurchaseDao().pending()
            if (pending.isNotEmpty()) return@withTransaction pending // Retry delivery, never charge again.
            val type = BoostType.ENERGY_CELL
            if (!rewardRepository.spend(RewardType.SPEND_BOOST, type.cost, "부스트 구매: ${type.id}")) {
                return@withTransaction null
            }
            val now = System.currentTimeMillis()
            val receipt = com.stepup.android.data.local.EnergyPurchase(java.util.UUID.randomUUID().toString(), now, 2.0)
            database.energyPurchaseDao().insert(receipt)
            boostDao.insert(BoostEntity(type = type.id, activatedAt = now, expiresAt = now))
            listOf(receipt)
        } ?: return PurchaseError.NOT_ENOUGH_BALANCE
        deliverEnergy(receipts)
        return null
    }

    suspend fun recoverEnergyPurchases() = deliverEnergy(database.energyPurchaseDao().pending())

    private suspend fun deliverEnergy(receipts: List<com.stepup.android.data.local.EnergyPurchase>) {
        for (receipt in receipts) {
            prefs.restorePurchasedEnergy(receipt.id, LocalDate.now().toEpochDay(), receipt.amount)
            database.withTransaction {
                if (database.energyPurchaseDao().pending().any { it.id == receipt.id }) {
                    rewardRepository.notify(NotificationType.BOOST_ACTIVATED, BoostType.ENERGY_CELL.id, BoostType.ENERGY_CELL.cost)
                    database.energyPurchaseDao().markDelivered(receipt.id)
                }
            }
        }
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

    suspend fun purgeExpired() {
        boostDao.purgeExpired(System.currentTimeMillis())
    }
}

enum class PurchaseError { NOT_ENOUGH_BALANCE, ALREADY_ACTIVE }
