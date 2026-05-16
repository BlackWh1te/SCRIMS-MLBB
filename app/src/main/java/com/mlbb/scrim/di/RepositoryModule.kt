package com.mlbb.scrim.di

import com.mlbb.scrim.data.cache.ProfileCacheRepository
import com.mlbb.scrim.data.cache.UnifiedCacheManager
import com.mlbb.scrim.data.local.*
import com.mlbb.scrim.data.repository.*
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object RepositoryModule {

    @Provides
    @Singleton
    fun provideMessageRepository(
        conversationDao: ConversationDao,
        messageDao: MessageDao
    ): MessageRepositoryInterface {
        return SupabaseMessageRepository(conversationDao, messageDao)
    }

    @Provides
    @Singleton
    fun provideScrimRepository(
        cacheManager: UnifiedCacheManager,
        scrimDao: ScrimDao
    ): ScrimRepositoryInterface {
        return SupabaseScrimRepository(cacheManager, scrimDao)
    }

    @Provides
    @Singleton
    fun provideTeamRepository(
        cacheManager: UnifiedCacheManager,
        teamDao: TeamDao,
        profileCache: ProfileCacheRepository
    ): TeamRepositoryInterface {
        return SupabaseTeamRepository(cacheManager, teamDao, profileCache)
    }

    @Provides
    @Singleton
    fun provideLeaderboardRepository(
        cacheManager: UnifiedCacheManager,
        leaderboardDao: LeaderboardDao
    ): LeaderboardRepositoryInterface {
        return SupabaseLeaderboardRepository(cacheManager, leaderboardDao)
    }

    @Provides
    @Singleton
    fun provideLfgRepository(
        cacheManager: UnifiedCacheManager,
        lfgPostDao: LfgPostDao
    ): LfgRepositoryInterface {
        return SupabaseLfgRepository(cacheManager, lfgPostDao)
    }

    @Provides
    @Singleton
    fun provideNotificationRepository(
        cacheManager: UnifiedCacheManager,
        notificationDao: NotificationDao
    ): SupabaseNotificationRepository {
        return SupabaseNotificationRepository(cacheManager, notificationDao)
    }

    @Provides
    @Singleton
    fun provideMatchResultRepository(
        profileCache: ProfileCacheRepository
    ): MatchResultRepositoryInterface {
        return SupabaseMatchResultRepository(profileCache)
    }

    @Provides
    @Singleton
    fun provideAuthRepository(
        @dagger.hilt.android.qualifiers.ApplicationContext context: android.content.Context,
        cacheManager: UnifiedCacheManager
    ): AuthRepositoryInterface {
        return SupabaseAuthRepository(context, cacheManager)
    }
}
