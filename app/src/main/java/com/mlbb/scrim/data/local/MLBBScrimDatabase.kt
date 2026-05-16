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
        // Cache entities (Phase 1)
        CacheMetadataEntity::class,
        TeamEntity::class,
        ScrimEntity::class,
        LeaderboardEntity::class,
        LfgPostEntity::class,
        NotificationEntity::class
    ],
    version = 3,
    exportSchema = false
)
abstract class MLBBScrimDatabase : RoomDatabase() {
    // Existing DAOs
    abstract fun profileDao(): ProfileDao
    abstract fun conversationDao(): ConversationDao
    abstract fun messageDao(): MessageDao

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
                .fallbackToDestructiveMigration()
                .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
