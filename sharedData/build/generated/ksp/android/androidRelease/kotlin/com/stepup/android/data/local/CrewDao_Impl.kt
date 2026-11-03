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
public class CrewDao_Impl(
  __db: RoomDatabase,
) : CrewDao {
  private val __db: RoomDatabase

  private val __insertAdapterOfCrewMembershipEntity: EntityInsertAdapter<CrewMembershipEntity>
  init {
    this.__db = __db
    this.__insertAdapterOfCrewMembershipEntity = object : EntityInsertAdapter<CrewMembershipEntity>() {
      protected override fun createQuery(): String = "INSERT OR ABORT INTO `crew_memberships` (`crewId`,`joinedAt`) VALUES (?,?)"

      protected override fun bind(statement: SQLiteStatement, entity: CrewMembershipEntity) {
        statement.bindText(1, entity.crewId)
        statement.bindLong(2, entity.joinedAt)
      }
    }
  }

  public override suspend fun insert(entity: CrewMembershipEntity): Unit = performSuspending(__db, false, true) { _connection ->
    __insertAdapterOfCrewMembershipEntity.insert(_connection, entity)
  }

  public override fun observeAll(): Flow<List<CrewMembershipEntity>> {
    val _sql: String = "SELECT * FROM crew_memberships"
    return createFlow(__db, false, arrayOf("crew_memberships")) { _connection ->
      val _stmt: SQLiteStatement = _connection.prepare(_sql)
      try {
        val _columnIndexOfCrewId: Int = getColumnIndexOrThrow(_stmt, "crewId")
        val _columnIndexOfJoinedAt: Int = getColumnIndexOrThrow(_stmt, "joinedAt")
        val _result: MutableList<CrewMembershipEntity> = mutableListOf()
        while (_stmt.step()) {
          val _item: CrewMembershipEntity
          val _tmpCrewId: String
          _tmpCrewId = _stmt.getText(_columnIndexOfCrewId)
          val _tmpJoinedAt: Long
          _tmpJoinedAt = _stmt.getLong(_columnIndexOfJoinedAt)
          _item = CrewMembershipEntity(_tmpCrewId,_tmpJoinedAt)
          _result.add(_item)
        }
        _result
      } finally {
        _stmt.close()
      }
    }
  }

  public override suspend fun leave(id: String) {
    val _sql: String = "DELETE FROM crew_memberships WHERE crewId = ?"
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

  public override suspend fun clear() {
    val _sql: String = "DELETE FROM crew_memberships"
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
