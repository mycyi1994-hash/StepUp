package com.stepup.android.data.repo

import com.stepup.android.data.local.BoostDao
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
    private val boostDao: BoostDao,
    private val rewardRepository: RewardRepository,
    private val prefs: UserPrefs,
) {

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
    suspend fun purchase(type: BoostType): PurchaseError? {
        val now = System.currentTimeMillis()
        if (!type.isInstant && boostDao.activeOf(type.id, now) != null) {
            return PurchaseError.ALREADY_ACTIVE
        }
        if (!rewardRepository.spend(RewardType.SPEND_BOOST, type.cost, "부스트 구매: ${type.id}")) {
            return PurchaseError.NOT_ENOUGH_BALANCE
        }

        when (type) {
            BoostType.ENERGY_CELL -> {
                // 즉시형 — 에너지 2칸 회복
                prefs.restoreEnergy(LocalDate.now().toEpochDay(), 2.0, rewardRepository.maxEnergyNow())
                boostDao.insert(BoostEntity(type = type.id, activatedAt = now, expiresAt = now))
            }
            else -> {
                boostDao.insert(
                    BoostEntity(
                        type = type.id,
                        activatedAt = now,
                        expiresAt = now + type.durationMillis,
                    )
                )
            }
        }
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

enum class PurchaseError { NOT_ENOUGH_BALANCE, ALREADY_ACTIVE }
