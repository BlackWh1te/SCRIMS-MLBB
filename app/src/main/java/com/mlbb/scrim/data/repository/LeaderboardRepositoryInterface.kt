package com.mlbb.scrim.data.repository

import com.mlbb.scrim.data.model.LeaderboardEntry
import com.mlbb.scrim.data.model.RankTier
import kotlinx.coroutines.flow.Flow

interface LeaderboardRepositoryInterface {
    suspend fun getLeaderboard(): Flow<Result<List<LeaderboardEntry>>>
    suspend fun getLeaderboardForTier(tier: RankTier): Flow<Result<List<LeaderboardEntry>>>
}
