package com.scrimslegends.app.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.scrimslegends.app.data.model.LeaderboardEntry
import com.scrimslegends.app.data.model.RankTier
import com.scrimslegends.app.data.repository.LeaderboardRepositoryInterface
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.toPersistentList
import javax.inject.Inject

@HiltViewModel
class LeaderboardViewModel @Inject constructor(
    private val repository: LeaderboardRepositoryInterface
) : ViewModel() {

    private var loadLeaderboardJob: Job? = null
    private var filterByTierJob: Job? = null

    private val _leaderboard = MutableStateFlow<ImmutableList<LeaderboardEntry>>(persistentListOf())
    val leaderboard: StateFlow<ImmutableList<LeaderboardEntry>> = _leaderboard.asStateFlow()

    private val _teamLeaderboard = MutableStateFlow<ImmutableList<com.scrimslegends.app.data.model.TeamLeaderboardEntry>>(persistentListOf())
    val teamLeaderboard: StateFlow<ImmutableList<com.scrimslegends.app.data.model.TeamLeaderboardEntry>> = _teamLeaderboard.asStateFlow()

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

            launch {
                repository.getLeaderboard().collect { playersResult ->
                    playersResult.onSuccess { entries ->
                        _leaderboard.value = entries.toPersistentList()
                    }.onFailure { exception ->
                        _error.value = exception.message
                    }
                    _isLoading.value = false
                    _isRefreshing.value = false
                }
            }

            launch {
                repository.getTeamLeaderboard().collect { teamsResult ->
                    teamsResult.onSuccess { entries ->
                        _teamLeaderboard.value = entries.toPersistentList()
                    }.onFailure { exception ->
                        if (_error.value == null) _error.value = exception.message
                    }
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

            launch {
                val playersFlow = if (tier == null) {
                    repository.getLeaderboard()
                } else {
                    repository.getLeaderboardForTier(tier)
                }
                playersFlow.collect { playersResult ->
                    playersResult.onSuccess { _leaderboard.value = it.toPersistentList() }
                        .onFailure { exception -> _error.value = exception.message }
                    _isLoading.value = false
                }
            }

            launch {
                repository.getTeamLeaderboard().collect { teamsResult ->
                    teamsResult.onSuccess { entries ->
                        val filteredTeams = if (tier != null) {
                            entries.filter { it.currentTier == tier }
                        } else entries
                        _teamLeaderboard.value = filteredTeams.toPersistentList()
                    }.onFailure { exception ->
                        if (_error.value == null) _error.value = exception.message
                    }
                }
            }
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
