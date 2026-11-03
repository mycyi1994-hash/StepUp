package com.stepup.android.`data`.local

import androidx.room.EntityInsertAdapter
import androidx.room.RoomDatabase
import androidx.room.coroutines.createFlow
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
import kotlinx.coroutines.flow.Flow

@Generated(value = ["androidx.room.RoomProcessor"])
@Suppress(names = ["UNCHECKED_CAST", "DEPRECATION", "REDUNDANT_PROJECTION", "REMOVAL"])
public class ClaimedEventDao_Impl(
  __db: RoomDatabase,
) : ClaimedEventDao {
  private val __db: RoomDatabase

  private val __insertAdapterOfClaimedEventEntity: EntityInsertAdapter<ClaimedEventEntity>

  private val __insertAdapterOfRewardEntity: EntityInsertAdapter<RewardEntity>

  private val __insertAdapterOfNotificationEntity: EntityInsertAdapter<NotificationEntity>
  init {
    this.__db = __db
    this.__insertAdapterOfClaimedEventEntity = object : EntityInsertAdapter<ClaimedEventEntity>() {
      protected override fun createQuery(): String = "INSERT OR IGNORE INTO `claimed_events` (`eventId`,`claimedAt`,`amount`) VALUES (?,?,?)"

      protected override fun bind(statement: SQLiteStatement, entity: ClaimedEventEntity) {
        statement.bindText(1, entity.eventId)
        statement.bindLong(2, entity.claimedAt)
        statement.bindDouble(3, entity.amount)
      }
    }
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

  public override suspend fun insert(entity: ClaimedEventEntity): Unit = performSuspending(__db, false, true) { _connection ->
    __insertAdapterOfClaimedEventEntity.insert(_connection, entity)
  }

  public override suspend fun insertReward(reward: RewardEntity): Unit = performSuspending(__db, false, true) { _connection ->
    __insertAdapterOfRewardEntity.insert(_connection, reward)
  }

  public override suspend fun insertNotification(notification: NotificationEntity): Unit = performSuspending(__db, false, true) { _connection ->
    __insertAdapterOfNotificationEntity.insert(_connection, notification)
  }

  public override suspend fun recordPaidClaim(claim: ClaimedEventEntity, eventId: String): Boolean = performInTransactionSuspending(__db) {
    super@ClaimedEventDao_Impl.recordPaidClaim(claim, eventId)
  }

  public override fun observeAll(): Flow<List<ClaimedEventEntity>> {
    val _sql: String = "SELECT * FROM claimed_events"
    return createFlow(__db, false, arrayOf("claimed_events")) { _connection ->
      val _stmt: SQLiteStatement = _connection.prepare(_sql)
      try {
        val _columnIndexOfEventId: Int = getColumnIndexOrThrow(_stmt, "eventId")
        val _columnIndexOfClaimedAt: Int = getColumnIndexOrThrow(_stmt, "claimedAt")
        val _columnIndexOfAmount: Int = getColumnIndexOrThrow(_stmt, "amount")
        val _result: MutableList<ClaimedEventEntity> = mutableListOf()
        while (_stmt.step()) {
          val _item: ClaimedEventEntity
          val _tmpEventId: String
          _tmpEventId = _stmt.getText(_columnIndexOfEventId)
          val _tmpClaimedAt: Long
          _tmpClaimedAt = _stmt.getLong(_columnIndexOfClaimedAt)
          val _tmpAmount: Double
          _tmpAmount = _stmt.getDouble(_columnIndexOfAmount)
          _item = ClaimedEventEntity(_tmpEventId,_tmpClaimedAt,_tmpAmount)
          _result.add(_item)
        }
        _result
      } finally {
        _stmt.close()
      }
    }
  }

  public override suspend fun byId(id: String): ClaimedEventEntity? {
    val _sql: String = "SELECT * FROM claimed_events WHERE eventId = ?"
    return performSuspending(__db, true, false) { _connection ->
      val _stmt: SQLiteStatement = _connection.prepare(_sql)
      try {
        var _argIndex: Int = 1
        _stmt.bindText(_argIndex, id)
        val _columnIndexOfEventId: Int = getColumnIndexOrThrow(_stmt, "eventId")
        val _columnIndexOfClaimedAt: Int = getColumnIndexOrThrow(_stmt, "claimedAt")
        val _columnIndexOfAmount: Int = getColumnIndexOrThrow(_stmt, "amount")
        val _result: ClaimedEventEntity?
        if (_stmt.step()) {
          val _tmpEventId: String
          _tmpEventId = _stmt.getText(_columnIndexOfEventId)
          val _tmpClaimedAt: Long
          _tmpClaimedAt = _stmt.getLong(_columnIndexOfClaimedAt)
          val _tmpAmount: Double
          _tmpAmount = _stmt.getDouble(_columnIndexOfAmount)
          _result = ClaimedEventEntity(_tmpEventId,_tmpClaimedAt,_tmpAmount)
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
    val _sql: String = "DELETE FROM claimed_events"
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
