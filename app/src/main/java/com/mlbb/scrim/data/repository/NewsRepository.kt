package com.mlbb.scrim.data.repository

import android.content.Context
import timber.log.Timber
import com.mlbb.scrim.data.localization.TranslationManager
import com.mlbb.scrim.data.model.NewsArticle
import com.mlbb.scrim.data.preferences.AppSettings
import com.mlbb.scrim.data.service.NewsApiClient
import com.mlbb.scrim.data.service.ProxyApiClient
import com.mlbb.scrim.data.service.TwitterApiClient
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale
import java.util.TimeZone

class NewsRepository(
    private val context: Context,
    private val translationManager: TranslationManager = TranslationManager()
) {

    companion object {
        private const val TAG = "NewsRepository"
        private const val NEWSAPI_KEY = com.mlbb.scrim.BuildConfig.NEWSAPI_KEY
        private const val X_BEARER_TOKEN = com.mlbb.scrim.BuildConfig.X_BEARER_TOKEN

        // Quota limits
        private const val X_API_MONTHLY_LIMIT = 100
        private const val X_API_CACHE_HOURS = 12L
        private const val X_API_CACHE_MS = X_API_CACHE_HOURS * 60 * 60 * 1000 // 12 hours

        // Anti-spam: minimum time between explicit pull-to-refresh (30 minutes)
        private const val MIN_EXPLICIT_REFRESH_MS = 30 * 60 * 1000 // 30 minutes

        // No demo articles — all news must come from real sources.
    }

    private var cachedArticles: List<NewsArticle> = emptyList()
    private var lastFetchTime: Long = 0
    private val memoryCacheMs = X_API_CACHE_MS // 12 hours

    private val appSettings by lazy { AppSettings(context) }

    /** Local file cache — survives app restarts, prevents DDoS-ing the backend */
    private val localCache by lazy { NewsCacheManager(context) }

    data class RefreshResult(
        val articles: List<NewsArticle>,
        val wasThrottled: Boolean = false,
        val minutesUntilRefresh: Int = 0
    )

    fun getNews(forceRefresh: Boolean = false, targetLanguage: String = "en"): Flow<Result<RefreshResult>> = flow {
        try {
            val now = System.currentTimeMillis()

            // Anti-spam: enforce minimum cooldown between explicit refreshes
            val lastExplicitRefresh = appSettings.xApiLastExplicitRefresh.first()
            val explicitCooldownRemaining = if (forceRefresh) {
                MIN_EXPLICIT_REFRESH_MS - (now - lastExplicitRefresh)
            } else 0L

            val isThrottled = forceRefresh && explicitCooldownRemaining > 0
            val effectiveForceRefresh = forceRefresh && !isThrottled

            if (isThrottled) {
                Timber.w(TAG, "Explicit refresh throttled. ${explicitCooldownRemaining / 1000 / 60} min remaining.")
            }

            // Check if we have valid cached articles in memory
            val useMemoryCache = !effectiveForceRefresh &&
                    cachedArticles.isNotEmpty() &&
                    (now - lastFetchTime) < memoryCacheMs

            val articles = if (useMemoryCache) {
                Timber.d(TAG, "Serving news from memory cache (${(now - lastFetchTime) / 1000 / 60} min old)")
                cachedArticles
            } else {
                if (effectiveForceRefresh) {
                    appSettings.setXApiLastExplicitRefresh(now)
                }
                val fetched = fetchFromSources(effectiveForceRefresh)
                cachedArticles = fetched
                lastFetchTime = now
                fetched
            }

            // Translate if needed (translation happens offline, doesn't count as an API request)
            val translatedArticles = if (targetLanguage != "en" && targetLanguage.isNotBlank()) {
                translateArticles(articles, targetLanguage)
            } else {
                articles
            }

            emit(Result.success(RefreshResult(
                articles = translatedArticles,
                wasThrottled = isThrottled,
                minutesUntilRefresh = if (isThrottled) (explicitCooldownRemaining / 1000 / 60).toInt() else 0
            )))
        } catch (e: Exception) {
            Timber.e(TAG, "Error fetching news", e)
            emit(Result.success(RefreshResult(
                articles = emptyList(),
                wasThrottled = false,
                minutesUntilRefresh = 0
            )))
        }
    }.flowOn(Dispatchers.IO)

    private suspend fun fetchFromSources(forceRefresh: Boolean): List<NewsArticle> {
        return try {
            val now = System.currentTimeMillis()

            // Priority -1: Local file cache on device — survives app restarts
            // This prevents a single user from DDoS-ing the backend proxy
            if (!forceRefresh) {
                val diskArticles = localCache.loadCache()
                if (diskArticles.isNotEmpty()) {
                    Timber.i(TAG, "Serving from local disk cache (${diskArticles.size} articles)")
                    return diskArticles
                }
            }

            // Priority 0: Backend Proxy API — unlimited, no X API quota burned
            // This is the preferred path for production.
            val proxyArticles = fetchFromProxy()
            if (proxyArticles.isNotEmpty()) {
                Timber.i(TAG, "Serving from backend proxy (${proxyArticles.size} articles)")
                localCache.saveCache(proxyArticles, source = "proxy")
                return proxyArticles
            }

            // Fallback path (used when proxy is unavailable or not deployed yet):
            // Priority 1: Official MLBB X (Twitter) account — only if quota allows
            val quotaStatus = checkXApiQuota(now)
            if (quotaStatus.canUse && (!quotaStatus.cacheValid || forceRefresh)) {
                Timber.d(TAG, "Fetching from X API (used ${quotaStatus.used}/$X_API_MONTHLY_LIMIT this month)")
                val xArticles = fetchFromTwitter()
                if (xArticles.isNotEmpty()) {
                    localCache.saveCache(xArticles, source = "x_api")
                    return xArticles
                }
            } else if (quotaStatus.cacheValid && cachedArticles.isNotEmpty()) {
                Timber.d(TAG, "X API cached. Skipping fetch. Quota: ${quotaStatus.used}/$X_API_MONTHLY_LIMIT")
            } else if (!quotaStatus.canUse) {
                Timber.w(TAG, "X API quota exhausted (${quotaStatus.used}/$X_API_MONTHLY_LIMIT). Using fallback.")
            }

            // Priority 2: NewsAPI (gaming news sites) — no strict quota
            val newsApiArticles = fetchFromNewsApi()
            if (newsApiArticles.isNotEmpty()) {
                localCache.saveCache(newsApiArticles, source = "newsapi")
                return newsApiArticles
            }

            // Priority 3: Reddit r/mobilelegends
            val redditArticles = fetchFromReddit()
            if (redditArticles.isNotEmpty()) {
                localCache.saveCache(redditArticles, source = "reddit")
                return redditArticles
            }

            // No fallback — return empty if all sources fail
            emptyList()
        } catch (e: Exception) {
            Timber.w(TAG, "Fetch failed, returning empty", e)
            emptyList()
        }
    }

    /**
     * Fetch news from our backend proxy API with drip-feed.
     *
     * The user has a dripIndex that auto-increments by +1 every 2 hours.
     * The backend only returns articles with dripIndex <= user's offset.
     * This prevents overwhelming the user with 20+ articles at once.
     *
     * Also updates totalInArchive from the response for UI progress bar.
     */
    private suspend fun fetchFromProxy(): List<NewsArticle> {
        return withContext(Dispatchers.IO) {
            try {
                // 1. Tick the drip — unlock +1 article per 2h elapsed
                val newlyUnlocked = appSettings.tickNewsDrip()
                if (newlyUnlocked > 0) {
                    Timber.i(TAG, "Drip: $newlyUnlocked new article(s) unlocked")
                }

                // 2. Fetch articles up to current drip index
                val dripIndex = appSettings.newsDripIndex.first()
                val response = ProxyApiClient.service.getNews(offset = dripIndex)

                if (response.isSuccessful) {
                    val body = response.body()
                    val articles = body?.articles ?: emptyList()

                    // Sync total archive count for progress bar
                    body?.totalInArchive?.let { total ->
                        appSettings.setNewsDripCountTotal(total)
                    }

                    Timber.d(TAG, "Proxy drip: ${articles.size} visible (offset=$dripIndex, unseen=${body?.unseen}, total=${body?.totalInArchive})")
                    articles
                } else {
                    Timber.w(TAG, "Proxy API error: ${response.code()}")
                    emptyList()
                }
            } catch (e: Exception) {
                Timber.w(TAG, "Proxy API unreachable — will try fallback sources. Reason: ${e.message}")
                emptyList()
            }
        }
    }

    private data class QuotaStatus(val canUse: Boolean, val used: Int, val cacheValid: Boolean)

    private suspend fun checkXApiQuota(now: Long): QuotaStatus {
        val appSettings = AppSettings(context)
        var used = appSettings.xApiRequestsUsed.first()
        var monthStart = appSettings.xApiMonthStart.first()
        val lastFetch = appSettings.xApiLastFetch.first()

        // Detect new month — reset counter
        val currentMonthStart = getMonthStartTimestamp(now)
        if (monthStart == 0L || monthStart != currentMonthStart) {
            Timber.i(TAG, "New month detected. Resetting X API quota counter.")
            appSettings.resetXApiQuota(currentMonthStart)
            used = 0
            monthStart = currentMonthStart
        }

        val canUse = used < X_API_MONTHLY_LIMIT
        val cacheValid = lastFetch > 0 && (now - lastFetch) < X_API_CACHE_MS

        return QuotaStatus(canUse = canUse, used = used, cacheValid = cacheValid)
    }

    private fun getMonthStartTimestamp(now: Long): Long {
        val cal = Calendar.getInstance(TimeZone.getTimeZone("UTC"))
        cal.timeInMillis = now
        cal.set(Calendar.DAY_OF_MONTH, 1)
        cal.set(Calendar.HOUR_OF_DAY, 0)
        cal.set(Calendar.MINUTE, 0)
        cal.set(Calendar.SECOND, 0)
        cal.set(Calendar.MILLISECOND, 0)
        return cal.timeInMillis
    }

    private suspend fun fetchFromTwitter(): List<NewsArticle> {
        return withContext(Dispatchers.IO) {
            try {
                val authHeader = "Bearer $X_BEARER_TOKEN"
                val response = TwitterApiClient.service.searchRecentTweets(
                    query = "from:MobileLegendsOL -is:retweet",
                    maxResults = 50, // Max per request — get as many as possible
                    authorization = authHeader
                )

                if (response.isSuccessful) {
                    val body = response.body()
                    val tweets = body?.data ?: emptyList()
                    val mediaMap = body?.includes?.media?.associateBy { it.mediaKey } ?: emptyMap()

                    // Track this successful API call
                    appSettings.incrementXApiRequest()
                    appSettings.setXApiLastFetch(System.currentTimeMillis())

                    Timber.i(TAG, "X API: fetched ${tweets.size} tweets. Used +1 request.")

                    tweets.mapNotNull { tweet ->
                        val imageUrl = tweet.attachments?.mediaKeys?.firstNotNullOfOrNull { key ->
                            when (mediaMap[key]?.type) {
                                "photo" -> mediaMap[key]?.url
                                "video" -> mediaMap[key]?.previewImageUrl
                                else -> null
                            }
                        } ?: ""

                        NewsArticle(
                            id = "x_${tweet.id}",
                            title = tweet.text.take(100).let {
                                if (it.length >= 100) "$it..." else it
                            },
                            description = tweet.text,
                            content = tweet.text,
                            url = "https://x.com/MobileLegendsOL/status/${tweet.id}",
                            imageUrl = imageUrl,
                            source = "X / @MobileLegendsOL",
                            publishedAt = parseTweetDateToMillis(tweet.createdAt),
                            originalLanguage = "en"
                        )
                    }
                } else {
                    val errorBody = response.errorBody()?.string()
                    Timber.w(TAG, "X API error: ${response.code()} - $errorBody")
                    // If rate limited (429), don't count as used
                    if (response.code() != 429) {
                        appSettings.incrementXApiRequest()
                    }
                    emptyList()
                }
            } catch (e: Exception) {
                Timber.w(TAG, "X/Twitter fetch failed", e)
                emptyList()
            }
        }
    }

    private fun parseTweetDateToMillis(dateString: String): Long {
        return try {
            val format = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", Locale.US)
            format.timeZone = TimeZone.getTimeZone("UTC")
            format.parse(dateString)?.time ?: System.currentTimeMillis()
        } catch (e: Exception) {
            try {
                val format = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.US)
                format.timeZone = TimeZone.getTimeZone("UTC")
                format.parse(dateString)?.time ?: System.currentTimeMillis()
            } catch (e2: Exception) {
                System.currentTimeMillis()
            }
        }
    }

    private suspend fun fetchFromNewsApi(): List<NewsArticle> {
        return withContext(Dispatchers.IO) {
            try {
                val service = NewsApiClient.createNewsApiService(NEWSAPI_KEY)
                val response = service.getMobileLegendsNews(
                    apiKey = NEWSAPI_KEY
                )
                if (response.isSuccessful) {
                    val body = response.body()
                    val articles = body?.articles ?: emptyList()

                    articles.filter { article ->
                        article.title.isNotBlank() &&
                        !article.title.contains("[Removed]", ignoreCase = true) &&
                        isMlbbRelated(article.title, article.description ?: "")
                    }.map { article ->
                        NewsArticle(
                            id = "newsapi_${article.url.hashCode()}",
                            title = article.title,
                            description = article.description ?: "",
                            content = article.content ?: article.description ?: "",
                            url = article.url,
                            imageUrl = article.urlToImage ?: "",
                            source = article.source?.name ?: "NewsAPI",
                            publishedAt = parseDateToMillis(article.publishedAt),
                            originalLanguage = "en"
                        )
                    }
                } else {
                    Timber.w(TAG, "NewsAPI error: ${response.code()}")
                    emptyList()
                }
            } catch (e: Exception) {
                Timber.w(TAG, "NewsAPI fetch failed", e)
                emptyList()
            }
        }
    }

    private fun isMlbbRelated(title: String, description: String): Boolean {
        val text = "$title $description".lowercase()

        // BLOCKLIST: if any of these non-MLBB terms appear, reject the article immediately
        val blocklist = listOf(
            "apple", "macbook", "iphone", "ipad", "mac pro", "imac", "mac studio",
            " Qualcomm ", "snapdragon", "intel", "amd", "nvidia", "rtx", "gpu",
            "processor", "chipset", "silicon", "tsmc", "semiconductor", "cpu",
            "tesla", "spacex", "elon musk", "boeing", "airbus", "ford", "toyota",
            "bitcoin", "ethereum", "crypto", "nft", "blockchain",
            "microsoft", "windows 11", "playstation 5", "ps5", "xbox",
            "gta 6", "gta vi", "call of duty", "fortnite", "valorant",
            "league of legends", "lol ", "dota", "cs2", "counter-strike",
            "genshin impact", "honkai", "pokemon", "minecraft",
            "nintendo", "switch 2", "super mario", "zelda",
            "samsung galaxy", "pixel ", "oneplus", "xiaomi"
        )
        if (blocklist.any { text.contains(it) }) {
            return false
        }

        // STRONG keywords: must match at least one of these to be considered MLBB news
        val strongKeywords = listOf(
            "mobile legends", "moonton", "mlbb",
            "mpl philippines", "mpl indonesia", "mpl malaysia", "mpl singapore",
            "mpl mena", "mpl brazil", "mpl latam",
            "magic chess", "land of dawn",
            "fanny mlbb", "lancelot mlbb", "brody mlbb", "chou mlbb", "lunox mlbb",
            "ling mlbb", "wanwan mlbb", "beatrix mlbb", "paquito mlbb",
            "martis mlbb", "helcurt mlbb", "selena mlbb", "moskov mlbb",
            "karrie mlbb", "claude mlbb", "granger mlbb", "kimmy mlbb",
            "bruno mlbb", "clint mlbb", "ixia mlbb", "natan mlbb",
            "harith mlbb", "cecilion mlbb", "xavier mlbb", "valentina mlbb",
            "joy mlbb", "fredrinn mlbb", "arlott mlbb", "novaria mlbb",
            "chip mlbb", "cici mlbb",
            "miya mlbb", "layla mlbb", "saber mlbb", "zilong mlbb",
            "aldous mlbb", "yin mlbb",
            "mcl ", "mcl mobile", "msc ", "msc mobile",
            "mythic rank", "mythic mlbb", "grandmaster mlbb",
            "savage mlbb", "maniac mlbb",
            "exp lane mlbb", "gold lane mlbb", "roam mlbb"
        )
        return strongKeywords.any { text.contains(it) }
    }

    private fun parseDateToMillis(dateString: String): Long {
        return try {
            val format = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.US)
            format.timeZone = TimeZone.getTimeZone("UTC")
            format.parse(dateString)?.time ?: System.currentTimeMillis()
        } catch (e: Exception) {
            System.currentTimeMillis()
        }
    }

    private suspend fun fetchFromReddit(): List<NewsArticle> {
        return withContext(Dispatchers.IO) {
            try {
                val response = NewsApiClient.redditService.getMobileLegendsPosts(limit = 20)
                if (response.isSuccessful) {
                    val listing = response.body()
                    val posts = listing?.data?.children?.mapNotNull { it.data } ?: emptyList()

                    posts.filter { post ->
                        // Filter out low-quality posts
                        post.title.isNotBlank() &&
                        !post.title.contains("[Removed]", ignoreCase = true) &&
                        post.thumbnail != null &&
                        post.thumbnail != "self" &&
                        post.thumbnail != "default"
                    }.map { post ->
                        NewsArticle(
                            id = "reddit_${post.id}",
                            title = post.title,
                            description = post.selfText.take(300),
                            content = post.selfText,
                            url = "https://www.reddit.com${post.permalink}",
                            imageUrl = if (post.thumbnail?.startsWith("http") == true)
                                post.thumbnail else "",
                            source = "r/${post.subreddit}",
                            publishedAt = (post.createdUtc * 1000).toLong(),
                            originalLanguage = "en"
                        )
                    }
                } else {
                    emptyList()
                }
            } catch (e: Exception) {
                Timber.w(TAG, "Reddit fetch failed", e)
                emptyList()
            }
        }
    }

    private suspend fun translateArticles(
        articles: List<NewsArticle>,
        targetLanguage: String
    ): List<NewsArticle> {
        return withContext(Dispatchers.IO) {
            articles.map { article ->
                try {
                    val (tTitle, tDesc, tContent) = translationManager.translateArticle(
                        article.title,
                        article.description,
                        article.content,
                        targetLanguage
                    )
                    article.copy(
                        title = tTitle,
                        description = tDesc,
                        content = tContent,
                        isTranslated = true
                    )
                } catch (e: Exception) {
                    article // Return original if translation fails
                }
            }
        }
    }

    suspend fun clearCache() {
        cachedArticles = emptyList()
        lastFetchTime = 0
        localCache.clearCache()
        Timber.d(TAG, "All caches cleared (memory + disk)")
    }

    fun close() {
        translationManager.closeTranslators()
    }
}
