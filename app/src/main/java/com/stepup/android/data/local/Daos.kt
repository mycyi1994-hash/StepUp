package com.stepup.android.data.local

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import androidx.room.Upsert
import kotlinx.coroutines.flow.Flow

@Dao
interface StepDao {

    @Upsert
    suspend fun upsert(day: DailyStepsEntity)

    @Query("SELECT * FROM daily_steps WHERE epochDay >= :fromDay ORDER BY epochDay ASC")
    fun observeSince(fromDay: Long): Flow<List<DailyStepsEntity>>

    @Query("SELECT * FROM daily_steps WHERE epochDay = :day")
    suspend fun byDay(day: Long): DailyStepsEntity?

    @Query("SELECT COALESCE(SUM(steps), 0) FROM daily_steps")
    fun observeTotalSteps(): Flow<Long>

    @Query("SELECT COALESCE(SUM(steps), 0) FROM daily_steps WHERE epochDay >= :fromDay")
    fun observeStepsSince(fromDay: Long): Flow<Long>
}

@Dao
interface WalkSessionDao {

    @Insert
    suspend fun insert(session: WalkSessionEntity)

    @Query("SELECT * FROM walk_sessions ORDER BY startedAt DESC LIMIT :limit")
    fun observeRecent(limit: Int): Flow<List<WalkSessionEntity>>

    @Query("SELECT COUNT(*) FROM walk_sessions")
    fun observeSessionCount(): Flow<Int>

    @Query("SELECT COUNT(*) AS runs, COALESCE(SUM(distanceMeters), 0) AS meters FROM walk_sessions")
    fun observeRunTotals(): Flow<RunTotals>

    @Query("SELECT * FROM walk_sessions WHERE uploadState = 'SIGNED' AND verdict NOT IN ('FLAGGED', 'VOID')")
    fun observeVerifiedSessions(): Flow<List<WalkSessionEntity>>

    @Query("SELECT COALESCE(SUM(durationSec), 0) FROM walk_sessions WHERE startedAt >= :fromMillis")
    fun observeDurationSince(fromMillis: Long): Flow<Long>

    /**
     * 아직 서버에 올리지 못한 세션을 오래된 것부터 준다.
     *
     * 오래된 순인 것은 `RewardDistributor` 의 청구 창이 7일이기 때문이다.
     * 최신 것부터 처리하면 창을 넘긴 세션이 영영 청구되지 않는다.
     *
     * 경로가 없는 세션은 거른다 — 서버가 판정할 수 없어 반드시 거절당한다.
     */
    @Query(
        """
        SELECT * FROM walk_sessions
         WHERE uploadState IN ('PENDING', 'FAILED')
           AND track != ''
           AND steps > 0
           AND recordingOwner = :owner
         ORDER BY startedAt ASC
         LIMIT :limit
        """,
    )
    suspend fun pendingUploads(limit: Int, owner: String = "legacy"): List<WalkSessionEntity>

    @Query("SELECT COUNT(*) FROM walk_sessions WHERE uploadState IN ('PENDING', 'FAILED') AND track != '' AND steps > 0")
    fun observePendingUploadCount(): Flow<Int>

    /**
     * 크루 러닝으로 달린 거리를, 크루별로 묶어서.
     *
     * 크루 순위의 재료다. 크루 이름이 붙지 않은 세션(혼자·번개러닝)은
     * 빠진다 — 크루원이 혼자 달린 거리까지 세면 사람 많은 크루가 자동으로
     * 1등이 된다.
     *
     * @param fromMillis 이 시각 이후에 시작한 러닝만. 전체기간이면 0.
     */
    @Query(
        """
        SELECT crewId AS crewId,
               COALESCE(SUM(distanceMeters), 0) AS meters,
               COUNT(*) AS runs
          FROM walk_sessions
         WHERE crewId != ''
           AND startedAt >= :fromMillis
         GROUP BY crewId
        """,
    )
    suspend fun crewDistances(fromMillis: Long): List<CrewDistance>

    @Update
    suspend fun update(session: WalkSessionEntity)
}

/** [WalkSessionDao.crewDistances] 의 한 줄 — 크루 하나의 누적 거리(m)와 횟수 */
data class RunTotals(val runs: Int, val meters: Double)

data class CrewDistance(
    val crewId: String,
    val meters: Double,
    val runs: Int,
)

data class RewardTotals(val balance: Double, val earned: Double, val spent: Double)

@Dao
interface RewardDao {

    @Query("SELECT COALESCE(SUM(amount), 0.0) AS balance, COALESCE(SUM(CASE WHEN amount > 0 THEN amount ELSE 0 END), 0.0) AS earned, COALESCE(SUM(CASE WHEN amount < 0 THEN -amount ELSE 0 END), 0.0) AS spent FROM rewards")
    fun observeTotals(): Flow<RewardTotals>

    @Insert
    suspend fun insert(reward: RewardEntity)

    @Query("SELECT COALESCE(SUM(amount), 0.0) FROM rewards")
    fun observeBalance(): Flow<Double>

    @Query("SELECT COALESCE(SUM(amount), 0.0) FROM rewards")
    suspend fun balanceNow(): Double

    /**
     * 잔액이 [entry] 의 차감액 이상일 때만 기록한다. 확인과 기록이 한 트랜잭션이라
     * 두 번 누르기처럼 동시에 들어온 차감이 둘 다 "잔액 충분"을 보고 통과하지 못한다.
     */
    @Transaction
    suspend fun spendIfEnough(entry: RewardEntity): Boolean {
        if (balanceNow() < -entry.amount) return false
        insert(entry)
        return true
    }

    @Query("SELECT * FROM rewards ORDER BY timestamp DESC, id DESC LIMIT :limit")
    fun observeLedger(limit: Int): Flow<List<RewardEntity>>

    @Query("SELECT COUNT(*) FROM rewards WHERE type = :type")
    fun observeCountByType(type: String): Flow<Int>

    @Query("SELECT COALESCE(SUM(amount), 0.0) FROM rewards WHERE amount > 0")
    fun observeEarnedTotal(): Flow<Double>

    /**
     * [fromMillis] 이후 **번** SUP — 러닝 · 목표 보너스 · 이벤트 · 파티.
     *
     * 거래소에서 옮겨 온 줄(판매 대금, 에스크로 해제)은 넣지 않는다. 신발을
     * 판 돈이 "오늘 획득"에 섞이면 뛰지 않은 날도 많이 번 것처럼 보인다.
     */
    @Query(
        "SELECT COALESCE(SUM(amount), 0.0) FROM rewards " +
            "WHERE amount > 0 AND timestamp >= :fromMillis " +
            "AND type IN ('EARN_WALK', 'BONUS_GOAL', 'EARN_EVENT', 'EARN_PARTY')",
    )
    fun observeEarnedSince(fromMillis: Long): Flow<Double>

    /** [type] 한 종류가 [fromMillis] 이후 적힌 합 — 오늘 목표 보너스가 나갔는지 같은 것 */
    @Query("SELECT COALESCE(SUM(amount), 0.0) FROM rewards WHERE type = :type AND timestamp >= :fromMillis")
    fun observeSumOfTypeSince(type: String, fromMillis: Long): Flow<Double>
}

@Dao
interface SneakerDao {

    @Insert
    suspend fun insert(sneaker: SneakerEntity): Long

    @Update
    suspend fun update(sneaker: SneakerEntity)

    @Delete
    suspend fun delete(sneaker: SneakerEntity)

    /**
     * rarity는 TEXT라 그냥 정렬하면 사전순(RARE > LEGENDARY > EPIC > COMMON)이 된다.
     * 도감은 등급이 높은 순으로 보여야 하므로 정렬 키를 따로 만든다.
     */
    @Query(
        """
        SELECT * FROM sneakers
        ORDER BY equipped DESC,
            CASE rarity
                WHEN 'LEGENDARY' THEN 3
                WHEN 'EPIC' THEN 2
                WHEN 'RARE' THEN 1
                ELSE 0
            END DESC,
            acquiredAt DESC
        """
    )
    fun observeAll(): Flow<List<SneakerEntity>>

    @Query("SELECT * FROM sneakers WHERE equipped = 1 LIMIT 1")
    fun observeEquipped(): Flow<SneakerEntity?>

    @Query("SELECT * FROM sneakers WHERE equipped = 1 LIMIT 1")
    suspend fun equippedNow(): SneakerEntity?

    @Query("SELECT * FROM sneakers WHERE id = :id")
    suspend fun byId(id: Long): SneakerEntity?

    @Query("UPDATE sneakers SET equipped = 0")
    suspend fun clearEquipped()

    /** One atomic write; a stale/missing target must not clear the current equipment. */
    @Query("UPDATE sneakers SET equipped = CASE WHEN id = :id THEN 1 ELSE 0 END WHERE EXISTS (SELECT 1 FROM sneakers WHERE id = :id)")
    suspend fun equipExclusively(id: Long): Int

    @Query("SELECT COUNT(*) FROM sneakers")
    suspend fun count(): Int

    @Query("SELECT COUNT(*) FROM sneakers")
    fun observeCount(): Flow<Int>

    @Query("SELECT COALESCE(MAX(mintNumber), 0) FROM sneakers")
    suspend fun maxMintNumber(): Int

    // ── 거래소와 맞춰 보기 ──────────────────────────────────────

    @Query("SELECT * FROM sneakers")
    suspend fun allNow(): List<SneakerEntity>

    @Query("SELECT * FROM sneakers WHERE serverId = :serverId LIMIT 1")
    suspend fun byServerId(serverId: Long): SneakerEntity?

    @Query("UPDATE sneakers SET serverId = :serverId WHERE id = :id")
    suspend fun setServerId(id: Long, serverId: Long)

    /** 거래소가 아는 신발들. 팔렸는지 맞춰 보는 데 쓴다. */
    @Query("SELECT * FROM sneakers WHERE serverId <> 0")
    suspend fun registered(): List<SneakerEntity>
}

@Dao
interface NewsDao {

    // 기본값을 두지 않는다 — Room 의 @Query 는 코틀린 기본 인자와 잘 맞지 않는다.
    @Query("SELECT * FROM news_items WHERE kind = :kind ORDER BY publishedAt DESC LIMIT :limit")
    fun observe(kind: String, limit: Int): Flow<List<NewsItemEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(items: List<NewsItemEntity>)

    /** 받아 온 것으로 갈아 끼운다. 지운 글이 계속 남아 있지 않게. */
    @Query("DELETE FROM news_items WHERE kind = :kind")
    suspend fun clear(kind: String)

    @Transaction
    suspend fun replace(kind: String, items: List<NewsItemEntity>) {
        clear(kind)
        upsert(items)
    }
}

@Dao
interface BoostDao {

    @Insert
    suspend fun insert(boost: BoostEntity)

    @Query("SELECT * FROM boosts WHERE expiresAt > :now ORDER BY expiresAt ASC")
    fun observeActive(now: Long): Flow<List<BoostEntity>>

    @Query("SELECT * FROM boosts WHERE type = :type AND expiresAt > :now LIMIT 1")
    suspend fun activeOf(type: String, now: Long): BoostEntity?

    /** [from]~[to] 사이에 한 번이라도 켜져 있던 [type] 부스트 */
    @Query("SELECT * FROM boosts WHERE type = :type AND activatedAt < :to AND expiresAt > :from LIMIT 1")
    suspend fun activeDuring(type: String, from: Long, to: Long): BoostEntity?

    @Query("DELETE FROM boosts WHERE expiresAt <= :now")
    suspend fun purgeExpired(now: Long)
}

@Dao
interface ClaimedEventDao {

    // 이미 받은 기록이 있으면 그대로 둔다(같은 키로 다시 넣다 충돌해 죽지 않게).
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insert(entity: ClaimedEventEntity)

    @Query("SELECT * FROM claimed_events")
    fun observeAll(): Flow<List<ClaimedEventEntity>>

    @Query("SELECT * FROM claimed_events WHERE eventId = :id")
    suspend fun byId(id: String): ClaimedEventEntity?

    @Insert
    suspend fun insertReward(reward: RewardEntity)

    @Insert
    suspend fun insertNotification(notification: NotificationEntity)

    /** The receipt, local credit and notification commit together, or none of them do. */
    @Transaction
    suspend fun recordPaidClaim(claim: ClaimedEventEntity, eventId: String): Boolean {
        require(claim.amount.isFinite() && claim.amount > 0)
        if (byId(claim.eventId) != null) return false
        insert(claim)
        insertReward(RewardEntity(timestamp = claim.claimedAt, type = RewardType.EARN_EVENT,
            amount = claim.amount, description = "이벤트 보상: $eventId"))
        insertNotification(NotificationEntity(timestamp = claim.claimedAt,
            type = NotificationType.EVENT_CLAIMED, argText = eventId, argAmount = claim.amount,
            argExtra = "", read = false, actioned = false))
        return true
    }
}

@Dao
interface CrewDao {

    @Insert
    suspend fun insert(entity: CrewMembershipEntity)

    @Query("SELECT * FROM crew_memberships")
    fun observeAll(): Flow<List<CrewMembershipEntity>>

    @Query("DELETE FROM crew_memberships WHERE crewId = :id")
    suspend fun leave(id: String)

    /** 크루가 서버로 옮겨 가면서 폰 안의 가입 기록은 더 쓰지 않는다 */
    @Query("DELETE FROM crew_memberships")
    suspend fun clear()
}

@Dao
interface CrewInfoDao {

    @Upsert
    suspend fun upsert(entity: CrewEntity)

    @Upsert
    suspend fun upsertAll(entities: List<CrewEntity>)

    @Query("SELECT * FROM crews ORDER BY kmAway ASC")
    fun observeAll(): Flow<List<CrewEntity>>

    @Query("SELECT * FROM crews WHERE id = :id")
    suspend fun byId(id: String): CrewEntity?

    @Query("SELECT COUNT(*) FROM crews")
    suspend fun count(): Int

    /** 크루가 서버로 옮겨 가면서 폰 안의 크루(시드 포함)는 더 쓰지 않는다 */
    @Query("DELETE FROM crews")
    suspend fun clear()
}

@Dao
interface PostDao {

    @Insert
    suspend fun insert(entity: PostEntity): Long

    @Insert
    suspend fun insertAll(entities: List<PostEntity>): List<Long>

    @Update
    suspend fun update(entity: PostEntity)

    @Query("DELETE FROM posts WHERE id = :id")
    suspend fun delete(id: Long)

    @Query("SELECT * FROM posts ORDER BY createdAt DESC")
    fun observeAll(): Flow<List<PostEntity>>

    /** 핫글을 뽑을 때처럼 한 번만 훑으면 되는 경우 */
    @Query("SELECT * FROM posts ORDER BY createdAt DESC")
    suspend fun allOnce(): List<PostEntity>

    @Query("SELECT * FROM posts WHERE id = :id")
    suspend fun byId(id: Long): PostEntity?

    @Query("SELECT COUNT(*) FROM posts")
    suspend fun count(): Int

    @Query("SELECT COUNT(*) FROM posts WHERE mine = 1")
    fun observeMineCount(): Flow<Int>

    /** 게시판이 서버로 옮겨 가면서 폰 안의 글(데모 포함)은 더 쓰지 않는다 */
    @Query("DELETE FROM posts")
    suspend fun clear()
}

@Dao
interface CourseDao {

    @Insert
    suspend fun insert(entity: CourseEntity): Long

    @Insert
    suspend fun insertAll(entities: List<CourseEntity>)

    @Update
    suspend fun update(entity: CourseEntity)

    /** 내 코스나 게시판에서 받아 둔 코스만 지운다. 기본 공원 코스는 남는다. */
    @Query("DELETE FROM courses WHERE id = :id AND (mine = 1 OR shared = 0)")
    suspend fun deleteLocal(id: Long)

    /** 같은 길의 코스 — 게시판 코스를 두 번 받아 두지 않게 찾는다. */
    @Query("SELECT * FROM courses WHERE track = :track ORDER BY mine DESC LIMIT 1")
    suspend fun byTrack(track: String): CourseEntity?

    /**
     * 데모 코스만 지운다. 내가 만든 코스는 남는다.
     *
     * 데모 코스의 좌표가 바뀔 때 갈아 끼우는 데 쓴다 — 예전 것은 지도 위에서
     * 한강을 가로질렀고, 그대로 두면 이미 설치한 사람은 계속 그 선을 본다.
     */
    @Query("DELETE FROM courses WHERE mine = 0 AND shared = 1")
    suspend fun deleteSeeded()

    @Query("SELECT * FROM courses ORDER BY mine DESC, createdAt DESC")
    fun observeAll(): Flow<List<CourseEntity>>

    @Query("SELECT * FROM courses WHERE id = :id")
    suspend fun byId(id: Long): CourseEntity?

    @Query("SELECT COUNT(*) FROM courses")
    suspend fun count(): Int
}

@Dao
interface CommentDao {

    @Insert
    suspend fun insert(entity: CommentEntity): Long

    @Insert
    suspend fun insertAll(entities: List<CommentEntity>)

    @Query("DELETE FROM comments WHERE id = :id OR parentId = :id")
    suspend fun deleteWithReplies(id: Long)

    @Query("DELETE FROM comments WHERE postId = :postId")
    suspend fun deleteForPost(postId: Long)

    @Query("SELECT * FROM comments WHERE postId = :postId ORDER BY createdAt ASC")
    fun observeForPost(postId: Long): Flow<List<CommentEntity>>

    @Query("SELECT * FROM comments WHERE id = :id")
    suspend fun byId(id: Long): CommentEntity?

    @Query("SELECT COUNT(*) FROM comments WHERE postId = :postId")
    suspend fun countForPost(postId: Long): Int

    @Query("SELECT COUNT(*) FROM comments WHERE parentId = :commentId")
    suspend fun replyCount(commentId: Long): Int

    @Query(
        "SELECT * FROM comments WHERE postId = :postId AND parentId = 0 AND mine = 1 " +
            "ORDER BY createdAt DESC LIMIT 1"
    )
    suspend fun latestMineTopLevel(postId: Long): CommentEntity?

    @Query("SELECT COUNT(*) FROM comments")
    suspend fun count(): Int

    /** 게시판이 서버로 옮겨 가면서 폰 안의 댓글(데모 포함)은 더 쓰지 않는다 */
    @Query("DELETE FROM comments")
    suspend fun clear()
}

@Dao
interface NotificationDao {

    @Insert
    suspend fun insert(entity: NotificationEntity)

    @Query("SELECT * FROM notifications ORDER BY timestamp DESC LIMIT :limit")
    fun observeAll(limit: Int): Flow<List<NotificationEntity>>

    @Query("SELECT COUNT(*) FROM notifications WHERE read = 0")
    fun observeUnreadCount(): Flow<Int>

    @Query("UPDATE notifications SET read = 1")
    suspend fun markAllRead()

    @Query("UPDATE notifications SET actioned = 1, read = 1 WHERE id = :id")
    suspend fun markActioned(id: Long)

    @Query("DELETE FROM notifications WHERE id = :id")
    suspend fun delete(id: Long)

    @Query("SELECT COUNT(*) FROM notifications")
    suspend fun count(): Int

    @Query("DELETE FROM notifications")
    suspend fun clear()

    /** 예전 첫 실행 때 넣어 둔, 이제는 없는 크루로 가는 초대 알림 */
    @Query(
        "DELETE FROM notifications WHERE type IN ('CREW_INVITE', 'PARTY_INVITE') " +
            "AND argExtra IN (:crewIds)",
    )
    suspend fun deleteInvitesTo(crewIds: List<String>)

    @Query("DELETE FROM notifications WHERE type = :type")
    suspend fun deleteByType(type: String)

    /**
     * "모두 읽음" 청소 — 아직 처리하지 않은 액션형 알림(초대·미수령 보상)은 남긴다.
     * 나머지는 전부 지운다.
     */
    @Query(
        """
        DELETE FROM notifications
        WHERE actioned = 1
           OR type NOT IN ('CREW_INVITE', 'PARTY_INVITE', 'EVENT_REWARD')
        """
    )
    suspend fun clearExceptPendingActions()
}
