package com.mlbb.scrim.data.service

import com.google.gson.annotations.SerializedName
import com.mlbb.scrim.data.model.NewsArticle
import retrofit2.Response
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.Query
import retrofit2.http.Url

// Reddit API response models (free, no auth needed)
data class RedditListing(
    val data: RedditData? = null
)

data class RedditData(
    val children: List<RedditChild>? = null
)

data class RedditChild(
    val data: RedditPost? = null
)

data class RedditPost(
    val id: String = "",
    val title: String = "",
    @SerializedName("selftext")
    val selfText: String = "",
    val url: String = "",
    @SerializedName("thumbnail")
    val thumbnail: String = "",
    @SerializedName("created_utc")
    val createdUtc: Double = 0.0,
    @SerializedName("subreddit")
    val subreddit: String = "",
    @SerializedName("permalink")
    val permalink: String = ""
)

// NewsAPI.org response models
data class NewsApiResponse(
    @SerializedName("status")
    val status: String = "",
    @SerializedName("articles")
    val articles: List<NewsApiArticle>? = null
)

data class NewsApiArticle(
    @SerializedName("title")
    val title: String = "",
    @SerializedName("description")
    val description: String? = null,
    @SerializedName("content")
    val content: String? = null,
    @SerializedName("url")
    val url: String = "",
    @SerializedName("urlToImage")
    val urlToImage: String? = null,
    @SerializedName("source")
    val source: NewsApiSource? = null,
    @SerializedName("publishedAt")
    val publishedAt: String = ""
)

data class NewsApiSource(
    @SerializedName("name")
    val name: String = ""
)

interface RedditApiService {
    @GET("r/mobilelegends/new.json")
    suspend fun getMobileLegendsPosts(
        @Query("limit") limit: Int = 25,
        @Header("User-Agent") userAgent: String = "MLBBScrimHost/1.0"
    ): Response<RedditListing>
}

interface NewsApiService {
    @GET("v2/everything")
    suspend fun getMobileLegendsNews(
        @Query("q") query: String = "\"Mobile Legends\" OR \"Moonton Games\" OR \"MLBB\" OR \"MPL Philippines\" OR \"MPL Indonesia\" OR \"MPL Malaysia\" OR \"MPL Singapore\" OR \"MPL MENA\" OR \"MPL Brazil\" OR \"MPL LATAM\" OR \"Mobile Legends World Championship\" OR \"MSC Mobile Legends\" OR \"MCL Mobile Legends\" OR \"Mythic Rank MLBB\" OR \"Magic Chess\" OR \"Moonton Tournament\"",
        @Query("language") language: String = "en",
        @Query("sortBy") sortBy: String = "publishedAt",
        @Query("pageSize") pageSize: Int = 50,
        @Query("apiKey") apiKey: String
    ): Response<NewsApiResponse>
}

object NewsApiClient {
    private const val REDDIT_BASE_URL = "https://www.reddit.com/"
    private const val NEWSAPI_BASE_URL = "https://newsapi.org/"

    val redditService: RedditApiService by lazy {
        Retrofit.Builder()
            .baseUrl(REDDIT_BASE_URL)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(RedditApiService::class.java)
    }

    fun createNewsApiService(apiKey: String): NewsApiService {
        return Retrofit.Builder()
            .baseUrl(NEWSAPI_BASE_URL)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(NewsApiService::class.java)
    }
}

// ── Proxy API (backend service) ────────────────────────────────────

/**
 * Response from our MLBB News Proxy API backend.
 * The backend scrapes X/Twitter every 10 hours and serves cached articles
 * with no rate limit to Android clients.
 */
/**
 * Drip-feed response from proxy API.
 * Backend stores all articles in an archive and releases them slowly
 * based on the user's drip offset.
 */
data class ProxyNewsResponse(
    @SerializedName("status")
    val status: String = "",
    @SerializedName("articles")
    val articles: List<NewsArticle> = emptyList(),
    @SerializedName("unlocked")
    val unlocked: Int = 0,
    @SerializedName("totalInArchive")
    val totalInArchive: Int = 0,
    @SerializedName("unseen")
    val unseen: Int = 0,
    @SerializedName("userOffset")
    val userOffset: Int = 0,
    @SerializedName("nextUnlockInMinutes")
    val nextUnlockInMinutes: Int = 0,
    @SerializedName("source")
    val source: String = ""
)

data class ProxyCountResponse(
    @SerializedName("status")
    val status: String = "",
    @SerializedName("total")
    val total: Int = 0
)

data class ProxyHealthResponse(
    @SerializedName("status")
    val status: String = "",
    @SerializedName("totalArticles")
    val totalArticles: Int = 0,
    @SerializedName("scraperIntervalHours")
    val scraperIntervalHours: Int = 0,
    @SerializedName("nextScrapeInMinutes")
    val nextScrapeInMinutes: Int = 0
)

interface ProxyApiService {
    /**
     * Drip-feed endpoint.
     * @param offset user's current drip offset (required)
     * @param limit max articles to return
     */
    @GET("news")
    suspend fun getNews(
        @Query("offset") offset: Int,
        @Query("limit") limit: Int? = null,
        @Query("apiKey") apiKey: String? = null
    ): Response<ProxyNewsResponse>

    @GET("news/count")
    suspend fun getCount(): Response<ProxyCountResponse>

    @GET("health")
    suspend fun getHealth(): Response<ProxyHealthResponse>
}

object ProxyApiClient {
    /**
     * CHANGE THIS to your deployed backend URL.
     *
     * For local development with Android emulator:
     *   "http://10.0.2.2:3000/"  (localhost from emulator POV)
     *
     * For production (Render, Railway, VPS):
     *   "https://your-production-url.onrender.com/"
     */
    private const val PROXY_BASE_URL = "https://news-service-yq17.onrender.com/"

    val service: ProxyApiService by lazy {
        Retrofit.Builder()
            .baseUrl(PROXY_BASE_URL)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(ProxyApiService::class.java)
    }
}
