package com.scrimslegends.app.di

import com.scrimslegends.app.data.cache.ProfileCacheRepository
import com.scrimslegends.app.data.cache.UnifiedCacheManager
import com.scrimslegends.app.data.local.*
import com.scrimslegends.app.data.repository.*
import com.scrimslegends.app.data.service.SupabaseRealtimeClient
import com.scrimslegends.app.data.service.SupabaseService
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
        val client = SupabaseRealtimeClient()
        // Wire into SupabaseService so SupabaseAuthenticator can trigger
        // a channel re-join after a silent JWT refresh.
        SupabaseService.realtimeClient = client
        return client
    }

    @Provides
    @Singleton
    fun provideMessageRepository(
        conversationDao: ConversationDao,
        messageDao: MessageDao,
        pendingMessageDao: PendingMessageDao,
        realtimeClient: SupabaseRealtimeClient,
        cacheManager: UnifiedCacheManager,
        database: ScrimsLegendsDatabase
    ): MessageRepositoryInterface {
        return SupabaseMessageRepository(conversationDao, messageDao, pendingMessageDao, realtimeClient, cacheManager, database)
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
        cacheManager: UnifiedCacheManager,
        matchResultDao: MatchResultDao,
        profileCache: ProfileCacheRepository
    ): MatchResultRepositoryInterface {
        return SupabaseMatchResultRepository(cacheManager, matchResultDao, profileCache)
    }

    @Provides
    @Singleton
    fun provideAuthRepository(
        @dagger.hilt.android.qualifiers.ApplicationContext context: android.content.Context,
        cacheManager: UnifiedCacheManager,
        realtimeManager: com.scrimslegends.app.data.service.RealtimeManager
    ): AuthRepositoryInterface {
        return SupabaseAuthRepository(context, cacheManager, realtimeManager)
    }

    @Provides
    @Singleton
    fun provideTournamentRepository(
        cacheManager: UnifiedCacheManager,
        tournamentDao: TournamentDao
    ): TournamentRepositoryInterface {
        return SupabaseTournamentRepository(cacheManager, tournamentDao)
    }

    @Provides
    @Singleton
    fun provideFcmTokenRepository(): FcmTokenRepositoryInterface {
        return FcmTokenRepository()
    }

    @Provides
    @Singleton
    fun provideMessageOutboxRepository(
        dao: MessageOutboxDao
    ): MessageOutboxRepositoryInterface {
        return MessageOutboxRepository(dao)
    }
}
