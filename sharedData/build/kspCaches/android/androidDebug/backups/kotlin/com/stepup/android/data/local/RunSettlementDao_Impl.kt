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
public class RunSettlementDao_Impl(
  __db: RoomDatabase,
) : RunSettlementDao {
  private val __db: RoomDatabase

  private val __insertAdapterOfRunSettlement: EntityInsertAdapter<RunSettlement>
  init {
    this.__db = __db
    this.__insertAdapterOfRunSettlement = object : EntityInsertAdapter<RunSettlement>() {
      protected override fun createQuery(): String = "INSERT OR ABORT INTO `run_settlements` (`recordingOwner`,`startedAt`,`energyDay`,`rewardedSteps`,`points`,`energyUsed`,`energyApplied`) VALUES (?,?,?,?,?,?,?)"

      protected override fun bind(statement: SQLiteStatement, entity: RunSettlement) {
        statement.bindText(1, entity.recordingOwner)
        statement.bindLong(2, entity.startedAt)
        statement.bindLong(3, entity.energyDay)
        statement.bindLong(4, entity.rewardedSteps.toLong())
        statement.bindDouble(5, entity.points)
        statement.bindDouble(6, entity.energyUsed)
        val _tmp: Int = if (entity.energyApplied) 1 else 0
        statement.bindLong(7, _tmp.toLong())
      }
    }
  }

  public override suspend fun insert(receipt: RunSettlement): Unit = performSuspending(__db, false, true) { _connection ->
    __insertAdapterOfRunSettlement.insert(_connection, receipt)
  }

  public override suspend fun find(owner: String, startedAt: Long): RunSettlement? {
    val _sql: String = "SELECT * FROM run_settlements WHERE recordingOwner = ? AND startedAt = ?"
    return performSuspending(__db, true, false) { _connection ->
      val _stmt: SQLiteStatement = _connection.prepare(_sql)
      try {
        var _argIndex: Int = 1
        _stmt.bindText(_argIndex, owner)
        _argIndex = 2
        _stmt.bindLong(_argIndex, startedAt)
        val _columnIndexOfRecordingOwner: Int = getColumnIndexOrThrow(_stmt, "recordingOwner")
        val _columnIndexOfStartedAt: Int = getColumnIndexOrThrow(_stmt, "startedAt")
        val _columnIndexOfEnergyDay: Int = getColumnIndexOrThrow(_stmt, "energyDay")
        val _columnIndexOfRewardedSteps: Int = getColumnIndexOrThrow(_stmt, "rewardedSteps")
        val _columnIndexOfPoints: Int = getColumnIndexOrThrow(_stmt, "points")
        val _columnIndexOfEnergyUsed: Int = getColumnIndexOrThrow(_stmt, "energyUsed")
        val _columnIndexOfEnergyApplied: Int = getColumnIndexOrThrow(_stmt, "energyApplied")
        val _result: RunSettlement?
        if (_stmt.step()) {
          val _tmpRecordingOwner: String
          _tmpRecordingOwner = _stmt.getText(_columnIndexOfRecordingOwner)
          val _tmpStartedAt: Long
          _tmpStartedAt = _stmt.getLong(_columnIndexOfStartedAt)
          val _tmpEnergyDay: Long
          _tmpEnergyDay = _stmt.getLong(_columnIndexOfEnergyDay)
          val _tmpRewardedSteps: Int
          _tmpRewardedSteps = _stmt.getLong(_columnIndexOfRewardedSteps).toInt()
          val _tmpPoints: Double
          _tmpPoints = _stmt.getDouble(_columnIndexOfPoints)
          val _tmpEnergyUsed: Double
          _tmpEnergyUsed = _stmt.getDouble(_columnIndexOfEnergyUsed)
          val _tmpEnergyApplied: Boolean
          val _tmp: Int
          _tmp = _stmt.getLong(_columnIndexOfEnergyApplied).toInt()
          _tmpEnergyApplied = _tmp != 0
          _result = RunSettlement(_tmpRecordingOwner,_tmpStartedAt,_tmpEnergyDay,_tmpRewardedSteps,_tmpPoints,_tmpEnergyUsed,_tmpEnergyApplied)
        } else {
          _result = null
        }
        _result
      } finally {
        _stmt.close()
      }
    }
  }

  public override suspend fun pendingEnergy(): List<RunSettlement> {
    val _sql: String = "SELECT * FROM run_settlements WHERE energyApplied = 0 ORDER BY energyDay, startedAt"
    return performSuspending(__db, true, false) { _connection ->
      val _stmt: SQLiteStatement = _connection.prepare(_sql)
      try {
        val _columnIndexOfRecordingOwner: Int = getColumnIndexOrThrow(_stmt, "recordingOwner")
        val _columnIndexOfStartedAt: Int = getColumnIndexOrThrow(_stmt, "startedAt")
        val _columnIndexOfEnergyDay: Int = getColumnIndexOrThrow(_stmt, "energyDay")
        val _columnIndexOfRewardedSteps: Int = getColumnIndexOrThrow(_stmt, "rewardedSteps")
        val _columnIndexOfPoints: Int = getColumnIndexOrThrow(_stmt, "points")
        val _columnIndexOfEnergyUsed: Int = getColumnIndexOrThrow(_stmt, "energyUsed")
        val _columnIndexOfEnergyApplied: Int = getColumnIndexOrThrow(_stmt, "energyApplied")
        val _result: MutableList<RunSettlement> = mutableListOf()
        while (_stmt.step()) {
          val _item: RunSettlement
          val _tmpRecordingOwner: String
          _tmpRecordingOwner = _stmt.getText(_columnIndexOfRecordingOwner)
          val _tmpStartedAt: Long
          _tmpStartedAt = _stmt.getLong(_columnIndexOfStartedAt)
          val _tmpEnergyDay: Long
          _tmpEnergyDay = _stmt.getLong(_columnIndexOfEnergyDay)
          val _tmpRewardedSteps: Int
          _tmpRewardedSteps = _stmt.getLong(_columnIndexOfRewardedSteps).toInt()
          val _tmpPoints: Double
          _tmpPoints = _stmt.getDouble(_columnIndexOfPoints)
          val _tmpEnergyUsed: Double
          _tmpEnergyUsed = _stmt.getDouble(_columnIndexOfEnergyUsed)
          val _tmpEnergyApplied: Boolean
          val _tmp: Int
          _tmp = _stmt.getLong(_columnIndexOfEnergyApplied).toInt()
          _tmpEnergyApplied = _tmp != 0
          _item = RunSettlement(_tmpRecordingOwner,_tmpStartedAt,_tmpEnergyDay,_tmpRewardedSteps,_tmpPoints,_tmpEnergyUsed,_tmpEnergyApplied)
          _result.add(_item)
        }
        _result
      } finally {
        _stmt.close()
      }
    }
  }

  public override suspend fun markEnergyApplied(owner: String, startedAt: Long) {
    val _sql: String = "UPDATE run_settlements SET energyApplied = 1 WHERE recordingOwner = ? AND startedAt = ?"
    return performSuspending(__db, false, true) { _connection ->
      val _stmt: SQLiteStatement = _connection.prepare(_sql)
      try {
        var _argIndex: Int = 1
        _stmt.bindText(_argIndex, owner)
        _argIndex = 2
        _stmt.bindLong(_argIndex, startedAt)
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
