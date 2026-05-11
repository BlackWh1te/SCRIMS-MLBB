package com.mlbb.scrim.data.repository

import com.mlbb.scrim.data.model.Scrim
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow

// Mock ScrimRepository for UI testing without Supabase
// TODO: Replace with actual Supabase implementation when dependencies are resolved
class ScrimRepository {
    
    private val scrims = mutableListOf<Scrim>()
    
    init {
        // Add some sample scrims for testing
        scrims.addAll(
            listOf(
                Scrim(
                    id = java.util.UUID.randomUUID().toString(),
                    teamId = "team1",
                    teamName = "Elite Squad",
                    teamLeader = "player1",
                    gameMode = com.mlbb.scrim.data.model.GameMode.RANKED,
                    region = com.mlbb.scrim.data.model.Region.EU,
                    skillLevel = com.mlbb.scrim.data.model.SkillLevel.ADVANCED,
                    scheduledTime = System.currentTimeMillis() + 3600000, // 1 hour from now
                    maxPlayers = 10,
                    currentPlayers = 6,
                    status = com.mlbb.scrim.data.model.ScrimStatus.OPEN,
                    description = "Looking for 4 more players for ranked scrim"
                ),
                Scrim(
                    id = java.util.UUID.randomUUID().toString(),
                    teamId = "team2",
                    teamName = "Phoenix Rising",
                    teamLeader = "player2",
                    gameMode = com.mlbb.scrim.data.model.GameMode.CUSTOM,
                    region = com.mlbb.scrim.data.model.Region.NA,
                    skillLevel = com.mlbb.scrim.data.model.SkillLevel.INTERMEDIATE,
                    scheduledTime = System.currentTimeMillis() + 7200000, // 2 hours from now
                    maxPlayers = 10,
                    currentPlayers = 8,
                    status = com.mlbb.scrim.data.model.ScrimStatus.OPEN,
                    description = "Custom game scrim, all welcome"
                ),
                Scrim(
                    id = java.util.UUID.randomUUID().toString(),
                    teamId = "team3",
                    teamName = "Moscow Wolves",
                    teamLeader = "player3",
                    gameMode = com.mlbb.scrim.data.model.GameMode.RANKED,
                    region = com.mlbb.scrim.data.model.Region.MCK,
                    skillLevel = com.mlbb.scrim.data.model.SkillLevel.PRO,
                    scheduledTime = System.currentTimeMillis() + 86400000, // tomorrow
                    maxPlayers = 10,
                    currentPlayers = 5,
                    status = com.mlbb.scrim.data.model.ScrimStatus.OPEN,
                    description = "Looking for a scrim tomorrow evening in Moscow"
                ),
                Scrim(
                    id = java.util.UUID.randomUUID().toString(),
                    teamId = "team4",
                    teamName = "Night Owls",
                    teamLeader = "player4",
                    gameMode = com.mlbb.scrim.data.model.GameMode.CUSTOM,
                    region = com.mlbb.scrim.data.model.Region.ASIA,
                    skillLevel = com.mlbb.scrim.data.model.SkillLevel.ALL,
                    scheduledTime = System.currentTimeMillis() + 18000000, // 5 hours from now
                    maxPlayers = 10,
                    currentPlayers = 3,
                    status = com.mlbb.scrim.data.model.ScrimStatus.OPEN,
                    description = "Late night custom scrim"
                ),
                Scrim(
                    id = java.util.UUID.randomUUID().toString(),
                    teamId = "team5",
                    teamName = "Apex Predators",
                    teamLeader = "player5",
                    gameMode = com.mlbb.scrim.data.model.GameMode.TOURNAMENT,
                    region = com.mlbb.scrim.data.model.Region.NA,
                    skillLevel = com.mlbb.scrim.data.model.SkillLevel.ADVANCED,
                    scheduledTime = System.currentTimeMillis() + 172800000, // day after tomorrow
                    maxPlayers = 10,
                    currentPlayers = 10,
                    status = com.mlbb.scrim.data.model.ScrimStatus.FILLED,
                    description = "Tournament practice match"
                ),
                Scrim(
                    id = java.util.UUID.randomUUID().toString(),
                    teamId = "team6",
                    teamName = "Storm Riders",
                    teamLeader = "player6",
                    gameMode = com.mlbb.scrim.data.model.GameMode.RANKED,
                    region = com.mlbb.scrim.data.model.Region.EU,
                    skillLevel = com.mlbb.scrim.data.model.SkillLevel.INTERMEDIATE,
                    scheduledTime = System.currentTimeMillis() + 259200000, // 3 days from now
                    maxPlayers = 10,
                    currentPlayers = 7,
                    status = com.mlbb.scrim.data.model.ScrimStatus.OPEN,
                    description = "Weekend ranked scrim"
                )
            )
        )
    }
    
    suspend fun getAllScrims(): Flow<Result<List<Scrim>>> = flow {
        emit(Result.success(scrims.toList()))
    }
    
    suspend fun getScrimById(id: String): Flow<Result<Scrim?>> = flow {
        emit(Result.success(scrims.find { it.id == id }))
    }
    
    suspend fun getScrimsByTeam(teamId: String): Flow<Result<List<Scrim>>> = flow {
        emit(Result.success(scrims.filter { it.teamId == teamId }))
    }
    
    suspend fun searchScrims(
        gameMode: com.mlbb.scrim.data.model.GameMode? = null,
        region: com.mlbb.scrim.data.model.Region? = null,
        skillLevel: com.mlbb.scrim.data.model.SkillLevel? = null,
        status: com.mlbb.scrim.data.model.ScrimStatus? = null
    ): Flow<Result<List<Scrim>>> = flow {
        var results = scrims.toList()
        
        if (gameMode != null) {
            results = results.filter { it.gameMode == gameMode }
        }
        if (region != null) {
            results = results.filter { it.region == region }
        }
        if (skillLevel != null) {
            results = results.filter { it.skillLevel == skillLevel }
        }
        if (status != null) {
            results = results.filter { it.status == status }
        }
        
        emit(Result.success(results))
    }
    
    suspend fun createScrim(scrim: Scrim): Flow<Result<Scrim>> = flow {
        kotlinx.coroutines.delay(500) // Simulate network delay
        
        val newScrim = scrim.copy(id = java.util.UUID.randomUUID().toString())
        scrims.add(newScrim)
        emit(Result.success(newScrim))
    }
    
    suspend fun updateScrim(scrim: Scrim): Flow<Result<Scrim>> = flow {
        kotlinx.coroutines.delay(500) // Simulate network delay
        
        val index = scrims.indexOfFirst { it.id == scrim.id }
        if (index != -1) {
            scrims[index] = scrim
            emit(Result.success(scrim))
        } else {
            emit(Result.failure(Exception("Scrim not found")))
        }
    }
    
    suspend fun deleteScrim(id: String): Flow<Result<Unit>> = flow {
        kotlinx.coroutines.delay(500) // Simulate network delay
        
        val removed = scrims.removeIf { it.id == id }
        if (removed) {
            emit(Result.success(Unit))
        } else {
            emit(Result.failure(Exception("Scrim not found")))
        }
    }
    
    suspend fun joinScrim(scrimId: String, playerId: String): Flow<Result<Scrim>> = flow {
        kotlinx.coroutines.delay(500) // Simulate network delay
        
        scrimId?.let { id ->
            val index = scrims.indexOfFirst { it.id == id }
            if (index != -1) {
                val scrim = scrims[index]
                if (scrim.currentPlayers < scrim.maxPlayers) {
                    scrims[index] = scrim.copy(currentPlayers = scrim.currentPlayers + 1)
                    emit(Result.success(scrims[index]))
                } else {
                    emit(Result.failure(Exception("Scrim is already full")))
                }
            } else {
                emit(Result.failure(Exception("Scrim not found")))
            }
        }
    }
    
    suspend fun leaveScrim(scrimId: String, playerId: String): Flow<Result<Scrim>> = flow {
        kotlinx.coroutines.delay(500) // Simulate network delay
        
        scrimId?.let { id ->
            val index = scrims.indexOfFirst { it.id == id }
            if (index != -1) {
                val scrim = scrims[index]
                if (scrim.currentPlayers > 0) {
                    scrims[index] = scrim.copy(currentPlayers = scrim.currentPlayers - 1)
                    emit(Result.success(scrims[index]))
                } else {
                    emit(Result.failure(Exception("Scrim is already empty")))
                }
            } else {
                emit(Result.failure(Exception("Scrim not found")))
            }
        }
    }
}
