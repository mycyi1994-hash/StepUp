package com.stepup.android.`data`.local

import androidx.room.EntityInsertAdapter
import androidx.room.RoomDatabase
import androidx.room.coroutines.createFlow
import androidx.room.util.appendPlaceholders
import androidx.room.util.getColumnIndexOrThrow
import androidx.room.util.getTotalChangedRows
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
public class NotificationDao_Impl(
  __db: RoomDatabase,
) : NotificationDao {
  private val __db: RoomDatabase

  private val __insertAdapterOfNotificationEntity: EntityInsertAdapter<NotificationEntity>
  init {
    this.__db = __db
    this.__insertAdapterOfNotificationEntity = object : EntityInsertAdapter<NotificationEntity>() {
      protected override fun createQuery(): String = "INSERT OR ABORT INTO `notifications` (`id`,`timestamp`,`type`,`argText`,`argAmount`,`argExtra`,`read`,`actioned`) VALUES (nullif(?, 0),?,?,?,?,?,?,?)"

      protected override fun bind(statement: SQLiteStatement, entity: NotificationEntity) {
        statement.bindLong(1, entity.id)
        statement.bindLong(2, entity.timestamp)
        statement.bindText(3, entity.type)
        statement.bindText(4, entity.argText)
        statement.bindDouble(5, entity.argAmount)
        statement.bindText(6, entity.argExtra)
        val _tmp: Int = if (entity.read) 1 else 0
        statement.bindLong(7, _tmp.toLong())
        val _tmp_1: Int = if (entity.actioned) 1 else 0
        statement.bindLong(8, _tmp_1.toLong())
      }
    }
  }

  public override suspend fun insert(entity: NotificationEntity): Unit = performSuspending(__db, false, true) { _connection ->
    __insertAdapterOfNotificationEntity.insert(_connection, entity)
  }

  public override fun observeAll(limit: Int): Flow<List<NotificationEntity>> {
    val _sql: String = "SELECT * FROM notifications ORDER BY timestamp DESC LIMIT ?"
    return createFlow(__db, false, arrayOf("notifications")) { _connection ->
      val _stmt: SQLiteStatement = _connection.prepare(_sql)
      try {
        var _argIndex: Int = 1
        _stmt.bindLong(_argIndex, limit.toLong())
        val _columnIndexOfId: Int = getColumnIndexOrThrow(_stmt, "id")
        val _columnIndexOfTimestamp: Int = getColumnIndexOrThrow(_stmt, "timestamp")
        val _columnIndexOfType: Int = getColumnIndexOrThrow(_stmt, "type")
        val _columnIndexOfArgText: Int = getColumnIndexOrThrow(_stmt, "argText")
        val _columnIndexOfArgAmount: Int = getColumnIndexOrThrow(_stmt, "argAmount")
        val _columnIndexOfArgExtra: Int = getColumnIndexOrThrow(_stmt, "argExtra")
        val _columnIndexOfRead: Int = getColumnIndexOrThrow(_stmt, "read")
        val _columnIndexOfActioned: Int = getColumnIndexOrThrow(_stmt, "actioned")
        val _result: MutableList<NotificationEntity> = mutableListOf()
        while (_stmt.step()) {
          val _item: NotificationEntity
          val _tmpId: Long
          _tmpId = _stmt.getLong(_columnIndexOfId)
          val _tmpTimestamp: Long
          _tmpTimestamp = _stmt.getLong(_columnIndexOfTimestamp)
          val _tmpType: String
          _tmpType = _stmt.getText(_columnIndexOfType)
          val _tmpArgText: String
          _tmpArgText = _stmt.getText(_columnIndexOfArgText)
          val _tmpArgAmount: Double
          _tmpArgAmount = _stmt.getDouble(_columnIndexOfArgAmount)
          val _tmpArgExtra: String
          _tmpArgExtra = _stmt.getText(_columnIndexOfArgExtra)
          val _tmpRead: Boolean
          val _tmp: Int
          _tmp = _stmt.getLong(_columnIndexOfRead).toInt()
          _tmpRead = _tmp != 0
          val _tmpActioned: Boolean
          val _tmp_1: Int
          _tmp_1 = _stmt.getLong(_columnIndexOfActioned).toInt()
          _tmpActioned = _tmp_1 != 0
          _item = NotificationEntity(_tmpId,_tmpTimestamp,_tmpType,_tmpArgText,_tmpArgAmount,_tmpArgExtra,_tmpRead,_tmpActioned)
          _result.add(_item)
        }
        _result
      } finally {
        _stmt.close()
      }
    }
  }

  public override fun observeUnreadCount(): Flow<Int> {
    val _sql: String = "SELECT COUNT(*) FROM notifications WHERE read = 0"
    return createFlow(__db, false, arrayOf("notifications")) { _connection ->
      val _stmt: SQLiteStatement = _connection.prepare(_sql)
      try {
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

  public override fun observeNewestId(): Flow<Long?> {
    val _sql: String = "SELECT MAX(id) FROM notifications"
    return createFlow(__db, false, arrayOf("notifications")) { _connection ->
      val _stmt: SQLiteStatement = _connection.prepare(_sql)
      try {
        val _result: Long?
        if (_stmt.step()) {
          val _tmp: Long?
          if (_stmt.isNull(0)) {
            _tmp = null
          } else {
            _tmp = _stmt.getLong(0)
          }
          _result = _tmp
        } else {
          _result = null
        }
        _result
      } finally {
        _stmt.close()
      }
    }
  }

  public override suspend fun byId(id: Long): NotificationEntity? {
    val _sql: String = "SELECT * FROM notifications WHERE id = ?"
    return performSuspending(__db, true, false) { _connection ->
      val _stmt: SQLiteStatement = _connection.prepare(_sql)
      try {
        var _argIndex: Int = 1
        _stmt.bindLong(_argIndex, id)
        val _columnIndexOfId: Int = getColumnIndexOrThrow(_stmt, "id")
        val _columnIndexOfTimestamp: Int = getColumnIndexOrThrow(_stmt, "timestamp")
        val _columnIndexOfType: Int = getColumnIndexOrThrow(_stmt, "type")
        val _columnIndexOfArgText: Int = getColumnIndexOrThrow(_stmt, "argText")
        val _columnIndexOfArgAmount: Int = getColumnIndexOrThrow(_stmt, "argAmount")
        val _columnIndexOfArgExtra: Int = getColumnIndexOrThrow(_stmt, "argExtra")
        val _columnIndexOfRead: Int = getColumnIndexOrThrow(_stmt, "read")
        val _columnIndexOfActioned: Int = getColumnIndexOrThrow(_stmt, "actioned")
        val _result: NotificationEntity?
        if (_stmt.step()) {
          val _tmpId: Long
          _tmpId = _stmt.getLong(_columnIndexOfId)
          val _tmpTimestamp: Long
          _tmpTimestamp = _stmt.getLong(_columnIndexOfTimestamp)
          val _tmpType: String
          _tmpType = _stmt.getText(_columnIndexOfType)
          val _tmpArgText: String
          _tmpArgText = _stmt.getText(_columnIndexOfArgText)
          val _tmpArgAmount: Double
          _tmpArgAmount = _stmt.getDouble(_columnIndexOfArgAmount)
          val _tmpArgExtra: String
          _tmpArgExtra = _stmt.getText(_columnIndexOfArgExtra)
          val _tmpRead: Boolean
          val _tmp: Int
          _tmp = _stmt.getLong(_columnIndexOfRead).toInt()
          _tmpRead = _tmp != 0
          val _tmpActioned: Boolean
          val _tmp_1: Int
          _tmp_1 = _stmt.getLong(_columnIndexOfActioned).toInt()
          _tmpActioned = _tmp_1 != 0
          _result = NotificationEntity(_tmpId,_tmpTimestamp,_tmpType,_tmpArgText,_tmpArgAmount,_tmpArgExtra,_tmpRead,_tmpActioned)
        } else {
          _result = null
        }
        _result
      } finally {
        _stmt.close()
      }
    }
  }

  public override suspend fun count(): Int {
    val _sql: String = "SELECT COUNT(*) FROM notifications"
    return performSuspending(__db, true, false) { _connection ->
      val _stmt: SQLiteStatement = _connection.prepare(_sql)
      try {
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

  public override suspend fun markAllRead() {
    val _sql: String = "UPDATE notifications SET read = 1"
    return performSuspending(__db, false, true) { _connection ->
      val _stmt: SQLiteStatement = _connection.prepare(_sql)
      try {
        _stmt.step()
      } finally {
        _stmt.close()
      }
    }
  }

  public override suspend fun markRead(id: Long): Int {
    val _sql: String = "UPDATE notifications SET read = 1 WHERE id = ?"
    return performSuspending(__db, false, true) { _connection ->
      val _stmt: SQLiteStatement = _connection.prepare(_sql)
      try {
        var _argIndex: Int = 1
        _stmt.bindLong(_argIndex, id)
        _stmt.step()
        getTotalChangedRows(_connection)
      } finally {
        _stmt.close()
      }
    }
  }

  public override suspend fun markReadUpTo(upToId: Long): Int {
    val _sql: String = "UPDATE notifications SET read = 1 WHERE read = 0 AND id <= ?"
    return performSuspending(__db, false, true) { _connection ->
      val _stmt: SQLiteStatement = _connection.prepare(_sql)
      try {
        var _argIndex: Int = 1
        _stmt.bindLong(_argIndex, upToId)
        _stmt.step()
        getTotalChangedRows(_connection)
      } finally {
        _stmt.close()
      }
    }
  }

  public override suspend fun markActioned(id: Long) {
    val _sql: String = "UPDATE notifications SET actioned = 1, read = 1 WHERE id = ?"
    return performSuspending(__db, false, true) { _connection ->
      val _stmt: SQLiteStatement = _connection.prepare(_sql)
      try {
        var _argIndex: Int = 1
        _stmt.bindLong(_argIndex, id)
        _stmt.step()
      } finally {
        _stmt.close()
      }
    }
  }

  public override suspend fun delete(id: Long) {
    val _sql: String = "DELETE FROM notifications WHERE id = ?"
    return performSuspending(__db, false, true) { _connection ->
      val _stmt: SQLiteStatement = _connection.prepare(_sql)
      try {
        var _argIndex: Int = 1
        _stmt.bindLong(_argIndex, id)
        _stmt.step()
      } finally {
        _stmt.close()
      }
    }
  }

  public override suspend fun clear() {
    val _sql: String = "DELETE FROM notifications"
    return performSuspending(__db, false, true) { _connection ->
      val _stmt: SQLiteStatement = _connection.prepare(_sql)
      try {
        _stmt.step()
      } finally {
        _stmt.close()
      }
    }
  }

  public override suspend fun deleteInvitesTo(crewIds: List<String>) {
    val _stringBuilder: StringBuilder = StringBuilder()
    _stringBuilder.append("DELETE FROM notifications WHERE type IN ('CREW_INVITE', 'PARTY_INVITE') AND argExtra IN (")
    val _inputSize: Int = crewIds.size
    appendPlaceholders(_stringBuilder, _inputSize)
    _stringBuilder.append(")")
    val _sql: String = _stringBuilder.toString()
    return performSuspending(__db, false, true) { _connection ->
      val _stmt: SQLiteStatement = _connection.prepare(_sql)
      try {
        var _argIndex: Int = 1
        for (_item: String in crewIds) {
          _stmt.bindText(_argIndex, _item)
          _argIndex++
        }
        _stmt.step()
      } finally {
        _stmt.close()
      }
    }
  }

  public override suspend fun deleteByType(type: String) {
    val _sql: String = "DELETE FROM notifications WHERE type = ?"
    return performSuspending(__db, false, true) { _connection ->
      val _stmt: SQLiteStatement = _connection.prepare(_sql)
      try {
        var _argIndex: Int = 1
        _stmt.bindText(_argIndex, type)
        _stmt.step()
      } finally {
        _stmt.close()
      }
    }
  }

  public override suspend fun clearExceptPendingActions() {
    val _sql: String = """
        |
        |        DELETE FROM notifications
        |        WHERE actioned = 1
        |           OR type NOT IN ('CREW_INVITE', 'PARTY_INVITE', 'EVENT_REWARD')
        |        
        """.trimMargin()
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
