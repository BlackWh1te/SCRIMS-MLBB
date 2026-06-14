package com.scrimslegends.app.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Cached notification for offline access and reduced API calls.
 */
@Entity(tableName = "cached_notifications")
data class NotificationEntity(
    @PrimaryKey val id: String,
    val userId: String,
    val type: String,
    val title: String,
    val message: String,
    val actionId: String? = null,
    val isRead: Boolean = false,
    val timestamp: Long = System.currentTimeMillis(),
    val lastUpdated: Long = System.currentTimeMillis()
)
