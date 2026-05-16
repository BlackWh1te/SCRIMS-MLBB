package com.mlbb.scrim.data.local

import androidx.room.*

@Dao
interface ScrimDao {
    @Query("SELECT * FROM cached_scrims ORDER BY lastUpdated DESC")
    suspend fun getAll(): List<ScrimEntity>

    @Query("SELECT * FROM cached_scrims WHERE id = :scrimId")
    suspend fun getById(scrimId: String): ScrimEntity?

    @Query("SELECT * FROM cached_scrims WHERE teamId = :teamId OR opponentTeamId = :teamId ORDER BY lastUpdated DESC")
    suspend fun getByTeam(teamId: String): List<ScrimEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(scrims: List<ScrimEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(scrim: ScrimEntity)

    @Query("DELETE FROM cached_scrims")
    suspend fun deleteAll()

    @Query("DELETE FROM cached_scrims WHERE id = :scrimId")
    suspend fun deleteById(scrimId: String)
}
