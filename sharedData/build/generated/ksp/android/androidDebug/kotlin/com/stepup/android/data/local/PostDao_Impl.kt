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
public class PostDao_Impl(
  __db: RoomDatabase,
) : PostDao {
  private val __db: RoomDatabase

  private val __insertAdapterOfPostEntity: EntityInsertAdapter<PostEntity>

  private val __updateAdapterOfPostEntity: EntityDeleteOrUpdateAdapter<PostEntity>
  init {
    this.__db = __db
    this.__insertAdapterOfPostEntity = object : EntityInsertAdapter<PostEntity>() {
      protected override fun createQuery(): String = "INSERT OR ABORT INTO `posts` (`id`,`category`,`crewId`,`author`,`title`,`body`,`createdAt`,`likes`,`liked`,`commentCount`,`mine`,`place`,`distanceKm`,`meetAt`,`capacity`,`joinedCount`,`joined`) VALUES (nullif(?, 0),?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?)"

      protected override fun bind(statement: SQLiteStatement, entity: PostEntity) {
        statement.bindLong(1, entity.id)
        statement.bindText(2, entity.category)
        statement.bindText(3, entity.crewId)
        statement.bindText(4, entity.author)
        statement.bindText(5, entity.title)
        statement.bindText(6, entity.body)
        statement.bindLong(7, entity.createdAt)
        statement.bindLong(8, entity.likes.toLong())
        val _tmp: Int = if (entity.liked) 1 else 0
        statement.bindLong(9, _tmp.toLong())
        statement.bindLong(10, entity.commentCount.toLong())
        val _tmp_1: Int = if (entity.mine) 1 else 0
        statement.bindLong(11, _tmp_1.toLong())
        statement.bindText(12, entity.place)
        statement.bindDouble(13, entity.distanceKm)
        statement.bindLong(14, entity.meetAt)
        statement.bindLong(15, entity.capacity.toLong())
        statement.bindLong(16, entity.joinedCount.toLong())
        val _tmp_2: Int = if (entity.joined) 1 else 0
        statement.bindLong(17, _tmp_2.toLong())
      }
    }
    this.__updateAdapterOfPostEntity = object : EntityDeleteOrUpdateAdapter<PostEntity>() {
      protected override fun createQuery(): String = "UPDATE OR ABORT `posts` SET `id` = ?,`category` = ?,`crewId` = ?,`author` = ?,`title` = ?,`body` = ?,`createdAt` = ?,`likes` = ?,`liked` = ?,`commentCount` = ?,`mine` = ?,`place` = ?,`distanceKm` = ?,`meetAt` = ?,`capacity` = ?,`joinedCount` = ?,`joined` = ? WHERE `id` = ?"

      protected override fun bind(statement: SQLiteStatement, entity: PostEntity) {
        statement.bindLong(1, entity.id)
        statement.bindText(2, entity.category)
        statement.bindText(3, entity.crewId)
        statement.bindText(4, entity.author)
        statement.bindText(5, entity.title)
        statement.bindText(6, entity.body)
        statement.bindLong(7, entity.createdAt)
        statement.bindLong(8, entity.likes.toLong())
        val _tmp: Int = if (entity.liked) 1 else 0
        statement.bindLong(9, _tmp.toLong())
        statement.bindLong(10, entity.commentCount.toLong())
        val _tmp_1: Int = if (entity.mine) 1 else 0
        statement.bindLong(11, _tmp_1.toLong())
        statement.bindText(12, entity.place)
        statement.bindDouble(13, entity.distanceKm)
        statement.bindLong(14, entity.meetAt)
        statement.bindLong(15, entity.capacity.toLong())
        statement.bindLong(16, entity.joinedCount.toLong())
        val _tmp_2: Int = if (entity.joined) 1 else 0
        statement.bindLong(17, _tmp_2.toLong())
        statement.bindLong(18, entity.id)
      }
    }
  }

  public override suspend fun insert(entity: PostEntity): Long = performSuspending(__db, false, true) { _connection ->
    val _result: Long = __insertAdapterOfPostEntity.insertAndReturnId(_connection, entity)
    _result
  }

  public override suspend fun insertAll(entities: List<PostEntity>): List<Long> = performSuspending(__db, false, true) { _connection ->
    val _result: List<Long> = __insertAdapterOfPostEntity.insertAndReturnIdsList(_connection, entities)
    _result
  }

  public override suspend fun update(entity: PostEntity): Unit = performSuspending(__db, false, true) { _connection ->
    __updateAdapterOfPostEntity.handle(_connection, entity)
  }

  public override fun observeAll(): Flow<List<PostEntity>> {
    val _sql: String = "SELECT * FROM posts ORDER BY createdAt DESC"
    return createFlow(__db, false, arrayOf("posts")) { _connection ->
      val _stmt: SQLiteStatement = _connection.prepare(_sql)
      try {
        val _columnIndexOfId: Int = getColumnIndexOrThrow(_stmt, "id")
        val _columnIndexOfCategory: Int = getColumnIndexOrThrow(_stmt, "category")
        val _columnIndexOfCrewId: Int = getColumnIndexOrThrow(_stmt, "crewId")
        val _columnIndexOfAuthor: Int = getColumnIndexOrThrow(_stmt, "author")
        val _columnIndexOfTitle: Int = getColumnIndexOrThrow(_stmt, "title")
        val _columnIndexOfBody: Int = getColumnIndexOrThrow(_stmt, "body")
        val _columnIndexOfCreatedAt: Int = getColumnIndexOrThrow(_stmt, "createdAt")
        val _columnIndexOfLikes: Int = getColumnIndexOrThrow(_stmt, "likes")
        val _columnIndexOfLiked: Int = getColumnIndexOrThrow(_stmt, "liked")
        val _columnIndexOfCommentCount: Int = getColumnIndexOrThrow(_stmt, "commentCount")
        val _columnIndexOfMine: Int = getColumnIndexOrThrow(_stmt, "mine")
        val _columnIndexOfPlace: Int = getColumnIndexOrThrow(_stmt, "place")
        val _columnIndexOfDistanceKm: Int = getColumnIndexOrThrow(_stmt, "distanceKm")
        val _columnIndexOfMeetAt: Int = getColumnIndexOrThrow(_stmt, "meetAt")
        val _columnIndexOfCapacity: Int = getColumnIndexOrThrow(_stmt, "capacity")
        val _columnIndexOfJoinedCount: Int = getColumnIndexOrThrow(_stmt, "joinedCount")
        val _columnIndexOfJoined: Int = getColumnIndexOrThrow(_stmt, "joined")
        val _result: MutableList<PostEntity> = mutableListOf()
        while (_stmt.step()) {
          val _item: PostEntity
          val _tmpId: Long
          _tmpId = _stmt.getLong(_columnIndexOfId)
          val _tmpCategory: String
          _tmpCategory = _stmt.getText(_columnIndexOfCategory)
          val _tmpCrewId: String
          _tmpCrewId = _stmt.getText(_columnIndexOfCrewId)
          val _tmpAuthor: String
          _tmpAuthor = _stmt.getText(_columnIndexOfAuthor)
          val _tmpTitle: String
          _tmpTitle = _stmt.getText(_columnIndexOfTitle)
          val _tmpBody: String
          _tmpBody = _stmt.getText(_columnIndexOfBody)
          val _tmpCreatedAt: Long
          _tmpCreatedAt = _stmt.getLong(_columnIndexOfCreatedAt)
          val _tmpLikes: Int
          _tmpLikes = _stmt.getLong(_columnIndexOfLikes).toInt()
          val _tmpLiked: Boolean
          val _tmp: Int
          _tmp = _stmt.getLong(_columnIndexOfLiked).toInt()
          _tmpLiked = _tmp != 0
          val _tmpCommentCount: Int
          _tmpCommentCount = _stmt.getLong(_columnIndexOfCommentCount).toInt()
          val _tmpMine: Boolean
          val _tmp_1: Int
          _tmp_1 = _stmt.getLong(_columnIndexOfMine).toInt()
          _tmpMine = _tmp_1 != 0
          val _tmpPlace: String
          _tmpPlace = _stmt.getText(_columnIndexOfPlace)
          val _tmpDistanceKm: Double
          _tmpDistanceKm = _stmt.getDouble(_columnIndexOfDistanceKm)
          val _tmpMeetAt: Long
          _tmpMeetAt = _stmt.getLong(_columnIndexOfMeetAt)
          val _tmpCapacity: Int
          _tmpCapacity = _stmt.getLong(_columnIndexOfCapacity).toInt()
          val _tmpJoinedCount: Int
          _tmpJoinedCount = _stmt.getLong(_columnIndexOfJoinedCount).toInt()
          val _tmpJoined: Boolean
          val _tmp_2: Int
          _tmp_2 = _stmt.getLong(_columnIndexOfJoined).toInt()
          _tmpJoined = _tmp_2 != 0
          _item = PostEntity(_tmpId,_tmpCategory,_tmpCrewId,_tmpAuthor,_tmpTitle,_tmpBody,_tmpCreatedAt,_tmpLikes,_tmpLiked,_tmpCommentCount,_tmpMine,_tmpPlace,_tmpDistanceKm,_tmpMeetAt,_tmpCapacity,_tmpJoinedCount,_tmpJoined)
          _result.add(_item)
        }
        _result
      } finally {
        _stmt.close()
      }
    }
  }

  public override suspend fun allOnce(): List<PostEntity> {
    val _sql: String = "SELECT * FROM posts ORDER BY createdAt DESC"
    return performSuspending(__db, true, false) { _connection ->
      val _stmt: SQLiteStatement = _connection.prepare(_sql)
      try {
        val _columnIndexOfId: Int = getColumnIndexOrThrow(_stmt, "id")
        val _columnIndexOfCategory: Int = getColumnIndexOrThrow(_stmt, "category")
        val _columnIndexOfCrewId: Int = getColumnIndexOrThrow(_stmt, "crewId")
        val _columnIndexOfAuthor: Int = getColumnIndexOrThrow(_stmt, "author")
        val _columnIndexOfTitle: Int = getColumnIndexOrThrow(_stmt, "title")
        val _columnIndexOfBody: Int = getColumnIndexOrThrow(_stmt, "body")
        val _columnIndexOfCreatedAt: Int = getColumnIndexOrThrow(_stmt, "createdAt")
        val _columnIndexOfLikes: Int = getColumnIndexOrThrow(_stmt, "likes")
        val _columnIndexOfLiked: Int = getColumnIndexOrThrow(_stmt, "liked")
        val _columnIndexOfCommentCount: Int = getColumnIndexOrThrow(_stmt, "commentCount")
        val _columnIndexOfMine: Int = getColumnIndexOrThrow(_stmt, "mine")
        val _columnIndexOfPlace: Int = getColumnIndexOrThrow(_stmt, "place")
        val _columnIndexOfDistanceKm: Int = getColumnIndexOrThrow(_stmt, "distanceKm")
        val _columnIndexOfMeetAt: Int = getColumnIndexOrThrow(_stmt, "meetAt")
        val _columnIndexOfCapacity: Int = getColumnIndexOrThrow(_stmt, "capacity")
        val _columnIndexOfJoinedCount: Int = getColumnIndexOrThrow(_stmt, "joinedCount")
        val _columnIndexOfJoined: Int = getColumnIndexOrThrow(_stmt, "joined")
        val _result: MutableList<PostEntity> = mutableListOf()
        while (_stmt.step()) {
          val _item: PostEntity
          val _tmpId: Long
          _tmpId = _stmt.getLong(_columnIndexOfId)
          val _tmpCategory: String
          _tmpCategory = _stmt.getText(_columnIndexOfCategory)
          val _tmpCrewId: String
          _tmpCrewId = _stmt.getText(_columnIndexOfCrewId)
          val _tmpAuthor: String
          _tmpAuthor = _stmt.getText(_columnIndexOfAuthor)
          val _tmpTitle: String
          _tmpTitle = _stmt.getText(_columnIndexOfTitle)
          val _tmpBody: String
          _tmpBody = _stmt.getText(_columnIndexOfBody)
          val _tmpCreatedAt: Long
          _tmpCreatedAt = _stmt.getLong(_columnIndexOfCreatedAt)
          val _tmpLikes: Int
          _tmpLikes = _stmt.getLong(_columnIndexOfLikes).toInt()
          val _tmpLiked: Boolean
          val _tmp: Int
          _tmp = _stmt.getLong(_columnIndexOfLiked).toInt()
          _tmpLiked = _tmp != 0
          val _tmpCommentCount: Int
          _tmpCommentCount = _stmt.getLong(_columnIndexOfCommentCount).toInt()
          val _tmpMine: Boolean
          val _tmp_1: Int
          _tmp_1 = _stmt.getLong(_columnIndexOfMine).toInt()
          _tmpMine = _tmp_1 != 0
          val _tmpPlace: String
          _tmpPlace = _stmt.getText(_columnIndexOfPlace)
          val _tmpDistanceKm: Double
          _tmpDistanceKm = _stmt.getDouble(_columnIndexOfDistanceKm)
          val _tmpMeetAt: Long
          _tmpMeetAt = _stmt.getLong(_columnIndexOfMeetAt)
          val _tmpCapacity: Int
          _tmpCapacity = _stmt.getLong(_columnIndexOfCapacity).toInt()
          val _tmpJoinedCount: Int
          _tmpJoinedCount = _stmt.getLong(_columnIndexOfJoinedCount).toInt()
          val _tmpJoined: Boolean
          val _tmp_2: Int
          _tmp_2 = _stmt.getLong(_columnIndexOfJoined).toInt()
          _tmpJoined = _tmp_2 != 0
          _item = PostEntity(_tmpId,_tmpCategory,_tmpCrewId,_tmpAuthor,_tmpTitle,_tmpBody,_tmpCreatedAt,_tmpLikes,_tmpLiked,_tmpCommentCount,_tmpMine,_tmpPlace,_tmpDistanceKm,_tmpMeetAt,_tmpCapacity,_tmpJoinedCount,_tmpJoined)
          _result.add(_item)
        }
        _result
      } finally {
        _stmt.close()
      }
    }
  }

  public override suspend fun byId(id: Long): PostEntity? {
    val _sql: String = "SELECT * FROM posts WHERE id = ?"
    return performSuspending(__db, true, false) { _connection ->
      val _stmt: SQLiteStatement = _connection.prepare(_sql)
      try {
        var _argIndex: Int = 1
        _stmt.bindLong(_argIndex, id)
        val _columnIndexOfId: Int = getColumnIndexOrThrow(_stmt, "id")
        val _columnIndexOfCategory: Int = getColumnIndexOrThrow(_stmt, "category")
        val _columnIndexOfCrewId: Int = getColumnIndexOrThrow(_stmt, "crewId")
        val _columnIndexOfAuthor: Int = getColumnIndexOrThrow(_stmt, "author")
        val _columnIndexOfTitle: Int = getColumnIndexOrThrow(_stmt, "title")
        val _columnIndexOfBody: Int = getColumnIndexOrThrow(_stmt, "body")
        val _columnIndexOfCreatedAt: Int = getColumnIndexOrThrow(_stmt, "createdAt")
        val _columnIndexOfLikes: Int = getColumnIndexOrThrow(_stmt, "likes")
        val _columnIndexOfLiked: Int = getColumnIndexOrThrow(_stmt, "liked")
        val _columnIndexOfCommentCount: Int = getColumnIndexOrThrow(_stmt, "commentCount")
        val _columnIndexOfMine: Int = getColumnIndexOrThrow(_stmt, "mine")
        val _columnIndexOfPlace: Int = getColumnIndexOrThrow(_stmt, "place")
        val _columnIndexOfDistanceKm: Int = getColumnIndexOrThrow(_stmt, "distanceKm")
        val _columnIndexOfMeetAt: Int = getColumnIndexOrThrow(_stmt, "meetAt")
        val _columnIndexOfCapacity: Int = getColumnIndexOrThrow(_stmt, "capacity")
        val _columnIndexOfJoinedCount: Int = getColumnIndexOrThrow(_stmt, "joinedCount")
        val _columnIndexOfJoined: Int = getColumnIndexOrThrow(_stmt, "joined")
        val _result: PostEntity?
        if (_stmt.step()) {
          val _tmpId: Long
          _tmpId = _stmt.getLong(_columnIndexOfId)
          val _tmpCategory: String
          _tmpCategory = _stmt.getText(_columnIndexOfCategory)
          val _tmpCrewId: String
          _tmpCrewId = _stmt.getText(_columnIndexOfCrewId)
          val _tmpAuthor: String
          _tmpAuthor = _stmt.getText(_columnIndexOfAuthor)
          val _tmpTitle: String
          _tmpTitle = _stmt.getText(_columnIndexOfTitle)
          val _tmpBody: String
          _tmpBody = _stmt.getText(_columnIndexOfBody)
          val _tmpCreatedAt: Long
          _tmpCreatedAt = _stmt.getLong(_columnIndexOfCreatedAt)
          val _tmpLikes: Int
          _tmpLikes = _stmt.getLong(_columnIndexOfLikes).toInt()
          val _tmpLiked: Boolean
          val _tmp: Int
          _tmp = _stmt.getLong(_columnIndexOfLiked).toInt()
          _tmpLiked = _tmp != 0
          val _tmpCommentCount: Int
          _tmpCommentCount = _stmt.getLong(_columnIndexOfCommentCount).toInt()
          val _tmpMine: Boolean
          val _tmp_1: Int
          _tmp_1 = _stmt.getLong(_columnIndexOfMine).toInt()
          _tmpMine = _tmp_1 != 0
          val _tmpPlace: String
          _tmpPlace = _stmt.getText(_columnIndexOfPlace)
          val _tmpDistanceKm: Double
          _tmpDistanceKm = _stmt.getDouble(_columnIndexOfDistanceKm)
          val _tmpMeetAt: Long
          _tmpMeetAt = _stmt.getLong(_columnIndexOfMeetAt)
          val _tmpCapacity: Int
          _tmpCapacity = _stmt.getLong(_columnIndexOfCapacity).toInt()
          val _tmpJoinedCount: Int
          _tmpJoinedCount = _stmt.getLong(_columnIndexOfJoinedCount).toInt()
          val _tmpJoined: Boolean
          val _tmp_2: Int
          _tmp_2 = _stmt.getLong(_columnIndexOfJoined).toInt()
          _tmpJoined = _tmp_2 != 0
          _result = PostEntity(_tmpId,_tmpCategory,_tmpCrewId,_tmpAuthor,_tmpTitle,_tmpBody,_tmpCreatedAt,_tmpLikes,_tmpLiked,_tmpCommentCount,_tmpMine,_tmpPlace,_tmpDistanceKm,_tmpMeetAt,_tmpCapacity,_tmpJoinedCount,_tmpJoined)
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
    val _sql: String = "SELECT COUNT(*) FROM posts"
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

  public override fun observeMineCount(): Flow<Int> {
    val _sql: String = "SELECT COUNT(*) FROM posts WHERE mine = 1"
    return createFlow(__db, false, arrayOf("posts")) { _connection ->
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

  public override suspend fun delete(id: Long) {
    val _sql: String = "DELETE FROM posts WHERE id = ?"
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
    val _sql: String = "DELETE FROM posts"
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
