package com.mlbb.scrim.data.local

import androidx.room.*

@Dao
interface TeamDao {
    @Query("SELECT * FROM cached_teams")
    suspend fun getAll(): List<TeamEntity>

    @Query("SELECT * FROM cached_teams WHERE id = :teamId")
    suspend fun getById(teamId: String): TeamEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(teams: List<TeamEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(team: TeamEntity)

    @Query("DELETE FROM cached_teams")
    suspend fun deleteAll()

    @Query("DELETE FROM cached_teams WHERE id = :teamId")
    suspend fun deleteById(teamId: String)
}
