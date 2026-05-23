package com.mlbb.scrim.viewmodel

import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.mlbb.scrim.data.model.PlayerRole
import com.mlbb.scrim.data.model.Team
import com.mlbb.scrim.data.model.TeamInvite
import com.mlbb.scrim.data.repository.TeamRepositoryInterface
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class TeamViewModel @Inject constructor(
    private val teamRepository: TeamRepositoryInterface,
    application: android.app.Application
) : AndroidViewModel(application) {

    private var loadTeamsJob: Job? = null
    private var createTeamJob: Job? = null
    private var loadTeamJob: Job? = null
    private var addPlayerJob: Job? = null
    private var removePlayerJob: Job? = null
    private var updatePlayerRoleJob: Job? = null
    private var leaveTeamJob: Job? = null
    private var deleteTeamJob: Job? = null
    private var sendInviteJob: Job? = null
    private var acceptInviteJob: Job? = null
    private var declineInviteJob: Job? = null
    private var loadInvitesJob: Job? = null
    private var loadOpenTeamsJob: Job? = null
    private var applyToTeamJob: Job? = null
    private var loadApplicationsJob: Job? = null
    private var acceptApplicationJob: Job? = null
    private var declineApplicationJob: Job? = null
    private var loadTeamStatsJob: Job? = null
    private var loadTeamRatingsJob: Job? = null
    private var submitRatingJob: Job? = null

    private val _teams = MutableStateFlow<List<Team>>(emptyList())
    val teams: StateFlow<List<Team>> = _teams.asStateFlow()
    
    private var currentUserId: String? = null
    
    private val _currentTeam = MutableStateFlow<Team?>(null)
    val currentTeam: StateFlow<Team?> = _currentTeam.asStateFlow()
    
    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _isRefreshing = MutableStateFlow(false)
    val isRefreshing: StateFlow<Boolean> = _isRefreshing.asStateFlow()
    
    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage.asStateFlow()

    private val _createSuccess = MutableStateFlow<Team?>(null)
    val createSuccess: StateFlow<Team?> = _createSuccess.asStateFlow()

    // ── Invite state ──
    private val _pendingInvites = MutableStateFlow<List<TeamInvite>>(emptyList())
    val pendingInvites: StateFlow<List<TeamInvite>> = _pendingInvites.asStateFlow()

    private val _teamInvites = MutableStateFlow<List<TeamInvite>>(emptyList())
    val teamInvites: StateFlow<List<TeamInvite>> = _teamInvites.asStateFlow()

    // ── Application state ──
    private val _openTeams = MutableStateFlow<List<Team>>(emptyList())
    val openTeams: StateFlow<List<Team>> = _openTeams.asStateFlow()

    private val _teamApplications = MutableStateFlow<List<com.mlbb.scrim.data.model.TeamApplication>>(emptyList())
    val teamApplications: StateFlow<List<com.mlbb.scrim.data.model.TeamApplication>> = _teamApplications.asStateFlow()

    private val _applicationSuccess = MutableStateFlow(false)
    val applicationSuccess: StateFlow<Boolean> = _applicationSuccess.asStateFlow()

    // ── Stats & Ratings state ──
    private val _teamStats = MutableStateFlow<Map<String, Any>>(emptyMap())
    val teamStats: StateFlow<Map<String, Any>> = _teamStats.asStateFlow()

    private val _teamRatings = MutableStateFlow<List<com.mlbb.scrim.data.model.TeamRating>>(emptyList())
    val teamRatings: StateFlow<List<com.mlbb.scrim.data.model.TeamRating>> = _teamRatings.asStateFlow()

    private val _averageRating = MutableStateFlow(0.0)
    val averageRating: StateFlow<Double> = _averageRating.asStateFlow()

    init {
        loadTeams()
    }
    
    fun loadTeams(isRefresh: Boolean = false) {
        loadTeamsJob?.cancel()
        loadTeamsJob = viewModelScope.launch {
            if (isRefresh) _isRefreshing.value = true
            _isLoading.value = true
            val userId = currentUserId
            if (userId != null) {
                teamRepository.getTeamsForUser(userId).collect { result ->
                    _isLoading.value = false
                    _isRefreshing.value = false
                    result.onSuccess { teamsList ->
                        _teams.value = teamsList
                    }.onFailure { error ->
                        _errorMessage.value = error.message
                    }
                }
            } else {
                teamRepository.getTeams().collect { result ->
                    _isLoading.value = false
                    _isRefreshing.value = false
                    result.onSuccess { teamsList ->
                        _teams.value = teamsList
                    }.onFailure { error ->
                        _errorMessage.value = error.message
                    }
                }
            }
        }
    }

    /** Set the current user ID so loadTeams() fetches only the user's teams */
    fun setUserId(userId: String?) {
        currentUserId = userId
        if (userId != null) loadTeams()
    }
    
    companion object {
        private const val MAX_LOGO_SIZE_BYTES = 3L * 1024 * 1024 // 3MB
    }

    fun createTeam(name: String, leaderId: String, logoUri: android.net.Uri? = null, isOpenForApplications: Boolean = false) {
        createTeamJob?.cancel()
        createTeamJob = viewModelScope.launch {
            _isLoading.value = true
            _errorMessage.value = null
            teamRepository.createTeam(name, leaderId, isOpenForApplications = isOpenForApplications).collect { result ->
                _isLoading.value = false
                result.onSuccess { team ->
                    _currentTeam.value = team
                    _createSuccess.value = team
                    if (logoUri != null) {
                        try {
                            val context = getApplication<android.app.Application>()
                            val bytes = context.contentResolver.openInputStream(logoUri)?.use { it.readBytes() } ?: return@collect
                            if (bytes.size > MAX_LOGO_SIZE_BYTES) {
                                _errorMessage.value = "Logo is too large. Max size is 3MB."
                                return@collect
                            }
                            (teamRepository as? com.mlbb.scrim.data.repository.SupabaseTeamRepository)
                                ?.uploadTeamLogo(team.id, bytes)
                        } catch (_: Exception) { }
                    }
                    loadTeams()
                }.onFailure { error ->
                    _errorMessage.value = error.message
                }
            }
        }
    }
    
    fun loadTeam(teamId: String) {
        loadTeamJob?.cancel()
        loadTeamJob = viewModelScope.launch {
            _isLoading.value = true
            teamRepository.getTeam(teamId).collect { result ->
                _isLoading.value = false
                result.onSuccess { team ->
                    _currentTeam.value = team
                }.onFailure { error ->
                    _errorMessage.value = error.message
                }
            }
        }
    }
    
    fun addPlayer(teamId: String, playerName: String, playerEmail: String) {
        addPlayerJob?.cancel()
        addPlayerJob = viewModelScope.launch {
            _isLoading.value = true
            _errorMessage.value = null
            teamRepository.addPlayer(teamId, playerName, playerEmail).collect { result ->
                _isLoading.value = false
                result.onSuccess { updatedTeam ->
                    _currentTeam.value = updatedTeam
                    loadTeams() // Refresh the list
                }.onFailure { error ->
                    _errorMessage.value = error.message
                }
            }
        }
    }
    
    fun removePlayer(teamId: String, playerId: String) {
        removePlayerJob?.cancel()
        removePlayerJob = viewModelScope.launch {
            _isLoading.value = true
            _errorMessage.value = null
            teamRepository.removePlayer(teamId, playerId).collect { result ->
                _isLoading.value = false
                result.onSuccess { updatedTeam ->
                    _currentTeam.value = updatedTeam
                    loadTeams() // Refresh the list
                }.onFailure { error ->
                    _errorMessage.value = error.message
                }
            }
        }
    }
    
    fun updatePlayerRole(teamId: String, playerId: String, newRole: PlayerRole) {
        updatePlayerRoleJob?.cancel()
        updatePlayerRoleJob = viewModelScope.launch {
            _isLoading.value = true
            _errorMessage.value = null
            teamRepository.updatePlayerRole(teamId, playerId, newRole).collect { result ->
                _isLoading.value = false
                result.onSuccess { updatedTeam ->
                    _currentTeam.value = updatedTeam
                    loadTeams()
                }.onFailure { error ->
                    _errorMessage.value = error.message
                }
            }
        }
    }

    fun leaveTeam(teamId: String, playerId: String) {
        leaveTeamJob?.cancel()
        leaveTeamJob = viewModelScope.launch {
            _isLoading.value = true
            _errorMessage.value = null
            teamRepository.removePlayer(teamId, playerId).collect { result ->
                _isLoading.value = false
                result.onSuccess { updatedTeam ->
                    _currentTeam.value = updatedTeam
                    loadTeams()
                }.onFailure { error ->
                    _errorMessage.value = error.message
                }
            }
        }
    }

    fun deleteTeam(teamId: String) {
        deleteTeamJob?.cancel()
        deleteTeamJob = viewModelScope.launch {
            _isLoading.value = true
            _errorMessage.value = null
            teamRepository.deleteTeam(teamId).collect { result ->
                _isLoading.value = false
                result.onSuccess {
                    if (_currentTeam.value?.id == teamId) {
                        _currentTeam.value = null
                    }
                    loadTeams() // Refresh the list
                }.onFailure { error ->
                    _errorMessage.value = error.message
                }
            }
        }
    }

    // ═══════════════════════════════════════════════════════════════
    // INVITE FLOW
    // ═══════════════════════════════════════════════════════════════

    /** Captain sends invite to a player */
    fun sendInvite(
        teamId: String,
        teamName: String,
        invitedBy: String,
        invitedByName: String,
        invitedUserId: String,
        invitedUserName: String
    ) {
        sendInviteJob?.cancel()
        sendInviteJob = viewModelScope.launch {
            _isLoading.value = true
            _errorMessage.value = null
            teamRepository.sendInvite(
                teamId, teamName, invitedBy, invitedByName,
                invitedUserId, invitedUserName
            ).collect { result ->
                _isLoading.value = false
                result.onSuccess {
                    loadTeamInvites(teamId)
                }.onFailure { error ->
                    _errorMessage.value = error.message
                }
            }
        }
    }

    /** Player accepts an invite → joins team */
    fun acceptInvite(inviteId: String) {
        acceptInviteJob?.cancel()
        acceptInviteJob = viewModelScope.launch {
            _isLoading.value = true
            _errorMessage.value = null
            teamRepository.acceptInvite(inviteId).collect { result ->
                _isLoading.value = false
                result.onSuccess { team ->
                    _currentTeam.value = team
                    loadTeams()
                    loadPendingInvites(team.leaderId) // Refresh invites
                }.onFailure { error ->
                    _errorMessage.value = error.message
                }
            }
        }
    }

    /** Player declines an invite */
    fun declineInvite(inviteId: String) {
        declineInviteJob?.cancel()
        declineInviteJob = viewModelScope.launch {
            _isLoading.value = true
            _errorMessage.value = null
            teamRepository.declineInvite(inviteId).collect { result ->
                _isLoading.value = false
                result.onSuccess {
                    // Refresh pending invites
                    _pendingInvites.value = _pendingInvites.value.filter { it.id != inviteId }
                }.onFailure { error ->
                    _errorMessage.value = error.message
                }
            }
        }
    }

    /** Load pending invites for a player */
    fun loadPendingInvites(userId: String) {
        loadInvitesJob?.cancel()
        loadInvitesJob = viewModelScope.launch {
            teamRepository.getInvitesForPlayer(userId).collect { result ->
                result.onSuccess { invites ->
                    _pendingInvites.value = invites
                }.onFailure { error ->
                    _errorMessage.value = error.message
                }
            }
        }
    }

    /** Load all invites for a team (captain view) */
    fun loadTeamInvites(teamId: String) {
        viewModelScope.launch {
            teamRepository.getInvitesForTeam(teamId).collect { result ->
                result.onSuccess { invites ->
                    _teamInvites.value = invites
                }.onFailure { error ->
                    _errorMessage.value = error.message
                }
            }
        }
    }
    
    // ─── Application Methods ───

    fun loadOpenTeams() {
        loadOpenTeamsJob?.cancel()
        loadOpenTeamsJob = viewModelScope.launch {
            _isLoading.value = true
            teamRepository.getOpenTeams().collect { result ->
                _isLoading.value = false
                result.onSuccess { teams ->
                    _openTeams.value = teams
                }.onFailure { error ->
                    _errorMessage.value = error.message
                }
            }
        }
    }

    fun applyToTeam(teamId: String, message: String? = null) {
        applyToTeamJob?.cancel()
        applyToTeamJob = viewModelScope.launch {
            _isLoading.value = true
            _applicationSuccess.value = false
            val userId = currentUserId ?: return@launch
            teamRepository.applyToTeam(teamId, userId, message).collect { result ->
                _isLoading.value = false
                result.onSuccess {
                    _applicationSuccess.value = true
                }.onFailure { error ->
                    _errorMessage.value = error.message
                }
            }
        }
    }

    fun loadTeamApplications(teamId: String) {
        loadApplicationsJob?.cancel()
        loadApplicationsJob = viewModelScope.launch {
            _isLoading.value = true
            teamRepository.getTeamApplications(teamId).collect { result ->
                _isLoading.value = false
                result.onSuccess { apps ->
                    _teamApplications.value = apps
                }.onFailure { error ->
                    _errorMessage.value = error.message
                }
            }
        }
    }

    fun acceptApplication(applicationId: String) {
        acceptApplicationJob?.cancel()
        acceptApplicationJob = viewModelScope.launch {
            _isLoading.value = true
            teamRepository.acceptApplication(applicationId).collect { result ->
                _isLoading.value = false
                result.onSuccess { team ->
                    _currentTeam.value = team
                    // Refresh applications
                    loadTeamApplications(team.id)
                }.onFailure { error ->
                    _errorMessage.value = error.message
                }
            }
        }
    }

    fun declineApplication(applicationId: String, teamId: String) {
        declineApplicationJob?.cancel()
        declineApplicationJob = viewModelScope.launch {
            _isLoading.value = true
            teamRepository.declineApplication(applicationId).collect { result ->
                _isLoading.value = false
                result.onSuccess {
                    loadTeamApplications(teamId)
                }.onFailure { error ->
                    _errorMessage.value = error.message
                }
            }
        }
    }

    fun clearApplicationSuccess() {
        _applicationSuccess.value = false
    }

    fun clearErrorMessage() {
        _errorMessage.value = null
    }

    fun clearRefreshing() {
        _isRefreshing.value = false
    }

    fun clearCreateSuccess() {
        _createSuccess.value = null
    }

    fun clearCurrentTeam() {
        _currentTeam.value = null
    }

    // ═══════════════════════════════════════════════════════════════
    // STATS & RATINGS
    // ═══════════════════════════════════════════════════════════════

    fun loadTeamStats(teamId: String) {
        loadTeamStatsJob?.cancel()
        loadTeamStatsJob = viewModelScope.launch {
            teamRepository.getTeamStats(teamId).collect { result ->
                result.onSuccess { _teamStats.value = it }
            }
        }
    }

    fun loadTeamRatings(teamId: String) {
        loadTeamRatingsJob?.cancel()
        loadTeamRatingsJob = viewModelScope.launch {
            teamRepository.getTeamRatings(teamId).collect { result ->
                result.onSuccess { _teamRatings.value = it }
            }
        }
    }

    fun submitTeamRating(
        teamId: String,
        raterTeamId: String,
        raterUserId: String,
        rating: Int,
        feedback: String
    ) {
        submitRatingJob?.cancel()
        submitRatingJob = viewModelScope.launch {
            teamRepository.submitTeamRating(teamId, raterTeamId, raterUserId, rating, feedback).collect { result ->
                result.onSuccess { loadTeamRatings(teamId) }
                    .onFailure { _errorMessage.value = it.message }
            }
        }
    }

    fun clearTeamStats() {
        _teamStats.value = emptyMap()
        _teamRatings.value = emptyList()
        _averageRating.value = 0.0
    }

    // ═══════════════════════════════════════════════════════════════
    // REALTIME SUBSCRIPTIONS
    // ═══════════════════════════════════════════════════════════════

    private var teamRealtimeJob: Job? = null
    private var inviteRealtimeJob: Job? = null

    /**
     * Subscribe to Realtime updates for a specific team.
     * Automatically updates _currentTeam when the team changes.
     */
    fun subscribeToTeamUpdates(teamId: String) {
        teamRealtimeJob?.cancel()
        teamRealtimeJob = viewModelScope.launch {
            teamRepository.subscribeToTeam(teamId).collect { updatedTeam ->
                _currentTeam.value = updatedTeam
            }
        }
    }

    /**
     * Subscribe to Realtime team invitations for the current user.
     * New invites appear instantly in _pendingInvites.
     */
    fun subscribeToTeamInvites(userId: String) {
        inviteRealtimeJob?.cancel()
        inviteRealtimeJob = viewModelScope.launch {
            teamRepository.subscribeToTeamInvites(userId).collect { newInvite ->
                val current = _pendingInvites.value.toMutableList()
                if (current.none { it.id == newInvite.id }) {
                    current.add(0, newInvite)
                    _pendingInvites.value = current
                }
            }
        }
    }

    /**
     * Stop all Realtime subscriptions.
     */
    fun stopRealtimeSubscriptions() {
        teamRealtimeJob?.cancel()
        inviteRealtimeJob?.cancel()
    }

    override fun onCleared() {
        super.onCleared()
        stopRealtimeSubscriptions()
    }
}
