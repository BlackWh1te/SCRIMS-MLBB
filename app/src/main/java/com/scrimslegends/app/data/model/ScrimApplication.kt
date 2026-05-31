package com.scrimslegends.app.data.model

data class ScrimApplication(
    val id: String = "",
    val scrimId: String = "",
    val applicantTeamId: String = "",
    val applicantTeamName: String = "",
    val applicantTeamLeader: String = "",
    val applicantTeamLeaderName: String = "",
    val applicantTeamAvatarUrl: String? = null,
    val applicantTeamPlayers: List<Player> = emptyList(),
    val status: ApplicationStatus = ApplicationStatus.PENDING,
    val appliedAt: Long = System.currentTimeMillis(),
    val respondedAt: Long? = null,
    val notes: String? = null
)

enum class ApplicationStatus {
    PENDING,    // Waiting for host team to approve
    APPROVED,   // Host approved, chat will open at scheduled time
    REJECTED,   // Host declined
    CANCELLED   // Applicant withdrew or match was cancelled
}
