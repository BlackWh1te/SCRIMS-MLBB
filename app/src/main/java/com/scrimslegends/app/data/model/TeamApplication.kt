package com.scrimslegends.app.data.model

data class TeamApplication(
    val id: String = "",
    val teamId: String = "",
    val teamName: String = "",
    val applicantUserId: String = "",
    val applicantName: String = "",
    val applicantAvatarUrl: String? = null,
    val status: TeamApplicationStatus = TeamApplicationStatus.PENDING,
    val message: String? = null,
    val createdAt: Long = System.currentTimeMillis(),
    val respondedAt: Long? = null
)

enum class TeamApplicationStatus {
    PENDING,
    ACCEPTED,
    DECLINED
}
