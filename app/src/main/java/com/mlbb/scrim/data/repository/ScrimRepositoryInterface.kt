package com.mlbb.scrim.data.repository

import com.mlbb.scrim.data.model.GameMode
import com.mlbb.scrim.data.model.Region
import com.mlbb.scrim.data.model.Scrim
import com.mlbb.scrim.data.model.ScrimApplication
import com.mlbb.scrim.data.model.ScrimRosterEntry
import com.mlbb.scrim.data.model.ScrimStatus
import com.mlbb.scrim.data.model.SkillLevel
import kotlinx.coroutines.flow.Flow

interface ScrimRepositoryInterface {
    fun getAllScrims(): Flow<Result<List<Scrim>>>
    fun getScrimById(id: String): Flow<Result<Scrim?>>
    fun getScrimsByTeam(teamId: String): Flow<Result<List<Scrim>>>
    fun searchScrims(query: String, gameMode: GameMode?, region: Region?, skillLevel: SkillLevel?, status: ScrimStatus?): Flow<Result<List<Scrim>>>
    suspend fun createScrim(scrim: Scrim): Flow<Result<Scrim>>
    suspend fun updateScrim(scrim: Scrim): Flow<Result<Scrim>>
    suspend fun deleteScrim(id: String): Flow<Result<Unit>>
    suspend fun applyToScrim(scrimId: String, application: ScrimApplication): Flow<Result<Scrim>>
    suspend fun approveApplication(scrimId: String, applicationId: String, conversationId: String): Flow<Result<Scrim>>
    suspend fun rejectApplication(scrimId: String, applicationId: String): Flow<Result<Scrim>>
    suspend fun cancelApplication(scrimId: String, applicationId: String): Flow<Result<Scrim>>
    suspend fun setScrimRoster(scrimId: String, teamId: String, roster: List<ScrimRosterEntry>): Flow<Result<Scrim>>
    suspend fun transitionToReadyCheck(scrimId: String): Flow<Result<Scrim>>
    suspend fun markReady(scrimId: String, teamId: String): Flow<Result<Scrim>>
    suspend fun uploadScreenshot(scrimId: String, teamId: String, screenshotUrl: String): Flow<Result<Scrim>>
    suspend fun completeScrim(scrimId: String, winnerTeamId: String): Flow<Result<Scrim>>
    fun calculatePointsChanges(scrim: Scrim): PointsResult
    suspend fun submitResult(scrimId: String, reporterId: String, winnerTeamId: String, notes: String?, screenshotUrl: String?): Flow<Result<Scrim>>
    suspend fun createAutoCancelledRecord(scrimId: String): Flow<Result<Unit>>
}
