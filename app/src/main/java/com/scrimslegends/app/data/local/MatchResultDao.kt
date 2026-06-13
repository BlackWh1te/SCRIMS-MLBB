package com.scrimslegends.app.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query

@Dao
interface MatchResultDao {
    @Query("SELECT * FROM cached_match_results ORDER BY createdAt DESC")
    suspend fun getAll(): List<MatchResultEntity>

    @Query("SELECT * FROM cached_match_results WHERE id = :id LIMIT 1")
    suspend fun getById(id: String): MatchResultEntity?

    @Query("SELECT * FROM cached_match_results WHERE teamAId = :teamId OR teamBId = :teamId ORDER BY createdAt DESC")
    suspend fun getByTeamId(teamId: String): List<MatchResultEntity>

    @Query("SELECT * FROM cached_match_results WHERE scrimId = :scrimId LIMIT 1")
    suspend fun getByScrimId(scrimId: String): MatchResultEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(matchResults: List<MatchResultEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(matchResult: MatchResultEntity)

    @Query("DELETE FROM cached_match_results")
    suspend fun deleteAll()
}
