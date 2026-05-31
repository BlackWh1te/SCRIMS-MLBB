package com.scrimslegends.app.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.scrimslegends.app.BuildConfig

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
    version = 16,
    // exportSchema = true so Room generates schema JSON files under app/schemas/.
    // Commit these files to version control to validate that all migration paths are
    // correct in CI and to prevent accidental data loss from missing migrations.
    exportSchema = true
)
abstract class ScrimsLegendsDatabase : RoomDatabase() {
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
        private var INSTANCE: ScrimsLegendsDatabase? = null

        fun getDatabase(context: Context): ScrimsLegendsDatabase {
            return INSTANCE ?: synchronized(this) {
                val builder = Room.databaseBuilder(
                    context.applicationContext,
                    ScrimsLegendsDatabase::class.java,
                    "scrims_legends_database"
                )
                .addMigrations(MIGRATION_5_6, MIGRATION_6_7, MIGRATION_7_8, MIGRATION_8_9, MIGRATION_9_10, MIGRATION_10_11, MIGRATION_11_12, MIGRATION_12_13, MIGRATION_13_14, MIGRATION_14_15, MIGRATION_15_16)

                // fallbackToDestructiveMigration() silently wipes the entire local
                // database (messages, conversations, notifications, cached profiles)
                // whenever a migration path is missing — unacceptable in production.
                // Allow it only on debug builds where data loss is acceptable.
                if (BuildConfig.DEBUG) {
                    builder.fallbackToDestructiveMigration()
                }

                val instance = builder.build()
                INSTANCE = instance
                instance
            }
        }
    }
}
