package com.mlbb.scrim.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(
    entities = [
        ProfileEntity::class,
        ConversationEntity::class,
        MessageEntity::class,
        PendingMessageEntity::class,
        // Cache entities (Phase 1)
        CacheMetadataEntity::class,
        TeamEntity::class,
        ScrimEntity::class,
        LeaderboardEntity::class,
        LfgPostEntity::class,
        NotificationEntity::class
    ],
    version = 11,
    exportSchema = false
)
abstract class MLBBScrimDatabase : RoomDatabase() {
    // Existing DAOs
    abstract fun profileDao(): ProfileDao
    abstract fun conversationDao(): ConversationDao
    abstract fun messageDao(): MessageDao
    abstract fun pendingMessageDao(): PendingMessageDao

    // Cache DAOs (Phase 1)
    abstract fun cacheMetadataDao(): CacheMetadataDao
    abstract fun teamDao(): TeamDao
    abstract fun scrimDao(): ScrimDao
    abstract fun leaderboardDao(): LeaderboardDao
    abstract fun lfgPostDao(): LfgPostDao
    abstract fun notificationDao(): NotificationDao

    companion object {
        @Volatile
        private var INSTANCE: MLBBScrimDatabase? = null

        fun getDatabase(context: Context): MLBBScrimDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    MLBBScrimDatabase::class.java,
                    "mlbb_scrim_database"
                )
                .addMigrations(MIGRATION_5_6, MIGRATION_6_7, MIGRATION_7_8, MIGRATION_8_9, MIGRATION_9_10, MIGRATION_10_11)
                .fallbackToDestructiveMigration()
                .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
