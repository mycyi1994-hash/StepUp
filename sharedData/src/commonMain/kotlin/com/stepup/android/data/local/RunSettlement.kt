package com.stepup.android.data.local

import androidx.room.Dao
import androidx.room.Entity
import androidx.room.Insert
import androidx.room.Query
import com.stepup.android.domain.SessionReward

/** Local settlement receipt; this is not evidence of server-confirmed SUP. */
@Entity(tableName = "run_settlements", primaryKeys = ["recordingOwner", "startedAt"])
data class RunSettlement(
    val recordingOwner: String,
    val startedAt: Long,
    val energyDay: Long,
    val rewardedSteps: Int,
    val points: Double,
    val energyUsed: Double,
    val energyApplied: Boolean = false,
) {
    fun reward() = SessionReward(rewardedSteps, points, energyUsed)
    val energyReceiptId: String get() = "${recordingOwner.length}:$recordingOwner:$startedAt"
}

@Dao
interface RunSettlementDao {
    @Insert suspend fun insert(receipt: RunSettlement)
    @Query("SELECT * FROM run_settlements WHERE recordingOwner = :owner AND startedAt = :startedAt")
    suspend fun find(owner: String, startedAt: Long): RunSettlement?
    @Query("SELECT * FROM run_settlements WHERE energyApplied = 0 ORDER BY energyDay, startedAt")
    suspend fun pendingEnergy(): List<RunSettlement>
    @Query("UPDATE run_settlements SET energyApplied = 1 WHERE recordingOwner = :owner AND startedAt = :startedAt")
    suspend fun markEnergyApplied(owner: String, startedAt: Long)
}
