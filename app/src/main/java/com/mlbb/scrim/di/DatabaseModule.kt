package com.mlbb.scrim.di

import android.content.Context
import com.mlbb.scrim.data.cache.ProfileCacheRepository
import com.mlbb.scrim.data.cache.UnifiedCacheManager
import com.mlbb.scrim.data.local.*
import com.mlbb.scrim.data.service.SupabaseService
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    @Provides
    @Singleton
    fun provideDatabase(@ApplicationContext context: Context): ScrimsLegendsDatabase {
        return ScrimsLegendsDatabase.getDatabase(context)
    }

    // ─── Existing DAOs ───

    @Provides
    fun provideProfileDao(database: ScrimsLegendsDatabase): ProfileDao {
        return database.profileDao()
    }

    @Provides
    fun provideConversationDao(database: ScrimsLegendsDatabase): ConversationDao {
        return database.conversationDao()
    }

    @Provides
    fun provideMessageDao(database: ScrimsLegendsDatabase): MessageDao {
        return database.messageDao()
    }

    @Provides
    fun providePendingMessageDao(database: ScrimsLegendsDatabase): PendingMessageDao {
        return database.pendingMessageDao()
    }

    // ─── Cache DAOs ───

    @Provides
    fun provideCacheMetadataDao(database: ScrimsLegendsDatabase): CacheMetadataDao {
        return database.cacheMetadataDao()
    }

    @Provides
    fun provideTeamDao(database: ScrimsLegendsDatabase): TeamDao {
        return database.teamDao()
    }

    @Provides
    fun provideScrimDao(database: ScrimsLegendsDatabase): ScrimDao {
        return database.scrimDao()
    }

    @Provides
    fun provideLeaderboardDao(database: ScrimsLegendsDatabase): LeaderboardDao {
        return database.leaderboardDao()
    }

    @Provides
    fun provideLfgPostDao(database: ScrimsLegendsDatabase): LfgPostDao {
        return database.lfgPostDao()
    }

    @Provides
    fun provideNotificationDao(database: ScrimsLegendsDatabase): NotificationDao {
        return database.notificationDao()
    }

    // ─── Cache Infrastructure ───

    @Provides
    @Singleton
    fun provideUnifiedCacheManager(cacheMetadataDao: CacheMetadataDao): UnifiedCacheManager {
        return UnifiedCacheManager(cacheMetadataDao)
    }

    @Provides
    @Singleton
    fun provideProfileCacheRepository(profileDao: ProfileDao): ProfileCacheRepository {
        return ProfileCacheRepository(SupabaseService.api, profileDao)
    }
}
