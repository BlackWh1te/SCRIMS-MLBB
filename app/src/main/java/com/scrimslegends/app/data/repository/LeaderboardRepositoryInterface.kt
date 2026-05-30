package com.scrimslegends.app.data.repository

import com.scrimslegends.app.data.model.LeaderboardEntry
import com.scrimslegends.app.data.model.RankTier
import kotlinx.coroutines.flow.Flow

interface LeaderboardRepositoryInterface {
    suspend fun getLeaderboard(): Flow<Result<List<LeaderboardEntry>>>
    suspend fun getLeaderboardForTier(tier: RankTier): Flow<Result<List<LeaderboardEntry>>>
    fun subscribeToLeaderboard(): Flow<LeaderboardEntry>
}
