package com.stepup.android.`data`.local

import androidx.room.EntityInsertAdapter
import androidx.room.RoomDatabase
import androidx.room.coroutines.createFlow
import androidx.room.util.appendPlaceholders
import androidx.room.util.getColumnIndexOrThrow
import androidx.room.util.performInTransactionSuspending
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
import kotlin.text.StringBuilder
import kotlinx.coroutines.flow.Flow

@Generated(value = ["androidx.room.RoomProcessor"])
@Suppress(names = ["UNCHECKED_CAST", "DEPRECATION", "REDUNDANT_PROJECTION", "REMOVAL"])
public class RewardDao_Impl(
  __db: RoomDatabase,
) : RewardDao {
  private val __db: RoomDatabase

  private val __insertAdapterOfRewardEntity: EntityInsertAdapter<RewardEntity>
  init {
    this.__db = __db
    this.__insertAdapterOfRewardEntity = object : EntityInsertAdapter<RewardEntity>() {
      protected override fun createQuery(): String = "INSERT OR ABORT INTO `rewards` (`id`,`timestamp`,`type`,`amount`,`description`) VALUES (nullif(?, 0),?,?,?,?)"

      protected override fun bind(statement: SQLiteStatement, entity: RewardEntity) {
        statement.bindLong(1, entity.id)
        statement.bindLong(2, entity.timestamp)
        statement.bindText(3, entity.type)
        statement.bindDouble(4, entity.amount)
        statement.bindText(5, entity.description)
      }
    }
  }

  public override suspend fun insertAll(rows: List<RewardEntity>): Unit = performSuspending(__db, false, true) { _connection ->
    __insertAdapterOfRewardEntity.insert(_connection, rows)
  }

  public override suspend fun insert(reward: RewardEntity): Unit = performSuspending(__db, false, true) { _connection ->
    __insertAdapterOfRewardEntity.insert(_connection, reward)
  }

  public override suspend fun spendIfEnough(entry: RewardEntity): Boolean = performInTransactionSuspending(__db) {
    super@RewardDao_Impl.spendIfEnough(entry)
  }

  public override fun observeTotals(): Flow<RewardTotals> {
    val _sql: String = "SELECT COALESCE(SUM(amount), 0.0) AS balance, COALESCE(SUM(CASE WHEN type IN ('EARN_WALK', 'BONUS_GOAL', 'EARN_EVENT', 'EARN_PARTY', 'EARN_COURSE', 'EARN_INVITE') AND amount > 0 THEN amount ELSE 0 END), 0.0) AS earned, COALESCE(SUM(CASE WHEN type IN ('SPEND_MINT', 'SPEND_UPGRADE', 'SPEND_BOOST', 'SPEND_DRAW', 'SPEND_REPAIR', 'TRADE_BUY', 'TRADE_FEE') AND amount < 0 THEN -amount ELSE 0 END), 0.0) AS spent FROM rewards"
    return createFlow(__db, false, arrayOf("rewards")) { _connection ->
      val _stmt: SQLiteStatement = _connection.prepare(_sql)
      try {
        val _columnIndexOfBalance: Int = 0
        val _columnIndexOfEarned: Int = 1
        val _columnIndexOfSpent: Int = 2
        val _result: RewardTotals
        if (_stmt.step()) {
          val _tmpBalance: Double
          _tmpBalance = _stmt.getDouble(_columnIndexOfBalance)
          val _tmpEarned: Double
          _tmpEarned = _stmt.getDouble(_columnIndexOfEarned)
          val _tmpSpent: Double
          _tmpSpent = _stmt.getDouble(_columnIndexOfSpent)
          _result = RewardTotals(_tmpBalance,_tmpEarned,_tmpSpent)
        } else {
          error("The query result was empty, but expected a single row to return a NON-NULL object of type 'com.stepup.android.`data`.local.RewardTotals'.")
        }
        _result
      } finally {
        _stmt.close()
      }
    }
  }

  public override fun observeBalance(): Flow<Double> {
    val _sql: String = "SELECT COALESCE(SUM(amount), 0.0) FROM rewards"
    return createFlow(__db, false, arrayOf("rewards")) { _connection ->
      val _stmt: SQLiteStatement = _connection.prepare(_sql)
      try {
        val _result: Double
        if (_stmt.step()) {
          val _tmp: Double
          _tmp = _stmt.getDouble(0)
          _result = _tmp
        } else {
          _result = 0.0
        }
        _result
      } finally {
        _stmt.close()
      }
    }
  }

  public override suspend fun balanceNow(): Double {
    val _sql: String = "SELECT COALESCE(SUM(amount), 0.0) FROM rewards"
    return performSuspending(__db, true, false) { _connection ->
      val _stmt: SQLiteStatement = _connection.prepare(_sql)
      try {
        val _result: Double
        if (_stmt.step()) {
          val _tmp: Double
          _tmp = _stmt.getDouble(0)
          _result = _tmp
        } else {
          _result = 0.0
        }
        _result
      } finally {
        _stmt.close()
      }
    }
  }

  public override fun observeLedger(limit: Int): Flow<List<RewardEntity>> {
    val _sql: String = "SELECT * FROM rewards ORDER BY timestamp DESC, id DESC LIMIT ?"
    return createFlow(__db, false, arrayOf("rewards")) { _connection ->
      val _stmt: SQLiteStatement = _connection.prepare(_sql)
      try {
        var _argIndex: Int = 1
        _stmt.bindLong(_argIndex, limit.toLong())
        val _columnIndexOfId: Int = getColumnIndexOrThrow(_stmt, "id")
        val _columnIndexOfTimestamp: Int = getColumnIndexOrThrow(_stmt, "timestamp")
        val _columnIndexOfType: Int = getColumnIndexOrThrow(_stmt, "type")
        val _columnIndexOfAmount: Int = getColumnIndexOrThrow(_stmt, "amount")
        val _columnIndexOfDescription: Int = getColumnIndexOrThrow(_stmt, "description")
        val _result: MutableList<RewardEntity> = mutableListOf()
        while (_stmt.step()) {
          val _item: RewardEntity
          val _tmpId: Long
          _tmpId = _stmt.getLong(_columnIndexOfId)
          val _tmpTimestamp: Long
          _tmpTimestamp = _stmt.getLong(_columnIndexOfTimestamp)
          val _tmpType: String
          _tmpType = _stmt.getText(_columnIndexOfType)
          val _tmpAmount: Double
          _tmpAmount = _stmt.getDouble(_columnIndexOfAmount)
          val _tmpDescription: String
          _tmpDescription = _stmt.getText(_columnIndexOfDescription)
          _item = RewardEntity(_tmpId,_tmpTimestamp,_tmpType,_tmpAmount,_tmpDescription)
          _result.add(_item)
        }
        _result
      } finally {
        _stmt.close()
      }
    }
  }

  public override fun observeLedgerPage(kind: Int, limit: Int): Flow<List<RewardEntity>> {
    val _sql: String = "SELECT * FROM rewards WHERE (? = 0 OR (? = 1 AND amount > 0) OR (? = 2 AND amount < 0)) ORDER BY timestamp DESC, id DESC LIMIT ?"
    return createFlow(__db, false, arrayOf("rewards")) { _connection ->
      val _stmt: SQLiteStatement = _connection.prepare(_sql)
      try {
        var _argIndex: Int = 1
        _stmt.bindLong(_argIndex, kind.toLong())
        _argIndex = 2
        _stmt.bindLong(_argIndex, kind.toLong())
        _argIndex = 3
        _stmt.bindLong(_argIndex, kind.toLong())
        _argIndex = 4
        _stmt.bindLong(_argIndex, limit.toLong())
        val _columnIndexOfId: Int = getColumnIndexOrThrow(_stmt, "id")
        val _columnIndexOfTimestamp: Int = getColumnIndexOrThrow(_stmt, "timestamp")
        val _columnIndexOfType: Int = getColumnIndexOrThrow(_stmt, "type")
        val _columnIndexOfAmount: Int = getColumnIndexOrThrow(_stmt, "amount")
        val _columnIndexOfDescription: Int = getColumnIndexOrThrow(_stmt, "description")
        val _result: MutableList<RewardEntity> = mutableListOf()
        while (_stmt.step()) {
          val _item: RewardEntity
          val _tmpId: Long
          _tmpId = _stmt.getLong(_columnIndexOfId)
          val _tmpTimestamp: Long
          _tmpTimestamp = _stmt.getLong(_columnIndexOfTimestamp)
          val _tmpType: String
          _tmpType = _stmt.getText(_columnIndexOfType)
          val _tmpAmount: Double
          _tmpAmount = _stmt.getDouble(_columnIndexOfAmount)
          val _tmpDescription: String
          _tmpDescription = _stmt.getText(_columnIndexOfDescription)
          _item = RewardEntity(_tmpId,_tmpTimestamp,_tmpType,_tmpAmount,_tmpDescription)
          _result.add(_item)
        }
        _result
      } finally {
        _stmt.close()
      }
    }
  }

  public override fun observeCountByType(type: String): Flow<Int> {
    val _sql: String = "SELECT COUNT(*) FROM rewards WHERE type = ?"
    return createFlow(__db, false, arrayOf("rewards")) { _connection ->
      val _stmt: SQLiteStatement = _connection.prepare(_sql)
      try {
        var _argIndex: Int = 1
        _stmt.bindText(_argIndex, type)
        val _result: Int
        if (_stmt.step()) {
          val _tmp: Int
          _tmp = _stmt.getLong(0).toInt()
          _result = _tmp
        } else {
          _result = 0
        }
        _result
      } finally {
        _stmt.close()
      }
    }
  }

  public override fun observeByTypes(types: List<String>, limit: Int): Flow<List<RewardEntity>> {
    val _stringBuilder: StringBuilder = StringBuilder()
    _stringBuilder.append("SELECT * FROM rewards WHERE type IN (")
    val _inputSize: Int = types.size
    appendPlaceholders(_stringBuilder, _inputSize)
    _stringBuilder.append(") AND amount > 0 ORDER BY timestamp DESC, id DESC LIMIT ")
    _stringBuilder.append("?")
    val _sql: String = _stringBuilder.toString()
    return createFlow(__db, false, arrayOf("rewards")) { _connection ->
      val _stmt: SQLiteStatement = _connection.prepare(_sql)
      try {
        var _argIndex: Int = 1
        for (_item: String in types) {
          _stmt.bindText(_argIndex, _item)
          _argIndex++
        }
        _argIndex = 1 + _inputSize
        _stmt.bindLong(_argIndex, limit.toLong())
        val _columnIndexOfId: Int = getColumnIndexOrThrow(_stmt, "id")
        val _columnIndexOfTimestamp: Int = getColumnIndexOrThrow(_stmt, "timestamp")
        val _columnIndexOfType: Int = getColumnIndexOrThrow(_stmt, "type")
        val _columnIndexOfAmount: Int = getColumnIndexOrThrow(_stmt, "amount")
        val _columnIndexOfDescription: Int = getColumnIndexOrThrow(_stmt, "description")
        val _result: MutableList<RewardEntity> = mutableListOf()
        while (_stmt.step()) {
          val _item_1: RewardEntity
          val _tmpId: Long
          _tmpId = _stmt.getLong(_columnIndexOfId)
          val _tmpTimestamp: Long
          _tmpTimestamp = _stmt.getLong(_columnIndexOfTimestamp)
          val _tmpType: String
          _tmpType = _stmt.getText(_columnIndexOfType)
          val _tmpAmount: Double
          _tmpAmount = _stmt.getDouble(_columnIndexOfAmount)
          val _tmpDescription: String
          _tmpDescription = _stmt.getText(_columnIndexOfDescription)
          _item_1 = RewardEntity(_tmpId,_tmpTimestamp,_tmpType,_tmpAmount,_tmpDescription)
          _result.add(_item_1)
        }
        _result
      } finally {
        _stmt.close()
      }
    }
  }

  public override fun observeCountByTypes(types: List<String>): Flow<Int> {
    val _stringBuilder: StringBuilder = StringBuilder()
    _stringBuilder.append("SELECT COUNT(*) FROM rewards WHERE type IN (")
    val _inputSize: Int = types.size
    appendPlaceholders(_stringBuilder, _inputSize)
    _stringBuilder.append(")")
    val _sql: String = _stringBuilder.toString()
    return createFlow(__db, false, arrayOf("rewards")) { _connection ->
      val _stmt: SQLiteStatement = _connection.prepare(_sql)
      try {
        var _argIndex: Int = 1
        for (_item: String in types) {
          _stmt.bindText(_argIndex, _item)
          _argIndex++
        }
        val _result: Int
        if (_stmt.step()) {
          val _tmp: Int
          _tmp = _stmt.getLong(0).toInt()
          _result = _tmp
        } else {
          _result = 0
        }
        _result
      } finally {
        _stmt.close()
      }
    }
  }

  public override fun observeTotalsByTypes(types: List<String>): Flow<RewardTypeTotals> {
    val _stringBuilder: StringBuilder = StringBuilder()
    _stringBuilder.append("SELECT COUNT(*) AS count, COALESCE(SUM(amount), 0.0) AS total FROM rewards WHERE type IN (")
    val _inputSize: Int = types.size
    appendPlaceholders(_stringBuilder, _inputSize)
    _stringBuilder.append(") AND amount > 0")
    val _sql: String = _stringBuilder.toString()
    return createFlow(__db, false, arrayOf("rewards")) { _connection ->
      val _stmt: SQLiteStatement = _connection.prepare(_sql)
      try {
        var _argIndex: Int = 1
        for (_item: String in types) {
          _stmt.bindText(_argIndex, _item)
          _argIndex++
        }
        val _columnIndexOfCount: Int = 0
        val _columnIndexOfTotal: Int = 1
        val _result: RewardTypeTotals
        if (_stmt.step()) {
          val _tmpCount: Int
          _tmpCount = _stmt.getLong(_columnIndexOfCount).toInt()
          val _tmpTotal: Double
          _tmpTotal = _stmt.getDouble(_columnIndexOfTotal)
          _result = RewardTypeTotals(_tmpCount,_tmpTotal)
        } else {
          error("The query result was empty, but expected a single row to return a NON-NULL object of type 'com.stepup.android.`data`.local.RewardTypeTotals'.")
        }
        _result
      } finally {
        _stmt.close()
      }
    }
  }

  public override fun observeEarnedTotal(): Flow<Double> {
    val _sql: String = "SELECT COALESCE(SUM(amount), 0.0) FROM rewards WHERE amount > 0 AND type IN ('EARN_WALK', 'BONUS_GOAL', 'EARN_EVENT', 'EARN_PARTY', 'EARN_COURSE', 'EARN_INVITE')"
    return createFlow(__db, false, arrayOf("rewards")) { _connection ->
      val _stmt: SQLiteStatement = _connection.prepare(_sql)
      try {
        val _result: Double
        if (_stmt.step()) {
          val _tmp: Double
          _tmp = _stmt.getDouble(0)
          _result = _tmp
        } else {
          _result = 0.0
        }
        _result
      } finally {
        _stmt.close()
      }
    }
  }

  public override fun observeEarnedSince(fromMillis: Long): Flow<Double> {
    val _sql: String = "SELECT COALESCE(SUM(amount), 0.0) FROM rewards WHERE amount > 0 AND timestamp >= ? AND type IN ('EARN_WALK', 'BONUS_GOAL', 'EARN_EVENT', 'EARN_PARTY', 'EARN_COURSE', 'EARN_INVITE')"
    return createFlow(__db, false, arrayOf("rewards")) { _connection ->
      val _stmt: SQLiteStatement = _connection.prepare(_sql)
      try {
        var _argIndex: Int = 1
        _stmt.bindLong(_argIndex, fromMillis)
        val _result: Double
        if (_stmt.step()) {
          val _tmp: Double
          _tmp = _stmt.getDouble(0)
          _result = _tmp
        } else {
          _result = 0.0
        }
        _result
      } finally {
        _stmt.close()
      }
    }
  }

  public override fun observeSumOfTypeSince(type: String, fromMillis: Long): Flow<Double> {
    val _sql: String = "SELECT COALESCE(SUM(amount), 0.0) FROM rewards WHERE type = ? AND timestamp >= ?"
    return createFlow(__db, false, arrayOf("rewards")) { _connection ->
      val _stmt: SQLiteStatement = _connection.prepare(_sql)
      try {
        var _argIndex: Int = 1
        _stmt.bindText(_argIndex, type)
        _argIndex = 2
        _stmt.bindLong(_argIndex, fromMillis)
        val _result: Double
        if (_stmt.step()) {
          val _tmp: Double
          _tmp = _stmt.getDouble(0)
          _result = _tmp
        } else {
          _result = 0.0
        }
        _result
      } finally {
        _stmt.close()
      }
    }
  }

  public override suspend fun deleteAll() {
    val _sql: String = "DELETE FROM rewards"
    return performSuspending(__db, false, true) { _connection ->
      val _stmt: SQLiteStatement = _connection.prepare(_sql)
      try {
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
