package com.mlbb.scrim.data.repository

import com.mlbb.scrim.data.model.PlayerRole
import com.mlbb.scrim.data.model.Team
import com.mlbb.scrim.data.model.TeamInvite
import kotlinx.coroutines.flow.Flow

interface TeamRepositoryInterface {
    suspend fun createTeam(name: String, leaderId: String, description: String = ""): Flow<Result<Team>>
    suspend fun getTeams(): Flow<Result<List<Team>>>
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
}
