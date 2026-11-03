package com.stepup.android.data.local

import androidx.room.ConstructedBy
import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.RoomDatabaseConstructor

@Database(
    entities = [
        DailyStepsEntity::class,
        WalkSessionEntity::class,
        RewardEntity::class,
        SneakerEntity::class,
        BoostEntity::class,
        EnergyPurchase::class,
        RunSettlement::class,
        ClaimedEventEntity::class,
        CrewMembershipEntity::class,
        CrewEntity::class,
        PostEntity::class,
        CommentEntity::class,
        CourseEntity::class,
        NotificationEntity::class,
        NewsItemEntity::class,
    ],
    version = 19,
    exportSchema = true,
)
@ConstructedBy(AppDatabaseConstructor::class)
abstract class AppDatabase : RoomDatabase() {
    abstract fun stepDao(): StepDao
    abstract fun walkSessionDao(): WalkSessionDao
    abstract fun rewardDao(): RewardDao
    abstract fun sneakerDao(): SneakerDao
    abstract fun boostDao(): BoostDao
    abstract fun energyPurchaseDao(): EnergyPurchaseDao
    abstract fun runSettlementDao(): RunSettlementDao
    abstract fun claimedEventDao(): ClaimedEventDao
    abstract fun crewDao(): CrewDao
    abstract fun crewInfoDao(): CrewInfoDao
    abstract fun postDao(): PostDao
    abstract fun commentDao(): CommentDao
    abstract fun courseDao(): CourseDao
    abstract fun notificationDao(): NotificationDao

    abstract fun newsDao(): NewsDao
}

/** Room KMP 가 만드는 생성자(iOS) — 안드로이드는 예전처럼 Room.databaseBuilder(context, AppDatabase::class.java, …) */
@Suppress("KotlinNoActualForExpect")
expect object AppDatabaseConstructor : RoomDatabaseConstructor<AppDatabase> {
    override fun initialize(): AppDatabase
}
