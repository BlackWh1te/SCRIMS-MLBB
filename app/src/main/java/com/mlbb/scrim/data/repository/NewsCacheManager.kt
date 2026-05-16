package com.mlbb.scrim.data.repository

import android.content.Context
import android.util.Log
import com.google.gson.Gson
import com.google.gson.GsonBuilder
import com.google.gson.reflect.TypeToken
import com.mlbb.scrim.data.model.NewsArticle
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

/**
 * Local file-based cache for news articles on the Android device.
 *
 * Purpose: DDoS protection for the backend proxy + faster repeat loads.
 * Once a user fetches news, it's stored locally for [CACHE_TTL_MS].
 * Subsequent loads within that window come from disk — zero network requests.
 */
class NewsCacheManager(private val context: Context) {

    companion object {
        private const val TAG = "NewsCacheManager"
        private const val CACHE_FILE_NAME = "news_cache_v1.json"

        /** Cache valid for 2 hours. Prevents spam-reloading from hitting the server. */
        const val CACHE_TTL_MS = 2L * 60 * 60 * 1000 // 2 hours

        /** Max cached articles to keep file size reasonable. */
        const val MAX_CACHED_ARTICLES = 100
    }

    private val gson: Gson = GsonBuilder()
        .setLenient()
        .create()

    private val cacheFile: File by lazy {
        File(context.filesDir, CACHE_FILE_NAME)
    }

    data class CachedNews(
        val articles: List<NewsArticle> = emptyList(),
        val cachedAt: Long = 0L,
        val source: String = ""
    )

    /**
     * Load cached articles if they exist and are not expired.
     *
     * @param maxAgeMs Maximum age in ms to consider cache valid (default 2h)
     * @return Cached articles if valid, empty list otherwise
     */
    suspend fun loadCache(maxAgeMs: Long = CACHE_TTL_MS): List<NewsArticle> {
        return withContext(Dispatchers.IO) {
            try {
                if (!cacheFile.exists()) {
                    Log.d(TAG, "No local cache file yet")
                    return@withContext emptyList<NewsArticle>()
                }

                val json = cacheFile.readText()
                val type = object : TypeToken<CachedNews>() {}.type
                val cached = gson.fromJson<CachedNews>(json, type)

                if (cached == null || cached.articles.isEmpty()) {
                    Log.d(TAG, "Local cache empty or corrupt")
                    return@withContext emptyList<NewsArticle>()
                }

                val age = System.currentTimeMillis() - cached.cachedAt
                if (age > maxAgeMs) {
                    Log.d(TAG, "Local cache expired (${age / 1000 / 60} min old > ${maxAgeMs / 1000 / 60} min TTL)")
                    return@withContext emptyList<NewsArticle>()
                }

                Log.d(TAG, "Local cache hit: ${cached.articles.size} articles, ${age / 1000 / 60} min old")
                cached.articles
            } catch (e: Exception) {
                Log.w(TAG, "Failed to load local cache", e)
                emptyList()
            }
        }
    }

    /**
     * Save articles to local cache file.
     */
    suspend fun saveCache(articles: List<NewsArticle>, source: String = "proxy") {
        withContext(Dispatchers.IO) {
            try {
                val trimmed = articles.take(MAX_CACHED_ARTICLES)
                val cached = CachedNews(
                    articles = trimmed,
                    cachedAt = System.currentTimeMillis(),
                    source = source
                )
                val json = gson.toJson(cached)
                cacheFile.writeText(json)
                Log.d(TAG, "Saved ${trimmed.size} articles to local cache")
            } catch (e: Exception) {
                Log.w(TAG, "Failed to save local cache", e)
            }
        }
    }

    /**
     * Clear the local cache file.
     */
    suspend fun clearCache() {
        withContext(Dispatchers.IO) {
            try {
                if (cacheFile.exists()) {
                    cacheFile.delete()
                    Log.d(TAG, "Local cache cleared")
                } else {
                    Log.d(TAG, "No local cache to clear")
                }
            } catch (e: Exception) {
                Log.w(TAG, "Failed to clear local cache", e)
            }
        }
    }

    /**
     * Check if a valid cache exists (for UI badges / quick checks).
     */
    suspend fun hasValidCache(maxAgeMs: Long = CACHE_TTL_MS): Boolean {
        return loadCache(maxAgeMs).isNotEmpty()
    }

    /**
     * Get cache age in minutes for display/debug.
     */
    suspend fun getCacheAgeMinutes(): Int {
        return withContext(Dispatchers.IO) {
            try {
                if (!cacheFile.exists()) return@withContext -1
                val json = cacheFile.readText()
                val type = object : TypeToken<CachedNews>() {}.type
                val cached = gson.fromJson<CachedNews>(json, type) ?: return@withContext -1
                val age = System.currentTimeMillis() - cached.cachedAt
                (age / 60000).toInt()
            } catch (e: Exception) {
                -1
            }
        }
    }
}
