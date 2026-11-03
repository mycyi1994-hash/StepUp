package com.stepup.android.`data`.local

import androidx.room.EntityDeleteOrUpdateAdapter
import androidx.room.EntityInsertAdapter
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
public class CourseDao_Impl(
  __db: RoomDatabase,
) : CourseDao {
  private val __db: RoomDatabase

  private val __insertAdapterOfCourseEntity: EntityInsertAdapter<CourseEntity>

  private val __updateAdapterOfCourseEntity: EntityDeleteOrUpdateAdapter<CourseEntity>
  init {
    this.__db = __db
    this.__insertAdapterOfCourseEntity = object : EntityInsertAdapter<CourseEntity>() {
      protected override fun createQuery(): String = "INSERT OR ABORT INTO `courses` (`id`,`name`,`area`,`distanceKm`,`elevationM`,`track`,`author`,`mine`,`shared`,`likes`,`liked`,`runCount`,`createdAt`) VALUES (nullif(?, 0),?,?,?,?,?,?,?,?,?,?,?,?)"

      protected override fun bind(statement: SQLiteStatement, entity: CourseEntity) {
        statement.bindLong(1, entity.id)
        statement.bindText(2, entity.name)
        statement.bindText(3, entity.area)
        statement.bindDouble(4, entity.distanceKm)
        statement.bindLong(5, entity.elevationM.toLong())
        statement.bindText(6, entity.track)
        statement.bindText(7, entity.author)
        val _tmp: Int = if (entity.mine) 1 else 0
        statement.bindLong(8, _tmp.toLong())
        val _tmp_1: Int = if (entity.shared) 1 else 0
        statement.bindLong(9, _tmp_1.toLong())
        statement.bindLong(10, entity.likes.toLong())
        val _tmp_2: Int = if (entity.liked) 1 else 0
        statement.bindLong(11, _tmp_2.toLong())
        statement.bindLong(12, entity.runCount.toLong())
        statement.bindLong(13, entity.createdAt)
      }
    }
    this.__updateAdapterOfCourseEntity = object : EntityDeleteOrUpdateAdapter<CourseEntity>() {
      protected override fun createQuery(): String = "UPDATE OR ABORT `courses` SET `id` = ?,`name` = ?,`area` = ?,`distanceKm` = ?,`elevationM` = ?,`track` = ?,`author` = ?,`mine` = ?,`shared` = ?,`likes` = ?,`liked` = ?,`runCount` = ?,`createdAt` = ? WHERE `id` = ?"

      protected override fun bind(statement: SQLiteStatement, entity: CourseEntity) {
        statement.bindLong(1, entity.id)
        statement.bindText(2, entity.name)
        statement.bindText(3, entity.area)
        statement.bindDouble(4, entity.distanceKm)
        statement.bindLong(5, entity.elevationM.toLong())
        statement.bindText(6, entity.track)
        statement.bindText(7, entity.author)
        val _tmp: Int = if (entity.mine) 1 else 0
        statement.bindLong(8, _tmp.toLong())
        val _tmp_1: Int = if (entity.shared) 1 else 0
        statement.bindLong(9, _tmp_1.toLong())
        statement.bindLong(10, entity.likes.toLong())
        val _tmp_2: Int = if (entity.liked) 1 else 0
        statement.bindLong(11, _tmp_2.toLong())
        statement.bindLong(12, entity.runCount.toLong())
        statement.bindLong(13, entity.createdAt)
        statement.bindLong(14, entity.id)
      }
    }
  }

  public override suspend fun insert(entity: CourseEntity): Long = performSuspending(__db, false, true) { _connection ->
    val _result: Long = __insertAdapterOfCourseEntity.insertAndReturnId(_connection, entity)
    _result
  }

  public override suspend fun insertAll(entities: List<CourseEntity>): Unit = performSuspending(__db, false, true) { _connection ->
    __insertAdapterOfCourseEntity.insert(_connection, entities)
  }

  public override suspend fun update(entity: CourseEntity): Unit = performSuspending(__db, false, true) { _connection ->
    __updateAdapterOfCourseEntity.handle(_connection, entity)
  }

  public override suspend fun byTrack(track: String): CourseEntity? {
    val _sql: String = "SELECT * FROM courses WHERE track = ? ORDER BY mine DESC LIMIT 1"
    return performSuspending(__db, true, false) { _connection ->
      val _stmt: SQLiteStatement = _connection.prepare(_sql)
      try {
        var _argIndex: Int = 1
        _stmt.bindText(_argIndex, track)
        val _columnIndexOfId: Int = getColumnIndexOrThrow(_stmt, "id")
        val _columnIndexOfName: Int = getColumnIndexOrThrow(_stmt, "name")
        val _columnIndexOfArea: Int = getColumnIndexOrThrow(_stmt, "area")
        val _columnIndexOfDistanceKm: Int = getColumnIndexOrThrow(_stmt, "distanceKm")
        val _columnIndexOfElevationM: Int = getColumnIndexOrThrow(_stmt, "elevationM")
        val _columnIndexOfTrack: Int = getColumnIndexOrThrow(_stmt, "track")
        val _columnIndexOfAuthor: Int = getColumnIndexOrThrow(_stmt, "author")
        val _columnIndexOfMine: Int = getColumnIndexOrThrow(_stmt, "mine")
        val _columnIndexOfShared: Int = getColumnIndexOrThrow(_stmt, "shared")
        val _columnIndexOfLikes: Int = getColumnIndexOrThrow(_stmt, "likes")
        val _columnIndexOfLiked: Int = getColumnIndexOrThrow(_stmt, "liked")
        val _columnIndexOfRunCount: Int = getColumnIndexOrThrow(_stmt, "runCount")
        val _columnIndexOfCreatedAt: Int = getColumnIndexOrThrow(_stmt, "createdAt")
        val _result: CourseEntity?
        if (_stmt.step()) {
          val _tmpId: Long
          _tmpId = _stmt.getLong(_columnIndexOfId)
          val _tmpName: String
          _tmpName = _stmt.getText(_columnIndexOfName)
          val _tmpArea: String
          _tmpArea = _stmt.getText(_columnIndexOfArea)
          val _tmpDistanceKm: Double
          _tmpDistanceKm = _stmt.getDouble(_columnIndexOfDistanceKm)
          val _tmpElevationM: Int
          _tmpElevationM = _stmt.getLong(_columnIndexOfElevationM).toInt()
          val _tmpTrack: String
          _tmpTrack = _stmt.getText(_columnIndexOfTrack)
          val _tmpAuthor: String
          _tmpAuthor = _stmt.getText(_columnIndexOfAuthor)
          val _tmpMine: Boolean
          val _tmp: Int
          _tmp = _stmt.getLong(_columnIndexOfMine).toInt()
          _tmpMine = _tmp != 0
          val _tmpShared: Boolean
          val _tmp_1: Int
          _tmp_1 = _stmt.getLong(_columnIndexOfShared).toInt()
          _tmpShared = _tmp_1 != 0
          val _tmpLikes: Int
          _tmpLikes = _stmt.getLong(_columnIndexOfLikes).toInt()
          val _tmpLiked: Boolean
          val _tmp_2: Int
          _tmp_2 = _stmt.getLong(_columnIndexOfLiked).toInt()
          _tmpLiked = _tmp_2 != 0
          val _tmpRunCount: Int
          _tmpRunCount = _stmt.getLong(_columnIndexOfRunCount).toInt()
          val _tmpCreatedAt: Long
          _tmpCreatedAt = _stmt.getLong(_columnIndexOfCreatedAt)
          _result = CourseEntity(_tmpId,_tmpName,_tmpArea,_tmpDistanceKm,_tmpElevationM,_tmpTrack,_tmpAuthor,_tmpMine,_tmpShared,_tmpLikes,_tmpLiked,_tmpRunCount,_tmpCreatedAt)
        } else {
          _result = null
        }
        _result
      } finally {
        _stmt.close()
      }
    }
  }

  public override fun observeAll(): Flow<List<CourseEntity>> {
    val _sql: String = "SELECT * FROM courses ORDER BY mine DESC, createdAt DESC"
    return createFlow(__db, false, arrayOf("courses")) { _connection ->
      val _stmt: SQLiteStatement = _connection.prepare(_sql)
      try {
        val _columnIndexOfId: Int = getColumnIndexOrThrow(_stmt, "id")
        val _columnIndexOfName: Int = getColumnIndexOrThrow(_stmt, "name")
        val _columnIndexOfArea: Int = getColumnIndexOrThrow(_stmt, "area")
        val _columnIndexOfDistanceKm: Int = getColumnIndexOrThrow(_stmt, "distanceKm")
        val _columnIndexOfElevationM: Int = getColumnIndexOrThrow(_stmt, "elevationM")
        val _columnIndexOfTrack: Int = getColumnIndexOrThrow(_stmt, "track")
        val _columnIndexOfAuthor: Int = getColumnIndexOrThrow(_stmt, "author")
        val _columnIndexOfMine: Int = getColumnIndexOrThrow(_stmt, "mine")
        val _columnIndexOfShared: Int = getColumnIndexOrThrow(_stmt, "shared")
        val _columnIndexOfLikes: Int = getColumnIndexOrThrow(_stmt, "likes")
        val _columnIndexOfLiked: Int = getColumnIndexOrThrow(_stmt, "liked")
        val _columnIndexOfRunCount: Int = getColumnIndexOrThrow(_stmt, "runCount")
        val _columnIndexOfCreatedAt: Int = getColumnIndexOrThrow(_stmt, "createdAt")
        val _result: MutableList<CourseEntity> = mutableListOf()
        while (_stmt.step()) {
          val _item: CourseEntity
          val _tmpId: Long
          _tmpId = _stmt.getLong(_columnIndexOfId)
          val _tmpName: String
          _tmpName = _stmt.getText(_columnIndexOfName)
          val _tmpArea: String
          _tmpArea = _stmt.getText(_columnIndexOfArea)
          val _tmpDistanceKm: Double
          _tmpDistanceKm = _stmt.getDouble(_columnIndexOfDistanceKm)
          val _tmpElevationM: Int
          _tmpElevationM = _stmt.getLong(_columnIndexOfElevationM).toInt()
          val _tmpTrack: String
          _tmpTrack = _stmt.getText(_columnIndexOfTrack)
          val _tmpAuthor: String
          _tmpAuthor = _stmt.getText(_columnIndexOfAuthor)
          val _tmpMine: Boolean
          val _tmp: Int
          _tmp = _stmt.getLong(_columnIndexOfMine).toInt()
          _tmpMine = _tmp != 0
          val _tmpShared: Boolean
          val _tmp_1: Int
          _tmp_1 = _stmt.getLong(_columnIndexOfShared).toInt()
          _tmpShared = _tmp_1 != 0
          val _tmpLikes: Int
          _tmpLikes = _stmt.getLong(_columnIndexOfLikes).toInt()
          val _tmpLiked: Boolean
          val _tmp_2: Int
          _tmp_2 = _stmt.getLong(_columnIndexOfLiked).toInt()
          _tmpLiked = _tmp_2 != 0
          val _tmpRunCount: Int
          _tmpRunCount = _stmt.getLong(_columnIndexOfRunCount).toInt()
          val _tmpCreatedAt: Long
          _tmpCreatedAt = _stmt.getLong(_columnIndexOfCreatedAt)
          _item = CourseEntity(_tmpId,_tmpName,_tmpArea,_tmpDistanceKm,_tmpElevationM,_tmpTrack,_tmpAuthor,_tmpMine,_tmpShared,_tmpLikes,_tmpLiked,_tmpRunCount,_tmpCreatedAt)
          _result.add(_item)
        }
        _result
      } finally {
        _stmt.close()
      }
    }
  }

  public override suspend fun byId(id: Long): CourseEntity? {
    val _sql: String = "SELECT * FROM courses WHERE id = ?"
    return performSuspending(__db, true, false) { _connection ->
      val _stmt: SQLiteStatement = _connection.prepare(_sql)
      try {
        var _argIndex: Int = 1
        _stmt.bindLong(_argIndex, id)
        val _columnIndexOfId: Int = getColumnIndexOrThrow(_stmt, "id")
        val _columnIndexOfName: Int = getColumnIndexOrThrow(_stmt, "name")
        val _columnIndexOfArea: Int = getColumnIndexOrThrow(_stmt, "area")
        val _columnIndexOfDistanceKm: Int = getColumnIndexOrThrow(_stmt, "distanceKm")
        val _columnIndexOfElevationM: Int = getColumnIndexOrThrow(_stmt, "elevationM")
        val _columnIndexOfTrack: Int = getColumnIndexOrThrow(_stmt, "track")
        val _columnIndexOfAuthor: Int = getColumnIndexOrThrow(_stmt, "author")
        val _columnIndexOfMine: Int = getColumnIndexOrThrow(_stmt, "mine")
        val _columnIndexOfShared: Int = getColumnIndexOrThrow(_stmt, "shared")
        val _columnIndexOfLikes: Int = getColumnIndexOrThrow(_stmt, "likes")
        val _columnIndexOfLiked: Int = getColumnIndexOrThrow(_stmt, "liked")
        val _columnIndexOfRunCount: Int = getColumnIndexOrThrow(_stmt, "runCount")
        val _columnIndexOfCreatedAt: Int = getColumnIndexOrThrow(_stmt, "createdAt")
        val _result: CourseEntity?
        if (_stmt.step()) {
          val _tmpId: Long
          _tmpId = _stmt.getLong(_columnIndexOfId)
          val _tmpName: String
          _tmpName = _stmt.getText(_columnIndexOfName)
          val _tmpArea: String
          _tmpArea = _stmt.getText(_columnIndexOfArea)
          val _tmpDistanceKm: Double
          _tmpDistanceKm = _stmt.getDouble(_columnIndexOfDistanceKm)
          val _tmpElevationM: Int
          _tmpElevationM = _stmt.getLong(_columnIndexOfElevationM).toInt()
          val _tmpTrack: String
          _tmpTrack = _stmt.getText(_columnIndexOfTrack)
          val _tmpAuthor: String
          _tmpAuthor = _stmt.getText(_columnIndexOfAuthor)
          val _tmpMine: Boolean
          val _tmp: Int
          _tmp = _stmt.getLong(_columnIndexOfMine).toInt()
          _tmpMine = _tmp != 0
          val _tmpShared: Boolean
          val _tmp_1: Int
          _tmp_1 = _stmt.getLong(_columnIndexOfShared).toInt()
          _tmpShared = _tmp_1 != 0
          val _tmpLikes: Int
          _tmpLikes = _stmt.getLong(_columnIndexOfLikes).toInt()
          val _tmpLiked: Boolean
          val _tmp_2: Int
          _tmp_2 = _stmt.getLong(_columnIndexOfLiked).toInt()
          _tmpLiked = _tmp_2 != 0
          val _tmpRunCount: Int
          _tmpRunCount = _stmt.getLong(_columnIndexOfRunCount).toInt()
          val _tmpCreatedAt: Long
          _tmpCreatedAt = _stmt.getLong(_columnIndexOfCreatedAt)
          _result = CourseEntity(_tmpId,_tmpName,_tmpArea,_tmpDistanceKm,_tmpElevationM,_tmpTrack,_tmpAuthor,_tmpMine,_tmpShared,_tmpLikes,_tmpLiked,_tmpRunCount,_tmpCreatedAt)
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
    val _sql: String = "SELECT COUNT(*) FROM courses"
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

  public override suspend fun deleteLocal(id: Long) {
    val _sql: String = "DELETE FROM courses WHERE id = ? AND (mine = 1 OR shared = 0)"
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

  public override suspend fun deleteSeeded() {
    val _sql: String = "DELETE FROM courses WHERE mine = 0 AND shared = 1"
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
