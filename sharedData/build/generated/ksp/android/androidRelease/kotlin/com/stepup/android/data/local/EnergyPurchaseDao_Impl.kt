package com.stepup.android.`data`.local

import androidx.room.EntityInsertAdapter
import androidx.room.RoomDatabase
import androidx.room.util.getColumnIndexOrThrow
import androidx.room.util.performSuspending
import androidx.sqlite.SQLiteStatement
import javax.`annotation`.processing.Generated
import kotlin.Boolean
import kotlin.Double
import kotlin.Int
import kotlin.Long
import kotlin.String
import kotlin.Suppress
import kotlin.Unit
import kotlin.collections.List
import kotlin.collections.MutableList
import kotlin.collections.mutableListOf
import kotlin.reflect.KClass

@Generated(value = ["androidx.room.RoomProcessor"])
@Suppress(names = ["UNCHECKED_CAST", "DEPRECATION", "REDUNDANT_PROJECTION", "REMOVAL"])
public class EnergyPurchaseDao_Impl(
  __db: RoomDatabase,
) : EnergyPurchaseDao {
  private val __db: RoomDatabase

  private val __insertAdapterOfEnergyPurchase: EntityInsertAdapter<EnergyPurchase>
  init {
    this.__db = __db
    this.__insertAdapterOfEnergyPurchase = object : EntityInsertAdapter<EnergyPurchase>() {
      protected override fun createQuery(): String = "INSERT OR ABORT INTO `energy_purchases` (`id`,`createdAt`,`amount`,`delivered`) VALUES (?,?,?,?)"

      protected override fun bind(statement: SQLiteStatement, entity: EnergyPurchase) {
        statement.bindText(1, entity.id)
        statement.bindLong(2, entity.createdAt)
        statement.bindDouble(3, entity.amount)
        val _tmp: Int = if (entity.delivered) 1 else 0
        statement.bindLong(4, _tmp.toLong())
      }
    }
  }

  public override suspend fun insert(receipt: EnergyPurchase): Unit = performSuspending(__db, false, true) { _connection ->
    __insertAdapterOfEnergyPurchase.insert(_connection, receipt)
  }

  public override suspend fun pending(): List<EnergyPurchase> {
    val _sql: String = "SELECT * FROM energy_purchases WHERE delivered = 0 ORDER BY createdAt, id"
    return performSuspending(__db, true, false) { _connection ->
      val _stmt: SQLiteStatement = _connection.prepare(_sql)
      try {
        val _columnIndexOfId: Int = getColumnIndexOrThrow(_stmt, "id")
        val _columnIndexOfCreatedAt: Int = getColumnIndexOrThrow(_stmt, "createdAt")
        val _columnIndexOfAmount: Int = getColumnIndexOrThrow(_stmt, "amount")
        val _columnIndexOfDelivered: Int = getColumnIndexOrThrow(_stmt, "delivered")
        val _result: MutableList<EnergyPurchase> = mutableListOf()
        while (_stmt.step()) {
          val _item: EnergyPurchase
          val _tmpId: String
          _tmpId = _stmt.getText(_columnIndexOfId)
          val _tmpCreatedAt: Long
          _tmpCreatedAt = _stmt.getLong(_columnIndexOfCreatedAt)
          val _tmpAmount: Double
          _tmpAmount = _stmt.getDouble(_columnIndexOfAmount)
          val _tmpDelivered: Boolean
          val _tmp: Int
          _tmp = _stmt.getLong(_columnIndexOfDelivered).toInt()
          _tmpDelivered = _tmp != 0
          _item = EnergyPurchase(_tmpId,_tmpCreatedAt,_tmpAmount,_tmpDelivered)
          _result.add(_item)
        }
        _result
      } finally {
        _stmt.close()
      }
    }
  }

  public override suspend fun markDelivered(id: String) {
    val _sql: String = "UPDATE energy_purchases SET delivered = 1 WHERE id = ?"
    return performSuspending(__db, false, true) { _connection ->
      val _stmt: SQLiteStatement = _connection.prepare(_sql)
      try {
        var _argIndex: Int = 1
        _stmt.bindText(_argIndex, id)
        _stmt.step()
      } finally {
        _stmt.close()
      }
    }
  }

  public companion object {
    public fun getRequiredConverters(): List<KClass<*>> = emptyList()
  }
}
