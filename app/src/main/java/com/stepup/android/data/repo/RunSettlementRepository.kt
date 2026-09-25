package com.stepup.android.data.repo

import androidx.room.withTransaction
import com.stepup.android.data.local.*
import com.stepup.android.data.prefs.UserPrefs
import com.stepup.android.domain.SessionReward
import java.time.LocalDate
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

/** Commits a local run exactly once. Server verification and account partitioning remain separate. */
class RunSettlementRepository(private val db: AppDatabase, private val prefs: UserPrefs) {
    private val mutex = Mutex()

    suspend fun settle(session: WalkSessionEntity, calculate: suspend (Long) -> SessionReward): SessionReward = mutex.withLock {
        require(session.id == 0L && session.startedAt > 0 && session.recordingOwner.isNotBlank())
        reconcileEnergy()
        val receipt = db.withTransaction {
            db.runSettlementDao().find(session.recordingOwner, session.startedAt)?.let { return@withTransaction it }
            val energyDay = LocalDate.now().toEpochDay()
            val reward = calculate(energyDay)
            require(reward.rewardedSteps in 0..session.steps && reward.points.isFinite() && reward.points >= 0 &&
                reward.energyUsed.isFinite() && reward.energyUsed >= 0)
            val created = RunSettlement(session.recordingOwner, session.startedAt,
                energyDay, reward.rewardedSteps, reward.points, reward.energyUsed)
            db.walkSessionDao().insert(session.copy(pointsEarned = reward.points))
            if (reward.points > 0) {
                val party = session.partySize > 1
                db.rewardDao().insert(RewardEntity(timestamp = session.endedAt,
                    type = if (party) RewardType.EARN_PARTY else RewardType.EARN_WALK,
                    amount = reward.points, description = "러닝 세션 적립 (${reward.rewardedSteps}보)"))
                db.notificationDao().insert(NotificationEntity(timestamp = session.endedAt,
                    type = if (party) NotificationType.PARTY_FINISHED else NotificationType.REWARD_EARNED,
                    argText = (if (party) session.partySize else reward.rewardedSteps).toString(),
                    argAmount = reward.points, argExtra = "", read = false, actioned = false))
            }
            db.runSettlementDao().insert(created)
            created
        }
        applyEnergy(receipt)
        receipt.reward()
    }

    suspend fun recoverEnergy() = mutex.withLock { reconcileEnergy() }

    private suspend fun reconcileEnergy() {
        db.runSettlementDao().pendingEnergy().forEach { applyEnergy(it) }
    }

    private suspend fun applyEnergy(receipt: RunSettlement) {
        if (receipt.energyApplied) return
        prefs.consumeRunEnergy(receipt.energyReceiptId, receipt.energyDay, receipt.energyUsed)
        db.runSettlementDao().markEnergyApplied(receipt.recordingOwner, receipt.startedAt)
    }
}
