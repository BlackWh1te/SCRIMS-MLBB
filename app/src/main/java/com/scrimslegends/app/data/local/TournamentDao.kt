package com.scrimslegends.app.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query

@Dao
interface TournamentDao {
    @Query("SELECT * FROM cached_tournaments ORDER BY createdAt DESC")
    suspend fun getAll(): List<TournamentEntity>

    @Query("SELECT * FROM cached_tournaments WHERE id = :id LIMIT 1")
    suspend fun getById(id: String): TournamentEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(tournaments: List<TournamentEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(tournament: TournamentEntity)

    @Query("DELETE FROM cached_tournaments")
    suspend fun deleteAll()
}
