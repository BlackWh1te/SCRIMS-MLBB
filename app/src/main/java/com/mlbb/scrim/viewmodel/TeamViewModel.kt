package com.mlbb.scrim.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mlbb.scrim.data.model.PlayerRole
import com.mlbb.scrim.data.model.Team
import com.mlbb.scrim.data.model.TeamInvite
import com.mlbb.scrim.data.repository.TeamRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch

class TeamViewModel : ViewModel() {

    private val teamRepository = TeamRepository()

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
    
    private val _teams = MutableStateFlow<List<Team>>(emptyList())
    val teams: StateFlow<List<Team>> = _teams.asStateFlow()
    
    private val _currentTeam = MutableStateFlow<Team?>(null)
    val currentTeam: StateFlow<Team?> = _currentTeam.asStateFlow()
    
    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()
    
    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage.asStateFlow()

    // ── Invite state ──
    private val _pendingInvites = MutableStateFlow<List<TeamInvite>>(emptyList())
    val pendingInvites: StateFlow<List<TeamInvite>> = _pendingInvites.asStateFlow()

    private val _teamInvites = MutableStateFlow<List<TeamInvite>>(emptyList())
    val teamInvites: StateFlow<List<TeamInvite>> = _teamInvites.asStateFlow()
    
    init {
        loadTeams()
    }
    
    fun loadTeams() {
        loadTeamsJob?.cancel()
        loadTeamsJob = viewModelScope.launch {
            _isLoading.value = true
            teamRepository.getTeams().collect { result ->
                _isLoading.value = false
                result.onSuccess { teamsList ->
                    _teams.value = teamsList
                }.onFailure { error ->
                    _errorMessage.value = error.message
                }
            }
        }
    }
    
    fun createTeam(name: String, leaderEmail: String) {
        createTeamJob?.cancel()
        createTeamJob = viewModelScope.launch {
            _isLoading.value = true
            _errorMessage.value = null
            teamRepository.createTeam(name, leaderEmail).collect { result ->
                _isLoading.value = false
                result.onSuccess { team ->
                    _currentTeam.value = team
                    loadTeams() // Refresh the list
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
    
    fun clearErrorMessage() {
        _errorMessage.value = null
    }
    
    fun clearCurrentTeam() {
        _currentTeam.value = null
    }
}
