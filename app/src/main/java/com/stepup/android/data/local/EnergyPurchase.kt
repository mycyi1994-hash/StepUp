package com.stepup.android.data.local

import androidx.room.Dao
import androidx.room.Entity
import androidx.room.Insert
import androidx.room.PrimaryKey
import androidx.room.Query

/** Debit and receipt are committed together; delivery to DataStore can be replayed. */
@Entity(tableName = "energy_purchases")
data class EnergyPurchase(
    @PrimaryKey val id: String,
    val createdAt: Long,
    val amount: Double,
    val delivered: Boolean = false,
)

@Dao
interface EnergyPurchaseDao {
    @Insert suspend fun insert(receipt: EnergyPurchase)
    @Query("SELECT * FROM energy_purchases WHERE delivered = 0 ORDER BY createdAt, id")
    suspend fun pending(): List<EnergyPurchase>
    @Query("UPDATE energy_purchases SET delivered = 1 WHERE id = :id")
    suspend fun markDelivered(id: String)
}
