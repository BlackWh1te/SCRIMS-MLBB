package com.mlbb.scrim.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mlbb.scrim.data.model.GameRole
import com.mlbb.scrim.data.model.LfgPost
import com.mlbb.scrim.data.model.Region
import com.mlbb.scrim.data.model.SkillLevel
import com.mlbb.scrim.data.repository.LfgRepositoryInterface
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.util.UUID
import javax.inject.Inject

@HiltViewModel
class LfgViewModel @Inject constructor(
    private val lfgRepository: LfgRepositoryInterface
) : ViewModel() {

    private val _posts = MutableStateFlow<List<LfgPost>>(emptyList())
    val posts: StateFlow<List<LfgPost>> = _posts.asStateFlow()

    private val _isLoading = MutableStateFlow(true)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _isRefreshing = MutableStateFlow(false)
    val isRefreshing: StateFlow<Boolean> = _isRefreshing.asStateFlow()

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error.asStateFlow()

    init {
        loadPosts()
    }

    fun loadPosts(isRefresh: Boolean = false) {
        viewModelScope.launch {
            if (isRefresh) _isRefreshing.value = true
            _isLoading.value = true
            _error.value = null
            
            lfgRepository.getAllPosts().collect { result ->
                result.onSuccess { list ->
                    _posts.value = list
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

    fun addPost(
        playerId: String,
        playerName: String,
        role: GameRole,
        region: Region,
        skillLevel: SkillLevel,
        message: String,
        mainHeroes: List<String> = emptyList(),
        bio: String = "",
        rank: String = "",
        totalMatches: Int = 0,
        winRate: String = "",
        rankedWinRate: String = "",
        inGameId: String = "",
        city: String = "",
        screenshotUrl: String = "",
        useMic: Boolean = false,
        playstyleTags: List<String> = emptyList(),
        discord: String = "",
        telegram: String = "",
        vk: String = "",
        facebook: String = ""
    ) {
        viewModelScope.launch {
            val post = LfgPost(
                id = UUID.randomUUID().toString(),
                playerId = playerId,
                playerName = playerName,
                role = role,
                region = region,
                skillLevel = skillLevel,
                message = message,
                mainHeroes = mainHeroes,
                bio = bio,
                rank = rank,
                totalMatches = totalMatches,
                winRate = winRate,
                rankedWinRate = rankedWinRate,
                inGameId = inGameId,
                city = city,
                screenshotUrl = screenshotUrl,
                useMic = useMic,
                playstyleTags = playstyleTags,
                discord = discord,
                telegram = telegram,
                vk = vk,
                facebook = facebook,
                createdAt = System.currentTimeMillis()
            )

            // Optimistically add to local list so it appears instantly
            _posts.value = _posts.value + post

            _isLoading.value = true
            lfgRepository.createPost(post).collect { result ->
                result.onSuccess { created ->
                    // Replace the optimistic post with the real one from server
                    _posts.value = _posts.value.map {
                        if (it.id == post.id) created else it
                    }
                    // Also do a full refresh to get the complete list
                    loadPosts()
                }.onFailure { exception ->
                    // Remove the optimistic post on failure
                    _posts.value = _posts.value.filter { it.id != post.id }
                    _error.value = exception.message
                    _isLoading.value = false
                }
            }
        }
    }

    fun deletePost(postId: String) {
        viewModelScope.launch {
            lfgRepository.deletePost(postId).collect { result ->
                result.onSuccess {
                    loadPosts()
                }
            }
        }
    }

    fun clearError() { _error.value = null }

    fun clearRefreshing() { _isRefreshing.value = false }
}
