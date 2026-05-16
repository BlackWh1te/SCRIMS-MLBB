package com.mlbb.scrim.data.repository

import android.content.Context
import android.util.Log
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
        private const val NEWSAPI_KEY = "0ef43d1109b04f99b04e5b1292dbc7d6"

        // X (Twitter) API v2 Bearer Token
        private const val X_BEARER_TOKEN = "AAAAAAAAAAAAAAAAAAAAAFE99gEAAAAA8Nmx2aEY1BaHnOMpsCWftfC7kMA%3Da92UZ0B8jQ0XhhHfINCfHiolLfGhC6uEAQKqWWgMPfpn12Kmkd"

        // Quota limits
        private const val X_API_MONTHLY_LIMIT = 100
        private const val X_API_CACHE_HOURS = 12L
        private const val X_API_CACHE_MS = X_API_CACHE_HOURS * 60 * 60 * 1000 // 12 hours

        // Anti-spam: minimum time between explicit pull-to-refresh (30 minutes)
        private const val MIN_EXPLICIT_REFRESH_MS = 30 * 60 * 1000 // 30 minutes

        // Demo news articles about MLBB
        private val demoNews = listOf(
            NewsArticle(
                id = "demo_1",
                title = "Mobile Legends MPL Season 14 Finals Set for Epic Showdown",
                description = "The top 4 teams will battle for the championship title and a prize pool of $300,000 in the upcoming MPL finals.",
                content = "The Mobile Legends Professional League (MPL) Season 14 has reached its climax with the top 4 teams qualifying for the grand finals. This season has seen record-breaking viewership numbers and intense competition across all regions. The finals will feature defending champions Blacklist International against rising contenders Echo, RSG, and ONIC.",
                url = "https://www.oneesports.gg/mobile-legends/",
                imageUrl = "https://images.unsplash.com/photo-1542751371-adc38448a05e?w=800",
                source = "ONE Esports",
                publishedAt = System.currentTimeMillis() - 3600000 * 2,
                originalLanguage = "en"
            ),
            NewsArticle(
                id = "demo_2",
                title = "Moonton Announces New Hero: The Shadow Assassin",
                description = "A new marksman/assassin hybrid hero is coming to the Land of Dawn with unique stealth mechanics.",
                content = "Moonton has teased their latest hero addition to Mobile Legends: Bang Bang. The Shadow Assassin brings a fresh take on the marksman role with stealth-based abilities that allow repositioning during team fights. Early test server footage shows impressive mobility and burst damage potential.",
                url = "https://www.oneesports.gg/mobile-legends/",
                imageUrl = "https://images.unsplash.com/photo-1511512578047-dfb367046420?w=800",
                source = "Moonton Official",
                publishedAt = System.currentTimeMillis() - 3600000 * 6,
                originalLanguage = "en"
            ),
            NewsArticle(
                id = "demo_3",
                title = "M5 World Championship: Southeast Asia Dominates Group Stage",
                description = "Teams from Philippines and Indonesia show strong performance in the M5 World Championship group stage.",
                content = "The M5 World Championship group stage has concluded with Southeast Asian teams showing their dominance. Filipino teams Blacklist International and AP.Bren secured top seeds, while Indonesian representatives ONIC Esports and RRQ also advanced to the knockout stage with strong showings.",
                url = "https://www.oneesports.gg/mobile-legends/",
                imageUrl = "https://images.unsplash.com/photo-1542751110-97427bbecf20?w=800",
                source = "Esports Insider",
                publishedAt = System.currentTimeMillis() - 3600000 * 12,
                originalLanguage = "en"
            ),
            NewsArticle(
                id = "demo_4",
                title = "Patch Notes 1.8.92: Major Balance Changes for Meta Heroes",
                description = "Fanny, Lancelot, and Brody receive significant adjustments in the latest patch update.",
                content = "The latest Mobile Legends patch brings substantial balance changes. Fanny's energy costs have been increased to reduce her early game dominance. Lancelot's ultimate damage scaling has been adjusted, while Brody receives a slight buff to his base attack speed. Several item changes are also included in this update.",
                url = "https://www.oneesports.gg/mobile-legends/",
                imageUrl = "https://images.unsplash.com/photo-1538481199705-c710c4e965fc?w=800",
                source = "Patch Notes",
                publishedAt = System.currentTimeMillis() - 3600000 * 24,
                originalLanguage = "en"
            ),
            NewsArticle(
                id = "demo_5",
                title = "New Skins Incoming: Starlight and Collector Events",
                description = "This month's skin lineup features stunning new designs for popular heroes.",
                content = "Moonton has revealed the upcoming skin releases for this month. The Starlight Pass will feature a mystical themed skin for mage hero Lunox, while the Collector event brings an exclusive legendary skin for fighter hero Chou. Limited-time events will also offer chances to obtain previous collector skins at discounted rates.",
                url = "https://www.oneesports.gg/mobile-legends/",
                imageUrl = "https://images.unsplash.com/photo-1550745165-9bc0b252726f?w=800",
                source = "MLBB News",
                publishedAt = System.currentTimeMillis() - 3600000 * 36,
                originalLanguage = "en"
            ),
            NewsArticle(
                id = "demo_6",
                title = "Ranked Season Reset: New Rewards and Rank Protection",
                description = "The new ranked season brings updated rewards and improved rank protection for players.",
                content = "A new ranked season has begun in Mobile Legends with refreshed rewards and an updated rank protection system. Players will now receive bonus protection points after consecutive losses, helping to reduce the frustration of deranking. New seasonal skins and exclusive avatar borders are also available as ranked rewards.",
                url = "https://www.oneesports.gg/mobile-legends/",
                imageUrl = "https://images.unsplash.com/photo-1519669556878-63bd25466644?w=800",
                source = "Game Guides",
                publishedAt = System.currentTimeMillis() - 3600000 * 48,
                originalLanguage = "en"
            )
        )
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
                Log.w(TAG, "Explicit refresh throttled. ${explicitCooldownRemaining / 1000 / 60} min remaining.")
            }

            // Check if we have valid cached articles in memory
            val useMemoryCache = !effectiveForceRefresh &&
                    cachedArticles.isNotEmpty() &&
                    (now - lastFetchTime) < memoryCacheMs

            val articles = if (useMemoryCache) {
                Log.d(TAG, "Serving news from memory cache (${(now - lastFetchTime) / 1000 / 60} min old)")
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
            Log.e(TAG, "Error fetching news", e)
            val fallback = if (targetLanguage != "en") {
                translateArticles(demoNews, targetLanguage)
            } else {
                demoNews
            }
            emit(Result.success(RefreshResult(
                articles = fallback,
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
                    Log.i(TAG, "Serving from local disk cache (${diskArticles.size} articles)")
                    return diskArticles
                }
            }

            // Priority 0: Backend Proxy API — unlimited, no X API quota burned
            // This is the preferred path for production.
            val proxyArticles = fetchFromProxy()
            if (proxyArticles.isNotEmpty()) {
                Log.i(TAG, "Serving from backend proxy (${proxyArticles.size} articles)")
                localCache.saveCache(proxyArticles, source = "proxy")
                return proxyArticles
            }

            // Fallback path (used when proxy is unavailable or not deployed yet):
            // Priority 1: Official MLBB X (Twitter) account — only if quota allows
            val quotaStatus = checkXApiQuota(now)
            if (quotaStatus.canUse && (!quotaStatus.cacheValid || forceRefresh)) {
                Log.d(TAG, "Fetching from X API (used ${quotaStatus.used}/$X_API_MONTHLY_LIMIT this month)")
                val xArticles = fetchFromTwitter()
                if (xArticles.isNotEmpty()) {
                    localCache.saveCache(xArticles, source = "x_api")
                    return xArticles
                }
            } else if (quotaStatus.cacheValid && cachedArticles.isNotEmpty()) {
                Log.d(TAG, "X API cached. Skipping fetch. Quota: ${quotaStatus.used}/$X_API_MONTHLY_LIMIT")
            } else if (!quotaStatus.canUse) {
                Log.w(TAG, "X API quota exhausted (${quotaStatus.used}/$X_API_MONTHLY_LIMIT). Using fallback.")
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

            // Final fallback: demo articles
            localCache.saveCache(demoNews, source = "demo")
            demoNews
        } catch (e: Exception) {
            Log.w(TAG, "Fetch failed, using demo data", e)
            localCache.saveCache(demoNews, source = "demo")
            demoNews
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
                    Log.i(TAG, "Drip: $newlyUnlocked new article(s) unlocked")
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

                    Log.d(TAG, "Proxy drip: ${articles.size} visible (offset=$dripIndex, unseen=${body?.unseen}, total=${body?.totalInArchive})")
                    articles
                } else {
                    Log.w(TAG, "Proxy API error: ${response.code()}")
                    emptyList()
                }
            } catch (e: Exception) {
                Log.w(TAG, "Proxy API unreachable — will try fallback sources. Reason: ${e.message}")
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
            Log.i(TAG, "New month detected. Resetting X API quota counter.")
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

                    Log.i(TAG, "X API: fetched ${tweets.size} tweets. Used +1 request.")

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
                    Log.w(TAG, "X API error: ${response.code()} - $errorBody")
                    // If rate limited (429), don't count as used
                    if (response.code() != 429) {
                        appSettings.incrementXApiRequest()
                    }
                    emptyList()
                }
            } catch (e: Exception) {
                Log.w(TAG, "X/Twitter fetch failed", e)
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
                    Log.w(TAG, "NewsAPI error: ${response.code()}")
                    emptyList()
                }
            } catch (e: Exception) {
                Log.w(TAG, "NewsAPI fetch failed", e)
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
                Log.w(TAG, "Reddit fetch failed", e)
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
        Log.d(TAG, "All caches cleared (memory + disk)")
    }

    fun close() {
        translationManager.closeTranslators()
    }
}
