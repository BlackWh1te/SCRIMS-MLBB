package com.mlbb.scrim.data.repository

import com.mlbb.scrim.data.model.LeaderboardEntry
import com.mlbb.scrim.data.model.RankTier
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow

class LeaderboardRepository {

    private val entries = mutableListOf(
        LeaderboardEntry(
            rank = 1,
            playerId = "p1",
            username = "ShadowSlayer",
            teamName = "Shadow Wolves",
            xp = 18500,
            wins = 45,
            losses = 12,
            totalMatches = 57,
            currentTier = RankTier.GRANDMASTER
        ),
        LeaderboardEntry(
            rank = 2,
            playerId = "p2",
            username = "PhoenixRise",
            teamName = "Phoenix Rising",
            xp = 16200,
            wins = 38,
            losses = 15,
            totalMatches = 53,
            currentTier = RankTier.MASTER
        ),
        LeaderboardEntry(
            rank = 3,
            playerId = "p3",
            username = "DiamondKing",
            teamName = "Elite Squad",
            xp = 14100,
            wins = 32,
            losses = 18,
            totalMatches = 50,
            currentTier = RankTier.MASTER
        ),
        LeaderboardEntry(
            rank = 4,
            playerId = "p4",
            username = "PlatinumEdge",
            teamName = "Cyber Legion",
            xp = 11500,
            wins = 28,
            losses = 20,
            totalMatches = 48,
            currentTier = RankTier.DIAMOND
        ),
        LeaderboardEntry(
            rank = 5,
            playerId = "p5",
            username = "GoldRush",
            teamName = "Nova Gaming",
            xp = 8900,
            wins = 22,
            losses = 25,
            totalMatches = 47,
            currentTier = RankTier.DIAMOND
        ),
        LeaderboardEntry(
            rank = 6,
            playerId = "p6",
            username = "SilverWing",
            teamName = "Sky Blazers",
            xp = 6200,
            wins = 18,
            losses = 22,
            totalMatches = 40,
            currentTier = RankTier.PLATINUM
        ),
        LeaderboardEntry(
            rank = 7,
            playerId = "p7",
            username = "IronFist",
            teamName = "Steel Guardians",
            xp = 4800,
            wins = 15,
            losses = 20,
            totalMatches = 35,
            currentTier = RankTier.GOLD
        ),
        LeaderboardEntry(
            rank = 8,
            playerId = "p8",
            username = "BronzeBeast",
            teamName = "Underdogs",
            xp = 3100,
            wins = 12,
            losses = 18,
            totalMatches = 30,
            currentTier = RankTier.GOLD
        ),
        LeaderboardEntry(
            rank = 9,
            playerId = "p9",
            username = "RookieAce",
            teamName = "New Bloods",
            xp = 1500,
            wins = 8,
            losses = 12,
            totalMatches = 20,
            currentTier = RankTier.SILVER
        ),
        LeaderboardEntry(
            rank = 10,
            playerId = "p10",
            username = "StarterX",
            teamName = "Beginners",
            xp = 600,
            wins = 4,
            losses = 8,
            totalMatches = 12,
            currentTier = RankTier.BRONZE
        )
    )

    suspend fun getLeaderboard(): Flow<Result<List<LeaderboardEntry>>> = flow {
        kotlinx.coroutines.delay(400)
        emit(Result.success(entries.sortedBy { it.rank }))
    }

    suspend fun getLeaderboardForTier(tier: RankTier): Flow<Result<List<LeaderboardEntry>>> = flow {
        kotlinx.coroutines.delay(300)
        val filtered = entries.filter { it.currentTier == tier }
            .sortedBy { it.rank }
        emit(Result.success(filtered))
    }

    fun addOrUpdateEntry(entry: LeaderboardEntry) {
        val existingIndex = entries.indexOfFirst { it.playerId == entry.playerId }
        if (existingIndex != -1) {
            entries[existingIndex] = entry
        } else {
            entries.add(entry)
        }
        // Recalculate ranks based on XP
        entries.sortByDescending { it.xp }
        entries.forEachIndexed { index, e ->
            val idx = entries.indexOfFirst { it.playerId == e.playerId }
            entries[idx] = e.copy(rank = index + 1)
        }
    }
}
