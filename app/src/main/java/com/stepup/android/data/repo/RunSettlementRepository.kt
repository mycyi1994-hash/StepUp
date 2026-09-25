package com.stepup.android.data.repo

import androidx.room.withTransaction
import com.stepup.android.data.local.*
import com.stepup.android.data.prefs.UserPrefs
import com.stepup.android.domain.SessionReward
import java.time.LocalDate
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

/** Commits a local run exactly once. Server verification and account partitioning remain separate. */
class RunSettlementRepository(
    private val db: AppDatabase,
    private val prefs: UserPrefs,
    /**
     * 서버 경제(A안). 켜져 있으면 폰은 적립 줄 · 알림 · 에너지 차감을 적지 않는다 — 금액은 서버가
     * 러닝을 확인한 뒤 정하고(record_session), 폰은 그 결과를 받아 온다. 계산 값은 화면의 예상치로만 쓴다.
     */
    private val serverEconomy: Boolean = false,
) {
    private val mutex = Mutex()

    suspend fun settle(session: WalkSessionEntity, calculate: suspend (Long) -> SessionReward): SessionReward = mutex.withLock {
        require(session.id == 0L && session.startedAt > 0 && session.recordingOwner.isNotBlank())
        reconcileEnergy()
        val receipt = db.withTransaction {
            db.runSettlementDao().find(session.recordingOwner, session.startedAt)?.let { return@withTransaction it }
            // 서버 경제의 에너지 하루는 한국 시각이다 — 폰 날짜를 쓰면 해외에서 에너지가 가득 찬 것처럼 보인다
            val energyDay = (if (serverEconomy) LocalDate.now(java.time.ZoneId.of("Asia/Seoul")) else LocalDate.now()).toEpochDay()
            val reward = calculate(energyDay)
            require(reward.rewardedSteps in 0..session.steps && reward.points.isFinite() && reward.points >= 0 &&
                reward.energyUsed.isFinite() && reward.energyUsed >= 0)
            val created = RunSettlement(session.recordingOwner, session.startedAt,
                energyDay, reward.rewardedSteps, reward.points, if (serverEconomy) 0.0 else reward.energyUsed)
            // 서버 경제에서는 적립액을 서버가 확인한 뒤에 적는다(ClaimRepository)
            db.walkSessionDao().insert(session.copy(pointsEarned = if (serverEconomy) 0.0 else reward.points))
            if (reward.points > 0 && !serverEconomy) {
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
