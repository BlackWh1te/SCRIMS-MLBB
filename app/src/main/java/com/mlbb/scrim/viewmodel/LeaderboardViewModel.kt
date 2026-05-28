package com.mlbb.scrim.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mlbb.scrim.data.model.LeaderboardEntry
import com.mlbb.scrim.data.model.RankTier
import com.mlbb.scrim.data.repository.LeaderboardRepositoryInterface
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class LeaderboardViewModel @Inject constructor(
    private val repository: LeaderboardRepositoryInterface
) : ViewModel() {

    private var loadLeaderboardJob: Job? = null
    private var filterByTierJob: Job? = null

    private val _leaderboard = MutableStateFlow<List<LeaderboardEntry>>(emptyList())
    val leaderboard: StateFlow<List<LeaderboardEntry>> = _leaderboard.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _isRefreshing = MutableStateFlow(false)
    val isRefreshing: StateFlow<Boolean> = _isRefreshing.asStateFlow()

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error.asStateFlow()

    private val _selectedTier = MutableStateFlow<RankTier?>(null)
    val selectedTier: StateFlow<RankTier?> = _selectedTier.asStateFlow()

    init {
        loadLeaderboard()
    }

    fun loadLeaderboard(isRefresh: Boolean = false) {
        loadLeaderboardJob?.cancel()
        loadLeaderboardJob = viewModelScope.launch {
            if (isRefresh) _isRefreshing.value = true
            _isLoading.value = true
            _error.value = null
            repository.getLeaderboard().collect { result ->
                result.onSuccess { entries ->
                    _leaderboard.value = entries
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

    fun filterByTier(tier: RankTier?) {
        _selectedTier.value = tier
        filterByTierJob?.cancel()
        filterByTierJob = viewModelScope.launch {
            _isLoading.value = true
            _error.value = null
            if (tier == null) {
                repository.getLeaderboard().collect { result ->
                    result.onSuccess { _leaderboard.value = it }
                        .onFailure { exception ->
                            _error.value = exception.message
                            _isLoading.value = false
                        }
                }
            } else {
                repository.getLeaderboardForTier(tier).collect { result ->
                    result.onSuccess { _leaderboard.value = it }
                        .onFailure { exception ->
                            _error.value = exception.message
                            _isLoading.value = false
                        }
                }
            }
            _isLoading.value = false
        }
    }

    fun clearError() {
        _error.value = null
    }

    fun clearRefreshing() {
        _isRefreshing.value = false
    }

    override fun onCleared() {
        super.onCleared()
        loadLeaderboardJob?.cancel()
        filterByTierJob?.cancel()
    }
}
