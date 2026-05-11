package com.mlbb.scrim.data.repository

import com.mlbb.scrim.data.model.Player
import com.mlbb.scrim.data.model.PlayerRole
import com.mlbb.scrim.data.model.Team
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow

// Mock TeamRepository for UI testing
// TODO: Replace with actual Supabase implementation when dependencies are resolved
class TeamRepository {
    
    private val teams = mutableListOf<Team>()
    private var currentTeamId: String? = null
    
    suspend fun createTeam(name: String, leaderEmail: String): Flow<Result<Team>> = flow {
        kotlinx.coroutines.delay(500) // Simulate network delay
        
        try {
            val team = Team(
                id = java.util.UUID.randomUUID().toString(),
                name = name,
                leaderId = leaderEmail,
                players = listOf(
                    Player(
                        id = java.util.UUID.randomUUID().toString(),
                        name = "Leader",
                        role = PlayerRole.LEADER,
                        email = leaderEmail
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
    
    suspend fun getTeams(): Flow<Result<List<Team>>> = flow {
        kotlinx.coroutines.delay(300) // Simulate network delay
        emit(Result.success(teams.toList()))
    }
    
    suspend fun getTeam(teamId: String): Flow<Result<Team>> = flow {
        kotlinx.coroutines.delay(300) // Simulate network delay
        val team = teams.find { it.id == teamId }
        if (team != null) {
            emit(Result.success(team))
        } else {
            emit(Result.failure(Exception("Team not found")))
        }
    }
    
    suspend fun addPlayer(teamId: String, playerName: String, playerEmail: String): Flow<Result<Team>> = flow {
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
    
    suspend fun removePlayer(teamId: String, playerId: String): Flow<Result<Team>> = flow {
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
    
    suspend fun updatePlayerRole(teamId: String, playerId: String, newRole: PlayerRole): Flow<Result<Team>> = flow {
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

    suspend fun deleteTeam(teamId: String): Flow<Result<Unit>> = flow {
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
}
