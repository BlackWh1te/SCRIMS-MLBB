package com.mlbb.scrim.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mlbb.scrim.data.model.MatchResult
import com.mlbb.scrim.data.repository.MatchResultRepositoryInterface
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class MatchResultViewModel @Inject constructor(
    private val matchResultRepository: MatchResultRepositoryInterface
) : ViewModel() {

    private var loadMatchResultsJob: Job? = null
    private var loadMatchResultByIdJob: Job? = null
    private var loadMatchResultsForTeamJob: Job? = null
    private var reportResultJob: Job? = null
    private var createMatchResultJob: Job? = null
    private var resolveDisputeJob: Job? = null

    private val _matchResults = MutableStateFlow<List<MatchResult>>(emptyList())
    val matchResults: StateFlow<List<MatchResult>> = _matchResults.asStateFlow()

    private val _selectedMatchResult = MutableStateFlow<MatchResult?>(null)
    val selectedMatchResult: StateFlow<MatchResult?> = _selectedMatchResult.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _isRefreshing = MutableStateFlow(false)
    val isRefreshing: StateFlow<Boolean> = _isRefreshing.asStateFlow()

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error.asStateFlow()

    private val _reportSuccess = MutableStateFlow(false)
    val reportSuccess: StateFlow<Boolean> = _reportSuccess.asStateFlow()

    init {
        loadMatchResults()
    }

    fun loadMatchResults(isRefresh: Boolean = false) {
        loadMatchResultsJob?.cancel()
        loadMatchResultsJob = viewModelScope.launch {
            if (isRefresh) _isRefreshing.value = true
            _isLoading.value = true
            _error.value = null

            matchResultRepository.getAllMatchResults().collect { result ->
                result.onSuccess { list ->
                    _matchResults.value = list
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

    fun loadMatchResultById(id: String) {
        loadMatchResultByIdJob?.cancel()
        loadMatchResultByIdJob = viewModelScope.launch {
            _isLoading.value = true
            _error.value = null

            matchResultRepository.getMatchResultById(id).collect { result ->
                result.onSuccess { matchResult ->
                    _selectedMatchResult.value = matchResult
                    _isLoading.value = false
                }.onFailure { exception ->
                    _error.value = exception.message
                    _isLoading.value = false
                }
            }
        }
    }

    fun loadMatchResultsForTeam(teamId: String) {
        loadMatchResultsForTeamJob?.cancel()
        loadMatchResultsForTeamJob = viewModelScope.launch {
            _isLoading.value = true
            _error.value = null

            matchResultRepository.getMatchResultsForTeam(teamId).collect { result ->
                result.onSuccess { list ->
                    _matchResults.value = list
                    _isLoading.value = false
                }.onFailure { exception ->
                    _error.value = exception.message
                    _isLoading.value = false
                }
            }
        }
    }

    fun reportResult(
        scrimId: String,
        teamId: String,
        reporterId: String,
        reporterName: String,
        reportedWinnerId: String,
        notes: String? = null,
        screenshotUrl: String? = null
    ) {
        reportResultJob?.cancel()
        reportResultJob = viewModelScope.launch {
            _isLoading.value = true
            _error.value = null
            _reportSuccess.value = false

            matchResultRepository.reportResult(
                scrimId = scrimId,
                teamId = teamId,
                reporterId = reporterId,
                reporterName = reporterName,
                reportedWinnerId = reportedWinnerId,
                notes = notes,
                screenshotUrl = screenshotUrl
            ).collect { result ->
                result.onSuccess { matchResult ->
                    _selectedMatchResult.value = matchResult
                    _reportSuccess.value = true
                    loadMatchResults()
                    _isLoading.value = false
                }.onFailure { exception ->
                    _error.value = exception.message
                    _isLoading.value = false
                }
            }
        }
    }

    fun createMatchResult(
        scrimId: String,
        teamAId: String,
        teamAName: String,
        teamBId: String,
        teamBName: String
    ) {
        createMatchResultJob?.cancel()
        createMatchResultJob = viewModelScope.launch {
            _isLoading.value = true
            _error.value = null

            matchResultRepository.createMatchResult(
                scrimId = scrimId,
                teamAId = teamAId,
                teamAName = teamAName,
                teamBId = teamBId,
                teamBName = teamBName
            ).collect { result ->
                result.onSuccess { matchResult ->
                    _selectedMatchResult.value = matchResult
                    loadMatchResults()
                    _isLoading.value = false
                }.onFailure { exception ->
                    _error.value = exception.message
                    _isLoading.value = false
                }
            }
        }
    }

    fun resolveDispute(
        matchResultId: String,
        confirmedWinnerId: String,
        adminNotes: String? = null
    ) {
        resolveDisputeJob?.cancel()
        resolveDisputeJob = viewModelScope.launch {
            _isLoading.value = true
            _error.value = null

            matchResultRepository.resolveDispute(
                matchResultId = matchResultId,
                confirmedWinnerId = confirmedWinnerId,
                adminNotes = adminNotes
            ).collect { result ->
                result.onSuccess { matchResult ->
                    _selectedMatchResult.value = matchResult
                    loadMatchResults()
                    _isLoading.value = false
                }.onFailure { exception ->
                    _error.value = exception.message
                    _isLoading.value = false
                }
            }
        }
    }

    fun clearSelectedMatchResult() {
        _selectedMatchResult.value = null
    }

    fun clearError() {
        _error.value = null
    }

    fun clearRefreshing() {
        _isRefreshing.value = false
    }

    fun clearReportSuccess() {
        _reportSuccess.value = false
    }

    override fun onCleared() {
        super.onCleared()
        loadMatchResultsJob?.cancel()
        loadMatchResultByIdJob?.cancel()
        loadMatchResultsForTeamJob?.cancel()
        reportResultJob?.cancel()
        createMatchResultJob?.cancel()
        resolveDisputeJob?.cancel()
    }
}
