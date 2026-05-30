package com.scrimslegends.app.data.repository

import com.scrimslegends.app.data.model.PlayerRole
import com.scrimslegends.app.data.model.Team
import com.scrimslegends.app.data.model.TeamApplication
import com.scrimslegends.app.data.model.TeamInvite
import kotlinx.coroutines.flow.Flow

interface TeamRepositoryInterface {
    suspend fun createTeam(name: String, leaderId: String, description: String = "", isOpenForApplications: Boolean = false): Flow<Result<Team>>
    suspend fun getTeams(): Flow<Result<List<Team>>>
    suspend fun getTeamsForUser(userId: String): Flow<Result<List<Team>>>
    suspend fun getTeam(teamId: String): Flow<Result<Team>>
    suspend fun addPlayer(teamId: String, playerName: String, playerEmail: String): Flow<Result<Team>>
    suspend fun removePlayer(teamId: String, playerId: String): Flow<Result<Team>>
    suspend fun updatePlayerRole(teamId: String, playerId: String, newRole: PlayerRole): Flow<Result<Team>>
    suspend fun deleteTeam(teamId: String): Flow<Result<Unit>>
    suspend fun sendInvite(teamId: String, teamName: String, invitedBy: String, invitedByName: String, invitedUserId: String, invitedUserName: String): Flow<Result<TeamInvite>>
    suspend fun acceptInvite(inviteId: String): Flow<Result<Team>>
    suspend fun declineInvite(inviteId: String): Flow<Result<Unit>>
    suspend fun getInvitesForPlayer(userId: String): Flow<Result<List<TeamInvite>>>
    suspend fun getInvitesForTeam(teamId: String): Flow<Result<List<TeamInvite>>>

    // ─── Team Application Methods ───
    suspend fun getOpenTeams(): Flow<Result<List<Team>>>
    suspend fun applyToTeam(teamId: String, applicantUserId: String, message: String? = null): Flow<Result<TeamApplication>>
    suspend fun getTeamApplications(teamId: String): Flow<Result<List<TeamApplication>>>
    suspend fun getMyApplications(userId: String): Flow<Result<List<TeamApplication>>>
    suspend fun acceptApplication(applicationId: String): Flow<Result<Team>>
    suspend fun declineApplication(applicationId: String): Flow<Result<Unit>>

    /** Subscribe to Realtime updates for a specific team (member changes, settings) */
    fun subscribeToTeam(teamId: String): Flow<Team>

    /** Subscribe to Realtime team invitations for a specific user */
    fun subscribeToTeamInvites(userId: String): Flow<TeamInvite>

    /** Fetch aggregate team stats (total scrims, wins, losses, points) from current members */
    suspend fun getTeamStats(teamId: String): Flow<Result<Map<String, Any>>>

    /** Fetch peer ratings + feedback left for this team */
    suspend fun getTeamRatings(teamId: String): Flow<Result<List<com.scrimslegends.app.data.model.TeamRating>>>

    /** Submit a rating + optional feedback for another team */
    suspend fun submitTeamRating(
        teamId: String,
        raterTeamId: String,
        raterUserId: String,
        rating: Int,
        feedback: String
    ): Flow<Result<Unit>>
}
