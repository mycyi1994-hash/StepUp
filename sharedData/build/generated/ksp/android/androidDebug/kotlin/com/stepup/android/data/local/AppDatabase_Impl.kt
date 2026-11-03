package com.stepup.android.`data`.local

import androidx.room.InvalidationTracker
import androidx.room.RoomOpenDelegate
import androidx.room.migration.AutoMigrationSpec
import androidx.room.migration.Migration
import androidx.room.util.TableInfo
import androidx.room.util.TableInfo.Companion.read
import androidx.room.util.dropFtsSyncTriggers
import androidx.sqlite.SQLiteConnection
import androidx.sqlite.execSQL
import javax.`annotation`.processing.Generated
import kotlin.Lazy
import kotlin.String
import kotlin.Suppress
import kotlin.collections.List
import kotlin.collections.Map
import kotlin.collections.MutableList
import kotlin.collections.MutableMap
import kotlin.collections.MutableSet
import kotlin.collections.Set
import kotlin.collections.mutableListOf
import kotlin.collections.mutableMapOf
import kotlin.collections.mutableSetOf
import kotlin.reflect.KClass

@Generated(value = ["androidx.room.RoomProcessor"])
@Suppress(names = ["UNCHECKED_CAST", "DEPRECATION", "REDUNDANT_PROJECTION", "REMOVAL"])
public class AppDatabase_Impl : AppDatabase() {
  private val _stepDao: Lazy<StepDao> = lazy {
    StepDao_Impl(this)
  }

  private val _walkSessionDao: Lazy<WalkSessionDao> = lazy {
    WalkSessionDao_Impl(this)
  }

  private val _rewardDao: Lazy<RewardDao> = lazy {
    RewardDao_Impl(this)
  }

  private val _sneakerDao: Lazy<SneakerDao> = lazy {
    SneakerDao_Impl(this)
  }

  private val _boostDao: Lazy<BoostDao> = lazy {
    BoostDao_Impl(this)
  }

  private val _energyPurchaseDao: Lazy<EnergyPurchaseDao> = lazy {
    EnergyPurchaseDao_Impl(this)
  }

  private val _runSettlementDao: Lazy<RunSettlementDao> = lazy {
    RunSettlementDao_Impl(this)
  }

  private val _claimedEventDao: Lazy<ClaimedEventDao> = lazy {
    ClaimedEventDao_Impl(this)
  }

  private val _crewDao: Lazy<CrewDao> = lazy {
    CrewDao_Impl(this)
  }

  private val _crewInfoDao: Lazy<CrewInfoDao> = lazy {
    CrewInfoDao_Impl(this)
  }

  private val _postDao: Lazy<PostDao> = lazy {
    PostDao_Impl(this)
  }

  private val _commentDao: Lazy<CommentDao> = lazy {
    CommentDao_Impl(this)
  }

  private val _courseDao: Lazy<CourseDao> = lazy {
    CourseDao_Impl(this)
  }

  private val _notificationDao: Lazy<NotificationDao> = lazy {
    NotificationDao_Impl(this)
  }

  private val _newsDao: Lazy<NewsDao> = lazy {
    NewsDao_Impl(this)
  }

  protected override fun createOpenDelegate(): RoomOpenDelegate {
    val _openDelegate: RoomOpenDelegate = object : RoomOpenDelegate(19, "f867391f2970e890288c861ee52acaf8", "df364b1d78dfda8671b3098f7eb23d78") {
      public override fun createAllTables(connection: SQLiteConnection) {
        connection.execSQL("CREATE TABLE IF NOT EXISTS `daily_steps` (`epochDay` INTEGER NOT NULL, `steps` INTEGER NOT NULL, `goal` INTEGER NOT NULL, `updatedAt` INTEGER NOT NULL, PRIMARY KEY(`epochDay`))")
        connection.execSQL("CREATE TABLE IF NOT EXISTS `walk_sessions` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `startedAt` INTEGER NOT NULL, `endedAt` INTEGER NOT NULL, `steps` INTEGER NOT NULL, `durationSec` INTEGER NOT NULL, `distanceMeters` REAL NOT NULL, `calories` REAL NOT NULL, `pointsEarned` REAL NOT NULL, `recordingOwner` TEXT NOT NULL DEFAULT 'legacy', `track` TEXT NOT NULL, `boostBps` INTEGER NOT NULL, `partySize` INTEGER NOT NULL, `faction` TEXT NOT NULL, `crewId` TEXT NOT NULL, `uploadState` TEXT NOT NULL, `uploadAttemptedAt` INTEGER NOT NULL, `uploadAttempts` INTEGER NOT NULL, `uploadError` TEXT NOT NULL, `verdict` TEXT NOT NULL, `claimSignature` TEXT NOT NULL, `claimSessionHash` TEXT NOT NULL, `claimAmount` TEXT NOT NULL, `claimDay` INTEGER NOT NULL, `claimDeadline` INTEGER NOT NULL, `mockLocation` INTEGER NOT NULL DEFAULT 0, `laps` TEXT NOT NULL DEFAULT '')")
        connection.execSQL("CREATE TABLE IF NOT EXISTS `rewards` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `timestamp` INTEGER NOT NULL, `type` TEXT NOT NULL, `amount` REAL NOT NULL, `description` TEXT NOT NULL)")
        connection.execSQL("CREATE TABLE IF NOT EXISTS `sneakers` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `factionId` TEXT NOT NULL, `rarity` TEXT NOT NULL, `variant` INTEGER NOT NULL, `level` INTEGER NOT NULL, `mintNumber` INTEGER NOT NULL, `luck` REAL NOT NULL, `comfort` REAL NOT NULL, `durability` INTEGER NOT NULL, `equipped` INTEGER NOT NULL, `acquiredAt` INTEGER NOT NULL, `serverId` INTEGER NOT NULL, `origin` TEXT NOT NULL DEFAULT '', `efficiencyBps` INTEGER NOT NULL DEFAULT 0, `comfortBps` INTEGER NOT NULL DEFAULT 0, `durabilityPts` REAL NOT NULL DEFAULT 100, `maxLevel` INTEGER NOT NULL DEFAULT 0, `status` TEXT NOT NULL DEFAULT 'OWNED', `chainState` TEXT NOT NULL DEFAULT 'APP', `canWithdraw` INTEGER NOT NULL DEFAULT 0, `serverUpgradeCost` REAL NOT NULL DEFAULT 0, `repairCostPerPoint` REAL NOT NULL DEFAULT 0, `genesisNo` INTEGER NOT NULL DEFAULT 0, `modelId` INTEGER NOT NULL DEFAULT 0, `tokenId` INTEGER NOT NULL DEFAULT 0, `durabilityNow` INTEGER NOT NULL DEFAULT 0, `durabilityMax` INTEGER NOT NULL DEFAULT 0, `durabilityBase` INTEGER NOT NULL DEFAULT 0, `efficiencyBaseBps` INTEGER NOT NULL DEFAULT 0, `comfortBaseBps` INTEGER NOT NULL DEFAULT 0, `pointsTotal` INTEGER NOT NULL DEFAULT 0, `pointsFree` INTEGER NOT NULL DEFAULT 0, `pointsEfficiency` INTEGER NOT NULL DEFAULT 0, `pointsComfort` INTEGER NOT NULL DEFAULT 0, `pointsDurability` INTEGER NOT NULL DEFAULT 0, `pointsRespec` INTEGER NOT NULL DEFAULT 0, `repairCostPerUnit` REAL NOT NULL DEFAULT 0)")
        connection.execSQL("CREATE TABLE IF NOT EXISTS `boosts` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `type` TEXT NOT NULL, `activatedAt` INTEGER NOT NULL, `expiresAt` INTEGER NOT NULL)")
        connection.execSQL("CREATE TABLE IF NOT EXISTS `energy_purchases` (`id` TEXT NOT NULL, `createdAt` INTEGER NOT NULL, `amount` REAL NOT NULL, `delivered` INTEGER NOT NULL, PRIMARY KEY(`id`))")
        connection.execSQL("CREATE TABLE IF NOT EXISTS `run_settlements` (`recordingOwner` TEXT NOT NULL, `startedAt` INTEGER NOT NULL, `energyDay` INTEGER NOT NULL, `rewardedSteps` INTEGER NOT NULL, `points` REAL NOT NULL, `energyUsed` REAL NOT NULL, `energyApplied` INTEGER NOT NULL, PRIMARY KEY(`recordingOwner`, `startedAt`))")
        connection.execSQL("CREATE TABLE IF NOT EXISTS `claimed_events` (`eventId` TEXT NOT NULL, `claimedAt` INTEGER NOT NULL, `amount` REAL NOT NULL, PRIMARY KEY(`eventId`))")
        connection.execSQL("CREATE TABLE IF NOT EXISTS `crew_memberships` (`crewId` TEXT NOT NULL, `joinedAt` INTEGER NOT NULL, PRIMARY KEY(`crewId`))")
        connection.execSQL("CREATE TABLE IF NOT EXISTS `crews` (`id` TEXT NOT NULL, `name` TEXT NOT NULL, `monogram` TEXT NOT NULL, `tagline` TEXT NOT NULL, `area` TEXT NOT NULL, `kmAway` REAL NOT NULL, `memberCount` INTEGER NOT NULL, `roster` TEXT NOT NULL, `createdAt` INTEGER NOT NULL, `owned` INTEGER NOT NULL, PRIMARY KEY(`id`))")
        connection.execSQL("CREATE TABLE IF NOT EXISTS `posts` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `category` TEXT NOT NULL, `crewId` TEXT NOT NULL, `author` TEXT NOT NULL, `title` TEXT NOT NULL, `body` TEXT NOT NULL, `createdAt` INTEGER NOT NULL, `likes` INTEGER NOT NULL, `liked` INTEGER NOT NULL, `commentCount` INTEGER NOT NULL, `mine` INTEGER NOT NULL, `place` TEXT NOT NULL, `distanceKm` REAL NOT NULL, `meetAt` INTEGER NOT NULL, `capacity` INTEGER NOT NULL, `joinedCount` INTEGER NOT NULL, `joined` INTEGER NOT NULL)")
        connection.execSQL("CREATE TABLE IF NOT EXISTS `comments` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `postId` INTEGER NOT NULL, `parentId` INTEGER NOT NULL, `author` TEXT NOT NULL, `body` TEXT NOT NULL, `createdAt` INTEGER NOT NULL, `mine` INTEGER NOT NULL)")
        connection.execSQL("CREATE TABLE IF NOT EXISTS `courses` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `name` TEXT NOT NULL, `area` TEXT NOT NULL, `distanceKm` REAL NOT NULL, `elevationM` INTEGER NOT NULL, `track` TEXT NOT NULL, `author` TEXT NOT NULL, `mine` INTEGER NOT NULL, `shared` INTEGER NOT NULL, `likes` INTEGER NOT NULL, `liked` INTEGER NOT NULL, `runCount` INTEGER NOT NULL, `createdAt` INTEGER NOT NULL)")
        connection.execSQL("CREATE TABLE IF NOT EXISTS `notifications` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `timestamp` INTEGER NOT NULL, `type` TEXT NOT NULL, `argText` TEXT NOT NULL, `argAmount` REAL NOT NULL, `argExtra` TEXT NOT NULL, `read` INTEGER NOT NULL, `actioned` INTEGER NOT NULL)")
        connection.execSQL("CREATE TABLE IF NOT EXISTS `news_items` (`url` TEXT NOT NULL, `title` TEXT NOT NULL, `source` TEXT NOT NULL, `summary` TEXT NOT NULL, `kind` TEXT NOT NULL, `publishedAt` INTEGER NOT NULL, PRIMARY KEY(`url`))")
        connection.execSQL("CREATE TABLE IF NOT EXISTS room_master_table (id INTEGER PRIMARY KEY,identity_hash TEXT)")
        connection.execSQL("INSERT OR REPLACE INTO room_master_table (id,identity_hash) VALUES(42, 'f867391f2970e890288c861ee52acaf8')")
      }

      public override fun dropAllTables(connection: SQLiteConnection) {
        connection.execSQL("DROP TABLE IF EXISTS `daily_steps`")
        connection.execSQL("DROP TABLE IF EXISTS `walk_sessions`")
        connection.execSQL("DROP TABLE IF EXISTS `rewards`")
        connection.execSQL("DROP TABLE IF EXISTS `sneakers`")
        connection.execSQL("DROP TABLE IF EXISTS `boosts`")
        connection.execSQL("DROP TABLE IF EXISTS `energy_purchases`")
        connection.execSQL("DROP TABLE IF EXISTS `run_settlements`")
        connection.execSQL("DROP TABLE IF EXISTS `claimed_events`")
        connection.execSQL("DROP TABLE IF EXISTS `crew_memberships`")
        connection.execSQL("DROP TABLE IF EXISTS `crews`")
        connection.execSQL("DROP TABLE IF EXISTS `posts`")
        connection.execSQL("DROP TABLE IF EXISTS `comments`")
        connection.execSQL("DROP TABLE IF EXISTS `courses`")
        connection.execSQL("DROP TABLE IF EXISTS `notifications`")
        connection.execSQL("DROP TABLE IF EXISTS `news_items`")
      }

      public override fun onCreate(connection: SQLiteConnection) {
      }

      public override fun onOpen(connection: SQLiteConnection) {
        internalInitInvalidationTracker(connection)
      }

      public override fun onPreMigrate(connection: SQLiteConnection) {
        dropFtsSyncTriggers(connection)
      }

      public override fun onPostMigrate(connection: SQLiteConnection) {
      }

      public override fun onValidateSchema(connection: SQLiteConnection): RoomOpenDelegate.ValidationResult {
        val _columnsDailySteps: MutableMap<String, TableInfo.Column> = mutableMapOf()
        _columnsDailySteps.put("epochDay", TableInfo.Column("epochDay", "INTEGER", true, 1, null, TableInfo.CREATED_FROM_ENTITY))
        _columnsDailySteps.put("steps", TableInfo.Column("steps", "INTEGER", true, 0, null, TableInfo.CREATED_FROM_ENTITY))
        _columnsDailySteps.put("goal", TableInfo.Column("goal", "INTEGER", true, 0, null, TableInfo.CREATED_FROM_ENTITY))
        _columnsDailySteps.put("updatedAt", TableInfo.Column("updatedAt", "INTEGER", true, 0, null, TableInfo.CREATED_FROM_ENTITY))
        val _foreignKeysDailySteps: MutableSet<TableInfo.ForeignKey> = mutableSetOf()
        val _indicesDailySteps: MutableSet<TableInfo.Index> = mutableSetOf()
        val _infoDailySteps: TableInfo = TableInfo("daily_steps", _columnsDailySteps, _foreignKeysDailySteps, _indicesDailySteps)
        val _existingDailySteps: TableInfo = read(connection, "daily_steps")
        if (!_infoDailySteps.equals(_existingDailySteps)) {
          return RoomOpenDelegate.ValidationResult(false, """
              |daily_steps(com.stepup.android.data.local.DailyStepsEntity).
              | Expected:
              |""".trimMargin() + _infoDailySteps + """
              |
              | Found:
              |""".trimMargin() + _existingDailySteps)
        }
        val _columnsWalkSessions: MutableMap<String, TableInfo.Column> = mutableMapOf()
        _columnsWalkSessions.put("id", TableInfo.Column("id", "INTEGER", true, 1, null, TableInfo.CREATED_FROM_ENTITY))
        _columnsWalkSessions.put("startedAt", TableInfo.Column("startedAt", "INTEGER", true, 0, null, TableInfo.CREATED_FROM_ENTITY))
        _columnsWalkSessions.put("endedAt", TableInfo.Column("endedAt", "INTEGER", true, 0, null, TableInfo.CREATED_FROM_ENTITY))
        _columnsWalkSessions.put("steps", TableInfo.Column("steps", "INTEGER", true, 0, null, TableInfo.CREATED_FROM_ENTITY))
        _columnsWalkSessions.put("durationSec", TableInfo.Column("durationSec", "INTEGER", true, 0, null, TableInfo.CREATED_FROM_ENTITY))
        _columnsWalkSessions.put("distanceMeters", TableInfo.Column("distanceMeters", "REAL", true, 0, null, TableInfo.CREATED_FROM_ENTITY))
        _columnsWalkSessions.put("calories", TableInfo.Column("calories", "REAL", true, 0, null, TableInfo.CREATED_FROM_ENTITY))
        _columnsWalkSessions.put("pointsEarned", TableInfo.Column("pointsEarned", "REAL", true, 0, null, TableInfo.CREATED_FROM_ENTITY))
        _columnsWalkSessions.put("recordingOwner", TableInfo.Column("recordingOwner", "TEXT", true, 0, "'legacy'", TableInfo.CREATED_FROM_ENTITY))
        _columnsWalkSessions.put("track", TableInfo.Column("track", "TEXT", true, 0, null, TableInfo.CREATED_FROM_ENTITY))
        _columnsWalkSessions.put("boostBps", TableInfo.Column("boostBps", "INTEGER", true, 0, null, TableInfo.CREATED_FROM_ENTITY))
        _columnsWalkSessions.put("partySize", TableInfo.Column("partySize", "INTEGER", true, 0, null, TableInfo.CREATED_FROM_ENTITY))
        _columnsWalkSessions.put("faction", TableInfo.Column("faction", "TEXT", true, 0, null, TableInfo.CREATED_FROM_ENTITY))
        _columnsWalkSessions.put("crewId", TableInfo.Column("crewId", "TEXT", true, 0, null, TableInfo.CREATED_FROM_ENTITY))
        _columnsWalkSessions.put("uploadState", TableInfo.Column("uploadState", "TEXT", true, 0, null, TableInfo.CREATED_FROM_ENTITY))
        _columnsWalkSessions.put("uploadAttemptedAt", TableInfo.Column("uploadAttemptedAt", "INTEGER", true, 0, null, TableInfo.CREATED_FROM_ENTITY))
        _columnsWalkSessions.put("uploadAttempts", TableInfo.Column("uploadAttempts", "INTEGER", true, 0, null, TableInfo.CREATED_FROM_ENTITY))
        _columnsWalkSessions.put("uploadError", TableInfo.Column("uploadError", "TEXT", true, 0, null, TableInfo.CREATED_FROM_ENTITY))
        _columnsWalkSessions.put("verdict", TableInfo.Column("verdict", "TEXT", true, 0, null, TableInfo.CREATED_FROM_ENTITY))
        _columnsWalkSessions.put("claimSignature", TableInfo.Column("claimSignature", "TEXT", true, 0, null, TableInfo.CREATED_FROM_ENTITY))
        _columnsWalkSessions.put("claimSessionHash", TableInfo.Column("claimSessionHash", "TEXT", true, 0, null, TableInfo.CREATED_FROM_ENTITY))
        _columnsWalkSessions.put("claimAmount", TableInfo.Column("claimAmount", "TEXT", true, 0, null, TableInfo.CREATED_FROM_ENTITY))
        _columnsWalkSessions.put("claimDay", TableInfo.Column("claimDay", "INTEGER", true, 0, null, TableInfo.CREATED_FROM_ENTITY))
        _columnsWalkSessions.put("claimDeadline", TableInfo.Column("claimDeadline", "INTEGER", true, 0, null, TableInfo.CREATED_FROM_ENTITY))
        _columnsWalkSessions.put("mockLocation", TableInfo.Column("mockLocation", "INTEGER", true, 0, "0", TableInfo.CREATED_FROM_ENTITY))
        _columnsWalkSessions.put("laps", TableInfo.Column("laps", "TEXT", true, 0, "''", TableInfo.CREATED_FROM_ENTITY))
        val _foreignKeysWalkSessions: MutableSet<TableInfo.ForeignKey> = mutableSetOf()
        val _indicesWalkSessions: MutableSet<TableInfo.Index> = mutableSetOf()
        val _infoWalkSessions: TableInfo = TableInfo("walk_sessions", _columnsWalkSessions, _foreignKeysWalkSessions, _indicesWalkSessions)
        val _existingWalkSessions: TableInfo = read(connection, "walk_sessions")
        if (!_infoWalkSessions.equals(_existingWalkSessions)) {
          return RoomOpenDelegate.ValidationResult(false, """
              |walk_sessions(com.stepup.android.data.local.WalkSessionEntity).
              | Expected:
              |""".trimMargin() + _infoWalkSessions + """
              |
              | Found:
              |""".trimMargin() + _existingWalkSessions)
        }
        val _columnsRewards: MutableMap<String, TableInfo.Column> = mutableMapOf()
        _columnsRewards.put("id", TableInfo.Column("id", "INTEGER", true, 1, null, TableInfo.CREATED_FROM_ENTITY))
        _columnsRewards.put("timestamp", TableInfo.Column("timestamp", "INTEGER", true, 0, null, TableInfo.CREATED_FROM_ENTITY))
        _columnsRewards.put("type", TableInfo.Column("type", "TEXT", true, 0, null, TableInfo.CREATED_FROM_ENTITY))
        _columnsRewards.put("amount", TableInfo.Column("amount", "REAL", true, 0, null, TableInfo.CREATED_FROM_ENTITY))
        _columnsRewards.put("description", TableInfo.Column("description", "TEXT", true, 0, null, TableInfo.CREATED_FROM_ENTITY))
        val _foreignKeysRewards: MutableSet<TableInfo.ForeignKey> = mutableSetOf()
        val _indicesRewards: MutableSet<TableInfo.Index> = mutableSetOf()
        val _infoRewards: TableInfo = TableInfo("rewards", _columnsRewards, _foreignKeysRewards, _indicesRewards)
        val _existingRewards: TableInfo = read(connection, "rewards")
        if (!_infoRewards.equals(_existingRewards)) {
          return RoomOpenDelegate.ValidationResult(false, """
              |rewards(com.stepup.android.data.local.RewardEntity).
              | Expected:
              |""".trimMargin() + _infoRewards + """
              |
              | Found:
              |""".trimMargin() + _existingRewards)
        }
        val _columnsSneakers: MutableMap<String, TableInfo.Column> = mutableMapOf()
        _columnsSneakers.put("id", TableInfo.Column("id", "INTEGER", true, 1, null, TableInfo.CREATED_FROM_ENTITY))
        _columnsSneakers.put("factionId", TableInfo.Column("factionId", "TEXT", true, 0, null, TableInfo.CREATED_FROM_ENTITY))
        _columnsSneakers.put("rarity", TableInfo.Column("rarity", "TEXT", true, 0, null, TableInfo.CREATED_FROM_ENTITY))
        _columnsSneakers.put("variant", TableInfo.Column("variant", "INTEGER", true, 0, null, TableInfo.CREATED_FROM_ENTITY))
        _columnsSneakers.put("level", TableInfo.Column("level", "INTEGER", true, 0, null, TableInfo.CREATED_FROM_ENTITY))
        _columnsSneakers.put("mintNumber", TableInfo.Column("mintNumber", "INTEGER", true, 0, null, TableInfo.CREATED_FROM_ENTITY))
        _columnsSneakers.put("luck", TableInfo.Column("luck", "REAL", true, 0, null, TableInfo.CREATED_FROM_ENTITY))
        _columnsSneakers.put("comfort", TableInfo.Column("comfort", "REAL", true, 0, null, TableInfo.CREATED_FROM_ENTITY))
        _columnsSneakers.put("durability", TableInfo.Column("durability", "INTEGER", true, 0, null, TableInfo.CREATED_FROM_ENTITY))
        _columnsSneakers.put("equipped", TableInfo.Column("equipped", "INTEGER", true, 0, null, TableInfo.CREATED_FROM_ENTITY))
        _columnsSneakers.put("acquiredAt", TableInfo.Column("acquiredAt", "INTEGER", true, 0, null, TableInfo.CREATED_FROM_ENTITY))
        _columnsSneakers.put("serverId", TableInfo.Column("serverId", "INTEGER", true, 0, null, TableInfo.CREATED_FROM_ENTITY))
        _columnsSneakers.put("origin", TableInfo.Column("origin", "TEXT", true, 0, "''", TableInfo.CREATED_FROM_ENTITY))
        _columnsSneakers.put("efficiencyBps", TableInfo.Column("efficiencyBps", "INTEGER", true, 0, "0", TableInfo.CREATED_FROM_ENTITY))
        _columnsSneakers.put("comfortBps", TableInfo.Column("comfortBps", "INTEGER", true, 0, "0", TableInfo.CREATED_FROM_ENTITY))
        _columnsSneakers.put("durabilityPts", TableInfo.Column("durabilityPts", "REAL", true, 0, "100", TableInfo.CREATED_FROM_ENTITY))
        _columnsSneakers.put("maxLevel", TableInfo.Column("maxLevel", "INTEGER", true, 0, "0", TableInfo.CREATED_FROM_ENTITY))
        _columnsSneakers.put("status", TableInfo.Column("status", "TEXT", true, 0, "'OWNED'", TableInfo.CREATED_FROM_ENTITY))
        _columnsSneakers.put("chainState", TableInfo.Column("chainState", "TEXT", true, 0, "'APP'", TableInfo.CREATED_FROM_ENTITY))
        _columnsSneakers.put("canWithdraw", TableInfo.Column("canWithdraw", "INTEGER", true, 0, "0", TableInfo.CREATED_FROM_ENTITY))
        _columnsSneakers.put("serverUpgradeCost", TableInfo.Column("serverUpgradeCost", "REAL", true, 0, "0", TableInfo.CREATED_FROM_ENTITY))
        _columnsSneakers.put("repairCostPerPoint", TableInfo.Column("repairCostPerPoint", "REAL", true, 0, "0", TableInfo.CREATED_FROM_ENTITY))
        _columnsSneakers.put("genesisNo", TableInfo.Column("genesisNo", "INTEGER", true, 0, "0", TableInfo.CREATED_FROM_ENTITY))
        _columnsSneakers.put("modelId", TableInfo.Column("modelId", "INTEGER", true, 0, "0", TableInfo.CREATED_FROM_ENTITY))
        _columnsSneakers.put("tokenId", TableInfo.Column("tokenId", "INTEGER", true, 0, "0", TableInfo.CREATED_FROM_ENTITY))
        _columnsSneakers.put("durabilityNow", TableInfo.Column("durabilityNow", "INTEGER", true, 0, "0", TableInfo.CREATED_FROM_ENTITY))
        _columnsSneakers.put("durabilityMax", TableInfo.Column("durabilityMax", "INTEGER", true, 0, "0", TableInfo.CREATED_FROM_ENTITY))
        _columnsSneakers.put("durabilityBase", TableInfo.Column("durabilityBase", "INTEGER", true, 0, "0", TableInfo.CREATED_FROM_ENTITY))
        _columnsSneakers.put("efficiencyBaseBps", TableInfo.Column("efficiencyBaseBps", "INTEGER", true, 0, "0", TableInfo.CREATED_FROM_ENTITY))
        _columnsSneakers.put("comfortBaseBps", TableInfo.Column("comfortBaseBps", "INTEGER", true, 0, "0", TableInfo.CREATED_FROM_ENTITY))
        _columnsSneakers.put("pointsTotal", TableInfo.Column("pointsTotal", "INTEGER", true, 0, "0", TableInfo.CREATED_FROM_ENTITY))
        _columnsSneakers.put("pointsFree", TableInfo.Column("pointsFree", "INTEGER", true, 0, "0", TableInfo.CREATED_FROM_ENTITY))
        _columnsSneakers.put("pointsEfficiency", TableInfo.Column("pointsEfficiency", "INTEGER", true, 0, "0", TableInfo.CREATED_FROM_ENTITY))
        _columnsSneakers.put("pointsComfort", TableInfo.Column("pointsComfort", "INTEGER", true, 0, "0", TableInfo.CREATED_FROM_ENTITY))
        _columnsSneakers.put("pointsDurability", TableInfo.Column("pointsDurability", "INTEGER", true, 0, "0", TableInfo.CREATED_FROM_ENTITY))
        _columnsSneakers.put("pointsRespec", TableInfo.Column("pointsRespec", "INTEGER", true, 0, "0", TableInfo.CREATED_FROM_ENTITY))
        _columnsSneakers.put("repairCostPerUnit", TableInfo.Column("repairCostPerUnit", "REAL", true, 0, "0", TableInfo.CREATED_FROM_ENTITY))
        val _foreignKeysSneakers: MutableSet<TableInfo.ForeignKey> = mutableSetOf()
        val _indicesSneakers: MutableSet<TableInfo.Index> = mutableSetOf()
        val _infoSneakers: TableInfo = TableInfo("sneakers", _columnsSneakers, _foreignKeysSneakers, _indicesSneakers)
        val _existingSneakers: TableInfo = read(connection, "sneakers")
        if (!_infoSneakers.equals(_existingSneakers)) {
          return RoomOpenDelegate.ValidationResult(false, """
              |sneakers(com.stepup.android.data.local.SneakerEntity).
              | Expected:
              |""".trimMargin() + _infoSneakers + """
              |
              | Found:
              |""".trimMargin() + _existingSneakers)
        }
        val _columnsBoosts: MutableMap<String, TableInfo.Column> = mutableMapOf()
        _columnsBoosts.put("id", TableInfo.Column("id", "INTEGER", true, 1, null, TableInfo.CREATED_FROM_ENTITY))
        _columnsBoosts.put("type", TableInfo.Column("type", "TEXT", true, 0, null, TableInfo.CREATED_FROM_ENTITY))
        _columnsBoosts.put("activatedAt", TableInfo.Column("activatedAt", "INTEGER", true, 0, null, TableInfo.CREATED_FROM_ENTITY))
        _columnsBoosts.put("expiresAt", TableInfo.Column("expiresAt", "INTEGER", true, 0, null, TableInfo.CREATED_FROM_ENTITY))
        val _foreignKeysBoosts: MutableSet<TableInfo.ForeignKey> = mutableSetOf()
        val _indicesBoosts: MutableSet<TableInfo.Index> = mutableSetOf()
        val _infoBoosts: TableInfo = TableInfo("boosts", _columnsBoosts, _foreignKeysBoosts, _indicesBoosts)
        val _existingBoosts: TableInfo = read(connection, "boosts")
        if (!_infoBoosts.equals(_existingBoosts)) {
          return RoomOpenDelegate.ValidationResult(false, """
              |boosts(com.stepup.android.data.local.BoostEntity).
              | Expected:
              |""".trimMargin() + _infoBoosts + """
              |
              | Found:
              |""".trimMargin() + _existingBoosts)
        }
        val _columnsEnergyPurchases: MutableMap<String, TableInfo.Column> = mutableMapOf()
        _columnsEnergyPurchases.put("id", TableInfo.Column("id", "TEXT", true, 1, null, TableInfo.CREATED_FROM_ENTITY))
        _columnsEnergyPurchases.put("createdAt", TableInfo.Column("createdAt", "INTEGER", true, 0, null, TableInfo.CREATED_FROM_ENTITY))
        _columnsEnergyPurchases.put("amount", TableInfo.Column("amount", "REAL", true, 0, null, TableInfo.CREATED_FROM_ENTITY))
        _columnsEnergyPurchases.put("delivered", TableInfo.Column("delivered", "INTEGER", true, 0, null, TableInfo.CREATED_FROM_ENTITY))
        val _foreignKeysEnergyPurchases: MutableSet<TableInfo.ForeignKey> = mutableSetOf()
        val _indicesEnergyPurchases: MutableSet<TableInfo.Index> = mutableSetOf()
        val _infoEnergyPurchases: TableInfo = TableInfo("energy_purchases", _columnsEnergyPurchases, _foreignKeysEnergyPurchases, _indicesEnergyPurchases)
        val _existingEnergyPurchases: TableInfo = read(connection, "energy_purchases")
        if (!_infoEnergyPurchases.equals(_existingEnergyPurchases)) {
          return RoomOpenDelegate.ValidationResult(false, """
              |energy_purchases(com.stepup.android.data.local.EnergyPurchase).
              | Expected:
              |""".trimMargin() + _infoEnergyPurchases + """
              |
              | Found:
              |""".trimMargin() + _existingEnergyPurchases)
        }
        val _columnsRunSettlements: MutableMap<String, TableInfo.Column> = mutableMapOf()
        _columnsRunSettlements.put("recordingOwner", TableInfo.Column("recordingOwner", "TEXT", true, 1, null, TableInfo.CREATED_FROM_ENTITY))
        _columnsRunSettlements.put("startedAt", TableInfo.Column("startedAt", "INTEGER", true, 2, null, TableInfo.CREATED_FROM_ENTITY))
        _columnsRunSettlements.put("energyDay", TableInfo.Column("energyDay", "INTEGER", true, 0, null, TableInfo.CREATED_FROM_ENTITY))
        _columnsRunSettlements.put("rewardedSteps", TableInfo.Column("rewardedSteps", "INTEGER", true, 0, null, TableInfo.CREATED_FROM_ENTITY))
        _columnsRunSettlements.put("points", TableInfo.Column("points", "REAL", true, 0, null, TableInfo.CREATED_FROM_ENTITY))
        _columnsRunSettlements.put("energyUsed", TableInfo.Column("energyUsed", "REAL", true, 0, null, TableInfo.CREATED_FROM_ENTITY))
        _columnsRunSettlements.put("energyApplied", TableInfo.Column("energyApplied", "INTEGER", true, 0, null, TableInfo.CREATED_FROM_ENTITY))
        val _foreignKeysRunSettlements: MutableSet<TableInfo.ForeignKey> = mutableSetOf()
        val _indicesRunSettlements: MutableSet<TableInfo.Index> = mutableSetOf()
        val _infoRunSettlements: TableInfo = TableInfo("run_settlements", _columnsRunSettlements, _foreignKeysRunSettlements, _indicesRunSettlements)
        val _existingRunSettlements: TableInfo = read(connection, "run_settlements")
        if (!_infoRunSettlements.equals(_existingRunSettlements)) {
          return RoomOpenDelegate.ValidationResult(false, """
              |run_settlements(com.stepup.android.data.local.RunSettlement).
              | Expected:
              |""".trimMargin() + _infoRunSettlements + """
              |
              | Found:
              |""".trimMargin() + _existingRunSettlements)
        }
        val _columnsClaimedEvents: MutableMap<String, TableInfo.Column> = mutableMapOf()
        _columnsClaimedEvents.put("eventId", TableInfo.Column("eventId", "TEXT", true, 1, null, TableInfo.CREATED_FROM_ENTITY))
        _columnsClaimedEvents.put("claimedAt", TableInfo.Column("claimedAt", "INTEGER", true, 0, null, TableInfo.CREATED_FROM_ENTITY))
        _columnsClaimedEvents.put("amount", TableInfo.Column("amount", "REAL", true, 0, null, TableInfo.CREATED_FROM_ENTITY))
        val _foreignKeysClaimedEvents: MutableSet<TableInfo.ForeignKey> = mutableSetOf()
        val _indicesClaimedEvents: MutableSet<TableInfo.Index> = mutableSetOf()
        val _infoClaimedEvents: TableInfo = TableInfo("claimed_events", _columnsClaimedEvents, _foreignKeysClaimedEvents, _indicesClaimedEvents)
        val _existingClaimedEvents: TableInfo = read(connection, "claimed_events")
        if (!_infoClaimedEvents.equals(_existingClaimedEvents)) {
          return RoomOpenDelegate.ValidationResult(false, """
              |claimed_events(com.stepup.android.data.local.ClaimedEventEntity).
              | Expected:
              |""".trimMargin() + _infoClaimedEvents + """
              |
              | Found:
              |""".trimMargin() + _existingClaimedEvents)
        }
        val _columnsCrewMemberships: MutableMap<String, TableInfo.Column> = mutableMapOf()
        _columnsCrewMemberships.put("crewId", TableInfo.Column("crewId", "TEXT", true, 1, null, TableInfo.CREATED_FROM_ENTITY))
        _columnsCrewMemberships.put("joinedAt", TableInfo.Column("joinedAt", "INTEGER", true, 0, null, TableInfo.CREATED_FROM_ENTITY))
        val _foreignKeysCrewMemberships: MutableSet<TableInfo.ForeignKey> = mutableSetOf()
        val _indicesCrewMemberships: MutableSet<TableInfo.Index> = mutableSetOf()
        val _infoCrewMemberships: TableInfo = TableInfo("crew_memberships", _columnsCrewMemberships, _foreignKeysCrewMemberships, _indicesCrewMemberships)
        val _existingCrewMemberships: TableInfo = read(connection, "crew_memberships")
        if (!_infoCrewMemberships.equals(_existingCrewMemberships)) {
          return RoomOpenDelegate.ValidationResult(false, """
              |crew_memberships(com.stepup.android.data.local.CrewMembershipEntity).
              | Expected:
              |""".trimMargin() + _infoCrewMemberships + """
              |
              | Found:
              |""".trimMargin() + _existingCrewMemberships)
        }
        val _columnsCrews: MutableMap<String, TableInfo.Column> = mutableMapOf()
        _columnsCrews.put("id", TableInfo.Column("id", "TEXT", true, 1, null, TableInfo.CREATED_FROM_ENTITY))
        _columnsCrews.put("name", TableInfo.Column("name", "TEXT", true, 0, null, TableInfo.CREATED_FROM_ENTITY))
        _columnsCrews.put("monogram", TableInfo.Column("monogram", "TEXT", true, 0, null, TableInfo.CREATED_FROM_ENTITY))
        _columnsCrews.put("tagline", TableInfo.Column("tagline", "TEXT", true, 0, null, TableInfo.CREATED_FROM_ENTITY))
        _columnsCrews.put("area", TableInfo.Column("area", "TEXT", true, 0, null, TableInfo.CREATED_FROM_ENTITY))
        _columnsCrews.put("kmAway", TableInfo.Column("kmAway", "REAL", true, 0, null, TableInfo.CREATED_FROM_ENTITY))
        _columnsCrews.put("memberCount", TableInfo.Column("memberCount", "INTEGER", true, 0, null, TableInfo.CREATED_FROM_ENTITY))
        _columnsCrews.put("roster", TableInfo.Column("roster", "TEXT", true, 0, null, TableInfo.CREATED_FROM_ENTITY))
        _columnsCrews.put("createdAt", TableInfo.Column("createdAt", "INTEGER", true, 0, null, TableInfo.CREATED_FROM_ENTITY))
        _columnsCrews.put("owned", TableInfo.Column("owned", "INTEGER", true, 0, null, TableInfo.CREATED_FROM_ENTITY))
        val _foreignKeysCrews: MutableSet<TableInfo.ForeignKey> = mutableSetOf()
        val _indicesCrews: MutableSet<TableInfo.Index> = mutableSetOf()
        val _infoCrews: TableInfo = TableInfo("crews", _columnsCrews, _foreignKeysCrews, _indicesCrews)
        val _existingCrews: TableInfo = read(connection, "crews")
        if (!_infoCrews.equals(_existingCrews)) {
          return RoomOpenDelegate.ValidationResult(false, """
              |crews(com.stepup.android.data.local.CrewEntity).
              | Expected:
              |""".trimMargin() + _infoCrews + """
              |
              | Found:
              |""".trimMargin() + _existingCrews)
        }
        val _columnsPosts: MutableMap<String, TableInfo.Column> = mutableMapOf()
        _columnsPosts.put("id", TableInfo.Column("id", "INTEGER", true, 1, null, TableInfo.CREATED_FROM_ENTITY))
        _columnsPosts.put("category", TableInfo.Column("category", "TEXT", true, 0, null, TableInfo.CREATED_FROM_ENTITY))
        _columnsPosts.put("crewId", TableInfo.Column("crewId", "TEXT", true, 0, null, TableInfo.CREATED_FROM_ENTITY))
        _columnsPosts.put("author", TableInfo.Column("author", "TEXT", true, 0, null, TableInfo.CREATED_FROM_ENTITY))
        _columnsPosts.put("title", TableInfo.Column("title", "TEXT", true, 0, null, TableInfo.CREATED_FROM_ENTITY))
        _columnsPosts.put("body", TableInfo.Column("body", "TEXT", true, 0, null, TableInfo.CREATED_FROM_ENTITY))
        _columnsPosts.put("createdAt", TableInfo.Column("createdAt", "INTEGER", true, 0, null, TableInfo.CREATED_FROM_ENTITY))
        _columnsPosts.put("likes", TableInfo.Column("likes", "INTEGER", true, 0, null, TableInfo.CREATED_FROM_ENTITY))
        _columnsPosts.put("liked", TableInfo.Column("liked", "INTEGER", true, 0, null, TableInfo.CREATED_FROM_ENTITY))
        _columnsPosts.put("commentCount", TableInfo.Column("commentCount", "INTEGER", true, 0, null, TableInfo.CREATED_FROM_ENTITY))
        _columnsPosts.put("mine", TableInfo.Column("mine", "INTEGER", true, 0, null, TableInfo.CREATED_FROM_ENTITY))
        _columnsPosts.put("place", TableInfo.Column("place", "TEXT", true, 0, null, TableInfo.CREATED_FROM_ENTITY))
        _columnsPosts.put("distanceKm", TableInfo.Column("distanceKm", "REAL", true, 0, null, TableInfo.CREATED_FROM_ENTITY))
        _columnsPosts.put("meetAt", TableInfo.Column("meetAt", "INTEGER", true, 0, null, TableInfo.CREATED_FROM_ENTITY))
        _columnsPosts.put("capacity", TableInfo.Column("capacity", "INTEGER", true, 0, null, TableInfo.CREATED_FROM_ENTITY))
        _columnsPosts.put("joinedCount", TableInfo.Column("joinedCount", "INTEGER", true, 0, null, TableInfo.CREATED_FROM_ENTITY))
        _columnsPosts.put("joined", TableInfo.Column("joined", "INTEGER", true, 0, null, TableInfo.CREATED_FROM_ENTITY))
        val _foreignKeysPosts: MutableSet<TableInfo.ForeignKey> = mutableSetOf()
        val _indicesPosts: MutableSet<TableInfo.Index> = mutableSetOf()
        val _infoPosts: TableInfo = TableInfo("posts", _columnsPosts, _foreignKeysPosts, _indicesPosts)
        val _existingPosts: TableInfo = read(connection, "posts")
        if (!_infoPosts.equals(_existingPosts)) {
          return RoomOpenDelegate.ValidationResult(false, """
              |posts(com.stepup.android.data.local.PostEntity).
              | Expected:
              |""".trimMargin() + _infoPosts + """
              |
              | Found:
              |""".trimMargin() + _existingPosts)
        }
        val _columnsComments: MutableMap<String, TableInfo.Column> = mutableMapOf()
        _columnsComments.put("id", TableInfo.Column("id", "INTEGER", true, 1, null, TableInfo.CREATED_FROM_ENTITY))
        _columnsComments.put("postId", TableInfo.Column("postId", "INTEGER", true, 0, null, TableInfo.CREATED_FROM_ENTITY))
        _columnsComments.put("parentId", TableInfo.Column("parentId", "INTEGER", true, 0, null, TableInfo.CREATED_FROM_ENTITY))
        _columnsComments.put("author", TableInfo.Column("author", "TEXT", true, 0, null, TableInfo.CREATED_FROM_ENTITY))
        _columnsComments.put("body", TableInfo.Column("body", "TEXT", true, 0, null, TableInfo.CREATED_FROM_ENTITY))
        _columnsComments.put("createdAt", TableInfo.Column("createdAt", "INTEGER", true, 0, null, TableInfo.CREATED_FROM_ENTITY))
        _columnsComments.put("mine", TableInfo.Column("mine", "INTEGER", true, 0, null, TableInfo.CREATED_FROM_ENTITY))
        val _foreignKeysComments: MutableSet<TableInfo.ForeignKey> = mutableSetOf()
        val _indicesComments: MutableSet<TableInfo.Index> = mutableSetOf()
        val _infoComments: TableInfo = TableInfo("comments", _columnsComments, _foreignKeysComments, _indicesComments)
        val _existingComments: TableInfo = read(connection, "comments")
        if (!_infoComments.equals(_existingComments)) {
          return RoomOpenDelegate.ValidationResult(false, """
              |comments(com.stepup.android.data.local.CommentEntity).
              | Expected:
              |""".trimMargin() + _infoComments + """
              |
              | Found:
              |""".trimMargin() + _existingComments)
        }
        val _columnsCourses: MutableMap<String, TableInfo.Column> = mutableMapOf()
        _columnsCourses.put("id", TableInfo.Column("id", "INTEGER", true, 1, null, TableInfo.CREATED_FROM_ENTITY))
        _columnsCourses.put("name", TableInfo.Column("name", "TEXT", true, 0, null, TableInfo.CREATED_FROM_ENTITY))
        _columnsCourses.put("area", TableInfo.Column("area", "TEXT", true, 0, null, TableInfo.CREATED_FROM_ENTITY))
        _columnsCourses.put("distanceKm", TableInfo.Column("distanceKm", "REAL", true, 0, null, TableInfo.CREATED_FROM_ENTITY))
        _columnsCourses.put("elevationM", TableInfo.Column("elevationM", "INTEGER", true, 0, null, TableInfo.CREATED_FROM_ENTITY))
        _columnsCourses.put("track", TableInfo.Column("track", "TEXT", true, 0, null, TableInfo.CREATED_FROM_ENTITY))
        _columnsCourses.put("author", TableInfo.Column("author", "TEXT", true, 0, null, TableInfo.CREATED_FROM_ENTITY))
        _columnsCourses.put("mine", TableInfo.Column("mine", "INTEGER", true, 0, null, TableInfo.CREATED_FROM_ENTITY))
        _columnsCourses.put("shared", TableInfo.Column("shared", "INTEGER", true, 0, null, TableInfo.CREATED_FROM_ENTITY))
        _columnsCourses.put("likes", TableInfo.Column("likes", "INTEGER", true, 0, null, TableInfo.CREATED_FROM_ENTITY))
        _columnsCourses.put("liked", TableInfo.Column("liked", "INTEGER", true, 0, null, TableInfo.CREATED_FROM_ENTITY))
        _columnsCourses.put("runCount", TableInfo.Column("runCount", "INTEGER", true, 0, null, TableInfo.CREATED_FROM_ENTITY))
        _columnsCourses.put("createdAt", TableInfo.Column("createdAt", "INTEGER", true, 0, null, TableInfo.CREATED_FROM_ENTITY))
        val _foreignKeysCourses: MutableSet<TableInfo.ForeignKey> = mutableSetOf()
        val _indicesCourses: MutableSet<TableInfo.Index> = mutableSetOf()
        val _infoCourses: TableInfo = TableInfo("courses", _columnsCourses, _foreignKeysCourses, _indicesCourses)
        val _existingCourses: TableInfo = read(connection, "courses")
        if (!_infoCourses.equals(_existingCourses)) {
          return RoomOpenDelegate.ValidationResult(false, """
              |courses(com.stepup.android.data.local.CourseEntity).
              | Expected:
              |""".trimMargin() + _infoCourses + """
              |
              | Found:
              |""".trimMargin() + _existingCourses)
        }
        val _columnsNotifications: MutableMap<String, TableInfo.Column> = mutableMapOf()
        _columnsNotifications.put("id", TableInfo.Column("id", "INTEGER", true, 1, null, TableInfo.CREATED_FROM_ENTITY))
        _columnsNotifications.put("timestamp", TableInfo.Column("timestamp", "INTEGER", true, 0, null, TableInfo.CREATED_FROM_ENTITY))
        _columnsNotifications.put("type", TableInfo.Column("type", "TEXT", true, 0, null, TableInfo.CREATED_FROM_ENTITY))
        _columnsNotifications.put("argText", TableInfo.Column("argText", "TEXT", true, 0, null, TableInfo.CREATED_FROM_ENTITY))
        _columnsNotifications.put("argAmount", TableInfo.Column("argAmount", "REAL", true, 0, null, TableInfo.CREATED_FROM_ENTITY))
        _columnsNotifications.put("argExtra", TableInfo.Column("argExtra", "TEXT", true, 0, null, TableInfo.CREATED_FROM_ENTITY))
        _columnsNotifications.put("read", TableInfo.Column("read", "INTEGER", true, 0, null, TableInfo.CREATED_FROM_ENTITY))
        _columnsNotifications.put("actioned", TableInfo.Column("actioned", "INTEGER", true, 0, null, TableInfo.CREATED_FROM_ENTITY))
        val _foreignKeysNotifications: MutableSet<TableInfo.ForeignKey> = mutableSetOf()
        val _indicesNotifications: MutableSet<TableInfo.Index> = mutableSetOf()
        val _infoNotifications: TableInfo = TableInfo("notifications", _columnsNotifications, _foreignKeysNotifications, _indicesNotifications)
        val _existingNotifications: TableInfo = read(connection, "notifications")
        if (!_infoNotifications.equals(_existingNotifications)) {
          return RoomOpenDelegate.ValidationResult(false, """
              |notifications(com.stepup.android.data.local.NotificationEntity).
              | Expected:
              |""".trimMargin() + _infoNotifications + """
              |
              | Found:
              |""".trimMargin() + _existingNotifications)
        }
        val _columnsNewsItems: MutableMap<String, TableInfo.Column> = mutableMapOf()
        _columnsNewsItems.put("url", TableInfo.Column("url", "TEXT", true, 1, null, TableInfo.CREATED_FROM_ENTITY))
        _columnsNewsItems.put("title", TableInfo.Column("title", "TEXT", true, 0, null, TableInfo.CREATED_FROM_ENTITY))
        _columnsNewsItems.put("source", TableInfo.Column("source", "TEXT", true, 0, null, TableInfo.CREATED_FROM_ENTITY))
        _columnsNewsItems.put("summary", TableInfo.Column("summary", "TEXT", true, 0, null, TableInfo.CREATED_FROM_ENTITY))
        _columnsNewsItems.put("kind", TableInfo.Column("kind", "TEXT", true, 0, null, TableInfo.CREATED_FROM_ENTITY))
        _columnsNewsItems.put("publishedAt", TableInfo.Column("publishedAt", "INTEGER", true, 0, null, TableInfo.CREATED_FROM_ENTITY))
        val _foreignKeysNewsItems: MutableSet<TableInfo.ForeignKey> = mutableSetOf()
        val _indicesNewsItems: MutableSet<TableInfo.Index> = mutableSetOf()
        val _infoNewsItems: TableInfo = TableInfo("news_items", _columnsNewsItems, _foreignKeysNewsItems, _indicesNewsItems)
        val _existingNewsItems: TableInfo = read(connection, "news_items")
        if (!_infoNewsItems.equals(_existingNewsItems)) {
          return RoomOpenDelegate.ValidationResult(false, """
              |news_items(com.stepup.android.data.local.NewsItemEntity).
              | Expected:
              |""".trimMargin() + _infoNewsItems + """
              |
              | Found:
              |""".trimMargin() + _existingNewsItems)
        }
        return RoomOpenDelegate.ValidationResult(true, null)
      }
    }
    return _openDelegate
  }

  protected override fun createInvalidationTracker(): InvalidationTracker {
    val _shadowTablesMap: MutableMap<String, String> = mutableMapOf()
    val _viewTables: MutableMap<String, Set<String>> = mutableMapOf()
    return InvalidationTracker(this, _shadowTablesMap, _viewTables, "daily_steps", "walk_sessions", "rewards", "sneakers", "boosts", "energy_purchases", "run_settlements", "claimed_events", "crew_memberships", "crews", "posts", "comments", "courses", "notifications", "news_items")
  }

  public override fun clearAllTables() {
    super.performClear(false, "daily_steps", "walk_sessions", "rewards", "sneakers", "boosts", "energy_purchases", "run_settlements", "claimed_events", "crew_memberships", "crews", "posts", "comments", "courses", "notifications", "news_items")
  }

  protected override fun getRequiredTypeConverterClasses(): Map<KClass<*>, List<KClass<*>>> {
    val _typeConvertersMap: MutableMap<KClass<*>, List<KClass<*>>> = mutableMapOf()
    _typeConvertersMap.put(StepDao::class, StepDao_Impl.getRequiredConverters())
    _typeConvertersMap.put(WalkSessionDao::class, WalkSessionDao_Impl.getRequiredConverters())
    _typeConvertersMap.put(RewardDao::class, RewardDao_Impl.getRequiredConverters())
    _typeConvertersMap.put(SneakerDao::class, SneakerDao_Impl.getRequiredConverters())
    _typeConvertersMap.put(BoostDao::class, BoostDao_Impl.getRequiredConverters())
    _typeConvertersMap.put(EnergyPurchaseDao::class, EnergyPurchaseDao_Impl.getRequiredConverters())
    _typeConvertersMap.put(RunSettlementDao::class, RunSettlementDao_Impl.getRequiredConverters())
    _typeConvertersMap.put(ClaimedEventDao::class, ClaimedEventDao_Impl.getRequiredConverters())
    _typeConvertersMap.put(CrewDao::class, CrewDao_Impl.getRequiredConverters())
    _typeConvertersMap.put(CrewInfoDao::class, CrewInfoDao_Impl.getRequiredConverters())
    _typeConvertersMap.put(PostDao::class, PostDao_Impl.getRequiredConverters())
    _typeConvertersMap.put(CommentDao::class, CommentDao_Impl.getRequiredConverters())
    _typeConvertersMap.put(CourseDao::class, CourseDao_Impl.getRequiredConverters())
    _typeConvertersMap.put(NotificationDao::class, NotificationDao_Impl.getRequiredConverters())
    _typeConvertersMap.put(NewsDao::class, NewsDao_Impl.getRequiredConverters())
    return _typeConvertersMap
  }

  public override fun getRequiredAutoMigrationSpecClasses(): Set<KClass<out AutoMigrationSpec>> {
    val _autoMigrationSpecsSet: MutableSet<KClass<out AutoMigrationSpec>> = mutableSetOf()
    return _autoMigrationSpecsSet
  }

  public override fun createAutoMigrations(autoMigrationSpecs: Map<KClass<out AutoMigrationSpec>, AutoMigrationSpec>): List<Migration> {
    val _autoMigrations: MutableList<Migration> = mutableListOf()
    return _autoMigrations
  }

  public override fun stepDao(): StepDao = _stepDao.value

  public override fun walkSessionDao(): WalkSessionDao = _walkSessionDao.value

  public override fun rewardDao(): RewardDao = _rewardDao.value

  public override fun sneakerDao(): SneakerDao = _sneakerDao.value

  public override fun boostDao(): BoostDao = _boostDao.value

  public override fun energyPurchaseDao(): EnergyPurchaseDao = _energyPurchaseDao.value

  public override fun runSettlementDao(): RunSettlementDao = _runSettlementDao.value

  public override fun claimedEventDao(): ClaimedEventDao = _claimedEventDao.value

  public override fun crewDao(): CrewDao = _crewDao.value

  public override fun crewInfoDao(): CrewInfoDao = _crewInfoDao.value

  public override fun postDao(): PostDao = _postDao.value

  public override fun commentDao(): CommentDao = _commentDao.value

  public override fun courseDao(): CourseDao = _courseDao.value

  public override fun notificationDao(): NotificationDao = _notificationDao.value

  public override fun newsDao(): NewsDao = _newsDao.value
}
