package com.mlbb.scrim.di

import com.mlbb.scrim.data.cache.ProfileCacheRepository
import com.mlbb.scrim.data.cache.UnifiedCacheManager
import com.mlbb.scrim.data.local.*
import com.mlbb.scrim.data.repository.*
import com.mlbb.scrim.data.service.SupabaseRealtimeClient
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
    fun provideRealtimeClient(): SupabaseRealtimeClient {
        return SupabaseRealtimeClient()
    }

    @Provides
    @Singleton
    fun provideMessageRepository(
        conversationDao: ConversationDao,
        messageDao: MessageDao,
        realtimeClient: SupabaseRealtimeClient
    ): MessageRepositoryInterface {
        return SupabaseMessageRepository(conversationDao, messageDao, realtimeClient)
    }

    @Provides
    @Singleton
    fun provideScrimRepository(
        cacheManager: UnifiedCacheManager,
        scrimDao: ScrimDao,
        realtimeClient: SupabaseRealtimeClient
    ): ScrimRepositoryInterface {
        return SupabaseScrimRepository(cacheManager, scrimDao, realtimeClient)
    }

    @Provides
    @Singleton
    fun provideTeamRepository(
        cacheManager: UnifiedCacheManager,
        teamDao: TeamDao,
        profileCache: ProfileCacheRepository,
        realtimeClient: SupabaseRealtimeClient
    ): TeamRepositoryInterface {
        return SupabaseTeamRepository(cacheManager, teamDao, profileCache, realtimeClient)
    }

    @Provides
    @Singleton
    fun provideLeaderboardRepository(
        cacheManager: UnifiedCacheManager,
        leaderboardDao: LeaderboardDao,
        realtimeClient: SupabaseRealtimeClient
    ): LeaderboardRepositoryInterface {
        return SupabaseLeaderboardRepository(cacheManager, leaderboardDao, realtimeClient)
    }

    @Provides
    @Singleton
    fun provideLfgRepository(
        cacheManager: UnifiedCacheManager,
        lfgPostDao: LfgPostDao,
        realtimeClient: SupabaseRealtimeClient
    ): LfgRepositoryInterface {
        return SupabaseLfgRepository(cacheManager, lfgPostDao, realtimeClient)
    }

    @Provides
    @Singleton
    fun provideNotificationRepository(
        cacheManager: UnifiedCacheManager,
        notificationDao: NotificationDao,
        realtimeClient: SupabaseRealtimeClient
    ): SupabaseNotificationRepository {
        return SupabaseNotificationRepository(cacheManager, notificationDao, realtimeClient)
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
