package com.stepup.android.`data`.local

import androidx.room.EntityDeleteOrUpdateAdapter
import androidx.room.EntityInsertAdapter
import androidx.room.RoomDatabase
import androidx.room.coroutines.createFlow
import androidx.room.util.getColumnIndexOrThrow
import androidx.room.util.getTotalChangedRows
import androidx.room.util.performSuspending
import androidx.sqlite.SQLiteStatement
import com.stepup.android.domain.RunMark
import com.stepup.android.domain.coaching.CoachRun
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
public class WalkSessionDao_Impl(
  __db: RoomDatabase,
) : WalkSessionDao {
  private val __db: RoomDatabase

  private val __insertAdapterOfWalkSessionEntity: EntityInsertAdapter<WalkSessionEntity>

  private val __updateAdapterOfWalkSessionEntity: EntityDeleteOrUpdateAdapter<WalkSessionEntity>
  init {
    this.__db = __db
    this.__insertAdapterOfWalkSessionEntity = object : EntityInsertAdapter<WalkSessionEntity>() {
      protected override fun createQuery(): String = "INSERT OR ABORT INTO `walk_sessions` (`id`,`startedAt`,`endedAt`,`steps`,`durationSec`,`distanceMeters`,`calories`,`pointsEarned`,`recordingOwner`,`track`,`boostBps`,`partySize`,`faction`,`crewId`,`uploadState`,`uploadAttemptedAt`,`uploadAttempts`,`uploadError`,`verdict`,`claimSignature`,`claimSessionHash`,`claimAmount`,`claimDay`,`claimDeadline`,`mockLocation`,`laps`) VALUES (nullif(?, 0),?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?)"

      protected override fun bind(statement: SQLiteStatement, entity: WalkSessionEntity) {
        statement.bindLong(1, entity.id)
        statement.bindLong(2, entity.startedAt)
        statement.bindLong(3, entity.endedAt)
        statement.bindLong(4, entity.steps.toLong())
        statement.bindLong(5, entity.durationSec)
        statement.bindDouble(6, entity.distanceMeters)
        statement.bindDouble(7, entity.calories)
        statement.bindDouble(8, entity.pointsEarned)
        statement.bindText(9, entity.recordingOwner)
        statement.bindText(10, entity.track)
        statement.bindLong(11, entity.boostBps.toLong())
        statement.bindLong(12, entity.partySize.toLong())
        statement.bindText(13, entity.faction)
        statement.bindText(14, entity.crewId)
        statement.bindText(15, entity.uploadState)
        statement.bindLong(16, entity.uploadAttemptedAt)
        statement.bindLong(17, entity.uploadAttempts.toLong())
        statement.bindText(18, entity.uploadError)
        statement.bindText(19, entity.verdict)
        statement.bindText(20, entity.claimSignature)
        statement.bindText(21, entity.claimSessionHash)
        statement.bindText(22, entity.claimAmount)
        statement.bindLong(23, entity.claimDay)
        statement.bindLong(24, entity.claimDeadline)
        val _tmp: Int = if (entity.mockLocation) 1 else 0
        statement.bindLong(25, _tmp.toLong())
        statement.bindText(26, entity.laps)
      }
    }
    this.__updateAdapterOfWalkSessionEntity = object : EntityDeleteOrUpdateAdapter<WalkSessionEntity>() {
      protected override fun createQuery(): String = "UPDATE OR ABORT `walk_sessions` SET `id` = ?,`startedAt` = ?,`endedAt` = ?,`steps` = ?,`durationSec` = ?,`distanceMeters` = ?,`calories` = ?,`pointsEarned` = ?,`recordingOwner` = ?,`track` = ?,`boostBps` = ?,`partySize` = ?,`faction` = ?,`crewId` = ?,`uploadState` = ?,`uploadAttemptedAt` = ?,`uploadAttempts` = ?,`uploadError` = ?,`verdict` = ?,`claimSignature` = ?,`claimSessionHash` = ?,`claimAmount` = ?,`claimDay` = ?,`claimDeadline` = ?,`mockLocation` = ?,`laps` = ? WHERE `id` = ?"

      protected override fun bind(statement: SQLiteStatement, entity: WalkSessionEntity) {
        statement.bindLong(1, entity.id)
        statement.bindLong(2, entity.startedAt)
        statement.bindLong(3, entity.endedAt)
        statement.bindLong(4, entity.steps.toLong())
        statement.bindLong(5, entity.durationSec)
        statement.bindDouble(6, entity.distanceMeters)
        statement.bindDouble(7, entity.calories)
        statement.bindDouble(8, entity.pointsEarned)
        statement.bindText(9, entity.recordingOwner)
        statement.bindText(10, entity.track)
        statement.bindLong(11, entity.boostBps.toLong())
        statement.bindLong(12, entity.partySize.toLong())
        statement.bindText(13, entity.faction)
        statement.bindText(14, entity.crewId)
        statement.bindText(15, entity.uploadState)
        statement.bindLong(16, entity.uploadAttemptedAt)
        statement.bindLong(17, entity.uploadAttempts.toLong())
        statement.bindText(18, entity.uploadError)
        statement.bindText(19, entity.verdict)
        statement.bindText(20, entity.claimSignature)
        statement.bindText(21, entity.claimSessionHash)
        statement.bindText(22, entity.claimAmount)
        statement.bindLong(23, entity.claimDay)
        statement.bindLong(24, entity.claimDeadline)
        val _tmp: Int = if (entity.mockLocation) 1 else 0
        statement.bindLong(25, _tmp.toLong())
        statement.bindText(26, entity.laps)
        statement.bindLong(27, entity.id)
      }
    }
  }

  public override suspend fun insert(session: WalkSessionEntity): Unit = performSuspending(__db, false, true) { _connection ->
    __insertAdapterOfWalkSessionEntity.insert(_connection, session)
  }

  public override suspend fun update(session: WalkSessionEntity): Unit = performSuspending(__db, false, true) { _connection ->
    __updateAdapterOfWalkSessionEntity.handle(_connection, session)
  }

  public override fun observeRecent(limit: Int): Flow<List<WalkSessionEntity>> {
    val _sql: String = "SELECT * FROM walk_sessions ORDER BY startedAt DESC LIMIT ?"
    return createFlow(__db, false, arrayOf("walk_sessions")) { _connection ->
      val _stmt: SQLiteStatement = _connection.prepare(_sql)
      try {
        var _argIndex: Int = 1
        _stmt.bindLong(_argIndex, limit.toLong())
        val _columnIndexOfId: Int = getColumnIndexOrThrow(_stmt, "id")
        val _columnIndexOfStartedAt: Int = getColumnIndexOrThrow(_stmt, "startedAt")
        val _columnIndexOfEndedAt: Int = getColumnIndexOrThrow(_stmt, "endedAt")
        val _columnIndexOfSteps: Int = getColumnIndexOrThrow(_stmt, "steps")
        val _columnIndexOfDurationSec: Int = getColumnIndexOrThrow(_stmt, "durationSec")
        val _columnIndexOfDistanceMeters: Int = getColumnIndexOrThrow(_stmt, "distanceMeters")
        val _columnIndexOfCalories: Int = getColumnIndexOrThrow(_stmt, "calories")
        val _columnIndexOfPointsEarned: Int = getColumnIndexOrThrow(_stmt, "pointsEarned")
        val _columnIndexOfRecordingOwner: Int = getColumnIndexOrThrow(_stmt, "recordingOwner")
        val _columnIndexOfTrack: Int = getColumnIndexOrThrow(_stmt, "track")
        val _columnIndexOfBoostBps: Int = getColumnIndexOrThrow(_stmt, "boostBps")
        val _columnIndexOfPartySize: Int = getColumnIndexOrThrow(_stmt, "partySize")
        val _columnIndexOfFaction: Int = getColumnIndexOrThrow(_stmt, "faction")
        val _columnIndexOfCrewId: Int = getColumnIndexOrThrow(_stmt, "crewId")
        val _columnIndexOfUploadState: Int = getColumnIndexOrThrow(_stmt, "uploadState")
        val _columnIndexOfUploadAttemptedAt: Int = getColumnIndexOrThrow(_stmt, "uploadAttemptedAt")
        val _columnIndexOfUploadAttempts: Int = getColumnIndexOrThrow(_stmt, "uploadAttempts")
        val _columnIndexOfUploadError: Int = getColumnIndexOrThrow(_stmt, "uploadError")
        val _columnIndexOfVerdict: Int = getColumnIndexOrThrow(_stmt, "verdict")
        val _columnIndexOfClaimSignature: Int = getColumnIndexOrThrow(_stmt, "claimSignature")
        val _columnIndexOfClaimSessionHash: Int = getColumnIndexOrThrow(_stmt, "claimSessionHash")
        val _columnIndexOfClaimAmount: Int = getColumnIndexOrThrow(_stmt, "claimAmount")
        val _columnIndexOfClaimDay: Int = getColumnIndexOrThrow(_stmt, "claimDay")
        val _columnIndexOfClaimDeadline: Int = getColumnIndexOrThrow(_stmt, "claimDeadline")
        val _columnIndexOfMockLocation: Int = getColumnIndexOrThrow(_stmt, "mockLocation")
        val _columnIndexOfLaps: Int = getColumnIndexOrThrow(_stmt, "laps")
        val _result: MutableList<WalkSessionEntity> = mutableListOf()
        while (_stmt.step()) {
          val _item: WalkSessionEntity
          val _tmpId: Long
          _tmpId = _stmt.getLong(_columnIndexOfId)
          val _tmpStartedAt: Long
          _tmpStartedAt = _stmt.getLong(_columnIndexOfStartedAt)
          val _tmpEndedAt: Long
          _tmpEndedAt = _stmt.getLong(_columnIndexOfEndedAt)
          val _tmpSteps: Int
          _tmpSteps = _stmt.getLong(_columnIndexOfSteps).toInt()
          val _tmpDurationSec: Long
          _tmpDurationSec = _stmt.getLong(_columnIndexOfDurationSec)
          val _tmpDistanceMeters: Double
          _tmpDistanceMeters = _stmt.getDouble(_columnIndexOfDistanceMeters)
          val _tmpCalories: Double
          _tmpCalories = _stmt.getDouble(_columnIndexOfCalories)
          val _tmpPointsEarned: Double
          _tmpPointsEarned = _stmt.getDouble(_columnIndexOfPointsEarned)
          val _tmpRecordingOwner: String
          _tmpRecordingOwner = _stmt.getText(_columnIndexOfRecordingOwner)
          val _tmpTrack: String
          _tmpTrack = _stmt.getText(_columnIndexOfTrack)
          val _tmpBoostBps: Int
          _tmpBoostBps = _stmt.getLong(_columnIndexOfBoostBps).toInt()
          val _tmpPartySize: Int
          _tmpPartySize = _stmt.getLong(_columnIndexOfPartySize).toInt()
          val _tmpFaction: String
          _tmpFaction = _stmt.getText(_columnIndexOfFaction)
          val _tmpCrewId: String
          _tmpCrewId = _stmt.getText(_columnIndexOfCrewId)
          val _tmpUploadState: String
          _tmpUploadState = _stmt.getText(_columnIndexOfUploadState)
          val _tmpUploadAttemptedAt: Long
          _tmpUploadAttemptedAt = _stmt.getLong(_columnIndexOfUploadAttemptedAt)
          val _tmpUploadAttempts: Int
          _tmpUploadAttempts = _stmt.getLong(_columnIndexOfUploadAttempts).toInt()
          val _tmpUploadError: String
          _tmpUploadError = _stmt.getText(_columnIndexOfUploadError)
          val _tmpVerdict: String
          _tmpVerdict = _stmt.getText(_columnIndexOfVerdict)
          val _tmpClaimSignature: String
          _tmpClaimSignature = _stmt.getText(_columnIndexOfClaimSignature)
          val _tmpClaimSessionHash: String
          _tmpClaimSessionHash = _stmt.getText(_columnIndexOfClaimSessionHash)
          val _tmpClaimAmount: String
          _tmpClaimAmount = _stmt.getText(_columnIndexOfClaimAmount)
          val _tmpClaimDay: Long
          _tmpClaimDay = _stmt.getLong(_columnIndexOfClaimDay)
          val _tmpClaimDeadline: Long
          _tmpClaimDeadline = _stmt.getLong(_columnIndexOfClaimDeadline)
          val _tmpMockLocation: Boolean
          val _tmp: Int
          _tmp = _stmt.getLong(_columnIndexOfMockLocation).toInt()
          _tmpMockLocation = _tmp != 0
          val _tmpLaps: String
          _tmpLaps = _stmt.getText(_columnIndexOfLaps)
          _item = WalkSessionEntity(_tmpId,_tmpStartedAt,_tmpEndedAt,_tmpSteps,_tmpDurationSec,_tmpDistanceMeters,_tmpCalories,_tmpPointsEarned,_tmpRecordingOwner,_tmpTrack,_tmpBoostBps,_tmpPartySize,_tmpFaction,_tmpCrewId,_tmpUploadState,_tmpUploadAttemptedAt,_tmpUploadAttempts,_tmpUploadError,_tmpVerdict,_tmpClaimSignature,_tmpClaimSessionHash,_tmpClaimAmount,_tmpClaimDay,_tmpClaimDeadline,_tmpMockLocation,_tmpLaps)
          _result.add(_item)
        }
        _result
      } finally {
        _stmt.close()
      }
    }
  }

  public override fun observeSessionCount(): Flow<Int> {
    val _sql: String = "SELECT COUNT(*) FROM walk_sessions"
    return createFlow(__db, false, arrayOf("walk_sessions")) { _connection ->
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

  public override fun observeRunTotals(): Flow<RunTotals> {
    val _sql: String = "SELECT COUNT(*) AS runs, COALESCE(SUM(distanceMeters), 0) AS meters FROM walk_sessions"
    return createFlow(__db, false, arrayOf("walk_sessions")) { _connection ->
      val _stmt: SQLiteStatement = _connection.prepare(_sql)
      try {
        val _columnIndexOfRuns: Int = 0
        val _columnIndexOfMeters: Int = 1
        val _result: RunTotals
        if (_stmt.step()) {
          val _tmpRuns: Int
          _tmpRuns = _stmt.getLong(_columnIndexOfRuns).toInt()
          val _tmpMeters: Double
          _tmpMeters = _stmt.getDouble(_columnIndexOfMeters)
          _result = RunTotals(_tmpRuns,_tmpMeters)
        } else {
          error("The query result was empty, but expected a single row to return a NON-NULL object of type 'com.stepup.android.`data`.local.RunTotals'.")
        }
        _result
      } finally {
        _stmt.close()
      }
    }
  }

  public override fun observeVerifiedSessions(): Flow<List<WalkSessionEntity>> {
    val _sql: String = "SELECT * FROM walk_sessions WHERE uploadState = 'SIGNED' AND verdict NOT IN ('FLAGGED', 'VOID')"
    return createFlow(__db, false, arrayOf("walk_sessions")) { _connection ->
      val _stmt: SQLiteStatement = _connection.prepare(_sql)
      try {
        val _columnIndexOfId: Int = getColumnIndexOrThrow(_stmt, "id")
        val _columnIndexOfStartedAt: Int = getColumnIndexOrThrow(_stmt, "startedAt")
        val _columnIndexOfEndedAt: Int = getColumnIndexOrThrow(_stmt, "endedAt")
        val _columnIndexOfSteps: Int = getColumnIndexOrThrow(_stmt, "steps")
        val _columnIndexOfDurationSec: Int = getColumnIndexOrThrow(_stmt, "durationSec")
        val _columnIndexOfDistanceMeters: Int = getColumnIndexOrThrow(_stmt, "distanceMeters")
        val _columnIndexOfCalories: Int = getColumnIndexOrThrow(_stmt, "calories")
        val _columnIndexOfPointsEarned: Int = getColumnIndexOrThrow(_stmt, "pointsEarned")
        val _columnIndexOfRecordingOwner: Int = getColumnIndexOrThrow(_stmt, "recordingOwner")
        val _columnIndexOfTrack: Int = getColumnIndexOrThrow(_stmt, "track")
        val _columnIndexOfBoostBps: Int = getColumnIndexOrThrow(_stmt, "boostBps")
        val _columnIndexOfPartySize: Int = getColumnIndexOrThrow(_stmt, "partySize")
        val _columnIndexOfFaction: Int = getColumnIndexOrThrow(_stmt, "faction")
        val _columnIndexOfCrewId: Int = getColumnIndexOrThrow(_stmt, "crewId")
        val _columnIndexOfUploadState: Int = getColumnIndexOrThrow(_stmt, "uploadState")
        val _columnIndexOfUploadAttemptedAt: Int = getColumnIndexOrThrow(_stmt, "uploadAttemptedAt")
        val _columnIndexOfUploadAttempts: Int = getColumnIndexOrThrow(_stmt, "uploadAttempts")
        val _columnIndexOfUploadError: Int = getColumnIndexOrThrow(_stmt, "uploadError")
        val _columnIndexOfVerdict: Int = getColumnIndexOrThrow(_stmt, "verdict")
        val _columnIndexOfClaimSignature: Int = getColumnIndexOrThrow(_stmt, "claimSignature")
        val _columnIndexOfClaimSessionHash: Int = getColumnIndexOrThrow(_stmt, "claimSessionHash")
        val _columnIndexOfClaimAmount: Int = getColumnIndexOrThrow(_stmt, "claimAmount")
        val _columnIndexOfClaimDay: Int = getColumnIndexOrThrow(_stmt, "claimDay")
        val _columnIndexOfClaimDeadline: Int = getColumnIndexOrThrow(_stmt, "claimDeadline")
        val _columnIndexOfMockLocation: Int = getColumnIndexOrThrow(_stmt, "mockLocation")
        val _columnIndexOfLaps: Int = getColumnIndexOrThrow(_stmt, "laps")
        val _result: MutableList<WalkSessionEntity> = mutableListOf()
        while (_stmt.step()) {
          val _item: WalkSessionEntity
          val _tmpId: Long
          _tmpId = _stmt.getLong(_columnIndexOfId)
          val _tmpStartedAt: Long
          _tmpStartedAt = _stmt.getLong(_columnIndexOfStartedAt)
          val _tmpEndedAt: Long
          _tmpEndedAt = _stmt.getLong(_columnIndexOfEndedAt)
          val _tmpSteps: Int
          _tmpSteps = _stmt.getLong(_columnIndexOfSteps).toInt()
          val _tmpDurationSec: Long
          _tmpDurationSec = _stmt.getLong(_columnIndexOfDurationSec)
          val _tmpDistanceMeters: Double
          _tmpDistanceMeters = _stmt.getDouble(_columnIndexOfDistanceMeters)
          val _tmpCalories: Double
          _tmpCalories = _stmt.getDouble(_columnIndexOfCalories)
          val _tmpPointsEarned: Double
          _tmpPointsEarned = _stmt.getDouble(_columnIndexOfPointsEarned)
          val _tmpRecordingOwner: String
          _tmpRecordingOwner = _stmt.getText(_columnIndexOfRecordingOwner)
          val _tmpTrack: String
          _tmpTrack = _stmt.getText(_columnIndexOfTrack)
          val _tmpBoostBps: Int
          _tmpBoostBps = _stmt.getLong(_columnIndexOfBoostBps).toInt()
          val _tmpPartySize: Int
          _tmpPartySize = _stmt.getLong(_columnIndexOfPartySize).toInt()
          val _tmpFaction: String
          _tmpFaction = _stmt.getText(_columnIndexOfFaction)
          val _tmpCrewId: String
          _tmpCrewId = _stmt.getText(_columnIndexOfCrewId)
          val _tmpUploadState: String
          _tmpUploadState = _stmt.getText(_columnIndexOfUploadState)
          val _tmpUploadAttemptedAt: Long
          _tmpUploadAttemptedAt = _stmt.getLong(_columnIndexOfUploadAttemptedAt)
          val _tmpUploadAttempts: Int
          _tmpUploadAttempts = _stmt.getLong(_columnIndexOfUploadAttempts).toInt()
          val _tmpUploadError: String
          _tmpUploadError = _stmt.getText(_columnIndexOfUploadError)
          val _tmpVerdict: String
          _tmpVerdict = _stmt.getText(_columnIndexOfVerdict)
          val _tmpClaimSignature: String
          _tmpClaimSignature = _stmt.getText(_columnIndexOfClaimSignature)
          val _tmpClaimSessionHash: String
          _tmpClaimSessionHash = _stmt.getText(_columnIndexOfClaimSessionHash)
          val _tmpClaimAmount: String
          _tmpClaimAmount = _stmt.getText(_columnIndexOfClaimAmount)
          val _tmpClaimDay: Long
          _tmpClaimDay = _stmt.getLong(_columnIndexOfClaimDay)
          val _tmpClaimDeadline: Long
          _tmpClaimDeadline = _stmt.getLong(_columnIndexOfClaimDeadline)
          val _tmpMockLocation: Boolean
          val _tmp: Int
          _tmp = _stmt.getLong(_columnIndexOfMockLocation).toInt()
          _tmpMockLocation = _tmp != 0
          val _tmpLaps: String
          _tmpLaps = _stmt.getText(_columnIndexOfLaps)
          _item = WalkSessionEntity(_tmpId,_tmpStartedAt,_tmpEndedAt,_tmpSteps,_tmpDurationSec,_tmpDistanceMeters,_tmpCalories,_tmpPointsEarned,_tmpRecordingOwner,_tmpTrack,_tmpBoostBps,_tmpPartySize,_tmpFaction,_tmpCrewId,_tmpUploadState,_tmpUploadAttemptedAt,_tmpUploadAttempts,_tmpUploadError,_tmpVerdict,_tmpClaimSignature,_tmpClaimSessionHash,_tmpClaimAmount,_tmpClaimDay,_tmpClaimDeadline,_tmpMockLocation,_tmpLaps)
          _result.add(_item)
        }
        _result
      } finally {
        _stmt.close()
      }
    }
  }

  public override fun observeDurationSince(fromMillis: Long): Flow<Long> {
    val _sql: String = "SELECT COALESCE(SUM(durationSec), 0) FROM walk_sessions WHERE startedAt >= ?"
    return createFlow(__db, false, arrayOf("walk_sessions")) { _connection ->
      val _stmt: SQLiteStatement = _connection.prepare(_sql)
      try {
        var _argIndex: Int = 1
        _stmt.bindLong(_argIndex, fromMillis)
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

  public override fun observeRecentFor(owner: String, limit: Int): Flow<List<WalkSessionEntity>> {
    val _sql: String = "SELECT * FROM walk_sessions WHERE recordingOwner IN (?, 'legacy') ORDER BY startedAt DESC LIMIT ?"
    return createFlow(__db, false, arrayOf("walk_sessions")) { _connection ->
      val _stmt: SQLiteStatement = _connection.prepare(_sql)
      try {
        var _argIndex: Int = 1
        _stmt.bindText(_argIndex, owner)
        _argIndex = 2
        _stmt.bindLong(_argIndex, limit.toLong())
        val _columnIndexOfId: Int = getColumnIndexOrThrow(_stmt, "id")
        val _columnIndexOfStartedAt: Int = getColumnIndexOrThrow(_stmt, "startedAt")
        val _columnIndexOfEndedAt: Int = getColumnIndexOrThrow(_stmt, "endedAt")
        val _columnIndexOfSteps: Int = getColumnIndexOrThrow(_stmt, "steps")
        val _columnIndexOfDurationSec: Int = getColumnIndexOrThrow(_stmt, "durationSec")
        val _columnIndexOfDistanceMeters: Int = getColumnIndexOrThrow(_stmt, "distanceMeters")
        val _columnIndexOfCalories: Int = getColumnIndexOrThrow(_stmt, "calories")
        val _columnIndexOfPointsEarned: Int = getColumnIndexOrThrow(_stmt, "pointsEarned")
        val _columnIndexOfRecordingOwner: Int = getColumnIndexOrThrow(_stmt, "recordingOwner")
        val _columnIndexOfTrack: Int = getColumnIndexOrThrow(_stmt, "track")
        val _columnIndexOfBoostBps: Int = getColumnIndexOrThrow(_stmt, "boostBps")
        val _columnIndexOfPartySize: Int = getColumnIndexOrThrow(_stmt, "partySize")
        val _columnIndexOfFaction: Int = getColumnIndexOrThrow(_stmt, "faction")
        val _columnIndexOfCrewId: Int = getColumnIndexOrThrow(_stmt, "crewId")
        val _columnIndexOfUploadState: Int = getColumnIndexOrThrow(_stmt, "uploadState")
        val _columnIndexOfUploadAttemptedAt: Int = getColumnIndexOrThrow(_stmt, "uploadAttemptedAt")
        val _columnIndexOfUploadAttempts: Int = getColumnIndexOrThrow(_stmt, "uploadAttempts")
        val _columnIndexOfUploadError: Int = getColumnIndexOrThrow(_stmt, "uploadError")
        val _columnIndexOfVerdict: Int = getColumnIndexOrThrow(_stmt, "verdict")
        val _columnIndexOfClaimSignature: Int = getColumnIndexOrThrow(_stmt, "claimSignature")
        val _columnIndexOfClaimSessionHash: Int = getColumnIndexOrThrow(_stmt, "claimSessionHash")
        val _columnIndexOfClaimAmount: Int = getColumnIndexOrThrow(_stmt, "claimAmount")
        val _columnIndexOfClaimDay: Int = getColumnIndexOrThrow(_stmt, "claimDay")
        val _columnIndexOfClaimDeadline: Int = getColumnIndexOrThrow(_stmt, "claimDeadline")
        val _columnIndexOfMockLocation: Int = getColumnIndexOrThrow(_stmt, "mockLocation")
        val _columnIndexOfLaps: Int = getColumnIndexOrThrow(_stmt, "laps")
        val _result: MutableList<WalkSessionEntity> = mutableListOf()
        while (_stmt.step()) {
          val _item: WalkSessionEntity
          val _tmpId: Long
          _tmpId = _stmt.getLong(_columnIndexOfId)
          val _tmpStartedAt: Long
          _tmpStartedAt = _stmt.getLong(_columnIndexOfStartedAt)
          val _tmpEndedAt: Long
          _tmpEndedAt = _stmt.getLong(_columnIndexOfEndedAt)
          val _tmpSteps: Int
          _tmpSteps = _stmt.getLong(_columnIndexOfSteps).toInt()
          val _tmpDurationSec: Long
          _tmpDurationSec = _stmt.getLong(_columnIndexOfDurationSec)
          val _tmpDistanceMeters: Double
          _tmpDistanceMeters = _stmt.getDouble(_columnIndexOfDistanceMeters)
          val _tmpCalories: Double
          _tmpCalories = _stmt.getDouble(_columnIndexOfCalories)
          val _tmpPointsEarned: Double
          _tmpPointsEarned = _stmt.getDouble(_columnIndexOfPointsEarned)
          val _tmpRecordingOwner: String
          _tmpRecordingOwner = _stmt.getText(_columnIndexOfRecordingOwner)
          val _tmpTrack: String
          _tmpTrack = _stmt.getText(_columnIndexOfTrack)
          val _tmpBoostBps: Int
          _tmpBoostBps = _stmt.getLong(_columnIndexOfBoostBps).toInt()
          val _tmpPartySize: Int
          _tmpPartySize = _stmt.getLong(_columnIndexOfPartySize).toInt()
          val _tmpFaction: String
          _tmpFaction = _stmt.getText(_columnIndexOfFaction)
          val _tmpCrewId: String
          _tmpCrewId = _stmt.getText(_columnIndexOfCrewId)
          val _tmpUploadState: String
          _tmpUploadState = _stmt.getText(_columnIndexOfUploadState)
          val _tmpUploadAttemptedAt: Long
          _tmpUploadAttemptedAt = _stmt.getLong(_columnIndexOfUploadAttemptedAt)
          val _tmpUploadAttempts: Int
          _tmpUploadAttempts = _stmt.getLong(_columnIndexOfUploadAttempts).toInt()
          val _tmpUploadError: String
          _tmpUploadError = _stmt.getText(_columnIndexOfUploadError)
          val _tmpVerdict: String
          _tmpVerdict = _stmt.getText(_columnIndexOfVerdict)
          val _tmpClaimSignature: String
          _tmpClaimSignature = _stmt.getText(_columnIndexOfClaimSignature)
          val _tmpClaimSessionHash: String
          _tmpClaimSessionHash = _stmt.getText(_columnIndexOfClaimSessionHash)
          val _tmpClaimAmount: String
          _tmpClaimAmount = _stmt.getText(_columnIndexOfClaimAmount)
          val _tmpClaimDay: Long
          _tmpClaimDay = _stmt.getLong(_columnIndexOfClaimDay)
          val _tmpClaimDeadline: Long
          _tmpClaimDeadline = _stmt.getLong(_columnIndexOfClaimDeadline)
          val _tmpMockLocation: Boolean
          val _tmp: Int
          _tmp = _stmt.getLong(_columnIndexOfMockLocation).toInt()
          _tmpMockLocation = _tmp != 0
          val _tmpLaps: String
          _tmpLaps = _stmt.getText(_columnIndexOfLaps)
          _item = WalkSessionEntity(_tmpId,_tmpStartedAt,_tmpEndedAt,_tmpSteps,_tmpDurationSec,_tmpDistanceMeters,_tmpCalories,_tmpPointsEarned,_tmpRecordingOwner,_tmpTrack,_tmpBoostBps,_tmpPartySize,_tmpFaction,_tmpCrewId,_tmpUploadState,_tmpUploadAttemptedAt,_tmpUploadAttempts,_tmpUploadError,_tmpVerdict,_tmpClaimSignature,_tmpClaimSessionHash,_tmpClaimAmount,_tmpClaimDay,_tmpClaimDeadline,_tmpMockLocation,_tmpLaps)
          _result.add(_item)
        }
        _result
      } finally {
        _stmt.close()
      }
    }
  }

  public override fun observeSessionCountFor(owner: String): Flow<Int> {
    val _sql: String = "SELECT COUNT(*) FROM walk_sessions WHERE recordingOwner IN (?, 'legacy')"
    return createFlow(__db, false, arrayOf("walk_sessions")) { _connection ->
      val _stmt: SQLiteStatement = _connection.prepare(_sql)
      try {
        var _argIndex: Int = 1
        _stmt.bindText(_argIndex, owner)
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

  public override fun observeRunTotalsFor(owner: String): Flow<RunTotals> {
    val _sql: String = "SELECT COUNT(*) AS runs, COALESCE(SUM(CASE WHEN verdict != 'VOID' THEN distanceMeters ELSE 0 END), 0) AS meters FROM walk_sessions WHERE recordingOwner IN (?, 'legacy')"
    return createFlow(__db, false, arrayOf("walk_sessions")) { _connection ->
      val _stmt: SQLiteStatement = _connection.prepare(_sql)
      try {
        var _argIndex: Int = 1
        _stmt.bindText(_argIndex, owner)
        val _columnIndexOfRuns: Int = 0
        val _columnIndexOfMeters: Int = 1
        val _result: RunTotals
        if (_stmt.step()) {
          val _tmpRuns: Int
          _tmpRuns = _stmt.getLong(_columnIndexOfRuns).toInt()
          val _tmpMeters: Double
          _tmpMeters = _stmt.getDouble(_columnIndexOfMeters)
          _result = RunTotals(_tmpRuns,_tmpMeters)
        } else {
          error("The query result was empty, but expected a single row to return a NON-NULL object of type 'com.stepup.android.`data`.local.RunTotals'.")
        }
        _result
      } finally {
        _stmt.close()
      }
    }
  }

  public override fun observeLongestMetersFor(owner: String): Flow<Double> {
    val _sql: String = "SELECT COALESCE(MAX(CASE WHEN verdict != 'VOID' THEN distanceMeters ELSE 0 END), 0) FROM walk_sessions WHERE recordingOwner IN (?, 'legacy')"
    return createFlow(__db, false, arrayOf("walk_sessions")) { _connection ->
      val _stmt: SQLiteStatement = _connection.prepare(_sql)
      try {
        var _argIndex: Int = 1
        _stmt.bindText(_argIndex, owner)
        val _result: Double
        if (_stmt.step()) {
          val _tmp: Double
          _tmp = _stmt.getDouble(0)
          _result = _tmp
        } else {
          _result = 0.0
        }
        _result
      } finally {
        _stmt.close()
      }
    }
  }

  public override fun observeLatestFor(owner: String): Flow<WalkSessionEntity?> {
    val _sql: String = "SELECT * FROM walk_sessions WHERE recordingOwner IN (?, 'legacy') ORDER BY startedAt DESC LIMIT 1"
    return createFlow(__db, false, arrayOf("walk_sessions")) { _connection ->
      val _stmt: SQLiteStatement = _connection.prepare(_sql)
      try {
        var _argIndex: Int = 1
        _stmt.bindText(_argIndex, owner)
        val _columnIndexOfId: Int = getColumnIndexOrThrow(_stmt, "id")
        val _columnIndexOfStartedAt: Int = getColumnIndexOrThrow(_stmt, "startedAt")
        val _columnIndexOfEndedAt: Int = getColumnIndexOrThrow(_stmt, "endedAt")
        val _columnIndexOfSteps: Int = getColumnIndexOrThrow(_stmt, "steps")
        val _columnIndexOfDurationSec: Int = getColumnIndexOrThrow(_stmt, "durationSec")
        val _columnIndexOfDistanceMeters: Int = getColumnIndexOrThrow(_stmt, "distanceMeters")
        val _columnIndexOfCalories: Int = getColumnIndexOrThrow(_stmt, "calories")
        val _columnIndexOfPointsEarned: Int = getColumnIndexOrThrow(_stmt, "pointsEarned")
        val _columnIndexOfRecordingOwner: Int = getColumnIndexOrThrow(_stmt, "recordingOwner")
        val _columnIndexOfTrack: Int = getColumnIndexOrThrow(_stmt, "track")
        val _columnIndexOfBoostBps: Int = getColumnIndexOrThrow(_stmt, "boostBps")
        val _columnIndexOfPartySize: Int = getColumnIndexOrThrow(_stmt, "partySize")
        val _columnIndexOfFaction: Int = getColumnIndexOrThrow(_stmt, "faction")
        val _columnIndexOfCrewId: Int = getColumnIndexOrThrow(_stmt, "crewId")
        val _columnIndexOfUploadState: Int = getColumnIndexOrThrow(_stmt, "uploadState")
        val _columnIndexOfUploadAttemptedAt: Int = getColumnIndexOrThrow(_stmt, "uploadAttemptedAt")
        val _columnIndexOfUploadAttempts: Int = getColumnIndexOrThrow(_stmt, "uploadAttempts")
        val _columnIndexOfUploadError: Int = getColumnIndexOrThrow(_stmt, "uploadError")
        val _columnIndexOfVerdict: Int = getColumnIndexOrThrow(_stmt, "verdict")
        val _columnIndexOfClaimSignature: Int = getColumnIndexOrThrow(_stmt, "claimSignature")
        val _columnIndexOfClaimSessionHash: Int = getColumnIndexOrThrow(_stmt, "claimSessionHash")
        val _columnIndexOfClaimAmount: Int = getColumnIndexOrThrow(_stmt, "claimAmount")
        val _columnIndexOfClaimDay: Int = getColumnIndexOrThrow(_stmt, "claimDay")
        val _columnIndexOfClaimDeadline: Int = getColumnIndexOrThrow(_stmt, "claimDeadline")
        val _columnIndexOfMockLocation: Int = getColumnIndexOrThrow(_stmt, "mockLocation")
        val _columnIndexOfLaps: Int = getColumnIndexOrThrow(_stmt, "laps")
        val _result: WalkSessionEntity?
        if (_stmt.step()) {
          val _tmpId: Long
          _tmpId = _stmt.getLong(_columnIndexOfId)
          val _tmpStartedAt: Long
          _tmpStartedAt = _stmt.getLong(_columnIndexOfStartedAt)
          val _tmpEndedAt: Long
          _tmpEndedAt = _stmt.getLong(_columnIndexOfEndedAt)
          val _tmpSteps: Int
          _tmpSteps = _stmt.getLong(_columnIndexOfSteps).toInt()
          val _tmpDurationSec: Long
          _tmpDurationSec = _stmt.getLong(_columnIndexOfDurationSec)
          val _tmpDistanceMeters: Double
          _tmpDistanceMeters = _stmt.getDouble(_columnIndexOfDistanceMeters)
          val _tmpCalories: Double
          _tmpCalories = _stmt.getDouble(_columnIndexOfCalories)
          val _tmpPointsEarned: Double
          _tmpPointsEarned = _stmt.getDouble(_columnIndexOfPointsEarned)
          val _tmpRecordingOwner: String
          _tmpRecordingOwner = _stmt.getText(_columnIndexOfRecordingOwner)
          val _tmpTrack: String
          _tmpTrack = _stmt.getText(_columnIndexOfTrack)
          val _tmpBoostBps: Int
          _tmpBoostBps = _stmt.getLong(_columnIndexOfBoostBps).toInt()
          val _tmpPartySize: Int
          _tmpPartySize = _stmt.getLong(_columnIndexOfPartySize).toInt()
          val _tmpFaction: String
          _tmpFaction = _stmt.getText(_columnIndexOfFaction)
          val _tmpCrewId: String
          _tmpCrewId = _stmt.getText(_columnIndexOfCrewId)
          val _tmpUploadState: String
          _tmpUploadState = _stmt.getText(_columnIndexOfUploadState)
          val _tmpUploadAttemptedAt: Long
          _tmpUploadAttemptedAt = _stmt.getLong(_columnIndexOfUploadAttemptedAt)
          val _tmpUploadAttempts: Int
          _tmpUploadAttempts = _stmt.getLong(_columnIndexOfUploadAttempts).toInt()
          val _tmpUploadError: String
          _tmpUploadError = _stmt.getText(_columnIndexOfUploadError)
          val _tmpVerdict: String
          _tmpVerdict = _stmt.getText(_columnIndexOfVerdict)
          val _tmpClaimSignature: String
          _tmpClaimSignature = _stmt.getText(_columnIndexOfClaimSignature)
          val _tmpClaimSessionHash: String
          _tmpClaimSessionHash = _stmt.getText(_columnIndexOfClaimSessionHash)
          val _tmpClaimAmount: String
          _tmpClaimAmount = _stmt.getText(_columnIndexOfClaimAmount)
          val _tmpClaimDay: Long
          _tmpClaimDay = _stmt.getLong(_columnIndexOfClaimDay)
          val _tmpClaimDeadline: Long
          _tmpClaimDeadline = _stmt.getLong(_columnIndexOfClaimDeadline)
          val _tmpMockLocation: Boolean
          val _tmp: Int
          _tmp = _stmt.getLong(_columnIndexOfMockLocation).toInt()
          _tmpMockLocation = _tmp != 0
          val _tmpLaps: String
          _tmpLaps = _stmt.getText(_columnIndexOfLaps)
          _result = WalkSessionEntity(_tmpId,_tmpStartedAt,_tmpEndedAt,_tmpSteps,_tmpDurationSec,_tmpDistanceMeters,_tmpCalories,_tmpPointsEarned,_tmpRecordingOwner,_tmpTrack,_tmpBoostBps,_tmpPartySize,_tmpFaction,_tmpCrewId,_tmpUploadState,_tmpUploadAttemptedAt,_tmpUploadAttempts,_tmpUploadError,_tmpVerdict,_tmpClaimSignature,_tmpClaimSessionHash,_tmpClaimAmount,_tmpClaimDay,_tmpClaimDeadline,_tmpMockLocation,_tmpLaps)
        } else {
          _result = null
        }
        _result
      } finally {
        _stmt.close()
      }
    }
  }

  public override fun observeDurationSinceFor(owner: String, fromMillis: Long): Flow<Long> {
    val _sql: String = "SELECT COALESCE(SUM(durationSec), 0) FROM walk_sessions WHERE startedAt >= ? AND recordingOwner IN (?, 'legacy')"
    return createFlow(__db, false, arrayOf("walk_sessions")) { _connection ->
      val _stmt: SQLiteStatement = _connection.prepare(_sql)
      try {
        var _argIndex: Int = 1
        _stmt.bindLong(_argIndex, fromMillis)
        _argIndex = 2
        _stmt.bindText(_argIndex, owner)
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

  public override fun observeRunTotalsSinceFor(owner: String, fromMillis: Long): Flow<RunTotals> {
    val _sql: String = "SELECT COUNT(*) AS runs, COALESCE(SUM(distanceMeters), 0) AS meters FROM walk_sessions WHERE startedAt >= ? AND recordingOwner IN (?, 'legacy') AND verdict != 'VOID'"
    return createFlow(__db, false, arrayOf("walk_sessions")) { _connection ->
      val _stmt: SQLiteStatement = _connection.prepare(_sql)
      try {
        var _argIndex: Int = 1
        _stmt.bindLong(_argIndex, fromMillis)
        _argIndex = 2
        _stmt.bindText(_argIndex, owner)
        val _columnIndexOfRuns: Int = 0
        val _columnIndexOfMeters: Int = 1
        val _result: RunTotals
        if (_stmt.step()) {
          val _tmpRuns: Int
          _tmpRuns = _stmt.getLong(_columnIndexOfRuns).toInt()
          val _tmpMeters: Double
          _tmpMeters = _stmt.getDouble(_columnIndexOfMeters)
          _result = RunTotals(_tmpRuns,_tmpMeters)
        } else {
          error("The query result was empty, but expected a single row to return a NON-NULL object of type 'com.stepup.android.`data`.local.RunTotals'.")
        }
        _result
      } finally {
        _stmt.close()
      }
    }
  }

  public override suspend fun pendingUploads(limit: Int, owner: String): List<WalkSessionEntity> {
    val _sql: String = """
        |
        |        SELECT * FROM walk_sessions
        |         WHERE uploadState IN ('PENDING', 'FAILED')
        |           AND (steps > 0 OR (durationSec > 0 AND startedAt >= 1792335600000))
        |           AND recordingOwner = ?
        |         ORDER BY uploadAttemptedAt ASC, startedAt ASC
        |         LIMIT ?
        |        
        """.trimMargin()
    return performSuspending(__db, true, false) { _connection ->
      val _stmt: SQLiteStatement = _connection.prepare(_sql)
      try {
        var _argIndex: Int = 1
        _stmt.bindText(_argIndex, owner)
        _argIndex = 2
        _stmt.bindLong(_argIndex, limit.toLong())
        val _columnIndexOfId: Int = getColumnIndexOrThrow(_stmt, "id")
        val _columnIndexOfStartedAt: Int = getColumnIndexOrThrow(_stmt, "startedAt")
        val _columnIndexOfEndedAt: Int = getColumnIndexOrThrow(_stmt, "endedAt")
        val _columnIndexOfSteps: Int = getColumnIndexOrThrow(_stmt, "steps")
        val _columnIndexOfDurationSec: Int = getColumnIndexOrThrow(_stmt, "durationSec")
        val _columnIndexOfDistanceMeters: Int = getColumnIndexOrThrow(_stmt, "distanceMeters")
        val _columnIndexOfCalories: Int = getColumnIndexOrThrow(_stmt, "calories")
        val _columnIndexOfPointsEarned: Int = getColumnIndexOrThrow(_stmt, "pointsEarned")
        val _columnIndexOfRecordingOwner: Int = getColumnIndexOrThrow(_stmt, "recordingOwner")
        val _columnIndexOfTrack: Int = getColumnIndexOrThrow(_stmt, "track")
        val _columnIndexOfBoostBps: Int = getColumnIndexOrThrow(_stmt, "boostBps")
        val _columnIndexOfPartySize: Int = getColumnIndexOrThrow(_stmt, "partySize")
        val _columnIndexOfFaction: Int = getColumnIndexOrThrow(_stmt, "faction")
        val _columnIndexOfCrewId: Int = getColumnIndexOrThrow(_stmt, "crewId")
        val _columnIndexOfUploadState: Int = getColumnIndexOrThrow(_stmt, "uploadState")
        val _columnIndexOfUploadAttemptedAt: Int = getColumnIndexOrThrow(_stmt, "uploadAttemptedAt")
        val _columnIndexOfUploadAttempts: Int = getColumnIndexOrThrow(_stmt, "uploadAttempts")
        val _columnIndexOfUploadError: Int = getColumnIndexOrThrow(_stmt, "uploadError")
        val _columnIndexOfVerdict: Int = getColumnIndexOrThrow(_stmt, "verdict")
        val _columnIndexOfClaimSignature: Int = getColumnIndexOrThrow(_stmt, "claimSignature")
        val _columnIndexOfClaimSessionHash: Int = getColumnIndexOrThrow(_stmt, "claimSessionHash")
        val _columnIndexOfClaimAmount: Int = getColumnIndexOrThrow(_stmt, "claimAmount")
        val _columnIndexOfClaimDay: Int = getColumnIndexOrThrow(_stmt, "claimDay")
        val _columnIndexOfClaimDeadline: Int = getColumnIndexOrThrow(_stmt, "claimDeadline")
        val _columnIndexOfMockLocation: Int = getColumnIndexOrThrow(_stmt, "mockLocation")
        val _columnIndexOfLaps: Int = getColumnIndexOrThrow(_stmt, "laps")
        val _result: MutableList<WalkSessionEntity> = mutableListOf()
        while (_stmt.step()) {
          val _item: WalkSessionEntity
          val _tmpId: Long
          _tmpId = _stmt.getLong(_columnIndexOfId)
          val _tmpStartedAt: Long
          _tmpStartedAt = _stmt.getLong(_columnIndexOfStartedAt)
          val _tmpEndedAt: Long
          _tmpEndedAt = _stmt.getLong(_columnIndexOfEndedAt)
          val _tmpSteps: Int
          _tmpSteps = _stmt.getLong(_columnIndexOfSteps).toInt()
          val _tmpDurationSec: Long
          _tmpDurationSec = _stmt.getLong(_columnIndexOfDurationSec)
          val _tmpDistanceMeters: Double
          _tmpDistanceMeters = _stmt.getDouble(_columnIndexOfDistanceMeters)
          val _tmpCalories: Double
          _tmpCalories = _stmt.getDouble(_columnIndexOfCalories)
          val _tmpPointsEarned: Double
          _tmpPointsEarned = _stmt.getDouble(_columnIndexOfPointsEarned)
          val _tmpRecordingOwner: String
          _tmpRecordingOwner = _stmt.getText(_columnIndexOfRecordingOwner)
          val _tmpTrack: String
          _tmpTrack = _stmt.getText(_columnIndexOfTrack)
          val _tmpBoostBps: Int
          _tmpBoostBps = _stmt.getLong(_columnIndexOfBoostBps).toInt()
          val _tmpPartySize: Int
          _tmpPartySize = _stmt.getLong(_columnIndexOfPartySize).toInt()
          val _tmpFaction: String
          _tmpFaction = _stmt.getText(_columnIndexOfFaction)
          val _tmpCrewId: String
          _tmpCrewId = _stmt.getText(_columnIndexOfCrewId)
          val _tmpUploadState: String
          _tmpUploadState = _stmt.getText(_columnIndexOfUploadState)
          val _tmpUploadAttemptedAt: Long
          _tmpUploadAttemptedAt = _stmt.getLong(_columnIndexOfUploadAttemptedAt)
          val _tmpUploadAttempts: Int
          _tmpUploadAttempts = _stmt.getLong(_columnIndexOfUploadAttempts).toInt()
          val _tmpUploadError: String
          _tmpUploadError = _stmt.getText(_columnIndexOfUploadError)
          val _tmpVerdict: String
          _tmpVerdict = _stmt.getText(_columnIndexOfVerdict)
          val _tmpClaimSignature: String
          _tmpClaimSignature = _stmt.getText(_columnIndexOfClaimSignature)
          val _tmpClaimSessionHash: String
          _tmpClaimSessionHash = _stmt.getText(_columnIndexOfClaimSessionHash)
          val _tmpClaimAmount: String
          _tmpClaimAmount = _stmt.getText(_columnIndexOfClaimAmount)
          val _tmpClaimDay: Long
          _tmpClaimDay = _stmt.getLong(_columnIndexOfClaimDay)
          val _tmpClaimDeadline: Long
          _tmpClaimDeadline = _stmt.getLong(_columnIndexOfClaimDeadline)
          val _tmpMockLocation: Boolean
          val _tmp: Int
          _tmp = _stmt.getLong(_columnIndexOfMockLocation).toInt()
          _tmpMockLocation = _tmp != 0
          val _tmpLaps: String
          _tmpLaps = _stmt.getText(_columnIndexOfLaps)
          _item = WalkSessionEntity(_tmpId,_tmpStartedAt,_tmpEndedAt,_tmpSteps,_tmpDurationSec,_tmpDistanceMeters,_tmpCalories,_tmpPointsEarned,_tmpRecordingOwner,_tmpTrack,_tmpBoostBps,_tmpPartySize,_tmpFaction,_tmpCrewId,_tmpUploadState,_tmpUploadAttemptedAt,_tmpUploadAttempts,_tmpUploadError,_tmpVerdict,_tmpClaimSignature,_tmpClaimSessionHash,_tmpClaimAmount,_tmpClaimDay,_tmpClaimDeadline,_tmpMockLocation,_tmpLaps)
          _result.add(_item)
        }
        _result
      } finally {
        _stmt.close()
      }
    }
  }

  public override fun observePendingUploadCount(): Flow<Int> {
    val _sql: String = "SELECT COUNT(*) FROM walk_sessions WHERE uploadState IN ('PENDING', 'FAILED') AND (steps > 0 OR (durationSec > 0 AND startedAt >= 1792335600000))"
    return createFlow(__db, false, arrayOf("walk_sessions")) { _connection ->
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

  public override fun observePendingUploadCountFor(owner: String): Flow<Int> {
    val _sql: String = "SELECT COUNT(*) FROM walk_sessions WHERE recordingOwner = ? AND uploadState IN ('PENDING', 'FAILED') AND (steps > 0 OR (durationSec > 0 AND startedAt >= 1792335600000))"
    return createFlow(__db, false, arrayOf("walk_sessions")) { _connection ->
      val _stmt: SQLiteStatement = _connection.prepare(_sql)
      try {
        var _argIndex: Int = 1
        _stmt.bindText(_argIndex, owner)
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

  public override suspend fun crewDistances(fromMillis: Long): List<CrewDistance> {
    val _sql: String = """
        |
        |        SELECT crewId AS crewId,
        |               COALESCE(SUM(distanceMeters), 0) AS meters,
        |               COUNT(*) AS runs
        |          FROM walk_sessions
        |         WHERE crewId != ''
        |           AND startedAt >= ?
        |         GROUP BY crewId
        |        
        """.trimMargin()
    return performSuspending(__db, true, false) { _connection ->
      val _stmt: SQLiteStatement = _connection.prepare(_sql)
      try {
        var _argIndex: Int = 1
        _stmt.bindLong(_argIndex, fromMillis)
        val _columnIndexOfCrewId: Int = 0
        val _columnIndexOfMeters: Int = 1
        val _columnIndexOfRuns: Int = 2
        val _result: MutableList<CrewDistance> = mutableListOf()
        while (_stmt.step()) {
          val _item: CrewDistance
          val _tmpCrewId: String
          _tmpCrewId = _stmt.getText(_columnIndexOfCrewId)
          val _tmpMeters: Double
          _tmpMeters = _stmt.getDouble(_columnIndexOfMeters)
          val _tmpRuns: Int
          _tmpRuns = _stmt.getLong(_columnIndexOfRuns).toInt()
          _item = CrewDistance(_tmpCrewId,_tmpMeters,_tmpRuns)
          _result.add(_item)
        }
        _result
      } finally {
        _stmt.close()
      }
    }
  }

  public override fun observeRecordRows(
    owner: String,
    from: Long,
    until: Long,
    limit: Int,
  ): Flow<List<RunRecordRow>> {
    val _sql: String = """
        |
        |        SELECT id, startedAt, endedAt, durationSec, distanceMeters, steps, uploadState, length(track) > 0 AS hasTrack
        |          FROM walk_sessions
        |         WHERE recordingOwner IN (?, 'legacy') AND startedAt >= ? AND startedAt < ?
        |         ORDER BY startedAt DESC, id DESC
        |         LIMIT ?
        |        
        """.trimMargin()
    return createFlow(__db, false, arrayOf("walk_sessions")) { _connection ->
      val _stmt: SQLiteStatement = _connection.prepare(_sql)
      try {
        var _argIndex: Int = 1
        _stmt.bindText(_argIndex, owner)
        _argIndex = 2
        _stmt.bindLong(_argIndex, from)
        _argIndex = 3
        _stmt.bindLong(_argIndex, until)
        _argIndex = 4
        _stmt.bindLong(_argIndex, limit.toLong())
        val _columnIndexOfId: Int = 0
        val _columnIndexOfStartedAt: Int = 1
        val _columnIndexOfEndedAt: Int = 2
        val _columnIndexOfDurationSec: Int = 3
        val _columnIndexOfDistanceMeters: Int = 4
        val _columnIndexOfSteps: Int = 5
        val _columnIndexOfUploadState: Int = 6
        val _columnIndexOfHasTrack: Int = 7
        val _result: MutableList<RunRecordRow> = mutableListOf()
        while (_stmt.step()) {
          val _item: RunRecordRow
          val _tmpId: Long
          _tmpId = _stmt.getLong(_columnIndexOfId)
          val _tmpStartedAt: Long
          _tmpStartedAt = _stmt.getLong(_columnIndexOfStartedAt)
          val _tmpEndedAt: Long
          _tmpEndedAt = _stmt.getLong(_columnIndexOfEndedAt)
          val _tmpDurationSec: Long
          _tmpDurationSec = _stmt.getLong(_columnIndexOfDurationSec)
          val _tmpDistanceMeters: Double
          _tmpDistanceMeters = _stmt.getDouble(_columnIndexOfDistanceMeters)
          val _tmpSteps: Int
          _tmpSteps = _stmt.getLong(_columnIndexOfSteps).toInt()
          val _tmpUploadState: String
          _tmpUploadState = _stmt.getText(_columnIndexOfUploadState)
          val _tmpHasTrack: Boolean
          val _tmp: Int
          _tmp = _stmt.getLong(_columnIndexOfHasTrack).toInt()
          _tmpHasTrack = _tmp != 0
          _item = RunRecordRow(_tmpId,_tmpStartedAt,_tmpEndedAt,_tmpDurationSec,_tmpDistanceMeters,_tmpSteps,_tmpUploadState,_tmpHasTrack)
          _result.add(_item)
        }
        _result
      } finally {
        _stmt.close()
      }
    }
  }

  public override fun observeRecordTotals(
    owner: String,
    from: Long,
    until: Long,
  ): Flow<RecordTotals> {
    val _sql: String = """
        |
        |        SELECT COUNT(*) AS runs,
        |               COALESCE(SUM(CASE WHEN distanceMeters > 0 AND verdict != 'VOID' THEN distanceMeters ELSE 0 END), 0) AS meters,
        |               COALESCE(SUM(durationSec), 0) AS seconds,
        |               COALESCE(SUM(CASE WHEN distanceMeters > 0 AND durationSec > 0 AND verdict != 'VOID' THEN distanceMeters ELSE 0 END), 0) AS pacedMeters,
        |               COALESCE(SUM(CASE WHEN distanceMeters > 0 AND durationSec > 0 AND verdict != 'VOID' THEN durationSec ELSE 0 END), 0) AS pacedSeconds,
        |               COALESCE(SUM(CASE WHEN distanceMeters > 0 AND verdict != 'VOID' THEN 1 ELSE 0 END), 0) AS measured
        |          FROM walk_sessions
        |         WHERE recordingOwner IN (?, 'legacy') AND startedAt >= ? AND startedAt < ?
        |        
        """.trimMargin()
    return createFlow(__db, false, arrayOf("walk_sessions")) { _connection ->
      val _stmt: SQLiteStatement = _connection.prepare(_sql)
      try {
        var _argIndex: Int = 1
        _stmt.bindText(_argIndex, owner)
        _argIndex = 2
        _stmt.bindLong(_argIndex, from)
        _argIndex = 3
        _stmt.bindLong(_argIndex, until)
        val _columnIndexOfRuns: Int = 0
        val _columnIndexOfMeters: Int = 1
        val _columnIndexOfSeconds: Int = 2
        val _columnIndexOfPacedMeters: Int = 3
        val _columnIndexOfPacedSeconds: Int = 4
        val _columnIndexOfMeasured: Int = 5
        val _result: RecordTotals
        if (_stmt.step()) {
          val _tmpRuns: Int
          _tmpRuns = _stmt.getLong(_columnIndexOfRuns).toInt()
          val _tmpMeters: Double
          _tmpMeters = _stmt.getDouble(_columnIndexOfMeters)
          val _tmpSeconds: Long
          _tmpSeconds = _stmt.getLong(_columnIndexOfSeconds)
          val _tmpPacedMeters: Double
          _tmpPacedMeters = _stmt.getDouble(_columnIndexOfPacedMeters)
          val _tmpPacedSeconds: Long
          _tmpPacedSeconds = _stmt.getLong(_columnIndexOfPacedSeconds)
          val _tmpMeasured: Int
          _tmpMeasured = _stmt.getLong(_columnIndexOfMeasured).toInt()
          _result = RecordTotals(_tmpRuns,_tmpMeters,_tmpSeconds,_tmpPacedMeters,_tmpPacedSeconds,_tmpMeasured)
        } else {
          error("The query result was empty, but expected a single row to return a NON-NULL object of type 'com.stepup.android.`data`.local.RecordTotals'.")
        }
        _result
      } finally {
        _stmt.close()
      }
    }
  }

  public override fun observeRunMarks(
    owner: String,
    from: Long,
    until: Long,
  ): Flow<List<RunMark>> {
    val _sql: String = "SELECT id, startedAt, CASE WHEN verdict != 'VOID' THEN distanceMeters ELSE 0 END AS meters FROM walk_sessions WHERE recordingOwner IN (?, 'legacy') AND startedAt >= ? AND startedAt < ?"
    return createFlow(__db, false, arrayOf("walk_sessions")) { _connection ->
      val _stmt: SQLiteStatement = _connection.prepare(_sql)
      try {
        var _argIndex: Int = 1
        _stmt.bindText(_argIndex, owner)
        _argIndex = 2
        _stmt.bindLong(_argIndex, from)
        _argIndex = 3
        _stmt.bindLong(_argIndex, until)
        val _columnIndexOfId: Int = 0
        val _columnIndexOfStartedAt: Int = 1
        val _columnIndexOfMeters: Int = 2
        val _result: MutableList<RunMark> = mutableListOf()
        while (_stmt.step()) {
          val _item: RunMark
          val _tmpId: Long
          _tmpId = _stmt.getLong(_columnIndexOfId)
          val _tmpStartedAt: Long
          _tmpStartedAt = _stmt.getLong(_columnIndexOfStartedAt)
          val _tmpMeters: Double
          _tmpMeters = _stmt.getDouble(_columnIndexOfMeters)
          _item = RunMark(_tmpStartedAt,_tmpMeters,_tmpId)
          _result.add(_item)
        }
        _result
      } finally {
        _stmt.close()
      }
    }
  }

  public override fun observeRecord(owner: String, id: Long): Flow<WalkSessionEntity?> {
    val _sql: String = "SELECT * FROM walk_sessions WHERE id = ? AND recordingOwner IN (?, 'legacy')"
    return createFlow(__db, false, arrayOf("walk_sessions")) { _connection ->
      val _stmt: SQLiteStatement = _connection.prepare(_sql)
      try {
        var _argIndex: Int = 1
        _stmt.bindLong(_argIndex, id)
        _argIndex = 2
        _stmt.bindText(_argIndex, owner)
        val _columnIndexOfId: Int = getColumnIndexOrThrow(_stmt, "id")
        val _columnIndexOfStartedAt: Int = getColumnIndexOrThrow(_stmt, "startedAt")
        val _columnIndexOfEndedAt: Int = getColumnIndexOrThrow(_stmt, "endedAt")
        val _columnIndexOfSteps: Int = getColumnIndexOrThrow(_stmt, "steps")
        val _columnIndexOfDurationSec: Int = getColumnIndexOrThrow(_stmt, "durationSec")
        val _columnIndexOfDistanceMeters: Int = getColumnIndexOrThrow(_stmt, "distanceMeters")
        val _columnIndexOfCalories: Int = getColumnIndexOrThrow(_stmt, "calories")
        val _columnIndexOfPointsEarned: Int = getColumnIndexOrThrow(_stmt, "pointsEarned")
        val _columnIndexOfRecordingOwner: Int = getColumnIndexOrThrow(_stmt, "recordingOwner")
        val _columnIndexOfTrack: Int = getColumnIndexOrThrow(_stmt, "track")
        val _columnIndexOfBoostBps: Int = getColumnIndexOrThrow(_stmt, "boostBps")
        val _columnIndexOfPartySize: Int = getColumnIndexOrThrow(_stmt, "partySize")
        val _columnIndexOfFaction: Int = getColumnIndexOrThrow(_stmt, "faction")
        val _columnIndexOfCrewId: Int = getColumnIndexOrThrow(_stmt, "crewId")
        val _columnIndexOfUploadState: Int = getColumnIndexOrThrow(_stmt, "uploadState")
        val _columnIndexOfUploadAttemptedAt: Int = getColumnIndexOrThrow(_stmt, "uploadAttemptedAt")
        val _columnIndexOfUploadAttempts: Int = getColumnIndexOrThrow(_stmt, "uploadAttempts")
        val _columnIndexOfUploadError: Int = getColumnIndexOrThrow(_stmt, "uploadError")
        val _columnIndexOfVerdict: Int = getColumnIndexOrThrow(_stmt, "verdict")
        val _columnIndexOfClaimSignature: Int = getColumnIndexOrThrow(_stmt, "claimSignature")
        val _columnIndexOfClaimSessionHash: Int = getColumnIndexOrThrow(_stmt, "claimSessionHash")
        val _columnIndexOfClaimAmount: Int = getColumnIndexOrThrow(_stmt, "claimAmount")
        val _columnIndexOfClaimDay: Int = getColumnIndexOrThrow(_stmt, "claimDay")
        val _columnIndexOfClaimDeadline: Int = getColumnIndexOrThrow(_stmt, "claimDeadline")
        val _columnIndexOfMockLocation: Int = getColumnIndexOrThrow(_stmt, "mockLocation")
        val _columnIndexOfLaps: Int = getColumnIndexOrThrow(_stmt, "laps")
        val _result: WalkSessionEntity?
        if (_stmt.step()) {
          val _tmpId: Long
          _tmpId = _stmt.getLong(_columnIndexOfId)
          val _tmpStartedAt: Long
          _tmpStartedAt = _stmt.getLong(_columnIndexOfStartedAt)
          val _tmpEndedAt: Long
          _tmpEndedAt = _stmt.getLong(_columnIndexOfEndedAt)
          val _tmpSteps: Int
          _tmpSteps = _stmt.getLong(_columnIndexOfSteps).toInt()
          val _tmpDurationSec: Long
          _tmpDurationSec = _stmt.getLong(_columnIndexOfDurationSec)
          val _tmpDistanceMeters: Double
          _tmpDistanceMeters = _stmt.getDouble(_columnIndexOfDistanceMeters)
          val _tmpCalories: Double
          _tmpCalories = _stmt.getDouble(_columnIndexOfCalories)
          val _tmpPointsEarned: Double
          _tmpPointsEarned = _stmt.getDouble(_columnIndexOfPointsEarned)
          val _tmpRecordingOwner: String
          _tmpRecordingOwner = _stmt.getText(_columnIndexOfRecordingOwner)
          val _tmpTrack: String
          _tmpTrack = _stmt.getText(_columnIndexOfTrack)
          val _tmpBoostBps: Int
          _tmpBoostBps = _stmt.getLong(_columnIndexOfBoostBps).toInt()
          val _tmpPartySize: Int
          _tmpPartySize = _stmt.getLong(_columnIndexOfPartySize).toInt()
          val _tmpFaction: String
          _tmpFaction = _stmt.getText(_columnIndexOfFaction)
          val _tmpCrewId: String
          _tmpCrewId = _stmt.getText(_columnIndexOfCrewId)
          val _tmpUploadState: String
          _tmpUploadState = _stmt.getText(_columnIndexOfUploadState)
          val _tmpUploadAttemptedAt: Long
          _tmpUploadAttemptedAt = _stmt.getLong(_columnIndexOfUploadAttemptedAt)
          val _tmpUploadAttempts: Int
          _tmpUploadAttempts = _stmt.getLong(_columnIndexOfUploadAttempts).toInt()
          val _tmpUploadError: String
          _tmpUploadError = _stmt.getText(_columnIndexOfUploadError)
          val _tmpVerdict: String
          _tmpVerdict = _stmt.getText(_columnIndexOfVerdict)
          val _tmpClaimSignature: String
          _tmpClaimSignature = _stmt.getText(_columnIndexOfClaimSignature)
          val _tmpClaimSessionHash: String
          _tmpClaimSessionHash = _stmt.getText(_columnIndexOfClaimSessionHash)
          val _tmpClaimAmount: String
          _tmpClaimAmount = _stmt.getText(_columnIndexOfClaimAmount)
          val _tmpClaimDay: Long
          _tmpClaimDay = _stmt.getLong(_columnIndexOfClaimDay)
          val _tmpClaimDeadline: Long
          _tmpClaimDeadline = _stmt.getLong(_columnIndexOfClaimDeadline)
          val _tmpMockLocation: Boolean
          val _tmp: Int
          _tmp = _stmt.getLong(_columnIndexOfMockLocation).toInt()
          _tmpMockLocation = _tmp != 0
          val _tmpLaps: String
          _tmpLaps = _stmt.getText(_columnIndexOfLaps)
          _result = WalkSessionEntity(_tmpId,_tmpStartedAt,_tmpEndedAt,_tmpSteps,_tmpDurationSec,_tmpDistanceMeters,_tmpCalories,_tmpPointsEarned,_tmpRecordingOwner,_tmpTrack,_tmpBoostBps,_tmpPartySize,_tmpFaction,_tmpCrewId,_tmpUploadState,_tmpUploadAttemptedAt,_tmpUploadAttempts,_tmpUploadError,_tmpVerdict,_tmpClaimSignature,_tmpClaimSessionHash,_tmpClaimAmount,_tmpClaimDay,_tmpClaimDeadline,_tmpMockLocation,_tmpLaps)
        } else {
          _result = null
        }
        _result
      } finally {
        _stmt.close()
      }
    }
  }

  public override fun observeFirstRecordAt(owner: String): Flow<Long?> {
    val _sql: String = "SELECT MIN(startedAt) FROM walk_sessions WHERE recordingOwner IN (?, 'legacy')"
    return createFlow(__db, false, arrayOf("walk_sessions")) { _connection ->
      val _stmt: SQLiteStatement = _connection.prepare(_sql)
      try {
        var _argIndex: Int = 1
        _stmt.bindText(_argIndex, owner)
        val _result: Long?
        if (_stmt.step()) {
          val _tmp: Long?
          if (_stmt.isNull(0)) {
            _tmp = null
          } else {
            _tmp = _stmt.getLong(0)
          }
          _result = _tmp
        } else {
          _result = null
        }
        _result
      } finally {
        _stmt.close()
      }
    }
  }

  public override suspend fun signedWithFollowUp(
    owner: String,
    marker: String,
    limit: Int,
  ): List<WalkSessionEntity> {
    val _sql: String = "SELECT * FROM walk_sessions WHERE recordingOwner = ? AND uploadState = 'SIGNED' AND uploadError = ? ORDER BY startedAt ASC LIMIT ?"
    return performSuspending(__db, true, false) { _connection ->
      val _stmt: SQLiteStatement = _connection.prepare(_sql)
      try {
        var _argIndex: Int = 1
        _stmt.bindText(_argIndex, owner)
        _argIndex = 2
        _stmt.bindText(_argIndex, marker)
        _argIndex = 3
        _stmt.bindLong(_argIndex, limit.toLong())
        val _columnIndexOfId: Int = getColumnIndexOrThrow(_stmt, "id")
        val _columnIndexOfStartedAt: Int = getColumnIndexOrThrow(_stmt, "startedAt")
        val _columnIndexOfEndedAt: Int = getColumnIndexOrThrow(_stmt, "endedAt")
        val _columnIndexOfSteps: Int = getColumnIndexOrThrow(_stmt, "steps")
        val _columnIndexOfDurationSec: Int = getColumnIndexOrThrow(_stmt, "durationSec")
        val _columnIndexOfDistanceMeters: Int = getColumnIndexOrThrow(_stmt, "distanceMeters")
        val _columnIndexOfCalories: Int = getColumnIndexOrThrow(_stmt, "calories")
        val _columnIndexOfPointsEarned: Int = getColumnIndexOrThrow(_stmt, "pointsEarned")
        val _columnIndexOfRecordingOwner: Int = getColumnIndexOrThrow(_stmt, "recordingOwner")
        val _columnIndexOfTrack: Int = getColumnIndexOrThrow(_stmt, "track")
        val _columnIndexOfBoostBps: Int = getColumnIndexOrThrow(_stmt, "boostBps")
        val _columnIndexOfPartySize: Int = getColumnIndexOrThrow(_stmt, "partySize")
        val _columnIndexOfFaction: Int = getColumnIndexOrThrow(_stmt, "faction")
        val _columnIndexOfCrewId: Int = getColumnIndexOrThrow(_stmt, "crewId")
        val _columnIndexOfUploadState: Int = getColumnIndexOrThrow(_stmt, "uploadState")
        val _columnIndexOfUploadAttemptedAt: Int = getColumnIndexOrThrow(_stmt, "uploadAttemptedAt")
        val _columnIndexOfUploadAttempts: Int = getColumnIndexOrThrow(_stmt, "uploadAttempts")
        val _columnIndexOfUploadError: Int = getColumnIndexOrThrow(_stmt, "uploadError")
        val _columnIndexOfVerdict: Int = getColumnIndexOrThrow(_stmt, "verdict")
        val _columnIndexOfClaimSignature: Int = getColumnIndexOrThrow(_stmt, "claimSignature")
        val _columnIndexOfClaimSessionHash: Int = getColumnIndexOrThrow(_stmt, "claimSessionHash")
        val _columnIndexOfClaimAmount: Int = getColumnIndexOrThrow(_stmt, "claimAmount")
        val _columnIndexOfClaimDay: Int = getColumnIndexOrThrow(_stmt, "claimDay")
        val _columnIndexOfClaimDeadline: Int = getColumnIndexOrThrow(_stmt, "claimDeadline")
        val _columnIndexOfMockLocation: Int = getColumnIndexOrThrow(_stmt, "mockLocation")
        val _columnIndexOfLaps: Int = getColumnIndexOrThrow(_stmt, "laps")
        val _result: MutableList<WalkSessionEntity> = mutableListOf()
        while (_stmt.step()) {
          val _item: WalkSessionEntity
          val _tmpId: Long
          _tmpId = _stmt.getLong(_columnIndexOfId)
          val _tmpStartedAt: Long
          _tmpStartedAt = _stmt.getLong(_columnIndexOfStartedAt)
          val _tmpEndedAt: Long
          _tmpEndedAt = _stmt.getLong(_columnIndexOfEndedAt)
          val _tmpSteps: Int
          _tmpSteps = _stmt.getLong(_columnIndexOfSteps).toInt()
          val _tmpDurationSec: Long
          _tmpDurationSec = _stmt.getLong(_columnIndexOfDurationSec)
          val _tmpDistanceMeters: Double
          _tmpDistanceMeters = _stmt.getDouble(_columnIndexOfDistanceMeters)
          val _tmpCalories: Double
          _tmpCalories = _stmt.getDouble(_columnIndexOfCalories)
          val _tmpPointsEarned: Double
          _tmpPointsEarned = _stmt.getDouble(_columnIndexOfPointsEarned)
          val _tmpRecordingOwner: String
          _tmpRecordingOwner = _stmt.getText(_columnIndexOfRecordingOwner)
          val _tmpTrack: String
          _tmpTrack = _stmt.getText(_columnIndexOfTrack)
          val _tmpBoostBps: Int
          _tmpBoostBps = _stmt.getLong(_columnIndexOfBoostBps).toInt()
          val _tmpPartySize: Int
          _tmpPartySize = _stmt.getLong(_columnIndexOfPartySize).toInt()
          val _tmpFaction: String
          _tmpFaction = _stmt.getText(_columnIndexOfFaction)
          val _tmpCrewId: String
          _tmpCrewId = _stmt.getText(_columnIndexOfCrewId)
          val _tmpUploadState: String
          _tmpUploadState = _stmt.getText(_columnIndexOfUploadState)
          val _tmpUploadAttemptedAt: Long
          _tmpUploadAttemptedAt = _stmt.getLong(_columnIndexOfUploadAttemptedAt)
          val _tmpUploadAttempts: Int
          _tmpUploadAttempts = _stmt.getLong(_columnIndexOfUploadAttempts).toInt()
          val _tmpUploadError: String
          _tmpUploadError = _stmt.getText(_columnIndexOfUploadError)
          val _tmpVerdict: String
          _tmpVerdict = _stmt.getText(_columnIndexOfVerdict)
          val _tmpClaimSignature: String
          _tmpClaimSignature = _stmt.getText(_columnIndexOfClaimSignature)
          val _tmpClaimSessionHash: String
          _tmpClaimSessionHash = _stmt.getText(_columnIndexOfClaimSessionHash)
          val _tmpClaimAmount: String
          _tmpClaimAmount = _stmt.getText(_columnIndexOfClaimAmount)
          val _tmpClaimDay: Long
          _tmpClaimDay = _stmt.getLong(_columnIndexOfClaimDay)
          val _tmpClaimDeadline: Long
          _tmpClaimDeadline = _stmt.getLong(_columnIndexOfClaimDeadline)
          val _tmpMockLocation: Boolean
          val _tmp: Int
          _tmp = _stmt.getLong(_columnIndexOfMockLocation).toInt()
          _tmpMockLocation = _tmp != 0
          val _tmpLaps: String
          _tmpLaps = _stmt.getText(_columnIndexOfLaps)
          _item = WalkSessionEntity(_tmpId,_tmpStartedAt,_tmpEndedAt,_tmpSteps,_tmpDurationSec,_tmpDistanceMeters,_tmpCalories,_tmpPointsEarned,_tmpRecordingOwner,_tmpTrack,_tmpBoostBps,_tmpPartySize,_tmpFaction,_tmpCrewId,_tmpUploadState,_tmpUploadAttemptedAt,_tmpUploadAttempts,_tmpUploadError,_tmpVerdict,_tmpClaimSignature,_tmpClaimSessionHash,_tmpClaimAmount,_tmpClaimDay,_tmpClaimDeadline,_tmpMockLocation,_tmpLaps)
          _result.add(_item)
        }
        _result
      } finally {
        _stmt.close()
      }
    }
  }

  public override suspend fun countFor(owner: String): Int {
    val _sql: String = "SELECT COUNT(*) FROM walk_sessions WHERE recordingOwner IN (?, 'legacy')"
    return performSuspending(__db, true, false) { _connection ->
      val _stmt: SQLiteStatement = _connection.prepare(_sql)
      try {
        var _argIndex: Int = 1
        _stmt.bindText(_argIndex, owner)
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

  public override suspend fun pendingSince(owner: String, since: Long): Int {
    val _sql: String = "SELECT COUNT(*) FROM walk_sessions WHERE recordingOwner = ? AND uploadState IN ('PENDING', 'FAILED') AND (steps > 0 OR (durationSec > 0 AND startedAt >= 1792335600000)) AND endedAt >= ?"
    return performSuspending(__db, true, false) { _connection ->
      val _stmt: SQLiteStatement = _connection.prepare(_sql)
      try {
        var _argIndex: Int = 1
        _stmt.bindText(_argIndex, owner)
        _argIndex = 2
        _stmt.bindLong(_argIndex, since)
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

  public override suspend fun idForStart(owner: String, startedAt: Long): Long? {
    val _sql: String = "SELECT id FROM walk_sessions WHERE recordingOwner IN (?, 'legacy') AND startedAt = ? ORDER BY id DESC LIMIT 1"
    return performSuspending(__db, true, false) { _connection ->
      val _stmt: SQLiteStatement = _connection.prepare(_sql)
      try {
        var _argIndex: Int = 1
        _stmt.bindText(_argIndex, owner)
        _argIndex = 2
        _stmt.bindLong(_argIndex, startedAt)
        val _result: Long?
        if (_stmt.step()) {
          if (_stmt.isNull(0)) {
            _result = null
          } else {
            _result = _stmt.getLong(0)
          }
        } else {
          _result = null
        }
        _result
      } finally {
        _stmt.close()
      }
    }
  }

  public override suspend fun trackOf(owner: String, id: Long): String? {
    val _sql: String = "SELECT track FROM walk_sessions WHERE id = ? AND recordingOwner IN (?, 'legacy')"
    return performSuspending(__db, true, false) { _connection ->
      val _stmt: SQLiteStatement = _connection.prepare(_sql)
      try {
        var _argIndex: Int = 1
        _stmt.bindLong(_argIndex, id)
        _argIndex = 2
        _stmt.bindText(_argIndex, owner)
        val _result: String?
        if (_stmt.step()) {
          if (_stmt.isNull(0)) {
            _result = null
          } else {
            _result = _stmt.getText(0)
          }
        } else {
          _result = null
        }
        _result
      } finally {
        _stmt.close()
      }
    }
  }

  public override fun observeCoachRuns(
    owner: String,
    from: Long,
    until: Long,
  ): Flow<List<CoachRun>> {
    val _sql: String = "SELECT id, startedAt, distanceMeters AS meters, durationSec AS seconds FROM walk_sessions WHERE recordingOwner = ? AND startedAt >= ? AND startedAt < ? AND distanceMeters > 0 AND durationSec > 0 AND verdict != 'VOID'"
    return createFlow(__db, false, arrayOf("walk_sessions")) { _connection ->
      val _stmt: SQLiteStatement = _connection.prepare(_sql)
      try {
        var _argIndex: Int = 1
        _stmt.bindText(_argIndex, owner)
        _argIndex = 2
        _stmt.bindLong(_argIndex, from)
        _argIndex = 3
        _stmt.bindLong(_argIndex, until)
        val _columnIndexOfId: Int = 0
        val _columnIndexOfStartedAt: Int = 1
        val _columnIndexOfMeters: Int = 2
        val _columnIndexOfSeconds: Int = 3
        val _result: MutableList<CoachRun> = mutableListOf()
        while (_stmt.step()) {
          val _item: CoachRun
          val _tmpId: Long
          _tmpId = _stmt.getLong(_columnIndexOfId)
          val _tmpStartedAt: Long
          _tmpStartedAt = _stmt.getLong(_columnIndexOfStartedAt)
          val _tmpMeters: Double
          _tmpMeters = _stmt.getDouble(_columnIndexOfMeters)
          val _tmpSeconds: Long
          _tmpSeconds = _stmt.getLong(_columnIndexOfSeconds)
          _item = CoachRun(_tmpId,_tmpStartedAt,_tmpMeters,_tmpSeconds)
          _result.add(_item)
        }
        _result
      } finally {
        _stmt.close()
      }
    }
  }

  public override fun observeCoachRows(owner: String, limit: Int): Flow<List<RunRecordRow>> {
    val _sql: String = """
        |
        |        SELECT id, startedAt, endedAt, durationSec, distanceMeters, steps, uploadState, length(track) > 0 AS hasTrack
        |          FROM walk_sessions
        |         WHERE recordingOwner = ? AND distanceMeters > 0 AND durationSec > 0
        |         ORDER BY startedAt DESC, id DESC
        |         LIMIT ?
        |        
        """.trimMargin()
    return createFlow(__db, false, arrayOf("walk_sessions")) { _connection ->
      val _stmt: SQLiteStatement = _connection.prepare(_sql)
      try {
        var _argIndex: Int = 1
        _stmt.bindText(_argIndex, owner)
        _argIndex = 2
        _stmt.bindLong(_argIndex, limit.toLong())
        val _columnIndexOfId: Int = 0
        val _columnIndexOfStartedAt: Int = 1
        val _columnIndexOfEndedAt: Int = 2
        val _columnIndexOfDurationSec: Int = 3
        val _columnIndexOfDistanceMeters: Int = 4
        val _columnIndexOfSteps: Int = 5
        val _columnIndexOfUploadState: Int = 6
        val _columnIndexOfHasTrack: Int = 7
        val _result: MutableList<RunRecordRow> = mutableListOf()
        while (_stmt.step()) {
          val _item: RunRecordRow
          val _tmpId: Long
          _tmpId = _stmt.getLong(_columnIndexOfId)
          val _tmpStartedAt: Long
          _tmpStartedAt = _stmt.getLong(_columnIndexOfStartedAt)
          val _tmpEndedAt: Long
          _tmpEndedAt = _stmt.getLong(_columnIndexOfEndedAt)
          val _tmpDurationSec: Long
          _tmpDurationSec = _stmt.getLong(_columnIndexOfDurationSec)
          val _tmpDistanceMeters: Double
          _tmpDistanceMeters = _stmt.getDouble(_columnIndexOfDistanceMeters)
          val _tmpSteps: Int
          _tmpSteps = _stmt.getLong(_columnIndexOfSteps).toInt()
          val _tmpUploadState: String
          _tmpUploadState = _stmt.getText(_columnIndexOfUploadState)
          val _tmpHasTrack: Boolean
          val _tmp: Int
          _tmp = _stmt.getLong(_columnIndexOfHasTrack).toInt()
          _tmpHasTrack = _tmp != 0
          _item = RunRecordRow(_tmpId,_tmpStartedAt,_tmpEndedAt,_tmpDurationSec,_tmpDistanceMeters,_tmpSteps,_tmpUploadState,_tmpHasTrack)
          _result.add(_item)
        }
        _result
      } finally {
        _stmt.close()
      }
    }
  }

  public override fun observeCoachRecord(owner: String, id: Long): Flow<WalkSessionEntity?> {
    val _sql: String = "SELECT * FROM walk_sessions WHERE id = ? AND recordingOwner = ?"
    return createFlow(__db, false, arrayOf("walk_sessions")) { _connection ->
      val _stmt: SQLiteStatement = _connection.prepare(_sql)
      try {
        var _argIndex: Int = 1
        _stmt.bindLong(_argIndex, id)
        _argIndex = 2
        _stmt.bindText(_argIndex, owner)
        val _columnIndexOfId: Int = getColumnIndexOrThrow(_stmt, "id")
        val _columnIndexOfStartedAt: Int = getColumnIndexOrThrow(_stmt, "startedAt")
        val _columnIndexOfEndedAt: Int = getColumnIndexOrThrow(_stmt, "endedAt")
        val _columnIndexOfSteps: Int = getColumnIndexOrThrow(_stmt, "steps")
        val _columnIndexOfDurationSec: Int = getColumnIndexOrThrow(_stmt, "durationSec")
        val _columnIndexOfDistanceMeters: Int = getColumnIndexOrThrow(_stmt, "distanceMeters")
        val _columnIndexOfCalories: Int = getColumnIndexOrThrow(_stmt, "calories")
        val _columnIndexOfPointsEarned: Int = getColumnIndexOrThrow(_stmt, "pointsEarned")
        val _columnIndexOfRecordingOwner: Int = getColumnIndexOrThrow(_stmt, "recordingOwner")
        val _columnIndexOfTrack: Int = getColumnIndexOrThrow(_stmt, "track")
        val _columnIndexOfBoostBps: Int = getColumnIndexOrThrow(_stmt, "boostBps")
        val _columnIndexOfPartySize: Int = getColumnIndexOrThrow(_stmt, "partySize")
        val _columnIndexOfFaction: Int = getColumnIndexOrThrow(_stmt, "faction")
        val _columnIndexOfCrewId: Int = getColumnIndexOrThrow(_stmt, "crewId")
        val _columnIndexOfUploadState: Int = getColumnIndexOrThrow(_stmt, "uploadState")
        val _columnIndexOfUploadAttemptedAt: Int = getColumnIndexOrThrow(_stmt, "uploadAttemptedAt")
        val _columnIndexOfUploadAttempts: Int = getColumnIndexOrThrow(_stmt, "uploadAttempts")
        val _columnIndexOfUploadError: Int = getColumnIndexOrThrow(_stmt, "uploadError")
        val _columnIndexOfVerdict: Int = getColumnIndexOrThrow(_stmt, "verdict")
        val _columnIndexOfClaimSignature: Int = getColumnIndexOrThrow(_stmt, "claimSignature")
        val _columnIndexOfClaimSessionHash: Int = getColumnIndexOrThrow(_stmt, "claimSessionHash")
        val _columnIndexOfClaimAmount: Int = getColumnIndexOrThrow(_stmt, "claimAmount")
        val _columnIndexOfClaimDay: Int = getColumnIndexOrThrow(_stmt, "claimDay")
        val _columnIndexOfClaimDeadline: Int = getColumnIndexOrThrow(_stmt, "claimDeadline")
        val _columnIndexOfMockLocation: Int = getColumnIndexOrThrow(_stmt, "mockLocation")
        val _columnIndexOfLaps: Int = getColumnIndexOrThrow(_stmt, "laps")
        val _result: WalkSessionEntity?
        if (_stmt.step()) {
          val _tmpId: Long
          _tmpId = _stmt.getLong(_columnIndexOfId)
          val _tmpStartedAt: Long
          _tmpStartedAt = _stmt.getLong(_columnIndexOfStartedAt)
          val _tmpEndedAt: Long
          _tmpEndedAt = _stmt.getLong(_columnIndexOfEndedAt)
          val _tmpSteps: Int
          _tmpSteps = _stmt.getLong(_columnIndexOfSteps).toInt()
          val _tmpDurationSec: Long
          _tmpDurationSec = _stmt.getLong(_columnIndexOfDurationSec)
          val _tmpDistanceMeters: Double
          _tmpDistanceMeters = _stmt.getDouble(_columnIndexOfDistanceMeters)
          val _tmpCalories: Double
          _tmpCalories = _stmt.getDouble(_columnIndexOfCalories)
          val _tmpPointsEarned: Double
          _tmpPointsEarned = _stmt.getDouble(_columnIndexOfPointsEarned)
          val _tmpRecordingOwner: String
          _tmpRecordingOwner = _stmt.getText(_columnIndexOfRecordingOwner)
          val _tmpTrack: String
          _tmpTrack = _stmt.getText(_columnIndexOfTrack)
          val _tmpBoostBps: Int
          _tmpBoostBps = _stmt.getLong(_columnIndexOfBoostBps).toInt()
          val _tmpPartySize: Int
          _tmpPartySize = _stmt.getLong(_columnIndexOfPartySize).toInt()
          val _tmpFaction: String
          _tmpFaction = _stmt.getText(_columnIndexOfFaction)
          val _tmpCrewId: String
          _tmpCrewId = _stmt.getText(_columnIndexOfCrewId)
          val _tmpUploadState: String
          _tmpUploadState = _stmt.getText(_columnIndexOfUploadState)
          val _tmpUploadAttemptedAt: Long
          _tmpUploadAttemptedAt = _stmt.getLong(_columnIndexOfUploadAttemptedAt)
          val _tmpUploadAttempts: Int
          _tmpUploadAttempts = _stmt.getLong(_columnIndexOfUploadAttempts).toInt()
          val _tmpUploadError: String
          _tmpUploadError = _stmt.getText(_columnIndexOfUploadError)
          val _tmpVerdict: String
          _tmpVerdict = _stmt.getText(_columnIndexOfVerdict)
          val _tmpClaimSignature: String
          _tmpClaimSignature = _stmt.getText(_columnIndexOfClaimSignature)
          val _tmpClaimSessionHash: String
          _tmpClaimSessionHash = _stmt.getText(_columnIndexOfClaimSessionHash)
          val _tmpClaimAmount: String
          _tmpClaimAmount = _stmt.getText(_columnIndexOfClaimAmount)
          val _tmpClaimDay: Long
          _tmpClaimDay = _stmt.getLong(_columnIndexOfClaimDay)
          val _tmpClaimDeadline: Long
          _tmpClaimDeadline = _stmt.getLong(_columnIndexOfClaimDeadline)
          val _tmpMockLocation: Boolean
          val _tmp: Int
          _tmp = _stmt.getLong(_columnIndexOfMockLocation).toInt()
          _tmpMockLocation = _tmp != 0
          val _tmpLaps: String
          _tmpLaps = _stmt.getText(_columnIndexOfLaps)
          _result = WalkSessionEntity(_tmpId,_tmpStartedAt,_tmpEndedAt,_tmpSteps,_tmpDurationSec,_tmpDistanceMeters,_tmpCalories,_tmpPointsEarned,_tmpRecordingOwner,_tmpTrack,_tmpBoostBps,_tmpPartySize,_tmpFaction,_tmpCrewId,_tmpUploadState,_tmpUploadAttemptedAt,_tmpUploadAttempts,_tmpUploadError,_tmpVerdict,_tmpClaimSignature,_tmpClaimSessionHash,_tmpClaimAmount,_tmpClaimDay,_tmpClaimDeadline,_tmpMockLocation,_tmpLaps)
        } else {
          _result = null
        }
        _result
      } finally {
        _stmt.close()
      }
    }
  }

  public override suspend fun recentSignedWithLaps(
    owner: String,
    since: Long,
    limit: Int,
  ): List<WalkSessionEntity> {
    val _sql: String = "SELECT * FROM walk_sessions WHERE recordingOwner = ? AND uploadState = 'SIGNED' AND laps != '' AND distanceMeters > 0 AND startedAt >= ? ORDER BY startedAt DESC LIMIT ?"
    return performSuspending(__db, true, false) { _connection ->
      val _stmt: SQLiteStatement = _connection.prepare(_sql)
      try {
        var _argIndex: Int = 1
        _stmt.bindText(_argIndex, owner)
        _argIndex = 2
        _stmt.bindLong(_argIndex, since)
        _argIndex = 3
        _stmt.bindLong(_argIndex, limit.toLong())
        val _columnIndexOfId: Int = getColumnIndexOrThrow(_stmt, "id")
        val _columnIndexOfStartedAt: Int = getColumnIndexOrThrow(_stmt, "startedAt")
        val _columnIndexOfEndedAt: Int = getColumnIndexOrThrow(_stmt, "endedAt")
        val _columnIndexOfSteps: Int = getColumnIndexOrThrow(_stmt, "steps")
        val _columnIndexOfDurationSec: Int = getColumnIndexOrThrow(_stmt, "durationSec")
        val _columnIndexOfDistanceMeters: Int = getColumnIndexOrThrow(_stmt, "distanceMeters")
        val _columnIndexOfCalories: Int = getColumnIndexOrThrow(_stmt, "calories")
        val _columnIndexOfPointsEarned: Int = getColumnIndexOrThrow(_stmt, "pointsEarned")
        val _columnIndexOfRecordingOwner: Int = getColumnIndexOrThrow(_stmt, "recordingOwner")
        val _columnIndexOfTrack: Int = getColumnIndexOrThrow(_stmt, "track")
        val _columnIndexOfBoostBps: Int = getColumnIndexOrThrow(_stmt, "boostBps")
        val _columnIndexOfPartySize: Int = getColumnIndexOrThrow(_stmt, "partySize")
        val _columnIndexOfFaction: Int = getColumnIndexOrThrow(_stmt, "faction")
        val _columnIndexOfCrewId: Int = getColumnIndexOrThrow(_stmt, "crewId")
        val _columnIndexOfUploadState: Int = getColumnIndexOrThrow(_stmt, "uploadState")
        val _columnIndexOfUploadAttemptedAt: Int = getColumnIndexOrThrow(_stmt, "uploadAttemptedAt")
        val _columnIndexOfUploadAttempts: Int = getColumnIndexOrThrow(_stmt, "uploadAttempts")
        val _columnIndexOfUploadError: Int = getColumnIndexOrThrow(_stmt, "uploadError")
        val _columnIndexOfVerdict: Int = getColumnIndexOrThrow(_stmt, "verdict")
        val _columnIndexOfClaimSignature: Int = getColumnIndexOrThrow(_stmt, "claimSignature")
        val _columnIndexOfClaimSessionHash: Int = getColumnIndexOrThrow(_stmt, "claimSessionHash")
        val _columnIndexOfClaimAmount: Int = getColumnIndexOrThrow(_stmt, "claimAmount")
        val _columnIndexOfClaimDay: Int = getColumnIndexOrThrow(_stmt, "claimDay")
        val _columnIndexOfClaimDeadline: Int = getColumnIndexOrThrow(_stmt, "claimDeadline")
        val _columnIndexOfMockLocation: Int = getColumnIndexOrThrow(_stmt, "mockLocation")
        val _columnIndexOfLaps: Int = getColumnIndexOrThrow(_stmt, "laps")
        val _result: MutableList<WalkSessionEntity> = mutableListOf()
        while (_stmt.step()) {
          val _item: WalkSessionEntity
          val _tmpId: Long
          _tmpId = _stmt.getLong(_columnIndexOfId)
          val _tmpStartedAt: Long
          _tmpStartedAt = _stmt.getLong(_columnIndexOfStartedAt)
          val _tmpEndedAt: Long
          _tmpEndedAt = _stmt.getLong(_columnIndexOfEndedAt)
          val _tmpSteps: Int
          _tmpSteps = _stmt.getLong(_columnIndexOfSteps).toInt()
          val _tmpDurationSec: Long
          _tmpDurationSec = _stmt.getLong(_columnIndexOfDurationSec)
          val _tmpDistanceMeters: Double
          _tmpDistanceMeters = _stmt.getDouble(_columnIndexOfDistanceMeters)
          val _tmpCalories: Double
          _tmpCalories = _stmt.getDouble(_columnIndexOfCalories)
          val _tmpPointsEarned: Double
          _tmpPointsEarned = _stmt.getDouble(_columnIndexOfPointsEarned)
          val _tmpRecordingOwner: String
          _tmpRecordingOwner = _stmt.getText(_columnIndexOfRecordingOwner)
          val _tmpTrack: String
          _tmpTrack = _stmt.getText(_columnIndexOfTrack)
          val _tmpBoostBps: Int
          _tmpBoostBps = _stmt.getLong(_columnIndexOfBoostBps).toInt()
          val _tmpPartySize: Int
          _tmpPartySize = _stmt.getLong(_columnIndexOfPartySize).toInt()
          val _tmpFaction: String
          _tmpFaction = _stmt.getText(_columnIndexOfFaction)
          val _tmpCrewId: String
          _tmpCrewId = _stmt.getText(_columnIndexOfCrewId)
          val _tmpUploadState: String
          _tmpUploadState = _stmt.getText(_columnIndexOfUploadState)
          val _tmpUploadAttemptedAt: Long
          _tmpUploadAttemptedAt = _stmt.getLong(_columnIndexOfUploadAttemptedAt)
          val _tmpUploadAttempts: Int
          _tmpUploadAttempts = _stmt.getLong(_columnIndexOfUploadAttempts).toInt()
          val _tmpUploadError: String
          _tmpUploadError = _stmt.getText(_columnIndexOfUploadError)
          val _tmpVerdict: String
          _tmpVerdict = _stmt.getText(_columnIndexOfVerdict)
          val _tmpClaimSignature: String
          _tmpClaimSignature = _stmt.getText(_columnIndexOfClaimSignature)
          val _tmpClaimSessionHash: String
          _tmpClaimSessionHash = _stmt.getText(_columnIndexOfClaimSessionHash)
          val _tmpClaimAmount: String
          _tmpClaimAmount = _stmt.getText(_columnIndexOfClaimAmount)
          val _tmpClaimDay: Long
          _tmpClaimDay = _stmt.getLong(_columnIndexOfClaimDay)
          val _tmpClaimDeadline: Long
          _tmpClaimDeadline = _stmt.getLong(_columnIndexOfClaimDeadline)
          val _tmpMockLocation: Boolean
          val _tmp: Int
          _tmp = _stmt.getLong(_columnIndexOfMockLocation).toInt()
          _tmpMockLocation = _tmp != 0
          val _tmpLaps: String
          _tmpLaps = _stmt.getText(_columnIndexOfLaps)
          _item = WalkSessionEntity(_tmpId,_tmpStartedAt,_tmpEndedAt,_tmpSteps,_tmpDurationSec,_tmpDistanceMeters,_tmpCalories,_tmpPointsEarned,_tmpRecordingOwner,_tmpTrack,_tmpBoostBps,_tmpPartySize,_tmpFaction,_tmpCrewId,_tmpUploadState,_tmpUploadAttemptedAt,_tmpUploadAttempts,_tmpUploadError,_tmpVerdict,_tmpClaimSignature,_tmpClaimSessionHash,_tmpClaimAmount,_tmpClaimDay,_tmpClaimDeadline,_tmpMockLocation,_tmpLaps)
          _result.add(_item)
        }
        _result
      } finally {
        _stmt.close()
      }
    }
  }

  public override suspend fun deleteRecord(owner: String, id: Long): Int {
    val _sql: String = "DELETE FROM walk_sessions WHERE id = ? AND recordingOwner IN (?, 'legacy') AND NOT (recordingOwner = ? AND uploadState IN ('PENDING', 'FAILED') AND (steps > 0 OR (durationSec > 0 AND startedAt >= 1792335600000)))"
    return performSuspending(__db, false, true) { _connection ->
      val _stmt: SQLiteStatement = _connection.prepare(_sql)
      try {
        var _argIndex: Int = 1
        _stmt.bindLong(_argIndex, id)
        _argIndex = 2
        _stmt.bindText(_argIndex, owner)
        _argIndex = 3
        _stmt.bindText(_argIndex, owner)
        _stmt.step()
        getTotalChangedRows(_connection)
      } finally {
        _stmt.close()
      }
    }
  }

  public companion object {
    public fun getRequiredConverters(): List<KClass<*>> = emptyList()
  }
}
