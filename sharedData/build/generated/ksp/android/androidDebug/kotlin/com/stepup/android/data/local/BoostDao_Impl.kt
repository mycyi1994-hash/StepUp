package com.stepup.android.`data`.local

import androidx.room.EntityInsertAdapter
import androidx.room.RoomDatabase
import androidx.room.coroutines.createFlow
import androidx.room.util.getColumnIndexOrThrow
import androidx.room.util.performSuspending
import androidx.sqlite.SQLiteStatement
import javax.`annotation`.processing.Generated
import kotlin.Int
import kotlin.Long
import kotlin.String
import kotlin.Suppress
import kotlin.Unit
import kotlin.collections.List
import kotlin.collections.MutableList
import kotlin.collections.mutableListOf
import kotlin.reflect.KClass
import kotlinx.coroutines.flow.Flow

@Generated(value = ["androidx.room.RoomProcessor"])
@Suppress(names = ["UNCHECKED_CAST", "DEPRECATION", "REDUNDANT_PROJECTION", "REMOVAL"])
public class BoostDao_Impl(
  __db: RoomDatabase,
) : BoostDao {
  private val __db: RoomDatabase

  private val __insertAdapterOfBoostEntity: EntityInsertAdapter<BoostEntity>
  init {
    this.__db = __db
    this.__insertAdapterOfBoostEntity = object : EntityInsertAdapter<BoostEntity>() {
      protected override fun createQuery(): String = "INSERT OR ABORT INTO `boosts` (`id`,`type`,`activatedAt`,`expiresAt`) VALUES (nullif(?, 0),?,?,?)"

      protected override fun bind(statement: SQLiteStatement, entity: BoostEntity) {
        statement.bindLong(1, entity.id)
        statement.bindText(2, entity.type)
        statement.bindLong(3, entity.activatedAt)
        statement.bindLong(4, entity.expiresAt)
      }
    }
  }

  public override suspend fun insertAll(rows: List<BoostEntity>): Unit = performSuspending(__db, false, true) { _connection ->
    __insertAdapterOfBoostEntity.insert(_connection, rows)
  }

  public override suspend fun insert(boost: BoostEntity): Unit = performSuspending(__db, false, true) { _connection ->
    __insertAdapterOfBoostEntity.insert(_connection, boost)
  }

  public override fun observeActive(now: Long): Flow<List<BoostEntity>> {
    val _sql: String = "SELECT * FROM boosts WHERE expiresAt > ? ORDER BY expiresAt ASC"
    return createFlow(__db, false, arrayOf("boosts")) { _connection ->
      val _stmt: SQLiteStatement = _connection.prepare(_sql)
      try {
        var _argIndex: Int = 1
        _stmt.bindLong(_argIndex, now)
        val _columnIndexOfId: Int = getColumnIndexOrThrow(_stmt, "id")
        val _columnIndexOfType: Int = getColumnIndexOrThrow(_stmt, "type")
        val _columnIndexOfActivatedAt: Int = getColumnIndexOrThrow(_stmt, "activatedAt")
        val _columnIndexOfExpiresAt: Int = getColumnIndexOrThrow(_stmt, "expiresAt")
        val _result: MutableList<BoostEntity> = mutableListOf()
        while (_stmt.step()) {
          val _item: BoostEntity
          val _tmpId: Long
          _tmpId = _stmt.getLong(_columnIndexOfId)
          val _tmpType: String
          _tmpType = _stmt.getText(_columnIndexOfType)
          val _tmpActivatedAt: Long
          _tmpActivatedAt = _stmt.getLong(_columnIndexOfActivatedAt)
          val _tmpExpiresAt: Long
          _tmpExpiresAt = _stmt.getLong(_columnIndexOfExpiresAt)
          _item = BoostEntity(_tmpId,_tmpType,_tmpActivatedAt,_tmpExpiresAt)
          _result.add(_item)
        }
        _result
      } finally {
        _stmt.close()
      }
    }
  }

  public override suspend fun activeOf(type: String, now: Long): BoostEntity? {
    val _sql: String = "SELECT * FROM boosts WHERE type = ? AND expiresAt > ? LIMIT 1"
    return performSuspending(__db, true, false) { _connection ->
      val _stmt: SQLiteStatement = _connection.prepare(_sql)
      try {
        var _argIndex: Int = 1
        _stmt.bindText(_argIndex, type)
        _argIndex = 2
        _stmt.bindLong(_argIndex, now)
        val _columnIndexOfId: Int = getColumnIndexOrThrow(_stmt, "id")
        val _columnIndexOfType: Int = getColumnIndexOrThrow(_stmt, "type")
        val _columnIndexOfActivatedAt: Int = getColumnIndexOrThrow(_stmt, "activatedAt")
        val _columnIndexOfExpiresAt: Int = getColumnIndexOrThrow(_stmt, "expiresAt")
        val _result: BoostEntity?
        if (_stmt.step()) {
          val _tmpId: Long
          _tmpId = _stmt.getLong(_columnIndexOfId)
          val _tmpType: String
          _tmpType = _stmt.getText(_columnIndexOfType)
          val _tmpActivatedAt: Long
          _tmpActivatedAt = _stmt.getLong(_columnIndexOfActivatedAt)
          val _tmpExpiresAt: Long
          _tmpExpiresAt = _stmt.getLong(_columnIndexOfExpiresAt)
          _result = BoostEntity(_tmpId,_tmpType,_tmpActivatedAt,_tmpExpiresAt)
        } else {
          _result = null
        }
        _result
      } finally {
        _stmt.close()
      }
    }
  }

  public override suspend fun activeAt(type: String, at: Long): BoostEntity? {
    val _sql: String = "SELECT * FROM boosts WHERE type = ? AND activatedAt <= ? AND expiresAt > ? LIMIT 1"
    return performSuspending(__db, true, false) { _connection ->
      val _stmt: SQLiteStatement = _connection.prepare(_sql)
      try {
        var _argIndex: Int = 1
        _stmt.bindText(_argIndex, type)
        _argIndex = 2
        _stmt.bindLong(_argIndex, at)
        _argIndex = 3
        _stmt.bindLong(_argIndex, at)
        val _columnIndexOfId: Int = getColumnIndexOrThrow(_stmt, "id")
        val _columnIndexOfType: Int = getColumnIndexOrThrow(_stmt, "type")
        val _columnIndexOfActivatedAt: Int = getColumnIndexOrThrow(_stmt, "activatedAt")
        val _columnIndexOfExpiresAt: Int = getColumnIndexOrThrow(_stmt, "expiresAt")
        val _result: BoostEntity?
        if (_stmt.step()) {
          val _tmpId: Long
          _tmpId = _stmt.getLong(_columnIndexOfId)
          val _tmpType: String
          _tmpType = _stmt.getText(_columnIndexOfType)
          val _tmpActivatedAt: Long
          _tmpActivatedAt = _stmt.getLong(_columnIndexOfActivatedAt)
          val _tmpExpiresAt: Long
          _tmpExpiresAt = _stmt.getLong(_columnIndexOfExpiresAt)
          _result = BoostEntity(_tmpId,_tmpType,_tmpActivatedAt,_tmpExpiresAt)
        } else {
          _result = null
        }
        _result
      } finally {
        _stmt.close()
      }
    }
  }

  public override suspend fun activeDuring(
    type: String,
    from: Long,
    to: Long,
  ): BoostEntity? {
    val _sql: String = "SELECT * FROM boosts WHERE type = ? AND activatedAt < ? AND expiresAt > ? LIMIT 1"
    return performSuspending(__db, true, false) { _connection ->
      val _stmt: SQLiteStatement = _connection.prepare(_sql)
      try {
        var _argIndex: Int = 1
        _stmt.bindText(_argIndex, type)
        _argIndex = 2
        _stmt.bindLong(_argIndex, to)
        _argIndex = 3
        _stmt.bindLong(_argIndex, from)
        val _columnIndexOfId: Int = getColumnIndexOrThrow(_stmt, "id")
        val _columnIndexOfType: Int = getColumnIndexOrThrow(_stmt, "type")
        val _columnIndexOfActivatedAt: Int = getColumnIndexOrThrow(_stmt, "activatedAt")
        val _columnIndexOfExpiresAt: Int = getColumnIndexOrThrow(_stmt, "expiresAt")
        val _result: BoostEntity?
        if (_stmt.step()) {
          val _tmpId: Long
          _tmpId = _stmt.getLong(_columnIndexOfId)
          val _tmpType: String
          _tmpType = _stmt.getText(_columnIndexOfType)
          val _tmpActivatedAt: Long
          _tmpActivatedAt = _stmt.getLong(_columnIndexOfActivatedAt)
          val _tmpExpiresAt: Long
          _tmpExpiresAt = _stmt.getLong(_columnIndexOfExpiresAt)
          _result = BoostEntity(_tmpId,_tmpType,_tmpActivatedAt,_tmpExpiresAt)
        } else {
          _result = null
        }
        _result
      } finally {
        _stmt.close()
      }
    }
  }

  public override suspend fun deleteAll() {
    val _sql: String = "DELETE FROM boosts"
    return performSuspending(__db, false, true) { _connection ->
      val _stmt: SQLiteStatement = _connection.prepare(_sql)
      try {
        _stmt.step()
      } finally {
        _stmt.close()
      }
    }
  }

  public override suspend fun purgeExpired(now: Long) {
    val _sql: String = "DELETE FROM boosts WHERE expiresAt <= ?"
    return performSuspending(__db, false, true) { _connection ->
      val _stmt: SQLiteStatement = _connection.prepare(_sql)
      try {
        var _argIndex: Int = 1
        _stmt.bindLong(_argIndex, now)
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
