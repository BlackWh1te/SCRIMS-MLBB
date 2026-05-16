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

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error.asStateFlow()

    init {
        loadPosts()
    }

    fun loadPosts() {
        viewModelScope.launch {
            _isLoading.value = true
            _error.value = null
            
            lfgRepository.getAllPosts().collect { result ->
                result.onSuccess { list ->
                    _posts.value = list
                    _isLoading.value = false
                }.onFailure { exception ->
                    _error.value = exception.message
                    _isLoading.value = false
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
        useMic: Boolean = false,
        playstyleTags: List<String> = emptyList(),
        discord: String = "",
        telegram: String = "",
        vk: String = "",
        facebook: String = ""
    ) {
        viewModelScope.launch {
            _isLoading.value = true
            val post = LfgPost(
                id = "",
                playerId = playerId,
                playerName = playerName,
                role = role,
                region = region,
                skillLevel = skillLevel,
                message = message,
                mainHeroes = mainHeroes,
                bio = bio,
                useMic = useMic,
                playstyleTags = playstyleTags,
                discord = discord,
                telegram = telegram,
                vk = vk,
                facebook = facebook,
                createdAt = System.currentTimeMillis()
            )
            
            lfgRepository.createPost(post).collect { result ->
                result.onSuccess {
                    loadPosts()
                }.onFailure { exception ->
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
}
