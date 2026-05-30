package com.scrimslegends.app.data.local

import androidx.room.*

@Dao
interface LfgPostDao {
    @Query("SELECT * FROM cached_lfg_posts ORDER BY createdAt DESC")
    suspend fun getAll(): List<LfgPostEntity>

    @Query("SELECT * FROM cached_lfg_posts WHERE playerId = :playerId ORDER BY createdAt DESC")
    suspend fun getByPlayer(playerId: String): List<LfgPostEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(posts: List<LfgPostEntity>)

    @Query("DELETE FROM cached_lfg_posts")
    suspend fun deleteAll()

    @Query("DELETE FROM cached_lfg_posts WHERE id = :postId")
    suspend fun deleteById(postId: String)
}
