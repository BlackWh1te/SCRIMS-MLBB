package com.scrimslegends.app.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.scrimslegends.app.data.model.ApplicationStatus
import com.scrimslegends.app.data.model.BestOf
import com.scrimslegends.app.data.model.GameMode
import com.scrimslegends.app.data.model.Region
import com.scrimslegends.app.data.model.Scrim
import com.scrimslegends.app.data.model.ScrimApplication
import com.scrimslegends.app.data.model.ScrimRosterEntry
import com.scrimslegends.app.data.model.ScrimStatus
import com.scrimslegends.app.data.model.SkillLevel
import com.scrimslegends.app.data.model.Player
import com.scrimslegends.app.data.repository.PlayerPointsChange
import com.scrimslegends.app.security.AuthorizationUtils
import com.scrimslegends.app.data.repository.PointsResult
import com.scrimslegends.app.data.repository.ScrimRepositoryInterface
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class ScrimViewModel @Inject constructor(
    private val scrimRepository: ScrimRepositoryInterface
) : ViewModel() {

    private val _scrims = MutableStateFlow<List<Scrim>>(emptyList())
    val scrims: StateFlow<List<Scrim>> = _scrims.asStateFlow()

    private val _selectedScrim = MutableStateFlow<Scrim?>(null)
    val selectedScrim: StateFlow<Scrim?> = _selectedScrim.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _isRefreshing = MutableStateFlow(false)
    val isRefreshing: StateFlow<Boolean> = _isRefreshing.asStateFlow()

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error.asStateFlow()

    // ── Points calculation result ──
    private val _pointsResult = MutableStateFlow<PointsResult>(PointsResult.empty())
    val pointsResult: StateFlow<PointsResult> = _pointsResult.asStateFlow()

    private var loadScrimsJob: Job? = null
    private var searchScrimsJob: Job? = null
    private var loadScrimJob: Job? = null

    init {
        loadScrims()
    }

    fun loadScrims(isRefresh: Boolean = false) {
        loadScrimsJob?.cancel()
        loadScrimsJob = viewModelScope.launch {
            if (isRefresh) _isRefreshing.value = true
            _isLoading.value = true
            _error.value = null

            scrimRepository.getAllScrims()
                .onStart { _isLoading.value = true }
                .catch { exception ->
                    _error.value = exception.message
                    _isLoading.value = false
                    _isRefreshing.value = false
                }
                .collect { result ->
                    result.onSuccess { scrimList ->
                        _scrims.value = scrimList
                        _isLoading.value = false
                        _isRefreshing.value = false
                    }.onFailure { exception ->
                        _error.value = exception.message
                        _isLoading.value = false
                        _isRefreshing.value = false
                    }
                }
        }
    }

    fun loadScrimById(id: String) {
        loadScrimJob?.cancel()
        loadScrimJob = viewModelScope.launch {
            _isLoading.value = true
            _error.value = null

            scrimRepository.getScrimById(id)
                .onStart { _isLoading.value = true }
                .catch { exception ->
                    _error.value = exception.message
                    _isLoading.value = false
                }
                .collect { result ->
                    result.onSuccess { scrim ->
                        _selectedScrim.value = scrim
                        _isLoading.value = false
                    }.onFailure { exception ->
                        _selectedScrim.value = null
                        _error.value = exception.message
                        _isLoading.value = false
                    }
                }
        }
    }

    fun searchScrims(
        query: String = "",
        gameMode: GameMode? = null,
        region: Region? = null,
        skillLevel: SkillLevel? = null,
        status: ScrimStatus? = null
    ) {
        searchScrimsJob?.cancel()
        searchScrimsJob = viewModelScope.launch {
            _isLoading.value = true
            _error.value = null

            scrimRepository.searchScrims(query, gameMode, region, skillLevel, status)
                .onStart { _isLoading.value = true }
                .catch { exception ->
                    _error.value = exception.message
                    _isLoading.value = false
                }
                .collect { result ->
                    result.onSuccess { scrimList ->
                        _scrims.value = scrimList
                        _isLoading.value = false
                    }.onFailure { exception ->
                        _error.value = exception.message
                        _isLoading.value = false
                    }
                }
        }
    }
    
    fun createScrim(
        teamId: String,
        teamName: String,
        teamLeader: String,
        gameMode: GameMode,
        region: Region,
        skillLevel: SkillLevel,
        bestOf: BestOf,
        scheduledTime: Long,
        description: String
    ) {
        viewModelScope.launch {
            _isLoading.value = true
            _error.value = null
            
            val newScrim = Scrim(
                teamId = teamId,
                teamName = teamName,
                teamLeader = teamLeader,
                gameMode = gameMode,
                region = region,
                skillLevel = skillLevel,
                bestOf = bestOf,
                scheduledTime = scheduledTime,
                description = description
            )
            
            scrimRepository.createScrim(newScrim).collect { result ->
                result.onSuccess { scrim ->
                    _selectedScrim.value = scrim
                    loadScrims() // Refresh the list
                    _isLoading.value = false
                }.onFailure { exception ->
                    _error.value = exception.message
                    _isLoading.value = false
                }
            }
        }
    }
    
    fun updateScrim(scrim: Scrim) {
        viewModelScope.launch {
            _isLoading.value = true
            _error.value = null
            
            scrimRepository.updateScrim(scrim).collect { result ->
                result.onSuccess { updatedScrim ->
                    _selectedScrim.value = updatedScrim
                    loadScrims() // Refresh the list
                    _isLoading.value = false
                }.onFailure { exception ->
                    _error.value = exception.message
                    _isLoading.value = false
                }
            }
        }
    }
    
    fun deleteScrim(id: String) {
        viewModelScope.launch {
            _isLoading.value = true
            _error.value = null
            
            scrimRepository.deleteScrim(id).collect { result ->
                result.onSuccess {
                    _selectedScrim.value = null
                    loadScrims() // Refresh the list
                    _isLoading.value = false
                }.onFailure { exception ->
                    _error.value = exception.message
                    _isLoading.value = false
                }
            }
        }
    }
    
    fun cancelScrim(scrimId: String, reason: String = "Cancelled by user") {
        viewModelScope.launch {
            _isLoading.value = true
            _error.value = null

            val scrim = _scrims.value.find { it.id == scrimId }
                ?: scrimRepository.getScrimById(scrimId)
                    .catch { emit(Result.failure(it)) }
                    .firstOrNull()
                    ?.getOrNull()

            if (scrim != null) {
                val cancelledBy = AuthorizationUtils.currentUserId()
                scrimRepository.updateScrim(
                    scrim.copy(
                        status = ScrimStatus.CANCELLED,
                        cancellationReason = reason,
                        cancelledBy = cancelledBy
                    )
                ).collect { result ->
                    result.onSuccess { updated ->
                        _selectedScrim.value = updated
                        loadScrims()
                        _isLoading.value = false
                    }.onFailure { exception ->
                        _error.value = exception.message
                        _isLoading.value = false
                    }
                }
            } else {
                _error.value = "Scrim not found"
                _isLoading.value = false
            }
        }
    }

    // ═══════════════════════════════════════════════════════════════
    // TEAM VS TEAM APPLICATION FLOW
    // ═══════════════════════════════════════════════════════════════

    /** Team B applies to a scrim posted by Team A */
    fun applyToScrim(
        scrimId: String,
        applicantTeamId: String,
        applicantTeamName: String,
        applicantTeamLeader: String,
        applicantTeamLeaderName: String
    ) {
        viewModelScope.launch {
            _isLoading.value = true
            _error.value = null

            val application = ScrimApplication(
                scrimId = scrimId,
                applicantTeamId = applicantTeamId,
                applicantTeamName = applicantTeamName,
                applicantTeamLeader = applicantTeamLeader,
                applicantTeamLeaderName = applicantTeamLeaderName,
                status = ApplicationStatus.PENDING
            )

            scrimRepository.applyToScrim(scrimId, application).collect { result ->
                result.onSuccess { scrim ->
                    _selectedScrim.value = scrim
                    loadScrims()
                    _isLoading.value = false
                }.onFailure { exception ->
                    _error.value = exception.message
                    _isLoading.value = false
                }
            }
        }
    }

    /** Team A leader approves an application from Team B */
    fun approveApplication(scrimId: String, applicationId: String, conversationId: String) {
        viewModelScope.launch {
            _isLoading.value = true
            _error.value = null

            scrimRepository.approveApplication(scrimId, applicationId, conversationId)
                .collect { result ->
                    result.onSuccess { scrim ->
                        _selectedScrim.value = scrim
                        loadScrims()
                        _isLoading.value = false
                    }.onFailure { exception ->
                        _error.value = exception.message
                        _isLoading.value = false
                    }
                }
        }
    }

    /** Team A leader rejects an application */
    fun rejectApplication(scrimId: String, applicationId: String) {
        viewModelScope.launch {
            _isLoading.value = true
            _error.value = null

            scrimRepository.rejectApplication(scrimId, applicationId).collect { result ->
                result.onSuccess { scrim ->
                    _selectedScrim.value = scrim
                    loadScrims()
                    _isLoading.value = false
                }.onFailure { exception ->
                    _error.value = exception.message
                    _isLoading.value = false
                }
            }
        }
    }

    /** Applicant cancels their pending application */
    fun cancelApplication(scrimId: String, applicationId: String) {
        viewModelScope.launch {
            _isLoading.value = true
            _error.value = null

            scrimRepository.cancelApplication(scrimId, applicationId).collect { result ->
                result.onSuccess { scrim ->
                    _selectedScrim.value = scrim
                    loadScrims()
                    _isLoading.value = false
                }.onFailure { exception ->
                    _error.value = exception.message
                    _isLoading.value = false
                }
            }
        }
    }

    // ═══════════════════════════════════════════════════════════════
    // SCRIM ROSTER — Captain assigns active/substitute players
    // ═══════════════════════════════════════════════════════════════

    /** Captain sets the roster for their team in a scrim */
    fun setScrimRoster(
        scrimId: String,
        teamId: String,
        roster: List<ScrimRosterEntry>
    ) {
        viewModelScope.launch {
            _isLoading.value = true
            _error.value = null

            scrimRepository.setScrimRoster(scrimId, teamId, roster).collect { result ->
                result.onSuccess { scrim ->
                    _selectedScrim.value = scrim
                    loadScrims()
                    _isLoading.value = false
                }.onFailure { exception ->
                    _error.value = exception.message
                    _isLoading.value = false
                }
            }
        }
    }

    /** Build roster entries from team players (defaults: all substitutes) */
    fun buildDefaultRoster(teamId: String, players: List<Player>): List<ScrimRosterEntry> {
        return players.map { player ->
            ScrimRosterEntry(
                playerId = player.id,
                playerName = player.name,
                teamId = teamId,
                isActive = false  // Default to substitute, captain must activate
            )
        }
    }

    // ═══════════════════════════════════════════════════════════════
    // READY FLOW — Captains press Ready at match start time
    // ═══════════════════════════════════════════════════════════════

    /** Transition scrim to READY_CHECK status at match time */
    fun transitionToReadyCheck(scrimId: String) {
        viewModelScope.launch {
            _isLoading.value = true
            _error.value = null

            scrimRepository.transitionToReadyCheck(scrimId).collect { result ->
                result.onSuccess { scrim ->
                    _selectedScrim.value = scrim
                    loadScrims()
                    _isLoading.value = false
                }.onFailure { exception ->
                    _error.value = exception.message
                    _isLoading.value = false
                }
            }
        }
    }

    /** Captain presses Ready button */
    fun markReady(scrimId: String, teamId: String) {
        viewModelScope.launch {
            _isLoading.value = true
            _error.value = null

            scrimRepository.markReady(scrimId, teamId).collect { result ->
                result.onSuccess { scrim ->
                    _selectedScrim.value = scrim
                    loadScrims()
                    _isLoading.value = false
                }.onFailure { exception ->
                    _error.value = exception.message
                    _isLoading.value = false
                }
            }
        }
    }

    // ═══════════════════════════════════════════════════════════════
    // SCREENSHOT FLOW
    // ═══════════════════════════════════════════════════════════════

    /** Captain uploads a screenshot */
    fun uploadScreenshot(scrimId: String, teamId: String, screenshotUrl: String) {
        viewModelScope.launch {
            _isLoading.value = true
            _error.value = null

            scrimRepository.uploadScreenshot(scrimId, teamId, screenshotUrl).collect { result ->
                result.onSuccess { scrim ->
                    _selectedScrim.value = scrim
                    loadScrims()
                    _isLoading.value = false
                }.onFailure { exception ->
                    _error.value = exception.message
                    _isLoading.value = false
                }
            }
        }
    }

    /** Upload a screenshot for a specific game in a best-of series */
    fun uploadGameScreenshot(scrimId: String, teamId: String, gameNumber: Int, screenshotUrl: String) {
        viewModelScope.launch {
            _isLoading.value = true
            _error.value = null

            scrimRepository.uploadGameScreenshot(scrimId, teamId, gameNumber, screenshotUrl).collect { result ->
                result.onSuccess { scrim ->
                    _selectedScrim.value = scrim
                    loadScrims()
                    _isLoading.value = false
                }.onFailure { exception ->
                    _error.value = exception.message
                    _isLoading.value = false
                }
            }
        }
    }

    /** Select the winner of a specific game */
    fun selectGameWinner(scrimId: String, gameNumber: Int, winnerTeamId: String) {
        viewModelScope.launch {
            _isLoading.value = true
            _error.value = null

            scrimRepository.selectGameWinner(scrimId, gameNumber, winnerTeamId).collect { result ->
                result.onSuccess { scrim ->
                    _selectedScrim.value = scrim
                    loadScrims()
                    _isLoading.value = false
                }.onFailure { exception ->
                    _error.value = exception.message
                    _isLoading.value = false
                }
            }
        }
    }

    // ═══════════════════════════════════════════════════════════════
    // COMPLETE SCRIM — Select winner, calculate points
    // ═══════════════════════════════════════════════════════════════

    /** Complete scrim with winner selection */
    fun completeScrim(scrimId: String, winnerTeamId: String) {
        viewModelScope.launch {
            _isLoading.value = true
            _error.value = null

            scrimRepository.completeScrim(scrimId, winnerTeamId).collect { result ->
                result.onSuccess { scrim ->
                    _selectedScrim.value = scrim
                    // Calculate points
                    _pointsResult.value = scrimRepository.calculatePointsChanges(scrim)
                    loadScrims()
                    _isLoading.value = false
                }.onFailure { exception ->
                    _error.value = exception.message
                    _isLoading.value = false
                }
            }
        }
    }

    /** Submit match result (legacy compatibility) */
    fun submitMatchResult(
        scrimId: String,
        reporterId: String,
        winnerTeamId: String,
        notes: String? = null,
        screenshotUrl: String? = null
    ) {
        viewModelScope.launch {
            _isLoading.value = true
            _error.value = null

            scrimRepository.submitResult(scrimId, reporterId, winnerTeamId, notes, screenshotUrl)
                .collect { result ->
                    result.onSuccess { scrim ->
                        _selectedScrim.value = scrim
                        _pointsResult.value = scrimRepository.calculatePointsChanges(scrim)
                        loadScrims()
                        _isLoading.value = false
                    }.onFailure { exception ->
                        _error.value = exception.message
                        _isLoading.value = false
                    }
                }
        }
    }

    /** Check overdue scrims and auto-cancel if no result within 2h deadline */
    fun checkAndAutoCancelOverdueScrims() {
        viewModelScope.launch {
            scrimRepository.getAllScrims().collect { result ->
                result.onSuccess { allScrims ->
                    val overdueScrims = allScrims.filter { scrim ->
                        scrim.opponentTeamId != null &&
                                scrim.isAutoCancelOverdue &&
                                scrim.resultSubmittedAt == null &&
                                scrim.status in setOf(ScrimStatus.IN_PROGRESS, ScrimStatus.READY_CHECK, ScrimStatus.FILLED)
                    }

                    overdueScrims.forEach { scrim ->
                        scrimRepository.createAutoCancelledRecord(scrim.id).collect { cancelResult ->
                            cancelResult.onSuccess {
                                loadScrims()
                            }.onFailure { exception ->
                                _error.value = "Failed to auto-cancel overdue scrim: ${exception.message}"
                            }
                        }
                    }
                }.onFailure { exception ->
                    _error.value = "Failed to load scrims for auto-cancel check: ${exception.message}"
                }
            }
        }
    }

    fun clearSelectedScrim() {
        _selectedScrim.value = null
    }
    
    fun clearError() {
        _error.value = null
    }

    fun clearRefreshing() {
        _isRefreshing.value = false
    }

    fun clearPointsResult() {
        _pointsResult.value = PointsResult.empty()
    }

    // ═══════════════════════════════════════════════════════════════
    // REALTIME SUBSCRIPTIONS
    // ═══════════════════════════════════════════════════════════════

    private var scrimRealtimeJob: Job? = null
    private var allScrimsRealtimeJob: Job? = null

    /**
     * Subscribe to Realtime updates for a specific scrim.
     * Automatically updates _selectedScrim when the scrim changes.
     */
    fun subscribeToScrimUpdates(scrimId: String) {
        scrimRealtimeJob?.cancel()
        scrimRealtimeJob = viewModelScope.launch {
            scrimRepository.subscribeToScrim(scrimId).collect { updatedScrim ->
                _selectedScrim.value = updatedScrim
            }
        }
    }

    /**
     * Subscribe to Realtime updates for all scrims.
     * Automatically updates _scrims list when any scrim changes.
     */
    fun subscribeToAllScrimUpdates() {
        allScrimsRealtimeJob?.cancel()
        allScrimsRealtimeJob = viewModelScope.launch {
            scrimRepository.subscribeToAllScrims().collect { updatedScrim ->
                val current = _scrims.value.toMutableList()
                val index = current.indexOfFirst { it.id == updatedScrim.id }
                if (index >= 0) {
                    current[index] = updatedScrim
                } else {
                    current.add(0, updatedScrim) // New scrim
                }
                _scrims.value = current
            }
        }
    }

    /**
     * Stop all Realtime subscriptions.
     */
    fun stopRealtimeSubscriptions() {
        scrimRealtimeJob?.cancel()
        allScrimsRealtimeJob?.cancel()
    }

    override fun onCleared() {
        super.onCleared()
        stopRealtimeSubscriptions()
    }
}
