package com.mlbb.scrim.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.mlbb.scrim.data.model.NewsArticle
import com.mlbb.scrim.data.preferences.AppSettings
import com.mlbb.scrim.data.repository.NewsRepository
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.launch

class NewsViewModel(application: Application) : AndroidViewModel(application) {

    private val newsRepository = NewsRepository(application)
    private val appSettings = AppSettings(application)

    private val _articles = MutableStateFlow<List<NewsArticle>>(emptyList())
    val articles: StateFlow<List<NewsArticle>> = _articles.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error.asStateFlow()

    private val _selectedArticle = MutableStateFlow<NewsArticle?>(null)
    val selectedArticle: StateFlow<NewsArticle?> = _selectedArticle.asStateFlow()

    private val _quotaInfo = MutableStateFlow(QuotaInfo(0, 100, true))
    val quotaInfo: StateFlow<QuotaInfo> = _quotaInfo.asStateFlow()

    private val _throttleInfo = MutableStateFlow<ThrottleInfo?>(null)
    val throttleInfo: StateFlow<ThrottleInfo?> = _throttleInfo.asStateFlow()

    private val _dripInfo = MutableStateFlow(DripInfo(0, 0, 0))
    val dripInfo: StateFlow<DripInfo> = _dripInfo.asStateFlow()

    private var currentLanguage: String = "en"
    private var loadJob: Job? = null

    init {
        loadQuotaInfo()
        loadDripInfo()
        // loadNews() is NOT called here — we only fetch when NewsScreen is visible
    }

    data class QuotaInfo(
        val used: Int,
        val limit: Int,
        val canUseX: Boolean
    ) {
        val remaining: Int = limit - used
        val percentUsed: Float = if (limit > 0) used.toFloat() / limit else 0f
    }

    data class ThrottleInfo(
        val minutesUntilRefresh: Int
    )

    data class DripInfo(
        val unlocked: Int,      // articles user can see
        val total: Int,         // articles in archive
        val unseen: Int         // articles locked behind drip timer
    )

    fun loadNews(languageCode: String = currentLanguage, forceRefresh: Boolean = false) {
        currentLanguage = languageCode
        loadJob?.cancel()
        loadJob = viewModelScope.launch {
            _isLoading.value = true
            _error.value = null

            newsRepository.getNews(forceRefresh = forceRefresh, targetLanguage = languageCode)
                .onStart { _isLoading.value = true }
                .catch { exception ->
                    _error.value = exception.message
                    _isLoading.value = false
                }
                .collect { result ->
                    result.onSuccess { refreshResult ->
                        _articles.value = refreshResult.articles
                        _throttleInfo.value = if (refreshResult.wasThrottled) {
                            ThrottleInfo(minutesUntilRefresh = refreshResult.minutesUntilRefresh)
                        } else null
                        _isLoading.value = false
                    }.onFailure { exception ->
                        _error.value = exception.message
                        _isLoading.value = false
                    }
                }
        }
    }

    fun refresh(languageCode: String = currentLanguage) {
        loadNews(languageCode, forceRefresh = true)
        loadQuotaInfo()
        loadDripInfo()
    }

    private fun loadQuotaInfo() {
        viewModelScope.launch {
            val used = appSettings.xApiRequestsUsed.first()
            val limit = 100
            val canUse = used < limit
            _quotaInfo.value = QuotaInfo(used, limit, canUse)
        }
    }

    private fun loadDripInfo() {
        viewModelScope.launch {
            val index = appSettings.newsDripIndex.first()
            val total = appSettings.newsDripCountTotal.first()
            _dripInfo.value = DripInfo(
                unlocked = index,
                total = total,
                unseen = (total - index).coerceAtLeast(0)
            )
        }
    }

    fun selectArticle(article: NewsArticle) {
        _selectedArticle.value = article
    }

    fun clearSelectedArticle() {
        _selectedArticle.value = null
    }

    fun clearError() {
        _error.value = null
    }

    override fun onCleared() {
        super.onCleared()
        loadJob?.cancel()
        newsRepository.close()
    }
}
