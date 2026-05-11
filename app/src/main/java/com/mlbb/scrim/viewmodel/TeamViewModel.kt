package com.mlbb.scrim.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mlbb.scrim.data.model.PlayerRole
import com.mlbb.scrim.data.model.Team
import com.mlbb.scrim.data.repository.TeamRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class TeamViewModel : ViewModel() {
    
    private val teamRepository = TeamRepository()
    
    private val _teams = MutableStateFlow<List<Team>>(emptyList())
    val teams: StateFlow<List<Team>> = _teams.asStateFlow()
    
    private val _currentTeam = MutableStateFlow<Team?>(null)
    val currentTeam: StateFlow<Team?> = _currentTeam.asStateFlow()
    
    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()
    
    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage.asStateFlow()
    
    init {
        loadTeams()
    }
    
    fun loadTeams() {
        viewModelScope.launch {
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
        viewModelScope.launch {
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
        viewModelScope.launch {
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
        viewModelScope.launch {
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
        viewModelScope.launch {
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
        viewModelScope.launch {
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
        viewModelScope.launch {
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
        viewModelScope.launch {
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
    
    fun clearErrorMessage() {
        _errorMessage.value = null
    }
    
    fun clearCurrentTeam() {
        _currentTeam.value = null
    }
}
