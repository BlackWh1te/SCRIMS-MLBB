package com.scrimslegends.app.data.repository

import com.scrimslegends.app.data.model.GameMode
import com.scrimslegends.app.data.model.Region
import com.scrimslegends.app.data.model.Scrim
import com.scrimslegends.app.data.model.ScrimApplication
import com.scrimslegends.app.data.model.ScrimRosterEntry
import com.scrimslegends.app.data.model.ScrimStatus
import com.scrimslegends.app.data.model.SkillLevel
import kotlinx.coroutines.flow.Flow

interface ScrimRepositoryInterface {
    fun getAllScrims(page: Int = 0, pageSize: Int = 200): Flow<Result<List<Scrim>>>
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

    /** Upload a screenshot for a specific game in a best-of series */
    suspend fun uploadGameScreenshot(scrimId: String, teamId: String, gameNumber: Int, screenshotUrl: String): Flow<Result<Scrim>>

    /** Select the winner of a specific game */
    suspend fun selectGameWinner(scrimId: String, gameNumber: Int, winnerTeamId: String): Flow<Result<Scrim>>

    /** Change series format mid-series (e.g. BO5 -> BO3) when teams can't finish all games */
    suspend fun changeSeriesFormat(scrimId: String, newBestOf: Int): Flow<Result<Scrim>>

    suspend fun completeScrim(scrimId: String, winnerTeamId: String?): Flow<Result<Scrim>>
    fun calculatePointsChanges(scrim: Scrim): PointsResult
    suspend fun submitResult(scrimId: String, reporterId: String, winnerTeamId: String, notes: String?, screenshotUrl: String?): Flow<Result<Scrim>>
    suspend fun createAutoCancelledRecord(scrimId: String): Flow<Result<Unit>>
    suspend fun cancelScrim(scrimId: String, reason: String, cancelledBy: String): Flow<Result<Unit>>

    /** Subscribe to Realtime updates for a specific scrim (status changes, ready check, etc.) */
    fun subscribeToScrim(scrimId: String): Flow<Scrim>

    /** Subscribe to Realtime updates for all scrims (new scrims, status changes) */
    fun subscribeToAllScrims(): Flow<Scrim>
}
