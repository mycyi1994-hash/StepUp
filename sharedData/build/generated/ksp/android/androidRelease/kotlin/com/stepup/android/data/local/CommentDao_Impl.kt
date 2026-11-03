package com.stepup.android.`data`.local

import androidx.room.EntityInsertAdapter
import androidx.room.RoomDatabase
import androidx.room.coroutines.createFlow
import androidx.room.util.getColumnIndexOrThrow
import androidx.room.util.performSuspending
import androidx.sqlite.SQLiteStatement
import javax.`annotation`.processing.Generated
import kotlin.Boolean
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
public class CommentDao_Impl(
  __db: RoomDatabase,
) : CommentDao {
  private val __db: RoomDatabase

  private val __insertAdapterOfCommentEntity: EntityInsertAdapter<CommentEntity>
  init {
    this.__db = __db
    this.__insertAdapterOfCommentEntity = object : EntityInsertAdapter<CommentEntity>() {
      protected override fun createQuery(): String = "INSERT OR ABORT INTO `comments` (`id`,`postId`,`parentId`,`author`,`body`,`createdAt`,`mine`) VALUES (nullif(?, 0),?,?,?,?,?,?)"

      protected override fun bind(statement: SQLiteStatement, entity: CommentEntity) {
        statement.bindLong(1, entity.id)
        statement.bindLong(2, entity.postId)
        statement.bindLong(3, entity.parentId)
        statement.bindText(4, entity.author)
        statement.bindText(5, entity.body)
        statement.bindLong(6, entity.createdAt)
        val _tmp: Int = if (entity.mine) 1 else 0
        statement.bindLong(7, _tmp.toLong())
      }
    }
  }

  public override suspend fun insert(entity: CommentEntity): Long = performSuspending(__db, false, true) { _connection ->
    val _result: Long = __insertAdapterOfCommentEntity.insertAndReturnId(_connection, entity)
    _result
  }

  public override suspend fun insertAll(entities: List<CommentEntity>): Unit = performSuspending(__db, false, true) { _connection ->
    __insertAdapterOfCommentEntity.insert(_connection, entities)
  }

  public override fun observeForPost(postId: Long): Flow<List<CommentEntity>> {
    val _sql: String = "SELECT * FROM comments WHERE postId = ? ORDER BY createdAt ASC"
    return createFlow(__db, false, arrayOf("comments")) { _connection ->
      val _stmt: SQLiteStatement = _connection.prepare(_sql)
      try {
        var _argIndex: Int = 1
        _stmt.bindLong(_argIndex, postId)
        val _columnIndexOfId: Int = getColumnIndexOrThrow(_stmt, "id")
        val _columnIndexOfPostId: Int = getColumnIndexOrThrow(_stmt, "postId")
        val _columnIndexOfParentId: Int = getColumnIndexOrThrow(_stmt, "parentId")
        val _columnIndexOfAuthor: Int = getColumnIndexOrThrow(_stmt, "author")
        val _columnIndexOfBody: Int = getColumnIndexOrThrow(_stmt, "body")
        val _columnIndexOfCreatedAt: Int = getColumnIndexOrThrow(_stmt, "createdAt")
        val _columnIndexOfMine: Int = getColumnIndexOrThrow(_stmt, "mine")
        val _result: MutableList<CommentEntity> = mutableListOf()
        while (_stmt.step()) {
          val _item: CommentEntity
          val _tmpId: Long
          _tmpId = _stmt.getLong(_columnIndexOfId)
          val _tmpPostId: Long
          _tmpPostId = _stmt.getLong(_columnIndexOfPostId)
          val _tmpParentId: Long
          _tmpParentId = _stmt.getLong(_columnIndexOfParentId)
          val _tmpAuthor: String
          _tmpAuthor = _stmt.getText(_columnIndexOfAuthor)
          val _tmpBody: String
          _tmpBody = _stmt.getText(_columnIndexOfBody)
          val _tmpCreatedAt: Long
          _tmpCreatedAt = _stmt.getLong(_columnIndexOfCreatedAt)
          val _tmpMine: Boolean
          val _tmp: Int
          _tmp = _stmt.getLong(_columnIndexOfMine).toInt()
          _tmpMine = _tmp != 0
          _item = CommentEntity(_tmpId,_tmpPostId,_tmpParentId,_tmpAuthor,_tmpBody,_tmpCreatedAt,_tmpMine)
          _result.add(_item)
        }
        _result
      } finally {
        _stmt.close()
      }
    }
  }

  public override suspend fun byId(id: Long): CommentEntity? {
    val _sql: String = "SELECT * FROM comments WHERE id = ?"
    return performSuspending(__db, true, false) { _connection ->
      val _stmt: SQLiteStatement = _connection.prepare(_sql)
      try {
        var _argIndex: Int = 1
        _stmt.bindLong(_argIndex, id)
        val _columnIndexOfId: Int = getColumnIndexOrThrow(_stmt, "id")
        val _columnIndexOfPostId: Int = getColumnIndexOrThrow(_stmt, "postId")
        val _columnIndexOfParentId: Int = getColumnIndexOrThrow(_stmt, "parentId")
        val _columnIndexOfAuthor: Int = getColumnIndexOrThrow(_stmt, "author")
        val _columnIndexOfBody: Int = getColumnIndexOrThrow(_stmt, "body")
        val _columnIndexOfCreatedAt: Int = getColumnIndexOrThrow(_stmt, "createdAt")
        val _columnIndexOfMine: Int = getColumnIndexOrThrow(_stmt, "mine")
        val _result: CommentEntity?
        if (_stmt.step()) {
          val _tmpId: Long
          _tmpId = _stmt.getLong(_columnIndexOfId)
          val _tmpPostId: Long
          _tmpPostId = _stmt.getLong(_columnIndexOfPostId)
          val _tmpParentId: Long
          _tmpParentId = _stmt.getLong(_columnIndexOfParentId)
          val _tmpAuthor: String
          _tmpAuthor = _stmt.getText(_columnIndexOfAuthor)
          val _tmpBody: String
          _tmpBody = _stmt.getText(_columnIndexOfBody)
          val _tmpCreatedAt: Long
          _tmpCreatedAt = _stmt.getLong(_columnIndexOfCreatedAt)
          val _tmpMine: Boolean
          val _tmp: Int
          _tmp = _stmt.getLong(_columnIndexOfMine).toInt()
          _tmpMine = _tmp != 0
          _result = CommentEntity(_tmpId,_tmpPostId,_tmpParentId,_tmpAuthor,_tmpBody,_tmpCreatedAt,_tmpMine)
        } else {
          _result = null
        }
        _result
      } finally {
        _stmt.close()
      }
    }
  }

  public override suspend fun countForPost(postId: Long): Int {
    val _sql: String = "SELECT COUNT(*) FROM comments WHERE postId = ?"
    return performSuspending(__db, true, false) { _connection ->
      val _stmt: SQLiteStatement = _connection.prepare(_sql)
      try {
        var _argIndex: Int = 1
        _stmt.bindLong(_argIndex, postId)
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

  public override suspend fun replyCount(commentId: Long): Int {
    val _sql: String = "SELECT COUNT(*) FROM comments WHERE parentId = ?"
    return performSuspending(__db, true, false) { _connection ->
      val _stmt: SQLiteStatement = _connection.prepare(_sql)
      try {
        var _argIndex: Int = 1
        _stmt.bindLong(_argIndex, commentId)
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

  public override suspend fun latestMineTopLevel(postId: Long): CommentEntity? {
    val _sql: String = "SELECT * FROM comments WHERE postId = ? AND parentId = 0 AND mine = 1 ORDER BY createdAt DESC LIMIT 1"
    return performSuspending(__db, true, false) { _connection ->
      val _stmt: SQLiteStatement = _connection.prepare(_sql)
      try {
        var _argIndex: Int = 1
        _stmt.bindLong(_argIndex, postId)
        val _columnIndexOfId: Int = getColumnIndexOrThrow(_stmt, "id")
        val _columnIndexOfPostId: Int = getColumnIndexOrThrow(_stmt, "postId")
        val _columnIndexOfParentId: Int = getColumnIndexOrThrow(_stmt, "parentId")
        val _columnIndexOfAuthor: Int = getColumnIndexOrThrow(_stmt, "author")
        val _columnIndexOfBody: Int = getColumnIndexOrThrow(_stmt, "body")
        val _columnIndexOfCreatedAt: Int = getColumnIndexOrThrow(_stmt, "createdAt")
        val _columnIndexOfMine: Int = getColumnIndexOrThrow(_stmt, "mine")
        val _result: CommentEntity?
        if (_stmt.step()) {
          val _tmpId: Long
          _tmpId = _stmt.getLong(_columnIndexOfId)
          val _tmpPostId: Long
          _tmpPostId = _stmt.getLong(_columnIndexOfPostId)
          val _tmpParentId: Long
          _tmpParentId = _stmt.getLong(_columnIndexOfParentId)
          val _tmpAuthor: String
          _tmpAuthor = _stmt.getText(_columnIndexOfAuthor)
          val _tmpBody: String
          _tmpBody = _stmt.getText(_columnIndexOfBody)
          val _tmpCreatedAt: Long
          _tmpCreatedAt = _stmt.getLong(_columnIndexOfCreatedAt)
          val _tmpMine: Boolean
          val _tmp: Int
          _tmp = _stmt.getLong(_columnIndexOfMine).toInt()
          _tmpMine = _tmp != 0
          _result = CommentEntity(_tmpId,_tmpPostId,_tmpParentId,_tmpAuthor,_tmpBody,_tmpCreatedAt,_tmpMine)
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
    val _sql: String = "SELECT COUNT(*) FROM comments"
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

  public override suspend fun deleteWithReplies(id: Long) {
    val _sql: String = "DELETE FROM comments WHERE id = ? OR parentId = ?"
    return performSuspending(__db, false, true) { _connection ->
      val _stmt: SQLiteStatement = _connection.prepare(_sql)
      try {
        var _argIndex: Int = 1
        _stmt.bindLong(_argIndex, id)
        _argIndex = 2
        _stmt.bindLong(_argIndex, id)
        _stmt.step()
      } finally {
        _stmt.close()
      }
    }
  }

  public override suspend fun deleteForPost(postId: Long) {
    val _sql: String = "DELETE FROM comments WHERE postId = ?"
    return performSuspending(__db, false, true) { _connection ->
      val _stmt: SQLiteStatement = _connection.prepare(_sql)
      try {
        var _argIndex: Int = 1
        _stmt.bindLong(_argIndex, postId)
        _stmt.step()
      } finally {
        _stmt.close()
      }
    }
  }

  public override suspend fun clear() {
    val _sql: String = "DELETE FROM comments"
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
