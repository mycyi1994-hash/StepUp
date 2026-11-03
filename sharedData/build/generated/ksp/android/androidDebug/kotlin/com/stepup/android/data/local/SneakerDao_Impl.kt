package com.stepup.android.`data`.local

import androidx.room.EntityDeleteOrUpdateAdapter
import androidx.room.EntityInsertAdapter
import androidx.room.RoomDatabase
import androidx.room.coroutines.createFlow
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
import kotlinx.coroutines.flow.Flow

@Generated(value = ["androidx.room.RoomProcessor"])
@Suppress(names = ["UNCHECKED_CAST", "DEPRECATION", "REDUNDANT_PROJECTION", "REMOVAL"])
public class SneakerDao_Impl(
  __db: RoomDatabase,
) : SneakerDao {
  private val __db: RoomDatabase

  private val __insertAdapterOfSneakerEntity: EntityInsertAdapter<SneakerEntity>

  private val __deleteAdapterOfSneakerEntity: EntityDeleteOrUpdateAdapter<SneakerEntity>

  private val __updateAdapterOfSneakerEntity: EntityDeleteOrUpdateAdapter<SneakerEntity>
  init {
    this.__db = __db
    this.__insertAdapterOfSneakerEntity = object : EntityInsertAdapter<SneakerEntity>() {
      protected override fun createQuery(): String = "INSERT OR ABORT INTO `sneakers` (`id`,`factionId`,`rarity`,`variant`,`level`,`mintNumber`,`luck`,`comfort`,`durability`,`equipped`,`acquiredAt`,`serverId`,`origin`,`efficiencyBps`,`comfortBps`,`durabilityPts`,`maxLevel`,`status`,`chainState`,`canWithdraw`,`serverUpgradeCost`,`repairCostPerPoint`,`genesisNo`,`modelId`,`tokenId`,`durabilityNow`,`durabilityMax`,`durabilityBase`,`efficiencyBaseBps`,`comfortBaseBps`,`pointsTotal`,`pointsFree`,`pointsEfficiency`,`pointsComfort`,`pointsDurability`,`pointsRespec`,`repairCostPerUnit`) VALUES (nullif(?, 0),?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?)"

      protected override fun bind(statement: SQLiteStatement, entity: SneakerEntity) {
        statement.bindLong(1, entity.id)
        statement.bindText(2, entity.factionId)
        statement.bindText(3, entity.rarity)
        statement.bindLong(4, entity.variant.toLong())
        statement.bindLong(5, entity.level.toLong())
        statement.bindLong(6, entity.mintNumber.toLong())
        statement.bindDouble(7, entity.luck)
        statement.bindDouble(8, entity.comfort)
        statement.bindLong(9, entity.durability.toLong())
        val _tmp: Int = if (entity.equipped) 1 else 0
        statement.bindLong(10, _tmp.toLong())
        statement.bindLong(11, entity.acquiredAt)
        statement.bindLong(12, entity.serverId)
        statement.bindText(13, entity.origin)
        statement.bindLong(14, entity.efficiencyBps.toLong())
        statement.bindLong(15, entity.comfortBps.toLong())
        statement.bindDouble(16, entity.durabilityPts)
        statement.bindLong(17, entity.maxLevel.toLong())
        statement.bindText(18, entity.status)
        statement.bindText(19, entity.chainState)
        val _tmp_1: Int = if (entity.canWithdraw) 1 else 0
        statement.bindLong(20, _tmp_1.toLong())
        statement.bindDouble(21, entity.serverUpgradeCost)
        statement.bindDouble(22, entity.repairCostPerPoint)
        statement.bindLong(23, entity.genesisNo.toLong())
        statement.bindLong(24, entity.modelId.toLong())
        statement.bindLong(25, entity.tokenId)
        statement.bindLong(26, entity.durabilityNow.toLong())
        statement.bindLong(27, entity.durabilityMax.toLong())
        statement.bindLong(28, entity.durabilityBase.toLong())
        statement.bindLong(29, entity.efficiencyBaseBps.toLong())
        statement.bindLong(30, entity.comfortBaseBps.toLong())
        statement.bindLong(31, entity.pointsTotal.toLong())
        statement.bindLong(32, entity.pointsFree.toLong())
        statement.bindLong(33, entity.pointsEfficiency.toLong())
        statement.bindLong(34, entity.pointsComfort.toLong())
        statement.bindLong(35, entity.pointsDurability.toLong())
        val _tmp_2: Int = if (entity.pointsRespec) 1 else 0
        statement.bindLong(36, _tmp_2.toLong())
        statement.bindDouble(37, entity.repairCostPerUnit)
      }
    }
    this.__deleteAdapterOfSneakerEntity = object : EntityDeleteOrUpdateAdapter<SneakerEntity>() {
      protected override fun createQuery(): String = "DELETE FROM `sneakers` WHERE `id` = ?"

      protected override fun bind(statement: SQLiteStatement, entity: SneakerEntity) {
        statement.bindLong(1, entity.id)
      }
    }
    this.__updateAdapterOfSneakerEntity = object : EntityDeleteOrUpdateAdapter<SneakerEntity>() {
      protected override fun createQuery(): String = "UPDATE OR ABORT `sneakers` SET `id` = ?,`factionId` = ?,`rarity` = ?,`variant` = ?,`level` = ?,`mintNumber` = ?,`luck` = ?,`comfort` = ?,`durability` = ?,`equipped` = ?,`acquiredAt` = ?,`serverId` = ?,`origin` = ?,`efficiencyBps` = ?,`comfortBps` = ?,`durabilityPts` = ?,`maxLevel` = ?,`status` = ?,`chainState` = ?,`canWithdraw` = ?,`serverUpgradeCost` = ?,`repairCostPerPoint` = ?,`genesisNo` = ?,`modelId` = ?,`tokenId` = ?,`durabilityNow` = ?,`durabilityMax` = ?,`durabilityBase` = ?,`efficiencyBaseBps` = ?,`comfortBaseBps` = ?,`pointsTotal` = ?,`pointsFree` = ?,`pointsEfficiency` = ?,`pointsComfort` = ?,`pointsDurability` = ?,`pointsRespec` = ?,`repairCostPerUnit` = ? WHERE `id` = ?"

      protected override fun bind(statement: SQLiteStatement, entity: SneakerEntity) {
        statement.bindLong(1, entity.id)
        statement.bindText(2, entity.factionId)
        statement.bindText(3, entity.rarity)
        statement.bindLong(4, entity.variant.toLong())
        statement.bindLong(5, entity.level.toLong())
        statement.bindLong(6, entity.mintNumber.toLong())
        statement.bindDouble(7, entity.luck)
        statement.bindDouble(8, entity.comfort)
        statement.bindLong(9, entity.durability.toLong())
        val _tmp: Int = if (entity.equipped) 1 else 0
        statement.bindLong(10, _tmp.toLong())
        statement.bindLong(11, entity.acquiredAt)
        statement.bindLong(12, entity.serverId)
        statement.bindText(13, entity.origin)
        statement.bindLong(14, entity.efficiencyBps.toLong())
        statement.bindLong(15, entity.comfortBps.toLong())
        statement.bindDouble(16, entity.durabilityPts)
        statement.bindLong(17, entity.maxLevel.toLong())
        statement.bindText(18, entity.status)
        statement.bindText(19, entity.chainState)
        val _tmp_1: Int = if (entity.canWithdraw) 1 else 0
        statement.bindLong(20, _tmp_1.toLong())
        statement.bindDouble(21, entity.serverUpgradeCost)
        statement.bindDouble(22, entity.repairCostPerPoint)
        statement.bindLong(23, entity.genesisNo.toLong())
        statement.bindLong(24, entity.modelId.toLong())
        statement.bindLong(25, entity.tokenId)
        statement.bindLong(26, entity.durabilityNow.toLong())
        statement.bindLong(27, entity.durabilityMax.toLong())
        statement.bindLong(28, entity.durabilityBase.toLong())
        statement.bindLong(29, entity.efficiencyBaseBps.toLong())
        statement.bindLong(30, entity.comfortBaseBps.toLong())
        statement.bindLong(31, entity.pointsTotal.toLong())
        statement.bindLong(32, entity.pointsFree.toLong())
        statement.bindLong(33, entity.pointsEfficiency.toLong())
        statement.bindLong(34, entity.pointsComfort.toLong())
        statement.bindLong(35, entity.pointsDurability.toLong())
        val _tmp_2: Int = if (entity.pointsRespec) 1 else 0
        statement.bindLong(36, _tmp_2.toLong())
        statement.bindDouble(37, entity.repairCostPerUnit)
        statement.bindLong(38, entity.id)
      }
    }
  }

  public override suspend fun insertAll(rows: List<SneakerEntity>): Unit = performSuspending(__db, false, true) { _connection ->
    __insertAdapterOfSneakerEntity.insert(_connection, rows)
  }

  public override suspend fun insert(sneaker: SneakerEntity): Long = performSuspending(__db, false, true) { _connection ->
    val _result: Long = __insertAdapterOfSneakerEntity.insertAndReturnId(_connection, sneaker)
    _result
  }

  public override suspend fun delete(sneaker: SneakerEntity): Unit = performSuspending(__db, false, true) { _connection ->
    __deleteAdapterOfSneakerEntity.handle(_connection, sneaker)
  }

  public override suspend fun update(sneaker: SneakerEntity): Unit = performSuspending(__db, false, true) { _connection ->
    __updateAdapterOfSneakerEntity.handle(_connection, sneaker)
  }

  public override fun observeAll(): Flow<List<SneakerEntity>> {
    val _sql: String = """
        |
        |        SELECT * FROM sneakers
        |        ORDER BY equipped DESC,
        |            CASE rarity
        |                WHEN 'LEGENDARY' THEN 3
        |                WHEN 'EPIC' THEN 2
        |                WHEN 'RARE' THEN 1
        |                ELSE 0
        |            END DESC,
        |            acquiredAt DESC
        |        
        """.trimMargin()
    return createFlow(__db, false, arrayOf("sneakers")) { _connection ->
      val _stmt: SQLiteStatement = _connection.prepare(_sql)
      try {
        val _columnIndexOfId: Int = getColumnIndexOrThrow(_stmt, "id")
        val _columnIndexOfFactionId: Int = getColumnIndexOrThrow(_stmt, "factionId")
        val _columnIndexOfRarity: Int = getColumnIndexOrThrow(_stmt, "rarity")
        val _columnIndexOfVariant: Int = getColumnIndexOrThrow(_stmt, "variant")
        val _columnIndexOfLevel: Int = getColumnIndexOrThrow(_stmt, "level")
        val _columnIndexOfMintNumber: Int = getColumnIndexOrThrow(_stmt, "mintNumber")
        val _columnIndexOfLuck: Int = getColumnIndexOrThrow(_stmt, "luck")
        val _columnIndexOfComfort: Int = getColumnIndexOrThrow(_stmt, "comfort")
        val _columnIndexOfDurability: Int = getColumnIndexOrThrow(_stmt, "durability")
        val _columnIndexOfEquipped: Int = getColumnIndexOrThrow(_stmt, "equipped")
        val _columnIndexOfAcquiredAt: Int = getColumnIndexOrThrow(_stmt, "acquiredAt")
        val _columnIndexOfServerId: Int = getColumnIndexOrThrow(_stmt, "serverId")
        val _columnIndexOfOrigin: Int = getColumnIndexOrThrow(_stmt, "origin")
        val _columnIndexOfEfficiencyBps: Int = getColumnIndexOrThrow(_stmt, "efficiencyBps")
        val _columnIndexOfComfortBps: Int = getColumnIndexOrThrow(_stmt, "comfortBps")
        val _columnIndexOfDurabilityPts: Int = getColumnIndexOrThrow(_stmt, "durabilityPts")
        val _columnIndexOfMaxLevel: Int = getColumnIndexOrThrow(_stmt, "maxLevel")
        val _columnIndexOfStatus: Int = getColumnIndexOrThrow(_stmt, "status")
        val _columnIndexOfChainState: Int = getColumnIndexOrThrow(_stmt, "chainState")
        val _columnIndexOfCanWithdraw: Int = getColumnIndexOrThrow(_stmt, "canWithdraw")
        val _columnIndexOfServerUpgradeCost: Int = getColumnIndexOrThrow(_stmt, "serverUpgradeCost")
        val _columnIndexOfRepairCostPerPoint: Int = getColumnIndexOrThrow(_stmt, "repairCostPerPoint")
        val _columnIndexOfGenesisNo: Int = getColumnIndexOrThrow(_stmt, "genesisNo")
        val _columnIndexOfModelId: Int = getColumnIndexOrThrow(_stmt, "modelId")
        val _columnIndexOfTokenId: Int = getColumnIndexOrThrow(_stmt, "tokenId")
        val _columnIndexOfDurabilityNow: Int = getColumnIndexOrThrow(_stmt, "durabilityNow")
        val _columnIndexOfDurabilityMax: Int = getColumnIndexOrThrow(_stmt, "durabilityMax")
        val _columnIndexOfDurabilityBase: Int = getColumnIndexOrThrow(_stmt, "durabilityBase")
        val _columnIndexOfEfficiencyBaseBps: Int = getColumnIndexOrThrow(_stmt, "efficiencyBaseBps")
        val _columnIndexOfComfortBaseBps: Int = getColumnIndexOrThrow(_stmt, "comfortBaseBps")
        val _columnIndexOfPointsTotal: Int = getColumnIndexOrThrow(_stmt, "pointsTotal")
        val _columnIndexOfPointsFree: Int = getColumnIndexOrThrow(_stmt, "pointsFree")
        val _columnIndexOfPointsEfficiency: Int = getColumnIndexOrThrow(_stmt, "pointsEfficiency")
        val _columnIndexOfPointsComfort: Int = getColumnIndexOrThrow(_stmt, "pointsComfort")
        val _columnIndexOfPointsDurability: Int = getColumnIndexOrThrow(_stmt, "pointsDurability")
        val _columnIndexOfPointsRespec: Int = getColumnIndexOrThrow(_stmt, "pointsRespec")
        val _columnIndexOfRepairCostPerUnit: Int = getColumnIndexOrThrow(_stmt, "repairCostPerUnit")
        val _result: MutableList<SneakerEntity> = mutableListOf()
        while (_stmt.step()) {
          val _item: SneakerEntity
          val _tmpId: Long
          _tmpId = _stmt.getLong(_columnIndexOfId)
          val _tmpFactionId: String
          _tmpFactionId = _stmt.getText(_columnIndexOfFactionId)
          val _tmpRarity: String
          _tmpRarity = _stmt.getText(_columnIndexOfRarity)
          val _tmpVariant: Int
          _tmpVariant = _stmt.getLong(_columnIndexOfVariant).toInt()
          val _tmpLevel: Int
          _tmpLevel = _stmt.getLong(_columnIndexOfLevel).toInt()
          val _tmpMintNumber: Int
          _tmpMintNumber = _stmt.getLong(_columnIndexOfMintNumber).toInt()
          val _tmpLuck: Double
          _tmpLuck = _stmt.getDouble(_columnIndexOfLuck)
          val _tmpComfort: Double
          _tmpComfort = _stmt.getDouble(_columnIndexOfComfort)
          val _tmpDurability: Int
          _tmpDurability = _stmt.getLong(_columnIndexOfDurability).toInt()
          val _tmpEquipped: Boolean
          val _tmp: Int
          _tmp = _stmt.getLong(_columnIndexOfEquipped).toInt()
          _tmpEquipped = _tmp != 0
          val _tmpAcquiredAt: Long
          _tmpAcquiredAt = _stmt.getLong(_columnIndexOfAcquiredAt)
          val _tmpServerId: Long
          _tmpServerId = _stmt.getLong(_columnIndexOfServerId)
          val _tmpOrigin: String
          _tmpOrigin = _stmt.getText(_columnIndexOfOrigin)
          val _tmpEfficiencyBps: Int
          _tmpEfficiencyBps = _stmt.getLong(_columnIndexOfEfficiencyBps).toInt()
          val _tmpComfortBps: Int
          _tmpComfortBps = _stmt.getLong(_columnIndexOfComfortBps).toInt()
          val _tmpDurabilityPts: Double
          _tmpDurabilityPts = _stmt.getDouble(_columnIndexOfDurabilityPts)
          val _tmpMaxLevel: Int
          _tmpMaxLevel = _stmt.getLong(_columnIndexOfMaxLevel).toInt()
          val _tmpStatus: String
          _tmpStatus = _stmt.getText(_columnIndexOfStatus)
          val _tmpChainState: String
          _tmpChainState = _stmt.getText(_columnIndexOfChainState)
          val _tmpCanWithdraw: Boolean
          val _tmp_1: Int
          _tmp_1 = _stmt.getLong(_columnIndexOfCanWithdraw).toInt()
          _tmpCanWithdraw = _tmp_1 != 0
          val _tmpServerUpgradeCost: Double
          _tmpServerUpgradeCost = _stmt.getDouble(_columnIndexOfServerUpgradeCost)
          val _tmpRepairCostPerPoint: Double
          _tmpRepairCostPerPoint = _stmt.getDouble(_columnIndexOfRepairCostPerPoint)
          val _tmpGenesisNo: Int
          _tmpGenesisNo = _stmt.getLong(_columnIndexOfGenesisNo).toInt()
          val _tmpModelId: Int
          _tmpModelId = _stmt.getLong(_columnIndexOfModelId).toInt()
          val _tmpTokenId: Long
          _tmpTokenId = _stmt.getLong(_columnIndexOfTokenId)
          val _tmpDurabilityNow: Int
          _tmpDurabilityNow = _stmt.getLong(_columnIndexOfDurabilityNow).toInt()
          val _tmpDurabilityMax: Int
          _tmpDurabilityMax = _stmt.getLong(_columnIndexOfDurabilityMax).toInt()
          val _tmpDurabilityBase: Int
          _tmpDurabilityBase = _stmt.getLong(_columnIndexOfDurabilityBase).toInt()
          val _tmpEfficiencyBaseBps: Int
          _tmpEfficiencyBaseBps = _stmt.getLong(_columnIndexOfEfficiencyBaseBps).toInt()
          val _tmpComfortBaseBps: Int
          _tmpComfortBaseBps = _stmt.getLong(_columnIndexOfComfortBaseBps).toInt()
          val _tmpPointsTotal: Int
          _tmpPointsTotal = _stmt.getLong(_columnIndexOfPointsTotal).toInt()
          val _tmpPointsFree: Int
          _tmpPointsFree = _stmt.getLong(_columnIndexOfPointsFree).toInt()
          val _tmpPointsEfficiency: Int
          _tmpPointsEfficiency = _stmt.getLong(_columnIndexOfPointsEfficiency).toInt()
          val _tmpPointsComfort: Int
          _tmpPointsComfort = _stmt.getLong(_columnIndexOfPointsComfort).toInt()
          val _tmpPointsDurability: Int
          _tmpPointsDurability = _stmt.getLong(_columnIndexOfPointsDurability).toInt()
          val _tmpPointsRespec: Boolean
          val _tmp_2: Int
          _tmp_2 = _stmt.getLong(_columnIndexOfPointsRespec).toInt()
          _tmpPointsRespec = _tmp_2 != 0
          val _tmpRepairCostPerUnit: Double
          _tmpRepairCostPerUnit = _stmt.getDouble(_columnIndexOfRepairCostPerUnit)
          _item = SneakerEntity(_tmpId,_tmpFactionId,_tmpRarity,_tmpVariant,_tmpLevel,_tmpMintNumber,_tmpLuck,_tmpComfort,_tmpDurability,_tmpEquipped,_tmpAcquiredAt,_tmpServerId,_tmpOrigin,_tmpEfficiencyBps,_tmpComfortBps,_tmpDurabilityPts,_tmpMaxLevel,_tmpStatus,_tmpChainState,_tmpCanWithdraw,_tmpServerUpgradeCost,_tmpRepairCostPerPoint,_tmpGenesisNo,_tmpModelId,_tmpTokenId,_tmpDurabilityNow,_tmpDurabilityMax,_tmpDurabilityBase,_tmpEfficiencyBaseBps,_tmpComfortBaseBps,_tmpPointsTotal,_tmpPointsFree,_tmpPointsEfficiency,_tmpPointsComfort,_tmpPointsDurability,_tmpPointsRespec,_tmpRepairCostPerUnit)
          _result.add(_item)
        }
        _result
      } finally {
        _stmt.close()
      }
    }
  }

  public override fun observeEquipped(): Flow<SneakerEntity?> {
    val _sql: String = "SELECT * FROM sneakers WHERE equipped = 1 LIMIT 1"
    return createFlow(__db, false, arrayOf("sneakers")) { _connection ->
      val _stmt: SQLiteStatement = _connection.prepare(_sql)
      try {
        val _columnIndexOfId: Int = getColumnIndexOrThrow(_stmt, "id")
        val _columnIndexOfFactionId: Int = getColumnIndexOrThrow(_stmt, "factionId")
        val _columnIndexOfRarity: Int = getColumnIndexOrThrow(_stmt, "rarity")
        val _columnIndexOfVariant: Int = getColumnIndexOrThrow(_stmt, "variant")
        val _columnIndexOfLevel: Int = getColumnIndexOrThrow(_stmt, "level")
        val _columnIndexOfMintNumber: Int = getColumnIndexOrThrow(_stmt, "mintNumber")
        val _columnIndexOfLuck: Int = getColumnIndexOrThrow(_stmt, "luck")
        val _columnIndexOfComfort: Int = getColumnIndexOrThrow(_stmt, "comfort")
        val _columnIndexOfDurability: Int = getColumnIndexOrThrow(_stmt, "durability")
        val _columnIndexOfEquipped: Int = getColumnIndexOrThrow(_stmt, "equipped")
        val _columnIndexOfAcquiredAt: Int = getColumnIndexOrThrow(_stmt, "acquiredAt")
        val _columnIndexOfServerId: Int = getColumnIndexOrThrow(_stmt, "serverId")
        val _columnIndexOfOrigin: Int = getColumnIndexOrThrow(_stmt, "origin")
        val _columnIndexOfEfficiencyBps: Int = getColumnIndexOrThrow(_stmt, "efficiencyBps")
        val _columnIndexOfComfortBps: Int = getColumnIndexOrThrow(_stmt, "comfortBps")
        val _columnIndexOfDurabilityPts: Int = getColumnIndexOrThrow(_stmt, "durabilityPts")
        val _columnIndexOfMaxLevel: Int = getColumnIndexOrThrow(_stmt, "maxLevel")
        val _columnIndexOfStatus: Int = getColumnIndexOrThrow(_stmt, "status")
        val _columnIndexOfChainState: Int = getColumnIndexOrThrow(_stmt, "chainState")
        val _columnIndexOfCanWithdraw: Int = getColumnIndexOrThrow(_stmt, "canWithdraw")
        val _columnIndexOfServerUpgradeCost: Int = getColumnIndexOrThrow(_stmt, "serverUpgradeCost")
        val _columnIndexOfRepairCostPerPoint: Int = getColumnIndexOrThrow(_stmt, "repairCostPerPoint")
        val _columnIndexOfGenesisNo: Int = getColumnIndexOrThrow(_stmt, "genesisNo")
        val _columnIndexOfModelId: Int = getColumnIndexOrThrow(_stmt, "modelId")
        val _columnIndexOfTokenId: Int = getColumnIndexOrThrow(_stmt, "tokenId")
        val _columnIndexOfDurabilityNow: Int = getColumnIndexOrThrow(_stmt, "durabilityNow")
        val _columnIndexOfDurabilityMax: Int = getColumnIndexOrThrow(_stmt, "durabilityMax")
        val _columnIndexOfDurabilityBase: Int = getColumnIndexOrThrow(_stmt, "durabilityBase")
        val _columnIndexOfEfficiencyBaseBps: Int = getColumnIndexOrThrow(_stmt, "efficiencyBaseBps")
        val _columnIndexOfComfortBaseBps: Int = getColumnIndexOrThrow(_stmt, "comfortBaseBps")
        val _columnIndexOfPointsTotal: Int = getColumnIndexOrThrow(_stmt, "pointsTotal")
        val _columnIndexOfPointsFree: Int = getColumnIndexOrThrow(_stmt, "pointsFree")
        val _columnIndexOfPointsEfficiency: Int = getColumnIndexOrThrow(_stmt, "pointsEfficiency")
        val _columnIndexOfPointsComfort: Int = getColumnIndexOrThrow(_stmt, "pointsComfort")
        val _columnIndexOfPointsDurability: Int = getColumnIndexOrThrow(_stmt, "pointsDurability")
        val _columnIndexOfPointsRespec: Int = getColumnIndexOrThrow(_stmt, "pointsRespec")
        val _columnIndexOfRepairCostPerUnit: Int = getColumnIndexOrThrow(_stmt, "repairCostPerUnit")
        val _result: SneakerEntity?
        if (_stmt.step()) {
          val _tmpId: Long
          _tmpId = _stmt.getLong(_columnIndexOfId)
          val _tmpFactionId: String
          _tmpFactionId = _stmt.getText(_columnIndexOfFactionId)
          val _tmpRarity: String
          _tmpRarity = _stmt.getText(_columnIndexOfRarity)
          val _tmpVariant: Int
          _tmpVariant = _stmt.getLong(_columnIndexOfVariant).toInt()
          val _tmpLevel: Int
          _tmpLevel = _stmt.getLong(_columnIndexOfLevel).toInt()
          val _tmpMintNumber: Int
          _tmpMintNumber = _stmt.getLong(_columnIndexOfMintNumber).toInt()
          val _tmpLuck: Double
          _tmpLuck = _stmt.getDouble(_columnIndexOfLuck)
          val _tmpComfort: Double
          _tmpComfort = _stmt.getDouble(_columnIndexOfComfort)
          val _tmpDurability: Int
          _tmpDurability = _stmt.getLong(_columnIndexOfDurability).toInt()
          val _tmpEquipped: Boolean
          val _tmp: Int
          _tmp = _stmt.getLong(_columnIndexOfEquipped).toInt()
          _tmpEquipped = _tmp != 0
          val _tmpAcquiredAt: Long
          _tmpAcquiredAt = _stmt.getLong(_columnIndexOfAcquiredAt)
          val _tmpServerId: Long
          _tmpServerId = _stmt.getLong(_columnIndexOfServerId)
          val _tmpOrigin: String
          _tmpOrigin = _stmt.getText(_columnIndexOfOrigin)
          val _tmpEfficiencyBps: Int
          _tmpEfficiencyBps = _stmt.getLong(_columnIndexOfEfficiencyBps).toInt()
          val _tmpComfortBps: Int
          _tmpComfortBps = _stmt.getLong(_columnIndexOfComfortBps).toInt()
          val _tmpDurabilityPts: Double
          _tmpDurabilityPts = _stmt.getDouble(_columnIndexOfDurabilityPts)
          val _tmpMaxLevel: Int
          _tmpMaxLevel = _stmt.getLong(_columnIndexOfMaxLevel).toInt()
          val _tmpStatus: String
          _tmpStatus = _stmt.getText(_columnIndexOfStatus)
          val _tmpChainState: String
          _tmpChainState = _stmt.getText(_columnIndexOfChainState)
          val _tmpCanWithdraw: Boolean
          val _tmp_1: Int
          _tmp_1 = _stmt.getLong(_columnIndexOfCanWithdraw).toInt()
          _tmpCanWithdraw = _tmp_1 != 0
          val _tmpServerUpgradeCost: Double
          _tmpServerUpgradeCost = _stmt.getDouble(_columnIndexOfServerUpgradeCost)
          val _tmpRepairCostPerPoint: Double
          _tmpRepairCostPerPoint = _stmt.getDouble(_columnIndexOfRepairCostPerPoint)
          val _tmpGenesisNo: Int
          _tmpGenesisNo = _stmt.getLong(_columnIndexOfGenesisNo).toInt()
          val _tmpModelId: Int
          _tmpModelId = _stmt.getLong(_columnIndexOfModelId).toInt()
          val _tmpTokenId: Long
          _tmpTokenId = _stmt.getLong(_columnIndexOfTokenId)
          val _tmpDurabilityNow: Int
          _tmpDurabilityNow = _stmt.getLong(_columnIndexOfDurabilityNow).toInt()
          val _tmpDurabilityMax: Int
          _tmpDurabilityMax = _stmt.getLong(_columnIndexOfDurabilityMax).toInt()
          val _tmpDurabilityBase: Int
          _tmpDurabilityBase = _stmt.getLong(_columnIndexOfDurabilityBase).toInt()
          val _tmpEfficiencyBaseBps: Int
          _tmpEfficiencyBaseBps = _stmt.getLong(_columnIndexOfEfficiencyBaseBps).toInt()
          val _tmpComfortBaseBps: Int
          _tmpComfortBaseBps = _stmt.getLong(_columnIndexOfComfortBaseBps).toInt()
          val _tmpPointsTotal: Int
          _tmpPointsTotal = _stmt.getLong(_columnIndexOfPointsTotal).toInt()
          val _tmpPointsFree: Int
          _tmpPointsFree = _stmt.getLong(_columnIndexOfPointsFree).toInt()
          val _tmpPointsEfficiency: Int
          _tmpPointsEfficiency = _stmt.getLong(_columnIndexOfPointsEfficiency).toInt()
          val _tmpPointsComfort: Int
          _tmpPointsComfort = _stmt.getLong(_columnIndexOfPointsComfort).toInt()
          val _tmpPointsDurability: Int
          _tmpPointsDurability = _stmt.getLong(_columnIndexOfPointsDurability).toInt()
          val _tmpPointsRespec: Boolean
          val _tmp_2: Int
          _tmp_2 = _stmt.getLong(_columnIndexOfPointsRespec).toInt()
          _tmpPointsRespec = _tmp_2 != 0
          val _tmpRepairCostPerUnit: Double
          _tmpRepairCostPerUnit = _stmt.getDouble(_columnIndexOfRepairCostPerUnit)
          _result = SneakerEntity(_tmpId,_tmpFactionId,_tmpRarity,_tmpVariant,_tmpLevel,_tmpMintNumber,_tmpLuck,_tmpComfort,_tmpDurability,_tmpEquipped,_tmpAcquiredAt,_tmpServerId,_tmpOrigin,_tmpEfficiencyBps,_tmpComfortBps,_tmpDurabilityPts,_tmpMaxLevel,_tmpStatus,_tmpChainState,_tmpCanWithdraw,_tmpServerUpgradeCost,_tmpRepairCostPerPoint,_tmpGenesisNo,_tmpModelId,_tmpTokenId,_tmpDurabilityNow,_tmpDurabilityMax,_tmpDurabilityBase,_tmpEfficiencyBaseBps,_tmpComfortBaseBps,_tmpPointsTotal,_tmpPointsFree,_tmpPointsEfficiency,_tmpPointsComfort,_tmpPointsDurability,_tmpPointsRespec,_tmpRepairCostPerUnit)
        } else {
          _result = null
        }
        _result
      } finally {
        _stmt.close()
      }
    }
  }

  public override suspend fun equippedNow(): SneakerEntity? {
    val _sql: String = "SELECT * FROM sneakers WHERE equipped = 1 LIMIT 1"
    return performSuspending(__db, true, false) { _connection ->
      val _stmt: SQLiteStatement = _connection.prepare(_sql)
      try {
        val _columnIndexOfId: Int = getColumnIndexOrThrow(_stmt, "id")
        val _columnIndexOfFactionId: Int = getColumnIndexOrThrow(_stmt, "factionId")
        val _columnIndexOfRarity: Int = getColumnIndexOrThrow(_stmt, "rarity")
        val _columnIndexOfVariant: Int = getColumnIndexOrThrow(_stmt, "variant")
        val _columnIndexOfLevel: Int = getColumnIndexOrThrow(_stmt, "level")
        val _columnIndexOfMintNumber: Int = getColumnIndexOrThrow(_stmt, "mintNumber")
        val _columnIndexOfLuck: Int = getColumnIndexOrThrow(_stmt, "luck")
        val _columnIndexOfComfort: Int = getColumnIndexOrThrow(_stmt, "comfort")
        val _columnIndexOfDurability: Int = getColumnIndexOrThrow(_stmt, "durability")
        val _columnIndexOfEquipped: Int = getColumnIndexOrThrow(_stmt, "equipped")
        val _columnIndexOfAcquiredAt: Int = getColumnIndexOrThrow(_stmt, "acquiredAt")
        val _columnIndexOfServerId: Int = getColumnIndexOrThrow(_stmt, "serverId")
        val _columnIndexOfOrigin: Int = getColumnIndexOrThrow(_stmt, "origin")
        val _columnIndexOfEfficiencyBps: Int = getColumnIndexOrThrow(_stmt, "efficiencyBps")
        val _columnIndexOfComfortBps: Int = getColumnIndexOrThrow(_stmt, "comfortBps")
        val _columnIndexOfDurabilityPts: Int = getColumnIndexOrThrow(_stmt, "durabilityPts")
        val _columnIndexOfMaxLevel: Int = getColumnIndexOrThrow(_stmt, "maxLevel")
        val _columnIndexOfStatus: Int = getColumnIndexOrThrow(_stmt, "status")
        val _columnIndexOfChainState: Int = getColumnIndexOrThrow(_stmt, "chainState")
        val _columnIndexOfCanWithdraw: Int = getColumnIndexOrThrow(_stmt, "canWithdraw")
        val _columnIndexOfServerUpgradeCost: Int = getColumnIndexOrThrow(_stmt, "serverUpgradeCost")
        val _columnIndexOfRepairCostPerPoint: Int = getColumnIndexOrThrow(_stmt, "repairCostPerPoint")
        val _columnIndexOfGenesisNo: Int = getColumnIndexOrThrow(_stmt, "genesisNo")
        val _columnIndexOfModelId: Int = getColumnIndexOrThrow(_stmt, "modelId")
        val _columnIndexOfTokenId: Int = getColumnIndexOrThrow(_stmt, "tokenId")
        val _columnIndexOfDurabilityNow: Int = getColumnIndexOrThrow(_stmt, "durabilityNow")
        val _columnIndexOfDurabilityMax: Int = getColumnIndexOrThrow(_stmt, "durabilityMax")
        val _columnIndexOfDurabilityBase: Int = getColumnIndexOrThrow(_stmt, "durabilityBase")
        val _columnIndexOfEfficiencyBaseBps: Int = getColumnIndexOrThrow(_stmt, "efficiencyBaseBps")
        val _columnIndexOfComfortBaseBps: Int = getColumnIndexOrThrow(_stmt, "comfortBaseBps")
        val _columnIndexOfPointsTotal: Int = getColumnIndexOrThrow(_stmt, "pointsTotal")
        val _columnIndexOfPointsFree: Int = getColumnIndexOrThrow(_stmt, "pointsFree")
        val _columnIndexOfPointsEfficiency: Int = getColumnIndexOrThrow(_stmt, "pointsEfficiency")
        val _columnIndexOfPointsComfort: Int = getColumnIndexOrThrow(_stmt, "pointsComfort")
        val _columnIndexOfPointsDurability: Int = getColumnIndexOrThrow(_stmt, "pointsDurability")
        val _columnIndexOfPointsRespec: Int = getColumnIndexOrThrow(_stmt, "pointsRespec")
        val _columnIndexOfRepairCostPerUnit: Int = getColumnIndexOrThrow(_stmt, "repairCostPerUnit")
        val _result: SneakerEntity?
        if (_stmt.step()) {
          val _tmpId: Long
          _tmpId = _stmt.getLong(_columnIndexOfId)
          val _tmpFactionId: String
          _tmpFactionId = _stmt.getText(_columnIndexOfFactionId)
          val _tmpRarity: String
          _tmpRarity = _stmt.getText(_columnIndexOfRarity)
          val _tmpVariant: Int
          _tmpVariant = _stmt.getLong(_columnIndexOfVariant).toInt()
          val _tmpLevel: Int
          _tmpLevel = _stmt.getLong(_columnIndexOfLevel).toInt()
          val _tmpMintNumber: Int
          _tmpMintNumber = _stmt.getLong(_columnIndexOfMintNumber).toInt()
          val _tmpLuck: Double
          _tmpLuck = _stmt.getDouble(_columnIndexOfLuck)
          val _tmpComfort: Double
          _tmpComfort = _stmt.getDouble(_columnIndexOfComfort)
          val _tmpDurability: Int
          _tmpDurability = _stmt.getLong(_columnIndexOfDurability).toInt()
          val _tmpEquipped: Boolean
          val _tmp: Int
          _tmp = _stmt.getLong(_columnIndexOfEquipped).toInt()
          _tmpEquipped = _tmp != 0
          val _tmpAcquiredAt: Long
          _tmpAcquiredAt = _stmt.getLong(_columnIndexOfAcquiredAt)
          val _tmpServerId: Long
          _tmpServerId = _stmt.getLong(_columnIndexOfServerId)
          val _tmpOrigin: String
          _tmpOrigin = _stmt.getText(_columnIndexOfOrigin)
          val _tmpEfficiencyBps: Int
          _tmpEfficiencyBps = _stmt.getLong(_columnIndexOfEfficiencyBps).toInt()
          val _tmpComfortBps: Int
          _tmpComfortBps = _stmt.getLong(_columnIndexOfComfortBps).toInt()
          val _tmpDurabilityPts: Double
          _tmpDurabilityPts = _stmt.getDouble(_columnIndexOfDurabilityPts)
          val _tmpMaxLevel: Int
          _tmpMaxLevel = _stmt.getLong(_columnIndexOfMaxLevel).toInt()
          val _tmpStatus: String
          _tmpStatus = _stmt.getText(_columnIndexOfStatus)
          val _tmpChainState: String
          _tmpChainState = _stmt.getText(_columnIndexOfChainState)
          val _tmpCanWithdraw: Boolean
          val _tmp_1: Int
          _tmp_1 = _stmt.getLong(_columnIndexOfCanWithdraw).toInt()
          _tmpCanWithdraw = _tmp_1 != 0
          val _tmpServerUpgradeCost: Double
          _tmpServerUpgradeCost = _stmt.getDouble(_columnIndexOfServerUpgradeCost)
          val _tmpRepairCostPerPoint: Double
          _tmpRepairCostPerPoint = _stmt.getDouble(_columnIndexOfRepairCostPerPoint)
          val _tmpGenesisNo: Int
          _tmpGenesisNo = _stmt.getLong(_columnIndexOfGenesisNo).toInt()
          val _tmpModelId: Int
          _tmpModelId = _stmt.getLong(_columnIndexOfModelId).toInt()
          val _tmpTokenId: Long
          _tmpTokenId = _stmt.getLong(_columnIndexOfTokenId)
          val _tmpDurabilityNow: Int
          _tmpDurabilityNow = _stmt.getLong(_columnIndexOfDurabilityNow).toInt()
          val _tmpDurabilityMax: Int
          _tmpDurabilityMax = _stmt.getLong(_columnIndexOfDurabilityMax).toInt()
          val _tmpDurabilityBase: Int
          _tmpDurabilityBase = _stmt.getLong(_columnIndexOfDurabilityBase).toInt()
          val _tmpEfficiencyBaseBps: Int
          _tmpEfficiencyBaseBps = _stmt.getLong(_columnIndexOfEfficiencyBaseBps).toInt()
          val _tmpComfortBaseBps: Int
          _tmpComfortBaseBps = _stmt.getLong(_columnIndexOfComfortBaseBps).toInt()
          val _tmpPointsTotal: Int
          _tmpPointsTotal = _stmt.getLong(_columnIndexOfPointsTotal).toInt()
          val _tmpPointsFree: Int
          _tmpPointsFree = _stmt.getLong(_columnIndexOfPointsFree).toInt()
          val _tmpPointsEfficiency: Int
          _tmpPointsEfficiency = _stmt.getLong(_columnIndexOfPointsEfficiency).toInt()
          val _tmpPointsComfort: Int
          _tmpPointsComfort = _stmt.getLong(_columnIndexOfPointsComfort).toInt()
          val _tmpPointsDurability: Int
          _tmpPointsDurability = _stmt.getLong(_columnIndexOfPointsDurability).toInt()
          val _tmpPointsRespec: Boolean
          val _tmp_2: Int
          _tmp_2 = _stmt.getLong(_columnIndexOfPointsRespec).toInt()
          _tmpPointsRespec = _tmp_2 != 0
          val _tmpRepairCostPerUnit: Double
          _tmpRepairCostPerUnit = _stmt.getDouble(_columnIndexOfRepairCostPerUnit)
          _result = SneakerEntity(_tmpId,_tmpFactionId,_tmpRarity,_tmpVariant,_tmpLevel,_tmpMintNumber,_tmpLuck,_tmpComfort,_tmpDurability,_tmpEquipped,_tmpAcquiredAt,_tmpServerId,_tmpOrigin,_tmpEfficiencyBps,_tmpComfortBps,_tmpDurabilityPts,_tmpMaxLevel,_tmpStatus,_tmpChainState,_tmpCanWithdraw,_tmpServerUpgradeCost,_tmpRepairCostPerPoint,_tmpGenesisNo,_tmpModelId,_tmpTokenId,_tmpDurabilityNow,_tmpDurabilityMax,_tmpDurabilityBase,_tmpEfficiencyBaseBps,_tmpComfortBaseBps,_tmpPointsTotal,_tmpPointsFree,_tmpPointsEfficiency,_tmpPointsComfort,_tmpPointsDurability,_tmpPointsRespec,_tmpRepairCostPerUnit)
        } else {
          _result = null
        }
        _result
      } finally {
        _stmt.close()
      }
    }
  }

  public override suspend fun byId(id: Long): SneakerEntity? {
    val _sql: String = "SELECT * FROM sneakers WHERE id = ?"
    return performSuspending(__db, true, false) { _connection ->
      val _stmt: SQLiteStatement = _connection.prepare(_sql)
      try {
        var _argIndex: Int = 1
        _stmt.bindLong(_argIndex, id)
        val _columnIndexOfId: Int = getColumnIndexOrThrow(_stmt, "id")
        val _columnIndexOfFactionId: Int = getColumnIndexOrThrow(_stmt, "factionId")
        val _columnIndexOfRarity: Int = getColumnIndexOrThrow(_stmt, "rarity")
        val _columnIndexOfVariant: Int = getColumnIndexOrThrow(_stmt, "variant")
        val _columnIndexOfLevel: Int = getColumnIndexOrThrow(_stmt, "level")
        val _columnIndexOfMintNumber: Int = getColumnIndexOrThrow(_stmt, "mintNumber")
        val _columnIndexOfLuck: Int = getColumnIndexOrThrow(_stmt, "luck")
        val _columnIndexOfComfort: Int = getColumnIndexOrThrow(_stmt, "comfort")
        val _columnIndexOfDurability: Int = getColumnIndexOrThrow(_stmt, "durability")
        val _columnIndexOfEquipped: Int = getColumnIndexOrThrow(_stmt, "equipped")
        val _columnIndexOfAcquiredAt: Int = getColumnIndexOrThrow(_stmt, "acquiredAt")
        val _columnIndexOfServerId: Int = getColumnIndexOrThrow(_stmt, "serverId")
        val _columnIndexOfOrigin: Int = getColumnIndexOrThrow(_stmt, "origin")
        val _columnIndexOfEfficiencyBps: Int = getColumnIndexOrThrow(_stmt, "efficiencyBps")
        val _columnIndexOfComfortBps: Int = getColumnIndexOrThrow(_stmt, "comfortBps")
        val _columnIndexOfDurabilityPts: Int = getColumnIndexOrThrow(_stmt, "durabilityPts")
        val _columnIndexOfMaxLevel: Int = getColumnIndexOrThrow(_stmt, "maxLevel")
        val _columnIndexOfStatus: Int = getColumnIndexOrThrow(_stmt, "status")
        val _columnIndexOfChainState: Int = getColumnIndexOrThrow(_stmt, "chainState")
        val _columnIndexOfCanWithdraw: Int = getColumnIndexOrThrow(_stmt, "canWithdraw")
        val _columnIndexOfServerUpgradeCost: Int = getColumnIndexOrThrow(_stmt, "serverUpgradeCost")
        val _columnIndexOfRepairCostPerPoint: Int = getColumnIndexOrThrow(_stmt, "repairCostPerPoint")
        val _columnIndexOfGenesisNo: Int = getColumnIndexOrThrow(_stmt, "genesisNo")
        val _columnIndexOfModelId: Int = getColumnIndexOrThrow(_stmt, "modelId")
        val _columnIndexOfTokenId: Int = getColumnIndexOrThrow(_stmt, "tokenId")
        val _columnIndexOfDurabilityNow: Int = getColumnIndexOrThrow(_stmt, "durabilityNow")
        val _columnIndexOfDurabilityMax: Int = getColumnIndexOrThrow(_stmt, "durabilityMax")
        val _columnIndexOfDurabilityBase: Int = getColumnIndexOrThrow(_stmt, "durabilityBase")
        val _columnIndexOfEfficiencyBaseBps: Int = getColumnIndexOrThrow(_stmt, "efficiencyBaseBps")
        val _columnIndexOfComfortBaseBps: Int = getColumnIndexOrThrow(_stmt, "comfortBaseBps")
        val _columnIndexOfPointsTotal: Int = getColumnIndexOrThrow(_stmt, "pointsTotal")
        val _columnIndexOfPointsFree: Int = getColumnIndexOrThrow(_stmt, "pointsFree")
        val _columnIndexOfPointsEfficiency: Int = getColumnIndexOrThrow(_stmt, "pointsEfficiency")
        val _columnIndexOfPointsComfort: Int = getColumnIndexOrThrow(_stmt, "pointsComfort")
        val _columnIndexOfPointsDurability: Int = getColumnIndexOrThrow(_stmt, "pointsDurability")
        val _columnIndexOfPointsRespec: Int = getColumnIndexOrThrow(_stmt, "pointsRespec")
        val _columnIndexOfRepairCostPerUnit: Int = getColumnIndexOrThrow(_stmt, "repairCostPerUnit")
        val _result: SneakerEntity?
        if (_stmt.step()) {
          val _tmpId: Long
          _tmpId = _stmt.getLong(_columnIndexOfId)
          val _tmpFactionId: String
          _tmpFactionId = _stmt.getText(_columnIndexOfFactionId)
          val _tmpRarity: String
          _tmpRarity = _stmt.getText(_columnIndexOfRarity)
          val _tmpVariant: Int
          _tmpVariant = _stmt.getLong(_columnIndexOfVariant).toInt()
          val _tmpLevel: Int
          _tmpLevel = _stmt.getLong(_columnIndexOfLevel).toInt()
          val _tmpMintNumber: Int
          _tmpMintNumber = _stmt.getLong(_columnIndexOfMintNumber).toInt()
          val _tmpLuck: Double
          _tmpLuck = _stmt.getDouble(_columnIndexOfLuck)
          val _tmpComfort: Double
          _tmpComfort = _stmt.getDouble(_columnIndexOfComfort)
          val _tmpDurability: Int
          _tmpDurability = _stmt.getLong(_columnIndexOfDurability).toInt()
          val _tmpEquipped: Boolean
          val _tmp: Int
          _tmp = _stmt.getLong(_columnIndexOfEquipped).toInt()
          _tmpEquipped = _tmp != 0
          val _tmpAcquiredAt: Long
          _tmpAcquiredAt = _stmt.getLong(_columnIndexOfAcquiredAt)
          val _tmpServerId: Long
          _tmpServerId = _stmt.getLong(_columnIndexOfServerId)
          val _tmpOrigin: String
          _tmpOrigin = _stmt.getText(_columnIndexOfOrigin)
          val _tmpEfficiencyBps: Int
          _tmpEfficiencyBps = _stmt.getLong(_columnIndexOfEfficiencyBps).toInt()
          val _tmpComfortBps: Int
          _tmpComfortBps = _stmt.getLong(_columnIndexOfComfortBps).toInt()
          val _tmpDurabilityPts: Double
          _tmpDurabilityPts = _stmt.getDouble(_columnIndexOfDurabilityPts)
          val _tmpMaxLevel: Int
          _tmpMaxLevel = _stmt.getLong(_columnIndexOfMaxLevel).toInt()
          val _tmpStatus: String
          _tmpStatus = _stmt.getText(_columnIndexOfStatus)
          val _tmpChainState: String
          _tmpChainState = _stmt.getText(_columnIndexOfChainState)
          val _tmpCanWithdraw: Boolean
          val _tmp_1: Int
          _tmp_1 = _stmt.getLong(_columnIndexOfCanWithdraw).toInt()
          _tmpCanWithdraw = _tmp_1 != 0
          val _tmpServerUpgradeCost: Double
          _tmpServerUpgradeCost = _stmt.getDouble(_columnIndexOfServerUpgradeCost)
          val _tmpRepairCostPerPoint: Double
          _tmpRepairCostPerPoint = _stmt.getDouble(_columnIndexOfRepairCostPerPoint)
          val _tmpGenesisNo: Int
          _tmpGenesisNo = _stmt.getLong(_columnIndexOfGenesisNo).toInt()
          val _tmpModelId: Int
          _tmpModelId = _stmt.getLong(_columnIndexOfModelId).toInt()
          val _tmpTokenId: Long
          _tmpTokenId = _stmt.getLong(_columnIndexOfTokenId)
          val _tmpDurabilityNow: Int
          _tmpDurabilityNow = _stmt.getLong(_columnIndexOfDurabilityNow).toInt()
          val _tmpDurabilityMax: Int
          _tmpDurabilityMax = _stmt.getLong(_columnIndexOfDurabilityMax).toInt()
          val _tmpDurabilityBase: Int
          _tmpDurabilityBase = _stmt.getLong(_columnIndexOfDurabilityBase).toInt()
          val _tmpEfficiencyBaseBps: Int
          _tmpEfficiencyBaseBps = _stmt.getLong(_columnIndexOfEfficiencyBaseBps).toInt()
          val _tmpComfortBaseBps: Int
          _tmpComfortBaseBps = _stmt.getLong(_columnIndexOfComfortBaseBps).toInt()
          val _tmpPointsTotal: Int
          _tmpPointsTotal = _stmt.getLong(_columnIndexOfPointsTotal).toInt()
          val _tmpPointsFree: Int
          _tmpPointsFree = _stmt.getLong(_columnIndexOfPointsFree).toInt()
          val _tmpPointsEfficiency: Int
          _tmpPointsEfficiency = _stmt.getLong(_columnIndexOfPointsEfficiency).toInt()
          val _tmpPointsComfort: Int
          _tmpPointsComfort = _stmt.getLong(_columnIndexOfPointsComfort).toInt()
          val _tmpPointsDurability: Int
          _tmpPointsDurability = _stmt.getLong(_columnIndexOfPointsDurability).toInt()
          val _tmpPointsRespec: Boolean
          val _tmp_2: Int
          _tmp_2 = _stmt.getLong(_columnIndexOfPointsRespec).toInt()
          _tmpPointsRespec = _tmp_2 != 0
          val _tmpRepairCostPerUnit: Double
          _tmpRepairCostPerUnit = _stmt.getDouble(_columnIndexOfRepairCostPerUnit)
          _result = SneakerEntity(_tmpId,_tmpFactionId,_tmpRarity,_tmpVariant,_tmpLevel,_tmpMintNumber,_tmpLuck,_tmpComfort,_tmpDurability,_tmpEquipped,_tmpAcquiredAt,_tmpServerId,_tmpOrigin,_tmpEfficiencyBps,_tmpComfortBps,_tmpDurabilityPts,_tmpMaxLevel,_tmpStatus,_tmpChainState,_tmpCanWithdraw,_tmpServerUpgradeCost,_tmpRepairCostPerPoint,_tmpGenesisNo,_tmpModelId,_tmpTokenId,_tmpDurabilityNow,_tmpDurabilityMax,_tmpDurabilityBase,_tmpEfficiencyBaseBps,_tmpComfortBaseBps,_tmpPointsTotal,_tmpPointsFree,_tmpPointsEfficiency,_tmpPointsComfort,_tmpPointsDurability,_tmpPointsRespec,_tmpRepairCostPerUnit)
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
    val _sql: String = "SELECT COUNT(*) FROM sneakers"
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

  public override fun observeCount(): Flow<Int> {
    val _sql: String = "SELECT COUNT(*) FROM sneakers"
    return createFlow(__db, false, arrayOf("sneakers")) { _connection ->
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

  public override suspend fun maxMintNumber(): Int {
    val _sql: String = "SELECT COALESCE(MAX(mintNumber), 0) FROM sneakers"
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

  public override suspend fun allNow(): List<SneakerEntity> {
    val _sql: String = "SELECT * FROM sneakers"
    return performSuspending(__db, true, false) { _connection ->
      val _stmt: SQLiteStatement = _connection.prepare(_sql)
      try {
        val _columnIndexOfId: Int = getColumnIndexOrThrow(_stmt, "id")
        val _columnIndexOfFactionId: Int = getColumnIndexOrThrow(_stmt, "factionId")
        val _columnIndexOfRarity: Int = getColumnIndexOrThrow(_stmt, "rarity")
        val _columnIndexOfVariant: Int = getColumnIndexOrThrow(_stmt, "variant")
        val _columnIndexOfLevel: Int = getColumnIndexOrThrow(_stmt, "level")
        val _columnIndexOfMintNumber: Int = getColumnIndexOrThrow(_stmt, "mintNumber")
        val _columnIndexOfLuck: Int = getColumnIndexOrThrow(_stmt, "luck")
        val _columnIndexOfComfort: Int = getColumnIndexOrThrow(_stmt, "comfort")
        val _columnIndexOfDurability: Int = getColumnIndexOrThrow(_stmt, "durability")
        val _columnIndexOfEquipped: Int = getColumnIndexOrThrow(_stmt, "equipped")
        val _columnIndexOfAcquiredAt: Int = getColumnIndexOrThrow(_stmt, "acquiredAt")
        val _columnIndexOfServerId: Int = getColumnIndexOrThrow(_stmt, "serverId")
        val _columnIndexOfOrigin: Int = getColumnIndexOrThrow(_stmt, "origin")
        val _columnIndexOfEfficiencyBps: Int = getColumnIndexOrThrow(_stmt, "efficiencyBps")
        val _columnIndexOfComfortBps: Int = getColumnIndexOrThrow(_stmt, "comfortBps")
        val _columnIndexOfDurabilityPts: Int = getColumnIndexOrThrow(_stmt, "durabilityPts")
        val _columnIndexOfMaxLevel: Int = getColumnIndexOrThrow(_stmt, "maxLevel")
        val _columnIndexOfStatus: Int = getColumnIndexOrThrow(_stmt, "status")
        val _columnIndexOfChainState: Int = getColumnIndexOrThrow(_stmt, "chainState")
        val _columnIndexOfCanWithdraw: Int = getColumnIndexOrThrow(_stmt, "canWithdraw")
        val _columnIndexOfServerUpgradeCost: Int = getColumnIndexOrThrow(_stmt, "serverUpgradeCost")
        val _columnIndexOfRepairCostPerPoint: Int = getColumnIndexOrThrow(_stmt, "repairCostPerPoint")
        val _columnIndexOfGenesisNo: Int = getColumnIndexOrThrow(_stmt, "genesisNo")
        val _columnIndexOfModelId: Int = getColumnIndexOrThrow(_stmt, "modelId")
        val _columnIndexOfTokenId: Int = getColumnIndexOrThrow(_stmt, "tokenId")
        val _columnIndexOfDurabilityNow: Int = getColumnIndexOrThrow(_stmt, "durabilityNow")
        val _columnIndexOfDurabilityMax: Int = getColumnIndexOrThrow(_stmt, "durabilityMax")
        val _columnIndexOfDurabilityBase: Int = getColumnIndexOrThrow(_stmt, "durabilityBase")
        val _columnIndexOfEfficiencyBaseBps: Int = getColumnIndexOrThrow(_stmt, "efficiencyBaseBps")
        val _columnIndexOfComfortBaseBps: Int = getColumnIndexOrThrow(_stmt, "comfortBaseBps")
        val _columnIndexOfPointsTotal: Int = getColumnIndexOrThrow(_stmt, "pointsTotal")
        val _columnIndexOfPointsFree: Int = getColumnIndexOrThrow(_stmt, "pointsFree")
        val _columnIndexOfPointsEfficiency: Int = getColumnIndexOrThrow(_stmt, "pointsEfficiency")
        val _columnIndexOfPointsComfort: Int = getColumnIndexOrThrow(_stmt, "pointsComfort")
        val _columnIndexOfPointsDurability: Int = getColumnIndexOrThrow(_stmt, "pointsDurability")
        val _columnIndexOfPointsRespec: Int = getColumnIndexOrThrow(_stmt, "pointsRespec")
        val _columnIndexOfRepairCostPerUnit: Int = getColumnIndexOrThrow(_stmt, "repairCostPerUnit")
        val _result: MutableList<SneakerEntity> = mutableListOf()
        while (_stmt.step()) {
          val _item: SneakerEntity
          val _tmpId: Long
          _tmpId = _stmt.getLong(_columnIndexOfId)
          val _tmpFactionId: String
          _tmpFactionId = _stmt.getText(_columnIndexOfFactionId)
          val _tmpRarity: String
          _tmpRarity = _stmt.getText(_columnIndexOfRarity)
          val _tmpVariant: Int
          _tmpVariant = _stmt.getLong(_columnIndexOfVariant).toInt()
          val _tmpLevel: Int
          _tmpLevel = _stmt.getLong(_columnIndexOfLevel).toInt()
          val _tmpMintNumber: Int
          _tmpMintNumber = _stmt.getLong(_columnIndexOfMintNumber).toInt()
          val _tmpLuck: Double
          _tmpLuck = _stmt.getDouble(_columnIndexOfLuck)
          val _tmpComfort: Double
          _tmpComfort = _stmt.getDouble(_columnIndexOfComfort)
          val _tmpDurability: Int
          _tmpDurability = _stmt.getLong(_columnIndexOfDurability).toInt()
          val _tmpEquipped: Boolean
          val _tmp: Int
          _tmp = _stmt.getLong(_columnIndexOfEquipped).toInt()
          _tmpEquipped = _tmp != 0
          val _tmpAcquiredAt: Long
          _tmpAcquiredAt = _stmt.getLong(_columnIndexOfAcquiredAt)
          val _tmpServerId: Long
          _tmpServerId = _stmt.getLong(_columnIndexOfServerId)
          val _tmpOrigin: String
          _tmpOrigin = _stmt.getText(_columnIndexOfOrigin)
          val _tmpEfficiencyBps: Int
          _tmpEfficiencyBps = _stmt.getLong(_columnIndexOfEfficiencyBps).toInt()
          val _tmpComfortBps: Int
          _tmpComfortBps = _stmt.getLong(_columnIndexOfComfortBps).toInt()
          val _tmpDurabilityPts: Double
          _tmpDurabilityPts = _stmt.getDouble(_columnIndexOfDurabilityPts)
          val _tmpMaxLevel: Int
          _tmpMaxLevel = _stmt.getLong(_columnIndexOfMaxLevel).toInt()
          val _tmpStatus: String
          _tmpStatus = _stmt.getText(_columnIndexOfStatus)
          val _tmpChainState: String
          _tmpChainState = _stmt.getText(_columnIndexOfChainState)
          val _tmpCanWithdraw: Boolean
          val _tmp_1: Int
          _tmp_1 = _stmt.getLong(_columnIndexOfCanWithdraw).toInt()
          _tmpCanWithdraw = _tmp_1 != 0
          val _tmpServerUpgradeCost: Double
          _tmpServerUpgradeCost = _stmt.getDouble(_columnIndexOfServerUpgradeCost)
          val _tmpRepairCostPerPoint: Double
          _tmpRepairCostPerPoint = _stmt.getDouble(_columnIndexOfRepairCostPerPoint)
          val _tmpGenesisNo: Int
          _tmpGenesisNo = _stmt.getLong(_columnIndexOfGenesisNo).toInt()
          val _tmpModelId: Int
          _tmpModelId = _stmt.getLong(_columnIndexOfModelId).toInt()
          val _tmpTokenId: Long
          _tmpTokenId = _stmt.getLong(_columnIndexOfTokenId)
          val _tmpDurabilityNow: Int
          _tmpDurabilityNow = _stmt.getLong(_columnIndexOfDurabilityNow).toInt()
          val _tmpDurabilityMax: Int
          _tmpDurabilityMax = _stmt.getLong(_columnIndexOfDurabilityMax).toInt()
          val _tmpDurabilityBase: Int
          _tmpDurabilityBase = _stmt.getLong(_columnIndexOfDurabilityBase).toInt()
          val _tmpEfficiencyBaseBps: Int
          _tmpEfficiencyBaseBps = _stmt.getLong(_columnIndexOfEfficiencyBaseBps).toInt()
          val _tmpComfortBaseBps: Int
          _tmpComfortBaseBps = _stmt.getLong(_columnIndexOfComfortBaseBps).toInt()
          val _tmpPointsTotal: Int
          _tmpPointsTotal = _stmt.getLong(_columnIndexOfPointsTotal).toInt()
          val _tmpPointsFree: Int
          _tmpPointsFree = _stmt.getLong(_columnIndexOfPointsFree).toInt()
          val _tmpPointsEfficiency: Int
          _tmpPointsEfficiency = _stmt.getLong(_columnIndexOfPointsEfficiency).toInt()
          val _tmpPointsComfort: Int
          _tmpPointsComfort = _stmt.getLong(_columnIndexOfPointsComfort).toInt()
          val _tmpPointsDurability: Int
          _tmpPointsDurability = _stmt.getLong(_columnIndexOfPointsDurability).toInt()
          val _tmpPointsRespec: Boolean
          val _tmp_2: Int
          _tmp_2 = _stmt.getLong(_columnIndexOfPointsRespec).toInt()
          _tmpPointsRespec = _tmp_2 != 0
          val _tmpRepairCostPerUnit: Double
          _tmpRepairCostPerUnit = _stmt.getDouble(_columnIndexOfRepairCostPerUnit)
          _item = SneakerEntity(_tmpId,_tmpFactionId,_tmpRarity,_tmpVariant,_tmpLevel,_tmpMintNumber,_tmpLuck,_tmpComfort,_tmpDurability,_tmpEquipped,_tmpAcquiredAt,_tmpServerId,_tmpOrigin,_tmpEfficiencyBps,_tmpComfortBps,_tmpDurabilityPts,_tmpMaxLevel,_tmpStatus,_tmpChainState,_tmpCanWithdraw,_tmpServerUpgradeCost,_tmpRepairCostPerPoint,_tmpGenesisNo,_tmpModelId,_tmpTokenId,_tmpDurabilityNow,_tmpDurabilityMax,_tmpDurabilityBase,_tmpEfficiencyBaseBps,_tmpComfortBaseBps,_tmpPointsTotal,_tmpPointsFree,_tmpPointsEfficiency,_tmpPointsComfort,_tmpPointsDurability,_tmpPointsRespec,_tmpRepairCostPerUnit)
          _result.add(_item)
        }
        _result
      } finally {
        _stmt.close()
      }
    }
  }

  public override suspend fun byServerId(serverId: Long): SneakerEntity? {
    val _sql: String = "SELECT * FROM sneakers WHERE serverId = ? LIMIT 1"
    return performSuspending(__db, true, false) { _connection ->
      val _stmt: SQLiteStatement = _connection.prepare(_sql)
      try {
        var _argIndex: Int = 1
        _stmt.bindLong(_argIndex, serverId)
        val _columnIndexOfId: Int = getColumnIndexOrThrow(_stmt, "id")
        val _columnIndexOfFactionId: Int = getColumnIndexOrThrow(_stmt, "factionId")
        val _columnIndexOfRarity: Int = getColumnIndexOrThrow(_stmt, "rarity")
        val _columnIndexOfVariant: Int = getColumnIndexOrThrow(_stmt, "variant")
        val _columnIndexOfLevel: Int = getColumnIndexOrThrow(_stmt, "level")
        val _columnIndexOfMintNumber: Int = getColumnIndexOrThrow(_stmt, "mintNumber")
        val _columnIndexOfLuck: Int = getColumnIndexOrThrow(_stmt, "luck")
        val _columnIndexOfComfort: Int = getColumnIndexOrThrow(_stmt, "comfort")
        val _columnIndexOfDurability: Int = getColumnIndexOrThrow(_stmt, "durability")
        val _columnIndexOfEquipped: Int = getColumnIndexOrThrow(_stmt, "equipped")
        val _columnIndexOfAcquiredAt: Int = getColumnIndexOrThrow(_stmt, "acquiredAt")
        val _columnIndexOfServerId: Int = getColumnIndexOrThrow(_stmt, "serverId")
        val _columnIndexOfOrigin: Int = getColumnIndexOrThrow(_stmt, "origin")
        val _columnIndexOfEfficiencyBps: Int = getColumnIndexOrThrow(_stmt, "efficiencyBps")
        val _columnIndexOfComfortBps: Int = getColumnIndexOrThrow(_stmt, "comfortBps")
        val _columnIndexOfDurabilityPts: Int = getColumnIndexOrThrow(_stmt, "durabilityPts")
        val _columnIndexOfMaxLevel: Int = getColumnIndexOrThrow(_stmt, "maxLevel")
        val _columnIndexOfStatus: Int = getColumnIndexOrThrow(_stmt, "status")
        val _columnIndexOfChainState: Int = getColumnIndexOrThrow(_stmt, "chainState")
        val _columnIndexOfCanWithdraw: Int = getColumnIndexOrThrow(_stmt, "canWithdraw")
        val _columnIndexOfServerUpgradeCost: Int = getColumnIndexOrThrow(_stmt, "serverUpgradeCost")
        val _columnIndexOfRepairCostPerPoint: Int = getColumnIndexOrThrow(_stmt, "repairCostPerPoint")
        val _columnIndexOfGenesisNo: Int = getColumnIndexOrThrow(_stmt, "genesisNo")
        val _columnIndexOfModelId: Int = getColumnIndexOrThrow(_stmt, "modelId")
        val _columnIndexOfTokenId: Int = getColumnIndexOrThrow(_stmt, "tokenId")
        val _columnIndexOfDurabilityNow: Int = getColumnIndexOrThrow(_stmt, "durabilityNow")
        val _columnIndexOfDurabilityMax: Int = getColumnIndexOrThrow(_stmt, "durabilityMax")
        val _columnIndexOfDurabilityBase: Int = getColumnIndexOrThrow(_stmt, "durabilityBase")
        val _columnIndexOfEfficiencyBaseBps: Int = getColumnIndexOrThrow(_stmt, "efficiencyBaseBps")
        val _columnIndexOfComfortBaseBps: Int = getColumnIndexOrThrow(_stmt, "comfortBaseBps")
        val _columnIndexOfPointsTotal: Int = getColumnIndexOrThrow(_stmt, "pointsTotal")
        val _columnIndexOfPointsFree: Int = getColumnIndexOrThrow(_stmt, "pointsFree")
        val _columnIndexOfPointsEfficiency: Int = getColumnIndexOrThrow(_stmt, "pointsEfficiency")
        val _columnIndexOfPointsComfort: Int = getColumnIndexOrThrow(_stmt, "pointsComfort")
        val _columnIndexOfPointsDurability: Int = getColumnIndexOrThrow(_stmt, "pointsDurability")
        val _columnIndexOfPointsRespec: Int = getColumnIndexOrThrow(_stmt, "pointsRespec")
        val _columnIndexOfRepairCostPerUnit: Int = getColumnIndexOrThrow(_stmt, "repairCostPerUnit")
        val _result: SneakerEntity?
        if (_stmt.step()) {
          val _tmpId: Long
          _tmpId = _stmt.getLong(_columnIndexOfId)
          val _tmpFactionId: String
          _tmpFactionId = _stmt.getText(_columnIndexOfFactionId)
          val _tmpRarity: String
          _tmpRarity = _stmt.getText(_columnIndexOfRarity)
          val _tmpVariant: Int
          _tmpVariant = _stmt.getLong(_columnIndexOfVariant).toInt()
          val _tmpLevel: Int
          _tmpLevel = _stmt.getLong(_columnIndexOfLevel).toInt()
          val _tmpMintNumber: Int
          _tmpMintNumber = _stmt.getLong(_columnIndexOfMintNumber).toInt()
          val _tmpLuck: Double
          _tmpLuck = _stmt.getDouble(_columnIndexOfLuck)
          val _tmpComfort: Double
          _tmpComfort = _stmt.getDouble(_columnIndexOfComfort)
          val _tmpDurability: Int
          _tmpDurability = _stmt.getLong(_columnIndexOfDurability).toInt()
          val _tmpEquipped: Boolean
          val _tmp: Int
          _tmp = _stmt.getLong(_columnIndexOfEquipped).toInt()
          _tmpEquipped = _tmp != 0
          val _tmpAcquiredAt: Long
          _tmpAcquiredAt = _stmt.getLong(_columnIndexOfAcquiredAt)
          val _tmpServerId: Long
          _tmpServerId = _stmt.getLong(_columnIndexOfServerId)
          val _tmpOrigin: String
          _tmpOrigin = _stmt.getText(_columnIndexOfOrigin)
          val _tmpEfficiencyBps: Int
          _tmpEfficiencyBps = _stmt.getLong(_columnIndexOfEfficiencyBps).toInt()
          val _tmpComfortBps: Int
          _tmpComfortBps = _stmt.getLong(_columnIndexOfComfortBps).toInt()
          val _tmpDurabilityPts: Double
          _tmpDurabilityPts = _stmt.getDouble(_columnIndexOfDurabilityPts)
          val _tmpMaxLevel: Int
          _tmpMaxLevel = _stmt.getLong(_columnIndexOfMaxLevel).toInt()
          val _tmpStatus: String
          _tmpStatus = _stmt.getText(_columnIndexOfStatus)
          val _tmpChainState: String
          _tmpChainState = _stmt.getText(_columnIndexOfChainState)
          val _tmpCanWithdraw: Boolean
          val _tmp_1: Int
          _tmp_1 = _stmt.getLong(_columnIndexOfCanWithdraw).toInt()
          _tmpCanWithdraw = _tmp_1 != 0
          val _tmpServerUpgradeCost: Double
          _tmpServerUpgradeCost = _stmt.getDouble(_columnIndexOfServerUpgradeCost)
          val _tmpRepairCostPerPoint: Double
          _tmpRepairCostPerPoint = _stmt.getDouble(_columnIndexOfRepairCostPerPoint)
          val _tmpGenesisNo: Int
          _tmpGenesisNo = _stmt.getLong(_columnIndexOfGenesisNo).toInt()
          val _tmpModelId: Int
          _tmpModelId = _stmt.getLong(_columnIndexOfModelId).toInt()
          val _tmpTokenId: Long
          _tmpTokenId = _stmt.getLong(_columnIndexOfTokenId)
          val _tmpDurabilityNow: Int
          _tmpDurabilityNow = _stmt.getLong(_columnIndexOfDurabilityNow).toInt()
          val _tmpDurabilityMax: Int
          _tmpDurabilityMax = _stmt.getLong(_columnIndexOfDurabilityMax).toInt()
          val _tmpDurabilityBase: Int
          _tmpDurabilityBase = _stmt.getLong(_columnIndexOfDurabilityBase).toInt()
          val _tmpEfficiencyBaseBps: Int
          _tmpEfficiencyBaseBps = _stmt.getLong(_columnIndexOfEfficiencyBaseBps).toInt()
          val _tmpComfortBaseBps: Int
          _tmpComfortBaseBps = _stmt.getLong(_columnIndexOfComfortBaseBps).toInt()
          val _tmpPointsTotal: Int
          _tmpPointsTotal = _stmt.getLong(_columnIndexOfPointsTotal).toInt()
          val _tmpPointsFree: Int
          _tmpPointsFree = _stmt.getLong(_columnIndexOfPointsFree).toInt()
          val _tmpPointsEfficiency: Int
          _tmpPointsEfficiency = _stmt.getLong(_columnIndexOfPointsEfficiency).toInt()
          val _tmpPointsComfort: Int
          _tmpPointsComfort = _stmt.getLong(_columnIndexOfPointsComfort).toInt()
          val _tmpPointsDurability: Int
          _tmpPointsDurability = _stmt.getLong(_columnIndexOfPointsDurability).toInt()
          val _tmpPointsRespec: Boolean
          val _tmp_2: Int
          _tmp_2 = _stmt.getLong(_columnIndexOfPointsRespec).toInt()
          _tmpPointsRespec = _tmp_2 != 0
          val _tmpRepairCostPerUnit: Double
          _tmpRepairCostPerUnit = _stmt.getDouble(_columnIndexOfRepairCostPerUnit)
          _result = SneakerEntity(_tmpId,_tmpFactionId,_tmpRarity,_tmpVariant,_tmpLevel,_tmpMintNumber,_tmpLuck,_tmpComfort,_tmpDurability,_tmpEquipped,_tmpAcquiredAt,_tmpServerId,_tmpOrigin,_tmpEfficiencyBps,_tmpComfortBps,_tmpDurabilityPts,_tmpMaxLevel,_tmpStatus,_tmpChainState,_tmpCanWithdraw,_tmpServerUpgradeCost,_tmpRepairCostPerPoint,_tmpGenesisNo,_tmpModelId,_tmpTokenId,_tmpDurabilityNow,_tmpDurabilityMax,_tmpDurabilityBase,_tmpEfficiencyBaseBps,_tmpComfortBaseBps,_tmpPointsTotal,_tmpPointsFree,_tmpPointsEfficiency,_tmpPointsComfort,_tmpPointsDurability,_tmpPointsRespec,_tmpRepairCostPerUnit)
        } else {
          _result = null
        }
        _result
      } finally {
        _stmt.close()
      }
    }
  }

  public override suspend fun registered(): List<SneakerEntity> {
    val _sql: String = "SELECT * FROM sneakers WHERE serverId <> 0"
    return performSuspending(__db, true, false) { _connection ->
      val _stmt: SQLiteStatement = _connection.prepare(_sql)
      try {
        val _columnIndexOfId: Int = getColumnIndexOrThrow(_stmt, "id")
        val _columnIndexOfFactionId: Int = getColumnIndexOrThrow(_stmt, "factionId")
        val _columnIndexOfRarity: Int = getColumnIndexOrThrow(_stmt, "rarity")
        val _columnIndexOfVariant: Int = getColumnIndexOrThrow(_stmt, "variant")
        val _columnIndexOfLevel: Int = getColumnIndexOrThrow(_stmt, "level")
        val _columnIndexOfMintNumber: Int = getColumnIndexOrThrow(_stmt, "mintNumber")
        val _columnIndexOfLuck: Int = getColumnIndexOrThrow(_stmt, "luck")
        val _columnIndexOfComfort: Int = getColumnIndexOrThrow(_stmt, "comfort")
        val _columnIndexOfDurability: Int = getColumnIndexOrThrow(_stmt, "durability")
        val _columnIndexOfEquipped: Int = getColumnIndexOrThrow(_stmt, "equipped")
        val _columnIndexOfAcquiredAt: Int = getColumnIndexOrThrow(_stmt, "acquiredAt")
        val _columnIndexOfServerId: Int = getColumnIndexOrThrow(_stmt, "serverId")
        val _columnIndexOfOrigin: Int = getColumnIndexOrThrow(_stmt, "origin")
        val _columnIndexOfEfficiencyBps: Int = getColumnIndexOrThrow(_stmt, "efficiencyBps")
        val _columnIndexOfComfortBps: Int = getColumnIndexOrThrow(_stmt, "comfortBps")
        val _columnIndexOfDurabilityPts: Int = getColumnIndexOrThrow(_stmt, "durabilityPts")
        val _columnIndexOfMaxLevel: Int = getColumnIndexOrThrow(_stmt, "maxLevel")
        val _columnIndexOfStatus: Int = getColumnIndexOrThrow(_stmt, "status")
        val _columnIndexOfChainState: Int = getColumnIndexOrThrow(_stmt, "chainState")
        val _columnIndexOfCanWithdraw: Int = getColumnIndexOrThrow(_stmt, "canWithdraw")
        val _columnIndexOfServerUpgradeCost: Int = getColumnIndexOrThrow(_stmt, "serverUpgradeCost")
        val _columnIndexOfRepairCostPerPoint: Int = getColumnIndexOrThrow(_stmt, "repairCostPerPoint")
        val _columnIndexOfGenesisNo: Int = getColumnIndexOrThrow(_stmt, "genesisNo")
        val _columnIndexOfModelId: Int = getColumnIndexOrThrow(_stmt, "modelId")
        val _columnIndexOfTokenId: Int = getColumnIndexOrThrow(_stmt, "tokenId")
        val _columnIndexOfDurabilityNow: Int = getColumnIndexOrThrow(_stmt, "durabilityNow")
        val _columnIndexOfDurabilityMax: Int = getColumnIndexOrThrow(_stmt, "durabilityMax")
        val _columnIndexOfDurabilityBase: Int = getColumnIndexOrThrow(_stmt, "durabilityBase")
        val _columnIndexOfEfficiencyBaseBps: Int = getColumnIndexOrThrow(_stmt, "efficiencyBaseBps")
        val _columnIndexOfComfortBaseBps: Int = getColumnIndexOrThrow(_stmt, "comfortBaseBps")
        val _columnIndexOfPointsTotal: Int = getColumnIndexOrThrow(_stmt, "pointsTotal")
        val _columnIndexOfPointsFree: Int = getColumnIndexOrThrow(_stmt, "pointsFree")
        val _columnIndexOfPointsEfficiency: Int = getColumnIndexOrThrow(_stmt, "pointsEfficiency")
        val _columnIndexOfPointsComfort: Int = getColumnIndexOrThrow(_stmt, "pointsComfort")
        val _columnIndexOfPointsDurability: Int = getColumnIndexOrThrow(_stmt, "pointsDurability")
        val _columnIndexOfPointsRespec: Int = getColumnIndexOrThrow(_stmt, "pointsRespec")
        val _columnIndexOfRepairCostPerUnit: Int = getColumnIndexOrThrow(_stmt, "repairCostPerUnit")
        val _result: MutableList<SneakerEntity> = mutableListOf()
        while (_stmt.step()) {
          val _item: SneakerEntity
          val _tmpId: Long
          _tmpId = _stmt.getLong(_columnIndexOfId)
          val _tmpFactionId: String
          _tmpFactionId = _stmt.getText(_columnIndexOfFactionId)
          val _tmpRarity: String
          _tmpRarity = _stmt.getText(_columnIndexOfRarity)
          val _tmpVariant: Int
          _tmpVariant = _stmt.getLong(_columnIndexOfVariant).toInt()
          val _tmpLevel: Int
          _tmpLevel = _stmt.getLong(_columnIndexOfLevel).toInt()
          val _tmpMintNumber: Int
          _tmpMintNumber = _stmt.getLong(_columnIndexOfMintNumber).toInt()
          val _tmpLuck: Double
          _tmpLuck = _stmt.getDouble(_columnIndexOfLuck)
          val _tmpComfort: Double
          _tmpComfort = _stmt.getDouble(_columnIndexOfComfort)
          val _tmpDurability: Int
          _tmpDurability = _stmt.getLong(_columnIndexOfDurability).toInt()
          val _tmpEquipped: Boolean
          val _tmp: Int
          _tmp = _stmt.getLong(_columnIndexOfEquipped).toInt()
          _tmpEquipped = _tmp != 0
          val _tmpAcquiredAt: Long
          _tmpAcquiredAt = _stmt.getLong(_columnIndexOfAcquiredAt)
          val _tmpServerId: Long
          _tmpServerId = _stmt.getLong(_columnIndexOfServerId)
          val _tmpOrigin: String
          _tmpOrigin = _stmt.getText(_columnIndexOfOrigin)
          val _tmpEfficiencyBps: Int
          _tmpEfficiencyBps = _stmt.getLong(_columnIndexOfEfficiencyBps).toInt()
          val _tmpComfortBps: Int
          _tmpComfortBps = _stmt.getLong(_columnIndexOfComfortBps).toInt()
          val _tmpDurabilityPts: Double
          _tmpDurabilityPts = _stmt.getDouble(_columnIndexOfDurabilityPts)
          val _tmpMaxLevel: Int
          _tmpMaxLevel = _stmt.getLong(_columnIndexOfMaxLevel).toInt()
          val _tmpStatus: String
          _tmpStatus = _stmt.getText(_columnIndexOfStatus)
          val _tmpChainState: String
          _tmpChainState = _stmt.getText(_columnIndexOfChainState)
          val _tmpCanWithdraw: Boolean
          val _tmp_1: Int
          _tmp_1 = _stmt.getLong(_columnIndexOfCanWithdraw).toInt()
          _tmpCanWithdraw = _tmp_1 != 0
          val _tmpServerUpgradeCost: Double
          _tmpServerUpgradeCost = _stmt.getDouble(_columnIndexOfServerUpgradeCost)
          val _tmpRepairCostPerPoint: Double
          _tmpRepairCostPerPoint = _stmt.getDouble(_columnIndexOfRepairCostPerPoint)
          val _tmpGenesisNo: Int
          _tmpGenesisNo = _stmt.getLong(_columnIndexOfGenesisNo).toInt()
          val _tmpModelId: Int
          _tmpModelId = _stmt.getLong(_columnIndexOfModelId).toInt()
          val _tmpTokenId: Long
          _tmpTokenId = _stmt.getLong(_columnIndexOfTokenId)
          val _tmpDurabilityNow: Int
          _tmpDurabilityNow = _stmt.getLong(_columnIndexOfDurabilityNow).toInt()
          val _tmpDurabilityMax: Int
          _tmpDurabilityMax = _stmt.getLong(_columnIndexOfDurabilityMax).toInt()
          val _tmpDurabilityBase: Int
          _tmpDurabilityBase = _stmt.getLong(_columnIndexOfDurabilityBase).toInt()
          val _tmpEfficiencyBaseBps: Int
          _tmpEfficiencyBaseBps = _stmt.getLong(_columnIndexOfEfficiencyBaseBps).toInt()
          val _tmpComfortBaseBps: Int
          _tmpComfortBaseBps = _stmt.getLong(_columnIndexOfComfortBaseBps).toInt()
          val _tmpPointsTotal: Int
          _tmpPointsTotal = _stmt.getLong(_columnIndexOfPointsTotal).toInt()
          val _tmpPointsFree: Int
          _tmpPointsFree = _stmt.getLong(_columnIndexOfPointsFree).toInt()
          val _tmpPointsEfficiency: Int
          _tmpPointsEfficiency = _stmt.getLong(_columnIndexOfPointsEfficiency).toInt()
          val _tmpPointsComfort: Int
          _tmpPointsComfort = _stmt.getLong(_columnIndexOfPointsComfort).toInt()
          val _tmpPointsDurability: Int
          _tmpPointsDurability = _stmt.getLong(_columnIndexOfPointsDurability).toInt()
          val _tmpPointsRespec: Boolean
          val _tmp_2: Int
          _tmp_2 = _stmt.getLong(_columnIndexOfPointsRespec).toInt()
          _tmpPointsRespec = _tmp_2 != 0
          val _tmpRepairCostPerUnit: Double
          _tmpRepairCostPerUnit = _stmt.getDouble(_columnIndexOfRepairCostPerUnit)
          _item = SneakerEntity(_tmpId,_tmpFactionId,_tmpRarity,_tmpVariant,_tmpLevel,_tmpMintNumber,_tmpLuck,_tmpComfort,_tmpDurability,_tmpEquipped,_tmpAcquiredAt,_tmpServerId,_tmpOrigin,_tmpEfficiencyBps,_tmpComfortBps,_tmpDurabilityPts,_tmpMaxLevel,_tmpStatus,_tmpChainState,_tmpCanWithdraw,_tmpServerUpgradeCost,_tmpRepairCostPerPoint,_tmpGenesisNo,_tmpModelId,_tmpTokenId,_tmpDurabilityNow,_tmpDurabilityMax,_tmpDurabilityBase,_tmpEfficiencyBaseBps,_tmpComfortBaseBps,_tmpPointsTotal,_tmpPointsFree,_tmpPointsEfficiency,_tmpPointsComfort,_tmpPointsDurability,_tmpPointsRespec,_tmpRepairCostPerUnit)
          _result.add(_item)
        }
        _result
      } finally {
        _stmt.close()
      }
    }
  }

  public override suspend fun deleteAll() {
    val _sql: String = "DELETE FROM sneakers"
    return performSuspending(__db, false, true) { _connection ->
      val _stmt: SQLiteStatement = _connection.prepare(_sql)
      try {
        _stmt.step()
      } finally {
        _stmt.close()
      }
    }
  }

  public override suspend fun clearEquipped() {
    val _sql: String = "UPDATE sneakers SET equipped = 0"
    return performSuspending(__db, false, true) { _connection ->
      val _stmt: SQLiteStatement = _connection.prepare(_sql)
      try {
        _stmt.step()
      } finally {
        _stmt.close()
      }
    }
  }

  public override suspend fun equipExclusively(id: Long): Int {
    val _sql: String = "UPDATE sneakers SET equipped = CASE WHEN id = ? THEN 1 ELSE 0 END WHERE EXISTS (SELECT 1 FROM sneakers WHERE id = ?)"
    return performSuspending(__db, false, true) { _connection ->
      val _stmt: SQLiteStatement = _connection.prepare(_sql)
      try {
        var _argIndex: Int = 1
        _stmt.bindLong(_argIndex, id)
        _argIndex = 2
        _stmt.bindLong(_argIndex, id)
        _stmt.step()
        getTotalChangedRows(_connection)
      } finally {
        _stmt.close()
      }
    }
  }

  public override suspend fun setServerId(id: Long, serverId: Long) {
    val _sql: String = "UPDATE sneakers SET serverId = ? WHERE id = ?"
    return performSuspending(__db, false, true) { _connection ->
      val _stmt: SQLiteStatement = _connection.prepare(_sql)
      try {
        var _argIndex: Int = 1
        _stmt.bindLong(_argIndex, serverId)
        _argIndex = 2
        _stmt.bindLong(_argIndex, id)
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
