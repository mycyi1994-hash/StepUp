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
public class CrewInfoDao_Impl(
  __db: RoomDatabase,
) : CrewInfoDao {
  private val __db: RoomDatabase

  private val __upsertAdapterOfCrewEntity: EntityUpsertAdapter<CrewEntity>
  init {
    this.__db = __db
    this.__upsertAdapterOfCrewEntity = EntityUpsertAdapter<CrewEntity>(object : EntityInsertAdapter<CrewEntity>() {
      protected override fun createQuery(): String = "INSERT INTO `crews` (`id`,`name`,`monogram`,`tagline`,`area`,`kmAway`,`memberCount`,`roster`,`createdAt`,`owned`) VALUES (?,?,?,?,?,?,?,?,?,?)"

      protected override fun bind(statement: SQLiteStatement, entity: CrewEntity) {
        statement.bindText(1, entity.id)
        statement.bindText(2, entity.name)
        statement.bindText(3, entity.monogram)
        statement.bindText(4, entity.tagline)
        statement.bindText(5, entity.area)
        statement.bindDouble(6, entity.kmAway)
        statement.bindLong(7, entity.memberCount.toLong())
        statement.bindText(8, entity.roster)
        statement.bindLong(9, entity.createdAt)
        val _tmp: Int = if (entity.owned) 1 else 0
        statement.bindLong(10, _tmp.toLong())
      }
    }, object : EntityDeleteOrUpdateAdapter<CrewEntity>() {
      protected override fun createQuery(): String = "UPDATE `crews` SET `id` = ?,`name` = ?,`monogram` = ?,`tagline` = ?,`area` = ?,`kmAway` = ?,`memberCount` = ?,`roster` = ?,`createdAt` = ?,`owned` = ? WHERE `id` = ?"

      protected override fun bind(statement: SQLiteStatement, entity: CrewEntity) {
        statement.bindText(1, entity.id)
        statement.bindText(2, entity.name)
        statement.bindText(3, entity.monogram)
        statement.bindText(4, entity.tagline)
        statement.bindText(5, entity.area)
        statement.bindDouble(6, entity.kmAway)
        statement.bindLong(7, entity.memberCount.toLong())
        statement.bindText(8, entity.roster)
        statement.bindLong(9, entity.createdAt)
        val _tmp: Int = if (entity.owned) 1 else 0
        statement.bindLong(10, _tmp.toLong())
        statement.bindText(11, entity.id)
      }
    })
  }

  public override suspend fun upsert(entity: CrewEntity): Unit = performSuspending(__db, false, true) { _connection ->
    __upsertAdapterOfCrewEntity.upsert(_connection, entity)
  }

  public override suspend fun upsertAll(entities: List<CrewEntity>): Unit = performSuspending(__db, false, true) { _connection ->
    __upsertAdapterOfCrewEntity.upsert(_connection, entities)
  }

  public override fun observeAll(): Flow<List<CrewEntity>> {
    val _sql: String = "SELECT * FROM crews ORDER BY kmAway ASC"
    return createFlow(__db, false, arrayOf("crews")) { _connection ->
      val _stmt: SQLiteStatement = _connection.prepare(_sql)
      try {
        val _columnIndexOfId: Int = getColumnIndexOrThrow(_stmt, "id")
        val _columnIndexOfName: Int = getColumnIndexOrThrow(_stmt, "name")
        val _columnIndexOfMonogram: Int = getColumnIndexOrThrow(_stmt, "monogram")
        val _columnIndexOfTagline: Int = getColumnIndexOrThrow(_stmt, "tagline")
        val _columnIndexOfArea: Int = getColumnIndexOrThrow(_stmt, "area")
        val _columnIndexOfKmAway: Int = getColumnIndexOrThrow(_stmt, "kmAway")
        val _columnIndexOfMemberCount: Int = getColumnIndexOrThrow(_stmt, "memberCount")
        val _columnIndexOfRoster: Int = getColumnIndexOrThrow(_stmt, "roster")
        val _columnIndexOfCreatedAt: Int = getColumnIndexOrThrow(_stmt, "createdAt")
        val _columnIndexOfOwned: Int = getColumnIndexOrThrow(_stmt, "owned")
        val _result: MutableList<CrewEntity> = mutableListOf()
        while (_stmt.step()) {
          val _item: CrewEntity
          val _tmpId: String
          _tmpId = _stmt.getText(_columnIndexOfId)
          val _tmpName: String
          _tmpName = _stmt.getText(_columnIndexOfName)
          val _tmpMonogram: String
          _tmpMonogram = _stmt.getText(_columnIndexOfMonogram)
          val _tmpTagline: String
          _tmpTagline = _stmt.getText(_columnIndexOfTagline)
          val _tmpArea: String
          _tmpArea = _stmt.getText(_columnIndexOfArea)
          val _tmpKmAway: Double
          _tmpKmAway = _stmt.getDouble(_columnIndexOfKmAway)
          val _tmpMemberCount: Int
          _tmpMemberCount = _stmt.getLong(_columnIndexOfMemberCount).toInt()
          val _tmpRoster: String
          _tmpRoster = _stmt.getText(_columnIndexOfRoster)
          val _tmpCreatedAt: Long
          _tmpCreatedAt = _stmt.getLong(_columnIndexOfCreatedAt)
          val _tmpOwned: Boolean
          val _tmp: Int
          _tmp = _stmt.getLong(_columnIndexOfOwned).toInt()
          _tmpOwned = _tmp != 0
          _item = CrewEntity(_tmpId,_tmpName,_tmpMonogram,_tmpTagline,_tmpArea,_tmpKmAway,_tmpMemberCount,_tmpRoster,_tmpCreatedAt,_tmpOwned)
          _result.add(_item)
        }
        _result
      } finally {
        _stmt.close()
      }
    }
  }

  public override suspend fun byId(id: String): CrewEntity? {
    val _sql: String = "SELECT * FROM crews WHERE id = ?"
    return performSuspending(__db, true, false) { _connection ->
      val _stmt: SQLiteStatement = _connection.prepare(_sql)
      try {
        var _argIndex: Int = 1
        _stmt.bindText(_argIndex, id)
        val _columnIndexOfId: Int = getColumnIndexOrThrow(_stmt, "id")
        val _columnIndexOfName: Int = getColumnIndexOrThrow(_stmt, "name")
        val _columnIndexOfMonogram: Int = getColumnIndexOrThrow(_stmt, "monogram")
        val _columnIndexOfTagline: Int = getColumnIndexOrThrow(_stmt, "tagline")
        val _columnIndexOfArea: Int = getColumnIndexOrThrow(_stmt, "area")
        val _columnIndexOfKmAway: Int = getColumnIndexOrThrow(_stmt, "kmAway")
        val _columnIndexOfMemberCount: Int = getColumnIndexOrThrow(_stmt, "memberCount")
        val _columnIndexOfRoster: Int = getColumnIndexOrThrow(_stmt, "roster")
        val _columnIndexOfCreatedAt: Int = getColumnIndexOrThrow(_stmt, "createdAt")
        val _columnIndexOfOwned: Int = getColumnIndexOrThrow(_stmt, "owned")
        val _result: CrewEntity?
        if (_stmt.step()) {
          val _tmpId: String
          _tmpId = _stmt.getText(_columnIndexOfId)
          val _tmpName: String
          _tmpName = _stmt.getText(_columnIndexOfName)
          val _tmpMonogram: String
          _tmpMonogram = _stmt.getText(_columnIndexOfMonogram)
          val _tmpTagline: String
          _tmpTagline = _stmt.getText(_columnIndexOfTagline)
          val _tmpArea: String
          _tmpArea = _stmt.getText(_columnIndexOfArea)
          val _tmpKmAway: Double
          _tmpKmAway = _stmt.getDouble(_columnIndexOfKmAway)
          val _tmpMemberCount: Int
          _tmpMemberCount = _stmt.getLong(_columnIndexOfMemberCount).toInt()
          val _tmpRoster: String
          _tmpRoster = _stmt.getText(_columnIndexOfRoster)
          val _tmpCreatedAt: Long
          _tmpCreatedAt = _stmt.getLong(_columnIndexOfCreatedAt)
          val _tmpOwned: Boolean
          val _tmp: Int
          _tmp = _stmt.getLong(_columnIndexOfOwned).toInt()
          _tmpOwned = _tmp != 0
          _result = CrewEntity(_tmpId,_tmpName,_tmpMonogram,_tmpTagline,_tmpArea,_tmpKmAway,_tmpMemberCount,_tmpRoster,_tmpCreatedAt,_tmpOwned)
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
    val _sql: String = "SELECT COUNT(*) FROM crews"
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

  public override suspend fun clear() {
    val _sql: String = "DELETE FROM crews"
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
