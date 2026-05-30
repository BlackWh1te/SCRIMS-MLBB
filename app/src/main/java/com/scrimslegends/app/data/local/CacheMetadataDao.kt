package com.scrimslegends.app.data.local

import androidx.room.*

@Dao
interface CacheMetadataDao {
    @Query("SELECT * FROM cache_metadata WHERE cacheKey = :key")
    suspend fun get(key: String): CacheMetadataEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun set(entity: CacheMetadataEntity)

    @Query("DELETE FROM cache_metadata WHERE cacheKey = :key")
    suspend fun delete(key: String)

    @Query("DELETE FROM cache_metadata WHERE cacheKey LIKE :prefix || '%'")
    suspend fun deleteByPrefix(prefix: String)

    @Query("DELETE FROM cache_metadata")
    suspend fun clearAll()
}
