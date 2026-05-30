package com.scrimslegends.app.data.model

/**
 * A rating + optional feedback left by one team for another after a scrim.
 */
data class TeamRating(
    val id: String = "",
    val teamId: String = "",
    val raterTeamId: String = "",
    val raterTeamName: String = "",
    val raterUserName: String = "",
    val rating: Int = 0,           // 1–5 stars
    val feedback: String = "",
    val createdAt: Long = System.currentTimeMillis()
)
