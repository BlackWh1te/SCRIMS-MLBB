package com.mlbb.scrim.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mlbb.scrim.data.model.GameMode
import com.mlbb.scrim.data.model.Region
import com.mlbb.scrim.data.model.Scrim
import com.mlbb.scrim.data.model.ScrimStatus
import com.mlbb.scrim.data.model.SkillLevel
import com.mlbb.scrim.data.repository.ScrimRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class ScrimViewModel : ViewModel() {
    
    private val scrimRepository = ScrimRepository()
    
    private val _scrims = MutableStateFlow<List<Scrim>>(emptyList())
    val scrims: StateFlow<List<Scrim>> = _scrims.asStateFlow()
    
    private val _selectedScrim = MutableStateFlow<Scrim?>(null)
    val selectedScrim: StateFlow<Scrim?> = _selectedScrim.asStateFlow()
    
    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()
    
    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error.asStateFlow()
    
    init {
        loadScrims()
    }
    
    fun loadScrims() {
        viewModelScope.launch {
            _isLoading.value = true
            _error.value = null
            
            scrimRepository.getAllScrims().collect { result ->
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
    
    fun loadScrimById(id: String) {
        viewModelScope.launch {
            _isLoading.value = true
            _error.value = null
            
            scrimRepository.getScrimById(id).collect { result ->
                result.onSuccess { scrim ->
                    _selectedScrim.value = scrim
                    _isLoading.value = false
                }.onFailure { exception ->
                    _error.value = exception.message
                    _isLoading.value = false
                }
            }
        }
    }
    
    fun searchScrims(
        gameMode: GameMode? = null,
        region: Region? = null,
        skillLevel: SkillLevel? = null,
        status: ScrimStatus? = null
    ) {
        viewModelScope.launch {
            _isLoading.value = true
            _error.value = null
            
            scrimRepository.searchScrims(gameMode, region, skillLevel, status).collect { result ->
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
    
    fun joinScrim(scrimId: String, playerId: String) {
        viewModelScope.launch {
            _isLoading.value = true
            _error.value = null
            
            scrimRepository.joinScrim(scrimId, playerId).collect { result ->
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
    
    fun cancelScrim(scrimId: String) {
        viewModelScope.launch {
            _isLoading.value = true
            _error.value = null

            val scrim = _scrims.value.find { it.id == scrimId }
            if (scrim != null) {
                scrimRepository.updateScrim(scrim.copy(status = ScrimStatus.CANCELLED)).collect { result ->
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

    fun leaveScrim(scrimId: String, playerId: String) {
        viewModelScope.launch {
            _isLoading.value = true
            _error.value = null
            
            scrimRepository.leaveScrim(scrimId, playerId).collect { result ->
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
    
    fun clearSelectedScrim() {
        _selectedScrim.value = null
    }
    
    fun clearError() {
        _error.value = null
    }
}
