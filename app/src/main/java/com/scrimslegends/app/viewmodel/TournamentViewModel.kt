package com.scrimslegends.app.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.scrimslegends.app.data.model.*
import com.scrimslegends.app.data.repository.TournamentRepositoryInterface
import com.scrimslegends.app.data.service.SupabaseRealtimeClient
import com.scrimslegends.app.data.service.SupabaseSession
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.toPersistentList
import timber.log.Timber
import javax.inject.Inject

@HiltViewModel
class TournamentViewModel @Inject constructor(
    private val tournamentRepository: TournamentRepositoryInterface,
    private val realtimeClient: SupabaseRealtimeClient
) : ViewModel() {

    // ── Tournament list ──
    private val _tournaments = MutableStateFlow<ImmutableList<Tournament>>(persistentListOf())
    val tournaments: StateFlow<ImmutableList<Tournament>> = _tournaments.asStateFlow()

    // ── Hosted tournaments (filtered from main list) ──
    private val _hostedTournaments = MutableStateFlow<ImmutableList<Tournament>>(persistentListOf())
    val hostedTournaments: StateFlow<ImmutableList<Tournament>> = _hostedTournaments.asStateFlow()

    // ── Selected tournament detail ──
    private val _selectedTournament = MutableStateFlow<Tournament?>(null)
    val selectedTournament: StateFlow<Tournament?> = _selectedTournament.asStateFlow()

    // ── Tournament requirements ──
    private val _requirements = MutableStateFlow<ImmutableList<TournamentRequirement>>(persistentListOf())
    val requirements: StateFlow<ImmutableList<TournamentRequirement>> = _requirements.asStateFlow()

    // ── Tournament teams (standings) ──
    private val _tournamentTeams = MutableStateFlow<ImmutableList<TournamentTeam>>(persistentListOf())
    val tournamentTeams: StateFlow<ImmutableList<TournamentTeam>> = _tournamentTeams.asStateFlow()

    // ── Swiss matches ──
    private val _matches = MutableStateFlow<ImmutableList<TournamentSwissMatch>>(persistentListOf())
    val matches: StateFlow<ImmutableList<TournamentSwissMatch>> = _matches.asStateFlow()

    // ── My applications ──
    private val _myApplications = MutableStateFlow<ImmutableList<TournamentApplication>>(persistentListOf())
    val myApplications: StateFlow<ImmutableList<TournamentApplication>> = _myApplications.asStateFlow()

    // ── My host request ──
    private val _myHostRequest = MutableStateFlow<TournamentHostRequest?>(null)
    val myHostRequest: StateFlow<TournamentHostRequest?> = _myHostRequest.asStateFlow()

    // ── Match roster ──
    private val _matchRoster = MutableStateFlow<ImmutableList<TournamentMatchRoster>>(persistentListOf())
    val matchRoster: StateFlow<ImmutableList<TournamentMatchRoster>> = _matchRoster.asStateFlow()

    // ── Tournament player stats ──
    private val _playerStats = MutableStateFlow<ImmutableList<TournamentPlayerStats>>(persistentListOf())
    val playerStats: StateFlow<ImmutableList<TournamentPlayerStats>> = _playerStats.asStateFlow()

    // ── Room secret ──
    private val _roomSecret = MutableStateFlow<TournamentMatchRoomSecret?>(null)
    val roomSecret: StateFlow<TournamentMatchRoomSecret?> = _roomSecret.asStateFlow()

    // ── UI state ──
    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _isRefreshing = MutableStateFlow(false)
    val isRefreshing: StateFlow<Boolean> = _isRefreshing.asStateFlow()

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error.asStateFlow()

    // ── One-shot action results ──
    private val _applyResult = MutableStateFlow<Result<Map<String, Any>>?>(null)
    val applyResult: StateFlow<Result<Map<String, Any>>?> = _applyResult.asStateFlow()

    private val _hostRequestResult = MutableStateFlow<Result<TournamentHostRequest>?>(null)
    val hostRequestResult: StateFlow<Result<TournamentHostRequest>?> = _hostRequestResult.asStateFlow()

    private val _createResult = MutableStateFlow<Result<Tournament>?>(null)
    val createResult: StateFlow<Result<Tournament>?> = _createResult.asStateFlow()

    private val _updateResult = MutableStateFlow<Result<Tournament>?>(null)
    val updateResult: StateFlow<Result<Tournament>?> = _updateResult.asStateFlow()

    // ── Pending logo upload (set by navigation before createTournament is called) ──
    var pendingLogoBytes: ByteArray? = null
    var pendingLogoMime: String? = null

    fun clearPendingLogo() {
        pendingLogoBytes = null
        pendingLogoMime = null
    }

    // ── Filters ──
    private var loadTournamentsJob: Job? = null
    private var loadTournamentJob: Job? = null
    private var loadMatchesJob: Job? = null
    private var loadApplicationsJob: Job? = null
    private var loadHostRequestJob: Job? = null
    private var realtimeMatchesJob: Job? = null
    private var realtimeTeamsJob: Job? = null
    private var realtimeTournamentId: String? = null

    // ── Filter state ──
    private val _statusFilter = MutableStateFlow<String?>(null)
    val statusFilter: StateFlow<String?> = _statusFilter.asStateFlow()

    private val _regionFilter = MutableStateFlow<String?>(null)
    val regionFilter: StateFlow<String?> = _regionFilter.asStateFlow()

    private val _skillLevelFilter = MutableStateFlow<String?>(null)
    val skillLevelFilter: StateFlow<String?> = _skillLevelFilter.asStateFlow()

    // ── User's team IDs (set from navigation layer) ──
    private val _myTeamIds = MutableStateFlow<ImmutableList<String>>(persistentListOf())
    val myTeamIds: StateFlow<ImmutableList<String>> = _myTeamIds.asStateFlow()

    fun setMyTeamIds(teamIds: List<String>) {
        _myTeamIds.value = teamIds.toPersistentList()
        // Re-apply isMyMatch flag to existing matches
        if (_matches.value.isNotEmpty()) {
            _matches.value = _matches.value.map { match ->
                match.copy(isMyMatch = match.teamAId in teamIds || match.teamBId in teamIds)
            }.toPersistentList()
        }
    }

    // ═══════════════════════════════════════════════════════════════
    // TOURNAMENT LIST
    // ═══════════════════════════════════════════════════════════════

    fun loadTournaments(isRefresh: Boolean = false) {
        loadTournamentsJob?.cancel()
        loadTournamentsJob = viewModelScope.launch {
            if (isRefresh) _isRefreshing.value = true
            _isLoading.value = true
            _error.value = null

            tournamentRepository.getTournaments(
                status = _statusFilter.value,
                region = _regionFilter.value,
                skillLevel = _skillLevelFilter.value
            ).onSuccess { list ->
                _tournaments.value = list.toPersistentList()
                val userId = SupabaseSession.getUserIdOrNull()
                _hostedTournaments.value = (if (userId != null) list.filter { it.hostUserId == userId } else emptyList()).toPersistentList()
                _isLoading.value = false
                _isRefreshing.value = false
            }.onFailure { e ->
                _error.value = e.message
                _isLoading.value = false
                _isRefreshing.value = false
            }
        }
    }

    fun setStatusFilter(status: String?) {
        _statusFilter.value = status
        loadTournaments()
    }

    fun setRegionFilter(region: String?) {
        _regionFilter.value = region
        loadTournaments()
    }

    fun setSkillLevelFilter(skillLevel: String?) {
        _skillLevelFilter.value = skillLevel
        loadTournaments()
    }

    // ═══════════════════════════════════════════════════════════════
    // TOURNAMENT DETAIL
    // ═══════════════════════════════════════════════════════════════

    fun loadTournamentById(tournamentId: String) {
        loadTournamentJob?.cancel()
        loadTournamentJob = viewModelScope.launch {
            _isLoading.value = true
            _error.value = null

            tournamentRepository.getTournamentById(tournamentId)
                .onSuccess { tournament ->
                    _selectedTournament.value = tournament
                    _isLoading.value = false
                    // Also load requirements, teams, matches, applications, and player stats
                    loadRequirements(tournamentId)
                    loadTournamentTeams(tournamentId)
                    loadTournamentMatches(tournamentId)
                    loadMyApplications()
                    loadTournamentPlayerStats(tournamentId)
                }.onFailure { e ->
                    _error.value = e.message
                    _isLoading.value = false
                }
        }
    }

    private fun loadRequirements(tournamentId: String) {
        viewModelScope.launch {
            tournamentRepository.getTournamentRequirements(tournamentId)
                .onSuccess { _requirements.value = it.toPersistentList() }
                .onFailure { _error.value = it.message }
        }
    }

    private fun loadTournamentTeams(tournamentId: String) {
        viewModelScope.launch {
            tournamentRepository.getTournamentTeams(tournamentId)
                .onSuccess { _tournamentTeams.value = it.toPersistentList() }
                .onFailure { _error.value = it.message }
        }
    }

    // ═══════════════════════════════════════════════════════════════
    // APPLICATIONS
    // ═══════════════════════════════════════════════════════════════

    fun loadMyApplications() {
        val userId = SupabaseSession.getUserIdOrNull() ?: return
        loadApplicationsJob?.cancel()
        loadApplicationsJob = viewModelScope.launch {
            _isLoading.value = true
            tournamentRepository.getMyApplications(userId)
                .onSuccess { apps ->
                    _myApplications.value = apps.toPersistentList()
                    _isLoading.value = false
                }.onFailure { e ->
                    _error.value = e.message
                    _isLoading.value = false
                }
        }
    }

    fun applyForTournament(tournamentId: String, teamId: String) {
        viewModelScope.launch {
            _isLoading.value = true
            _applyResult.value = null

            tournamentRepository.applyForTournament(tournamentId, teamId)
                .onSuccess { result ->
                    _applyResult.value = Result.success(result)
                    _isLoading.value = false
                    // Refresh applications
                    loadMyApplications()
                }.onFailure { e ->
                    _applyResult.value = Result.failure(e)
                    _error.value = e.message
                    _isLoading.value = false
                }
        }
    }

    // ═══════════════════════════════════════════════════════════════
    // HOST REQUEST
    // ═══════════════════════════════════════════════════════════════

    fun loadMyHostRequest() {
        val userId = SupabaseSession.getUserIdOrNull() ?: return
        loadHostRequestJob?.cancel()
        loadHostRequestJob = viewModelScope.launch {
            tournamentRepository.getMyHostRequest(userId)
                .onSuccess { _myHostRequest.value = it }
                .onFailure { _error.value = it.message }
        }
    }

    fun submitHostRequest(
        motivation: String,
        experience: String?,
        telegramChannel: String?,
        socialLinks: List<String>
    ) {
        viewModelScope.launch {
            _isLoading.value = true
            _hostRequestResult.value = null

            tournamentRepository.submitHostRequest(motivation, experience, telegramChannel, socialLinks)
                .onSuccess { request ->
                    _myHostRequest.value = request
                    _hostRequestResult.value = Result.success(request)
                    _isLoading.value = false
                }.onFailure { e ->
                    _hostRequestResult.value = Result.failure(e)
                    _error.value = e.message
                    _isLoading.value = false
                }
        }
    }

    // ═══════════════════════════════════════════════════════════════
    // CREATE TOURNAMENT
    // ═══════════════════════════════════════════════════════════════

    fun createTournament(tournament: Tournament, requirements: List<TournamentRequirement> = emptyList()) {
        viewModelScope.launch {
            _isLoading.value = true
            _createResult.value = null

            tournamentRepository.createTournament(tournament)
                .onSuccess { created ->
                    _selectedTournament.value = created
                    var isPartialFailure = false
                    // Insert requirements after the tournament row is committed
                    if (requirements.isNotEmpty()) {
                        tournamentRepository.createRequirements(created.id, requirements)
                            .onFailure { e -> 
                                _error.value = "Tournament created but requirements failed: ${e.message}" 
                                isPartialFailure = true
                            }
                        loadRequirements(created.id)
                    }
                    // Upload logo if provided
                    val logoBytes = pendingLogoBytes
                    if (logoBytes != null) {
                        tournamentRepository.uploadTournamentLogo(created.id, logoBytes, pendingLogoMime ?: "image/jpeg")
                            .onFailure { e -> 
                                _error.value = "Tournament created but logo upload failed: ${e.message}" 
                                isPartialFailure = true
                            }
                        pendingLogoBytes = null
                        pendingLogoMime = null
                    }
                    
                    if (isPartialFailure) {
                        _createResult.value = Result.failure(Exception("Tournament created with partial failures. Please check logo and requirements."))
                    } else {
                        _createResult.value = Result.success(created)
                    }
                    _isLoading.value = false
                    loadTournaments()
                }.onFailure { e ->
                    _createResult.value = Result.failure(e)
                    _error.value = e.message
                    _isLoading.value = false
                    pendingLogoBytes = null
                    pendingLogoMime = null
                }
        }
    }

    fun saveRequirements(tournamentId: String, requirements: List<TournamentRequirement>) {
        viewModelScope.launch {
            _isLoading.value = true
            tournamentRepository.createRequirements(tournamentId, requirements)
                .onSuccess {
                    _isLoading.value = false
                    loadRequirements(tournamentId)
                }.onFailure { e ->
                    _error.value = e.message
                    _isLoading.value = false
                }
        }
    }

    fun deleteRequirement(requirementId: String, tournamentId: String) {
        viewModelScope.launch {
            tournamentRepository.deleteRequirement(requirementId)
                .onSuccess { loadRequirements(tournamentId) }
                .onFailure { e -> _error.value = e.message }
        }
    }

    // ═══════════════════════════════════════════════════════════════
    // UPDATE TOURNAMENT (host only, registration phase)
    // ═══════════════════════════════════════════════════════════════

    fun updateTournament(tournamentId: String, updates: Map<String, Any?>) {
        viewModelScope.launch {
            _isLoading.value = true
            _updateResult.value = null
            _error.value = null

            tournamentRepository.updateTournament(tournamentId, updates)
                .onSuccess { updated ->
                    _selectedTournament.value = updated
                    _updateResult.value = Result.success(updated)
                    _isLoading.value = false
                    loadTournaments() // Refresh list
                }.onFailure { e ->
                    _updateResult.value = Result.failure(e)
                    _error.value = e.message
                    _isLoading.value = false
                }
        }
    }

    // ═══════════════════════════════════════════════════════════════
    // SWISS MATCHES
    // ═══════════════════════════════════════════════════════════════

    fun loadTournamentMatches(tournamentId: String) {
        loadMatchesJob?.cancel()
        loadMatchesJob = viewModelScope.launch {
            _isLoading.value = true
            tournamentRepository.getTournamentMatches(tournamentId)
                .onSuccess { 
                    val myIds = _myTeamIds.value
                    _matches.value = it.map { match ->
                        match.copy(isMyMatch = match.teamAId in myIds || match.teamBId in myIds)
                    }.toPersistentList()
                    _isLoading.value = false
                }.onFailure { e ->
                    _error.value = e.message
                    _isLoading.value = false
                }
        }
    }

    fun loadTournamentPlayerStats(tournamentId: String) {
        viewModelScope.launch {
            tournamentRepository.getTournamentPlayerStats(tournamentId)
                .onSuccess { _playerStats.value = it.toPersistentList() }
                .onFailure { _error.value = it.message }
        }
    }

    // ═══════════════════════════════════════════════════════════════
    // MATCH ROSTER
    // ═══════════════════════════════════════════════════════════════

    fun loadMatchRoster(matchId: String, teamId: String, gameNumber: Int = 1) {
        viewModelScope.launch {
            tournamentRepository.getMatchRoster(matchId, teamId, gameNumber)
                .onSuccess { _matchRoster.value = it.toPersistentList() }
                .onFailure { _error.value = it.message }
        }
    }

    fun setMatchRoster(matchId: String, teamId: String, gameNumber: Int, playerIds: List<String>) {
        viewModelScope.launch {
            _isLoading.value = true
            tournamentRepository.setMatchRoster(matchId, teamId, gameNumber, playerIds)
                .onSuccess { _isLoading.value = false }
                .onFailure { e ->
                    _error.value = e.message
                    _isLoading.value = false
                }
        }
    }

    // ═══════════════════════════════════════════════════════════════
    // ROOM SECRET
    // ═══════════════════════════════════════════════════════════════

    fun loadRoomSecret(matchId: String) {
        viewModelScope.launch {
            tournamentRepository.getMatchRoomSecret(matchId)
                .onSuccess { _roomSecret.value = it }
                .onFailure { _error.value = it.message }
        }
    }

    // ═══════════════════════════════════════════════════════════════
    // HELPERS
    // ═══════════════════════════════════════════════════════════════

    fun clearError() {
        _error.value = null
    }

    fun clearApplyResult() {
        _applyResult.value = null
    }

    fun clearCreateResult() {
        _createResult.value = null
    }

    fun clearUpdateResult() {
        _updateResult.value = null
    }

    fun clearHostRequestResult() {
        _hostRequestResult.value = null
    }

    // ═══════════════════════════════════════════════════════════════
    // REALTIME — tournament_swiss_matches + tournament_teams
    // ═══════════════════════════════════════════════════════════════

    fun startTournamentRealtime(tournamentId: String) {
        if (realtimeTournamentId == tournamentId) return // already subscribed
        stopTournamentRealtime()
        realtimeTournamentId = tournamentId

        // Subscribe to match status / result changes
        realtimeMatchesJob = viewModelScope.launch {
            try {
                realtimeClient.subscribeToTable(
                    table       = "tournament_swiss_matches",
                    event       = "*",
                    filter      = "tournament_id=eq.$tournamentId",
                    channelName = "tournament_matches_$tournamentId"
                ).collect {
                    Timber.d("Realtime: tournament_swiss_matches event for $tournamentId")
                    loadTournamentMatches(tournamentId)
                }
            } catch (e: Exception) {
                if (e is kotlinx.coroutines.CancellationException) throw e
                Timber.w("Tournament matches realtime error: ${e.message}")
            }
        }

        // Subscribe to team standings changes (check-in, points, disqualification)
        realtimeTeamsJob = viewModelScope.launch {
            try {
                realtimeClient.subscribeToTable(
                    table       = "tournament_teams",
                    event       = "*",
                    filter      = "tournament_id=eq.$tournamentId",
                    channelName = "tournament_teams_$tournamentId"
                ).collect {
                    Timber.d("Realtime: tournament_teams event for $tournamentId")
                    loadTournamentTeams(tournamentId)
                }
            } catch (e: Exception) {
                if (e is kotlinx.coroutines.CancellationException) throw e
                Timber.w("Tournament teams realtime error: ${e.message}")
            }
        }
    }

    fun stopTournamentRealtime() {
        realtimeTournamentId?.let { id ->
            realtimeClient.unsubscribe("tournament_matches_$id")
            realtimeClient.unsubscribe("tournament_teams_$id")
        }
        realtimeMatchesJob?.cancel()
        realtimeTeamsJob?.cancel()
        realtimeMatchesJob = null
        realtimeTeamsJob = null
        realtimeTournamentId = null
    }

    override fun onCleared() {
        super.onCleared()
        loadTournamentsJob?.cancel()
        loadTournamentJob?.cancel()
        loadMatchesJob?.cancel()
        loadApplicationsJob?.cancel()
        loadHostRequestJob?.cancel()
        stopTournamentRealtime()
    }

    // ═══════════════════════════════════════════════════════════════
    // SWISS PAIRING & TOURNAMENT MANAGEMENT (HOST ACTIONS)
    // ═══════════════════════════════════════════════════════════════

    fun generateSwissPairings(tournamentId: String) {
        viewModelScope.launch {
            _isLoading.value = true
            _error.value = null
            tournamentRepository.generateSwissPairings(tournamentId)
                .onSuccess {
                    _isLoading.value = false
                    loadTournamentById(tournamentId)
                    loadTournamentMatches(tournamentId)
                }.onFailure { e ->
                    _error.value = e.message
                    _isLoading.value = false
                }
        }
    }

    fun submitMatchResult(matchId: String, winnerTeamId: String?, isDraw: Boolean, gameAScore: Int = 0, gameBScore: Int = 0) {
        viewModelScope.launch {
            _isLoading.value = true
            _error.value = null
            tournamentRepository.submitMatchResult(matchId, winnerTeamId, isDraw, gameAScore, gameBScore)
                .onSuccess {
                    // Refresh the current tournament data
                    _selectedTournament.value?.let { loadTournamentById(it.id) }
                }.onFailure { e ->
                    _error.value = e.message
                    _isLoading.value = false
                }
        }
    }

    fun awardMatchPoints(matchId: String) {
        viewModelScope.launch {
            _error.value = null
            tournamentRepository.awardMatchPoints(matchId)
                .onFailure { e -> _error.value = e.message }
        }
    }

    fun updateTournamentScores(tournamentId: String) {
        viewModelScope.launch {
            _error.value = null
            tournamentRepository.updateTournamentScores(tournamentId)
                .onSuccess { loadTournamentById(tournamentId) }
                .onFailure { e -> _error.value = e.message }
        }
    }

    fun recalculateTiebreakers(tournamentId: String) {
        viewModelScope.launch {
            _error.value = null
            tournamentRepository.recalculateTiebreakers(tournamentId)
                .onSuccess { loadTournamentById(tournamentId) }
                .onFailure { e -> _error.value = e.message }
        }
    }

    fun disqualifyTeam(tournamentId: String, teamId: String, reason: String) {
        viewModelScope.launch {
            _isLoading.value = true
            _error.value = null
            tournamentRepository.disqualifyTeam(tournamentId, teamId, reason)
                .onSuccess {
                    _isLoading.value = false
                    loadTournamentById(tournamentId)
                }.onFailure { e ->
                    _error.value = e.message
                    _isLoading.value = false
                }
        }
    }

    fun checkNoShows(tournamentId: String) {
        viewModelScope.launch {
            _error.value = null
            tournamentRepository.checkNoShows(tournamentId)
                .onFailure { e -> _error.value = e.message }
        }
    }

    fun cancelTournament(tournamentId: String, reason: String? = null) {
        viewModelScope.launch {
            _isLoading.value = true
            _error.value = null
            tournamentRepository.cancelTournament(tournamentId, reason)
                .onSuccess {
                    _isLoading.value = false
                    loadTournamentById(tournamentId)
                    loadTournaments()
                }.onFailure { e ->
                    _error.value = e.message
                    _isLoading.value = false
                }
        }
    }

    fun uploadTournamentLogo(tournamentId: String, fileBytes: ByteArray, contentType: String) {
        viewModelScope.launch {
            _isLoading.value = true
            _error.value = null
            tournamentRepository.uploadTournamentLogo(tournamentId, fileBytes, contentType)
                .onSuccess {
                    _isLoading.value = false
                    loadTournamentById(tournamentId)
                }.onFailure { e ->
                    _error.value = "Logo upload failed: ${e.message}"
                    _isLoading.value = false
                }
        }
    }

    fun resolveDispute(matchId: String, winnerTeamId: String?, isDraw: Boolean, resolution: String) {
        viewModelScope.launch {
            _isLoading.value = true
            _error.value = null
            tournamentRepository.resolveDispute(matchId, winnerTeamId, isDraw, resolution)
                .onSuccess {
                    _isLoading.value = false
                    _selectedTournament.value?.let { loadTournamentById(it.id) }
                }.onFailure { e ->
                    _error.value = e.message
                    _isLoading.value = false
                }
        }
    }

    fun scheduleMatch(matchId: String, scheduledAt: Long) {
        viewModelScope.launch {
            _isLoading.value = true
            _error.value = null
            tournamentRepository.scheduleMatch(matchId, scheduledAt)
                .onSuccess {
                    _isLoading.value = false
                    _selectedTournament.value?.let { loadTournamentById(it.id) }
                }.onFailure { e ->
                    _error.value = e.message
                    _isLoading.value = false
                }
        }
    }

    fun checkInTeam(tournamentId: String, teamId: String) {
        viewModelScope.launch {
            _isLoading.value = true
            _error.value = null
            tournamentRepository.checkInTeam(tournamentId, teamId)
                .onSuccess {
                    _isLoading.value = false
                    loadTournamentById(tournamentId)
                }.onFailure { e ->
                    _error.value = e.message
                    _isLoading.value = false
                }
        }
    }

    fun completeTournament(tournamentId: String) {
        viewModelScope.launch {
            _isLoading.value = true
            _error.value = null
            tournamentRepository.completeTournament(tournamentId)
                .onSuccess {
                    _isLoading.value = false
                    loadTournamentById(tournamentId)
                    loadTournaments()
                }.onFailure { e ->
                    _error.value = e.message
                    _isLoading.value = false
                }
        }
    }

    fun reviewApplication(applicationId: String, approved: Boolean, rejectionReason: String?) {
        viewModelScope.launch {
            _isLoading.value = true
            _error.value = null
            tournamentRepository.reviewApplication(applicationId, approved, rejectionReason)
                .onSuccess {
                    _isLoading.value = false
                    _selectedTournament.value?.let { loadTournamentById(it.id) }
                }.onFailure { e ->
                    _error.value = e.message
                    _isLoading.value = false
                }
        }
    }
}
