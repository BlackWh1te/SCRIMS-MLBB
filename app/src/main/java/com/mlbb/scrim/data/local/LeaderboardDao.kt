package com.mlbb.scrim.data.local

import androidx.room.*

@Dao
interface LeaderboardDao {
    @Query("SELECT * FROM cached_leaderboard ORDER BY odinalRank ASC")
    suspend fun getAll(): List<LeaderboardEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(entries: List<LeaderboardEntity>)

    @Query("DELETE FROM cached_leaderboard")
    suspend fun deleteAll()
}
