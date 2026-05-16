package com.mlbb.scrim.data.repository

import com.mlbb.scrim.data.model.InviteStatus
import com.mlbb.scrim.data.model.Player
import com.mlbb.scrim.data.model.PlayerRole
import com.mlbb.scrim.data.model.Team
import com.mlbb.scrim.data.model.TeamInvite
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow

/**
 * Team repository managing team creation, member invites, and roster management.
 *
 * Current implementation: In-memory mock for UI development.
 * Next step: Integrate with Supabase (table: teams, team_members, team_invitations).
 */
class TeamRepository : TeamRepositoryInterface {
    
    private val teams = mutableListOf<Team>()
    private val invites = mutableListOf<TeamInvite>()
    private var currentTeamId: String? = null
    
    override suspend fun createTeam(name: String, leaderId: String, description: String): Flow<Result<Team>> = flow {
        kotlinx.coroutines.delay(500) // Simulate network delay
        
        try {
            val team = Team(
                id = java.util.UUID.randomUUID().toString(),
                name = name,
                leaderId = leaderId,
                players = listOf(
                    Player(
                        id = java.util.UUID.randomUUID().toString(),
                        name = "Leader",
                        role = PlayerRole.LEADER,
                        email = leaderId
                    )
                )
            )
            teams.add(team)
            currentTeamId = team.id
            emit(Result.success(team))
        } catch (e: Exception) {
            emit(Result.failure(e))
        }
    }
    
    override suspend fun getTeams(): Flow<Result<List<Team>>> = flow {
        kotlinx.coroutines.delay(300) // Simulate network delay
        emit(Result.success(teams.toList()))
    }
    
    override suspend fun getTeam(teamId: String): Flow<Result<Team>> = flow {
        kotlinx.coroutines.delay(300) // Simulate network delay
        val team = teams.find { it.id == teamId }
        if (team != null) {
            emit(Result.success(team))
        } else {
            emit(Result.failure(Exception("Team not found")))
        }
    }
    
    override suspend fun addPlayer(teamId: String, playerName: String, playerEmail: String): Flow<Result<Team>> = flow {
        kotlinx.coroutines.delay(500) // Simulate network delay
        
        try {
            val teamIndex = teams.indexOfFirst { it.id == teamId }
            if (teamIndex == -1) {
                emit(Result.failure(Exception("Team not found")))
                return@flow
            }
            
            val team = teams[teamIndex]
            if (team.isFull) {
                emit(Result.failure(Exception("Team is full (max 7 players)")))
                return@flow
            }
            
            val updatedTeam = team.copy(
                players = team.players + Player(
                    id = java.util.UUID.randomUUID().toString(),
                    name = playerName,
                    role = PlayerRole.MEMBER,
                    email = playerEmail
                )
            )
            
            teams[teamIndex] = updatedTeam
            emit(Result.success(updatedTeam))
        } catch (e: Exception) {
            emit(Result.failure(e))
        }
    }
    
    override suspend fun removePlayer(teamId: String, playerId: String): Flow<Result<Team>> = flow {
        kotlinx.coroutines.delay(500) // Simulate network delay
        
        try {
            val teamIndex = teams.indexOfFirst { it.id == teamId }
            if (teamIndex == -1) {
                emit(Result.failure(Exception("Team not found")))
                return@flow
            }
            
            val team = teams[teamIndex]
            val playerToRemove = team.players.find { it.id == playerId }
            
            if (playerToRemove?.role == PlayerRole.LEADER) {
                emit(Result.failure(Exception("Cannot remove team leader")))
                return@flow
            }
            
            val updatedTeam = team.copy(
                players = team.players.filter { it.id != playerId }
            )
            
            teams[teamIndex] = updatedTeam
            emit(Result.success(updatedTeam))
        } catch (e: Exception) {
            emit(Result.failure(e))
        }
    }
    
    override suspend fun updatePlayerRole(teamId: String, playerId: String, newRole: PlayerRole): Flow<Result<Team>> = flow {
        kotlinx.coroutines.delay(300)

        val teamIndex = teams.indexOfFirst { it.id == teamId }
        if (teamIndex == -1) {
            emit(Result.failure(Exception("Team not found")))
            return@flow
        }

        val team = teams[teamIndex]
        val playerIndex = team.players.indexOfFirst { it.id == playerId }
        if (playerIndex == -1) {
            emit(Result.failure(Exception("Player not found")))
            return@flow
        }

        val updatedPlayers = team.players.toMutableList()
        val player = updatedPlayers[playerIndex]

        // Only allow role changes for non-leader players
        if (player.role == PlayerRole.LEADER) {
            emit(Result.failure(Exception("Cannot change team leader role")))
            return@flow
        }

        updatedPlayers[playerIndex] = player.copy(role = newRole)
        val updatedTeam = team.copy(players = updatedPlayers)
        teams[teamIndex] = updatedTeam
        emit(Result.success(updatedTeam))
    }
    
    override suspend fun deleteTeam(teamId: String): Flow<Result<Unit>> = flow {
        kotlinx.coroutines.delay(500) // Simulate network delay
        
        try {
            teams.removeIf { it.id == teamId }
            if (currentTeamId == teamId) {
                currentTeamId = null
            }
            emit(Result.success(Unit))
        } catch (e: Exception) {
            emit(Result.failure(e))
        }
    }
    
    fun getCurrentTeamId(): String? = currentTeamId
    
    fun generateInviteLink(teamId: String, teamName: String): String {
        // Mock invite link generation
        // In production, this would be a real deep link or web URL
        return "https://mlbb-scrim.app/join?team=$teamId&name=${teamName.replace(" ", "%20")}"
    }

    // ═══════════════════════════════════════════════════════════════
    // INVITE FLOW — Captain invites players, they accept/decline
    // ═══════════════════════════════════════════════════════════════

    /** Captain sends an invite to a player */
    override suspend fun sendInvite(
        teamId: String,
        teamName: String,
        invitedBy: String,
        invitedByName: String,
        invitedUserId: String,
        invitedUserName: String
    ): Flow<Result<TeamInvite>> = flow {
        kotlinx.coroutines.delay(500)
        try {
            // Check team isn't full
            val team = teams.find { it.id == teamId }
            if (team == null) {
                emit(Result.failure(Exception("Team not found")))
                return@flow
            }
            if (team.isFull) {
                emit(Result.failure(Exception("Team is full (max 7 players)")))
                return@flow
            }
            // Check no duplicate pending invite
            val existingInvite = invites.find {
                it.teamId == teamId &&
                it.invitedUserId == invitedUserId &&
                it.status == InviteStatus.PENDING
            }
            if (existingInvite != null) {
                emit(Result.failure(Exception("Invite already sent to this player")))
                return@flow
            }
            // Check player isn't already on the team
            if (team.players.any { it.id == invitedUserId }) {
                emit(Result.failure(Exception("Player is already on the team")))
                return@flow
            }

            val invite = TeamInvite(
                id = java.util.UUID.randomUUID().toString(),
                teamId = teamId,
                teamName = teamName,
                invitedBy = invitedBy,
                invitedByName = invitedByName,
                invitedUserId = invitedUserId,
                invitedUserName = invitedUserName,
                status = InviteStatus.PENDING
            )
            invites.add(invite)
            emit(Result.success(invite))
        } catch (e: Exception) {
            emit(Result.failure(e))
        }
    }

    /** Player accepts an invite → added to team */
    override suspend fun acceptInvite(inviteId: String): Flow<Result<Team>> = flow {
        kotlinx.coroutines.delay(500)
        try {
            val inviteIndex = invites.indexOfFirst { it.id == inviteId }
            if (inviteIndex == -1) {
                emit(Result.failure(Exception("Invite not found")))
                return@flow
            }
            val invite = invites[inviteIndex]
            if (invite.status != InviteStatus.PENDING) {
                emit(Result.failure(Exception("Invite is no longer pending")))
                return@flow
            }
            // Update invite status
            invites[inviteIndex] = invite.copy(
                status = InviteStatus.ACCEPTED,
                respondedAt = System.currentTimeMillis()
            )
            // Add player to team
            val teamIndex = teams.indexOfFirst { it.id == invite.teamId }
            if (teamIndex == -1) {
                emit(Result.failure(Exception("Team not found")))
                return@flow
            }
            val team = teams[teamIndex]
            if (team.isFull) {
                // Team filled since invite was sent
                invites[inviteIndex] = invite.copy(status = InviteStatus.EXPIRED)
                emit(Result.failure(Exception("Team is now full, invite expired")))
                return@flow
            }
            val updatedTeam = team.copy(
                players = team.players + Player(
                    id = invite.invitedUserId,
                    name = invite.invitedUserName,
                    role = PlayerRole.MEMBER,
                    email = ""
                )
            )
            teams[teamIndex] = updatedTeam
            emit(Result.success(updatedTeam))
        } catch (e: Exception) {
            emit(Result.failure(e))
        }
    }

    /** Player declines an invite */
    override suspend fun declineInvite(inviteId: String): Flow<Result<Unit>> = flow {
        kotlinx.coroutines.delay(300)
        try {
            val inviteIndex = invites.indexOfFirst { it.id == inviteId }
            if (inviteIndex == -1) {
                emit(Result.failure(Exception("Invite not found")))
                return@flow
            }
            val invite = invites[inviteIndex]
            if (invite.status != InviteStatus.PENDING) {
                emit(Result.failure(Exception("Invite is no longer pending")))
                return@flow
            }
            invites[inviteIndex] = invite.copy(
                status = InviteStatus.DECLINED,
                respondedAt = System.currentTimeMillis()
            )
            emit(Result.success(Unit))
        } catch (e: Exception) {
            emit(Result.failure(e))
        }
    }

    /** Captain cancels a pending invite */
    suspend fun cancelInvite(inviteId: String): Flow<Result<Unit>> = flow {
        kotlinx.coroutines.delay(300)
        try {
            val inviteIndex = invites.indexOfFirst { it.id == inviteId }
            if (inviteIndex == -1) {
                emit(Result.failure(Exception("Invite not found")))
                return@flow
            }
            invites[inviteIndex] = invites[inviteIndex].copy(status = InviteStatus.CANCELLED)
            emit(Result.success(Unit))
        } catch (e: Exception) {
            emit(Result.failure(e))
        }
    }

    /** Get pending invites for a player */
    override suspend fun getInvitesForPlayer(userId: String): Flow<Result<List<TeamInvite>>> = flow {
        kotlinx.coroutines.delay(300)
        val playerInvites = invites.filter {
            it.invitedUserId == userId && it.status == InviteStatus.PENDING
        }.sortedByDescending { it.createdAt }
        emit(Result.success(playerInvites))
    }

    /** Get all invites for a team (captain view) */
    override suspend fun getInvitesForTeam(teamId: String): Flow<Result<List<TeamInvite>>> = flow {
        kotlinx.coroutines.delay(300)
        val teamInvites = invites.filter { it.teamId == teamId }
        emit(Result.success(teamInvites))
    }
}
