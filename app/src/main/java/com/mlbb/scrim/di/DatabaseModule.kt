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
    fun provideDatabase(@ApplicationContext context: Context): MLBBScrimDatabase {
        return MLBBScrimDatabase.getDatabase(context)
    }

    // ─── Existing DAOs ───

    @Provides
    fun provideProfileDao(database: MLBBScrimDatabase): ProfileDao {
        return database.profileDao()
    }

    @Provides
    fun provideConversationDao(database: MLBBScrimDatabase): ConversationDao {
        return database.conversationDao()
    }

    @Provides
    fun provideMessageDao(database: MLBBScrimDatabase): MessageDao {
        return database.messageDao()
    }

    // ─── Cache DAOs ───

    @Provides
    fun provideCacheMetadataDao(database: MLBBScrimDatabase): CacheMetadataDao {
        return database.cacheMetadataDao()
    }

    @Provides
    fun provideTeamDao(database: MLBBScrimDatabase): TeamDao {
        return database.teamDao()
    }

    @Provides
    fun provideScrimDao(database: MLBBScrimDatabase): ScrimDao {
        return database.scrimDao()
    }

    @Provides
    fun provideLeaderboardDao(database: MLBBScrimDatabase): LeaderboardDao {
        return database.leaderboardDao()
    }

    @Provides
    fun provideLfgPostDao(database: MLBBScrimDatabase): LfgPostDao {
        return database.lfgPostDao()
    }

    @Provides
    fun provideNotificationDao(database: MLBBScrimDatabase): NotificationDao {
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
