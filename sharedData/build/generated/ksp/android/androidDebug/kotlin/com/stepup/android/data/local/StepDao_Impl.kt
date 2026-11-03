package com.stepup.android.`data`.local

import androidx.room.EntityDeleteOrUpdateAdapter
import androidx.room.EntityInsertAdapter
import androidx.room.EntityUpsertAdapter
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
public class StepDao_Impl(
  __db: RoomDatabase,
) : StepDao {
  private val __db: RoomDatabase

  private val __upsertAdapterOfDailyStepsEntity: EntityUpsertAdapter<DailyStepsEntity>
  init {
    this.__db = __db
    this.__upsertAdapterOfDailyStepsEntity = EntityUpsertAdapter<DailyStepsEntity>(object : EntityInsertAdapter<DailyStepsEntity>() {
      protected override fun createQuery(): String = "INSERT INTO `daily_steps` (`epochDay`,`steps`,`goal`,`updatedAt`) VALUES (?,?,?,?)"

      protected override fun bind(statement: SQLiteStatement, entity: DailyStepsEntity) {
        statement.bindLong(1, entity.epochDay)
        statement.bindLong(2, entity.steps.toLong())
        statement.bindLong(3, entity.goal.toLong())
        statement.bindLong(4, entity.updatedAt)
      }
    }, object : EntityDeleteOrUpdateAdapter<DailyStepsEntity>() {
      protected override fun createQuery(): String = "UPDATE `daily_steps` SET `epochDay` = ?,`steps` = ?,`goal` = ?,`updatedAt` = ? WHERE `epochDay` = ?"

      protected override fun bind(statement: SQLiteStatement, entity: DailyStepsEntity) {
        statement.bindLong(1, entity.epochDay)
        statement.bindLong(2, entity.steps.toLong())
        statement.bindLong(3, entity.goal.toLong())
        statement.bindLong(4, entity.updatedAt)
        statement.bindLong(5, entity.epochDay)
      }
    })
  }

  public override suspend fun upsert(day: DailyStepsEntity): Unit = performSuspending(__db, false, true) { _connection ->
    __upsertAdapterOfDailyStepsEntity.upsert(_connection, day)
  }

  public override fun observeSince(fromDay: Long): Flow<List<DailyStepsEntity>> {
    val _sql: String = "SELECT * FROM daily_steps WHERE epochDay >= ? ORDER BY epochDay ASC"
    return createFlow(__db, false, arrayOf("daily_steps")) { _connection ->
      val _stmt: SQLiteStatement = _connection.prepare(_sql)
      try {
        var _argIndex: Int = 1
        _stmt.bindLong(_argIndex, fromDay)
        val _columnIndexOfEpochDay: Int = getColumnIndexOrThrow(_stmt, "epochDay")
        val _columnIndexOfSteps: Int = getColumnIndexOrThrow(_stmt, "steps")
        val _columnIndexOfGoal: Int = getColumnIndexOrThrow(_stmt, "goal")
        val _columnIndexOfUpdatedAt: Int = getColumnIndexOrThrow(_stmt, "updatedAt")
        val _result: MutableList<DailyStepsEntity> = mutableListOf()
        while (_stmt.step()) {
          val _item: DailyStepsEntity
          val _tmpEpochDay: Long
          _tmpEpochDay = _stmt.getLong(_columnIndexOfEpochDay)
          val _tmpSteps: Int
          _tmpSteps = _stmt.getLong(_columnIndexOfSteps).toInt()
          val _tmpGoal: Int
          _tmpGoal = _stmt.getLong(_columnIndexOfGoal).toInt()
          val _tmpUpdatedAt: Long
          _tmpUpdatedAt = _stmt.getLong(_columnIndexOfUpdatedAt)
          _item = DailyStepsEntity(_tmpEpochDay,_tmpSteps,_tmpGoal,_tmpUpdatedAt)
          _result.add(_item)
        }
        _result
      } finally {
        _stmt.close()
      }
    }
  }

  public override suspend fun byDay(day: Long): DailyStepsEntity? {
    val _sql: String = "SELECT * FROM daily_steps WHERE epochDay = ?"
    return performSuspending(__db, true, false) { _connection ->
      val _stmt: SQLiteStatement = _connection.prepare(_sql)
      try {
        var _argIndex: Int = 1
        _stmt.bindLong(_argIndex, day)
        val _columnIndexOfEpochDay: Int = getColumnIndexOrThrow(_stmt, "epochDay")
        val _columnIndexOfSteps: Int = getColumnIndexOrThrow(_stmt, "steps")
        val _columnIndexOfGoal: Int = getColumnIndexOrThrow(_stmt, "goal")
        val _columnIndexOfUpdatedAt: Int = getColumnIndexOrThrow(_stmt, "updatedAt")
        val _result: DailyStepsEntity?
        if (_stmt.step()) {
          val _tmpEpochDay: Long
          _tmpEpochDay = _stmt.getLong(_columnIndexOfEpochDay)
          val _tmpSteps: Int
          _tmpSteps = _stmt.getLong(_columnIndexOfSteps).toInt()
          val _tmpGoal: Int
          _tmpGoal = _stmt.getLong(_columnIndexOfGoal).toInt()
          val _tmpUpdatedAt: Long
          _tmpUpdatedAt = _stmt.getLong(_columnIndexOfUpdatedAt)
          _result = DailyStepsEntity(_tmpEpochDay,_tmpSteps,_tmpGoal,_tmpUpdatedAt)
        } else {
          _result = null
        }
        _result
      } finally {
        _stmt.close()
      }
    }
  }

  public override fun observeTotalSteps(): Flow<Long> {
    val _sql: String = "SELECT COALESCE(SUM(steps), 0) FROM daily_steps"
    return createFlow(__db, false, arrayOf("daily_steps")) { _connection ->
      val _stmt: SQLiteStatement = _connection.prepare(_sql)
      try {
        val _result: Long
        if (_stmt.step()) {
          val _tmp: Long
          _tmp = _stmt.getLong(0)
          _result = _tmp
        } else {
          _result = 0L
        }
        _result
      } finally {
        _stmt.close()
      }
    }
  }

  public override fun observeStepsSince(fromDay: Long): Flow<Long> {
    val _sql: String = "SELECT COALESCE(SUM(steps), 0) FROM daily_steps WHERE epochDay >= ?"
    return createFlow(__db, false, arrayOf("daily_steps")) { _connection ->
      val _stmt: SQLiteStatement = _connection.prepare(_sql)
      try {
        var _argIndex: Int = 1
        _stmt.bindLong(_argIndex, fromDay)
        val _result: Long
        if (_stmt.step()) {
          val _tmp: Long
          _tmp = _stmt.getLong(0)
          _result = _tmp
        } else {
          _result = 0L
        }
        _result
      } finally {
        _stmt.close()
      }
    }
  }

  public companion object {
    public fun getRequiredConverters(): List<KClass<*>> = emptyList()
  }
}
