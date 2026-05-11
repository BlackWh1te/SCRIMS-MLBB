package com.mlbb.scrim.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mlbb.scrim.data.model.LeaderboardEntry
import com.mlbb.scrim.data.model.RankTier
import com.mlbb.scrim.data.repository.LeaderboardRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class LeaderboardViewModel : ViewModel() {

    private val repository = LeaderboardRepository()

    private val _leaderboard = MutableStateFlow<List<LeaderboardEntry>>(emptyList())
    val leaderboard: StateFlow<List<LeaderboardEntry>> = _leaderboard.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _selectedTier = MutableStateFlow<RankTier?>(null)
    val selectedTier: StateFlow<RankTier?> = _selectedTier.asStateFlow()

    init {
        loadLeaderboard()
    }

    fun loadLeaderboard() {
        viewModelScope.launch {
            _isLoading.value = true
            repository.getLeaderboard().collect { result ->
                result.onSuccess { entries ->
                    _leaderboard.value = entries
                }
                _isLoading.value = false
            }
        }
    }

    fun filterByTier(tier: RankTier?) {
        _selectedTier.value = tier
        viewModelScope.launch {
            _isLoading.value = true
            if (tier == null) {
                repository.getLeaderboard().collect { result ->
                    result.onSuccess { _leaderboard.value = it }
                }
            } else {
                repository.getLeaderboardForTier(tier).collect { result ->
                    result.onSuccess { _leaderboard.value = it }
                }
            }
            _isLoading.value = false
        }
    }
}
