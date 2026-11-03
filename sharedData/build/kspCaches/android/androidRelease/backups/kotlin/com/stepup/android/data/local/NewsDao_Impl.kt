package com.stepup.android.`data`.local

import androidx.room.EntityInsertAdapter
import androidx.room.RoomDatabase
import androidx.room.coroutines.createFlow
import androidx.room.util.getColumnIndexOrThrow
import androidx.room.util.performInTransactionSuspending
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
public class NewsDao_Impl(
  __db: RoomDatabase,
) : NewsDao {
  private val __db: RoomDatabase

  private val __insertAdapterOfNewsItemEntity: EntityInsertAdapter<NewsItemEntity>
  init {
    this.__db = __db
    this.__insertAdapterOfNewsItemEntity = object : EntityInsertAdapter<NewsItemEntity>() {
      protected override fun createQuery(): String = "INSERT OR REPLACE INTO `news_items` (`url`,`title`,`source`,`summary`,`kind`,`publishedAt`) VALUES (?,?,?,?,?,?)"

      protected override fun bind(statement: SQLiteStatement, entity: NewsItemEntity) {
        statement.bindText(1, entity.url)
        statement.bindText(2, entity.title)
        statement.bindText(3, entity.source)
        statement.bindText(4, entity.summary)
        statement.bindText(5, entity.kind)
        statement.bindLong(6, entity.publishedAt)
      }
    }
  }

  public override suspend fun upsert(items: List<NewsItemEntity>): Unit = performSuspending(__db, false, true) { _connection ->
    __insertAdapterOfNewsItemEntity.insert(_connection, items)
  }

  public override suspend fun replace(kind: String, items: List<NewsItemEntity>): Unit = performInTransactionSuspending(__db) {
    super@NewsDao_Impl.replace(kind, items)
  }

  public override fun observe(kind: String, limit: Int): Flow<List<NewsItemEntity>> {
    val _sql: String = "SELECT * FROM news_items WHERE kind = ? ORDER BY publishedAt DESC LIMIT ?"
    return createFlow(__db, false, arrayOf("news_items")) { _connection ->
      val _stmt: SQLiteStatement = _connection.prepare(_sql)
      try {
        var _argIndex: Int = 1
        _stmt.bindText(_argIndex, kind)
        _argIndex = 2
        _stmt.bindLong(_argIndex, limit.toLong())
        val _columnIndexOfUrl: Int = getColumnIndexOrThrow(_stmt, "url")
        val _columnIndexOfTitle: Int = getColumnIndexOrThrow(_stmt, "title")
        val _columnIndexOfSource: Int = getColumnIndexOrThrow(_stmt, "source")
        val _columnIndexOfSummary: Int = getColumnIndexOrThrow(_stmt, "summary")
        val _columnIndexOfKind: Int = getColumnIndexOrThrow(_stmt, "kind")
        val _columnIndexOfPublishedAt: Int = getColumnIndexOrThrow(_stmt, "publishedAt")
        val _result: MutableList<NewsItemEntity> = mutableListOf()
        while (_stmt.step()) {
          val _item: NewsItemEntity
          val _tmpUrl: String
          _tmpUrl = _stmt.getText(_columnIndexOfUrl)
          val _tmpTitle: String
          _tmpTitle = _stmt.getText(_columnIndexOfTitle)
          val _tmpSource: String
          _tmpSource = _stmt.getText(_columnIndexOfSource)
          val _tmpSummary: String
          _tmpSummary = _stmt.getText(_columnIndexOfSummary)
          val _tmpKind: String
          _tmpKind = _stmt.getText(_columnIndexOfKind)
          val _tmpPublishedAt: Long
          _tmpPublishedAt = _stmt.getLong(_columnIndexOfPublishedAt)
          _item = NewsItemEntity(_tmpUrl,_tmpTitle,_tmpSource,_tmpSummary,_tmpKind,_tmpPublishedAt)
          _result.add(_item)
        }
        _result
      } finally {
        _stmt.close()
      }
    }
  }

  public override suspend fun clear(kind: String) {
    val _sql: String = "DELETE FROM news_items WHERE kind = ?"
    return performSuspending(__db, false, true) { _connection ->
      val _stmt: SQLiteStatement = _connection.prepare(_sql)
      try {
        var _argIndex: Int = 1
        _stmt.bindText(_argIndex, kind)
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
