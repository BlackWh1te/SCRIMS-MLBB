package com.scrimslegends.app.data.local

import androidx.room.*

@Dao
interface NotificationDao {
    @Query("SELECT * FROM cached_notifications WHERE userId = :userId ORDER BY timestamp DESC")
    suspend fun getForUser(userId: String): List<NotificationEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(notifications: List<NotificationEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(notification: NotificationEntity)

    @Query("UPDATE cached_notifications SET isRead = 1 WHERE id = :notificationId")
    suspend fun markAsRead(notificationId: String)

    @Query("UPDATE cached_notifications SET isRead = 1 WHERE userId = :userId")
    suspend fun markAllAsRead(userId: String)

    @Query("DELETE FROM cached_notifications WHERE id = :notificationId")
    suspend fun deleteById(notificationId: String)

    @Query("DELETE FROM cached_notifications WHERE userId = :userId")
    suspend fun deleteAllForUser(userId: String)
}
